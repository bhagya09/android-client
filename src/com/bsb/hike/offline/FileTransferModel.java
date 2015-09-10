package com.bsb.hike.offline;

import org.json.JSONObject;

import com.bsb.hike.models.ConvMessage;

/**
 * 
 * @author himanshu This class contains information about progress of the file and the JSON that will make the file
 * 
 */
public class FileTransferModel
{
	private TransferProgress transferProgress;

	private ConvMessage convMessage;

	public FileTransferModel(TransferProgress transferProgress, ConvMessage convMessage)
	{
		this.transferProgress = transferProgress;
		this.convMessage = convMessage;
	}

	public TransferProgress getTransferProgress()
	{
		return transferProgress;
	}

	public JSONObject getPacket()
	{
		return convMessage.serialize();
	}

	public ConvMessage getConvMessage()
	{
		return convMessage;
	}

	public long getMessageId()
	{
		return convMessage.getMsgID();
	}

}