package com.bsb.hike.modules.httpmgr.hikehttp;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class HttpRequestConstants
{
	private static boolean isProduction = true;

	private static boolean isSSL = false;

	private static final String HTTP = "http://";

	private static final String HTTPS = "https://";

	public static final String PRODUCTION_API = "api.im.hike.in";

	public static final String STAGING_API = "staging.im.hike.in";

	public static final String PLATFORM_PRODUCTION_API = "platform.hike.in";

	public static final String PLATFORM_STAGING_API = "staging.platform.hike.in";
	
	public static final String STICKERS_PRODUCTION_API = "stickers.im.hike.in";

	//TODO CHATTHEME Revisit PRODUCTION URL
	public static final String CHATTHEME_PRODUCTION_API = "";

	public static final String FT_PRODUCTION_API = "ft.im.hike.in";

	public static final String STICKERS_CDN_PRODUCTION_API = "staticstickers.im.hike.in";
	
	public static final String BASE_LINK_SHARING_URL = HTTP + "hike.in";
		
	public static final int PRODUCTION_PORT = 80;

	public static final int PRODUCTION_PORT_SSL = 443;

	public static final int STAGING_PORT = 80;

	public static final int STAGING_PORT_SSL = 443;

	public static final int PORT = STAGING_PORT;

	private static String BASE_URL = HTTP + PRODUCTION_API;

	private static String BASE_PLATFORM_URL = HTTP + PLATFORM_PRODUCTION_API;
	
	private static String BASE_STICKERS_URL = HTTP + STICKERS_PRODUCTION_API;

	private static String BASE_STICKERS_CDN_URL = HTTP + STICKERS_CDN_PRODUCTION_API;

	//TODO CHATTHEME change the URL to Production
	private static String BASE_CHATTHEME_URL = HTTP + STAGING_API;

	private static final String BASE_V1 = "/v1";

	private static final String BASE_V2 = "/v2";
	
	private static final String BASE_V3 = "/v3";

	private static final String BASE_V4 = "/v4";

	private static final String BASE_V5 = "/v5";

	private static final String BASE_ACCOUNT = "/account";

	private static final String BASE_USER = "/user";

	private static final String BASE_STICKER = "/stickers";

	private static final String BASE_CHATTHEME = "/cbg";

	private static final String BASE_SHOP = "/shop";

	private static final String BASE_INVITE = "/invite";
	
	private static final String BASE_SDK_PROD = "oauth.hike.in/o/oauth2/";
	
	public static final String BASE_SDK_STAGING = "http://stagingoauth.im.hike.in/o/oauth2/";
	
	public static final String BASE_AUTH_SDK_STAGING = "http://stagingoauth.im.hike.in/o/oauth2/";
	
	public static final String BASE_AUTH_SDK_PROD = "http://oauth.platform.hike.in/o/oauth2/";

	private static String BASE_AUTH_SDK = HTTP + BASE_AUTH_SDK_PROD;
	
	private static String BASE_SDK = HTTP + BASE_SDK_PROD;

	private static final String BASE_CREATE = "/create";

	private static final String BASE_READ = "/read";

	private static final String BASE_ADDRESS_BOOK_READ = "/addressBook";
	
	private static final String STICKER_SHARE_PATH = "/stickershare/" ;
	
	private static final String QA_CONTENT = "qa-content.hike.in";

	private static final String ANONYMOUS_NAME = "/anonymousName";

	private static final String STAGING_HIKECALLER_API = "http://52.76.46.27:5000";

    private static final String ANALYTICS_UPLOAD_PATH = "/logs/analytics";
	
	private static final String PRODUCTION_HIKECALLER_API = "https://caller.hike.in";

	private static final String BASE_NAME = "/name";

	private static final String BASE_BLOCK = "/block";

	private static final String BASE_BLOCKED_LIST = "/blocked_list";

	private static final String HIKE_SETTINGS = "/hikesettings";

	private static final String  FETCH_TODAYS_BIRTHDAY_URL = "/events/birthday";

	private static final String PREF_PATH = "/pref";

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
		changeBaseAuthUrl();
		changeChatThemeUrl();
	}

	public static synchronized void toggleSSL()
	{
		isSSL = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		changeBaseUrl();
		changeBasePlatformUrl();
		changeBaseStickersUrl();
		changeBaseAuthUrl();
		changeChatThemeUrl();
	}

	private static void changeBaseAuthUrl()
	{
		BASE_AUTH_SDK = "";
		BASE_AUTH_SDK += (isSSL) ? HTTPS : HTTP;
		BASE_AUTH_SDK += (isProduction) ? BASE_AUTH_SDK_PROD : BASE_AUTH_SDK_STAGING;
	}
	private static void changeBaseUrl()
	{
		BASE_URL = "";
		BASE_URL += (isSSL) ? HTTPS : HTTP;
		
		if(isProduction)
		{
			BASE_URL += PRODUCTION_API;
		}
		else
		{
			//get staging or custom staging url
			setupStagingUrl();
		}
		
		BASE_SDK = "";
		BASE_SDK += (isSSL) ? HTTPS : HTTP;
		BASE_SDK += (isProduction) ? BASE_SDK_PROD : BASE_SDK_STAGING;
	}
	
	private static void setupStagingUrl()
	{
		int whichServer = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, AccountUtils._STAGING_HOST);
		if (whichServer == AccountUtils._CUSTOM_HOST)
		{
			String host = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.CUSTOM_HTTP_HOST, AccountUtils.PRODUCTION_HOST);
			int port = AccountUtils.port = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.CUSTOM_HTTP_PORT, AccountUtils.PRODUCTION_PORT);
			// all custom ip request should point to http and not https
			BASE_URL = HTTP + host + ":" + Integer.toString(port);

		}
		else
		{
			BASE_URL += STAGING_API; // staging host
		}
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

		BASE_STICKERS_CDN_URL = "";
		BASE_STICKERS_CDN_URL += (isSSL) ? HTTPS : HTTP;
		BASE_STICKERS_CDN_URL += (isProduction) ? STICKERS_CDN_PRODUCTION_API : STAGING_API;
	}

	//TODO CHATTHEME Revisit PRODUCTION URL
	private static void changeChatThemeUrl()
	{
		BASE_CHATTHEME_URL = "";
		BASE_CHATTHEME_URL += HTTP;
		//BASE_CHATTHEME_URL += (isProduction) ? CHATTHEME_PRODUCTION_API : CHATTHEME_STAGING_API;
		BASE_CHATTHEME_URL += STAGING_API;
	}

	/*********************************************************************************************************************************************/
	public static String chatThemeBgImgUploadBase()
	{
		return BASE_CHATTHEME_URL + BASE_V1 + BASE_CHATTHEME + "/custom";
	}

	public static String chatThemeAssetsDownloadBase()
	{
		return BASE_CHATTHEME_URL + BASE_V1 + BASE_CHATTHEME + "/assets";
	}

	public static String chatThemeAssetIdDownloadBase()
	{
		return BASE_CHATTHEME_URL + BASE_V1 + BASE_CHATTHEME + "/prop";
	}

	public static String singleStickerDownloadBase()
	{
		return BASE_STICKERS_URL + BASE_V3 + BASE_STICKER;
	}

	public static String singleStickerImageDownloadBase()
	{
		return BASE_STICKERS_CDN_URL + BASE_V4 + BASE_STICKER + "/image";
	}

	public static String multiStickerDownloadUrl()
	{
		return BASE_STICKERS_URL + BASE_V3 + BASE_STICKER;
	}

	public static String multiStickerImageDownloadUrl()
	{
		return BASE_STICKERS_CDN_URL + BASE_V4 + BASE_STICKER + "/image";
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
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + BASE_SHOP;
	}


	public static String stickerCategoryFetchPrefOrderUrl()
	{
		return BASE_STICKERS_URL + BASE_V4 + BASE_SHOP + "/fetch_shop_order";
	}

	public static String stickerShopFetchCategoryUrl()
	{
		return BASE_STICKERS_URL + BASE_V4 + BASE_SHOP + "/update_metadata";
	}

	public static String stickerShopFetchCategoryTagsUrl()
	{
		return BASE_STICKERS_URL + BASE_V4 + BASE_SHOP + "/update_tags";
	}

    public static String stickerSignupUpgradeUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/categories";
	}
	
	public static String latestApkInfoUrl()
	{
		return BASE_URL + BASE_V1 + "/latest-apk-info";
	}
	
	public static String getStickerTagsUrl()
	{
		return BASE_STICKERS_URL + BASE_V3 + BASE_STICKER + "/tagdata";
	}

	public static String singleStickerTagsUrl()
	{
		return BASE_STICKERS_URL + BASE_V4 + BASE_STICKER + "/tags";
	}

	public static String getForcedStickersUrl()
	{
		return BASE_STICKERS_URL + BASE_V4 + BASE_STICKER + "/force_stickers";
	}
	
	public static String stickerCategoryDetailsUrl()
	{
		return BASE_STICKERS_URL + BASE_V1 + BASE_STICKER + "/categories";
	}

	public static String quickSuggestionUrl()
	{
		return HTTP + "54.251.141.175:8080" + "/quickSuggestions";
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

	public static String getDeleteStatusBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_USER + "/status-delete";
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

	public static String postAddressbookBaseV3Url()
	{
		return BASE_URL + BASE_V3 + BASE_ACCOUNT + "/addressbook";
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
		if(!Utils.isUserAuthenticated(HikeMessengerApp.getInstance().getApplicationContext()))
		{
			return BASE_URL + BASE_V1 + "/pa/";
		}
		else
		{
			return BASE_URL + BASE_V1 +  "/";
		}
	}

	public static String deleteAccountBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/account-delete";
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
		return BASE_SDK_STAGING;
	}
	
	
	public static String authBaseUrl()
	{
		return BASE_AUTH_SDK+"authorize" + "?" ;
	}

	public static String registerBrandUrl()
	{
		return BASE_SDK_STAGING+"registerbrand";
	}
	
	public static String clientIdUrl()
	{
		return BASE_SDK_STAGING+"createnewclientid";
	}
	public static String groupProfileBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/group/";
	}

	public static String getHikeJoinTimeBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile/";
	}

	public static String getHikeJoinTimeBaseV2Url()
	{
		return BASE_URL + BASE_V2 + BASE_ACCOUNT + "/profile/";
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
	
	public static String updateUnLoveLinkUrl()
	{
		return BASE_URL + BASE_V1 + "/status/unlove";
	}
	
	public static String updateLoveLinkUrl()
	{
		return BASE_URL + BASE_V1 + "/status/love";
	}

	public static String registerViewActionUrl()
	{
		return BASE_URL + BASE_V1 + "/status/view";
	}
	
	public static String getActionsUpdateUrl()
	{
		return BASE_URL + BASE_V1 + "/status/love/get_counts_with_msisdn";
	}
	
	public static String getPostImageSUUrl()
	{
		return BASE_URL + BASE_V1 + "/user/status-timeline";
	}
	
	public static String signUpPinCallBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/pin-call";
	}
	public static String languageListUrl()
	{
		return BASE_URL + BASE_V1 + "/lang_list";
	}
	public static String getMorestickersUrl()
	{
		return BASE_URL+STICKER_SHARE_PATH ;
	}

	public static String getDeleteAvatarBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/avatar-delete";
	}

	public static String getAnonymousNameFetchUrl()
	{
		return BASE_PLATFORM_URL + BASE_USER + BASE_V1 + ANONYMOUS_NAME;
	}
	
	public static String getGroupBaseUrlForLinkSharing()
	{
		return BASE_URL + BASE_V2 + "/group/";
	}
	
	public static String getBaseCodeGCAcceptUrl()
	{
		return BASE_URL + BASE_V2 + "/gcjp/";
	}
	
	public static String getBotdiscoveryTableUrl()
	{
		// TODO Add complete url here
		return BASE_PLATFORM_URL;
	}

	public static String getLanguageDictionaryBaseUrl()
	{
		return BASE_URL + BASE_V1 + "/dict/";
	}
	
	public static String getHikeCallerUrl()
	{
		if (isProduction)
		{
			return PRODUCTION_HIKECALLER_API + BASE_NAME;
		}
		else
		{
			return STAGING_HIKECALLER_API + BASE_NAME;
		}
	}

	public static String getHikeCallerBlockUrl()
	{
		if (isProduction)
		{
			return PRODUCTION_HIKECALLER_API+ BASE_BLOCK;
		}
		else
		{
			return STAGING_HIKECALLER_API + BASE_BLOCK;
		}
	}


	public static String getBlockedCallerListUrl()
	{
		if (isProduction)
		{
			return PRODUCTION_HIKECALLER_API+ BASE_BLOCKED_LIST;
		}
		else
		{
			return STAGING_HIKECALLER_API + BASE_BLOCKED_LIST;
		}
	}

	public static String getMicroAppLoggingUrl(boolean isSuccess)
	{
		String suffix = "/mapps/api" + BASE_V2 + "/apps/ack/" + (isSuccess ? "success" : "failure");

		if (isProduction)
		{
			return HTTPS + "mapps." + PLATFORM_PRODUCTION_API + suffix;
		}
		else
		{
			return HTTPS + QA_CONTENT + suffix ;
		}
	}
    public static String httpNetworkTestUrl()
    {
        if (isProduction)
        {
            return HTTP + "ping.im.hike.in" + BASE_V1 + "/android";
        }
        else
        {
            return HTTP + STAGING_API  + BASE_V1 + "/android";
        }
    }

	public static String getUploadFileBaseUrl()
	{
		return AccountUtils.fileTransferBase + "/user/pft/";
	}

	public static String getValidateFileKeyBaseUrl()
	{
		return AccountUtils.fileTransferBaseDownloadUrl;
	}

	public static String getFastFileUploadBaseUrl()
	{
		return AccountUtils.fastFileUploadUrl;
	}

	public static String getUploadContactOrLocationBaseUrl()
	{
		return AccountUtils.fileTransferBase + "/user/ft";
	}

	public static String getAnalyticsUrl()
	{
		return  BASE_URL + BASE_V1 + ANALYTICS_UPLOAD_PATH;
	}

    /*
     * Async Method to fetch latest micro app from server for forward card case
     */
    public static String getBotDownloadUrlV2()
    {
        String suffix = "/mapps/api" + BASE_V2 + "/apps/install.json";

        if (isProduction)
        {
            return HTTPS + "mapps." + PLATFORM_PRODUCTION_API + suffix;
        }
        else
        {
            return HTTPS + QA_CONTENT + suffix ;
        }
    }

	public static String getHistoricalStatusUpdatesUrl()
	{
		return  BASE_URL + BASE_V1 + "hsu/";
	}

	public static String fetchUIDForMissingMsisdnUrl()
	{
		return BASE_URL + BASE_V2 + "/user-identifier-update-graph";
	}
		public static String getSettingsUploadUrl()
	{
		return  BASE_URL + BASE_V5 + HIKE_SETTINGS;
	}

	public static String getSettingsDownloadUrl()
	{
		return  BASE_URL + BASE_V5 + HIKE_SETTINGS;
	}

	public static String getFetchBdayUrl()
	{
		return BASE_URL + BASE_V1 + FETCH_TODAYS_BIRTHDAY_URL;
	}

	public static String editProfileNameBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/name";
	}

	public static String editProfileEmailGenderBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/profile";
	}

	public static String editDOBBaseUrl()
	{
		return BASE_URL + BASE_V1 + BASE_ACCOUNT + "/dob";
	}

	public static String getBDPrefUpdateUrl()
	{
		return editDOBBaseUrl() + PREF_PATH;
	}
}
