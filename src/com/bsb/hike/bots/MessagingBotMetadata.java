package com.bsb.hike.bots;

import com.bsb.hike.HikeConstants;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhit on 06/05/15.
 */
public class MessagingBotMetadata
{
	JSONObject json;

	boolean isReceiveEnabled;

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

}
