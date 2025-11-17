# ARSettingsManager Guide ⚙️🎨

## Overview
Complete user customization system for AR document scanning with themes, performance modes, and accessibility options.

## Features ✅
1. ✅ 10 color themes for AR overlays
2. ✅ Quality threshold adjustment (5-10)
3. ✅ 6 guideline styles with opacity control
4. ✅ Audio/vibration/voice feedback settings
5. ✅ AR effect intensity levels (0-100%)
6. ✅ Accessibility mode with high contrast
7. ✅ 4 performance modes
8. ✅ 5 preset configurations
9. ✅ Settings export/import
10. ✅ Auto-save to SharedPreferences

## Quick Start

```java
// Initialize
ARSettingsManager settings = new ARSettingsManager(context);

// Set listener
settings.setSettingsChangeListener(() -> {
    applySettings();
});

// Apply a preset
settings.applyPreset(Preset.PROFESSIONAL);

// Or customize individual settings
settings.setOverlayTheme(OverlayTheme.BLUE);
settings.setQualityThreshold(8);
settings.setGuidelineOpacity(0.7f);
```

## Complete Integration

```java
public class ARCameraActivity extends AppCompatActivity {
    
    private ARSettingsManager settings;
    private AROverlayView overlayView;
    private ARPerformanceManager performanceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize settings manager
        settings = new ARSettingsManager(this);
        
        // Listen for changes
        settings.setSettingsChangeListener(() -> {
            applyAllSettings();
        });
        
        // Apply current settings
        applyAllSettings();
    }
    
    private void applyAllSettings() {
        // Apply overlay theme
        overlayView.setPrimaryColor(settings.getPrimaryColor());
        overlayView.setSecondaryColor(settings.getSecondaryColor());
        overlayView.setGuidelineOpacity(settings.getGuidelineOpacity());
        overlayView.setGuidelineStyle(settings.getGuidelineStyle());
        
        // Apply performance settings
        PerformanceMode perfMode = settings.getPerformanceMode();
        performanceManager.setQuality(perfMode);
        
        // Apply feedback settings
        audioManager.setEnabled(settings.isAudioFeedbackEnabled());
        vibrationManager.setEnabled(settings.isVibrationFeedbackEnabled());
        voiceGuidance.setEnabled(settings.isVoiceGuidanceEnabled());
        
        // Apply AR effects
        documentRenderer.setEffectIntensity(settings.getAREffectIntensity());
        documentRenderer.setShowEffects(settings.shouldShowAREffects());
        
        // Apply accessibility
        if (settings.isAccessibilityMode()) {
            enableAccessibilityFeatures();
        }
        
        // Apply auto-capture settings
        autoCaptureManager.setEnabled(settings.isAutoCaptureEnabled());
        autoCaptureManager.setQualityThreshold(settings.getQualityThreshold());
        autoCaptureManager.setCountdownDuration(settings.getCountdownDuration());
    }
    
    private void showSettingsDialog() {
        ARSettingsDialog dialog = new ARSettingsDialog(this, settings);
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        settings.cleanup();
    }
}
```

## 1. Overlay Themes

### Available Themes

```java
DEFAULT       - Green, Blue, Orange
BLUE          - Blue spectrum
GREEN         - Green spectrum
PURPLE        - Purple/Magenta
ORANGE        - Orange/Yellow
RED           - Red spectrum
CYAN          - Cyan/Aqua
YELLOW        - Yellow/Gold
MONOCHROME    - White/Gray
HIGH_CONTRAST - Yellow/Magenta/Cyan
```

### Set Theme

```java
// Set by enum
settings.setOverlayTheme(OverlayTheme.BLUE);

// Get current theme
OverlayTheme theme = settings.getOverlayTheme();

// Get all themes
List<OverlayTheme> themes = settings.getAvailableThemes();
```

### Use Theme Colors

```java
// Get colors from theme
int primary = settings.getPrimaryColor();
int secondary = settings.getSecondaryColor();
int accent = settings.getAccentColor();

// Apply to overlay
overlayView.setPrimaryColor(primary);
overlayView.setBorderColor(secondary);

// Quality-based color
int color = settings.getQualityBasedColor(qualityScore);
// Quality 8-10: Primary color
// Quality 6-7: Secondary color
// Quality <6: Accent color
```

