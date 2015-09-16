package com.bsb.hike.bots;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;


/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotMetadata
{
	JSONObject json;
	JSONObject helperData;
	String appName;
	JSONObject cardObj;
	String appPackage;
    private String unReadCountShowType;
	private int targetPlatform;

	private static final String DEFAULT_UNREAD_COUNT = "1+";
	private String nonMessagingBotType;
	private String url;
	private boolean isSpecialBot;
	private boolean replace;
	private String callbackId, parentMsisdn;

	public NonMessagingBotMetadata(String jsonString)
	{
		try
		{
			this.json = new JSONObject(jsonString);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			this.json = new JSONObject();
		}

		/**
		 * Can get an NPE if the string jsonString is null
		 */
		catch (NullPointerException e)
		{
			e.printStackTrace();
			this.json = new JSONObject();
		}
		init(json);
	}



	public NonMessagingBotMetadata(JSONObject metadata)
	{
		this.json = (null == metadata) ? new JSONObject() : metadata;

		init(json);
	}

	private void init(JSONObject metadata)
	{

		setNonMessagingBotType(json.optString(HikePlatformConstants.NON_MESSAGING_BOT_TYPE, HikePlatformConstants.MICROAPP_MODE));
		setTargetPlatform(json.optInt(HikePlatformConstants.TARGET_PLATFORM));
		setReplace(json.optBoolean(HikePlatformConstants.REPLACE_MICROAPP_VERSION));
		setParentMsisdn(json.optString(HikePlatformConstants.PARENT_MSISDN));
		setCallbackId(json.optString(HikePlatformConstants.CALLBACK_ID));

		if (json.has(HikePlatformConstants.CARD_OBJECT))
		{
			cardObj = metadata.optJSONObject(HikePlatformConstants.CARD_OBJECT);

			if (cardObj.has(HikePlatformConstants.APP_NAME))
			{
				setAppName(cardObj.optString(HikePlatformConstants.APP_NAME));
			}

			if (cardObj.has(HikePlatformConstants.APP_PACKAGE))
			{
				setAppPackage(cardObj.optString(HikePlatformConstants.APP_PACKAGE));
			}

			if (cardObj.has(HikePlatformConstants.URL))
			{
				setUrl(cardObj.optString(HikePlatformConstants.URL));
			}

			if (cardObj.has(HikePlatformConstants.SPECIAL))
			{
				setIsSpecialBot(cardObj.optBoolean(HikePlatformConstants.SPECIAL));
			}

		}

		setUnreadCountShowType();
	}

	// if unreadCountShowType is less than 0 then we need to set showType as default that is hardcoded one
	// if unreadCountShowType is 0 then we will set 0 
	// if number of digits is >4 it will set as max 4
	private void setUnreadCountShowType()
	{
		try
		{
			this.unReadCountShowType = json.optString(BotUtils.UNREAD_COUNT_SHOW_TYPE, DEFAULT_UNREAD_COUNT);
			int unReadCountType = Integer.parseInt(this.unReadCountShowType);
			if (unReadCountType < 0)
			{
				this.unReadCountShowType = DEFAULT_UNREAD_COUNT;
			}
			this.unReadCountShowType = this.unReadCountShowType.substring(0, (this.unReadCountShowType.length() < 4) ? this.unReadCountShowType.length() : 4);
		}
		catch (NumberFormatException e)
		{
			this.unReadCountShowType = this.unReadCountShowType.substring(0, (this.unReadCountShowType.length() < 4) ? this.unReadCountShowType.length() : 4);
			Logger.d("Non Messaging Bot Metadata", "handled number format exception");
		}
	}


	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public String getAppPackage()
	{
		return appPackage;
	}

	public void setAppPackage(String appPackage)
	{
		this.appPackage = appPackage;
	}

	public JSONObject getCardObj()
	{
		return cardObj;
	}

	public void setCardObj(JSONObject cardObj)
	{
		this.cardObj = cardObj;
	}

	public int getTargetPlatform()
	{
		return targetPlatform;
	}

	public void setTargetPlatform(int targetPlatform)
	{
		this.targetPlatform = targetPlatform < 0 || targetPlatform > HikePlatformConstants.CURRENT_VERSION ? HikePlatformConstants.CURRENT_VERSION : targetPlatform;
	}

	public JSONObject getJson()
	{
		return json;
	}

	public String getNonMessagingBotType()
	{
		return nonMessagingBotType;
	}

	public void setNonMessagingBotType(String nonMessagingBotType)
	{
		this.nonMessagingBotType = nonMessagingBotType;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public boolean isMicroAppMode()
	{
		return nonMessagingBotType.equals(HikePlatformConstants.MICROAPP_MODE);
	}

	public boolean isWebUrlMode()
	{
		return nonMessagingBotType.equals(HikePlatformConstants.URL_MODE);
	}

	@Override
	public String toString()
	{
		return json.toString();
	}

	public List<OverFlowMenuItem> getOverflowItems()
	{
		return null;
	}
	
	public String getUnreadCountShowType()
	{
		return unReadCountShowType;
	}

	public boolean isSpecialBot()
	{
		return isSpecialBot;
	}

	public void setIsSpecialBot(boolean isSpecialBot)
	{
		this.isSpecialBot = isSpecialBot;
	}

	public boolean shouldReplace()
	{
		return replace;
	}

	public void setReplace(boolean replace)
	{
		this.replace = replace;
	}

	public String getCallbackId()
	{
		return callbackId;
	}

	public void setCallbackId(String callbackId)
	{
		this.callbackId = callbackId;
	}

	public String getParentMsisdn()
	{
		return parentMsisdn;
	}

	public void setParentMsisdn(String parentMsisdn)
	{
		this.parentMsisdn = parentMsisdn;
	}
}
