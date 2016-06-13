package com.bsb.hike.modules.stickerdownloadmgr;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.parameterMappingRequest;

public class ParameterMappingDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{
	private static String TAG = "ParameterMappingDownloadTask";

	private RequestToken requestToken;

	public ParameterMappingDownloadTask()
	{

	}

	@Override
	public void execute()
	{
		requestToken = parameterMappingRequest(getRequestId(), getResponseListener());

		if (requestToken.isRequestRunning())
		{
			return;
		}

		requestToken.execute();
	}

	private IRequestListener getResponseListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (!Utils.isResponseValid(response))
				{
					Logger.e(TAG, "parameter mapping request failed null or invalid response");
					doOnFailure(null);
					return;
				}
				Logger.d(TAG, "Got response for parameter mapping request " + response.toString());

				JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

				if (null == data)
				{
					Logger.e(TAG, "parameter mapping request failed null data");
					doOnFailure(null);
					return;
				}

				doOnSuccess(data);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.d(TAG, "response failed.");
				doOnFailure(httpException);
			}
		};
	}

	@Override
	public void cancel()
	{
		if(requestToken != null)
		{
			requestToken.cancel();
		}
	}

	@Override
	public String getRequestId()
	{
		return StickerRequestType.PARAMETER_MAPPING.getLabel();
	}


	@Override
	public Bundle getRequestBundle() {
		return null;
	}

	@Override
	public void doOnSuccess(Object result)
	{
		JSONObject paramMappingJSON = (JSONObject) result;
		parseParameterMapping(paramMappingJSON);
	}

	private void parseParameterMapping(JSONObject paramMappingJSON)
	{
		Iterator<String> urls = paramMappingJSON.keys();
		List<Pair<String, Pair<String, String>>> parameterMapping = new ArrayList<>(paramMappingJSON.length());
		while (urls.hasNext()) {
			try {
				String url = urls.next();
				JSONObject urlJSON = paramMappingJSON.getJSONObject(url);
				parseUrlJSON(parameterMapping, url, urlJSON);
			} catch (JSONException e) {
				Logger.e(TAG, "exception in parsing response ", e);
			}
		}
		HikeConversationsDatabase.getInstance().insertParameterMappingInDb(parameterMapping);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_PARAMETER_MAPPING_FETCH_TIME, System.currentTimeMillis());
	}

	private void parseUrlJSON(List<Pair<String, Pair<String, String>>> parameterMapping, String url, JSONObject urlJSON) throws JSONException
	{
		Iterator<String> methods = urlJSON.keys();
		while (methods.hasNext()) {
			String method = methods.next();
			JSONArray parameterArray = urlJSON.getJSONArray(method);

			if (parameterArray == null) {
				continue;
			}
			parameterMapping.add(new Pair<>(url, new Pair<>(method, parameterArray.toString())));
		}
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		Logger.d(TAG, "response failed.");
	}
}