### Theme Selection UI

```java
// Create theme selector
Spinner themeSpinner = findViewById(R.id.spinnerTheme);
List<String> themeNames = new ArrayList<>();
for (OverlayTheme theme : settings.getAvailableThemes()) {
    themeNames.add(theme.name);
}

ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
    android.R.layout.simple_spinner_item, themeNames);
themeSpinner.setAdapter(adapter);

themeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, 
                              int position, long id) {
        OverlayTheme theme = settings.getAvailableThemes().get(position);
        settings.setOverlayTheme(theme);
    }
});
```

## 2. Quality Threshold

### Set Threshold

```java
// Range: 5-10
settings.setQualityThreshold(8);

// Get current threshold
int threshold = settings.getQualityThreshold();

// Get description
String desc = settings.getQualityThresholdDescription();
// Examples:
// 9-10: "Very Strict - Only excellent quality"
// 8: "Strict - High quality required"
// 7: "Normal - Good quality expected"
// 6: "Lenient - Acceptable quality"
// 5: "Very Lenient - Any quality accepted"
```

### Quality Slider UI

```java
SeekBar qualitySlider = findViewById(R.id.seekBarQuality);
TextView qualityDesc = findViewById(R.id.tvQualityDesc);

qualitySlider.setMin(5);
qualitySlider.setMax(10);
qualitySlider.setProgress(settings.getQualityThreshold());

qualitySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, 
                                 boolean fromUser) {
        settings.setQualityThreshold(progress);
        qualityDesc.setText(settings.getQualityThresholdDescription());
    }
});
```

## 3. Guideline Settings

### Guideline Styles

```java
SOLID          - Solid continuous line
DASHED         - Dashed line (- - - -)
DOTTED         - Dotted line (· · · ·)
ANIMATED       - Animated moving line
CORNERS_ONLY   - Only corner indicators
MINIMAL        - Minimal UI
```

### Set Style & Opacity

```java
// Set style
settings.setGuidelineStyle(GuidelineStyle.DASHED);

// Set opacity (0.1 - 1.0)
settings.setGuidelineOpacity(0.7f);

// Get current settings
GuidelineStyle style = settings.getGuidelineStyle();
float opacity = settings.getGuidelineOpacity();
```

### Guideline UI

```java
// Style selector
Spinner styleSpinner = findViewById(R.id.spinnerGuidelineStyle);
List<GuidelineStyle> styles = settings.getAvailableGuidelineStyles();

// Opacity slider
SeekBar opacitySlider = findViewById(R.id.seekBarOpacity);
opacitySlider.setMax(100);
opacitySlider.setProgress((int)(settings.getGuidelineOpacity() * 100));

opacitySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, 
                                 boolean fromUser) {
        settings.setGuidelineOpacity(progress / 100f);
    }
});
```

## 4. Audio/Vibration Feedback

### Enable/Disable Feedback

```java
// Individual settings
settings.setAudioFeedback(true);
settings.setVibrationFeedback(true);
settings.setVoiceGuidance(true);

// Enable/disable all at once
settings.setAllFeedback(true);

// Check state
boolean audio = settings.isAudioFeedbackEnabled();
boolean vibration = settings.isVibrationFeedbackEnabled();
boolean voice = settings.isVoiceGuidanceEnabled();
```

### Feedback UI

```java
SwitchCompat switchAudio = findViewById(R.id.switchAudio);
SwitchCompat switchVibration = findViewById(R.id.switchVibration);
SwitchCompat switchVoice = findViewById(R.id.switchVoice);

switchAudio.setChecked(settings.isAudioFeedbackEnabled());
switchAudio.setOnCheckedChangeListener((btn, checked) -> {
    settings.setAudioFeedback(checked);
});

switchVibration.setChecked(settings.isVibrationFeedbackEnabled());
switchVibration.setOnCheckedChangeListener((btn, checked) -> {
    settings.setVibrationFeedback(checked);
});

switchVoice.setChecked(settings.isVoiceGuidanceEnabled());
switchVoice.setOnCheckedChangeListener((btn, checked) -> {
    settings.setVoiceGuidance(checked);
});
```

