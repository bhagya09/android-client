package com.hike.abtest.dataparser;

import java.util.Map;

import com.hike.abtest.model.Experiment;
import com.hike.abtest.model.Variable;

/**
 * Created by abhijithkrishnappa on 11/04/16.
 */
public abstract class ExperimentsLoader {
    Map<String, Experiment> mExperimentMap = null;
    Map<String, Variable> mVariableMap = null;
    boolean isParsed = false;
    Map<String, ?> mRequest = null;

    ExperimentsLoader(Map<String, ?> request) {
        mRequest = request;
    }

    public abstract void parse() throws ParserException;

    public Map<String, Experiment> getExperiments() {
        if(!isParsed) throw new IllegalStateException();
        return mExperimentMap;
    }

    public Map<String, Variable> getVariables() {
        if(!isParsed) throw new IllegalStateException();
        return mVariableMap;
    }
}
