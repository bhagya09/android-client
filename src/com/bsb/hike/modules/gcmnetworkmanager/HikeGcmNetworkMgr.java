package com.bsb.hike.modules.gcmnetworkmanager;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;

import java.util.List;

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

    public boolean isGooglePlayServicesAvailable()
    {
        int resultCode = Utils.getPlayServicesAvailableCode(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    @Override
    public void schedule(Config config)
    {
        if (!isGooglePlayServicesAvailable())
        {
            Logger.e(TAG, "google play services not available");
            return;
        }

        OneoffTask task = new OneoffTask.Builder()
                .setTag(config.getTag())
                .setService(config.getService())
                .setExecutionWindow(config.getWindowStart(), config.getWindowEnd())
                .setExtras(config.getExtras())
                .setPersisted(config.isPersisted())
                .setRequiredNetwork(config.getRequiredNetwork())
                .setUpdateCurrent(config.isUpdateCurrent())
                .setRequiresCharging(config.getRequiresCharging())
                .build();

        GcmNetworkManager.getInstance(context).schedule(task);
    }

    @Override
    public void cancelTask(String tag, Class<? extends GcmTaskService> gcmTaskService)
    {
        if (!isGooglePlayServicesAvailable())
        {
            Logger.e(TAG, "google play services not available");
            return;
        }

        GcmNetworkManager.getInstance(context).cancelTask(tag, gcmTaskService);
    }

    @Override
    public void cancelAllTasks(Class<? extends GcmTaskService> gcmTaskService)
    {
        if (!isGooglePlayServicesAvailable())
        {
            Logger.e(TAG, "google play services not available");
            return;
        }

        GcmNetworkManager.getInstance(context).cancelAllTasks(gcmTaskService);
    }

	public void triggerPendingGcmNetworkCalls()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				Logger.d(TAG, "MAKING PENDING CALLS ON APP START");
				List<Config> getPendingTaskConfigs = HttpRequestStateDB.getInstance().getPendingGcmTaskConfigs();

				if (Utils.isEmpty(getPendingTaskConfigs))
				{
					return;
				}

				for (Config config : getPendingTaskConfigs)
				{
					HikeGcmNetworkMgr.getInstance().schedule(config);
				}
			}
		}, 0);
	}
}
