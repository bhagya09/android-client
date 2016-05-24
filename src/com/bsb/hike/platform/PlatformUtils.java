package com.bsb.hike.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.auth.AuthListener;
import com.bsb.hike.platform.auth.PlatformAuthenticationManager;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.platform.content.PlatformZipDownloader;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.BirthdayUtils;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * @author piyush
 * 
 *         Class for all Utility methods related to Platform code
 */
public class PlatformUtils
{
	private static final String TAG = "PlatformUtils";

	private static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

    public static ConcurrentHashMap<String,Integer> assocMappRequestStatusMap = new ConcurrentHashMap<String,Integer>();

	/**
	 * 
	 * metadata:{'layout_id':'','file_id':'','card_data':{},'helper_data':{}}
	 * 
	 * This function reads helper json given in parameter and update it in metadata of message , it inserts new keys in metadata present in helper and updates old
	 */
	public static String updateHelperData(String helper, String originalMetadata)
	{

		if (originalMetadata != null)
		{
			try
			{
				JSONObject metadataJSON = new JSONObject(originalMetadata);
				JSONObject helperData = new JSONObject(helper);
				JSONObject cardObj = metadataJSON.optJSONObject(HikePlatformConstants.CARD_OBJECT);
				JSONObject oldHelper = cardObj.optJSONObject(HikePlatformConstants.HELPER_DATA);
				JSONObject newHelperData = mergeJSONObjects(oldHelper, helperData);
				cardObj.put(HikePlatformConstants.HELPER_DATA, newHelperData);
				metadataJSON.put(HikePlatformConstants.CARD_OBJECT, cardObj);
				originalMetadata = metadataJSON.toString();
				return originalMetadata;
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Caught a JSON Exception in UpdateHelperMetadata" + e.toString());
				e.printStackTrace();
			}
		}
		else
		{
			Logger.e(TAG, "Meta data is null in UpdateHelperData");
		}
		return null;
	}

	/**
	 * Call this function to merge two JSONObjects. Will iterate for the keys present in the dataDiff. Will add the key in the oldData if not already present or will update the
	 * value in oldData if the key is present.
	 * 
	 * @param oldData
	 *            : the data that wants to be merged.
	 * @param dataDiff
	 *            : the diff that will be merged with the old data.
	 * @return : the merged data.
	 */
	public static JSONObject mergeJSONObjects(JSONObject oldData, JSONObject dataDiff)
	{
		if (oldData == null)
		{
			oldData = new JSONObject();
		}
		Iterator<String> i = dataDiff.keys();
		while (i.hasNext())
		{
			String key = i.next();
			try
			{
				oldData.put(key, dataDiff.get(key));
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Caught a JSON Exception while merging helper data" + e.toString());
			}
		}
		return oldData;
	}

	public static void openActivity(Activity context, String data)
	{
		String activityName = null;
		JSONObject mmObject = null;

		if (context == null || TextUtils.isEmpty(data))
		{
			Logger.e(TAG, "Either activity is null or data is empty/null in openActivity");
			return;
		}

		try
		{
			mmObject = new JSONObject(data);
			activityName = mmObject.optString(HikeConstants.SCREEN);

			if (activityName.equals(HIKESCREEN.SETTING.toString()))
			{
				IntentFactory.openSetting(context);
			}

			if (activityName.equals(HIKESCREEN.ACCOUNT.toString()))
			{
				IntentFactory.openSettingAccount(context);
			}
			if (activityName.equals(HIKESCREEN.FREE_SMS.toString()))
			{
				IntentFactory.openSettingSMS(context);
			}
			if (activityName.equals(HIKESCREEN.MEDIA.toString()))
			{
				IntentFactory.openSettingMedia(context);
			}
			if (activityName.equals(HIKESCREEN.NOTIFICATION.toString()))
			{
				IntentFactory.openSettingNotification(context);
			}
			if (activityName.equals(HIKESCREEN.PRIVACY.toString()))
			{
				IntentFactory.openSettingPrivacy(context);
			}
			if (activityName.equals(HIKESCREEN.TIMELINE.toString()))
			{
				IntentFactory.openTimeLine(context);
			}
			if (activityName.equals(HIKESCREEN.NEWGRP.toString()))
			{
				context.startActivity(new Intent(context, CreateNewGroupOrBroadcastActivity.class));
			}
			if (activityName.equals(HIKESCREEN.INVITEFRNDS.toString()))
			{
				context.startActivity(new Intent(context, TellAFriend.class));
			}
			if (activityName.equals(HIKESCREEN.REWARDS_EXTRAS.toString()))
			{
				context.startActivity(IntentFactory.getRewardsIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STICKER_SHOP.toString()))
			{
				context.startActivity(IntentFactory.getStickerShopIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STICKER_SHOP_SETTINGS.toString()))
			{
				context.startActivity(IntentFactory.getStickerSettingIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STATUS.toString()))
			{
				context.startActivity(new Intent(context, StatusUpdate.class));
			}
			if (activityName.equals(HIKESCREEN.HIDDEN_MODE.toString()))
			{
				if (context instanceof HomeActivity)
				{
					((HomeActivity) context).hikeLogoClicked();
				}
			}
			if (activityName.equals(HIKESCREEN.COMPOSE_CHAT.toString()))
			{
				if (mmObject.optBoolean(AnalyticsConstants.BOT_DISCOVERY, false))
				{
					context.startActivity(IntentFactory.getComposeChatIntentWithBotDiscovery(context));
				}
				else
				{
					context.startActivity(IntentFactory.getComposeChatIntent(context));
				}
			}
			if (activityName.equals(HIKESCREEN.COMPOSE_CHAT_WITH_BDAY.toString()))
			{
				if (mmObject.has(HikeConstants.MSISDNS)) {
					BirthdayUtils.saveBirthdaysFromTip(mmObject);
				}
				context.startActivity(IntentFactory.getComposeChatIntent(context));
			}
			if (activityName.equals(HIKESCREEN.INVITE_SMS.toString()))
			{
				boolean selectAll = mmObject.optBoolean(ProductPopupsConstants.SELECTALL, false);
				Intent intent = new Intent(context, HikeListActivity.class);
				intent.putExtra(ProductPopupsConstants.SELECTALL, selectAll);
				context.startActivity(intent);
			}
			if (activityName.equals(HIKESCREEN.FAVOURITES.toString()))
			{
				context.startActivity(IntentFactory.getFavouritesIntent(context));
			}
			if (activityName.equals(HIKESCREEN.HOME_SCREEN.toString()))
			{
				context.startActivity(Utils.getHomeActivityIntent(context));
			}
			if (activityName.equals(HIKESCREEN.PROFILE_PHOTO.toString()))
			{

				Intent intent = IntentFactory.getProfileIntent(context);
				if (mmObject.optBoolean(ProductPopupsConstants.SHOW_CAMERA, false))
				{
					intent.putExtra(ProductPopupsConstants.SHOW_CAMERA, true);
				}
				context.startActivity(intent);

			}
			if (activityName.equals(HIKESCREEN.EDIT_PROFILE.toString()))
			{
				Intent intent = IntentFactory.getProfileIntent(context);
				intent.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
				context.startActivity(intent);

			}
			if (activityName.equals(HIKESCREEN.PROFILE_DOB.toString()))
			{
				Intent intent = IntentFactory.getProfileIntent(context);
				intent.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
				intent.putExtra(HikeConstants.Extras.PROFILE_DOB, true);
				context.startActivity(intent);

			}
			if (activityName.equals(HIKESCREEN.INVITE_WHATSAPP.toString()))
			{
				IntentFactory.openInviteWatsApp(context);
			}
			if (activityName.equals(HIKESCREEN.OPENINBROWSER.toString()))
			{
				String url = mmObject.optString(HikeConstants.URL);

				if (!TextUtils.isEmpty(url))
				{
					Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					context.startActivity(in);
				}
			}
			if (activityName.equals(HIKESCREEN.OPENAPPSTORE.toString()))
			{
				String url = mmObject.optString(HikeConstants.URL);

				if (!TextUtils.isEmpty(url))
				{
					IntentFactory.launchPlayStore(url, context);
				}
			}
			if (activityName.equals(HIKESCREEN.HELP.toString()))
			{
				IntentFactory.openSettingHelp(context);
			}
			if (activityName.equals(HIKESCREEN.NUXINVITE.toString()))
			{
				context.startActivity(IntentFactory.openNuxFriendSelector(context));
			}
			if (activityName.equals(HIKESCREEN.NUXREMIND.toString()))
			{
				context.startActivity(IntentFactory.openNuxCustomMessage(context));
			}
			if (activityName.equals(HIKESCREEN.BROADCAST.toString()))
			{
				IntentFactory.createBroadcastIntent(context);
			}
			if (activityName.equals(HIKESCREEN.CHAT_HEAD.toString()))
			{
				if (ChatHeadUtils.areWhitelistedPackagesSharable(context))
				{
					boolean show_popup = mmObject.optBoolean(ProductPopupsConstants.NATIVE_POPUP, false);
					Intent intent = IntentFactory.getStickerShareSettingsIntent(context);
					intent.putExtra(ProductPopupsConstants.NATIVE_POPUP, show_popup);
					context.startActivity(intent);
				}
				else
				{
					Toast.makeText(context, context.getString(R.string.sticker_share_popup_not_activate_toast), Toast.LENGTH_LONG).show();
				}
			}
			if (activityName.equals(HIKESCREEN.HIKE_CALLER.toString()))
			{
				Utils.setSharedPrefValue(context, HikeConstants.ACTIVATE_STICKY_CALLER_PREF, true);
				ChatHeadUtils.registerCallReceiver();
				IntentFactory.openStickyCallerSettings(context, false);
			}
			if (activityName.equals(HIKESCREEN.ACCESS.toString()))
			{
				IntentFactory.openAccessibilitySettings(context);
			}
			if (activityName.equals(HIKESCREEN.GAME_ACTIVITY.toString()))
			{
				String extraData;
				String msisdn = mmObject.optString(HikeConstants.MSISDN);
				extraData = mmObject.optString(HikeConstants.DATA);
				Intent i = IntentFactory.getNonMessagingBotIntent(msisdn, context, extraData);
				if (context != null)
				{
					if (!(getLastGame().equals(msisdn)))
					{
						killProcess(context, HikePlatformConstants.GAME_PROCESS);
						Logger.d(TAG, "process killed");
					}
					HikeContentDatabase.getInstance().putInContentCache(HikePlatformConstants.LAST_GAME,
							BotUtils.getBotInfoForBotMsisdn(HikePlatformConstants.GAME_CHANNEL).getNamespace(), msisdn);
					context.startActivity(i);
				}
			}
			if (activityName.equals(HIKESCREEN.OPEN_MICROAPP.toString()))
			{
				Intent intent = IntentFactory.getNonMessagingBotIntent(mmObject.getString(HikeConstants.MSISDN), context);
				intent.putExtra(HikePlatformConstants.EXTRA_DATA, mmObject.optString(HikePlatformConstants.EXTRA_DATA));
				context.startActivity(intent);
			}
			if (activityName.equals(HIKESCREEN.CHAT_THREAD.toString()))
			{
				String msisdn = mmObject.optString("msisdn");
				if (TextUtils.isEmpty(msisdn))
				{
					Logger.e(TAG, "Msisdn is missing in the packet");
					return;
				}
				if(StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive() &&
						PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED, false))
				{
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_SHOW_REPEATED, true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
					return;
				}
				Intent in = IntentFactory.getIntentForAnyChatThread(context, msisdn, mmObject.optBoolean("isBot"),
						ChatThreadActivity.ChatThreadOpenSources.MICRO_APP);
				if (in != null)
				{
					if(mmObject.has(HikeConstants.Extras.MSG))
					{
						String preTypedText = mmObject.optString(HikeConstants.Extras.MSG);
						if(!TextUtils.isEmpty(preTypedText))
						{
							in.putExtra(HikeConstants.Extras.MSG, preTypedText);
						}
					}
					context.startActivity(in);
				}
				else
				{
					Toast.makeText(context, context.getString(R.string.app_not_enabled), Toast.LENGTH_SHORT).show();
				}
			}
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in openActivity : " + e.getMessage());
			e.printStackTrace();
		}
		catch (ActivityNotFoundException e)
		{
			Toast.makeText(context, "No activity found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

    /*
     * Method to download assoc mapp (helper files) requested with cbot packet.
     */
	public static void downloadAssocMappForNonMessagingBot(final JSONObject assocMappJson, final BotInfo botInfo, final boolean enableBot, final String botChatTheme,
			final String notifType, final NonMessagingBotMetadata botMetadata, final boolean resumeSupport, final boolean autoResume)
	{

		final PlatformContentModel platformContentModel = PlatformContentModel.make(assocMappJson.toString(), HikePlatformConstants.PlatformBotType.HIKE_MAPPS);
		PlatformContentRequest rqst = PlatformContentRequest.make(platformContentModel, new PlatformContentListener<PlatformContentModel>()
		{
			long fileLength = 0;

			@Override
			public void onComplete(PlatformContentModel content)
			{
				Logger.d(TAG, "microapp download packet success.");
				// Store successful micro app creation in db
				mAppCreationSuccessHandling(assocMappJson);
				assocMappDownloadHandling(botInfo, enableBot, botChatTheme, notifType, botMetadata, resumeSupport,autoResume);
			}

			@Override
			public void onEventOccured(int uniqueId, PlatformContent.EventCode event)
			{
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikePlatformConstants.ERROR_CODE, event.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
				{
					microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, platformContentModel, jsonObject);
					Logger.d(TAG, "microapp already exists.");
					assocMappDownloadHandling(botInfo, enableBot, botChatTheme, notifType, botMetadata, resumeSupport,autoResume);
				}
				else
				{
					try
					{
						if (fileLength > 0)
						{
							jsonObject.put(AnalyticsConstants.FILE_SIZE, String.valueOf(fileLength));
						}
						jsonObject.put(AnalyticsConstants.INTERNAL_STORAGE_SPACE, String.valueOf(Utils.getFreeInternalStorage()) + " MB");
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "JSONException " + e.getMessage());
					}

                    // In case of assoc mapp failure, remove entry from hashmap
                    assocMappRequestStatusMap.remove(botInfo.getMsisdn());
					microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOAD_FAILED, platformContentModel, jsonObject);
					Logger.wtf(TAG, "microapp download packet failed.Because it is" + event.toString());
				}

			}

			@Override
			public void downloadedContentLength(long length)
			{
				fileLength = length;
			}
		});

