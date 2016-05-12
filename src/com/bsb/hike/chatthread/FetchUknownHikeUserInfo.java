package com.bsb.hike.chatthread;

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

import org.json.JSONObject;

public class FetchUknownHikeUserInfo
{

	private static final String TAG = "FetchHikeUser";

	/**
	 * This function is used in the chatThread to fetch details about a user. i.e., whether an unknown user is on hike or not. <br>
	 * Sample responses : <br>
	 * {"profile":{"jointime":1385821790,"name":"Test"},"stat":"ok","onhike":"true”} <br>
	 * {"stat":"ok","onhike":"false”}
	 *
	 * @param msisdn
	 * @param callerContentModel
	 */
	public static void fetchHikeUserInfo(final String msisdn, final boolean insertNewRow, final CallerContentModel callerContentModel)
	{
		RequestToken requestToken = HttpRequests.fetchUnknownChatUserInfo(msisdn, insertNewRow, new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				if (result == null || result.getBody() == null)
				{
					return;
				}

				JSONObject jsonObject = (JSONObject) result.getBody().getContent();
				CallerContentModel updatedCallerContentModel = ChatHeadUtils.getUpdatedCallerContentModelFromResponse(callerContentModel, jsonObject, msisdn);

				Logger.d("c_spam", "HTTP res SUCCESS :- " + updatedCallerContentModel);

				if (!ChatHeadUtils.isFullNameValid(updatedCallerContentModel.getFullName()))
				{
					Logger.d("c_spam", " as Name ( "+ updatedCallerContentModel.getFullName()+" ) is not valid, so not entring in DB + not showing view and firing null pubsub" );
					HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, null);
				}
				else
				{
					// Insert new row with Creation time = 0
					if (insertNewRow)
					{
						Logger.d("c_spam", "HTTP res SUCCESS :- going insert new row in in DB");
						ContactManager.getInstance().insertIntoCallerTable(updatedCallerContentModel, false, false, 0);
					}
					else
					{
						// Update md, expiry time for this msisdn in table
						Logger.d("c_spam", "HTTP res SUCCESS :- updating md, expiry time in in DB");
						ContactManager.getInstance().updateMdIntoCallerTable(updatedCallerContentModel);
					}
					Logger.d("c_spam", "HTTP res SUCCESS :- Firing pubsub " + HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW + " data:- " + updatedCallerContentModel);
					HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, updatedCallerContentModel);
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

				Logger.d("c_spam", "HTTP res FAILED, Firing pubsub " + HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW + " data:- " + callerContentModel);

				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, callerContentModel);
			}
		}, ChatHeadUtils.NO_OF_HTTP_CALL_RETRY, ChatHeadUtils.HTTP_CALL_RETRY_DELAY, ChatHeadUtils.HTTP_CALL_RETRY_MULTIPLIER);
		requestToken.execute();
	}

}
