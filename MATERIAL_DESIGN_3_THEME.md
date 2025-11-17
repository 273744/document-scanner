# Material Design 3 Theme Implementation

## Overview
Document Scanner app now features a complete Material Design 3 (Material You) theme with full day/night mode support, smooth animations, and accessibility-compliant color contrast.

## Color Palette

### Light Theme Colors
**Primary Colors** (Brand Identity - Indigo)
- Primary: `#5C6BC0` - Main brand color for app bars, FABs, primary buttons
- On Primary: `#FFFFFF` - Text/icons on primary color
- Primary Container: `#E1E4FF` - Lighter variant for backgrounds
- On Primary Container: `#001552` - Text on primary containers

**Secondary Colors** (Accent - Teal)
- Secondary: `#00796B` - Secondary actions, highlights
- On Secondary: `#FFFFFF` - Text on secondary
- Secondary Container: `#A7F3E3` - Light teal backgrounds
- On Secondary Container: `#002019` - Text on secondary containers

**Tertiary Colors** (PDF Actions - Orange)
- Tertiary: `#F57C00` - PDF generation, important actions
- On Tertiary: `#FFFFFF` - Text on tertiary
- Tertiary Container: `#FFE0B2` - Light orange backgrounds
- On Tertiary Container: `#2E1500` - Text on tertiary containers

**Error Colors**
- Error: `#D32F2F` - Error states
- Error Container: `#FFCDD2` - Error backgrounds
- On Error: `#FFFFFF` - Text on error
- On Error Container: `#370B1E` - Text on error containers

**Surface & Background**
- Background: `#FDFBFF` - App background
- On Background: `#1B1B1F` - Text on background
- Surface: `#FDFBFF` - Card/sheet surfaces
- On Surface: `#1B1B1F` - Text on surfaces
- Surface Variant: `#E2E1EC` - Alternative surfaces
- On Surface Variant: `#45464F` - Text on surface variants

### Dark Theme Colors
All colors automatically adapt for dark mode with proper contrast ratios:
- Primary: `#BEC2FF` (lighter for visibility on dark)
- Background: `#1B1B1F` (deep dark)
- Surface: `#1B1B1F` (consistent with background)

## Theme Features

### 1. Dynamic Color Support (Android 12+)
- Automatically adapts to system wallpaper colors when enabled
- Falls back to custom colors on older Android versions
- Maintains brand identity while respecting user preferences

### 2. Day/Night Mode
**Automatic Switching:**
```xml
<!-- Theme automatically switches based on system settings -->
values/themes.xml → Light theme
values-night/themes.xml → Dark theme
```

**Key Differences:**
- Status bar: Primary color (light) / Surface color (dark)
- Navigation bar: Surface color with appropriate contrast
- Text colors: High contrast maintained in both modes

### 3. Material Components Styling

**Buttons:**
- Corner radius: 12dp (modern rounded look)
- Min height: 48dp (accessibility touch target)
- Letter spacing: 0.02 (improved readability)
- No all caps (better UX)

**Cards:**
- Corner radius: 16dp
- Elevation: 2dp (subtle depth)
- Content padding: 16dp

**Text Input Fields:**
- All corners: 12dp (consistent rounding)
- Outlined style for clarity

**FABs (Floating Action Buttons):**
- Corner radius: 16dp
- Primary color scheme
- Prominent for main actions

**Bottom Sheets:**
- Top corners: 24dp (signature Material 3 look)
- Bottom corners: 0dp (flush with bottom)

### 4. Typography Scale

**Headline 1** (32sp, Medium)
- Use: Page titles, major headings

**Headline 2** (24sp, Medium)
- Use: Section headers, toolbar titles

**Body 1** (16sp, Regular)
- Use: Primary content, descriptions

**Body 2** (14sp, Regular)
- Use: Secondary text, captions

### 5. Animations & Transitions

**Activity Transitions:**
- Enter: Slide in from right with fade
- Exit: Slide out to left with fade
- Duration: 300ms
- Interpolator: Accelerate-decelerate

**Element Animations:**
- Scale fade in: Dialog and modal appearances
- Scale fade out: Dismissals
- Duration: 200-300ms

