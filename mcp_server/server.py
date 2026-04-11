"""
Quizzez Console MCP Server
===========================

A Model Context Protocol (MCP) server that exposes the Quizzez Android app's
in-app console system to AI agents.  Seven tools are registered:

1. list_commands        -- List available commands (filterable by category/role)
2. get_command_help     -- Detailed help for a specific command
3. execute_command      -- Execute a command on device/emulator via ADB
4. suggest_command      -- Autocomplete suggestions for partial input
5. validate_command     -- Syntax validation without execution
6. build_command        -- Natural-language to command translation
7. get_command_examples -- Usage examples for a command

Run with:
    python server.py                          # stdio transport (default, for Claude Desktop)
    python server.py --device emulator-5554   # target a specific ADB device
"""

from __future__ import annotations

import argparse
import logging
import re
import sys
from difflib import SequenceMatcher

from adb_bridge import AdbBridge
from command_registry import (
    ALL_COMMANDS,
    CATEGORIES,
    ROLE_HIERARCHY,
    CommandInfo,
    format_command_detail,
    format_command_table,
    get_command_by_name,
    get_commands_by_category,
    get_commands_for_role,
    role_meets_minimum,
    search_commands,
)
from mcp.server.fastmcp import FastMCP

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
    stream=sys.stderr,
)
logger = logging.getLogger("quizzez-mcp")

# ---------------------------------------------------------------------------
# Server & executor
# ---------------------------------------------------------------------------

mcp = FastMCP(
    "Quizzez Console",
    instructions=(
        "MCP server for the Quizzez Android app console system. "
        "Provides access to ~32 console commands to inspect, manage, and automate "
        "tasks in the app via ADB on a connected device or emulator.\n\n"
        "Available tools:\n"
        "- list_commands: List commands (filter by category/role)\n"
        "- get_command_help: Detailed help for a specific command\n"
        "- execute_command: Execute a command via ADB on a real device/emulator\n"
        "- suggest_command: Autocomplete suggestions for partial input\n"
        "- validate_command: Validate syntax without executing\n"
        "- build_command: Translate natural language to a console command\n"
        "- get_command_examples: View usage examples for a command"
    ),
)

_adb_bridge = AdbBridge()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _format_output(output) -> str:
    """Format a CommandOutput into a plain-text string for the agent."""
    if output.error_message:
        return f"[ERROR] {output.error_message}"
    if not output.lines:
        return "(no output)"
    parts: list[str] = []
    for line in output.lines:
        prefix = ""
        if line.style == "ERROR":
            prefix = "[ERROR] "
        elif line.style == "WARNING":
            prefix = "[WARNING] "
        elif line.style == "SUCCESS":
            prefix = "[OK] "
        parts.append(f"{prefix}{line.text}")
    return "\n".join(parts)


def _parse_command_name(raw: str) -> str:
    """Extract the first token (command name) from a raw command string."""
    return raw.strip().split()[0].lower() if raw.strip() else ""


def _known_flags_for(cmd: CommandInfo) -> set[str]:
    """Return the combined set of recognised long flag names for *cmd*."""
    flags: set[str] = set(cmd.value_flags) | set(cmd.boolean_flags)
    return flags


def _similarity(a: str, b: str) -> float:
    return SequenceMatcher(None, a.lower(), b.lower()).ratio()


# ---------------------------------------------------------------------------
# Tool 1 -- list_commands
# ---------------------------------------------------------------------------


@mcp.tool()
def list_commands(category: str = "", role: str = "USER") -> str:
    """List available console commands, optionally filtered by category or role.

    Args:
        category: Filter by category (util, user, system, pipe, admin). Empty = all.
        role: User role (GUEST, USER, ADMIN, SUPERUSER). Default: USER.

    Returns:
        Table of commands with name, description, and category.
    """
    role = role.upper() if role else "USER"
    if role not in ROLE_HIERARCHY:
        return f"Invalid role: '{role}'. Choose from: {', '.join(ROLE_HIERARCHY)}"

    if category:
        cat = category.lower()
        if cat not in CATEGORIES:
            return (
                f"Invalid category: '{category}'. Choose from: {', '.join(CATEGORIES)}"
            )
        cmds = [
            c
            for c in get_commands_by_category(cat)
            if role_meets_minimum(role, c.minimum_role)
        ]
        header = f"Commands in category '{cat}' (role: {role}):\n\n"
        return header + format_command_table(cmds, show_category=False)

    cmds = get_commands_for_role(role)
    header = f"All available commands (role: {role}):\n\n"

    # Group by category for nicer display.
    sections: list[str] = []
    for cat in CATEGORIES:
        cat_cmds = [c for c in cmds if c.category == cat]
        if cat_cmds:
            section_header = f"[{cat.upper()}]"
            rows: list[str] = []
            for cmd in cat_cmds:
                alias_str = f" ({', '.join(cmd.aliases)})" if cmd.aliases else ""
                rows.append(f"  {cmd.name}{alias_str:<20s} {cmd.description}")
            sections.append(section_header + "\n" + "\n".join(rows))

    footer = f"\nTotal: {len(cmds)} commands. Use 'get_command_help' for details."
    return header + "\n\n".join(sections) + "\n" + footer


