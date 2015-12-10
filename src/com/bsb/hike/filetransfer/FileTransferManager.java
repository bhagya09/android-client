package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Logger;

/**
 * 
 */
public class FileTransferManager
{
	public static String FT_CANCEL = "ft_cancel";

	public static String READ_FAIL = "read_fail";

	public static String UPLOAD_FAILED = "upload_failed";

	public static String UNABLE_TO_DOWNLOAD = "unable_to_download";

	public static final int FAKE_PROGRESS_DURATION = 8 * 1000;

	private final Context context;

	private final ConcurrentHashMap<Long, FileTransferBase> fileTaskMap;

	private final int taskLimit;

	private final int TASK_OVERFLOW_LIMIT = 90;

	private static volatile FileTransferManager _instance = null;

	public enum NetworkType
	{
		WIFI
		{
			@Override
			public int getMaxChunkSize()
			{
				return 1024 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 256 * 1024;
			}
		},
		FOUR_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 512 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 128 * 1024;
			}
		},
		THREE_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 256 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 128 * 1024;
			}
		},
		TWO_G
		{
			@Override
			public int getMaxChunkSize()
			{
				return 32 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 16 * 1024;
			}
		},
		NO_NETWORK
		{
			@Override
			public int getMaxChunkSize()
			{
				return 2 * 1024;
			}

			@Override
			public int getMinChunkSize()
			{
				return 1 * 1024;
			}
		};

		public abstract int getMaxChunkSize();

		public abstract int getMinChunkSize();
	}

    /**
     *
     * @param ctx
     */
	private FileTransferManager(Context ctx)
	{
		context = ctx;
		taskLimit = context.getResources().getInteger(R.integer.ft_limit);
		fileTaskMap = new ConcurrentHashMap<>();
	}

    /**
     *
     * @param context
     * @return
     */
	public static FileTransferManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (FileTransferManager.class)
			{
				if (_instance == null)
					_instance = new FileTransferManager(context.getApplicationContext());
			}
		}
		return _instance;
	}

    /**
     *
     * @param msgId
     * @return
     */
	public boolean isFileTaskExist(long msgId)
	{
		return fileTaskMap.containsKey(msgId);
	}

    /**
     *
     * @param msgId
     */
	void removeTask(long msgId)
	{
		fileTaskMap.remove(msgId);
	}

    /**
     *
     * @return
     */
	public int getTaskLimit()
	{
		return taskLimit;
	}

    /**
     *
     * @return
     */
	public int remainingTransfers()
	{
		if (taskLimit > fileTaskMap.size())
		{
			return (taskLimit - fileTaskMap.size());
		}
		return 0;
	}

    /**
     *
     * @return
     */
	public boolean taskOverflowLimitAchieved()
	{
		if (fileTaskMap.size() >= TASK_OVERFLOW_LIMIT)
		{
			return true;
		}
		return false;
	}

    /**
     *
     * @param msgId
     * @return
     */
	public ConvMessage getMessage(long msgId)
	{
		FileTransferBase ftb = fileTaskMap.get(msgId);
		if (ftb != null)
		{
			Object userContext = ftb.getUserContext();
			if (userContext instanceof ConvMessage)
				return (ConvMessage) userContext;
		}
		return null;
	}

    /**
     *
     * @param destinationFile
     * @param fileKey
     * @param msgId
     * @param hikeFileType
     * @param userContext
     * @param showToast
     */
	public void downloadFile(File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, ConvMessage userContext, boolean showToast)
	{
		if (destinationFile.exists())
		{
			// TODO
			return;
		}

		if (!isFileTaskExist(msgId) && taskOverflowLimitAchieved())
		{
			return;
		}

		if (isFileTaskExist(msgId))
		{
			DownloadFileTask task = (DownloadFileTask) fileTaskMap.get(msgId);
			task.download();
			return;
		}

		File tempDownloadedFile;
		try
		{
			/*
			 * Changes done to fix the issue where some users are getting FileNotFoundEXception while creating file.
			 */
			File dir = FTUtils.getHikeTempDir(context);
			if (!dir.exists())
			{
				if (!dir.mkdirs())
				{
					Logger.d("DownloadFileTask", "failed to create directory");
					Toast.makeText(context, R.string.no_sd_card, Toast.LENGTH_SHORT).show();
					return;
				}
			}
			tempDownloadedFile = new File(dir, destinationFile.getName() + ".part");
			if (!tempDownloadedFile.exists())
				tempDownloadedFile.createNewFile();
		}
		catch (NullPointerException e)
		{
			FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_INIT_1_1, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "NO_SD_CARD : ", e);
			Toast.makeText(context, R.string.no_sd_card, Toast.LENGTH_SHORT).show();
			return;
		}
		catch (IOException e)
		{
			FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_INIT_1_2, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "NO_SD_CARD : ", e);
			Logger.d("DownloadFileTask", "Failed to create File. " + e);
			Toast.makeText(context, R.string.no_sd_card, Toast.LENGTH_SHORT).show();
			return;
		}

		DownloadFileTask task = new DownloadFileTask(context, tempDownloadedFile, destinationFile, fileKey, msgId, hikeFileType, userContext);
		fileTaskMap.put(msgId, task);
		task.download();
	}

    /**
     *
     * @param convMessage
     * @param fileKey
     */
	public void uploadFile(ConvMessage convMessage, String fileKey)
    {
		if (isFileTaskExist(convMessage.getMsgID()))
		{
			UploadFileTask task = (UploadFileTask) fileTaskMap.get(convMessage.getMsgID());
			task.upload();
			return;
		}
        // TODO
		// validateFilePauseState(convMessage.getMsgID());
		// return;
		// }
		//

		if (!isFileTaskExist(convMessage.getMsgID()) && taskOverflowLimitAchieved())
		{
			return;
		}

		UploadFileTask task = new UploadFileTask(context, convMessage, fileKey);
		fileTaskMap.put(convMessage.getMsgID(), task);
		task.startFileUploadProcess();
	}

    /**
     *
     * @param contactList
     * @param messageList
     * @param fileKey
     */
	public void uploadFile(List<ContactInfo> contactList, List<ConvMessage> messageList, String fileKey)
	{
		ConvMessage convMessage = messageList.get(0);
		// TODO if (isFileTaskExist(convMessage.getMsgID())){
		// validateFilePauseState(convMessage.getMsgID());
		// return;
		// }

		if (!isFileTaskExist(convMessage.getMsgID()) && taskOverflowLimitAchieved())
		{
			return;
		}

		UploadFileTask task = new UploadFileTask(context, contactList, messageList, fileKey);
		for (ConvMessage msg : messageList)
		{
			fileTaskMap.put(msg.getMsgID(), task);
		}
		task.startFileUploadProcess();
	}

    /**
     *
     * @param convMessage
     * @param uploadingContact
     */
	public void uploadContactOrLocation(ConvMessage convMessage, boolean uploadingContact)
	{
		// TODO
		// if (isFileTaskExist(convMessage.getMsgID())){
		// validateFilePauseState(convMessage.getMsgID());
		// return;
		// }
		if (!isFileTaskExist(convMessage.getMsgID()) && taskOverflowLimitAchieved())
		{
			return;
		}

		UploadContactOrLocationTask task = new UploadContactOrLocationTask(context, convMessage, uploadingContact);
		fileTaskMap.put(convMessage.getMsgID(), task);
		task.execute();
	}

    /**
     *
     * @param msgId
     * @param mFile
     * @param sent
     * @param fileSize
     */
	public void cancelTask(long msgId, File mFile, boolean sent, long fileSize)
	{
		FileSavedState fss;
		if (sent)
			fss = getUploadFileState(msgId, mFile);
		else
			fss = getDownloadFileState(msgId, mFile);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.INITIALIZED)
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				obj.cancel();
			}
			Logger.d(getClass().getSimpleName(), "deleting state file" + msgId);
			deleteStateFile(msgId, mFile);
			if (!sent)
			{
				Logger.d(getClass().getSimpleName(), "deleting tempFile" + msgId);
				File tempDownloadedFile = new File(FTUtils.getHikeTempDir(context), mFile.getName() + ".part");
				if (tempDownloadedFile != null && tempDownloadedFile.exists())
					tempDownloadedFile.delete();

			}
			// TODO
			//FTAnalyticEvents analyticEvent = FTAnalyticEvents.getAnalyticEvents(getAnalyticFile(mFile, msgId));
			//String network = analyticEvent.mNetwork + "/" + getNetworkTypeString();
			//analyticEvent.sendFTSuccessFailureEvent(network, fileSize, FTAnalyticEvents.FT_FAILED);
			deleteLogFile(msgId, mFile);
		}
	}

    /**
     *
     * @param msgId
     */
	public void pauseTask(long msgId)
	{
		FileTransferBase obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			obj.pause();
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}
	}

	/**
	 * This will be used when user deletes corresponding chat bubble
	 *
	 * @param msgId
	 * @param mFile
	 */
	private void deleteStateFile(long msgId, File mFile)
	{
		// TODO
	}

	/**
	 * This will be used when user deletes corresponding chat bubble
	 *
	 * @param msgId
	 * @param mFile
	 */
	private void deleteLogFile(long msgId, File mFile)
	{
		String fName = mFile.getName() + ".log." + msgId;
		File f = new File(FTUtils.getHikeTempDir(context), fName);
		if (f != null)
			f.delete();
	}

	/**
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		fileTaskMap.clear();
		FTUtils.deleteAllFTRFiles(context);
		_instance = null;
	}

    /**
     * This function gives the state of downloading for a file
     *
     * @param msgId
     * @param mFile
     * @return
     */
	public FileSavedState getDownloadFileState(long msgId, File mFile)
	{
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return obj.getFileSavedState();
			}
			return new FileSavedState(FTState.IN_PROGRESS, 0, 0, 0);
		}
		else
		{
			if (mFile == null)
			{
				// TODO // @GM only for now. Has to be handled properly
				return new FileSavedState();
			}

			FileSavedState fss = null;
			if (mFile.exists())
			{
				fss = new FileSavedState(FTState.COMPLETED, 100, 100, 100);
			}
			else
			{
				// TODO get filesaved state from task http manager
				// if (fss.getAnimatedProgress() > 0)
				// setAnimatedProgress(fss.getAnimatedProgress(), msgId);
			}
			return fss != null ? fss : new FileSavedState();
		}
	}

    /**
     * This function gives the state of uploading for a file
     *
     * @param msgId
     * @param mFile
     * @return
     */
	public FileSavedState getUploadFileState(long msgId, File mFile)
	{
		Logger.d(getClass().getSimpleName(), "Returning state for message ID : " + msgId);
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return obj.getFileSavedState();
			}
			return new FileSavedState(FTState.IN_PROGRESS, 0, 0, 0);
		}
		else
		{
			// TODO
			// Logger.d(getClass().getSimpleName(), "Returning from second call");
			// if (mFile == null) // @GM only for now. Has to be handled properly
			// return new FileSavedState();
			//
			FileSavedState fss = null;
			// FileInputStream fileIn = null;
			// ObjectInputStream in = null;
			// try
			// {
			// String fName = mFile.getName() + ".bin." + msgId;
			// Logger.d(getClass().getSimpleName(), fName);
			// File f = new File(getHikeTempDir(), fName);
			// if (!f.exists())
			// {
			// return new FileSavedState();
			// }
			// fileIn = new FileInputStream(f);
			// in = new ObjectInputStream(fileIn);
			// fss = (FileSavedState) in.readObject();
			// if (fss.getAnimatedProgress() > 0)
			// setAnimatedProgress(fss.getAnimatedProgress(), msgId);
			// }
			// catch (IOException i)
			// {
			// i.printStackTrace();
			// FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", i);
			// }
			// catch (ClassNotFoundException e)
			// {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", e);
			// }
			// catch (Exception e)
			// {
			// e.printStackTrace();
			// Logger.e(getClass().getSimpleName(), "Exception while reading state file : ", e);
			// FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", e);
			// }
			// finally
			// {
			// Utils.closeStreams(in, fileIn);
			// }
			return fss != null ? fss : new FileSavedState();
		}
	}

    /**
     * Caller should handle the 0 return value
     *
     * @param msgId
     * @param mFile
     * @param sent
     * @return
     */
	public int getFTProgress(long msgId, File mFile, boolean sent)
	{
		// TODO get file saved state from http manager request token
		FileSavedState fss;
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				int pr = obj.getProgressPercentage();
				Log.d("FTM", "progress : " + pr);
				return pr;
			}
		}

		if (sent)
			fss = getUploadFileState(msgId, mFile);
		else
			fss = getDownloadFileState(msgId, mFile);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.ERROR)
		{
			if (fss.getTotalSize() > 0)
			{
				long temp = fss.getTransferredSize();
				temp *= 100;
				temp /= fss.getTotalSize();
				return (int) temp;
			}

			else
				return 0;
		}
		else if (fss.getFTState() == FTState.COMPLETED)
		{
			return 100;
		}
		else
			return 0;
	}

    /**
     *
     * @param msgId
     * @return
     */
	public int getChunkSize(long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return obj.getChunkSize();
			}
		}
		return 0;
	}

    /**
     *
     * @param animatedProgress
     * @param msgId
     */
	public void setAnimatedProgress(int animatedProgress, long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				obj.setAnimatedProgress(animatedProgress);
			}
		}
	}

    /**
     *
     * @param msgId
     * @return
     */
	public int getAnimatedProgress(long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FileTransferBase obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return obj.getAnimatedProgress();
			}
		}
		return 0;
	}
}
