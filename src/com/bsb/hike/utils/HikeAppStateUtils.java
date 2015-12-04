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
		if (!Utils.isHoneycombOrHigher())
		{
			if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY_IN_BG)
			{
				handleRestart(activity);
			}
			else
			{
				handleResumeOrStart(activity);
			}
		}
	}

	public static void onStart(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "onStart called.");
		if (Utils.isHoneycombOrHigher())
		{
			handleResumeOrStart(activity);
		}
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
		if (Utils.isHoneycombOrHigher())
		{
			handleRestart(activity);
		}
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

	public static void onPause(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "onPause called");
		if (!Utils.isHoneycombOrHigher())
		{
			handlePauseOrStop(activity);
		}
	}

	public static void onStop(Activity activity)
	{
		Logger.d(TAG + activity.getClass().getSimpleName(), "OnStop");
		if (Utils.isHoneycombOrHigher())
		{
			handlePauseOrStop(activity);
		}

	}

	private static void handlePauseOrStop(Activity activity)
	{
		if ((HikeMessengerApp.currentState == CurrentState.BACK_PRESSED) && (activity instanceof HomeActivity
				|| activity instanceof NUXInviteActivity||activity instanceof HomeFtueActivity))
		{
			Logger.d(TAG + activity.getClass().getSimpleName(), "App was closed");
			HikeMessengerApp.currentState = CurrentState.CLOSED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
		else
		{
			if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY)
			{
				Logger.d(TAG, "App was going to another activity");
				HikeMessengerApp.currentState = CurrentState.RESUMED;
			}
			else if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED)
			{
				//case when back is pressed but the user was not on HomeActivity (still inside the app)
				HikeMessengerApp.currentState = CurrentState.RESUMED;
			}
			else if (HikeMessengerApp.currentState != CurrentState.BACKGROUNDED && HikeMessengerApp.currentState != CurrentState.CLOSED
					&& HikeMessengerApp.currentState != CurrentState.NEW_ACTIVITY_IN_BG)
			{
				if (Utils.isHoneycombOrHigher() && activity.isChangingConfigurations())
				{
					Logger.d(TAG, "App was going to another activity");
					HikeMessengerApp.currentState = CurrentState.RESUMED;
					return;
				}
				Logger.d(TAG + activity.getClass().getSimpleName(), "App was backgrounded. Sending packet");
				HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
				Utils.appStateChanged(activity.getApplicationContext(), true, Utils.isHoneycombOrHigher());
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
