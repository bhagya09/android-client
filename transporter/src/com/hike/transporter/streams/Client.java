package com.hike.transporter.streams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.hike.transporter.DefaultRetryPolicy;
import com.hike.transporter.TException;
import com.hike.transporter.interfaces.IConnectionConnect;
import com.hike.transporter.models.Config;
import com.hike.transporter.utils.Logger;

/**
 * 
 * @author himanshu/GauravK
 * 
 * 	This class behaves as client and try to connect to the server
 *
 */
public class Client implements Runnable
{

	private Config clientConfig;

	private Socket socket;

	private IConnectionConnect callback;

	private Thread myThread;

	private DefaultRetryPolicy defaultRetryPolicy = null;

	private boolean connected;

	public Client(Config config, IConnectionConnect callback)
	{
		this.clientConfig = config;
		this.callback = callback;
		defaultRetryPolicy = new DefaultRetryPolicy(config.getDefaultRetryPolicy());
	}

	public void startClient()
	{
		myThread = new Thread(this);
		myThread.start();
	}

	@Override
	public void run()
	{
		

		while (defaultRetryPolicy.getRetryCount() > 0)
		{
			try
			{
				socket = new Socket();
				socket.connect((new InetSocketAddress(clientConfig.getIp(), clientConfig.getPort())), clientConfig.getConnectTimeout());
				defaultRetryPolicy.setRetryDone();
				connected = true;
			}
			catch (SocketTimeoutException e)
			{
				e.printStackTrace();
				Logger.d("Transporter", "SocketTimeOut Exception in Client connect");
			}
			catch (IOException e)
			{
				if (defaultRetryPolicy.getRetryCount() > 0)
				{
					try
					{
						Thread.sleep(defaultRetryPolicy.getRetryDelay());
						defaultRetryPolicy.consumeRetry();
					}
					catch (InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
				else
				{
					defaultRetryPolicy.setRetryDone();
				}
				Logger.d("Transporter", "IO Exception in Client connect");
				e.printStackTrace();
			}
		}
		if (connected)
		{
			callback.onConnectionMade(socket);
		}
		else
		{
			callback.onConnectionFailure(new TException(TException.CLIENT_TIMEOUT));
		}
	}
}
