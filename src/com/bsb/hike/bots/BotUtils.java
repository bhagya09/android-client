package com.bsb.hike.bots;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;

/**
 * This class is for utility methods of bots
 * 
 * @author piyush
 * 
 */
public class BotUtils
{
	/**
	 * adding default bots to bot hashmap. The config is set using {@link com.bsb.hike.bots.MessagingBotConfiguration}, where every bit is set according to the requirement
	 * https://docs.google.com/spreadsheets/d/1hTrC9GdGRXrpAt9gnFnZACiTz2Th8aUIzVB5hrlD_4Y/edit#gid=0
	 */
	public static Map<String, BotInfo> getDefaultHardCodedBotInfoObjects()
	{
		Map<String, BotInfo> botsMap = new HashMap<String, BotInfo>();

		BotInfo teamHike = new BotInfo.HikeBotBuilder("+hike+").setConvName("team hike").configuration(4527).build();

		BotInfo emmaBot = new BotInfo.HikeBotBuilder("+hike1+").setConvName("Emma from hike").configuration(2069487).build();

		BotInfo gamesOnHike = new BotInfo.HikeBotBuilder("+hike2+").setConvName("Games on hike").configuration(21487).build();

		BotInfo hikeDaily = new BotInfo.HikeBotBuilder("+hike3+").setConvName("hike daily").configuration(2069487).build();

		BotInfo hikeSupport = new BotInfo.HikeBotBuilder("+hike4+").setConvName("hike support").configuration(2069487).build();

		BotInfo natasha = new BotInfo.HikeBotBuilder("+hike5+").setConvName("Natasha").configuration(2069487).build();

		BotInfo cricketBot = new BotInfo.HikeBotBuilder("+hikecricket+").setConvName("Cricket 2015").configuration(21487).build();

		botsMap.put("+hike+", teamHike);
		botsMap.put("+hike1+", emmaBot);
		botsMap.put("+hike2+", gamesOnHike);
		botsMap.put("+hike3+", hikeDaily);
		botsMap.put("+hike4+", hikeSupport);
		botsMap.put("+hike5+", natasha);
		botsMap.put("+hikecricket+", cricketBot);

		return botsMap;
	}

	/**
	 * adding default bots on app upgrade. The config is set using {@link com.bsb.hike.bots.MessagingBotConfiguration}, where every bit is set according to the requirement
	 * https://docs.google.com/spreadsheets/d/1hTrC9GdGRXrpAt9gnFnZACiTz2Th8aUIzVB5hrlD_4Y/edit#gid=0
	 */
	public static void addDefaultBotsToDB(Context context)
	{
		defaultBotEntry("+hike+", "team hike", null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.hiketeam, context.getApplicationContext()), 4527, false, context);

		defaultBotEntry("+hike1+", "Emma from hike", null, null, 2069487, true, context);

		defaultBotEntry("+hike2+", "Games on hike", null, null, 21487, false, context);

		defaultBotEntry("+hike3+", "hike daily", null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.hikedaily, context.getApplicationContext()), 21487, false, context);

		defaultBotEntry("+hike4+", "hike support", null, null, 2069487, true, context);

		defaultBotEntry("+hike5+", "Natasha", null, null, 2069487, true, context);

		defaultBotEntry("+hikecricket+", "Cricket 2015", HikePlatformConstants.CRICKET_CHAT_THEME_ID,
				HikeBitmapFactory.getBase64ForDrawable(R.drawable.cric_icon, context.getApplicationContext()), 21487, false, context);
	}

	/**
	 * Hard coding the cricket bot on the App's onCreate so that there is a cricket bot entry when there is no bot currently in the app. Using the shared prefs for that matter.
	 * Hardcoding the bot name, bot msisdn, the bot chat theme, bot's dp and its type. Can be updated using the AC packet cbot and delete using the ac packet dbot.
	 **/
	private static void defaultBotEntry(final String msisdn, final String name, final String chatThemeId, final String dp, final int config, final boolean isReceiveEnabled,
			Context context)
	{

		Logger.d("create bot", "default bot entry started");
		final JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.MSISDN, msisdn);
			jsonObject.put(HikeConstants.NAME, name);

			if (!TextUtils.isEmpty(dp))
			{
				jsonObject.put(HikeConstants.BOT_THUMBNAIL, dp);
			}

			if (!TextUtils.isEmpty(chatThemeId))
			{
				jsonObject.put(HikeConstants.BOT_CHAT_THEME, chatThemeId);
			}
			jsonObject.put(HikePlatformConstants.BOT_TYPE, HikeConstants.MESSAGING_BOT);
			jsonObject.put(HikeConstants.CONFIGURATION, config);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.IS_RECEIVE_ENABLED_IN_BOT, isReceiveEnabled);
			jsonObject.put(HikeConstants.METADATA, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		MqttMessagesManager.getInstance(context.getApplicationContext()).createBot(jsonObject);
	}
}
