/**
 * 
 */
package com.bsb.hike.ces.ft;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.ces.CesConstants;
import com.bsb.hike.ces.CesDataInfoFormatBuilder;
import com.bsb.hike.ces.CesUtils;
import com.bsb.hike.utils.Logger;


/**
 * @author suyash
 *
 */
public class FTDataInfoFormatBuilder < B extends FTDataInfoFormatBuilder<B> > extends CesDataInfoFormatBuilder<B>
{
	private final String TAG = "FTDataInfoFormatBuilder";
	private String ft_unique_Id; //msgId + uid
	private String fileType;
	private String ftSessionId;
	private String stackTrace;
	private String ft_type; // upload or download
	private boolean isManualRetry;
	private boolean file_available_on_server; // success or fail
	private int chunk_number; // which number of chunk is getting uploaded
	private int ft_status; // complete or fail
	private long procTime;
	private long networkProcTime;
	private long fileSize;
	private long chunkSize;
	private final long SIZE_100KB = 100 * 1024;
	private final long SIZE_200KB = 200 * 1024;
	private final long SIZE_300KB = 300 * 1024;
	private final long SIZE_500KB = 500 * 1024;
	private final long SIZE_1MB = 1 * 1024 * 1024;
	private final long SIZE_5MB = 5 * 1024 * 1024;
	
	public B setUniqueId(String ft_unique_Id)
	{
		this.ft_unique_Id = ft_unique_Id;
		return self();
	}

	public B setSessionId(String ftSessionId)
	{
		this.ftSessionId = ftSessionId;
		return self();
	}

	public B setFileType(String fileType)
	{
		this.fileType = fileType;
		return self();
	}

	public B setStackTrace(String stackTrace)
	{
		this.stackTrace = stackTrace;
		return self();
	}

	public B setFTTaskType(String ft_type)
	{
		this.ft_type = ft_type;
		return self();
	}

	public B setManualRetry(boolean isManualRetry)
	{
		this.isManualRetry = isManualRetry;
		return self();
	}

	public B setFileAvailability(boolean file_available_on_server)
	{
		this.file_available_on_server = file_available_on_server;
		return self();
	}

	public B setWhichChunk(int chunk_number)
	{
		this.chunk_number = chunk_number;
		return self();
	}

	public B setFTStatus(int ft_status)
	{
		this.ft_status = ft_status;
		return self();
	}

	public B setProcTime(long procTime)
	{
		this.procTime = procTime;
		return self();
	}

	public B setNetProcTime(long networkProcTime)
	{
		this.networkProcTime = networkProcTime;
		return self();
	}

	public B setFileSize(long fileSize)
	{
		this.fileSize = fileSize;
		return self();
	}

	public B setChunkSize(long chunkSize)
	{
		this.chunkSize = chunkSize;
		return self();
	}

	@Override
	protected B self()
	{
		return (B) this;
	}

