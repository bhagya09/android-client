package com.bsb.hike.models;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.BitmapModule.RecyclingBitmapDrawable;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.video.VideoUtilities.VideoEditedInfo;

public class HikeFile
{
	public static enum HikeFileType
	{
		PROFILE, IMAGE, VIDEO, AUDIO, LOCATION, CONTACT, AUDIO_RECORDING, OTHER,APK;

		public static HikeFileType fromString(String fileTypeString)
		{
			return fromString(fileTypeString, false);
		}

		public static HikeFileType fromString(String fileTypeString, boolean isRecording)
		{
			if (!TextUtils.isEmpty(fileTypeString))
			{
				if (fileTypeString.startsWith("video"))
				{
					return HikeFileType.VIDEO;
				}
				else if (fileTypeString.startsWith("audio"))
				{
					return isRecording ? HikeFileType.AUDIO_RECORDING : HikeFileType.AUDIO;
				}
				else if (fileTypeString.startsWith(HikeConstants.LOCATION_CONTENT_TYPE))
				{
					return HikeFileType.LOCATION;
				}
				else if (fileTypeString.startsWith("image"))
				{
					return HikeFileType.IMAGE;
				}
				else if (fileTypeString.startsWith(HikeConstants.CONTACT_CONTENT_TYPE))
				{
					return HikeFileType.CONTACT;
				}
				else if (fileTypeString.contains("package-archive"))	
				{
					return HikeFileType.APK;
				}
			}
			return HikeFileType.OTHER;
		}

		public static HikeFileType fromFilePath(String filepath,boolean isRecording)
		{
			String fileExtension = Utils.getFileExtension(filepath);
			String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			return HikeFileType.fromString(fileType, isRecording);
		}
		
		public static String toString(HikeFileType hikeFileType)
		{
			if (hikeFileType == PROFILE || hikeFileType == IMAGE)
			{
				return "image/*";
			}
			else if (hikeFileType == VIDEO)
			{
				return "video/*";
			}
			else if (hikeFileType == AUDIO || hikeFileType == AUDIO_RECORDING)
			{
				return "audio/*";
			}
			else if (hikeFileType == LOCATION)
			{
				return HikeConstants.LOCATION_CONTENT_TYPE;
			}
			else if (hikeFileType == CONTACT)
			{
				return HikeConstants.CONTACT_CONTENT_TYPE;
			}
			else if (hikeFileType == OTHER)
			{
				return "application/octet-stream";
			}
			else if (hikeFileType == APK)
			{
				return "application/vnd.android.package-archive";
			}
			return null;
		}

		public static String getFileTypeMessage(Context context, HikeFileType hikeFileType, boolean isSent)
		{
			if (hikeFileType == PROFILE || hikeFileType == IMAGE)
			{
				return isSent ? context.getString(R.string.image_msg_sent) : context.getString(R.string.image_msg_received);
			}
			else if (hikeFileType == VIDEO)
			{
				return isSent ? context.getString(R.string.video_msg_sent) : context.getString(R.string.video_msg_received);
			}
			else if (hikeFileType == AUDIO)
			{
				return isSent ? context.getString(R.string.audio_msg_sent) : context.getString(R.string.audio_msg_received);
			}
			else if (hikeFileType == LOCATION)
			{
				return isSent ? context.getString(R.string.location_msg_sent) : context.getString(R.string.location_msg_received);
			}
			else if (hikeFileType == CONTACT)
			{
				return isSent ? context.getString(R.string.contact_msg_sent) : context.getString(R.string.contact_msg_received);
			}
			else if (hikeFileType == AUDIO_RECORDING)
			{
				return isSent ? context.getString(R.string.audio_recording_msg_sent) : context.getString(R.string.audio_recording_msg_received);
			}
			return isSent ? context.getString(R.string.file_msg_sent) : context.getString(R.string.file_msg_received);
		}
	}

	private String fileName;

	private String fileTypeString;

	private String thumbnailString;

	private Drawable thumbnail;

	private String fileKey;

	private long fileSize;

	private HikeFileType hikeFileType;

	private File file;

	private long recordingDuration = -1;

	private String sourceFilePath;

	private double latitude;

	private double longitude;

	private int zoomLevel;

	private String address;

	private String displayName;

	private JSONArray phoneNumbers;

	private JSONArray emails;

	private JSONArray addresses;

