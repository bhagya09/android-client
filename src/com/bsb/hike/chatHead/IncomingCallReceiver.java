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

	public static boolean callReceived = false;
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			StickyCaller.callCurrentNumber = incomingNumber; 
			ring = true;
			StickyCaller.CALL_TYPE = StickyCaller.INCOMING;
			ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), incomingNumber);
			break;

		case TelephonyManager.CALL_STATE_OFFHOOK:
			callReceived = true;
			if (StickyCaller.CALL_TYPE == StickyCaller.OUTGOING)
			{
				StickyCaller.removeCallerViewWithDelay(StickyCaller.OUTGOING_DELAY);
			}
			else if (StickyCaller.CALL_TYPE == StickyCaller.INCOMING)
			{
				StickyCaller.removeCallerViewWithDelay(StickyCaller.INCOMING_DELAY);
			}
			break;

		case TelephonyManager.CALL_STATE_IDLE:
			StickyCaller.removeCallerView();
			StickyCaller.CALL_TYPE = StickyCaller.NONE; 
			// missed call block
			if (ring == true && callReceived == false)
			{   
				StickyCaller.CALL_TYPE = StickyCaller.MISSED; 
				StickyCaller.MISSED_CALL_TIMINGS = ChatHeadUtils.getdateFromSystemTime();
				ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance(), StickyCaller.callCurrentNumber);
			}
			
			if (StickyCaller.toCall && StickyCaller.callCurrentNumber !=  null)
			{
				ChatHeadUtils.onCallClickedFromCallerCard(HikeMessengerApp.getInstance().getApplicationContext(), StickyCaller.callCurrentNumber, VoIPUtils.CallSource.HIKE_STICKY_CALLER);
				StickyCaller.toCall = false;
			}
			callReceived = false;
			ring = false;
			break;
		}
	}

}