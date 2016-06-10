package com.bsb.hike.timeline;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.WorkerThread;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by atul on 01/06/16.
 */
public class TimelineUtils {

    public static final String KEY_LAST_SEEN_SUID = "last_seen_su_pref";

    @WorkerThread //Multiple prefs and DB calls
    public static String getTimelineSubText() {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0); // To support old code
        int newUpdates = getUnseenStatusCount(); // no. of updates as per stealth mode
        int newLikes = HikeConversationsDatabase.getInstance().getUnreadActivityFeedCount(true); // no. of loves
        if (newUpdates > 0) {
            return context.getString(R.string.timeline_sub_new_updt);
        } else if (newLikes > 0) {
            return context.getResources().getQuantityString(R.plurals.abbrev_new_likes, newLikes, newLikes);
        } else {
            return context.getString(R.string.timeline_sub_no_updt);
        }
    }

    private static int getLastSeenSUID() {
        return (int) HikeSharedPreferenceUtil.getInstance().getData(KEY_LAST_SEEN_SUID, 0l);
    }

    public static void updateLastSeenSUID() {
        HikeHandlerUtil.getInstance().postRunnable(new Runnable() {
            @Override
            public void run() {
                String[] friendMsisdns = HikeConversationsDatabase.getTimelineFriendsMsisdn(ContactManager.getInstance().getSelfMsisdn());
                if (Utils.isEmpty(friendMsisdns)) {
                    return;
                }

                List<StatusMessage> statusMessages = HikeConversationsDatabase.getInstance().getStatusMessages(true, 1, -1, getLastSeenSUID(), false, friendMsisdns);
                if (Utils.isEmpty(statusMessages)) {
                    return;
                }

                HikeSharedPreferenceUtil.getInstance().saveData(KEY_LAST_SEEN_SUID, statusMessages.get(0).getId());

            }
        });
    }

    public static int getUnseenStatusCount() {
        String[] friendMsisdns = HikeConversationsDatabase.getTimelineFriendsMsisdn(ContactManager.getInstance().getSelfMsisdn());
        if (Utils.isEmpty(friendMsisdns)) {
            return 0;
        }
        List<StatusMessage> statusMessages = HikeConversationsDatabase.getInstance().getStatusMessages(true, 1, -1, getLastSeenSUID(), true, friendMsisdns);
        if (Utils.isEmpty(statusMessages)) {
            return 0;
        }

        return statusMessages.size();
    }

}
