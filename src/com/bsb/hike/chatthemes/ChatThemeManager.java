package com.bsb.hike.chatthemes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeManager
{
	private static ChatThemeManager mInstance;

	// Helps the manager class with all the asset maintainance
	private ChatThemeAssetHelper mAssetHelper;

	// Helps the manager with all the Drawable assets
	private ChatThemeDrawableHelper mDrawableHelper;

	// Maintains the Map of Chatthemes
	private HashMap<String, HikeChatTheme> mChatThemesList;

	private ChatThemeManager()
	{
		initialize();
	}

	public static ChatThemeManager getInstance()
	{
		if (mInstance == null)
		{
			synchronized (ChatThemeManager.class)
			{
				if (mInstance == null)
				{
					mInstance = new ChatThemeManager();
				}
			}
		}
		return mInstance;
	}

	private void initialize()
	{
		mChatThemesList = new HashMap<>();
		mDrawableHelper = new ChatThemeDrawableHelper();
		mAssetHelper = new ChatThemeAssetHelper();
	}

	public ChatThemeAssetHelper getAssetHelper()
	{
		return mAssetHelper;
	}

	public ChatThemeDrawableHelper getDrawableHelper()
	{
		return mDrawableHelper;
	}

	private HikeChatTheme getTheme(String themeId)
	{
		return mChatThemesList.get(themeId);
	}

	/**
	 * Checks if all the assets for this is theme are available or not
	 *
	 * @param themeID
	 *            theme id
	 * @return boolean
	 *
	 */
	public boolean isThemeAvailable(String themeId)
	{
		HikeChatTheme theme = getTheme(themeId);
		return mAssetHelper.isAssetsAvailableForTheme(theme);
	}

	public String[] getMissingAssetsForTheme(String themeId)
	{
		return mAssetHelper.getMissingAssets(getTheme(themeId).getAssets());
	}

	//MQTT Signal packet processing
	public void processNewThemeSignal(JSONArray data)
	{
		try
		{
			ArrayList<HikeChatTheme> themeList = new ArrayList<>();
			ArrayList<HikeChatThemeAsset> assetsList = new ArrayList<>();
			HashSet<String> assetIds = new HashSet<>();

			int len = data.length();
			//looping of the n no themes sent in the packet
			for (int i = 0; i < len; i++) {
				HikeChatTheme theme = new HikeChatTheme();
				JSONObject t = data.getJSONObject(i);

				String themeID = t.getString(HikeChatThemeConstants.JSON_SIGNAL_THEME_THEMEID);
				theme.setThemeId(themeID);

				// looping to the no of indexes for a theme
				for (byte j = 0; j < HikeChatThemeConstants.ASSET_INDEX_COUNT; j++) {
					JSONObject assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME[j]);
					int type = assetObj.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
					String id = assetObj.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE);
					theme.setAsset(j, id);
					if (!assetIds.contains(id)) {
						assetsList.add(new HikeChatThemeAsset(id, type, null));
					}
				}
				themeList.add(theme);
			}
			HikeConversationsDatabase.getInstance().saveChatThemes(themeList);
			HikeConversationsDatabase.getInstance().saveChatThemeAssets(assetsList);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void processDeleteThemeSignal(JSONObject data)
	{

	}

}
