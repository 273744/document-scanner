package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * ARAnimationController - Smooth AR transition animations
 *
 * Features:
 * - Document boundary fade in/out animations
 * - Quality score number rolling animations
 * - Smooth overlay color transitions
 * - Capture countdown with AR effects
 * - Success celebration animations
 * - Guide arrow animations for positioning
 * - Loading states with AR indicators
 * - Custom easing functions
 * - Performance optimization
 */
public class ARAnimationController {

    private static final String TAG = "ARAnimationController";

    // Animation durations (ms)
    private static final long FADE_DURATION = 300;
    private static final long COLOR_TRANSITION_DURATION = 400;
    private static final long SCORE_ROLL_DURATION = 600;
    private static final long COUNTDOWN_DURATION = 1000;
    private static final long CELEBRATION_DURATION = 1500;
    private static final long ARROW_PULSE_DURATION = 800;
    private static final long LOADING_ROTATION_DURATION = 1200;

    // Easing interpolators
    private Interpolator easeInOut = new AccelerateDecelerateInterpolator();
    private Interpolator easeOut = new DecelerateInterpolator();
    private Interpolator easeIn = new AccelerateInterpolator();
    private Interpolator bounce = new BounceInterpolator();
    private Interpolator overshoot = new OvershootInterpolator(2.0f);
    private Interpolator anticipateOvershoot = new AnticipateOvershootInterpolator(2.0f);
    private Interpolator linear = new LinearInterpolator();

    // Active animators
    private List<ValueAnimator> activeAnimators = new ArrayList<>();

    // Animation callbacks
    private AnimationCallbacks callbacks;

    // State
    private boolean isAnimating = false;
    private float currentBoundaryAlpha = 0f;
    private int currentScore = 0;
    private int currentColor = 0;

    /**
     * Constructor
     */
    public ARAnimationController() {
        // Initialize
    }

    // ================================
    // 1. Document Boundary Animations
    // ================================

