package com.bsb.hike.messageinfo;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.OneToNConversation;

import java.util.HashSet;
import java.util.TreeMap;

/**
 * Created by ravi on 5/15/16.
 */
public class MessageInfoLoaderData {
    public ConvMessage convMessage;
    public OneToNConversation conversation;
    public HashSet<MessageInfo> messageInfoHashSet;
    public TreeMap<String, MessageInfoDataModel.MessageInfoParticipantData> participantTreeMap;
    public boolean areAnyReceiptsReceived;

}
