package com.bsb.hike.modules.httpmgr.hikehttp;

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

	private static final String PLATFORM_PRODUCTION_API = "platform.hike.in";

	private static final String PLATFORM_STAGING_API = "staging.platform.hike.in";
	
	private static final String STICKERS_PRODUCTION_API = "stickers.im.hike.in";
		
	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 80;

	public static final int STAGING_PORT_SSL = 443;

	public static final int PORT = STAGING_PORT;

	private static String BASE_URL = HTTP + PRODUCTION_API;

	private static String BASE_PLATFORM_URL = HTTP + PLATFORM_PRODUCTION_API;
	
	private static String BASE_STICKERS_URL = HTTP + STICKERS_PRODUCTION_API;

	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";

	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

	private static final String BASE_INVITE = "/invite";
	
	private static final String BASE_SDK_PROD = "oauth.hike.in/o/oauth2/";
	
	public static final String BASE_SDK_STAGING = "stagingoauth.im.hike.in/o/oauth2/";
	
	private static String BASE_SDK = HTTP + BASE_SDK_PROD;

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
		changeBaseStickersUrl();
	}

	public static synchronized void toggleSSL()
	{
		isSSL = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		changeBaseUrl();
		changeBasePlatformUrl();
		changeBaseStickersUrl();
	}

	private static void changeBaseUrl()
	{
		BASE_URL = "";
		BASE_URL += (isSSL) ? HTTPS : HTTP;
		BASE_URL += (isProduction) ? PRODUCTION_API : STAGING_API;
		
		BASE_SDK = "";
		BASE_SDK += (isSSL) ? HTTPS : HTTP;
		BASE_SDK += (isProduction) ? BASE_SDK_PROD : BASE_SDK_STAGING;
	}

	private static void changeBasePlatformUrl()
	{
		BASE_PLATFORM_URL = "";
		BASE_PLATFORM_URL += HTTP;
		BASE_PLATFORM_URL += (isProduction) ? PLATFORM_PRODUCTION_API : PLATFORM_STAGING_API;
	}
	
	private static void changeBaseStickersUrl()
	{
		BASE_STICKERS_URL = "";
		BASE_STICKERS_URL += (isSSL) ? HTTPS : HTTP;
		BASE_STICKERS_URL += (isProduction) ? STICKERS_PRODUCTION_API : STAGING_API;
	}
	
	
	
	/*********************************************************************************************************************************************/

	public static String singleStickerDownloadBase()
	{
		return BASE_STICKERS_URL + BASE_V1 + "/stickers";
	}

	public static String multiStickerDownloadUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER;
	}
	
	public static String stickerPalleteImageDownloadUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/enable_disable";
	}
	
	public static String stickerPreviewImageDownloadUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/preview";
	}
	
	public static String stickerShopDownloadUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/shop";
	}
	
	public static String stickerSignupUpgradeUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/categories";
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

	public static String postAddressbookBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/addressbook";
	}

	public static String updateAddressbookBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/addressbook-update";
	}

	public static String postDeviceDetailsBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/update";
	}

	public static String postGreenBlueDetailsBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/info";
	}

	public static String sendUserLogsInfoBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/";
	}

	public static String deleteAccountBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT;
	}

	public static String unlinkAccountBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/unlink";
	}

	public static String getGroupBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/group/";
	}

	public static String getAvatarBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/avatar";
	}

	public static String getStaticAvatarBaseUrl()
	{
		return BASE_URL + ":" + PORT + "/static/avatars/";
	}

	public static String editProfileAvatarBase()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/avatar";
	}

	public static String authSDKBaseUrl()
	{
		return BASE_SDK;
	}

	public static String groupProfileBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/group/";
	}

	public static String getHikeJoinTimeBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile/";
	}

	public static String registerAccountBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT;
	}
	
	public static String validateNumberBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/validate";
	}
	
	public static String setProfileUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile";
	}
}
