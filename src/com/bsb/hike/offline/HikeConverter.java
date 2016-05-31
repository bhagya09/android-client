package com.bsb.hike.offline;

import java.io.File;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FTMessageBuilder;
import com.bsb.hike.filetransfer.FTMessageBuilder.FTConvMsgCreationListener;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants.MessageType;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;
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

	public void buildFileConsignment(final String filePath, String fileKey, final HikeFileType hikeFileType, String fileType, final boolean isRecording,
									 final long recordingDuration, int attachmentType, String msisdn, final String apkLabel)
	{
				buildFileConsignment(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, apkLabel,null);
	}

	public void buildFileConsignment(final String filePath, String fileKey, final HikeFileType hikeFileType, String fileType, final boolean isRecording,
			final long recordingDuration, int attachmentType, String msisdn, final String apkLabel, String caption)
	{
		if (filePath == null)
		{
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return;
		}

		final File sourceFile = new File(filePath);

		if (!isFileValid(sourceFile, msisdn))
		{
			return;
		}

		FTMessageBuilder.Builder mBuilder = new FTMessageBuilder.Builder().setSourceFile(sourceFile).setFileKey(fileKey).setHikeFileType(hikeFileType).setFileType(fileType)
				.setRec(isRecording).setRecordingDuration(recordingDuration).setAttachement(attachmentType).setMsisdn(msisdn).setIsOffline(true).setCaption(caption)
				.setListener(new FTConvMsgCreationListener()
				{

					@Override
					public void onFTConvMsgCreation(List<ConvMessage> convMsgs)
					{

						if (convMsgs != null && convMsgs.size() > 0)
						{
							ConvMessage convMessage = convMsgs.get(0);

							if (convMessage == null)
							{
								return;
							}

							File file = new File(filePath);
							String fileName = file.getName();
							if (hikeFileType == HikeFileType.APK && !TextUtils.isEmpty(apkLabel))
								fileName = apkLabel + ".apk";
							HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
							updateHikeFile(hikeFile, sourceFile, hikeFileType, recordingDuration, fileName);
							convMessage.setTimestamp(System.currentTimeMillis() / 1000);
							JSONObject fileJSON = hikeFile.serialize();
							JSONObject metadata = convMessage.getMetadata().getJSON();
							JSONArray filesArray = new JSONArray();
							try
							{
								filesArray.put(fileJSON);
								metadata.put(HikeConstants.FILES, filesArray);
								MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
								messageMetadata.getHikeFiles().get(0).setFileName(fileName);
								messageMetadata.getJSON().putOpt(HikeConstants.FILE_NAME, fileName);
								convMessage.setMetadata(messageMetadata);
							}
							catch (JSONException e)
							{
								Logger.e(TAG, "JSON Exception while creating offline convmessage");
							}

							convMessage.setMessageOriginType(OriginType.OFFLINE);
							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
							HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_MESSAGE_SENT, convMessage);
							SenderConsignment senderConsignment = getFileConsignment(convMessage, false);
							if (senderConsignment != null)
							{
								OfflineController.getInstance().sendConsignment(senderConsignment);
							}
						}
					}

				});
		mBuilder.build();
	}

	private boolean isFileValid(File sourceFile,String msisdn)
	{
		if(!(sourceFile!=null && sourceFile.exists()))
		{
			return false;	
		}
		
		/*
		 * Checking file transfer limit version For V1 it was INT_MAX For V2 and above no limit is applied
		 */
		if(sourceFile.length()>Integer.MAX_VALUE)
		{
			if(!OfflineUtils.isFeautureAvailable(OfflineConstants.OFFLINE_VERSION_NUMER,
					OfflineUtils.getConnectedDeviceVersion(),OfflineConstants.UNLIMITED_FT_VERSION))
			{
				ContactInfo contactInfo  = ContactManager.getInstance().getContact(msisdn);
				String name  = msisdn;
				if(contactInfo!=null && !TextUtils.isEmpty(contactInfo.getFirstNameAndSurname()))
				{
					name = contactInfo.getFirstNameAndSurname();
				}
				HikeMessengerApp.getInstance().showToast(context.getString(R.string.upgrade_for_larger_files,name), Toast.LENGTH_LONG);
				return false;
			}
		}
		return true;
	}
	
	private void updateHikeFile(HikeFile hikeFile, File sourceFile, HikeFileType hikeFileType, long recordingDuration, String fileName)
	{
		hikeFile.setFileSize(sourceFile.length());
		hikeFile.setHikeFileType(hikeFileType);
		hikeFile.setRecordingDuration(recordingDuration);
		hikeFile.setSent(true);
		hikeFile.setFileName(fileName);
		hikeFile.setFile(sourceFile);
	}
	
	public SenderConsignment getFileConsignment(ConvMessage convMessage, boolean persistence) 
	{
		String filePath = OfflineUtils.getFilePathFromJSON(convMessage.serialize());
		File file = new File(filePath);

		/*
		 * Checking file transfer limit version For V1 it was INT_MAX For V2 and above no limit is applied
		 */

		if (file.length() > Integer.MAX_VALUE)
		{
			if (!OfflineUtils.isFeautureAvailable(OfflineConstants.OFFLINE_VERSION_NUMER, OfflineUtils.getConnectedDeviceVersion(), OfflineConstants.UNLIMITED_FT_VERSION))
			{
				String msisdn  = convMessage.getMsisdn();
				ContactInfo contactInfo  = ContactManager.getInstance().getContact(msisdn);
				String name  = msisdn;
				if(contactInfo!=null && !TextUtils.isEmpty(contactInfo.getFirstNameAndSurname()))
				{
					name = contactInfo.getFirstNameAndSurname();
				}
				HikeMessengerApp.getInstance().showToast(context.getString(R.string.upgrade_for_larger_files,name), Toast.LENGTH_LONG);
				return null;
			}
		}

		SenderConsignment senderConsignment = new SenderConsignment.Builder(convMessage.serialize().toString(), OfflineConstants.FILE_TOPIC).file(file).persistance(persistence)
				.ackRequired(true).build();
		senderConsignment.setTag(convMessage);
		senderConsignment.setAwb(convMessage.getMsgID());

		FileTransferModel fileTransferModel = new FileTransferModel(new TransferProgress(0, OfflineUtils.getTotalChunks(file.length())), convMessage);
		fileManager.addToCurrentSendingFile(convMessage.getMsgID(), fileTransferModel);
		return senderConsignment;
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
		if(message!=null)
		OfflineUtils.saveDeliveryReceipt(msgId,tempConvMessage);

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
				OfflineController.getInstance().setConnectedClientInfo(messageJSON);
				OfflineController.getInstance().setConnectingDeviceAsConnected();
				OfflineController.getInstance().setOfflineState(OfflineConstants.OFFLINE_STATE.CONNECTED);
				OfflineController.getInstance().sendConnectedCallback();
			}
			else if(OfflineUtils.isVoipPacket(messageJSON))
			{
				VoIPUtils.handleVOIPPacket(context, messageJSON);
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
					throw new OfflineException(OfflineException.PEER_DISCONNECTED);
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
		HikeMessengerApp.getInstance().showToast(OfflineUtils.getErrorStringId(errorCode),Toast.LENGTH_SHORT);
		
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
			Transporter.getInstance().publishWhenConnected(senderConsignment);
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
