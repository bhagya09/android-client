package com.hike.abtest.dataparser;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hike.abtest.Logger;

/**
 * Created by abhijithkrishnappa on 03/04/16.
 */
public class ExperimentInitJson extends ExperimentInit{
    private static final String TAG = ExperimentInitJson.class.getSimpleName();

    public ExperimentInitJson(String requestPayload) {
        super(requestPayload);
    }

    public void parse() throws ParserException{
        JSONObject jsonRequest = null;
        try {
            jsonRequest = new JSONObject(mRequestPayload);
            Logger.d(TAG, "mRequestPayload: " + mRequestPayload);
            if(jsonRequest.has(ProtoMapper.EXPERIMENT_LIST)) {
                mExperimentMap = parseValidateExperiments(jsonRequest.getJSONArray(ProtoMapper.EXPERIMENT_LIST));
            }
            //If empty, throw exception.
            if(mExperimentMap==null || mExperimentMap.size() == 0) {
                throw new ParserException("No experiments to be initalized",
                        ParserException.ERROR_FIELDS_MISSING);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            throw new ParserException("Error Parsing request");
        }
        isParsed = true;
    }

    Map<String, String> parseValidateExperiments(JSONArray experiments) throws ParserException{
        Logger.d(TAG, "parseValidateExperiments: ");

        Map<String, String> experimentMap = new HashMap<String, String>();
        Gson gson = new Gson();
        Type type = new TypeToken<List<ProtoMapper.ExperimentInitMapper>>() {}.getType();
        List<ProtoMapper.ExperimentInitMapper> fromJson = gson.fromJson(experiments.toString(), type);

        int index = 0;
        for(ProtoMapper.ExperimentInitMapper expMap : fromJson) {
            //Validate Experiments
            if(expMap.experimentId == null || expMap.variantId == null
                    || expMap.startTime <= 0 || expMap.endTime <= 0) {
                throw new ParserException("Invalid Experiment", ParserException.ERROR_INVALID_EXPERIMENT);
            }

            //Validate Variables in the Experiment
            for(ProtoMapper.VariableMapper variableMap : expMap.variableList) {
                if(variableMap.variableName ==null || variableMap.type==0 || variableMap.type>4 ||
                        variableMap.defaultValue ==null || variableMap.experimentValue ==null) {
                    throw new ParserException("Invalid Variable", ParserException.ERROR_INVALID_VARIABLE);
                }
            }

            //Experiment validated. Add to Map
            try {
                experimentMap.put(expMap.experimentId, getExperiment(experiments.getJSONObject(index)));
            } catch (JSONException e) {
                e.printStackTrace();
                throw new ParserException("Error storing experiment");
            }
            index++;
        }

        return experimentMap;
    }

    public String getExperiment(JSONObject experiment) throws JSONException {
        return experiment.toString();
    }
}
