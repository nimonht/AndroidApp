# Quizzez Console MCP Server

MCP (Model Context Protocol) server for the Quizzez Android application's console system. This server allows AI agents (such as Claude, GitHub Copilot, or any MCP-compatible assistant) to interact with the app's ~32 console commands to inspect, manage, and automate tasks.

## Overview

The Quizzez application has a built-in console system with ~32 commands, divided into 5 groups:

| Category | Commands | Description |
|----------|----------|-------------|
| **util** | `clear`, `echo`, `history`, `alias`, `help`, `config` | General utility commands |
| **user** | `whoami`, `my` | Personal information and data |
| **system** | `ping`, `cache`, `sync` | System and synchronization |
| **pipe** | `grep`, `sort`, `head`, `tail`, `count`, `log` | Pipe-style data processing |
| **admin** | `ban`, `unban`, `role`, `perm`, `userinfo`, `del`, `quizinfo`, `publish`, `unpublish`, `restore`, `ls`, `stats`, `search`, `export`, `purge` | System administration |

The MCP server provides 7 tools:

1. **`list_commands`** -- List commands, filter by category or role
2. **`get_command_help`** -- Detailed help for a specific command
3. **`execute_command`** -- Execute a command on a device or mock
4. **`suggest_command`** -- Autocomplete suggestions from partial input
5. **`validate_command`** -- Check syntax without executing
6. **`build_command`** -- Translate natural language into a console command
7. **`get_command_examples`** -- Usage examples for a command

## Requirements

- Python 3.10 or higher
- `mcp` package (MCP Python SDK)
- ADB (Android Debug Bridge) -- only required when executing commands on a real device/emulator

## Installation

### 1. Create a virtual environment (recommended)

```bash
cd AndroidApp/mcp_server
python -m venv .venv
source .venv/bin/activate    # Linux/macOS
# .venv\Scripts\activate     # Windows
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Verify installation

```bash
python -c "from mcp.server.fastmcp import FastMCP; print('MCP SDK OK')"
```

## Running the server

### stdio mode (default, used by all MCP clients)

```bash
python server.py
```

### Mock mode (no Android device required)

```bash
python server.py --mock
```

When running with `--mock`, all `execute_command` calls return realistic mock results in Vietnamese without requiring an ADB connection.

---

## Zed Editor configuration

Zed uses a `settings.json` file for MCP server configuration. You can configure it globally or per-project.

### Option A: Global configuration

Open Zed settings (`Ctrl+,` or `Cmd+,`), or edit `~/.config/zed/settings.json` directly. Add the `context_servers` key:

```json
{
  "context_servers": {
    "quizzez-console": {
      "command": {
        "path": "/home/thanh/School/AndroidApp/mcp_server/.venv/bin/python",
        "args": [
          "/home/thanh/School/AndroidApp/mcp_server/server.py",
          "--mock"
        ]
      }
    }
  }
}
```

### Option B: Per-project configuration

Create or edit `.zed/settings.json` in your project root (`AndroidApp/.zed/settings.json`):

```json
{
  "context_servers": {
    "quizzez-console": {
      "command": {
        "path": "/home/thanh/School/AndroidApp/mcp_server/.venv/bin/python",
        "args": [
          "/home/thanh/School/AndroidApp/mcp_server/server.py",
          "--mock"
        ]
      }
    }
  }
}
```

### Option C: Using system Python (if MCP SDK is installed globally)

If you installed `mcp` into your system Python (not a venv), point directly to it:

```json
{
  "context_servers": {
    "quizzez-console": {
      "command": {
        "path": "python",
        "args": [
          "/home/thanh/School/AndroidApp/mcp_server/server.py",
          "--mock"
        ]
      }
    }
  }
}
```

### Option D: Using uv

If you manage Python with `uv`, you can run the server through it:

```json
{
  "context_servers": {
    "quizzez-console": {
      "command": {
        "path": "uv",
        "args": [
          "run",
          "--directory",
          "/home/thanh/School/AndroidApp/mcp_server",
          "python",
          "server.py",
          "--mock"
        ]
      }
    }
  }
}
```

### Verify it works in Zed

1. Save `settings.json` and restart Zed (or reload the window with `Ctrl+Shift+P` > "workspace: reload").
2. Open the **Assistant Panel** (`Ctrl+Shift+A` / `Cmd+Shift+A`).
3. The Copilot / Claude agent should now see the 7 Quizzez Console tools.
4. Try asking: *"List all available Quizzez console commands"* -- the agent will call `list_commands`.
5. Or ask: *"Execute the ping command in mock mode"* -- it will call `execute_command`.

### Troubleshooting Zed MCP

| Symptom | Fix |
|---------|-----|
| Server not appearing | Verify `path` points to the correct Python binary where `mcp` is installed. Run `<path> -c "import mcp; print('OK')"` to check. |
| Import errors in logs | The venv may not have `mcp` installed. Run `<venv>/bin/pip install mcp` inside it. |
| Tools not showing | Open the Zed command palette (`Ctrl+Shift+P`) and search for "debug: open log" to see MCP server stderr output. |
| Want real device | Remove `"--mock"` from `args` and ensure ADB is on your PATH. |

---

## Claude Desktop configuration

Add to the Claude Desktop configuration file:

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

### Mock mode (no ADB required)

```json
{
  "mcpServers": {
    "quizzez-console": {
      "command": "python",
      "args": [
        "/home/thanh/School/AndroidApp/mcp_server/server.py",
        "--mock"
      ]
    }
  }
}
```

### With virtual environment

```json
{
  "mcpServers": {
    "quizzez-console": {
      "command": "/home/thanh/School/AndroidApp/mcp_server/.venv/bin/python",
      "args": [
        "/home/thanh/School/AndroidApp/mcp_server/server.py",
        "--mock"
      ]
    }
  }
}
```

### With ADB (real device/emulator)

```json
{
  "mcpServers": {
    "quizzez-console": {
      "command": "python",
      "args": [
        "/home/thanh/School/AndroidApp/mcp_server/server.py"
      ],
      "env": {
        "PATH": "/path/to/android-sdk/platform-tools:/usr/bin:/bin"
      }
    }
  }
}
```

---

## Usage with Android Emulator

### Step 1: Start the emulator

```bash
# List available AVDs
emulator -list-avds

