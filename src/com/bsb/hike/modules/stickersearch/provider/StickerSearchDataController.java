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

	public void setupStickerSearchWizard(JSONObject json, int state)
	{
		Logger.i(TAG, "setupStickerSearchWizard(" + json + ", " + state + ")");

		if (!((state == StickerSearchConstants.STICKER_DATA_FIRST_SETUP) || (state == StickerSearchConstants.STICKER_DATA_UPDATE_TRIAL)))
		{
			Logger.e(TAG, "setupStickerSearchWizard(), Invalid trial request.");
			return;
		}

		synchronized (StickerSearchDataController.class)
		{
			JSONObject packsData = json.optJSONObject(HikeConstants.PACKS);
			if ((packsData == null) || (packsData.length() <= 0))
			{
				return;
			}

			Iterator<String> categories = packsData.keys();
			HashSet<String> stickerCodeList = new HashSet<String>();
			Map<String, ArrayList<String>> packStoryData = new HashMap<String, ArrayList<String>>();
			Map<String, ArrayList<Object>> stickerTagData = new HashMap<String, ArrayList<Object>>();
			Set<String> untaggedSet = new HashSet<String>();

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
					Logger.e(TAG, "setupStickerSearchWizard(), Empty pack data: " + packId);
					continue;
				}

				JSONObject stickersData = packData.optJSONObject(HikeConstants.STICKERS);
				if ((stickersData == null) || (stickersData.length() <= 0))
				{
					Logger.e(TAG, "setupStickerSearchWizard(), No sticker for pack: " + packId);
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

					String stickerInfo = StickerManager.getInstance().getStickerSetString(stickerId.trim(), packId.trim());
					JSONObject stickerData = stickersData.optJSONObject(stickerId);

					if ((stickerData == null) || (stickerData.length() <= 0))
					{
						Logger.e(TAG, "setupStickerSearchWizard(), Empty sticker data: " + stickerInfo);
						continue;
					}

					if (state == StickerSearchConstants.STICKER_DATA_FIRST_SETUP)
					{
						if (TextUtils.isEmpty(stickerData.optString(HikeConstants.IMAGE)))
						{
							Logger.e(TAG, "setupStickerSearchWizard(), Empty sticker image data: " + stickerInfo);
							continue;
						}
					}
					else if (state == StickerSearchConstants.STICKER_DATA_UPDATE_TRIAL)
					{
						Logger.v(TAG, "setupStickerSearchWizard(), No dependency on sticker image data: " + stickerInfo);
					}

					JSONObject tagData = stickerData.optJSONObject("tag_data");

					if ((tagData != null) && (tagData.length() > 0))
					{
						JSONObject tags = tagData.optJSONObject("catgrs");

						ArrayList<String> themeList = new ArrayList<String>();
						ArrayList<String> tagList = new ArrayList<String>();
						ArrayList<String> tagLanguageList = new ArrayList<String>();
						ArrayList<String> tagCategoryList = new ArrayList<String>();
						ArrayList<Integer> tagExactMatchPriorityList = new ArrayList<Integer>();
						ArrayList<Integer> tagPriorityList = new ArrayList<Integer>();
						ArrayList<Integer> stickerMomentList = new ArrayList<Integer>();
						ArrayList<String> stickerFestivalList = new ArrayList<String>();

						ArrayList<String> tempMatchElements = new ArrayList<String>();
						ArrayList<String> tempRemainingMatchElements = new ArrayList<String>();

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
									Logger.e(TAG, "setupStickerSearchWizard(), Fetching tag data of sticker: " + stickerInfo + ", language: " + languageId);

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
														tempMatchElements.add(tag);
														tempRemainingMatchElements.add(tag);
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
												formattedKey = key.toLowerCase(Locale.ENGLISH).replace("*c", "");

												for (int i = 0; i < tagArray.length(); i++)
												{
													tag = tagArray.optString(i);
													if (!Utils.isBlank(tag))
													{
														tag = tag.trim().toUpperCase(Locale.ENGLISH);
														themeList.add(tag);

														if (!HikeStickerSearchBaseConstants.THEME_DEFAULT.equals(tag))
														{
															tagList.add(tag);
															tagLanguageList.add(languageId);
															tagCategoryList.add(formattedKey);
															exactMatchPriority = tempMatchElements.indexOf(tag);
															tagExactMatchPriorityList.add(exactMatchPriority);
															if (exactMatchPriority >= 0)
															{
																tempRemainingMatchElements.remove(tag);
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
												formattedKey = key.replace("*c", "");

												for (int i = 0; i < tagArray.length(); i++)
												{
													tag = tagArray.optString(i);
													if (!Utils.isBlank(tag))
													{
														tag = tag.trim().toUpperCase(Locale.ENGLISH);
														tagList.add(tag);
														tagLanguageList.add(languageId);
														tagCategoryList.add(formattedKey);
														exactMatchPriority = tempMatchElements.indexOf(tag);
														tagExactMatchPriorityList.add(exactMatchPriority);
														if (exactMatchPriority >= 0)
														{
															tempRemainingMatchElements.remove(tag);
														}
														tagPriorityList.add(i);
													}
												}
											}
											else
											{
												Logger.w(TAG, "setupStickerSearchWizard(), Unresolved key: " + key + " was found for sticker id: " + stickerInfo);
											}
										}
										else
										{
											Logger.w(TAG, "setupStickerSearchWizard(), Dictionary of '" + key + "' is invalid/ empty for sticker id: " + stickerInfo);
										}
									}

									for (String remainingTag : tempRemainingMatchElements)
									{
										tagList.add(remainingTag);
										tagLanguageList.add(languageId);
										tagCategoryList.add("title");
										exactMatchPriority = tempMatchElements.indexOf(remainingTag);
										tagExactMatchPriorityList.add(exactMatchPriority);
										tagPriorityList.add(exactMatchPriority);
									}

									tempMatchElements.clear();
									tempRemainingMatchElements.clear();
								}
								else
								{
									Logger.e(TAG, "setupStickerSearchWizard(), Empty sticker tag data: " + stickerInfo + ", language: " + languageId);
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
							Logger.e(TAG, "setupStickerSearchWizard(), Empty sticker tag data: " + stickerInfo);
						}

						int stickerTagDataCount = tagList.size();
						if (stickerTagDataCount > 0)
						{
							boolean momentDataEntered = false;
							boolean festivalDataEntered = false;
							JSONObject attributeData = tagData.optJSONObject("attrbs");

							if ((attributeData != null) && (attributeData.length() > 0))
							{
								Logger.v(TAG, "setupStickerSearchWizard(), No. of attributes attached with " + stickerInfo + " = " + attributeData.length());
								Iterator<String> attributeKeys = attributeData.keys();
								String key;

								while (attributeKeys.hasNext())
								{
									key = attributeKeys.next();
									if (key.equalsIgnoreCase("*atime"))
									{
										int momentCode = attributeData.optInt(key, HikeStickerSearchBaseConstants.MOMENT_DEFAULT);

										for (int i = 0; i < stickerTagDataCount; i++)
										{
											stickerMomentList.add(momentCode);
										}

										momentDataEntered = true;
									}
									else if (key.equalsIgnoreCase("*afestival"))
									{
										JSONArray festivalArray = attributeData.optJSONArray(key);
										Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", festivals: " + festivalArray);
										StringBuilder sb = new StringBuilder();
										String festivalString;

										if (festivalArray != null)
										{
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

										festivalString = sb.toString();

										for (int i = 0; i < stickerTagDataCount; i++)
										{
											stickerFestivalList.add(festivalString);
										}

										festivalDataEntered = true;
									}
								}
							}
							else
							{
								Logger.e(TAG, "setupStickerSearchWizard(), No attribute attached with sticker: " + stickerInfo);
							}

							if (!momentDataEntered)
							{
								for (int i = 0; i < stickerTagDataCount; i++)
								{
									stickerMomentList.add(HikeStickerSearchBaseConstants.MOMENT_DEFAULT);
								}
							}

							if (!festivalDataEntered)
							{
								for (int i = 0; i < stickerTagDataCount; i++)
								{
									stickerFestivalList.add("");
								}
							}

							// build tag data
							stickerTagData.put(
									stickerInfo,
									buildStickerTagData(tagList, tagLanguageList, tagCategoryList, themeList, tagExactMatchPriorityList, tagPriorityList, stickerMomentList,
											stickerFestivalList));
							stickerCodeList.add(stickerInfo);
						}
						else
						{
							untaggedSet.add(stickerInfo);
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
						Logger.w(TAG, "setupStickerSearchWizard(), No story is attached for pack: " + packId);
						ArrayList<String> stories = new ArrayList<String>();
						stories.add(HikeStickerSearchBaseConstants.STORY_DEFAULT);
						packStoryData.put(packId, stories);
					}
				}
				else
				{
					Logger.e(TAG, "setupStickerSearchWizard(), Invalid/ Empty tagging is attached with stickers of pack: " + packId);
				}
			}

			Logger.v(TAG, "setupStickerSearchWizard(), Pack-Story data: " + packStoryData);
			Logger.v(TAG, "setupStickerSearchWizard(), Sticker-Tag data: " + stickerTagData);
			HikeStickerSearchDatabase.getInstance().insertStickerTagData(packStoryData, stickerTagData);

			Logger.i(TAG, "setupStickerSearchWizard(), Current untagged stickers: " + untaggedSet);
			if (state == StickerSearchConstants.STICKER_DATA_UPDATE_TRIAL)
			{
				Set<String> pendingRetrySet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
				Set<String> updateRetrySet = new HashSet<String>();
				Logger.i(TAG, "setupStickerSearchWizard(), Previous tag fetching trail list: " + pendingRetrySet);

				if (pendingRetrySet != null)
				{
					for (String stickerCode : pendingRetrySet)
					{
						if (!stickerCodeList.contains(stickerCode) && !untaggedSet.contains(stickerCode))
						{
							updateRetrySet.add(stickerCode);
						}
					}
				}

				Logger.i(TAG, "setupStickerSearchWizard(), Updating tag fetching retry list: " + updateRetrySet);
				HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, updateRetrySet);
			}
		}
	}

	public void updateStickerList(Set<String> stickerInfoSet)
	{
		Logger.i(TAG, "updateStickerList(" + stickerInfoSet + ")");

		synchronized (StickerSearchDataController.class)
		{
			HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfoSet);
		}
	}

	private ArrayList<Object> buildStickerTagData(ArrayList<String> tags, ArrayList<String> languages, ArrayList<String> tagCategories, ArrayList<String> themes,
			ArrayList<Integer> tagExactMatchPriorities, ArrayList<Integer> tagPriorities, ArrayList<Integer> moments, ArrayList<String> festivals)
	{
		ArrayList<Object> data = new ArrayList<Object>(HikeStickerSearchBaseConstants.INDEX_TAG_DATA_COUNT);

		// Do not change the order of insertion as per indices defined as followed
		// INDEX_TAG_DATA_TAG_PHRASE = 0;
		// INDEX_TAG_DATA_PHRASE_LANGUAGE = 1;
		// INDEX_TAG_DATA_TAG_STATE_CATEGORY = 2;
		// INDEX_TAG_DATA_CHAT_THEME = 3;
		// INDEX_TAG_DATA_EXACTNESS_PRIORITY = 4;
		// INDEX_TAG_DATA_MOMENT_CODE = 5;
		// INDEX_TAG_DATA_FESTIVALS = 6;
		// INDEX_TAG_DATA_PRIORITY = 7;
		// INDEX_TAG_DATA_COUNT = 8;

		data.add(tags);
		data.add(languages);
		data.add(tagCategories);
		data.add(themes);
		data.add(tagExactMatchPriorities);
		data.add(moments);
		data.add(festivals);
		data.add(tagPriorities);

		return data;
	}

	public void analyseMessageSent(String prevText, Sticker sticker, String nextText)
	{
		Logger.i(TAG, "analyseMessageSet(" + prevText + ", " + sticker + ", " + nextText + ")");

		HikeStickerSearchDatabase.getInstance().analyseMessageSent(prevText, sticker, nextText);
	}

	public static boolean startRebalancing()
	{
		Logger.i(TAG, "startRebalancing()");

		return HikeStickerSearchDatabase.getInstance().startRebalancing();
	}
}