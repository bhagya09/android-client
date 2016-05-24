package com.bsb.hike.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class GetHikeJoinTimeTask implements IHikeHTTPTask
{
	private String msisdn;

	private RequestToken requestToken;

	public GetHikeJoinTimeTask(String msisdn)
	{
		this.msisdn = msisdn;
		this.requestToken = HttpRequests.getHikeJoinTimeRequest(msisdn, getRequestListener());
	}

	@Override
	public void execute()
	{
		if (requestToken.isRequestRunning())
		{
			return;
		}

		requestToken.execute();
	}

	@Override
	public void cancel()
	{
		requestToken.cancel();
	}

	public IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					Logger.d(getClass().getSimpleName(), "Hike join time request succeeded, Response : " + response);
					JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
					long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);

					if (hikeJoinTime > 0)
					{
						Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
						SharedPreferences preferences = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

						String selfMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, null);
						if (selfMsisdn != null && selfMsisdn.equals(msisdn))
						{
							Editor editor = preferences.edit();
							editor.putLong(HikeMessengerApp.USER_JOIN_TIME, hikeJoinTime);
							editor.commit();
							HikeMessengerApp.getPubSub().publish(HikePubSub.USER_JOIN_TIME_OBTAINED, new Pair<String, Long>(msisdn, hikeJoinTime));
						}
						else
						{
							hikeJoinTime = Utils.applyServerTimeOffset(ctx, hikeJoinTime);
							HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(msisdn, hikeJoinTime));
						}

						ContactManager.getInstance().updateHikeStatus(msisdn, true);
						HikeConversationsDatabase.getInstance().updateOnHikeStatus(msisdn, true);

					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.e(getClass().getSimpleName(), "Hike join time request failed : " + httpException.getMessage());
			}
		};
	}
}
