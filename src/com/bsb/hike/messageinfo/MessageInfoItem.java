package com.bsb.hike.messageinfo;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by ravi on 4/21/16.
 */

public abstract class MessageInfoItem
{

    public static final int HEADER_ID = -1;

    public static final int EMPTY_ID = -2;

    public static final int REQUEST_ID = -3;

    public static final int SHARED_MEDIA = -5;

    public static final int SHARED_CONTENT = -6;

    public static final int MEMBERS = -7;

    public static final int ADD_MEMBERS = -8;

    public static final int GROUP_MEMBER = -9;
    public static final int PHONE_NUMBER = -11;

    public static final int GROUP_SETTINGS = -12;

    public static final int GROUP_RIGHTS_INFO = -13;

    //TODO: Need to find a better way
    public static final int READLIST=1;
    public static final int DELIVEREDLIST=2;
    public static final int PLAYEDLIST=3;
    public static final int READ_CONTACT =4;
    public static final int DELIVERED_CONTACT =5;
    public static final int PLAYED_CONTACT =6;
    private int type=DELIVEREDLIST;

    protected Context mContext= HikeMessengerApp.getInstance().getApplicationContext();

    private int itemId;

    private Object text;

    public MessageInfoItem(int itemId, Object text)
    {
        this.itemId = itemId;
        if(text != null)
            this.text = text;
    }

    public int getItemId()
    {
        return itemId;
    }

    public Object getText()
    {
        return text;
    }

    public static class MessageStatusHeader extends MessageInfoItem
    {

        private String headerString;

        public MessageStatusHeader(int itemId)
        {
            super(itemId, null);
        }

        public MessageStatusHeader(int itemId,String headerString)
        {

            super(itemId, null);
            this.headerString = headerString;
        }
        public void setHeaderString(String headerString){
            this.headerString=headerString;
        }

        public String getHeaderString()
        {
            return headerString;
        }
    }



    public static class PhoneNumberInfo extends MessageInfoItem
    {
        public PhoneNumberInfo(int itemId, Object text)
        {
            super(itemId, text);
        }
    }

    public static class MesageInfoContactItem extends MessageInfoItem
    {


        private String name;

        private String number;

        public MesageInfoContactItem(int itemId,String name,String number)
        {
            super(itemId, name==null?number:name);
            this.name=name;
            this.number=number;
            // TODO Auto-generated constructor stub

        }


    }
    public static class MesageInfoParticipantItem extends MessageInfoItem
    {

        private int type=DELIVEREDLIST;
        private String name;

        private String number;

        private long readTimeStamp,deliveryTimeStamp,playedTimeStamp;

        MessageInfoDataModel.MessageInfoParticipantData participantData;

        public MesageInfoParticipantItem(int itemId,String name,String number)
    {
        super(itemId, name==null?number:name);
        this.name=name;
        this.number=number;
        // TODO Auto-generated constructor stub

    }

        public MesageInfoParticipantItem(MessageInfoDataModel.MessageInfoParticipantData participantData,int itemId,int type)
        {
            super(itemId, participantData.contactInfo.getMsisdn());
            this.participantData=participantData;
            this.type=type;
            readTimeStamp=participantData.getReadTimeStamp();
            deliveryTimeStamp=participantData.getDeliveredTimeStamp();
            playedTimeStamp=participantData.getPlayedTimeStamp();

            // TODO Auto-generated constructor stub

        }
        public void setReadTimeStamp(long readTimeStamp){
            this.readTimeStamp=readTimeStamp;
    }
        public void setDeliveryTimeStamp(long deliveryTimeStamp){
            this.deliveryTimeStamp=deliveryTimeStamp;
        }
        public void setPlayedTimeStamp(long playedTimeStamp){
            this.playedTimeStamp=playedTimeStamp;
        }
        public String getReadTimeStamp(){
            return Utils.getFormattedTime(false,mContext,participantData.getReadTimeStamp());
        }

        public String getDeliveredTimeStamp(){
            return Utils.getFormattedTime(false,mContext,participantData.getDeliveredTimeStamp());
        }
        public String getPlayedTimeStamp(){
            return Utils.getFormattedTime(false,mContext,participantData.getPlayedTimeStamp());
        }
        public String getInfo()
        {
            return participantData.getContactInfo().getNameOrMsisdn();
        }
        public boolean hasbeenDelivered()
        {
            return participantData.hasBeenDelivered();
        }
        public boolean hasRead()
        {
            return participantData.hasRead();
        }
        public boolean hasPlayed(){
            return participantData.hasBeenPlayed();
        }
        public ContactInfo getContactInfo(){
            return participantData.contactInfo;
        }
        public String getTimeStampDescription(){
            String desc="None";
            Logger.d("MessageInfo"," read "+readTimeStamp+" delivery timestamp "+deliveryTimeStamp+" played Time "+playedTimeStamp);
            switch (type){
                case READ_CONTACT:
                    if(readTimeStamp==0)
                        desc="Not yet";
                    else
                    desc="Read at "+getReadTimeStamp();
                    break;
                case PLAYED_CONTACT:
                    if(playedTimeStamp==0)
                        desc="Not yet";
                    else
                    desc="Played at "+getPlayedTimeStamp();
                    break;
                case DELIVERED_CONTACT:
                    if(deliveryTimeStamp==0)
                        desc="Not delivered yet";
                    else
                    desc="Delivered at "+getDeliveredTimeStamp();
                    break;
                 default:
                    desc="No activity";




            }
            return desc;
        }

    }
    public static class MesageInfoReadItem extends MessageInfoItem
    {


        private String name;

        private String number;

        public MesageInfoReadItem(int itemId,String name,String number)
        {
            super(itemId, name==null?number:name);
            this.name=name;
            this.number=number;
            // TODO Auto-generated constructor stub
        }


    }



}