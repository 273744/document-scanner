package com.srikanth.docscanner;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Pose;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ARGuidanceManager - Intelligent user positioning guidance
 *
 * Features:
 * - Analyze camera position relative to document
 * - Generate directional guidance messages
 * - Display AR arrows for optimal positioning
 * - Voice guidance integration (TTS)
 * - Vibration feedback for alignment
 * - Progressive guidance (rough to fine)
 * - Multi-language support
 * - User learning and adaptation
 */
public class ARGuidanceManager {

    private static final String TAG = "ARGuidanceManager";

    // Distance thresholds (meters)
    private static final float OPTIMAL_DISTANCE = 0.5f;
    private static final float MIN_DISTANCE = 0.3f;
    private static final float MAX_DISTANCE = 1.0f;
    private static final float DISTANCE_TOLERANCE = 0.1f;

    // Angle thresholds (degrees)
    private static final float OPTIMAL_ANGLE = 90f; // Perpendicular
    private static final float ANGLE_TOLERANCE = 15f;
    private static final float ANGLE_WARNING = 30f;

    // Positioning stages
    private static final int STAGE_FAR = 0;
    private static final int STAGE_APPROACHING = 1;
    private static final int STAGE_NEAR = 2;
    private static final int STAGE_FINE_TUNING = 3;
    private static final int STAGE_OPTIMAL = 4;

    // Vibration patterns (milliseconds)
    private static final long[] VIBRATION_APPROACH = {0, 100};
    private static final long[] VIBRATION_ALIGNED = {0, 50, 100, 50};
    private static final long[] VIBRATION_OPTIMAL = {0, 200, 100, 200};

    // Context and components
    private Context context;
    private Vibrator vibrator;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // Current state
    private int currentStage = STAGE_FAR;
    private GuidanceDirection lastGuidance = null;
    private float currentDistance = 0f;
    private float currentAngle = 0f;
    private long lastVoiceGuidanceTime = 0;
    private long lastVibrationTime = 0;

    // Settings
    private boolean voiceGuidanceEnabled = true;
    private boolean vibrationEnabled = true;
    private boolean arrowsEnabled = true;
    private String languageCode = "en";
    private Locale currentLocale = Locale.ENGLISH;

    // Learning and adaptation
    private int alignmentCount = 0;
    private float averageAlignmentTime = 0f;
    private boolean userIsExperienced = false;

    // Callbacks
    private GuidanceCallback callback;

    /**
     * Constructor
     */
    public ARGuidanceManager(Context context) {
        this.context = context;

        // Initialize vibrator
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize Text-to-Speech
        initializeTTS();
    }

    // ================================
    // 1. Position Analysis
    // ================================

    /**
     * Analyze camera position relative to document
     */
    public GuidanceResult analyzePosition(Camera camera,
                                         ARDocumentDetector.DetectedDocument document) {
        if (document == null || document.center3D == null || document.normal == null) {
            return null;
        }

        GuidanceResult result = new GuidanceResult();

        // Get camera pose
        Pose cameraPose = camera.getPose();
        float[] cameraPos = {cameraPose.tx(), cameraPose.ty(), cameraPose.tz()};
        float[] cameraForward = cameraPose.getZAxis();

        // Calculate distance to document
        currentDistance = calculateDistance(cameraPos, document.center3D);
        result.distance = currentDistance;

        // Calculate viewing angle
        currentAngle = calculateViewingAngle(cameraForward, document.normal);
        result.angle = currentAngle;

        // Determine positioning stage
        result.stage = determineStage(currentDistance, currentAngle);

        // Generate guidance
        result.direction = determineDirection(cameraPos, document, cameraForward);

        // Calculate alignment score (0-100)
        result.alignmentScore = calculateAlignmentScore(currentDistance, currentAngle);

        // Determine if position is optimal
        result.isOptimal = (result.alignmentScore >= 90);

        return result;
    }

