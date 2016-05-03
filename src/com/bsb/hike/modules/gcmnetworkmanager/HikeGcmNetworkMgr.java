package com.bsb.hike.modules.gcmnetworkmanager;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.google.android.gms.gcm.GcmTaskService;

/**
 * Created by sidharth on 03/05/16.
 */
public class HikeGcmNetworkMgr implements IGcmNetworkMgr
{
    public static final String TAG = "HikeGcmNetworkMgr";

    private volatile static HikeGcmNetworkMgr _instance;

    private Context context;

    private HikeGcmNetworkMgr()
    {
        context = HikeMessengerApp.getInstance().getApplicationContext();
    }

    public static HikeGcmNetworkMgr getInstance()
    {
        if (_instance == null)
        {
            synchronized (HikeGcmNetworkMgr.class)
            {
                if (_instance == null)
                {
                    _instance = new HikeGcmNetworkMgr();
                }
            }
        }
        return _instance;
    }

    @Override
    public void schedule(Config config)
    {

    }

    @Override
    public void cancelTask(String tag, GcmTaskService gcmTaskService)
    {

    }
}
