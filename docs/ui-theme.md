# UI Theme — Implementation Guide

**Purpose:** Central reference for colors, typography, spacing, and components so the app UI is consistent with the new design.

## Design Tokens

All design tokens are centralized in `design-tokens.json` at the repository root. This file serves as the single source of truth for:

- **Colors**: primary, onPrimary, secondary, background, surface, error, textPrimary, textSecondary
- **Typography**: display, headline, title, body, label styles (size in sp, weight, lineHeight)
- **Spacing**: xs, sm, md, lg, xl, xxl (in dp)
- **Radii**: small, medium, large, extraLarge (in dp)
- **Elevation**: level0 through level5 (in dp)

### Design Token Structure

```json
{
  "colors": {
    "primary": "#4A90E2",
    "onPrimary": "#FFFFFF",
    "secondary": "#27AE60",
    "background": "#F5F7FA",
    "surface": "#FFFFFF",
    "error": "#E74C3C",
    "textPrimary": "#2C3E50",
    "textSecondary": "#7F8C8D"
  },
  "typography": {
    "bodyLarge": {"size": 16, "weight": 400, "lineHeight": 24},
    "titleMedium": {"size": 16, "weight": 500, "lineHeight": 24}
  },
  "spacing": {"xs": 4, "sm": 8, "md": 16, "lg": 24, "xl": 32},
  "radii": {"small": 4, "medium": 8, "large": 16}
}
```

## How to Use Tokens

### Jetpack Compose

Use the theme system via `MaterialTheme`. All theme files are located in `app/src/main/java/com/example/androidapp/ui/theme/`.

**Files:**
- `Color.kt` - Color definitions mapped from design-tokens.json
- `Type.kt` - Typography scale
- `Theme.kt` - Theme composition with light/dark color schemes
- `Shape.kt` - Corner radius definitions

**Example - Using Theme Colors:**

```kotlin
@Composable
fun MyScreen() {
    QuizCodeTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = { /* action */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Click Me")
            }
        }
    }
}
```

**Example - Using Semantic Colors:**

```kotlin
import com.example.androidapp.ui.theme.Success
import com.example.androidapp.ui.theme.Error
import com.example.androidapp.ui.theme.Warning

@Composable
fun StatusIndicator(status: String) {
    val color = when (status) {
        "success" -> Success
        "error" -> Error
        "warning" -> Warning
        else -> MaterialTheme.colorScheme.onSurface
    }

    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = status,
        tint = color
    )
}
```

### XML Layouts

Use `@color/`, `@dimen/`, and style references. All XML resources are in `app/src/main/res/values/`.

**Files:**
- `colors.xml` - Color definitions
- `themes.xml` - Material 3 theme configuration
- `dimens.xml` - Spacing, radius, and text size definitions

**Example - Using Colors in XML:**

```xml
<TextView
    android:id="@+id/title"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/md_theme_light_onBackground"
    android:textSize="@dimen/text_headline_large"
    android:text="@string/welcome" />

<Button
    android:id="@+id/primaryButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:backgroundTint="@color/md_theme_light_primary"
    android:textColor="@color/md_theme_light_onPrimary"
    android:text="@string/action_continue" />
```

**Example - Using Spacing:**

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="@dimen/spacing_md">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="@dimen/spacing_sm"
        android:text="@string/label" />

</LinearLayout>
```

## Component Guidelines

### Buttons

- **Filled Button**: Use `primary` color for container, corner radius `medium` (8dp), padding `md` (16dp)
- **Outlined Button**: Use `outline` color for border, transparent background
- **Text Button**: Use `primary` color for text, no background

**Compose Example:**

```kotlin
Button(
    onClick = { /* action */ },
    shape = MaterialTheme.shapes.medium
) {
    Text("Primary Action")
}
```

### Cards

- **Background**: Use `surface` color
- **Corner Radius**: Use `medium` (8dp) or `large` (16dp)
- **Padding**: Use `md` (16dp) for content
- **Elevation**: Use level 1-3 based on hierarchy

**Compose Example:**

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Card Title",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Card content goes here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### App Bar

- **Background**: Use `surface` color
- **Title Text**: Use `onSurface` color with `titleLarge` style
- **Elevation**: 4dp (level 4)

**Compose Example:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
TopAppBar(
    title = {
        Text(
            text = "Screen Title",
            style = MaterialTheme.typography.titleLarge
        )
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
)
```

### Text Fields

- **Outlined Style**: Use `outline` color for border
- **Filled Style**: Use `surfaceVariant` for background
- **Corner Radius**: Use `small` (4dp)

**Compose Example:**

```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Enter text") },
    shape = MaterialTheme.shapes.small,
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
)
```

## Accessibility

### Color Contrast

All color combinations meet WCAG 2.1 Level AA standards:

