package com.hike.cognito.rulesengine;

import com.bsb.hike.utils.Logger;
import com.hike.cognito.datapoints.DataPointMapper;
import com.hike.cognito.model.DataPointVO;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abhijithkrishnappa on 08/06/16.
 */
public class RulesEngineLegacy extends RulesEngine{

    private static final String TAG = RulesEngineLegacy.class.getSimpleName();

    @Override
    public List<DataPointVO> getOnDemandDataPoints(JSONObject policy) {
        List<DataPointVO> datapointList = new ArrayList<DataPointVO>();

        if (policy.optBoolean(DataPointMapper.ID_CALL_LOGS)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_CALL_LOGS));
        }
        if (policy.optBoolean(DataPointMapper.ID_STATIC_LOCATION)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_STATIC_LOCATION));
        }
        if (policy.optBoolean(DataPointMapper.ID_ALL_APPS)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_ALL_APPS));
        }
        if (policy.optBoolean(DataPointMapper.ID_ADVERTISING_ID)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_ADVERTISING_ID));
        }
        if (policy.optBoolean(DataPointMapper.ID_SESSION_LOG)) {
            //TODO possibly turn this into "gl":true to "gl":"stl"
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_SESSION_LOG));
        }
        if (policy.optBoolean(DataPointMapper.ID_PHONE_SPEC)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_PHONE_SPEC));
        }
        if (policy.optBoolean(DataPointMapper.ID_DEVICE_DETAILS)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_DEVICE_DETAILS));
        }
        if (policy.optBoolean(DataPointMapper.ID_ACCOUNT_INFO)) {
            datapointList.add(getOnDemandDataPointById(DataPointMapper.ID_ACCOUNT_INFO));
        }

        return datapointList;
    }

    @Override
    public DataPointVO getOnDemandDataPointById(String requestId) {
        Logger.d(TAG, "getOnDemandDataPointById, requestId" + requestId);
        return new DataPointVO(requestId, true, true, DataPointMapper.getClassForDataPoint(requestId));
    }
}
