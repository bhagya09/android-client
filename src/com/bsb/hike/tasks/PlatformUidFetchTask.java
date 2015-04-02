package com.bsb.hike.tasks;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDRequestListener;
import com.bsb.hike.utils.Logger;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by shobhit on 30/03/15.
 */
public class PlatformUidFetchTask implements IHikeHTTPTask
{
	int fetchType;

	String url;

	JSONObject postParams;

	List<Header> headerList;


	public PlatformUidFetchTask(int fetchType, String url)
	{
		this(fetchType, url, null, null);
	}

	public PlatformUidFetchTask(int fetchType, String url, JSONObject body)
	{
		this(fetchType,url,body,null);
	}

	public PlatformUidFetchTask(int fetchType, String url, List<Header> headers)
	{
		this(fetchType,url,null, headers);
	}

	/**
	 * Task used for the http call. Runs in the background.
	 * @param fetchType : HikePlatformConstants.PlatformUIDFetchType --> SELF, FULL_ADDRESS_BOOK, PARTIAL_ADDRESS_BOOK.
	 * @param url
	 * @param body : post params for any call
	 * @param headers : headers for the http call
	 */
	public PlatformUidFetchTask(int fetchType, String url, JSONObject body, List<Header> headers)
	{
		this.fetchType = fetchType;
		this.url = url;
		this.postParams = body;
		this.headerList = headers;
	}

	@Override
	public void execute()
	{
		RequestToken token = null;
		switch (fetchType)
		{
		case HikePlatformConstants.PlatformUIDFetchType.SELF:
			Logger.d(HikePlatformConstants.PLATFORM_UID_FETCH_TAG, "request to fetch platform uid for " + fetchType + " with url " + url);
			token = HttpRequests.postPlatformUserIdFetchRequest(url, new PlatformUIDRequestListener(fetchType));
			break;

		case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
			Logger.d(HikePlatformConstants.PLATFORM_UID_FETCH_TAG, "request to fetch platform uid for " + fetchType + " with url " + url + " for the msisdns " + postParams);
			token = HttpRequests.postPlatformUserIdForPartialAddressBookFetchRequest(url, postParams, new PlatformUIDRequestListener(fetchType), headerList);
			break;

		case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:
			Logger.d(HikePlatformConstants.PLATFORM_UID_FETCH_TAG, "request to fetch platform uid for " + fetchType + " with url " + url);
			token = HttpRequests.getPlatformUserIdForFullAddressBookFetchRequest(url, new PlatformUIDRequestListener(fetchType), headerList);
			break;
		}
		if (!token.isRequestRunning())
		{
			token.execute();
		}
	}

	@Override
	public void cancel()
	{

	}
}
