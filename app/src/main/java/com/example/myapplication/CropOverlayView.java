package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * CropOverlayView - Custom view for displaying image with draggable crop corners
 * Allows user to adjust document crop area by dragging corner handles
 */
public class CropOverlayView extends View {

    private static final String TAG = "CropOverlayView";
    private static final float CORNER_RADIUS = 20f;
    private static final float TOUCH_TOLERANCE = 50f;

    // Paints
    private Paint imagePaint;
    private Paint overlayPaint;
    private Paint linePaint;
    private Paint cornerPaint;
    private Paint cornerBorderPaint;

    // Image
    private Bitmap imageBitmap;
    private RectF imageRect;
    private float imageScale = 1f;
    private float imageOffsetX = 0f;
    private float imageOffsetY = 0f;

    // Crop corners (top-left, top-right, bottom-right, bottom-left)
    private PointF[] corners = new PointF[4];
    private int selectedCorner = -1;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialize paints and corners
     */
    private void init() {
        // Image paint
        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imagePaint.setFilterBitmap(true);

        // Overlay paint (semi-transparent dark)
        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#80000000"));
        overlayPaint.setStyle(Paint.Style.FILL);

        // Line paint (crop border)
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00FF00")); // Green
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        // Corner paint (fill)
        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(Color.parseColor("#00FF00")); // Green
        cornerPaint.setStyle(Paint.Style.FILL);

        // Corner border paint
        cornerBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerBorderPaint.setColor(Color.WHITE);
        cornerBorderPaint.setStrokeWidth(3f);
        cornerBorderPaint.setStyle(Paint.Style.STROKE);

        // Initialize corners
        for (int i = 0; i < 4; i++) {
            corners[i] = new PointF();
        }
    }

    /**
     * Set image bitmap and initialize corners
     */
    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;
        calculateImageBounds();
        initializeCorners();
        invalidate();
    }

    /**
     * Calculate image bounds to fit view
     */
    private void calculateImageBounds() {
        if (imageBitmap == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int bitmapWidth = imageBitmap.getWidth();
        int bitmapHeight = imageBitmap.getHeight();

        // Calculate scale to fit
        float scaleX = (float) viewWidth / bitmapWidth;
        float scaleY = (float) viewHeight / bitmapHeight;
        imageScale = Math.min(scaleX, scaleY);

        // Calculate scaled dimensions
        float scaledWidth = bitmapWidth * imageScale;
        float scaledHeight = bitmapHeight * imageScale;

        // Center image
        imageOffsetX = (viewWidth - scaledWidth) / 2f;
        imageOffsetY = (viewHeight - scaledHeight) / 2f;

        // Create image rect
        imageRect = new RectF(
            imageOffsetX,
            imageOffsetY,
            imageOffsetX + scaledWidth,
            imageOffsetY + scaledHeight
        );
    }

    /**
     * Initialize corner positions to image bounds with margin
     */
    private void initializeCorners() {
        if (imageRect == null) {
            return;
        }

        float margin = 40f;

        // Top-left
        corners[0].set(imageRect.left + margin, imageRect.top + margin);

        // Top-right
        corners[1].set(imageRect.right - margin, imageRect.top + margin);

        // Bottom-right
        corners[2].set(imageRect.right - margin, imageRect.bottom - margin);

        // Bottom-left
        corners[3].set(imageRect.left + margin, imageRect.bottom - margin);
    }

    /**
     * Reset corners to initial position
     */
    public void resetCorners() {
        initializeCorners();
        invalidate();
    }

    /**
     * Get corner points in bitmap coordinates
     */
    public PointF[] getCornerPoints() {
        PointF[] bitmapCorners = new PointF[4];

        for (int i = 0; i < 4; i++) {
            // Convert screen coordinates to bitmap coordinates
            float bitmapX = (corners[i].x - imageOffsetX) / imageScale;
            float bitmapY = (corners[i].y - imageOffsetY) / imageScale;

            // Clamp to bitmap bounds
            bitmapX = Math.max(0, Math.min(imageBitmap.getWidth(), bitmapX));
            bitmapY = Math.max(0, Math.min(imageBitmap.getHeight(), bitmapY));

            bitmapCorners[i] = new PointF(bitmapX, bitmapY);
        }

        return bitmapCorners;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (imageBitmap != null) {
            calculateImageBounds();
            initializeCorners();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageBitmap == null || imageRect == null) {
            return;
        }

        // Draw image
        canvas.drawBitmap(imageBitmap, null, imageRect, imagePaint);

        // Draw dark overlay outside crop area
        drawOverlay(canvas);

        // Draw crop area border
        drawCropBorder(canvas);

        // Draw corner handles
        drawCorners(canvas);
    }

    /**
     * Draw dark overlay outside crop area
     */
    private void drawOverlay(Canvas canvas) {
        // Create path for crop area
        Path cropPath = new Path();
        cropPath.moveTo(corners[0].x, corners[0].y);
        cropPath.lineTo(corners[1].x, corners[1].y);
        cropPath.lineTo(corners[2].x, corners[2].y);
        cropPath.lineTo(corners[3].x, corners[3].y);
        cropPath.close();

        // Save canvas state
        canvas.save();

        // Clip to inverse of crop area
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.clipPath(cropPath, android.graphics.Region.Op.DIFFERENCE);

        // Draw overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Restore canvas
        canvas.restore();
    }

    /**
     * Draw crop area border lines
     */
    private void drawCropBorder(Canvas canvas) {
        // Draw lines connecting corners
        for (int i = 0; i < 4; i++) {
            PointF p1 = corners[i];
            PointF p2 = corners[(i + 1) % 4];
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint);
        }
    }

    /**
     * Draw corner handles
     */
    private void drawCorners(Canvas canvas) {
        for (int i = 0; i < 4; i++) {
            PointF corner = corners[i];

            // Draw outer circle (border)
            canvas.drawCircle(corner.x, corner.y, CORNER_RADIUS + 3, cornerBorderPaint);

            // Draw inner circle (fill)
            canvas.drawCircle(corner.x, corner.y, CORNER_RADIUS, cornerPaint);

            // Highlight selected corner
            if (i == selectedCorner) {
                Paint highlightPaint = new Paint(cornerPaint);
                highlightPaint.setAlpha(128);
                canvas.drawCircle(corner.x, corner.y, CORNER_RADIUS * 1.5f, highlightPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Find nearest corner
                selectedCorner = findNearestCorner(x, y);
                if (selectedCorner != -1) {
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedCorner != -1) {
                    // Move selected corner
                    corners[selectedCorner].set(x, y);

                    // Constrain to image bounds
                    constrainCornerToBounds(selectedCorner);

                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                selectedCorner = -1;
                invalidate();
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Find nearest corner to touch point
     */
    private int findNearestCorner(float x, float y) {
        int nearest = -1;
        float minDistance = TOUCH_TOLERANCE;

        for (int i = 0; i < 4; i++) {
            float dx = x - corners[i].x;
            float dy = y - corners[i].y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * Constrain corner to stay within image bounds
     */
    private void constrainCornerToBounds(int cornerIndex) {
        if (imageRect == null) {
            return;
        }

        PointF corner = corners[cornerIndex];

        // Clamp to image bounds
        corner.x = Math.max(imageRect.left, Math.min(imageRect.right, corner.x));
        corner.y = Math.max(imageRect.top, Math.min(imageRect.bottom, corner.y));
    }
}

