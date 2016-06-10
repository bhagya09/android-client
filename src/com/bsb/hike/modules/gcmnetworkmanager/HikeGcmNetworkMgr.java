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
        try {
            if (!isGooglePlayServicesAvailable()) {
                Logger.e(TAG, "google play services not available");
                removeGcmTaskConfigFromDB(config);
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
        } catch (Throwable e) {
            removeGcmTaskConfigFromDB(config);
            Logger.wtf(TAG, "Error while scheduling", e);
        }
    }

    @Override
    public void cancelTask(Config config)
    {
        removeGcmTaskConfigFromDB(config);

        try {
            if (!isGooglePlayServicesAvailable()) {
                Logger.e(TAG, "google play services not available");
                return;
            }

            if (config != null) {
                GcmNetworkManager.getInstance(context).cancelTask(config.getTag(), config.getService());
            }
        } catch (Throwable e) {
            Logger.wtf(TAG, "Error while cancelling task", e);
        }
    }

    @Override
    public void cancelAllTasks(Class<? extends GcmTaskService> gcmTaskService)
    {
        removeAllGcmTasksFromDB();

        try {

            if (!isGooglePlayServicesAvailable()) {
                Logger.e(TAG, "google play services not available");
                return;
            }

            GcmNetworkManager.getInstance(context).cancelAllTasks(gcmTaskService);
        } catch (Throwable e) {
            Logger.wtf(TAG, "Error while cancelling all tasks", e);
        }
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

    public void removeGcmTaskConfigFromDB(final Config config)
    {
        HikeHandlerUtil.getInstance().postAtFront(new Runnable()
        {
            @Override
            public void run()
            {
                if (config != null)
                {
                    Logger.d(TAG, "removing config from db with tag : " + config.getTag());
                    HttpRequestStateDB.getInstance().deleteBundleForTag(config.getTag());
                }
            }
        });
    }

    public void removeAllGcmTasksFromDB()
    {
        HikeHandlerUtil.getInstance().postAtFront(new Runnable()
        {
            @Override
            public void run()
            {
                    Logger.d(TAG, "removing all tasks from db : ");
                    HttpRequestStateDB.getInstance().deleteAllGcmTasksFromDb();
            }
        });
    }

    public void updateGcmTaskConfigInDB(final Config config)
    {
        HikeHandlerUtil.getInstance().postAtFront(new Runnable()
        {
            @Override
            public void run()
            {
                if (config != null)
                {
                    Logger.d(TAG, "updating config in db with tag : " + config.getTag());
                    HttpRequestStateDB.getInstance().update(config.getTag(), config.toBundle());
                }
            }
        });
    }
}