    /**
     * Calculate distance between two 3D points
     */
    private float calculateDistance(float[] p1, float[] p2) {
        float dx = p2[0] - p1[0];
        float dy = p2[1] - p1[1];
        float dz = p2[2] - p1[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate viewing angle (degrees)
     */
    private float calculateViewingAngle(float[] cameraForward, float[] documentNormal) {
        // Calculate dot product
        float dot = Math.abs(
            cameraForward[0] * documentNormal[0] +
            cameraForward[1] * documentNormal[1] +
            cameraForward[2] * documentNormal[2]
        );

        // Convert to angle (0 = parallel, 90 = perpendicular)
        float angle = (float) Math.toDegrees(Math.acos(Math.max(0, Math.min(1, dot))));
        return Math.abs(90 - angle); // Deviation from perpendicular
    }

    /**
     * Determine positioning stage
     */
    private int determineStage(float distance, float angle) {
        if (distance < MIN_DISTANCE - 0.1f) {
            return STAGE_FAR; // Too close
        } else if (distance > MAX_DISTANCE + 0.2f) {
            return STAGE_FAR; // Too far
        } else if (Math.abs(distance - OPTIMAL_DISTANCE) < DISTANCE_TOLERANCE &&
                   angle < ANGLE_TOLERANCE) {
            return STAGE_OPTIMAL; // Perfect position
        } else if (Math.abs(distance - OPTIMAL_DISTANCE) < DISTANCE_TOLERANCE * 2) {
            return STAGE_FINE_TUNING; // Close, needs minor adjustment
        } else if (distance > OPTIMAL_DISTANCE - 0.2f && distance < OPTIMAL_DISTANCE + 0.3f) {
            return STAGE_NEAR; // Getting close
        } else {
            return STAGE_APPROACHING; // Moving towards optimal
        }
    }

    /**
     * Determine movement direction needed
     */
    private GuidanceDirection determineDirection(float[] cameraPos,
                                                 ARDocumentDetector.DetectedDocument document,
                                                 float[] cameraForward) {
        GuidanceDirection direction = new GuidanceDirection();

        // Calculate vector from camera to document
        float dx = document.center3D[0] - cameraPos[0];
        float dy = document.center3D[1] - cameraPos[1];
        float dz = document.center3D[2] - cameraPos[2];

        // Distance adjustments
        if (currentDistance < MIN_DISTANCE) {
            direction.moveBackward = true;
        } else if (currentDistance > MAX_DISTANCE) {
            direction.moveForward = true;
        } else if (currentDistance < OPTIMAL_DISTANCE - DISTANCE_TOLERANCE) {
            direction.moveForward = true;
        } else if (currentDistance > OPTIMAL_DISTANCE + DISTANCE_TOLERANCE) {
            direction.moveBackward = true;
        }

        // Horizontal alignment
        if (Math.abs(dx) > 0.05f) {
            if (dx > 0) {
                direction.moveRight = true;
            } else {
                direction.moveLeft = true;
            }
        }

        // Vertical alignment
        if (Math.abs(dy) > 0.05f) {
            if (dy > 0) {
                direction.moveUp = true;
            } else {
                direction.moveDown = true;
            }
        }

        // Angle adjustments
        if (currentAngle > ANGLE_TOLERANCE) {
            if (currentAngle > ANGLE_WARNING) {
                direction.tiltNeeded = true;
            }
        }

        return direction;
    }

    /**
     * Calculate alignment score (0-100)
     */
    private int calculateAlignmentScore(float distance, float angle) {
        // Distance score (0-50)
        float distanceDeviation = Math.abs(distance - OPTIMAL_DISTANCE);
        float distanceScore = Math.max(0, 50 - (distanceDeviation / OPTIMAL_DISTANCE) * 50);

        // Angle score (0-50)
        float angleScore = Math.max(0, 50 - (angle / ANGLE_WARNING) * 50);

        return (int) (distanceScore + angleScore);
    }

    // ================================
    // 2. Guidance Generation
    // ================================

    /**
     * Generate guidance message
     */
    public String generateGuidanceMessage(GuidanceResult result) {
        if (result == null) {
            return getLocalizedString("point_at_document");
        }

        // Progressive guidance based on stage
        switch (result.stage) {
            case STAGE_FAR:
                if (result.distance > MAX_DISTANCE) {
                    return getLocalizedString("move_closer");
                } else {
                    return getLocalizedString("move_back");
                }

            case STAGE_APPROACHING:
                return generateApproachingGuidance(result);

            case STAGE_NEAR:
                return generateNearGuidance(result);

            case STAGE_FINE_TUNING:
                return generateFineTuningGuidance(result);

            case STAGE_OPTIMAL:
                return getLocalizedString("position_optimal");

            default:
                return getLocalizedString("point_at_document");
        }
    }

    /**
     * Generate guidance for approaching stage
     */
    private String generateApproachingGuidance(GuidanceResult result) {
        StringBuilder message = new StringBuilder();

        // Primary direction
        if (result.direction.moveForward) {
            message.append(getLocalizedString("move_closer"));
        } else if (result.direction.moveBackward) {
            message.append(getLocalizedString("move_back"));
        }

        // Secondary direction (if experienced user)
        if (userIsExperienced) {
            if (result.direction.moveLeft) {
                message.append(" ").append(getLocalizedString("and_left"));
            } else if (result.direction.moveRight) {
                message.append(" ").append(getLocalizedString("and_right"));
            }
        }

        return message.toString();
    }

    /**
     * Generate guidance for near stage
     */
    private String generateNearGuidance(GuidanceResult result) {
        // More specific guidance when close
        if (result.direction.moveLeft) {
            return getLocalizedString("move_left_slightly");
        } else if (result.direction.moveRight) {
            return getLocalizedString("move_right_slightly");
        } else if (result.direction.moveUp) {
            return getLocalizedString("move_up_slightly");
        } else if (result.direction.moveDown) {
            return getLocalizedString("move_down_slightly");
        } else if (result.direction.tiltNeeded) {
            return getLocalizedString("tilt_perpendicular");
        }

        return getLocalizedString("almost_there");
    }

    /**
     * Generate guidance for fine tuning stage
     */
    private String generateFineTuningGuidance(GuidanceResult result) {
        if (result.angle > ANGLE_TOLERANCE) {
            return getLocalizedString("adjust_angle");
        }

        // Very specific micro-adjustments
        if (result.direction.moveLeft || result.direction.moveRight ||
            result.direction.moveUp || result.direction.moveDown) {
            return getLocalizedString("minor_adjustment");
        }

        return getLocalizedString("hold_steady");
    }

    // ================================
    // 3. AR Arrow Display
    // ================================

    /**
     * Get AR arrow data for visualization
     */
    public ArrowGuidance getArrowGuidance(GuidanceResult result) {
        if (!arrowsEnabled || result == null) {
            return null;
        }

        ArrowGuidance arrows = new ArrowGuidance();

        // Determine which arrows to show
        if (result.direction.moveForward) {
            arrows.showForward = true;
        }
        if (result.direction.moveBackward) {
            arrows.showBackward = true;
        }
        if (result.direction.moveLeft) {
            arrows.showLeft = true;
        }
        if (result.direction.moveRight) {
            arrows.showRight = true;
        }
        if (result.direction.moveUp) {
            arrows.showUp = true;
        }
        if (result.direction.moveDown) {
            arrows.showDown = true;
        }

        // Arrow intensity based on deviation
        arrows.intensity = Math.min(1.0f, (100 - result.alignmentScore) / 100f);

        return arrows;
    }

    // ================================
    // 4. Voice Guidance
    // ================================

    /**
     * Initialize Text-to-Speech
     */
    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(currentLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language not supported, using default");
                    tts.setLanguage(Locale.ENGLISH);
                }
                ttsReady = true;
                Log.d(TAG, "TTS initialized successfully");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    /**
     * Speak guidance message
     */
    public void speakGuidance(String message) {
        if (!voiceGuidanceEnabled || !ttsReady || tts == null) {
            return;
        }

        // Throttle voice guidance (minimum 2 seconds between messages)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVoiceGuidanceTime < 2000) {
            return;
        }

        lastVoiceGuidanceTime = currentTime;

        // Speak message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }

        Log.d(TAG, "Voice guidance: " + message);
    }

    /**
     * Speak guidance for result
     */
    public void speakGuidanceForResult(GuidanceResult result) {
        String message = generateGuidanceMessage(result);
        speakGuidance(message);
    }

    // ================================
    // 5. Vibration Feedback
    // ================================

    /**
     * Provide vibration feedback
     */
    public void provideVibrationFeedback(GuidanceResult result) {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        // Throttle vibrations (minimum 500ms between)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVibrationTime < 500) {
            return;
        }

        lastVibrationTime = currentTime;

        // Vibration pattern based on stage
        long[] pattern = null;

        switch (result.stage) {
            case STAGE_APPROACHING:
            case STAGE_NEAR:
                pattern = VIBRATION_APPROACH;
                break;

            case STAGE_FINE_TUNING:
                pattern = VIBRATION_ALIGNED;
                break;

            case STAGE_OPTIMAL:
                pattern = VIBRATION_OPTIMAL;
                // Update learning data
                recordSuccessfulAlignment();
                break;
        }

        if (pattern != null) {
            vibrate(pattern);
        }
    }

