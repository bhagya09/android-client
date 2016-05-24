package com.bsb.hike.offline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.util.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.media.MediaScannerConnection;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.offline.OfflineConstants.MessageType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.hike.transporter.utils.Logger;

public class OfflineFileManager
{
	private static final String TAG = "OfflineManager";
	private Context context;
	private Map<Long, FileTransferModel> currentSendingFiles=null;
	private Map<Long, FileTransferModel> currentReceivingFiles=null;

	public OfflineFileManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();
		currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();
		currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();
	}
	
	public void deleteRemainingReceivingFiles()
	{
		if (currentReceivingFiles.size() > 0)
		{
			ArrayList<Long> rMsgIds = new ArrayList<>(currentReceivingFiles.size());

			for (Entry<Long, FileTransferModel> itr : currentReceivingFiles.entrySet())
			{
				FileTransferModel model = itr.getValue();
				if (!(model.getTransferProgress().getCurrentChunks() == model.getTransferProgress().getTotalChunks()))
					rMsgIds.add(itr.getKey());
			}
			deleteFiles(rMsgIds, OfflineController.getInstance().getConnectedDevice());

		}
	}

	public void deleteFiles(ArrayList<Long> rMsgIds,String msisdn)
	{
		if(rMsgIds==null|| rMsgIds.isEmpty() || TextUtils.isEmpty(msisdn))
			return;
		ChatThreadUtils.deleteMessagesFromDb(rMsgIds, false, rMsgIds.get(rMsgIds.size() - 1), msisdn);

		final ConvMessage deleteFilesConvMessage = OfflineUtils.createOfflineInlineConvMessage(msisdn, context.getString(R.string.files_not_received),
				OfflineConstants.OFFLINE_FILES_NOT_RECEIVED_TYPE);

		HikeConversationsDatabase.getInstance().addConversationMessages(deleteFilesConvMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, deleteFilesConvMessage);
	}
	
	
	public void onChunkSend(ConvMessage convMessage) 
	{
		if (convMessage == null)
			return;
		
		OfflineUtils.showSpinnerProgress(currentSendingFiles.get(convMessage.getMsgID()));
	}

	public void onChunkRead(ConvMessage convMessage) 
	{
		if(convMessage==null)
		{
			return;
		}
			
		OfflineUtils.showSpinnerProgress(currentReceivingFiles.get(convMessage
				.getMsgID()));
	}
	
	public FileTransferModel getConvMessageFromCurrentSendingFiles(long msgId) 
	{
		if (currentSendingFiles.containsKey(msgId)) {
			return currentSendingFiles.get(msgId);
		}
		return null;
	}
	
	public FileSavedState getUploadFileState(ConvMessage convMessage, File file)
	{
		// AND-5089
		if (file == null)
		{
			return new FileSavedState();
		}
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		synchronized (currentSendingFiles)
		{

			if (currentSendingFiles.containsKey(msgId))
			{
				// Defensive check as in shutdown we  clear the map 
				FileTransferModel mmModel = currentSendingFiles.get(msgId);
				if (mmModel != null)
				{
					fss = new FileSavedState(FTState.IN_PROGRESS,  file.length(),mmModel.getTransferProgress().getCurrentChunks()
							* OfflineConstants.CHUNK_SIZE * 1024,0);
					return fss;
				}
			}
			if (TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				fss = new FileSavedState(FTState.ERROR, file.length(), 0,0);
			}
			else
			{
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey(),0);
			}
			return fss;
		}
	}
	
	/**
	 * 
	 * @param convMessage
	 * @param file
	 * @return TODO:Removing the try catch for the app to crash. So that we can
	 *         debug, what the issue was.
	 */
	public FileSavedState getDownloadFileState(ConvMessage convMessage, File file)
	{
		// AND-5089
		if (file == null)
		{
			return new FileSavedState();
		}
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

		synchronized (currentReceivingFiles)
		{

			if (currentReceivingFiles.containsKey(msgId))
			{
				FileTransferModel mmModel = currentReceivingFiles.get(msgId);
				// Defensive check as in shutdown we  clear the map 
				if (mmModel != null)
				{
					fss = new FileSavedState(FTState.IN_PROGRESS,  file.length(), mmModel.getTransferProgress().getCurrentChunks() * OfflineConstants.CHUNK_SIZE * 1024,0);
					return fss;
				}
			}
			if (file.exists() || (hikeFile.getHikeFileType() == HikeFileType.CONTACT))
			{
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey(),0);
			}
			else
			{
				fss = new FileSavedState(FTState.ERROR, hikeFile.getFileKey(),0);
			}
		}
		return fss;
	}

	public void addToCurrentReceivingFile(long msgId, FileTransferModel fileTransferModel) 
	{
		currentReceivingFiles.put(msgId, fileTransferModel);
		HikeSharedPreferenceUtil.getInstance().saveData(OfflineConstants.CURRENT_RECIEVING_MSG_ID, msgId);
	}

	public void removeFromCurrentReceivingFile(long msgId) 
	{
		synchronized (currentReceivingFiles) 
		{
			if (currentReceivingFiles.containsKey(msgId)) 
			{
				currentReceivingFiles.remove(msgId);
				HikeSharedPreferenceUtil.getInstance().removeData(OfflineConstants.CURRENT_RECIEVING_MSG_ID);
			}
		}
	}

	public void addToCurrentSendingFile(long msgId, FileTransferModel fileTransferModel) 
	{
		if (OfflineUtils.isConnectedToSameMsisdn(fileTransferModel.getPacket(), OfflineController.getInstance().getConnectedDevice())) 
		{
			currentSendingFiles.put(msgId, fileTransferModel);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}
	}

	public void removeFromCurrentSendingFile(long msgId) 
	{
		synchronized (currentSendingFiles) 
		{
			if (currentSendingFiles.containsKey(msgId)) 
			{
				currentSendingFiles.remove(msgId);
			}
		}

	}

	public void removeFileAndUpdateView(long msgId)
	{
		removeFromCurrentSendingFile(msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}
	
	public void shutDown() 
	{
		deleteRemainingReceivingFiles();
		currentReceivingFiles.clear();
		currentSendingFiles.clear();
	}

	public void onFileCompleted(ConvMessage message, File file)
	{
		JSONObject messageJSON = message.serialize();
		if (OfflineUtils.isStickerMessage(messageJSON))
		{
			Sticker sticker = OfflineUtils.getSticker(messageJSON);
			File tempSticker = file;
			String filePath=null;
			File stickerImage = null;
			if (sticker != null && !sticker.isStickerAvailable())
			{
				try
				{
					filePath = OfflineUtils.createStkDirectory(messageJSON);
					if (filePath != null)
					{
						stickerImage = new File(filePath);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				if (filePath != null && stickerImage != null)
				{
					tempSticker.renameTo(stickerImage);
				}

			}
			else
			{
				// delete file
				if (tempSticker != null && tempSticker.exists())
					tempSticker.delete();
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}
		else
		{
			File tempFile = file;
			File hikePath=new File(OfflineUtils.getFilePathFromJSON(messageJSON));
			boolean result=tempFile.renameTo(hikePath);
			Logger.d(TAG, "renaming result is " + result);
			// Now scanning the above received file as it was not visible to android before
			MediaScannerConnection.scanFile(HikeMessengerApp.getInstance().getApplicationContext(), new String[] { hikePath.getAbsolutePath() }, null, null);
			FileTransferModel fileTransferModel = currentReceivingFiles.get(message.getMsgID());

			removeFromCurrentReceivingFile(message.getMsgID());
			OfflineUtils.showSpinnerProgress(fileTransferModel);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_FILE_COMPLETED, message);
		SessionTracFilePOJO pojo = OfflineSessionTracking.getInstance().getFileSession(message.getMsgID());
		if (pojo != null)
		{
			pojo.recordEndTime();
		}
	}

	public void handleFileDelivered(long msgId, ConvMessage tempConvMessage) 
	{
		HikeFile hikeFile = tempConvMessage.getMetadata().getHikeFiles().get(0);
		removeBinFileIfExists(hikeFile.getFilePath(),tempConvMessage.getMsgID());
		HikeConversationsDatabase.getInstance().updateMessageMetadata(msgId, OfflineUtils.getUpdatedMessageMetaData(tempConvMessage));
		HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
		removeFromCurrentSendingFile(msgId);
	}
	

	private void removeBinFileIfExists(String file, long msgId) {

		File x = context.getExternalFilesDir("hiketmp");
		File bin = new File(x, file + ".bin." + msgId);
		if (bin != null && bin.exists()) {
			bin.delete();
		}
	}

	public void handleMessageReceived(ConvMessage convMessage) 
	{
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		long totalChunks = getTotalChunks(hikeFile.getFileSize());
		
		FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0, totalChunks), convMessage);
		addToCurrentReceivingFile(convMessage.getMsgID(), fileTransferModel);
		
		String fileType = HikeFileType.toString(hikeFile.getHikeFileType());
		long fileSize = hikeFile.getFileSize();
		SessionTracFilePOJO pojo = new SessionTracFilePOJO(fileType, fileSize, MessageType.RECEIVED.ordinal());
		OfflineSessionTracking.getInstance().addToListOfFiles(convMessage.getMsgID(), pojo);
		
	}
	
	private long getTotalChunks(long fileSize)
	{
		long totalChunks = OfflineUtils.getTotalChunks(fileSize);
		return totalChunks;
	}
	
	public long getTransferProgress(long msgId, boolean isSent)
	{
		if (isSent) 
		{
			if (!currentSendingFiles.containsKey(msgId))
				return -1;
			else
				return currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		} 
		else 
		{
			if (!currentReceivingFiles.containsKey(msgId))
				return -1;
			else
				return currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}
	}
}
