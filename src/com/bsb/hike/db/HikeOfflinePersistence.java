package com.bsb.hike.db;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.dbcommand.SetPragmaModeCommand;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.utils.Logger;
import com.hike.transporter.interfaces.IPersistanceInterface;
import com.hike.transporter.models.SenderConsignment;

/**
 * 
 * 	@author Deepak Malik
 *
 *	Contains Persistence related functions for Offline related messaging based on HikeMqttPersistence
 */
public class HikeOfflinePersistence implements IPersistanceInterface
{
	private static final String TAG = "HikeOfflinePersistence";

	private SQLiteDatabase mDb;
	
	private static HikeOfflinePersistence hikeOfflinePersistence;
	
	private HikeMqttPersistence hikeMqttPersistence;
	
	public static HikeOfflinePersistence getInstance()
	{
		if(hikeOfflinePersistence == null)
		{
			hikeOfflinePersistence = new HikeOfflinePersistence();
		}
		return hikeOfflinePersistence;
	}
	
	private HikeOfflinePersistence()
	{
		hikeMqttPersistence = HikeMqttPersistence.getInstance();
		mDb = hikeMqttPersistence.getDb();
		SetPragmaModeCommand setPragmaModeCommand = new SetPragmaModeCommand(mDb);
		setPragmaModeCommand.execute();
	}

	@Override
	public void addToPersistance(String nameSpace, String message, long awb)
	{
		HikePacket hikePacket = getHikePacket(nameSpace, awb, message);
		if (hikePacket == null)
		{
			Logger.d(TAG, "HikePacket is null");
			return;
		}
		else
		{
			try 
			{
				hikeMqttPersistence.addSentMessage(hikePacket);
				Logger.d(TAG, "Inserted in to Db with AWB number is "+awb);
			} 
			catch (MqttPersistenceException e) 
			{
				Logger.e(TAG, "Unable to Persist message!!", e);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void deleteFromPersistance(long awb)
	{
		hikeMqttPersistence.removeMessage(awb);
		Logger.d(TAG, "Deleted  in to Db with awb Number "+awb);
	}

	@Override
	public void deleteFromPersistance(String nameSpace)
	{
		Logger.d(TAG, "Persistence contains no column for namespace/msisdn!");
	}
	
	@Override
	public void deleteFromPersistance(List<Long> listAwbNumber) {
		hikeMqttPersistence.removeMessages(listAwbNumber);
	}

	@Override
	public void deleteAll()
	{
		hikeMqttPersistence.removeMessage(HikeConstants.OFFLINE_MESSAGE_TYPE);
		Logger.d(TAG, "Deleted  in to Db with type "+ HikeConstants.OFFLINE_MESSAGE_TYPE);
	}

	@Override
	public List<SenderConsignment> getAllPendingMsgs(String nameSpace)
	{
		Logger.d(TAG, "Get all pending messages for msisdn: " + nameSpace);
		List<ConvMessage> unreceivedConvMessages = HikeConversationsDatabase.getInstance().getUnDeliveredMessages(nameSpace);
		if (unreceivedConvMessages == null || unreceivedConvMessages.isEmpty())
			return null;
		
		List<SenderConsignment> senderConsignments = new ArrayList<>();
		SenderConsignment senderConsignment=null;
		for(ConvMessage convMessage : unreceivedConvMessages)
		{
			senderConsignment = getSenderConsignment(convMessage);
			if(senderConsignment!=null)
			{
				senderConsignments.add(senderConsignment);
			}
		}
		return senderConsignments;
	}
	
	private HikePacket getHikePacket(String msisdn, long msgId, String message)
	{
		HikePacket hikePacket = null;
		try 
		{
			hikePacket = new HikePacket(message.getBytes("UTF-8"), msgId, System.currentTimeMillis(), HikeConstants.OFFLINE_MESSAGE_TYPE);
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		return hikePacket;
	}
	
	private SenderConsignment getSenderConsignment(ConvMessage convMessage) 
	{
		if (!convMessage.isFileTransferMessage())
			return OfflineController.getInstance().getSenderConsignment(convMessage, false);
		else 
			return null;
	}

}
