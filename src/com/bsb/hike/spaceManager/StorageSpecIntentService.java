package com.bsb.hike.spaceManager;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.customClasses.HikeIntentService;

/**
 * IntentService class to trigger & compute directory sizes as per received packet
 */
public class StorageSpecIntentService extends HikeIntentService
{
    private static final String TAG = "StorageSpecIntentService";

    public static final String ACTION_GET_CUSTOM_DIRECTORY_ANALYTICS = "com.bsb.hike.CUSTOM_DIRECTORY_ANALYTICS";

    public static final String ACTION_GET_INTERNAL_STORAGE_ANALYTICS = "com.bsb.hike.INTERNAL_STORAGE_ANALYTICS";

    public static final String ACTION_GET_EXTERNAL_STORAGE_ANALYTICS = "com.bsb.hike.EXTERNAL_STORAGE_ANALYTICS";

    public static final String ACTION_GET_SHARED_STORAGE_ANALYTICS = "com.bsb.hike.SHARED_STORAGE_ANALYTICS";

    public StorageSpecIntentService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Logger.d(TAG, "received intent " + intent.getAction());

        boolean shouldMapContainedFiles = intent.getBooleanExtra(HikeConstants.SPACE_MANAGER.MAP_DIRECTORY, false);

        if(intent.getAction() == null)
        {
            return;
        }

        switch (intent.getAction())
        {
            case ACTION_GET_CUSTOM_DIRECTORY_ANALYTICS:
                String dirPath = intent.getStringExtra(HikeConstants.SPACE_MANAGER.DIRECTORY_PATH);
                customDirectoryAnalytics(dirPath, shouldMapContainedFiles);
                break;

            case ACTION_GET_INTERNAL_STORAGE_ANALYTICS:
                internalStorageAnalytics(shouldMapContainedFiles);
                break;

            case ACTION_GET_EXTERNAL_STORAGE_ANALYTICS:
                externalStorageAnalytics(shouldMapContainedFiles);
                break;

            case ACTION_GET_SHARED_STORAGE_ANALYTICS:
                sharedStorageAnalytics(shouldMapContainedFiles);
                break;
        }
    }

    private void customDirectoryAnalytics(String dirPath, boolean shouldMapContainedFiles)
    {
        StorageSpecUtils.recordDirectoryAnalytics(dirPath, shouldMapContainedFiles);
    }

    private void internalStorageAnalytics(boolean shouldMapContainedFiles)
    {
        StorageSpecUtils.recordInternalHikeDirAnalytics(shouldMapContainedFiles);
    }

    private void externalStorageAnalytics(boolean shouldMapContainedFiles)
    {
        StorageSpecUtils.recordExternalHikeDirAnalytics(shouldMapContainedFiles);
    }

    private void sharedStorageAnalytics(boolean shouldMapContainedFiles)
    {
        StorageSpecUtils.recordSharedHikeDirAnalytics(shouldMapContainedFiles);
    }
}
