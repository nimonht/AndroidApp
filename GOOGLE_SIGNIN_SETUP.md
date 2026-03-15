# Google Sign-In Setup Guide

This guide explains how to configure Google Sign-In for the QuizCode Android app.

## Overview

The app uses the modern **Android Credential Manager API** for Google Sign-In, which provides:
- One-Tap Sign-In experience
- Automatic account selection
- Better security with nonce-based authentication
- Backward compatibility through Google Play Services

## Prerequisites

1. A Google Cloud Platform (GCP) project with Firebase enabled
2. Firebase project already configured in the app (existing setup)
3. Access to Google Cloud Console

## Setup Steps

### 1. Get SHA-1 Certificate Fingerprint

First, get your app's SHA-1 fingerprint for both debug and release builds:

#### Debug Certificate (for development)
```bash
# On Linux/macOS
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# On Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

#### Release Certificate (for production)
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

Copy the **SHA-1** fingerprint from the output.

### 2. Configure Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (already configured)
3. Go to **Project Settings** → **General**
4. Under "Your apps", find your Android app
5. Click **Add fingerprint**
6. Paste your SHA-1 fingerprints (add both debug and release)
7. Click **Save**

### 3. Enable Google Sign-In in Firebase

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Click on **Google** provider
3. Toggle **Enable**
4. Set a public-facing name for your project (e.g., "QuizCode")
5. Set a support email
6. Click **Save**

### 4. Create OAuth 2.0 Web Client ID

**IMPORTANT**: The Credential Manager requires a **Web application** client ID, NOT an Android client ID.

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to **APIs & Services** → **Credentials**
4. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
5. Select **Web application** as the application type
6. Give it a name (e.g., "QuizCode Web Client")
7. Under "Authorized redirect URIs", you can leave it blank for mobile apps
8. Click **Create**
9. Copy the **Client ID** (format: `XXXXXX.apps.googleusercontent.com`)

### 5. Configure the App

Add your Web Client ID to the project:

#### Option A: Using gradle.properties (Recommended for Development)

Create or edit `gradle.properties` in your project root:

```properties
# Google OAuth 2.0 Web Client ID
googleWebClientId=YOUR_CLIENT_ID.apps.googleusercontent.com
```

**Note**: Add `gradle.properties` to `.gitignore` to keep secrets out of version control.

#### Option B: Using Command Line (CI/CD)

Pass the client ID at build time:

```bash
./gradlew assembleDebug -PgoogleWebClientId=YOUR_CLIENT_ID.apps.googleusercontent.com
```

#### Option C: Using Environment Variables (CI/CD)

Set an environment variable and reference it in `gradle.properties`:

```bash
export GOOGLE_WEB_CLIENT_ID="YOUR_CLIENT_ID.apps.googleusercontent.com"
```

Then in `gradle.properties`:
```properties
googleWebClientId=${GOOGLE_WEB_CLIENT_ID}
```

### 6. Verify Configuration

The app will read the client ID from `BuildConfig.GOOGLE_WEB_CLIENT_ID`.

If not configured, you'll see a fallback placeholder in the code:
```kotlin
val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.ifEmpty {
    "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
}
```

Replace this fallback or configure the gradle property.

## Testing Google Sign-In

### Prerequisites for Testing
1. Physical Android device or emulator with Google Play Services
2. A Google account signed in on the device
3. Internet connection

### Test Flow
1. Open the app
2. Navigate to Login or Register screen
3. Tap "Đăng nhập bằng Google" button
4. A Google account picker should appear (One-Tap)
5. Select an account
6. The app should authenticate and log you in

### Common Issues

#### Issue: "Developer Error" or "Error 10"
**Cause**: SHA-1 fingerprint mismatch or not configured.
**Solution**: Ensure SHA-1 is correctly added to Firebase project settings.

#### Issue: "Sign-in failed"
**Cause**: Web Client ID not configured or incorrect.
**Solution**: Verify the Web Client ID in Google Cloud Console and gradle.properties.

#### Issue: "API not enabled"
**Cause**: Google Sign-In API not enabled in Google Cloud.
**Solution**: Go to APIs & Services → Enable "Google Sign-In API".

#### Issue: No account picker appears
**Cause**: No Google account on device or Play Services not updated.
**Solution**: Add a Google account to device settings and update Google Play Services.

## Architecture

### Flow Diagram
```
User taps Google button
    ↓
LoginScreen calls handleGoogleSignIn()
    ↓
GoogleSignInHelper.signIn() invokes Credential Manager
    ↓
User selects Google account (One-Tap UI)
    ↓
Credential Manager returns ID token
    ↓
AuthViewModel receives GoogleSignIn event
    ↓
AuthRepository.signInWithGoogleToken() called
    ↓
Firebase Auth exchanges token for credentials
    ↓
User document created/updated in Firestore
    ↓
User cached in Room database
    ↓
AuthUiState.Authenticated emitted
    ↓
User logged in successfully
```

### Key Files
- **UI**: `LoginScreen.kt`, `RegisterScreen.kt`
- **Component**: `GoogleSignInButton.kt`
- **Helper**: `GoogleSignInHelper.kt` (domain/util)
- **ViewModel**: `AuthViewModel.kt`
- **Repository**: `AuthRepositoryImpl.kt`
- **Config**: `app/build.gradle.kts`

## Security Features

1. **Nonce-based authentication**: Prevents replay attacks
2. **SHA-256 hashing**: Secure nonce transmission
3. **Server-side verification**: Firebase validates tokens
4. **No client secrets**: Uses public OAuth flow
5. **Automatic credential management**: Handled by Google Play Services

## Production Checklist

- [ ] SHA-1 fingerprint for release keystore added to Firebase
- [ ] Web Client ID configured in production build
- [ ] Google Sign-In enabled in Firebase Authentication
- [ ] App published with correct package name (`com.example.androidapp`)
- [ ] Support email configured in Google Cloud Console
- [ ] Privacy policy URL added (if required)
- [ ] Terms of service URL added (if required)

## References

- [Android Credential Manager Documentation](https://developer.android.com/identity/sign-in/credential-manager)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Firebase Authentication - Google](https://firebase.google.com/docs/auth/android/google-signin)
- [Google Cloud Console](https://console.cloud.google.com/)

## Support

For issues or questions:
1. Check Firebase Console logs
2. Review Logcat output for detailed errors
3. Verify all configuration steps above
4. Check Google Play Services version on device (must be up-to-date)
