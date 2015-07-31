/**
 * File   : StickerSearchDataController.java
 * Content: It provides intermediate gateway to functions of Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
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

		synchronized (StickerSearchDataController.class)
		{
			HikeStickerSearchDatabase.getInstance().prepare();
		}
	}

	public void clear(boolean isNeedToClearAllData)
	{
		Logger.i(TAG, "clear(" + isNeedToClearAllData + ")");

		synchronized (StickerSearchDataController.class)
		{
			HikeStickerSearchDatabase.getInstance().deleteDataInTables(isNeedToClearAllData);;
		}
	}

	public void setupStickerSearchWizard(JSONObject json, int state)
	{
		Logger.i(TAG, "setupStickerSearchWizard(" + json + ", " + state + ")");

		if (!((state == StickerSearchConstants.TRIAL_STICKER_DATA_FIRST_SETUP) || (state == StickerSearchConstants.TRIAL_STICKER_DATA_UPDATE_REFRESH)))
		{
			Logger.e(TAG, "setupStickerSearchWizard(), Invalid trial request.");
			return;
		}

		JSONObject packsData = json.optJSONObject(HikeConstants.PACKS);
		if ((packsData == null) || (packsData.length() <= 0))
		{
			return;
		}

		Set<String> receivedStickerSet = new HashSet<String>();
		HashSet<String> stickerCodeSet = new HashSet<String>();
		Map<String, ArrayList<String>> packStoryData = new HashMap<String, ArrayList<String>>();
		ArrayList<TagToStcikerDataContainer> stickersTagData = new ArrayList<TagToStcikerDataContainer>();
		Iterator<String> categories = packsData.keys();

		while (categories.hasNext())
		{
			String packId = categories.next();
			if (Utils.isBlank(packId))
			{
				Logger.e(TAG, "setupStickerSearchWizard(), Invalid pack id.");
				continue;
			}

			JSONObject packData = packsData.optJSONObject(packId);
			if ((packData == null) || (packData.length() <= 0))
			{
				Logger.e(TAG, "setupStickerSearchWizard(), Empty json data for pack: " + packId);
				continue;
			}

			JSONObject stickersData = packData.optJSONObject(HikeConstants.STICKERS);
			if ((stickersData == null) || (stickersData.length() <= 0))
			{
				Logger.e(TAG, "setupStickerSearchWizard(), No sticker was found inside pack: " + packId);
				continue;
			}

			Iterator<String> stickers = stickersData.keys();
			int packTagDataCount = 0;

			while (stickers.hasNext())
			{
				String stickerId = stickers.next();
				if (Utils.isBlank(stickerId))
				{
					Logger.e(TAG, "setupStickerSearchWizard(), Invalid sticker id inside pack: " + packId);
					continue;
				}

				String stickerInfo = StickerManager.getInstance().getStickerSetString(stickerId, packId);
				receivedStickerSet.add(stickerInfo);

				JSONObject stickerData = stickersData.optJSONObject(stickerId);

				if ((stickerData == null) || (stickerData.length() <= 0))
				{
					Logger.e(TAG, "setupStickerSearchWizard(), Empty json data for sticker: " + stickerInfo);
					continue;
				}

				if (state == StickerSearchConstants.TRIAL_STICKER_DATA_FIRST_SETUP)
				{
					if (TextUtils.isEmpty(stickerData.optString(HikeConstants.IMAGE)))
					{
						Logger.e(TAG, "setupStickerSearchWizard(), Empty image data for sticker: " + stickerInfo);
						continue;
					}
				}
				else if (state == StickerSearchConstants.TRIAL_STICKER_DATA_UPDATE_REFRESH)
				{
					Logger.v(TAG, "setupStickerSearchWizard(), No dependency on image data for sticker: " + stickerInfo);
				}

				JSONObject tagData = stickerData.optJSONObject("tag_data");
				Logger.v(TAG, "setupStickerSearchWizard(), Sticker: " + stickerInfo + ", tag data: " + tagData);

				if ((tagData != null) && (tagData.length() > 0))
				{
					JSONObject tags = tagData.optJSONObject("catgrs");

					ArrayList<String> themeList = new ArrayList<String>();
					ArrayList<String> tagList = new ArrayList<String>();
					ArrayList<String> tagLanguageList = new ArrayList<String>();
					ArrayList<String> tagCategoryList = new ArrayList<String>();
					ArrayList<Integer> tagExactMatchPriorityList = new ArrayList<Integer>();
					ArrayList<Integer> tagPriorityList = new ArrayList<Integer>();
					int stickerMomentCode = HikeStickerSearchBaseConstants.MOMENT_CODE_UNIVERSAL;
					String stickerFestivals = StickerSearchConstants.STRING_EMPTY;

					ArrayList<String> tempExactMatchElements = new ArrayList<String>();
					ArrayList<String> tempRemainingExactMatchElements = new ArrayList<String>();

					if ((tags != null) && (tags.length() > 0))
					{
						Iterator<String> languages = tags.keys();

						while (languages.hasNext())
						{
							String languageId = languages.next();
							if (Utils.isBlank(languageId))
							{
								Logger.e(TAG, "setupStickerSearchWizard(), Invalid language id for sticker: " + stickerInfo);
								continue;
							}

							JSONObject dictionaryData = tags.optJSONObject(languageId);

							if ((dictionaryData != null) && (dictionaryData.length() > 0))
							{
								languageId = languageId.trim().toLowerCase(Locale.ENGLISH);
								Logger.v(TAG, "setupStickerSearchWizard(), Fetching language:" + languageId + " tag data for sticker: " + stickerInfo);

								String key;
								String formattedKey;
								String tag;
								int exactMatchPriority;
								Iterator<String> tagTypeKeys = dictionaryData.keys();

								while (tagTypeKeys.hasNext())
								{
									key = tagTypeKeys.next();

									if ("*ctitle".equalsIgnoreCase(key))
									{
										JSONArray tagArray = dictionaryData.optJSONArray(key);
										Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", exact matching tags: " + tagArray);

										if ((tagArray != null) && (tagArray.length() > 0))
										{
											for (int i = 0; i < tagArray.length(); i++)
											{
												tag = tagArray.optString(i);
												if (!Utils.isBlank(tag))
												{
													tag = tag.trim().toUpperCase(Locale.ENGLISH);
													tempExactMatchElements.add(tag);
													tempRemainingExactMatchElements.add(tag);
												}
											}
										}

										dictionaryData.remove(key);
										break;
									}
								}

								tagTypeKeys = dictionaryData.keys();

								while (tagTypeKeys.hasNext())
								{
									key = tagTypeKeys.next();

									if ("*ctheme".equalsIgnoreCase(key))
									{
										JSONArray tagArray = dictionaryData.optJSONArray(key);
										Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", theme tags: " + tagArray);

										if ((tagArray != null) && (tagArray.length() > 0))
										{
											formattedKey = key.toLowerCase(Locale.ENGLISH).replace("*c", StickerSearchConstants.STRING_EMPTY);

											for (int i = 0; i < tagArray.length(); i++)
											{
												tag = tagArray.optString(i);
												if (!Utils.isBlank(tag))
												{
													tag = tag.trim().toLowerCase(Locale.ENGLISH);
													themeList.add(tag);

													if (!HikeStickerSearchBaseConstants.DEFAULT_THEME_TAG.equalsIgnoreCase(tag))
													{
														tag = tag.trim().toUpperCase(Locale.ENGLISH);
														tagList.add(tag);
														tagLanguageList.add(languageId);
														tagCategoryList.add(formattedKey);
														exactMatchPriority = tempExactMatchElements.indexOf(tag);
														tagExactMatchPriorityList.add(exactMatchPriority);
														if (exactMatchPriority >= 0)
														{
															tempRemainingExactMatchElements.remove(tag);
														}
														tagPriorityList.add(i);
													}
												}
											}
										}

										dictionaryData.remove(key);
										break;
									}
								}

								tagTypeKeys = dictionaryData.keys();

								while (tagTypeKeys.hasNext())
								{
									key = tagTypeKeys.next();
									JSONArray tagArray = dictionaryData.optJSONArray(key);
									Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", '" + key + "' tags: " + tagArray);

									if ((tagArray != null) && (tagArray.length() > 0))
									{
										key = key.toLowerCase(Locale.ENGLISH);
										if (key.startsWith("*c"))
										{
											formattedKey = key.replace("*c", StickerSearchConstants.STRING_EMPTY);

											for (int i = 0; i < tagArray.length(); i++)
											{
												tag = tagArray.optString(i);
												if (!Utils.isBlank(tag))
												{
													tag = tag.trim().toUpperCase(Locale.ENGLISH);
													tagList.add(tag);
													tagLanguageList.add(languageId);
													tagCategoryList.add(formattedKey);
													exactMatchPriority = tempExactMatchElements.indexOf(tag);
													tagExactMatchPriorityList.add(exactMatchPriority);
													if (exactMatchPriority >= 0)
													{
														tempRemainingExactMatchElements.remove(tag);
													}
													tagPriorityList.add(i);
												}
											}
										}
										else
										{
											Logger.w(TAG, "setupStickerSearchWizard(), Unresolved key:" + key + " was found for sticker id: " + stickerInfo);
										}
									}
									else
									{
										Logger.w(TAG, "setupStickerSearchWizard(), Dictionary of '" + key + "' is invalid/ empty for sticker id: " + stickerInfo);
									}
								}

								for (String remainingExactMatchTag : tempRemainingExactMatchElements)
								{
									tagList.add(remainingExactMatchTag);
									tagLanguageList.add(languageId);
									tagCategoryList.add("title");
									exactMatchPriority = tempExactMatchElements.indexOf(remainingExactMatchTag);
									tagExactMatchPriorityList.add(exactMatchPriority);
									tagPriorityList.add(exactMatchPriority);
								}

								tempExactMatchElements.clear();
								tempRemainingExactMatchElements.clear();
							}
							else
							{
								Logger.e(TAG, "setupStickerSearchWizard(), Empty language:" + languageId + " tag data for sticker: " + stickerInfo);
							}
						}

						if (themeList.size() <= 0)
						{
							tagList.clear();
							tagLanguageList.clear();
							tagCategoryList.clear();
							tagExactMatchPriorityList.clear();
							tagPriorityList.clear();

							Logger.e(TAG, "setupStickerSearchWizard(), No valid theme is attached with sticker: " + stickerInfo);
							continue;
						}
					}
					else
					{
						Logger.e(TAG, "setupStickerSearchWizard(), Empty tag data for sticker: " + stickerInfo);
					}

					int stickerTagDataCount = tagList.size();
					if (stickerTagDataCount > 0)
					{
						JSONObject attributeData = tagData.optJSONObject("attrbs");

						if ((attributeData != null) && (attributeData.length() > 0))
						{
							Logger.v(TAG, "setupStickerSearchWizard(), No. of attributes attached with sticker:" + stickerInfo + " = " + attributeData.length());
							Iterator<String> attributeKeys = attributeData.keys();
							String key;

							while (attributeKeys.hasNext())
							{
								key = attributeKeys.next();

								if (key.toLowerCase(Locale.ENGLISH).startsWith("*a"))
								{
									if (key.equalsIgnoreCase("*atime"))
									{
										stickerMomentCode = attributeData.optInt(key, HikeStickerSearchBaseConstants.MOMENT_CODE_UNIVERSAL);
									}
									else if (key.equalsIgnoreCase("*afestival"))
									{
										JSONArray festivalArray = attributeData.optJSONArray(key);
										Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", festivals: " + festivalArray);
										StringBuilder sb = new StringBuilder();

										if (festivalArray != null)
										{
											String festivalString;
											for (int i = 0; i < festivalArray.length(); i++)
											{
												festivalString = festivalArray.optString(i);
												if (!Utils.isBlank(festivalString))
												{
													sb.append(festivalString.trim().toUpperCase(Locale.ENGLISH));
													sb.append(",");
												}
											}

											if (sb.length() > 0)
											{
												sb.setLength(sb.length() - 1);
											}
										}

										stickerFestivals = sb.toString();
									}
								}
								else
								{
									Logger.w(TAG, "setupStickerSearchWizard(), Unresolved key:" + key + " was found for sticker id: " + stickerInfo);
								}
							}
						}
						else
						{
							Logger.e(TAG, "setupStickerSearchWizard(), No attribute is attached with sticker: " + stickerInfo);
						}

						stickersTagData.add(new TagToStcikerDataContainer(stickerInfo, tagList, tagLanguageList, tagCategoryList, themeList, tagExactMatchPriorityList,
								tagPriorityList, stickerMomentCode, stickerFestivals));
						stickerCodeSet.add(stickerInfo);
					}

					packTagDataCount += stickerTagDataCount;
				}
				else
				{
					Logger.w(TAG, "setupStickerSearchWizard(), No tagging is available for sticker: " + stickerInfo);
				}
			}

			if (packTagDataCount > 0)
			{
				JSONArray storyData = packData.optJSONArray("stories");
				if ((storyData != null) && (storyData.length() > 0))
				{
					Logger.v(TAG, "setupStickerSearchWizard(), pack: " + packId + ", stories: " + storyData);
					ArrayList<String> stories = new ArrayList<String>();
					String story;

					for (int i = 0; i < storyData.length(); i++)
					{
						story = storyData.optString(i);
						if (!Utils.isBlank(story))
						{
							stories.add(story.trim().toLowerCase(Locale.ENGLISH));
						}
					}
					packStoryData.put(packId, stories);
				}
				else
				{
					Logger.wtf(TAG, "setupStickerSearchWizard(), No story is attached for pack: " + packId);
					ArrayList<String> stories = new ArrayList<String>();
					stories.add(HikeStickerSearchBaseConstants.DEFAULT_STORY);
					packStoryData.put(packId, stories);
				}
			}
			else
			{
				Logger.w(TAG, "setupStickerSearchWizard(), Invalid/ Empty tagging is attached with stickers of pack: " + packId);
			}
		}

		HashSet<String> untaggedSet = new HashSet<String>();
		untaggedSet.addAll(receivedStickerSet);
		untaggedSet.removeAll(stickerCodeSet);
		stickerCodeSet.clear();
		Logger.i(TAG, "setupStickerSearchWizard(), Current untagged stickers: " + untaggedSet);
		untaggedSet.clear();

		if (stickersTagData.size() > 0)
		{
			synchronized (StickerSearchDataController.class)
			{
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Pack-Story data: " + packStoryData);
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Sticker-Tag data: " + stickersTagData);
				HikeStickerSearchDatabase.getInstance().insertStickerTagData(packStoryData, stickersTagData);
			}
		}

		Set<String> pendingRetrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
		Set<String> updateRetrySet = new HashSet<String>();
		Logger.i(TAG, "setupStickerSearchWizard(), Previous tag fetching trial list: " + pendingRetrySet);

		if (pendingRetrySet != null)
		{
			for (String stickerCode : pendingRetrySet)
			{
				if (!receivedStickerSet.contains(stickerCode))
				{
					updateRetrySet.add(stickerCode);
				}
			}
		}

		Logger.i(TAG, "setupStickerSearchWizard(), Updating tag fetching retry list: " + updateRetrySet);
		HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, updateRetrySet);
		pendingRetrySet.clear();
		updateRetrySet.clear();

		receivedStickerSet.clear();
	}

	public void updateStickerList(Set<String> stickerInfoSet)
	{
		Logger.i(TAG, "updateStickerList(" + stickerInfoSet + ")");

		synchronized (StickerSearchDataController.class)
		{
			HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfoSet);
		}
	}

	public void analyseMessageSent(String prevText, Sticker sticker, String nextText)
	{
		Logger.i(TAG, "analyseMessageSent(" + prevText + ", " + sticker + ", " + nextText + ")");

		HikeStickerSearchDatabase.getInstance().analyseMessageSent(prevText, sticker, nextText);
	}

	public static boolean startRebalancing()
	{
		Logger.i(TAG, "startRebalancing()");

		synchronized (StickerSearchDataController.class)
		{
			return HikeStickerSearchDatabase.getInstance().summarizeAndDoRebalancing();
		}
	}
}