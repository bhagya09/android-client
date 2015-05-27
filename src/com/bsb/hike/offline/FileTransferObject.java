package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

public class FileTransferObject
{
	public TransferProgress transferProgress;

	public JSONObject packetData;

	public FileTransferObject(TransferProgress transferProgress, JSONObject packetData)
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
		if (packetData.optJSONObject(HikeConstants.DATA) != null)
		{
			try
			{
				return packetData.getJSONObject(HikeConstants.DATA).optLong(HikeConstants.MESSAGE_ID, -1);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return -1;
	}
}