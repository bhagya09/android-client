package com.bsb.hike.bots;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.models.OverFlowMenuItem;

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotMetadata
{
	JSONObject json;

	public NonMessagingBotMetadata(String jsonString)
	{
		try
		{
			json = new JSONObject(jsonString);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			json = new JSONObject();
		}
	}

	
	
}
