package com.bsb.hike.offline;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;
import static com.bsb.hike.offline.OfflineConstants.PORT_FILE_TRANSFER;
import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;
import static com.bsb.hike.offline.OfflineConstants.SOCKET_TIMEOUT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

/**
 * 
 * @author himanshu, deepak malik
 *	This class mainly deals with socket connection,text and file are send and received in this class only.
 */
public class OfflineThreadManager
{

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
	
	private TextReceiveThread textReceiveThread=null;
	
	private FileReceiverThread fileReceiverThread=null;

	public static OfflineThreadManager getInstance()
	{
		return _instance;
	}

	private OfflineThreadManager()
	{
		textTransferThread = new TextTransferThread(textMessageCallback);
		fileTransferThread = new FileTransferThread();
		textReceiveThread=new TextReceiveThread();
		fileReceiverThread=new FileReceiverThread();
		
	}
	
	public void startSendingThreads()
	{
		offlineManager=OfflineManager.getInstance();
		if(textTransferThread.isAlive())
		textTransferThread.interrupt();
		{
			textTransferThread=new TextTransferThread(textMessageCallback);
			textTransferThread.start();
		}
		
		if(fileTransferThread.isAlive())
		fileTransferThread.interrupt();
		{
			fileTransferThread=new FileTransferThread();
			fileTransferThread.start();
		}
	}
	
	public void startReceivingThreads()
	{
		offlineManager=OfflineManager.getInstance();
		if(textReceiveThread.isAlive())
		textReceiveThread.interrupt();
		{
			textReceiveThread=new TextReceiveThread();
			textReceiveThread.start();
		}
		if(fileReceiverThread.isAlive())
		fileReceiverThread.interrupt();
		{
			fileReceiverThread=new FileReceiverThread();
			fileReceiverThread.start();
		}
	}
	
	
	class TextTransferThread extends Thread
	{
		
		boolean isNotConnected=true;
		JSONObject packet;
		boolean val;
		IMessageSentOffline listener=null;
		
