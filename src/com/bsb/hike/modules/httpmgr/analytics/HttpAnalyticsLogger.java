package com.bsb.hike.modules.httpmgr.analytics;

import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.DEFAULT_HTTP_ANALYTICS;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_ANALYTICS_TYPE;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_EXCEPTION_ANALYTICS;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_METHOD_TYPE;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_PRODUCT_AREA;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_REQUEST_ANALYTICS_PARAM;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.HTTP_REQUEST_URL_FILTER;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.MAX_RANGE_HTTP_ANALYTICS;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.REQUEST_LOG_EVENT;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.RESPONSE_CODE;
import static com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants.RESPONSE_LOG_EVENT;

import java.net.URL;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class HttpAnalyticsLogger
{
	/**
	 * Returns true if request should be logged otherwise false based on the percentage i.e how much logging we want to do
	 * 
	 * @param requestAnalyticsKey
	 * @return
	 */
	public static boolean shouldSendLog(String requestAnalyticsKey)
	{
		if (TextUtils.isEmpty(requestAnalyticsKey))
		{
			return false;
		}

		int randomInt = new Random().nextInt(MAX_RANGE_HTTP_ANALYTICS);
		int maxAllowed = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PROB_NUM_HTTP_ANALYTICS, DEFAULT_HTTP_ANALYTICS);
		if (randomInt > maxAllowed)
		{
			return false;
		}
		return true;
	}

	/**
	 * Logs the http request analytics. Should be called just before request is executed
	 * 
	 * @param trackId
	 * @param requestUrl
	 */
	public static void logHttpRequest(String trackId, URL requestUrl, String methodType, String analyticsParam)
	{
		if (TextUtils.isEmpty(trackId))
		{
			return;
		}

		JSONObject metadata = new JSONObject();
		try
		{
			metadata = getHttpLogBasicJson(trackId, requestUrl, REQUEST_LOG_EVENT, methodType, analyticsParam);
			HAManager.getInstance().record(HTTP_ANALYTICS_TYPE, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, HTTP_ANALYTICS_TYPE);
		}
		catch (JSONException e)
		{
			LogFull.e("Exception occurred while sending request log : " + e);
		}
	}
	
	/**
	 * Logs the successful http response analytics. Should be called just after we receive the response from the server
	 * 
	 * @param trackId
	 * @param requestUrl
	 * @param responseCode
	 * @param methodType
	 * @param analyticsParam
	 */
	public static void logSuccessfullResponseReceived(String trackId, URL requestUrl, int responseCode, String methodType, String analyticsParam)
	{
		if (TextUtils.isEmpty(trackId))
		{
			return;
		}

		JSONObject metadata = new JSONObject();
		try
		{
			metadata = getHttpLogBasicJson(trackId, requestUrl, RESPONSE_LOG_EVENT, methodType, analyticsParam);
			metadata.put(RESPONSE_CODE, responseCode);
			HAManager.getInstance().record(HTTP_ANALYTICS_TYPE, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, HTTP_ANALYTICS_TYPE);
		}
		catch (JSONException e)
		{
			LogFull.e("Exception occurred while sending response received log : " + e);
		}
	}

	/**
	 * Logs the http response analytics. Should be called just after we receive the response from the server
	 * 
	 * @param trackId
	 * @param requestUrl
	 * @param responseCode
	 */
	public static void logResponseReceived(String trackId, URL requestUrl, int responseCode, String methodType, String analyticsParam)
	{
		logResponseReceived(trackId, requestUrl, responseCode, methodType, analyticsParam, null);
	}

	/**
	 * Logs the http response analytics. Should be called just after we receive the response from the server
	 * 
	 * @param trackId
	 * @param requestUrl
	 * @param responseCode
	 * @param methodType
	 * @param analyticsParam
	 * @param exception
	 */
	public static void logResponseReceived(String trackId, URL requestUrl, int responseCode, String methodType, String analyticsParam, String exception)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HTTP_EXCEPTION_LOGGING, false))
		{
			logDevException(requestUrl, responseCode, methodType, analyticsParam, exception);
		}

		if (TextUtils.isEmpty(trackId))
		{
			return;
		}

		JSONObject metadata = new JSONObject();
		try
		{
			metadata = getHttpLogBasicJson(trackId, requestUrl, RESPONSE_LOG_EVENT, methodType, analyticsParam);
			metadata.put(RESPONSE_CODE, responseCode);
			if (!TextUtils.isEmpty(exception))
			{
				metadata.put(HTTP_EXCEPTION_ANALYTICS, exception);
			}
			HAManager.getInstance().record(HTTP_ANALYTICS_TYPE, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, HTTP_ANALYTICS_TYPE);
		}
		catch (JSONException e)
		{
			LogFull.e("Exception occurred while sending response received log : " + e);
		}
	}

	/**
	 * Logs the dev exception for every error response of http request or some exception ocuurs
	 * 
	 * @param requestUrl
	 * @param responseCode
	 * @param methodType
	 * @param analyticsParam
	 * @param exception
	 */
	private static void logDevException(URL requestUrl, int responseCode, String methodType, String analyticsParam, String exception)
	{
		JSONObject info = null;
		try
		{
			if (!TextUtils.isEmpty(analyticsParam))
			{
				info = new JSONObject();
				info.put(HTTP_REQUEST_ANALYTICS_PARAM, analyticsParam);
			}

			if (!TextUtils.isEmpty(exception))
			{
				if (null == info)
				{
					info = new JSONObject();
				}
				info.put(HTTP_EXCEPTION_ANALYTICS, exception);
			}

			String devArea = processRequestUrl(requestUrl.toString()) + "_" + methodType + "_" + responseCode;
			HAManager.getInstance().logDevEvent(HTTP_PRODUCT_AREA, devArea, info);
		}
		catch (JSONException e)
		{
			LogFull.e("Exception occurred while logging dev exception log : " + e);
		}
	}

	/**
	 * This method returns a basic json object which contains track id, request analytics key , event stage and connection type for analytics log
	 * 
	 * @param trackId
	 * @param requestUrl
	 * @param logEventStage
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject getHttpLogBasicJson(String trackId, URL requestUrl, int logEventStage, String methodType, String analyticsParam) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put(AnalyticsConstants.TRACK_ID, trackId);
		json.put(HTTP_METHOD_TYPE, methodType);
		if (!TextUtils.isEmpty(analyticsParam))
		{
			json.put(HTTP_REQUEST_ANALYTICS_PARAM, analyticsParam);
		}
		json.put(AnalyticsConstants.EVENT_KEY, processRequestUrl(requestUrl.toString()));
		json.put(AnalyticsConstants.REL_EVENT_STAGE, logEventStage);
		json.put(AnalyticsConstants.CONNECTION_TYPE, Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext()));
		return json;
	}

	/**
	 * This will return filtered url for example if url is "im.hike.in/something" then this method returns "/something"
	 * 
	 * @param requestUrl
	 * @return
	 */
	private static String processRequestUrl(String requestUrl)
	{
		int index = requestUrl.indexOf(HTTP_REQUEST_URL_FILTER);
		if (index >= 0)
		{
			index += HTTP_REQUEST_URL_FILTER.length();
			if (index < requestUrl.length())
			{
				return new String(requestUrl.substring(index));
			}
		}
		return requestUrl;
	}
}
