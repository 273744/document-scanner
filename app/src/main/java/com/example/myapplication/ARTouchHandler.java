package com.example.myapplication;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;

import java.util.List;

/**
 * ARTouchHandler - Complete AR interaction management
 *
 * Features:
 * - Handle touch events on AR document overlays
 * - Select specific documents from multiple detections
 * - Manual corner adjustment with touch gestures
 * - Pinch-to-zoom for detailed inspection
 * - Double-tap to focus camera
 * - Long-press for options menu
 * - Gesture recognition for AR interactions
 * - Accurate 2D touch to 3D AR space conversion
 */
public class ARTouchHandler implements View.OnTouchListener {

    private static final String TAG = "ARTouchHandler";

    // Touch thresholds
    private static final float TOUCH_SLOP = 20f; // Pixels
    private static final float CORNER_TOUCH_RADIUS = 60f; // Pixels
    private static final float DOCUMENT_TOUCH_THRESHOLD = 100f; // Pixels
    private static final long LONG_PRESS_TIMEOUT = 500; // Milliseconds
    private static final int DOUBLE_TAP_TIMEOUT = 300; // Milliseconds

    // Context and components
    private Context context;
    private Frame currentFrame;
    private Camera currentCamera;
    private ARCoordinateMapper coordinateMapper;
    private List<ARDocumentDetector.DetectedDocument> detectedDocuments;

    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // Touch state
    private PointF touchDownPoint = new PointF();
    private PointF lastTouchPoint = new PointF();
    private long touchDownTime = 0;
    private boolean isDragging = false;
    private boolean isPinching = false;
    private int selectedCornerIndex = -1;
    private ARDocumentDetector.DetectedDocument selectedDocument = null;
    private ARDocumentDetector.DetectedDocument draggedDocument = null;

    // Double tap detection
    private long lastTapTime = 0;
    private int tapCount = 0;

    // Long press detection
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;

    // Zoom state
    private float currentZoomLevel = 1.0f;
    private float minZoom = 0.5f;
    private float maxZoom = 3.0f;

    // Callbacks
    private TouchEventCallback callback;

    /**
     * Constructor
     */
    public ARTouchHandler(Context context) {
        this.context = context;
        this.coordinateMapper = new ARCoordinateMapper();

        // Setup gesture detectors
        setupGestureDetectors();
    }

    /**
     * Setup gesture detectors
     */
    private void setupGestureDetectors() {
        // Standard gesture detector
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Scale gesture detector for pinch-to-zoom
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    // ================================
    // 1. Main Touch Event Handling
    // ================================

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Update current touch point
        lastTouchPoint.set(event.getX(), event.getY());

        // Handle gesture detectors first
        boolean handled = false;

        if (scaleGestureDetector != null) {
            handled = scaleGestureDetector.onTouchEvent(event);
        }

        if (gestureDetector != null) {
            handled |= gestureDetector.onTouchEvent(event);
        }

        // Handle custom touch events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handled |= handleTouchDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                handled |= handleTouchMove(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handled |= handleTouchUp(event);
                break;
        }

