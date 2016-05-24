package com.bsb.hike.chatthread;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
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
		RequestToken requestToken = HttpRequests.getHikeJoinTimeRequest(msisdn, new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
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
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.e(TAG, " failure in fetchHike User with error code : " + httpException.getErrorCode());
			}
		});
		requestToken.execute();
	}

}
