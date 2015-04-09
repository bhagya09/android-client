package com.bsb.hike.modules.httpmgr.analytics;

public class HttpAnalyticsConstants
{
	public static final String TRACK_ID_HEADER_KEY = "X-Track-Id";

	public static final String RESPONSE_CODE = "res_code";

	public static final String HTTP_ANALYTICS_TYPE = "rel_http";

	public static final String HTTP_METHOD_TYPE = "method";
	
	public static final String HTTP_REQUEST_ANALYTICS_PARAM = "param";
	
	public static final int REQUEST_LOG_EVENT = 1;

	public static final int RESPONSE_LOG_EVENT = 4;

	/** Used for creating random integer within this range which is used for determining whether we want to log http analytics event or not */
	public static final int MAX_RANGE_HTTP_ANALYTICS = 10000;

	/** Default value which is used for determining whether we want to log http analytics event or not */
	public static final int DEFAULT_HTTP_ANALYTICS = 500;
	
	public static final String HTTP_REQUEST_URL_FILTER = ".hike.in";
	
	public static final String HTTP_SINGLE_STICKER_DOWNLOAD_ANALYTICS_PARAM = "singleSticker";

	public static final String HTTP_TOTAL_STICKER_COUNT_ANALYTICS_PARAM = "totalStickers";

	public static final String HTTP_MULTI_STICKER_DOWNLOAD_ANALYTICS_PARAM = "multiStickers";
}
