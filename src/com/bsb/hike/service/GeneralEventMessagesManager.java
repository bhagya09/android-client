package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.offline.OfflineUtils;

import android.content.Context;


public class GeneralEventMessagesManager
{
	private final Context context;
	
	private static volatile GeneralEventMessagesManager instance;
	
	private GeneralEventMessagesManager(Context context)
	{
		this.context = context;
	}
	
	public void handleGeneralMessage(JSONObject packet) throws JSONException
	{
		JSONObject data  = packet.getJSONObject(HikeConstants.DATA);
		if (data!=null)
		{
			String type = data.optString(HikeConstants.TYPE);
			
			if(HikeConstants.GeneralEventMessagesTypes.OFFLINE.equals(type))
			{
				if (data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.OFFLINE_MESSAGE_REQUEST))
				{
					OfflineUtils.handleOfflineRequestPacket(context, packet);
				}
				if(data.optString(HikeConstants.SUB_TYPE).equals(HikeConstants.OFFLINE_MESSAGE_REQUEST_CANCEL))
				{
					OfflineUtils.handleOfflineCancelRequestPacket(context,packet);
				}
			}
			
		}
	}

	public static GeneralEventMessagesManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (GeneralEventMessagesManager.class)
			{
				if (instance == null)
				{
					instance = new GeneralEventMessagesManager(context);
				}
			}
		}
		return instance;
	}
	
}
