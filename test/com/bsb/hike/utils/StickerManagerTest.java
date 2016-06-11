package com.bsb.hike.utils;

import android.util.Log;

import com.bsb.hike.BuildConfig;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.roboClasses.CustomRobolectricGradleTestRunner;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.roboClasses.MultiDexShadowClass;
import com.bsb.hike.db.HikeConversationsDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

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

    @Test
    public void parseStickerCategoryMetadataTest() throws JSONException {

        //Checking NUll/Empty cases
        assertNull(StickerManager.getInstance().parseStickerCategoryMetadata(null));
        assertNull(StickerManager.getInstance().parseStickerCategoryMetadata(new JSONObject()));
        assertNull(StickerManager.getInstance().parseStickerCategoryMetadata(new JSONObject("{}")));

        //Checking compulsory info cases
        JSONObject jsonWithoutCatID = new JSONObject("{\"name\": \"Girl Power\",\n" +
                "\"sticker_list\": [\"036_tearycrying.png\"], \"ts\": 1463530707, \"nos\": 37, \"size\": 685643}");
        assertNull(StickerManager.getInstance().parseStickerCategoryMetadata(jsonWithoutCatID));


        JSONObject jsonWithoutCatName = new JSONObject("{\"catId\": \"humanoid2\",\n" +
                "\"sticker_list\": [\"036_tearycrying.png\"], \"ts\": 1463530707, \"nos\": 37, \"size\": 685643}");
        assertNull(StickerManager.getInstance().parseStickerCategoryMetadata(jsonWithoutCatName));

        //Checking correct input case for Sticker Category Details Download
        JSONObject properInputJson = new JSONObject("{\"name\": \"Girl Power\", \"catId\": \"humanoid2\",\n" +
                "\"sticker_list\": [\"036_tearycrying.png\", \"030_cry.png\",\n" +
                "\"023_Blushing.png\", \"039_eyesrolling.png\", \"035_angry.png\",\n" +
                "\"003_fist.png\", \"026_fire.png\", \"047_happyexcited.png\",\n" +
                "\"032_oops.png\", \"045_smirk.png\", \"037_fine.png\", \"034_boxing.png\",\n" +
                "\"046_rockon.png\", \"005_horror.png\", \"043_sad.png\",\n" +
                "\"038_talktohand.png\", \"040_yuck.png\", \"006_temptation.png\",\n" +
                "\"002_jaw.png\", \"044_stopit.png\", \"029_wink2.png\", \"028_wink.png\",\n" +
                "\"027_muscle.png\", \"024_worry.png\", \"010_weight.png\", \"041_enough.png\",\n" +
                "\"048_zipped.png\", \"011_threadmill.png\", \"001_sale.png\",\n" +
                "\"009_lost.png\", \"018_Dance.png\", \"025_nails.png\", \"031_angry.png\",\n" +
                "\"020_Cinema.png\", \"042_fakesmile.png\", \"012_vacation.png\",\n" +
                "\"017_Dress.png\"], \"ts\": 1463530707, \"nos\": 37, \"size\": 685643}");

        StickerCategory testStickerCategory = new StickerCategory.Builder()
                .setCategoryId("humanoid2")
                .setCategoryName("Girl Power")
                .setCategorySize(685643)
                .setTotalStickers(37)
                .setPackUpdationTime(1463530707)
                .setAllStickerListString((new JSONArray("[\"036_tearycrying.png\", \"030_cry.png\",\n" +
                        "\"023_Blushing.png\", \"039_eyesrolling.png\", \"035_angry.png\",\n" +
                        "\"003_fist.png\", \"026_fire.png\", \"047_happyexcited.png\",\n" +
                        "\"032_oops.png\", \"045_smirk.png\", \"037_fine.png\", \"034_boxing.png\",\n" +
                        "\"046_rockon.png\", \"005_horror.png\", \"043_sad.png\",\n" +
                        "\"038_talktohand.png\", \"040_yuck.png\", \"006_temptation.png\",\n" +
                        "\"002_jaw.png\", \"044_stopit.png\", \"029_wink2.png\", \"028_wink.png\",\n" +
                        "\"027_muscle.png\", \"024_worry.png\", \"010_weight.png\", \"041_enough.png\",\n" +
                        "\"048_zipped.png\", \"011_threadmill.png\", \"001_sale.png\",\n" +
                        "\"009_lost.png\", \"018_Dance.png\", \"025_nails.png\", \"031_angry.png\",\n" +
                        "\"020_Cinema.png\", \"042_fakesmile.png\", \"012_vacation.png\",\n" +
                        "\"017_Dress.png\"]")).toString())
                .build();

        StickerCategory outputCategory = StickerManager.getInstance().parseStickerCategoryMetadata(properInputJson);
        assertEquals(outputCategory, testStickerCategory);
        assertEquals(outputCategory.getCategoryName(), testStickerCategory.getCategoryName());
        assertEquals(outputCategory.getCategorySize(), testStickerCategory.getCategorySize());
        assertEquals(outputCategory.getPackUpdationTime(), testStickerCategory.getPackUpdationTime());
        assertEquals(outputCategory.getTotalStickers(), testStickerCategory.getTotalStickers());
        assertEquals(outputCategory.getAllStickerListString(), testStickerCategory.getAllStickerListString());
        assertEquals(outputCategory.getAllStickers().size(), testStickerCategory.getAllStickers().size());
        assertNotEquals(outputCategory.getAllStickers().size(), 0);

    }

}
