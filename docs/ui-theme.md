# UI Theme Guide

This document is the authoritative reference for the QuizCode visual design system.
All colors, spacing values, corner radii, typography, and elevation levels are defined
in `design-tokens.json` at the repository root and mirrored into both the Compose theme
package and the Android resource files.

The Compose and XML themes intentionally populate the full Material 3 color system,
including inverse, container, outline variant, and layered surface roles, so components
do not fall back to the default baseline palette.

---

## 1. Design Tokens

`design-tokens.json` is the single source of truth. The file is grouped into:

| Section | Description |
|---------|-------------|
| `color` | Brand palette, semantic/status colors, surface/background pairs |
| `typography` | MD3 type scale with font size, line height, letter spacing, weight |
| `spacing` | 4 dp base-unit scale (0, 4, 8, 12, 16 … 64 dp) |
| `radius` | Corner radius scale (none, xs, sm, md, lg, xl, full) |
| `elevation` | Shadow levels (0, 1, 2, 4, 8 dp) |

When updating a token, update `design-tokens.json` first, then reflect the change in
both the Compose files and the XML resource files.

---

## 2. Color Palette

### Brand Colors

| Token | Light value | Dark value | Usage |
|-------|-------------|------------|-------|
| Primary | `#4A90E2` | `#7AB4F0` | Main interactive elements, buttons, links |
| PrimaryDark | `#2C6DB5` | `#2C6DB5` | Pressed state, container on dark |
| PrimaryLight | `#7AB4F0` | `#4A90E2` | Tonal containers |
| Secondary | `#27AE60` | `#52C57F` | Secondary actions, success indicators |
| Tertiary | `#F5A623` | `#F5A623` | Warm accent, badges, highlights |

### Background & Surface

| Token | Light | Dark |
|-------|-------|------|
| Background | `#F5F7FA` | `#121416` |
| Surface | `#FFFFFF` | `#1E2126` |
| SurfaceVariant | `#EAF0F6` | `#2A2F38` |

Layered surfaces used by Material 3 containers:

| Token | Light | Dark |
|-------|-------|------|
| SurfaceDim | `#ECEFF4` | `#16191D` |
| SurfaceBright | `#FFFFFF` | `#363B43` |
| SurfaceContainerLow | `#F8FAFD` | `#181B20` |
| SurfaceContainer | `#F2F5F9` | `#1E2126` |
| SurfaceContainerHigh | `#EDF1F6` | `#252A31` |
| SurfaceContainerHighest | `#E7ECF3` | `#2D333C` |

### Semantic Colors

| Token | Value | Usage |
|-------|-------|-------|
| Success | `#27AE60` | Correct answers, pass state |
| Error | `#E74C3C` | Wrong answers, validation errors |
| Warning | `#F5A623` | Cautions, time-running-low |
| Info | `#4A90E2` | Informational messages |
| GoldStar | `#FFD700` | High-score badge, achievements |

---

## 3. Typography

The type scale follows Material Design 3. All styles use the system default font
(Roboto on Android). Custom fonts can be introduced by replacing `FontFamily.Default`
in `ui/theme/Type.kt`.

| Role | Size | Weight | Usage |
|------|------|--------|-------|
| displayLarge | 57 sp | Regular | Large hero text |
| headlineLarge | 32 sp | Regular | Screen titles |
| headlineMedium | 28 sp | Regular | Section headings |
| titleLarge | 22 sp | Regular | Card / dialog titles |
| titleMedium | 16 sp | Medium | Toolbar, list headers |
| bodyLarge | 16 sp | Regular | Primary body text |
| bodyMedium | 14 sp | Regular | Secondary body text |
| labelLarge | 14 sp | Medium | Button labels |
| labelSmall | 11 sp | Medium | Captions, badges |

---

## 4. Shape Scale

Corner radii follow the MD3 shape system defined in `ui/theme/Shape.kt` and `dimens.xml`.

| Token | Value | Usage |
|-------|-------|-------|
| extraSmall | 4 dp | Chips, small tags |
| small | 8 dp | Input fields, small cards |
| medium | 12 dp | Standard cards |
| large | 16 dp | Bottom sheets, dialogs (corners) |
| extraLarge | 28 dp | FAB, large modals |
| full | 50 dp radius helper | Buttons, avatar rings, capsules |

In Compose, `MaterialTheme.shapes` only exposes the standard MD3 slots through
`extraLarge`. Use `FullShape` for the dedicated pill helper:

```kotlin
Button(
    onClick = {},
    shape = FullShape
) {
    Text("Join quiz")
}
```

