package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pushkargupta on 14/12/15.
 */
public class AppState implements Parcelable
{
    public String json;

    public AppState(String json)
    {
        this.json=json;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(json);
    }
    public AppState(Parcel source)
    {
        this.json = source.readString();
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public AppState createFromParcel(Parcel in)
        {
            return new AppState(in);
        }

        public AppState[] newArray(int size)
        {
            return new AppState[size];
        }
    };
}
