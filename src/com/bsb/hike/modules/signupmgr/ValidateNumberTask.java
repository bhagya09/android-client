package com.bsb.hike.modules.signupmgr;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.validateNumberRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

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
		RequestToken requestToken = validateNumberRequest(getPostObject(), getRequestListener());
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
				Logger.d("HTTP", "Successfully validated phone number.");
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
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