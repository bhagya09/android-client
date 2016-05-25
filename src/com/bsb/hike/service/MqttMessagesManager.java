package com.bsb.hike.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.CustomKeyboard;
import com.bsb.hike.bots.CustomKeyboardManager;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.chatthemes.HikeChatThemeConstants;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.dbcommand.GetSqliteVersionCommand;
import com.bsb.hike.filetransfer.FTApkManager;
import com.bsb.hike.filetransfer.FTUtils;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MessagePrivateData;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.WhitelistDomain;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ContactUtils;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.signupmgr.PostAddressBookTask;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.modules.signupmgr.PostAddressBookTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchUtility;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformContentListener;
import com.bsb.hike.platform.PlatformContentRequest;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformZipDownloader;
import com.bsb.hike.productpopup.AtomicTipManager;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.spaceManager.StorageSpecUtils;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.triggers.InterceptUtils;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.userlogs.UserLogInfo;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ClearGroupTypingNotification;
import com.bsb.hike.utils.ClearTypingNotification;
import com.bsb.hike.utils.FestivePopup;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPUtils;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hike.abtest.ABTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Rishabh This class is used for saving all the mqtt messages in the db based on their types. Its also used to publish these events for the UI to make the changes,
 *         wherever applicable. This class should be a singleton, since only one instance should be used managing these messages
 */
public class MqttMessagesManager
{

	public static final String UJFile = "uj_file";

	private final HikeConversationsDatabase convDb;

	private final SharedPreferences settings;

	private final SharedPreferences appPrefs;

	private final Context context;

	private final HikePubSub pubSub;

	private final Map<String, TypingNotification> typingNotificationMap;

	private final Handler clearTypingNotificationHandler;

	private static volatile MqttMessagesManager instance;

	private final String userMsisdn;

	private boolean isBulkMessage = false;

	private LinkedList<ConvMessage> messageList;

	private Map<String, LinkedList<ConvMessage>> messageListMap;

	private Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap;

	private static int lastNotifPacket;

	private static final String DP_DOWNLOAD_TAG = "dp_download";

    private String TAG = "MqttMessagesManager";

	private MqttMessagesManager(Context context)
	{
		Logger.d(getClass().getSimpleName(), "initialising MqttMessagesManager");
		this.convDb = HikeConversationsDatabase.getInstance();
		this.settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
		this.pubSub = HikeMessengerApp.getPubSub();
		this.typingNotificationMap = HikeMessengerApp.getTypingNotificationSet();
		this.clearTypingNotificationHandler = new Handler(Looper.getMainLooper());
		this.appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.userMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
	}

