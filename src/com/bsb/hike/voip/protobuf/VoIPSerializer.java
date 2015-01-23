package com.bsb.hike.voip.protobuf;

import android.util.Log;

import com.bsb.hike.voip.VoIPDataPacket;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.protobuf.DataPacketProtoBuf.DataPacket;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class VoIPSerializer {
	
	public static byte[] serialize (VoIPDataPacket dp) {
		
		DataPacket.Builder protoBufBuilder = DataPacket.newBuilder();
		
		if (dp.getData() != null)
			protoBufBuilder.setData(ByteString.copyFrom(dp.getData()));
		
		if (dp.getDestinationIP() != null) 
			protoBufBuilder.setDestinationIP(dp.getDestinationIP());
		
    	protoBufBuilder.setEncrypted(dp.isEncrypted())
		.setPacketType(dp.getType().getValue())
		.setDestinationPort(dp.getDestinationPort())
		.setPacketNumber(dp.getPacketNumber())
		.setRequiresAck(dp.isRequiresAck())
		.setTimestamp(dp.getTimestamp())
		.setVoicePacketNumber(dp.getVoicePacketNumber());
    	
    	DataPacket dataPacket = protoBufBuilder.build();
    	return dataPacket.toByteArray();
		
	}

	
	public static Object deserialize(byte[] bytes) {

		VoIPDataPacket dp = new VoIPDataPacket();

		try {
			DataPacket protoBuf = DataPacket.parseFrom(bytes);
			
			dp.setPacketType(PacketType.fromValue(protoBuf.getPacketType()));
			dp.setEncrypted(protoBuf.getEncrypted());
			dp.setData(protoBuf.getData().toByteArray());
			dp.setDestinationIP(protoBuf.getDestinationIP());
			dp.setDestinationPort(protoBuf.getDestinationPort());
			dp.setPacketNumber(protoBuf.getPacketNumber());
			dp.setRequiresAck(protoBuf.getRequiresAck());
			dp.setVoicePacketNumber(protoBuf.getVoicePacketNumber());
			dp.setTimestamp(protoBuf.getTimestamp());
			
		} catch (InvalidProtocolBufferException e) {
			Log.e("VoIP Serializer", "Error decoding protocol buffer packet");
		}
		
		return dp;
	}
}
