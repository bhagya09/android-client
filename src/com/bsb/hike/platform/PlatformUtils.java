package com.bsb.hike.platform;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.*;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.platform.content.*;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.*;

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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author piyush
 * 
 *         Class for all Utility methods related to Platform code
 */
public class PlatformUtils
{
	private static final String TAG = "PlatformUtils";
	
	private static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
	
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
	 * Call this function to merge two JSONObjects. Will iterate for the keys present in the dataDiff. Will add the key in the oldData if not already
	 * present or will update the value in oldData if the key is present.
	 * @param oldData : the data that wants to be merged.
	 * @param dataDiff : the diff that will be merged with the old data.
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
					Utils.launchPlayStore(url, context);
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
			if(activityName.equals(HIKESCREEN.ACCESS.toString()))
			{
				IntentFactory.openAccessibilitySettings(context);
			}
			if (activityName.equals(HIKESCREEN.GAME_ACTIVITY.toString()))
			{
				String extraData;
				String msisdn = mmObject.optString(HikeConstants.MSISDN);
				extraData=mmObject.optString(HikeConstants.DATA);
				Intent i=IntentFactory.getNonMessagingBotIntent(msisdn,context,extraData);
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
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (ActivityNotFoundException e)
		{
			Toast.makeText(context, "No activity found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

	/**
	 * download the microapp and then set the state to whatever that has been passed by the server.
	 * @param botInfo
	 * @param enableBot
	 */
	public static void downloadZipForNonMessagingBot(final BotInfo botInfo, final boolean enableBot, final String botChatTheme, final String notifType, NonMessagingBotMetadata botMetadata, boolean resumeSupport)
	{

        byte requestType = PlatformContentModel.HIKE_MICRO_APPS;
        if(botMetadata.isNativeMode())
            requestType = PlatformContentModel.NATIVE_APPS;

		PlatformContentRequest rqst = PlatformContentRequest.make(
				PlatformContentModel.make(botInfo.getMetadata(),requestType), new PlatformContentListener<PlatformContentModel>()
				{

					long zipFileSize = 0;

					@Override
					public void onComplete(PlatformContentModel content)
					{
						Logger.d(TAG, "microapp download packet success.");
						botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
					}

					@Override
					public void onEventOccured(int uniqueCode,PlatformContent.EventCode event)
					{
						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							//do nothing
							return;
						}
						else if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
						{
							Logger.d(TAG, "microapp already exists");
							botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
						}
						else
						{
							Logger.wtf(TAG, "microapp download packet failed." + event.toString());
							JSONObject json = new JSONObject();
							try
							{
								json.put(HikePlatformConstants.ERROR_CODE, event.toString());
								if (zipFileSize > 0)
								{
									json.put(AnalyticsConstants.FILE_SIZE, String.valueOf(zipFileSize));
								}
								json.put(AnalyticsConstants.INTERNAL_STORAGE_SPACE, String.valueOf(Utils.getFreeInternalStorage()) + " MB");
								createBotAnalytics(HikePlatformConstants.BOT_CREATION_FAILED, botInfo, json);
								createBotMqttAnalytics(HikePlatformConstants.BOT_CREATION_FAILED_MQTT, botInfo, json);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}

						}
					}

					@Override
					public void downloadedContentLength(long length)
					{
						zipFileSize = length;
					}
				});

        if(botMetadata.isNativeMode()) {
            rqst.setRequestType(PlatformContentRequest.NATIVE_APPS);
            rqst.getContentData().setRequestType(PlatformContentModel.NATIVE_APPS);
        }
		downloadAndUnzip(rqst, false,botMetadata.shouldReplace(), botMetadata.getCallbackId(),resumeSupport,botInfo.getMsisdn());

	}

	public static void botCreationSuccessHandling(BotInfo botInfo, boolean enableBot, String botChatTheme, String notifType)
	{
		enableBot(botInfo, enableBot);
		BotUtils.updateBotParamsInDb(botChatTheme, botInfo, enableBot, notifType);
		createBotAnalytics(HikePlatformConstants.BOT_CREATED, botInfo);
		createBotMqttAnalytics(HikePlatformConstants.BOT_CREATED_MQTT, botInfo);
	}

	private static void createBotMqttAnalytics(String key, BotInfo botInfo)
	{
		createBotMqttAnalytics(key, botInfo, null);
	}

	private static void createBotMqttAnalytics(String key, BotInfo botInfo, JSONObject json)
	{

		try
		{
			JSONObject metadata = json == null ? new JSONObject() : new JSONObject(json.toString());
			JSONObject data = new JSONObject();
			data.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.NON_UI_EVENT);

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

	private static void createBotAnalytics(String key, BotInfo botInfo, JSONObject json)
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
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void enableBot(BotInfo botInfo, boolean enableBot)
	{
		if (enableBot && botInfo.isNonMessagingBot())
		{
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(botInfo);
		}
	}

	/**
	 * download the microapp, can be used by nonmessaging as well as messaging only to download and unzip the app.
	 * @param downloadData: the data used to download microapp from ac packet to download the app.
	 */
	public static void downloadZipFromPacket(final JSONObject downloadData)
	{
		if (downloadData == null)
		{
			return;
		}

		final PlatformContentModel platformContentModel = PlatformContentModel.make(downloadData.toString());
		PlatformContentRequest rqst = PlatformContentRequest.make(
				platformContentModel, new PlatformContentListener<PlatformContentModel>()
				{
					long fileLength = 0;

					@Override
					public void onComplete(PlatformContentModel content)
					{
						microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, content);
						Logger.d(TAG, "microapp download packet success.");
					}

					@Override
					public void onEventOccured(int uniqueId,PlatformContent.EventCode event)
					{

						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							//do nothing
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
								Logger.e(TAG, "JSONException " +e.getMessage());
							}

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
				boolean doReplace = downloadData.optBoolean(HikePlatformConstants.REPLACE_MICROAPP_VERSION);
				String callbackId = downloadData.optString(HikePlatformConstants.CALLBACK_ID);
				downloadAndUnzip(rqst, false,doReplace, callbackId);

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
	
	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled , boolean doReplace)
	{
		downloadAndUnzip(request, isTemplatingEnabled, doReplace, null);
	}
	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled)
	{
		downloadAndUnzip(request, isTemplatingEnabled, false);
	}
	
	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled, boolean doReplace, String callbackId, boolean resumeSupported,String msisdn)
	{
		PlatformZipDownloader downloader =  new PlatformZipDownloader(request, isTemplatingEnabled, doReplace, callbackId, resumeSupported,msisdn);
		if (!downloader.isMicroAppExist() || doReplace)
		{
			downloader.downloadAndUnzip();
		}
		else
		{
			request.getListener().onEventOccured(request.getContentData()!=null ? request.getContentData().getUniqueId() : 0,PlatformContent.EventCode.ALREADY_DOWNLOADED);
		}
	}

	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled, boolean doReplace, String callbackId)
	{
		downloadAndUnzip(request, isTemplatingEnabled, doReplace, callbackId, false,"");
	}

	/**
	 * Creating a forwarding message for Non-messaging microApp
	 * @param metadata: the metadata made after merging the json given by the microApp
	 * @param text:     hm text
	 * @return
	 */
	public static ConvMessage getConvMessageFromJSON(JSONObject metadata, String text, String msisdn) throws JSONException
	{


		ConvMessage convMessage = Utils.makeConvMessage(msisdn, true);
		convMessage.setMessage(text);
		convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
		convMessage.webMetadata = new WebMetadata(PlatformContent.getForwardCardData(metadata.toString()));
		convMessage.setMsisdn(msisdn);
		return convMessage;

	}
	
	public static byte[] prepareFileBody(String filePath)
	{
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";
		File file = new File(filePath);
		if(file.exists() && !file.isDirectory()){
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
			Logger.e("fileUplaod","file body not present");
			return null;
		}
	    finally
	    {
		    try
			{
	    		if(fileInputStream != null)
	    		{
					fileInputStream.close();
	    		}
			}
			catch (IOException e)
			{
				Logger.e("fileUpload","Couldn't Read File");
			}
	    }
	    return setupFileBytes(boundaryMessage, boundary, chunkSize,fileContent);
		}
		else
		{
			Logger.e("fileUpload","Invalid file Path");
			return null;
		}
	}
	
