package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends FileTransferBase
{
	public static final String FILE_TOO_LARGE_ERROR_MESSAGE = "file_too_large_error";

	private File tempDownloadedFile;

	public DownloadFileTask(Context ctx, File tempFile, File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, ConvMessage userContext)
	{
		super(ctx, destinationFile, msgId, hikeFileType);
		this.tempDownloadedFile = tempFile;
		this.fileKey = fileKey;
		this.userContext = userContext;
	}

	public void download()
	{
		IRequestListener downlaodFileRequestListener = getDownloadRequestListener();

		String downLoadUrl = null;
		if (userContext != null)
		{
			ConvMessage msg = (ConvMessage) userContext;
			HikeFile hikeFile = msg.getMetadata().getHikeFiles().get(0);
			if (hikeFile != null)
			{
				downLoadUrl = hikeFile.getDownloadURL();
			}
		}
		if (TextUtils.isEmpty(downLoadUrl))
		{
			downLoadUrl = AccountUtils.fileTransferBaseDownloadUrl + fileKey;
		}

		if (requestToken == null)
		{
			requestToken = HttpRequests.downloadFile(tempDownloadedFile.getAbsolutePath(), downLoadUrl, msgId, downlaodFileRequestListener, chunkSizePolicy);
		}
		requestToken.execute();
	}

	public IRequestListener getDownloadRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				FileTransferManager.getInstance(context).removeTask(msgId);
				String md5Hash = null;
				for (Header h : result.getHeaders())
				{
					String name = h.getName();
					if (name.equals(ETAG))
					{
						md5Hash = h.getValue();
					}
				}
				doOnSuccess(md5Hash);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure(httpException);
				FileTransferManager.getInstance(context).removeTask(msgId);
			}
		};
	}

	private void doOnSuccess(String md5Hash)
	{
		String file_md5Hash = Utils.fileToMD5(tempDownloadedFile.getPath());
		if (md5Hash != null)
		{
			Logger.d(getClass().getSimpleName(), "Phone's md5 : " + file_md5Hash);
			if (!md5Hash.equals(file_md5Hash))
			{
				Logger.d(getClass().getSimpleName(), "The md5's are not equal...Deleting the files...");
				sendCrcLog(file_md5Hash);
			}

		}
		else
		{
			sendCrcLog(file_md5Hash);
		}

		boolean isFileMoved = tempDownloadedFile.renameTo(mFile);
		/*
		 * File.RenameTo() is platform dependent and relies on a few conditions to be met in order to successfully rename a file. So adding fall back to move the file.
		 */
		if (!isFileMoved)
		{
			// FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "RENAME_FAILED");
			isFileMoved = Utils.moveFile(tempDownloadedFile, mFile);
		}
		if (!isFileMoved) // if failed
		{
			Logger.d(getClass().getSimpleName(), "FT failed");
			// error();
			// FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "READ_FAIL");
			// TODO check what to do here?? return FTResult.READ_FAIL;
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "FT Completed");
			try
			{
				// Added sleep to complete the progress.
				// TODO Need to remove sleep and implement in a better way to achieve the progress UX.
				Thread.sleep(300);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			// deleteStateFile();
		}

		if (mFile != null)
		{
			if (mFile.exists() && hikeFileType != HikeFileType.AUDIO_RECORDING)
			{
				// this is to refresh media library
				context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
			}
			if (HikeFileType.IMAGE == hikeFileType)
				HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_FILE_DOWNLOADED, (ConvMessage) userContext);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	private void sendCrcLog(String md5)
	{
		Utils.sendMd5MismatchEvent(mFile.getName(), fileKey, md5, tempDownloadedFile.length(), true);
	}

	private void doOnFailure(HttpException httpException)
	{
		Throwable ex = httpException.getCause();
		if (ex instanceof FileNotFoundException)
		{
			Toast.makeText(context, R.string.no_sd_card, Toast.LENGTH_SHORT).show();
		}
		else if (ex instanceof IOException && ex.getMessage().equals(FILE_TOO_LARGE_ERROR_MESSAGE))
		{
			Toast.makeText(context, R.string.not_enough_space, Toast.LENGTH_SHORT).show();
		}
		else {
			int errorCode = httpException.getErrorCode();
			switch (errorCode)
			{
				case HttpException.REASON_CODE_CANCELLATION:
					deleteTempFile();
					// TODO
					// deleteStateFile();
					// FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_STATE_CHANGE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "state", "CANCELLED");
					Toast.makeText(context, R.string.download_cancelled, Toast.LENGTH_SHORT).show();
				case HttpException.REASON_CODE_MALFORMED_URL:
					Logger.e(getClass().getSimpleName(), "Invalid URL");
					Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
					break;
				default:
					if (errorCode / 100 != 2)
					{
						Toast.makeText(context, R.string.file_expire, Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
					}
					break;
			}
		}
		// TODO
		// result == FTResult.FAILED_UNRECOVERABLE ? R.string.download_failed_fatal
		// result == FTResult.CARD_UNMOUNT ? R.string.card_unmount

		if (mFile != null)
		{
			mFile.delete();
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);

	}

	private void deleteTempFile()
	{
		if (tempDownloadedFile != null && tempDownloadedFile.exists())
			tempDownloadedFile.delete();
	}
}
