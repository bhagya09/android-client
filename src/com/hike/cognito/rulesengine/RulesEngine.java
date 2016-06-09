package com.hike.cognito.rulesengine;

import com.hike.cognito.model.DataPointVO;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by abhijithkrishnappa on 07/06/16.
 */
public abstract class RulesEngine {

    public abstract List<DataPointVO> getOnDemandDataPoints(JSONObject policy);

    public abstract DataPointVO getOnDemandDataPointById(String requestId);
}
