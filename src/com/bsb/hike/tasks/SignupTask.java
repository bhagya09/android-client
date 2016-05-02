package com.bsb.hike.tasks;

import static com.bsb.hike.backup.AccountBackupRestore.STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION;
import static com.bsb.hike.backup.AccountBackupRestore.STATE_RESTORE_FAILURE_MSISDN_MISMATCH;
import static com.bsb.hike.backup.AccountBackupRestore.STATE_RESTORE_SUCCESS;

import java.io.File;
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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.backup.AccountBackupRestore.RestoreErrorStates;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.AccountInfo;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.signupmgr.PostAddressBookTask;
import com.bsb.hike.modules.signupmgr.RegisterAccountTask;
import com.bsb.hike.modules.signupmgr.SetProfileTask;
import com.bsb.hike.modules.signupmgr.ValidateNumberTask;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.crashlytics.android.Crashlytics;
import com.bsb.hike.backup.BackupUtils;

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
				for (int i = 0; i < extra.length; ++i)
				{
					SmsMessage sms = SmsMessage.createFromPdu((byte[]) extra[i]);
					String body = sms.getMessageBody();
					String pin = null;

					if (!TextUtils.isEmpty(body))
					{
						pin = Utils.getSMSPinCode(body);
					}

					if (pin != null)
					{
						if(getDisplayChild() != SignupActivity.PIN){
							SignupTask.this.addUserInput(pin);
						} else{
							SignupTask.this.autoFillPin(pin);
						}
						this.abortBroadcast();
						break;
					}
				}
			}
		}
	}

	public interface OnSignupTaskProgressUpdate
	{
		public void onProgressUpdate(StateValue value);
		
		public void onFinish(boolean success);
	}
	
	public int getDisplayChild()
	{
		/**
		 * This is being added here because SignupTask can be called from WelcomeActivity as well as SignupActivity. We were getting ClassCast Exceptions for Context, hence this
		 * defensive check
		 */
		if (context instanceof SignupActivity)
		{
			return ((SignupActivity) context).getDisplayItem();
		}

		else
		{
			return -1;
		}
	}

	public void autoFillPin(String pin)
	{
		((SignupActivity) context).autoFillPin(pin);
	}

	public enum State
	{
		MSISDN, ADDRESSBOOK, NAME, PULLING_PIN, PIN, ERROR, PROFILE_IMAGE, GENDER, SCANNING_CONTACTS, PIN_VERIFIED, BACKUP_AVAILABLE, RESTORING_BACKUP, RESTORING_CLOUD_SETTINGS, SHOW_STICKER_RESTORE_DIALOG
	}

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

	private Context context;

	private String data;

	private SMSReceiver receiver;

	/**
	 * Making this public for preactivation case.We don't want to show any notification while the user is
	 *  signing up.So making this public to achieve the same
	 */
	public static SignupTask signupTask;

	private OnSignupTaskProgressUpdate onSignupTaskProgressUpdate;

	private boolean isRunning = false;

	private boolean isPinError = false;

	private HikeHttpRequest profilePicRequest;

	private Bitmap profilePicSmall;

	public static boolean isAlreadyFetchingNumber = false;

	private Birthday birthdate;

	private Boolean isFemale;

	private String userName;
		
	private static final String INDIA_ISO = "IN";

	public static final String START_UPLOAD_PROFILE = "start";

	public static final String FINISHED_UPLOAD_PROFILE = "finish";
	
	public boolean isRunning()
	{
		return isRunning;
	}

	private SignupTask(Activity activity)
	{
		this.onSignupTaskProgressUpdate = (OnSignupTaskProgressUpdate) activity;
		this.context = activity;
		SignupTask.isAlreadyFetchingNumber = false; 
	}

	public static SignupTask getSignupTask(Activity activity)
	{
		if (signupTask == null || signupTask.isCancelled())
		{
			signupTask = new SignupTask(activity);
		}
		else
		{
			signupTask.setActivity(activity);
		}
		return signupTask;
	}

	public void addUserInput(String string)
	{
		this.data = string;
		synchronized (this)
		{
			this.notify();
		}
	}

	public void addProfilePicPath(String path, Bitmap profilePic)
	{
		if(path != null)
		{
			Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putString(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH, path);
			editor.commit();
		}
		this.profilePicSmall = profilePic;
	}

	public void addBirthdate(Birthday birthdate)
	{
		this.birthdate = birthdate;
	}

	public void addUserName(String name)
	{
		this.userName = name;
	}

	
	public void addGender(Boolean isFemale)
	{
		this.isFemale = isFemale;
	}

	@Override
	protected void onPreExecute()
	{
		isRunning = true;
		super.onPreExecute();
	}
	
	@Override
	protected Boolean doInBackground(Void... unused)
	{
		Logger.e("SignupTask", "FETCHING NUMBER? " + isAlreadyFetchingNumber);
		isPinError = false;
		SharedPreferences settings = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		boolean ab_scanned = settings.getBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, false);
		boolean canPullInSms = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
		String name = settings.getString(HikeMessengerApp.NAME_SETTING, null);
		Boolean restored = settings.getBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, false);

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		if (msisdn == null)
		{

			/*
			 * need to get the MSISDN. If we're on Wifi don't bother trying to autodetect
			 */
			deletePreviouslySavedProfileImages();
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = manager.getNetworkCountryIso().toUpperCase();

			AccountInfo accountInfo = null;
			if (!SignupTask.isAlreadyFetchingNumber && INDIA_ISO.equals(countryIso) && !wifi.isConnected())
			{
				accountInfo = new RegisterAccountTask(null, null).execute();
				if (accountInfo == null)
				{
					/* network error, signal a failure */
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}
			}
			if (accountInfo == null || TextUtils.isEmpty(accountInfo.getMsisdn()))
			{
				if (!SignupTask.isAlreadyFetchingNumber)
				{
					/* no MSISDN, ask the user for it */
					publishProgress(new StateValue(State.MSISDN, ""));
					SignupTask.isAlreadyFetchingNumber = true;
				}
				/* wait until we're notified that we have the msisdn */
				try
				{
					synchronized (this)
					{
						this.wait();
					}
				}
				catch (InterruptedException e)
				{
					Logger.d("SignupTask", "Interrupted exception while waiting for msisdn", e);
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}

				String number = this.data;
				this.data = null;
				Logger.d("SignupTask", "NUMBER RECEIVED: " + number);

				if (canPullInSms)
				{
					/*
					 * register broadcast receiver to get the actual PIN code, and pass it to us
					 */
					IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
					intentFilter.setPriority(999);
					receiver = new SMSReceiver();

					this.context.getApplicationContext().registerReceiver(receiver, new IntentFilter(intentFilter));
				}

				String unauthedMSISDN = new ValidateNumberTask(number).execute();
				
				if (TextUtils.isEmpty(unauthedMSISDN))
				{
					Logger.d("SignupTask", "Unable to send PIN to user");
					publishProgress(new StateValue(State.ERROR, null));
					return Boolean.FALSE;
				}

				/*
				 * Saving this for the call me functionality
				 */
				Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.MSISDN_ENTERED, unauthedMSISDN);
				editor.commit();

				if (canPullInSms)
				{
					publishProgress(new StateValue(State.PULLING_PIN, null));

					synchronized (this)
					{
						/* wait until we get an SMS from the server */
						try
						{
							this.wait(HikeConstants.PIN_CAPTURE_TIME);
						}
						catch (InterruptedException e)
						{
							Logger.e("SignupTask", "Task was interrupted", e);
						}
					}

				}
				
				accountInfo = null;
				do
				{
					if (this.data == null)
					{
						data = "";
						if (!isPinError)
						{
							publishProgress(new StateValue(State.PIN, data));
						}
						synchronized (this)
						{
							try
							{
								this.wait();
							}
							catch (InterruptedException e)
							{
								Logger.e("SignupTask", "Task was interrupted while taking the pin", e);
							}
						}
					}
					if (isCancelled())
					{
						/* just gtfo */
						Logger.d("SignupTask", "Task was cancelled");
						return Boolean.FALSE;
					}
					String pin = this.data;
					if (TextUtils.isEmpty(pin))
					{
						publishProgress(new StateValue(State.ERROR, HikeConstants.CHANGE_NUMBER));
						return Boolean.FALSE;
					}
					accountInfo = new RegisterAccountTask(pin, unauthedMSISDN).execute();
					/*
					 * if it fails, we try once again.
					 */
					if (accountInfo == null)
					{
						accountInfo = new RegisterAccountTask(pin, unauthedMSISDN).execute();
					}

					if (accountInfo == null)
					{
						this.data = null;
						publishProgress(new StateValue(State.ERROR, null));
						return Boolean.FALSE;
					}
					else if (accountInfo.getSmsCredits() == -1)
					{
						this.data = null;
						isPinError = true;
						publishProgress(new StateValue(State.PIN, HikeConstants.PIN_ERROR));
					}
				}
				while (this.data == null);
				
				if(canPullInSms && receiver != null)
				{
					this.context.getApplicationContext().unregisterReceiver(receiver);
					receiver = null;
				}
				
				publishProgress(new StateValue(State.PIN_VERIFIED, null));
				synchronized (this)
				{
					try
					{
						this.wait();
					}
					catch (InterruptedException e)
					{
						Logger.e("SignupTask", "Task was interrupted while taking the pin", e);
					}
				}
				
			}

			Logger.d("SignupTask", "saving MSISDN/Token");
			msisdn = accountInfo.getMsisdn();
			/* save the new msisdn */
			Utils.savedAccountCredentials(accountInfo, settings.edit());
			//Check for crash reporting tool
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CRASH_REPORTING_TOOL, HikeConstants.ACRA).equals(HikeConstants.CRASHLYTICS))
			{
				Crashlytics.setUserIdentifier(accountInfo.getUid());
			}
			String hikeUID = accountInfo.getUid();
			String hikeToken = accountInfo.getToken();
			if (!TextUtils.isEmpty(hikeUID) && !TextUtils.isEmpty(hikeToken))
			{
				PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.SELF);
			}
			/* msisdn set, yay */
			publishProgress(new StateValue(State.MSISDN, msisdn));
		}
		else
		{
			if (!(context instanceof SignupActivity))
			{
				//Ideally UI should be on SignupActivity at this point   
				//if it is not than we need to update UI.
				publishProgress(new StateValue(State.MSISDN, HikeConstants.DONE));
			}
		}
		
		this.data = null;
		// We're doing this to prevent the WelcomeScreen from being shown the
		// next time we start the app.
		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		Editor ed = this.context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		ed.putBoolean(HikeMessengerApp.ACCEPT_TERMS, true);
		ed.commit();
		
		/*
		 * the below logic is to correctly update signup activity views
		 * 1. if we already have user gender that means now we need to move
		 * to final state of scanning contacts screen
		 * 2. if we don't have gender but name means we need to show gender screen
		 * 3. if we don't have both of the above means we need to show name screen
		 */
		if(isFemale != null)
		{
			publishProgress(new StateValue(State.SCANNING_CONTACTS, ""));
		}
		else if(userName != null)
		{
			publishProgress(new StateValue(State.GENDER, ""));
		}
		else
		{
			publishProgress(new StateValue(State.NAME, ""));
		}
		
		
		/* scan the addressbook */
		if (!ab_scanned)
		{
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			ContactManager conMgr = ContactManager.getInstance();
			List<ContactInfo> contactinfos = null;
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HIDE_DELETED_CONTACTS, false))
			{
				contactinfos = conMgr.getContacts(this.context);
			}
			else
			{
				contactinfos = conMgr.getContactsOld(this.context);
			}
			if(contactinfos == null)
			{
				return Boolean.FALSE;
			}
			conMgr.setGreenBlueStatus(this.context, contactinfos);
			
			try
			{
				Logger.d("Signup", "Starting AB scanning");
				Map<String, List<ContactInfo>> contacts = conMgr.convertToMap(contactinfos);

				boolean addressBookPosted = new PostAddressBookTask(contacts).execute();
				if (addressBookPosted == false)
				{
					publishProgress(new StateValue(State.ERROR, HikeConstants.ADDRESS_BOOK_ERROR));
					return Boolean.FALSE;
				}
			}
			catch (Exception e)
			{
				Logger.e("SignupTask", "Unable to post address book", e);
				publishProgress(new StateValue(State.ERROR, HikeConstants.ADDRESS_BOOK_ERROR));
				return Boolean.FALSE;
			}

			Editor editor = settings.edit();
			editor.putBoolean(HikeMessengerApp.ADDRESS_BOOK_SCANNED, true);
			editor.putBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, true);
			editor.commit();
			/*
			 * addressbook scanned, sick
			 */
			publishProgress(new StateValue(State.ADDRESSBOOK, ""));
		}
		else
		{
			publishProgress(new StateValue(State.ADDRESSBOOK, HikeConstants.DONE));
		}
		
		Logger.d("Signup", "AB scanning done");

		if (isCancelled())
		{
			/* just gtfo */
			Logger.d("SignupTask", "Task was cancelled");
			return Boolean.FALSE;
		}

		if (name == null)
		{
			try
			{
				if (userName == null)
				{
					/*
					 * publishing this will cause the the Activity to ask the user for a name and signal us
					 */
					publishProgress(new StateValue(State.NAME, ""));
					synchronized (this)
					{
						this.wait();
					}
				}
				
				if (isFemale == null)
				{
					/*
					 * publishing this will cause the the Activity to ask the user for a name and signal us
					 */
					publishProgress(new StateValue(State.GENDER, ""));
					synchronized (this)
					{
						this.wait();
					}
				}
				
				if(getDisplayChild() != SignupActivity.SCANNING_CONTACTS)
				{
					publishProgress(new StateValue(State.SCANNING_CONTACTS, ""));
				}
				publishProgress(new StateValue(State.PROFILE_IMAGE, START_UPLOAD_PROFILE));
				
				new SetProfileTask(userName, birthdate, isFemale.booleanValue()).execute();
			}
			catch (InterruptedException e)
			{
				Logger.e("SignupTask", "Interrupted exception while waiting for name", e);
				publishProgress(new StateValue(State.ERROR, null));
				return Boolean.FALSE;
			}
			catch (NetworkErrorException e)
			{
				Logger.e("SignupTask", "Unable to set name", e);
				publishProgress(new StateValue(State.ERROR, null));
				return Boolean.FALSE;
			}
			catch (IllegalStateException e)
			{
				Logger.e(getClass().getSimpleName(), "Null Token", e);
				return Boolean.FALSE;
			}

			this.data = null;
			Editor editor = settings.edit();
			editor.putString(HikeMessengerApp.NAME_SETTING, userName);
			editor.putInt(HikeConstants.Extras.GENDER, isFemale ? 2 : 1);
			if (birthdate != null)
			{
				editor.putInt(HikeMessengerApp.BIRTHDAY_DAY, birthdate.day);
				editor.putInt(HikeMessengerApp.BIRTHDAY_MONTH, birthdate.month);
				editor.putInt(HikeMessengerApp.BIRTHDAY_YEAR, birthdate.year);
			}
			/*
			 * Setting these values as true for now. They will be reset on upgrades.
			 */
			editor.putBoolean(HikeMessengerApp.DEVICE_DETAILS_SENT, true);
			editor.putBoolean(HikeMessengerApp.UPGRADE_RAI_SENT, true);
			editor.commit();
		}
		else
		{
			userName = name;
		}

		/* set the name */
		publishProgress(new StateValue(State.NAME, userName));

		if (profilePicSmall != null)
		{
				byte[] bytes = BitmapUtils.bitmapToBytes(profilePicSmall, Bitmap.CompressFormat.JPEG, 100);
				ContactManager.getInstance().setIcon(msisdn, bytes, false);
		}
		
		publishProgress(new StateValue(State.PROFILE_IMAGE, FINISHED_UPLOAD_PROFILE));

		// Setting up preferences for new user.
		Editor edit = settings.edit();
		/*
		 * We show these tips only to upgrading users
		 */
		edit.putBoolean(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, true);
		/*
		 * Re-initilizing hidden mode
		 */
		StealthModeManager.getInstance().initiate();
		/*
		 * We don't want to show red dot on overflow menu for new users
		 */
		edit.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, true);
		edit.commit();
		
		if (!restored)
		{
			this.data = null;
			try
			{
		
				// If was already restoring backup, no need to check anything, make another attempt at it.
				if (settings.getBoolean(HikeMessengerApp.RESTORING_BACKUP, false))
				{
					this.data = "true";
				}
				else if (AccountBackupRestore.getInstance(context).isBackupAvailable())
				{
					publishProgress(new StateValue(State.BACKUP_AVAILABLE,null));
					// After publishing 'backup available' the task waits for the user to make an input(Restore or Skip)
					synchronized (this)
					{
						this.wait();
					}
				}
					
				
				/*
				 * The following while loop executes restore operation(if user has selected so)
				 * As soon as the loop starts it resets itself, so it wont execute again unless its again
				 * set by the user.
				 */
				while (!TextUtils.isEmpty(this.data))
				{
					this.data = null;
					
					@RestoreErrorStates int restoreStatus = restore(settings);
					
					// A delay so that user is able to understand the UI animations.
					synchronized (this)
					{
						this.wait(HikeConstants.BACKUP_RESTORE_UI_DELAY);
					}
					if (restoreStatus == STATE_RESTORE_SUCCESS)
					{
						BotUtils.postAccountRestoreSetup();
						publishProgress(new StateValue(State.RESTORING_BACKUP,Boolean.TRUE.toString()));

						if (BackupUtils.isDeviceDpiDifferent())
						{
							publishProgress(new StateValue(State.SHOW_STICKER_RESTORE_DIALOG, null)); //Give a signal to show the sticker restore dialog.
						}
					}

					else
					{
						BotUtils.initBots();
						switch (restoreStatus)
						{
						case STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION:
							publishProgress(new StateValue(State.RESTORING_BACKUP, context.getString(R.string.restore_version_error)));
							break;
						case STATE_RESTORE_FAILURE_MSISDN_MISMATCH:
							publishProgress(new StateValue(State.RESTORING_BACKUP, context.getString(R.string.restore_msisdn_error)));
							break;
						default:
							publishProgress(new StateValue(State.RESTORING_BACKUP, Boolean.FALSE.toString()));
							break;
						}
						// After publishing 'restore failed' the task waits for the user to again make an input(Restore or Skip)
						synchronized (this)
						{
							this.wait();
						}
					}
				}
				this.data = null;
				if (!isCancelled())
				{
					Editor editor = settings.edit();
					editor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
					editor.commit();
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				Logger.e("SignupTask","Interrupted while waiting for user's choice on restore.");
				return Boolean.FALSE;
			}
		}

		try
		{
			// Fetch user settings from server
			publishProgress(new StateValue(State.RESTORING_CLOUD_SETTINGS, null));
			synchronized (this)
			{
				this.wait();
			}
		}
		catch (InterruptedException iex)
		{
			iex.printStackTrace();
			Logger.e("SignupTask","Interrupted while waiting for returning user's setting restore");
		}

		Logger.d("SignupTask", "Publishing Token_Created");

		/* tell the service to start listening for new messages */
		isAlreadyFetchingNumber = false;

		settings.edit().putBoolean(StickerManager.STICKER_FOLDER_NAMES_UPGRADE_DONE, true).commit();

		return Boolean.TRUE;
	}

	@RestoreErrorStates
	private int restore(SharedPreferences settings)
	{
		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.RESTORING_BACKUP, true);
		editor.commit();
		
		publishProgress(new StateValue(State.RESTORING_BACKUP,null));
		@RestoreErrorStates  int restoreStatus = AccountBackupRestore.getInstance(context).restore();
		
		if (restoreStatus == STATE_RESTORE_SUCCESS)
		{
			/**
			 * This will shutdown the contact manager completely and then Contact Manager will be initialized with new hike user db values that restored during backup restore
			 * process.
			 */
			ContactManager.getInstance().refreshContactManager();
			editor.putBoolean(HikeMessengerApp.RESTORE_ACCOUNT_SETTING, true);
			editor.commit();
		}

		editor.putBoolean(HikeMessengerApp.RESTORING_BACKUP, false);
		editor.commit();
		
		return restoreStatus;
	}
	
	@Override
	protected void onCancelled()
	{
		if (signupTask != null)
		{
			signupTask.isRunning = false;
		}
		signupTask = null;
		Logger.d("SignupTask", "onCancelled called");
		unregisterReceiver();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{

		if (signupTask != null)
		{
			signupTask.isRunning = false;
		}
		signupTask = null;
		onSignupTaskProgressUpdate.onFinish(result.booleanValue());
	}

	@Override
	protected void onProgressUpdate(StateValue... values)
	{
		onSignupTaskProgressUpdate.onProgressUpdate(values[0]);
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.context = activity;
		this.onSignupTaskProgressUpdate = (OnSignupTaskProgressUpdate) activity;
	}

	@Override
	public boolean isFinished()
	{
		return false;
	}

	public void cancelTask()
	{
		this.cancel(true);
		Logger.d("SignupTask", "cancelling it manually");
		unregisterReceiver();
	}

	/*
	 * For removing intent when finishing the activity
	 */
	private void unregisterReceiver()
	{
		if (receiver != null)
		{
			try
			{
				this.context.unregisterReceiver(receiver);
			}
			catch (IllegalArgumentException e)
			{
				Logger.d("SignupTask", "IllegalArgumentException while unregistering receiver", e);
			}
			receiver = null;
		}
	}

	public static SignupTask startTask(Activity activity)
	{
		getSignupTask(activity);
		if (!signupTask.isRunning())
		{
			Utils.executeSignupTask(signupTask);
		}
		return signupTask;
	}

	public static SignupTask startTask(Activity activity, String userName, Boolean isFemale, Birthday birthday, Bitmap profilePicSmall)
	{
		getSignupTask(activity);
		if (!signupTask.isRunning())
		{
			signupTask.addGender(isFemale);
			signupTask.addUserName(userName);
			signupTask.addBirthdate(birthday);
			signupTask.addProfilePicPath(null, profilePicSmall);
			/*
			 * if we are on signupActivity we should not anymore try to
			 * auto auth
			 */
			SignupTask.isAlreadyFetchingNumber = true;
			Utils.executeSignupTask(signupTask);
		}
		return signupTask;
	}

	public static SignupTask restartTask(Activity activity)
	{
		if (signupTask != null && signupTask.isRunning())
		{
			signupTask.cancelTask();
		}
		startTask(activity);
		return signupTask;
	}
	
	public static SignupTask restartTask(Activity activity, String userName, Boolean isFemale, Birthday birthday, Bitmap profilePicSmall)
	{
		if (signupTask != null && signupTask.isRunning())
		{
			signupTask.cancelTask();
		}
		startTask(activity, userName, isFemale, birthday, profilePicSmall);
		return signupTask;
	}
	
    private void deletePreviouslySavedProfileImages()
    {
    	String dirPath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
    	File dir = new File(dirPath);
    	if (!dir.exists() || !dir.isDirectory())
    	{
    		return;
    	}
    	
    	File[] profileList = dir.listFiles();
    	if( (profileList == null) || (profileList.length==0))
    	{
    		return;
    	}
    	
    	for (File file : profileList)
    	{
    		file.delete();
    	}
    }

    protected final void publishProgress(StateValue value) 
	{

		if (value != null)
		{
			Logger.d("SignupTask", " publishing state : " + value.state + " val : " + value.value);
		}

		super.publishProgress(value);
	}
}
