package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.bsb.hike.HikeConstants;

public class HikePacket implements Parcelable
{
	private byte[] message;

	private long msgId;

	private long timeStamp;

	private long packetId = -1;
	
	private int packetType = HikeConstants.NORMAL_MESSAGE_TYPE;

	//This is unique id, for msg tracking
	private String trackId;
	
	// This stores 'm'/'nmr'
	private String msgType;
	
	public int getPacketType()
	{
		return packetType;
	}

	public void setPacketType(int packetType)
	{
		this.packetType = packetType;
	}

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

	public HikePacket(byte[] message, long msgId, long timeStamp, int packetType)
	{
		this(message, msgId, timeStamp, -1, packetType);
	}

	public HikePacket(byte[] message, long msgId, long timeStamp, long packetId, int packetType)
	{
		this(message, msgId, timeStamp, packetId, packetType, null);
	}

	public HikePacket(byte[] message, long msgId, long timeStamp, long packetId, int packetType, String trackId)
	{
		this(message, msgId, timeStamp, packetId, packetType, null, null);
	}

	public HikePacket(byte[] message, long msgId, long timeStamp, long packetId, int packetType, String trackId, String msgType)
	{
		this.message = message;
		this.msgId = msgId;
		this.timeStamp = timeStamp;
		this.packetId = packetId;
		this.packetType = packetType;
		this.trackId = trackId;
		this.msgType = msgType;
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
		dest.writeLong(packetId);
		dest.writeInt(packetType);
		dest.writeString(trackId);
		dest.writeString(msgType);
	}

	public static final Creator<HikePacket> CREATOR = new Creator<HikePacket>()
	{
		@Override
		public HikePacket[] newArray(int size)
		{
			return new HikePacket[size];
		}

		@Override
		public HikePacket createFromParcel(Parcel source)
		{
			byte[] message = new byte[source.readInt()];
			source.readByteArray(message);
			long msgId = source.readLong();
			long timeStamp = source.readLong();
			long packetId = source.readLong();
			int packetType = source.readInt();
			String trackId = source.readString();
			String msgType = source.readString();
			HikePacket hikePacket = new HikePacket(message, msgId, timeStamp, packetId, packetType, trackId, msgType);
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

	public String getMsgType()
	{
		return msgType;
	}
	
	public void setMsgType(String msgType)
	{
		this.msgType = msgType;
	}
}
