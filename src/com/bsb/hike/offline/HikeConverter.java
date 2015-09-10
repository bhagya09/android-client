package com.bsb.hike.offline;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.offline.OfflineConstants.MessageType;
import com.bsb.hike.service.MqttMessagesManager;
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

	private Context context;
	private OfflineMessagesManager messagesManager = null;
	private OfflineFileManager fileManager;
	
	public HikeConverter(OfflineFileManager fileManager) 
	{
		this.fileManager = fileManager;
		init();
	}

	private void init() {
		context = HikeMessengerApp.getInstance().getApplicationContext();
	}

	public SenderConsignment getFileConsignment(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			int attachmentType, String msisdn, String apkLabel)
	{
		ConvMessage convMessage = getConvMessageForFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, apkLabel);
		if (convMessage != null)
			return getFileConsignment(convMessage, false);
		else
			return null;
	}

	public SenderConsignment getFileConsignment(ConvMessage convMessage, boolean persistence) 
	{
		String filePath = OfflineUtils.getFilePathFromJSON(convMessage.serialize());
		File file = new File(filePath);
		SenderConsignment senderConsignment = new SenderConsignment.Builder(convMessage.serialize().toString(), OfflineConstants.FILE_TOPIC).file(file).persistance(persistence)
				.ackRequired(true).build();
		senderConsignment.setTag(convMessage);
		senderConsignment.setAwb(convMessage.getMsgID());

		FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0, OfflineUtils.getTotalChunks((int) file.length())), convMessage);
		fileManager.addToCurrentSendingFile(convMessage.getMsgID(), fileTransferModel);
		return senderConsignment;
	}

	private ConvMessage getConvMessageForFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			int attachmentType, String msisdn, String apkLabel)
	{
		if (filePath == null)
		{
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return null;
		}
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
	public void onTransitBegin(SenderConsignment senderConsignment)
	{
		if (senderConsignment == null || senderConsignment.getTag() == null)
		{
			return;
		}
		ConvMessage convMessage = (ConvMessage) senderConsignment.getTag();
		if (senderConsignment.getFile() != null)
		{
			
			if (convMessage.getMetadata().getSticker() == null)
			{
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				String fileType = HikeFileType.toString(hikeFile.getHikeFileType());
				long fileSize = hikeFile.getFileSize();
				SessionTracFilePOJO pojo = new SessionTracFilePOJO(fileType, fileSize);
				OfflineSessionTracking.getInstance().addToListOfFiles(convMessage.getMsgID(), pojo);
			}
			else
			{
				Sticker stk = convMessage.getMetadata().getSticker();
				String catId = stk.getCategoryId();
				String stkId = stk.getStickerId();
				SessionTrackingStickerPOJO pojo = new SessionTrackingStickerPOJO(senderConsignment.getTotalFileSize(), catId, stkId);
				OfflineSessionTracking.getInstance().addToListOfFiles(convMessage.getMsgID(), pojo);
			}
		}
		else
		{
			if (convMessage.getMetadata() != null)
			{
				if (convMessage.isFileTransferMessage())
				{
					HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
					if (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
					{
						OfflineSessionTracking.getInstance().incrementContact(true);
					}
				}
				else
				{
					OfflineSessionTracking.getInstance().incrementMsgSend(convMessage.getMetadata().isPokeMessage());
				}
			}
				
			else
			{
				OfflineSessionTracking.getInstance().incrementMsgSend(false);
			}
		}
	}

	@Override
	public void onTransitEnd(SenderConsignment senderConsignment) {
		if (senderConsignment == null || senderConsignment.getTag() == null)
		{
			return;
		}
		ConvMessage convMessage = (ConvMessage) senderConsignment.getTag();
		if (senderConsignment.getFile() != null)
		{
			SessionTracFilePOJO pojo = OfflineSessionTracking.getInstance().getFileSession(convMessage.getMsgID());
			if (pojo != null)
			{
				pojo.recordEndTime();
			}
		}
	}

	@Override
	public void onChunkSend(SenderConsignment senderConsignment) {
		fileManager.onChunkSend((ConvMessage)senderConsignment.getTag());
	}

	/**
	 * This function is called when message gets Delivered.
	 * @param senderConsignment
	 */
	@Override
	public void onMessageDelivered(SenderConsignment senderConsignment) 
	{
		if (senderConsignment == null)
		{
			Logger.d(TAG, "sender consignment is null in onMessageDelivered in HikeConverter");
			return;
		}
		
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
		Logger.d(AnalyticsConstants.MSG_REL_TAG,"Rows updated in Db for convMessage Sttae");
				
		if(!tempConvMessage.isOfflineMessage())
		{
			Logger.d(TAG, "Updating Ordinal value to Offline  for offline msgs");
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_MESSAGE_ORIGIN_TYPE, new Pair<Long, Integer>(msgId, ConvMessage.OriginType.OFFLINE.ordinal()));
		}
		if (rowsUpdated == 0) 
		{
			Logger.d(getClass().getSimpleName(), "No rows updated");
		}
		
		if (OfflineUtils.isFileTransferMessage(message)) 
		{ 
			fileManager.handleFileDelivered(msgId, tempConvMessage);
		}
		
		Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
		OfflineSessionTracking.getInstance().incrementDel();
	}


	@Override
	public void onMessageReceived(ReceiverConsignment receiverConsignment) 
	{
		JSONObject messageJSON;
		try 
		{
			messageJSON = new JSONObject(receiverConsignment.message);
			OfflineUtils.toggleToAndFromField(messageJSON, OfflineController.getInstance().getConnectedDevice());
			if(!OfflineUtils.isContactTransferMessage(messageJSON) && OfflineUtils.isFileTransferMessage(messageJSON)) 
			{
				setFileVariablesAndUpdateJSON(messageJSON);
				ConvMessage convMessage = new ConvMessage(messageJSON, context);
				
				if (!addToDatabase(convMessage))
					return;
				
				fileManager.handleMessageReceived(convMessage);
				receiverConsignment.setTag(convMessage);
			}
			else if(OfflineUtils.isMessageReadType(messageJSON))
			{
				MqttMessagesManager.getInstance(context).saveMessageRead(messageJSON);
				JSONArray serverIds = messageJSON.optJSONArray(HikeConstants.DATA);
				OfflineSessionTracking.getInstance().incrementRead(serverIds.length());
			}
			else if(OfflineUtils.isInfoPkt(messageJSON))
			{
				Logger.d(TAG, "Info Packet received ...>>" + messageJSON.toString() +"and "+messageJSON.opt(OfflineConstants.CONNECTION_ID));
				OfflineSessionTracking.getInstance().updateConnectionId(messageJSON.optLong(OfflineConstants.CONNECTION_ID));
			}
			else 
			{
				
				if (OfflineUtils.isChatThemeMessage(messageJSON))
				{
					if (messagesManager == null)
					{
						messagesManager = new OfflineMessagesManager();
					}
					messagesManager.handleChatThemeMessage(messageJSON);
					OfflineSessionTracking.getInstance().incrementMsgSend(false);
					return;
				}
				else if (OfflineUtils.isDisconnectPkt(messageJSON))
				{
					throw new OfflineException(OfflineException.USER_DISCONNECTED);
				}
				ConvMessage convMessage = new ConvMessage(messageJSON, context);
				if (OfflineUtils.isStickerMessage(messageJSON))
				{
					receiverConsignment.setTag(convMessage);
				}
				else
				{
					if (convMessage.getMetadata() != null)
						OfflineSessionTracking.getInstance().incrementMsgRec(convMessage.getMetadata().isPokeMessage());
					else
						OfflineSessionTracking.getInstance().incrementMsgRec(false);
				}
				if (!addToDatabase(convMessage))
					return;
				
				if (convMessage.getMetadata() != null && convMessage.getMetadata().getSticker()!=null)
				{
					Sticker stk = convMessage.getMetadata().getSticker();
					String catId = stk.getCategoryId();
					String stkId = stk.getStickerId();
					SessionTrackingStickerPOJO pojo = new SessionTrackingStickerPOJO(receiverConsignment.getTotalFileSize(), catId, stkId, MessageType.RECEIVED.ordinal());
					OfflineSessionTracking.getInstance().addToListOfFiles(convMessage.getMsgID(), pojo);
				}
				if(OfflineUtils.isContactTransferMessage(messageJSON))
				{
					OfflineSessionTracking.getInstance().incrementContact(false);
				}
			}
		} 
		catch (JSONException jsonException) 
		{
			OfflineController.getInstance().onDisconnect(new OfflineException(OfflineException.JSON_EXCEPTION));
		} 
		catch (OfflineException offlineException) 
		{
			OfflineController.getInstance().onDisconnect(offlineException);
		}
	}

	private boolean addToDatabase(ConvMessage convMessage)
	{
		convMessage.setMessageOriginType(OriginType.OFFLINE);
		boolean isInserted = HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		if (isInserted)
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED,convMessage);
		return isInserted;
	}

	private void setFileVariablesAndUpdateJSON(JSONObject messageJSON) throws JSONException 
	{
		JSONObject fileJSON = getFileJSONFromMetadata(messageJSON);
		Boolean isRecording = false;
		if (fileJSON.has(HikeConstants.PLAYTIME))
		{
			isRecording = true;
		}
		HikeFileType hikeFileType = HikeFileType.fromString(fileJSON.getString(HikeConstants.CONTENT_TYPE),isRecording);
		String fileName = Utils.getFinalFileName(hikeFileType, fileJSON.getString(HikeConstants.FILE_NAME));
		String filePath = OfflineUtils.getFileBasedOnType(hikeFileType.ordinal(), fileName);
		updateMessageJSON(messageJSON, filePath, fileName);
	}
	
	private JSONObject getFileJSONFromMetadata(JSONObject messageJSON) throws JSONException
	{
		JSONObject metadata;
		metadata = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
		JSONObject fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
		return fileJSON;
	}
	
	private void updateMessageJSON(JSONObject messageJSON, String filePath, String fileName) throws JSONException 
	{
		OfflineUtils.toggleToAndFromField(messageJSON, OfflineController.getInstance().getConnectedDevice());
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.FILE_PATH, filePath);
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.SOURCE_FILE_PATH,filePath);
		(messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
						.getJSONObject(0).putOpt(HikeConstants.FILE_NAME, fileName);

	}

	@Override
	public void onChunkRead(ReceiverConsignment receiverConsignment) {
		fileManager.onChunkRead((ConvMessage) receiverConsignment.getTag());
	}
	
	
	// Convmessage is stored as tag here 
	@Override
	public void onFileCompleted(ReceiverConsignment receiver) 
	{
		if (receiver.getTag() == null)
			return;
		fileManager.onFileCompleted((ConvMessage)receiver.getTag(), receiver.getFile());
	}

	public SenderConsignment getMessageConsignment(ConvMessage convMessage, boolean persistence) 
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
					stickerFile).ackRequired(true).persistance(persistence).build();
		} 
		else 
		{
			senderConsignment = new SenderConsignment.Builder(
					messageJSON.toString(), OfflineConstants.TEXT_TOPIC).ackRequired(true).persistance(persistence).build();
		}
		senderConsignment.setAwb(convMessage.getMsgID());
		senderConsignment.setTag(convMessage);
		return senderConsignment;
	}
	

	public SenderConsignment getDisconnectConsignment(String connectedDevice) 
	{
		JSONObject disconnectPkt = OfflineUtils.createDisconnectPkt(connectedDevice);
		SenderConsignment senderConsignment;
		senderConsignment = new SenderConsignment.Builder(disconnectPkt.toString(), 
							OfflineConstants.TEXT_TOPIC).ackRequired(false).persistance(false).build();
		return senderConsignment;
	}
	

	public void shutDown(TException tException) 
	{
	}

	@Override
	public void onError(SenderConsignment senderConsignment, ERRORCODES errorCode) 
	{
		HikeMessengerApp.getInstance().showToast(OfflineUtils.getErrorStringId(errorCode));
		
		switch(errorCode)
		{
		case NOT_CONNECTED:
			// TODO: SHift properly
			Logger.d(TAG,"in Not Connected");
			Transporter.getInstance().publishWhenConnected(senderConsignment);
			break;
		case NOT_ENOUGH_MEMORY:
		case SD_CARD_NOT_PRESENT:
		case SD_CARD_NOT_WRITABLE:
			try 
			{
				
				fileManager.removeFileAndUpdateView(OfflineUtils.getMsgId(new JSONObject(senderConsignment.getMessage())));
			}
			catch (JSONException e) 
			{
				//HikeMessengerApp.getInstance().showToast("JSONException dude... Ask the other guy to send proper message");
				Logger.e(TAG,"Json exception");
			}
			break;
		default:
			
		}
	}
	
	public void releaseResources() 
	{
	}

	public SenderConsignment getMRConsignement(JSONObject object) 
	{
		SenderConsignment consignment=new SenderConsignment.Builder(object.toString(), OfflineConstants.TEXT_TOPIC).ackRequired(false).persistance(true).build();
		consignment.setAwb(OfflineUtils.getMsgId(object));
		return consignment;
	}
}
