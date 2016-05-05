package com.bsb.hike.platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.GroupDetails;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;

public class PlatformHelper
{

	public static Handler mHandler;

	public static final String TAG = "PlatformHelper";

	// Call this method to put data in cache. This will be a key-value pair.
	public static void putInCache(String key, String value, String namespace)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, namespace, value);
	}

	// Call this function to get data from cache corresponding to a key
	public static String getFromCache(String key, String namespace)
	{
		return HikeContentDatabase.getInstance().getFromContentCache(key, namespace);

	}

	// Function to log Analytics
	public static void logAnalytics(String isUI, String subType, String json, BotInfo mBotInfo)
	{

		try
		{
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.BOT_MSISDN, mBotInfo.getMsisdn());
			jsonObject.put(AnalyticsConstants.BOT_NAME, mBotInfo.getConversationName());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	// Function to log Analytics
	public static void logAnalytics(String isUI, String subType, String json, String botMsisdn, String botName)
	{

		try
		{
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.BOT_MSISDN, botMsisdn);
			jsonObject.put(AnalyticsConstants.BOT_NAME, botName);
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	// Function to forward to chat
	public static void forwardToChat(String json, String hikeMessage, BotInfo mBotInfo, Activity activity)
	{
		Logger.i(TAG, "Received this json in forward to chat : " + json + "\n Received this hm : " + hikeMessage);

		if (TextUtils.isEmpty(json) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}

		try
		{
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			JSONObject cardObj = new JSONObject(json);

			/**
			 * Blindly inserting the appName in the cardObj JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());
            /*
             *  Adding these fields for determining compatibility and making sync call to server on recipient (Code added in versioning release)
             */

            // Add mAppVersionCode from forward Card if its present in the bot
            if(metadata.getFwdCardObj() != null)
            {
                JSONObject forwardCardObj = metadata.getFwdCardObj();
                int forwardCardMAppVersionCode = forwardCardObj.optInt(HikePlatformConstants.MAPP_VERSION_CODE,-1);
                cardObj.put(HikePlatformConstants.MAPP_VERSION_CODE,forwardCardMAppVersionCode);
            }
            else
            {
                cardObj.put(HikePlatformConstants.MAPP_VERSION_CODE, metadata.getmAppVersionCode());
            }

			JSONObject webMetadata = new JSONObject();
			webMetadata.put(HikePlatformConstants.TARGET_PLATFORM, metadata.getTargetPlatform());
			webMetadata.put(HikePlatformConstants.CARD_OBJECT, cardObj);
			webMetadata.put(HikePlatformConstants.FORWARD_CARD_OBJECT, metadata.getFwdCardObj());

            ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());
			message.setNameSpace(mBotInfo.getNamespace());
			if (message != null)
			{
				startComPoseChatActivity(message, activity);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendNormalEvent(String messageHash, String eventData, String namespace, BotInfo botInfo)
	{
		PlatformUtils.sendPlatformMessageEvent(eventData, messageHash, namespace, botInfo);
	}

	public static void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo, final Activity activity, int hashcode)
	{
		if (TextUtils.isEmpty(cardObject) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}

		try
		{
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			JSONObject cardObj = new JSONObject(cardObject);

			boolean isGroupFirst = true;
			ArrayList<String> composeExcludedList = new ArrayList<>();
			if(cardObj.has(HikeConstants.Extras.IS_GROUP_FIRST))
			{
				isGroupFirst = cardObj.getBoolean(HikeConstants.Extras.IS_GROUP_FIRST);
			}
			if(cardObj.has(HikeConstants.Extras.COMPOSE_EXCLUDE_LIST))
			{
				JSONArray composeExcludedArray = cardObj.getJSONArray(HikeConstants.Extras.COMPOSE_EXCLUDE_LIST);
				for(int i = 0 ; i < composeExcludedArray.length(); i++)
				{
					composeExcludedList.add(composeExcludedArray.getString(i));
				}
			}


			/**
			 * Blindly inserting the appName in the cardObject JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());

            /*
             *  Adding these fields for determining compatibility and making sync call to server on recipient (Code added in versioning release)
             */
            JSONObject forwardCardObj = metadata.getFwdCardObj();
            int mAppVersionCode = forwardCardObj.optInt(HikePlatformConstants.MAPP_VERSION_CODE,-1);
            cardObj.put(HikePlatformConstants.MAPP_VERSION_CODE,mAppVersionCode);

			JSONObject webMetadata = new JSONObject();
			webMetadata.put(HikePlatformConstants.TARGET_PLATFORM, metadata.getTargetPlatform());
			webMetadata.put(HikePlatformConstants.CARD_OBJECT, cardObj);
			webMetadata.put(HikePlatformConstants.FORWARD_CARD_OBJECT, metadata.getFwdCardObj());

			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());

			message.setParticipantInfoState(ConvMessage.ParticipantInfoState.NO_INFO);
			JSONObject sharedDataJson = new JSONObject(sharedData);
			sharedDataJson.getJSONObject(HikePlatformConstants.EVENT_CARDDATA).put(HikePlatformConstants.EVENT_FROM_USER_MSISDN, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null));

			NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			sharedDataJson.getJSONObject(HikePlatformConstants.EVENT_CARDDATA).put(HikePlatformConstants.PARENT_MSISDN, nonMessagingBotMetadata.getParentMsisdn());

			sharedDataJson.put(HikePlatformConstants.EVENT_TYPE, HikePlatformConstants.SHARED_EVENT);

			message.setPlatformData(sharedDataJson);
			message.setNameSpace(mBotInfo.getNamespace());
			pickContactAndSend(message, activity, hashcode, isGroupFirst, composeExcludedList);

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo, final Activity activity)
	{
		sendSharedMessage(cardObject, hikeMessage, sharedData, mBotInfo, activity, -1);
	}

	public static String getAllEventsForMessageHash(String messageHash, String namespace)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(TAG, "can't return all events as the message hash is " + messageHash);
			return null;
		}
		String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHash(messageHash, namespace);
		return eventData;
	}

	public static String getAllEventsForMessageHashFromUser(String messageHash, String namespace, String fromUserId)
	{
		if (TextUtils.isEmpty(messageHash) || TextUtils.isEmpty(fromUserId))
		{
			Logger.e(TAG, "can't return all events as the message hash is " + messageHash);
			Logger.e(TAG, "can't return all events as the fromUserId is " + fromUserId);
			return null;
		}
		String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHashFromUser(messageHash, namespace, fromUserId);
		return eventData;
	}

	public static String getAllEventsData(String namespace)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, true);
		return messageData;
	}

	public static String getSharedEventsData(String namespace)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, false);
		return messageData;
	}

	public static String getGroupDetails(String groupId)
	{
		try {
			GroupDetails groupDetails = ContactManager.getInstance().getGroupDetails(groupId);
			JSONObject groupDetailsJson = new JSONObject();
			groupDetailsJson.put("name", groupDetails.getGroupName());
			BitmapDrawable bitmap = HikeMessengerApp.getLruCache().getIconFromCache(groupId);
			if(bitmap !=null)
			{
				String picture = Utils.drawableToString(bitmap);
				File groupPicFile = new File(HikeMessengerApp.getInstance().getExternalCacheDir(), "group_"+ groupId + ".jpg");
				if(!groupPicFile.exists())
				{
					groupPicFile.createNewFile();
					Utils.saveByteArrayToFile(groupPicFile, picture.getBytes());
				}
				groupDetailsJson.put("picture" , groupPicFile.getAbsolutePath());
			}
			else
			{
				groupDetailsJson.put("picture" , "");
			}

			Map<String, JSONObject> msisdnDetailsMap = new HashMap<>();
			Iterator<PairModified<GroupParticipant, String>> iterator = ContactManager.getInstance().getGroupParticipants(groupId, false, false).iterator();
			while(iterator.hasNext())
			{
				PairModified<GroupParticipant, String> pairModified = iterator.next();
				String msisdn = pairModified.getFirst().getContactInfo().getMsisdn();
				String name = pairModified.getSecond();

				JSONObject nameJSON = new JSONObject();
				nameJSON.put("name", name);
				BitmapDrawable participantBitmap = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
				if(participantBitmap !=null)
				{
					String picture = Utils.drawableToString(participantBitmap);
					File contactPicFile = new File(HikeMessengerApp.getInstance().getExternalCacheDir(), groupId + "_" + name + ".jpg");
					if(!contactPicFile.exists())
					{
						contactPicFile.createNewFile();
						Utils.saveByteArrayToFile(contactPicFile, picture.getBytes());
					}
					nameJSON.put("picture", contactPicFile);
				}
				else
				{
					nameJSON.put("picture", "");
				}
				msisdnDetailsMap.put(msisdn, nameJSON);
			}

			JSONObject groupParticipants = new JSONObject(new Gson().toJson(msisdnDetailsMap));
			groupDetailsJson.put("participants", groupParticipants);
			return groupDetailsJson.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void pickContactAndSend(ConvMessage message, final Activity activity, int hashcode)
	{
		if (activity != null)
		{
			final Intent intent = IntentFactory.getForwardIntentForConvMessage(activity, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()), false);
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PICK_CONTACT_AND_SEND_MODE);
			if (hashcode < 0)
				intent.putExtra(JavascriptBridge.tag, hashcode);
			intent.putExtra(HikePlatformConstants.REQUEST_CODE, JavascriptBridge.PICK_CONTACT_AND_SEND_REQUEST);
			intent.putExtra(HikeConstants.Extras.THUMBNAILS_REQUIRED, true);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);

		}
	}

	public static void pickContactAndSend(ConvMessage message, final Activity activity, int hashcode, boolean isGroupFirst, ArrayList<String> composeExcludedList)
	{
		if (activity != null)
		{
			final Intent intent = IntentFactory.getForwardIntentForConvMessage(activity, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()), false);
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PICK_CONTACT_AND_SEND_MODE);
			if (hashcode < 0)
				intent.putExtra(JavascriptBridge.tag, hashcode);
			intent.putExtra(HikePlatformConstants.REQUEST_CODE, JavascriptBridge.PICK_CONTACT_AND_SEND_REQUEST);
			intent.putExtra(HikeConstants.Extras.THUMBNAILS_REQUIRED, true);
			intent.putExtra(HikeConstants.Extras.IS_GROUP_FIRST, isGroupFirst);
			intent.putStringArrayListExtra(HikeConstants.Extras.COMPOSE_EXCLUDE_LIST, composeExcludedList);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);

		}
	}

	public static void startComPoseChatActivity(final ConvMessage message, final Activity mContext)
	{
		PlatformHelper.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
		if (null == mHandler)
		{
			Log.e(TAG, "handler is null");
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mContext != null)
				{
					final Intent intent = IntentFactory.getForwardIntentForConvMessage(mContext, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()),
							true);
					mContext.startActivity(intent);
				}
				else
				{
					Log.d(TAG, "context is null");
				}
			}
		});
	}

	public static void deleteEvent(String eventId)
	{
		if (TextUtils.isEmpty(eventId))
		{
			Logger.e(TAG, "event can't be deleted as the event id is " + eventId);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteEvent(eventId);
	}

	public static void deleteAllEventsForMessage(String messageHash)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(TAG, "the events corresponding to the message hash can't be deleted as the message hash is " + messageHash);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForMessage(messageHash);
	}

	public static void postStatusUpdate(String status, String moodId, String imageFilePath)
	{
		int mood;

		try
		{
			mood = Integer.parseInt(moodId);
		}
		catch (NumberFormatException e)
		{
			Logger.e(TAG, "moodId to postStatusUpdate should be a number.");
			mood = -1;
		}

		Utils.postStatusUpdate(status, mood, imageFilePath);

	}

	public static void showPopup(String contentData, final Activity activity)
	{
		final HikeBaseActivity hikeBaseActivity;
		if (TextUtils.isEmpty(contentData) || activity == null)
		{
			Logger.e(TAG, "Either activity or contentData to showPopup is null. Returning.");
			return;
		}
		if (activity instanceof HikeBaseActivity)
		{
			hikeBaseActivity = (HikeBaseActivity) activity;
		}
		else
		{
			Logger.e(TAG, "Activity passed to showPopup is not subclass of HikeAppStateBaseFragmentActivity. Returning.");
			return;
		}
		try
		{
			JSONObject data = new JSONObject(contentData);
			if (!checkContentData(data))
			{
				return;
			}
			final ProductContentModel mmModel = ProductContentModel.makeProductContentModel(data);
			ProductInfoManager.getInstance().parseAndShowPopup(mmModel, new IActivityPopup()
			{
				@Override
				public void onSuccess(ProductContentModel productContentModel)
				{
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							hikeBaseActivity.showPopupDialog(mmModel);
						}
					});

				}

				@Override
				public void onFailure()
				{
					Logger.e(TAG, "Failure occured when opening popup.");
				}
			});

		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in showPopup : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static boolean checkContentData(JSONObject data) throws JSONException
	{
		if (data.has(HikePlatformConstants.CARD_OBJECT))
		{
			JSONObject cardObj = data.optJSONObject(HikePlatformConstants.CARD_OBJECT);
			if (cardObj == null)
			{
				Logger.e(TAG, "cardObj is null in contentData. Returning.");
				return false;
			}
			if (!cardObj.has(HikePlatformConstants.LAYOUT_DATA))
			{
				cardObj.put(HikePlatformConstants.LAYOUT_DATA, new JSONObject());
			}
			else
			{
				if (!(cardObj.get(HikePlatformConstants.LAYOUT_DATA) instanceof JSONObject))
				{
					cardObj.put(HikePlatformConstants.LAYOUT_DATA, new JSONObject());
				}
			}
		}
		else
		{
			Logger.e(TAG, "cardObj not present in contentData. Returning.");
			return false;
		}
		return true;
	}

	public static void chooseFile(final String id,final String displayCameraItem,final Context weakActivityRef)
	{
		mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
		if (null == mHandler)
		{
			Logger.e("FileUpload", "mHandler is null");
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (weakActivityRef != null)
				{
					int galleryFlags;
					if (Boolean.valueOf(displayCameraItem))
					{
						galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;
					}
					else
					{
						galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS;
					}
					File newSentFile = Utils.createNewFile(HikeFile.HikeFileType.IMAGE, "", true);
					if (newSentFile != null)
					{
						galleryFlags = galleryFlags | GalleryActivity.GALLERY_CROP_IMAGE; // This also gives an option to edit/rotate
					}

					Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(weakActivityRef, galleryFlags, newSentFile == null ? null : newSentFile.getAbsolutePath());
					galleryPickerIntent.putExtra(HikeConstants.CALLBACK_ID, id);

					((Activity) weakActivityRef).startActivityForResult(galleryPickerIntent, HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST);
				}
			}
		});
	}

}