## 5. AR Effect Intensity

### Set Intensity

```java
// Range: 0.0 - 1.0
settings.setAREffectIntensity(0.8f);

// Enable/disable effects
settings.setShowAREffects(true);

// Get current settings
float intensity = settings.getAREffectIntensity();
boolean showEffects = settings.shouldShowAREffects();

// Get description
String desc = settings.getEffectIntensityDescription();
// Examples:
// 0.9-1.0: "Maximum - Full visual effects"
// 0.7-0.9: "High - Rich visual feedback"
// 0.5-0.7: "Medium - Balanced effects"
// 0.3-0.5: "Low - Subtle effects"
// 0.0-0.3: "Minimal - Basic visuals only"
```

### Effect Intensity UI

```java
SwitchCompat switchEffects = findViewById(R.id.switchShowEffects);
SeekBar intensitySlider = findViewById(R.id.seekBarIntensity);
TextView intensityDesc = findViewById(R.id.tvIntensityDesc);

switchEffects.setChecked(settings.shouldShowAREffects());
switchEffects.setOnCheckedChangeListener((btn, checked) -> {
    settings.setShowAREffects(checked);
    intensitySlider.setEnabled(checked);
});

intensitySlider.setMax(100);
intensitySlider.setProgress((int)(settings.getAREffectIntensity() * 100));
intensitySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, 
                                 boolean fromUser) {
        settings.setAREffectIntensity(progress / 100f);
        intensityDesc.setText(settings.getEffectIntensityDescription());
    }
});
```

## 6. Accessibility Options

### Enable Accessibility Mode

```java
// Enable accessibility mode
// - Enables high contrast
// - Enables large targets
// - Enables voice guidance
// - Enables vibration
settings.setAccessibilityMode(true);

// Check state
boolean accessible = settings.isAccessibilityMode();
```

### Individual Accessibility Features

```java
// High contrast
settings.setHighContrast(true);
boolean highContrast = settings.isHighContrast();

// Large targets
settings.setLargeTargets(true);
boolean largeTargets = settings.isLargeTargets();

// Get target size multiplier
float multiplier = settings.getTargetSizeMultiplier();
// Returns 1.5x if large targets enabled, 1.0x otherwise
```

### Accessibility UI

```java
SwitchCompat switchAccessibility = findViewById(R.id.switchAccessibility);
SwitchCompat switchHighContrast = findViewById(R.id.switchHighContrast);
SwitchCompat switchLargeTargets = findViewById(R.id.switchLargeTargets);

switchAccessibility.setChecked(settings.isAccessibilityMode());
switchAccessibility.setOnCheckedChangeListener((btn, checked) -> {
    settings.setAccessibilityMode(checked);
    
    // Update dependent switches
    switchHighContrast.setChecked(settings.isHighContrast());
    switchLargeTargets.setChecked(settings.isLargeTargets());
});

// Apply target size
float sizeMultiplier = settings.getTargetSizeMultiplier();
cornerHandle.setLayoutParams(new LayoutParams(
    (int)(60 * sizeMultiplier),
    (int)(60 * sizeMultiplier)
));
```

## 7. Performance Modes

### Available Modes

```java
PERFORMANCE      - 50% resolution, no effects, 1 doc
BALANCED         - 75% resolution, effects, 2 docs
QUALITY          - 100% resolution, effects, 4 docs
BATTERY_SAVER    - 35% resolution, no effects, 1 doc
```

### Set Performance Mode

```java
// Set mode
settings.setPerformanceMode(PerformanceMode.BALANCED);

// Get current mode
PerformanceMode mode = settings.getPerformanceMode();

// Get all modes
List<PerformanceMode> modes = settings.getAvailablePerformanceModes();

// Get mode properties
float resScale = settings.getResolutionScale();
int maxDocs = settings.getMaxDocuments();
```

### Performance Mode UI

