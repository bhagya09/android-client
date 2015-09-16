package com.hike.transporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.TConstants.State;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu/Gaurav
 * 
 * We listen for ScreenOn and Off events so that we can communicate the same to the other device.that we can go to
 * sleep anytime soon
 *
 */
public class ScreenToggleReceiver extends BroadcastReceiver
{

	private Config config;

	public ScreenToggleReceiver(Config config)
	{
		this.config = config;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			Utils.setScreenStatus(true);
			SenderConsignment senderConsignment = Utils.createHeartBeatPacket(config.getAckTopic());
			if (Transporter.getInstance().getState() == State.CONNECTED)
			{
				Transporter.getInstance().publish(senderConsignment);
			}
		}
		else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Utils.setScreenStatus(false);
			SenderConsignment senderConsignment = Utils.createHeartBeatPacket(config.getAckTopic());
			if (Transporter.getInstance().getState() == State.CONNECTED)
			{
				Transporter.getInstance().publish(senderConsignment);
			}
		}
	}

}
