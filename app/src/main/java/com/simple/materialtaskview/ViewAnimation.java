package com.simple.materialtaskview;

import android.animation.ValueAnimator;
import android.graphics.Rect;


/* Common code related to view animations */
public class ViewAnimation {

    /* The animation context for a task view animation into Recents */
    public static class TaskViewEnterContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;
        // An update listener to notify as the enter animation progresses (used for the home transition)
        ValueAnimator.AnimatorUpdateListener updateListener;

        // These following properties are updated for each task view we start the enter animation on

        // Whether or not the current task occludes the launch target
        boolean currentTaskOccludesLaunchTarget;
        // The task rect for the current stack
        Rect currentTaskRect;
        // The transform of the current task view
        TaskViewTransform currentTaskTransform;
        // The view index of the current task view
        int currentStackViewIndex;
        // The total number of task views
        int currentStackViewCount;

        public TaskViewEnterContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

    /* The animation context for a task view animation out of Recents */
    public static class TaskViewExitContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;

        // The translationY to apply to a TaskView to move it off the bottom of the task stack
        int offscreenTranslationY;

        public TaskViewExitContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

}
