package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pushkargupta on 11/01/16.
 */
public class EventData implements Parcelable
{
    public String isEventId;
    public String id;


    public EventData(Boolean isEventId,String id)
    {
        this.isEventId = String.valueOf(isEventId);
        this.id =id;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(isEventId);
        dest.writeString(id);
    }

    public EventData(Parcel source)
    {
        this.isEventId = source.readString();
        this.id = source.readString();
    }

    public static final Creator CREATOR = new Creator()
    {
        public EventData createFromParcel(Parcel in)
        {
            return new EventData(in);
        }

        public EventData[] newArray(int size)
        {
            return new EventData[size];
        }
    };
}
