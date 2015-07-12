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
import android.widget.Toast;

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
import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.models.ReceiverConsignment;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.TConstants.ERRORCODES;

/**
 * 
 * @author sahil This class will convert ConvMessage to SenderConsignement and
 *         ReceiverConsignment back to convmessage and will be an intermediate
 *         between hike and transporter
 */

public class HikeConverter implements IMessageReceived, IMessageSent {

	private static final String TAG = HikeConverter.class.getName();

	private Map<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();
	private Map<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private static HikeConverter _instance = null;
	private Context context;
	private OfflineMessagesManager messagesManager = null;

	private HikeConverter() 
	{
		init();
	}

	public static HikeConverter getInstance() {
		if (_instance == null) {
			synchronized (HikeConverter.class) {
				if (_instance == null) {
					_instance = new HikeConverter();

				}
			}
		}
		return _instance;
	}

	private void init() {
		context = HikeMessengerApp.getInstance().getApplicationContext();
	}

	public void sendFile(String filePath, String fileKey,
			HikeFileType hikeFileType, String fileType, boolean isRecording,
			long recordingDuration, int attachmentType, String msisdn,
			String apkLabel) 
	{
		ConvMessage convMessage = getConvMessageForFileTransfer(filePath,
				fileKey, hikeFileType, fileType, isRecording,
				recordingDuration, attachmentType, msisdn, apkLabel);
		sendFile(convMessage);
	}

	public void sendFile(ConvMessage convMessage) 
	{
		String filePath = OfflineUtils.getFilePathFromJSON(convMessage.serialize());
		File file = new File(filePath);
		SenderConsignment senderConsignment = new SenderConsignment.Builder(
				convMessage.serialize().toString(), OfflineConstants.FILE_TOPIC)
				.file(file).persistance(false).ackRequired(true).build();
		senderConsignment.setTag(convMessage);
		senderConsignment.setAwb((int) convMessage.getMsgID());
		
		FileTransferModel fileTransferModel = new FileTransferModel(
				new TransferProgress(0, OfflineUtils.getTotalChunks((int) file
						.length())), convMessage);
		addToCurrentSendingFile(convMessage.getMsgID(), fileTransferModel);
		OfflineManager.getInstance().sendConsignment(senderConsignment);
	}

