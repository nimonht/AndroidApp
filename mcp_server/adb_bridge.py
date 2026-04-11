"""
ADB bridge for communicating with the Quizzez Android app console.

This module provides two execution backends:

1. **AdbBridge** -- sends console commands to a running Android app/emulator
   via ADB ordered broadcast intents and reads the result directly from the
   broadcast's return value (Base64-encoded JSON).

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
   debug and release builds. It replaces the previous approach that used
   `run-as` to read a private file, which was restricted to debuggable builds.

2. **MockExecutor** -- returns realistic mock responses for all ~32 commands
   so that the MCP server can be developed and tested without a live device.

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
import random
import re
import shutil
import subprocess
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone

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
            return False, "adb khong tim thay tren PATH"
        try:
            result = _run_adb(["devices"])
            lines = [
                ln
                for ln in result.stdout.strip().splitlines()[1:]
                if ln.strip() and "device" in ln
            ]
            if not lines:
                return False, "Khong co thiet bi nao ket noi"
            devices = [ln.split("\t")[0] for ln in lines]
            return True, f"Thiet bi ket noi: {', '.join(devices)}"
        except Exception as exc:
            return False, f"Loi khi kiem tra thiet bi: {exc}"

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
                error_message="adb khong tim thay tren PATH. Hay cai dat Android SDK.",
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
                        f"Khong nhan duoc ket qua tu ung dung sau {self.timeout}s. "
                        "Kiem tra app dang chay va ConsoleBroadcastReceiver da duoc dang ky."
                    ),
                )
            return self._parse_result(raw_json)

        except subprocess.TimeoutExpired:
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message=(
                    f"Lenh ADB timeout sau {self.timeout + BROADCAST_TIMEOUT_BUFFER:.0f}s. "
                    "Lenh co the dang chay tren thiet bi nhung qua thoi gian cho."
                ),
            )
        except Exception as exc:
            logger.exception("ADB execution error for command: %r", command)
            return CommandOutput(
                success=False,
                exit_code=1,
                error_message=f"Loi ADB: {exc}",
            )

    # -- private helpers -----------------------------------------------------

    def _adb_args(self) -> list[str]:
        """Return device-selector arguments for adb if a serial is set."""
        if self.device_serial:
            return ["-s", self.device_serial]
        return []

    def _broadcast_and_receive(self, command: str) -> str | None:
        """
        Send an ordered broadcast and return the decoded JSON result string,
        or None if the broadcast produced no result data within the timeout.

        Protocol:
          1. adb shell am broadcast --ordered -a <ACTION> -n <RECEIVER> -e command "<cmd>"
          2. ConsoleBroadcastReceiver executes the command asynchronously (goAsync),
             then calls pendingResult.setResultData(Base64(json)) + pendingResult.finish().
          3. am broadcast prints: Broadcast completed: result=N, data="<base64>"
          4. We extract the base64 token via regex and decode it to UTF-8 JSON.
        """
        escaped = command.replace('"', '\\"')

        result = _run_adb(
            self._adb_args()
            + [
                "shell",
                "am",
                "broadcast",
                "--ordered",
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


# ---------------------------------------------------------------------------
# MockExecutor -- realistic responses without a real device
# ---------------------------------------------------------------------------


class MockExecutor:
    """Returns realistic mock responses for all ~32 commands.

    Pipe commands (grep, sort, head, tail, count) are parsed but not applied
    in mock mode -- only the first pipeline segment is executed.
    """

    # -- Static mock data ----------------------------------------------------

    _USERS = [
        {
            "id": "user_001",
            "email": "admin@quizzez.app",
            "name": "Admin",
            "role": "ADMIN",
            "status": "Hoat dong",
        },
        {
            "id": "user_002",
            "email": "demo@quizzez.app",
            "name": "Demo User",
            "role": "USER",
            "status": "Hoat dong",
        },
        {
            "id": "user_003",
            "email": "test@quizzez.app",
            "name": "Test User",
            "role": "USER",
            "status": "Bi cam",
        },
        {
            "id": "user_004",
            "email": "guest@quizzez.app",
            "name": "Guest",
            "role": "GUEST",
            "status": "Hoat dong",
        },
    ]

    _QUIZZES = [
        {
            "id": "quiz_001",
            "title": "Kotlin co ban",
            "author": "demo@q.app",
            "status": "Cong khai",
            "questions": 10,
            "plays": 245,
        },
        {
            "id": "quiz_002",
            "title": "Jetpack Compose",
            "author": "demo@q.app",
            "status": "Rieng tu",
            "questions": 15,
            "plays": 89,
        },
        {
            "id": "quiz_003",
            "title": "Thuat toan",
            "author": "admin@q.app",
            "status": "Nhap",
            "questions": 20,
            "plays": 0,
        },
    ]

    _ATTEMPTS = [
        {
            "id": "att_001",
            "quiz": "Kotlin co ban",
            "user": "demo@q.app",
            "score": "80%",
            "date": "14/03/2025",
        },
        {
            "id": "att_002",
            "quiz": "Jetpack Compose",
            "user": "demo@q.app",
            "score": "73%",
            "date": "13/03/2025",
        },
    ]

    _POOL = [
        {
            "id": "pool_001",
            "content": "val vs var trong Kotlin?",
            "tag": "kotlin",
            "author": "demo@q.app",
        },
        {
            "id": "pool_002",
            "content": "ViewModel la gi?",
            "tag": "android",
            "author": "demo@q.app",
        },
    ]

    _LOG_ENTRIES = [
        ("14:20:00", "I", "SyncManager", "Sync completed successfully"),
        ("14:21:01", "D", "QuizRepo", "Loaded 12 quizzes from cache"),
        ("14:22:02", "W", "AuthRepo", "User session refreshed"),
        ("14:23:03", "E", "NetworkMonitor", "Network state changed: CONNECTED"),
        ("14:24:04", "I", "RoomDB", "Query returned 5 results in 23ms"),
        ("14:25:05", "I", "Firebase", "Snapshot listener registered for quizzes"),
        ("14:26:06", "D", "ViewModel", "UI state updated: HomeUiState(loading=false)"),
        ("14:27:07", "I", "Compose", "Recomposition triggered for QuizCard"),
    ]

    _PERMISSIONS = [
        ("MANAGE_USERS", "Quan ly tai khoan nguoi dung"),
        ("CHANGE_USER_ROLES", "Thay doi vai tro nguoi dung"),
        ("DELETE_USERS", "Xoa tai khoan nguoi dung"),
        ("BAN_USERS", "Cam/go cam nguoi dung"),
        ("MANAGE_QUIZZES", "Quan ly bai kiem tra"),
        ("DELETE_QUIZZES", "Xoa bai kiem tra"),
        ("PUBLISH_QUIZZES", "Xuat ban bai kiem tra"),
        ("VIEW_REPORTS", "Xem bao cao va thong ke"),
    ]

    # Permissions held by the mock admin account
    _ADMIN_PERMS = {
        "MANAGE_USERS",
        "BAN_USERS",
        "MANAGE_QUIZZES",
        "PUBLISH_QUIZZES",
        "VIEW_REPORTS",
    }

    # -- Lifecycle -----------------------------------------------------------

    def __init__(self) -> None:
        self._history: list[str] = []
        self._aliases: dict[str, str] = {}
        self._config: dict[str, str] = {
            "theme": "dark",
            "language": "vi",
            "notifications": "true",
            "auto_sync": "true",
            "sync_interval": "15m",
            "cache_size": "50MB",
            "debug_mode": "false",
        }

    # -- Entry point ---------------------------------------------------------

    def execute(self, command: str) -> CommandOutput:
        # In mock mode pipe commands are not functionally applied; run only
        # the first pipeline segment.
        first_segment = command.split("|")[0].strip()

        if first_segment:
            self._history.append(first_segment)

        name, args, flags = self._parse(first_segment)
        if not name:
            return CommandOutput(success=True, exit_code=0, lines=[])

        dispatch: dict[str, object] = {
            # util
            "help": self._mock_help,
            "?": self._mock_help,
            "h": self._mock_help,
            "man": self._mock_help,
            "clear": self._mock_clear,
            "cls": self._mock_clear,
            "clr": self._mock_clear,
            "echo": self._mock_echo,
            "print": self._mock_echo,
            "history": self._mock_history,
            "hist": self._mock_history,
            "alias": self._mock_alias,
            "config": self._mock_config,
            "cfg": self._mock_config,
            "settings": self._mock_config,
            # user
            "whoami": self._mock_whoami,
            "user": self._mock_whoami,
            "my": self._mock_my,
            "mine": self._mock_my,
            # system
            "ping": self._mock_ping,
            "p": self._mock_ping,
            "cache": self._mock_cache,
            "sync-cache": self._mock_cache,
            "sync": self._mock_sync,
            # pipe
            "grep": self._mock_grep,
            "filter": self._mock_grep,
            "sort": self._mock_sort,
            "head": self._mock_head,
            "tail": self._mock_tail,
            "count": self._mock_count,
            "wc": self._mock_count,
            "log": self._mock_log,
            "logs": self._mock_log,
            # admin
            "ban": self._mock_ban,
            "unban": self._mock_unban,
            "role": self._mock_role,
            "perm": self._mock_perm,
            "permission": self._mock_perm,
            "userinfo": self._mock_userinfo,
            "uinfo": self._mock_userinfo,
            "del": self._mock_del,
            "delete": self._mock_del,
            "quizinfo": self._mock_quizinfo,
            "qi": self._mock_quizinfo,
            "publish": self._mock_publish,
            "pub": self._mock_publish,
            "unpublish": self._mock_unpublish,
            "unpub": self._mock_unpublish,
            "restore": self._mock_restore,
            "ls": self._mock_ls,
            "list": self._mock_ls,
            "stats": self._mock_stats,
            "statistics": self._mock_stats,
            "search": self._mock_search,
            "find": self._mock_search,
            "s": self._mock_search,
            "export": self._mock_export,
            "exp": self._mock_export,
            "purge": self._mock_purge,
            "cleanup": self._mock_purge,
        }

        # Check user-defined aliases before failing
        if name not in dispatch and name in self._aliases:
            expanded = self._aliases[name]
            if args:
                expanded += " " + " ".join(args)
            return self.execute(expanded)

        handler = dispatch.get(name)
        if handler is None:
            return _err(
                f"Lenh khong tim thay: '{name}'. Go 'help' de xem danh sach lenh."
            )

        return handler(args, flags)  # type: ignore[operator]

    # -- Tokenizer -----------------------------------------------------------

    @staticmethod
    def _parse(
        command: str,
    ) -> tuple[str, list[str], dict[str, str | None]]:
        """Minimal tokeniser: returns (name, positional_args, flags)."""
        tokens = command.split()
        if not tokens:
            return "", [], {}

        name = tokens[0].lower()
        args: list[str] = []
        flags: dict[str, str | None] = {}
        i = 1
        while i < len(tokens):
            tok = tokens[i]
            if tok.startswith("--"):
                key = tok[2:]
                if i + 1 < len(tokens) and not tokens[i + 1].startswith("-"):
                    flags[key] = tokens[i + 1]
                    i += 2
                else:
                    flags[key] = None
                    i += 1
            elif tok.startswith("-") and len(tok) == 2 and tok != "--":
                flags[tok[1:]] = None
                i += 1
            else:
                args.append(tok)
                i += 1

        return name, args, flags

    # -------------------------------------------------------------------------
    # Handlers -- util
    # -------------------------------------------------------------------------

    def _mock_help(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if args:
            return _ok(f"Dung 'get_command_help {args[0]}' de xem huong dan chi tiet.")
        lines = [
            OutputLine("=== DANH SACH LENH ===", "HEADER"),
            OutputLine(""),
            OutputLine("[util]", "INFO"),
            OutputLine("  clear (cls, clr)    Xoa man hinh console"),
            OutputLine("  echo (print)        In van ban ra console"),
            OutputLine("  history (hist)      Hien thi lich su lenh"),
            OutputLine("  alias               Quan ly bi danh lenh"),
            OutputLine("  help (?, h, man)    Hien thi danh sach lenh"),
            OutputLine("  config (cfg)        Quan ly cau hinh"),
            OutputLine(""),
            OutputLine("[user]", "INFO"),
            OutputLine("  whoami (user)       Thong tin nguoi dung"),
            OutputLine("  my (mine)           Xem du lieu ca nhan"),
            OutputLine(""),
            OutputLine("[system]", "INFO"),
            OutputLine("  ping (p)            Kiem tra ket noi"),
            OutputLine("  cache               Quan ly bo nho dem"),
            OutputLine("  sync                Quan ly dong bo"),
            OutputLine(""),
            OutputLine("[pipe]", "INFO"),
            OutputLine("  grep (filter)       Loc dong theo mau"),
            OutputLine("  sort                Sap xep dong"),
            OutputLine("  head / tail         Lay dong dau/cuoi"),
            OutputLine("  count (wc)          Dem dong/tu/ky tu"),
            OutputLine("  log (logs)          Xem nhat ky"),
            OutputLine(""),
            OutputLine("[admin]", "INFO"),
            OutputLine("  ban / unban         Cam/go cam nguoi dung"),
            OutputLine("  role                Thay doi vai tro"),
            OutputLine("  perm                Quan ly quyen han"),
            OutputLine("  del (delete)        Xoa doi tuong"),
            OutputLine("  ls (list)           Liet ke doi tuong"),
            OutputLine("  stats               Thong ke he thong"),
            OutputLine("  search (find, s)    Tim kiem"),
            OutputLine("  export (exp)        Xuat du lieu"),
            OutputLine("  purge (cleanup)     Don dep du lieu"),
            OutputLine(""),
            OutputLine("Tong cong: 32 lenh. Go 'help <lenh>' de xem chi tiet."),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_clear(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        return CommandOutput(success=True, exit_code=0, lines=[])

    def _mock_echo(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        return _ok(" ".join(args))

    def _mock_history(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "clear" in flags:
            self._history.clear()
            return _ok("Da xoa lich su lenh.")

        history = list(self._history)
        if "unique" in flags:
            seen: set[str] = set()
            unique = []
            for h in history:
                if h not in seen:
                    seen.add(h)
                    unique.append(h)
            history = unique

        if "reverse" in flags:
            history = list(reversed(history))

        limit = int(args[0]) if args and args[0].isdigit() else None
        if limit:
            history = history[-limit:]

        if not history:
            return _ok("Chua co lich su lenh nao.")

        numbered = "no-numbered" not in flags
        lines: list[OutputLine] = [OutputLine("=== LICH SU LENH ===", "HEADER")]
        for i, cmd in enumerate(history, start=1):
            text = f"  {i:4d}  {cmd}" if numbered else f"  {cmd}"
            lines.append(OutputLine(text))
        lines.append(OutputLine(f"Tong cong: {len(self._history)} lenh"))
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_alias(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "clear" in flags:
            self._aliases.clear()
            return _ok("Da xoa tat ca bi danh.")

        if "remove" in flags:
            name = flags["remove"]
            if name and name in self._aliases:
                del self._aliases[name]
                return _ok(f"Da xoa bi danh '{name}'.")
            return _err(f"Bi danh '{name}' khong ton tai.")

        # alias name=value
        if args:
            raw = " ".join(args)
            eq = raw.find("=")
            if eq > 0:
                alias_name = raw[:eq].strip()
                alias_val = raw[eq + 1 :].strip()
                self._aliases[alias_name] = alias_val
                return _ok(f"Da tao bi danh: {alias_name} = {alias_val}")
            return _err("Cu phap sai. Dung: alias <ten>=<gia_tri>")

        # list aliases
        if not self._aliases:
            return _ok("Chua co bi danh nao.")
        lines: list[OutputLine] = [OutputLine("=== BI DANH ===", "HEADER")]
        for k, v in self._aliases.items():
            lines.append(OutputLine(f"  {k} = {v}"))
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_config(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        sub = args[0].lower() if args else "list"

        if sub == "list":
            lines: list[OutputLine] = [OutputLine("=== CAU HINH ===", "HEADER")]
            for k, v in self._config.items():
                lines.append(OutputLine(f"  {k:<16} = {v}"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "get":
            key = args[1] if len(args) > 1 else ""
            if key in self._config:
                return _ok(f"{key} = {self._config[key]}")
            return _err(f"Khoa cau hinh khong tim thay: '{key}'")

        if sub == "set":
            if len(args) < 3:
                return _err("Cu phap: config set <key> <value>")
            key, val = args[1], args[2]
            self._config[key] = val
            return _ok(f"Da dat {key} = {val}", style="SUCCESS")

        if sub == "reset":
            key = args[1] if len(args) > 1 else ""
            defaults = {
                "theme": "light",
                "language": "vi",
                "notifications": "true",
                "auto_sync": "true",
                "sync_interval": "15m",
                "cache_size": "50MB",
                "debug_mode": "false",
            }
            if key in defaults:
                self._config[key] = defaults[key]
                return _ok(f"Da dat lai {key} = {defaults[key]}")
            return _err(f"Khoa khong hop le: '{key}'")

        if sub == "export":
            return _ok(json.dumps(self._config, ensure_ascii=False))

        return _err(
            f"Tham so khong hop le: '{sub}'. Dung: list | get | set | reset | export"
        )

    # -------------------------------------------------------------------------
    # Handlers -- user
    # -------------------------------------------------------------------------

    def _mock_whoami(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        fmt = flags.get("format", "")
        if fmt == "json":
            data = {
                "id": "user_mock_001",
                "email": "demo@quizzez.app",
                "displayName": "Demo User",
                "role": "USER",
                "isBanned": False,
                "createdAt": "2024-01-15T08:30:00Z",
            }
            return _ok(json.dumps(data, indent=2, ensure_ascii=False))
        lines = [
            OutputLine("=== THONG TIN NGUOI DUNG ===", "HEADER"),
            OutputLine("  ID:         user_mock_001"),
            OutputLine("  Email:      demo@quizzez.app"),
            OutputLine("  Ten:        Demo User"),
            OutputLine("  Vai tro:    USER"),
            OutputLine("  Trang thai: Hoat dong", "SUCCESS"),
            OutputLine("  Tao luc:    15/01/2024 08:30"),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_my(self, args: list[str], flags: dict[str, str | None]) -> CommandOutput:
        sub = args[0].lower() if args else ""

        if sub == "stats":
            lines = [
                OutputLine("=== THONG KE CA NHAN ===", "HEADER"),
                OutputLine("  Quiz da tao:        3"),
                OutputLine("  Lan lam bai:        12"),
                OutputLine("  Diem trung binh:    78.5%"),
                OutputLine("  Sao trung binh:     3.2 / 5"),
                OutputLine("  Chuoi lam bai:      5 ngay"),
                OutputLine("  Thoi gian TB:       06:15"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "quizzes":
            limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 99
            quizzes = [
                ("Lap trinh Kotlin co ban", "10 cau", "Cong khai", "245 luot"),
                ("Android Jetpack Compose", "15 cau", "Rieng tu", "89 luot"),
                ("Thuat toan va CTDL", "20 cau", "Nhap", "0 luot"),
            ][:limit]
            lines: list[OutputLine] = [
                OutputLine("=== BAI KIEM TRA CUA BAN ===", "HEADER")
            ]
            for i, (title, q, status, plays) in enumerate(quizzes, 1):
                lines.append(
                    OutputLine(f"  {i}. {title:<30} | {q} | {status:<10} | {plays}")
                )
            lines.append(OutputLine(""))
            lines.append(OutputLine(f"Tong cong: {len(quizzes)} bai kiem tra"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "attempts":
            limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 99
            attempts = [
                ("Kotlin co ban", "8/10 (80%)", "***", "05:23", "14/03/2025"),
                ("Android Compose", "12/15 (80%)", "***", "08:45", "13/03/2025"),
                ("Lich su Viet Nam", "7/10 (70%)", "**", "04:12", "12/03/2025"),
            ][:limit]
            lines = [OutputLine("=== LAN LAM BAI GAN DAY ===", "HEADER")]
            for i, (quiz, score, stars, t, date) in enumerate(attempts, 1):
                lines.append(
                    OutputLine(
                        f"  {i}. {quiz:<20} | {score:<12} | {stars:<5} | {t} | {date}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(OutputLine(f"Tong cong: {len(attempts)} lan lam bai"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "pool":
            lines = [OutputLine("=== CAU HOI DONG GOP ===", "HEADER")]
            for i, p in enumerate(self._POOL, 1):
                lines.append(OutputLine(f"  {i}. [{p['tag'].upper()}] {p['content']}"))
            lines.append(OutputLine(""))
            lines.append(OutputLine(f"Tong cong: {len(self._POOL)} cau hoi trong pool"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        return _err("Cu phap: my <quizzes|attempts|stats|pool> [--limit <so>]")

    # -------------------------------------------------------------------------
    # Handlers -- system
    # -------------------------------------------------------------------------

    def _mock_ping(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        count = int(flags["count"]) if "count" in flags and flags["count"] else 3
        service = flags.get("service", "")
        service_label = f" ({service})" if service else ""

        latencies = [round(random.uniform(15.0, 120.0), 1) for _ in range(count)]
        lines: list[OutputLine] = [
            OutputLine(f"Dang kiem tra ket noi{service_label}...")
        ]
        for i, ms in enumerate(latencies, 1):
            lines.append(OutputLine(f"  Lan {i}: {ms}ms"))

        mn = min(latencies)
        avg = round(sum(latencies) / len(latencies), 1)
        mx = max(latencies)
        lines.append(OutputLine(""))
        lines.append(OutputLine(f"  Min: {mn}ms | Trung binh: {avg}ms | Max: {mx}ms"))
        lines.append(OutputLine("Ket noi thanh cong!", "SUCCESS"))
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_cache(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        sub = args[0].lower() if args else "status"

        if sub == "status":
            lines = [
                OutputLine("=== TRANG THAI BO NHO DEM ===", "HEADER"),
                OutputLine("  Trang thai:       IDLE", "SUCCESS"),
                OutputLine("  Muc cho xu ly:    0"),
                OutputLine("  Muc that bai:     0"),
                OutputLine("  Lan dong bo cuoi: 14/03/2025 10:25"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "clear":
            return _ok("Da xoa bo nho dem.", style="SUCCESS")

        if sub == "sync":
            lines = [
                OutputLine("Dang dong bo bo nho dem..."),
                OutputLine("Dong bo hoan tat.", "SUCCESS"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "retry":
            return _ok("Khong co thao tac nao can thu lai.", style="SUCCESS")

        return _err(
            f"Tham so khong hop le: '{sub}'. Dung: status | clear | sync | retry"
        )

    def _mock_sync(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        sub = args[0].lower() if args else "status"

        if sub == "status":
            lines = [
                OutputLine("=== TRANG THAI DONG BO ===", "HEADER"),
                OutputLine("  Trang thai:       IDLE", "SUCCESS"),
                OutputLine("  Ket noi mang:     Online (WiFi)", "SUCCESS"),
                OutputLine("  Thao tac cho:     0"),
                OutputLine("  Thao tac loi:     0"),
                OutputLine("  Dong bo cuoi:     14/03/2025 10:25"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "now":
            lines = [
                OutputLine("Bat dau dong bo..."),
                OutputLine("  Day 2 muc len may chu..."),
                OutputLine("  Keo 0 muc tu may chu..."),
                OutputLine("Dong bo hoan tat thanh cong!", "SUCCESS"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "push":
            return _ok("Da day du lieu local len may chu.", style="SUCCESS")

        if sub == "pull":
            return _ok("Da keo du lieu tu may chu ve.", style="SUCCESS")

        if sub == "retry":
            return _ok("Khong co thao tac nao can thu lai.", style="SUCCESS")

        return _err(
            f"Tham so khong hop le: '{sub}'. Dung: now | status | push | pull | retry"
        )

    # -------------------------------------------------------------------------
    # Handlers -- pipe
    # -------------------------------------------------------------------------

    def _mock_grep(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        return _ok(
            "[Mock] grep: loc du lieu theo mau. "
            "Ket qua day du chi co khi chay tren thiet bi that."
        )

    def _mock_sort(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        return _ok(
            "[Mock] sort: sap xep du lieu. "
            "Ket qua day du chi co khi chay tren thiet bi that."
        )

    def _mock_head(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        n = args[0] if args else "10"
        return _ok(
            f"[Mock] head {n}: lay {n} dong dau. "
            "Ket qua day du chi co khi chay tren thiet bi that."
        )

    def _mock_tail(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        n = args[0] if args else "10"
        return _ok(
            f"[Mock] tail {n}: lay {n} dong cuoi. "
            "Ket qua day du chi co khi chay tren thiet bi that."
        )

    def _mock_count(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        return _ok(
            "[Mock] count: dem dong/tu/ky tu. "
            "Ket qua day du chi co khi chay tren thiet bi that."
        )

    def _mock_log(self, args: list[str], flags: dict[str, str | None]) -> CommandOutput:
        level_filter = (flags.get("level") or "").lower()
        tag_filter = (flags.get("tag") or "").lower()
        regex_filter = flags.get("regex", "")
        limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 50

        _level_style = {"I": "NORMAL", "D": "MUTED", "W": "WARNING", "E": "ERROR"}

        entries = self._LOG_ENTRIES
        if level_filter:
            lvl_map = {
                "info": "I",
                "debug": "D",
                "warn": "W",
                "warning": "W",
                "error": "E",
            }
            code = lvl_map.get(level_filter, level_filter.upper()[0])
            entries = [e for e in entries if e[1] == code]
        if tag_filter:
            entries = [e for e in entries if tag_filter in e[2].lower()]
        if regex_filter:
            try:
                pat = re.compile(regex_filter, re.IGNORECASE)
                entries = [e for e in entries if pat.search(e[3])]
            except re.error:
                pass

        entries = entries[:limit]

        lines: list[OutputLine] = [OutputLine("=== NHAT KY UNG DUNG ===", "HEADER")]
        for ts, lvl, tag, msg in entries:
            style = _level_style.get(lvl, "NORMAL")
            lines.append(OutputLine(f"  {ts} {lvl}/{tag}: {msg}", style))
        lines.append(OutputLine(f"Hien thi {len(entries)} dong"))
        return CommandOutput(success=True, exit_code=0, lines=lines)

    # -------------------------------------------------------------------------
    # Handlers -- admin
    # -------------------------------------------------------------------------

    def _mock_ban(self, args: list[str], flags: dict[str, str | None]) -> CommandOutput:
        dry_run = "dry-run" in flags
        confirm = "confirm" in flags
        role = flags.get("role", "")

        if not confirm and not dry_run:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")

        target = args[0] if args else (f"vai tro {role}" if role else "?")
        prefix = "[DRY-RUN] " if dry_run else ""
        lines = [
            OutputLine(f"{prefix}Se cam: {target}", "WARNING"),
            OutputLine(
                "  Khong co thay doi nao duoc thuc hien."
                if dry_run
                else f"Da cam {target} thanh cong.",
                "SUCCESS" if not dry_run else "NORMAL",
            ),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_unban(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "confirm" not in flags:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")
        target = args[0] if args else "?"
        return _ok(f"Da go cam {target} thanh cong.", style="SUCCESS")

    def _mock_role(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "confirm" not in flags:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")
        if len(args) < 2:
            return _err("Cu phap: role <email|id> <vai_tro> --confirm")
        target, new_role = args[0], args[1].upper()
        return _ok(
            f"Da thay doi vai tro cua {target} thanh {new_role}.", style="SUCCESS"
        )

    def _mock_perm(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        sub = args[0].lower() if args else "list"

        if sub == "list":
            lines: list[OutputLine] = [
                OutputLine("=== QUYEN HAN HE THONG ===", "HEADER")
            ]
            for perm, desc in self._PERMISSIONS:
                lines.append(OutputLine(f"  {perm:<22} {desc}"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub == "show":
            target = args[1] if len(args) > 1 else "admin@quizzez.app"
            lines = [OutputLine(f"=== QUYEN CUA {target} ===", "HEADER")]
            for perm, _ in self._PERMISSIONS:
                has = perm in self._ADMIN_PERMS
                mark = "[x]" if has else "[ ]"
                style = "SUCCESS" if has else "NORMAL"
                lines.append(OutputLine(f"  {mark} {perm}", style))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if sub in ("grant", "revoke"):
            if "confirm" not in flags:
                return _err("Lenh huy diet! Them --confirm de xac nhan.")
            if len(args) < 3:
                return _err(f"Cu phap: perm {sub} <email|id> <quyen> --confirm")
            target, perm_name = args[1], args[2].upper()
            action = "Cap" if sub == "grant" else "Thu hoi"
            return _ok(
                f"Da {action.lower()} quyen {perm_name} cho {target}.",
                style="SUCCESS",
            )

        return _err(
            f"Tham so khong hop le: '{sub}'. Dung: list | show | grant | revoke"
        )

    def _mock_userinfo(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        target = args[0] if args else "demo@quizzez.app"
        fmt = flags.get("format", "")

        # Find the matching mock user (fall back to first user if not found)
        user = next(
            (u for u in self._USERS if u["email"] == target or u["id"] == target),
            self._USERS[1],
        )

        if fmt == "json":
            data = {
                "id": user["id"],
                "email": user["email"],
                "displayName": user["name"],
                "role": user["role"],
                "status": user["status"],
                "createdAt": "2024-01-01T12:00:00Z",
                "lastLogin": "2025-03-14T09:30:00Z",
                "quizCount": 5,
                "attemptCount": 23,
                "averageScore": 82.3,
            }
            return _ok(json.dumps(data, indent=2, ensure_ascii=False))

        lines = [
            OutputLine(f"=== THONG TIN NGUOI DUNG: {target} ===", "HEADER"),
            OutputLine(f"  ID:              {user['id']}"),
            OutputLine(f"  Email:           {user['email']}"),
            OutputLine(f"  Ten hien thi:    {user['name']}"),
            OutputLine(f"  Vai tro:         {user['role']}"),
            OutputLine(
                f"  Trang thai:      {user['status']}",
                "SUCCESS" if user["status"] == "Hoat dong" else "WARNING",
            ),
            OutputLine("  Tao luc:         01/01/2024 12:00"),
            OutputLine("  Dang nhap cuoi:  14/03/2025 09:30"),
            OutputLine("  So quiz:         5"),
            OutputLine("  So lan lam bai:  23"),
            OutputLine("  Diem TB:         82.3%"),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_del(self, args: list[str], flags: dict[str, str | None]) -> CommandOutput:
        dry_run = "dry-run" in flags
        confirm = "confirm" in flags

        if not confirm and not dry_run:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")

        # Determine object type
        if "u" in flags:
            obj_type = "nguoi dung"
        elif "q" in flags:
            obj_type = "bai kiem tra"
        elif "a" in flags:
            obj_type = "lan lam bai"
        elif "p" in flags:
            obj_type = "cau hoi pool"
        else:
            return _err("Phai chi dinh loai doi tuong: -u | -q | -a | -p")

        target = args[0] if args else "?"
        prefix = "[DRY-RUN] " if dry_run else ""
        lines = [
            OutputLine(f"{prefix}Se xoa {obj_type}: {target}", "WARNING"),
            OutputLine(
                "  Khong co thay doi nao duoc thuc hien."
                if dry_run
                else f"Da xoa {obj_type} '{target}' thanh cong.",
                "NORMAL" if dry_run else "SUCCESS",
            ),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_quizinfo(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        quiz_id = args[0] if args else "quiz_001"
        quiz = next(
            (q for q in self._QUIZZES if q["id"] == quiz_id),
            self._QUIZZES[0],
        )
        lines: list[OutputLine] = [
            OutputLine(f"=== THONG TIN QUIZ: {quiz_id} ===", "HEADER"),
            OutputLine(f"  Tieu de:       Lap trinh {quiz['title']}"),
            OutputLine(f"  Tac gia:       demo@quizzez.app"),
            OutputLine(
                f"  Trang thai:    {quiz['status']}",
                "SUCCESS" if quiz["status"] == "Cong khai" else "NORMAL",
            ),
            OutputLine(f"  So cau hoi:    {quiz['questions']}"),
            OutputLine(f"  So luot lam:   {quiz['plays']}"),
            OutputLine("  Diem TB:       75.2%"),
            OutputLine("  Tags:          kotlin, android, co-ban"),
            OutputLine("  Tao luc:       15/01/2024 08:30"),
            OutputLine("  Cap nhat:      01/03/2025 14:00"),
        ]
        if "questions" in flags:
            lines += [
                OutputLine(""),
                OutputLine("  --- Cau hoi ---", "INFO"),
                OutputLine("  1. Kotlin la gi? (4 lua chon)"),
                OutputLine("  2. val vs var? (4 lua chon)"),
                OutputLine("  3. Null safety la gi? (4 lua chon)"),
                OutputLine(f"  ... va {quiz['questions'] - 3} cau hoi khac"),
            ]
        if "stats" in flags:
            lines += [
                OutputLine(""),
                OutputLine("  --- Thong ke ---", "INFO"),
                OutputLine("  Diem cao nhat:  100% (user_xyz)"),
                OutputLine("  Diem thap nhat: 30%"),
                OutputLine("  Thoi gian TB:   05:30"),
            ]
        if "attempts" in flags:
            lines += [
                OutputLine(""),
                OutputLine("  --- Lan lam bai ---", "INFO"),
            ]
            for a in self._ATTEMPTS:
                lines.append(
                    OutputLine(
                        f"  {a['id']}  {a['user']:<20} {a['score']}  {a['date']}"
                    )
                )
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_publish(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "confirm" not in flags:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")
        quiz_id = args[0] if args else "?"
        return _ok(f"Da xuat ban quiz '{quiz_id}' thanh cong.", style="SUCCESS")

    def _mock_unpublish(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "confirm" not in flags:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")
        quiz_id = args[0] if args else "?"
        return _ok(f"Da huy xuat ban quiz '{quiz_id}'.", style="SUCCESS")

    def _mock_restore(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        if "confirm" not in flags:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")
        quiz_id = args[0] if args else "?"
        return _ok(f"Da khoi phuc quiz '{quiz_id}' thanh cong.", style="SUCCESS")

    def _mock_ls(self, args: list[str], flags: dict[str, str | None]) -> CommandOutput:
        fmt = flags.get("format", "")
        limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 999

        if "u" in flags:
            role_filter = (flags.get("filter") or "").lower()
            users = self._USERS
            if "role=" in role_filter:
                wanted = role_filter.split("role=")[-1].upper()
                users = [u for u in users if u["role"] == wanted]
            users = users[:limit]
            if fmt == "json":
                return _ok(json.dumps(users, indent=2, ensure_ascii=False))
            lines: list[OutputLine] = [
                OutputLine("=== DANH SACH NGUOI DUNG ===", "HEADER"),
                OutputLine(
                    f"  {'ID':<16} {'Email':<25} {'Vai tro':<10} Trang thai",
                    "TABLE_HEADER",
                ),
            ]
            for u in users:
                style = "WARNING" if u["status"] == "Bi cam" else "NORMAL"
                lines.append(
                    OutputLine(
                        f"  {u['id']:<16} {u['email']:<25} {u['role']:<10} {u['status']}",
                        style,
                    )
                )
            lines.append(OutputLine(""))
            lines.append(
                OutputLine(f"Hien thi {len(users)} / {len(self._USERS)} nguoi dung")
            )
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if "q" in flags:
            quizzes = self._QUIZZES[:limit]
            if fmt == "json":
                return _ok(json.dumps(quizzes, indent=2, ensure_ascii=False))
            lines = [
                OutputLine("=== DANH SACH BAI KIEM TRA ===", "HEADER"),
                OutputLine(
                    f"  {'ID':<16} {'Tieu de':<25} {'Tac gia':<14} Trang thai",
                    "TABLE_HEADER",
                ),
            ]
            for q in quizzes:
                lines.append(
                    OutputLine(
                        f"  {q['id']:<16} {q['title']:<25} {q['author']:<14} {q['status']}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(
                OutputLine(
                    f"Hien thi {len(quizzes)} / {len(self._QUIZZES)} bai kiem tra"
                )
            )
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if "a" in flags:
            attempts = self._ATTEMPTS[:limit]
            if fmt == "json":
                return _ok(json.dumps(attempts, indent=2, ensure_ascii=False))
            lines = [
                OutputLine("=== DANH SACH LAN LAM BAI ===", "HEADER"),
                OutputLine(
                    f"  {'ID':<16} {'Quiz':<20} {'Nguoi lam':<14} {'Diem':<8} Ngay",
                    "TABLE_HEADER",
                ),
            ]
            for a in attempts:
                lines.append(
                    OutputLine(
                        f"  {a['id']:<16} {a['quiz']:<20} {a['user']:<14} {a['score']:<8} {a['date']}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(
                OutputLine(
                    f"Hien thi {len(attempts)} / {len(self._ATTEMPTS)} lan lam bai"
                )
            )
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if "p" in flags:
            pool = self._POOL[:limit]
            if fmt == "json":
                return _ok(json.dumps(pool, indent=2, ensure_ascii=False))
            lines = [
                OutputLine("=== DANH SACH POOL ===", "HEADER"),
                OutputLine(
                    f"  {'ID':<16} {'Noi dung':<32} {'Tag':<10} Tac gia",
                    "TABLE_HEADER",
                ),
            ]
            for p in pool:
                lines.append(
                    OutputLine(
                        f"  {p['id']:<16} {p['content']:<32} {p['tag']:<10} {p['author']}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(
                OutputLine(f"Hien thi {len(pool)} / {len(self._POOL)} cau hoi pool")
            )
            return CommandOutput(success=True, exit_code=0, lines=lines)

        return _err(
            "Phai chi dinh loai doi tuong: -u (users) | -q (quizzes) | -a (attempts) | -p (pool)"
        )

    def _mock_stats(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        fmt = flags.get("format", "")
        data = {
            "totalUsers": 156,
            "totalQuizzes": 89,
            "totalAttempts": 1234,
            "totalPoolItems": 45,
            "activeUsersToday": 23,
            "averageScore": 74.5,
        }
        if fmt == "json":
            return _ok(json.dumps(data, indent=2, ensure_ascii=False))
        lines = [
            OutputLine("=== THONG KE HE THONG ===", "HEADER"),
            OutputLine(f"  Tong nguoi dung:       {data['totalUsers']}"),
            OutputLine(
                f"  Nguoi dung hoat dong:  {data['activeUsersToday']} (hom nay)"
            ),
            OutputLine(f"  Tong bai kiem tra:     {data['totalQuizzes']}"),
            OutputLine(f"  Tong lan lam bai:      {data['totalAttempts']:,}"),
            OutputLine(f"  Cau hoi pool:          {data['totalPoolItems']}"),
            OutputLine(f"  Diem trung binh:       {data['averageScore']}%"),
            OutputLine("  Nguoi dung bi cam:     3", "WARNING"),
            OutputLine("  Quiz nhap:             12", "WARNING"),
            OutputLine("  Quiz cong khai:        65", "SUCCESS"),
            OutputLine("  Quiz rieng tu:         12"),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_search(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        keyword = args[0].lower() if args else ""
        limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 999
        fmt = flags.get("format", "")

        if "u" in flags:
            results = [
                u
                for u in self._USERS
                if keyword in u["email"].lower() or keyword in u["name"].lower()
            ][:limit]
            if fmt == "json":
                return _ok(json.dumps(results, indent=2, ensure_ascii=False))
            lines: list[OutputLine] = [
                OutputLine(
                    f"=== KET QUA TIM KIEM NGUOI DUNG: '{keyword}' ===", "HEADER"
                )
            ]
            for u in results:
                lines.append(
                    OutputLine(
                        f"  {u['id']:<10} {u['email']:<25} {u['name']:<15} {u['role']}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(OutputLine(f"Tim thay {len(results)} ket qua"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if "q" in flags:
            results_q = [
                q
                for q in self._QUIZZES
                if keyword in q["title"].lower() or keyword in q["author"].lower()
            ][:limit]
            if fmt == "json":
                return _ok(json.dumps(results_q, indent=2, ensure_ascii=False))
            lines = [
                OutputLine(f"=== KET QUA TIM KIEM QUIZ: '{keyword}' ===", "HEADER")
            ]
            for q in results_q:
                lines.append(
                    OutputLine(
                        f"  {q['id']:<10} {q['title']:<28} {q['questions']} cau    {q['status']}"
                    )
                )
            lines.append(OutputLine(""))
            lines.append(OutputLine(f"Tim thay {len(results_q)} ket qua"))
            return CommandOutput(success=True, exit_code=0, lines=lines)

        return _err("Phai chi dinh loai doi tuong: -u (users) | -q (quizzes)")

    def _mock_export(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        sub = args[0].lower() if args else "stats"
        fmt = (flags.get("format") or "table").lower()
        limit = int(flags["limit"]) if "limit" in flags and flags["limit"] else 999

        datasets: dict[str, list] = {
            "users": self._USERS,
            "quizzes": self._QUIZZES,
            "attempts": self._ATTEMPTS,
            "stats": [
                {"key": k, "value": v}
                for k, v in {
                    "totalUsers": 156,
                    "totalQuizzes": 89,
                    "totalAttempts": 1234,
                    "averageScore": 74.5,
                }.items()
            ],
            "logs": [
                {"time": e[0], "level": e[1], "tag": e[2], "message": e[3]}
                for e in self._LOG_ENTRIES
            ],
        }

        if sub not in datasets:
            return _err(
                f"Tap du lieu khong hop le: '{sub}'. "
                "Dung: users | quizzes | attempts | stats | logs"
            )

        data = datasets[sub][:limit]
        count = len(data)

        if fmt == "json":
            lines: list[OutputLine] = [
                OutputLine(json.dumps(data, indent=2, ensure_ascii=False), "CODE"),
                OutputLine(f"Da xuat {sub} dang JSON ({count} ban ghi).", "SUCCESS"),
            ]
            return CommandOutput(success=True, exit_code=0, lines=lines)

        if fmt == "csv":
            if data:
                header = ",".join(data[0].keys())
                rows = [",".join(str(v) for v in row.values()) for row in data]
                lines = (
                    [OutputLine(header, "TABLE_HEADER")]
                    + [OutputLine(r, "TABLE_ROW") for r in rows]
                    + [
                        OutputLine(
                            f"Da xuat {sub} dang CSV ({count} ban ghi).", "SUCCESS"
                        )
                    ]
                )
                return CommandOutput(success=True, exit_code=0, lines=lines)

        # default: table
        lines = [
            OutputLine(f"=== XUAT DU LIEU: {sub.upper()} ===", "HEADER"),
            OutputLine(f"  Da xuat {sub} voi {count} ban ghi.", "SUCCESS"),
        ]
        return CommandOutput(success=True, exit_code=0, lines=lines)

    def _mock_purge(
        self, args: list[str], flags: dict[str, str | None]
    ) -> CommandOutput:
        dry_run = "dry-run" in flags
        confirm = "confirm" in flags

        if not confirm and not dry_run:
            return _err("Lenh huy diet! Them --confirm de xac nhan.")

        sub = args[0].lower() if args else "trash"
        valid = {
            "trash",
            "inactive",
            "old-attempts",
            "orphans",
            "banned",
            "empty-quizzes",
        }
        if sub not in valid:
            return _err(
                f"Muc tieu khong hop le: '{sub}'. "
                "Dung: trash | inactive | old-attempts | orphans | banned | empty-quizzes"
            )

        # Deterministic mock counts so repeated dry-runs are consistent
        mock_counts = {
            "trash": 0,
            "inactive": 4,
            "old-attempts": 17,
            "orphans": 15,
            "banned": 3,
            "empty-quizzes": 2,
        }
        count = mock_counts.get(sub, 0)

        prefix = "[DRY-RUN] " if dry_run else ""
        lines: list[OutputLine] = [
            OutputLine(f"{prefix}Don dep '{sub}': {count} muc.", "WARNING"),
        ]
        if dry_run:
            lines.append(OutputLine("  Khong co thay doi nao duoc thuc hien."))
        else:
            lines.append(OutputLine(f"  Da xoa {count} muc thanh cong.", "SUCCESS"))

        return CommandOutput(success=True, exit_code=0, lines=lines)


# ---------------------------------------------------------------------------
# Module-level helpers
# ---------------------------------------------------------------------------


def _ok(
    text_or_lines: "str | list[OutputLine]",
    style: str = "NORMAL",
) -> CommandOutput:
    """Return a successful CommandOutput from a string or a list of OutputLines."""
    if isinstance(text_or_lines, str):
        lines = [OutputLine(text_or_lines, style)]
    else:
        lines = text_or_lines
    return CommandOutput(success=True, exit_code=0, lines=lines)


def _err(message: str, exit_code: int = 1) -> CommandOutput:
    """Return a failed CommandOutput with a single ERROR-styled line."""
    return CommandOutput(
        success=False,
        exit_code=exit_code,
        lines=[OutputLine(message, "ERROR")],
    )
