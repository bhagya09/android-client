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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import org.apache.http.message.BasicNameValuePair;
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
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.http.CustomSSLSocketFactory;
import com.bsb.hike.http.GzipByteArrayEntity;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.AccountInfo;
import com.bsb.hike.models.Birthday;
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

	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 80;

	public static final int STAGING_PORT_SSL = 443;

	public static String host = PRODUCTION_HOST;

	public static int port = PRODUCTION_PORT;

	public static String base = HTTP_STRING + host + "/v1";

	public static String baseV2 = HTTP_STRING + host + "/v2";

	public static final String PRODUCTION_FT_HOST = "ft.im.hike.in";

	public static String fileTransferHost = PRODUCTION_FT_HOST;

	public static String fileTransferBase = HTTP_STRING + fileTransferHost + ":" + Integer.toString(port) + "/v1";

	public static final String FILE_TRANSFER_DOWNLOAD_BASE = "/user/ft/";

	public static String fileTransferBaseDownloadUrl = fileTransferBase + FILE_TRANSFER_DOWNLOAD_BASE;
	
	public static String fastFileUploadUrl = fileTransferBase + FILE_TRANSFER_DOWNLOAD_BASE + "ffu/";

	public static String partialfileTransferBaseUrl = base + "/user/pft";

	public static final String FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION = "hike.in/f/";

	public static final String FILE_TRANSFER_BASE_VIEW_URL_STAGING = "staging.im.hike.in/f/";

	public static String fileTransferBaseViewUrl = FILE_TRANSFER_BASE_VIEW_URL_PRODUCTION;

	public static final String REWARDS_PRODUCTION_BASE = "hike.in/rewards/";

	public static final String REWARDS_STAGING_BASE = "staging.im.hike.in/rewards/";

	public static String rewardsUrl = REWARDS_PRODUCTION_BASE;

	public static final String GAMES_PRODUCTION_BASE = "hike.in/games/";

	public static final String GAMES_STAGING_BASE = "staging.im.hike.in/games/";

	public static String gamesUrl = GAMES_PRODUCTION_BASE;

	public static final String STICKERS_PRODUCTION_BASE = "hike.in/s/%1$s/%2$s";

	public static final String STICKERS_STAGING_BASE = "staging.im.hike.in/s/%1$s/%2$s";

	public static String stickersUrl = HTTP_STRING + STICKERS_PRODUCTION_BASE;
	
	public static final String H2O_TUTORIAL_PRODUCTION_BASE = "hike.in/offlinedemo/";

	public static final String H2O_TUTORIAL_STAGING_BASE = "staging.im.hike.in/offlinedemo/";

	public static String h2oTutorialUrl = HTTP_STRING + H2O_TUTORIAL_PRODUCTION_BASE;

	public static boolean ssl = false;

	public static final String NETWORK_PREFS_NAME = "NetworkPrefs";

	public static HttpClient mClient = null;

	public static String mToken = null;

	public static String mUid = null;

	public static String appVersion = null;
	
	public static final String SDK_AUTH_BASE_URL_STAGING = "http://stagingoauth.im.hike.in/o/oauth2/";

	public static final String SDK_AUTH_BASE_URL_PROD = "http://oauth.hike.in/o/oauth2/";
	
	public static String SDK_AUTH_BASE = SDK_AUTH_BASE_URL_PROD;
	
	public static final String SDK_AUTH_PATH_AUTHORIZE = "authorize";
	
	public static final String SDK_AUTH_PARAM_RESPONSE_TYPE = "response_type";
	
	public static final String SDK_AUTH_PARAM_CLIENT_ID = "client_id";
	
	public static final String SDK_AUTH_PARAM_SCOPE = "scope";
	
	public static final String SDK_AUTH_PARAM_PACKAGE_NAME = "package_name";
	
	public static final String SDK_AUTH_PARAM_SHA1 = "sha1";

	public static final String ANALYTICS_UPLOAD_BASE = "/logs/analytics";
	
	public static String analyticsUploadUrl = base + ANALYTICS_UPLOAD_BASE;
	
	public static String USER_DP_UPDATE_URL = "/account/avatar";
	
	public static String GROUP_DP_UPDATE_URL_PREFIX = "/group/";
	
	public static String GROUP_DP_UPDATE_URL_SUFFIX = "/avatar";
	
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

	public static void addUserAgent(HttpRequestBase request)
	{
		request.addHeader("User-Agent", "android-" + appVersion);
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

	public static AccountInfo registerAccount(Context context, String pin, String unAuthMSISDN)
	{
		HttpPost httppost = new HttpPost(base + "/account");
		AbstractHttpEntity entity = null;
		JSONObject data = new JSONObject();
		try
		{
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			String osVersion = Build.VERSION.RELEASE;
			String deviceId = "";

			try
			{
				deviceId = Utils.getHashedDeviceId(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
				Logger.d("AccountUtils", "Android ID is " + Secure.ANDROID_ID);
			}
			catch (NoSuchAlgorithmException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String os = HikeConstants.ANDROID;
			String carrier = manager.getNetworkOperatorName();
			String device = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = "";
			try
			{
				appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			}
			catch (NameNotFoundException e)
			{
				Logger.e("AccountUtils", "Unable to get app version");
			}

			String deviceKey = manager.getDeviceId();

			data.put("set_cookie", "0");
			data.put("devicetype", os);
			data.put(HikeConstants.LogEvent.OS, os);
			data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
			data.put("deviceid", deviceId);
			data.put("devicetoken", deviceId);
			data.put("deviceversion", device);
			data.put(HikeConstants.DEVICE_KEY, deviceKey);
			data.put("appversion", appVersion);
			data.put("invite_token", context.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE).getString("utm_source", ""));

			if (pin != null)
			{
				data.put("msisdn", unAuthMSISDN);
				data.put("pin", pin);
			}
			Utils.addCommonDeviceDetails(data, context);

			Logger.d("AccountUtils", "Creating Account " + data.toString());
			entity = new GzipByteArrayEntity(data.toString().getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.wtf("AccountUtils", "creating a string entity from an entry string threw!", e);
		}
		catch (JSONException e)
		{
			Logger.wtf("AccountUtils", "creating a string entity from an entry string threw!", e);
		}
		httppost.setEntity(entity);

		JSONObject obj = executeRequest(httppost);
		if ((obj == null))
		{
			Logger.w("HTTP", "Unable to create account");
			// raise an exception?
			return null;
		}

		Logger.d("AccountUtils", "AccountCreation " + obj.toString());
		if ("fail".equals(obj.optString("stat")))
		{
			AccountInfo accountInfo = new AccountInfo.Builder()
					.setToken(null)
					.setMsisdn(null)
					.setUid(null)
					.setBackupToken(null)
					.setSmsCredits(-1)
					.setAllInvitee(0)
					.setAllInviteJoined(0)
					.setCountryCode(null)
					.build();
			return accountInfo;
		}
		String token = obj.optString("token");
		String msisdn = obj.optString("msisdn");
		String uid = obj.optString("uid");
		String backupToken = obj.optString("backup_token");
		int smsCredits = obj.optInt(HikeConstants.MqttMessageTypes.SMS_CREDITS);
		int all_invitee = obj.optInt(HikeConstants.ALL_INVITEE_2);
		int all_invitee_joined = obj.optInt(HikeConstants.ALL_INVITEE_JOINED_2);
		String country_code = obj.optString("country_code");

		Logger.d("HTTP", "Successfully created account token:" + token + "msisdn: " + msisdn + " uid: " + uid + "backup_token: " + backupToken);
		return new AccountInfo.Builder()
				.setToken(token)
				.setMsisdn(msisdn)
				.setUid(uid)
				.setBackupToken(backupToken)
				.setSmsCredits(smsCredits)
				.setAllInvitee(all_invitee)
				.setAllInviteJoined(all_invitee_joined)
				.setCountryCode(country_code)
				.build();
	}

	public static String validateNumber(String number)
	{
		HttpPost httppost = new HttpPost(base + "/account/validate?digits=4");
		AbstractHttpEntity entity = null;
		JSONObject data = new JSONObject();
		try
		{
			data.put("phone_no", number);
			entity = new GzipByteArrayEntity(data.toString().getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "creating a string entity from an entry string threw!", e);
		}

		JSONObject obj = executeRequest(httppost);
		if (obj == null)
		{
			Logger.w("HTTP", "Unable to Validate Phone Number.");
			// raise an exception?
			return null;
		}

		String msisdn = obj.optString("msisdn");
		Logger.d("HTTP", "Successfully validated phone number.");
		return msisdn;
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
	
	public static void addTokenForAuthReq(HttpRequestBase req) throws IllegalStateException
	{
		assertIfTokenNull();
		if (TextUtils.isEmpty(mToken))
		{
			throw new IllegalStateException("Token is null");
		}
		req.addHeader(new BasicHeader("cookie", "uid="+mUid+";token="+mToken));
	}

	private static void assertIfTokenNull()
	{
		// Assert.assertTrue("Token is empty", !TextUtils.isEmpty(mToken));
	}

	public static JSONObject setProfile(String name, Birthday birthdate, boolean isFemale) throws NetworkErrorException, IllegalStateException
	{
		HttpPost httppost = new HttpPost(base + "/account/profile");
		addToken(httppost);
		JSONObject data = new JSONObject();

		try
		{
			data.put("name", name);
			data.put("gender", isFemale ? "f" : "m");
			if (birthdate != null)
			{
				JSONObject bday = new JSONObject();
				if(birthdate.day != 0)
				{
					bday.put("day", birthdate.day);
				}
				if(birthdate.month != 0)
				{
					bday.put("month", birthdate.month);
				}
				bday.put("year", birthdate.year);
				data.put("dob", bday);
			}
			data.put("screen", "signup");

			AbstractHttpEntity entity = new GzipByteArrayEntity(data.toString().getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
			entity.setContentType("application/json");
			httppost.setEntity(entity);
			JSONObject obj = executeRequest(httppost);
			if ((obj == null) || (!"ok".equals(obj.optString("stat"))))
			{
				throw new NetworkErrorException("Unable to set name");
			}
			return obj;
		}
		catch (JSONException e)
		{
			Logger.wtf("AccountUtils", "Unable to encode name as JSON");
			return null;
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.wtf("AccountUtils", "Unable to encode name");
			return null;
		}
	}

	public static JSONObject postAddressBook(String token, Map<String, List<ContactInfo>> contactsMap) throws IllegalStateException, IOException
	{
		HttpPost httppost = new HttpPost(base + "/account/addressbook");
		addToken(httppost);
		JSONObject data;
		data = ContactUtils.getJsonContactList(contactsMap, true);
		if (data == null)
		{
			return null;
		}
		String encoded = data.toString();

		Logger.d("ACCOUNT UTILS", "Json data is : " + encoded);
		AbstractHttpEntity entity = new GzipByteArrayEntity(encoded.getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
		entity.setContentType("application/json");
		httppost.setEntity(entity);
		JSONObject obj = executeRequest(httppost);
		return obj;
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

	public static JSONObject getJsonContactList(Map<String, List<ContactInfo>> contactsMap, boolean sendWAValue)
	{
		JSONObject updateContacts = new JSONObject();
		for (String id : contactsMap.keySet())
		{
			try
			{
				List<ContactInfo> list = contactsMap.get(id);
				JSONArray contactInfoList = new JSONArray();
				for (ContactInfo cInfo : list)
				{
					JSONObject contactInfo = new JSONObject();
					contactInfo.put("name", cInfo.getName());
					contactInfo.put("phone_no", cInfo.getPhoneNum());
					if (sendWAValue)
					{
						contactInfo.put("t1", ContactUtils.calculateGreenBlueValue(cInfo.isOnGreenBlue()));
					}
					contactInfoList.put(contactInfo);
				}
				updateContacts.put(id, contactInfoList);
			}
			catch (JSONException e)
			{
				Logger.d("ACCOUNT UTILS", "Json exception while getting contact list.");
				e.printStackTrace();
			}
		}
		return updateContacts;
	}

	public static JSONObject getWAJsonContactList(List<ContactInfo> contactsList)
	{
		JSONObject contactsJson = new JSONObject();
		try
		{
			for (ContactInfo cInfo : contactsList)
			{
				JSONObject waInfoObject = new JSONObject();
				waInfoObject.put("t1", ContactUtils.calculateGreenBlueValue(cInfo.isOnGreenBlue()));
				contactsJson.put(cInfo.getMsisdn(), waInfoObject);
			}

		}
		catch (JSONException e)
		{
			Logger.d("ACCOUNT UTILS", "Json exception while getting WA info list.");
			e.printStackTrace();
		}
		return contactsJson;
	}

	public static List<ContactInfo> getContactList(JSONObject obj, Map<String, List<ContactInfo>> new_contacts_by_id)
	{
		List<ContactInfo> server_contacts = new ArrayList<ContactInfo>();
		JSONObject addressbook;
		try
		{
			if ((obj == null) || ("fail".equals(obj.optString("stat"))))
			{
				Logger.w("HTTP", "Unable to upload address book");
				// TODO raise a real exception here
				return null;
			}
			Logger.d("AccountUtils", "Reply from addressbook:" + obj.toString());
			addressbook = obj.getJSONObject("addressbook");
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "Invalid json object", e);
			return null;
		}

		for (Iterator<?> it = addressbook.keys(); it.hasNext();)
		{
			String id = (String) it.next();
			JSONArray entries = addressbook.optJSONArray(id);
			List<ContactInfo> cList = new_contacts_by_id.get(id);
			for (int i = 0; i < entries.length(); ++i)
			{
				JSONObject entry = entries.optJSONObject(i);
				String msisdn = entry.optString("msisdn");
				boolean onhike = entry.optBoolean("onhike");
				String platformId = entry.optString(HikePlatformConstants.PLATFORM_USER_ID);
				ContactInfo info = new ContactInfo(id, msisdn, cList.get(i).getName(), cList.get(i).getPhoneNum(), onhike, platformId);
				server_contacts.add(info);
			}
		}
		return server_contacts;
	}

	public static List<String> getBlockList(JSONObject obj)
	{
		JSONArray blocklist;
		List<String> blockListMsisdns = new ArrayList<String>();
		if ((obj == null) || ("fail".equals(obj.optString("stat"))))
		{
			Logger.w("HTTP", "Unable to upload address book");
			// TODO raise a real exception here
			return null;
		}
		Logger.d("AccountUtils", "Reply from addressbook:" + obj.toString());
		blocklist = obj.optJSONArray("blocklist");
		if (blocklist == null)
		{
			Logger.e("AccountUtils", "Received blocklist as null");
			return null;
		}

		for (int i = 0; i < blocklist.length(); i++)
		{
			try
			{
				blockListMsisdns.add(blocklist.getString(i));
			}
			catch (JSONException e)
			{
				Logger.e("AccountUtils", "Invalid json object", e);
				return null;
			}
		}
		return blockListMsisdns;
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
			case PROFILE_PIC:
				requestBase = new HttpPost(base + hikeHttpRequest.getPath());
				/*
				 * Adding MD5 header to validate the file at server side.
				 */
				String fileMd5 = Utils.fileToMD5(hikeHttpRequest.getFilePath());
				requestBase.addHeader("Content-MD5", fileMd5);
				entity = new FileEntity(new File(hikeHttpRequest.getFilePath()), "");
				break;

			case STATUS_UPDATE:			
			case SOCIAL_POST:
			case OTHER:
				requestBase = new HttpPost(base + hikeHttpRequest.getPath());
				entity = new GzipByteArrayEntity(hikeHttpRequest.getPostData(), HTTP.DEFAULT_CONTENT_CHARSET);
				break;
				

			case DELETE_STATUS:
			case DELETE_DP:
				requestBase = new HttpDelete(base + hikeHttpRequest.getPath());
				break;

			case HIKE_JOIN_TIME:
				requestBase = new HttpGet(base + hikeHttpRequest.getPath());
				break;

			case PREACTIVATION:
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
			if (((obj == null) || (!"ok".equals(obj.optString("stat"))) && requestType != RequestType.HIKE_JOIN_TIME))
			{
				throw new NetworkErrorException("Unable to perform request");
			}
			if (requestType == RequestType.PREACTIVATION)
			{
				hikeHttpRequest.setResponse(obj);

			}
			else
			/*
			 * We need the response to save the id of the status.
			 */
			if (requestType == RequestType.STATUS_UPDATE || requestType == RequestType.HIKE_JOIN_TIME || requestType == RequestType.PROFILE_PIC
					|| requestType == RequestType.SOCIAL_POST || requestType == RequestType.OTHER)
			{
				hikeHttpRequest.setResponse(obj);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			Logger.wtf("AccountUtils", "Unable to encode name");
		}
	}

	public static int getBytesUploaded(String sessionId) throws ClientProtocolException, IOException
	{
		int val = 0;
		HttpRequestBase req = new HttpGet(AccountUtils.fileTransferBase + "/user/pft/");
		addToken(req);
		req.addHeader("X-SESSION-ID", sessionId);
		AccountUtils.setNoTransform(req);
		HttpClient httpclient = getClient(req);
		HttpResponse response = httpclient.execute(req);
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() == HttpStatus.SC_OK)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			response.getEntity().writeTo(out);
			out.close();
			String responseString = out.toString();
			return Integer.parseInt(responseString) + 1;
		}
		else
		{
			// Closes the connection.
			response.getEntity().getContent().close();
		}
		return val;
	}

	public static void setNoTransform(URLConnection urlConnection)
	{
		urlConnection.setRequestProperty("Cache-Control", "no-transform");
	}

	public static void setNoTransform(HttpRequestBase request)
	{
		request.addHeader("Cache-Control", "no-transform");
	}
}
