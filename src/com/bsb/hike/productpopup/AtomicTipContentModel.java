package com.bsb.hike.productpopup;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.IllegalFormatException;

/**
 * @author paramshah
 */
public class AtomicTipContentModel
{
    private String tipId;

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

    private boolean isCancellable;

    private boolean isSilent;

    private boolean showNotification;

    private String notifTitle;

    private String notifText;

    private String ctaLink;

    private String ctaAction;

    private String jsonString;

    private String analyticsTag;

    private int headerTextColor;

    private int bodyTextColor;

    private boolean isCircularIcon;

    @Status private int tipStatus;

    private int hashCode = -1;

    public static final int UNSEEN = 0;

    public static final int SEEN = 1;

    public static final int DISMISSED = 2;

    @IntDef({UNSEEN, SEEN, DISMISSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status{}

    private AtomicTipContentModel(JSONObject tipContentJSON)
    {
        this.tipId = tipContentJSON.optString(HikeConstants.TIP_ID);
        this.header = processTipText(tipContentJSON.optJSONObject(HikeConstants.HEADER));
        this.headerTextColor = processTipTextColor(tipContentJSON.optJSONObject(HikeConstants.HEADER), true);
        this.body = processTipText(tipContentJSON.optJSONObject(HikeConstants.BODY));
        this.bodyTextColor = processTipTextColor(tipContentJSON.optJSONObject(HikeConstants.BODY), false);
        this.icon = tipContentJSON.optString(HikeConstants.ICON, "");
        this.isCircularIcon = tipContentJSON.optBoolean(AtomicTipManager.IS_CIRCULAR_ICON, false);
        this.priority = tipContentJSON.optInt(HikeConstants.TIP_PRIORITY);
        this.startTime = tipContentJSON.optLong(ProductPopupsConstants.START_TIME, 0L);
        this.endTime = tipContentJSON.optLong(ProductPopupsConstants.END_TIME, 0L);
        this.isCancellable = tipContentJSON.optBoolean(ProductPopupsConstants.IS_CANCELLABLE, true);
        this.analyticsTag = tipContentJSON.optString(AnalyticsConstants.EXP_ANALYTICS_TAG, AnalyticsConstants.AtomicTipsAnalyticsConstants.TIPS);
        prcessBackground(tipContentJSON.optJSONObject(HikeConstants.BACKGROUND));
        processNotifItems(tipContentJSON.optJSONObject(HikeConstants.PLAY_NOTIFICATION));
        processTipCTA(tipContentJSON.optJSONObject(HikeConstants.TIP_CTA));
        this.jsonString = tipContentJSON.toString();
        this.hashCode();
        iconKey = String.format(hashCode + "icon");
        bgImgKey = String.format(hashCode + "bgimg");
    }

    private String processTipText(JSONObject tipTextData)
    {
        String text = "";
        if(tipTextData == null)
        {
            return text;
        }

        text = tipTextData.optString(HikeConstants.TEXT, "");
        String msisdn = tipTextData.optString(HikeConstants.MSISDN);
        if(!TextUtils.isEmpty(msisdn) && !TextUtils.isEmpty(text))
        {
            try
            {
                ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, true);
                if(tipTextData.optBoolean(AtomicTipManager.SHOW_LAST_NAME, false))
                {
                    text = String.format(text, contactInfo.getNameOrMsisdn());
                }
                else
                {
                    text = String.format(text, contactInfo.getFirstName());
                }
            }
            catch (IllegalFormatException ife)
            {
                //Since there was a format error empty string will be shown
                text = "";
            }
        }

        return text;
    }

    private int processTipTextColor(JSONObject tipTextData, boolean isHeader)
    {
        Context hikeAppCtx = HikeMessengerApp.getInstance().getApplicationContext();
        int color = isHeader ? hikeAppCtx.getResources().getColor(R.color.atomic_tip_header_text)
                : hikeAppCtx.getResources().getColor(R.color.atomic_tip_body_text);

        if(tipTextData == null)
        {
            return color;
        }

        String textColor = tipTextData.optString(HikeConstants.TEXT_COLOR, "");
        if(!TextUtils.isEmpty(textColor))
        {
            try
            {
                color = Color.parseColor(textColor);
            }
            catch (IllegalArgumentException iae)
            {
                //Doing nothing here as already assigned default values above. this is just to avoid a crash.
            }
        }

        return color;
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
            this.showNotification = false;
            return;
        }
        else
        {
            this.notifTitle = tipNotifData.optString(HikeConstants.NOTIFICATION_TITLE);
            this.notifText = tipNotifData.optString(HikeConstants.NOTIFICATION_TEXT);
            this.isSilent = tipNotifData.optBoolean(HikeConstants.SILENT, true);
            this.showNotification = true;
        }
    }

    private void processTipCTA(JSONObject tipCTAData)
    {
        if(tipCTAData != null)
        {
            this.ctaLink = tipCTAData.optJSONObject(HikeConstants.TIP_CTA_LINK).toString();
            this.ctaAction = tipCTAData.optString(HikeConstants.MqttMessageTypes.ACTION, AtomicTipManager.NO_CTA_ACTION);
        }
        else
        {
            this.ctaAction = AtomicTipManager.NO_CTA_ACTION;
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
            hashCode = tipId.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof AtomicTipContentModel))
        {
            return false;
        }

        AtomicTipContentModel newModel = (AtomicTipContentModel) obj;
        if(this.hashCode() == newModel.hashCode())
        {
            return true;
        }

        return false;
    }

    public boolean isSilent()
    {
        return isSilent;
    }

    public String getTipId()
    {
        return tipId;
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

    @Status
    public int getTipStatus()
    {
        return tipStatus;
    }

    public void setTipStatus(@Status int status)
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

    public boolean isCancellable()
    {
        return isCancellable;
    }

    public boolean isShowNotification()
    {
        return showNotification;
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

    public String getAnalyticsTag()
    {
        return analyticsTag;
    }

    public int getHeaderTextColor()
    {
        return headerTextColor;
    }

    public int getBodyTextColor()
    {
        return bodyTextColor;
    }

    public boolean isCircularIcon()
    {
        return isCircularIcon;
    }
}
