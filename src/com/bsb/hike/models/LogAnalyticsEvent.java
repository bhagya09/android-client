package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sagar on 03/11/15.
 */
public class LogAnalyticsEvent implements Parcelable
{
	public String isUI;

	public String subType;

	public String json;

	public String botMsisdn;

	public String botName;

	public LogAnalyticsEvent(String isUI, String subType, String json, String botMsisdn, String botName)
	{
		this.isUI = isUI;
		this.subType = subType;
		this.json = json;
		this.botMsisdn = botMsisdn;
		this.botName = botName;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(isUI);
		dest.writeString(subType);
		dest.writeString(json);
		dest.writeString(botMsisdn);
		dest.writeString(botName);
	}

	public LogAnalyticsEvent(Parcel source)
	{
		this.isUI = source.readString();
		this.subType = source.readString();
		this.json = source.readString();
		this.botMsisdn = source.readString();
		this.botName = source.readString();
	}

	public static final Creator CREATOR = new Creator()
	{
		public LogAnalyticsEvent createFromParcel(Parcel in)
		{
			return new LogAnalyticsEvent(in);
		}

		public LogAnalyticsEvent[] newArray(int size)
		{
			return new LogAnalyticsEvent[size];
		}
	};

}
