package com.bsb.hike.offline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeOfflinePersistence;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.runnables.FileReceiverRunnable;
import com.bsb.hike.offline.runnables.FileTransferRunnable;
import com.bsb.hike.offline.runnables.TextReceiveRunnable;
import com.bsb.hike.offline.runnables.TextTransferRunnable;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu, deepak malik This class mainly deals with socket connection,text and file are send and received in this class only.
 */
public class OfflineThreadManager
{

	private static OfflineThreadManager _instance = new OfflineThreadManager();

	private final static String TAG = getInstance().getClass().getName();

	private TextTransferRunnable textTransferRunnable = null;

	private Thread fileTransferThread = null;

	private FileTransferRunnable fileTransferRunnable = null;

	private OfflineManager offlineManager = null;

	private Thread textReceiveThread = null;

	private TextReceiveRunnable textReceiveRunnable = null;

	private Thread fileReceiverThread = null;

	private FileReceiverRunnable fileReceiverRunnable;

	private Thread textTransferThread;

	AtomicInteger threadConnectCount = new AtomicInteger();

	public static OfflineThreadManager getInstance()
	{
		return _instance;
	}

	private OfflineThreadManager()
	{
		textTransferRunnable = new TextTransferRunnable(textMessageCallback, connectCallback);
		fileTransferRunnable = new FileTransferRunnable(fileMessageCallback, connectCallback);
		textReceiveRunnable = new TextReceiveRunnable(textMessageCallback, fileMessageCallback, connectCallback);
		fileReceiverRunnable = new FileReceiverRunnable(connectCallback);

	}

	private IConnectCallback connectCallback = new IConnectCallback()
	{

		@Override
		public void onDisconnect(OfflineException e)
		{
			offlineManager.shutDown(e);
		}

		@Override
		public void onConnect()
		{
			Logger.d(TAG, "onConnect() called with value " + threadConnectCount.get());
			if (threadConnectCount.incrementAndGet() == OfflineConstants.ALL_THREADS_CONNECTED)
			{
				// all threads connected
				offlineManager.onConnected();
			}

		}
	};

	public void startSendingThreads()
	{
		startTextSendingThread();

		startFileSendingThread();
	}

	public void startTextSendingThread()
	{
		offlineManager = OfflineManager.getInstance();
		textTransferThread = new Thread(textTransferRunnable);
		textTransferThread.start();
	}

	public void startFileSendingThread()
	{
		offlineManager = OfflineManager.getInstance();
		fileTransferThread = new Thread(fileTransferRunnable);
		fileTransferThread.start();
	}

	public void startReceivingThreads()
	{
		offlineManager = OfflineManager.getInstance();
		textReceiveThread = new Thread(textReceiveRunnable);
		textReceiveThread.start();

		fileReceiverThread = new Thread(fileReceiverRunnable);
		fileReceiverThread.start();

	}

	/**
	 * 
	 * @param packet
	 * @param outputStream
	 * @return TODO: Properly handle sticker and normal message and update the Db
	 * @throws IOException
	 */

	public boolean sendOfflineText(JSONObject packet, OutputStream outputStream) throws IOException, OfflineException
	{
		String fileUri = null;
		InputStream inputStream = null;
		boolean isSent = false;
		Logger.d(TAG, "Goiing to send ext :::::" + packet.toString());
		if (OfflineUtils.isStickerMessage(packet))
		{
			fileUri = OfflineUtils.getStickerPath(packet);
			File f = new File(fileUri);
			OfflineUtils.putStkLenInPkt(packet, f.length());
			Logger.d(TAG, "Middle " + f.getPath());
			try
			{
				inputStream = new FileInputStream(fileUri);
			}
			catch (FileNotFoundException e)
			{
				Logger.d(TAG, e.toString());
				return false;
			}

			byte[] messageBytes = packet.toString().getBytes("UTF-8");
			int length = messageBytes.length;
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.write(intToBArray, 0, intToBArray.length);
			outputStream.write(messageBytes, 0, length);
			// copy the sticker to the stream
			isSent = OfflineUtils.copyFile(inputStream, outputStream, f.length());
			inputStream.close();
		}
		// for normal text messages and ping packet
		else
		{
			byte[] messageBytes = packet.toString().getBytes("UTF-8");
			int length = messageBytes.length;
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.write(intToBArray, 0, intToBArray.length);
			outputStream.write(messageBytes, 0, length);
			isSent = true;
		}
		
		if(OfflineUtils.isDisconnectPkt(packet))
		{
			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			throw new OfflineException(OfflineException.DISCONNECT);
		}
		// Updating database
		return isSent;
	}

