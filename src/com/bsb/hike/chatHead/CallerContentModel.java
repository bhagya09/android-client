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

	@Expose
	private boolean is_spam;

	@Expose
	private int spam_count;

	private String full_name;

	private boolean is_block;


	public String getLastName()
	{
		if (full_name != null)
		{
			return null;
		}
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
		if (full_name != null)
		{
			return  full_name;
		}
		
		if (name != null && name.has(FIRST_NAME))
		{
			return name.get(FIRST_NAME).getAsString();
		}
		else
		{
			return null;
		}
	}

	public String getFullName()
	{
		if (full_name != null)
		{
			return  full_name;
		}

		String firstName = getFirstName();

		String lastName = getLastName();

		if (firstName == null && lastName == null)
		{
			return null;
		}

		String name = "";
		if (firstName != null)
		{
			name = firstName + " ";
		}
		if (lastName != null)
		{
			name = name + lastName;
		}
		return name;
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

	public int getSpamCount()
	{
		return spam_count;
	}

	public boolean isSpam()
	{
		return is_spam;
	}

	public boolean isBlock()
	{
		return is_block;
	}

	public void setBlock(boolean is_block)
	{
		this.is_block = is_block;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public void setIsOnHike(boolean is_on_hike)
	{
		this.is_on_hike = is_on_hike;
	}

	public void setSpamCount(int spam_count)
	{
		this.spam_count = spam_count;
	}

	public void setIsSpam(boolean is_spam)
	{
		this.is_spam = is_spam;
	}

	public void setFullName(String full_name)
	{
		this.full_name = full_name;
	}
}
