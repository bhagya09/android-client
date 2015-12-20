package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.db.AccountBackupRestore;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonItemCheckedListener;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonPojo;
import com.bsb.hike.dialog.DialogUtils;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.offline.OfflineController;
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
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.IconListPreference;
import com.bsb.hike.view.IconPreference;
import com.bsb.hike.view.NotificationToneListPreference;
import com.bsb.hike.view.PreferenceWithSubText;
import com.bsb.hike.view.SeekBarPreference;
import com.bsb.hike.view.SwitchPreferenceCompat;
import com.kpt.adaptxt.beta.AdaptxtSettings;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings;
import com.kpt.adaptxt.beta.KPTAddonItem;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class HikePreferences extends HikeAppStateBasePreferenceActivity implements OnPreferenceClickListener, 
							OnPreferenceChangeListener, DeleteAccountListener, BackupAccountListener, RingtoneFetchListener
{
	private enum BlockingTaskType
	{
		NONE, DELETING_ACCOUNT, UNLINKING_ACCOUNT, /*UNLINKING_TWITTER,*/ BACKUP_ACCOUNT, FETCH_RINGTONE
	}

	private ActivityCallableTask mTask;

	ProgressDialog mDialog;

	private Toolbar _toolBar;
	
	private BlockingTaskType blockingTaskType = BlockingTaskType.NONE;
	
	public static final float PREF_ENABLED_ALPHA = 1.0f;
	
	public static final float PREF_DISABLED_ALPHA = 0.24f;
	
	private boolean mIsResumed = false;
	
	KPTAdaptxtAddonSettings kptSettings;

	List<KPTAddonItem> mInstalledLanguagesList;

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
		_toolBar=(Toolbar)findViewById(R.id.abp__toolbar);
		_toolBar.setClickable(true);
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
       if (preferences == R.xml.keyboard_settings_preferences && titleRes == R.string.settings_localization || preferences == R.xml.kpt_advanced_preferences
    		   || preferences == R.xml.keyboard_preferences || preferences == R.xml.text_correction_preferences)
		{
			kptSettings = KptKeyboardManager.getInstance().getKptSettings();
			saveKeyboardPref();
		}

		addClickPreferences();
		addSwitchPreferences();
		addSeekbarPreferences();
		addAppLanguagePreference();

		Preference deletePreference = getPreferenceScreen().findPreference(HikeConstants.DELETE_PREF);
		if (deletePreference != null)
		{
			Utils.logEvent(HikePreferences.this, HikeConstants.LogEvent.PRIVACY_SCREEN);
		}
		else
		{
			Utils.logEvent(HikePreferences.this, HikeConstants.LogEvent.NOTIFICATION_SCREEN);
		}
		
		addSMSCardEnablePref();
				
		addStealthPrefListeners();
		
		Preference videoCompressPreference = getPreferenceScreen().findPreference(HikeConstants.COMPRESS_VIDEO_CATEGORY);
		if(videoCompressPreference != null && android.os.Build.VERSION.SDK_INT < 18)
		{
			getPreferenceScreen().removePreference(videoCompressPreference);
		}
		
		tryToSetupSMSPreferencesScreen();
		setupToolBar(titleRes);

	}
	
	private void addSMSCardEnablePref()
	{
		final SwitchPreferenceCompat smsCardEnablePref = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.SMS_CARD_ENABLE_PREF);
		if (smsCardEnablePref != null)
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_SMS_CARD_PREF, false))
			{
				smsCardEnablePref.setDependency(HikeConstants.ACTIVATE_STICKY_CALLER_PREF);
				smsCardEnablePref.setOnPreferenceChangeListener(this);
			}
			else
			{
				getPreferenceScreen().removePreference(smsCardEnablePref);
			}
		}
	}
	
	private void addSeekbarPreferences() {
		addOnSeekbarChangeListeners(HikeConstants.LONG_PRESS_DUR_PREF,200);
		addOnSeekbarChangeListeners(HikeConstants.KEYPRESS_VOL_PREF,1);
		addOnSeekbarChangeListeners(HikeConstants.KEYPRESS_VIB_DUR_PREF, 1);
	}

	private void saveKeyboardPref()
	{
		if (!HikeMessengerApp.isCustomKeyboardUsable())
		{
			PreferenceCategory keyboardSettings = (PreferenceCategory) getPreferenceScreen().findPreference(HikeConstants.KEYBOARD_SETTING_PREF_CATEGORY);
			if (keyboardSettings != null)
			{
				getPreferenceScreen().removePreference(keyboardSettings);
				return;
			}
		}

		Preference kbdPref = findPreference(HikeConstants.KEYBOARD_PREF);
		if (kbdPref != null && kbdPref instanceof SwitchPreferenceCompat)
		{
			boolean val = HikeMessengerApp.isSystemKeyboard();
			((SwitchPreferenceCompat) kbdPref).setChecked(!val);
		}

		ListPreference localLanguagePrf = (ListPreference) findPreference(HikeConstants.LOCAL_LANGUAGE_PREF);
		if (localLanguagePrf != null && localLanguagePrf instanceof ListPreference)
		{
			localLanguagePrf.setValue(LocalLanguageUtils.getApplicationLocalLanguage(HikePreferences.this).getDisplayName());
		}

		setKeyboardLangSummary();

		setPrefValueFromKpt(HikeConstants.AUTO_CAPITALIZATION_PREF, kptSettings.getAutoCapitalizationState());
		setPrefValueFromKpt(HikeConstants.AUTO_SPACING_PREF, kptSettings.getAutoSpacingState());
		setPrefValueFromKpt(HikeConstants.GLIDE_PREF, kptSettings.getGlideState());
		setPrefValueFromKpt(HikeConstants.SOUND_ON_KEYPRESS_PREF, kptSettings.getSoundOnKeyPressState());
		setPrefValueFromKpt(HikeConstants.POPUP_ON_KEYPRESS_PREF, kptSettings.getPopupOnKeyPressState());
		setPrefValueFromKpt(HikeConstants.VIBRATE_ON_KEYPRESS_PREF, kptSettings.getVibrateOnKeyPressState());
		setPrefValueFromKpt(HikeConstants.AUTO_CORRECT_PREF, kptSettings.getAutoCorrectionState());
		setPrefValueFromKpt(HikeConstants.DISPLAY_SUGGESTIONS_PREF, kptSettings.getDisplaySuggestionsState());
		setPrefValueFromKpt(HikeConstants.PRIVATE_MODE_PREF, kptSettings.getPrivateModeState());
		setPrefValueFromKpt(HikeConstants.DISPLAY_ACCENTS_PREF, kptSettings.getDisplayAccentsState());
		setPrefValueFromKpt(HikeConstants.LONG_PRESS_DUR_PREF, kptSettings.getLongPressDuration());
		setPrefValueFromKpt(HikeConstants.KEYPRESS_VOL_PREF, (int)kptSettings.getKeyPressSoundVolume());
		setPrefValueFromKpt(HikeConstants.KEYPRESS_VIB_DUR_PREF, kptSettings.getKeyPressVibrationDuration());
	}

	private void setPrefValueFromKpt(String PreferenceName, int state)
	{
		Preference preference = findPreference(PreferenceName);
		if (preference != null && preference instanceof SwitchPreferenceCompat)
		{
			boolean value = true;
			switch (state)
			{
				case AdaptxtSettings.KPT_TRUE:
					value = true;
					break;
				case AdaptxtSettings.KPT_FALSE:
					value = false;
					break;
			}
			((SwitchPreferenceCompat)preference).setChecked(value);
		}
		else if (preference != null && preference instanceof SeekBarPreference)
		{
			((SeekBarPreference)preference).setCurrentValue(state);
		}
	}
	
	private void setKeyboardLangSummary()
	{
		IconPreference kbdLanguagePref = (IconPreference) findPreference(HikeConstants.KEYBOARD_LANGUAGE_PREF);
		if (kbdLanguagePref != null && kbdLanguagePref instanceof IconPreference)
		{
			String summary = new String();
			ArrayList<KPTAddonItem> langList = KptKeyboardManager.getInstance().getInstalledLanguagesList();
			for (KPTAddonItem item : langList)
			{
				if (KptKeyboardManager.getInstance().getDictionaryLanguageStatus(item) == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_LOADED)
				{
					summary += item.getDisplayName();
					summary += ", ";
				}
			}
			summary = summary.substring(0, Math.max(summary.length() - 2, 0));
			kbdLanguagePref.setSummary(summary);			
		}
	}
	
	private void addClickPreferences()
	{
		addOnPreferenceClickListeners(HikeConstants.DELETE_PREF);
		addOnPreferenceClickListeners(HikeConstants.BACKUP_PREF);
		addOnPreferenceClickListeners(HikeConstants.UNLINK_PREF);
		addOnPreferenceClickListeners(HikeConstants.STICKER_REORDER_PREF);
		addOnPreferenceClickListeners(HikeConstants.BLOKED_LIST_PREF);
		addOnPreferenceClickListeners(HikeConstants.SYSTEM_HEALTH_PREF);
		addOnPreferenceClickListeners(HikeConstants.HELP_FAQS_PREF);
		addOnPreferenceClickListeners(HikeConstants.HELP_FEEDBACK_PREF);
		addOnPreferenceClickListeners(HikeConstants.HELP_TNC_PREF);
		addOnPreferenceClickListeners(HikeConstants.STATUS_BOOLEAN_PREF);
		addOnPreferenceClickListeners(HikeConstants.CHAT_BG_NOTIFICATION_PREF);
		addOnPreferenceClickListeners(HikeConstants.NOTIF_SOUND_PREF);
		addOnPreferenceClickListeners(HikeConstants.FAV_LIST_PREF);
		addOnPreferenceClickListeners(HikeConstants.KEYBOARD_LANGUAGE_PREF);
		addKeyboardPreferenceClickListeners(HikeConstants.KEYBOARD_PRIMARY_PREF);
		addKeyboardPreferenceClickListeners(HikeConstants.TEXT_CORRECTION_PREF);
		addKeyboardPreferenceClickListeners(HikeConstants.KEYBOARD_ADV_PREF);
	}
	
	private void addKeyboardPreferenceClickListeners(String preferenceName)
	{
		Preference preference = getPreferenceScreen().findPreference(preferenceName);
		if (preference != null)
		{
			Logger.d(getClass().getSimpleName(), preferenceName + " preference not null" + preference.getKey());
			preference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), preferenceName + " preference is null");
		}
	}
	
	private void addSwitchPreferences()
	{
		addOnPreferenceChangeListeners(HikeConstants.PROFILE_PIC_PREF);
		addOnPreferenceChangeListeners(HikeConstants.SEND_ENTER_PREF);
		addOnPreferenceChangeListeners(HikeConstants.DOUBLE_TAP_PREF);
		addOnPreferenceChangeListeners(HikeConstants.H2O_NOTIF_BOOLEAN_PREF);
		addOnPreferenceChangeListeners(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF);
		addKeyboardPreferenceChangeListener();
		addOnPreferenceChangeListeners(HikeConstants.GLIDE_PREF);
		addOnPreferenceChangeListeners(HikeConstants.AUTO_CORRECT_PREF);
		addOnPreferenceChangeListeners(HikeConstants.AUTO_CAPITALIZATION_PREF);
		addOnPreferenceChangeListeners(HikeConstants.AUTO_SPACING_PREF);
		addOnPreferenceChangeListeners(HikeConstants.DISPLAY_SUGGESTIONS_PREF);
		addOnPreferenceChangeListeners(HikeConstants.PRIVATE_MODE_PREF);
		addOnPreferenceChangeListeners(HikeConstants.DISPLAY_ACCENTS_PREF);
		addOnPreferenceChangeListeners(HikeConstants.POPUP_ON_KEYPRESS_PREF);
		addOnPreferenceChangeListeners(HikeConstants.SOUND_ON_KEYPRESS_PREF);
		addOnPreferenceChangeListeners(HikeConstants.VIBRATE_ON_KEYPRESS_PREF);
		addOnPreferenceChangeListeners(HikeConstants.ACTIVATE_STICKY_CALLER_PREF);
		addStickerRecommendPreferenceChangeListener();
		addSslPreferenceChangeListener();
		addStickerRecommendAutopopupPreferenceChangeListener();
		addEnableKnownNumberCardPrefListener();
	}
	
	private void addKeyboardPreferenceChangeListener()
	{
		final SwitchPreferenceCompat preference = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.KEYBOARD_PREF);
		if (preference != null)
		{
			preference.shouldDisableDependents();
			preference.setOnPreferenceChangeListener(this);
		}
	}
	
	private void addEnableKnownNumberCardPrefListener()
	{
		final SwitchPreferenceCompat knownContactEnablePref = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF);
		if (knownContactEnablePref != null)
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_KNOWN_NUMBER_CARD, false))
			{
				knownContactEnablePref.setDependency(HikeConstants.ACTIVATE_STICKY_CALLER_PREF);
				knownContactEnablePref.setOnPreferenceChangeListener(this);
			}
			else
			{
				getPreferenceScreen().removePreference(knownContactEnablePref);
			}
		}
	}
	
	private void addAppLanguagePreference()
	{
		final ListPreference languagePref = (ListPreference) getPreferenceScreen().findPreference(HikeConstants.LOCAL_LANGUAGE_PREF);
		if (languagePref != null)
		{
			final LocalLanguage localLanguage = LocalLanguageUtils.getApplicationLocalLanguage(HikePreferences.this);
			languagePref.setSummary(localLanguage.getDisplayName());
			languagePref.setNegativeButtonText(R.string.cancel);
			CharSequence entries[] = new String[localLanguage.getDeviceSupportedHikeLanguages(HikePreferences.this).size()];
			int i=0;
			for (LocalLanguage language : localLanguage.getDeviceSupportedHikeLanguages(HikePreferences.this))
			{
				entries[i] = language.getDisplayName();
				i++;
			}
			languagePref.setEntries(entries);
			languagePref.setEntryValues(entries);
			languagePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					for (LocalLanguage language : localLanguage.getDeviceSupportedHikeLanguages(HikePreferences.this)) {
						if (language.getDisplayName().equalsIgnoreCase((String) newValue)) {
							LocalLanguageUtils.setApplicationLocalLanguage(language);
							languagePref.setSummary(language.getDisplayName());
							//AND-3956 Begin: resetting offline parameters on language change
							String offlineParams = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}");
							OfflineController.getInstance().setConfiguration(offlineParams);
							//AND-3956 End
							restartHomeActivity();
						}
					}
					return true;
				}
			});
			//AND-4046 Begin
			languagePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UNSUPPORTED_LANG_TOAST_SHOWN, false))
					{
						String unsupportedLanguages = LocalLanguage.getUnsupportedLocaleToastText(HikePreferences.this);
						if (!TextUtils.isEmpty(unsupportedLanguages))
						{
							Toast.makeText(HikePreferences.this, unsupportedLanguages, Toast.LENGTH_LONG).show();
						}
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UNSUPPORTED_LANG_TOAST_SHOWN, true);
					}
					return false;
				}
			});
			//AND-4046 End
		}
	}

	private void restartHomeActivity()
	{
		IntentFactory.freshLaunchHomeActivity(getApplicationContext());
	}

	private void addStealthPrefListeners()
	{
		Preference stealthPreference = getPreferenceScreen().findPreference(HikeConstants.STEALTH_PREF_SCREEN);
		if(stealthPreference != null)
		{
			if (StealthModeManager.getInstance().isSetUp())
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
				
				IconListPreference changeStealthTimeout = (IconListPreference) getPreferenceScreen().findPreference(HikeConstants.CHANGE_STEALTH_TIMEOUT);
				if (changeStealthTimeout != null)
				{
					changeStealthTimeout.setTitle(getString(R.string.change_stealth_timeout) + ": " + changeStealthTimeout.getEntry());
					changeStealthTimeout.setSummary(R.string.change_stealth_timeout_body);
					changeStealthTimeout.setOnPreferenceChangeListener(this);
				}
				SwitchPreferenceCompat stealthIndicatorEnabled = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.STEALTH_INDICATOR_ENABLED);
				if (stealthIndicatorEnabled != null)
				{
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
					{
						stealthIndicatorEnabled.setTitle(R.string.enable_stealth_indicator);
						stealthIndicatorEnabled.setSummary(R.string.enable_stealth_indicator_body);
					}
					stealthIndicatorEnabled.setOnPreferenceChangeListener(this);			
				}
				
				SwitchPreferenceCompat stealthNotificationEnabled = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.STEALTH_NOTIFICATION_ENABLED);
				if (stealthNotificationEnabled != null)
				{
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
					{
						stealthNotificationEnabled.setTitle(R.string.enable_stealth_notification);
						stealthNotificationEnabled.setSummary(R.string.enable_stealth_notification_body);
					}
					stealthNotificationEnabled.setOnPreferenceChangeListener(this);
				}
				
			}
			else
			{
				getPreferenceScreen().removePreference(stealthPreference);
			}
		}

		Preference stealthCategory = getPreferenceScreen().findPreference(HikeConstants.STEALTH_PERF_SETTING);
		if(stealthCategory != null)
		{
			if (StealthModeManager.getInstance().isSetUp())
			{	
				Preference stealthModeSettings = getPreferenceScreen().findPreference(HikeConstants.STEALTH_MODE_PREF);
				if (stealthModeSettings != null)
				{
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
					{
						stealthModeSettings.setTitle(R.string.stealth_mode_title);
						stealthModeSettings.setSummary(R.string.stealth_mode_title_body);
					}
					stealthModeSettings.setOnPreferenceClickListener(this);
				}
				
			}else
			{
				getPreferenceScreen().removePreference(stealthCategory);
			}
		}
	}
	
	private void addStickerRecommendPreferenceChangeListener()
	{
		final SwitchPreferenceCompat stickerRecommendPreference = (SwitchPreferenceCompat) getPreferenceScreen()
				.findPreference(HikeConstants.STICKER_RECOMMEND_PREF);
		if (stickerRecommendPreference != null)
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, false))
			{
				stickerRecommendPreference.setOnPreferenceChangeListener(this);
			}
			else
			{
				PreferenceCategory stickerPreferenceCategory = (PreferenceCategory) findPreference(HikeConstants.STICKER_SETTINGS);
				stickerPreferenceCategory.removePreference(stickerRecommendPreference);
			}
		}
	}
	
	private void addSslPreferenceChangeListener()
	{
		final SwitchPreferenceCompat sslPreference = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.SSL_PREF);
		if (sslPreference != null)
		{
			if(Utils.isSSLAllowed())
			{
				sslPreference.setOnPreferenceChangeListener(this);
			}
			else
			{
				if(getPreferenceScreen().findPreference(HikeConstants.PRIVACY_SETTINGS_CATEGORY) instanceof PreferenceCategory)
				{
					PreferenceCategory privacySettingsCategory = ((PreferenceCategory) getPreferenceScreen().findPreference(HikeConstants.PRIVACY_SETTINGS_CATEGORY));
					if (privacySettingsCategory != null)
					{
						privacySettingsCategory.removePreference(sslPreference);
					}
				}
			}
		}
	}
	
	private void addStickerRecommendAutopopupPreferenceChangeListener()
	{
		final SwitchPreferenceCompat stickerRecommendAutopopupPreference = (SwitchPreferenceCompat) getPreferenceScreen()
				.findPreference(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF);
		if (stickerRecommendAutopopupPreference != null)
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, false))
			{
				stickerRecommendAutopopupPreference.setDependency(HikeConstants.STICKER_RECOMMEND_PREF);
				stickerRecommendAutopopupPreference.setOnPreferenceChangeListener(this);
			}
			else
			{
				PreferenceCategory stickerPreferenceCategory = (PreferenceCategory) findPreference(HikeConstants.STICKER_SETTINGS);
				stickerPreferenceCategory.removePreference(stickerRecommendAutopopupPreference);
			}
		}
	}
	
	private void addOnPreferenceClickListeners(String preferenceName)
	{
		Preference preference = getPreferenceScreen().findPreference(preferenceName);
		if (preference != null)
		{
			Logger.d(getClass().getSimpleName(), preferenceName + " preference not null" + preference.getKey());
			preference.setOnPreferenceClickListener(this);
		}
		else
		{
			Logger.d(getClass().getSimpleName(), preferenceName + " preference is null");
		}
	}
	
	private void addOnPreferenceChangeListeners(String preferenceName)
	{
		final SwitchPreferenceCompat preference = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(preferenceName);
		if (preference != null)
		{
			preference.setOnPreferenceChangeListener(this);
		}
	}
	
	private void addOnSeekbarChangeListeners(String preferenceName, int min) {
		final SeekBarPreference preference = (SeekBarPreference) getPreferenceScreen()
				.findPreference(preferenceName);
		if (preference != null) {
			preference.setMinimun(min);
			preference.setOnPreferenceChangeListener(this);
			setSeekbarPrefSummary(preferenceName, min);
		}
	}
	
	private void setSeekbarPrefSummary(String preferenceName, int min)
	{
		SeekBarPreference preference = (SeekBarPreference) getPreferenceScreen().findPreference(preferenceName);
		if (preference != null)
		{
			if (preferenceName == HikeConstants.KEYPRESS_VOL_PREF)
			{
				preference.setSummary("Level " + kptSettings.getKeyPressSoundVolume());
			}
			else if (preferenceName == HikeConstants.KEYPRESS_VIB_DUR_PREF)
			{
				preference.setSummary(kptSettings.getKeyPressVibrationDuration() + " ms");
			}
			else if (preferenceName == HikeConstants.LONG_PRESS_DUR_PREF)
			{
				preference.setSummary(kptSettings.getLongPressDuration() + " ms");
			}
		}
	}
	
	private void tryToSetupSMSPreferencesScreen()
	{
		Preference hikeOffline = getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_HIKE_OFFLINE);
		
		if (hikeOffline != null)
		{
			if (Utils.isKitkatOrHigher())
			{
				getPreferenceScreen().removePreference(hikeOffline);
			}
			
			else
			{
				String titleString = getString(R.string.hike_offline);
				String summaryString = getString(R.string.undelivered_sms_setting_summary);

				if (PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false))
				{
					if (PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
					{
						titleString += ": " + getString(R.string.regular_sms);
					}
					else
					{
						titleString += ": " + getString(R.string.free_hike_sms);
					}
					summaryString = getString(R.string.undelivered_sms_setting_remember);
				}

				hikeOffline.setTitle(titleString);
				hikeOffline.setSummary(summaryString);

				hikeOffline.setOnPreferenceClickListener(this);
			}
		}
		
		SwitchPreferenceCompat unifiedInbox = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_RECEIVE_SMS_PREF);
		
		if (unifiedInbox != null)
		{
			if (Utils.isKitkatOrHigher())
			{
				getPreferenceScreen().removePreference(unifiedInbox);
			}
			else
			{
				unifiedInbox.setTitle(R.string.default_client_header);
				unifiedInbox.setSummary(R.string.default_client_info);
				unifiedInbox.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false ));
				unifiedInbox.setOnPreferenceChangeListener(this);
			}
		}
		
		SwitchPreferenceCompat freeHike2SMS = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.FREE_SMS_PREF);
		
		if (freeHike2SMS != null)
		{
			freeHike2SMS.setTitle(R.string.free_hike_to_sms);
			freeHike2SMS.setSummary(R.string.free_sms_msg);
			freeHike2SMS.shouldDisableDependents();
			freeHike2SMS.setOnPreferenceChangeListener(this);
		}
		
		PreferenceWithSubText hike2hike = (PreferenceWithSubText) getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.HIKE_HIKE);
		{
			if (hike2hike != null)
			{
				hike2hike.setDependency(HikeConstants.SMS_SETTINGS.FREE_SMS_PREF);
			}
		}

		PreferenceWithSubText freeHike2SMSIndia = (PreferenceWithSubText) getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.FREE_HIKE_TO_SMS_INDIA);
		{
			if (freeHike2SMSIndia != null)
			{
				freeHike2SMSIndia.setDependency(HikeConstants.SMS_SETTINGS.FREE_SMS_PREF);
				int credits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getInt(HikeMessengerApp.SMS_SETTING, 0);
				freeHike2SMSIndia.setSubText(Integer.toString(credits));
			}
		}
		
		Preference earnFreeSMS = getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_EARN_FREE_SMS);
		
		if (earnFreeSMS != null)
		{
			earnFreeSMS.setDependency(HikeConstants.SMS_SETTINGS.FREE_SMS_PREF);
			earnFreeSMS.setOnPreferenceClickListener(this);
		}
		
		Preference inviteViaSMS = getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_INVITE_VIA_SMS);
		
		if (inviteViaSMS != null)
		{
			inviteViaSMS.setDependency(HikeConstants.SMS_SETTINGS.FREE_SMS_PREF);
			inviteViaSMS.setOnPreferenceClickListener(this);
		}
		
		SharedPreferences smsSettings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = smsSettings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();
		
	}

	private void setupToolBar(int titleRes)
	{
		_toolBar = (Toolbar) findViewById(R.id.abp__toolbar);
		_toolBar.setClickable(true);
		View backContainer = findViewById(R.id.back);
		TextView title = (TextView) findViewById(R.id.title);
		title.setText(titleRes);
		backContainer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(HikeConstants.Extras.BLOKING_TASK_TYPE, blockingTaskType.ordinal());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mIsResumed = true;
		setKeyboardLangSummary();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mIsResumed = false;
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
			// dismissing any existing dialog before showing the new one
			dismissProgressDialog();
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
					task.execute();
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
			String localappLang = LocalLanguageUtils.getApplicationLocalLanguageLocale();
			String helpURL = HikeConstants.HELP_URL;
			if(!TextUtils.isEmpty(localappLang)) {
				Uri modifiedURI = Uri.parse(helpURL).buildUpon().appendQueryParameter("locale", localappLang).build();
				helpURL = modifiedURI.toString();
			}
			Utils.startWebViewActivity(HikePreferences.this, helpURL, getString(R.string.faq));
		}
		else if (HikeConstants.HELP_TNC_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "T & C preference selected");
			Utils.startWebViewActivity(HikePreferences.this, HikeConstants.T_AND_C_URL, getString(R.string.terms_conditions_title));
		}
		else if (HikeConstants.HELP_FEEDBACK_PREF.equals(preference.getKey()))
		{
			Logger.d(getClass().getSimpleName(), "contact preference selected");
			Intent intent = IntentFactory.getEmailOpenIntent(HikePreferences.this);
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
				HikeMqttManagerNew.getInstance().sendMessage(jsonObject, MqttConstants.MQTT_QOS_ONE);
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
				HikeMqttManagerNew.getInstance().sendMessage(jsonObject, MqttConstants.MQTT_QOS_ONE);
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

				StealthModeManager.getInstance().setTipVisibility(false, ConversationTip.RESET_STEALTH_TIP);

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
				dialogStrings[2] = getString(R.string.CONFIRM);
				dialogStrings[3] = getString(R.string.CANCEL);
				
				HikeDialogFactory.showDialog(this, HikeDialogFactory.RESET_STEALTH_DIALOG, new HikeDialogListener()
				{

					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, System.currentTimeMillis());

						StealthModeManager.getInstance().setTipVisibility(true, ConversationTip.RESET_STEALTH_TIP);

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
			LockPattern.confirmPattern(HikePreferences.this, true, HikeConstants.ResultCodes.CONFIRM_AND_ENTER_NEW_PASSWORD);
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
					Utils.executeAsyncTask(task);
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
		else if(HikeConstants.STEALTH_MODE_PREF.equals(preference.getKey()))
		{
			startActivity(Utils.getIntentForHiddenSettings(HikePreferences.this));
		}
		
		/**
		 * SMS Pref Clicks begin here
		 */
		// Invite Via SMS Click
		else if (HikeConstants.SMS_SETTINGS.KEY_INVITE_VIA_SMS.equals(preference.getKey()))
		{
			Utils.logEvent(this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);

			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.INVITE_SMS_SCREEN_FROM_CREDIT);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}

			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Intent intent = IntentFactory.getInviteViaSMSIntent(this);
			startActivity(intent);
		}
		
		else if (HikeConstants.SMS_SETTINGS.KEY_EARN_FREE_SMS.equals(preference.getKey()))
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.START_HIKING);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Intent intent = new Intent(this, ComposeChatActivity.class);
			startActivity(intent);

		}
		
		else if (HikeConstants.SMS_SETTINGS.KEY_HIKE_OFFLINE.equals(preference.getKey()))
		{
			showSMSDialog();
		}
		
		else if(HikeConstants.STICKER_REORDER_PREF.equals(preference.getKey()))
		{
			Intent i = new Intent(HikePreferences.this, StickerSettingsActivity.class);
			startActivity(i);
		}
		else if (HikeConstants.KEYBOARD_LANGUAGE_PREF.equals(preference.getKey()))
		{
			IntentFactory.openKeyboardLanguageSetting(HikePreferences.this);
		}
		else if(HikeConstants.KEYBOARD_PRIMARY_PREF.equals(preference.getKey()))
		{
			startActivity(IntentFactory.getIntentForKeyboardPrimarySettings(HikePreferences.this));
		}
		else if(HikeConstants.TEXT_CORRECTION_PREF.equals(preference.getKey()))
		{
			startActivity(IntentFactory.getIntentForTextCorrectionSettings(HikePreferences.this));
		}
		else if(HikeConstants.KEYBOARD_ADV_PREF.equals(preference.getKey()))
		{
			startActivity(IntentFactory.getIntentForKeyboardAdvSettings(HikePreferences.this));
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
	
	private void stealthConfirmPasswordOnPreferenceChange(Preference preference, Object value)
	{
		Bundle stealthBundle = new Bundle();
		if(preference instanceof IconListPreference)
		{
			stealthBundle.putString(preference.getKey(), (String) value);
		}
		else if (preference instanceof SwitchPreferenceCompat)
		{
			stealthBundle.putBoolean(preference.getKey(), (boolean) value);	
		}
		LockPattern.confirmPattern(HikePreferences.this, true, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_CHANGE_PREF, stealthBundle);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		Logger.d("HikePreferences", "Preference changed: " + preference.getKey());

		if (HikeConstants.CHANGE_STEALTH_TIMEOUT.equals(preference.getKey()))
		{
			stealthConfirmPasswordOnPreferenceChange(preference, newValue);
			return false;
		}

		if (newValue instanceof Integer)
		{
			int value = (int) newValue;
			if (HikeConstants.LONG_PRESS_DUR_PREF.equals(preference.getKey()))
			{
				kptSettings.setLongPressDuration(value);
				preference.setSummary(value + " ms");
			}
			else if (HikeConstants.KEYPRESS_VOL_PREF.equals(preference.getKey()))
			{
				kptSettings.setKeyPressSoundVolume(value);
				preference.setSummary("Level " + value);
			}
			else if (HikeConstants.KEYPRESS_VIB_DUR_PREF.equals(preference.getKey()))
			{
				kptSettings.setKeyPressVibrationDuration(value);
				preference.setSummary(value + " ms");
			}
		}
		else if (newValue instanceof Boolean)
		{
			boolean isChecked = (Boolean) newValue;

			if (HikeConstants.SMS_SETTINGS.KEY_RECEIVE_SMS_PREF.equals(preference.getKey())) {
				try {
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.UNIFIED_INBOX, String.valueOf(isChecked));
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				} catch (JSONException e) {
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}

				if (!isChecked) {
					Editor editor = PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).edit();
					editor.putBoolean(HikeConstants.SEND_SMS_PREF, false);
					editor.commit();
				} else {
					if (!HikePreferences.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false)) {
						showSMSSyncDialog();
					}
				}
			} else if (HikeConstants.PROFILE_PIC_PREF.equals(preference.getKey())) {
				JSONObject object = new JSONObject();
				try {
					object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

					int avatarSetting = 1;
					if (isChecked) {
						avatarSetting = 2;
					}
					JSONObject data = new JSONObject();
					data.put(HikeConstants.AVATAR, avatarSetting);
					object.put(HikeConstants.DATA, data);

					HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
				} catch (JSONException e) {
					Logger.w(getClass().getSimpleName(), "Invalid json", e);
				}
			} else if (HikeConstants.SMS_SETTINGS.FREE_SMS_PREF.equals(preference.getKey())) {
				Logger.d(getClass().getSimpleName(), "Free SMS toggled");
				HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, isChecked);

				try {
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.FREE_SMS_ON, String.valueOf(isChecked));
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				} catch (JSONException e) {
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
				HikeMessengerApp.getPubSub().publish(HikePubSub.ENTER_TO_SEND_SETTINGS_CHANGED, isChecked);
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
					e.printStackTrace();
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.NUDGE_SETTINGS_CHANGED, isChecked);
			} else if (HikeConstants.STICKER_RECOMMEND_PREF.equals(preference.getKey())) {
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_RECOMMEND_PREF, isChecked);
				HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_RECOMMEND_PREFERENCE_CHANGED, null);

				StickerManager.getInstance().sendRecommendationlSettingsStateAnalytics(StickerManager.FROM_CHAT_SETTINGS, isChecked);
			} else if (HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF.equals(preference.getKey())) {
				StickerSearchManager.getInstance().setShowAutoPopupSettingOn(isChecked);
				StickerSearchManager.getInstance().saveOrDeleteAutoPopupTrialState(true);

				// Auto-suggestion setting is turned on by user, remove disable toast pref which was set automatically due to rejection pattern
				if (isChecked) {
					HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_AUTO_RECOMMEND_SETTING_OFF_TIP);
				}

				StickerManager.getInstance().sendRecommendationAutopopupSettingsStateAnalytics(StickerManager.FROM_CHAT_SETTINGS, isChecked);
			} else if (HikeConstants.SSL_PREF.equals(preference.getKey())) {
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(HikeConstants.SSL_PREF, isChecked).commit();
				Utils.setupUri();
				HttpRequestConstants.toggleSSL();
				LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(HikePubSub.SSL_PREFERENCE_CHANGED));
			} else if (HikeConstants.STATUS_BOOLEAN_PREF.equals(preference.getKey())) {
				//Handled in OnPreferenceClick
			} else if (HikeConstants.ACTIVATE_STICKY_CALLER_PREF.equals(preference.getKey())) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(HikePreferences.this);
				Editor prefEditor = prefs.edit();
				prefEditor.putBoolean(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, isChecked);
				prefEditor.commit();

				if (isChecked) {
					ChatHeadUtils.registerCallReceiver();
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.ACTIVATE_BUTTON, null);
				} else {
					ChatHeadUtils.unregisterCallReceiver();
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.DEACTIVATE_BUTTON, null);
				}
			} else if (HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF.equals(preference.getKey())) {
				if (isChecked) {
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.KNOWN_CARD_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.ACTIVATE_BUTTON, null);

				} else {
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.KNOWN_CARD_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.DEACTIVATE_BUTTON, null);
				}
			}
			else if(HikeConstants.SMS_CARD_ENABLE_PREF.equals(preference.getKey()))
			{
				if (isChecked)
				{
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SMS_CARD_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.ACTIVATE_BUTTON, null);
			
				}
				else
				{
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SMS_CARD_SETTINGS_TOGGLE, null,
							AnalyticsConstants.StickyCallerEvents.DEACTIVATE_BUTTON, null);
				}
			}
			else if (HikeConstants.NUJ_NOTIF_BOOLEAN_PREF.equals(preference.getKey())) {
				try {
					JSONObject metadata = new JSONObject();

					if (isChecked) {
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_NUJ_ON);
					} else {
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_NUJ_OFF);
					}
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				} catch (JSONException e) {
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				JSONObject object = new JSONObject();

				try {
					object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

					JSONObject data = new JSONObject();
					data.put(HikeConstants.UJ_NOTIF_SETTING, isChecked ? 1 : 0);
					data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
					object.put(HikeConstants.DATA, data);

					HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
				} catch (JSONException e) {
					Logger.w(getClass().getSimpleName(), "Invalid json", e);
				}
			} else if (HikeConstants.H2O_NOTIF_BOOLEAN_PREF.equals(preference.getKey())) {
				try {
					JSONObject metadata = new JSONObject();

					if (isChecked) {
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_H2O_ON);
					} else {
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SETTINGS_NOTIFICATION_H2O_OFF);
					}
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				} catch (JSONException e) {
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			} else if (HikeConstants.STEALTH_NOTIFICATION_ENABLED.equals(preference.getKey())) {
				stealthConfirmPasswordOnPreferenceChange(preference, newValue);
				return false;
			} else if (HikeConstants.STEALTH_INDICATOR_ENABLED.equals(preference.getKey())) {
				stealthConfirmPasswordOnPreferenceChange(preference, newValue);
				return false;
			} else if (HikeConstants.KEYBOARD_PREF.equals(preference.getKey())) {
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SYSTEM_KEYBOARD_SELECTED, !isChecked);
				trackAnalyticEvent(HikeConstants.LogEvent.HIKE_KEYBOARD_ON, HikeConstants.TOGGLE, isChecked);
				HikeMessengerApp.getPubSub().publish(HikePubSub.KEYBOARD_SWITCHED, null);
			} else if (HikeConstants.GLIDE_PREF.equals(preference.getKey())) {
				kptSettings.setGlideState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.GLIDE_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.AUTO_CORRECT_PREF.equals(preference.getKey())) {
				kptSettings.setAutoCorrectionState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.AUTO_CORRECT_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.AUTO_CAPITALIZATION_PREF.equals(preference.getKey())) {
				kptSettings.setAutoCapitalizationState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.AUTO_CAPITALIZATION_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.AUTO_SPACING_PREF.equals(preference.getKey())) {
				kptSettings.setAutoSpacingState(isChecked ? AdaptxtSettings.KPT_TRUE : kptSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.AUTO_SPACING_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.DISPLAY_SUGGESTIONS_PREF.equals(preference.getKey())) {
				kptSettings.setDisplaySuggestionsState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.DISPLAY_SUGGESTION_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.PRIVATE_MODE_PREF.equals(preference.getKey())) {
				kptSettings.setPrivateModeState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.PRIVATE_MODE_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.DISPLAY_ACCENTS_PREF.equals(preference.getKey())) {
				kptSettings.setDisplayAccentsState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
			} else if (HikeConstants.POPUP_ON_KEYPRESS_PREF.equals(preference.getKey())) {
				kptSettings.setPopupOnKeyPressState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.KEYPRESS_POPUP_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.SOUND_ON_KEYPRESS_PREF.equals(preference.getKey())) {
				kptSettings.setSoundOnKeyPressState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.KEYPRESS_SOUND_ON, HikeConstants.TOGGLE, isChecked);
			} else if (HikeConstants.VIBRATE_ON_KEYPRESS_PREF.equals(preference.getKey())) {
				kptSettings.setVibrateOnKeyPressState(isChecked ? AdaptxtSettings.KPT_TRUE : AdaptxtSettings.KPT_FALSE);
				trackAnalyticEvent(HikeConstants.LogEvent.KEYPRESS_VIBRATION_ON, HikeConstants.TOGGLE, isChecked);
			}
		}
		return true;
	}

	private void showSMSSyncDialog()
	{
		HikeDialogFactory.showDialog(this, HikeDialogFactory.SMS_SYNC_DIALOG, true);
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

	/*
	 * This method tracks the click analytic events on switch preferences
	 */
	private void trackAnalyticEvent(String eventKey, String event, boolean isChecked)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, eventKey);
			metadata.put(event, String.valueOf(isChecked));
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + event + "\n" + e);
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
					if(slectedPrivacyId == -1)
					{
						Toast.makeText(getBaseContext(), R.string.ls_change_failed, Toast.LENGTH_SHORT).show();
						return false;
					}
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
					preference.setTitle(getString(R.string.last_seen_header) + ": " + selectedPrivacyValue);
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
						((IconListPreference)preference).setTitleColor(R.color.settings_text_header_color);
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
			lp.setTitleColor(R.color.blue_hike);
		lp.setTitle(lp.getTitle() + ": " + lp.getEntry());
		lp.setNegativeButtonText(R.string.CANCEL);
	}

	private String getLSSummaryText()
	{
		String defValue = getApplicationContext().getString(R.string.privacy_my_contacts);
		String selectedValue = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(HikeConstants.LAST_SEEN_PREF_LIST, defValue);
		String summaryTxt = null;
		int selIndex = Integer.parseInt(selectedValue);
		if(selIndex == -1)
			return null;
		switch (HikeConstants.PrivacyOptions.values()[selIndex]) {
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
		long lastBackupTime = AccountBackupRestore.getInstance(getApplicationContext()).getLastBackupTime();
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
				//AND-3843 Begin
				ListPreference  preferenceVib= (ListPreference) preference;
				int index = preferenceVib.findIndexOfValue(newValue.toString());
				if (index >= 0) {
					preference.setTitle(getString(R.string.vibrate) + ": " + preferenceVib.getEntries()[index]);
				}
				//AND-3843 End
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
                //AND-3843 Begin
		if(TextUtils.isEmpty(lp.getEntry())){
			lp.setValueIndex(1); // 1= DEFAULT, which is default mentioned in notifications_preferences.xml
		}
		lp.setTitle(lp.getTitle() + ": " + lp.getEntry());
                //AND-3843 End
		lp.setNegativeButtonText(R.string.CANCEL);
		
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
					ListPreference preferenceLed = (ListPreference) preference;
					int index = preferenceLed.findIndexOfValue(newValue.toString());

					if (index >= 0) {
						preference.setTitle(getString(R.string.led_notification) + ": " + preferenceLed.getEntries()[index]);
					}

					if(getString(R.string.led_color_none_key).equals(newValue.toString()))
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
		

		String entry = (String) ledPref.getEntry();
		if (entry == null)
		{
			/*
			 * Notification Led Color There are three case following :-
			 * 
			 * 1>> 3.6.0 where we provide checkbox and key to store value is HikeConstants.LED_PREF(value boolean type) in DefaultSharedPreferences.
			 * 
			 * 2>> and upto 3.8.9 we provide list of color and value and entry contains the same string and now values are stored with key
			 * HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE in HikeSharedPreferenceUtil.
			 * 
			 * 3>> Now we changing values in this version with hexvalues of color because some phone are unable to parse purple color and also remove default value from
			 * notification_preferences.xml now we set default value at run time.
			 */
			
			HikeSharedPreferenceUtil hikeSharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();
			int previousVersionColor = HikeConstants.LED_DEFAULT_WHITE_COLOR;
			if (hikeSharedPreferenceUtil.contains(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE))
			{
				previousVersionColor = hikeSharedPreferenceUtil.getData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_NONE_COLOR);
			}
			else
			{
				try
				{	//this case will occur when user never changed his notification color in previous build but open notification setting screen at least once.
					previousVersionColor = Color.parseColor(ledPref.getValue());
				}
				catch (Exception e)
				{
					Logger.e(getClass().getSimpleName(), "Color Parsing Error from key HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE whose value is " + ledPref.getValue(), e);
				}

			}

			if (previousVersionColor == HikeConstants.LED_NONE_COLOR)
			{
				ledPref.setValueIndex(0);
			}
			else
			{

				String[] ledPrefValues = getResources().getStringArray(R.array.ledPrefValues);
				for (int i = 1; i < ledPrefValues.length; i++)
				{
					try
					{
						if (Color.parseColor(ledPrefValues[i].toLowerCase(Locale.getDefault())) == previousVersionColor)
						{
							ledPref.setValueIndex(i);
						}
					}
					catch (Exception e)
					{
						Logger.e(getClass().getSimpleName(), "Color Parsing Error = " + ledPrefValues[i], e);
					}
				}
			}

		}

		ledPref.setTitle(ledPref.getTitle() + ": " + ledPref.getEntry());
		ledPref.setNegativeButtonText(R.string.CANCEL);
	}

	@Override
	public void accountDeleted(final boolean isSuccess)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (isSuccess)
				{
					accountDeleted();
				}
				else
				{
					dismissProgressDialog();
					int duration = Toast.LENGTH_LONG;
					Toast toast = Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.unlink_account_failed), duration);
					toast.show();
				}
			}
		});
	}
	
	/**
	 * Adding this to handle the onactivityresult callback for reset password 
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_CHANGE_PREF)
		{
			if(resultCode != RESULT_OK)
			{
				return;
			}
			
			JSONObject metadata = new JSONObject();
			try
			{
			metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StealthEvents.STEALTH);
			metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StealthEvents.STEALTH_PREFERENCE_CHANGE);
				
			Bundle stealthBundle = data.getExtras();
			if(stealthBundle != null)
			{
				if(stealthBundle.containsKey(HikeConstants.CHANGE_STEALTH_TIMEOUT))
				{
					IconListPreference changeStealthTimeout = (IconListPreference)getPreferenceScreen().findPreference(HikeConstants.CHANGE_STEALTH_TIMEOUT);
					CharSequence newTimeoutKey = changeStealthTimeout.getEntries()[changeStealthTimeout.findIndexOfValue(stealthBundle.getString(HikeConstants.CHANGE_STEALTH_TIMEOUT))];
					changeStealthTimeout.setTitle(getString(R.string.change_stealth_timeout) + ": " + newTimeoutKey);
					String newValue = stealthBundle.getString(HikeConstants.CHANGE_STEALTH_TIMEOUT);
					changeStealthTimeout.setValue(newValue);
					metadata.put(HikeConstants.KEY, HikeConstants.CHANGE_STEALTH_TIMEOUT);
					metadata.put(HikeConstants.VALUE, newValue);
				}
				else if(stealthBundle.containsKey(HikeConstants.STEALTH_INDICATOR_ENABLED))
				{
					SwitchPreferenceCompat stealthIndicatorEnabled = (SwitchPreferenceCompat)getPreferenceScreen().findPreference(HikeConstants.STEALTH_INDICATOR_ENABLED);
					boolean newValue = stealthBundle.getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED);
					if(!newValue)
					{
						HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STEALTH_INDICATOR_SHOW_REPEATED);
						HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE);
					}
					stealthIndicatorEnabled.setChecked(newValue);
					metadata.put(HikeConstants.KEY, HikeConstants.STEALTH_INDICATOR_ENABLED);
					metadata.put(HikeConstants.VALUE, newValue);
				}
				else if(stealthBundle.containsKey(HikeConstants.STEALTH_NOTIFICATION_ENABLED))
				{
					SwitchPreferenceCompat stealthNotificationEnabled = (SwitchPreferenceCompat)getPreferenceScreen().findPreference(HikeConstants.STEALTH_NOTIFICATION_ENABLED);
					boolean newValue = stealthBundle.getBoolean(HikeConstants.STEALTH_NOTIFICATION_ENABLED);
					stealthNotificationEnabled.setChecked(newValue); 
					metadata.put(HikeConstants.KEY, HikeConstants.STEALTH_NOTIFICATION_ENABLED);
					metadata.put(HikeConstants.VALUE, newValue);
				}		
			}

			} catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);

		}
		else
		{
			if(data != null)
			{
				//passing true here to denote that this is coming from the password reset operation
				data.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET, true);	
			}
		}
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
		if(notificationPreference != null && mIsResumed && !isFinishing())
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

		HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
	}
	
	private void showSMSDialog()
	{
		final CustomAlertRadioButtonDialog dialog = new CustomAlertRadioButtonDialog(this, HikeDialogFactory.SMS_PREF_DIALOG,  DialogUtils.getSMSOptions(this), new RadioButtonItemCheckedListener()
		{
			@Override
			public void onRadioButtonItemClicked(RadioButtonPojo whichItem, CustomAlertRadioButtonDialog dialog)
			{
				dialog.selectedRadioGroup = whichItem;
			}
			
		});
		
		HikeDialogListener listener = new HikeDialogListener()
		{
			
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				if (dialog.getCheckedRadioButtonId() != R.string.free_hike_sms && !PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
				{
					showSMSClientDialog(dialog.getCheckedRadioButtonId() == R.string.free_hike_sms);
				}
				else
				{
					smsDialogActionClicked(true, dialog.getCheckedRadioButtonId() == R.string.free_hike_sms);
				}
				hikeDialog.dismiss();
			}
			
			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}
			
			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				smsDialogActionClicked(false, dialog.getCheckedRadioButtonId() == R.string.free_hike_sms);
				hikeDialog.dismiss();
			}
		};
		dialog.setTitle(R.string.choose_setting);
		dialog.setPositiveButton(R.string.ALWAYS, listener);
		dialog.setNegativeButton(R.string.JUST_ONCE, listener);
		dialog.show();
	}
	
	private void showSMSClientDialog(final boolean isSendHikeChecked)
	{
		HikeDialogListener smsClientDialogListener = new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				
				Utils.setReceiveSmsSetting(HikePreferences.this, true);
				if (!getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
				{
					showSMSSyncDialog();
				}
				smsDialogActionClicked(true, isSendHikeChecked);
				
				SwitchPreferenceCompat unifiedInbox = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_RECEIVE_SMS_PREF);
				
				if (unifiedInbox != null)
				{
					unifiedInbox.setTitle(R.string.default_client_header);
					unifiedInbox.setSummary(R.string.default_client_info);
					unifiedInbox.setChecked(PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.RECEIVE_SMS_PREF,
				 false));
				}
				
				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
				
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				smsDialogActionClicked(false, isSendHikeChecked);
				hikeDialog.dismiss();
			}

		};
		
		HikeDialogFactory.showDialog(this, HikeDialogFactory.SMS_CLIENT_DIALOG, smsClientDialogListener, false, null, false);  
	}


	private void smsDialogActionClicked(boolean alwaysBtnClicked, boolean isSendHikeChecked)
	{
		if(alwaysBtnClicked)
		{
			Utils.setSendUndeliveredAlwaysAsSmsSetting(this, true, !isSendHikeChecked);
		}
		else
		{
			Utils.setSendUndeliveredAlwaysAsSmsSetting(this, false);
		}

		Preference pref = getPreferenceScreen().findPreference(HikeConstants.SMS_SETTINGS.KEY_HIKE_OFFLINE);
		
		if (pref != null)
		{
			String titleString = getString(R.string.hike_offline);
			String summaryString = getString(R.string.undelivered_sms_setting_summary);

			if (PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false))
			{
				if (PreferenceManager.getDefaultSharedPreferences(HikePreferences.this).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
				{
					titleString += ": " + getString(R.string.regular_sms);
				}
				else
				{
					titleString += ": " + getString(R.string.free_hike_sms);
				}
				summaryString = getString(R.string.undelivered_sms_setting_remember);
			}
			
			pref.setTitle(titleString);
			pref.setSummary(summaryString);
		}
	}

}
