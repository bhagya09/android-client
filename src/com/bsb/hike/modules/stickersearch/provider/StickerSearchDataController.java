/**
 * File   : StickerSearchDataController.java
 * Content: It provides intermediate gateway to functions of Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import com.google.gson.JsonObject;

public enum StickerSearchDataController {

	INSTANCE;
	private static final String TAG = StickerSearchDataController.class.getSimpleName();

	private static final Object sStickerSearckDataLock = new Object();

	public static StickerSearchDataController getInstance() {
		return INSTANCE;
	}

	public void init() {
		Logger.d(TAG, "init()");
		HikeStickerSearchDatabase.init();

		synchronized (sStickerSearckDataLock) {
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
		Logger.d(TAG, "setupStickerSearchWizard(" + json + ", " + state + ")");

		synchronized (StickerSearchDataController.class) {
			JSONObject packs = json.optJSONObject("packs");
			if (packs == null || packs.length() == 0) {
				return;
			}

			Iterator<String> categories = packs.keys();
			ArrayList<String> tagList = new ArrayList<String>();
			ArrayList<String> stickerList = new ArrayList<String>();
			Set<String> untaggedSet = new HashSet<String>();
			Set<String> retrySet = new HashSet<String>();

			while (categories.hasNext()) {
				String categoryId = categories.next();
				JSONObject packData = packs.optJSONObject(categoryId);
				if (packData == null || packData.length() == 0) {
					Logger.d(TAG, "Empty pack data: " + categoryId);
					continue;
				}

				JSONObject stickerData = packData.optJSONObject("stkrs");
				if (stickerData == null || stickerData.length() == 0) {
					Logger.d(TAG, "No stickers for pack: " + categoryId);
					continue;
				}
				Iterator<String> stickers = stickerData.keys();

				if (packData.has("stories")) {
					Object storiesData = packData.opt("stories");
					if (storiesData != null && storiesData instanceof String []) {
						String [] stories = (String []) storiesData;
						Logger.d(TAG, categoryId + ": Stories = " + Arrays.toString(stories));
					}
				}

				while (stickers.hasNext()) {
					String stickerId = stickers.next();
					JSONObject data = stickerData.optJSONObject(stickerId);
					if (data == null || data.length() == 0) {
						Logger.d(TAG, "Empty sticker data: " + categoryId + "_" + stickerId);
						continue;
					}

					if (!data.has("img") || data.opt("img") == null) {
						Logger.d(TAG, "Empty sticker image data: " + categoryId + "_" + stickerId);
						continue;
					}

					JSONObject tagData = data.optJSONObject("tag_data");
					boolean isTagDataEmpty = true;

					if (tagData != null && tagData.length() > 0) {
						JSONObject tags = tagData.optJSONObject("catgrs");

						if (tags != null && tags.length() > 0) {
							Iterator<String> languages = tags.keys();
							ArrayList<String> tempElements = new ArrayList<String>();

							while (languages.hasNext()) {
								String languageId = languages.next();
								JSONObject dictionaryData = tags.optJSONObject(languageId);

								if (dictionaryData != null && dictionaryData.length() > 0) {
									Object [] s = new String [8];
									s [0] = dictionaryData.opt("*cbehaviour");
									s [1] = dictionaryData.opt("*ctheme");
									s [2] = dictionaryData.opt("*creaction");
									s [3] = dictionaryData.opt("*cresponse");
									s [4] = dictionaryData.opt("*cemotion");
									s [5] = dictionaryData.opt("*cfeeling");
									s [6] = dictionaryData.opt("*cgeneral");
									s [7] = dictionaryData.opt("*cother");

									boolean isCurruptedDictionary = false;
									for (int m = 0; m < 8; m++) {
										if (s [m] != null && s [m] instanceof String []) {
											String [] dictionary = (String []) s [m]; 

											for (String element : dictionary) {
												tempElements.add(element);
											}
										} else {
											isCurruptedDictionary = true;
										}
									}

									if (isCurruptedDictionary) {
										retrySet.add(stickerId + ":" + categoryId);
										isTagDataEmpty = false;
									}
								}
							}

							int numberOfElements = tempElements.size();
							if (numberOfElements > 0) {
								isTagDataEmpty = false;
								String stickerInfo = stickerId + ":" + categoryId;

								tagList.addAll(tempElements);
								for (int i = 0; i <numberOfElements; i++) {
									stickerList.add(stickerInfo);
								}
							}
						}

						JSONObject attributes = tagData.optJSONObject("attrbs");
						if (attributes != null) {
							Logger.d(TAG, "No. of attributes attached with " + categoryId + "_" + stickerId + " = " + attributes.length());
						} else {
							Logger.d(TAG, "No attribute attached with " + categoryId + "_" + stickerId);
						}
					}

					if (isTagDataEmpty) {
						untaggedSet.add(stickerId + ":" + categoryId);
					}
				}
			}

			HikeStickerSearchDatabase.getInstance().insertIntoFTSTable(tagList, HikeStickerSearchDatabase.getInstance().insertIntoPrimaryTable(tagList, stickerList));

			Set<String> pendingRetrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
			Logger.d(TAG, "previous retry list: " + pendingRetrySet);

			if (pendingRetrySet != null) {
				for (String s : pendingRetrySet) {
					if (!stickerList.contains(s)) {
						retrySet.add(s);
					}
				}
			}

			Logger.d(TAG, "updated retry list: " + retrySet);
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, retrySet);

			Logger.d(TAG, "Untagged stickers: " + untaggedSet);
		}
	}

	public void updateStickerSearchWizrd(JsonObject json, int state) {
		
	}

	public void updateStickerList(Set<String> stickerInfo) {

		synchronized (StickerSearchDataController.class) {
			HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfo);
		}
	}
}