package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhitmandloi on 15/01/15.
 */
public class WebMetadata
{
	private String notifText = "";

	private JSONObject helperData;

	private int cardHeight;

	private JSONObject cardobj;

	private String appName;

	private String layoutId;

	private boolean longPressDisabled;
	
	private int targetPlatform;

	private String parentMsisdn;

	public String getLayoutId()
	{
		return layoutId;
	}

	public void setLayoutId(String layoutId)
	{
		this.layoutId = layoutId;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public int getCardHeight()
	{
		return cardHeight;
	}

	public void setCardHeight(int cardHeight)
	{
		try
		{
			cardobj.put(HikePlatformConstants.HEIGHT, String.valueOf(cardHeight));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public String getNotifText()
	{
		return notifText;
	}

	public JSONObject getHelperData()
	{
		return helperData;
	}

	public void setHelperData(JSONObject helperData)
	{
		if (null != helperData)
		{
			this.helperData = helperData;
		}
		else
		{
			this.helperData = new JSONObject();
		}
	}

	private JSONObject json;

	private String mPush;

	public WebMetadata(String jsonString) throws JSONException
	{
		this(new JSONObject(jsonString));
	}
	
	
	public WebMetadata(JSONObject metadata)
	{
		this.json = metadata;
		if (metadata.has(HikePlatformConstants.CARD_OBJECT))
		{
			cardobj = metadata.optJSONObject(HikePlatformConstants.CARD_OBJECT);

			
			setTargetPlatform(this.json.optInt(HikePlatformConstants.TARGET_PLATFORM));
			
			if (cardobj.has(HikePlatformConstants.HELPER_DATA))
			{
				setHelperData(cardobj.optJSONObject(HikePlatformConstants.HELPER_DATA));
			}

			if (cardobj.has(HikePlatformConstants.APP_NAME))
			{
				setAppName(cardobj.optString(HikePlatformConstants.APP_NAME));
			}

			if (cardobj.has(HikePlatformConstants.HEIGHT))
			{
				this.cardHeight = Integer.parseInt(cardobj.optString(HikePlatformConstants.HEIGHT));
			}

			if (cardobj.has(HikePlatformConstants.LAYOUT))
			{
				setLayoutId(cardobj.optString(HikePlatformConstants.LAYOUT));
			}

			// Extract notif text
			if (cardobj.has(HikePlatformConstants.NOTIF_TEXT_WC))
			{
				setNotifText(cardobj.optString(HikePlatformConstants.NOTIF_TEXT_WC));
			}

			if (cardobj.has(HikePlatformConstants.WC_PUSH_KEY))
			{
				setPush(cardobj.optString(HikePlatformConstants.WC_PUSH_KEY));
			}

			if (cardobj.has(HikePlatformConstants.PARENT_MSISDN))
			{
				setParentMsisdn(cardobj.optString(HikePlatformConstants.PARENT_MSISDN));
			}
			setLongPressDisabled(cardobj.optBoolean(HikePlatformConstants.LONG_PRESS_DISABLED));
		}
		else
		{
			cardobj = new JSONObject();
		}


	}

	public boolean isLongPressDisabled()
	{
		return longPressDisabled;
	}

	private void setLongPressDisabled(boolean longPressDisabled)
	{
		this.longPressDisabled = longPressDisabled;
	}

	private void setPush(String optString)
	{
		this.mPush = optString;
	}

	public String getPushType()
	{
		return mPush;
	}

	public JSONObject getJSON()
	{
		return json;
	}

	public String JSONtoString()
	{
		return json.toString();
	}

	public String getAlarmData()
	{
		return getString(HikePlatformConstants.ALARM_DATA);
	}

	private String getString(String key)
	{
		return json.optString(key);
	}

	public void setNotifText(String notifText)
	{
		this.notifText = notifText;
	}
	
	public int getPlatformJSCompatibleVersion()
	{
		return targetPlatform; 
	}
	
	
	public void setTargetPlatform(int targetPlatform)
	{
		this.targetPlatform = targetPlatform < 0 || targetPlatform > HikePlatformConstants.CURRENT_VERSION ? HikePlatformConstants.CURRENT_VERSION : targetPlatform;
	}
	
	public int getTargetPlatform()
	{
		return targetPlatform;
	}

	public String getParentMsisdn()
	{
		return parentMsisdn;
	}

	public void setParentMsisdn(String parentMsisdn)
	{
		this.parentMsisdn = parentMsisdn;
	}

    public void setCardobj(JSONObject cardobj)
    {
        this.cardobj = cardobj;
    }

    public JSONObject getCardobj()
    {
        return cardobj;
    }

}
