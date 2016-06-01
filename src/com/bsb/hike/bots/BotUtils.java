package com.bsb.hike.bots;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.net.Uri;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
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
import com.bsb.hike.notifications.ToastListener;
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
import java.util.List;
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
	public static Map<String, BotInfo> getDefaultHardCodedBotInfoObjects(Context context)
	{
		Map<String, BotInfo> botsMap = new HashMap<String, BotInfo>();

		BotInfo teamHike = new BotInfo.HikeBotBuilder(HikePlatformConstants.TEAM_HIKE_MSISDN).setConvName(context.getString(R.string.team_hike_bot)).setConfig(4527).build();

		BotInfo emmaBot = new BotInfo.HikeBotBuilder(HikePlatformConstants.EMMA_BOT_MSISDN).setConvName(context.getString(R.string.emma_bot)).setConfig(2069487).build();

		BotInfo gamesOnHike = new BotInfo.HikeBotBuilder(HikePlatformConstants.GAMES_HIKE_MSISDN).setConvName(context.getString(R.string.games_bot)).setConfig(21487).build();

		BotInfo hikeDaily = new BotInfo.HikeBotBuilder(HikePlatformConstants.HIKE_DAILY_MSISDN).setConvName(context.getString(R.string.hike_daily_bot)).setConfig(22511).build();

		BotInfo hikeSupport = new BotInfo.HikeBotBuilder(HikePlatformConstants.HIKE_SUPPORT_MSISDN).setConvName(context.getString(R.string.hike_support_bot)).setConfig(2069487).build();

		BotInfo natasha = new BotInfo.HikeBotBuilder(HikePlatformConstants.NATASHA_MSISDN).setConvName(context.getString(R.string.natasha_bot)).setConfig(35624943).build();

		BotInfo cricketBot = new BotInfo.HikeBotBuilder(HikePlatformConstants.CRICKET_HIKE_MSISDN).setConvName(context.getString(R.string.cricket_bot)).setConfig(21487).build();

		botsMap.put(HikePlatformConstants.TEAM_HIKE_MSISDN, teamHike);
		botsMap.put(HikePlatformConstants.EMMA_BOT_MSISDN, emmaBot);
		botsMap.put(HikePlatformConstants.GAMES_HIKE_MSISDN, gamesOnHike);
		botsMap.put(HikePlatformConstants.HIKE_DAILY_MSISDN, hikeDaily);
		botsMap.put(HikePlatformConstants.HIKE_SUPPORT_MSISDN, hikeSupport);
		botsMap.put(HikePlatformConstants.NATASHA_MSISDN, natasha);
		botsMap.put(HikePlatformConstants.CRICKET_HIKE_MSISDN, cricketBot);

		return botsMap;
	}

	/**
	 * adding default bots on app upgrade. The config is set using {@link com.bsb.hike.bots.MessagingBotConfiguration}, where every bit is set according to the requirement
	 * https://docs.google.com/spreadsheets/d/1hTrC9GdGRXrpAt9gnFnZACiTz2Th8aUIzVB5hrlD_4Y/edit#gid=0
	 */
	public static void addDefaultBotsToDB(Context context)
	{
		defaultBotEntry(HikePlatformConstants.TEAM_HIKE_MSISDN, context.getString(R.string.team_hike_bot), null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.hiketeam, context.getApplicationContext()), 4527, false, context);

		defaultBotEntry(HikePlatformConstants.EMMA_BOT_MSISDN, context.getString(R.string.emma_bot), null, null, 2069487, true, context);

		defaultBotEntry(HikePlatformConstants.GAMES_HIKE_MSISDN, context.getString(R.string.games_bot), null, null, 21487, false, context);

		defaultBotEntry(HikePlatformConstants.HIKE_DAILY_MSISDN, context.getString(R.string.hike_daily_bot), null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.hikedaily, context.getApplicationContext()), 22511, false, context);

		defaultBotEntry(HikePlatformConstants.HIKE_SUPPORT_MSISDN, context.getString(R.string.hike_support_bot), null, null, 2069487, true, context);

		defaultBotEntry(HikePlatformConstants.NATASHA_MSISDN, context.getString(R.string.natasha_bot), null, HikeBitmapFactory.getBase64ForDrawable(R.drawable.natasha, context.getApplicationContext()), 35624943, true, context);

		defaultBotEntry(HikePlatformConstants.CRICKET_HIKE_MSISDN, context.getString(R.string.cricket_bot), HikePlatformConstants.CRICKET_CHAT_THEME_ID,
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

		createBot(jsonObject, Utils.getNetworkShortinOrder(Utils.getNetworkTypeAsString(context)));
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
        
        /*
        * Set up current platform sdk version by getting its value from the database
        * Removing db query from ui thread and setting it up on Hike handler util thread..
        */
        HikeHandlerUtil mThread;
        mThread = HikeHandlerUtil.getInstance();
        mThread.startHandlerThread();

		mThread.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				HikeContentDatabase.getInstance().initSdkMap();

			}
		});
                
        Logger.d("hikeMappInfo", "Keys are " + HikeMessengerApp.hikeMappInfo.keySet() + "------");
        Logger.d("hikeMappInfo", "values are " + HikeMessengerApp.hikeMappInfo.values());
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
	 * Utility method to delete the bot files from the file system
	 * Sample DMapp packet would be of this form :: {
                                "t": "ac",
                                "d": {
                                    "dmapp": [
                                        {
                                          "msisdn":["+hikenews+" ,"+hikecoupons+"]
                                        }]
                                    }
                               }
	 *
	 *
	 * @param jsonObj	:	The bot Json object containing the properties of the bot files to be deleted
	 */
	public static void removeMicroAppFromVersioningPathByMsisdn(final JSONObject jsonObj)
	{
		// Performing deletion operation on Backend thread;
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Code path to be deleted that is being generated after platform versioning release
					JSONArray appsToBeRemoved = jsonObj.getJSONArray(HikePlatformConstants.MSISDN);
					for (int i = 0; i < appsToBeRemoved.length(); i++)
					{
						String msisdn = appsToBeRemoved.get(i).toString();
						BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

						// If botInfo is null i.e bot is not present, ignore this request and continue with further requests
						if (botInfo == null)
							continue;

						byte botType = botInfo.getBotType();
						String microAppVersioningPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS;
						String appName = msisdn.substring(1, msisdn.length() - 1);
						microAppVersioningPath = PlatformUtils.generateMappUnZipPathForBotType(botType, microAppVersioningPath, appName);
						Logger.d("FileSystemAccess", "To delete the file path being used after versioning: " + microAppVersioningPath);

						if (PlatformUtils.deleteDirectory(microAppVersioningPath))
						{
							String sentData = AnalyticsConstants.REMOVE_SUCCESS;
							JSONObject json = new JSONObject();
							json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.REMOVE_MICRO_APP);
							json.putOpt(AnalyticsConstants.REMOVE_MICRO_APP, sentData);
							json.putOpt(AnalyticsConstants.MICRO_APP_ID, appName);
							HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.REMOVE_MICRO_APP, json);
						}
					}
				}
				catch (JSONException e1)
				{
					e1.printStackTrace();
				}
			}
		});

	}


    /*
	 * Utility method to delete the bot files from the file system
	 * Sample DMapp packet would be of this form ::
	 *                         {
                                "t": "ac",
                                "d": {
                                    "dmapp": [
                                        {"appName":["newsappv84" ,"newsappv85"]
                                        }
                                    }
                               }

	 * @param jsonObj	:	The bot Json object containing the properties of the bot files to be deleted
	 */
	public static void removeMicroAppByAppName(final JSONObject jsonObj)
	{
		// Performing deletion operation on Backend thread;
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{

				try
				{
					JSONArray appsToBeRemoved = jsonObj.getJSONArray(HikePlatformConstants.APP_NAME);
						for (int i = 0; i < appsToBeRemoved.length(); i++)
						{
							String appName = appsToBeRemoved.get(i).toString();

                            if(TextUtils.isEmpty(appName))
                                return;

                            String microAppsDirectoryPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS;
							String webMicroAppsPath = PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS, microAppsDirectoryPath,
									appName);
							String mAppsPath = PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS, microAppsDirectoryPath, appName);
							String nativeMicroAppsPath = PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.NATIVE_APPS, microAppsDirectoryPath,
									appName);
							String popupsPath = PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.ONE_TIME_POPUPS, microAppsDirectoryPath,
									appName);

							if (PlatformUtils.deleteDirectory(webMicroAppsPath) || PlatformUtils.deleteDirectory(mAppsPath) || PlatformUtils.deleteDirectory(nativeMicroAppsPath)
									|| PlatformUtils.deleteDirectory(popupsPath))
							{
								String sentData = AnalyticsConstants.REMOVE_SUCCESS;
								JSONObject json = new JSONObject();
								json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.REMOVE_MICRO_APP);
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
		});
	}


	/**
	 * Utility method to create an entry of Bot in bot_table, conversations_table. Also create a conversation on the conversation fragment.
	 * 
	 * @param jsonObj
	 *            The bot Json object containing the properties of the bot to be created
	 */
	public static void createBot(JSONObject jsonObj , int currentNetwork)
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

		if (ContactManager.getInstance().isBlocked(msisdn) && jsonObj.optBoolean(HikePlatformConstants.ENABLE_BOT))
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

            // Check if botInfo generated is null, stop the flow and call cbot failed analytics
            if(botInfo == null)
            {
                PlatformUtils.invalidDataBotAnalytics(botInfo);
                return;
            }

            // Check for rejecting cbot lower version mAppVersionCode and botVersionCode and stop the flow if user already has an upper version of same msisdn bot running
			if (jsonObj.has(HikePlatformConstants.METADATA))
			{
				int currentBotInfoMAppVersionCode = 0, mAppVersionCode = 0, botVersionCode = 0, currentBotVersionCode = 0;

				// Get existing bot version details
				BotInfo currentBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
				if (currentBotInfo != null)
				{
					currentBotInfoMAppVersionCode = currentBotInfo.getMAppVersionCode();
					currentBotVersionCode = currentBotInfo.getVersion();
				}

				// Get received cbot version details for comparison
				if (jsonObj.has(HikePlatformConstants.BOT_VERSION))
				{
					botVersionCode = jsonObj.optInt(HikePlatformConstants.BOT_VERSION);
				}
				JSONObject mdJsonObject = jsonObj.optJSONObject(HikePlatformConstants.METADATA);
				JSONObject cardObjectJson = mdJsonObject.optJSONObject(HikePlatformConstants.CARD_OBJECT);
				if (cardObjectJson != null)
					mAppVersionCode = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE, -1);

				// Ignore the packet and send invalid bot analytics if packet does not contain mAppVersionCode field
                if(mAppVersionCode == -1)
                {
                    PlatformUtils.invalidPacketAnalytics(botInfo);
                    return;
                }
                else if (currentBotInfo != null && (mAppVersionCode < currentBotInfoMAppVersionCode || botVersionCode < currentBotVersionCode
						|| (mAppVersionCode == currentBotInfoMAppVersionCode && botVersionCode == currentBotVersionCode)))
				{
                    /**
                     * If we are rejecting packet but enableBot is set as true for the same, we need to honour that scenario
                     * Notification will be played only if notifType is Silent/Loud
                     */
                    if(enableBot)
                        PlatformUtils.enableBot(currentBotInfo, enableBot, true);

                    if (!HikeConstants.OFF.equals(notifType))
                    {
                        ToastListener.getInstance().showBotDownloadNotification(msisdn, currentBotInfo.getLastMessageText(),notifType.equals(HikeConstants.SILENT));
                    }

                    PlatformUtils.invalidPacketAnalytics(botInfo);
                    Pair<BotInfo,Boolean> botInfoCreatedSuccessfullyPair = new Pair(botInfo,true);
                    HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_CREATED, botInfoCreatedSuccessfullyPair);
                    return;
                }
            }


			NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());
			if(checkIfDownloadInProgress(botMetadata.getAppName()))
			{
				return;
			}

            if (botMetadata.isMicroAppMode())
			{

					PlatformUtils.addToPlatformDownloadStateTable(botMetadata.getAppName(), botMetadata.getmAppVersionCode(), jsonObj.toString(), HikePlatformConstants.PlatformTypes.CBOT,jsonObj.optLong(HikePlatformConstants.TTL, HikePlatformConstants.oneDayInMS),botMetadata.getPrefNetwork(), HikePlatformConstants.PlatformDwnldState.IN_PROGRESS,botMetadata.getAutoResume());
				if(botMetadata.getPrefNetwork() < currentNetwork)
					return; // Restricting download only to better network than pref.
				botInfo.setBotType(HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS);

                // Check to ensure a cbot request for a msisdn does not start processing if one is already in process
                if(!PlatformUtils.assocMappRequestStatusMap.containsKey(botInfo.getMsisdn()))
                    PlatformUtils.processCbotPacketForNonMessagingBot(botInfo, enableBot, botChatTheme, notifType, botMetadata, botMetadata.isResumeSupported());
			}
			else if (botMetadata.isWebUrlMode())
			{
				PlatformUtils.botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
			}
			else if (botMetadata.isNativeMode())
			{

				PlatformUtils.addToPlatformDownloadStateTable(botMetadata.getAppName(), botMetadata.getmAppVersionCode(), jsonObj.toString(),
						HikePlatformConstants.PlatformTypes.CBOT, jsonObj.optLong(HikePlatformConstants.TTL, HikePlatformConstants.oneDayInMS),
						jsonObj.optInt(HikePlatformConstants.PREF_NETWORK, Utils.getNetworkShortinOrder(HikePlatformConstants.DEFULT_NETWORK)),
						HikePlatformConstants.PlatformDwnldState.IN_PROGRESS, botMetadata.getAutoResume());
				if (botMetadata.getPrefNetwork() < currentNetwork)
					return; // Restricting download only to better network than pref.
				botInfo.setBotType(HikePlatformConstants.PlatformBotType.NATIVE_APPS);

                // In case of native micro app we don't need to process any assoc mapp in background, so download micro app packet directly
                PlatformUtils.downloadMicroAppZipForNonMessagingCbotPacket(botInfo, enableBot, botChatTheme, notifType, botMetadata, botMetadata.isResumeSupported());
			}
		}

		Logger.d("create bot", "It takes " + String.valueOf(System.currentTimeMillis() - startTime) + "msecs");
	}

	private static boolean checkIfDownloadInProgress(String appName)
	{
		return HikeContentDatabase.getInstance().isMicroAppDownloadRunning(appName);
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
		if (jsonObj.has(HikePlatformConstants.TRIGGGER_POINT_FOR_MENU))
		{
			int triggerPoint = jsonObj.optInt(HikePlatformConstants.TRIGGGER_POINT_FOR_MENU);
			botInfo.setTriggerPointFormenu(triggerPoint);
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
		
		if (jsonObj.has(HikePlatformConstants.TRIGGGER_POINT_FOR_MENU))
		{
			int trigger = jsonObj.optInt(HikePlatformConstants.TRIGGGER_POINT_FOR_MENU);
			botInfo.setTriggerPointFormenu(trigger);
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

		if(jsonObj.has(HikePlatformConstants.CLIENT_ID)){
			botInfo.setClientId(jsonObj.optString(HikePlatformConstants.CLIENT_ID));
		}
		if(jsonObj.has(HikePlatformConstants.CLIENT_HASH)){
			botInfo.setClientHash(jsonObj.optString(HikePlatformConstants.CLIENT_HASH));
		}
        if (jsonObj.has(HikePlatformConstants.METADATA))
        {
            int mAppVersionCode = 0;
            JSONObject mdJsonObject = jsonObj.optJSONObject(HikePlatformConstants.METADATA);
            JSONObject cardObjectJson = mdJsonObject.optJSONObject(HikePlatformConstants.CARD_OBJECT);
            if(cardObjectJson != null)
                mAppVersionCode = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE,-1);

            if (mAppVersionCode > 0)
            {
                botInfo.setMAppVersionCode(mAppVersionCode);
            }
        }


        return botInfo;

	}

	public static BotInfo getBotInfoFormessagingBots(JSONObject jsonObj, String msisdn)
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

        Pair<BotInfo,Boolean> botInfoCreatedSuccessfullyPair = new Pair(botInfo,true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_CREATED, botInfoCreatedSuccessfullyPair);
		
		/**
		 * Notification will be played only if notifType is Silent/Loud
		 */
		if (!HikeConstants.OFF.equals(notifType))
		{
			ToastListener.getInstance().showBotDownloadNotification(msisdn, botInfo.getLastMessageText(),notifType.equals(HikeConstants.SILENT));
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
	 * This method makes a HTTP Post Call to fetch avatar of a bot if it is not present in the HikeUserDb / Local Storage
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
					if (mBotInfo.isConvPresent() && !isBotDpPresent(mBotInfo.getMsisdn()))
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
							public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
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

	public static void addAllMicroAppMenu(List<OverFlowMenuItem> overFlowMenuItems,int triggerPoint, Context context)
	{
		for (final BotInfo mBotInfo : HikeMessengerApp.hikeBotInfoMap.values())
		{

			if(!mBotInfo.isConvPresent()&&mBotInfo.getTriggerPointFormenu()==triggerPoint){
				 if (mBotInfo.getMsisdn().equalsIgnoreCase(HikeConstants.MicroApp_Msisdn.HIKE_WALLET))
				{
					overFlowMenuItems.add(new OverFlowMenuItem(context.getString(R.string.wallet_menu), 0, 0, R.string.wallet_menu));
				}else if (mBotInfo.getMsisdn().equalsIgnoreCase(HikeConstants.MicroApp_Msisdn.HIKE_RECHARGE))
				{
					overFlowMenuItems.add(new OverFlowMenuItem(context.getString(R.string.recharge_menu), 0, 0, R.string.recharge_menu));
				}
			}
		}
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

	/**
	 * Utility method to check whether a bot DP Exists either with the ContactManager or on the Disk.
	 *
	 * @param msisdn
	 * @return
	 */
	private static boolean isBotDpPresent(String msisdn)
	{
		File botDpFile = new File(getBotThumbnailRootFolder() + msisdn);

		if (!botDpFile.exists())
		{
			//Possibly the Bot DP exists in the HikeUserDb
			return ContactManager.getInstance().hasIcon(msisdn);
		}
		return true;
	}

    /**
     * Is bot url boolean.
     *
     * @param uri
     *            the uri
     * @return the boolean
     */
    public static boolean isBotUrl(Uri uri)
    {
        if (HikeConstants.HIKE_SERVICE.equals(uri.getScheme()) && HikePlatformConstants.BOTS.equals(uri.getAuthority()) && !TextUtils.isEmpty(uri.getQueryParameter(HikeConstants.HANDLE)))
        {
            return true;
        }
        return false;
    }


	public static String getParentMsisdnFromBotMsisdn(String botMsisdn)
	{
		if(TextUtils.isEmpty(botMsisdn))
		{
			return null;
		}
		BotInfo botInfo = getBotInfoForBotMsisdn(botMsisdn);
		if(botInfo == null)
		{
			return null;
		}
		NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());
		return nonMessagingBotMetadata.getParentMsisdn();
	}

	public static JSONObject getBotInfoAsString(BotInfo botInfo) throws JSONException, IOException
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(HikePlatformConstants.BOT_DESCRIPTION, botInfo.getBotDescription());
		jsonObject.put(HikePlatformConstants.BOT_TYPE, botInfo.getBotType());
		jsonObject.put(HikePlatformConstants.HELPER_DATA, botInfo.getHelperData());
		jsonObject.put(HikePlatformConstants.METADATA, botInfo.getMetadata());
		jsonObject.put(HikePlatformConstants.NAMESPACE, botInfo.getNamespace());
		jsonObject.put(HikePlatformConstants.MSISDN, botInfo.getMsisdn());
		jsonObject.put(HikePlatformConstants.MAPP_VERSION_CODE, botInfo.getMAppVersionCode());
		jsonObject.put(HikePlatformConstants.VERSION, botInfo.getVersion());
		jsonObject.put(HikePlatformConstants.NAME, botInfo.getConversationName());
		jsonObject.put(HikePlatformConstants.TYPE, botInfo.getType());
		BitmapDrawable bitmap = HikeMessengerApp.getLruCache().getIconFromCache(botInfo.getMsisdn());
		if(bitmap !=null)
		{
			String picture = Utils.drawableToString(bitmap);
			File botPicFile = new File(HikeMessengerApp.getInstance().getExternalCacheDir(), "bot_"+ botInfo.getMsisdn() + ".jpg");
			if(!botPicFile.exists())
			{
				botPicFile.createNewFile();
				Utils.saveByteArrayToFile(botPicFile, picture.getBytes());
			}
			jsonObject.put("picture", botPicFile.getAbsolutePath());
		}
		else
		{
			jsonObject.put("picture" , "");
		}
		return jsonObject;
	}
	
}
