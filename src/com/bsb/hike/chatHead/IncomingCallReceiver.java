package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;

public class IncomingCallReceiver extends PhoneStateListener
{
	public static boolean wasRinging;

	private String TAG = "INCOMING_CALL_RECEIVER";

	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			wasRinging = true;
			ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), incomingNumber);
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			wasRinging = true;
			break;
		case TelephonyManager.CALL_STATE_IDLE:
			wasRinging = false;
			StickyCaller.removeCallerView();
			break;
		}
	}
}