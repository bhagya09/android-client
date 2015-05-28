package com.bsb.hike.offline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
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
	
	public  boolean copyFile(InputStream inputStream, OutputStream outputStream)
	{
		return copyFile(inputStream, outputStream,-1,false,false);
	}
	
	public   boolean copyFile(InputStream inputStream, OutputStream out, long msgId, boolean showProgress, boolean isSent) 
	{
		byte buf[] = new byte[OfflineConstants.CHUNK_SIZE];
		int len;
		boolean isCopied = false;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);
				if (showProgress)
				{
					if(isSent)
					{
						synchronized (currentSendingFilesLock) {
							FileTransferModel fileTransfer=currentSendingFiles.get(msgId);
							fileTransfer.getTransferProgress().currentChunks++;
						}
					}
					else
					{
						synchronized (currentReceivingFilesLock) {
							FileTransferModel fileTransfer=currentReceivingFiles.get(msgId);
							fileTransfer.getTransferProgress().currentChunks++;
						}
						
					}
					showSpinnerProgress(isSent);
				}
			}
			out.close();
			inputStream.close();
			isCopied = true;
		} catch (IOException e) {
			Logger.e("Spinner", "Exception in copyFile: ", e);
			isCopied = false;
		}
		return isCopied;
	}
	
	private void showSpinnerProgress(boolean isSent)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		return;
	}

	public BlockingQueue<JSONObject> getTextQueue()
	{
		return textMessageQueue;
	}

	public BlockingQueue<FileTransferModel> getFileTransferQueue()
	{
		return fileTransferQueue;
	}
	
	
}
