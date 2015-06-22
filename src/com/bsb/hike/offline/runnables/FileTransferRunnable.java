package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;
import static com.bsb.hike.offline.OfflineConstants.PORT_FILE_TRANSFER;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.bsb.hike.offline.FileTransferModel;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.IMessageSentOffline;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu
 *	Runnable responsible for sending file to server
 */
public class FileTransferRunnable implements Runnable
{

	private static final String TAG = "OfflineThreadManager";

	FileTransferModel fileTranserObject;

	boolean val;

	boolean isNotConnected = true;

	String host = null;

	int tries;

	IMessageSentOffline callback = null;

	private Socket fileSendSocket = null;

	private OfflineManager offlineManager = null;
	
	IConnectCallback connectCallback=null;

	public FileTransferRunnable(IMessageSentOffline fileMessageCallback,IConnectCallback connectCallback)
	{
		this.callback = fileMessageCallback;
		this.connectCallback=connectCallback;
	}

	@Override
	public void run()
	{
		isNotConnected=true;
		tries=0;
		offlineManager = OfflineManager.getInstance();
		Logger.d(TAG, "File Transfer Thread -->" + "Going to connect to socket");
		while (isNotConnected)
		{
			try
			{

				fileSendSocket = new Socket();
				if (offlineManager.isHotspotCreated())
				{
					host = OfflineUtils.getIPFromMac(null);
				}
				else
				{
					host = IP_SERVER;
				}
				Logger.d(TAG, "host is " + host);
				fileSendSocket.bind(null);
				fileSendSocket.connect(new InetSocketAddress(host, PORT_FILE_TRANSFER));
				isNotConnected = false;
				connectCallback.onConnect();
			}
			catch (IOException e)
			{
				if (++tries < OfflineConstants.MAX_TRIES)
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
		}
		FileSendRunnable runnable;
		try
		{
			OfflineThreadManager.getInstance().startFileReceivingThread(new FileReceiveRunnable(fileSendSocket.getInputStream(), connectCallback));
			runnable = new FileSendRunnable(connectCallback, fileSendSocket.getOutputStream());
			runnable.run();
			
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public void shutDown()
	{
		try
		{
			OfflineUtils.closeSocket(fileSendSocket);
		}
		catch (IOException ex)
		{

		}
	}

}