	private JSONArray events;

	private boolean isSent;
	
	private String img_quality;
	
	private String downloadURL;

	private VideoEditedInfo vEditInfo;
	
	private HikeFileMetadata metadata;

	private HikeFile()
	{
		metadata = new HikeFileMetadata();
	}

	private int attachmentType = -1;

	public HikeFile(String fileName, String fileTypeString, String fileKey, long fileSize, String downloadUrl)
	{
		this();
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.fileKey = fileKey;
		this.fileSize = fileSize;
		this.downloadURL = downloadUrl;
		this.hikeFileType = HikeFileType.fromString(fileTypeString, recordingDuration != -1);
		this.isSent = false;
	}

	public HikeFile(JSONObject fileJSON, boolean isSent)
	{
		this();
		this.fileName = fileJSON.optString(HikeConstants.FILE_NAME);
		this.fileTypeString = fileJSON.optString(HikeConstants.CONTENT_TYPE);
		this.thumbnailString = fileJSON.optString(HikeConstants.THUMBNAIL, null);
		this.thumbnail = thumbnail == null ? makeThumbnailFromString(thumbnailString) : thumbnail;
		this.sourceFilePath = fileJSON.optString(HikeConstants.SOURCE_FILE_PATH);
		if(isSent)
		{
			this.file = new File(fileJSON.optString(HikeConstants.FILE_PATH));
		}
		this.fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
		this.fileSize = fileJSON.optLong(HikeConstants.FILE_SIZE);
		this.latitude = fileJSON.optDouble(HikeConstants.LATITUDE);
		this.longitude = fileJSON.optDouble(HikeConstants.LONGITUDE);
		this.zoomLevel = fileJSON.optInt(HikeConstants.ZOOM_LEVEL, HikeConstants.DEFAULT_ZOOM_LEVEL);
		this.address = fileJSON.optString(HikeConstants.ADDRESS);
		if (this.file != null && (TextUtils.isEmpty(this.fileName) || !isSent))
		{
			// Update the file name to prevent duplicacy
			this.fileName = this.file.getName();
		}
		this.displayName = fileJSON.optString(HikeConstants.NAME);
		this.phoneNumbers = fileJSON.optJSONArray(HikeConstants.PHONE_NUMBERS);
		this.emails = fileJSON.optJSONArray(HikeConstants.EMAILS);
		this.addresses = fileJSON.optJSONArray(HikeConstants.ADDRESSES);
		this.events = fileJSON.optJSONArray(HikeConstants.EVENTS);
		this.recordingDuration = fileJSON.optLong(HikeConstants.PLAYTIME, -1);
		this.hikeFileType = HikeFileType.fromString(fileTypeString, recordingDuration != -1);
		this.isSent = isSent;
		this.img_quality = fileJSON.optString(HikeConstants.FILE_IMAGE_QUALITY, null);
		if(!isSent)
		{
			this.downloadURL = fileJSON.optString(HikeConstants.DOWNLOAD_FILE_URL_KEY);
		}
		if(fileJSON.has(FTAnalyticEvents.FT_ATTACHEMENT_TYPE))
		{
			this.attachmentType = fileJSON.optInt(FTAnalyticEvents.FT_ATTACHEMENT_TYPE);
		}
		setCaption(fileJSON.optString(HikeConstants.CAPTION));
		// this.file = TextUtils.isEmpty(this.fileKey) ? null : Utils
		// .getOutputMediaFile(hikeFileType, fileName);
	}

