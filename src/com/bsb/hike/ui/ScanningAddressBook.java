package com.bsb.hike.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeUserDatabase;

public class ScanningAddressBook extends Activity
{

	private class ScanAddressBookTask extends AsyncTask<Void, Void, Void>
	{

		private ScanningAddressBook mActivity;

		private Cursor mContacts;

		private Cursor mPhones;

		public ScanAddressBookTask(ScanningAddressBook activity)
		{
			mActivity = activity;
		}

		public void setActivity(ScanningAddressBook activity)
		{
			if (activity == null)
			{
				mActivity.stopManagingCursor(mContacts);
				mActivity.stopManagingCursor(mPhones);
			}
			else
			{
				activity.startManagingCursor(mContacts);
				activity.startManagingCursor(mPhones);
			}
			mActivity = activity;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts.DISPLAY_NAME };

			String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
			mContacts = mActivity.managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			int idFieldColumnIndex = mContacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = mContacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
			Log.d("SAB", "Starting to scan address book");
			long tm = System.currentTimeMillis();
			Map<String, String> contactNames = new HashMap<String, String>();
			while (mContacts.moveToNext())
			{
				String id = mContacts.getString(idFieldColumnIndex);
				String name = mContacts.getString(nameFieldColumnIndex);
				contactNames.put(id, name);
			}

			mPhones = mActivity.managedQuery(Phone.CONTENT_URI, new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null, null, null);

			int numberColIdx = mPhones.getColumnIndex(Phone.NUMBER);
			int idColIdx = mPhones.getColumnIndex(Phone.CONTACT_ID);
			while (mPhones.moveToNext())
			{
				String number = mPhones.getString(numberColIdx);
				String id = mPhones.getString(idColIdx);
				String name = contactNames.get(id);
				if ((name != null) && (number != null))
				{
					contactinfos.add(new ContactInfo(id, number, name));
				}
			}

			Log.d("SAB", "Finished scan address book " + (System.currentTimeMillis() - tm));
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			HikeUserDatabase db = null;
			try
			{
				List<ContactInfo> addressbook = AccountUtils.postAddressBook(token, contactinfos);
				//TODO this exception should be raised from the postAddressBook code
				if (addressbook == null)
				{
					throw new IOException("Unable to retrieve address book");
				}
				Log.d("SAB", "about to insert");
				db = new HikeUserDatabase(ScanningAddressBook.this);
				db.setAddressBook(addressbook);

				/* Add a default message from hike */
				// TODO get the number for hikebot from the server?
				ContactInfo hikeContactInfo = new ContactInfo("__HIKE__", "TD-HIKE", "HikeBot");
				hikeContactInfo.onhike = true;
				db.addContact(hikeContactInfo);
				ConvMessage message = new ConvMessage(getResources().getString(R.string.hikebot_message), hikeContactInfo.number,
						System.currentTimeMillis() / 1000, ConvMessage.State.RECEIVED_UNREAD);

				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED_FROM_OTHER_CLIENT, message);
			}
			catch (Exception e)
			{
				Log.e("ScanningAddressBook", "Unable to post address book", e);
				Intent intent = new Intent(ScanningAddressBook.this, AccountCreateSuccess.class);
				intent.putExtra("failed", true);
				startActivity(intent);
				finish();
				return null;
			}
			finally
			{
				if (db != null)
				{
					db.close();
				}
			}

			Log.d("ScanningAddressBook", "Finished scanning addressbook");
			SharedPreferences.Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, true);
			editor.commit();

			Intent intent = new Intent(ScanningAddressBook.this, MessagesList.class);
			startActivity(intent);
			finish();
			
			return null;
		}
	}

	ScanAddressBookTask mTask;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		mTask.setActivity(null);
		return mTask;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// TODO this is called when you rotate the screen. We shouldn't
		Log.d(ScanningAddressBook.class.getSimpleName(), "onCreate");
		setContentView(R.layout.scanningcontactlist);
		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ScanAddressBookTask)
		{
			mTask = (ScanAddressBookTask) retained;
			mTask.setActivity(this);
		}
		else
		{
			mTask = new ScanAddressBookTask(this);
			mTask.execute();
		}
	}

}
