package com.bsb.hike.tasks;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;

public class SignupTask extends AsyncTask<Void, SignupTask.StateValue, Boolean> implements ActivityCallableTask
{
	
	private class SMSReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Bundle extras = intent.getExtras();
			if (extras != null)
			{
				Object[] extra = (Object[]) extras.get("pdus");
				for(int i = 0; i < extra.length; ++i)
				{
					SmsMessage sms = SmsMessage.createFromPdu((byte[]) extra[i]);
					String body = sms.getMessageBody();
					String pin = Utils.getSMSPinCode(body);
					if (pin != null)
					{
						SignupTask.this.addUserInput(pin);
						this.abortBroadcast();
						break;
					}
				}
			}
		}
	}

	public enum State
	{
		MSISDN,
		ADDRESSBOOK,
		NAME,
		PIN,
		ERROR
	};

	public class StateValue
	{
		public State state;
		public String value;
		public StateValue(State state, String value)
		{
			this.state = state;
			this.value = value;
		}
	};

	private SignupActivity signupActivity;
	private Context context;
	private String data;
	private SMSReceiver receiver;

	public SignupTask(SignupActivity activity)
	{
		this.signupActivity = activity;
		this.context = activity;
	}

	public void addUserInput(String string)
	{
		this.data = string;
		synchronized(this)
		{
			this.notify();
		}
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		SharedPreferences settings = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		boolean ab_scanned = settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false);
		String name = settings.getString(HikeMessengerApp.NAME_SETTING, null);
		
		if (isCancelled())
		{
			/* just gtfo */
			Log.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}
		
		if (msisdn == null)
		{
			/* need to get the MSISDN */
			AccountUtils.AccountInfo accountInfo = AccountUtils.registerAccount(null,null);
			if (accountInfo == null)
			{
				/* network error, signal a failure */
				publishProgress(new StateValue(State.MSISDN, null));
				return Boolean.FALSE;
			}

			if (TextUtils.isEmpty(accountInfo.msisdn))
			{
				/* no MSISDN, ask the user for it */
				publishProgress(new StateValue(State.MSISDN, ""));
				/* wait until we're notified that we have the msisdn */
				try
				{
					synchronized(this)
					{
						this.wait();	
					}
				}
				catch (InterruptedException e)
				{
					Log.d("SignupTask", "Interrupted exception while waiting for msisdn", e);
					publishProgress(new StateValue(State.MSISDN, null));
					return Boolean.FALSE;
				}

				String number = this.data;
				this.data = null;

				/* register broadcast receiver to get the actual PIN code, and pass it to us */
				IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
				intentFilter.setPriority(99);
				receiver = new SMSReceiver();
				
				this.context.getApplicationContext().registerReceiver(receiver, new IntentFilter(intentFilter));
				String unauthedMSISDN = AccountUtils.validateNumber(number);

				if (unauthedMSISDN != null)
				{
					synchronized(this)
					{
						/* wait until we get an SMS from the server */
						try
						{
							/* TODO add a timeout so if we don't get the SMS,
							 * we throw an error an ask the user enter manually */
							this.wait(15*1000);
						}
						catch (InterruptedException e)
						{
							Log.e("SignupTask", "Task was interrupted", e);
						}
					}
				}

				this.context.getApplicationContext().unregisterReceiver(receiver);
				receiver = null;
				
				if(this.data == null){
					data="";
					publishProgress(new StateValue(State.PIN, data));
					
					synchronized (this)
					{
						try
						{
							this.wait();
						}
						catch (InterruptedException e)
						{
							Log.e("SignupTask", "Task was interrupted while taking the pin", e);
						}
					}
				}
				
				if (isCancelled())
				{
					/* just gtfo */
					Log.d("SignupTask", "Task was cancelled");
					return Boolean.FALSE;
				}

				accountInfo = null;
				String pin = this.data;
				accountInfo = AccountUtils.registerAccount(pin, unauthedMSISDN);
				if (accountInfo == null)
				{
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}
				Utils.savedAccountCredentials(accountInfo, settings.edit());

				msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
			}
			else
			{
				msisdn = accountInfo.msisdn;
				/* save the new msisdn */
				Utils.savedAccountCredentials(accountInfo, settings.edit());
			}
		}

		/* msisdn set, yay */
		publishProgress(new StateValue(State.MSISDN, msisdn));
		
		if (isCancelled())
		{
			/* just gtfo */
			Log.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}
		
		/* scan the addressbook */
		if (!ab_scanned)
		{
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			List<ContactInfo> contactinfos = ContactUtils.getContacts(this.context);
			HikeUserDatabase db = null;
			try
			{
				Map<String, List<ContactInfo>> contacts = ContactUtils.convertToMap(contactinfos);
				List<ContactInfo> addressbook = AccountUtils.postAddressBook(token, contacts);
				//TODO this exception should be raised from the postAddressBook code
				if (addressbook == null)
				{
					throw new IOException("Unable to retrieve address book");
				}
				Log.d("SignupTask", "about to insert addressbook");
				db = new HikeUserDatabase(this.context);
				db.setAddressBook(addressbook);
			}
			catch (Exception e)
			{
				Log.e("SignupTask", "Unable to post address book", e);
				publishProgress(new StateValue(State.ADDRESSBOOK, null));
				return Boolean.FALSE;
			}
			finally
			{
				if (db != null)
				{
					db.close();
				}
			}

			Editor editor = settings.edit();
			editor.putBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, true);
			editor.commit();
		}

		/* addressbook scanned, sick
		 */
		publishProgress(new StateValue(State.ADDRESSBOOK, ""));
		
		if (isCancelled())
		{
			/* just gtfo */
			Log.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}
		
		if (name == null)
		{
			/* publishing this will cause the the Activity to ask the user for a name and signal us */
			publishProgress(new StateValue(State.NAME, ""));
			try
			{
				synchronized(this)
				{
					this.wait();					
				}

				name = this.data;
				AccountUtils.setName(name);
			}
			catch (InterruptedException e)
			{
				Log.e("SignupTask", "Interrupted exception while waiting for name", e);
				publishProgress(new StateValue(State.NAME, null));
				return Boolean.FALSE;
			}
			catch (NetworkErrorException e)
			{
				Log.e("SignupTask", "Unable to set name", e);
				publishProgress(new StateValue(State.NAME, null));
				return Boolean.FALSE;
			}

			this.data = null;
			Editor editor = settings.edit();
			editor.putString(HikeMessengerApp.NAME_SETTING, name);
			editor.commit();
		}

		/* set the name */
		publishProgress(new StateValue(State.NAME, name));
		/* operation successful, chill for a second */
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
		}
		return Boolean.TRUE;
	}
	
	@Override
	protected void onCancelled() 
	{
		Log.d("SignupTask", "onCancelled called");
		/*
		 * For removing intent when finishing the activity
		 */
		if (receiver != null)
		{
			this.context.getApplicationContext().unregisterReceiver(receiver);
			receiver = null;
		}
	}
	
	@Override
	protected void onPostExecute(Boolean result)
	{
		signupActivity.onFinish(result.booleanValue());
	}

	@Override
	protected void onProgressUpdate(StateValue... values)
	{
		signupActivity.onProgressUpdate(values[0]);
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.context = activity;
		this.signupActivity = (SignupActivity) activity;
	}

	@Override
	public boolean isFinished()
	{
		return false;
	}
	
	public void cancelTask()
	{
		this.cancel(true);
		Log.d("SignupTask", "cancelling it manually");
		/*
		 * For removing intent when finishing the activity
		 */
		if (receiver != null)
		{
			this.context.unregisterReceiver(receiver);
			receiver = null;
		}
	}

}
