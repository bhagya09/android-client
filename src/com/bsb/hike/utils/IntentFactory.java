package com.bsb.hike.utils;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.chatHead.StickerShareSettings;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.ConnectedAppsActivity;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.ui.FileSelectActivity;
import com.bsb.hike.ui.FtueBroadcast;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.NUXInviteActivity;
import com.bsb.hike.ui.NuxSendCustomMessageActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.PictureEditer;
import com.bsb.hike.ui.PinHistoryActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.ui.ShareLocation;
import com.bsb.hike.ui.SignupActivity;
import com.bsb.hike.ui.StickerSettingsActivity;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.ui.TimelineActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;
import com.bsb.hike.voip.view.CallRateActivity;
import com.bsb.hike.voip.view.VoIPActivity;

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

	public static Intent shareIntent(String mimeType, String imagePath, String text, int type, String pkgName)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(mimeType);
		if (!TextUtils.isEmpty(text))
		{
			intent.putExtra(Intent.EXTRA_TEXT, text);
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
		context.startActivity(new Intent(context, CreditsActivity.class));
	}

	public static void openSettingAccount(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.account_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.account);
		context.startActivity(intent);
	}

	
	public static void openSettingStickerOnOtherApp(Context context)
	{
		Intent intent = new Intent(context, StickerShareSettings.class);
		context.startActivity(intent);
	}
	
	public static void openSettingHelp(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.help_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.help);
		context.startActivity(intent);
	}

	public static void openSettingChat(Context context)
	{
		Intent intent = new Intent(context, HikePreferences.class);
		intent.putExtra(HikeConstants.Extras.PREF, R.xml.chat_settings_preferences);
		intent.putExtra(HikeConstants.Extras.TITLE, R.string.settings_chat);
		context.startActivity(intent);
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
		context.startActivity(new Intent(context, TimelineActivity.class));
	}
	
	public static void openHikeExtras(Context context)
	{
		context.startActivity(getGamingIntent(context));
	}

	public static Intent getGamingIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String hikeExtrasUrl = prefs.getString(HikeConstants.HIKE_EXTRAS_URL, AccountUtils.gamesUrl);

		if (!TextUtils.isEmpty(hikeExtrasUrl))
		{
			Uri gamesUri = Utils.getFormedUri(context, hikeExtrasUrl, prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
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
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		String rewards_url = prefs.getString(HikeConstants.REWARDS_URL, AccountUtils.rewardsUrl);

		if (!TextUtils.isEmpty(rewards_url))
		{
			Uri rewardsUri = Utils.getFormedUri(context, rewards_url, prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
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
	
	public static Intent getStickerShareWebViewActivityIntent(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(context.getApplicationContext(), WebViewActivity.class);
		/*
		 * New task flag is needed as we are opening our activity in another task outside hike existing task and clear task to remove existing activities of hike task so that user
		 * can return from where he came to this task
		 */
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
				HttpRequestConstants.getMorestickersUrl() + HikeConstants.ANDROID + "/" + prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, context.getString(R.string.more_stickers));

		return intent;
	}

	public static Intent getStickerShareSettingsIntent(Context context)
	{
		Intent intent = new Intent(context, StickerShareSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return intent;
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

	public static void createBroadcastFtue(Context appContext)
	{
		Intent intent = new Intent(appContext.getApplicationContext(), FtueBroadcast.class);
		intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		appContext.startActivity(intent);
	}

	public static void createBroadcastDefault(Context appContext)
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
		newMediaFileIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (0.9 * HikeConstants.MAX_FILE_SIZE));
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

	public static Intent createChatThreadIntentFromMsisdn(Context context, String msisdnOrGroupId, boolean openKeyBoard)
	{
		Intent intent = new Intent();

		intent.setClass(context, ChatThreadActivity.class);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdnOrGroupId);
		intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, ChatThreadUtils.getChatThreadType(msisdnOrGroupId));
		intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, openKeyBoard);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static Intent createChatThreadIntentFromContactInfo(Context context, ContactInfo contactInfo, boolean openKeyBoard)
	{
		// If the contact info was made using a group conversation, then the
		// Group ID is in the contact ID
		boolean isGroupConv = OneToNConversationUtils.isOneToNConversation(contactInfo.getMsisdn());
		return createChatThreadIntentFromMsisdn(context, isGroupConv ? contactInfo.getId() : contactInfo.getMsisdn(), openKeyBoard);
	}

	public static Intent createChatThreadIntentFromConversation(Context context, ConvInfo conversation)
	{
		Intent intent = new Intent(context, ChatThreadActivity.class);
		if (conversation.getConversationName() != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, conversation.getConversationName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		String whichChatThread = ChatThreadUtils.getChatThreadType(conversation.getMsisdn());
		intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, whichChatThread);
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
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
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

	public static Intent getHikeGalleryPickerIntent(Context context, int flags,String croppedOutputDestination)
	{
		
		boolean allowMultiSelect = (flags & GalleryActivity.GALLERY_ALLOW_MULTISELECT )!=0;
		boolean categorizeByFolders = (flags & GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS)!=0;
		boolean enableCameraPick = (flags & GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM)!=0;
		boolean editSelectedImage = (flags & GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE)!=0;
		boolean compressEdited = (flags & GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE)!=0;
		boolean forProfileUpdate = (flags & GalleryActivity.GALLERY_FOR_PROFILE_PIC_UPDATE)!=0;
		
		Intent intent = new Intent(context, GalleryActivity.class);
		Bundle b = new Bundle();
		b.putBoolean(GalleryActivity.DISABLE_MULTI_SELECT_KEY, !allowMultiSelect);
		b.putBoolean(GalleryActivity.FOLDERS_REQUIRED_KEY, categorizeByFolders);
		b.putBoolean(GalleryActivity.ENABLE_CAMERA_PICK, enableCameraPick);
		
		ArrayList<Intent> destIntents = new ArrayList<Intent>();
		
		if(editSelectedImage)
		{
			destIntents.add(IntentFactory.getPictureEditorActivityIntent(context, null, compressEdited, null, forProfileUpdate));
		}
		
		if(croppedOutputDestination != null)
		{
			destIntents.add(IntentFactory.getCropActivityIntent(context, null, croppedOutputDestination, true, 100, false));
		}
		
		if(destIntents.size()>0)
		{
			b.putParcelableArrayList(HikeBaseActivity.DESTINATION_INTENT, destIntents);
		}
		
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

	public static void openHomeActivity(Context context, boolean clearTop)
	{
		Intent in = new Intent(context, HomeActivity.class);
		if (clearTop)
		{
			in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		context.startActivity(in);
	}
	
	public static void openHomeActivityInOtherTask(Context context, boolean flag)
	{
		Intent in = new Intent(context, HomeActivity.class);
		if (flag)
		{
			in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		}		
		context.startActivity(in);
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
	
	public static Intent getNonMessagingBotIntent(String msisdn, String url, String title, Context context)
	{
		Intent intent = getWebViewActivityIntent(context, url, title);
		intent.putExtra(WebViewActivity.WEBVIEW_MODE, WebViewActivity.MICRO_APP_MODE);
		intent.putExtra(HikeConstants.MSISDN, msisdn);
		
		return intent;
	}

	public static Intent getForwardIntentForConvMessage(Context context, ConvMessage convMessage, String metadata)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
		JSONArray multipleMsgArray = new JSONArray();
		JSONObject multiMsgFwdObject = new JSONObject();
		try
		{
			multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, convMessage.getMessageType());
			if (metadata != null)
			{
				multiMsgFwdObject.put(HikeConstants.METADATA, metadata);
			}
			multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, convMessage.getMessage());
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

	public static Intent getComposeChatIntent(Activity context)
	{
		Intent intent = new Intent(context, ComposeChatActivity.class);
		intent.putExtra(HikeConstants.Extras.EDIT, true);
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
		Intent intent = new Intent(context, StickerShopActivity.class);
		
		return intent;
	}
	public static Intent getStickerShopIntent(Context context, boolean flags)
	{
		Intent intent = new Intent(context, StickerShopActivity.class);
		
		if(flags)
		{  /*New task flag is needed as we are opening our activity in another task outside hike existing task and clear task 
		 to remove existing activities of hike task so that user can return from where he came to this task*/  
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}
		return intent;
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

	public static Intent getVoipCallIntent(Context context, String msisdn, VoIPUtils.CallSource source)
	{
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.OUTGOING_CALL);
		intent.putExtra(VoIPConstants.Extras.MSISDN, msisdn);
		intent.putExtra(VoIPConstants.Extras.CALL_SOURCE, source.ordinal());
		return intent;
	}

	public static Intent getVoipCallIntent(Context context, ArrayList<String> msisdns, VoIPUtils.CallSource source)
	{
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.OUTGOING_CALL);
		intent.putStringArrayListExtra(VoIPConstants.Extras.MSISDNS, msisdns);
		intent.putExtra(VoIPConstants.Extras.CALL_SOURCE, source.ordinal());
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
	
	public static Intent getCropActivityIntent(Context context, String path, String destPath, boolean preventScaling, int quality,boolean circleHighlight)
	{
		/* Crop the image */
		Intent intent = new Intent(context, CropImage.class);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, destPath);
		intent.putExtra(HikeConstants.Extras.IMAGE_PATH, path);
		intent.putExtra(HikeConstants.Extras.CIRCLE_HIGHLIGHT, circleHighlight);
		intent.putExtra(HikeConstants.Extras.SCALE, false);
		intent.putExtra(HikeConstants.Extras.RETURN_CROP_RESULT_TO_FILE, preventScaling);
		intent.putExtra(HikeConstants.Extras.ASPECT_X, 1);
		intent.putExtra(HikeConstants.Extras.ASPECT_Y, 1);
		intent.putExtra(HikeConstants.Extras.JPEG_COMPRESSION_QUALITY, quality);
		return intent;
	}

	
}
