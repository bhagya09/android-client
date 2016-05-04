/**
 * File   : StickerSearchDataController.java
 * Content: It provides intermediate gateway to functions of Sticker_Search_Database.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.StickerEventDataContainer;
import com.bsb.hike.modules.stickersearch.datamodel.StickerTagDataContainer;
import com.bsb.hike.modules.stickersearch.datamodel.StickerTagDataContainer.StickerTagDataBuilder;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
			HikeStickerSearchDatabase.getInstance().deleteDataInTables(isNeedToClearAllData);
		}
	}

	public void setupStickerSearchWizard(JSONObject json, int state)
	{
		JSONObject packsData = json.optJSONObject(HikeConstants.PACKS);

		Set<String> receivedStickers = new HashSet<String>();
		Set<String> stickersWithValidTags = new HashSet<String>();
		Map<String, List<String>> packStoryData = new HashMap<String, List<String>>();
		Set<StickerEventDataContainer> eventsData = new HashSet<StickerEventDataContainer>();
		List<StickerTagDataContainer> stickersTagData = new ArrayList<StickerTagDataContainer>();
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
                Sticker sticker = new Sticker(packId,stickerId);
				if (Utils.isBlank(stickerId))
				{
					Logger.e(TAG, "setupStickerSearchWizard(), Invalid sticker id inside pack: " + packId);
					continue;
				}

				String stickerInfo = sticker.getStickerCode();
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
							tagDataWithScript
									.add(new Pair<String, JSONObject>(HikeStickerSearchBaseConstants.DEFAULT_STICKER_TAG_SCRIPT_ISO_CODE, tagData.optJSONObject("catgrs")));
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
					ArrayList<StickerEventDataContainer> stickerEvents = null;

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
													tag = tagArray.optString(i).trim().toUpperCase(Locale.ENGLISH);

													// Do not add exact tag of any language in list, if it is already added for same language due to duplicates in json response
													if (!Utils.isBlank(tag) && !tempExactMatchElements.contains(tag))
													{
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
													tag = tagArray.optString(i).trim().toLowerCase(Locale.ENGLISH);

													// Do not add theme of any language in list, if it is already added for same language due to duplicates in json response
													if (!Utils.isBlank(tag) && !themeList.contains(tag))
													{
														themeList.add(tag);

														if (!HikeStickerSearchBaseConstants.DEFAULT_THEME_TAG.equalsIgnoreCase(tag))
														{
															tag = tag.toUpperCase(Locale.ENGLISH);
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
													tag = tagArray.optString(i).trim().toUpperCase(Locale.ENGLISH);
													if (!Utils.isBlank(tag))
													{
														// Do not add tag of any language in list, if it is already added for same language due to duplicates in json response
														int indexOfPreExistingTagInList = tagList.indexOf(tag);
														if ((indexOfPreExistingTagInList < 0) || !languageId.equals(tagLanguageList.get(indexOfPreExistingTagInList)))
														{
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

					// Check themes associated with tags per sticker for each language and script altogether, if tags are received
					//if ((themeList.size() < 0) && (tagList.size() > 0))
					//temp hack since theme list is not being used currently and also server does not send theme list for tags in regional scripts
					if ((themeList.size() < 0) && (tagList.size() > 0))
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
									JSONObject festiveData = attributeData.optJSONObject(key);
									Logger.v(TAG, "setupStickerSearchWizard(), sticker id: " + stickerInfo + ", events: " + festiveData);

									if (festiveData != null)
									{
										if (festiveData.length() > 0)
										{
											Iterator<String> events = festiveData.keys();
											stickerEvents = new ArrayList<StickerEventDataContainer>(festiveData.length());

											while (events.hasNext())
											{
												String event = events.next();
												JSONObject eventData = festiveData.optJSONObject(event);
												if (eventData != null)
												{
													StickerEventDataContainer stickerEvent = new StickerEventDataContainer(event, eventData);
													stickerEvents.add(stickerEvent);
													eventsData.add(stickerEvent);
												}
											}
										}
										/* If some sticker converts from non-festive to festive, then server has to send *afestival key-data but with empty list */
										else
										{
											stickerEvents = new ArrayList<StickerEventDataContainer>(0);
										}
									}
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
							.priorities(tagExactMatchPriorityList, tagPriorityList).events(stickerMomentCode, stickerEvents).build());
					stickersWithValidTags.add(stickerInfo);

					packTagDataCount += tagList.size();
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
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Pack-Story data for packs: " + packStoryData.keySet());
				Logger.v(TAG, "setupStickerSearchWizard(), Ready to insert Sticker-Tag data for stickers (count): " + stickersTagData.size());

				try
				{
					HikeStickerSearchDatabase.getInstance().insertStickerTagData(packStoryData, eventsData, stickersTagData);
				}
				catch (Throwable t)
				{
					Logger.e(HikeStickerSearchDatabase.TAG, "Error while inserting tags !!!", t);
				}
			}
		}

		if ((state == StickerSearchConstants.STATE_STICKER_DATA_REFRESH)
				|| (HikeSharedPreferenceUtil.getInstance(HikeStickerSearchBaseConstants.SHARED_PREF_STICKER_DATA).getData(HikeConstants.STICKER_TAG_RETRY_ON_FAILED_LOCALLY,
						StickerSearchConstants.DECISION_STATE_YES) == StickerSearchConstants.DECISION_STATE_YES))
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

				int remainingSetSize = updateRetrySet.size();

				// Turn on sticker recommendation, if number of stickers left in queue is less than threshold value
				if (remainingSetSize < StickerSearchConstants.THRESHOLD_NUM_STICKERS)
				{
					Logger.d(TAG, "setupStickerSearchWizard(), Turnig recommendation on after threshold is crossed.");
					StickerManager.getInstance().toggleStickerRecommendation(true);
				}

				Logger.i(TAG, "setupStickerSearchWizard(), Updating tag fetching retry list: " + updateRetrySet);
				if (remainingSetSize > 0)
				{
					StickerManager.getInstance().saveStickerSet(updateRetrySet, state, true);
				}
				else
				{
					StickerManager.getInstance().removeStickerSet(state);
					takeDecisionOnState(state);
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

	private void takeDecisionOnState(int state)
	{
		switch (state)
		{
		case StickerSearchConstants.STATE_STICKER_DATA_REFRESH:
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_SUCCESSFUL_STICKER_TAG_REFRESH_TIME, System.currentTimeMillis());
			break;
		case StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD:
			StickerLanguagesManager.getInstance().downloadTagsForNextLanguage();
			break;
		}
	}

	public void updateStickerList(Set<String> infoSet, int type)
	{
		Logger.i(TAG, "updateStickerList(" + infoSet + ")");

		synchronized (StickerSearchDataController.class)
		{
			switch (type)
			{
				case StickerSearchConstants.REMOVAL_BY_CATEGORY_DELETED: // Set of categories deleted
					HikeStickerSearchDatabase.getInstance().removeTagsForDeletedCategories(infoSet);
					break;

				case StickerSearchConstants.REMOVAL_BY_STICKER_DELETED: // Set of stickers deleted
					HikeStickerSearchDatabase.getInstance().removeTagsForDeletedStickers(infoSet);
					break;

				case StickerSearchConstants.REMOVAL_BY_EXCLUSION_IN_EXISTING_STCIKERS: // Set of current existing stickers
					HikeStickerSearchDatabase.getInstance().removeTagsForNonExistingStickers(infoSet);
					break;

				default:
					Logger.e(TAG, "updateStickerList(), Unknown request type.");
			}
		}
	}

	public void analyseMessageSent(String prevText, Sticker sticker, String nextText)
	{
		Logger.i(TAG, "analyseMessageSent(" + prevText + ", " + sticker + ", " + nextText + ")");
		synchronized (StickerSearchDataController.class)
		{
			try
			{
				HikeStickerSearchDatabase.getInstance().analyseMessageSent(prevText, sticker, nextText);
			}
			catch (Throwable t)
			{
				Logger.wtf(HikeStickerSearchDatabase.TAG, "Error while updating sent message-sticker history!!!", t);
			}
		}
	}

	public void loadStickerEvents()
	{
		Logger.i(TAG, "loadStickerEvents()");
		synchronized (StickerSearchDataController.class)
		{
			// Update events for the day, only if required to do so
			StickerEventSearchManager.getInstance().loadNowCastEvents();
		}
	}

	public static boolean startRebalancing()
	{
		Logger.i(TAG, "startRebalancing()");
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
}