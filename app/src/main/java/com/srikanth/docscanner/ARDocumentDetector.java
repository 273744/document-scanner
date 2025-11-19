package com.srikanth.docscanner;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ARDocumentDetector - Combines OpenCV with ARCore for document detection
 *
 * Features:
 * - Process AR camera frames for document detection
 * - Convert AR frame to OpenCV Mat format
 * - Detect rectangular document boundaries
 * - Calculate 3D coordinates for AR overlay positioning
 * - Handle multiple documents in single frame
 * - Real-time quality scoring (1-10 scale)
 * - Confidence levels for detection
 * - Coordinate transformation between camera and AR space
 */
public class ARDocumentDetector {

    private static final String TAG = "ARDocumentDetector";

    // Detection parameters
    private static final double MIN_AREA = 10000; // Minimum document area in pixels
    private static final double MAX_AREA = 1000000; // Maximum document area
    private static final double MIN_ASPECT_RATIO = 0.5; // Minimum width/height ratio
    private static final double MAX_ASPECT_RATIO = 2.0; // Maximum width/height ratio
    private static final int CANNY_THRESHOLD_1 = 50;
    private static final int CANNY_THRESHOLD_2 = 150;
    private static final double EPSILON_FACTOR = 0.02; // For contour approximation

    // Quality scoring weights
    private static final float WEIGHT_AREA = 0.25f;
    private static final float WEIGHT_CORNERS = 0.25f;
    private static final float WEIGHT_CONTRAST = 0.20f;
    private static final float WEIGHT_LIGHTING = 0.15f;
    private static final float WEIGHT_ALIGNMENT = 0.15f;

    // Processing matrices
    private Mat grayMat;
    private Mat blurredMat;
    private Mat edgesMat;
    private Mat hierarchyMat;

    // Detection results
    private List<DetectedDocument> detectedDocuments = new ArrayList<>();

    // Frame info
    private int frameWidth;
    private int frameHeight;
    private float[] cameraIntrinsics = new float[4]; // fx, fy, cx, cy

    // Statistics
    private long lastProcessTime = 0;
    private int frameCount = 0;

    /**
     * Constructor
     */
    public ARDocumentDetector() {
        // Initialize OpenCV matrices
        grayMat = new Mat();
        blurredMat = new Mat();
        edgesMat = new Mat();
        hierarchyMat = new Mat();
    }

    // ================================
    // Main Processing Pipeline
    // ================================

    /**
     * Process AR frame for document detection
     */
    public List<DetectedDocument> processFrame(Frame frame, Camera camera) {
        long startTime = System.currentTimeMillis();

        try {
            // Get camera image
            Image cameraImage = frame.acquireCameraImage();

            // Convert to OpenCV Mat
            Mat imageMat = convertARImageToMat(cameraImage);

            // Store frame dimensions
            frameWidth = imageMat.width();
            frameHeight = imageMat.height();

            // Get camera intrinsics for 3D calculations
            updateCameraIntrinsics(camera);

            // Detect documents
            detectedDocuments = detectDocuments(imageMat, frame, camera);

            // Close camera image
            cameraImage.close();

            // Update statistics
            lastProcessTime = System.currentTimeMillis() - startTime;
            frameCount++;

            if (frameCount % 30 == 0) {
                Log.d(TAG, String.format("Processed frame in %dms, found %d documents",
                    lastProcessTime, detectedDocuments.size()));
            }

        } catch (NotYetAvailableException e) {
            Log.w(TAG, "Camera image not yet available");
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }

        return detectedDocuments;
    }

    // ================================
    // AR Frame to OpenCV Conversion
    // ================================

    /**
     * Convert AR camera Image to OpenCV Mat
     */
    private Mat convertARImageToMat(Image image) {
        // AR camera image is in YUV_420_888 format
        int width = image.getWidth();
        int height = image.getHeight();

        // Get Y plane (grayscale)
        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();

        // Create Mat from Y plane
        Mat yMat = new Mat(height, width, CvType.CV_8UC1);
        byte[] yData = new byte[yBuffer.remaining()];
        yBuffer.get(yData);
        yMat.put(0, 0, yData);

        return yMat;
    }

    /**
     * Alternative: Convert to RGB Mat (slower but more versatile)
     */
    private Mat convertARImageToRGBMat(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // Convert NV21 to RGB
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
            image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);

        // Decode to Mat
        byte[] imageBytes = out.toByteArray();
        Mat rgbMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        // Additional decoding would be needed here

