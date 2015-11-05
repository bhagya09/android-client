package com.bsb.hike.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.analytics.AnalyticsConstants.MessageType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.CustomSSLSocketFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.HikeApacheHostNameVerifier;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.video.HikeVideoCompressor;
import com.bsb.hike.video.VideoUtilities;
import com.bsb.hike.video.VideoUtilities.VideoEditedInfo;

public class UploadFileTask extends FileTransferBase
{
	private String X_SESSION_ID;

	private Uri picasaUri = null;

	private String fileType;

	private String msisdn;

	private boolean isRecipientOnhike;

	private File selectedFile = null;

	private long recordingDuration = -1;

	private FutureTask<FTResult> futureTask;

	private static String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

	private int bufferSize = 1;

	private boolean freshStart;
	
	private ArrayList<ContactInfo> contactList;
	
	private ArrayList<ConvMessage> messageList;
	
	private boolean isMultiMsg;
	
	private int mAttachementType;

	private HttpClient client;

	private HttpContext httpContext = HttpClientContext.create();

	private String caption;

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, String msisdn, File sourceFile,
			String fileKey, String fileType, HikeFileType hikeFileType, boolean isRecording, boolean isForwardMsg, boolean isRecipientOnHike, long recordingDuration, int attachement, String argCaption)
	{
		super(handler, fileTaskMap, ctx, sourceFile, -1, hikeFileType, token, uId);
		this.msisdn = msisdn;
		this.fileType = fileType;
		this.isRecipientOnhike = isRecipientOnHike;
		this.recordingDuration = recordingDuration;
		this.isRecipientOnhike = isRecipientOnHike;
		this.fileKey = fileKey;
		_state = FTState.INITIALIZED;
		this.mAttachementType = attachement;
		this.caption = argCaption;
		createConvMessage(false);
	}
	
	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, ArrayList<ContactInfo> contactList, File sourceFile,
			String fileKey, String fileType, HikeFileType hikeFileType, boolean isRecording, boolean isForwardMsg, boolean isRecipientOnHike, long recordingDuration, int attachement)
	{
		super(handler, fileTaskMap, ctx, sourceFile, -1, hikeFileType, token, uId);
		this.contactList = contactList;
		this.isMultiMsg = true;
		this.fileType = fileType;
		this.isRecipientOnhike = isRecipientOnHike;
		this.recordingDuration = recordingDuration;
		this.isRecipientOnhike = isRecipientOnHike;
		this.fileKey = fileKey;
		_state = FTState.INITIALIZED;
		this.mAttachementType = attachement;
		createConvMessage(false);
	}

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Object convMessage,
			boolean isRecipientOnHike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.isRecipientOnhike = isRecipientOnHike;
		userContext = convMessage;
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		if (!TextUtils.isEmpty(hikeFile.getSourceFilePath()))
			if (hikeFile.getSourceFilePath().startsWith(HikeConstants.PICASA_PREFIX))
			{
				this.picasaUri = Uri.parse(hikeFile.getSourceFilePath().substring(HikeConstants.PICASA_PREFIX.length()));
			}
		_state = FTState.INITIALIZED;
	}

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Uri picasaUri, Object convMessage,
			boolean isRecipientOnHike)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.picasaUri = picasaUri;
		this.isRecipientOnhike = isRecipientOnHike;
		userContext = convMessage;
		_state = FTState.INITIALIZED;
		createConvMessage(false);
	}

	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Uri picasaUri,
			HikeFileType hikeFileType, String msisdn, boolean isRecipientOnHike, int attachement)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.picasaUri = picasaUri;
		this.hikeFileType = hikeFileType;
		this.msisdn = msisdn;
		this.isRecipientOnhike = isRecipientOnHike;
		_state = FTState.INITIALIZED;
		this.mAttachementType = attachement;
		createConvMessage(false);
	}
	
	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, Uri picasaUri,
			HikeFileType hikeFileType, List<ContactInfo> contactList, boolean isRecipientOnHike, int attachement)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.picasaUri = picasaUri;
		this.hikeFileType = hikeFileType;
		this.contactList = new ArrayList<>(contactList);
		this.isMultiMsg = true;
		this.isRecipientOnhike = isRecipientOnHike;
		_state = FTState.INITIALIZED;
		this.mAttachementType = attachement;
		createConvMessage(false);
	}
	
	protected UploadFileTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, String token, String uId, String msisdn, File sourceFile,
			String fileKey, String fileType, HikeFileType hikeFileType, boolean isRecording, long recordingDuration, int attachement,String fileName)
	{
		super(handler, fileTaskMap, ctx, sourceFile, -1, hikeFileType, token, uId);
		this.msisdn = msisdn;
		this.fileType = fileType;
		this.isRecipientOnhike = true;
		this.recordingDuration = recordingDuration;
		this.fileKey = fileKey;
		_state = FTState.IN_PROGRESS;
		this.mAttachementType = attachement;
		createConvMessage(true);
		stateFile = getStateFile(((ConvMessage) userContext));
		JSONObject metadata = ((ConvMessage) userContext).getMetadata().getJSON();
		JSONArray filesArray = new JSONArray();

		HikeFile hikeFile = ((ConvMessage)userContext).getMetadata().getHikeFiles().get(0);
		hikeFile.setFileSize(sourceFile.length());
		hikeFile.setHikeFileType(hikeFileType);
		hikeFile.setRecordingDuration(recordingDuration);
		hikeFile.setSent(true);
		hikeFile.setFileName(fileName);
		hikeFile.setFile(sourceFile);
		JSONObject fileJSON =  hikeFile.serialize();
		
		try 
		{
			filesArray.put(fileJSON);
			metadata.put(HikeConstants.FILES, filesArray);
			MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
			messageMetadata.getHikeFiles().get(0).setFileName(fileName);
			messageMetadata.getJSON().putOpt(HikeConstants.FILE_NAME, fileName);
			((ConvMessage) userContext).setMetadata(messageMetadata);
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		((ConvMessage) userContext).setTimestamp(System.currentTimeMillis() / 1000);
		Logger.d("OfflineManager","Message Metadata is "+((ConvMessage) userContext).getMetadata().getJSON());
		((ConvMessage) userContext).setMessageOriginType(OriginType.OFFLINE);
		HikeConversationsDatabase.getInstance().addConversationMessages((ConvMessage) userContext, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_MESSAGE_SENT, (ConvMessage) userContext);
	}

	protected void setFutureTask(FutureTask<FTResult> fuTask)
	{
		futureTask = fuTask;
		if (isMultiMsg)
		{
			for (ConvMessage msg : messageList)
			{
				fileTaskMap.put(msg.getMsgID(),futureTask);
			}
		}
		else
		{
			fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);
		}
	}

	// private ConvMessage createConvMessage(Uri picasaUri, File mFile, HikeFileType hikeFileType, String msisdn, boolean isRecipientOnhike, String fileType, long
	// recordingDuration) throws FileTransferCancelledException, Exception
	
	
	private void createConvMessage(boolean isOfflineMessage)
	{
		try
		{
			// TODO Auto-generated method stub
			System.gc();
			File destinationFile;
			String fileName = Utils.getFinalFileName(hikeFileType);
			JSONObject metadata;
			if (picasaUri == null)
			{
				destinationFile = mFile;
				fileName = destinationFile.getName();
				Bitmap thumbnail = null;
				String thumbnailString = null;
				String quality = null;
				if (hikeFileType == HikeFileType.IMAGE)
				{
					Bitmap.Config config = Bitmap.Config.RGB_565;
					if(Utils.hasJellyBeanMR1()){
						config = Bitmap.Config.ARGB_8888;
					}
					thumbnail = HikeBitmapFactory.scaleDownBitmap(destinationFile.getPath(), HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
							config, false, false);
					thumbnail = Utils.getRotatedBitmap(destinationFile.getPath(), thumbnail);
					if (thumbnail == null && !TextUtils.isEmpty(fileKey))
					{
						BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
						if (bd != null)
							thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey).getBitmap();
					}
					quality = getImageQuality();
				}
				else if (hikeFileType == HikeFileType.VIDEO)
				{
					thumbnail = ThumbnailUtils.createVideoThumbnail(destinationFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
					if (thumbnail == null && !TextUtils.isEmpty(fileKey))
					{
						BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey);
						if (bd != null)
							thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(fileKey).getBitmap();
					}
				}
				if (thumbnail != null)
				{
					int compressQuality;
					if (hikeFileType == HikeFileType.IMAGE)
					{
//						Bitmap bluredThumb = Utils.createBlurredImage(thumbnail, context);
//						if(bluredThumb != null){
//							compressQuality = 60;
//							thumbnail = bluredThumb;
//						}else
							compressQuality = 25;
					}else{
						compressQuality = 75;
					}
					byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
					thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
					thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
					// thumbnail.recycle();
					Logger.d(getClass().getSimpleName(), "Sent Thumbnail Size : " + tBytes.length);
				}
			
				metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, thumbnailString, thumbnail, recordingDuration, mFile.getPath(), mFile.length(), quality,caption);
			}
			else
			// this is the case for picasa picture
			{
				String[] filePathColumn = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
				Cursor cursor = context.getContentResolver().query(picasaUri, filePathColumn, null, null, null);
				// if it is a picasa image on newer devices with OS 3.0 and
				// up
				if (cursor != null)
				{
					cursor.moveToFirst();
					int nameIdx = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
					if (nameIdx != -1)
					{
						// fileName = cursor.getString(nameIdx);
					}
				}
				destinationFile = Utils.getOutputMediaFile(hikeFileType, fileName, true);
				if (TextUtils.isEmpty(fileName))
				{
					fileName = destinationFile.getName();
				}
				metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, null, null, recordingDuration, HikeConstants.PICASA_PREFIX + picasaUri.toString(), 0, ImageQuality.IMAGE_QUALITY_ORIGINAL,caption);
			}
			if (isMultiMsg)
			{
				messageList = new ArrayList<ConvMessage>();

				MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
				for (ContactInfo contact : contactList)
				{
					ConvMessage msg = createConvMessage(fileName, messageMetadata, contact.getMsisdn(), isRecipientOnhike);
					messageList.add(msg);
				}
				userContext = messageList.get(0);
				ArrayList<ConvMessage> pubsubMsgList = new ArrayList<ConvMessage>();
				pubsubMsgList.add((ConvMessage) userContext);
				MultipleConvMessage multiConMsg = new MultipleConvMessage(pubsubMsgList, contactList);
				HikeConversationsDatabase.getInstance().addConversations(multiConMsg.getMessageList(), multiConMsg.getContactList(),false);
				msgId = ((ConvMessage) userContext).getMsgID();
				for (int i=1 ; i < messageList.size() ; i++)
				{
					messageList.get(i).setMsgID(msgId + i);
				}
				multiConMsg.sendPubSubForConvScreenMultiMessage();
			}
			else
			{
				userContext = createConvMessage(fileName, metadata, msisdn, isRecipientOnhike);
				
				ConvMessage convMessageObject = (ConvMessage)userContext;
				if(convMessageObject.isBroadcastConversation())
				{
					convMessageObject.setMessageOriginType(OriginType.BROADCAST);
				}

				if(!isOfflineMessage)
					HikeConversationsDatabase.getInstance().addConversationMessages(convMessageObject,true);
				
				// 1) user clicked Media file and sending it
				MsgRelLogManager.startMessageRelLogging((ConvMessage) userContext, MessageType.MULTIMEDIA);
				msgId = ((ConvMessage) userContext).getMsgID();
				
				//Message sent from here will only do an entry in conversation db it is not actually being sent to server.
				if(!isOfflineMessage)
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);

					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessageObject);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FTR_INIT_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "ConvMsgCreation", "On conv message creation : ", e);
			e.printStackTrace();
			return;
		}
	}

	private ConvMessage createConvMessage(String fileName, MessageMetadata metadata, String msisdn, boolean isRecipientOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		return convMessage;
	}

	private ConvMessage createConvMessage(String fileName, JSONObject metadata, String msisdn, boolean isRecipientOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		return convMessage;
	}

	private JSONObject getFileTransferMetadata(String fileName, String fileType, HikeFileType hikeFileType, String thumbnailString, Bitmap thumbnail, long recordingDuration,
			String sourceFilePath, long fileSize, String img_quality, String caption) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName, TextUtils.isEmpty(fileType) ? HikeFileType.toString(hikeFileType) : fileType, thumbnailString, thumbnail, recordingDuration,
				sourceFilePath, fileSize, true, img_quality).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		if(!TextUtils.isEmpty(caption))
		{
			metadata.put(HikeConstants.CAPTION, caption);	
		}
		return metadata;
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
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		hikeFileType = hikeFile.getHikeFileType();

		selectedFile = new File(hikeFile.getFilePath());
		String fileName = selectedFile.getName();
		if (hikeFile.getFilePath() == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing FileNotFoundException due to file path is null ");
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
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_2_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing READ_FAIL when selected file is null");
						throw new Exception(FileTransferManager.READ_FAIL);
					}
					
					if(selectedFile.exists() && selectedFile.length() > 0)
					{
						selectedFile = Utils.getOutputMediaFile(hikeFileType, null, true);
					}
					/*
					 * Changes done to fix the issue where some users are getting FileNotFoundEXception while creating file.
					 */
					try {
						if(!selectedFile.exists())
							selectedFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (!Utils.compressAndCopyImage(mFile.getPath(), selectedFile.getPath(), context))
					{
						Logger.d(getClass().getSimpleName(), "throwing copy file exception");
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing READ_FAIL on unsuccessful comression of image");
						throw new Exception(FileTransferManager.READ_FAIL);
					}
					hikeFile.setFile(selectedFile);
				}
				else if(hikeFileType == HikeFileType.VIDEO)
				{
					File compFile = null;
					VideoEditedInfo info = null;
					if(!isFileKeyValid && android.os.Build.VERSION.SDK_INT >= 18 && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.COMPRESS_VIDEO, true))
					{
						info = VideoUtilities.processOpenVideo(mFile.getPath());
						if(info != null)
						{
							if(info.isCompRequired)
							{
								long time = System.currentTimeMillis();
								/*
								 * Changes done to avoid the creation of multiple compressed file. Here I'm using message id as unique id of file.
								 */
								String destFileName = "Vid_" + msgId + ".mp4";
								info.destFile = Utils.getOutputMediaFile(HikeFileType.VIDEO, destFileName, true);
								if(info.destFile.exists())
									info.destFile.delete();
								hikeFile.setVideoEditedInfo(info);
								HikeVideoCompressor instance = new HikeVideoCompressor();
								compFile = instance.compressVideo(hikeFile);
								Logger.d(getClass().getSimpleName(), "Video compression time = " + (System.currentTimeMillis() - time));
							}
						}
					}
					if(compFile != null && compFile.exists()){
						FTAnalyticEvents.sendVideoCompressionEvent(info.originalWidth + "x" + info.originalHeight, info.resultWidth + "x" + info.resultHeight,
								mFile.length(),  compFile.length(), 1);
						selectedFile = compFile;
						Utils.deleteFileFromHikeDir(context, mFile, hikeFileType);
					}else{
						if(info != null)
						{
							FTAnalyticEvents.sendVideoCompressionEvent(info.originalWidth + "x" + info.originalHeight, info.resultWidth + "x" + info.resultHeight,
									 mFile.length(), 0, 0);
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
				((ConvMessage) userContext).setMetadata(metadata);
			}
		}
		else
		// picasa case
		{
			try
			{
				Utils.downloadAndSaveFile(context.getContentResolver(), selectedFile, picasaUri);
			}
			catch (Exception e)
			{
				FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FTR_INIT_3_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", 
								"Throwing UNABLE_TO_DOWNLOAD- Unable to dowload file from picasa uri :", e);
				throw new Exception(FileTransferManager.UNABLE_TO_DOWNLOAD);
			}

			Bitmap thumbnail = null;
			String thumbnailString = null;
			if (hikeFileType == HikeFileType.IMAGE)
			{
				Bitmap.Config config = Bitmap.Config.RGB_565;
				if(Utils.hasJellyBeanMR1()){
					config = Bitmap.Config.ARGB_8888;
				}
				thumbnail = HikeBitmapFactory.scaleDownBitmap(selectedFile.getPath(), HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
						config, true, false);
				thumbnail = Utils.getRotatedBitmap(selectedFile.getPath(), thumbnail);
			}
			else if (hikeFileType == HikeFileType.VIDEO)
			{
				thumbnail = ThumbnailUtils.createVideoThumbnail(selectedFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
			}
			if (thumbnail != null)
			{
				int compressQuality;
				if (hikeFileType == HikeFileType.IMAGE)
				{
//					Bitmap bluredThumb = Utils.createBlurredImage(thumbnail, context);
//					if(bluredThumb != null){
//						compressQuality = 60;
//						thumbnail = bluredThumb;
//					}else
					compressQuality = 25;
				}else{
					compressQuality = 75;
				}
				byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
				thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
				thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
				
				// thumbnail.recycle();
			}
			else
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FTR_INIT_3_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing Network error");
				throw new Exception("Network error");
			}
			
			JSONObject metadata = getFileTransferMetadata(fileName, fileType, hikeFileType, thumbnailString, thumbnail, ImageQuality.IMAGE_QUALITY_ORIGINAL);
			hikeFile.removeSourceFile();
			((ConvMessage) userContext).setMetadata(metadata);
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
			HikeConversationsDatabase.getInstance().updateMessageMetadata(((ConvMessage) userContext).getMsgID(), ((ConvMessage) userContext).getMetadata());
		}
		fileName = hikeFile.getFileName();
		fileType = hikeFile.getFileTypeString();
		hikeFileType = hikeFile.getHikeFileType();

		ConvMessage msg = (ConvMessage) userContext;
		stateFile = getStateFile(msg);
		File lofFile = FileTransferManager.getInstance(context).getAnalyticFile(msg.getMetadata().getHikeFiles().get(0).getFile(), msg.getMsgID());
		this.analyticEvents =  FTAnalyticEvents.getAnalyticEvents(lofFile);
		Logger.d(getClass().getSimpleName(), "Upload state bin file :: " + fileName + ".bin." + ((ConvMessage) userContext).getMsgID());
	}

	private JSONObject getFileTransferMetadata(String fileName, String fileType, HikeFileType hikeFileType, String thumbnailString, Bitmap thumbnail, String img_quality) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName, TextUtils.isEmpty(fileType) ? HikeFileType.toString(hikeFileType) : fileType, thumbnailString, thumbnail, recordingDuration, true, img_quality)
				.serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}

	@Override
	public FTResult call()
	{
		mThread = Thread.currentThread();
		boolean isValidKey = false;
		try{
			isValidKey = isFileKeyValid();
		}catch(Exception e){
			Logger.e(getClass().getSimpleName(), "Exception", e);
			saveStateOnNoInternet();
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FK_VALIDATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		try
		{
			if (isValidKey)
			{
				try
				{
					initFileUpload(isValidKey);
				}
				catch (Exception e)
				{
					Logger.e(getClass().getSimpleName(), "exception while initFileUpload when key is valid", e);
				}
				/*
				 * Setting event in case of forward when file key is validated.
				 */
				this.analyticEvents.mAttachementType = this.mAttachementType;
				this.analyticEvents.mNetwork = FileTransferManager.getInstance(context).getNetworkTypeString();
			}
			else
			{
				initFileUpload(isValidKey);
			}
			if(!Utils.isUserOnline(context))
			{
				saveStateOnNoInternet();
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "No Internet error");
				return FTResult.UPLOAD_FAILED;
			}
			
			if(((ConvMessage) userContext).getMetadata().getHikeFiles().get(0).getFileSize()>HikeConstants.MAX_FILE_SIZE)
			{
				return FTResult.FILE_SIZE_EXCEEDING;
			}
			
		}
		catch (FileTransferCancelledException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "UPLOAD_FAILED - ", e);
			return FTResult.CARD_UNMOUNT;
		}
		catch (Exception e)
		{
			if (e != null)
			{
				Logger.e(getClass().getSimpleName(), "Exception", e);
				if (FileTransferManager.READ_FAIL.equals(e.getMessage()))
				{
					FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", e);
					return FTResult.READ_FAIL;
				}
				else if (FileTransferManager.UNABLE_TO_DOWNLOAD.equals(e.getMessage()))
				{
					FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "DOWNLOAD_FAILED - ", e);
					return FTResult.DOWNLOAD_FAILED;
				}
			}
		}

		try
		{
			if (_state == FTState.CANCELLED)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "All", "CANCELLED UPLOAD");
				return FTResult.CANCELLED;
			}

			if (TextUtils.isEmpty(fileKey))
			{

				JSONObject response = null;
				freshStart = true;
				while(freshStart)
				{
					freshStart = false;
					response = uploadFile(selectedFile); // <<----- this is the main upload function where upload to server is done
				}

				if (_state == FTState.CANCELLED)
					return FTResult.CANCELLED;
				else if (_state == FTState.PAUSED)
					return FTResult.PAUSED;
				else if (response == null)
					return FTResult.UPLOAD_FAILED;
				JSONObject fileJSON = response.getJSONObject(HikeConstants.DATA_2);
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
				fileType = fileJSON.optString(HikeConstants.CONTENT_TYPE);
				fileSize = (int) fileJSON.optLong(HikeConstants.FILE_SIZE);
			}else
				_state = FTState.IN_PROGRESS;
			/*
			 * Saving analytic event before publishing the mqtt message.
			 */
			this.analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(selectedFile, msgId));

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileSize(fileSize);
			hikeFile.setFileTypeString(fileType);

			filesArray.put(hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);
			metadata.put(HikeConstants.CAPTION, "thisisatestcaption");

			if (isMultiMsg)
			{
				long ts = System.currentTimeMillis() / 1000;

				MessageMetadata messageMetadata = new MessageMetadata(metadata, true);
				for (ConvMessage msg : messageList)
				{
					msg.setMetadata(messageMetadata);
					msg.setTimestamp(ts);
					HikeConversationsDatabase.getInstance().updateMessageMetadata(msg.getMsgID(), msg.getMetadata());
					//HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, msg);
				}
				ArrayList<ConvMessage> pubsubMsgList = new ArrayList<ConvMessage>();
				pubsubMsgList.add((ConvMessage) userContext);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_UPLOADED, new MultipleConvMessage(pubsubMsgList, contactList));
			}
			else
			{
				ConvMessage convMessageObject = (ConvMessage)userContext;
				convMessageObject.setMetadata(metadata);
	
				// The file was just uploaded to the servers, we want to publish
				// this event
				convMessageObject.setTimestamp(System.currentTimeMillis() / 1000);
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, convMessageObject);
	
				if(convMessageObject.isBroadcastConversation())
				{
					List<PairModified<GroupParticipant, String>> participantList= ContactManager.getInstance().getGroupParticipants(convMessageObject.getMsisdn(), false, false);
					for (PairModified<GroupParticipant, String> grpParticipant : participantList)
					{
						String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
						convMessageObject.addToSentToMsisdnsList(msisdn);
					}
					OneToNConversationUtils.addBroadcastRecipientConversations(convMessageObject);
				}
				
				//Message sent from here will contain file key and also message_id ==> this is actually being sent to the server.
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);
			}
			deleteStateFile();
			Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
			_state = FTState.COMPLETED;
		}
		catch (MalformedURLException e)
		{
			error();
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "URLCreation", "UPLOAD_FAILED - ", e);
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (FileNotFoundException e)
		{
			error();
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "READ_FAIL - ", e);
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.READ_FAIL;
		}
		catch (ClientProtocolException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_3, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "ClientProtocolException UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (IOException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "all", "IOException UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (JSONException e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_5, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "json", "JSONException UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		catch (Exception e)
		{
			error();
			Logger.e(getClass().getSimpleName(), "Exception", e);
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_CALLBACK_AREA_1_6, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Exception UPLOAD_FAILED - ", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject uploadFile(File sourceFile) throws MalformedURLException, FileNotFoundException, IOException, JSONException, ClientProtocolException, Exception
	{
		int mStart = 0;
		JSONObject responseJson = null;
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(hikeFile.getFile(), msgId);
		long length = sourceFile.length();
		if (length < 1)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing FileNotFoundException because File size less than 1 byte");
			throw new FileNotFoundException("File size less than 1 byte");
		}
		setFileTotalSize(length);
		// Bug Fix: 13029
		setBytesTransferred(fst.getTransferredSize());
		long temp = _bytesTransferred;
		temp *= 100;
		if (_totalSize > 0)
		{
			temp /= _totalSize;
			progressPercentage = (int) temp;
		}
		// represents this file is either not started or unrecovered error has happened
		Logger.d(getClass().getSimpleName(), "Starting Upload from state : " + fst.getFTState().toString());
		if (fst.getFTState().equals(FTState.NOT_STARTED))
		{
			this.analyticEvents.mAttachementType = this.mAttachementType;
			this.analyticEvents.mNetwork = FileTransferManager.getInstance(context).getNetworkTypeString();
			try
			{
				Logger.d(getClass().getSimpleName(), "Verifying MD5");
				JSONObject responseMd5 = verifyMD5(selectedFile);
				if(responseMd5 != null)
				{
					FTAnalyticEvents.sendQuickUploadEvent(1);
					return responseMd5;
				}
				else
					FTAnalyticEvents.sendQuickUploadEvent(0);
			}
			catch (Exception e)
			{
				FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_QUICK_AREA, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Exception QUICK UPLOAD_FAILED - ", e);
				Logger.e(getClass().getSimpleName(), "Exception", e);
				return null;
			}
			// here as we are starting new upload, we have to create the new session id
			X_SESSION_ID = UUID.randomUUID().toString();
			Logger.d(getClass().getSimpleName(), "SESSION_ID: " + X_SESSION_ID);
		}
		else if (fst.getFTState().equals(FTState.PAUSED) || fst.getFTState().equals(FTState.ERROR))
		{
			/*
			 * In case user paused the transfer during the last chunk. The Upload was completed and the response from server was stored with the state file. So when resumed, the
			 * response is read from state file. If this is not null the response is returned.
			 */
			if (fst.getResponseJson() != null)
			{
				_state = FTState.COMPLETED;
				deleteStateFile();

				responseJson = fst.getResponseJson();
				return responseJson;
			}
			X_SESSION_ID = fst.getSessionId();
			if(X_SESSION_ID != null)
			{
				URL baseUrl = mUrl = new URL(AccountUtils.fileTransferBase + "/user/pft/");
				try{
					mStart = AccountUtils.getBytesUploaded(String.valueOf(X_SESSION_ID), mUrl.toString(), getHttpScheme());
				}catch(Exception ex)
				{
					handleException(ex);
					mUrl = getUpdatedURL(mUrl, "ResumeLength", FTAnalyticEvents.UPLOAD_FILE_TASK, baseUrl);
					mStart = AccountUtils.getBytesUploaded(String.valueOf(X_SESSION_ID), mUrl.toString(), getHttpScheme());
				}
			}
			else
				mStart = 0;
			if (mStart <= 0)
			{
				X_SESSION_ID = UUID.randomUUID().toString();
				mStart = 0;
			}
			Logger.d(getClass().getSimpleName(), "SESSION_ID: " + X_SESSION_ID);
			this.analyticEvents.mRetryCount += 1;
		}
		_state = FTState.IN_PROGRESS;
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		
		if (mStart >= length)
		{
			mStart = 0;
			X_SESSION_ID = UUID.randomUUID().toString();
		}
		// @GM setting transferred bytes if there are any
		setBytesTransferred(mStart);
		URL baseUrl = mUrl = new URL(AccountUtils.fileTransferBase + "/user/pft/");
		RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
		raf.seek(mStart);

		setChunkSize();
		if (mStart == 0)
		{
			chunkSize = chunkSize / 5;
		}
		/*
		 * Safe check for the case where chunk size equals zero while calculating based on network and device memory.
		 * https://hike.fogbugz.com/default.asp?42482
		 */
		if(chunkSize <= 0)
		{
			FTAnalyticEvents.sendFTDevEvent(FTAnalyticEvents.UPLOAD_FILE_TASK, "Chunk size is less than or equal to 0, so setting it to default i.e. 100kb");
			chunkSize = DEFAULT_CHUNK_SIZE;
		}

		if (chunkSize > length)
			chunkSize = (int) length;
		setBufferSize();

		String boundaryMesssage = getBoundaryMessage();
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";

		int start = mStart;
		int end = (int) length;
		if (end >= (start + chunkSize))
			end = start + chunkSize;
		else
			chunkSize = end - start;
		end--;

		byte[] fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
		this.analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(sourceFile, msgId));

		while (end < length && responseJson == null)
		{
			if (_state != FTState.IN_PROGRESS) // this is to check if user has PAUSED or cancelled the upload
				break;

			Logger.d(getClass().getSimpleName(), "bytes " + start + "-" + end + "/" + length + ";  chunk:" + chunkSize + ";  buffer:" + bufferSize);
			boolean resetAndUpdate = false;
			int bytesRead = raf.read(fileBytes, boundaryMesssage.length(), chunkSize);
			if (bytesRead == -1)
			{
				raf.close();
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_FILE_READ, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "file", "Throwing IOException in partial read. files ended");
				throw new IOException("Exception in partial read. files ended");
			}
			String contentRange = "bytes " + start + "-" + end + "/" + length;
			String responseString = send(contentRange, fileBytes);

			if (end == (length - 1) && responseString != null)
			{
				Logger.d(getClass().getSimpleName(), "response: " + responseString);
				try
				{
					responseJson = new JSONObject(responseString);
				}
				catch(JSONException e)
				{
					FTAnalyticEvents.logDevException(FTAnalyticEvents.JSON_PARSING_ISSUE, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "Parsing response json received from server",
							"Response = " + responseString, e);
					raf.close();
					throw e;
				}
				incrementBytesTransferred(chunkSize);
				resetAndUpdate = true; // To update UI
			}
			else
			{

				// In case there is error uploading this chunk
				if (responseString == null)
				{
					if (shouldRetry() && Utils.isUserOnline(context))
					{
						if (freshStart)
						{
							raf.close();
							return null;
						}
						mUrl = getUpdatedURL(mUrl, "UploadingFile", FTAnalyticEvents.UPLOAD_FILE_TASK, baseUrl);
						raf.seek(start);
						setChunkSize();
						if (chunkSize > length)
							chunkSize = (int) length;
						if(chunkSize > (length - start))
							chunkSize = (int) (length - start);
						if (end != (start + chunkSize - 1))
						{
							end = (start + chunkSize - 1);
							fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
						}
						setBufferSize();
					}
					else
					{
						raf.close();
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_RETRY_COMPLETE, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "retry", "Throwing IOException on retry attempts complete");
						throw new IOException("Exception in partial upload. Auto Retry attempts completed");
					}

				}
				// When the chunk uploaded successfully
				else
				{
					start += chunkSize;
					incrementBytesTransferred(chunkSize);
					saveIntermediateProgress(X_SESSION_ID);
					resetAndUpdate = true; // To reset retry logic and update UI

					end = (int) length;
					setChunkSize();
					fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
					if (end >= (start + chunkSize))
					{
						end = start + chunkSize;
						end--;
					}
					else
					{
						end--;
						chunkSize = end - start + 1;
						fileBytes = setupFileBytes(boundaryMesssage, boundary, chunkSize);
					}
				}
			}

			/*
			 * Resetting reconnect logic Updating UI
			 */
			if (resetAndUpdate)
			{
				retry = true;
				reconnectTime = 0;
				retryAttempts = 0;
				temp = _bytesTransferred;
				temp *= 100;
				temp /= _totalSize;
				progressPercentage = (int) temp;
				if(_state != FTState.PAUSED)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);

				}
			}
		}

		switch (_state)
		{
		case CANCELLED:
			Logger.d(getClass().getSimpleName(), "FT Cancelled");
			deleteStateFile();
			break;
		case IN_PROGRESS:
			// Added sleep to complete the progress.
			//TODO Need to remove sleep and implement in a better way to achieve the progress UX.
			Thread.sleep(300);
			Logger.d(getClass().getSimpleName(), "FT Completed");
			_state = FTState.COMPLETED;
			deleteStateFile();
			break;
		case PAUSED:
			_state = FTState.PAUSED;
			Logger.d(getClass().getSimpleName(), "FT PAUSED");
			// In case upload was complete response JSON is to be saved not the Session_ID
			if (responseJson != null)
				saveFileState(X_SESSION_ID,responseJson);
			else
				saveFileState(X_SESSION_ID);
			this.analyticEvents.saveAnalyticEvent(FileTransferManager.getInstance(context).getAnalyticFile(sourceFile, msgId));
			break;
		default:
			break;
		}
		try
		{
			// we don't want to screw up result even if inputstream is not closed
			raf.close();
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "exception while closing random access file", e);
		}
		return responseJson;
	}

	private void setBufferSize()
	{
		bufferSize = 1;
		while ((bufferSize * 2) < chunkSize)
			bufferSize *= 2;
	}

	private byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + chunkSize + boundary.length()];
		System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
		System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + chunkSize, boundary.length());
		return fileBytes;
	}

	String getBoundaryMessage()
	{
		String sendingFileType = "";
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(fileType) || HikeConstants.CONTACT_CONTENT_TYPE.equals(fileType)
				|| HikeConstants.VOICE_MESSAGE_CONTENT_TYPE.equals(fileType))
		{
			sendingFileType = fileType;
		}
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append("\r\n");
		String name = selectedFile.getName();
		try
		{
			name = URLEncoder.encode(selectedFile.getName(), "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.d(getClass().getSimpleName(), "encode file name: " + name);
		res.append("Content-Disposition: form-data; name=\"").append("file").append("\"; filename=\"").append(name).append("\"\r\n").append("Content-Type: ")
				.append(sendingFileType).append("\r\n\r\n");
		return res.toString();
	}
	
	private File getStateFile(ConvMessage msg)
	{
		HikeFile file = msg.getMetadata().getHikeFiles().get(0);
		return new File(FileTransferManager.getInstance(context).getHikeTempDir(), file.getFileName() + ".bin." + msg.getMsgID());
	}
	
	@Override
	protected void deleteStateFile()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.deleteStateFile(getStateFile(msg));
			}
		}
		else
		{
			super.deleteStateFile();
		}
	}
	
	@Override
	protected void saveFileState(String sessionId)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.saveFileState(getStateFile(msg), _state, sessionId, null);
			}
		}
		else
		{
			super.saveFileState(sessionId);
		}
	}
	
	@Override
	protected void saveFileKeyState(String fileKey)
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg:messageList)
			{
				super.saveFileKeyState(getStateFile(msg), fileKey);
			}
		}
		else
		{
			super.saveFileKeyState(fileKey);
		}
	}

	private boolean isFileKeyValid() throws Exception
	{
		if (TextUtils.isEmpty(fileKey)){
			msgId = ((ConvMessage) userContext).getMsgID();
			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(hikeFile.getFile(), msgId);
			deleteStateFile();
			if(fst != null && !TextUtils.isEmpty(fst.getFileKey())){
				fileKey = fst.getFileKey();
			}else
				return false;
		}
		// If we are not able to verify the filekey validity from the server, fall back to uploading the file		
		final int MAX_RETRY = 3;
		int retry =0;
		URL baseUrl = mUrl = new URL(AccountUtils.fileTransferBaseDownloadUrl + fileKey);
		while(retry < MAX_RETRY)
		{
			try
			{
				mUrl = getUpdatedURL(mUrl, "FileKeyValidation", FTAnalyticEvents.UPLOAD_FILE_TASK, baseUrl);
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
				client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());
				client.getConnectionManager().getSchemeRegistry().register(getHttpScheme());
				HttpHead head = new HttpHead(mUrl.toString());
				head.addHeader("Cookie", "user=" + token + ";uid=" + uId);
				AccountUtils.setNoTransform(head);
	
				HttpResponse resp = client.execute(head);
				int resCode = resp.getStatusLine().getStatusCode();
				// Make sure the response code is 200.
				if (resCode == RESPONSE_OK)
				{
					// This is to get the file size from server
					// continue anyway if not able to obtain the size
					try
					{
						String range = resp.getFirstHeader("Content-Range").getValue();
						fileSize = Integer.valueOf(range.substring(range.lastIndexOf("/") + 1, range.length()));
					}
					catch (Exception e)
					{
						e.printStackTrace();
						fileSize = 0;
					}
					return true;
				}
				else
				{
					fileKey = null;
					return false;
				}
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				handleException(e);
				retry++;
				if(retry == (MAX_RETRY-1))
					throw e;
			}
		}
		throw new Exception("Network error.");
	}

	/*
	 * this function was created to notify the UI but is not required for now. Not deleted if required again
	 */
	/*private boolean shouldSendProgress()
	{
		int x = progressPercentage / 10;
		if (x < num)
			return false;
		// @GM 'num++' will create a problem in future if with decide to increase "BUFFER_SIZE"(which we will)
		// num++;
		num = x + 1;
		return true;
	}*/

	private String send(String contentRange, byte[] fileBytes)
	{
		if(client == null)
		{
			client = new DefaultHttpClient();
			client.getParams().setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, bufferSize);
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
			long so_timeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT, 180 * 1000l);
			Logger.d("UploadFileTask", "Socket timeout = " + so_timeout);
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, (int) so_timeout);
			client.getParams().setParameter(CoreConnectionPNames.TCP_NODELAY, true);
			client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());
		}
		long time = System.currentTimeMillis();
		Logger.d("UploadFileTask", "Upload URL = " + mUrl.toString());
		HttpPost post = new HttpPost(mUrl.toString());
		String res = null;
		int resCode = 0;
		try
		{
			post.addHeader("Connection", "Keep-Alive");
			post.addHeader("Content-Name", selectedFile.getName());
			post.addHeader("X-Thumbnail-Required", "0");
			post.addHeader("X-SESSION-ID", X_SESSION_ID);
			post.addHeader("X-CONTENT-RANGE", contentRange);
			post.addHeader("Cookie", "user=" + token + ";UID=" + uId);
			AccountUtils.setNoTransform(post);
			Logger.d(getClass().getSimpleName(), "user=" + token + ";UID=" + uId);
			post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

			post.setEntity(new ByteArrayEntity(fileBytes));

			client.getConnectionManager().getSchemeRegistry().register(getHttpScheme());
			HttpResponse response = client.execute(post, httpContext);
			resCode = response.getStatusLine().getStatusCode();
			res = EntityUtils.toString(response.getEntity());
		}
		catch (ConnectTimeoutException ex)
		{
			handleException(ex);
			ex.printStackTrace();
			Logger.e(getClass().getSimpleName(), "FT Upload time out error : " + ex.getMessage());
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_HTTP_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "ConnectTimeoutException : " , ex);
			return null;
		}
		catch (Exception e)
		{
			handleException(e);
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "FT Upload error : " + e.getMessage());
			FTAnalyticEvents.logDevException(FTAnalyticEvents.UPLOAD_HTTP_OPERATION, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "Unknown exception : ", e);

			if(e.getMessage() == null)
			{
				error();
				res = null;
				retry = false;
			}
			return null;
		}
		if (resCode != 0 && resCode != RESPONSE_OK && resCode != RESPONSE_ACCEPTED)
		{
			error();
			res = null;
			if (retryAttempts >= MAX_RETRY_ATTEMPTS || resCode == RESPONSE_BAD_REQUEST || resCode == RESPONSE_NOT_FOUND)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_HTTP_OPERATION, resCode, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "Upload stopped");
				retry = false;
			}
			else if (resCode == INTERNAL_SERVER_ERROR)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_HTTP_OPERATION, resCode, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "INTERNAL_SERVER_ERROR");
				deleteStateFile();
				_state = FTState.IN_PROGRESS;
				freshStart = true;
			}
			else if (resCode >= 400)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_HTTP_OPERATION, resCode, FTAnalyticEvents.UPLOAD_FILE_TASK, "http", "Response code greater than 400");
				deleteStateFile();
				_state = FTState.IN_PROGRESS;
				retry = false;
			}
		}
		time = System.currentTimeMillis() - time;
		boolean isCompleted = resCode == RESPONSE_OK ? true : false;
		int netType = Utils.getNetworkType(context);
		if (resCode == RESPONSE_OK || resCode == RESPONSE_ACCEPTED)
		{
			String fileExtension = Utils.getFileExtension(selectedFile.getPath());
			String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			FTAnalyticEvents.logFTProcessingTime(FTAnalyticEvents.UPLOAD_FILE_TASK, X_SESSION_ID, isCompleted, fileBytes.length, time, contentRange, netType, fileType);
		}
		Logger.d(getClass().getSimpleName(), "Upload time: " + time / 1000 + "." + time % 1000 + "s.  Response: " + resCode);
		return res;
	}

	private Scheme getHttpScheme()
	{
		Scheme scheme = null;
		if (AccountUtils.ssl)
		{
				KeyStore dummyTrustStore;
				try
				{
					dummyTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					dummyTrustStore.load(null, null);
					SSLSocketFactory sf = new CustomSSLSocketFactory(dummyTrustStore);
					HikeApacheHostNameVerifier hostVerifier = new HikeApacheHostNameVerifier();
					hostVerifier.setFTHostIps(FileTransferManager.getInstance(context).getFTHostUris());
					sf.setHostnameVerifier(hostVerifier);
					scheme = new Scheme("https", sf, AccountUtils.port);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return null;
				}
		}
		else
		{
			scheme = new Scheme("http", PlainSocketFactory.getSocketFactory(), AccountUtils.port);
		}
		return scheme;
	}

	private void error()
	{
		_state = FTState.ERROR;
		saveFileState(X_SESSION_ID);
	}

	public void postExecute(FTResult result)
	{
		Logger.d(getClass().getSimpleName(), "PostExecute--> Thread Details : " + Thread.currentThread().toString() + "Time : " + System.currentTimeMillis() / 1000);
		Logger.d(getClass().getSimpleName(), result.toString());
		if (userContext != null)
		{
			removeTask();
			this.pausedProgress = -1;
			if(result != FTResult.PAUSED)
			{
					HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			}
		}

		if (result != FTResult.PAUSED && result != FTResult.SUCCESS)
		{
			final int errorStringId = result == FTResult.READ_FAIL ? R.string.unable_to_read : result == FTResult.CANCELLED ? R.string.upload_cancelled
					: result == FTResult.FAILED_UNRECOVERABLE ? R.string.upload_failed : result == FTResult.FILE_SIZE_EXCEEDING ? R.string.max_file_size
							: result == FTResult.CARD_UNMOUNT ? R.string.card_unmount : result == FTResult.DOWNLOAD_FAILED ? R.string.download_failed : R.string.upload_failed;

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	private void removeTask()
	{
		if (isMultiMsg)
		{
			for (ConvMessage msg: messageList)
			{
				FileTransferManager.getInstance(context).removeTask(msg.getMsgID());
			}
		}
		else
		{
			FileTransferManager.getInstance(context).removeTask(((ConvMessage) userContext).getMsgID());
		}
	}
	
	private String getImageQuality(){
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_DEFAULT);
		String imageQuality = ImageQuality.IMAGE_QUALITY_DEFAULT;
		switch (quality)
		{
		case ImageQuality.QUALITY_ORIGINAL:
			imageQuality = ImageQuality.IMAGE_QUALITY_ORIGINAL;
			break;
		case ImageQuality.QUALITY_MEDIUM:
			imageQuality = ImageQuality.IMAGE_QUALITY_MEDIUM;
			break;
		case ImageQuality.QUALITY_SMALL:
			imageQuality = ImageQuality.IMAGE_QUALITY_SMALL;
			break;
		}
		return imageQuality;
	}
	
	private JSONObject verifyMD5(File mfile) throws Exception
	{
		String fileMD5 = Utils.fileToMD5(mfile.getAbsolutePath());

		// If we are not able to verify the md5 validity from the server, fall back to uploading the file
		final int MAX_RETRY = 3;
		int retry = 0;
		URL baseUrl = mUrl = new URL(AccountUtils.fastFileUploadUrl + fileMD5);
		while (retry < MAX_RETRY)
		{
			try
			{
				mUrl = getUpdatedURL(mUrl, "VerifyMd5", FTAnalyticEvents.UPLOAD_FILE_TASK, baseUrl);
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
				client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());
				client.getConnectionManager().getSchemeRegistry().register(getHttpScheme());
				HttpHead head = new HttpHead(mUrl.toString());
				AccountUtils.setNoTransform(head);

				HttpResponse resp = client.execute(head);
				int resCode = resp.getStatusLine().getStatusCode();
				// Make sure the response code is 200.
				JSONObject responseJson;
				
				if (resCode == RESPONSE_OK)
				{
					try
					{
						responseJson = new JSONObject();
						JSONObject resData = new JSONObject();
						resData.put(HikeConstants.FILE_KEY, resp.getFirstHeader(HikeConstants.FILE_KEY).getValue());
						resData.put(HikeConstants.CONTENT_TYPE, resp.getFirstHeader(HikeConstants.CONTENT_TYPE).getValue());
						resData.put(HikeConstants.FILE_SIZE, resp.getFirstHeader(HikeConstants.FILE_SIZE).getValue());
						resData.put(HikeConstants.FILE_NAME, resp.getFirstHeader(HikeConstants.FILE_NAME).getValue());
						responseJson.put(HikeConstants.DATA_2, resData);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						responseJson = null;
					}
					return responseJson;
				}
				else
				{
					return null;
				}
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				handleException(e);
				retry++;
				if (retry == MAX_RETRY)
					throw e;
			}
		}
		throw new Exception("Network error.");
	}

	private void saveStateOnNoInternet()
	{
		HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
		FileSavedState fst = FileTransferManager.getInstance(context).getUploadFileState(hikeFile.getFile(), msgId);
		if(fst.getSessionId() == null)
		{
			_state = FTState.ERROR;
			stateFile = getStateFile((ConvMessage) userContext);
			saveFileKeyState(fileKey);
			fileKey = null;
		}
	}
}
