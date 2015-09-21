package com.bsb.hike.chatHead;

import java.lang.reflect.Method;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;
import com.bsb.hike.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class IncomingCallReceiver extends PhoneStateListener
{
	private String TAG = "INCOMING_CALL_RECEIVER";

	private static boolean ring = false;

	private static boolean callReceived = false;

	
	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			ring = true;
			StickyCaller.isOnCall =true;
			ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), incomingNumber);
			break;

		case TelephonyManager.CALL_STATE_OFFHOOK:
			callReceived = true;
			break;

		case TelephonyManager.CALL_STATE_IDLE:
			StickyCaller.removeCallerView();
			StickyCaller.isOnCall = false; 
			// missed call block
			if (ring == true && callReceived == false)
			{   
			//	ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), incomingNumber);
			}
			
			if (StickyCaller.toCall && StickyCaller.callCurrentNumber !=  null)
			{
				Utils.onCallClicked(HikeMessengerApp.getInstance(), StickyCaller.callCurrentNumber, VoIPUtils.CallSource.HIKE_STICKY_CALLER);
				StickyCaller.toCall = false;
			}
			callReceived = false;
			ring = false;
			break;
		}
	}

}