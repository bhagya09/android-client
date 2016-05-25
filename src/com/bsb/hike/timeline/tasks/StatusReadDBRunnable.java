package com.bsb.hike.timeline.tasks;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by atul on 25/05/16.
 */
public class StatusReadDBRunnable implements Runnable {
    private final List<String> mSuIdList;

    public StatusReadDBRunnable(List<String> suIDsList) {
        mSuIdList = suIDsList;
    }

    @Override
    public void run() {
        if (Utils.isEmpty(mSuIdList)) {
            return;
        }

        int changesMade = HikeConversationsDatabase.getInstance().markStatusAsRead(mSuIdList);

        if (changesMade > 0) {
            HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.STATUS_MARKED_READ, changesMade);
        }
    }
}
