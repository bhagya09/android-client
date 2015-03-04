package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.ui.HikeDialog.HikeDialogListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	private enum DialogShowing
	{
		SMS_SYNC_CONFIRMATION_DIALOG, SMS_SYNCING_DIALOG
	}

	private SharedPreferences settings;

	private TextView creditsCurrent;

	private String[] pubSubListeners = { HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.INVITEE_NUM_CHANGED, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE,
			HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START };

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private View freeSmsContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		initalizeViews(savedInstanceState);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (savedInstanceState != null)
		{
			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils.showSMSSyncDialog(this, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (smsDialog != null && smsDialog.isShowing())
		{
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
		super.onSaveInstanceState(outState);
	}

	private void initalizeViews(Bundle savedInstanceState)
	{
		setContentView(R.layout.credits);

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		creditsCurrent = (TextView) findViewById(R.id.credits_num);

		updateCredits();
		setupActionBar();

		freeSmsContainer = findViewById(R.id.free_sms_details);
		freeSmsContainer.setVisibility(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.FREE_SMS_PREF, true) ? View.VISIBLE : View.GONE);

		View receiveSmsParent = findViewById(R.id.receive_sms_item);
		View freeSMSParent = findViewById(R.id.free_sms_item);
		View undeliveredSmsParent = findViewById(R.id.undelivered_sms_item);

		setupPreferenceLayout(freeSMSParent, false);
		if(!Utils.isKitkatOrHigher())
		{
			setupPreferenceLayout(receiveSmsParent, true);
			setupUndeliveredSmsPrefLayout();
		}
		else
		{
			undeliveredSmsParent.setVisibility(View.GONE);
			receiveSmsParent.setVisibility(View.GONE);
		}

		receiveSmsParent.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				boolean isChecked = togglePreference(HikeConstants.RECEIVE_SMS_PREF, (CheckBox) v.findViewById(R.id.receive_sms_checkbox));

				Utils.sendDefaultSMSClientLogEvent(isChecked);

				if (isChecked)
				{
					if (!CreditsActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
					}
				}
			}
		});

		freeSMSParent.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				boolean isChecked = togglePreference(HikeConstants.FREE_SMS_PREF, (CheckBox) v.findViewById(R.id.free_sms_checkbox));

				freeSmsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

				HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, isChecked);

				Utils.sendFreeSmsLogEvent(isChecked);
			}
		});
		
		undeliveredSmsParent.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				showSMSDialog();
			}
		});
	}

	private boolean togglePreference(String key, CheckBox checkBox)
	{
		checkBox.toggle();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Editor editor = preferences.edit();
		editor.putBoolean(key, checkBox.isChecked());
		editor.commit();

		return checkBox.isChecked();
	}

	private void setupPreferenceLayout(View parent, boolean receiveSmsPref)
	{
		TextView title = (TextView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_title : R.id.free_sms_title);
		TextView summary = (TextView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_summary : R.id.free_sms_summary);
		ImageView icon = (ImageView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_icon : R.id.free_sms_icon);
		CheckBox checkBox = (CheckBox) parent.findViewById(receiveSmsPref ? R.id.receive_sms_checkbox : R.id.free_sms_checkbox);

		title.setText(receiveSmsPref ? R.string.default_client_header : R.string.free_hike_to_sms);
		summary.setText(receiveSmsPref ? R.string.default_client_info : R.string.free_sms_msg);
		icon.setImageResource(receiveSmsPref ? R.drawable.preference_default_client : R.drawable.preference_free_sms);
		checkBox.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(receiveSmsPref ? HikeConstants.RECEIVE_SMS_PREF : HikeConstants.FREE_SMS_PREF,
				receiveSmsPref ? false : true));
	}
	
	private void setupUndeliveredSmsPrefLayout()
	{
		View parent = findViewById(R.id.undelivered_sms_item);
		TextView title = (TextView) parent.findViewById(R.id.undelivered_sms_title);
		TextView summary = (TextView) parent.findViewById(R.id.undelivered_sms_summary);
		ImageView icon = (ImageView) parent.findViewById(R.id.undelivered_sms_icon);

		String titleString = getString(R.string.hike_offline);
		String summaryString = getString(R.string.undelivered_sms_setting_summary);
		if (PreferenceManager.getDefaultSharedPreferences(CreditsActivity.this).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false))
		{
			if (PreferenceManager.getDefaultSharedPreferences(CreditsActivity.this).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
			{ 
				titleString += " - " + getString(R.string.regular_sms);
			}
			else
			{
				titleString += " - " + getString(R.string.free_hike_sms);
			}
			summaryString = getString(R.string.undelivered_sms_setting_remember);
		}
		title.setText(titleString);
		summary.setText(summaryString);
		icon.setImageResource(R.drawable.ic_bolt_blue);
	}

	public void onInviteClick(View v)
	{
		Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.INVITE_SMS_SCREEN_FROM_CREDIT);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

		Intent intent = new Intent(CreditsActivity.this, HikeListActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CREDITS_SCREEN, true);
		startActivity(intent);
	}

	public void onStartHikingClick(View v)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.START_HIKING);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}

		Intent intent = new Intent(this, ComposeChatActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);

		if (smsDialog != null)
		{
			smsDialog.cancel();
			smsDialog = null;
		}

		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		/*
		 * Here we check if we are already showing the twitter webview. If we are, we dont do any other UI changes.
		 */
		if ((HikePubSub.SMS_CREDIT_CHANGED.equals(type) || HikePubSub.INVITEE_NUM_CHANGED.equals(type)))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateCredits();
				}
			});
		}
		else if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					smsDialog = Utils.showSMSSyncDialog(CreditsActivity.this, true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type) || HikePubSub.SMS_SYNC_FAIL.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (smsDialog != null)
					{
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_START.equals(type))
		{
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		}

	}

	private void updateCredits()
	{
		int currentCredits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);

		creditsCurrent.setText(Integer.toString(currentCredits));
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.free_sms_txt);
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
	
	private void showSMSDialog()
	{
		final Dialog dialog = new Dialog(CreditsActivity.this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_undelivered_popup);
		dialog.setCancelable(true);

		TextView popupHeader = (TextView) dialog.findViewById(R.id.popup_header);
		View hikeSMS = dialog.findViewById(R.id.hike_sms_container);
		View nativeSMS = dialog.findViewById(R.id.native_sms_container);
		TextView nativeHeader = (TextView) dialog.findViewById(R.id.native_sms_header);
		TextView nativeSubtext = (TextView) dialog.findViewById(R.id.native_sms_subtext);
		TextView hikeSmsHeader = (TextView) dialog.findViewById(R.id.hike_sms_header);
		TextView hikeSmsSubtext = (TextView) dialog.findViewById(R.id.hike_sms_subtext);
		
		popupHeader.setText(getString(R.string.choose_setting));
		hikeSmsSubtext.setText(getString(R.string.free_hike_sms_subtext, settings.getInt(HikeMessengerApp.SMS_SETTING, 0)));
		
		final CheckBox sendHike = (CheckBox) dialog.findViewById(R.id.hike_sms_checkbox);

		final CheckBox sendNative = (CheckBox) dialog.findViewById(R.id.native_sms_checkbox);

		final Button alwaysBtn = (Button) dialog.findViewById(R.id.btn_always);
		final Button justOnceBtn = (Button) dialog.findViewById(R.id.btn_just_once);
		
		boolean sendNativeAlwaysPref = PreferenceManager.getDefaultSharedPreferences(CreditsActivity.this).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false);
		
		sendHike.setChecked(!sendNativeAlwaysPref);
		sendNative.setChecked(sendNativeAlwaysPref);

		nativeHeader.setText(R.string.regular_sms);
		hikeSmsHeader.setText(R.string.free_hike_sms);

		OnClickListener hikeSMSOnClickListener =  new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(true);
				sendNative.setChecked(false);
			}
		};
		
		OnClickListener nativeSMSOnClickListener =  new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(false);
				sendNative.setChecked(true);
			}
		};
		
		hikeSMS.setOnClickListener(hikeSMSOnClickListener);
		sendHike.setOnClickListener(hikeSMSOnClickListener);
		nativeSMS.setOnClickListener(nativeSMSOnClickListener);
		sendNative.setOnClickListener(nativeSMSOnClickListener);

		alwaysBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (!sendHike.isChecked() && !PreferenceManager.getDefaultSharedPreferences(CreditsActivity.this).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
				{
					showSMSClientDialog(sendHike.isChecked());
				}
				else
				{
					smsDialogActionClicked(true, sendHike.isChecked());
				}
				dialog.dismiss();
			}
		});
		
		justOnceBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				smsDialogActionClicked(false, sendHike.isChecked());
				dialog.dismiss();
			}
		});

		dialog.show();
	}
	
	private void smsDialogActionClicked(boolean alwaysBtnClicked, boolean isSendHikeChecked)
	{
		if(alwaysBtnClicked)
		{
			Utils.setSendUndeliveredAlwaysAsSmsSetting(CreditsActivity.this, true, !isSendHikeChecked);
		}
		else
		{
			Utils.setSendUndeliveredAlwaysAsSmsSetting(CreditsActivity.this, false);
		}
		setupUndeliveredSmsPrefLayout();
	}
	
	private void showSMSClientDialog(final boolean isSendHikeChecked)
	{

		HikeDialogListener smsClientDialogListener = new HikeDialog.HikeDialogListener()
		{

			@Override
			public void positiveClicked(Dialog dialog)
			{
				
				Utils.setReceiveSmsSetting(CreditsActivity.this, true);
				if (!settings.getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
				}
				smsDialogActionClicked(true, isSendHikeChecked);
				setupPreferenceLayout(findViewById(R.id.receive_sms_item), true);
				dialog.dismiss();
			}

			@Override
			public void neutralClicked(Dialog dialog)
			{
				
			}

			@Override
			public void negativeClicked(Dialog dialog)
			{
				smsDialogActionClicked(false, isSendHikeChecked);
				dialog.dismiss();
			}

			@Override
			public void onSucess(Dialog dialog)
			{
				// TODO Auto-generated method stub
				
			}
		};
		
		Dialog dialog = HikeDialog.showDialog(CreditsActivity.this, HikeDialog.SMS_CLIENT_DIALOG, smsClientDialogListener, false, null, false);  
		dialog.show();
	}
}