package com.hike.transporter.streams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import com.hike.transporter.Consignee;
import com.hike.transporter.TException;
import com.hike.transporter.interfaces.IConsigneeListener;
import com.hike.transporter.models.Config;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.Utils;


/**
 * 
 * @author himanshu/GauravK
 * 
 * This class behaves as a server and accepts incoming connections
 *
 */
public class Server implements Runnable
{

	Config serverConfig;

	ServerSocket serverSocket = null;

	private static final String TAG = "Server";

	Socket socket;

	Thread myThread;

	IConsigneeListener consigneeListener;

	public Server(Config config, IConsigneeListener listener)
	{
		this.serverConfig = config;
		this.consigneeListener = listener;
	}

	public void startServer()
	{
		myThread = new Thread(this,"serverThread");
		myThread.start();
	}

	@Override
	public void run()
	{
		try
		{
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			SocketAddress addr = new InetSocketAddress(serverConfig.getPort());
			serverSocket.bind(addr);

			while (true)
			{
				Logger.d(TAG, "Going to wait for Client socket");
				socket = serverSocket.accept();
				// start receiving runnable;
				Consignee runnConsignee = new Consignee(socket, consigneeListener);
				runnConsignee.start("ConsigneeThread");
			}
		}
		catch (IOException | TException e)
		{
			Logger.d("Transporter", "IOException or TException in Server connect");
			e.printStackTrace();
			consigneeListener.onErrorOccuredConsignee(new TException(TException.SERVER_EXCEPTION));
		}

	}

	public void stop()
	{
		if (myThread != null)
		{
			Logger.d(TAG,myThread.getName());
			myThread.interrupt();
		}
		releaseResources();
	}

	private void releaseResources()
	{
		Logger.d("Transporter", "Goining to release resources fir server");
		try
		{
			Utils.closeSocket(serverSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
