package com.bsb.hike.chatthemes;

import java.util.ArrayList;
import java.util.HashMap;

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

			int len = data.length();
			for (int i = 0; i < len; i++)
			{
				HikeChatTheme theme = new HikeChatTheme();
				JSONObject t = data.getJSONObject(i);

				String themeID = t.getString(HikeChatThemeConstants.JSON_SIGNAL_THEME_THEMEID);
				theme.setThemeId(themeID);

				JSONObject assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_PORTRAIT);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_BG_PORTRAIT, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_LANDSCAPE);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_BG_LANDSCAPE, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_ACTION_BAR);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_ACTION_BAR_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_CHAT_BUBBLE_BG);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_CHAT_BUBBLE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_SENT_NUDGE);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_SENT_NUDGE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_RECEIVE_NUDGE);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_RECEIVED_NUDGE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_INLINE_STATUS_BG);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_INLINE_STATUS_MSG_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_MULTI_SELECT_BUBBLE);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_OFFLINE_MSG_BG);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_OFFLINE_MESSAGE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_STATUS_BAR_BG);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_SMS_TOGGLE_BG);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_SMS_TOGGLE_BG, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_BUBBLE_COLOR);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_BUBBLE_COLOR, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_STATUS_BAR_COLOR);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_COLOR, theme, assetsList);

				assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME_THUMBNAIL);
				parseSignalIntoChatThemeObjects(assetObj, HikeChatThemeConstants.ASSET_INDEX_THUMBNAIL, theme, assetsList);


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

	private void parseSignalIntoChatThemeObjects(JSONObject assetObj, byte assetIndex, HikeChatTheme theme, ArrayList<HikeChatThemeAsset> assetList){
		try
		{
			int type = assetObj.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
			String id = assetObj.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE);
			theme.setAsset(assetIndex, id);
			assetList.add(new HikeChatThemeAsset(id, type, null));
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
