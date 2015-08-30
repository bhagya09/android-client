package com.bsb.hike.service;

import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.utils.Logger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.accessibility.AccessibilityEvent;

public class HikeAccessibilityService extends AccessibilityService
{

	static final String TAG = "HikeAccessibilitySerivce";

	private String getEventType(AccessibilityEvent event)
	{

		String value = event.getEventType() + " ";
		switch (event.getEventType())
		{
		case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
			return value + "TYPE_NOTIFICATION_STATE_CHANGED";
		case AccessibilityEvent.TYPE_VIEW_CLICKED:
			return value + "TYPE_VIEW_CLICKED";
		case AccessibilityEvent.TYPE_VIEW_FOCUSED:
			return value + "TYPE_VIEW_FOCUSED";
		case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
			return value + "TYPE_VIEW_LONG_CLICKED";
		case AccessibilityEvent.TYPE_VIEW_SELECTED:
			return value + "TYPE_VIEW_SELECTED";
		case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
			return value + "TYPE_WINDOW_STATE_CHANGED";
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
//		Logger.d(TAG, String.format("onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s", getEventType(event), event.getClassName(), event.getPackageName(),
//				event.getEventTime(), getEventText(event)));
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
	public void onDestroy()
	{
		Logger.d(TAG,"detroying service");
		ChatHeadUtils.startOrStopService(false);
		super.onDestroy();
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
		info.loadDescription(getPackageManager());
		setServiceInfo(info);
		ChatHeadUtils.startOrStopService(false);
	}

}
