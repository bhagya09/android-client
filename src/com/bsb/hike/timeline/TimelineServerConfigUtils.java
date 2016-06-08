package com.bsb.hike.timeline;

import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by atul on 08/06/16.
 */
public class TimelineServerConfigUtils {
    public static final String AC_KEY_STORY_DURATION = "stryTimeLmtSeconds";

    public static long getStoryTimeLimit() {
        return HikeSharedPreferenceUtil.getInstance().getData(AC_KEY_STORY_DURATION, TimeUnit.HOURS.toSeconds(24));
    }
}
