package com.bsb.hike.timeline.tasks;

import android.support.annotation.NonNull;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by atul on 25/05/16.
 */
public class StatusReadDBRunnable implements Runnable {

    private WeakReference<ViewPositionVerifier> mVerifierRef;

    public interface ViewPositionVerifier {
        boolean isViewVisible(int viewPos);
    }

    private final List<String> mSuIdList;

    private int viewPosn = -1;

    public StatusReadDBRunnable(List<String> suIDsList) {
        mSuIdList = suIDsList;
    }

    private final String TAG = "MarkSURead";

    public void setViewPosn(int viewPosn, @NonNull ViewPositionVerifier verifier) {
        this.viewPosn = viewPosn;
        mVerifierRef = new WeakReference<ViewPositionVerifier>(verifier);
        Logger.d(TAG, viewPosn + " added to waiting list");
    }

    public int getViewPosn() {
        return viewPosn;
    }

    @Override
    public void run() {
        if (!Utils.isEmpty(mSuIdList)) {
            if (viewPosn != -1) {
                Logger.d(TAG, viewPosn + " executing");
                ViewPositionVerifier verifier = mVerifierRef.get();
                if (verifier != null && verifier.isViewVisible(viewPosn)) {
                    Logger.d(TAG, viewPosn + " visible. marking as read");
                    markRead();
                } else {
                    Logger.d(TAG, viewPosn + " not visible. removing from list");
                }
            } else {
                markRead();
            }
        }

        StatusReadDBManager.getInstance().setFinished(this);
    }

    private void markRead() {
        int changesMade = HikeConversationsDatabase.getInstance().markStatusAsRead(mSuIdList);

        if (changesMade > 0) {
            HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.STATUS_MARKED_READ, mSuIdList);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StatusReadDBRunnable) {
            StatusReadDBRunnable compareRunnable = (StatusReadDBRunnable) o;
            if (compareRunnable.getViewPosn() == getViewPosn()) {
                return true;
            } else {
                return false;
            }
        }
        return super.equals(o);
    }
}
