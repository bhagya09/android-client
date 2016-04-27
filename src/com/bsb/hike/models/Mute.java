package com.bsb.hike.models;

/**
 * This class contains the Mute object which is used by the ConvInfo object.
 *
 * Created by anubansal on 13/04/16.
 */
public class Mute
{
    private String msisdn;

    private boolean isMute = false;

    private boolean showNotification = false;

    private int muteDuration = 0;

    private int muteEndTime;

    private Mute(InitBuilder builder)
    {
        this.msisdn = builder.msisdn;
        this.isMute = builder.isMute;
        this.showNotification = builder.showNotification;
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

    public boolean shouldShowNotifInMute()
    {
        return showNotification;
    }

    public void setShowNotifInMute(boolean muteNotification)
    {
        this.showNotification = muteNotification;
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

    public static class InitBuilder
    {

        private String msisdn;

        private boolean isMute;

        private boolean showNotification;

        private int muteDuration;

        public InitBuilder(String msisdn)
        {
            this.msisdn = msisdn;
        }

        public InitBuilder setIsMute(boolean isMute)
        {
            this.isMute = isMute;
            return getSelfObject();
        }

        public InitBuilder setShowNotifInMute(boolean muteNotification)
        {
            this.showNotification = muteNotification;
            return getSelfObject();
        }

        public InitBuilder setMuteDuration(int muteDuration)
        {
            this.muteDuration = muteDuration;
            return getSelfObject();
        }

        protected InitBuilder getSelfObject()
        {
            return this;
        }

        public Mute build()
        {
            return new Mute(this);
        }
    }

}
