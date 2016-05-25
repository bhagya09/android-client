package com.bsb.hike.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickerShareSettings;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.cropimage.CropCompression;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.platform.CocosGamingActivity;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.UpgradeIntentService;
import com.bsb.hike.spaceManager.StorageSpecIntentService;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.ui.ApkSelectionActivity;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.ConnectedAppsActivity;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.FileSelectActivity;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.GallerySelectionViewer;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.ui.HikeDirectHelpPageActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.HomeFtueActivity;
import com.bsb.hike.ui.NUXInviteActivity;
import com.bsb.hike.ui.NuxSendCustomMessageActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.PictureEditer;
import com.bsb.hike.ui.PinHistoryActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.ProfilePicActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.modules.fusedlocation.ShareLocation;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.modules.packPreview.PackPreviewActivity;
import com.bsb.hike.ui.StickerSettingsActivity;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;
import com.bsb.hike.voip.view.CallRateActivity;
import com.bsb.hike.voip.view.VoIPActivity;
import com.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IntentFactory
{
	public static void openSetting(Context context)
	{
		context.startActivity(new Intent(context, SettingsActivity.class));
	}

	public static void openSettingNotification(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.notification_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.notifications);
		context.startActivity(intent);
	}
	public static Intent shareFunctionality(Intent intent, ConvMessage message, MessagesAdapter mAdapter, int shareableMessagesCount,Context context)
	{   
		int share_type = HikeConstants.Extras.NOT_SHAREABLE ;
	
		boolean showShareFunctionality = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.SHOW_SHARE_FUNCTIONALITY, false);
		if (mAdapter.getSelectedCount() == 1 && Utils.isPackageInstalled(context, HikeConstants.Extras.WHATSAPP_PACKAGE) && showShareFunctionality)
		{
			if (message.isStickerMessage())
			{
				share_type = HikeConstants.Extras.ShareTypes.STICKER_SHARE;
			}

			if (message.isImageMsg())
			{
				share_type = HikeConstants.Extras.ShareTypes.IMAGE_SHARE;
			}

			if (message.isTextMsg())
			{
				share_type = HikeConstants.Extras.ShareTypes.TEXT_SHARE;
			}

			switch (share_type)
			{
			case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
				Sticker sticker = message.getMetadata().getSticker();
				String filePath = StickerManager.getInstance().getStickerDirectoryForCategoryId(sticker.getCategoryId()) + HikeConstants.LARGE_STICKER_ROOT;
				File stickerFile = new File(filePath, sticker.getStickerId());
				String filePathBmp = stickerFile.getAbsolutePath();
				intent.putExtra(HikeConstants.Extras.SHARE_TYPE, HikeConstants.Extras.ShareTypes.STICKER_SHARE);
				intent.putExtra(HikeConstants.Extras.SHARE_CONTENT, filePathBmp);
				intent.putExtra(StickerManager.STICKER_ID, sticker.getStickerId());
				intent.putExtra(StickerManager.CATEGORY_ID, sticker.getCategoryId());
				break;

			case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
				String text = message.getMessage();
				intent.putExtra(HikeConstants.Extras.SHARE_TYPE, HikeConstants.Extras.ShareTypes.TEXT_SHARE);
				intent.putExtra(HikeConstants.Extras.SHARE_CONTENT, text);
				break;

			case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
				if (shareableMessagesCount == 1)
				{
					HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
					intent.putExtra(HikeConstants.Extras.SHARE_TYPE, HikeConstants.Extras.ShareTypes.IMAGE_SHARE);
					intent.putExtra(HikeConstants.Extras.SHARE_CONTENT, hikeFile.getExactFilePath());
				}
				break;
			}

		}
		return intent;
	}

	public static void openSettingPrivacy(Context context)
	{
		context.startActivity(Utils.getIntentForPrivacyScreen(context));
	}

	public static void openSettingMedia(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.media_download_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_media);
		context.startActivity(intent);
	}

	public static Intent shareIntent(String mimeType, String imagePath, String text, int type, String pkgName, boolean isFromChatHead)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(mimeType);
		if (!TextUtils.isEmpty(text))
		{
			intent.putExtra(Intent.EXTRA_TEXT, text);
		}
		if (isFromChatHead)
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		else
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}
		if (pkgName != null)
		{
			intent.setPackage(pkgName);
		}
		if (type != HikeConstants.Extras.ShareTypes.TEXT_SHARE)
		{

			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath));

		}
		return intent;
	}

	public static void openSettingSMS(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.sms_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.free_sms_txt);
		context.startActivity(intent);
	}

	public static void openSettingAccount(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.account_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
		context.startActivity(intent);
	}

	
	public static void openStickerSettings(Context context)
	{
		context.startActivity(getStickerShareSettingsIntent(context));
	}
	
	public static void openSettingHelp(Context context)
	{
		Intent intent = null;
		if (BotUtils.isBot(HikePlatformConstants.CUSTOMER_SUPPORT_BOT_MSISDN))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(HikePlatformConstants.CUSTOMER_SUPPORT_BOT_MSISDN);
			if (botInfo.isNonMessagingBot())
			{
				intent = getNonMessagingBotIntent(HikePlatformConstants.CUSTOMER_SUPPORT_BOT_MSISDN, context);
			}
			else
			{
				intent = getSettingHelpIntent(context);
			}
		}
		else
		{
			intent = getSettingHelpIntent(context);
		}

		if (intent != null)
		{
			context.startActivity(intent);
			helpOpenedAnalytics();
		}
	}

	private static void helpOpenedAnalytics()
	{
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.HELP_CLICKED);
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private static Intent getSettingHelpIntent(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
		return intent;
	}

	public static void openSettingChat(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.chat_settings_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_chat);
		context.startActivity(intent);
	}

	public static void openStickerSettingsActivity(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.sticker_settings_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_sticker);
		context.startActivity(intent);
	}

	public static void openSettingLocalization(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.keyboard_settings_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.language);
		context.startActivity(intent);
	}

	public static void openStickyCallerSettings(Context context, boolean isFromOutside)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.sticky_caller_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.sticky_caller_settings);
		if (isFromOutside)
		{
			ChatHeadUtils.insertHomeActivitBeforeStarting(intent);
		}
		else
		{
			context.startActivity(intent);
		}
	}
	
	public static void openInviteSMS(Context context)
	{
		context.startActivity(new Intent(context, HikeListActivity.class));
	}

	public static void openInviteWatsApp(Context context)
	{
		Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
		whatsappIntent.setType("text/plain");
		whatsappIntent.setPackage(HikeConstants.PACKAGE_WATSAPP);
		String inviteText = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY, context.getString(R.string.watsapp_invitation));
		String inviteToken = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
		inviteText = inviteText + inviteToken;
		whatsappIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
		try
		{
			context.startActivity(whatsappIntent);
		}
		catch (android.content.ActivityNotFoundException ex)
		{
			Toast.makeText(context.getApplicationContext(), "Could not find WhatsApp in System", Toast.LENGTH_SHORT).show();
		}
	}

	public static void openTimeLine(Context context)
	{
		context.startActivity(getTimelineIntent(context));
	}

	public static Intent getTimelineIntent(Context context)
	{
		return new Intent(context, TimelineActivity.class);
	}
	
	public static void openHikeExtras(Context context)
	{
		context.startActivity(getGamingIntent(context));
	}

	public static Intent getGamingIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String extraBotMsisdn = prefs.getString(HikeConstants.EXTRAS_BOT_MSISDN, null);
		if (!TextUtils.isEmpty(extraBotMsisdn) && BotUtils.isBot(extraBotMsisdn) && (BotUtils.getBotInfoForBotMsisdn(extraBotMsisdn)).isNonMessagingBot())
		{
			return getNonMessagingBotIntent(extraBotMsisdn, context);
		}
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String hikeExtrasUrl = prefs.getString(HikeConstants.HIKE_EXTRAS_URL, AccountUtils.gamesUrl);

		if (!TextUtils.isEmpty(hikeExtrasUrl))
		{
			Uri gamesUri = Utils.getFormedUri(context, hikeExtrasUrl, prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			gamesUri = appendLocaleToUri(gamesUri);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, gamesUri.toString());
		}

		String hikeExtrasName = prefs.getString(HikeConstants.HIKE_EXTRAS_NAME, context.getString(R.string.hike_extras));

		if (!TextUtils.isEmpty(hikeExtrasName))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, hikeExtrasName);
		}

		return intent;
	}

	public static void openHikeRewards(Context context)
	{
		context.startActivity(getRewardsIntent(context));
	}

	public static Intent getRewardsIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String rewardsBotMsisdn = prefs.getString(HikeConstants.REWARDS_BOT_MSISDN, null);
		if (!TextUtils.isEmpty(rewardsBotMsisdn) && BotUtils.isBot(rewardsBotMsisdn) && (BotUtils.getBotInfoForBotMsisdn(rewardsBotMsisdn)).isNonMessagingBot())
		{
			return getNonMessagingBotIntent(rewardsBotMsisdn, context);
		}
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String rewards_url = prefs.getString(HikeConstants.REWARDS_URL, AccountUtils.rewardsUrl);

		if (!TextUtils.isEmpty(rewards_url))
		{
			Uri rewardsUri = Utils.getFormedUri(context, rewards_url, prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
			rewardsUri = appendLocaleToUri(rewardsUri);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, rewardsUri.toString());
		}

		String rewards_name = prefs.getString(HikeConstants.REWARDS_NAME, context.getString(R.string.rewards));

		if (!TextUtils.isEmpty(rewards_name))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, rewards_name);
		}
		intent.putExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, true);

		return intent;
	}

	private static Uri appendLocaleToUri(Uri appendTo) {
		String localappLang = LocalLanguageUtils.getApplicationLocalLanguageLocale();
		if(!TextUtils.isEmpty(localappLang)){
			appendTo = appendTo.buildUpon().appendQueryParameter("locale", localappLang).build();
		}
		return appendTo;
	}

	public static Intent getStickerShareWebViewActivityIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
				HttpRequestConstants.getMorestickersUrl() + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, context.getString(R.string.more_stickers));

		return intent;
	}

	public static Intent getStickerShareSettingsIntent(Context context)
	{
		HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.HIKE_STICKER_SETTING);
		return new Intent(context, StickerShareSettings.class);
	}

	public static Intent createNewBroadcastActivityIntent(Context appContext)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), CreateNewGroupOrBroadcastActivity.class);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		return intent;
	}

	public static Intent openComposeChatIntentForBroadcast(Context appContext, String convId, String convName)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), ComposeChatActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString(HikeConstants.Extras.ONETON_CONVERSATION_NAME, convName);
		bundle.putString(HikeConstants.Extras.CONVERSATION_ID, convId);
		bundle.putBoolean(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.putExtra(HikeConstants.Extras.BROADCAST_CREATE_BUNDLE, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent openComposeChatIntentForGroup(Context appContext, String convId, String convName, int setting)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), ComposeChatActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString(HikeConstants.Extras.ONETON_CONVERSATION_NAME, convName);
		bundle.putString(HikeConstants.Extras.CONVERSATION_ID, convId);
		bundle.putInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS, setting);
		bundle.putBoolean(HikeConstants.Extras.CREATE_GROUP, true);
		intent.putExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getForwardStickerIntent(Context context, String stickerId, String categoryId)
	{
		Utils.sendUILogEvent(HikeConstants.LogEvent.FORWARD_MSG);
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		JSONArray multipleMsgArray = new JSONArray();
		try
		{
			JSONObject multiMsgFwdObject = new JSONObject();
			multiMsgFwdObject.putOpt(StickerManager.FWD_CATEGORY_ID, categoryId);
			multiMsgFwdObject.putOpt(StickerManager.FWD_STICKER_ID, stickerId);
			multipleMsgArray.put(multiMsgFwdObject);
		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}

		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
		return intent;
	}

	public static void createBroadcastIntent(Context appContext)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		appContext.startActivity(intent);
	}
	

	public static Intent getVideoRecordingIntent()
	{
		Intent newMediaFileIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!ChatThreadUtils.isBigVideoSharingEnabled()) {
			newMediaFileIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (0.9 * HikeConstants.MAX_FILE_SIZE));
		}
		Intent pickVideo = new Intent(Intent.ACTION_PICK).setType("video/*");
		return Intent.createChooser(pickVideo, "").putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { newMediaFileIntent });
	}

	public static Intent getLocationPickerIntent(Context context)
	{
		return new Intent(context, ShareLocation.class);
	}

	public static Intent getContactPickerIntent()
	{
		return new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
	}

	public static Intent getAudioShareIntent(Context context)
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		return intent;
	}

	public static Intent getFileSelectActivityIntent(Context context, String msisdn)
	{
		Intent intent = new Intent(context, FileSelectActivity.class);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		return intent;
	}

	/**
	 * Returns intent for viewing a user's profile screen
	 * 
	 * @param context
	 * @param isConvOnHike
	 * @param mMsisdn
	 * @return
	 */
	public static Intent getSingleProfileIntent(Context context, boolean isConvOnHike, String mMsisdn)
	{
		Intent intent = new Intent();

		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		/**
		 * Negation of is self chat true
		 */
		if (!(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "").equals(mMsisdn)))
		{
			intent.putExtra(HikeConstants.Extras.CONTACT_INFO, mMsisdn);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, isConvOnHike);
		}

		return intent;
	}

	/**
	 * Returns intent for viewing group profile screen
	 * 
	 * @param context
	 * @param mMsisdn
	 * @return
	 */

	public static Intent getGroupProfileIntent(Context context, String mMsisdn)
	{
		Intent intent = new Intent();

		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mMsisdn);

		return intent;
	}

	/**
	 * Returns intent for viewing broadcast profile screen
	 * 
	 * @param context
	 * @param mMsisdn
	 * @return
	 */

	public static Intent getBroadcastProfileIntent(Context context, String mMsisdn)
	{
		Intent intent = new Intent();

		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		intent.putExtra(HikeConstants.Extras.BROADCAST_LIST, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST, mMsisdn);

		return intent;
	}

	/**
	 * Used for retrieving the intent to place a call
	 * 
	 * @param mMsisdn
	 * @return
	 */
	public static Intent getCallIntent(String mMsisdn)
	{
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + mMsisdn));
		return callIntent;
	}

	public static Intent createChatThreadIntentFromMsisdn(Context context, String msisdnOrGroupId, boolean openKeyBoard, boolean newGroup, int source)
	{
		Intent intent = new Intent();

		intent.setClass(context, ChatThreadActivity.class);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdnOrGroupId);
		intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, ChatThreadUtils.getChatThreadType(msisdnOrGroupId));
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, openKeyBoard);
		intent.putExtra(HikeConstants.Extras.NEW_GROUP, newGroup);
		intent.putExtra(HikeConstants.Extras.CHAT_INTENT_TIMESTAMP, System.currentTimeMillis());
		intent.putExtra(ChatThreadActivity.CHAT_THREAD_SOURCE, source);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static Intent createChatThreadIntentFromContactInfo(Context context, ContactInfo contactInfo, boolean openKeyBoard, boolean newGroup, int source)
	{
		// If the contact info was made using a group conversation, then the
		// Group ID is in the contact ID
		boolean isGroupConv = OneToNConversationUtils.isOneToNConversation(contactInfo.getMsisdn());
		return createChatThreadIntentFromMsisdn(context, isGroupConv ? contactInfo.getId() : contactInfo.getMsisdn(), openKeyBoard, newGroup, source);
	}

	public static Intent createChatThreadIntentFromConversation(Context context, ConvInfo conversation, int source)
	{
		Intent intent = new Intent(context, ChatThreadActivity.class);
		if (conversation.getConversationName() != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, conversation.getConversationName());
		}
		if (conversation.getLastConversationMsg() != null)
		{
			intent.putExtra(HikeConstants.Extras.LAST_MESSAGE_TIMESTAMP, conversation.getLastConversationMsg().getTimestamp());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		String whichChatThread = ChatThreadUtils.getChatThreadType(conversation.getMsisdn());
		intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, whichChatThread);
		intent.putExtra(HikeConstants.Extras.CHAT_INTENT_TIMESTAMP, System.currentTimeMillis());
		intent.putExtra(ChatThreadActivity.CHAT_THREAD_SOURCE, source);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getHomeActivityIntent(Context context)
	{
		Intent intent = new Intent(context, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getComposeChatActivityIntent(Context context)
	{
		return new Intent(context, ComposeChatActivity.class);
	}

	/**
	 * Utility method to create an intent to share any file on hike
	 * @param context
	 * @param interceptUri resource uri of the file to share
	 * @param type type of file being shared
	 * @return created intent or null
	 */
	public static Intent getShareIntent(Context context, Uri interceptUri, String type) throws NullPointerException
	{
		if(interceptUri == null)
		{
			Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "Got null uri for share intent");
			return null;
		}
		else
		{
			Intent shareIntent = new Intent(context, ComposeChatActivity.class);
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_STREAM, interceptUri);
			shareIntent.setType(type);
			return shareIntent;
		}
	}

	/**
	 * Utility method to create an intent to set an image as hike dp
	 * @param context
	 * @param interceptUri content uri of the image
	 * @return created intent or null
	 */
	public static Intent setDpIntent(Context context, Uri interceptUri) throws NullPointerException
	{
		if(interceptUri == null)
		{
			Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "Got null uri for dp intent");
			return null;
		}
		else
		{
			Intent dpIntent = new Intent(context, ProfilePicActivity.class);
			dpIntent.putExtra(HikeMessengerApp.FILE_PATH, Utils.getAbsolutePathFromUri(interceptUri, context, false));
			return dpIntent;
		}

	}

	/**
	 * Utility method to create a PendingIntent which wraps an intercept broadcast action.
	 * Parameters can be provided differently if creating a new BroadcastReceiver
	 * @param context
	 * @param action custom intent action string
	 * @param type intercept type - Image/Video/ScreenShot
	 * @param interceptUri uri for the intercept item
	 * @return a PendingIntent which will broadcast the provided action
	 */
	public static PendingIntent getInterceptBroadcast(Context context, String action, String type, Uri interceptUri)
	{
		Intent intent = new Intent(action);
		intent.putExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_URI, interceptUri);
		intent.putExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_TYPE, type);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/**
	 * Utility method which will insert home activity intent before provided action intent and launch them.
	 * Implemented using PendingIntents.
	 * @param context
	 * @param actionIntent the intent to launch
	 */
	public static void openInterceptActionActivity(Context context, Intent actionIntent)
	{
		if(actionIntent != null)
		{
			Intent homeIntent = Utils.getHomeActivityIntent(context);
			actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Intent[] intentSequence = new Intent[] { homeIntent, actionIntent } ;
			PendingIntent actionPI = PendingIntent.getActivities(context, 0, intentSequence, PendingIntent.FLAG_ONE_SHOT);
			try
			{
				actionPI.send();
			}
			catch (PendingIntent.CanceledException e)
			{
				Logger.d("Intercepts","Pending Intent Cancelled Exception");
			}
		}
	}

	public static Intent getPinHistoryIntent(Context context, String msisdn)
	{
		Intent intent = new Intent();
		intent.setClass(context, PinHistoryActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.TEXT_PINS, msisdn);
		return intent;
	}

	public static Intent getForwardImageIntent(Context context, File argFile)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(argFile));
		intent.setType("image");
		return intent;
	}
	
	public static Intent getMultipleFileForwardIntent(Context context, ArrayList<Uri> filePaths,HikeFileType type)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filePaths);
		intent.setAction(Intent.ACTION_SEND_MULTIPLE);
		intent.setType(HikeFileType.toString(type));
		return intent;
	}

	/*
	TODO: Fix input params
	 */
	public static Intent getImageSelectionIntent(Context argContext, List<GalleryItem> argSelectedImages,boolean fromDeviceGallery)
	{
		return getImageSelectionIntent(argContext,argSelectedImages,fromDeviceGallery,false);
	}

	/*
	TODO: Fix input params
	 */
	public static Intent getImageSelectionIntent(Context argContext, List<GalleryItem> argSelectedImages,boolean fromDeviceGallery, boolean fromCameraCapture)
	{
		return getImageSelectionIntent(argContext, argSelectedImages, fromDeviceGallery, fromCameraCapture,null);
	}

	public static Intent getImageSelectionIntent(Context argContext, List<GalleryItem> argSelectedImages,boolean fromDeviceGallery, boolean fromCameraCapture, ParcelableSparseArray captions)
	{
		Intent multiIntent = new Intent(argContext,GallerySelectionViewer.class);
		multiIntent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, new ArrayList(argSelectedImages));
		multiIntent.putExtra(GallerySelectionViewer.FROM_DEVICE_GALLERY_SHARE, fromDeviceGallery);
		multiIntent.putExtra(GallerySelectionViewer.FROM_CAMERA_CAPTURE, fromCameraCapture);
		if(captions != null)
		{
			multiIntent.putExtra(HikeConstants.CAPTION,captions);
		}

		return multiIntent;
	}

	public static Intent getHikeGalleryPickerIntent(Context context, int flags,String outputDestination)
	{
		
		boolean allowMultiSelect = (flags & GalleryActivity.GALLERY_ALLOW_MULTISELECT )!=0;
		boolean categorizeByFolders = (flags & GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS)!=0;
		boolean enableCameraPick = (flags & GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM)!=0;
		boolean editSelectedImage = (flags & GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE)!=0;
		boolean compressEdited = (flags & GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE)!=0;
		boolean forProfileUpdate = (flags & GalleryActivity.GALLERY_FOR_PROFILE_PIC_UPDATE)!=0;
		boolean cropDPImage = (flags & GalleryActivity.GALLERY_CROP_FOR_DP_IMAGE)!=0;
		boolean cropImage = (flags & GalleryActivity.GALLERY_CROP_IMAGE)!=0;
		
		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		b.putBoolean(GalleryActivity.FOLDERS_REQUIRED_KEY, categorizeByFolders);
		b.putBoolean(GalleryActivity.ENABLE_CAMERA_PICK, enableCameraPick);
		
		ArrayList<Intent> destIntents = new ArrayList<Intent>();
		
		if(editSelectedImage && Utils.isPhotosEditEnabled())
		{
			destIntents.add(IntentFactory.getPictureEditorActivityIntent(context, null, compressEdited, cropDPImage?null:outputDestination, forProfileUpdate));
		}
		
		if(cropImage)
		{
			CropCompression compression = new CropCompression().maxWidth(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN).maxHeight(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN).quality(80);
			destIntents.add(IntentFactory.getCropActivityIntent(context, null, outputDestination, compression,true,false));
		}
		else if(cropDPImage)
		{
			CropCompression compression = new CropCompression().maxWidth(640).maxHeight(640).quality(80);
			destIntents.add(IntentFactory.getCropActivityIntent(context, null, outputDestination, compression,false,true));
		}
		
		if(destIntents.size()>0)
		{
			b.putParcelableArrayList(HikeBaseActivity.DESTINATION_INTENT, destIntents);
		}
		
		intent.putExtras(b);
		return intent;
	}

	public static Intent getProfilePicUpdateIntent(Context context, int galleryFlags)
	{

		boolean allowMultiSelect = (galleryFlags & GalleryActivity.GALLERY_ALLOW_MULTISELECT) != 0;
		boolean categorizeByFolders = (galleryFlags & GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS) != 0;
		boolean enableCameraPick = (galleryFlags & GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM) != 0;

		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		b.putBoolean(GalleryActivity.FOLDERS_REQUIRED_KEY, categorizeByFolders);
		b.putBoolean(GalleryActivity.ENABLE_CAMERA_PICK, enableCameraPick);

		ArrayList<Intent> destIntents = new ArrayList<Intent>();

		destIntents.add(new Intent(context, ProfilePicActivity.class));

		b.putParcelableArrayList(HikeBaseActivity.DESTINATION_INTENT, destIntents);

		intent.putExtras(b);
		return intent;
	}

	public static Intent getImageChooserIntent(Context context, int galleryFlags,String destFile, CropCompression cropCompression, boolean fixAspectRatio)
	{
		return getImageChooserIntent(context, galleryFlags, destFile,  cropCompression, fixAspectRatio, CropImageView.DEFAULT_ASPECT_RATIO_X, CropImageView.DEFAULT_ASPECT_RATIO_Y);
	}

	public static Intent getImageChooserIntent(Context context, int galleryFlags,String destFile, CropCompression cropCompression, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY)
	{

		boolean allowMultiSelect = (galleryFlags & GalleryActivity.GALLERY_ALLOW_MULTISELECT) != 0;
		boolean categorizeByFolders = (galleryFlags & GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS) != 0;
		boolean enableCameraPick = (galleryFlags & GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM) != 0;

		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		b.putBoolean(GalleryActivity.FOLDERS_REQUIRED_KEY, categorizeByFolders);
		b.putBoolean(GalleryActivity.ENABLE_CAMERA_PICK, enableCameraPick);

		ArrayList<Intent> destIntents = new ArrayList<Intent>();

		destIntents.add(getCropActivityIntent(context,null,destFile,cropCompression,true,fixAspectRatio, aspectRatioX, aspectRatioY));

		b.putParcelableArrayList(HikeBaseActivity.DESTINATION_INTENT, destIntents);

		intent.putExtras(b);
		return intent;
	}

	public static void openConnectedApps(Context appContext)
	{
		appContext.startActivity(new Intent(appContext, ConnectedAppsActivity.class));
	}

	public static void openHikeSDKAuth(Context appContext, Message msg)
	{
		Intent hikeAuthIntent = new Intent("com.bsb.hike.ui.HikeAuthActivity");
		hikeAuthIntent.putExtra(HikeAuthActivity.MESSAGE_INDEX, Message.obtain(msg));
		hikeAuthIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		hikeAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		appContext.startActivity(hikeAuthIntent);
	}

	public static void openWelcomeActivity(Context appContext)
	{
		Intent i = new Intent(appContext, WelcomeActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}

	public static void openSignupActivity(Context appContext)
	{
		Intent i = new Intent(appContext, SignupActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}

	public static void reopenSignupActivity(Context context)
	{
		Intent i = new Intent(context, SignupActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}

	//AND-3999 Begin - Flickering observed on FTUE launch
	public static void freshLaunchHomeFtueActivity(Context appContext)
	{
		Intent i = new Intent(appContext, HomeFtueActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}
	//AND-3999 End

	public static void openHomeFtueActivity(Context appContext)
	{
		Intent i = new Intent(appContext, HomeFtueActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		appContext.startActivity(i);
	}

	public static void openHomeActivity(Context context)
	{
		Intent in = new Intent(context, HomeActivity.class);
		in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(in);
	}

	public static void openHomeActivity(Context context, boolean clearTop)
	{
		Intent in = new Intent(context, HomeActivity.class);
		if (clearTop)
		{
			in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		context.startActivity(in);
	}

	/*
	 * The returned intent will be similar to the one used by android for opening an activity from the Launcher icon
	 */
	public static Intent getHomeActivityIntentAsLauncher(Context context)
	{
		Intent homeIntent = Intent.makeMainActivity(new ComponentName(context, HomeActivity.class));
		homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return homeIntent;
	}

	public static void freshLaunchHomeActivity(Context context){
		if(Utils.isLollipopOrHigher()){
			context.startActivity(IntentFactory.getHomeActivityIntentAsFreshLaunch(context));
		}else {
			relaunchApplicationWithPendingIntent(context);
		}
	}
	/*This will not send FG, BG packet to the server*/
	public static Intent getHomeActivityIntentAsFreshLaunch(Context context)
	{
		Intent homeIntent = Intent.makeMainActivity(new ComponentName(context, HomeActivity.class));
		homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return homeIntent;
	}

	/*This will not send FG, BG packet to the server*/
	public static void relaunchApplicationWithPendingIntent(Context context)
	{
		Intent mStartActivity = new Intent(context, HomeActivity.class);
		mStartActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
	}

	public static Intent openInviteFriends(Activity context)
	{
		Intent in = new Intent(context, NUXInviteActivity.class);
		in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return in;
	}

	public static Intent openNuxFriendSelector(Activity context)
	{
		Intent in = new Intent(context, ComposeChatActivity.class);
		in.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		in.putExtra(HikeConstants.Extras.NUX_INCENTIVE_MODE, true);
		return in;
	}

	public static Intent openNuxCustomMessage(Activity context)
	{
		Intent in = new Intent(context, NuxSendCustomMessageActivity.class);
		return in;
	}

	public static Intent getWebViewActivityIntent(Context context, String url, String title)
	{
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, url);

		if (!TextUtils.isEmpty(title))
		{
			intent.putExtra(HikeConstants.Extras.TITLE, title);
		}
		intent.putExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, true);

		return intent;

	}


	public static Intent getNonMessagingBotIntent(String msisdn, Context context)
	{
		return(getNonMessagingBotIntent(msisdn,context,null));
	}
	
	public static Intent getNonMessagingBotIntent(String msisdn, Context context,String data)
	{
		if (BotUtils.isBot(msisdn))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
			
			if (botInfo.isNonMessagingBot())
			{
				NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());
				if (nonMessagingBotMetadata.isNativeMode())
				{
					Intent i = new Intent(context,CocosGamingActivity.class);
					i.putExtra(HikeConstants.MSISDN, msisdn);
					i.putExtra(HikeConstants.DATA,data);
					return i;
				}
				else
				{
					Intent intent = getWebViewActivityIntent(context, "", "");
					intent.putExtra(WebViewActivity.WEBVIEW_MODE, nonMessagingBotMetadata.isWebUrlMode() ? WebViewActivity.WEB_URL_BOT_MODE : WebViewActivity.MICRO_APP_MODE);
					intent.putExtra(HikeConstants.MSISDN, msisdn);
					return intent;
				}

			}
		}

		return new Intent();
	}

	public static Intent getForwardIntentForConvMessage(Context context, ConvMessage convMessage, String metadata, boolean includeAllUsers )
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, includeAllUsers);
		JSONArray multipleMsgArray = new JSONArray();
		JSONObject multiMsgFwdObject = new JSONObject();
		try
		{
			multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, convMessage.getMessageType());
			if (metadata != null)
			{
				multiMsgFwdObject.put(HikeConstants.METADATA, metadata);
			}
			multiMsgFwdObject.put(HikeConstants.PLATFORM_PACKET, convMessage.getPlatformData());
			multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, convMessage.getMessage());
			multiMsgFwdObject.put(HikePlatformConstants.NAMESPACE, convMessage.getNameSpace());
			multipleMsgArray.put(multiMsgFwdObject);
		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}
		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
		intent.putExtra(HikeConstants.Extras.PREV_MSISDN, convMessage.getMsisdn());

		return intent;
	}

    /*
     * This method is used for fetching an intent object meant to forward some text to hike chats.
     */
	public static Intent getForwardIntentForPlainText(Context context, String text ,String analyticsExtra)
	{
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);
		ConvMessage convMessage = Utils.makeConvMessage(myMsisdn, text, true);
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		intent.putExtra(HikeConstants.Extras.SHOW_TIMELINE, false);
		if (!TextUtils.isEmpty(analyticsExtra))
		{
			intent.putExtra(AnalyticsConstants.ANALYTICS_EXTRA, analyticsExtra);
		}
		JSONArray multipleMsgArray = new JSONArray();
		JSONObject multiMsgFwdObject = new JSONObject();
		try
		{
			multiMsgFwdObject.put(HikeConstants.MESSAGE, convMessage.getMessage());
			multipleMsgArray.put(multiMsgFwdObject);
		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}
		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());

		return intent;
	}
    /*
     * This method to used for fetching an intent object to share text to hike or other apps.
     */
	public static Intent getShareIntentForPlainText(String text){
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		intent.setType("text/plain");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}
	public static Intent getComposeChatIntent(Activity context)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.EDIT, true);
		return intent;
	}

	public static Intent getComposeChatIntentWithBotDiscovery(Activity context)
	{
		Intent intent = getComposeChatIntent(context);
		intent.putExtra(HikeConstants.Extras.IS_MICROAPP_SHOWCASE_INTENT, true);
		return intent;
	}

	public static Intent getFavouritesIntent(Activity context)
	{
		Intent intent = new Intent(context, PeopleActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getStickerShopIntent(Context context)
	{
		return new Intent(context, StickerShopActivity.class);
	}

	public static Intent getStickerSettingIntent(Activity context)
	{
		Intent intent = new Intent(context, StickerSettingsActivity.class);
		return intent;
	}

	public static Intent getProfileIntent(Activity context)
	{

		Intent intent = new Intent();
		intent.setClass(context, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	/**
	 * Retrieves an intent to make an outgoing voip call. 
	 * @param context
	 * @param msisdn
	 * @param source
	 * @return
	 */
	public static Intent getVoipCallIntent(Context context, String msisdn, VoIPUtils.CallSource source)
	{
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.OUTGOING_CALL);
		intent.putExtra(VoIPConstants.Extras.MSISDN, msisdn);
		intent.putExtra(VoIPConstants.Extras.CALL_SOURCE, source.ordinal());
		return intent;
	}

	/**
	 * Retrieves an intent to make an outgoing voip call to multiple recipients (conference) at once. 
	 * @param context
	 * @param msisdns
	 * @param groupChatMsisdn
	 * @param source
	 * @return intent if network check is passed. NULL otherwise. 
	 */
	public static Intent getVoipCallIntent(Context context, ArrayList<String> msisdns, String groupChatMsisdn, VoIPUtils.CallSource source)
	{
		// Check if we are on a fast enough network to make a conference call 
		if (!VoIPUtils.checkIfConferenceIsAllowed(HikeMessengerApp.getInstance(), msisdns.size()))
			return null;
		
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.OUTGOING_CALL);
		intent.putStringArrayListExtra(VoIPConstants.Extras.MSISDNS, msisdns);
		intent.putExtra(VoIPConstants.Extras.CALL_SOURCE, source.ordinal());
		if (!TextUtils.isEmpty(groupChatMsisdn))
			intent.putExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN, groupChatMsisdn);
		
		return intent;
	}

	public static Intent getVoipCallRateActivityIntent(Context context)
	{
		Intent intent = new Intent(context, CallRateActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	public static Intent getVoipIncomingCallIntent(Context context)
	{
		Intent intent = new Intent(context, VoIPActivity.class);
		intent.putExtra(VoIPConstants.Extras.INCOMING_CALL, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	public static Intent getBrowserIntent(String url)
	{
		return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	}

	public static Intent getPictureEditorActivityIntent(Context context, String imageFileName, boolean compressOutput, String destinationPath,boolean forProfileUpdate)
	{
		Intent i = new Intent(context, PictureEditer.class);
		
		i.setAction(HikeConstants.HikePhotos.PHOTOS_ACTION_CODE);

		if (imageFileName != null)
		{
			i.putExtra(HikeMessengerApp.FILE_PATH, imageFileName);
		}
		if (destinationPath != null)
		{
			i.putExtra(HikeConstants.HikePhotos.DESTINATION_FILENAME, destinationPath);
		}
		i.putExtra(HikeConstants.HikePhotos.EDITOR_ALLOW_COMPRESSION_KEY, compressOutput);
		
		i.putExtra(HikeConstants.HikePhotos.ONLY_PROFILE_UPDATE, forProfileUpdate);
		
		return i;
	}

	/**
	 * If the EXTRA_OUTPUT is not present, then a small sized image is returned as a Bitmap object in the extra field
	 *
	 * For images, save the file path as a preferences since in some devices the reference to the file becomes null.
	 * 
	 * @param boolean : whether the image should be saved to a specified path to ensure full quality. File : the specified file where the full quality image should be saved.
	 * 
	 * @return Camera Intent
	 */
	public static Intent getNativeCameraAppIntent(boolean getFullSizedCaptureResult, File destination)
	{
		Intent pickIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (getFullSizedCaptureResult)
		{
			pickIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destination));
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.FILE_PATH, destination.getAbsolutePath());
		}
		return pickIntent;
	}

	public static void startShareImageIntent(String mimeType, String imagePath, String text)
	{
		Intent s = new Intent(android.content.Intent.ACTION_SEND);
		s.setType(mimeType);
		s.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath));
		if (!TextUtils.isEmpty(text))
		{
			s.putExtra(Intent.EXTRA_TEXT, text);
		}
		s.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Logger.i("imageShare", "shared image with " + s.getExtras());
		HikeMessengerApp.getInstance().getApplicationContext().startActivity(s);

	}
	
	public static void startShareImageIntent(String mimeType, String imagePath)
	{
		startShareImageIntent(mimeType, imagePath, null);
	}
	
	public static Intent getCropActivityIntent(Context argActivity, String argPath, String argDestPath, CropCompression argCropCompression,boolean allowEditing,boolean fixAspectRatio)
	{
		return getCropActivityIntent(argActivity, argPath, argDestPath, argCropCompression, allowEditing, fixAspectRatio, CropImageView.DEFAULT_ASPECT_RATIO_X, CropImageView.DEFAULT_ASPECT_RATIO_Y);
	}

	public static Intent getCropActivityIntent(Context argActivity, String argPath, String argDestPath, CropCompression argCropCompression,boolean allowEditing,boolean fixAspectRatio, int aspectRatioX, int aspectRatioY)
	{
		Intent cropIntent = new Intent(argActivity, HikeCropActivity.class);
		cropIntent.putExtra(HikeCropActivity.CROPPED_IMAGE_PATH, argDestPath);
		cropIntent.putExtra(HikeCropActivity.SOURCE_IMAGE_PATH, argPath);
		cropIntent.putExtra(HikeCropActivity.ALLOW_EDITING,allowEditing);
		cropIntent.putExtra(HikeCropActivity.FIXED_ASPECT_RATIO,fixAspectRatio);
		cropIntent.putExtra(HikeCropActivity.ASPECT_RATIO_X, aspectRatioX);
		cropIntent.putExtra(HikeCropActivity.ASPECT_RATIO_Y, aspectRatioY);

		//https://code.google.com/p/android/issues/detail?id=6822
		Bundle cropCompBundle = new Bundle();
		cropCompBundle.putParcelable(HikeCropActivity.CROP_COMPRESSION, argCropCompression);

		cropIntent.putExtra(HikeCropActivity.CROP_COMPRESSION, cropCompBundle);
		return cropIntent;
	}


	public static Intent getApkSelectionActivityIntent(Context context) 
	{
		Intent intent = new Intent(context, ApkSelectionActivity.class);
		return intent;
	}
	
	public static Intent getHikeDirectHelpPageActivityIntent(Context context)
	{
		Intent intent = new Intent(context, HikeDirectHelpPageActivity.class);
		return intent;
	}
	
	public static Intent getInviteViaSMSIntent(Context context)
	{
		Intent intent = new Intent(context, HikeListActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CREDITS_SCREEN, true);
		return intent;
	}
		
	public static Intent getEmailOpenIntent(Context context)
	{
		return getEmailOpenIntent(context, null, null, null);
	}

	/**
	 * Call this function to send email
	 * @param context
	 * @param subject: the Subject of the email. If subject is empty, the fallback subject is "Feedback on hike for Android" in different languages.
	 * @param body:    the body of the email. User can change the body on his own as well.
	 * @param sendTo:  the sender email id. if email id is empty, the fallback email is sent to "support@hike.in"
	 * @return
	 */
	public static Intent getEmailOpenIntent(Context context, String subject, String body, String sendTo)
	{
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setData(Uri.parse("mailto:" + (TextUtils.isEmpty(sendTo) ? HikeConstants.MAIL : sendTo)));

		if (null == body)
		{
			body = "";
		}
		StringBuilder message = new StringBuilder(body);
		message.append("\n\n");

		try
		{
			message.append("hike Version:" + " " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName + "\n");
		}
		catch (PackageManager.NameNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		message.append("Device name:" + " " + Build.MANUFACTURER + " " + Build.MODEL + "\n");

		message.append("Android version:" + " " + Build.VERSION.RELEASE + "\n");

		String msisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, context.MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, "");
		message.append("Phone No:" + " " + msisdn);

		intent.putExtra(Intent.EXTRA_TEXT, message.toString());
		intent.putExtra(Intent.EXTRA_SUBJECT, TextUtils.isEmpty(subject) ? "Feedback on hike for Android" : subject);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent getPostStatusUpdateIntent(Activity argActivity, String text, String argImagePath, boolean compressImage)
	{
		Intent intent = new Intent(argActivity, StatusUpdate.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		if (!TextUtils.isEmpty(argImagePath))
		{
			intent.putExtra(StatusUpdate.STATUS_UPDATE_IMAGE_PATH, argImagePath);
			intent.putExtra(StatusUpdate.ENABLE_COMPRESSION,compressImage);
		}

		if (!TextUtils.isEmpty(text))
		{
			intent.putExtra(StatusUpdate.STATUS_UPDATE_TEXT, text);
		}

		return intent;
	}

	public static void openAccessibilitySettings(Activity activity)
	{
		Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		activity.startActivityForResult(intent, 0);
	}

	public static Intent getAddMembersToExistingGroupIntent(Context context, String mLocalMSISDN)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		return intent;
	}
	
	public static Intent getAddMembersToExistingBroadcastIntent(Context context, String mLocalMSISDN)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.BROADCAST_LIST, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST, mLocalMSISDN);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		return intent;
	}

	public static void openInviteWatsApp(Context context, String inviteText)
	{
		Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
		whatsappIntent.setType("text/plain");
		whatsappIntent.setPackage(HikeConstants.PACKAGE_WATSAPP);
		whatsappIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
		try
		{
			context.startActivity(whatsappIntent);
		}
		catch (android.content.ActivityNotFoundException ex)
		{
			Toast.makeText(context.getApplicationContext(), "Could not find WhatsApp in System", Toast.LENGTH_SHORT).show();
		}
	}
	
	public static void openIntentForGameActivity(Context context)
	{
		//TODO:Pass Intent of game activity and any extras.
//				Intent i = new Intent(context,SettingsActivity.class);
//		
//			context.startActivity(i);
	}
	
	public static Intent getIntentForBots(BotInfo mBotInfo, Context context)
	{
		if (mBotInfo.isNonMessagingBot())
		{
			return IntentFactory.getNonMessagingBotIntent(mBotInfo.getMsisdn(), context);
		}
		else
		{
			return IntentFactory.createChatThreadIntentFromMsisdn(context, mBotInfo.getMsisdn(), false, false, ChatThreadActivity.ChatThreadOpenSources.MICRO_APP);
		}
	}
	
	public static Intent getIntentForAnyChatThread(Context context, String msisdn, boolean isBot, int source)
	{
		if (isBot)
		{
			return IntentFactory.getIntentForBots(context, msisdn);
		}
		else
		{
			return IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, false, false, source);
		}

	}

	public static Intent getIntentForBots(Context mContext, String msisdn)
	{
		BotInfo mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

		if (mBotInfo == null)
		{
			mBotInfo = HikeConversationsDatabase.getInstance().getBotInfoForMsisdn(msisdn);
		}

		Intent intent = null;

		if (mBotInfo != null && mBotInfo.isNonMessagingBot())
		{
			intent = IntentFactory.getNonMessagingBotIntent(msisdn, mContext);
		}
		return intent;
	}

	public static void openPackPreviewIntent(Context context, String catId, int position, StickerConstants.PackPreviewClickSource previewClickSource, String previewClickSearchKey)
	{
		Intent intent = new Intent(context, PackPreviewActivity.class);
		intent.putExtra(HikeConstants.STICKER_CATEGORY_ID, catId);
		intent.putExtra(HikeConstants.POSITION, position);
		context.startActivity(intent);
		StickerManager.getInstance().sendPackPreviewOpenAnalytics(catId, position, previewClickSource.getValue(), previewClickSearchKey);
	}

	public static String getTextFromActionSendIntent(Intent presentIntent)
	{
		String msg = null;

		if(presentIntent == null)
		{
			return msg;
		}

		if (presentIntent.hasExtra(Intent.EXTRA_TEXT) || presentIntent.hasExtra(HikeConstants.Extras.MSG))
		{
			msg = presentIntent.getStringExtra(presentIntent.hasExtra(HikeConstants.Extras.MSG) ? HikeConstants.Extras.MSG : Intent.EXTRA_TEXT);
			if (msg == null)
			{
				Bundle extraText = presentIntent.getExtras();
				if (extraText.get(Intent.EXTRA_TEXT) != null)
				{
					msg = extraText.get(Intent.EXTRA_TEXT).toString();
				}
			}

			if (msg != null && presentIntent.hasExtra(Intent.EXTRA_SUBJECT))
			{
				String subject = presentIntent.getStringExtra(Intent.EXTRA_SUBJECT).toString();
				if (!TextUtils.isEmpty(subject))
				{
					msg = subject + "\n" + msg;
				}
			}

		}
		return msg;
	}

    /**
     *
     * @return returns launch intent with persistant alarm flags
     */
    public static Intent getPersistantAlarmIntent()
    {
        Intent intent = new Intent();
        intent.putExtra(HikeAlarmManager.INTENT_EXTRA_DELETE_FROM_DATABASE, false);
        return intent;
    }

	public static void launchPlayStore(String packageName, Activity context)
	{
		Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
		marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		try
		{
			context.startActivity(marketIntent);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.e(HomeActivity.class.getSimpleName(), "Unable to open market");
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
		}
	}

	public static void startUpgradeIntent(Context context)
	{
		// turn off future push notifications as soon as the app has
		// started.
		// this has to be turned on whenever the upgrade finishes.
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPGRADING, true);
		SharedPreferences.Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, true);
		editor.commit();

		Intent msgIntent = new Intent(context, UpgradeIntentService.class);
		context.startService(msgIntent);
	}

	/**
	 * Method creates an intent with provided action and extras to launch {@link StorageSpecIntentService}
	 * @param action
	 * @param dirPath
	 * @param shouldMapContainedFiles
	 */
	public static void startStorageSpecIntent(String action, String dirPath, boolean shouldMapContainedFiles)
	{
		Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
		Intent storageSpecIntent = new Intent(hikeAppContext, StorageSpecIntentService.class);
		storageSpecIntent.setAction(action);
		storageSpecIntent.putExtra(HikeConstants.SPACE_MANAGER.MAP_DIRECTORY, shouldMapContainedFiles);
		if(!TextUtils.isEmpty(dirPath))
		{
			storageSpecIntent.putExtra(HikeConstants.SPACE_MANAGER.DIRECTORY_PATH, dirPath);
		}
		hikeAppContext.startService(storageSpecIntent);
	}

}