        // As this flow is there for MAPP flow, setting the request type to Hike Mapps
        rqst.setBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS);
        rqst.getContentData().setBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS);

		// Setting up parameters for downloadAndUnzip call
		boolean isTemplatingEnabled = false;
		boolean doReplace = false;
		String callbackId = null;
		String assocCbot = "";

		downloadAndUnzip(rqst, isTemplatingEnabled, doReplace, callbackId, resumeSupport, assocCbot,autoResume);
	}

    /**
     * download the microapp and then set the state to whatever that has been passed by the server.
     *
     * @param botInfo
     * @param enableBot
     */
    public static void processCbotPacketForNonMessagingBot(final BotInfo botInfo, final boolean enableBot, final String botChatTheme, final String notifType,
			NonMessagingBotMetadata botMetadata, boolean resumeSupport)
	{
		boolean autoResume=botMetadata.getAutoResume();
        // On receiving request to process cbot packet , add entry in assocMapp requests map with initial count as no of mapps request to be completed
        if (botMetadata != null && botMetadata.getAsocmapp() != null)
		{
			JSONArray assocMappJsonArray = botMetadata.getAsocmapp();

			int assocMappsCount = assocMappJsonArray.length();

            // add entry in assocMapp requests map with initial count as no of mapps request to be completed
            assocMappRequestStatusMap.put(botInfo.getMsisdn(),assocMappsCount);
            for (int i = 0; i < assocMappsCount; i++)
			{
				// Get assoc mapp json object from cbot json array
				JSONObject assocMappJson = null;
				try
				{
					assocMappJson = assocMappJsonArray.getJSONObject(i);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

                // Code to check if micro app already exists or not and make download micro app call if assoc map already exist
				if (assocMappJson != null)
				{
					JSONObject cardObjectJson = assocMappJson.optJSONObject(HikePlatformConstants.CARD_OBJECT);
					if (cardObjectJson != null)
					{
						final int version = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE, 0);
						final String appName = cardObjectJson.optString(HikePlatformConstants.APP_NAME, "");

						if (isMicroAppExistForMappPacket(appName, version) && i == assocMappsCount - 1)
						{
                            assocMappRequestStatusMap.remove(botInfo.getMsisdn());
                            // Download micro app for non messaging bot
							downloadMicroAppZipForNonMessagingCbotPacket(botInfo, enableBot, botChatTheme, notifType, botMetadata, resumeSupport);
						}

						else if (!isMicroAppExistForMappPacket(appName, version))//Download Associated mapp
						{
							downloadAssocMappForNonMessagingBot(assocMappJson,botInfo,enableBot,botChatTheme,notifType,botMetadata,resumeSupport,autoResume);
						}
					}
				}
			}
		}

        else // No AssocMapp array found. Reverting to default behaviour.
        {
            // Download micro app for non messaging bot
            downloadMicroAppZipForNonMessagingCbotPacket(botInfo,enableBot,botChatTheme,notifType,botMetadata,resumeSupport);
        }
	}


    /*
     * method to download the micro app zip as per cbot packet
     */
	public static void downloadMicroAppZipForNonMessagingCbotPacket(final BotInfo botInfo, final boolean enableBot, final String botChatTheme, final String notifType,
			final NonMessagingBotMetadata botMetadata, boolean resumeSupport)
	{
		boolean autoResume = botMetadata.getAutoResume();
		PlatformContentRequest rqst = PlatformContentRequest.make(PlatformContentModel.make(botInfo.getMetadata(), botInfo.getBotType()),
				new PlatformContentListener<PlatformContentModel>()
				{

					long zipFileSize = 0;

					@Override
					public void onComplete(PlatformContentModel content)
					{
						Logger.d(TAG, "microapp download packet success.");
						botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
					}

					@Override
					public void onEventOccured(int uniqueCode, PlatformContent.EventCode event)
					{
						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							// do nothing
							return;
						}
						else if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
						{
							Logger.d(TAG, "microapp already exists");
							botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
						}
						else
						{
							if(botMetadata.getAutoResume() && !(PlatformContent.EventCode.UNZIP_FAILED.toString().equals(event.toString())) && !(PlatformContent.EventCode.INCOMPLETE_ZIP_DOWNLOAD.toString().equals(event.toString())))
							{
								// In case of failure updating status
								updatePlatformDownloadState(botMetadata.getAppName(), botMetadata.getmAppVersionCode(), HikePlatformConstants.PlatformDwnldState.FAILED);
								sendDownloadPausedAnalytics(botMetadata.getAppName());
							}
							else {
								Pair<BotInfo, Boolean> botInfoCreationFailedPair = new Pair(botInfo, false);
								HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_CREATED, botInfoCreationFailedPair);
								Logger.wtf(TAG, "microapp download packet failed." + event.toString());
								JSONObject json = new JSONObject();
								try {
									json.put(HikePlatformConstants.ERROR_CODE, event.toString());
									if (zipFileSize > 0) {
										json.put(AnalyticsConstants.FILE_SIZE, String.valueOf(zipFileSize));
									}
									json.put(AnalyticsConstants.INTERNAL_STORAGE_SPACE, String.valueOf(Utils.getFreeInternalStorage()) + " MB");
									createBotAnalytics(HikePlatformConstants.BOT_CREATION_FAILED, botInfo, json);
									createBotMqttAnalytics(HikePlatformConstants.BOT_CREATION_FAILED_MQTT, botInfo, json);

								} catch (JSONException e) {
									e.printStackTrace();
								}
							}

						}
					}

					@Override
					public void downloadedContentLength(long length)
					{
						zipFileSize = length;
					}
				});

		// Stop the flow and return from here in case any exception occurred and contentData becomes null
		if (rqst.getContentData() == null)
		{
            invalidDataBotAnalytics(botInfo);
            Logger.e(TAG, "Stop the micro app download flow for incorrect request");
			return;
		}


		rqst.setBotType(botInfo.getBotType());
		rqst.getContentData().cardObj.setmAppVersionCode(botInfo.getMAppVersionCode());
		rqst.getContentData().setBotType(botInfo.getBotType());
		rqst.getContentData().setMsisdn(botInfo.getMsisdn());

		downloadAndUnzip(rqst, false, botMetadata.shouldReplace(), botMetadata.getCallbackId(), resumeSupport, "", autoResume);
	}
    

	public static void botCreationSuccessHandling(BotInfo botInfo, boolean enableBot, String botChatTheme, String notifType)
	{
		enableBot(botInfo, enableBot, true);
		BotUtils.updateBotParamsInDb(botChatTheme, botInfo, enableBot, notifType);
		createBotAnalytics(HikePlatformConstants.BOT_CREATED, botInfo);
		createBotMqttAnalytics(HikePlatformConstants.BOT_CREATED_MQTT, botInfo);
		// Removing from download state table on success
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
		if (metadata != null)
		{
			removeFromPlatformDownloadStateTable(metadata.getAppName(), metadata.getmAppVersionCode());
		}
	}

	private static void createBotMqttAnalytics(String key, BotInfo botInfo)
	{
		createBotMqttAnalytics(key, botInfo, null);
	}

	public static void requestAuthToken(BotInfo botInfo, AuthListener authListener, int tokenLife){
		PlatformAuthenticationManager manager =new PlatformAuthenticationManager(botInfo.getClientId(), botInfo.getMsisdn(), HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null), HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null),authListener);
		manager.requestAuthToken(tokenLife);
	}
	private static void createBotMqttAnalytics(String key, BotInfo botInfo, JSONObject json)
	{

		try
		{
			JSONObject metadata = json == null ? new JSONObject() : new JSONObject(json.toString());
			JSONObject data = new JSONObject();
			data.put(HikeConstants.EVENT_TYPE, String.valueOf(botInfo.getMAppVersionCode()));

			metadata.put(HikeConstants.EVENT_KEY, key);
			metadata.put(AnalyticsConstants.BOT_NAME, botInfo.getConversationName());
			metadata.put(AnalyticsConstants.BOT_MSISDN, botInfo.getMsisdn());
			metadata.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			metadata.put(AnalyticsConstants.NETWORK_TYPE, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
			metadata.put(AnalyticsConstants.APP_VERSION, AccountUtils.getAppVersion());

			data.put(HikeConstants.METADATA, metadata);

			Utils.sendLogEvent(data, AnalyticsConstants.DOWNLOAD_EVENT, null);
		}
		catch (JSONException e)
		{
			Logger.w("LE", "Invalid json");
		}
	}

	private static void createBotAnalytics(String key, BotInfo botInfo)
	{
		createBotAnalytics(key, botInfo, null);
	}

    public static void createBotAnalytics(String key, BotInfo botInfo, JSONObject json)
	{
		if (json == null)
		{
			json = new JSONObject();
		}
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, key);
			json.put(AnalyticsConstants.BOT_NAME, botInfo.getConversationName());
			json.put(AnalyticsConstants.BOT_MSISDN, botInfo.getMsisdn());
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			HikeAnalyticsEvent.analyticsForNonMessagingBots(String.valueOf(botInfo.getMAppVersionCode()), AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void enableBot(BotInfo botInfo, boolean enableBot, boolean increaseUnread)
	{
		if (enableBot && botInfo.isNonMessagingBot())
		{
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(botInfo);
			Utils.rearrangeChat(botInfo.getMsisdn(), true, increaseUnread);
		}
	}

	/**
	 * Method used download the microapp for mapp packet flow, can be used by nonmessaging as well as messaging only to download and unzip the app.
	 * 
	 * @param downloadData
	 *            : the data used to download microapp from ac packet to download the app.
	 */
	public static void downloadZipFromPacket(final JSONObject downloadData, int currentNetwork)
	{
		if (downloadData == null)
		{
			return;
		}
		final boolean autoResume = downloadData.optBoolean(HikePlatformConstants.AUTO_RESUME, false);

        // Check here to reject a mapp packet if its latest version is already present on device
        int currentMappVersionCode = 0,mAppVersionCode = 0;
        JSONObject cardObjectJson = downloadData.optJSONObject(HikePlatformConstants.CARD_OBJECT);
        String appName = cardObjectJson.optString(HikePlatformConstants.APP_NAME);
        if (cardObjectJson != null)
            mAppVersionCode = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE, -1);
        if(HikeMessengerApp.hikeMappInfo.containsKey(appName))
            currentMappVersionCode = HikeMessengerApp.hikeMappInfo.get(appName);
		if (mAppVersionCode <= currentMappVersionCode)
		{
            // Ignore the packet if data is already present on device and fire pubsub for the same
            Pair<BotInfo, Boolean> mAppCreatedSuccessfullyPair = new Pair(appName, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MAPP_CREATED, mAppCreatedSuccessfullyPair);
            return;
		}

        final PlatformContentModel platformContentModel = PlatformContentModel.make(downloadData.toString(), HikePlatformConstants.PlatformBotType.HIKE_MAPPS);
		PlatformContentRequest rqst = PlatformContentRequest.make(platformContentModel, new PlatformContentListener<PlatformContentModel>()
		{
			long fileLength = 0;

			@Override
			public void onComplete(PlatformContentModel content)
			{
				microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, content);
				Logger.d(TAG, "microapp download packet success.");
                // Store successful micro app creation in db
                mAppCreationSuccessHandling(downloadData);
				// Removing from state table in case of Download success
				removeFromPlatformDownloadStateTable(platformContentModel.getId(), platformContentModel.cardObj.getmAppVersionCode());
			}

			@Override
			public void onEventOccured(int uniqueId, PlatformContent.EventCode event)
			{

				if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
				{
					// do nothing
					return;
				}

				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikePlatformConstants.ERROR_CODE, event.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
				{
					microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, platformContentModel, jsonObject);
					Logger.d(TAG, "microapp already exists.");
				}
				else
				{
                    try
					{
						if (fileLength > 0)
						{
							jsonObject.put(AnalyticsConstants.FILE_SIZE, String.valueOf(fileLength));
						}
						jsonObject.put(AnalyticsConstants.INTERNAL_STORAGE_SPACE, String.valueOf(Utils.getFreeInternalStorage()) + " MB");
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "JSONException " + e.getMessage());
					}
                    // Publish pubsub for failure case of mapp packet received
                    JSONObject cardObjectJson = downloadData.optJSONObject(HikePlatformConstants.CARD_OBJECT);

                    if (cardObjectJson != null) {
                        final String appName = cardObjectJson.optString(HikePlatformConstants.APP_NAME, "");
                        // Publish pubsub for failed creation of mapp packet received
                        Pair<BotInfo,Boolean> mAppCreatedSuccessfullyPair = new Pair(appName,false);
                        HikeMessengerApp.getPubSub().publish(HikePubSub.MAPP_CREATED, mAppCreatedSuccessfullyPair);
                    }
					//Updating state in case of failure
					if (autoResume && !(PlatformContent.EventCode.UNZIP_FAILED.toString().equals(event.toString())))
					{

						updatePlatformDownloadState(platformContentModel.getId(), platformContentModel.cardObj.getmAppVersionCode(),
								HikePlatformConstants.PlatformDwnldState.FAILED);
						sendDownloadPausedAnalytics(platformContentModel.getId());
					}
					if (!autoResume)
					{
						PlatformUtils.removeFromPlatformDownloadStateTable(platformContentModel.getId(), platformContentModel.cardObj.getmAppVersionCode());
					}
					else
					{
						microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOAD_FAILED, platformContentModel, jsonObject);
						Logger.wtf(TAG, "microapp download packet failed.Because it is" + event.toString());
					}
				}
			}

			@Override
			public void downloadedContentLength(long length)
			{
				fileLength = length;
			}
		});

		// Stop the flow and return from here in case any exception occurred and contentData becomes null
        if (rqst.getContentData() == null)
        {
            Logger.e(TAG,"Stop the micro app download flow for incorrect request");
            return;
        }

        // As this flow is there for MAPP flow, setting the request type to Hike Mapps
        boolean isWebCard = downloadData.optBoolean(HikePlatformConstants.IS_WEB_CARD, false);
        if(!isWebCard)
        {
            rqst.setBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS);
            rqst.getContentData().setBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS);
        }

		boolean doReplace = downloadData.optBoolean(HikePlatformConstants.REPLACE_MICROAPP_VERSION);
		String callbackId = downloadData.optString(HikePlatformConstants.CALLBACK_ID);
		boolean resumeSupported = downloadData.optBoolean(HikePlatformConstants.RESUME_SUPPORTED);
		String assoc_cbot = downloadData.optString(HikePlatformConstants.ASSOCIATE_CBOT, "");
		int prefNetwork = downloadData.optInt(HikePlatformConstants.PREF_NETWORK, Utils.getNetworkShortinOrder(HikePlatformConstants.DEFULT_NETWORK));
		if (autoResume)
		{
			resumeSupported = true;
		}
			PlatformUtils.addToPlatformDownloadStateTable(rqst.getContentData().getId(), rqst.getContentData().cardObj.getmAppVersionCode(), downloadData.toString(), HikePlatformConstants.PlatformTypes.MAPP,
					downloadData.optLong(HikePlatformConstants.TTL, HikePlatformConstants.oneDayInMS), downloadData.optInt(HikePlatformConstants.PREF_NETWORK, Utils.getNetworkShortinOrder(HikePlatformConstants.DEFULT_NETWORK)), HikePlatformConstants.PlatformDwnldState.IN_PROGRESS,autoResume);
		if(currentNetwork <= 0 || prefNetwork < currentNetwork)
		{
			return;    // Do not download if current network is below preferred network.
		}
		downloadAndUnzip(rqst, false, doReplace, callbackId, resumeSupported, assoc_cbot, autoResume);
	}

	private static void microappDownloadAnalytics(String key, PlatformContentModel content)
	{
		microappDownloadAnalytics(key, content, null);
	}

	private static void microappDownloadAnalytics(String key, PlatformContentModel content, JSONObject json)
	{
		if (json == null)
		{
			json = new JSONObject();
		}

		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, key);
			json.put(AnalyticsConstants.APP_NAME, content.getId());
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Sample log lines : { "t": "le_android", "d": { "et": "nonUiEvent", "st": "dwnld", "ep": "HIGH", "cts": 1453620927336, "tag": "plf", "md": { "ek": "micro_app", "event":
	 * "exception_track", "fld1": "java.io.FileNotFoundException: abc", "fld2": "hikenewsv14", "fld4" : "true", "platformUid": "VTBoRgRzkEkRVAu3", "networkType": "1", "app_version": "4.1.0.36",
	 * "sid": 1453620914078 } } }
	 *
	 * @param appName
	 * @param errorMsg
	 */
	public static void microappIOFailedAnalytics(String appName, String errorMsg, boolean isReadException)
	{
		try
		{
			JSONObject json = new JSONObject();

			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
			json.put(AnalyticsConstants.EVENT, "exception_track");
			json.put(AnalyticsConstants.LOG_FIELD_1, errorMsg); //Error
			json.put(AnalyticsConstants.LOG_FIELD_2, appName); //App Name
			json.put(AnalyticsConstants.LOG_FIELD_4, Boolean.toString(isReadException)); //App Name
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled, boolean doReplace, String callbackId, boolean resumeSupported, String assocCbot,boolean autoResume)
	{
        // Parameters to call if micro app already exists method and stop the micro app downloading flow in this case
        String mAppName = request.getContentData().cardObj.getAppName();
        int mAppVersionCode = request.getContentData().cardObj.getmAppVersionCode();
        byte botType = request.getBotType();
        String msisdn = request.getContentData().getMsisdn();

        // Code to check if micro app already exists on device based on its bot type and code path structure
		boolean isMicroAppExist = false;
        switch (botType)
        {
            case HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS:
                isMicroAppExist = PlatformUtils.isMicroAppExist(mAppName, mAppVersionCode, msisdn, botType);
                break;
            case HikePlatformConstants.PlatformBotType.ONE_TIME_POPUPS:
                isMicroAppExist = PlatformUtils.isMicroAppExistForPopUps(mAppName, botType);
                break;
            case HikePlatformConstants.PlatformBotType.NATIVE_APPS:
                isMicroAppExist = PlatformUtils.isMicroAppExist(mAppName, mAppVersionCode, msisdn, botType);
                break;
            case HikePlatformConstants.PlatformBotType.HIKE_MAPPS:
                isMicroAppExist = PlatformUtils.isMicroAppExistForMappPacket(mAppName, mAppVersionCode);
                break;
        }
        if (!isMicroAppExist)
		{
            PlatformZipDownloader downloader = new PlatformZipDownloader.Builder().setArgRequest(request).setIsTemplatingEnabled(isTemplatingEnabled)
                    .setCallbackId(callbackId).setResumeSupported(resumeSupported).setAssocCbotMsisdn(assocCbot).setAutoResume(autoResume).createPlatformZipDownloader();
            downloader.downloadAndUnzip();
		}
		else
		{
			request.getListener().onEventOccured(request.getContentData() != null ? request.getContentData().getUniqueId() : 0, PlatformContent.EventCode.ALREADY_DOWNLOADED);
		}
	}

	/**
	 * Creating a forwarding message for Non-messaging microApp
	 * 
	 * @param metadata
	 *            : the metadata made after merging the json given by the microApp
	 * @param text
	 *            : hm text
	 * @return
	 */
	public static ConvMessage getConvMessageFromJSON(JSONObject metadata, String text, String msisdn) throws JSONException
	{

		ConvMessage convMessage = Utils.makeConvMessage(msisdn, true);
		convMessage.setMessage(text);
		convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
		convMessage.webMetadata = new WebMetadata(PlatformContent.getForwardCardData(metadata.toString()));

        // Added check for solving bug for forward card scenario from platform sdk decoupling
        JSONObject cardObj = convMessage.webMetadata.getCardobj();
        JSONObject ldJson = cardObj.getJSONObject(HikePlatformConstants.LAYOUT_DATA);
        ldJson.put(HikePlatformConstants.PLATFORM_SDK_PATH,"");
        cardObj.put(HikePlatformConstants.LAYOUT_DATA,ldJson);
        convMessage.webMetadata.setCardobj(cardObj);

		convMessage.setMsisdn(msisdn);
		return convMessage;

	}

	public static byte[] prepareFileBody(String filePath)
	{
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";
		File file = new File(filePath);
		if (file.exists() && !file.isDirectory())
		{
			int chunkSize = (int) file.length();
			String boundaryMessage = getBoundaryMessage(filePath);
			byte[] fileContent = new byte[(int) file.length()];
			FileInputStream fileInputStream = null;
			try
			{
				fileInputStream = new FileInputStream(file);
				fileInputStream.read(fileContent);
			}
			catch (IOException | NullPointerException e)
			{
				Logger.e("fileUplaod", "file body not present");
				return null;
			}
			finally
			{
				try
				{
					if (fileInputStream != null)
					{
						fileInputStream.close();
					}
				}
				catch (IOException e)
				{
					Logger.e("fileUpload", "Couldn't Read File");
				}
			}
			return setupFileBytes(boundaryMessage, boundary, chunkSize, fileContent);
		}
		else
		{
			Logger.e("fileUpload", "Invalid file Path");
			return null;
		}
	}

	public static void uploadFile(final String filePath, final String url, final IFileUploadListener fileListener)
	{
		if (filePath == null)
		{
			Logger.d("FileUpload", "File Path specified as null");
			fileListener.onRequestFailure("File Path null");
		}
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();

		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				byte[] fileBytes = prepareFileBody(filePath);
				if (fileBytes != null)
				{
					String response = send(fileBytes, filePath, url, fileListener);
					Logger.d("FileUpload", response);
				}
				else
				{

					Logger.e("fileUpload", "Empty File Body");
					return;
				}
			}
		});

	}

	private static String send(byte[] fileBytes, final String filePath, final String url, IFileUploadListener filelistener)
	{
		HttpClient client = AccountUtils.getClient(null);
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
		long so_timeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT, 180 * 1000l);
		Logger.d("UploadFileTask", "Socket timeout = " + so_timeout);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, (int) so_timeout);
		client.getParams().setParameter(CoreConnectionPNames.TCP_NODELAY, true);
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());

		HttpPost post = new HttpPost(url);
		String res = null;
		int resCode = 0;
		try
		{
			post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

			HikeSharedPreferenceUtil mpref = HikeSharedPreferenceUtil.getInstance();
			String platformUID = mpref.getData(HikeMessengerApp.PLATFORM_UID_SETTING, null);
			String platformToken = mpref.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null);
			if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
			{
				post.addHeader(HttpHeaderConstants.COOKIE_HEADER_NAME, HikePlatformConstants.PLATFORM_TOKEN + "=" + platformToken + "; " + HikePlatformConstants.PLATFORM_USER_ID
						+ "=" + platformUID);
			}

			post.setEntity(new ByteArrayEntity(fileBytes));
			HttpResponse response = client.execute(post);
			Logger.d("FileUpload", response.toString());
			resCode = response.getStatusLine().getStatusCode();

			res = EntityUtils.toString(response.getEntity());
			Logger.d("FileUpload", "" + resCode);
		}
		catch (IOException | NullPointerException ex)
		{
			Logger.e("FileUpload", ex.toString());
			filelistener.onRequestFailure(ex.toString());
			return ex.toString();
		}
		Logger.d("FileUpload", res);
		if (resCode == 200)
		{
			filelistener.onRequestSuccess(res);
		}
		else
		{
			filelistener.onRequestFailure(res);
		}
		return res;
	}

	/*
	 * gets the boundary message for the file path
	 */
	private static String getBoundaryMessage(String filePath)
	{
		String sendingFileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(filePath));
		File selectedFile = new File(filePath);
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append("\r\n");
		String name = selectedFile.getName();
		res.append("Content-Disposition: form-data; name=\"").append("file").append("\"; filename=\"").append(name).append("\"\r\n").append("Content-Type: ")
				.append(sendingFileType).append("\r\n\r\n");
		return res.toString();
	}

	/*
	 * Sets up the file byte array with boundary message File Content and boundary returns the completed setup file byte array
	 */
	private static byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize, byte[] fileContent)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + fileContent.length + boundary.length()];
		try
		{
			System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
			System.arraycopy(fileContent, 0, fileBytes, boundaryMesssage.length(), fileContent.length);
			System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + fileContent.length, boundary.length());
		}
		catch (NullPointerException | ArrayStoreException | IndexOutOfBoundsException e)
		{

			Logger.d("FileUpload", e.toString());
			return null;
		}
		return fileBytes;
	}

	public static List<Header> getHeaders()
	{

		HikeSharedPreferenceUtil mpref = HikeSharedPreferenceUtil.getInstance();
		String platformUID = mpref.getData(HikeMessengerApp.PLATFORM_UID_SETTING, null);
		String platformToken = mpref.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null);
		if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
		{
			List<Header> headers = new ArrayList<Header>(1);
			headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME, HikePlatformConstants.PLATFORM_TOKEN + "=" + platformToken + "; "
					+ HikePlatformConstants.PLATFORM_USER_ID + "=" + platformUID));

			return headers;
		}
		return new ArrayList<Header>();
	}

	/*
	 * This function is called to read the list of files from the System from a folder
	 * 
	 * @param filePath : The complete file path that is about to be read returns the JSON Array of the file paths of the all the files in a folder
	 * 
	 * @param doDeepLevelAccess : To specify if we want to read all the internal files and folders recursively
	 */
	public static JSONArray readFileList(String filePath, boolean doDeepLevelAccess)
	{
		File directory = new File(filePath);
		if (directory.exists() && !directory.isDirectory())
		{
			Logger.d("FileSystemAccess", "Cannot read a single file");
			return null;
		}
		else if (!directory.exists())
		{
			Logger.d("FileSystemAccess", "Invalid file path!");
			return null;
		}
		ArrayList<File> list = filesReader(directory, doDeepLevelAccess);
		JSONArray mArray = new JSONArray();
		for (int i = 0; i < list.size(); i++)
		{
			String path = HikePlatformConstants.FILE_DESCRIPTOR + list.get(i).getAbsolutePath();// adding the file descriptor
			mArray.put(path);
		}
		return mArray;
	}

	public static JSONArray trimFilePath(JSONArray mArray,String pathToBeTrimmedOut)
	{
		JSONArray trimmedArray = new JSONArray();
		for (int i = 0; i < mArray.length(); i++)
		{
			String path;
			try
			{
				path = mArray.get(i).toString();
				path = path.replaceAll(pathToBeTrimmedOut, "");
				path = path.replaceAll(HikePlatformConstants.FILE_DESCRIPTOR, "");
				trimmedArray.put(path);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return trimmedArray;
	}

	// Method that returns the reads the list of files
	public static ArrayList<File> filesReader(File root, boolean doDeepLevelAccess)
	{
		ArrayList<File> a = new ArrayList<>();

		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			if (doDeepLevelAccess)
			{
				if (files[i].isDirectory())
				{
					a.addAll(filesReader(files[i], doDeepLevelAccess));
				}
				else
				{
					a.add(files[i]);
				}

			}
			else
			{
				a.add(files[i]);
			}
		}
		return a;
	}

	/*
	 * This function is called to copy a directory from one location to another location
	 * 
	 * @param sourceLocation : The folder which is about to be copied
	 * 
	 * @param targetLocation : The folder where the directory is about to be copied
	 */
	public static boolean copyDirectoryTo(File sourceLocation, File targetLocation) throws IOException
	{
		if (sourceLocation.isDirectory())
		{
			if (!targetLocation.exists())
			{
				targetLocation.mkdir();
			}
			String[] children = sourceLocation.list();
			for (int i = 0; i < sourceLocation.listFiles().length; i++)
			{
				copyDirectoryTo(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
			}
		}
		else
		{

			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
		return true;
	}

	/*
	 * This function is called to delete a particular file from the System
	 * 
	 * @param filePath : The complete file path of the file that is about to be deleted returns whether the file is deleted or not Does not return a guaranteed call for a full
	 * delete
	 */
	public static boolean deleteDirectory(String filePath)
	{
		File deletedDir = new File(filePath);
		if (deletedDir.exists())
		{
			boolean isDeleted = deleteOp(deletedDir);
			Logger.d("FileSystemAccess", "Directory exists!");
			Logger.d("FileSystemAccess", (isDeleted) ? "File is deleted" : " File not deleted");
			return isDeleted;
		}
		else
		{
			Logger.d("FileSystemAccess", "Invalid file path!");
			return false;
		}
	}

	// This method performs the actual deletion of the file
	public static boolean deleteOp(File dir)
	{
		Logger.d("FileSystemAccess", "In delete");
		if (dir.exists())
		{// This checks if the file/folder exits or not
			if (dir.isDirectory())// This checks if the call is made to delete a particular file (eg. "index.html") or an entire sub-folder
			{
				String[] children = dir.list();
				if (children != null && children.length>0)
				{
					for (int i = 0; i < children.length; i++)
					{
						File temp = new File(dir, children[i]);
						if (temp.isDirectory())
						{
							Logger.d("DeleteRecursive", "Recursive Call" + temp.getPath());
							deleteOp(temp);
						}
						else
						{
							Logger.d("DeleteRecursive", "Delete File" + temp.getPath());
							boolean b = temp.delete();
							if (!b)
							{
								Logger.d("DeleteRecursive", "DELETE FAIL");
								return false;
							}
						}
					}
				}
                dir.delete();
			}
			else
			{
				dir.delete();
			}
			Logger.d("FileSystemAccess", "Delete done!");
			return true;
		}
		return false;
	}

	public static void multiFwdStickers(Context context, String stickerId, String categoryId, boolean selectAll)
	{
		if (context == null)
		{
			return;
		}

		Intent intent = IntentFactory.getForwardStickerIntent(context, stickerId, categoryId);
		intent.putExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, selectAll);
		context.startActivity(intent);
	}

	public static void downloadStkPk(String metaData)
	{
		try
		{
			JSONObject object = new JSONObject(metaData);
			String categoryId = object.optString(StickerManager.CATEGORY_ID);
			String categoryName = object.optString(StickerManager.CATEGORY_NAME);
			int totalStickers = object.optInt(StickerManager.TOTAL_STICKERS);
			int categorySize = object.optInt(StickerManager.CATEGORY_SIZE);

			if (!TextUtils.isEmpty(categoryId) && !TextUtils.isEmpty(categoryName))
			{
				StickerCategory category = new StickerCategory.Builder()
						.setCategoryId(categoryId)
						.setCategoryName(categoryName)
						.setTotalStickers(totalStickers)
						.setCategorySize(categorySize)
						.build();
				downloadStkPk(category);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void downloadStkPk(StickerCategory category)
	{
		StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(category.getCategoryId());
		stickerPalleteImageDownloadTask.execute();
		StickerManager.getInstance().initialiseDownloadStickerPackTask(category, StickerConstants.DownloadType.NEW_CATEGORY, StickerManager.getInstance().getPackDownloadBodyJson(StickerConstants.DownloadSource.POPUP));
	}

	public static void OnChatHeadPopupActivateClick()
	{
		Context context = HikeMessengerApp.getInstance();
		if (ChatHeadUtils.areWhitelistedPackagesSharable(context))
		{
			Toast.makeText(context, context.getString(R.string.sticker_share_popup_activate_toast), Toast.LENGTH_LONG).show();

				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.ENABLE, true);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.USER_CONTROL, true);
				JSONArray packagesJSONArray;
				try
				{
					packagesJSONArray = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null));
					if (packagesJSONArray != null)
					{
						ChatHeadUtils.setAllApps(packagesJSONArray, true, false);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				ChatHeadUtils.startOrStopService(true);

		}
		else
		{
			Toast.makeText(context, context.getString(R.string.sticker_share_popup_not_activate_toast), Toast.LENGTH_LONG).show();
		}
	}

	public static void sendPlatformCrashAnalytics(String crashType, String msisdn)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.APP_CRASH_EVENT);
			json.put(HikeConstants.MSISDN, msisdn);
			json.put(AnalyticsConstants.DATA, crashType);
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.APP_CRASH_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void sendPlatformCrashAnalytics(String crashType)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.APP_CRASH_EVENT);
			json.put(AnalyticsConstants.DATA, crashType);
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.APP_CRASH_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void sharedDataHandlingForMessages(ConvMessage conv)
	{
		try
		{
			if (conv.getPlatformData() != null)
			{
				JSONObject sharedData = conv.getPlatformData();
				String namespaces = sharedData.getString(HikePlatformConstants.RECIPIENT_NAMESPACES);
				if (TextUtils.isEmpty(namespaces))
				{
					Logger.e(HikePlatformConstants.TAG, "no namespaces defined.");
					return;
				}
				String[] namespaceList = namespaces.split(",");
				for (String namespace : namespaceList)
				{
					String eventType = sharedData.optString(HikePlatformConstants.EVENT_TYPE, HikePlatformConstants.SHARED_EVENT);
					long mappedEventId = -1;
					if (!conv.isSent())
					{
						mappedEventId = sharedData.getLong(HikePlatformConstants.MAPPED_EVENT_ID);
					}
					String metadata = sharedData.getString(HikePlatformConstants.EVENT_CARDDATA);
					int state = conv.isSent() ? HikePlatformConstants.EventStatus.EVENT_SENT : HikePlatformConstants.EventStatus.EVENT_RECEIVED;
					MessageEvent messageEvent = new MessageEvent(eventType, conv.getMsisdn(), namespace, metadata, conv.createMessageHash(), state, conv.getSendTimestamp(),
							mappedEventId);
					long eventId = HikeConversationsDatabase.getInstance().insertMessageEvent(messageEvent);
					if (eventId < 0)
					{
						Logger.e(HikePlatformConstants.TAG, "Duplicate Message Event");
					}
					else
					{
						sharedData.put(HikePlatformConstants.MAPPED_EVENT_ID, eventId);
						conv.setPlatformData(sharedData);
					}
				}

			}
		}
		catch (JSONException e)
		{
			// TODO catch block
			e.printStackTrace();
		}

	}

	/**
	 * Call this method to send platform message event. This method sends an event to the msisdn that it determines when it queries the messages table based on the message hash.
	 * 
	 * @param eventMetadata
	 * @param messageHash
	 * @param nameSpace
	 */
	public static void sendPlatformMessageEvent(String eventMetadata, String messageHash, String nameSpace)
	{
		String msisdn = HikeConversationsDatabase.getInstance().getMsisdnFromMessageHash(messageHash);
		if (TextUtils.isEmpty(msisdn))
		{
			Logger.e(HikePlatformConstants.TAG, "Message Hash is incorrect");
			return;
		}

		try
		{
			JSONObject data = new JSONObject(eventMetadata);
			String cardData = data.getString(HikePlatformConstants.EVENT_CARDDATA);
			MessageEvent messageEvent = new MessageEvent(HikePlatformConstants.NORMAL_EVENT, msisdn, nameSpace, cardData, messageHash,
					HikePlatformConstants.EventStatus.EVENT_SENT, System.currentTimeMillis());

			HikeMessengerApp.getPubSub().publish(HikePubSub.PLATFORM_CARD_EVENT_SENT, new Pair<MessageEvent, JSONObject>(messageEvent, data));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Used to record analytics for bot opens via push notifications Sample JSON : {"ek":"bno","bot_msisdn":"+hikecricketnew+"}
	 */
	public static void recordBotOpenSource(String msisdn, String source)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.BOT_NOTIF_TRACKER);
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
			json.put(AnalyticsConstants.BOT_OPEN_SOURCE, source);
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}

		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static JSONObject getPlatformContactInfo(String msisdn)
	{
		JSONObject jsonObject;
		ContactInfo info = ContactManager.getInstance().getContact(msisdn, true, false);
		try
		{
			if (info == null)
			{
				jsonObject = new JSONObject();
				jsonObject.put("name", msisdn);
			}
			else
			{
				jsonObject = info.getPlatformInfo();
			}
			return jsonObject;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return new JSONObject();
		}
	}

	/**
	 * Called from MQTTManager, this method is used to resync PlatformUserId and PlatformTokens for clients which have become out of sync with server
	 * 
	 * sample packet :
	 * 
	 * { "plfsync" : { "platformUid" : "ABCDEFxxxxxx" , "platformToken" : "PQRSTUVxxxxxx" }
	 * 
	 * @param plfSyncJson
	 */
	public static void savePlatformCredentials(JSONObject plfSyncJson)
	{
		String newPlatformUserId = plfSyncJson.optString(HikePlatformConstants.PLATFORM_USER_ID, "");

		String newPlatformToken = plfSyncJson.optString(HikePlatformConstants.PLATFORM_TOKEN, "");

		Logger.i(TAG, "New Platform UserID : " + newPlatformUserId + " , new platform token : " + newPlatformToken);

		if (!TextUtils.isEmpty(newPlatformUserId))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PLATFORM_UID_SETTING, newPlatformUserId);
		}

		if (!TextUtils.isEmpty(newPlatformToken))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, newPlatformToken);
		}
	}

	/**
	 * Call this method to get the latitude and longitude and whether Gps is on/off
	 * 
	 * @param LocationManager
	 * @param Location
	 * 
	 */
	public static String getLatLongFromLocation(LocationManager locationManager, Location location)
	{
		JSONObject json = new JSONObject();
		double longitude = 0;
		double latitude = 0;

		if (location != null)
		{
			longitude = location.getLongitude();
			latitude = location.getLatitude();
		}
		// getting GPS status
		try
		{
			JSONObject s_values = new JSONObject();
			s_values.put("latitude", latitude);
			s_values.put("longitude", longitude);
			json.put("coords", s_values);
			json.put("gpsAvailable", locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return json.toString();
	}

	/**
	 * Returns a String array, which contains the following values :<br>
	 * [ <total-downloaded-bytes> , <progress> , <original downloaded file path>, <url from which it was downloaded> ]
	 * 
	 * @param filePath
	 * @return
	 */
	public static String[] readPartialDownloadState(String filePath)
	{
		String[] data = new String[4];
		int i = 0;
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(filePath));
			String line;

			while ((line = reader.readLine()) != null)
			{
				data[i] = line.split(FileRequestPersistent.FILE_DELIMETER)[1];
				i++;
			}
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		finally
		{
			if (reader != null)
			{
				Utils.closeStreams(reader);
			}
		}

		return data;
	}

	public static String getLastGame()
	{
		if (BotUtils.isBot(HikePlatformConstants.GAME_CHANNEL))
		{
			return HikeContentDatabase.getInstance().getFromContentCache(HikePlatformConstants.LAST_GAME, BotUtils.getBotInfoForBotMsisdn(HikePlatformConstants.GAME_CHANNEL).getNamespace());
		}

		else //Highly improbable, can only happen when Games Channel is not yet installed.
		{
			return "";
		}
	}

	public static void killProcess(Activity context,String process)
	{
		if (context != null)
		{
			ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
			List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
			for (int i = 0; i < procInfos.size(); i++)
			{
				if (procInfos.get(i).processName.equals(process))
				{
					int pid = procInfos.get(i).pid;
					android.os.Process.killProcess(pid);

				}
			}
		}
	}

	public static Header getDownloadRangeHeader(long startOffset)
	{
		return new Header("Range", "bytes=" + startOffset + "-");
	}

	/*
	 * Code to append unzip path based on request type for micro app unzip process
	 */
	public static String generateMappUnZipPathForBotType(byte botType, String unzipPath, String microAppName)
	{
		// Generate unzip path for the given request type
		switch (botType)
		{
		case HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS:
			unzipPath += PlatformContentConstants.HIKE_WEB_MICRO_APPS + microAppName + File.separator;
			break;
		case HikePlatformConstants.PlatformBotType.ONE_TIME_POPUPS:
			unzipPath += PlatformContentConstants.HIKE_ONE_TIME_POPUPS + microAppName + File.separator;
			break;
		case HikePlatformConstants.PlatformBotType.NATIVE_APPS:
			unzipPath += PlatformContentConstants.HIKE_GAMES + microAppName + File.separator;
			break;
		case HikePlatformConstants.PlatformBotType.HIKE_MAPPS:
			unzipPath += PlatformContentConstants.HIKE_MAPPS + microAppName + File.separator;
			break;
		}
		return unzipPath;
	}

	/**
	 * Returns the root folder path for Hike MicroApps <br>
	 * eg : "/data/data/com.bsb.hike/files/Content/HikeMicroApps/"
	 *
	 * @return
	 */
	public static String getMicroAppContentRootFolder()
	{
		File file = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS);
		if (!file.exists())
		{
			file.mkdirs();
		}

		return PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS;
	}

	/**
	 * Utility method to send a delivery report for Event related messages via the general event framework. The packet structure is as follows : <br>
	 * {“t” : “ge1”, “d”: { “t” : “dr”, “d”:”155”}, “to” : "9717xxxxx" }
	 * 
	 * @param mappedEventId
	 * @param receiverMsisdn
	 */
	public static void sendGeneralEventDeliveryReport(long mappedEventId, String receiverMsisdn)
	{

		JSONObject jObj = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{

			jObj.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ONE);
			jObj.put(HikeConstants.TO, receiverMsisdn);

			data.put(HikeConstants.TYPE, HikeConstants.GeneralEventMessagesTypes.GENERAL_EVENT_DR);
			data.put(HikePlatformConstants.EVENT_DATA, mappedEventId);

			jObj.put(HikeConstants.DATA, data);
			HikeMqttManagerNew.getInstance().sendMessage(jObj, MqttConstants.MQTT_QOS_ONE);
		}

		catch (JSONException e)
		{
			Logger.e(TAG, "JSON Exception while sending DR packet for normal event" + e.toString());
		}
	}

	/**
	 * Utility method to send microapp download success or failure analytics
	 * 
	 * @param success
	 * @param appName
	 * @param mAppVersionCode
	 */
	public static void sendMicroAppServerAnalytics(boolean success, String appName, int mAppVersionCode)
	{
		sendMicroAppServerAnalytics(success,appName,mAppVersionCode,-1);
	}
	public static void sendMicroAppServerAnalytics(boolean success, String appName, int mAppVersionCode,int errorCode)
	{
        // Json to be sent to server for analysing micro-apps acks analytics
        JSONObject json = new JSONObject();

        try
		{
			JSONObject appsJsonObject = new JSONObject();
            appsJsonObject.put(HikePlatformConstants.APP_NAME, appName);
            appsJsonObject.put(HikePlatformConstants.APP_VERSION, mAppVersionCode);
            appsJsonObject.put(HikePlatformConstants.ERROR_CODE,errorCode);

            // Put apps JsonObject in the final json
            json.put(HikePlatformConstants.APPS, appsJsonObject);

			RequestToken token = HttpRequests.microAppPostRequest(HttpRequestConstants.getMicroAppLoggingUrl(success), json, new IRequestListener()
			{
				@Override
                public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
				{

				}

				@Override
				public void onRequestSuccess(Response result)
				{

				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{

				}
			});
			token.execute();
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception occured while sending microapp analytics : " + e.toString());
		}

	}

	public static void requestRecurringLocationUpdates(JSONObject json)
	{
		long duration = json.optInt(HikePlatformConstants.DURATION, 0);
		final long interval = json.optInt(HikePlatformConstants.TIME_INTERVAL, 0);

		// Checking if a request is already running
		if (HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, -1L) >= 0L && interval >= 0)
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, -1L) >= System.currentTimeMillis() + duration)
				return;
			HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, System.currentTimeMillis() + duration);
			HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.TIME_INTERVAL, interval);
		}

		Logger.i(TAG, "Starting recurring location updates at time : " + System.currentTimeMillis() + ". Duration : " + duration + " Interval : " + interval);

		final GpsLocation gps = GpsLocation.getInstance();

		gps.requestRecurringLocation(new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				Logger.i(TAG, "Location available : " + location.getLatitude() + " , " + location.getLongitude() + " Source : " + location.getProvider());
				locationAnalytics(location);
				if (HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, -1L) < location.getTime() + interval) {
					Logger.i(TAG, "Stopping recurring location updates at time : " + System.currentTimeMillis());
					gps.removeUpdates(this);
					HikeSharedPreferenceUtil.getInstance().removeData(HikePlatformConstants.RECURRING_LOCATION_END_TIME);
				}
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {

			}

			@Override
			public void onProviderEnabled(String provider) {

			}

			@Override
			public void onProviderDisabled(String provider) {

			}
		}, interval, duration);
	}

	/**
	 * Method to send log location updates to analytics.
	 * @param location
     */
	public static void locationAnalytics(Location location)
	{
		JSONObject json = new JSONObject();
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		String provider = location.getProvider();

		try
		{
			json.put(HikeConstants.LATITUDE, latitude);
			json.put(HikeConstants.LONGITUDE, longitude);
			json.put(HikeConstants.LOCATION_PROIVDER, provider);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.USER_LOCATION, json);
	}

	public static void resumeLoggingLocationIfRequired()
	{
		long endTime = HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, -1L);
		HikeSharedPreferenceUtil.getInstance().removeData(HikePlatformConstants.RECURRING_LOCATION_END_TIME);
		if (endTime >= 0 && System.currentTimeMillis() < endTime)
		{
			Logger.i("PlatformUtils", "Resuming location updates");
			JSONObject json = new JSONObject();
			try
			{
				json.put(HikePlatformConstants.DURATION, endTime - System.currentTimeMillis());
				json.put(HikePlatformConstants.TIME_INTERVAL, HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.TIME_INTERVAL, 0L));

				PlatformUtils.requestRecurringLocationUpdates(json);
			} catch (JSONException e)
			{
				Logger.e("PlatformUtils", "JSONException in resumeLoggingLocationIfRequired : "+e.getMessage());
				e.printStackTrace();
			}

		}
	}

	public static void addLocaleToInitJSON(JSONObject jsonObject) throws JSONException
	{
		jsonObject.put(HikeConstants.LOCALE, LocalLanguageUtils.getApplicationLocalLanguageLocale());
		jsonObject.put(HikeConstants.DEVICE_LOCALE, LocalLanguageUtils.getDeviceDefaultLocale());
	}

	public static String getNotifBody(JSONObject jsonObj)
	{
		if (jsonObj.has(HikeConstants.LANG_ARRAY))
		{
			try
			{
				JSONObject langJSON = Utils.getDataBasedOnAppLanguage(jsonObj.getJSONArray(HikeConstants.LANG_ARRAY).toString());
				if (langJSON != null)
				{
					return langJSON.optString(HikeConstants.BODY);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return jsonObj.optString(HikeConstants.BODY);
	}

    /*
     * Method for inserting MAPP successful entry into content database
     */
	public static void mAppCreationSuccessHandling(JSONObject mAppJson)
	{
		if (mAppJson == null)
			return;

		JSONObject cardObjectJson = mAppJson.optJSONObject(HikePlatformConstants.CARD_OBJECT);

		if (cardObjectJson != null)
		{
			final int version = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE, 0);
			final String appName = cardObjectJson.optString(HikePlatformConstants.APP_NAME, "");
			final String appPackage = cardObjectJson.optString(HikePlatformConstants.APP_PACKAGE, "");

            // Publish pubsub for successful creation of mapp packet received
            Pair<BotInfo,Boolean> mAppCreatedSuccessfullyPair = new Pair(appName,true);
            HikeMessengerApp.getPubSub().publish(HikePubSub.MAPP_CREATED, mAppCreatedSuccessfullyPair);

			HikeHandlerUtil mThread;
			mThread = HikeHandlerUtil.getInstance();
			mThread.startHandlerThread();

			mThread.postRunnable(new Runnable()
			{
				@Override
				public void run()
				{
					HikeContentDatabase.getInstance().insertIntoMAppDataTable(appName, version, appPackage);
				}
			});
		}
	}

    /*
     * This method would check if assoc mapps required for this cbot are downloaded, then trigger the download of micro app
     */
    private static void assocMappDownloadHandling(final BotInfo botInfo, final boolean enableBot, final String botChatTheme, final String notifType,
                                                  final NonMessagingBotMetadata botMetadata, boolean resumeSupport, boolean autoResume) {

        if(assocMappRequestStatusMap.containsKey(botInfo.getMsisdn()))
        {
            assocMappRequestStatusMap.put(botInfo.getMsisdn(),assocMappRequestStatusMap.get(botInfo.getMsisdn()) - 1);

            if(assocMappRequestStatusMap.get(botInfo.getMsisdn()) == 0)
            {
                assocMappRequestStatusMap.remove(botInfo.getMsisdn());
                downloadMicroAppZipForNonMessagingCbotPacket(botInfo,enableBot,botChatTheme,notifType,botMetadata,resumeSupport);
            }
        }
    }

    /*
     * Method to determine if micro app already exists or not and compares its version code
     */
    public static boolean isMicroAppExist(String microAppName, int microAppVersionCode, String msisdn, byte botType)
    {
        // Generate path and check the case for the old micro app directory
        File oldMicroAppFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR, microAppName);
        if (oldMicroAppFolder.exists())
            return true;

        // check if msisdn is empty, stop the flow as we can not determine current micro app versioning details without msisdn
        if(TextUtils.isEmpty(msisdn))
            return false;

        try
        {
            String unzipPath = getMicroAppContentRootFolder();
            unzipPath = PlatformUtils.generateMappUnZipPathForBotType(botType,unzipPath,microAppName);

            // Details for current micro app running on device
            BotInfo currentBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
            int currentBotInfoMAppVersionCode = 0;
            if(currentBotInfo != null)
                currentBotInfoMAppVersionCode = currentBotInfo.getMAppVersionCode();

            if (new File(unzipPath).exists() && microAppVersionCode <= currentBotInfoMAppVersionCode)
                return true;

        }
        catch (NullPointerException npe)
        {
            Logger.e("PlatformZipDownloader isMicroAppExist",npe.toString());
            npe.printStackTrace();
        }

        return false;
    }

    /*
     * Method to determine if micro app already exists or not and compares its version code
     */
	public static boolean isMicroAppExistForPopUps(String microAppName, byte botType)
	{
		// Generate path and check the case for the old micro app directory code
		File oldMicroAppFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR, microAppName);

		// Generate path for versioning path and check the case for the new micro app directory code
		String newMicroAppFolderUnzipPath = getMicroAppContentRootFolder();
		newMicroAppFolderUnzipPath = PlatformUtils.generateMappUnZipPathForBotType(botType, newMicroAppFolderUnzipPath, microAppName);

        // First check in the older code path directory, then in the newer path else returns false
		if (oldMicroAppFolder.exists())
			return true;
		else if (new File(newMicroAppFolderUnzipPath).exists())
			return true;
		else
			return false;
	}

    /*
    * Method to determine if mapp sdk exists and compares its version code
    */
    public static boolean isMicroAppExistForMappPacket(String mAppName,int mAppVersionCode)
    {
        // Generate path for the old mapp directory
        File unzipPath = new File(getMicroAppContentRootFolder() + PlatformContentConstants.HIKE_MAPPS, mAppName);

        int currentMappVersionCode = 0;
        if(HikeMessengerApp.hikeMappInfo.containsKey(mAppName))
            currentMappVersionCode = HikeMessengerApp.hikeMappInfo.get(mAppName);
        else
            return false;

        return (unzipPath.exists() && mAppVersionCode <= currentMappVersionCode) ? true : false;
    }


	public static void share(String text, String caption, Activity context, CustomWebView mWebView) {
		FileOutputStream fos = null;
		File cardShareImageFile = null;
		if (context != null) {
			try {
				if (TextUtils.isEmpty(text)) {
					//text = mContext.getString(R.string.cardShareHeading); // fallback
				}

				cardShareImageFile = new File(context.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
				fos = new FileOutputStream(cardShareImageFile);
				View share = LayoutInflater.from(context).inflate(com.bsb.hike.R.layout.web_card_share, null);
				// set card image
				ImageView image = (ImageView) share.findViewById(com.bsb.hike.R.id.image);
				Bitmap b = Utils.viewToBitmap(mWebView);
				image.setImageBitmap(b);

				// set heading here
				TextView heading = (TextView) share.findViewById(R.id.heading);
				heading.setText(text);

				// set description text
				TextView tv = (TextView) share.findViewById(com.bsb.hike.R.id.description);
				tv.setText(Html.fromHtml(context.getString(com.bsb.hike.R.string.cardShareDescription)));

				Bitmap shB = Utils.undrawnViewToBitmap(share);
				Logger.i(TAG, " width height of layout to share " + share.getWidth() + " , " + share.getHeight());
				shB.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.flush();
				Logger.i(TAG, "share webview card " + cardShareImageFile.getAbsolutePath());
				IntentFactory.startShareImageIntent("image/jpeg", "file://" + cardShareImageFile.getAbsolutePath(),
						TextUtils.isEmpty(caption) ? context.getString(com.bsb.hike.R.string.cardShareCaption) : caption);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(context, context.getString(com.bsb.hike.R.string.error_card_sharing), Toast.LENGTH_SHORT).show();
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						// Do nothing
						e.printStackTrace();
					}
				}
			}
			if (cardShareImageFile != null && cardShareImageFile.exists()) {
				cardShareImageFile.deleteOnExit();
			}
		}
	}


	public static String getRunningGame(Context context)
	{
		String gameId = "";
		String lastGame = getLastGame();

		if (context == null || TextUtils.isEmpty(lastGame))
		{
			Logger.e(TAG, "Either activity is null or lastgame is null in getRunningGame");
			return gameId;
		}

		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
		for (int i = 0; i < procInfos.size(); i++)
		{
			if (procInfos.get(i).processName.equals(HikePlatformConstants.GAME_PROCESS))
			{
				gameId = lastGame;
				break;
			}
		}
		Logger.d(TAG, "getRunningGame: " + gameId);
		return gameId;
	}


    public static void sendStickertoAllHikeContacts(String stickerId, String categoryId) {

        List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
        List<ContactInfo> recentList = ContactManager.getInstance().getAllConversationContactsSorted(true, true);
        //reversing it so maintain order

		if (allContacts == null || allContacts.isEmpty()) {
			return;
		}
        Collections.reverse(recentList);

        //removing duplicate contacts
        allContacts.removeAll(recentList);

        //creating new order-->recent contacts-->all contacts
        recentList.addAll(allContacts);
        allContacts = recentList;


        List<ContactInfo> finalContacts=new ArrayList<>(allContacts.size());
        for (ContactInfo ci : allContacts) {
            if(!ci.isBot()&&ci.isOnhike()&&!ci.isBlocked())  // add more check here ..ex:stealth,unknown etc...
            {
                finalContacts.add(ci);
            }
        }

        Sticker sticker = new Sticker(categoryId, stickerId);

        if(!sticker.isDisabled())
        {
            StickerManager.getInstance().addRecentStickerToPallete(sticker);
        }

        ConvMessage cm = getConvMessageForSticker(sticker, categoryId, allContacts.get(0), StickerManager.FROM_FORWARD);

        if (cm != null) {
            List<ConvMessage> multiMsg = new ArrayList<>();
            multiMsg.add(cm);
            sendMultiMessages(multiMsg, finalContacts);
        } else {
            Logger.wtf("productpopup", "ConvMessage is Null");
        }
    }

	private static void sendMultiMessages(List<ConvMessage> multipleMessageList, List<ContactInfo> arrayList)
	{
		MultipleConvMessage multiMessages = new MultipleConvMessage(multipleMessageList, arrayList, System.currentTimeMillis() / 1000, false, null);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_SENT, multiMessages);
	}

	public static ConvMessage getConvMessageForSticker(Sticker sticker, String categoryIdIfUnknown, ContactInfo contactInfo, String source)
	{
		if (contactInfo == null)
		{
			return null;
		}
		ConvMessage convMessage = Utils.makeConvMessage(contactInfo.getMsisdn(), "Sticker",contactInfo.isOnhike());

		JSONObject metadata = new JSONObject();
		try
		{
			String categoryId = sticker.getCategoryId();
			metadata.put(StickerManager.CATEGORY_ID, categoryId);

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());

			if(!source.equalsIgnoreCase(StickerManager.FROM_OTHER))
			{
				metadata.put(StickerManager.SEND_SOURCE, source);
			}
			convMessage.setMetadata(metadata);
			Logger.d("productpopup", "metadata: " + metadata.toString());
		}
		catch (JSONException e)
		{
			Logger.e("productpopup", "Invalid JSON", e);
		}
		return convMessage;
	}


    /*
     * Method to determine and send analytics for disk space occupied by the platform. This method is called on app update and also it can be invoked by sending nmapp packet
     * Json generated here :: {"fld1":"disk_consumption","fld3":"app_updated","fld2":"DP","ek":"micro_app","fld5":111107,"event":"nmapp"}
     */
	public static void platformDiskConsumptionAnalytics(String analyticsTriggerPoint)
	{
        // Get list of all micro apps installed in content directory
		JSONArray mArray = PlatformUtils.readFileList(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS, false);
        long contentFolderLength = 0,directorySize;

        // Precautionary check to check if these files are indeed folders and preventing NPE
        File platformContentDirectory = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR);
        if(platformContentDirectory.exists())
            contentFolderLength = Utils.folderSize(platformContentDirectory);

        for (int i = 0; i < mArray.length(); i++)
		{
			try
			{
				String path = (String) mArray.get(i);
				path = path.replaceAll(PlatformContentConstants.PLATFORM_CONTENT_DIR, "");
				path = path.replaceAll(HikePlatformConstants.FILE_DESCRIPTOR, "");
				File microAppFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + path);

				if (microAppFile.isDirectory() && Utils.folderSize(microAppFile) > 0)
				{
                    directorySize = Utils.folderSize(microAppFile);
                    JSONObject json = new JSONObject();
					json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
					json.putOpt(AnalyticsConstants.EVENT, AnalyticsConstants.NOTIFY_MICRO_APP_STATUS);
					json.putOpt(AnalyticsConstants.LOG_FIELD_1, AnalyticsConstants.DISK_CONSUMPTION_ANALYTICS);
					json.putOpt(AnalyticsConstants.LOG_FIELD_2, path); // App Name
					json.putOpt(AnalyticsConstants.LOG_FIELD_3, analyticsTriggerPoint); // Analytics Trigger Point
					json.putOpt(AnalyticsConstants.LOG_FIELD_5, directorySize); // Directory disk consumption
                    json.putOpt(AnalyticsConstants.LOG_FIELD_6, contentFolderLength); // Total content directory size
					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_INFO, json);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

    /*
     * Method to determine and send analytics for disk space occupied by the micro app just being installed. This method is called on successful cbot,mapp and popup creation
     * Json generated here :: {"fld6":3237192,"fld1":"hikecoupons","ek":"micro_app","fld5":448390,"event":"microapp_disk_consumption"}
     */
    public static void microAppDiskConsumptionAnalytics(String appName)
    {
        try
        {
            JSONObject json = new JSONObject();
            long contentFolderLength = 0,botFileSize =0;

            // Precautionary check to check if these files are indeed folders and preventing NPE
            if(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR).isDirectory())
                contentFolderLength = Utils.folderSize(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR));
            if(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + appName).isDirectory())
                botFileSize = Utils.folderSize(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + appName));

            json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
            json.putOpt(AnalyticsConstants.EVENT, AnalyticsConstants.MICROAPP_DISK_CONSUMPTION);

            json.putOpt(AnalyticsConstants.LOG_FIELD_1, appName); //App Name
            json.putOpt(AnalyticsConstants.LOG_FIELD_5, botFileSize); // installed microapp disk consumption
            json.putOpt(AnalyticsConstants.LOG_FIELD_6, contentFolderLength); // Total content directory size

            HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }


    public static void invalidDataBotAnalytics(BotInfo botInfo) {
        // Added analytics event to consider this micro app download as failure because of invalid data
        PlatformContent.EventCode event = PlatformContent.EventCode.INVALID_DATA;
        Logger.wtf(TAG, "microapp download packet failed." + event.toString());
        JSONObject json = new JSONObject();
        try
        {
            json.put(HikePlatformConstants.ERROR_CODE, event.toString());
            PlatformUtils.createBotAnalytics(HikePlatformConstants.BOT_CREATION_FAILED, botInfo, json);
            PlatformUtils.createBotMqttAnalytics(HikePlatformConstants.BOT_CREATION_FAILED_MQTT, botInfo, json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    //{"t":"le_android","d":{"et":"uiEvent","st":"click","ep":"HIGH","cts":1457198967791,"tag":"plf","md":{"ek":"micro_app","event":"botContentShared","fld4":"aGlrZS1jb250bnQtc3RvcmU=ZmM0M2QyNzUtMzQ0Zi00ZDMwLTk3N2UtMGM5YzJjMzEzYjFjLlZsZ1hONFJYcnp0M1hZc3I","fld1":"IMAGE","bot_msisdn":"+hikeviral+","sid":1457198959796}}}
	public static void sendBotFileShareAnalytics(HikeFile hikeFile, String msisdn)
	{
		String fileKey = hikeFile.getFileKey();
		JSONObject json = new JSONObject();
		try {
			json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT, AnalyticsConstants.BOT_CONTENT_SHARED);
			json.putOpt(AnalyticsConstants.LOG_FIELD_4, fileKey);
			json.putOpt(AnalyticsConstants.LOG_FIELD_1, hikeFile.getHikeFileType());
			json.putOpt(AnalyticsConstants.BOT_MSISDN, msisdn);
		} catch (JSONException e) {
			Logger.d(TAG, "Exception in bot share utils");
		}
		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
	}
/*
 *Method to add data to the State table
 */
	public static void addToPlatformDownloadStateTable(final String name, final int mAppVersionCode, final String data,@HikePlatformConstants.PlatformTypes final int type, final long ttl,final int prefNetwork,@HikePlatformConstants.PlatformDwnldState final int state, final boolean autoResume)
	{
		if (mAppVersionCode <-1 || TextUtils.isEmpty(name) || ttl < 0)
		{
			return;
		}
		HikeHandlerUtil handler = HikeHandlerUtil.getInstance();
		handler.startHandlerThread();
		handler.postRunnable(new Runnable() {
			@Override
			public void run() {
				HikeContentDatabase.getInstance().addToPlatformDownloadStateTable(name, mAppVersionCode, data, type, System.currentTimeMillis() + ttl, prefNetwork, state,(autoResume) ? 1 : 0);
			}
		});
	}

	/*
	Method to remove from PlatformDownload table
	*/
	public static void removeFromPlatformDownloadStateTable(final String name, final int mAppVersionCode)
	{
		if (TextUtils.isEmpty(name) || mAppVersionCode <-1)
		{
			return;
		}
		HikeHandlerUtil handler = HikeHandlerUtil.getInstance();
		handler.startHandlerThread();
		handler.postRunnable(new Runnable() {
			@Override
			public void run() {
				HikeContentDatabase.getInstance().removeFromPlatformDownloadStateTable(name, mAppVersionCode);
				//Deleting state and incomplete downloaded zip file.
				if (!TextUtils.isEmpty(name + FileRequestPersistent.STATE_FILE_EXT))
				{
					Utils.deleteFile(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + name + FileRequestPersistent.STATE_FILE_EXT));
					Utils.deleteFile(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME + File.separator + name +".zip"));
				}
			}
		});
	}

	// Method to update Platform Download table

	public static void updatePlatformDownloadState(final String name, final int mAppVersionCode, @HikePlatformConstants.PlatformDwnldState final int newState)
	{
		if (mAppVersionCode <-1 || TextUtils.isEmpty(name))
		{
			return;
		}
		HikeHandlerUtil handler = HikeHandlerUtil.getInstance();
		handler.startHandlerThread();
		handler.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				HikeContentDatabase.getInstance().updatePlatformDownloadState(name, mAppVersionCode, newState);
			}
		});
	}

	/*
	 * Method to retry pending downloads and also pause any current downloads if network is downgraded. Also remove from table if ttl has expired.
	 */
	public static void retryPendingDownloadsIfAny(final int currentNetwork)
	{
		Logger.i(TAG, "Restarting pending bot downloads...");
		HikeHandlerUtil handler = HikeHandlerUtil.getInstance();
		handler.startHandlerThread();
		handler.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				long currentTime = System.currentTimeMillis();
				Cursor c = HikeContentDatabase.getInstance().getAllPendingPlatformDownloads();
				if (c == null)
				{
					Logger.e(TAG, "There are no platform downloads to retry. Returning");
					return;
				}
				while (c.moveToNext())
				{
					try
					{
						JSONObject json = new JSONObject(c.getString(c.getColumnIndex(HikePlatformConstants.PACKET_DATA)));

						int type = c.getInt(c.getColumnIndex(HikePlatformConstants.TYPE));

						long ttl = c.getLong(c.getColumnIndex(HikePlatformConstants.TTL));

						int prefNetwork =c.getInt(c.getColumnIndex(HikePlatformConstants.PREF_NETWORK));

						String name = c.getString(c.getColumnIndex(HikePlatformConstants.APP_NAME));

						if(c.getInt(c.getColumnIndex(HikePlatformConstants.AUTO_RESUME)) != 1)
						{
							continue;    // Moving ahead only if auto_resume is true.
						}

						if (currentTime > ttl)
						{
							int mAppVersionCode = c.getInt(c.getColumnIndex(HikePlatformConstants.MAPP_VERSION_CODE));
							removeFromPlatformDownloadStateTable(name, mAppVersionCode);
							sendFailDueToTTL(name);
							continue;
						}
						if(prefNetwork < currentNetwork) // Pausing a request if  the network is downgraded.
						{
							PairModified<RequestToken, Integer> tokenCountPair = PlatformZipDownloader.getCurrentDownloadingRequests().get(name);
							if (null != tokenCountPair && null != tokenCountPair.getFirst())
							{
								tokenCountPair.getFirst().cancel();
							}
						}
						else // Only retry on higher NetworkTypes
						{
							sendDownloadResumedAnalytics(name);
							switch (type)
							{
								case HikePlatformConstants.PlatformTypes.CBOT:
									BotUtils.createBot(json,currentNetwork);
									break;
								case HikePlatformConstants.PlatformTypes.MAPP:
									downloadZipFromPacket(json,currentNetwork);
									break;
							}
						}
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "Exception in retryPendingDownloadsIfAny : "+e.getMessage());
						e.printStackTrace();
					}
				}
			}
		});
	}

	private static String getMsisdnFromAppName(String name)
	{
		if (TextUtils.isEmpty(name))
			return "";
		return "+" + name + "+";
	}

	// analytics json : {"d":{"ep":"HIGH","st":"filetransfer","et":"nonUiEvent","md":{"sid":1458220124380,"fld6”:567888,"fld1”:"hikenews","ek":"micro_app","event":"download_paused"},"cts":1458220184473,"tag":"plf"},"t":"le_android”}
	private static void sendDownloadPausedAnalytics(String id) {
		int downloadedLength=0;
		JSONObject json = new JSONObject();
		try
		{
			if(!TextUtils.isEmpty(id)) {
				String filePath = PlatformContentConstants.PLATFORM_CONTENT_DIR + id + FileRequestPersistent.STATE_FILE_EXT;
				String data[] = PlatformUtils.readPartialDownloadState(filePath);
				if (data == null || data.length < 1 || TextUtils.isEmpty(data[1]))
				{
					return;
				}
				downloadedLength = Integer.parseInt(data[0]);
			}
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.DOWNLOAD_PAUSED);
			json.putOpt(AnalyticsConstants.LOG_FIELD_6, downloadedLength);
			json.putOpt(AnalyticsConstants.LOG_FIELD_1, id);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, json);

	}
