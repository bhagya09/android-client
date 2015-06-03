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
import java.util.concurrent.BlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.support.v4.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

/**
 * 
 * @author himanshu, deepak malik
 *	This class mainly deals with socket connection,text and file are send and received in this class only.
 */
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
	
	private TextReceiveThread textReceiveThread=null;
	
	private FileReceiverThread fileReceiverThread=null;

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
	
	public void startSendingThreads()
	{
		textTransferThread.start();
		fileTransferThread.start();
	}
	
	public void startReceivingThreads()
	{
		textReceiveThread.start();
		fileReceiverThread.start();
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
				Logger.e(TAG, "IO Exception occured.Socket was not bounded or connect failed");
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
					val = sendOfflineFile(fileTranserObject,fileSendSocket.getOutputStream());
				}
			} catch (InterruptedException e) {
				Logger.e(TAG,"Some called interrupt on File transfer Thread");
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Logger.e(TAG, "IO Exception occured.Socket was not bounded or connect failed");
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
		int type;
		int msgSize;
		InputStream inputStream=null;
		@Override
		public void run()
		{
			if (!textServerSocket.isBound())
			{
				try
				{
					textServerSocket = new ServerSocket(PORT_TEXT_MESSAGE);
					textReceiverSocket = textServerSocket.accept();
					inputStream=textReceiverSocket.getInputStream();
					while(true)
					{
						byte[] convMessageLength = new byte[4];
						inputStream.read(convMessageLength, 0, 4);
						offlineManager.setIsOfflineFileTransferInProgress(true);
						msgSize = OfflineUtils.byteArrayToInt(convMessageLength);
						byte[] msgJSON = new byte[msgSize];
						inputStream.read(msgJSON, 0, msgSize);
						String msgString = new String(msgJSON, "UTF-8");
						Logger.d(TAG, "" + msgSize);
						JSONObject messageJSON = new JSONObject(msgString);

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
							offlineManager.onConnected(textReceiverSocket.getInetAddress().getHostAddress());
						}
						else if (OfflineUtils.isGhostPacket(messageJSON))
						{
							// ghost packet received reschedule the disconnect
							// timer for another cycle
							// type = GHOST_PACKET;
							// Logger.d(TAG, "Ghost Packet received");
							// messageJSON.put(HikeConstants.FROM, connectedDevice);
							// messageJSON.remove(HikeConstants.TO);
							// removeRunnable(ghostPacketDisconnect);
							// mThread.postRunnableWithDelay(ghostPacketDisconnect, ghostPacketReceiveTimeout);
						}
						else
						{
							if (OfflineUtils.isStickerMessage(messageJSON))
							{
								File stickerImage = isStickerPresentInApp(messageJSON);
								if (stickerImage.exists() == false)
								{
									FileOutputStream outputStream = new FileOutputStream(stickerImage);
									offlineManager.copyFile(inputStream, outputStream, stickerImage.length());
									outputStream.close();
								}
								// remove data from stream
								else
								{
									long length = stickerImage.length();
									while(length > 0)
									{
										long bytesSkipped = inputStream.skip(length);
										length -= bytesSkipped;
									}
								}
							}
							else if (OfflineUtils.isChatThemeMessage(messageJSON))
							{
								HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_THEME_CHANGE_MESSAGE, messageJSON);
							}
							else
							{
								// It's a normal Text Message
								messageJSON.put(HikeConstants.FROM, offlineManager.getConnectedDevice());
								messageJSON.remove(HikeConstants.TO);
								convMessage = new ConvMessage(messageJSON, HikeMessengerApp.getInstance().getApplicationContext());
							}

							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
							HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

						}
						// TODO:Handle ghost packet
						// if(isDisconnectPosted && type != 10)
						// {
						// shouldBeDisconnected = true;
						// disconnectAfterTimeout();
						// }
						offlineManager.setIsOfflineFileTransferInProgress(false);
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
					offlineManager.setIsOfflineFileTransferInProgress(true);
					InputStream inputstream = fileReceiveSocket.getInputStream();
					
					while(true)
					{
						byte[] metaDataLengthArray = new byte[4];
						inputstream.read(metaDataLengthArray,0,4);
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
						try 
						{
							message = new JSONObject(metaDataString);
							message.put(HikeConstants.FROM,offlineManager.getConnectedDevice());
							message.remove(HikeConstants.TO);
	
							JSONObject metadata =  message.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
							mappedMsgId = message.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
							
							fileJSON = metadata.getJSONArray(HikeConstants.FILES).getJSONObject(0);
							int fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
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
							offlineManager.copyFile(inputstream, new FileOutputStream(f), mappedMsgId, true, false, outputStream.getChannel().size());
							outputStream.close();
							f.renameTo(new File(filePath));
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						
						offlineManager.removeFromCurrentReceivingFile(mappedMsgId);
						//TODO:Disconnection handling:
						//if(isDisconnectPosted)
						//{
						//	shouldBeDisconnected = true;
						//	disconnectAfterTimeout();
						//}
						offlineManager.setIsOfflineFileTransferInProgress(false);
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
			
			try
			{
				byte[] messageBytes = packet.toString().getBytes("UTF-8");
				int length = messageBytes.length;
				byte[] intToBArray = OfflineUtils.intToByteArray(length);
				outputStream.write(intToBArray, 0, intToBArray.length);
				outputStream.write(messageBytes, 0, length);
				isSent = offlineManager.copyFile(inputStream, outputStream, fileUri.getBytes("UTF-8").length);
				inputStream.close();
				
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return false;
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
			}
			catch (IOException e) 
			{
				e.printStackTrace();
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

	private boolean sendOfflineFile(FileTransferModel fileTransferModel,OutputStream outputStream)
	{
		offlineManager.setIsOfflineFileTransferInProgress(true);
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
			fileTransferModel.getTransferProgress().setCurrentChunks(OfflineUtils.getTotalChunks(fileSize));
			
			//TODO:We can listen to PubSub ...Why to do this ...????
			//showUploadTransferNotification(msgID,fileSize);
			
			inputStream = new FileInputStream(new File(fileUri));
			isSent = offlineManager.copyFile(inputStream, outputStream, msgID, true, true,fileSize);
			inputStream.close();
			offlineManager.removeFromCurrentSendingFile(msgID);
			
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
		offlineManager.setIsOfflineFileTransferInProgress(false);
		return isSent;
				

	}
	
	public File isStickerPresentInApp(JSONObject messageJSON) throws JSONException, IOException
	{
		messageJSON.put(HikeConstants.FROM, OfflineManager.getInstance().getConnectedDevice());
		messageJSON.remove(HikeConstants.TO);
		
		String ctgId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_CATEGORY);
		String stkId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);
		Sticker sticker = new Sticker(ctgId, stkId);

		File stickerImage;
		String stickerPath = OfflineUtils.getStickerPath(sticker);
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

}
