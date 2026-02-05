# Firestore Backend Documentation

## Overview

This directory contains the complete Firestore database schema definitions, security rules, and deployment configuration for the QuizCode Android application.

## Directory Structure

```
firestore/
├── schemas/                      # JSON Schema definitions
│   ├── users.schema.json         # Users collection
│   ├── quizzes.schema.json       # Quizzes collection
│   ├── questions.schema.json     # Questions subcollection
│   ├── choices.schema.json       # Choices subcollection
│   ├── attempts.schema.json      # Attempts collection
│   ├── questionPool.schema.json  # Question pool collection
│   └── shareCodes.schema.json    # Share codes lookup
├── SCHEMA_RELATIONSHIPS.md       # ERD and database relationships
└── DEPLOYMENT.md                 # Deployment guide
```

## Quick Start

```bash
# Start Firebase emulators
firebase emulators:start
```

Access Emulator UI at: http://localhost:4000

### Verify Deployment

1. Check Firestore rules in Firebase Console
2. Verify indexes are created (may take a few minutes)
3. Test authentication and data access from Android app

## Schema Files

Each schema file follows JSON Schema Draft 7 specification and includes:

- Field definitions with types and constraints
- Required fields
- Validation rules (min/max length, patterns)
- Index recommendations
- Example documents

### Collection Schemas

#### 1. Users (`users.schema.json`)

Stores user profile information.

**Key Fields:**
- `username`: Unique username (3-30 chars)
- `email`: User email address
- `displayName`: Optional display name
- `createdAt`, `updatedAt`, `deletedAt`: Timestamps

**Access:**
- Read: All authenticated users
- Write: Own document only

#### 2. Quizzes (`quizzes.schema.json`)

Main quiz collection with metadata.

**Key Fields:**
- `ownerId`: Reference to user
- `title`: Quiz title (1-200 chars)
- `description`: Optional description
- `isPublic`: Public/private flag
- `shareCode`: 6-char unique code
- `tags`: Array of tags
- `questionCount`, `attemptCount`: Counters

**Access:**
- Read: Public quizzes by all, private by owner
- Write: Owner only

#### 3. Questions (`questions.schema.json`)

Subcollection under quizzes with question data.

**Path:** `quizzes/{quizId}/questions/{questionId}`

**Key Fields:**
- `content`: Question text
- `mediaUrl`: Optional image/video URL
- `explanation`: Optional explanation
- `points`: Points for correct answer (1-100)
- `position`: Order in quiz
- `choiceCount`: Number of choices (2-10)
- `allowMultipleCorrect`: Multiple correct answers flag

**Access:**
- Read: Inherit from parent quiz
- Write: Quiz owner only

#### 4. Choices (`choices.schema.json`)

Subcollection under questions with answer choices.

**Path:** `quizzes/{quizId}/questions/{questionId}/choices/{choiceId}`

**Key Fields:**
- `content`: Choice text
- `isCorrect`: Correct answer flag
- `position`: Order within question

**Constraints:**
- 2-10 choices per question
- At least one correct choice required

**Access:**
- Read: Inherit from parent question
- Write: Quiz owner only

#### 5. Attempts (`attempts.schema.json`)

User quiz attempt records.

**Key Fields:**
- `quizId`: Reference to quiz
- `userId`: Reference to user (or "guest_xxx")
- `questionOrder`: Randomized question order
- `choiceOrders`: Randomized choice orders per question
- `answers`: Single answer map
- `multiAnswers`: Multiple answer map
- `score`, `maxScore`: Scoring
- `startedAt`, `finishedAt`: Timestamps

**Access:**
- Read: Attempt owner or quiz owner
- Create: All (including guests)
- Update: Attempt owner only

#### 6. QuestionPool (`questionPool.schema.json`)

Community contributed questions.

**Key Fields:**
- `content`: Question text
- `choices`: Array of choice objects
- `correctIndices`: Array of correct choice indices
- `tags`: Category tags
- `points`: Suggested points
- `allowMultipleCorrect`: Multiple correct flag
- `contributorId`: Contributor user ID (nullable)
- `sourceQuizId`: Original quiz
- `isActive`: Active status
- `usageCount`: Usage counter

**Access:**
- Read: Active questions only
- Create: Authenticated users
- Update: Contributor only

#### 7. ShareCodes (`shareCodes.schema.json`)

Share code to quiz ID lookup table.

**Document ID:** The share code itself (e.g., "ABC123")

**Key Fields:**
- `quizId`: Reference to quiz
- `createdAt`: Creation timestamp
- `expiresAt`: Optional expiration

**Access:**
- Read: All users (for lookup)
- Write: Server-only (Cloud Functions)

## Security Rules

### Overview

Security rules are defined in `/firestore.rules` and implement:

- Authentication checks
- Ownership validation
- Field-level validation
- Soft delete support
- Guest user support

### Key Functions

```javascript
isSignedIn()           // Check if user is authenticated
isOwner(userId)        // Check if user owns document
isGuest(userId)        // Check if user is guest
getQuiz(quizId)        // Fetch quiz document
isQuizPublic(quizId)   // Check if quiz is public
isQuizOwner(quizId)    // Check if user owns quiz
isActive()             // Check if not soft-deleted
```

### Rule Patterns

#### Owner-Only Write
```javascript
allow write: if isOwner(resource.data.ownerId)
```

#### Public Read, Owner Write
```javascript
allow read: if resource.data.isPublic == true || isOwner(resource.data.ownerId)
allow write: if isOwner(resource.data.ownerId)
```

#### Guest Support
```javascript
allow create: if isSignedIn() || isGuest(request.resource.data.userId)
```

## Indexes

Composite indexes are defined in `/firestore.indexes.json`.

### Key Indexes

1. **Public Quiz Listing**
   ```json
   {
     "fields": [
       {"fieldPath": "isPublic", "order": "ASCENDING"},
       {"fieldPath": "createdAt", "order": "DESCENDING"}
     ]
   }
   ```

2. **User Quiz History**
   ```json
   {
     "fields": [
       {"fieldPath": "userId", "order": "ASCENDING"},
       {"fieldPath": "startedAt", "order": "DESCENDING"}
     ]
   }
   ```

3. **Tag Search**
   ```json
   {
     "fields": [
       {"fieldPath": "tags", "arrayConfig": "CONTAINS"},
       {"fieldPath": "createdAt", "order": "DESCENDING"}
     ]
   }
   ```

## Data Relationships

See [SCHEMA_RELATIONSHIPS.md](./SCHEMA_RELATIONSHIPS.md) for:

- Complete ERD diagram
- Collection hierarchy
- Relationship details
- Data access patterns
- Common queries
- Data integrity rules
- Cascade delete rules

## Deployment

See [DEPLOYMENT.md](./DEPLOYMENT.md) for:

- Prerequisites
- Setup steps
- Testing with emulators
- Production deployment
- Verification steps
- Troubleshooting
- Cost optimization

## Resources

- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Security Rules Guide](https://firebase.google.com/docs/firestore/security/get-started)
- [JSON Schema Specification](https://json-schema.org/)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
