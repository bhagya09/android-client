package com.bsb.hike.timeline;

import android.content.Context;
import android.content.SharedPreferences;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

/**
 * Created by atul on 01/06/16.
 */
public class TimelineUtils {

    public static String getTimelineSubText() {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0); // To support old code
        int newUpdates = Utils.getNotificationCount(sharedPref, true, false, true, false); // no. of updates
        int newLikes = Utils.getNotificationCount(sharedPref, false, true, false, false); // no. of loves
        if (newUpdates > 0) {
            return context.getString(R.string.timeline_sub_new_updt);
        } else if (newLikes > 0) {
            if (newLikes == 1) {
                return context.getString(R.string.timeline_sub_like);
            } else {
                return String.format(context.getString(R.string.timeline_sub_likes), newLikes);
            }
        } else {
            return context.getString(R.string.timeline_sub_no_updt);
        }
    }
}
