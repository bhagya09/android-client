package com.bsb.hike.chatthread;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.chatHead.CallerContentModel;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
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
	 * @param ctx
	 * @param msisdn
	 */
	public static void fetchHikeUserInfo(final Context ctx, final String msisdn)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(HikeConstants.MSISDN, msisdn);
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "jsonException");
		}

		RequestToken requestToken = HttpRequests.postCallerMsisdn(HttpRequestConstants.getHikeCallerUrl(), json, new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				String resultContent = result.getBody().getContent() == null ? null : result.getBody().getContent().toString();
				CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(resultContent);
				Logger.d(TAG, resultContent);
				if (callerContentModel != null && callerContentModel.getMsisdn() != null)
				{
					ContactManager.getInstance().insertIntoCallerTable(callerContentModel, true, true);
				}
                //HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_UNKNOWN_USER_INFO_VIEW, callerContentModel);
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
		}, StickyCaller.THREE_RETRIES, ChatHeadUtils.HTTP_CALL_RETRY_DELAY, ChatHeadUtils.HTTP_CALL_RETRY_MULTIPLIER, false);
		requestToken.execute();
	}

}
