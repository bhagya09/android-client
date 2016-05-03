package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ContactUtils
{
	public static JSONObject getJsonContactList(Map<String, List<ContactInfo>> contactsMap, boolean sendWAValue)
	{
		JSONObject updateContacts = new JSONObject();
		for (String id : contactsMap.keySet())
		{
			try
			{
				List<ContactInfo> list = contactsMap.get(id);
				JSONArray contactInfoList = new JSONArray();
				for (ContactInfo cInfo : list)
				{
					JSONObject contactInfo = new JSONObject();
					contactInfo.put("name", cInfo.getName());
					contactInfo.put("phone_no", cInfo.getPhoneNum());
					if (sendWAValue)
					{
						contactInfo.put("t1", calculateGreenBlueValue(cInfo.isOnGreenBlue()));
					}
					contactInfoList.put(contactInfo);
				}
				updateContacts.put(id, contactInfoList);
			}
			catch (JSONException e)
			{
				Logger.d("ACCOUNT UTILS", "Json exception while getting contact list.");
				e.printStackTrace();
			}
		}
		return updateContacts;
	}

	public static JSONObject getWAJsonContactList(List<ContactInfo> contactsList)
	{
		JSONObject contactsJson = new JSONObject();
		try
		{
			for (ContactInfo cInfo : contactsList)
			{
				JSONObject waInfoObject = new JSONObject();
				waInfoObject.put("t1", calculateGreenBlueValue(cInfo.isOnGreenBlue()));
				contactsJson.put(cInfo.getMsisdn(), waInfoObject);
			}

		}
		catch (JSONException e)
		{
			Logger.d("ACCOUNT UTILS", "Json exception while getting WA info list.");
			e.printStackTrace();
		}
		return contactsJson;
	}

	public static List<ContactInfo> getContactList(JSONObject obj, Map<String, List<ContactInfo>> new_contacts_by_id)
	{
		List<ContactInfo> server_contacts = new ArrayList<ContactInfo>();
		JSONObject addressbook;
		try
		{
			if ((obj == null) || ("fail".equals(obj.optString("stat"))))
			{
				Logger.w("HTTP", "Unable to upload address book");
				// TODO raise a real exception here
				return null;
			}
			Logger.d("AccountUtils", "Reply from addressbook:" + obj.toString());
			addressbook = obj.getJSONObject("addressbook");
		}
		catch (JSONException e)
		{
			Logger.e("AccountUtils", "Invalid json object", e);
			return null;
		}

		for (Iterator<?> it = addressbook.keys(); it.hasNext();)
		{
			String id = (String) it.next();
			JSONArray entries = addressbook.optJSONArray(id);
			List<ContactInfo> cList = new_contacts_by_id.get(id);
			for (int i = 0; i < entries.length(); ++i)
			{
				JSONObject entry = entries.optJSONObject(i);
				String msisdn = entry.optString("m");
				boolean onhike = entry.optBoolean("oh");
				String platformId = entry.optString("pu");
				String uid=entry.optString("u");
				Logger.d("AddressBookPost","MSISDN : "+ msisdn + " UID : "+ uid + "ON HIKE STATUS : "+ onhike);
				ContactInfo info = new ContactInfo(id, msisdn, cList.get(i).getName(), cList.get(i).getPhoneNum(), onhike, platformId,uid);
				server_contacts.add(info);
			}
		}
		return server_contacts;
	}

	public static List<String> getBlockList(JSONObject obj)
	{
		JSONArray blocklist;
		List<String> blockListMsisdns = new ArrayList<String>();
		if ((obj == null) || ("fail".equals(obj.optString("stat"))))
		{
			Logger.w("HTTP", "Unable to upload address book");
			// TODO raise a real exception here
			return null;
		}
		Logger.d("AccountUtils", "Reply from addressbook:" + obj.toString());
		blocklist = obj.optJSONArray("blocklist");
		if (blocklist == null)
		{
			Logger.e("AccountUtils", "Received blocklist as null");
			return null;
		}

		for (int i = 0; i < blocklist.length(); i++)
		{
			try
			{
				blockListMsisdns.add(blocklist.getString(i));
			}
			catch (JSONException e)
			{
				Logger.e("AccountUtils", "Invalid json object", e);
				return null;
			}
		}
		return blockListMsisdns;
	}

	public static int calculateGreenBlueValue(boolean isOnGreenBlue)
	{
		int rand = (new Random()).nextInt(100);
		int msb = (rand / 10);
		if (isOnGreenBlue)
		{
			return ((msb & 1) == 0 ? rand : (rand + 10) % 100);
		}
		else
		{
			return ((msb & 1) != 0 ? rand : (rand + 10));
		}
	}

	public static void triggerSyncContacts(Context mContext)
	{
		if(!HikeMessengerApp.syncingContacts)
		{
			if(Utils.isUserOnline(mContext))
			{
				Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
				contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
				mContext.sendBroadcast(contactSyncIntent);
			}
		}
	}
}
