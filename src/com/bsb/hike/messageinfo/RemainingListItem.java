package com.bsb.hike.messageinfo;

import android.widget.ImageView;

/**
 * Created by ravi on 5/2/16.
 */
public class RemainingListItem {

    public String name;
    public int drawableStatus;
    public ImageView contactIcon;
    public String msisdn;
    public RemainingListItem(String displayName,String msisdn,int drawableStatus){
        this.name=displayName;
        this.msisdn=msisdn;
        this.drawableStatus=drawableStatus;
    }

}
