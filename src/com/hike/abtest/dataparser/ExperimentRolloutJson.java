package com.hike.abtest.dataparser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 26/04/16.
 */
public class ExperimentRolloutJson extends ExperimentInitJson {

    public ExperimentRolloutJson(String requestPayload) {
        super(requestPayload);
    }

    public String getExperiment(JSONObject experiment) throws JSONException {
        experiment.put(ProtoMapper.EXPERIMENT_ROLL_OUT, true);
        return experiment.toString();
    }

}
