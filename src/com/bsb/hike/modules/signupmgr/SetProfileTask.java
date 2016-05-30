package com.bsb.hike.modules.signupmgr;

import android.accounts.NetworkErrorException;
import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.setProfileRequest;

public class SetProfileTask
{
	private String name;

	private Birthday birthdate;

	private boolean isFemale;

	private JSONObject resultObject;

	public SetProfileTask(String name, Birthday birthdate, boolean isFemale)
	{
		this.name = name;
		this.birthdate = birthdate;
		this.isFemale = isFemale;
	}

	public JSONObject execute() throws NetworkErrorException
	{
		JSONObject postObject = getPostObject();
		if (postObject == null)
		{
			return null;
		}

		RequestToken requestToken = setProfileRequest(postObject, getRequestListener(), new SignUpHttpRetryPolicy(SignUpHttpRetryPolicy.MAX_RETRY_COUNT, BasicRetryPolicy.DEFAULT_RETRY_DELAY, BasicRetryPolicy.DEFAULT_BACKOFF_MULTIPLIER));
		requestToken.execute();

		if (resultObject == null)
		{
			throw new NetworkErrorException("Unable to set name");
		}
		return resultObject;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				if (!Utils.isResponseValid(response))
				{
					resultObject = null;
					return;
				}
				else
				{
					resultObject = response;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				resultObject = null;
			}
		};
	}

	private JSONObject getPostObject()
	{
		try
		{
			JSONObject data = new JSONObject();
			data.put(HikeConstants.NAME, name);
			data.put(HikeConstants.GENDER, isFemale ? HikeConstants.FEMALE : HikeConstants.MALE);
			if (birthdate != null)
			{
				JSONObject bday = new JSONObject();
				if (birthdate.day != 0)
				{
					bday.put(HikeConstants.DAY, birthdate.day);
				}
				if (birthdate.month != 0)
				{
					bday.put(HikeConstants.MONTH, birthdate.month);
				}
				bday.put(HikeConstants.YEAR, birthdate.year);
				data.put(HikeConstants.DOB, bday);
			}
			data.put("screen", "signup");
			return data;
		}
		catch (JSONException e)
		{
			return null;
		}
	}
}