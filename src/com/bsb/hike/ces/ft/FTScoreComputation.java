/**
 * 
 */
package com.bsb.hike.ces.ft;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * @author suyash
 *
 */
public class FTScoreComputation implements ScoreComputationImpl{

	private final String TAG = "FTScoreComputation";
	private JSONObject l1Data;
	private final int MEAN90_SCORE = 0;
	private final int MEAN80_SCORE = 1;
	private final int MEAN10_SCORE = 2;
	private final int MEDIAN_SCORE = 3;
	private final int AVERAGE_SCORE = 4;

	/*
	 * {
    "<yymmdd>" : {
                "s" : { 
                             "m1" : {"2" : 0.5, "3" : 0.6, "4" : 0.6, "1" : 0.8}
                      }
              },
    "<yymmdd>" : {...},
    ....
}
	 */
	@Override
	public JSONObject computeScore() {
		JSONObject scoreJson = null;
		ConcurrentHashMap<String, List<Integer>> scores = new ConcurrentHashMap<>();
		CesDiskManager disk = new CesDiskManager(CESModule.FT, CesUtils.getDayBeforeUTCDate(), CesDiskManager.DataFlushMode.FLUSH);
		File mFile = new File(disk.getFilePath(CESInfoType.L1));
		if(mFile != null && mFile.exists())
		{
			List<JSONObject> data =  disk.get(CESInfoType.L1);
			if(data == null || data.isEmpty())
			{
				return null;
			}
			scores = computeScoreAndCreateL1Data(data.get(0), scores);
			if(scores != null)
			{
				try {
					JSONObject sData = new JSONObject();
					for (Enumeration<String> enumerator = scores.keys(); enumerator.hasMoreElements();) {
						String key = enumerator.nextElement();
						List<Integer> scoreData = scores.get(key);
						sData.put(key, getScore(scoreData));
					}
					scoreJson = new JSONObject();
					scoreJson.put(CesConstants.FT_MODULE, sData);
				} catch (JSONException e)
				{
					Logger.e(TAG, "JSONException : ", e);
				}
			}
		}
		if(scoreJson != null)
		{
			Logger.d(TAG, "Score Json for level one data = " + scoreJson.toString());
		}
		return scoreJson;
	}

	@Override
	public JSONObject getL1Data(JSONArray requiredData)
	{
		JSONObject resultData = null; 
		if(requiredData != null)
		{
			try
			{
				resultData = new JSONObject();
				for (int i = 0; i < requiredData.length(); i++)
				{
					String net = requiredData.getString(i);
					if(l1Data != null)
					{
						if(l1Data.getJSONObject(CesConstants.FT_MODULE).has(net))
						{
							resultData.put(net, l1Data.getJSONObject(CesConstants.FT_MODULE).get(net));
						}
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(resultData != null)
		{
			Logger.d(TAG, "Required Json for level one data = " + resultData.toString());
		}
		return resultData;
	}

	private int getScore(List<Integer> list)
	{
		int score = 0;
		if(list.isEmpty())
		{
			return score;
		}
		int mType = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.ConfigureKey.COMPUTE_SCORE_ALGO, MEDIAN_SCORE);
		Collections.sort(list);
		switch (mType)
		{
			case MEDIAN_SCORE:
				score = list.get(list.size()*1/2);
				break;
			case MEAN90_SCORE:
				score = list.get(list.size()*9/10);	
				break;
			case MEAN80_SCORE:
				score = list.get(list.size()*8/10);
				break;
			case MEAN10_SCORE:
				score = list.get(list.size()*1/10);
				break;
			case AVERAGE_SCORE:
				Integer sum = 0;
				for (Integer mark : list) {
			        sum += mark;
			    }
				score = (int) (sum.doubleValue() / list.size());
				break;
		}
		return score;
	}

	private ConcurrentHashMap<String, List<Integer>> computeScoreAndCreateL1Data(JSONObject data, ConcurrentHashMap<String, List<Integer>> scores)
	{
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
						String network = mData.getString(AnalyticsConstants.V2.NETWORK);
						JSONObject bucketObj = null;
						JSONObject netObj = null;
						int fti_count = 0;
						if(moduleData.has(network))
						{
							netObj = moduleData.getJSONObject(network);
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

						int fas_value = mData.getInt(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER);
						int manual_retry = mData.getInt(CesConstants.LevelOneDataKey.MANUAL_RETRIES);
						long procTime = mData.getLong(CesConstants.LevelOneDataKey.FT_PROCESSING_TIME);
						long fileSize = mData.getLong(CesConstants.LevelOneDataKey.FILE_SIZES);
						double avg_speed = CesUtils.getSpeedInKbps(fileSize, procTime);
						if(fti == CesConstants.FT_STATUS_COMPLETE)
						{
							as.put(avg_speed);
							bucketObj.put(CesConstants.LevelOneDataKey.AVERAGE_SPEED, as);
	
							mr.put(manual_retry);
							bucketObj.put(CesConstants.LevelOneDataKey.MANUAL_RETRIES, mr);
	
							fas.put(fas_value);
							bucketObj.put(CesConstants.LevelOneDataKey.FILE_AVAILABLE_ON_SERVER, fas);
						}

						bucketObj.put(CesConstants.LevelOneDataKey.FT_INCOMPLETE, fti_count);
						
						netObj.put(mData.getString(CesConstants.LevelOneDataKey.FILE_SIZE_BUCKET), bucketObj);

						moduleData.put(network, netObj);
						computeScoreForIndividualSession(avg_speed, manual_retry, fti, fas_value, network, scores);
					}
					l1Data = new JSONObject();
					l1Data.put(CesConstants.FT_MODULE, moduleData);
					Logger.d(TAG, "Final Json for level one data = " + l1Data.toString());
				}
			} catch (JSONException e)
			{
				Logger.e(TAG, "FileNotFoundException : ", e);
			}
		}
		return scores;
	}

	private void computeScoreForIndividualSession(double as, int mr, int fti, int fas, String network, ConcurrentHashMap<String, List<Integer>> scores)
	{
		int score = 0;
		if (fti == CesConstants.FT_STATUS_COMPLETE)
		{
			int mRetryFactor = (int) Math.pow(2, mr);
		    score = (int) ((as * 100 / CesUtils.getMaxSpeed(network)) / mRetryFactor);
		    if (fas == 1)
		    {
		    	score = Math.min(score, 100/mRetryFactor);
		    }
		}
		if(scores.containsKey(network))
		{
			scores.get(network).add(score);
		}
		else
		{
			List<Integer> scoreList = new LinkedList<Integer>();
			scoreList.add(score);
			scores.put(network, scoreList);
		}
	}
}
