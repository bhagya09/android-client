/**
 * 
 */
package com.bsb.hike.ces.ft;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.ces.CesBaseCallable;
import com.bsb.hike.ces.CesConstants;
import com.bsb.hike.ces.CesUtils;
import com.bsb.hike.ces.CesDataInfoFormatBuilder;
import com.bsb.hike.ces.disk.CesDiskManager;
import com.bsb.hike.utils.Logger;

/**
 * @author suyash
 *
 */
public class CesFtTask extends CesBaseCallable{

	private CesDataInfoFormatBuilder<?> cesData;
	private final String TAG = "CesFtTask";

	public CesFtTask(CesDataInfoFormatBuilder<?> cesData)
	{
		this.cesData = cesData;
	}

	@Override
	public Void call() throws Exception
	{
		JSONObject l1Data = cesData.buildLevelOneInfo();
		handleLevelOneData(l1Data);
		handleLevelTwoData(cesData.buildLevelTwoInfo());
		return null;
	}

	private void handleLevelTwoData(JSONObject l2Data)
	{
		if(l2Data == null)
		{
			return;
		}
		CesDiskManager disk = new CesDiskManager(CesConstants.CESModule.FT, CesUtils.getCurrentUTCDate(), CesDiskManager.DataFlushMode.FLUSH);
		disk.add(CesConstants.CESInfoType.L2, l2Data);
	}

	private void handleLevelOneData(JSONObject l1Data)
	{
		if(l1Data == null)
		{
			return;
		}
		CesDiskManager disk = new CesDiskManager(CesConstants.CESModule.FT, CesUtils.getCurrentUTCDate(), CesDiskManager.DataFlushMode.FLUSH);
		List<JSONObject> storedL1DataList = disk.get(CesConstants.CESInfoType.L1);
		if(storedL1DataList != null && !storedL1DataList.isEmpty())
		{
			disk.update(CesConstants.CESInfoType.L1, updateL1Data(l1Data, storedL1DataList.get(0)));
		}
		else
		{
			disk.add(CesConstants.CESInfoType.L1, prepareL1Data(l1Data));
		}
	}

