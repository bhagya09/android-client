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

    private Object lock = new Object();

    private StatusReadDBManager(){}

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
            synchronized (lock) {
                runnableList.add(suReadDBRunnable);
            }
            HikeHandlerUtil.getInstance().postRunnableWithDelay(suReadDBRunnable, 2000);
        }
    }

    public void setFinished(StatusReadDBRunnable finished) {
        synchronized (lock) {
            runnableList.remove(finished);
        }
    }

    public void stopAll() {
        for (StatusReadDBRunnable runnable : runnableList) {
            HikeHandlerUtil.getInstance().removeRunnable(runnable);
        }

        synchronized (lock) {
            runnableList.clear();
        }
    }
}