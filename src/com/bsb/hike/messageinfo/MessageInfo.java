package com.bsb.hike.messageinfo;

/**
 * Created by ravi on 4/18/16.
 */
public class MessageInfo {
    String receiverMsisdn;
    long read_timestamp;
    long delivered_timestamp;
    long played_timestamp;

    public MessageInfo(String receiverMsisdn,long read_timestamp,long delivered_timestamp){
      this.receiverMsisdn=receiverMsisdn;
        this.read_timestamp=read_timestamp;
        this.delivered_timestamp=delivered_timestamp;
    }

    public long getDeliveredTimestamp() {

        return delivered_timestamp;
    }
    public long getReadTimestamp()
    {
        return read_timestamp;

    }
    public long getPlayedTimestamp(){
        return played_timestamp;
    }
    public String getReceiverMsisdn(){
        return receiverMsisdn;
    }

}
