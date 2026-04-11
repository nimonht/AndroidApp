"""
ADB bridge for communicating with the Quizzez Android app console.

Sends console commands to a running Android app/emulator via ADB ordered
broadcast intents and reads the result directly from the broadcast's return
value (Base64-encoded JSON).

Transport detail:
  Send:   adb shell am broadcast --ordered
              -a  com.example.androidapp.CONSOLE_COMMAND
              -n  com.example.androidapp/.ConsoleBroadcastReceiver
              -e  command "<cmd>"
  Receive: ConsoleBroadcastReceiver.setResultData(base64Json) before finish().
           am broadcast then prints:
             Broadcast completed: result=0, data="<base64>"
           We regex-extract the base64 token and decode it to JSON.

This transport requires NO file-system access and works identically in
debug and release builds.  It replaces the previous approach that used
``run-as`` to read a private file, which was restricted to debuggable builds.

Result JSON shape (set by the app, decoded here):
  {
    "success": true,
    "exitCode": 0,
    "output": [
      {"text": "...", "style": "NORMAL"},
      {"text": "...", "style": "SUCCESS"}
    ]
  }
"""

from __future__ import annotations

import base64
import json
import logging
import re
import shutil
import subprocess
import time
from dataclasses import dataclass, field

logger = logging.getLogger("quizzez-mcp.adb")

PACKAGE = "com.example.androidapp"
ACTION = f"{PACKAGE}.CONSOLE_COMMAND"
RECEIVER = f"{PACKAGE}/.ConsoleBroadcastReceiver"

# Seconds added on top of self.timeout for the subprocess hard wall-clock limit.
# Accounts for ADB round-trip overhead and Binder IPC latency.
BROADCAST_TIMEOUT_BUFFER = 5.0

# Default end-to-end timeout for a single command (am broadcast blocks until
# ConsoleBroadcastReceiver calls PendingResult.finish()).
DEFAULT_COMMAND_TIMEOUT = 15.0

# Compiled once: extracts the Base64 payload from am broadcast stdout line:
#   Broadcast completed: result=0, data="<base64>"
_BROADCAST_DATA_RE = re.compile(r'data="([A-Za-z0-9+/=]+)"')


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------


@dataclass
class CommandOutput:
    """Parsed result of a console command execution."""

    success: bool
    exit_code: int
    lines: list[OutputLine] = field(default_factory=list)
    raw_json: str | None = None
    error_message: str | None = None

    @property
    def text(self) -> str:
        """All output lines joined as plain text."""
        return "\n".join(line.text for line in self.lines)


@dataclass
class OutputLine:
    """A single styled output line."""

    text: str
    style: str = "NORMAL"


# ---------------------------------------------------------------------------
# ADB helpers
# ---------------------------------------------------------------------------


def _run_adb(
    args: list[str], timeout: float = 10.0
) -> subprocess.CompletedProcess[str]:
    """Run an adb command and return the CompletedProcess."""
    cmd = ["adb"] + args
    logger.debug("Running: %s", " ".join(cmd))
    return subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        timeout=timeout,
    )


# ---------------------------------------------------------------------------
# AdbBridge -- real device / emulator communication
# ---------------------------------------------------------------------------


