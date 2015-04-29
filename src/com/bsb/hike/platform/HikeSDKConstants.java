package com.bsb.hike.platform;

/**
 * Contains exposed constants.
 * 
 * @author Atul M
 */
public class HikeSDKConstants
{

	/** Hike SDK version. */
	public static final float HIKE_SDK_VERSION = 1.0F;

	/** Hike messenger application package name. */
	public static final String HIKE_MSGN_PKG_NAME = "com.bsb.hike";

	public static final String HIKE_PROVIDER_BASE = "content://";
	
	/** Hike content provider authority. */
	public static final String HIKE_AUTHORITY = "com.bsb.hike.providers.HikeProvider";

	/** Hike id column name. */
	public static final String HIKE_ID_COLUMN = "id";

	/** Hike avatar column name. */
	public static final String HIKE_AVATAR_COLUMN = "image";

	/** Key for transferring data across process. */
	public static final String HIKE_REQ_DATA_ID = "data";

	/** Key for transferring filter type for getHikeUsers() API. */
	public static final String HIKE_REQ_FILTER_ID = "filter";

	/** Key for transferring message for sendMessage() API. */
	public static final String HIKE_REQ_SEND_MSG_ID = "message";

	/** Key for transferring sdk version. */
	public static final String HIKE_REQ_SDK_VERSION_ID = "version";

	/** Key for transferring client app id. */
	public static final String HIKE_REQ_SDK_CLIENT_APP_ID = "clientAppId";

	/** Key for transferring client secret id. */
	public static final String HIKE_REQ_SDK_CLIENT_SECRET_ID = "clientSecretId";

	/** Key for transferring package name. */
	public static final String HIKE_REQ_SDK_CLIENT_PKG_NAME = "clientPkgName";

	/** Key for transferring access token. */
	public static final String HIKE_REQ_SDK_CLIENT_ACC_TOKEN = "clientAccessT";

	/** Since self user hike id is not available, use a custom id to represent the same */
	public static final String HIKE_SELF_USER_CODE = "-1";

	public static final String PREF_HIKE_SDK_INSTALL_CLICKED_KEY = "installedViaSDK";

	public static final String PREF_HIKE_SDK_INSTALL_DENIED_KEY = "refusedInstallSDK";

}
