package com.bsb.hike.offline.runnables;

import java.io.IOException;
import java.io.OutputStream;

import com.bsb.hike.offline.FileTransferModel;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.Logger;

public class FileSendRunnable implements Runnable
{

	private IConnectCallback connectCallback;
	private FileTransferModel fileTranserObject;
	private OfflineManager offlineManager;
	private OutputStream outputStream;
	private static final String TAG = "OfflineThreadManager";
	
	public FileSendRunnable(IConnectCallback connectCallback,OutputStream outputStream)
	{
		this.connectCallback=connectCallback;
		this.outputStream=outputStream;
	}
	@Override
	public void run()
	{
		try
		{
			offlineManager = OfflineManager.getInstance();
			Logger.d(TAG, "File Transfer Thread Connected");
			while (true)
			{
				fileTranserObject = OfflineManager.getInstance().getFileTransferQueue().take();

				offlineManager.setInOfflineFileTransferInProgress(true);

				OfflineThreadManager.getInstance().sendOfflineFile(fileTranserObject,outputStream);

				Logger.d(TAG, "Waiting for ack of msgid: " + OfflineUtils.getMsgId(fileTranserObject.getPacket()));

				offlineManager.setInOfflineFileTransferInProgress(false);
			}
		}
		catch (InterruptedException e)
		{
			Logger.e(TAG, "Some called interrupt on File transfer Thread");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "IO Exception occured in FileTransferThread.Socket was not bounded or connect failed");
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.CLIENT_DISCONNETED));
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "FileTransferThread. Did we pass correct Address here ? ?");
		}
		catch (OfflineException e)
		{
			connectCallback.onDisconnect(e);
			e.printStackTrace();
	}

}
}
