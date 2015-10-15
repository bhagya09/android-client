package com.bsb.hike.chatHead;

import org.json.JSONException;
import org.json.JSONObject;
import com.bsb.hike.utils.Logger;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;


/**
 * @author ashishagarwal
 * 
 *         Content Model for Hike Caller
 * 
 */

public class CallerContentModel
{

	final private String FIRST_NAME = "first_name";

	final private String LAST_NAME = "last_name";

	@Expose
	private String location, msisdn;

	@Expose
	private boolean is_on_hike;

	@Expose
	private JsonObject name;

	public String getLastName()
	{
		if (name != null && name.has(LAST_NAME))
		{
			return name.get(LAST_NAME).getAsString();
		}
		else
		{
			return null;
		}
	}

	public String getFirstName()
	{
		if (name != null && name.has(FIRST_NAME))
		{
			return name.get(FIRST_NAME).getAsString();
		}
		else
		{
			return null;
		}
	}

	public String getLocation()
	{
		return location;
	}

	public boolean getIsOnHike()
	{
		return is_on_hike;
	}
	
	public String getMsisdn()
	{
		return msisdn;
	}

}