- **Body Text**: Minimum contrast ratio of 4.5:1
  - Text primary (#2C3E50) on background (#F5F7FA): ~11:1 ✓
  - Text primary (#2C3E50) on surface (#FFFFFF): ~12:1 ✓
- **Large Text**: Minimum contrast ratio of 3:1
- **Interactive Elements**: Minimum contrast ratio of 3:1

### Best Practices

1. **Always use semantic color names** from MaterialTheme instead of hardcoded values
2. **Provide content descriptions** for all images and icons:
   ```kotlin
   Icon(
       imageVector = Icons.Default.Home,
       contentDescription = "Navigate to home"
   )
   ```
3. **Respect user font size settings** by using `sp` units for text (automatically handled by MaterialTheme)
4. **Support dark mode** by using theme-aware colors:
   ```kotlin
   QuizCodeTheme(darkTheme = isSystemInDarkTheme()) {
       // Your UI
   }
   ```

## Migration Guide

### For New Screens

#### Compose:

1. Wrap your screen composable in `QuizCodeTheme { }`:
   ```kotlin
   @Composable
   fun NewScreen() {
       QuizCodeTheme {
           // Your UI here
       }
   }
   ```

2. Use `MaterialTheme` for colors, typography, and shapes:
   ```kotlin
   Text(
       text = "Title",
       style = MaterialTheme.typography.titleLarge,
       color = MaterialTheme.colorScheme.primary
   )
   ```

#### XML:

1. Ensure your Activity uses the theme:
   ```xml
   <activity
       android:name=".NewActivity"
       android:theme="@style/Theme.AndroidApp" />
   ```

2. Reference colors and dimensions from resources:
   ```xml
   android:textColor="@color/md_theme_light_primary"
   android:padding="@dimen/spacing_md"
   ```

### Migrating Existing Screens

1. **Replace hardcoded colors** with theme references:
   - Before: `Color(0xFF6200EE)`
   - After: `MaterialTheme.colorScheme.primary`

2. **Replace hardcoded dimensions** with spacing tokens:
   - Before: `Modifier.padding(16.dp)`
   - After: Use 16dp which maps to `spacing_md` token

3. **Update typography**:
   - Before: `fontSize = 22.sp, fontWeight = FontWeight.Bold`
   - After: `style = MaterialTheme.typography.titleLarge`

## Dark Theme Support

The theme automatically supports dark mode based on system settings. To test dark mode:

**Compose:**
```kotlin
// Preview both themes
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyScreenPreview() {
    QuizCodeTheme {
        MyScreen()
    }
}
```

**Emulator/Device:**
- Settings → Display → Dark theme

## Typography Scale Reference

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| displayLarge | 57sp | 400 | Hero text, major headlines |
| displayMedium | 45sp | 400 | Featured content |
| displaySmall | 36sp | 400 | Section headers |
| headlineLarge | 32sp | 400 | Screen titles |
| headlineMedium | 28sp | 400 | Card headers |
| headlineSmall | 24sp | 400 | Subsection titles |
| titleLarge | 22sp | 400 | App bar titles |
| titleMedium | 16sp | 500 | List item titles |
| titleSmall | 14sp | 500 | Overlines, captions with emphasis |
| bodyLarge | 16sp | 400 | Primary body text |
| bodyMedium | 14sp | 400 | Secondary body text |
| bodySmall | 12sp | 400 | Small descriptions |
| labelLarge | 14sp | 500 | Button text |
| labelMedium | 12sp | 500 | Chip text, badges |
| labelSmall | 11sp | 500 | Small labels, timestamps |

## Spacing Scale Reference

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4dp | Minimal spacing, icon padding |
| sm | 8dp | Compact spacing, related elements |
| md | 16dp | Standard padding, default spacing |
| lg | 24dp | Section spacing, card margins |
| xl | 32dp | Screen margins, major sections |
| xxl | 48dp | Large gaps, feature spacing |

## Testing Checklist

When implementing or updating UI:

- [ ] Colors use theme references (no hardcoded hex values)
- [ ] Text uses typography scale from MaterialTheme
- [ ] Spacing uses token values from dimens.xml or consistent dp values
- [ ] Components have proper corner radii
- [ ] Dark mode is supported and tested
- [ ] Contrast ratios meet accessibility standards
- [ ] Icons have content descriptions
- [ ] UI scales properly with system font size settings
- [ ] Preview/test on different screen sizes

## Troubleshooting

### Colors not updating

**Issue**: UI still shows old colors after theme update.

**Solution**:
1. Clean and rebuild: `./gradlew clean assembleDebug`
2. Ensure you're wrapping composables in `QuizCodeTheme { }`
3. Check that dynamic color is enabled/disabled as needed

### Typography not applying

**Issue**: Text styles don't match design.

**Solution**:
1. Use `style = MaterialTheme.typography.X` instead of manual `fontSize`/`fontWeight`
2. Ensure QuizCodeTheme wrapper is present
3. Check that custom TextStyle isn't overriding theme values

### XML theme not reflecting changes

**Issue**: XML layouts don't show new theme.

**Solution**:
1. Verify Activity has `android:theme="@style/Theme.AndroidApp"` in manifest
2. Use `@color/md_theme_light_*` references, not direct color values
3. Invalidate caches and restart Android Studio

## Release Notes

### Theme Update (Current Version)

**Major Changes:**
- Introduced centralized design token system (`design-tokens.json`)
- Updated color palette to new brand colors (blue primary, green secondary)
- Implemented Material 3 color system with full light/dark theme support
- Created comprehensive spacing and radius token system
- Updated XML resources to match Compose theme

**Breaking Changes:**
- Old color constants (Purple80, Pink40, etc.) removed from Color.kt
- Semantic color names changed (Success, Error, Warning now use new values)
- All screens should reference MaterialTheme colors instead of old constants

**Migration Required:**
- Replace old color constants with MaterialTheme.colorScheme references
- Update any hardcoded colors to use theme colors
- Test dark mode compatibility

---

## Additional Resources

- [Material Design 3 Guidelines](https://m3.material.io/)
- [Jetpack Compose Theme Documentation](https://developer.android.com/jetpack/compose/themes)
- [Material Components for Android](https://material.io/develop/android)
- [WCAG Contrast Checker](https://webaim.org/resources/contrastchecker/)

For questions or suggestions about the UI theme, please refer to this documentation or open an issue.
