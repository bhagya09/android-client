package com.bsb.hike.productpopup;

import com.bsb.hike.HikeConstants;

import org.json.JSONObject;

/**
 * @author paramshah
 */
public class AtomicTipContentModel
{
    private String header;

    private String body;

    private String icon;

    private String bgColor;

    private int priority;

    private long startTime;

    private long endTime;

    private boolean isSilent;

    private String notifTitle;

    private String notifText;

    private String ctaLink;

    private String ctaAction;

    private String ctaName;

    private String jsonString;

    private int hashCode = -1;

    private AtomicTipContentModel(JSONObject tipContentJSON)
    {
        this.header = tipContentJSON.optString(HikeConstants.HEADER, "");
        this.body = tipContentJSON.optString(HikeConstants.BODY, "");
        this.icon = tipContentJSON.optString(HikeConstants.ICON, "");
        this.bgColor = tipContentJSON.optString(HikeConstants.BACKGROUND_COLOR,"");
        this.priority = tipContentJSON.optInt(HikeConstants.TIP_PRIORITY);
        this.startTime = tipContentJSON.optLong(ProductPopupsConstants.START_TIME, 0L);
        this.endTime = tipContentJSON.optLong(ProductPopupsConstants.END_TIME, 0L);
        processNotifItems(tipContentJSON.optJSONObject(HikeConstants.PLAY_NOTIFICATION));
        processTipCTA(tipContentJSON.optJSONObject(HikeConstants.TIP_CTA));
        this.jsonString = tipContentJSON.toString();
        this.hashCode();
    }

    private void processNotifItems(JSONObject tipNotifData)
    {
        if(tipNotifData == null)
        {
            this.isSilent = true;
            return;
        }

        this.isSilent = tipNotifData.optBoolean(HikeConstants.SILENT, true);

        if(!isSilent)
        {
            this.notifTitle = tipNotifData.optString(HikeConstants.NOTIFICATION_TITLE);
            this.notifText = tipNotifData.optString(HikeConstants.NOTIFICATION_TEXT);
        }
    }

    private void processTipCTA(JSONObject tipCTAData)
    {
        if(tipCTAData != null)
        {
            this.ctaLink = tipCTAData.optJSONObject(HikeConstants.TIP_CTA_LINK).toString();
            this.ctaAction = tipCTAData.optString(HikeConstants.MqttMessageTypes.ACTION, "");
            this.ctaName = tipCTAData.optString(HikeConstants.NAME, "");
        }
    }

    public AtomicTipContentModel getAtomicTipContentModel(JSONObject tipContentData)
    {
        return new AtomicTipContentModel(tipContentData);
    }

    @Override
    public int hashCode()
    {
        if(hashCode == -1)
        {
            hashCode = new String(getStartTime() + getEndTime() + getPriority() + getHeader() + "").hashCode();
        }
        return hashCode;
    }

    public boolean isSilent()
    {
        return isSilent;
    }

    public String getHeader()
    {
        return header;
    }

    public String getBody()
    {
        return body;
    }

    public String getIcon()
    {
        return icon;
    }

    public String getNotifTitle()
    {
        return notifTitle;
    }

    public String getNotifText()
    {
        return notifText;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public String getCtaLink()
    {
        return ctaLink;
    }

    public String getCtaAction()
    {
        return ctaAction;
    }

    public String getCtaName()
    {
        return ctaName;
    }

    public String getBgColor()
    {
        return bgColor;
    }

    public int getPriority()
    {
        return priority;
    }

    public String getJsonString()
    {
        return jsonString;
    }
}
