package com.bsb.hike.bots;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	
	public static final String SHOW_UNREAD_COUNT_ZERO = "0";

	public static final String SHOW_UNREAD_COUNT_ACTUAL = "-1";
	
	private static final String TAG = "BotUtils";
	
	public static boolean fetchBotThumbnails = true;

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
		fetchBotIcons();
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
		botInfo.setUnreadCount(0);
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
	/*
	 * Uility method to delete the bot files from the file system
	 * 
	 * @param jsonObj	:	The bot Json object containing the properties of the bot files to be deleted
	 */
	public static void removeMicroApp(JSONObject jsonObj){
		try
		{
			JSONArray appsToBeRemoved = jsonObj.getJSONArray(HikePlatformConstants.APP_NAME);
			for (int i = 0; i< appsToBeRemoved.length(); i++){
				String appName =  appsToBeRemoved.get(i).toString();
				String makePath = PlatformContentConstants.PLATFORM_CONTENT_DIR +  appName;
				Logger.d("FileSystemAccess", "To delete the path : " + makePath);
				if(PlatformUtils.deleteDirectory(makePath)){
					String sentData = AnalyticsConstants.REMOVE_SUCCESS;
					JSONObject json = new JSONObject();
					json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.REMOVE_MICRO_APP);
					json.putOpt(AnalyticsConstants.REMOVE_MICRO_APP, sentData);
					json.putOpt(AnalyticsConstants.MICRO_APP_ID, appName);
					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.REMOVE_MICRO_APP, json);
				}
			}
		}
		catch (JSONException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
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
			BotUtils.createAndInsertBotDp(msisdn, thumbnailString);
		}
		
		String botChatTheme = jsonObj.optString(HikeConstants.BOT_CHAT_THEME);
		BotInfo botInfo = null;
		if (type.equals(HikeConstants.MESSAGING_BOT))
		{
			botInfo = getBotInfoFormessagingBots(jsonObj, msisdn);
			PlatformUtils.botCreationSuccessHandling(botInfo, false, botChatTheme, notifType);
		}
		else if (type.equals(HikeConstants.NON_MESSAGING_BOT))
		{
			botInfo = getBotInfoForNonMessagingBots(jsonObj, msisdn);
			boolean enableBot = jsonObj.optBoolean(HikePlatformConstants.ENABLE_BOT);
			NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());
			if (botMetadata.isMicroAppMode())
			{
				PlatformUtils.downloadZipForNonMessagingBot(botInfo, enableBot, botChatTheme, notifType, botMetadata, botMetadata.isResumeSupported());
			}
			else if (botMetadata.isWebUrlMode())
			{
				PlatformUtils.botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
			}
			else if (botMetadata.isNativeMode())
			{
				PlatformUtils.downloadZipForNonMessagingBot(botInfo, enableBot, botChatTheme, notifType, botMetadata, botMetadata.isResumeSupported());
			}

		}

		Logger.d("create bot", "It takes " + String.valueOf(System.currentTimeMillis() - startTime) + "msecs");
	}

	private static BotInfo getBotInfoForNonMessagingBots(JSONObject jsonObj, String msisdn)
	{
		
		BotInfo existingBotInfo = getBotInfoForBotMsisdn(msisdn);
		BotInfo botInfo = null;
		
		if (existingBotInfo != null)
		{
			try
			{
				Object clonedObj = existingBotInfo.clone();
				if (clonedObj instanceof BotInfo)
				{
					botInfo = (BotInfo) clonedObj;
				}

			}
			catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
		}

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
			String metadata = jsonObj.optString(HikeConstants.METADATA);
			botInfo.setMetadata(metadata);
		}

		if (jsonObj.has(HikePlatformConstants.BOT_VERSION))
		{
			int version = jsonObj.optInt(HikePlatformConstants.BOT_VERSION);
			botInfo.setVersion(version);
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
		BotInfo existingBotInfo = getBotInfoForBotMsisdn(msisdn);
		BotInfo botInfo = null;
		
		if (existingBotInfo != null)
		{
			try
			{
				Object clonedObj = existingBotInfo.clone();
				if (clonedObj instanceof BotInfo)
				{
					botInfo = (BotInfo) clonedObj;
				}

			}
			catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
		}
		
		
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
		
		HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_CREATED, botInfo);
		
		/**
		 * Notification will be played only if enable bot is true and notifType is Silent/Loud
		 */
		if (enableBot && (!HikeConstants.OFF.equals(notifType)))
		{
			HikeNotification.getInstance().notifyStringMessage(msisdn, botInfo.getLastMessageText(), notifType.equals(HikeConstants.SILENT), NotificationType.OTHER);
		}
	}
	
	public static void updateBotConfiguration(BotInfo botInfo, String msisdn, int config)
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
		if (HikeMessengerApp.currentState != CurrentState.BACKGROUNDED && HikeMessengerApp.currentState != CurrentState.CLOSED)
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(convInfo.getMsisdn());

			NonMessagingBotConfiguration configuration = new NonMessagingBotConfiguration(botInfo.getConfiguration());

			if (convInfo.getLastConversationMsg() != null && configuration.isSlideInEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				configuration.setBit(NonMessagingBotConfiguration.SLIDE_IN, false);
				updateBotConfiguration(botInfo, convInfo.getMsisdn(), configuration.getConfig());
				return BOT_SLIDE_IN_ANIMATION;
			}
			else if (convInfo.getLastConversationMsg() != null && configuration.isReadSlideOutEnabled()
					&& convInfo.getLastConversationMsg().getState() != ConvMessage.State.RECEIVED_UNREAD)
			{
				return BOT_READ_SLIDE_OUT_ANIMATION;
			}
		}
		return NO_ANIMATION;
	}

	private static int getMessagingBotAnimationType(ConvInfo convInfo)
	{
		if (HikeMessengerApp.currentState != CurrentState.BACKGROUNDED && HikeMessengerApp.currentState != CurrentState.CLOSED)
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(convInfo.getMsisdn());
			MessagingBotMetadata messagingBotMetadata = new MessagingBotMetadata(botInfo.getMetadata());
			MessagingBotConfiguration configuration = new MessagingBotConfiguration(botInfo.getConfiguration(), messagingBotMetadata.isReceiveEnabled());

			if (convInfo.getLastConversationMsg() != null && configuration.isSlideInEnabled() && convInfo.getLastConversationMsg().getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				configuration.setBit(MessagingBotConfiguration.SLIDE_IN, false);
				updateBotConfiguration(botInfo, convInfo.getMsisdn(), configuration.getConfig());
				return BOT_SLIDE_IN_ANIMATION;
			}
			else if (convInfo.getLastConversationMsg() != null && configuration.isReadSlideOutEnabled()
					&& convInfo.getLastConversationMsg().getState() != ConvMessage.State.RECEIVED_UNREAD)
			{
				return BOT_READ_SLIDE_OUT_ANIMATION;
			}
		}
		return NO_ANIMATION;
	}
	
	/**
	 * This method makes a HTTP Post Call to fetch avatar of a bot if it is not present in the HikeUserDb
	 */
	public static void fetchBotIcons()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				Logger.i(TAG, "Checking for bot icons");
				for (final BotInfo mBotInfo : HikeMessengerApp.hikeBotInfoMap.values())
				{
					if (mBotInfo.isConvPresent() && !ContactManager.getInstance().hasIcon(mBotInfo.getMsisdn()))
					{
						Logger.i(TAG, "Making icon request for " + mBotInfo.getMsisdn() + mBotInfo.getConversationName());
						RequestToken botRequestToken = HttpRequests.getAvatarForBots(mBotInfo.getMsisdn(), new IRequestListener()
						{
							@Override
							public void onRequestSuccess(Response result)
							{
								byte[] response = (byte[]) result.getBody().getContent();

								Logger.i(TAG, "Bot icon request successful for " + mBotInfo.getMsisdn());
								if (response != null && response.length > 0)
								{
									BotUtils.createAndInsertBotDp(mBotInfo.getMsisdn(), response);
								}

							}

							@Override
							public void onRequestProgressUpdate(float progress)
							{
							}

							@Override
							public void onRequestFailure(HttpException httpException)
							{
								Logger.i(
										TAG,
										"Bot icon request failed for " + mBotInfo + " Reason :  " + httpException.getMessage() + " Error Code "
												+ httpException.getErrorCode());
							}
						});

						botRequestToken.execute();
					}

				}
			}
		}, 0);
	}

	public static boolean isSpecialBot(BotInfo botInfo)
	{
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return false;
		}
		return true;
	}
	
	/**
	 * Returns whether bot Discovery feature is enabled or not
	 * 
	 * @return
	 */
	public static boolean isBotDiscoveryEnabled()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_BOT_DISCOVERY, false);
	}

