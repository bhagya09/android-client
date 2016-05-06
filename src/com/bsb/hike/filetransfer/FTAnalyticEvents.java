package com.bsb.hike.filetransfer;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FTAnalyticEvents
{
	public String mNetwork;

	public int mAttachementType;

	public static final String FT_NETWORK_TYPE = "con";

	public static final String FT_ATTACHEMENT_TYPE = "at";

	public static final String FT_ATTACHEMENT_SHARED_AS = "atsrc";

	public static final String FT_STATUS = "s";

	private static final String VIDEO_INPUT_RESOLUTION = "inputRes";

	private static final String VIDEO_OUTPUT_RESOLUTION = "outRes";

	private static final String VIDEO_INPUT_SIZE = "inputSize";

	private static final String VIDEO_OUTPUT_SIZE = "outSize";

	private static final String VIDEO_COMPRESS_STATE = "vidCompSt";

	private static final String VIDEO_COMPRESSION = "videoCompression";

	private static final String TIME_TO_COMPRESS_VIDEO = "videoCompTime";

	private static final String QUICK_UPLOAD = "quickUpload";

	private static final String QUICK_UPLOAD_STATUS = "quSt";

	private static final String FT_TASK_NAME = "tn";

	public static final String DOWNLOAD_FILE_TASK = "download";

	public static final String UPLOAD_FILE_TASK = "upload";

	private static final String FT_ERROR_MESSAGE = "ftem";
	
	public static final int FT_SUCCESS = 0;

	public static final int FT_FAILED = 1;

	public static final int GALLERY_ATTACHEMENT = 0;

	public static final int FILE_ATTACHEMENT = 1;

	public static final int VIDEO_ATTACHEMENT = 2;

	public static final int CAMERA_ATTACHEMENT = 3;

	public static final int AUDIO_ATTACHEMENT = 4;

	public static final int DOWNLOAD_ATTACHEMENT = 5;

	public static final int OTHER_ATTACHEMENT = 6;

	private static final String FTR_PRODUCT_AREA = "ftr";

	private static final String FTR_EXCEPTION_ANALYTICS = "ex";

	private static final String FTR_ERROR_ANALYTICS = "ex";

	private static final String RESPONSE_CODE = "resCode";

	private static final String FTR_TASK_TYPE = "taskType";

	private static final String FTR_OPERATION_TYPE = "opType";

	public static final String DOWNLOAD_INIT_1_1 = "download_init_1_1";
	public static final String DOWNLOAD_INIT_1_2 = "download_init_1_2";
	public static final String DOWNLOAD_INIT_1_3 = "download_init_1_3";

	public static final String DOWNLOAD_INIT_2_1 = "download_init_2_1";
	public static final String DOWNLOAD_INIT_2_2 = "download_init_2_2";

	public static final String DOWNLOAD_CONN_INIT_1 = "download_conn_init_1";

	public static final String DOWNLOAD_CONN_INIT_2_1 = "download_conn_init_2_1";
	public static final String DOWNLOAD_CONN_INIT_2_2 = "download_conn_init_2_2";

	public static final String DOWNLOAD_MEM_CHECK = "download_mem_check";

	public static final String DOWNLOAD_DATA_WRITE = "download_data_write";

	public static final String DOWNLOAD_STATE_CHANGE = "download_state_change";

	public static final String DOWNLOAD_RENAME_FILE = "download_rename_file";

	public static final String DOWNLOAD_UNKNOWN_ERROR = "download_unknown_error";

	public static final String DOWNLOAD_CLOSING_STREAM = "download_closing_streams";

	public static final String UPLOAD_INIT_1_1 = "upload_init_1_1";
	public static final String UPLOAD_INIT_1_2 = "upload_init_1_2";
	public static final String UPLOAD_INIT_1_3 = "upload_init_1_3";
	public static final String UPLOAD_INIT_1_4 = "upload_init_1_4";

	public static final String UPLOAD_INIT_2_1 = "upload_init_2_1";
	public static final String UPLOAD_INIT_2_2 = "upload_init_2_2";
	public static final String UPLOAD_INIT_2_3 = "upload_init_2_3";
	public static final String UPLOAD_INIT_2_4 = "upload_init_2_4";
	public static final String UPLOAD_INIT_2_5 = "upload_init_2_5";
	public static final String UPLOAD_INIT_2_6 = "upload_init_2_6";

	public static final String UPLOAD_INIT_3 = "upload_init_3";

	public static final String UPLOAD_INIT_4_1 = "upload_init_4_1";
	public static final String UPLOAD_INIT_4_2 = "upload_init_4_2";

	public static final String UPLOAD_INIT_5 = "upload_init_5";

	public static final String UPLOAD_INIT_6 = "upload_init_6";

	public static final String UPLOAD_INIT_7_1 = "upload_init_7_1";
	public static final String UPLOAD_INIT_7_2 = "upload_init_7_2";

	public static final String UPLOAD_FTR_INIT_1 = "upload_ftr_init_1";

	public static final String UPLOAD_FTR_INIT_2_1 = "upload_ftr_init_2_1";
	public static final String UPLOAD_FTR_INIT_2_2 = "upload_ftr_init_2_2";

	public static final String UPLOAD_FTR_INIT_3_1 = "upload_ftr_init_3_1";
	public static final String UPLOAD_FTR_INIT_3_2 = "upload_ftr_init_3_2";

	public static final String UPLOAD_FTR_INIT_4 = "upload_ftr_init_4";

	public static final String UPLOAD_FK_VALIDATION = "upload_fk_validation";

	public static final String UPLOAD_FILE_OPERATION = "upload_file_operation";

	public static final String UPLOAD_CALLBACK_AREA_1_1 = "upload_callback_area_1_1";
	public static final String UPLOAD_CALLBACK_AREA_1_2 = "upload_callback_area_1_2";
	public static final String UPLOAD_CALLBACK_AREA_1_3 = "upload_callback_area_1_3";
	public static final String UPLOAD_CALLBACK_AREA_1_4 = "upload_callback_area_1_4";
	public static final String UPLOAD_CALLBACK_AREA_1_5 = "upload_callback_area_1_5";
	public static final String UPLOAD_CALLBACK_AREA_1_6 = "upload_callback_area_1_6";

	public static final String UPLOAD_QUICK_AREA = "upload_quick_area";

	public static final String UPLOAD_CALLBACK_AREA_2 = "upload_callback_area_2";

	public static final String UPLOAD_FILE_READ = "upload_file_read";

	public static final String UPLOAD_RETRY_COMPLETE = "upload_retry_complete";

	public static final String UPLOAD_HTTP_OPERATION = "upload_http_operation";

	public static final String UNABLE_TO_START_ACTIVITY = "unable_to_start_activity";

	public static final String UNABLE_TO_CREATE_HIKE_TEMP_DIR = "unable_to_hike_temp_dir";

	public static final String BAD_RESUME_LENGTH = "bad_resume_len";

	public static final String JSON_PARSING_ISSUE = "json_parsing_issue";

	public static final String HOST_FALLBACK = "host_fallback";

	public static final String FT_STATE_READ_FAIL = "ft_state_read_fail";

	public static final String FT_BENCH_MARK = "rel_ft";

	public static final String FT_PROCESSING_TIME = "ft_pt";

	public static final String FT_CHUNK_SIZE = "ft_cs";

	public static final String FT_COMPLETED = "ft_c";

	public static final String FT_FILE_ID = "ft_fId";

	public static final String FT_CONTENT_RANGE = "ft_cr";

	public static final String FT_FILE_TYPE = "ft_fileType";
	
	public static final int APK_ATTACHMENT = 7;
	
	public FTAnalyticEvents(JSONObject logMetaData)
	{
		if(logMetaData == null)
			return;
		try
		{
			this.mAttachementType = logMetaData.getInt(FT_ATTACHEMENT_TYPE);
			this.mNetwork = logMetaData.getString(FT_NETWORK_TYPE);
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
			Utils.closeStreams(file);
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
			Utils.closeStreams(bufReader, file);
		}
		return ftAnalyticEvent != null ? ftAnalyticEvent : new FTAnalyticEvents();
	}

	/*
	 * We send an event every time user transfer file whether it is succeeded or canceled.
	 */
	public void sendFTSuccessFailureEvent(String network,  long fileSize, int status, String attachmentShardeAs, int mAttachementType)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(FT_ATTACHEMENT_TYPE, mAttachementType);
			metadata.put(FT_NETWORK_TYPE, network);
			metadata.put(HikeConstants.FILE_SIZE, fileSize);
			metadata.put(FT_STATUS, status);
			if(!TextUtils.isEmpty(attachmentShardeAs)) {
				metadata.put(FT_ATTACHEMENT_SHARED_AS, attachmentShardeAs);
			}
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, EventPriority.HIGH, metadata, HikeConstants.LogEvent.FILE_TRANSFER_STATUS);			
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json while logging FT send status.", e);
		}
	}

	public static void sendVideoCompressionEvent(String inputRes, String outRes, long inputSize, long outSize, int compressedState){
		sendVideoCompressionEvent(inputRes, outRes, inputSize, outSize, compressedState, 0);
	}

	/*
	 * Send an event for video compression
	 */
	public static void sendVideoCompressionEvent(String inputRes, String outRes, long inputSize, long outSize, int compressedState, long timeToCompress)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(VIDEO_INPUT_RESOLUTION, inputRes);
			metadata.put(VIDEO_OUTPUT_RESOLUTION, outRes);
			metadata.put(VIDEO_INPUT_SIZE, inputSize);
			metadata.put(VIDEO_OUTPUT_SIZE, outSize);
			metadata.put(VIDEO_COMPRESS_STATE, compressedState);
			if(compressedState != 0) metadata.put(TIME_TO_COMPRESS_VIDEO,timeToCompress);
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

	public static void sendFTDevEvent(String taskName, String errorMsg)
	{
		JSONObject error = new JSONObject();
		try {
			error.put(FT_TASK_NAME, taskName);
			error.put(FT_ERROR_MESSAGE, errorMsg);
			HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.FILE_TRANSFER, EventPriority.HIGH, error);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	+	 * Logs the dev exception for every error/exception in FTR 
	+	 * 
	+	 * @param devArea
	+	 * @param responseCode
	+	 * @param taskType
	+	 * @param operation
	+	 * @param exception
	+	 */
	public static void logDevException(String devArea, int responseCode, String taskType, String operation, String errorMsg, Throwable exception) 
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OTHER_EXCEPTION_LOGGING, false))
			return;

		JSONObject info = new JSONObject();
		try {
			info.put(RESPONSE_CODE, responseCode);
			info.put(FTR_TASK_TYPE, taskType);
			if (!TextUtils.isEmpty(operation)) {
				info.put(FTR_OPERATION_TYPE, operation);
			}
			String stackTrace = Utils.getStackTrace(exception);
			if (!TextUtils.isEmpty(stackTrace)) {
				info.put(FTR_EXCEPTION_ANALYTICS, errorMsg + stackTrace);
			}
			HAManager.getInstance().logDevEvent(FTR_PRODUCT_AREA, devArea, info);
		} catch (JSONException e) {
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "FTR : Exception occurred while logging dev exception log : "+ e);
		}
	}

	/**
	+	 * Logs the dev exception for every error/exception in FTR 
	+	 * 
	+	 * @param devArea
	+	 * @param responseCode
	+	 * @param taskType
	+	 * @param operation
	+	 * @param errorMsg
	+	 */
	public static void logDevError(String devArea, int responseCode, String taskType, String operation, String errorMsg) 
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OTHER_EXCEPTION_LOGGING, false))
			return;

		JSONObject info = new JSONObject();
		try {
			info.put(RESPONSE_CODE, responseCode);
			info.put(FTR_TASK_TYPE, taskType);
			if (!TextUtils.isEmpty(operation)) {
				info.put(FTR_OPERATION_TYPE, operation);
			}
			if (!TextUtils.isEmpty(errorMsg)) {
				info.put(FTR_ERROR_ANALYTICS, errorMsg);
			}
			HAManager.getInstance().logDevEvent(FTR_PRODUCT_AREA, devArea, info);
		} catch (JSONException e) {
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "FTR : Exception occurred while logging dev exception log : "+ e);
		}
	}

	/**
	* Logs the file transfer processing time for every chunk 
	* 
	* @param taskType
	* @param sessionId
	* @param isCompleted
	* @param chunkSize
	* @param timeTaken
	* @param contentRange
	* @param networkType
	* @param fileType
	*/
	public static void logFTProcessingTime(String taskType, String sessionId, boolean isCompleted, long chunkSize, long timeTaken, String contentRange, int networkType, String fileType) 
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FT_LATENCY_LOGGING, false))
			return;

		JSONObject metadata = new JSONObject();
		try {
			metadata.put(FT_FILE_ID, sessionId);
			metadata.put(FTR_TASK_TYPE, taskType);
			metadata.put(FT_CHUNK_SIZE, chunkSize);
			metadata.put(FT_PROCESSING_TIME, timeTaken);
			metadata.put(FT_COMPLETED, isCompleted);
			metadata.put(FT_CONTENT_RANGE, contentRange);
			metadata.put(FT_NETWORK_TYPE, networkType);
			metadata.put(FT_FILE_TYPE, fileType);
			HAManager.getInstance().record(FT_BENCH_MARK, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, FT_BENCH_MARK);
		} catch (JSONException e) {
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "FTR : Exception occurred while logging processing time : "+ e);
		}
	}

	public String toString()
	{
		return "AttachementType : " + mAttachementType + ", NetworkType : " + mNetwork;
	}

	//Sending File Transfer analytics for bots.
	public static void platformAnalytics(String msisdn,String fileKey, String fileType)
	{
		JSONObject json = new JSONObject();
		try {
			json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT, AnalyticsConstants.BOT_CONTENT_DOWNLAODED);
			json.putOpt(AnalyticsConstants.LOG_FIELD_4, fileKey);
			json.putOpt(AnalyticsConstants.LOG_FIELD_1, fileType);
			json.putOpt(AnalyticsConstants.BOT_MSISDN, msisdn);
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch(JSONException e)
		{
			Logger.e("FileAnalyticsEvent",e.toString());
		}
	}
}
