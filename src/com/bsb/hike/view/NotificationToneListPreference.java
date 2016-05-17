package com.bsb.hike.view;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.SoundUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationToneListPreference extends ListPreference implements DialogInterface.OnClickListener
{
	private Context mContext;

	private int mClickedDialogEntryIndex;

	private Map<String, Uri> ringtonesNameURIMap;

	private static int HIKE_JINNGLE_INDEX = 2;

	private static final String STATE_PARENT = "state_parent";

	private static final String SOUND_PREF_KEY = "sound_pref_key";

	private static final String SOUND_PREF_VALUES = "sound_pref_values";

	public NotificationToneListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		this.ringtonesNameURIMap = new LinkedHashMap<String, Uri>();
		//this.setValueIndex(HIKE_JINNGLE_INDEX);
		String defaultTone = mContext.getResources().getString(R.string.notif_sound_Hike);
		String selectedRingtone = PreferenceManager.getDefaultSharedPreferences(mContext).getString(HikeConstants.NOTIF_SOUND_PREF, defaultTone);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIF_SOUND_PREF, selectedRingtone);
		setTitle(mContext.getString(R.string.notificationSoundTitle) + " - " + selectedRingtone);
	}


	@Override
	protected void onBindView(View view)
	{
		ViewCompat.setAlpha(view, isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);
		
		super.onBindView(view);
	}

	private int getValueIndex()
	{
		return findIndexOfValue(this.getValue().toString());
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder)
	{
		mClickedDialogEntryIndex = getValueIndex();
		builder.setSingleChoiceItems(this.getEntries(), mClickedDialogEntryIndex, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				// stop the previously playing ringtone first
				SoundUtils.stopSound();
				mClickedDialogEntryIndex = which;
				playSoundAsPerToneClicked();
			}
		});

		builder.setPositiveButton(R.string.OK, this);
		builder.setNegativeButton(R.string.CANCEL, this);
	}

	private void playSoundAsPerToneClicked()
	{
		Object newValue = this.getEntryValues()[mClickedDialogEntryIndex];
		String selectedNotificationTone = newValue.toString();
		if (mContext.getString(R.string.notif_sound_off).equals(selectedNotificationTone))
		{
			// Here No Sound is played
			return;
		}
		else if (mContext.getString(R.string.notif_sound_Hike).equals(selectedNotificationTone))
		{
			SoundUtils.playSoundFromRaw(mContext, R.raw.hike_jingle_15, AudioManager.STREAM_NOTIFICATION);
		}
		else if (mContext.getString(R.string.notif_sound_default).equals(selectedNotificationTone))
		{
			SoundUtils.playSound(mContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioManager.STREAM_NOTIFICATION);
		}
		else
		{
			SoundUtils.playSound(mContext, ringtonesNameURIMap.get((String) newValue), AudioManager.STREAM_NOTIFICATION);
		}
	}

	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			SoundUtils.stopSound();
			String selectedRingtoneValue = this.getEntryValues()[mClickedDialogEntryIndex].toString();
			this.setValue(selectedRingtoneValue);
			setTitle(mContext.getString(R.string.notificationSoundTitle) + " - " + selectedRingtoneValue);
			recordToneChangePrefValue(selectedRingtoneValue);
			String selectedRintoneUri = getFinalSelectedRingtoneUri(selectedRingtoneValue);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.NOTIFICATION_TONE_URI, selectedRintoneUri);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.NOTIFICATION_TONE_NAME, selectedRingtoneValue);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIF_SOUND_PREF, selectedRingtoneValue);
			dialog.dismiss();
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			SoundUtils.stopSound();
			dialog.dismiss();
			break;

		default:
			break;
		}
	}

	private String getFinalSelectedRingtoneUri(String selectedRingtoneValue)
	{
		String ringtoneUri = "";
		if(mClickedDialogEntryIndex == 0)
		{
			ringtoneUri = mContext.getResources().getString(R.string.notif_sound_off);
		}
		else if(mClickedDialogEntryIndex == 1)
		{
			ringtoneUri = mContext.getResources().getString(R.string.notif_sound_default);
		}
		else if(mClickedDialogEntryIndex == 2)
		{
			ringtoneUri = mContext.getResources().getString(R.string.notif_sound_Hike);
		}
		else
		{
			ringtoneUri = ringtonesNameURIMap.get(selectedRingtoneValue).toString();
		}
		return ringtoneUri;
	}
	
	private void setEntryAndValues(Map<String, Uri> ringtonesNameURIMap)
	{
		this.ringtonesNameURIMap = ringtonesNameURIMap;
		CharSequence[] rintoneCharSeq = ringtonesNameURIMap.keySet().toArray(new CharSequence[ringtonesNameURIMap.size()]);
		setEntries(rintoneCharSeq);
		setEntryValues(rintoneCharSeq);
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);
		this.setEnabled(true);
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable superState = super.onSaveInstanceState();

		Bundle state = new Bundle();
		state.putParcelable(STATE_PARENT, superState);
		
		state.putStringArrayList(SOUND_PREF_KEY, new ArrayList<String>(ringtonesNameURIMap.keySet()));
		
		ArrayList<String> soundUriValues = new ArrayList<String>();
		for (Uri uri : ringtonesNameURIMap.values())
		{
			if(uri != null)
			{
				soundUriValues.add(uri.toString());
			}
			else
			{
				soundUriValues.add(null);
			}
		}
		state.putStringArrayList(SOUND_PREF_VALUES, soundUriValues);
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		Bundle savedState = (Bundle) state;

		Parcelable superState = savedState.getParcelable(STATE_PARENT);
		ArrayList<String> keys= savedState.getStringArrayList(SOUND_PREF_KEY);
		ArrayList<String> values = savedState.getStringArrayList(SOUND_PREF_VALUES);
		
		int i = 0;
		for (String key : keys)
		{
			if(values.get(i) != null)
			{
				ringtonesNameURIMap.put(key, Uri.parse(values.get(i)));
			}
			else
			{
				ringtonesNameURIMap.put(key, null);
			}
			i++;
		}
		
		setEntryAndValues(ringtonesNameURIMap);
		super.onRestoreInstanceState(superState);
	}

	public void setContext(Context context)
	{
		this.mContext = context;
	}

	public void createAndShowDialog(Map<String, Uri> ringtonesNameURIMap2)
	{
		dismissDialog();
		setEntryAndValues(ringtonesNameURIMap2);
		showDialog(null);
	}
	
	public boolean isEmpty()
	{
		return ringtonesNameURIMap.isEmpty();
	}

	@Override
	protected void onClick()
	{
		// TODO Auto-generated method stub
		super.onClick();
		if(isEmpty())
		{
			dismissDialog();
		}
	}
	
	private void dismissDialog()
	{
		if(this.getDialog() != null && this.getDialog().isShowing())
		{
			this.getDialog().dismiss();
		}
	}

	private void recordToneChangePrefValue(String selectedRingtoneValue)
	{
		try
		{
			JSONObject json = HikeAnalyticsEvent.getSettingsAnalyticsJSON();

			if (json != null)
			{
				json.put(AnalyticsConstants.V2.FAMILY, "notif_tone");
				json.put(AnalyticsConstants.V2.GENUS, selectedRingtoneValue);
				HAManager.getInstance().recordV2(json);
			}
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

}
