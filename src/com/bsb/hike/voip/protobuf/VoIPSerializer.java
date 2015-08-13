package com.bsb.hike.voip.protobuf;

import android.util.Log;

import com.bsb.hike.voip.VoIPDataPacket;
import com.bsb.hike.voip.VoIPDataPacket.BroadcastListItem;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.protobuf.DataPacketProtoBuf.DataPacket;
import com.bsb.hike.voip.protobuf.DataPacketProtoBuf.DataPacket.BroadcastHost;
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
		.setVoicePacketNumber(dp.getVoicePacketNumber())
		.setIsVoice(dp.isVoice());
    	
    	// Broadcast List (just needs to be serialized, since only
    	// the relay server needs it)
    	if (dp.getBroadcastList() != null) {
    		DataPacket.BroadcastHost.Builder broadcastListBuilder = DataPacket.BroadcastHost.newBuilder();
    		for (BroadcastListItem item : dp.getBroadcastList()) {
        		broadcastListBuilder.setIp(item.getIp());
        		broadcastListBuilder.setPort(item.getPort());
        		BroadcastHost host = broadcastListBuilder.build();
    			protoBufBuilder.addBroadcastList(host);
    		}
    	}
    	
    	// Multiple audio packets
    	if (dp.getDataList() != null) {
    		for (byte[] data : dp.getDataList())
    			protoBufBuilder.addDataList(ByteString.copyFrom(data));
    	}
    	
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
			dp.setVoice(protoBuf.getIsVoice());

			if (protoBuf.getDataListCount() > 0) {
				for (ByteString data : protoBuf.getDataListList()) {
					dp.addToDataList(data.toByteArray());
				}
			}
			
		} catch (InvalidProtocolBufferException e) {
			Log.e("VoIP Serializer", "Error decoding protocol buffer packet");
		}
		
		return dp;
	}
}
