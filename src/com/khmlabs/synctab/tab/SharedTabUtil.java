package com.khmlabs.synctab.tab;

import java.util.List;

public class SharedTabUtil {

    public static SharedTab getRecentSharedTab(List<SharedTab> sharedTabs) {
        long max = Long.MIN_VALUE;
        SharedTab recent = null;

        for (SharedTab each : sharedTabs) {
            if (each.getTimestamp() > max) {
                max = each.getTimestamp();
                recent = each;
            }
        }

        return recent;
    }

    public static SharedTab getOldestSharedTab(List<SharedTab> sharedTabs) {
        long min = Long.MAX_VALUE;
        SharedTab recent = null;

        for (SharedTab each : sharedTabs) {
            if (each.getTimestamp() < min) {
                min = each.getTimestamp();
                recent = each;
            }
        }

        return recent;
    }
}
