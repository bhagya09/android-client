package com.hike.cognito;

import android.support.annotation.VisibleForTesting;

import com.bsb.hike.utils.Logger;
import com.hike.cognito.datapoints.DataPointTask;
import com.hike.cognito.model.DataPointVO;
import com.hike.cognito.rulesengine.RulesEngine;
import com.hike.cognito.rulesengine.RulesEngineLegacy;
import com.hike.cognito.transport.Transport;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by abhijithkrishnappa on 07/06/16.
 */
public class TaskManager {
    private static final String TAG = TaskManager.class.getSimpleName();
    private static TaskManager instance = null;
    private RulesEngine mRulesEngine = null;
    private TaskProcessor mTaskProcessor = null;

    private TaskManager() {
        mTaskProcessor = new TaskProcessor();
        mRulesEngine = new RulesEngineLegacy();
    }

    private static TaskManager getInstance() {
        if (instance == null) {
            synchronized (TaskManager.class) {
                if (instance == null) {
                    instance = new TaskManager();
                }
            }
        }
        return instance;
    }

    public static void handleOnDemandTriggerLegacy(JSONObject onDemandPolicy) {
        getInstance().handleOnDemandPolicy(onDemandPolicy);
    }

    public static void handleOnDemandTriggerLegacy(String requestId) {
        getInstance().handleOnDemandId(requestId);
    }

    private void handleOnDemandId(String requestId) {
        DataPointVO dataPoint = mRulesEngine.getOnDemandDataPointById(requestId);
        if(dataPoint!=null) {
            scheduleDataPoint(dataPoint);
        }
    }

    private void handleOnDemandPolicy(JSONObject onDemandPolicy) {
        List<DataPointVO> datapointList = mRulesEngine.getOnDemandDataPoints(onDemandPolicy);
        Logger.d(TAG,"datapointList size: " +datapointList.size());
        scheduleDataPoints(datapointList);
    }

    private void scheduleDataPoints(List<DataPointVO> datapointList) {
        for(DataPointVO datapointVO : datapointList) {
            scheduleDataPoint(datapointVO);
        }
    }

    private void scheduleDataPoint(DataPointVO datapointVO) {
        DataPointTask dataPointTask = getNewDataPointTask(datapointVO);
        if(dataPointTask!=null) {
            mTaskProcessor.processTask(dataPointTask);
        }
    }

    private DataPointTask getNewDataPointTask(DataPointVO datapointVO) {
        DataPointTask datapointTask = null;
        try {
            Constructor<?> ctor = datapointVO.getDataPointTaskClass().getConstructor(String.class, Boolean.class, Integer.class);

            datapointTask = (DataPointTask)ctor.newInstance(new Object[] {
                    datapointVO.getId(), datapointVO.isPii(),
                    datapointVO.isRealTime() ? Transport.TRANSPORT_TYPE_REALTIME : Transport.TRANSPORT_TYPE_DEFAULT });
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
            e.printStackTrace();
        }
        return datapointTask;
    }
}
