package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhit on 27/07/15.
 */
public class MessageEvent implements Parcelable
{

	private int eventStatus;

	private int eventType;

	private long sentTimeStamp;

	private long mappedEventId;

	private String msisdn;

	private String nameSpace;

	private String eventMetadata;

	private String messageHash;

	private long eventId;

	private long messageId;

	private String parent_msisdn;

	private String hike_message;

	private String fromUserMsisdn;

	public MessageEvent(String eventType, String msisdn, String nameSpace, String eventMetadata, String messageHash, int eventStatus, long timeStamp)
	{
		this(eventType, msisdn, nameSpace, eventMetadata, messageHash, eventStatus, timeStamp, -1);
	}

	public MessageEvent(String eventType, String msisdn, String nameSpace, String eventMetadata, String messageHash, int eventStatus, long timeStamp, long mappedEventId)
	{
		this(eventType, msisdn, nameSpace, eventMetadata, messageHash, eventStatus, timeStamp, mappedEventId, -1);
	}

	public MessageEvent(String eventType, String msisdn, String nameSpace, String eventMetadata, String messageHash, int eventStatus, long timeStamp, long mappedEventId, long messageId)
	{
		this(eventType, msisdn, nameSpace, eventMetadata, messageHash, eventStatus, timeStamp, mappedEventId, messageId, null);
	}
	public MessageEvent(String eventType, String msisdn, String nameSpace, String eventMetadata, String messageHash, int eventStatus, long timeStamp, long mappedEventId, long messageId, String parent_msisdn)
	{
		this(eventType, msisdn, nameSpace, eventMetadata, messageHash, eventStatus, timeStamp, mappedEventId, messageId,parent_msisdn,"");
	}

	public MessageEvent(String eventType, String msisdn, String nameSpace, String eventMetadata, String messageHash, int eventStatus, long timeStamp, long mappedEventId, long messageId, String parent_msisdn,String hike_message)
	{
		setEventType(eventType);
		this.msisdn = msisdn;
		this.nameSpace = nameSpace;
		this.eventMetadata = eventMetadata;
		this.messageHash = messageHash;
		this.eventStatus = eventStatus;
		this.sentTimeStamp = timeStamp;
		this.mappedEventId = mappedEventId;
		this.messageId = messageId;
		this.parent_msisdn = parent_msisdn;
		this.hike_message=hike_message;

		try {
			if(this.eventMetadata !=null)
            {
                JSONObject eventMetadataJsonObject = new JSONObject(eventMetadata);
				if(eventMetadataJsonObject.has(HikePlatformConstants.EVENT_FROM_USER_ID))
				{
					this.fromUserMsisdn = eventMetadataJsonObject.getString(HikePlatformConstants.EVENT_FROM_USER_ID);
				}
				if(eventMetadataJsonObject.has(HikePlatformConstants.PARENT_MSISDN) && TextUtils.isEmpty(parent_msisdn))
				{
					this.parent_msisdn = eventMetadataJsonObject.getString(HikePlatformConstants.PARENT_MSISDN);
				}
            }

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public int getEventStatus()
	{
		return eventStatus;
	}

	public void setEventStatus(int eventStatus)
	{
		this.eventStatus = eventStatus;
	}

	public int getEventType()
	{
		return eventType;
	}

	public void setEventType(String type)
	{
		if (type.equals(HikePlatformConstants.SHARED_EVENT))
		{
			this.eventType = HikePlatformConstants.EventType.SHARED_EVENT;
		}
		else
		{
			this.eventType = HikePlatformConstants.EventType.NORMAL_EVENT;
		}
	}

	public boolean isEventSent(int state)
	{
		return state == HikePlatformConstants.EventStatus.EVENT_SENT;
	}

	public long getSentTimeStamp()
	{
		return sentTimeStamp;
	}

	public void setSentTimeStamp(long sentTimeStamp)
	{
		this.sentTimeStamp = sentTimeStamp;
	}

	public long getMappedEventId()
	{
		return mappedEventId;
	}

	public void setMappedEventId(long mappedEventId)
	{
		this.mappedEventId = mappedEventId;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public String getNameSpace()
	{
		return nameSpace;
	}

	public void setNameSpace(String nameSpace)
	{
		this.nameSpace = nameSpace;
	}

	public String getEventMetadata()
	{
		return eventMetadata;
	}

	public void setEventMetadata(String eventMetadata)
	{
		this.eventMetadata = eventMetadata;
	}

	public String getParent_msisdn()
	{
		return parent_msisdn;
	}

	public String getMessageHash()
	{
		return messageHash;
	}

	public void setMessageHash(String messageHash)
	{
		this.messageHash = messageHash;
	}

	public long getEventId()
	{
		return eventId;
	}

	public void setEventId(long eventId)
	{
		this.eventId = eventId;
	}

	public long getMessageId()
	{
		return messageId;
	}

	public String createEventHash()
	{
		/*
		 * event hash used for duplicate check in received events
		 */
		if (!isEventSent(eventStatus))
		{
			return getSenderMsisdn() + "_" + getSentTimeStamp() + "_" + getMappedEventId();

		}
		return null;
	}

	public String getSenderMsisdn()
	{
     	return isEventSent(eventStatus) ? ContactManager.getInstance().getSelfMsisdn() : msisdn;
	}

	public String getHikeMessage()
	{
		return hike_message;
	}

	public String getFromUserMsisdn()
	{
		return fromUserMsisdn;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(eventStatus);
		dest.writeInt(eventType);
		dest.writeLong(sentTimeStamp);
		dest.writeLong(mappedEventId);
		dest.writeString(msisdn);
		dest.writeString(nameSpace);
		dest.writeString(eventMetadata);
		dest.writeString(messageHash);
		dest.writeLong(eventId);
		dest.writeLong(messageId);
		dest.writeString(parent_msisdn);
		dest.writeString(hike_message);
	}

	public MessageEvent(Parcel source)
	{
		this.eventStatus = source.readInt();
		this.eventType = source.readInt();
		this.sentTimeStamp = source.readLong();
		this.mappedEventId = source.readLong();
		this.msisdn = source.readString();
		this.nameSpace = source.readString();
		this.eventMetadata = source.readString();
		this.messageHash = source.readString();
		this.eventId = source.readLong();
		this.messageId = source.readLong();
		this.parent_msisdn = source.readString();
		this.hike_message = source.readString();
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
	{
		public MessageEvent createFromParcel(Parcel in)
		{
			return new MessageEvent(in);
		}

		public MessageEvent[] newArray(int size)
		{
			return new MessageEvent[size];
		}
	};


}

