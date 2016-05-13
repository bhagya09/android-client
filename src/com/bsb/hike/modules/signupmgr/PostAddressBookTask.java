package com.bsb.hike.modules.signupmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DbException;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.PairModified;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.postAddressBookRequest;

public class PostAddressBookTask
{
	private Map<String, List<ContactInfo>> contactsMap;

	private JSONObject resultObject;

	public PostAddressBookTask(Map<String, List<ContactInfo>> contactsMap)
	{
		this.contactsMap = contactsMap;
	}

	public boolean execute()
	{
		JSONObject postObject = getPostObject();
		if (postObject == null)
		{
			return false;
		}
		
		RequestToken requestToken = postAddressBookRequest(postObject, getRequestListener(), getResponseInterceptor(), new SignUpHttpRetryPolicy(SignUpHttpRetryPolicy.MAX_RETRY_COUNT, BasicRetryPolicy.DEFAULT_RETRY_DELAY, BasicRetryPolicy.DEFAULT_BACKOFF_MULTIPLIER));
		requestToken.execute();
		return (resultObject != null);
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

	private IResponseInterceptor getResponseInterceptor()
	{
		return new IResponseInterceptor()
		{
			@Override
			public void intercept(Chain chain)
			{
				JSONObject jsonForAddressBookAndBlockList = (JSONObject) chain.getResponseFacade().getBody().getContent();

				if (jsonForAddressBookAndBlockList == null)
				{
					return;
				}

				List<ContactInfo> addressbook = ContactUtils.getContactList(jsonForAddressBookAndBlockList, contactsMap);
				List<PairModified<String,String>> blockList = ContactUtils.getBlockList(jsonForAddressBookAndBlockList);

				if (jsonForAddressBookAndBlockList.has(HikeConstants.PREF))
				{
					JSONObject prefJson = jsonForAddressBookAndBlockList.optJSONObject(HikeConstants.PREF);
					JSONArray contactsArray = prefJson.optJSONArray(HikeConstants.CONTACTS);
					if (contactsArray != null)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, contactsArray.toString());
					}
				}

				try
				{
					ContactManager.getInstance().setAddressBookAndBlockList(addressbook, blockList);
					chain.proceed();
				}
				catch (DbException e)
				{
					resultObject = null;
					e.printStackTrace();
				}
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