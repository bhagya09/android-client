package com.bsb.hike.bots;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhit on 06/05/15.
 */
public class MessagingBotMetadata
{
	JSONObject json;

	boolean isReceiveEnabled;
	
	private String unReadCountShowType;
	
	public MessagingBotMetadata(String metadata)
	{
		try
		{
			this.json = new JSONObject(metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			this.json = new JSONObject();
		}
		/**
		 * Can get an NPE if the string metadata is null
		 */
		catch (NullPointerException e)
		{
			e.printStackTrace();
			this.json = new JSONObject();
		}
		
		init();

	}

	public MessagingBotMetadata(JSONObject jsonObject)
	{

		this.json = (null == jsonObject) ? new JSONObject() : jsonObject;

		init();
	}

	private void init()
	{

		this.isReceiveEnabled = json.optBoolean(HikeConstants.IS_RECEIVE_ENABLED_IN_BOT, true);
		
		setUnreadCountShowType();
		
	}

	private void setUnreadCountShowType()
	{   
		// if unreadCountShowType is less than 0 then we need to set showType as -1 and it will show actual count
		// if unreadCountShowType is 0 then we will set 0 
		// if number of digits is >4 it will set as max 4 
		try
		{
			this.unReadCountShowType = json.optString(BotUtils.UNREAD_COUNT_SHOW_TYPE, BotUtils.SHOW_UNREAD_COUNT_ACTUAL);
			int unReadCountType = Integer.parseInt(this.unReadCountShowType);
			if (unReadCountType < 0)
			{
				this.unReadCountShowType = BotUtils.SHOW_UNREAD_COUNT_ACTUAL;
			}
			this.unReadCountShowType = this.unReadCountShowType.substring(0, (this.unReadCountShowType.length() < 4) ? this.unReadCountShowType.length() : 4);
		}
		catch (NumberFormatException e)
		{
			this.unReadCountShowType = this.unReadCountShowType.substring(0, (this.unReadCountShowType.length() < 4) ? this.unReadCountShowType.length() : 4);
			Logger.d("Messaging Bot Metadata", "handled number format exception");
		}
	}

	public boolean isReceiveEnabled()
	{
		return isReceiveEnabled;
	}

	public void setReceiveEnabled(boolean isReceiveEnabled)
	{
		this.isReceiveEnabled = isReceiveEnabled;
	}

	@Override
	public String toString()
	{
		return json.toString();
	}
	
	public String getUnreadCountShowType()
	{
		return unReadCountShowType;
	}
	
}
