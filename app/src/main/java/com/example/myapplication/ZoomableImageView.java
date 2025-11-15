package com.example.myapplication;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * ZoomableImageView - Custom ImageView with zoom and pan support
 * Supports pinch-to-zoom and double-tap to zoom
 */
public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {

    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    // For panning
    private float lastTouchX;
    private float lastTouchY;
    private float posX;
    private float posY;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // Handle panning
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (scale > MIN_SCALE) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    posX += dx;
                    posY += dy;

                    updateMatrix();

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
        }

        return true;
    }

    private void updateMatrix() {
        matrix.setScale(scale, scale);
        matrix.postTranslate(posX, posY);
        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));

            // Reset position when zooming out to min scale
            if (scale == MIN_SCALE) {
                posX = 0;
                posY = 0;
            }

            updateMatrix();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Toggle between min and max zoom on double tap
            if (scale > MIN_SCALE) {
                scale = MIN_SCALE;
                posX = 0;
                posY = 0;
            } else {
                scale = MAX_SCALE / 2;
            }
            updateMatrix();
            return true;
        }
    }
}

