package com.srikanth.docscanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * AROverlayView - Custom view for AR document boundary visualization
 *
 * Features:
 * - Animated document edge outlines
 * - Corner handles for precise positioning
 * - Alignment grid lines and guides
 * - Distance and angle indicators
 * - Quality-based color animations (red→yellow→green)
 * - 3D preview box showing final scan area
 * - Quality score with smooth number animations
 * - High-performance Canvas drawing
 */
public class AROverlayView extends View {

    private static final String TAG = "AROverlayView";

    // Paint objects
    private Paint boundaryPaint;
    private Paint cornerPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint shadowPaint;
    private Paint fillPaint;
    private Paint scorePaint;
    private Paint previewPaint;

    // Animation
    private ValueAnimator colorAnimator;
    private ValueAnimator pulseAnimator;
    private ValueAnimator scoreAnimator;
    private float animationProgress = 0f;
    private float pulseScale = 1.0f;
    private float currentScore = 0f;
    private float targetScore = 0f;

    // Document data
    private PointF[] documentCorners = null;
    private int qualityScore = 0;
    private boolean showGrid = true;
    private boolean showCorners = true;
    private boolean showDistances = true;
    private boolean showAngles = true;
    private boolean show3DPreview = true;
    private boolean showQualityScore = true;

    // Colors
    private int currentBoundaryColor = Color.RED;
    private int targetBoundaryColor = Color.RED;

    // Quality color thresholds
    private static final int COLOR_POOR = Color.rgb(220, 53, 69);      // Red
    private static final int COLOR_FAIR = Color.rgb(255, 193, 7);      // Yellow
    private static final int COLOR_GOOD = Color.rgb(40, 167, 69);      // Green
    private static final int COLOR_EXCELLENT = Color.rgb(0, 123, 255); // Blue

    // Dimensions
    private static final float CORNER_SIZE = 40f;
    private static final float CORNER_INNER_SIZE = 20f;
    private static final float BOUNDARY_WIDTH = 4f;
    private static final float GRID_LINE_WIDTH = 1f;
    private static final float SHADOW_RADIUS = 8f;

    // Animation constants
    private static final long COLOR_ANIMATION_DURATION = 300;
    private static final long PULSE_DURATION = 1000;
    private static final long SCORE_ANIMATION_DURATION = 500;

    // 3D preview
    private float previewDepth = 30f;
    private float previewAngle = 30f;

    /**
     * Constructor
     */
    public AROverlayView(Context context) {
        super(context);
        init();
    }

    public AROverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AROverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialize paints and animators
     */
    private void init() {
        // Boundary paint
        boundaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(BOUNDARY_WIDTH);
        boundaryPaint.setColor(currentBoundaryColor);
        boundaryPaint.setStrokeCap(Paint.Cap.ROUND);
        boundaryPaint.setStrokeJoin(Paint.Join.ROUND);

        // Corner paint
        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setStyle(Paint.Style.FILL);
        cornerPaint.setColor(Color.WHITE);

        // Grid paint
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(GRID_LINE_WIDTH);
        gridPaint.setColor(Color.WHITE);
        gridPaint.setAlpha(100);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        // Text paint
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

        // Shadow paint
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(50);
        shadowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(
            SHADOW_RADIUS, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Fill paint (semi-transparent)
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(30);

        // Score paint
        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(72f);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setFakeBoldText(true);
        scorePaint.setShadowLayer(8f, 0f, 4f, Color.BLACK);

        // Preview paint (for 3D box)
        previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeWidth(2f);
        previewPaint.setColor(Color.WHITE);
        previewPaint.setAlpha(150);

        // Setup animators
        setupAnimators();

        // Enable hardware acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    /**
     * Setup animators
     */
    private void setupAnimators() {
        // Pulse animator for selected document
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.1f, 1.0f);
        pulseAnimator.setDuration(PULSE_DURATION);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseScale = (float) animation.getAnimatedValue();
            invalidate();
        });

