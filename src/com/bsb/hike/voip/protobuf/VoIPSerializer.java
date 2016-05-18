package com.bsb.hike.voip.protobuf;

import android.util.Log;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPDataPacket;
import com.bsb.hike.voip.VoIPDataPacket.BroadcastListItem;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.protobuf.DataPacketProtoBuf.DataPacket;
import com.bsb.hike.voip.protobuf.DataPacketProtoBuf.DataPacket.BroadcastHost;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;

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
    	try {
        	if (dp.getDataList() != null) {
        		for (byte[] data : dp.getDataList())
        			protoBufBuilder.addDataList(ByteString.copyFrom(data));
        	}
    	} catch (NullPointerException e) {
    		Logger.w(VoIPConstants.TAG, "VoIPSerializer NullPointerException: " + e.toString());
    	}
    	
    	DataPacket dataPacket = protoBufBuilder.build();
    	return dataPacket.toByteArray();
	}

	
	public static Object deserialize(byte[] bytes, int length) {

		VoIPDataPacket dp = new VoIPDataPacket();

		// Our byte stream has a one-byte prefix that we need to ignore while parsing the protobuf.
		CodedInputStream codedInputStream = CodedInputStream.newInstance(bytes, 1, length - 1);

		DataPacket protoBuf = null;
		try {
            protoBuf = DataPacket.parseFrom(codedInputStream);
        } catch (IOException e) {
            Logger.w(VoIPConstants.TAG, "VoIPSerializer IOException : " + e.toString());
        }

		if (protoBuf == null) {
			Logger.w(VoIPConstants.TAG, "Deserialized protobuf is NULL.");
			return null;
		}

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

		return dp;
	}
}
