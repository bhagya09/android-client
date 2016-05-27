package com.hike.abtest.dataparser;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hike.abtest.Logger;
import com.hike.abtest.model.Experiment;
import com.hike.abtest.model.Variable;

/**
 * Created by abhijithkrishnappa on 11/04/16.
 */
public class ExperimentsLoaderJson extends ExperimentsLoader {

    private static final String TAG = ExperimentsLoaderJson.class.getSimpleName();

    ExperimentsLoaderJson(Map<String, ?> request) {
        super(request);
    }

    public void parse() throws ParserException {
        mExperimentMap = new HashMap<String, Experiment>();
        mVariableMap = new HashMap<String, Variable>();

        parseExperiments(getJSONArrayRequest());

        //If empty, throw exception.
        if (mExperimentMap == null || mExperimentMap.size() == 0 || mVariableMap == null || mVariableMap.size() == 0) {
            mExperimentMap = null;
            mVariableMap = null;
            return;
        }

        Collections.unmodifiableMap(mExperimentMap);
        Collections.unmodifiableMap(mVariableMap);

        isParsed = true;
    }

    private JSONArray getJSONArrayRequest() {
        JSONArray jsonArray = null;
        try {
            if (mRequest != null && mRequest.size() > 0) {
                Collection<String> expList = (Collection<String>) mRequest.values();
                jsonArray = new JSONArray();
                for (String exp : expList) {
                    JSONObject jObj = new JSONObject(exp);
                    jsonArray.put(jObj);
                }
            }
        } catch (NullPointerException npe) {
            Logger.d(TAG, "No experiments..");
            return null;
        } catch (JSONException jse) {
            Logger.d(TAG, "Error in parsing");
            return null;
        }
        return jsonArray;
    }

    public void parseExperiments(JSONArray experiments) throws ParserException {
        if (mExperimentMap == null || mVariableMap == null) {
            throw new NullPointerException();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<ProtoMapper.ExperimentInitMapper>>() {
        }.getType();
        List<ProtoMapper.ExperimentInitMapper> fromJson = gson.fromJson(experiments.toString(), type);

        for (ProtoMapper.ExperimentInitMapper expMap : fromJson) {
            if (expMap.experimentId == null || expMap.variantId == null
                    || expMap.startTime <= 0 || expMap.endTime <= 0) {
                throw new ParserException("Invalid Experiment", ParserException.ERROR_INVALID_EXPERIMENT);
            }

            mExperimentMap.put(expMap.experimentId, new Experiment(expMap.experimentId, expMap.variantId,
                    expMap.description, expMap.startTime, expMap.endTime, expMap.isRollout, expMap.callbackUrl));

            for (ProtoMapper.VariableMapper varMap : expMap.variableList) {
                if (varMap.variableName == null || varMap.type == 0 || varMap.type > 4 ||
                        varMap.defaultValue == null || varMap.experimentValue == null) {
                    throw new ParserException("Invalid Experiment", ParserException.ERROR_INVALID_VARIABLE);
                }

                mVariableMap.put(varMap.variableName, Variable.getInstance(varMap.variableName, expMap.experimentId, varMap.type,
                        varMap.defaultValue, varMap.experimentValue));
            }
        }
    }
}
