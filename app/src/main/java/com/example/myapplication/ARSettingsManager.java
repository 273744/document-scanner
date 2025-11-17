package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ARSettingsManager - User customization for AR features
 *
 * Features:
 * - AR overlay color themes and styles
 * - Quality threshold adjustment
 * - Guide line preferences
 * - Audio/vibration feedback settings
 * - AR effect intensity levels
 * - Accessibility options
 * - Performance vs quality trade-offs
 * - Preset configurations
 */
public class ARSettingsManager {

    private static final String TAG = "ARSettingsManager";
    private static final String PREFS_NAME = "ARSettings";

    // Setting keys
    private static final String KEY_OVERLAY_THEME = "overlay_theme";
    private static final String KEY_QUALITY_THRESHOLD = "quality_threshold";
    private static final String KEY_GUIDELINE_OPACITY = "guideline_opacity";
    private static final String KEY_GUIDELINE_STYLE = "guideline_style";
    private static final String KEY_AUDIO_FEEDBACK = "audio_feedback";
    private static final String KEY_VIBRATION_FEEDBACK = "vibration_feedback";
    private static final String KEY_VOICE_GUIDANCE = "voice_guidance";
    private static final String KEY_AR_EFFECT_INTENSITY = "ar_effect_intensity";
    private static final String KEY_SHOW_AR_EFFECTS = "show_ar_effects";
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";
    private static final String KEY_HIGH_CONTRAST = "high_contrast";
    private static final String KEY_LARGE_TARGETS = "large_targets";
    private static final String KEY_PERFORMANCE_MODE = "performance_mode";
    private static final String KEY_AUTO_CAPTURE_ENABLED = "auto_capture_enabled";
    private static final String KEY_COUNTDOWN_DURATION = "countdown_duration";
    private static final String KEY_CURRENT_PRESET = "current_preset";

    // Context and preferences
    private Context context;
    private SharedPreferences prefs;

    // Current settings
    private OverlayTheme currentTheme = OverlayTheme.DEFAULT;
    private int qualityThreshold = 8;
    private float guidelineOpacity = 0.7f;
    private GuidelineStyle guidelineStyle = GuidelineStyle.SOLID;
    private boolean audioFeedback = true;
    private boolean vibrationFeedback = true;
    private boolean voiceGuidance = true;
    private float arEffectIntensity = 0.8f;
    private boolean showAREffects = true;
    private boolean accessibilityMode = false;
    private boolean highContrast = false;
    private boolean largeTargets = false;
    private PerformanceMode performanceMode = PerformanceMode.BALANCED;
    private boolean autoCaptureEnabled = true;
    private int countdownDuration = 3;

    // Callbacks
    private SettingsChangeListener listener;

    // ================================
    // 1. Overlay Color Themes
    // ================================

    public enum OverlayTheme {
        DEFAULT("Default", 0xFF00FF00, 0xFF0088FF, 0xFFFFAA00),
        BLUE("Blue", 0xFF0088FF, 0xFF00CCFF, 0xFF0044FF),
        GREEN("Green", 0xFF00FF00, 0xFF00CC00, 0xFF88FF00),
        PURPLE("Purple", 0xFFAA00FF, 0xFFCC00FF, 0xFF8800FF),
        ORANGE("Orange", 0xFFFF8800, 0xFFFFAA00, 0xFFFF6600),
        RED("Red", 0xFFFF0000, 0xFFFF4444, 0xFFCC0000),
        CYAN("Cyan", 0xFF00FFFF, 0xFF00CCCC, 0xFF00AAAA),
        YELLOW("Yellow", 0xFFFFFF00, 0xFFFFDD00, 0xFFFFBB00),
        MONOCHROME("Monochrome", 0xFFFFFFFF, 0xFFCCCCCC, 0xFF888888),
        HIGH_CONTRAST("High Contrast", 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF);

        public final String name;
        public final int primaryColor;
        public final int secondaryColor;
        public final int accentColor;

        OverlayTheme(String name, int primary, int secondary, int accent) {
            this.name = name;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            this.accentColor = accent;
        }

