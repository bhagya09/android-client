package com.hike.abtest.dataparser;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * DataParser, a factory class which provides specific interface based on request
 * Created by abhijithkrishnappa on 03/04/16.
 */
public class DataParser {
    public static final String REQUEST_TYPE_EXPERIMENT_INIT = "AB-Exp-Init";
    public static final String REQUEST_TYPE_EXPERIMENT_ABORT = "AB-Exp-Abort";
    public static final String REQUEST_TYPE_EXPERIMENT_ROLL_OUT = "AB-Exp-Rollout";
    public static final String REQUEST_TYPE_INVALID = "INVALID";
    private static final String REQUEST_FORMAT_JSON = "json";
    private static final String REQUEST_FORMAT_XML = "xml";


    private String requestFormat = REQUEST_FORMAT_JSON;

    public DataParser() {

    }

    public String getRequestType(String requestPayload) throws ParserException {
        String notificationType = REQUEST_TYPE_INVALID;
        //TODO: check implement
        try {
            JSONObject jsonObject = new JSONObject(requestPayload);
            if (jsonObject.has(ProtoMapper.REQUEST_TYPE)) {
                notificationType = jsonObject.getString("type");
            } else {
                throw new ParserException("Error parsing request, no request type",
                        ParserException.ERROR_FIELDS_MISSING);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ParserException("Error parsing request, no request type",
                    ParserException.ERROR_FIELDS_MISSING);
        }

        return notificationType;
    }

    public ExperimentInit getExperimentInit(String notificationPayload) throws ParserException {
        switch (requestFormat) {
            case REQUEST_FORMAT_JSON:
                return new ExperimentInitJson(notificationPayload);
            default:
                throw new ParserException("Invalid packet format configuration");
        }
    }

    public ExperimentAbort getExperimentAbort(String notificationPayload) throws ParserException {
        switch (requestFormat) {
            case REQUEST_FORMAT_JSON:
                return new ExperimentAbortJson(notificationPayload);
            default:
                throw new ParserException("Invalid packet format configuration");
        }
    }

    public ExperimentInit getRollout(String notificationPayload) throws ParserException {
        switch (requestFormat) {
            case REQUEST_FORMAT_JSON:
                return new ExperimentRolloutJson(notificationPayload);
            default:
                throw new ParserException("Invalid packet format configuration");
        }
    }

    public ExperimentsLoader getExperimentLoader(Map<String, ?> experiments) throws ParserException {
        switch (requestFormat) {
            case REQUEST_FORMAT_JSON:
                return new ExperimentsLoaderJson(experiments);
            default:
                throw new ParserException("Invalid packet format configuration");
        }
    }

    public static boolean isABTestMessage(String requestType) {
        boolean result = false;
        switch (requestType) {
            case REQUEST_TYPE_EXPERIMENT_INIT:
            case REQUEST_TYPE_EXPERIMENT_ROLL_OUT:
            case REQUEST_TYPE_EXPERIMENT_ABORT:
                result = true;
                break;
            default:
                result = false;
        }
        return result;
    }
}
