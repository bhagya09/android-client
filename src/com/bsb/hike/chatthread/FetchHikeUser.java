package com.bsb.hike.chatthread;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchHikeUser
{
	private static final String TAG = "FetchHikeUser";

	/**
	 * This function is used in the chatThread to fetch details about a user. i.e., whether an unknown user is on hike or not. <br>
	 * Sample responses : <br>
	 * {"profile":{"jointime":1385821790,"name":"Test"},"stat":"ok","onhike":"true”} <br>
	 * {"stat":"ok","onhike":"false”}
	 * 
	 * @param ctx
	 * @param msisdn
	 */
	public static void fetchHikeUser(final Context ctx, final String msisdn)
	{
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/profile/" + msisdn, RequestType.HIKE_JOIN_TIME, new HikeHttpCallback()
		{
			@Override
			public void onSuccess(JSONObject response)
			{
				Logger.d(TAG, "Response for account/profile request: " + response.toString());
				try
				{
					boolean onHike = response.getBoolean(HikeConstants.ON_HIKE);
					if (onHike)
					{
						JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
						long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);
						if (hikeJoinTime > 0)
						{
							hikeJoinTime = Utils.applyServerTimeOffset(ctx, hikeJoinTime);
							HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(msisdn, hikeJoinTime));
							ContactManager.getInstance().updateHikeStatus(ctx, msisdn, true);
							HikeConversationsDatabase.getInstance().updateOnHikeStatus(msisdn, true);
							HikeMessengerApp.getPubSub().publish(HikePubSub.USER_JOINED, msisdn);
						}
					}

					else
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.USER_LEFT, msisdn);
					}
				}

				catch (JSONException e)
				{
					e.printStackTrace();
					Logger.e(TAG, " JSON Error in fetchHike User : " + e.toString());
				}
			}

			@Override
			public void onFailure()
			{
				// TODO Handle failure of the call.
				super.onFailure();
			}
		});

		HikeHTTPTask getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
		Utils.executeHttpTask(getHikeJoinTimeTask, hikeHttpRequest);
	}

}
