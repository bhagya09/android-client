package com.bsb.hike.bots;

import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;


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
    String appPackageV2;
    private String unReadCountShowType;
	private int targetPlatform;

	private static final String DEFAULT_UNREAD_COUNT = "1+";
	private String nonMessagingBotType;
	private String url;
	private boolean isSpecialBot;
	private String targetActivity;
	private boolean replace;
	private String callbackId, parentMsisdn;
	private JSONObject fwdCardObj;
	private boolean resumeSupported;
	private JSONArray assoc_mapp;
    private int mAppVersionCode;
	private boolean autoResume;
	private int prefNetwork;

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
		setResumeSupported(json.optBoolean(HikePlatformConstants.RESUME_SUPPORTED));
		setAsocmapp(json.optJSONArray(HikePlatformConstants.ASSOCIATE_MAPP));
		setAutoResume(json.optBoolean(HikePlatformConstants.AUTO_RESUME,false));
		setPrefNetwork(json.optInt(HikePlatformConstants.PREF_NETWORK,Utils.getNetworkShortinOrder(HikePlatformConstants.DEFULT_NETWORK)));

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

            if (cardObj.has(HikePlatformConstants.APP_PACKAGE_V2))
            {
                setAppPackageV2(cardObj.optString(HikePlatformConstants.APP_PACKAGE_V2));
            }

			if (cardObj.has(HikePlatformConstants.URL))
			{
				setUrl(cardObj.optString(HikePlatformConstants.URL));
			}

			if (cardObj.has(HikePlatformConstants.SPECIAL))
			{
				setIsSpecialBot(cardObj.optBoolean(HikePlatformConstants.SPECIAL));
			}
			
			if (cardObj.has(HikePlatformConstants.TARGET_ACTIVITY))
			{
				setTargetActivity(cardObj.optString(HikePlatformConstants.TARGET_ACTIVITY));
			}

            if (cardObj.has(HikePlatformConstants.MAPP_VERSION_CODE))
            {
                setmAppVersionCode(cardObj.optInt(HikePlatformConstants.MAPP_VERSION_CODE));
            }

		}

		if (json.has((HikePlatformConstants.FORWARD_CARD_OBJECT)))
		{
			fwdCardObj = metadata.optJSONObject(HikePlatformConstants.FORWARD_CARD_OBJECT);
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

    public String getAppPackageV2()
    {
        return appPackageV2;
    }

    public void setAppPackageV2(String appPackageV2)
    {
        this.appPackageV2 = appPackageV2;
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
	
	public boolean isNativeMode()
	{
		return nonMessagingBotType.equals(HikePlatformConstants.NATIVE_MODE);
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

	
	public void setTargetActivity(String targetActivity)
	{
		this.targetActivity=targetActivity;
	}
	
	public String getTargetActivity()
	{
		return targetActivity;
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

	public JSONObject getFwdCardObj()
	{
		return fwdCardObj;
	}

	public JSONObject getHelperData()
	{
		return helperData;
	}
	public void setResumeSupported(boolean isResume)
	{
		this.resumeSupported=isResume;
	}
	public boolean isResumeSupported()
	{
		return resumeSupported;
	}
	public void setAsocmapp(JSONArray mapp)
	{

			assoc_mapp=mapp;
	}
	public JSONArray getAsocmapp()
	{
		return assoc_mapp;
	}

    public void setmAppVersionCode(int mAppVersionCode) {
        this.mAppVersionCode = mAppVersionCode;
    }

    public int getmAppVersionCode() {
        return mAppVersionCode;
    }

    /*
     * Method to retrieve unzipped micro app stored file path for this bot. (For default case, this method returns file path for micro app mode)
     */
	public String getBotFilePath()
	{
        switch (nonMessagingBotType)
		{
		case HikePlatformConstants.MICROAPP_MODE:
			return PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_WEB_MICRO_APPS + getAppName();
		case HikePlatformConstants.NATIVE_MODE:
            // If file is not found in the newer structured hierarchy directory path, then look for file in the older content directory path used before versioning
            String microAppPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_GAMES + getAppName();
            if(new File(microAppPath).exists())
            {
                return microAppPath;
            }
            else
            {
                return PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + getAppName();
            }
		default:
            return PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_WEB_MICRO_APPS + getAppName();
		}

	}

	public void setAutoResume(boolean autoResume)
	{
		this.autoResume=autoResume;
		if (autoResume)
		{
			resumeSupported = true;
		} // for auto resume resume supported has to be true.
	}

	public boolean getAutoResume()
	{
		return autoResume;
	}

	public void setPrefNetwork(int prefNetwork)
	{
		this.prefNetwork = prefNetwork;
	}

	public int getPrefNetwork()
	{
		return prefNetwork;
	}

}
