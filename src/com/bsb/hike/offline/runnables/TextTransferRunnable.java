package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;
import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;
import static com.bsb.hike.offline.OfflineConstants.SOCKET_TIMEOUT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.json.JSONObject;

import com.bsb.hike.offline.IMessageSentOffline;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;

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

	public TextTransferRunnable(IMessageSentOffline textMessageCallback)
	{
		this.callback = textMessageCallback;
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
				//textSendSocket.bind(null);

				textSendSocket.connect((new InetSocketAddress(host, PORT_TEXT_MESSAGE)), SOCKET_TIMEOUT);
				Logger.d(TAG, "Text Transfer Thread Connected");
				isNotConnected = false;

			}
			catch (IOException e)
			{
				Logger.d(TAG, "TIO Exception in connect " + offlineManager.getOfflineState() + " "+offlineManager.getConnectedDevice());
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
				//
				}
			}
		}
		try
		{
			while (true)
			{
				packet = OfflineManager.getInstance().getTextQueue().take();
				{
					// TODO : Send Offline Text and take action on the basis of boolean i.e. clock or single tick
					Logger.d("OfflineThreadManager", "Going to send Text");

					if (OfflineThreadManager.getInstance().sendOfflineText(packet, textSendSocket.getOutputStream()))
					{
						callback.onSuccess(packet);
					}
					else
					{
						callback.onFailure(packet);
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			Logger.e(TAG, "Some called interrupt on File transfer Thread");
			offlineManager.setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
			e.printStackTrace();
		}
		catch (SocketTimeoutException e)
		{
			Logger.e(TAG, "SOCKET time out exception occured in TextTransferThread.");
			// offlineManager.shutDown();
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "TextTransferThread. IO Exception occured.Socket was not bounded");
			offlineManager.shutDown();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "TextTransferThread. Did we pass correct Address here ? ?");
			// offlineManager.shutDown();
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
