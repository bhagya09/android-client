package com.bsb.hike.models;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ConvMessagePacketKeys;
import com.bsb.hike.HikeConstants.MESSAGE_TYPE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.StringUtils;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.platform.ContentLove;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SearchManager.Searchable;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomMessageTextView.DimentionMatrixHolder;
import com.bsb.hike.view.CustomMessageTextView.ViewDimentions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConvMessage implements Searchable, DimentionMatrixHolder, Unique, Comparable<ConvMessage>

{
	private boolean isBlockAddHeader;

	private long msgID; // this corresponds to msgID stored in sender's DB
	
	private long mappedMsgId; // this corresponds to msgID stored in receiver's
								// DB

	private String mMessage;

	private String mMsisdn;

	private long mTimestamp;

	private boolean mIsSent;

	private boolean mIsSMS;

	private State mState = State.SENT_UNCONFIRMED;

	private boolean mInvite;

	private MessageMetadata metadata;

	private String groupParticipantMsisdn;

	private ParticipantInfoState participantInfoState;

	private boolean isFileTransferMessage;

	private boolean isStickerMessage;

	private TypingNotification typingNotification;

	private JSONArray readByArray;
	
	private JSONObject platformData;

	private boolean shouldShowPush = true;

	private boolean isTickSoundPlayed = false;
	
	private int  hashMessage= HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE;
	
	private int contentId;
	
	private String nameSpace;
	
	private ViewDimentions viewDimentions;
	
	private int notificationType;
	
	private long sortingId = -1;

	public String getNameSpace()
	{
		return nameSpace;
	}

	public void setNameSpace(String nameSpace)
	{
		this.nameSpace = (null == nameSpace ? "": nameSpace);
	}
	
	public void setPlatformData(JSONObject platformData)
	{
		this.platformData = platformData;
	}

	public int getContentId()
	{
		return contentId;
	}
	
	public void setContentId(int contentId)
	{
		this.contentId = contentId;
	}
	
	private ArrayList<String> sentToMsisdnsList = new ArrayList<String>();
	
	private String messageBroadcastId = null;
	
	private long serverId = -1;

	private long sendTimestamp = -1;
	
	private MessagePrivateData privateData;
	
	public int getHashMessage()
	{
		return hashMessage;
	}

	public void setHashMessage(int hashMessage)
	{
		this.hashMessage = hashMessage;
	}

	private int unreadCount = -1;
	private int messageType = HikeConstants.MESSAGE_TYPE.PLAIN_TEXT;
	// private boolean showResumeButton = true;
	public ContentLove contentLove;
	public PlatformMessageMetadata platformMessageMetadata;

	public WebMetadata webMetadata;

	private JSONObject messageEventData;

	public JSONObject getMessageEventData()
	{
		return messageEventData;
	}

	public void setMessageEventData(JSONObject messageEventData)
	{
		this.messageEventData = messageEventData;
	}


	/* Adding entries to the beginning of this list is not backwards compatible */
	public enum OriginType
	{
		NORMAL, /* message sent to server */
		BROADCAST, /* message originated from a broadcast */
		OFFLINE, /* message originated when connected in Hike Offline */
	};
	
	private OriginType messageOriginType = OriginType.NORMAL;

	public boolean isLovePresent(){
		return contentLove!=null;
	}
	public int getMessageType()
	{
		return messageType;
	}

	public void setMessageType(int messageType)
	{
		this.messageType = messageType;
	}

	public boolean isInvite()
	{
		return mInvite;
	}

	public void setInvite(boolean mIsInvite)
	{
		this.mInvite = mIsInvite;
	}

	public boolean isFileTransferMessage()
	{
		return isFileTransferMessage;
	}

	public void setIsFileTranferMessage(boolean isFileTransferMessage)
	{
		this.isFileTransferMessage = isFileTransferMessage;
	}

    public void setIsSent(boolean isSent){this.mIsSent = isSent;}

	public boolean isStickerMessage()
	{
		return isStickerMessage;
	}

	public void setIsStickerMessage(boolean isStickerMessage)
	{
		this.isStickerMessage = isStickerMessage;
	}

	// public void setResumeButtonVisibility(boolean visible)
	// {
	// showResumeButton = visible;
	// }
	//
	// public boolean getResumeButtonVisibility()
	// {
	// return showResumeButton;
	// }

	/* Adding entries to the beginning of this list is not backwards compatible */
	public static enum State
	{
		SENT_UNCONFIRMED, /* message sent to server */
		SENT_FAILED, /* message could not be sent, manually retry */
		SENT_CONFIRMED, /* message received by server */
		SENT_DELIVERED, /* message delivered to client device */
		SENT_DELIVERED_READ, /* message viewed by recipient */
		RECEIVED_UNREAD, /* message received, but currently unread */
		RECEIVED_READ, /* message received and read */
		UNKNOWN
	};
	

	public static enum ParticipantInfoState
	{
		NO_INFO, // This is a normal message
		PARTICIPANT_LEFT, // The participant has left
		PARTICIPANT_JOINED, // The participant has joined
		GROUP_END, // Group chat has ended
		USER_OPT_IN, DND_USER, USER_JOIN, CHANGED_GROUP_NAME, CHANGED_GROUP_IMAGE, BLOCK_INTERNATIONAL_SMS, INTRO_MESSAGE, STATUS_MESSAGE, CHAT_BACKGROUND,
		VOIP_CALL_SUMMARY, VOIP_MISSED_CALL_OUTGOING, VOIP_MISSED_CALL_INCOMING,CHANGE_ADMIN, GC_SETTING_CHANGE,OFFLINE_INLINE_MESSAGE , OFFLINE_FILE_NOT_RECEIVED, FRIEND_REQUSET_STATUS;

		public static ParticipantInfoState fromJSON(JSONObject obj)
		{
			String type = obj.optString(HikeConstants.TYPE);
			if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type))
			{
				return ParticipantInfoState.PARTICIPANT_JOINED;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type))
			{
				return ParticipantInfoState.PARTICIPANT_LEFT;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_ADMIN_UPDATE.equals(type))
			{
				return ParticipantInfoState.CHANGE_ADMIN;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_SETTINGS_CHANGE.equals(type))
			{
				return ParticipantInfoState.GC_SETTING_CHANGE;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type))
			{
				return ParticipantInfoState.GROUP_END;
			}
			else if (HikeConstants.MqttMessageTypes.USER_JOINED.equals(type))
			{
				return USER_JOIN;
			}
			else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type))
			{
				return USER_OPT_IN;
			}
			else if (HikeConstants.DND.equals(type))
			{
				return DND_USER;
			}
			else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type))
			{
				return CHANGED_GROUP_NAME;
			}
			else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type))
			{
				return CHANGED_GROUP_IMAGE;
			}
			else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(type))
			{
				return BLOCK_INTERNATIONAL_SMS;
			}
			else if (HikeConstants.INTRO_MESSAGE.equals(type))
			{
				return ParticipantInfoState.INTRO_MESSAGE;
			}
			else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type))
			{
				return STATUS_MESSAGE;
			}
			else if (HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(type))
			{
				return CHAT_BACKGROUND;
			}
			else if (HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY.equals(type))
			{
				return VOIP_CALL_SUMMARY;
			}
			else if (HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING.equals(type))
			{
				return VOIP_MISSED_CALL_INCOMING;
			}
			else if (HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING.equals(type))
			{
				return VOIP_MISSED_CALL_OUTGOING;
			}
			else if (OfflineConstants.OFFLINE_INLINE_MESSAGE.equals(type))
			{
				return OFFLINE_INLINE_MESSAGE;
			}
			else if(OfflineConstants.OFFLINE_FILES_NOT_RECEIVED_TYPE.equals(type))
			{
				return OFFLINE_FILE_NOT_RECEIVED;
			}

			else if (HikeConstants.FRIENDS_SYSTEM_MESSAGE.equals(type))
			{
				return FRIEND_REQUSET_STATUS;
			}
				
			return NO_INFO;
		}
	}

	public ConvMessage(){
        this.mTimestamp = System.currentTimeMillis()/1000;
		
	}
	public ConvMessage(int unreadCount, long timestamp, long msgId, long sortingId)
	{
		this.unreadCount = unreadCount;
		this.mTimestamp = timestamp;
		this.msgID = msgId;
		this.sortingId = sortingId;
	}

	public ConvMessage(TypingNotification typingNotification)
	{
		this.typingNotification = typingNotification;
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState)
	{
		this(message, msisdn, timestamp, msgState, -1, -1, -1);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, null, sortingId);
	}
	
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, false,HikeConstants.MESSAGE_TYPE.PLAIN_TEXT, -1);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, false,HikeConstants.MESSAGE_TYPE.PLAIN_TEXT, sortingId);
	}
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, int type, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, false, type, sortingId);
	}
	
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS, int type)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, isSMS, ParticipantInfoState.NO_INFO, type,0, "", -1);
	}
	
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS, int type, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, isSMS, ParticipantInfoState.NO_INFO, type,0, "", sortingId);
	}
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS, int type,int contentId, String nameSpace, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, isSMS, ParticipantInfoState.NO_INFO, type, contentId, nameSpace, sortingId);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS,
			ParticipantInfoState participantInfoState, int type,int contentId, String nameSpace, long sortingId)
	{
		this(message, msisdn, timestamp, msgState, msgid, mappedMsgId, groupParticipantMsisdn, isSMS, ParticipantInfoState.NO_INFO, type, contentId, nameSpace, null, sortingId);
	}
	
	public ConvMessage(String message, String msisdn, long timestamp, State msgState, long msgid, long mappedMsgId, String groupParticipantMsisdn, boolean isSMS,
			ParticipantInfoState participantInfoState, int type,int contentId, String nameSpace, JSONObject platformData, long sortingId)
	{
		assert (msisdn != null);
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
		this.msgID = msgid;
		this.mappedMsgId = mappedMsgId;
		mIsSent = isMessageSent(msgState);
		this.groupParticipantMsisdn = groupParticipantMsisdn;
		this.mIsSMS = isSMS;
		this.messageType= type;
		setState(msgState);
		if(msgState.ordinal() >= State.SENT_CONFIRMED.ordinal())
		{
			setTickSoundPlayed(true);
		}
		this.participantInfoState = participantInfoState;
		setContentId(contentId);
		setNameSpace(nameSpace);
		setPlatformData(platformData);
		setSortingId(sortingId);
		
	}
	
	public ConvMessage(ConvMessage other) {
		this.mappedMsgId = other.mappedMsgId;
		this.groupParticipantMsisdn = other.groupParticipantMsisdn;
		this.hashMessage = other.hashMessage;
		this.isBlockAddHeader = other.isBlockAddHeader;
		this.isFileTransferMessage = other.isFileTransferMessage;
		this.isStickerMessage = other.isStickerMessage;
		this.isTickSoundPlayed = other.isTickSoundPlayed;
		this.messageType = other.messageType;
		this.mInvite = other.mInvite;
		this.mIsSent = other.mIsSent;
		this.mIsSMS = other.mIsSMS;
		this.mMessage = other.mMessage;
		this.msgID = other.msgID;
		this.mState = other.mState;
		this.mTimestamp = other.mTimestamp;
		this.participantInfoState = other.participantInfoState;
		this.shouldShowPush = other.shouldShowPush;
		this.unreadCount = other.unreadCount;
		this.metadata = other.metadata;
		this.platformMessageMetadata = other.platformMessageMetadata;
		this.webMetadata = other.webMetadata;
		this.contentLove = other.contentLove;
		this.messageOriginType  = other.messageOriginType;
		this.platformData = other.platformData;
		if (other.isBroadcastConversation())
		{
			this.messageBroadcastId = other.getMsisdn();
		}
		this.privateData = other.privateData;
		try {
			this.readByArray = other.readByArray !=null? new JSONArray(other.readByArray.toString()) : null;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		setSortingId(other.sortingId);
				
	}

	// TODO Here set "pd" as well
	public ConvMessage(JSONObject obj, Context context) throws JSONException
	{
		this.mMsisdn = obj.getString(obj.has(HikeConstants.TO) ? HikeConstants.TO : HikeConstants.FROM); /*
																										 * represents msg is coming from another client
																										 */
		this.groupParticipantMsisdn = obj.has(HikeConstants.TO) && obj.has(HikeConstants.FROM) ? obj.getString(HikeConstants.FROM) : null;
		
		if (obj.has(HikeConstants.SEND_TIMESTAMP))
		{
			this.sendTimestamp = obj.getLong(HikeConstants.SEND_TIMESTAMP);
		}
		
		JSONObject data = obj.getJSONObject(HikeConstants.DATA);
		if (data.has(HikeConstants.SMS_MESSAGE))
		{
			this.mMessage = data.getString(HikeConstants.SMS_MESSAGE);
			mIsSMS = true;
		}
		else
		{
			this.mMessage = data.getString(HikeConstants.HIKE_MESSAGE);
			mIsSMS = false;
		}
		
		this.mTimestamp = data.getLong(HikeConstants.TIMESTAMP);
		/* if we're deserialized an object from json, it's always unread */
		setState(State.RECEIVED_UNREAD);
		msgID = -1;
		String mappedMsgID = data.getString(HikeConstants.MESSAGE_ID);
		try
		{
			this.mappedMsgId = Long.parseLong(mappedMsgID);
		}
		catch (NumberFormatException e)
		{
			Logger.e("CONVMESSAGE", "Exception occured while parsing msgId. Exception : " + e);
			this.mappedMsgId = -1;
			throw new JSONException("Problem in JSON while parsing msgID.");
		}
		this.participantInfoState = ParticipantInfoState.NO_INFO;
		if (data.optBoolean(HikeConstants.POKE))
		{
			JSONObject md = data.has(HikeConstants.METADATA) ? data.getJSONObject(HikeConstants.METADATA) : new JSONObject();
			md.put(HikeConstants.POKE, true);
			data.put(HikeConstants.METADATA, md);
		}

		if (data.has(HikeConstants.METADATA))
		{
			JSONObject mdata = data.getJSONObject(HikeConstants.METADATA);

			if (mdata.has(HikeConstants.PIN_MESSAGE))
			{
				this.messageType = mdata.getInt(HikeConstants.PIN_MESSAGE);
			}
			// TODO : We should parse metadata based on message type, so doing now for content, we should clean the else part sometime
			if(HikeConstants.ConvMessagePacketKeys.CONTENT_TYPE.equals(obj.optString(HikeConstants.SUB_TYPE))){
				this.messageType  = MESSAGE_TYPE.CONTENT;
				platformMessageMetadata  = new PlatformMessageMetadata(data.optJSONObject(HikeConstants.METADATA), context, mIsSent);
                platformMessageMetadata.addToThumbnailTable();
                platformMessageMetadata.thumbnailMap.clear();
			}
			else if (ConvMessagePacketKeys.WEB_CONTENT_TYPE.equals(obj.optString(HikeConstants.SUB_TYPE)))
			{
				this.messageType  = MESSAGE_TYPE.WEB_CONTENT;
				setContentId(mdata.optInt(HikePlatformConstants.CONTENT_ID));
				setNameSpace(mdata.optString(HikePlatformConstants.NAMESPACE));
				webMetadata = new WebMetadata(mdata);
			}
			else if (ConvMessagePacketKeys.FORWARD_WEB_CONTENT_TYPE.equals(obj.optString(HikeConstants.SUB_TYPE)))
			{
				this.messageType  = MESSAGE_TYPE.FORWARD_WEB_CONTENT;
				setContentId(mdata.optInt(HikePlatformConstants.CONTENT_ID));
				setNameSpace(mdata.optString(HikePlatformConstants.NAMESPACE));
				webMetadata = new WebMetadata(mdata);
			}
			else
			{
				setMetadata(data.getJSONObject(HikeConstants.METADATA));
			}
		}
		
		if(data.has(HikeConstants.PLATFORM_PACKET))
		{
			Logger.i("ConvMessage", "Data has Platform Packet");
			
			platformData = data.optJSONObject(HikeConstants.PLATFORM_PACKET);
		}
		
		this.isStickerMessage = HikeConstants.STICKER.equals(obj.optString(HikeConstants.SUB_TYPE));
		/**
		 * This is to specifically handle the hike bot cases for now but can be generically used to control which messages have push enabled
		 */
		if (data.has(HikeConstants.PUSH))
		{
			this.shouldShowPush = data.optBoolean(HikeConstants.PUSH, true);
		}
		
	}

	public ConvMessage(JSONObject obj, Conversation conversation, Context context, boolean isSelfGenerated) throws JSONException
	{
		setMetadata(obj);

		if (participantInfoState != ParticipantInfoState.USER_JOIN || conversation != null)
		{
			this.mMsisdn = conversation != null ? conversation.getMsisdn() : obj.has(HikeConstants.TO) ? obj.getString(HikeConstants.TO) : obj.getString(HikeConstants.FROM);
			this.groupParticipantMsisdn = obj.has(HikeConstants.TO) && obj.has(HikeConstants.FROM) ? obj.getString(HikeConstants.FROM) : null;
		}
		else
		{
			this.mMsisdn = obj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
		}
		
		if (obj.has(HikeConstants.SEND_TIMESTAMP))
		{
			this.sendTimestamp = obj.getLong(HikeConstants.SEND_TIMESTAMP);
		}

		this.mMessage = "";
		this.mTimestamp = System.currentTimeMillis() / 1000;
		JSONObject data = obj.optJSONObject(HikeConstants.DATA);
		if(data!=null)
		{
			mTimestamp = data.optLong(HikeConstants.TIMESTAMP, mTimestamp);
		}
		switch (this.participantInfoState)
		{
		case PARTICIPANT_JOINED:
			JSONArray arr = metadata.getGcjParticipantInfo();
			String highlight = Utils.getOneToNConversationJoinHighlightText(arr, (OneToNConversation) conversation, metadata.isNewGroup()&&metadata.getGroupAdder()!=null, context);
			this.mMessage = OneToNConversationUtils.getParticipantAddedMessage(this, context, highlight);
			break;
		case PARTICIPANT_LEFT:
			this.mMessage = OneToNConversationUtils.getParticipantRemovedMessage(conversation.getMsisdn(), context, ((OneToNConversation) conversation).getConvParticipantFirstNameAndSurname(metadata.getMsisdn()));
			break;
		case CHANGE_ADMIN:
			String myMsidn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

			JSONObject dataO = obj.optJSONObject(HikeConstants.DATA);
			String msisdns = dataO.optString(HikeConstants.ADMIN_MSISDN);
			if (!msisdns.equalsIgnoreCase(myMsidn))
			{
				this.shouldShowPush = false;
			}
			this.mMessage = OneToNConversationUtils.getAdminUpdatedMessage(this, context);
			break;
		case GC_SETTING_CHANGE:
			this.mMessage = OneToNConversationUtils.getSettingUpdatedMessage(this, context);
			break;
		case GROUP_END:
			this.mMessage = OneToNConversationUtils.getConversationEndedMessage(conversation.getMsisdn(), context);
			break;
		case USER_JOIN:
			//This is to specifically handle the cases for which pushes are not required for UJ, UL, etc.\
			this.shouldShowPush = obj.optJSONObject(HikeConstants.DATA).optBoolean(HikeConstants.PUSH, metadata.shouldShowPush());
			
			String fName = null;
			if (conversation != null)
			{
				if (conversation instanceof OneToNConversation)
				{
					fName = ((OneToNConversation) conversation).getConvParticipantFirstNameAndSurname(metadata.getMsisdn());
				}
				else
				{
					fName = Utils.getFirstName(conversation.getLabel());
				}
			}
			else
			{
				fName = ContactManager.getInstance().getContact(metadata.getMsisdn(), false, true).getFirstName();
			}
			if(fName != null)
			{
				this.mMessage = String.format(metadata.getJSON().getJSONObject(HikeConstants.DATA).optString(HikeConstants.UserJoinMsg.NOTIF_TEXT), fName);	
			}
			break;
		case USER_OPT_IN:
			String name;
			if (conversation instanceof GroupConversation)
			{
				name = ((GroupConversation) conversation).getConvParticipantFirstNameAndSurname(metadata.getMsisdn());
			}
			else
			{
				name = Utils.getFirstName(conversation.getLabel());
			}
			this.mMessage = String.format(context.getString(conversation instanceof GroupConversation ? R.string.joined_conversation : R.string.optin_one_to_one), name);
			break;
		case CHANGED_GROUP_NAME:
		case CHANGED_GROUP_IMAGE:
			String msisdn = metadata.getMsisdn();
			String userMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			isSelfGenerated=userMsisdn.equals(msisdn);
			String participantName = isSelfGenerated? context.getString(R.string.you) : ((OneToNConversation) conversation).getConvParticipantFirstNameAndSurname(msisdn);
			
			if (participantInfoState == ParticipantInfoState.CHANGED_GROUP_NAME)
			{
				this.mMessage = OneToNConversationUtils.getConversationNameChangedMessage(conversation.getMsisdn(), context, participantName);
			}
			else
			{
				this.mMessage = StringUtils.getYouFormattedString(context, userMsisdn.equals(msisdn), R.string.you_change_group_image, R.string.change_group_image, participantName);
			}
			break;
		case BLOCK_INTERNATIONAL_SMS:
			this.mMessage = context.getString(R.string.block_internation_sms);
			break;
		case STATUS_MESSAGE:
			this.mTimestamp = metadata.getStatusMessage().getTimeStamp();
			String msg;
			if (metadata.getStatusMessage().getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				msg = context.getString(R.string.changed_profile);
			}
			else if (metadata.getStatusMessage().getStatusMessageType() == StatusMessageType.IMAGE || metadata.getStatusMessage().getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
			{
				msg = context.getString(R.string.posted_photo);
			}
			else
			{
				msg = metadata.getStatusMessage().getText();
			}
			this.mMessage = "\"" + msg + "\"";
			/*
			 * We want all status message state to be read by default.
			 */
			isSelfGenerated = true;
			break;
		case CHAT_BACKGROUND:
			if (conversation != null)
			{

				String nameString;
				if (conversation instanceof OneToNConversation)
				{
					nameString = ((OneToNConversation) conversation).getConvParticipantFirstNameAndSurname(metadata.getMsisdn());
				}
				else
				{
					nameString = Utils.getFirstName(conversation.getLabel());
				}
				this.mMessage = context.getString(R.string.chat_bg_changed, nameString);
			}
			break;
		case VOIP_MISSED_CALL_INCOMING:
			this.mMessage = context.getString(R.string.voip_missed_call_notif);
			break;
		}

		if(isSelfGenerated || (metadata != null && metadata.isSync()))
		{
			setState(State.RECEIVED_READ);
		}
		else
		{
			setState(State.RECEIVED_UNREAD);
		}

	}

	public void setMetadata(MessageMetadata messageMetadata)
	{	
		if(messageMetadata!=null){
		this.metadata = messageMetadata;
		isFileTransferMessage = this.metadata.getHikeFiles() != null  &&   this.metadata.getHikeFiles().size() > 0;

		participantInfoState = this.metadata.getParticipantInfoState() ;

		isStickerMessage = this.metadata.getSticker() != null;
		}
		
	}

	public void setMetadata(JSONObject metadata) throws JSONException
	{
		if (metadata != null)
		{
			this.metadata = new MessageMetadata(metadata, mIsSent);

			isFileTransferMessage = this.metadata.getHikeFiles() != null  &&   this.metadata.getHikeFiles().size() > 0;

			participantInfoState = this.metadata.getParticipantInfoState();

			isStickerMessage = this.metadata.getSticker() != null;
			
		}
	}

	public void setMetadata(String metadataString) throws JSONException
	{
		if (!TextUtils.isEmpty(metadataString))
		{
			JSONObject metadata = new JSONObject(metadataString);
			setMetadata(metadata);
		}
	}

	public ParticipantInfoState getParticipantInfoState()
	{
		return participantInfoState;
	}

	public void setParticipantInfoState(ParticipantInfoState participantInfoState)
	{
		this.participantInfoState = participantInfoState;
	}

	public MessageMetadata getMetadata()
	{
		return this.metadata;
	}

	public void setMessage(String mMessage)
	{
		this.mMessage = mMessage;
	}

	public String getMessage()
	{
		return mMessage;
	}

	public boolean isSent()
	{
		return mIsSent;
	}

	public void setTimestamp(long timeStamp)
	{
		this.mTimestamp = timeStamp;
	}

	public long getTimestamp()
	{
		return this.mTimestamp;
	}

	public State getState()
	{
		return mState;
	}

	public String getMsisdn()
	{
		return mMsisdn;
	}
	
	/*
	 * Return actual sender of the message. In case of one-to-one mMsisdn is sender
	 * But in case of group, msisdn of perticular participant would be returned. 
	 */
	public String getSenderMsisdn()
	{
		if(isSent())
		{
			return ContactManager.getInstance().getSelfMsisdn();
		}
		else if(!TextUtils.isEmpty(groupParticipantMsisdn))
		{
			return groupParticipantMsisdn;
		}
		else
		{
			return mMsisdn;
		}
	}

	public String getGroupParticipantMsisdn()
	{
		return groupParticipantMsisdn;
	}

	@Override
	public String toString()
	{
		return "ConvMessage [msgID=" + msgID + ", sortingId=" + sortingId + ", mappedMsgId=" + mappedMsgId + ", mMessage=" + mMessage + ", mMsisdn=" + mMsisdn + ", mTimestamp=" + mTimestamp + ", mIsSent="
				+ mIsSent + ", mState=" + mState + ", metadata=" + metadata + ", privateData=" + privateData + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result + ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mMsisdn == null) ? 0 : mMsisdn.hashCode());
		result = prime * result + ((mState == null) ? 0 : mState.hashCode());
		result = prime * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
		result = prime * result + (int) (msgID ^ (msgID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConvMessage other = (ConvMessage) obj;

		if (msgID != other.msgID)
		{
			return false;
		}
		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null)
		{
			if (other.mMessage != null)
				return false;
		}
		else if (!mMessage.equals(other.mMessage))
			return false;
		if (mMsisdn == null)
		{
			if (other.mMsisdn != null)
				return false;
		}
		else if (!mMsisdn.equals(other.mMsisdn))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}

	public JSONObject serialize()
	{
		return serialize(false);
	}

	public JSONObject serialize(boolean sendNativeInvite)
	{
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject md = null;
		try
		{
			if (participantInfoState == ParticipantInfoState.CHAT_BACKGROUND && metadata!=null)
			{
				object = metadata.getJSON();
			}
			else
			{
				if (metadata != null)
				{
					md = metadata.getJSON();
	
					if (isFileTransferMessage || isStickerMessage)
					{
						data.put(HikeConstants.METADATA, md);
						
					}else if(messageType!=HikeConstants.MESSAGE_TYPE.PLAIN_TEXT)
					{
						data.put(HikeConstants.METADATA, md);
				    }
					else if (metadata.isPokeMessage())
					{
						data.put(HikeConstants.POKE, true);
					}
				}
				
				// Adding "pd" into packet for new type of packet
				if(privateData != null && privateData.getTrackID() != null)
				{
					object.put(HikeConstants.PRIVATE_DATA, privateData.serialize());
				}
				
				data.put(!mIsSMS ? HikeConstants.HIKE_MESSAGE : HikeConstants.SMS_MESSAGE, mMessage);
				
				data.put(HikeConstants.TIMESTAMP, mTimestamp);

				if (mInvite)
				{
					data.put(HikeConstants.MESSAGE_ID, System.currentTimeMillis());
				}
				else
				{
					data.put(HikeConstants.MESSAGE_ID, msgID);

					if(StealthModeManager.getInstance().isStealthMsisdn(mMsisdn) && isSent())
					{
						data.put(HikeConstants.STEALTH, true);
					}
				}

				object.put(HikeConstants.TO, mMsisdn);
				object.put(HikeConstants.DATA, data);
				if (isStickerMessage)
				{
					object.put(HikeConstants.SUB_TYPE, HikeConstants.STICKER);
				}

				if (sendNativeInvite && mInvite)
				{
					object.put(HikeConstants.SUB_TYPE, HikeConstants.NO_SMS);
				}
				if (isBroadcastConversation())
				{
					ArrayList<String> contactsList = getSentToMsisdnsList();
					JSONArray msisdnArray = new JSONArray();
					for (int i=0; i<contactsList.size();i++)
					{
						msisdnArray.put((String)contactsList.get(i));
					}
					
					data.put(HikeConstants.LIST, msisdnArray);
					object.put(HikeConstants.DATA, data);
				}
				JSONObject metadata;
				// TODO : we should add all sub types here and set metadata accordingly
				switch(messageType){
				case MESSAGE_TYPE.CONTENT:
					object.put(HikeConstants.SUB_TYPE, ConvMessagePacketKeys.CONTENT_TYPE);
					data.put(HikeConstants.METADATA, platformMessageMetadata.getJSON());
					break;

				case MESSAGE_TYPE.WEB_CONTENT:
					object.put(HikeConstants.SUB_TYPE, ConvMessagePacketKeys.WEB_CONTENT_TYPE);
					data.put(HikeConstants.PLATFORM_PACKET, getPlatformData());
					metadata = webMetadata.getJSON();
					metadata.put(HikePlatformConstants.NAMESPACE, nameSpace);
					data.put(HikeConstants.METADATA, metadata);
					break;

				case MESSAGE_TYPE.FORWARD_WEB_CONTENT:
					object.put(HikeConstants.SUB_TYPE, ConvMessagePacketKeys.FORWARD_WEB_CONTENT_TYPE);
					data.put(HikeConstants.PLATFORM_PACKET, getPlatformData());
					metadata = webMetadata.getJSON();
					metadata.put(HikePlatformConstants.NAMESPACE, nameSpace);
					data.put(HikeConstants.METADATA, metadata);
					break;

				}
				
				//object.put(OfflineConstants.IS_OFFLINE_MESSAGE, IsOfflineMessage());
				object.put(HikeConstants.TYPE, mInvite ? HikeConstants.MqttMessageTypes.INVITE : HikeConstants.MqttMessageTypes.MESSAGE);
				object.put(HikeConstants.SEND_TIMESTAMP, getSendTimestamp());
			}
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public String getTimestampFormatted(boolean pretty, Context context)
	{
		return Utils.getFormattedTime(pretty, context, mTimestamp);
	}

	public String getMessageDate(Context context)
	{
		return Utils.getFormattedDate(context, mTimestamp);
	}

	public void setMsgID(long msgID)
	{
		this.msgID = msgID;
	}

	public long getMsgID()
	{
		return msgID;
	}

	public void setMappedMsgID(long msgID)
	{
		this.mappedMsgId = msgID;
	}

	public long getMappedMsgID()
	{
		return mappedMsgId;
	}
	
	public int getUnreadCount()
	{
		return unreadCount;
	}

	public static State stateValue(int val)
	{
		return State.values()[val];
	}
	
	public static OriginType originTypeValue(int val)
	{
		return OriginType.values()[val];
	}

	public void setState(State state)
	{
		/* only allow the state to increase */
		if (((mState != null) ? mState.ordinal() : 0) <= state.ordinal())
		{
			mState = state;
		}

		/*
		 * We have a bug where a message is flipping from sent to received add this assert to track down when/where it's happening assert(mIsSent == (mState ==
		 * State.SENT_UNCONFIRMED || mState == State.SENT_CONFIRMED || mState == State.SENT_DELIVERED || mState == State.SENT_DELIVERED_READ || mState == State.SENT_FAILED));
		 */
	}

	public JSONObject serializeDeliveryReportRead()
	{
		JSONObject object = new JSONObject();
		JSONArray ids = new JSONArray();
		try
		{
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
			object.put(HikeConstants.TO, mMsisdn);
			if(privateData != null && privateData.getTrackID() != null && !OneToNConversationUtils.isGroupConversation(mMsisdn))
			{
				// "d":{"msgid1":{track_id:"value"}}
				JSONObject obj = new JSONObject();
				Logger.d(AnalyticsConstants.MSG_REL_TAG, "pd serializing for dr, "+ privateData.serialize().toString());
				obj.put(String.valueOf(mappedMsgId), privateData.serialize());
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ);
				object.put(HikeConstants.DATA, obj);
			}
			else
			{
				ids.put(String.valueOf(mappedMsgId));
				object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
				object.put(HikeConstants.DATA, ids);
			}
			if(OriginType.OFFLINE==messageOriginType){
				object.put(HikeConstants.TIMESTAMP,getTimestamp());
			}
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}

		return object;
	}

	public void setSMS(boolean isSMS)
	{
		this.mIsSMS = isSMS;
	}

	public boolean isSMS()
	{
		return mIsSMS;
	}

	public TypingNotification getTypingNotification()
	{
		return typingNotification;
	}

	public void setTypingNotification(TypingNotification typingNotification)
	{
		this.typingNotification = typingNotification;
	}

	public JSONArray getReadByArray()
	{
		return readByArray;
	}

	public void setReadByArray(String readBy)
	{
		if (TextUtils.isEmpty(readBy))
		{
			return;
		}
		try
		{
			this.readByArray = new JSONArray(readBy);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON");
		}
	}

	public int getImageState()
	{
		/* received messages have no img */
		if (!isSent())
		{
			return -1;
		}

		/* failed is handled separately, since it's applicable to SMS messages */
		if (mState == State.SENT_FAILED)
		{
			return R.drawable.ic_failed;
		}

		switch (mState)
		{
		case SENT_DELIVERED:
			return R.drawable.ic_delivered;
		case SENT_DELIVERED_READ:
			return R.drawable.ic_read;
		case SENT_CONFIRMED:
			return R.drawable.ic_sent;
		case SENT_UNCONFIRMED:
			return R.drawable.ic_retry_sending;
		default:
			return R.drawable.ic_blank;
		}
	}

	public boolean isOneToNChat()
	{
		return OneToNConversationUtils.isOneToNConversation(this.mMsisdn);
	}

	/**
	 * @return the shouldShowPush
	 */
	public boolean isShouldShowPush()
	{
		return shouldShowPush;
	}

	/**
	 * @param shouldShowPush
	 *            the shouldShowPush to set
	 */
	public void setShouldShowPush(boolean shouldShowPush)
	{
		this.shouldShowPush = shouldShowPush;
	}

	public void setBlockAddHeader(boolean isBlockAddHeader)
	{
		this.isBlockAddHeader = isBlockAddHeader;
	}

	public boolean isBlockAddHeader()
	{
		return isBlockAddHeader;
	}
	
	public boolean isTickSoundPlayed()
	{
		return isTickSoundPlayed;
	}

	public void setTickSoundPlayed(boolean isTickSoundPlayed)
	{
		this.isTickSoundPlayed = isTickSoundPlayed;
	}

	/**
	 * Whether a notification sound should be played while displaying this message in Android notifications shade
	 * 
	 * @return
	 */
	public boolean isSilent()
	{
		if (getPlatformData() != null)
		{
			return isNormalMessageSilent();
		}

		if (ContactManager.getInstance().isChatMuted(mMsisdn))
		{
			return true;
		}

		if (getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT && webMetadata != null)
		{
			return webMetadata.getPushType().equals(HikePlatformConstants.SILENT_PUSH);
		}
		// Do not play sound in case of bg change, status updates
		if ((getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND) || (getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED)
				 || (getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE))
		{
			return true;
		}
		else if(getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
		{
			return metadata.isSilent();
		}
		else
		{
			return false;
		}
	}
	
	public boolean isImageMsg()
		{
			return isFileTransferMessage() && getMetadata() != null && getMetadata().getHikeFiles().get(0).getHikeFileType() == HikeFileType.IMAGE ;
			
		}
		
		public boolean isTextMsg()
		{
			if(getMessageType() != MESSAGE_TYPE.PLAIN_TEXT)
			{
				return false;
			}
			
			//a MESSAGE_TYPE.PLAIN_TEXT type message might be ft, sticker or nudge.So, rolling out these possibilities
			if (isFileTransferMessage() || isStickerMessage() || (getMetadata() != null && getMetadata().isPokeMessage()))
			{
				return false;
			}
				
			return true;
		}
	

	public static boolean isMessageSent(State msgState)
	{
		return !(msgState==State.RECEIVED_READ || msgState == State.RECEIVED_UNREAD);
	}

	public void setMsisdn(String msisdn){
		this.mMsisdn = msisdn;
	}

	@Override
	public boolean doesItemContain(String s)
	{
		if (isFileTransferMessage())
		{
			HikeFile hikeFile = getMetadata().getHikeFiles().get(0);
			// Name of walkie talkie file is not user specified.
			// No need to perform any search on this.
			if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
			{
				return false;
			}
			// For contacts, search is to be performed on multiple values.
			else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
			{
				String dispName = hikeFile.getDisplayName();
				if (!TextUtils.isEmpty(dispName) && dispName.toLowerCase().contains(s))
				{
					return true;
				}
				List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);
				String phone = null, email = null;
				for (ContactInfoData contactInfoData : items)
				{
					if (contactInfoData.getDataType() == DataType.PHONE_NUMBER)
					{
						phone = contactInfoData.getData();
						if (!TextUtils.isEmpty(phone) && phone.toLowerCase().contains(s))
						{
							return true;
						}
					}
					else if (contactInfoData.getDataType() == DataType.EMAIL)
					{
						email = contactInfoData.getData().toLowerCase();
						if (!TextUtils.isEmpty(email) && email.toLowerCase().contains(s))
						{
							return true;
						}
					}
				}
			}
			// Search on file name for all others
			else if (hikeFile.getFileName().toLowerCase().contains(s))
			{
				return true;
			}
			
		}
		// Search on status messages.
		else if (getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
		{
			if (getMetadata().getStatusMessage().getText()!=null && getMetadata().getStatusMessage().getText().toLowerCase().contains(s))
			{
				return true;
			}
		}
		// No search on system updates/messages.
		else if (getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			return false;
		}
		// No search on sticker/nudge messages.
		// Atleast till theres no tagging.
		else if (isStickerMessage() || (metadata != null && metadata.isPokeMessage()))
		{
			return false;
		}
		// Text search for all others
		else if (!TextUtils.isEmpty(getMessage()))
		{
			if (getMessage().toLowerCase().contains(s))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isVoipMissedCallMsg()
	{
		return participantInfoState == ParticipantInfoState.VOIP_MISSED_CALL_INCOMING;
	}
	
	public boolean isBroadcastConversation() {
		return OneToNConversationUtils.isBroadcastConversation(this.mMsisdn);
	}
	
	public boolean isBroadcastMessage() {
		return messageOriginType == OriginType.BROADCAST;
	}
	
	public boolean isOfflineMessage(){
		return messageOriginType == OriginType.OFFLINE;
	}
	
	public ArrayList<String> getSentToMsisdnsList() {
		return sentToMsisdnsList;
	}

	public void setSentToMsisdnsList(ArrayList<String> sentToMsisdnsList) {
		this.sentToMsisdnsList.addAll(sentToMsisdnsList);
	}

	public void addToSentToMsisdnsList(String msisdn) {
		this.sentToMsisdnsList.add(msisdn);
	}

	public boolean hasBroadcastId() {
		return messageBroadcastId != null;
	}

	public String getMessageBroadcastId() {
		return this.messageBroadcastId;
	}

	public OriginType getMessageOriginType()
	{
		return messageOriginType;
	}

	public void setMessageOriginType(OriginType messageOriginType)
	{
		this.messageOriginType = messageOriginType;
	}

	public long getServerId()
	{
		if(isBroadcastMessage() && !isBroadcastConversation())
		{
			return serverId;
		}
		else
		{
			return msgID;
		}
	}

	public void setServerId(long serverId)
	{
		this.serverId = serverId;
	}

	/**
	 * @return the notificaionType
	 */
	public int getNotificationType()
	{
		return notificationType;
	}

	/**
	 * @param notificationType the notificaionType to set
	 */
	public void setNotificaionType(int notificationType)
	{
		this.notificationType = notificationType;
	}

	public MessagePrivateData getPrivateData()
	{
		return privateData;
	}
	
	public void setPrivateData(MessagePrivateData messagePrivateData)
	{	
		if(messagePrivateData != null)
		{
			this.privateData = messagePrivateData;
		}
	}
	
	@Override
	public ViewDimentions getDimentionMatrix()
	{
		return this.viewDimentions;
	}

	@Override
	public void setDimentionMatrix(ViewDimentions vD)
	{
		this.viewDimentions = vD;
	}

	@Override
	public Long getUniqueId()
	{
		return getSortingId();
	}

	public long getSendTimestamp()
	{
		return sendTimestamp;
	}

	public void setSendTimestamp(long sendTimestamp)
	{
		this.sendTimestamp = sendTimestamp;
	}
	
	public String createMessageHash()
	{
		/*
		 * ParticipantInfoState == ParticipantInfoState.NO_INFO
		 * implies, type "m" messages. We can't currently create
		 * and keep message hash for other message types because
		 * many of of them don't have certain info. for eg. SU,GCJ etc,  
		 * don't have msgID. 
		 */
		if (getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			String messageHash = getSenderMsisdn() + "_" + getSendTimestamp() + "_";
			messageHash += isSent() ? getMsgID() : getMappedMsgID();

			Logger.d(getClass().getSimpleName(), "Message hash: " + messageHash);
			return messageHash;
		}
		return null;
	}

	public JSONObject getPlatformData()
	{
		return this.platformData;
	}
	
	public boolean isNormalMessageSilent()
	{
		return getPlatformData().optString(HikeConstants.PLAY_NOTIFICATION).equals(HikeConstants.SILENT);
	}

	@Override
	public int compareTo(ConvMessage another)
	{
		if (another == null )
		{
			return 1;
		}
		
		if (this.equals(another))
		{
			return 0;
		}
		
		long other_sortingId = another.sortingId;
		
		if (this.sortingId == other_sortingId) 
		{
			return (this.msgID < another.msgID) ? -1 : 1;
		}
		
		return (this.sortingId < other_sortingId) ? -1 : 1; 
			
	}

	/**
	 * @return the sortingId
	 */
	public long getSortingId()
	{
		return sortingId;
	}

	/**
	 * @param sortId the sortingId to set
	 */
	public void setSortingId(long sortId)
	{
		this.sortingId = sortId;
	}
	
	
	public static class ConvMessageComparator implements Comparator<ConvMessage>
	{
		/**
		 * This comparator reverses the order of the normal comparable
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */

		@Override
		public int compare(ConvMessage lhs, ConvMessage rhs)
		{
			if (rhs == null)
			{
				return 1;
			}

			return lhs.compareTo(rhs);
		}

	}

	/**
	 * Used to forcefully set the state of the ConvMessage. This is used by General Events since we need to change the state of messages from sent to received and vice versa.
	 *
	 * @param state
	 */
	public void setStateForced(State state)
	{
		if (state != null)
		{
			mState = state;
		}
	}

}
