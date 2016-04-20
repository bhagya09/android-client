package com.hike.abtest.dataparser;

import java.util.List;

/**
 * Created by abhijithkrishnappa on 05/04/16.
 */
public class ProtoMapper {
    public static final String REQUEST_TYPE = "type";
    public static final String EXPERIMENT_LIST = "experimentList";


    static class ExperimentInitMapper {
        String experimentId = null;
        String description = null;
        String variantId = null;
        long startTime = 0l;
        long endTime = 0l;
        List<VariableMapper> variableList = null;
        boolean isRollout;
        String callbackUrl = null;
    }

    static class VariableMapper {
        String variableName = null;
        int type = 0;
        Object defaultValue = null;
        Object experimentalValue = null;
    }

    static class ExperimentAbortMapper {
        String experimentId = null;
        String variantId = null;
    }
}