class AdbBridge:
    """
    Sends console commands to the Android app via ADB ordered broadcast intents
    and reads the result from the broadcast's return value.

    The app's ConsoleBroadcastReceiver:
      1. Receives the intent.
      2. Calls goAsync() so a coroutine can complete asynchronously.
      3. Executes the command via CommandExecutor.
      4. Calls pendingResult.setResultData(Base64(json)) then pendingResult.finish().

    am broadcast (--ordered) blocks until finish() is called, then prints:
      Broadcast completed: result=0, data="<base64>"

    We parse the base64 token from that line and decode it to raw JSON.

    No file-system access is required.  Works in debug AND release builds.
    """

    def __init__(
        self,
        *,
        device_serial: str | None = None,
        timeout: float = DEFAULT_COMMAND_TIMEOUT,
    ):
        self.device_serial = device_serial
        self.timeout = timeout

    # -- public API ----------------------------------------------------------

    def check_adb_available(self) -> bool:
        """Return True if the ``adb`` binary is on PATH."""
        return shutil.which("adb") is not None

    def check_device_connected(self) -> tuple[bool, str]:
        """
        Check whether at least one device/emulator is connected.

        Returns (connected, detail_message).
        """
        if not self.check_adb_available():
            return False, "adb not found on PATH"
        try:
            result = _run_adb(["devices"])
            lines = [
                ln
                for ln in result.stdout.strip().splitlines()[1:]
                if ln.strip() and "device" in ln
            ]
            if not lines:
                return False, "No device connected"
            devices = [ln.split("\t")[0] for ln in lines]
            return True, f"Connected devices: {', '.join(devices)}"
        except Exception as exc:
            return False, f"Error checking devices: {exc}"

    def execute(self, command: str) -> CommandOutput:
        """
        Execute a console command on the connected device/emulator.

        Sends an ordered broadcast to ConsoleBroadcastReceiver and waits for
        the result to be returned via setResultData(base64Json).  No polling
        or file-system access is required -- the result travels entirely over
        Binder IPC and is printed to am broadcast stdout.
        """
        if not self.check_adb_available():
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message="adb not found on PATH. Please install the Android SDK.",
            )

        connected, msg = self.check_device_connected()
        if not connected:
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message=msg,
            )

        try:
            raw_json = self._broadcast_and_receive(command)
            if raw_json is None:
                return CommandOutput(
                    success=False,
                    exit_code=1,
                    error_message=(
                        f"No result received from app after {self.timeout}s. "
                        "Ensure the app is running and ConsoleBroadcastReceiver is registered."
                    ),
                )
            return self._parse_result(raw_json)

        except subprocess.TimeoutExpired:
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message=(
                    f"ADB command timed out after {self.timeout + BROADCAST_TIMEOUT_BUFFER:.0f}s. "
                    "The command may still be running on the device but wait time exceeded."
                ),
            )
        except Exception as exc:
            logger.exception("ADB execution error for command: %r", command)
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message=f"ADB error: {exc}",
            )

    # -- private helpers -----------------------------------------------------

    def _adb_args(self) -> list[str]:
        """Return device-selector arguments for adb if a serial is set."""
        if self.device_serial:
            return ["-s", self.device_serial]
        return []

    def _broadcast_and_receive(self, command: str) -> str | None:
        """
        Send a broadcast and return the decoded JSON result string,
        or None if the broadcast produced no result data within the timeout.

        Protocol:
          1. adb shell am broadcast -a <ACTION> -n <RECEIVER> -e command "<cmd>"
          2. ConsoleBroadcastReceiver executes the command asynchronously (goAsync),
             then calls pendingResult.setResultData(Base64(json)) + pendingResult.finish().
          3. am broadcast prints: Broadcast completed: result=N, data="<base64>"
          4. We extract the base64 token via regex and decode it to UTF-8 JSON.

        Note: --ordered is intentionally omitted.  It is not recognised on all
        Android API levels (the flag was removed / renamed in newer platform
        builds).  The app's ConsoleBroadcastReceiver returns result data via
        setResultData regardless, and am broadcast always prints the data= field
        when a component is explicitly targeted with -n.
        """
        escaped = command.replace('"', '\\"')

        result = _run_adb(
            self._adb_args()
            + [
                "shell",
                "am",
                "broadcast",
                "-a",
                ACTION,
                "-n",
                RECEIVER,
                "-e",
                "command",
                f'"{escaped}"',
            ],
            timeout=self.timeout + BROADCAST_TIMEOUT_BUFFER,
        )

        if result.returncode != 0:
            logger.warning(
                "am broadcast returned exit code %d. stderr: %s",
                result.returncode,
                result.stderr.strip(),
            )
            return None

        match = _BROADCAST_DATA_RE.search(result.stdout)
        if not match:
            logger.warning(
                "No base64 result data in am broadcast output. "
                "Ensure ConsoleBroadcastReceiver is registered and the app is running. "
                "Raw stdout: %r",
                result.stdout.strip(),
            )
            return None

        try:
            raw_json = base64.b64decode(match.group(1)).decode("utf-8")
            logger.debug(
                "Decoded %d bytes of JSON from broadcast result.", len(raw_json)
            )
            return raw_json
        except Exception as exc:
            logger.warning("base64 decode failed for broadcast result: %s", exc)
            return None

    @staticmethod
    def _parse_result(raw_json: str) -> CommandOutput:
        try:
            data = json.loads(raw_json)
        except json.JSONDecodeError as exc:
            return CommandOutput(
                success=False,
                exit_code=1,
                raw_json=raw_json,
                error_message=f"JSON parse error: {exc}",
            )

        lines = [
            OutputLine(text=item.get("text", ""), style=item.get("style", "NORMAL"))
            for item in data.get("output", [])
        ]
        return CommandOutput(
            success=data.get("success", False),
            exit_code=data.get("exitCode", 1 if not data.get("success") else 0),
            lines=lines,
            raw_json=raw_json,
        )
