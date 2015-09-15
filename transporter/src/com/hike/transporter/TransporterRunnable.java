package com.hike.transporter;

import java.net.Socket;

import com.hike.transporter.utils.Logger;

public class TransporterRunnable implements Runnable
{

	Thread myThread;

	Socket socket;

	protected boolean hasShutDown;

	public TransporterRunnable(Socket socket)
	{
		this.socket = socket;
	}

	@Override
	public void run()
	{

	}

	public void start(String name)
	{
		myThread = new Thread(this, name);
		myThread.start();

	}

	public void stop()
	{
		if (myThread == null)
		{
			return;
		}
		hasShutDown = true;
		releaseResources();
		Logger.d("Transporter",myThread.getName());
		myThread.interrupt();
	}

	protected void releaseResources()
	{
	}

}
