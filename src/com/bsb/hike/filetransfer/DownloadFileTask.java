package com.bsb.hike.filetransfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileTransferChunkSizePolicy;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DownloadFileTask extends FileTransferBase
{
	public static final String FILE_TOO_LARGE_ERROR_MESSAGE = "file_too_large_error";

	public static final String CARD_UNMOUNT_ERROR = "card_mount_error";

	private File tempDownloadedFile;

	private boolean showToast;

	private String downLoadUrl;

	protected DownloadFileTask(Context ctx, File tempFile, File destinationFile, String fileKey, long msgId, HikeFileType hikeFileType, ConvMessage userContext, boolean showToast)
	{
		super(ctx, destinationFile, msgId, hikeFileType);
		this.fileKey = fileKey;
		this.tempDownloadedFile = tempFile;
		this.showToast = showToast;
		this.userContext = userContext;
	}

	public void download()
	{
		IRequestListener downloadFileRequestListener = getDownloadRequestListener();

		downLoadUrl = null;

		HikeFile hikeFile = null;
		if (userContext == null)
		{
			JSONObject jo;
			try
			{
				jo = new JSONObject(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_JSON, "{}"));
				hikeFile = new HikeFile(jo, false);
			}
			catch (JSONException e)
			{
				FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_INIT_2_1, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "JSONException", "DOWNLOAD_FAILED : " , e);
				removeTaskAndShowToast(HikeConstants.FTResult.DOWNLOAD_FAILED);
				Logger.e("DownloadFileTask", "Json exception while creating hike file object : ", e);
				return;
			}
		}
		else
		{
			hikeFile = userContext.getMetadata().getHikeFiles().get(0);
		}

		String fileTypeString = "";

		if (hikeFile != null)
		{
			downLoadUrl = hikeFile.getDownloadURL();
			fileTypeString = hikeFile.getFileTypeString();
		}

		if (TextUtils.isEmpty(downLoadUrl))
		{
			downLoadUrl = AccountUtils.fileTransferBaseDownloadUrl + fileKey;
		}

		requestToken = HttpRequests.downloadFile(tempDownloadedFile.getAbsolutePath(), downLoadUrl, msgId, downloadFileRequestListener,
					new FileTransferChunkSizePolicy(context), fileTypeString);
		requestToken.execute();
	}

	public IRequestListener getDownloadRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				String md5Hash = null;
				for (Header h : result.getHeaders())
				{
					String name = h.getName();
					if (ETAG.equals(name))
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
			}
		};
	}

	private void doOnSuccess(String md5Hash)
	{
		if (getFileSavedState().getFTState() == FTState.PAUSED)
		{
			return;
		}

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
			FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "RENAME_FAILED");
			isFileMoved = Utils.moveFile(tempDownloadedFile, mFile);
		}
		if (!isFileMoved) // if failed
		{
			Logger.d(getClass().getSimpleName(), "FT failed");
			FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_RENAME_FILE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "READ_FAIL");
			removeTaskAndShowToast(HikeConstants.FTResult.READ_FAIL);
			return;
		}
		else
		{
			Logger.d(getClass().getSimpleName(), "FT Completed");
			getFileSavedState().setFTState(FTState.COMPLETED);
			HttpManager.getInstance().deleteRequestStateFromDB(downLoadUrl, String.valueOf(msgId));
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
			if (HikeFileType.APK == hikeFileType)
			{
				FTApkManager.checkAndActOnDownloadedApk(mFile);
			}
		}
		FileTransferManager.getInstance(context).removeTask(msgId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);

		FileTransferManager.getInstance(context).logTaskCompletedAnalytics(msgId, userContext, true);
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
			FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_INIT_1_3, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "NO_SD_CARD : ", ex);
			removeTaskAndShowToast(HikeConstants.FTResult.NO_SD_CARD);
		}
		else if (ex instanceof IOException && FILE_TOO_LARGE_ERROR_MESSAGE.equals(ex.getMessage()))
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_MEM_CHECK, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "FILE_TOO_LARGE");
			removeTaskAndShowToast(HikeConstants.FTResult.FILE_TOO_LARGE);
		}
		else if (ex instanceof IOException && CARD_UNMOUNT_ERROR.equals(ex.getMessage()))
		{
			FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_DATA_WRITE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "file", "CARD_UNMOUNT : ", ex);
			removeTaskAndShowToast(HikeConstants.FTResult.CARD_UNMOUNT);
		}
		else
		{
			int errorCode = httpException.getErrorCode();
			switch (errorCode)
			{
				case HttpException.REASON_CODE_NO_NETWORK:
					FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_CONN_INIT_2_1, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "http", "DOWNLOAD_FAILED : No Internet");
					removeTaskAndShowToast(HikeConstants.FTResult.DOWNLOAD_FAILED);
					break;
				case HttpException.REASON_CODE_CANCELLATION:
					deleteTempFile();
					HttpManager.getInstance().deleteRequestStateFromDB(downLoadUrl, String.valueOf(msgId));
					FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_STATE_CHANGE, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "state", "CANCELLED");
					removeTaskAndShowToast(HikeConstants.FTResult.CANCELLED);
				case HttpException.REASON_CODE_MALFORMED_URL:
					Logger.e(getClass().getSimpleName(), "Invalid URL");
					FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_INIT_2_1, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "UrlCreation", "DOWNLOAD_FAILED : " , ex);
					removeTaskAndShowToast(HikeConstants.FTResult.DOWNLOAD_FAILED);
					break;
				default:
					if (errorCode / 100 != 2)
					{
						FTAnalyticEvents.logDevError(FTAnalyticEvents.DOWNLOAD_CONN_INIT_2_2, errorCode, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "http", "SERVER_ERROR");
						removeTaskAndShowToast(HikeConstants.FTResult.SERVER_ERROR);
					}
					else
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.DOWNLOAD_UNKNOWN_ERROR, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "all", "DOWNLOAD_FAILED", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.DOWNLOAD_FAILED);
					}
					break;
			}
		}
	}

	private void deleteTempFile()
	{
		if (tempDownloadedFile != null && tempDownloadedFile.exists())
			tempDownloadedFile.delete();
	}

	@Override
	public FileSavedState getFileSavedState()
	{
		FileSavedState fss = super.getFileSavedState();
		if (fss == null)
		{
			fss = HttpManager.getInstance().getRequestStateFromDB(downLoadUrl, String.valueOf(msgId));
		}
		return fss != null ? fss : new FileSavedState();
	}

	private void removeTaskAndShowToast(final HikeConstants.FTResult result)
	{
		FileTransferManager.getInstance(context).removeTask(msgId);
		if (showToast) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					switch (result) {
						case UPLOAD_FAILED:
							Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
							break;
						case CARD_UNMOUNT:
							Toast.makeText(context, R.string.card_unmount, Toast.LENGTH_SHORT).show();
							break;
						case READ_FAIL:
							Toast.makeText(context, R.string.unable_to_read, Toast.LENGTH_SHORT).show();
							break;
						case DOWNLOAD_FAILED:
							Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
							break;
						case FILE_SIZE_EXCEEDING:
							Toast.makeText(context, R.string.max_file_size, Toast.LENGTH_SHORT).show();
							break;
						case CANCELLED:
							Toast.makeText(context, R.string.download_cancelled, Toast.LENGTH_SHORT).show();
							break;
						case NO_SD_CARD:
							Toast.makeText(context, R.string.no_sd_card, Toast.LENGTH_SHORT).show();
							break;
						case FILE_TOO_LARGE:
							Toast.makeText(context, R.string.not_enough_space, Toast.LENGTH_SHORT).show();
							break;
						case SERVER_ERROR:
							Toast.makeText(context, R.string.file_expire, Toast.LENGTH_SHORT).show();
							break;
					}
				}
			});
		}
		if (mFile != null)
		{
			mFile.delete();
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}
}
