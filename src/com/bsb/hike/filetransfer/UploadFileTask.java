package com.bsb.hike.filetransfer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.ces.CesConstants;
import com.bsb.hike.ces.CustomerExperienceScore;
import com.bsb.hike.ces.ft.FTDataInfoFormatBuilder;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileTransferChunkSizePolicy;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.video.HikeVideoCompressor;
import com.bsb.hike.video.VideoUtilities;
import com.bsb.hike.video.VideoUtilities.VideoEditedInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getFastFileUploadBaseUrl;

public class UploadFileTask extends FileTransferBase
{
	private static final String TAG = "UploadFileTask";

	private String fileType;

	private int mAttachementType;

	private File selectedFile = null;

	private Uri picasaUri = null;

	private boolean isMultiMsg;

	private List<ContactInfo> contactList;

	private List<ConvMessage> messageList;

	private String vidCompressionRequired = "0";

	private int retryCount = 0;

	private long startTime;

	private boolean isManualRetry;

	private long networkTimeMs;

	public UploadFileTask(Context ctx, ConvMessage convMessage, String fileKey, boolean isManualRetry)
	{
		super(ctx, null, -1, null);
		this.userContext = convMessage;
		if (userContext != null)
		{
			this.msgId = userContext.getMsgID();
			HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
			if (!TextUtils.isEmpty(hikeFile.getSourceFilePath()))
			{
				if (hikeFile.getSourceFilePath().startsWith(HikeConstants.PICASA_PREFIX))
				{
					this.picasaUri = Uri.parse(hikeFile.getSourceFilePath().substring(HikeConstants.PICASA_PREFIX.length()));
				}
			}
		}
		this.fileKey = fileKey;
		this.isManualRetry = isManualRetry;
	}

	public UploadFileTask(Context ctx, List<ContactInfo> contactList, List<ConvMessage> messageList, String fileKey, boolean isManualRetry)
	{
		super(ctx, null, -1, null);
		this.userContext = messageList.get(0);
		this.msgId = userContext.getMsgID();
		this.contactList = contactList;
		this.messageList = messageList;
		this.isMultiMsg = true;
		this.fileKey = fileKey;
		this.isManualRetry = isManualRetry;
	}

	private IRequestListener getValidateFileKeyRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				int resCode = result.getStatusCode();
				if (resCode == HttpURLConnection.HTTP_OK)
				{
					// This is to get the file size from server
					// continue anyway if not able to obtain the size
					try
					{
						List<Header> headers = result.getHeaders();
						for (Header header : headers)
						{
							if (header.getName().equals("Content-Range"))
							{
								String range = header.getValue();
								fileSize = Integer.valueOf(range.substring(range.lastIndexOf("/") + 1, range.length()));
							}
							else if (header.getName().equals(HttpHeaderConstants.NETWORK_TIME_INCLUDING_RETRIES))
							{
								String timeString = header.getValue();
								networkTimeMs += (Integer.valueOf(timeString) / 1000);
							}
						}
					}
					catch (Exception e)
					{
						Logger.e(TAG, "exception while reading file size from validate file key request success", e);
						fileSize = 0;
					}
				}

				verifyMd5(true);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				if (errorResponse != null)
				{
					networkTimeMs += getNetworkTimeMsIncludingRetriesFromHeaders(errorResponse.getHeaders());
				}

