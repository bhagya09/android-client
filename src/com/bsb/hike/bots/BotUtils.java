package com.bsb.hike.bots;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This class is for utility methods of bots
 * 
 * @author piyush
 * 
 */
public class BotUtils
{
	
	public static final int NO_ANIMATION = 0;
	
	public static final int BOT_SLIDE_IN_ANIMATION = 1;
	
	public static final int BOT_READ_SLIDE_OUT_ANIMATION = 2;
	
	public static final String UNREAD_COUNT_SHOW_TYPE = "unrdCntShw";
	
	public static final int SHOW_UNREAD_COUNT_ZERO = 0;

	public static final int SHOW_UNREAD_COUNT_ONE = 1;

	public static final int SHOW_UNREAD_COUNT_ACTUAL = 2;

	/**
	 * adding default bots to bot hashmap. The config is set using {@link com.bsb.hike.bots.MessagingBotConfiguration}, where every bit is set according to the requirement
	 * https://docs.google.com/spreadsheets/d/1hTrC9GdGRXrpAt9gnFnZACiTz2Th8aUIzVB5hrlD_4Y/edit#gid=0
	 */
	public static Map<String, BotInfo> getDefaultHardCodedBotInfoObjects()
	{
		Map<String, BotInfo> botsMap = new HashMap<String, BotInfo>();

		BotInfo teamHike = new BotInfo.HikeBotBuilder("+hike+").setConvName("team hike").setConfig(4527).build();

		BotInfo emmaBot = new BotInfo.HikeBotBuilder("+hike1+").setConvName("Emma from hike").setConfig(2069487).build();

		BotInfo gamesOnHike = new BotInfo.HikeBotBuilder("+hike2+").setConvName("Games on hike").setConfig(21487).build();

		BotInfo hikeDaily = new BotInfo.HikeBotBuilder("+hike3+").setConvName("hike daily").setConfig(2069487).build();

		BotInfo hikeSupport = new BotInfo.HikeBotBuilder("+hike4+").setConvName("hike support").setConfig(2069487).build();

		BotInfo natasha = new BotInfo.HikeBotBuilder("+hike5+").setConvName("Natasha").setConfig(2069487).build();

		BotInfo cricketBot = new BotInfo.HikeBotBuilder("+hikecricket+").setConvName("Cricket 2015").setConfig(21487).build();

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

		defaultBotEntry("+hike5+", "Natasha", null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.natasha, context.getApplicationContext()), 2069487, true, context);

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

