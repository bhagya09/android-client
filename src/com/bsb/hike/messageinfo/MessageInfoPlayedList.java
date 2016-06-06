package com.bsb.hike.messageinfo;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;

/**
 * Created by ravi on 4/27/16.
 */
public class MessageInfoPlayedList extends MessageInfoList {
    public MessageInfoPlayedList(int totalNumberofParticipants,int emptyStateText){
        super(totalNumberofParticipants);
        messageStatusHeader=new MessageInfoItem.MessageStatusHeader(HikeMessengerApp.getInstance().getString(R.string.played_list), R.drawable.ic_double_tick_r);
        remainingItem.setEmptyStateText(emptyStateText);
    }

    public void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData){
        if(participantData.hasBeenPlayed()){
            allDisplayedContactItems.add(new MessageInfoItem.MesageInfoParticipantItem(participantData,MessageInfoItem.MesageInfoParticipantItem.PLAYED_CONTACT, MessageInfoAdapter.LIST_ONE_TO_N_CONTACT_READ));
        }else
        {
            remainingItem.remainingItemList.add(participantData);
        }
    }
}
