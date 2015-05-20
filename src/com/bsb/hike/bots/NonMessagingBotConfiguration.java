package com.bsb.hike.bots;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.TextUtils;

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
	
	/**
	 * Bit positions for configData. These positions start from the most significant bit
	 */
	public static final byte OVERFLOW_MENU  = 0;
	
	public static final byte ALLOW_BACK_PRESS = 1;
	
	public static final byte ENABLE_LANDSCAPE = 2;
	
	public static final byte ENABLE_PORTRAIT = 3;
	
	public static final byte LONG_TAP = 4;
	
	/**
	 * Bit positions end here.
	 */
	
	public boolean shouldShowOverflowMenu()
	{
		return isBitSet(OVERFLOW_MENU);
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

	public NonMessagingBotConfiguration(int config, String configData)
	{
		this(config);
		initConfigData(configData);
	}
	
	public void setConfigData(String newConfigData)
	{
		initConfigData(newConfigData);
	}

	private void initConfigData(String configData)
	{
		if (!TextUtils.isEmpty(configData))
		{
			try
			{
				this.configData = new JSONObject(configData);
			}
			catch (JSONException e)
			{
				this.configData = new JSONObject();
			}

			return;
		}

		else
		{
			this.configData = new JSONObject();
		}
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
			boolean enabled = jsonObject.optBoolean(HikePlatformConstants.ENABLED, true);
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
	 * Utility method to update the overflow menu title
	 * 
	 * @param id
	 * @param JSONObject
	 *            menuObj
	 */
	public void updateOverFlowMenu(int id, JSONObject menuObj)
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
						menuItems.put(i, menuObj);
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
	 * This method replaces the overflow menu array in the config data
	 * 
	 * @param newMenuJSON
	 */
	public void replaceOverflowMenu(String newMenuJSON)
	{
		if (configData != null)
		{
			try
			{
				JSONArray menuArray = new JSONArray(newMenuJSON);
				configData.put(HikePlatformConstants.OVERFLOW_MENU, menuArray);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	 * Default value will be PORTRAIT_LANDSCAPE.
	 * 
	 * If portrait flag is enabled and landscape flag is enabled as well, we return PORTRAIT_LANDSCAPE. If only portrait/landscape is enabled we return the respective value
	 * 
	 * @return
	 */
	public int getOritentationForBot()
	{
		if (configData != null)
		{
			if (isPortraitEnabled())
			{
				if (isLandscapeEnabled())
				{
					return Configuration.ORIENTATION_UNDEFINED;
				}

				else
				{
					return Configuration.ORIENTATION_PORTRAIT;
				}
			}

			else
			{
				if (isLandscapeEnabled())
				{
					return Configuration.ORIENTATION_LANDSCAPE;
				}

				else
				{
					return Configuration.ORIENTATION_UNDEFINED;
				}
			}
		}
		return Configuration.ORIENTATION_UNDEFINED;
	}
}