package com.bsb.hike.bots;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R;

import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.Logger;

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotConfiguration extends BotConfiguration
{
	private JSONObject configData;
	
	private static final String TAG = "NonMessagingBotConfig";

	public JSONObject getConfigData()
	{
		return configData;
	}

	public NonMessagingBotConfiguration(int config)
	{
		super(config);
	}

	public NonMessagingBotConfiguration(int config, JSONObject configData)
	{
		super(config);
		this.configData = configData;
	}

	/**
	 * Returns a list of overflow items from botConfig JSON
	 * 
	 * @return
	 */
	public List<OverFlowMenuItem> getOverflowItems()
	{
		if (configData != null)
		{
			try
			{
				JSONArray menuItems = configData.getJSONArray(HikePlatformConstants.OVERFLOW_MENU);
				List<OverFlowMenuItem> items = new ArrayList<OverFlowMenuItem>();
				for (int i = 0; i < menuItems.length(); i++)
				{
					items.add(parseMenuItemFromJSON(menuItems.getJSONObject(i)));
				}
				
				return items;
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Geting JSON exception while reading overflow menu items : " + e.toString());
			}

		}
		return null;
	}

	private OverFlowMenuItem parseMenuItemFromJSON(JSONObject jsonObject)
	{
		try
		{
			String title = jsonObject.getString(HikePlatformConstants.TITLE);
			int id = jsonObject.getInt(HikePlatformConstants.ID);
			boolean enabled = jsonObject.getBoolean(HikePlatformConstants.ENABLED);
			/**
			 * Note : This is a dummy icon. Will replace once I get proper assets
			 */
			return new OverFlowMenuItem(title, -1, enabled ? R.drawable.ic_delete : -1, id, enabled);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Geting JSON exception while reading overflow menu items : " + e.toString());
		}
		
		return null;
	}
	
	/**
	 * Utility method to update the overflow menu for a given id
	 * 
	 * @param id
	 * @param newTitle
	 * @param enabled
	 */

	public void updateOverFlowMenu(int id, String newTitle, boolean enabled)
	{
		if (configData != null)
		{
			Logger.v(TAG, "Trying to update overflow menu for : " + id + " for this botConfig : " + configData.toString());
			try
			{
				JSONArray menuItems = configData.getJSONArray(HikePlatformConstants.OVERFLOW_MENU);
				for (int i = 0; i < menuItems.length(); i++)
				{
					JSONObject menuJSON = menuItems.getJSONObject(i);
					int menuId = menuJSON.getInt(HikePlatformConstants.ID);
					if (menuId == id)
					{
						updateMenuJSON(newTitle, enabled, menuJSON);
						break;
					}
				}

			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Geting JSON exception while reading overflow menu items : " + e.toString());
			}

		}
	}

	/**
	 * Utility method to update the OverFlowMenu field in BotConfig JSON
	 * 
	 * @param newTitle
	 * @param enabled
	 * @param menuJSON
	 */
	private void updateMenuJSON(String newTitle, boolean enabled, JSONObject menuJSON)
	{
		try
		{
			menuJSON.put(HikePlatformConstants.TITLE, newTitle);
			menuJSON.put(HikePlatformConstants.ENABLED, enabled);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Got JSON Exception in updateMenuJSON " + e.toString());
		}
	}

	/**
	 * Utility method to updte overflow menu for a given id
	 * 
	 * @param id
	 * @param enabled
	 */
	public void updateOverFlowMenu(int id, boolean enabled)
	{
		if (configData != null)
		{
			Logger.v(TAG, "Trying to update overflow menu for : " + id + " for this botConfig : " + configData.toString());
			try
			{
				JSONArray menuItems = configData.getJSONArray(HikePlatformConstants.OVERFLOW_MENU);
				for (int i = 0; i < menuItems.length(); i++)
				{
					JSONObject menuJSON = menuItems.getJSONObject(i);
					int menuId = menuJSON.getInt(HikePlatformConstants.ID);
					if (menuId == id)
					{
						updateMenuJSON(enabled, menuJSON);
						break;
					}
				}

			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Geting JSON exception while reading overflow menu items : " + e.toString());
			}

		}
	}

	private void updateMenuJSON(boolean enabled, JSONObject menuJSON)
	{
		try
		{
			menuJSON.put(HikePlatformConstants.ENABLED, enabled);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Got JSON Exception in updateMenuJSON " + e.toString());
		}
		
	}
}