	public static void uploadFile(final String filePath,final String url,final IFileUploadListener fileListener)
	{
		if(filePath == null)
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
			    if(fileBytes!=null)
			    {
				String response = send(fileBytes,filePath,url,fileListener);
				Logger.d("FileUpload", response);
			    }
			    else
			    {
			    	Logger.e("fileUpload","Empty File Body");
			    	return ;
			    }
			}
		});

	}
	
	private static String send(byte[] fileBytes,final String filePath,final String url,IFileUploadListener filelistener)
	{
		HttpClient client =  AccountUtils.getClient(null);
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
			post.setEntity(new ByteArrayEntity(fileBytes));
			HttpResponse response = client.execute(post);
			Logger.d("FileUpload", response.toString());
			resCode = response.getStatusLine().getStatusCode();
			
			res = EntityUtils.toString(response.getEntity());
			Logger.d("FileUpload",""+resCode);
		}
		catch (IOException | NullPointerException ex)
		{
			Logger.e("FileUpload", ex.toString());
			filelistener.onRequestFailure(ex.toString());
			return ex.toString();
		}
		Logger.d("FileUpload", res);
		if(resCode == 200)
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
	 * Sets up the file byte array with boundary message File Content and boundary
	 * returns the completed setup file byte array
	 */
	private static byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize,byte[] fileContent)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + fileContent.length + boundary.length()];
		try
		{
			System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
			System.arraycopy(fileContent, 0, fileBytes, boundaryMesssage.length(), fileContent.length);
			System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + fileContent.length, boundary.length());
		}
		catch(NullPointerException | ArrayStoreException | IndexOutOfBoundsException e)
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
			headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME,
					HikePlatformConstants.PLATFORM_TOKEN + "=" + platformToken + "; " + HikePlatformConstants.PLATFORM_USER_ID + "=" + platformUID));

			return headers;
		}
		return new ArrayList<Header>();
	}
	
	/*
	 * This function is called to read the list of files from the System from a folder
	 * 
	 * @param filePath : The complete file path that is about to be read returns the JSON Array of the file paths of the all the files in a folder
	 * @param doDeepLevelAccess : To specify if we want to read all the internal files and folders recursively
	 */
	public static JSONArray readFileList(String filePath,boolean doDeepLevelAccess)
	{	
		File directory = new File(filePath);
		if (directory.exists() && !directory.isDirectory())
		{
			Logger.d("FileSystemAccess", "Cannot read a single file");
			return null;
		}
		else if(!directory.exists())
		{
			Logger.d("FileSystemAccess", "Invalid file path!");
			return null;
		}
		ArrayList<File> list = filesReader(directory,doDeepLevelAccess);
		JSONArray mArray = new JSONArray();
		for (int i = 0; i < list.size(); i++)
		{
			String path = HikePlatformConstants.FILE_DESCRIPTOR + list.get(i).getAbsolutePath();// adding the file descriptor
			mArray.put(path);
		}
		return mArray;
	}
	
	public static JSONArray trimFilePath(JSONArray mArray)
	{
		JSONArray trimmedArray = new JSONArray();
		for (int i = 0; i < mArray.length(); i++)
		{
			String path;
			try
			{
				path = mArray.get(i).toString();
				path = path.replaceAll(PlatformContentConstants.PLATFORM_CONTENT_DIR, "");
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
	public static ArrayList<File> filesReader(File root,boolean doDeepLevelAccess)
	{
		ArrayList<File> a = new ArrayList<>();

		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			if (doDeepLevelAccess)
			{
				if(files[i].isDirectory())
				{
					a.addAll(filesReader(files[i],doDeepLevelAccess));	
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
	 * @param sourceLocation : The folder which is about to be copied
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
			  byte[] buf = new byte[1024]; int len;
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
	 * @param filePath : The complete file path of the file that is about to be deleted returns whether the file is deleted or not
	 * Does not return a guaranteed call for a full delete
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
				StickerCategory category = new StickerCategory(categoryId, categoryName, totalStickers, categorySize);
				downloadStkPk(category);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static  void downloadStkPk(StickerCategory category)
	{
		StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(category.getCategoryId());
		stickerPalleteImageDownloadTask.execute();
		StickerManager.getInstance().initialiseDownloadStickerTask(category, DownloadSource.POPUP, DownloadType.NEW_CATEGORY, HikeMessengerApp.getInstance().getApplicationContext());

	}
	
	public static  void OnChatHeadPopupActivateClick()
	{
		Context context = HikeMessengerApp.getInstance();
		if (ChatHeadUtils.areWhitelistedPackagesSharable(context))
		{
			Toast.makeText(context, context.getString(R.string.sticker_share_popup_activate_toast), Toast.LENGTH_LONG).show();
			if (ChatHeadUtils.checkDeviceFunctionality())
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, true);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, true);
				JSONArray packagesJSONArray;
				try
				{
					packagesJSONArray = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null));
					if (packagesJSONArray != null)
					{
						ChatHeadUtils.setAllApps(packagesJSONArray, true);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				ChatHeadUtils.startOrStopService(true);
			}
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
			json.put(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.APP_CRASH_EVENT);
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
			if(conv.getPlatformData() != null)
			{
				JSONObject sharedData = conv.getPlatformData();
				String namespaces = sharedData.getString(HikePlatformConstants.RECIPIENT_NAMESPACES);
				if (TextUtils.isEmpty(namespaces))
				{
					Logger.e(HikePlatformConstants.TAG, "no namespaces defined.");
					return;
				}
				String [] namespaceList = namespaces.split(",");
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
					MessageEvent messageEvent = new MessageEvent(eventType, conv.getMsisdn(), namespace, metadata, conv.createMessageHash(), state, conv.getSendTimestamp(), mappedEventId);
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
			//TODO catch block
			e.printStackTrace();
		}

	}

	/**
	 * Call this method to send platform message event. This method sends an event to the msisdn that it determines when it queries the messages table based on the message hash.
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
	 * Used to record analytics for bot opens via push notifications
	 * Sample JSON : {"ek":"bno","bot_msisdn":"+hikecricketnew+"}
	 */
	public static void recordBotOpenViaNotification(String msisdn)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.BOT_NOTIF_TRACKER);
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
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
		return HikeContentDatabase.getInstance().getFromContentCache(HikePlatformConstants.LAST_GAME,BotUtils.getBotInfoForBotMsisdn(HikePlatformConstants.GAME_CHANNEL).getNamespace());
	}
	public static void killProcess(Activity context,String process)
	{
		if (context != null)
		{
			ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
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

}