    /**
     * Vibrate with pattern
     */
    private void vibrate(long[] pattern) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    // ================================
    // 6. Progressive Guidance
    // ================================

    /**
     * Update guidance based on progress
     */
    public void updateProgressiveGuidance(GuidanceResult result) {
        if (result == null) {
            return;
        }

        int newStage = result.stage;

        // Stage changed - provide feedback
        if (newStage != currentStage) {
            onStageChanged(currentStage, newStage, result);
            currentStage = newStage;
        }

        // Update callback
        if (callback != null) {
            callback.onGuidanceUpdate(result);
        }
    }

    /**
     * Handle stage transition
     */
    private void onStageChanged(int oldStage, int newStage, GuidanceResult result) {
        Log.d(TAG, "Stage changed: " + oldStage + " -> " + newStage);

        // Provide appropriate feedback
        switch (newStage) {
            case STAGE_APPROACHING:
                speakGuidance(getLocalizedString("getting_closer"));
                break;

            case STAGE_NEAR:
                speakGuidance(getLocalizedString("almost_there"));
                vibrate(VIBRATION_APPROACH);
                break;

            case STAGE_FINE_TUNING:
                speakGuidance(getLocalizedString("fine_tuning"));
                vibrate(VIBRATION_ALIGNED);
                break;

            case STAGE_OPTIMAL:
                speakGuidance(getLocalizedString("position_optimal"));
                vibrate(VIBRATION_OPTIMAL);
                if (callback != null) {
                    callback.onOptimalPositionReached(result);
                }
                break;
        }
    }

