package com.hike.transporter;

import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.hike.transporter.interfaces.IConnectionListener;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.interfaces.IPersistanceInterface;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.DataManager;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.streams.Client;
import com.hike.transporter.streams.Server;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.TConstants.ERRORCODES;
import com.hike.transporter.utils.TConstants.State;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu/Gaurav
 * 
 * This class is the entryPoint for everyone that is going to use this library.It exposes methods to start the lib
 * as a client or as a server.It allows to publish message when connected 
 *
 */
public class Transporter
{
	private static final String TAG = "Transporter";

	private Context context;

	private static volatile Transporter transporter;

	private Config config;

	private volatile State state = State.CLOSED;

	private Server server;

	private CallbackHandler callbackHandler;

	private IMessageSent messageSentCallback = null;
	
	private IPersistanceInterface persistanceInterface;
	
	private volatile long lastActivityTime = 0;

	public static Transporter getInstance()
	{
		if (transporter == null)
		{
			synchronized (Transporter.class)
			{
				if (transporter == null)

					transporter = new Transporter();
			}
		}
		return transporter;
	}

	public void initAsClient(Config config, Context context, IMessageSent messageSentCallbak, IMessageReceived messageReceivedCallback, IConnectionListener connectionListener,
			Looper looper,IPersistanceInterface persistanceInterface)
	{
		this.config = config;
		this.context = context;
		this.messageSentCallback = messageSentCallbak;
		this.persistanceInterface=persistanceInterface;
		DBManager.getInstance();
		HandlerUtil.getInstance(looper, config);
		Utils.initScreenStatus(context);
		callbackHandler = new CallbackHandler(connectionListener, messageSentCallbak, messageReceivedCallback, config, context);
		setState(State.CONNECTING);
		for (int i = 0; i < config.getTopics().size(); i++)
		{
			Client client = new Client(config, callbackHandler);
			client.startClient();
		}
	}

	/**
	* Call this function if you want Transporter to run as server on this mobile, you need to pass config which ensures port number to listen on. 
	*/
	
	public void initAsServer(Config config, Context context, IMessageSent messageSentCallbak, IMessageReceived messageReceivedCallback, IConnectionListener connectionListener,
			Looper looper,IPersistanceInterface persistanceInterface)
	{
		this.config = config;
		this.context = context;
		this.messageSentCallback=messageSentCallbak;
		this.persistanceInterface=persistanceInterface;
		callbackHandler = new CallbackHandler(connectionListener, messageSentCallbak, messageReceivedCallback, config, context);
		DBManager.getInstance();
		HandlerUtil.getInstance(looper, config);
		Utils.initScreenStatus(context);
		setState(State.CONNECTING);
		server = new Server(config, callbackHandler);
		server.startServer();
	}

	public void stopServer()
	{
		if (server != null)
		{
			server.stop();
		}
	}

	public void publish(SenderConsignment senderConsignment)
	{

		// Insert into The DataBase

		if (state == State.CONNECTED)
		{
			if (TextUtils.isEmpty(senderConsignment.type))
			{
				senderConsignment.type = senderConsignment.file == null ? TConstants.TEXT : TConstants.FILE;
			}
			// if it is file message, we need to ask other party whether this much space is available or not
			if (senderConsignment.type == TConstants.FILE && !senderConsignment.isSpaceCheckedOnClient())
			{
				// ask for space
				DataManager.getInstance().saveMsg(senderConsignment);
				DataManager.getInstance().putInMessageMap(senderConsignment);
				DataManager.getInstance().publishMsgToQueue(Utils.createFileRequestPacket(senderConsignment.getAwb(), senderConsignment.getTotalFileSize(), config.getAckTopic()));
			}
			else
			{
				DataManager.getInstance().publishMsgToQueue(senderConsignment);
			}
		}
		else
		{
			if (shouldSentMessageToCaller())
				messageSentCallback.onError(senderConsignment, ERRORCODES.NOT_CONNECTED);
		}
	}

	private boolean shouldSentMessageToCaller()
	{
		if (messageSentCallback != null)
		{
			return true;
		}
		return false;
	}

	/**
	 * @return the state
	 */
	public State getState()
	{
		return state;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(State state)
	{
		this.state = state;
	}

	public Context getApplicationContext()
	{
		return context;
	}

	public void shutDown()
	{
		if (callbackHandler != null)
		{
			callbackHandler.onErrorOccuredConsignee(new TException(TException.USER_DISCONNECTED));
		}
	}

	void shutDown(int errorCode)
	{
		if (callbackHandler != null)
		{
			callbackHandler.shutDown(new TException((byte) errorCode));
		}
	}

	public Config getConfig()
	{
		return config;
	}

	public void publishWhenConnected(SenderConsignment senderConsignment)
	{
		senderConsignment.setAwb(senderConsignment.hashCode());
		Message msg = Message.obtain();
		msg.what = TConstants.THandlerConstants.SAVE_MSG_DB;
		msg.obj = senderConsignment;
		HandlerUtil.getInstance().sendMessage(msg);	
	}

	public void deleteFromPersistance(String nameSpace)
	{
		if (TextUtils.isEmpty(nameSpace))
		{
			throw new IllegalArgumentException("Name space cannot be null or empty !!");
		}
		DBManager.getInstance().deleteFromPersistance(nameSpace);
	}
	
	public IPersistanceInterface getPersistance()
	{
		return persistanceInterface;
	}
	
	public void deleteTempFiles(Context context)
	{
		Utils.deleteTempFiles(context);
	}
	
	void setLastActivityTime(long val)
	{
		this.lastActivityTime = val;
		Logger.d(TAG,"Last Activity Time"+lastActivityTime);
	}

	long getLastActivitytime()
	{
		return lastActivityTime;
	}
	
}
