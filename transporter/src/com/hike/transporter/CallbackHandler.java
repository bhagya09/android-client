package com.hike.transporter;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.text.TextUtils;

import com.hike.transporter.interfaces.IConnectionConnect;
import com.hike.transporter.interfaces.IConnectionListener;
import com.hike.transporter.interfaces.IConsigneeListener;
import com.hike.transporter.interfaces.IConsignerListener;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.DataManager;
import com.hike.transporter.models.ReceiverConsignment;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.models.Topic;
import com.hike.transporter.models.TopicResources;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.TConstants.ERRORCODES;
import com.hike.transporter.utils.TConstants.State;
import com.hike.transporter.utils.TConstants.THandlerConstants;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu,GauravK
 * 
 *         This class deals with all the callbacks that are received from the consignee and consigner thread.It passes the callback back to the user based on response codes.
 * 
 */
public class CallbackHandler implements IConnectionConnect, IConsignerListener, IConsigneeListener
{

	private static final String TAG = "Transporter";

	private AtomicInteger numberOfConnections = new AtomicInteger(0);

	private Config config;

	private Object Lock = new Object();

	private IConnectionListener connectionCallback;

	private IMessageSent messageSentCallback;

	private IMessageReceived messageReceivedCallback;

	Context context;

	private ScreenToggleReceiver receiver = null;
	
	private volatile int totalConnections=0;

	public CallbackHandler(IConnectionListener connectionCallback, IMessageSent messageSentCallback, IMessageReceived messageReceivedCallback, Config config, Context context)
	{
		this.connectionCallback = connectionCallback;
		this.messageSentCallback = messageSentCallback;
		this.messageReceivedCallback = messageReceivedCallback;
		this.config = config;
		this.context = context;
		init();
	}

	private void init()
	{
		registerReceiver(context);
	}

	/**
	 * This is called when Client connection is made and sends as Handshake packet to the server to register the stream vs the topic
	 */
	@Override
	public void onConnectionMade(Socket soc)
	{
		try
		{
			LinkedBlockingQueue<SenderConsignment> blockingQueue = new LinkedBlockingQueue<SenderConsignment>();

			Consigner senderRunnable = new Consigner(soc, this, blockingQueue);

			Consignee receiverRunnable = new Consignee(soc, this);

			Logger.d(TAG, "Current Connected  ..." + numberOfConnections.get());

			synchronized (Lock)
			{
				TopicResources resources = new TopicResources(config.getTopics().get(numberOfConnections.get()), senderRunnable, receiverRunnable, blockingQueue);
				DataManager.getInstance().putInTopicMap(config.getTopics().get(numberOfConnections.get()).getName(), resources);

				Logger.d(TAG,"Total topic is "+config.getTopics().size());
				try
				{
					if (numberOfConnections.get() + 1 == config.getTopics().size())
					{
						// create config packet
						Logger.d(TAG,"All Threads Connected Will send congig pkt in handshake");
						blockingQueue.put(Utils.createHandShakePkt(config.getTopics().get(numberOfConnections.get()), Transporter.getInstance().getConfig()));
					}
					else
					{
						blockingQueue.put(Utils.createHandShakePkt(config.getTopics().get(numberOfConnections.get()), null));
					}

				}
				catch (InterruptedException e)
				{
					Logger.d(TAG, "Exception in Connection MAde");
					e.printStackTrace();
				}
				numberOfConnections.incrementAndGet();
				Logger.d(TAG, "The number of connection connected is " + numberOfConnections.get());
				receiverRunnable.start("ReceiverThread");
			}
			senderRunnable.run();

		}
		catch (TException e1)
		{
			e1.printStackTrace();
		}

	}

	/**
	 * Called when the connection is successfull ,both from consigner and consignee end.
	 */
	private void onConnectionSuccess()
	{
		Logger.d(TAG, "Calling OnConnection Success");
		Transporter.getInstance().setState(State.CONNECTED);
		if (config.isSendOldPersistedPackages())
		{
			Message msg = Message.obtain();
			msg.what = THandlerConstants.SEND_ALL_MSG;
			msg.obj = config.getNamespace();
			HandlerUtil.getInstance().sendMessage(msg);
		}
		connectionCallback.onConnect();
		startSendingGhostPackets();
		Utils.postDisconnectForGhostPackets(Utils.getScreenStatus(),config);
	}

