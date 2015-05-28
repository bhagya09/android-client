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

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private static OfflineThreadManager _instance = new OfflineThreadManager();

	private final static String TAG = getInstance().getClass().getName();

	private TextTransferThread textTransferThread = null;

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
		textMessageQueue = OfflineManager.getInstance().getTextQueue();
		fileTransferQueue = OfflineManager.getInstance().getFileTransferQueue();
		textTransferThread = new TextTransferThread();
		fileTransferThread = new FileTransferThread();
		offlineManager = OfflineManager.getInstance();
	}
	
	public void startThread()
	{
		textTransferThread.start();
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
						val = sendOfflineText(packet,textSendSocket.getOutputStream());
				}
			} 
			catch (InterruptedException e) {
				Logger.e(TAG,"Some called interrupt on File transfer Thread");
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Logger.e(TAG, "IO Exception occured.Socket was not bounded");
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
				Logger.e(TAG,"Did we pass correct Address here ? ?");
			}
		}
	}
	
	
	class FileTransferThread extends Thread
	{
		FileTransferModel fileTranserObject;
		boolean val;
		String host=null;
		@Override
		public void run() {
			try 
			{
				if(!fileSendSocket.isBound())
				{
					if(offlineManager.isHotspotCreated())
					{
						 host = OfflineUtils.getIPFromMac(null);
					}
					else
					{
						host = IP_SERVER;
					}
					fileSendSocket.bind(null);
					fileSendSocket.connect(new InetSocketAddress(host, PORT_FILE_TRANSFER));
				}
				
				while(((fileTranserObject = fileTransferQueue.take()) != null))
				{
					//TODO : Send Offline Text and take action on the basis of boolean  i.e. clock or single tick
					val = transferFile(packet);
				}
			} catch (InterruptedException e) {
				Logger.e(TAG,"Some called interrupt on File transfer Thread");
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Logger.e(TAG, "IO Exception occured.Socket was not bounded");
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
				Logger.e(TAG,"Did we pass correct Address here ? ?");
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
					textReceiverSocket = textServerSocket.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(textReceiverSocket.getInputStream()));
					while (true)
					{

						while (in.readLine() != null)
						{
							// process the inputStream.
						}
					}

				}
				catch (IOException e)
				{
					e.printStackTrace();
					Logger.e(TAG, "IO Exception occured.Socket was not bounded");
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
					Logger.e(TAG, "Did we pass correct Address here ? ?");
				}
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
				catch(IOException e)
				{
					e.printStackTrace();
					Logger.e(TAG, "IO Exception occured.Socket was not bounded");
				}
				catch(IllegalArgumentException e)
				{
					e.printStackTrace();
					Logger.e(TAG,"Did we pass correct Address here ? ?");
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
		boolean isSent=false;
		if(OfflineUtils.isStickerMessage(packet))
		{
			try
			{
				fileUri = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(OfflineConstants.STICKER_PATH);
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
				return false;
			}
			byte[] messageBytes = packet.toString().getBytes();
			int length = messageBytes.length;
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			try
			{
				outputStream.write(intToBArray, 0, intToBArray.length);
				outputStream.write(messageBytes, 0, length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return false;
			}
			
			isSent = offlineManager.copyFile(inputStream, outputStream);
		}
		else
			{
				String messageJSON = packet.toString();
				byte[] messageBytes = packet.toString().getBytes();
				int length = messageBytes.length;
				byte[] intToBArray = OfflineUtils.intToByteArray(length);
				try
				{
					outputStream.write(intToBArray, 0, intToBArray.length);
					outputStream.write(messageBytes,0, length);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return false;
				}
				
			}
		
		// Updating database
		if (!OfflineUtils.isGhostPacket(packet))
		{
			long msgId;
			try
			{
				msgId = packet.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);

				String msisdn = packet.getString(HikeConstants.TO);
				int rowsUpdated = OfflineUtils.updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
				if (rowsUpdated == 0)
				{
					Logger.d(getClass().getSimpleName(), "No rows updated");
				}
				Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return isSent;
	}
	
	
	
}
