
package com.simple.library;

/**
 * Constants
 */
public class Constants {
    public static class DebugFlags {
        public static final boolean Verbose = false;

        public static class App {
            public static final boolean EnableScreenshotAppTransition = false;
            public static final boolean EnableTransitionThumbnailDebugMode = false;
            public static final boolean EnableTaskFiltering = false;
            public static final boolean EnableTaskStackClipping = true;
            public static final boolean EnableTaskBarTouchEvents = true;
            public static final boolean EnableDevAppInfoOnLongPress = true;
            public static final boolean EnableDebugMode = false;
            public static final boolean EnableSearchLayout = true;
            public static final boolean EnableThumbnailAlphaOnFrontmost = false;
            public static final boolean DisableBackgroundCache = false;
            public static final boolean EnableSimulatedTaskGroups = false;
            public static final int TaskAffiliationsGroupCount = 12;
            public static final boolean EnableSystemServicesProxy = false;
            public static final int SystemServicesProxyMockPackageCount = 3;
            public static final int SystemServicesProxyMockTaskCount = 100;
        }
    }

    public static class Values {
        public static class App {
            public static int AppWidgetHostId = 1024;
            public static String Key_SearchAppWidgetId = "searchAppWidgetId";
            public static String Key_DebugModeEnabled = "debugModeEnabled";
            public static String DebugModeVersion = "A";
        }

        public static class RecentsTaskLoader {
            public static final int PreloadFirstTasksCount = 6;
        }

        public static class TaskStackView {
            public static final int TaskStackOverscrollRange = 150;
            public static final int FilterStartDelay = 25;
        }
    }
}
