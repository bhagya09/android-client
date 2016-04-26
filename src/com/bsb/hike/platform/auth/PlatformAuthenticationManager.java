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
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * related doc: https://docs.google.com/document/d/1477TI-AIA8tUAdZAMFLUC6JIvBetjzdjtQZEakNUgs8/edit
 * 
 * @author Rashmi Dang
 */
public class PlatformAuthenticationManager
{

	private AuthListener _callBack;

	private String _clientId;

	private  boolean _isLongTermTokenRequired = false;

	private String _mAppId;

	private static String _pUID;

	private static String _pToken;

	public static final String tag = "PlatformAuthenticationManager";

	public PlatformAuthenticationManager(String clientId,String mAppId,String platformUID, String platformToken, AuthListener callback)
	{

		_pUID = platformUID;
		_pToken = platformToken;
		_clientId = clientId;
		_callBack = callback;
		_mAppId = mAppId;
	}

	public void requestAuthToken(int tokenLife)
	{
		if(tokenLife==DBConstants.LONG_LIVED){
			_isLongTermTokenRequired = true;
		}
		if (_clientId!=null)
		{
			requestAuthToken(_clientId);
		}
		else{
			Logger.d(tag, "Client id is null");
			if(_callBack != null) {
				_callBack.onTokenErrorResponse(null);
			}
		}
	}

	// Request for auth token
	public void requestAuthToken(String clientId)
	{
		if(clientId==null){
			return;
		}
		RequestToken requestToken = authSDKRequest(_pUID, _pToken, clientId, new AuthTokenRequestListener());
		requestToken.execute();
	}
	private String getAuthGetData(String clientId)
	{
		String expiry_type = HikePlatformConstants.AuthConstants.AUTH_SHORT_TYPE;
		if(_isLongTermTokenRequired){
			expiry_type = HikePlatformConstants.AuthConstants.AUTH_LONG_TYPE;
		}
		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_RESPONSE_TYPE,HikePlatformConstants.AuthConstants.AUTH_TEST_RESPONSE_TYPE));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, clientId));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SCOPE, HikePlatformConstants.AuthConstants.AUTH_TEST_PARAM_SCOPE));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_PACKAGE_NAME, HikePlatformConstants.AuthConstants.AUTH_TEST_CLIENT_PACKAGE_NAME));
		params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_EXPIRY_TYPE, expiry_type));
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
	public RequestToken authSDKRequest(String puid, String pToken,String clientId, IRequestListener requestListener)
	{
		List<Header> headerList = new ArrayList<Header>(1);
		headerList.add(new Header(HikePlatformConstants.COOKIE, HikePlatformConstants.PLATFORM_USER_ID + "=" + puid + ";" + HikePlatformConstants.PLATFORM_TOKEN + "=" + pToken));

		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(HttpRequestConstants.authBaseUrl() + getAuthGetData(clientId)).setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener).setResponseOnUIThread(true).setHeaders(headerList).build();
		return requestToken;
	}


	// /Request listener for auth Token
	private class AuthTokenRequestListener implements IRequestListener
	{
		public static final String tag = "AuthTokenRequestListener";
		public void onRequestSuccess(Response result)
		{
			try
			{
				Logger.d(PlatformAuthenticationManager.class.getCanonicalName(), "on task success");
				JSONObject responseJSON = (JSONObject) result.getBody().getContent();
				JSONObject responseData = responseJSON.getJSONObject(HikePlatformConstants.RESPONSE);
				if (responseData != null)
				{
					if (responseData.has(HikePlatformConstants.ERROR))
					{
						Logger.d(tag, "access token request failed: "+responseData);
						onRequestFailure(null);
						return;
					}
					if (responseData.has(HikePlatformConstants.PLATFORM_AUTH_TOKEN))
					{
						String accessToken = responseData.getString(HikePlatformConstants.PLATFORM_AUTH_TOKEN);
						if(_isLongTermTokenRequired){
							HikeContentDatabase.getInstance().addAuthToken(_mAppId,accessToken);
						}
									Logger.d(tag, "access token recieved");
						if(_callBack != null) {
							_callBack.onTokenResponse(accessToken);
						}

					}

				}
				else
				{
					onRequestFailure(null);
					return;
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				onRequestFailure(null);
				return;
			}
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			Logger.d(tag, "auth request failed: " + httpException.getErrorCode());
			if(_callBack != null){
				_callBack.onTokenErrorResponse(null);
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			// TODO Auto-generated method stub

		}
	}
	
	

}
