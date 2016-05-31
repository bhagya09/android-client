package com.bsb.hike.modules.lastseenmgr;

import android.support.annotation.Nullable;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.BulkLastSeenRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchBulkLastSeenTask
{	
	
	private String TAG = "FetchBulkLastSeenTask";
	
	public FetchBulkLastSeenTask()
	{
		
	}
	
	public RequestToken start()
	{
		RequestToken requestToken = BulkLastSeenRequest(getRequestListener(), new LastSeenRetryPolicy());
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

					Utils.handleBulkLastSeenPacket(HikeMessengerApp.getInstance(), response);
					
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