	private ConvMessage getConvMessageForFileTransfer(String filePath,
			String fileKey, HikeFileType hikeFileType, String fileType,
			boolean isRecording, long recordingDuration, int attachmentType,
			String msisdn, String apkLabel) 
	{
		int type = hikeFileType.ordinal();
		File file = new File(filePath);
		String fileName = file.getName();
		if (type == HikeFileType.APK.ordinal())
			fileName = apkLabel + ".apk";
		ConvMessage convMessage = FileTransferManager.getInstance(context)
				.uploadOfflineFile(msisdn, file, fileKey, fileType,
						hikeFileType, isRecording, recordingDuration,
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
		ConvMessage convMessage = (ConvMessage) senderConsignment.getTag();
		OfflineUtils.showSpinnerProgress(currentSendingFiles.get(convMessage
				.getMsgID()));
	}

	/**
	 * This function is called when message gets Delivered.
	 * @param senderConsignment
	 */
	@Override
	public void onMessageDelivered(SenderConsignment senderConsignment) 
	{
		ConvMessage convMessage = null;
		JSONObject message = null;
		
		/*
		 * senderConsignment.getTag() is null when it is made from the 
		 * persistence db of the library. Tag is never stored in the persistence db, hence it is null
		 * 
		 */
		if (senderConsignment.getTag() != null)
		{
			convMessage = (ConvMessage) senderConsignment.getTag();
			message = convMessage.serialize();
		}
		else
		{
			try 
			{
				message = new JSONObject(senderConsignment.getMessage());
			} 
			catch (JSONException e) 
			{
				message = null;
				e.printStackTrace();
			}
		}
		
		ConvMessage tempConvMessage = null;
		try {
			tempConvMessage = (convMessage != null) ? convMessage : new ConvMessage(message, context);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		/*
		 * We take msgId from JSON but not ConvMessage as the constructor
		 * of ConvMessage(JSONObject, Context) is used for receiver end only
		 * Thus it does not contain convMessage.getMsgId() but convMessage.getMappedMsgId()
		 * Hence to avoid this confusion we serialise convMessage to JSONObject 
		 * from where msgId is obtained.
		 * 
		 * 
		 * We need tempConvMessage for getting MessageMetadata to update 
		 * ConvMessage for double tick
		 */
		String msisdn = tempConvMessage.getMsisdn();
		long msgId = OfflineUtils.getMsgId(message);
		
		int rowsUpdated = OfflineUtils.updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
		if (rowsUpdated == 0) 
		{
			Logger.d(getClass().getSimpleName(), "No rows updated");
		}
		if (!OfflineUtils.isContactTransferMessage(message) && OfflineUtils.isFileTransferMessage(message)) 
		{ 
			HikeConversationsDatabase.getInstance().updateMessageMetadata(msgId, OfflineUtils.getUpdatedMessageMetaData(tempConvMessage));
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
			removeFromCurrentSendingFile(msgId);
		}
		
		Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
	}


	@Override
	public void onMessageReceived(ReceiverConsignment receiverConsignment) 
	{
		JSONObject messageJSON;
		try 
		{
			messageJSON = new JSONObject(receiverConsignment.message);
			toggleToAndFromField(messageJSON);
			if(!OfflineUtils.isContactTransferMessage(messageJSON) && OfflineUtils.isFileTransferMessage(messageJSON)) 
			{
				
				setFileVariablesAndUpdateJSON(messageJSON);
				int totalChunks = getTotalChunks(messageJSON);
				
				ConvMessage convMessage = new ConvMessage(messageJSON, context);
				FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0, totalChunks), convMessage);
				receiverConsignment.setTag(convMessage);
				
				addToDatabase(convMessage);
				addToCurrentReceivingFile(convMessage.getMsgID(),
						fileTransferModel);
			} 
			else if(OfflineUtils.isInfoPkt(messageJSON))
			{
				Logger.d(TAG, "Info Packet received ...>>" + messageJSON.toString());
			}
			else 
			{
				if (OfflineUtils.isChatThemeMessage(messageJSON)) 
				{
					if(messagesManager==null)
					{
						messagesManager = new OfflineMessagesManager();
					}
					messagesManager.handleChatThemeMessage(messageJSON);
					return;
				} 
				else if (OfflineUtils.isDisconnectPkt(messageJSON)) 
				{
					throw new OfflineException(OfflineException.USER_DISCONNECTED);
				}
				
				ConvMessage convMessage = new ConvMessage(messageJSON, HikeMessengerApp.getInstance().getApplicationContext());
				if (OfflineUtils.isStickerMessage(messageJSON)) 
				{
					receiverConsignment.setTag(convMessage);
				}
				addToDatabase(convMessage);
			}
		} 
		catch (JSONException jsonException) 
		{
			OfflineManager.getInstance().onDisconnect(new OfflineException(OfflineException.JSON_EXCEPTION));
		} 
		catch (OfflineException offlineException) 
		{
			OfflineManager.getInstance().onDisconnect(offlineException);
		}
	}

