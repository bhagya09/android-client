package com.hike.transporter.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.interfaces.IConsigneeListener;
import com.hike.transporter.models.ReceiverConsignment;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;

/**
 * 
 * @author himanshu/GauravK
 *	A wrapper over InputStream to handle all the incoming connections
 */
public class TransporterInputStream
{

	private static final String TAG = "InputStream";

	private InputStream inputStream;

	public TransporterInputStream(InputStream in)
	{
		this.inputStream = in;
	}

	public JSONObject readPackage() throws IOException, JSONException
	{
		byte[] convMessageLength = new byte[4];
		int readBytes = inputStream.read(convMessageLength, 0, 4);

		int msgSize = byteArrayToInt(convMessageLength);

		Logger.d(TAG, "Read Bytes is " + readBytes + "and msg Size is  " + msgSize);
		// Loggerger.d(TAG,"Msg size is "+msgSize);
		if (msgSize <= 0)
		{
			throw new IOException();
		}
		byte[] msgJSON = new byte[msgSize];
		int offset = 0;
		while (msgSize > 0)
		{
			int len = inputStream.read(msgJSON, offset, msgSize);
			if (len < 0)
				throw new IOException();
			offset += len;
			msgSize -= len;
		}
		String msgString = new String(msgJSON, "UTF-8");
		JSONObject obj = new JSONObject(msgString);
		return obj;
	}

	public  void readFile(OutputStream out, IConsigneeListener listener, ReceiverConsignment receiverConsignment, long fileSize) throws TException, IOException
	{
		byte buf[] = new byte[TConstants.CHUNK_SIZE];
		int len = 0;
		int readLen = 0;
		boolean shouldinformUseraboutRemaingFileChunk = !(fileSize % TConstants.CHUNK_SIZE == 0);
		try
		{

			long prev = 0;
			while (fileSize >= TConstants.CHUNK_SIZE)
			{
				
				readLen = inputStream.read(buf, 0, TConstants.CHUNK_SIZE);
				if (readLen < 0)
					throw new TException(TException.EXCEPTION_READ_FILE);

				out.write(buf, 0, readLen);
				len += readLen;
				fileSize -= readLen;
				if (listener!=null && ((len / TConstants.CHUNK_SIZE) != prev))
				{
					prev = len / TConstants.CHUNK_SIZE;
					Logger.d(TAG, "Chunk read " + prev + "");
					receiverConsignment.setReadFileSize(len);
					listener.onChunkRead(receiverConsignment);
					// showSpinnerProgress(fileTransferModel);
				}
			}
			
			while (fileSize > 0)
			{
				buf = new byte[(int) fileSize];
				readLen = inputStream.read(buf);
				if (readLen < 0)
				{
					throw new TException(TException.EXCEPTION_READ_FILE);
				}
				fileSize -= readLen;
				len += readLen;
				out.write(buf, 0, readLen);
				
			}
			if (shouldinformUseraboutRemaingFileChunk&&listener!=null)
			{
				Logger.d(TAG,"informing user about remaing chunk " + prev + "");
				receiverConsignment.setReadFileSize(len);
				listener.onChunkRead(receiverConsignment);
			}
			out.close();
		}
		catch (IOException e)
		{
			Logger.e(TAG, "Exception in copyFile: ", e);
			out.close();
			throw new TException(e, TException.EXCEPTION_READ_FILE);
		}
	}

	private int byteArrayToInt(byte[] bytes)
	{
		return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}

	public void close() throws IOException
	{
		inputStream.close();
	}
}
