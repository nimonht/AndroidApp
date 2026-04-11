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
    description="Xoa man hinh console",
    usage="clear",
    category="util",
    minimum_role="USER",
    examples=[
        ("clear", "Xoa toan bo noi dung man hinh console"),
    ],
)

ECHO = CommandInfo(
    name="echo",
    aliases=["print"],
    description="In van ban ra console",
    usage="echo <text> [--style <kieu>] [--repeat <so_lan>] [--upper] [--lower] [--timestamp]",
    category="util",
    minimum_role="USER",
    value_flags=["style", "repeat"],
    boolean_flags=["upper", "lower", "timestamp"],
    examples=[
        ("echo Hello World", "In 'Hello World' ra console"),
        ("echo --style success Thanh cong!", "In dong mau xanh la"),
        ("echo --repeat 3 abc", "In 'abc' 3 lan"),
        ("echo --upper hello", "In 'HELLO'"),
        ("echo --timestamp Ghi chu", "In kem thoi gian hien tai"),
    ],
)

HISTORY = CommandInfo(
    name="history",
    aliases=["hist"],
    description="Hien thi va quan ly lich su lenh",
    usage=(
        "history [<so_luong>] [--search <tu_khoa>] [--regex <mau>] [--clear] "
        "[--unique] [--reverse] [--numbered] [--no-numbered] "
        "[--since <thoi_gian>] [--format <dinh_dang>] [--export]"
    ),
    category="util",
    minimum_role="USER",
    value_flags=["search", "regex", "since", "format"],
    boolean_flags=["clear", "unique", "reverse", "numbered", "no-numbered", "export"],
    examples=[
        ("history", "Hien thi toan bo lich su lenh"),
        ("history 10", "Hien thi 10 lenh gan nhat"),
        ("history --search ping", "Tim lenh co chua 'ping'"),
        ("history --clear", "Xoa lich su lenh"),
        ("history --unique --reverse", "Hien thi lenh duy nhat, thu tu nguoc"),
        ("history --export", "Xuat lich su ra dinh dang van ban"),
    ],
)

ALIAS = CommandInfo(
    name="alias",
    aliases=[],
    description="Quan ly bi danh lenh",
    usage="alias [<ten>=<gia_tri>] [--remove <ten>] [--clear] [--list]",
    category="util",
    minimum_role="USER",
    value_flags=["remove"],
    boolean_flags=["clear", "list"],
    examples=[
        ("alias", "Liet ke tat ca bi danh"),
        ("alias ll=ls --limit 20", "Tao bi danh 'll' cho lenh 'ls --limit 20'"),
        ("alias --remove ll", "Xoa bi danh 'll'"),
        ("alias --clear", "Xoa tat ca bi danh"),
        ("alias --list", "Hien thi tat ca bi danh dang bang"),
    ],
)

HELP = CommandInfo(
    name="help",
    aliases=["?", "h", "man"],
    description="Hien thi danh sach lenh hoac chi tiet lenh",
    usage=(
        "help [<ten_lenh>] [--all] [--category <danh_muc>] "
        "[--search <tu_khoa>] [--format <dinh_dang>] [--flags] [--examples]"
    ),
    category="util",
    minimum_role="USER",
    value_flags=["category", "search", "format"],
    boolean_flags=["all", "flags", "examples"],
    examples=[
        ("help", "Liet ke tat ca lenh kha dung"),
        ("help ping", "Xem huong dan chi tiet lenh ping"),
        ("help --all", "Hien thi tat ca lenh ke ca lenh bi khoa"),
        ("help --category admin", "Chi hien thi lenh thuoc nhom admin"),
        ("help --search xoa", "Tim lenh co mo ta chua 'xoa'"),
        ("help ban --examples", "Xem vi du su dung lenh ban"),
        ("help ban --flags", "Xem danh sach co (flags) cua lenh ban"),
    ],
)

CONFIG = CommandInfo(
    name="config",
    aliases=["cfg", "settings"],
    description="Quan ly cau hinh ung dung",
    usage="config <list|get|set|reset|export> [<key>] [<value>]",
    category="util",
    minimum_role="USER",
    examples=[
        ("config list", "Liet ke tat ca cau hinh"),
        ("config get theme", "Xem gia tri cau hinh 'theme'"),
        ("config set theme dark", "Dat theme sang che do toi"),
        ("config reset theme", "Dat lai cau hinh 'theme' ve mac dinh"),
        ("config export", "Xuat toan bo cau hinh"),
    ],
)

# ---------------------------------------------------------------------------
# user category
# ---------------------------------------------------------------------------