	public HikeFile(String fileName, String fileTypeString, String thumbnailString, Bitmap thumbnail, long recordingDuration, boolean isSent, String img_quality)
	{
		this();
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString, recordingDuration != -1);
		this.thumbnailString = thumbnailString;
		this.thumbnail = makeThumbnailFromBitmap(thumbnail);
		this.recordingDuration = recordingDuration;
		this.isSent = isSent;
		this.img_quality = img_quality;
	}

	public HikeFile(String fileName, String fileTypeString, String thumbnailString, Bitmap thumbnail, long recordingDuration, String source, long fileSize, boolean isSent,
			String img_quality, int attachmentType)
	{
		this();
		this.fileName = fileName;
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString, recordingDuration != -1);
		this.thumbnailString = thumbnailString;
		this.thumbnail = makeThumbnailFromBitmap(thumbnail);
		this.recordingDuration = recordingDuration;
		this.sourceFilePath = source;
		this.isSent = isSent;
		this.fileSize = fileSize;
		this.img_quality = img_quality;
		this.attachmentType = attachmentType;
	}

	public HikeFile(double latitude, double longitude, int zoomLevel, String address, String thumbnailString, Bitmap thumbnail, boolean isSent)
	{
		this();
		this.fileName = HikeConstants.LOCATION_FILE_NAME;
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
		this.fileTypeString = HikeConstants.LOCATION_CONTENT_TYPE;
		this.hikeFileType = HikeFileType.fromString(fileTypeString);
		this.address = address;
		this.thumbnailString = thumbnailString;
		this.thumbnail = makeThumbnailFromBitmap(thumbnail);
		this.isSent = isSent;
	}

	private Drawable makeThumbnailFromBitmap(Bitmap bitmap)
	{
		Drawable thumbnail = HikeBitmapFactory.getBitmapDrawable(bitmap);
		if (thumbnail instanceof RecyclingBitmapDrawable)
		{
			((RecyclingBitmapDrawable) thumbnail).incrementCacheReference();
		}
		return thumbnail;
	}

	private Drawable makeThumbnailFromString(String thumbnailString)
	{
		Drawable thumbnail = HikeBitmapFactory.stringToDrawable(thumbnailString);
		if (thumbnail instanceof RecyclingBitmapDrawable)
		{
			((RecyclingBitmapDrawable) thumbnail).incrementCacheReference();
		}
		return thumbnail;
	}

	public JSONObject serialize()
	{
		try
		{
			JSONObject fileJSON = new JSONObject();
			fileJSON.putOpt(HikeConstants.CONTENT_TYPE, fileTypeString);
			fileJSON.putOpt(HikeConstants.FILE_NAME, fileName);
			fileJSON.putOpt(HikeConstants.FILE_KEY, fileKey);
			fileJSON.putOpt(HikeConstants.FILE_SIZE, fileSize);
			fileJSON.putOpt(HikeConstants.THUMBNAIL, thumbnailString);
			//Commenting isSent check as file path needs to be populated in case of native card received message.
			if(/*isSent && */(getHikeFileType() != HikeFileType.CONTACT) && (getHikeFileType() != HikeFileType.LOCATION))
			{
				File file = getFile();
				if (file != null)
				{
					fileJSON.putOpt(HikeConstants.FILE_PATH, file.getPath());
				}
			}
			if (sourceFilePath != null)
			{
				fileJSON.putOpt(HikeConstants.SOURCE_FILE_PATH, sourceFilePath);
			}
			if (recordingDuration != -1)
			{
				fileJSON.put(HikeConstants.PLAYTIME, recordingDuration);
			}
			if (HikeConstants.LOCATION_CONTENT_TYPE.equals(fileTypeString))
			{
				fileJSON.putOpt(HikeConstants.LATITUDE, latitude);
				fileJSON.putOpt(HikeConstants.LONGITUDE, longitude);
				fileJSON.putOpt(HikeConstants.ZOOM_LEVEL, zoomLevel);
				fileJSON.putOpt(HikeConstants.ADDRESS, address);
			}
			else if (HikeConstants.CONTACT_CONTENT_TYPE.equals(fileTypeString))
			{
				fileJSON.putOpt(HikeConstants.NAME, displayName);
				fileJSON.putOpt(HikeConstants.PHONE_NUMBERS, phoneNumbers);
				fileJSON.putOpt(HikeConstants.EMAILS, emails);
				fileJSON.putOpt(HikeConstants.ADDRESSES, addresses);
				fileJSON.putOpt(HikeConstants.EVENTS, events);
			}
			if(this.hikeFileType == HikeFileType.IMAGE && this.img_quality != null){
				fileJSON.putOpt(HikeConstants.FILE_IMAGE_QUALITY, this.img_quality);
			}
			if(attachmentType != -1)
			{
				fileJSON.putOpt(FTAnalyticEvents.FT_ATTACHEMENT_TYPE, attachmentType);
			}
			if(downloadURL != null)
			{
				fileJSON.putOpt(HikeConstants.DOWNLOAD_FILE_URL_KEY, downloadURL);
			}

			String attachmentSharedAs = getAttachmentSharedAs();
			if (!TextUtils.isEmpty(attachmentSharedAs)) {
				fileJSON.putOpt(HikeConstants.ATTACHEMENT_SHARED_FROM, attachmentSharedAs);
			}
			return fileJSON;
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return null;
	}

	public String getAttachmentSharedAs() {
		// Differentiate between a walkie-talkie msg and an audio attachment
		String audioSrcType = "";
		if (hikeFileType != null) {
			if (HikeFileType.AUDIO_RECORDING == hikeFileType) {
				audioSrcType = "wktk";
			} else if (HikeFileType.AUDIO == hikeFileType) {
				audioSrcType = "audmsg";
			}
		}
		return audioSrcType;
	}

	public String getExactFilePath()
	{
		if(hikeFileType == HikeFileType.IMAGE || !TextUtils.isEmpty(fileKey))
		{
			return getFilePath();
		}
		else 
		{
			return sourceFilePath;
		}
	}
	
	public String getFileName()
	{
		return fileName;
	}

	public void setFile(File f)
	{
		this.file = f;
	}

	public void setFileTypeString(String fileTypeString)
	{
		this.fileTypeString = fileTypeString;
		this.hikeFileType = HikeFileType.fromString(fileTypeString, recordingDuration != -1);
	}

	public String getFileTypeString()
	{
		return fileTypeString;
	}

	public String getThumbnailString()
	{
		return thumbnailString;
	}

	public void setThumbnail(Drawable thumbnail)
	{
		this.thumbnail = thumbnail;
	}

	public Drawable getThumbnail()
	{
		return thumbnail;
	}

	public void setFileKey(String fileKey)
	{
		this.fileKey = fileKey;
		if (file == null)
			this.file = Utils.getOutputMediaFile(hikeFileType, fileName, isSent);
	}

	public String getFileKey()
	{
		return fileKey;
	}

	public void setFileSize(long fileSize)
	{
		this.fileSize = fileSize;
	}

	public long getFileSize()
	{
		return fileSize;
	}

	public HikeFileType getHikeFileType()
	{
		return hikeFileType;
	}

	public void setHikeFileType(HikeFileType hikeFileType)
	{
		this.hikeFileType = hikeFileType;
	}

	public double getLatitude()
	{
		return latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	public int getZoomLevel()
	{
		return zoomLevel;
	}

	public String getAddress()
	{
		return address;
	}

	public long getRecordingDuration()
	{
		return recordingDuration;
	}

	public void setRecordingDuration(long recordingDuration)
	{
		this.recordingDuration = recordingDuration;
	}

	public int getAttachementType()
	{
		return this.attachmentType;
	}

	public boolean wasFileDownloaded()
	{
		if (hikeFileType == HikeFileType.LOCATION || hikeFileType == HikeFileType.CONTACT)
		{
			return true;
		}
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return false;
		}
		if (file == null)
		{
			File file = Utils.getOutputMediaFile(hikeFileType, fileName, isSent);
			return file == null ? false : file.exists();
		}
		return file.exists();
	}

	public String getFilePath()
	{
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE)
		{
			return null;
		}
		if (file == null)
		{
			file = Utils.getOutputMediaFile(hikeFileType, fileName, isSent);
		}
		if(file != null)
			return file.getPath();
		else
			return null;
	}

	public File getFile()
	{
		if (file == null)
		{
			file = Utils.getOutputMediaFile(hikeFileType, fileName, isSent);
		}
		return file;
	}

	public String getSourceFilePath()
	{
		return sourceFilePath;
	}
	
	public void setSourceFilePath(String sourceFilePath)
	{
		this.sourceFilePath = sourceFilePath;
	}

	public void removeSourceFile()
	{
		sourceFilePath = null;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public JSONArray getPhoneNumbers()
	{
		return phoneNumbers;
	}

	public JSONArray getEmails()
	{
		return emails;
	}

	public JSONArray getAddresses()
	{
		return addresses;
	}

	public JSONArray getEvents()
	{
		return events;
	}

	public void setFileName(String fName)
	{
		fileName = fName;
	}
	
	public void setVideoEditedInfo(VideoEditedInfo vEditInfo)
	{
		this.vEditInfo = vEditInfo;
	}
	/*
	 * Get server configured download url 
	 */
	public String getDownloadURL()
	{
		Logger.d("HikeDownloadURL", "DowloadURL = " + downloadURL);
		return downloadURL;
	}

	public VideoEditedInfo getVideoEditorInfo()
	{
		return this.vEditInfo;
	}

	public boolean isSent()
	{
		return isSent;
	}

	public void setSent(boolean isSent)
	{
		this.isSent = isSent;
	}
	
	/*
	 * this method might return null file object in some cases. So, We always need to use exactFilePathFileExists() method to check weather
	 * actually file exists or not.
	 */
	public File getFileFromExactFilePath()
	{
		String exactFilePath = getExactFilePath();
		/*
		 * Added empty check for exact file path because if file path is empty then application get crashed on creating new file. Cases where exact file path can be empty are : 1)
		 * ExternalStorageState is None. (2) Source file path is null Fogbugz Id : 37242
		 */
		if (!TextUtils.isEmpty(exactFilePath) && (file == null || !file.getAbsolutePath().equals(exactFilePath)))
		{
			file = new File(exactFilePath);
		}
		return file;
	}
	
	public boolean exactFilePathFileExists()
	{
		File file = getFileFromExactFilePath();
		return file != null && file.exists();
	}

	public void shareFile(Context context)
	{
		switch (getHikeFileType())
		{
		case LOCATION:
		case CONTACT:
		case PROFILE:
			return;

		default:
			break;
		}
		/*
		 * getting exact file path to support sharing even not fully uploaded files
		 */
		String currentFileSelectionPath = HikeConstants.FILE_SHARE_PREFIX + getExactFilePath();
		String currentFileSelectionMimeType = getFileTypeString();
		IntentFactory.startShareImageIntent(currentFileSelectionMimeType, currentFileSelectionPath);
	}

	public static void openFile(File file, String fileTypeString, Context context)
	{
		Intent openFile = new Intent(Intent.ACTION_VIEW);
		openFile.setDataAndType(Uri.fromFile(file), fileTypeString);
		try
		{
			context.startActivity(openFile);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w("HikeFile", "Trying to open an unknown format", e);
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
		}
	}

	public static void openFile(HikeFile hikeFile, Context context)
	{
		openFile(hikeFile.getFile(), hikeFile.getFileTypeString(), context);
	}
	
	/**
	 * This method is used to delete HikeFiles from the SDCard/Internal memory of the phone. The method obtains Id from the MediaStore and then deletes files from MediaStore 
	 * 
	 * @param hikeFile
	 */
	
	public void delete(Context context)
	{
		/*
		 * Added check for hike media gallery because it is required to delete the media of that directory only.
		 */
		if(this.getFilePath() == null || !this.getFilePath().startsWith(Utils.getFileParent(this.getHikeFileType(), this.isSent())))
		{
			return;
		}
		String[] retCol = null;
		Uri uri = null;
		int id = -1;
		switch (this.getHikeFileType())
		{
		case IMAGE:
			retCol = new String[] { MediaStore.Images.Media._ID };
			uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			id = getMediaId(this.getFilePath(), retCol, uri, context);
			break;
		case VIDEO:
			retCol = new String[] { MediaStore.Video.Media._ID };
			uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
			id = getMediaId(this.getFilePath(), retCol, uri, context);
			break;
		default:
			break;
		}

		if (id != -1)
		{
			context.getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
		}

		if (this.exactFilePathFileExists())
		{
			this.getFileFromExactFilePath().delete();
		}
	}
	
	/**
	 * This method is used to query the internal Media Db of Android for the index of a media file.
	 * 
	 * @param filePath
	 * @return The index of the Media from the relevant table if present or -1 if not present/the params supplied are null.
	 */
	
	public static int getMediaId(String filePath, String[] retCol, Uri uri, Context context)
	{
		int id = -1;
		if (retCol == null || uri == null || filePath == null)
		{
			return -1;
		}
		Cursor cur = context.getContentResolver().query(uri, retCol, MediaStore.MediaColumns.DATA + "=?", new String[] { filePath }, null);
		if (cur != null)
		{
			try
			{
				if (cur.getCount() == 0)
				{
					return -1;
				}
				cur.moveToFirst();

				id = cur.getInt(cur.getColumnIndex(MediaStore.MediaColumns._ID));
			}
			finally
			{
				cur.close();
			}
		}
		return id;
	}
	
	public void setCaption(String caption)
	{
		metadata.setCaption(caption);
	}
	
	public String getCaption()
	{
		return metadata.getCaption();
	}
	
}