	public JSONObject prepareL1Data(JSONObject data)
	{
		JSONObject uidBasedData = null;
		JSONObject l1Data = null;

		try {
			uidBasedData = new JSONObject();
			uidBasedData.put((String) data.get(AnalyticsConstants.V2.VAL_STR), getData(data));//data.get(AnalyticsConstants.V2.VAL_STR)

			l1Data = new JSONObject();
			l1Data.put((String) data.get(AnalyticsConstants.V2.VARIETY), uidBasedData);
		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		Logger.d(TAG, "Ces l1 data in server format = " + l1Data.toString());
		return l1Data;
	}

	public JSONObject updateL1Data(JSONObject data, JSONObject storedData)
	{
		
		JSONObject uidBasedData = null;
		JSONObject l1Data = null;
		try {
			if(storedData.has((String) data.get(AnalyticsConstants.V2.VARIETY)))
			{
				l1Data = (JSONObject) storedData.get((String) data.get(AnalyticsConstants.V2.VARIETY));
				if(l1Data.has((String) data.get(AnalyticsConstants.V2.VAL_STR)))
				{
					uidBasedData = (JSONObject) l1Data.get((String) data.get(AnalyticsConstants.V2.VAL_STR));
					String stored_network = uidBasedData.getString(AnalyticsConstants.V2.NETWORK);
					int stored_net_type = Integer.parseInt(stored_network);
					String network = data.getString(AnalyticsConstants.V2.NETWORK);
					int net_type = Integer.parseInt(network);
					if(net_type > stored_net_type || net_type == 1)
					{
						uidBasedData.put(AnalyticsConstants.V2.NETWORK, network);
					}

					double stored_averageSpeeds = uidBasedData.getDouble(CesConstants.LevelOneDataKey.AVERAGE_SPEED);
					double averageSpeeds = data.getDouble(AnalyticsConstants.V2.RACE);
					uidBasedData.put(CesConstants.LevelOneDataKey.AVERAGE_SPEED, (stored_averageSpeeds + averageSpeeds)/2);
						
					int stored_manualRetries = uidBasedData.getInt(CesConstants.LevelOneDataKey.MANUAL_RETRIES);
					int manualRetries = data.getInt(AnalyticsConstants.V2.BREED);
					uidBasedData.put(CesConstants.LevelOneDataKey.MANUAL_RETRIES, (manualRetries + stored_manualRetries));

					uidBasedData.put(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER, data.getInt(AnalyticsConstants.V2.SECTION));

					int fti = data.getInt(CesConstants.AnalyticsV2.DIVISON);
					if(fti == CesConstants.FT_STATUS_COMPLETE)
					{
						uidBasedData.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, fti);
					}
					else
					{
						uidBasedData.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, CesConstants.FT_STATUS_INCOMPLETE);
					}
		
					long store_procTimes = uidBasedData.getLong(CesConstants.LevelOneDataKey.FT_PROCESSING_TIME);
					long procTimes = data.getLong(AnalyticsConstants.V2.USER_STATE);
					uidBasedData.put(CesConstants.LevelOneDataKey.FT_PROCESSING_TIME, (procTimes + store_procTimes));

					uidBasedData.put(CesConstants.LevelOneDataKey.FILE_SIZES, data.getString(AnalyticsConstants.V2.VAL_INT));

					uidBasedData.put(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET, data.get(AnalyticsConstants.V2.FORM));
				}
				else
				{
					l1Data.put((String) data.get(AnalyticsConstants.V2.VAL_STR), getData(data));
				}
			}
		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		Logger.d(TAG, "Update : Ces l1 data in server format = " + l1Data.toString());
		return storedData;
	}

	private JSONObject getData(JSONObject data)
	{
		JSONObject uidBasedData = null;
		try
		{
			uidBasedData = new JSONObject();

			int fti = data.getInt(CesConstants.AnalyticsV2.DIVISON);
			if(fti == CesConstants.FT_STATUS_COMPLETE)
			{
				uidBasedData.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, CesConstants.FT_STATUS_COMPLETE);
			}
			else
			{
				uidBasedData.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, CesConstants.FT_STATUS_INCOMPLETE);
			}

			uidBasedData.put(CesConstants.LevelOneDataKey.AVERAGE_SPEED, data.getDouble(AnalyticsConstants.V2.RACE));
			uidBasedData.put(CesConstants.LevelOneDataKey.MANUAL_RETRIES, data.getInt(AnalyticsConstants.V2.BREED));
			uidBasedData.put(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER, data.getInt(AnalyticsConstants.V2.SECTION));

			uidBasedData.put(AnalyticsConstants.V2.NETWORK, data.getString(AnalyticsConstants.V2.NETWORK));

			uidBasedData.put(CesConstants.LevelOneDataKey.FT_PROCESSING_TIME, data.getLong(AnalyticsConstants.V2.USER_STATE));

			uidBasedData.put(CesConstants.LevelOneDataKey.FILE_SIZES, data.getLong(AnalyticsConstants.V2.VAL_INT));

			uidBasedData.put(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET, data.get(AnalyticsConstants.V2.FORM));
		}
		catch(JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		return uidBasedData;
	}

	/*
	 * { "m1" : { "ft_uid_1" : {"as"     : as_long,   # Average upload speed
                           "mr"     : mr_int,  # Number of manual retries
                           "fti"    : 2,     # Total number of file transfers that failed
                           "fas"    : fas_int,
                           "file_size"   : size_long,
                           "proc_time" : t_long,
                           "nw" : "higherNetwork",
                           "size_bucket" : "b0"
                          }
                 },
            "ft_uid_2" : {...},
         }
		}
	 */
}
