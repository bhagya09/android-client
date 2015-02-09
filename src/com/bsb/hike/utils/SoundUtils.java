package com.bsb.hike.utils;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;

public class SoundUtils
{
	private static int systemStreamVol;
	
	private static Handler soundHandler = new Handler(Looper.getMainLooper());

	private static MediaPlayer mediaPlayer = new MediaPlayer();

	private static Runnable stopSoundRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			mediaPlayer.reset();
			setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};

	private static MediaPlayer.OnCompletionListener completeListener = new MediaPlayer.OnCompletionListener()
	{

		@Override
		public void onCompletion(MediaPlayer mp)
		{
			mediaPlayer.reset();
			soundHandler.removeCallbacks(stopSoundRunnable);
			setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};

	private static MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener()
	{

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra)
		{
			Logger.e("SoundUtils", "MediaPlayer -- OnERROR!!! WHAT:: " + what + " EXTRAS:: " + extra);
			mediaPlayer.reset();
			setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
			return true;
		}
	};

	private static Runnable resetSystemStreamVolRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	};
	
	public static boolean isTickSoundEnabled(Context context)
	{
		return (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.TICK_SOUND_PREF, true) && !Utils.isUserInAnyTypeOfCall(context) && !isAnyMusicPlaying(context));
	}

	/**
	 * we are using stream_ring so that use can control volume from mobile and this stream is not in use when user is chatting and vice-versa
	 * 
	 * @param context
	 * @param soundId
	 */
	public static void playSoundFromRaw(final Context context, int soundId)
	{
		Logger.i("sound", "playing sound " + soundId);

		// remove any previous handler
		soundHandler.removeCallbacks(stopSoundRunnable);

		// resetting media player
		mediaPlayer.reset();

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
		setSystemStreamVolToNotifStreamVol(context);
		Resources res = context.getResources();
		AssetFileDescriptor afd = res.openRawResourceFd(soundId);

		try
		{
			mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			afd.close();

			mediaPlayer.setOnCompletionListener(completeListener);
			mediaPlayer.setOnErrorListener(errorListener);
			mediaPlayer.prepare();
			mediaPlayer.start();

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
	}

	public static void playDefaultNotificationSound(Context context)
	{
		try
		{
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.setStreamType(AudioManager.STREAM_SYSTEM);
			setSystemStreamVolToNotifStreamVol(context);
			r.play();
			soundHandler.postDelayed(resetSystemStreamVolRunnable, HikeConstants.STOP_NOTIF_SOUND_TIME);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
		}
	}

	/**
	 * Plays non-ducking sound from given Uri. Plays on {@link android.Media.AudioManager#STREAM_SYSTEM AudioManager.STREAM_SYSTEM} to enable non-ducking playback.
	 * 
	 * @param context
	 * @param soundUri
	 */
	public static void playSound(final Context context, Uri soundUri)
	{
		// remove any previous handler
		soundHandler.removeCallbacks(stopSoundRunnable);

		// resetting media player
		mediaPlayer.reset();

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
		setSystemStreamVolToNotifStreamVol(context);
		
		try
		{
			mediaPlayer.setDataSource(context, soundUri);

			mediaPlayer.setOnCompletionListener(completeListener);
			mediaPlayer.setOnErrorListener(errorListener);
			mediaPlayer.prepare();
			mediaPlayer.start();
			soundHandler.postDelayed(stopSoundRunnable, HikeConstants.STOP_NOTIF_SOUND_TIME);

		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			stopMediaPlayerProperly();
		}
	}

	/**
	 * This sets vol of SYSTEM_STREAM to vol of STREAM_NOTIFICATION
	 * @param context
	 */
	public static void setSystemStreamVolToNotifStreamVol(Context context)
	{
		systemStreamVol = getCurrentVolume(context, AudioManager.STREAM_SYSTEM);
		int notifVol = getCurrentVolume(context, AudioManager.STREAM_NOTIFICATION);
		setCurrentVolume(context, AudioManager.STREAM_SYSTEM, notifVol);
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

	private static void stopMediaPlayerProperly()
	{
		soundHandler.removeCallbacks(stopSoundRunnable);

		mediaPlayer.reset();
		
		setCurrentVolume(HikeMessengerApp.getInstance().getApplicationContext(), AudioManager.STREAM_SYSTEM, systemStreamVol);
	}
}
