package com.hike.transporter.streams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.hike.transporter.TException;
import com.hike.transporter.interfaces.IConsignerListener;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;

/**
 * 
 * @author himanshu/GauravK
 * 
 * This class handles all the outgoing communication
 *
 */
public class TransporterOutputStream
{

	private static final String TAG = "OutputStream";

	OutputStream outputStream;

	public TransporterOutputStream(OutputStream stream)
	{
		outputStream = stream;
	}

	public void writeString(String packet) throws IOException, TException
	{
		byte[] metaDataBytes = packet.getBytes("UTF-8");
		int length = metaDataBytes.length;
		Logger.d(TAG, "Sizeof metaString: " + length);
		byte[] intToBArray = intToByteArray(length);
		outputStream.write(intToBArray, 0, intToBArray.length);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(metaDataBytes);
		writeFile(byteArrayInputStream, metaDataBytes.length);
		byteArrayInputStream.close();
	}

	private void writeFile(ByteArrayInputStream byteArrayInputStream,int length) throws TException, IOException
	{
		writeFile(byteArrayInputStream, null, length,-1);

	}

	public void writeFile(InputStream inputStream, IConsignerListener listener, long fileSize,long awb) throws TException, IOException
	{
		byte buf[] = new byte[TConstants.CHUNK_SIZE];
		int len = 0,readLen=0;
		boolean shouldinformUseraboutRemaingFileChunk = !(fileSize % TConstants.CHUNK_SIZE == 0);
		try
		{

			long prev = 0;
			while (fileSize >= TConstants.CHUNK_SIZE)
			{
				
				readLen = inputStream.read(buf, 0, TConstants.CHUNK_SIZE);
				if (readLen < 0)
					throw new TException(TException.EXCEPTION_READ_FILE);

				outputStream.write(buf, 0, readLen);
				len += readLen;
				fileSize -= readLen;
				if (listener!=null && ((len / TConstants.CHUNK_SIZE) != prev))
				{
					prev = len / TConstants.CHUNK_SIZE;
					Logger.d(TAG, "Chunk read " + prev + "");
					// showSpinnerProgress(fileTransferModel);
					listener.onChunkSend(awb, len);
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
				outputStream.write(buf, 0, readLen);
			}
			if (shouldinformUseraboutRemaingFileChunk&&listener!=null)
			{
				Logger.d(TAG,"informing user about remaing chunk " + prev + "");
				listener.onChunkSend(awb, len);
			}
		}
		catch (IOException e)
		{
			Logger.e(TAG, "Exception in copyFile: ", e);
			throw new TException(e, TException.EXCEPTION_READ_FILE);
		}
	}

	public byte[] intToByteArray(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /* >> 0 */);
		return result;
	}

	public void close() throws IOException
	{
		outputStream.close();
	}

}
