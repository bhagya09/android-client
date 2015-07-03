package com.bsb.hike.offline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter.FTViewHolder;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.models.ReceiverConsignment;
import com.hike.transporter.models.SenderConsignment;

/**
 * 
 * @author sahil This class will convert ConvMessage to SenderConsignement and ReceiverConsignment back to convmessage and 
 *  will be an intermediate between hike and transporter   
 */

public class HikeConverter implements IMessageReceived , IMessageSent  {
	
	private static final String TAG = HikeConverter.class.getName();

	private Map<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private Map<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private static HikeConverter _instance = null;
	private Context context;
	private OfflineManager offlineManager = null;
	private OfflineMessagesManager messagesManager = null;
	
	private HikeConverter()
	{
		init();
	}

	public static HikeConverter getInstance()
	{
		if (_instance == null)
		{
			synchronized (HikeConverter.class)
			{
				if (_instance == null)
				{
					_instance = new HikeConverter();

				}
			}
		}
		return _instance;
	}
	
	private void init() {
		context  =  HikeMessengerApp.getInstance().getApplicationContext();
		offlineManager = OfflineManager.getInstance();
	}
	
	public void sendFile(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			int attachmentType, String msisdn, String apkLabel)
	{
		ConvMessage convMessage =  getConvMessageForFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, apkLabel);
		File file =  new File(filePath);
		FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0,OfflineUtils.getTotalChunks((int)file.length())), convMessage);
		SenderConsignment senderConsignment =  new SenderConsignment.Builder(convMessage.serialize().toString(),OfflineConstants.FILE_TOPIC).file(file).persistance(false).build();	
		senderConsignment.setTag(convMessage);
		addToCurrentSendingFile(convMessage.getMsgID(), fileTransferModel);
		offlineManager.sendConsignment(senderConsignment);
	}
	
	public void sendFile(ConvMessage convMessage)
	{
		String filePath  = OfflineUtils.getFilePathFromJSON(convMessage.serialize());
		File file  = new File(filePath);
		SenderConsignment senderConsignment =  new SenderConsignment.Builder(convMessage.serialize().toString(),OfflineConstants.FILE_TOPIC).file(file).persistance(false).build();	
		senderConsignment.setTag(convMessage);
		FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0,OfflineUtils.getTotalChunks((int)file.length())), convMessage);
		addToCurrentSendingFile(convMessage.getMsgID(), fileTransferModel);
		offlineManager.sendConsignment(senderConsignment);
	}
	
	private ConvMessage getConvMessageForFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			int attachmentType, String msisdn, String apkLabel)
	{
		int type = hikeFileType.ordinal();
		File file = new File(filePath);
		String fileName = file.getName();
		if (type == HikeFileType.APK.ordinal())
			fileName = apkLabel + ".apk";
		ConvMessage convMessage = FileTransferManager.getInstance(context).uploadOfflineFile(msisdn, file, fileKey, fileType, hikeFileType, isRecording, recordingDuration,
				attachmentType, fileName);
		return convMessage;
	}
	
	@Override
	public void onTransitBegin(SenderConsignment senderConsignment) {
		
	}

	@Override
	public void onTransitEnd(SenderConsignment senderConsignment) {
		
	}

	@Override
	public void onChunkSend(SenderConsignment senderConsignment) {
		
	}

	@Override
	public void onMessageDelivered(SenderConsignment senderConsignment) {
		ConvMessage convMessage  = (ConvMessage)senderConsignment.getTag();
		String msisdn = convMessage.getMsisdn();
		long msgId = convMessage.getMsgID();
		if(convMessage.isFileTransferMessage())
		{
			HikeConversationsDatabase.getInstance().updateMessageMetadata(msgId, OfflineUtils.getUpdatedMessageMetaData(convMessage));
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
			removeFromCurrentSendingFile(convMessage.getMsgID());
		}
		
		int rowsUpdated = OfflineUtils.updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
		if (rowsUpdated == 0)
		{
			Logger.d(getClass().getSimpleName(), "No rows updated");
		}
		Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
	}

	@Override
	public void onError(SenderConsignment senderConsignment, int errorCode) {
		
	}

	@Override
	public void onMessageReceived(ReceiverConsignment receiverConsignment) {
		JSONObject messageJSON;
		try {
			messageJSON = new JSONObject(receiverConsignment.message);
			toggleToAndFromField(messageJSON);
			if(OfflineUtils.isFileTransferMessage(messageJSON))
			{
				// file Variables
				String fileName="", filePath= ""; 
				Integer totalChunks = 0 ;
				setFileVariables(messageJSON, fileName, filePath, totalChunks);
				updateMessageJSON(messageJSON,filePath,fileName);
				ConvMessage convMessage = new ConvMessage(messageJSON,context);
				FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0, totalChunks), convMessage);
				receiverConsignment.setTag(convMessage);
				addToDatabase(convMessage);
				addToCurrentReceivingFile(convMessage.getMsgID(), fileTransferModel);
			}
			else
			{
				if(OfflineUtils.isChatThemeMessage(messageJSON))
				{
					messagesManager.handleChatThemeMessage(messageJSON);
					return;
				}
				ConvMessage convMessage = new ConvMessage(messageJSON, HikeMessengerApp.getInstance().getApplicationContext());
				if(OfflineUtils.isStickerMessage(messageJSON))
				{
					receiverConsignment.setTag(convMessage);
				}
				addToDatabase(convMessage);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void addToDatabase(ConvMessage convMessage) {
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
	}

	private void updateMessageJSON(JSONObject messageJSON, String filePath,
			String fileName) {

		try {
			messageJSON.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
			messageJSON.remove(HikeConstants.TO);
			(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(HikeConstants.FILE_PATH,
					filePath);
			(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(HikeConstants.FILE_NAME,
					fileName);
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setFileVariables(JSONObject messageJSON, String fileName,
			String filePath, Integer totalChunks) {
		JSONObject metadata;
		try {
			metadata = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			JSONObject  fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
			int fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
			int type = fileJSON.getInt(HikeConstants.HIKE_FILE_TYPE);
			fileName = Utils.getFinalFileName(HikeFileType.values()[type], fileJSON.getString(HikeConstants.FILE_NAME));
			filePath = OfflineUtils.getFileBasedOnType(type, fileName);
			totalChunks = OfflineUtils.getTotalChunks(fileSize);
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onChunkRead(ReceiverConsignment receiver) {
		
	}

	@Override
	public void onFileCompleted(ReceiverConsignment receiver) {
		ConvMessage  message = (ConvMessage)receiver.getTag();
		JSONObject  messageJSON = message.serialize();
		if(OfflineUtils.isStickerMessage(messageJSON))
		{
			String stpath = OfflineUtils.getStickerPath(messageJSON);
			File stickerImage = new File(stpath);
			File tempSticker = receiver.getFile();
			if (!stickerImage.exists())
			{
				try {
					OfflineUtils.createStkDirectory(messageJSON);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				tempSticker.renameTo(stickerImage);
			}
			// delete file 
			else
			{
				if(tempSticker!=null && tempSticker.exists())
					tempSticker.delete();
			}
		}
		else
		{
			File tempFile = receiver.getFile();
			tempFile.renameTo(new File(OfflineUtils.getFilePathFromJSON(message.serialize())));
			FileTransferModel  fileTransferModel = currentReceivingFiles.get(message.getMsgID());
			removeFromCurrentReceivingFile(message.getMsgID());
			offlineManager.setInOfflineFileTransferInProgress(false);
			OfflineUtils.showSpinnerProgress(fileTransferModel);
		}
				// TODO:Disconnection handling:
		// if(isDisconnectPosted)
		// {
		// shouldBeDisconnected = true;
		// disconnectAfterTimeout();
		// }
		
	}

	public void sendMessage(ConvMessage convMessage) {
		JSONObject messageJSON = convMessage.serialize();
		SenderConsignment  senderConsignment = null;
		if(OfflineUtils.isStickerMessage(messageJSON))
		{
			String fileUri = OfflineUtils.getStickerPath(messageJSON);
			File stickerFile = new File(fileUri);
			OfflineUtils.putStkLenInPkt(messageJSON, stickerFile.length());
			senderConsignment =  new SenderConsignment.Builder(messageJSON.toString(),OfflineConstants.TEXT_TOPIC).file(stickerFile).build();	
		}
		else
		{
			senderConsignment =  new SenderConsignment.Builder(messageJSON.toString(),OfflineConstants.TEXT_TOPIC).build();	
		}
		senderConsignment.setTag(convMessage);
		offlineManager.sendConsignment(senderConsignment);
	}
	
	public FileTransferModel getConvMessageFromCurrentSendingFiles(long msgId)
	{
		if (currentSendingFiles.containsKey(msgId))
		{
			return currentSendingFiles.get(msgId);
		}
		return null;
	}
	
	private FileSavedState getUploadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		synchronized (currentSendingFiles)
		{
			if (currentSendingFiles.containsKey(msgId))
			{
				// Logger.d("Spinner", "Current Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
			}
			else if (convMessage.getState().equals(State.SENT_UNCONFIRMED))
			{
				fss = new FileSavedState(FTState.ERROR, (int) file.length(), 0);
			}
			else
			{
				// Logger.d("Spinner", "Completed Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
			}
			return fss;
		}
	}
	
	/**
	 * 
	 * @param convMessage
	 * @param file
	 * @return TODO:Removing the try catch for the app to crash. So that we can debug, what the issue was.
	 */
	private FileSavedState getDownloadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

		synchronized (currentReceivingFiles)
		{

			if (currentReceivingFiles.containsKey(msgId))
			{
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);

			}
			else
			{
				if (file.exists())
				{
					fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());

				}
				else
				{
					File f = new File(FileTransferManager.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).getHikeTempDir(), "tempImage_" + file.getName());
					if (f.exists())
					{
						Logger.d(TAG, "tempFile Delete" + f.getName());
						f.delete();
					}
					fss = new FileSavedState(FTState.ERROR, hikeFile.getFileKey());
				}
			}
		}
		return fss;
	}

	public FileSavedState getFileState(ConvMessage convMessage, File file)
	{
		return convMessage.isSent() ? getUploadFileState(convMessage, file) : getDownloadFileState(convMessage, file);
	}

	public void addToCurrentReceivingFile(long msgId, FileTransferModel fileTransferModel)
	{
		currentReceivingFiles.put(msgId, fileTransferModel);
	}

	public void removeFromCurrentReceivingFile(long msgId)
	{
		synchronized (currentReceivingFiles)
		{

			if (currentReceivingFiles.containsKey(msgId))
			{
				currentReceivingFiles.remove(msgId);
			}
		}
	}

	public void addToCurrentSendingFile(long msgId, FileTransferModel fileTransferModel)
	{
		if (OfflineUtils.isConnectedToSameMsisdn(fileTransferModel.getPacket(),offlineManager.getConnectedDevice()))
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

	private void showTransferProgress(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent)
	{
		Logger.d(TAG, "in showTransferProgress");
		int num = 0;
		if (isSent)
		{
			if (!currentSendingFiles.containsKey(msgId))
				return;
			else
				num = currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}
		else
		{
			//Logger.d(TAG, "showTransferProgress trying to get msg id is " + msgId);
			if (!currentReceivingFiles.containsKey(msgId))
				return;
			else
				num = currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}
	
		long progress = (((long)num*OfflineConstants.CHUNK_SIZE*100)/hikeFile.getFileSize());
		Logger.d(TAG, "CurrentSizeReceived: " + num + " FileSize: " + hikeFile.getFileSize() + 
				" Progress -> " +  progress + "  msgId  --->"+msgId);
		
		if (fss.getFTState() == FTState.IN_PROGRESS && progress == 0 && isSent)
		{
			holder.initializing.setVisibility(View.VISIBLE);
		}
		 else if (fss.getFTState() == FTState.IN_PROGRESS)
		{
			if (progress < 100)
				holder.circularProgress.setProgress(progress * 0.01f);
			if (Utils.isHoneycombOrHigher())
				holder.circularProgress.stopAnimation();

			Logger.d("Spinner", "Msg Id is......... "+ msgId  + ".........holder.circularProgress=" + holder.circularProgress.getCurrentProgress()*100
					+ " Progress=" + progress);
			
			float animatedProgress = 0 * 0.01f;
			if (fss.getTotalSize() > 0)
			{
				animatedProgress = (float) OfflineConstants.CHUNK_SIZE;
				animatedProgress = animatedProgress / fss.getTotalSize();
			}
			if (Utils.isHoneycombOrHigher())
			{
				if (holder.circularProgress.getCurrentProgress() < (0.95f) && progress == 100)
				{
					holder.circularProgress.setAnimatedProgress((int) (holder.circularProgress.getCurrentProgress() * 100), (int) progress, 300);
				}
				else
					holder.circularProgress.setAnimatedProgress((int) progress, (int) progress + (int) (animatedProgress * 100), 6 * 1000);
			}
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
	}

	public void setupFileState(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent, boolean ext)
	{
		int playImage = -1;
		int retryImage = R.drawable.ic_retry_image_video;
		if (!ext)
		{
			playImage = R.drawable.ic_videoicon;
		}
		holder.ftAction.setVisibility(View.GONE);
		holder.circularProgressBg.setVisibility(View.GONE);
		holder.initializing.setVisibility(View.GONE);
		holder.circularProgress.setVisibility(View.GONE);
		switch (fss.getFTState())
		{
		case IN_PROGRESS:
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			holder.circularProgress.setVisibility(View.VISIBLE);
			Logger.d(TAG, "IN_PROGRESS");
			showTransferProgress(holder, fss, msgId, hikeFile, isSent);
			break;
		case COMPLETED:
			holder.circularProgressBg.setVisibility(View.GONE);
			holder.circularProgress.resetProgress();
			Logger.d(TAG, "COMPLETED");
			holder.circularProgress.setVisibility(View.GONE);
			if (hikeFile.getHikeFileType() == HikeFileType.VIDEO && !ext)
			{
				holder.ftAction.setImageResource(playImage);
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			break;
		case ERROR:
			if(isSent)
			{
				holder.ftAction.setImageResource(retryImage);
				holder.ftAction.setContentDescription(context.getResources().getString(R.string.content_des_retry_file_download));
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}break;
		default:
			break;
		}
		holder.ftAction.setScaleType(ScaleType.CENTER);
	}
	
	public void handleRetryButton(ConvMessage convMessage)
	{
		if (offlineManager.getOfflineState() != OFFLINE_STATE.CONNECTED)
		{
			HikeMessengerApp.getInstance().showToast("You are not connected..!! Kindly connect.");
			return;
		}
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		Logger.d(TAG, "Hike File type is: " + hikeFile.getHikeFileType().ordinal());
		int length = hikeFile.getFileSize();
		
		File f = hikeFile.getFile();
		if (f.exists())
		{
			sendFile(convMessage);
		}
		else
		{
			HikeMessengerApp.getInstance().showToast("File not found.!!");
		}
	}

	public void deleteRemainingReceivingFiles() {
		
		if (currentReceivingFiles.size() > 0)
		{
			ArrayList<Long> rMsgIds = new ArrayList<>(currentReceivingFiles.size());

			for (Entry<Long, FileTransferModel> itr : currentReceivingFiles.entrySet())
			{
				rMsgIds.add(itr.getKey());
			}

			ChatThreadUtils.deleteMessagesFromDb(rMsgIds, false, rMsgIds.get(rMsgIds.size() - 1), "o:" + offlineManager.getConnectedDevice());
			final ConvMessage deleteFilesConvMessage = OfflineUtils.createOfflineInlineConvMessage("o:" + offlineManager.getConnectedDevice(), context.getString(R.string.files_not_received),
					OfflineConstants.OFFLINE_FILES_NOT_RECEIVED_TYPE);
			HikeConversationsDatabase.getInstance().addConversationMessages(deleteFilesConvMessage, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED,deleteFilesConvMessage);
		}
	} 
	
	private void toggleToAndFromField(JSONObject message)
	{
		try {
			message.put(HikeConstants.FROM, "o:" + offlineManager.getConnectedDevice());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		message.remove(HikeConstants.TO);
	}
	
}