WHOAMI = CommandInfo(
    name="whoami",
    aliases=["user"],
    description="Hien thi thong tin nguoi dung hien tai",
    usage="whoami [--format <dinh_dang>]",
    category="user",
    minimum_role="USER",
    value_flags=["format"],
    examples=[
        ("whoami", "Hien thi thong tin nguoi dung"),
        ("whoami --format json", "Hien thi thong tin dang JSON"),
    ],
)

MY = CommandInfo(
    name="my",
    aliases=["mine"],
    description="Xem du lieu ca nhan",
    usage="my <quizzes|attempts|stats|pool> [--limit <so>] [--sort <truong>] [--format <dinh_dang>]",
    category="user",
    minimum_role="USER",
    value_flags=["limit", "sort", "format"],
    examples=[
        ("my quizzes", "Liet ke bai kiem tra cua ban"),
        ("my attempts --limit 5", "Xem 5 lan lam bai gan nhat"),
        ("my stats", "Xem thong ke ca nhan"),
        ("my pool", "Xem cau hoi da dong gop vao pool"),
        (
            "my quizzes --sort date --format table",
            "Liet ke quiz sap xep theo ngay, dang bang",
        ),
    ],
)

# ---------------------------------------------------------------------------
# system category
# ---------------------------------------------------------------------------

PING = CommandInfo(
    name="ping",
    aliases=["p"],
    description="Kiem tra ket noi mang",
    usage="ping [--target <muc_tieu>] [--count <so_lan>]",
    category="system",
    minimum_role="USER",
    value_flags=["target", "count", "timeout", "service"],
    boolean_flags=["verbose"],
    examples=[
        ("ping", "Kiem tra ket noi co ban"),
        ("ping --count 5", "Ping 5 lan va hien thi thong ke"),
        ("ping --service auth", "Kiem tra ket noi den dich vu xac thuc"),
        ("ping --verbose", "Hien thi chi tiet ket qua ping"),
    ],
)

CACHE = CommandInfo(
    name="cache",
    aliases=["sync-cache"],
    description="Quan ly bo nho dem dong bo",
    usage="cache <status|clear|sync|retry>",
    category="system",
    minimum_role="USER",
    examples=[
        ("cache status", "Xem trang thai bo nho dem"),
        ("cache clear", "Xoa bo nho dem"),
        ("cache sync", "Dong bo bo nho dem voi may chu"),
        ("cache retry", "Thu lai cac thao tac dong bo that bai"),
    ],
)

SYNC = CommandInfo(
    name="sync",
    aliases=[],
    description="Quan ly dong bo du lieu",
    usage="sync <now|status|push|pull|retry>",
    category="system",
    minimum_role="USER",
    examples=[
        ("sync status", "Xem trang thai dong bo hien tai"),
        ("sync now", "Bat dau dong bo ngay lap tuc"),
        ("sync push", "Day du lieu local len may chu"),
        ("sync pull", "Keo du lieu tu may chu ve"),
        ("sync retry", "Thu lai cac thao tac dong bo that bai"),
    ],
)

# ---------------------------------------------------------------------------
# pipe category
# ---------------------------------------------------------------------------

GREP = CommandInfo(
    name="grep",
    aliases=["filter"],
    description="Loc dong theo mau",
    usage="grep <mau> [--ignore-case] [--invert] [--count] [--word] [--context <so_dong>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["context"],
    boolean_flags=["ignore-case", "invert", "count", "word"],
    examples=[
        ("ls -q | grep kotlin", "Loc quiz co ten chua 'kotlin'"),
        ("log --limit 50 | grep --ignore-case error", "Loc log co chua 'error'"),
        ("my quizzes | grep --invert draft", "Loc quiz khong phai nhap"),
        ("my quizzes | grep --count public", "Dem so dong chua 'public'"),
    ],
)

SORT = CommandInfo(
    name="sort",
    aliases=[],
    description="Sap xep dong",
    usage="sort [--reverse] [--numeric] [--field <vi_tri>] [--random] [--unique] [--ignore-case]",
    category="pipe",
    minimum_role="USER",
    value_flags=["field"],
    boolean_flags=["reverse", "numeric", "random", "unique", "ignore-case"],
    examples=[
        ("my quizzes | sort", "Sap xep quiz theo thu tu ABC"),
        ("my quizzes | sort --reverse", "Sap xep nguoc"),
        ("my quizzes | sort --numeric --field 2", "Sap xep theo truong so thu 2"),
        ("my quizzes | sort --random", "Xao tron ngau nhien"),
    ],
)

