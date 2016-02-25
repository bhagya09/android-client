package com.bsb.hike.chatthemes;

import java.util.HashSet;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeAssetHelper implements HikePubSub.Listener
{

	// maintains the hashset of downloaded themes
	private HashSet<String> mDownloadedAssets;

	private String[] mPubSubListeners = { HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS, HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_FAILURE };

	public ChatThemeAssetHelper()
	{
		mDownloadedAssets = new HashSet<>();
		HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
	}

	/**
	 * add assets to the downloaded set
	 *
	 * @param String
	 *            assetId Or an array of ids
	 * @return Void
	 *
	 */
	public void addDownloadedAsset(String assetId)
	{
		if (!mDownloadedAssets.contains(assetId))
		{
			mDownloadedAssets.add(assetId);
		}
	}

	public void addDownloadedAssets(String[] assets)
	{
		for (int i = 0; i < assets.length; i++)
		{
			addDownloadedAsset(assets[i]);
		}
	}

	/**
	 * Checks if all the assets for this is theme are available or not.
	 *
	 * @param theme
	 *            HikeChatTheme
	 * @return boolean
	 *
	 */
	public boolean isAssetsAvailableForTheme(HikeChatTheme theme)
	{
		if (theme.getAssetDownloadStatus() != HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE)
		{
			updateAssetsDownloadStatus(theme);
		}
		return ((theme.getAssetDownloadStatus() & HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE) == HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE);
	}

	/**
	 * Updates the Download status of a asset for given theme
	 *
	 * @param theme
	 *            HikeChatTheme
	 * @return void
	 *
	 */
	public void updateAssetsDownloadStatus(HikeChatTheme theme)
	{
		HikeChatThemeAsset[] assets = theme.getAssets();
		for (int i = 0; i < HikeChatThemeConstants.ASSET_INDEX_COUNT; i++)
		{
			if ((assets[i] != null) && (mDownloadedAssets.contains(assets[i].getAssetId())))
			{
				theme.setAssetDownloadStatus(1 << i);
			}
		}
	}

	/*
	 * Assets are saved in the application cache directory, which is secured to some extent from the user, but not guaranteed When we are trying to set the theme, after the file
	 * operation, we found like the asset is delete. though in db and datastructure we have the entry as successfully downloaded. we can make use of this method to reverse the
	 * entry. *
	 * 
	 * @param theme HikeChatTheme
	 * 
	 * @param assetType Type of asset
	 * 
	 * @return boolean
	 */
	public void setAssetMissing(HikeChatTheme theme, byte assetIndex)
	{
		int assetStatus = theme.getAssetDownloadStatus();
		assetStatus &= ~(1 << assetIndex);
		theme.overrideAssetDownloadStatus(assetStatus);

		// TODO CHATTHEME Update asset missing in to DB here
	}

	public void getMissingAssets(HikeChatTheme theme)
	{
		// TODO CHATTHEME To Pool Missing assets from DB
	}

	/**
	 * Places the Network request to Download Assets, Place request only for image assets.
	 *
	 * @param String
	 *            [] assetIds
	 * @return void
	 *
	 */
	// TODO CHATTHEME Validation , to place request only for images, on the calling side
	public void assetDownloadRequest(String[] assetIds)
	{
		DownloadAssetsTask downloadAssets = new DownloadAssetsTask(assetIds);
		downloadAssets.execute();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS.equals(type))
		{
			String[] downloadedAssets = (String[]) object;
			addDownloadedAssets(downloadedAssets);
		}
	}
}
