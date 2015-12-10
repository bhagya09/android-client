package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import static com.bsb.hike.HikeConstants.IMAGE_QUALITY;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.video.HikeVideoCompressor;
import com.bsb.hike.video.VideoUtilities;
import com.bsb.hike.video.VideoUtilities.VideoEditedInfo;

public class UploadFileTask extends FileTransferBase
{
	private String fileType;

	private ConvMessage userContext;

	private long msgId;

	private String fileKey;

	private Context context;

	protected int fileSize;

	protected HikeFileType hikeFileType;

	private int mAttachementType;

	private File selectedFile = null;

	private File mFile = null;

	private Uri picasaUri = null;

	private boolean isMultiMsg;

	private List<ContactInfo> contactList;

	private List<ConvMessage> messageList;

	public UploadFileTask(Context ctx, ConvMessage convMessage, String fileKey)
	{
		super(ctx, null, -1, null);
		this.userContext = convMessage;
		if (userContext != null)
			this.msgId = userContext.getMsgID();
		this.fileKey = fileKey;
		HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
		if (!TextUtils.isEmpty(hikeFile.getSourceFilePath()))
		{
			if (hikeFile.getSourceFilePath().startsWith(HikeConstants.PICASA_PREFIX))
			{
				this.picasaUri = Uri.parse(hikeFile.getSourceFilePath().substring(HikeConstants.PICASA_PREFIX.length()));
			}
		}
	}

