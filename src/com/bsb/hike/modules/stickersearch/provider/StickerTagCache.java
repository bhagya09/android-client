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

    private volatile static StickerTagCache mCache;

    private int tagState;

    private int currentCacheSize;

    private void setupCacheForTagState()
    {
        switch(mCache.tagState)
        {
            case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
                currentCacheSize = StickerSearchUtils.getUndownloadedTagsStickersCount();
                break;
        }
    }

    private StickerTagCache()
    {
    }

    /* Get the instance of this class from outside */
    public static StickerTagCache getInstance()
    {
        if (mCache == null)
        {
            synchronized (StickerTagCache.class)
            {
                if (mCache == null)
                {
                    mCache = new StickerTagCache();
                }
            }
        }

        return mCache;
    }

    public boolean isValidStickerTagsJSON(JSONObject stickerJSON)
    {
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

        if(mCache.tagState !=  StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD)
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

    public void insertTags(JSONObject stickerJSON,int tagState)
    {

        setTagState(tagState);

        setupCacheForTagState();

        if(!isValidStickerTagsJSON(stickerJSON))
        {
            return;
        }

        preTagsInsertTask(stickerJSON);
        StickerSearchDataController.getInstance().setupStickerSearchWizard(stickerJSON, mCache.tagState);
    }


    public void setTagState(int tagState) {
        this.tagState = tagState;
    }
}
