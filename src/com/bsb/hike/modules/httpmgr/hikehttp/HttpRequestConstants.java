package com.bsb.hike.modules.httpmgr.hikehttp;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class HttpRequestConstants
{
	private static boolean isProduction = true;
	
	private static boolean isSSL = false;
	
	private static final String HTTP = "http://";

	//TODO change it to https
	private static final String HTTPS = "https://";

	private static final String PRODUCTION_API = "api.im.hike.in";
	
	private static final String STAGING_API = "staging.im.hike.in";

	private static final String PLATFORM_PRODUCTION_API = "platform.hike.in";

	private static final String PLATFORM_STAGING_API = "staging.platform.hike.in";
		
	public static final int STAGING_PORT = 80;
	
	public static final int PORT = STAGING_PORT;

	private static String BASE_URL = HTTP + PRODUCTION_API;

	private static String BASE_PLATFORM_URL = HTTP + PLATFORM_PRODUCTION_API;

	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";

	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

	private static final String BASE_INVITE = "/invite";

	private static final String BASE_CREATE = "/create";

	private static final String BASE_READ = "/read";

	private static final String BASE_ADDRESS_BOOK_READ = "/addressBook";

	public static synchronized void setUpBase()
	{
		toggleStaging();
		toggleSSL();
	}
	
	public static synchronized void toggleStaging()
	{
		isProduction = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION, true);
		changeBaseUrl();
		changeBasePlatformUrl();
	}

	public static synchronized void toggleSSL()
	{
		isSSL = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		changeBaseUrl();
		changeBasePlatformUrl();
	}
	
	private static void changeBaseUrl()
	{
		BASE_URL = "";
		BASE_URL += (isSSL) ? HTTPS : HTTP;
		BASE_URL += (isProduction) ? PRODUCTION_API : STAGING_API;
	}

	private static void changeBasePlatformUrl()
	{
		BASE_PLATFORM_URL = "";
		BASE_PLATFORM_URL += HTTP;
		BASE_PLATFORM_URL += (isProduction) ? PLATFORM_PRODUCTION_API : PLATFORM_STAGING_API;
	}
	
	
	
	/*********************************************************************************************************************************************/
	
	public static String singleStickerDownloadBase()
	{
		return BASE_URL + BASE_V1 + "/stickers";
	}
	
	public static String multiStickerDownloadUrl()
	{
		return BASE_URL + BASE_V1 + BASE_STICKER;
	}
	
	public static String lastSeenUrl()
	{
		return BASE_URL + BASE_V1 + BASE_USER + "/lastseen";
	}

	public static String bulkLastSeenUrl()
	{
		return BASE_URL + BASE_V2 + BASE_USER + "/bls";
	}
	
	public static String getStatusBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_USER + "/status";
	}

	public static String selfPlatformUidFetchUrl()
	{
		return BASE_PLATFORM_URL + BASE_USER + BASE_V1 + BASE_CREATE;
	}

	public static String platformUidForPartialAddressBookFetchUrl()
	{
		return BASE_PLATFORM_URL  + BASE_USER + BASE_V1 + BASE_READ;
	}

	public static String platformUIDForFullAddressBookFetchUrl()
	{
		return BASE_PLATFORM_URL  + BASE_USER +  BASE_V1 + BASE_ADDRESS_BOOK_READ;
	}

	public static String sendDeviceDetailBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/device";
	}

	public static String preActivationBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/pa";
	}
}
