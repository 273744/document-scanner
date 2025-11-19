package com.srikanth.docscanner;

import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;

import org.opencv.core.Point;

import java.util.List;

/**
 * ARCoordinateMapper - 3D coordinate transformation system for AR overlays
 *
 * Features:
 * - Convert document corners from 2D image to 3D AR space
 * - Handle camera projection matrix transformations
 * - Map screen coordinates to world coordinates
 * - Calculate proper overlay positioning for documents
 * - Handle device orientation changes
 * - Maintain accurate tracking during camera movement
 * - Mathematical transformations for perspective correction
 */
public class ARCoordinateMapper {

    private static final String TAG = "ARCoordinateMapper";

    // Camera intrinsics
    private float[] cameraIntrinsics = new float[4]; // fx, fy, cx, cy
    private int imageWidth;
    private int imageHeight;

    // Transformation matrices
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];
    private float[] inverseViewProjectionMatrix = new float[16];

    // Device orientation
    private int displayRotation = 0;
    private boolean orientationChanged = false;

    // Coordinate systems
    public enum CoordinateSystem {
        SCREEN,      // 2D screen coordinates (pixels)
        NDC,         // Normalized Device Coordinates (-1 to 1)
        CAMERA,      // Camera space (right-handed, -Z forward)
        WORLD        // AR world space
    }

    /**
     * Constructor
     */
    public ARCoordinateMapper() {
        // Initialize identity matrices
        android.opengl.Matrix.setIdentityM(viewMatrix, 0);
        android.opengl.Matrix.setIdentityM(projectionMatrix, 0);
        android.opengl.Matrix.setIdentityM(viewProjectionMatrix, 0);
        android.opengl.Matrix.setIdentityM(inverseViewProjectionMatrix, 0);
    }

    // ================================
    // 1. Update Camera State
    // ================================

    /**
     * Update camera intrinsics and matrices
     */
    public void updateCameraState(Camera camera, int width, int height, int rotation) {
        // Update dimensions
        this.imageWidth = width;
        this.imageHeight = height;

        // Check for orientation change
        if (this.displayRotation != rotation) {
            this.displayRotation = rotation;
            this.orientationChanged = true;
            Log.d(TAG, "Orientation changed to: " + rotation);
        } else {
            this.orientationChanged = false;
        }

        // Get camera intrinsics
        float[] focalLength = camera.getImageIntrinsics().getFocalLength();
        float[] principalPoint = camera.getImageIntrinsics().getPrincipalPoint();

        cameraIntrinsics[0] = focalLength[0];    // fx
        cameraIntrinsics[1] = focalLength[1];    // fy
        cameraIntrinsics[2] = principalPoint[0]; // cx
        cameraIntrinsics[3] = principalPoint[1]; // cy

        // Get view and projection matrices
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

        // Calculate view-projection matrix
        android.opengl.Matrix.multiplyMM(viewProjectionMatrix, 0,
            projectionMatrix, 0, viewMatrix, 0);

        // Calculate inverse for unprojection
        android.opengl.Matrix.invertM(inverseViewProjectionMatrix, 0,
            viewProjectionMatrix, 0);

        Log.v(TAG, String.format("Camera state updated - Size: %dx%d, Rotation: %d",
            width, height, rotation));
    }

    // ================================
    // 2. 2D Image to 3D AR Space
    // ================================

    /**
     * Convert document corners from 2D image to 3D AR space
     */
    public float[][] convertImageTo3DSpace(Point[] imageCorners, Frame frame,
                                          Camera camera, float estimatedDepth) {
        float[][] corners3D = new float[imageCorners.length][3];

        for (int i = 0; i < imageCorners.length; i++) {
            corners3D[i] = convertImagePointTo3D(
                (float) imageCorners[i].x,
                (float) imageCorners[i].y,
                estimatedDepth,
                camera
            );
        }

        Log.d(TAG, String.format("Converted %d corners from 2D to 3D", imageCorners.length));

        return corners3D;
    }

    /**
     * Convert single 2D image point to 3D AR space
     */
    public float[] convertImagePointTo3D(float pixelX, float pixelY,
                                        float depth, Camera camera) {
        // Step 1: Pixel coordinates to normalized image coordinates
        float[] normalizedImage = pixelToNormalizedImage(pixelX, pixelY);

        // Step 2: Normalized image to camera space with depth
        float[] cameraSpace = normalizedImageToCameraSpace(
            normalizedImage[0], normalizedImage[1], depth);

        // Step 3: Camera space to world space
        float[] worldSpace = cameraSpaceToWorldSpace(cameraSpace, camera);

        return worldSpace;
    }

    /**
     * Convert pixel coordinates to normalized image coordinates
     */
    private float[] pixelToNormalizedImage(float pixelX, float pixelY) {
        // Get camera intrinsics
        float fx = cameraIntrinsics[0];
        float fy = cameraIntrinsics[1];
        float cx = cameraIntrinsics[2];
        float cy = cameraIntrinsics[3];

        // Normalize using camera intrinsics
        float normalizedX = (pixelX - cx) / fx;
        float normalizedY = (pixelY - cy) / fy;

        return new float[]{normalizedX, normalizedY};
    }

    /**
     * Convert normalized image coordinates to camera space with depth
     */
    private float[] normalizedImageToCameraSpace(float normX, float normY, float depth) {
        // In camera space: X right, Y down, Z forward (into scene)
        // Using pinhole camera model
        float x = normX * depth;
        float y = normY * depth;
        float z = -depth; // Negative because camera looks down -Z

        return new float[]{x, y, z};
    }

    /**
     * Convert camera space to world space
     */
    private float[] cameraSpaceToWorldSpace(float[] cameraPoint, Camera camera) {
        // Get camera pose (camera to world transformation)
        Pose cameraPose = camera.getPose();
        float[] poseMatrix = new float[16];
        cameraPose.toMatrix(poseMatrix, 0);

        // Transform point using pose matrix
        float[] worldPoint = transformPoint(poseMatrix, cameraPoint);

        return worldPoint;
    }

    // ================================
    // 3. Screen to World Coordinates
    // ================================

    /**
     * Map screen coordinates to world coordinates using ray casting
     */
    public float[] screenToWorldCoordinates(float screenX, float screenY,
                                           Frame frame, Camera camera) {
        // Convert screen to NDC
        float[] ndc = screenToNDC(screenX, screenY);

        // Perform ray cast
        List<HitResult> hitResults = frame.hitTest(screenX, screenY);

        if (!hitResults.isEmpty()) {
            // Use first hit result
            HitResult hit = hitResults.get(0);
            Pose hitPose = hit.getHitPose();

            float[] worldPoint = new float[3];
            worldPoint[0] = hitPose.tx();
            worldPoint[1] = hitPose.ty();
            worldPoint[2] = hitPose.tz();

            Log.d(TAG, String.format("Screen (%.1f, %.1f) -> World (%.3f, %.3f, %.3f)",
                screenX, screenY, worldPoint[0], worldPoint[1], worldPoint[2]));

            return worldPoint;
        }

        // No hit - use unprojection with estimated depth
        return unprojectScreenPoint(screenX, screenY, 1.0f, camera);
    }

    /**
     * Convert screen coordinates to NDC (Normalized Device Coordinates)
     */
    private float[] screenToNDC(float screenX, float screenY) {
        // Convert to NDC range [-1, 1]
        float ndcX = (2.0f * screenX / imageWidth) - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY / imageHeight); // Flip Y

        return new float[]{ndcX, ndcY};
    }

    /**
     * Unproject screen point to world space
     */
    private float[] unprojectScreenPoint(float screenX, float screenY,
                                        float depth, Camera camera) {
        // Convert to NDC
        float[] ndc = screenToNDC(screenX, screenY);

        // Create NDC point with depth
        float[] ndcPoint = {ndc[0], ndc[1], -1.0f, 1.0f}; // Near plane

        // Transform by inverse view-projection matrix
        float[] worldPoint = new float[4];
        android.opengl.Matrix.multiplyMV(worldPoint, 0,
            inverseViewProjectionMatrix, 0, ndcPoint, 0);

        // Perspective divide
        if (worldPoint[3] != 0) {
            worldPoint[0] /= worldPoint[3];
            worldPoint[1] /= worldPoint[3];
            worldPoint[2] /= worldPoint[3];
        }

        return new float[]{worldPoint[0], worldPoint[1], worldPoint[2]};
    }

    // ================================
    // 4. World to Screen Coordinates
    // ================================

    /**
     * Project world coordinates to screen coordinates
     */
    public float[] worldToScreenCoordinates(float[] worldPoint, Camera camera) {
        // Transform world point to clip space
        float[] clipPoint = new float[4];
        float[] worldPoint4 = {worldPoint[0], worldPoint[1], worldPoint[2], 1.0f};

        android.opengl.Matrix.multiplyMV(clipPoint, 0,
            viewProjectionMatrix, 0, worldPoint4, 0);

        // Perspective divide
        if (clipPoint[3] != 0) {
            clipPoint[0] /= clipPoint[3];
            clipPoint[1] /= clipPoint[3];
            clipPoint[2] /= clipPoint[3];
        }

        // NDC to screen coordinates
        float screenX = (clipPoint[0] + 1.0f) * imageWidth / 2.0f;
        float screenY = (1.0f - clipPoint[1]) * imageHeight / 2.0f; // Flip Y

        return new float[]{screenX, screenY};
    }

    // ================================
    // 5. Overlay Positioning
    // ================================

    /**
     * Calculate proper overlay positioning for document
     */
    public OverlayPosition calculateOverlayPosition(float[][] corners3D,
                                                   Plane plane, Camera camera) {
        OverlayPosition position = new OverlayPosition();

        // Calculate center point
        position.center = calculateCenter(corners3D);

        // Get plane pose for proper alignment
        if (plane != null) {
            Pose planePose = plane.getCenterPose();
            position.pose = planePose;

            // Calculate local coordinate system
            position.rightVector = planePose.getXAxis();
            position.upVector = planePose.getYAxis();
            position.normalVector = planePose.getZAxis();
        } else {
            // Use default coordinate system
            position.rightVector = new float[]{1, 0, 0};
            position.upVector = new float[]{0, 1, 0};
            position.normalVector = new float[]{0, 0, 1};
        }

        // Calculate dimensions
        position.width = calculateDistance(corners3D[0], corners3D[1]);
        position.height = calculateDistance(corners3D[1], corners3D[2]);

        // Calculate rotation to face camera
        position.rotationToCamera = calculateRotationToCamera(
            position.normalVector, camera);

        Log.d(TAG, String.format("Overlay position - Center: (%.3f, %.3f, %.3f), " +
            "Size: %.3f x %.3f, Rotation: %.1f°",
            position.center[0], position.center[1], position.center[2],
            position.width, position.height,
            Math.toDegrees(position.rotationToCamera)));

        return position;
    }

    /**
     * Calculate center point of corners
     */
    private float[] calculateCenter(float[][] corners) {
        float x = 0, y = 0, z = 0;

        for (float[] corner : corners) {
            x += corner[0];
            y += corner[1];
            z += corner[2];
        }

        int count = corners.length;
        return new float[]{x / count, y / count, z / count};
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
     * Calculate rotation to face camera
     */
    private float calculateRotationToCamera(float[] normal, Camera camera) {
        // Get camera forward direction
        float[] cameraForward = camera.getPose().getZAxis();

        // Calculate angle between normal and camera forward
        float dot = dotProduct(normal, cameraForward);
        float angle = (float) Math.acos(Math.max(-1.0f, Math.min(1.0f, dot)));

        return angle;
    }

    // ================================
    // 6. Device Orientation Handling
    // ================================

    /**
     * Handle device orientation changes
     */
    public float[] adjustForOrientation(float[] point) {
        if (!orientationChanged) {
            return point;
        }

        // Rotate point based on display rotation
        float[] rotated = new float[3];

        switch (displayRotation) {
            case android.view.Surface.ROTATION_0:
                // Portrait - no change
                rotated[0] = point[0];
                rotated[1] = point[1];
                rotated[2] = point[2];
                break;

            case android.view.Surface.ROTATION_90:
                // Landscape left
                rotated[0] = -point[1];
                rotated[1] = point[0];
                rotated[2] = point[2];
                break;

            case android.view.Surface.ROTATION_180:
                // Portrait upside down
                rotated[0] = -point[0];
                rotated[1] = -point[1];
                rotated[2] = point[2];
                break;

            case android.view.Surface.ROTATION_270:
                // Landscape right
                rotated[0] = point[1];
                rotated[1] = -point[0];
                rotated[2] = point[2];
                break;

            default:
                rotated = point;
        }

        return rotated;
    }

    /**
     * Get rotation matrix for display rotation
     */
    public float[] getOrientationRotationMatrix() {
        float[] matrix = new float[16];
        android.opengl.Matrix.setIdentityM(matrix, 0);

        float angle = 0;
        switch (displayRotation) {
            case android.view.Surface.ROTATION_90:
                angle = 90;
                break;
            case android.view.Surface.ROTATION_180:
                angle = 180;
                break;
            case android.view.Surface.ROTATION_270:
                angle = 270;
                break;
        }

        if (angle != 0) {
            android.opengl.Matrix.rotateM(matrix, 0, angle, 0, 0, 1);
        }

        return matrix;
    }

    // ================================
    // 7. Perspective Correction
    // ================================

    /**
     * Apply perspective correction to document corners
     */
    public float[][] correctPerspective(float[][] corners3D, Camera camera) {
        float[][] corrected = new float[corners3D.length][3];

        // Get camera position
        Pose cameraPose = camera.getPose();
        float[] cameraPos = {cameraPose.tx(), cameraPose.ty(), cameraPose.tz()};

        // Calculate perspective-corrected positions
        for (int i = 0; i < corners3D.length; i++) {
            corrected[i] = correctPointPerspective(corners3D[i], cameraPos);
        }

        return corrected;
    }

    /**
     * Correct single point for perspective
     */
    private float[] correctPointPerspective(float[] point, float[] cameraPos) {
        // Calculate distance from camera
        float distance = calculateDistance(point, cameraPos);

        // Apply perspective correction factor
        float correctionFactor = 1.0f + (distance * 0.05f);

        float[] corrected = new float[3];
        corrected[0] = point[0] * correctionFactor;
        corrected[1] = point[1] * correctionFactor;
        corrected[2] = point[2] * correctionFactor;

        return corrected;
    }

    /**
     * Calculate homography matrix for perspective transform
     */
    public float[] calculateHomography(Point[] srcPoints, Point[] dstPoints) {
        // This is a simplified version
        // Full implementation would solve for homography matrix

        float[] homography = new float[9];
        android.opengl.Matrix.setIdentityM(homography, 0);

        // For production, use OpenCV's findHomography
        // org.opencv.calib3d.Calib3d.findHomography()

        return homography;
    }

    // ================================
    // 8. Tracking Stability
    // ================================

    /**
     * Check if tracking is stable for current frame
     */
    public boolean isTrackingStable(Camera camera, Plane plane) {
        // Check camera tracking state
        if (camera.getTrackingState() != com.google.ar.core.TrackingState.TRACKING) {
            return false;
        }

        // Check plane tracking state
        if (plane != null &&
            plane.getTrackingState() != com.google.ar.core.TrackingState.TRACKING) {
            return false;
        }

        return true;
    }

    /**
     * Apply smoothing to reduce jitter
     */
    public float[] smoothPosition(float[] currentPos, float[] previousPos,
                                  float smoothingFactor) {
        if (previousPos == null) {
            return currentPos;
        }

        float[] smoothed = new float[3];
        for (int i = 0; i < 3; i++) {
            smoothed[i] = previousPos[i] * (1 - smoothingFactor) +
                         currentPos[i] * smoothingFactor;
        }

        return smoothed;
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Transform point by matrix
     */
    private float[] transformPoint(float[] matrix, float[] point) {
        float[] point4 = {point[0], point[1], point[2], 1.0f};
        float[] result = new float[4];

        android.opengl.Matrix.multiplyMV(result, 0, matrix, 0, point4, 0);

        // Perspective divide if needed
        if (result[3] != 0 && result[3] != 1) {
            result[0] /= result[3];
            result[1] /= result[3];
            result[2] /= result[3];
        }

        return new float[]{result[0], result[1], result[2]};
    }

    /**
     * Calculate dot product
     */
    private float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * Calculate cross product
     */
    private float[] crossProduct(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    /**
     * Normalize vector
     */
    private float[] normalize(float[] vector) {
        float length = (float) Math.sqrt(
            vector[0] * vector[0] +
            vector[1] * vector[1] +
            vector[2] * vector[2]
        );

        if (length == 0) {
            return new float[]{0, 0, 0};
        }

        return new float[]{
            vector[0] / length,
            vector[1] / length,
            vector[2] / length
        };
    }

    /**
     * Calculate angle between two vectors
     */
    public float angleBetween(float[] a, float[] b) {
        float dot = dotProduct(normalize(a), normalize(b));
        return (float) Math.acos(Math.max(-1.0f, Math.min(1.0f, dot)));
    }

    // ================================
    // Getters
    // ================================

    public float[] getCameraIntrinsics() {
        return cameraIntrinsics.clone();
    }

    public float[] getViewMatrix() {
        return viewMatrix.clone();
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix.clone();
    }

    public float[] getViewProjectionMatrix() {
        return viewProjectionMatrix.clone();
    }

    public int getDisplayRotation() {
        return displayRotation;
    }

    public boolean hasOrientationChanged() {
        return orientationChanged;
    }

    // ================================
    // Inner Classes
    // ================================

    /**
     * Overlay position data
     */
    public static class OverlayPosition {
        public float[] center;           // Center point in world space
        public Pose pose;                // Plane pose
        public float[] rightVector;      // Local X axis
        public float[] upVector;         // Local Y axis
        public float[] normalVector;     // Local Z axis (normal)
        public float width;              // Document width in meters
        public float height;             // Document height in meters
        public float rotationToCamera;   // Rotation angle to face camera

        @Override
        public String toString() {
            return String.format("OverlayPosition[center=(%.3f,%.3f,%.3f), size=%.3fx%.3f, rot=%.1f°]",
                center[0], center[1], center[2], width, height,
                Math.toDegrees(rotationToCamera));
        }
    }

    /**
     * Coordinate transformation result
     */
    public static class TransformationResult {
        public float[] point;            // Transformed point
        public CoordinateSystem fromSystem;
        public CoordinateSystem toSystem;
        public boolean success;
        public String error;

        public TransformationResult(float[] point, CoordinateSystem from,
                                   CoordinateSystem to) {
            this.point = point;
            this.fromSystem = from;
            this.toSystem = to;
            this.success = true;
        }

        public TransformationResult(String error, CoordinateSystem from,
                                   CoordinateSystem to) {
            this.error = error;
            this.fromSystem = from;
            this.toSystem = to;
            this.success = false;
        }
    }
}


