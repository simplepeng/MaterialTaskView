package com.simple.library;

import android.animation.Animator.AnimatorListener;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;


public class TaskView extends FrameLayout{

    View mContent;
    TaskViewThumbnail mThumbnailView;
    RecentsConfiguration mConfig;
    boolean mIsFullScreenView = false;
    float mTaskProgress;
    ObjectAnimator mTaskProgressAnimator;
    ObjectAnimator mDimAnimator;
    float mMaxDimScale;
    int mDim;
    AccelerateInterpolator mDimInterpolator = new AccelerateInterpolator(1f);
    PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.MULTIPLY);
    Paint mLayerPaint = new Paint();
    
    // Optimizations
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setTaskProgress((Float) animation.getAnimatedValue());
                }
            };
                        
    public TaskView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mConfig = RecentsConfiguration.getInstance();
        setTaskProgress(getTaskProgress());
        setDim(getDim());
        // TODO Auto-generated constructor stub
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        // TODO Auto-generated constructor stub
    }

    public TaskView(Context context) {
        this(context,null);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onFinishInflate() {
        // Bind the views
        mContent = findViewById(R.id.task_view_content);
        mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthWithoutPadding = width - this.getPaddingLeft() - this.getPaddingRight();
        int heightWithoutPadding = height - this.getPaddingTop() - this.getPaddingBottom();
        
        // Measure the content
        mContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY));

        // Measure the bar view, thumbnail, and footer
//        mHeaderView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(mConfig.taskBarHeight, MeasureSpec.EXACTLY));
//        if (mFooterView != null) {
//            mFooterView.measure(
//                    MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(mConfig.taskViewLockToAppButtonHeight,
//                            MeasureSpec.EXACTLY));
//        }
        
