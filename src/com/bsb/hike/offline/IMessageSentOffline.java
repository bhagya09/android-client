package com.bsb.hike.offline;

import com.bsb.hike.models.ConvMessage;

public interface IMessageSentOffline
{
	public void onSuccess(ConvMessage convMessage);
	
	public void onFailure(ConvMessage convMessage);
}
