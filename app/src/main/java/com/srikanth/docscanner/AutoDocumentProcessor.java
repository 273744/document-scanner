package com.srikanth.docscanner;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AutoDocumentProcessor - Automatic edge detection and image enhancement
 *
 * Features:
 * - Automatic document edge detection using OpenCV
 * - Perspective correction and cropping
 * - Automatic image enhancement (brightness, contrast, sharpness)
 * - Black & white conversion with adaptive thresholding
 * - Quality scoring for detected edges
 */
public class AutoDocumentProcessor {

    private static final String TAG = "AutoDocProcessor";

    // Edge detection parameters
    private static final double MIN_AREA_RATIO = 0.1; // 10% of image area
    private static final double MAX_AREA_RATIO = 0.95; // 95% of image area
    private static final int CANNY_LOW_THRESHOLD = 50;
    private static final int CANNY_HIGH_THRESHOLD = 150;
    private static final double CONTOUR_EPSILON = 0.02;
    private static final int BLUR_SIZE = 5;

    // Enhancement parameters
    private static final double ALPHA_CONTRAST = 1.5; // Contrast control (1.0-3.0)
    private static final int BETA_BRIGHTNESS = 0; // Brightness control (-100 to 100)
    private static final int ADAPTIVE_THRESHOLD_BLOCK_SIZE = 11;
    private static final int ADAPTIVE_THRESHOLD_C = 2;

    /**
     * Result class for edge detection
     */
    public static class EdgeDetectionResult {
        public boolean success;
        public PointF[] corners;
        public float qualityScore; // 0-10
        public String message;

        public EdgeDetectionResult(boolean success, PointF[] corners, float qualityScore, String message) {
            this.success = success;
            this.corners = corners;
            this.qualityScore = qualityScore;
            this.message = message;
        }
    }

    /**
     * Enhancement type
     */
    public enum EnhancementType {
        ORIGINAL,       // No enhancement
        AUTO_ENHANCE,   // Automatic brightness/contrast
        BLACK_WHITE,    // Black & white with threshold
        GRAYSCALE,      // Simple grayscale
        SHARP          // Sharpened image
    }

    // ================================
    // AUTOMATIC EDGE DETECTION
    // ================================

    /**
     * Automatically detect document edges in bitmap
     */
    public static EdgeDetectionResult detectEdges(Bitmap bitmap) {
        if (bitmap == null) {
            return new EdgeDetectionResult(false, null, 0, "Null bitmap");
        }

        try {
            // Convert bitmap to OpenCV Mat
            Mat srcMat = new Mat();
            Utils.bitmapToMat(bitmap, srcMat);

            // Process image for edge detection
            Mat grayMat = preprocessForEdgeDetection(srcMat);

            // Find document contours
            List<MatOfPoint> contours = findContours(grayMat);

            // Find best document rectangle
            MatOfPoint2f documentContour = findDocumentContour(contours, srcMat.width(), srcMat.height());

            if (documentContour != null) {
                // Convert to corner points
                Point[] points = documentContour.toArray();
                PointF[] corners = orderPoints(points);

                // Calculate quality score
                float quality = calculateEdgeQuality(corners, srcMat.width(), srcMat.height());

                // Release matrices
                srcMat.release();
                grayMat.release();
                documentContour.release();

                return new EdgeDetectionResult(true, corners, quality, "Document detected");
            } else {
                // Release matrices
                srcMat.release();
                grayMat.release();

                // Return full image corners as fallback
                PointF[] fullCorners = new PointF[]{
                    new PointF(0, 0),
                    new PointF(bitmap.getWidth(), 0),
                    new PointF(bitmap.getWidth(), bitmap.getHeight()),
                    new PointF(0, bitmap.getHeight())
                };
                return new EdgeDetectionResult(false, fullCorners, 3.0f, "No document detected, using full image");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error detecting edges", e);
            return new EdgeDetectionResult(false, null, 0, "Error: " + e.getMessage());
        }
    }

    /**
     * Preprocess image for edge detection
     */
    private static Mat preprocessForEdgeDetection(Mat srcMat) {
        Mat grayMat = new Mat();
        Mat blurMat = new Mat();
        Mat edgeMat = new Mat();

        // Convert to grayscale
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(grayMat, blurMat, new Size(BLUR_SIZE, BLUR_SIZE), 0);

        // Edge detection using Canny
        Imgproc.Canny(blurMat, edgeMat, CANNY_LOW_THRESHOLD, CANNY_HIGH_THRESHOLD);

        // Dilate to connect nearby edges
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edgeMat, edgeMat, kernel);

        // Release temporary matrices
        grayMat.release();
        blurMat.release();
        kernel.release();

        return edgeMat;
    }