	/**
	 * This is called when the server receives a handshake packet from the client.This packet register the stream for the topic.
	 * If an config packet is received,then the server sets	 it configuration.
	 */
	@Override
	public void onHandShake(TransporterRunnable runnable, JSONObject json, Socket socket)
	{
		try
		{
			Logger.d(TAG, "Handshake Received");
			LinkedBlockingQueue<SenderConsignment> blockingQueue = new LinkedBlockingQueue<SenderConsignment>();
			Consigner senderRunnable;

			senderRunnable = new Consigner(socket, this, blockingQueue);

			String topic = Utils.getTopicFromHandShake(json);
			Topic t = new Topic(topic);
			TopicResources resources = new TopicResources(t, senderRunnable, (Consignee) runnable, blockingQueue);
			DataManager.getInstance().putInTopicMap(topic, resources);

			List<Topic> listTopics = config.getTopics();
			listTopics.add(t);
			String config = Utils.getConfig(json);
			senderRunnable.start("SenderRunnable");
			if (!TextUtils.isEmpty(config))
			{
				Transporter.getInstance().getConfig().deserialize(config);
				totalConnections=Utils.getTotalConnections(json);
			}
			if (numberOfConnections.incrementAndGet() == totalConnections && Transporter.getInstance().getState() != State.CONNECTED)
			{
				Transporter.getInstance().setState(State.CONNECTED);
				SenderConsignment senderConsignment = Utils.createHandShakePktFromServer(Transporter.getInstance().getConfig().getAckTopic());
				Transporter.getInstance().publish(senderConsignment);
				onConnectionSuccess();
			}
		}
		catch (TException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public void onAckReceived(long awb)
	{
		Logger.d(TAG, "onAckReceived" + awb + "");
		// Delete the message from the db as well.
		if (awb != -1)
		{
			SenderConsignment consignment = DataManager.getInstance().getSenderConsigment(awb);
			if (consignment != null)
			{
				messageSentCallback.onMessageDelivered(consignment);
				DataManager.getInstance().remove(awb);
			}
		}
	}

	@Override
	public void onHeartBeat(boolean screenOn)
	{
		Utils.postDisconnectForGhostPackets(screenOn, config);
	}

	@Override
	public void onApplicationData(ReceiverConsignment receiverConsignment)
	{
		Logger.d(TAG, "onApplicationData Received");
		if (receiverConsignment.type == TConstants.TEXT && receiverConsignment.getAwb() != -1)
		{
			Logger.d(TAG, "onApplicationData Received" + "creating ack packet for Text");
			SenderConsignment senderConsignment = Utils.createAckPacket(receiverConsignment.getAwb(), config.getAckTopic());
			DataManager.getInstance().publishMsgToQueue(senderConsignment);
		}

		messageReceivedCallback.onMessageReceived(receiverConsignment);

	}

	@Override
	public void onChunkRead(ReceiverConsignment receiverConsignment)
	{
		messageReceivedCallback.onChunkRead(receiverConsignment);
		Transporter.getInstance().setLastActivityTime(System.currentTimeMillis());
	}

	@Override
	public void onFileCompleted(ReceiverConsignment receiverConsignment)
	{
		
		if (receiverConsignment.type == TConstants.FILE && receiverConsignment.getAwb() != -1)
		{
			Logger.d(TAG, "onApplicationData Received" + "creating ack packet for File" + receiverConsignment.getAwb());
			SenderConsignment senderConsignment = Utils.createAckPacket(receiverConsignment.getAwb(), config.getAckTopic());
			DataManager.getInstance().publishMsgToQueue(senderConsignment);
		}
		messageReceivedCallback.onFileCompleted(receiverConsignment);
	}

	@Override
	public void onTransitBegin(long awb)
	{
		if (awb != -1)
		{
			SenderConsignment consignment = DataManager.getInstance().getSenderConsigment(awb);
			if (consignment != null)
			{
				messageSentCallback.onTransitBegin(consignment);
			}
			
		}
	}

	@Override
	public void onTransitEnd(long awb)
	{
		if (awb != -1)
		{
			SenderConsignment consignment = DataManager.getInstance().getSenderConsigment(awb);
			if (consignment != null)
			{
				messageSentCallback.onTransitEnd(consignment);
			}
		}
	}

	@Override
	public void onChunkSend(long awb, int fileSend)
	{
		if (awb != -1)
		{
			SenderConsignment senderConsignment = DataManager.getInstance().getSenderConsigment(awb);
			if (senderConsignment != null)
			{
				senderConsignment.setSendFileSize(fileSend);
				messageSentCallback.onChunkSend(senderConsignment);
				Transporter.getInstance().setLastActivityTime(System.currentTimeMillis());
			}
		}
	}

	@Override
	public void onErrorOccuredConsignee(TException e)
	{
		shutDown(e);
	}

	public void shutDown(TException e)
	{
		Logger.d(TAG, "Going to shutDown" + e.getReasonCode());
		if (Transporter.getInstance().getState() == State.CONNECTED || Transporter.getInstance().getState() == State.CONNECTING)
		{
			Transporter.getInstance().setState(State.DISCONNECTING);
			Logger.d(TAG,"current state is disconnecting..."+Transporter.getInstance().getState());
			unRegisterReceiver(context);
			DataManager.getInstance().releaseResources();
			Transporter.getInstance().stopServer();
			connectionCallback.onDisconnect(e);
			Transporter.getInstance().setState(State.DISCONNECTED);
			HandlerUtil.getInstance().removeCallbacksAndMessages(null);
			numberOfConnections.set(0);
			totalConnections=0;
		}
	}

	private void startSendingGhostPackets()
	{
		Message msg = Message.obtain();
		msg.what = TConstants.THandlerConstants.SEND_HEARTBEAT_PACKET;
		HandlerUtil.getInstance().sendMessage(msg);
	}

	private void registerReceiver(Context context)
	{
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		receiver = new ScreenToggleReceiver(config);
		context.registerReceiver(receiver, intentFilter);
	}

	private void unRegisterReceiver(Context context)
	{
		Logger.d(TAG,"Goining to unregister receiver");
		if (receiver != null)
		{
			try
			{

				context.unregisterReceiver(receiver);
				Logger.d(TAG, "receiver unregistered");
				receiver = null;
			}
			catch (IllegalArgumentException e)
			{
				Logger.d(TAG, "Oops caught a IllegalArgument Exception ...??");
			}
		}
	}

	@Override
	public void onFileRequest(long awb, int fileSizeBytes)
	{
		int fileSystemCheckCode = Utils.isFreeSpaceAvailable(fileSizeBytes);
		SenderConsignment consignment = Utils.createFileRequestReplyPacket(awb, fileSystemCheckCode, config.getAckTopic());
		if (consignment != null)
		{
			DataManager.getInstance().publishMsgToQueue(consignment);
		}
	}

	@Override
	public void onFileRequestReply(long awb, int code)
	{
		SenderConsignment consignment = DataManager.getInstance().getSenderConsigment(awb);
		if (consignment != null)
		{
			if (code == TConstants.SUCCESS)
			{
				// free space available

				consignment.setSpaceCheckedOnClient(true);
				consignment.persistance=false;
				DataManager.getInstance().publishMsgToQueue(consignment);
			}
			else
			{
				DataManager.getInstance().remove(awb);
				messageSentCallback.onError(consignment, ERRORCODES.getEnumValue(code));
			}
		}
	}

	@Override
	public void onConnectionFailure(TException e)
	{
		shutDown(e);
		connectionCallback.onDisconnect(e);
	}

	@Override
	public void onErrorOccuredConsigner(TException e, long awb)
	{
		if (e.getReasonCode() == TException.FILE_NOT_FOUND_EXCEPTION)
		{
			SenderConsignment consignment = DataManager.getInstance().getSenderConsigment(awb);
			if (consignment != null)
			{
				messageSentCallback.onError(consignment, ERRORCODES.FILE_NOT_EXISTS);
				DataManager.getInstance().remove(awb);
			}
		}
		else
		{
			shutDown(e);
		}
	}

	@Override
	public void onHandShakeFromServer(boolean status)
	{
		onConnectionSuccess();
	}

}
