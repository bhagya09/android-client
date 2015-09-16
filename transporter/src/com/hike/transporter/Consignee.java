package com.hike.transporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import com.hike.transporter.interfaces.IConsigneeListener;
import com.hike.transporter.models.ReceiverConsignment;
import com.hike.transporter.streams.TransporterInputStream;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.Utils;

/**
 * 
 * @author himanshu/Gaurav
 * 
 *         This class is responsible for sending the data over the network.
 * 
 */
public class Consignee extends TransporterRunnable
{
	private TransporterInputStream transporterInputStream;

	private IConsigneeListener listener;

	private String TAG = "Consignee";

	File tempFile = null;
	
	FileOutputStream outputStream = null;

	public Consignee(Socket socket, IConsigneeListener listener) throws TException
	{
		super(socket);
		this.listener = listener;
		try
		{
			transporterInputStream = new TransporterInputStream(socket.getInputStream());
		}
		catch (IOException e)
		{
			throw new TException(e, TException.IO_EXCEPTION);
		}
	}

	@Override
	public void run()
	{
		if(myThread==null)
		{
			myThread=Thread.currentThread();
		}
		try
		{
			while (true)
			{

				if (myThread.isInterrupted() || hasShutDown)
				{
					listener.onErrorOccuredConsignee(new TException(TException.INTERRUPTED_EXCEPTION));
					break;
				}
				JSONObject json = transporterInputStream.readPackage();
				Logger.d(TAG, "Message Received" + json.toString());
				if (Utils.isAckPacket(json))
				{
					listener.onAckReceived(Utils.getAwbNumberFromAckPkt(json));
				}
				else if (Utils.isHeartBeatPkt(json))
				{
					listener.onHeartBeat(Utils.getScreenStatusFromHeartBeatPkt(json));
				}
				else if (Utils.isHandShake(json))
				{
					listener.onHandShake(this, json, socket);
				}
				else if (Utils.isTextPkt(json))
				{
					listener.onApplicationData(getReceiverConsignment(json, false));

				}
				else if (Utils.isFileRequest(json))
				{
					// ask file system and send some
					listener.onFileRequest(Utils.getAwbNumberFromAckPkt(json), Utils.getFileSizeFromFileRequestPkt(json));
				}
				else if (Utils.isFileRequestReply(json))
				{
					// ask file system and send some
					listener.onFileRequestReply(Utils.getAwbNumberFromAckPkt(json), Utils.getCode(json));
				}
				else if(Utils.isHandShakePktFromServer(json))
				{
					listener.onHandShakeFromServer(true);
				}
				else if (Utils.isFilePkt(json))
				{
					ReceiverConsignment receiverConsignment = getReceiverConsignment(json, true);
					listener.onApplicationData(receiverConsignment);
					tempFile = Utils.createTempFile(Transporter.getInstance().getApplicationContext());
					outputStream = new FileOutputStream(tempFile);
					transporterInputStream.readFile(outputStream, listener, receiverConsignment, Utils.getFileSizeFromPacket(json));
					// read file here
					Utils.closeStream(outputStream);
					receiverConsignment.file = tempFile;
					listener.onFileCompleted(receiverConsignment);
					tempFile=null;
				}
			}
		}
		catch (IOException e)
		{
			if (tempFile != null && tempFile.exists())
			{
				Logger.d(TAG,"deleting temp file");
				tempFile.delete();
				Utils.closeStream(outputStream);
			}
			Logger.d(TAG, "IOException on Conginee Thread");
			e.printStackTrace();
			listener.onErrorOccuredConsignee(new TException(e, TException.IO_EXCEPTION));
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "JSONException on Conginee Thread");
			e.printStackTrace();
			listener.onErrorOccuredConsignee(new TException(e, TException.JSON_EXCEPTION));
		}
		catch (TException e)
		{
			if (tempFile != null && tempFile.exists())
			{
				Logger.d(TAG, "deleting temp file");
				tempFile.delete();
				Utils.closeStream(outputStream);
			}
			Logger.d(TAG, "TException on Conginee Thread");
			e.printStackTrace();
			listener.onErrorOccuredConsignee(e);
		}
	}

	private ReceiverConsignment getReceiverConsignment(JSONObject json, boolean isFile) throws JSONException
	{
		return new ReceiverConsignment.Builder(json.getString(TConstants.DATA)).totalFileSize(isFile ? Utils.getFileSizeFromPacket(json) : 0)
				.type(isFile ? TConstants.FILE : TConstants.TEXT).awb(Utils.getAwbNumberFromNormalPkt(json)).build();
	}

	@Override
	public void releaseResources()
	{
		Logger.d(TAG, "Goinig to release resources for Conignee");
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
