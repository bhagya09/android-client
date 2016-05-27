package com.bsb.hike.timeline.tasks;

import com.bsb.hike.models.HikeHandlerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by atul on 26/05/16.
 */
public class StatusReadDBManager {
    private static StatusReadDBManager mInstance;

    private List<StatusReadDBRunnable> runnableList = new ArrayList<StatusReadDBRunnable>();

    public static StatusReadDBManager getInstance() {
        if (mInstance == null) {
            synchronized (StatusReadDBManager.class) {
                if (mInstance == null) {
                    mInstance = new StatusReadDBManager();
                }
            }
        }
        return mInstance;
    }

    public void execute(StatusReadDBRunnable suReadDBRunnable) {
        if (!runnableList.contains(suReadDBRunnable)) {
            runnableList.add(suReadDBRunnable);
            HikeHandlerUtil.getInstance().postRunnableWithDelay(suReadDBRunnable, 2000);
        }
    }

    public void setFinished(StatusReadDBRunnable finished) {
        runnableList.remove(finished);
    }
}