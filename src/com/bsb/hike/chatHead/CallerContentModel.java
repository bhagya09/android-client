package com.bsb.hike.chatHead;

import android.text.TextUtils;

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

	private boolean is_synced;

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

    private long updation_time, creation_time;

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
		if (!TextUtils.isEmpty(full_name))
		{
			return  full_name;
		}

		String firstName = getFirstName();

		String lastName = getLastName();

		if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName))
		{
			return null;
		}

		String name = "";

		if (!TextUtils.isEmpty(firstName))
		{
			name = firstName + " ";
		}
		if (!TextUtils.isEmpty(lastName))
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

	public boolean isSynced()
	{
		return is_synced;
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

	public void setIsSynced(boolean is_synced)
	{
		this.is_synced = is_synced;
	}

	public void setUpdationTime(long updation_time)
	{
		this.updation_time = updation_time;
	}

	public void setCreationTime(long creation_time)
	{
		this.creation_time = creation_time;
	}

	public long getUpdationTime()
	{
		return updation_time;
	}

	public long getCreationTime()
	{
		return creation_time;
	}
}
