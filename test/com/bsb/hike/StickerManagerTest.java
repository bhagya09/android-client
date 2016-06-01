package com.bsb.hike;

import android.util.Log;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by ashishagarwal on 31/05/16.
 */

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = { MultiDexShadowClass.class })
public class StickerManagerTest {

    private String TAG = "StickerManagerTest";

    @Before
    public void setUp()
    {
        HikeConversationsDatabase.getInstance().getWritableDatabase().acquireReference();

        ShadowLog.stream = System.out;
        Log.d(TAG, "setUp");
    }

    @Test
    public void isMiniStickersEnabledTest()
    {
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MINI_STICKER_ENABLED, true);
        assertTrue(StickerManager.getInstance().isMiniStickersEnabled());

        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MINI_STICKER_ENABLED, false);
        assertFalse(StickerManager.getInstance().isMiniStickersEnabled());

        HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.MINI_STICKER_ENABLED);
        assertTrue(StickerManager.getInstance().isMiniStickersEnabled());

    }


    @Test
    public void isShopSearchEnabledTest()
    {
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, true);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, true);
        assertTrue(StickerManager.getInstance().isShopSearchEnabled());

        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, true);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, false);
        assertFalse(StickerManager.getInstance().isShopSearchEnabled());

        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, false);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, true);
        assertFalse(StickerManager.getInstance().isShopSearchEnabled());

        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, false);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, false);
        assertFalse(StickerManager.getInstance().isShopSearchEnabled());

        HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED);
        HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE);
        assertFalse(StickerManager.getInstance().isShopSearchEnabled());


    }

}
