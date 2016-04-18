package com.hike.abtest.model;

import com.hike.abtest.Logger;

/**
 * Variable domain model
 * Created by abhijithkrishnappa on 29/03/16.
 */
public class Variable<T> {
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_LONG = 3;
    public static final int TYPE_STRING = 4;

    String mVariableName = null;
    int mType = 0;
    T mDefaultValue = null;
    T mExperimentValue = null;
    String mExperimentId = null;

    private Variable(String variableName, String experimentId, int type, T defaultValue, T experimentValue) {
        mVariableName = variableName;
        mExperimentId = experimentId;
        mType = type;
        mDefaultValue = defaultValue;
        mExperimentValue = experimentValue;
    }

    public static Variable getInstance(String variableName, String experimentId, int type,
                                       Object defaultValue, Object experimentalValue) {
        Logger.d("Check","type" + type + " variableName:"  + variableName);
        switch(type) {
            case TYPE_BOOLEAN:
                return new Variable<Boolean>(variableName, experimentId, type,
                        Boolean.parseBoolean((String)defaultValue), Boolean.parseBoolean((String)experimentalValue));
            case TYPE_INTEGER:
                return new Variable<Integer>(variableName, experimentId, type,
                        Integer.parseInt((String)defaultValue),Integer.parseInt((String)experimentalValue));
            case TYPE_LONG:
                return new Variable<Long>(variableName, experimentId, type,
                        Long.parseLong((String)defaultValue), Long.parseLong((String)experimentalValue));
            case TYPE_STRING:
                return new Variable<String>(variableName, experimentId, type,
                         (String)defaultValue, (String)experimentalValue);
            default:
                return null;
        }

    }
    public T getDefaultValue() {
        return mDefaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        mDefaultValue = defaultValue;
    }

    public T getExperimentValue() {
        return mExperimentValue;
    }

    public void setExperimentValue(T experimentValue) {
        mExperimentValue = experimentValue;
    }

    public int getType() {
        return mType;
    }

    public void setExperimentId(String experimentId) {
        mExperimentId = experimentId;
    }

    public String getExperimentId() {
        return mExperimentId;
    }

    public String toString() {
        return "VariableName: " + mVariableName + " Type:" + mType + " DefaultValue:"
                + mDefaultValue + " ExperimentValue:" + mExperimentValue + " ExperimentId:" + mExperimentId;
    }
}