**Available Animations:**
- `@anim/slide_in_right` - New activity enters
- `@anim/slide_out_left` - Previous activity exits
- `@anim/scale_fade_in` - Dialogs, cards appear
- `@anim/scale_fade_out` - Elements disappear

### 6. Accessibility Features

**Color Contrast:**
- All text colors meet WCAG AA standards (4.5:1 for normal text)
- Large text meets AAA standards (3:1)
- Icons have sufficient contrast

**Touch Targets:**
- Minimum 48dp × 48dp for all interactive elements
- Proper spacing between elements

**Semantic Colors:**
- Error: Red tones (universally recognized)
- Success: Green tones (positive actions)
- Warning: Orange tones (caution)

## Usage in Activities

### Applying Theme
```xml
<!-- AndroidManifest.xml -->
<application
    android:theme="@style/Theme.MyApplication">
```

### Using Theme Colors in Layouts
```xml
<!-- Primary color button -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.App.Button"
    android:backgroundTint="?attr/colorPrimary"
    android:textColor="?attr/colorOnPrimary" />

<!-- Surface card -->
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.App.Card"
    android:backgroundTint="?attr/colorSurface" />
```

### Custom Color Usage
```xml
<!-- Scanner overlay -->
<View
    android:background="@color/scanner_overlay" />

<!-- Corner indicators -->
<View
    android:background="@color/scanner_corner" />
```

## Custom Colors

**Scanner-Specific:**
- `scanner_overlay`: `#66000000` - Semi-transparent overlay for camera
- `scanner_corner`: `#5C6BC0` - Corner markers for document detection
- `scanner_grid`: `#4DFFFFFF` - Grid lines overlay

**Semantic Colors:**
- `success_green`: `#4CAF50` - Success messages
- `warning_orange`: `#FF9800` - Warnings

## Theme Testing

### Test Scenarios
1. ✅ Switch device between light/dark mode
2. ✅ Test all activities in both modes
3. ✅ Verify button visibility and contrast
4. ✅ Check dialog theming
5. ✅ Validate bottom sheet appearance
6. ✅ Test animations smoothness
7. ✅ Verify accessibility with TalkBack

### Supported Android Versions
- **Minimum SDK 24 (Android 7.0)**: Full theme support
- **SDK 29+ (Android 10+)**: Enhanced gesture navigation colors
- **SDK 31+ (Android 12+)**: Dynamic color support (optional)

## Implementation Details

### Material 3 Dependencies
```gradle
implementation("com.google.android.material:material:1.12.0")
```

### Theme Hierarchy
```
Theme.Material3.Light.NoActionBar (Base)
  └── Theme.MyApplication (Custom)
      ├── Color attributes
      ├── Component styles
      ├── Typography
      └── Animations
```

### Override in Activities (if needed)
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Force light theme for specific activity
    setTheme(R.style.Theme_MyApplication)
    super.onCreate(savedInstanceState)
}
```

## Future Enhancements
- [ ] Add custom font family
- [ ] Implement Material Motion transitions
- [ ] Add splash screen theming (Android 12+)
- [ ] Create widget themes
- [ ] Add theme picker for manual control

## Resources
- [Material Design 3 Guidelines](https://m3.material.io/)
- [Android Material Components](https://material.io/develop/android)
- [Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)

## File Structure
```
res/
├── values/
│   ├── colors.xml          # All color definitions
│   └── themes.xml          # Light theme
├── values-night/
│   └── themes.xml          # Dark theme
└── anim/
    ├── slide_in_right.xml  # Enter animation
    ├── slide_out_left.xml  # Exit animation
    ├── scale_fade_in.xml   # Appear animation
    └── scale_fade_out.xml  # Disappear animation
```

## Migration Notes
- All activities automatically inherit the new theme
- No code changes required in Java/Kotlin files
- Existing Material components automatically updated
- Custom views may need color attribute updates

## Build & Test
```bash
# Clean build
./gradlew clean

# Build with new theme
./gradlew assembleDebug

# Install and test
./gradlew installDebug
```

