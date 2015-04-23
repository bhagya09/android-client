/**
 * 
 */
package com.bsb.hike.models.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

/**
 * This class extends ConversationMetadata. It contains the extra JSON fields required to form OneToNConversation metadata.
 * 
 * @author Anu/Piyush
 */
public class OneToNConversationMetadata extends ConversationMetadata {

	/**
	 * @throws JSONException 
	 * 
	 */
	public OneToNConversationMetadata(String jsonString) throws JSONException {
		super(jsonString);
		if (jsonString == null)
		{
			setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, -1);
			setUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
			setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
		}
	}

	private JSONObject getPinJson(int pinType) throws JSONException
	{
		JSONObject json = jsonObject.optJSONObject(Integer.toString(pinType));
		if (json == null)
		{
			jsonObject.put(Integer.toString(pinType), json = new JSONObject());
		}
		return json;
	}
	
	public long getLastPinId(int pinType) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		return pinJSON.getLong(HikeConstants.ID);
	}
	
	public long getLastPinTimeStamp(int pinType) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		if(pinJSON.has(HikeConstants.TIMESTAMP))
		{
			return pinJSON.getLong(HikeConstants.TIMESTAMP);
		}
		return -1;
	}
	
	public void setLastPinTimeStamp(int pinType, long timeStamp) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		pinJSON.put(HikeConstants.TIMESTAMP, timeStamp);
	}

	public int getUnreadPinCount(int pinType) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		return pinJSON.getInt(HikeConstants.UNREAD_COUNT);
	}

	public boolean isShowLastPin(int pinType)
	{
		try
		{
			JSONObject pinJson = getPinJson(pinType);
			return pinJson.getBoolean(HikeConstants.TO_SHOW);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public void setLastPinId(int pinType, long id) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		pinJSON.put(HikeConstants.ID, id);
	}

	public void setUnreadPinCount(int pinType, int count) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		pinJSON.put(HikeConstants.UNREAD_COUNT, count);
	}

	public void incrementUnreadPinCount(int pinType) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		pinJSON.put(HikeConstants.UNREAD_COUNT, pinJSON.getInt(HikeConstants.UNREAD_COUNT) + 1);
	}
	
	public void decrementUnreadPinCount(int pinType) throws JSONException
	{
		JSONObject pinJSON = getPinJson(pinType);
		int unreadCount =pinJSON.getInt(HikeConstants.UNREAD_COUNT);
		if(unreadCount>0)
		{
	    	pinJSON.put(HikeConstants.UNREAD_COUNT, pinJSON.getInt(HikeConstants.UNREAD_COUNT) - 1);
		}
	}

	public void setShowLastPin(int pinType, boolean isShow) throws JSONException
	{
		JSONObject pinJson = getPinJson(pinType);
		pinJson.put(HikeConstants.TO_SHOW, isShow);
	}

	public void setPinDisplayed(int pinType, boolean isShow) throws JSONException
	{
		JSONObject pinJson = getPinJson(pinType);
		pinJson.put(HikeConstants.PIN_DISPLAYED, isShow);
	}
	
	public boolean isPinDisplayed(int pinType)
	{
		try
		{
			JSONObject pinJson = getPinJson(pinType);
			return pinJson.getBoolean(HikeConstants.PIN_DISPLAYED);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
