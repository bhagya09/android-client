package com.hike.abtest.dataparser;

import java.util.Map;

/**
 * Created by abhijithkrishnappa on 03/04/16.
 */
public abstract class ExperimentInit {
    Map<String, String> mExperimentMap = null;
    String mRequestPayload = null;
    boolean isParsed = false;

    ExperimentInit(String requestPayload) {
        mRequestPayload = requestPayload;
    }

    public abstract void parse() throws ParserException;

    public Map<String, String> getExperimentsMap() {
        if(!isParsed) throw new IllegalStateException();
        return mExperimentMap;
    }
}
