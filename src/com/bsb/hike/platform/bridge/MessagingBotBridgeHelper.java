package com.bsb.hike.platform.bridge;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.MessagingBotAlarmManager;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.WebMetadata;

public class MessagingBotBridgeHelper
{

	public static void deleteMessage(long mId,String msisdn,BaseAdapter adapter)
	{

		ArrayList<Long> msgIds = new ArrayList<Long>(1);
		msgIds.add(mId);
		Bundle bundle = new Bundle();
		ConvMessage message=HikeConversationsDatabase.getInstance().getLastMessage(msisdn);
		if (message != null && message.getMsgID() == mId)
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, true);
		}
		else
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, false);
		}

		bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
	
	}
	
	
	public static void setAlarm(String json, String timeInMills,Context context,int messageId)
	{
		try
		{
			MessagingBotAlarmManager.setAlarm(context, new JSONObject(json), messageId, Long.valueOf(timeInMills));
		}
		catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public static WebMetadata updateHelperData(long messageId,String json)
	{
		try
		{
			
			String originalmetadata = HikeConversationsDatabase.getInstance().getMetadataOfMessage(messageId);
			originalmetadata = PlatformUtils.updateHelperData(json, originalmetadata);
			if (originalmetadata != null)
			{
				HikeConversationsDatabase.getInstance().updateMetadataOfMessage(messageId, originalmetadata);
				return new WebMetadata(originalmetadata);
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static WebMetadata updateMetadata(int messageId, String json){

		try
		{
			
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata(messageId, json);

			if (updatedJSON != null)
			{
				return new WebMetadata(updatedJSON);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	return null;
	}

	public static void deleteAlarm(int messageId)
	{
		HikeConversationsDatabase.getInstance().deleteAppAlarm(messageId);
	}
	
}
