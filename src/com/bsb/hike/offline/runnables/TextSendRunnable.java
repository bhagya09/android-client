package com.bsb.hike.offline.runnables;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.json.JSONObject;

import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.Logger;

public class TextSendRunnable implements Runnable
{
	JSONObject packet = null;

	private static final String TAG = "OfflineThreadManager";

	private OutputStream outputStream;

	private IConnectCallback connectCallback = null;;

	public TextSendRunnable(OutputStream outputStream, IConnectCallback connectCallback)
	{
		this.outputStream = outputStream;
		this.connectCallback = connectCallback;
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				packet = OfflineManager.getInstance().getTextQueue().take();
				{
					Logger.d("OfflineThreadManager", "Going to send Text");
					OfflineThreadManager.getInstance().sendOfflineText(packet, outputStream);
					Logger.d(TAG, "Waiting for ack of msgid: " + OfflineUtils.getMsgId(packet));
				}
			}
		}
		catch (InterruptedException e)
		{
			Logger.e(TAG, "Some called interrupt on Text transfer Thread");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Logger.e(TAG, "TextTransferThread. IO Exception occured.Socket was not bounded");
			connectCallback.onDisconnect(new OfflineException(e, OfflineException.SERVER_DISCONNED));
		}
		catch (OfflineException e)
		{
			connectCallback.onDisconnect(e);
			e.printStackTrace();
		}
	}
}