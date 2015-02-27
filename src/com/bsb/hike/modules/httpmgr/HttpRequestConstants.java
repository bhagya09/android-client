package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class HttpRequestConstants
{
	private static boolean isProduction = true;
	
	private static boolean isSSL = false;
	
	private static final String HTTP = "http://";
	
	private static final String HTTPS = "https://";
	
	private static final String PRODUCTION_API = "api.im.hike.in";
	
	private static final String STAGING_API = "staging.im.hike.in";
	
	private static String BASE_URL = HTTP + PRODUCTION_API;
	
	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";

	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

	private static final String BASE_INVITE = "/invite";

	public static synchronized void setUpBase()
	{
		toggleStaging();
		toggleSSL();
	}
	
	public static synchronized void toggleStaging()
	{
		isProduction = HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.getInstance()).getData(HikeMessengerApp.PRODUCTION, true);
		changeBaseUrl();
	}

	public static synchronized void toggleSSL()
	{
		isSSL = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		changeBaseUrl();
	}
	
	private static void changeBaseUrl()
	{
		BASE_URL = "";
		BASE_URL += (isSSL) ? HTTPS : HTTP;
		BASE_URL += (isProduction) ? PRODUCTION_API : STAGING_API;
	}
	
	
	
	/*********************************************************************************************************************************************/
	
	public static String singleStickerDownloadBase()
	{
		return BASE_URL + BASE_V1 + "/stickers";
	}
}