        return handled || true; // Always consume touch events
    }

    /**
     * Handle touch down
     */
    private boolean handleTouchDown(MotionEvent event) {
        touchDownPoint.set(event.getX(), event.getY());
        touchDownTime = System.currentTimeMillis();
        isDragging = false;

        // Check for corner touch
        selectedCornerIndex = findTouchedCorner(event.getX(), event.getY());

        if (selectedCornerIndex != -1 && selectedDocument != null) {
            // Touching a corner - prepare for adjustment
            if (callback != null) {
                callback.onCornerTouchStart(selectedDocument, selectedCornerIndex);
            }
            return true;
        }

        // Check for document touch
        ARDocumentDetector.DetectedDocument touchedDoc = findTouchedDocument(
            event.getX(), event.getY());

        if (touchedDoc != null) {
            draggedDocument = touchedDoc;

            if (callback != null) {
                callback.onDocumentTouchStart(touchedDoc);
            }

            // Setup long press detection
            setupLongPress(touchedDoc);

            return true;
        }

        return false;
    }

    /**
     * Handle touch move
     */
    private boolean handleTouchMove(MotionEvent event) {
        if (isPinching) {
            return true; // Pinch is handled by ScaleGestureDetector
        }

        float dx = event.getX() - touchDownPoint.x;
        float dy = event.getY() - touchDownPoint.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > TOUCH_SLOP) {
            isDragging = true;
            cancelLongPress();
        }

        if (isDragging) {
            if (selectedCornerIndex != -1 && selectedDocument != null) {
                // Dragging corner for adjustment
                handleCornerDrag(event.getX(), event.getY());
                return true;
            }
        }

        return false;
    }

    /**
     * Handle touch up
     */
    private boolean handleTouchUp(MotionEvent event) {
        cancelLongPress();

        if (selectedCornerIndex != -1 && selectedDocument != null) {
            // Finished corner adjustment
            if (callback != null) {
                callback.onCornerTouchEnd(selectedDocument, selectedCornerIndex);
            }
            selectedCornerIndex = -1;
            return true;
        }

        if (!isDragging && draggedDocument != null) {
            // Single tap on document (not dragged)
            handleDocumentTap(draggedDocument);
        }

        // Reset state
        isDragging = false;
        draggedDocument = null;

        return false;
    }

    // ================================
    // 2. Document Selection
    // ================================

    /**
     * Find touched document from multiple detections
     */
    private ARDocumentDetector.DetectedDocument findTouchedDocument(float x, float y) {
        if (detectedDocuments == null || detectedDocuments.isEmpty()) {
            return null;
        }

        // Check each detected document
        for (ARDocumentDetector.DetectedDocument doc : detectedDocuments) {
            if (isPointInDocument(x, y, doc)) {
                return doc;
            }
        }

        return null;
    }

    /**
     * Check if point is inside document boundary
     */
    private boolean isPointInDocument(float x, float y,
                                      ARDocumentDetector.DetectedDocument doc) {
        if (doc.corners2D == null || doc.corners2D.length != 4) {
            return false;
        }

        // Use ray casting algorithm
        int intersections = 0;

        for (int i = 0; i < 4; i++) {
            org.opencv.core.Point p1 = doc.corners2D[i];
            org.opencv.core.Point p2 = doc.corners2D[(i + 1) % 4];

            if (rayIntersectsSegment(x, y, p1, p2)) {
                intersections++;
            }
        }

        return (intersections % 2) == 1; // Odd number = inside
    }

    /**
     * Ray casting helper
     */
    private boolean rayIntersectsSegment(float px, float py,
                                        org.opencv.core.Point p1,
                                        org.opencv.core.Point p2) {
        if (p1.y > p2.y) {
            org.opencv.core.Point temp = p1;
            p1 = p2;
            p2 = temp;
        }

        if (py < p1.y || py > p2.y) {
            return false;
        }

        if (p1.y == p2.y) {
            return false;
        }

        double xIntersection = (py - p1.y) * (p2.x - p1.x) / (p2.y - p1.y) + p1.x;
        return px < xIntersection;
    }

    /**
     * Handle document tap (selection)
     */
    private void handleDocumentTap(ARDocumentDetector.DetectedDocument doc) {
        // Check for double tap
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            tapCount++;

            if (tapCount == 2) {
                // Double tap detected
                handleDoubleTap(doc);
                tapCount = 0;
                return;
            }
        } else {
            tapCount = 1;
        }

        lastTapTime = currentTime;

        // Single tap - select document
        selectDocument(doc);
    }

    /**
     * Select document
     */
    private void selectDocument(ARDocumentDetector.DetectedDocument doc) {
        selectedDocument = doc;

        if (callback != null) {
            callback.onDocumentSelected(doc);
        }

        Log.d(TAG, "Document selected: " + doc.trackingId);
    }

    // ================================
    // 3. Manual Corner Adjustment
    // ================================

    /**
     * Find touched corner
     */
    private int findTouchedCorner(float x, float y) {
        if (selectedDocument == null || selectedDocument.corners2D == null) {
            return -1;
        }

        // Check each corner
        for (int i = 0; i < selectedDocument.corners2D.length; i++) {
            org.opencv.core.Point corner = selectedDocument.corners2D[i];
            float dx = (float) (x - corner.x);
            float dy = (float) (y - corner.y);
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < CORNER_TOUCH_RADIUS) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Handle corner drag for manual adjustment
     */
    private void handleCornerDrag(float newX, float newY) {
        if (selectedDocument == null || selectedCornerIndex == -1) {
            return;
        }

        // Update corner position
        selectedDocument.corners2D[selectedCornerIndex].x = newX;
        selectedDocument.corners2D[selectedCornerIndex].y = newY;

        // Convert to 3D if possible
        if (currentFrame != null && currentCamera != null) {
            float[] point3D = coordinateMapper.convertImagePointTo3D(
                newX, newY, 0.5f, currentCamera);

            if (selectedDocument.corners3D != null &&
                selectedCornerIndex < selectedDocument.corners3D.length) {
                selectedDocument.corners3D[selectedCornerIndex] = point3D;
            }
        }

        // Notify callback
        if (callback != null) {
            callback.onCornerAdjusted(selectedDocument, selectedCornerIndex,
                new PointF(newX, newY));
        }

        Log.v(TAG, String.format("Corner %d adjusted to (%.1f, %.1f)",
            selectedCornerIndex, newX, newY));
    }

    // ================================
    // 4. Pinch-to-Zoom
    // ================================

    /**
     * Scale gesture listener for pinch-to-zoom
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isPinching = true;

            if (callback != null) {
                callback.onZoomStart(currentZoomLevel);
            }

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newZoom = currentZoomLevel * scaleFactor;

            // Clamp zoom level
            newZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));

            if (newZoom != currentZoomLevel) {
                currentZoomLevel = newZoom;

                if (callback != null) {
                    callback.onZoomChanged(currentZoomLevel, scaleFactor);
                }

                Log.d(TAG, "Zoom level: " + currentZoomLevel);
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isPinching = false;

            if (callback != null) {
                callback.onZoomEnd(currentZoomLevel);
            }
        }
    }

    // ================================
    // 5. Double-Tap to Focus
    // ================================

    /**
     * Handle double tap on document
     */
    private void handleDoubleTap(ARDocumentDetector.DetectedDocument doc) {
        if (callback != null) {
            callback.onDocumentDoubleTap(doc);
        }

        // Focus camera on document
        focusCameraOnDocument(doc);

        Log.d(TAG, "Double tap on document: " + doc.trackingId);
    }

    /**
     * Focus camera on document
     */
    private void focusCameraOnDocument(ARDocumentDetector.DetectedDocument doc) {
        if (currentFrame == null || doc.center3D == null) {
            return;
        }

        // Perform AR hit test at document center
        float[] screenCenter = coordinateMapper.worldToScreenCoordinates(
            doc.center3D, currentCamera);

        List<HitResult> hits = currentFrame.hitTest(screenCenter[0], screenCenter[1]);

        if (!hits.isEmpty()) {
            HitResult hit = hits.get(0);

            if (callback != null) {
                callback.onFocusRequested(hit, doc);
            }
        }
    }

    // ================================
    // 6. Long-Press Options Menu
    // ================================

    /**
     * Setup long press detection
     */
    private void setupLongPress(final ARDocumentDetector.DetectedDocument doc) {
        cancelLongPress(); // Cancel any existing

        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDragging) {
                    handleLongPress(doc);
                }
            }
        };

        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
    }

    /**
     * Cancel long press detection
     */
    private void cancelLongPress() {
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    /**
     * Handle long press on document
     */
    private void handleLongPress(ARDocumentDetector.DetectedDocument doc) {
        if (callback != null) {
            callback.onDocumentLongPress(doc, lastTouchPoint);
        }

        // Haptic feedback
        // view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        Log.d(TAG, "Long press on document: " + doc.trackingId);
    }

    // ================================
    // 7. Gesture Recognition
    // ================================

    /**
     * Custom gesture listener
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // Handled in handleTouchUp
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Handled in handleDocumentTap
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Handled via custom long press detection
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                              float velocityX, float velocityY) {
            // Detect swipe gestures
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX > 0) {
                        handleSwipeRight();
                    } else {
                        handleSwipeLeft();
                    }
                    return true;
                }
            } else {
                if (Math.abs(diffY) > 100 && Math.abs(velocityY) > 100) {
                    if (diffY > 0) {
                        handleSwipeDown();
                    } else {
                        handleSwipeUp();
                    }
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Handle swipe gestures
     */
    private void handleSwipeLeft() {
        if (callback != null) {
            callback.onSwipeGesture(SwipeDirection.LEFT);
        }
        Log.d(TAG, "Swipe left");
    }

    private void handleSwipeRight() {
        if (callback != null) {
            callback.onSwipeGesture(SwipeDirection.RIGHT);
        }
        Log.d(TAG, "Swipe right");
    }

    private void handleSwipeUp() {
        if (callback != null) {
            callback.onSwipeGesture(SwipeDirection.UP);
        }
        Log.d(TAG, "Swipe up");
    }

    private void handleSwipeDown() {
        if (callback != null) {
            callback.onSwipeGesture(SwipeDirection.DOWN);
        }
        Log.d(TAG, "Swipe down");
    }

    // ================================
    // 8. Coordinate Conversion
    // ================================

    /**
     * Convert 2D touch to 3D AR space
     */
    public float[] touch2DTo3D(float screenX, float screenY) {
        if (currentFrame == null || currentCamera == null) {
            return null;
        }

        return coordinateMapper.screenToWorldCoordinates(
            screenX, screenY, currentFrame, currentCamera);
    }

    /**
     * Convert 3D AR space to 2D touch
     */
    public float[] touch3DTo2D(float[] worldPoint) {
        if (currentCamera == null) {
            return null;
        }

        return coordinateMapper.worldToScreenCoordinates(worldPoint, currentCamera);
    }

    // ================================
    // State Management
    // ================================

    /**
     * Update AR frame and camera
     */
    public void updateFrame(Frame frame, Camera camera) {
        this.currentFrame = frame;
        this.currentCamera = camera;

        if (coordinateMapper != null) {
            coordinateMapper.updateCameraState(camera, 1080, 1920, 0); // TODO: Get actual dimensions
        }
    }

    /**
     * Update detected documents
     */
    public void updateDetectedDocuments(List<ARDocumentDetector.DetectedDocument> documents) {
        this.detectedDocuments = documents;
    }

    /**
     * Get selected document
     */
    public ARDocumentDetector.DetectedDocument getSelectedDocument() {
        return selectedDocument;
    }

    /**
     * Clear selection
     */
    public void clearSelection() {
        selectedDocument = null;
        selectedCornerIndex = -1;

        if (callback != null) {
            callback.onSelectionCleared();
        }
    }

    /**
     * Reset zoom
     */
    public void resetZoom() {
        currentZoomLevel = 1.0f;
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(TouchEventCallback callback) {
        this.callback = callback;
    }

    /**
     * Touch event callback interface
     */
    public interface TouchEventCallback {
        void onDocumentTouchStart(ARDocumentDetector.DetectedDocument doc);
        void onDocumentSelected(ARDocumentDetector.DetectedDocument doc);
        void onDocumentDoubleTap(ARDocumentDetector.DetectedDocument doc);
        void onDocumentLongPress(ARDocumentDetector.DetectedDocument doc, PointF point);

        void onCornerTouchStart(ARDocumentDetector.DetectedDocument doc, int cornerIndex);
        void onCornerAdjusted(ARDocumentDetector.DetectedDocument doc, int cornerIndex, PointF newPosition);
        void onCornerTouchEnd(ARDocumentDetector.DetectedDocument doc, int cornerIndex);

        void onZoomStart(float currentZoom);
        void onZoomChanged(float newZoom, float scaleFactor);
        void onZoomEnd(float finalZoom);

        void onFocusRequested(HitResult hitResult, ARDocumentDetector.DetectedDocument doc);

        void onSwipeGesture(SwipeDirection direction);

        void onSelectionCleared();
    }

    /**
     * Swipe direction enum
     */
    public enum SwipeDirection {
        LEFT, RIGHT, UP, DOWN
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        cancelLongPress();
        longPressHandler.removeCallbacksAndMessages(null);
        callback = null;
        detectedDocuments = null;
        selectedDocument = null;
    }
}

