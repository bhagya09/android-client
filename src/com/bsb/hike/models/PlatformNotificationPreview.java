package com.bsb.hike.models;

/**
 * This class uses the builder pattern to construct model objects for
 * {@link com.bsb.hike.notifications.platformNotifications.PlatformNotificationMsgStack)
 */
public class PlatformNotificationPreview
{
    private Boolean clubByMsisdn;
    private String msisdn;
    private String title;
    private String body;
    private PlatformNotificationPreviewBuilder builder;

    private PlatformNotificationPreview(PlatformNotificationPreviewBuilder builder)
    {
        this.builder=builder;
        init();
    }
    public void init()
    {
        clubByMsisdn=builder.clubByMsisdn;
        msisdn=builder.msisdn;
        title=builder.title;
        body=builder.body;
    }

    public Boolean getClubByMsisdn()
    {
        return clubByMsisdn;
    }

    public String getMsisdn()
    {
        return msisdn;
    }

    public String getTitle()
    {
        return title;
    }

    public String getBody()
    {
        return body;
    }

    public static class PlatformNotificationPreviewBuilder
    {
        public Boolean clubByMsisdn;
        public String msisdn;
        public String title;
        public String body;
        //These are compulsory params
        public PlatformNotificationPreviewBuilder(Boolean clubByMsisdn,String msisdn,String title,String body)
        {
            this.clubByMsisdn=clubByMsisdn;
            this.msisdn=msisdn;
            this.title=title;
            this.body=body;
        }
        //Add builder for other optionals

        public PlatformNotificationPreview build()
        {
            return new PlatformNotificationPreview(this);
        }

    }
}
