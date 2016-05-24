package com.bsb.hike.messageinfo;

import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;

import java.util.ArrayList;

/**
 * Created by ravi on 4/27/16.
 */
public class MessageInfoReadList extends MessageInfoList {

    public MessageInfoReadList(int totalNumberofParticipants,String header,int emptyStateText){
        super(totalNumberofParticipants);
        messageStatusHeader=new MessageInfoItem.MessageStatusHeader(header, R.drawable.ic_double_tick_r_blue);
        listType=MessageInfoItem.MesageInfoRemainingItem.READ_REMAINING;
        remainingItem=new MessageInfoItem.MesageInfoRemainingItem(MessageInfoItem.REMAINING_ITEM,totalNumberofParticipants, MessageInfoAdapter.LIST_REMAINING_GROUP, R.string.emptydeliveredlist,listType);

        remainingItem.remainingItemList=new ArrayList<MessageInfoDataModel.MessageInfoParticipantData>();
        remainingItem.setEmptyStateText(emptyStateText);
    }

    public void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData){
        if(participantData.hasRead()){
            allDisplayedContactItems.add(new MessageInfoItem.MesageInfoParticipantItem(participantData,MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT, MessageInfoAdapter.LIST_ONE_TO_N_CONTACT_READ));
        }else
        {
            remainingItem.remainingItemList.add(participantData);
        }
    }

}
