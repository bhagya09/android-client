package com.bsb.hike.tasks;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.updateAddressBookRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;

public class UpdateAddressBookTask
{
	private Map<String, List<ContactInfo>> new_contacts_by_id;

	private JSONArray ids_json;

	private JSONObject resultObject;

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
			PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformUIDFetchType.PARTIAL_ADDRESS_BOOK, msisdnForMissingPlatformUID.toArray(new String[] {}));
		}

		RequestToken requestToken = updateAddressBookRequest(postObject, getRequestListener());
		requestToken.execute();
		return AccountUtils.getContactList(resultObject, new_contacts_by_id);
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
			public void onRequestFailure(HttpException httpException)
			{
				resultObject = null;
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
			data.put("update", AccountUtils.getJsonContactList(new_contacts_by_id, false));
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "Invalid JSON put", e);
		}
		return data;
	}
}