package com.simple.materialtaskview;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class RecentsView extends FrameLayout{

    RecentsConfiguration mConfig;
    LayoutInflater mInflater;
    View mSearchBar = null;
    ReferenceCountedTrigger t;
    
    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        // TODO Auto-generated constructor stub
    }

    public RecentsView(Context context) {
        this(context, null);
        // TODO Auto-generated constructor stub
    }
    
    public RecentsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mConfig = RecentsConfiguration.getInstance();
        mInflater = LayoutInflater.from(context);
        
    }
    
    public void setTaskStacks(){
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View v = getChildAt(i);
            removeViewAt(i);
        }
        int numStacks = 1;//mStacks.size();
        if (t == null){
            t = new ReferenceCountedTrigger(this.getContext(), null, null, null);
        }
        
        for (int i = 0; i < numStacks; i++) {
            //TaskStack stack = mStacks.get(i);
            TaskStackView stackView = new TaskStackView(getContext()/*, stack*/);
            stackView.startEnterRecentsAnimation(new ViewAnimation.TaskViewEnterContext(t));
            //stackView.setCallbacks(this);
            // Enable debug mode drawing
            addView(stackView);
        }
    }
    
    public boolean hasSearchBar() {
        return false;//mSearchBar != null;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect taskStackBounds = new Rect();
        mConfig.getTaskStackBounds(width, height, mConfig.systemInsets.top,
                mConfig.systemInsets.right, taskStackBounds);

        // Measure each TaskStackView with the full width and height of the window since the 
        // transition view is a child of that stack view
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != mSearchBar && child.getVisibility() != GONE) {
                TaskStackView tsv = (TaskStackView) child;
                // Set the insets to be the top/left inset + search bounds
                tsv.setStackInsetRect(taskStackBounds);
                tsv.measure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != mSearchBar && child.getVisibility() != GONE) {
                child.layout(left, top, left + child.getMeasuredWidth(),
                        top + child.getMeasuredHeight());
            }
        }
    }
    
//    @Override
//    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
//        // Update the configuration with the latest system insets and trigger a relayout
//        mConfig.updateSystemInsets(insets.getSystemWindowInsets());
//        requestLayout();
//        return insets.consumeSystemWindowInsets();
//    }
}