    /**
     * Fade in document boundary
     */
    public void fadeInBoundary(final View overlayView) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentBoundaryAlpha, 1.0f);
        animator.setDuration(FADE_DURATION);
        animator.setInterpolator(easeOut);
        animator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            currentBoundaryAlpha = alpha;
            overlayView.setAlpha(alpha);

            if (callbacks != null) {
                callbacks.onBoundaryAlphaChanged(alpha);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (callbacks != null) {
                    callbacks.onBoundaryFadeInComplete();
                }
                removeAnimator((ValueAnimator) animation);
            }
        });

        startAnimator(animator);
    }

    /**
     * Fade out document boundary
     */
    public void fadeOutBoundary(final View overlayView) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentBoundaryAlpha, 0f);
        animator.setDuration(FADE_DURATION);
        animator.setInterpolator(easeIn);
        animator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            currentBoundaryAlpha = alpha;
            overlayView.setAlpha(alpha);

            if (callbacks != null) {
                callbacks.onBoundaryAlphaChanged(alpha);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlayView.setVisibility(View.GONE);
                if (callbacks != null) {
                    callbacks.onBoundaryFadeOutComplete();
                }
                removeAnimator((ValueAnimator) animation);
            }
        });

        startAnimator(animator);
    }

    /**
     * Pulse boundary (for detected document)
     */
    public void pulseBoundary(final View overlayView, int repeatCount) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 1.15f, 1.0f);
        animator.setDuration(600);
        animator.setRepeatCount(repeatCount);
        animator.setInterpolator(easeInOut);
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            overlayView.setScaleX(scale);
            overlayView.setScaleY(scale);
        });

        startAnimator(animator);
    }

    // ================================
    // 2. Quality Score Animations
    // ================================

    /**
     * Animate quality score with rolling numbers
     */
    public void animateScoreRoll(int fromScore, int toScore,
                                 final ScoreUpdateCallback callback) {
        ValueAnimator animator = ValueAnimator.ofInt(fromScore, toScore);
        animator.setDuration(SCORE_ROLL_DURATION);
        animator.setInterpolator(new CustomEaseOut());
        animator.addUpdateListener(animation -> {
            int score = (int) animation.getAnimatedValue();
            currentScore = score;

            if (callback != null) {
                callback.onScoreUpdate(score);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (callback != null) {
                    callback.onScoreAnimationComplete(toScore);
                }
                removeAnimator((ValueAnimator) animation);
            }
        });

        startAnimator(animator);
    }

    /**
     * Animate score with bounce effect (for milestones)
     */
    public void animateScoreBounce(final View scoreView) {
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f);
        scaleAnimator.setDuration(500);
        scaleAnimator.setInterpolator(bounce);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            scoreView.setScaleX(scale);
            scoreView.setScaleY(scale);
        });

        startAnimator(scaleAnimator);
    }

    // ================================
    // 3. Color Transition Animations
    // ================================

    /**
     * Smooth color transition
     */
    public void animateColorTransition(int fromColor, int toColor,
                                       final ColorUpdateCallback callback) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        animator.setDuration(COLOR_TRANSITION_DURATION);
        animator.setInterpolator(easeInOut);
        animator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            currentColor = color;

            if (callback != null) {
                callback.onColorUpdate(color);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (callback != null) {
                    callback.onColorTransitionComplete(toColor);
                }
                removeAnimator((ValueAnimator) animation);
            }
        });

        startAnimator(animator);
    }

    /**
     * Quality-based color transition (red -> yellow -> green)
     */
    public void animateQualityColor(int qualityScore,
                                    final ColorUpdateCallback callback) {
        int targetColor;

        if (qualityScore >= 8) {
            targetColor = 0xFF007BFF; // Blue - Excellent
        } else if (qualityScore >= 6) {
            targetColor = 0xFF28A745; // Green - Good
        } else if (qualityScore >= 4) {
            targetColor = 0xFFFFC107; // Yellow - Fair
        } else {
            targetColor = 0xFFDC3545; // Red - Poor
        }

        if (currentColor == 0) {
            currentColor = targetColor;
            if (callback != null) {
                callback.onColorUpdate(targetColor);
            }
        } else {
            animateColorTransition(currentColor, targetColor, callback);
        }
    }

    // ================================
    // 4. Capture Countdown Animation
    // ================================

    /**
     * Animate capture countdown (3, 2, 1, GO!)
     */
    public void animateCountdown(final CountdownCallback callback) {
        final int[] countdownValues = {3, 2, 1, 0}; // 0 = "GO!"

        for (int i = 0; i < countdownValues.length; i++) {
            final int value = countdownValues[i];
            final boolean isLast = (i == countdownValues.length - 1);

            long delay = i * COUNTDOWN_DURATION;

            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(COUNTDOWN_DURATION);
            animator.setStartDelay(delay);
            animator.setInterpolator(easeOut);
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();

                if (callback != null) {
                    callback.onCountdownUpdate(value, progress);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (callback != null) {
                        callback.onCountdownNumber(value);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isLast && callback != null) {
                        callback.onCountdownComplete();
                    }
                    removeAnimator((ValueAnimator) animation);
                }
            });

            startAnimator(animator);
        }
    }

    /**
     * Animate countdown with AR effects (scale + fade)
     */
    public void animateCountdownWithEffects(final View countdownView,
                                           final CountdownCallback callback) {
        countdownView.setVisibility(View.VISIBLE);

        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.5f, 1.5f);
        scaleAnimator.setDuration(COUNTDOWN_DURATION);
        scaleAnimator.setInterpolator(easeOut);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            countdownView.setScaleX(scale);
            countdownView.setScaleY(scale);
        });

        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1.0f, 0f);
        alphaAnimator.setDuration(COUNTDOWN_DURATION);
        alphaAnimator.setInterpolator(easeIn);
        alphaAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            countdownView.setAlpha(alpha);
        });

        startAnimator(scaleAnimator);
        startAnimator(alphaAnimator);
    }

    // ================================
    // 5. Success Celebration Animation
    // ================================

    /**
     * Success celebration with multiple effects
     */
    public void animateSuccess(final View successView,
                               final CelebrationCallback callback) {
        successView.setVisibility(View.VISIBLE);
        successView.setScaleX(0f);
        successView.setScaleY(0f);
        successView.setAlpha(0f);

        // Scale animation with overshoot
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0f, 1.0f);
        scaleAnimator.setDuration(600);
        scaleAnimator.setInterpolator(overshoot);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            successView.setScaleX(scale);
            successView.setScaleY(scale);
        });

        // Fade in
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0f, 1.0f);
        alphaAnimator.setDuration(300);
        alphaAnimator.setInterpolator(easeOut);
        alphaAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            successView.setAlpha(alpha);
        });

        // Rotation animation
        ValueAnimator rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(800);
        rotationAnimator.setInterpolator(easeInOut);
        rotationAnimator.addUpdateListener(animation -> {
            float rotation = (float) animation.getAnimatedValue();
            successView.setRotation(rotation);
        });

        // Fade out after showing
        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(1.0f, 0f);
        fadeOutAnimator.setDuration(400);
        fadeOutAnimator.setStartDelay(1000);
        fadeOutAnimator.setInterpolator(easeIn);
        fadeOutAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            successView.setAlpha(alpha);
        });
        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                successView.setVisibility(View.GONE);
                if (callback != null) {
                    callback.onCelebrationComplete();
                }
                removeAnimator((ValueAnimator) animation);
            }
        });

        startAnimator(scaleAnimator);
        startAnimator(alphaAnimator);
        startAnimator(rotationAnimator);
        startAnimator(fadeOutAnimator);

        if (callback != null) {
            callback.onCelebrationStart();
        }
    }

    /**
     * Confetti burst animation
     */
    public void animateConfetti(final ConfettiCallback callback) {
        if (callback == null) return;

        // Create multiple confetti particles
        int particleCount = 20;

        for (int i = 0; i < particleCount; i++) {
            final int particleId = i; // Make final for lambda
            final float startX = 0.5f; // Center
            final float startY = 0.5f;
            final float angle = (float) (Math.random() * 360);
            final float velocity = 0.3f + (float) Math.random() * 0.5f;
            final int color = getRandomConfettiColor();

            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(CELEBRATION_DURATION);
            animator.setInterpolator(new DecelerateInterpolator(2.0f));
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();

                // Calculate position
                float radians = (float) Math.toRadians(angle);
                float distance = velocity * progress;
                float x = startX + (float) Math.cos(radians) * distance;
                float y = startY + (float) Math.sin(radians) * distance;

                // Gravity effect
                y += progress * progress * 0.3f;

                // Rotation
                float rotation = progress * 720f; // 2 full rotations

                // Alpha (fade out)
                float alpha = 1.0f - progress;

                callback.onConfettiUpdate(particleId, x, y, rotation, alpha, color);
            });

            startAnimator(animator);
        }
    }

    // ================================
    // 6. Guide Arrow Animations
    // ================================

    /**
     * Animate guide arrow pointing in direction
     */
    public void animateGuideArrow(final View arrowView, float targetAngle) {
        // Rotate to point in direction
        ValueAnimator rotationAnimator = ValueAnimator.ofFloat(
            arrowView.getRotation(), targetAngle);
        rotationAnimator.setDuration(400);
        rotationAnimator.setInterpolator(easeInOut);
        rotationAnimator.addUpdateListener(animation -> {
            float rotation = (float) animation.getAnimatedValue();
            arrowView.setRotation(rotation);
        });

        startAnimator(rotationAnimator);
    }

    /**
     * Pulse guide arrow to draw attention
     */
    public void pulseGuideArrow(final View arrowView) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f);
        animator.setDuration(ARROW_PULSE_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(easeInOut);
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            arrowView.setScaleX(scale);
            arrowView.setScaleY(scale);
        });

        startAnimator(animator);
    }

    /**
     * Animate arrow movement (to guide positioning)
     */
    public void animateArrowMovement(final View arrowView,
                                     float fromX, float fromY,
                                     float toX, float toY) {
        ValueAnimator xAnimator = ValueAnimator.ofFloat(fromX, toX);
        xAnimator.setDuration(800);
        xAnimator.setInterpolator(easeInOut);
        xAnimator.addUpdateListener(animation -> {
            float x = (float) animation.getAnimatedValue();
            arrowView.setTranslationX(x);
        });

        ValueAnimator yAnimator = ValueAnimator.ofFloat(fromY, toY);
        yAnimator.setDuration(800);
        yAnimator.setInterpolator(easeInOut);
        yAnimator.addUpdateListener(animation -> {
            float y = (float) animation.getAnimatedValue();
            arrowView.setTranslationY(y);
        });

        startAnimator(xAnimator);
        startAnimator(yAnimator);
    }

    /**
     * Stop arrow animations
     */
    public void stopArrowAnimations() {
        cancelAllAnimators();
    }

    // ================================
    // 7. Loading State Animations
    // ================================

    /**
     * Rotating loading indicator
     */
    public void animateLoading(final View loadingView) {
        loadingView.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(LOADING_ROTATION_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(linear);
        animator.addUpdateListener(animation -> {
            float rotation = (float) animation.getAnimatedValue();
            loadingView.setRotation(rotation);
        });

        startAnimator(animator);
    }

    /**
     * Pulsing loading indicator
     */
    public void animateLoadingPulse(final View loadingView) {
        loadingView.setVisibility(View.VISIBLE);

        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.8f, 1.2f, 0.8f);
        scaleAnimator.setDuration(1000);
        scaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnimator.setInterpolator(easeInOut);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            loadingView.setScaleX(scale);
            loadingView.setScaleY(scale);
        });

        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.5f, 1.0f, 0.5f);
        alphaAnimator.setDuration(1000);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setInterpolator(easeInOut);
        alphaAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            loadingView.setAlpha(alpha);
        });

        startAnimator(scaleAnimator);
        startAnimator(alphaAnimator);
    }

    /**
     * Stop loading animation
     */
    public void stopLoading(final View loadingView) {
        cancelAllAnimators();
        loadingView.setVisibility(View.GONE);
        loadingView.setRotation(0);
        loadingView.setScaleX(1.0f);
        loadingView.setScaleY(1.0f);
        loadingView.setAlpha(1.0f);
    }

    // ================================
    // Custom Easing Functions
    // ================================

    /**
     * Custom ease out curve
     */
    private static class CustomEaseOut implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return (float) (1 - Math.pow(1 - input, 3));
        }
    }

    /**
     * Elastic ease out (bouncy effect)
     */
    public static class ElasticEaseOut implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            if (input == 0 || input == 1) return input;

            float p = 0.3f;
            return (float) (Math.pow(2, -10 * input) *
                Math.sin((input - p / 4) * (2 * Math.PI) / p) + 1);
        }
    }

    /**
     * Cubic bezier easing
     */
    public static class CubicBezierEasing implements Interpolator {
        private float x1, y1, x2, y2;

        public CubicBezierEasing(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public float getInterpolation(float input) {
            // Simplified cubic bezier (would need full implementation for production)
            return input * input * (3.0f - 2.0f * input);
        }
    }

    // ================================
    // Animation Management
    // ================================

    /**
     * Start animator and track it
     */
    private void startAnimator(ValueAnimator animator) {
        activeAnimators.add(animator);
        animator.start();
        isAnimating = true;
    }

    /**
     * Remove animator from tracking
     */
    private void removeAnimator(ValueAnimator animator) {
        activeAnimators.remove(animator);
        if (activeAnimators.isEmpty()) {
            isAnimating = false;
        }
    }

    /**
     * Cancel all active animations
     */
    public void cancelAllAnimators() {
        for (ValueAnimator animator : new ArrayList<>(activeAnimators)) {
            animator.cancel();
        }
        activeAnimators.clear();
        isAnimating = false;
    }

    /**
     * Check if any animation is running
     */
    public boolean isAnimating() {
        return isAnimating;
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallbacks(AnimationCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public interface AnimationCallbacks {
        void onBoundaryAlphaChanged(float alpha);
        void onBoundaryFadeInComplete();
        void onBoundaryFadeOutComplete();
    }

    public interface ScoreUpdateCallback {
        void onScoreUpdate(int score);
        void onScoreAnimationComplete(int finalScore);
    }

    public interface ColorUpdateCallback {
        void onColorUpdate(int color);
        void onColorTransitionComplete(int finalColor);
    }

    public interface CountdownCallback {
        void onCountdownNumber(int number);
        void onCountdownUpdate(int number, float progress);
        void onCountdownComplete();
    }

    public interface CelebrationCallback {
        void onCelebrationStart();
        void onCelebrationComplete();
    }

    public interface ConfettiCallback {
        void onConfettiUpdate(int particleId, float x, float y,
                             float rotation, float alpha, int color);
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Get random confetti color
     */
    private int getRandomConfettiColor() {
        int[] colors = {
            0xFFFF6B6B, // Red
            0xFF4ECDC4, // Cyan
            0xFFFFE66D, // Yellow
            0xFF95E1D3, // Mint
            0xFFF38181, // Pink
            0xFFAA96DA  // Purple
        };
        return colors[(int) (Math.random() * colors.length)];
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        cancelAllAnimators();
        callbacks = null;
    }
}

