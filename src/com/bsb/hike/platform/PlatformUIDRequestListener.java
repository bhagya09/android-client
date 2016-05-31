package com.bsb.hike.platform;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
	{
		Logger.e(HikePlatformConstants.FETCH_TAG, httpException.toString());
		if (fetchType == HikePlatformConstants.PlatformFetchType.SELF_ANONYMOUS_NAME)
		{
			JSONObject result = new JSONObject();
			try
			{
				result.put("Status", "Failure");
			} catch (JSONException e)
			{
				e.printStackTrace();
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.ANONYMOUS_NAME_SET,result);
		}
	}

	/**
	 * The logic is such that the call for full address book loading is pretty heavy and will be called only once irrespective of its success or failure. So, in the
	 * app upgrade, it will check whether the pref is set for an http call or not. If yes, then the http call will take place.
	 * @param result
	 */
	@Override
	public void onRequestSuccess(Response result)
	{
		HikeSharedPreferenceUtil mPrefs = HikeSharedPreferenceUtil.getInstance();
		JSONObject obj = (JSONObject) result.getBody().getContent();
		switch (fetchType)
		{
		case HikePlatformConstants.PlatformFetchType.SELF:


			Logger.d(HikePlatformConstants.FETCH_TAG, "response for the platform uid request for " + fetchType + " is " + obj.toString());
			if (obj.has(HikePlatformConstants.PLATFORM_USER_ID) && obj.has(HikePlatformConstants.PLATFORM_TOKEN))
			{
				String platformUID = obj.optString(HikePlatformConstants.PLATFORM_USER_ID);
				String platformToken = obj.optString(HikePlatformConstants.PLATFORM_TOKEN);

				if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
				{
					mPrefs.saveData(HikeMessengerApp.PLATFORM_UID_SETTING, platformUID);
					mPrefs.saveData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, platformToken);
					if(obj.has(HikePlatformConstants.ANONYMOUS_NAME))
					{
						String anonName = obj.optString(HikePlatformConstants.ANONYMOUS_NAME);
						if (!TextUtils.isEmpty(anonName))
						{
							mPrefs.saveData(HikeMessengerApp.ANONYMOUS_NAME_SETTING, anonName);
						}
					}

					if (mPrefs.getData(HikePlatformConstants.PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH, -1) == HikePlatformConstants.MAKE_HTTP_CALL)
					{
						PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.FULL_ADDRESS_BOOK);
						mPrefs.saveData(HikePlatformConstants.PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH, HikePlatformConstants.HTTP_CALL_MADE);
					}
				}
			}

			break;

		case HikePlatformConstants.PlatformFetchType.PARTIAL_ADDRESS_BOOK:
		case HikePlatformConstants.PlatformFetchType.FULL_ADDRESS_BOOK:

			JSONArray response = (JSONArray) result.getBody().getContent();
			Logger.d(HikePlatformConstants.FETCH_TAG, "response for the platform uid request for " + fetchType + " is " + response.toString());
			ContactManager.getInstance().platformUserIdEntry(response);

			break;

		case HikePlatformConstants.PlatformFetchType.SELF_ANONYMOUS_NAME:

			Logger.d(HikePlatformConstants.FETCH_TAG, "response for the anonymous request for " + fetchType + " is " + obj.toString());
			if (obj.has(HikePlatformConstants.ANONYMOUS_NAME))
			{
				String anonName = obj.optString(HikePlatformConstants.ANONYMOUS_NAME);
				if (!TextUtils.isEmpty(anonName) )
				{
					mPrefs.saveData(HikeMessengerApp.ANONYMOUS_NAME_SETTING, anonName);
					HikeMessengerApp.getPubSub().publish(HikePubSub.ANONYMOUS_NAME_SET, obj);
				}
			}
			else if (obj.has(HikePlatformConstants.ERROR))
			{
				String errorMessage = obj.optString(HikePlatformConstants.ERROR);
				HikeMessengerApp.getPubSub().publish(HikePubSub.ANONYMOUS_NAME_SET, obj);
				//TODO send this error message to the required place, that'll indeed call this api again to fetch anon name.
			}
			break;

		case HikePlatformConstants.PlatformFetchType.OTHER_ANONYMOUS_NAME:
			Logger.d(HikePlatformConstants.FETCH_TAG, "response for the anonymous request for " + fetchType + " is " + obj.toString());
			if (obj.has(HikePlatformConstants.ANONYMOUS_NAMES))
			{
				String anonNames = obj.optString(HikePlatformConstants.ANONYMOUS_NAME);
				//TODO send these anonNames to the required place, native or html.
			}
		}

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		//do nothing
	}
}