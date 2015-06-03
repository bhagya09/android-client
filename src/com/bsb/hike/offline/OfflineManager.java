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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu, deepak malik
 * This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue. 
 */
public class OfflineManager
{
	public static final OfflineManager _instance = new OfflineManager();

	private boolean isHotSpotCreated = false;
	
	private ConcurrentHashMap<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private ConcurrentHashMap<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private BlockingQueue<JSONObject> textMessageQueue = null;

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private volatile boolean isFileTransferInProgress=false;
	
	private Context context;
	
	private String connectedDevice;
	
	Handler handler =new Handler(HikeHandlerUtil.getInstance().getLooper())
	{
		public void handleMessage(android.os.Message msg) {
			if(msg==null)
			{
				return;
			}
			
			handleMsgOnBackEndThread(msg);
		}

	};

	
	private static final String TAG = OfflineManager.class.getName();
	
	private OfflineManager()
	{
		init();
	}

	public static OfflineManager getInstance()
	{
		return _instance;
	}
	
	
	private void handleMsgOnBackEndThread(Message msg)
	{
		switch(msg.what)
		{
		case OfflineConstants.HandlerConstants.SAVE_MSG_DB:
			saveToDb((ConvMessage)msg.obj);
			break;
		}
	};
	
	private void saveToDb(ConvMessage convMessage)
	{
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage,true);
	}
	
	public void performWorkOnBackEndThread(Message msg)
	{
		handler.sendMessage(msg);
	}

	/**
	 * Initialize all your functions here
	 */
	private void init()
	{
		textMessageQueue=new LinkedBlockingQueue<>();
		fileTransferQueue=new LinkedBlockingQueue<>();
		context=HikeMessengerApp.getInstance().getApplicationContext();
	}

	public void setIsHotSpotCreated(boolean isHotSpotCreated)
	{
		this.isHotSpotCreated = isHotSpotCreated;
	}

	public boolean isHotspotCreated()
	{
		return isHotSpotCreated;
	}
	
	public synchronized void addToTextQueue(JSONObject message)
	{
		textMessageQueue.add(message);
	}
	
	public synchronized void addToFileQueue(FileTransferModel fileTransferObject)
	{
		fileTransferQueue.add(fileTransferObject);
		addToCurrentSendingFile(fileTransferObject.getMessageId(), fileTransferObject);
	}
	
	public  boolean copyFile(InputStream inputStream, OutputStream outputStream,long fileSize)
	{
		return copyFile(inputStream, outputStream,-1,false,false,fileSize);
	}
	
	public boolean copyFile(InputStream inputStream, OutputStream out, long msgId, boolean showProgress, boolean isSent,long fileSize) 
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
				buf = new byte[(int)fileSize];
				len = inputStream.read(buf);
				fileSize -= len;
				out.write(buf, 0, len);
				if (showProgress)
				{
					showSpinnerProgress(isSent,msgId);
				}
			}
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
			FileTransferModel fileTransfer=currentSendingFiles.get(msgId);
			fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
		}
		else
		{
			FileTransferModel fileTransfer=currentReceivingFiles.get(msgId);
			fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);	
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

	public FileSavedState getFileState(ConvMessage convMessage, File file)
	{
		return convMessage.isSent() ? getUploadFileState(convMessage,file):getDownloadFileState(convMessage,file); 
	}
	
	private FileSavedState getUploadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		
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
		return fss;
	}
	
	/**
	 * 
	 * @param convMessage
	 * @param file
	 * @return
	 * TODO:Removing the try catch for the app to crash .So that we can debug, what the issue was.
	 */
	private FileSavedState getDownloadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMappedMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

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
		return fss;
	}
	
	public void setIsOfflineFileTransferInProgress(boolean val)
	{
		isFileTransferInProgress = val;
	}

	public String getConnectedDevice()
	{
		return connectedDevice;
	}
	
	public void addToCurrentReceivingFile(long msgId,FileTransferModel fileTransferModel)
	{
		currentReceivingFiles.put(msgId,fileTransferModel);
	}

	public void removeFromCurrentReceivingFile(long msgId)
	{
		if(currentReceivingFiles.contains(msgId))
		{
			currentReceivingFiles.remove(msgId);
		}
	}
	
	public void addToCurrentSendingFile(long msgId, FileTransferModel fileTransferModel)
	{
		currentSendingFiles.put(msgId, fileTransferModel);
	}
	
	public void removeFromCurrentSendingFile(long msgId)
	{
		if(currentSendingFiles.contains(msgId))
		{
			currentSendingFiles.remove(msgId);
		}
	}

	public void initialiseOfflineFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			 int attachmentType, String msisdn,String apkLabel)
	{
		int type = hikeFileType.ordinal();
		File file = new File(filePath);
		String fileName = file.getName();
		if (type == HikeFileType.APK.ordinal())
			fileName = apkLabel + ".apk";
		ConvMessage convMessage = FileTransferManager.getInstance(context).uploadOfflineFile(msisdn, file, fileKey, fileType, hikeFileType, isRecording,
				recordingDuration, attachmentType, fileName);
		addToFileQueue(new FileTransferModel(new TransferProgress(), convMessage.serialize()));
	}

	public void onConnected(String hostAddress) {
		
		
	}
}
