package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.http.CustomSSLSocketFactory;
import com.bsb.hike.http.GzipByteArrayEntity;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;

public class AccountUtils
{

	public static final String HTTP_STRING = "http://";

	public static final String HTTPS_STRING = "https://";

	public static final String PRODUCTION_HOST = "api.im.hike.in";

	public static final String STAGING_HOST = "staging.im.hike.in";
	
	public static final String DEV_STAGING_HOST = "staging2.im.hike.in";
	
	public static final int _PRODUCTION_HOST = 0;

	public static final int _STAGING_HOST = 1;

	public static final int _DEV_STAGING_HOST = 2;
	
	public static final int _CUSTOM_HOST = 4;

	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 80;

	public static final int STAGING_PORT_SSL = 443;

	public static String host = PRODUCTION_HOST;

	public static int port = PRODUCTION_PORT;

	public static String base = HTTP_STRING + host + "/v1";

	public static String baseV2 = HTTP_STRING + host + "/v2";

	public static String fileTransferHost = HttpRequestConstants.FT_PRODUCTION_API;

	public static String fileTransferBase = HTTP_STRING + fileTransferHost + ":" + Integer.toString(port) + "/v1";

	public static final String FILE_TRANSFER_DOWNLOAD_BASE = "/user/ft/";

	public static String fileTransferBaseDownloadUrl = fileTransferBase + FILE_TRANSFER_DOWNLOAD_BASE;

	public static String fastFileUploadUrl = fileTransferBase + FILE_TRANSFER_DOWNLOAD_BASE + "ffu/";

	public static final String REWARDS_PRODUCTION_BASE = "hike.in/rewards/";

	public static final String REWARDS_STAGING_PATH = "/rewards/";

	public static String rewardsUrl = REWARDS_PRODUCTION_BASE;

	public static final String GAMES_PRODUCTION_BASE = "hike.in/games/";

	public static final String GAMES_STAGING_PATH = "/games/";

	public static String gamesUrl = GAMES_PRODUCTION_BASE;

	public static final String STICKERS_PRODUCTION_BASE = "hike.in/s/%1$s/%2$s";

	public static final String STICKERS_STAGING_PATH = "/s/%1$s/%2$s";

	public static String stickersUrl = HTTP_STRING + STICKERS_PRODUCTION_BASE;
	
	public static final String H2O_TUTORIAL_PRODUCTION_BASE = "hike.in/offlinedemo/";

	public static final String H2O_TUTORIAL_STAGING_PATH = "/offlinedemo/";

	public static String h2oTutorialUrl = HTTP_STRING + H2O_TUTORIAL_PRODUCTION_BASE;

	public static boolean ssl = false;

	public static HttpClient mClient = null;

	public static String mToken = null;

	public static String mUid = null;

	public static String appVersion = null;
	
	public static final String SDK_AUTH_BASE_URL_STAGING = "http://stagingoauth.im.hike.in/o/oauth2/";

	public static final String SDK_AUTH_BASE_URL_PROD = "http://oauth.hike.in/o/oauth2/";
	
	public static String SDK_AUTH_BASE = SDK_AUTH_BASE_URL_PROD;
	
	public static final String SDK_AUTH_PARAM_RESPONSE_TYPE = "response_type";
	
	public static final String SDK_AUTH_PARAM_CLIENT_ID = "client_id";
	
	public static final String SDK_AUTH_PARAM_SCOPE = "scope";
	
	public static final String SDK_AUTH_PARAM_PACKAGE_NAME = "package_name";
	
	public static final String SDK_AUTH_PARAM_SHA1 = "sha1";
	
	public static final String SDK_AUTH_EXPIRY_TYPE = "expiry_type";

	public static final String ANALYTICS_UPLOAD_PATH = "/logs/analytics";
	
	public static String analyticsUploadUrl = base + ANALYTICS_UPLOAD_PATH;
	
	public static void setToken(String token)
	{
		mToken = token;
	}

	public static void setUID(String uid)
	{
		mUid = uid;
	}

	public static void setAppVersion(String version)
	{
		appVersion = version;
	}

	public static String getAppVersion()
	{
		return appVersion;
	}

