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
	private String lastName;

	@Expose
	private String firstName;

	@Expose
	private String location;

	@Expose
	private String is_on_hike;

	@Expose
	private JsonObject name;

	public String getLastName()
	{
		if (name != null)
		{
			return name.get(LAST_NAME).getAsString();
		}
		else
		{
			return "";
		}
	}

	public String getFirsttName()
	{
		if (name != null)
		{
			return name.get(FIRST_NAME).getAsString();
		}
		else
			return "";
	}

	public String getLocation()
	{
		return location;
	}

	public String getIsOnHike()
	{
		return is_on_hike;
	}

}
