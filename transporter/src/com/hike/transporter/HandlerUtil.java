package com.hike.transporter;

import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.TConstants.State;
import com.hike.transporter.utils.TConstants.THandlerConstants;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu
 * 
 * A default Thread on which all the communication happens
 *
 */
public class HandlerUtil extends Handler
{
	public static volatile HandlerUtil _instance = null;

	private Config config = null;

	private HandlerUtil(Looper looper, Config config)
	{
		super(looper);
		this.config = config;
	}

	@Override
	public void handleMessage(Message msg)
	{
		if (msg == null)
		{
			return;
		}
		handlerMsgOnBackEndThread(msg);
	}

	private void handlerMsgOnBackEndThread(Message msg)
	{
		switch (msg.what)
		{
		case THandlerConstants.SAVE_MSG_DB:
			saveMsgDb((SenderConsignment) msg.obj);
			break;

		case THandlerConstants.DEL_MSG_DB:
			deleteMsgFromDb((long) msg.obj);
			break;

		case THandlerConstants.SEND_ALL_MSG:
			sendAllPreviousMsg((String) msg.obj);
			break;

		case THandlerConstants.SEND_HEARTBEAT_PACKET:
			sendHeartBeatPacket();
			Message newMsg = Message.obtain(msg);
			
			// this case happens when we remove all messages form the queue and this one is still processing so this will 
				//continue posting heartBeat pkt.
			if (Transporter.getInstance().getState() == State.CONNECTED)
			{
				sendMessageDelayed(newMsg, TConstants.GHOST_PACKET_SEND_TIME);
			}
			break;

		case THandlerConstants.DISCONNECT_AFTER_TIMEOUT:
			if ((System.currentTimeMillis() - Transporter.getInstance().getLastActivitytime() >= config.getKeepAlive()))
			{
				Transporter.getInstance().shutDown(TException.HEARTBEAT_TIMEOUT);
			}
			else
			{
				Logger.d("Transporter", "Posting on Handler");
				Utils.postDisconnectForGhostPackets(true, config);
			}
			break;
		}
	}

	private void sendHeartBeatPacket()
	{
		Logger.d("Transporter", "In sendGhostPacket");
		if (Transporter.getInstance().getState() == State.CONNECTED)
		{
			SenderConsignment senderConsignment = Utils.createHeartBeatPacket(config.getAckTopic());
			Transporter.getInstance().publish(senderConsignment);
		}
	}

	private void sendAllPreviousMsg(String nameSpace)
	{
		List<SenderConsignment> listPendingMsg = DBManager.getInstance().getAllPendingMsgs(nameSpace);
		if (listPendingMsg != null)
		{
		Logger.d("Transporter", "Going to send all saved message from DB");
			for (SenderConsignment senderConsignment : listPendingMsg)
			{
				Transporter.getInstance().publish(senderConsignment);
			}
		}
	}

	private void deleteMsgFromDb(long awb)
	{
		DBManager.getInstance().deleteFromPersistance(awb);
	}

	private void saveMsgDb(SenderConsignment senderConsignment)
	{
		if (senderConsignment.persistance)
		{
			DBManager.getInstance().addToPersistance(config.getNamespace(), senderConsignment.message, senderConsignment.getAwb());
		}
	}

	public static HandlerUtil getInstance(Looper looper, Config config)
	{
		if (_instance == null)
		{
			synchronized (HandlerUtil.class)
			{
				if (_instance == null)
				{
					_instance = new HandlerUtil(looper, config);
				}
			}
		}
		return _instance;
	}

	public static HandlerUtil getInstance()
	{
		return _instance;
	}

}
