package com.bsb.hike.offline.runnables;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.FileTransferModel;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.offline.TransferProgress;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FileReceiveRunnable implements Runnable
{
	private static final String TAG = "OfflineThreadManager";

	private InputStream inputStream;

	private IConnectCallback connectCallback;

	File tempFile = null;

	FileTransferModel fileTransferModel = null;

	private JSONObject fileJSON = null;

	private JSONObject message = null;

	private String filePath = "";

	private long mappedMsgId = -1;

	private String fileName = "";

	private int fileSize = 0;

	private int totalChunks = 0;

	private OfflineManager offlineManager;

	private ConvMessage convMessage = null;

	public FileReceiveRunnable(InputStream inputStream, IConnectCallback connectCallback)
	{
		this.connectCallback = connectCallback;
		this.inputStream = inputStream;
	}

	@Override
	public void run()
	{
		offlineManager = OfflineManager.getInstance();
		try
		{
			while (true)
			{
				byte[] metaDataLengthArray = new byte[4];
				int msgSize = inputStream.read(metaDataLengthArray, 0, 4);
				Logger.d(TAG, "Read File Receiver ThreadBytes is " + msgSize);
				int metaDataLength = OfflineUtils.byteArrayToInt(metaDataLengthArray);

				// This is the case when the other person swipes the app. We read zero on stream and need to throw IO Excetion
				if (metaDataLength == 0)
				{
					throw new IOException();
				}
				Logger.d(TAG, "Size of MetaString: " + metaDataLength);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(metaDataLength);
				boolean isMetaDataReceived = OfflineUtils.copyFile(inputStream, byteArrayOutputStream, metaDataLength);
				Logger.d(TAG, "Metadata Received Properly: " + isMetaDataReceived);
				byteArrayOutputStream.close();

				byte[] metaDataBytes = byteArrayOutputStream.toByteArray();
				String metaDataString = new String(metaDataBytes, "UTF-8");
				Logger.d(TAG, metaDataString);

				try
				{
					message = new JSONObject(metaDataString);

					toggleToAndFromField(message);

					initializeFileVariables(message);

					convMessage = new ConvMessage(message, HikeMessengerApp.getInstance().getApplicationContext());

					fileTransferModel = new FileTransferModel(new TransferProgress(0, totalChunks), convMessage);
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "Code phata in JSON initialisations", e);
					e.printStackTrace();
					throw new OfflineException(OfflineException.JSON_EXCEPTION);
				}
				// update DB and UI.
				HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
				offlineManager.addToCurrentReceivingFile(convMessage.getMsgID(), fileTransferModel);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
				Logger.d(TAG, filePath);

				tempFile = OfflineUtils.createTempFile(fileName);

				FileOutputStream outputStream = new FileOutputStream(tempFile);
				OfflineUtils.copyFile(inputStream, new FileOutputStream(tempFile), fileTransferModel, true, false, fileSize);
				Logger.d(TAG, "File Received Successfully");
				OfflineUtils.closeOutputStream(outputStream);
				tempFile.renameTo(new File(filePath));

				// if f!=null and exception occurs we need to delete the temp file
				tempFile = null;

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
			if (tempFile != null && tempFile.exists())
			{
				Logger.d(TAG, "Going to delete file in FRR");
				tempFile.delete();
			}
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
			if (tempFile != null && tempFile.exists())
			{
				Logger.d(TAG, "Going to delete file in FRR");
				tempFile.delete();
			}
			connectCallback.onDisconnect(e);

		}

	}

	private void initializeFileVariables(JSONObject message) throws JSONException
	{
		JSONObject metadata = message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
		mappedMsgId = message.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);

		fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
		fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
		int type = fileJSON.getInt(HikeConstants.HIKE_FILE_TYPE);
		fileName = Utils.getFinalFileName(HikeFileType.values()[type], fileJSON.getString(HikeConstants.FILE_NAME));
		filePath = OfflineUtils.getFileBasedOnType(type, fileName);
		totalChunks = OfflineUtils.getTotalChunks(fileSize);

		(message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(HikeConstants.FILE_PATH,
				filePath);
		(message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(HikeConstants.FILE_NAME,
				fileName);

	}

	private void toggleToAndFromField(JSONObject message) throws JSONException
	{
		message.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
		message.remove(HikeConstants.TO);

	}

}
