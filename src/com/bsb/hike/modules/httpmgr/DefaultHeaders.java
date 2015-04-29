package com.bsb.hike.modules.httpmgr;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.utils.AccountUtils;

/**
 * Class to add default headers to the request like user-agent and cookie etc
 * 
 * @author sidharth
 * 
 */
public final class DefaultHeaders
{
	// TODO move these constants to HttpHeaderConstants created by shobhit when that is merged in internal release
	public static final String CACHE_CONTROL = "Cache-Control";
	
	public static final String NO_TRANSFORM = "no-transform";
	
	private static <T> List<Header> getDefaultHeaders(Request<T> request)
	{
		List<Header> headers = new ArrayList<Header>(3);

		if (AccountUtils.appVersion != null && !Utils.containsHeader(request.getHeaders(), HttpHeaderConstants.USER_AGENT_HEADER_NAME))
		{
			headers.add(new Header(HttpHeaderConstants.USER_AGENT_HEADER_NAME, HttpHeaderConstants.ANDROID + "-" + AccountUtils.appVersion));
		}
		if (AccountUtils.mToken != null && AccountUtils.mUid != null && !Utils.containsHeader(request.getHeaders(), HttpHeaderConstants.COOKIE_HEADER_NAME))
		{
			headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME, HttpHeaderConstants.USER + "=" + AccountUtils.mToken + "; " + HttpHeaderConstants.UID + "=" + AccountUtils.mUid));
		}
		if (!Utils.containsHeader(request.getHeaders(), CACHE_CONTROL))
		{
			headers.add(new Header(CACHE_CONTROL, NO_TRANSFORM));
		}
		return headers;
	}

	public static <T> void applyDefaultHeaders(Request<T> request)
	{
		request.addHeaders(getDefaultHeaders(request));
	}

	private DefaultHeaders()
	{

	}
}
