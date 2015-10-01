package com.bsb.hike.service;

import java.util.HashSet;
import java.util.Set;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.ServiceConnection;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.Intent;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeConstants.ChatHead;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.ChatHeadViewManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class HikeAccessibilityService extends AccessibilityService
{
	private final String TAG = "HikeAccessService";
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		ChatHeadViewManager.getInstance(this).onConfigChanged();
	}
	
	@Override
	public void onDestroy()
	{
		ChatHeadViewManager.getInstance(this).onDestroy();;
		super.onDestroy();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Logger.d(TAG, "onCreate");
		
		ChatHeadViewManager.getInstance(this).onCreate();
	}
	
	private String getEventType(AccessibilityEvent event)
	{

		String value = event.getEventType() + " ";
		CharSequence packageName = event.getPackageName();
		switch (event.getEventType())
		{
		case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
			return value + "TYPE_NOTIFICATION_STATE_CHANGED";
		case AccessibilityEvent.TYPE_VIEW_FOCUSED:
			return value + "TYPE_VIEW_FOCUSED";
		case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
			return value + "TYPE_VIEW_LONG_CLICKED";
		case AccessibilityEvent.TYPE_VIEW_SELECTED:
			return value + "TYPE_VIEW_SELECTED";
		case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
		case AccessibilityEvent.TYPE_VIEW_CLICKED:
		case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
			String currentKeyboard =  Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
			boolean keyboardOpen = !TextUtils.isEmpty(currentKeyboard) ? currentKeyboard.contains(packageName) || TextUtils.isEmpty(packageName) : false;

			Logger.d("UmangX",currentKeyboard  +  " " + packageName + " " + keyboardOpen);
			
			// some keyboards do not display their names, like korean keyboard
			String hikePackage = HikeMessengerApp.getInstance().getPackageName();
			boolean hikeIsOpen = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS).contains(hikePackage);
			boolean chatHeadStickerPickerIsOpen = getEventText(event).equals("hike") || packageName.equals(hikePackage);
			boolean snoozed = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SNOOZE, false);

			if(hikeIsOpen || !( chatHeadStickerPickerIsOpen || keyboardOpen || snoozed))
			{
				Set<String> packages = new HashSet<String>(1);
				packages.add( event.getPackageName().toString());
				ChatHeadViewManager.getInstance(this).actionWindowChange(packages);
			}
			return value + "TYPE_WINDOW_CONTENT-STATE_CHANGED or TYPE_VIEW_CLICKED";
			
		case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
			return value + "TYPE_VIEW_TEXT_CHANGED";
		}
		return value + "default";
	}

	private String getEventText(AccessibilityEvent event)
	{
		StringBuilder sb = new StringBuilder();
		for (CharSequence s : event.getText())
		{
			sb.append(s);
		}
		return sb.toString();
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event)
	{
		Logger.d(TAG, String.format("onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s", getEventType(event), event.getClassName(), event.getPackageName(),
				event.getEventTime(), getEventText(event)));
	}

	@Override
	public void onInterrupt()
	{
		Logger.d(TAG, "onInterrupt");
	}

	@Override
	public void unbindService(ServiceConnection conn)
	{
		Logger.d(TAG,"Unbinding service");
		super.unbindService(conn);
	}
	
	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags)
	{
		Logger.d(TAG,"binding service");
		return super.bindService(service, conn, flags);
	}
	
	@Override
	protected void onServiceConnected()
	{
		super.onServiceConnected();
		Logger.d(TAG, "onServiceConnected");
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.flags = AccessibilityServiceInfo.DEFAULT;
		info.notificationTimeout = 100;
		info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		setServiceInfo(info);
		ChatHeadUtils.startOrStopService(false);
	}

}
