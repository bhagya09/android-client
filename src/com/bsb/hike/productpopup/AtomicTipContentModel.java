package com.bsb.hike.productpopup;

import com.bsb.hike.HikeConstants;

import org.json.JSONObject;

import java.util.Comparator;

/**
 * @author paramshah
 */
public class AtomicTipContentModel
{
    private String header;

    private String body;

    private String icon;

    private String bgColor;

    private String bgImage;

    private boolean isBgColor;

    private String iconKey;

    private String bgImgKey;

    private int priority;

    private long startTime;

    private long endTime;

    private boolean isSilent;

    private String notifTitle;

    private String notifText;

    private String ctaLink;

    private String ctaAction;

    private String jsonString;

    private int tipStatus;

    private int hashCode = -1;

    public enum AtomicTipStatus
    {
        UNSEEN(0),
        SEEN(1),
        DISMISSED(2);

        private int value;

        AtomicTipStatus(int val)
        {
            value = val;
        }

        public int getValue()
        {
            return value;
        }
    }

    private AtomicTipContentModel(JSONObject tipContentJSON)
    {
        this.header = tipContentJSON.optString(HikeConstants.HEADER, "");
        this.body = tipContentJSON.optString(HikeConstants.BODY, "");
        this.icon = tipContentJSON.optString(HikeConstants.ICON, "");
        this.priority = tipContentJSON.optInt(HikeConstants.TIP_PRIORITY);
        this.startTime = tipContentJSON.optLong(ProductPopupsConstants.START_TIME, 0L);
        this.endTime = tipContentJSON.optLong(ProductPopupsConstants.END_TIME, 0L);
        prcessBackground(tipContentJSON.optJSONObject(HikeConstants.BACKGROUND));
        processNotifItems(tipContentJSON.optJSONObject(HikeConstants.PLAY_NOTIFICATION));
        processTipCTA(tipContentJSON.optJSONObject(HikeConstants.TIP_CTA));
        this.jsonString = tipContentJSON.toString();
        this.hashCode();
        iconKey = String.format(hashCode + "icon");
        bgImgKey = String.format(hashCode + "bgimg");
    }

    private void prcessBackground(JSONObject tipBgData)
    {
        if(tipBgData == null)
        {
            return;
        }

        if(tipBgData.has(HikeConstants.BACKGROUND_COLOR))
        {
            this.bgColor = tipBgData.optString(HikeConstants.BACKGROUND_COLOR,"");
            isBgColor = true;
        }
        else
        {
            isBgColor = false;
            this.bgImage = tipBgData.optString(HikeConstants.IMAGE,"");
        }
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
        }
    }

    public static AtomicTipContentModel getAtomicTipContentModel(JSONObject tipContentData)
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

    public int getTipStatus()
    {
        return tipStatus;
    }

    public void setTipStatus(int status)
    {
        tipStatus = status;
    }

    public String getBgImage()
    {
        return bgImage;
    }

    public boolean isBgColor()
    {
        return isBgColor;
    }

    public String getIconKey()
    {
        return iconKey;
    }

    public String getBgImgKey()
    {
        return bgImgKey;
    }

    public static Comparator<AtomicTipContentModel> tipsComparator = new Comparator<AtomicTipContentModel>()
    {
        @Override
        public int compare(AtomicTipContentModel lhs, AtomicTipContentModel rhs)
        {
            int statusComp = Integer.valueOf(lhs.getTipStatus()).compareTo(rhs.getTipStatus());
            if(statusComp != 0)
            {
                return statusComp;
            }
            else
            {
                return Integer.valueOf(lhs.getPriority()).compareTo(rhs.getPriority());
            }
        }
    };
}
