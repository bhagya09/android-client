package com.bsb.hike.messageinfo;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ravi on 4/20/16.
 */
public class OnetoOneDataModel extends MessageInfoDataModel
{

	public OnetoOneDataModel(String msisdn, long messageID)
	{
		super(msisdn, messageID);
	}

	@Override
	public HashMap<String, MessageInfoParticipantData> getAllDeliveredMembers()
	{
		return null;
	}

	@Override
	public HashMap<String, MessageInfoParticipantData> getAllReadMembers()
	{
		return null;
	}

	@Override
	public HashMap<String, MessageInfoParticipantData> getAllParticipants(String msisdn)
	{
		return null;
	}

	@Override
	public void fetchAllParticipantsInfo()
	{

		ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, true);
		messageInfoMap = mDb.getMessageInfo(messageID);
		convMessage=mDb.getMessageFromID(messageID,msisdn);
		Iterator<MessageInfo> iterator = messageInfoMap.iterator();
		participantTreeMap.put(contactInfo.getMsisdn(), new MessageInfoParticipantData(contactInfo, 0, 0));
		while (iterator.hasNext())
		{
			MessageInfo info = iterator.next();
			MessageInfoParticipantData participant = participantTreeMap.get(info.getReceiverMsisdn());
			participant.setDeliveredTimeStamp(info.getDeliveredTimestamp());
			participant.setReadTimeStamp(info.getReadTimestamp());
			participant.setPlayedTimeStamp(info.getPlayedTimestamp());
		}
	}


}