//        mActionButtonView.measure(
//                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.AT_MOST),
//                MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.AT_MOST));
        if (mIsFullScreenView) {
            // Measure the thumbnail height to be the full dimensions
            mThumbnailView.measure(
                    MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY));
        } else {
            // Measure the thumbnail to be square
            mThumbnailView.measure(
                    MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
        //invalidateOutline();
    }
    
    public boolean isFullScreenView(){
        return mIsFullScreenView;
    }
    
    void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask,
                                             boolean occludesLaunchTarget, int offscreenY) {
        if (mConfig.launchedFromAppWithScreenshot) {
            if (isTaskViewLaunchTargetTask) {
                // Hide the footer during the transition in, and animate it out afterwards?
//                if (mFooterView != null) {
//                    mFooterView.animateFooterVisibility(false, 0);
//                }
            } else {
                // Don't do anything for the side views when animating in
            }

        } else if (mConfig.launchedFromAppWithThumbnail) {
            if (isTaskViewLaunchTargetTask) {
                // Hide the action button if it exists
                //mActionButtonView.setAlpha(0f);
                // Set the dim to 0 so we can animate it in
                //initialDim = 0;
            } else if (occludesLaunchTarget) {
                // Move the task view off screen (below) so we can animate it in
                setTranslationY(offscreenY);
            }
        } else if (mConfig.launchedFromHome) {
            // Move the task view off screen (below) so we can animate it in
            setTranslationY(offscreenY);
            //setTranslationZ(0);
            setScaleX(1f);
            setScaleY(1f);
        }
    }
    
    /** Animates this task view as it enters recents */
    void startEnterRecentsAnimation(final ViewAnimation.TaskViewEnterContext ctx) {
        final TaskViewTransform transform = ctx.currentTaskTransform;
        int startDelay = 0;

        if (mConfig.launchedFromAppWithScreenshot) {
            if (/*mTask.isLaunchTarget*/false) {
                Rect taskRect = ctx.currentTaskRect;
                int duration = mConfig.taskViewEnterFromHomeDuration * 10;
                int windowInsetTop = mConfig.systemInsets.top; // XXX: Should be for the window
                float taskScale = ((float) taskRect.width() / getMeasuredWidth()) * transform.scale;
                float scaledYOffset = ((1f - taskScale) * getMeasuredHeight()) / 2;
                float scaledWindowInsetTop = (int) (taskScale * windowInsetTop);
                float scaledTranslationY = taskRect.top + transform.translationY -
                        (scaledWindowInsetTop + scaledYOffset);
                startDelay = mConfig.taskViewEnterFromHomeStaggerDelay;

                // Animate the top clip
//                mViewBounds.animateClipTop(windowInsetTop, duration,
//                        new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animation) {
//                        int y = (Integer) animation.getAnimatedValue();
//                        mHeaderView.setTranslationY(y);
//                    }
//                });
                // Animate the bottom or right clip
                int size = Math.round((taskRect.width() / taskScale));
                if (mConfig.hasHorizontalLayout()) {
                    //mViewBounds.animateClipRight(getMeasuredWidth() - size, duration);
                } else {
                    //mViewBounds.animateClipBottom(getMeasuredHeight() - (windowInsetTop + size), duration);
                }
                // Animate the task bar of the first task view
                animate()
                        .scaleX(taskScale)
                        .scaleY(taskScale)
                        .translationY(scaledTranslationY)
                        .setDuration(duration)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                //setIsFullScreen(false);
                                requestLayout();

                                // Reset the clip
                                //mViewBounds.setClipTop(0);
                                //mViewBounds.setClipBottom(0);
                                //mViewBounds.setClipRight(0);
                                // Reset the bar translation
                                //mHeaderView.setTranslationY(0);
                                // Animate the footer into view (if it is the front most task)
                                //animateFooterVisibility(true, mConfig.taskBarEnterAnimDuration);

                                // Unbind the thumbnail from the screenshot
                                //RecentsTaskLoader.getInstance().loadTaskData(mTask);
                                // Recycle the full screen screenshot
                                //AlternateRecentsComponent.consumeLastScreenshot();

                               //mCb.onTaskViewFullScreenTransitionCompleted();

                                // Decrement the post animation trigger
                                ctx.postAnimationTrigger.decrement();
                            }
                        })
                        .start();
            } else {
                // Animate the footer into view
                //animateFooterVisibility(true, 0);
            }
            ctx.postAnimationTrigger.increment();

        } else if (mConfig.launchedFromAppWithThumbnail) {
            if (/*mTask.isLaunchTarget*/false) {
                // Animate the dim/overlay
                if (Constants.DebugFlags.App.EnableThumbnailAlphaOnFrontmost) {
                    // Animate the thumbnail alpha before the dim animation (to prevent updating the
                    // hardware layer)
                    mThumbnailView.startEnterRecentsAnimation(mConfig.taskBarEnterAnimDelay,
                            new Runnable() {
                                @Override
                                public void run() {
                                    animateDimToProgress(0, mConfig.taskBarEnterAnimDuration,
                                            ctx.postAnimationTrigger.decrementOnAnimationEnd());
                                }
                            });
                } else {
                    // Immediately start the dim animation
                    animateDimToProgress(mConfig.taskBarEnterAnimDelay,
                            mConfig.taskBarEnterAnimDuration,
                            ctx.postAnimationTrigger.decrementOnAnimationEnd());
                }
                ctx.postAnimationTrigger.increment();

                // Animate the footer into view
                //animateFooterVisibility(true, mConfig.taskBarEnterAnimDuration);

                // Animate the action button in
//                mActionButtonView.animate().alpha(1f)
//                        .setStartDelay(mConfig.taskBarEnterAnimDelay)
//                        .setDuration(mConfig.taskBarEnterAnimDuration)
//                        .setInterpolator(mConfig.fastOutLinearInInterpolator)
//                        .withLayer()
//                        .start();
            } else {
                // Animate the task up if it was occluding the launch target
                if (ctx.currentTaskOccludesLaunchTarget) {
                    setTranslationY(transform.translationY + mConfig.taskViewAffiliateGroupEnterOffsetPx);
                    setAlpha(0f);
                    animate().setListener(null);
                    animate().alpha(1f)
                            .translationY(transform.translationY)
                            .setStartDelay(mConfig.taskBarEnterAnimDelay)
                            //.setUpdateListener(null)
                            .setInterpolator(mConfig.fastOutSlowInInterpolator)
                            .setDuration(mConfig.taskViewEnterFromHomeDuration)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    // Decrement the post animation trigger
                                    ctx.postAnimationTrigger.decrement();
                                }
                            })
                            .start();
                    ctx.postAnimationTrigger.increment();
                }
            }
            startDelay = mConfig.taskBarEnterAnimDelay;

        } else if (mConfig.launchedFromHome) {
            // Animate the tasks up
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex - 1);
            int delay = mConfig.taskViewEnterFromHomeDelay +
                    frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay;

            setScaleX(transform.scale);
            setScaleY(transform.scale);
            if (!mConfig.fakeShadows) {
                //animate().translationZ(transform.translationZ);
            }
            //animate().setListener((AnimatorListener) ctx.updateListener);
            animate()
                    .translationY(transform.translationY)
                    .setStartDelay(delay)
                    //.setUpdateListener(ctx.updateListener)
                    .setInterpolator(mConfig.quintOutInterpolator)
                    .setDuration(mConfig.taskViewEnterFromHomeDuration +
                            frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            // Decrement the post animation trigger
                            ctx.postAnimationTrigger.decrement();
                        }
                    })
                    .start();
            ctx.postAnimationTrigger.increment();

            // Animate the footer into view
            //animateFooterVisibility(true, mConfig.taskViewEnterFromHomeDuration);
            startDelay = delay;

        } else {
            // Animate the footer into view
            //animateFooterVisibility(true, 0);
        }

        // Enable the focus animations from this point onwards so that they aren't affected by the
        // window transitions
        postDelayed(new Runnable() {
            @Override
            public void run() {
                //enableFocusAnimations();
            }
        }, (startDelay / 2));
    }
    
    /** Sets the current task progress. */
    public void setTaskProgress(float p) {
        mTaskProgress = p;
        //mViewBounds.setAlpha(p);
        updateDimFromTaskProgress();
    }

    /** Returns the current task progress. */
    public float getTaskProgress() {
        return mTaskProgress;
    }

    /** Returns the current dim. */
    public void setDim(int dim) {
        mDim = dim;
        if (mDimAnimator != null) {
            mDimAnimator.removeAllListeners();
            mDimAnimator.cancel();
        }
        if (mConfig.useHardwareLayers) {
            // Defer setting hardware layers if we have not yet measured, or there is no dim to draw
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                if (mDimAnimator != null) {
                    mDimAnimator.removeAllListeners();
                    mDimAnimator.cancel();
                }

                int inverse = 255 - mDim;
                //mDimColorFilter.setColor(Color.argb(0xFF, inverse, inverse, inverse));
                //mLayerPaint.setColorFilter(mDimColorFilter);
                mContent.setLayerType(LAYER_TYPE_HARDWARE, mLayerPaint);
            }
        } else {
            float dimAlpha = mDim / 255.0f;
            if (mThumbnailView != null) {
                mThumbnailView.setDimAlpha(dimAlpha);
            }
//            if (mHeaderView != null) {
//                mHeaderView.setDimAlpha(dim);
//            }
        }
    }

    /** Returns the current dim. */
    public int getDim() {
        return mDim;
    }

    /** Animates the dim to the task progress. */
    void animateDimToProgress(int delay, int duration, AnimatorListener postAnimRunnable) {
        // Animate the dim into view as well
        int toDim = getDimFromTaskProgress();
        if (toDim != getDim()) {
            ObjectAnimator anim = ObjectAnimator.ofInt(TaskView.this, "dim", toDim);
            anim.setStartDelay(delay);
            anim.setDuration(duration);
            if (postAnimRunnable != null) {
                anim.addListener(postAnimRunnable);
            }
            anim.start();
        }
    }

    /** Compute the dim as a function of the scale of this view. */
    int getDimFromTaskProgress() {
        float dim = mMaxDimScale * mDimInterpolator.getInterpolation(1f - mTaskProgress);
        return (int) (dim * 255);
    }

    /** Update the dim as a function of the scale of this view. */
    void updateDimFromTaskProgress() {
        setDim(getDimFromTaskProgress());
    }
    
    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration) {
        updateViewPropertiesToTaskTransform(toTransform, duration, null);
    }
    
    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration,
                                ValueAnimator.AnimatorUpdateListener updateCallback) {
            // If we are a full screen view, then only update the Z to keep it in order
            // XXX: Also update/animate the dim as well
//            if (mIsFullScreenView) {
//                if (!mConfig.fakeShadows &&
//                    toTransform.hasTranslationZChangedFrom(getTranslationZ())) {
//                    setTranslationZ(toTransform.translationZ);
//                }
//                return;
//            }
            
            // Apply the transform
            toTransform.applyToTaskView(this, duration, mConfig.fastOutSlowInInterpolator, false,
            !mConfig.fakeShadows, updateCallback);
            
            // Update the task progress
            if (mTaskProgressAnimator != null) {
                mTaskProgressAnimator.removeAllListeners();
                mTaskProgressAnimator.cancel();
            }
            if (duration <= 0) {
                setTaskProgress(toTransform.p);
            } else {
                mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", toTransform.p);
                mTaskProgressAnimator.setDuration(duration);
                mTaskProgressAnimator.addUpdateListener(mUpdateDimListener);
                mTaskProgressAnimator.start();
            }
    }
    
}
