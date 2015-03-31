package com.bsb.hike.tasks;

import android.os.AsyncTask;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDRequestListener;
import org.json.JSONObject;

/**
 * Created by shobhit on 30/03/15.
 */
public class PlatformUidFetchTask extends AsyncTask<Void, Void, Void>
{
	int fetchType;

	String url;

	JSONObject postParams;

	public PlatformUidFetchTask(int fetchType, String url)
	{
		this(fetchType, url, null);
	}

	public PlatformUidFetchTask(int fetchType, String url, JSONObject body)
	{
		this.fetchType = fetchType;
		this.url = url;
		this.postParams = body;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		RequestToken token = null;
		switch (fetchType)
		{
		case HikePlatformConstants.PlatformUIDFetchType.SELF:
		case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
			token = HttpRequests.postPlatformUserIdFetchRequest(url, postParams, new PlatformUIDRequestListener(fetchType));
			break;

		case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:
			token = HttpRequests.getPlatformUserIdFetchRequest(url, new PlatformUIDRequestListener(fetchType));
			break;
		}
		if (!token.isRequestRunning())
		{
			token.execute();
		}
		return null;
	}
}
