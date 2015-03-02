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
	/**
	 * Message sending sound is not played under following conditions
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
				&& !isSilentMode(context));
	}

	/**
	 * Plays non-ducking sound on Music Stream from given file inside raw folder. 
	 * 
	 * @param context
	 * @param soundId
	 */
	public static void playSoundFromRaw(final Context context, int soundId)
	{
		Logger.i("sound", "playing sound " + soundId);

		// define sound URI, the sound to be played when there's a notification
		Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
		try
		{
			Ringtone r = RingtoneManager.getRingtone(context, soundUri);
		    r.setStreamType(AudioManager.STREAM_MUSIC);
		    r.play();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Plays non-ducking sound on Music Stream from default tone. 
	 * @param context
	 */
	public static void playDefaultNotificationSound(Context context)
	{
		try
		{
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.setStreamType(AudioManager.STREAM_MUSIC);
			r.play();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Plays non-ducking sound on Music Stream from given Uri. 
	 * 
	 * @param context
	 * @param soundUri
	 */
	public static void playSound(final Context context, Uri soundUri)
	{
		try
		{
			Ringtone r = RingtoneManager.getRingtone(context, soundUri);
			r.setStreamType(AudioManager.STREAM_MUSIC);
			r.play();
		}
		catch (Exception e)
		{
			e.printStackTrace();
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

	public static boolean isSilentMode(Context context)
	{
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = am.getRingerMode();
		return (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE);
	}
	
}
