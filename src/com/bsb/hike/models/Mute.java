package com.bsb.hike.models;

import com.bsb.hike.HikeConstants.MuteDuration;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

/**
 * This class contains the Mute object which is used by the ConvInfo object.
 * <p/>
 * Created by anubansal on 13/04/16.
 */
public class Mute {
    private String msisdn;

    private boolean isMute = false;

    private boolean showNotification = false;

    private int muteDuration = 0;

    private long muteEndTime;

    private long muteTimestamp;

    private Mute(InitBuilder builder) {
        this.msisdn = builder.msisdn;
        this.isMute = builder.isMute;
        this.showNotification = builder.showNotification;
        this.muteDuration = builder.muteDuration;
        this.muteTimestamp = builder.muteTimestamp;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public boolean isMute() {
        return isMute;
    }

    public void setIsMute(boolean isMute) {
        this.isMute = isMute;
        if (isMute) {
            this.muteTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * @return the time when the conversation was muted in seconds
     */
    public long getMuteTimestamp() {
        return this.muteTimestamp;
    }

    public boolean shouldShowNotifInMute() {
        return showNotification;
    }

    public void setShowNotifInMute(boolean muteNotification) {
        this.showNotification = muteNotification;
    }

    public int getMuteDuration() {
        return muteDuration;
    }

    public void setMuteDuration(int muteDuration) {
        this.muteDuration = muteDuration;
    }

    public String getMuteDurationString() {
        switch (muteDuration) {
            case MuteDuration.DURATION_EIGHT_HOURS:
                return HikeMessengerApp.getInstance().getApplicationContext().getResources().getString(R.string.mute_chat_eight_hrs);
            case MuteDuration.DURATION_ONE_WEEK:
                return HikeMessengerApp.getInstance().getApplicationContext().getResources().getString(R.string.mute_chat_one_week);
            case MuteDuration.DURATION_ONE_YEAR:
                return HikeMessengerApp.getInstance().getApplicationContext().getResources().getString(R.string.mute_chat_one_yr);
            default:
                return HikeMessengerApp.getInstance().getApplicationContext().getResources().getString(R.string.mute_chat_eight_hrs);
        }
    }

    /**
     * @return muteEndTime in milliseconds for the following durations : 8 hours, 1 week, 1 year
     */
    public long getMuteEndTime() {
        switch (muteDuration) {
            case MuteDuration.DURATION_EIGHT_HOURS:
                muteEndTime = muteTimestamp + (8 * 60 * 60 * 1000);
                break;

            case MuteDuration.DURATION_ONE_WEEK:
                muteEndTime = muteTimestamp + (7 * 24 * 60 * 60 * 1000);
                break;

            case MuteDuration.DURATION_ONE_YEAR:
                muteEndTime = muteTimestamp + (365 * 24 * 60 * 60 * 1000);
                break;
        }
        return muteEndTime;
    }

    public void setMuteTimestamp(long muteTimestamp) {
        this.muteTimestamp = muteTimestamp;
    }

    public static class InitBuilder {

        private String msisdn;

        private boolean isMute;

        private boolean showNotification;

        private int muteDuration;

        private long muteTimestamp;

        public InitBuilder(String msisdn) {
            this.msisdn = msisdn;
        }

        public InitBuilder setIsMute(boolean isMute) {
            this.isMute = isMute;
            return getSelfObject();
        }

        public InitBuilder setShowNotifInMute(boolean muteNotification) {
            this.showNotification = muteNotification;
            return getSelfObject();
        }

        public InitBuilder setMuteDuration(int muteDuration) {
            this.muteDuration = muteDuration;
            return getSelfObject();
        }

        public InitBuilder setMuteTimestamp(long muteTimestamp) {
            this.muteTimestamp = muteTimestamp;
            return getSelfObject();
        }

        protected InitBuilder getSelfObject() {
            return this;
        }

        public Mute build() {
            return new Mute(this);
        }
    }

}
