package com.bsb.hike.modules.signupmgr;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.validateNumberRequest;

public class ValidateNumberTask
{
	private String msisdn;

	private String resultMsisdn;

	public ValidateNumberTask(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public String execute()
	{
		RequestToken requestToken = validateNumberRequest(getPostObject(), getRequestListener(), new SignUpHttpRetryPolicy(SignUpHttpRetryPolicy.MAX_RETRY_COUNT, BasicRetryPolicy.DEFAULT_RETRY_DELAY, BasicRetryPolicy.DEFAULT_BACKOFF_MULTIPLIER));
		requestToken.execute();
		return resultMsisdn;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject obj = (JSONObject) result.getBody().getContent();

				resultMsisdn = obj.optString("msisdn");
				int defaultCallMeTimer = resultMsisdn.startsWith(HikeConstants.INDIA_COUNTRY_CODE)? 10 : 150; 
				SignupActivity.callMeWaitTime = (obj.optInt("callMeTimer", defaultCallMeTimer)) * 1000;
				Logger.d("HTTP", "Successfully validated phone number.");
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				resultMsisdn = null;
			}
		};
	}

	private JSONObject getPostObject()
	{
		JSONObject data = new JSONObject();
		try
		{
			data.put("phone_no", msisdn);
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "creating a string entity from an entry string threw!", e);
		}

		return data;
	}

}
