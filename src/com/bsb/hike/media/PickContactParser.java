package com.bsb.hike.media;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.PhonebookContact;

public class PickContactParser
{
	public static PhonebookContact onContactResult(int resultCode, Intent data, Context context)
	{
		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null)
		{
			return null;
		}
		else
		{
			return getContactData(data.getData().getLastPathSegment(), context);
		}
	}

	/**
	 * This function queries contact table for data, It is preferable to call this function in non UI thread
	 * 
	 * @param contactId
	 * @param context
	 * @return {@link PhonebookContact} Or null if some exception occurs
	 */
	public static PhonebookContact getContactData(String contactId, Context context)
	{
		PhonebookContact contact = new PhonebookContact();
		StringBuilder mimeTypes = new StringBuilder("(");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Phone.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Email.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(StructuredPostal.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Event.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(StructuredName.CONTENT_ITEM_TYPE) + ")");

		String selection = Data.CONTACT_ID + " =? AND " + Data.MIMETYPE + " IN " + mimeTypes.toString();

		String[] projection = new String[] { Data.DATA1, Data.DATA2, Data.DATA3, Data.MIMETYPE, Data.DISPLAY_NAME };

		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, selection, new String[] { contactId }, null);

		int data1Idx = c.getColumnIndex(Data.DATA1);
		int data2Idx = c.getColumnIndex(Data.DATA2);
		int data3Idx = c.getColumnIndex(Data.DATA3);
		int mimeTypeIdx = c.getColumnIndex(Data.MIMETYPE);
		int nameIdx = c.getColumnIndex(Data.DISPLAY_NAME);

		JSONObject contactJson = new JSONObject();

		JSONArray phoneNumbersJson = null;
		JSONArray emailsJson = null;
		JSONArray addressesJson = null;
		JSONArray eventsJson = null;

		List<ContactInfoData> items = new ArrayList<ContactInfoData>();
		String name = null;
		try
		{
			while (c.moveToNext())
			{
				String mimeType = c.getString(mimeTypeIdx);

				if (!contactJson.has(HikeConstants.NAME))
				{
					String dispName = c.getString(nameIdx);
					contactJson.put(HikeConstants.NAME, dispName);
					name = dispName;
				}

				if (Phone.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (phoneNumbersJson == null)
					{
						phoneNumbersJson = new JSONArray();
						contactJson.put(HikeConstants.PHONE_NUMBERS, phoneNumbersJson);
					}

					String type = Phone.getTypeLabel(context.getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String msisdn = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, msisdn);
					phoneNumbersJson.put(data);

					items.add(new ContactInfoData(DataType.PHONE_NUMBER, msisdn, type));
				}
				else if (Email.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (emailsJson == null)
					{
						emailsJson = new JSONArray();
						contactJson.put(HikeConstants.EMAILS, emailsJson);
					}

					String type = Email.getTypeLabel(context.getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String email = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, email);
					emailsJson.put(data);

					items.add(new ContactInfoData(DataType.EMAIL, email, type));
				}
				else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (addressesJson == null)
					{
						addressesJson = new JSONArray();
						contactJson.put(HikeConstants.ADDRESSES, addressesJson);
					}

					String type = StructuredPostal.getTypeLabel(context.getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String address = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, address);
					addressesJson.put(data);

					items.add(new ContactInfoData(DataType.ADDRESS, address, type));
				}
				else if (Event.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (eventsJson == null)
					{
						eventsJson = new JSONArray();
						contactJson.put(HikeConstants.EVENTS, eventsJson);
					}

					String event;
					int eventType = c.getInt(data2Idx);
					if (eventType == Event.TYPE_ANNIVERSARY)
					{
						event = context.getString(R.string.anniversary);
					}
					else if (eventType == Event.TYPE_OTHER)
					{
						event = context.getString(R.string.other);
					}
					else if (eventType == Event.TYPE_BIRTHDAY)
					{
						event = context.getString(R.string.birthday);
					}
					else
					{
						event = c.getString(data3Idx);
					}
					String type = event.toString();
					String eventDate = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, eventDate);
					eventsJson.put(data);

					items.add(new ContactInfoData(DataType.EVENT, eventDate, type));
				}
			}
			contact.name = name;
			contact.items = items;
			contact.jsonData = contactJson;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}

		return contact;

	}
}
