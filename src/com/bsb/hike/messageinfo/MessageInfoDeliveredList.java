package com.bsb.hike.messageinfo;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;

import java.util.ArrayList;

/**
 * Created by ravi on 4/27/16.
 */
public class MessageInfoDeliveredList extends MessageInfoList {

    public MessageInfoDeliveredList(int totalNumberofParticipants,int emptyStateText){
        super(totalNumberofParticipants);
        messageStatusHeader=new MessageInfoItem.MessageStatusHeader(HikeMessengerApp.getInstance().getString(R.string.delivered_list), R.drawable.ic_double_tick_blue);
        listType=MessageInfoItem.MesageInfoRemainingItem.DELIVERED_REMAINING;
        remainingItem=new MessageInfoItem.MesageInfoRemainingItem(MessageInfoItem.REMAINING_ITEM,totalNumberofParticipants, MessageInfoAdapter.LIST_REMAINING_GROUP, R.string.emptydeliveredlist,listType);

        remainingItem.remainingItemList=new ArrayList<MessageInfoDataModel.MessageInfoParticipantData>();
        remainingItem.setEmptyStateText(emptyStateText);

    }

    public void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData){
        if(participantData.hasBeenDelivered()&&!participantData.hasRead()){
            allDisplayedContactItems.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT, MessageInfoAdapter.LIST_ONE_TO_N_CONTACT_DELIVERED));
        }else if(!participantData.hasBeenDelivered())
        {
            remainingItem.remainingItemList.add(participantData);
        }
    }
}