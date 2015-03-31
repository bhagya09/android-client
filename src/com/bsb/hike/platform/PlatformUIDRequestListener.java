package com.bsb.hike.platform;

import android.util.Pair;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by shobhit on 30/03/15.
 */

public class PlatformUIDRequestListener implements IRequestListener
{

	int fetchType;

	public PlatformUIDRequestListener(int fetchType)
	{
		this.fetchType = fetchType;
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		//TODO handle failure
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		JSONArray response = (JSONArray) result.getBody().getContent();
		switch (fetchType)
		{
		case HikePlatformConstants.PlatformUIDFetchType.SELF:
			try
			{
				JSONObject obj = response.getJSONObject(0);
				if (obj.has(HikePlatformConstants.PLATFORM_USER_ID))
				{
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PLATFORM_UID_SETTING, obj.optString(HikePlatformConstants.PLATFORM_USER_ID));
					HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.PLATFORM_UID_FETCH_AT_UPGRADE, 2);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			break;

		case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
		case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:

			ContactManager.getInstance().platformUserIdEntry(response);

			break;
		}

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		//do nothing
	}
}