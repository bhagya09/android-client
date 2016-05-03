package com.bsb.hike.messageinfo;

import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by ravi on 4/20/16.
 */
public abstract class MessageInfoDataModel
{

	public abstract HashMap<String, MessageInfoParticipantData> getAllDeliveredMembers();

	public abstract HashMap<String, MessageInfoParticipantData> getAllReadMembers();

	public abstract HashMap<String, MessageInfoParticipantData> getAllParticipants(String msisdn);

	public TreeMap<String, MessageInfoParticipantData> participantTreeMap=new TreeMap<String,MessageInfoParticipantData>();;

	public long messageID;

	public MessageInfoDataModel(String msisdn, long messageID)
	{
		this.msisdn = msisdn;
		this.messageID = messageID;
	}

	protected HashSet<MessageInfo> messageInfoMap;

	protected List<MessageInfoParticipantData> allParticipantsList;

	public abstract void fetchAllParticipantsInfo();

	protected HikeConversationsDatabase mDb = HikeConversationsDatabase.getInstance();

	protected ConvMessage convMessage;

	public String msisdn;

	public static class MessageInfoParticipantData
	{
		public ContactInfo contactInfo;

		MessageInfoParticipantData(ContactInfo contactInfo, long readTimeStamp, long deliveredTimeStamp)
		{
			this.contactInfo = contactInfo;
			this.readTimeStamp = readTimeStamp;
			this.deliveredTimeStamp = deliveredTimeStamp;
		}
		MessageInfoParticipantData(ContactInfo contactInfo, long readTimeStamp, long deliveredTimeStamp,long playedTimeStamp)
		{
			this.contactInfo = contactInfo;
			this.readTimeStamp = readTimeStamp;
			this.deliveredTimeStamp = deliveredTimeStamp;
			this.playedTimeStamp = playedTimeStamp;
		}


		private long readTimeStamp;

		private long deliveredTimeStamp;

		private long playedTimeStamp;

		public void setReadTimeStamp(long readTimeStamp)
		{
			this.readTimeStamp = readTimeStamp;

		}
		public int getDrawableMessageState(){
			if(hasBeenPlayed()){
				return R.drawable.ic_double_tick_r_blue;

			}else if(hasRead()){
				return R.drawable.ic_double_tick_r_blue;
			}else if(hasBeenDelivered()){
				return R.drawable.ic_double_tick_black;
			}else
				return R.drawable.ic_single_tick;
		}
		public void setPlayedTimeStamp(long playedTimeStamp)
		{
			this.playedTimeStamp = playedTimeStamp;
		}

		public void setDeliveredTimeStamp(long deliveredTimeStamp)
		{
			this.deliveredTimeStamp = deliveredTimeStamp;

		}

		public boolean hasRead()
		{
			return readTimeStamp != 0;
		}

		public boolean hasBeenDelivered()
		{
			return deliveredTimeStamp != 0;
		}

		public long getReadTimeStamp()
		{
			return readTimeStamp;
		}

		public boolean hasBeenPlayed()
		{
			return playedTimeStamp != 0;
		}

		public long getDeliveredTimeStamp()
		{
			return deliveredTimeStamp;
		}

		public long getPlayedTimeStamp()
		{
			return playedTimeStamp;
		}

		public ContactInfo getContactInfo(){
			return contactInfo;
		}
	}
	public ConvMessage getConvMessage(){
		return convMessage;
	}
	public static class ListViewMessageView
	{

	}
}
