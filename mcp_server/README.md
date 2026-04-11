# Quizzez Console MCP Server

A [Model Context Protocol (MCP)](https://spec.modelcontextprotocol.io) server
that connects AI agents directly to the Quizzez Android app's built-in console system via ADB.
AI agents can inspect, manage, and automate tasks in the app by calling the 7 registered tools.

---

## Overview

The Quizzez app ships with an in-app console (~32 commands) organized into 5 categories:

| Category   | Commands                                                                          | Description                        |
|------------|-----------------------------------------------------------------------------------|------------------------------------|
| **util**   | `clear`, `echo`, `history`, `alias`, `help`, `config`                            | General utility commands           |
| **user**   | `whoami`, `my`                                                                    | Personal data and user info        |
| **system** | `ping`, `cache`, `sync`                                                           | System and synchronization         |
| **pipe**   | `grep`, `sort`, `head`, `tail`, `count`, `log`                                    | Pipeline-style data processing     |
| **admin**  | `ban`, `unban`, `role`, `perm`, `userinfo`, `del`, `quizinfo`, `publish`, `unpublish`, `restore`, `ls`, `stats`, `search`, `export`, `purge` | Administration |

The MCP server exposes 7 tools to AI agents:

| Tool                  | Purpose                                                      |
|-----------------------|--------------------------------------------------------------|
| `list_commands`       | List available commands, filterable by category or role      |
| `get_command_help`    | Full documentation for a specific command                    |
| `execute_command`     | Execute a command on the device/emulator via ADB             |
| `suggest_command`     | Autocomplete suggestions from partial input                  |
| `validate_command`    | Syntax validation without execution                          |
| `build_command`       | Translate a natural-language description into a command      |
| `get_command_examples`| Usage examples for a command                                 |

---

## How It Works

All command execution goes through a real ADB ordered broadcast:

```
adb shell am broadcast --ordered \
  -a  com.example.androidapp.CONSOLE_COMMAND \
  -n  com.example.androidapp/.ConsoleBroadcastReceiver \
  -e  command "<cmd>"
```

The app's `ConsoleBroadcastReceiver`:
1. Receives the intent and calls `goAsync()`
2. Executes the command via `CommandExecutor`
3. Base64-encodes the JSON result and calls `pendingResult.setResultData(base64Json)`
4. Calls `pendingResult.finish()`

`am broadcast` blocks until `finish()` is called, then prints:
```
Broadcast completed: result=0, data="<base64>"
```

The server decodes the Base64 payload and parses it as:
```json
{
  "success": true,
  "exitCode": 0,
  "output": [
    { "text": "Connection successful!", "style": "SUCCESS" },
    { "text": "Latency: 45ms",          "style": "NORMAL"  }
  ]
}
```
---

## Requirements

- **Python 3.10+**
- **`mcp` package** (MCP Python SDK ≥ 1.0.0)
- **ADB (Android Debug Bridge)** — must be on your `PATH`
- **Android emulator or physical device** running the Quizzez app

---

## Installation

### 1. Create a virtual environment

```bash
cd AndroidApp/mcp_server
python -m venv .venv
source .venv/bin/activate        # Linux/macOS
# .venv\Scripts\activate         # Windows
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Verify the MCP SDK

```bash
python -c "from mcp.server.fastmcp import FastMCP; print('MCP SDK OK')"
```

---

## Running the Server

```bash
# Default: stdio transport, auto-detect any connected device
python server.py

# Target a specific ADB device/emulator serial
python server.py --device emulator-5554
```

The server runs in stdio mode, which is the standard transport for all MCP clients
(Claude Desktop, Zed, VS Code, etc.).

---

## Prerequisites: Getting the App Running

### Step 1 — Start the Android emulator

```bash
# List available AVDs
emulator -list-avds

# Start the emulator
emulator -avd <avd_name>
```

### Step 2 — Verify ADB connection

```bash
adb devices
# Expected output:
# List of devices attached
# emulator-5554    device
```

### Step 3 — Build and install the Quizzez app

```bash
cd AndroidApp
./gradlew installDebug
```

### Step 4 — Open the console in the app

**Profile → Developer Tools → Console**

The console screen does **not** need to be visible for commands to execute — the
`ConsoleBroadcastReceiver` is registered globally. However, the app must be running
in the foreground or background.


## Example Claude Desktop Configuration

Config file locations:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "quizzez-console": {
      "command": "Path to your project directory/AndroidApp/mcp_server/.venv/bin/python",
      "args": [
        "Path to your project directory/AndroidApp/mcp_server/server.py"
      ],
      "env": {
        "PATH": "Path to your home directory/Android/Sdk/platform-tools:/usr/bin:/bin"
      }
    }
  }
}
```

---

## Tool Reference

### `list_commands`

List all available commands, optionally filtered by category or role.

| Parameter  | Type   | Default | Description                                      |
|------------|--------|---------|--------------------------------------------------|
| `category` | string | `""`    | Filter: `util`, `user`, `system`, `pipe`, `admin` |
| `role`     | string | `"USER"`| Role filter: `GUEST`, `USER`, `ADMIN`, `SUPERUSER`|

---

### `get_command_help`

Get full documentation for a specific command.

| Parameter      | Type   | Description                             |
|----------------|--------|-----------------------------------------|
| `command_name` | string | Command name or alias (e.g. `"ban"`)    |

Returns: description, syntax, flags, examples, required role/permission.

---

### `execute_command`

Execute a command on the connected device/emulator via ADB broadcast.

| Parameter | Type   | Description                                        |
|-----------|--------|----------------------------------------------------|
| `command` | string | Full command string (e.g. `"ping --count 3"`)      |

The app must be running. Returns the command output, prefixed with `[ERROR]` on failure.

---

### `suggest_command`

Get autocomplete suggestions from a partial input string.

| Parameter       | Type   | Description                              |
|-----------------|--------|------------------------------------------|
| `partial_input` | string | Partial input (e.g. `"ls -"`, `"ban --"`) |

---

### `validate_command`

Validate command syntax without executing it.

| Parameter | Type   | Description                                        |
|-----------|--------|----------------------------------------------------|
| `command` | string | Full command string to validate                    |

Returns: `VALID` or `INVALID`, plus warnings for destructive commands, missing
arguments, unknown flags, and role requirements.

---

### `build_command`

Translate a natural-language description into a concrete console command.
Supports both English and Vietnamese input.

| Parameter     | Type   | Description                                              |
|---------------|--------|----------------------------------------------------------|
| `description` | string | Natural language description (e.g. `"ban user@ex.com"`) |

Returns: suggested command, syntax, concrete command string, and related alternatives.

---

### `get_command_examples`

Get usage examples for a specific command.

| Parameter      | Type   | Description                               |
|----------------|--------|-------------------------------------------|
| `command_name` | string | Command name or alias (e.g. `"export"`)   |

---

## Troubleshooting

| Symptom                           | Likely Cause                                    | Fix                                                                                         |
|-----------------------------------|-------------------------------------------------|---------------------------------------------------------------------------------------------|
| `[ERROR] adb not found on PATH`   | ADB binary missing or not on PATH               | Set `env.PATH` in the MCP config to include your `platform-tools` directory                 |
| `[ERROR] No device connected`     | Emulator not running or USB not authorized      | Run `adb devices` to confirm. Accept the USB authorization dialog on the device if needed   |
| `[ERROR] No result received from app after 15s` | App not running or receiver not registered | Launch the Quizzez app. Check `adb logcat -s ConsoleBroadcastReceiver` for errors          |
| `Broadcast completed: result=0` with no data | App is running but console not initialized | Navigate to any screen in the app to ensure it is fully initialized                        |
| Tools not appearing in Zed        | Python path or MCP SDK issue                    | Run `<python_path> -c "import mcp; print('OK')"` to verify the SDK is installed            |
| Server crashes on startup         | Import error                                    | Run `python server.py` from the terminal and inspect the stderr output                     |

### Debug mode

```bash
# Run the server manually and capture all stderr logs
python server.py 2>debug.log

# Watch logs in real time
python server.py 2>&1 | tee debug.log

# Inspect the ADB broadcast directly (without the MCP layer)
adb shell am broadcast --ordered \
  -a com.example.androidapp.CONSOLE_COMMAND \
  -n com.example.androidapp/.ConsoleBroadcastReceiver \
  -e command "ping"
```

### Confirm the receiver is registered

```bash
adb shell cmd package dump com.example.androidapp | grep -A2 "ConsoleBroadcastReceiver"
```

---

## Development

### Project structure

```
mcp_server/
  server.py           -- MCP server entry point; 7 tool definitions
  adb_bridge.py       -- ADB communication layer (AdbBridge class)
  command_registry.py -- Complete command metadata for all ~32 commands
  requirements.txt    -- Python dependencies
  README.md           -- This file
```

### Adding a new command

1. Add a `CommandInfo` entry to `command_registry.py` and include it in `ALL_COMMANDS`.
2. Implement the corresponding Kotlin `Command` in `domain/console/commands/` in the Android app.
3. Register the Kotlin command in `CommandRegistry` inside the app.
4. The MCP server picks up the new command automatically via the registry — no server changes needed.

### Running unit tests

```bash
# Verify all imports and command registry integrity
python -c "
from adb_bridge import AdbBridge, CommandOutput, OutputLine
from command_registry import ALL_COMMANDS, format_command_detail
print(f'{len(ALL_COMMANDS)} commands loaded')
b = AdbBridge()
print(f'ADB available: {b.check_adb_available()}')
"
```

---

## Links

- [MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io)
- [Android Debug Bridge (ADB) reference](https://developer.android.com/tools/adb)
