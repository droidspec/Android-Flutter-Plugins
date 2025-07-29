package com.ahmed.pdfx.pdfreadx.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {
    private Matrix matrix = new Matrix();
    private float scaleFactor = 1f;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private PointF lastTouch = new PointF();
    private float minScale = 1f;
    private float maxScale = 5f;
    private boolean isAnimating = false;
    private ZoomListener zoomListener;
    private float[] matrixValues = new float[9];
    private OnSwipeListener onSwipeListener;
    private float swipeOffset = 0;
    private boolean isSwiping = false;

    // Swipe detection constants
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final float SWIPE_EDGE_WIDTH_RATIO = 0.1f; // 10% of screen width

    // Animation variables
    private ValueAnimator zoomAnimator;
    private float animStartScale, animEndScale;
    private PointF animFocusPoint = new PointF();

    public interface ZoomListener {
        void onZoomChanged(float scale);
    }

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
        void onSwipeProgress(float offset, boolean isRightSwipe);
    }

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        float density = getResources().getDisplayMetrics().density;
        maxScale = Math.max(5f, 3f * density);
    }

    public void setZoomListener(ZoomListener listener) {
        this.zoomListener = listener;
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.onSwipeListener = listener;
    }

    public float getMinScale() {
        return minScale;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && getDrawable() != null) {
            updateInitialScale();
        }
    }

    private void updateInitialScale() {
        if (getDrawable() == null) return;

        int drawableWidth = getDrawable().getIntrinsicWidth();
        int drawableHeight = getDrawable().getIntrinsicHeight();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (drawableWidth <= 0 || drawableHeight <= 0) return;

        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        minScale = Math.min(scaleX, scaleY);

        matrix.setScale(minScale, minScale);

        float scaledWidth = drawableWidth * minScale;
        float scaledHeight = drawableHeight * minScale;
        float translateX = (viewWidth - scaledWidth) / 2;
        float translateY = (viewHeight - scaledHeight) / 2;

        matrix.postTranslate(translateX, translateY);
        setImageMatrix(matrix);
        scaleFactor = minScale;

        if (zoomListener != null) {
            zoomListener.onZoomChanged(scaleFactor);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (!isAnimating) {
            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouch.set(curr);
                    isSwiping = false;
                    swipeOffset = 0;

                    // Check if touch started in edge area
                    float edgeWidth = getWidth() * SWIPE_EDGE_WIDTH_RATIO;
                    if (curr.x < edgeWidth || curr.x > getWidth() - edgeWidth) {
                        isSwiping = true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!scaleGestureDetector.isInProgress()) {
                        float dx = curr.x - lastTouch.x;
                        float dy = curr.y - lastTouch.y;

                        if (isSwiping && Math.abs(dx) > Math.abs(dy) && scaleFactor <= minScale * 1.1f) {
                            swipeOffset = dx * 0.5f;

                            matrix.postTranslate(dx * 0.5f, 0);
                            setImageMatrix(matrix);

                            if (onSwipeListener != null) {
                                onSwipeListener.onSwipeProgress(swipeOffset, dx > 0);
                            }
                        } else {
                            handlePanning(curr);
                        }
                    }
                    lastTouch.set(curr.x, curr.y);
                    break;

                case MotionEvent.ACTION_UP:
                    if (isSwiping) {
                        if (Math.abs(swipeOffset) > getWidth() / 4) {
                            if (swipeOffset > 0 && onSwipeListener != null) {
                                onSwipeListener.onSwipeRight();
                            } else if (onSwipeListener != null) {
                                onSwipeListener.onSwipeLeft();
                            }
                        }
                        animateToScale(scaleFactor, getWidth()/2f, getHeight()/2f);
                    } else {
                        postDelayed(this::checkBounds, 100);
                    }
                    isSwiping = false;
                    break;
            }
        }
        return true;
    }

    private void handlePanning(PointF curr) {
        float dx = curr.x - lastTouch.x;
        float dy = curr.y - lastTouch.y;

        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];

        float imageWidth = getDrawable().getIntrinsicWidth() * scaleX;
        float imageHeight = getDrawable().getIntrinsicHeight() * scaleY;

        if (imageWidth > getWidth()) {
            if (transX + dx > 0) dx = -transX;
            else if (transX + dx < getWidth() - imageWidth) dx = getWidth() - imageWidth - transX;
        } else {
            dx = 0;
        }

        if (imageHeight > getHeight()) {
            if (transY + dy > 0) dy = -transY;
            else if (transY + dy < getHeight() - imageHeight) dy = getHeight() - imageHeight - transY;
        } else {
            dy = 0;
        }

        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    private void checkBounds() {
        if (isAnimating) return;

        float[] values = new float[9];
        matrix.getValues(values);
        float scaleX = values[Matrix.MSCALE_X];

        if (scaleX < minScale * 0.9f) {
            animateToScale(minScale, getWidth()/2f, getHeight()/2f);
        }
    }

    public void resetZoom() {
        animateToScale(minScale, getWidth() / 2f, getHeight() / 2f);
    }

    private void handleDoubleTap(float x, float y) {
        matrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];

        float targetScale;
        if (currentScale <= minScale * 1.1f) {
            targetScale = Math.min(minScale * 2.5f, maxScale);
        } else {
            targetScale = minScale;
        }

        animateToScale(targetScale, x, y);
    }

    private void animateToScale(float targetScale, float focusX, float focusY) {
        if (zoomAnimator != null && zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
        }

        matrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTrans = getCurrentTranslation();
        PointF targetTrans = calculateTargetTranslation(targetScale, focusX, focusY);

        animFocusPoint.set(focusX, focusY);
        animStartScale = currentScale;
        animEndScale = targetScale;

        zoomAnimator = ValueAnimator.ofFloat(0f, 1f);
        zoomAnimator.setDuration(250);
        zoomAnimator.setInterpolator(new DecelerateInterpolator());
        zoomAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            float interpolatedScale = animStartScale + (animEndScale - animStartScale) * fraction;

            float interpolatedTransX = currentTrans.x + (targetTrans.x - currentTrans.x) * fraction;
            float interpolatedTransY = currentTrans.y + (targetTrans.y - currentTrans.y) * fraction;

            matrix.setScale(interpolatedScale, interpolatedScale);
            matrix.postTranslate(interpolatedTransX, interpolatedTransY);
            setImageMatrix(matrix);
            scaleFactor = interpolatedScale;

            if (zoomListener != null) {
                zoomListener.onZoomChanged(scaleFactor);
            }
        });

        zoomAnimator.start();
        isAnimating = true;
        zoomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                if (animEndScale != minScale) {
                    postDelayed(() -> checkBounds(), 300);
                }
            }
        });
    }

    private PointF getCurrentTranslation() {
        float[] values = new float[9];
        matrix.getValues(values);
        return new PointF(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
    }

    private PointF calculateTargetTranslation(float targetScale, float focusX, float focusY) {
        float imageWidth = getDrawable().getIntrinsicWidth() * targetScale;
        float imageHeight = getDrawable().getIntrinsicHeight() * targetScale;

        float targetTransX = focusX - (focusX * targetScale / scaleFactor)
                + (matrixValues[Matrix.MTRANS_X] * targetScale / scaleFactor);
        float targetTransY = focusY - (focusY * targetScale / scaleFactor)
                + (matrixValues[Matrix.MTRANS_Y] * targetScale / scaleFactor);

        if (imageWidth <= getWidth()) {
            targetTransX = (getWidth() - imageWidth) / 2;
        } else {
            targetTransX = Math.min(0, Math.max(getWidth() - imageWidth, targetTransX));
        }

        if (imageHeight <= getHeight()) {
            targetTransY = (getHeight() - imageHeight) / 2;
        } else {
            targetTransY = Math.min(0, Math.max(getHeight() - imageHeight, targetTransY));
        }

        return new PointF(targetTransX, targetTransY);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isAnimating = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scaleFactor * detector.getScaleFactor();
            newScale = Math.max(minScale * 0.8f, Math.min(newScale, maxScale * 1.2f));

            matrix.setScale(newScale, newScale, detector.getFocusX(), detector.getFocusY());
            adjustTranslation(newScale);
            setImageMatrix(matrix);
            scaleFactor = newScale;

            if (zoomListener != null) {
                zoomListener.onZoomChanged(scaleFactor);
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isAnimating = false;
            if (scaleFactor < minScale * 0.9f) {
                animateToScale(minScale, detector.getFocusX(), detector.getFocusY());
            } else if (scaleFactor > maxScale * 1.1f) {
                animateToScale(maxScale, detector.getFocusX(), detector.getFocusY());
            } else {
                checkBounds();
            }
        }

        private void adjustTranslation(float newScale) {
            if (getDrawable() == null) return;

            matrix.getValues(matrixValues);
            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];
            float imageWidth = getDrawable().getIntrinsicWidth() * newScale;
            float imageHeight = getDrawable().getIntrinsicHeight() * newScale;

            if (imageWidth > getWidth()) {
                if (transX > 0) transX = 0;
                else if (transX < getWidth() - imageWidth) transX = getWidth() - imageWidth;
            } else {
                transX = (getWidth() - imageWidth) / 2;
            }

            if (imageHeight > getHeight()) {
                if (transY > 0) transY = 0;
                else if (transY < getHeight() - imageHeight) transY = getHeight() - imageHeight;
            } else {
                transY = (getHeight() - imageHeight) / 2;
            }

            matrix.postTranslate(transX - matrixValues[Matrix.MTRANS_X], transY - matrixValues[Matrix.MTRANS_Y]);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            handleDoubleTap(e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            performClick();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        if (onSwipeListener != null) {
                            onSwipeListener.onSwipeRight();
                        }
                    } else {
                        if (onSwipeListener != null) {
                            onSwipeListener.onSwipeLeft();
                        }
                    }
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
            return false;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}