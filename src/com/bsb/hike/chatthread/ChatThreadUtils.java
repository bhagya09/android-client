package com.bsb.hike.chatthread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthemes.HikeChatThemeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FTMessageBuilder;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MovingList;
import com.bsb.hike.models.Mute;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatThreadUtils
{
	private static final String TAG = "ChatThreadUtils";

	public static boolean isWT1RevampEnabled(Context context)
	{
		boolean wtRevamp = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WT_1_REVAMP_ENABLED, false);
		return wtRevamp;
	}

	public static boolean isMessageInfoEnabled() {
		boolean enabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MESSAGE_INFO_ENABLED, false);

		return enabled;
	}
	public static boolean isCustomChatThemeEnabled()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CUSTOM_CHATTHEME_ENABLED, false);
	}

	public static boolean disableOverlayEffectForCCT()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CUSTOM_CHATTHEME_DISABLE_OVERLAY, false);

	}

	protected static void playUpDownAnimation(Context context, final View view)
	{
		if (view == null)
		{
			return;
		}

		Animation an = AnimationUtils.loadAnimation(context, R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
			}
		});
		view.startAnimation(an);
	}
	
	protected static void playPinUpAnimation(Context context, final View view, int animId)
	{
		if (view == null)
		{
			return;
		}
		
		Animation an = AnimationUtils.loadAnimation(context, animId);
		
		an.setAnimationListener(new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.VISIBLE);
			}
		});
		
		view.startAnimation(an);
	}

	/**
	 * This method is used to add pin related parameters in the convMessage
	 * 
	 * @param convMessage
	 */
	protected static void modifyMessageToPin(Context context, ConvMessage convMessage)
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.PIN_MESSAGE, 1);
			convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
			convMessage.setMetadata(jsonObject);
			convMessage.setHashMessage(HikeConstants.HASH_MESSAGE_TYPE.HASH_PIN_MESSAGE);
		}
		catch (JSONException je)
		{
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
			je.printStackTrace();
		}
	}

	protected static boolean checkMessageTypeFromHash(Context context, ConvMessage convMessage, String hashType)
	{
		Pattern p = Pattern.compile("(?i)" + hashType + ".*", Pattern.DOTALL);
		String message = convMessage.getMessage();
		if (p.matcher(message).matches())
		{

			convMessage.setMessage(message.substring(hashType.length()).trim());
			return true;
		}
		return false;
	}

	protected static void doBulkMqttPublish(JSONArray ids, String msisdn)
	{
		JSONObject jsonObject = new JSONObject();

		try
		{
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
			jsonObject.put(HikeConstants.TO, msisdn);
			jsonObject.put(HikeConstants.DATA, ids);
		}

		catch (JSONException e)
		{
			Logger.wtf(TAG, "Exception in Adding bulk messages : " + e.toString());
		}

		HikeMqttManagerNew.getInstance().sendMessage(jsonObject, MqttConstants.MQTT_QOS_ONE);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MSG_READ, msisdn);
	}

	protected static void clearTempData(Context context)
	{
		Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}
	
	protected static void uploadFile(Context context, String msisdn, String filePath, HikeFileType fileType, boolean isConvOnHike, int attachmentType)
	{
		uploadFile(context, msisdn, filePath, fileType, isConvOnHike, attachmentType, null);
	}

	protected static void uploadFile(Context context, String msisdn, String filePath, HikeFileType fileType, boolean isConvOnHike, int attachmentType, String caption)
	{
		Logger.i(TAG, "upload file , filepath " + filePath + " filetype " + fileType);
		initialiseFileTransfer(context, msisdn, filePath, null, fileType, null, false, -1, false, isConvOnHike, attachmentType, caption);
	}
	
	protected static void initiateFileTransferFromIntentData(Context context, String msisdn, String fileType, String filePath, boolean convOnHike, int attachmentType)
	{
		initiateFileTransferFromIntentData(context, msisdn, fileType, filePath, null, false, -1, convOnHike, attachmentType);
	}

	protected static void initiateFileTransferFromIntentData(Context context, String msisdn, String fileType, String filePath, String fileKey, boolean isRecording,
			long recordingDuration, boolean convOnHike, int attachmentType)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

		Logger.d(TAG, "Forwarding file- Type:" + fileType + " Path: " + filePath);

		Logger.d("Suyash", "ChThUtil : isCloudMediaUri" + Utils.isPicasaUri(filePath));
		initialiseFileTransfer(context, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, convOnHike, attachmentType);
	}

	protected static void initiateFileTransferFromIntentData(Context context, String msisdn, String fileType, String filePath, String fileKey, boolean isRecording,
															 long recordingDuration, boolean convOnHike, int attachmentType, String caption)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

		Logger.d(TAG, "Forwarding file- Type:" + fileType + " Path: " + filePath);

		Logger.d("Suyash", "ChThUtil : isCloudMediaUri" + Utils.isPicasaUri(filePath));
		initialiseFileTransfer(context, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, convOnHike, attachmentType,caption);
	}

	protected static void initialiseFileTransfer(Context context, String msisdn, String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording,
												 long recordingDuration, boolean isForwardingFile, boolean convOnHike, int attachmentType)
	{
		initialiseFileTransfer(context, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, isForwardingFile, convOnHike, attachmentType, null);
	}
	protected static void initialiseFileTransfer(Context context, String msisdn, String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording,
			long recordingDuration, boolean isForwardingFile, boolean convOnHike, int attachmentType, String caption)
	{
		clearTempData(context);

		if (filePath == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_3, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "InitialiseFileTransfer - File Path is null");
			return;
		}
		File file = new File(filePath);
		Logger.d(TAG, "File size: " + file.length() + " File name: " + file.getName());

		boolean skipMaxSizeCheck = isMaxSizeUploadableFile(hikeFileType,context);

		if (!skipMaxSizeCheck && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(context, R.string.max_file_size, Toast.LENGTH_SHORT).show();
			if (hikeFileType == HikeFileType.VIDEO) {
				Utils.recordEventMaxSizeToastShown(ChatAnalyticConstants.VIDEO_MAX_SIZE_TOAST_SHOWN, getChatThreadType(msisdn), msisdn, file.length());
			}
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_1_3, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "InitialiseFileTransfer - Max size limit reached.");
			return;
		}
		FTMessageBuilder.Builder mBuilder = new FTMessageBuilder.Builder()
				.setMsisdn(msisdn)
				.setSourceFile(file)
				.setFileKey(fileKey)
				.setFileType(fileType)
				.setHikeFileType(hikeFileType)
				.setRec(isRecording)
				.setForwardMsg(isForwardingFile)
				.setRecipientOnHike(convOnHike)
				.setRecordingDuration(recordingDuration)
				.setAttachement(attachmentType)
				.setCaption(caption);
		mBuilder.build();
				
	}

	protected static void onShareFile(Context context, String msisdn, Intent intent, boolean isConvOnHike)
	{
		String fileKey = null;

		if (intent.hasExtra(HikeConstants.Extras.FILE_KEY))
		{
			fileKey = intent.getStringExtra(HikeConstants.Extras.FILE_KEY);
		}
		String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
		String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);
		String caption = intent.getStringExtra(HikeConstants.CAPTION);
		int attachmentType = FTAnalyticEvents.FILE_ATTACHEMENT;

		boolean isRecording = false;
		long recordingDuration = -1;

		if (intent.hasExtra(HikeConstants.Extras.RECORDING_TIME))
		{
			recordingDuration = intent.getLongExtra(HikeConstants.Extras.RECORDING_TIME, -1);
			isRecording = true;
			fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
		}

		Logger.d("FileSelect", "Sharing file path = " + filePath);
		if (filePath == null)
		{
			Toast.makeText(context, R.string.unknown_file_error, Toast.LENGTH_SHORT).show();
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_5, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "OnShareFile - Unsupprted file");
		}
		else
		{
			ChatThreadUtils.initiateFileTransferFromIntentData(context, msisdn, fileType, filePath, fileKey, isRecording, recordingDuration, isConvOnHike, attachmentType,caption);
		}
	}

	protected static boolean shouldShowLastSeen(String msisdn, Context context, boolean convOnHike, boolean isBlocked)
	{
		if (Utils.isFavToFriendsMigrationAllowed() && !ContactManager.getInstance().isTwoWayFriend(msisdn))
		{
			return false; // We do not want to show the last seen in this case if the user is not 2way friend
		}

		if (convOnHike && !isBlocked && !BotUtils.isBot(msisdn) && !OfflineUtils.isConnectedToSameMsisdn(msisdn))
		{
			return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}

	protected static boolean checkNetworkError()
	{
		return HikeMessengerApp.networkError;
	}

	protected static void initialiseLocationTransfer(Context context, String msisdn, double latitude, double longitude, int zoomLevel, boolean convOnHike, boolean newConvIfnotExist)
	{
		FTMessageBuilder.Builder msgBuilder = new FTMessageBuilder.Builder()
		.setMsisdn(msisdn)
		.setLatitude(latitude)
		.setLongitude(longitude)
		.setZoomLevel(zoomLevel)
		.setRecipientOnHike(convOnHike)
		.setNewConvIfnotExist(newConvIfnotExist)
		.setHikeFileType(HikeFileType.LOCATION);
		msgBuilder.build();
	}

	protected static void initialiseContactTransfer(Context context, String msisdn, JSONObject contactJson, boolean convOnHike)
	{
		Logger.i(TAG, "initiate contact transfer " + contactJson.toString());
		FTMessageBuilder.Builder msgBuilder = new FTMessageBuilder.Builder()
			.setMsisdn(msisdn)
			.setContactJson(contactJson)
			.setRecipientOnHike(convOnHike)
			.setNewConvIfnotExist(true)
			.setHikeFileType(HikeFileType.CONTACT);
			msgBuilder.build();
	}

	protected static int incrementDecrementMsgsCount(int var, boolean isMsgSelected)
	{
		return isMsgSelected ? var + 1 : var - 1;
	}

	public static void deleteMessagesFromDb(ArrayList<Long> msgIds, boolean deleteMediaFromPhone, long lastMsgId, String msisdn)
	{
		boolean isLastMessage = (msgIds.contains(lastMsgId));
		Bundle bundle = new Bundle();
		bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, isLastMessage);
		bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, deleteMediaFromPhone);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
	}

	protected static void setStickerMetadata(ConvMessage convMessage, Sticker sticker, String source)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(StickerManager.CATEGORY_ID, sticker.getCategoryId());

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());

			if (!source.equalsIgnoreCase(StickerManager.FROM_OTHER))
			{
				metadata.put(StickerManager.SEND_SOURCE, source);
			}

			metadata.put(StickerManager.STICKER_TYPE, sticker.getStickerType().ordinal());

			convMessage.setMetadata(metadata);
			Logger.d(TAG, "metadata: " + metadata.toString());
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Invalid JSON", e);
		}
	}

	protected static ConvMessage getChatThemeConvMessage(Context context, long timestamp, String bgId, Conversation conv, boolean isCustom)
	{

		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();
		ConvMessage convMessage;
		try
		{
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.BG_ID, bgId);
			data.put(HikeConstants.CUSTOM, isCustom);

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.CHAT_BACKGROUD);
			jsonObject.put(HikeConstants.TO, conv.getMsisdn());
			jsonObject.put(HikeConstants.FROM, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, ""));

			convMessage = new ConvMessage(jsonObject, conv, context, true);

		}
		catch (JSONException e)
		{
			e.printStackTrace();
			convMessage = null;
		}

		return convMessage;
	}
	
	protected static void setPokeMetadata(ConvMessage convMessage)
	{
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.POKE, true);
			convMessage.setMetadata(metadata);
		}

		catch (JSONException e)
		{
			Logger.e(TAG, "Invalid JSON in sendPoke() : " + e.toString());
		}
	}
	
	protected static ConvMessage checkNUpdateFTMsg(Context context, ConvMessage message)
	{
		if (message.isSent() && message.isFileTransferMessage())
		{
			if (message.isOfflineMessage())
			{
				ConvMessage msg = OfflineController.getInstance().getMessage(message.getMsgID());
				return msg;
			}
			ConvMessage msg = FileTransferManager.getInstance(context).getMessage(message.getMsgID());
			return msg;
		}
		return null;
	}
	
	//Adding Channel Seletor to take decision based on Online or Offline message
	protected static void publishReadByForMessage(ConvMessage message, HikeConversationsDatabase mConversationDb, String msisdn,IChannelSelector channelSelector)
	{
		message.setState(ConvMessage.State.RECEIVED_READ);
		mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal(), msisdn);
		
		
		if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			// For Offline Messaging
			// We are sending MR for text messages and contact transfer here and not for other file transfer 
			if((channelSelector instanceof OfflineChannel) && 
					(OfflineUtils.isContactTransferMessage(message.serialize()) ||  !message.isFileTransferMessage()))
			{
				OfflineController.getInstance().sendMR(message.serializeDeliveryReportRead());
			}
			else if(channelSelector instanceof OnlineChannel)
			{
				HikeMqttManagerNew.getInstance().sendMessage(message.serializeDeliveryReportRead(), MqttConstants.MQTT_QOS_ONE);
			}
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.MSG_READ, msisdn);
	}
	
	protected static boolean isLastMessageReceivedAndUnread(MovingList<ConvMessage> messages)
	{
		ConvMessage lastMsg = null;

		/**
		 * Extracting the last contextual message
		 */
		for (int i = messages.size() - 1; i >= 0; i--)
		{
			ConvMessage msg = messages.get(i);

			/**
			 * Do nothing if it's a typing notification
			 */
			if (msg.getTypingNotification() != null || msg.isSent())
			{
				continue;
			}

			lastMsg = msg;
			break;
		}

		if (lastMsg == null)
		{
			return false;
		}

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD || lastMsg.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
	}
	
	protected static void decrementUnreadPInCount(Conversation mConversation, boolean isActivityVisible)
	{
		if (mConversation != null)
		{
			OneToNConversationMetadata metadata = (OneToNConversationMetadata) mConversation.getMetadata();
			if (!metadata.isPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN) && isActivityVisible)
			{
				try
				{
					metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
					metadata.decrementUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
			}
		}
	}
	
	protected static void recordStickerFTUEClick()
	{
		HAManager.getInstance().record(HikeConstants.LogEvent.STICKER_FTUE_BTN_CLICK, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH);
	}
	
	/**
	 * Utility method to get Status bar height in Android phones using reflection
	 * 
	 * @param context
	 * @return
	 */
	public static int getStatusBarHeight(Context context)
	{
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
		{
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
	
	/**
	 * This method scales the image proportional to the given view height and width. By using {@code Matrix.ScaleToFit} instead of {@link ScaleType} we avoid the image view from moving
	 * up/down when keyboard opens. This method also preserves the aspect ratio of the original bitmap by calculating its new height/width opportunistically
	 * 
	 * @param drawable
	 * @param imageView
	 */
	public static void applyMatrixTransformationToImageView(Drawable drawable, ImageView imageView)
	{
		/**
		 * Drawable width and height
		 */
		float imageWidth = drawable.getIntrinsicWidth();
		float imageHeight =drawable.getIntrinsicHeight();
		
		/**
		 * View height and width
		 */
		float viewWidth = imageView.getContext().getResources().getDisplayMetrics().widthPixels;
		float viewHeight = imageView.getContext().getResources().getDisplayMetrics().heightPixels - getStatusBarHeight(imageView.getContext());
		
		RectF dst; //Destination rectangle frame in which we have to place the drawable
		/**
		 * We scale the image on the basis of the smaller dimension.
		 * We also preserve the aspect ratio of the original drawable
		 */
		if (imageWidth > imageHeight)
		{
			dst = new RectF(0, 0, viewWidth, viewHeight);
		}
		
		else
		{
			dst = new RectF(0, 0, viewWidth, viewWidth * imageHeight/imageWidth);
		}
		
		Matrix matrix = new Matrix();
		matrix.setRectToRect(new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()), dst, Matrix.ScaleToFit.FILL);
		Logger.d(TAG, "Matrix:"+ matrix.toString());
		imageView.setImageMatrix(matrix);
	}
	
	/**
	 * Returns the kind of chat thread to open based on the msisdn
	 * 
	 * @param msisdn
	 * @return
	 */
	public static String getChatThreadType(String msisdn)
	{
		if (OneToNConversationUtils.isBroadcastConversation(msisdn))
		{
			return HikeConstants.Extras.BROADCAST_CHAT_THREAD;
		}

		else if (OneToNConversationUtils.isGroupConversation(msisdn))
		{
			return HikeConstants.Extras.GROUP_CHAT_THREAD;
		}
		
		else if (BotUtils.isBot(msisdn))
		{
			return HikeConstants.Extras.BOT_CHAT_THREAD;
		}
		
		return HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD;
	}

	/**
	 * Sends nmr/mr as per pd is present in convmessage or not.Not sending MR for Offline conversation
	 * @param msisdn
	 */
	public static void sendMR(String msisdn, List<ConvMessage> unreadConvMessages, boolean readMessageExists,IChannelSelector  channelSelector)
	{
		List<Pair<Long, JSONObject>> pairList = null;
		if (readMessageExists)
		{
			// here we know which msg ids should be marked as read, therefore passing unread conv messages to db method
			pairList = HikeConversationsDatabase.getInstance().updateStatusAndSendDeliveryReport(unreadConvMessages);
		}
		else
		{
			// mark all msgs of this chat thread as read
			pairList = HikeConversationsDatabase.getInstance().updateStatusAndSendDeliveryReport(msisdn);
		}

		if (pairList == null)
		{
			return;
		}
		
		try
		{

			JSONObject dataMR = new JSONObject();

			JSONArray ids = new JSONArray();

			for (int i = 0; i < pairList.size(); i++)
			{
				Pair<Long, JSONObject> pair = pairList.get(i);
				JSONObject object = pair.second;
				if (object.has(HikeConstants.PRIVATE_DATA))
				{
					String pdString = object.optString(HikeConstants.PRIVATE_DATA);
					JSONObject pd = new JSONObject(pdString);
					if (pd != null)
					{
						String trackId = pd.optString(HikeConstants.MSG_REL_UID);
						if (trackId != null)
						{
							dataMR.putOpt(String.valueOf(pair.first), pd);
							// Logs for Msg Reliability
							MsgRelLogManager.recordMsgRel(trackId, MsgRelEventType.RECEIVER_OPENS_CONV_SCREEN, msisdn);
						}
						else
						{
							ids.put(String.valueOf(pair.first));
						}
					}
				}
				else
				{
					ids.put(String.valueOf(pair.first));
				}
			}

			Logger.d("UnreadBug", "Unread count event triggered");

			/*
			 * If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read avoid sending read notifications for group chats
			 */
			if (ids != null && ids.length() > 0)
			{
				JSONObject object = new JSONObject();
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
				object.put(HikeConstants.TO, msisdn);
				object.put(HikeConstants.DATA, ids);
				if(channelSelector instanceof OfflineChannel){
					object.put(HikeConstants.TIMESTAMP,System.currentTimeMillis()/1000);
				}
				channelSelector.postMR(object);
			}

			if (dataMR != null && dataMR.length() > 0)
			{
				JSONObject object = new JSONObject();
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ);
				object.put(HikeConstants.TO, msisdn);
				object.put(HikeConstants.DATA, dataMR);

				channelSelector.postMR(object);
			}
			Logger.d(TAG, "Unread Count event triggered");

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Utility method for returning msisdn from action:SendTo intent which is invoked from outside the application
	 * 
	 * @param intent
	 * @return
	 */
	public static String getMsisdnFromSendToIntent(Intent intent)
	{
		String smsToString = intent.getDataString();
		smsToString = Uri.decode(smsToString); //Since this is coming from an external intent, the DataString can be null"
		if (smsToString != null)
		{
			int index = smsToString.indexOf(intent.getData().getScheme() + ":");
			if (index != -1)
			{
				index += (intent.getData().getScheme() + ":").length();
				String msisdn = smsToString.substring(index, smsToString.length());
				if (msisdn != null)
				{
					return msisdn.trim();
				}
			}
		}

		return null;
	}

	public static void processTasks(final Intent intent)
	{
		String msisdn = intent.getStringExtra(HikeConstants.MSISDN);
		boolean showNotification = intent.getBooleanExtra(HikeConstants.MUTE_NOTIF, true);
		if (TextUtils.isEmpty(msisdn))
		{
			return;
		}
		//CE-765: If notification were choosen not be shown, then we reset it
		if (!showNotification) showNotification = true;
		Mute mute = new Mute.InitBuilder(msisdn).setIsMute(false).setShowNotifInMute(showNotification).setMuteDuration(0).build();
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, mute);
	}

	public static boolean isBigVideoSharingEnabled()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LARGE_VIDEO_SHARING_ENABLED, false);
	}
	/**
	 * @param convMessage
	 */
	public static  String getMessageType(ConvMessage convMessage)
	{
		if (convMessage == null)
		{
			return null;
		}

		if (convMessage.isStickerMessage())
		{
			return AnalyticsConstants.MessageType.STICKER;
		}
		/**
		 * If NO Metadata ===> It was a "Text" Msg in 1-1 Conv
		 */
		else if (convMessage.getMetadata() != null)
		{
			if (convMessage.getMetadata().isPokeMessage())
			{
				return AnalyticsConstants.MessageType.NUDGE;
			}

			List<HikeFile> list = convMessage.getMetadata().getHikeFiles();
			/**
			 * If No HikeFile List ====> It was a "Text" Msg in gc
			 */
			if (list != null)
			{
				final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				HikeFileType fileType = hikeFile.getHikeFileType();
				switch (fileType)
				{
					case CONTACT:
						return AnalyticsConstants.MessageType.CONTACT;

					case LOCATION:
						return AnalyticsConstants.MessageType.LOCATION;

					case AUDIO:
					case AUDIO_RECORDING:
						return hikeFile.getAttachmentSharedAs();

					case VIDEO:
						return AnalyticsConstants.MessageType.VEDIO;

					case IMAGE:
						return AnalyticsConstants.MessageType.IMAGE;

					case APK:
						return ChatAnalyticConstants.MessageInfoEvents.APK;

					default:
						return ChatAnalyticConstants.MessageInfoEvents.MESSAGE_INFO_FILE_TYPE_OTHER;

				}
			}
			else
			{
				return AnalyticsConstants.MessageType.TEXT;
			}
		}

		return AnalyticsConstants.MessageType.TEXT;

	}


	public static boolean isMaxSizeUploadableFile(HikeFileType hikeFileType, Context context){
		boolean skipMaxSizeCheck = (isBigVideoSharingEnabled() && hikeFileType == HikeFileType.VIDEO);
		//Do pre-compression size check as before if compression have been turned off by the user.
		if(skipMaxSizeCheck && (android.os.Build.VERSION.SDK_INT < 18
				|| !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.COMPRESS_VIDEO, true))) {
			skipMaxSizeCheck = false;
		}
		return skipMaxSizeCheck;
	}
}
