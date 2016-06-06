package com.bsb.hike.modules.lastseenmgr;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.LastSeenRequest;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchLastSeenTask
{
	private String TAG = "FetchLastSeenTask";
	
	private String msisdn;
	
	public FetchLastSeenTask(String msisdn)
	{
		this.msisdn = msisdn;
	}
	
	public RequestToken start()
	{
		/*
		 * Adding this check to ensure we don't make a request for empty/null msisdn.
		 * TODO figure out why this is happening
		 */
		
		if (TextUtils.isEmpty(msisdn))
		{
			Logger.w("LastSeenTask", "msisdn is null!");
			return null;
		}
		HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "doInBackground", "Sending req", msisdn);

		RequestToken requestToken = LastSeenRequest(msisdn, getRequestListener(), new LastSeenRetryPolicy());
		requestToken.execute();
		return requestToken;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					
					Logger.d(TAG, "response : " + response.toString());
					JSONObject data = response.getJSONObject(HikeConstants.DATA);
					long res = data.getLong(HikeConstants.LAST_SEEN);

					/*
					 * Update current last seen value.
					 */
					long currentLastSeenValue = res;
					
					long lastSeenValue;
					int offline;
					
					/*
					 * We only apply the offset if the value is greater than 0 since 0 and -1 are reserved.
					 */
					if (currentLastSeenValue > 0)
					{
						offline = 1;
						lastSeenValue = Utils.applyServerTimeOffset(HikeMessengerApp.getInstance(), currentLastSeenValue);
					}
					else
					{
						offline = (int) currentLastSeenValue;
						lastSeenValue = System.currentTimeMillis() / 1000;
					}

					ContactManager.getInstance().updateLastSeenTime(msisdn, lastSeenValue);
					ContactManager.getInstance().updateIsOffline(msisdn, offline);
					ContactInfo contact = ContactManager.getInstance().getContact(msisdn, true, true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contact);
					HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "saveResult", "Updated CM", msisdn);
					HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "onPostExecute", "reseult recv "+ result, msisdn);
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "JSON exception", e);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.e(TAG, "exception : ", httpException);
			}
		};
	}
}