# Start the emulator
emulator -avd <avd_name>
```

### Step 2: Verify ADB connection

```bash
adb devices
# Expected output:
# List of devices attached
# emulator-5554    device
```

### Step 3: Install and run the Quizzez app

```bash
cd AndroidApp
./gradlew installDebug
```

### Step 4: Open the console screen in the app

In the Quizzez app: **Profile > Developer Tools > Console**

The console screen must be open to receive commands from the MCP server via ADB broadcast.

### Step 5: Run the MCP server (without the --mock flag)

```bash
python server.py
```

### ADB communication protocol

The server sends commands to the app via broadcast intent:

```
adb shell am broadcast \
  -a com.example.androidapp.CONSOLE_COMMAND \
  -e command "<command>"
```

The app writes results to a JSON file:

```
/data/data/com.example.androidapp/files/console_output.json
```

The server reads the results:

```
adb shell cat /data/data/com.example.androidapp/files/console_output.json
```

Result JSON format:

```json
{
  "success": true,
  "exitCode": 0,
  "output": [
    {"text": "Connection successful!", "style": "SUCCESS"},
    {"text": "Latency: 45ms", "style": "NORMAL"}
  ]
}
```

## Development

### Adding a new command

1. Add a `CommandInfo` to `command_registry.py`
2. Add it to the `ALL_COMMANDS` list
3. Add a mock handler to `MockExecutor._HANDLERS` in `adb_bridge.py`
4. The server automatically detects new commands via the registry

### Quick testing

```bash
# Run the server in mock mode and test by piping JSON via stdin
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | python server.py --mock
```

### Debug

The server logs to stderr. To view detailed logs:

```bash
python server.py --mock 2>debug.log
```

## Current limitations

- **ADB protocol**: The app must be open with the Console screen active to receive broadcast intents. Future versions may use a background service.
- **Pipe commands**: When using the mock executor, pipe commands (grep, sort, head, tail, count) return instructional messages instead of performing actual piping. Pipes only work fully when executing on a real device.
- **Authentication**: The mock executor simulates a user with the USER role. To test admin commands, run on a device with an account that has the appropriate permissions.

## Links

- [MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io)
