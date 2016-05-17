package com.hike.abtest.dataparser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by abhijithkrishnappa on 05/04/16.
 */
public class ProtoMapper {
    public static final String REQUEST_TYPE = "t";
    public static final String EXPERIMENT_LIST = "expList";
    public static final String EXPERIMENT_ROLL_OUT = "isRollout";

    static class ExperimentInitMapper {
        @SerializedName("expId")
        String experimentId = null;
        @SerializedName("expType")
        int expType = 0;
        @SerializedName("desc")
        String description = null;
        @SerializedName("variantId")
        String variantId = null;
        @SerializedName("sTime")
        long startTime = 0l;
        @SerializedName("eTime")
        long endTime = 0l;
        @SerializedName("varList")
        List<VariableMapper> variableList = null;
        @SerializedName("cbUrl")
        String callbackUrl = null;
        boolean isRollout = false;
    }

    static class VariableMapper {
        @SerializedName("varName")
        String variableName = null;
        @SerializedName("type")
        int type = 0;
        @SerializedName("defValue")
        Object defaultValue = null;
        @SerializedName("expValue")
        Object experimentValue = null;
    }

    static class ExperimentAbortMapper {
        @SerializedName("expId")
        String experimentId = null;
        @SerializedName("variantId")
        String variantId = null;
    }
}
