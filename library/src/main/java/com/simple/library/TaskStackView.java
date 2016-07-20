package com.simple.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import com.example.testtaskviews.Task.TaskKey;

import java.util.ArrayList;
import java.util.HashMap;

public class TaskStackView extends FrameLayout implements TaskStackViewScroller.TaskStackViewScrollerCallbacks{

    RecentsConfiguration mConfig;
    TaskStackViewLayoutAlgorithm mLayoutAlgorithm;
    LayoutInflater mInflater;
    TaskStackViewScroller mStackScroller;
    TaskStackViewTouchHandler mTouchHandler;
    Rect mTaskStackBounds = new Rect();
    Rect mTmpRect = new Rect();
    boolean mAwaitingFirstLayout = true;
    final int MAX_SIZE = 10; 
    boolean mStackViewsDirty = true;
    int mStackViewsAnimationDuration;
    ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<TaskViewTransform>();
    int[] mTmpVisibleRange = new int[2];
    HashMap<Task, TaskView> mTmpTaskViewMap = new HashMap<Task, TaskView>();
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    
    ArrayList<Task> mTasks;
    ViewAnimation.TaskViewEnterContext mStartEnterAnimationContext;
    
    ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            requestUpdateStackViewsClip();
        }
    };
    
    void requestUpdateStackViewsClip() {
        //if (!mStackViewsClipDirty) {
            invalidate();
        //    mStackViewsClipDirty = true;
        //}
    }
    
    /** The stack insets to apply to the stack contents */
    public void setStackInsetRect(Rect r) {
        mTaskStackBounds.set(r);
    }
    
    public TaskStackView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        mConfig = RecentsConfiguration.getInstance();
        mInflater = LayoutInflater.from(context);
        mLayoutAlgorithm = new TaskStackViewLayoutAlgorithm(mConfig);
        mStackScroller = new TaskStackViewScroller(context, mConfig, mLayoutAlgorithm);
        mStackScroller.setCallbacks(this);
        mTouchHandler = new TaskStackViewTouchHandler(context, this, mConfig, mStackScroller);
        
        int[] cs = {0xff000000,0xffff0000,0xffff00ff,0xfffff000,0xff0000ff,0xff000000,0xffff0000,0xffff00ff,0xfffff000,0xff0000ff};
        mTasks = new ArrayList<Task>();
        for(int i = 0; i < MAX_SIZE; i++){
            TaskView tv = createView(context);
            TaskViewThumbnail th = (TaskViewThumbnail)tv.findViewById(R.id.task_view_thumbnail);
            th.setBackgroundColor(cs[i]);
            this.addView(tv);
            Task c = new Task();
            TaskKey key = new TaskKey(i, new Intent(), i, 0, 0);
            c.key = key;
            mTasks.add(c);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHandler.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mTouchHandler.onTouchEvent(ev);
    }
    
    @Override
    public void computeScroll() {
        mStackScroller.computeScroll();
        // Synchronize the views
        synchronizeStackViewsWithModel();
        //clipTaskViews();
        // Notify accessibility
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED);
    }
    
    /** Updates the min and max virtual scroll bounds */
    void updateMinMaxScroll(boolean boundScrollToNewMinMax, boolean launchedWithAltTab,
            boolean launchedFromHome) {
        // Compute the min and max scroll values
        mLayoutAlgorithm.computeMinMaxScroll(mTasks, launchedWithAltTab, launchedFromHome);

        // Debug logging
        if (boundScrollToNewMinMax) {
            mStackScroller.boundScroll();
        }
    }
    
    /** Computes the stack and task rects */
    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds,
            boolean launchedWithAltTab, boolean launchedFromHome) {
        // Compute the rects in the stack algorithm
        mLayoutAlgorithm.computeRects(windowWidth, windowHeight, taskStackBounds);

        // Update the scroll bounds
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Compute our stack/task rects
        Rect taskStackBounds = new Rect(mTaskStackBounds);
        taskStackBounds.bottom -= mConfig.systemInsets.bottom;
        computeRects(width, height, taskStackBounds, mConfig.launchedWithAltTab,
                mConfig.launchedFromHome);
        
        // If this is the first layout, then scroll to the front of the stack and synchronize the
        // stack views immediately to load all the views
        if (mAwaitingFirstLayout) {
            mStackScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }
        
        // Measure each of the TaskViews
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            if (tv.isFullScreenView()) {
                tv.measure(widthMeasureSpec, heightMeasureSpec);
            } else {
                if (tv.getBackground() != null) {
                    tv.getBackground().getPadding(mTmpRect);
                } else {
                    mTmpRect.setEmpty();
                }
                tv.measure(
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mTaskRect.width() + mTmpRect.left + mTmpRect.right,
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mTaskRect.height() + mTmpRect.top + mTmpRect.bottom +
                            /*tv.getMaxFooterHeight()*/0, MeasureSpec.EXACTLY));
            }
        }
        
        setMeasuredDimension(width, height);
    }
    
    /**
     * This is called with the size of the space not including the top or right insets, or the
     * search bar height in portrait (but including the search bar width in landscape, since we want
     * to draw under it.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Layout each of the children
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            if (tv.isFullScreenView()) {
                tv.layout(left, top, left + tv.getMeasuredWidth(), top + tv.getMeasuredHeight());
            } else {
                if (tv.getBackground() != null) {
                    tv.getBackground().getPadding(mTmpRect);
                } else {
                    mTmpRect.setEmpty();
                }
                tv.layout(mLayoutAlgorithm.mTaskRect.left - mTmpRect.left,
                        mLayoutAlgorithm.mTaskRect.top - mTmpRect.top,
                        mLayoutAlgorithm.mTaskRect.right + mTmpRect.right,
                        mLayoutAlgorithm.mTaskRect.bottom + mTmpRect.bottom +
                                /*tv.getMaxFooterHeight()*/0);
            }
        }

        if (mAwaitingFirstLayout) {
            mAwaitingFirstLayout = false;
            onFirstLayout();
        }
    }
    
    void onFirstLayout() {
        int offscreenY = mLayoutAlgorithm.mViewRect.bottom -
                (mLayoutAlgorithm.mTaskRect.top - mLayoutAlgorithm.mViewRect.top);
		int childCount = getChildCount();
        // Prepare the first view for its enter animation
        for (int i = childCount - 1; i >= 0; i--) {
            TaskView tv = (TaskView) getChildAt(i);
            boolean occludesLaunchTarget = true;
            tv.prepareEnterRecentsAnimation(false, occludesLaunchTarget, offscreenY);
        }
        
        startEnterRecentsAnimation(mStartEnterAnimationContext);
    }
    
    
    public void startEnterRecentsAnimation(ViewAnimation.TaskViewEnterContext ctx) {
           if (mAwaitingFirstLayout) {
            //mStartEnterAnimationRequestedAfterLayout = true;
            mStartEnterAnimationContext = ctx;
            return;
        }
        int childCount = getChildCount();
        // Animate all the task views into view
        for (int i = childCount - 1; i >= 0; i--) {
            TaskView tv = (TaskView) getChildAt(i);
            Task task = mTasks.get(i);
            ctx.currentTaskTransform = new TaskViewTransform();
            ctx.currentStackViewIndex = i;
            ctx.currentStackViewCount = childCount;
            ctx.currentTaskRect = mLayoutAlgorithm.mTaskRect;
            ctx.currentTaskOccludesLaunchTarget = true;//(launchTargetTask != null) &&
                    //launchTargetTask.group.isTaskAboveTask(task, launchTargetTask);
            ctx.updateListener = mRequestUpdateClippingListener;
            mLayoutAlgorithm.getStackTransform(task, mStackScroller.getStackScroll(), ctx.currentTaskTransform, null);
            tv.startEnterRecentsAnimation(ctx);
        }
        
        
    }
    public TaskView createView(Context context) {
        return (TaskView) mInflater.inflate(R.layout.recents_task_view, this, false);
    }
    
    public boolean isTransformedTouchPointInView(float x, float y, View child) {
        return false;//isTransformedTouchPointInView(x, y, child, null);
    }

    @Override
    public void onScrollChanged(float p) {
        // TODO Auto-generated method stub
        requestSynchronizeStackViewsWithModel();
        postInvalidateOnAnimation();
    }
    
    void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }
    
    
    /** Synchronizes the views with the model */
    boolean synchronizeStackViewsWithModel() {
        if (mStackViewsDirty) {
            //RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
            //SystemServicesProxy ssp = loader.getSystemServicesProxy();

            // Get all the task transforms
            ArrayList<Task> tasks = mTasks;
            float stackScroll = mStackScroller.getStackScroll();
            int[] visibleRange = mTmpVisibleRange;
            boolean isValidVisibleRange = updateStackTransforms(mCurrentTaskTransforms, tasks,
                    stackScroll, visibleRange, false);

            Log.d("yangkai taskView","stackScroll = "+stackScroll);
            
            Log.d("yangkai taskView","visibleRange = "+visibleRange[0]+" "+visibleRange[1]);
            // Return all the invisible children to the pool
            mTmpTaskViewMap.clear();
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                TaskView tv = (TaskView) getChildAt(i);
                Task task = mTasks.get(i);//tv.getTask();
                int taskIndex = i;//mStack.indexOfTask(task);
                if (visibleRange[1] <= taskIndex && taskIndex <= visibleRange[0]) {
                    mTmpTaskViewMap.put(task, tv);
                } else {
                    //mViewPool.returnViewToPool(tv);
                }
                TaskViewTransform transform = mCurrentTaskTransforms.get(i);
                if (i < visibleRange[0] || i >= visibleRange[1]){
                    if (Float.compare(transform.p, 0f) <= 0) {
                      //mLayoutAlgorithm.getStackTransform(0f, 0f, mTmpTransform, null);
                  } else {
                      mLayoutAlgorithm.getStackTransform(1f, 0f, mTmpTransform, null);
                  }
                  tv.updateViewPropertiesToTaskTransform(mTmpTransform, 0);
                }
            }

            // Pick up all the newly visible children and update all the existing children
            for (int i = visibleRange[0]; isValidVisibleRange && i >= visibleRange[1]; i--) {
                Task task = tasks.get(i);
                TaskViewTransform transform = mCurrentTaskTransforms.get(i);
                TaskView tv = mTmpTaskViewMap.get(task);
                int taskIndex = i;//mStack.indexOfTask(task);

//                if (tv == null) {
//                    tv = mViewPool.pickUpViewFromPool(task, task);
//
//                    if (mStackViewsAnimationDuration > 0) {
//                        // For items in the list, put them in start animating them from the
//                        // approriate ends of the list where they are expected to appear
//                        if (Float.compare(transform.p, 0f) <= 0) {
//                            mLayoutAlgorithm.getStackTransform(0f, 0f, mTmpTransform, null);
//                        } else {
//                            mLayoutAlgorithm.getStackTransform(1f, 0f, mTmpTransform, null);
//                        }
//                        tv.updateViewPropertiesToTaskTransform(mTmpTransform, 0);
//                    }
//                }

                // Animate the task into place
                tv.updateViewPropertiesToTaskTransform(mCurrentTaskTransforms.get(taskIndex),
                        mStackViewsAnimationDuration, mRequestUpdateClippingListener);

                // Request accessibility focus on the next view if we removed the task
                // that previously held accessibility focus
                childCount = getChildCount();
//                if (childCount > 0 && ssp.isTouchExplorationEnabled()) {
//                    TaskView atv = (TaskView) getChildAt(childCount - 1);
//                    int indexOfTask = mStack.indexOfTask(atv.getTask());
//                    if (mPrevAccessibilityFocusedIndex != indexOfTask) {
//                        tv.requestAccessibilityFocus();
//                        mPrevAccessibilityFocusedIndex = indexOfTask;
//                    }
//                }
            }

            // Reset the request-synchronize params
            mStackViewsAnimationDuration = 0;
            mStackViewsDirty = false;
            //mStackViewsClipDirty = true;
            return true;
        }
        return false;
    }
    void requestSynchronizeStackViewsWithModel(int duration) {
        if (!mStackViewsDirty) {
            invalidate();
            mStackViewsDirty = true;
        }
        if (mAwaitingFirstLayout) {
            // Skip the animation if we are awaiting first layout
            mStackViewsAnimationDuration = 0;
        } else {
            mStackViewsAnimationDuration = Math.max(mStackViewsAnimationDuration, duration);
        }
    }
    
    /**
     * Gets the stack transforms of a list of tasks, and returns the visible range of tasks.
     */
    private boolean updateStackTransforms(ArrayList<TaskViewTransform> taskTransforms,
                                       ArrayList<Task> tasks,
                                       float stackScroll,
                                       int[] visibleRangeOut,
                                       boolean boundTranslationsToRect) {
        // XXX: We should be intelligent about where to look for the visible stack range using the
        //      current stack scroll.
        // XXX: We should log extra cases like the ones below where we don't expect to hit very often
        // XXX: Print out approximately how many indices we have to go through to find the first visible transform

        int taskTransformCount = taskTransforms.size();
        int taskCount = tasks.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;

        // We can reuse the task transforms where possible to reduce object allocation
        if (taskTransformCount < taskCount) {
            // If there are less transforms than tasks, then add as many transforms as necessary
            for (int i = taskTransformCount; i < taskCount; i++) {
                taskTransforms.add(new TaskViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            // If there are more transforms than tasks, then just subset the transform list
            taskTransforms.subList(0, taskCount);
        }

        // Update the stack transforms
        TaskViewTransform prevTransform = null;
        for (int i = taskCount - 1; i >= 0; i--) {
            TaskViewTransform transform = mLayoutAlgorithm.getStackTransform(tasks.get(i),
                    stackScroll, taskTransforms.get(i), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i;
                }
                backMostVisibleIndex = i;
            } else {
                if (backMostVisibleIndex != -1) {
                    // We've reached the end of the visible range, so going down the rest of the
                    // stack, we can just reset the transforms accordingly
                    while (i >= 0) {
                        taskTransforms.get(i).reset();
                        i--;
                    }
                    break;
                }
            }

            if (boundTranslationsToRect) {
                transform.translationY = Math.min(transform.translationY,
                        mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        return frontMostVisibleIndex != -1 && backMostVisibleIndex != -1;
    }
}