HEAD = CommandInfo(
    name="head",
    aliases=[],
    description="Lay N dong dau",
    usage="head [<so_dong>] [--skip <so>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["skip"],
    examples=[
        ("my quizzes | head 5", "Lay 5 dong dau"),
        ("log --limit 100 | head 10", "Lay 10 dong dau tu log"),
        ("my quizzes | head 10 --skip 5", "Bo 5 dong dau, lay 10 dong tiep"),
    ],
)

TAIL = CommandInfo(
    name="tail",
    aliases=[],
    description="Lay N dong cuoi",
    usage="tail [<so_dong>] [--skip <so>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["skip"],
    examples=[
        ("my quizzes | tail 5", "Lay 5 dong cuoi"),
        ("log --limit 100 | tail 20", "Lay 20 dong cuoi tu log"),
        ("my attempts | tail 3 --skip 2", "Bo 2 dong cuoi, lay 3 dong truoc do"),
    ],
)

COUNT = CommandInfo(
    name="count",
    aliases=["wc"],
    description="Dem dong/tu/ky tu",
    usage="count [--lines] [--words] [--chars] [--unique] [--non-empty] [--freq <vi_tri>]",
    category="pipe",
    minimum_role="USER",
    value_flags=["freq"],
    boolean_flags=["lines", "words", "chars", "unique", "non-empty"],
    examples=[
        ("my quizzes | count", "Dem so dong (so quiz)"),
        ("my quizzes | count --words", "Dem tong so tu"),
        ("my quizzes | count --unique", "Dem so dong duy nhat"),
        ("log --limit 100 | count --non-empty", "Dem dong khong rong"),
        ("ls -u | count --freq 3", "Thong ke tan suat truong thu 3"),
    ],
)

LOG = CommandInfo(
    name="log",
    aliases=["logs"],
    description="Xem va loc nhat ky ung dung",
    usage=(
        "log [--level <muc>] [--tag <the>] [--regex <mau>] "
        "[--limit <so>] [--since <thoi_gian>] [--format <dinh_dang>] [--export]"
    ),
    category="pipe",
    minimum_role="USER",
    value_flags=["level", "tag", "regex", "limit", "since", "format"],
    boolean_flags=["export"],
    examples=[
        ("log", "Hien thi nhat ky gan nhat"),
        ("log --level error", "Chi hien thi log loi"),
        ("log --tag SyncManager", "Loc log theo tag"),
        ("log --limit 50 --since 1h", "50 dong log trong 1 gio qua"),
        ("log --export", "Xuat log ra dinh dang van ban"),
        ("log --regex 'Exception.*null'", "Loc log theo regex"),
    ],
)

# ---------------------------------------------------------------------------
# admin category
# ---------------------------------------------------------------------------

BAN = CommandInfo(
    name="ban",
    aliases=[],
    description="Cam nguoi dung",
    usage="ban <email|id> [--reason <ly_do>] [--confirm]",
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
        ("ban user@example.com --confirm", "Cam nguoi dung theo email"),
        ("ban user@example.com --reason 'Vi pham' --confirm", "Cam voi ly do cu the"),
        ("ban --role USER --dry-run", "Xem truoc nguoi dung se bi cam"),
        ("ban user1@ex.com user2@ex.com --confirm", "Cam nhieu nguoi dung cung luc"),
    ],
)

UNBAN = CommandInfo(
    name="unban",
    aliases=[],
    description="Go cam nguoi dung",
    usage="unban <email|id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="BAN_USERS",
    boolean_flags=["confirm", "verbose", "quiet"],
    examples=[
        ("unban user@example.com --confirm", "Go cam nguoi dung"),
    ],
)

ROLE = CommandInfo(
    name="role",
    aliases=[],
    description="Thay doi vai tro nguoi dung",
    usage="role <email|id> <vai_tro> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="CHANGE_USER_ROLES",
    boolean_flags=["confirm"],
    examples=[
        ("role user@example.com ADMIN --confirm", "Thang cap nguoi dung len ADMIN"),
        ("role user@example.com USER --confirm", "Ha cap nguoi dung ve USER"),
    ],
)

PERM = CommandInfo(
    name="perm",
    aliases=["permission"],
    description="Quan ly quyen han quan tri",
    usage="perm <list|show|grant|revoke> [<email|id>] [<quyen>] [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="MANAGE_PERMISSIONS",
    boolean_flags=["confirm"],
    examples=[
        ("perm list", "Liet ke tat ca quyen han"),
        ("perm show admin@example.com", "Xem quyen cua nguoi dung"),
        ("perm grant admin@ex.com BAN_USERS --confirm", "Cap quyen BAN_USERS"),
        (
            "perm revoke admin@ex.com DELETE_USERS --confirm",
            "Thu hoi quyen DELETE_USERS",
        ),
    ],
)