//	/**
//	 * Utility method called to syncBotDiscoveryTable with the table present on server
//	 */
//	public static void syncBotDiscoveryTable()
//	{
//		makeBotDiscoveryDownloadRequest(true, false, 0);
//	}
//	
//	/**
//	 * This method makes the POST call to fetch the botInfo objects for bot discoverytable
//	 * 
//	 * @param allRequired
//	 *            - All bots required from server. If true, we ignore the sendClientBots value 
//	 * @param sendclientBots
//	 *            - Whether client bots are required to be sent to the server for ordering
//	 * @param offset
//	 *            - The offset from where the data is to be fetched. if allRequired is true, offset is hardcoded as 0
//	 */
//	public static void makeBotDiscoveryDownloadRequest(boolean allRequired, boolean sendclientBots, int offset)
//	{
//		Logger.v(TAG, "Making bot discovery table download request");
//		JSONObject body = new JSONObject();
//		int mOffset = offset;
//
//		try
//		{
//			if (allRequired)
//			{
//				body.put(HikePlatformConstants.ALL_REQUIRED, true);
//				mOffset = 0;
//			}
//
//			if (!allRequired && sendclientBots)
//			{
//				body.put(HikePlatformConstants.BOTS, getClientBotJSONArray());
//			}
//
//			BotDiscoveryDownloadTask task = new BotDiscoveryDownloadTask(mOffset, body);
//
//			task.execute();
//
//		}
//		catch (JSONException e)
//		{
//			Logger.v(TAG, "Making bot discovery table download request : got an exception " + e);
//		}
//	}
//
//	/**
//	 * Returns the bot msisdns present in the client in the form of an JSONArray eg : [“+hike1+”, “+hikenews+”,”+hikecricket+”, “hikegrowth+”]
//	 * 
//	 * @return {@link JSONArray}
//	 */
//	private static JSONArray getClientBotJSONArray()
//	{
//		JSONArray botArray = new JSONArray();
//
//		for (String msisdn : HikeMessengerApp.hikeBotInfoMap.keySet())
//		{
//			botArray.put(msisdn);
//		}
//
//		return botArray;
//	}
	
	/**
	 * Log analytics for discovery bot download request.
	 * @param msisdn
	 * @param name
	 */
	public static void discoveryBotDownloadAnalytics(String msisdn, String name)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.DISCOVERY_BOT_DOWNLOAD);
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			json.put(AnalyticsConstants.BOT_NAME, name);
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
			json.put(HikePlatformConstants.NETWORK_TYPE, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSON Exception in botDownloadAnalytics "+e.getMessage());
		}
		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.BOT_DISCOVERY, json);
	}
	
	/**
	 * Unblock the bot and add to the conversation list.
	 * @param botInfo
	 * @param origin : From where the unblocking has been triggered. Example : Overflow menu, bot discovery etc.
	 */
	public static void unblockBotIfBlocked(BotInfo botInfo, String origin)
	{
		if (botInfo.isBlocked())
		{
			botInfo.setBlocked(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, botInfo.getMsisdn());
			BotConversation.analyticsForBots(botInfo.getMsisdn(), HikePlatformConstants.BOT_UNBLOCK_CHAT, origin, AnalyticsConstants.CLICK_EVENT, null);
		}
	}
	
	/**
	 * Utility method for persisting a Bot's DP. Note : This method should be the central place for handling bot dp's in a single place. If DP is persisted without calling this
	 * method, the results might be catastrophic
	 */
	public static void createAndInsertBotDp(String msisdn, byte[] imageData)
	{

		File botDpPath = new File(getBotThumbnailRootFolder() + msisdn);

		// Save Icon to file
		try
		{
			Utils.saveByteArrayToFile(botDpPath, imageData);
			HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);

		}

		catch (IOException e)
		{
			Logger.e(TAG, "Unable to save dp for bot with msisdn : " + msisdn + " Error : " + e.toString());
		}

	}
	
	/**
	 * Utility method for persisting a Bot's DP. Note : This method should be the central place for handling bot dp's in a single place. If DP is persisted without calling this
	 * method, the results might be catastrophic
	 */
	public static void createAndInsertBotDp(String msisdn, String imageData)
	{
		File botDpPath = new File(getBotThumbnailRootFolder() + msisdn);

		try
		{
			Utils.saveBase64StringToFile(botDpPath, imageData);
			HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
		}
		catch (IOException e)
		{
			Logger.e(TAG, "Unable to save dp for bot with msisdn : " + msisdn + " Error : " + e.toString());
		}
		
	}	
	
	
	/**
	 * Returns the root folder path for bot thumbnails <br>
	 * eg : "/data/data/com.bsb.hike/files/Content/DP/"
	 * 
	 * @return
	 */
	private static String getBotThumbnailRootFolder()
	{
		File file = new File (PlatformContentConstants.PLATFORM_CONTENT_DIR + "DP");
		if (!file.exists())
		{
			file.mkdirs();
		}
		
		return PlatformContentConstants.PLATFORM_CONTENT_DIR + "DP" + File.separator;
	}
	
	public static Bitmap getBotDp(String botMsisdn)
	{
		File file = new File (BotUtils.getBotThumbnailRootFolder() + botMsisdn);
		if (file.exists())
		{
			return HikeBitmapFactory.decodeFile(BotUtils.getBotThumbnailRootFolder() + botMsisdn);
		}

		Logger.v(TAG, "File does not exist for : " + botMsisdn + " Maybe it's not a bot");
		return null;
	}
	

}
