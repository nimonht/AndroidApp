"""
Quizzez Console MCP Server
===========================

A Model Context Protocol (MCP) server that exposes the Quizzez Android app's
in-app console system to AI agents.  Seven tools are registered:

1. list_commands      -- List available commands (filterable by category/role)
2. get_command_help   -- Detailed help for a specific command
3. execute_command    -- Execute a command on device or via mock
4. suggest_command    -- Autocomplete suggestions for partial input
5. validate_command   -- Syntax validation without execution
6. build_command      -- Natural-language to command translation
7. get_command_examples -- Usage examples for a command

Run with:
    python server.py            # stdio transport (default, for Claude Desktop)
    python server.py --mock     # force mock executor for all execute calls
"""

from __future__ import annotations

import argparse
import logging
import re
import sys
from difflib import SequenceMatcher

from adb_bridge import AdbBridge, MockExecutor
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
# Server & executors
# ---------------------------------------------------------------------------

mcp = FastMCP(
    "Quizzez Console",
    instructions=(
        "MCP server cho he thong console cua ung dung Quizzez Android. "
        "Cung cap truy cap vao ~32 lenh console de kiem tra, quan ly va "
        "tuong tac voi ung dung thong qua AI agent.\n\n"
        "Cac tool chinh:\n"
        "- list_commands: Liet ke lenh (loc theo danh muc/vai tro)\n"
        "- get_command_help: Xem huong dan chi tiet cho 1 lenh\n"
        "- execute_command: Thuc thi lenh (mock hoac ADB)\n"
        "- suggest_command: Goi y autocomplete\n"
        "- validate_command: Kiem tra cu phap\n"
        "- build_command: Dich ngon ngu tu nhien thanh lenh\n"
        "- get_command_examples: Xem vi du su dung"
    ),
)

_adb_bridge = AdbBridge()
_mock_executor = MockExecutor()

# Set via --mock CLI flag; when True every execute_command call uses the mock.
_force_mock: bool = False


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _format_output(output) -> str:
    """Format a CommandOutput into a plain-text string for the agent."""
    if output.error_message:
        return f"[LOI] {output.error_message}"
    if not output.lines:
        return "(khong co ket qua)"
    parts: list[str] = []
    for line in output.lines:
        prefix = ""
        if line.style == "ERROR":
            prefix = "[LOI] "
        elif line.style == "WARNING":
            prefix = "[CANH BAO] "
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
    """Liet ke cac lenh console co san, co the loc theo danh muc hoac vai tro.

    Args:
        category: Loc theo danh muc (util, user, system, pipe, admin). De trong = tat ca.
        role: Vai tro nguoi dung (GUEST, USER, ADMIN, SUPERUSER). Mac dinh: USER.

    Returns:
        Bang lenh voi ten, mo ta, danh muc.
    """
    role = role.upper() if role else "USER"
    if role not in ROLE_HIERARCHY:
        return f"Vai tro khong hop le: '{role}'. Chon: {', '.join(ROLE_HIERARCHY)}"

    if category:
        cat = category.lower()
        if cat not in CATEGORIES:
            return f"Danh muc khong hop le: '{category}'. Chon: {', '.join(CATEGORIES)}"
        cmds = [
            c
            for c in get_commands_by_category(cat)
            if role_meets_minimum(role, c.minimum_role)
        ]
        header = f"Lenh trong danh muc '{cat}' (vai tro: {role}):\n\n"
        return header + format_command_table(cmds, show_category=False)

    cmds = get_commands_for_role(role)
    header = f"Tat ca lenh kha dung (vai tro: {role}):\n\n"

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

    footer = f"\nTong cong: {len(cmds)} lenh. Dung 'get_command_help' de xem chi tiet."
    return header + "\n\n".join(sections) + "\n" + footer


# ---------------------------------------------------------------------------
# Tool 2 -- get_command_help
# ---------------------------------------------------------------------------


