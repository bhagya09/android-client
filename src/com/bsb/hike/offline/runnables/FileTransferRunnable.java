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
		try
		{
			Logger.d(TAG, "File Transfer Thread Connected");
			while (true)
			{
				fileTranserObject = OfflineManager.getInstance().getFileTransferQueue().take();
				// TODO : Send Offline Text and take action on the basis of boolean i.e. clock or single tick
				offlineManager.setInOfflineFileTransferInProgress(true);
				if (OfflineThreadManager.getInstance().sendOfflineFile(fileTranserObject, fileSendSocket.getOutputStream()))
				{
					callback.onSuccess(fileTranserObject.getPacket());
				}
				else
				{
					callback.onFailure(fileTranserObject.getPacket());
				}
				offlineManager.setInOfflineFileTransferInProgress(false);
			}
		}
		catch (InterruptedException e)
		{
			Logger.e(TAG, "Some called interrupt on File transfer Thread");
			//offlineManager.setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "IO Exception occured in FileTransferThread.Socket was not bounded or connect failed");
			offlineManager.shutDown(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			// offlineManager.shutDown();
			Logger.e(TAG, "FileTransferThread. Did we pass correct Address here ? ?");
		}
		catch (OfflineException e)
		{
			offlineManager.shutDown(new OfflineException(e,OfflineException.CLIENT_DISCONNETED));
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
