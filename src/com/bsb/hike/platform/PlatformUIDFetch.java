package com.bsb.hike.platform;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.tasks.PlatformUidFetchTask;
import com.bsb.hike.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
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
			case HikePlatformConstants.PlatformFetchType.SELF:
				url = HttpRequestConstants.selfPlatformUidFetchUrl();
				new PlatformUidFetchTask(fetchType, url).execute();
				break;

			case HikePlatformConstants.PlatformFetchType.SELF_ANONYMOUS_NAME:
				url = HttpRequestConstants.getAnonymousNameFetchUrl();
				JSONObject jObj = new JSONObject();
				headers = PlatformUtils.getHeaders();
				try
				{
					String name = (varargs != null && varargs.length > 0) ? varargs[0] : "";
					if (TextUtils.isEmpty(name))
					{
						break;
					}
					jObj.put(HikeConstants.NAME, name);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				new PlatformUidFetchTask(fetchType, url, jObj, headers).execute();
				break;

			case HikePlatformConstants.PlatformFetchType.PARTIAL_ADDRESS_BOOK:
				url = HttpRequestConstants.platformUidForPartialAddressBookFetchUrl();
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikePlatformConstants.HIKE_MSISDN,  new JSONArray(Arrays.asList(varargs)));
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				headers = PlatformUtils.getHeaders();
				if (headers != null)
				{
					new PlatformUidFetchTask(fetchType, url, jsonObject, headers).execute();
				}
				break;

			case HikePlatformConstants.PlatformFetchType.FULL_ADDRESS_BOOK:
				url = HttpRequestConstants.platformUIDForFullAddressBookFetchUrl();
				headers = PlatformUtils.getHeaders();
				if (headers != null)
				{
					new PlatformUidFetchTask(fetchType, url, headers).execute();
				}
				break;

			case HikePlatformConstants.PlatformFetchType.OTHER_ANONYMOUS_NAME:
				url = HttpRequestConstants.getAnonymousNameFetchUrl();
				url += "?" + HikePlatformConstants.PLATFORM_UIDS + "=" + Utils.getCommaSeperatedStringFromArray(varargs);
				headers = PlatformUtils.getHeaders();
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

}
