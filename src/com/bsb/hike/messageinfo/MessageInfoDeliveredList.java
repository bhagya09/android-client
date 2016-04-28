package com.bsb.hike.messageinfo;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;
import com.bsb.hike.platform.HikeSDKMessageFilter;

/**
 * Created by ravi on 4/27/16.
 */
public class MessageInfoDeliveredList extends MessageInfoList {
    public MessageInfoDeliveredList(int totalNumberofParticipants,int emptyStateText){
        super(totalNumberofParticipants);
        messageStatusHeader=new MessageInfoItem.MessageStatusHeader(HikeMessengerApp.getInstance().getString(R.string.delivered_list), R.drawable.ic_double_tick);
        remainingItem.setEmptyStateText(emptyStateText);
    }

    public void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData){
        if(participantData.hasBeenDelivered()){
            allDisplayedContactItems.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT, MessageInfoAdapter.LIST_CONTACT_GROUP));
        }else
        {
            remainingItem.remainingItemList.add(participantData);
        }
    }
}
