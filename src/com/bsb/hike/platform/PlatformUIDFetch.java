package com.bsb.hike.platform;

import android.text.TextUtils;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.tasks.PlatformUidFetchTask;
import com.bsb.hike.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhit on 30/03/15.
 */
public class PlatformUIDFetch
{

	public static void fetchPlatformUid(int fetchType, String... varargs)
	{
		String url = "";
		PlatformUidFetchTask platformUidFetchTask = null;
		try
		{
			switch (fetchType)
			{
			case HikePlatformConstants.PlatformUIDFetchType.SELF:
				String hikeUID = varargs[0];
				String hikeToken = varargs[1];
				if (TextUtils.isEmpty(hikeUID) || TextUtils.isEmpty(hikeToken))
				{
					break;
				}
				url = HttpRequestConstants.selfPlatformUidFetchUrl();
				JSONObject obj = new JSONObject();
				try
				{
					obj.put(HikePlatformConstants.HIKE_UID, hikeUID);
					obj.put(HikePlatformConstants.HIKE_TOKEN, hikeToken);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				platformUidFetchTask = new PlatformUidFetchTask(fetchType, url, obj);
				break;

			case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
				url = HttpRequestConstants.platformUidForAddressBookFetchUrl();
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikePlatformConstants.HIKE_UIDS, varargs);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				platformUidFetchTask = new PlatformUidFetchTask(fetchType, url, jsonObject);
				break;

			case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:
				String hikeuid = varargs[0];
				url = HttpRequestConstants.platformUidForAddressBookFetchUrl() + "?" + HikePlatformConstants.HIKE_UID + "=" + hikeuid;
				platformUidFetchTask = new PlatformUidFetchTask(fetchType, url);
				break;

			}
			Utils.executeAsyncTask(platformUidFetchTask);
		}
		catch (IndexOutOfBoundsException iobe)
		{
			iobe.printStackTrace();
		}

	}

}
