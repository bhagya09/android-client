package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu This class has all the utility methods to handle packets types involved in Offline Messaging.
 */
public class OfflineMessagesManager
{
	private Context context;

	private OfflineManager offlineManager;

	private OfflineThreadManager threadManager;

	private static final String TAG = "OfflineThreadManager";

	public OfflineMessagesManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();

		offlineManager = OfflineManager.getInstance();

		threadManager = OfflineThreadManager.getInstance();
	}

	public void handlePingPacket(JSONObject messageJSON)
	{
		// Start client thread.
		offlineManager.setConnectedDevice(OfflineUtils.getMsisdnFromPingPacket(messageJSON));
		threadManager.startSendingThreads();
	}

	public void handleGhostPacket(JSONObject messageJSON)
	{
		Logger.d(TAG, "Ghost Packet received");
		offlineManager.restartGhostTimeout(OfflineUtils.getScreenStatusFromGstPkt(messageJSON));
	}

	public void handleAckPacket(JSONObject messageJSON, IMessageSentOffline fileCallback, IMessageSentOffline textCallback) throws JSONException
	{
		messageJSON.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
		messageJSON.remove(HikeConstants.TO);
		Logger.d(TAG, "ACK PAcket received for msgId: " + OfflineUtils.getMsgIdFromAckPacket(messageJSON));

		if (OfflineUtils.isAckForFileMessage(messageJSON))
		{
			fileCallback.onSuccess(messageJSON);
		}
		else
		{
			textCallback.onSuccess(messageJSON);
		}
	}

	public void handleSpaceCheckPacket(JSONObject messageJSON)
	{
		offlineManager.addToTextQueue(OfflineUtils.createSpaceAck(messageJSON));
	}

	public void handleSpaceAckPacket(JSONObject messageJSON)
	{
		offlineManager.canSendFile(messageJSON);
	}

}
