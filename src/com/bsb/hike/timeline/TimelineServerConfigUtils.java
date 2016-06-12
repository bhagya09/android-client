package com.bsb.hike.timeline;

import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by atul on 08/06/16.
 */
public class TimelineServerConfigUtils {
    public static final String AC_KEY_STORY_DURATION = "stryTimeLmtSeconds";

    public static final String AC_KEY_CAMSHY_SUBTEXT = "stryCamShyStr";

    public static final String AC_KEY_CAMSHY_ENABLED = "stryCamShyEn";

    public static long getStoryTimeLimit() {
        return HikeSharedPreferenceUtil.getInstance().getData(AC_KEY_STORY_DURATION, TimeUnit.HOURS.toSeconds(24));
    }

    public static Set<String> getCameraShySubtext() {
        return HikeSharedPreferenceUtil.getInstance().getStringSet(AC_KEY_CAMSHY_SUBTEXT, new HashSet<String>());
    }

    public static boolean isCameraShyEnabled() {
        return HikeSharedPreferenceUtil.getInstance().getData(AC_KEY_CAMSHY_ENABLED, true);
    }
}
