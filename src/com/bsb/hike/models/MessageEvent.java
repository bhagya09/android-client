package com.bsb.hike.models;

import android.text.TextUtils;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;
import org.json.JSONObject;

/**
 * Created by shobhit on 27/07/15.
 */
public class MessageEvent
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
		setEventType(eventType);
		this.msisdn = msisdn;
		this.nameSpace = nameSpace;
		this.eventMetadata = eventMetadata;
		this.messageHash = messageHash;
		this.eventStatus = eventStatus;
		this.sentTimeStamp = timeStamp;
		this.mappedEventId = mappedEventId;
		this.messageId = messageId;
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

}

