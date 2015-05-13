package com.bsb.hike.bots;

import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.platform.HikePlatformConstants;
import org.json.JSONException;
import org.json.JSONObject;

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
	}



	public NonMessagingBotMetadata(JSONObject metadata)
	{
		this.json = (null == metadata) ? new JSONObject() : metadata;

		if (json.has(HikePlatformConstants.CARD_OBJECT))
		{
			cardObj = metadata.optJSONObject(HikePlatformConstants.CARD_OBJECT);

			if (cardObj.has(HikePlatformConstants.HELPER_DATA))
			{
				setHelperData(cardObj.optJSONObject(HikePlatformConstants.HELPER_DATA));
			}

			if (cardObj.has(HikePlatformConstants.APP_NAME))
			{
				setAppName(cardObj.optString(HikePlatformConstants.APP_NAME));
			}
		}
	}

	public JSONObject getHelperData()
	{
		return helperData;
	}

	public void setHelperData(JSONObject helperData)
	{
		this.helperData = helperData;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public JSONObject getCardObj()
	{
		return cardObj;
	}

	public void setCardObj(JSONObject cardObj)
	{
		this.cardObj = cardObj;
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

}
