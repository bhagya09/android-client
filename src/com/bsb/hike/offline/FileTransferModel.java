package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

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