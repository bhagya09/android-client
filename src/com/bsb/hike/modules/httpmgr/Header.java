package com.bsb.hike.modules.httpmgr;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * A util class for the header of the {@link Request} or {@link Response} class
 * 
 * @author sidharth
 * 
 */
public class Header implements Comparable<Header>
{
	private String name;

	private String value;

	public Header(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String val)
	{
		this.value = val;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (other == null || getClass() != other.getClass())
		{
			return false;
		}

		Header header = (Header) other;
		if (name != null ? !name.equals(header.name) : header.name != null)
		{
			return false;
		}
		if (value != null ? !value.equals(header.value) : header.value != null)
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int result = name != null ? name.hashCode() : 0;
		/*
		 * Why 31 ?? A nice property of 31 is that the multiplication can be replaced by a shift and a subtraction for better performance
		 * http://stackoverflow.com/questions/3869252/what-is-the-preferred-way-of-implementing-hashcode
		 * 
		 * Also refer Effective Java by John Bloch (Item 9: Always override hashCode when you override equals) for more details
		 */
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}

	@Override
	public String toString()
	{
		return (name != null ? name : "") + ": " + (value != null ? value : "");
	}

	@Override
	public int compareTo(Header another)
	{
		if (another == null)
		{
			return -1;
		}
		String name = this.getName();
		if (TextUtils.isEmpty(name))
		{
			return 1;
		}
		return name.compareTo(another.getName());
	}
}
