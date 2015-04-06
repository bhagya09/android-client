package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

public class MessagePrivateData
{
	private String trackID;
	
	private String msgType;
	
	public String getMsgType()
	{
		return msgType;
	}
	
	public void setMsgType(String msgType)
	{
		this.msgType = msgType;
	}
	
	public String getTrackID()
	{
		return trackID;
	}
	
	public void setTrackID(String trackID)
	{
		this.trackID = trackID;
	}

	public MessagePrivateData(String traclID)
	{
		this.trackID = traclID;
		this.msgType = "-1";
	}
	
	public MessagePrivateData(String traclID, String msgType)
	{
		this.trackID = traclID;
		this.msgType = msgType;
	}

	public JSONObject serialize()
	{
		JSONObject pd = null;
		
		try
		{
			pd = new JSONObject();
			pd.put(HikeConstants.MSG_REL_UID, getTrackID());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		
		return pd;
	}

	@Override
	public String toString()
	{
		return "MessagePrivateData [trackID=" + trackID + ", msgType=" + msgType + "]";
	}
	
	
}
