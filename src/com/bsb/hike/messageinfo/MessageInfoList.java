package com.bsb.hike.messageinfo;

import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ravi on 4/25/16.
 */
public abstract class MessageInfoList {
    public List<MessageInfoItem.MesageInfoParticipantItem> allDisplayedContactItems;
    public MessageInfoItem.MesageInfoRemainingItem remainingItem;
    public MessageInfoItem.MessageStatusHeader messageStatusHeader;
    public boolean shouldAddList(){
        return !(getRemainingItemCount()==0&&allDisplayedContactItems.size()==0);
    }
    public int totalNumberofParticipants;
    public abstract void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData);
    public MessageInfoList(int totalNumberofParticipants){

        this.totalNumberofParticipants=totalNumberofParticipants;
        allDisplayedContactItems=new ArrayList<MessageInfoItem.MesageInfoParticipantItem>();
        remainingItem=new MessageInfoItem.MesageInfoRemainingItem(MessageInfoItem.REMAINING_ITEM,totalNumberofParticipants, MessageInfoAdapter.LIST_REMAINING_GROUP, R.string.emptydeliveredlist);
        remainingItem.remainingItemList=new ArrayList<MessageInfoDataModel.MessageInfoParticipantData>();


    }
    public int getRemainingItemCount()
    {
        return remainingItem.remainingItemList.size();
    }


}
