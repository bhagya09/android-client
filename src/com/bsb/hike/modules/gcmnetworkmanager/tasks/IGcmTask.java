package com.bsb.hike.modules.gcmnetworkmanager.tasks;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by anubhavgupta on 05/05/16.
 */
public interface IGcmTask<T>
{
    T execute(TaskParams taskParams);
}
