package com.bsb.hike.platform;

import android.content.SharedPreferences;
import android.text.TextUtils;
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

	/**
	 * The logic is such that the call for full address book loading is pretty heavy and will be called only once irrespective of its success or failure. So, in the
	 * app upgrade, it will check whether the pref is set for an http call or not. If yes, then the http call will take place.
	 * @param result
	 */
	@Override
	public void onRequestSuccess(Response result)
	{

		switch (fetchType)
		{
		case HikePlatformConstants.PlatformUIDFetchType.SELF:

			HikeSharedPreferenceUtil mPrefs = HikeSharedPreferenceUtil.getInstance();
			JSONObject obj = (JSONObject) result.getBody().getContent();
			if (obj.has(HikePlatformConstants.PLATFORM_USER_ID) && obj.has(HikePlatformConstants.PLATFORM_TOKEN))
			{
				String platformToken = obj.optString(HikePlatformConstants.PLATFORM_USER_ID);
				String platformUID = obj.optString(HikePlatformConstants.PLATFORM_TOKEN);

				if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
				{
					mPrefs.saveData(HikeMessengerApp.PLATFORM_UID_SETTING, platformUID);
					mPrefs.saveData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, platformToken);

					if (mPrefs.getData(HikePlatformConstants.PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH, -1) == HikePlatformConstants.MAKE_HTTP_CALL)
					{
						PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK);
						mPrefs.saveData(HikePlatformConstants.PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH, HikePlatformConstants.HTTP_CALL_MADE);
					}
				}
			}

			break;

		case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
		case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:

			JSONArray response = (JSONArray) result.getBody().getContent();
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