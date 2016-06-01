package com.bsb.hike.spaceManager;

import android.content.Intent;

import com.bsb.hike.HikeConstants;
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

    public static final String ACTION_FETCH_SPACE_MANAGER_ITEMS = "com.bsb.hike.FETCH_SPC_MGR_ITEMS";

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

            case ACTION_FETCH_SPACE_MANAGER_ITEMS:
                fetchSpaceManagerItems();
                break;
        }
    }

    private void customDirectoryAnalytics(String dirPath, boolean shouldMapContainedFiles)
    {
        SpaceManagerUtils.recordDirectoryAnalytics(dirPath, shouldMapContainedFiles);
    }

    private void internalStorageAnalytics(boolean shouldMapContainedFiles)
    {
        SpaceManagerUtils.recordInternalHikeDirAnalytics(shouldMapContainedFiles);
    }

    private void externalStorageAnalytics(boolean shouldMapContainedFiles)
    {
        SpaceManagerUtils.recordExternalHikeDirAnalytics(shouldMapContainedFiles);
    }

    private void sharedStorageAnalytics(boolean shouldMapContainedFiles)
    {
        SpaceManagerUtils.recordSharedHikeDirAnalytics(shouldMapContainedFiles);
    }

    private void fetchSpaceManagerItems()
    {
        SpaceManagerItemsFetcher.fetchItems();
    }
}