		createBot(jsonObject);
	}
	
	public static boolean isBot(String msisdn)
	{
		if (HikeMessengerApp.hikeBotInfoMap != null)
		{
			return HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn);
		}
		else
		{
			// Not probable
			return false;
		}
	}
	
	public static BotInfo getBotInfoForBotMsisdn(String msisdn)
	{
		return HikeMessengerApp.hikeBotInfoMap.get(msisdn);
	}

	/**
	 * This method populates the hashmap for bots from db after restore has been done
	 */
	public static void postAccountRestoreSetup()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_FOR_DEFAULT_BOT_ENTRY, true);
		initBots();
	}
	
	/**
	 * This method is to be called in the following cases : 1. Normal upgrade of app ( We need to perform migration)<br>
	 * 2. If we do an unlink relink on the current version (Assuming that migration was already done once), there are 2 cases : <br>
	 * a) Either we restore backup<br>
	 * b) We do not restore backup<br>
	 * 
	 * In 2 (a) : We set the preference flags to false and populate the map for bots from db.<br>
	 * 
	 * In 2 (b) : We call initBots without modifying the prefs. This is same as performing step 1. <br>
	 * 
	 * 3. Under normal circumstances, it will also be called from HikeMessengerApp's onCreate<br>
	 * 
	 * 4. In case of failure of restoring backup
	 * 
	 * 5. In unlink-relink case, (without the backup coming into picture and without force closing the app) 
	 */
	public static void initBots()
	{

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.UPGRADE_FOR_DEFAULT_BOT_ENTRY, true))
		{
			addDefaultBotsToDB(HikeMessengerApp.getInstance().getApplicationContext());
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_FOR_DEFAULT_BOT_ENTRY, false);
		}

		HikeConversationsDatabase.getInstance().getBotHashmap();
		Logger.d("create bot", "Keys are " + HikeMessengerApp.hikeBotInfoMap.keySet() + "------");
		Logger.d("create bot", "values are " + HikeMessengerApp.hikeBotInfoMap.values());
	}

	/**
	 * Call this method to delete the bot conversation. This is the central method and should be th one that should be called to delete a bot conversation.
	 * @param msisdn : the bot msisdn that will be deleted.
	 * @param hardDelete : whether we want to delete the bot from table and from map as well?
	 */
	public static void deleteBotConversation(String msisdn, boolean hardDelete)
	{
		Logger.d("delete bot", "bot to be deleted is " + msisdn + " and hard delete is " + String.valueOf(hardDelete));
		BotInfo botInfo = getBotInfoForBotMsisdn(msisdn);
		if (botInfo == null)
		{
			return;
		}
		if(hardDelete)
		{
			HikeMessengerApp.hikeBotInfoMap.remove(msisdn);
			HikeConversationsDatabase.getInstance().deleteBot(msisdn);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_THIS_CONVERSATION, botInfo);
	}

	public static void deleteBot(String msisdn)
	{
		if (!Utils.validateBotMsisdn(msisdn))
		{
			return;
		}
		deleteBotConversation(msisdn , true);
	}

	/**
	 * Utility method to create an entry of Bot in bot_table, conversations_table. Also create a conversation on the conversation fragment.
	 * 
	 * @param jsonObj
	 *            The bot Json object containing the properties of the bot to be created
	 */
	public static void createBot(JSONObject jsonObj)
	{
		long startTime = System.currentTimeMillis();

		String type = jsonObj.optString(HikePlatformConstants.BOT_TYPE);
		if (TextUtils.isEmpty(type))
		{
			Logger.e("bot error", "type is null.");
			return;
		}

		String msisdn = jsonObj.optString(HikeConstants.MSISDN);
		if (!Utils.validateBotMsisdn(msisdn))
		{
			return;
		}

		if (ContactManager.getInstance().isBlocked(msisdn))
		{
			Logger.e("bot error", "bot is blocked by user.");
			return;
		}

		String thumbnailString = jsonObj.optString(HikeConstants.BOT_THUMBNAIL);
		String notifType = jsonObj.optString(HikeConstants.PLAY_NOTIFICATION, HikeConstants.OFF);
		
		if (!TextUtils.isEmpty(thumbnailString))
		{
			ContactManager.getInstance().setIcon(msisdn, Base64.decode(thumbnailString, Base64.DEFAULT), false);
			HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
		}

		String botChatTheme = jsonObj.optString(HikeConstants.BOT_CHAT_THEME);
		BotInfo botInfo = null;
		if (type.equals(HikeConstants.MESSAGING_BOT))
		{
			botInfo = getBotInfoFormessagingBots(jsonObj, msisdn);
			updateBotParamsInDb(botChatTheme, botInfo, false, notifType);
		}
		else if (type.equals(HikeConstants.NON_MESSAGING_BOT))
		{
			botInfo = getBotInfoForNonMessagingBots(jsonObj, msisdn);
			boolean enableBot = jsonObj.optBoolean(HikePlatformConstants.ENABLE_BOT);
			PlatformUtils.downloadZipForNonMessagingBot(botInfo, enableBot, botChatTheme, notifType);
		}

		Logger.d("create bot", "It takes " + String.valueOf(System.currentTimeMillis() - startTime) + "msecs");
	}

	private static BotInfo getBotInfoForNonMessagingBots(JSONObject jsonObj, String msisdn)
	{
		BotInfo botInfo = getBotInfoForBotMsisdn(msisdn);

		if (null == botInfo)
		{
			botInfo = new BotInfo.HikeBotBuilder(msisdn)
					.setType(BotInfo.NON_MESSAGING_BOT)
					.setIsMute(false)
					.build();
		}

		if (jsonObj.has(HikeConstants.NAME))
		{
			String name = jsonObj.optString(HikeConstants.NAME);
			botInfo.setmConversationName(name);
		}

		JSONObject configData = null;
		if (jsonObj.has(HikePlatformConstants.CONFIG_DATA))
		{
			configData = jsonObj.optJSONObject(HikePlatformConstants.CONFIG_DATA);
		}

		if (jsonObj.has(HikeConstants.CONFIGURATION))
		{
			int config = jsonObj.optInt(HikeConstants.CONFIGURATION, Integer.MAX_VALUE);
			NonMessagingBotConfiguration configuration = configData == null ? new NonMessagingBotConfiguration(config)
					: new NonMessagingBotConfiguration(config, configData.toString());
			botInfo.setConfiguration(configuration.getConfig());
			botInfo.setConfigData(null == configuration.getConfigData() ? "" : configuration.getConfigData().toString());
		}

		if (jsonObj.has(HikePlatformConstants.NAMESPACE))
		{
			String namespace = jsonObj.optString(HikePlatformConstants.NAMESPACE);
			botInfo.setNamespace(namespace);
		}

		if (jsonObj.has(HikeConstants.METADATA))
		{
			JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);
			NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(metadata);
			botInfo.setMetadata(botMetadata.toString());
		}

		if (jsonObj.has(HikePlatformConstants.HELPER_DATA))
		{

			try
			{
				JSONObject helperDataDiff = jsonObj.optJSONObject(HikePlatformConstants.HELPER_DATA);
				JSONObject oldHelperData = TextUtils.isEmpty(botInfo.getHelperData()) ? new JSONObject() : new JSONObject(botInfo.getHelperData());
				JSONObject newHelperData = PlatformUtils.mergeJSONObjects(oldHelperData, helperDataDiff);
				botInfo.setHelperData(newHelperData.toString());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

		}

		return botInfo;
	}

	private static BotInfo getBotInfoFormessagingBots(JSONObject jsonObj, String msisdn)
	{
		BotInfo botInfo = getBotInfoForBotMsisdn(msisdn);
		if (null == botInfo)
		{
			botInfo = new BotInfo.HikeBotBuilder(msisdn)
					.setType(BotInfo.MESSAGING_BOT)
					.setIsMute(false)
					.build();
		}

		if (jsonObj.has(HikeConstants.NAME))
		{
			String name = jsonObj.optString(HikeConstants.NAME);
			botInfo.setmConversationName(name);
		}

		if (jsonObj.has(HikeConstants.METADATA))
		{
			JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);
			MessagingBotMetadata messagingBotMetadata = new MessagingBotMetadata(metadata);
			if (jsonObj.has(HikeConstants.CONFIGURATION))
			{
				int config = jsonObj.optInt(HikeConstants.CONFIGURATION, Integer.MAX_VALUE);
				MessagingBotConfiguration configuration = new MessagingBotConfiguration(config, messagingBotMetadata.isReceiveEnabled());
				botInfo.setConfiguration(configuration.getConfig());
			}
			botInfo.setMetadata(messagingBotMetadata.toString());
		}

		return botInfo;
	}

	/**
	 * Utility method to update the bot entries as well as playNotif in case of enable bot
	 * 
	 * @param botChatTheme
	 * @param botInfo
	 * @param enableBot
	 * @param notifType
	 */
	public static void updateBotParamsInDb(String botChatTheme, BotInfo botInfo, boolean enableBot, String notifType)
	{
		String msisdn = botInfo.getMsisdn();
		HikeConversationsDatabase convDb = HikeConversationsDatabase.getInstance();
		convDb.setChatBackground(msisdn, botChatTheme, System.currentTimeMillis()/1000);
		convDb.insertBot(botInfo);

		HikeMessengerApp.hikeBotInfoMap.put(msisdn, botInfo);
		ContactInfo contact = new ContactInfo(msisdn, msisdn, botInfo.getConversationName(), msisdn);
		contact.setFavoriteType(ContactInfo.FavoriteType.NOT_FRIEND);
		ContactManager.getInstance().updateContacts(contact);
		HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_ADDED, contact);
		
		/**
		 * Notification will be played only if enable bot is true and notifType is Silent/Loud
		 */
		if (enableBot && (!HikeConstants.OFF.equals(notifType)))
		{
			HikeNotification.getInstance().notifyStringMessage(msisdn, botInfo.getLastMessageText(), notifType.equals(HikeConstants.SILENT), NotificationType.OTHER);
		}
	}
	
	private static void updateBotConfiguration(BotInfo botInfo, String msisdn, int config)
	{
		HikeConversationsDatabase.getInstance().updateBotConfiguration(msisdn, config);
		botInfo.setConfiguration(config);
	}

	public static int getBotAnimaionType(ConvInfo convInfo)
	{
		if (BotUtils.isBot(convInfo.getMsisdn()))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(convInfo.getMsisdn());

			if (botInfo.isMessagingBot())
			{
				return getMessagingBotAnimationType(convInfo);
			}
			else
			{
				return getNonMessagingBotAnimationType(convInfo);
			}
		}
		return NO_ANIMATION;
	}

	private static int getNonMessagingBotAnimationType(ConvInfo convInfo)
	{
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(convInfo.getMsisdn());

		NonMessagingBotConfiguration configuration = new NonMessagingBotConfiguration(botInfo.getConfiguration());

		if (configuration.isSlideInEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_UNREAD)
		{
			configuration.setBit(NonMessagingBotConfiguration.SLIDE_IN, false);
			updateBotConfiguration(botInfo, convInfo.getMsisdn(), configuration.getConfig());
			return BOT_SLIDE_IN_ANIMATION;
		}
		else if (configuration.isReadSlideOutEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_READ)
		{
			return BOT_READ_SLIDE_OUT_ANIMATION;
		}

		return NO_ANIMATION;
	}

	private static int getMessagingBotAnimationType(ConvInfo convInfo)
	{
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(convInfo.getMsisdn());
		MessagingBotMetadata messagingBotMetadata = new MessagingBotMetadata(botInfo.getMetadata());
		MessagingBotConfiguration configuration = new MessagingBotConfiguration(botInfo.getConfiguration(), messagingBotMetadata.isReceiveEnabled());

		if (configuration.isSlideInEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_UNREAD)
		{
			configuration.setBit(MessagingBotConfiguration.SLIDE_IN, false);
			updateBotConfiguration(botInfo, convInfo.getMsisdn(), configuration.getConfig());
			return BOT_SLIDE_IN_ANIMATION;
		}
		else if (configuration.isReadSlideOutEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_READ)
		{
			return BOT_READ_SLIDE_OUT_ANIMATION;
		}

		return NO_ANIMATION;
	}

}
