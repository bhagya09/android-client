package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.PORT_FILE_TRANSFER;

import java.io.ByteArrayOutputStream;
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

import android.os.Environment;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.offline.FileTransferModel;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.offline.TransferProgress;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu, deepak malik
 *	Runnable responsible for receving file from client
 */
public class FileReceiverRunnable implements Runnable
{

	private static final String TAG = "OfflineThreadManager";

	private ServerSocket fileServerSocket = null;

	private Socket fileReceiveSocket = null;

	OfflineManager offlineManager = null;
	
	IConnectCallback connectCallback=null;
	
	File f = null;
	
	FileTransferModel fileTransferModel = null;

	public FileReceiverRunnable(IConnectCallback connectCallback)
	{
		this.connectCallback = connectCallback;
	}

	@Override
	public void run()
	{
		try
		{
			offlineManager = OfflineManager.getInstance();
			Logger.d(TAG, "Going to wait for fileReceive socket");
			fileServerSocket = new ServerSocket();
			fileServerSocket.setReuseAddress(true);
			SocketAddress addr = new InetSocketAddress(PORT_FILE_TRANSFER);
			fileServerSocket.bind(addr);
			Logger.d(TAG, "Going to wait for fileReceive socket");
			fileReceiveSocket = fileServerSocket.accept();
			Logger.d(TAG, "fileReceive socket connection success");

			InputStream inputstream = fileReceiveSocket.getInputStream();
			connectCallback.onConnect();

			while (true)
			{
				byte[] metaDataLengthArray = new byte[4];
				int msgSize = inputstream.read(metaDataLengthArray, 0, 4);
				Logger.d(TAG, "Read File Receiver ThreadBytes is " + msgSize);
				int metaDataLength = OfflineUtils.byteArrayToInt(metaDataLengthArray);

				// This is the case when the other person swipes the app. We read zero on stream and need to throw IO Excetion
				if (metaDataLength == 0)
				{
					throw new IOException();
				}
				Logger.d(TAG, "Size of MetaString: " + metaDataLength);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(metaDataLength);
				boolean isMetaDataReceived = OfflineUtils.copyFile(inputstream, byteArrayOutputStream, metaDataLength);
				Logger.d(TAG, "Metadata Received Properly: " + isMetaDataReceived);
				byteArrayOutputStream.close();

				byte[] metaDataBytes = byteArrayOutputStream.toByteArray();
				String metaDataString = new String(metaDataBytes, "UTF-8");
				Logger.d(TAG, metaDataString);

				JSONObject fileJSON = null;
				JSONObject message = null;
				String filePath = "";
				long mappedMsgId = -1;
				String fileName = "";
				int fileSize = 0;
				int totalChunks=0;
				try
				{
					message = new JSONObject(metaDataString);
					message.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
					message.remove(HikeConstants.TO);

					JSONObject metadata = message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
					mappedMsgId = message.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);

					fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
					fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
					int type = fileJSON.getInt(HikeConstants.HIKE_FILE_TYPE);
					fileName = fileJSON.getString(HikeConstants.FILE_NAME);
					filePath = OfflineUtils.getFileBasedOnType(type, fileName);
					totalChunks = OfflineUtils.getTotalChunks(fileSize);
					
				}
				catch (JSONException e1)
				{
					Logger.e(TAG, "Code phata in JSON initialisations", e1);
					e1.printStackTrace();
				}

				ConvMessage convMessage = null;
				fileTransferModel =  new FileTransferModel(new TransferProgress(0, totalChunks), message);
				try
				{
					(message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(
							HikeConstants.FILE_PATH, filePath);
					convMessage = new ConvMessage(message, HikeMessengerApp.getInstance().getApplicationContext());
					
					// update DB and UI.
					HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
					offlineManager.addToCurrentReceivingFile(convMessage.getMsgID(), fileTransferModel);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
					Logger.d(TAG, filePath);

					// TODO : Revisit the logic again.
					f = new File(Environment.getExternalStorageDirectory() + "/" + "Hike/Media/hike Images" + "/tempImage_" + fileName);
					File dirs = new File(f.getParent());
					if (!dirs.exists())
						dirs.mkdirs();
					// created a temporary file which on successful download will be renamed.
					f.createNewFile();
					
					// TODO:Can be done via show progress pubsub.
					// showDownloadTransferNotification(mappedMsgId, fileSize);
					FileOutputStream outputStream = new FileOutputStream(f);
					// TODO:Take action on the basis of return type.
					OfflineUtils.copyFile(inputstream, new FileOutputStream(f),fileTransferModel, true, false, fileSize);
					OfflineUtils.closeOutputStream(outputStream);
					f.renameTo(new File(filePath));
					
					// if f!=null and exception occurs we need to delete the temp file
					f = null;
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				// send ack for the packet received
				JSONObject ackJSON = OfflineUtils.createAckPacket(convMessage.getMsisdn(), mappedMsgId, true);
				offlineManager.addToTextQueue(ackJSON);
				
				offlineManager.removeFromCurrentReceivingFile(convMessage.getMsgID());

				OfflineUtils.showSpinnerProgress(fileTransferModel);
				// TODO:Disconnection handling:
				// if(isDisconnectPosted)
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
			Logger.e(TAG, "File Receiver Thread " + " IO Exception occured.Socket was not bounded");
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Did we pass correct Address here ? ?");
		}
		catch (OfflineException e)
		{
			e.printStackTrace();
			if (f != null)
			{
				Logger.d(TAG, "Going to delete file in FRR");
				f.delete();
			}
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));

		}
	}

	public void shutDown()
	{

		try
		{
			OfflineUtils.closeSocket(fileReceiveSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			OfflineUtils.closeSocket(fileServerSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