@mcp.tool()
def get_command_help(command_name: str) -> str:
    """Xem huong dan chi tiet cho mot lenh console cu the.

    Args:
        command_name: Ten lenh hoac bi danh (vd: "ban", "ls", "h").

    Returns:
        Tai lieu day du: mo ta, cu phap, flags, vi du, quyen.
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
                f"Lenh '{command_name}' khong ton tai. "
                f"Co phai ban muon noi '{best.name}'?\n\n"
            )
            return suggestion + format_command_detail(best)
        return (
            f"Lenh '{command_name}' khong ton tai. "
            f"Go list_commands de xem danh sach lenh."
        )
    return format_command_detail(cmd)


# ---------------------------------------------------------------------------
# Tool 3 -- execute_command
# ---------------------------------------------------------------------------


@mcp.tool()
def execute_command(command: str, use_mock: bool = False) -> str:
    """Thuc thi lenh console tren thiet bi/emulator hoac che do mock.

    Args:
        command: Chuoi lenh day du (vd: "ping --count 3", "ls -u --limit 5").
        use_mock: True = dung mock (khong can thiet bi). False = gui qua ADB.

    Returns:
        Ket qua lenh (co the nhieu dong).
    """
    if not command or not command.strip():
        return "[LOI] Lenh trong. Hay nhap mot lenh hop le."

    logger.info("execute_command: %r (mock=%s)", command, use_mock or _force_mock)

    if use_mock or _force_mock:
        result = _mock_executor.execute(command.strip())
        return _format_output(result)

    # Real ADB execution
    if not _adb_bridge.check_adb_available():
        return (
            "[LOI] adb khong tim thay tren PATH.\n"
            "Hay cai dat Android SDK hoac dung use_mock=true de test."
        )

    connected, detail = _adb_bridge.check_device_connected()
    if not connected:
        return f"[LOI] {detail}\nHay ket noi thiet bi/emulator hoac dung use_mock=true."

    result = _adb_bridge.execute(command.strip())
    return _format_output(result)


# ---------------------------------------------------------------------------
# Tool 4 -- suggest_command
# ---------------------------------------------------------------------------


@mcp.tool()
def suggest_command(partial_input: str) -> str:
    """Goi y lenh tu dau vao mot phan (autocomplete).

    Args:
        partial_input: Chuoi dau vao hien tai (vd: "hel", "ls -", "ban --").

    Returns:
        Danh sach goi y kem mo ta.
    """
    text = partial_input.strip().lower()
    if not text:
        return "Nhap it nhat mot ky tu de nhan goi y."

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
            return f"Khong tim thay lenh nao bat dau bang '{first_token}'."

        lines = [f"Goi y cho '{partial_input}':", ""]
        for name, desc in matches[:10]:
            lines.append(f"  {name:<20s} {desc}")
        return "\n".join(lines)

    # Command is known; suggest flags/subcommands
    lines = [f"Goi y cho '{partial_input}' (lenh: {cmd.name}):", ""]

    current_token = parts[-1] if not partial_input.endswith(" ") else ""

    # If typing a flag
    if current_token.startswith("--"):
        prefix = current_token.lstrip("-")
        all_flags = list(cmd.value_flags) + list(cmd.boolean_flags)
        matching = [f for f in all_flags if f.startswith(prefix)]
        if matching:
            for f in matching:
                tag = "<gia_tri>" if f in cmd.value_flags else "(boolean)"
                lines.append(f"  --{f}  {tag}")
        else:
            lines.append("  (khong co flag nao khop)")
    elif current_token.startswith("-") and len(current_token) <= 2:
        # Short flags -- just list the available single-char flags
        lines.append("  Flags kha dung:")
        for f in cmd.boolean_flags[:8]:
            lines.append(f"    --{f}")
        for f in cmd.value_flags[:8]:
            lines.append(f"    --{f} <gia_tri>")
    else:
        # Suggest subcommands / general flags
        # Check for subcommand patterns in usage
        usage_match = re.search(r"<(\w+(?:\|\w+)+)>", cmd.usage)
        if usage_match:
            options = usage_match.group(1).split("|")
            used = set(parts[1:])
            remaining = [o for o in options if o not in used]
            if remaining:
                lines.append("  Tham so:")
                for opt in remaining:
                    lines.append(f"    {opt}")

        if cmd.value_flags or cmd.boolean_flags:
            lines.append("  Flags kha dung:")
            for f in cmd.value_flags:
                lines.append(f"    --{f} <gia_tri>")
            for f in cmd.boolean_flags:
                lines.append(f"    --{f}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Tool 5 -- validate_command
# ---------------------------------------------------------------------------


@mcp.tool()
def validate_command(command: str) -> str:
    """Kiem tra cu phap lenh ma khong thuc thi.

    Args:
        command: Chuoi lenh can kiem tra (vd: "ban user@ex.com --confirm").

    Returns:
        Ket qua xac thuc: hop le hay khong, cac van de phat hien.
    """
    if not command or not command.strip():
        return "KHONG HOP LE: Lenh trong."

    text = command.strip()
    parts = text.split()
    cmd_name = parts[0].lower()

    cmd = get_command_by_name(cmd_name)
    if cmd is None:
        suggestions: list[str] = []
        for c in ALL_COMMANDS:
            if _similarity(cmd_name, c.name) > 0.5:
                suggestions.append(c.name)
        msg = f"KHONG HOP LE: Lenh '{cmd_name}' khong ton tai."
        if suggestions:
            msg += f" Co phai: {', '.join(suggestions)}?"
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
                issues.append(f"Flag khong nhan dien: --{flag_name}")
        elif part.startswith("-") and len(part) == 2 and part[1].isalpha():
            short = part[1]
            if short not in known_short and short not in known:
                warnings.append(f"Flag ngan co the khong hop le: -{short}")

    # Check destructive commands need --confirm
    if cmd.is_destructive:
        has_confirm = "--confirm" in parts
        has_dry_run = "--dry-run" in parts
        if not has_confirm and not has_dry_run:
            warnings.append(
                "Lenh huy diet! Can --confirm de thuc thi hoac --dry-run de xem truoc."
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
            warnings.append(f"Co the thieu tham so bat buoc: <{example_args}>")

    # Check role requirement
    if cmd.minimum_role != "USER":
        warnings.append(f"Lenh nay yeu cau vai tro toi thieu: {cmd.minimum_role}")

    if cmd.required_permission:
        warnings.append(f"Can quyen: {cmd.required_permission}")

    # Build result
    lines: list[str] = []
    if not issues:
        lines.append(f"HOP LE: Cu phap lenh '{cmd.name}' dung.")
    else:
        lines.append(f"KHONG HOP LE: Phat hien {len(issues)} loi:")
        for i, issue in enumerate(issues, 1):
            lines.append(f"  {i}. {issue}")

    if warnings:
        lines.append("")
        lines.append(f"Luu y ({len(warnings)}):")
        for w in warnings:
            lines.append(f"  - {w}")

    lines.append("")
    lines.append(f"Cu phap chuan: {cmd.usage}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Tool 6 -- build_command
# ---------------------------------------------------------------------------

# Keyword -> command mapping for natural-language resolution.
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
    "quiz": ["quizinfo", "ls", "my"],
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
    """Xay dung lenh console tu mo ta bang ngon ngu tu nhien.

    Args:
        description: Mo ta viec muon lam (vd: "cam nguoi dung user@ex.com",
                     "xem thong ke he thong", "liet ke tat ca quiz").

    Returns:
        Lenh duoc goi y kem giai thich.
    """
    if not description or not description.strip():
        return "Hay mo ta viec ban muon lam."

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
            "Khong the xac dinh lenh phu hop. "
            "Hay mo ta cu the hon hoac dung list_commands de xem danh sach."
        )

    # Sort by score descending
    ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    top_name = ranked[0][0]
    top_cmd = get_command_by_name(top_name)
    assert top_cmd is not None

    lines: list[str] = []
    lines.append(f"Lenh goi y: {top_cmd.name}")
    lines.append(f"Mo ta:      {top_cmd.description}")
    lines.append(f"Cu phap:    {top_cmd.usage}")
    lines.append("")

    # Try to build a concrete command from the description
    concrete = _build_concrete(top_cmd, text, words)
    lines.append(f"Lenh cu the: {concrete}")

    if top_cmd.is_destructive:
        lines.append("")
        lines.append(
            "Luu y: Lenh nay co tac dong khong the hoan tac. "
            "Them --confirm de xac nhan hoac --dry-run de xem truoc."
        )

    # Show alternatives if close scores
    if len(ranked) > 1:
        lines.append("")
        lines.append("Cac lenh lien quan khac:")
        for alt_name, alt_score in ranked[1:4]:
            alt_cmd = get_command_by_name(alt_name)
            if alt_cmd:
                lines.append(f"  - {alt_cmd.name}: {alt_cmd.description}")

    return "\n".join(lines)


def _build_concrete(cmd: CommandInfo, text: str, words: list[str]) -> str:
    """Attempt to construct a concrete command string from NL description."""
    parts = [cmd.name]

    # Extract email-like tokens
    emails = [w for w in words if "@" in w and "." in w]

    # Extract ID-like tokens (hex strings, long alphanumeric)
    ids = [w for w in words if len(w) > 8 and w.isalnum() and not w.isalpha()]

    # Detect entity-type flags from context
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
    """Xem vi du su dung cho mot lenh cu the.

    Args:
        command_name: Ten lenh hoac bi danh (vd: "ban", "ls", "my").

    Returns:
        Danh sach vi du kem mo ta.
    """
    cmd = get_command_by_name(command_name)
    if cmd is None:
        return (
            f"Lenh '{command_name}' khong ton tai. "
            "Go list_commands de xem danh sach lenh."
        )

    if not cmd.examples:
        return f"Lenh '{cmd.name}' chua co vi du nao."

    lines: list[str] = [
        f"Vi du su dung cho '{cmd.name}':",
        f"Cu phap: {cmd.usage}",
        "",
    ]
    for i, (example_cmd, example_desc) in enumerate(cmd.examples, 1):
        lines.append(f"  {i}. $ {example_cmd}")
        lines.append(f"     {example_desc}")
        lines.append("")

    if cmd.is_destructive:
        lines.append(
            "Luu y: Lenh huy diet -- luon them --confirm de xac nhan "
            "hoac --dry-run de xem truoc."
        )

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(description="Quizzez Console MCP Server")
    parser.add_argument(
        "--mock",
        action="store_true",
        help="Force mock executor for all execute_command calls (no ADB needed).",
    )
    args = parser.parse_args()

    global _force_mock
    _force_mock = args.mock

    if _force_mock:
        logger.info("Mock mode enabled -- all commands will use MockExecutor.")

    logger.info("Starting Quizzez Console MCP server (stdio transport)...")
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
