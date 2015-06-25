package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public abstract class FileTransferBase implements Callable<FTResult>
{
	public enum FTState
	{
		NOT_STARTED, INITIALIZED, IN_PROGRESS, // DOWNLOADING OR UPLOADING
		PAUSED, CANCELLED, COMPLETED, ERROR
	}

	protected static String NETWORK_ERROR_1 = "timed out";

	protected static String NETWORK_ERROR_2 = "Unable to resolve host";

	protected static String NETWORK_ERROR_3 = "Network is unreachable";
	
	protected static int RESPONSE_OK = 200;
	
	protected static int RESPONSE_ACCEPTED = 201;
	
	protected static int RESPONSE_BAD_REQUEST = 400;
	
	protected static int RESPONSE_NOT_FOUND = 404;
	
	protected static int INTERNAL_SERVER_ERROR = 500;

	protected String token;

	protected String uId;

	protected static String ETAG = "Etag";

	protected boolean retry = true; // this will be used when network fails and you have to retry

	protected short retryAttempts = 0;

	protected short MAX_RETRY_ATTEMPTS = 10;

	protected int reconnectTime = 0;

	protected int MAX_RECONNECT_TIME = 20; // in seconds

	protected Handler handler;

	protected int progressPercentage;

	protected Object userContext = null;

	protected Context context;

	// this will be used for filename in download and upload both
	protected File mFile;

	protected String fileKey; // this is used for download from server , and in upload too
	
	protected int fileSize;

	protected URL mUrl;

	protected File stateFile; // this represents state file in which file state will be saved

	protected volatile FTState _state;

	protected long msgId;

	protected HikeFileType hikeFileType;

	protected volatile int _totalSize = 0;

	protected volatile int _bytesTransferred = 0;

	protected int chunkSize = 0;
	
	protected volatile Thread mThread = null;

	protected ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap;
	
	protected int pausedProgress ;

	protected FTAnalyticEvents analyticEvents;

	protected final int DEFAULT_CHUNK_SIZE = 100 * 1024;

	protected FileTransferBase(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, File destinationFile, long msgId, HikeFileType hikeFileType)
	{
		this.handler = handler;
		this.mFile = destinationFile;
		this.msgId = msgId;
		this.hikeFileType = hikeFileType;
		context = ctx;
		this.fileTaskMap = fileTaskMap;
	}
	
	protected FileTransferBase(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, File destinationFile, long msgId, HikeFileType hikeFileType, String token, String uId)
	{
		this.handler = handler;
		this.mFile = destinationFile;
		this.msgId = msgId;
		this.hikeFileType = hikeFileType;
		context = ctx;
		this.fileTaskMap = fileTaskMap;
		this.token = token;
		this.uId = uId;
	}

	protected void setFileTotalSize(int ts)
	{
		_totalSize = ts;
	}

	// this will be used for both upload and download
	protected void incrementBytesTransferred(int value)
	{
		_bytesTransferred += value;
	}

	protected void setBytesTransferred(int value)
	{
		_bytesTransferred = value;
	}
	
	protected void saveIntermediateProgress(String uuid)
	{
		saveFileState(FTState.ERROR, uuid, null);
	}

	protected void saveFileState(String uuid)
	{
		saveFileState(uuid, null);
	}

	protected void saveFileState(String uuid, JSONObject response)
	{
		saveFileState(_state, uuid, response);
	}
	
	private void saveFileState(FTState state, String uuid, JSONObject response)
	{
		FileSavedState fss = new FileSavedState(state, _totalSize, _bytesTransferred, uuid, response);
		try
		{
			FileOutputStream fileOut = new FileOutputStream(stateFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(fss);
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			out.close();
			fileOut.close();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
	}
	
	protected void saveFileState(File stateFile, FTState state, String uuid, JSONObject response)
	{
		FileSavedState fss = new FileSavedState(state, _totalSize, _bytesTransferred, uuid, response);
		try
		{
			FileOutputStream fileOut = new FileOutputStream(stateFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(fss);
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			out.close();
			fileOut.close();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
	}
	
	protected void saveFileKeyState(File stateFile, String mFileKey)
	{
		FileSavedState fss = new FileSavedState(_state, mFileKey);
		try
		{
			FileOutputStream fileOut = new FileOutputStream(stateFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(fss);
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			out.close();
			fileOut.close();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
	}
	
	protected void saveFileKeyState(String mFileKey)
	{
		FileSavedState fss = new FileSavedState(_state, mFileKey);
		try
		{
			FileOutputStream fileOut = new FileOutputStream(stateFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(fss);
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			out.close();
			fileOut.close();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
	}

	protected void deleteStateFile()
	{
		deleteStateFile(stateFile);
	}
	
	protected void deleteStateFile(File file)
	{
		if (file != null && file.exists())
			file.delete();
	}

	protected void setState(FTState mState)
	{
		// if state is completed we will not change it '
		if (!mState.equals(FTState.COMPLETED))
			_state = mState;
	}

	protected boolean shouldRetry()
	{
		if (retry && retryAttempts < MAX_RETRY_ATTEMPTS)
		{
			// make first attempt within first 5 seconds
			if (reconnectTime == 0)
			{
				Random random = new Random();
				reconnectTime = random.nextInt(5) + 1;
			}
			else
			{
				reconnectTime *= 2;
			}
			reconnectTime = reconnectTime > MAX_RECONNECT_TIME ? MAX_RECONNECT_TIME : reconnectTime;
			try
			{
				Thread.sleep(reconnectTime * 1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.d(getClass().getSimpleName(),"Sleep interrupted: " + mThread.toString());
				e.printStackTrace();
			}
			retryAttempts++;
			Logger.d(getClass().getSimpleName(), "FTR retry # : " + retryAttempts + " for msgId : " + msgId);
			return true;
		}
		else
		{
			retryAttempts++;
			Logger.d(getClass().getSimpleName(), "Returning false on retry attempt No. " + retryAttempts);
			return false;
		}
	}
	
	Thread getThread()
	{
		return mThread;
	}
	
	protected void setChunkSize()
	{
		NetworkType networkType = FileTransferManager.getInstance(context).getNetworkType();
		if (Utils.scaledDensityMultiplier > 1)
			chunkSize = networkType.getMaxChunkSize();
		else if (Utils.scaledDensityMultiplier == 1)
			chunkSize = networkType.getMinChunkSize() * 2;
		else
			chunkSize = networkType.getMinChunkSize();
		//chunkSize = NetworkType.WIFI.getMaxChunkSize();

		try
		{
			long mem = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
			if (chunkSize > (int) (mem / 8))
				chunkSize = (int) (mem / 8);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected URLConnection initConn() throws IOException
	{
		URLConnection conn = (HttpURLConnection) mUrl.openConnection();
		if (AccountUtils.ssl)
		{
			((HttpsURLConnection) conn).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
		}
		AccountUtils.addUserAgent(conn);
		AccountUtils.setNoTransform(conn);;
		return conn;
	}
	
	public Object getUserContext()
	{
		return userContext;
	}
	
	public int getPausedProgress()
	{
		return this.pausedProgress;
	}

	public void setPausedProgress(int pausedProgress)
	{
		this.pausedProgress = pausedProgress;
	}
}
