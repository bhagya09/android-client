package com.bsb.hike.spaceManager;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides directory/file size as requested via packet from server.
 *
 */
public class SpaceManagerUtils
{
    private static final String HIKE_STORAGE_INTERNAL = "internal";

    private static final String HIKE_STORAGE_EXTERNAL = "external";

    private static final String HIKE_STORAGE_SHARED = "shared";

    private static final String FILE = "file";

    private static final String DIRECTORY = "dir";

    public static final String CATEGORY_TAG = "CategoryItem";

    public static final String SUB_CATEGORY_TAG = "SubCategoryItem";

    public static final String SPACE_MANAGER_ITEMS = "spcMgrItems";

    /**
     * Computes and returns a Map with key as directory path and value as directory size. The boolean
     * parameter is set true if map of all files within directory with respective sizes is also required.
     * @param path - path of file/directory
     * @param shouldMapContainedFiles - if true, adds file path and size for each immediate child of given directory
     * @return
     */
    public static Map<String, String> getDirectorySizeMap(String path, boolean shouldMapContainedFiles)
    {
        Map<String, String> directorySizeMap = new HashMap<>();
        if(!isValidFile(path))
        {
            return null;
        }

        File reqDir = new File(path);

        if(reqDir.isFile())
        {
            long fileSize = reqDir.length();
            directorySizeMap.put(path, Long.toString(fileSize));
            return directorySizeMap;
        }

        File listFiles[] = reqDir.listFiles();
        if(listFiles == null)
        {
            return directorySizeMap;
        }
        else
        {
            if(listFiles.length == 0)
            {
                directorySizeMap.put(path, Long.toString(0));
                return directorySizeMap;
            }
            else
            {
                long dirSize = 0;
                for(File currFile : listFiles)
                {
                    long currFileSize;

                    if(currFile.isFile())
                    {
                        currFileSize = currFile.length();
                    }
                    else
                    {
                        currFileSize = Utils.folderSize(currFile);
                    }

                    if(shouldMapContainedFiles)
                    {
                        directorySizeMap.put(currFile.getAbsolutePath(), Long.toString(currFileSize));
                    }

                    dirSize += currFileSize;
                }

                directorySizeMap.put(path, Long.toString(dirSize));

                return directorySizeMap;
            }
        }
    }

    /**
     * Method to add storage consumption data for a particular file/directory into analytics
     * @param path - path of file/directory
     * @param shouldMapContainedFiles - if true, will add analytics entry for each immediate child of given directory
     */
    public static void recordDirectoryAnalytics(String path, boolean shouldMapContainedFiles)
    {
        Map<String, String> directorSizeMap = getDirectorySizeMap(path, shouldMapContainedFiles);
        if(directorSizeMap == null)
        {
            return;
        }

        for(Map.Entry<String, String> mapEntry : directorSizeMap.entrySet())
        {
            String filePath = mapEntry.getKey();
            String fileSize = mapEntry.getValue();
            String fileType = (new File(filePath)).isDirectory() ? DIRECTORY : FILE;
            recordStorageSpaceAnalytics(filePath, fileSize, fileType);
        }
    }

