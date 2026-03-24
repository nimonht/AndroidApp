# Dokka Documentation Setup

This document describes the Dokka documentation generation and deployment setup for the Quizzez Android app.

## Overview

The project uses [Dokka 2.0.0](https://github.com/Kotlin/dokka), the official documentation engine for Kotlin, to automatically generate API documentation from KDoc comments in the source code. The documentation is automatically built and deployed to GitHub Pages on every push to the master branch.

## Configuration

### Gradle Setup

**Version Catalog** (`gradle/libs.versions.toml`):
- Dokka version: `2.0.0`
- Plugin reference: `libs.plugins.dokka`

**Root `build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.dokka)
}
```

**App Module `app/build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.dokka)
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
            )
        )

        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl.set(
                uri("https://github.com/nimonht/AndroidApp/tree/master/app/src/main/java").toURL()
            )
            remoteLineSuffix.set("#L")
        }
    }
}
```

### Documentation Features

- **Visibility**: Documents public and protected APIs
- **Source Linking**: Links documentation to GitHub source code with line numbers
- **Output Format**: HTML documentation
- **Output Location**: `app/build/dokka/html/`

## GitHub Actions Workflow

The project includes an automated workflow (`.github/workflows/dokka-docs.yml`) that:

1. Triggers on pushes to the `master` branch or manual dispatch
2. Sets up JDK 17 and Android SDK
3. Generates Dokka HTML documentation
4. Deploys to the `gh-pages` branch using JamesIves/github-pages-deploy-action@v4

### Workflow Permissions

The workflow requires `contents: write` permission to push to the `gh-pages` branch.

## Local Documentation Generation

To generate documentation locally:

```bash
./gradlew :app:dokkaHtml
```

The generated documentation will be available at `app/build/dokka/html/index.html`.

## Enabling GitHub Pages

To enable the published documentation:

1. Go to your repository on GitHub
2. Navigate to **Settings** > **Pages**
3. Under **Source**, select:
   - **Branch**: `gh-pages`
   - **Folder**: `/ (root)`
4. Click **Save**

Once configured, the documentation will be available at:
```
https://nimonht.github.io/AndroidApp/
```

## Writing Documentation

Use KDoc comments to document your code:

```kotlin
/**
 * Calculates the score for a quiz attempt.
 *
 * @param answers The user's answers
 * @param correctAnswers The correct answers
 * @return The calculated score as a percentage
 * @throws IllegalArgumentException if answer counts don't match
 */
fun calculateScore(answers: List<String>, correctAnswers: List<String>): Int {
    // implementation
}
```

### KDoc Tags

- `@param` - Parameter description
- `@return` - Return value description
- `@throws` - Exceptions that may be thrown
- `@see` - Reference to related elements
- `@sample` - Code sample
- `@since` - Version information

## Troubleshooting

### Build Fails with "google-services.json is missing"

The workflow creates a dummy `google-services.json` file automatically. For local builds, ensure you have a valid `google-services.json` file or create a dummy one in `app/google-services.json`.

### Documentation is Empty

Ensure your code has KDoc comments on public APIs. Dokka only documents elements with proper visibility modifiers.

## References

- [Dokka Documentation](https://kotlinlang.org/docs/dokka-introduction.html)
- [KDoc Syntax](https://kotlinlang.org/docs/kotlin-doc.html)
- [GitHub Pages Deploy Action](https://github.com/JamesIves/github-pages-deploy-action)
