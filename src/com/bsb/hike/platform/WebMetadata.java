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

	private static final String PUSH_SILENT= "silent";
	

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
		}
		else
		{
			cardobj = new JSONObject();
		}

		if (metadata.has(HikePlatformConstants.LONG_PRESS_DISABLED))
		{
			if (metadata.optBoolean(HikePlatformConstants.LONG_PRESS_DISABLED))
			{
				setLongPressDisabled(true);
			}
			else
			{
				setLongPressDisabled(false);
			}
		}
		else
		{
			setLongPressDisabled(false);
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

	public boolean isSilent()
	{
		return PUSH_SILENT.equals(mPush);
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
}