    /**
     * Find contours in edge-detected image
     */
    private static List<MatOfPoint> findContours(Mat edgeMat) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(edgeMat, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        return contours;
    }

    /**
     * Find the best document contour from all detected contours
     */
    private static MatOfPoint2f findDocumentContour(List<MatOfPoint> contours, int imageWidth, int imageHeight) {
        if (contours.isEmpty()) {
            return null;
        }

        double imageArea = imageWidth * imageHeight;
        double minArea = imageArea * MIN_AREA_RATIO;
        double maxArea = imageArea * MAX_AREA_RATIO;

        // Sort contours by area (largest first)
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                return Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1));
            }
        });

        // Find first contour that forms a valid quadrilateral
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // Check area constraints
            if (area < minArea || area > maxArea) {
                continue;
            }

            // Approximate contour to polygon
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approx = new MatOfPoint2f();
            double epsilon = CONTOUR_EPSILON * Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, approx, epsilon, true);

            // Check if it's a quadrilateral
            if (approx.total() == 4 && Imgproc.isContourConvex(new MatOfPoint(approx.toArray()))) {
                contour2f.release();
                return approx;
            }

            contour2f.release();
            approx.release();
        }

        return null;
    }

    /**
     * Order points in clockwise order: top-left, top-right, bottom-right, bottom-left
     */
    private static PointF[] orderPoints(Point[] points) {
        if (points.length != 4) {
            return null;
        }

        // Calculate sum and difference for each point
        float[] sum = new float[4];
        float[] diff = new float[4];

        for (int i = 0; i < 4; i++) {
            sum[i] = (float) (points[i].x + points[i].y);
            diff[i] = (float) (points[i].y - points[i].x);
        }

        // Top-left has smallest sum
        int topLeftIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (sum[i] < sum[topLeftIdx]) topLeftIdx = i;
        }

        // Bottom-right has largest sum
        int bottomRightIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (sum[i] > sum[bottomRightIdx]) bottomRightIdx = i;
        }

        // Top-right has smallest difference
        int topRightIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (diff[i] < diff[topRightIdx]) topRightIdx = i;
        }

        // Bottom-left has largest difference
        int bottomLeftIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (diff[i] > diff[bottomLeftIdx]) bottomLeftIdx = i;
        }

        return new PointF[]{
            new PointF((float) points[topLeftIdx].x, (float) points[topLeftIdx].y),
            new PointF((float) points[topRightIdx].x, (float) points[topRightIdx].y),
            new PointF((float) points[bottomRightIdx].x, (float) points[bottomRightIdx].y),
            new PointF((float) points[bottomLeftIdx].x, (float) points[bottomLeftIdx].y)
        };
    }

    /**
     * Calculate quality score for detected edges (0-10)
     */
    private static float calculateEdgeQuality(PointF[] corners, int width, int height) {
        if (corners == null || corners.length != 4) {
            return 0;
        }

        float score = 10.0f;

        // Check if corners are too close to edges (reduce score)
        int margin = Math.min(width, height) / 20; // 5% margin
        for (PointF corner : corners) {
            if (corner.x < margin || corner.x > width - margin ||
                corner.y < margin || corner.y > height - margin) {
                score -= 1.5f;
            }
        }

        // Check if shape is roughly rectangular
        float angle1 = calculateAngle(corners[0], corners[1], corners[2]);
        float angle2 = calculateAngle(corners[1], corners[2], corners[3]);
        float angle3 = calculateAngle(corners[2], corners[3], corners[0]);
        float angle4 = calculateAngle(corners[3], corners[0], corners[1]);

        float avgAngleDiff = Math.abs(angle1 - 90) + Math.abs(angle2 - 90) +
                            Math.abs(angle3 - 90) + Math.abs(angle4 - 90);
        avgAngleDiff /= 4;

        if (avgAngleDiff > 30) {
            score -= 2.0f;
        } else if (avgAngleDiff > 15) {
            score -= 1.0f;
        }

        // Check area coverage (should be substantial)
        float detectedArea = calculatePolygonArea(corners);
        float imageArea = width * height;
        float areaCoverage = detectedArea / imageArea;

        if (areaCoverage < 0.2f) {
            score -= 2.0f;
        } else if (areaCoverage < 0.4f) {
            score -= 1.0f;
        }

        return Math.max(0, Math.min(10, score));
    }

    /**
     * Calculate angle between three points
     */
    private static float calculateAngle(PointF p1, PointF p2, PointF p3) {
        float angle = (float) Math.toDegrees(
            Math.atan2(p3.y - p2.y, p3.x - p2.x) -
            Math.atan2(p1.y - p2.y, p1.x - p2.x)
        );

        if (angle < 0) angle += 360;
        if (angle > 180) angle = 360 - angle;

        return angle;
    }

    /**
     * Calculate area of polygon
     */
    private static float calculatePolygonArea(PointF[] corners) {
        float area = 0;
        int j = corners.length - 1;

        for (int i = 0; i < corners.length; i++) {
            area += (corners[j].x + corners[i].x) * (corners[j].y - corners[i].y);
            j = i;
        }

        return Math.abs(area / 2.0f);
    }

    // ================================
    // PERSPECTIVE CORRECTION & CROP
    // ================================

    /**
     * Apply perspective correction and crop document
     */
    public static Bitmap applyCropWithPerspective(Bitmap bitmap, PointF[] corners) {
        if (bitmap == null || corners == null || corners.length != 4) {
            return bitmap;
        }

        try {
            // Convert bitmap to Mat
            Mat srcMat = new Mat();
            Utils.bitmapToMat(bitmap, srcMat);

            // Calculate destination dimensions
            float width1 = distance(corners[0], corners[1]);
            float width2 = distance(corners[2], corners[3]);
            float height1 = distance(corners[0], corners[3]);
            float height2 = distance(corners[1], corners[2]);

            int maxWidth = (int) Math.max(width1, width2);
            int maxHeight = (int) Math.max(height1, height2);

            // Source points
            MatOfPoint2f srcPoints = new MatOfPoint2f(
                new Point(corners[0].x, corners[0].y),
                new Point(corners[1].x, corners[1].y),
                new Point(corners[2].x, corners[2].y),
                new Point(corners[3].x, corners[3].y)
            );

            // Destination points (rectangle)
            MatOfPoint2f dstPoints = new MatOfPoint2f(
                new Point(0, 0),
                new Point(maxWidth - 1, 0),
                new Point(maxWidth - 1, maxHeight - 1),
                new Point(0, maxHeight - 1)
            );

            // Get perspective transform matrix
            Mat transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

            // Apply perspective transformation
            Mat dstMat = new Mat();
            Imgproc.warpPerspective(srcMat, dstMat, transformMatrix, new Size(maxWidth, maxHeight));

            // Convert back to bitmap
            Bitmap croppedBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dstMat, croppedBitmap);

            // Release matrices
            srcMat.release();
            dstMat.release();
            srcPoints.release();
            dstPoints.release();
            transformMatrix.release();

            return croppedBitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error applying perspective correction", e);
            return bitmap;
        }
    }

    /**
     * Calculate distance between two points
     */
    private static float distance(PointF p1, PointF p2) {
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ================================
    // AUTOMATIC IMAGE ENHANCEMENT
    // ================================

    /**
     * Apply automatic enhancement to bitmap
     */
    public static Bitmap autoEnhance(Bitmap bitmap) {
        return enhanceBitmap(bitmap, EnhancementType.AUTO_ENHANCE);
    }

    /**
     * Enhance bitmap with specified type
     */
    public static Bitmap enhanceBitmap(Bitmap bitmap, EnhancementType type) {
        if (bitmap == null) {
            return null;
        }

        try {
            Mat srcMat = new Mat();
            Utils.bitmapToMat(bitmap, srcMat);

            Mat enhancedMat;

            switch (type) {
                case AUTO_ENHANCE:
                    enhancedMat = applyAutoEnhancement(srcMat);
                    break;
                case BLACK_WHITE:
                    enhancedMat = applyBlackWhite(srcMat);
                    break;
                case GRAYSCALE:
                    enhancedMat = applyGrayscale(srcMat);
                    break;
                case SHARP:
                    enhancedMat = applySharpening(srcMat);
                    break;
                default:
                    enhancedMat = srcMat.clone();
                    break;
            }

            // Convert back to bitmap
            Bitmap enhancedBitmap = Bitmap.createBitmap(
                enhancedMat.width(), enhancedMat.height(), Bitmap.Config.ARGB_8888
            );
            Utils.matToBitmap(enhancedMat, enhancedBitmap);

            // Release matrices
            srcMat.release();
            enhancedMat.release();

            return enhancedBitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error enhancing bitmap", e);
            return bitmap;
        }
    }

    /**
     * Apply automatic enhancement (brightness, contrast, sharpness)
     */
    private static Mat applyAutoEnhancement(Mat srcMat) {
        Mat grayMat = new Mat();
        Mat enhancedMat = new Mat();

        // Convert to grayscale
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // Apply histogram equalization for better contrast
        Imgproc.equalizeHist(grayMat, enhancedMat);

        // Apply adaptive contrast
        enhancedMat.convertTo(enhancedMat, -1, ALPHA_CONTRAST, BETA_BRIGHTNESS);

        // Apply sharpening
        Mat sharpened = applySharpeningToMat(enhancedMat);

        // Convert back to RGBA
        Mat result = new Mat();
        Imgproc.cvtColor(sharpened, result, Imgproc.COLOR_GRAY2RGBA);

        // Release temporary matrices
        grayMat.release();
        enhancedMat.release();
        sharpened.release();

        return result;
    }

    /**
     * Apply black & white conversion with adaptive thresholding
     */
    private static Mat applyBlackWhite(Mat srcMat) {
        Mat grayMat = new Mat();
        Mat bwMat = new Mat();

        // Convert to grayscale
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // Apply adaptive thresholding
        Imgproc.adaptiveThreshold(grayMat, bwMat, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
            ADAPTIVE_THRESHOLD_BLOCK_SIZE, ADAPTIVE_THRESHOLD_C);

        // Convert back to RGBA
        Mat result = new Mat();
        Imgproc.cvtColor(bwMat, result, Imgproc.COLOR_GRAY2RGBA);

        // Release temporary matrices
        grayMat.release();
        bwMat.release();

        return result;
    }

    /**
     * Apply grayscale conversion
     */
    private static Mat applyGrayscale(Mat srcMat) {
        Mat grayMat = new Mat();
        Mat result = new Mat();

        // Convert to grayscale
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // Convert back to RGBA for consistency
        Imgproc.cvtColor(grayMat, result, Imgproc.COLOR_GRAY2RGBA);

        grayMat.release();

        return result;
    }

    /**
     * Apply sharpening filter
     */
    private static Mat applySharpening(Mat srcMat) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        Mat sharpened = applySharpeningToMat(grayMat);

        Mat result = new Mat();
        Imgproc.cvtColor(sharpened, result, Imgproc.COLOR_GRAY2RGBA);

        grayMat.release();
        sharpened.release();

        return result;
    }

    /**
     * Apply sharpening kernel to Mat
     */
    private static Mat applySharpeningToMat(Mat srcMat) {
        Mat kernel = new Mat(3, 3, CvType.CV_32F);
        kernel.put(0, 0,
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        );

        Mat sharpened = new Mat();
        Imgproc.filter2D(srcMat, sharpened, -1, kernel);

        kernel.release();

        return sharpened;
    }

    /**
     * Complete auto-process: detect edges, crop, and enhance
     */
    public static Bitmap autoProcessDocument(Bitmap bitmap, EnhancementType enhancementType) {
        if (bitmap == null) {
            return null;
        }

        Log.d(TAG, "Starting auto-process for document");

        // Step 1: Detect edges
        EdgeDetectionResult edgeResult = detectEdges(bitmap);

        if (!edgeResult.success) {
            Log.w(TAG, "Edge detection failed: " + edgeResult.message);
            // Apply enhancement to original image
            return enhanceBitmap(bitmap, enhancementType);
        }

        Log.d(TAG, "Edges detected with quality: " + edgeResult.qualityScore);

        // Step 2: Apply perspective correction and crop
        Bitmap croppedBitmap = applyCropWithPerspective(bitmap, edgeResult.corners);

        // Step 3: Apply enhancement
        Bitmap enhancedBitmap = enhanceBitmap(croppedBitmap, enhancementType);

        // Release intermediate bitmap if different
        if (croppedBitmap != bitmap && croppedBitmap != enhancedBitmap) {
            croppedBitmap.recycle();
        }

        Log.d(TAG, "Auto-process completed successfully");

        return enhancedBitmap;
    }
}

