package com.bsb.hike.chatHead;

import com.bsb.hike.utils.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChatHeadReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			{
				ChatHeadUtils.stopService(context);
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				ChatHeadUtils.serviceDecision(context,false);
			}
	}
}