/**
 * File   : StickerSearchDataController.java
 * Content: It provides intermediate gateway to functions of Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public enum StickerSearchDataController
{

	INSTANCE;
	private static final String TAG = StickerSearchDataController.class.getSimpleName();

	public static StickerSearchDataController getInstance()
	{
		return INSTANCE;
	}

	public void init()
	{
		Logger.i(TAG, "init()");
		HikeStickerSearchDatabase.getInstance().prepare();
	}

	public void setupStickerSearchWizard(JSONObject json, int state)
	{
		Logger.i(TAG, "setupStickerSearchWizard(" + json + ", " + state + ")");

		synchronized (StickerSearchDataController.class)
		{
			JSONObject packs = json.optJSONObject(HikeConstants.PACKS);
			if (packs == null || packs.length() == 0)
			{
				return;
			}

			Iterator<String> categories = packs.keys();
			ArrayList<String> tagList = new ArrayList<String>();
			ArrayList<Integer> tagPriorityList = new ArrayList<Integer>();
			ArrayList<Integer> stickerMomentList = new ArrayList<Integer>();
			ArrayList<String> stickerList = new ArrayList<String>();
			Set<String> untaggedSet = new HashSet<String>();

			while (categories.hasNext())
			{
				String categoryId = categories.next();
				if (Utils.isBlank(categoryId))
				{
					Logger.d(TAG, "Invalid pack id");
					continue;
				}

				JSONObject packData = packs.optJSONObject(categoryId);
				if (packData == null || packData.length() == 0)
				{
					Logger.d(TAG, "Empty pack data: " + categoryId);
					continue;
				}

				JSONObject stickerData = packData.optJSONObject(HikeConstants.STICKERS);
				if (stickerData == null || stickerData.length() == 0)
				{
					Logger.d(TAG, "No stickers for pack: " + categoryId);
					continue;
				}
				Iterator<String> stickers = stickerData.keys();

				if (packData.has("stories"))
				{
					JSONArray storiesData = packData.optJSONArray("stories");
					if (storiesData != null)
					{
						Logger.d(TAG, categoryId + ": Stories = " + storiesData);
					}
				}

				while (stickers.hasNext())
				{
					String stickerId = stickers.next();
					if (Utils.isBlank(stickerId))
					{
						Logger.d(TAG, "Invalid sticker id for pack: " + categoryId);
						continue;
					}

					String stickerInfo = StickerManager.getInstance().getStickerSetString(stickerId, categoryId);
					JSONObject data = stickerData.optJSONObject(stickerId);
					if (data == null || data.length() == 0)
					{
						Logger.d(TAG, "Empty sticker data: " + stickerInfo);
						continue;
					}

					if (Utils.isBlank(data.optString(HikeConstants.IMAGE)))
					{
						Logger.d(TAG, "Empty sticker image data: " + stickerInfo);
						continue;
					}

					JSONObject tagData = data.optJSONObject("tag_data");
					boolean isTagDataEmpty = true;

					if (tagData != null && tagData.length() > 0)
					{
						JSONObject tags = tagData.optJSONObject("catgrs");
						int stickerDataCount = 0;

						if (tags != null && tags.length() > 0)
						{
							Iterator<String> languages = tags.keys();
							ArrayList<String> tempElements = new ArrayList<String>();
							ArrayList<Integer> tempPriorities = new ArrayList<Integer>();
							ArrayList<String> tempMatchElements = new ArrayList<String>();

							while (languages.hasNext())
							{
								String languageId = languages.next();
								JSONObject dictionaryData = tags.optJSONObject(languageId);
								tempMatchElements.clear();

								if (dictionaryData != null && dictionaryData.length() > 0)
								{
									JSONArray[] s = new JSONArray[9];
									s[0] = dictionaryData.optJSONArray("*cbehaviour");
									s[1] = dictionaryData.optJSONArray("*ctheme");
									s[2] = dictionaryData.optJSONArray("*creaction");
									s[3] = dictionaryData.optJSONArray("*cresponse");
									s[4] = dictionaryData.optJSONArray("*cemotion");
									s[5] = dictionaryData.optJSONArray("*cfeeling");
									s[6] = dictionaryData.optJSONArray("*cgeneral");
									s[7] = dictionaryData.optJSONArray("*cother");
									s[8] = dictionaryData.optJSONArray("*ctitle");

									if (s[8] != null)
									{
										Logger.d(TAG, "Received tags: " + s[8].toString());
										for (int i = 0; i < s[8].length(); i++)
										{
											if (!Utils.isBlank(s[8].optString(i)))
											{
												tempMatchElements.add(s[8].optString(i).toUpperCase(Locale.ENGLISH));
											}
										}
									}
									else
									{
										Logger.d(TAG, stickerInfo + " dictionary at index = " + 8 + " is invalid or, empty.");
									}

									for (int m = 0; m < 8; m++)
									{
										if (s[m] != null)
										{
											Logger.d(TAG, "Received tags: " + s[m].toString());
											for (int i = 0; i < s[m].length(); i++)
											{
												if (!Utils.isBlank(s[m].optString(i)))
												{
													tempElements.add(s[m].optString(i).toUpperCase(Locale.ENGLISH));
													tempPriorities.add(tempMatchElements.indexOf(s[m].optString(i).toUpperCase(Locale.ENGLISH)));
												}
											}
										}
										else
										{
											Logger.d(TAG, stickerInfo + " dictionary at index = " + m + " is invalid or, empty.");
										}
									}
								}
							}

							stickerDataCount = tempElements.size();
							if (stickerDataCount > 0)
							{
								isTagDataEmpty = false;

								tagList.addAll(tempElements);
								tagPriorityList.addAll(tempPriorities);
							}
						}

						JSONObject attributes = tagData.optJSONObject("attrbs");
						if (attributes != null)
						{
							Logger.d(TAG, "No. of attributes attached with " + stickerInfo + " = " + attributes.length());
							int momentCode = attributes.optInt("*atime", -1);
							for (int i = 0; i < stickerDataCount; i++)
							{
								stickerMomentList.add(momentCode);
								stickerList.add(stickerInfo);
							}
						}
						else
						{
							Logger.d(TAG, "No attribute attached with " + stickerInfo);
							for (int i = 0; i < stickerDataCount; i++)
							{
								stickerMomentList.add(-1);
								stickerList.add(stickerInfo);
							}
						}
					}

					if (isTagDataEmpty)
					{
						untaggedSet.add(stickerInfo);
					}
				}
			}

			Logger.d(TAG, "Stickers: " + stickerList);
			Logger.d(TAG, "Tags: " + tagList);
			Logger.d(TAG, "Tag Priorities: " + tagPriorityList);
			Logger.d(TAG, "Sticker moments: " + stickerMomentList);

			HikeStickerSearchDatabase.getInstance().insertIntoFTSTable(tagList, HikeStickerSearchDatabase.getInstance().insertIntoPrimaryTable(tagList, tagPriorityList, stickerMomentList, stickerList));

			Logger.d(TAG, "Untagged stickers: " + untaggedSet);
		}
	}

	public void updateStickerSearchWizard(JSONObject json, int state)
	{
		Logger.i(TAG, "updateStickerSearchWizard(" + json + ", " + state + ")");

		synchronized (StickerSearchDataController.class)
		{
			JSONObject packs = json.optJSONObject(HikeConstants.PACKS);
			if (packs == null || packs.length() == 0)
			{
				return;
			}

			Iterator<String> categories = packs.keys();
			ArrayList<String> tagList = new ArrayList<String>();
			ArrayList<Integer> tagPriorityList = new ArrayList<Integer>();
			ArrayList<Integer> stickerMomentList = new ArrayList<Integer>();
			ArrayList<String> stickerList = new ArrayList<String>();
			Set<String> untaggedSet = new HashSet<String>();

			while (categories.hasNext())
			{
				String categoryId = categories.next();
				if (Utils.isBlank(categoryId))
				{
					Logger.d(TAG, "Invalid pack id");
					continue;
				}

				JSONObject packData = packs.optJSONObject(categoryId);
				if (packData == null || packData.length() == 0)
				{
					Logger.d(TAG, "Empty pack data: " + categoryId);
					continue;
				}

				JSONObject stickerData = packData.optJSONObject(HikeConstants.STICKERS);
				if (stickerData == null || stickerData.length() == 0)
				{
					Logger.d(TAG, "No stickers for pack: " + categoryId);
					continue;
				}
				Iterator<String> stickers = stickerData.keys();

				if (packData.has("stories"))
				{
					JSONArray storiesData = packData.optJSONArray("stories");
					if (storiesData != null)
					{
						Logger.d(TAG, categoryId + ": Stories = " + storiesData);
					}
				}

				while (stickers.hasNext())
				{
					String stickerId = stickers.next();
					if (Utils.isBlank(stickerId))
					{
						Logger.d(TAG, "Invalid sticker id for pack: " + categoryId);
						continue;
					}

					String stickerInfo = StickerManager.getInstance().getStickerSetString(stickerId, categoryId);
					JSONObject data = stickerData.optJSONObject(stickerId);
					if (data == null || data.length() == 0)
					{
						Logger.d(TAG, "Empty sticker data: " + stickerInfo);
						continue;
					}

					JSONObject tagData = data.optJSONObject("tag_data");
					boolean isTagDataEmpty = true;

					if (tagData != null && tagData.length() > 0)
					{
						JSONObject tags = tagData.optJSONObject("catgrs");
						int stickerDataCount = 0;

						if (tags != null && tags.length() > 0)
						{
							Iterator<String> languages = tags.keys();
							ArrayList<String> tempElements = new ArrayList<String>();
							ArrayList<Integer> tempPriorities = new ArrayList<Integer>();
							ArrayList<String> tempMatchElements = new ArrayList<String>();

							while (languages.hasNext())
							{
								String languageId = languages.next();
								JSONObject dictionaryData = tags.optJSONObject(languageId);
								tempMatchElements.clear();

								if (dictionaryData != null && dictionaryData.length() > 0)
								{
									JSONArray[] s = new JSONArray[9];
									s[0] = dictionaryData.optJSONArray("*cbehaviour");
									s[1] = dictionaryData.optJSONArray("*ctheme");
									s[2] = dictionaryData.optJSONArray("*creaction");
									s[3] = dictionaryData.optJSONArray("*cresponse");
									s[4] = dictionaryData.optJSONArray("*cemotion");
									s[5] = dictionaryData.optJSONArray("*cfeeling");
									s[6] = dictionaryData.optJSONArray("*cgeneral");
									s[7] = dictionaryData.optJSONArray("*cother");
									s[8] = dictionaryData.optJSONArray("*ctitle");

									if (s[8] != null)
									{
										Logger.d(TAG, "Received tags: " + s[8].toString());
										for (int i = 0; i < s[8].length(); i++)
										{
											if (!Utils.isBlank(s[8].optString(i)))
											{
												tempMatchElements.add(s[8].optString(i).toUpperCase(Locale.ENGLISH));
											}
										}
									}
									else
									{
										Logger.d(TAG, stickerInfo + " dictionary at index = " + 8 + " is invalid or, empty.");
									}

									for (int m = 0; m < 8; m++)
									{
										if (s[m] != null)
										{
											Logger.d(TAG, "Received tags: " + s[m].toString());
											for (int i = 0; i < s[m].length(); i++)
											{
												if (!Utils.isBlank(s[m].optString(i)))
												{
													tempElements.add(s[m].optString(i).toUpperCase(Locale.ENGLISH));
													tempPriorities.add(tempMatchElements.indexOf(s[m].optString(i).toUpperCase(Locale.ENGLISH)));
												}
											}
										}
										else
										{
											Logger.d(TAG, stickerInfo + " dictionary at index = " + m + " is invalid or, empty.");
										}
									}
								}
							}

							int numberOfElements = tempElements.size();
							if (numberOfElements > 0)
							{
								isTagDataEmpty = false;

								tagList.addAll(tempElements);
								tagPriorityList.addAll(tempPriorities);
							}
						}

						JSONObject attributes = tagData.optJSONObject("attrbs");
						if (attributes != null)
						{
							Logger.d(TAG, "No. of attributes attached with " + stickerInfo + " = " + attributes.length());
							int momentCode = attributes.optInt("*atime", -1);
							for (int i = 0; i < stickerDataCount; i++)
							{
								stickerMomentList.add(momentCode);
								stickerList.add(stickerInfo);
							}
						}
						else
						{
							Logger.d(TAG, "No attribute attached with " + stickerInfo);
							for (int i = 0; i < stickerDataCount; i++)
							{
								stickerMomentList.add(-1);
								stickerList.add(stickerInfo);
							}
						}
					}

					if (isTagDataEmpty)
					{
						untaggedSet.add(stickerInfo);
					}
				}
			}

			Logger.d(TAG, "Stickers: " + stickerList);
			Logger.d(TAG, "Tags: " + tagList);
			Logger.d(TAG, "Tag Priorities: " + tagPriorityList);
			Logger.d(TAG, "Sticker moments: " + stickerMomentList);

			HikeStickerSearchDatabase.getInstance().insertIntoFTSTable(tagList, HikeStickerSearchDatabase.getInstance().insertIntoPrimaryTable(tagList, tagPriorityList, stickerMomentList, stickerList));

			Logger.d(TAG, "Current untagged stickers: " + untaggedSet);
			Set<String> pendingRetrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
			Set<String> updateRetrySet = new HashSet<String>();
			Logger.d(TAG, "previous retry list: " + pendingRetrySet);

			if (pendingRetrySet != null)
			{
				for (String s : pendingRetrySet)
				{
					if (!stickerList.contains(s) && !untaggedSet.contains(s))
					{
						updateRetrySet.add(s);
					}
				}
			}

			Logger.d(TAG, "updated retry list: " + updateRetrySet);
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, updateRetrySet);
		}
	}

	public void updateStickerList(Set<String> stickerInfo)
	{
		synchronized (StickerSearchDataController.class)
		{
			HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfo);
		}
	}
}