"""
Complete command reference data for the Quizzez Android app console.

This module contains metadata for all ~35 console commands, grouped by category.
The data mirrors the Kotlin Command implementations in
domain/console/commands/ and is used by the MCP server to provide
documentation, validation, and suggestion capabilities without a live
device connection.

Categories: util, user, system, pipe, admin
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional


@dataclass(frozen=True)
class CommandInfo:
    """Metadata for a single console command."""

    name: str
    aliases: list[str] = field(default_factory=list)
    description: str = ""
    usage: str = ""
    category: str = "general"
    minimum_role: str = "USER"
    is_destructive: bool = False
    required_permission: Optional[str] = None
    examples: list[tuple[str, str]] = field(default_factory=list)
    value_flags: list[str] = field(default_factory=list)
    boolean_flags: list[str] = field(default_factory=list)


# ---------------------------------------------------------------------------
# util category
# ---------------------------------------------------------------------------

CLEAR = CommandInfo(
    name="clear",
    aliases=["cls", "clr"],
    description="Clear the console screen",
    usage="clear",
    category="util",
    minimum_role="USER",
    examples=[
        ("clear", "Clear all console output"),
    ],
)

ECHO = CommandInfo(
    name="echo",
    aliases=["print"],
    description="Print text to the console",
    usage="echo <text> [--style <style>] [--repeat <count>] [--upper] [--lower] [--timestamp]",
    category="util",
    minimum_role="USER",
    value_flags=["style", "repeat"],
    boolean_flags=["upper", "lower", "timestamp"],
    examples=[
        ("echo Hello World", "Print 'Hello World' to the console"),
        ("echo --style success Thanh cong!", "Print a green-styled line"),
        ("echo --repeat 3 abc", "Print 'abc' 3 times"),
        ("echo --upper hello", "Print 'HELLO' in uppercase"),
        ("echo --timestamp Ghi chu", "Print with current timestamp"),
    ],
)

HISTORY = CommandInfo(
    name="history",
    aliases=["hist"],
    description="Display and manage command history",
    usage=(
        "history [<count>] [--search <keyword>] [--regex <pattern>] [--clear] "
        "[--unique] [--reverse] [--numbered] [--no-numbered] "
        "[--since <time>] [--format <format>] [--export]"
    ),
    category="util",
    minimum_role="USER",
    value_flags=["search", "regex", "since", "format"],
    boolean_flags=["clear", "unique", "reverse", "numbered", "no-numbered", "export"],
    examples=[
        ("history", "Show full command history"),
        ("history 10", "Show the last 10 commands"),
        ("history --search ping", "Search for commands containing 'ping'"),
        ("history --clear", "Clear command history"),
        ("history --unique --reverse", "Show unique commands in reverse order"),
        ("history --export", "Export history as plain text"),
    ],
)

ALIAS = CommandInfo(
    name="alias",
    aliases=[],
    description="Manage command aliases",
    usage="alias [<name>=<value>] [--remove <name>] [--clear] [--list]",
    category="util",
    minimum_role="USER",
    value_flags=["remove"],
    boolean_flags=["clear", "list"],
    examples=[
        ("alias", "List all aliases"),
        ("alias ll=ls --limit 20", "Create alias 'll' for 'ls --limit 20'"),
        ("alias --remove ll", "Remove alias 'll'"),
        ("alias --clear", "Clear all aliases"),
        ("alias --list", "Show all aliases as a table"),
    ],
)

HELP = CommandInfo(
    name="help",
    aliases=["?", "h", "man"],
    description="Display command list or detailed help",
    usage=(
        "help [<command_name>] [--all] [--category <category>] "
        "[--search <keyword>] [--format <format>] [--flags] [--examples]"
    ),
    category="util",
    minimum_role="USER",
    value_flags=["category", "search", "format"],
    boolean_flags=["all", "flags", "examples"],
    examples=[
        ("help", "List all available commands"),
        ("help ping", "View detailed help for the ping command"),
        ("help --all", "Show all commands including restricted ones"),
        ("help --category admin", "Show only admin category commands"),
        ("help --search delete", "Find commands whose description contains 'delete'"),
        ("help ban --examples", "View usage examples for the ban command"),
        ("help ban --flags", "View flags for the ban command"),
    ],
)

CONFIG = CommandInfo(
    name="config",
    aliases=["cfg", "settings"],
    description="Manage application configuration",
    usage="config <list|get|set|reset|export> [<key>] [<value>]",
    category="util",
    minimum_role="USER",
    examples=[
        ("config list", "List all configuration keys"),
        ("config get theme", "Get value of 'theme' config key"),
        ("config set theme dark", "Set theme to dark mode"),
        ("config reset theme", "Reset 'theme' config key to default"),
        ("config export", "Export all configuration"),
    ],
)

# ---------------------------------------------------------------------------
# user category
# ---------------------------------------------------------------------------

WHOAMI = CommandInfo(
    name="whoami",
    aliases=["user"],
    description="Display current user information",
    usage="whoami [--format <format>]",
    category="user",
    minimum_role="USER",
    value_flags=["format"],
    examples=[
        ("whoami", "Display current user info"),
        ("whoami --format json", "Display user info as JSON"),
    ],
)

MY = CommandInfo(
    name="my",
    aliases=["mine"],
    description="View personal data",
    usage="my <quizzes|attempts|stats|pool> [--limit <n>] [--sort <field>] [--format <format>]",
    category="user",
    minimum_role="USER",
    value_flags=["limit", "sort", "format"],
    examples=[
        ("my quizzes", "List your quizzes"),
        ("my attempts --limit 5", "View your last 5 quiz attempts"),
        ("my stats", "View personal statistics"),
        ("my pool", "View questions contributed to the pool"),
        (
            "my quizzes --sort date --format table",
            "List quizzes sorted by date, table format",
        ),
    ],
)

# ---------------------------------------------------------------------------
# system category
# ---------------------------------------------------------------------------

PING = CommandInfo(
    name="ping",
    aliases=["p"],
    description="Check network connectivity",
    usage="ping [--target <target>] [--count <count>]",
    category="system",
    minimum_role="USER",
    value_flags=["target", "count", "timeout", "service"],
    boolean_flags=["verbose"],
    examples=[
        ("ping", "Basic connectivity check"),
        ("ping --count 5", "Ping 5 times and show statistics"),
        ("ping --service auth", "Check connectivity to the auth service"),
        ("ping --verbose", "Show verbose ping results"),
    ],
)

CACHE = CommandInfo(
    name="cache",
    aliases=["sync-cache"],
    description="Manage sync cache",
    usage="cache <status|clear|sync|retry>",
    category="system",
    minimum_role="USER",
    examples=[
        ("cache status", "View cache status"),
        ("cache clear", "Clear the cache"),
        ("cache sync", "Sync cache with the server"),
        ("cache retry", "Retry failed sync operations"),
    ],
)

SYNC = CommandInfo(
    name="sync",
    aliases=[],
    description="Manage data synchronization",
    usage="sync <now|status|push|pull|retry>",
    category="system",
    minimum_role="USER",
    examples=[
        ("sync status", "View current sync status"),
        ("sync now", "Trigger immediate sync"),
        ("sync push", "Push local data to the server"),
        ("sync pull", "Pull data from the server"),
        ("sync retry", "Retry failed sync operations"),
    ],
)

# ---------------------------------------------------------------------------
# pipe category
# ---------------------------------------------------------------------------

GREP = CommandInfo(
    name="grep",
    aliases=["filter"],
    description="Filter lines by pattern",
    usage="grep <pattern> [--ignore-case] [--invert] [--count] [--word] [--context <n>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["context"],
    boolean_flags=["ignore-case", "invert", "count", "word"],
    examples=[
        ("ls -q | grep kotlin", "Filter quizzes whose name contains 'kotlin'"),
        ("log --limit 50 | grep --ignore-case error", "Filter logs containing 'error'"),
        ("my quizzes | grep --invert draft", "Filter quizzes that are not drafts"),
        ("my quizzes | grep --count public", "Count lines containing 'public'"),
    ],
)

SORT = CommandInfo(
    name="sort",
    aliases=[],
    description="Sort lines",
    usage="sort [--reverse] [--numeric] [--field <position>] [--random] [--unique] [--ignore-case]",
    category="pipe",
    minimum_role="USER",
    value_flags=["field"],
    boolean_flags=["reverse", "numeric", "random", "unique", "ignore-case"],
    examples=[
        ("my quizzes | sort", "Sort quizzes alphabetically"),
        ("my quizzes | sort --reverse", "Sort in reverse order"),
        ("my quizzes | sort --numeric --field 2", "Sort numerically by field 2"),
        ("my quizzes | sort --random", "Shuffle randomly"),
    ],
)

HEAD = CommandInfo(
    name="head",
    aliases=[],
    description="Take N leading lines",
    usage="head [<n>] [--skip <n>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["skip"],
    examples=[
        ("my quizzes | head 5", "Take the first 5 lines"),
        ("log --limit 100 | head 10", "Take the first 10 lines from log"),
        ("my quizzes | head 10 --skip 5", "Skip 5 lines, take next 10"),
    ],
)

TAIL = CommandInfo(
    name="tail",
    aliases=[],
    description="Take N trailing lines",
    usage="tail [<n>] [--skip <n>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["skip"],
    examples=[
        ("my quizzes | tail 5", "Take the last 5 lines"),
        ("log --limit 100 | tail 20", "Take the last 20 lines from log"),
        ("my attempts | tail 3 --skip 2", "Skip last 2 lines, take previous 3"),
    ],
)

COUNT = CommandInfo(
    name="count",
    aliases=["wc"],
    description="Count lines/words/characters",
    usage="count [--lines] [--words] [--chars] [--unique] [--non-empty] [--freq <position>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["freq"],
    boolean_flags=["lines", "words", "chars", "unique", "non-empty"],
    examples=[
        ("my quizzes | count", "Count lines (number of quizzes)"),
        ("my quizzes | count --words", "Count total words"),
        ("my quizzes | count --unique", "Count unique lines"),
        ("log --limit 100 | count --non-empty", "Count non-empty lines"),
        ("ls -u | count --freq 3", "Frequency count of field 3"),
    ],
)

LOG = CommandInfo(
    name="log",
    aliases=["logs"],
    description="View and filter application logs",
    usage=(
        "log [--level <level>] [--tag <tag>] [--regex <pattern>] "
        "[--limit <n>] [--since <time>] [--format <format>] [--export]"
    ),
    category="pipe",
    minimum_role="USER",
    value_flags=["level", "tag", "regex", "limit", "since", "format"],
    boolean_flags=["export"],
    examples=[
        ("log", "Show recent log entries"),
        ("log --level error", "Show only error logs"),
        ("log --tag SyncManager", "Filter logs by tag"),
        ("log --limit 50 --since 1h", "50 log lines from the last hour"),
        ("log --export", "Export log as plain text"),
        ("log --regex 'Exception.*null'", "Filter logs by regex"),
    ],
)

# ---------------------------------------------------------------------------
# admin category
# ---------------------------------------------------------------------------

BAN = CommandInfo(
    name="ban",
    aliases=[],
    description="Ban a user",
    usage="ban <email|id> [--reason <reason>] [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="BAN_USERS",
    value_flags=[
        "reason",
        "role",
        "search",
        "regex",
        "inactive-days",
        "exclude",
        "format",
    ],
    boolean_flags=["confirm", "dry-run", "verbose", "quiet", "all"],
    examples=[
        ("ban user@example.com --confirm", "Ban a user by email"),
        (
            "ban user@example.com --reason 'Vi pham' --confirm",
            "Ban with a specific reason",
        ),
        ("ban --role USER --dry-run", "Preview users that would be banned"),
        ("ban user1@ex.com user2@ex.com --confirm", "Ban multiple users at once"),
    ],
)

UNBAN = CommandInfo(
    name="unban",
    aliases=[],
    description="Unban a user",
    usage="unban <email|id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="BAN_USERS",
    boolean_flags=["confirm", "verbose", "quiet"],
    examples=[
        ("unban user@example.com --confirm", "Unban a user"),
    ],
)

ROLE = CommandInfo(
    name="role",
    aliases=[],
    description="Change user role",
    usage="role <email|id> <role> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="CHANGE_USER_ROLES",
    boolean_flags=["confirm"],
    examples=[
        ("role user@example.com ADMIN --confirm", "Promote user to ADMIN"),
        ("role user@example.com USER --confirm", "Demote user to USER"),
    ],
)

PERM = CommandInfo(
    name="perm",
    aliases=["permission"],
    description="Manage admin permissions",
    usage="perm <list|show|grant|revoke> [<email|id>] [<permission>] [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="MANAGE_PERMISSIONS",
    boolean_flags=["confirm"],
    examples=[
        ("perm list", "List all permissions"),
        ("perm show admin@example.com", "View permissions of a user"),
        ("perm grant admin@ex.com BAN_USERS --confirm", "Grant BAN_USERS permission"),
        (
            "perm revoke admin@ex.com DELETE_USERS --confirm",
            "Revoke DELETE_USERS permission",
        ),
    ],
)

USERINFO = CommandInfo(
    name="userinfo",
    aliases=["uinfo"],
    description="View detailed user information",
    usage="userinfo <email|id>",
    category="admin",
    minimum_role="ADMIN",
    required_permission="MANAGE_USERS",
    value_flags=["format"],
    examples=[
        ("userinfo user@example.com", "View user info by email"),
        ("userinfo abc123", "View user info by ID"),
    ],
)

DEL = CommandInfo(
    name="del",
    aliases=["delete"],
    description="Delete an object",
    usage="del <-u|-q|-a|-p> <id> [--confirm] [--dry-run]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    boolean_flags=["confirm", "dry-run", "u", "q", "a", "p"],
    examples=[
        ("del -u user123 --confirm", "Delete user by ID"),
        ("del -q quiz456 --confirm", "Delete quiz by ID"),
        ("del -a attempt789 --confirm", "Delete attempt by ID"),
        ("del -p pool012 --confirm", "Delete pool question by ID"),
        ("del -q quiz456 --dry-run", "Preview deletion (no actual delete)"),
    ],
)

QUIZINFO = CommandInfo(
    name="quizinfo",
    aliases=["qi"],
    description="View detailed quiz information",
    usage="quizinfo <id> [--questions] [--attempts] [--stats]",
    category="admin",
    minimum_role="USER",
    boolean_flags=["questions", "attempts", "stats"],
    examples=[
        ("quizinfo quiz123", "View basic quiz information"),
        ("quizinfo quiz123 --questions", "Include question list"),
        ("quizinfo quiz123 --attempts", "Include attempt list"),
        ("quizinfo quiz123 --stats", "Include statistics"),
        ("quizinfo quiz123 --questions --stats", "Include questions and statistics"),
    ],
)

PUBLISH = CommandInfo(
    name="publish",
    aliases=["pub"],
    description="Publish a quiz",
    usage="publish <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="PUBLISH_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("publish quiz123 --confirm", "Publish the quiz"),
    ],
)

UNPUBLISH = CommandInfo(
    name="unpublish",
    aliases=["unpub"],
    description="Unpublish a quiz",
    usage="unpublish <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="PUBLISH_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("unpublish quiz123 --confirm", "Unpublish the quiz"),
    ],
)

RESTORE = CommandInfo(
    name="restore",
    aliases=[],
    description="Restore a deleted quiz",
    usage="restore <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="DELETE_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("restore quiz123 --confirm", "Restore the deleted quiz"),
    ],
)

LS = CommandInfo(
    name="ls",
    aliases=["list"],
    description="List objects",
    usage=(
        "ls <-u|-q|-a|-p> [--limit <n>] [--offset <n>] "
        "[--sort <field>] [--order <asc|desc>] [--format <format>] [--filter <condition>]"
    ),
    category="admin",
    minimum_role="ADMIN",
    value_flags=["limit", "offset", "sort", "order", "format", "filter", "role"],
    boolean_flags=["u", "q", "a", "p"],
    examples=[
        ("ls -u", "List users"),
        ("ls -q --limit 10", "List 10 quizzes"),
        ("ls -a --sort date --order desc", "List attempts sorted by date"),
        ("ls -u --format json", "List users as JSON"),
        ("ls -u --filter 'role=ADMIN'", "Filter users by role"),
        ("ls -p --limit 20 --offset 10", "List pool questions with pagination"),
    ],
)

STATS = CommandInfo(
    name="stats",
    aliases=["statistics"],
    description="System statistics",
    usage="stats [--format <format>]",
    category="admin",
    minimum_role="ADMIN",
    required_permission="VIEW_REPORTS",
    value_flags=["format"],
    examples=[
        ("stats", "View overall system statistics"),
        ("stats --format json", "Export statistics as JSON"),
    ],
)

SEARCH = CommandInfo(
    name="search",
    aliases=["find", "s"],
    description="Search users or quizzes",
    usage="search <-u|-q> <keyword> [--exact] [--regex] [--limit <n>] [--format <format>]",
    category="admin",
    minimum_role="USER",
    value_flags=["limit", "format"],
    boolean_flags=["u", "q", "exact", "regex"],
    examples=[
        ("search -u john", "Search for users named 'john'"),
        ("search -q kotlin", "Search for quizzes named 'kotlin'"),
        ("search -u --exact admin@example.com", "Exact match by email"),
        ("search -q --regex '^Test.*2024'", "Search quizzes by regex"),
        ("search -q kotlin --limit 5 --format table", "Find 5 quizzes in table format"),
    ],
)

EXPORT = CommandInfo(
    name="export",
    aliases=["exp"],
    description="Export data",
    usage="export <users|quizzes|attempts|stats|logs> [--format <csv|json|table>] [--limit <n>]",
    category="admin",
    minimum_role="ADMIN",
    required_permission="VIEW_REPORTS",
    value_flags=["format", "limit"],
    examples=[
        ("export users", "Export user list"),
        ("export quizzes --format json", "Export quizzes as JSON"),
        ("export stats --format csv", "Export statistics as CSV"),
        ("export logs --limit 100", "Export last 100 log entries"),
        ("export attempts --format table", "Export attempts as table"),
    ],
)

PURGE = CommandInfo(
    name="purge",
    aliases=["cleanup"],
    description="Clean up data",
    usage=(
        "purge <trash|inactive|old-attempts|orphans|banned|empty-quizzes> "
        "[--before <date>] [--confirm] [--dry-run]"
    ),
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    value_flags=["before"],
    boolean_flags=["confirm", "dry-run"],
    examples=[
        ("purge trash --confirm", "Permanently delete items in trash"),
        (
            "purge inactive --before 2024-01-01 --dry-run",
            "Preview inactive users",
        ),
        ("purge old-attempts --before 2023-06-01 --confirm", "Delete old attempts"),
        (
            "purge orphans --dry-run",
            "Find orphaned questions (not belonging to any quiz)",
        ),
        ("purge banned --confirm", "Delete data of banned users"),
        ("purge empty-quizzes --confirm", "Delete quizzes with no questions"),
    ],
)

# ---------------------------------------------------------------------------
# Master list and lookup helpers
# ---------------------------------------------------------------------------

ALL_COMMANDS: list[CommandInfo] = [
    # util
    CLEAR,
    ECHO,
    HISTORY,
    ALIAS,
    HELP,
    CONFIG,
    # user
    WHOAMI,
    MY,
    # system
    PING,
    CACHE,
    SYNC,
    # pipe
    GREP,
    SORT,
    HEAD,
    TAIL,
    COUNT,
    LOG,
    # admin
    BAN,
    UNBAN,
    ROLE,
    PERM,
    USERINFO,
    DEL,
    QUIZINFO,
    PUBLISH,
    UNPUBLISH,
    RESTORE,
    LS,
    STATS,
    SEARCH,
    EXPORT,
    PURGE,
]

CATEGORIES = ["util", "user", "system", "pipe", "admin"]

ROLE_HIERARCHY = ["GUEST", "USER", "ADMIN", "SUPERUSER"]


def _role_index(role: str) -> int:
    """Return the numeric index of a role in the hierarchy (higher = more privilege)."""
    try:
        return ROLE_HIERARCHY.index(role.upper())
    except ValueError:
        return 1  # default to USER


def role_meets_minimum(user_role: str, minimum_role: str) -> bool:
    """Check whether *user_role* meets or exceeds *minimum_role*."""
    return _role_index(user_role) >= _role_index(minimum_role)


def get_command_by_name(name: str) -> CommandInfo | None:
    """Look up a command by primary name or alias (case-insensitive)."""
    lower = name.lower()
    for cmd in ALL_COMMANDS:
        if cmd.name == lower:
            return cmd
        if lower in (a.lower() for a in cmd.aliases):
            return cmd
    return None


def get_commands_by_category(category: str) -> list[CommandInfo]:
    """Return all commands belonging to *category* (case-insensitive)."""
    cat = category.lower()
    return [c for c in ALL_COMMANDS if c.category == cat]


def get_commands_for_role(role: str) -> list[CommandInfo]:
    """Return all commands accessible to the given *role*."""
    return [c for c in ALL_COMMANDS if role_meets_minimum(role, c.minimum_role)]


def search_commands(query: str) -> list[CommandInfo]:
    """Search commands by name, alias, description, or usage (case-insensitive)."""
    q = query.lower()
    results: list[CommandInfo] = []
    for cmd in ALL_COMMANDS:
        if (
            q in cmd.name.lower()
            or q in cmd.description.lower()
            or q in cmd.usage.lower()
            or any(q in a.lower() for a in cmd.aliases)
        ):
            results.append(cmd)
    return results


def format_command_table(
    commands: list[CommandInfo], *, show_category: bool = True
) -> str:
    """Format a list of commands as a plain-text table."""
    if not commands:
        return "(no commands found)"

    lines: list[str] = []
    # Header
    if show_category:
        lines.append(f"{'Command':<16} {'Category':<10} {'Description'}")
        lines.append(f"{'----':<16} {'--------':<10} {'----'}")
    else:
        lines.append(f"{'Command':<16} {'Description'}")
        lines.append(f"{'----':<16} {'----'}")

    for cmd in commands:
        alias_str = f" ({', '.join(cmd.aliases)})" if cmd.aliases else ""
        name_col = f"{cmd.name}{alias_str}"
        if show_category:
            lines.append(f"{name_col:<16} {cmd.category:<10} {cmd.description}")
        else:
            lines.append(f"{name_col:<16} {cmd.description}")

    return "\n".join(lines)


def format_command_detail(cmd: CommandInfo) -> str:
    """Format full documentation for a single command."""
    lines: list[str] = []

    lines.append(f"=== {cmd.name.upper()} ===")
    lines.append("")
    lines.append(f"  Description: {cmd.description}")
    lines.append(f"  Usage:       {cmd.usage}")
    lines.append(f"  Category:    {cmd.category}")
    lines.append(f"  Role:        {cmd.minimum_role}")

    if cmd.aliases:
        lines.append(f"  Aliases:     {', '.join(cmd.aliases)}")

    if cmd.is_destructive:
        lines.append("  Warning:     This command is IRREVERSIBLE (requires --confirm)")

    if cmd.required_permission:
        lines.append(f"  Permission:  {cmd.required_permission}")

    if cmd.value_flags or cmd.boolean_flags:
        lines.append("")
        lines.append("  Flags:")
        for f in cmd.value_flags:
            lines.append(f"    --{f} <value>")
        for f in cmd.boolean_flags:
            lines.append(f"    --{f}")

    if cmd.examples:
        lines.append("")
        lines.append("  Examples:")
        for example_cmd, example_desc in cmd.examples:
            lines.append(f"    $ {example_cmd}")
            lines.append(f"      {example_desc}")

    return "\n".join(lines)
