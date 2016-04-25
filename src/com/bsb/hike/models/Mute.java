package com.bsb.hike.models;

import android.content.Context;

import com.bsb.hike.R;

/**
 * This class contains the Mute object which is used by the ConvInfo object.
 *
 * Created by anubansal on 13/04/16.
 */
public class Mute
{
    private String msisdn;

    private boolean isMute = false;

    private boolean muteNotification = false;

    private int muteDuration = 0;

    private int muteEndTime;

    public static final int MUTE_DURATION_EIGHT_HOURS = 0;

    public static final int MUTE_DURATION_ONE_WEEK = 1;

    public static final int MUTE_DURATION_ONE_YEAR = 2;

    private Mute(InitBuilder<?> builder)
    {
        this.msisdn = builder.msisdn;
        this.isMute = builder.isMute;
        this.muteNotification = builder.muteNotification;
        this.muteDuration = builder.muteDuration;
    }

    public String getMsisdn()
    {
        return msisdn;
    }

    public void setMsisdn(String msisdn)
    {
        this.msisdn = msisdn;
    }

    public boolean isMute()
    {
        return isMute;
    }

    public void setIsMute(boolean isMute)
    {
        this.isMute = isMute;
    }

    public boolean isNotificationMuted()
    {
        return muteNotification;
    }

    public void setNotificationMuted(boolean muteNotification)
    {
        this.muteNotification = muteNotification;
    }

    public int getMuteDuration()
    {
        return muteDuration;
    }

    public void setMuteDuration(int muteDuration)
    {
        this.muteDuration = muteDuration;
    }

    public int getMuteEndTime()
    {
        return muteEndTime;
    }

    public static String[] getMuteDurationsList(Context context)
    {
        String[] muteDurationsList = {context.getString(R.string.mute_chat_eight_hrs),
                context.getString(R.string.mute_chat_one_week), context.getString(R.string.mute_chat_one_yr)};

        return muteDurationsList;
    }

    private static abstract class InitBuilder<P extends InitBuilder<P>>
    {

        private String msisdn;

        private boolean isMute;

        private boolean muteNotification;

        private int muteDuration;

        protected InitBuilder(String msisdn)
        {
            this.msisdn = msisdn;
        }

        protected abstract P getSelfObject();

        public P setIsMute(boolean isMute)
        {
            this.isMute = isMute;
            return getSelfObject();
        }

        public P setMuteNotification(boolean muteNotification)
        {
            this.muteNotification = muteNotification;
            return getSelfObject();
        }

        public P setMuteDuration(int muteDuration)
        {
            this.muteDuration = muteDuration;
            return getSelfObject();
        }

        public Mute build()
        {
            return new Mute(this);
        }
    }

    public static class MuteBuilder extends InitBuilder<MuteBuilder>
    {

        public MuteBuilder(String msisdn)
        {
            super(msisdn);
        }

        protected MuteBuilder getSelfObject()
        {
            return this;
        }
    }
}
