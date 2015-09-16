package com.hike.transporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import com.hike.transporter.interfaces.IConsignerListener;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.streams.TransporterOutputStream;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu/Gaurav
 * 
 * This class is responsible for all the data that are received over the netwrok
 *
 */
public class Consigner extends TransporterRunnable
{

	private TransporterOutputStream transporterOutputStream;

	private IConsignerListener listener;

	private BlockingQueue<SenderConsignment> queue;

	private FileInputStream inputStream = null;

	public Consigner(Socket socket, IConsignerListener listener, BlockingQueue<SenderConsignment> queue) throws TException
	{
		super(socket);
		this.listener = listener;
		this.queue = queue;
		try
		{
			transporterOutputStream = new TransporterOutputStream(socket.getOutputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new TException(e, TException.IO_EXCEPTION);
		}
	}

	@Override
	public void run()
	{
		if (myThread == null)
		{
			myThread = Thread.currentThread();
		}
		try
		{
			while (true)
			{

				if (myThread.isInterrupted() || hasShutDown)
				{
					listener.onErrorOccuredConsigner(new TException(TException.INTERRUPTED_EXCEPTION), -1);
					break;
				}
				SenderConsignment consignment = queue.take();
				Logger.d("Transpoter", "Goining to send text" + consignment.serialize());
				listener.onTransitBegin(consignment.getAwb());
				if (TConstants.FILE.equals(consignment.type))
				{
					// file message
					// 1 write meta data
					// 2 write file data
					if (!consignment.getFile().exists())
					{
						listener.onErrorOccuredConsigner(new TException(TException.FILE_NOT_FOUND_EXCEPTION), consignment.getAwb());
						continue;
					}
					transporterOutputStream.writeString(consignment.serialize());
					inputStream = new FileInputStream(consignment.getFile());
					transporterOutputStream.writeFile(inputStream, listener, (int) consignment.getFile().length(), consignment.getAwb());
					Utils.closeStream(inputStream);
					inputStream = null;
				}
				else
				{
					// text messsage
					transporterOutputStream.writeString(consignment.serialize());
				}
				listener.onTransitEnd(consignment.getAwb());

			}
		}
		catch (IOException e)
		{
			Logger.d("Transporter", "IOException occured  on Consigner Thread");
			Utils.closeStream(inputStream);
			e.printStackTrace();
			listener.onErrorOccuredConsigner(new TException(e, TException.IO_EXCEPTION), -1);
		}
		catch (TException e)
		{
			Logger.d("Transporter", "IOException occured  on Consigner Thread");
			Utils.closeStream(inputStream);
			listener.onErrorOccuredConsigner(e, -1);
		}
		catch (InterruptedException e)
		{
			Logger.d("Transporter", "SomeOne called Interrupt on Consigner Thread");
			e.printStackTrace();
			listener.onErrorOccuredConsigner(new TException(e, TException.INTERRUPTED_EXCEPTION), -1);
		}
	}

	@Override
	public void releaseResources()
	{
		Logger.d("transporter", "IOException on Consigner Thread");
		try
		{
			Utils.closeSocket(socket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
