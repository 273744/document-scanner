package com.srikanth.docscanner;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.LightEstimate;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * DocumentQualityAnalyzer - Live quality scoring and feedback system
 *
 * Features:
 * - Analyze document sharpness and focus
 * - Check lighting conditions and shadows
 * - Evaluate perspective and angle quality
 * - Detect motion blur and camera shake
 * - Calculate overall quality score (1-10)
 * - Provide specific improvement suggestions
 * - Determine optimal capture timing
 * - Return actionable guidance for users
 */
public class DocumentQualityAnalyzer {

    private static final String TAG = "DocumentQualityAnalyzer";

    // Quality thresholds
    private static final double SHARPNESS_THRESHOLD_EXCELLENT = 100.0;
    private static final double SHARPNESS_THRESHOLD_GOOD = 50.0;
    private static final double SHARPNESS_THRESHOLD_FAIR = 25.0;

    private static final double LIGHTING_MIN_GOOD = 0.5;
    private static final double LIGHTING_MAX_GOOD = 1.8;
    private static final double LIGHTING_MIN_FAIR = 0.3;
    private static final double LIGHTING_MAX_FAIR = 2.5;

    private static final double MOTION_BLUR_THRESHOLD = 0.3;
    private static final double SHADOW_THRESHOLD = 0.4;

    // Scoring weights
    private static final float WEIGHT_SHARPNESS = 0.30f;
    private static final float WEIGHT_LIGHTING = 0.25f;
    private static final float WEIGHT_PERSPECTIVE = 0.20f;
    private static final float WEIGHT_MOTION = 0.15f;
    private static final float WEIGHT_UNIFORMITY = 0.10f;

    // History for temporal analysis
    private List<Double> sharpnessHistory = new ArrayList<>();
    private List<Double> motionHistory = new ArrayList<>();
    private static final int HISTORY_SIZE = 10;

    // Analysis matrices
    private Mat laplacianMat;
    private Mat sobelX;
    private Mat sobelY;
    private Mat grayMat;
    private Mat blurredMat;

    /**
     * Constructor
     */
    public DocumentQualityAnalyzer() {
        laplacianMat = new Mat();
        sobelX = new Mat();
        sobelY = new Mat();
        grayMat = new Mat();
        blurredMat = new Mat();
    }

    // ================================
    // Main Quality Analysis
    // ================================

    /**
     * Analyze document quality and return comprehensive results
     */
    public QualityResult analyzeQuality(Mat documentImage, Frame frame, Camera camera,
                                       ARDocumentDetector.DetectedDocument document) {
        QualityResult result = new QualityResult();

        // 1. Analyze sharpness and focus
        result.sharpnessScore = analyzeSharpness(documentImage);
        result.sharpnessQuality = evaluateSharpness(result.sharpnessScore);

        // 2. Check lighting conditions
        result.lightingScore = analyzeLighting(documentImage, frame);
        result.lightingQuality = evaluateLighting(result.lightingScore);
        result.hasShadows = detectShadows(documentImage);

        // 3. Evaluate perspective and angle
        result.perspectiveScore = analyzePerspective(document, camera);
        result.perspectiveQuality = evaluatePerspective(result.perspectiveScore);

        // 4. Detect motion blur
        result.motionBlurScore = detectMotionBlur(documentImage);
        result.hasMotionBlur = result.motionBlurScore > MOTION_BLUR_THRESHOLD;

        // 5. Check uniformity (even lighting, no glare)
        result.uniformityScore = analyzeUniformity(documentImage);

        // 6. Calculate overall quality score (1-10)
        result.overallScore = calculateOverallScore(result);

        // 7. Determine optimal capture timing
        result.isOptimalForCapture = determineOptimalTiming(result);

        // 8. Generate improvement suggestions
        result.suggestions = generateSuggestions(result);

        // 9. Get capture recommendation
        result.recommendation = getRecommendation(result);

        return result;
    }

    // ================================
    // 1. Sharpness Analysis
    // ================================

