#!/usr/bin/env python3
"""
generate-sample-data.py -- Comprehensive sample data generator for Quizzez Android app.

Generates realistic Vietnamese-language quiz data and writes it directly to
Firestore (either the emulator or a live project). Designed for stress testing
the app and its Firebase backend.

Usage:
    # Against the local emulator (default):
    python generate-sample-data.py

    # Against a live project with service account credentials:
    python generate-sample-data.py --project-id my-project --credentials sa.json

    # Clean existing data first, then generate 200-scale data:
    python generate-sample-data.py --clean --count 200

Requirements:
    pip install -r requirements.txt
"""

import argparse
import hashlib
import os
import random
import string
import sys
import time
import uuid
from datetime import datetime, timezone


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate sample data for the Quizzez Android app Firestore backend.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate default data against local emulator:
  python generate-sample-data.py

  # Generate 200-scale data, cleaning first:
  python generate-sample-data.py --count 200 --clean

  # Against a live Firebase project:
  python generate-sample-data.py --project-id quizzez-prod --credentials sa-key.json
        """,
    )
    parser.add_argument(
        "--project-id",
        default=None,
        help="Firebase project ID. Required for live projects; auto-detected for emulator.",
    )
    parser.add_argument(
        "--credentials",
        default=None,
        help="Path to service account JSON key file. If omitted, uses the Firestore emulator.",
    )
    parser.add_argument(
        "--emulator-host",
        default="localhost:8080",
        help="Firestore emulator host:port (default: localhost:8080).",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Delete all existing data before generating new data.",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=100,
        help="Scale factor for data generation (default: 100). Controls the base number of quizzes.",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="Random seed for reproducible data generation.",
    )
    return parser.parse_args()


# Call parse_args early so --help works without dependencies
if "-h" in sys.argv or "--help" in sys.argv:
    parse_args()

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
    from google.auth.credentials import AnonymousCredentials
except ImportError:
    print("ERROR: firebase-admin package is required.")
    print("Install it with: pip install firebase-admin")
    sys.exit(1)


class _EmulatorCredential(credentials.Base):
    """Minimal credential that wraps google.auth AnonymousCredentials.

    Used when talking to the Firestore emulator so the SDK never
    attempts to locate real Google Cloud credentials.
    """

    def get_credential(self):
        return AnonymousCredentials()


# ---------------------------------------------------------------------------
# Firestore collection names (mirrors FirestoreCollections.kt)
# ---------------------------------------------------------------------------
COLLECTION_USERS = "users"
COLLECTION_QUIZZES = "quizzes"
COLLECTION_QUESTIONS = "questions"
COLLECTION_CHOICES = "choices"
COLLECTION_ATTEMPTS = "attempts"
COLLECTION_SHARE_CODES = "shareCodes"
COLLECTION_QUESTION_POOL = "questionPool"
COLLECTION_QUIZ_DELETIONS = "quizDeletions"

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000
NOW_MS = int(time.time() * 1000)
BATCH_LIMIT = 400  # Firestore batch write limit is 500; stay well below

# ---------------------------------------------------------------------------
# Vietnamese content pools
# ---------------------------------------------------------------------------
TAGS = [
    "Toan hoc",
    "Khoa hoc",
    "Lich su",
    "Dia ly",
    "Van hoc",
    "Tieng Anh",
    "Tin hoc",
    "Am nhac",
    "The thao",
    "Phim anh",
    "Vat ly",
    "Hoa hoc",
    "Sinh hoc",
    "Kinh te",
    "Triet hoc",
    "Nghe thuat",
    "Cong nghe",
    "Y hoc",
    "Phap luat",
    "Xa hoi hoc",
]

FIRST_NAMES = [
    "Minh",
    "Hoa",
    "Tuan",
    "Lan",
    "Duc",
    "Mai",
    "Hung",
    "Linh",
    "Nam",
    "Phuong",
    "Thanh",
    "Huong",
    "Quang",
    "Thao",
    "Long",
    "Ngoc",
    "Khanh",
    "Yen",
    "Hai",
    "Trang",
    "Dung",
    "Van",
    "Binh",
    "Anh",
    "Son",
    "Thu",
    "Phuc",
    "Nhi",
    "Dat",
    "Uyen",
]

LAST_NAMES = [
    "Nguyen",
    "Tran",
    "Le",
    "Pham",
    "Hoang",
    "Huynh",
    "Phan",
    "Vu",
    "Vo",
    "Dang",
    "Bui",
    "Do",
    "Ho",
    "Ngo",
    "Duong",
    "Ly",
    "Trinh",
    "Luong",
    "Dinh",
    "Lam",
]

QUIZ_TITLE_TEMPLATES = [
    "Kiem tra kien thuc {tag} co ban",
    "Trac nghiem {tag} nang cao",
    "Bai tap {tag} tong hop",
    "On tap {tag} cuoi ky",
    "Kham pha {tag} qua cau hoi",
    "Thach thuc {tag} cho ban",
    "Hoc vui voi {tag}",
    "200 cau hoi {tag} hay nhat",
    "Luyen tap {tag} moi ngay",
    "Trac nghiem nhanh {tag}",
    "Sieu tri tue: {tag}",
    "Ai la trieu phu: {tag}",
    "Thu tai {tag} cua ban",
    "Cau hoi kho ve {tag}",
    "Chien thang {tag}",
    "{tag} tu A den Z",
    "Bai kiem tra {tag} so 1",
    "Nang cao trinh do {tag}",
    "Do vui {tag} cuoi tuan",
    "Thi thu {tag} online",
]

QUIZ_DESCRIPTIONS = [
    "Bai kiem tra giup ban danh gia kien thuc tong quan ve chu de nay.",
    "Tap hop cac cau hoi trac nghiem tu co ban den nang cao.",
    "Luyen tap va on tap kien thuc mot cach hieu qua.",
    "Thach thuc ban than voi nhung cau hoi thu vi va bo ich.",
    "Danh cho nhung ai muon kiem tra va nang cao trinh do.",
    "Bo cau hoi duoc chon loc tu nhieu nguon uy tin.",
    "Hoc tap qua hinh thuc trac nghiem sinh dong.",
    "Cau hoi da dang, phu hop voi moi trinh do.",
    None,  # Some quizzes have no description
    None,
]

QUESTION_TEMPLATES = [
    "Dau la {concept} dung trong {tag}?",
    "Chon phuong an dung nhat ve {concept} trong linh vuc {tag}.",
    "{concept} nao sau day la chinh xac?",
    "Tinh chat nao thuoc ve {concept}?",
    "Khi nao {concept} xay ra trong {tag}?",
    "Ai la nguoi phat hien ra {concept}?",
    "Dac diem nao mo ta dung nhat ve {concept}?",
    "{concept} co bao nhieu loai chinh trong {tag}?",
    "Trong {tag}, {concept} duoc su dung de lam gi?",
    "Menh de nao ve {concept} la SAI?",
    "Cau nao sau day KHONG dung ve {concept}?",
    "Theo ly thuyet {tag}, {concept} la gi?",
    "Ung dung cua {concept} trong thuc te la gi?",
    "Y nghia cua {concept} doi voi {tag} la gi?",
    "So sanh {concept} voi cac khai niem tuong tu trong {tag}.",
]

CONCEPTS = [
    "khai niem co ban",
    "dinh ly chinh",
    "phuong phap giai",
    "cong thuc quan trong",
    "nguyen ly hoat dong",
    "quy tac ap dung",
    "dac diem noi bat",
    "phan loai chinh",
    "lich su phat trien",
    "ung dung thuc tien",
    "moi quan he",
    "yeu to anh huong",
    "qua trinh bien doi",
    "dieu kien can thiet",
    "ket qua thi nghiem",
    "ly thuyet nen tang",
    "mo hinh tieu bieu",
    "he thong phan cap",
    "phuong trinh co ban",
    "bien so quan trong",
]

CORRECT_CHOICE_TEMPLATES = [
    "Day la dap an dung cho cau hoi nay",
    "Phuong an chinh xac nhat",
    "Dap an A - phuong an dung",
    "Ket qua chinh xac theo ly thuyet",
    "Lua chon dung nhat trong cac phuong an",
]

WRONG_CHOICE_TEMPLATES = [
    "Phuong an nay khong chinh xac",
    "Day khong phai la dap an dung",
    "Lua chon sai - khong phu hop voi de bai",
    "Dap an nay chua chinh xac",
    "Phuong an gay nhieu nhung khong dung",
    "Cau tra loi khong dung voi kien thuc da hoc",
    "Day la mot phuong an nhieu, khong phai dap an",
    "Ket qua nay chi dung trong truong hop dac biet",
]

EXPLANATIONS = [
    "Dap an dung vi day la dinh nghia co ban trong linh vuc nay.",
    "Theo tai lieu tham khao, phuong an nay la chinh xac nhat.",
    "Day la kien thuc nen tang can nho.",
    "Giai thich: dua tren nguyen ly co ban da duoc chung minh.",
    "Cau tra loi dung vi no phu hop voi tat ca cac dieu kien da cho.",
    None,  # Some questions have no explanation
    None,
    None,
]

AVATAR_URLS = [
    "https://api.dicebear.com/7.x/avataaars/svg?seed={seed}",
    "https://api.dicebear.com/7.x/bottts/svg?seed={seed}",
    "https://api.dicebear.com/7.x/pixel-art/svg?seed={seed}",
    "https://api.dicebear.com/7.x/identicon/svg?seed={seed}",
]

THUMBNAIL_URLS = [
    "https://picsum.photos/seed/{seed}/400/300",
    "https://picsum.photos/seed/{seed}/600/400",
    None,  # Many quizzes have no thumbnail
    None,
    None,
]


# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------


def gen_uuid():
    """Generate a UUID4 string."""
    return str(uuid.uuid4())


def gen_share_code():
    """Generate a 6-character alphanumeric share code (uppercase + digits)."""
    chars = string.ascii_uppercase + string.digits
    return "".join(random.choices(chars, k=6))


def random_timestamp_last_n_days(n=30):
    """Return a random millisecond timestamp within the last n days."""
    offset_ms = random.randint(0, n * 24 * 60 * 60 * 1000)
    return NOW_MS - offset_ms


def ms_to_firestore_timestamp(ms):
    """Convert a millisecond timestamp to a Firestore-compatible datetime."""
    return datetime.fromtimestamp(ms / 1000.0, tz=timezone.utc)


def compute_quiz_checksum(title, description, questions_data):
    """
    Replicate ChecksumUtil.computeQuizChecksum from the Kotlin codebase.

    Computes SHA-256 over quiz title, description, and ordered question/choice
    content. Questions are sorted by position; choices within each question
    are sorted by position.
    """
    sep = "|"
    parts = []
    parts.append(title)
    parts.append(sep)
    parts.append(description or "")
    parts.append(sep)

    sorted_questions = sorted(questions_data, key=lambda q: q["position"])
    for q in sorted_questions:
        parts.append(q["content"])
        parts.append(sep)
        parts.append(q.get("mediaUrl") or "")
        parts.append(sep)
        parts.append(q.get("explanation") or "")
        parts.append(sep)
        parts.append(str(q.get("points", 1)))
        parts.append(sep)
        parts.append(str(q.get("allowMultipleCorrect", False)).lower())
        parts.append(sep)
        sorted_choices = sorted(q["choices"], key=lambda c: c["position"])
        for c in sorted_choices:
            parts.append(c["content"])
            parts.append(sep)
            parts.append(str(c["isCorrect"]).lower())
            parts.append(sep)

    data = "".join(parts)
    digest = hashlib.sha256(data.encode("utf-8")).hexdigest()
    return digest


def pick_tags(min_count=1, max_count=4):
    """Pick a random subset of tags."""
    count = random.randint(min_count, max_count)
    return random.sample(TAGS, min(count, len(TAGS)))


def make_display_name():
    """Generate a realistic Vietnamese display name."""
    return "{} {}".format(random.choice(LAST_NAMES), random.choice(FIRST_NAMES))


def make_username(display_name, index):
    """Generate a username from display name."""
    parts = display_name.lower().split()
    base = "".join(parts)
    return "{}{:03d}".format(base, index)


def make_email(username):
    """Generate a plausible email from username."""
    domains = [
        "gmail.com",
        "yahoo.com",
        "outlook.com",
        "edu.vn",
        "student.hcmus.edu.vn",
    ]
    return "{}@{}".format(username, random.choice(domains))


# ---------------------------------------------------------------------------
# Data generators
# ---------------------------------------------------------------------------


def generate_users(count):
    """Generate user data dicts ready for Firestore."""
    users = []
    # Ensure at least 2 admins
    admin_count = max(2, count // 10)
    banned_count = max(1, count // 20)

    for i in range(count):
        uid = gen_uuid()
        display_name = make_display_name()
        username = make_username(display_name, i)
        email = make_email(username)
        created_at_ms = random_timestamp_last_n_days(30)
        updated_at_ms = created_at_ms + random.randint(0, 5 * 24 * 60 * 60 * 1000)

        if i < admin_count:
            role = "admin"
        else:
            role = "user"

        is_banned = count - banned_count <= i < count

        seed = username
        avatar_template = random.choice(AVATAR_URLS)
        photo_url = avatar_template.format(seed=seed)

        user = {
            "id": uid,
            "email": email,
            "displayName": display_name,
            "username": username,
            "photoUrl": photo_url,
            "role": role,
            "createdAt": ms_to_firestore_timestamp(created_at_ms),
            "updatedAt": ms_to_firestore_timestamp(updated_at_ms),
            "deletedAt": ms_to_firestore_timestamp(NOW_MS) if is_banned else None,
        }
        users.append(user)

    return users


def generate_quizzes(count, users):
    """
    Generate quiz data dicts. Returns (quizzes, all_questions, all_choices).

    Each quiz gets 3-8 questions; each question gets 2-5 choices with at least
    1 correct answer. Quizzes are distributed across users and quiz states.
    """
    # Only non-banned, non-guest users can own quizzes
    eligible_owners = [
        u for u in users if u["role"] in ("user", "admin") and u["deletedAt"] is None
    ]
    if not eligible_owners:
        eligible_owners = users[:1]

    quizzes = []
    all_questions = []  # list of (quiz_id, question_dict)
    all_choices = []  # list of (quiz_id, question_id, choice_dict)

    for i in range(count):
        quiz_id = gen_uuid()
        owner = random.choice(eligible_owners)
        tags = pick_tags()
        primary_tag = tags[0]

        title_template = random.choice(QUIZ_TITLE_TEMPLATES)
        title = title_template.format(tag=primary_tag)
        description = random.choice(QUIZ_DESCRIPTIONS)

        created_at_ms = random_timestamp_last_n_days(30)
        updated_at_ms = created_at_ms + random.randint(0, 3 * 24 * 60 * 60 * 1000)

        # Decide quiz state:
        #   10% draft (not public, no share code, will have deletedAt=None)
        #   10% soft-deleted
        #   30% published private (not public, has share code)
        #   50% published public
        roll = random.random()
        if roll < 0.10:
            is_public = False
            share_code = None
            deleted_at_ms = None
            state_label = "draft"
        elif roll < 0.20:
            is_public = random.choice([True, False])
            share_code = gen_share_code() if random.random() < 0.5 else None
            deleted_at_ms = updated_at_ms + random.randint(
                1000, 2 * 24 * 60 * 60 * 1000
            )
            state_label = "deleted"
        elif roll < 0.50:
            is_public = False
            share_code = gen_share_code()
            deleted_at_ms = None
            state_label = "private"
        else:
            is_public = True
            share_code = gen_share_code() if random.random() < 0.4 else None
            deleted_at_ms = None
            state_label = "public"

        # Generate questions for this quiz
        num_questions = random.randint(3, 8)
        quiz_questions_data = []  # For checksum computation

        for q_idx in range(num_questions):
            question_id = gen_uuid()
            concept = random.choice(CONCEPTS)
            q_template = random.choice(QUESTION_TEMPLATES)
            q_content = q_template.format(concept=concept, tag=primary_tag)
            is_multi_select = random.random() < 0.2
            explanation = random.choice(EXPLANATIONS)
            media_url = None  # Keep it simple; no media for generated data
            points = random.choice([1, 1, 1, 2, 2, 5])

            # Generate choices
            num_choices = random.randint(2, 5)
            num_correct = 1
            if is_multi_select and num_choices >= 3:
                num_correct = random.randint(1, min(3, num_choices - 1))

            choices_data = []
            for c_idx in range(num_choices):
                choice_id = gen_uuid()
                if c_idx < num_correct:
                    c_content = random.choice(CORRECT_CHOICE_TEMPLATES)
                    c_content = "{} (#{})".format(c_content, c_idx + 1)
                    is_correct = True
                else:
                    c_content = random.choice(WRONG_CHOICE_TEMPLATES)
                    c_content = "{} (#{})".format(c_content, c_idx + 1)
                    is_correct = False

                choice_dict = {
                    "id": choice_id,
                    "content": c_content,
                    "isCorrect": is_correct,
                    "position": c_idx,
                }
                choices_data.append(choice_dict)
                all_choices.append((quiz_id, question_id, choice_dict))

            # Shuffle choices so correct ones are not always first
            random.shuffle(choices_data)
            for new_pos, cd in enumerate(choices_data):
                cd["position"] = new_pos

            question_dict = {
                "id": question_id,
                "content": q_content,
                "choices": choices_data,
                "allowMultipleCorrect": is_multi_select,
                "choiceCount": num_choices,
                "explanation": explanation,
                "mediaUrl": media_url,
                "points": points,
                "position": q_idx,
            }
            quiz_questions_data.append(question_dict)
            all_questions.append((quiz_id, question_dict))

        # Compute checksum
        checksum = compute_quiz_checksum(title, description, quiz_questions_data)

        # Thumbnail
        thumb_template = random.choice(THUMBNAIL_URLS)
        thumbnail_url = (
            thumb_template.format(seed=quiz_id[:8]) if thumb_template else None
        )

        quiz = {
            "id": quiz_id,
            "ownerId": owner["id"],
            "title": title,
            "description": description,
            "authorName": owner["displayName"],
            "thumbnailUrl": thumbnail_url,
            "tags": tags,
            "questionCount": num_questions,
            "attemptCount": 0,  # Will be updated after attempts are generated
            "isPublic": is_public,
            "shareCode": share_code,
            "checksum": checksum,
            "createdAt": ms_to_firestore_timestamp(created_at_ms),
            "updatedAt": ms_to_firestore_timestamp(updated_at_ms),
            "deletedAt": ms_to_firestore_timestamp(deleted_at_ms)
            if deleted_at_ms
            else None,
            "_state": state_label,
            "_questions": quiz_questions_data,
        }
        quizzes.append(quiz)

    return quizzes, all_questions, all_choices


def generate_attempts(count, users, quizzes):
    """
    Generate attempt data. Only non-deleted quizzes with questions are eligible.
    Returns list of attempt dicts and a map of quiz_id -> attempt count.
    """
    eligible_quizzes = [
        q for q in quizzes if q["deletedAt"] is None and q["questionCount"] > 0
    ]
    if not eligible_quizzes:
        print("  WARNING: No eligible quizzes for attempts. Skipping.")
        return [], {}

    eligible_users = [u for u in users if u["deletedAt"] is None]
    if not eligible_users:
        eligible_users = users[:1]

    attempts = []
    quiz_attempt_counts = {}

    for _ in range(count):
        attempt_id = gen_uuid()
        quiz = random.choice(eligible_quizzes)
        user = random.choice(eligible_users)
        questions = quiz["_questions"]
        total_questions = len(questions)

        # Simulate a quiz attempt with random answers
        score = 0
        answers = {}
        multi_answers = {}
        question_order = [q["id"] for q in questions]
        random.shuffle(question_order)

        for q in questions:
            q_id = q["id"]
            choices = q["choices"]
            correct_ids = [c["id"] for c in choices if c["isCorrect"]]
            all_choice_ids = [c["id"] for c in choices]

            # Simulate answer selection: 60% chance of getting it right
            if random.random() < 0.6:
                selected = correct_ids[:]
            else:
                num_selected = random.randint(1, min(2, len(all_choice_ids)))
                selected = random.sample(all_choice_ids, num_selected)

            # Check correctness (exact set match, same as ScoreCalculator)
            if set(selected) == set(correct_ids):
                score += 1

            answers[q_id] = selected[0] if selected else ""
            multi_answers[q_id] = selected

        # Generate realistic timestamps
        start_ms = random_timestamp_last_n_days(25)
        duration_ms = random.randint(30_000, 30 * 60_000)  # 30s to 30min
        end_ms = start_ms + duration_ms

        attempt = {
            "id": attempt_id,
            "userId": user["id"],
            "quizId": quiz["id"],
            "questionOrder": question_order,
            "choiceOrders": {},
            "answers": {k: v for k, v in answers.items()},
            "multiAnswers": multi_answers,
            "score": score,
            "maxScore": total_questions,
            "startedAt": ms_to_firestore_timestamp(start_ms),
            "finishedAt": ms_to_firestore_timestamp(end_ms),
        }
        attempts.append(attempt)

        quiz_attempt_counts[quiz["id"]] = quiz_attempt_counts.get(quiz["id"], 0) + 1

    return attempts, quiz_attempt_counts


def generate_share_codes(count, quizzes):
    """Generate share code documents for published (non-draft, non-deleted) quizzes."""
    eligible = [
        q for q in quizzes if q["deletedAt"] is None and q["shareCode"] is not None
    ]
    if not eligible:
        print("  WARNING: No eligible quizzes for share codes. Skipping.")
        return []

    codes = []
    used_codes = set()

    for i in range(min(count, len(eligible))):
        quiz = eligible[i % len(eligible)]
        code = quiz["shareCode"]
        if code in used_codes:
            code = gen_share_code()
        used_codes.add(code)

        # 70% of share codes do not expire; 30% expire in 7-30 days
        if random.random() < 0.3:
            expires_ms = NOW_MS + random.randint(7, 30) * 24 * 60 * 60 * 1000
            expires_at = ms_to_firestore_timestamp(expires_ms)
        else:
            expires_at = None

        share_code = {
            "code": code,
            "quizId": quiz["id"],
            "expiresAt": expires_at,
        }
        codes.append(share_code)

    # Generate additional codes if needed (for quizzes not yet covered)
    remaining = count - len(codes)
    for _ in range(remaining):
        quiz = random.choice(eligible)
        code = gen_share_code()
        while code in used_codes:
            code = gen_share_code()
        used_codes.add(code)

        if random.random() < 0.3:
            expires_ms = NOW_MS + random.randint(7, 30) * 24 * 60 * 60 * 1000
            expires_at = ms_to_firestore_timestamp(expires_ms)
        else:
            expires_at = None

        share_code = {
            "code": code,
            "quizId": quiz["id"],
            "expiresAt": expires_at,
        }
        codes.append(share_code)

    return codes


def generate_question_pool_items(count, users, quizzes):
    """Generate question pool item documents from existing quiz questions."""
    eligible_quizzes = [q for q in quizzes if q["_questions"]]
    eligible_users = [u for u in users if u["deletedAt"] is None]
    if not eligible_quizzes or not eligible_users:
        return []

    pool_items = []
    for _ in range(count):
        pool_id = gen_uuid()
        quiz = random.choice(eligible_quizzes)
        question = random.choice(quiz["_questions"])
        contributor = random.choice(eligible_users)

        # Flatten choices to PoolChoiceDto format
        pool_choices = []
        correct_indices = []
        for idx, c in enumerate(
            sorted(question["choices"], key=lambda x: x["position"])
        ):
            pool_choices.append(
                {
                    "content": c["content"],
                    "isCorrect": c["isCorrect"],
                }
            )
            if c["isCorrect"]:
                correct_indices.append(idx)

        tags = pick_tags(1, 3)
        created_ms = random_timestamp_last_n_days(20)

        # 10% chance of anonymous contribution, 15% chance of inactive
        contributor_id = None if random.random() < 0.1 else contributor["id"]
        is_active = random.random() > 0.15

        pool_item = {
            "id": pool_id,
            "content": question["content"],
            "choices": pool_choices,
            "correctIndices": correct_indices,
            "tags": tags,
            "mediaUrl": None,
            "points": random.choice([1, 2, 5, 5, 10]),
            "allowMultipleCorrect": question["allowMultipleCorrect"],
            "contributorId": contributor_id,
            "sourceQuizId": quiz["id"],
            "isActive": is_active,
            "usageCount": random.randint(0, 50),
            "createdAt": ms_to_firestore_timestamp(created_ms),
        }
        pool_items.append(pool_item)

    return pool_items


# ---------------------------------------------------------------------------
# Firestore write helpers
# ---------------------------------------------------------------------------


def _strip_doc_id(data, id_field="id"):
    """Return a shallow copy of *data* without the ``@DocumentId`` field.

    The Kotlin Firestore SDK annotates certain DTO properties with
    ``@DocumentId``, which tells the SDK to populate them from the
    document path and **exclude** them from the document body when
    writing.  When reading, the SDK throws a ``RuntimeException``
    if the document body contains a field whose name matches a
    ``@DocumentId``-annotated property.

    Because the Python Admin SDK has no knowledge of ``@DocumentId``,
    we must strip these fields manually before calling ``set()``.
    """
    return {k: v for k, v in data.items() if k != id_field}


def delete_collection(db, collection_path, batch_size=400):
    """Delete all documents in a collection (non-recursive)."""
    coll_ref = db.collection(collection_path)
    deleted = 0

    while True:
        docs = list(coll_ref.limit(batch_size).stream())
        if not docs:
            break
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
            deleted += 1
        batch.commit()

    return deleted


def delete_all_data(db):
    """Delete all documents from every known collection."""
    print("\n--- Cleaning existing data ---")
    collections = [
        COLLECTION_USERS,
        COLLECTION_ATTEMPTS,
        COLLECTION_SHARE_CODES,
        COLLECTION_QUESTION_POOL,
        COLLECTION_QUIZ_DELETIONS,
    ]

    total_deleted = 0
    for coll_name in collections:
        count = delete_collection(db, coll_name)
        if count > 0:
            print("  Deleted {} documents from '{}'".format(count, coll_name))
        total_deleted += count

    # Quizzes require recursive deletion (questions -> choices subcollections)
    quiz_deleted = 0
    quiz_docs = list(db.collection(COLLECTION_QUIZZES).stream())
    for quiz_doc in quiz_docs:
        quiz_ref = quiz_doc.reference
        # Delete choices subcollections under each question
        question_docs = list(quiz_ref.collection(COLLECTION_QUESTIONS).stream())
        for q_doc in question_docs:
            choice_docs = list(q_doc.reference.collection(COLLECTION_CHOICES).stream())
            if choice_docs:
                batch = db.batch()
                for c_doc in choice_docs:
                    batch.delete(c_doc.reference)
                    total_deleted += 1
                batch.commit()
            q_doc.reference.delete()
            total_deleted += 1
        quiz_ref.delete()
        quiz_deleted += 1
        total_deleted += 1

    if quiz_deleted > 0:
        print(
            "  Deleted {} quiz trees from '{}'".format(quiz_deleted, COLLECTION_QUIZZES)
        )

    print("  Total documents deleted: {}".format(total_deleted))


def write_users(db, users):
    """Write user documents to Firestore."""
    print("\n  Writing {} users...".format(len(users)))
    written = 0
    batch = db.batch()
    batch_count = 0

    for user in users:
        doc_ref = db.collection(COLLECTION_USERS).document(user["id"])
        # Strip "id" -- @DocumentId on UserDto
        doc_data = _strip_doc_id(user, "id")
        batch.set(doc_ref, doc_data)
        batch_count += 1
        written += 1

        if batch_count >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print("    -> {} users written".format(written))
    return written


def write_quizzes_with_questions(db, quizzes):
    """
    Write quiz documents with their questions and choices as subcollections.

    Firestore structure:
      quizzes/{quizId}
        questions/{questionId}
          choices/{choiceId}
    """
    print("\n  Writing {} quizzes with questions and choices...".format(len(quizzes)))
    quiz_count = 0
    question_count = 0
    choice_count = 0

    for quiz in quizzes:
        quiz_id = quiz["id"]
        questions_data = quiz.pop("_questions", [])
        state_label = quiz.pop("_state", "unknown")

        # Write quiz document -- strip "id" (@DocumentId on QuizDto)
        quiz_ref = db.collection(COLLECTION_QUIZZES).document(quiz_id)
        quiz_ref.set(_strip_doc_id(quiz, "id"))
        quiz_count += 1

        # Write questions and choices as subcollections
        for q_data in questions_data:
            q_id = q_data["id"]
            choices = q_data.pop("choices", [])

            # Write question document (without embedded choices for subcollection model)
            q_ref = quiz_ref.collection(COLLECTION_QUESTIONS).document(q_id)

            # Store the question with an empty choices list (choices go in subcollection)
            # Strip "id" -- @DocumentId on QuestionDto
            q_doc_data = _strip_doc_id(q_data, "id")
            q_doc_data["choices"] = []
            q_ref.set(q_doc_data)
            question_count += 1

            # Write choices as subcollection
            if choices:
                batch = db.batch()
                for c_data in choices:
                    c_id = c_data["id"]
                    c_ref = q_ref.collection(COLLECTION_CHOICES).document(c_id)
                    # Strip "id" -- @DocumentId on ChoiceDto
                    batch.set(c_ref, _strip_doc_id(c_data, "id"))
                    choice_count += 1
                batch.commit()

            # Restore choices for other uses (like pool items referencing this quiz)
            q_data["choices"] = choices

        # Restore _questions for potential later use
        quiz["_questions"] = questions_data
        quiz["_state"] = state_label

        if quiz_count % 10 == 0:
            print("    -> Progress: {}/{} quizzes".format(quiz_count, len(quizzes)))

    print(
        "    -> {} quizzes, {} questions, {} choices written".format(
            quiz_count, question_count, choice_count
        )
    )
    return quiz_count, question_count, choice_count


def write_attempts(db, attempts):
    """Write attempt documents to Firestore."""
    print("\n  Writing {} attempts...".format(len(attempts)))
    written = 0
    batch = db.batch()
    batch_count = 0

    for attempt in attempts:
        doc_ref = db.collection(COLLECTION_ATTEMPTS).document(attempt["id"])
        # Strip "id" -- @DocumentId in AttemptDto
        batch.set(doc_ref, _strip_doc_id(attempt, "id"))
        batch_count += 1
        written += 1

        if batch_count >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print("    -> {} attempts written".format(written))
    return written


def write_share_codes(db, share_codes):
    """Write share code documents (document ID = the code itself)."""
    print("\n  Writing {} share codes...".format(len(share_codes)))
    written = 0
    batch = db.batch()
    batch_count = 0

    for sc in share_codes:
        code = sc["code"]
        doc_ref = db.collection(COLLECTION_SHARE_CODES).document(code)
        # Strip "code" -- @DocumentId in ShareCodeDto
        batch.set(doc_ref, _strip_doc_id(sc, "code"))
        batch_count += 1
        written += 1

        if batch_count >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print("    -> {} share codes written".format(written))
    return written


def write_question_pool(db, pool_items):
    """Write question pool item documents."""
    print("\n  Writing {} question pool items...".format(len(pool_items)))
    written = 0
    batch = db.batch()
    batch_count = 0

    for item in pool_items:
        doc_ref = db.collection(COLLECTION_QUESTION_POOL).document(item["id"])
        # Strip "id" -- @DocumentId in QuestionPoolItemDto
        batch.set(doc_ref, _strip_doc_id(item, "id"))
        batch_count += 1
        written += 1

        if batch_count >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print("    -> {} pool items written".format(written))
    return written


def update_quiz_attempt_counts(db, quizzes, quiz_attempt_counts):
    """Update attemptCount on quiz documents after attempts have been generated."""
    if not quiz_attempt_counts:
        return

    print(
        "\n  Updating attempt counts on {} quizzes...".format(len(quiz_attempt_counts))
    )
    batch = db.batch()
    batch_count = 0

    for quiz_id, count in quiz_attempt_counts.items():
        doc_ref = db.collection(COLLECTION_QUIZZES).document(quiz_id)
        batch.update(doc_ref, {"attemptCount": count})
        batch_count += 1

        if batch_count >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print("    -> Updated {} quizzes".format(len(quiz_attempt_counts)))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def _detect_project_id():
    """Try to read the project ID from .firebaserc in the repo root.

    Falls back to 'quizzez-emulator' if the file is missing or unparseable.
    """
    import json as _json

    # The script lives in scripts/, so .firebaserc is one level up.
    firebaserc_path = os.path.join(os.path.dirname(__file__), "..", ".firebaserc")
    try:
        with open(firebaserc_path, "r") as f:
            data = _json.load(f)
        projects = data.get("projects", {})
        # Pick the first project ID found (e.g. "staging" -> "quizzgenandroid").
        if projects:
            detected = next(iter(projects.values()))
            print("Auto-detected project ID from .firebaserc: {}".format(detected))
            return detected
    except (OSError, ValueError, StopIteration):
        pass
    return "quizzez-emulator"


def init_firestore(args):
    """Initialize Firebase Admin SDK and return a Firestore client."""
    if args.credentials:
        # Live project with service account
        cred = credentials.Certificate(args.credentials)
        project_id = args.project_id
        if not project_id:
            # Try to extract from the service account JSON
            import json

            with open(args.credentials, "r") as f:
                sa_data = json.load(f)
            project_id = sa_data.get("project_id")

        if not project_id:
            print("ERROR: --project-id is required when using --credentials")
            sys.exit(1)

        app = firebase_admin.initialize_app(cred, {"projectId": project_id})
        print("Connected to LIVE Firestore project: {}".format(project_id))
    else:
        # Emulator mode -- use anonymous credentials so the SDK does not
        # attempt to locate real Google Cloud credentials.
        os.environ["FIRESTORE_EMULATOR_HOST"] = args.emulator_host
        project_id = args.project_id or _detect_project_id()

        anon_cred = _EmulatorCredential()
        app = firebase_admin.initialize_app(anon_cred, {"projectId": project_id})
        print("Connected to Firestore EMULATOR at: {}".format(args.emulator_host))
        print("Project ID: {}".format(project_id))

    return firestore.client()


def compute_counts(base_count):
    """
    Compute the number of each entity to generate based on the base count.

    The base count controls the number of quizzes. Other entities scale
    proportionally to produce realistic data distributions.
    """
    quiz_count = max(base_count, 10)
    user_count = max(base_count // 5, 20)
    # Questions and choices are generated per-quiz, not controlled here
    attempt_count = max(base_count * 2, 100)
    share_code_count = max(base_count // 5, 20)
    pool_count = max(base_count // 3, 30)

    return {
        "users": user_count,
        "quizzes": quiz_count,
        "attempts": attempt_count,
        "share_codes": share_code_count,
        "pool_items": pool_count,
    }


def main():
    args = parse_args()

    if args.seed is not None:
        random.seed(args.seed)
        print("Using random seed: {}".format(args.seed))

    counts = compute_counts(args.count)

    print("=" * 60)
    print("Quizzez Sample Data Generator")
    print("=" * 60)
    print("\nPlanned generation:")
    print("  Users:              {}".format(counts["users"]))
    print("  Quizzes:            {}".format(counts["quizzes"]))
    print("  Questions per quiz: 3-8 (randomly varied)")
    print("  Choices per question: 2-5 (randomly varied)")
    print("  Attempts:           {}".format(counts["attempts"]))
    print("  Share codes:        {}".format(counts["share_codes"]))
    print("  Pool items:         {}".format(counts["pool_items"]))

    # Initialize Firestore
    db = init_firestore(args)

    # Clean if requested
    if args.clean:
        delete_all_data(db)

    # Generate data
    print("\n--- Generating data ---")

    print("\n[1/6] Generating users...")
    users = generate_users(counts["users"])
    admin_count = sum(1 for u in users if u["role"] == "admin")
    banned_count = sum(1 for u in users if u["deletedAt"] is not None)
    print(
        "  Generated {} users ({} admins, {} banned)".format(
            len(users), admin_count, banned_count
        )
    )

    print("\n[2/6] Generating quizzes with questions and choices...")
    quizzes, all_questions, all_choices = generate_quizzes(counts["quizzes"], users)
    state_counts = {}
    for q in quizzes:
        state = q.get("_state", "unknown")
        state_counts[state] = state_counts.get(state, 0) + 1
    print("  Generated {} quizzes:".format(len(quizzes)))
    for state, cnt in sorted(state_counts.items()):
        print("    - {}: {}".format(state, cnt))
    print("  Generated {} questions total".format(len(all_questions)))
    print("  Generated {} choices total".format(len(all_choices)))

    print("\n[3/6] Generating attempts...")
    attempts, quiz_attempt_counts = generate_attempts(
        counts["attempts"], users, quizzes
    )
    if attempts:
        avg_score = sum(a["score"] for a in attempts) / len(attempts)
        avg_max = sum(a["maxScore"] for a in attempts) / len(attempts)
        print(
            "  Generated {} attempts (avg score: {:.1f}/{:.1f})".format(
                len(attempts), avg_score, avg_max
            )
        )

    print("\n[4/6] Generating share codes...")
    share_codes = generate_share_codes(counts["share_codes"], quizzes)
    print("  Generated {} share codes".format(len(share_codes)))

    print("\n[5/6] Generating question pool items...")
    pool_items = generate_question_pool_items(counts["pool_items"], users, quizzes)
    active_pool = sum(1 for p in pool_items if p["isActive"])
    anon_pool = sum(1 for p in pool_items if p["contributorId"] is None)
    print(
        "  Generated {} pool items ({} active, {} anonymous)".format(
            len(pool_items), active_pool, anon_pool
        )
    )

    # Update quiz attempt counts in the data
    for quiz in quizzes:
        quiz["attemptCount"] = quiz_attempt_counts.get(quiz["id"], 0)

    # Write to Firestore
    print("\n--- Writing to Firestore ---")

    total_docs = 0

    total_docs += write_users(db, users)
    qc, questionc, choicec = write_quizzes_with_questions(db, quizzes)
    total_docs += qc + questionc + choicec
    total_docs += write_attempts(db, attempts)
    total_docs += write_share_codes(db, share_codes)
    total_docs += write_question_pool(db, pool_items)

    update_quiz_attempt_counts(db, quizzes, quiz_attempt_counts)

    # Print summary
    print("\n" + "=" * 60)
    print("GENERATION COMPLETE")
    print("=" * 60)
    print("\nSummary:")
    print("  Users:          {:>6}".format(len(users)))
    print("  Quizzes:        {:>6}".format(len(quizzes)))
    print("  Questions:      {:>6}".format(len(all_questions)))
    print("  Choices:        {:>6}".format(len(all_choices)))
    print("  Attempts:       {:>6}".format(len(attempts)))
    print("  Share codes:    {:>6}".format(len(share_codes)))
    print("  Pool items:     {:>6}".format(len(pool_items)))
    print("  --------------------------")
    print("  Total documents:{:>6}".format(total_docs))

    print("\nFirestore collections written:")
    print("  - {}".format(COLLECTION_USERS))
    print(
        "  - {} (with /{} and /{} subcollections)".format(
            COLLECTION_QUIZZES, COLLECTION_QUESTIONS, COLLECTION_CHOICES
        )
    )
    print("  - {}".format(COLLECTION_ATTEMPTS))
    print("  - {}".format(COLLECTION_SHARE_CODES))
    print("  - {}".format(COLLECTION_QUESTION_POOL))

    print("\nDone.")


if __name__ == "__main__":
    main()
