package com.bsb.hike.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;

public class SoundUtils
{
	private static final String TAG = "SoundUtils";
	
	private static Ringtone mRingtone = null;

	/**
	 * Message sending sound is NOT played under following conditions
	 * 1) Settings are off
	 * 2) User is in Audio/Vedio/Voip Call
	 * 3) Music is playing
	 * 4) Mode is Silent/Vibrate 
	 * @param context
	 * @return
	 */
	public static boolean isTickSoundEnabled(Context context)
	{
		return (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.TICK_SOUND_PREF, true) 
				&& !Utils.isUserInAnyTypeOfCall(context) 
				&& !isAnyMusicPlaying(context)
				&& !isSilentOrVibrateMode(context));
	}

	/**
	 * Plays non-ducking sound on Music Stream from given file inside raw folder. 
	 * 
	 * @param context
	 * @param soundId
	 * @param streamTYpe: on Which stream this sound is to be played Notification/Ringtone/Music
	 */
	public static void playSoundFromRaw(final Context context, int soundId, int streamType)
	{
		Logger.i("sound", "playing sound " + soundId);

		// define sound URI, the sound to be played when there's a notification
		Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
		playSound(context, soundUri, streamType);
	}

	/**
	 * Plays non-ducking sound on Music Stream from default tone. 
	 * @param context
	 */
	public static void playDefaultNotificationSound(Context context)
	{
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		playSound(context, notification, AudioManager.STREAM_MUSIC);
	}

	/**
	 * Plays non-ducking sound on Music Stream from given Uri. 
	 * 
	 * @param context
	 * @param soundUri
	 * @param streamTYpe: on Which stream this sound is to be played Notification/Ringtone/Music
	 */
	public static void playSound(final Context context, Uri soundUri, int streamType)
	{
		mRingtone = RingtoneManager.getRingtone(context, soundUri);
		if(mRingtone != null)
		{
			mRingtone.setStreamType(streamType);
			mRingtone.play();
		}
		else
		{
			Logger.e(TAG, "Failed to open ringtone: " + soundUri);
		}
	}

	/**
	 * Stops previously playing ringtone
	 */
	public static void stopSound()
	{
		if(mRingtone != null)
		{
			mRingtone.stop();
		}
	}

	public static int getCurrentVolume(Context context, int streamType)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am.getStreamVolume(streamType);
	}

	public static void setCurrentVolume(Context context, int streamType, int vol)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(streamType, vol, AudioManager.ADJUST_SAME);
	}

	public static boolean isAnyMusicPlaying(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am.isMusicActive();
	}

	public static boolean isSilentOrVibrateMode(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = am.getRingerMode();
		return (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE);
	}
	
	public static boolean isSilentMode(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = am.getRingerMode();
		return (ringerMode == AudioManager.RINGER_MODE_SILENT);
	}

	public static boolean isNotificationStreamVolZero(Context context)
	{
		return getCurrentVolume(context, AudioManager.STREAM_NOTIFICATION) > 0 ? false : true;
	}
	
}
