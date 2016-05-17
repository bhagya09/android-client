package com.bsb.hike.utils;

import android.app.Activity;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.HomeFtueActivity;
import com.bsb.hike.ui.NUXInviteActivity;

public class HikeAppStateUtils
{

	private static final String TAG = "HikeAppState";

	public static void onCreate(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was opened. Sending packet");
			HikeMessengerApp.currentState = CurrentState.OPENED;
			Utils.appStateChanged(activity.getApplicationContext());
		}

	}

	public static void onResume(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "onResume called");

		if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED)
		{
			// if back is pressed OR finish is called on an activity B
			// and the activity stack has another activity A lying below activity B
			// then at least onResume of activity A will be called
			// where we change the state to OLD_ACTIVITY (onStart might not be called for partially visible A)
			HikeMessengerApp.currentState = CurrentState.OLD_ACTIVITY;
		}
		else if(HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY)
		{
			HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY_INTERNAL;
		}
	}

	public static void onStart(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "onStart called.");
		handleResumeOrStart(activity);
	}

	private static void handleResumeOrStart(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was resumed. Sending packet");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
	}

	public static void onRestart(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "App was restarted.");
		handleRestart(activity);
	}

	private static void handleRestart(Activity activity)
	{
		/*
		 * This code was added for the case when an activity is opened when the app is in the background. Here we check is the screen is on and if it is, we send an fg packet.
		 */
		if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY_IN_BG)
		{
			boolean isScreenOn = Utils.isScreenOn(activity.getApplicationContext());
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was restarted. Is Screen on? " + isScreenOn);
			if (!isScreenOn)
			{
				return;
			}
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was restarted. Sending packet");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
	}

	public static void onBackPressed()
	{
		Logger.d(TAG, "onBackPressed called");
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
	}

	public static void onStop(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "OnStop");
		handlePauseOrStop(activity);
	}

	private static void handlePauseOrStop(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED)
		{
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was closed");
			HikeMessengerApp.currentState = CurrentState.CLOSED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
		else
		{
			if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY_INTERNAL || HikeMessengerApp.currentState == CurrentState.OLD_ACTIVITY)
			{
				Logger.d(TAG, "App was going to another activity new or a previous one through finish");
				HikeMessengerApp.currentState = CurrentState.RESUMED;
			}
			else if(HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY)
			{
				HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
				Logger.d(TAG,"New Activity is an external activity so sending BG packet, but not closing hidden mode");
				Utils.appStateChanged(activity.getApplicationContext(), false, false);
			}

			else if (HikeMessengerApp.currentState != CurrentState.BACKGROUNDED && HikeMessengerApp.currentState != CurrentState.CLOSED
					&& HikeMessengerApp.currentState != CurrentState.NEW_ACTIVITY_IN_BG)
			{
				if (activity.isChangingConfigurations())
				{
					Logger.d(TAG, "App was going to another activity");
					HikeMessengerApp.currentState = CurrentState.RESUMED;
					return;
				}
				Logger.d(TAG + activity.getClass().getSimpleName(), "App was backgrounded. Sending packet");
				HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
				Utils.appStateChanged(activity.getApplicationContext(), true, true);
			}
		}
	}

	public static void finish()
	{
		//this check it to prevent app in going to a back_pressed state from a BG/Closed state
		//TODO : making a better FSM for state handling where transitions from one State to another are pre defined on Actions
		if(HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY || HikeMessengerApp.currentState == CurrentState.OPENED 
				|| HikeMessengerApp.currentState == CurrentState.RESUMED)
		{
			HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
		}
	}

	public static void startActivityForResult(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "startActivityForResult. Previous state: " + HikeMessengerApp.currentState);


		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY_IN_BG;
		}
		else
		{
			HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY;
		}
	}

	public static void onActivityResult(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED)
		{
			Logger.d(TAG + activity.getClass().getSimpleName(), "App returning from activity with result. Sending packet");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
	}

}