	public static synchronized HttpClient createClient()
	{
		if (mClient != null)
		{
			Logger.d("AccountUtils", "Socket timeout when http client already created = " + mClient.getParams().getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0));
			return mClient;
		}
		Logger.d("SSL", "Initialising the HTTP CLIENT");

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		/*
		 * set the connection timeout to 6 seconds, and the waiting for data timeout to 30 seconds
		 */
		HttpConnectionParams.setConnectionTimeout(params, HikeConstants.CONNECT_TIMEOUT);
		long so_timeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.GENERAL_SO_TIMEOUT, 180 * 1000l);
		Logger.d("AccountUtils", "Socket timeout while creating client = " + so_timeout);
		HttpConnectionParams.setSoTimeout(params, (int) so_timeout);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		if (ssl)
		{
			
				KeyStore dummyTrustStore;
				try
				{
					dummyTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					dummyTrustStore.load(null, null);
					SSLSocketFactory sf = new CustomSSLSocketFactory(dummyTrustStore);
					sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
					schemeRegistry.register(new Scheme("https", sf, port));
					schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), PRODUCTION_PORT));
					Logger.i("scheme", "all schemes "+schemeRegistry.getSchemeNames().toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return null;
				}
		}
		else
		{
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port));
		}

		ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
		HttpClient httpClient = new DefaultHttpClient(cm, params);
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + appVersion);

		mClient = httpClient;
		return httpClient;
	}

	public static void setSocketTimeout(int timeout)
	{
		if(mClient != null)
			mClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
	}

	public static HttpClient getClient(HttpRequestBase request)
	{
		HttpClient client = createClient();
		return client;
	}

	public static void addUserAgent(URLConnection urlConnection)
	{
		urlConnection.addRequestProperty("User-Agent", "android-" + appVersion);
	}

	public static JSONObject executeRequest(HttpRequestBase request)
	{
		setNoTransform(request);
		HttpClient client = getClient(request);
		HttpResponse response;
		try
		{
			Logger.d("HTTP", "Performing HTTP Request " + request.getRequestLine());
			Logger.d("HTTP", "to host" + request);
			response = client.execute(request);
			Logger.d("HTTP", "finished request");
			if (response.getStatusLine().getStatusCode() != 200)
			{
				Logger.w("HTTP", "Request Failed: " + response.getStatusLine());
				return null;
			}

			HttpEntity entity = response.getEntity();
			return getResponse(entity.getContent());
		}
		catch (ClientProtocolException e)
		{
			Logger.e("HTTP", "Invalid Response", e);
			e.printStackTrace();
		}
		catch (IOException e)
		{
			Logger.e("HTTP", "Unable to perform request", e);
		}
		return null;
	}

	public static JSONObject getResponse(InputStream is) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();
		CharBuffer target = CharBuffer.allocate(10000);
		int read = reader.read(target);
		while (read >= 0)
		{
			builder.append(target.array(), 0, read);
			target.clear();
			read = reader.read(target);
		}
		Logger.d("HTTP", "request finished");
		try
		{
			return new JSONObject(builder.toString());
		}
		catch (JSONException e)
		{
			Logger.e("HTTP", "Invalid JSON Response", e);
		}
		return null;
	}

	public static void addToken(HttpRequestBase req) throws IllegalStateException
	{
		assertIfTokenNull();
		if (TextUtils.isEmpty(mToken))
		{
			throw new IllegalStateException("Token is null");
		}
		req.addHeader("Cookie", "user=" + mToken + "; UID=" + mUid);
	}

	private static void assertIfTokenNull()
	{
		// Assert.assertTrue("Token is empty", !TextUtils.isEmpty(mToken));
	}

	public static void performRequest(HikeHttpRequest hikeHttpRequest, boolean addToken) throws NetworkErrorException, IllegalStateException
	{
		HttpRequestBase requestBase = null;
		AbstractHttpEntity entity = null;
		RequestType requestType = hikeHttpRequest.getRequestType();
		try
		{
			switch (requestType)
			{
				case OTHER:
					requestBase = new HttpPost(base + hikeHttpRequest.getPath());
					entity = new GzipByteArrayEntity(hikeHttpRequest.getPostData(), HTTP.DEFAULT_CONTENT_CHARSET);
					break;
			}
			if (addToken)
			{
				addToken(requestBase);
			}

			if (entity != null)
			{
				entity.setContentType(hikeHttpRequest.getContentType());
				((HttpPost) requestBase).setEntity(entity);
			}
			JSONObject obj = executeRequest(requestBase);
			Logger.d("AccountUtils", "Response: " + obj);
			if (((obj == null) || (!"ok".equals(obj.optString("stat")))))
			{
				throw new NetworkErrorException("Unable to perform request");
			}
			if (requestType == RequestType.OTHER)
			{
				hikeHttpRequest.setResponse(obj);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.wtf("AccountUtils", "Unable to encode name");
		}
	}

	public static void setNoTransform(URLConnection urlConnection)
	{
		urlConnection.setRequestProperty("Cache-Control", "no-transform");
	}

	public static void setNoTransform(HttpRequestBase request)
	{
		request.addHeader("Cache-Control", "no-transform");
	}
	
	/**
	 * 
	 * @param new_contacts_by_id
	 *            new entries to update with. These will replace contact IDs on the server
	 * @param ids_json
	 *            , these are ids that are no longer present and should be removed
	 * @return
	 */
	public static List<ContactInfo> updateAddressBook(Map<String, List<ContactInfo>> new_contacts_by_id, JSONArray ids_json) throws IllegalStateException
	{
		HttpPost request = new HttpPost(base + "/account/addressbook-update");
		addToken(request);
		JSONObject data = new JSONObject();

		try
		{
			data.put("remove", ids_json);
			data.put("update", ContactUtils.getJsonContactList(new_contacts_by_id, false));
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "Invalid JSON put", e);
			return null;
		}

		ArrayList<String> msisdnForMissingPlatformUID = ContactManager.getInstance().getMsisdnForMissingPlatformUID();

		if (msisdnForMissingPlatformUID != null && msisdnForMissingPlatformUID.size()>0)
		{
			PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.PARTIAL_ADDRESS_BOOK, msisdnForMissingPlatformUID.toArray(new String[] { }));
		}

		String encoded = data.toString();
		// try
		// {
		AbstractHttpEntity entity = new ByteArrayEntity(encoded.getBytes());
		request.setEntity(entity);
		entity.setContentType("application/json");
		JSONObject obj = executeRequest(request);
		if(obj == null)
		{
			recordAddressBookUploadFailException(data.toString());
		}
		return ContactUtils.getContactList(obj, new_contacts_by_id);
	}
	
	private static void recordAddressBookUploadFailException(String jsonString)
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EXCEPTION_ANALYTIS_ENABLED, false))
		{
			return;
		}
		try
		{
			JSONObject metadata = new JSONObject();

			metadata.put(HikeConstants.PAYLOAD, jsonString);

			Logger.d("AccountUtils", "recording addressbook upload fail event. json = " + jsonString);
			HAManager.getInstance().record(HikeConstants.EXCEPTION, HikeConstants.LogEvent.ADDRESSBOOK_UPLOAD, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
}
