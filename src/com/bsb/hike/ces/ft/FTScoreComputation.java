/**
 * 
 */
package com.bsb.hike.ces.ft;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.ces.CesConstants;
import com.bsb.hike.ces.CesConstants.CESInfoType;
import com.bsb.hike.ces.CesConstants.CESModule;
import com.bsb.hike.ces.CesUtils;
import com.bsb.hike.ces.ScoreComputationImpl;
import com.bsb.hike.ces.disk.CesDiskManager;
import com.bsb.hike.utils.Logger;

/**
 * @author suyash
 *
 */
public class FTScoreComputation implements ScoreComputationImpl{

	private final String TAG = "FTScoreComputation";

	@Override
	public JSONObject getLevelOneInfo() {
		CesDiskManager disk = new CesDiskManager(CESModule.FT, CesUtils.getCurrentUTCDate(), CesDiskManager.DataFlushMode.FLUSH);/*getDayBeforeUTCDate()*/
		File mFile = new File(disk.getFilePath(CESInfoType.L1));
		if(mFile != null && mFile.exists())
		{
			List<JSONObject> data =  disk.get(CESInfoType.L1);
			if(data == null || data.isEmpty())
			{
				return null;
			}
			return changeFormat(data.get(0));
		}
		return null;
	}

	@Override
	public int computeScore() {
		// TODO Auto-generated method stub
		return 0;
	}

	private JSONObject changeFormat(JSONObject data)
	{
		JSONObject result = null;
		JSONObject ft_uids = null;
		if(data != null)
		{
			try {
				JSONObject moduleData = null;
				ft_uids = data.getJSONObject(CesConstants.FT_MODULE);
				if(ft_uids != null)
				{
					moduleData = new JSONObject();
					for (Iterator<String> iterator = ft_uids.keys(); iterator.hasNext();) {
						String key = (String) iterator.next();
						JSONArray as = null;
						JSONArray mr = null;
						JSONArray fas = null;
						JSONObject mData = (JSONObject) ft_uids.get(key);
						int fti = mData.getInt(CesConstants.LevelOneDataKey.FT_INCOMPLETE);
						JSONObject bucketObj = null;
						JSONObject netObj = null;
						int fti_count = 0;
						if(moduleData.has(mData.getString(AnalyticsConstants.V2.NETWORK)))
						{
							netObj = moduleData.getJSONObject(mData.getString(AnalyticsConstants.V2.NETWORK));
							if(netObj.has(mData.getString(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET)))
							{
								bucketObj = (JSONObject) netObj.get(mData.getString(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET));
								as = bucketObj.getJSONArray(CesConstants.LevelOneDataKey.AVERAGE_SPEED);
								mr = bucketObj.getJSONArray(CesConstants.LevelOneDataKey.MANUAL_RETRIES);
								fas = bucketObj.getJSONArray(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER);
								fti_count = bucketObj.getInt(CesConstants.LevelOneDataKey.FT_INCOMPLETE);
								if(fti == CesConstants.FT_STATUS_INCOMPLETE)
								{
									fti_count += 1;
								}
							}
							else
							{
								as = new JSONArray();
								mr = new JSONArray();
								fas = new JSONArray();
								fti_count = fti;
								bucketObj = new JSONObject();
							}
						}
						else
						{
							as = new JSONArray();
							mr = new JSONArray();
							fas = new JSONArray();
							fti_count = fti;
							bucketObj = new JSONObject();
							netObj = new JSONObject();
						}
						
						if(fti == CesConstants.FT_STATUS_COMPLETE)
						{
							long procTime = mData.getLong(CesConstants.LevelOneDataKey.FT_PROCESSING_TIME);
							long fileSize = mData.getLong(CesConstants.LevelOneDataKey.FILE_SIZES);
							as.put(CesUtils.getSpeedInKbps(fileSize, procTime));
							bucketObj.put(CesConstants.LevelOneDataKey.AVERAGE_SPEED, as);
	
							mr.put(mData.getInt(CesConstants.LevelOneDataKey.MANUAL_RETRIES));
							bucketObj.put(CesConstants.LevelOneDataKey.MANUAL_RETRIES, mr);
	
							fas.put(mData.getInt(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER));
							bucketObj.put(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER, fas);
						}

						bucketObj.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, fti_count);
						
						netObj.put(mData.getString(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET), bucketObj);

						moduleData.put(mData.getString(AnalyticsConstants.V2.NETWORK), netObj);
					}
					result = new JSONObject();
					result.put(CesConstants.FT_MODULE, moduleData);
					Logger.d(TAG, "Final Json for level one data = " + result.toString());
				}
			} catch (JSONException e)
			{
				Logger.e(TAG, "FileNotFoundException : ", e);
			}
		}
		return result;
	}
}