        return rgbMat;
    }

    // ================================
    // Document Detection Pipeline
    // ================================

    /**
     * Detect documents in OpenCV Mat
     */
    private List<DetectedDocument> detectDocuments(Mat imageMat, Frame frame, Camera camera) {
        List<DetectedDocument> documents = new ArrayList<>();

        // Preprocessing
        preprocessImage(imageMat);

        // Edge detection
        detectEdges();

        // Find contours
        List<MatOfPoint> contours = findContours();

        // Filter and analyze contours
        for (MatOfPoint contour : contours) {
            DetectedDocument doc = analyzeContour(contour, imageMat, frame, camera);

            if (doc != null && doc.confidence > 0.3f) {
                documents.add(doc);
            }
        }

        // Sort by confidence (highest first)
        Collections.sort(documents, new Comparator<DetectedDocument>() {
            @Override
            public int compare(DetectedDocument d1, DetectedDocument d2) {
                return Float.compare(d2.confidence, d1.confidence);
            }
        });

        // Limit to top 5 documents
        if (documents.size() > 5) {
            documents = documents.subList(0, 5);
        }

        return documents;
    }

    /**
     * Preprocess image for edge detection
     */
    private void preprocessImage(Mat imageMat) {
        // Convert to grayscale if needed
        if (imageMat.channels() > 1) {
            Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        } else {
            imageMat.copyTo(grayMat);
        }

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(grayMat, blurredMat, new Size(5, 5), 0);
    }

    /**
     * Detect edges using Canny
     */
    private void detectEdges() {
        Imgproc.Canny(blurredMat, edgesMat, CANNY_THRESHOLD_1, CANNY_THRESHOLD_2);

        // Dilate edges to connect nearby contours
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edgesMat, edgesMat, kernel);
    }

    /**
     * Find contours in edge map
     */
    private List<MatOfPoint> findContours() {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgesMat, contours, hierarchyMat,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    /**
     * Analyze contour to check if it's a document
     */
    private DetectedDocument analyzeContour(MatOfPoint contour, Mat imageMat,
                                           Frame frame, Camera camera) {
        // Calculate area
        double area = Imgproc.contourArea(contour);

        if (area < MIN_AREA || area > MAX_AREA) {
            return null;
        }

        // Approximate contour to polygon
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        MatOfPoint2f approx = new MatOfPoint2f();
        double epsilon = EPSILON_FACTOR * Imgproc.arcLength(contour2f, true);
        Imgproc.approxPolyDP(contour2f, approx, epsilon, true);

        // Check if it's a quadrilateral (4 corners)
        if (approx.total() != 4) {
            return null;
        }

        // Get corners
        Point[] corners = approx.toArray();

        // Check if it's convex
        MatOfPoint approxPoints = new MatOfPoint(approx.toArray());
        if (!Imgproc.isContourConvex(approxPoints)) {
            return null;
        }

        // Calculate bounding rectangle
        org.opencv.core.Rect boundingRect = Imgproc.boundingRect(contour);
        double aspectRatio = (double) boundingRect.width / boundingRect.height;

        // Check aspect ratio
        if (aspectRatio < MIN_ASPECT_RATIO || aspectRatio > MAX_ASPECT_RATIO) {
            return null;
        }

        // Create detected document
        DetectedDocument doc = new DetectedDocument();
        doc.corners2D = corners;
        doc.boundingRect = boundingRect;
        doc.area = area;
        doc.aspectRatio = aspectRatio;

        // Calculate 3D coordinates
        calculate3DCoordinates(doc, frame, camera);

        // Calculate quality score
        doc.qualityScore = calculateQualityScore(doc, imageMat, camera);

        // Calculate confidence
        doc.confidence = calculateConfidence(doc, imageMat);

        return doc;
    }

    // ================================
    // 3D Coordinate Calculation
    // ================================

    /**
     * Calculate 3D coordinates for AR overlay positioning
     */
    private void calculate3DCoordinates(DetectedDocument doc, Frame frame, Camera camera) {
        // Convert 2D screen coordinates to 3D AR coordinates
        Point[] corners2D = doc.corners2D;
        float[][] corners3D = new float[4][3];

        // Get camera pose
        Pose cameraPose = camera.getPose();

        for (int i = 0; i < 4; i++) {
            // Normalize screen coordinates to [-1, 1]
            float normalizedX = (float) ((corners2D[i].x / frameWidth) * 2.0f - 1.0f);
            float normalizedY = (float) ((corners2D[i].y / frameHeight) * 2.0f - 1.0f);

            // Estimate depth based on document size (heuristic)
            float estimatedDepth = estimateDepth(doc);

            // Unproject to 3D using camera intrinsics
            float[] point3D = unprojectPoint(normalizedX, normalizedY,
                estimatedDepth, camera);

            corners3D[i] = point3D;
        }

        doc.corners3D = corners3D;

        // Calculate center point
        doc.center3D = calculateCenter3D(corners3D);

        // Calculate normal vector (perpendicular to plane)
        doc.normal = calculateNormal(corners3D);
    }

    /**
     * Estimate depth based on document size
     */
    private float estimateDepth(DetectedDocument doc) {
        // Use known document sizes (A4: 210mm x 297mm, Letter: 216mm x 279mm)
        // Estimate depth based on image size and focal length

        double imageWidth = Math.max(doc.boundingRect.width, doc.boundingRect.height);
        double realWorldWidth = 0.297; // A4 height in meters (longest dimension)

        // Using similar triangles: depth = (realWorldWidth * focalLength) / imageWidth
        float focalLength = cameraIntrinsics[0]; // fx
        float depth = (float) ((realWorldWidth * focalLength) / imageWidth);

        // Clamp depth to reasonable range (0.2m to 2m)
        depth = Math.max(0.2f, Math.min(2.0f, depth));

        return depth;
    }

    /**
     * Unproject 2D screen point to 3D space
     */
    private float[] unprojectPoint(float normalizedX, float normalizedY,
                                   float depth, Camera camera) {
        // Get camera intrinsics
        float fx = cameraIntrinsics[0];
        float fy = cameraIntrinsics[1];
        float cx = cameraIntrinsics[2];
        float cy = cameraIntrinsics[3];

        // Convert normalized coordinates back to pixel coordinates
        float pixelX = (normalizedX + 1.0f) * frameWidth / 2.0f;
        float pixelY = (normalizedY + 1.0f) * frameHeight / 2.0f;

        // Unproject using pinhole camera model
        float x = (pixelX - cx) * depth / fx;
        float y = (pixelY - cy) * depth / fy;
        float z = -depth; // Negative Z in camera space (points away from camera)

        // Transform from camera space to world space
        Pose cameraPose = camera.getPose();
        float[] cameraSpace = {x, y, z};
        float[] worldSpace = transformToWorldSpace(cameraSpace, cameraPose);

        return worldSpace;
    }

    /**
     * Transform point from camera space to world space
     */
    private float[] transformToWorldSpace(float[] cameraSpace, Pose cameraPose) {
        // Get camera pose matrix
        float[] poseMatrix = new float[16];
        cameraPose.toMatrix(poseMatrix, 0);

        // Apply transformation
        float x = poseMatrix[0] * cameraSpace[0] + poseMatrix[4] * cameraSpace[1] +
                  poseMatrix[8] * cameraSpace[2] + poseMatrix[12];
        float y = poseMatrix[1] * cameraSpace[0] + poseMatrix[5] * cameraSpace[1] +
                  poseMatrix[9] * cameraSpace[2] + poseMatrix[13];
        float z = poseMatrix[2] * cameraSpace[0] + poseMatrix[6] * cameraSpace[1] +
                  poseMatrix[10] * cameraSpace[2] + poseMatrix[14];

        return new float[]{x, y, z};
    }

    /**
     * Calculate center point of document in 3D
     */
    private float[] calculateCenter3D(float[][] corners3D) {
        float x = 0, y = 0, z = 0;

        for (float[] corner : corners3D) {
            x += corner[0];
            y += corner[1];
            z += corner[2];
        }

        return new float[]{x / 4, y / 4, z / 4};
    }

    /**
     * Calculate normal vector of document plane
     */
    private float[] calculateNormal(float[][] corners3D) {
        // Calculate normal using cross product of two edges
        float[] v1 = subtract3D(corners3D[1], corners3D[0]);
        float[] v2 = subtract3D(corners3D[2], corners3D[0]);

        float[] normal = crossProduct(v1, v2);

        // Normalize
        float length = (float) Math.sqrt(
            normal[0] * normal[0] +
            normal[1] * normal[1] +
            normal[2] * normal[2]
        );

        return new float[]{
            normal[0] / length,
            normal[1] / length,
            normal[2] / length
        };
    }

    /**
     * Subtract two 3D vectors
     */
    private float[] subtract3D(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    /**
     * Calculate cross product of two 3D vectors
     */
    private float[] crossProduct(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    // ================================
    // Quality Scoring (1-10 scale)
    // ================================

    /**
     * Calculate quality score (1-10 scale)
     */
    private int calculateQualityScore(DetectedDocument doc, Mat imageMat, Camera camera) {
        float totalScore = 0;

        // 1. Area score (larger is better, up to a point)
        float areaScore = calculateAreaScore(doc);
        totalScore += areaScore * WEIGHT_AREA;

        // 2. Corner detection score (sharper corners = better)
        float cornerScore = calculateCornerScore(doc);
        totalScore += cornerScore * WEIGHT_CORNERS;

        // 3. Contrast score (higher contrast = better)
        float contrastScore = calculateContrastScore(doc, imageMat);
        totalScore += contrastScore * WEIGHT_CONTRAST;

        // 4. Lighting score (even lighting = better)
        float lightingScore = calculateLightingScore(imageMat, camera);
        totalScore += lightingScore * WEIGHT_LIGHTING;

        // 5. Alignment score (perpendicular to camera = better)
        float alignmentScore = calculateAlignmentScore(doc, camera);
        totalScore += alignmentScore * WEIGHT_ALIGNMENT;

        // Convert to 1-10 scale
        int finalScore = Math.round(totalScore * 10);
        return Math.max(1, Math.min(10, finalScore));
    }

    /**
     * Calculate area score component
     */
    private float calculateAreaScore(DetectedDocument doc) {
        // Optimal area is around 30-50% of frame
        double optimalArea = frameWidth * frameHeight * 0.4;
        double ratio = doc.area / optimalArea;

        if (ratio > 1.0) {
            ratio = 1.0 / ratio; // Penalize too large
        }

        return (float) ratio;
    }

    /**
     * Calculate corner sharpness score
     */
    private float calculateCornerScore(DetectedDocument doc) {
        // Check if corners form 90-degree angles (rectangular)
        float totalAngleDeviation = 0;

        for (int i = 0; i < 4; i++) {
            Point p1 = doc.corners2D[i];
            Point p2 = doc.corners2D[(i + 1) % 4];
            Point p3 = doc.corners2D[(i + 2) % 4];

            double angle = calculateAngle(p1, p2, p3);
            double deviation = Math.abs(angle - 90.0);
            totalAngleDeviation += deviation;
        }

        float avgDeviation = totalAngleDeviation / 4;
        float score = 1.0f - (avgDeviation / 90.0f);

        return Math.max(0, Math.min(1, score));
    }

    /**
     * Calculate angle between three points
     */
    private double calculateAngle(Point p1, Point p2, Point p3) {
        double v1x = p1.x - p2.x;
        double v1y = p1.y - p2.y;
        double v2x = p3.x - p2.x;
        double v2y = p3.y - p2.y;

        double dot = v1x * v2x + v1y * v2y;
        double mag1 = Math.sqrt(v1x * v1x + v1y * v1y);
        double mag2 = Math.sqrt(v2x * v2x + v2y * v2y);

        double cosAngle = dot / (mag1 * mag2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAngle))));
    }

    /**
     * Calculate contrast score
     */
    private float calculateContrastScore(DetectedDocument doc, Mat imageMat) {
        // Extract document region
        Mat roi = extractROI(imageMat, doc);

        if (roi.empty()) {
            return 0.5f;
        }

        // Calculate mean and standard deviation
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(roi, mean, stddev);

        double contrast = stddev.get(0, 0)[0];

        // Normalize to 0-1 (stddev typically 0-100)
        float score = (float) Math.min(contrast / 50.0, 1.0);

        roi.release();
        mean.release();
        stddev.release();

        return score;
    }

    /**
     * Calculate lighting score
     */
    private float calculateLightingScore(Mat imageMat, Camera camera) {
        // Use AR light estimate
        float pixelIntensity = 1.0f; // Would get from frame.getLightEstimate()

        // Optimal intensity is around 0.7-1.5
        float score;
        if (pixelIntensity < 0.5f) {
            score = pixelIntensity / 0.5f;
        } else if (pixelIntensity > 1.5f) {
            score = 1.5f / pixelIntensity;
        } else {
            score = 1.0f;
        }

        return Math.max(0, Math.min(1, score));
    }

    /**
     * Calculate alignment score (how perpendicular to camera)
     */
    private float calculateAlignmentScore(DetectedDocument doc, Camera camera) {
        if (doc.normal == null) {
            return 0.5f;
        }

        // Get camera forward direction
        Pose cameraPose = camera.getPose();
        float[] forward = cameraPose.getZAxis(); // Camera looks down -Z

        // Calculate dot product (1.0 = perpendicular, 0.0 = parallel)
        float dot = Math.abs(
            doc.normal[0] * forward[0] +
            doc.normal[1] * forward[1] +
            doc.normal[2] * forward[2]
        );

        // Perpendicular is best (dot product close to 0)
        return 1.0f - dot;
    }

    /**
     * Extract region of interest
     */
    private Mat extractROI(Mat imageMat, DetectedDocument doc) {
        try {
            return imageMat.submat(doc.boundingRect);
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract ROI", e);
            return new Mat();
        }
    }

    // ================================
    // Confidence Calculation
    // ================================

    /**
     * Calculate detection confidence (0-1 scale)
     */
    private float calculateConfidence(DetectedDocument doc, Mat imageMat) {
        float confidence = 0;

        // Area confidence (optimal size)
        double areaRatio = doc.area / (frameWidth * frameHeight);
        if (areaRatio > 0.2 && areaRatio < 0.6) {
            confidence += 0.3f;
        } else if (areaRatio > 0.1 && areaRatio < 0.8) {
            confidence += 0.15f;
        }

        // Shape confidence (4 corners, convex)
        confidence += 0.2f; // Already filtered for this

        // Aspect ratio confidence (close to A4/Letter ratio)
        double idealRatio = 1.414; // A4 ratio
        double ratioDiff = Math.abs(doc.aspectRatio - idealRatio);
        if (ratioDiff < 0.2) {
            confidence += 0.2f;
        } else if (ratioDiff < 0.5) {
            confidence += 0.1f;
        }

        // Edge strength confidence
        confidence += 0.3f * calculateEdgeStrength(doc, imageMat);

        return Math.max(0, Math.min(1, confidence));
    }

    /**
     * Calculate edge strength
     */
    private float calculateEdgeStrength(DetectedDocument doc, Mat imageMat) {
        // Sample edges around perimeter
        // Strong edges = high confidence

        // Simplified version
        return 0.8f;
    }

    // ================================
    // Camera Intrinsics
    // ================================

    /**
     * Update camera intrinsics for 3D calculations
     */
    private void updateCameraIntrinsics(Camera camera) {
        float[] intrinsics = camera.getImageIntrinsics().getFocalLength();
        float[] principal = camera.getImageIntrinsics().getPrincipalPoint();

        cameraIntrinsics[0] = intrinsics[0]; // fx
        cameraIntrinsics[1] = intrinsics[1]; // fy
        cameraIntrinsics[2] = principal[0];   // cx
        cameraIntrinsics[3] = principal[1];   // cy
    }

    // ================================
    // Getters
    // ================================

    /**
     * Get detected documents
     */
    public List<DetectedDocument> getDetectedDocuments() {
        return detectedDocuments;
    }

    /**
     * Get processing time
     */
    public long getLastProcessTime() {
        return lastProcessTime;
    }

    /**
     * Get frame count
     */
    public int getFrameCount() {
        return frameCount;
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Release OpenCV resources
     */
    public void cleanup() {
        if (grayMat != null) grayMat.release();
        if (blurredMat != null) blurredMat.release();
        if (edgesMat != null) edgesMat.release();
        if (hierarchyMat != null) hierarchyMat.release();

        detectedDocuments.clear();

        Log.d(TAG, "ARDocumentDetector cleaned up");
    }

    // ================================
    // Inner Classes
    // ================================

    /**
     * Detected document data
     */
    public static class DetectedDocument {
        // 2D data (screen space)
        public Point[] corners2D;                // 4 corners in screen coordinates
        public org.opencv.core.Rect boundingRect; // Bounding rectangle
        public double area;                       // Area in pixels
        public double aspectRatio;                // Width/height ratio

        // 3D data (AR world space)
        public float[][] corners3D;              // 4 corners in 3D space
        public float[] center3D;                 // Center point in 3D
        public float[] normal;                   // Normal vector

        // Quality metrics
        public int qualityScore;                 // 1-10 scale
        public float confidence;                 // 0-1 scale

        // Identification
        public long timestamp;                   // Detection timestamp
        public int trackingId;                   // Unique ID for tracking

        public DetectedDocument() {
            timestamp = System.currentTimeMillis();
            trackingId = (int) (Math.random() * 100000);
        }

        @Override
        public String toString() {
            return String.format("Document[id=%d, quality=%d, confidence=%.2f, area=%.0f]",
                trackingId, qualityScore, confidence, area);
        }
    }
}


