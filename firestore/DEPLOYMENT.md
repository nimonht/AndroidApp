# Firebase Backend Setup & Deployment Guide

## Overview

This document provides step-by-step instructions for setting up and deploying the Firebase backend for the QuizCode Android application.

## Prerequisites

- Node.js (v18 or higher)
- Firebase CLI installed globally: `npm install -g firebase-tools`
- Firebase project created at [Firebase Console](https://console.firebase.google.com/)
- `google-services.json` file placed in `app/` directory

## Project Structure

```
AndroidApp/
├── firestore.rules              # Firestore security rules
├── firestore.indexes.json       # Firestore composite indexes
├── storage.rules                # Firebase Storage security rules
├── firebase.json                # Firebase configuration
└── firestore/
    ├── schemas/                 # Collection schema definitions
    │   ├── users.schema.json
    │   ├── quizzes.schema.json
    │   ├── questions.schema.json
    │   ├── choices.schema.json
    │   ├── attempts.schema.json
    │   ├── questionPool.schema.json
    │   └── shareCodes.schema.json
    └── SCHEMA_RELATIONSHIPS.md  # Database ERD and relationships
```

## Setup Steps

### 1. Install Dependencies

```bash
# Install Firebase CLI globally (if not already installed)
npm install -g firebase-tools

# Install project dependencies
npm install
```

### 2. Login to Firebase

```bash
firebase login
```

Follow the browser prompts to authenticate with your Google account.

### 3. Initialize Firebase Project

If not already initialized, run:

```bash
firebase init
```

Select:
- **Firestore**: Configure security rules and indexes
- **Storage**: Configure security rules
- **Emulators**: Set up local emulators for development

When prompted:
- Use existing `firestore.rules` file
- Use existing `firestore.indexes.json` file
- Use existing `storage.rules` file
- Set up emulators for: Authentication, Firestore, Storage

### 4. Link to Firebase Project

```bash
firebase use --add
```

Select your Firebase project from the list and give it an alias (e.g., "production").

### 5. Review Configuration Files

#### firestore.rules
Contains security rules for all collections. Key features:
- User-specific read/write permissions
- Public/private quiz access control
- Owner-only write access for quizzes
- Guest user support for attempts
- Server-only write access for share codes

#### firestore.indexes.json
Defines composite indexes for efficient querying:
- Quiz filtering by public status and creation date
- User-specific quiz and attempt queries
- Tag-based searches
- Share code lookups

#### storage.rules
Security rules for Firebase Storage:
- Profile image uploads (5MB limit)
- Quiz media files (images: 5MB, videos: 50MB)
- Owner-only upload permissions

### 6. Test with Emulators (Recommended)

Before deploying to production, test locally:

```bash
firebase emulators:start
```

This will start:
- Firestore Emulator: `http://localhost:8080`
- Storage Emulator: `http://localhost:9199`
- Authentication Emulator: `http://localhost:9099`
- Emulator UI: `http://localhost:4000`

Update your Android app to connect to emulators during development:

```kotlin
// In your Firebase initialization code (for development only)
val firestore = Firebase.firestore
val auth = Firebase.auth
val storage = Firebase.storage

if (BuildConfig.DEBUG) {
    firestore.useEmulator("10.0.2.2", 8080)  // 10.0.2.2 for Android emulator
    auth.useEmulator("10.0.2.2", 9099)
    storage.useEmulator("10.0.2.2", 9199)
}
```

### 7. Deploy to Production

#### Deploy Firestore Rules and Indexes

```bash
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
```

#### Deploy Storage Rules

```bash
firebase deploy --only storage
```

#### Deploy All at Once

```bash
firebase deploy
```

## Verification Steps

### 1. Verify Firestore Rules

```bash
# Test rules in Firebase Console
# Go to: Firestore Database > Rules
# Click "Run rules" to test various scenarios
```

### 2. Verify Indexes

```bash
# Check index creation status
firebase firestore:indexes

# View in Firebase Console
# Go to: Firestore Database > Indexes
```

All indexes should show "Enabled" status.

### 3. Test Security Rules

Create a test document to verify rules:

```bash
# In Firestore Console, try to:
# 1. Create a quiz as authenticated user (should succeed)
# 2. Read public quiz without authentication (should succeed)
# 3. Update another user's quiz (should fail)
# 4. Write to shareCodes collection (should fail for clients)
```

## Collection Setup

### Initial Data Structure

Collections will be created automatically when first document is added. No manual setup required.

### Recommended First Steps

1. **Create Test User**
   - Use Firebase Authentication to create a test user
   - User document will be created in `users/` collection on first app login

2. **Create Test Quiz**
   - Use the Android app to create a quiz
   - This will initialize the `quizzes/` collection

3. **Generate Share Code**
   - Use Cloud Functions (future) or admin SDK to create share code entries
   - For now, manually add to `shareCodes/` collection using Firebase Console

## Security Rules Summary

### Users Collection
- **Read**: All authenticated users
- **Write**: Own document only

### Quizzes Collection
- **Read**: Public quizzes by all, private by owner
- **Write**: Owner only

### Questions/Choices Subcollections
- **Read**: Inherit parent quiz permissions
- **Write**: Quiz owner only

### Attempts Collection
- **Read**: Attempt owner or quiz owner
- **Create**: All users (including guests)
- **Update**: Attempt owner only

### QuestionPool Collection
- **Read**: Active questions only
- **Create**: Authenticated users
- **Update**: Contributor only

### ShareCodes Collection
- **Read**: All users (for lookup)
- **Write**: Server-only (Cloud Functions)

## Monitoring and Maintenance

### Monitor Usage

```bash
# View Firestore usage
firebase firestore:usage

# View project info
firebase projects:list
```

### Check Rules Version

```bash
firebase firestore:rules:get
```

### Update Rules

After modifying `firestore.rules`:

```bash
firebase deploy --only firestore:rules
```

### Add New Index

When adding a new composite query, Firebase will suggest required indexes. Add them to `firestore.indexes.json` and deploy:

```bash
firebase deploy --only firestore:indexes
```

## Common Issues and Solutions

### Issue: "Missing or insufficient permissions"

**Solution**: Check security rules. Ensure:
- User is authenticated (`request.auth != null`)
- User has proper ownership or permissions
- Document is not soft-deleted (`deletedAt == null`)

### Issue: "Query requires an index"

**Solution**: 
1. Check Firebase Console error message for index creation link
2. Or add index to `firestore.indexes.json` manually
3. Deploy: `firebase deploy --only firestore:indexes`

### Issue: "Share code creation fails"

**Solution**: Share codes can only be created server-side (Cloud Functions) or via Admin SDK. Clients cannot write to `shareCodes/` collection per security rules.

### Issue: "Emulator connection failed in Android"

**Solution**: 
- Use `10.0.2.2` instead of `localhost` for Android emulator
- Use computer's local IP for physical devices
- Ensure emulators are running before starting app

## Backup and Restore

### Export Firestore Data

```bash
# Export all collections
firebase firestore:export gs://your-bucket/backup-$(date +%Y%m%d)

# Export specific collections
firebase firestore:export gs://your-bucket/backup --collections=quizzes,users
```

### Import Firestore Data

```bash
firebase firestore:import gs://your-bucket/backup-20240205
```

## Support and Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase CLI Reference](https://firebase.google.com/docs/cli)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)