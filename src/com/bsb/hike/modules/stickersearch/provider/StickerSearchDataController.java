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

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.StickerTagDataContainer;
import com.bsb.hike.modules.stickersearch.datamodel.StickerTagDataContainer.StickerTagDataBuilder;
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

		if (Utils.isHoneycombOrHigher())
		{
			synchronized (StickerSearchDataController.class)
			{
				HikeStickerSearchDatabase.getInstance().prepare();
			}
		}
		else
		{
			Logger.d(TAG, "init(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
		}
	}

	public void clear(boolean isNeedToClearAllData)
	{
		Logger.i(TAG, "clear(" + isNeedToClearAllData + ")");

		if (Utils.isHoneycombOrHigher())
		{
			synchronized (StickerSearchDataController.class)
			{
				HikeStickerSearchDatabase.getInstance().deleteDataInTables(isNeedToClearAllData);
			}
		}
		else
		{
			Logger.d(TAG, "clear(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
		}
	}

	public void setupStickerSearchWizard(JSONObject json, int state)
	{
		if (!Utils.isHoneycombOrHigher())
		{
			Logger.d(TAG, "setupStickerSearchWizard(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
			StickerManager.getInstance().removeStickerSet(state);
			return;
		}

		if (!((state == StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT) || (state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH)))
		{
			Logger.e(TAG, "setupStickerSearchWizard(), Invalid state.");
			return;
		}

		JSONObject packsData = json.optJSONObject(HikeConstants.PACKS);
		if ((packsData == null) || (packsData.length() <= 0))
		{
			return;
		}

		Set<String> receivedStickers = new HashSet<String>();
		HashSet<String> stickersWithValidTags = new HashSet<String>();
		Map<String, ArrayList<String>> packStoryData = new HashMap<String, ArrayList<String>>();
		ArrayList<StickerTagDataContainer> stickersTagData = new ArrayList<StickerTagDataContainer>();
		Iterator<String> packs = packsData.keys();

		while (packs.hasNext())
		{
			String packId = packs.next();
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
				receivedStickers.add(stickerInfo);

				JSONObject stickerData = stickersData.optJSONObject(stickerId);

				if ((stickerData == null) || (stickerData.length() <= 0))
				{
					Logger.e(TAG, "setupStickerSearchWizard(), Empty json data for sticker: " + stickerInfo);
					continue;
				}

				JSONObject tagData = stickerData.optJSONObject("tag_data");
				Logger.v(TAG, "setupStickerSearchWizard(), Sticker: " + stickerInfo + ", tag data: " + tagData);

				if ((tagData != null) && (tagData.length() > 0))
				{
					Iterator<String> catgrs = tagData.keys();
					ArrayList<Pair<String, JSONObject>> tagDataWithScript = new ArrayList<Pair<String, JSONObject>>(tagData.length());

					while (catgrs.hasNext())
					{
						String catgrsType = catgrs.next();
						if ("catgrs".equals(catgrsType))
						{
							tagDataWithScript.add(new Pair<String, JSONObject>(HikeStickerSearchBaseConstants.STICKER_TAG_KEYBOARD_ISO_DEFAULT, tagData.optJSONObject("catgrs")));
						}
						else if ("catgrs_loc".equals(catgrsType))
						{
							JSONObject scriptData = tagData.optJSONObject("catgrs_loc");

							if ((scriptData != null) && (scriptData.length() > 0))
							{
								Iterator<String> scripts = scriptData.keys();
								while (scripts.hasNext())
								{
									String script = scripts.next();
									tagDataWithScript.add(new Pair<String, JSONObject>(script, scriptData.optJSONObject(script)));
								}
							}
						}
						else
						{
							Logger.v(TAG, "setupStickerSearchWizard(), Sticker: " + stickerInfo + ", other than categories key:" + catgrsType);
						}
					}

					int scriptCount = tagDataWithScript.size();
					ArrayList<String> themeList = new ArrayList<String>();
					ArrayList<String> tagList = new ArrayList<String>();
					ArrayList<String> tagLanguageList = new ArrayList<String>();
					ArrayList<String> tagScriptList = new ArrayList<String>();
					ArrayList<String> tagCategoryList = new ArrayList<String>();
					ArrayList<Integer> tagExactMatchPriorityList = new ArrayList<Integer>();
					ArrayList<Integer> tagPriorityList = new ArrayList<Integer>();
					int stickerMomentCode = HikeStickerSearchBaseConstants.MOMENT_CODE_UNIVERSAL;
					String stickerFestivals = StickerSearchConstants.STRING_EMPTY;

					for (int scriptIndex = 0; scriptIndex < scriptCount; scriptIndex++)
					{
						String scriptId = tagDataWithScript.get(scriptIndex).first;
						JSONObject tags = tagDataWithScript.get(scriptIndex).second;

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
															tagScriptList.add(scriptId);
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
														tagScriptList.add(scriptId);
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
										tagScriptList.add(scriptId);
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
						}
						else
						{
							Logger.e(TAG, "setupStickerSearchWizard(), Empty tag data for sticker: " + stickerInfo);
						}

						tempExactMatchElements = null;
						tempRemainingExactMatchElements = null;
					}

					// Check themes associated with tags per sticker for each language and script altogether
					if (themeList.size() <= 0)
					{
						themeList = null;
						tagList.clear();
						tagList = null;
						tagLanguageList.clear();
						tagLanguageList = null;
						tagScriptList.clear();
						tagScriptList = null;
						tagCategoryList.clear();
						tagCategoryList = null;
						tagExactMatchPriorityList.clear();
						tagExactMatchPriorityList = null;
						tagPriorityList.clear();
						tagPriorityList = null;

						Logger.e(TAG, "setupStickerSearchWizard(), No valid theme is attached with sticker: " + stickerInfo);
						continue;
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
													sb.append(StickerSearchConstants.STRING_DISSOCIATOR);
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

						stickersTagData.add(new StickerTagDataBuilder(stickerInfo, tagList, themeList, tagLanguageList).tagCategories(tagCategoryList).scripts(tagScriptList)
								.priorities(tagExactMatchPriorityList, tagPriorityList).events(stickerMomentCode, stickerFestivals).build());
						stickersWithValidTags.add(stickerInfo);
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
		untaggedSet.addAll(receivedStickers);
		untaggedSet.removeAll(stickersWithValidTags);
		stickersWithValidTags.clear();
		stickersWithValidTags = null;
		Logger.i(TAG, "setupStickerSearchWizard(), Received untagged stickers: " + untaggedSet);
		untaggedSet.clear();
		untaggedSet = null;

		if (stickersTagData.size() > 0)
		{
			synchronized (StickerSearchDataController.class)
			{
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Pack-Story data: " + packStoryData);
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Sticker-Tag data: " + stickersTagData);

				try
				{
					HikeStickerSearchDatabase.getInstance().insertStickerTagData(packStoryData, stickersTagData);
				}
				catch (Throwable t)
				{
					Logger.e(HikeStickerSearchDatabase.TAG, "Error while inserting tags !!!", t);
				}
			}
		}

		if ((state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH)
				|| (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_TAG_RETRY_ON_FAILED_LOCALLY, StickerSearchConstants.DECISION_STATE_YES) == StickerSearchConstants.DECISION_STATE_YES))
		{
			Set<String> pendingRetrySet = StickerManager.getInstance().getStickerSet(state);

			if (pendingRetrySet != null)
			{
				Set<String> updateRetrySet = new HashSet<String>();

				for (String stickerCode : pendingRetrySet)
				{
					if (!receivedStickers.contains(stickerCode))
					{
						updateRetrySet.add(stickerCode);
					}
				}

				Logger.i(TAG, "setupStickerSearchWizard(), Updating tag fetching retry list: " + updateRetrySet);
				if (updateRetrySet.size() > 0)
				{
					StickerManager.getInstance().saveStickerSet(updateRetrySet, state);
				}
				else
				{
					StickerManager.getInstance().removeStickerSet(state);
					
					if(state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_SUCCESSFUL_STICKER_TAG_REFRESH_TIME, System.currentTimeMillis());
					}
				}
				pendingRetrySet.clear();
				pendingRetrySet = null;
				updateRetrySet.clear();
				updateRetrySet = null;
			}
		}

		receivedStickers.clear();
		receivedStickers = null;
	}

	public void updateStickerList(Set<String> stickerInfoSet)
	{
		Logger.i(TAG, "updateStickerList(" + stickerInfoSet + ")");

		if (Utils.isHoneycombOrHigher())
		{
			synchronized (StickerSearchDataController.class)
			{
				HikeStickerSearchDatabase.getInstance().disableTagsForDeletedStickers(stickerInfoSet);
			}
		}
		else
		{
			Logger.d(TAG, "updateStickerList(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
		}
	}

	public void analyseMessageSent(String prevText, Sticker sticker, String nextText)
	{
		Logger.i(TAG, "analyseMessageSent(" + prevText + ", " + sticker + ", " + nextText + ")");

		if (Utils.isHoneycombOrHigher())
		{
			HikeStickerSearchDatabase.getInstance().analyseMessageSent(prevText, sticker, nextText);
		}
		else
		{
			Logger.d(TAG, "analyseMessageSent(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
		}
	}

	public static boolean startRebalancing()
	{
		Logger.i(TAG, "startRebalancing()");

		if (Utils.isHoneycombOrHigher())
		{
			synchronized (StickerSearchDataController.class)
			{
				try
				{
					return HikeStickerSearchDatabase.getInstance().summarizeAndDoRebalancing();
				}
				catch (Throwable t)
				{
					Logger.wtf(HikeStickerSearchDatabase.TAG_REBALANCING, "Error while performing summarization and other updates !!!", t);
					return true;
				}
			}
		}
		else
		{
			Logger.d(TAG, "startRebalancing(), Sticker Recommendation is not supported in Android OS v 2.3.x or lower.");
			return false;
		}
	}
}