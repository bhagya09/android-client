package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.AppConfig;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.engine.HttpEngine;
import com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier;
import com.bsb.hike.modules.httpmgr.engine.RequestProcessor;
import com.bsb.hike.modules.httpmgr.log.HttpLogger;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.log.LogHttp;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestState;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will be used for initialization by and outside world and for adding or canceling a request by {@link RequestToken}
 * 
 * @author anubhav & sidharth
 * 
 */
public class HttpManager
{
	private static volatile HttpManager _instance;

	private static RequestProcessor requestProcessor;

	private static List<String> productionHostUris;

	private static List<String> platformProductionHostUris;

	private static List<String> ftHostUris;

	private HttpManager(ClientOptions options)
	{
		if (AppConfig.DEBUG_LOGS_ENABLED)
		{
			HttpLogger.plant(new LogFull("Http"));
			HttpLogger.plant(new LogHttp("Http"));
		}
		setHostUris();
		HttpEngine engine = new HttpEngine();
		RequestListenerNotifier notifier = new RequestListenerNotifier(engine);
		requestProcessor = new RequestProcessor(options, engine, notifier);
	}

	public static HttpManager getInstance()
	{
		if (_instance == null)
		{
			throw new IllegalStateException("Http Manager not initialized");
		}
		return _instance;
	}

	/**
	 * Initializes the http manager with default {@link ClientOptions}
	 */
	public static void init()
	{
		init(null);
	}

	private void setHostUris()
	{
		setProductionHostUris();
		setPlatformProductionHostUris();
		setFtHostUris();
	}

	public static void setProductionHostUris()
	{
		JSONArray ipArray = null;
		if (HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.HTTP_HOST_IPS))
		{
			String ipString = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HTTP_HOST_IPS, "");
			try
			{
				ipArray = new JSONArray(ipString);
			}
			catch (JSONException e)
			{
				LogFull.e("Exception while parsing : " + e);
			}
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			int len = ipArray.length();
			productionHostUris = new ArrayList<String>(len);
			for (int i = 0; i < len; i++)
			{
				if (ipArray.optString(i) != null)
				{
					productionHostUris.add(ipArray.optString(i));
				}
			}
		}
		else
		{
			productionHostUris = new ArrayList<String>();
			productionHostUris.add("54.169.191.114");
			productionHostUris.add("54.169.191.115");
			productionHostUris.add("54.169.191.116");
			productionHostUris.add("54.169.191.113");
		}
	}

	public static void setPlatformProductionHostUris()
	{
		platformProductionHostUris = new ArrayList<String>();
		String ipString = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HTTP_HOST_PLATFORM_IPS, "");
		JSONArray ipArray = null;
		try
		{
			ipArray = new JSONArray(ipString);
		}
		catch (JSONException e)
		{
			LogFull.e("Exception while parsing = ", e);
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			int len = ipArray.length();
			for (int i = 0; i < len; i++)
			{
				if (ipArray.optString(i) != null)
				{
					platformProductionHostUris.add(ipArray.optString(i));
				}
			}
		}
		else
		{
			platformProductionHostUris.add("54.169.191.117");
			platformProductionHostUris.add("54.169.191.118");
		}
	}

	public static List<String> getProductionHostUris()
	{
		return productionHostUris;
	}

	public static List<String> getPlatformProductionHostUris()
	{
		return platformProductionHostUris;
	}

	public static List<String> getFtHostUris() {
		return ftHostUris;
	}

	public static void setFtHostUris()
	{
		ftHostUris = new ArrayList<>();
		String ipString = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FT_HOST_IPS, "");
		JSONArray ipArray = null;

		try
		{
			if (!TextUtils.isEmpty(ipString)) {
				ipArray = new JSONArray(ipString);
			}
		}
		catch (JSONException e)
		{
			LogFull.d("Exception while parsing = " + e);
			e.printStackTrace();
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			int len = ipArray.length();
			for (int i = 0; i < len; i++)
			{
				if (ipArray.optString(i) != null)
				{
					LogFull.d("FT host api[" + i + "] = " + ipArray.optString(i));
					ftHostUris.add(ipArray.optString(i));
				}
			}
		}
		else
		{
			ftHostUris.add("54.169.191.114");
			ftHostUris.add("54.169.191.115");
			ftHostUris.add("54.169.191.116");
			ftHostUris.add("54.169.191.113");
		}
	}

	/**
	 * Initializes the http manager with {@link ClientOptions} passed as parameter
	 * 
	 * @param options
	 */
	public static void init(ClientOptions options)
	{
		if (_instance == null)
		{
			synchronized (HttpManager.class)
			{
				if (_instance == null)
				{
					_instance = new HttpManager(options);
				}
			}
		}
	}

	/**
	 * Submits the request to {@link RequestProcessor}
	 * 
	 * @param request
	 */
	<T> void addRequest(Request<T> request)
	{
		addRequest(request, null);
	}

	/**
	 * Submits the request to {@link RequestProcessor} with client options
	 * 
	 * @param request
	 * @param options
	 */
	<T> void addRequest(Request<T> request, ClientOptions options)
	{
		requestProcessor.addRequest(request, options);
	}

	/**
	 * Cancels the request
	 * 
	 * @param request
	 */
	<T> void cancel(Request<T> request)
	{
		request.cancel();
	}

	<T> void addRequestListener(Request<T> request, IRequestListener listener)
	{
		request.addRequestListeners(listener);
	}

	/**
	 * Removes particular listener from list of listeners for a request
	 * 
	 * @param request
	 * @param listener
	 */
	<T> void removeListener(Request<T> request, IRequestListener listener)
	{
		List<IRequestListener> listeners = new ArrayList<IRequestListener>(1);
		listeners.add(listener);

		request.removeRequestListeners(listeners);
	}

	/**
	 * Removes list of listeners from list of request listeners for a request
	 * 
	 * @param request
	 * @param listeners
	 */
	<T> void removeListeners(Request<T> request, List<IRequestListener> listeners)
	{
		request.removeRequestListeners(listeners);
	}

	/**
	 * Determines whether a request is running or not
	 * 
	 * @param request
	 * @return
	 */
	<T> boolean isRequestRunning(Request<T> request)
	{
		return requestProcessor.isRequestRunning(request);
	}

	public FileSavedState getRequestStateFromDB(String url, String defaultId)
	{
		String input = url + defaultId;
		String requestId = HttpUtils.calculateMD5hash(input);
		HttpRequestState state = HttpRequestStateDB.getInstance().getRequestState(requestId);
		if (state == null)
		{
			return null;
		}
		JSONObject metadata = state.getMetadata();
		return FileSavedState.getFileSavedStateFromJSON(metadata);
	}

	public void deleteRequestStateFromDB(String url, String defaultId)
	{
		String input = url + defaultId;
		String requestId = HttpUtils.calculateMD5hash(input);
		HttpRequestStateDB.getInstance().deleteState(requestId);
	}

	public void saveRequestStateInDB(String url, String defaultId, FileSavedState fss)
	{
		String input = url + defaultId;
		String requestId = HttpUtils.calculateMD5hash(input);
		HttpRequestState state = new HttpRequestState(requestId);
		state.setMetadata(fss.toJSON());
		HttpRequestStateDB.getInstance().insertOrReplaceRequestState(state);
	}

	/**
	 * Shutdown method to close everything (setting all variables to null for easy garbage collection)
	 */
	public static void shutdown()
	{
		requestProcessor.shutdown();
		requestProcessor = null;
		_instance = null;
	}
}
