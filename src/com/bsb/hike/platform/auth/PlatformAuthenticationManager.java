package com.bsb.hike.platform.auth;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
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

	private HikeSharedPreferenceUtil _mPrefs;

	private AuthListener _callBack;

	private String _clientId;

	private static String _pUID;

	private static String _pToken;

	public static final String tag = "PlatformAuthenticationManager";

	public PlatformAuthenticationManager(String platformUID, String platformToken, AuthListener callback)
	{

		_pUID = platformUID;
		_pToken = platformToken;
		_mPrefs = HikeSharedPreferenceUtil.getInstance();
		_clientId = _mPrefs.getData(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, null);
		_callBack = callback;
	}

	
	//Check whether clientId generated or not 
	//If not generated yet..... firstly generate client id
	//Otherwise start request for auth token
	
	public String requestAuthToken()
	{
		if (haveClientId())
		{
			requestAuthToken(_clientId);
		}
		else
		{
			requestBrandRegisteration();
		}
		return null;
	}

	//Check for clientid (if its generated or not)
	private boolean haveClientId()
	{
		if (_clientId != null)
		{
			return true;
		}
		return false;

	}

	// Request for brand registeration
	private void requestBrandRegisteration()
	{
		RequestToken brandRequest = AuthHttpRequestUtility.registerBrandRequest(new BrandRequestListener());
		brandRequest.execute();

	}

	// Request for client id
	private void requestForClientId(String product, String email)
	{
		RequestToken clientId = AuthHttpRequestUtility.clientIdRequest(product, email, new ClientIDRequestListener());
		clientId.execute();
	}

	// Request for auth token
	public void requestAuthToken(String clientId)
	{
		RequestToken requestToken = AuthHttpRequestUtility.authSDKRequest(_pUID, _pToken, clientId, new AuthTokenRequestListener());
		requestToken.execute();
	}

	// Request listener for brand registeration
	private class BrandRequestListener implements IRequestListener
	{
		public static final String tag = "BrandRequestListener";
		public void onRequestFailure(HttpException httpException)
		{
			Logger.d(tag, "brand registeration request failed: "+httpException.getErrorCode());
		}

		@Override
		public void onRequestSuccess(Response result)
		{
			try
			{
				Logger.d(tag, "register brand ---on task success");
				JSONObject responseData = (JSONObject) result.getBody().getContent();

				if (responseData.has(HikePlatformConstants.ERROR))
				{
					Logger.d(tag, "brand registeration request failed: "+responseData);
					onRequestFailure(null);
					return;
				}
				String product = "";
				String email = "";
				if (responseData.has(HikeConstants.PRODUCT))
				{
					product = responseData.getString(HikeConstants.PRODUCT);
				}
				if (responseData.has(HikeConstants.EMAIL))
				{
					email = responseData.getString(HikeConstants.EMAIL);
				}
				requestForClientId(product, email);

			}
			catch (JSONException e)
			{
				e.printStackTrace();
				onRequestFailure(null);
				return;
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
		}
	}

	// /Request listener for client id
	private class ClientIDRequestListener implements IRequestListener
	{
		public static final String tag = "ClientIDRequestListener";
		@Override
		public void onRequestFailure(HttpException httpException)
		{
			Logger.d(tag, "client id request failed: "+httpException.getErrorCode());
		}

		@Override
		public void onRequestSuccess(Response result)
		{
			try
			{
				Logger.d(tag, "clientId request ---on task success");
				JSONObject responseData = (JSONObject) result.getBody().getContent();

				if (responseData.has(HikePlatformConstants.ERROR))
				{
					Logger.d(tag, "client id request failed: "+responseData);
					onRequestFailure(null);
					return;
				}
				if (responseData.has(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID))
				{
					String clientId = responseData.getString(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID);
					_mPrefs.saveData(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, clientId);
					requestAuthToken(clientId);
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
		public void onRequestProgressUpdate(float progress)
		{
			// TODO Auto-generated method stub

		}

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
						_mPrefs.saveData(HikePlatformConstants.PLATFORM_AUTH_TOKEN, accessToken.hashCode());
						if (responseData.has(HikePlatformConstants.PLATFORM_AUTH_TOKEN_EXPIRY))
						{
							_mPrefs.saveData(HikePlatformConstants.PLATFORM_AUTH_TOKEN_EXPIRY, responseData.getString(HikePlatformConstants.PLATFORM_AUTH_TOKEN_EXPIRY));

						}
						Logger.d(tag, "access token recieved");
						_callBack.onTokenResponse(accessToken);

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
			Logger.d(tag, "auth request failed: "+httpException.getErrorCode());
			_callBack.onTokenErrorResponse(null);

		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			// TODO Auto-generated method stub

		}
	}

}