	public boolean sendOfflineFile(FileTransferModel fileTransferModel, OutputStream outputStream) throws IOException, OfflineException
	{
		boolean isSent = false;
		String fileUri = null;
		InputStream inputStream = null;
		JSONObject jsonFile = null;
		try
		{
			JSONArray jsonFiles = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			jsonFile = (JSONObject) jsonFiles.get(0);
			fileUri = jsonFile.getString(HikeConstants.FILE_PATH);

			String metaString = fileTransferModel.getPacket().toString();
			Logger.d(TAG, metaString);

			byte[] metaDataBytes = metaString.getBytes("UTF-8");
			int length = metaDataBytes.length;
			Logger.d(TAG, "Sizeof metaString: " + length);
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.write(intToBArray, 0, intToBArray.length);

			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(metaDataBytes);
			boolean isMetaDataSent = OfflineUtils.copyFile(byteArrayInputStream, outputStream, metaDataBytes.length);
			Logger.d(TAG, "FileMetaDataSent:" + isMetaDataSent);
			byteArrayInputStream.close();

			int fileSize = 0;
			fileSize = jsonFile.getInt(HikeConstants.FILE_SIZE);

			long msgID;
			msgID = fileTransferModel.getMessageId();

			// TODO:We can listen to PubSub ...Why to do this ...????
			// showUploadTransferNotification(msgID,fileSize);

			inputStream = new FileInputStream(new File(fileUri));
			long time = System.currentTimeMillis();
			isSent = OfflineUtils.copyFile(inputStream, outputStream, fileTransferModel, true, true, fileSize);
			// in seconds
			long TimeTaken = (System.currentTimeMillis() - time) / 1000;
			if (TimeTaken > 0)
				Logger.d(TAG, "Time taken to send file is " + TimeTaken + "Speed is " + fileSize / (1024 * 1024 * TimeTaken));
			inputStream.close();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}

		return isSent;

	}

	/**
	 * Handle the call of file message here ...either success or failure
	 */
	private IMessageSentOffline textMessageCallback = new IMessageSentOffline()
	{
		@Override
		public void onSuccess(JSONObject packet)
		{
			//if (!OfflineUtils.isGhostPacket(packet) && !OfflineUtils.isPingPacket(packet))
			if (OfflineUtils.isAckPacket(packet))
			{
				long msgId;
				try
				{
					//msgId = packet.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
					msgId = OfflineUtils.getMsgIdFromAckPacket(packet);
					String msisdn = packet.getString(HikeConstants.FROM);
					long startTime = System.currentTimeMillis();
					int rowsUpdated = OfflineUtils.updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
					Logger.d(TAG, "Time  taken: " + (System.currentTimeMillis() - startTime));
					if (rowsUpdated == 0)
					{
						Logger.d(getClass().getSimpleName(), "No rows updated");
					}
					Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
					Logger.d(TAG, "Message Delivered Successfully");
					HikeOfflinePersistence.getInstance().removeMessage(msgId);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onFailure(JSONObject packet)
		{
			Logger.d(TAG, "Message sending failed");
		}
	};

	private IMessageSentOffline fileMessageCallback = new IMessageSentOffline()
	{
		@Override
		public void onSuccess(JSONObject packet)
		{
			long msgID = -1;
			String msisdn = null;
			try
			{
			msgID = OfflineUtils.getMsgIdFromAckPacket(packet);
			msisdn = packet.getString(HikeConstants.FROM);
			offlineManager.removeFromCurrentSendingFile(msgID);

			// Update Delivered status
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
			int rowsUpdated = OfflineUtils.updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);
			if (rowsUpdated == 0)
			{
				Logger.d(getClass().getSimpleName(), "No rows updated");
			}
			Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
			HikeOfflinePersistence.getInstance().removeMessage(msgID);
		}

		@Override
		public void onFailure(JSONObject packet)
		{
			Logger.d(TAG, "File sending failed");
		}

	};

	public void shutDown()
	{
		Logger.d(TAG, "Goining to close ALL SOCKETS");
		threadConnectCount.set(0);
		textTransferRunnable.shutDown();

		Logger.d(TAG, "closing  text socket ALL SOCKETS");
		fileTransferRunnable.shutDown();

		fileReceiverRunnable.shutDown();

		textReceiveRunnable.shutDown();

		interrupt(textTransferThread);
		interrupt(fileTransferThread);
		interrupt(textReceiveThread);
		interrupt(fileReceiverThread);
	}

	private void interrupt(Thread t)
	{
		if (t == null)
			return;
		if (t.isAlive())
		{
			t.interrupt();
		}
	}

}
