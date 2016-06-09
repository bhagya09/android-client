package com.bsb.hike.modules.contactmgr;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.PrivacyPreferences;

public class PrivacyPreferencePersistence {

    private final HikeUserDatabase db;

    public PrivacyPreferencePersistence(HikeUserDatabase db) {
        this.db = db;
    }

    public void flushOldPrivacyValues(boolean lastSeenFlush, boolean statusUpdateFlush) {
        db.flushOldPrivacyValues(lastSeenFlush, statusUpdateFlush);
    }

    public void toggleLastSeenSetting(final ContactInfo mContactInfo, final boolean isChecked) {
        mContactInfo.getPrivacyPrefs().setLastSeen(isChecked);

        execute(new Runnable() {
            @Override
            public void run() {
                db.setLastSeenForMsisdns(mContactInfo.getMsisdn(), isChecked ? 1 : 0);
            }
        });
    }

    public void toggleStatusUpdateSetting(final ContactInfo mContactInfo, final boolean isChecked) {
        mContactInfo.getPrivacyPrefs().setStatusUpdate(isChecked);

        execute(new Runnable() {
            @Override
            public void run() {
                db.setSUSettingForMsisdns(mContactInfo.getMsisdn(), isChecked ? 1 : 0);
            }
        });
    }

    public PrivacyPreferences getPrivacyPrefsForAGivenMsisdn(String msisdn) {
        return db.getPrivacyPreferencesForAGivenMsisdn(msisdn);
    }

    public boolean shouldShowStatusUpdateForGivenMsisdn(String msisdn) {
        PrivacyPreferences privacyPreferences = db.getPrivacyPreferencesForAGivenMsisdn(msisdn);
        if (privacyPreferences == null) return false;
        return privacyPreferences.shouldShowStatusUpdate();
    }

    public void setAllLastSeenValues(final boolean newValue) {
        execute(new Runnable() {
            @Override
            public void run() {
                db.setAllLastSeenPrivacyValues(newValue);
            }
        });
    }

    protected void execute(Runnable runnable) {
        HikeHandlerUtil.getInstance().postAtFront(runnable);
    }
}