    /**
     * Analyze document sharpness using Laplacian variance
     */
    private double analyzeSharpness(Mat documentImage) {
        // Convert to grayscale if needed
        if (documentImage.channels() > 1) {
            Imgproc.cvtColor(documentImage, grayMat, Imgproc.COLOR_BGR2GRAY);
        } else {
            documentImage.copyTo(grayMat);
        }

        // Apply Laplacian filter
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F);

        // Calculate variance (higher = sharper)
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacianMat, mean, stddev);

        double variance = Math.pow(stddev.get(0, 0)[0], 2);

        // Add to history
        updateHistory(sharpnessHistory, variance);

        Log.d(TAG, String.format("Sharpness variance: %.2f", variance));

        return variance;
    }

    /**
     * Evaluate sharpness quality level
     */
    private QualityLevel evaluateSharpness(double sharpnessScore) {
        if (sharpnessScore >= SHARPNESS_THRESHOLD_EXCELLENT) {
            return QualityLevel.EXCELLENT;
        } else if (sharpnessScore >= SHARPNESS_THRESHOLD_GOOD) {
            return QualityLevel.GOOD;
        } else if (sharpnessScore >= SHARPNESS_THRESHOLD_FAIR) {
            return QualityLevel.FAIR;
        } else {
            return QualityLevel.POOR;
        }
    }

    /**
     * Additional sharpness analysis using edge detection
     */
    private double analyzeEdgeSharpness(Mat documentImage) {
        // Sobel edge detection
        Imgproc.Sobel(grayMat, sobelX, CvType.CV_64F, 1, 0);
        Imgproc.Sobel(grayMat, sobelY, CvType.CV_64F, 0, 1);

        // Calculate edge magnitude
        Mat magnitude = new Mat();
        Core.magnitude(sobelX, sobelY, magnitude);

        // Mean edge strength
        Scalar meanEdge = Core.mean(magnitude);
        double edgeStrength = meanEdge.val[0];

        magnitude.release();

        return edgeStrength;
    }

    // ================================
    // 2. Lighting Analysis
    // ================================

    /**
     * Analyze lighting conditions
     */
    private double analyzeLighting(Mat documentImage, Frame frame) {
        double lightScore = 0;

        // Get AR light estimate
        LightEstimate lightEstimate = frame.getLightEstimate();
        float pixelIntensity = lightEstimate.getPixelIntensity();

        // Analyze image brightness
        double imageBrightness = analyzeImageBrightness(documentImage);

        // Combine AR and image-based lighting info
        lightScore = (pixelIntensity + imageBrightness / 255.0) / 2.0;

        Log.d(TAG, String.format("Lighting - AR: %.2f, Image: %.2f, Combined: %.2f",
            pixelIntensity, imageBrightness, lightScore));

        return lightScore;
    }

    /**
     * Analyze image brightness
     */
    private double analyzeImageBrightness(Mat documentImage) {
        // Convert to grayscale
        if (documentImage.channels() > 1) {
            Imgproc.cvtColor(documentImage, grayMat, Imgproc.COLOR_BGR2GRAY);
        } else {
            documentImage.copyTo(grayMat);
        }

        // Calculate mean brightness
        Scalar meanBrightness = Core.mean(grayMat);
        return meanBrightness.val[0];
    }

    /**
     * Evaluate lighting quality
     */
    private QualityLevel evaluateLighting(double lightingScore) {
        if (lightingScore >= LIGHTING_MIN_GOOD && lightingScore <= LIGHTING_MAX_GOOD) {
            return QualityLevel.EXCELLENT;
        } else if (lightingScore >= LIGHTING_MIN_FAIR && lightingScore <= LIGHTING_MAX_FAIR) {
            return QualityLevel.FAIR;
        } else {
            return QualityLevel.POOR;
        }
    }

    /**
     * Detect shadows in document
     */
    private boolean detectShadows(Mat documentImage) {
        // Convert to HSV
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(documentImage, hsvMat, Imgproc.COLOR_BGR2HSV);

        // Extract V channel (brightness)
        List<Mat> channels = new ArrayList<>();
        Core.split(hsvMat, channels);
        Mat vChannel = channels.get(2);

        // Calculate brightness variation
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(vChannel, mean, stddev);

        double variation = stddev.get(0, 0)[0] / mean.get(0, 0)[0];

        // High variation indicates shadows/uneven lighting
        boolean hasShadows = variation > SHADOW_THRESHOLD;

        // Cleanup
        hsvMat.release();
        for (Mat channel : channels) {
            channel.release();
        }

        Log.d(TAG, String.format("Shadow detection - Variation: %.2f, Has shadows: %b",
            variation, hasShadows));

        return hasShadows;
    }

    // ================================
    // 3. Perspective Analysis
    // ================================

    /**
     * Analyze perspective and viewing angle
     */
    private double analyzePerspective(ARDocumentDetector.DetectedDocument document,
                                      Camera camera) {
        if (document == null || document.normal == null) {
            return 0.5; // Neutral score
        }

        // Get camera viewing direction
        float[] cameraForward = camera.getPose().getZAxis();

        // Calculate angle between camera and document normal
        float dotProduct = Math.abs(
            document.normal[0] * cameraForward[0] +
            document.normal[1] * cameraForward[1] +
            document.normal[2] * cameraForward[2]
        );

        // Perpendicular view is best (dot product close to 0)
        double perspectiveScore = 1.0 - dotProduct;

        // Also check aspect ratio distortion
        double aspectRatioScore = evaluateAspectRatio(document.aspectRatio);

        // Combine scores
        double finalScore = (perspectiveScore * 0.7 + aspectRatioScore * 0.3);

        Log.d(TAG, String.format("Perspective - Angle score: %.2f, Aspect score: %.2f, Final: %.2f",
            perspectiveScore, aspectRatioScore, finalScore));

        return finalScore;
    }

    /**
     * Evaluate aspect ratio quality
     */
    private double evaluateAspectRatio(double aspectRatio) {
        // A4 ratio: 1.414, Letter: 1.294
        double idealRatio = 1.414;
        double difference = Math.abs(aspectRatio - idealRatio);

        // Score based on difference
        if (difference < 0.1) {
            return 1.0; // Excellent
        } else if (difference < 0.3) {
            return 0.8; // Good
        } else if (difference < 0.5) {
            return 0.5; // Fair
        } else {
            return 0.2; // Poor
        }
    }

    /**
     * Evaluate perspective quality level
     */
    private QualityLevel evaluatePerspective(double perspectiveScore) {
        if (perspectiveScore >= 0.8) {
            return QualityLevel.EXCELLENT;
        } else if (perspectiveScore >= 0.6) {
            return QualityLevel.GOOD;
        } else if (perspectiveScore >= 0.4) {
            return QualityLevel.FAIR;
        } else {
            return QualityLevel.POOR;
        }
    }

    // ================================
    // 4. Motion Blur Detection
    // ================================

    /**
     * Detect motion blur and camera shake
     */
    private double detectMotionBlur(Mat documentImage) {
        // Use frequency domain analysis
        double motionScore = analyzeMotionBlurFrequency(documentImage);

        // Also check temporal stability
        double temporalScore = analyzeTemporalStability();

        // Combine scores
        double finalScore = (motionScore * 0.7 + temporalScore * 0.3);

        // Add to history
        updateHistory(motionHistory, finalScore);

        Log.d(TAG, String.format("Motion blur score: %.2f", finalScore));

        return finalScore;
    }

    /**
     * Analyze motion blur using frequency domain
     */
    private double analyzeMotionBlurFrequency(Mat documentImage) {
        // Simplified motion blur detection
        // Real implementation would use FFT

        // Apply Gaussian blur and compare
        Imgproc.GaussianBlur(grayMat, blurredMat, new Size(5, 5), 0);

        // Calculate difference
        Mat diff = new Mat();
        Core.absdiff(grayMat, blurredMat, diff);

        Scalar meanDiff = Core.mean(diff);
        double blurAmount = meanDiff.val[0] / 255.0;

        diff.release();

        // Higher difference = less blur
        return 1.0 - blurAmount;
    }

    /**
     * Analyze temporal stability (frame-to-frame consistency)
     */
    private double analyzeTemporalStability() {
        if (sharpnessHistory.size() < 3) {
            return 0.5; // Neutral
        }

        // Calculate variance in recent sharpness measurements
        double sum = 0;
        double sumSq = 0;
        int count = Math.min(5, sharpnessHistory.size());

        for (int i = sharpnessHistory.size() - count; i < sharpnessHistory.size(); i++) {
            double value = sharpnessHistory.get(i);
            sum += value;
            sumSq += value * value;
        }

        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);
        double stability = 1.0 / (1.0 + variance / (mean * mean + 1.0));

        return stability;
    }

    // ================================
    // 5. Uniformity Analysis
    // ================================

    /**
     * Analyze uniformity (even lighting, no glare)
     */
    private double analyzeUniformity(Mat documentImage) {
        // Convert to grayscale
        if (documentImage.channels() > 1) {
            Imgproc.cvtColor(documentImage, grayMat, Imgproc.COLOR_BGR2GRAY);
        } else {
            documentImage.copyTo(grayMat);
        }

        // Calculate histogram
        Mat hist = new Mat();
        Imgproc.calcHist(
            java.util.Arrays.asList(grayMat),
            new org.opencv.core.MatOfInt(0),
            new Mat(),
            hist,
            new org.opencv.core.MatOfInt(256),
            new org.opencv.core.MatOfFloat(0, 256)
        );

        // Analyze histogram distribution
        double uniformityScore = analyzeHistogramUniformity(hist);

        hist.release();

        Log.d(TAG, String.format("Uniformity score: %.2f", uniformityScore));

        return uniformityScore;
    }

    /**
     * Analyze histogram uniformity
     */
    private double analyzeHistogramUniformity(Mat hist) {
        // Calculate entropy or standard deviation
        double sum = 0;
        double sumSq = 0;
        int count = 0;

        for (int i = 0; i < hist.rows(); i++) {
            double value = hist.get(i, 0)[0];
            if (value > 0) {
                sum += value;
                sumSq += value * value;
                count++;
            }
        }

        if (count == 0) return 0;

        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);
        double stddev = Math.sqrt(variance);

        // Lower stddev = more uniform
        double uniformity = 1.0 / (1.0 + stddev / (mean + 1.0));

        return uniformity;
    }

    // ================================
    // 6. Overall Score Calculation
    // ================================

    /**
     * Calculate overall quality score (1-10)
     */
    private int calculateOverallScore(QualityResult result) {
        // Normalize component scores to 0-1
        double sharpnessNorm = normalizeSharpness(result.sharpnessScore);
        double lightingNorm = normalizeLighting(result.lightingScore);
        double perspectiveNorm = result.perspectiveScore;
        double motionNorm = 1.0 - result.motionBlurScore;
        double uniformityNorm = result.uniformityScore;

        // Apply weights
        double weightedScore =
            sharpnessNorm * WEIGHT_SHARPNESS +
            lightingNorm * WEIGHT_LIGHTING +
            perspectiveNorm * WEIGHT_PERSPECTIVE +
            motionNorm * WEIGHT_MOTION +
            uniformityNorm * WEIGHT_UNIFORMITY;

        // Apply penalties
        if (result.hasShadows) {
            weightedScore *= 0.85; // 15% penalty
        }

        if (result.hasMotionBlur) {
            weightedScore *= 0.80; // 20% penalty
        }

        // Convert to 1-10 scale
        int finalScore = (int) Math.round(weightedScore * 10);
        finalScore = Math.max(1, Math.min(10, finalScore));

        Log.d(TAG, String.format("Overall quality score: %d/10", finalScore));

        return finalScore;
    }

    /**
     * Normalize sharpness score to 0-1
     */
    private double normalizeSharpness(double sharpnessScore) {
        // Use sigmoid-like function
        return Math.min(1.0, sharpnessScore / SHARPNESS_THRESHOLD_EXCELLENT);
    }

    /**
     * Normalize lighting score to 0-1
     */
    private double normalizeLighting(double lightingScore) {
        // Optimal range is 0.5-1.8
        if (lightingScore < LIGHTING_MIN_GOOD) {
            return lightingScore / LIGHTING_MIN_GOOD;
        } else if (lightingScore > LIGHTING_MAX_GOOD) {
            return LIGHTING_MAX_GOOD / lightingScore;
        } else {
            return 1.0; // Perfect
        }
    }

    // ================================
    // 7. Optimal Timing Detection
    // ================================

    /**
     * Determine if current moment is optimal for capture
     */
    private boolean determineOptimalTiming(QualityResult result) {
        // Check if all key metrics are good
        boolean sharpnessGood = result.sharpnessScore >= SHARPNESS_THRESHOLD_GOOD;
        boolean lightingGood = result.lightingQuality != QualityLevel.POOR;
        boolean perspectiveGood = result.perspectiveScore >= 0.6;
        boolean noMotion = !result.hasMotionBlur;
        boolean stable = isStable();

        boolean isOptimal = sharpnessGood && lightingGood && perspectiveGood &&
                           noMotion && stable;

        Log.d(TAG, String.format("Optimal timing: %b (sharp:%b, light:%b, persp:%b, motion:%b, stable:%b)",
            isOptimal, sharpnessGood, lightingGood, perspectiveGood, noMotion, stable));

        return isOptimal;
    }

    /**
     * Check if measurements are stable (not changing rapidly)
     */
    private boolean isStable() {
        if (sharpnessHistory.size() < 5) {
            return false;
        }

        // Check recent variance
        int count = 5;
        double sum = 0;
        double sumSq = 0;

        for (int i = sharpnessHistory.size() - count; i < sharpnessHistory.size(); i++) {
            double value = sharpnessHistory.get(i);
            sum += value;
            sumSq += value * value;
        }

        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);
        double coefficientOfVariation = Math.sqrt(variance) / (mean + 1.0);

        // Stable if CV < 0.1
        return coefficientOfVariation < 0.1;
    }

    // ================================
    // 8. Improvement Suggestions
    // ================================

    /**
     * Generate specific improvement suggestions
     */
    private List<String> generateSuggestions(QualityResult result) {
        List<String> suggestions = new ArrayList<>();

        // Sharpness suggestions
        if (result.sharpnessQuality == QualityLevel.POOR) {
            suggestions.add("Hold phone steady - image is blurry");
            suggestions.add("Tap screen to focus on document");
        } else if (result.sharpnessQuality == QualityLevel.FAIR) {
            suggestions.add("Hold phone steadier for sharper image");
        }

        // Lighting suggestions
        if (result.lightingScore < LIGHTING_MIN_FAIR) {
            suggestions.add("Need more light - turn on flash or move to brighter area");
        } else if (result.lightingScore > LIGHTING_MAX_FAIR) {
            suggestions.add("Too bright - reduce direct light or turn off flash");
        } else if (result.lightingQuality == QualityLevel.FAIR) {
            suggestions.add("Improve lighting for better quality");
        }

        // Shadow suggestions
        if (result.hasShadows) {
            suggestions.add("Shadows detected - adjust lighting angle");
        }

        // Perspective suggestions
        if (result.perspectiveQuality == QualityLevel.POOR) {
            suggestions.add("Move phone directly above document");
            suggestions.add("Align document parallel to phone");
        } else if (result.perspectiveQuality == QualityLevel.FAIR) {
            suggestions.add("Adjust angle - hold phone more perpendicular");
        }

        // Motion suggestions
        if (result.hasMotionBlur) {
            suggestions.add("Stop moving - camera shake detected");
            suggestions.add("Brace phone against stable surface if possible");
        }

        // Positive feedback
        if (result.overallScore >= 8) {
            suggestions.add("✓ Quality is excellent - ready to capture!");
        } else if (result.overallScore >= 6) {
            suggestions.add("✓ Quality is good - minor improvements possible");
        }

        return suggestions;
    }

    // ================================
    // 9. Capture Recommendation
    // ================================

    /**
     * Get capture recommendation
     */
    private CaptureRecommendation getRecommendation(QualityResult result) {
        if (result.isOptimalForCapture && result.overallScore >= 8) {
            return CaptureRecommendation.CAPTURE_NOW;
        } else if (result.overallScore >= 6) {
            return CaptureRecommendation.READY;
        } else if (result.overallScore >= 4) {
            return CaptureRecommendation.NEEDS_IMPROVEMENT;
        } else {
            return CaptureRecommendation.NOT_READY;
        }
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Update history list with new value
     */
    private void updateHistory(List<Double> history, double value) {
        history.add(value);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
    }

    /**
     * Reset analyzer state
     */
    public void reset() {
        sharpnessHistory.clear();
        motionHistory.clear();
    }

    /**
     * Cleanup OpenCV resources
     */
    public void cleanup() {
        if (laplacianMat != null) laplacianMat.release();
        if (sobelX != null) sobelX.release();
        if (sobelY != null) sobelY.release();
        if (grayMat != null) grayMat.release();
        if (blurredMat != null) blurredMat.release();

        sharpnessHistory.clear();
        motionHistory.clear();

        Log.d(TAG, "DocumentQualityAnalyzer cleaned up");
    }

    // ================================
    // Inner Classes and Enums
    // ================================

    /**
     * Quality level enumeration
     */
    public enum QualityLevel {
        EXCELLENT("Excellent"),
        GOOD("Good"),
        FAIR("Fair"),
        POOR("Poor");

        private final String displayName;

        QualityLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Capture recommendation
     */
    public enum CaptureRecommendation {
        CAPTURE_NOW("Capture Now - Perfect Quality!"),
        READY("Ready to Capture"),
        NEEDS_IMPROVEMENT("Improve Quality Before Capture"),
        NOT_READY("Not Ready - Follow Suggestions");

        private final String message;

        CaptureRecommendation(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Quality analysis result
     */
    public static class QualityResult {
        // Component scores
        public double sharpnessScore;
        public QualityLevel sharpnessQuality;

        public double lightingScore;
        public QualityLevel lightingQuality;
        public boolean hasShadows;

        public double perspectiveScore;
        public QualityLevel perspectiveQuality;

        public double motionBlurScore;
        public boolean hasMotionBlur;

        public double uniformityScore;

        // Overall
        public int overallScore; // 1-10 scale

        // Timing
        public boolean isOptimalForCapture;

        // Feedback
        public List<String> suggestions;
        public CaptureRecommendation recommendation;

        /**
         * Get formatted quality report
         */
        public String getQualityReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Quality Analysis ===\n");
            report.append(String.format("Overall Score: %d/10\n\n", overallScore));
            report.append(String.format("Sharpness: %s (%.2f)\n",
                sharpnessQuality.getDisplayName(), sharpnessScore));
            report.append(String.format("Lighting: %s (%.2f)%s\n",
                lightingQuality.getDisplayName(), lightingScore,
                hasShadows ? " - Shadows Detected" : ""));
            report.append(String.format("Perspective: %s (%.2f)\n",
                perspectiveQuality.getDisplayName(), perspectiveScore));
            report.append(String.format("Motion Blur: %s\n",
                hasMotionBlur ? "Detected" : "None"));
            report.append(String.format("Uniformity: %.2f\n\n", uniformityScore));
            report.append(String.format("Recommendation: %s\n\n",
                recommendation.getMessage()));

            if (!suggestions.isEmpty()) {
                report.append("Suggestions:\n");
                for (String suggestion : suggestions) {
                    report.append("• ").append(suggestion).append("\n");
                }
            }

            return report.toString();
        }

        @Override
        public String toString() {
            return String.format("QualityResult[score=%d/10, sharp=%s, light=%s, persp=%s, optimal=%b]",
                overallScore, sharpnessQuality, lightingQuality, perspectiveQuality,
                isOptimalForCapture);
        }
    }
}