    // ================================
    // 7. Multi-Language Support
    // ================================

    /**
     * Set language
     */
    public void setLanguage(String languageCode) {
        this.languageCode = languageCode;
        this.currentLocale = new Locale(languageCode);

        // Update TTS language
        if (tts != null && ttsReady) {
            tts.setLanguage(currentLocale);
        }

        Log.d(TAG, "Language set to: " + languageCode);
    }

    /**
     * Get localized string
     */
    private String getLocalizedString(String key) {
        Map<String, String> strings = getLocalizedStrings();
        return strings.getOrDefault(key, key);
    }

    /**
     * Get localized strings for current language
     */
    private Map<String, String> getLocalizedStrings() {
        Map<String, String> strings = new HashMap<>();

        switch (languageCode) {
            case "en":
                strings.put("point_at_document", "Point camera at document");
                strings.put("move_closer", "Move closer");
                strings.put("move_back", "Move back");
                strings.put("move_left_slightly", "Move slightly left");
                strings.put("move_right_slightly", "Move slightly right");
                strings.put("move_up_slightly", "Move slightly up");
                strings.put("move_down_slightly", "Move slightly down");
                strings.put("tilt_perpendicular", "Hold camera perpendicular");
                strings.put("adjust_angle", "Adjust camera angle");
                strings.put("minor_adjustment", "Minor adjustment needed");
                strings.put("hold_steady", "Hold steady");
                strings.put("getting_closer", "Getting closer");
                strings.put("almost_there", "Almost there");
                strings.put("fine_tuning", "Fine tuning position");
                strings.put("position_optimal", "Perfect position!");
                strings.put("and_left", "and left");
                strings.put("and_right", "and right");
                break;

            case "es":
                strings.put("point_at_document", "Apunta la cámara al documento");
                strings.put("move_closer", "Acércate");
                strings.put("move_back", "Aléjate");
                strings.put("move_left_slightly", "Mueve un poco a la izquierda");
                strings.put("move_right_slightly", "Mueve un poco a la derecha");
                strings.put("position_optimal", "¡Posición perfecta!");
                break;

            case "fr":
                strings.put("point_at_document", "Pointez la caméra vers le document");
                strings.put("move_closer", "Approchez-vous");
                strings.put("move_back", "Reculez");
                strings.put("position_optimal", "Position parfaite!");
                break;

            case "de":
                strings.put("point_at_document", "Kamera auf Dokument richten");
                strings.put("move_closer", "Näher kommen");
                strings.put("move_back", "Zurückgehen");
                strings.put("position_optimal", "Perfekte Position!");
                break;

            case "zh":
                strings.put("point_at_document", "将相机对准文档");
                strings.put("move_closer", "靠近一点");
                strings.put("move_back", "后退一点");
                strings.put("position_optimal", "位置完美！");
                break;

            case "ja":
                strings.put("point_at_document", "カメラを書類に向けてください");
                strings.put("move_closer", "近づいてください");
                strings.put("move_back", "離れてください");
                strings.put("position_optimal", "完璧な位置です！");
                break;

            default:
                // Default to English
                return getLocalizedStrings(); // Recursive call with "en"
        }

        return strings;
    }

