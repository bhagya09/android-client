package com.bsb.hike.tasks;

import android.support.annotation.Nullable;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.updateAddressBookRequest;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class UpdateAddressBookTask
{
	private Map<String, List<ContactInfo>> new_contacts_by_id;

	private JSONArray ids_json;

	private JSONObject resultObject;

	private RequestToken requestToken;
	
	/**
	 * 
	 * @param new_contacts_by_id
	 *            new entries to update with. These will replace contact IDs on the server
	 * @param ids_json
	 *            , these are ids that are no longer present and should be removed
	 * @return
	 */
	public UpdateAddressBookTask(Map<String, List<ContactInfo>> new_contacts_by_id, JSONArray ids_json)
	{
		this.new_contacts_by_id = new_contacts_by_id;
		this.ids_json = ids_json;
	}

	public List<ContactInfo> execute()
	{
		JSONObject postObject = getPostObject();
		if (postObject == null)
		{
			return null;
		}

		ArrayList<String> msisdnForMissingPlatformUID = ContactManager.getInstance().getMsisdnForMissingPlatformUID();

		if (msisdnForMissingPlatformUID != null && msisdnForMissingPlatformUID.size() > 0)
		{
			PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.PARTIAL_ADDRESS_BOOK, msisdnForMissingPlatformUID.toArray(new String[] {}));
		}
		
		requestToken = updateAddressBookRequest(postObject, getRequestListener());
		requestToken.execute();
		return ContactUtils.getContactList(resultObject, new_contacts_by_id);
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				resultObject = response;
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				resultObject = null;
				// TODO Below code is for investigating an issue where invalid json is received at server end , should be removed once issue is solved
				if (httpException.getErrorCode() == HttpURLConnection.HTTP_BAD_REQUEST)
				{
					IRequestBody requestBody = requestToken.getRequestBody();
					if (requestBody instanceof JsonBody)
					{
						byte[] bytes = ((JsonBody) requestBody).getBytes();
						recordAddressBookUploadFailException(new String(bytes));
					}
				}
			}
		};
	}

	private JSONObject getPostObject()
	{
		JSONObject data = null;
		try
		{
			data = new JSONObject();
			data.put("remove", ids_json);
			data.put("update", ContactUtils.getJsonContactList(new_contacts_by_id, false));
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "Invalid JSON put", e);
		}
		return data;
	}
	
	private void recordAddressBookUploadFailException(String jsonString)
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EXCEPTION_ANALYTIS_ENABLED, false))
		{
			return;
		}
		try
		{
			JSONObject metadata = new JSONObject();

			metadata.put(HikeConstants.PAYLOAD, jsonString);

			Logger.d(getClass().getSimpleName(), "recording update addressbook upload fail event. json = " + jsonString);
			HAManager.getInstance().record(HikeConstants.EXCEPTION, HikeConstants.LogEvent.ADDRESSBOOK_UPLOAD, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "invalid json");
		}
	}
}