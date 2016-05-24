package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pushkargupta on 04/04/16.
 */
public class NotifData implements Parcelable
{
    public String key;
    public String msisdn;

    public NotifData(String key, String msisdn)
    {
        this.key =key;
        this.msisdn=msisdn;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(key);
        dest.writeString(msisdn);
    }
    public NotifData(Parcel source)
    {
        this.key = source.readString();
        this.msisdn = source.readString();
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public NotifData createFromParcel(Parcel in)
        {
            return new NotifData(in);
        }

        public NotifData[] newArray(int size)
        {
            return new NotifData[size];
        }
    };
}