//analytics json : {"d":{"ep":"HIGH","st":"filetransfer","et":"nonUiEvent","md":{"sid":1458220124380,"ek":"micro_app","fld1”:"hikenews","event":"download_resumed"},"cts":1458220217498,"tag":"plf"},"t":"le_android"}
	private static void sendDownloadResumedAnalytics(String id) {
		int downladedLength=0;
		JSONObject json = new JSONObject();
		try
		{
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.DOWNLOAD_RESUMED);
			json.putOpt(AnalyticsConstants.LOG_FIELD_1, id);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, json);

	}


	public static void insertUrl(JSONArray array) {
		String key;
		String url;
		int life;
		JSONObject jsonObject;
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		for (int i = 0; i < array.length(); i++) {
			try {
				jsonObject = array.getJSONObject(i);
				key = jsonObject.getString(DBConstants.URL_KEY);
				url = jsonObject.getString(HikeConstants.URL);
				life = jsonObject.getInt(DBConstants.LIFE);
				db.insertURL(key, url, life);
			} catch (JSONException e) {
				Logger.e(TAG, e.toString());
			}
		}


	}

	public static void deleteUrl(JSONArray array) {
		String key;
		JSONObject jsonObject;
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		for (int i = 0; i < array.length(); i++) {
			try {
				jsonObject = array.getJSONObject(i);
				key = jsonObject.getString(DBConstants.URL_KEY);
				db.deleteURL(key);
			} catch (JSONException e) {
				Logger.e(TAG, e.toString());
			}
		}


	}

	public static List<Header> getHeaderForOauth(String oAuth) {
		List<Header> headers = new ArrayList<Header>(1);
		headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME,
				"OAUTH" + "=" + oAuth));

		return headers;
	}

	public static String getFileUploadJson(Intent data)
	{
		String filepath = data.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);

		if (TextUtils.isEmpty(filepath))
		{
			// Could be from crop activity
			filepath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);
		}

		if (TextUtils.isEmpty(filepath))
		{
			Logger.e("FileUpload", "Invalid file Path");
			return "";
		}
		else
		{
			filepath = filepath.toLowerCase();
			Logger.d("FileUpload", "Path of selected file :" + filepath);
			String fileExtension = MimeTypeMap.getFileExtensionFromUrl(filepath).toLowerCase();
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase()); // fixed size type extension
			Logger.d("FileUpload", "mime type  of selected file :" + mimeType);
			JSONObject json = new JSONObject();
			try
			{
				json.put("filePath", filepath);
				json.put("mimeType", mimeType);
				json.put("filesize", (new File(filepath)).length());
				return json.toString();
			}
			catch (JSONException e)
			{
				Logger.e("FileUpload", "Unable to send in Json");
				return "";
			}

		}

	}

	/**
	 * analytics json : {"d":{"ep":"HIGH","st":"filetransfer","et":"nonUiEvent","md":{"sid":1460011903528,"fld1":"pushkar11","ek":"micro_app","fld2":"+pushkar11+","event":"ttlExpired"},"cts":1460011966846,"tag":"plf"},"t":"le_android"}
	 * @param name
	 */
	public static void sendFailDueToTTL(String name)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
			json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.TTL_EXPIRED);
			json.putOpt(AnalyticsConstants.LOG_FIELD_1, name);
			json.putOpt(AnalyticsConstants.LOG_FIELD_2,getMsisdnFromAppName(name));
		} catch (JSONException e)
		{
			Logger.e(TAG,"Errorin sending analytics");
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.FILE_TRANSFER, json);
	}

    /**
     * Sample log lines : { "t": "le_android", "d": { "et": "nonUiEvent", "st": "dwnld", "ep": "HIGH", "cts": 1453620927336, "tag": "plf", "md": { "ek": "micro_app", "event":
     * "exception_track", "fld2": "java.io.FileNotFoundException: abc", "platformUid": "VTBoRgRzkEkRVAu3", "networkType": "1", "app_version": "4.1.0.36",
     * "sid": 1453620914078 } } }
     *
     * @param errorMsg
     */
    public static void microappsMigrationFailedAnalytics(String errorMsg)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
            json.put(AnalyticsConstants.EVENT, AnalyticsConstants.MIGRATION_FAILURE_ANALYTICS);
            json.put(AnalyticsConstants.LOG_FIELD_2, errorMsg); //Error
            HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }
    
}
