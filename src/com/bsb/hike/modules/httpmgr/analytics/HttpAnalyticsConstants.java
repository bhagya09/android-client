package com.bsb.hike.modules.httpmgr.analytics;

public class HttpAnalyticsConstants
{
	public static final String TRACK_ID_HEADER_KEY = "X-Track-Id";

	public static final String RESPONSE_CODE = "res_code";

	public static final String HTTP_ANALYTICS_TYPE = "rel_http";

	public static final int REQUEST_LOG_EVENT = 1;

	public static final int RESPONSE_LOG_EVENT = 4;

	/** Used for creating random integer within this range which is used for determining whether we want to log http analytics event or not */
	public static final int MAX_RANGE_HTTP_ANALYTICS = 10000;

	/** Default value which is used for determining whether we want to log http analytics event or not */
	public static final int DEFAULT_HTTP_ANALYTICS = 500;

	public static final String LAST_SEEN_REQUEST_ANALYTICS_KEY = "new_ls";

	public static final String BULK_LAST_SEEN_REQUEST_ANALYTICS_KEY = "bls";

	public static final String STATUS_UPDATE_REQUEST_ANALYTICS_KEY = "su";

	public static final String MULTI_STICKER_DOWNLOAD_REQUEST_ANALYTICS_KEY = "multiStkD";

	public static final String SINGLE_STICKER_DOWNLOAD_REQUEST_ANALYTICS_KEY = "singleStkD";

	public static final String PLATFORM_ZIP_DOWNLOAD_REQUEST_ANALYTICS_KEY = "pf_zipd";
}