	public UploadFileTask(Context ctx, List<ContactInfo> contactList, List<ConvMessage> messageList, String fileKey)
	{
		super(ctx, null, -1, null);
		this.userContext = messageList.get(0);
		this.msgId = userContext.getMsgID();
		this.contactList = contactList;
		this.messageList = messageList;
		this.isMultiMsg = true;
		this.fileKey = fileKey;
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
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						fileSize = 0;
					}
				}

				try
				{
					initFileUpload(true);
					verifyMd5(selectedFile);
				}
				catch (FileTransferCancelledException e)
				{
					e.printStackTrace();
					// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", e);
					Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
					// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", e);
					Toast.makeText(context, R.string.card_unmount, Toast.LENGTH_SHORT).show();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					Logger.e(getClass().getSimpleName(), "Exception", e);
					if (FileTransferManager.READ_FAIL.equals(e.getMessage()))
					{
						// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", e);
						Toast.makeText(context, R.string.unable_to_read, Toast.LENGTH_SHORT).show();
					}
					else if (FileTransferManager.UNABLE_TO_DOWNLOAD.equals(e.getMessage()))
					{
						// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "DOWNLOAD_FAILED - ", e);
						Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				Log.d("FTMUploadFileTask2", "progress : " + progress);
				HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				if (httpException.getErrorCode() % 100 > 0)
				{
					fileKey = null;
					try
					{
						initFileUpload(false);
					}
					catch (FileTransferCancelledException e)
					{
						e.printStackTrace();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					verifyMd5(selectedFile);
				}
				else
				{
					// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", e);
					Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
				}
			}
		};
	}

	public void validateFileKey()
	{
		if (TextUtils.isEmpty(fileKey))
		{
			HikeFile hikeFile = userContext.getMetadata().getHikeFiles().get(0);
			FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(msgId, hikeFile.getFile());
			// TODO deleteStateFile();
			if (fst != null && !TextUtils.isEmpty(fst.getFileKey()))
			{
				fileKey = fst.getFileKey();
			}
			else
			{
				try
				{
					initFileUpload(false);
					verifyMd5(selectedFile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return;
			}
		}

		// If we are not able to verify the filekey validity from the server, fall back to uploading the file
		RequestToken validateFileKeyToken = HttpRequests.validateFileKey(fileKey, getValidateFileKeyRequestListener());
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
					if (!isFileKeyValid && android.os.Build.VERSION.SDK_INT >= 18
							&& PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.COMPRESS_VIDEO, true))
					{
						info = VideoUtilities.processOpenVideo(mFile.getPath());
						if (info != null)
						{
							if (info.isCompRequired)
							{
								long time = System.currentTimeMillis();
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
							}
						}
					}
					if (compFile != null && compFile.exists())
					{
						FTAnalyticEvents.sendVideoCompressionEvent(info.originalWidth + "x" + info.originalHeight, info.resultWidth + "x" + info.resultHeight, mFile.length(),
								compFile.length(), 1);
						selectedFile = compFile;
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
					hikeFile.setFile(selectedFile);
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

		ConvMessage msg = userContext;
		// TODO stateFile = getStateFile(msg);
		// File lofFile = FileTransferManager.getInstance(context).getAnalyticFile(msg.getMetadata().getHikeFiles().get(0).getFile(), msg.getMsgID());
		// this.analyticEvents = FTAnalyticEvents.getAnalyticEvents(lofFile);
		Logger.d(getClass().getSimpleName(), "Upload state bin file :: " + fileName + ".bin." + userContext.getMsgID());

		if (userContext.getMetadata().getHikeFiles().get(0).getFileSize() > HikeConstants.MAX_FILE_SIZE)
		{
			// TODO
			// return FTResult.FILE_SIZE_EXCEEDING;
		}
	}

	public void startFileUploadProcess()
	{
		try
		{
			validateFileKey();
		}
		catch (Exception e)
		{
			// TODO
			// Logger.e(getClass().getSimpleName(), "Exception", e);
			// saveStateOnNoInternet();
			// FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", e);
			// return FTResult.UPLOAD_FAILED;
		}
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

	public void verifyMd5(final File sourceFile)
	{
		String fileMD5 = Utils.fileToMD5(sourceFile.getAbsolutePath());
		RequestToken token = HttpRequests.verifyMd5(fileMD5, new IRequestListener()
		{
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				uploadFile(sourceFile);
			}

			@Override
			public void onRequestSuccess(Response result)
			{
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
				}
				catch (JSONException ex)
				{
					int c = 0;
					c++;
					Logger.d("http", " ex :" + ex + c);
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}
		});
		token.execute();
	}

	public void uploadFile(File sourceFile)
	{
		if (requestToken == null || !requestToken.isRequestRunning())
		{
			requestToken = HttpRequests.uploadFile(sourceFile.getAbsolutePath(), msgId, new IRequestListener()
			{
				@Override
				public void onRequestSuccess(Response result)
				{
					byte[] b = (byte[]) result.getBody().getContent();
					Logger.d("HttpResponseUpload", "  result json : " + (new String(b)));
					try
					{
						JSONObject responseJson = new JSONObject(new String(b));
						HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);

						handleSuccessJSON(responseJson);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

					if (userContext != null)
					{
						removeTask();
						HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
					}
				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{
					Logger.d("HttpResponseUpload", "  onprogress update called : " + progress);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
				}

				@Override
				public void onRequestFailure(HttpException httpException)
				{
					Logger.e("HttpResponseUpload", "  onprogress failure called : ", httpException.getCause());
					if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
					{
						// deleteStateFile();
						Toast.makeText(context, R.string.upload_cancelled, Toast.LENGTH_SHORT).show();
					}
					else if (httpException.getErrorCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
					{
						// deleteStateFile();
					}

					if (userContext != null)
					{
						removeTask();
						HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
					}

				}
			}, chunkSizePolicy);
		}
		requestToken.execute();
	}

	private void handleSuccessJSON(JSONObject responseJson) throws JSONException
	{
		JSONObject fileJSON = responseJson.getJSONObject(HikeConstants.DATA_2);
		fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		fileType = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		fileSize = (int) fileJSON.optLong(HikeConstants.FILE_SIZE);

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
		// TODO deleteStateFile();
		Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
	}

	public void upload()
	{
		if (requestToken != null)
		{
			requestToken.execute();
		}
	}

//	protected void deleteStateFile()
//	{
//		if (isMultiMsg)
//		{
//			for (ConvMessage msg : messageList)
//			{
//				super.deleteStateFile(getStateFile(msg));
//			}
//		}
//		else
//		{
//			super.deleteStateFile();
//		}
//	}
//
//	// @Override
//	protected void saveFileState(String sessionId)
//	{
//		if (isMultiMsg)
//		{
//			for (ConvMessage msg : messageList)
//			{
//				super.saveFileState(getStateFile(msg), _state, sessionId, null);
//			}
//		}
//		else
//		{
//			super.saveFileState(sessionId);
//		}
//	}
//
//	// @Override
//	protected void saveFileKeyState(String fileKey)
//	{
//		if (isMultiMsg)
//		{
//			for (ConvMessage msg : messageList)
//			{
//				super.saveFileKeyState(getStateFile(msg), fileKey);
//			}
//		}
//		else
//		{
//			super.saveFileKeyState(fileKey);
//		}
//	}

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
}
