package com.bsb.hike.chatthread;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.FetchUIDTaskPojo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

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
					boolean onHike = response.getBoolean("oh");
					if (onHike)
					{
						JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
						String uid = profile.optString(DBConstants.HIKE_UID, null);
						long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);
						if (hikeJoinTime > 0)
						{
							hikeJoinTime = Utils.applyServerTimeOffset(ctx, hikeJoinTime);
							HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(msisdn, hikeJoinTime));
							if(!TextUtils.isEmpty(uid)) {
								Set<FetchUIDTaskPojo> fetchUIDTaskPojoSet = new HashSet<>(1);
								fetchUIDTaskPojoSet.add(new FetchUIDTaskPojo(msisdn, uid));
								HikeUserDatabase.getInstance().updateContactUid(fetchUIDTaskPojoSet);
								//Update only in present in cache
								ContactInfo ci = ContactManager.getInstance().getContact(msisdn);
								if (ci != null) {
									ci.setUid(uid);
								}
							}
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
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e(TAG, " failure in fetchHike User with error code : " + httpException.getErrorCode());
			}
		});
		requestToken.execute();
	}

}
