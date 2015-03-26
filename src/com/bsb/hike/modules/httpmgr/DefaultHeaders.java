package com.bsb.hike.modules.httpmgr;

import java.util.ArrayList;
import java.util.List;

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
	private static <T> List<Header> getDefaultHeaders(Request<T> request)
	{
		List<Header> headers = new ArrayList<Header>(2);

		if (AccountUtils.appVersion != null && !Utils.containsHeader(request.getHeaders(), "User-Agent"))
		{
			headers.add(new Header("User-Agent", "android-" + AccountUtils.appVersion));
		}
		if (AccountUtils.mToken != null && AccountUtils.mUid != null && !Utils.containsHeader(request.getHeaders(), "Cookie"))
		{
			headers.add(new Header("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid));
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
