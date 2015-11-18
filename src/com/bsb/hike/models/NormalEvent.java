package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sagar on 03/11/15.
 */
public class NormalEvent implements Parcelable {
    public String messageHash;
    public String eventData;
    public String namespace;

    public NormalEvent(String messageHash, String eventData, String namespace)
    {
        this.messageHash = messageHash;
        this.eventData = eventData;
        this.namespace = namespace;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(messageHash);
        dest.writeString(eventData);
        dest.writeString(namespace);
    }

    public NormalEvent(Parcel source)
    {
        this.messageHash = source.readString();
        this.eventData = source.readString();
        this.namespace = source.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public NormalEvent createFromParcel(Parcel in)
        {
            return new NormalEvent(in);
        }

        public NormalEvent[] newArray(int size)
        {
            return new NormalEvent[size];
        }
    };

}
