package com.bsb.hike.platform;

import android.text.TextUtils;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.tasks.PlatformUidFetchTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shobhit on 30/03/15.
 */
public class PlatformUIDFetch
{
	/**
	 * Used to fetch the platform UID. 3 types of requests are there. One to fetch the individual Platform User ID and Platform token
	 * and the other 2 to fetch the platform user id of full/partial address book of the user.
	 * @param fetchType : HikePlatformConstants.PlatformUIDFetchType --> SELF, FULL_ADDRESS_BOOK, PARTIAL_ADDRESS_BOOK.
	 * @param varargs : String[] msisdn for partial address book.
	 */
	public static void fetchPlatformUid(int fetchType, String... varargs)
	{
		String url = "";
		List<Header> headers;
		try
		{
			switch (fetchType)
			{
			case HikePlatformConstants.PlatformUIDFetchType.SELF:
				url = HttpRequestConstants.selfPlatformUidFetchUrl();
				new PlatformUidFetchTask(fetchType, url).execute();
				break;

			case HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK:
				url = HttpRequestConstants.platformUidForPartialAddressBookFetchUrl();
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikePlatformConstants.HIKE_MSISDN, varargs);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				headers = getHeaders();
				if (headers != null)
				{
					new PlatformUidFetchTask(fetchType, url, jsonObject, headers).execute();
				}
				break;

			case HikePlatformConstants.PlatformUIDFetchType.FULL_ADDRESS_BOOK:
				url = HttpRequestConstants.platformUIDForFullAddressBookFetchUrl();
				headers = getHeaders();
				if (headers != null)
				{
					new PlatformUidFetchTask(fetchType, url, headers).execute();
				}
				break;

			}

		}
		catch (IndexOutOfBoundsException iobe)
		{
			iobe.printStackTrace();
		}

	}

	private static List<Header> getHeaders()
	{

		HikeSharedPreferenceUtil mpref = HikeSharedPreferenceUtil.getInstance();
		String platformUID = mpref.getData(HikeMessengerApp.PLATFORM_UID_SETTING, null);
		String platformToken = mpref.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null);
		if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
		{
			List<Header> headers = new ArrayList<Header>(1);
			if (platformToken != null && platformUID != null)
			{
				headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME, HikePlatformConstants.PLATFORM_TOKEN + "=" + platformToken + "; " + HikePlatformConstants.PLATFORM_USER_ID + "=" + platformUID));
			}

			return headers;
		}
		return null;
	}

}