	private void addToDatabase(ConvMessage convMessage) {
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED,convMessage);
	}

	private void updateMessageJSON(JSONObject messageJSON, String filePath, String fileName) throws JSONException 
	{
		toggleToAndFromField(messageJSON);
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.FILE_PATH, filePath);
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.SOURCE_FILE_PATH,filePath);
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.FILE_NAME, fileName);

	}

	private void setFileVariablesAndUpdateJSON(JSONObject messageJSON) throws JSONException 
	{
		JSONObject fileJSON = getFileJSONFromMetadata(messageJSON);
		int type = fileJSON.getInt(HikeConstants.HIKE_FILE_TYPE);
		String fileName = Utils.getFinalFileName(HikeFileType.values()[type], fileJSON.getString(HikeConstants.FILE_NAME));
		String filePath = OfflineUtils.getFileBasedOnType(type, fileName);
		updateMessageJSON(messageJSON, filePath, fileName);
	}
	
	private JSONObject getFileJSONFromMetadata(JSONObject messageJSON) throws JSONException
	{
		JSONObject metadata;
		metadata = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
		JSONObject fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
		return fileJSON;
	}
	
	private int getTotalChunks(JSONObject messageJSON) throws JSONException
	{
		JSONObject fileJSON = getFileJSONFromMetadata(messageJSON);
		int fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
		int totalChunks = OfflineUtils.getTotalChunks(fileSize);
		return totalChunks;
	}

	@Override
	public void onChunkRead(ReceiverConsignment receiverConsignment) {
		ConvMessage convMessage = (ConvMessage) receiverConsignment.getTag();
		OfflineUtils.showSpinnerProgress(currentReceivingFiles.get(convMessage
				.getMsgID()));
	}

	@Override
	public void onFileCompleted(ReceiverConsignment receiver) 
	{
		ConvMessage message = (ConvMessage) receiver.getTag();
		JSONObject messageJSON = message.serialize();
		
		if (OfflineUtils.isStickerMessage(messageJSON)) 
		{
			String stpath = OfflineUtils.getStickerPath(messageJSON);
			File stickerImage = new File(stpath);
			File tempSticker = receiver.getFile();
			
			if (!stickerImage.exists()) 
			{
				try 
				{
					stickerImage = new File(OfflineUtils.createStkDirectory(messageJSON));
				} 
				catch (JSONException e) 
				{
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				tempSticker.renameTo(stickerImage);
			}
			else 
			{
				// delete file
				if (tempSticker != null && tempSticker.exists())
					tempSticker.delete();
			}
		} 
		else 
		{
			File tempFile = receiver.getFile();
			tempFile.renameTo(new File(OfflineUtils.getFilePathFromJSON(messageJSON)));
			FileTransferModel fileTransferModel = currentReceivingFiles.get(message.getMsgID());
			
			removeFromCurrentReceivingFile(message.getMsgID());
			OfflineManager.getInstance().setInOfflineFileTransferInProgress(false);
			OfflineUtils.showSpinnerProgress(fileTransferModel);
		}
	}

	public void sendMessage(ConvMessage convMessage) 
	{
		JSONObject messageJSON = convMessage.serialize();
		SenderConsignment senderConsignment = null;
		Logger.d(TAG, "Going to send to text topic");
		if (OfflineUtils.isStickerMessage(messageJSON)) 
		{
			String fileUri = OfflineUtils.getStickerPath(messageJSON);
			File stickerFile = new File(fileUri);
			OfflineUtils.putStkLenInPkt(messageJSON, stickerFile.length());
			senderConsignment = new SenderConsignment.Builder(
					messageJSON.toString(), OfflineConstants.TEXT_TOPIC).file(
					stickerFile).ackRequired(true).persistance(true).build();
		} 
		else 
		{
			senderConsignment = new SenderConsignment.Builder(
					messageJSON.toString(), OfflineConstants.TEXT_TOPIC)
					.build();
		}
		senderConsignment.setAwb((int) convMessage.getMsgID());
		senderConsignment.setTag(convMessage);
		OfflineManager.getInstance().sendConsignment(senderConsignment);
	}

	public void sendDisconnectMessage() 
	{
		JSONObject disconnectPkt = OfflineUtils.createDisconnectPkt(OfflineManager.getInstance().getConnectedDevice());
		Logger.d(TAG, "Going to send disconnect packet");
		SenderConsignment senderConsignment;
		senderConsignment = new SenderConsignment.Builder(disconnectPkt.toString(), 
							OfflineConstants.TEXT_TOPIC).ackRequired(false).persistance(false).build();
		OfflineManager.getInstance().sendConsignment(senderConsignment);
	}

	public FileTransferModel getConvMessageFromCurrentSendingFiles(long msgId) 
	{
		if (currentSendingFiles.containsKey(msgId)) {
			return currentSendingFiles.get(msgId);
		}
		return null;
	}

	private FileSavedState getUploadFileState(ConvMessage convMessage, File file) 
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		synchronized (currentSendingFiles) {
			if (currentSendingFiles.containsKey(msgId)) 
			{
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), 
						currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
			} 
			else if (convMessage.getState().equals(State.SENT_UNCONFIRMED)) 
			{
				fss = new FileSavedState(FTState.ERROR, (int) file.length(), 0);
			} 
			else 
			{
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
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
	private FileSavedState getDownloadFileState(ConvMessage convMessage, File file) 
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

		synchronized (currentReceivingFiles) 
		{

			if (currentReceivingFiles.containsKey(msgId)) 
			{
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(),
						currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
			} 
			else 
			{
				if (file.exists() || (hikeFile.getHikeFileType() == HikeFileType.CONTACT))
				{
					fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
				}
				else 
				{
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
		if (OfflineUtils.isConnectedToSameMsisdn(fileTransferModel.getPacket(), OfflineManager.getInstance().getConnectedDevice())) 
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

	private void showTransferProgress(FTViewHolder holder, FileSavedState fss,
			long msgId, HikeFile hikeFile, boolean isSent) 
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
			if (!currentReceivingFiles.containsKey(msgId))
				return;
			else
				num = currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}

		long progress = (((long) num * OfflineConstants.CHUNK_SIZE * 100) / hikeFile.getFileSize());
		Logger.d(TAG, "CurrentSizeReceived: " + num + " FileSize: "+ hikeFile.getFileSize() + 
				" Progress -> " + progress + "  msgId  --->" + msgId);

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

			Logger.d("Spinner", "Msg Id is......... " + msgId + ".........holder.circularProgress="
					+ holder.circularProgress.getCurrentProgress() * 100 + " Progress=" + progress);

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
					holder.circularProgress.setAnimatedProgress( (int) (holder.circularProgress.getCurrentProgress() * 100), (int) progress, 300);
				}
				else
					holder.circularProgress.setAnimatedProgress((int) progress, (int) progress + (int) (animatedProgress * 100), 6 * 1000);
			}
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
	}

	public void setupFileState(FTViewHolder holder, FileSavedState fss,
			long msgId, HikeFile hikeFile, boolean isSent, boolean ext) 
	{
		int playImage = -1;
		int retryImage = R.drawable.ic_retry_image_video;
		if (!ext) {
			playImage = R.drawable.ic_videoicon;
		}
		holder.ftAction.setVisibility(View.GONE);
		holder.circularProgressBg.setVisibility(View.GONE);
		holder.initializing.setVisibility(View.GONE);
		holder.circularProgress.setVisibility(View.GONE);
		switch (fss.getFTState()) {
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
			if (hikeFile.getHikeFileType() == HikeFileType.VIDEO && !ext) {
				holder.ftAction.setImageResource(playImage);
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			break;
		case ERROR:
			if (isSent) {
				holder.ftAction.setImageResource(retryImage);
				holder.ftAction.setContentDescription(context.getResources()
						.getString(R.string.content_des_retry_file_download));
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			break;
		default:
			break;
		}
		holder.ftAction.setScaleType(ScaleType.CENTER);
	}

	public void handleRetryButton(ConvMessage convMessage) 
	{
		if (OfflineManager.getInstance().getOfflineState() != OFFLINE_STATE.CONNECTED) 
		{
			HikeMessengerApp.getInstance().showToast(
					"You are not connected..!! Kindly connect.");
			return;
		}
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		Logger.d(TAG, "Hike File type is: "
				+ hikeFile.getHikeFileType().ordinal());

		File f = hikeFile.getFile();
		if (f.exists()) {
			sendFile(convMessage);
		} else {
			HikeMessengerApp.getInstance().showToast("File not found.!!");
		}
	}

	public void deleteRemainingReceivingFiles() 
	{
		if (currentReceivingFiles.size() > 0) 
		{
			ArrayList<Long> rMsgIds = new ArrayList<>(currentReceivingFiles.size());

			for (Entry<Long, FileTransferModel> itr : currentReceivingFiles
					.entrySet()) 
			{
				rMsgIds.add(itr.getKey());
			}

			ChatThreadUtils.deleteMessagesFromDb(rMsgIds, false, rMsgIds.get(rMsgIds.size() - 1), 
												OfflineManager.getInstance().getConnectedDevice());
			
			final ConvMessage deleteFilesConvMessage = OfflineUtils.createOfflineInlineConvMessage(
					 OfflineManager.getInstance().getConnectedDevice(),context.getString(R.string.files_not_received),
					OfflineConstants.OFFLINE_FILES_NOT_RECEIVED_TYPE);
			
			HikeConversationsDatabase.getInstance().addConversationMessages(deleteFilesConvMessage, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, deleteFilesConvMessage);
		}
	}

	private void toggleToAndFromField(JSONObject message) throws JSONException 
	{
		message.put(HikeConstants.FROM,OfflineManager.getInstance().getConnectedDevice());
		message.remove(HikeConstants.TO);
	}

	public void shutDown(TException tException) 
	{
		currentReceivingFiles.clear();
		currentSendingFiles.clear();
	}

	@Override
	public void onError(SenderConsignment senderConsignment, ERRORCODES errorCode) 
	{
		HikeMessengerApp.getInstance().showToast(OfflineUtils.getErrorString(errorCode));
		
		switch(errorCode)
		{
		case NOT_CONNECTED:
			Transporter.getInstance().publishWhenConnected(senderConsignment);
			break;
		case NOT_ENOUGH_MEMORY:
		case SD_CARD_NOT_PRESENT:
		case SD_CARD_NOT_WRITABLE:
			try 
			{
				removeFromCurrentSendingFile(OfflineUtils.getMsgId(new JSONObject(senderConsignment.getMessage())));
				HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			} 
			catch (JSONException e) 
			{
				HikeMessengerApp.getInstance().showToast("JSONException dude... Ask the other guy to send proper message");
				e.printStackTrace();
			}
			break;
		default:
			
		}
	}

}