	/*
	 * L1 Format
	 * 
	 *   {"module": "m1", "network": 1, "bucket": "b0", "file_size": 1203, "proc_time": 100, "speed": 12.03, "manual_retry-count"; 4, "fti": 1, "fas": 0}

		final format - {"v": "m1", "nw": 1, "f": "b0", "vi": 1203, "us": 100, "ra": 12.03, "b"; 4, "d": 1, "sec": 0, "s" : "l1" }
	 */
	@Override
	public JSONObject buildLevelOneInfo()
	{
		JSONObject l1Data = null;
		try {
			l1Data = new JSONObject();
			l1Data.put(AnalyticsConstants.V2.VARIETY, CesConstants.FT_MODULE);
			l1Data.put(AnalyticsConstants.V2.VAL_STR, this.ft_unique_Id == null ? "" : this.ft_unique_Id );
			l1Data.put(AnalyticsConstants.V2.NETWORK, this.netType == null ? "-1" : this.netType);
			l1Data.put(AnalyticsConstants.V2.FORM, this.getSizeBucket(this.fileSize));
			l1Data.put(AnalyticsConstants.V2.VAL_INT, this.fileSize);
			l1Data.put(AnalyticsConstants.V2.USER_STATE, this.procTime);
			l1Data.put(AnalyticsConstants.V2.RACE, CesUtils.getSpeedInKbps(this.fileSize, this.procTime));
			l1Data.put(AnalyticsConstants.V2.BREED, this.isManualRetry ? 1 : 0);
			l1Data.put(CesConstants.AnalyticsV2.DIVISON, this.ft_status);//divison
			l1Data.put(AnalyticsConstants.V2.SECTION, this.file_available_on_server ? 1 : 0);
			l1Data.put(AnalyticsConstants.V2.SPECIES, CesConstants.LEVEL_ONE);
			Logger.d(TAG, "Ces l1 data = " + l1Data.toString());
		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		
		return l1Data;
	}

	/*
	 * L2 Format
	 * 
	 *   {“m1”:{“uniqueId” : "VWXuGJkrTHXe8ojo" {“ft_type”:”upload”, “sessionId” : XGSB6377, “fileSize” : 1203, “fileType”: “jpeg”, “someTime” : 100 , 
	 *   “ManualRetryCount” : 0 , “iSquickUpload” : 1, “ft_status” : 0, “networkTime” : 23, “StackTrace”:”IOException”, “chunkSize” : 232, “chunkNumber”:1, “connType”:1}}}

	Final format: {"v": "m1" <module>, "vs": "VWXuGJkrTHXe8ojo"<uniqueId>, "c": "upload"<“ft_type”> "nw": 1 <connType>, "f": "jpeg"<“fileType”>, 
	"vi": 1203<fileSize>, "us": 100<“someTime”>, "t": 23<networkTime>, "ra": "XGSB6377"<“sessionId”>,  "b"; 0<“ManualRetryCount”>, "d": 0<“ft_status”>, 
	"sec": 1<“iSquickUpload”>, "pop": 232<“chunkSize”>, "cs": 1<“chunkNumber”> , "ser": "IOException"<“StackTrace”>, "s" : "l2" }
	
	"k":"act_rel"
	"o":"ces_l2"
	"p":"CES"
	"uk":"ces_l2"
	->"ver":"v2"
	"ts":
	"fu": "uid"
	"msisdn":"9128382982"
	"av":"app_version"
	"ov":"os_version"
	"fa" : "cts"
	
	 */
	@Override
	public JSONObject buildLevelTwoInfo()
	{
		JSONObject l2Data = null;
		try {
			l2Data = new JSONObject();
			l2Data.put(AnalyticsConstants.V2.VARIETY, CesConstants.FT_MODULE);
			l2Data.put(AnalyticsConstants.V2.VAL_STR, this.ft_unique_Id == null ? "" : this.ft_unique_Id);
			l2Data.put(AnalyticsConstants.V2.CLASS, this.ft_type);
			l2Data.put(AnalyticsConstants.V2.NETWORK, this.netType);
			l2Data.put(AnalyticsConstants.V2.FORM, this.fileType == null ? "" : this.fileType);
			l2Data.put(AnalyticsConstants.V2.VAL_INT, this.fileSize);
			l2Data.put(AnalyticsConstants.V2.USER_STATE, this.procTime);
			l2Data.put(CesConstants.AnalyticsV2.TRIBE, this.networkProcTime);
			l2Data.put(AnalyticsConstants.V2.RACE, this.ftSessionId == null ? "" : this.ftSessionId);
			l2Data.put(AnalyticsConstants.V2.BREED, this.isManualRetry ? 1 : 0);
			l2Data.put(CesConstants.AnalyticsV2.DIVISON, this.ft_status);
			l2Data.put(AnalyticsConstants.V2.SECTION, this.file_available_on_server ? 1 : 0);
			l2Data.put(AnalyticsConstants.V2.POPULATION, this.chunkSize);
			l2Data.put(AnalyticsConstants.V2.CENSUS, this.chunk_number);
			l2Data.put(AnalyticsConstants.V2.SERIES, this.stackTrace == null ? "" : this.stackTrace);
			l2Data.put(AnalyticsConstants.V2.KINGDOM, CesConstants.CES_ACT_REL);
			l2Data.put(AnalyticsConstants.V2.ORDER, CesConstants.CES_L2_DATA);
			l2Data.put(AnalyticsConstants.V2.PHYLUM, CesConstants.CES);
			l2Data.put(AnalyticsConstants.V2.UNIQUE_KEY, CesConstants.CES_L2_DATA);
			l2Data.put(AnalyticsConstants.V2.SPECIES, CesConstants.LEVEL_TWO);
			Logger.d(TAG, "Ces l2 data = " + l2Data.toString());
		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		return l2Data;
	}

	private String getSizeBucket(long fileSize)
	{
		return fileSize < SIZE_100KB ? CesConstants.SizeBucket.BUCKET_0KB_100KB : fileSize < SIZE_200KB ? CesConstants.SizeBucket.BUCKET_100KB_200KB :
			fileSize < SIZE_300KB ? CesConstants.SizeBucket.BUCKET_200KB_300KB : fileSize < SIZE_500KB ? CesConstants.SizeBucket.BUCKET_300KB_500KB : 
				fileSize < SIZE_1MB ? CesConstants.SizeBucket.BUCKET_500KB_1MB : fileSize < SIZE_5MB ? CesConstants.SizeBucket.BUCKET_1MB_5MB : 
					CesConstants.SizeBucket.BUCKET_GREATER_THAN_5MB;
	}
}
