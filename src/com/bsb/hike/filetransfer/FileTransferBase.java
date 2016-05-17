package com.bsb.hike.filetransfer;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.RequestToken;

import java.io.File;

public class FileTransferBase
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

	protected static String ETAG = "Etag";

	protected ConvMessage userContext = null;

	protected Context context;

	// this will be used for filename in download and upload both
	protected File mFile;

	protected String fileKey; // this is used for download from server , and in upload too

	protected int fileSize;

	protected long msgId;

	protected HikeFileType hikeFileType;

	protected RequestToken requestToken;

	protected int chunkSize;

	protected final int DEFAULT_CHUNK_SIZE = 100 * 1024;

	protected Handler handler;

	protected FileTransferBase(Context ctx, File destinationFile, long msgId, HikeFileType hikeFileType)
	{
		this.context = ctx;
		this.mFile = destinationFile;
		this.msgId = msgId;
		this.hikeFileType = hikeFileType;
		handler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
	}

	public Object getUserContext()
	{
		return userContext;
	}

	public int getChunkSize()
	{
		if (requestToken != null)
			return requestToken.getChunkSize();
		return 0;
	}

	public int getAnimatedProgress()
	{
		FileSavedState state = getFileSavedState();
		if (state == null)
		{
			return 0;
		}
		return state.getAnimatedProgress();
	}

	public void setAnimatedProgress(int s)
	{
		FileSavedState state = getFileSavedState();
		if (state != null)
		{
			state.setAnimatedProgress(s);
		}
	}

	public FileSavedState getFileSavedState()
	{
		if (requestToken == null || !requestToken.isRequestRunning())
		{
			return new FileSavedState(FTState.INITIALIZED, 0, 0, 0);
		}
		return requestToken.getState();
	}

	public int getProgressPercentage()
	{
		FileSavedState state = getFileSavedState();
		if (state == null)
		{
			return 0;
		}

		if (state.getTotalSize() == 0)
		{
			return 0;
		}
		return (int) ((state.getTransferredSize() * 100) / state.getTotalSize());
	}

	public void pause()
	{
		if (requestToken != null)
		{
			requestToken.pause();
		}
	}

	public void cancel()
	{
		if (requestToken != null)
		{
			requestToken.cancel();
		}
	}
}