package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.gson.JsonObject;

import android.text.TextUtils;

public enum StickerSearchSetupManager {

	INSTANCE;
	private static final String TAG = StickerSearchSetupManager.class.getSimpleName();

	public static StickerSearchSetupManager getInstance() {
		return INSTANCE;
	}

	public void init() {
		HikeStickerSearchDatabase.init(HikeMessengerApp.getInstance());

		synchronized (StickerSearchSetupManager.class) {
			if (!HikeSharedPreferenceUtil.getInstance().getData("isPopulated", false)) {
				HikeStickerSearchDatabase.getInstance().markDataInsertionInitiation();
				String [] tables = new String [27];
				tables [0] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH;
				for (int i = 0; i < 26; i++) {
					tables [i + 1] = HikeStickerSearchBaseConstants.TABLE_STICKER_TAG_SEARCH + (char) (((int) 'A') + i);
				}
				Logger.d(TAG, "Starting population first time...");
				HikeStickerSearchDatabase.getInstance().createVirtualTable(tables);
				HikeSharedPreferenceUtil.getInstance().saveData("isPopulated", true);
			}
		}
	}

	public void setupStickerSearchWizard(JSONObject json, int state) {

		synchronized (StickerSearchSetupManager.class) {
			JSONArray tagData = json.optJSONArray("tagdata");
			
			if(tagData == null || tagData.length() == 0)
			{
				return;
			}
			
			ArrayList<String> tags = new ArrayList<String>();
			ArrayList<String> stickers = new ArrayList<String>();
			
			

			for (int i = 0; i < tagData.length(); i++) {
				JSONObject obj = tagData.optJSONObject(i);
				
				if(obj == null)
				{
					continue;
				}
				
				String catId = obj.optString("catId");
				String [] sIds = obj.optString("sIds").split(",");
				JSONArray tagList = obj.optJSONArray("tags");
				
				if(tagList == null || tagList.length() == 0)
				{
					continue;
				}
				
				ArrayList<String> temp = new ArrayList<String>();

				for (int j = 0; j < tagList.length(); j++) {
					JSONObject cobj = tagList.optJSONObject(j);

					String [] s = new String [8];
					s [0] = cobj.optString("*cbehaviour");
					s [1] = cobj.optString("*ctheme");
					s [2] = cobj.optString("*creaction");
					s [3] = cobj.optString("*cresponse");
					s [4] = cobj.optString("*cemotion");
					s [5] = cobj.optString("*cfeeling");
					s [6] = cobj.optString("*cgeneral");
					s [7] = cobj.optString("*cother");

					for (int m = 0; m <8; m++) {
						if (!TextUtils.isEmpty(s [m])) {
							temp.addAll(Arrays.asList(s [m].split(",")));
						}
					}
				}

				for (int k = 0; k < sIds.length; k++) {
					tags.addAll(temp);
					String s = sIds [k] + ":" + catId;
					for (int l = 0; l < temp.size(); l++) {
						stickers.add(s);
					}
				}
			}

			HikeStickerSearchDatabase.getInstance().insertIntoFTSTable(tags, HikeStickerSearchDatabase.getInstance().insertIntoPrimaryTable(tags, stickers));
			Set<String> categorySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
			Set<String> updateSet = new HashSet<String>();
			if (categorySet != null) {
				updateSet.addAll(categorySet);
				Logger.d(TAG, "initial list: " + updateSet);
				for (String s : categorySet) {
					if (stickers.contains(s)) {
						updateSet.remove(s);
					}
				}
				Logger.d(TAG, "updated list: " + updateSet);
				HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, updateSet);
			}
		}
	}

	public void updateStickerSearchWizrd(JsonObject json, int state) {
		
	}

	public void updateStickerList(Set<String> stickerInfo) {

		synchronized (StickerSearchSetupManager.class) {
			HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfo);
		}
	}
}
