package com.bsb.hike.filetransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;

public class FTAnalyticEvents
{
	public int mRetryCount;

	public int mPauseCount;

	public String mNetwork;

	public int mAttachementType;

	public static final String FT_RETRY_COUNT = "rc";

	public static final String FT_PAUSE_COUNT = "pc";

	public static final String FT_NETWORK_TYPE = "nw";

	public static final String FT_ATTACHEMENT_TYPE = "at";

	public static final String FT_STATUS = "s";

	private static final String VIDEO_INPUT_RESOLUTION = "inputRes";

	private static final String VIDEO_OUTPUT_RESOLUTION = "outRes";

	private static final String VIDEO_INPUT_SIZE = "inputSize";

	private static final String VIDEO_OUTPUT_SIZE = "outSize";

	private static final String VIDEO_COMPRESS_STATE = "vidCompSt";

	private static final String VIDEO_COMPRESSION = "videoCompression";

	private static final String QUICK_UPLOAD = "quickUpload";

	private static final String QUICK_UPLOAD_STATUS = "quSt";
	
	public static final int FT_SUCCESS = 0;

	public static final int FT_FAILED = 1;

	public static final int GALLERY_ATTACHEMENT = 0;

	public static final int FILE_ATTACHEMENT = 1;

	public static final int VIDEO_ATTACHEMENT = 2;

	public static final int CAMERA_ATTACHEMENT = 3;

	public static final int AUDIO_ATTACHEMENT = 4;

	public static final int DOWNLOAD_ATTACHEMENT = 5;

	public static final int OTHER_ATTACHEMENT = 6;
	
	public FTAnalyticEvents(JSONObject logMetaData)
	{
		if(logMetaData == null)
			return;
		try
		{
			this.mAttachementType = logMetaData.getInt(FT_ATTACHEMENT_TYPE);
			this.mNetwork = logMetaData.getString(FT_NETWORK_TYPE);
			this.mRetryCount = logMetaData.getInt(FT_RETRY_COUNT);
			this.mPauseCount = logMetaData.getInt(FT_PAUSE_COUNT);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public FTAnalyticEvents()
	{
	}

	public void saveAnalyticEvent(File logEventFile)
	{
		FileWriter file = null;
		try
		{
			file = new FileWriter(logEventFile);
			JSONObject metadata = new JSONObject();
			metadata.put(FT_ATTACHEMENT_TYPE, this.mAttachementType);
			metadata.put(FT_NETWORK_TYPE, this.mNetwork);
			metadata.put(FT_RETRY_COUNT, this.mRetryCount);
			metadata.put(FT_PAUSE_COUNT, this.mPauseCount);
			Logger.d("FTAnalyticEvents", "write data = " + metadata.toString());
			file.write(metadata.toString());
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(file != null)
					file.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static FTAnalyticEvents getAnalyticEvents(File mFile)
	{
		if (mFile == null || !mFile.exists())
			return new FTAnalyticEvents();

		FileReader file = null;
		StringBuffer sb = new StringBuffer();
		BufferedReader bufReader = null;
		FTAnalyticEvents ftAnalyticEvent = null;
		try
		{
			file = new FileReader(mFile);
			bufReader = new BufferedReader(file);
			int s = 0;
			while ((s = bufReader.read())!=-1) {
	            sb.append((char)s);
			}
			JSONObject data = new JSONObject(sb.toString());
			ftAnalyticEvent = new FTAnalyticEvents(data);
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(bufReader != null)
					bufReader.close();
				if(file != null)
					file.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return ftAnalyticEvent != null ? ftAnalyticEvent : new FTAnalyticEvents();
	}

	/*
	 * We send an event every time user transfer file whether it is succeeded or canceled.
	 */
	public void sendFTSuccessFailureEvent(String network,  int fileSize, int status)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(FT_ATTACHEMENT_TYPE, this.mAttachementType);
			metadata.put(FT_NETWORK_TYPE, network);
			metadata.put(FT_RETRY_COUNT, this.mRetryCount);
			metadata.put(FT_PAUSE_COUNT, this.mPauseCount);
			metadata.put(HikeConstants.FILE_SIZE, fileSize);
			metadata.put(FT_STATUS, status);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, EventPriority.HIGH, metadata, HikeConstants.LogEvent.FILE_TRANSFER_STATUS);			
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json while logging FT send status.", e);
		}
	}
	
	/*
	 * Send an event for video compression
	 */
	public static void sendVideoCompressionEvent(String inputRes, String outRes, int inputSize, int outSize, int compressedState)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(VIDEO_INPUT_RESOLUTION, inputRes);
			metadata.put(VIDEO_OUTPUT_RESOLUTION, outRes);
			metadata.put(VIDEO_INPUT_SIZE, inputSize);
			metadata.put(VIDEO_OUTPUT_SIZE, outSize);
			metadata.put(VIDEO_COMPRESS_STATE, compressedState);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, VIDEO_COMPRESSION, EventPriority.HIGH, metadata, VIDEO_COMPRESSION);			
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json while video compression", e);
		}
	}

	/*
	 * Send an event for video compression
	 */
	public static void sendQuickUploadEvent(int quickUploadStatus)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(QUICK_UPLOAD_STATUS, quickUploadStatus);			
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, EventPriority.HIGH, metadata, QUICK_UPLOAD);			
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json while video compression", e);
		}
	}

	public String toString()
	{
		return "AttachementType : " + mAttachementType + ", NetworkType : " + mNetwork + ", RetryCount : " + mRetryCount + ", PauseCount : " + mPauseCount;
	}
}