---

## 5. Spacing

Use the named dimension resources to maintain consistent rhythm.

```xml
<!-- XML example -->
android:padding="@dimen/spacing_4"   <!-- 16 dp -->
android:margin="@dimen/spacing_2"    <!-- 8 dp  -->
```

```kotlin
// Compose example
Modifier.padding(16.dp)   // spacing_4
Spacer(modifier = Modifier.height(8.dp))  // spacing_2
```

---

## 6. Using the Theme in Compose

Wrap your root composable with `QuizCodeTheme`:

```kotlin
import com.example.androidapp.ui.theme.QuizCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizCodeTheme {
                // Your app content
            }
        }
    }
}
```

### Accessing Tokens in a Composable

```kotlin
@Composable
fun MyButton() {
    Button(
        onClick = {},
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary   // #4A90E2
        ),
        shape = FullShape
    ) {
        Text(
            text = "Start Quiz",
            style = MaterialTheme.typography.labelLarge          // 14 sp / Medium
        )
    }
}
```

### Semantic Color Usage

```kotlin
// Correct answer feedback
Icon(
    imageVector = Icons.Default.Check,
    tint = Success   // import from ui.theme.Color
)

// Error feedback
Text(
    text = "Wrong answer",
    color = MaterialTheme.colorScheme.error
)

// High-score star
Icon(
    imageVector = Icons.Default.Star,
    tint = GoldStar
)
```

---

## 7. Using the Theme in XML Views

The theme is applied via `Theme.AndroidApp` (or `Theme.AndroidApp.NoActionBar`) in
`AndroidManifest.xml`. Reference design tokens using XML resources:

```xml
<!-- Button with brand primary color -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="@dimen/button_height"
    android:backgroundTint="@color/brand_primary"
    android:textColor="@color/brand_on_primary"
    android:padding="@dimen/spacing_4"
    app:cornerRadius="@dimen/radius_full" />

<!-- Card with medium radius -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="@dimen/radius_md"
    app:cardElevation="@dimen/elevation_2"
    android:padding="@dimen/card_padding" />
```

### Dark Mode

Night-mode overrides are in `res/values-night/themes.xml`. The system automatically
selects dark colors when the user has dark mode enabled. Do not hard-code colors in
XML layouts - always reference color resources.

---

## 8. Migration Guide

If you are updating code that used the old purple Material 3 default colors:

| Old | New | Compose reference |
|-----|-----|-------------------|
| `Purple40` (`#6650A4`) | `Primary` (`#4A90E2`) | `MaterialTheme.colorScheme.primary` |
| `PurpleGrey40` | `OnSurfaceVariant` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `Pink40` | `Tertiary` (`#F5A623`) | `MaterialTheme.colorScheme.tertiary` |
| `SurfaceLight` | `Surface` (`#FFFFFF`) | `MaterialTheme.colorScheme.surface` |

Steps:
1. Replace hard-coded `Color(0xFF...)` literals with named constants from `ui/theme/Color.kt`
   or `MaterialTheme.colorScheme.*` slots.
2. Replace hard-coded `RoundedCornerShape(N.dp)` with `MaterialTheme.shapes.*` slots or
   named `@dimen/radius_*` references.
3. Replace hard-coded padding/margin values with `@dimen/spacing_*` / the dp ladder.
4. Run `./gradlew lint` to catch any remaining style violations.

---

## 9. Accessibility

- All primary/background color pairs meet WCAG AA contrast (4.5:1 for text, 3:1 for UI).
- Do not rely on color alone to convey state; pair color with icons or text labels.
- Minimum touch target size: 48 dp x 48 dp (`@dimen/button_height`).
- Use `contentDescription` on all icon-only buttons and images.

---

## 10. Component Quick Reference

| Component | Background | Text | Shape |
|-----------|-----------|------|-------|
| Primary button | `primary` | `onPrimary` | `FullShape` |
| Secondary button | `secondaryContainer` | `onSecondaryContainer` | `FullShape` |
| Quiz card | `surface` | `onSurface` | `shapes.medium` |
| Choice button | `surfaceVariant` | `onSurfaceVariant` | `shapes.small` |
| Code input | `surface` | `onSurface` | `shapes.small` |
| Bottom nav | `surface` | `onSurfaceVariant` / `primary` | — |
| Chip / tag | `primaryContainer` | `onPrimaryContainer` | `shapes.extraSmall` |
| Score card | `surface` | `onSurface` + `GoldStar` | `shapes.large` |
