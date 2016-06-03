package com.bsb.hike.models.Conversation;

import com.bsb.hike.HikeConstants;

import org.json.JSONException;

public class OneToOneConversationMetadata extends ConversationMetadata
{

	/**
	 * @param jsonString
	 * @throws JSONException
	 */
	public OneToOneConversationMetadata(String jsonString) throws JSONException
	{
		super(jsonString);

		if(!jsonObject.has(HikeConstants.SHOW_USER_INFO_VIEW))
		{
			setUserInfoViewToBeShown(true);
		}
	}

	public boolean isUserInfoViewToBeShown()
	{
		try
		{
			return jsonObject.getBoolean(HikeConstants.SHOW_USER_INFO_VIEW);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public void setUserInfoViewToBeShown(boolean userInfoViewToBeShown) throws JSONException
	{
		jsonObject.put(HikeConstants.SHOW_USER_INFO_VIEW, userInfoViewToBeShown);
	}
}