USERINFO = CommandInfo(
    name="userinfo",
    aliases=["uinfo"],
    description="Xem thong tin chi tiet nguoi dung",
    usage="userinfo <email|id>",
    category="admin",
    minimum_role="ADMIN",
    required_permission="MANAGE_USERS",
    value_flags=["format"],
    examples=[
        ("userinfo user@example.com", "Xem thong tin nguoi dung theo email"),
        ("userinfo abc123", "Xem thong tin nguoi dung theo ID"),
    ],
)

DEL = CommandInfo(
    name="del",
    aliases=["delete"],
    description="Xoa doi tuong",
    usage="del <-u|-q|-a|-p> <id> [--confirm] [--dry-run]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    boolean_flags=["confirm", "dry-run", "u", "q", "a", "p"],
    examples=[
        ("del -u user123 --confirm", "Xoa nguoi dung theo ID"),
        ("del -q quiz456 --confirm", "Xoa bai kiem tra theo ID"),
        ("del -a attempt789 --confirm", "Xoa lan lam bai theo ID"),
        ("del -p pool012 --confirm", "Xoa cau hoi pool theo ID"),
        ("del -q quiz456 --dry-run", "Xem truoc viec xoa (khong thuc su xoa)"),
    ],
)

QUIZINFO = CommandInfo(
    name="quizinfo",
    aliases=["qi"],
    description="Xem thong tin chi tiet bai kiem tra",
    usage="quizinfo <id> [--questions] [--attempts] [--stats]",
    category="admin",
    minimum_role="USER",
    boolean_flags=["questions", "attempts", "stats"],
    examples=[
        ("quizinfo quiz123", "Xem thong tin co ban cua quiz"),
        ("quizinfo quiz123 --questions", "Xem kem danh sach cau hoi"),
        ("quizinfo quiz123 --attempts", "Xem kem cac lan lam bai"),
        ("quizinfo quiz123 --stats", "Xem kem thong ke"),
        ("quizinfo quiz123 --questions --stats", "Xem cau hoi va thong ke"),
    ],
)

PUBLISH = CommandInfo(
    name="publish",
    aliases=["pub"],
    description="Xuat ban bai kiem tra",
    usage="publish <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="PUBLISH_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("publish quiz123 --confirm", "Xuat ban bai kiem tra"),
    ],
)

UNPUBLISH = CommandInfo(
    name="unpublish",
    aliases=["unpub"],
    description="Huy xuat ban bai kiem tra",
    usage="unpublish <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="PUBLISH_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("unpublish quiz123 --confirm", "Huy xuat ban bai kiem tra"),
    ],
)

RESTORE = CommandInfo(
    name="restore",
    aliases=[],
    description="Khoi phuc bai kiem tra da xoa",
    usage="restore <id> [--confirm]",
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    required_permission="DELETE_QUIZZES",
    boolean_flags=["confirm"],
    examples=[
        ("restore quiz123 --confirm", "Khoi phuc bai kiem tra da xoa"),
    ],
)

LS = CommandInfo(
    name="ls",
    aliases=["list"],
    description="Liet ke doi tuong",
    usage=(
        "ls <-u|-q|-a|-p> [--limit <so>] [--offset <so>] "
        "[--sort <truong>] [--order <asc|desc>] [--format <dinh_dang>] [--filter <dieu_kien>]"
    ),
    category="admin",
    minimum_role="ADMIN",
    value_flags=["limit", "offset", "sort", "order", "format", "filter", "role"],
    boolean_flags=["u", "q", "a", "p"],
    examples=[
        ("ls -u", "Liet ke nguoi dung"),
        ("ls -q --limit 10", "Liet ke 10 bai kiem tra"),
        ("ls -a --sort date --order desc", "Liet ke lan lam bai sap xep theo ngay"),
        ("ls -u --format json", "Liet ke nguoi dung dang JSON"),
        ("ls -u --filter 'role=ADMIN'", "Loc nguoi dung theo vai tro"),
        ("ls -p --limit 20 --offset 10", "Liet ke pool voi phan trang"),
    ],
)

