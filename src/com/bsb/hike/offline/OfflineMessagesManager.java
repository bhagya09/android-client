package com.bsb.hike.offline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.service.MqttMessagesManager;
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

	public void handleStickerMessage(JSONObject messageJSON,File stickerImage,InputStream inputStream) throws OfflineException,IOException,JSONException
	{
		String stpath = OfflineUtils.getStickerPath(messageJSON);
		stickerImage = new File(stpath);
		if (!stickerImage.exists())
		{
			OfflineUtils.createStkDirectory(messageJSON);
			FileOutputStream outputStream = new FileOutputStream(stickerImage);
			OfflineUtils.copyFile(inputStream, outputStream, OfflineUtils.getStkLenFrmPkt(messageJSON));
			OfflineUtils.closeOutputStream(outputStream);
		}
		// remove data from stream
		else
		{
			long fileSize = OfflineUtils.getStkLenFrmPkt(messageJSON);
			while (fileSize > 0)
			{
				long len = inputStream.skip(fileSize);
				fileSize -= len;
			}
		}
		// set stickerImage to null, to avoid deleting it if download is complete
		stickerImage = null;  
	}

	public void handleChatThemeMessage(JSONObject messageJSON) throws JSONException
	{
		messageJSON.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
		MqttMessagesManager.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).saveChatBackground(messageJSON);
	}

}
