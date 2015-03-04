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
		this.message = message;
		this.msgId = msgId;
		this.timeStamp = timeStamp;
		this.packetId = packetId;
		this.packetType = packetType;
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
			return new HikePacket(message, msgId, timeStamp, packetId, packetType);
		}
	};
}
