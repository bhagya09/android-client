package com.bsb.hike.models.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class adds metadata JSONObject to the conversation object.
 * 
 * @author Anu/Piyush
 */
public class ConversationMetadata {
	
	protected JSONObject jsonObject;
	
	/**
	 * @param jsonString
	 * @throws JSONException
	 */
	public ConversationMetadata(String jsonString) throws JSONException
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
}