```java
RadioGroup radioPerformance = findViewById(R.id.radioGroupPerformance);

// Create radio buttons for each mode
for (PerformanceMode mode : settings.getAvailablePerformanceModes()) {
    RadioButton radio = new RadioButton(this);
    radio.setText(mode.displayName);
    radio.setId(View.generateViewId());
    
    if (mode == settings.getPerformanceMode()) {
        radio.setChecked(true);
    }
    
    radio.setOnClickListener(v -> {
        settings.setPerformanceMode(mode);
    });
    
    radioPerformance.addView(radio);
}
```

## 8. Auto-Capture Settings

### Configure Auto-Capture

```java
// Enable/disable
settings.setAutoCaptureEnabled(true);

// Set countdown duration (0-10 seconds)
settings.setCountdownDuration(3);

// Get current settings
boolean enabled = settings.isAutoCaptureEnabled();
int countdown = settings.getCountdownDuration();
```

### Auto-Capture UI

```java
SwitchCompat switchAutoCapture = findViewById(R.id.switchAutoCapture);
SeekBar countdownSlider = findViewById(R.id.seekBarCountdown);

switchAutoCapture.setChecked(settings.isAutoCaptureEnabled());
switchAutoCapture.setOnCheckedChangeListener((btn, checked) -> {
    settings.setAutoCaptureEnabled(checked);
    countdownSlider.setEnabled(checked);
});

countdownSlider.setMax(10);
countdownSlider.setProgress(settings.getCountdownDuration());
countdownSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, 
                                 boolean fromUser) {
        settings.setCountdownDuration(progress);
        tvCountdown.setText(progress + " seconds");
    }
});
```

## 9. Preset Configurations

### Available Presets

```java
PROFESSIONAL     - Quality-focused (Q:8, Blue, Solid, Quality mode)
QUICK_SCAN       - Speed-focused (Q:6, Green, Corners, Performance mode)
ACCESSIBLE       - Accessibility (Q:7, High contrast, Solid, Balanced)
BATTERY_EFFICIENT- Battery-saving (Q:7, Default, Minimal, Battery mode)
CUSTOM           - User customized
```

### Apply Preset

```java
// Apply preset
settings.applyPreset(Preset.PROFESSIONAL);

// Get current preset
Preset current = settings.getCurrentPreset();

// Get all presets
List<Preset> presets = settings.getAvailablePresets();
```

### Preset Selector UI

```java
Spinner presetSpinner = findViewById(R.id.spinnerPreset);
List<Preset> presets = settings.getAvailablePresets();
List<String> presetNames = new ArrayList<>();

for (Preset preset : presets) {
    presetNames.add(preset.displayName);
}

ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
    android.R.layout.simple_spinner_item, presetNames);
presetSpinner.setAdapter(adapter);

presetSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, 
                              int position, long id) {
        Preset preset = presets.get(position);
        settings.applyPreset(preset);
        
        // Refresh UI to show preset settings
        refreshSettingsUI();
    }
});
```

## 10. Settings Export/Import

### Export Settings

```java
// Export to map
Map<String, Object> exportedSettings = settings.exportSettings();

// Save to file
File file = new File(getFilesDir(), "ar_settings.json");
JSONObject json = new JSONObject(exportedSettings);
FileWriter writer = new FileWriter(file);
writer.write(json.toString());
writer.close();

// Or share via Intent
Intent shareIntent = new Intent(Intent.ACTION_SEND);
shareIntent.setType("text/plain");
shareIntent.putExtra(Intent.EXTRA_TEXT, json.toString());
startActivity(Intent.createChooser(shareIntent, "Share Settings"));
```

### Import Settings

```java
// Import from map
Map<String, Object> importedSettings = loadFromFile();
settings.importSettings(importedSettings);

// Or from JSON string
String jsonString = readFromFile();
JSONObject json = new JSONObject(jsonString);
Map<String, Object> map = jsonToMap(json);
settings.importSettings(map);
```

### Reset to Defaults

```java
// Reset all settings
settings.resetToDefaults();

// Show confirmation dialog
new AlertDialog.Builder(this)
    .setTitle("Reset Settings")
    .setMessage("Reset all settings to default values?")
    .setPositiveButton("Reset", (dialog, which) -> {
        settings.resetToDefaults();
        refreshSettingsUI();
    })
    .setNegativeButton("Cancel", null)
    .show();
```

