package com.hike.abtest.dataparser;

import java.lang.reflect.Type;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Created by abhijithkrishnappa on 03/04/16.
 */
public class ExperimentAbortJson extends ExperimentAbort {

    public ExperimentAbortJson(String requestPayload) {
        super(requestPayload);
    }

    public void parse() throws ParserException {
        JSONObject jsonRequest = null;
        try {
            jsonRequest = new JSONObject(mRequestPayload);

            if(jsonRequest.has(ProtoMapper.EXPERIMENT_LIST)) {
                parseAbortExperiments(jsonRequest.getJSONArray(ProtoMapper.EXPERIMENT_LIST),
                        mExperimentIdList);
            }

            if(mExperimentIdList.size() == 0) {
                throw new ParserException("Invalid Experiment Abort request",
                        ParserException.ERROR_FIELDS_MISSING);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            throw new ParserException("Invalid Experiment Abort request",
                    ParserException.ERROR_FIELDS_MISSING);
        }
        isParsed = true;
    }

    void parseAbortExperiments(JSONArray abortExperimentList,
                               List<String> mExperimentIdList) throws ParserException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<ProtoMapper.ExperimentAbortMapper>>() {}.getType();
        List<ProtoMapper.ExperimentAbortMapper> fromJson = gson.fromJson(abortExperimentList.toString(), type);

        for(ProtoMapper.ExperimentAbortMapper expAbort : fromJson) {
            if(expAbort.experimentId == null) {
                throw new ParserException("Invalid Experiment Abort request",
                        ParserException.ERROR_FIELDS_MISSING);
            }

            mExperimentIdList.add(expAbort.experimentId);
        }
    }
}
