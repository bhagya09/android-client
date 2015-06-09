package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * 	@author Deepak Malik
 *
 *	Offline Hike packet based on Hike Packet model for offline related messaging
 */
public class OfflineHikePacket implements Parcelable
{	
	private byte[] message;

	private long msgId;

	private long timeStamp;

	// this is row no. in Offline_messages table in HikePersistenceDb
	private long packetId = -1;

	//This is unique id, for msg tracking
	private String trackId;
	
	private String msisdn;

	public long getPacketId()
	{
		return packetId;
	}

	public void setPacketId(long packetId)
	{
		this.packetId = packetId;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public byte[] getMessage()
	{
		return message;
	}

	public long getMsgId()
	{
		return msgId;
	}

	public OfflineHikePacket(byte[] message, long msgId, long timeStamp, String msisdn)
	{
		this(message, msgId, timeStamp, msisdn, -1);
	}

	public OfflineHikePacket(byte[] message, long msgId, long timeStamp, String msisdn, long packetId)
	{
		this(message, msgId, timeStamp, msisdn, packetId, null);
	}

	public OfflineHikePacket(byte[] message, long msgId, long timeStamp, String msisdn, long packetId, String trackId)
	{
		this.message = message;
		this.msgId = msgId;
		this.timeStamp = timeStamp;
		this.msisdn = msisdn;
		this.packetId = packetId;
		this.trackId = trackId;
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(message.length);
		dest.writeByteArray(message);
		dest.writeLong(msgId);
		dest.writeLong(timeStamp);
		dest.writeString(msisdn);
		dest.writeLong(packetId);;
		dest.writeString(trackId);
	}
	
	public static final Creator<OfflineHikePacket> CREATOR = new Creator<OfflineHikePacket>()
	{
		@Override
		public OfflineHikePacket[] newArray(int size)
		{
			return new OfflineHikePacket[size];
		}

		@Override
		public OfflineHikePacket createFromParcel(Parcel source)
		{
			byte[] message = new byte[source.readInt()];
			source.readByteArray(message);
			long msgId = source.readLong();
			long timeStamp = source.readLong();
			String msisdn = source.readString();
			long packetId = source.readLong();
			String trackId = source.readString();
			OfflineHikePacket hikePacket = new OfflineHikePacket(message, msgId, timeStamp, msisdn, packetId, trackId);
			return hikePacket;
		}
	};

	public String getTrackId()
	{
		return trackId;
	}
	
	public void setTrackId(String trackId)
	{
		this.trackId = trackId;
	}

	public String getMsisdn()
	{
		return msisdn;
	}
	
	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

}
