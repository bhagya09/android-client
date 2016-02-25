package com.bsb.hike.platform.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;

public class AuthHttpRequestUtility

{
	public static JSONObject getBrandRegisterPostdata()
	{
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.EMAIL, AuthConstants.AUTH_TEST_EMAIL);
			data.put(HikeConstants.PRODUCT, AuthConstants.AUTH_TEST_PRODUCT);
			data.put(HikeConstants.CLIENT_URL, AuthConstants.AUTH_TEST_CLIENT_URL);
			data.put(HikeConstants.CLIENT_IMAGE_URL, AuthConstants.AUTH_TEST_IMAGE_URL);
		}
		catch (JSONException e)
		{
			Logger.e("Exception", "Invalid JSON", e);
		}
		return data;
	}

	public static JSONObject getClientIdPostdata(String product, String email)
	{
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.EMAIL, email);
			data.put(HikeConstants.CLIENT_TYPE, AuthConstants.AUTH_TEST_CLIENT_TYPE);
			data.put(HikeConstants.PACKAGE_NAME, AuthConstants.AUTH_TEST_CLIENT_PACKAGE_NAME);
			data.put(HikeConstants.SHA1, AuthConstants.AUTH_TEST_SHA1);
		}
		catch (JSONException e)
		{
			Logger.e("Exception", "Invalid JSON", e);
		}
		return data;
	}
	
	private static String getAuthGetData(String clientId)
	{
		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_RESPONSE_TYPE, AuthConstants.AUTH_TEST_RESPONSE_TYPE));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, clientId));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SCOPE, AuthConstants.AUTH_TEST_PARAM_SCOPE));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_PACKAGE_NAME, AuthConstants.AUTH_TEST_CLIENT_PACKAGE_NAME));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SHA1, AuthConstants.AUTH_TEST_SHA1));
		// }

		String paramString = URLEncodedUtils.format(params, "UTF-8");
		try
		{
			paramString = URLDecoder.decode(paramString, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
			return null;
		}
		return paramString;
	}

	
	//////////////////Request Tokens/////////////////////
	public static RequestToken authSDKRequest(String puid, String pToken,String clientId, IRequestListener requestListener)
	{
		List<Header> headerList = new ArrayList<Header>(1);
		headerList.add(new Header(HikePlatformConstants.COOKIE, HikePlatformConstants.PLATFORM_USER_ID + "=" + puid + ";" + HikePlatformConstants.PLATFORM_TOKEN + "=" + pToken));

		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(HttpRequestConstants.authBaseUrl() + getAuthGetData(clientId)).setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener).setResponseOnUIThread(true).setHeaders(headerList).build();
		return requestToken;
	}

	public static RequestToken registerBrandRequest( IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(getBrandRegisterPostdata());
		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(HttpRequestConstants.registerBrandUrl()).setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener).post(body).build();
		return requestToken;
	}

	public static RequestToken clientIdRequest(String product, String email,IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(getClientIdPostdata(product, email));
		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(HttpRequestConstants.clientIdUrl()).setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener).post(body).build();
		return requestToken;
	}


}