	public static MqttMessagesManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (MqttMessagesManager.class)
			{
				if (instance == null)
				{
					instance = new MqttMessagesManager(context);
				}
			}
		}
		return instance;
	}

	public void close()
	{
		instance = null;
	}

	private void saveIcon(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);
		/*
		 * We don't consider this packet if the msisdn is the user's msisdn or a group conversation.
		 */
		if (OneToNConversationUtils.isGroupConversation(msisdn) || userMsisdn.equals(msisdn))
		{
			return;
		}
		String iconBase64 = jsonObj.getString(HikeConstants.DATA);
		// ContactManager.getInstance().setIcon(msisdn, Base64.decode(iconBase64, Base64.DEFAULT), false);
		HikeImageWorker.doContactManagerIconChange(msisdn, Base64.decode(iconBase64, Base64.DEFAULT), false);

		HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
		// IconCacheManager.getInstance().clearIconForMSISDN(msisdn);

		/*
		 * Only auto download if the ic packet is not generated due to signup.
		 */
		if (!HikeConstants.SIGNUP_IC.equals(jsonObj.optString(HikeConstants.SUB_TYPE)))
		{
			FavoriteType favType = ContactManager.getInstance().getFriendshipStatus(msisdn);
			if (favType == FavoriteType.FRIEND || favType == FavoriteType.REQUEST_SENT || favType == FavoriteType.REQUEST_SENT_REJECTED)
			{
				Logger.d(DP_DOWNLOAD_TAG, "Received IC Packet, going to download");
				autoDownloadGroupImage(msisdn);
			}
		}
	}

	private void saveDisplayPic(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.getString(HikeConstants.TO);
		String iconBase64 = jsonObj.getString(HikeConstants.DATA);

		// String fromMSISDN = jsonObj.getString(HikeConstants.FROM);

		// boolean groupDpSetByMe = fromMSISDN.equals(HikeMessengerApp.getInstance().getMsisdn());

		// Removing this check for now need a server check to cover all scenarios
		// processing the MQTT request for group DP change only when DP updated by contact or no DP image present for this group(Re-Signup case)
		// if(!groupDpSetByMe || !(new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT+File.separator+Utils.getProfileImageFileName(groupId)).exists()))
		// {
		String newIconIdentifier = null;
		ContactManager conMgr = ContactManager.getInstance();
		if (iconBase64.length() < 6)
		{
			newIconIdentifier = iconBase64;
		}
		else
		{
			newIconIdentifier = iconBase64.substring(0, 5) + iconBase64.substring(iconBase64.length() - 6);
		}

		String oldIconIdentifier = conMgr.getIconIdentifierString(groupId);

		/*
		 * Same Icon
		 */
		if (newIconIdentifier.equals(oldIconIdentifier))
		{
			return;
		}

		HikeImageWorker.doContactManagerIconChange(groupId, Base64.decode(iconBase64, Base64.DEFAULT), false);
		// conMgr.setIcon(groupId, Base64.decode(iconBase64, Base64.DEFAULT), false);

		HikeMessengerApp.getLruCache().clearIconForMSISDN(groupId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, groupId);

		// IconCacheManager.getInstance().clearIconForMSISDN(groupId);
		autoDownloadGroupImage(groupId);
		// }

		boolean saveStatusMsg = true;
		if (jsonObj.has(HikeConstants.METADATA))
		{
			JSONObject mdata = jsonObj.getJSONObject(HikeConstants.METADATA);
			if (mdata.has(HikeConstants.MqttMessageTypes.SYNC))
			{
				int syncState = mdata.getInt(HikeConstants.MqttMessageTypes.SYNC);
				if (syncState == 1)
				{
					saveStatusMsg = false;
				}
			}
		}
		if (saveStatusMsg)
		{
			saveStatusMsg(jsonObj, groupId);
		}

	}

	private void saveSMSCredits(JSONObject jsonObj) throws JSONException
	{
		Integer credits = jsonObj.optInt(HikeConstants.DATA);
		if (settings.getInt(HikeMessengerApp.SMS_SETTING, 0) == 0)
		{
			if (credits > 0)
			{
				convDb.setOverlay(false, null);
			}
		}
		Editor mEditor = settings.edit();
		mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
		mEditor.commit();
		this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, credits);
	}

	private JSONObject buildUserJoinParams(JSONObject jsonObj, String userType) throws JSONException
	{

		HikeSharedPreferenceUtil ujPrefs = HikeSharedPreferenceUtil.getInstance(UJFile);
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		boolean isNewUser = userType.equals(HikeConstants.NEW_USER);

		if (!data.has(HikeConstants.UserJoinMsg.NOTIF_TEXT))
		{
			String notificationText = ujPrefs.getData(userType + HikeConstants.UserJoinMsg.NOTIF_TEXT,
					context.getString(isNewUser ? R.string.joined_hike : R.string.user_back_on_hike));
			data.put(HikeConstants.UserJoinMsg.NOTIF_TEXT, notificationText);
		}
		if (!data.has(HikeConstants.UserJoinMsg.NOTIF_TITLE))
		{
			String notificationTitle = ujPrefs.getData(userType + HikeConstants.UserJoinMsg.NOTIF_TITLE, context.getString(R.string.last_seen_more_ct));
			data.put(HikeConstants.UserJoinMsg.NOTIF_TITLE, notificationTitle);
		}
		if (!data.has(HikeConstants.UserJoinMsg.PUSH_SETTING))
		{
			int pushType = ujPrefs.getData(userType + HikeConstants.UserJoinMsg.PUSH_SETTING, HikeConstants.PushType.silent);
			data.put(HikeConstants.UserJoinMsg.PUSH_SETTING, pushType);
		}
		if (!data.has(HikeConstants.UserJoinMsg.PERSIST_CHAT))
		{
			boolean persistChat = ujPrefs.getData(userType + HikeConstants.UserJoinMsg.PERSIST_CHAT, HikeConstants.UserJoinMsg.defaultPersistChat);
			data.put(HikeConstants.UserJoinMsg.PERSIST_CHAT, persistChat);
		}

		return jsonObj;
	}

	private void saveUserJoinedOrLeft(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		String msisdn = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
		boolean joined = HikeConstants.MqttMessageTypes.USER_JOINED.equals(type);
		String userType = jsonObj.optString(HikeConstants.SUB_TYPE, HikeConstants.NEW_USER);
		long joinTime = 0;

		// by default the chat shall not persist

		SharedPreferences settings = context.getSharedPreferences(UJFile, Context.MODE_PRIVATE);
		if (joined)
		{
			jsonObj = buildUserJoinParams(jsonObj, userType);

			joinTime = jsonObj.optLong(HikeConstants.TIMESTAMP);
			long ts = settings.getLong(msisdn, -1);
			// -1 shows last uj was for some other msisdn or this user has left or pref file do not exist
			if (ts != -1 && ts == joinTime)
				// this shows UJ is duplicate so ignore
				return;
			else
				// last join time was different from latest time
				settings.edit().putLong(msisdn, joinTime).commit();
		}
		else
		{
			// if user left Hike simply remove the value from pref
			settings.edit().remove(msisdn).commit();
		}

		ContactManager.getInstance().updateHikeStatus(this.context, msisdn, joined);
		this.convDb.updateOnHikeStatus(msisdn, joined);

		if (joined)
		{
			if (joinTime > 0)
			{
				joinTime = Utils.applyServerTimeOffset(context, joinTime);
				ContactManager.getInstance().setHikeJoinTime(msisdn, joinTime);
			}

			ContactInfo contact = ContactManager.getInstance().getContact(msisdn, true, false);
			boolean showRecentlyJoined = contact.getHikeJoinTime() > 0 && !contact.isUnknownContact();

			if (appPrefs.getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true) && !ContactManager.getInstance().isBlocked(msisdn)
					&& jsonObj.getJSONObject(HikeConstants.DATA).optBoolean(HikeConstants.SHOW_NOTIFICATION, true))
			{
				if (jsonObj.getJSONObject(HikeConstants.DATA).optBoolean(HikeConstants.UserJoinMsg.PERSIST_CHAT, HikeConstants.UserJoinMsg.defaultPersistChat))
				{
					if (showRecentlyJoined)
					{
						this.settings.edit().putBoolean(HikeConstants.SHOW_RECENTLY_JOINED, true).commit();
					}
					saveStatusMsg(jsonObj, msisdn);
				}
				else
				{
					if (showRecentlyJoined)
					{
						this.settings.edit().putBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, true).commit();
					}
					ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);
					if (convMessage != null)
					{
						this.pubSub.publish(HikePubSub.USER_JOINED_NOTIFICATION, convMessage);
					}
				}
			}
		}
		else
		{
			HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			// IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
		}

		/*
		 * Change the friend type since the user has now left hike and delete this contact's status messages.
		 */
		if (!joined)
		{
			convDb.updateAdminStatus(msisdn);
			convDb.deleteStatusMessagesForMsisdn(msisdn);
			ContactManager.getInstance().updateAdminState(msisdn);
			removeOrPostponeFriendType(msisdn);
		}

		this.pubSub.publish(joined ? HikePubSub.USER_JOINED : HikePubSub.USER_LEFT, msisdn);
	}

	private void saveInviteInfo(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		int invited = data.optInt(HikeConstants.ALL_INVITEE);
		int invited_joined = data.optInt(HikeConstants.ALL_INVITEE_JOINED);
		String totalCreditsPerMonth = data.optString(HikeConstants.TOTAL_CREDITS_PER_MONTH);
		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.INVITED, invited);
		editor.putInt(HikeMessengerApp.INVITED_JOINED, invited_joined);
		if (!TextUtils.isEmpty(totalCreditsPerMonth) && Integer.parseInt(totalCreditsPerMonth) > 0)
		{
			editor.putString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, totalCreditsPerMonth);
		}
		editor.commit();
		this.pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
	}

	private void saveGCJoin(JSONObject jsonObj) throws JSONException
	{
		if (jsonObj.getJSONArray(HikeConstants.DATA).length() == 0)
		{
			return;
		}
		OneToNConversation oneToNConversation;

		oneToNConversation = OneToNConversation.createOneToNConversationFromJSON(jsonObj);

		boolean groupRevived = false;
		if (!ContactManager.getInstance().isGroupAlive(oneToNConversation.getMsisdn()))
		{

			Logger.d(getClass().getSimpleName(), "Group is not alive");
			int updated = this.convDb.toggleGroupDeadOrAlive(oneToNConversation.getMsisdn(), true);
			Logger.d(getClass().getSimpleName(), "Group revived? " + updated);
			groupRevived = updated > 0;

			if (groupRevived)
			{
				if (oneToNConversation instanceof BroadcastConversation)
				{
					jsonObj.put(HikeConstants.NEW_BROADCAST, true);
				}
				else if (oneToNConversation instanceof GroupConversation)
				{
					jsonObj.put(HikeConstants.NEW_GROUP, true);
				}
				pubSub.publish(HikePubSub.CONVERSATION_REVIVED, oneToNConversation.getMsisdn());
			}

		}
		int gcjAdd = this.convDb.addRemoveGroupParticipants(oneToNConversation.getMsisdn(), oneToNConversation.getConversationParticipantList(), groupRevived);
		JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);
		if (!groupRevived && gcjAdd != HikeConstants.NEW_PARTICIPANT)
		{
			if (metadata != null)
			{

				this.convDb.setGroupCreationTime(oneToNConversation.getMsisdn(), oneToNConversation.getCreationDate());
				changeGroupSettings(oneToNConversation, metadata);
			}
			Logger.d(getClass().getSimpleName(), "GCJ Message was already received");
			return;
		}
		Logger.d(getClass().getSimpleName(), "GCJ Message is new");

		/*
		 * 
		 * if (!groupRevived && !ContactManager.getInstance().isGroupExist(groupConversation.getMsisdn())) { Logger.d(getClass().getSimpleName(),
		 * "The group conversation does not exists"); if (metadata != null) {
		 * 
		 * String groupName = metadata.optString(HikeConstants.NAME); groupConversation = (GroupConversation) this.convDb.addConversation(groupConversation.getMsisdn(), false,
		 * groupName, groupConversation.getGroupOwner()); groupConversation.setContactName(groupName);
		 * ContactManager.getInstance().insertGroup(groupConversation.getMsisdn(),groupName); }
		 */
		// Adding a key to the json signify that this was the GCJ
		// received for group creation
		// jsonObj.put(HikeConstants.NEW_GROUP, true);
		if (!groupRevived && !ContactManager.getInstance().isGroupExist(oneToNConversation.getMsisdn()))
		{
			Logger.d(getClass().getSimpleName(), "The group conversation does not exists");

			String groupName = "";
			if (metadata != null)
			{
				// Earlier there were 2 queries, one to make the group conv and second to set the name. I have combined the both
				groupName = metadata.optString(HikeConstants.NAME);
			}

			oneToNConversation = (OneToNConversation) this.convDb.addConversation(oneToNConversation.getMsisdn(), false, groupName, oneToNConversation.getConversationOwner(),
					null, oneToNConversation.getCreationDate(), oneToNConversation.getConversationCreator());
			ContactManager.getInstance().insertGroup(oneToNConversation.getMsisdn(), groupName);

			// Adding a key to the json signify that this was the GCJ
			// received for group creation
			if (oneToNConversation instanceof BroadcastConversation)
			{
				jsonObj.put(HikeConstants.NEW_BROADCAST, true);
			}
			else if (oneToNConversation instanceof GroupConversation)
			{
				jsonObj.put(HikeConstants.NEW_GROUP, true);
			}
		}

		if (metadata != null)
		{
			JSONObject chatBgJson = metadata.optJSONObject(HikeConstants.CHAT_BACKGROUND);
			if (chatBgJson != null)
			{
				String bgId = chatBgJson.optString(HikeConstants.BG_ID);
				String groupId = oneToNConversation.getMsisdn();
				try
				{
					/*
					 * We don't support custom themes yet.
					 */
					if (chatBgJson.optBoolean(HikeConstants.CUSTOM))
					{
						//throw new IllegalArgumentException();
						//putting request to download the asset ids for the theme
						ChatThemeManager.getInstance().downloadThemeAssetsMetadata(bgId, true);
					}

					String chatThemeId = bgId;
					convDb.setChatBackground(groupId, bgId, 0);

				}
				catch (IllegalArgumentException e)
				{
					/*
					 * This exception is thrown for unknown themes. Do nothing
					 */
				}

			}
			changeGroupSettings(oneToNConversation, metadata);
		}

		saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
	}

	private void changeGroupSettings(OneToNConversation oneToNConversation, JSONObject metadata)
	{
		int role = 0;
		if (metadata.has(HikeConstants.ROLE))
		{
			role = metadata.optInt(HikeConstants.ROLE);
		}
		int setting = -1;
		if (metadata.has(HikeConstants.GROUP_SETTING))
		{
			setting = metadata.optInt(HikeConstants.GROUP_SETTING);
		}
		if (setting > -1 || role > -1)
		{
			this.convDb.changeGroupSettings(oneToNConversation.getMsisdn(), setting, role, new ContentValues());
			Logger.d(getClass().getSimpleName(), "GCJ Message - GS setting change");
		}
	}

	private void saveGCLeave(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		String msisdn = jsonObj.optString(HikeConstants.DATA);
		if (this.convDb.setParticipantLeft(groupId, msisdn) > 0)
		{
			ContactManager.getInstance().removeGroupParticipant(groupId, msisdn);
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
	}

	private void saveAdminUpdate(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		String msisdn = data.optString(HikeConstants.ADMIN_MSISDN);
		if (msisdn.equalsIgnoreCase(userMsisdn))
		{
			int setting = data.optInt(HikeConstants.SETTING);
			this.convDb.changeGroupSettings(groupId, setting, 1, new ContentValues());
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
		else
		{

			if (this.convDb.setParticipantAdmin(groupId, msisdn) > 0)
			{
				ContactManager.getInstance().setParticipantAdmin(groupId, msisdn);
				saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
			}
			changeGroupSettings(jsonObj, false);
		}
	}

	private void changeGroupSettings(JSONObject jsonObj, boolean directSettingChange) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		int setting = data.optInt(HikeConstants.SETTING);
		this.convDb.changeGroupSettings(groupId, setting, -1, new ContentValues());
		if (directSettingChange)
		{
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
	}

	private void saveGCName(JSONObject jsonObj) throws JSONException
	{
		String groupname = jsonObj.optString(HikeConstants.DATA);
		String groupId = jsonObj.optString(HikeConstants.TO);

		if (this.convDb.setGroupName(groupId, groupname) > 0)
		{
			ContactManager.getInstance().setGroupName(groupId, groupname);
			this.pubSub.publish(HikePubSub.ONETONCONV_NAME_CHANGED, groupId);

			boolean showPush = true;
			JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);
			if (metadata != null)
			{
				showPush = metadata.optBoolean(HikeConstants.PUSH, true);
			}
			if (showPush)
			{
				saveStatusMsg(jsonObj, groupId);
			}
		}
	}

	private void saveGCEnd(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		if (this.convDb.toggleGroupDeadOrAlive(groupId, false) > 0)
		{
			this.convDb.changeGroupSettings(groupId, 0, 0, new ContentValues());
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
	}

	public void saveMessage(JSONObject jsonObj) throws JSONException
	{
		final ConvMessage convMessage = messagePreProcess(jsonObj);

		// Logs for Msg Reliability
		MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.RECEIVER_MQTT_RECVS_SENT_MSG);
		if (ContactManager.getInstance().isBlocked(convMessage.getMsisdn()))
		{
			// discard message since the conversation is blocked
			return;
		}

        if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT)
		{
			downloadZipForPlatformMessage(convMessage);
		}
		else
		{
			saveMessage(convMessage);
		}

 	}

	private void saveMessage(ConvMessage convMessage)
	{
		/*
		 * adding message in db if not duplicate. In case of duplicate message we don't do further processing and return
		 */
		if (!convDb.addConversationMessages(convMessage, true))
		{
			return;
		}

		// Logs for Msg Reliability
		MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.RECIEVR_RECV_MSG);

		/*
		 * Return if there is no conversation for this msisdn.
		 */
		if (!ContactManager.getInstance().isConvExists(convMessage.getMsisdn()))
		{
			Logger.d(getClass().getSimpleName(), "conversation does not exist");
			return;
		}

		// We have to do publish this here since we are adding the message
		// to the db here, and the id is set after inserting into the db.
		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

		messageProcessVibrate(convMessage);
		messageProcessFT(convMessage);
		messageProcessSticker(convMessage);
		messageProcessStickerRecommendation(convMessage);

		if (convMessage.isOneToNChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			ConvMessage convMessageNew = convDb.showParticipantStatusMessage(convMessage.getMsisdn());
			if (convMessageNew != null)
			{
				if (convDb.addConversationMessages(convMessageNew, true))
				{
					this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessageNew);
				}

			}
		}

		removeTypingNotification(convMessage.getMsisdn(), convMessage.getGroupParticipantMsisdn());
	}

	private void downloadZipForPlatformMessage(final ConvMessage convMessage)
	{

		PlatformContentRequest rqst = PlatformContentRequest.make(
				PlatformContentModel.make(convMessage.webMetadata.JSONtoString()), new PlatformContentListener<PlatformContentModel>()
				{

					@Override
					public void onComplete(PlatformContentModel content)
					{
						saveMessage(convMessage);
					}

					@Override
					public void onEventOccured(int uniqueId,PlatformContent.EventCode event)
					{
						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							//do nothing
							return;
						}
						else if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
						{
							Logger.d(HikePlatformConstants.TAG, "microapp already exists");
						}
						else
						{
							saveMessage(convMessage);
							HikeAnalyticsEvent.cardErrorAnalytics(event, convMessage);
						}
					}
				});

		// Stop the flow and return from here in case any exception occurred and contentData becomes null
		if(rqst.getContentData() == null)
			return;

		//  Parameters to call if micro app already exists method for this case
		String mAppName = rqst.getContentData().cardObj.getAppName();
		int mAppVersionCode = rqst.getContentData().cardObj.getmAppVersionCode();
		byte botType = rqst.getBotType();
		String msisdn = rqst.getContentData().getMsisdn();

		if (!PlatformUtils.isMicroAppExist(mAppName,mAppVersionCode,msisdn,botType))
		{
			PlatformZipDownloader downloader = new PlatformZipDownloader.Builder().setArgRequest(rqst).setIsTemplatingEnabled(false).createPlatformZipDownloader();
			downloader.downloadAndUnzip();
		}
		else
		{
			saveMessage(convMessage);
		}
	}


	private void saveMessageBulk(JSONObject jsonObj) throws JSONException
	{
		ConvMessage convMessage = messagePreProcess(jsonObj);
		addToLists(convMessage.getMsisdn(), convMessage);

		MsgRelLogManager.logMsgRelEvent(convMessage, MsgRelEventType.RECIEVR_RECV_MSG);

		if (convMessage.isOneToNChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			ConvMessage convMessageNew = convDb.showParticipantStatusMessage(convMessage.getMsisdn());
			if (convMessageNew != null)
			{
				addToLists(convMessageNew.getMsisdn(), convMessageNew);
			}
		}
	}

    /**
     * This function pre-process on message of type "m" like make convMessage object , set metadata and timestamp
     *
     * @param jsonObj
     *            the JsonObject of type "m"
     *
     * @return ConvMessage object
     */
    private ConvMessage messagePreProcess(JSONObject jsonObj) throws JSONException
    {
        ConvMessage convMessage = new ConvMessage(jsonObj, context);
        if (convMessage.isStickerMessage())
        {
            convMessage.setMessage(context.getString(R.string.sent_sticker));
        }
        // AND-3843 begin
        if (convMessage.getMetadata() != null && convMessage.getMetadata().isPokeMessage())
        {
            convMessage.setMessage(context.getString(R.string.poke_msg));
        }
        // AND-3843 End
		/*
		 * Need to rename every audio recording to a unique name since the ios client is sending every file with the same name.
		 */
        if (convMessage.isFileTransferMessage())
        {
            MessageMetadata messageMetadata = convMessage.getMetadata();
            HikeFile hikeFile = messageMetadata.getHikeFiles().get(0);
            JSONObject metadataJson = messageMetadata.getJSON();
            // this value indicates that file is not downloaded yet
            JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
            for (int i = 0; i < fileArray.length(); i++)
            {
                JSONObject fileJson = fileArray.getJSONObject(i);
                Logger.d(getClass().getSimpleName(), "Previous json: " + fileJson);
                if (hikeFile.getHikeFileType() != HikeFileType.CONTACT && hikeFile.getHikeFileType() != HikeFileType.LOCATION) // dont change name for contact or location
                    fileJson.put(HikeConstants.FILE_NAME, Utils.getFinalFileName(hikeFile.getHikeFileType(), hikeFile.getFileName()));
                Logger.d(getClass().getSimpleName(), "New json: " + fileJson);
            }
			/*
			 * Resetting the metadata
			 */
            convMessage.setMetadata(metadataJson);
        }

        // Check if "pd" is there in response ===> if msg was a trackable msg
        // If found ===> update "pd" field of convMessage
        if (jsonObj.has(HikeConstants.PRIVATE_DATA))
        {
            JSONObject pd = jsonObj.getJSONObject(HikeConstants.PRIVATE_DATA);
            String uid = pd.getString(HikeConstants.MSG_REL_UID);
            MessagePrivateData messagePrivateData = new MessagePrivateData(uid);
            convMessage.setPrivateData(messagePrivateData);
        }

        //Check if message had platform data in the "pt" packet
        if(convMessage.getPlatformData() != null)
        {
            JSONObject keyboardJson = convMessage.getPlatformData().optJSONObject(HikeConstants.KEYBOARD_DATA);
            if(keyboardJson != null)
            {
                // Create custom keyboard object from keyboard json received in message
                JsonParser parser = new JsonParser();
                JsonObject keyboardJsonObj = (JsonObject) parser.parse(keyboardJson.toString());
                CustomKeyboard customKeyboard = new Gson().fromJson(keyboardJsonObj, CustomKeyboard.class);

                // Delete previous keyboard data from shared pref is the remove previous keyboard is set as true
                if(customKeyboard != null && customKeyboard.getRemove())
                    CustomKeyboardManager.getInstance().removeFromSharedPreferences(convMessage.getMsisdn());
                else
                    CustomKeyboardManager.getInstance().saveToSharedPreferences(convMessage.getMsisdn(), convMessage.getPlatformData());
            }
        }
        else
        {
            // Delete keyboard data from shared pref if the keyboard type is not persistent
            CustomKeyboard customKeyboard = CustomKeyboardManager.getInstance().getCustomKeyboardObject(convMessage.getMsisdn());
            if(customKeyboard != null && !customKeyboard.getKeep())
                CustomKeyboardManager.getInstance().removeFromSharedPreferences(convMessage.getMsisdn());
        }

		/*
		 * Applying the offset.
		 */
        convMessage.setTimestamp(Utils.applyServerTimeOffset(context, convMessage.getTimestamp()));

        return convMessage;
    }

	/**
	 * This function download sticker if not already downloaded
	 */
	private void messageProcessSticker(ConvMessage convMessage)
	{
		if (convMessage.isStickerMessage())
		{
			Sticker sticker = convMessage.getMetadata().getSticker();

			if (!sticker.isStickerAvailable())
			{
				StickerManager.getInstance().initiateSingleStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId(), convMessage);
			}
		}
	}

	/**
	 * This function gives data to sticker recommendation
	 */
	private void messageProcessStickerRecommendation(ConvMessage convMessage)
	{
		if (convMessage.isStickerMessage())
		{
			StickerSearchManager.getInstance().receivedMessage(null, convMessage.getMetadata().getSticker(), null);
		}
		else if (convMessage.isTextMsg())
		{
			StickerSearchManager.getInstance().receivedMessage(convMessage.getMessage(), null, null);
		}
	}

	/**
	 * This function decides whether to vibrate or not for a given message
	 */
	private void messageProcessVibrate(ConvMessage convMessage)
	{
		if (convMessage.getMetadata() != null)
		{
			if (convMessage.getMetadata().isPokeMessage())
			{
				boolean vibrate = false;
				String msisdn = convMessage.getMsisdn();
				if (ContactManager.getInstance().isConvExists(msisdn))
				{
					boolean activeStealthChat = StealthModeManager.getInstance().isStealthMsisdn(msisdn) && StealthModeManager.getInstance().isActive();
					boolean stealthNotifPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.STEALTH_NOTIFICATION_ENABLED, true);
					if (activeStealthChat || stealthNotifPref || !StealthModeManager.getInstance().isStealthMsisdn(msisdn))
					{

						if (OneToNConversationUtils.isGroupConversation(msisdn))
						{
							if (!HikeConversationsDatabase.getInstance().isGroupMuted(msisdn))
							{
								vibrate = true;
							}
						}
						else
						{
							vibrate = true;
						}
					}

				}
				if (vibrate)
				{
					Utils.vibrateNudgeReceived(context);
				}
			}
		}
		Logger.d(getClass().getSimpleName(), "Receiver received Message : " + convMessage.getMessage() + "		;	Receiver Msg ID : " + convMessage.getMsgID() + "	; Mapped msgID : "
				+ convMessage.getMappedMsgID());

	}

	/**
	 * This function does processing on file transfer message
	 *
	 * @param convMessage
	 *            the ConvMessage object with message id and conversation object initialized
	 */
	private void messageProcessFT(ConvMessage convMessage)
	{

		/*
		 * We need to add the name here in order to fix the bug where if the client receives two files of the same name, it shows the same file under both files.
		 */
		HikeFile hikeFile = null;
		if (convMessage.isFileTransferMessage())
		{
			hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			Logger.d(getClass().getSimpleName(), "FT MESSAGE: " + " NAME: " + hikeFile.getFileName() + " KEY: " + hikeFile.getFileKey() + "MSG ID: " + convMessage.getMsgID());
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
		{
			hikeFile = convMessage.platformMessageMetadata.getHikeFiles().get(0);
			Logger.d(getClass().getSimpleName(),
					"Native Card MESSAGE: " + " NAME: " + hikeFile.getFileName() + " KEY: " + hikeFile.getFileKey() + "MSG ID: " + convMessage.getMsgID());

		}
		if(hikeFile != null){
			Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
		}
		ContactManager manager = ContactManager.getInstance();
		String msisdn = convMessage.getMsisdn();
		/*
		 * Start auto download for media files
		 */
		String name = OneToNConversationUtils.isGroupConversation(msisdn) ? manager.getName(msisdn) : manager.getContact(msisdn, false, true).getName();
		if ((convMessage.isFileTransferMessage() || convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT) && ((!TextUtils.isEmpty(name)) || BotUtils.isBot(msisdn))
				&& (manager.isConvExists(msisdn)) && hikeFile != null)
		{

			NetworkType networkType = FTUtils.getNetworkType(context);
			if (hikeFile.getHikeFileType() == HikeFileType.IMAGE)
			{
				if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
						|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
				{
					FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
							convMessage, false, hikeFile);
				}
			}
			else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO || hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
			{
				if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_AUDIO_PREF, true))
						|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_AUDIO_PREF, false)))
				{
					FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
							convMessage, false, hikeFile);
				}
			}
			else if (hikeFile.getHikeFileType() == HikeFileType.VIDEO)
			{
				if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_VIDEO_PREF, true))
						|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_VIDEO_PREF, false)))
				{
					FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
							convMessage, false, hikeFile);
				}
			}

		}
	}

	private void saveDeliveryReport(JSONObject jsonObj) throws JSONException
	{
		String id = jsonObj.optString(HikeConstants.DATA);
		String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		long serverID;
		try
		{
			serverID = Long.parseLong(id);
		}
		catch (NumberFormatException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception occured while parsing msgId. Exception : " + e);
			serverID = -1;
		}
		Logger.d(getClass().getSimpleName(), "Delivery report received for msgid : " + serverID + "	;	REPORT : DELIVERED");

		Map<String, ArrayList<Long>> map = convDb.getMsisdnMapForServerId(serverID, msisdn);
		if (map != null && !map.isEmpty())
		{
			for (String chatMsisdn : map.keySet())
			{
				ArrayList<Long> values = map.get(chatMsisdn);
				if (values != null && !values.isEmpty())
				{
					long msgId = values.get(0); // max size this list will be of 1 only
					saveDeliveryReport(msgId, chatMsisdn);

					Logger.d(AnalyticsConstants.MSG_REL_TAG, "Handling ndr for json: " + jsonObj);
					MsgRelLogManager.logMsgRelDR(jsonObj, MsgRelEventType.DR_SHOWN_AT_SENEDER_SCREEN);
				}
			}
		}
	}

	private void saveDeliveryReport(long msgID, String msisdn)
	{
		int rowsUpdated = updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);

		if (rowsUpdated == 0)
		{
			Logger.d(getClass().getSimpleName(), "No rows updated");
			return;
		}

		Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);

		this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, pair);
	}

	/**
	 * <li>This function does specific "dr" processing for bulk.</li> <li>adds message id to {@link #messageStatusMap} second field if this id is grater than that present in second
	 * field
	 *
	 * @param jsonObj
	 *            JsonObject of type "dr"
	 * @throws JSONException
	 */
	private void saveDeliveryReportBulk(JSONObject jsonObj) throws JSONException
	{
		String id = jsonObj.optString(HikeConstants.DATA);
		String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		long msgID;
		try
		{
			msgID = Long.parseLong(id);
		}
		catch (NumberFormatException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception occured while parsing msgId. Exception : " + e);
			msgID = -1;
		}
		Logger.d(getClass().getSimpleName(), "Delivery report received for msgid : " + msgID + "	;	REPORT : DELIVERED");

		MsgRelLogManager.logMsgRelDR(jsonObj, MsgRelEventType.DR_SHOWN_AT_SENEDER_SCREEN);

		/*
		 * update message status map with max dr msgId corresponding to its msisdn
		 */

		if (messageStatusMap.get(msisdn) == null)
		{
			messageStatusMap.put(msisdn, new PairModified<PairModified<Long, Set<String>>, Long>(null, (long) -1));
		}
		if (null == messageStatusMap.get(msisdn).getFirst())
		{
			Set<String> msisdnSet = new HashSet<String>();
			PairModified<Long, Set<String>> pair = new PairModified<Long, Set<String>>((long) -1, msisdnSet);
			messageStatusMap.get(msisdn).setFirst(pair);
		}
		if (msgID > messageStatusMap.get(msisdn).getSecond())
		{
			messageStatusMap.get(msisdn).setSecond(msgID);
		}

	}

	public synchronized void saveMessageRead(JSONObject jsonObj) throws JSONException
	{
		JSONArray serverIds = jsonObj.optJSONArray(HikeConstants.DATA);

		if (serverIds == null)
		{
			Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
			return;
		}

		String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : id;

		ArrayList<Long> serverIdsArrayList = new ArrayList<Long>(serverIds.length());

		for (int i = 0; i < serverIds.length(); i++)
		{
			serverIdsArrayList.add(serverIds.optLong(i));
		}

		if (!OneToNConversationUtils.isOneToNConversation(id))
		{
			Map<String, ArrayList<Long>> map = convDb.getMsisdnMapForServerIds(serverIdsArrayList, id);
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "NOT GC so --> For mr/nmr, calling : ids, map" + serverIdsArrayList + " , .. " + map);
			if (map != null && !map.isEmpty())
			{
				for (String chatMsisdn : map.keySet())
				{
					ArrayList<Long> values = map.get(chatMsisdn);
					saveMessageRead(chatMsisdn, values, participantMsisdn);
				}
			}
		}
		else
		{
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "GROUP MR so --> For mr/nmr, calling : " + serverIdsArrayList);
			// This will only be called in case of group MR. there is bug in which MR for one person
			// in group are recieved by all other participants in group. If for those MR we try to find
			// a msisdn map we would end up finding a wrong message in db which we will incorrectly mark
			// is read.
			saveMessageRead(id, serverIdsArrayList, participantMsisdn);
		}
	}

	private void saveMessageRead(String msisdn, ArrayList<Long> msgIds, String participantMsisdn)
	{
		if (msgIds == null || msgIds.isEmpty())
		{
			Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
			return;
		}

		if (!OneToNConversationUtils.isOneToNConversation(msisdn))
		{

			ArrayList<Long> updatedMessageIds = convDb.setAllDeliveredMessagesReadForMsisdn(msisdn, msgIds);
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "For mr/nmr, reading : " + updatedMessageIds);
			if (updatedMessageIds == null || updatedMessageIds.isEmpty())
			{
				return;
			}
			long[] updatedMsgIdsLongArray = new long[updatedMessageIds.size()];
			for (int i = 0; i < updatedMessageIds.size(); i++)
			{
				updatedMsgIdsLongArray[i] = updatedMessageIds.get(i);
			}

			Pair<String, long[]> pair = new Pair<String, long[]>(msisdn, updatedMsgIdsLongArray);
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "For mr/nmr, firing pubsub MESSAGE_DELIVERED_READ: " + updatedMsgIdsLongArray);
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, pair);
		}
		else
		{
			if (TextUtils.isEmpty(participantMsisdn))
			{
				return;
			}

			long maxMsgId = convDb.setReadByForGroup(msisdn, msgIds, participantMsisdn);

			if (maxMsgId > 0)
			{
				Pair<Long, String> pair = new Pair<Long, String>(maxMsgId, participantMsisdn);
				Pair<String, Pair<Long, String>> groupPair = new Pair<String, Pair<Long, String>>(msisdn, pair);
				this.pubSub.publish(HikePubSub.ONETON_MESSAGE_DELIVERED_READ, groupPair);
			}
		}
	}

	private void saveNewMessageRead(JSONObject jsonObj)
	{
		try
		{
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "For nmr,jsonObject: " + jsonObj);

			// "d":{"msgid1":{track_id:"value"}}
			JSONObject msgMetadata = jsonObj.optJSONObject(HikeConstants.DATA);
			if (msgMetadata != null)
			{
				Iterator<?> keys = msgMetadata.keys();
				JSONArray serverIds = new JSONArray();
				while (keys.hasNext())
				{
					Long key = Long.parseLong((String) keys.next());
					serverIds.put(key);

					// Log Events For Message Reliability
					JSONObject pd = (JSONObject) msgMetadata.getJSONObject(String.valueOf(key));
					if (pd != null && pd.has(HikeConstants.MSG_REL_UID))
					{
						MsgRelLogManager.recordMsgRel(pd.optString(HikeConstants.MSG_REL_UID), MsgRelEventType.MR_SHOWN_AT_SENEDER_SCREEN, "-1");
					}
				}
				jsonObj.put(HikeConstants.DATA, serverIds);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, "For nmr,jsonObject sent to call 'mr' API: " + jsonObj);

				saveMessageRead(jsonObj);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * <li>This function does specific "mr" processing for bulk.</li>
	 * <p>
	 * In 1-1 conversation it adds max message id from ids list to {@link #messageStatusMap} first field if this id is greater than that present in first field
	 * </p>
	 *
	 * <p>
	 * In group conversation since we receive mr for messages sent by others also we have to first check whether the list of ids present in mr belongs to our conversation or not. <br>
	 * We call {@link HikeConversationsDatabase#getMrIdForGroup(String, long[])} passing groupId and ids as arguments.</br> It will return max id from list if it belongs to this
	 * conversation else it will return -1.
	 * <li>if id returned is less than that already present in first field we simply return</li>
	 * <li>if equals we have to participant msisdn to set</li>
	 * <li>if greater than we have clear set and update update both set and msgid fields in pair</li>
	 *
	 * @param jsonObj
	 *            -- mr json containing list of ids
	 * @throws JSONException
	 */
	private void saveMessageReadBulk(JSONObject jsonObj) throws JSONException
	{

		JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
		String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);

		String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : "";

		if (msgIds == null)
		{
			Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
			return;
		}

		if (messageStatusMap.get(id) == null)
		{
			messageStatusMap.put(id, new PairModified<PairModified<Long, Set<String>>, Long>(null, (long) -1));
		}

		if (null == messageStatusMap.get(id).getFirst())
		{
			Set<String> msisdnSet = new HashSet<String>();
			PairModified<Long, Set<String>> pair = new PairModified<Long, Set<String>>((long) -1, msisdnSet);
			messageStatusMap.get(id).setFirst(pair);
		}

		PairModified<Long, Set<String>> pair = messageStatusMap.get(id).getFirst();
		long msgID = -1;

		if (OneToNConversationUtils.isOneToNConversation(id))
		{
			ArrayList<Long> ids = new ArrayList<Long>(msgIds.length());
			for (int i = 0; i < msgIds.length(); i++)
			{
				ids.add(msgIds.optLong(i));
			}

			msgID = convDb.getMrIdForGroup(id, ids);
			if (pair.getFirst() > msgID)
			{
				return;
			}

			if (pair.getFirst() < msgID)
			{
				pair.setFirst(msgID);
				pair.getSecond().clear();
			}
			pair.getSecond().add(participantMsisdn);
		}
		else
		{

			for (int i = 0; i < msgIds.length(); i++)
			{
				long tempId = msgIds.optLong(i);
				if (tempId > msgID)
				{
					msgID = tempId;
				}
				if (msgID > pair.getFirst())
				{
					pair.setFirst(msgID);
				}
			}

		}
	}

	private void saveTyping(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : null;

		if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type))
		{
			addTypingNotification(id, participantMsisdn);
		}
		else
		{
			removeTypingNotification(id, participantMsisdn);
		}
	}

	private void saveUpdateAvailable(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		String version = data.optString(HikeConstants.VERSION);
		Editor editor = settings.edit();
		int update = Utils.isUpdateRequired(version, context) ? (data.optBoolean(HikeConstants.CRITICAL) ? HikeConstants.CRITICAL_UPDATE : HikeConstants.NORMAL_UPDATE)
				: HikeConstants.NO_UPDATE;
		editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, update);
		editor.putString(HikeConstants.Extras.UPDATE_MESSAGE, data.optString(HikeConstants.MqttMessageTypes.MESSAGE));
		editor.commit();
		if (update != HikeConstants.NO_UPDATE)
		{
			this.pubSub.publish(HikePubSub.UPDATE_AVAILABLE, update);
		}
	}

	private void saveAccountInfo(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

		boolean inviteTokenAdded = false;
		boolean inviteeNumChanged = false;
		boolean showNewRewards = false;
		boolean showNewGames = false;
		boolean talkTimeChanged = false;
		int newTalkTime = 0;

		Editor editor = settings.edit();
		if (data.has(HikeConstants.INVITE_TOKEN))
		{
			editor.putString(HikeConstants.INVITE_TOKEN, data.getString(HikeConstants.INVITE_TOKEN));
			inviteTokenAdded = true;
		}
		if (data.has(HikeConstants.TOTAL_CREDITS_PER_MONTH))
		{
			editor.putString(HikeConstants.TOTAL_CREDITS_PER_MONTH, data.getString(HikeConstants.TOTAL_CREDITS_PER_MONTH));
			inviteeNumChanged = true;
		}
		if (data.optBoolean(HikeConstants.DEFAULT_SMS_CLIENT_TUTORIAL))
		{
			setDefaultSMSClientTutorialSetting();
		}
		if (data.has(HikeConstants.ENABLE_FREE_INVITES))
		{
			boolean sendNativeInvite = !data.optBoolean(HikeConstants.ENABLE_FREE_INVITES, true);
			boolean showFreeInvitePopup = data.optBoolean(HikeConstants.SHOW_FREE_INVITES) && !settings.getBoolean(HikeMessengerApp.SET_FREE_INVITE_POPUP_PREF_FROM_AI, false);
			if (showFreeInvitePopup)
			{
				editor.putBoolean(HikeMessengerApp.SET_FREE_INVITE_POPUP_PREF_FROM_AI, true);
				editor.putBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, true);
			}

			handleSendNativeInviteKey(sendNativeInvite, showFreeInvitePopup, null, null, editor);
		}
		if(data.has(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER))
		{
			publishOpenComposeChatOnceEvent(data, settings);
		}
		if (data.has(HikeConstants.ACCOUNT))
		{
			JSONObject account = data.getJSONObject(HikeConstants.ACCOUNT);
			if (account.has(HikeConstants.ACCOUNTS))
			{
				JSONObject accounts = account.getJSONObject(HikeConstants.ACCOUNTS);
				/*
				 * if (accounts.has(HikeConstants.TWITTER)) { try { JSONObject twitter = accounts.getJSONObject(HikeConstants.TWITTER); String token =
				 * twitter.getString(HikeConstants.ID); String tokenSecret = twitter.getString(HikeConstants.TOKEN); HikeMessengerApp.makeTwitterInstance(token, tokenSecret);
				 * editor.putString(HikeMessengerApp.TWITTER_TOKEN, token); editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, tokenSecret);
				 * editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, true); } catch (JSONException e) { Logger.w(getClass().getSimpleName(), "Unknown format for twitter",
				 * e); } }
				 */
			}
			if (account.has(HikeMessengerApp.BACKUP_TOKEN_SETTING))
			{
				String backupToken = account.getString(HikeMessengerApp.BACKUP_TOKEN_SETTING);
				editor.putString(HikeMessengerApp.BACKUP_TOKEN_SETTING, backupToken);
			}
			if (account.has(HikeConstants.MUTED))
			{
				JSONObject mutedGroups = account.getJSONObject(HikeConstants.MUTED);
				JSONArray groupIds = mutedGroups.names();
				if (groupIds != null && groupIds.length() > 0)
				{
					for (int i = 0; i < groupIds.length(); i++)
					{
						convDb.toggleGroupMute(groupIds.optString(i), true);
					}
				}
			}

			if (account.has(HikeConstants.FAVORITES))
			{
				JSONObject favorites = account.getJSONObject(HikeConstants.FAVORITES);

				if (favorites.length() > 0)
				{
					//ContactManager.getInstance().setMultipleContactsToFavorites(favorites);
				}
			}

			String rewardsToken = account.optString(HikeConstants.REWARDS_TOKEN);
			if (!TextUtils.isEmpty(rewardsToken))
			{
				/*
				 * Server may disable rewards but not necessary invalidate rewards token. To help Server not resend rewards token (and thus save server side DB queries) when later
				 * enabling rewards, client will be caching the rewards token.
				 */
				editor.putString(HikeMessengerApp.REWARDS_TOKEN, rewardsToken);
				// TODO. Should this be games_token ?
				editor.putString(HikeMessengerApp.GAMES_TOKEN, rewardsToken);

				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, account.optBoolean(HikeConstants.SHOW_REWARDS));
				editor.putBoolean(HikeMessengerApp.SHOW_GAMES, account.optBoolean(HikeConstants.SHOW_GAMES));
				if (account.optBoolean(HikeConstants.SHOW_REWARDS))
				{
					showNewRewards = true;
				}

				if (account.optBoolean(HikeConstants.SHOW_GAMES))
				{
					showNewGames = true;
				}

			}
			else
			{
				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, false);
				editor.putBoolean(HikeMessengerApp.SHOW_GAMES, false);
			}

			if (account.has(HikeConstants.REWARDS))
			{
				JSONObject rewards = account.getJSONObject(HikeConstants.REWARDS);

				int talkTime = rewards.optInt(HikeConstants.TALK_TIME, -1);
				if (talkTime > -1)
				{
					editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
					talkTimeChanged = true;
					newTalkTime = talkTime;
				}
			}
			if (account.has(HikeConstants.NEW_LAST_SEEN_SETTING))
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				Editor settingEditor = settings.edit();
				if (account.has(HikeConstants.LAST_SEEN_SETTING))
				{
					settingEditor.putBoolean(HikeConstants.LAST_SEEN_PREF, account.optBoolean(HikeConstants.LAST_SEEN_SETTING, true));
				}
				settingEditor.putString(HikeConstants.LAST_SEEN_PREF_LIST, Integer.toString(account.optInt(HikeConstants.NEW_LAST_SEEN_SETTING)));
				settingEditor.commit();
			}
			if (account.has(HikeConstants.UJ_NOTIF_SETTING))
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				Editor settingEditor = settings.edit();
				settingEditor.putBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, account.optInt(HikeConstants.UJ_NOTIF_SETTING, 1) == 1 ? true : false);
				settingEditor.commit();
			}
			if (account.has(HikeConstants.CHAT_BACKGROUNDS))
			{
				JSONArray chatBackgroundArray = account.getJSONArray(HikeConstants.CHAT_BACKGROUNDS);
				convDb.setChatThemesFromArray(chatBackgroundArray);
			}
			if (account.has(HikeConstants.CHAT_BACKGROUD_NOTIFICATION))
			{
				boolean showNotification = account.optInt(HikeConstants.CHAT_BG_NOTIFICATION_PREF, 0) != -1;
				Editor settingEditor = settings.edit();
				settingEditor.putBoolean(HikeConstants.CHAT_BG_NOTIFICATION_PREF, showNotification);
				settingEditor.commit();
			}
			if (account.has(HikeConstants.AVATAR))
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				Editor settingEditor = settings.edit();
				int dpSetting = account.optInt(HikeConstants.AVATAR, 1);
				boolean defaultSetting = false;
				if (dpSetting == 2)
				{
					defaultSetting = true;
				}
				settingEditor.putBoolean(HikeConstants.PROFILE_PIC_PREF, defaultSetting);
				settingEditor.commit();
			}
			/*
			 * WebView names and their respective urls for Rewards and Hike Extras will be controlled by server
			 */
			// Hike Extras
			if (account.has(HikeConstants.HIKE_EXTRAS_NAME))
			{
				String hikeExtrasName = account.getString(HikeConstants.HIKE_EXTRAS_NAME);
				editor.putString(HikeConstants.HIKE_EXTRAS_NAME, hikeExtrasName);
			}
			if (account.has(HikeConstants.HIKE_EXTRAS_URL))
			{
				String hikeExtrasUrl = account.getString(HikeConstants.HIKE_EXTRAS_URL);
				editor.putString(HikeConstants.HIKE_EXTRAS_URL, hikeExtrasUrl);
			}

			// Hike Rewards
			if (account.has(HikeConstants.REWARDS_NAME))
			{
				String rewards_name = account.getString(HikeConstants.REWARDS_NAME);
				editor.putString(HikeConstants.REWARDS_NAME, rewards_name);
			}
			if (account.has(HikeConstants.REWARDS_URL))
			{
				String rewards_url = account.getString(HikeConstants.REWARDS_URL);
				editor.putString(HikeConstants.REWARDS_URL, rewards_url);
			}
		}
		// this logic requires the backup token which is being setup in the previous if case
		UserLogInfo.requestUserLogs(data);

		editor.commit();
		if (inviteTokenAdded)
		{
			pubSub.publish(HikePubSub.INVITE_TOKEN_ADDED, null);
		}
		if (inviteeNumChanged)
		{
			pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
		}
		if (talkTimeChanged)
		{
			pubSub.publish(HikePubSub.TALK_TIME_CHANGED, newTalkTime);
		}
		if (showNewGames || showNewRewards)
		{
			this.pubSub.publish(HikePubSub.UPDATE_OF_MENU_NOTIFICATION, null);
		}
		if (data.has(HikeConstants.METADATA))
		{
			JSONObject mmobObject = data.getJSONObject(HikeConstants.METADATA);
			if (mmobObject.has(HikeConstants.NUX))
				NUXManager.getInstance().parseNuxPacket(mmobObject.getJSONObject(HikeConstants.NUX).toString());
		}

		/**
		 * { "d" : { "plfsync" : { "platformUid" : "ABCDEFxxxxxx" , "platformToken" : "PQRSTUVxxxxxx" } } }
		 */
		if (data.has(HikePlatformConstants.PLATFORM_USER_ID_SYNC))
		{
			JSONObject plfSyncJson = data.getJSONObject(HikePlatformConstants.PLATFORM_USER_ID_SYNC);
			PlatformUtils.savePlatformCredentials(plfSyncJson);
		}

		/*
		 * activating stickey
		 */
		ChatHeadUtils.activateChatHead(null);

	}

	private void publishOpenComposeChatOnceEvent(JSONObject data, SharedPreferences settings) throws JSONException
	{
		settings.edit().putBoolean(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER, data.getBoolean(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER)).commit();
		HikeMessengerApp.getPubSub().publish(HikePubSub.OPEN_COMPOSE_CHAT_SCREEN, null);
	}

	private void saveUserOptIn(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);

		// For one-to-one chat
		saveStatusMsg(jsonObj, msisdn);

		List<String> groupConversations = convDb.listOfGroupConversationsWithMsisdn(msisdn);

		// Set the dnd status for the participant for all group chats
		convDb.updateDndStatus(msisdn);
		// For group chats
		for (String groupId : groupConversations)
		{
			saveStatusMsg(jsonObj, groupId);
		}
	}

	private void saveBlockInternationalSMS(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		saveStatusMsg(jsonObj, msisdn);
	}

	private void saveAddFavorite(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);

		ContactManager conMgr = ContactManager.getInstance();
		/*
		 * Ignore if contact is blocked.
		 */
		if (conMgr.isBlocked(msisdn))
		{
			return;
		}

		ContactInfo contactInfo = conMgr.getContact(msisdn, true, false);
		if (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
		{
			return;
		}
		FavoriteType currentType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentType == FavoriteType.NOT_FRIEND || currentType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.REQUEST_RECEIVED
				: FavoriteType.FRIEND;

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
		this.pubSub.publish(favoriteType == FavoriteType.REQUEST_RECEIVED ? HikePubSub.FAVORITE_TOGGLED : HikePubSub.FRIEND_REQUEST_ACCEPTED, favoriteToggle);

		if (favoriteType == favoriteType.FRIEND)
		{
			incrementUnseenStatusCount();

			ConvMessage message = Utils.generateAddFriendSystemMessage(msisdn, HikeMessengerApp.getInstance().getString(R.string.friend_req_inline_msg_received, contactInfo.getFirstNameAndSurname()), true, State.RECEIVED_UNREAD);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ADD_INLINE_FRIEND_MSG, message);
		}
		else if (favoriteType == favoriteType.REQUEST_RECEIVED && currentType != favoriteType.REQUEST_RECEIVED)
		{
			int count = settings.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
			if (count >= 0)
			{
				Utils.incrementOrDecrementFriendRequestCount(settings, 1);
			}

			ConvMessage message = Utils.generateAddFriendSystemMessage(msisdn, HikeMessengerApp.getInstance().getString(R.string.friend_req_inline_msg_received, contactInfo.getFirstNameAndSurname()), true, State.RECEIVED_UNREAD);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ADD_INLINE_FRIEND_MSG, message);
		}

		contactInfo.setFavoriteType(favoriteType);

		if (favoriteType == FavoriteType.FRIEND)
		{
			StatusMessage statusMessage = new StatusMessage(0, null, msisdn, contactInfo.getName(), context.getString(R.string.confirmed_friend),
					StatusMessageType.FRIEND_REQUEST_ACCEPTED, System.currentTimeMillis() / 1000);

			convDb.addStatusMessage(statusMessage, true);

			pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
			pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
		}
	}

	public void saveAutoApkInstallConfig(JSONObject jsonObj) throws JSONException
	{

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.MqttMessageTypes.AUTO_APK);
			metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.AutoApkEvents.RECEIVED_INITIAL_PING);

			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ANALYTICS_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.w("AUTOAPK", "Invalid json");
		}


		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		final Editor e = settings.edit();

		String connectionType = data.optString(HikeConstants.CONNECTION_TYPE, "wifi");
		e.putString(HikeConstants.CONNECTION_TYPE, connectionType);

		final int downloadTime = data.optInt(HikeConstants.DOWNLOAD_TIME, 72);
		e.putInt(HikeConstants.DOWNLOAD_TIME, downloadTime);

		final int installPromptFreq = data.optInt(HikeConstants.INSTALL_PROMPT_FREQUENCY, 3);
		e.putInt(HikeConstants.INSTALL_PROMPT_FREQUENCY, installPromptFreq);


		final long installPromptInterval = data.optLong(HikeConstants.INSTALL_PROMPT_INTERVAL, 0l);
		e.putLong(HikeConstants.INSTALL_PROMPT_INTERVAL, installPromptInterval);

		final double apkSizeMultiplier = data.optDouble(HikeConstants.APK_SIZE_MULTIPLIER, 3.0);
		e.putFloat(HikeConstants.APK_SIZE_MULTIPLIER, (float)apkSizeMultiplier);

		JSONObject tipJson = data.optJSONObject(HikeConstants.INSTALL_PROMPT_METHOD);
		if(tipJson != null)
		{
			e.putString(HikeConstants.AutoApkDownload.NEW_APK_TIP_JSON, tipJson.toString());
		}
		else
		{
			Logger.d("AUTOAPK","tip json not present");
		}

		FTApkManager.makeRequest(apkSizeMultiplier);

		e.commit();
	}
	
	private void saveAccountConfig(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		Editor editor = settings.edit();

		if (data.has(HikeConstants.VOIP_BITRATE_2G))
		{
			int bitrate = data.getInt(HikeConstants.VOIP_BITRATE_2G);
			editor.putInt(HikeMessengerApp.VOIP_BITRATE_2G, bitrate);
		}
		if (data.has(HikeConstants.VOIP_BITRATE_3G))
		{
			int bitrate = data.getInt(HikeConstants.VOIP_BITRATE_3G);
			editor.putInt(HikeMessengerApp.VOIP_BITRATE_3G, bitrate);
		}
		if (data.has(HikeConstants.VOIP_BITRATE_WIFI))
		{
			int bitrate = data.getInt(HikeConstants.VOIP_BITRATE_WIFI);
			editor.putInt(HikeMessengerApp.VOIP_BITRATE_WIFI, bitrate);
		}
		if (data.has(HikeConstants.VOIP_BITRATE_CONFERENCE))
		{
			int bitrate = data.getInt(HikeConstants.VOIP_BITRATE_CONFERENCE);
			editor.putInt(HikeConstants.VOIP_BITRATE_CONFERENCE, bitrate);
		}
		if (data.has(HikeConstants.VOIP_ACTIVATED))
		{
			int activateVoip = data.getInt(HikeConstants.VOIP_ACTIVATED);
			editor.putInt(HikeConstants.VOIP_ACTIVATED, activateVoip);
		}
		if (data.has(HikeConstants.VOIP_FTUE_POPUP))
		{
			boolean showFtuePopup = data.getBoolean(HikeConstants.VOIP_FTUE_POPUP);
			editor.putBoolean(HikeMessengerApp.SHOW_VOIP_FTUE_POPUP, showFtuePopup);
		}
		if (data.has(HikeConstants.VOIP_CALL_RATE_POPUP_FREQ))
		{
			int freq = data.getInt(HikeConstants.VOIP_CALL_RATE_POPUP_FREQ);
			editor.putInt(HikeMessengerApp.VOIP_CALL_RATE_POPUP_FREQUENCY, freq);
		}
		if (data.has(HikeConstants.VOIP_RELAY_SERVER_PORT))
		{
			int port = data.getInt(HikeConstants.VOIP_RELAY_SERVER_PORT);
			editor.putInt(HikeConstants.VOIP_RELAY_SERVER_PORT, port);
		}
		if (data.has(HikeConstants.VOIP_AEC_ENABLED))
		{
			boolean aecEnabled = data.getBoolean(HikeConstants.VOIP_AEC_ENABLED);
			editor.putBoolean(HikeConstants.VOIP_AEC_ENABLED, aecEnabled);
		}
		if (data.has(HikeConstants.VOIP_CONFERENCING_ENABLED))
		{
			boolean enabled = data.getBoolean(HikeConstants.VOIP_CONFERENCING_ENABLED);
			editor.putBoolean(HikeConstants.VOIP_CONFERENCING_ENABLED, enabled);
		}
		if (data.has(HikeConstants.VOIP_GROUP_CALL_ENABLED))
		{
			boolean enabled = data.getBoolean(HikeConstants.VOIP_GROUP_CALL_ENABLED);
			editor.putBoolean(HikeConstants.VOIP_GROUP_CALL_ENABLED, enabled);
		}
		if (data.has(HikeConstants.VOIP_AEC_CPU_NR))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_CPU_NR);
			editor.putInt(HikeConstants.VOIP_AEC_CPU_NR, val);
		}
		if (data.has(HikeConstants.VOIP_AEC_CPU))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_CPU);
			editor.putInt(HikeConstants.VOIP_AEC_CPU, val);
		}
		if (data.has(HikeConstants.VOIP_AEC_MO))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_MO);
			editor.putInt(HikeConstants.VOIP_AEC_MO, val);
		}
		if (data.has(HikeConstants.VOIP_AEC_TYPE))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_TYPE);
			editor.putInt(HikeConstants.VOIP_AEC_TYPE, val);
		}
		if (data.has(HikeConstants.VOIP_AEC_CNP))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_CNP);
			editor.putInt(HikeConstants.VOIP_AEC_CNP, val);
		}
		if (data.has(HikeConstants.VOIP_AEC_TAIL_TYPE))
		{
			int val = data.getInt(HikeConstants.VOIP_AEC_TAIL_TYPE);
			editor.putInt(HikeConstants.VOIP_AEC_TAIL_TYPE, val);
		}
		if (data.has(HikeConstants.VOIP_RATINGS_LEFT))
		{
			int val = data.getInt(HikeConstants.VOIP_RATINGS_LEFT);
			editor.putInt(HikeConstants.VOIP_RATINGS_LEFT, val);
		}
		if (data.has(HikeConstants.VOIP_RELAY_IPS))
		{
			JSONArray array = data.getJSONArray(HikeConstants.VOIP_RELAY_IPS);
			Set<String> ips = new HashSet<>();
			for (int i = 0; i < array.length(); i++)
			{
				ips.add(array.getString(i));
			}
			editor.putStringSet(HikeConstants.VOIP_RELAY_IPS, ips);
		}

		if (data.has(HikeConstants.REWARDS_TOKEN))
		{
			String rewardToken = data.getString(HikeConstants.REWARDS_TOKEN);
			editor.putString(HikeMessengerApp.REWARDS_TOKEN, rewardToken);
		}
		if (data.has(HikeConstants.GAMES_TOKEN))
		{
			String gamesToken = data.getString(HikeConstants.GAMES_TOKEN);
			editor.putString(HikeMessengerApp.GAMES_TOKEN, gamesToken);
		}
		if (data.has(HikeConstants.SHOW_REWARDS))
		{
			boolean showRewards = data.getBoolean(HikeConstants.SHOW_REWARDS);
			if (TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.REWARDS_TOKEN, "")))
			{
				showRewards = false;
			}
			editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, showRewards);
			editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, !showRewards);
			if (showRewards)
			{
				editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
			}
		}
		if (data.has(HikeConstants.SHOW_GAMES))
		{
			boolean showGames = data.getBoolean(HikeConstants.SHOW_GAMES);
			if (TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.REWARDS_TOKEN, "")))
			{
				showGames = false;
			}
			editor.putBoolean(HikeMessengerApp.SHOW_GAMES, showGames);
			editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, !showGames);
			if (showGames)
			{
				editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
			}
		}
		if (data.has(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS))
		{
			JSONArray array = data.getJSONArray(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS);
			editor.putString(HikeMessengerApp.BATCH_STATUS_NOTIFICATION_VALUES, array.toString());
		}
		if (data.has(HikeConstants.ENABLE_FREE_INVITES))
		{
			String newId = data.optString(HikeConstants.MESSAGE_ID);
			String currentId = settings.getString(HikeMessengerApp.FREE_INVITE_PREVIOUS_ID, "");
			/*
			 * Duplicate check
			 */
			if (currentId.equals(newId))
			{
				Logger.d(getClass().getSimpleName(), "Duplicate enable free invite packet");
				return;
			}

			editor.putString(HikeMessengerApp.FREE_INVITE_PREVIOUS_ID, newId);
			editor.putBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, false);

			boolean sendNativeInvite = !data.optBoolean(HikeConstants.ENABLE_FREE_INVITES, true);
			boolean showFreeInvitePopup = data.optBoolean(HikeConstants.SHOW_FREE_INVITES);
			String header = data.optString(HikeConstants.FREE_INVITE_POPUP_TITLE);
			String body = data.optString(HikeConstants.FREE_INVITE_POPUP_TEXT);

			handleSendNativeInviteKey(sendNativeInvite, showFreeInvitePopup, header, body, editor);

			/*
			 * Show notification if free SMS is turned on.
			 */
			if (!sendNativeInvite && HikeMessengerApp.isIndianUser())
			{
				Bundle bundle = new Bundle();
				bundle.putString(HikeConstants.Extras.FREE_SMS_POPUP_BODY, body);
				bundle.putString(HikeConstants.Extras.FREE_SMS_POPUP_HEADER, header);

				this.pubSub.publish(HikePubSub.SHOW_FREE_INVITE_SMS, bundle);
			}
		}
		if (data.has(HikeConstants.MQTT_IP_ADDRESSES))
		{
			JSONArray ipArray = data.getJSONArray(HikeConstants.MQTT_IP_ADDRESSES);
			if (null != ipArray && ipArray.length() > 0)
			{
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(HikePubSub.IPS_CHANGED).putExtra("ips", ipArray.toString()));
			}
		}

		if (data.has(MqttConstants.MQTT_PORTS))
		{
			JSONArray portsArray = data.getJSONArray(MqttConstants.MQTT_PORTS);
			if (null != portsArray && portsArray.length() > 0)
			{
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(
						new Intent(HikePubSub.PORTS_CHANGED).putExtra(MqttConstants.MQTT_PORTS, portsArray.toString()));
			}
		}
		// watsapp invite message
		if (data.has(HikeConstants.WATSAPP_INVITE_ENABLED))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.WATSAPP_INVITE_ENABLED, data.getBoolean(HikeConstants.WATSAPP_INVITE_ENABLED));
		}
		if (data.has(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY))
		{
			String message = data.getString(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY, message);
		}

		/*
		 * WebView names and their respective urls for Rewards and Hike Extras will be controlled by server
		 */
		// Hike Extras
		if (data.has(HikeConstants.HIKE_EXTRAS_NAME))
		{
			String hikeExtrasName = data.getString(HikeConstants.HIKE_EXTRAS_NAME);
			editor.putString(HikeConstants.HIKE_EXTRAS_NAME, hikeExtrasName);
		}
		if (data.has(HikeConstants.HIKE_EXTRAS_URL))
		{
			String hikeExtrasUrl = data.getString(HikeConstants.HIKE_EXTRAS_URL);
			editor.putString(HikeConstants.HIKE_EXTRAS_URL, hikeExtrasUrl);
		}

		// Hike Rewards
		if (data.has(HikeConstants.REWARDS_NAME))
		{
			String rewards_name = data.getString(HikeConstants.REWARDS_NAME);
			editor.putString(HikeConstants.REWARDS_NAME, rewards_name);
		}
		if (data.has(HikeConstants.REWARDS_URL))
		{
			String rewards_url = data.getString(HikeConstants.REWARDS_URL);
			editor.putString(HikeConstants.REWARDS_URL, rewards_url);
		}
		if (data.has(HikeConstants.REPLY_NOTIFICATION_RETRY_TIMER))
		{
			int retryTimeInMinutes = data.getInt(HikeConstants.REPLY_NOTIFICATION_RETRY_TIMER);
			editor.putLong(HikeMessengerApp.RETRY_NOTIFICATION_COOL_OFF_TIME, retryTimeInMinutes * 60 * 1000);
		}
		if (data.has(HikeConstants.MqttMessageTypes.CREATE_MULTIPLE_BOTS))
		{
			JSONArray botsTobeAdded = data.getJSONArray(HikeConstants.MqttMessageTypes.CREATE_MULTIPLE_BOTS);
			for (int i = 0; i< botsTobeAdded.length(); i++){
				BotUtils.createBot((JSONObject) botsTobeAdded.get(i),Utils.getNetworkShortinOrder(Utils.getNetworkTypeAsString(HikeMessengerApp.getInstance().getApplicationContext())));

			}
		}
		if (data.has(HikeConstants.MqttMessageTypes.REMOVE_MICRO_APP))
		{
			JSONArray microAppsTobeRemoved = data.getJSONArray(HikeConstants.MqttMessageTypes.REMOVE_MICRO_APP);
			for (int i = 0; i < microAppsTobeRemoved.length(); i++)
			{
				// First check dmapp packet json structure and based on the packet structure call respective method to delete from versioning directory or older micro app path
                JSONObject jsonObject = (JSONObject) microAppsTobeRemoved.get(i);

				if(jsonObject.has(HikePlatformConstants.APP_NAME))
                {
                    BotUtils.removeMicroAppByAppName((JSONObject) microAppsTobeRemoved.get(i));
                }
                if(jsonObject.has(HikePlatformConstants.MSISDN))
                {
                    BotUtils.removeMicroAppFromVersioningPathByMsisdn((JSONObject) microAppsTobeRemoved.get(i));
                }
                else
                {
                    Logger.e(TAG, "Stopping mapps delete flow as wrong packet structure is being sent for dmapp");
                }
			}
		}
		if (data.has(HikeConstants.MqttMessageTypes.NOTIFY_MICRO_APP_STATUS))
		{
			boolean doNotify = data.optBoolean(HikeConstants.MqttMessageTypes.NOTIFY_MICRO_APP_STATUS);
			if (doNotify)
			{
				PlatformUtils.platformDiskConsumptionAnalytics(HikeConstants.MqttMessageTypes.NOTIFY_MICRO_APP_STATUS);
			}
		}
		if (data.has(HikeConstants.MqttMessageTypes.DELETE_MULTIPLE_BOTS))
		{
			JSONArray botsTobeAdded = data.getJSONArray(HikeConstants.MqttMessageTypes.DELETE_MULTIPLE_BOTS);
			for (int i = 0; i < botsTobeAdded.length(); i++)
			{
				BotUtils.deleteBot((String) botsTobeAdded.get(i));
			}
		}

		if (data.has(HikeConstants.MqttMessageTypes.MICROAPP_DOWNLOAD))
		{
			JSONArray appsToBeDownloaded = data.getJSONArray(HikeConstants.MqttMessageTypes.MICROAPP_DOWNLOAD);
			for (int i = 0; i < appsToBeDownloaded.length(); i++)
			{
				PlatformUtils.downloadZipFromPacket((JSONObject) appsToBeDownloaded.get(i),Utils.getNetworkShortinOrder(Utils.getNetworkTypeAsString(context)));
			}
		}
		if (data.has(HikeConstants.GET_BULK_LAST_SEEN))
		{
			boolean blsPref = data.optBoolean(HikeConstants.GET_BULK_LAST_SEEN);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.BULK_LAST_SEEN_PREF, blsPref);
		}

		// check for analytics configuration packet
		if (data.has(AnalyticsConstants.ANALYTICS_FILESIZE))
		{
			long fileSize = data.getLong(AnalyticsConstants.ANALYTICS_FILESIZE);
			HAManager.getInstance().setFileMaxSize(fileSize);
		}
		if (data.has(AnalyticsConstants.ANALYTICS))
		{
			boolean isAnalyticsEnabled = data.getBoolean(AnalyticsConstants.ANALYTICS);
			HAManager.getInstance().setAnalyticsEnabled(isAnalyticsEnabled);
		}
		if (data.has(AnalyticsConstants.ANALYTICS_TOTAL_SIZE))
		{
			long size = data.getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE);
			HAManager.getInstance().setAnalyticsMaxSizeOnClient(size);
		}
		if (data.has(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY))
		{
			int freq = data.getInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY);
			HAManager.getInstance().setAnalyticsSendFrequency(freq);
		}
		if (data.has(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE))
		{
			int size = data.getInt(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE);
			HAManager.getInstance().setMaxInMemoryEventsSize(size);
		}
		if (data.has(HikeConstants.ENABLE_DETAILED_HTTP_LOGGING))
		{
			boolean enableDetailedHttpLogging = data.getBoolean(HikeConstants.ENABLE_DETAILED_HTTP_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DETAILED_HTTP_LOGGING_ENABLED, enableDetailedHttpLogging);
		}

		// this is a PnC of ["nu"/"ru"] and ["Txt"/"Ttl"/"Cht"/"Typ"]
		// we also assume that if "nuTxt" field or "nuTyp" field is present then others exist too
		if (data.has(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT) || data.has(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.PUSH_SETTING))
		{
			HikeSharedPreferenceUtil ujPrefs = HikeSharedPreferenceUtil.getInstance(UJFile);

			String notificationText = data.optString(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT, context.getString(R.string.joined_hike));
			ujPrefs.saveData(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT, notificationText);

			String notificationTitle = data.optString(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.NOTIF_TITLE, context.getString(R.string.last_seen_more_ct));
			ujPrefs.saveData(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.NOTIF_TITLE, notificationTitle);

			int notificationType = data.optInt(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.PUSH_SETTING, HikeConstants.PushType.silent);
			ujPrefs.saveData(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.PUSH_SETTING, notificationType);

			boolean persistChat = data.optBoolean(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.PERSIST_CHAT, HikeConstants.UserJoinMsg.defaultPersistChat);
			ujPrefs.saveData(HikeConstants.NEW_USER + HikeConstants.UserJoinMsg.PERSIST_CHAT, persistChat);
		}
		// we also assume that if "ruTxt" field or "ruTyp" field is present then others exist too
		if (data.has(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT) || data.has(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.PUSH_SETTING))
		{
			HikeSharedPreferenceUtil ujPrefs = HikeSharedPreferenceUtil.getInstance(UJFile);

			String notificationText = data.optString(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT, context.getString(R.string.user_back_on_hike));
			ujPrefs.saveData(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.NOTIF_TEXT, notificationText);

			String notificationTitle = data.optString(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.NOTIF_TITLE, context.getString(R.string.last_seen_more_ct));
			ujPrefs.saveData(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.NOTIF_TITLE, notificationTitle);

			int notificationType = data.optInt(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.PUSH_SETTING, HikeConstants.PushType.silent);
			ujPrefs.saveData(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.PUSH_SETTING, notificationType);

			boolean persistChat = data.optBoolean(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.PERSIST_CHAT, HikeConstants.UserJoinMsg.defaultPersistChat);
			ujPrefs.saveData(HikeConstants.RETURNING_USER + HikeConstants.UserJoinMsg.PERSIST_CHAT, persistChat);

		}
		if (data.has(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT))
		{
			long timeout = data.getLong(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT, timeout);
		}
		if (data.has(HikeConstants.Extras.GENERAL_SO_TIMEOUT))
		{
			long timeout = data.getLong(HikeConstants.Extras.GENERAL_SO_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.GENERAL_SO_TIMEOUT, timeout);
			AccountUtils.setSocketTimeout((int) timeout);
		}
		if (data.has(HikeConstants.Extras.OKHTTP_CONNECT_TIMEOUT))
		{
			int timeout = data.getInt(HikeConstants.Extras.OKHTTP_CONNECT_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.OKHTTP_CONNECT_TIMEOUT, timeout);
		}
		if (data.has(HikeConstants.Extras.OKHTTP_READ_TIMEOUT))
		{
			int timeout = data.getInt(HikeConstants.Extras.OKHTTP_READ_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.OKHTTP_READ_TIMEOUT, timeout);
		}
		if (data.has(HikeConstants.Extras.OKHTTP_WRITE_TIMEOUT))
		{
			int timeout = data.getInt(HikeConstants.Extras.OKHTTP_WRITE_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.OKHTTP_WRITE_TIMEOUT, timeout);
		}
		if (data.has(HikeConstants.OK_HTTP))
		{
			boolean okhttp = data.getBoolean(HikeConstants.OK_HTTP);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.TOGGLE_OK_HTTP, okhttp);
		}

		if (data.has(HikeMessengerApp.ENABLE_ADDRESSBOOK_THROUGH_HTTP_MGR))
		{
			boolean enAb = data.getBoolean(HikeMessengerApp.ENABLE_ADDRESSBOOK_THROUGH_HTTP_MGR);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.ENABLE_ADDRESSBOOK_THROUGH_HTTP_MGR, enAb);
		}

		if (data.has(HikeMessengerApp.EDIT_PROFILE_THROUGH_HTTP_MGR))
		{
			boolean editProfileHttpMgr = data.getBoolean(HikeMessengerApp.EDIT_PROFILE_THROUGH_HTTP_MGR);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.EDIT_PROFILE_THROUGH_HTTP_MGR, editProfileHttpMgr);
		}

		if (data.has(HikeConstants.Extras.CHANGE_MAX_MESSAGE_PROCESS_TIME))
		{
			long maxMessageProcessTime = data.optLong(HikeConstants.Extras.CHANGE_MAX_MESSAGE_PROCESS_TIME);
			if (maxMessageProcessTime > 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.MAX_MESSAGE_PROCESS_TIME, maxMessageProcessTime);
			}
		}
		if (data.has(HikeConstants.Extras.ENABLE_PHOTOS))
		{
			boolean enablePhoto = data.getBoolean(HikeConstants.Extras.ENABLE_PHOTOS);
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.ENABLE_PHOTOS, enablePhoto);

			/**
			 * This Pubsub updates ActionBar on HomeActivity
			 */
			this.pubSub.publish(HikePubSub.UPDATE_OF_PHOTOS_ICON, null);
		}
		if (data.has(HikeConstants.Extras.ENABLE_CLOUD_SETTING_BACKUP))
		{
			boolean enableSettingsBackup = data.getBoolean(HikeConstants.Extras.ENABLE_CLOUD_SETTING_BACKUP);
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.ENABLE_CLOUD_SETTING_BACKUP, enableSettingsBackup);
		}
		if (data.has(HikeConstants.Extras.ENABLE_SEND_LOGS))
		{
			boolean enableSendLogs = data.getBoolean(HikeConstants.Extras.ENABLE_SEND_LOGS);
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.ENABLE_SEND_LOGS, enableSendLogs);
			AppConfig.refresh();
		}
		if (data.has(HikeConstants.Extras.STATUS_UPDATE_SHOW_COUNTS))
		{
			boolean showCount = data.optBoolean(HikeConstants.Extras.STATUS_UPDATE_SHOW_COUNTS);

			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.STATUS_UPDATE_SHOW_COUNTS, showCount);

			boolean showLikes = data.optBoolean(HikeConstants.Extras.STATUS_UPDATE_SHOW_LIKES, false);

			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.STATUS_UPDATE_SHOW_LIKES, showLikes);
		}
		if (data.has(HikeConstants.URL_WHITELIST))
		{
			handleWhitelistDomains(data.getString(HikeConstants.URL_WHITELIST));
		}
		if (data.has(HikeConstants.REPLY_NOTIFICATION_RETRY_COUNT))
		{
			int replyNotificationRetryCount = data.getInt(HikeConstants.REPLY_NOTIFICATION_RETRY_COUNT);
			editor.putInt(HikeMessengerApp.MAX_REPLY_RETRY_NOTIF_COUNT, replyNotificationRetryCount);
		}
		if (data.has(HikeConstants.NOTIFICATION_RETRY))
		{
			String notification = data.getString(HikeConstants.NOTIFICATION_RETRY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIFICATION_RETRY_JSON, notification);
		}
		if (data.has(HikeConstants.Extras.STICKER_DESCRIPTION))
		{
			String shareStrings = data.getString(HikeConstants.Extras.STICKER_DESCRIPTION);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.STICKER_DESCRIPTION, shareStrings);
		}
		if (data.has(HikeConstants.Extras.STICKER_CAPTION))
		{
			String shareStrings = data.getString(HikeConstants.Extras.STICKER_CAPTION);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.STICKER_CAPTION, shareStrings);
		}
		if (data.has(HikeConstants.Extras.TEXT_HEADING))
		{
			String shareStrings = data.getString(HikeConstants.Extras.TEXT_HEADING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.TEXT_HEADING, shareStrings);
		}
		if (data.has(HikeConstants.Extras.TEXT_CAPTION))
		{
			String shareStrings = data.getString(HikeConstants.Extras.TEXT_CAPTION);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.TEXT_CAPTION, shareStrings);
		}
		if (data.has(HikeConstants.Extras.IMAGE_HEADING))
		{
			String shareStrings = data.getString(HikeConstants.Extras.IMAGE_HEADING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.IMAGE_HEADING, shareStrings);
		}
		if (data.has(HikeConstants.Extras.IMAGE_DESCRIPTION))
		{
			String shareStrings = data.getString(HikeConstants.Extras.IMAGE_DESCRIPTION);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.IMAGE_DESCRIPTION, shareStrings);
		}
		if (data.has(HikeConstants.Extras.IMAGE_CAPTION))
		{
			String shareStrings = data.getString(HikeConstants.Extras.IMAGE_CAPTION);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.IMAGE_CAPTION, shareStrings);
		}
		if (data.has(HikeConstants.Extras.SHOW_SHARE_FUNCTIONALITY))
		{
			boolean shareStrings = data.getBoolean(HikeConstants.Extras.SHOW_SHARE_FUNCTIONALITY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.SHOW_SHARE_FUNCTIONALITY, shareStrings);
		}
		if (data.has(HikeConstants.ChatHead.STICKER_WIDGET))
		{
			ChatHeadUtils.activateChatHead(data);
		}

		if (data.has(HikeConstants.PROB_NUM_TEXT_MSG))
		{
			int textMsgMaxNumber = data.getInt(HikeConstants.PROB_NUM_TEXT_MSG);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PROB_NUM_TEXT_MSG, textMsgMaxNumber);
		}
		if (data.has(HikeConstants.PROB_NUM_STICKER_MSG))
		{
			int stkMsgMaxNumber = data.getInt(HikeConstants.PROB_NUM_STICKER_MSG);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PROB_NUM_STICKER_MSG, stkMsgMaxNumber);
		}
		if (data.has(HikeConstants.PROB_NUM_MULTIMEDIA_MSG))
		{
			int multimediaMsgMaxNumber = data.getInt(HikeConstants.PROB_NUM_MULTIMEDIA_MSG);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.PROB_NUM_MULTIMEDIA_MSG, multimediaMsgMaxNumber);
		}
		if (data.has(HikeConstants.ENABLE_EXCEPTION_ANALYTIS))
		{
			boolean enable = data.getBoolean(HikeConstants.ENABLE_EXCEPTION_ANALYTIS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.EXCEPTION_ANALYTIS_ENABLED, enable);
		}
		if (data.has(HikeConstants.CONTACT_UPDATE_WAIT_TIME))
		{
			long contactUpdateWaitTime = data.getLong(HikeConstants.CONTACT_UPDATE_WAIT_TIME);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.CONTACT_UPDATE_WAIT_TIME, contactUpdateWaitTime);
		}
		if (data.has(HikeConstants.DELETE_IC_ON_CONTACT_REMOVE))
		{
			boolean deleteIc = data.getBoolean(HikeConstants.DELETE_IC_ON_CONTACT_REMOVE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DELETE_IC_ON_CONTACT_REMOVE, deleteIc);
		}
		if (data.has(HikeConstants.CONTACT_REMOVE_DUPLICATES_WHILE_SYNCING))
		{
			boolean removeDuplicates = data.getBoolean(HikeConstants.CONTACT_REMOVE_DUPLICATES_WHILE_SYNCING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CONTACT_REMOVE_DUPLICATES_WHILE_SYNCING, removeDuplicates);
		}
		if (data.has(HikeConstants.SESSION_LOG_TRACKING))
		{
			boolean sessionLogEnable = data.getBoolean(HikeConstants.SESSION_LOG_TRACKING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SESSION_LOG_TRACKING, sessionLogEnable);
			ChatHeadUtils.startOrStopService(false);
		}
		UserLogInfo.requestUserLogs(data);

		if (data.has(HikeConstants.PROB_NUM_HTTP_ANALYTICS))
		{
			int httpAnalyticsMaxNumber = data.getInt(HikeConstants.PROB_NUM_HTTP_ANALYTICS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.PROB_NUM_HTTP_ANALYTICS, httpAnalyticsMaxNumber);
		}

		if (data.has(HikeConstants.NOTIFIACTION_DELAY_GROUP))
		{
			int groupNotificationDelay = data.getInt(HikeConstants.NOTIFIACTION_DELAY_GROUP);
			if (groupNotificationDelay >= 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIFIACTION_DELAY_GROUP, groupNotificationDelay);
			}
		}

		if (data.has(HikeConstants.NOTIFIACTION_DELAY_ONE_TO_ONE))
		{
			int oneToOneNotificationDelay = data.getInt(HikeConstants.NOTIFIACTION_DELAY_ONE_TO_ONE);
			if (oneToOneNotificationDelay >= 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIFIACTION_DELAY_ONE_TO_ONE, oneToOneNotificationDelay);
			}
		}

		if (data.has(HikeConstants.KEYBOARD_CONFIGURATION))
		{
			int kc = data.getInt(HikeConstants.KEYBOARD_CONFIGURATION);
			if (kc >= 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KEYBOARD_CONFIGURATION, kc);
			}
		}

		if (data.has(HikeConstants.KPT_EXIT_SERVER_SWITCH))
		{
			boolean showExitUI = data.getBoolean(HikeConstants.KPT_EXIT_SERVER_SWITCH);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KPT_EXIT_SERVER_SWITCH, showExitUI);
		}

		if (data.has(HikeConstants.KPT_EXIT_SERVER_TEXT))
		{
			String text = data.getString(HikeConstants.KPT_EXIT_SERVER_TEXT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KPT_EXIT_SERVER_TEXT, text);
		}

		if (data.has(HikeConstants.KPT_EXIT_HEADING))
		{
			String text = data.getString(HikeConstants.KPT_EXIT_HEADING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KPT_EXIT_HEADING, text);
		}

		if (data.has(HikeConstants.KPT_EXIT_PHONE_BUTTON))
		{
			String text = data.getString(HikeConstants.KPT_EXIT_PHONE_BUTTON);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KPT_EXIT_PHONE_BUTTON, text);
		}

		if (data.has(HikeConstants.KPT_EXIT_GOOGLE_BUTTON))
		{
			String text = data.getString(HikeConstants.KPT_EXIT_GOOGLE_BUTTON);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.KPT_EXIT_GOOGLE_BUTTON, text);
		}

		if (data.has(HikeConstants.SUPER_COMPRESSED_IMG_SIZE))
		{
			int superCompressedImgSize = data.getInt(HikeConstants.SUPER_COMPRESSED_IMG_SIZE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SUPER_COMPRESSED_IMG_SIZE, superCompressedImgSize);
		}
		if (data.has(HikeConstants.NORMAL_IMG_SIZE))
		{
			int normalImgSize = data.getInt(HikeConstants.NORMAL_IMG_SIZE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NORMAL_IMG_SIZE, normalImgSize);
		}
		if (data.has(HikeConstants.DEFAULT_IMG_QUALITY_FOR_SMO))
		{
			int defaultImgQuality = data.getInt(HikeConstants.DEFAULT_IMG_QUALITY_FOR_SMO);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DEFAULT_IMG_QUALITY_FOR_SMO, defaultImgQuality);
		}
		if (data.has(HikeConstants.SHOW_TOAST_FOR_DEGRADING_QUALITY))
		{
			boolean toShowToastForDegradingQuality = data.getBoolean(HikeConstants.SHOW_TOAST_FOR_DEGRADING_QUALITY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_TOAST_FOR_DEGRADING_QUALITY, toShowToastForDegradingQuality);

		}
		if (data.has(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_SMALL))
		{
			long image_size_small = data.getLong(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_SMALL);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_SMALL, image_size_small);
		}
		if (data.has(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_MEDIUM))
		{
			long image_size_medium = data.getLong(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_MEDIUM);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_MEDIUM, image_size_medium);
		}
		if (data.has(HikeConstants.SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY))
		{
			int image_quality = data.getInt(HikeConstants.SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY, image_quality);
		}
		if (data.has(HikeConstants.STEALTH))
		{
			String stealthValue = data.getString(HikeConstants.STEALTH);
			if (stealthValue.equals(HikeConstants.ENABLED_STEALTH))
			{
				HikeAnalyticsEvent.sendStealthMsisdns(StealthModeManager.getInstance().getStealthMsisdns(), null);
			}
		}

		if (data.has(HikeConstants.OTHER_EXCEPTION_LOGGING))
		{
			boolean otherExceptionLogging = data.getBoolean(HikeConstants.OTHER_EXCEPTION_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.OTHER_EXCEPTION_LOGGING, otherExceptionLogging);
		}
		if (data.has(HikeConstants.HTTP_EXCEPTION_LOGGING))
		{
			boolean httpExceptionLogging = data.getBoolean(HikeConstants.HTTP_EXCEPTION_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HTTP_EXCEPTION_LOGGING, httpExceptionLogging);
		}
		if (data.has(HikeConstants.CONN_PROD_AREA_LOGGING))
		{
			boolean connLogging = data.getBoolean(HikeConstants.CONN_PROD_AREA_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CONN_PROD_AREA_LOGGING, connLogging);
		}
		if (data.has(HikeConstants.GCM_PROD_AREA_LOGGING)) {
			boolean gcmLogging = data.getBoolean(HikeConstants.GCM_PROD_AREA_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.GCM_PROD_AREA_LOGGING, gcmLogging);
		}
		if (data.has(HikeConstants.SERVER_CONFIGURABLE_GROUP_SETTING))
		{
			int groupSetting = data.getInt(HikeConstants.SERVER_CONFIGURABLE_GROUP_SETTING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SERVER_CONFIGURABLE_GROUP_SETTING, groupSetting);
		}
		if (data.has(HikeConstants.MESSAGING_PROD_AREA_LOGGING))
		{
			boolean msgingLogging = data.getBoolean(HikeConstants.MESSAGING_PROD_AREA_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MESSAGING_PROD_AREA_LOGGING, msgingLogging);
		}
		if (data.has(HikeConstants.SPECIAL_DAY_TRIGGER))
		{
			boolean independenceTrigger = data.getBoolean(HikeConstants.SPECIAL_DAY_TRIGGER);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SPECIAL_DAY_TRIGGER, independenceTrigger);
		}
		if (data.has(HikeConstants.ENABLE_TIMELINE_FTUE))
		{
			boolean enableTimelineFTUE = data.getBoolean(HikeConstants.ENABLE_TIMELINE_FTUE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, enableTimelineFTUE);
		}
		if (data.has(HikeConstants.NOTIFICATIONS_PRIORITY))
		{
			int priority = data.getInt(HikeConstants.NOTIFICATIONS_PRIORITY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NOTIFICATIONS_PRIORITY, priority);
		}
		if (data.has(MqttConstants.MQTT_PING_SENDER))
		{
			int pingSender = data.getInt(MqttConstants.MQTT_PING_SENDER);
			HikeSharedPreferenceUtil.getInstance().saveData(MqttConstants.MQTT_PING_SENDER, pingSender);
		}
		if (data.has(MqttConstants.ALARM_PING_WAKELOCK_TIMEOUT))
		{
			int alarmPingWakeLockTimeout = data.getInt(MqttConstants.ALARM_PING_WAKELOCK_TIMEOUT);
			HikeSharedPreferenceUtil.getInstance().saveData(MqttConstants.ALARM_PING_WAKELOCK_TIMEOUT, alarmPingWakeLockTimeout);
		}
		if (data.has(HikeConstants.FT_HOST_IPS))
		{
			String ftHostIps = data.getString(HikeConstants.FT_HOST_IPS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FT_HOST_IPS, ftHostIps);
			HttpManager.setFtHostUris();
		}
		if (data.has(HikeConstants.HTTP_HOST_IPS))
		{
			String httpHostIps = data.getString(HikeConstants.HTTP_HOST_IPS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HTTP_HOST_IPS, httpHostIps);
			HttpManager.setProductionHostUris();
		}
		if (data.has(HikeConstants.HTTP_HOST_PLATFORM_IPS))
		{
			String httpHostIps = data.getString(HikeConstants.HTTP_HOST_PLATFORM_IPS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HTTP_HOST_PLATFORM_IPS, httpHostIps);
			HttpManager.setPlatformProductionHostUris();
		}
		if (data.has(HikeConstants.SPECIAL_DAY_TRIGGER))
		{
			boolean independenceTrigger = data.getBoolean(HikeConstants.SPECIAL_DAY_TRIGGER);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SPECIAL_DAY_TRIGGER, independenceTrigger);
		}

		if (data.has(HikeConstants.STICKER_RECOMMENDATION_ENABLED))
		{
			boolean isStickerRecommendationEnabled = data.getBoolean(HikeConstants.STICKER_RECOMMENDATION_ENABLED);
			Logger.d(StickerTagWatcher.TAG, "received sticker recommendation server side packet, value : " + isStickerRecommendationEnabled);
			StickerManager.getInstance().toggleStickerRecommendation(isStickerRecommendationEnabled);
		}

		if (data.has(HikeConstants.STICKER_AUTO_RECOMMENDATION_ENABLED))
		{
			boolean autoRecommendationTurningOn = data.getBoolean(HikeConstants.STICKER_AUTO_RECOMMENDATION_ENABLED);

			// Turn on auto-suggestion of stickers
			if (autoRecommendationTurningOn)
			{
				// Remove previous observations for turning off, if any
				StickerSearchManager.getInstance().saveOrDeleteAutoPopupTrialState(true);

				if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, false))
				{
					StickerSearchUtility.saveStickerRecommendationSettingsValue(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, true);

					StickerSearchManager.getInstance().setShowAutoPopupSettingOn(true);
				}
			}
			// Trigger to turn off auto-suggestion of stickers depending upon pattern found in packet
			else
			{
				int autoRecommendationContinuousRejectionCount = data.optInt(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF, 0);

				// Set turning-off pattern auto-suggestion of stickers, if they are getting rejected
				if (autoRecommendationContinuousRejectionCount > 0)
				{
					int autoRecommendationRejectionPatternCount = data.optInt(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF,
							StickerSearchConstants.MINIMUM_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT);

					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF,
							autoRecommendationContinuousRejectionCount);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF,
							autoRecommendationRejectionPatternCount);

					StickerSearchManager.getInstance().setShowAutoPopupTurnOffPattern(autoRecommendationContinuousRejectionCount, autoRecommendationRejectionPatternCount);
				}
				// Turn off auto-suggestion of stickers, if no pattern is recognized in packet or rejection count is zero (no-tolerance)
				else
				{
					// Remove previous observations for turning off, if any
					StickerSearchManager.getInstance().saveOrDeleteAutoPopupTrialState(true);

					if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, false))
					{
						StickerSearchUtility.saveStickerRecommendationSettingsValue(HikeConstants.STICKER_RECOMMEND_AUTOPOPUP_PREF, false);

						StickerSearchManager.getInstance().setShowAutoPopupSettingOn(false);
					}
				}
			}
		}

		if (data.has(HikeConstants.STICKER_TAG_REFRESH_TIME_INTERVAL))
		{
			long tagRefreshInterval = data.getLong(HikeConstants.STICKER_TAG_REFRESH_TIME_INTERVAL);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_TAG_REFRESH_TIME_INTERVAL, tagRefreshInterval);
		}

		if (data.has(HikeConstants.STICKER_RECOMMENDATION_DOWNLOAD_TAGS))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DEFAULT_TAGS_DOWNLOADED, false);
			StickerManager.getInstance().downloadDefaultTagsFirstTime(false);
		}

		if (data.has(HikeConstants.STICKER_RECOMMENDATION_CONFIGURATION_DATA))
		{
			StickerSearchUtility.saveStickerRecommendationConfiguration(data.getJSONObject(HikeConstants.STICKER_RECOMMENDATION_CONFIGURATION_DATA));
		}

		if (data.has(HikeConstants.CHAT_SEARCH_ENABLED))
		{
			boolean chatSearchEnable = data.getBoolean(HikeConstants.CHAT_SEARCH_ENABLED);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CHAT_SEARCH_ENABLED, chatSearchEnable);
		}

		if (data.has(HikeConstants.DP_IMAGE_SIZE))
		{
			int dpImageSize = data.getInt(HikeConstants.DP_IMAGE_SIZE);
			editor.putInt(HikeConstants.DP_IMAGE_SIZE, dpImageSize);
		}

		if (data.has(HikeConstants.INVITE_TOKEN))
		{
			editor.putString(HikeConstants.INVITE_TOKEN, data.getString(HikeConstants.INVITE_TOKEN));
		}

		if (data.has(HikeConstants.InviteSection.INVITE_SECTION))
		{
			JSONObject inviteSection = data.getJSONObject(HikeConstants.InviteSection.INVITE_SECTION);
			if (inviteSection.has(HikeConstants.InviteSection.SHOW_EXTRA_INVITE_SECTION))
			{
				editor.putBoolean(HikeConstants.InviteSection.SHOW_EXTRA_INVITE_SECTION, inviteSection.getBoolean(HikeConstants.InviteSection.SHOW_EXTRA_INVITE_SECTION));
			}
			if (inviteSection.has(HikeConstants.InviteSection.INVITE_SECTION_MAIN_TEXT))
			{
				editor.putString(HikeConstants.InviteSection.INVITE_SECTION_MAIN_TEXT, inviteSection.getString(HikeConstants.InviteSection.INVITE_SECTION_MAIN_TEXT));
			}
			if (inviteSection.has(HikeConstants.InviteSection.INVITE_SECTION_BOTTOM_TEXT))
			{
				editor.putString(HikeConstants.InviteSection.INVITE_SECTION_BOTTOM_TEXT, inviteSection.getString(HikeConstants.InviteSection.INVITE_SECTION_BOTTOM_TEXT));
			}
			if (inviteSection.has(HikeConstants.InviteSection.INVITE_SECTION_IMAGE))
			{
				editor.putString(HikeConstants.InviteSection.INVITE_SECTION_IMAGE, inviteSection.getString(HikeConstants.InviteSection.INVITE_SECTION_IMAGE));
			}
		}

		if (data.has(HikeConstants.REWARDS_BOT_MSISDN))
		{
			editor.putString(HikeConstants.REWARDS_BOT_MSISDN, data.getString(HikeConstants.REWARDS_BOT_MSISDN));
		}

		if (data.has(HikeConstants.EXTRAS_BOT_MSISDN))
		{
			editor.putString(HikeConstants.EXTRAS_BOT_MSISDN, data.getString(HikeConstants.EXTRAS_BOT_MSISDN));
		}
		if (data.has(HikeConstants.REFERRAL_EMAIL_TEXT))
		{
			editor.putString(HikeConstants.REFERRAL_EMAIL_TEXT, data.getString(HikeConstants.REFERRAL_EMAIL_TEXT));
		}
		if (data.has(HikeConstants.REFERRAL_OTHER_TEXT))
		{
			editor.putString(HikeConstants.REFERRAL_OTHER_TEXT, data.getString(HikeConstants.REFERRAL_OTHER_TEXT));
		}
		if (data.has(HikeConstants.FT_LATENCY_LOGGING))
		{
			boolean httpExceptionLogging = data.getBoolean(HikeConstants.FT_LATENCY_LOGGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FT_LATENCY_LOGGING, httpExceptionLogging);
		}

		if (data.has(HikeConstants.ALL_STICKER_TAG_DOWNLOAD))
		{
			boolean shouldDownload = data.getBoolean(HikeConstants.ALL_STICKER_TAG_DOWNLOAD);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.TAG_FIRST_TIME_DOWNLOAD, shouldDownload);

			if (shouldDownload)
			{
				StickerManager.getInstance().downloadStickerTagData();
			}
		}

		if (data.has(HikeConstants.NUDGE_SEND_COOLOFF_TIME))
		{
			int nudgeCoolOffTime = data.getInt(HikeConstants.NUDGE_SEND_COOLOFF_TIME);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NUDGE_SEND_COOLOFF_TIME, nudgeCoolOffTime);
		}
		if (data.has(HikeConstants.OFFLINE))
		{
			String offline = data.optString(HikeConstants.OFFLINE, "{}");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.OFFLINE, offline);
			OfflineController.getInstance().setConfiguration(offline);
		}
		if (data.has(HikeConstants.SHOW_HIGH_RES_IMAGE))
		{
			boolean activate = data.getBoolean(HikeConstants.SHOW_HIGH_RES_IMAGE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_HIGH_RES_IMAGE, activate);
		}
		if (data.has(HikeConstants.ENABLE_GC_VIA_LINK_SHARING))
		{
			boolean enableGCViaLinkSharing = data.getBoolean(HikeConstants.ENABLE_GC_VIA_LINK_SHARING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_GC_VIA_LINK_SHARING, enableGCViaLinkSharing);
		}
		if (data.has(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA))
		{
			String textGCWA = data.getString(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA, textGCWA);
		}
		if (data.has(HikeConstants.MENU_OPTIONS_FOR_GC_VIA_OTHERS))
		{
			String textGCOthers = data.getString(HikeConstants.MENU_OPTIONS_FOR_GC_VIA_OTHERS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MENU_OPTIONS_FOR_GC_VIA_OTHERS, textGCOthers);
		}
		if (data.has(HikeConstants.ENABLE_MENU_OPTION_FOR_GC_VIA_WA))
		{
			boolean enableMenu = data.getBoolean(HikeConstants.ENABLE_MENU_OPTION_FOR_GC_VIA_WA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_MENU_OPTION_FOR_GC_VIA_WA, enableMenu);
		}
		if (data.has(HikeConstants.ENABLE_MENU_OPTIONS_FOR_GC_VIA_OTHERS))
		{
			boolean enableMenu = data.getBoolean(HikeConstants.ENABLE_MENU_OPTIONS_FOR_GC_VIA_OTHERS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_MENU_OPTIONS_FOR_GC_VIA_OTHERS, enableMenu);
		}
		if (data.has(HikeConstants.TEXT_FOR_GC_VIA_WA))
		{
			String textGCWA = data.getString(HikeConstants.TEXT_FOR_GC_VIA_WA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TEXT_FOR_GC_VIA_WA, textGCWA);
		}
		if (data.has(HikeConstants.TEXT_FOR_GC_VIA_OTHERS))
		{
			String textGCOthers = data.getString(HikeConstants.TEXT_FOR_GC_VIA_OTHERS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TEXT_FOR_GC_VIA_OTHERS, textGCOthers);
		}
		if (data.has(StickyCaller.SHOW_STICKY_CALLER))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.SHOW_STICKY_CALLER, data.optBoolean(StickyCaller.SHOW_STICKY_CALLER, false));
			if (!data.optBoolean(StickyCaller.SHOW_STICKY_CALLER, false))
			{
				ChatHeadUtils.unregisterCallReceiver();
			}
		}
		if (data.has(HikeConstants.ACTIVATE_STICKY_CALLER_PREF))
		{
			Utils.setSharedPrefValue(context, HikeConstants.ACTIVATE_STICKY_CALLER_PREF, data.optBoolean(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, false));
			if (data.optBoolean(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, false))
			{
				ChatHeadUtils.registerCallReceiver();
			}
			else
			{
				ChatHeadUtils.unregisterCallReceiver();
			}
		}
		if (data.has(StickyCaller.SHOW_KNOWN_NUMBER_CARD))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.SHOW_KNOWN_NUMBER_CARD, data.optBoolean(StickyCaller.SHOW_KNOWN_NUMBER_CARD, false));
		}
		if (data.has(HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF))
		{
			Utils.setSharedPrefValue(context, HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF, data.optBoolean(HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF, true));
		}
		if (data.has(StickyCaller.SHOW_FREEMESSAGE))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.SHOW_FREEMESSAGE, data.optBoolean(StickyCaller.SHOW_FREEMESSAGE, true));
		}
		if (data.has(StickyCaller.SHOW_FREECALL))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.SHOW_FREECALL, data.optBoolean(StickyCaller.SHOW_FREECALL, true));
		}
		if (data.has(StickyCaller.SHOW_SMS_CARD_PREF))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.SHOW_SMS_CARD_PREF, data.optBoolean(StickyCaller.SHOW_SMS_CARD_PREF, false));
		}
		if (data.has(HikeConstants.SMS_CARD_ENABLE_PREF))
		{
			Utils.setSharedPrefValue(context, HikeConstants.SMS_CARD_ENABLE_PREF, data.optBoolean(HikeConstants.SMS_CARD_ENABLE_PREF, false));
		}
		if (data.has(StickyCaller.ENABLE_CLIPBOARD_CARD))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.ENABLE_CLIPBOARD_CARD, data.optBoolean(StickyCaller.ENABLE_CLIPBOARD_CARD, true));
			ChatHeadUtils.registerOrUnregisterClipboardListener(context);
		}
		if (data.has(HikeConstants.BOT_TABLE_REFRESH))
		{
			boolean shouldrefreshBotTable = data.getBoolean(HikeConstants.BOT_TABLE_REFRESH);
			if (shouldrefreshBotTable)
			{
				if (data.has(HikeConstants.BOTS)) // Expect a JSONArray here
				{
					// This pubSub is listened by DbconversationListener, which fills the db
					HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_DISCOVERY_DOWNLOAD_SUCCESS, data);
				}

				else
				// Assuming this is simply a flush packet for the table
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_DISCOVERY_TABLE_FLUSH, null);
				}
			}
		}

		if (data.has(HikeConstants.ADD_DISCOVERY_BOTS))
		{
			if (data.getBoolean(HikeConstants.ADD_DISCOVERY_BOTS) && data.has(HikeConstants.BOTS))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_DISCOVERY_DOWNLOAD_SUCCESS, data);
			}

			else
			{
				Logger.e("BotDiscovery", "Did not find { bot : [] } Bot array to populate");
			}
		}

		if (data.has(HikeConstants.ENABLE_BOT_DISCOVERY))
		{
			boolean enableBotDiscovery = data.getBoolean(HikeConstants.ENABLE_BOT_DISCOVERY);

			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_BOT_DISCOVERY, enableBotDiscovery);
		}

		if (data.has(HikeConstants.GET_DISCOVERY_BOTS))
		{
			boolean shouldGetBots = data.getBoolean(HikeConstants.GET_DISCOVERY_BOTS);
			if (shouldGetBots)
			{
				JSONObject json = new JSONObject();
				JSONArray botsArray = HikeContentDatabase.getInstance().getDiscoveryBotMsisdnArray();
				json.put(AnalyticsConstants.DISCOVERY_BOTS, botsArray);
				json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.GET_DISCOVERY_BOT_LIST);
				json.put(AnalyticsConstants.DISCOVERY_BOTS, json);
				HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.BOT_DISCOVERY, json);
			}
		}

		if (data.has(HikeConstants.BOTS_DISCOVERY_SECTION))
		{
			String sectionName = data.optString(HikeConstants.BOTS_DISCOVERY_SECTION, HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.hike_apps));
			if (!TextUtils.isEmpty(sectionName))
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BOTS_DISCOVERY_SECTION, sectionName);
			}
		}
		if (data.has(HikeConstants.FT_USE_APACHE_HTTP_CLIENT))
		{
			boolean useApacheClient = data.getBoolean(HikeConstants.FT_USE_APACHE_HTTP_CLIENT);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FT_USE_APACHE_HTTP_CLIENT, useApacheClient);
		}
		if (data.has(HikeConstants.NEW_CHAT_RED_DOT))
		{
			boolean shouldShowRedDot = data.optBoolean(HikeConstants.NEW_CHAT_RED_DOT);
			if (shouldShowRedDot)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NEW_CHAT_RED_DOT, true);
				HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_NEW_CHAT_RED_DOT, null);
			}
		}
		if (data.has(HikeConstants.INTERCEPTS.SHOW_INTERCEPTS))
		{
			JSONObject showIntercepts = data.getJSONObject(HikeConstants.INTERCEPTS.SHOW_INTERCEPTS);

			if (showIntercepts.has(HikeConstants.INTERCEPTS.VIDEO))
			{
				boolean video = showIntercepts.getBoolean(HikeConstants.INTERCEPTS.VIDEO);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.SHOW_VIDEO_INTERCEPT, video);
			}
			if (showIntercepts.has(HikeConstants.INTERCEPTS.IMAGE))
			{
				boolean image = showIntercepts.getBoolean(HikeConstants.INTERCEPTS.IMAGE);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.SHOW_IMAGE_INTERCEPT, image);
			}
			if (showIntercepts.has(HikeConstants.INTERCEPTS.SCREENSHOTS))
			{
				boolean screenshot = showIntercepts.getBoolean(HikeConstants.INTERCEPTS.SCREENSHOTS);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.SHOW_SCREENSHOT_INTERCEPT, screenshot);
                InterceptUtils.registerOrUnregisterScreenshotObserver();
			}
		}
		if (data.has(HikeConstants.INTERCEPTS.ENABLE_INTERCEPTS))
		{
			JSONObject enableIntercepts = data.getJSONObject(HikeConstants.INTERCEPTS.ENABLE_INTERCEPTS);

			if (enableIntercepts.has(HikeConstants.INTERCEPTS.VIDEO) && !HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.INTERCEPTS.ENABLE_VIDEO_INTERCEPT))
			{
				boolean video = enableIntercepts.getBoolean(HikeConstants.INTERCEPTS.VIDEO);
				Utils.setSharedPrefValue(context, HikeConstants.INTERCEPTS.ENABLE_VIDEO_INTERCEPT, video);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.ENABLE_VIDEO_INTERCEPT, video);
			}
			if (enableIntercepts.has(HikeConstants.INTERCEPTS.IMAGE) && !HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.INTERCEPTS.ENABLE_IMAGE_INTERCEPT))
			{
				boolean image = enableIntercepts.getBoolean(HikeConstants.INTERCEPTS.IMAGE);
				Utils.setSharedPrefValue(context, HikeConstants.INTERCEPTS.ENABLE_IMAGE_INTERCEPT, image);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.ENABLE_IMAGE_INTERCEPT, image);
			}
			if (enableIntercepts.has(HikeConstants.INTERCEPTS.SCREENSHOTS)
					&& !HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.INTERCEPTS.ENABLE_SCREENSHOT_INTERCEPT))
			{
				boolean screenshot = enableIntercepts.getBoolean(HikeConstants.INTERCEPTS.SCREENSHOTS);
				Utils.setSharedPrefValue(context, HikeConstants.INTERCEPTS.ENABLE_SCREENSHOT_INTERCEPT, screenshot);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.ENABLE_SCREENSHOT_INTERCEPT, screenshot);
                InterceptUtils.registerOrUnregisterScreenshotObserver();
			}
		}
		if(data.has(HikeConstants.SPACE_MANAGER.NOTIFY_DISK_SPACE_USAGE))
		{
			boolean notifyDiskUsage = data.optBoolean(HikeConstants.SPACE_MANAGER.NOTIFY_DISK_SPACE_USAGE, false);
			if(notifyDiskUsage)
			{
				if(data.has(HikeConstants.SPACE_MANAGER.DIRECTORY_LIST))
				{
					JSONArray dirList = data.getJSONArray(HikeConstants.SPACE_MANAGER.DIRECTORY_LIST);
					StorageSpecUtils.processDirectoryList(dirList);
				}
			}
		}
		if (data.has(HikeConstants.BADGECOUNTER))
		{
			boolean enableBadgeCount = data.getBoolean(HikeConstants.BADGECOUNTER);

			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BADGE_COUNT_ENABLED, enableBadgeCount);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
		}

		if (data.has(HikeConstants.SHOW_GPS_DIALOG))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_GPS_DIALOG, data.optBoolean(HikeConstants.SHOW_GPS_DIALOG));
		}
		if (data.has(OfflineConstants.HIKE_DIRECT_MENU_OPTIONS))
		{
			String options = data.getJSONObject(OfflineConstants.HIKE_DIRECT_MENU_OPTIONS).toString();
			HikeSharedPreferenceUtil.getInstance().saveData(OfflineConstants.HIKE_DIRECT_MENU_OPTIONS, options);
		}

		if (data.has(HikePlatformConstants.RECURRING_LOCATION))
		{
			JSONObject json = data.getJSONObject(HikePlatformConstants.RECURRING_LOCATION);
			PlatformUtils.requestRecurringLocationUpdates(json);
		}
		if (data.has(HikeConstants.LOCALIZATION_ENABLED))
		{
			Utils.setLocalizationEnable(data.optBoolean(HikeConstants.LOCALIZATION_ENABLED));
		}
		if (data.has(HikeConstants.HTTP_NETWORK_CHECK_CALL))
		{
			boolean enable = data.getBoolean(HikeConstants.HTTP_NETWORK_CHECK_CALL);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HTTP_NETWORK_CHECK_CALL, enable);
		}
		if (data.has(HikePlatformConstants.CUSTOM_TABS))
		{
			boolean enable = data.getBoolean(HikePlatformConstants.CUSTOM_TABS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.CUSTOM_TABS, enable);
		}
		if(data.has(HikePlatformConstants.NEW_AUTH_ENABLE))
		{
			boolean enable=data.getBoolean(HikePlatformConstants.NEW_AUTH_ENABLE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.NEW_AUTH_ENABLE, enable);
		}
		if (data.has(HikeConstants.WHITE_SCREEN_FIX))
		{
			boolean enableWhiteScreenFix = data.getBoolean(HikeConstants.WHITE_SCREEN_FIX);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.WHITE_SCREEN_FIX, enableWhiteScreenFix);
			CustomWebView.setApplyWhiteScreenFix(enableWhiteScreenFix);
		}
		if(data.has(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER))
		{
			publishOpenComposeChatOnceEvent(data, settings);
		}

		if(data.has(HikeConstants.MAX_RETRY_COUNT_MAPPS))
		{
			int newRetryCount = data.optInt(HikeConstants.MAX_RETRY_COUNT_MAPPS, HikePlatformConstants.MAPP_DEFAULT_RETRY_COUNT);
			if (newRetryCount < 1)
			{
				return;
			}

			Logger.d("Platform", "New Retry Count is : " + newRetryCount);

			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MAX_RETRY_COUNT_MAPPS, newRetryCount);
		}

		if(data.has(HikeConstants.CRASH_REPORTING_TOOL))
		{
			String crashRepT=data.optString(HikeConstants.CRASH_REPORTING_TOOL, HikeConstants.CRASHLYTICS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CRASH_REPORTING_TOOL,crashRepT);
		}
		if(data.has(HikeConstants.SINGLE_STICKER_CDN))
		{
			boolean singleStickerCdnEnabled = data.getBoolean(HikeConstants.SINGLE_STICKER_CDN);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SINGLE_STICKER_CDN, singleStickerCdnEnabled);
		}

		if (data.has(HikeConstants.STICKER_PACK_CDN))
		{
			boolean stickerPackCdnEnabled = data.getBoolean(HikeConstants.STICKER_PACK_CDN);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_PACK_CDN, stickerPackCdnEnabled);
		}

		if (data.has(HikeConstants.STICKER_PACK_CDN_THRESHOLD))
		{
			int stickerPackCdnThreshold = data.getInt(HikeConstants.STICKER_PACK_CDN_THRESHOLD);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_PACK_CDN_THRESHOLD, stickerPackCdnThreshold);
		}


		if (data.has(HikeConstants.ENABLE_AB_SYNC_CHANGE))
		{
			boolean enableABSyncChange = data.getBoolean(HikeConstants.ENABLE_AB_SYNC_CHANGE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_AB_SYNC_CHANGE, enableABSyncChange);
			if(enableABSyncChange)
			{
				long delayInSync = 5;
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.CONTACT_UPDATE_WAIT_TIME, delayInSync);
			}
			else
			{
				long delayInSync = 60;
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.CONTACT_UPDATE_WAIT_TIME, delayInSync);
			}
			ContactUtils.triggerSyncContacts(context);
		}

		if (data.has(HikeConstants.HIDE_DELETED_CONTACTS))
		{
			boolean hideDelContacts = data.getBoolean(HikeConstants.HIDE_DELETED_CONTACTS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.HIDE_DELETED_CONTACTS, hideDelContacts);
			ContactUtils.triggerSyncContacts(context);
		}

		if (data.has(HikeConstants.ENABLE_AB_SYNC_DEBUGING))
		{
			boolean enableABSyncDebug = data.getBoolean(HikeConstants.ENABLE_AB_SYNC_DEBUGING);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_AB_SYNC_DEBUGING, enableABSyncDebug);
			ContactUtils.triggerSyncContacts(context);
		}

		if (data.has(HikeConstants.NET_BLOCKED_STATE_ANALYTICS)) {
			boolean enableAnalytics = data.getBoolean(HikeConstants.NET_BLOCKED_STATE_ANALYTICS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NET_BLOCKED_STATE_ANALYTICS, enableAnalytics);
		}
	//1-->TWIN PRIME CLIENT,0-->OKCLIENT
		if (data.has(HikeConstants.TP_ENABLE)) {
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TP_ENABLE, data.optInt(HikeConstants.TP_ENABLE, 1));
		}
		if (data.has(HikeConstants.ADD_URL))
		{
			try
			{
				JSONArray array = data.getJSONArray(HikeConstants.ADD_URL);
				PlatformUtils.insertUrl(array);
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), e.toString());
			}
		}
		if (data.has(HikeConstants.DELETE_URL))
		{
			try
			{
				JSONArray array = data.getJSONArray(HikeConstants.DELETE_URL);
				PlatformUtils.deleteUrl(array);
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), e.toString());
			}

		}

		if(data.has(HikeConstants.SHOW_STICKER_PREVIEW))
		{
			boolean showStickerPreview = data.getBoolean(HikeConstants.SHOW_STICKER_PREVIEW);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_STICKER_PREVIEW, showStickerPreview);
		}

		if(data.has(HikeConstants.PROB_ACTIVITY_OPEN))
		{
			int prob=data.optInt(HikeConstants.PROB_ACTIVITY_OPEN,HikeConstants.DEFAULT_ACTIVITY_OPEN);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.PROB_ACTIVITY_OPEN,prob);
		}

		if (data.has(HikeConstants.JOURNAL_MODE_INDEX))
		{
			int journalModeIndex = data.getInt(HikeConstants.JOURNAL_MODE_INDEX);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.JOURNAL_MODE_INDEX, journalModeIndex);
		}
		if (data.has(HikeConstants.WT_1_REVAMP_ENABLED))
		{
			boolean enabled = data.getBoolean(HikeConstants.WT_1_REVAMP_ENABLED);
			editor.putBoolean(HikeConstants.WT_1_REVAMP_ENABLED, enabled);
		}
		if (data.has(HikeConstants.CUSTOM_CHATTHEME_ENABLED))
		{
			boolean enabled = data.getBoolean(HikeConstants.CUSTOM_CHATTHEME_ENABLED);
			editor.putBoolean(HikeConstants.CUSTOM_CHATTHEME_ENABLED, enabled);
		}
		if (data.has(HikeConstants.CUSTOM_CHATTHEME_DISABLE_OVERLAY))
		{
			boolean disabled = data.getBoolean(HikeConstants.CUSTOM_CHATTHEME_DISABLE_OVERLAY);
			editor.putBoolean(HikeConstants.CUSTOM_CHATTHEME_DISABLE_OVERLAY, disabled);
		}
		if (data.has(HikeConstants.LARGE_VIDEO_SHARING_ENABLED))
		{
			boolean enabled = data.getBoolean(HikeConstants.LARGE_VIDEO_SHARING_ENABLED);
			editor.putBoolean(HikeConstants.LARGE_VIDEO_SHARING_ENABLED, enabled);
		}
		if (data.has(HikeConstants.IS_NEW_USER))
		{
			boolean isNewUser = data.getBoolean(HikeConstants.IS_NEW_USER);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_NEW_USER, isNewUser);
		}

		if (data.has(HikeConstants.FTUE_FRIENDS_COUNT))
		{
			int newFriendsFtueCount = data.getInt(HikeConstants.FTUE_FRIENDS_COUNT);
			if (newFriendsFtueCount > 0) //Saving only a positive value
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FTUE_FRIENDS_COUNT, newFriendsFtueCount);
			}
		}
		if(data.has(HikePlatformConstants.FLUSH_DOWNLOAD_TABLE))
		{
			HikeContentDatabase.getInstance().flushPlatformDownloadStateTable();
		}

		if (data.has(HikeConstants.NUM_ROWS_INITIALLY_VISIBLE))
		{
			int numRowsInitiallyVisible = data.getInt(HikeConstants.NUM_ROWS_INITIALLY_VISIBLE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NUM_ROWS_INITIALLY_VISIBLE, numRowsInitiallyVisible);
		}

		if (data.has(HikeConstants.DISK_CACHE_SIZE))
		{
			long diskCacheSize = data.getLong(HikeConstants.DISK_CACHE_SIZE);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DISK_CACHE_SIZE, diskCacheSize);
		}

		if(data.has(HikeConstants.Shortcut.UPDATE))
		{
			updateShortCut(data);
		}

		if(data.has(HikeConstants.CONTACT_UPDATE))
		{
			saveContacts(data, false);
		}
		else if (data.has(HikeConstants.CONTACT_NUMBER_OLD))
		{
			saveContacts(data, true);
		}

		if(data.has(HikeConstants.SHOW_RECOMMENDED_PACKS))
		{
			boolean showRecommendedPacks = data.getBoolean(HikeConstants.SHOW_RECOMMENDED_PACKS);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_RECOMMENDED_PACKS, showRecommendedPacks);
		}

		if (data.has(HikeConstants.CHUNK_SIZE_POLICY))
		{
			int chunkSizePolicy = data.getInt(HikeConstants.CHUNK_SIZE_POLICY);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CHUNK_SIZE_POLICY, chunkSizePolicy);
		}

		if(data.has(HikeConstants.NUMBER_OF_ROWS_FOR_ORDER))
		{
			int orderRowsCount = data.optInt(HikeConstants.NUMBER_OF_ROWS_FOR_ORDER, StickerConstants.DEFAULT_NUMBER_OF_ROWS_FOR_ORDER);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.NUMBER_OF_ROWS_FOR_ORDER, orderRowsCount);
		}

		if(data.has(HikeConstants.PACK_UPDATION_PAGE_SIZE))
		{
			int packUpdationPageSize = data.optInt(HikeConstants.PACK_UPDATION_PAGE_SIZE, StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_UPDATION_METADATA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.PACK_UPDATION_PAGE_SIZE, packUpdationPageSize);
		}

		if(data.has(HikeConstants.PACK_CREATION_PAGE_SIZE))
		{
			int packCreationPageSize = data.optInt(HikeConstants.PACK_CREATION_PAGE_SIZE, StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_CREATION_METADATA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.PACK_CREATION_PAGE_SIZE, packCreationPageSize);
		}

		if(data.has(HikeConstants.FETCH_METADATA_PACK_COUNT))
		{
			int fetchMetadataPackCount = data.optInt(HikeConstants.FETCH_METADATA_PACK_COUNT, StickerConstants.DEFAULT_CATEGORIES_TO_FETCH_DATA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FETCH_METADATA_PACK_COUNT, fetchMetadataPackCount);
		}

		if(data.has(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE))
		{
			QuickStickerSuggestionController.getInstance().toggleQuickSuggestionOnReceive(data.optBoolean(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE));
		}

		if(data.has(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT))
		{
			QuickStickerSuggestionController.getInstance().toggleQuickSuggestionOnSent(data.optBoolean(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_SENT));
		}

		if(data.has(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL))
		{
			QuickStickerSuggestionController.getInstance().refreshQuickSuggestionTtl(data.optLong(HikeConstants.QUICK_SUGGESTED_STICKERS_TTL));
		}

		if (data.has(HikeConstants.ENABLE_BDAY_IN_CCA))
		{
			boolean showBdayInCCA = data.getBoolean(HikeConstants.ENABLE_BDAY_IN_CCA);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_BDAY_IN_CCA, showBdayInCCA);
		}

		if (data.has(HikeConstants.BDAY_HTTP_CALL_TIME_GAP))
		{
			long bdayHttpTimeGap = data.getLong(HikeConstants.BDAY_HTTP_CALL_TIME_GAP);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BDAY_HTTP_CALL_TIME_GAP, bdayHttpTimeGap);
		}

		if (data.has(HikeConstants.DISABLE_QUICK_UPLOAD))
		{
			boolean disableQuickUpload = data.getBoolean(HikeConstants.DISABLE_QUICK_UPLOAD);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DISABLE_QUICK_UPLOAD, disableQuickUpload);
		}

		editor.commit();
		this.pubSub.publish(HikePubSub.UPDATE_OF_MENU_NOTIFICATION, null);

	}

	private void updateShortCut(final JSONObject data) throws  JSONException
	{
		String msisdn = data.getString(HikeConstants.MSISDN);

		if(StealthModeManager.getInstance().isStealthMsisdn(msisdn))
		{
			return;
		}

		final ConvInfo info;
		if (BotUtils.isBot(msisdn))
		{
			info = BotUtils.getBotInfoForBotMsisdn(msisdn);
		}
		else
		{
			info = new ConvInfo.ConvInfoBuilder(msisdn).setConvName(ContactManager.getInstance().getName(msisdn)).build();
		}

		if (data.getString(HikeConstants.Shortcut.UPDATE).equals(HikeConstants.Shortcut.CREATE))
		{
			HikeHandlerUtil.getInstance().postRunnable(new Runnable()
			{
				@Override
				public void run()
				{
					Utils.createShortcut(context, info, data.optBoolean(HikeConstants.TOAST, false));
				}
			});
		}
	}

	private void saveContacts(JSONObject data, boolean makeAnEditCall) throws JSONException
	{
		String newMsisdn = data.optString(HikeConstants.CONTACT_UPDATE,"");
		String name = data.optString(HikeConstants.CONTACT_NAME, "");
		String oldMsisdn = data.optString(HikeConstants.CONTACT_NUMBER_OLD, "");

		if(!makeAnEditCall)
		{
			if(TextUtils.isEmpty(newMsisdn))
			{
				return;
			}
		boolean changeMsisdn = !TextUtils.isEmpty(oldMsisdn);

		Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(changeMsisdn? oldMsisdn : newMsisdn));
		PairModified<String, String> contactIdPair = Utils.doesContactContainHikeCustomPhoneType(context, contactUri);

		if(contactIdPair == null)
		{
			if(changeMsisdn)
			{
				contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(newMsisdn));
				contactIdPair = Utils.doesContactContainHikeCustomPhoneType(context, contactUri);
				if(contactIdPair != null)
				{
					return;
				}
			}
			List<AccountData> accountDatas = Utils.getAccountList(context);
			List<ContactInfoData> items = new ArrayList<ContactInfoData>();
			items.add(new ContactInfoData(ContactInfoData.DataType.PHONE_NUMBER, newMsisdn, HikeConstants.HIKE_CUSTOM_PHONE_TYPE));
			Utils.addToContacts(items, TextUtils.isEmpty(name) ? newMsisdn : name, context, (accountDatas.isEmpty()) ? null : accountDatas.get(0), false);
		}
		else
		{
			if(changeMsisdn)
			{
				Utils.updateContactWithHikeCustomPhoneType(context, contactIdPair.getFirst(), contactIdPair.getSecond(), newMsisdn);
			}
		}
		}
		else
		{
			if(TextUtils.isEmpty(oldMsisdn))
			{
				return;
			}
			Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(oldMsisdn));
			String oldName = data.optString("cntct_nm_old","");
			PairModified<String, String> contactIdPair = Utils.doesContactContainHikeCustomPhoneType(context, contactUri, oldName);
			if(contactIdPair != null)
			{
				Utils.updateNameWithHikeCustomPhoneType(context, contactIdPair.getFirst(), contactIdPair.getSecond(), TextUtils.isEmpty(name) ? oldMsisdn : name);
			}
		}

	}

	private void saveRewards(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

		int talkTime = data.getInt(HikeConstants.TALK_TIME);

		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
		editor.commit();

		this.pubSub.publish(HikePubSub.TALK_TIME_CHANGED, talkTime);
	}

	private void saveAction(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		if (data.optBoolean(HikeConstants.POST_AB))
		{
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			List<ContactInfo> contactinfos = null;
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HIDE_DELETED_CONTACTS, false))
			{
				contactinfos = ContactManager.getInstance().getContacts(this.context);
			}
			else
			{
				contactinfos = ContactManager.getInstance().getContactsOld(this.context);
			}
			if(contactinfos == null)
			{
				return;
			}
			Map<String, List<ContactInfo>> contacts = ContactManager.getInstance().convertToMap(contactinfos);
			try
			{
				new PostAddressBookTask(contacts).execute();
			}
			catch (IllegalStateException e)
			{
				Logger.w(getClass().getSimpleName(), "Exception while posting ab", e);
			}
		}
		if (data.optBoolean(HikeConstants.PUSH))
		{
			Editor editor = settings.edit();
			editor.putBoolean(HikeMessengerApp.GCM_ID_SENT, false);
			editor.commit();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.REGISTER_GCM_SIGNUP, HikeConstants.REGISTEM_GCM_AFTER_SIGNUP);
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));
		}
                if (data.optBoolean(HikeConstants.GCM_STALE_REGISTRATION_REFRESH))
                {
                    GCMRegistrar.unregister(context.getApplicationContext());
                }
		if (data.optBoolean(HikeConstants.DEFAULT_SMS_CLIENT_TUTORIAL))
		{
			setDefaultSMSClientTutorialSetting();
		}
		if (data.optBoolean(HikeConstants.POST_INFO))
		{
			Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, false);
			editor.commit();
			context.sendBroadcast(new Intent(HikeService.SEND_GB_DETAILS_TO_SERVER_ACTION));
		}
		// server on demand analytics data to be sent from client
		if (data.optBoolean(AnalyticsConstants.ANALYTICS))
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOADING FROM DEMAND PACKET ROUTE---");

			HAManager.getInstance().sendAnalyticsData(true, true);
		}

		if (data.optBoolean(HikeConstants.PATCH_AB))
		{
			byte contactSyncResult = ContactManager.getInstance().syncUpdates(context);
			Logger.d(getClass().getSimpleName(), "contacts sync result : " + contactSyncResult);
		}

		if (data.optBoolean(HikeConstants.LOG_SQLITE_PROPERTIES))
		{
			String journalMode = HikeConversationsDatabase.getInstance().getJournalMode();
			JSONObject json = new JSONObject();
			json.put(HikeConstants.JOURNAL_MODE, journalMode);
			GetSqliteVersionCommand getSqliteVersionCommand = new GetSqliteVersionCommand();
			String sqliteVersion = (String) getSqliteVersionCommand.execute();
			json.put(HikeConstants.SQLITE_VERSION, sqliteVersion);
			HAManager.getInstance().logDevEvent(AnalyticsConstants.DATABASE_AREA, AnalyticsConstants.SQLITE_PROPERTY, json);
		}
	}

	private void saveStatusUpdate(JSONObject jsonObj) throws JSONException
	{
		StatusMessage statusMessage = new StatusMessage(jsonObj);

		ContactManager conMgr = ContactManager.getInstance();

		JSONObject jsonData = jsonObj.getJSONObject(HikeConstants.DATA);
		/*
		 * This would be true for unsupported status message types. We should not be doing anything if we get one.
		 * 
		 * Also if the user is blocked, we ignore the message.
		 *
		 * Ignore if user is not two way friend. This is just for 2 way friends A/B testing.
		 */
		if (statusMessage.getStatusMessageType() == null
				|| conMgr.isBlocked(statusMessage.getMsisdn())
				|| (!conMgr.isTwoWayFriend(statusMessage.getMsisdn()))
				)
		{
			return;
		}
		/*
		 * Applying the offset.
		 */
		long timeStamp = Utils.applyServerTimeOffset(context, statusMessage.getTimeStamp());
		statusMessage.setTimeStamp(timeStamp);
		if (jsonObj.has(HikeConstants.TIMESTAMP))
		{
			/*
			 * We need to replace serverTimeOffsetApplied timestamp in jsonObject as well
			 */
			jsonObj.put(HikeConstants.TIMESTAMP, timeStamp);
		}

		ContactInfo contactInfo = conMgr.getContact(statusMessage.getMsisdn(), true, false);
		FavoriteType favoriteType = contactInfo.getFavoriteType();
		/*
		 * Only add updates to timeline for contacts that have a 2-way relationship with the user.
		 */
		long id = convDb.addStatusMessage(statusMessage, favoriteType == FavoriteType.FRIEND);

		if (id == -1)
		{
			Logger.d(getClass().getSimpleName(), "This status message was already added");
			return;
		}

		if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			String iconBase64 = (String) jsonData.remove(HikeConstants.THUMBNAIL);
			conMgr.setIcon(statusMessage.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);

			/**
			 * Problem: on DP upload, Two MQTT packets come IC + SU SO if SU comes first then in notification popped old DP was shown as inside cache previous DP was there which is
			 * corrected in IC packet
			 *
			 * But When IC packets comes first, then there was no problem, as DB was updated in it
			 *
			 * So via making extra DB call, this problem is solved
			 */
			conMgr.setIcon(statusMessage.getMsisdn(), Base64.decode(iconBase64, Base64.DEFAULT), false);
		}
		// Check if status message has image other than dp updates
		else if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
		{
			final String iconBase64 = jsonData.optString(HikeConstants.THUMBNAIL);
			final String fileKey = jsonData.optString(HikeConstants.SU_IMAGE_KEY);

			if (!TextUtils.isEmpty(fileKey) && !TextUtils.isEmpty(iconBase64))
			{
				// Add to fileThumbnail table viz. fk - thumb
				HikeConversationsDatabase.getInstance().addFileThumbnail(fileKey, Base64.decode(iconBase64, Base64.DEFAULT));
			}
			else
			{
				throw new JSONException("saveStatusUpdate() : Image SU doesnt contain fileKey or Thumbnail");
			}
		}

		statusMessage.setName(contactInfo.getNameOrMsisdn());

		if (favoriteType == FavoriteType.FRIEND)
		{
			incrementUnseenStatusCount();
			pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC || statusMessage.getStatusMessageType() == StatusMessageType.IMAGE
					|| statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
			{
				/*
				 * Start auto download of the profile image.
				 */
				if (!isBulkMessage) // do not autodownload in case of bulkmessage
				{
					Logger.d(DP_DOWNLOAD_TAG, "Received SU Packet, going to download");
					autoDownloadProfileImage(statusMessage, true);
				}
			}
		}
		else if (statusMessage.isHistoricalUpdate())
		{
			Logger.d(getClass().getSimpleName(), "(HSU but NOT FRIEND),... add fav after su, so not sent to pubsub");
		}

		pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);

		/**
		 * Add to Conv DB only if non historical update as they are not to be shown inside CT
		 */
		if (!statusMessage.isHistoricalUpdate())
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			ConvMessage convMessage = saveStatusMsg(jsonObj, msisdn);

			if (convMessage == null)
			{
				return;
			}

			convDb.setMessageIdForStatus(statusMessage.getMappedId(), convMessage.getMsgID());
		}
	}

	private void saveDeleteStatus(JSONObject jsonObj) throws JSONException
	{
		String statusId = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.STATUS_ID);
		pubSub.publish(HikePubSub.DELETE_STATUS, statusId);
	}

	private void savePostponeOrRemoveFavorite(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);
		removeOrPostponeFriendType(msisdn);
	}

	private void saveBatchStatusUpdate(JSONObject jsonObj) throws JSONException
	{
		/*
		 * Only proceed if the user has selected a batch update preference
		 */
		if (PreferenceManager.getDefaultSharedPreferences(context).getInt(HikeConstants.STATUS_PREF, 0) <= 0)
		{
			return;
		}

		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

		String header = data.getString(HikeConstants.BATCH_HEADER);
		String message = data.getString(HikeConstants.BATCH_MESSAGE);

		pubSub.publish(HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED, new Pair<String, String>(header, message));
	}

	private void saveSticker(JSONObject jsonObj) throws JSONException
	{
		String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		if (HikeConstants.ADD_STICKER.equals(subType))
		{
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			int stickerCount = data.optInt(HikeConstants.COUNT, -1);
			int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
			String description  = data.optString(HikeConstants.DESCRIPTION, null);
			JSONArray stickerArray = data.optJSONArray(HikeConstants.STICKER_LIST);
			String stickerListString = Utils.isEmpty(stickerArray) ? null : stickerArray.toString();
			StickerManager.getInstance().updateStickerCategoryData(categoryId, true, stickerCount, categorySize, description, stickerListString);
		}
		else if (HikeConstants.REMOVE_STICKER.equals(subType) || HikeConstants.REMOVE_CATEGORY.equals(subType))
		{
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			if (HikeConstants.REMOVE_CATEGORY.equals(subType))
			{
				StickerManager.getInstance().removeCategory(categoryId, true);
			}
			else
			{
				JSONArray stickerIds = data.getJSONArray(HikeConstants.STICKER_IDS);
				Set<String> removedStickerSet = new HashSet<String>();

				for (int i = 0; i < stickerIds.length(); i++)
				{
                    Sticker sticker = new Sticker(categoryId, stickerIds.getString(i));
					StickerManager.getInstance().removeSticker(sticker);
					removedStickerSet.add(sticker.getStickerCode());
				}
				int stickerCount = data.optInt(HikeConstants.COUNT, -1);
				int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
				JSONArray stickerArray = data.optJSONArray(HikeConstants.STICKER_LIST);

				/*
				 * We should not update updateAvailable field in this case
				 */
				String stickerListString = Utils.isEmpty(stickerArray) ? null : stickerArray.toString();
				StickerManager.getInstance().updateStickerCategoryData(categoryId, null, stickerCount, categorySize, null, stickerListString);
				// Remove tags being used for sticker search w.r.t. deleted stickers here
				StickerManager.getInstance().removeTagForDeletedStickers(removedStickerSet);
			}
		}
		else if (HikeConstants.SHOP.equals(subType))
		{
			boolean showBadge = data.optBoolean(HikeConstants.BADGE, false);
			/*
			 * reseting sticker shop update time so that next time we fetch a fresh sicker shop
			 */
			StickerManager.getInstance().resetStickerShopLastUpdateTime();
			if (showBadge)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, true);
			}
		}

		else if (HikeConstants.ADD_CATEGORY.equals(subType))
		{
			if ((!data.has(StickerManager.CATEGORY_ID)) || (!data.has(HikeConstants.CAT_NAME)))
			{
				/**
				 * We are returning if we don't find category Id or Category name in the MQTT packet.
				 */

				Logger.d("SaveSticker", "Did not receive category Id and category Name. Returning");
				return;
			}

			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			String categoryName = data.getString(HikeConstants.CAT_NAME);
			int stickerCount = data.optInt(HikeConstants.COUNT, -1);
			int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
			int position = data.optInt(HikeConstants.PALLETE_POSITION, -1);
			String description = data.optString(HikeConstants.DESCRIPTION, null);
			JSONArray stickerArray = data.optJSONArray(HikeConstants.STICKER_LIST);
			String stickerListString = Utils.isEmpty(stickerArray) ? null : stickerArray.toString();

			/**
			 * Creating the sticker object here
			 */
			int pos = (position < 1 ? (HikeConversationsDatabase.getInstance().getMaxStickerCategoryIndex() + 1) : position);
			pos = (pos < 1 ? StickerManager.DEFAULT_POSITION : pos);

			StickerCategory stickerCategory = new StickerCategory.Builder()
					.setCategoryId(categoryId)
					.setCategoryName(categoryName)
					.setCategorySize(categorySize == -1 ? 0 : categorySize)
					.setCategoryDesc(description)
					.setTotalStickers(stickerCount == -1 ? 0 : stickerCount)
					.setUpdateAvailable(true)									//To show the green badge on category
					.setIsVisible(true)											//To make it visible in palette
					.setIsDownloaded(true)										// to be treated as downloaded
					.setCatIndex(pos)											//Choosing it's index based on the above logic
					.setState(StickerCategory.NONE)
					.setAllStickerListString(stickerListString)
					.build();
			StickerManager.getInstance().addNewCategoryInPallete(stickerCategory);
		}
		else if (HikeConstants.STICKER_FORCE_DOWNLOAD.equals(subType))
		{
			boolean badge = data.optBoolean(HikeConstants.BADGE, false);
			if(badge)
			{
				StickerSearchManager.getInstance().downloadForcedStickers();
			}
		}
		else if (HikeConstants.MINI_STICKER_ENABLED.equals(subType))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.MINI_STICKER_ENABLED, data.optBoolean(HikeConstants.BADGE, false));
		}
		else if (HikeConstants.DISPLAY_MINI_IN_CT.equals(subType))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DISPLAY_MINI_IN_CT, data.optBoolean(HikeConstants.BADGE, false));
		}
		else if (HikeConstants.UNDOWNLOADED_DATA.equals(subType))
		{

			HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_VISIBLE_IN_RECO_COUNT, data.optInt(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_VISIBLE_IN_RECO_COUNT, 0));
			HikeSharedPreferenceUtil.getInstance().saveData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_CACHE_LIMIT, data.optInt(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_CACHE_LIMIT, 0));
		}
		else if (HikeConstants.LOG_EMOTICON_USAGE_SWITCH.equals(subType))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LOG_EMOTICON_USAGE_SWITCH, data.optBoolean(HikeConstants.BADGE, false));
		}
        else if(HikeConstants.STICKER_SIZE.equals(subType))
        {
            int stickerSize = data.optInt(HikeConstants.STICKER_SIZE,-1);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SIZE, stickerSize);
        }
        else if(HikeConstants.SHOW_STICKER_SHOP_SEARCH_FTUE_LIMIT.equals(subType))
        {
            int limit = data.optInt(HikeConstants.LIMIT_KEY, HikeConstants.DEFAULT_SEARCH_FTUE_LIMIT);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_STICKER_SHOP_SEARCH_FTUE_LIMIT, limit);
        }
        else if(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE.equals(subType))
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STICKER_SHOP_SEARCH_TOGGLE, data.optBoolean(HikeConstants.BADGE, false));
        }
        else if(CategorySearchManager.SHOP_SEARCH_WEIGHTS.equals(subType))
        {
            String weightString = data.optString(HikeConstants.STICKER_SCORE_WEIGHTAGE, CategorySearchManager.DEFAULT_WEIGHTS_INPUT);
            HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchManager.SHOP_SEARCH_WEIGHTS, weightString);
        }
        else if(CategorySearchManager.SEARCH_RESULTS_LIMIT.equals(subType))
        {
            int limit = data.optInt(HikeConstants.LIMIT_KEY, CategorySearchManager.DEFAULT_SEARCH_RESULTS_LIMIT);
            HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchManager.SEARCH_RESULTS_LIMIT, limit);
        }
        else if(CategorySearchManager.SEARCH_QUERY_LENGTH_THRESHOLD.equals(subType))
        {
            int limit = data.optInt(HikeConstants.LIMIT_KEY, CategorySearchManager.DEFAULT_SEARCH_QUERY_LENGTH_THRESHOLD);
            HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchManager.SEARCH_QUERY_LENGTH_THRESHOLD, limit);
        }
        else if(CategorySearchManager.AUTO_SEARCH_TIME.equals(subType))
        {
            long limit = data.optLong(HikeConstants.LIMIT_KEY, CategorySearchManager.DEFAULT_AUTO_SEARCH_TIME);
            HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchManager.AUTO_SEARCH_TIME, limit);
        }
        else if(CategorySearchManager.SEARCH_RESULTS_LOG_LIMIT.equals(subType))
        {
            int limit = data.optInt(HikeConstants.LIMIT_KEY, CategorySearchManager.DEFAULT_SEARCH_RESULTS_LOG_LIMIT);
            HikeSharedPreferenceUtil.getInstance().saveData(CategorySearchManager.SEARCH_RESULTS_LOG_LIMIT, limit);
        }
	}

	private void saveBulkLastSeen(JSONObject jsonObj) throws JSONException
	{
		Utils.handleBulkLastSeenPacket(context, jsonObj);
	}

	private void saveLastSeen(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		long lastSeenTime = data.getLong(HikeConstants.LAST_SEEN);
		int isOffline;
		HAManager.getInstance().recordLastSeenEvent(MqttMessagesManager.class.getName(), "saveLastSeen", null, msisdn);
		/*
		 * Apply offset only if value is greater than 0
		 */
		if (lastSeenTime > 0)
		{
			isOffline = 1;
			lastSeenTime = Utils.applyServerTimeOffset(context, lastSeenTime);
		}
		else
		{
			/*
			 * Otherwise the last seen time notifies that the user is either online or has turned the setting off.
			 */
			isOffline = (int) lastSeenTime;
			lastSeenTime = System.currentTimeMillis() / 1000;
		}

		ContactManager.getInstance().updateLastSeenTime(msisdn, lastSeenTime);
		ContactManager.getInstance().updateIsOffline(msisdn, (int) isOffline);
		HAManager.getInstance().recordLastSeenEvent(MqttMessagesManager.class.getName(), "saveLastSeen", "updated CM", msisdn);
		ContactInfo contact = ContactManager.getInstance().getContact(msisdn, true, true);
		pubSub.publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contact);
	}

	private void saveServerTimestamp(JSONObject jsonObj) throws JSONException
	{
		long serverTimestamp = jsonObj.getLong(HikeConstants.TIMESTAMP);
		long diff = (System.currentTimeMillis() / 1000) - serverTimestamp;
		Logger.d(getClass().getSimpleName(), "Diff b/w server and client: " + diff);
		Editor editor = settings.edit();
		editor.putLong(HikeMessengerApp.SERVER_TIME_OFFSET, diff);

		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		long serverTimestampInMsec = serverTimestamp * 1000 + data.getLong(HikeConstants.TIMESTAMP_MILLIS);
		long diffInMsec = System.currentTimeMillis() - serverTimestampInMsec;
		Logger.d(getClass().getSimpleName(), "Diff b/w server and client in msec : " + diffInMsec);
		editor.putLong(HikeMessengerApp.SERVER_TIME_OFFSET_MSEC, diffInMsec);
		editor.commit();
	}

	private void saveProtip(JSONObject jsonObj) throws JSONException
	{
		// We should delete the last showing pro tip from the DB, we don't
		// need it anymore.
		// As per the last request from growth team, we don't need to show
		// the older pro tips once the latest pro tips come in.
		long currentProtipId = -1l;

		// Defensive check. TODO Remove protip code from application.
		try
		{
			currentProtipId = settings.getLong(HikeMessengerApp.CURRENT_PROTIP, -1l);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		boolean isValidProtip = false;

		Protip protip = new Protip(jsonObj);
		// check upfront if this protip is a valid protip
		if (protip != null && currentProtipId != protip.getId())
		{
			isValidProtip = true;
		}
		// only if its a valid protip, proceed with the display
		if (isValidProtip)
		{

			/*
			 * Applying the offset.
			 */
			protip.setTimeStamp(Utils.applyServerTimeOffset(context, protip.getTimeStamp()));
			long id = convDb.addProtip(protip);
			if (id == -1)
			{
				Logger.d(getClass().getSimpleName(), "Error adding this protip");
				return; // for some reason the insertion failed,
			}
			// delete all pro tips before these.
			// we dont need them anymore.

			convDb.deleteAllProtipsBeforeThisId(id);
			protip.setId(id);
			Editor editor = settings.edit();
			editor.putLong(HikeMessengerApp.CURRENT_PROTIP, protip.getId());
			editor.commit();
			String iconBase64 = jsonObj.getJSONObject(HikeConstants.DATA).optString(HikeConstants.THUMBNAIL);
			if (!TextUtils.isEmpty(iconBase64))
			{
				// ContactManager.getInstance().setIcon(protip.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
				HikeImageWorker.doContactManagerIconChange(protip.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
			}
			// increment the unseen status count straight away.
			// we've got a new pro tip.
			incrementUnseenStatusCount();

			StatusMessage statusMessage = new StatusMessage(protip);
			// download the protip only if the URL is non empty
			// also respect the user's auto photo download setting.
			if (!TextUtils.isEmpty(protip.getImageURL())
					&& ((FTUtils.getNetworkType(context) == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true)) || (FTUtils.getNetworkType(context) != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true))))
			{
				autoDownloadProtipImage(statusMessage, true);
			}
			pubSub.publish(HikePubSub.PROTIP_ADDED, protip);
		}
	}

	private void saveUpdatePush(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		String devType = data.optString(HikeConstants.DEV_TYPE);
		String id = data.optString(HikeConstants.MESSAGE_ID);
		String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_UPDATE_PACKET_ID, "");
		if (!TextUtils.isEmpty(devType) && devType.equals(HikeConstants.ANDROID) && !TextUtils.isEmpty(id) && !lastPushPacketId.equals(id))
		{
			String version = data.optString(HikeConstants.UPDATE_VERSION);
			String updateURL = data.optString(HikeConstants.Extras.URL);
			int update = Utils.isUpdateRequired(version, context) ? (data.optBoolean(HikeConstants.CRITICAL_UPDATE_KEY) ? HikeConstants.CRITICAL_UPDATE
					: HikeConstants.NORMAL_UPDATE) : HikeConstants.NO_UPDATE;
			if ((update == HikeConstants.CRITICAL_UPDATE || update == HikeConstants.NORMAL_UPDATE))
			{
				if (Utils.isUpdateRequired(version, context))
				{
					Editor editor = settings.edit();
					editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, update);
					editor.putString(HikeConstants.Extras.UPDATE_MESSAGE, data.optString(HikeConstants.MESSAGE));
					editor.putString(HikeConstants.Extras.LATEST_VERSION, version);
					editor.putString(HikeConstants.Extras.LAST_UPDATE_PACKET_ID, id);

					if (!TextUtils.isEmpty(updateURL))
						editor.putString(HikeConstants.Extras.URL, updateURL);
					editor.commit();
					this.pubSub.publish(HikePubSub.UPDATE_PUSH, update);
				}
			}
		}
	}

	private void saveApplicationsPush(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		String id = data.optString(HikeConstants.MESSAGE_ID);
		String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_APPLICATION_PUSH_PACKET_ID, "");
		String devType = data.optString(HikeConstants.DEV_TYPE);
		String message = data.optString(HikeConstants.MESSAGE);
		String packageName = data.optString(HikeConstants.PACKAGE);

		if (!TextUtils.isEmpty(devType) && devType.equals(HikeConstants.ANDROID) && !TextUtils.isEmpty(message) && !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(id)
				&& !lastPushPacketId.equals(id))
		{
			Editor editor = settings.edit();
			editor.putString(HikeConstants.Extras.APPLICATIONSPUSH_MESSAGE, data.optString(HikeConstants.MESSAGE));
			editor.putString(HikeConstants.Extras.LAST_APPLICATION_PUSH_PACKET_ID, id);
			editor.commit();
			this.pubSub.publish(HikePubSub.APPLICATIONS_PUSH, packageName);
		}
	}

	public void flushNotifOrTip(String type)
	{
		Editor editor = settings.edit();

		if (type.equals(HikeConstants.PERSISTENT_NOTIFICATION))
		{
			editor.putBoolean(HikeConstants.SHOULD_SHOW_PERSISTENT_NOTIF, false);
			editor.commit();
			this.pubSub.publish(HikePubSub.FLUSH_PERSISTENT_NOTIF, null);
		}
		else if (type.equals(HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE))
		{
			editor.putBoolean(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, false);
			editor.commit();
			this.pubSub.publish(HikePubSub.REMOVE_TIP, ConversationTip.UPDATE_CRITICAL_TIP);
		}

	}

	public void saveChatBackground(JSONObject jsonObj) throws JSONException
	{
		if(saveChatThemeSignals(jsonObj))
		{
			return;
		}
		String from = jsonObj.optString(HikeConstants.FROM);
		String to = jsonObj.optString(HikeConstants.TO);

		long timestamp = jsonObj.optLong(HikeConstants.TIMESTAMP);
		timestamp = Utils.applyServerTimeOffset(context, timestamp);

		boolean isGroupConversation = false;
		if (!TextUtils.isEmpty(to))
		{
			isGroupConversation = OneToNConversationUtils.isGroupConversation(to);
		}
		String id = isGroupConversation ? to : from;

		Pair<String, Long> chatThemedata = convDb.getChatThemeIdAndTimestamp(id);

		if (chatThemedata != null)
		{
			long oldTimestamp = chatThemedata.second;
			if (oldTimestamp > timestamp)
			{
				/*
				 * We should ignore this packet since its either old or duplicate.
				 */
				return;
			}
			else if (oldTimestamp == timestamp)
			{
				JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
				String bgId = data.optString(HikeConstants.BG_ID);

				if (bgId.equals(chatThemedata.first))
				{
					/*
					 * Duplicate theme.
					 */
					return;
				}
			}
		}

		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		String bgId = data.optString(HikeConstants.BG_ID);

		try
		{
			/*
			 * If this is a custom theme, we should show it as not supported.
			 */
			if (data.optBoolean(HikeConstants.CUSTOM)) {
				//throw new IllegalArgumentException();
				//putting a request to downlaod the asset ids for the theme
				ChatThemeManager.getInstance().downloadThemeAssetsMetadata(bgId, true);
			}else {
				String chatThemeId = bgId;
				this.pubSub.publish(HikePubSub.CHAT_BACKGROUND_CHANGED, new Pair<String, String>(id, bgId));
			}
			convDb.setChatBackground(id, bgId, timestamp);
			saveStatusMsg(jsonObj, id);
		}
		catch (IllegalArgumentException e)
		{
			/*
			 * This exception is thrown for unknown themes. Show an unsupported message Now in this case, we don't do anything. if user doesn't have certain theme that chatthread
			 * will keep on current applied theme.
			 */
		}
	}

	private boolean saveChatThemeSignals(JSONObject jsonObj)
	{
		try
		{
			if(jsonObj.has(HikeConstants.SUB_TYPE)) {
				String type = jsonObj.getString(HikeConstants.SUB_TYPE);
				if (type.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_NEW_THEME))//New Theme Signal
				{
					JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
					JSONArray themeData = data.getJSONArray(HikeChatThemeConstants.JSON_SIGNAL_THEME_DATA);
					//TODO CHATTHEME, Enable if it OTA Themes
					//ChatThemeManager.getInstance().processNewThemeSignal(themeData, false);
					return true;
				} else if (type.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_DEL_THEME))//Delete Theme Packet
				{
					JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
					ChatThemeManager.getInstance().processDeleteThemeSignal(data);
					return true;
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private void saveGroupOwnerChange(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.getString(HikeConstants.TO);
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		String msisdn = data.getString(HikeConstants.MSISDN);

		if (msisdn.equalsIgnoreCase(userMsisdn))
		{
			this.convDb.changeGroupSettings(groupId, -1, 1, new ContentValues());
		}
		else
		{

			if (this.convDb.setParticipantAdmin(groupId, msisdn) > 0)
			{
				ContactManager.getInstance().setParticipantAdmin(groupId, msisdn);
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.GROUP_OWNER_CHANGE, jsonObj);
	}

	private void saveRequestDP(JSONObject jsonObj) throws JSONException
	{
		final String groupId = jsonObj.getString(HikeConstants.TO);
		OneToNConversationUtils.uploadGroupProfileImage(groupId);
	}

	private void savePopup(JSONObject jsonObj) throws JSONException
	{
		String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
		if (subType.equals(HikeConstants.SHOW_STEALTH_POPUP))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String id = data.optString(HikeConstants.MESSAGE_ID);
			String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_STEALTH_POPUP_ID, "");

			if (!TextUtils.isEmpty(id))
			{
				if (lastPushPacketId.equals(id))
				{
					Logger.d(getClass().getSimpleName(), "Duplicate popup packet ! Gotcha");
					return;
				}
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "Returning with empty packet Id");
				return; // empty packet id : ignore this packet
			}

			String header = data.optString(HikeConstants.HEADER, context.getString(R.string.stealth_unread_tip_header));
			String body = data.optString(HikeConstants.BODY, context.getString(R.string.stealth_unread_tip_message));

			if (data.optBoolean(HikeConstants.ANIMATE, false))
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE, true);
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
			}
			else if (!TextUtils.isEmpty(header) && !TextUtils.isEmpty(body))
			{
				Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.STEALTH_UNREAD_TIP_HEADER, header);
				editor.putString(HikeMessengerApp.STEALTH_UNREAD_TIP_MESSAGE, body);
				editor.putBoolean(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, true);
				editor.putString(HikeMessengerApp.LAST_STEALTH_POPUP_ID, id);
				editor.commit();

				if (data.optBoolean(HikeConstants.PUSH, true)) // Toast this only if the push flag is true
				{
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.STEALTH_PUSH_BODY, body);
					bundle.putString(HikeConstants.Extras.STEALTH_PUSH_HEADER, header);
					this.pubSub.publish(HikePubSub.STEALTH_POPUP_WITH_PUSH, bundle);
				}
			}
		}
		else if (subType.equals(HikeConstants.HOLI_POPUP))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_FESTIVE_POPUP, FestivePopup.HOLI_POPUP);
		}
		else if (HikeConstants.PLAY_NOTIFICATION.equals(subType))
		{
			playNotification(jsonObj);
		}
		else if (subType.equals(HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);

			if (data != null)
			{
				if (data.has(HikeConstants.FLUSH))
				{
					if (data.optBoolean(HikeConstants.FLUSH, false))
					{
						flushNotifOrTip(subType);
					}
				}
				else
				{
					performUpdateTask(data, false);
				}
			}

		}
		else if (subType.equals(HikeConstants.INVITE_TIP))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			if (data != null)
			{
				Editor editor = settings.edit();
				editor.putBoolean(HikeConstants.SHOW_INVITE_TIP, true);
				editor.putString(HikeConstants.INVITE_TIP_HEADER, data.optString(HikeConstants.HEADER, ""));
				editor.putString(HikeConstants.INVITE_TIP_BODY, data.optString(HikeConstants.BODY, ""));
				editor.putString(HikeConstants.INVITE_TIP_LABEL, data.optString(HikeConstants.LABEL, ""));
				editor.putString(HikeConstants.INVITE_TIP_DISMISS, data.optString(HikeConstants.DISMISS, ""));
				editor.putString(HikeConstants.INVITE_TIP_BG_COLOR, data.optString(HikeConstants.BACKGROUND_COLOR, ""));
				editor.commit();
			}

		}
		else if (subType.equals(HikeConstants.PERSISTENT_NOTIFICATION))
		{

			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			if (data != null)
			{
				if (data.has(HikeConstants.FLUSH))
				{
					if (data.optBoolean(HikeConstants.FLUSH))
					{
						flushNotifOrTip(subType);
					}
				}
				else
				{
					String version = data.optString(HikeConstants.UPDATE_VERSION, "");
					String updateURL = data.optString(HikeConstants.Extras.URL, "");

					if (Utils.isUpdateRequired(version, context))
					{
						Editor editor = settings.edit();
						editor.putBoolean(HikeConstants.SHOULD_SHOW_PERSISTENT_NOTIF, true);
						editor.putBoolean(HikeConstants.IS_PERS_NOTIF_ALARM_SET, false);
						editor.putString(HikeConstants.Extras.UPDATE_MESSAGE, data.optString(HikeConstants.MESSAGE, ""));
						Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Target version for persistent notif:" + version);
						editor.putString(HikeConstants.Extras.LATEST_VERSION, version);
						editor.putString(HikeConstants.UPDATE_TITLE, data.optString(HikeConstants.Extras.TITLE, ""));
						editor.putString(HikeConstants.UPDATE_ACTION, data.optString(HikeConstants.MqttMessageTypes.ACTION, ""));
						editor.putString(HikeConstants.UPDATE_LATER, data.optString(HikeConstants.DISMISS, ""));
						editor.putLong(HikeConstants.UPDATE_ALARM, data.optLong(HikeConstants.PERSISTENT_NOTIF_ALARM_INTERVAL, HikeConstants.PERS_NOTIF_ALARM_DEFAULT));

						if (!TextUtils.isEmpty(updateURL))
							editor.putString(HikeConstants.Extras.URL, updateURL);
						editor.commit();
						this.pubSub.publish(HikePubSub.SHOW_PERSISTENT_NOTIF, null);
					}
				}
			}

		}
		else if(subType.equals(HikeConstants.MqttMessageTypes.TIP))
		{
			saveOrFlushAtomicTip(jsonObj);
		}
		else
		{
			// updatePopUpData
			updateAtomicPopUpData(jsonObj);
		}
	}

	public void performUpdateTask(JSONObject data, boolean isApkDownloaded)
	{
		boolean isCritical = data.optBoolean(HikeConstants.CRITICAL_UPDATE_KEY, false);
		String version = data.optString(HikeConstants.UPDATE_VERSION, isApkDownloaded ?
				HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.NEW_APK_VERSION, "") : "");

		int update = Utils.isUpdateRequired(version, context) ? ((isCritical || isApkDownloaded) ? HikeConstants.CRITICAL_UPDATE
				: HikeConstants.NORMAL_UPDATE) : HikeConstants.NO_UPDATE;
		if(update == HikeConstants.CRITICAL_UPDATE || update == HikeConstants.NORMAL_UPDATE)
		{
			Editor editor = settings.edit();
			editor.putBoolean(HikeConstants.SHOW_CRITICAL_UPDATE_TIP, isCritical || isApkDownloaded);
			editor.putBoolean(HikeConstants.SHOW_NORMAL_UPDATE_TIP, !isCritical && !isApkDownloaded);
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Target version for update tip:"+version);
			editor.putString(HikeConstants.Extras.LATEST_VERSION, version);
			editor.putString(HikeConstants.UPDATE_TIP_HEADER, data.optString(HikeConstants.HEADER, ""));
			editor.putString(HikeConstants.UPDATE_TIP_BODY, data.optString(HikeConstants.BODY, ""));
			editor.putString(HikeConstants.UPDATE_TIP_LABEL, data.optString(HikeConstants.LABEL, ""));
			editor.putString(HikeConstants.UPDATE_TIP_DISMISS, data.optString(HikeConstants.DISMISS, ""));
			editor.putString(HikeConstants.UPDATE_TIP_BG_COLOR, data.optString(HikeConstants.BACKGROUND_COLOR, ""));
			editor.putBoolean(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK, isApkDownloaded);

			String updateURL = data.optString(HikeConstants.Extras.URL, "");
			if (!TextUtils.isEmpty(updateURL) && !isApkDownloaded)
				editor.putString(HikeConstants.Extras.URL, updateURL);
			editor.commit();
			if(isApkDownloaded)
			{
				this.pubSub.publish(HikePubSub.UPDATE_PUSH, update);
			}

		}
	}
	
	private void playNotification(JSONObject jsonObj)
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		if (data != null)
		{
			int hash = data.toString().hashCode();
			// it is safety check, it is possible that server sends same packet twice (we have seen cases in GCM)
			// we are dependent upon in memory hash of last packet.
			if (lastNotifPacket != hash)
			{
				lastNotifPacket = hash;
				String body = PlatformUtils.getNotifBody(data);
				String destination = data.optString("u");
				boolean silent = data.optBoolean(HikeConstants.SILENT, true);
				boolean rearrangeChat = data.optBoolean(HikeConstants.REARRANGE_CHAT, false);
				// This field is valid only for non messaging bots
				boolean updateUnreadCount = data.optBoolean(HikeConstants.UPDATE_UNREAD_COUNT, false);
				JSONObject metadata = data.has(HikeConstants.METADATA) ? data.optJSONObject(HikeConstants.METADATA) : new JSONObject();
				Logger.i("mqttMessageManager", "Play Notification packet from Server " + data.toString());
				// chat thread -- by default silent is true, so no sound
				// open respective chat thread

				if (BotUtils.isBot(destination))
				{
					BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(destination);
					if (botInfo.isNonMessagingBot())
					{
						if (ContactManager.getInstance().isBlocked(destination))
						{
							return;
						}

						String hikeMessage = metadata.optString(HikePlatformConstants.HIKE_MESSAGE);
						if (!TextUtils.isEmpty(hikeMessage))
						{
							convDb.updateLastMessageForNonMessagingBot(destination, hikeMessage);
							// Saving lastConvMessage in memory as well to refresh the UI
							botInfo.setLastConversationMsg(Utils.makeConvMessage(destination, hikeMessage, true, State.RECEIVED_UNREAD));
						}

						Utils.rearrangeChat(destination, rearrangeChat, updateUnreadCount);

						if (!Utils.isConversationMuted(destination) && data.optBoolean(HikeConstants.PUSH, true))
						{

							if(data.has(HikePlatformConstants.HIKE_AFFINITY) && !data.optBoolean(HikePlatformConstants.HIKE_AFFINITY))
							{
								HikeNotification.getInstance().showPlatformNotification(data, jsonObj.optString(HikeConstants.FROM));
							}
							else
							{
								generateNotification(body, destination, silent);
							}
						}

						// to update the badge counter
						HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_MESSAGE_CHANGED, null);

						String notifData = metadata.optString(HikePlatformConstants.NOTIF_DATA);
						if (!TextUtils.isEmpty(notifData))
						{
							convDb.updateNotifDataForMicroApps(destination, notifData);

							HikeMessengerApp.getPubSub().publish(HikePubSub.NOTIF_DATA_RECEIVED, botInfo);
						}
					}
					else
					{
						if (data.optBoolean(HikeConstants.PUSH, true) && !TextUtils.isEmpty(destination) && !TextUtils.isEmpty(body))
						{

							if (ContactManager.getInstance().isBlocked(destination))
							{
								blockedMessageAnalytics(HikePlatformConstants.NOTIF);
								return;
							}
							String contentId = metadata.optString(HikePlatformConstants.CONTENT_ID);
							String nameSpace = metadata.optString(HikePlatformConstants.NAMESPACE);

							if (convDb.isContentMessageExist(destination, contentId, nameSpace))
							{
								if (!Utils.isConversationMuted(destination))
								{
									Utils.rearrangeChat(destination, rearrangeChat, updateUnreadCount);
								}

								generateNotification(body, destination, silent);

							}
						}
					}
				}
				else
				{
					Logger.e("mqtt", "msisdn is not that of a bot . Msisdn is----->" + destination);
				}

			}
			else
			{
				Logger.e("mqttMessageManager", "duplicate Notification packet from server " + data.toString());
			}
		}
	}

	private void generateNotification(String body, String destination, boolean silent)
	{

		HikeNotification.getInstance().notifyStringMessage(destination, body, silent, NotificationType.OTHER);

	}

	private void blockedMessageAnalytics(String type)
	{
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BLOCKED_MESSAGE);
			metadata.put(HikeConstants.TYPE, type);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, HikeConstants.LogEvent.GCM_ANALYTICS_CONTEXT, metadata);
	}

	private void saveTip(JSONObject jsonObj)
	{
		String subType = jsonObj.optString(HikeConstants.SUB_TYPE);
		if (subType.equals(HikeConstants.STICKER))
		{
			Editor editor = settings.edit();
			if (!settings.contains(HikeMessengerApp.SHOWN_EMOTICON_TIP))
				editor.putBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false);
			editor.commit();
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_FTUE_TIP, null);
		}
	}

	/**
	 * <br>
	 * This function handles bulk packet</br>
	 *
	 * @param bulkObj
	 *            - bulk json object of type "bm"
	 * @throws JSONException
	 */

	public void saveBulkMessage(JSONObject bulkObj) throws JSONException
	{
		boolean shouldFallBackToNormal = false;
		JSONObject bulkMessages = bulkObj.optJSONObject(HikeConstants.DATA);

		if (bulkMessages != null)
		{
			JSONArray msgArray = bulkMessages.optJSONArray(HikeConstants.MESSAGES);
			if (null != msgArray && msgArray.length() > 0)
			{
				isBulkMessage = true;
				int i = 0;
				int length = msgArray.length();
				Logger.d("bulkpacket", "length of json array : " + length);

				Logger.d("BulkProcess", "started");
				long time1 = System.currentTimeMillis();

				/*
				 * Initialize all the datastructures used for bulk packet processing
				 */
				messageList = new LinkedList<ConvMessage>(); // it will store all the convMessage object that can be added to list in one transaction
				messageListMap = new HashMap<String, LinkedList<ConvMessage>>(); // it will store list of conversation objects based on msisdn
				messageStatusMap = new HashMap<String, PairModified<PairModified<Long, Set<String>>, Long>>(); // it will store pair mapping to msisdn. pair first value is a pair
				// which contains max "mr" msgid and msisdns of participants that
				// read it. // pair second value is max "dr" message id

				try
				{
					ContactManager.getInstance().getWritableDatabase().beginTransaction();
					convDb.initializeIfRequiredAndGetWriteDatabase().beginTransaction();

					while (i < length)
					{
						JSONObject jsonObj = msgArray.optJSONObject(i++);
						if (jsonObj != null)
						{
							saveMqttMessage(jsonObj);
						}
					}
					Logger.d("BulkProcess", "going on");
					finalProcessing();
					convDb.initializeIfRequiredAndGetWriteDatabase().setTransactionSuccessful();
					ContactManager.getInstance().getWritableDatabase().setTransactionSuccessful();
				}
				catch (JSONException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					Logger.e("BulkProcessor", "Exception during processing ", e);
					shouldFallBackToNormal = true; // fallback to one message processing
				}
				finally
				{
					convDb.initializeIfRequiredAndGetWriteDatabase().endTransaction();
					ContactManager.getInstance().getWritableDatabase().endTransaction();

					Logger.d("BulkProcess", "stopped");
					isBulkMessage = false;
					Logger.d("bulkPacket", "total time : " + (System.currentTimeMillis() - time1));
				}

				/*
				 * If there is some exception processing bulk packet we fallback to normal single message processing of bulk packet messages
				 */
				if (shouldFallBackToNormal)
				{
					i = 0;
					while (i < length)
					{
						JSONObject jsonObj = msgArray.optJSONObject(i++);
						if (jsonObj != null)
						{
							saveMqttMessage(jsonObj);
						}
					}
				}

			}
		}
	}

	private void finalProcessing() throws JSONException
	{

		/*
		 * The list returned by {@link HikeConversationsDatabase#addConversationsBulk(List<ConvMessages>)} contains non duplicate messages This list is used for further processing
		 */

		if (messageList.size() > 0)
		{
			messageList = convDb.addConversationsBulk(messageList);
		}

		/*
		 * lastPinMap is map of msisdn to a pair containing last pin message for a conversation and count of total number of pin messages in bulk packet for that conversation
		 */
		HashMap<String, PairModified<ConvMessage, Integer>> lastPinMap = new HashMap<String, PairModified<ConvMessage, Integer>>();

		for (ConvMessage convMessage : messageList)
		{
			String msisdn = convMessage.getMsisdn();
			if (messageListMap.get(msisdn) == null)
			{
				messageListMap.put(msisdn, new LinkedList<ConvMessage>());
			}
			messageListMap.get(msisdn).add(convMessage); // adds each message into messageListMap according to msisdn

			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				if (lastPinMap.get(msisdn) == null)
				{
					lastPinMap.put(msisdn, new PairModified<ConvMessage, Integer>(null, 0));
				}
				lastPinMap.get(msisdn).setFirst(convMessage); // update last pin message for a msisdn
				lastPinMap.get(msisdn).setSecond(lastPinMap.get(msisdn).getSecond() + 1); // increment pin unread count for a msisdn
			}

		}

		/*
		 * Increment unread count for each msisdn/groupId
		 */
		if (messageListMap.size() > 0)
		{
			convDb.incrementUnreadCountBulk(messageListMap);
		}

		/*
		 * contains last message for each conversation to be added to conversation table
		 */
		ArrayList<ConvMessage> lastMessageList = new ArrayList<ConvMessage>(messageListMap.keySet().size());
		for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
		{
			LinkedList<ConvMessage> list = entry.getValue();
			if (list.size() > 0)
			{
				lastMessageList.add(list.get(list.size() - 1));
			}
		}

		/*
		 * add last message and last pin and unread pin count to conversation table
		 */
		if (lastMessageList.size() > 0)
		{
			convDb.addLastConversations(lastMessageList, lastPinMap);
		}

		/*
		 * update status of messages and also update readByString in group info table for each group
		 */
		if (messageStatusMap.size() > 0)
		{
			convDb.updateStatusBulk(messageStatusMap);
			convDb.setReadByForGroupBulk(messageStatusMap);
		}

		/*
		 * Since now messages contains message id and conversation object we can process ft messages
		 */
		for (ConvMessage convMessage : messageList)
		{
			messageProcessFT(convMessage);
			messageProcessSticker(convMessage);
			messageProcessStickerRecommendation(convMessage);
		}

		/*
		 * publish the events for updating chat thread and conversation table
		 */
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_RECEIVED, messageListMap);
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_DELIVERED_READ, messageStatusMap);
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_NOTIFICATION, messageListMap);
	}

	private void addToLists(String msisdn, ConvMessage convMessage)
	{
		// We were not checking for blocked list here while processing the bulk packet.
		if (ContactManager.getInstance().isBlocked(msisdn))
		{
			return;
		}

		messageList.add(convMessage);
	}

	public void saveMqttMessage(JSONObject jsonObj) throws JSONException
	{
		Logger.d("Gcm test", jsonObj.toString());

		String type = jsonObj.optString(HikeConstants.TYPE);
		Logger.d(VoIPConstants.TAG, "Received message of type: " + type); // TODO: Remove me!
		if (HikeConstants.MqttMessageTypes.ICON.equals(type)) // Icon changed
		{
			saveIcon(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type))
		{
			saveDisplayPic(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.SMS_CREDITS.equals(type)) // Credits
		// changed
		{
			saveSMSCredits(jsonObj);
		}
		else if ((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type)) || (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type))) // User
		// joined/left
		{
			saveUserJoinedOrLeft(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.INVITE_INFO.equals(type)) // Invite
		// info
		{
			saveInviteInfo(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type)) // Group
		// chat
		// join
		{
			saveGCJoin(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_ADMIN_UPDATE.equals(type)) // Group
		// chat
		// join
		{
			saveAdminUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_SETTINGS_CHANGE.equals(type)) // Group
		// chat
		// join
		{
			changeGroupSettings(jsonObj, true);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type)) // Group
		// chat
		// leave
		{
			saveGCLeave(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type)) // Group
		// chat
		// name
		// change
		{
			saveGCName(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) // Group
		// chat
		// end
		{
			saveGCEnd(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0.equals(type) || HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1.equals(type))
		{
			VoIPUtils.handleVOIPPacket(context, jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type)) // Message
		// received
		// from
		// server
		{
			if (isBulkMessage)
			{
				saveMessageBulk(jsonObj);
			}
			else
			{
				saveMessage(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type)) // Message
		// delivered
		// to
		// receiver
		{
			MsgRelLogManager.logMsgRelDR(jsonObj, MsgRelEventType.DR_RECEIVED_AT_SENEDER_MQTT);
			if (isBulkMessage)
			{
				saveDeliveryReportBulk(jsonObj);
			}
			else
			{
				saveDeliveryReport(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type)) // Message
		// has
		// been
		// read
		{
			if (isBulkMessage)
			{
				saveMessageReadBulk(jsonObj);
			}
			else
			{
				saveMessageRead(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type) || HikeConstants.MqttMessageTypes.END_TYPING.equals(type))
		{
			saveTyping(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE.equals(type))
		{
			saveUpdateAvailable(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_INFO.equals(type))
		{
			saveAccountInfo(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type))
		{
			saveUserOptIn(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(type))
		{
			saveBlockInternationalSMS(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
		{
			saveAddFavorite(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG.equals(type))
		{
			saveAccountConfig(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.AUTO_APK.equals(type))
		{
			saveAutoApkInstallConfig(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REWARDS.equals(type))
		{
			saveRewards(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACTION.equals(type))
		{
			saveAction(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type))
		{
			saveStatusUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.DELETE_STATUS.equals(type))
		{
			saveDeleteStatus(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE.equals(type) || HikeConstants.MqttMessageTypes.REMOVE_FAVORITE.equals(type))
		{
			savePostponeOrRemoveFavorite(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BATCH_STATUS_UPDATE.equals(type))
		{
			saveBatchStatusUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.STICKER.equals(type))
		{
			saveSticker(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BULK_LAST_SEEN.equals(type))
		{
			saveBulkLastSeen(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.LAST_SEEN.equals(type))
		{
			saveLastSeen(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.SERVER_TIMESTAMP.equals(type))
		{
			saveServerTimestamp(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.PROTIP.equals(type))
		{
			saveProtip(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_PUSH.equals(type))
		{
			saveUpdatePush(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.APPLICATIONS_PUSH.equals(type))
		{
			saveApplicationsPush(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(type))
		{
			saveChatBackground(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_OWNER_CHANGE.equals(type))
		{
			saveGroupOwnerChange(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REQUEST_DP.equals(type))
		{
			saveRequestDP(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.POPUP.equals(type))
		{
			savePopup(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REMOVE_PIC.equals(type))
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			if (HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			}
		}
		else if (HikeConstants.MqttMessageTypes.BULK_MESSAGE.equals(type))
		{
			saveBulkMessage(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.TIP.equals(type))
		{
			saveTip(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.NUX.equals(type))
		{
			saveNuxPacket(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.PACKET_ECHO.equals(type))
		{
			handlePacketEcho(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.PRODUCT_POPUP.equals(type))
		{
			if (jsonObj.has(HikeConstants.DATA))
			{
				JSONObject mmData = jsonObj.getJSONObject(HikeConstants.DATA);

				if (mmData.has(HikeConstants.METADATA))
				{
					JSONObject mmMetaData = mmData.getJSONObject(HikeConstants.METADATA);

					if (mmMetaData.optBoolean(HikeConstants.FLUSH))
					{
						ProductInfoManager.getInstance().deleteAllPopups();
					}
					else
					{
						ProductInfoManager.getInstance().parsePopupPacket(mmMetaData);
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED, null);
				}

			}
		}
		else if (HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ.equals(type))// Message came with
		// 'pd' means message is to be tracked for reliability
		{
			saveNewMessageRead(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ONE.equals(type) || HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ZERO.equals(type))
		{
			GeneralEventMessagesManager.getInstance().handleGeneralMessage(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACTIVITY_UPDATE.equals(type))
		{
			saveActivityUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.TIMELINE_PREFFERED_CONTACTS.equals(type))
		{
			saveTimelineFTUEPrefferedContacts(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GENERAL_EVENT_PACKET_ZERO.equals(type))
		{
			GeneralEventMessagesManager.getInstance().handleGeneralMessage(jsonObj);
		}
		else if(HikeConstants.MqttMessageTypes.PLATFORM_INFRA_CONFIG.equals(type))
		{
			saveInfraConfig(jsonObj);
		}

		else if (HikeConstants.TOAST.equals(type))
		{
			showToast(jsonObj);
		}
		else if (ABTest.onRequestReceived(type, jsonObj))
		{
			//Do nothing, if its a ABTest message its handled
		}
	}
	
	private void saveInfraConfig(JSONObject jsonObj)
	{
		if (jsonObj.has(HikeConstants.DATA))
		{
			try
			{
				JSONObject dJson = jsonObj.getJSONObject(HikeConstants.DATA);
				
				JSONArray configArray = dJson.getJSONArray(HikeConstants.URLS);
				for (int i = 0; i < configArray.length(); i++)
				{
					JSONObject chatBgJson = configArray.optJSONObject(i);

					if (chatBgJson == null)
					{
						continue;
					}

					String url = chatBgJson.optString(HikeConstants.URL);
					String key = chatBgJson.optString(HikeConstants.KEY);
					int life = chatBgJson.optInt(HikeConstants.LIFE);
					convDb.insertURL(key, url, life);

				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void showToast(final JSONObject jsonObj) throws JSONException
	{
		if(jsonObj.has(HikeConstants.DATA))
		{
			final JSONObject toastJson = jsonObj.getJSONObject(HikeConstants.DATA);
			new Handler(Looper.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast toast =  Toast.makeText(context, toastJson.optString(HikeConstants.Toast.TOAST_MESSAGE,
									context.getString(R.id.app_name)),
							toastJson.optInt(HikeConstants.Toast.TOAST_DURATION, Toast.LENGTH_SHORT));
					toast.setGravity(toastJson.optInt(HikeConstants.Toast.TOAST_GRAVITY, toast.getGravity()), toast.getXOffset(), toast.getYOffset());
					toast.show();
				}
			});

		}
	}

	private void saveActivityUpdate(final JSONObject jsonObj)
	{
		Logger.d(HikeConstants.TIMELINE_LOGS, "ac_up packet received " + jsonObj);
		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					FeedDataModel feedData = new FeedDataModel(jsonObj);

					if (feedData.getObjType() == ActivityObjectTypes.STATUS_UPDATE)
					{
						StatusMessage statusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(feedData.getObjID());
						if (statusMessage == null)
						{
							Logger.d(HikeConstants.TIMELINE_LOGS, "Status Msg is null so returning  for MappedOd " + feedData.getObjID());
							return;
						}
					}

					boolean isSuccess = HikeConversationsDatabase.getInstance().addActivityUpdate(feedData);

					TimelineActionsManager.getInstance().getActionsData().updateByActivityFeed(feedData);

					//Saving count to file to display the counter at home screen
					int count = HikeConversationsDatabase.getInstance().getUnreadActivityFeedCount(false);
					if(count != -1)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.USER_TIMELINE_ACTIVITY_COUNT, count);

						// To update Overflow menu on Home Screen
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
						HikeMessengerApp.getPubSub().publish(HikePubSub.UNSEEN_STATUS_COUNT_CHANGED, null);
					}

					if (isSuccess)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, feedData);

						HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE_NOTIF, feedData);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}, 0);
	}

	private void handleSendNativeInviteKey(boolean sendNativeInvite, boolean showFreeSmsPopup, String header, String body, Editor editor)
	{
		if (!HikeMessengerApp.isIndianUser())
		{
			return;
		}
		editor.putBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, sendNativeInvite);
		if (sendNativeInvite)
		{
			/*
			 * If native is being turned on, we remove all preferences saved for not showing the native SMS invite dialog so that the user is shown these dialogs again.
			 */
			editor.remove(HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED);
			editor.remove(HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED);
			editor.remove(HikeConstants.OPERATOR_SMS_ALERT_CHECKED);

			editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false);
		}
		else
		{
			/*
			 * Else we set a preference to show a dialog in the home screen that the free Invites are turned on.
			 */
			editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, showFreeSmsPopup);
			if (showFreeSmsPopup)
			{
				editor.putString(HikeMessengerApp.FREE_INVITE_POPUP_BODY, body);
				editor.putString(HikeMessengerApp.FREE_INVITE_POPUP_HEADER, header);
			}
		}

	}

	private void autoDownloadProfileImage(StatusMessage statusMessage, boolean statusUpdate)
	{
		if ((FTUtils.getNetworkType(context) == NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
				|| (FTUtils.getNetworkType(context) != NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
		{
			return;
		}

		String fileName = Utils.getProfileImageFileName(statusMessage.getMappedId());
		HikeImageDownloader downLoaderFragment = HikeImageDownloader.newInstance(statusMessage.getMappedId(), fileName, true, statusUpdate, statusMessage.getMsisdn(),
				statusMessage.getNotNullName(), null, true, true);
		downLoaderFragment.startLoadingTask();
	}

	private void autoDownloadGroupImage(String id)
	{
		if ((FTUtils.getNetworkType(context) == NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
				|| (FTUtils.getNetworkType(context) != NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
		{
			return;
		}

		String fileName = Utils.getProfileImageFileName(id);
		HikeImageDownloader downLoaderFragment = HikeImageDownloader.newInstance(id, fileName, true, false, null, null, null, true, true);
		downLoaderFragment.startLoadingTask();
	}

	private void autoDownloadProtipImage(StatusMessage statusMessage, boolean statusUpdate)
	{
		String fileName = Utils.getProfileImageFileName(statusMessage.getMappedId());
		HikeImageDownloader downLoaderFragment = HikeImageDownloader.newInstance(statusMessage.getMappedId(), fileName, true, statusUpdate, statusMessage.getMsisdn(),
				statusMessage.getNotNullName(), statusMessage.getProtip().getImageURL(), true, true);
		downLoaderFragment.startLoadingTask();
	}

	private void setDefaultSMSClientTutorialSetting()
	{
		/*
		 * If settings already contains this key, no need to do anything since this has already been handled.
		 */
		if (settings.contains(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP))
		{
			return;
		}

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, false);
		editor.commit();
	}

	private void removeOrPostponeFriendType(String msisdn)
	{
		ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, true);
		if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND)
		{
			return;
		}
		FavoriteType currentFavoriteType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentFavoriteType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.NOT_FRIEND
				: FavoriteType.REQUEST_SENT_REJECTED;

		ContactInfo contact = new ContactInfo(contactInfo);

		ContactInfo updatedContact = new ContactInfo(contactInfo);
		updatedContact.setFavoriteType(favoriteType);
		ContactManager.getInstance().updateContacts(updatedContact);

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(contact, favoriteType);
		this.pubSub.publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}

	private void incrementUnseenStatusCount()
	{
		int count = settings.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		count++;
		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, count);
		editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
		editor.commit();

		pubSub.publish(HikePubSub.UNSEEN_STATUS_COUNT_CHANGED, null);
	}

	private void updateDbBatch(long[] ids, ConvMessage.State status, String msisdn)
	{
		convDb.updateBatch(ids, status.ordinal(), msisdn);
	}

	private int updateDB(Object object, ConvMessage.State status, String msisdn)
	{
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations
		 */
		return convDb.updateMsgStatus(msgID, status.ordinal(), msisdn);
	}

	private ConvMessage saveStatusMsg(JSONObject jsonObj, String msisdn) throws JSONException
	{
		if (isBulkMessage)
		{
			ConvMessage convMessage = saveStatusMsgBulk(jsonObj, msisdn);
			return convMessage;
		}
		ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);

		if (convMessage == null)
		{
			return null;
		}

		convDb.addConversationMessages(convMessage, true);

		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

		statusMessagePostProcess(convMessage, jsonObj);

		return convMessage;
	}

	private ConvMessage saveStatusMsgBulk(JSONObject jsonObj, String msisdn) throws JSONException
	{
		ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);

		if (convMessage == null)
		{
			return null;
		}

		addToLists(msisdn, convMessage);

		statusMessagePostProcess(convMessage, jsonObj);

		return convMessage;
	}

	private ConvMessage statusMessagePreProcess(JSONObject jsonObj, String msisdn) throws JSONException
	{
		Conversation conversation = convDb.getConversationWithLastMessage(msisdn);

		boolean isChatBgMsg = HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(jsonObj.getString(HikeConstants.TYPE));
		boolean isUJMsg = HikeConstants.MqttMessageTypes.USER_JOINED.equals(jsonObj.getString(HikeConstants.TYPE));
		boolean isGettingCredits = false;
		if (isUJMsg)
		{
			isGettingCredits = jsonObj.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS, -1) > 0;
		}
		/*
		 * If the message is of type 'uj' we want to show it for all known contacts regardless of if the user currently has an existing conversation. We also want to show the 'uj'
		 * message in all the group chats with that participant. Otherwise for other types, we only show the message if the user already has an existing conversation.
		 */
		if (!isChatBgMsg)
		{
			if ((conversation == null && (!isUJMsg || !ContactManager.getInstance().doesContactExist(msisdn)))
					|| (conversation != null && TextUtils.isEmpty(conversation.getConversationName()) && isUJMsg && !isGettingCredits && !(conversation instanceof GroupConversation)))
			{
				return null;
			}
		}
		ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
		if (OneToNConversationUtils.isOneToNConversation(convMessage.getMsisdn()))
		{
			ContactManager.getInstance().updateGroupRecency(convMessage.getMsisdn(), convMessage.getTimestamp());
		}
		return convMessage;
	}

	private void statusMessagePostProcess(ConvMessage convMessage, JSONObject jsonObj) throws JSONException
	{
		if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED || convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END || convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGE_ADMIN
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GC_SETTING_CHANGE)
		{
			this.pubSub.publish(
					convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? HikePubSub.PARTICIPANT_JOINED_ONETONCONV : convMessage
							.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT ? HikePubSub.PARTICIPANT_LEFT_ONETONCONV
							: convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGE_ADMIN ? HikePubSub.ONETONCONV_ADMIN_UPDATE : convMessage
									.getParticipantInfoState() == ParticipantInfoState.GC_SETTING_CHANGE ? HikePubSub.ONETONCONV_SETTING_UPDATE : HikePubSub.GROUP_END, jsonObj);
		}
	}

	private void addTypingNotification(String id, String participant)
	{
		TypingNotification typingNotification;
		ClearTypingNotification clearTypingNotification;
		boolean isGroupConversation = !TextUtils.isEmpty(participant);

		if (!typingNotificationMap.containsKey(id))
		{
			if (isGroupConversation)
			{
				clearTypingNotification = new ClearGroupTypingNotification(id, participant);
				typingNotification = new GroupTypingNotification(id, participant, (ClearGroupTypingNotification) clearTypingNotification);
			}
			else
			{
				clearTypingNotification = new ClearTypingNotification(id);

				typingNotification = new TypingNotification(id, clearTypingNotification);
			}

			typingNotificationMap.put(id, typingNotification);
		}
		else
		{
			typingNotification = typingNotificationMap.get(id);

			if (isGroupConversation)
			{
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) typingNotification;
				if (!groupTypingNotification.hasParticipant(participant))
				{
					clearTypingNotification = new ClearGroupTypingNotification(id, participant);

					groupTypingNotification.addParticipant(participant);
					groupTypingNotification.addClearTypingNotification((ClearGroupTypingNotification) clearTypingNotification);
				}
				else
				{
					clearTypingNotification = groupTypingNotification.getClearTypingNotification(participant);
				}
			}
			else
			{
				clearTypingNotification = typingNotification.getClearTypingNotification();
			}
			clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);
		}
		clearTypingNotificationHandler.postDelayed(clearTypingNotification, HikeConstants.LOCAL_CLEAR_TYPING_TIME);

		this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, typingNotification);
	}

	private void removeTypingNotification(String id, String participant)
	{

		boolean isGroupConversation = !TextUtils.isEmpty(participant);

		TypingNotification typingNotification = typingNotificationMap.get(id);

		ClearTypingNotification clearTypingNotification;

		if (typingNotification != null)
		{
			if (isGroupConversation)
			{
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) typingNotification;
				groupTypingNotification.removeParticipant(participant);
				Logger.d("TypingNotification", "Particpant size: " + groupTypingNotification.getGroupParticipantList().size());
				if (groupTypingNotification.getGroupParticipantList().isEmpty())
				{
					typingNotificationMap.remove(id);
				}
				clearTypingNotification = groupTypingNotification.getClearTypingNotification(participant);
			}
			else
			{
				typingNotificationMap.remove(id);
				clearTypingNotification = typingNotification.getClearTypingNotification();
			}

			clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);

			/*
			 * We only publish this event if we actually removed a typing notification
			 */
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, typingNotification);
		}
	}

	/**
	 * Method to parse atomic tip packet
	 * @param jsonObj
     */
	public void saveOrFlushAtomicTip(JSONObject jsonObj)
	{
		Logger.d(getClass().getSimpleName(), "Parsing atomic tip packet");
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		if(data != null)
		{
			if(data.has(HikeConstants.FLUSH))
			{
				if(data.optBoolean(HikeConstants.FLUSH, false))
				{
					Logger.d(getClass().getSimpleName(), "Received atomic tip flush packet!");
					AtomicTipManager.getInstance().processFlushPacket();
					return;
				}
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "Received new atomic tip packet, saving it!");
				AtomicTipManager.getInstance().parseAtomicTipPacket(data);
			}
		}
	}

	/**
	 * We call it atomic pop up , as we discard old if any when new comes --gauravKhanna
	 *
	 * @param jsonObj
	 *            - jsonFromServer
	 * @throws JSONException
	 */
	public void updateAtomicPopUpData(JSONObject jsonObj) throws JSONException
	{
		Logger.i("tip", jsonObj.toString());
		String subType = jsonObj.getString(HikeConstants.SUB_TYPE);

		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		if (isDuplicateOrWrongPacket("last" + subType, data))
		{
			return;
		}
		Logger.i("tip", "id passed");
		String header = data.optString(HikeConstants.HEADER);
		String body = data.optString(HikeConstants.BODY);
		if (!TextUtils.isEmpty(header) && !TextUtils.isEmpty(body))
		{
			HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
			pref.saveData("last" + subType, data.getString(HikeConstants.MESSAGE_ID));
			String[] keys = getPopUpTypeAndShowNotification(subType, (data.optBoolean(HikeConstants.PUSH, true) ? body : null));
			if (keys != null)
			{
				pref.saveData(keys[0], header);
				pref.saveData(keys[1], body);
				pref.saveData(keys[2], subType);
				String url = data.optString(HikeConstants.URL);
				// for http based generic URL
				if (!TextUtils.isEmpty(url) && HikeMessengerApp.ATOMIC_POP_UP_HTTP.equals(subType))
				{
					pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, url);
				}
				else if (HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC.equals(subType))
				{
					// for app specific generic tip
					String what = data.optString(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT);
					if (!TextUtils.isEmpty(what))
					{
						try
						{
							pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT, Integer.parseInt(what));
						}
						catch (NumberFormatException nf)
						{
							nf.printStackTrace();
							// don know where to go on click, lets remove key so tip id not displayed
							pref.saveData(keys[0], "");
						}
					}
				}
				Logger.i("tip", "writing to pref passed " + header + " -- " + body + " -- subtype " + subType);
			}
			else
			{
				Logger.i("tip", "writing to pref failed , could not find keys  ");
			}
		}
		else
		{
			Logger.i("tip", "header message failed " + header + " -- " + body);
		}
	}

	/**
	 * Since we use over write mechanism per screen for tips , say tip 1 and tip2 arrives for main screen , so we over write tip1 and save only tip2
	 *
	 *
	 * @param subType
	 *            -- subtype which comes from server
	 * @return String array for header ,message and subtype keys , 0 is header , 1 is message , 2 is subtypekey
	 *
	 *         in addition it shows notification whereever applicable
	 */
	private String[] getPopUpTypeAndShowNotification(String subType, String notificationTextIfApplicable)
	{
		// for chat screen
		if (HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_STICKER.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_THEME.equals(subType))
		{
			Logger.i("tip", "subtype for chat");
			return new String[] { HikeMessengerApp.ATOMIC_POP_UP_HEADER_CHAT, HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_CHAT, HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT };
		}

		// for main screen
		Logger.i("tip", "subtype for main");
		if (HikeMessengerApp.ATOMIC_POP_UP_FAVOURITES.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_INVITE.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_PROFILE_PIC.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_STATUS.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_INFORMATIONAL.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_HTTP.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC.equals(subType))
		{
			// show notification
			if (notificationTextIfApplicable != null)
			{
				Bundle bundle = new Bundle();
				bundle.putString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_MESSAGE, notificationTextIfApplicable);
				bundle.putString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_SCREEN, HomeActivity.class.getName());
				this.pubSub.publish(HikePubSub.ATOMIC_POPUP_WITH_PUSH, bundle);

			}
			return new String[] { HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_MAIN, HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN };

		}
		Logger.i("tip", "subtype for nothing , it shud not reach here");
		// it will not reach here
		return null;
	}

	private boolean isDuplicateOrWrongPacket(String key, JSONObject jsonObject)
	{
		String id = jsonObject.optString(HikeConstants.MESSAGE_ID);
		return TextUtils.isEmpty(id) || HikeSharedPreferenceUtil.getInstance().getData(key, "").equals(id);
	}

	private void saveNuxPacket(JSONObject jsonObject)
	{
		NUXManager.getInstance().parseNuxPacket(jsonObject.toString());
	}

	private void handlePacketEcho(JSONObject json)
	{
		// TODO : Code for DR comes here
		JSONObject data = json.optJSONObject(HikeConstants.DATA);
		if (data != null)
		{
			try
			{
				JSONObject object = new JSONObject();
				object.put(HikeConstants.DATA, data);
				HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.MqttMessageTypes.PACKET_ECHO, object);
			}
			catch (JSONException je)
			{
				je.printStackTrace();
			}
		}
	}

	private void handleWhitelistDomains(String jsonString)
	{
		try
		{
			Logger.i("mqttwhitelist", "whitelist packet " + jsonString);
			JSONObject urls = new JSONObject(jsonString);
			boolean enabled = urls.optBoolean(HikeConstants.ENABLED);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLED_WHITELISTED_FEATURE, enabled);
			if (enabled)
			{
				HikeContentDatabase.getInstance().deleteAllDomainsFromWhitelist();
				JSONArray inHike = urls.optJSONArray(HikeConstants.IN_HIKE_URL_WHITELIST);
				if (inHike != null)
				{
					saveWhiteListDomains(inHike, WhitelistDomain.WHITELISTED_IN_HIKE);
				}
				JSONArray inBrowser = urls.optJSONArray(HikeConstants.BROWSER_URL_WHITELIST);
				if (inBrowser != null)
				{
					saveWhiteListDomains(inBrowser, WhitelistDomain.WHITELISTED_IN_BROWSER);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			// DO Nothing
		}
	}

	private void saveWhiteListDomains(JSONArray array, int whitelistState)
	{
		WhitelistDomain[] domains = new WhitelistDomain[array.length()];
		try
		{
			for (int i = 0; i < array.length(); i++)
			{
				String dom = array.getString(i);
				if (!TextUtils.isEmpty(dom))
				{
					WhitelistDomain domain = new WhitelistDomain(dom, whitelistState, dom);
					domains[i] = domain;
				}
			}
			HikeContentDatabase.getInstance().addDomainInWhitelist(domains);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveGCMMessage(JSONObject json)
	{
		try
		{
			String type = json.optString(HikeConstants.TYPE);
			if (HikeConstants.MqttMessageTypes.PACKET_ECHO.equals(type))
			{
				handlePacketEcho(json);
			}
			else
			{
				Logger.i("gcmMqttMessage", "message received " + json.toString());

				// Check if the message is expired
				String expiryTime = json.optString(HikeConstants.EXPIRE_AT);

				JSONObject pushAckJson = json.optJSONObject(HikeConstants.PUSHACK);

				String from = json.optString(HikeConstants.FROM);
				if (ContactManager.getInstance().isBlocked(from))
				{
					blockedMessageAnalytics(HikePlatformConstants.CARD);
					// discard message since the conversation is blocked
					return;
				}

				if (!TextUtils.isEmpty(expiryTime))
				{
					try
					{
						long expiry = Long.valueOf(expiryTime);
						long currentEpoch = System.currentTimeMillis();
						currentEpoch = currentEpoch / 1000;
						if (currentEpoch > expiry)
						{
							Logger.i("gcmMqttMessage", "message expired " + json.toString());
							JSONObject metadata = new JSONObject();
							metadata.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.GCM_EXPIRED);
							metadata.put(HikeConstants.EXPIRE_AT, expiryTime);

							if (pushAckJson != null)
							{
								metadata.put(HikeConstants.PUSHACK, pushAckJson);
							}

							HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.LogEvent.GCM_ANALYTICS_CONTEXT, EventPriority.HIGH, metadata);

							// Discard message since it has expired
							return;
						}
					}
					catch (NumberFormatException nfe)
					{
						nfe.printStackTrace();
						// Assuming message is not expired
					}
				}

				if (pushAckJson != null)
				{
					// Record push ack
					JSONObject metadata = new JSONObject();
					metadata.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.GCM_PUSH_ACK);
					metadata.put(HikeConstants.PUSHACK, pushAckJson);
					HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.LogEvent.GCM_ANALYTICS_CONTEXT, EventPriority.HIGH, metadata);
				}

				if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type))
				{
					saveMessage(json);
				}
				else if (HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG.equals(type))
				{
					saveAccountConfig(json);
				}
				else if (HikeConstants.MqttMessageTypes.POPUP.equals(type))
				{
					savePopup(json);
				}
				else if (HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0.equals(type) || HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1.equals(type))
				{
					VoIPUtils.handleVOIPPacket(context, json);
				}
				else if (HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ONE.equals(type) || HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ZERO.equals(type))
				{
					GeneralEventMessagesManager.getInstance().handleGeneralMessage(json);
				}
				else
				{
					Logger.e("gcmMqttMessage", "Unexpected type received via GCM mqtt equivalent messages");
				}
			}
		}
		catch (JSONException je)
		{
			je.printStackTrace();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	private void saveTimelineFTUEPrefferedContacts(JSONObject jsonObj)
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		JSONArray msisdnArray = data.optJSONArray(HikeConstants.CONTACTS);
		int counter = data.optInt(HikeConstants.COUNT);
		Logger.d("tl_ftue", "ftue packet received " + msisdnArray + ", count is " + counter);
		Set<String> msisdnSet = null;
		if (msisdnArray != null)
		{
			msisdnSet = new LinkedHashSet<String>();
			for (int i = 0; i < msisdnArray.length(); i++)
			{
				String msisdn = msisdnArray.optString(i);
				if (!TextUtils.isEmpty(msisdn))
				{
					msisdnSet.add(msisdn);
					if (!new File(Utils.getProfileImageFileName(msisdn)).exists())
					{
						HikeImageDownloader.newInstance(msisdn, Utils.getProfileImageFileName(msisdn), ContactManager.getInstance().hasIcon(msisdn), false, null, null, null,
								false, false).startLoadingTask();
					}
				}

			}
		}

		// case 1) if init not show or on top
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INIT_CARD_SHOWN, true)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INIT_CARD_ON_TOP, false))
		{
			// just replace these values
			HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, msisdnSet);
			Logger.d("tl_ftue", "ftue packet, case  init not show or on top " + msisdnSet);
		}
		// case 2) exit card on top:-
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_ON_TOP, false))
		{
			// just replace these values
			HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, msisdnSet);
			Logger.d("tl_ftue", "ftue packet, case  exit card on top " + msisdnSet);
		}

		// case 3) fav card is on top or no cards on top
		else
		{
			// fetch previous list
			Set<String> list = HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, null);

			// list will be NON-EMPTY if any fav card is on top
			if (list != null && !list.isEmpty())
			{
				// fetch current item from previous list
				Iterator<String> iterator = list.iterator();
				String currentMsisdn = null;
				if (iterator.hasNext())
				{
					currentMsisdn = iterator.next();
					Logger.d("tl_ftue", "ftue packet, card on top is fav card and from SP " + currentMsisdn);
				}

				// add to new list
				if (!TextUtils.isEmpty(currentMsisdn))
				{
					msisdnSet.add(currentMsisdn);
					counter = counter + 1;
					Logger.d("tl_ftue", "ftue packet, card on top is fav card, so inc counter " + counter);
				}
			}
			else
			{
				Logger.d("tl_ftue", "ftue packet, NO CARDS ON TOP ");
			}

			// save new list
			HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, msisdnSet);
			Logger.d("tl_ftue", "ftue packet, case fav card is on top or no cards on top " + msisdnSet);
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TIMELINE_FTUE_CARD_TO_SHOW_COUNTER, counter);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, true);
		Utils.incrementUnseenStatusCount();
		// HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_FTUE_LIST_UPDATE, new Pair<Set<String>, Integer>(msisdnSet, counter));
	}
}
