package com.bsb.hike.chatthread;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Anu Bansal on 09/03/16.
 */

public class KeyboardOffBoarding
{
	public interface KeyboardShutdownListener
	{
		void onDestroyed();
	}

	public static final String KEYBOARD_SHUTDOWN_STATE = "keyboardShutdownState";
	public static final int NOT_STARTED = 0;
	public static final int SHOWING = 1;
	public static final int SHOWN = 2;
	private int mState;

	private ViewGroup container;

	private Activity mActivity;

	private View rootView;

	private KeyboardShutdownListener keyboardShutdownListener;

	public KeyboardOffBoarding()
	{
		mState = HikeSharedPreferenceUtil.getInstance().getData(KEYBOARD_SHUTDOWN_STATE, NOT_STARTED);
	}

	public void init(Activity activity, LayoutInflater inflater, ViewGroup container, KeyboardShutdownListener listener)
	{
		this.mActivity = activity;
		this.container = container;
		this.keyboardShutdownListener = listener;
		rootView = inflater.inflate(R.layout.keyboard_off_boarding, container, false);
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Utils.blockOrientationChange(mActivity);
	}

	public void showView()
	{
		mState = SHOWING;
		updateState(mState);
		container.addView(rootView);
		rootView.findViewById(R.id.btn_phone_keyboard).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				destroy();
			}
		});
	}

	public boolean isShowing()
	{
		return (mState == SHOWING);
	}

	public boolean isOffBoardingComplete()
	{
		return (mState == SHOWN);
	}

	public boolean shouldShowKeyboardOffBoardingUI()
	{
		/*
		 *Localized keyboard is for india users only. Other users still have setting but do not see the FTUE
         */
		if(HikeMessengerApp.isIndianUser() && mState < SHOWN) {
			return true;
		}
		return false;
	}

	public void destroy()
	{
		mState = SHOWN;
		updateState(mState);
		container.removeAllViews();
		container.invalidate();
		if (keyboardShutdownListener != null)
			keyboardShutdownListener.onDestroyed();
	}

	private void updateState(int state)
    {
        mState = state;
        HikeSharedPreferenceUtil.getInstance().saveData(KEYBOARD_SHUTDOWN_STATE, state);
    }
}
