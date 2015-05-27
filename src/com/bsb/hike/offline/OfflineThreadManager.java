package com.bsb.hike.offline;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;
import static com.bsb.hike.offline.OfflineConstants.PORT_FILE_TRANSFER;
import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;
import static com.bsb.hike.offline.OfflineConstants.SOCKET_TIMEOUT;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.Logger;

public class OfflineThreadManager
{

	private BlockingQueue<JSONObject> textMessageQueue = null;

	private BlockingQueue<FileTransferObject> fileTransferQueue = null;

	private static OfflineThreadManager _instance = new OfflineThreadManager();

	private final static String TAG = getInstance().getClass().getName();

	private TextTransferThread textThread = null;

	private Socket textReceiverSocket = null;

	private Socket fileReceiveSocket = null;

	private Socket textSendSocket = null;

	private Socket fileSendSocket = null;

	private ServerSocket textServerSocket = null;

	private ServerSocket fileServerSocket = null;

	private FileTransferThread fileTransferThread = null;

	private OfflineManager offlineManager = null;

	public static OfflineThreadManager getInstance()
	{
		return _instance;
	}

	private OfflineThreadManager()
	{
		textMessageQueue = new LinkedBlockingQueue<JSONObject>();
		fileTransferQueue = new LinkedBlockingQueue<FileTransferObject>();
		textThread = new TextTransferThread();
		fileTransferThread = new FileTransferThread();
		offlineManager=OfflineManager.getInstance();
	}
	
	public void startThread()
	{
		textThread.start();
		fileTransferThread.start();
	}
	
	
	class TextTransferThread extends Thread
	{
		
		JSONObject packet;
		boolean val;
		@Override
		public void run() {
			try
			{
				String host=null;
				if(!textSendSocket.isBound())
				{
					if(offlineManager.isHotspotCreated())
					{
						 host = OfflineUtils.getIPFromMac(null);
					}
					else
					{
						host = IP_SERVER;
					}
					textSendSocket.bind(null);
					textSendSocket.connect((new InetSocketAddress(host, PORT_TEXT_MESSAGE)), SOCKET_TIMEOUT);
				}
				
				while(((packet = textMessageQueue.take()) != null))
				{
						//TODO : Send Offline Text and take action on the basis of boolean  i.e. clock or single tick
						val = sendOfflineText(packet);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	class FileTransferThread extends Thread
	{
		FileTransferObject fileTranserObject;
		boolean val;
		@Override
		public void run() {
			try 
			{
				while(((fileTranserObject = fileTransferQueue.take()) != null))
				{
					//TODO : Send Offline Text and take action on the basis of boolean  i.e. clock or single tick
					val = transferFile(packet);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	class TextReceiveThread extends Thread
	{
		@Override
		public void run()
		{
			if (!textServerSocket.isBound())
			{
				try
				{
					textServerSocket = new ServerSocket(PORT_TEXT_MESSAGE);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			try
			{
				textReceiverSocket= textServerSocket.accept();
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(textReceiverSocket.getInputStream()));
				while (true)
				{

					while (in.readLine() != null)
					{
						//process the inputStream.
					}
				}
				
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	class FileReceiverThread extends Thread
	{
		@Override
		public void run()
		{
			if (!fileServerSocket.isBound())
			{
				try
				{
					fileServerSocket = new ServerSocket(PORT_FILE_TRANSFER);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				try
				{
					fileReceiveSocket = fileServerSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(fileReceiveSocket.getInputStream()));
					while (true)
					{
						while (in.readLine() != null)
						{
							// process the file input Stream
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
		}
	}
	
	/**
	 * 
	 * @param packet
	 * @param outputStream
	 * @return
	 * TODO: Properly handle sticker and normal message and update the Db
	 */  
	
	private boolean sendOfflineText(JSONObject packet,OutputStream outputStream)
	{
		String fileUri  =null;
		InputStream inputStream=null;
		if(OfflineUtils.isStickerMessage(packet))
		{
			try
			{
				fileUri = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(HikeConstants.STICKER_PATH);
			}
			catch (JSONException e1)
			{
				e1.printStackTrace();
				return false;
			}
			File f = new File(fileUri);
			Logger.d(TAG, "Middle " + f.getPath());
			try
			{
				inputStream = new FileInputStream(fileUri);
			}
			catch (FileNotFoundException e)
			{
				Logger.d(TAG, e.toString());
			}
			byte[] messageBytes = packet.toString().getBytes();
			int length = messageBytes.length;
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.write(intToBArray, 0, intToBArray.length);
			outputStream.write(messageBytes, 0, length);
			isSent = copyFile(inputStream, outputStream, msgId, false, true);
		}
		else
			{
				String messageJSON = packet.toString();
				byte[] messageBytes = packet.toString().getBytes();
				int length = messageBytes.length;
				byte[] intToBArray = OfflineUtils.intToByteArray(length);
				outputStream.write(intToBArray, 0, intToBArray.length);
				outputStream.write(messageBytes,0, length);
			}
		
		//Updating database
		if(!OfflineUtils.isGhostPacket(packet))
		{
			String msisdn =  packet.getString(HikeConstants.TO);
			int rowsUpdated = updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
			if (rowsUpdated == 0)
			{
				Logger.d(getClass().getSimpleName(), "No rows updated");
			}
			Pair<String, Long> pair = new Pair<String, Long>(msisdn,msgId);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);	
		}
			
	}
}
