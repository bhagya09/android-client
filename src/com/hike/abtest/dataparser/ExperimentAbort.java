package com.hike.abtest.dataparser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abhijithkrishnappa on 03/04/16.
 */
public abstract class ExperimentAbort {
    List<String> mExperimentIdList = new ArrayList<String>();
    String mRequestPayload = null;
    boolean isParsed = false;

    ExperimentAbort(String requestPayload) {
        mRequestPayload = requestPayload;
    }

    public abstract void parse() throws ParserException;

    public List<String> getExperimentIds() {
        if(!isParsed) throw new IllegalStateException();
        return mExperimentIdList;
    }
}
