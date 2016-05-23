package com.bsb.hike.ces;

import org.json.JSONObject;

/**
 * @author suyash
 *
 */
public interface ScoreComputationImpl {

	public JSONObject getLevelOneInfo();
	public int computeScore();

}