STATS = CommandInfo(
    name="stats",
    aliases=["statistics"],
    description="Thong ke he thong",
    usage="stats [--format <dinh_dang>]",
    category="admin",
    minimum_role="ADMIN",
    required_permission="VIEW_REPORTS",
    value_flags=["format"],
    examples=[
        ("stats", "Xem thong ke he thong tong quan"),
        ("stats --format json", "Xuat thong ke dang JSON"),
    ],
)

SEARCH = CommandInfo(
    name="search",
    aliases=["find", "s"],
    description="Tim kiem nguoi dung hoac bai kiem tra",
    usage="search <-u|-q> <tu_khoa> [--exact] [--regex] [--limit <so>] [--format <dinh_dang>]",
    category="admin",
    minimum_role="USER",
    value_flags=["limit", "format"],
    boolean_flags=["u", "q", "exact", "regex"],
    examples=[
        ("search -u john", "Tim nguoi dung co ten 'john'"),
        ("search -q kotlin", "Tim bai kiem tra co ten 'kotlin'"),
        ("search -u --exact admin@example.com", "Tim chinh xac theo email"),
        ("search -q --regex '^Test.*2024'", "Tim quiz bang regex"),
        ("search -q kotlin --limit 5 --format table", "Tim 5 quiz dang bang"),
    ],
)

EXPORT = CommandInfo(
    name="export",
    aliases=["exp"],
    description="Xuat du lieu",
    usage="export <users|quizzes|attempts|stats|logs> [--format <csv|json|table>] [--limit <so>]",
    category="admin",
    minimum_role="ADMIN",
    required_permission="VIEW_REPORTS",
    value_flags=["format", "limit"],
    examples=[
        ("export users", "Xuat danh sach nguoi dung"),
        ("export quizzes --format json", "Xuat quiz dang JSON"),
        ("export stats --format csv", "Xuat thong ke dang CSV"),
        ("export logs --limit 100", "Xuat 100 dong log gan nhat"),
        ("export attempts --format table", "Xuat lan lam bai dang bang"),
    ],
)

PURGE = CommandInfo(
    name="purge",
    aliases=["cleanup"],
    description="Don dep du lieu",
    usage=(
        "purge <trash|inactive|old-attempts|orphans|banned|empty-quizzes> "
        "[--before <ngay>] [--confirm] [--dry-run]"
    ),
    category="admin",
    minimum_role="ADMIN",
    is_destructive=True,
    value_flags=["before"],
    boolean_flags=["confirm", "dry-run"],
    examples=[
        ("purge trash --confirm", "Xoa vinh vien cac muc trong thung rac"),
        (
            "purge inactive --before 2024-01-01 --dry-run",
            "Xem truoc nguoi dung khong hoat dong",
        ),
        ("purge old-attempts --before 2023-06-01 --confirm", "Xoa lan lam bai cu"),
        ("purge orphans --dry-run", "Tim cau hoi mo coi (khong thuoc quiz nao)"),
        ("purge banned --confirm", "Xoa du lieu cua nguoi dung bi cam"),
        ("purge empty-quizzes --confirm", "Xoa quiz khong co cau hoi"),
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
        return "(khong co lenh nao)"

    lines: list[str] = []
    # Header
    if show_category:
        lines.append(f"{'Lenh':<16} {'Danh muc':<10} {'Mo ta'}")
        lines.append(f"{'----':<16} {'--------':<10} {'----'}")
    else:
        lines.append(f"{'Lenh':<16} {'Mo ta'}")
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
    lines.append(f"  Mo ta:     {cmd.description}")
    lines.append(f"  Su dung:   {cmd.usage}")
    lines.append(f"  Danh muc:  {cmd.category}")
    lines.append(f"  Vai tro:   {cmd.minimum_role}")

    if cmd.aliases:
        lines.append(f"  Bi danh:   {', '.join(cmd.aliases)}")

    if cmd.is_destructive:
        lines.append(
            "  Canh bao:  Lenh nay co tac dong KHONG THE HOAN TAC (can --confirm)"
        )

    if cmd.required_permission:
        lines.append(f"  Quyen:     {cmd.required_permission}")

    if cmd.value_flags or cmd.boolean_flags:
        lines.append("")
        lines.append("  Co (flags):")
        for f in cmd.value_flags:
            lines.append(f"    --{f} <gia_tri>")
        for f in cmd.boolean_flags:
            lines.append(f"    --{f}")

    if cmd.examples:
        lines.append("")
        lines.append("  Vi du:")
        for example_cmd, example_desc in cmd.examples:
            lines.append(f"    $ {example_cmd}")
            lines.append(f"      {example_desc}")

    return "\n".join(lines)
