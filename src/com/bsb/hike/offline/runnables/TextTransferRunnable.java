package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;
import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;
import static com.bsb.hike.offline.OfflineConstants.SOCKET_TIMEOUT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.json.JSONObject;

import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.IMessageSentOffline;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu
 *	Runnable responsible for receving text from client
 */
public class TextTransferRunnable implements Runnable
{

	private int currentTries = 0;

	JSONObject packet;

	boolean val;

	private IMessageSentOffline callback;

	private static final String TAG = "OfflineThreadManager";

	OfflineManager offlineManager = null;

	private boolean isNotConnected =true;
	
	private Socket textSendSocket = null;
	
	IConnectCallback connectCallback=null;
	
	IMessageSentOffline fileCallback;

	public TextTransferRunnable(IMessageSentOffline textMessageCallback,IMessageSentOffline fileCallback,IConnectCallback connectCallback)
	{
		this.callback = textMessageCallback;
		this.connectCallback=connectCallback;
		this.fileCallback=fileCallback;
	}

	@Override
	public void run()
	{
		offlineManager = OfflineManager.getInstance();
		String host = null;
		Logger.d(TAG, "Text Transfer Thread -->" + "Going to connect to socket");
		isNotConnected=true;
		currentTries=0;
		while (isNotConnected)
		{
			try
			{

				if (offlineManager.isHotspotCreated())
				{
					host = OfflineUtils.getIPFromMac(null);
				}
				else
				{
					host = IP_SERVER;
				}

				textSendSocket = new Socket();
				textSendSocket.connect((new InetSocketAddress(host, PORT_TEXT_MESSAGE)), SOCKET_TIMEOUT);
				Logger.d(
						TAG,
						"Text Transfer Thread Connected" + "the receivre buffer size is " + textSendSocket.getReceiveBufferSize() + "send buffer is "
								+ textSendSocket.getSendBufferSize());
				isNotConnected = false;
				connectCallback.onConnect();

			}
			catch (IOException e)
			{
				Logger.d(TAG, "TIO Exception in connect " + offlineManager.getOfflineState() + " " + offlineManager.getConnectedDevice());
				if (++currentTries < OfflineConstants.MAX_TRIES)
				{
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
				else
				{
					connectCallback.onDisconnect(new OfflineException(OfflineException.CLIENT_COULD_NOT_CONNECT));
					return;
				}
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
				Logger.e(TAG, "TextTransferThread. Did we pass correct Address here ? ?");
			}
		}
		TextSendRunnable runnable;
		try
		{
			OfflineThreadManager.getInstance().startTextReceivingThread(new TextReceiveRunnable(textSendSocket.getInputStream(), callback, fileCallback, connectCallback));
			runnable = new TextSendRunnable(textSendSocket.getOutputStream(), connectCallback);
			runnable.run();
			}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

	public void shutDown()
	{
		try
		{
			OfflineUtils.closeSocket(textSendSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
