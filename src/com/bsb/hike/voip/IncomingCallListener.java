package com.bsb.hike.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.view.VoIPActivity;

public class IncomingCallListener extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		// Android bug
		// EXTRA_STATE_RINGING can be called twice sometimes
		if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
			// We have an incoming call
			if (VoIPService.isConnected()) {
				// We are in a call
				// Put it on hold
				Logger.d(VoIPConstants.TAG, "Detected incoming call. Putting VoIP on hold.");
				Intent i = new Intent(context, VoIPActivity.class);
				i.putExtra("action", VoIPConstants.INCOMING_NATIVE_CALL_HOLD);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
		}
	}
}