    // ================================
    // 8. User Learning
    // ================================

    /**
     * Record successful alignment
     */
    private void recordSuccessfulAlignment() {
        alignmentCount++;

        // Calculate average alignment time
        // (Would need to track start time in real implementation)

        // After 5 successful alignments, consider user experienced
        if (alignmentCount >= 5 && !userIsExperienced) {
            userIsExperienced = true;
            Log.d(TAG, "User is now considered experienced");

            if (callback != null) {
                callback.onUserBecameExperienced();
            }
        }
    }

    /**
     * Reset user learning data
     */
    public void resetUserLearning() {
        alignmentCount = 0;
        averageAlignmentTime = 0f;
        userIsExperienced = false;
    }

    // ================================
    // Settings
    // ================================

    public void setVoiceGuidanceEnabled(boolean enabled) {
        this.voiceGuidanceEnabled = enabled;
    }

    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    public void setArrowsEnabled(boolean enabled) {
        this.arrowsEnabled = enabled;
    }

    public boolean isUserExperienced() {
        return userIsExperienced;
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(GuidanceCallback callback) {
        this.callback = callback;
    }

    public interface GuidanceCallback {
        void onGuidanceUpdate(GuidanceResult result);
        void onOptimalPositionReached(GuidanceResult result);
        void onUserBecameExperienced();
    }

    // ================================
    // Data Classes
    // ================================

    public static class GuidanceResult {
        public float distance;
        public float angle;
        public int stage;
        public GuidanceDirection direction;
        public int alignmentScore;
        public boolean isOptimal;
    }

    public static class GuidanceDirection {
        public boolean moveForward;
        public boolean moveBackward;
        public boolean moveLeft;
        public boolean moveRight;
        public boolean moveUp;
        public boolean moveDown;
        public boolean tiltNeeded;
    }

    public static class ArrowGuidance {
        public boolean showForward;
        public boolean showBackward;
        public boolean showLeft;
        public boolean showRight;
        public boolean showUp;
        public boolean showDown;
        public float intensity;
    }

    // ================================
    // Cleanup
    // ================================

    public void cleanup() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        callback = null;

        Log.d(TAG, "ARGuidanceManager cleaned up");
    }
}


