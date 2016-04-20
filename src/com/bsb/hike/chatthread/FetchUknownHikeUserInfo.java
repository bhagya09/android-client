package com.bsb.hike.chatthread;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatHead.CallerContentModel;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

public class FetchUknownHikeUserInfo
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
	public static void fetchHikeUserInfo(final Context ctx, final String msisdn, final boolean insertNewRow)
	{
		final JSONObject json = new JSONObject();
		try
		{
			json.put(HikeConstants.MSISDN, msisdn);
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "jsonException");
		}

		RequestToken requestToken = HttpRequests.fetchUnknownChatUserInfo(json, new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				// get model with md filed ready
				if (result == null || result.getBody() == null)
				{
					return;
				}
				JSONObject jsonObject = (JSONObject) result.getBody().getContent();
				CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelFromResponse(jsonObject);

				if (callerContentModel != null && callerContentModel.getMsisdn() != null)
				{
					//Insert new row with Creation time = 0
					if (insertNewRow)
					{
						ContactManager.getInstance().insertIntoCallerTable(callerContentModel, false, false);
					}
					else
					{
						//Update md for this msisdn in table
						ContactManager.getInstance().updateMdIntoCallerTable(callerContentModel);
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, callerContentModel);
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

				//HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, null);
			}
		}, ChatHeadUtils.NO_OF_HTTP_CALL_RETRY, ChatHeadUtils.HTTP_CALL_RETRY_DELAY, ChatHeadUtils.HTTP_CALL_RETRY_MULTIPLIER);
		requestToken.execute();
	}

}
