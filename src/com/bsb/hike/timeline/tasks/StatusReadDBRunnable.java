package com.bsb.hike.timeline.tasks;

import android.support.annotation.NonNull;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.timeline.TimelineUtils;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by atul on 25/05/16.
 */
public class StatusReadDBRunnable implements Runnable {

    private WeakReference<ViewPositionVerifier> mVerifierRef;

    public interface ViewPositionVerifier {
        boolean isViewVisible(int viewPos);
    }

    private final List<StatusMessage> mSUList;

    private int viewPosn = -1;

    public StatusReadDBRunnable(List<StatusMessage> suIDsList) {
        mSUList = suIDsList;
    }

    private String mSourceAnalytics;

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
        if (!Utils.isEmpty(mSUList)) {
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
        ArrayList<String> mSuIdList = new ArrayList<>();
        for (StatusMessage suMsg : mSUList) {
            mSuIdList.add(suMsg.getMappedId());
        }

        int changesMade = HikeConversationsDatabase.getInstance().markStatusAsRead(mSuIdList);

        if (changesMade > 0) {
            HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.STATUS_MARKED_READ, mSuIdList);
            logViewAnalytics();
        }
    }

    private void logViewAnalytics() {
        for (StatusMessage suMsg : mSUList) {
            try {
                JSONObject json = new JSONObject();
                json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.UK_SU_READ);
                json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
                json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
                json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
                json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.UK_SU_READ);
                json.put(AnalyticsConstants.V2.FAMILY, TimelineUtils.getAnalyticsFamilyName(suMsg));
                HAManager.getInstance().recordV2(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    public void setSourceAnalytics(String mSourceAnalytics) {
        this.mSourceAnalytics = mSourceAnalytics;
    }
}
