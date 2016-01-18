package com.bsb.hike.modules.stickersearch.provider;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by akhiltripathi
 */
public class StickerTagCache
{
    private static final String TAG = StickerTagCache.class.getName();

    private static final int VERSION = 1;

    private static StickerTagCache mCache;//To Do : Discuss if this should be volatile

    private static final Object mCacheInitLock = new Object();

    private int currentStickerCount;

    private int tagType;

    private int currentCacheSize;

    private void init()
    {
        switch(mCache.tagType)
        {
            case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
                currentCacheSize = StickerSearchUtils.getUndownloadedTagsStickersCount();
                break;
        }
    }

    private StickerTagCache(int tagType)
    {
        this.tagType = tagType;
    }

    /* Get the instance of this class from outside */
    public static StickerTagCache getInstance(int tagType)
    {
        if (mCache == null)
        {
            synchronized (mCacheInitLock)
            {
                if (mCache == null)
                {
                    mCache = new StickerTagCache(tagType);
                    mCache.init();
                }
            }
        }

        return mCache;
    }

    public boolean isValidStickerTagsJSON(JSONObject stickerJSON)
    {
        if (!Utils.isHoneycombOrHigher())
        {
            Logger.d(TAG, "setupStickerSearchWizard(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
            StickerManager.getInstance().removeStickerSet(mCache.tagType);
            return false;
        }

        if (stickerJSON == null)
        {
            return false;
        }

        JSONObject packsData = stickerJSON.optJSONObject(HikeConstants.PACKS);
        if ((packsData == null) || (packsData.length() <= 0))
        {
            return false;
        }

        return true;
    }

    public void preTagsInsertTask(JSONObject stickerJSON)
    {

        if(mCache.tagType !=  StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD)
        {
            //Currently cache only applied for undownloaded tags thus no pre processing required for other kinds
            return;
        }

        JSONObject packsData = stickerJSON.optJSONObject(HikeConstants.PACKS);

        Iterator<String> packs = packsData.keys();

        int stickersToBeInsertedCount = 0;

        while (packs.hasNext())
        {
            String packId = packs.next();
            if (Utils.isBlank(packId)) {
                Logger.e(TAG, "setupStickerSearchWizard(), Invalid pack id.");
                continue;
            }

            JSONObject packData = packsData.optJSONObject(packId);
            if ((packData == null) || (packData.length() <= 0)) {
                Logger.e(TAG, "setupStickerSearchWizard(), Empty json data for pack: " + packId);
                continue;
            }

            JSONObject stickersData = packData.optJSONObject(HikeConstants.STICKERS);
            if (stickersData == null)  {
                Logger.e(TAG, "setupStickerSearchWizard(), No sticker was found inside pack: " + packId);
                continue;
            }

            stickersToBeInsertedCount += stickersData.length();

        }

        HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_TAG_COUNT, stickersToBeInsertedCount + currentCacheSize);
    }

    public void insertTags(JSONObject stickerJSON)
    {

        if(!isValidStickerTagsJSON(stickerJSON))
        {
            return;
        }

        preTagsInsertTask(stickerJSON);
        StickerSearchDataController.getInstance().setupStickerSearchWizard(stickerJSON, mCache.tagType);
    }





}
