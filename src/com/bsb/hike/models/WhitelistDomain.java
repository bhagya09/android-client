package com.bsb.hike.models;

import java.net.MalformedURLException;
import java.net.URL;

import android.text.TextUtils;

public class WhitelistDomain
{
	public static final int NOT_WHITE_LISTED = -1;

	public static final int WHITELISTED_IN_BROWSER = 0;

	public static final int WHITELISTED_IN_HIKE = 1;

	private String url;
	
	private String domain;
	
	private int whitelistState = NOT_WHITE_LISTED;

	public WhitelistDomain(String url, int whitelistState)
	{
		this.url = url;
		this.whitelistState = whitelistState;
	}
	
	public WhitelistDomain(String url, int whitelistState,String domain)
	{
		this.url = url;
		this.whitelistState = whitelistState;
		this.domain = domain;
	}

	public String getDomain()
	{
		if(this.domain!=null)
		{
			return this.domain;
		}
		// domain
		if(!TextUtils.isEmpty(url))
		{
			try
			{
				URL mUrl = new URL(url);
				String domain = mUrl.getHost();
				if(domain.startsWith("www."))
				{
					return domain.substring(4);
				}
				return domain;
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
			
		}
		return "";
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public int getWhitelistState()
	{
		return whitelistState;
	}

	public void setWhitelistState(int whitelistState)
	{
		this.whitelistState = whitelistState;
	}
	
	public boolean isOpenInHikeAllowed()
	{
		return WHITELISTED_IN_HIKE == this.whitelistState;
	}
	
	public boolean isOpenInBrowserAllowed()
	{
		return WHITELISTED_IN_BROWSER == this.whitelistState;
	}
}