    /**
     * Method to record collected data into analytics
     * @param dirPath
     * @param dirSize
     * @param dirType
     */
    private static void recordStorageSpaceAnalytics(String dirPath, String dirSize, String dirType)
    {
        JSONObject metadata = new JSONObject();
        try
        {
            metadata.putOpt(AnalyticsConstants.EVENT_KEY, HikeConstants.SPACE_MANAGER.NOTIFY_DISK_SPACE_USAGE);
            metadata.putOpt(HikeConstants.SPACE_MANAGER.DIRECTORY_PATH, dirPath);
            metadata.putOpt(HikeConstants.SPACE_MANAGER.DIRECTORY_SIZE, dirSize);
            metadata.putOpt(HikeConstants.SPACE_MANAGER.DIRECTORY_TYPE, dirType);
            HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.SPACE_MANAGER.DISK_SPACE_INFO, HAManager.EventPriority.HIGH, metadata);
        }
        catch (JSONException je)
        {
            je.printStackTrace();
        }

    }

    /**
     * Method to add storage consumption data for Hike Internal Directory(/data/data/com.bsb.hike) into analytics.
     * @param shouldMapContainedFiles
     */
    public static void recordInternalHikeDirAnalytics(boolean shouldMapContainedFiles)
    {
        recordDirectoryAnalytics(HikeMessengerApp.getInstance().getApplicationContext().getFilesDir().getParent(), shouldMapContainedFiles);
    }

    /**
     * Method to add analytics for storage consumption by Hike External Directory
     * {@link Context#getExternalFilesDir(java.lang.String)}
     * @param shouldMapContainedFiles
     */
    public static void recordExternalHikeDirAnalytics(boolean shouldMapContainedFiles)
    {
        recordDirectoryAnalytics(HikeMessengerApp.getInstance().getApplicationContext().getExternalFilesDir(null).getAbsolutePath(), shouldMapContainedFiles);
    }

    /**
     * Method to add analytics for storage consumption by Hike files in device's shared external storage
     * @param shouldExapnd
     */
    public static void recordSharedHikeDirAnalytics(boolean shouldExapnd)
    {
        recordDirectoryAnalytics(HikeConstants.HIKE_DIRECTORY_ROOT, shouldExapnd);
    }

    /**
     * Method to format the computed file/directory size as a single point decimal string with appropriate unit
     * @param size - computed directory/file size to be formatted
     * @return
     */
    public static String formatFileSize(double size)
    {

        String[] sizes = {"B", "KB", "MB", "GB"};
        int base = 1024, count = 0;

        while(size >= base && count < sizes.length - 1)
        {
            size /= base;
            count++;
        }

        return String.format("%.1f%s", size, sizes[count]);
    }

    /**
     * Method to determine whether given path points to a valid file or not
     * @param path
     * @return
     */
    public static boolean isValidFile(String path)
    {
        if(TextUtils.isEmpty(path))
        {
            return false;
        }

        File reqDir = new File(path);

        if(!reqDir.exists())
        {
            return false;
        }

        if(reqDir.isFile() && reqDir.length() == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * Method to process list of directories received via packet from server
     * @param dirList
     */
    public static void processDirectoryList(JSONArray dirList)
    {
        if(dirList == null || dirList.length() == 0)
        {
            return;
        }

        try
        {
            for(int i = 0; i < dirList.length(); i++)
            {
                String dirPath = dirList.getJSONObject(i).optString(HikeConstants.SPACE_MANAGER.DIRECTORY_PATH, "");
                boolean shouldMapContainedFiles = dirList.getJSONObject(i).optBoolean(HikeConstants.SPACE_MANAGER.MAP_DIRECTORY, false);
                startIntentServiceAction(dirPath, shouldMapContainedFiles);
            }
        }
        catch (JSONException je)
        {
            je.printStackTrace();
        }
    }

    /**
     * Methhod to trigger {@link SpaceManagerIntentService} with appropriate action
     * @param dirPath
     * @param shouldMapContainedFiles
     */
    private static void startIntentServiceAction(String dirPath, boolean shouldMapContainedFiles)
    {
        switch (dirPath)
        {
            case HIKE_STORAGE_INTERNAL:
                IntentFactory.startStorageSpecIntent(SpaceManagerIntentService.ACTION_GET_INTERNAL_STORAGE_ANALYTICS,
                        null, shouldMapContainedFiles);
                break;

            case HIKE_STORAGE_EXTERNAL:
                IntentFactory.startStorageSpecIntent(SpaceManagerIntentService.ACTION_GET_EXTERNAL_STORAGE_ANALYTICS,
                        null, shouldMapContainedFiles);
                break;

            case HIKE_STORAGE_SHARED:
                IntentFactory.startStorageSpecIntent(SpaceManagerIntentService.ACTION_GET_SHARED_STORAGE_ANALYTICS,
                        null, shouldMapContainedFiles);
                break;

            default:
                IntentFactory.startStorageSpecIntent(SpaceManagerIntentService.ACTION_GET_CUSTOM_DIRECTORY_ANALYTICS,
                        dirPath, shouldMapContainedFiles);
        }
    }
}
