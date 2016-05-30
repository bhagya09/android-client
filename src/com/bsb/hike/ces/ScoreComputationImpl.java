package com.bsb.hike.ces;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author suyash
 *
 */
public interface ScoreComputationImpl {

	public JSONObject computeScore();
	public JSONObject getL1Data(JSONArray requiredData);

}
