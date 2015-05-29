package com.bsb.hike.offline;

import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.Logger;

public class OfflineManager
{
	public static final OfflineManager _instance = new OfflineManager();

	private boolean isHotSpotCreated = false;
	
	private HashMap<Long, FileTransferModel> currentSendingFiles = new HashMap<Long, FileTransferModel>();

	private HashMap<Long, FileTransferModel> currentReceivingFiles = new HashMap<Long, FileTransferModel>();

	private final static Object currentReceivingFilesLock = new Object();

	private final static Object currentSendingFilesLock = new Object();
	
	private BlockingQueue<JSONObject> textMessageQueue = null;

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private volatile boolean inFileTransferInProgress=false;
	
	private static final String TAG=OfflineManager.class.getName();
	
	private OfflineManager()
	{
		init();
	}

	public static OfflineManager getInstance()
	{
		return _instance;
	}
	
	/**
	 * Initialize all your functions here
	 */
	private void init()
	{
		textMessageQueue=new LinkedBlockingQueue<>();
		fileTransferQueue=new LinkedBlockingQueue<>();
	}

	public void setIsHotSpotCreated(boolean isHotSpotCreated)
	{
		this.isHotSpotCreated = isHotSpotCreated;
	}

	public boolean isHotspotCreated()
	{
		return isHotSpotCreated;
	}
	
	public void addToTextQueue(JSONObject message)
	{
		textMessageQueue.add(message);
	}
	
	public void addToFileQueue(FileTransferModel fileTransferObject)
	{
		fileTransferQueue.add(fileTransferObject);
	}
	
	public  boolean copyFile(InputStream inputStream, OutputStream outputStream,int fileSize)
	{
		return copyFile(inputStream, outputStream,-1,false,false,fileSize);
	}
	
	public boolean copyFile(InputStream inputStream, OutputStream out, long msgId, boolean showProgress, boolean isSent,int fileSize) 
	{
		byte buf[] = new byte[OfflineConstants.CHUNK_SIZE];
		int len;
		boolean isCopied = false;
		try {
			while (fileSize>=OfflineConstants.CHUNK_SIZE) {
				len = inputStream.read(buf);
				out.write(buf, 0, len);
				fileSize -= len;	
				if (showProgress)
				{
					
					showSpinnerProgress(isSent,msgId);
				}
			}
			while(fileSize > 0) 
			{
				buf = new byte[fileSize];
				len = inputStream.read(buf);
				fileSize -= len;
				out.write(buf, 0, len);
				if (showProgress)
				{
					showSpinnerProgress(isSent,msgId);
				}
			}
			inputStream.close();
			isCopied = true;
		} catch (IOException e) {
			Logger.e("Spinner", "Exception in copyFile: ", e);
			isCopied = false;
		}
		return isCopied;
	}
	
	private void showSpinnerProgress(boolean isSent,long msgId)
	{
		if(isSent)
		{
			synchronized (currentSendingFilesLock) {
				FileTransferModel fileTransfer=currentSendingFiles.get(msgId);
				fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
			}
		}
		else
		{
			synchronized (currentReceivingFilesLock) {
				FileTransferModel fileTransfer=currentReceivingFiles.get(msgId);
				fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
			}
			
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public BlockingQueue<JSONObject> getTextQueue()
	{
		return textMessageQueue;
	}

	public BlockingQueue<FileTransferModel> getFileTransferQueue()
	{
		return fileTransferQueue;
	}
	
	public String getHostAddress()
	{
		String host = null;
		if (isHotspotCreated())
		{
			host = OfflineUtils.getIPFromMac(null);
		}
		else
		{
			host = IP_SERVER;
		}
		return host;
	}
	
	
	public boolean sendOfflineFile(FileTransferModel fileTransferModel,OutputStream outputStream)
	{
		inFileTransferInProgress = true;
		boolean isSent = true;
		String fileUri =null;
		InputStream inputStream = null;
		JSONObject  jsonFile =  null;
		try
		{
			JSONArray jsonFiles = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			jsonFile = (JSONObject) jsonFiles.get(0);
			fileUri = jsonFile.getString(HikeConstants.FILE_PATH);
			inputStream = new FileInputStream(fileUri);
			
			String metaString = fileTransferModel.getPacket().toString();
			Logger.d(TAG, metaString);
			byte[] metaDataBytes = metaString.getBytes("UTF-8");
			int length = metaDataBytes.length;
			Logger.d(TAG, "Sizeof metaString: " + length);
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.flush();
			outputStream.write(intToBArray, 0, intToBArray.length);
			ByteArrayInputStream byteArrayInputStream =  new ByteArrayInputStream(metaDataBytes);
			boolean isMetaDataSent = copyFile(byteArrayInputStream, outputStream, metaDataBytes.length);
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
			
			
			isSent = copyFile(inputStream, outputStream, msgID, true, true,fileSize);
				synchronized (currentSendingFilesLock) {
					currentSendingFiles.remove(msgID);
				}
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
		inFileTransferInProgress=false;
		return isSent;
				

	}

	public FileSavedState getFileState(ConvMessage convMessage, File file)
	{
		return convMessage.isSent() ? getUploadFileState(convMessage,file):getDownloadFileState(convMessage,file); 
	}
	
	private FileSavedState getUploadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		synchronized (currentSendingFilesLock)
		{
			if (currentSendingFiles.containsKey(msgId))
			{
				Logger.d("Spinner", "Current Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
			}
			else
			{
				Logger.d("Spinner", "Completed Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
			}
		}
		return fss;
	}
	
	/**
	 * 
	 * @param convMessage
	 * @param file
	 * @return
	 * TODO:Removing the try catch for the app to crash .So that we cab debug ki what was the issue.
	 */
	private FileSavedState getDownloadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMappedMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		synchronized (currentReceivingFilesLock)
		{
			if (currentReceivingFiles.containsKey(msgId))
			{
				Logger.d("Spinner", "Current Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);

			}
			else
			{
				Logger.d("Spinner", "Completed Msg Id -> " + msgId);
				fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
			}
		}
		return fss;
	}
	
}
