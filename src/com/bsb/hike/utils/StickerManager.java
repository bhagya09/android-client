package com.bsb.hike.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.quickstickersuggestions.tasks.FetchForAllStickerQuickSuggestionTask;
import com.bsb.hike.modules.stickerdownloadmgr.FetchCategoryRanksTask;
import com.bsb.hike.modules.stickerdownloadmgr.DefaultTagDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.FetchCategoryMetadataTask;
import com.bsb.hike.modules.stickerdownloadmgr.FetchCategoryTagDataTask;
import com.bsb.hike.modules.stickerdownloadmgr.FetchShopPackDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.MultiStickerDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.MultiStickerImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.MultiStickerQuickSuggestionDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.ParameterMappingDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerQuickSuggestionDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerTagDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerCategoryDataUpdateTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerCategoryDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPreviewImageDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerSignupUpgradeDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.UserParameterDownloadTask;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.utils.Utils.ExternalStorageState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bsb.hike.backup.BackupUtils;
import com.bsb.hike.backup.model.BackupMetadata;

public class StickerManager
{
	private static final String TAG = StickerManager.class.getSimpleName();

	public static final String STICKERS_MOVED_EXTERNAL_TO_INTERNAL = "movedStickersExtToInt";

	public static final String RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED = "recentStickerSerializationCorrected";

	public static final String RESET_REACHED_END_FOR_DEFAULT_STICKERS = "resetReachedEndForDefaultStickers";

	public static final String CORRECT_DEFAULT_STICKER_DIALOG_PREFERENCES = "correctDefaultStickerDialogPreferences";

	public static final String SHOWN_STICKERS_TUTORIAL = "shownStickersTutorial";

	public static final String STICKERS_DOWNLOADED = "st_downloaded";

	public static final String MORE_STICKERS_DOWNLOADED = "st_more_downloaded";

	public static final String STICKERS_FAILED = "st_failed";

	public static final String STICKERS_PROGRESS = "st_progress";

	public static final String STICKER_DOWNLOAD_TYPE = "stDownloadType";

	public static final String STICKER_DATA_BUNDLE = "stickerDataBundle";

	public static final String STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE = "stickerDownloadFailedTooLarge";

	public static final String STICKER_CATEGORY = "stickerCategory";

	public static final String RECENT_STICKER_SENT = "recentStickerSent";

	public static final String RECENTS_UPDATED = "recentsUpdated";

	public static final String STICKER_ID = "stId";

	public static final String CATEGORY_ID = "catId";

	public static final String FWD_STICKER_ID = "fwdStickerId";

	public static final String FWD_CATEGORY_ID = "fwdCategoryId";

	public static final String STICKERS_UPDATED = "stickersUpdated";

	public static final String STICKER_PREVIEW_DOWNLOADED = "stickerPreviewDownloaded";

	public static final String QUICK_STICKER_SUGGESTION_FETCH_SUCCESS = "quickStickerSuggestionFetchSuccess";

	public static final String QUICK_STICKER_SUGGESTION_FETCH_FAILED = "quickStickerSuggestionFetchFailed";

	public static final String QUICK_STICKER_SUGGESTION_FTUE_STICKER_CLICKED = "quickStickerSuggestionFtueStickerClicked";

	public static final String ADD_NO_MEDIA_FILE_FOR_STICKERS = "addNoMediaFileForStickers";

	public static final String ADD_NO_MEDIA_FILE_FOR_STICKER_OTHER_FOLDERS = "addNoMediaFileForStickerOtherFolders";

	public static final String DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER = "delDefaultDownloadedExpressionsStickers";

	public static final String UPGRADE_STICKER_CATEGORIES_TABLE = "updateStickerCategoriesTable";

	public static final String HARCODED_STICKERS = "harcodedStickers";

	public static final String STICKER_IDS = "stickerIds";

	public static final String CATEGORY_IDS = "catIds";

	public static final String RESOURCE_IDS = "resourceIds";

	public static final String MOVED_HARDCODED_STICKERS_TO_SDCARD = "movedHardCodedStickersToSdcard";

	public static int RECENT_STICKERS_COUNT = 30;

	public static int MAX_CUSTOM_STICKERS_COUNT = 30;

	public static final int SIZE_IMAGE = (int) (80 * Utils.densityMultiplier);

	public static final int PREVIEW_IMAGE_SIZE = (int) (58 * Utils.scaledDensityMultiplier);

	public static final String UPGRADE_FOR_STICKER_SHOP_VERSION_1 = "upgradeForStickerShopVersion1";

	public static final String STICKERS_JSON_FILE_NAME = "stickers_data";

	public static final String STICKER_CATEGORIES = "stickerCategories";

	public static final String CATEGORY_NAME = "categoryName";

	public static final String IS_VISIBLE = "isVisible";

	public static final String IS_CUSTOM = "isCustom";

	public static final String IS_ADDED = "isAdded";

	public static final String CATEGORY_INDEX = "catIndex";

	public static final String METADATA = "metadata";

	public static final String TIMESTAMP = "timestamp";

	public static final String TOTAL_STICKERS = "totalStickers";

	public static final String DOWNLOAD_PREF = "downloadPref";

	public static final String RECENT = "recent";

	public static final String DOGGY_CATEGORY = "doggy";

	public static final String EXPRESSIONS = "expressions";

	public static final String HUMANOID = "humanoid";

	public static final String LOVE = "love";

	public static final String QUICK_SUGGESTIONS = "quick_suggestions";

	public static final String OTHER_STICKER_ASSET_ROOT = "/other";

	public static final String PALLATE_ICON = "pallate_icon";

	public static final String PALLATE_ICON_SELECTED = "pallate_icon_selected";

	public static final String PREVIEW_IMAGE = "preview";

	public static final int PALLATE_ICON_TYPE = 0;

	public static final int PALLATE_ICON_SELECTED_TYPE = 1;

	public static final int PREVIEW_IMAGE_SHOP_TYPE = 2;

	public static final int PREVIEW_IMAGE_PALETTE_TYPE = 3;

	public static final int PREVIEW_IMAGE_EMPTY_PALETTE_TYPE = 4;

	public static final int PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE = 5;

	public static final int PREVIEW_IMAGE_PACK_PREVIEW_PALETTE_TYPE =6;

	public static final String OTHER_ICON_TYPE = ".png";

	public static final String CATEGORY_SIZE = "categorySize";

	public static final String STICKERS_SIZE_DOWNLOADED = "stickersSizeDownloaded";

	public static final String PERCENTAGE = "percentage";

	public static final String STICKER_SHOP_DATA_FULLY_FETCHED = "stickerShopDataFullyFetched";

	public static final String STICKER_SHOP_RANK_FULLY_FETCHED = "stickerShopRankFullyFetched";

	public static final String SEND_SOURCE = "source";

	public static final String FROM_RECENT = "r";

	public static final String FROM_FORWARD = "f";

	public static final String FROM_AUTO_RECOMMENDATION_PANEL = "ar";

	public static final String FROM_BLUE_TAP_RECOMMENDATION_PANEL = "br";

	public static final String FROM_STICKER_RECOMMENDATION_FTUE = "ft";

	public static final String FROM_OTHER = "o";

	public static final String FROM_QR = "qr";

	public static final String FROM_QF = "qf";

	public static final String REJECT_FROM_CROSS = "crs";

	public static final String REJECT_FROM_IGNORE = "ign";

	public static final String FROM_CHAT_SETTINGS = "cs";

	public static final long MINIMUM_FREE_SPACE = 10 * 1024 * 1024;

	public static final String SHOW_STICKER_SHOP_BADGE = "showStickerShopBadge";

	public static final String STICKER_RES_ID = "stickerResId";

	private static final String REMOVE_LEGACY_GREEN_DOTS = "removeLegacyGreenDots";

	public static final String STICKER_ERROR_LOG = "stkELog";

	public static final int DEFAULT_POSITION = 3;

	public static final String STICKER_FOLDER_NAMES_UPGRADE_DONE = "upgradeForStickerFolderNames";

	public static final String STICKER_MESSAGE_TAG = "Sticker";

	public static final String STRING_NO_MEDIA = ".nomedia";

	public static final String STRING_EMPTY = "";

	public static final String STRING_DELIMETER = ":";

	public static final int INDEX_CATEGORY_ID = 0;

	public static final int INDEX_STICKER_ID = 1;

	public static final int INDEX_INFO_BOUND = 2;

	public static final int SHOP_PAGE_SIZE = 100;

	public static final String STICKER_TYPE = "s_t";

	private final Map<String, StickerCategory> stickerCategoriesMap;

	public static String stickerExternalDir;

	public FilenameFilter stickerFileFilter = new FilenameFilter()
	{
		@Override
		public boolean accept(File file, String fileName)
		{
			return !STRING_NO_MEDIA.equalsIgnoreCase(fileName);
		}
	};

	private Context context;

	private boolean showLastCategory = false;

	private static volatile StickerManager instance;

	public static StickerManager getInstance()
	{
		if (instance == null)
		{
			synchronized (StickerManager.class)
			{
				if (instance == null)
					instance = new StickerManager();
			}
		}
		return instance;
	}

	private StickerManager()
	{
		stickerCategoriesMap = Collections.synchronizedMap(new LinkedHashMap<String, StickerCategory>());
		context = HikeMessengerApp.getInstance();
		stickerExternalDir = getStickerExternalDirFilePath();
		logStickerFolderError();
	}

	/**
	 * DO NOT USE.
     */
	public String getOldStickerExternalDirFilePath()
	{
		String externalDir = Utils.getExternalFilesDirPath(null);
		String stickerExternalDir = (externalDir == null ? null : externalDir + "/stickers"); // hard-code path to remove dependency on constant
		return stickerExternalDir;
	}

