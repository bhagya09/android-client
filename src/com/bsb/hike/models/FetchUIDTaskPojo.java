package com.bsb.hike.models;

/**
 * Created by himanshu on 04/05/16.
 */
public class FetchUIDTaskPojo {

    private String msisdn;

    private String uid;

    // Hike Id

    //Profile Name

    public FetchUIDTaskPojo(String msisdn, String uid) {
        this.msisdn = msisdn;
        this.uid = uid;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getUid() {
        return uid;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
