package com.bsb.hike.offline;

import org.json.JSONObject;

import com.bsb.hike.models.ConvMessage;

public interface IMessageSentOffline
{
	public void onSuccess(JSONObject packet);

	public void onFailure(JSONObject packet);
}