	public String getStickerExternalDirFilePath()
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.BackupRestore.KEY_MOVED_STICKER_EXTERNAL, false))
		{
			String externalDir = getOldStickerExternalDirFilePath();
			return externalDir;
		}

		return getNewStickerDirFilePath();
	}

	public String getNewStickerDirFilePath()
	{
		String stickerExternalDir = HikeConstants.HIKE_DIRECTORY_ROOT + HikeConstants.STICKERS_ROOT;
		return stickerExternalDir;
	}

	public void doInitialSetup()
	{
		// move stickers from external to internal if not done
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (!settings.getBoolean(StickerManager.RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED, false))
		{
			updateRecentStickerFile(settings);
		}

		if (!settings.getBoolean(StickerManager.STICKER_FOLDER_NAMES_UPGRADE_DONE, false))
		{
			updateStickerFolderNames();
			settings.edit().putBoolean(StickerManager.STICKER_FOLDER_NAMES_UPGRADE_DONE, true).commit();
		}

		if (!HikeSharedPreferenceUtil.getInstance().getData(StickerManager.UPGRADE_STICKER_CATEGORIES_TABLE, false))
		{
			resetSignupUpgradeCallPreference();
			HikeConversationsDatabase.getInstance().markAllCategoriesAsDownloaded();
			HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.UPGRADE_STICKER_CATEGORIES_TABLE, true);
		}

		setupStickerCategoryList();

		if (!settings.getBoolean(StickerManager.ADD_NO_MEDIA_FILE_FOR_STICKERS, false))
		{
			addNoMediaFilesToStickerDirectories();
		}

		if (!settings.getBoolean(StickerManager.ADD_NO_MEDIA_FILE_FOR_STICKER_OTHER_FOLDERS, false))
		{
			addNoMediaFilesToStickerDirectories();
		}

		/*
		 * this code path will be for users upgrading to the build where we make expressions a default loaded category
		 */
		if (!settings.getBoolean(StickerManager.DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER, false))
		{
			settings.edit().putBoolean(StickerManager.DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER, true).commit();

			if (checkIfStickerCategoryExists(DOGGY_CATEGORY))
			{
				StickerManager.getInstance().setStickerUpdateAvailable(DOGGY_CATEGORY, true);
			}
		}

		/**
		 * This code path is used for removing green dot bug, in which even though there are no stickers to download, the green dot persists.
		 * 
		 * TODO : Remove this code flow after 3-4 release cycles.
		 */

		if (!settings.getBoolean(StickerManager.REMOVE_LEGACY_GREEN_DOTS, false))
		{
			removeLegacyGreenDots();
			settings.edit().putBoolean(StickerManager.REMOVE_LEGACY_GREEN_DOTS, true).commit();
		}

		cachingStickersOnStart();

        retryInsertForStickers();

		doUpgradeTasks();

		initiateFetchCategoryRanksAndDataTask();

		QuickStickerSuggestionController.getInstance().retryFailedQuickSuggestions();

		makeCallForUserParameters();
}

	public void fetchCategoryMetadataTask(List<StickerCategory> list)
	{
		if (!Utils.isEmpty(list))
		{
			FetchCategoryMetadataTask fetchCategoryMetadataTask = new FetchCategoryMetadataTask(list);
			fetchCategoryMetadataTask.execute();
		}
	}

	public void initiateFetchCategoryRanksAndDataTask()
	{
		initiateFetchCategoryRanksAndDataTask(0, false);
	}

	public void initiateFetchCategoryRanksAndDataTask(final int offset, final boolean fetchRankForced)
	{
		if(!Utils.isUserSignedUp(HikeMessengerApp.getInstance(), false) || Utils.isUpgradeIntentServiceRunning())
		{
			Logger.d(TAG, "user not signed up or user is upgrading");
			return;
		}
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				boolean isMoreThanDay = (System.currentTimeMillis()
						- HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATE_SHOP_RANK_TIMESTAMP, 0L)) > HikeConstants.ONE_DAY_MILLS;
				if (isMoreThanDay || (fetchRankForced && !HikeSharedPreferenceUtil.getInstance().getData(StickerManager.STICKER_SHOP_RANK_FULLY_FETCHED, false)))
				{
					executeFetchCategoryRankTask(isMoreThanDay, offset);
				}
				else
				{
					refreshPacksData();
				}
			}
		});
	}

	private void executeFetchCategoryRankTask(boolean isMoreThanDay, int offset)
	{
		if (isMoreThanDay)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ALREDAY_FETCHED_CATEGORIES_RANK_LIMIT, 0);
		}
		else
		{
			int alreadyFetchedCategoriesLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ALREDAY_FETCHED_CATEGORIES_RANK_LIMIT, 0);
			offset = alreadyFetchedCategoriesLimit > offset ? alreadyFetchedCategoriesLimit : offset;
		}
		FetchCategoryRanksTask fetchCategoryRanksTask = new FetchCategoryRanksTask(offset);
		fetchCategoryRanksTask.execute();
	}

	public void fetchCategoryTagdataTask(List<CategoryTagData> list)
    {
        if (!Utils.isEmpty(list))
        {
            FetchCategoryTagDataTask fetchCategoryTagDataTask = new FetchCategoryTagDataTask(list);
            fetchCategoryTagDataTask.execute();
        }
    }

	public List<StickerCategory> getStickerCategoryList()
	{
		List<StickerCategory> stickerCategoryList = new ArrayList<StickerCategory>(stickerCategoriesMap.values());
		Collections.sort(stickerCategoryList);
		return stickerCategoryList;
	}

	public Map<String, StickerCategory> getStickerCategoryMap()
	{
		return stickerCategoriesMap;
	}

	public void setupStickerCategoryList()
	{
		/*
		 * TODO : This will throw an exception in case of remove category as, this function will be called from mqtt thread and stickerCategories will be called from UI thread
		 * also.
		 */
		stickerCategoriesMap.clear();
		stickerCategoriesMap.putAll(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(true));
	}


	public void removeCategory(String removedCategoryId, boolean forceRemoveCategory)
	{
		HikeConversationsDatabase.getInstance().removeStickerCategory(removedCategoryId, forceRemoveCategory);
        StickerCategory removedCategory = stickerCategoriesMap.remove(removedCategoryId);

        if(removedCategory == null)
        {
            removedCategory = new StickerCategory.Builder().setCategoryId(removedCategoryId).build(); // creating new instance because of invisible category
        }

        int removedUcid = removedCategory.getUcid();

		Set<String> removedSet = new HashSet<String>();
        Set<Integer> removedUcids = new HashSet<Integer>();

		if (!removedCategory.isCustom())
		{
			String categoryDirPath = getStickerDirectoryForCategoryId(removedCategoryId);
			if (categoryDirPath != null)
			{
				File smallCatDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);
				String bigCatDirPath = categoryDirPath;
				// Removing only large and small stickers folders in case of pack delete by user; otherwise removing entire category folder
				if (!forceRemoveCategory)
				{
					bigCatDirPath += HikeConstants.LARGE_STICKER_ROOT;
				}
				File bigCatDir = new File(bigCatDirPath);
				if (smallCatDir.exists())
				{
					String[] stickerIds = smallCatDir.list();
					for (String stickerId : stickerIds)
					{
                        Sticker sticker = new Sticker(removedCategoryId, stickerId);
						removeStickerFromCustomCategory(sticker);
                        if(!forceRemoveCategory)
                        {
                            removedSet.add(sticker.getStickerCode());
                        }
					}
				}
				Utils.deleteFile(bigCatDir);
				Utils.deleteFile(smallCatDir);
			}
        }
        HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);

		// Remove data being used for sticker search w.r.t. deleted sticker category here
		if (forceRemoveCategory)
		{
			removedSet.add(removedCategoryId);
            removedUcids.add(removedUcid);
			StickerSearchManager.getInstance().removeDeletedStickerTags(removedSet, StickerSearchConstants.REMOVAL_BY_CATEGORY_DELETED);
            CategorySearchManager.removeShopSearchTagsForCategory(removedUcids);
            deleteStickerForCategory(removedCategory);
		}
		else
		{
			removeTagForDeletedStickers(removedSet);
            deactivateStickerForCategory(removedCategory);
		}
	}

	public void removeTagForDeletedStickers(Set<String> removedStickerInfoSet)
	{
		StickerSearchManager.getInstance().removeDeletedStickerTags(removedStickerInfoSet, StickerSearchConstants.REMOVAL_BY_STICKER_DELETED);
	}

	public void addNoMediaFilesToStickerDirectories()
	{
		if(isStickerFolderError())
		{
			return;
		}
		File root = new File(stickerExternalDir);
		if (!root.exists())
		{
			return;
		}
		addNoMedia(root);

		HikeSharedPreferenceUtil.getInstance().saveData(ADD_NO_MEDIA_FILE_FOR_STICKERS, true);
		HikeSharedPreferenceUtil.getInstance().saveData(ADD_NO_MEDIA_FILE_FOR_STICKER_OTHER_FOLDERS, true);
	}

	private void addNoMedia(File directory)
	{
		try
		{
			String path = directory.getPath();
			if (path.endsWith(HikeConstants.LARGE_STICKER_ROOT) || path.endsWith(HikeConstants.SMALL_STICKER_ROOT) || path.endsWith(OTHER_STICKER_ASSET_ROOT))
			{
				Utils.makeNoMediaFile(directory);
			}
			else if (directory.isDirectory())
			{
				File[] files = directory.listFiles();

				if(files == null)
				{
					return ;
				}

				for (File file : files)
				{
					addNoMedia(file);
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * 
	 * @return pair in which first parameter is boolean -- delete tags if true else not. pair in which second parameter is list -- list of sticker categories currently user has
	 */
	public Pair<Boolean, List<StickerCategory>> getAllStickerCategories()
	{
		List<StickerCategory> allCategoryList = null;
		if (isStickerFolderError())
		{
			sendStickerFolderLockedError("Unable to access android folder.");
			return new Pair<Boolean, List<StickerCategory>>(false, null);
		}

		File root = new File(stickerExternalDir);
		if (!root.exists() || !root.isDirectory())
		{
			sendStickerFolderLockedError("Unable to access sticker root folder.");
			return new Pair<Boolean, List<StickerCategory>>(false, null);
		}

		File[] files = root.listFiles();

		if (files == null || files.length == 0)
		{
			sendStickerFolderLockedError("Sticker root folder is empty.");
			return new Pair<Boolean, List<StickerCategory>>(true, null);
		}

		allCategoryList = new ArrayList<>(files.length);

		for (File file : files)
		{
			if (file.isDirectory())
			{
				StickerCategory stickerCategory = new StickerCategory.Builder().setCategoryId(file.getName()).build();
				allCategoryList.add(stickerCategory);
			}
		}

		sendStickerFolderLockedError("Current sticker categories count = " + allCategoryList.size());

		return new Pair<Boolean, List<StickerCategory>>(true, allCategoryList);
	}

	public List<Sticker> getAllStickers()
	{
		List<Sticker> stickerSet = null;

		List<StickerCategory> stickerCategoryList = StickerManager.getInstance().getAllStickerCategories().second;
		if (Utils.isEmpty(stickerCategoryList))
		{
			Logger.wtf(TAG, "Empty sticker category list while downloading tags first time.");
		}
		else
		{
			stickerSet = new ArrayList<>();

			for (StickerCategory category : stickerCategoryList)
			{
				List<Sticker> stickers = category.getStickerListFromFiles();
				stickerSet.addAll(stickers);
			}
		}



		return stickerSet;
	}

	public void addRecentSticker(Sticker st)
	{
		if (stickerCategoriesMap.containsKey(StickerManager.RECENT))
		{
			((CustomStickerCategory) stickerCategoriesMap.get(StickerManager.RECENT)).addSticker(st);
		}
	}

	public void removeSticker(Sticker sticker)
	{
		String categoryDirPath = getStickerDirectoryForCategoryId(sticker.getCategoryId());
		if (categoryDirPath == null)
		{
			return;
		}
		File categoryDir = new File(categoryDirPath);

		/*
		 * If the category itself does not exist, then we have nothing to delete
		 */
		if (!categoryDir.exists())
		{
			return;
		}

		File stickerSmall = new File(categoryDir + HikeConstants.SMALL_STICKER_ROOT, sticker.getStickerId());
		stickerSmall.delete();
		deactivateSticker(sticker);

		if (stickerCategoriesMap == null)
		{
			return;
		}
		Sticker st = new Sticker(sticker.getCategoryId(), sticker.getStickerId());
		removeStickerFromCustomCategory(st);
	}

	private void removeStickerFromCustomCategory(Sticker st)
	{
		for (StickerCategory category : stickerCategoriesMap.values())
		{
			if (category.isCustom())
			{
				((CustomStickerCategory) category).removeSticker(st);
				Logger.d(TAG, "Sticker removed from custom category : " + category.getCategoryId());
			}
		}
	}

	public void setStickerUpdateAvailable(String categoryId, boolean updateAvailable)
	{
		updateStickerCategoryData(categoryId, updateAvailable, -1, -1, null, null);
	}

	public void updateStickerCategoryData(String categoryId, Boolean updateAvailable, int totalStickerCount, int categorySize, String description, String stickerListString)
	{
		StickerCategory category = stickerCategoriesMap.get(categoryId);
		if (category != null)
		{
			if (updateAvailable != null)
			{
				// Update Available will be true only if total count received is greater than existing sticker count
				updateAvailable = (totalStickerCount > category.getTotalStickers());
				category.setUpdateAvailable(updateAvailable);
			}
			if (totalStickerCount != -1)
			{
				category.setTotalStickers(totalStickerCount);
			}
			if (categorySize != -1)
			{
				category.setCategorySize(categorySize);
			}
		}

		/**
		 * Not setting update available flag for invisible category
		 */
		if (category == null && updateAvailable != null)
		{
			updateAvailable = false;
		}

		HikeConversationsDatabase.getInstance().updateStickerCategoryData(categoryId, updateAvailable, totalStickerCount, categorySize, description, stickerListString);
	}

	public String getInternalStickerDirectoryForCategoryId(String catId)
	{
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT + "/" + catId;
	}

	/**
	 * Returns the directory for a sticker category.
	 * 
	 * @param catId
	 * 
	 * @return
	 */
	public String getStickerDirectoryForCategoryId(String catId)
	{
		/*
		 * We give a higher priority to external storage. If we find an exisiting directory in the external storage, we will return its path. Otherwise if there is an exisiting
		 * directory in internal storage, we return its path.
		 * 
		 * If the directory is not available in both cases, we return the external storage's path if external storage is available. Else we return the internal storage's path.
		 */
		boolean externalAvailable = false;
		ExternalStorageState st = Utils.getExternalStorageState();
		Logger.d(TAG, "External Storage state : " + st.name());
		if (st == ExternalStorageState.WRITEABLE)
		{
			externalAvailable = true;
			String stickerDirPath = getStickerCategoryDirPath(catId);
			Logger.d(TAG, "Sticker dir path : " + stickerDirPath);
			if (stickerDirPath == null)
			{
				return null;
			}

			File stickerDir = new File(stickerDirPath);

			if (stickerDir.exists())
			{
				Logger.d(TAG, "Sticker Dir exists .... so returning");
				return stickerDir.getPath();
			}
		}
		if (externalAvailable)
		{
			Logger.d(TAG, "Returning external storage dir.");
			return getStickerCategoryDirPath(catId);
		}
		else
		{
			return null;
		}
	}

	public String getStickerCategoryDirPath(String categoryId)
	{
		if (!isStickerFolderError())
		{
			return stickerExternalDir + File.separator + categoryId;
		}

		return null;
	}

	public boolean checkIfStickerCategoryExists(String categoryId)
	{
		String path = getStickerDirectoryForCategoryId(categoryId);
		if (path == null)
			return false;

		File categoryDir = new File(path + HikeConstants.SMALL_STICKER_ROOT);
		if (categoryDir.exists())
		{
			String[] stickerIds = categoryDir.list(stickerFileFilter);
			if (stickerIds.length > 0)
				return true;
			else
				return false;
		}
		return false;
	}

	public StickerCategory getCategoryForId(String categoryId)
	{
		return stickerCategoriesMap.get(categoryId);
	}

	public void saveRecents()
	{
		StickerCategory customCategory = stickerCategoriesMap.get(StickerManager.RECENT);

		if (customCategory == null)
		{
			return;
		}

		/**
		 * Putting an instance of check here to avoid ClassCastException.
		 */

		if (!(customCategory instanceof CustomStickerCategory))
		{
			Logger.d("StickerManager", "Inside saveSortedListforCategory : " + customCategory.getCategoryName() + " is not CustomStickerCategory");
			return;
		}

		Set<Sticker> list = ((CustomStickerCategory) customCategory).getStickerSet();

		HikeConversationsDatabase.getInstance().saveRecentStickers(list);
	}

	public void saveSortedListForCategory(String catId)
	{
		StickerCategory customCategory = stickerCategoriesMap.get(catId);

		if (customCategory == null)
		{
			return;
		}

		/**
		 * Putting an instance of check here to avoid ClassCastException.
		 */

		if (!(customCategory instanceof CustomStickerCategory))
		{
			Logger.d("StickerManager", "Inside saveSortedListforCategory : " + customCategory.getCategoryName() + " is not CustomStickerCategory");
			return;
		}

		Set<Sticker> list = ((CustomStickerCategory) customCategory).getStickerSet();
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try
		{
			if (list.size() == 0)
				return;

			long t1 = System.currentTimeMillis();
			String extDir = StickerManager.getInstance().getInternalStickerDirectoryForCategoryId(catId);
			File dir = new File(extDir);
			if (!dir.exists() && !dir.mkdirs())
			{
				return;
			}
			File catFile = new File(extDir, catId + ".bin");
			fileOut = new FileOutputStream(catFile);
			out = new ObjectOutputStream(fileOut);
			out.writeInt(list.size());
			synchronized (list)
			{
				Iterator<Sticker> it = list.iterator();
				Sticker st = null;
				while (it.hasNext())
				{
					try
					{
						st = it.next();
						st.serializeObj(out);
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception while serializing a sticker : " + st.getStickerId(), e);
					}
				}
			}
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			long t2 = System.currentTimeMillis();
			Logger.d(TAG, "Time in ms to save sticker list of category : " + catId + " to file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception while saving category file.", e);
		}
		finally
		{
			Utils.closeStreams(out, fileOut);
		}
	}

	public void deleteStickers()
	{
		/*
		 * First delete all stickers, if any, in the internal memory
		 */
		String dirPath = context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT;
		File dir = new File(dirPath);
		if (dir.exists())
		{
			Utils.deleteFile(dir);
		}

		/*
		 * Next is the external memory. We first check if its available or not.
		 */
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE || isStickerFolderError())
		{
			return;
		}

		File extDir = new File(stickerExternalDir);
		if (extDir.exists())
		{
			Utils.deleteFile(extDir);
		}

		/* Delete recent stickers */
		String recentsDir = getStickerDirectoryForCategoryId(StickerManager.RECENT);
		File rDir = new File(recentsDir);
		if (rDir.exists())
			Utils.deleteFile(rDir);
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	public void deleteDuplicateFiles(HashSet<String> originalNames, String fileDir)
	{
		File dir = new File(fileDir);
		String[] fileNames = null;
		if (dir.exists() && dir.isDirectory())
		{
			fileNames = dir.list();
		}
		else
		{
			return;
		}
		for (String fileName : fileNames)
		{
			if (originalNames.contains(fileName))
			{
				File file = new File(fileDir, fileName);
				if (file.exists())
				{
					file.delete();
				}
			}
		}
	}

	private String getStickerRootDirectory(Context context)
	{
		boolean externalAvailable = false;
		ExternalStorageState st = Utils.getExternalStorageState();
		if (st == ExternalStorageState.WRITEABLE)
		{
			externalAvailable = true;
			if (stickerExternalDir == null)
			{
				return null;
			}

			File stickerDir = new File(stickerExternalDir);

			if (stickerDir.exists())
			{
				return stickerDir.getPath();
			}
		}
		File stickerDir = new File(getInternalStickerRootDirectory(context));
		if (stickerDir.exists())
		{
			return stickerDir.getPath();
		}
		if (externalAvailable)
		{
			return stickerExternalDir;
		}
		return getInternalStickerRootDirectory(context);
	}

	private String getInternalStickerRootDirectory(Context context)
	{
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT;
	}

	public Map<String, StickerCategory> getStickerToCategoryMapping(Context context)
	{
		String stickerRootDirectoryString = getStickerRootDirectory(context);

		/*
		 * Return null if the the path is null or empty
		 */
		if (TextUtils.isEmpty(stickerRootDirectoryString))
		{
			return null;
		}

		File stickerRootDirectory = new File(stickerRootDirectoryString);

		/*
		 * Return null if the directory is null or does not exist
		 */
		if (stickerRootDirectory == null || !stickerRootDirectory.exists())
		{
			return null;
		}

		Map<String, StickerCategory> stickerToCategoryMap = new HashMap<String, StickerCategory>();

		File[] stickerRootFiles = stickerRootDirectory.listFiles();

		if (stickerRootFiles == null)
		{
			return null;
		}

		for (File stickerCategoryDirectory : stickerRootFiles)
		{
			/*
			 * If this is not a directory we have no need for this file.
			 */
			if (!stickerCategoryDirectory.isDirectory())
			{
				continue;
			}

			File stickerCategorySmallDirectory = new File(stickerCategoryDirectory.getAbsolutePath() + HikeConstants.SMALL_STICKER_ROOT);

			/*
			 * We also don't want to do anything if the category does not have a small folder.
			 */
			if (stickerCategorySmallDirectory == null || !stickerCategorySmallDirectory.exists())
			{
				continue;
			}
			StickerCategory stickerCategory = stickerCategoriesMap.get(stickerCategoryDirectory.getName());
			if (stickerCategory == null)
			{
				stickerCategory = new StickerCategory.Builder().setCategoryId(stickerCategoryDirectory.getName()).build();
			}
			File[] smallDirFiles = stickerCategorySmallDirectory.listFiles();

			if (smallDirFiles == null)
			{
				continue;
			}

			for (File stickerFile : smallDirFiles)
			{
				stickerToCategoryMap.put(stickerFile.getName(), stickerCategory);
			}
		}

		return stickerToCategoryMap;
	}

	/**
	 * solves recent sticker proguard issue , we serialize stickers , but proguard is changing file name sometime and recent sticker deserialize fails , and we loose recent sticker
	 * file
	 * 
	 * fix is : we read file , make recent sticker file as per new name and proguard has been changed so it will not obfuscate file name of Sticker
	 */
	public final void updateRecentStickerFile(SharedPreferences settings)
	{
		Logger.i("recent", "Recent Sticker Save Mechanism started");
		// save to preference as we want to try correction logic only once
		Editor edit = settings.edit();
		edit.putBoolean(StickerManager.RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED, true);
		edit.commit();
		Map<String, StickerCategory> stickerCategoryMapping = getStickerToCategoryMapping(context);
		// we do not want to try more than once, any failure , lets ignore this process there after
		if (stickerCategoryMapping == null)
		{
			return;
		}
		BufferedReader bufferedReader = null;
		try
		{
			String filePath = getInternalStickerDirectoryForCategoryId(StickerManager.RECENT);
			File dir = new File(filePath);
			if (!dir.exists())
			{
				return;
			}
			File file = new File(dir, StickerManager.RECENT + ".bin");
			if (file.exists())
			{
				bufferedReader = new BufferedReader(new FileReader(file));
				String line = STRING_EMPTY;
				StringBuilder str = new StringBuilder();
				while ((line = bufferedReader.readLine()) != null)
				{
					str.append(line);
				}
				Set<Sticker> recentStickers = Collections.synchronizedSet(new LinkedHashSet<Sticker>());

				Pattern p = Pattern.compile("(\\d{3}_.*?\\.png.*?)");
				Matcher m = p.matcher(str);

				while (m.find())
				{
					String stickerId = m.group();
					Logger.i("recent", "Sticker id found is " + stickerId);
					Sticker st = new Sticker();
					st.setStickerData(-1, stickerId, stickerCategoryMapping.get(stickerId));
					recentStickers.add(st);
				}
				saveSortedListForCategory(StickerManager.RECENT);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			Logger.i("recent", "Recent Sticker Save Mechanism finished");
			if (bufferedReader != null)
			{
				try
				{
					bufferedReader.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	public byte[] saveLargeStickers(String largeStickerDirPath, String stickerId, String stickerData) throws IOException
	{
		File f = new File(largeStickerDirPath, stickerId);
		return Utils.saveBase64StringToFile(f, stickerData);
	}

	/*
	 * TODO this logic is temporary we yet need to change it
	 */
	public void saveLargeStickers(String largeStickerDirPath, String stickerId, Bitmap largeStickerBitmap) throws IOException
	{
		if (largeStickerBitmap != null)
		{
			BitmapUtils.saveBitmapToFileAndRecycle(largeStickerDirPath, stickerId, largeStickerBitmap);
		}
	}

	public void saveSmallStickers(String smallStickerDirPath, String stickerId, String largeStickerFilePath) throws IOException
	{
		Bitmap bitmap = HikeBitmapFactory.decodeSmallStickerFromObject(largeStickerFilePath, SIZE_IMAGE, SIZE_IMAGE, Bitmap.Config.ARGB_8888);
		if (bitmap != null)
		{
			BitmapUtils.saveBitmapToFileAndRecycle(smallStickerDirPath, stickerId, bitmap);
		}
	}

	public void saveSmallStickers(String smallStickerDirPath, String stickerId, byte[] largeStickerByteArray) throws IOException
	{
		Bitmap bitmap = HikeBitmapFactory.decodeSmallStickerFromObject(largeStickerByteArray, SIZE_IMAGE, SIZE_IMAGE, Bitmap.Config.ARGB_8888);
		if (bitmap != null)
		{
			BitmapUtils.saveBitmapToFileAndRecycle(smallStickerDirPath, stickerId, bitmap);
		}
	}

	public static boolean moveHardcodedStickersToSdcard(Context context)
	{
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return false;
		}
		boolean result = true;
		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, STICKERS_JSON_FILE_NAME));
			JSONArray harcodedStickers = jsonObj.optJSONArray(HARCODED_STICKERS);
			for (int i = 0; i < harcodedStickers.length(); i++)
			{
				JSONObject obj = harcodedStickers.optJSONObject(i);
				String categoryId = obj.getString(CATEGORY_ID);

				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (directoryPath == null)
				{
					result = false;
					break;
				}

				Resources mResources = context.getResources();
				File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
				File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);

				if (!smallStickerDir.exists())
				{
					smallStickerDir.mkdirs();
				}
				if (!largeStickerDir.exists())
				{
					largeStickerDir.mkdirs();
				}

				Utils.makeNoMediaFile(largeStickerDir);
				Utils.makeNoMediaFile(smallStickerDir);

				JSONArray stickerIds = obj.getJSONArray(STICKER_IDS);
				JSONArray resourceIds = obj.getJSONArray(RESOURCE_IDS);

				for (int j = 0; j < stickerIds.length(); j++)
				{
					String stickerId = stickerIds.optString(j);
					String resName = resourceIds.optString(j);
					int resourceId = mResources.getIdentifier(resName, "drawable", context.getPackageName());
					Bitmap stickerBitmap = HikeBitmapFactory.decodeBitmapFromResource(mResources, resourceId, Bitmap.Config.ARGB_8888);
					File f = new File(largeStickerDir, stickerId);
					StickerManager.getInstance().saveLargeStickers(largeStickerDir.getAbsolutePath(), stickerId, stickerBitmap);
					if (f != null)
					{
						StickerManager.getInstance().saveSmallStickers(smallStickerDir.getAbsolutePath(), stickerId, f.getAbsolutePath());
					}
					else
					{
						Logger.i(TAG, "moveHardcodedStickersToSdcard failed resName = " + resName + " not found");
						result = false;
					}
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			result = false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			result = false;
		}

		return result;
	}

	public boolean moveStickerPreviewAssetsToSdcard()
	{
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return false;
		}
		boolean result = true;
		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);

				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (directoryPath == null)
				{
					result = false;
					break;
				}

				File otherAssetsDir = getOtherAssetsStickerDirectory(directoryPath);

				String pallateIcon = obj.optString(StickerManager.PALLATE_ICON);
				String pallateIconSelected = obj.optString(StickerManager.PALLATE_ICON_SELECTED);
				String previewImage = obj.optString(StickerManager.PREVIEW_IMAGE);

				saveAssetToDirectory(otherAssetsDir, pallateIcon, StickerManager.PALLATE_ICON);
				saveAssetToDirectory(otherAssetsDir, pallateIconSelected, StickerManager.PALLATE_ICON_SELECTED);
				if (!TextUtils.isEmpty(previewImage))
				{
					saveAssetToDirectory(otherAssetsDir, previewImage, StickerManager.PREVIEW_IMAGE);
				}
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}

		return result;
	}

	private void saveAssetToDirectory(File dir, String assetName, String fileName) throws IOException
	{
		int assetResId = context.getResources().getIdentifier(assetName, "drawable", context.getPackageName());
		Bitmap assetBitmap = HikeBitmapFactory.decodeBitmapFromResource(context.getResources(), assetResId, Bitmap.Config.ARGB_8888);
		if (assetBitmap != null)
		{
			File file = new File(dir, fileName + ".png");
			BitmapUtils.saveBitmapToFile(file, assetBitmap);
		}
	}

	public File getOtherAssetsStickerDirectory(String directoryPath)
	{
		File otherAssetsDir = new File(directoryPath + OTHER_STICKER_ASSET_ROOT);

		if (!otherAssetsDir.exists())
		{
			otherAssetsDir.mkdirs();
		}

		Utils.makeNoMediaFile(otherAssetsDir);
		return otherAssetsDir;
	}

	public List<StickerCategory> getMyStickerCategoryList()
	{
		ArrayList<StickerCategory> stickerCategories = new ArrayList<StickerCategory>(stickerCategoriesMap.values());
		Collections.sort(stickerCategories);
		ArrayList<StickerCategory> invisibleCategories = new ArrayList<StickerCategory>(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(false)
				.values());
		Collections.sort(invisibleCategories);
		stickerCategories.addAll(invisibleCategories);
		Iterator<StickerCategory> it = stickerCategories.iterator();
		while (it.hasNext())
		{
			StickerCategory sc = it.next();
			if (sc.isCustom())
			{
				it.remove();
			}
		}

		return stickerCategories;
	}

	public void saveVisibilityAndIndex(Set<StickerCategory> stickerCategories)
	{
		/**
		 * Removing invisible/Adding visible categories from the StickerCategory Map
		 */
		for (StickerCategory stickerCategory : stickerCategories)
		{
			if (!stickerCategory.isVisible())
			{
				stickerCategoriesMap.remove(stickerCategory.getCategoryId());
			}
			else
			{
				stickerCategoriesMap.put(stickerCategory.getCategoryId(), stickerCategory);
			}
		}

		HikeConversationsDatabase.getInstance().updateVisibilityAndIndex(stickerCategories);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	public int getNumColumnsForStickerGrid(Context context)
	{
		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

		return ((int) (screenWidth / SIZE_IMAGE));
	}

	public void sucessFullyDownloadedStickers(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;
		String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
		DownloadType downloadType = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
		DownloadSource downloadSource = (DownloadSource) b.getSerializable(HikeConstants.DOWNLOAD_SOURCE);
		StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);

		if (category == null)
		{
			return;
		}
		category.updateDownloadedStickersCount();
		if (downloadSource == DownloadSource.SHOP || downloadSource == DownloadSource.SETTINGS || downloadSource == DownloadSource.POPUP)
		{
			category.setState(StickerCategory.DONE_SHOP_SETTINGS);
		}
		else
		{
			category.setState(StickerCategory.DONE);
		}
		if (DownloadType.UPDATE.equals(downloadType))
		{
			StickerManager.getInstance().setStickerUpdateAvailable(categoryId, false);
			Intent i = new Intent(StickerManager.STICKERS_UPDATED);
			i.putExtra(CATEGORY_ID, categoryId);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}

		else if (DownloadType.MORE_STICKERS.equals(downloadType))
		{
			Intent i = new Intent(StickerManager.MORE_STICKERS_DOWNLOADED);
			i.putExtra(CATEGORY_ID, categoryId);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}

		else if (DownloadType.NEW_CATEGORY.equals(downloadType))
		{
			Intent i = new Intent(StickerManager.STICKERS_DOWNLOADED);
			i.putExtra(CATEGORY_ID, categoryId);
			i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}
	}

	public void stickersDownloadFailed(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;
		String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
		StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
		if (category != null)
		{
			category.setState(StickerCategory.RETRY); // Doing it here for safety. On orientation change, the stickerAdapter reference can become null, hence the broadcast won't be
														// received there
		}
		Intent i = new Intent(StickerManager.STICKERS_FAILED);
		i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
	}

	public void onStickersDownloadProgress(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;

		Intent i = new Intent(StickerManager.STICKERS_PROGRESS);
		i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
	}

	/**
	 * Returns a category preview {@link Bitmap}
	 *
	 * @param ctx
	 * @param categoryId
	 * @param downloadIfNotFound
	 *            -- true if it should be downloaded if not found.
	 * @return {@link Bitmap}
	 */
	public Bitmap getCategoryOtherAsset(Context ctx, String categoryId, int type, int width, int height, boolean downloadIfNotFound)
	{
		String baseFilePath = getStickerDirectoryForCategoryId(categoryId) + OTHER_STICKER_ASSET_ROOT + "/";
		Bitmap bitmap = null;
		int defaultIconResId = 0;
		switch (type)
		{
		case PALLATE_ICON_TYPE:
			if(isQuickSuggestionCategory(categoryId))
			{
				bitmap = HikeBitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_thunderbolt_inactive);
			}
			else
			{
				baseFilePath += PALLATE_ICON + OTHER_ICON_TYPE;
				bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
				defaultIconResId = R.drawable.misc_sticker_placeholder;
			}
			break;
		case PALLATE_ICON_SELECTED_TYPE:
			if(isQuickSuggestionCategory(categoryId))
			{
				bitmap = HikeBitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_thunderbolt);
			}
			else
			{
				baseFilePath += PALLATE_ICON_SELECTED + OTHER_ICON_TYPE;
				bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
				defaultIconResId = R.drawable.misc_sticker_placeholder_selected;
			}
			break;
		case PREVIEW_IMAGE_EMPTY_PALETTE_TYPE:
		case PREVIEW_IMAGE_SHOP_TYPE:
		case PREVIEW_IMAGE_PALETTE_TYPE:
		case PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE:
		case PREVIEW_IMAGE_PACK_PREVIEW_PALETTE_TYPE:
			baseFilePath += PREVIEW_IMAGE + OTHER_ICON_TYPE;
			if (width <= 0 || height <= 0)
			{
				bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
			}
			else
			{
				bitmap = HikeBitmapFactory.scaleDownBitmap(baseFilePath, width, height, true, false);
			}
			defaultIconResId = R.drawable.shop_placeholder;
			break;
		default:
			break;
		}
		if (bitmap == null)
		{
			bitmap = HikeBitmapFactory.decodeResource(ctx.getResources(), defaultIconResId);
			if (downloadIfNotFound)
			{
				switch (type)
				{
				case PALLATE_ICON_TYPE:
				case PALLATE_ICON_SELECTED_TYPE:
					StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(categoryId);
					stickerPalleteImageDownloadTask.execute();
					break;
				case PREVIEW_IMAGE_EMPTY_PALETTE_TYPE:
				case PREVIEW_IMAGE_SHOP_TYPE:
				case PREVIEW_IMAGE_PALETTE_TYPE:
				case PREVIEW_IMAGE_PACK_PREVIEW_SHOP_TYPE:
				case PREVIEW_IMAGE_PACK_PREVIEW_PALETTE_TYPE:
					StickerPreviewImageDownloadTask stickerPreviewImageDownloadTask = new StickerPreviewImageDownloadTask(categoryId, type);
					stickerPreviewImageDownloadTask.execute();
					break;
				default:
					break;
				}
			}
		}

		return bitmap;
	}

	/**
	 * Generates StickerPageAdapterItemList based on the StickersList provided
	 * 
	 * @param stickersList
	 * @return
	 */
	public List<StickerPageAdapterItem> generateStickerPageAdapterItemList(List<Sticker> stickersList)
	{
		List<StickerPageAdapterItem> stickerPageList = new ArrayList<StickerPageAdapterItem>();
		if (stickersList != null)
		{
			for (Sticker st : stickersList)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, st));
			}
		}
		return stickerPageList;
	}

	public JSONArray getAllInitialyInsertedStickerCategories()
	{
		JSONObject jsonObj;
		JSONArray jsonArray = new JSONArray();
		try
		{
			jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);
				boolean isCustom = obj.optBoolean(StickerManager.IS_CUSTOM);
				if (!isCustom)
				{
					jsonArray.put(categoryId);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return jsonArray;
	}

	public void checkAndDownLoadStickerData()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickerManager.STICKERS_SIZE_DOWNLOADED, false))
		{
			return;
		}

		StickerSignupUpgradeDownloadTask stickerSignupUpgradeDownloadTask = new StickerSignupUpgradeDownloadTask(getAllInitialyInsertedStickerCategories());
		stickerSignupUpgradeDownloadTask.execute();
	}

	public void showStickerRecommendTurnOnToast()
	{
		boolean stickerRecommendPref = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMEND_PREF, true);
		boolean stickerRecommendAutoPref = StickerSearchUtility.getStickerRecommendationSettingsValue(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, true);
		boolean stickerRecommendTurnOnToastPref = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMEND_SETTING_OFF_TOAST, false);
		boolean stickerAutoRecommendTurnOffTipPref = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_AUTO_RECOMMEND_SETTING_OFF_TIP, false);

		if (!stickerRecommendTurnOnToastPref && (!stickerRecommendPref || (!stickerRecommendAutoPref && !stickerAutoRecommendTurnOffTipPref)))
		{
			Toast.makeText(HikeMessengerApp.getInstance(), HikeMessengerApp.getInstance().getResources().getString(R.string.sticker_recommend_settings_toast), Toast.LENGTH_LONG)
					.show();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_RECOMMEND_SETTING_OFF_TOAST, true);
		}
	}

	public void updateStickerCategoriesMetadata(JSONArray jsonArray)
	{
		int length = jsonArray.length();
		List<StickerCategory> visibleStickerCategories = new ArrayList<StickerCategory>();
		int humanoidCategoryIndex = stickerCategoriesMap.get(HUMANOID).getCategoryIndex();
		for (int i = 0; i < length; i++)
		{
			JSONObject jsonObj = jsonArray.optJSONObject(i);

			if (jsonObj != null)
			{

				StickerCategory category = parseStickerCategoryMetadata(jsonObj);
				if(category == null)
				{
					continue;
				}
				if (category.isVisible())
				{
					stickerCategoriesMap.put(category.getCategoryId(), category);
				}
				visibleStickerCategories.add(category);
				category.setCategoryIndex(humanoidCategoryIndex + visibleStickerCategories.size());
				category.setIsDownloaded(true); // all initial categories are downloaded
			}
		}
		if (!visibleStickerCategories.isEmpty())
		{
			// Updating category index for all other sticker categories as well
			for (StickerCategory stickerCategory : stickerCategoriesMap.values())
			{
				if (visibleStickerCategories.contains(stickerCategory) || stickerCategory.isCustom() || stickerCategory.getCategoryId().equals(HUMANOID))
				{
					continue;
				}
				int currentIndex = stickerCategory.getCategoryIndex();
				stickerCategory.setCategoryIndex(currentIndex + visibleStickerCategories.size());
			}
		}

		HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(stickerCategoriesMap.values());
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	public void initiateSingleStickerDownloadTask(String stickerId, String categoryId, ConvMessage convMessage)
	{
		initiateSingleStickerDownloadTask(stickerId, categoryId, convMessage, false);
	}

	public void initiateSingleStickerDownloadTask(String stickerId, String categoryId, ConvMessage convMessage, boolean isMini)
	{
		SingleStickerDownloadTask singleStickerDownloadTask = new SingleStickerDownloadTask(stickerId, categoryId, convMessage, isMini);
		singleStickerDownloadTask.execute();
	}

	public void initiateMiniStickerDownloadTask(String stickerId, String categoryId)
	{
		initiateSingleStickerDownloadTask(stickerId, categoryId, null, true);
	}

	public void initiateSingleStickerTagDownloadTask(String stickerId, String categoryId)
	{
		SingleStickerTagDownloadTask singleStickerTagDownloadTask = new SingleStickerTagDownloadTask(stickerId, categoryId);
		singleStickerTagDownloadTask.execute();
	}

	public void initialiseDownloadStickerPackTask(StickerCategory category, JSONObject bodyJson)
	{
		DownloadType downloadType = category.isUpdateAvailable() ? DownloadType.UPDATE : DownloadType.MORE_STICKERS;
		initialiseDownloadStickerPackTask(category, downloadType, bodyJson);
	}

	public void initialiseCategoryDetailsTask(String categoryId)
	{
		StickerCategoryDownloadTask stickerCategoryDownloadTask = new StickerCategoryDownloadTask(categoryId);
		stickerCategoryDownloadTask.execute();
	}

	public void initialiseDownloadStickerPackTask(StickerCategory category, DownloadType downloadType, JSONObject bodyJson)
	{
		if (stickerCategoriesMap.containsKey(category.getCategoryId()))
		{
			category = stickerCategoriesMap.get(category.getCategoryId());
		}
		if (category.getTotalStickers() == 0 || category.getDownloadedStickersCount() < category.getTotalStickers())
		{
			category.setState(StickerCategory.DOWNLOADING);
			makePackDownloadCall(category, downloadType, bodyJson);
		}
		else if (category.getDownloadedStickersCount() >= category.getTotalStickers())
		{
			category.setState(StickerCategory.DONE);
		}
		saveCategoryAsVisible(category);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	private void makePackDownloadCall(StickerCategory category, DownloadType downloadType, JSONObject bodyJson)
	{
		if ((category.getTotalStickers() <= 0 || category.getDownloadedStickersCount() > HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_PACK_CDN_THRESHOLD,
				StickerConstants.DEFAULT_STICKER_THRESHOLD_FOR_CDN))
				|| !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_PACK_CDN, true))
		{
			MultiStickerDownloadTask multiStickerDownloadTask = new MultiStickerDownloadTask(category, downloadType, bodyJson);
			multiStickerDownloadTask.execute();
		}
		else
		{
			MultiStickerImageDownloadTask multiStickerImageDownloadTask = new MultiStickerImageDownloadTask(category, downloadType, bodyJson);
			multiStickerImageDownloadTask.execute();
		}
	}

	public void initiateSingleStickerQuickSuggestionDownloadTask(Sticker quickSuggestSticker)
	{
		if(!QuickStickerSuggestionController.getInstance().isQuickSuggestionEnabled())
		{
			return ;
		}
		QuickStickerSuggestionController.getInstance().saveInRetrySet(quickSuggestSticker);
		SingleStickerQuickSuggestionDownloadTask singleStickerQuickSuggestionDownloadTask = new SingleStickerQuickSuggestionDownloadTask(quickSuggestSticker);
		singleStickerQuickSuggestionDownloadTask.execute();
	}

	public void initiateMultiStickerQuickSuggestionDownloadTask(Set<Sticker> quickSuggestStickerSet)
	{
		if(!QuickStickerSuggestionController.getInstance().isQuickSuggestionEnabled())
		{
			return ;
		}
		QuickStickerSuggestionController.getInstance().saveInRetrySet(quickSuggestStickerSet);
		MultiStickerQuickSuggestionDownloadTask multiStickerQuickSuggestionDownloadTask = new MultiStickerQuickSuggestionDownloadTask(quickSuggestStickerSet);
		multiStickerQuickSuggestionDownloadTask.execute();
	}

	public StickerCategory parseStickerCategoryMetadata(JSONObject jsonObj)
	{
		try
		{
			String catId = jsonObj.getString(StickerManager.CATEGORY_ID);

			StickerCategory category = stickerCategoriesMap.get(catId);
			if (category == null)
			{
				category = HikeConversationsDatabase.getInstance().getStickerCategoryforId(catId);
				if (category == null)
				{
					category = new StickerCategory.Builder().setCategoryId(catId).build();
				}
			}

			category.setCategoryName(jsonObj.getString(HikeConstants.CAT_NAME));

			if (jsonObj.has(HikeConstants.VISIBLITY)) {
				boolean isVisible = jsonObj.optInt(HikeConstants.VISIBLITY) == 1;
				category.setVisible(isVisible);
				if (category.isVisible()) {
					stickerCategoriesMap.put(catId, category);
				}
			}
			if (jsonObj.has(HikeConstants.NUMBER_OF_STICKERS)) {
				category.setTotalStickers(jsonObj.optInt(HikeConstants.NUMBER_OF_STICKERS, 0));
			}

			if (jsonObj.has(HikeConstants.SIZE)) {
				category.setCategorySize(jsonObj.optInt(HikeConstants.SIZE, 0));
			}

			if (jsonObj.has(HikeConstants.DESCRIPTION)) {
				category.setDescription(jsonObj.optString(HikeConstants.DESCRIPTION, ""));
			}

			if (jsonObj.has(HikeConstants.STICKER_LIST)) {
				JSONArray stickerArray = jsonObj.optJSONArray(HikeConstants.STICKER_LIST);
				String allStickerListString = Utils.isEmpty(stickerArray) ? null : stickerArray.toString();
				category.setAllStickerListString(allStickerListString);
			}

			if(jsonObj.has(HikeConstants.SIMILAR_PACKS)) {
				JSONArray similarPacksArray = jsonObj.optJSONArray(HikeConstants.SIMILAR_PACKS);
				String similarPacksString = Utils.isEmpty(similarPacksArray) ? null : similarPacksArray.toString();
				category.setSimilarPacksString(similarPacksString);
			}

			if(jsonObj.has(HikeConstants.AUTHOR)) {
				String author = jsonObj.optString(HikeConstants.AUTHOR);
				category.setAuthor(author);
			}

			if(jsonObj.has(HikeConstants.COPYRIGHT)) {
				String copyright = jsonObj.optString(HikeConstants.COPYRIGHT);
				category.setCopyRightString(copyright);
			}

			if(jsonObj.has(HikeConstants.STATE)) {
				int state = jsonObj.optInt(HikeConstants.STATE);
				category.setIsDisabled(state == 1 ? false : true);
			}
			if(jsonObj.has(HikeConstants.TIMESTAMP)) {
				int ts = jsonObj.optInt(HikeConstants.TIMESTAMP);
				category.setPackUpdationTime(ts);
			}
			if(jsonObj.has(HikeConstants.UCID)) {
				int ucid = jsonObj.optInt(HikeConstants.UCID);
				category.setUcid(ucid);
			}
			return category;
		}
		catch(JSONException ex)
		{
			Logger.e(TAG, "exception during sticker category json parsing", ex);
		}
		return null;
	}

	private void saveCategoryAsVisible(StickerCategory category)
	{
		if (category.isVisible())
		{
			return;
		}
		category.setVisible(true);
		category.setIsDownloaded(true);
		int catIdx = HikeConversationsDatabase.getInstance().getMaxStickerCategoryIndex();
		category.setCategoryIndex(catIdx == -1 ? stickerCategoriesMap.size() : (catIdx + 1));
		stickerCategoriesMap.put(category.getCategoryId(), category);
		HikeConversationsDatabase.getInstance().insertInToStickerCategoriesTable(category);
	}

	public boolean moreDataAvailableForStickerShop()
	{
		return !HikeSharedPreferenceUtil.getInstance().getData(STICKER_SHOP_DATA_FULLY_FETCHED, false);
	}

	public boolean isMinimumMemoryAvailable()
	{
		double freeSpace = Utils.getFreeSpace();

		Logger.d(TAG, "Free space: " + freeSpace);
		if (freeSpace > MINIMUM_FREE_SPACE)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public String getCategoryOtherAssetLoaderKey(String categoryId, int type)
	{
		return categoryId + HikeConstants.DELIMETER + type;
	}

	public void checkAndSendAnalytics(boolean visible)
	{
		if (visible)
		{
			if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SETTING_CHECK_BOX_CLICKED, false))
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_SETTING_CHECK_BOX_CLICKED, true);

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_CHECK_BOX_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		}
		else
		{
			if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_SETTING_UNCHECK_BOX_CLICKED, false))
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_SETTING_UNCHECK_BOX_CLICKED, true);

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_UNCHECK_BOX_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		}
	}

	/**
	 * This method is used for adding a new sticker category in pallete on the fly. The category is placed at a position in the pallete if specified, else at the end
	 */
	public void addNewCategoryInPallete(StickerCategory stickerCategory)
	{
		if (stickerCategoriesMap.containsKey(stickerCategory.getCategoryId()))
		{
			/**
			 * Discard the add packet.
			 */
			return;
		}

		boolean isCategoryInserted = HikeConversationsDatabase.getInstance().insertNewCategoryInPallete(stickerCategory);
		/**
		 * If isCategoryInserted is false, we simply return, since it's a duplicate category
		 */
		if (!isCategoryInserted)
		{
			return;
		}

		ArrayList<StickerCategory> updateCategories = new ArrayList<StickerCategory>();
		/**
		 * Incrementing the index of other categories by 1 to accommodate the new category in between
		 */

		for (StickerCategory category : stickerCategoriesMap.values())
		{
			if (category.getCategoryIndex() < stickerCategory.getCategoryIndex())
			{
				continue;
			}

			category.setCategoryIndex(category.getCategoryIndex() + 1);
			updateCategories.add(category);
		}

		stickerCategoriesMap.put(stickerCategory.getCategoryId(), stickerCategory);
		HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(updateCategories);
		/**
		 * Now download the Enable disable images as well as preview image
		 */
		StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(stickerCategory.getCategoryId());
		stickerPalleteImageDownloadTask.execute();

		StickerPreviewImageDownloadTask stickerPreviewImageDownloadTask = new StickerPreviewImageDownloadTask(stickerCategory.getCategoryId(), StickerManager.PREVIEW_IMAGE_EMPTY_PALETTE_TYPE);
		stickerPreviewImageDownloadTask.execute();

		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	/**
	 * To cater to a corner case, where server sent an update available packet, and before a user could download the new updates for a pack, the user received the new stickers. In
	 * that case, the updateAvailable flag still remains true for that category. Thus, we are removing it in case the count of stickers in folder == the actual stickers.
	 * 
	 * This method updates the sticker category object in memory as well as database.
	 *
	 */

	public void checkAndRemoveUpdateFlag(String categoryId)
	{
		StickerCategory category = getCategoryForId(categoryId);

		if (category == null)
		{
			category = HikeConversationsDatabase.getInstance().getStickerCategoryforId(categoryId);
		}

		if (category == null)
		{
			Logger.wtf(TAG, "No category found in db. Which sticker was being downloaded  : ? " + categoryId);
			return;
		}

		/**
		 * Proceeding only if a valid category is found
		 */

		if (shouldRemoveGreenDot(category))
		{
			category.setUpdateAvailable(false);

			if (category.getState() == StickerCategory.UPDATE)
			{
				category.setState(StickerCategory.NONE);
			}

			HikeConversationsDatabase.getInstance().saveUpdateFlagOfStickerCategory(category);
		}
	}

	/**
	 * Checks if category has updateAvailable flag as true and the total count of downloaded stickers in folder is same as those present in the category.
	 * 
	 * @param category
	 * @return
	 */
	private boolean shouldRemoveGreenDot(StickerCategory category)
	{
		if (category.isUpdateAvailable())
		{
			int stickerListSize = category.getStickerList().size();

			if (stickerListSize > 0 && stickerListSize == category.getTotalStickers())
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * This method is used to remove legacy green dots where needed
	 */

	private void removeLegacyGreenDots()
	{
		List<StickerCategory> myStickersList = getMyStickerCategoryList();
		ArrayList<StickerCategory> updatedList = new ArrayList<StickerCategory>();

		if (myStickersList != null)
		{
			for (StickerCategory stickerCategory : myStickersList)
			{
				if (shouldRemoveGreenDot(stickerCategory))
				{
					stickerCategory.setUpdateAvailable(false);
					updatedList.add(stickerCategory);
				}
			}

			if (updatedList.size() > 0)
			{
				HikeConversationsDatabase.getInstance().saveUpdateFlagOfStickerCategory(updatedList);
			}
		}
	}

	/**
	 * This method is to update our sticker folder names from large/small to stickers_l and stickers_s. This is being done because some cleanmaster was cleaning large named folder
	 * content
	 */
	public void updateStickerFolderNames()
	{
		if(isStickerFolderError())
		{
			return ;
		}
		File stickersRoot = new File(stickerExternalDir);

		if (!stickersRoot.exists() || !stickersRoot.canRead())
		{
			Logger.d("StickerManager", "sticker root doesn't exit or is not readable");
			return;
		}

		File[] files = stickersRoot.listFiles();

		if (files == null)
		{
			Logger.d("StickerManager", "sticker root is not a directory");
			return;
		}

		// renaming large/small folders for all categories
		for (File categoryRoot : files)
		{
			// if categoryRoot(eg. humanoid/love etc.) file is not a directory we should not do anything.
			if (categoryRoot == null || !categoryRoot.isDirectory())
			{
				continue;
			}

			File[] categoryAssetFiles = categoryRoot.listFiles();

			if (categoryAssetFiles == null)
			{
				continue;
			}

			for (File categoryAssetFile : categoryAssetFiles)
			{
				// if categoryAssetFile(eg. large/small/other) is not a directory we should not do anything.
				if (categoryAssetFile == null || !categoryAssetFile.isDirectory())
				{
					continue;
				}

				if (categoryAssetFile.getName().equals(HikeConstants.OLD_LARGE_STICKER_FOLDER_NAME))
				{
					Logger.d("StickerManager", "changing large file name for : " + categoryRoot.getName() + "category");
					categoryAssetFile.renameTo(new File(categoryRoot + HikeConstants.LARGE_STICKER_ROOT));
				}
				else if (categoryAssetFile.getName().equals(HikeConstants.OLD_SMALL_STICKER_FOLDER_NAME))
				{
					Logger.d("StickerManager", "changing small file name for : " + categoryRoot.getName() + "category");
					categoryAssetFile.renameTo(new File(categoryRoot + HikeConstants.SMALL_STICKER_ROOT));
				}
			}

		}
	}

	public void downloadStickerTagData()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.TAG_FIRST_TIME_DOWNLOAD, true))
		{
			if ((Utils.getExternalStorageState() == ExternalStorageState.NONE))
			{
				return;
			}
			StickerSearchManager.getInstance().downloadStickerTags(true, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT,
					StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE));
		}
		else
		{
			StickerSearchManager.getInstance().downloadStickerTags(false, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT,
					StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE));
			StickerSearchManager.getInstance().downloadStickerTags(false, StickerSearchConstants.STATE_STICKER_DATA_REFRESH,
					StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE));
			StickerSearchManager.getInstance().downloadStickerTags(false, StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD,
					StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE));
		}
	}

	public void downloadDefaultTagsFirstTime(boolean isSignUp)
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DEFAULT_TAGS_DOWNLOADED, false))
		{
			StickerLanguagesManager.getInstance().redownloadAllDefaultTagsForLanguages(isSignUp);
		}
	}

	public void downloadDefaultTags(boolean isSignUp, Collection<String> languages)
	{
		DefaultTagDownloadTask defaultTagDownloadTask = new DefaultTagDownloadTask(isSignUp, languages);
		defaultTagDownloadTask.execute();
	}

	public void refreshTagData()
	{
		long stickerTagRefreshPeriod = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_TAG_REFRESH_PERIOD,
				StickerSearchConstants.DEFAULT_STICKER_TAG_REFRESH_TIME_INTERVAL);
		long lastTagRefreshTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_STICKER_TAG_REFRESH_TIME, 0L);

		if ((System.currentTimeMillis() - lastTagRefreshTime) > stickerTagRefreshPeriod)
		{
			StickerSearchManager.getInstance().downloadStickerTags(true, StickerSearchConstants.STATE_STICKER_DATA_REFRESH,
					StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE));
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_STICKER_TAG_REFRESH_TIME, System.currentTimeMillis());
		}
	}

	public Set<Sticker> getStickerSetFromStickerStringSet(Set<String> stickerStringSet)
	{
		Set<Sticker> stickerSet = new HashSet<>(stickerStringSet.size());
		for (String info : stickerStringSet)
		{
			Pair<String, String> pair = getStickerInfoFromSetString(info);
			stickerSet.add(new Sticker(pair.first, pair.second));
		}
		return stickerSet;
	}

	public Sticker getStickerFromSetString(String info)
	{
		Pair<String, String> pair = getStickerInfoFromSetString(info);
		return new Sticker(pair.first, pair.second);
	}

    public Pair<String, String> getStickerInfoFromSetString(String info)
	{
		Pair<String, String> pair;
		if (info != null)
		{
			String[] infoString = info.split(STRING_DELIMETER);
			if (infoString.length >= INDEX_INFO_BOUND)
			{
				pair = new Pair<String, String>(infoString[INDEX_CATEGORY_ID], infoString[INDEX_STICKER_ID]);
			}
			else
			{
				pair = new Pair<String, String>(STRING_EMPTY, STRING_EMPTY);
			}
		}
		else
		{
			pair = new Pair<String, String>(STRING_EMPTY, STRING_EMPTY);
		}

		return pair;
	}

	public void saveInStickerTagSet(Sticker sticker)
	{
		Set<String> stickerSet = new HashSet<>(1);
		stickerSet.add(sticker.getStickerCode());

		StickerManager.getInstance().saveStickerSet(stickerSet, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT, false);
	}

	public void addRecentStickerToPallete(Sticker sticker)
	{
		StickerManager.getInstance().addRecentSticker(sticker);
		LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(
				new Intent(StickerManager.RECENTS_UPDATED).putExtra(StickerManager.RECENT_STICKER_SENT, (Serializable) sticker));
	}

	/**
	 * This method is to cache stickers and sticker-categories, so that their loading becomes fast on opening sticker palette the first time.
	 */
	private void cachingStickersOnStart()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnableWithDelay(new Runnable() {
			@Override
			public void run() {
				Logger.d("StickerCaching", "CachingStickersOnStart");
				cacheStickersForGivenCategory(StickerManager.RECENT);
				cacheStickerPaletteIcons();
			}
		}, 0);
	}

	/**
	 * @param categoryId
	 *            fetching recent stickers in cache.
	 */
	private void cacheStickersForGivenCategory(String categoryId)
	{
		StickerCategory category = getCategoryForId(categoryId);
		if (category == null)
		{
			return;
		}
		Logger.d("StickerCaching", "Category cached : " + categoryId);
		// loading two rows, hence *2
		int stickersToLoad = (getNumColumnsForStickerGrid(context) * 2);
		loadStickersForGivenCategory(category, stickersToLoad);
	}

	/**
	 * This method makes bitmap for each sticker in the given category and puts it in cache.
	 */
	private void loadStickersForGivenCategory(StickerCategory category, int noOfStickers)
	{
		HikeLruCache cache = HikeMessengerApp.getLruCache();
		if (cache == null)
		{
			return;
		}
		List<Sticker> stickerList = category.getStickerList();
		BitmapDrawable drawable = null;
		// Checking the lesser value out of current size of category and the size provided. This is to avoid NPE in case of smaller category size
		// as well as to make sure only the minimum required stickers are being cached.
		int stickersToLoad = Math.min(noOfStickers, stickerList.size());
		for (int i = 0; i < stickersToLoad; i++)
		{
			Sticker sticker = stickerList.get(i);
			String cacheKey = getStickerCacheKey(sticker, StickerConstants.StickerType.SMALL);
			Bitmap bitmap = HikeBitmapFactory.decodeFile(sticker.getSmallStickerPath());
			if (bitmap != null)
			{
				drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), bitmap);
				Logger.d(TAG, "Putting data in cache : " + cacheKey);
				cache.putInCache(cacheKey, drawable);
			}

		}
	}

	/**
	 * This method caches the first few sticker categories.
	 */
	private void cacheStickerPaletteIcons()
	{
		HikeLruCache cache = HikeMessengerApp.getLruCache();
		if (cache == null)
		{
			return;
		}
		List<StickerCategory> categoryList = getStickerCategoryList();

		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		// Checking the lesser value out of current size of category list and the stickers accommodated on given screen width. This is to avoid NPE in case of smaller category list
		// size as well as to make sure only the minimum required categories are being cached.
		int categoriesToLoad = Math.min(categoryList.size(), (int) (screenWidth / (context.getResources().getDimension(R.dimen.sticker_btn_width))));

		for (int i = 0; i < categoriesToLoad; i++)
		{
			String categoryId = categoryList.get(i).getCategoryId();
			Bitmap bitmap_unselected = getCategoryOtherAsset(context, categoryId, StickerManager.PALLATE_ICON_TYPE, -1, -1, true);
			if (bitmap_unselected != null)
			{
				BitmapDrawable drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), bitmap_unselected);
				String key = getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PALLATE_ICON_TYPE);
				Logger.d(TAG, "Putting data in cache : " + key);
				cache.putInCache(key, drawable);
			}

			Bitmap bitmap_selected = getCategoryOtherAsset(context, categoryId, StickerManager.PALLATE_ICON_SELECTED_TYPE, -1, -1, true);
			if (bitmap_selected != null)
			{
				BitmapDrawable drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), bitmap_selected);
				String key = getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PALLATE_ICON_SELECTED_TYPE);
				Logger.d(TAG, "Putting data in cache : " + key);
				cache.putInCache(key, drawable);
			}

		}
	}

	public void doUpgradeTasks()
	{
		if (!Utils.isUserSignedUp(HikeMessengerApp.getInstance(), false))
		{
			return;
		}

		StickerLanguagesManager.getInstance().addToLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
				Collections.singletonList(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE));
		HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE, true);
		StickerManager.getInstance().downloadStickerTagData();
		StickerManager.getInstance().downloadDefaultTagsFirstTime(false);
	}

	public void doSignupTasks()
	{
		if (!Utils.isUserSignedUp(HikeMessengerApp.getInstance(), false))
		{
			return;
		}

		StickerLanguagesManager.getInstance().addToLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
				Collections.singletonList(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE));
		HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE, false);
		StickerManager.getInstance().downloadStickerTagData();
		StickerManager.getInstance().downloadDefaultTagsFirstTime(true);
		initiateFetchCategoryRanksAndDataTask();
	}

    /**
     *
     * @param buttonType : the content ID of the button pressed
     *
     * Sticker Button Ids : HikeMessengerApp.EMOTICON_BUTTON_CLICK_ANALYTICS ;
     *                      HikeMessengerApp.STICKER_SEARCH_BUTTON_CLICK_ANALYTICS ;
     *                      HikeMessengerApp.STICKER_PALLETE_BUTTON_CLICK_ANALYTICS
     *
     */

	public void logStickerButtonsPressAnalytics(String buttonType)
	{
		int pressCount = HikeSharedPreferenceUtil.getInstance().getData(buttonType, 0);
		HikeSharedPreferenceUtil.getInstance().saveData(buttonType, ++pressCount);
	}

	public void sendStickerButtonsClickAnalytics()
	{
		String[] stickerButtons = { HikeMessengerApp.EMOTICON_BUTTON_CLICK_ANALYTICS, HikeMessengerApp.STICKER_PALLETE_BUTTON_CLICK_ANALYTICS,
				HikeMessengerApp.STICKER_SEARCH_BUTTON_CLICK_ANALYTICS };

		String[] stickerButtonAnalyticsKeys = { HikeConstants.LogEvent.EMOTICON_BTN_CLICKED, HikeConstants.LogEvent.STICKER_BTN_CLICKED,
				HikeConstants.LogEvent.STICKER_SEARCH_BTN_CLICKED };

		for (int i = 0; ((i < stickerButtonAnalyticsKeys.length) && (i < stickerButtons.length)); i++)
		{
			int pressCount = HikeSharedPreferenceUtil.getInstance().getData(stickerButtons[i], 0);
			if (pressCount <= 0)
			{
				continue;
			}
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, stickerButtonAnalyticsKeys[i]);
				metadata.put(AnalyticsConstants.CLICK_COUNT, pressCount);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);

				HikeSharedPreferenceUtil.getInstance().saveData(stickerButtons[i], 0);

			}
			catch (JSONException e)
			{
				Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
			}
		}

	}

    /**
     * JSON Structure example
     {
         "md":{
             "eD":[
                 {
                 "eName":"X-(",
                 "eCnt":11
                 },
                 {
                 "eName":":\")",
                 "eCnt":4
                 },
                 {
                 "eName":"=)",
                 "eCnt":3
                 },
                 {
                 "eName":":-P",
                 "eCnt":3
                 },
                 {
                 "eName":"^.^",
                 "eCnt":6
                 }
                ],
             "ek":"eSnt"
         }
     }
     *
     */


	public void sendEmoticonUsageAnalytics()
	{
		String emoticonsSent = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EMOTICONS_CLICKED_LIST, "");

		if (TextUtils.isEmpty(emoticonsSent) || !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOG_EMOTICON_USAGE_SWITCH, true))
		{
			return;
		}

		try
		{
			JSONObject emojiList = new JSONObject(emoticonsSent);
			Iterator<String> emoticons = emojiList.keys();

			JSONObject metadata = new JSONObject();
			JSONArray emojiUsage = new JSONArray();
			while (emoticons.hasNext())
			{
				JSONObject emoji = new JSONObject();

				String emojiName = emoticons.next();
				int count = emojiList.getInt(emojiName);

				emoji.put(HikeConstants.LogEvent.EMOTICON_NAME, emojiName);
				emoji.put(HikeConstants.LogEvent.EMOTICON_COUNT, count);

				emojiUsage.put(emoji);

			}

			if (emojiUsage.length() > 0)
			{
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.EMOTICON_SENT);
				metadata.put(HikeConstants.LogEvent.EMOTICON_DATA, emojiUsage);

				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			}

			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.EMOTICONS_CLICKED_LIST, "");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	public void logEmoticonUsageAnalytics(final String emoticon)
	{
		if (TextUtils.isEmpty(emoticon) || !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOG_EMOTICON_USAGE_SWITCH, true))
		{
			return;
		}

		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				String emoticonsSent = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EMOTICONS_CLICKED_LIST, "");

				try
				{
					JSONObject emojiList = null;

					if (TextUtils.isEmpty(emoticonsSent))
					{
						emojiList = new JSONObject();
					}
					else
					{
						emojiList = new JSONObject(emoticonsSent);
					}

					emojiList.put(emoticon, (emojiList.optInt(emoticon, 0) + 1));
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.EMOTICONS_CLICKED_LIST, emojiList.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public void sendStickerDailyAnalytics()
	{
		sendStickerError();
		sendStickerButtonsClickAnalytics();
		sendEmoticonUsageAnalytics();
		sendStickerPackAndOrderListForAnalytics();
		CategorySearchManager.sendSearchedCategoryDailyReport();
		StickerSearchManager.getInstance().sendStickerSearchDailyAnalytics();
	}

	/**
	 * Send sticker pack id and its order for analytics one time in day
	 */
	public void sendStickerPackAndOrderListForAnalytics()
	{
		try
		{
			/*
			 * TO DO Unification of all events, which needs to run only once in a day
			 */
			long currentTime = System.currentTimeMillis();

			String categoriesViewed = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.VIEWED_IN_PALLETE_CATEGORY_LIST, "");
			JSONObject catList = (TextUtils.isEmpty(categoriesViewed)) ? new JSONObject() : new JSONObject(categoriesViewed);

			List<StickerCategory> stickerCategories = getMyStickerCategoryList();

			if (Utils.isEmpty(stickerCategories))
			{
				return;
			}

			JSONArray stickerPackAndOrderList = new JSONArray();

			for (StickerCategory stickerCategory : stickerCategories)
			{
				int index;
				if (stickerCategory.isVisible())
				{
					index = stickerCategory.getCategoryIndex();
				}
				else
				{
					index = -1;
				}

				int scrollVisibilityMagnitude = 0, tapVisibilityMagnitude = 0;
				if (catList.has(stickerCategory.getCategoryId()))
				{
					JSONObject categoryVisibility = catList.getJSONObject(stickerCategory.getCategoryId());
					scrollVisibilityMagnitude = categoryVisibility.optInt(HikeConstants.SCROLL_COUNT);
					tapVisibilityMagnitude = categoryVisibility.optInt(HikeConstants.CLICK_COUNT);
				}

				stickerPackAndOrderList.put(stickerCategory.getCategoryId() + STRING_DELIMETER + index + STRING_DELIMETER + scrollVisibilityMagnitude + STRING_DELIMETER + tapVisibilityMagnitude);
			}

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.PACK_DATA_ANALYTIC_EVENT);
			metadata.put(HikeConstants.NUMBER_OF_PACKS, stickerCategories.size());
			metadata.put(HikeConstants.PACK_DATA, stickerPackAndOrderList);
			metadata.put(
					HikeConstants.KEYBOARD_LIST,
					new JSONArray(StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
							StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)));

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_STICKER_PACK_AND_ORDERING_SENT_TIME, currentTime);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.VIEWED_IN_PALLETE_CATEGORY_LIST, "");
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send recommendation panel settings button click analytics
	 */
	public void sendRecommendationPanelSettingsButtonClickAnalytics()
	{
		HAManager.getInstance().record(HikeConstants.LogEvent.STICKER_RECOMMENDATION_PANEL_SETTINGS_BTN_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT,
				EventPriority.HIGH);
	}

	/**
	 * Send recommendation settings state analytics
	 */
	public void sendRecommendationlSettingsStateAnalytics(String source, boolean state)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_MANUAL_SETTING_STATE);
			metadata.put(HikeConstants.SOURCE, source);
			metadata.put("sts", (state ? 1 : 0));

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send recommendation auto-popup settings state analytics
	 */
	public void sendRecommendationAutopopupSettingsStateAnalytics(String source, boolean state)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_AUTOPOPUP_SETTING_STATE);
			metadata.put(HikeConstants.SOURCE, source);
			metadata.put("sts", (state ? 1 : 0));

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send recommendation rejection analytics
	 */
	public void sendRecommendationRejectionAnalytics(String recommendationSource, String rejectionSource, String tappedWord, String taggedPhrase)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_REJECTION_KEY);
			metadata.put(HikeConstants.SOURCE, rejectionSource);
			metadata.put(HikeConstants.RECOMMENDATION_SOURCE, recommendationSource);
			metadata.put(HikeConstants.TAGGED_PHRASE, taggedPhrase);
			metadata.put(HikeConstants.TAP_WORD, tappedWord);
			metadata.put(HikeConstants.KEYBOARD_LIST, StickerSearchUtils.getCurrentLanguageISOCode());

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send recommendation rejection analytics from FTUE
	 */
	public void sendRecommendationRejectionAnalyticsFtue(boolean firstFtueVisible, String recommendationSource, String rejectionSource, String tappedWord, String taggedPhrase)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, (firstFtueVisible ? HikeConstants.LogEvent.STICKER_RECOMMENDATION_FTUE1_REJECTION_KEY
					: HikeConstants.LogEvent.STICKER_RECOMMENDATION_FTUE2_REJECTION_KEY));
			metadata.put(HikeConstants.RECOMMENDATION_SOURCE, recommendationSource);
			metadata.put(HikeConstants.SOURCE, rejectionSource);
			metadata.put(HikeConstants.TAGGED_PHRASE, taggedPhrase);
			metadata.put(HikeConstants.TAP_WORD, tappedWord);
			metadata.put(HikeConstants.KEYBOARD_LIST, StickerSearchUtils.getCurrentLanguageISOCode());

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send recommendation selection analytics
	 */
	public void sendRecommendationSelectionAnalytics(String source, Sticker sticker, int selectedIndex, int numTotal, int numVisible, String tappedWord, String taggedPhrase)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_SELECTION_KEY);
			metadata.put(HikeConstants.SOURCE, source);
			metadata.put(HikeConstants.ACCURACY, selectedIndex + STRING_DELIMETER + numTotal + STRING_DELIMETER + numVisible);
			metadata.put(HikeConstants.TAGGED_PHRASE, taggedPhrase);
			metadata.put(HikeConstants.TAP_WORD, tappedWord);
			metadata.put(HikeConstants.STICKER_ID, sticker.getStickerId());
			metadata.put(HikeConstants.CATEGORY_ID, sticker.getCategoryId());
			metadata.put(HikeConstants.KEYBOARD_LIST, StickerSearchUtils.getCurrentLanguageISOCode());
            metadata.put(StickerManager.STICKER_TYPE, sticker.getStickerType().ordinal());
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Send sticker search data accuracy summary analytics and return true, if analytics sent
	 */
	public boolean sendRecommendationAccuracyAnalytics(String timeStamp, Map<String, PairModified<Integer, Integer>> autoPopupClicksPerLanguageMap,
			Map<String, PairModified<Integer, Integer>> tapOnHighlightWordClicksPerLanguageMap)
	{
		try
		{
			/*
			 * TO DO Unification of all events, which needs to run only once in a day
			 */
			long lastAccuracyMatricesSentTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_RECOMMENDATION_ACCURACY_ANALYTICS_SENT_TIME, 0L);
			long currentTime = System.currentTimeMillis();

			if ((currentTime - lastAccuracyMatricesSentTime) >= 24 * 60 * 60 * 1000) // greater than one day
			{
				Set<String> languages;
				PairModified<Integer, Integer> totalAndAcceptedRecommendationCountPair;

				// Build auto-popup data for each language
				JSONObject autoPopupData;
				if ((autoPopupClicksPerLanguageMap != null) && (autoPopupClicksPerLanguageMap.size() > 0))
				{
					languages = autoPopupClicksPerLanguageMap.keySet();
					autoPopupData = new JSONObject();

					for (String languageISOCode : languages)
					{
						totalAndAcceptedRecommendationCountPair = autoPopupClicksPerLanguageMap.get(languageISOCode);
						autoPopupData.put(languageISOCode,
								totalAndAcceptedRecommendationCountPair.getFirst() + STRING_DELIMETER + totalAndAcceptedRecommendationCountPair.getSecond());
					}
				}
				else
				{
					autoPopupData = null;
				}

				// Build highlight word tapping data for each language
				JSONObject tapOnHighlightWordData;
				if ((tapOnHighlightWordClicksPerLanguageMap != null) && (tapOnHighlightWordClicksPerLanguageMap.size() > 0))
				{
					languages = tapOnHighlightWordClicksPerLanguageMap.keySet();
					tapOnHighlightWordData = new JSONObject();

					for (String languageISOCode : languages)
					{
						totalAndAcceptedRecommendationCountPair = tapOnHighlightWordClicksPerLanguageMap.get(languageISOCode);
						tapOnHighlightWordData.put(languageISOCode,
								totalAndAcceptedRecommendationCountPair.getFirst() + STRING_DELIMETER + totalAndAcceptedRecommendationCountPair.getSecond());
					}
				}
				else
				{
					tapOnHighlightWordData = null;
				}

				if ((autoPopupData != null) || (tapOnHighlightWordData != null))
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_ACCURACY_INDEX_KEY);
					metadata.put(HikeConstants.STICKER_SEARCH_EVENT_TIME_STAMP, timeStamp);
					metadata.put(HikeConstants.STICKER_SEARCH_AUTO_POPUP_DATA, autoPopupData);
					metadata.put(HikeConstants.STICKER_SEARCH_HAIGHLIGHT_WORD_DATA, tapOnHighlightWordData);

					HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.STICKER_SEARCH_BACKEND, EventPriority.HIGH, metadata);
					return true;
				}

				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LAST_RECOMMENDATION_ACCURACY_ANALYTICS_SENT_TIME, currentTime);
			}
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}

		return false;
	}

	/**
	 * Send sticker settings clicked analytics i.e. Delete, Hide, Reorder and Update selected
	 */
	public void sendStickerSettingsEventAnalytics(String event)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, event);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	/**
	 * Send sticker pack hide/unhide analytics
	 * @param catId
	 * @param visibility
	 */
	public void sendPackHideAnalytics(String catId, boolean visibility)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_PACK_HIDE);
			metadata.put(HikeConstants.CATEGORY_ID, catId);
			metadata.put(HikeConstants.PACK_VISIBILITY, visibility);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	/**
	 * Send sticker pack delete analytics
	 * @param event
	 * @param catId
	 */
	public void sendPackDeleteAnalytics(String event, String catId)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, event);
			metadata.put(HikeConstants.CATEGORY_ID, catId);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	/**
	 * Send sticker pack update analytics
	 * @param event
	 * @param catId
	 */
	public void sendPackUpdateAnalytics(String event, String catId)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, event);
			metadata.put(HikeConstants.CATEGORY_ID, catId);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	/**
	 * Send sticker pack reorder analytics
	 * @param catId
	 * @param oldPosition
	 * @param newPosition
	 */
	public void sendPackReorderAnalytics(String catId, int oldPosition, int newPosition)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_PACK_REORDERED);
			metadata.put(HikeConstants.CATEGORY_ID, catId);
			metadata.put(HikeConstants.OLD_PACK_POSITION, oldPosition);
			metadata.put(HikeConstants.NEW_PACK_POSITION, newPosition);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	/**
	 * Send sticker search data rebalancing analytics
	 */
	public void sendRebalancingAnalytics(String timeStamp, long initialDBSize, long availableMemory, int initialRowCount, int deletedRowCount)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_RECOMMENDATION_REBALANCING_SUMMERIZATION);
			metadata.put(HikeConstants.STICKER_SEARCH_EVENT_TIME_STAMP, timeStamp);
			metadata.put(HikeConstants.STICKER_SEARCH_REBALANCING_MEMORY_STATUS, initialDBSize + STRING_DELIMETER + availableMemory);
			metadata.put(HikeConstants.STICKER_SEARCH_REBALANCING_ROW_STATUS, initialRowCount + STRING_DELIMETER + deletedRowCount);

			HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.STICKER_SEARCH_BACKEND, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}

	/**
	 * Used for logging sticker/emoticon weird behaviors
	 * 
	 * @param errorMsg
	 */
	public void sendStickerFolderLockedError(String errorMsg)
	{

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_FOLDER_ERROR);
			metadata.put(TAG, errorMsg);
			HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.STICKER_SEARCH, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public Set<String> getStickerSet(int state)
	{
		switch (state)
		{
		case StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT:
			return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, new HashSet<String>());

		case StickerSearchConstants.STATE_STICKER_DATA_REFRESH:
			return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_REFRESH_SET, new HashSet<String>());

		case StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD:
			return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET_FOR_LANGUAGE, new HashSet<String>());

		case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
			return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET_FORCED_SET, new HashSet<String>());

		default:
			return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, new HashSet<String>());
		}
	}

	public void saveStickerSet(Set<String> stickerSet, int state, boolean forceReplace)
	{

		if (!forceReplace)
		{
			Logger.d(TAG, "sticker set to insert : " + stickerSet);
			Logger.d(TAG, "current sticker set : " + StickerManager.getInstance().getStickerSet(state));

			stickerSet.addAll(StickerManager.getInstance().getStickerSet(state));

			Logger.d(TAG, "sticker set after new set insert: " + stickerSet);
		}

		switch (state)
		{
		case StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT:
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, stickerSet);
			break;

		case StickerSearchConstants.STATE_STICKER_DATA_REFRESH:
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_REFRESH_SET, stickerSet);
			break;

		case StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD:
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET_FOR_LANGUAGE, stickerSet);
			break;

		case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET_FORCED_SET, stickerSet);
			break;

		}
	}

	public void removeStickerSet(int state)
	{
		switch (state)
		{
		case StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.STICKER_SET);
			break;

		case StickerSearchConstants.STATE_STICKER_DATA_REFRESH:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.STICKER_REFRESH_SET);
			break;

		case StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.STICKER_SET_FOR_LANGUAGE);
			break;

		case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
			HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.STICKER_SET_FORCED_SET);
			break;
		}
	}

	public void toggleStickerRecommendation(boolean state)
	{
		Logger.d(TAG, "Toggling SR enable status to " + state);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, state);
	}

	public void resetSignupUpgradeCallPreference()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, false);
	}

	public List<Sticker> getForcedRecentsStickers()
	{
		Set<String> input = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeConstants.FORCED_RECENTS_LIST, null);

		ArrayList<Sticker> resultSet = new ArrayList<Sticker>();

		if (Utils.isEmpty(input))
		{
			return null;
		}

		int length = input.size();

		Iterator<String> resultsStickers = input.iterator();

		try
		{
			while (resultsStickers.hasNext())
			{

				JSONObject recentSticker = new JSONObject(resultsStickers.next());

				long startDate = recentSticker.getLong(HikeConstants.START);
				long endDate = recentSticker.getLong(HikeConstants.END);

				if (System.currentTimeMillis() > startDate && System.currentTimeMillis() < endDate)
				{
					Sticker temp = new Sticker(recentSticker.getString(HikeConstants.CATEGORY_ID), recentSticker.getString(HikeConstants.STICKER_ID));
					if(temp.isStickerAvailable())
					{
						int rank = recentSticker.getInt(HikeConstants.RANK);
						resultSet.ensureCapacity(rank);
						resultSet.add(rank-1,temp);
					}
				}
				else if(System.currentTimeMillis() > endDate)
				{
					resultsStickers.remove();
				}

			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (input.size() <= 0)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FORCED_RECENTS_PRESENT, false);
		}

		HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeConstants.FORCED_RECENTS_LIST, input);

		for(int i =resultSet.size()-1;i>=0;i--)
		{
			if(resultSet.get(i) == null)
			{
				resultSet.remove(i);
			}
		}

		return resultSet;
	}

	public Set<String> getStickerSetFromList(List<Sticker> stickerList)
	{
		Set<String> stickerSet = new HashSet<>();
		if (!Utils.isEmpty(stickerList))
		{
			for (Sticker sticker : stickerList)
			{
				stickerSet.add(sticker.getStickerCode());
			}
		}
		return stickerSet;
	}

	public List<Sticker> getStickerListFromString(String categoryId, String stickerListString)
	{
		List<Sticker> stickerList = null;
		try
		{
			if (!TextUtils.isEmpty(stickerListString))
			{
				JSONArray stickerListJSON = new JSONArray(stickerListString);

				int length = stickerListJSON.length();

				stickerList = new ArrayList<>(length);
				for (int i = 0; i < length; i++)
				{
					Sticker sticker = new Sticker(categoryId, stickerListJSON.getString(i));
					stickerList.add(sticker);
				}
			}
		}
		catch (JSONException ex)
		{
			Logger.e(TAG, "exception while making sticker from sticker list json ", ex);
		}

		return stickerList;
	}

	public List<StickerCategory> getSimilarPacksFromString(String similarPacksString)
	{
		List<StickerCategory> similarPacks = null;
		try
		{
			if (!TextUtils.isEmpty(similarPacksString))
			{
				JSONArray similarPacksJson = new JSONArray(similarPacksString);

				int length = similarPacksJson.length();

				similarPacks = new ArrayList<>(length);
				for (int i = 0; i < length; i++)
				{
					StickerCategory stickerCategory =  parseStickerCategoryMetadata(similarPacksJson.getJSONObject(i));
					if(stickerCategory != null)
					{
						similarPacks.add(stickerCategory);
					}
				}
			}
		}
		catch (JSONException ex)
		{
			Logger.e(TAG, "exception while making sticker from sticker list json ", ex);
		}

		return similarPacks;
	}

	public boolean isMiniStickersEnabled()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MINI_STICKER_ENABLED, true);
	}

    public boolean isShopSearchEnabled()
    {
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, false);
    }

	public void saveSticker(Sticker sticker, StickerConstants.StickerType stickerType)
	{
		List<Sticker> stickers = new ArrayList<Sticker>(1);
		stickers.add(sticker);
		saveSticker(stickers, stickerType);
	}

	public void saveSticker(List<Sticker> stickers, StickerConstants.StickerType stickerType)
	{
		HikeConversationsDatabase.getInstance().insertStickersToDB(stickers, stickerType);

        for(Sticker sticker : stickers)
        {
            removeFromTableStickerSet(sticker);
        }
	}

	public void deactivateSticker(Sticker sticker)
	{
		List<Sticker> stickers = new ArrayList<Sticker>(1);
		stickers.add(sticker);
		deactivateSticker(stickers);
	}

	public void deactivateSticker(List<Sticker> stickers)
	{
		HikeConversationsDatabase.getInstance().deactivateStickerFromDB(stickers);
	}

    public void deactivateStickerForCategory(StickerCategory category)
    {
        List<StickerCategory> categories = new ArrayList<StickerCategory>(1);
        categories.add(category);
        deactivateStickerForCategory(categories);
    }

    public void deactivateStickerForCategory(List<StickerCategory> categories)
    {
        HikeConversationsDatabase.getInstance().deactivateStickersForCategories(categories);
    }

    public void deleteStickerForCategory(StickerCategory category)
    {
        List<StickerCategory> categories = new ArrayList<StickerCategory>(1);
        categories.add(category);
        deleteStickerForCategory(categories);
    }

    public void deleteStickerForCategory(List<StickerCategory> categories)
    {
        HikeConversationsDatabase.getInstance().deleteStickersForCategories(categories);
    }

	public boolean shouldDisplayMiniStickerOnChatThread()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DISPLAY_MINI_IN_CT, false) && isMiniStickersEnabled();
	}

    /**
     *
     * @param sticker object for which the mini sticker key has to be generated
     * @return A sticker based unique code that contains only alphanumeric characters separated by '_' (as per key name format constraints of disk cache)
     */
    public String generateMiniStickerPath(Sticker sticker)
    {
        String miniStickerPath = HikeConstants.MINI_KEY_PREFIX;

        String code = sticker.getStickerCode();

        int keyLength = code.length()>HikeConstants.MAX_DISK_CACHE_KEY_LENGTH?HikeConstants.MAX_DISK_CACHE_KEY_LENGTH:code.length();

        String key = code.toLowerCase().substring(0, keyLength).replaceAll("[^a-z0-9_-]", "");

        miniStickerPath += key;

        sticker.setMiniStickerPath(miniStickerPath);

        return miniStickerPath;
    }

	public void saveStickerSetFromJSON(JSONObject stickers, String categoryId) throws JSONException
	{
		List<Sticker> stickerList = new ArrayList<>();

		for (Iterator<String> keys = stickers.keys(); keys.hasNext();)
		{
			String stickerId = keys.next();
			JSONObject stickerData = stickers.getJSONObject(stickerId);

			Sticker sticker = new Sticker(categoryId, stickerId);
			sticker.setWidth(stickerData.optInt(HikeConstants.WIDTH));
			sticker.setHeight(stickerData.optInt(HikeConstants.HEIGHT));
			sticker.setLargeStickerPath(sticker.getLargeStickerFilePath());
			sticker.setSmallStickerPath(sticker.getSmallStickerFilePath());
			stickerList.add(sticker);
		}

		saveSticker(stickerList, StickerConstants.StickerType.LARGE);
	}

    public void saveMiniStickerSetFromJSON(JSONObject stickers, String categoryId) throws JSONException
    {
        List<Sticker> stickerList = new ArrayList<>();

        for (Iterator<String> keys = stickers.keys(); keys.hasNext();)
        {
            String stickerId = keys.next();
            JSONObject stickerData = stickers.getJSONObject(stickerId);

            Sticker sticker = new Sticker(categoryId, stickerId);

            if(sticker.isStickerAvailable())
            {
                continue;
            }

            sticker.setWidth(stickerData.optInt(HikeConstants.WIDTH));
            sticker.setHeight(stickerData.optInt(HikeConstants.HEIGHT));
            stickerList.add(sticker);
        }

        saveSticker(stickerList, StickerConstants.StickerType.MINI);
    }

    public String getStickerCacheKey(Sticker sticker, StickerConstants.StickerType stickerType)
    {
        String path = sticker.getStickerCode();
        switch (stickerType)
        {
            case MINI:
                path += HikeConstants.DELIMETER + sticker.getMiniStickerPath();
                break;
            case SMALL:
                path += HikeConstants.DELIMETER + sticker.getSmallStickerPath();
                break;
            case LARGE:
                path += HikeConstants.DELIMETER + sticker.getLargeStickerPath();
                break;
        }

        return path;

    }

	public static int getStickerSize()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_SIZE, -1) != -1)
		{
			return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_SIZE, -1);
		}
		else
		{
			switch (Utils.getResolutionId())
			{
			case HikeConstants.XXXHDPI_ID:
			case HikeConstants.XXHDPI_ID:
				return 540;
			case HikeConstants.XHDPI_ID:
				return 356;
			case HikeConstants.HDPI_ID:
				return 264;
			case HikeConstants.MDPI_ID:
				return 157;
			case HikeConstants.LDPI_ID:
				return 120;
			default:
				return 356;
			}
		}
	}

	public boolean isStickerFolderError()
	{
		if(TextUtils.isEmpty(stickerExternalDir))
		{
			stickerExternalDir = getStickerExternalDirFilePath();
		}

		return TextUtils.isEmpty(stickerExternalDir);
	}

	private void sendStickerError()
	{
		sendStickerFolderError();
		sendSingleStickerDownloadError();
		sendStickerPackDownloadError();
	}

	private void logStickerFolderError()
	{
		if(isStickerFolderError())
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_FOLDER_LOCKED_ERROR_OCCURED, true);
		}
	}

	private void sendStickerFolderError()
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_FOLDER_LOCKED_ERROR_OCCURED, false))
		{
			sendStickerFolderLockedError("Unable to access android folder.");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_FOLDER_LOCKED_ERROR_OCCURED, false);
		}
	}

	private void sendSingleStickerDownloadError()
	{
		int count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SINGLE_STICKER_DOWNLOAD_ERROR_COUNT, 0);
		if(count > 0)
		{
			sendStickerDownloadErrors(HikeConstants.SINGLE_STICKER, count);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SINGLE_STICKER_DOWNLOAD_ERROR_COUNT, 0);
		}
	}


	private void sendStickerPackDownloadError()
	{
		int count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_PACK_DOWNLOAD_ERROR_COUNT, 0);
		if(count > 0)
		{
			sendStickerDownloadErrors(HikeConstants.STICKER_PACK, count);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_PACK_DOWNLOAD_ERROR_COUNT, 0);
		}
	}

	private void sendStickerDownloadErrors(String eventType, int count)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_ERROR);
			metadata.put(HikeConstants.EVENT_TYPE, eventType);
			metadata.put(HikeConstants.COUNT, count);

			HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json", e);
		}
	}


	public void logStickerDownloadError(String type)
	{
		switch (type)
		{
			case HikeConstants.SINGLE_STICKER:
				int count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SINGLE_STICKER_DOWNLOAD_ERROR_COUNT, 0);
				count +=1;
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SINGLE_STICKER_DOWNLOAD_ERROR_COUNT, count);
				break;
			case HikeConstants.STICKER_PACK:
				count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_PACK_DOWNLOAD_ERROR_COUNT, 0);
				count +=1;
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_PACK_DOWNLOAD_ERROR_COUNT, count);
				break;
		}
	}

	public void setShowLastCategory(boolean showLastCategory)
	{
		this.showLastCategory = showLastCategory;
	}
	
	public void resetStickerTablesToDefault()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();

		mThread.postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1(); // This prepopulates the Categories Table
				moveStickerPreviewAssetsToSdcard(); // This is a heavy operation and hence needs to be done on the BG Thread.
				HikeConversationsDatabase.getInstance().upgradeForStickerTable();// This prepopulates the Stickers Table
                HikeConversationsDatabase.getInstance().markAllCategoriesAsDownloaded();
                setupStickerCategoryList();			// Set up the in-memory list so that the pallete can function		 +				HikeConversationsDatabase.getInstance().upgradeForStickerTable();
                resetSignupUpgradeCallPreference(); // This is needed to make a call to the server to fetch categories in order of user's region/location
			}
		}, 0);

    }

    public void postRestoreSetup()
    {
		// Check if we are restoring sticker backup from a non sticker backup compatible version
		BackupMetadata metadata = BackupUtils.getBackupMetadata();
		if (metadata != null)
		{
			int oldBackupVersion = metadata.getAppVersion();

			if (oldBackupVersion <= AccountBackupRestore.STICKER_BACKUP_THRESHHOLD_VERSION)
			{
				HikeConversationsDatabase.getInstance().clearTable(DBConstants.STICKER_TABLE);
			}

			if (oldBackupVersion < AccountBackupRestore.STICKER_CATEGORY_TABLE_UPDATE_VERSION)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.UPGRADE_STICKER_CATEGORIES_TABLE, false); // Need to mark already downloaded categories as downloaded
			}
		}

		HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1);
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BackupRestore.KEY_MOVED_STICKER_EXTERNAL, false); // Need to reset sticker tables again
		HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, true); // No need to fetch cat metadata again since we have restored old categories
		// Download Tags for whatever stickers are present now
		Set<String> stickersSet = new HashSet<>();
		for (Sticker s : getAllStickers())
		{
			stickersSet.add(s.getStickerCode());
		}
		StickerSearchManager.getInstance().downloadStickerTags(true, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT, stickersSet, StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE));
		
		handleDifferentDpi();
    }

	public boolean getShowLastCategory()
	{
		return showLastCategory;
	}

	private void refreshPacksData()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATED_ALL_CATEGORIES_METADATA, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATED_ALL_CATEGORIES_TAGDATA, false))
		{
			Logger.v(TAG, "already updated all categories data true");
			return;
		}
		StickerCategoryDataUpdateTask stickerCategoryDataUpdateTask = new StickerCategoryDataUpdateTask();
		HikeHandlerUtil.getInstance().postRunnable(stickerCategoryDataUpdateTask);
	}

	/**
	 *	Migrate sticker assets to different directory
	 *
	 * 1. Move sticker assets
	 *
	 * 2. Update sticker file path in sticker table (Note: New path updated to directory returned by getStickerExternalDirFilePath}
	 */
	public boolean migrateStickerAssets(String fromPath, String toPath)
	{
		if (isStickerFolderError())
		{
			recordStickerMigrationFailure("Got Sticker Folder error! Failed to migrate stickers");
			return false;
		}

		int oldCount = TextUtils.isEmpty(fromPath) ? 0 : Utils.getFilesCountRecursive(new File(fromPath));

		boolean isMoved = moveStickersFolder(fromPath, toPath);

		if (isMoved)
		{
			// Assets migrated successfully
			// Update stickers path
			stickerExternalDir = HikeConstants.HIKE_DIRECTORY_ROOT + HikeConstants.STICKERS_ROOT; // We need to re-init this path to the new path now

			int newCount = TextUtils.isEmpty(toPath) ? 0 : Utils.getFilesCountRecursive(new File(toPath));

			Logger.d("StickerMigration", " Old Count : " + oldCount + " New Count : " + newCount);

			if (HikeConversationsDatabase.getInstance().upgradeForStickerTable())
			{
				recordStickerMigrationSuccess("Stickers Successfully Moved. Old Count : " + oldCount + " New Count : " + newCount);
				doInitialSetup();
				return true;
			}
			else
			{
				recordStickerMigrationFailure("failed to upgrade for sticker table. Old Count : " + oldCount + " New Count : " + newCount);
				return false;
			}
		}
		else
		{
			recordStickerMigrationFailure("failed to move stickers");
		}

		return isMoved;
	}

	/**
	 * Move stickers from 0/Android/data/com.bsb.hike/stickers to 0/Hike/stickers
	 * 
	 * @param fromPath
	 * @param toPath
	 * @return true, is the operation was successful
	 */
	private boolean moveStickersFolder(String fromPath, String toPath)
	{
		if (!TextUtils.isEmpty(fromPath) && !TextUtils.isEmpty(toPath))// Paths are not null
		{
			// Copy to! We do not need to check size since we are merely renaming file paths on same mount

			return Utils.moveDirectoryByRename(new File(fromPath), new File(toPath));
		}
		else
		{
			recordStickerMigrationFailure("Either fromPath is null or toPath is null ");
			return false;
		}
	}

	public void handleDifferentDpi()
	{
		if (BackupUtils.isDeviceDpiDifferent())
		{
			// Genuine case of device change or alteration in densityDPI value
			// 1. Flush Sticker Table
			// 2. Remove Sticker Assets. They are no longer useful
			HikeConversationsDatabase.getInstance().clearTable(DBConstants.STICKER_TABLE);
			deleteStickers();
		}
	}

	public JSONArray getAllCategoriesFromDbAsJsonArray()
	{
		JSONArray jsonArray = new JSONArray();

		List<StickerCategory> catList = HikeConversationsDatabase.getInstance().getAllStickerCategories();

		if (!Utils.isEmpty(catList))
		{

			for (StickerCategory category : catList)
			{
				String categoryId = category.getCategoryId();
				if (!category.isCustom())
				{
					jsonArray.put(categoryId);
				}
			}
		}

		return jsonArray;
	}

    public void saveInTableStickerSet(Sticker sticker)
    {
        Set<String> stickerSet = HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.STICKER_DOWNLOAD_ATTEMPTED_SET,new HashSet<String>());
        stickerSet.add(sticker.getStickerCode());
        HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.STICKER_DOWNLOAD_ATTEMPTED_SET, stickerSet);
    }

    public void removeFromTableStickerSet(Sticker sticker)
    {
        Set<String> stickerSet = HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.STICKER_DOWNLOAD_ATTEMPTED_SET,new HashSet<String>());
        stickerSet.remove(sticker.getStickerCode());
        HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.STICKER_DOWNLOAD_ATTEMPTED_SET, stickerSet);
    }

    public void retryInsertForStickers()
    {
        Set<String> stickerSet = HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.STICKER_DOWNLOAD_ATTEMPTED_SET,new HashSet<String>());

        if(Utils.isEmpty(stickerSet))
        {
            return;
        }

        List<Sticker> stickers = new ArrayList<>(stickerSet.size());

        for(String stickerCode : stickerSet)
        {
            stickers.add(getStickerFromSetString(stickerCode));
        }

        saveSticker(stickers, StickerConstants.StickerType.LARGE);

    }


	public void sendPackPreviewOpenAnalytics(String categoryId, int position, String packPreviewClickSource, String packPreviewClickSearchKey)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.PACK_PREVIEW);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.PACK_PREVIEW);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.SPECIES, categoryId);
			json.put(AnalyticsConstants.V2.SOURCE, packPreviewClickSource);
			json.put(AnalyticsConstants.V2.VAL_STR, packPreviewClickSearchKey);
			json.put(AnalyticsConstants.V2.VAL_INT, position);
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in logging analytics for pack preview open");
		}
	}

	public void sendViewAllClickAnalytics(String categoryId)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.VIEW_ALL);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.VIEW_ALL);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.SPECIES, categoryId);
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in logging analytics for pack preview open");
		}
	}

	public void sendResponseTimeAnalytics(Response response, String methodType, String categoryId, String stickerId)
	{
		try
		{
			if (response == null || TextUtils.isEmpty(response.getUrl()) || Utils.isEmpty(response.getHeaders())
					|| !HttpUtils.containsHeader(response.getHeaders(), HttpHeaderConstants.NETWORK_TIME))
			{
				return;
			}

			long timeTaken = TimeUnit.NANOSECONDS.toMillis(Long.valueOf(HttpUtils.getHeader(response.getHeaders(), HttpHeaderConstants.NETWORK_TIME).getValue()));


			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.STICKER_DOWNLOAD_TIME);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.HTTP_EVENT);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.NON_UI_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, methodType);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.VARIETY, timeTaken);
			json.put(AnalyticsConstants.V2.FORM, AnalyticsConstants.HTTP_EVENT);
			json.put(AnalyticsConstants.V2.VAL_STR, ContactManager.getInstance().getSelfMsisdn());
			json.put(AnalyticsConstants.V2.NETWORK, Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext()));
			json.put(AnalyticsConstants.V2.SPECIES, categoryId);
			json.put(AnalyticsConstants.V2.GENUS, stickerId);
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "json exception in logging sticker response time", e);
		}
	}


	public JSONObject getPackDownloadBodyJson(DownloadSource downloadSource)
	{
		return getPackDownloadBodyJson(downloadSource, -1, false);
	}

	public JSONObject getPackDownloadBodyJson(DownloadSource downloadSource, int position, boolean viewAllClicked)
	{
		try {

			JSONObject body = new JSONObject();
			body.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
			body.put(HikeConstants.DOWNLOAD_SOURCE, downloadSource.getValue());
			body.put(HikeConstants.POSITION, position);
			body.put(HikeConstants.VIEW_ALL_CLICKED, viewAllClicked ? 1 : 0);

			return body;
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "error in making body for pack download");
		}

		return null;
	}

    public void updateStickerShopSearchAllowedStatus()
    {
        if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATED_ALL_CATEGORIES_METADATA, false)
                && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATED_ALL_CATEGORIES_TAGDATA, false))
        {
            Logger.v(TAG, "Sticker Search marked allowed");
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_ALLOWED, true);
        }
    }

	/***
	 *
	 * @param catId
	 * @return
	 *
	 *         This function can return null if file doesnot exist.
	 */
	public Set<Sticker> getRecentStickersFromFile()
	{
		Set<Sticker> list = null;
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		String catId = RECENT;
		String dirPath = StickerManager.getInstance().getInternalStickerDirectoryForCategoryId(catId);

		try
		{
			long t1 = System.currentTimeMillis();
			Logger.d(TAG, "Calling function get sorted list for category : " + catId);
			File dir = new File(dirPath);
			if (!dir.exists())
			{
				dir.mkdirs();
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(RECENT_STICKERS_COUNT));
			}
			File catFile = new File(dirPath, catId + ".bin");
			if (!catFile.exists())
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(RECENT_STICKERS_COUNT));
			fileIn = new FileInputStream(catFile);
			in = new ObjectInputStream(fileIn);
			int size = in.readInt();
			list = Collections.synchronizedSet(new LinkedHashSet<Sticker>(size));
			for (int i = 0; i < size; i++)
			{
				try
				{
					Sticker s = new Sticker();
					s.deSerializeObj(in);
					File f = new File(s.getSmallStickerPath());
					if(f.exists())
					{
						list.add(s);
					}
				}
				catch (Exception e)
				{
					Logger.e(TAG, "Exception while deserializing sticker", e);
				}
			}
			long t2 = System.currentTimeMillis();
			Logger.d(TAG, "Time in ms to get sticker list of category : " + catId + " from file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception while reading category file.", e);
			list = Collections.synchronizedSet(new LinkedHashSet<Sticker>(RECENT_STICKERS_COUNT));
		}
		finally
		{
			Utils.closeStreams(in, fileIn);
		}
		return list;
	}

	public boolean migrateRecent()
	{
		Set<Sticker> recentStickers = getRecentStickersFromFile();
		HikeConversationsDatabase.getInstance().saveRecentStickers(recentStickers);
		refreshRecents();
		try
		{
			Utils.delete(new File(StickerManager.getInstance().getInternalStickerDirectoryForCategoryId(RECENT)));
		}
		catch(IOException e)
		{
			Logger.e(TAG, "exception in deleting recents file");
		}
		return true;
	}

	public void refreshRecents()
	{
		StickerCategory stickerCategory = StickerManager.getInstance().getCategoryForId(StickerManager.RECENT);
		if(stickerCategory != null && stickerCategory instanceof CustomStickerCategory)
		{
			((CustomStickerCategory) stickerCategory).loadStickers();
		}
	}

	public void markAllCategoriesAsDownloaded()
	{
		HikeConversationsDatabase.getInstance().markAllCategoriesAsDownloaded();
		setupStickerCategoryList(); //Set up the sticker category list again
	}

	public void executeFetchShopPackTask(int limit)
	{
		FetchShopPackDownloadTask fetchShopPackDownloadTask = new FetchShopPackDownloadTask(limit);
		fetchShopPackDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void recordStickerMigrationFailure(String errorString)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, "backup");
			json.put(AnalyticsConstants.V2.KINGDOM, "act_hs");
			json.put(AnalyticsConstants.V2.ORDER, "stk_rstr_error");
			if (!TextUtils.isEmpty(errorString))
			{
				json.put(AnalyticsConstants.V2.GENUS, errorString);
			}
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public void fetchQuickSuggestionForAllStickers()
	{
		if (!QuickStickerSuggestionController.getInstance().isQuickSuggestionEnabled())
		{
			return;
		}
		HikeHandlerUtil.getInstance().postRunnable(new FetchForAllStickerQuickSuggestionTask());
	}

	public boolean isRecentCategory(String categotyId)
	{
		return (TextUtils.isEmpty(categotyId) || !categotyId.equalsIgnoreCase(RECENT)) ? false : true;
	}

	public boolean isQuickSuggestionCategory(String categotyId)
	{
		return (TextUtils.isEmpty(categotyId) || !categotyId.equalsIgnoreCase(QUICK_SUGGESTIONS)) ? false : true;
	}

	public void sendStickerClickedLogs(final ConvMessage convMessage, final int type)
	{
        if(convMessage == null)
        {
            return;
        }

		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
                Sticker sticker = convMessage.getMetadata().getSticker();
                boolean isSent = convMessage.isSent();


                try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(AnalyticsConstants.V2.GENUS, sticker.getCategoryId());
					metadata.put(AnalyticsConstants.V2.SPECIES, sticker.getStickerId());
					metadata.put(AnalyticsConstants.V2.SOURCE, convMessage.isOneToNChat() ? HikeConstants.GROUP_CONVERSATION : HikeConstants.ONE_TO_ONE_CONVERSATION);
					metadata.put(AnalyticsConstants.V2.FROM_USER, convMessage.getSenderMsisdn());
					metadata.put(AnalyticsConstants.V2.TO_USER, convMessage.getMsisdn());
					metadata.put(AnalyticsConstants.TYPE, type);
					metadata.put(AnalyticsConstants.V2.FORM, isSent);
					sendStickerClickedLogs(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "sendStickerClickedLogs() : Exception while parsing JSON");
				}
			}
		});
	}

	public void sendStickerClickedLogs(final JSONObject clickedStickerMetadata)
	{
		if (clickedStickerMetadata == null)
		{
			return;
		}

		try
		{
			clickedStickerMetadata.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_STICKER_LOGS);
			clickedStickerMetadata.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			clickedStickerMetadata.put(AnalyticsConstants.V2.UNIQUE_KEY, HikeConstants.LogEvent.STICKER_CLICKED);
			clickedStickerMetadata.put(AnalyticsConstants.V2.ORDER, HikeConstants.LogEvent.STICKER_CLICKED);
			clickedStickerMetadata.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			HAManager.getInstance().recordV2(clickedStickerMetadata);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "sendStickerClickedLogs() : Exception while parsing JSON");
		}
	}

	public void logCategoryPalleteVisibilityAnalytics(final StickerCategory category, final boolean selectedByTap)
	{
		if (category == null)
		{
			return;
		}

		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				String categoriesViewed = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.VIEWED_IN_PALLETE_CATEGORY_LIST, "");

				try
				{
					JSONObject catList = (TextUtils.isEmpty(categoriesViewed)) ? new JSONObject() : new JSONObject(categoriesViewed);
					JSONObject categoryVisibility = catList.has(category.getCategoryId()) ? catList.getJSONObject(category.getCategoryId()) : new JSONObject();

					String visibilitySrc = selectedByTap ? HikeConstants.CLICK_COUNT : HikeConstants.SCROLL_COUNT;

					categoryVisibility.put(visibilitySrc, (categoryVisibility.optInt(visibilitySrc, 0) + 1));
					catList.put(category.getCategoryId(), categoryVisibility);

					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.VIEWED_IN_PALLETE_CATEGORY_LIST, catList.toString());
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "sendStickerClickedLogs() : Exception while parsing JSON");
				}
			}
		});
	}

	public void recordStickerMigrationSuccess(String successString)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, "backup");
			json.put(AnalyticsConstants.V2.KINGDOM, "act_hs");
			json.put(AnalyticsConstants.V2.ORDER, "stk_rstr_success");
			if (!TextUtils.isEmpty(successString))
			{
				json.put(AnalyticsConstants.V2.GENUS, successString);
			}
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void addQuickSuggestionCategoryToMap(StickerCategory stickerCategory)
	{
		stickerCategoriesMap.put(stickerCategory.getCategoryId(), stickerCategory);
	}

	public void removeQuickSuggestionCategoryFromMap(String catId)
	{
		stickerCategoriesMap.remove(catId);
	}

	public void makeCallForUserParameters()
	{
		long lastUserParametersFetchTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_USER_PARAMETER_FETCH_TIME, 0L);
		long lastParameterMappingFetchTime = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LAST_PARAMETER_MAPPING_FETCH_TIME, 0L);
		long refreshPeriod = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.USER_PARAMTER_REFRESH_PERIOD, 7 * HikeConstants.ONE_DAY_MILLS);

		if((System.currentTimeMillis() - lastUserParametersFetchTime) >= refreshPeriod)
		{
			initiateUserParameterDownloadTask();
		}

		if((System.currentTimeMillis() - lastParameterMappingFetchTime) >= refreshPeriod)
		{
			initiateParameterMappingDownloadTask();
		}
	}

	public void initiateUserParameterDownloadTask()
	{
		UserParameterDownloadTask userParameterDownloadTask = new UserParameterDownloadTask();
		userParameterDownloadTask.execute();
	}

	public void initiateParameterMappingDownloadTask()
	{
		ParameterMappingDownloadTask parameterMappingDownloadTask = new ParameterMappingDownloadTask();
		parameterMappingDownloadTask.execute();
	}
}