		public TextTransferThread(IMessageSentOffline listener)
		{
			this.listener=listener;
		}
		@Override
		public void run() {
				
					String host=null;
					Logger.d(TAG,"Text Transfer Thread -->"+"Going to connect to socket");

		
			while (isNotConnected)
			{
				try
				{
					textSendSocket = new Socket();
					if (offlineManager.isHotspotCreated())
					{
						host = OfflineUtils.getIPFromMac(null);
					}
					else
					{
						host = IP_SERVER;
					}
					textSendSocket.bind(null);
					textSendSocket.connect((new InetSocketAddress(host, PORT_TEXT_MESSAGE)), SOCKET_TIMEOUT);
					Logger.d(TAG, "Text Transfer Thread Connected");
					isNotConnected=false;

				}
				catch (IOException e)
				{
					Logger.e(TAG, "IOEXCEPTION");
				}
			}
					try
					{
					while(true)
					{
					packet = OfflineManager.getInstance().getTextQueue().take();
					{
						// TODO : Send Offline Text and take action on the basis of boolean i.e. clock or single tick
						Logger.d("OfflineThreadManager", "Going to send Text");

						if (sendOfflineText(packet, textSendSocket.getOutputStream()))
						{
							listener.onSuccess(packet);
						}
						else
						{
							listener.onFailure(packet);
						}
					}
					}
				}
				catch (InterruptedException e) {
					Logger.e(TAG,"Some called interrupt on File transfer Thread");
					e.printStackTrace();
				}
				catch(SocketTimeoutException e)
				{
					Logger.e(TAG, "SOCKET time out exception occured");
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
		boolean isNotConnected=true;
		String host=null;
		int tries;
		@Override
		public void run() {
			Logger.d(TAG,"File Transfer Thread -->"+"Going to connect to socket");
			while (isNotConnected)
			{
				try
				{

					fileSendSocket = new Socket();
					if (offlineManager.isHotspotCreated())
					{
						host = OfflineUtils.getIPFromMac(null);
					}
					else
					{
						host = IP_SERVER;
					}
					Logger.d(TAG, "host is " + host);
					fileSendSocket.bind(null);
					fileSendSocket.connect(new InetSocketAddress(host, PORT_FILE_TRANSFER));
					Logger.d(TAG, "File Transfer Thread Connected");
					isNotConnected = false;
				}
				catch (IOException e)
				{
					Logger.d(TAG, "IO Exception in File Transfer Thread");
					try
					{
						Thread.sleep(300);
					}
					catch (InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
			}
			try
			{
				while (true)
				{
					fileTranserObject=OfflineManager.getInstance().getFileTransferQueue().take();
					// TODO : Send Offline Text and take action on the basis of boolean i.e. clock or single tick
					val = sendOfflineFile(fileTranserObject, fileSendSocket.getOutputStream());
				}
			}
			catch (InterruptedException e)
			{
				Logger.e(TAG, "Some called interrupt on File transfer Thread");
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Logger.e(TAG, "IO Exception occured.Socket was not bounded or connect failed");
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
				Logger.e(TAG,"Did we pass correct Address here ? ?");
			}
		}
	}
	
	class TextReceiveThread extends Thread
	{
		int type;
		int msgSize;
		InputStream inputStream=null;
		@Override
		public void run()
		{
				try
				{
					textServerSocket = new ServerSocket(PORT_TEXT_MESSAGE);
					Logger.d(TAG,"TextReceiveThread" + "Will be waiting on accept");
					textReceiverSocket = textServerSocket.accept();
					Logger.d(TAG,"TextReceiveThread" + "Connection successfull");
					inputStream=textReceiverSocket.getInputStream();
					while(true)
					{
						byte[] convMessageLength = new byte[4];
						inputStream.read(convMessageLength, 0, 4);
						msgSize = OfflineUtils.byteArrayToInt(convMessageLength);
						// Logger.d(TAG,"Msg size is "+msgSize);
						if(msgSize==0)
							continue;
						byte[] msgJSON = new byte[msgSize];
						int fileSizeRead=msgSize;
						int offset = 0;
						while(msgSize>0)
						{
							int len = inputStream.read(msgJSON, offset, msgSize);
							offset += len;
							msgSize -= len;
						}String msgString = new String(msgJSON, "UTF-8");
						Logger.d(TAG, "" + msgSize);
						JSONObject messageJSON = new JSONObject(msgString);
						Logger.d(TAG,"Message Received :-->"+msgString);

						// TODO: Ghost Packet Logic to come here
						// if ( isDisconnectPosted && !(isGhostPacket(messageJSON)) )
						// {
						// shouldBeDisconnected = false;
						// removeRunnable(waitingTimer);
						// isDisconnectPosted = true;
						// }

						ConvMessage convMessage = null;

						if (OfflineUtils.isPingPacket(messageJSON))
						{
							// Start client thread.
							startSendingThreads();
							String connectedDevice = OfflineUtils.getMsisdnFromPingPacket(messageJSON);
							offlineManager.onConnected(connectedDevice);
						}
						else if (OfflineUtils.isGhostPacket(messageJSON))
						{
							Logger.d(TAG, "Ghost Packet received");
							offlineManager.restartGhostTimeout();
						}
						else
						{
							messageJSON.put(HikeConstants.FROM, "o:"+offlineManager.getConnectedDevice());
							messageJSON.remove(HikeConstants.TO);
							
							if (OfflineUtils.isStickerMessage(messageJSON))
							{
								File stickerImage = isStickerPresentInApp(messageJSON);
								if (stickerImage.exists() == false)
								{
									FileOutputStream outputStream = new FileOutputStream(stickerImage);
									offlineManager.copyFile(inputStream, outputStream, stickerImage.length());
									OfflineUtils.closeOutputStream(outputStream);
								}
								// remove data from stream
								else
								{
									long fileSize = stickerImage.length();
									while(fileSize>0)
									{
										long len = inputStream.skip(fileSize);
										fileSize -= len;
									}
								}
							}
							else if (OfflineUtils.isChatThemeMessage(messageJSON))
							{
								//HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_THEME_CHANGE_MESSAGE, messageJSON);
								messageJSON.put(HikeConstants.TIMESTAMP, System.currentTimeMillis()/1000);
								MqttMessagesManager.getInstance(HikeMessengerApp.getInstance().getApplicationContext()).saveChatBackground(messageJSON);
								continue;
							}
							else
							{
								// It's a normal Text Message
								Logger.d(TAG,"Connected deive sis " + offlineManager.getConnectedDevice());
								
							}
							convMessage = new ConvMessage(messageJSON, HikeMessengerApp.getInstance().getApplicationContext());
							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
							HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

						}
						// TODO:Handle ghost packet
						// if(isDisconnectPosted && type != 10)
						// {
						// shouldBeDisconnected = true;
						// disconnectAfterTimeout();
						// }
						offlineManager.setInOfflineFileTransferInProgress(false);
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
					Logger.e(TAG, "Did we pass correct Address here ??");
				}
				catch (JSONException e)
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
				try
				{
					Logger.d(TAG,"Going to wait for fileReceive socket");
					fileServerSocket = new ServerSocket(PORT_FILE_TRANSFER);
					fileReceiveSocket = fileServerSocket.accept();
					
					Logger.d(TAG,"fileReceive socket connection success");
					offlineManager.setInOfflineFileTransferInProgress(true);

					InputStream inputstream = fileReceiveSocket.getInputStream();
					
					while(true)
					{
						byte[] metaDataLengthArray = new byte[4];
						int msgSize = inputstream.read(metaDataLengthArray,0,4);
						if(msgSize==0)
							continue;
						int metaDataLength = OfflineUtils.byteArrayToInt(metaDataLengthArray);
						Logger.d(TAG, "Size of MetaString: " + metaDataLength);
						ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(metaDataLength);
						boolean isMetaDataReceived = OfflineManager.getInstance().copyFile(inputstream, byteArrayOutputStream, metaDataLength);
						Logger.d(TAG, "Metadata Received Properly: " + isMetaDataReceived);
						byteArrayOutputStream.close();
						byte[] metaDataBytes = byteArrayOutputStream.toByteArray();
						String metaDataString = new String(metaDataBytes, "UTF-8");
						Logger.d(TAG, metaDataString);
						JSONObject fileJSON = null;
						JSONObject message = null;
						String filePath = "";
						long mappedMsgId = -1;
						String fileName = "";
						int fileSize=0;
						try 
						{
							message = new JSONObject(metaDataString);
							message.put(HikeConstants.FROM, "o:"+offlineManager.getConnectedDevice());
							message.remove(HikeConstants.TO);
	
							JSONObject metadata =  message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
							mappedMsgId = message.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
							
							fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
							fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
							int type = fileJSON.getInt(HikeConstants.HIKE_FILE_TYPE);
							fileName =  fileJSON.getString(HikeConstants.FILE_NAME);
							filePath = OfflineUtils.getFileBasedOnType(type, fileName);
							int totalChunks = OfflineUtils.getTotalChunks(fileSize);
							offlineManager.addToCurrentReceivingFile(mappedMsgId, new FileTransferModel( new TransferProgress(0, totalChunks), message));
							
						} 
						catch (JSONException e1) 
						{
							Logger.e(TAG, "Code phata in JSON initialisations", e1);
							e1.printStackTrace();
						}
	
						ConvMessage convMessage = null;
						try 
						{
							(message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES)).getJSONObject(0).putOpt(
									HikeConstants.FILE_PATH, filePath);
							convMessage = new ConvMessage(message, HikeMessengerApp.getInstance().getApplicationContext());
							
							// update DB and UI.
							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
							HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
							Logger.d(TAG, filePath);
	
							// TODO : Revisit the logic again.
							File f = new File(Environment.getExternalStorageDirectory() + "/" + "Hike/Media/hike Images" + "/tempImage_" + fileName);
							File dirs = new File(f.getParent());
							if (!dirs.exists())
								dirs.mkdirs();
							f.createNewFile();
							// TODO:Can be done via show progress pubsub.
							// showDownloadTransferNotification(mappedMsgId, fileSize);
							FileOutputStream outputStream = new FileOutputStream(f);
							// TODO:Take action on the basis of return type.
							offlineManager.copyFile(inputstream, new FileOutputStream(f), mappedMsgId, true, false, fileSize);
							OfflineUtils.closeOutputStream(outputStream);
							f.renameTo(new File(filePath));
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						
						offlineManager.removeFromCurrentReceivingFile(mappedMsgId);
						
						offlineManager.showSpinnerProgress(false, mappedMsgId);
						//TODO:Disconnection handling:
						//if(isDisconnectPosted)
						//{
						//	shouldBeDisconnected = true;
						//	disconnectAfterTimeout();
						//}
						offlineManager.setInOfflineFileTransferInProgress(false);
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
			fileUri = OfflineUtils.getStickerPath(packet);
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
			
			try
			{
				byte[] messageBytes = packet.toString().getBytes("UTF-8");
				int length = messageBytes.length;
				byte[] intToBArray = OfflineUtils.intToByteArray(length);
				outputStream.write(intToBArray, 0, intToBArray.length);
				outputStream.write(messageBytes, 0, length);
				isSent = offlineManager.copyFile(inputStream, outputStream, f.length());
				inputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				isSent=false;
			}
			
		}
		// for normal text messages and ping packet
		else
		{
			try
			{
				byte[] messageBytes = packet.toString().getBytes("UTF-8");
				int length = messageBytes.length;
				byte[] intToBArray = OfflineUtils.intToByteArray(length);
				outputStream.write(intToBArray, 0, intToBArray.length);
				outputStream.write(messageBytes,0, length);
				isSent=true;
			}
			catch (IOException e)
			{
				Logger.d(TAG,"IO Exception in sendOfflineText");
				e.printStackTrace();
				isSent=false;
			}

		}
		
		// Updating database
		
		return isSent;
	}

	private boolean sendOfflineFile(FileTransferModel fileTransferModel,OutputStream outputStream)
	{
		offlineManager.setInOfflineFileTransferInProgress(true);
		boolean isSent = true;
		String fileUri =null;
		InputStream inputStream = null;
		JSONObject  jsonFile =  null;
		try
		{
			JSONArray jsonFiles = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			jsonFile = (JSONObject) jsonFiles.get(0);
			fileUri = jsonFile.getString(HikeConstants.FILE_PATH);
			
			String metaString = fileTransferModel.getPacket().toString();
			Logger.d(TAG, metaString);
			
			byte[] metaDataBytes = metaString.getBytes("UTF-8");
			int length = metaDataBytes.length;
			Logger.d(TAG, "Sizeof metaString: " + length);
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.write(intToBArray, 0, intToBArray.length);
			
			ByteArrayInputStream byteArrayInputStream =  new ByteArrayInputStream(metaDataBytes);
			boolean isMetaDataSent = offlineManager.copyFile(byteArrayInputStream, outputStream, metaDataBytes.length);
			Logger.d(TAG, "FileMetaDataSent:" + isMetaDataSent);
			byteArrayInputStream.close();
			
			JSONObject metadata;
			int fileSize  = 0;
			metadata = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			JSONObject fileJSON = (JSONObject) metadata.getJSONArray(HikeConstants.FILES).get(0);
			fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
			
			long msgID;
			msgID = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
			
			//TODO:We can listen to PubSub ...Why to do this ...????
			//showUploadTransferNotification(msgID,fileSize);
			
			inputStream = new FileInputStream(new File(fileUri));
			long time=System.currentTimeMillis();
			isSent = offlineManager.copyFile(inputStream, outputStream, msgID, true, true,fileSize);
			// in seconds
			long TimeTaken=(System.currentTimeMillis()-time)/1000;
			Logger.d(TAG,"Time taken to send file is "+ TimeTaken + "Speed is "+ fileSize/(1024*1024*TimeTaken) );
			inputStream.close();
			offlineManager.removeFromCurrentSendingFile(fileTransferModel.getMessageId());
			
			// Update Delivered status
			String msisdn = fileTransferModel.getPacket().getString(HikeConstants.TO);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
			int rowsUpdated = OfflineUtils.updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);
			if (rowsUpdated == 0)
			{
				Logger.d(getClass().getSimpleName(), "No rows updated");
			}
			Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		offlineManager.setInOfflineFileTransferInProgress(false);
		return isSent;
				

	}
	
	public File isStickerPresentInApp(JSONObject messageJSON) throws JSONException, IOException
	{
		String ctgId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.CATEGORY_ID);
		String stkId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);
		Sticker sticker = new Sticker(ctgId, stkId);

		File stickerImage;
		String stickerPath = sticker.getStickerPath(HikeMessengerApp.getInstance().getApplicationContext());
		stickerImage = new File(stickerPath);

		// sticker is not present
		if (stickerImage == null || (stickerImage.exists() == false))
		{
			File parent = new File(stickerImage.getParent());
			if (!parent.exists())
				parent.mkdirs();
			stickerImage.createNewFile();
			
		}
		return stickerImage;
	}
	
	/**
	 * Handle the call of text message here ...either success or failure
	 */
	private IMessageSentOffline fileMessageCallback=new IMessageSentOffline()
	{
		
		@Override
		public void onSuccess(JSONObject convMessage)
		{
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onFailure(JSONObject convMessage)
		{
			// TODO Auto-generated method stub
			
		}
	};
	
	/**
	 * Handle the call of file message here ...either success or failure
	 */
	private IMessageSentOffline textMessageCallback =new IMessageSentOffline()
	{
//TODO:delete from DataBase (Mqtt wala also)
		@Override
		public void onSuccess(JSONObject packet)
		{
			if (!OfflineUtils.isGhostPacket(packet) && !OfflineUtils.isPingPacket(packet))
			{
				long msgId;
				try
				{
					msgId = packet.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);

					String msisdn = packet.getString(HikeConstants.TO);
					long startTime = System.currentTimeMillis();
					int rowsUpdated = OfflineUtils.updateDB(msgId, ConvMessage.State.SENT_DELIVERED, msisdn);
					Logger.d(TAG, "Time  taken: " + (System.currentTimeMillis() - startTime));
					if (rowsUpdated == 0)
					{
						Logger.d(getClass().getSimpleName(), "No rows updated");
					}
					Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgId);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
					Logger.d(TAG, "Message Send Successfully");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onFailure(JSONObject packet)
		{
			Logger.d(TAG, "Message sending failed");
		}
	};

}
