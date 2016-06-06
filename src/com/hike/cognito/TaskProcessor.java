package com.hike.cognito;

import android.os.Handler;
import android.os.HandlerThread;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Utils;
import com.hike.cognito.datapoints.DataPointTask;

/**
 * Created by abhijithkrishnappa on 19/05/16.
 */
public class TaskProcessor {

    private TaskProcessor() {
    }

    public static void processTaskWithDelay(DataPointTask dataPointTask, long delay) {
        HikeHandlerUtil.getInstance().postRunnableWithDelay(dataPointTask, delay);
    }


    public static void removeRunnable(DataPointTask dataPointTask) {
        HikeHandlerUtil.getInstance().removeRunnable(dataPointTask);
    }

    public static void processTask(DataPointTask dataPointTask) {
        HikeHandlerUtil.getInstance().postRunnable(dataPointTask);
    }

    public static void processTaskOnPriority(DataPointTask dataPointTask) {
        HikeHandlerUtil.getInstance().postAtFront(dataPointTask);
    }
}
