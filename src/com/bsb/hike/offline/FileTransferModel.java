package com.bsb.hike.offline;

import org.json.JSONObject;

/**
 * 
 * @author himanshu
 *	This class contains information about progress of the file and the JSON that will make the file
 *
 */
public class FileTransferModel
{
	private TransferProgress transferProgress;

	private JSONObject packetData;

	public FileTransferModel(TransferProgress transferProgress, JSONObject packetData)
	{
		this.transferProgress = transferProgress;
		this.packetData = packetData;
	}

	public TransferProgress getTransferProgress()
	{
		return transferProgress;
	}

	public JSONObject getPacket()
	{
		return packetData;
	}

	public long getMessageId()
	{
		return OfflineUtils.getMsgId(getPacket());
	}
	
	
}