        public int getQualityColor(int qualityScore) {
            if (qualityScore >= 8) {
                return primaryColor; // Excellent - primary
            } else if (qualityScore >= 6) {
                return secondaryColor; // Good - secondary
            } else {
                return accentColor; // Fair/Poor - accent
            }
        }
    }

    // ================================
    // 2. Guideline Styles
    // ================================

    public enum GuidelineStyle {
        SOLID("Solid Line"),
        DASHED("Dashed Line"),
        DOTTED("Dotted Line"),
        ANIMATED("Animated"),
        CORNERS_ONLY("Corners Only"),
        MINIMAL("Minimal");

        public final String displayName;

        GuidelineStyle(String displayName) {
            this.displayName = displayName;
        }
    }

    // ================================
    // 3. Performance Modes
    // ================================

    public enum PerformanceMode {
        PERFORMANCE("Performance Priority", 0.5f, false, 1),
        BALANCED("Balanced", 0.75f, true, 2),
        QUALITY("Quality Priority", 1.0f, true, 4),
        BATTERY_SAVER("Battery Saver", 0.35f, false, 1);

        public final String displayName;
        public final float resolutionScale;
        public final boolean enableEffects;
        public final int maxDocuments;

        PerformanceMode(String name, float scale, boolean effects, int maxDocs) {
            this.displayName = name;
            this.resolutionScale = scale;
            this.enableEffects = effects;
            this.maxDocuments = maxDocs;
        }
    }

    // ================================
    // 4. Presets
    // ================================

    public enum Preset {
        PROFESSIONAL("Professional", 8, OverlayTheme.BLUE, 0.7f,
            GuidelineStyle.SOLID, PerformanceMode.QUALITY, true),
        QUICK_SCAN("Quick Scan", 6, OverlayTheme.GREEN, 0.5f,
            GuidelineStyle.CORNERS_ONLY, PerformanceMode.PERFORMANCE, true),
        ACCESSIBLE("Accessible", 7, OverlayTheme.HIGH_CONTRAST, 1.0f,
            GuidelineStyle.SOLID, PerformanceMode.BALANCED, true),
        BATTERY_EFFICIENT("Battery Efficient", 7, OverlayTheme.DEFAULT, 0.6f,
            GuidelineStyle.MINIMAL, PerformanceMode.BATTERY_SAVER, false),
        CUSTOM("Custom", 8, OverlayTheme.DEFAULT, 0.7f,
            GuidelineStyle.SOLID, PerformanceMode.BALANCED, true);

        public final String displayName;
        public final int qualityThreshold;
        public final OverlayTheme theme;
        public final float guidelineOpacity;
        public final GuidelineStyle guidelineStyle;
        public final PerformanceMode performanceMode;
        public final boolean showEffects;

        Preset(String name, int quality, OverlayTheme theme, float opacity,
               GuidelineStyle style, PerformanceMode perfMode, boolean effects) {
            this.displayName = name;
            this.qualityThreshold = quality;
            this.theme = theme;
            this.guidelineOpacity = opacity;
            this.guidelineStyle = style;
            this.performanceMode = perfMode;
            this.showEffects = effects;
        }
    }

    /**
     * Constructor
     */
    public ARSettingsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved settings
        loadSettings();

