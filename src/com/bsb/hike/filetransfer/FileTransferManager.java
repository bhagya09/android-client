package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/* 
 * This manager will manage the upload and download (File Transfers).
 * A general thread pool is maintained which will be used for both downloads and uploads.
 * The manager will run on main thread hence an executor is used to delegate task to thread pool threads.
 */
public class FileTransferManager extends BroadcastReceiver
{
	private final Context context;

	private final ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap;

	private String HIKE_TEMP_DIR_NAME = "hikeTmp";

	// Constant variables
	private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

	private final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

	private final short KEEP_ALIVE_TIME = 60; // in seconds

	private static int minChunkSize = 8 * 1024;

	private static int maxChunkSize = 128 * 1024;
	
	private final int taskLimit;
	
	private final int TASK_OVERFLOW_LIMIT = 90;

	private final ExecutorService pool;

	private static volatile FileTransferManager _instance = null;

	private SharedPreferences settings;

	private final Handler handler;

	public static String FT_CANCEL = "ft_cancel";

	public static String READ_FAIL = "read_fail";

	public static String UPLOAD_FAILED = "upload_failed";

	public static String UNABLE_TO_DOWNLOAD = "unable_to_download";

	private List<String> ftHostURIs = null;

	public static final int FAKE_PROGRESS_DURATION = 8 * 1000;

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
	};

	private class MyThreadFactory implements ThreadFactory
	{
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r)
		{
			int threadCount = threadNumber.getAndIncrement();
			Thread t = new Thread(r);
			// This approach reduces resource competition between the Runnable object's thread and the UI thread.
			t.setPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE + android.os.Process.THREAD_PRIORITY_BACKGROUND);
			t.setName("FT Thread-" + threadCount);
			Logger.d(getClass().getSimpleName(), "Running FT thread : " + t.getName());
			return t;
		}
	}

	private class MyFutureTask extends FutureTask<FTResult>
	{
		private FileTransferBase task;

		public MyFutureTask(FileTransferBase callable)
		{
			super(callable);
			this.task = callable;
		}

		private FileTransferBase getTask()
		{
			return task;
		}

		@Override
		public void run()
		{
			Logger.d(getClass().getSimpleName(), "TimeCheck: Starting time : " + System.currentTimeMillis());
			super.run();
		}

		@Override
		protected void done()
		{
			super.done();
			FTResult result = FTResult.UPLOAD_FAILED;
			try
			{
				result = this.get();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				e.printStackTrace();
			}

			if(task._state == FTState.COMPLETED)
			{
				HikeFile hikefile = ((ConvMessage) task.userContext).getMetadata().getHikeFiles().get(0);
				FTAnalyticEvents analyticEvent = FTAnalyticEvents.getAnalyticEvents(getAnalyticFile(hikefile.getFile(), task.msgId));
				String network = analyticEvent.mNetwork + "/" + getNetworkTypeString();
				analyticEvent.sendFTSuccessFailureEvent(network, hikefile.getFileSize(), FTAnalyticEvents.FT_SUCCESS);
				deleteLogFile(task.msgId, hikefile.getFile());
			}
			if (task instanceof DownloadFileTask)
				((DownloadFileTask) task).postExecute(result);
			else if (task instanceof UploadFileTask)
				((UploadFileTask) task).postExecute(result);
			else
				((UploadContactOrLocationTask) task).postExecute(result);

			Logger.d(getClass().getSimpleName(), "TimeCheck: Exiting  time : " + System.currentTimeMillis());
		}
	}

	private FileTransferManager(Context ctx)
	{
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		fileTaskMap = new ConcurrentHashMap<Long, FutureTask<FTResult>>();
		// here choosing TimeUnit in seconds as minutes are added after api level 9
		pool = new ThreadPoolExecutor(2, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue, new MyThreadFactory());
		context = ctx;
		handler = new Handler(context.getMainLooper());
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(this, filter);
		taskLimit = context.getResources().getInteger(R.integer.ft_limit);
		setFThostURIs();
	}
	

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

	public boolean isFileTaskExist(long msgId)
	{
		return fileTaskMap.containsKey(msgId);
	}

	public ConvMessage getMessage(long msgId)
	{
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			Object msg = ((MyFutureTask) obj).getTask().getUserContext();
			if (msg != null)
			{
				return ((ConvMessage) msg);
			}
		}
		return null;
	}

	public void downloadFile(File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, ConvMessage userContext, boolean showToast)
	{
		if (isFileTaskExist(msgId)){
			validateFilePauseState(msgId);
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		DownloadFileTask task = new DownloadFileTask(handler, fileTaskMap, context, destinationFile, fileKey, msgId, hikeFileType, userContext, showToast, token, uId);
		try
		{
			MyFutureTask ft = new MyFutureTask(task);
			fileTaskMap.put(msgId, ft);
			pool.execute(ft); // this future is used to cancel pause the task
		}
		catch (RejectedExecutionException rjEx)
		{
			// handle this properly
		}

	}

	public void uploadFile(ConvMessage convMessage, String fileKey)
	{
		if (isFileTaskExist(convMessage.getMsgID())){
			validateFilePauseState(convMessage.getMsgID());
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, convMessage, fileKey);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void uploadFile(List<ContactInfo> contactList, List<ConvMessage> messageList, String fileKey)
	{
		ConvMessage convMessage = messageList.get(0);
		if (isFileTaskExist(convMessage.getMsgID())){
			validateFilePauseState(convMessage.getMsgID());
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadFileTask task = new UploadFileTask(handler, fileTaskMap, context, token, uId, contactList, messageList, fileKey);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}
	
	public void uploadContactOrLocation(ConvMessage convMessage, boolean uploadingContact)
	{
		if (isFileTaskExist(convMessage.getMsgID())){
			validateFilePauseState(convMessage.getMsgID());
			return;
		}
		if(taskOverflowLimitAchieved())
			return;
		
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
		UploadContactOrLocationTask task = new UploadContactOrLocationTask(handler, fileTaskMap, context, convMessage, uploadingContact, token, uId);
		MyFutureTask ft = new MyFutureTask(task);
		task.setFutureTask(ft);
		pool.execute(ft);
	}

	public void removeTask(long msgId)
	{
		fileTaskMap.remove(msgId);
	}

	/*
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		fileTaskMap.clear();
		pool.shutdown();
		deleteAllFTRFiles();
		_instance = null;
	}

	public void cancelTask(long msgId, File mFile, boolean sent, long fileSize)
	{
		FileSavedState fss;
		if (sent)
			fss = getUploadFileState(msgId, mFile);
		else
			fss = getDownloadFileState(msgId, mFile);

		if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.INITIALIZED)
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				((MyFutureTask) obj).getTask().setState(FTState.CANCELLED);
			}
			Logger.d(getClass().getSimpleName(), "deleting state file" + msgId);
			deleteStateFile(msgId, mFile);
			if (!sent)
			{
				Logger.d(getClass().getSimpleName(), "deleting tempFile" + msgId);
				File tempDownloadedFile = new File(getHikeTempDir(), mFile.getName() + ".part");
				if (tempDownloadedFile != null && tempDownloadedFile.exists())
					tempDownloadedFile.delete();

			}
			FTAnalyticEvents analyticEvent = FTAnalyticEvents.getAnalyticEvents(getAnalyticFile(mFile, msgId));
			String network = analyticEvent.mNetwork + "/" + getNetworkTypeString();
			analyticEvent.sendFTSuccessFailureEvent(network, fileSize, FTAnalyticEvents.FT_FAILED);
			deleteLogFile(msgId, mFile);
		}
	}

	public void pauseTask(long msgId)
	{
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			FileTransferBase task = ((MyFutureTask) obj).getTask();
			task.setPausedProgress(task._bytesTransferred);
			task.setState(FTState.PAUSED);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			task.analyticEvents.mPauseCount += 1;
			Logger.d(getClass().getSimpleName(), "pausing the task....");
		}
	}

	// this will be used when user deletes corresponding chat bubble
	public void deleteStateFile(long msgId, File mFile)
	{
		String fName = mFile.getName() + ".bin." + msgId;
		File f = new File(getHikeTempDir(), fName);
		if (f != null)
			f.delete();
	}

	// this will be used when user deletes corresponding chat bubble
	public void deleteLogFile(long msgId, File mFile)
	{
		String fName = mFile.getName() + ".log." + msgId;
		File f = new File(getHikeTempDir(), fName);
		if (f != null)
			f.delete();
	}

	// this will be used when user deletes account or unlink account
	public void deleteAllFTRFiles()
	{
		if (getHikeTempDir() != null && getHikeTempDir().listFiles() != null)
			for (File f : getHikeTempDir().listFiles())
			{
				if (f != null)
				{
					try
					{
						f.delete();
					}
					catch (Exception e)
					{
						Logger.e(getClass().getSimpleName(), "Exception while deleting state file : ", e);
					}
				}
			}
	}

	// this function gives the state of downloading for a file
	public FileSavedState getDownloadFileState(long msgId, File mFile)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred, 
						((MyFutureTask) obj).getTask().animatedProgress);
			}
			else
			{
				return new FileSavedState(FTState.IN_PROGRESS, 0, 0, 0);
			}
		}
		else
			return getDownloadFileState(mFile, msgId);
	}

	/*
	 * here mFile is the file provided by the caller (original file) null : represents unhandled error and should be handled accordingly
	 */
	public FileSavedState getDownloadFileState(File mFile, long msgId)
	{
		if (mFile == null) // @GM only for now. Has to be handled properly
			return new FileSavedState();

		FileSavedState fss = null;
		if (mFile.exists())
		{
			fss = new FileSavedState(FTState.COMPLETED, 100, 100, 100);
		}
		else
		{
			FileInputStream fileIn = null;
			ObjectInputStream in = null;
			try
			{
				String fName = mFile.getName() + ".bin." + msgId;
				File f = new File(getHikeTempDir(), fName);
				if (!f.exists())
					return new FileSavedState();
				fileIn = new FileInputStream(f);
				in = new ObjectInputStream(fileIn);
				fss = (FileSavedState) in.readObject();
				if(fss.getAnimatedProgress() > 0)
					setAnimatedProgress(fss.getAnimatedProgress(), msgId);
			}
			catch (IOException i)
			{
				i.printStackTrace();
				FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File", "Reading download state failed", i);
			}
			catch (ClassNotFoundException e)
			{
				FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File", "Reading download state failed", e);
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Logger.e(getClass().getSimpleName(), "Exception while reading state file : ", e);
				FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File", "Reading download state failed", e);
			}
			finally
			{
				Utils.closeStreams(in, fileIn);
			}
		}
		return fss != null ? fss : new FileSavedState();
	}

	public void setAnimatedProgress(int animatedProgress, long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				((MyFutureTask) obj).getTask().animatedProgress = animatedProgress;
			}
		}
	}

	public int getAnimatedProgress(long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return ((MyFutureTask) obj).getTask().animatedProgress;
			}
		}
		return 0;
	}

	// this function gives the state of uploading for a file
	public FileSavedState getUploadFileState(long msgId, File mFile)
	{
		Logger.d(getClass().getSimpleName(), "Returning state for message ID : " + msgId);
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				Logger.d(getClass().getSimpleName(), "Returning: " + ((MyFutureTask) obj).getTask()._state.toString());
				return new FileSavedState(((MyFutureTask) obj).getTask()._state, ((MyFutureTask) obj).getTask()._totalSize, ((MyFutureTask) obj).getTask()._bytesTransferred, 
						((MyFutureTask) obj).getTask().animatedProgress);
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "Returning: in_prog");
				return new FileSavedState(FTState.IN_PROGRESS, 0, 0, 0);
			}
		}
		else
			return getUploadFileState(mFile, msgId);
	}

	public FileSavedState getUploadFileState(File mFile, long msgId)
	{
		Logger.d(getClass().getSimpleName(), "Returning from second call");
		if (mFile == null) // @GM only for now. Has to be handled properly
			return new FileSavedState();

		FileSavedState fss = null;
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		try
		{
			String fName = mFile.getName() + ".bin." + msgId;
			Logger.d(getClass().getSimpleName(), fName);
			File f = new File(getHikeTempDir(), fName);
			if (!f.exists())
			{
				return new FileSavedState();
			}
			fileIn = new FileInputStream(f);
			in = new ObjectInputStream(fileIn);
			fss = (FileSavedState) in.readObject();
			if(fss.getAnimatedProgress() > 0)
				setAnimatedProgress(fss.getAnimatedProgress(), msgId);
		}
		catch (IOException i)
		{
			i.printStackTrace();
			FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", i);
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", e);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception while reading state file : ", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.FT_STATE_READ_FAIL, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "File", "Reading upload state failed", e);
		}
		finally
		{
			Utils.closeStreams(in, fileIn);
		}
		return fss != null ? fss : new FileSavedState();

	}

	// Fetches the type of internet connection the device is using
	public NetworkType getNetworkType()
	{
		
		int networkType = Utils.getNetworkType(context);
		
		switch (networkType)
		{
		case -1 :
			return NetworkType.NO_NETWORK;
		case 0: 
			return NetworkType.TWO_G;
		case 1: 
			return NetworkType.WIFI;
		case 2: 
			return NetworkType.TWO_G;
		case 3: 
			return NetworkType.THREE_G;
		case 4: 
			return NetworkType.FOUR_G;
		default:
			return NetworkType.TWO_G;
		}
	}

	private void setChunkSize(NetworkType networkType)
	{
		maxChunkSize = networkType.getMaxChunkSize();
		minChunkSize = networkType.getMinChunkSize();
	}

	private void resumeAllTasks()
	{
		for (Entry<Long, FutureTask<FTResult>> entry : fileTaskMap.entrySet())
		{
			if (entry != null)
			{
				FutureTask<FTResult> obj = entry.getValue();
				if (obj != null)
				{
					Thread t = ((MyFutureTask) obj).getTask().getThread();
					if (t != null)
					{
						if (t.getState() == State.TIMED_WAITING)
						{
							Logger.d(getClass().getSimpleName(), "interrupting the task: " + t.toString());
							t.interrupt();
						}
					}
				}
			}

		}
	}

	public int getMaxChunkSize()
	{
		return maxChunkSize;
	}

	public int getMinChunkSize()
	{
		return minChunkSize;
	}

	public File getHikeTempDir()
	{
		File hikeDir = context.getExternalFilesDir(null);
		if(hikeDir == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UNABLE_TO_CREATE_HIKE_TEMP_DIR, 0, FTAnalyticEvents.UPLOAD_FILE_TASK + ":" + FTAnalyticEvents.DOWNLOAD_FILE_TASK,
					"File", "Hike dir is null when external storage state is - " + Environment.getExternalStorageState());
			return null;
		}
		File hikeTempDir = new File(hikeDir, HIKE_TEMP_DIR_NAME);
		if(hikeTempDir != null && !hikeTempDir.exists())
		{
			if (!hikeTempDir.mkdirs())
			{
				Logger.d("FileTransferManager", "failed to create directory");
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UNABLE_TO_CREATE_HIKE_TEMP_DIR, 0, FTAnalyticEvents.UPLOAD_FILE_TASK + ":" + FTAnalyticEvents.DOWNLOAD_FILE_TASK,
						"File", "Unable to create Hike temp dir when external storage state is - " + Environment.getExternalStorageState());
				return null;
			}
		}
		return hikeTempDir;
	}

	/**
	 * caller should handle the 0 return value
	 * */

	public int getFTProgress(long msgId, File mFile, boolean sent)
	{
		FileSavedState fss;
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return ((MyFutureTask) obj).getTask().progressPercentage;
			}
		}

		if (sent)
			fss = getUploadFileState(mFile, msgId);
		else
			fss = getDownloadFileState(mFile, msgId);

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

	public int getChunkSize(long msgId)
	{
		if (isFileTaskExist(msgId))
		{
			FutureTask<FTResult> obj = fileTaskMap.get(msgId);
			if (obj != null)
			{
				return ((MyFutureTask) obj).getTask().chunkSize;
			}
		}
		return 0;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			Logger.d(getClass().getSimpleName(), "Connectivity Change Occured");
			// if network available then proceed
			if (Utils.isUserOnline(context))
			{
				NetworkType networkType = getNetworkType();
				setChunkSize(networkType);
				resumeAllTasks();
			}
		}
	}
	
	public int remainingTransfers()
	{
		if(taskLimit > fileTaskMap.size())
			return (taskLimit - fileTaskMap.size());
		else
			return 0;
	}

	public int getTaskLimit()
	{
		return taskLimit;
	}

	public boolean taskOverflowLimitAchieved()
	{
		if(fileTaskMap.size() >= TASK_OVERFLOW_LIMIT)
			return true;
		else
			return false;
	}
	
	private void validateFilePauseState(long msgId){
		FutureTask<FTResult> obj = fileTaskMap.get(msgId);
		if (obj != null)
		{
			FileTransferBase task = ((MyFutureTask) obj).getTask();
			if(task.getPausedProgress() == task._bytesTransferred && task._state == FTState.PAUSED){
				task.setState(FTState.IN_PROGRESS);
				HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			}
		}
	}
	
	public File getAnalyticFile(File file, long  msgId)
	{
		return new File(FileTransferManager.getInstance(context).getHikeTempDir(), file.getName() + ".log." + msgId);
	}

	public String getNetworkTypeString()
	{
		String netTypeString = "n";
		switch (getNetworkType())
		{
			case NO_NETWORK:
				netTypeString = "n";
				break;
			case TWO_G:
				netTypeString = "2g";
				break;
			case THREE_G:
				netTypeString = "3g";
				break;
			case FOUR_G:
				netTypeString = "4g";
				break;
			case WIFI:
				netTypeString = "wifi";
				break;
			default:
				break;
		}
		return netTypeString;
	}

	public void setFThostURIs()
	{
		String ipString = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FT_HOST_IPS, "");
		JSONArray ipArray = null;

		try
		{
			ipArray = new JSONArray(ipString);
		}
		catch (JSONException e)
		{
			Logger.d("UploadFileTask", "Exception while parsing = " + e);
			e.printStackTrace();
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			ftHostURIs = new ArrayList<String>(ipArray.length() + 1);
			int len = ipArray.length();

			ftHostURIs.add(AccountUtils.PRODUCTION_FT_HOST);
			for (int i = 0; i < len; i++)
			{
				if (ipArray.optString(i) != null)
				{
					Logger.d("UploadFileTask", "FT host api[" + i + "] = " + ipArray.optString(i));
					ftHostURIs.add(ipArray.optString(i));
				}
			}
		}
		else
		{
			ftHostURIs = new ArrayList<String>(9);

			ftHostURIs.add(AccountUtils.PRODUCTION_FT_HOST);
			ftHostURIs.add("54.169.191.114");
			ftHostURIs.add("54.169.191.115");
			ftHostURIs.add("54.169.191.116");
			ftHostURIs.add("54.169.191.113");
		}
		HttpManager.setFtHostUris(ftHostURIs);
	}

	public String getHost()
	{
		String host = AccountUtils.PRODUCTION_FT_HOST;
		if(ftHostURIs != null)
		{
			Random random = new Random();
			int index = random.nextInt(ftHostURIs.size() - 1) + 1;
			host = ftHostURIs.get(index);
		}
		return host;
	}

	/**
	 * Returns FT fallback Host
	 * @return List<String>
	 */
	public List<String> getFTHostUris()
	{
		return this.ftHostURIs;
	}
}
