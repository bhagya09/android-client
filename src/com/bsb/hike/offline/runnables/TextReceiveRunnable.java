package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.IMessageSentOffline;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu, deepak malik
 *	Runnable responsible for receving text from client
 */
public class TextReceiveRunnable implements Runnable
{

	private static final String TAG = "OfflineThreadManager";

	int type;

	int msgSize;

	InputStream inputStream = null;

	private ServerSocket textServerSocket = null;

	private Socket textReceiverSocket = null;

	private OfflineManager offlineManager;
	
	IConnectCallback connectCallback;
	
	private IMessageSentOffline textCallback;
	
	private IMessageSentOffline fileCallback;

	File stickerImage = null;

	public TextReceiveRunnable(IMessageSentOffline textCallback, IMessageSentOffline fileCallback,IConnectCallback connectCallback)
	{
		this.textCallback = textCallback;
		this.fileCallback = fileCallback;
		this.connectCallback=connectCallback;
	}


	@Override
	public void run()
	{
		try
		{
			offlineManager = OfflineManager.getInstance();
			textServerSocket = new ServerSocket();
			textServerSocket.setReuseAddress(true);
			SocketAddress addr = new InetSocketAddress(PORT_TEXT_MESSAGE);
			textServerSocket.bind(addr);

			Logger.d(TAG, "TextReceiveThread" + "Will be waiting on accept");
			textReceiverSocket = textServerSocket.accept();
			Logger.d(TAG, "TextReceiveThread" + "Connection successfull and the receiver buffer is "+ textReceiverSocket.getReceiveBufferSize() + "and the send buffer is " + textReceiverSocket.getSendBufferSize() );
			connectCallback.onConnect();
			inputStream = textReceiverSocket.getInputStream();
			while (true)
			{				
				byte[] convMessageLength = new byte[4];
				int readBytes = inputStream.read(convMessageLength, 0, 4);

				msgSize = OfflineUtils.byteArrayToInt(convMessageLength);
				Logger.d(TAG, "Read Bytes is " + readBytes + "and msg Size is  " + msgSize);
				// Logger.d(TAG,"Msg size is "+msgSize);
				if (msgSize == 0)
				{
					throw new IOException();
				}
				byte[] msgJSON = new byte[msgSize];
				int fileSizeRead = msgSize;
				int offset = 0;
				while (msgSize > 0)
				{
					int len = inputStream.read(msgJSON, offset, msgSize);
					offset += len;
					msgSize -= len;
				}
				String msgString = new String(msgJSON, "UTF-8");
				Logger.d(TAG, "" + msgSize);
				JSONObject messageJSON = new JSONObject(msgString);
				Logger.d(TAG, "Message Received :-->" + msgString);

				// TODO: Ghost Packet Logic to come here
				// if ( isDisconnectPosted && !(isGhostPacket(messageJSON)) )
				// {
				// shouldBeDisconnected = false;
				// removeRunnable(waitingTimer);
				// isDisconnectPosted = true;
				// }

				ConvMessage convMessage = null;

				if (OfflineUtils.isPingPacket(messageJSON))
				{
					// Start client thread.
					offlineManager.setConnectedDevice(OfflineUtils.getMsisdnFromPingPacket(messageJSON));
					OfflineThreadManager.getInstance().startSendingThreads();
				}
				else if (OfflineUtils.isGhostPacket(messageJSON))
				{
					Logger.d(TAG, "Ghost Packet received");
					offlineManager.restartGhostTimeout(OfflineUtils.getScreenStatusFromGstPkt(messageJSON));
				}
				else if (OfflineUtils.isAckPacket(messageJSON))
				{
					messageJSON.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
					messageJSON.remove(HikeConstants.TO);
					Logger.d(TAG, "ACK PAcket received for msgId: " +  OfflineUtils.getMsgIdFromAckPacket(messageJSON));
					if (OfflineUtils.isAckForFileMessage(messageJSON))
						fileCallback.onSuccess(messageJSON);
					else
						textCallback.onSuccess(messageJSON);
				}
				else
				{
					messageJSON.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
					messageJSON.remove(HikeConstants.TO);

					if (OfflineUtils.isStickerMessage(messageJSON))
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
					else if (OfflineUtils.isChatThemeMessage(messageJSON))
					{
						// HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_THEME_CHANGE_MESSAGE, messageJSON);
						messageJSON.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
						MqttMessagesManager.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).saveChatBackground(messageJSON);
						continue;
					}
					else
					{
						// It's a normal Text Message
						Logger.d(TAG, "Connected deive sis " + offlineManager.getConnectedDevice());

					}
					convMessage = new ConvMessage(messageJSON, HikeMessengerApp.getInstance().getApplicationContext());
					long mappedMsgId = convMessage.getMappedMsgID();
					
					// send ack for the packet received
					JSONObject ackJSON = OfflineUtils.createAckPacket(convMessage.getMsisdn(), mappedMsgId, false);
					offlineManager.addToTextQueue(ackJSON);
					
					HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

				}
				// TODO:Handle ghost packet
				// if(isDisconnectPosted && type != 10)
				// {
				// shouldBeDisconnected = true;
				// disconnectAfterTimeout();
				// }
				offlineManager.setInOfflineFileTransferInProgress(false);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Exception in TextReceiveThread. IO Exception occured.Socket was not bounded");
			if (stickerImage != null)
				stickerImage.delete();
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Did we pass correct Address here ?? Server Socket did not bind.");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (OfflineException e)
		{
			e.printStackTrace();
			if (stickerImage != null)
			{
				Logger.d(TAG, "GOing to delete stickerImage in TRR");
				stickerImage.delete();
			}
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));

		}
	}
	
	
	public void shutDown()
	{
		try
		{
			OfflineUtils.closeSocket(textReceiverSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			OfflineUtils.closeSocket(textServerSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
		

}
