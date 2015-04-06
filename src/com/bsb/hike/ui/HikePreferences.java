package com.bsb.hike.ui;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.ActivityCallableTask;
import com.bsb.hike.tasks.BackupAccountTask;
import com.bsb.hike.tasks.BackupAccountTask.BackupAccountListener;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.tasks.DeleteAccountTask.DeleteAccountListener;
import com.bsb.hike.tasks.RingtoneFetcherTask;
import com.bsb.hike.tasks.RingtoneFetcherTask.RingtoneFetchListener;
import com.bsb.hike.ui.utils.LockPattern;
import com.bsb.hike.utils.HikeAppStateBasePreferenceActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.IconCheckBoxPreference;
import com.bsb.hike.view.IconListPreference;
import com.bsb.hike.view.NotificationToneListPreference;

public class HikePreferences extends HikeAppStateBasePreferenceActivity implements OnPreferenceClickListener, 
							OnPreferenceChangeListener, DeleteAccountListener, BackupAccountListener, RingtoneFetchListener
{

	private enum BlockingTaskType
	{
		NONE, DELETING_ACCOUNT, UNLINKING_ACCOUNT, /*UNLINKING_TWITTER,*/ BACKUP_ACCOUNT, FETCH_RINGTONE
	}

	private ActivityCallableTask mTask;

	ProgressDialog mDialog;

	private boolean isDeleting;

	private BlockingTaskType blockingTaskType = BlockingTaskType.NONE;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return ((mTask != null) && (!mTask.isFinished())) ? mTask : null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikepreferences);

		Intent intent = getIntent();
		int preferences = intent.getIntExtra(HikeConstants.Extras.PREF, -1);
		int titleRes = intent.getIntExtra(HikeConstants.Extras.TITLE, 0);

		Logger.d(getClass().getSimpleName(), preferences + " + " + titleRes);
		addPreferencesFromResource(preferences);

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ActivityCallableTask)
		{
			if (savedInstanceState != null)
			{
				blockingTaskType = BlockingTaskType.values()[savedInstanceState.getInt(HikeConstants.Extras.BLOKING_TASK_TYPE)];
			}
			setBlockingTask((ActivityCallableTask) retained);
			mTask.setActivity(this);
		}

		Preference deletePreference = getPreferenceScreen().findPreference(HikeConstants.DELETE_PREF);
		if (deletePreference != null)
		{
			Utils.logEvent(HikePreferences.this, HikeConstants.LogEvent.PRIVACY_SCREEN);
			deletePreference.setOnPreferenceClickListener(this);
		}
		else
		{
			Utils.logEvent(HikePreferences.this, HikeConstants.LogEvent.NOTIFICATION_SCREEN);
		}
		Preference backupPreference = getPreferenceScreen().findPreference(HikeConstants.BACKUP_PREF);
		if (backupPreference != null)
		{
			backupPreference.setOnPreferenceClickListener(this);
		}
		Preference unlinkPreference = getPreferenceScreen().findPreference(HikeConstants.UNLINK_PREF);
		if (unlinkPreference != null)
		{
			unlinkPreference.setOnPreferenceClickListener(this);
		}
		
		/*Preference unlinkFacebookPreference = getPreferenceScreen().findPreference(HikeConstants.UNLINK_FB);
		if (unlinkFacebookPreference != null)
		{
			Session session = Session.getActiveSession();
			if (session != null)
			{
				unlinkFacebookPreference.setOnPreferenceClickListener(this);
			}
			else
			{
				getPreferenceScreen().removePreference(unlinkFacebookPreference);
			}
		}

		Preference unlinkTwitterPreference = getPreferenceScreen().findPreference(HikeConstants.UNLINK_TWITTER);
		if (unlinkTwitterPreference != null)
		{
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false))
			{
				unlinkTwitterPreference.setOnPreferenceClickListener(this);
			}
			else
			{
				getPreferenceScreen().removePreference(unlinkTwitterPreference);
			}
		}*/

		final IconCheckBoxPreference profilePicPreference = (IconCheckBoxPreference) getPreferenceScreen().findPreference(HikeConstants.PROFILE_PIC_PREF);
		if (profilePicPreference != null)
		{
			profilePicPreference.setOnPreferenceChangeListener(this);
		}
		final IconCheckBoxPreference sendEnterPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.SEND_ENTER_PREF);
		if (sendEnterPreference != null) {
			sendEnterPreference.setOnPreferenceChangeListener(this);
		}
		final IconCheckBoxPreference doubleTapPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.DOUBLE_TAP_PREF);
		if (doubleTapPreference != null) {
			doubleTapPreference.setOnPreferenceChangeListener(this);
		}
		final IconCheckBoxPreference freeSmsPreference = (IconCheckBoxPreference) getPreferenceScreen().findPreference(HikeConstants.FREE_SMS_PREF);
		if (freeSmsPreference != null)
		{
			freeSmsPreference.setOnPreferenceChangeListener(this);
		}

		final IconCheckBoxPreference sslPreference = (IconCheckBoxPreference) getPreferenceScreen().findPreference(HikeConstants.SSL_PREF);
		if (sslPreference != null)
		{
			String countryCode = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.COUNTRY_CODE, "");
			if(countryCode.equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE))
			{
				getPreferenceScreen().removePreference(sslPreference);
			}
			else
			{
				sslPreference.setOnPreferenceChangeListener(this);
			}
		}

		Preference blockedListPreference = getPreferenceScreen().findPreference(HikeConstants.BLOKED_LIST_PREF);
		if (blockedListPreference != null)
		{
			Logger.d(getClass().getSimpleName(), "blockedListPreference preference not null" + blockedListPreference.getKey());
			blockedListPreference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "blockedListPreference preference is null");
		}

		Preference systemHealthPreference = getPreferenceScreen().findPreference(HikeConstants.SYSTEM_HEALTH_PREF);
		if (systemHealthPreference != null)
		{
			Logger.d(getClass().getSimpleName(), "systemHealthPreference preference is not null");
			systemHealthPreference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "systemHealthPreference preference is null");
		}

		Preference helpFaqsPreference = getPreferenceScreen().findPreference(HikeConstants.HELP_FAQS_PREF);
		if (helpFaqsPreference != null)
		{
			Logger.d(getClass().getSimpleName(), "helpFaqsPreference preference is not null" + helpFaqsPreference.getKey());
			helpFaqsPreference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "helpFaqsPreference preference is null");
		}

		Preference helpContactPreference = getPreferenceScreen().findPreference(HikeConstants.HELP_FEEDBACK_PREF);
		if (helpContactPreference != null)
		{
			Logger.d(getClass().getSimpleName(), "helpContactPreference preference is not null");
			helpContactPreference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "helpContactPreference preference is null");
		}

		Preference termsConditionsPref = getPreferenceScreen().findPreference(HikeConstants.HELP_TNC_PREF);
				
		if (termsConditionsPref != null)
		{
			Logger.d(getClass().getSimpleName(), "termsConditionsPref is not null");
			termsConditionsPref.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "termsConditionsPref is null");
		}
		
		Preference mutePreference = getPreferenceScreen().findPreference(HikeConstants.STATUS_BOOLEAN_PREF);
		if (mutePreference != null)
		{
			mutePreference.setOnPreferenceClickListener(this);
		}

		Preference h2oNotifPreference = getPreferenceScreen().findPreference(HikeConstants.H2O_NOTIF_BOOLEAN_PREF);
		if (h2oNotifPreference != null)
		{
			h2oNotifPreference.setOnPreferenceChangeListener(this);
		}
		
		Preference nujNotifPreference = getPreferenceScreen().findPreference(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF);
		if (nujNotifPreference != null)
		{
			nujNotifPreference.setOnPreferenceChangeListener(this);
 		}
		
		Preference muteChatBgPreference = getPreferenceScreen().findPreference(HikeConstants.CHAT_BG_NOTIFICATION_PREF);
		if (muteChatBgPreference != null)
		{
			muteChatBgPreference.setOnPreferenceClickListener(this);
		}

		Preference stealthPreference = getPreferenceScreen().findPreference(HikeConstants.STEALTH_PERF_SETTING);
		if(stealthPreference != null)
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
			{
				Preference resetStealthPreference = getPreferenceScreen().findPreference(HikeConstants.RESET_STEALTH_PREF);
				if (resetStealthPreference != null)
				{
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
					{
						resetStealthPreference.setTitle(R.string.resetting_complete_stealth_header);
						resetStealthPreference.setSummary(R.string.resetting_complete_stealth_info);
					}
					resetStealthPreference.setOnPreferenceClickListener(this);
				}
				Preference resetStealthPassword = getPreferenceScreen().findPreference(HikeConstants.CHANGE_STEALTH_PASSCODE);
				if (resetStealthPassword != null)
				{
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
					{
						resetStealthPassword.setTitle(R.string.change_stealth_password);
						resetStealthPassword.setSummary(R.string.change_stealth_password_body);
					}
					resetStealthPassword.setOnPreferenceClickListener(this);
				}
			}else
			{
				getPreferenceScreen().removePreference(stealthPreference);
			}
		}
		Preference notificationRingtonePreference = getPreferenceScreen().findPreference(HikeConstants.NOTIF_SOUND_PREF);
		if (notificationRingtonePreference != null)
		{
			notificationRingtonePreference.setOnPreferenceClickListener(this);
		}
		Preference videoCompressPreference = getPreferenceScreen().findPreference(HikeConstants.COMPRESS_VIDEO_CATEGORY);
		if(videoCompressPreference != null && android.os.Build.VERSION.SDK_INT < 18)
		{
			getPreferenceScreen().removePreference(videoCompressPreference);
		}

		Preference favoriteListPreference = getPreferenceScreen().findPreference(HikeConstants.FAV_LIST_PREF);
		if (favoriteListPreference != null)
		{
			favoriteListPreference.setOnPreferenceClickListener(this);
		}
		setupActionBar(titleRes);

	}

	private void setupActionBar(int titleRes)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleRes);
		backContainer.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(HikeConstants.Extras.BLOKING_TASK_TYPE, blockingTaskType.ordinal());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mTask = null;
	}

	public void setBlockingTask(ActivityCallableTask task)
	{
		Logger.d("HikePreferences", "setting task:" + task.isFinished());
		if (!task.isFinished())
		{
			mTask = task;
			String title = getString(R.string.account);
			String message = "";
			switch (blockingTaskType)
			{
			case DELETING_ACCOUNT:
				message = getString(R.string.deleting_account);
				break;
			case UNLINKING_ACCOUNT:
				message = getString(R.string.unlinking_account);
				break;
			/*case UNLINKING_TWITTER:
				message = getString(R.string.social_unlinking);
				break;*/
			case BACKUP_ACCOUNT:
				title = getString(R.string.account_backup);
				message = getString(R.string.creating_backup_message);
				break;
			case FETCH_RINGTONE:
				mDialog = new ProgressDialog(this);
				mDialog.setMessage(getResources().getString(R.string.ringtone_loader));
				mDialog.show();
				return;
			default:
				return;
			}
			mDialog = ProgressDialog.show(this, title, message);
		}
	}

	public void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	public boolean onPreferenceClick(final Preference preference)
	{
		Logger.d("HikePreferences", "Preference clicked: " + preference.getKey());
		if (preference.getKey().equals(HikeConstants.DELETE_PREF))
		{
			 Intent i = new Intent(getApplicationContext(), DeleteAccount.class);
			 startActivity(i);
		}
		else if (preference.getKey().equals(HikeConstants.BACKUP_PREF))
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BACKUP);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			BackupAccountTask task = new BackupAccountTask(getApplicationContext(), HikePreferences.this);
			blockingTaskType = BlockingTaskType.BACKUP_ACCOUNT;
			setBlockingTask(task);
			Utils.executeBoolResultAsyncTask(task);
		}
		else if (preference.getKey().equals(HikeConstants.UNLINK_PREF))
		{
			HikeDialogFactory.showDialog(HikePreferences.this, HikeDialogFactory.UNLINK_ACCOUNT_CONFIRMATION_DIALOG, new HikeDialogListener()
			{
				
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					DeleteAccountTask task = new DeleteAccountTask(HikePreferences.this, false, getApplicationContext());
					blockingTaskType = BlockingTaskType.UNLINKING_ACCOUNT;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
					hikeDialog.dismiss();
				}
				
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
				}
				
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
			});
		}
		/*else if (preference.getKey().equals(HikeConstants.UNLINK_FB))
		{
			HikeDialogFactory.showDialog(HikePreferences.this, HikeDialogFactory.UNLINK_FB_DIALOG, new HikeDialogListener()
			{
				
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
					editor.putBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false);
					editor.commit();
					Session session = Session.getActiveSession();
					if (session != null)
					{
						session.closeAndClearTokenInformation();
						Session.setActiveSession(null);
					}
					Toast.makeText(getApplicationContext(), R.string.social_unlink_success, Toast.LENGTH_SHORT).show();
					getPreferenceScreen().removePreference(getPreferenceScreen().findPreference(HikeConstants.UNLINK_FB));
					
					hikeDialog.dismiss();
				}
				
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
					
				}
				
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					
				}
			}, null);
		}
		
		else if (preference.getKey().equals(HikeConstants.UNLINK_TWITTER))
		{
			
			HikeDialogFactory.showDialog(HikePreferences.this, HikeDialogFactory.UNLINK_TWITTER_DIALOG, new HikeDialogListener()
			{
				
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					UnlinkTwitterTask task = new UnlinkTwitterTask(HikePreferences.this, getApplicationContext());
					blockingTaskType = BlockingTaskType.UNLINKING_TWITTER;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
					hikeDialog.dismiss();
				}
				
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
					
				}
				
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					
				}
			}, null);
			
		}
			};

			confirmDialog.setOkButton(R.string.unlink, dialogOkClickListener);
			confirmDialog.setCancelButton(R.string.cancel);
			confirmDialog.show();

		}*/
		else if (HikeConstants.BLOKED_LIST_PREF.equals(preference.getKey()))
		{
			Intent intent = new Intent(HikePreferences.this, HikeListActivity.class);
			intent.putExtra(HikeConstants.Extras.BLOCKED_LIST, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
		else if (HikeConstants.SYSTEM_HEALTH_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "system health preference selected");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(HikeConstants.SYSTEM_HEALTH_URL));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			try
			{
				startActivity(intent);
			}
			catch (ActivityNotFoundException e)
			{
				Toast.makeText(getApplicationContext(), R.string.system_health_error, Toast.LENGTH_SHORT).show();
			}
		}
		else if (HikeConstants.HELP_FAQS_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "FAQ preference selected");
			Utils.startWebViewActivity(HikePreferences.this,HikeConstants.HELP_URL,getString(R.string.faq));
		}
		else if (HikeConstants.HELP_TNC_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "T & C preference selected");
			Utils.startWebViewActivity(HikePreferences.this, HikeConstants.T_AND_C_URL, getString(R.string.terms_conditions_title));
		}
		else if (HikeConstants.HELP_FEEDBACK_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "contact preference selected");
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:" + HikeConstants.MAIL));

			StringBuilder message = new StringBuilder("\n\n");

			try
			{
				message.append(getString(R.string.hike_version) + " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + "\n");
			}
			catch (NameNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			message.append(getString(R.string.device_name) + " " + Build.MANUFACTURER + " " + Build.MODEL + "\n");

			message.append(getString(R.string.android_version) + " " + Build.VERSION.RELEASE + "\n");

			String msisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, "");
			message.append(getString(R.string.msisdn) + " " + msisdn);

			intent.putExtra(Intent.EXTRA_TEXT, message.toString());
			intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_on_hike));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			try
			{
				startActivity(intent);
			}
			catch (ActivityNotFoundException e)
			{
				Toast.makeText(getApplicationContext(), R.string.email_error, Toast.LENGTH_SHORT).show();
			}
		}
		else if (HikeConstants.STATUS_BOOLEAN_PREF.equals(preference.getKey()))
		{
			SharedPreferences settingPref = PreferenceManager.getDefaultSharedPreferences(this);
			int statusIntPreference = settingPref.getInt(HikeConstants.STATUS_PREF, 0);

			int newValue;

			Editor editor = settingPref.edit();
			if (statusIntPreference == 0)
			{
				newValue = -1;
				editor.putInt(HikeConstants.STATUS_PREF, newValue);
			}
			else
			{
				newValue = 0;
				editor.putInt(HikeConstants.STATUS_PREF, newValue);
			}
			editor.commit();

			try
			{
				JSONObject jsonObject = new JSONObject();
				JSONObject data = new JSONObject();
				data.put(HikeConstants.PUSH_SU, newValue);
				data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
				jsonObject.put(HikeConstants.DATA, data);
				jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
				HikeMqttManagerNew.getInstance().sendMessage(jsonObject, HikeMqttManagerNew.MQTT_QOS_ONE);
			}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), e);
			}
		}
		else if (HikeConstants.CHAT_BG_NOTIFICATION_PREF.equals(preference.getKey()))
		{
			/*
			 * Send to server
			 */
			SharedPreferences settingPref = PreferenceManager.getDefaultSharedPreferences(this);
			try
			{
				JSONObject jsonObject = new JSONObject();
				JSONObject data = new JSONObject();
				data.put(HikeConstants.CHAT_BACKGROUD_NOTIFICATION, settingPref.getBoolean(HikeConstants.CHAT_BG_NOTIFICATION_PREF, true) ? 0 : -1);
				data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
				jsonObject.put(HikeConstants.DATA, data);
				jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
				HikeMqttManagerNew.getInstance().sendMessage(jsonObject, HikeMqttManagerNew.MQTT_QOS_ONE);
			}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), e);
			}
		}
		else if (HikeConstants.RESET_STEALTH_PREF.equals(preference.getKey()))
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
			{
				Utils.cancelScheduledStealthReset();

				preference.setTitle(R.string.reset_complete_stealth_header);
				preference.setSummary(R.string.reset_complete_stealth_info);

				HikeMessengerApp.getPubSub().publish(HikePubSub.RESET_STEALTH_CANCELLED, null);

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
			else
			{
				Object[] dialogStrings = new Object[4];
				dialogStrings[0] = getString(R.string.initiate_reset_stealth_header);
				dialogStrings[1] = getString(R.string.initiate_reset_stealth_body);
				dialogStrings[2] = getString(R.string.confirm);
				dialogStrings[3] = getString(R.string.cancel);
				
				HikeDialogFactory.showDialog(this, HikeDialogFactory.RESET_STEALTH_DIALOG, new HikeDialogListener()
				{

					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, System.currentTimeMillis());

						HikeMessengerApp.getPubSub().publish(HikePubSub.RESET_STEALTH_INITIATED, null);

						preference.setTitle(R.string.resetting_complete_stealth_header);
						preference.setSummary(R.string.resetting_complete_stealth_info);

						Intent intent = new Intent(HikePreferences.this, HomeActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);

						hikeDialog.dismiss();
						
						try
						{
							JSONObject metadata = new JSONObject();
							metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_INIT);
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
						}
						catch(JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}
					}

					@Override
					public void neutralClicked(HikeDialog hikeDialog)
					{

					}

					@Override
					public void negativeClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
					}

				}, dialogStrings);
			}

		}
		else if(HikeConstants.CHANGE_STEALTH_PASSCODE.equals(preference.getKey()))
		{
			LockPattern.confirmPattern(HikePreferences.this, true);
		}
		else if(HikeConstants.NOTIF_SOUND_PREF.equals(preference.getKey()))
		{
			Preference notificationPreference = getPreferenceScreen().findPreference(HikeConstants.NOTIF_SOUND_PREF);
			if(notificationPreference != null)
			{
				NotificationToneListPreference notifToneListPref = (NotificationToneListPreference) notificationPreference;
				if(notifToneListPref.isEmpty())
				{
					RingtoneFetcherTask task = new RingtoneFetcherTask(HikePreferences.this, false, getApplicationContext());
					blockingTaskType = BlockingTaskType.FETCH_RINGTONE;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
				}
			}
		}
		else if (HikeConstants.FAV_LIST_PREF.equals(preference.getKey()))
		{
			HAManager.logClickEvent(HikeConstants.LogEvent.MANAGE_FAV_LIST_SETTING);
			Intent intent = new Intent(HikePreferences.this, PeopleActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
		return true;
	}

	/**
	 * For redirecting back to the Welcome Screen.
	 */
	public void accountDeleted()
	{
		dismissProgressDialog();
		/*
		 * First we send the user to the Main Activity(MessagesList) from there we redirect him to the welcome screen.
		 */
		Intent dltIntent = new Intent(this, HomeActivity.class);
		dltIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(dltIntent);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		boolean isChecked = (Boolean) newValue;
		((IconCheckBoxPreference) preference).setChecked(isChecked);

		if (HikeConstants.RECEIVE_SMS_PREF.equals(preference.getKey()))
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.UNIFIED_INBOX, String.valueOf(isChecked));
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			if (!isChecked)
			{
				Editor editor = PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, false);
				editor.commit();
			}
			else
			{
				if (!HikePreferences.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
				}
			}
		}
		else if (HikeConstants.PROFILE_PIC_PREF.equals(preference.getKey()))
		{
			JSONObject object = new JSONObject();
			try
			{
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

				int avatarSetting =1;
				if(isChecked){
					avatarSetting = 2;
				}
				JSONObject data = new JSONObject();
				data.put(HikeConstants.AVATAR, avatarSetting);
				object.put(HikeConstants.DATA, data);

				HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
	     	}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), "Invalid json", e);
			}
		}
		else if (HikeConstants.FREE_SMS_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "Free SMS toggled");
			HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, isChecked);

			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.FREE_SMS_ON, String.valueOf(isChecked));
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		} else if (HikeConstants.SEND_ENTER_PREF.equals(preference.getKey())) {

			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					HikePreferences.this).edit();
			editor.putBoolean(HikeConstants.SEND_ENTER_PREF, isChecked);
			editor.commit();
			JSONObject metadata = new JSONObject();
			if (isChecked) {
				preference.setSummary(getResources().getString(
						R.string.enter_setting_info));
				try {
					metadata.put(HikeConstants.EVENT_KEY,
							HikeConstants.LogEvent.SETTINGS_ENTER_ON);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				preference.setSummary(getResources().getString(
						R.string.new_line_setting_info));
				try {
					metadata.put(HikeConstants.EVENT_KEY,
							HikeConstants.LogEvent.SETTINGS_ENTER_OFF);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT,
					AnalyticsConstants.CLICK_EVENT, metadata);
		} else if (HikeConstants.DOUBLE_TAP_PREF.equals(preference.getKey())) {

			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					HikePreferences.this).edit();
			editor.putBoolean(HikeConstants.DOUBLE_TAP_PREF, isChecked);
			editor.commit();
			try {
				JSONObject metadata = new JSONObject();
				if (isChecked) {
					metadata.put(HikeConstants.EVENT_KEY,
							HikeConstants.LogEvent.SETTINGS_NUDGE_ON);
				} else {
					metadata.put(HikeConstants.EVENT_KEY,
							HikeConstants.LogEvent.SETTINGS_NUDGE_OFF);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT,
						AnalyticsConstants.CLICK_EVENT, metadata);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (HikeConstants.SSL_PREF.equals(preference.getKey()))
		{
			Utils.setupUri(this.getApplicationContext());
			HttpRequestConstants.toggleSSL();
			LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(HikePubSub.SSL_PREFERENCE_CHANGED));
		}
		else if (HikeConstants.STATUS_BOOLEAN_PREF.equals(preference.getKey()))
		{
			//Handled in OnPreferenceClick
		}
		else if (HikeConstants.NUJ_NOTIF_BOOLEAN_PREF.equals(preference.getKey()))
		{			
			try
			{
				JSONObject metadata = new JSONObject();
				
				if(isChecked)
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_NUJ_ON);
				}
				else{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_NUJ_OFF);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}			
			JSONObject object = new JSONObject();
			
			try
			{
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

				JSONObject data = new JSONObject();
				data.put(HikeConstants.UJ_NOTIF_SETTING, isChecked? 1:0 );
				data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
				object.put(HikeConstants.DATA, data);

				HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
			}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), "Invalid json", e);
			}
		}
		else if (HikeConstants.H2O_NOTIF_BOOLEAN_PREF.equals(preference.getKey()))
		{
			try
			{
				JSONObject metadata = new JSONObject();
				
				if(isChecked)
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_H2O_ON);
				}
				else{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_H2O_OFF);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
		return false;
	}

	@Override
	@Deprecated
	public void addPreferencesFromResource(int preferencesResId)
	{
		// TODO Auto-generated method stub
		super.addPreferencesFromResource(preferencesResId);
		switch (preferencesResId)
		{
		case R.xml.notification_preferences:
			updateNotifPrefView();
			break;
		case R.xml.account_preferences:
			updateAccountBackupPrefView();
			break;
		case R.xml.privacy_preferences:
			updatePrivacyPrefView();
			break;
		}
	}

	private void updatePrivacyPrefView()
	{
		IconListPreference lp = (IconListPreference) getPreferenceScreen().findPreference(HikeConstants.LAST_SEEN_PREF_LIST);
		lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				try
				{
					int slectedPrivacyId = Integer.parseInt(newValue.toString());
					String selectedPrivacyValue = "";
					boolean isLSEnabled = true;
					String ls_summary = null;
					switch (HikeConstants.PrivacyOptions.values()[slectedPrivacyId]) {
						case NOBODY:
							isLSEnabled = false;
							selectedPrivacyValue = getApplicationContext().getString(R.string.privacy_nobody_key);
							ls_summary = getApplicationContext().getString(R.string.ls_nobody_summary);
							HAManager.logClickEvent(HikeConstants.LogEvent.LS_NOBODY_CLICKED);
							break;
						case EVERYONE:
							selectedPrivacyValue = getApplicationContext().getString(R.string.privacy_everyone_key);
							ls_summary = getApplicationContext().getString(R.string.ls_everyone_summary);
							HAManager.logClickEvent(HikeConstants.LogEvent.LS_EVERYONE_CLICKED);
							break;
						case FAVORITES:
							selectedPrivacyValue = getApplicationContext().getString(R.string.privacy_favorites_key);
							ls_summary = getApplicationContext().getString(R.string.ls_favorites_summary);
							HAManager.logClickEvent(HikeConstants.LogEvent.LS_FAVOURITES_CLICKED);
							break;
						case MY_CONTACTS:
							selectedPrivacyValue = getApplicationContext().getString(R.string.privacy_my_contacts_key);
							ls_summary = getApplicationContext().getString(R.string.ls_my_contacts_summary);
							HAManager.logClickEvent(HikeConstants.LogEvent.LS_MY_CONTACTS_CLICKED);
							break;
					}
					preference.setTitle(getString(R.string.last_seen_header) + " : " + selectedPrivacyValue);
					preference.setSummary(ls_summary);
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(HikeConstants.LAST_SEEN_PREF, isLSEnabled).commit();
					sendNLSToServer(slectedPrivacyId, isLSEnabled);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return true;
			}
		});

		lp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(HikeConstants.HIGHLIGHT_NLS_PERF, true))
				{
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(HikeConstants.HIGHLIGHT_NLS_PERF, false).commit();
					if(preference instanceof IconListPreference)
						((IconListPreference)preference).setTitleColor(R.color.list_item_header);
				}
				HAManager.logClickEvent(HikeConstants.LogEvent.LS_SETTING_CLICKED);
				return false;
			}
		});
		String ls_summary = getLSSummaryText();
		if(ls_summary != null && !ls_summary.isEmpty())
		{
			lp.setSummary(ls_summary);
		}
		if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(HikeConstants.HIGHLIGHT_NLS_PERF, true))
			lp.setTitleColor(R.color.unread_message_blue);
		lp.setTitle(lp.getTitle() + " : " + lp.getEntry());
	}

	private String getLSSummaryText()
	{
		String defValue = getApplicationContext().getString(R.string.privacy_my_contacts);
		String selectedValue = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(HikeConstants.LAST_SEEN_PREF_LIST, defValue);
		String summaryTxt = null;
		switch (HikeConstants.PrivacyOptions.values()[Integer.parseInt(selectedValue)]) {
			case NOBODY:
				summaryTxt = getApplicationContext().getString(R.string.ls_nobody_summary);
				break;
			case EVERYONE:
				summaryTxt = getApplicationContext().getString(R.string.ls_everyone_summary);
				break;
			case FAVORITES:
				summaryTxt = getApplicationContext().getString(R.string.ls_favorites_summary);
				break;
			case MY_CONTACTS:
				summaryTxt = getApplicationContext().getString(R.string.ls_my_contacts_summary);
				break;
		}
		return summaryTxt;
	}

	private void updateAccountBackupPrefView()
	{
		Preference preference = getPreferenceScreen().findPreference(HikeConstants.BACKUP_PREF);
		long lastBackupTime = DBBackupRestore.getInstance(getApplicationContext()).getLastBackupTime();
		if (lastBackupTime > 0)
		{
			String lastBackup = getResources().getString(R.string.last_backup);
			String time = Utils.getFormattedDateTimeWOSecondsFromTimestamp(lastBackupTime/1000, getResources().getConfiguration().locale);
			preference.setSummary(lastBackup + ": " + time);
		}
		else
		{
			String backupMissing = getResources().getString(R.string.backup_missing);
			preference.setSummary(backupMissing);
		}
	}
	
	private void updateNotifPrefView()
	{
		ListPreference lp = (ListPreference) getPreferenceScreen().findPreference(HikeConstants.VIBRATE_PREF_LIST);
		lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				preference.setTitle(getString(R.string.vibrate) + " - " + (newValue.toString()));
				try
				{
					Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
					if (vibrator != null)
					{
						if (getString(R.string.vib_long).equals(newValue.toString()))
						{
							// play long
							vibrator.vibrate(HikeConstants.LONG_VIB_PATTERN, -1);
						}
						else if (getString(R.string.vib_short).equals(newValue.toString()))
						{
							// play short
							vibrator.vibrate(HikeConstants.SHORT_VIB_PATTERN, -1);
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return true;
			}
		});
		lp.setTitle(lp.getTitle() + " - " + lp.getValue());
		
		ListPreference ledPref = (ListPreference) getPreferenceScreen().findPreference(HikeConstants.COLOR_LED_PREF);
		ledPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				// Color.parseColor throws an IllegalArgumentException exception 
				// If the string cannot be parsed
				try
				{
					preference.setTitle(getString(R.string.led_notification) + " - " + (newValue.toString()));
					if("None".equals(newValue.toString()))
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_NONE_COLOR);
					}
					else
					{
						int finalColor = Color.parseColor(newValue.toString().toLowerCase());
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, finalColor);
					}
					return true;
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
					return false;
				}
			}
		});
		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(preferenceManager.contains(HikeConstants.LED_PREF))
		{
			boolean led = preferenceManager.getBoolean(HikeConstants.LED_PREF, true);
			if (!led)
			{
				ledPref.setDefaultValue(HikeConstants.LED_NONE_COLOR);
				ledPref.setValueIndex(0);
			}
			
			//removing previous Key
			preferenceManager.edit().remove(HikeConstants.LED_PREF).commit();
		}
		ledPref.setTitle(ledPref.getTitle() + " - " + ledPref.getValue());
	}

	@Override
	public void accountDeleted(boolean isSuccess)
	{
		if (isSuccess)
		{
			accountDeleted();
		}
		else
		{
			dismissProgressDialog();
		}

	}
	/**
	 * Adding this to handle the onactivityresult callback for reset password 
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//passing true here to denote that this is coming from the password reset operation
		data.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, true);
		LockPattern.onLockActivityResult(this, requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void accountBacked(boolean isSuccess)
	{
		 dismissProgressDialog();
		 updateAccountBackupPrefView();
	}

	@Override
	public void onRingtoneFetched(boolean isSuccess, Map<String, Uri> ringtonesNameURIMap)
	{
		// TODO Auto-generated method stub
		mTask = null;
		dismissProgressDialog();
		Preference notificationPreference = getPreferenceScreen().findPreference(HikeConstants.NOTIF_SOUND_PREF);
		if(notificationPreference != null)
		{
			NotificationToneListPreference notifToneListPref = (NotificationToneListPreference) notificationPreference;
			notifToneListPref.createAndShowDialog(ringtonesNameURIMap);
		}
	}

	public static void sendNLSToServer(int slectedPrivacyId, boolean isLSEnabled) throws JSONException
	{
		JSONObject object = new JSONObject();
		object.put(HikeConstants.TYPE,
				HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

		JSONObject data = new JSONObject();
		data.put(HikeConstants.NEW_LAST_SEEN_SETTING, slectedPrivacyId);
		data.put(HikeConstants.LAST_SEEN_SETTING, isLSEnabled);
		data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
		object.put(HikeConstants.DATA, data);

		HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
	}
}