				if (httpException.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
				{
					saveNoNetworkState(fileKey);
					FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", httpException);
					removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
					logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					return;
				}

				if (httpException.getErrorCode() / 100 > 0)
				{
					retryCount++;
					if (retryCount >= FileTransferManager.MAX_RETRY_COUNT)
					{
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else
					{
						// do this in any failure response code returned by server
						logCesData(CesConstants.FT_STATUS_IN_PROGRESS, false, String.valueOf(httpException.getErrorCode()));
						fileKey = null;
						verifyMd5(false);
					}
				}
				else
				{
					if (httpException.getCause() instanceof FileTransferCancelledException)
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
					}
					else if (httpException.getCause() instanceof FileNotFoundException)
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.CARD_UNMOUNT);
					}
					else if (httpException.getCause() instanceof Exception)
					{
						Throwable throwable = httpException.getCause();
						if (FileTransferManager.READ_FAIL.equals(throwable.getMessage()))
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.READ_FAIL);
						}
						else
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						}
					}
					else
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
					}
					logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, Utils.getStackTrace(httpException));
				}
			}
		};
	}

	public void validateFileKey()
	{
		if (!FileTransferManager.getInstance(context).isFileTaskExist(msgId))
		{
			return;
		}

		if (TextUtils.isEmpty(fileKey))
		{
			HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
			FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(msgId, hikeFile.getFile());
			if (fst != null && !TextUtils.isEmpty(fst.getFileKey()))
			{
				fileKey = fst.getFileKey();
				deleteStateFile();
			}
			else
			{
				verifyMd5(false);
				return;
			}
		}

		retryCount = 0;
		// If we are not able to verify the filekey validity from the server, fall back to uploading the file
		RequestToken validateFileKeyToken = HttpRequests.validateFileKey(fileKey, msgId, getValidateFileKeyRequestListener());
		validateFileKeyToken.execute();
	}

	/**
	 * This function do the initial steps for uploading i.e Create copy of file to upload (if required)
	 * 
	 * Note : All these steps are done if and only if required else this function will simply return
	 * 
	 * @throws Exception
	 */
	private void initFileUpload(boolean isFileKeyValid) throws FileTransferCancelledException, Exception
	{
		HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
		hikeFileType = hikeFile.getHikeFileType();
		this.mAttachementType = hikeFile.getAttachementType();

		selectedFile = new File(hikeFile.getFilePath());
		String fileName = selectedFile.getName();
		if (hikeFile.getFilePath() == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file",
					"Throwing FileNotFoundException due to file path is null ");
			throw new FileNotFoundException("File is not accessible. SDCard unmount");
		}
		if (picasaUri == null)
		{
			// Added isEmpty check instead of null check because in some devices it returns empty string rather than null.
			if (TextUtils.isEmpty(hikeFile.getSourceFilePath()))
			{
				Logger.d("This filepath: ", selectedFile.getPath());
				Logger.d("Hike filepath: ", Utils.getFileParent(hikeFileType, true));
			}
			else
			{
				mFile = new File(hikeFile.getSourceFilePath());
				if (!isFileKeyValid && mFile.exists() && hikeFileType == HikeFileType.IMAGE && !mFile.getPath().startsWith(Utils.getFileParent(hikeFileType, true)))
				{
					selectedFile = Utils.getOutputMediaFile(hikeFileType, fileName, true);
					if (selectedFile == null)
					{
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_2_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file",
								"Throwing READ_FAIL when selected file is null");
						throw new Exception(FileTransferManager.READ_FAIL);
					}

					if (selectedFile.exists() && selectedFile.length() > 0)
					{
						selectedFile = Utils.getOutputMediaFile(hikeFileType, null, true);
					}
					/*
					 * Changes done to fix the issue where some users are getting FileNotFoundEXception while creating file.
					 */
					try
					{
						if (!selectedFile.exists())
							selectedFile.createNewFile();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					if (!Utils.compressAndCopyImage(mFile.getPath(), selectedFile.getPath(), context))
					{
						Logger.d(getClass().getSimpleName(), "throwing copy file exception");
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file",
								"Throwing READ_FAIL on unsuccessful comression of image");
						throw new Exception(FileTransferManager.READ_FAIL);
					}
					hikeFile.setFile(selectedFile);
				}
				else if (hikeFileType == HikeFileType.VIDEO)
				{
					File compFile = null;
					VideoEditedInfo info = null;
					long time = 0;
					if (!isFileKeyValid && android.os.Build.VERSION.SDK_INT >= 18
							&& PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.COMPRESS_VIDEO, true))
					{
						info = VideoUtilities.processOpenVideo(mFile.getPath());
						if (info != null)
						{
							if (info.isCompRequired)
							{
								time = System.currentTimeMillis();
								/*
								 * Changes done to avoid the creation of multiple compressed file. Here I'm using message id as unique id of file.
								 */
								String destFileName = "Vid_" + msgId + ".mp4";
								info.destFile = Utils.getOutputMediaFile(HikeFileType.VIDEO, destFileName, true);
								if (info.destFile.exists())
									info.destFile.delete();
								hikeFile.setVideoEditedInfo(info);
								HikeVideoCompressor instance = new HikeVideoCompressor();
								compFile = instance.compressVideo(hikeFile);
								Logger.d(getClass().getSimpleName(), "Video compression time = " + (System.currentTimeMillis() - time));
								time = (System.currentTimeMillis() - time);
							}
						}
					}
					if (compFile != null && compFile.exists())
					{
						FTAnalyticEvents.sendVideoCompressionEvent(info.originalWidth + "x" + info.originalHeight, info.resultWidth + "x" + info.resultHeight, mFile.length(),
								compFile.length(), 1, time);
						selectedFile = compFile;
						vidCompressionRequired = "1";
						Utils.deleteFileFromHikeDir(context, mFile, hikeFileType);
					}
					else
					{
						if (info != null)
						{
							FTAnalyticEvents.sendVideoCompressionEvent(info.originalWidth + "x" + info.originalHeight, info.resultWidth + "x" + info.resultHeight, mFile.length(),
									0, 0);
						}
						selectedFile = mFile;
					}
					if (selectedFile.length() > HikeConstants.MAX_FILE_SIZE) {
						String msisdn = ((ConvMessage) userContext).getMsisdn();
						Utils.recordEventMaxSizeToastShown(ChatAnalyticConstants.VIDEO_MAX_SIZE_TOAST_SHOWN, ChatThreadUtils.getChatThreadType(msisdn), msisdn, hikeFile.getFileSize());
					}
					hikeFile.setFile(selectedFile);
					hikeFile.setFileSize(selectedFile.length());
				}
				// do not copy the file if it is video or audio or any other file
				else
				{
					selectedFile = mFile;
					hikeFile.setFile(selectedFile);
				}
				hikeFile.removeSourceFile();
				JSONObject metadata = new JSONObject();
				JSONArray filesArray = new JSONArray();
				filesArray.put(hikeFile.serialize());
				metadata.put(HikeConstants.FILES, filesArray);
				metadata.put(HikeConstants.CAPTION, ((ConvMessage) userContext).getMetadata().getCaption());
				((ConvMessage) userContext).setMetadata(metadata);
				userContext.setMetadata(metadata);
			}
		}
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				HikeConversationsDatabase.getInstance().updateMessageMetadata(msg.getMsgID(), msg.getMetadata());
			}
		}
		else
		{
			HikeConversationsDatabase.getInstance().updateMessageMetadata(userContext.getMsgID(), userContext.getMetadata());
		}
		fileName = hikeFile.getFileName();
		fileType = hikeFile.getFileTypeString();
		hikeFileType = hikeFile.getHikeFileType();
	}

	public void startFileUploadProcess()
	{
		startTime = System.nanoTime() / (1000 * 1000);
		validateFileKey();
	}

	public void upload()
	{
		uploadFile(selectedFile);
	}

	private String getImageQuality()
	{
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, HikeConstants.ImageQuality.QUALITY_DEFAULT);
		String imageQuality = HikeConstants.ImageQuality.IMAGE_QUALITY_DEFAULT;
		switch (quality)
		{
		case HikeConstants.ImageQuality.QUALITY_ORIGINAL:
			imageQuality = HikeConstants.ImageQuality.IMAGE_QUALITY_ORIGINAL;
			break;
		case HikeConstants.ImageQuality.QUALITY_MEDIUM:
			imageQuality = HikeConstants.ImageQuality.IMAGE_QUALITY_MEDIUM;
			break;
		case HikeConstants.ImageQuality.QUALITY_SMALL:
			imageQuality = HikeConstants.ImageQuality.IMAGE_QUALITY_SMALL;
			break;
		}
		return imageQuality;
	}

	private IRequestInterceptor getInitFileUploadInterceptor(final boolean isFileKeyValid) 
	{
		return new IRequestInterceptor()
		{
			@Override
			public void intercept(Chain chain) throws Exception
			{
				initFileUpload(isFileKeyValid);
				if (userContext.getMetadata().getHikeFiles().get(0).getFileSize() > HikeConstants.MAX_FILE_SIZE)
				{
					removeTaskAndShowToast(HikeConstants.FTResult.FILE_SIZE_EXCEEDING);
					return;
				}
				if (TextUtils.isEmpty(fileKey))
				{
					String fileMd5 = Utils.fileToMD5(selectedFile.getAbsolutePath());
					chain.getRequestFacade().setUrl(new URL(getFastFileUploadBaseUrl() + fileMd5));
					FileSavedState fst = HttpManager.getInstance().getRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId));//if (not started) then proceed
					if (fst == null || fst.getFTState() == FTState.NOT_STARTED)
					{
						chain.proceed();
					}
					else
					{
						uploadFile(selectedFile);
					}
				}
				else
				{
					postFileUploadMsgProcessing();
					logCesData(CesConstants.FT_STATUS_COMPLETE, true);
				}
			}
		};
	}

	private void logCesData(int state, boolean isQuickUpload)
	{
		logCesData(state, isQuickUpload, null);
	}

	private void logCesData(int state, boolean isQuickUpload, String stackTrace)
	{
		FTDataInfoFormatBuilder<?> builder = new FTDataInfoFormatBuilder<>()
				.setNetType(String.valueOf(Utils.getNetworkType(context)))
				.setFileSize(fileSize)
				.setFileAvailability(isQuickUpload)
				.setManualRetry(isManualRetry)
				.setFileType(fileType)
				.setFTStatus(state)
				.setFTTaskType(CesConstants.FT_UPLOAD)
				.setNetProcTime(networkTimeMs)
				.setProcTime((System.nanoTime() / (1000 * 1000)) - startTime)
				.setUniqueId(msgId + "_" + AccountUtils.mUid);

		if (!TextUtils.isEmpty(stackTrace))
		{
			builder.setStackTrace(stackTrace);
		}

		builder.setModule(CesConstants.FT_MODULE);
		CustomerExperienceScore.getInstance().recordCesData(CesConstants.CESModule.FT, builder);
	}

	public void verifyMd5(final boolean isFileKeyValid)
	{
		if (!FileTransferManager.getInstance(context).isFileTaskExist(msgId))
		{
			return;
		}

		retryCount = 0;
		RequestToken token = HttpRequests.verifyMd5(msgId, new IRequestListener()
		{
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				if (errorResponse != null)
				{
					networkTimeMs += getNetworkTimeMsIncludingRetriesFromHeaders(errorResponse.getHeaders());
				}

				FTAnalyticEvents.sendQuickUploadEvent(0);
				if (httpException.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
				{
					saveNoNetworkState(fileKey);
					FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "No Internet error");
					removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
					logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
				}
				else if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
				{
					FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "All", "CANCELLED UPLOAD");
					removeTaskAndShowToast(HikeConstants.FTResult.CANCELLED);
					logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
				}
				else if (httpException.getErrorCode() / 100 > 0)
				{
					retryCount++;
					if (retryCount >= FileTransferManager.MAX_RETRY_COUNT)
					{
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else
					{
						logCesData(CesConstants.FT_STATUS_IN_PROGRESS, false, String.valueOf(httpException.getErrorCode()));
						// do this in any failure response code returned by server
						uploadFile(selectedFile);
					}
				}
				else
				{
					if (httpException.getCause() instanceof FileTransferCancelledException)
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
					}
					else if (httpException.getCause() instanceof FileNotFoundException)
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.CARD_UNMOUNT);
					}
					else if (httpException.getCause() instanceof Exception)
					{
						Throwable throwable = httpException.getCause();
						if (FileTransferManager.READ_FAIL.equals(throwable.getMessage()))
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.READ_FAIL);
						}
						else if (FileTransferManager.UNABLE_TO_DOWNLOAD.equals(throwable.getMessage()))
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "DOWNLOAD_FAILED - ",
									httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.DOWNLOAD_FAILED);
						}
						else
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_QUICK_AREA, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Exception QUICK UPLOAD_FAILED - ",
									httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						}
					}
					logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, Utils.getStackTrace(httpException));
				}
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION, true) && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DISABLE_QUICK_UPLOAD, false))
				{
					uploadFile(selectedFile);
					return;
				}

				networkTimeMs += getNetworkTimeMsIncludingRetriesFromHeaders(result.getHeaders());

				FTAnalyticEvents.sendQuickUploadEvent(1);
				JSONObject responseJson = new JSONObject();
				try
				{
					List<Header> headers = result.getHeaders();
					JSONObject resData = new JSONObject();
					for (Header h : headers)
					{
						if (h.getName() == null)
						{
							continue;
						}
						switch (h.getName())
						{
						case HikeConstants.FILE_KEY:
							resData.put(HikeConstants.FILE_KEY, h.getValue());
							break;
						case HikeConstants.CONTENT_TYPE:
							resData.put(HikeConstants.CONTENT_TYPE, h.getValue());
							break;
						case HikeConstants.FILE_SIZE:
							resData.put(HikeConstants.FILE_SIZE, h.getValue());
							break;
						case HikeConstants.FILE_NAME:
							resData.put(HikeConstants.FILE_NAME, h.getValue());
							break;
						}
					}
					responseJson.put(HikeConstants.DATA_2, resData);
					handleSuccessJSON(responseJson);
					logCesData(CesConstants.FT_STATUS_COMPLETE, true);
				}
				catch (JSONException ex)
				{
					Logger.e("UploadFileTask", " ex :" , ex);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}
		}, getInitFileUploadInterceptor(isFileKeyValid));
		token.execute();
	}

	public IRequestInterceptor getUploadFileInterceptor()
	{
		return new IRequestInterceptor() {
			@Override
			public void intercept(Chain chain) throws Exception {
				JSONObject json = getFileSavedState().getResponseJson();
				if (json != null)
				{
					handleSuccessJSON(json);
				}
				else
				{
					chain.proceed();
				}
			}
		};
	}

	public void uploadFile(File sourceFile)
	{
		uploadFile(sourceFile, false);
	}

	public void uploadFile(File sourceFile, boolean retry)
	{
		if (!FileTransferManager.getInstance(context).isFileTaskExist(msgId))
		{
			return;
		}

		if (!retry)
		{
			retryCount = 0;
		}

		if (requestToken == null || !requestToken.isRequestRunning())
		{
			String fileTypeToSendInHttpCall = (hikeFileType == HikeFileType.AUDIO_RECORDING) ? fileType : "";
			requestToken = HttpRequests.uploadFile(sourceFile.getAbsolutePath(), msgId, vidCompressionRequired, fileTypeToSendInHttpCall, new IRequestListener()
			{
				@Override
				public void onRequestSuccess(Response result)
				{
					networkTimeMs += getNetworkTimeMsIncludingRetriesFromHeaders(result.getHeaders());

					FTState state = getFileSavedState().getFTState();
					if (state == FTState.COMPLETED)
					{
						byte[] b = (byte[]) result.getBody().getContent();
						Logger.d("HttpResponseUpload", "  result json : " + (new String(b)));
						try
						{
							JSONObject responseJson = new JSONObject(new String(b));
							handleSuccessJSON(responseJson);
							logCesData(CesConstants.FT_STATUS_COMPLETE, false);
						}
						catch (JSONException e)
						{
							Logger.e(TAG, "Error occurred while using json after upload file succeeded", e);
						}
					}
					else
					{
						String sessionId = getFileSavedState().getSessionId();
						if (!TextUtils.isEmpty(sessionId))
						{
							saveSessionId(sessionId);
						}
					}
				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{
					Logger.d("HttpResponseUpload", "  onprogress update called : " + progress);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
				}

				@Override
				public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
				{
					Logger.e("HttpResponseUpload", "  onprogress failure called : ", httpException.getCause());

					if (errorResponse != null)
					{
						networkTimeMs += getNetworkTimeMsIncludingRetriesFromHeaders(errorResponse.getHeaders());
					}

					if (httpException.getErrorCode() == HttpException.REASON_CODE_REQUEST_PAUSED)
					{
						if (userContext != null)
						{
							removeTask();
							HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
						}
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else if (httpException.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
					{
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
					{
						deleteStateFile();
						removeTaskAndShowToast(HikeConstants.FTResult.CANCELLED);
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else if (httpException.getErrorCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
					{
						retryCount++;
						if (retryCount >= FileTransferManager.MAX_RETRY_COUNT)
						{
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
							logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
						}
						else
						{
							deleteStateFile();
							logCesData(CesConstants.FT_STATUS_IN_PROGRESS, false, String.valueOf(httpException.getErrorCode()));
							handler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									uploadFile(selectedFile, true);
								}
							}, FileTransferManager.RETRY_DELAY);
						}
					}
					else if (httpException.getErrorCode() / 100 > 0)
					{
						retryCount++;
						if (retryCount >= FileTransferManager.MAX_RETRY_COUNT)
						{
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
							logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
						}
						else
						{
							logCesData(CesConstants.FT_STATUS_IN_PROGRESS, false, String.valueOf(httpException.getErrorCode()));
							handler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									uploadFile(selectedFile, true);
								}
							}, FileTransferManager.RETRY_DELAY);
						}
					}
					else if (httpException.getErrorCode() == HttpException.REASON_CODE_MALFORMED_URL)
					{
						FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "URLCreation", "UPLOAD_FAILED - ", httpException);
						Logger.e(getClass().getSimpleName(), "Exception", httpException);
						removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, String.valueOf(httpException.getErrorCode()));
					}
					else
					{
						if (httpException.getCause() instanceof FileNotFoundException)
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", httpException);
							Logger.e(getClass().getSimpleName(), "Exception", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.READ_FAIL);
						}
						else if (httpException.getCause() instanceof IOException)
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "all",
									"IOException UPLOAD_FAILED - ", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						}
						else if (httpException.getCause() instanceof JSONException)
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_5, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "json",
									"JSONException UPLOAD_FAILED - ", httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						}
						else if (httpException.getCause() instanceof Exception)
						{
							FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_6, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Exception UPLOAD_FAILED - ",
									httpException);
							removeTaskAndShowToast(HikeConstants.FTResult.UPLOAD_FAILED);
						}
						logCesData(CesConstants.FT_STATUS_INCOMPLETE, false, Utils.getStackTrace(httpException));
					}
				}
			}, getUploadFileInterceptor(), new FileTransferChunkSizePolicy(context));
		}
		requestToken.execute();
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	private void handleSuccessJSON(JSONObject responseJson) throws JSONException
	{
		JSONObject fileJSON = responseJson.getJSONObject(HikeConstants.DATA_2);
		fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		fileType = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		fileSize = (int) fileJSON.optLong(HikeConstants.FILE_SIZE);
		postFileUploadMsgProcessing();
	}

	private void postFileUploadMsgProcessing() throws JSONException
	{
		/*
		 * Saving analytic event before publishing the mqtt message.
		 */

		// this.analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(selectedFile, msgId));

		JSONObject metadata = new JSONObject();
		JSONArray filesArray = new JSONArray();

		HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
		hikeFile.setFileKey(fileKey);
		hikeFile.setFileSize(fileSize);
		hikeFile.setFileTypeString(fileType);

		filesArray.put(hikeFile.serialize());
		metadata.put(HikeConstants.FILES, filesArray);
		metadata.put(HikeConstants.CAPTION, ((ConvMessage) userContext).getMetadata().getCaption());

		if (isMultiMsg)
		{
			long ts = System.currentTimeMillis() / 1000;

			MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
			for (ConvMessage msg : messageList)
			{
				msg.setMetadata(messageMetadata);
				msg.setTimestamp(ts);
				HikeConversationsDatabase.getInstance().updateMessageMetadata(msg.getMsgID(), msg.getMetadata());
			}
			ArrayList<ConvMessage> pubsubMsgList = new ArrayList<ConvMessage>();
			pubsubMsgList.add(userContext);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_UPLOADED, new MultipleConvMessage(pubsubMsgList, contactList));
		}
		else
		{
			ConvMessage convMessageObject = userContext;
			convMessageObject.setMetadata(metadata);

			// The file was just uploaded to the servers, we want to publish
			// this event
			convMessageObject.setTimestamp(System.currentTimeMillis() / 1000);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, convMessageObject);

			if (convMessageObject.isBroadcastConversation())
			{
				List<PairModified<GroupParticipant, String>> participantList = ContactManager.getInstance().getGroupParticipants(convMessageObject.getMsisdn(), false, false);
				for (PairModified<GroupParticipant, String> grpParticipant : participantList)
				{
					String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
					convMessageObject.addToSentToMsisdnsList(msisdn);
				}
				OneToNConversationUtils.addBroadcastRecipientConversations(convMessageObject);
			}

			// Message sent from here will contain file key and also message_id ==> this is actually being sent to the server.
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);
		}
		deleteStateFile();
		Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());

		if (userContext != null)
		{
			removeTask();
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}

		FileTransferManager.getInstance(context).logTaskCompletedAnalytics(msgId, userContext, false);
	}

	protected void saveSessionId(String sessionId)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				saveSessionId(getFileSavedState(), msg.getMsgID(), sessionId);
			}
		}
		else
		{
			saveSessionId(getFileSavedState(), msgId, sessionId);
		}
	}

	protected void saveSessionId(FileSavedState state, long msgId, String sessionId)
	{
		if (state == null)
		{
			state = new FileSavedState();
		}
		state.setFTState(FTState.ERROR);
		state.setSessionId(sessionId);
		HttpManager.getInstance().saveRequestStateInDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId), state);
	}

	protected void saveNoNetworkState(String fileKey)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				saveNoNetworkState(fileKey, msg.getMsgID());
			}
		}
		else
		{
			saveNoNetworkState(fileKey, msgId);
		}
	}

	protected void saveNoNetworkState(String fileKey, long msgId)
	{
		FileSavedState fss = HttpManager.getInstance().getRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId));
		if (fss == null)
		{
			fss = new FileSavedState();
		}
		fss.setFTState(FTState.ERROR);
		fss.setFileKey(fileKey);
		HttpManager.getInstance().saveRequestStateInDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId), fss);
	}

	protected void deleteStateFile()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg: messageList)
			{
				HttpManager.getInstance().deleteRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msg.getMsgID()));
			}
		}
		else
		{
			HttpManager.getInstance().deleteRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId));
		}
	}

	private void removeTask()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				FileTransferManager.getInstance(context).removeTask(msg.getMsgID());
			}
		}
		else
		{
			FileTransferManager.getInstance(context).removeTask(userContext.getMsgID());
		}
	}

	@Override
	public FileSavedState getFileSavedState()
	{
		FileSavedState fss = super.getFileSavedState();
		if (fss == null)
		{
			fss = HttpManager.getInstance().getRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId));
		}
		return fss != null ? fss : new FileSavedState();
	}

	private void removeTaskAndShowToast(final HikeConstants.FTResult result)
	{
		if (isMultiMsg)
		{
			FileSavedState fss = HttpManager.getInstance().getRequestStateFromDB(HttpRequestConstants.getUploadFileBaseUrl(), String.valueOf(msgId));
			if (fss != null)
			{
				String sessionId = fss.getSessionId();
				if (!TextUtils.isEmpty(sessionId))
				{
					saveSessionId(sessionId);
				}
			}
		}

		if (userContext != null)
		{
			removeTask();
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}

		if (getFileSavedState().getFTState() != FTState.PAUSED)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					switch (result)
					{
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
							Toast.makeText(context, R.string.upload_cancelled, Toast.LENGTH_SHORT).show();
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
	}

	private long getNetworkTimeMsIncludingRetriesFromHeaders(List<Header> headers)
	{
		if (headers == null || headers.isEmpty())
		{
			return 0;
		}

		long timeInNs = 0;
		for (Header header : headers)
		{
			if (HttpHeaderConstants.NETWORK_TIME_INCLUDING_RETRIES.equals(header.getName()))
			{
				String timeString = header.getValue();
				if (!TextUtils.isEmpty(timeString))
					timeInNs = Long.valueOf(timeString);
			}
		}
		return timeInNs / (1000 * 1000);
	}
}