        // Score animator
        scoreAnimator = ValueAnimator.ofFloat(0f, 1f);
        scoreAnimator.setDuration(SCORE_ANIMATION_DURATION);
        scoreAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scoreAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            currentScore = currentScore + (targetScore - currentScore) * progress;
            invalidate();
        });
    }

    // ================================
    // Drawing Methods
    // ================================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (documentCorners == null || documentCorners.length != 4) {
            return;
        }

        // Save canvas state
        canvas.save();

        // Apply pulse scale if animating
        if (pulseAnimator.isRunning()) {
            float centerX = (documentCorners[0].x + documentCorners[2].x) / 2;
            float centerY = (documentCorners[0].y + documentCorners[2].y) / 2;
            canvas.scale(pulseScale, pulseScale, centerX, centerY);
        }

        // Draw in order (back to front)
        if (show3DPreview) {
            draw3DPreviewBox(canvas);
        }

        if (showGrid) {
            drawAlignmentGrid(canvas);
        }

        drawDocumentFill(canvas);
        drawDocumentBoundary(canvas);

        if (showCorners) {
            drawCornerHandles(canvas);
        }

        if (showDistances) {
            drawDistanceIndicators(canvas);
        }

        if (showAngles) {
            drawAngleIndicators(canvas);
        }

        // Restore canvas
        canvas.restore();

        // Draw quality score (not affected by scaling)
        if (showQualityScore) {
            drawQualityScore(canvas);
        }
    }

    /**
     * Draw animated document boundary
     */
    private void drawDocumentBoundary(Canvas canvas) {
        Path path = new Path();
        path.moveTo(documentCorners[0].x, documentCorners[0].y);

        for (int i = 1; i < documentCorners.length; i++) {
            path.lineTo(documentCorners[i].x, documentCorners[i].y);
        }
        path.close();

        // Update paint color
        boundaryPaint.setColor(currentBoundaryColor);

        // Draw shadow
        canvas.drawPath(path, shadowPaint);

        // Draw boundary
        canvas.drawPath(path, boundaryPaint);
    }

    /**
     * Draw semi-transparent fill
     */
    private void drawDocumentFill(Canvas canvas) {
        Path path = new Path();
        path.moveTo(documentCorners[0].x, documentCorners[0].y);

        for (int i = 1; i < documentCorners.length; i++) {
            path.lineTo(documentCorners[i].x, documentCorners[i].y);
        }
        path.close();

        fillPaint.setColor(currentBoundaryColor);
        canvas.drawPath(path, fillPaint);
    }

    /**
     * Draw corner handles
     */
    private void drawCornerHandles(Canvas canvas) {
        for (PointF corner : documentCorners) {
            // Outer circle (colored)
            cornerPaint.setColor(currentBoundaryColor);
            canvas.drawCircle(corner.x, corner.y, CORNER_SIZE / 2, cornerPaint);

            // Inner circle (white)
            cornerPaint.setColor(Color.WHITE);
            canvas.drawCircle(corner.x, corner.y, CORNER_INNER_SIZE / 2, cornerPaint);

            // Draw corner "L" shapes
            drawCornerL(canvas, corner, CORNER_SIZE);
        }
    }

    /**
     * Draw L-shaped corner marker
     */
    private void drawCornerL(Canvas canvas, PointF center, float size) {
        Paint lPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lPaint.setStyle(Paint.Style.STROKE);
        lPaint.setStrokeWidth(3f);
        lPaint.setColor(currentBoundaryColor);
        lPaint.setStrokeCap(Paint.Cap.ROUND);

        float halfSize = size / 2;

        // Horizontal line
        canvas.drawLine(
            center.x - halfSize, center.y,
            center.x + halfSize, center.y,
            lPaint
        );

        // Vertical line
        canvas.drawLine(
            center.x, center.y - halfSize,
            center.x, center.y + halfSize,
            lPaint
        );
    }

    /**
     * Draw alignment grid
     */
    private void drawAlignmentGrid(Canvas canvas) {
        // Calculate grid bounds from corners
        float minX = Math.min(Math.min(documentCorners[0].x, documentCorners[1].x),
                             Math.min(documentCorners[2].x, documentCorners[3].x));
        float maxX = Math.max(Math.max(documentCorners[0].x, documentCorners[1].x),
                             Math.max(documentCorners[2].x, documentCorners[3].x));
        float minY = Math.min(Math.min(documentCorners[0].y, documentCorners[1].y),
                             Math.min(documentCorners[2].y, documentCorners[3].y));
        float maxY = Math.max(Math.max(documentCorners[0].y, documentCorners[1].y),
                             Math.max(documentCorners[2].y, documentCorners[3].y));

        float width = maxX - minX;
        float height = maxY - minY;

        // Draw rule of thirds grid (4 lines)
        gridPaint.setColor(Color.WHITE);
        gridPaint.setAlpha(80);

        // Vertical lines
        canvas.drawLine(minX + width / 3, minY, minX + width / 3, maxY, gridPaint);
        canvas.drawLine(minX + 2 * width / 3, minY, minX + 2 * width / 3, maxY, gridPaint);

        // Horizontal lines
        canvas.drawLine(minX, minY + height / 3, maxX, minY + height / 3, gridPaint);
        canvas.drawLine(minX, minY + 2 * height / 3, maxX, minY + 2 * height / 3, gridPaint);

        // Draw center crosshair
        float centerX = (minX + maxX) / 2;
        float centerY = (minY + maxY) / 2;

        gridPaint.setAlpha(150);
        canvas.drawLine(centerX - 20, centerY, centerX + 20, centerY, gridPaint);
        canvas.drawLine(centerX, centerY - 20, centerX, centerY + 20, gridPaint);
    }

    /**
     * Draw distance indicators between corners
     */
    private void drawDistanceIndicators(Canvas canvas) {
        textPaint.setTextSize(28f);
        textPaint.setColor(Color.WHITE);

        // Draw distance for each edge
        for (int i = 0; i < documentCorners.length; i++) {
            PointF p1 = documentCorners[i];
            PointF p2 = documentCorners[(i + 1) % documentCorners.length];

            // Calculate midpoint
            float midX = (p1.x + p2.x) / 2;
            float midY = (p1.y + p2.y) / 2;

            // Calculate distance
            float distance = calculateDistance(p1, p2);

            // Convert to cm (assuming standard scaling)
            float distanceCm = distance / 10f; // Simplified conversion

            // Draw background
            String text = String.format("%.1f cm", distanceCm);
            RectF bounds = new RectF();
            textPaint.getTextBounds(text, 0, text.length(),
                new android.graphics.Rect((int)bounds.left, (int)bounds.top,
                    (int)bounds.right, (int)bounds.bottom));

            // Draw text
            canvas.drawText(text, midX, midY, textPaint);
        }
    }

    /**
     * Draw angle indicators at corners
     */
    private void drawAngleIndicators(Canvas canvas) {
        textPaint.setTextSize(24f);

        for (int i = 0; i < documentCorners.length; i++) {
            PointF p1 = documentCorners[(i - 1 + documentCorners.length) % documentCorners.length];
            PointF p2 = documentCorners[i];
            PointF p3 = documentCorners[(i + 1) % documentCorners.length];

            // Calculate angle
            float angle = calculateAngle(p1, p2, p3);

            // Offset text position
            float offsetX = (p2.x - p1.x + p2.x - p3.x) * 0.1f;
            float offsetY = (p2.y - p1.y + p2.y - p3.y) * 0.1f;

            // Draw angle
            String angleText = String.format("%.0f°", angle);
            canvas.drawText(angleText, p2.x + offsetX, p2.y + offsetY, textPaint);
        }
    }

    /**
     * Draw 3D preview box
     */
    private void draw3DPreviewBox(Canvas canvas) {
        // Calculate 3D offset based on preview depth and angle
        float offsetX = previewDepth * (float) Math.cos(Math.toRadians(previewAngle));
        float offsetY = previewDepth * (float) Math.sin(Math.toRadians(previewAngle));

        // Create back face corners
        PointF[] backCorners = new PointF[4];
        for (int i = 0; i < 4; i++) {
            backCorners[i] = new PointF(
                documentCorners[i].x - offsetX,
                documentCorners[i].y - offsetY
            );
        }

        // Draw back face (lighter)
        previewPaint.setAlpha(80);
        Path backPath = new Path();
        backPath.moveTo(backCorners[0].x, backCorners[0].y);
        for (int i = 1; i < 4; i++) {
            backPath.lineTo(backCorners[i].x, backCorners[i].y);
        }
        backPath.close();
        canvas.drawPath(backPath, previewPaint);

        // Draw connecting lines
        previewPaint.setAlpha(120);
        for (int i = 0; i < 4; i++) {
            canvas.drawLine(
                documentCorners[i].x, documentCorners[i].y,
                backCorners[i].x, backCorners[i].y,
                previewPaint
            );
        }

        // Draw front face outline (already drawn by main boundary)
        // This just adds to the 3D effect
    }

    /**
     * Draw quality score with animation
     */
    private void drawQualityScore(Canvas canvas) {
        // Draw in top-right corner
        float x = getWidth() - 100;
        float y = 120;

        // Background circle
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.BLACK);
        bgPaint.setAlpha(150);
        canvas.drawCircle(x, y, 60, bgPaint);

        // Colored ring based on quality
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(8f);
        ringPaint.setColor(currentBoundaryColor);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        // Draw arc representing score
        RectF arcRect = new RectF(x - 50, y - 50, x + 50, y + 50);
        float sweepAngle = (currentScore / 10f) * 360f;
        canvas.drawArc(arcRect, -90, sweepAngle, false, ringPaint);

        // Draw score text
        scorePaint.setColor(currentBoundaryColor);
        String scoreText = String.format("%.0f", currentScore);
        canvas.drawText(scoreText, x, y + 20, scorePaint);

        // Draw label
        textPaint.setTextSize(20f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("/10", x, y + 45, textPaint);
    }

    // ================================
    // Animation Methods
    // ================================

    /**
     * Animate color based on quality score
     */
    private void animateColorChange(int targetColor) {
        if (currentBoundaryColor == targetColor) {
            return;
        }

        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }

        colorAnimator = ValueAnimator.ofArgb(currentBoundaryColor, targetColor);
        colorAnimator.setDuration(COLOR_ANIMATION_DURATION);
        colorAnimator.addUpdateListener(animation -> {
            currentBoundaryColor = (int) animation.getAnimatedValue();
            invalidate();
        });
        colorAnimator.start();
    }

    /**
     * Start pulse animation
     */
    public void startPulseAnimation() {
        if (!pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
    }

    /**
     * Stop pulse animation
     */
    public void stopPulseAnimation() {
        if (pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
            pulseScale = 1.0f;
            invalidate();
        }
    }

    /**
     * Animate score change
     */
    private void animateScoreChange(float newScore) {
        if (currentScore == newScore) {
            return;
        }

        targetScore = newScore;

        if (scoreAnimator != null && scoreAnimator.isRunning()) {
            scoreAnimator.cancel();
        }

        scoreAnimator.start();
    }

    // ================================
    // Update Methods
    // ================================

    /**
     * Update document corners
     */
    public void setDocumentCorners(PointF[] corners) {
        if (corners != null && corners.length == 4) {
            this.documentCorners = corners;
            invalidate();
        }
    }

    /**
     * Update quality score (0-10)
     */
    public void setQualityScore(int score) {
        this.qualityScore = Math.max(0, Math.min(10, score));

        // Update color based on score
        int targetColor;
        if (qualityScore >= 8) {
            targetColor = COLOR_EXCELLENT;
        } else if (qualityScore >= 6) {
            targetColor = COLOR_GOOD;
        } else if (qualityScore >= 4) {
            targetColor = COLOR_FAIR;
        } else {
            targetColor = COLOR_POOR;
        }

        animateColorChange(targetColor);
        animateScoreChange(qualityScore);
    }

    /**
     * Update from detected document
     */
    public void updateFromDocument(ARDocumentDetector.DetectedDocument document,
                                   int qualityScore) {
        if (document != null && document.corners2D != null) {
            PointF[] corners = new PointF[4];
            for (int i = 0; i < 4; i++) {
                corners[i] = new PointF(
                    (float) document.corners2D[i].x,
                    (float) document.corners2D[i].y
                );
            }
            setDocumentCorners(corners);
            setQualityScore(qualityScore);
        }
    }

    // ================================
    // Visibility Controls
    // ================================

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        invalidate();
    }

    public void setShowCorners(boolean show) {
        this.showCorners = show;
        invalidate();
    }

    public void setShowDistances(boolean show) {
        this.showDistances = show;
        invalidate();
    }

    public void setShowAngles(boolean show) {
        this.showAngles = show;
        invalidate();
    }

    public void setShow3DPreview(boolean show) {
        this.show3DPreview = show;
        invalidate();
    }

    public void setShowQualityScore(boolean show) {
        this.showQualityScore = show;
        invalidate();
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Calculate distance between two points
     */
    private float calculateDistance(PointF p1, PointF p2) {
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate angle between three points
     */
    private float calculateAngle(PointF p1, PointF p2, PointF p3) {
        float v1x = p1.x - p2.x;
        float v1y = p1.y - p2.y;
        float v2x = p3.x - p2.x;
        float v2y = p3.y - p2.y;

        float dot = v1x * v2x + v1y * v2y;
        float mag1 = (float) Math.sqrt(v1x * v1x + v1y * v1y);
        float mag2 = (float) Math.sqrt(v2x * v2x + v2y * v2y);

        float cosAngle = dot / (mag1 * mag2);
        return (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAngle))));
    }

    /**
     * Clear overlay
     */
    public void clear() {
        documentCorners = null;
        qualityScore = 0;
        currentScore = 0;
        targetScore = 0;
        stopPulseAnimation();
        invalidate();
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }
        clear();
    }
}