# ---------------------------------------------------------------------------
# Tool 2 -- get_command_help
# ---------------------------------------------------------------------------


@mcp.tool()
def get_command_help(command_name: str) -> str:
    """Get detailed help documentation for a specific console command.

    Args:
        command_name: Command name or alias (e.g. "ban", "ls", "h").

    Returns:
        Full documentation: description, syntax, flags, examples, required role.
    """
    cmd = get_command_by_name(command_name)
    if cmd is None:
        # Try fuzzy match
        best: CommandInfo | None = None
        best_score = 0.0
        for c in ALL_COMMANDS:
            score = max(
                _similarity(command_name, c.name),
                max((_similarity(command_name, a) for a in c.aliases), default=0.0),
            )
            if score > best_score:
                best_score = score
                best = c
        if best and best_score > 0.5:
            suggestion = (
                f"Command '{command_name}' not found. Did you mean '{best.name}'?\n\n"
            )
            return suggestion + format_command_detail(best)
        return (
            f"Command '{command_name}' not found. "
            "Use list_commands to see all available commands."
        )
    return format_command_detail(cmd)


# ---------------------------------------------------------------------------
# Tool 3 -- execute_command
# ---------------------------------------------------------------------------


@mcp.tool()
def execute_command(command: str) -> str:
    """Execute a console command on the connected Android device or emulator via ADB.

    The Quizzez app must be running on the device/emulator with the ConsoleBroadcastReceiver
    registered. The command is dispatched via ADB ordered broadcast and the result is
    read from the broadcast return value.

    Args:
        command: Full command string (e.g. "ping --count 3", "ls -u --limit 5").

    Returns:
        Command output (may be multi-line). Prefixed with [ERROR] on failure.
    """
    if not command or not command.strip():
        return "[ERROR] Empty command. Please provide a valid command string."

    logger.info("execute_command: %r", command)

    if not _adb_bridge.check_adb_available():
        return (
            "[ERROR] adb not found on PATH.\n"
            "Install Android SDK platform-tools and ensure adb is on your PATH."
        )

    connected, detail = _adb_bridge.check_device_connected()
    if not connected:
        return f"[ERROR] {detail}\nPlease connect a device or start an emulator."

    result = _adb_bridge.execute(command.strip())
    return _format_output(result)


# ---------------------------------------------------------------------------
# Tool 4 -- suggest_command
# ---------------------------------------------------------------------------


