package com.bsb.hike.bots;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.graphics.Color;

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
	
	private boolean configDataRefreshed = false;
	
	/**
	 * Bit positions for configData. These positions start from the most significant bit
	 */
	public static final byte OVERFLOW_MENU  = 0;
	
	public static final byte SHOW_BLOCK = 1;
	
	public static final byte SHOW_MUTE = 2;
	
	public static final byte ALLOW_BACK_PRESS = 3;
	
	public static final byte ENABLE_LANDSCAPE = 4;
	
	public static final byte ENABLE_PORTRAIT = 5;
	
	public static final byte LONG_TAP = 6;
	
	/**
	 * Bit positions end here.
	 */
	
	private static final int PORTRAIT = 0;
	
	private static final int LANDSCAPE = 1;
	
	private static final int PORTRAIT_LANDSCAPE = 3;
	
	public boolean shouldShowOverflowMenu()
	{
		return isBitSet(OVERFLOW_MENU);
	}
	
	public boolean isBlockEnabled()
	{
		return isBitSet(SHOW_BLOCK);
	}
	
	public boolean isMuteEnabled()
	{
		return isBitSet(SHOW_MUTE);
	}
	
	public boolean isBackPressAllowed()
	{
		return isBitSet(ALLOW_BACK_PRESS);
	}
	
	public boolean isLandscapeEnabled()
	{
		return isBitSet(ENABLE_LANDSCAPE);
	}
	
	public boolean isPortraitEnabled()
	{
		return isBitSet(ENABLE_PORTRAIT);
	}
	
	public boolean isLongTapEnabled()
	{
		return isBitSet(LONG_TAP);
	}
	
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
			return new OverFlowMenuItem(title, 0, enabled ? R.drawable.ic_delete : 0, id, enabled);
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
						setConfigDataRefreshed(true);
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
	 * Utility method to update the overflow menu title
	 * @param id
	 * @param newTitle
	 */
	public void updateOverFlowMenu(int id, String newTitle)
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
						updateMenuJSON(newTitle, menuJSON);
						setConfigDataRefreshed(true);
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
	 * Utility to update the title in BotConfig JSON
	 * 
	 * @param newTitle
	 * @param menuJSON
	 */
	private void updateMenuJSON(String newTitle, JSONObject menuJSON)
	{
		try
		{
			menuJSON.put(HikePlatformConstants.TITLE, newTitle);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Got JSON Exception in updateMenuJSON " + e.toString());
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
						setConfigDataRefreshed(true);
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

	public OverFlowMenuItem getOverflowItemForId(int id)
	{
		if (configData != null)
		{
			Logger.v(TAG, "Trying to get overflow menu for : " + id + " for this botConfig : " + configData.toString());
			try
			{
				JSONArray menuItems = configData.getJSONArray(HikePlatformConstants.OVERFLOW_MENU);
				for (int i = 0; i < menuItems.length(); i++)
				{
					JSONObject menuJSON = menuItems.getJSONObject(i);
					int menuId = menuJSON.getInt(HikePlatformConstants.ID);
					if (menuId == id)
					{
						return parseMenuItemFromJSON(menuJSON); 
					}
				}
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Geting JSON exception while reading overflow menu items : " + e.toString());
			}
		}
		
		return null;
	}

	/**
	 * @return the wasConfigDataRefreshed
	 */
	public boolean isConfigDataRefreshed()
	{
		return configDataRefreshed;
	}

	/**
	 * @param wasConfigDataRefreshed the wasConfigDataRefreshed to set
	 */
	public void setConfigDataRefreshed(boolean wasConfigDataRefreshed)
	{
		this.configDataRefreshed = wasConfigDataRefreshed;
	}

	/**
	 * Method to remove menu from JSON Array of menu options in configData
	 * @param id
	 */
	public void removeOverflowMenu(int id)
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
						menuItems.remove(i);
						setConfigDataRefreshed(true);
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
	 * Utility method to get action bar color from configData
	 * 
	 * @return
	 */
	public int getActionBarColor()
	{
		if (configData != null)
		{
			String color = configData.optString(HikePlatformConstants.AB_COLOR, "transparent");
			return Color.parseColor(color);
		}
		return -1;
	}
	
	/**
	 * Utility method to get orientation for Bot. <br>
	 * 0 indicates : PORTRAIT <br>
	 * 1 indicates : LANDSCAPE
	 * 
	 * Default value will be PORTRAIT
	 * 
	 * @return
	 */
	public int getOritentationForBot()
	{
		int config = PORTRAIT;
		if (configData != null)
		{
			config = configData.optInt(HikePlatformConstants.ORIENTATION, PORTRAIT);
		}

		return config;
	}
}