package com.bsb.hike.bots;

import com.bsb.hike.models.OverFlowMenuItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotMetadata
{
	JSONObject json;


	public NonMessagingBotMetadata(String jsonString) throws JSONException
	{
		this(new JSONObject(jsonString));
	}

	public NonMessagingBotMetadata(JSONObject metadata)
	{
		this.json = metadata;
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