## Complete Settings UI Example

```java
public class ARSettingsActivity extends AppCompatActivity {
    
    private ARSettingsManager settings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_settings);
        
        settings = new ARSettingsManager(this);
        
        setupThemeSettings();
        setupQualitySettings();
        setupGuidelineSettings();
        setupFeedbackSettings();
        setupEffectSettings();
        setupAccessibilitySettings();
        setupPerformanceSettings();
        setupPresetButtons();
    }
    
    private void setupThemeSettings() {
        Spinner themeSpinner = findViewById(R.id.spinnerTheme);
        // ... setup theme selector
    }
    
    private void setupQualitySettings() {
        SeekBar qualitySlider = findViewById(R.id.seekBarQuality);
        TextView qualityDesc = findViewById(R.id.tvQualityDesc);
        
        qualitySlider.setMin(5);
        qualitySlider.setMax(10);
        qualitySlider.setProgress(settings.getQualityThreshold());
        qualityDesc.setText(settings.getQualityThresholdDescription());
        
        qualitySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, 
                                         boolean fromUser) {
                settings.setQualityThreshold(progress);
                qualityDesc.setText(settings.getQualityThresholdDescription());
            }
        });
    }
    
    private void setupPresetButtons() {
        findViewById(R.id.btnPresetProfessional).setOnClickListener(v -> {
            settings.applyPreset(Preset.PROFESSIONAL);
            refreshAllSettings();
        });
        
        findViewById(R.id.btnPresetQuickScan).setOnClickListener(v -> {
            settings.applyPreset(Preset.QUICK_SCAN);
            refreshAllSettings();
        });
        
        findViewById(R.id.btnPresetAccessible).setOnClickListener(v -> {
            settings.applyPreset(Preset.ACCESSIBLE);
            refreshAllSettings();
        });
        
        findViewById(R.id.btnPresetBattery).setOnClickListener(v -> {
            settings.applyPreset(Preset.BATTERY_EFFICIENT);
            refreshAllSettings();
        });
        
        findViewById(R.id.btnResetDefaults).setOnClickListener(v -> {
            showResetConfirmation();
        });
    }
    
    private void refreshAllSettings() {
        // Refresh all UI elements to reflect current settings
        setupThemeSettings();
        setupQualitySettings();
        setupGuidelineSettings();
        // ... refresh all other settings
    }
}
```

## Settings Summary

### Get Summary String

```java
String summary = settings.getSettingsSummary();
Log.d(TAG, summary);

// Output example:
// Theme: Blue
// Quality: 8/10 (Strict - High quality required)
// Guidelines: Solid (70% opacity)
// Audio: On, Vibration: On, Voice: On
// AR Effects: On (80% intensity)
// Performance: Balanced
// Auto-capture: On (3s countdown)
// Accessibility: Disabled
```

## Best Practices

### 1. Initialize Early

```java
// Initialize in Application or Activity onCreate
settings = new ARSettingsManager(context);

// Set listener before any operations
settings.setSettingsChangeListener(() -> {
    applySettings();
});
```

### 2. Apply Settings Consistently

```java
private void applySettings() {
    // Apply all settings whenever they change
    applyTheme();
    applyQuality();
    applyGuidelines();
    applyFeedback();
    applyEffects();
    applyPerformance();
}
```

### 3. Provide Visual Feedback

```java
settings.setSettingsChangeListener(() -> {
    // Show toast or snackbar
    Snackbar.make(rootView, "Settings updated", Snackbar.LENGTH_SHORT).show();
    
    // Apply immediately
    applySettings();
});
```

### 4. Use Presets for Quick Setup

```java
// Offer presets in onboarding
if (isFirstRun()) {
    showPresetSelectionDialog();
} else {
    // Load saved settings
    applySettings();
}
```

## Status: ✅ PRODUCTION-READY
- 10 color themes
- Quality adjustment (5-10)
- 6 guideline styles
- Complete feedback control
- AR effect intensity
- Accessibility features
- 4 performance modes
- 5 presets
- Export/import support
- Auto-save

**Complete user customization for perfect AR experience!** ⚙️🎨✨

