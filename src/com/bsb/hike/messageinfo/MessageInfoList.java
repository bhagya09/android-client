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
    public static final int BIG_DIVIDER_CONTACTLIST=2;
    protected static int listType=1;
    public boolean shouldAddList(){
        return !(getRemainingItemCount()==0&&allDisplayedContactItems.size()==0);
    }
    public int totalNumberofParticipants;
    public abstract void addParticipant(MessageInfoDataModel.MessageInfoParticipantData participantData);
    public MessageInfoList(int totalNumberofParticipants){

        this.totalNumberofParticipants=totalNumberofParticipants;
        allDisplayedContactItems=new ArrayList<MessageInfoItem.MesageInfoParticipantItem>();


    }
    public void setDivider(){
        if(!allDisplayedContactItems.isEmpty()){
            allDisplayedContactItems.get(allDisplayedContactItems.size()-1).setDividerBehavior(BIG_DIVIDER_CONTACTLIST);
        }
    }
    public int getRemainingItemCount()
    {
        return remainingItem.remainingItemList.size();
    }


}
