package com.bsb.hike.filetransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.analytics.AnalyticsConstants.MessageType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FTMessageBuilder {

	private static final String TAG = "FTMessageBuilder";

	private static Runnable buildConvMsg(final Init<?> builder)
	{
		Runnable buildRunnable = new Runnable() {
			
			@Override
			public void run() {
				List<ConvMessage> ftConvMsgs = buildFTMessage(builder);
				if(builder.listener != null)
				{
					builder.listener.onFTConvMsgCreation(ftConvMsgs);
				}
				else
				{
					Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
					/*
					 * Sharing with multiple contacts.
					 */
					if(builder.contactList != null && !builder.contactList.isEmpty())
					{
						FileTransferManager.getInstance(mContext).uploadFile(builder.contactList, ftConvMsgs, builder.fileKey);
					}
					else
					{
						for (Iterator<ConvMessage> iterator = ftConvMsgs.iterator(); iterator.hasNext();)
						{
							ConvMessage convMessage = (ConvMessage) iterator.next();
							if(builder.hikeFileType == HikeFileType.CONTACT || builder.hikeFileType == HikeFileType.LOCATION)
							{
								FileTransferManager.getInstance(mContext).uploadContactOrLocation(convMessage, builder.hikeFileType == HikeFileType.CONTACT);
							}
							else
							{
								FileTransferManager.getInstance(mContext).uploadFile(convMessage, builder.fileKey);
							}
						}
					}
				}
			}
		};
		return buildRunnable;
		
	}

	private static List<ConvMessage> buildFTMessage(final Init<?> builder)
	{
		List<ConvMessage> ftConvMsgs = null;
		switch (builder.hikeFileType) {
		case CONTACT:
		case LOCATION:
			ftConvMsgs = createLocationOrContactConvMessage(builder);
			break;
		default:
			ftConvMsgs = createMediaOrFileConvMessage(builder);
			break;
		}
		return ftConvMsgs;
	}

	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}

		@Override
		public void build() {
			FTHandler.getInstance().postRunnableWithDelay(buildConvMsg(this), 0);
		}

		public List<ConvMessage> buildInSync()
		{
			return buildFTMessage(this);
		}
	}

	/**
	 * Creates ft conv message for Media or Files
	 * 
	 * @param builder
	 * @return List<ConvMessage>
	 */
	private static List<ConvMessage> createMediaOrFileConvMessage(Init<?> builder)
	{
		List<ConvMessage> ftMessages = new ArrayList<ConvMessage>();
		try
		{
			System.gc();
			File destinationFile;
			String fileName = Utils.getFinalFileName(builder.hikeFileType);
			JSONObject metadata;
			if (builder.cloudMediaUri == null)
			{
				destinationFile = builder.sourceFile;
				fileName = destinationFile.getName();
				String thumbnailString = null;
				BitmapDrawable bitDrawable = HikeMessengerApp.getLruCache().get(GalleryImageLoader.GALLERY_KEY_PREFIX + destinationFile.getPath());
				Bitmap thumbnail = null;
				if(bitDrawable != null)
				{
					thumbnail = ImageWorker.drawableToBitmap(bitDrawable);
				}
				else
				{
					thumbnail = FTUtils.getMediaThumbnail(builder.hikeFileType, destinationFile, builder.fileKey);
				}
				if (thumbnail != null)
				{
					byte [] tBytes = FTUtils.compressThumb(thumbnail, builder.hikeFileType);
					thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
					thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
				}
				metadata = getFileOrMediaMetadata(builder, fileName, thumbnailString, thumbnail);
			}
			else
			{
				destinationFile = Utils.getOutputMediaFile(builder.hikeFileType, fileName, true);
				if (TextUtils.isEmpty(fileName))
				{
					fileName = destinationFile.getName();
				}
				metadata = getFileOrMediaMetadata(builder, fileName, null, null);
			}
			/*
			 * Handling of Sharing with multiple contacts case.
			 */
			if (builder.contactList != null && !builder.contactList.isEmpty())
			{
				for (ContactInfo contact : builder.contactList)
				{
					ConvMessage msg = createConvMessage(fileName, metadata, builder, contact.getMsisdn(), contact.isOnhike());
					ftMessages.add(msg);
				}
				ConvMessage ftMessage = ftMessages.get(0);
				builder.messageList = new ArrayList<ConvMessage>();
				builder.messageList.add(ftMessage);
				MultipleConvMessage multiConMsg = new MultipleConvMessage(builder.messageList, builder.contactList);
				HikeConversationsDatabase.getInstance().addConversations(multiConMsg.getMessageList(), multiConMsg.getContactList(),false);
				for (int i=1 ; i < ftMessages.size() ; i++)
				{
					ftMessages.get(i).setMsgID(ftMessage.getMsgID() + i);
				}
				multiConMsg.sendPubSubForConvScreenMultiMessage();
			}
			else
			{
				ConvMessage ftMessage = createConvMessage(fileName, metadata, builder, builder.msisdn, builder.isRecipientOnHike);
				ConvMessage convMessageObject = ftMessage;
				if(convMessageObject.isBroadcastConversation())
				{
					convMessageObject.setMessageOriginType(OriginType.BROADCAST);
				}

				ftMessages.add(ftMessage);
				if(!builder.isOffline)
				{
					HikeConversationsDatabase.getInstance().addConversationMessages(convMessageObject,true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);
				}

				MsgRelLogManager.startMessageRelLogging(ftMessage, MessageType.MULTIMEDIA);
				
				HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessageObject);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception on creation of LocationOrContact ConvMessage : ", e);
			e.printStackTrace();
		}
		return ftMessages;
	}

	/**
	 * Creates ft conv message for Contact or Location
	 * 
	 * @param builder
	 * @return ArrayList<ConvMessage>
	 */
	private static List<ConvMessage> createLocationOrContactConvMessage(Init<?> builder)
	{
		List<ConvMessage> ftMessages = null;
		try
		{
			JSONObject metadata;
			metadata = getContactOrLocationMetadata(builder);

			String hikeMessage = builder.hikeFileType == HikeFileType.LOCATION ? HikeConstants.LOCATION_FILE_NAME : HikeConstants.CONTACT_FILE_NAME;
			ConvMessage ftMessage = createConvMessage(hikeMessage, metadata, builder, builder.msisdn, builder.isRecipientOnHike);
			
			if (TextUtils.isEmpty(builder.fileKey))
			{
				MsgRelLogManager.startMessageRelLogging(ftMessage, MessageType.MULTIMEDIA);
				
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, ftMessage);
			}
			ftMessages = new ArrayList<ConvMessage>();
			ftMessages.add(ftMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, ftMessage);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception on creation of LocationOrContact ConvMessage : ", e);
		}
		return ftMessages;
	}

	/**
	 * Creates ft conv message for media or files
	 * 
	 * @param hikeMessage
	 * @param metadata
	 * @param builder
	 * @param msisdn
	 * @return ConvMessage
	 */
	private static ConvMessage createConvMessage(String hikeMessage, JSONObject metadata, Init<?> builder, String msisdn, boolean isOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(hikeMessage, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isOnhike);
		if(builder.hikeFileType == HikeFileType.LOCATION || builder.hikeFileType == HikeFileType.CONTACT)
		{
			if(convMessage.isBroadcastConversation())
			{
				convMessage.setMessageOriginType(OriginType.BROADCAST);
			}
			HikeConversationsDatabase.getInstance().addConversationMessages(convMessage,builder.newConvIfnotExist);
		}
		return convMessage;
	}

	/**
	 * Creates meta data for Location or Contact
	 * 
	 * @param builder
	 * @return JSONObject
	 */
	private static JSONObject getContactOrLocationMetadata(Init<?> builder) throws JSONException
	{
		JSONArray files = new JSONArray();
		switch (builder.hikeFileType) {
		case LOCATION:
			files.put(new HikeFile(builder.latitude, builder.longitude,builder.zoomLevel, null, null, null, true).serialize());
			break;
		case CONTACT:
			builder.contactJson.put(HikeConstants.FILE_NAME, builder.contactJson.optString(HikeConstants.NAME, HikeConstants.CONTACT_FILE_NAME));
			builder.contactJson.put(HikeConstants.CONTENT_TYPE, HikeConstants.CONTACT_CONTENT_TYPE);
			files.put(builder.contactJson);
			break;
		default:
			break;
		}
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}

	/**
	 * Creates meta data for file or media
	 * 
	 * @param builder
	 * @param fileName
	 * @param thumbnailString
	 * @param thumbnail
	 * @return JSONObject
	 */
	private static JSONObject getFileOrMediaMetadata(Init<?> builder, String fileName, String thumbnailString, Bitmap thumbnail) throws JSONException
	{
		JSONArray files = new JSONArray();
		String fileType = TextUtils.isEmpty(builder.fileType) ? HikeFileType.toString(builder.hikeFileType) : builder.fileType;
		files.put(new HikeFile(fileName, fileType, thumbnailString, thumbnail, builder.recordingDuration, builder.sourceFile.getPath(),
				(int)(builder.sourceFile.length()), true, FTUtils.getImageQuality(), builder.attachement).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		if(!TextUtils.isEmpty(builder.caption))
		{
			metadata.put(HikeConstants.CAPTION, builder.caption);
		}

		return metadata;
	}

	protected static abstract class Init<S extends Init<S>>
	{
		private String msisdn;
		private File sourceFile;
		private String fileKey;
		private String fileType;
		private HikeFileType hikeFileType;
		private boolean isRec;
		private boolean isForwardMsg;
		private boolean isRecipientOnHike;
		private long recordingDuration;
		private int attachement;
		private String caption;

		private List<ContactInfo> contactList;
		private List<ConvMessage> messageList;

		private Uri cloudMediaUri;

		private double latitude;
		private double longitude;
		private int zoomLevel;

		private JSONObject contactJson;
		private boolean newConvIfnotExist;
		private boolean isOffline;

		private FTConvMsgCreationListener listener;

		protected abstract S self();

		/**
		 * Sets the msisdn of the recipient
		 * 
		 * @param msisdn
		 */
		public S setMsisdn(String msisdn)
		{
			this.msisdn = msisdn;
			return self();
		}

		/**
		 * Sets the source file
		 * 
		 * @param sourceFile
		 * @return
		 */
		public S setSourceFile(File sourceFile)
		{
			this.sourceFile = sourceFile;
			return self();
		}

		/**
		 * Sets the fileKey of request
		 * 
		 * @param fileKey
		 */
		public S setFileKey(String fileKey)
		{
			this.fileKey = fileKey;
			return self();
		}
		
		/**
		 * Sets the file type
		 * 
		 * @param fileType
		 */
		public S setFileType(String fileType)
		{
			this.fileType = fileType;
			return self();
		}

		/**
		 * Set caption associated with file
		 *
		 * @param argCaption
		 */
		public S setCaption(String argCaption)
		{
			if(!TextUtils.isEmpty(argCaption))
				this.caption = argCaption;

			return self();
		}

		/**
		 * Sets the hike file type
		 * 
		 * @param hikeFileType
		 */
		public S setHikeFileType(HikeFileType hikeFileType) {
			this.hikeFileType = hikeFileType;
			return self();
		}

		/**
		 * Set true if audio recording. 
		 * 
		 * @param isRec
		 */
		public S setRec(boolean isRec) {
			this.isRec = isRec;
			return self();
		}

		/**
		 * Set true if it is a forwarded message
		 * 
		 * @param isForwardMsg
		 */
		public S setForwardMsg(boolean isForwardMsg) {
			this.isForwardMsg = isForwardMsg;
			return self();
		}

		/**
		 * Set true if Recipient is on hike
		 * 
		 * @param isRecipientOnHike
		 */
		public S setRecipientOnHike(boolean isRecipientOnHike) {
			this.isRecipientOnHike = isRecipientOnHike;
			return self();
		}

		/**
		 * Set recording duration of voice recorded
		 * 
		 * @param recordingDuration
		 */
		public S setRecordingDuration(long recordingDuration) {
			this.recordingDuration = recordingDuration;
			return self();
		}

		/**
		 * Sets attachment type for analytics
		 * 
		 * @param attachement
		 */
		public S setAttachement(int attachement) {
			this.attachement = attachement;
			return self();
		}

		/**
		 * Sets contact list in case of multiple user selected to send a file
		 * 
		 * @param contactList
		 */
		public S setContactList(List<ContactInfo> contactList) {
			this.contactList = (ArrayList<ContactInfo>) contactList;
			return self();
		}

		/**
		 * Sets URI in case of cloud app's media
		 * 
		 * @param cloudMediaUri
		 */
		public S setCloudMediaUri(Uri cloudMediaUri) {
			this.cloudMediaUri = cloudMediaUri;
			return self();
		}

		/**
		 * Sets latitude
		 * 
		 * @param latitude
		 */
		public S setLatitude(double latitude) {
			this.latitude = latitude;
			return self();
		}

		/**
		 * Sets longitude
		 * 
		 * @param longitude
		 */
		public S setLongitude(double longitude) {
			this.longitude = longitude;
			return self();
		}

		/**
		 * Sets zoom level for google static maps api
		 * 
		 * @param zoomLevel
		 */
		public S setZoomLevel(int zoomLevel) {
			this.zoomLevel = zoomLevel;
			return self();
		}

		/**
		 * Sets Json for contact sharing
		 * 
		 * @param contactJson
		 */
		public S setContactJson(JSONObject contactJson) {
			this.contactJson = contactJson;
			return self();
		}

		/**
		 * Sets true if conv message does not exist
		 * 
		 * @param newConvIfnotExist
		 */
		public S setNewConvIfnotExist(boolean newConvIfnotExist) {
			this.newConvIfnotExist = newConvIfnotExist;
			return self();
		}

		/**
		 * Sets listener to get created ft conv message
		 * 
		 * @param listener
		 */
		public S setListener(FTConvMsgCreationListener listener) {
			this.listener = listener;
			return self();
		}

		/**
		 * Sets true if user is sending file in offline mode.
		 * 
		 * @param mOffline
		 */
		public S setIsOffline(boolean mOffline) {
			this.isOffline = mOffline;
			return self();
		}

		public abstract void build();
	}

	/**
	 * Interface to provide FT conversation message when created.
	 */
	public static interface FTConvMsgCreationListener
	{
		void onFTConvMsgCreation(List<ConvMessage> convMsgs);
	}
}
