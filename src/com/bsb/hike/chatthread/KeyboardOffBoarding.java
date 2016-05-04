package com.bsb.hike.chatthread;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Anu Bansal on 09/03/16.
 */

public class KeyboardOffBoarding
{
	public interface KeyboardShutdownListener
	{
		void onShown();

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

	private View mainView;

	private KeyboardShutdownListener keyboardShutdownListener;

	public KeyboardOffBoarding()
	{
		mState = HikeSharedPreferenceUtil.getInstance().getData(KEYBOARD_SHUTDOWN_STATE, NOT_STARTED);
	}

	public void init(Activity activity, LayoutInflater inflater, ViewGroup container, KeyboardShutdownListener listener, View mainView)
	{
		this.mActivity = activity;
		this.container = container;
		this.keyboardShutdownListener = listener;
		this.mainView = mainView;
		rootView = inflater.inflate(R.layout.keyboard_off_boarding, container, false);
		setUIText(rootView);
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	private void setUIText(View rootView) {

		String bodyText = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPT_EXIT_SERVER_TEXT, null);
		if (bodyText != null) {
			TextView textView = (TextView)(rootView.findViewById(R.id.kpt_exit_text));
			textView.setText(bodyText);
		}

		String heading = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPT_EXIT_HEADING, null);
		if (heading != null) {
			((TextView)(rootView.findViewById(R.id.kpt_exit_heading))).setText(heading);
		}

		String phoneBtnText = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPT_EXIT_PHONE_BUTTON, null);
		if (phoneBtnText != null) {
			((TextView)(rootView.findViewById(R.id.btn_phone_keyboard))).setText(phoneBtnText);
		}

		String googleBtnTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPT_EXIT_GOOGLE_BUTTON, null);
		if (googleBtnTxt != null) {
			((TextView)(rootView.findViewById(R.id.btn_google_keyboard))).setText(googleBtnTxt);
		}

	}

	public boolean showView() {
		if (mActivity != null) {

			mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			mState = SHOWING;
			updateState(mState);
			if(container.getChildCount() == 0) {
				container.addView(rootView);
			}
			int rootViewHeight = (int) (mActivity.getResources().getDimension(R.dimen.keyboard_exit_ui));
			updatePadding(rootViewHeight);

			if (keyboardShutdownListener != null) {
				keyboardShutdownListener.onShown();
			}

			rootView.findViewById(R.id.btn_phone_keyboard).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					destroy();
					trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_EXIT_UI_OPEN_KEYBOARD);
				}
			});

			rootView.findViewById(R.id.btn_google_keyboard).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					final String appPackageName = "com.google.android.apps.inputmethod.hindi&hl=en";
					try {
						mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
					} catch (android.content.ActivityNotFoundException anfe) {
						mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
					}
					destroy();
					trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_EXIT_UI_PLAYSTORE_BUTTON);
				}
			});

			return true;
		}

		return false;
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
		if (!(showKptExitUI())) {
			return false;
		}

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
		updatePadding(0);
		mState = SHOWN;
		updateState(mState);
		if(container != null) {
			container.removeAllViews();
			container.invalidate();
		}
		if (keyboardShutdownListener != null)
			keyboardShutdownListener.onDestroyed();
	}

	public void hide()
	{
		updatePadding(0);
		mState = NOT_STARTED;
		updateState(mState);
		Utils.unblockOrientationChange(mActivity);
		if(container != null) {
			container.removeAllViews();
			container.invalidate();
		}
	}

	private void updateState(int state)
    {
        mState = state;
        HikeSharedPreferenceUtil.getInstance().saveData(KEYBOARD_SHUTDOWN_STATE, state);
    }

	private void updatePadding(int bottomPadding)
	{
		if (mainView != null && mainView.getPaddingBottom() != bottomPadding)
		{
			mainView.setPadding(0, 0, 0, bottomPadding);
		}
	}

	/*
     * This method is to track the analytic events on various UI clicks
     */
	private void trackClickAnalyticEvents(String event)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, event);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
	}

	private static boolean kptExitServerSwitch() {
//		server-side switch
		boolean show = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPT_EXIT_SERVER_SWITCH, false);
		return show;
	}

	private static boolean kptKeyboardSet() {

		boolean kptKeyboardSet = !(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KPTConstants.SYSTEM_KEYBOARD_SELECTED, true));
		return kptKeyboardSet;
	}

	private boolean showKptExitUI() {

		return kptExitServerSwitch() || kptKeyboardSet();
	}



}
