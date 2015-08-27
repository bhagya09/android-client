package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.hikehttp.hostnameverifier.HikeHostNameVerifier;
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

	public enum FTExceptionReason
	{
		NO_EXCEPTION, UNKNOWN_HOST, SOCKET_EXCEPTION, SOCKET_TIMEOUT, CONNECT_TIMEOUT, HOST_CONNECT_EXCEPTION, CONNECT_EXCEPTION
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

	protected short MAX_RETRY_ATTEMPTS = 3;

	protected int reconnectTime = 0;

	protected int MAX_RECONNECT_TIME = 8; // in seconds

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

	protected volatile FTExceptionReason mExceptionType = FTExceptionReason.NO_EXCEPTION;
	protected int animatedProgress = 0;

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
		FileSavedState fss = new FileSavedState(state, _totalSize, _bytesTransferred, uuid, response, animatedProgress);
		writeToFile(fss, stateFile);
	}
	
	protected void saveFileState(File stateFile, FTState state, String uuid, JSONObject response)
	{
		FileSavedState fss = new FileSavedState(state, _totalSize, _bytesTransferred, uuid, response, animatedProgress);
		writeToFile(fss, stateFile);
	}
	
	protected void saveFileKeyState(File stateFile, String mFileKey)
	{
		FileSavedState fss = new FileSavedState(_state, mFileKey, animatedProgress);
		writeToFile(fss, stateFile);
	}
	
	protected void saveFileKeyState(String mFileKey)
	{
		FileSavedState fss = new FileSavedState(_state, mFileKey, animatedProgress);
		writeToFile(fss, stateFile);
	}

	private void writeToFile(FileSavedState fss, File mStateFile)
	{
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try
		{
			fileOut = new FileOutputStream(mStateFile);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(fss);
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		finally
		{
			Utils.closeStreams(out, fileOut);
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
			// make first attempt after 1 second
			if (reconnectTime == 0)
			{
				reconnectTime = 1;
			}
			else
			{
				// increase wait time exponentially on next retries.
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
			HikeHostNameVerifier hostVerifier = new HikeHostNameVerifier();
			hostVerifier.setFtHostIps(FileTransferManager.getInstance(context).getFTHostUris());
			((HttpsURLConnection) conn).setHostnameVerifier(hostVerifier);
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

	public void handleException(Throwable e)
	{
		if(e instanceof UnknownHostException)
			mExceptionType = FTExceptionReason.UNKNOWN_HOST;
		else if(e instanceof SocketException)
			mExceptionType = FTExceptionReason.SOCKET_EXCEPTION;
		else if(e instanceof SocketTimeoutException)
			mExceptionType = FTExceptionReason.SOCKET_TIMEOUT;
		else if(e instanceof ConnectException)
			mExceptionType = FTExceptionReason.CONNECT_EXCEPTION;
		else if(e instanceof ConnectTimeoutException)
			mExceptionType = FTExceptionReason.CONNECT_TIMEOUT;
		else if(e instanceof HttpHostConnectException)
			mExceptionType = FTExceptionReason.HOST_CONNECT_EXCEPTION;
	}

	public URL getUpdatedURL(URL mUrl, String logText, String taskType, URL baseUrl)
	{
		URL resultUrl = mUrl;
		switch (mExceptionType) {
			case UNKNOWN_HOST:
			case HOST_CONNECT_EXCEPTION:
			case CONNECT_EXCEPTION:
			case CONNECT_TIMEOUT:
			case SOCKET_EXCEPTION:
			case SOCKET_TIMEOUT:
				try {
					String host = FileTransferManager.getInstance(context).getHost();
					resultUrl = new URL(mUrl.getProtocol(), host, mUrl.getPort(), mUrl.getFile());
					Logger.d("FileTransferBase", logText + " , fallback host = " + host);
					FTAnalyticEvents.logDevError(FTAnalyticEvents.HOST_FALLBACK, 0, taskType, logText, "Fallback Host : " + host);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			default:
				break;
		}
		return resultUrl;
	}
}
