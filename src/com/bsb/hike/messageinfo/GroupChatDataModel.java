package com.bsb.hike.messageinfo;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.PairModified;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ravi on 4/20/16.
 */
public class GroupChatDataModel extends MessageInfoDataModel
{
	public GroupChatDataModel(String msisdn, long messageID)
	{
		super(msisdn, messageID);
	}

	OneToNConversation oneToNConversation;

	private Map<String, PairModified<GroupParticipant, String>> participantMap;

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

		convMessage=mDb.getMessageFromID(messageID,msisdn);
		messageInfoMap = mDb.getMessageInfo(messageID);

		oneToNConversation = (OneToNConversation) mDb.getConversation(msisdn, 0, false);
		participantMap = oneToNConversation.getConversationParticipantList();

		Iterator it = participantMap.values().iterator();
		while (it.hasNext())
		{
			PairModified<GroupParticipant,String> pair = (PairModified<GroupParticipant,String>) it.next();
			GroupParticipant p=pair.getFirst();
			participantTreeMap.put(p.getContactInfo().getMsisdn(), new MessageInfoParticipantData(p.getContactInfo(), 0, 0));
		}
		if (messageInfoMap != null)
		{
			Iterator<MessageInfo> iterator = messageInfoMap.iterator();
			while (iterator.hasNext())
			{
				MessageInfo info = iterator.next();
                MessageInfoParticipantData participant = participantTreeMap.get(info.getReceiverMsisdn());
				//If the participant is no longer in group ,we are not showing his information right now
				if(participant!=null){
				participant.setDeliveredTimeStamp(info.getDeliveredTimestamp());
				participant.setReadTimeStamp(info.getReadTimestamp());
				participant.setPlayedTimeStamp(info.getPlayedTimestamp());
				}
			}
		}
	}
	public void refreshData(){

		messageInfoMap = mDb.getMessageInfo(messageID);
		if (messageInfoMap != null)
		{
			Iterator<MessageInfo> iterator = messageInfoMap.iterator();
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

}