@mcp.tool()
def suggest_command(partial_input: str) -> str:
    """Get autocomplete suggestions for a partial command input.

    Args:
        partial_input: Current input string (e.g. "hel", "ls -", "ban --").

    Returns:
        List of suggestions with descriptions.
    """
    text = partial_input.strip().lower()
    if not text:
        return "Enter at least one character to get suggestions."

    parts = text.split()
    first_token = parts[0]

    # If we're still typing the command name
    cmd = get_command_by_name(first_token)
    if cmd is None or len(parts) == 1 and not partial_input.endswith(" "):
        # Suggest matching command names
        matches: list[tuple[str, str]] = []
        for c in ALL_COMMANDS:
            if c.name.startswith(first_token):
                matches.append((c.name, c.description))
            for alias in c.aliases:
                if alias.lower().startswith(first_token):
                    matches.append((f"{alias} -> {c.name}", c.description))

        if not matches:
            # Fuzzy fallback
            scored = []
            for c in ALL_COMMANDS:
                s = _similarity(first_token, c.name)
                if s > 0.4:
                    scored.append((s, c.name, c.description))
            scored.sort(reverse=True)
            matches = [(name, desc) for _, name, desc in scored[:5]]

        if not matches:
            return f"No commands found starting with '{first_token}'."

        lines = [f"Suggestions for '{partial_input}':", ""]
        for name, desc in matches[:10]:
            lines.append(f"  {name:<20s} {desc}")
        return "\n".join(lines)

    # Command is known; suggest flags/subcommands
    lines = [f"Suggestions for '{partial_input}' (command: {cmd.name}):", ""]

    current_token = parts[-1] if not partial_input.endswith(" ") else ""

    # If typing a flag
    if current_token.startswith("--"):
        prefix = current_token.lstrip("-")
        all_flags = list(cmd.value_flags) + list(cmd.boolean_flags)
        matching = [f for f in all_flags if f.startswith(prefix)]
        if matching:
            for f in matching:
                tag = "<value>" if f in cmd.value_flags else "(boolean)"
                lines.append(f"  --{f}  {tag}")
        else:
            lines.append("  (no matching flags)")
    elif current_token.startswith("-") and len(current_token) <= 2:
        # Short flags -- just list the available single-char flags
        lines.append("  Available flags:")
        for f in cmd.boolean_flags[:8]:
            lines.append(f"    --{f}")
        for f in cmd.value_flags[:8]:
            lines.append(f"    --{f} <value>")
    else:
        # Suggest subcommands / general flags
        # Check for subcommand patterns in usage
        usage_match = re.search(r"<(\w+(?:\|\w+)+)>", cmd.usage)
        if usage_match:
            options = usage_match.group(1).split("|")
            used = set(parts[1:])
            remaining = [o for o in options if o not in used]
            if remaining:
                lines.append("  Arguments:")
                for opt in remaining:
                    lines.append(f"    {opt}")

        if cmd.value_flags or cmd.boolean_flags:
            lines.append("  Available flags:")
            for f in cmd.value_flags:
                lines.append(f"    --{f} <value>")
            for f in cmd.boolean_flags:
                lines.append(f"    --{f}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Tool 5 -- validate_command
# ---------------------------------------------------------------------------


@mcp.tool()
def validate_command(command: str) -> str:
    """Validate command syntax without executing it.

    Args:
        command: Full command string to validate (e.g. "ban user@ex.com --confirm").

    Returns:
        Validation result: whether it is valid, and any issues detected.
    """
    if not command or not command.strip():
        return "INVALID: Empty command."

    text = command.strip()
    parts = text.split()
    cmd_name = parts[0].lower()

    cmd = get_command_by_name(cmd_name)
    if cmd is None:
        suggestions: list[str] = []
        for c in ALL_COMMANDS:
            if _similarity(cmd_name, c.name) > 0.5:
                suggestions.append(c.name)
        msg = f"INVALID: Command '{cmd_name}' not found."
        if suggestions:
            msg += f" Did you mean: {', '.join(suggestions)}?"
        return msg

    issues: list[str] = []
    warnings: list[str] = []

    # Check for unrecognised flags
    known = _known_flags_for(cmd)
    known_short = {"u", "q", "a", "p"}  # common short flags
    for part in parts[1:]:
        if part.startswith("--"):
            flag_name = part.lstrip("-").split("=")[0]
            if flag_name and flag_name not in known:
                issues.append(f"Unrecognized flag: --{flag_name}")
        elif part.startswith("-") and len(part) == 2 and part[1].isalpha():
            short = part[1]
            if short not in known_short and short not in known:
                warnings.append(f"Short flag may be invalid: -{short}")

    # Check destructive commands need --confirm
    if cmd.is_destructive:
        has_confirm = "--confirm" in parts
        has_dry_run = "--dry-run" in parts
        if not has_confirm and not has_dry_run:
            warnings.append(
                "Destructive command! Requires --confirm to execute or --dry-run to preview."
            )

    # Check subcommand requirement patterns
    usage_lower = cmd.usage.lower()
    if "<" in usage_lower:
        # Check if a required positional arg is likely missing
        non_flag_args = [p for p in parts[1:] if not p.startswith("-")]
        # Rough heuristic: usage pattern has required arg markers
        required_markers = re.findall(r"<([^>]+)>", cmd.usage)
        required_positional = [
            m for m in required_markers if "|" in m or not m.startswith("[")
        ]
        if required_positional and not non_flag_args:
            example_args = required_positional[0]
            warnings.append(f"Possibly missing required argument: <{example_args}>")

    # Check role requirement
    if cmd.minimum_role != "USER":
        warnings.append(f"This command requires minimum role: {cmd.minimum_role}")

    if cmd.required_permission:
        warnings.append(f"Requires permission: {cmd.required_permission}")

    # Build result
    lines: list[str] = []
    if not issues:
        lines.append(f"VALID: Syntax for command '{cmd.name}' looks correct.")
    else:
        lines.append(f"INVALID: {len(issues)} error(s) found:")
        for i, issue in enumerate(issues, 1):
            lines.append(f"  {i}. {issue}")

    if warnings:
        lines.append("")
        lines.append(f"Notes ({len(warnings)}):")
        for w in warnings:
            lines.append(f"  - {w}")

    lines.append("")
    lines.append(f"Standard syntax: {cmd.usage}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Tool 6 -- build_command
# ---------------------------------------------------------------------------

# Keyword -> command mapping for natural-language resolution.
# Vietnamese and English keys are both intentional to support bilingual NL input.
_NL_KEYWORDS: dict[str, list[str]] = {
    # Vietnamese keywords
    "xoa": ["del", "clear", "purge"],
    "cam": ["ban"],
    "go cam": ["unban"],
    "liet ke": ["ls"],
    "danh sach": ["ls"],
    "tim": ["search", "grep"],
    "tim kiem": ["search"],
    "loc": ["grep"],
    "thong ke": ["stats"],
    "xuat ban": ["publish"],
    "huy xuat ban": ["unpublish"],
    "khoi phuc": ["restore"],
    "nguoi dung": ["userinfo", "ls", "whoami", "search"],
    "quiz": ["quizinfo", "ls", "search", "my"],
    "bai kiem tra": ["quizinfo", "ls", "my"],
    "lich su": ["history"],
    "cau hinh": ["config"],
    "dong bo": ["sync", "cache"],
    "quyen": ["perm", "role"],
    "vai tro": ["role"],
    "ket noi": ["ping"],
    "nhat ky": ["log"],
    "xuat": ["export"],
    "don dep": ["purge"],
    "dem": ["count"],
    "sap xep": ["sort"],
    "bi danh": ["alias"],
    "in": ["echo"],
    # English keywords
    "delete": ["del", "purge"],
    "remove": ["del"],
    "ban": ["ban"],
    "unban": ["unban"],
    "list": ["ls"],
    "find": ["search", "grep"],
    "search": ["search"],
    "filter": ["grep"],
    "statistics": ["stats"],
    "publish": ["publish"],
    "unpublish": ["unpublish"],
    "restore": ["restore"],
    "user": ["userinfo", "whoami", "ls"],
    "history": ["history"],
    "config": ["config"],
    "settings": ["config"],
    "sync": ["sync"],
    "permission": ["perm"],
    "role": ["role"],
    "ping": ["ping"],
    "connect": ["ping"],
    "log": ["log"],
    "export": ["export"],
    "purge": ["purge"],
    "clean": ["purge"],
    "count": ["count"],
    "sort": ["sort"],
    "alias": ["alias"],
    "echo": ["echo"],
    "print": ["echo"],
    "help": ["help"],
}


@mcp.tool()
def build_command(description: str) -> str:
    """Build a console command from a natural-language description.

    Args:
        description: Description of what you want to do (e.g. "ban user user@ex.com",
                     "view system statistics", "list all quizzes").

    Returns:
        Suggested command with explanation.
    """
    if not description or not description.strip():
        return "Please describe what you want to do."

    text = description.strip().lower()
    words = text.split()

    # Score each command based on keyword hits and description similarity
    scores: dict[str, float] = {}
    for cmd in ALL_COMMANDS:
        score = 0.0

        # Direct name/alias mention
        for w in words:
            if w == cmd.name or w in cmd.aliases:
                score += 10.0

        # Keyword matches
        for keyword, cmd_names in _NL_KEYWORDS.items():
            if keyword in text:
                if cmd.name in cmd_names:
                    score += 5.0

        # Description similarity
        score += _similarity(text, cmd.description) * 3.0

        # Usage similarity
        score += _similarity(text, cmd.usage) * 1.0

        if score > 0:
            scores[cmd.name] = score

    if not scores:
        return (
            "Unable to determine a matching command. "
            "Please be more specific or use list_commands to browse commands."
        )

    # Sort by score descending
    ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    top_name = ranked[0][0]
    top_cmd = get_command_by_name(top_name)
    assert top_cmd is not None

    lines: list[str] = []
    lines.append(f"Suggested command: {top_cmd.name}")
    lines.append(f"Description:       {top_cmd.description}")
    lines.append(f"Syntax:            {top_cmd.usage}")
    lines.append("")

    # Try to build a concrete command from the description
    concrete = _build_concrete(top_cmd, text, words)
    lines.append(f"Concrete command: {concrete}")

    if top_cmd.is_destructive:
        lines.append("")
        lines.append(
            "Note: This command is irreversible. "
            "Add --confirm to execute or --dry-run to preview."
        )

    # Show alternatives if close scores
    if len(ranked) > 1:
        lines.append("")
        lines.append("Related commands:")
        for alt_name, alt_score in ranked[1:4]:
            alt_cmd = get_command_by_name(alt_name)
            if alt_cmd:
                lines.append(f"  - {alt_cmd.name}: {alt_cmd.description}")

    return "\n".join(lines)


def _build_concrete(cmd: CommandInfo, text: str, words: list[str]) -> str:
    """Attempt to construct a concrete command string from an NL description."""
    parts = [cmd.name]

    # Extract email-like tokens
    emails = [w for w in words if "@" in w and "." in w]

    # Extract ID-like tokens (hex strings, long alphanumeric)
    ids = [w for w in words if len(w) > 8 and w.isalnum() and not w.isalpha()]

    # Detect entity-type flags from context.
    # Bilingual word sets are intentional to support Vietnamese and English NL input.
    entity_words = {"nguoi dung", "user", "users"}
    quiz_words = {"quiz", "bai kiem tra", "quizzes"}
    attempt_words = {"attempt", "lan lam bai", "attempts"}
    pool_words = {"pool", "cau hoi"}

    needs_entity_flag = cmd.name in ("ls", "del", "search")
    if needs_entity_flag:
        if any(w in text for w in entity_words):
            parts.append("-u")
        elif any(w in text for w in quiz_words):
            parts.append("-q")
        elif any(w in text for w in attempt_words):
            parts.append("-a")
        elif any(w in text for w in pool_words):
            parts.append("-p")

    # Detect subcommands for commands that need them
    sub_cmds_map = {
        "my": ["quizzes", "attempts", "stats", "pool"],
        "cache": ["status", "clear", "sync", "retry"],
        "sync": ["now", "status", "push", "pull", "retry"],
        "config": ["list", "get", "set", "reset", "export"],
        "perm": ["list", "show", "grant", "revoke"],
        "export": ["users", "quizzes", "attempts", "stats", "logs"],
        "purge": [
            "trash",
            "inactive",
            "old-attempts",
            "orphans",
            "banned",
            "empty-quizzes",
        ],
    }
    if cmd.name in sub_cmds_map:
        for sub in sub_cmds_map[cmd.name]:
            if sub in text:
                parts.append(sub)
                break
        else:
            # Default subcommand
            defaults = {
                "my": "quizzes",
                "cache": "status",
                "sync": "status",
                "config": "list",
                "perm": "list",
                "export": "stats",
                "purge": "trash",
            }
            if cmd.name in defaults:
                parts.append(defaults[cmd.name])

    # Add extracted identifiers
    for email in emails:
        parts.append(email)
    for eid in ids:
        if eid not in parts:
            parts.append(eid)

    # Extract quoted strings from original text
    quoted = re.findall(r"['\"]([^'\"]+)['\"]", text)
    for q in quoted:
        if q not in " ".join(parts):
            parts.append(f'"{q}"')

    # Detect common flag intents
    if "json" in words:
        parts.append("--format json")
    elif "csv" in words:
        parts.append("--format csv")
    elif "table" in words or "bang" in words:
        parts.append("--format table")

    if any(w in text for w in ["gioi han", "limit", "so luong"]):
        # Try to find a number
        for w in words:
            if w.isdigit():
                parts.append(f"--limit {w}")
                break

    return " ".join(parts)


# ---------------------------------------------------------------------------
# Tool 7 -- get_command_examples
# ---------------------------------------------------------------------------


@mcp.tool()
def get_command_examples(command_name: str) -> str:
    """Get usage examples for a specific console command.

    Args:
        command_name: Command name or alias (e.g. "ban", "ls", "my").

    Returns:
        List of examples with descriptions.
    """
    cmd = get_command_by_name(command_name)
    if cmd is None:
        return (
            f"Command '{command_name}' not found. "
            "Use list_commands to see all available commands."
        )

    if not cmd.examples:
        return f"No examples available for '{cmd.name}'."

    lines: list[str] = [
        f"Usage examples for '{cmd.name}':",
        f"Syntax: {cmd.usage}",
        "",
    ]
    for i, (example_cmd, example_desc) in enumerate(cmd.examples, 1):
        lines.append(f"  {i}. $ {example_cmd}")
        lines.append(f"     {example_desc}")
        lines.append("")

    if cmd.is_destructive:
        lines.append(
            "Note: Destructive command -- always add --confirm to execute "
            "or --dry-run to preview."
        )

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(description="Quizzez Console MCP Server")
    parser.add_argument(
        "--device",
        metavar="SERIAL",
        default=None,
        help="Target a specific ADB device/emulator serial (e.g. emulator-5554).",
    )
    args = parser.parse_args()

    if args.device:
        _adb_bridge.device_serial = args.device
        logger.info("Targeting device: %s", args.device)

    logger.info("Starting Quizzez Console MCP server (stdio transport)...")
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