        Log.d(TAG, "ARSettingsManager initialized");
    }

    // ================================
    // Settings Management
    // ================================

    /**
     * Load settings from preferences
     */
    private void loadSettings() {
        try {
            currentTheme = OverlayTheme.valueOf(
                prefs.getString(KEY_OVERLAY_THEME, OverlayTheme.DEFAULT.name()));
        } catch (Exception e) {
            currentTheme = OverlayTheme.DEFAULT;
        }

        qualityThreshold = prefs.getInt(KEY_QUALITY_THRESHOLD, 8);
        guidelineOpacity = prefs.getFloat(KEY_GUIDELINE_OPACITY, 0.7f);

        try {
            guidelineStyle = GuidelineStyle.valueOf(
                prefs.getString(KEY_GUIDELINE_STYLE, GuidelineStyle.SOLID.name()));
        } catch (Exception e) {
            guidelineStyle = GuidelineStyle.SOLID;
        }

        audioFeedback = prefs.getBoolean(KEY_AUDIO_FEEDBACK, true);
        vibrationFeedback = prefs.getBoolean(KEY_VIBRATION_FEEDBACK, true);
        voiceGuidance = prefs.getBoolean(KEY_VOICE_GUIDANCE, true);
        arEffectIntensity = prefs.getFloat(KEY_AR_EFFECT_INTENSITY, 0.8f);
        showAREffects = prefs.getBoolean(KEY_SHOW_AR_EFFECTS, true);
        accessibilityMode = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);
        highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false);
        largeTargets = prefs.getBoolean(KEY_LARGE_TARGETS, false);

        try {
            performanceMode = PerformanceMode.valueOf(
                prefs.getString(KEY_PERFORMANCE_MODE, PerformanceMode.BALANCED.name()));
        } catch (Exception e) {
            performanceMode = PerformanceMode.BALANCED;
        }

        autoCaptureEnabled = prefs.getBoolean(KEY_AUTO_CAPTURE_ENABLED, true);
        countdownDuration = prefs.getInt(KEY_COUNTDOWN_DURATION, 3);

        Log.d(TAG, "Settings loaded: Theme=" + currentTheme + ", Quality=" + qualityThreshold);
    }

    /**
     * Save settings to preferences
     */
    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_OVERLAY_THEME, currentTheme.name());
        editor.putInt(KEY_QUALITY_THRESHOLD, qualityThreshold);
        editor.putFloat(KEY_GUIDELINE_OPACITY, guidelineOpacity);
        editor.putString(KEY_GUIDELINE_STYLE, guidelineStyle.name());
        editor.putBoolean(KEY_AUDIO_FEEDBACK, audioFeedback);
        editor.putBoolean(KEY_VIBRATION_FEEDBACK, vibrationFeedback);
        editor.putBoolean(KEY_VOICE_GUIDANCE, voiceGuidance);
        editor.putFloat(KEY_AR_EFFECT_INTENSITY, arEffectIntensity);
        editor.putBoolean(KEY_SHOW_AR_EFFECTS, showAREffects);
        editor.putBoolean(KEY_ACCESSIBILITY_MODE, accessibilityMode);
        editor.putBoolean(KEY_HIGH_CONTRAST, highContrast);
        editor.putBoolean(KEY_LARGE_TARGETS, largeTargets);
        editor.putString(KEY_PERFORMANCE_MODE, performanceMode.name());
        editor.putBoolean(KEY_AUTO_CAPTURE_ENABLED, autoCaptureEnabled);
        editor.putInt(KEY_COUNTDOWN_DURATION, countdownDuration);

        editor.apply();

        Log.d(TAG, "Settings saved");
    }

    // ================================
    // 1. Overlay Theme Settings
    // ================================

    public void setOverlayTheme(OverlayTheme theme) {
        if (this.currentTheme != theme) {
            this.currentTheme = theme;
            saveSettings();
            notifySettingsChanged();
        }
    }

    public OverlayTheme getOverlayTheme() {
        return currentTheme;
    }

    public List<OverlayTheme> getAvailableThemes() {
        List<OverlayTheme> themes = new ArrayList<>();
        for (OverlayTheme theme : OverlayTheme.values()) {
            themes.add(theme);
        }
        return themes;
    }

    public int getPrimaryColor() {
        return currentTheme.primaryColor;
    }

    public int getSecondaryColor() {
        return currentTheme.secondaryColor;
    }

    public int getAccentColor() {
        return currentTheme.accentColor;
    }

    public int getQualityBasedColor(int qualityScore) {
        return currentTheme.getQualityColor(qualityScore);
    }

    // ================================
    // 2. Quality Threshold Settings
    // ================================

    public void setQualityThreshold(int threshold) {
        this.qualityThreshold = Math.max(5, Math.min(10, threshold));
        saveSettings();
        notifySettingsChanged();
    }

    public int getQualityThreshold() {
        return qualityThreshold;
    }

    public String getQualityThresholdDescription() {
        if (qualityThreshold >= 9) {
            return "Very Strict - Only excellent quality";
        } else if (qualityThreshold >= 8) {
            return "Strict - High quality required";
        } else if (qualityThreshold >= 7) {
            return "Normal - Good quality expected";
        } else if (qualityThreshold >= 6) {
            return "Lenient - Acceptable quality";
        } else {
            return "Very Lenient - Any quality accepted";
        }
    }

    // ================================
    // 3. Guideline Settings
    // ================================

    public void setGuidelineOpacity(float opacity) {
        this.guidelineOpacity = Math.max(0.1f, Math.min(1.0f, opacity));
        saveSettings();
        notifySettingsChanged();
    }

    public float getGuidelineOpacity() {
        return guidelineOpacity;
    }

    public void setGuidelineStyle(GuidelineStyle style) {
        this.guidelineStyle = style;
        saveSettings();
        notifySettingsChanged();
    }

    public GuidelineStyle getGuidelineStyle() {
        return guidelineStyle;
    }

    public List<GuidelineStyle> getAvailableGuidelineStyles() {
        List<GuidelineStyle> styles = new ArrayList<>();
        for (GuidelineStyle style : GuidelineStyle.values()) {
            styles.add(style);
        }
        return styles;
    }

    // ================================
    // 4. Audio/Vibration Settings
    // ================================

    public void setAudioFeedback(boolean enabled) {
        this.audioFeedback = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean isAudioFeedbackEnabled() {
        return audioFeedback;
    }

    public void setVibrationFeedback(boolean enabled) {
        this.vibrationFeedback = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean isVibrationFeedbackEnabled() {
        return vibrationFeedback;
    }

    public void setVoiceGuidance(boolean enabled) {
        this.voiceGuidance = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean isVoiceGuidanceEnabled() {
        return voiceGuidance;
    }

    public void setAllFeedback(boolean enabled) {
        this.audioFeedback = enabled;
        this.vibrationFeedback = enabled;
        this.voiceGuidance = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    // ================================
    // 5. AR Effect Intensity
    // ================================

    public void setAREffectIntensity(float intensity) {
        this.arEffectIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
        saveSettings();
        notifySettingsChanged();
    }

    public float getAREffectIntensity() {
        return arEffectIntensity;
    }

    public void setShowAREffects(boolean show) {
        this.showAREffects = show;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean shouldShowAREffects() {
        return showAREffects;
    }

    public String getEffectIntensityDescription() {
        if (arEffectIntensity >= 0.9f) {
            return "Maximum - Full visual effects";
        } else if (arEffectIntensity >= 0.7f) {
            return "High - Rich visual feedback";
        } else if (arEffectIntensity >= 0.5f) {
            return "Medium - Balanced effects";
        } else if (arEffectIntensity >= 0.3f) {
            return "Low - Subtle effects";
        } else {
            return "Minimal - Basic visuals only";
        }
    }

    // ================================
    // 6. Accessibility Settings
    // ================================

    public void setAccessibilityMode(boolean enabled) {
        this.accessibilityMode = enabled;

        if (enabled) {
            // Apply accessibility-friendly defaults
            setHighContrast(true);
            setLargeTargets(true);
            setVoiceGuidance(true);
            setVibrationFeedback(true);
        }

        saveSettings();
        notifySettingsChanged();
    }

    public boolean isAccessibilityMode() {
        return accessibilityMode;
    }

    public void setHighContrast(boolean enabled) {
        this.highContrast = enabled;

        if (enabled) {
            setOverlayTheme(OverlayTheme.HIGH_CONTRAST);
            setGuidelineOpacity(1.0f);
        }

        saveSettings();
        notifySettingsChanged();
    }

    public boolean isHighContrast() {
        return highContrast;
    }

    public void setLargeTargets(boolean enabled) {
        this.largeTargets = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean isLargeTargets() {
        return largeTargets;
    }

    public float getTargetSizeMultiplier() {
        return largeTargets ? 1.5f : 1.0f;
    }

    // ================================
    // 7. Performance Settings
    // ================================

    public void setPerformanceMode(PerformanceMode mode) {
        this.performanceMode = mode;
        saveSettings();
        notifySettingsChanged();
    }

    public PerformanceMode getPerformanceMode() {
        return performanceMode;
    }

    public List<PerformanceMode> getAvailablePerformanceModes() {
        List<PerformanceMode> modes = new ArrayList<>();
        for (PerformanceMode mode : PerformanceMode.values()) {
            modes.add(mode);
        }
        return modes;
    }

    public float getResolutionScale() {
        return performanceMode.resolutionScale;
    }

    public int getMaxDocuments() {
        return performanceMode.maxDocuments;
    }

    // ================================
    // 8. Auto-Capture Settings
    // ================================

    public void setAutoCaptureEnabled(boolean enabled) {
        this.autoCaptureEnabled = enabled;
        saveSettings();
        notifySettingsChanged();
    }

    public boolean isAutoCaptureEnabled() {
        return autoCaptureEnabled;
    }

    public void setCountdownDuration(int seconds) {
        this.countdownDuration = Math.max(0, Math.min(10, seconds));
        saveSettings();
        notifySettingsChanged();
    }

    public int getCountdownDuration() {
        return countdownDuration;
    }

    // ================================
    // 9. Preset Configurations
    // ================================

    public void applyPreset(Preset preset) {
        setQualityThreshold(preset.qualityThreshold);
        setOverlayTheme(preset.theme);
        setGuidelineOpacity(preset.guidelineOpacity);
        setGuidelineStyle(preset.guidelineStyle);
        setPerformanceMode(preset.performanceMode);
        setShowAREffects(preset.showEffects);

        // Save preset name
        prefs.edit().putString(KEY_CURRENT_PRESET, preset.name()).apply();

        notifySettingsChanged();

        Log.d(TAG, "Applied preset: " + preset.displayName);
    }

    public Preset getCurrentPreset() {
        String presetName = prefs.getString(KEY_CURRENT_PRESET, Preset.CUSTOM.name());
        try {
            return Preset.valueOf(presetName);
        } catch (Exception e) {
            return Preset.CUSTOM;
        }
    }

    public List<Preset> getAvailablePresets() {
        List<Preset> presets = new ArrayList<>();
        for (Preset preset : Preset.values()) {
            if (preset != Preset.CUSTOM) {
                presets.add(preset);
            }
        }
        return presets;
    }

    // ================================
    // Settings Export/Import
    // ================================

    /**
     * Export settings as map
     */
    public Map<String, Object> exportSettings() {
        Map<String, Object> settings = new HashMap<>();

        settings.put("theme", currentTheme.name());
        settings.put("quality_threshold", qualityThreshold);
        settings.put("guideline_opacity", guidelineOpacity);
        settings.put("guideline_style", guidelineStyle.name());
        settings.put("audio_feedback", audioFeedback);
        settings.put("vibration_feedback", vibrationFeedback);
        settings.put("voice_guidance", voiceGuidance);
        settings.put("ar_effect_intensity", arEffectIntensity);
        settings.put("show_ar_effects", showAREffects);
        settings.put("accessibility_mode", accessibilityMode);
        settings.put("high_contrast", highContrast);
        settings.put("large_targets", largeTargets);
        settings.put("performance_mode", performanceMode.name());
        settings.put("auto_capture_enabled", autoCaptureEnabled);
        settings.put("countdown_duration", countdownDuration);

        return settings;
    }

    /**
     * Import settings from map
     */
    public void importSettings(Map<String, Object> settings) {
        try {
            if (settings.containsKey("theme")) {
                currentTheme = OverlayTheme.valueOf((String) settings.get("theme"));
            }
            if (settings.containsKey("quality_threshold")) {
                qualityThreshold = (int) settings.get("quality_threshold");
            }
            if (settings.containsKey("guideline_opacity")) {
                guidelineOpacity = (float) settings.get("guideline_opacity");
            }
            if (settings.containsKey("guideline_style")) {
                guidelineStyle = GuidelineStyle.valueOf((String) settings.get("guideline_style"));
            }
            if (settings.containsKey("audio_feedback")) {
                audioFeedback = (boolean) settings.get("audio_feedback");
            }
            if (settings.containsKey("vibration_feedback")) {
                vibrationFeedback = (boolean) settings.get("vibration_feedback");
            }
            if (settings.containsKey("voice_guidance")) {
                voiceGuidance = (boolean) settings.get("voice_guidance");
            }
            if (settings.containsKey("ar_effect_intensity")) {
                arEffectIntensity = (float) settings.get("ar_effect_intensity");
            }
            if (settings.containsKey("show_ar_effects")) {
                showAREffects = (boolean) settings.get("show_ar_effects");
            }
            if (settings.containsKey("accessibility_mode")) {
                accessibilityMode = (boolean) settings.get("accessibility_mode");
            }
            if (settings.containsKey("high_contrast")) {
                highContrast = (boolean) settings.get("high_contrast");
            }
            if (settings.containsKey("large_targets")) {
                largeTargets = (boolean) settings.get("large_targets");
            }
            if (settings.containsKey("performance_mode")) {
                performanceMode = PerformanceMode.valueOf((String) settings.get("performance_mode"));
            }
            if (settings.containsKey("auto_capture_enabled")) {
                autoCaptureEnabled = (boolean) settings.get("auto_capture_enabled");
            }
            if (settings.containsKey("countdown_duration")) {
                countdownDuration = (int) settings.get("countdown_duration");
            }

            saveSettings();
            notifySettingsChanged();

            Log.d(TAG, "Settings imported successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error importing settings", e);
        }
    }

    /**
     * Reset to default settings
     */
    public void resetToDefaults() {
        currentTheme = OverlayTheme.DEFAULT;
        qualityThreshold = 8;
        guidelineOpacity = 0.7f;
        guidelineStyle = GuidelineStyle.SOLID;
        audioFeedback = true;
        vibrationFeedback = true;
        voiceGuidance = true;
        arEffectIntensity = 0.8f;
        showAREffects = true;
        accessibilityMode = false;
        highContrast = false;
        largeTargets = false;
        performanceMode = PerformanceMode.BALANCED;
        autoCaptureEnabled = true;
        countdownDuration = 3;

        saveSettings();
        notifySettingsChanged();

        Log.d(TAG, "Settings reset to defaults");
    }

    // ================================
    // Callbacks
    // ================================

    public void setSettingsChangeListener(SettingsChangeListener listener) {
        this.listener = listener;
    }

    private void notifySettingsChanged() {
        if (listener != null) {
            listener.onSettingsChanged();
        }
    }

    public interface SettingsChangeListener {
        void onSettingsChanged();
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Get settings summary
     */
    public String getSettingsSummary() {
        return String.format(
            "Theme: %s\n" +
            "Quality: %d/10 (%s)\n" +
            "Guidelines: %s (%.0f%% opacity)\n" +
            "Audio: %s, Vibration: %s, Voice: %s\n" +
            "AR Effects: %s (%.0f%% intensity)\n" +
            "Performance: %s\n" +
            "Auto-capture: %s (%ds countdown)\n" +
            "Accessibility: %s",
            currentTheme.name,
            qualityThreshold,
            getQualityThresholdDescription(),
            guidelineStyle.displayName,
            guidelineOpacity * 100,
            audioFeedback ? "On" : "Off",
            vibrationFeedback ? "On" : "Off",
            voiceGuidance ? "On" : "Off",
            showAREffects ? "On" : "Off",
            arEffectIntensity * 100,
            performanceMode.displayName,
            autoCaptureEnabled ? "On" : "Off",
            countdownDuration,
            accessibilityMode ? "Enabled" : "Disabled"
        );
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        listener = null;
        Log.d(TAG, "ARSettingsManager cleaned up");
    }
}

