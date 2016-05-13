package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class CallerMetadata {

    JSONObject jsonObject;

    /**
     * @param jsonString
     * @throws JSONException
     */
	public CallerMetadata(String jsonString)
	{
		try
		{
			if (jsonString != null)
			{
				jsonObject = new JSONObject(jsonString);
			}
			else
			{
				jsonObject = new JSONObject();
			}
		}
		catch (JSONException ex)
		{
			ex.printStackTrace();
		}
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    protected void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String toString()
    {
        return jsonObject.toString();
    }

    public void setChatSpamCount(int count) throws JSONException
    {
        jsonObject.put(HikeConstants.CHAT_SPAM_COUNT, count);
    }

    public int getChatSpamCount() throws JSONException
    {
        return jsonObject.optInt(HikeConstants.CHAT_SPAM_COUNT);
    }

    public void setIsUserSpammedByYou(int isSpamByYou) throws JSONException
    {
        jsonObject.put(HikeConstants.IS_USER_CHAT_SPAMMED_BY_YOU, isSpamByYou);
    }

    public int getIsUserSpammedByYou() throws JSONException
    {
        return jsonObject.optInt(HikeConstants.IS_USER_CHAT_SPAMMED_BY_YOU);
    }

    public boolean isEmpty()
    {
        return jsonObject.length() > 0;
    }

}
