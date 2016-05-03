package com.bsb.hike.modules.gcmnetworkmanager;

import com.google.android.gms.gcm.GcmTaskService;

/**
 * Created by sidharth on 03/05/16.
 */
public interface IGcmNetworkMgr
{
    void schedule(Config config);

    void cancelTask(String tag, GcmTaskService gcmTaskService);
}
