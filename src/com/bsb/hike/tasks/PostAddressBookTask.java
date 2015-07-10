package com.bsb.hike.tasks;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.postAddressBookRequest;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;

public class PostAddressBookTask
{
	private Map<String, List<ContactInfo>> contactsMap;

	private JSONObject resultObject;

	public PostAddressBookTask(Map<String, List<ContactInfo>> contactsMap)
	{
		this.contactsMap = contactsMap;
	}

	public JSONObject execute()
	{
		JSONObject postObject = getPostObject();
		if (postObject == null)
		{
			return null;
		}
		RequestToken requestToken = postAddressBookRequest(postObject, getRequestListener());
		requestToken.execute();
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
		data = ContactUtils.getJsonContactList(contactsMap, true);
		return data;
	}
}