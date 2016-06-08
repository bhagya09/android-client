package com.bsb.hike.db;


import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_ACTION_BAR_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_BG_LANDSCAPE;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_BG_PORTRAIT;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_BUBBLE_COLOR;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_CHAT_BUBBLE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_INLINE_STATUS_MSG_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_OFFLINE_MESSAGE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_RECEIVED_NUDGE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_SENT_NUDGE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_SMS_TOGGLE_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_BG;
import static com.bsb.hike.chatthemes.HikeChatThemeConstants.ASSET_INDEX_THUMBNAIL;
import static com.bsb.hike.db.DBConstants.ACTIONS_TABLE;
import static com.bsb.hike.db.DBConstants.ACTION_COUNT;
import static com.bsb.hike.db.DBConstants.ACTION_ID;
import static com.bsb.hike.db.DBConstants.ACTION_OBJECT_ID;
import static com.bsb.hike.db.DBConstants.ACTION_OBJECT_TYPE;
import static com.bsb.hike.db.DBConstants.ACTORS;
import static com.bsb.hike.db.DBConstants.BOT_CONFIGURATION;
import static com.bsb.hike.db.DBConstants.BOT_TABLE;
import static com.bsb.hike.db.DBConstants.COLUMN_TYPE_INTEGER;
import static com.bsb.hike.db.DBConstants.COLUMN_TYPE_TEXT;
import static com.bsb.hike.db.DBConstants.COMMA_SEPARATOR;
import static com.bsb.hike.db.DBConstants.CONFIG_DATA;
import static com.bsb.hike.db.DBConstants.CONVERSATIONS_TABLE;
import static com.bsb.hike.db.DBConstants.CONVERSATION_METADATA;
import static com.bsb.hike.db.DBConstants.CREATE_TABLE;
import static com.bsb.hike.db.DBConstants.ChatThemes;
import static com.bsb.hike.db.DBConstants.EVENT_FROM_USER_MSISDN;
import static com.bsb.hike.db.DBConstants.EVENT_ID;
import static com.bsb.hike.db.DBConstants.EVENT_METADATA;
import static com.bsb.hike.db.DBConstants.EVENT_PARENT_MSISDN;
import static com.bsb.hike.db.DBConstants.EVENT_STATUS;
import static com.bsb.hike.db.DBConstants.EVENT_TYPE;
import static com.bsb.hike.db.DBConstants.FEED_ACTOR;
import static com.bsb.hike.db.DBConstants.FEED_OBJECT_ID;
import static com.bsb.hike.db.DBConstants.FEED_OBJECT_TYPE;
import static com.bsb.hike.db.DBConstants.FEED_TABLE;
import static com.bsb.hike.db.DBConstants.FEED_TS;
import static com.bsb.hike.db.DBConstants.HIKE_CONTENT;
import static com.bsb.hike.db.DBConstants.HIKE_CONV_DB.LOVE_TABLE;
import static com.bsb.hike.db.DBConstants.HIKE_CONV_DB.REF_COUNT;
import static com.bsb.hike.db.DBConstants.HIKE_UID;
import static com.bsb.hike.db.DBConstants.IS_MUTE;
import static com.bsb.hike.db.DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME;
import static com.bsb.hike.db.DBConstants.MESSAGES_TABLE;
import static com.bsb.hike.db.DBConstants.MESSAGE_EVENT_TABLE;
import static com.bsb.hike.db.DBConstants.MESSAGE_HASH;
import static com.bsb.hike.db.DBConstants.MESSAGE_ID;
import static com.bsb.hike.db.DBConstants.MESSAGE_METADATA;
import static com.bsb.hike.db.DBConstants.METHOD;
import static com.bsb.hike.db.DBConstants.MSISDN;
import static com.bsb.hike.db.DBConstants.PARAMETER_KEY;
import static com.bsb.hike.db.DBConstants.PARAMETER_LIST;
import static com.bsb.hike.db.DBConstants.PARAMETER_MAPPING_TABLE;
import static com.bsb.hike.db.DBConstants.PARAMETER_VALUE;
import static com.bsb.hike.db.DBConstants.QUICK_SUGGESTED_REPLY_STICKERS;
import static com.bsb.hike.db.DBConstants.QUICK_SUGGESTED_SENT_STICKERS;
import static com.bsb.hike.db.DBConstants.STATUS_MAPPED_ID;
import static com.bsb.hike.db.DBConstants.STATUS_TABLE;
import static com.bsb.hike.db.DBConstants.UNREAD_COUNT;
import static com.bsb.hike.db.DBConstants.URL;
import static com.bsb.hike.db.DBConstants.USER_PARAMETER_TABLE;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.db.DBConstants.HIKE_CONV_DB;
import com.bsb.hike.db.DatabaseErrorHandlers.ConversationDatabaseErrorHandler;
import com.bsb.hike.db.dbcommand.SetPragmaModeCommand;
import com.bsb.hike.messageinfo.MessageInfo;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ConvMessageComparator;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation.BotConversation;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.ConversationMetadata;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConvInfo;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.models.Conversation.OneToOneConversation;
import com.bsb.hike.models.Conversation.OneToOneConversationMetadata;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.FetchUIDTaskPojo;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Mute;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.ConversationMsisdns;
import com.bsb.hike.modules.contactmgr.GroupDetails;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.ContentLove;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.nativecards.NativeCardUtils;
import com.bsb.hike.service.GeneralEventMessagesManager;
import com.bsb.hike.service.UpgradeIntentService;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class HikeConversationsDatabase extends SQLiteOpenHelper
{

	private static volatile SQLiteDatabase mDb;

	private static volatile HikeConversationsDatabase hikeConversationsDatabase;

	private static Context mContext;

	public static void init(Context context)
	{
		if (hikeConversationsDatabase == null)
		{
			mContext = context;
			hikeConversationsDatabase = new HikeConversationsDatabase(context);
		}
	}

	public static HikeConversationsDatabase getInstance()
	{
		return hikeConversationsDatabase;
	}

	private HikeConversationsDatabase(Context context)
	{
		super(context, DBConstants.CONVERSATIONS_DATABASE_NAME, null, DBConstants.CONVERSATIONS_DATABASE_VERSION, new ConversationDatabaseErrorHandler());
		initializeIfRequiredAndGetWriteDatabase();
	}
	
	public SQLiteDatabase initializeIfRequiredAndGetWriteDatabase()
	{
		if (mDb == null || !mDb.isOpen())
		{
			mDb = super.getWritableDatabase();
			SetPragmaModeCommand setPragmaModeCommand = new SetPragmaModeCommand(mDb);
			setPragmaModeCommand.execute();
		}
		return mDb;
	}

	public String getJournalMode()
	{
		String journalMode = null;
		try
		{
			Cursor cursor = mDb.rawQuery("PRAGMA journal_mode;", null);
			if (cursor.moveToFirst())
			{
				journalMode = cursor.getString(0);
				Logger.d(getClass().getSimpleName(), "Journal mode = " + journalMode);
			}
		}
		catch (Throwable th)
		{
			Logger.d(getClass().getSimpleName(), "exception in getting journal mode", th);
		}
		return journalMode;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		if (db == null)
		{
			db = mDb;
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.MESSAGES_TABLE
				+ " ( "
				+ DBConstants.MESSAGE + " TEXT, " // The message text
				+ DBConstants.MSG_STATUS + " INTEGER, " // Whether the message is sent or not. Plus also tells us the current state of the message.
				+ DBConstants.TIMESTAMP + " INTEGER, " // Message time stamp, send or receiving time in seconds
				+ DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // The message id (Unique)
				+ DBConstants.MAPPED_MSG_ID + " INTEGER, " // The message id of the message on the sender's side (Only applicable for received messages)
				+ DBConstants.CONV_ID + " INTEGER," // Deprecated
				+ DBConstants.MESSAGE_METADATA + " TEXT, " // Extra info of the message. Stored in JSON format
				+ DBConstants.PRIVATE_DATA + " TEXT, " // Private info of the message. Stored in JSON format
				+ DBConstants.GROUP_PARTICIPANT + " TEXT, " // The MSISDN of the participant that sent the message (Only for groups)
				+ DBConstants.IS_HIKE_MESSAGE + " INTEGER DEFAULT -1, " // Whether the message is a hike or SMS message.
				+ DBConstants.READ_BY + " TEXT, " // Deprecated
				+ DBConstants.MSISDN + " TEXT, " // The conversation's msisdn. This will be the msisdn for one-to-one and the group id for groups
				+ DBConstants.MESSAGE_HASH + " TEXT DEFAULT NULL, " // Used for duplication checks.
				+ DBConstants.MESSAGE_TYPE + " INTEGER" + " INTEGER DEFAULT -1, " // The type of the message.
				+ DBConstants.HIKE_CONV_DB.LOVE_ID_REL + " INTEGER DEFAULT -1, " // love id applicable to few messages like content
				+ DBConstants.HIKE_CONTENT.CONTENT_ID + " INTEGER DEFAULT -1, " // content id applicable to few messages like content
				+ DBConstants.HIKE_CONTENT.NAMESPACE + " TEXT DEFAULT 'message',"  //namespace for uniqueness of content
				+ DBConstants.SERVER_ID + " INTEGER, "
				+ DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0, " //normal/broadcast/multi-forward
				//This column would contain actual sending time of message in milliseconds. IN CASE OF receiving messages as well, we would have actual SENDING TIME of OTHER CLIENT here.
				+ DBConstants.SEND_TIMESTAMP + " INTEGER, "
				+ DBConstants.SORTING_ID + " INTEGER DEFAULT -1"
				+ " ) ";

		db.execSQL(sql);
		sql=getReceiptsTableCreateStatement();
		db.execSQL(sql);
		sql = DBConstants.CREATE_INDEX + DBConstants.RECEIPTS_TABLE_CONTENT_INDEX + " ON " + DBConstants.RECEIPTS_TABLE + " ( " + DBConstants.MESSAGE_ID + " ) ";
		db.execSQL(sql);

		sql = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_NAMESPACE_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.NAMESPACE + " ) ";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.CONVERSATION_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.CONV_ID + " , " + DBConstants.TIMESTAMP
				+ " DESC" + " )";
		db.execSQL(sql);

		createIndexOverServerIdField(db);

		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.MESSAGE_HASH_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_HASH + " DESC" + " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.CONVERSATIONS_TABLE
				+ " ( "
				+ DBConstants.CONV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Deprecated
				+ DBConstants.ONHIKE + " INTEGER, " // Whether the conversation is on hike or not
				+ DBConstants.CONTACT_ID + " TEXT, " // Deprecated
				+ DBConstants.MSISDN + " UNIQUE, " // The conversation's msisdn. This will be the msisdn for one-to-one and the group id for groups
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, " // Flag. Whether to show the SMS Credits overlay or not.

				// Messages table columns begin (We keep the last entry of messages for a conversation)
				+ DBConstants.MESSAGE + " TEXT, "
				+ DBConstants.MSG_STATUS + " INTEGER, "
				+ DBConstants.LAST_MESSAGE_TIMESTAMP + " INTEGER, "
				+ DBConstants.MESSAGE_ID + " INTEGER, "
				+ DBConstants.MAPPED_MSG_ID + " INTEGER, "
				+ DBConstants.MESSAGE_METADATA + " TEXT, "
				+ DBConstants.PRIVATE_DATA + " TEXT, "
				+ DBConstants.GROUP_PARTICIPANT + " TEXT, "
				+ DBConstants.SERVER_ID + " INTEGER, "
				+ DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0, " // last message origin type
				// Messages table columns end

				+ DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0, " // Whether the message is a status message.
				+ DBConstants.UNREAD_COUNT + " INTEGER DEFAULT 0, " // The unread count for the conversation
				+ DBConstants.IS_STEALTH + " INTEGER DEFAULT 0, " // Whether the conversation is a hidden conversation.
				+ DBConstants.SORTING_TIMESTAMP + " LONG, " //This timestamp will be used for sorting conversation objects
				+ DBConstants.CONVERSATION_METADATA + " TEXT" // Extra info. JSON format
				+ " )";
		db.execSQL(sql);

		sql = getChatPropertiesTableCreateStatement();
		db.execSQL(sql);

		sql = getChatPropertiesIndexCreateStatement();
		db.execSQL(sql);

		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID + " TEXT, " // The group id.
				+ DBConstants.MSISDN + " TEXT, " // Msisdn of the group participant
				+ DBConstants.NAME + " TEXT, " // Name of the participant
				+ DBConstants.ONHIKE + " INTEGER, " // Whether the participant is on hike or not.
				+ DBConstants.HAS_LEFT + " INTEGER, " // Whether the participant has left the group or not.
				+ DBConstants.ON_DND + " INTEGER, " // Whether the participant is on DND or not
				+ DBConstants.SHOWN_STATUS + " INTEGER, " // Whether we have shown a DND status for this participant or not. 
				+ DBConstants.TYPE + " INTEGER  DEFAULT 0 , " // Whether the participant is an admin or not.
				+ DBConstants.HIKE_UID + " TEXT DEFAULT NULL"
				+	")";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN
				+ " ) ";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_INFO_TABLE
				+ " ( "
				+ DBConstants.GROUP_ID + " TEXT PRIMARY KEY, " // The group id
				+ DBConstants.GROUP_NAME + " TEXT, " // Name of the group
				+ DBConstants.GROUP_OWNER + " TEXT, " // Group owner's msisdn
				+ DBConstants.GROUP_ALIVE + " INTEGER, " // Whether the group is alive or not
				+ DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0, " // Whether the group is muted or not
				+ DBConstants.READ_BY + " TEXT, " // An array of the msisdns that have read the message.
				+ DBConstants.MESSAGE_ID + " INTEGER, " // The message id of the message we are showing the read by for.
				+ DBConstants.GROUP_CREATION_TIME + " LONG DEFAULT -1, "  //Group creation time
				+ DBConstants.GROUP_CREATOR + " TEXT DEFAULT NULL" // Group creator's msisdn
				+ " )";
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE
				+ " ( "
				+ DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, " // The index of the emoticon
				+ DBConstants.LAST_USED + " INTEGER" // Timestamp of when it was used
				+ " )";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX + " ON " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " ) ";
		db.execSQL(sql);

		sql = getStatusTableCreationStatement();
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.STATUS_INDEX + " ON " + DBConstants.STATUS_TABLE + " ( " + DBConstants.MSISDN + " ) ";
		db.execSQL(sql);

		sql = getStickerCategoryTableCreateQuery();
		db.execSQL(sql);

		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.PROTIP_TABLE
				+ " ("
				+ DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Protip id
				+ DBConstants.PROTIP_MAPPED_ID + " TEXT UNIQUE, " // Protip id sent by the server
				+ DBConstants.HEADER + " TEXT, "  // Protip header text
				+ DBConstants.PROTIP_TEXT + " TEXT, " // Protip subtext
				+ DBConstants.TIMESTAMP + " INTEGER, " // Protip time stamp
				+ DBConstants.IMAGE_URL + " TEXT, " // Protip image URL
				+ DBConstants.WAIT_TIME + " INTEGER, " // When to show the Tip
				+ DBConstants.PROTIP_GAMING_DOWNLOAD_URL + " TEXT"
				+ " )";
		db.execSQL(sql);
		sql = getSharedMediaTableCreateQuery();
		db.execSQL(sql);
		sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_TABLE
				+ " ("
				+ DBConstants.FILE_KEY + " TEXT PRIMARY KEY, " // File key
				+ DBConstants.IMAGE + " BLOB," // File thumbnail
                + REF_COUNT + " INTEGER"      // number of messages using the thumbnail.
				+ " )";
		db.execSQL(sql);
		sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_INDEX + " ON " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " )";
		db.execSQL(sql);

		sql = CREATE_TABLE + DBConstants.BOT_TABLE
				+ " ("
				+ DBConstants.MSISDN + " TEXT UNIQUE, "        //msisdn of bot
				+ DBConstants.NAME + " TEXT, "				//bot name
				+ DBConstants.CONVERSATION_METADATA + " TEXT, "  //bot metadata
				+ DBConstants.IS_MUTE + " INTEGER DEFAULT 0, "  // bot conv mute or not
				+ DBConstants.BOT_TYPE + " INTEGER DEFAULT 1, "				//bot type m/nm by default messaging
				+ DBConstants.BOT_CONFIGURATION + " INTEGER DEFAULT " + String.valueOf(Integer.MAX_VALUE) + ", "	//bot configurations.. different server controlled properties of bot.
				+ DBConstants.CONFIG_DATA + " TEXT, "            //config data for the bot.
				+ HIKE_CONTENT.NAMESPACE + " TEXT, "         //namespace of a bot for caching purpose.
				+ HIKE_CONTENT.NOTIF_DATA + " TEXT, "       //notif data used for notifications pertaining to the microapp
				+ HIKE_CONTENT.HELPER_DATA + " TEXT DEFAULT '{}', "  //helper data
				+ HIKE_CONTENT.BOT_VERSION + " INTEGER DEFAULT 0,"//bot version for bot upgrade scenario
				+ HIKE_CONTENT.BOT_TRIGGER_POINT + " INTEGER DEFAULT 0, " //by default ... its hould be 0 as not any trigger point
				+ HIKE_CONTENT.CLIENT_ID + " TEXT, "  
				+ HIKE_CONTENT.CLIENT_HASH + " TEXT, "
				+ DBConstants.HIKE_UID + " TEXT " // UID of the bot
				+ ")";
		db.execSQL(sql);
		sql = getActionsTableCreateQuery();
		db.execSQL(sql);
		
		sql = getFeedTableCreateQuery();
		db.execSQL(sql);

		// This table has the data related to the card to card messaging. This table has the data shared among the microapps
		sql = getMessageEventTableCreateStatement();
		db.execSQL(sql);

		sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EVENT_HASH_INDEX + " ON " + DBConstants.MESSAGE_EVENT_TABLE + " ( " + DBConstants.EVENT_HASH + " )";
		db.execSQL(sql);
		
		db.execSQL(getMsisdnAndSortingIdIndex()); //This index is for querying the messages table
		db.execSQL(getSortingIndexQuery()); //This index enables O(1) access for max sort id query, which will be used frequently

		sql = getURLTableCreateStatement();
		db.execSQL(sql);

		// to be aware of the users for whom db upgrade should not be done in future to fix AND-704
		saveCurrentConvDbVersionToPrefs();


		String sqlIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.FEED_INDEX + " ON " + DBConstants.FEED_TABLE + " ( " + DBConstants.FEED_ACTION_ID + ", "
				+ DBConstants.FEED_OBJECT_ID + ", " + DBConstants.FEED_ACTOR + " ) ";
		db.execSQL(sqlIndex);

        sql = getStickerTableCreateQuery();
        db.execSQL(sql);

		sql = getStickerCategoryRankTableCreateQuery();
		db.execSQL(sql);

		sql = getUCIDIndexOnStickerCategoryTable();
		db.execSQL(sql);

		sql = getUCIDIndexOnStickerCategoryRankTable();
		db.execSQL(sql);

		sql = getRecentStickersTableCreateQuery();
		db.execSQL(sql);

		//creating tables for OTA ChatThemes
		sql = getAssetTableCreateQuery();
		db.execSQL(sql);

		sql = getThemeTableCreateQuery();
		db.execSQL(sql);

		sql = getUserParameterTableCreateQuery();
		db.execSQL(sql);

		sql = getParameterMappingTableCreateQuery();
		db.execSQL(sql);
	}

	private void createIndexOverServerIdField(SQLiteDatabase db)
	{
		//creating index over server Id field
		String sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.SERVER_ID_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.SERVER_ID + " DESC" + " )";
		db.execSQL(sql);
	}

	public void deleteAll()
	{
		mDb.delete(DBConstants.CONVERSATIONS_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGES_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, null, null);
		mDb.delete(DBConstants.GROUP_INFO_TABLE, null, null);
		mDb.delete(DBConstants.EMOTICON_TABLE, null, null);
		mDb.delete(DBConstants.STATUS_TABLE, null, null);
		mDb.delete(DBConstants.STICKER_CATEGORIES_TABLE, null, null);
		mDb.delete(DBConstants.PROTIP_TABLE, null, null);
		mDb.delete(DBConstants.SHARED_MEDIA_TABLE, null, null);
		mDb.delete(DBConstants.FILE_THUMBNAIL_TABLE, null, null);
		mDb.delete(DBConstants.CHAT_PROPERTIES_TABLE, null, null);
		mDb.delete(DBConstants.BOT_TABLE, null, null);
		mDb.delete(DBConstants.ACTIONS_TABLE, null, null);
		mDb.delete(DBConstants.FEED_TABLE, null, null);
		mDb.delete(DBConstants.MESSAGE_EVENT_TABLE, null, null);
		mDb.delete(DBConstants.URL_TABLE, null, null);
		mDb.delete(DBConstants.STICKER_TABLE, null, null);
		mDb.delete(DBConstants.STICKER_CATEGORY_RANK_TABLE, null, null);
		mDb.delete(DBConstants.RECENT_STICKERS_TABLE, null, null);
		mDb.delete(DBConstants.RECEIPTS_TABLE,null,null);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (db == null)
		{
			db = mDb;
		}

		if (oldVersion < 2)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.ONHIKE + " INTEGER";
			db.execSQL(alter);
		}

		if (oldVersion < 3)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.ON_DND + " INTEGER";
			db.execSQL(alter);
			alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.SHOWN_STATUS + " INTEGER";
			db.execSQL(alter);
		}

		// This is being done to change the column type of column "name" in the
		// group members table
		if (oldVersion < 4)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " RENAME TO " + "temp_table";

			String dropIndex = "DROP INDEX " + DBConstants.GROUP_INDEX;

			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + " STRING, " + DBConstants.MSISDN + " TEXT, "
					+ DBConstants.NAME + " TEXT, " + DBConstants.ONHIKE + " INTEGER, " + DBConstants.HAS_LEFT + " INTEGER, " + DBConstants.ON_DND + " INTEGER, "
					+ DBConstants.SHOWN_STATUS + " INTEGER " + " )";

			String createIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.GROUP_INDEX + " ON " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", "
					+ DBConstants.MSISDN + " ) ";

			String insert = "INSERT INTO " + DBConstants.GROUP_MEMBERS_TABLE + " SELECT * FROM temp_table";

			String drop = "DROP TABLE IF EXISTS temp_table";

			db.execSQL(alter);
			db.execSQL(dropIndex);
			db.execSQL(create);
			db.execSQL(createIndex);
			db.execSQL(insert);
			db.execSQL(drop);
		}

		// Creating Emotions Table and adding index
		if (oldVersion < 5)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " INTEGER PRIMARY KEY, " + DBConstants.LAST_USED
					+ " INTEGER" + " )";
			db.execSQL(create);
			create = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EMOTICON_INDEX + " ON " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + " ) ";
			db.execSQL(create);
		}

		// Add muteGroup column
		if (oldVersion < 6)
		{
			String alter = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.MUTE_GROUP + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		/*
		 * We won't use the DB to manage the file name and key anymore. Instead we write this data to a file. So we write all the current data in the db to the file.
		 */
		if (oldVersion < 7)
		{
			Cursor table = null;
			Cursor c = null;
			try
			{
				/*
				 * Check if the table exists first.
				 */
				table = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[] { DBConstants.FILE_TABLE });
				/*
				 * Only do something if the table exists.
				 */
				if (table.moveToFirst())
				{

					c = db.query(DBConstants.FILE_TABLE, null, null, null, null, null, null);

					int fileNameIdx = c.getColumnIndex(DBConstants.FILE_NAME);
					int fileKeyIdx = c.getColumnIndex(DBConstants.FILE_KEY);

					JSONObject data = new JSONObject();

					while (c.moveToNext())
					{
						String fileName = c.getString(fileNameIdx);
						String fileKey = c.getString(fileKeyIdx);

						try
						{
							data.put(fileName, fileKey);
						}
						catch (JSONException e)
						{
							Logger.e(getClass().getSimpleName(), "Invalid values");
						}
					}
					Logger.d(getClass().getSimpleName(), "DB data: " + data.toString());
					Utils.makeNewFileWithExistingData(data);

					String drop = "DROP TABLE IF EXISTS " + DBConstants.FILE_TABLE;
					db.execSQL(drop);
				}
			}
			finally
			{
				if (table != null)
				{
					table.close();
				}
				if (c != null)
				{
					c.close();
				}
			}
		}

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 8)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 9)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 10)
		// {
		// }

		// No need to make status table here. We are making a new one in version 12
		// if (oldVersion < 11)
		// {
		// }

		/*
		 * Dropping the earlier status table to ensure the previous statuses (if any) are deleted.
		 */
		if (oldVersion < 12)
		{
			// Drop table is required for user upgrading from build numbers: 8, 9, 10 or 11
			String drop = "DROP TABLE IF EXISTS " + DBConstants.STATUS_TABLE;
			String create = getStatusTableCreationStatement();

			String alter1 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MSG_STATUS + " INTEGER";
			String alter3 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.TIMESTAMP + " INTEGER";
			String alter4 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ID + " INTEGER";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MAPPED_MSG_ID + " INTEGER";
			String alter6 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_METADATA + " TEXT";
			String alter7 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.GROUP_PARTICIPANT + " TEXT";
			String alter8 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.IS_STATUS_MSG + " INTEGER DEFAULT 0";

			db.execSQL(drop);
			db.execSQL(create);
			db.execSQL(alter1);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			db.execSQL(alter6);
			db.execSQL(alter7);
			db.execSQL(alter8);

			denormaliseConversations(db);
		}

		/*
		 * Adding index on MSISDN in status table.
		 */
		if (oldVersion < 13)
		{
			String createIndex = "CREATE INDEX IF NOT EXISTS " + DBConstants.STATUS_INDEX + " ON " + DBConstants.STATUS_TABLE + " ( " + DBConstants.MSISDN + " ) ";
			db.execSQL(createIndex);
		}

		/*
		 * Adding a column to keep a track of the message type i.e hike or sms.
		 */
		if (oldVersion < 14)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.IS_HIKE_MESSAGE + " INTEGER DEFAULT -1";
			db.execSQL(alter);
		}

		// No need to make sticker cateogory table here. We are making a new one in version 30
		//if (oldVersion < 15)
		//{
		//	String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKERS_TABLE + " (" + DBConstants.CATEGORY_ID + " TEXT PRIMARY KEY, " + DBConstants.TOTAL_NUMBER
		//			+ " INTEGER, " + DBConstants.REACHED_END + " INTEGER," + DBConstants.UPDATE_AVAILABLE + " INTEGER" + " )";
		//	db.execSQL(create);
		//}
		/*
		 * Version 16 adds the protips table.
		 */
		if (oldVersion < 16)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.PROTIP_TABLE + " (" + DBConstants.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ DBConstants.PROTIP_MAPPED_ID + " TEXT UNIQUE, " + DBConstants.HEADER + " TEXT, " + DBConstants.PROTIP_TEXT + " TEXT, " + DBConstants.TIMESTAMP + " INTEGER, "
					+ DBConstants.IMAGE_URL + " TEXT, " + DBConstants.WAIT_TIME + " INTEGER" + " )";
			db.execSQL(create);
		}

		/*
		 * Version 17 add the unread column.
		 */
		if (oldVersion < 17)
		{
			String alter = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.UNREAD_COUNT + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		/*
		 * Version 18 adds the shared media and file thumbnail table. We also parse through all the messages to populate these tables.
		 */
		if (oldVersion < 18)
		{
			String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.SHARED_MEDIA_TABLE + " (" + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY, " + DBConstants.CONV_ID
					+ " INTEGER" + " )";
			db.execSQL(create);
			create = "CREATE TABLE IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " TEXT PRIMARY KEY, " + DBConstants.IMAGE + " BLOB" + " )";
			db.execSQL(create);
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 18
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, 1);
			editor.commit();
		}

		/*
		 * Version 19 adds the 'read by' column in the messages table.
		 */
		if (oldVersion < 19)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.READ_BY + " TEXT";
			db.execSQL(alter);
		}

		/*
		 * Version 20 adds an index for the file thumbnails table.
		 */
		if (oldVersion < 20)
		{
			String createIndex = "CREATE INDEX IF NOT EXISTS " + DBConstants.FILE_THUMBNAIL_INDEX + " ON " + DBConstants.FILE_THUMBNAIL_TABLE + " (" + DBConstants.FILE_KEY + " )";
			db.execSQL(createIndex);
		}

		/*
		 * Version 21 adds PROTIP_GAMING_DOWNLOAD_URL to the protip table
		 */
		if (oldVersion < 21)
		{
			String alter = "ALTER TABLE " + DBConstants.PROTIP_TABLE + " ADD COLUMN " + DBConstants.PROTIP_GAMING_DOWNLOAD_URL + " TEXT";
			db.execSQL(alter);
		}

		/*
		 * Version 22 adds the Chat BG table.
		 */
		if (oldVersion < 22)
		{
			String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.ChatThemes.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.ChatThemes.THEME_COL_BG_ID + " TEXT)";
			db.execSQL(sql);
			sql = "CREATE INDEX IF NOT EXISTS " + DBConstants.ChatThemes.CHAT_BG_INDEX + " ON " + DBConstants.ChatThemes.CHAT_BG_TABLE + " (" + DBConstants.MSISDN + ")";
			db.execSQL(sql);
		}

		/*
		 * Version 23 adds the timestamp column to the chat bg table
		 */
		if (oldVersion < 23)
		{
			String alter = "ALTER TABLE " + DBConstants.ChatThemes.CHAT_BG_TABLE + " ADD COLUMN " + DBConstants.TIMESTAMP + " INTEGER";
			db.execSQL(alter);
		}

		/*
		 * Version 24 adds the stealth column to the converations table
		 */
		if (oldVersion < 24)
		{
			String alter = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.IS_STEALTH + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		/*
		 * Version 27 adds the message hash column to the messages table Version 27 adds READ_BY and MESSAGE_ID column in groupInfo table
		 */
		if (oldVersion < 27)
		{
			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MSISDN + " TEXT";
			String alter1 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_HASH + " TEXT DEFAULT NULL";
			String createIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.MESSAGE_HASH_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MESSAGE_HASH
					+ " DESC" + " )";
			String alter2 = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.READ_BY + " TEXT";
			String alter3 = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ID + " INTEGER";
			String alter4 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_TYPE + " INTEGER DEFAULT -1";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.CONVERSATION_METADATA + " TEXT";
			db.execSQL(alter);
			db.execSQL(alter1);
			db.execSQL(createIndex);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 27
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, 1);
			editor.commit();
		}

		/*
		 * Version 28 migrates msisdn entries to message table
		 */
		if (oldVersion < 28)
		{
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 28
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, 1);
			editor.commit();
		}

		/*
		 * Version 29 removes the empty one-to-one conversations
		 */
		if (oldVersion < 29)
		{
			deleteEmptyConversations(db);
		}
		
		/*
		 * Version 30 fixes a Play Store crash where some DBs did not have a moodId column.
		 * We will first query the table and check if this column exists, if not we will drop
		 * the table and create a new one
		 */
		if (oldVersion < 30)
		{
			if (!checkIfStatusTableIsValid(db))
			{
				dropAndRecreateStatusTable(db);
			}
		}

		if (oldVersion < 31)
		{
			db.execSQL(getStickerCategoryTableCreateQuery());
			// Edit the preference to ensure that HikeMessenger app knows we've
			// reached the
			// upgrade flow for version 31
			Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1);
			editor.putInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1);
			editor.commit();
		}

		/**
		 * We introduced content here, so love concept came in message table for content shared by people. Ref count is introduced to keep a track on the number of messages where
		 * this thumbnail is attached.
		 */
		if (oldVersion < 32)
		{
			String alter2 = "ALTER TABLE " + DBConstants.FILE_THUMBNAIL_TABLE + " ADD COLUMN " + REF_COUNT + " INTEGER";
			db.execSQL(alter2);

			String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.HIKE_CONV_DB.LOVE_ID_REL + " INTEGER";
			db.execSQL(alter);
		}

		if (oldVersion < 33)
		{
			String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.BOT_TABLE + " (" + DBConstants.MSISDN + " TEXT UNIQUE, " + DBConstants.NAME + " TEXT, "
					+ DBConstants.CONVERSATION_METADATA + " TEXT)";
			db.execSQL(sql);

		}


		if (oldVersion < 35)
		{
			String alter = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.IS_MUTE + " INTEGER DEFAULT 0";
			db.execSQL(alter);
		}

		if(oldVersion < 36)
		{
			String alterContentId = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.HIKE_CONTENT.CONTENT_ID + " INTEGER DEFAULT -1"; // content id applicable to few messages
			db.execSQL(alterContentId);

			String alterNamespace = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + HIKE_CONTENT.NAMESPACE + " TEXT DEFAULT 'message'"; // namespace for uniqueness applicable to few messages
			db.execSQL(alterNamespace);
			String alter1 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			String alter2 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0";
			String alter3 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			//We are adding a new field sorting timestamp to use this for ordering purpose.
			String alter4 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.SORTING_TIMESTAMP + " LONG";
			String alter5 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.MESSAGE_ORIGIN_TYPE + " INTEGER DEFAULT 0";
			String alter6 = "ALTER TABLE " + DBConstants.SHARED_MEDIA_TABLE + " ADD COLUMN " + DBConstants.SERVER_ID + " INTEGER";
			String createIndex1 = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_CONTENT_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.CONTENT_ID + " ) ";
			String createIndex2 = DBConstants.CREATE_INDEX + DBConstants.MESSAGE_TABLE_NAMESPACE_INDEX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.HIKE_CONTENT.NAMESPACE + " ) ";
			
			db.execSQL(alter1);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			db.execSQL(alter6);
			db.execSQL(createIndex1);
			db.execSQL(createIndex2);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_FOR_SERVER_ID_FIELD, 1);
		}

		if (oldVersion < 37)
		{
			String alter1 = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.PRIVATE_DATA + " TEXT";
			db.execSQL(alter1);
			
			String alter2 = "ALTER TABLE " + DBConstants.CONVERSATIONS_TABLE + " ADD COLUMN " + DBConstants.PRIVATE_DATA + " TEXT";
			db.execSQL(alter2);
		}
		if (oldVersion < 39)
		{

			String alter1 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.BOT_TYPE + " INTEGER DEFAULT 1"; // by default messaging.
			String alter2 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.BOT_CONFIGURATION + " INTEGER";
			String alter3 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.CONFIG_DATA + " TEXT";
			String alter4 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.NAMESPACE + " TEXT";
			String alter5 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.NOTIF_DATA + " TEXT";
			String alter6 = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.HELPER_DATA + " TEXT";

			db.execSQL(alter1);
			db.execSQL(alter2);
			db.execSQL(alter3);
			db.execSQL(alter4);
			db.execSQL(alter5);
			db.execSQL(alter6);
		}

        //Add creation time column
		if (oldVersion < 40) {
			String alter = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE
					+ " ADD COLUMN " + DBConstants.GROUP_CREATION_TIME
					+" LONG DEFAULT -1";
			db.execSQL(alter);
		}

		/*
		 * Version 41 has been removed intentionally.
		 * There were different changes made in upgrade to version 41. Some of them went out to beta users in market.
		 * As a result the other additional changes in 41 were not reflected to those users.
		 * So version 41 was discarded and 42 was added instead. Version 42 upgrade adds columns only if they are not already there.
		 * 
		 * To avoid the same in future, it is decided that from now now:
		 * 1. Full db support will be provided for UPGRADING an internal_release to any other internal_release
		 * 2. The beta releases made to the costumers will only be made from internal_release
		 */
		if (oldVersion < 42)
		{
			if (!Utils.isColumnExistsInTable(db, DBConstants.MESSAGES_TABLE, DBConstants.SEND_TIMESTAMP))
			{
				String alter = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.SEND_TIMESTAMP + " LONG DEFAULT -1";
				db.execSQL(alter);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_CREATOR))
			{
				String alter = "ALTER TABLE " + DBConstants.GROUP_INFO_TABLE + " ADD COLUMN " + DBConstants.GROUP_CREATOR + " TEXT DEFAULT NULL";
				db.execSQL(alter);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.GROUP_MEMBERS_TABLE, DBConstants.TYPE))
			{
				String alter = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.TYPE + " INTEGER  DEFAULT 0";
				db.execSQL(alter);
			}
		}


		if(oldVersion < 43)
		{
			String dropLoveTable = "DROP TABLE IF EXISTS " + LOVE_TABLE;
			db.execSQL(dropLoveTable);
			
			String sql = getActionsTableCreateQuery();
			db.execSQL(sql);
			
			sql = getFeedTableCreateQuery();
			db.execSQL(sql);
			
			if (!Utils.isColumnExistsInTable(db, DBConstants.STATUS_TABLE, DBConstants.FILE_KEY))
			{
				String alterST = "ALTER TABLE " + DBConstants.STATUS_TABLE + " ADD COLUMN " + DBConstants.FILE_KEY + " TEXT";
				db.execSQL(alterST);
			}
		}
		
		if (oldVersion < 44)
		{
			// This table has the data related to the card to card messaging. This table has the data shared among the microapps
			String sql = getMessageEventTableCreateStatement();
			db.execSQL(sql);

			String sqlIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.EVENT_HASH_INDEX + " ON " + DBConstants.MESSAGE_EVENT_TABLE + " ( " + DBConstants.EVENT_HASH
					+ " )";
			db.execSQL(sqlIndex);
		}
		
		if (oldVersion < 45)
		{
			if (!Utils.isColumnExistsInTable(db, DBConstants.MESSAGES_TABLE, DBConstants.SORTING_ID))
			{
				String alterMessageTable = "ALTER TABLE " + DBConstants.MESSAGES_TABLE + " ADD COLUMN " + DBConstants.SORTING_ID + " INTEGER DEFAULT -1";
				db.execSQL(alterMessageTable);

				// This indicates that an update happened here. This field will be used by UpgradeIntentService
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_SORTING_ID_FIELD, 1);
			}

			if (!Utils.isColumnExistsInTable(db, DBConstants.BOT_TABLE, HIKE_CONTENT.BOT_VERSION))
			{
				String alterTable = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.BOT_VERSION + " INTEGER DEFAULT 0";
				db.execSQL(alterTable);
			}
		}

		if (oldVersion < 47)
		{
			Long time = System.currentTimeMillis();
			db.execSQL(getMsisdnAndSortingIdIndex()); //This index is for querying the messages table
			db.execSQL(getSortingIndexQuery()); //This index enables O(1) access for max sort id query, which will be used frequently
			Logger.d("HikeConversationsDatabase", "Time taken to create indices for sortingId : " + (System.currentTimeMillis() - time));
		}

		if (oldVersion < 48)
		{
			try
			{
				String sqlIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.FEED_INDEX + " ON " + DBConstants.FEED_TABLE + " ( " + DBConstants.FEED_ACTION_ID + ", "
						+ DBConstants.FEED_OBJECT_ID + ", " + DBConstants.FEED_ACTOR + " ) ";
				db.execSQL(sqlIndex);
			}
			catch (SQLiteException sqe)
			{
				db.delete(DBConstants.FEED_TABLE, null, null);
				String sqlIndex = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.FEED_INDEX + " ON " + DBConstants.FEED_TABLE + " ( " + DBConstants.FEED_ACTION_ID + ", "
						+ DBConstants.FEED_OBJECT_ID + ", " + DBConstants.FEED_ACTOR + " ) ";
				db.execSQL(sqlIndex);
			}
		
		}

		if(oldVersion < 49)
        {
			String alter = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.BOT_TRIGGER_POINT + " INTEGER DEFAULT 0";
			db.execSQL(alter);

			String sql = getURLTableCreateStatement();
			db.execSQL(sql);

			sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + DBConstants.URL_KEY_INDEX + " ON " + DBConstants.URL_TABLE + " ( " + DBConstants.URL_KEY + " )";
			db.execSQL(sql);
			
			sql = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.CLIENT_ID + " TEXT";
			db.execSQL(sql);
			
			sql = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + HIKE_CONTENT.CLIENT_HASH + " TEXT";
			db.execSQL(sql);

            if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.CATEGORY_DESCRIPTION))
			{
				String alter1 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.CATEGORY_DESCRIPTION + " TEXT";
				db.execSQL(alter1);
			}
			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.STICKER_LIST))
			{
				String alter2 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.STICKER_LIST + " TEXT";
				db.execSQL(alter2);
			}
			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.IS_DOWNLOADED))
			{
				String alter3 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.IS_DOWNLOADED + " INTEGER DEFAULT 0";
				db.execSQL(alter3);
			}
			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.SIMILAR_CATEGORIES))
			{
				String alter4 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.SIMILAR_CATEGORIES + " TEXT";
				db.execSQL(alter4);
			}

			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.AUTHOR))
			{
				String alter4 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.AUTHOR + " TEXT";
				db.execSQL(alter4);
			}

			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.COPYRIGHT_STRING))
			{
				String alter4 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.COPYRIGHT_STRING + " TEXT";
				db.execSQL(alter4);
			}
			 sql = getStickerTableCreateQuery();
			db.execSQL(sql);

        }
		if(oldVersion < 50)
		{
			if (!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.UCID))
			{
				String alter1 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.UCID + " INTEGER ";
				db.execSQL(alter1);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.UPDATED_PREVIEW_TIMESTAMP))
			{
				String alter2 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.UPDATED_PREVIEW_TIMESTAMP + " INTEGER DEFAULT 0";
				db.execSQL(alter2);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.UPDATED_METADATA_TIMESTAMP))
			{
				String alter3 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.UPDATED_METADATA_TIMESTAMP + " INTEGER DEFAULT 0";
				db.execSQL(alter3);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.STICKER_CATEGORIES_TABLE, DBConstants.IS_DISABLED))
			{
				String alter4 = "ALTER TABLE " + DBConstants.STICKER_CATEGORIES_TABLE + " ADD COLUMN " + DBConstants.IS_DISABLED + " INTEGER DEFAULT 0";
				db.execSQL(alter4);
			}

			String sql = getStickerCategoryRankTableCreateQuery();
			db.execSQL(sql);

			sql = getUCIDIndexOnStickerCategoryTable();
			db.execSQL(sql);

			sql = getUCIDIndexOnStickerCategoryRankTable();
            db.execSQL(sql);

			sql = getRecentStickersTableCreateQuery();
			db.execSQL(sql);

		}


		if(oldVersion < 51)
		{
			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_TABLE, DBConstants.QUICK_SUGGESTED_REPLY_STICKERS))
			{
				String alter1 = "ALTER TABLE " + DBConstants.STICKER_TABLE + " ADD COLUMN " + DBConstants.QUICK_SUGGESTED_REPLY_STICKERS + " TEXT";
				db.execSQL(alter1);
			}

			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_TABLE, DBConstants.QUICK_SUGGESTED_SENT_STICKERS))
			{
				String alter2 = "ALTER TABLE " + DBConstants.STICKER_TABLE + " ADD COLUMN " + DBConstants.QUICK_SUGGESTED_SENT_STICKERS + " TEXT";
				db.execSQL(alter2);
			}

			if(!Utils.isColumnExistsInTable(db, DBConstants.STICKER_TABLE, DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME))
			{
				String alter3 = "ALTER TABLE " + DBConstants.STICKER_TABLE + " ADD COLUMN " + DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME + " INTEGER DEFAULT 0";
				db.execSQL(alter3);
			}
		}

		if(oldVersion < 52)
		{
			//creating tables for OTA ChatThemes
			//adding a prevThemeId column to the chat_bg_table
			if(!Utils.isColumnExistsInTable(db, DBConstants.ChatThemes.CHAT_BG_TABLE, ChatThemes.PREV_THEME_ID_COL))
			{
				String addPrevThemeIdCol = "ALTER TABLE " + DBConstants.ChatThemes.CHAT_BG_TABLE + " ADD COLUMN "
						+ ChatThemes.PREV_THEME_ID_COL + COLUMN_TYPE_TEXT + " DEFAULT '0'";
				db.execSQL(addPrevThemeIdCol);
			}

			String assetTableQuery = getAssetTableCreateQuery();
			db.execSQL(assetTableQuery);

			String themeTableQuery = getThemeTableCreateQuery();
			db.execSQL(themeTableQuery);
		}

		if(oldVersion < 53)
		{
			// Adding UID coloum in Bots Table
			if (!Utils.isColumnExistsInTable(db, DBConstants.BOT_TABLE, DBConstants.HIKE_UID))
			{
				String sql = "ALTER TABLE " + DBConstants.BOT_TABLE + " ADD COLUMN " + DBConstants.HIKE_UID + " TEXT";
				db.execSQL(sql);
			}

			if (!Utils.isColumnExistsInTable(db, DBConstants.GROUP_MEMBERS_TABLE, DBConstants.HIKE_UID))
			{
				String sql = "ALTER TABLE " + DBConstants.GROUP_MEMBERS_TABLE + " ADD COLUMN " + DBConstants.HIKE_UID + " TEXT DEFAULT NULL";
				db.execSQL(sql);
			}

		}

		if(oldVersion < 54)
		{
			// adding EVENT_PARENT_MSISDN and EVENT_FROM_USER_MSISDN column in MESSAGE_EVENT_TABLE
			if (!Utils.isColumnExistsInTable(db, DBConstants.MESSAGE_EVENT_TABLE, DBConstants.EVENT_PARENT_MSISDN))
			{
				String alter1 = "ALTER TABLE " + DBConstants.MESSAGE_EVENT_TABLE + " ADD COLUMN " + DBConstants.EVENT_PARENT_MSISDN + " TEXT";
				db.execSQL(alter1);
			}
			if (!Utils.isColumnExistsInTable(db, DBConstants.MESSAGE_EVENT_TABLE, DBConstants.EVENT_FROM_USER_MSISDN))
			{
				String alter2 = "ALTER TABLE " + DBConstants.MESSAGE_EVENT_TABLE + " ADD COLUMN " + DBConstants.EVENT_FROM_USER_MSISDN + " TEXT";
				db.execSQL(alter2);
			}

		}


		if(oldVersion < 55)
		{
			String sql = "DROP TABLE IF EXISTS " + DBConstants.STICKER_SHOP_TABLE;
			db.execSQL(sql);
		}

		if(oldVersion < 56)
		{
			String create = getChatPropertiesTableCreateStatement();
			db.execSQL(create);

			String index = getChatPropertiesIndexCreateStatement();
			db.execSQL(index);
		}

		if(oldVersion<57)
		{

			String sql=getReceiptsTableCreateStatement();
			db.execSQL(sql);
			sql = DBConstants.CREATE_INDEX + DBConstants.RECEIPTS_TABLE_CONTENT_INDEX + " ON " + DBConstants.RECEIPTS_TABLE + " ( " + DBConstants.MESSAGE_ID + " ) ";
			db.execSQL(sql);
		}

		if (oldVersion < 58)
		{
			String sql = getUserParameterTableCreateQuery();
			db.execSQL(sql);

			sql = getParameterMappingTableCreateQuery();
			db.execSQL(sql);
		}
	}

	public void reinitializeDB()
	{
		Logger.d(getClass().getSimpleName(), "Reinitialising conversation DB");
		close();
		Logger.d(getClass().getSimpleName(), "Conversation DB is closed now");
		
		hikeConversationsDatabase = new HikeConversationsDatabase(HikeMessengerApp.getInstance());
		/*
		 * We can remove this line, if we can guarantee, NoOne keeps a local copy of HikeConversationsDatabase. 
		 * right now we store convDb reference in some classes and use that refenence to query db. ex. DbConversationListener. 
		 * i.e. on restore we have two objects of HikeConversationsDatabase in memory.
		 */
		hikeConversationsDatabase.initializeIfRequiredAndGetWriteDatabase();
		Logger.d(getClass().getSimpleName(), "Conversation DB initialization is complete");
	}

	public void clearTable(String table)
	{
		mDb.delete(table, null, null);
	}

	private String getStatusTableCreationStatement()
	{
		return "CREATE TABLE IF NOT EXISTS " + DBConstants.STATUS_TABLE
				+ " ("
				+ DBConstants.STATUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // Status id
				+ DBConstants.STATUS_MAPPED_ID + " TEXT UNIQUE, " // Status id sent by the server
				+ DBConstants.MSISDN + " TEXT, " // Msisdn of the person who generated the status
				+ DBConstants.STATUS_TEXT + " TEXT, " // Text of the status
				+ DBConstants.STATUS_TYPE + " INTEGER, " // Type of status
				+ DBConstants.TIMESTAMP + " INTEGER, " // Time stamp of status
				+ DBConstants.MESSAGE_ID + " INTEGER DEFAULT 0, " // Message id of the message this status generated in the messages table. Only valid if status is received when a one to one conversation exists.
				+ DBConstants.SHOW_IN_TIMELINE + " INTEGER, " // Whether this status should be shown in the timeline or not.
				+ DBConstants.MOOD_ID + " INTEGER, " // The mood id of the status
				+ DBConstants.TIME_OF_DAY + " INTEGER, " // Deprecated.
				+ DBConstants.FILE_KEY + " TEXT" // Text of the status
				+ " )";
	}

	/**
	 * This method checks if the status table has a mood id column. We had to add this to fix a bug
	 * where this column did not exist for a few users.
	 * @param db
	 * @return
	 */
	private boolean checkIfStatusTableIsValid(SQLiteDatabase db)
	{
		Cursor c = null;
		try
		{
			c = db.query(DBConstants.STATUS_TABLE, new String[] { DBConstants.MOOD_ID }, null, null, null, null, null);
			return true;
		}
		catch (SQLiteException e)
		{
			Logger.w(getClass().getSimpleName(), "Mood id column does not exist");
			return false;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private String getMessageEventTableCreateStatement()
	{
		return CREATE_TABLE + DBConstants.MESSAGE_EVENT_TABLE
				+ " ("
				+ DBConstants.EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ DBConstants.MESSAGE_HASH + " TEXT, "        //message hash of the message that is related to the event.
				+ DBConstants.EVENT_METADATA + " TEXT, "      //the card to card messaging shared data
				+ DBConstants.EVENT_STATUS + " INTEGER, "    //current status of the event... sent or received
				+ DBConstants.EVENT_TYPE + " INTEGER, "      //whether shared message or an event
				+ DBConstants.TIMESTAMP + " INTEGER, " // Event time stamp
				+ DBConstants.MAPPED_EVENT_ID + " INTEGER, " // The message id of the message on the sender's side (Only applicable for received messages)
				+ DBConstants.MSISDN + " TEXT, " // The conversation's msisdn. This will be the msisdn for one-to-one and the group id for groups
				+ DBConstants.EVENT_PARENT_MSISDN + " TEXT, " // The conversation's parent msisdn. This will be the parent msisdn for one-to-one and the group id for groups
				+ DBConstants.EVENT_FROM_USER_MSISDN + " TEXT, " // The user id of the person sending the event
				+ DBConstants.EVENT_HASH + " TEXT DEFAULT NULL, " // Used for duplication checks.
				+ HIKE_CONTENT.NAMESPACE + " TEXT DEFAULT 'message'"  //namespace for uniqueness of content
				+ ")";
	}

	private String getChatPropertiesTableCreateStatement()
	{
		return "CREATE TABLE IF NOT EXISTS " + DBConstants.CHAT_PROPERTIES_TABLE
				+ " ("
				+ DBConstants.MSISDN + " TEXT UNIQUE, " // Msisdn or group id
				+ DBConstants.ChatThemes.THEME_COL_BG_ID + " TEXT, " // Chat theme id
				+ DBConstants.BG_TIMESTAMP + " INTEGER, " // Timestamp when this theme was changed.
				+ DBConstants.ChatThemes.PREV_THEME_ID_COL + COLUMN_TYPE_TEXT + " DEFAULT '0', " // Prev theme id set for the chat
				+ DBConstants.IS_MUTE + " INTEGER DEFAULT 0, "  // conv mute or not
				+ DBConstants.MUTE_TIMESTAMP + " INTEGER DEFAULT 0, " // Timestamp when this mute pref was changed.
				+ DBConstants.MUTE_DURATION + " INTEGER DEFAULT 0, " //Time duration for which this msisdn is muted
				+ DBConstants.MUTE_NOTIFICATION + " INTEGER DEFAULT 0, " //
				+ DBConstants.MUTE_END_TIME + " INTEGER "
				+ ")";
	}

	private String getChatPropertiesIndexCreateStatement()
	{
		return "CREATE INDEX IF NOT EXISTS " + DBConstants.CHAT_PROPERTIES_INDEX + " ON " + DBConstants.CHAT_PROPERTIES_TABLE + " (" + DBConstants.MSISDN + ")";
	}

	private void dropAndRecreateStatusTable(SQLiteDatabase db)
	{
		String drop = "DROP TABLE IF EXISTS " + DBConstants.STATUS_TABLE;
		String create = getStatusTableCreationStatement();

		db.execSQL(drop);
		db.execSQL(create);
	}

	public int updateOnHikeStatus(String msisdn, boolean onHike)
	{
		Cursor conversationCursor = null;
		Cursor groupMemberCursor = null;
		try
		{
			String selection = DBConstants.MSISDN + "=?";
			String[] args = { msisdn };

			conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			groupMemberCursor = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.ONHIKE }, selection, args, null, null, null);

			ContentValues values = new ContentValues();
			values.put(DBConstants.ONHIKE, onHike);

			int rowsUpdated = 0;

			if (conversationCursor.moveToFirst())
			{

				boolean prevOnHikeVal = conversationCursor.getInt(conversationCursor.getColumnIndex(DBConstants.ONHIKE)) == 1;

				if (prevOnHikeVal != onHike)
				{
					rowsUpdated += mDb.update(DBConstants.CONVERSATIONS_TABLE, values, selection, args);
				}
			}

			if (groupMemberCursor.moveToFirst())
			{

				boolean prevOnHikeVal = groupMemberCursor.getInt(groupMemberCursor.getColumnIndex(DBConstants.ONHIKE)) == 1;

				if (prevOnHikeVal != onHike)
				{
					rowsUpdated += mDb.update(DBConstants.GROUP_MEMBERS_TABLE, values, selection, args);
				}
			}

			return rowsUpdated;

		}
		finally
		{
			if (conversationCursor != null)
			{
				conversationCursor.close();
			}
			if (groupMemberCursor != null)
			{
				groupMemberCursor.close();
			}
		}
	}

	private String getStickerCategoryRankTableCreateQuery()
	{
		String sql = CREATE_TABLE +
				     DBConstants.STICKER_CATEGORY_RANK_TABLE + " ( " +
                     DBConstants.RANK + " INTEGER, " +
                     DBConstants.UCID + " INTEGER PRIMARY KEY, " +
				     DBConstants.IS_PACK_METADATA_UPDATED + " INTEGER DEFAULT 0, " +
                     DBConstants.IS_PACK_TAGDATA_UPDATED + " INTEGER DEFAULT 0)";

		return sql;
	}


	public void updateAdminStatus(String msisdn)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.TYPE, 0);
		mDb.updateWithOnConflict(DBConstants.GROUP_MEMBERS_TABLE, values, MSISDN + "=?", new String[]{msisdn}, SQLiteDatabase.CONFLICT_REPLACE);
	}

	/**
	 * Adds single message to database
	 *
	 * @param message
	 *            - message to be added to database
	 * @return result of {@link #addConversations(List)} function
	 */
	public boolean addConversationMessages(ConvMessage message, boolean createConvIfNotExist)
	{
		List<ConvMessage> l = new ArrayList<ConvMessage>(1);
		l.add(message);
		return addConversations(l, createConvIfNotExist);
	}

	public int updateMsgStatus(long msgID, int val, String msisdn)
	{
		String initialWhereClause = DBConstants.MESSAGE_ID + " =" + String.valueOf(msgID);

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, val, msisdn);
	}

	public int updateMsgStatusBetween(long startMsgId, long endMsgId, int val, String msisdn)
	{
		String initialWhereClause = DBConstants.MESSAGE_ID + " BETWEEN " + startMsgId + " AND " + endMsgId;

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, val, msisdn);
	}

	/**
	 * 
	 * @param msisdn
	 * @param maxMsgId
	 * @return
	 * 
	 * 	Get a list of MsgId that are going to be marked as R.
	 */
	public ArrayList<Long> getCurrentUnreadMessageIdsForMsisdn(String msisdn, long maxMsgId)
	{
		ArrayList<Long> ids = new ArrayList<Long>();

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID,  DBConstants.MSG_STATUS, DBConstants.MESSAGE_ORIGIN_TYPE }, DBConstants.MSISDN + "=? AND "+ DBConstants.MESSAGE_ID + "<=? AND " + DBConstants.MSG_STATUS + "<"
					+ State.SENT_DELIVERED_READ.ordinal(), new String[] { msisdn, String.valueOf(maxMsgId) }, null, null, null);

			//c=getCursor( msisdn,maxMsgId);
			while (c.moveToNext())
			{
				long id = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				int msgStatus = c.getInt(c.getColumnIndex(DBConstants.MSG_STATUS));
				int msgOriginType = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ORIGIN_TYPE));
				/*
				 * 1. if message is not a broadcast one than its fine this is message is a right candidate for
				 * force R.
				 * 2. if message is broadcast then only if its status is Delivered we chhose this as a candidate for
				 * force R.
				 * 3.If we are connected in OfflineMode then we allow only those message for R that are delivered.As our single tick msg
				 * we getting R due to this check.This check is only application for offline mode and it should not hamper online mode.
				 * 
				 */
				if (ConvMessage.originTypeValue(msgOriginType) != OriginType.BROADCAST
						|| ConvMessage.stateValue(msgStatus) == State.SENT_DELIVERED)
				{
					if(!(OfflineUtils.isConnectedToSameMsisdn(msisdn)&&(ConvMessage.stateValue(msgStatus).ordinal() < State.SENT_DELIVERED.ordinal())))
							ids.add(id);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return ids;
	}

	public ArrayList<Long>  setAllDeliveredMessagesReadForMsisdn(String msisdn, ArrayList<Long> msgIds)
	{
		long maxMsgId = Utils.getMaxLongValue(msgIds);
		ArrayList<Long> messageIdsToBeUpdated = getCurrentUnreadMessageIdsForMsisdn(msisdn, maxMsgId);
		Logger.d(AnalyticsConstants.MSG_REL_TAG, "For mr/nmr, Unread Msg Ids for maxMsgTd: " + maxMsgId +" , messageIdsToBeUpdated: "+ messageIdsToBeUpdated);
		
		if(messageIdsToBeUpdated == null || messageIdsToBeUpdated.isEmpty())
		{
			return null;
		}
		
		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + Utils.valuesToCommaSepratedString(messageIdsToBeUpdated);

		int status = State.SENT_DELIVERED_READ.ordinal();

		String query = initialWhereClause;

		executeUpdateMessageStatusStatement(query, status, msisdn);

		return messageIdsToBeUpdated;
	}
	public void setAllDeliveredMessageReceiptsReadforMsisdn(String msisdn,ArrayList<Long> messageIdsTobeUpdated,long timestamp){
		if(messageIdsTobeUpdated==null||messageIdsTobeUpdated.isEmpty()){
			return ;
		}
		//Only for testing will be disabled later on
		Logger.d("messageinfodata","logging in database msisdn "+msisdn);
		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + Utils.valuesToCommaSepratedString(messageIdsTobeUpdated);
		initialWhereClause = initialWhereClause
			+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.RECEIVER_MSISDN + " =" + DatabaseUtils.sqlEscapeString(msisdn)) : "");
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.READ_TIMESTAMP, timestamp);
		int numRows = mDb.update(DBConstants.RECEIPTS_TABLE, contentValues, initialWhereClause, null);
		Logger.d("delivery", "Got read receipt numRows updated is " + numRows);


	}
	public void setReceiptsReadByGroupMsisdn(String msisdn,ArrayList<Long> messageIdsTobeUpdated,long timestamp){
		if(messageIdsTobeUpdated==null||messageIdsTobeUpdated.isEmpty()){
			return ;
		}
		//Only for testing will be disabled later on
		Logger.d("delivery","logging in database msisdn readby group "+msisdn);
		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + Utils.valuesToCommaSepratedString(messageIdsTobeUpdated);
		initialWhereClause = initialWhereClause
			+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.RECEIVER_MSISDN + " =" + DatabaseUtils.sqlEscapeString(msisdn)) : "");
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.READ_TIMESTAMP, timestamp);
		int numRows = mDb.update(DBConstants.RECEIPTS_TABLE, contentValues, initialWhereClause, null);

	}

	/**
	 *
	 * @param groupId
	 *            -- groupId of group for which mr packet came
	 * @param ids
	 *            -- list of ids present in mr packet
	 * @param msisdn
	 *            -- partipant msisdn from which mr packet came
	 * @return id -- maxMsgId from list of ids that are sent by user. If ids doesn't contains any id sent by user return -1
	 */
	public long setReadByForGroup(String groupId, ArrayList<Long> ids, String msisdn)
	{
		Cursor c = null;
		Cursor conversationCursor = null;
		try
		{
			mDb.beginTransaction();
			conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null,
					null, null);

			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
					null, null, null);

			if (!conversationCursor.moveToFirst())
			{
				return -1;

			}

			if (c.moveToFirst())
			{
				String readByString = null;
				long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

				ContentValues contentValues = new ContentValues();

				long maxMsgId = getMrIdForGroup(groupId, ids);			// get max sent message id from list of ids

				if(maxMsgId >= msgId)			// we are updating readBy string if max message id is greater than or equal to that present in group info table
				{
					readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));

					JSONArray readByArray;
					try
					{
						if (TextUtils.isEmpty(readByString))
						{
							readByArray = new JSONArray();
						}
						else
						{
							readByArray = new JSONArray(readByString);
						}
					}
					catch (JSONException e)
					{
						Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
						readByArray = new JSONArray();
					}
					/*
					 * Checking if this number has already been added.
					 */
					boolean alreadyAdded = false;
					for (int i = 0; i < readByArray.length(); i++)
					{
						if (readByArray.optString(i).equals(msisdn))
						{
							alreadyAdded = true;
							break;
						}
					}
					if (!alreadyAdded)
					{
						readByArray.put(msisdn);
						contentValues.put(DBConstants.READ_BY, readByArray.toString());
						mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
					}
				}
				if (maxMsgId > 0)
				{

					long conversationMsgId = conversationCursor.getLong(conversationCursor.getColumnIndex(DBConstants.MESSAGE_ID));
					int minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
					int maxStatusOrdinal = State.SENT_DELIVERED_READ.ordinal();

					/*
					 * Making sure we only set the status of sent messages.
					 */
					contentValues.clear();
					String whereClause = DBConstants.MESSAGE_ID + " <= " + maxMsgId + " AND " + DBConstants.MSG_STATUS + " > " + minStatusOrdinal + " AND "
							+ DBConstants.MSG_STATUS + " < " + maxStatusOrdinal + " AND " + DBConstants.MSISDN + "=?";
					contentValues.put(DBConstants.MSG_STATUS, State.SENT_DELIVERED_READ.ordinal());
					mDb.update(DBConstants.MESSAGES_TABLE, contentValues, whereClause, new String[] { groupId });

					if (conversationMsgId == maxMsgId)
					{
						mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { groupId });
					}
				}

				mDb.setTransactionSuccessful();
				return maxMsgId;
			}

		}
		finally
		{
			if (c != null)
			{
				c.close();
			}

			if (conversationCursor != null)
			{
				conversationCursor.close();
			}

			mDb.endTransaction();
		}
		return -1;
	}

	public void setReadByForGroupBulk(Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap)
	{

		long maxMsgId = -1;
		Cursor c = null;
		Cursor conversationCursor = null;
		for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet()) // Iterate through status map
		{

			String groupId = entry.getKey();
			PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
			maxMsgId = pair.getFirst().getFirst();

			if (maxMsgId == -1)
			{
				continue;
			}
			try
			{
				c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + " =? ", new String[] { groupId },
						null, null, null);

				conversationCursor = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?", new String[] { groupId }, null,
						null, null);

				if (!conversationCursor.moveToFirst())
				{
					continue;
				}
				if (c.moveToFirst())
				{
					String readByString = null;
					long msgId = c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID));

					if (maxMsgId >= msgId)
					{
						readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
					}
					else
					{
						continue;
					}

					JSONArray readByArray;
					try
					{
						if (TextUtils.isEmpty(readByString))
						{
							readByArray = new JSONArray();
						}
						else
						{
							readByArray = new JSONArray(readByString);
						}

					}
					catch (JSONException e)
					{
						Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
						readByArray = new JSONArray();
					}
					/*
					 * Checking if this number has already been added.
					 */
					for (String msisdn : pair.getFirst().getSecond())
					{
						boolean alreadyAdded = false;
						int length = readByArray.length();

						for (int i = 0; i < length; i++)
						{
							if (readByArray.optString(i).equals(msisdn))
							{
								alreadyAdded = true;
								break;
							}
						}
						if (!alreadyAdded)
						{
							readByArray.put(msisdn);
						}
					}

					long conversationMsgId = conversationCursor.getLong(conversationCursor.getColumnIndex(DBConstants.MESSAGE_ID));

					ContentValues contentValues = new ContentValues();
					if (conversationMsgId == maxMsgId)
					{
						contentValues.put(DBConstants.MSG_STATUS, State.SENT_DELIVERED_READ.ordinal());
						mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { groupId });
					}
					contentValues.clear();
					contentValues.put(DBConstants.READ_BY, readByArray.toString());
					mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });

				}
			}
			finally
			{
				if (c != null)
				{
					c.close();
				}
				if (conversationCursor != null)
				{
					conversationCursor.close();
				}
			}
		}
	}

	public void setMessageState(String msisdn, long msgId, int status)
	{
		if (msgId == -1)
		{
			return;
		}
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (status <= State.SENT_DELIVERED_READ.ordinal())
		{
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = status;
		}
		else
		{
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = status;
		}
		Cursor c = null;
		long[] ids = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID }, DBConstants.MSISDN + "=?  AND " + DBConstants.MSG_STATUS + ">=" + minStatusOrdinal
					+ " AND " + DBConstants.MSG_STATUS + "<" + maxStatusOrdinal + " AND " + DBConstants.MESSAGE_ID + "<=" + msgId, new String[] { msisdn }, null, null, null);

			ids = new long[c.getCount()];
			int i = 0;
			while (c.moveToNext())
			{
				long id = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				ids[i++] = id;
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		if (ids != null)
		{
			updateBatch(ids, status, msisdn);
		}
	}

	public int updateBatch(long[] ids, int status, String msisdn)
	{
		StringBuilder sb = new StringBuilder("(");
		/* TODO make utils.join work for arrays */
		for (int i = 0; i < ids.length; i++)
		{
			sb.append(ids[i]);
			if (i != ids.length - 1)
			{
				sb.append(",");
			}
		}
		sb.append(")");

		String initialWhereClause = DBConstants.MESSAGE_ID + " in " + sb.toString();

		String query = initialWhereClause;

		return executeUpdateMessageStatusStatement(query, status, msisdn);
	}

	public void updateStatusBulk(Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap)
	{

		String msisdn;
		for (Entry<String, PairModified<PairModified<Long, Set<String>>, Long>> entry : messageStatusMap.entrySet())
		{

			msisdn = (String) entry.getKey();
			PairModified<PairModified<Long, Set<String>>, Long> pair = entry.getValue();
			setMessageState(msisdn, pair.getFirst().getFirst(), State.SENT_DELIVERED_READ.ordinal());
			setMessageState(msisdn, pair.getSecond(), State.SENT_DELIVERED.ordinal());
		}
	}
	 public long executeMessageDeliveryReceipt(ContentValues contentValues){
		 //Only for testing will be disabled later on
		 Logger.d("messageinfodata","logging in database messagedeliveryreceipt execute " );
		 long rowID=-1;
		 try {
			 rowID = mDb.insert(DBConstants.RECEIPTS_TABLE, null, contentValues);
		 }catch (SQLiteException e)
		 {
			 e.printStackTrace();
		 }
		return rowID;
	 }
	public void saveDeliveryReceipt(long msgId,String fromMsisdn,long timestamp,String toGroupOrSingleMsisdn){
		ContentValues contentValues=new ContentValues();
		contentValues.put(DBConstants.MESSAGE_ID,msgId);
		contentValues.put(DBConstants.RECEIVER_MSISDN,fromMsisdn);
		contentValues.put(DBConstants.DELIVERY_TIMESTAMP,timestamp);
		contentValues.put(DBConstants.MSISDN,toGroupOrSingleMsisdn);
		executeMessageDeliveryReceipt(contentValues);
	}
	public int executeUpdateMessageStatusStatement(String updateStatement, int status, String msisdn)
	{
		int minStatusOrdinal;
		int maxStatusOrdinal;
		if (status <= State.SENT_DELIVERED_READ.ordinal())
		{
			minStatusOrdinal = State.SENT_UNCONFIRMED.ordinal();
			maxStatusOrdinal = status;
		}
		else
		{
			minStatusOrdinal = State.RECEIVED_UNREAD.ordinal();
			maxStatusOrdinal = status;
		}

		updateStatement = updateStatement + " AND " + DBConstants.MSG_STATUS + " >= " + minStatusOrdinal + " AND " + DBConstants.MSG_STATUS + " <= " + maxStatusOrdinal
				+ (!TextUtils.isEmpty(msisdn) ? (" AND " + DBConstants.MSISDN + " =" + DatabaseUtils.sqlEscapeString(msisdn)) : "");

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSG_STATUS, status);
		int numRows = mDb.update(DBConstants.MESSAGES_TABLE, contentValues, updateStatement, null);

		if (status == State.RECEIVED_READ.ordinal())
		{
			contentValues.put(DBConstants.UNREAD_COUNT, 0);
		}
		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, updateStatement, null);

		return numRows;
	}
	
	public boolean addActivityUpdate(FeedDataModel feedData)
	{
		boolean isUpdated = false;
		
		if (feedData.getActionType() == ActionTypes.LIKE)
		{
			isUpdated = addActivityLike(feedData);
		}
		else if (feedData.getActionType() == ActionTypes.UNLIKE)
		{
			isUpdated = deleteActivityLike(feedData);
		}
		else if (feedData.getActionType() == ActionTypes.VIEW)
		{
			// Later versions
		}
		else if (feedData.getActionType() == ActionTypes.COMMENT)
		{
			// Later versions
		}

		return isUpdated;
	}

	private boolean addActivityLike(FeedDataModel feedData)
	{
		boolean isComplete = false;
		ContentValues conVal = new ContentValues();
		conVal.put(DBConstants.FEED_OBJECT_TYPE, feedData.getObjType().getTypeString());
		conVal.put(DBConstants.FEED_OBJECT_ID, feedData.getObjID());
		conVal.put(DBConstants.FEED_ACTION_ID, ActionsDataModel.ActionTypes.LIKE.getKey());
		conVal.put(DBConstants.FEED_ACTOR, feedData.getActor());
		conVal.put(DBConstants.FEED_TS, feedData.getTimestamp());

		String whereQuery = DBConstants.FEED_TS + " = ? AND " + DBConstants.FEED_ACTOR + " = ?";

		String[] whereArgs = new String[] { Long.toString(feedData.getTimestamp()), feedData.getActor() };

		Cursor cursor = mDb.query(DBConstants.FEED_TABLE, null, whereQuery, whereArgs, null, null, null, new Integer(1).toString());

		if (cursor.moveToFirst())
		{
			return false;
		}

		long rowID = mDb.insert(DBConstants.FEED_TABLE, null, conVal);

		if (rowID == -1L)
		{
			isComplete = false;
		}
		else
		{
			isComplete = true;
			Logger.d(HikeConstants.TIMELINE_LOGS, "Adding Like into DB " + feedData);
		}

		if (isComplete)
		{
			ArrayList<String> actorList = new ArrayList<String>();
			actorList.add(feedData.getActor());
			changeActionCountForObjID(feedData.getObjID(), feedData.getObjType().getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, true);

			// Fire UPDATE_ACTIVITY_FEED_ICON_NOTIFICATION pubsub
			fireUpdateNotificationIconPubsub(TimelineActivity.FETCH_FEED_FROM_DB);
		}

		return isComplete;
	}
	
	public int changeActionCountForObjID(String objID, String objType, int actionType, List<String> actors, boolean toIncrement)
	{
		Cursor c = null;

		try
		{
			String whereQuery = DBConstants.ACTION_OBJECT_ID + " = ? AND " + DBConstants.ACTION_OBJECT_TYPE + " = ? AND " + DBConstants.ACTION_ID + " = ?";

			String[] whereArgs = new String[] { objID, objType, String.valueOf(actionType) };

			String[] requiredColumns = new String[] { DBConstants.ACTORS };

			// Update in actions table as well
			c = mDb.query(DBConstants.ACTIONS_TABLE, requiredColumns, whereQuery, whereArgs, null, null, null);

			int cIdxActors = c.getColumnIndexOrThrow(DBConstants.ACTORS);

			if (c.moveToFirst())
			{
				ContentValues cv = new ContentValues();

				String actorsJSON = c.getString(cIdxActors);

				JSONArray existingArray = null;
				try
				{
					existingArray = new JSONArray(actorsJSON);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					return -1;
				}
				
				Logger.d("changeActionCountForObjID", "Initial array: "+existingArray.toString());

				if (toIncrement)
				{
					for (String msisdn : actors)
					{
						boolean isDuplicate = false;
						for (int i = 0; i < existingArray.length(); i++)
						{
							try
							{
								if (existingArray.getString(i).equals(msisdn))
								{
									isDuplicate = true;
									break;
								}
							}
							catch (JSONException e)
							{
								e.printStackTrace();
								continue;
							}
						}

						if (isDuplicate)
						{
							return -1;
						}
						else
						{
							existingArray.put(msisdn);
						}
					}

					cv.put(DBConstants.ACTORS, existingArray.toString());
					cv.put(DBConstants.ACTION_COUNT, existingArray.length());
				}
				else
				{
					if (existingArray.length() == 0)
					{
						// Do nothing
						return -1;
					}
					else
					{
						JSONArray newArray = new JSONArray();
						
						for (String msisdn : actors)
						{
							for (int i = existingArray.length() - 1; i >= 0; i--)
							{
								try
								{
									if (!existingArray.getString(i).equals(msisdn))
									{
										newArray.put(existingArray.getString(i));
									}
								}
								catch (JSONException e)
								{
									e.printStackTrace();
									continue;
								}
							}
						}

						cv.put(DBConstants.ACTORS, newArray.toString());
						cv.put(DBConstants.ACTION_COUNT, newArray.length());
					}
				}
				
				Logger.d("changeActionCountForObjID", "final array: "+existingArray.toString());

				return mDb.update(DBConstants.ACTIONS_TABLE, cv, whereQuery, whereArgs);
			}

		}
		finally
		{
			c.close();
		}

		return -1;
	}

	private boolean deleteActivityLike(FeedDataModel feedData)
	{
		boolean isComplete = false;

		String whereClause = DBConstants.FEED_ACTOR + "=? AND " + DBConstants.FEED_OBJECT_ID + "=? AND " + DBConstants.FEED_ACTION_ID + "=? AND " + DBConstants.FEED_OBJECT_TYPE
				+ "=?";

		int rowDeleted = mDb.delete(DBConstants.FEED_TABLE, whereClause, new String[]{feedData.getActor(), feedData.getObjID(),
			String.valueOf(ActionsDataModel.ActionTypes.LIKE.getKey()), feedData.getObjType().getTypeString()});

		if (rowDeleted != 0)
		{
			isComplete = true;
			Logger.d(HikeConstants.TIMELINE_LOGS, "removing Like from DB " + feedData);
		}
		else
		{
			isComplete = false;
		}
		
		if(isComplete)
		{
			// Update in actions table as well
			ArrayList<String> actorList = new ArrayList<String>();
			actorList.add(feedData.getActor());
			changeActionCountForObjID(feedData.getObjID(), feedData.getObjType().getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, false);
		
			if(!isAnyFeedEntryPresent())
			{
				fireUpdateNotificationIconPubsub(TimelineActivity.NO_FEED_PRESENT);
			}
			else
			{
				fireUpdateNotificationIconPubsub(TimelineActivity.FETCH_FEED_FROM_DB);
			}
		}

		return isComplete;
	}

	/**
	 * Updates Icon for ActivityFeedNotification with 
	 * @param count number of unread feeds
	 * 
	 * count = FETCH_FEED_FROM_DB :-  go for DB fetch
	 *       = NO_FEED_PRESENT :-  feed table empty, returning -1
	 * else:- show count value as it is
	 */
	private void fireUpdateNotificationIconPubsub(int count)
	{
		if(count == TimelineActivity.FETCH_FEED_FROM_DB)
		{
			count = getUnreadActivityFeedCount(true);
			Logger.d(HikeConstants.TIMELINE_LOGS, "unread activity feeds from DB " + count);
		}
		Logger.d(HikeConstants.TIMELINE_LOGS, "firing ACTIVITY_FEED_COUNT_CHANGED " + count);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_FEED_COUNT_CHANGED, new Integer(count));
	}
	
	/**
	 * @return count of unreadActivity Feed
	 */
	public int getUnreadActivityFeedCount(boolean checkForStealth)
	{
		String where = DBConstants.READ + " = 0 ";
		int count = 0;

		Cursor cursor = mDb.query(DBConstants.FEED_TABLE, null, where, null, null, null, null);

		if (cursor != null && cursor.moveToFirst())
		{
			int columnIndex = cursor.getColumnIndex(DBConstants.FEED_ACTOR);
			do
			{
				String msisdn = cursor.getString(columnIndex);
				if (checkForStealth && StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
				{
					continue;
				}
				else
				{
					++count;
				}
			}
			while (cursor.moveToNext());
		}

		return count;
	}
	
	public boolean isAnyFeedEntryPresent() {
		String count = "SELECT * FROM " + DBConstants.FEED_TABLE;
		Cursor cursor = mDb.rawQuery(count, null);
		if (cursor != null && cursor.moveToFirst())
		{
			int columnIndex = cursor.getColumnIndex(DBConstants.FEED_ACTOR);
			do {
				String msisdn = cursor.getString(columnIndex);
				if(StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
				{
					continue;
				}
				else
				{
					return true;
				}
			} while (cursor.moveToNext());
		}
		return false;
	}

	/**
	 * Delete All entries from Feed Table for this SU
	 * 
	 * @return
	 */
	public boolean deleteActivityFeedForStatus(String mappedId)
	{
		boolean isComplete = false;

		String whereClause = DBConstants.FEED_OBJECT_ID + "=?";

		int rowDeleted = mDb.delete(DBConstants.FEED_TABLE, whereClause, new String[]{mappedId});

		if (rowDeleted != 0)
		{
			isComplete = true;
			Logger.d(HikeConstants.TIMELINE_LOGS, "removing "+ rowDeleted + " ActivityFeed from DB for id " + mappedId);
		}
		else
		{
			isComplete = false;
		}

		if (isComplete)
		{
			if(!isAnyFeedEntryPresent())
			{
				fireUpdateNotificationIconPubsub(TimelineActivity.NO_FEED_PRESENT);
			}
			else
			{
				fireUpdateNotificationIconPubsub(TimelineActivity.FETCH_FEED_FROM_DB);
			}
		}

		return isComplete;
	}

	/**
	 * Updates all rows of Feed_Table to 1 (i.e marks all Feeds as read)
	 * 
	 * @return
	 */
	public boolean updateActivityFeedReadStatus()
	{
		boolean isComplete = false;
		ContentValues conVal = new ContentValues();
		conVal.put(DBConstants.READ, 1);
		
		String where = DBConstants.READ + " = 0 ";
		long rowID = mDb.update(DBConstants.FEED_TABLE, conVal, where, null);

		Logger.d(HikeConstants.TIMELINE_LOGS, "inside updateActivityFeedReadStatus,feeds marked as read " + rowID);
		
		if (rowID == -1L)
		{
			isComplete = false;
		}
		else
		{
			isComplete = true;
		}

		if(isComplete)
		{
			if(isAnyFeedEntryPresent())
			{
				fireUpdateNotificationIconPubsub(0);
			}
			else
			{
				fireUpdateNotificationIconPubsub(TimelineActivity.NO_FEED_PRESENT);
			}
		}
		
		return isComplete;
	}
	
	public Cursor getActivityFeedsCursor(String[] msisdnList)
	{
		StringBuilder msisdnSelection = null;
		if (msisdnList != null)
		{
			msisdnSelection = new StringBuilder("(");
			for (String msisdn : msisdnList)
			{
				msisdnSelection.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
			msisdnSelection.replace(msisdnSelection.lastIndexOf(","), msisdnSelection.length(), ")");
		}

		StringBuilder selectQuery = new StringBuilder("SELECT  * FROM " + FEED_TABLE + " ft, "
		            + STATUS_TABLE + " st " + " WHERE ft." + FEED_OBJECT_ID
		            + " = " + "st." + STATUS_MAPPED_ID + " AND ft."
		            + FEED_OBJECT_TYPE + " = '" +  ActivityObjectTypes.STATUS_UPDATE.getTypeString()+"'");

		if (!TextUtils.isEmpty(msisdnSelection))
		{
			selectQuery.append(" AND "+"ft."+FEED_ACTOR + " IN " + msisdnSelection.toString());
		}

		selectQuery.append(" ORDER BY ft." + FEED_TS + " DESC");

		return mDb.rawQuery(selectQuery.toString(), null);
	}

	public static String[] getTimelineFriendsMsisdn(String userMsisdn)
	{
		List<ContactInfo> friendsList = ContactManager.getInstance().getContactsOfFavoriteType(ContactInfo.FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, userMsisdn);

		Logger.d(HikeConstants.TIMELINE_LOGS, "list of friends from CM before filter" + friendsList);

		ArrayList<String> msisdnList = new ArrayList<String>();

		for (ContactInfo contactInfo : friendsList)
		{
			if (TextUtils.isEmpty(contactInfo.getMsisdn()))
			{
				continue;
			}

			if(StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()) && !StealthModeManager.getInstance().isActive())
			{
				continue;
			}

			msisdnList.add(contactInfo.getMsisdn());
		}

		msisdnList.add(userMsisdn);

		String[] friendMsisdns = new String[msisdnList.size()];
		msisdnList.toArray(friendMsisdns);
		Logger.d(HikeConstants.TIMELINE_LOGS, "list of friends after filter whose SU we are fetching " + friendMsisdns);
		return friendMsisdns;
	}
	
	/**
	 * Extracts the thumbnail string from the metadata to save it in a different table. Returns this extracted string so that it can be set back in the metadata once the insertion
	 * has been done.
	 *
	 * @param metadata
	 * @return
	 */
	private String extractThumbnailFromMetadata(MessageMetadata metadata)
	{
		if (metadata == null || metadata.getHikeFiles() == null)
		{
			return null;
		}
		try
		{
			HikeFile hikeFile = metadata.getHikeFiles().get(0);
			if (TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				return null;
			}

			JSONObject metadataJson = metadata.getJSON();
			JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
			JSONObject fileJson;

			fileJson = fileArray.getJSONObject(0);
			fileJson.remove(HikeConstants.THUMBNAIL);

			String thumbnailString = hikeFile.getThumbnailString();

			if (TextUtils.isEmpty(thumbnailString))
			{
				return null;
			}
			addFileThumbnail(hikeFile.getFileKey(), Base64.decode(thumbnailString, Base64.DEFAULT));

			return thumbnailString;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
			return null;
		}
	}

	private String extractThumbnailFromMetadata(JSONObject metadata)
	{

		JSONArray fileArray = metadata.optJSONArray(HikeConstants.FILES);
		JSONObject fileJson;
		if (metadata == null || fileArray == null)
		{
			return null;
		}
		try
		{
			HikeFile hikeFile = new HikeFile(fileArray.getJSONObject(0), true);
			if (TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				return null;
			}



			fileJson = fileArray.getJSONObject(0);
			fileJson.remove(HikeConstants.THUMBNAIL);

			String thumbnailString = hikeFile.getThumbnailString();

			if (TextUtils.isEmpty(thumbnailString))
			{
				return null;
			}
			addFileThumbnail(hikeFile.getFileKey(), Base64.decode(thumbnailString, Base64.DEFAULT));

			return thumbnailString;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
			return null;
		}
	}
	private String getActionsTableCreateQuery()
	{

		String sql = CREATE_TABLE + DBConstants.ACTIONS_TABLE + " (" + BaseColumns._ID + " INTEGER, " // auto increment _id
				+ DBConstants.ACTION_OBJECT_TYPE + " TEXT NOT NULL, " // object type(su, card)
				+ DBConstants.ACTION_OBJECT_ID + " TEXT, " // object id (suid, card id)
				+ DBConstants.ACTION_ID + " INTEGER, " // action id (love, comment,view)
				+ DBConstants.ACTION_COUNT + " INTEGER DEFAULT 0, " // action count
				+ DBConstants.ACTORS + " TEXT DEFAULT '[]', " // actor msisdns
				+ DBConstants.ACTION_METADATA + " TEXT DEFAULT '{}', " // md
				+ DBConstants.ACTION_LAST_UPDATE + " INTEGER DEFAULT 0, " // last updated
				+ "PRIMARY KEY ("+DBConstants.ACTION_OBJECT_ID+", "+DBConstants.ACTION_ID+")" // composite primary key - (obj id + action id)
				+ ")";

		return sql;
	}

	private String getFeedTableCreateQuery()
	{

		String sql = CREATE_TABLE + DBConstants.FEED_TABLE + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY, " // auto increment _id
				+ DBConstants.FEED_OBJECT_TYPE + " TEXT NOT NULL, " // object type(su, card)
				+ DBConstants.FEED_OBJECT_ID + " TEXT, " // object id (suid, card id)
				+ DBConstants.FEED_ACTION_ID + " INTEGER, " // action id (love, comment,view,fav)
				+ DBConstants.FEED_ACTOR + " TEXT, " // actor
				+ DBConstants.READ + " INTEGER DEFAULT 0, " // read - 1/unread -0
				+ DBConstants.FEED_METADATA + " TEXT DEFAULT '{}', " // md
				+ DBConstants.FEED_TS + " INTEGER DEFAULT 0" // timestamp
				+ ")";
		return sql;
	}

	private String getStickerTableCreateQuery()
	{
		String sql = CREATE_TABLE + DBConstants.STICKER_TABLE
				+ "("
				+ DBConstants.STICKER_ID + " TEXT, "
				+ DBConstants.CATEGORY_ID + " TEXT, "
				+ DBConstants.WIDTH + " INTEGER, "
				+ DBConstants.HEIGHT + " INTEGER, "
				+ DBConstants.LARGE_STICKER_PATH + " TEXT, "
				+ DBConstants.SMALL_STICKER_PATH + " TEXT, "
				+ DBConstants.IS_ACTIVE + " INTEGER DEFAULT " + DBConstants.DEFAULT_ACTIVE_STATE + ", "
                + DBConstants.TYPE + " INTEGER DEFAULT " + StickerConstants.StickerType.LARGE.ordinal() + ", "
				+ DBConstants.QUICK_SUGGESTED_REPLY_STICKERS + " TEXT, "
				+ DBConstants.QUICK_SUGGESTED_SENT_STICKERS + " TEXT, "
				+ DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME + " INTEGER DEFAULT 0, "
				+ "PRIMARY KEY ("+DBConstants.CATEGORY_ID +" , "+ DBConstants.STICKER_ID +" )"
				+ " )";

		return sql;
	}

	private String getRecentStickersTableCreateQuery()
	{
		String sql = CREATE_TABLE + DBConstants.RECENT_STICKERS_TABLE
				+ " ( "
				+ DBConstants.STICKER_ID + " TEXT, "
				+ DBConstants.CATEGORY_ID + " TEXT, "
				+ "PRIMARY KEY ( " + DBConstants.CATEGORY_ID + " , " + DBConstants.STICKER_ID + " ) "
				+ " ) ";

		return sql;
	}

	private void addThumbnailStringToMetadata(MessageMetadata metadata, String thumbnailString)
	{
		if (TextUtils.isEmpty(thumbnailString))
		{
			return;
		}

		try
		{
			JSONObject metadataJson = metadata.getJSON();
			JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
			JSONObject fileJson;

			fileJson = fileArray.getJSONObject(0);
			fileJson.put(HikeConstants.THUMBNAIL, thumbnailString);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
		}
	}
	private void addThumbnailStringToMetadata(JSONObject metadataJson, String thumbnailString)
	{
		if (TextUtils.isEmpty(thumbnailString))
		{
			return;
		}
		try
		{
			JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
			JSONObject fileJson;

			fileJson = fileArray.getJSONObject(0);
			fileJson.put(HikeConstants.THUMBNAIL, thumbnailString);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid json");
		}
	}
	public void updateMessageMetadata(long serverID, MessageMetadata metadata)
	{
		String thumbnailString = extractThumbnailFromMetadata(metadata);

		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MESSAGE_METADATA, metadata.serialize());
		try
		{
			mDb.beginTransaction();
			mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[] { String.valueOf(serverID) });
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.SERVER_ID + "=? AND " + DBConstants.IS_STATUS_MSG + " = 0", new String[] { String.valueOf(serverID) });
			updateSharedMediaTableMetadata(serverID, metadata);
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception in updateMessageMetadata: ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
		addThumbnailStringToMetadata(metadata, thumbnailString);
	}
	public void updateMessageMetadata(long serverID, JSONObject metadata)
	{
		String thumbnailString = extractThumbnailFromMetadata(metadata);

		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MESSAGE_METADATA, metadata.toString());
		try
		{
			mDb.beginTransaction();
			mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[] { String.valueOf(serverID) });
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.SERVER_ID + "=? AND " + DBConstants.IS_STATUS_MSG + " = 0", new String[] { String.valueOf(serverID) });
			updateSharedMediaTableMetadata(serverID, metadata);
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception in updateMessageMetadata: ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
		addThumbnailStringToMetadata(metadata, thumbnailString);
	}
	private void updateSharedMediaTableMetadata(long serverId, MessageMetadata metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		putMetadataAccordingToFileType(contentValues, metadata, false);
		if(!contentValues.containsKey(DBConstants.MESSAGE_METADATA))
		{
			return;
		}
		mDb.update(DBConstants.SHARED_MEDIA_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[]{String.valueOf(serverId)});
	}
	private void updateSharedMediaTableMetadata(long serverId, JSONObject metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		putMetadataAccordingToFileType(contentValues, metadata, false);
		if(!contentValues.containsKey(DBConstants.MESSAGE_METADATA))
		{
			return;
		}
		mDb.update(DBConstants.SHARED_MEDIA_TABLE, contentValues, DBConstants.SERVER_ID + "=?", new String[]{String.valueOf(serverId)});
	}
	public void updateConversationMetadata(String msisdn, com.bsb.hike.models.Conversation.ConversationMetadata metadata)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.CONVERSATION_METADATA, metadata.toString());
		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[]{msisdn});
		HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_META_DATA_UPDATED, new Pair<String, ConversationMetadata>(msisdn, metadata));
	}

	private void bindConversationInsert(SQLiteStatement insertStatement, ConvMessage conv,boolean bindForConvId)
	{
		if(conv.getMessage() == null)
		{
			return;
		}
		
		final int sendTimestampColumn = 1;
		final int msgOriginTypeColumn = 2;
		final int messageColumn = 3;
		final int msgStatusColumn = 4;
		final int timestampColumn = 5;
		final int mappedMsgIdColumn = 6;
		final int messageMetadataColumn = 7;
		final int privateDataColumn = 8;
		final int groupParticipant = 9;
		final int isHikeMessageColumn = 10;
		final int messageHash = 11;
		final int typeColumn = 12;
		final int msgMsisdnColumn = 13;
		final int contentIdColumn = 14;
		final int nameSpaceColumn = 15;
		final int msisdnColumn = 16;
		
		insertStatement.clearBindings();
		insertStatement.bindString(messageColumn, conv.getMessage());
		// 0 -> SENT_UNCONFIRMED ; 1 -> SENT_CONFIRMED ; 2 -> RECEIVED_UNREAD ;
		// 3 -> RECEIVED_READ
		insertStatement.bindLong(msgStatusColumn, conv.getState().ordinal());
		insertStatement.bindLong(timestampColumn, conv.getTimestamp());
		insertStatement.bindLong(mappedMsgIdColumn, conv.getMappedMsgID());
		if(bindForConvId){
		insertStatement.bindString(msisdnColumn, conv.getMsisdn());
		}
        if(conv.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT) {
            insertStatement.bindString(messageMetadataColumn, conv.platformMessageMetadata != null ? conv.platformMessageMetadata.JSONtoString() : "");

        } else if(conv.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || conv.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT) {
			insertStatement.bindString(messageMetadataColumn, conv.webMetadata != null ? conv.webMetadata.JSONtoString() : "");

		}else
        {
            insertStatement.bindString(messageMetadataColumn, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
        }

        if(conv.getPrivateData() != null)
    	{
        	Logger.d(AnalyticsConstants.MSG_REL_TAG, "pd after serializing, "+ conv.getPrivateData().serialize().toString());
        	insertStatement.bindString(privateDataColumn, conv.getPrivateData() != null ? conv.getPrivateData().serialize().toString() : "");
    	}
        
		insertStatement.bindLong(isHikeMessageColumn, conv.isSMS() ? 0 : 1);
		insertStatement.bindString(groupParticipant, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");
		
		if(!conv.isSent())
		{
			//Sent messages hash would only be added after message insertion is complete
			//and message id has been generated. we should not do it from here.
			String msgHash = conv.createMessageHash();
			
			if(msgHash != null)
			{
				insertStatement.bindString(messageHash, msgHash);
			}
		}
		insertStatement.bindLong(typeColumn, conv.getMessageType());
		insertStatement.bindString(msgMsisdnColumn, conv.getMsisdn());
		insertStatement.bindLong(contentIdColumn, conv.getContentId());
		insertStatement.bindString(nameSpaceColumn, conv.getNameSpace() != null ? conv.getNameSpace() : "");
		insertStatement.bindLong(msgOriginTypeColumn, conv.getMessageOriginType().ordinal());
		insertStatement.bindLong(sendTimestampColumn, conv.getSendTimestamp());
	}

	private String createMessageHash(String msisdn, long mappedMsgId, String message, long ts)
	{
		String msgHash = null;
		if (TextUtils.isEmpty(message))
		{
			msgHash = msisdn + mappedMsgId + ts;
		}
		else
		{
			msgHash = msisdn + mappedMsgId + message.charAt(0) + message.charAt(message.length() - 1) + ts;
		}
		Logger.d(getClass().getSimpleName(), "Message hash: " + msgHash);
		return msgHash;
	}

	public boolean addConversations(List<ConvMessage> convMessages, boolean createConvIfNotExist)
	{
		if(convMessages.isEmpty())
		{
			return false;
		}
		ArrayList<ContactInfo> contacts= new ArrayList<ContactInfo>(1);
		String msisdn = convMessages.get(0).getMsisdn();
		contacts.add(new ContactInfo(msisdn, msisdn, null, null, !convMessages.get(0).isSMS()));

		return addConversations(convMessages, contacts,createConvIfNotExist);
	}
	
	private long insertMessage(SQLiteStatement insertStatement, ConvMessage conv) throws Exception
	{
		long msgId = -1;
		long sortingId = -1;
		try
		{
			msgId = insertStatement.executeInsert();
			sortingId = getMaxSortingIdFromMessages() + 1;
			conv.setMsgID(msgId);
			conv.setSortingId(sortingId);
			ContentValues contentValues = new ContentValues();
			contentValues.put(DBConstants.SERVER_ID, conv.getServerId());
			contentValues.put(DBConstants.SORTING_ID, sortingId);
			if (conv.isSent())
			{
				// for recieved messages message hash would directly be added from insertStatement.executeInsert() statement
				// we don't need to re-insert it here
				contentValues.put(DBConstants.MESSAGE_HASH, conv.createMessageHash());
			}
			mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[]{Long.toString(conv.getMsgID())});

		}
		catch (Exception e)
		{
			conv.setMsgID(-1);
			conv.setSortingId(-1);
			Logger.e(getClass().getSimpleName(), "Duplicate value ", e);
			logDuplicateMessageEntry(conv, e);
			throw e;
		}

		return msgId;
	}

	/**
	 *
	 * @param convMessages
	 *            -- list of messages to be added to database
	 * @return <li><b>true</b> if messages successfully added to database</li> <li><b>false</b> if messages are not inserted to database possibly due to duplicate</li>
	 */
	public boolean addConversations(List<ConvMessage> convMessages, List<ContactInfo> contacts,boolean createConvIfNotExist)
	{
        SQLiteStatement insertStatement = getSqLiteStatementToInsertIntoMessagesTable(createConvIfNotExist);

		try
		{
			mDb.beginTransaction();

			long msgId = -1;

			int unreadMessageCount = 0;

            Map<String, Pair<List<String>, Long>> map = new HashMap<String, Pair<List<String>, Long>>();
			int totalMessage = convMessages.size()-1;
			long baseId = -1;
			long baseCurrentTime = System.currentTimeMillis();
			for (ContactInfo contact : contacts)
			{
				for (int i=0;i<=totalMessage;i++)
				{
					ConvMessage conv  = convMessages.get(i);
					conv.setSMS(!contact.isOnhike());
					conv.setMsisdn(contact.getMsisdn());
					
					if(conv.isSent())
					{
						//We only need to set this in case of sent messages
						//for recieving messages we should get it inside mqtt packet only
						conv.setSendTimestamp(baseCurrentTime);
					}
					
					String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());
					
					long sortingTimeStamp = conv.getTimestamp();
					if(conv.getTimestamp() <= 0)
					{
						sortingTimeStamp = System.currentTimeMillis()/1000;
					}
					
					long lastMessageTimeStamp = sortingTimeStamp;

					bindConversationInsert(insertStatement, conv,createConvIfNotExist);

					/*
					 * In case message is duplicate insert statement will throw exception . It will catch that exception and will return false denoting duplicate message case
					 */
					try
					{
						msgId = insertMessage(insertStatement, conv);
					}
					catch (Exception e)
					{
						// duplicate message return false
						return false;
					}
					
					addThumbnailStringToMetadata(conv.getMetadata(), thumbnailString);
					/*
					 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a 1-n message, If it is not and the
					 * conversation does not exist we do not add a conversation.
					 */
					if (msgId <= 0 && !OneToNConversationUtils.isOneToNConversation(conv.getMsisdn()))
					{

						addConversation(conv.getMsisdn(), !conv.isSMS(), null, null, conv, -1l,null);
						bindConversationInsert(insertStatement, conv,createConvIfNotExist);
						try
						{
							msgId = insertMessage(insertStatement, conv);
						}
						catch (Exception e1)
						{
							// duplicate message return false
							return false;
						}
						
						assert (msgId >= 0);
					}
					if(baseId==-1){
						baseId = msgId;
					}

					conv.setMsgID(msgId);
					com.bsb.hike.chatthread.ChatThread.addtoMessageMap(conv);

					/*
					 * msdId > 0 means that the conversation exists.
					 */
					if (msgId > 0 && (conv.isFileTransferMessage() || NativeCardUtils.isNativeCardFTMessage(conv)) )
					{
						addSharedMedia(conv);
					}
					if (Utils.shouldIncrementCounter(conv))
					{
						unreadMessageCount++;
					}
					/*
					 * Updating the conversations table only for last message for each contact
					 */
					if(i==totalMessage){
					// incrementing timestamp to save different timestamp for each conversation so that we can fetch in order
					ContentValues contentValues = getContentValueForConversationMessage(conv,lastMessageTimeStamp, sortingTimeStamp++);

					//TODO proper check for broadcast message
					if(conv.isBroadcastMessage() && !conv.isBroadcastConversation())
					{
						//We donot update sorting timestamp value if this is broadcast message in normal 1-1 chat
						contentValues.remove(DBConstants.SORTING_TIMESTAMP);
					}
					mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() });
					}
				// upgrade groupInfoTable
				updateReadBy(conv);
				if (OneToNConversationUtils.isOneToNConversation(conv.getMsisdn()))
				{
					long timestamp = 0;
					Pair<List<String>, Long> pair = map.get(conv.getMsisdn());
					if (pair != null)
						timestamp = pair.second;
					if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
					{
						timestamp = conv.getTimestamp();
					}
					List<String> lastMsisdns = new ArrayList<String>();
					if (conv.getMetadata() != null)
						lastMsisdns = getGroupLastMsgMsisdn(conv.getMetadata().getJSON());
					if (lastMsisdns.size() == 0 && null != conv.getGroupParticipantMsisdn())
					{
						lastMsisdns.add(conv.getGroupParticipantMsisdn());
					}
					map.put(conv.getMsisdn(), new Pair<List<String>, Long>(lastMsisdns, timestamp));
				}
					/*
					 * Shared data for platform card messages
					 */
					if (conv.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || conv.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT)
					{
						PlatformUtils.sharedDataHandlingForMessages(conv);
					}
				}

				incrementUnreadCounter(contact.getMsisdn(), unreadMessageCount);
			}
			// setting base DB id as whole packet logic for multi forward depends on base ID, i.e : first contact -> first message 
			convMessages.get(0).setMsgID(baseId);
			mDb.setTransactionSuccessful();
			ContactManager.getInstance().removeOlderLastGroupMsisdns(map);
			return true;
		}
		finally
		{
			insertStatement.close();
			mDb.endTransaction();
		}
	}

	private void logDuplicateMessageEntry(ConvMessage conv, Exception e)
	{
		//if server switch is off
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MESSAGING_PROD_AREA_LOGGING, true))
		{
			return;
		}
				
		JSONObject infoJson = new JSONObject();
		try 
		{
			infoJson.put(AnalyticsConstants.ERROR_TRACE, e.toString());
			infoJson.put(AnalyticsConstants.MESSAGE_DATA, conv.toString());
		
			HAManager.getInstance().logDevEvent(HikeConstants.MESSAGING, HikeConstants.DUPLICATE, infoJson);
		} 
		catch (JSONException jsonEx) 
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json:",jsonEx);
		}
		
	}

	/**
	 *
	 * @param convMessages
	 *            -- list of messages came in bulk packet
	 * @return <li><b>list</b> of non duplicate messages successfully added to database</li>
	 *
	 */
	public LinkedList<ConvMessage> addConversationsBulk(List<ConvMessage> convMessages)
	{
		Logger.d("bulkPacket", "adding conversation started");
		LinkedList<ConvMessage> resultList = new LinkedList<ConvMessage>();
        SQLiteStatement insertStatement = getSqLiteStatementToInsertIntoMessagesTable(true);
		try
		{
			long msgId = -1;

			for (ConvMessage conv : convMessages)
			{
				if(conv.getMessageEventData()!=null)
				{
					try
					{
						resultList.add(conv);
						GeneralEventMessagesManager.getInstance().handleGeneralMessage(conv.getMessageEventData());
					}
					catch (JSONException e)
					{
						Logger.e("HikeConversationDataBase", "Error in message event");
					}

				}
				else
				{
					String thumbnailString = extractThumbnailFromMetadata(conv.getMetadata());
					bindConversationInsert(insertStatement, conv, true);

					try {
						msgId = insertMessage(insertStatement, conv);
					} catch (Exception e) {
						// duplicate message . Skip further processing
						continue;
					}
					addThumbnailStringToMetadata(conv.getMetadata(), thumbnailString);
				/*
				 * Represents we dont have any conversation made for this msisdn. Here we are also checking whether the message is a group message, If it is and the conversation
				 * does not exist we do not add a conversation.
				 */
					if (msgId <= 0 && !OneToNConversationUtils.isOneToNConversation(conv.getMsisdn())) {
						addConversation(conv.getMsisdn(), !conv.isSMS(), null, null, conv, -1l, null);

						bindConversationInsert(insertStatement, conv, true);
						try {
							msgId = insertMessage(insertStatement, conv);
						} catch (Exception e) {
							// duplicate message . Skip further processing
							continue;
						}

						assert (msgId >= 0);
					}
					conv.setMsgID(msgId);
					com.bsb.hike.chatthread.ChatThread.addtoMessageMap(conv);

				/*
				 * msdId > 0 means that the conversation exists.
				 */
					if (msgId > 0 && (conv.isFileTransferMessage() || NativeCardUtils.isNativeCardFTMessage(conv)) )
					{
						addSharedMedia(conv);
					}

				/*
				 * Shared data for platform card messages
				 */
					if ((conv.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || conv.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT) && conv.getParticipantInfoState() == ParticipantInfoState.NO_INFO) {
						PlatformUtils.sharedDataHandlingForMessages(conv);
					}
					resultList.add(conv);
				}
			}

			Logger.d("BulkProcess", "adding conversation returning");
			return resultList;
		}
		finally
		{
			insertStatement.close();
		}
	}

	public ArrayList<ContactInfo> addBroadcastRecipientConversations(ConvMessage convMessage)
	{
		ContactManager contactManager = ContactManager.getInstance();
		ArrayList<ConvMessage> convMessages= new ArrayList<ConvMessage>(1);
		ConvMessage broadcastConvMessage = new ConvMessage(convMessage);
		//Here we set origin type of 
		broadcastConvMessage.setMessageOriginType(ConvMessage.OriginType.BROADCAST);
		broadcastConvMessage.setServerId(convMessage.getMsgID());
		convMessages.add(broadcastConvMessage);
		ArrayList<String> contactMsisdns = convMessage.getSentToMsisdnsList();
		ArrayList<ContactInfo> contacts = new ArrayList<ContactInfo>();
		for (String msisdn : contactMsisdns)
		{
			ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, contactManager.getName(msisdn), msisdn, true);
			contacts.add(contactInfo);
		}

		addConversations(convMessages, contacts, false);

		return contacts;
	}

	private SQLiteStatement getSqLiteStatementToInsertIntoMessagesTable(boolean createConvIfNotExist) {
        SQLiteStatement insertStatement = null;
        //TODO we need to insert messageOrginType field in both of these queries.
        if(createConvIfNotExist){
            insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.SEND_TIMESTAMP + "," + DBConstants.MESSAGE_ORIGIN_TYPE + "," + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
                    + DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + ","+ DBConstants.PRIVATE_DATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.CONV_ID
                    + ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE   + "," + DBConstants.MSISDN +  "," + HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + " ) "
                    + " SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, " + DBConstants.CONV_ID + ", ?, ?, ?, ?, ?, ? FROM " + DBConstants.CONVERSATIONS_TABLE + " WHERE " + DBConstants.CONVERSATIONS_TABLE + "."
                    + DBConstants.MSISDN + "=?");
        }else{
            insertStatement = mDb.compileStatement("INSERT INTO " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.SEND_TIMESTAMP + "," + DBConstants.MESSAGE_ORIGIN_TYPE + "," + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + ","
                    + DBConstants.TIMESTAMP + "," + DBConstants.MAPPED_MSG_ID + " ," + DBConstants.MESSAGE_METADATA + ","+ DBConstants.PRIVATE_DATA + "," + DBConstants.GROUP_PARTICIPANT
                    + ", " + DBConstants.IS_HIKE_MESSAGE + "," + DBConstants.MESSAGE_HASH + "," + DBConstants.MESSAGE_TYPE  + "," + DBConstants.MSISDN +  "," + HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + " ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

        }
        return insertStatement;
    }

	/**
	 * This function updates unread count for each msisdn/groupId
	 *
	 * @param messageListMap
	 *            -- map of msisdn/groupid to list of conversation messages
	 */
	public void incrementUnreadCountBulk(Map<String, LinkedList<ConvMessage>> messageListMap)
	{
		for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
		{
			LinkedList<ConvMessage> list = entry.getValue();

			if(list != null && !list.isEmpty())		// check for empty or null lists
			{
				int unreadCount = 0;
				for(ConvMessage conv : list)
				{
					/*
					 * We don't increment unreadcount if message is status message
					 */
					if(Utils.shouldIncrementCounter(conv))
					{
						unreadCount ++;
					}
				}

				// update DB
				if(unreadCount != 0)
				{
					incrementUnreadCounter(entry.getKey(), unreadCount);
				}
			}
		}
	}

	/**
	 *
	 * @param convMessages
	 *            -- list of messages to be added to conversation table
	 * @param lastPinMap
	 *            -- list of pin messages to be added to conversation table
	 */
	public void addLastConversations(List<ConvMessage> convMessages, HashMap<String, PairModified<ConvMessage, Integer>> lastPinMap)
	{
		Map<String, Pair<List<String>, Long>> map = new HashMap<String, Pair<List<String>, Long>>();

		for (ConvMessage conv : convMessages)
		{
			String msisdn = conv.getMsisdn();
			ContentValues contentValues = getContentValueForConversationMessage(conv,conv.getTimestamp());

			//TODO proper check for broadcast message
			if(conv.isBroadcastMessage() && !conv.isBroadcastConversation())
			{
				//We donot update sorting timestamp value if this is broadcast message in normal 1-1 chat
				contentValues.remove(DBConstants.SORTING_TIMESTAMP);
			}
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });

			if (lastPinMap.get(conv.getMsisdn()) != null)
			{
				lastPinMap.get(msisdn).setSecond(lastPinMap.get(msisdn).getSecond() - 1);
			}

			if (OneToNConversationUtils.isOneToNConversation(conv.getMsisdn()))
			{
				long timestamp = 0;
				Pair<List<String>, Long> pair = map.get(conv.getMsisdn());
				if (pair != null)
					timestamp = pair.second;
				if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
				{
					timestamp = conv.getTimestamp();
				}
				List<String> lastMsisdns = new ArrayList<String>();
				if (conv.getMetadata() != null)
					lastMsisdns = getGroupLastMsgMsisdn(conv.getMetadata().getJSON());
				if (lastMsisdns.size() == 0 && null != conv.getGroupParticipantMsisdn())
				{
					lastMsisdns.add(conv.getGroupParticipantMsisdn());
				}
				map.put(conv.getMsisdn(), new Pair<List<String>, Long>(lastMsisdns, timestamp));
			}
		}
		ContactManager.getInstance().removeOlderLastGroupMsisdns(map);

		for (Entry<String, PairModified<ConvMessage, Integer>> entry : lastPinMap.entrySet())
		{
			PairModified<ConvMessage, Integer> pair = entry.getValue();
			String msisdn = entry.getKey();
			ContentValues contentValues = getContentValueForPinConversationMessage(pair.getFirst(), new ContentValues(), pair.getSecond());
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
	}

	public void addLastPinConversations(List<ConvMessage> convMessages)
	{
		for (ConvMessage conv : convMessages)
		{
			ContentValues contentValues = getContentValueForPinConversationMessage(conv, new ContentValues());
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() });
		}
	}
	
	public boolean changeGroupSettings(String grpId, int setting,
			int setMyselfAdmin, ContentValues contentValues) {
		Cursor c = null;
		boolean isAmAdmin = false;
		try {

			c = mDb.query(DBConstants.CONVERSATIONS_TABLE,
					new String[] { DBConstants.CONVERSATION_METADATA },
					DBConstants.MSISDN + "=?", new String[] { grpId }, null,
					null, null);
			int metadataIndex = c
					.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext()) {
				String metadata = c.getString(metadataIndex);
				try {
					OneToNConversationMetadata convMetadata;
					if (metadata != null) {
						convMetadata = new OneToNConversationMetadata(metadata);
					} else {
						convMetadata = new OneToNConversationMetadata(null);
					}

					if(setting!=-1){
					convMetadata.setAddMembersRights(setting);
					}
					if (setMyselfAdmin != -1) {
						convMetadata.setMyselfAsAdmin(setMyselfAdmin);
					}
					if (setMyselfAdmin == 1) {
						
						isAmAdmin = true;
					}else{
						
						try {
							if(convMetadata.amIAdmin())
							{
							  isAmAdmin =convMetadata.amIAdmin();
							}
						} catch (Exception e) {
							isAmAdmin = false;
							
						}
					}
					contentValues.put(DBConstants.CONVERSATION_METADATA,
							convMetadata.toString());
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.CONV_META_DATA_UPDATED,
							new Pair<String, ConversationMetadata>(grpId,
									convMetadata));

					mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues,
							DBConstants.MSISDN + "=?", new String[] { grpId });
					return isAmAdmin;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		} finally {
			if (c != null) {
				c.close();
			}
		}
		return isAmAdmin;

	}

	public void updateIsHikeMessageState(long id, boolean isHikeMessage)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_HIKE_MESSAGE, isHikeMessage);

		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[]{Long.toString(id)});
	}

	private ContentValues getContentValueForConversationMessage(ConvMessage conv,long timeStampForMessage)
	{
		return getContentValueForConversationMessage(conv, timeStampForMessage, timeStampForMessage);
	}
	private ContentValues getContentValueForConversationMessage(ConvMessage conv,long lastMessageTimeStamp, long sortingTimeStamp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE, conv.getMessage());
		contentValues.put(DBConstants.MESSAGE_METADATA, conv.getMetadata() != null ? conv.getMetadata().serialize() : "");
		contentValues.put(DBConstants.GROUP_PARTICIPANT, conv.getGroupParticipantMsisdn() != null ? conv.getGroupParticipantMsisdn() : "");

		boolean statusMessage = conv.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
		contentValues.put(DBConstants.IS_STATUS_MSG, statusMessage);

		if (!statusMessage)
		{
			contentValues.put(DBConstants.MESSAGE_ID, conv.getMsgID());
			contentValues.put(DBConstants.SERVER_ID, conv.getServerId());
			contentValues.put(DBConstants.MAPPED_MSG_ID, conv.getMappedMsgID());
			contentValues.put(DBConstants.MSG_STATUS, conv.getState().ordinal());
			contentValues.put(DBConstants.SORTING_TIMESTAMP, sortingTimeStamp);
			contentValues.put(DBConstants.LAST_MESSAGE_TIMESTAMP, lastMessageTimeStamp);
			contentValues.put(DBConstants.MESSAGE_ORIGIN_TYPE, conv.getMessageOriginType().ordinal());
		}
		if (conv.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			contentValues = getContentValueForPinConversationMessage(conv, contentValues);
		}
		
		if(conv.getPrivateData() != null)
		{
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "pd after serializing, "+ conv.getPrivateData().serialize().toString());
		}
		contentValues.put(DBConstants.PRIVATE_DATA, conv.getPrivateData() != null ? conv.getPrivateData().serialize().toString() : "");
		return contentValues;
	}

	/*
	 * add pin related content values to the content values that comes in argument and return modified content values
	 */
	private ContentValues getContentValueForPinConversationMessage(ConvMessage conv, ContentValues contentValues)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() }, null,
					null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);
				try
				{
					OneToNConversationMetadata convMetadata;
					if (metadata != null)
					{
						convMetadata = new OneToNConversationMetadata(metadata);
					}
					else
					{
						convMetadata = new OneToNConversationMetadata(null);
						convMetadata.setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, conv.getMsgID());
					}
					long preTimeStamp = convMetadata.getLastPinTimeStamp(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					long currentTimeStamp = conv.getTimestamp();
					if (preTimeStamp < currentTimeStamp)
					{
						convMetadata = updatePinMetadata(conv, convMetadata, 0);

					}
					contentValues.put(DBConstants.CONVERSATION_METADATA, convMetadata.toString());
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_META_DATA_UPDATED, new Pair<String, ConversationMetadata>(conv.getMsisdn(), convMetadata));
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return contentValues;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/*
	 * add pin related content values to the content values that comes in argument and return modified content values This function is specific for bulk. We also give unread count
	 * in this function.
	 */
	private ContentValues getContentValueForPinConversationMessage(ConvMessage conv, ContentValues contentValues, int unreadCount)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { conv.getMsisdn() }, null,
					null, null);
			int metadataIndex = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			if (c.moveToNext())
			{
				String metadata = c.getString(metadataIndex);

				try
				{
					OneToNConversationMetadata convMetaData = null;
					convMetaData = new OneToNConversationMetadata(metadata);

					convMetaData = updatePinMetadata(conv, convMetaData, unreadCount);
					contentValues.put(DBConstants.CONVERSATION_METADATA, convMetaData.toString());
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_META_DATA_UPDATED, new Pair<String, ConversationMetadata>(conv.getMsisdn(), convMetaData));
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return contentValues;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public OneToNConversationMetadata updatePinMetadata(ConvMessage msg, OneToNConversationMetadata metadata, int unreadCount)
	{
		try
		{
			if (metadata != null)
			{

				// update only for received
				if (!msg.isSent())
				{
					if (unreadCount != 0)
					{
						metadata.setUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, (metadata.getUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN) + unreadCount));
					}
					else
					{
						metadata.incrementUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					}

					metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, false);
				}
				else
				{
					metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
				}

				metadata.setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
				metadata.setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, msg.getMsgID());
				metadata.setLastPinTimeStamp(HikeConstants.MESSAGE_TYPE.TEXT_PIN, msg.getTimestamp());
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metadata;
	}

	public void deleteConversation(String msisdn)
	{
		try
		{
			mDb.beginTransaction();
			mDb.delete(DBConstants.CONVERSATIONS_TABLE, DBConstants.MSISDN + "=?", new String[]{msisdn});
			mDb.delete(DBConstants.MESSAGES_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
			mDb.delete(DBConstants.SHARED_MEDIA_TABLE, DBConstants.MSISDN + "=?", new String[]{msisdn});
			mDb.delete(DBConstants.RECEIPTS_TABLE, DBConstants.MSISDN + "=?", new String[]{msisdn});
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
				mDb.delete(DBConstants.GROUP_INFO_TABLE, DBConstants.GROUP_ID + " =?", new String[] { msisdn });
				removeChatThemeForMsisdn(msisdn);
			}

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void deleteBot(String msisdn){
		mDb.beginTransaction();
		try
		{
			mDb.delete(DBConstants.BOT_TABLE, DBConstants.MSISDN + "=?", new String[] { msisdn });
			removeChatThemeForMsisdn(msisdn);
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner, String grpCreator)
	{
		return addConversation(msisdn, onhike, groupName, groupOwner, null, -1l, grpCreator);
	}

	/**
	 * Add a conversation to the db
	 *
	 * @param msisdn
	 *            the msisdn of the contact
	 * @param onhike
	 *            true iff the contact is onhike. If this is false, we consult the local db as well
	 * @param groupName
	 *            the name of the group. Sent as <code>null</code> if the conversation is not a group conversation
	 * @param initialConvMessage
	 *            the initial ConvMessage object to be added to the conversation before publishing the new conversation event
	 * @return Conversation object representing the conversation
	 */
	public Conversation addConversation(String msisdn, boolean onhike, String groupName, String groupOwner, ConvMessage initialConvMessage, Long groupCreationTime, String grpCreator)
	{
		ContactInfo contactInfo = OneToNConversationUtils.isOneToNConversation(msisdn) ? new ContactInfo(msisdn, msisdn, groupName, msisdn) : ContactManager.getInstance().getContact(msisdn,
				false, true);
		InsertHelper ih = null;
		try
		{
			ih = new InsertHelper(mDb, DBConstants.CONVERSATIONS_TABLE);
			ih.prepareForInsert();
			ih.bind(ih.getColumnIndex(DBConstants.MSISDN), msisdn);
			if (contactInfo != null)
			{
				ih.bind(ih.getColumnIndex(DBConstants.CONTACT_ID), contactInfo.getId());
				onhike |= contactInfo.isOnhike();
			}

			ih.bind(ih.getColumnIndex(DBConstants.ONHIKE), onhike);

			long id = 0l;
			try
			{
				id = ih.execute();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}

			if (id >= 0)
			{
				Conversation conv = null;
				if (OneToNConversationUtils.isOneToNConversation(msisdn))
				{
					if (OneToNConversationUtils.isGroupConversation(msisdn))
					{
						conv = new GroupConversation.ConversationBuilder(msisdn).setConvName((contactInfo != null) ? contactInfo.getName() : null).setConversationOwner(groupOwner)
								.setIsAlive(true).setCreationTime(groupCreationTime).setConversationCreator(grpCreator).build();
					}
					else if (OneToNConversationUtils.isBroadcastConversation(msisdn))
					{
						conv = new BroadcastConversation.ConversationBuilder(msisdn).setConvName((contactInfo != null) ? contactInfo.getName() : null).setConversationOwner(groupOwner)
								.setIsAlive(true).setCreationTime(groupCreationTime).build();
					}
					InsertHelper groupInfoIH = null;
					try
					{
						groupInfoIH = new InsertHelper(mDb, DBConstants.GROUP_INFO_TABLE);
						groupInfoIH.prepareForInsert();
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ID), msisdn);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_NAME), groupName);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_OWNER), groupOwner);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_ALIVE), 1);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_CREATION_TIME), groupCreationTime);
						groupInfoIH.bind(groupInfoIH.getColumnIndex(DBConstants.GROUP_CREATOR), grpCreator);
						groupInfoIH.execute();
					}
					finally
					{
						if (groupInfoIH != null)
						{
							groupInfoIH.close();
						}
					}

					((OneToNConversation) conv).setConversationParticipantList(ContactManager.getInstance().getGroupParticipants(msisdn, false, false));
				}
				
				else if (BotUtils.isBot(msisdn))
				{
					BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
					conv = new BotConversation.ConversationBuilder(msisdn).setConvInfo(botInfo).build();
				}
				else
				{
					conv = new OneToOneConversation.ConversationBuilder(msisdn).setConvName((contactInfo != null) ? contactInfo.getName() : null).setIsOnHike(onhike).build();
				}
				
				if (initialConvMessage != null)
				{
					conv.updateLastConvMessage(initialConvMessage);
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv.getConvInfo());
				return conv;

			}
			/* TODO does this happen? If so, what should we do? */
			Logger.wtf("Conversationadding", "Couldn't add conversation --- race condition?");
			return null;

		}
		finally
		{
			if (ih != null)
			{
				ih.close();
			}
		}
	}

	public void insertBot(BotInfo botInfo)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, botInfo.getMsisdn());
		values.put(DBConstants.NAME, botInfo.getConversationName());
		values.put(DBConstants.CONVERSATION_METADATA, botInfo.getMetadata());
		values.put(DBConstants.IS_MUTE, botInfo.isMute() ? 1 : 0);
		values.put(DBConstants.BOT_TYPE, botInfo.getType());
		values.put(DBConstants.BOT_CONFIGURATION, botInfo.getConfiguration());
		values.put(DBConstants.CONFIG_DATA, botInfo.getConfigData());
		values.put(HIKE_CONTENT.NAMESPACE, botInfo.getNamespace());
		values.put(HIKE_CONTENT.HELPER_DATA, botInfo.getHelperData());
		values.put(HIKE_CONTENT.BOT_VERSION, botInfo.getVersion());
		values.put(HIKE_CONTENT.BOT_TRIGGER_POINT, botInfo.getTriggerPointFormenu());
		values.put(HIKE_CONTENT.CLIENT_ID, botInfo.getClientId());
		values.put(HIKE_CONTENT.CLIENT_HASH, botInfo.getClientHash());
		values.put(DBConstants.HIKE_UID,botInfo.getUid());
		mDb.insertWithOnConflict(DBConstants.BOT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void updateUIDForBot(Set<FetchUIDTaskPojo> fecTaskPojo) {
		if (Utils.isEmpty(fecTaskPojo)) {
			return;
		}
		mDb.beginTransaction();
		try {

			ContentValues cv = new ContentValues();
			for (FetchUIDTaskPojo pojo : fecTaskPojo) {
				cv.put(HIKE_UID, pojo.getUid());
				mDb.update(DBConstants.BOT_TABLE, cv, DBConstants.MSISDN + " =?", new String[]{pojo.getMsisdn()});
				cv.clear();
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	public boolean isBotMuted(String msisdn)
	{
		Cursor c = null;
		int muteInt = 0;
		try
		{
			String selection = DBConstants.MSISDN + " = ?";

			c = mDb.query(DBConstants.BOT_TABLE, new String[] { DBConstants.IS_MUTE }, selection, new String[] { msisdn }, null, null, null, null);

			if (c.moveToFirst())
			{
				muteInt = c.getInt(c.getColumnIndex(DBConstants.IS_MUTE));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return muteInt == 0 ? false : true;
	}
	

	public List<ConvMessage> getConversationThread(String msisdn, int limit, Conversation conversation, long maxSortId, long minSortId)
	{
		Long time = System.currentTimeMillis();
		String limitStr = (limit == -1) ? null : new Integer(limit).toString();
		String selection = DBConstants.MSISDN + " = ?";
		if (maxSortId != -1)
		{
			selection = selection + " AND " + DBConstants.SORTING_ID + "<" + maxSortId;
		}
		if (minSortId != -1)
		{
			selection = selection + " AND " + DBConstants.SORTING_ID + ">" + minSortId;
		}
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE,DBConstants.MESSAGE_ORIGIN_TYPE, DBConstants.SORTING_ID}, selection, new String[] { msisdn }, null, null, DBConstants.SORTING_ID + " DESC", limitStr);


			List<ConvMessage> elements = getMessagesFromDB(c, conversation);
			Collections.sort(elements, new ConvMessageComparator());
			
			Logger.d("ChatThread", "Time taken to query messages from db : " + (System.currentTimeMillis() - time));
			return elements;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	
	public List<ConvMessage> getUnDeliveredMessages(String msisdn)
	{
		String selection = DBConstants.MSISDN + " = ?" + " AND " + DBConstants.MSG_STATUS + " < " + State.SENT_DELIVERED.ordinal();
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE,DBConstants.MESSAGE_ORIGIN_TYPE, DBConstants.SORTING_ID}, selection, new String[] { msisdn }, null, null, DBConstants.SORTING_ID + " DESC");


			List<ConvMessage> elements = getMessagesFromDB(c,true,msisdn);
			Collections.sort(elements, new ConvMessageComparator());

			return elements;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	public Conversation getConversation(String msisdn, int limit, boolean getMetadata)
	{

		Cursor c = null;
		Conversation conv = null;
		try
		{
			if (getMetadata)
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH,
						DBConstants.CONVERSATION_METADATA }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			else
			{
				c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONTACT_ID, DBConstants.ONHIKE, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH },
						DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			}
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			int unreadCount = c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
			boolean isStealth = c.getInt(c.getColumnIndex(DBConstants.IS_STEALTH)) != 0;
			String metadata = null;
			/**
			 * Group Conversation
			 */
			if (OneToNConversationUtils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn);
				/**
				 * A rare case where there is a residual entry in the Conv_table, but not in the group_info table. Though it should not happen
				 */
				if (conv == null)
				{
					return null;
				}
				conv.setIsStealth(isStealth);
			}
			/**
			 * Broadcast Conversation
			 */
			else if (OneToNConversationUtils.isBroadcastConversation(msisdn))
			{
				conv = getBroadcastConversation(msisdn);
				/**
				 * A rare case where there is a residual entry in the Conv_table, but not in the group_info table. Though it should not happen
				 */
				if (conv == null)
				{
					return null;
				}
				conv.setIsStealth(isStealth);
			}
			
			/**
			 * Normal 1-1 conversation
			 */
			else
			{
				if (BotUtils.isBot(msisdn))
				{
					BotInfo botInfo= BotUtils.getBotInfoForBotMsisdn(msisdn);
					conv = new BotConversation.ConversationBuilder(msisdn).setConvInfo(botInfo).build();
				}
				else
				{
					ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, false, true, false);
					String name = contactInfo.getName();
					onhike |= contactInfo.isOnhike();
					conv = new OneToOneConversation.ConversationBuilder(msisdn).setConvName(name).setIsOnHike(onhike).setIsStealth(isStealth).build();
				}


			}
			if (getMetadata)
			{
				metadata = c.getString(c.getColumnIndex(DBConstants.CONVERSATION_METADATA));
				try
				{
					if (conv instanceof OneToOneConversation)
					{
						//conv.setMetadata(new ConversationMetadata(metadata));
						conv.setMetadata(new OneToOneConversationMetadata(metadata));
					}
					else if (conv instanceof OneToNConversation)
					{
						conv.setMetadata(new OneToNConversationMetadata(metadata));
					}
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			List<ConvMessage> messages;
			if (limit != 0)
			{
				if (limit != -1 && unreadCount > limit)
				{
					messages = getConversationThread(msisdn, unreadCount, conv, -1, -1);
				}
				else
				{
					messages = getConversationThread(msisdn, limit, conv, -1, -1);
				}
				conv.setMessages(messages);
			}
			
			conv.setUnreadCount(unreadCount);
			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	/**
	 *
	 * @param msgId of the convmessage we are fetching
	 * @return
     */

	public ConvMessage getConvMessageForMsgId(long msgId)
	{
		// msgid cant be negative so returning null here
		
		if(msgId < 0)
		{
			return null;
		}
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, null, DBConstants.MESSAGE_ID + " =? ", new String[] { Long.toString(msgId) }, null, null, null , null);

			if (c.moveToFirst())
			{
				final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
				final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
				final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
				final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
				final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
				final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
				final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
				final int sortingIdColumn = c.getColumnIndex(DBConstants.SORTING_ID);

				ConvMessage message = new ConvMessage(c.getString(msgColumn), c.getString(msisdnColumn), c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						msgId, c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), c.getLong(sortingIdColumn));
				String metadata = c.getString(metadataColumn);
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}

				return message;
			}
		}
		catch (Exception e)
		{
			Logger.e(HikeConversationsDatabase.class.getName(), "exception in fetching convmessage for msgId : " + msgId , e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return null;
	}

	public Conversation getConversation(String msisdn, int limit)
	{
		return getConversation(msisdn, limit, false);
	}

	public ConvMessage getLastMessage(String msisdn)
	{
		/*
		 * We get the latest message from the messages table
		 */
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, null, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, DBConstants.SORTING_ID + " DESC " , "1");
			
			if (c.moveToFirst())
			{
				final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
				final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
				final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
				final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
				final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
				final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
				final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
				final int sortingIdColumn = c.getColumnIndex(DBConstants.SORTING_ID);

				ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), c.getLong(sortingIdColumn));
				String metadata = c.getString(metadataColumn);
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}
				
				return message;
			}
		} 
		catch (Exception e) 
		{
			Logger.e(HikeConversationsDatabase.class.getName(), e.toString());
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return null;
	}
 
	/**
	 * Using this method to get the conversation with the last message. If there is no last message, we return null if the conversation is not a GC.
	 *
	 * @param msisdn
	 * @return
	 */
	public Conversation getConversationWithLastMessage(String msisdn)
	{
		Cursor c = null;
		Conversation conv = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.ONHIKE, DBConstants.SORTING_TIMESTAMP },
					DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (!c.moveToFirst())
			{
				Logger.d(getClass().getSimpleName(), "Could not find db entry");
				return null;
			}

			boolean onhike = c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0;
			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int lastMessageTsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

			String messageString = c.getString(msgColumn);
			String metadata = c.getString(metadataColumn);

			/*
			 * If the message does not contain any text or metadata, its an empty message and the conversation is blank.
			 */
			if (!OneToNConversationUtils.isOneToNConversation(msisdn) && TextUtils.isEmpty(messageString) && TextUtils.isEmpty(metadata))
			{
				return null;
			}

			if (OneToNConversationUtils.isGroupConversation(msisdn))
			{
				conv = getGroupConversation(msisdn);
			}
			
			else if (OneToNConversationUtils.isBroadcastConversation(msisdn))
			{
				conv = getBroadcastConversation(msisdn);
			}
			
			else if (BotUtils.isBot(msisdn))
			{
				BotInfo botInfo= BotUtils.getBotInfoForBotMsisdn(msisdn);
				conv = new BotConversation.ConversationBuilder(msisdn).setConvInfo(botInfo).build();
			}
			else
			{
				ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, false, true);

				onhike |= contactInfo.isOnhike();
				conv = new OneToOneConversation.ConversationBuilder(msisdn).setConvName(contactInfo.getName()).setIsOnHike(onhike).build();
			}

			ConvMessage message = new ConvMessage(messageString, msisdn, c.getInt(lastMessageTsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
					c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
			try
			{
				message.setMetadata(metadata);
			}
			catch (JSONException e)
			{
				Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			conv.updateLastConvMessage(message);

			return conv;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	public HashSet<MessageInfo> getMessageInfo(long messageId){
		Cursor c=null;
		HashSet<MessageInfo> set=new HashSet<MessageInfo>();
		try{
			String selection = DBConstants.MESSAGE_ID + "=?";
			String[] whereArgs = new String[] { Long.toString(messageId)};
			c = mDb.query(DBConstants.RECEIPTS_TABLE, new String[] { DBConstants.RECEIVER_MSISDN, DBConstants.READ_TIMESTAMP, DBConstants.DELIVERY_TIMESTAMP }, selection, whereArgs, null,
				null, null);

			final int msisdnColumn = c.getColumnIndex(DBConstants.RECEIVER_MSISDN);
			final int readTimestampColumn = c.getColumnIndex(DBConstants.READ_TIMESTAMP);
			final int deliveryTimestampColumn = c.getColumnIndex(DBConstants.DELIVERY_TIMESTAMP);
			while(c.moveToNext()){
				String msisdn = c.getString(msisdnColumn);
				long read_timestamp = c.getLong(readTimestampColumn);
				long delivery_timestamp = c.getLong(deliveryTimestampColumn);
				set.add(new MessageInfo(msisdn,read_timestamp,delivery_timestamp));
			}
		}catch (Exception e){}
		finally {
			if(c!=null)
				c.close();
		}
		return set;
	}
	public ConversationMsisdns getConversationMsisdns()
	{
		Cursor c = null;
		List<String> oneToOneMsisdns = new ArrayList<String>();
		Map<String, Pair<List<String>, Long>> grpLastMsisdns = new HashMap<String, Pair<List<String>, Long>>();

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.GROUP_PARTICIPANT, DBConstants.MESSAGE_METADATA ,DBConstants.SORTING_TIMESTAMP}, null, null, null,
					null, null);

			final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int timestampColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnColumn);
				String groupParticipant = c.getString(groupParticipantColumn);
				String metadata = c.getString(metadataColumn);
				long timestamp = c.getLong(timestampColumn);

				if (OneToNConversationUtils.isOneToNConversation(msisdn))
				{
					List<String> grpMsisdns = null;
					try
					{
						if (!TextUtils.isEmpty(metadata))
						{
							grpMsisdns = getGroupLastMsgMsisdn(new JSONObject(metadata));
							if (null != grpMsisdns)
							{
								if (grpMsisdns.isEmpty())
								{
									if (null != groupParticipant && !groupParticipant.equals(""))
									{
										grpMsisdns.add(groupParticipant);
									}
								}
							}
						}
						grpLastMsisdns.put(msisdn, new Pair<List<String>, Long>(grpMsisdns,timestamp));
					}
					catch (JSONException e)
					{
						Logger.e("HikeConversationsDatabase", "Exception while getting last group message msisdns : " + e);
					}
				}
				else
				{
					oneToOneMsisdns.add(msisdn);
				}
			}
			ConversationMsisdns convsMsisdns = new ConversationMsisdns(oneToOneMsisdns, grpLastMsisdns);
			return convsMsisdns;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	/*
	 * This method returns a list of ContactInfo for all 1-1 conversations with timestamp set.
	 */
	public List<ContactInfo> getOneToOneContacts(boolean removeNewOrReturningUsers)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.SORTING_TIMESTAMP, DBConstants.MESSAGE_METADATA}, null, null, null,
					null, null);

			final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int sortingTimestampColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);

			List<ContactInfo> oneToOneContacts = new ArrayList<ContactInfo>();
			while(c.moveToNext())
			{
				String msisdn = c.getString(msisdnColumn);
				String metadata = c.getString(metadataColumn);
				long timestamp = c.getLong(sortingTimestampColumn);

				if(!OneToNConversationUtils.isOneToNConversation(msisdn))
				{
					if (!TextUtils.isEmpty(metadata) && removeNewOrReturningUsers)
					{
						try
						{
							JSONObject md = new JSONObject(metadata);
							if(md.optString(HikeConstants.TYPE).equals(HikeConstants.MqttMessageTypes.USER_JOINED))
							{
								continue;
							}
						}
						catch (JSONException e)
						{
							Logger.e("HikeConversationsDatabase", "Exception while parsing metadata for fetching new users msisdn : " + e);
						}
					}
					/*
					 * It is assumed that the contact would be in persistence cache since we have a conversation going on for this msisdn.
					 * If not, it makes a db query and loads into persistence cache.
					 */
					ContactInfo contact = ContactManager.getInstance().getContact(msisdn, false, true);
					contact.setLastMessaged(timestamp);
					oneToOneContacts.add(contact);
				}
			}
			return oneToOneContacts;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<String> getGroupLastMsgMsisdn(JSONObject metadata)
	{
		List<String> grpLastMsisdns = new ArrayList<String>();
		try
		{
			ParticipantInfoState participantInfoState = metadata.has(HikeConstants.DND_USERS) || metadata.has(HikeConstants.DND_NUMBERS) ? ParticipantInfoState.DND_USER
					: ParticipantInfoState.fromJSON(metadata);
			switch (participantInfoState)
			{
			case CHANGED_GROUP_NAME:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
				break;

			case DND_USER:
				JSONArray dndNumbers = metadata.has(HikeConstants.DND_USERS) ? metadata.getJSONArray(HikeConstants.DND_USERS) : metadata.getJSONArray(HikeConstants.DND_NUMBERS);
				if (dndNumbers != null && dndNumbers.length() > 0)
				{
					for (int i = 0; i < dndNumbers.length(); i++)
					{
						String dndName = dndNumbers.optString(i);
						grpLastMsisdns.add(dndName);
					}
				}
				break;

			case PARTICIPANT_JOINED:
				JSONArray participantInfoArray = metadata.getJSONArray(HikeConstants.DATA);

				//This handles the check that of no group participant than, group is successfully created
				if(participantInfoArray == null || participantInfoArray.length() == 0)
				{
					break;
				}
				
				JSONObject participant = (JSONObject) participantInfoArray.opt(0);
				grpLastMsisdns.add(participant.optString(HikeConstants.MSISDN));

				if (participantInfoArray.length() == 2)
				{
					JSONObject participant2 = (JSONObject) participantInfoArray.opt(1);
					grpLastMsisdns.add(participant2.optString(HikeConstants.MSISDN));
				}
				break;

			case PARTICIPANT_LEFT:
				grpLastMsisdns.add(metadata.getString(HikeConstants.DATA));
				break;

			case USER_JOIN:
			case USER_OPT_IN:
				grpLastMsisdns.add(metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN));
				break;

			case CHANGED_GROUP_IMAGE:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
				break;
			case STATUS_MESSAGE:
			case CHAT_BACKGROUND:
				grpLastMsisdns.add(metadata.getString(HikeConstants.FROM));
			}
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception while getting last message in group msisdn from metadata " + e);
		}
		return grpLastMsisdns;
	}

	public Map<String, GroupDetails> getIdGroupDetailsMap(List<String> grpIds)
	{
		StringBuilder groupIds = new StringBuilder("(");
		for (String grpId : grpIds)
		{
			groupIds.append(DatabaseUtils.sqlEscapeString(grpId) + ",");
		}
		int idx = groupIds.lastIndexOf(",");
		if (idx >= 0)
		{
			groupIds.replace(idx, groupIds.length(), ")");
		}

		Cursor groupInfoCursor = null;
		try
		{
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,

					new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP , DBConstants.GROUP_CREATION_TIME,DBConstants.GROUP_CREATOR}, DBConstants.GROUP_ID + " IN " + groupIds, null,
					null, null, null);
			Map<String, GroupDetails> map = new HashMap<String, GroupDetails>();
			final int groupIdIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupAliveIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ALIVE);
			final int groupMuteIdx = groupInfoCursor.getColumnIndex(DBConstants.MUTE_GROUP);
			final int groupCreationIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_CREATION_TIME);
			final int groupCreatorIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_CREATOR);
			while (groupInfoCursor.moveToNext())
			{
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				boolean groupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;
				boolean groupMute = groupInfoCursor.getInt(groupMuteIdx) != 0;
				long groupCreationTime = groupInfoCursor.getLong(groupCreationIdx);
				String groupCreator = groupInfoCursor.getString(groupCreatorIdx);
				map.put(groupId, new GroupDetails(groupId, groupName, groupAlive, groupMute, groupCreationTime, groupCreator));
			}
			return map;
		}
		finally
		{
			if (null != groupInfoCursor)
			{
				groupInfoCursor.close();
			}

		}
	}

	/**
	 * Returns a hash map between group id and group name
	 *
	 * @return
	 */
	public Map<String, GroupDetails> getIdGroupDetailsMap()
	{
		Cursor groupInfoCursor = null;
		try
		{
			groupInfoCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP, DBConstants.GROUP_CREATION_TIME, DBConstants.GROUP_CREATOR  }, null, null, null,
					null, null);

			Map<String, GroupDetails> map = new HashMap<String, GroupDetails>();

			final int groupIdIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ID);
			final int groupNameIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_NAME);
			final int groupAliveIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_ALIVE);
			final int groupMuteIdx = groupInfoCursor.getColumnIndex(DBConstants.MUTE_GROUP);
			final int groupCreationIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_CREATION_TIME);
			final int groupCreatorIdx = groupInfoCursor.getColumnIndex(DBConstants.GROUP_CREATOR);
			while (groupInfoCursor.moveToNext())
			{
				String groupId = groupInfoCursor.getString(groupIdIdx);
				String groupName = groupInfoCursor.getString(groupNameIdx);
				boolean groupAlive = groupInfoCursor.getInt(groupAliveIdx) != 0;
				boolean groupMute = groupInfoCursor.getInt(groupMuteIdx) != 0;
				long groupCreationTime = groupInfoCursor.getLong(groupCreationIdx);
				String groupCreator = groupInfoCursor.getString(groupCreatorIdx);

				map.put(groupId, new GroupDetails(groupId, groupName, groupAlive, groupMute, groupCreationTime, groupCreator));
			}
			return map;
		}
		finally
		{
			if (null != groupInfoCursor)
			{
				groupInfoCursor.close();
			}

		}
	}
	public int setGroupCreationTime(String groupId, long time)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[]{DBConstants.GROUP_CREATION_TIME}, DBConstants.GROUP_ID + "=?", new String[]{groupId}, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			long existingTime = c.getLong(c.getColumnIndex(DBConstants.GROUP_CREATION_TIME));

			if (time ==existingTime)
			{
				return 0;
			}

			ContentValues values = new ContentValues(1);
			values.put(DBConstants.GROUP_CREATION_TIME, time);
			return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	
	/**
	 * Returns a list of convInfo objects to be displayed on the home screen
	 * 
	 * @return
	 * 			{@link ConvInfo}
	 */			
	public List<ConvInfo> getConvInfoObjects()
	{
		long startTime = System.currentTimeMillis();
		Cursor c = null;

		c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.ONHIKE, DBConstants.LAST_MESSAGE_TIMESTAMP,
				DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.UNREAD_COUNT, DBConstants.IS_STEALTH,
				DBConstants.SORTING_TIMESTAMP }, null, null, null, null, null);

		Map<String, ConvInfo> conversations = new HashMap<String, ConvInfo>();

		final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int lastMessageTsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
		final int sortingTsColumn = c.getColumnIndex(DBConstants.SORTING_TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		final int unreadCountColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);
		final int isStealthColumn = c.getColumnIndex(DBConstants.IS_STEALTH);
		final int isOnHikeColumn = c.getColumnIndex(DBConstants.ONHIKE);

		try
		{
			List<String> groupIds = new ArrayList<String>();
			List<String> msisdns = new ArrayList<String>();

			while (c.moveToNext())
			{

				ConvInfo convInfo;
				String msisdn = c.getString(msisdnIdx);
				String messageString = c.getString(msgColumn);
				String metadata = c.getString(metadataColumn);
				long lastMessageTimestamp = c.getLong(lastMessageTsColumn);
				long sortingTimestamp = c.getLong(sortingTsColumn);
				boolean onhike = c.getInt(isOnHikeColumn) != 0;
				Mute mute = ContactManager.getInstance().getMute(msisdn);

				boolean muteApproach = HikeSharedPreferenceUtil.getInstance().getData(
						(ChatThreadUtils.getChatThreadType(msisdn) == HikeConstants.Extras.GROUP_CHAT_THREAD ? HikeConstants.MUTE_GC_SERVER_SWITCH : HikeConstants.MUTE_ONE_TO_ONE_SERVER_SWITCH), true);

				/*
				 *	This handles the backup restore case when a conversation is muted.
				 */
				if (muteApproach && mute != null && mute.isMute() && mute.getMuteDuration() != HikeConstants.MuteDuration.DURATION_FOREVER && mute.getMuteEndTime() < System.currentTimeMillis())
				{
					mute.setIsMute(false);
					mute.setShowNotifInMute(true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, mute);
				}

				//If broadcast or group converstaion, create a oneToN object
				if (OneToNConversationUtils.isOneToNConversation(msisdn))
				{
					GroupDetails details = ContactManager.getInstance().getGroupDetails(msisdn);
					if (null == details)
					{
						groupIds.add(msisdn);
						convInfo = new OneToNConvInfo.ConvInfoBuilder(msisdn).setConversationAlive(true).setIsMute(false).setOnHike(onhike).build();
					}
					else
					{
						String name = details.getGroupName();
						boolean groupAlive = details.isGroupAlive();
						convInfo = new OneToNConvInfo.ConvInfoBuilder(msisdn).setConversationAlive(groupAlive).setIsMute((mute != null) ? mute.isMute() : false).setOnHike(onhike).setConvName(name).build();
					}
				}
				else
				{
					ContactInfo contact=null;
					if (BotUtils.isBot(msisdn))
					{
						convInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
						convInfo.setIsMute((mute != null) ? mute.isMute() : false);
						contact = ContactManager.getInstance().getContact(convInfo.getMsisdn());
					}

					else
					{
						convInfo = new ConvInfo.ConvInfoBuilder(msisdn).setSortingTimeStamp(sortingTimestamp).setOnHike(onhike).setIsMute((mute != null) ? mute.isMute() : false).build();
						contact = ContactManager.getInstance().getContact(convInfo.getMsisdn());
					}
					
					ContactManager.getInstance().updateContactRecency(msisdn, sortingTimestamp, false);

					if (null == contact)
					{
						msisdns.add(msisdn);
					}
					else
					{
						/**
						 * The contact manager can have null names for bot and if it sets null here, we're screwed big time.
						 */
						if (!BotUtils.isBot(msisdn))
						{
							convInfo.setmConversationName(contact.getName());
						}
					}
				}

				convInfo.setUnreadCount(c.getInt(unreadCountColumn));
				convInfo.setStealth(c.getInt(isStealthColumn) == 1);

				ConvMessage message = new ConvMessage(messageString, msisdn, lastMessageTimestamp, ConvMessage.stateValue(c.getInt(msgStatusColumn)), c.getLong(msgIdColumn),
						c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}

				convInfo.setLastConversationMsg(message);
				convInfo.setSortingTimeStamp(sortingTimestamp);
				Logger.d("HikeConversationDatabase", "conversation msisdn : " + msisdn);
				conversations.put(msisdn, convInfo);

			}

			if (msisdns.size() > 0)
			{
				List<ContactInfo> contacts = ContactManager.getInstance().getContact(msisdns, false, true);
				for (ContactInfo contact : contacts)
				{
					ConvInfo convinfo = conversations.get(contact.getMsisdn());
					convinfo.setmConversationName(contact.getName());
				}
			}

			if (groupIds.size() > 0)
			{
				Logger.d("HikeConversationDatabase", " group ids list that returned null for group details : " + groupIds);
				Map<String, GroupDetails> groupDetailsMap = ContactManager.getInstance().getGroupDetails(groupIds);
				for (Entry<String, GroupDetails> mapEntry : groupDetailsMap.entrySet())
				{
					ConvInfo convInfo = conversations.get(mapEntry.getKey());
					GroupDetails details = mapEntry.getValue();
					if (null != details)
					{
						String name = details.getGroupName();
						boolean groupAlive = details.isGroupAlive();
						boolean isMuteGroup = details.isGroupMute();
						convInfo.setmConversationName(name);
						((OneToNConvInfo) convInfo).setConversationAlive(groupAlive);
						convInfo.setIsMute(isMuteGroup);
					}
				}
			}
		}

		finally
		{
			c.close();
		}

		List<ConvInfo> conversationsList = new ArrayList<ConvInfo>(conversations.values());

		Collections.sort(conversationsList, Collections.reverseOrder());

		Logger.d("ConversationsTimeTest", "Query time: " + (System.currentTimeMillis() - startTime));

		return conversationsList;
	}

	private ConvMessage getLastMessageForConversation(String msisdn)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[]{DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.LAST_MESSAGE_TIMESTAMP, DBConstants.MAPPED_MSG_ID,
					DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT}, DBConstants.MSISDN + "=?", new String[]{msisdn}, null, null, null);

			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.LAST_MESSAGE_TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);

			if (c.moveToFirst())
			{
				ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn));
				String metadata = c.getString(metadataColumn);
				try
				{
					message.setMetadata(metadata);
				}
				catch (JSONException e)
				{
					Logger.e(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}
				return message;
			}
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ConvMessage getLastPinForConversation(OneToNConversation conversation)
	{
		Cursor c = null;

			long msgId;
			try
			{
				msgId = conversation.getMetadata().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
					DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
					DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE, DBConstants.MESSAGE_ORIGIN_TYPE, DBConstants.SORTING_ID}, DBConstants.MESSAGE_ID + " =?", new String[] { Long.toString(msgId) }, null, null, null, null);
			List<ConvMessage> elements = getMessagesFromDB(c, conversation);
			return elements.size() > 0 ? elements.get(elements.size() - 1) : null;
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
			return null;

	}

	private GroupConversation getGroupConversation(String msisdn)
	{
		Cursor groupCursor = null;
		try
		{
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
					new String[] { DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP, DBConstants.GROUP_CREATION_TIME , DBConstants.GROUP_CREATOR }, DBConstants.GROUP_ID + " = ? ",
					new String[] { msisdn }, null, null, null);
			if (!groupCursor.moveToFirst())
			{
				Logger.w(getClass().getSimpleName(), "Could not find db entry: " + msisdn);
				return null;
			}

			String groupName = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_NAME));
			String groupOwner = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_OWNER));
			boolean isGroupAlive = groupCursor.getInt(groupCursor.getColumnIndex(DBConstants.GROUP_ALIVE)) != 0;
			boolean isMuted = groupCursor.getInt(groupCursor.getColumnIndex(DBConstants.MUTE_GROUP)) != 0;
			long groupCreationTime = groupCursor.getLong(groupCursor.getColumnIndex(DBConstants.GROUP_CREATION_TIME));
			String groupCreator = groupCursor.getString(groupCursor.getColumnIndex(DBConstants.GROUP_CREATOR));

			GroupConversation conv;
			conv = new GroupConversation.ConversationBuilder(msisdn).setConvName(groupName).setConversationOwner(groupOwner).setIsAlive(isGroupAlive).setCreationTime(groupCreationTime).setConversationCreator(groupCreator).build();
			conv.setConversationParticipantList(ContactManager.getInstance().getActiveConversationParticipants(msisdn));
//			conv.setGroupMemberAliveCount(getActiveParticipantCount(msisdn));
			conv.setIsMute(isMuted);
		
			return conv;
		}
		finally
		{
			if (groupCursor != null)
			{
				groupCursor.close();
			}
		}
	}
	
	private BroadcastConversation getBroadcastConversation(String msisdn)
	{
		Cursor broadcastCursor = null;
		try
		{
			broadcastCursor = mDb.query(DBConstants.GROUP_INFO_TABLE,
				new String[]{DBConstants.GROUP_NAME, DBConstants.GROUP_OWNER, DBConstants.GROUP_ALIVE, DBConstants.MUTE_GROUP, DBConstants.GROUP_CREATION_TIME}, DBConstants.GROUP_ID + " = ? ",
				new String[]{msisdn}, null, null, null);
			if (!broadcastCursor.moveToFirst())
			{
				Logger.w(getClass().getSimpleName(), "Could not find db entry: " + msisdn);
				return null;
			}

			String groupName = broadcastCursor.getString(broadcastCursor.getColumnIndex(DBConstants.GROUP_NAME));
			String groupOwner = broadcastCursor.getString(broadcastCursor.getColumnIndex(DBConstants.GROUP_OWNER));
			boolean isGroupAlive = broadcastCursor.getInt(broadcastCursor.getColumnIndex(DBConstants.GROUP_ALIVE)) != 0;
			boolean isMuted = broadcastCursor.getInt(broadcastCursor.getColumnIndex(DBConstants.MUTE_GROUP)) != 0;
			long groupCreationTime = broadcastCursor.getLong(broadcastCursor.getColumnIndex(DBConstants.GROUP_CREATION_TIME));

			BroadcastConversation conv;
			conv = new BroadcastConversation.ConversationBuilder(msisdn).setConvName(groupName).setConversationOwner(groupOwner).setIsAlive(isGroupAlive).setCreationTime(groupCreationTime).build();
			conv.setConversationParticipantList(ContactManager.getInstance().getActiveConversationParticipants(msisdn));
//			conv.setGroupMemberAliveCount(getActiveParticipantCount(msisdn));
			conv.setIsMute(isMuted);
			conv.setCreationDate(groupCreationTime);

			return conv;
		}
		finally
		{
			if (broadcastCursor != null)
			{
				broadcastCursor.close();
			}
		}
	}

	public List<Pair<Long, JSONObject>> updateStatusAndSendDeliveryReport(String msisdn)
	{

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.PRIVATE_DATA }, DBConstants.MSISDN + "=? and " + DBConstants.MSG_STATUS
					+ "=?", new String[] { msisdn, Integer.toString(ConvMessage.State.RECEIVED_UNREAD.ordinal()) }, null, null, null);
			/* If there are no rows in the cursor then simply return null */
			if (c.getCount() <= 0)
			{
				return null;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("(");

			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int mappedMsgIdIdx = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msgMetadataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
			final int privatedataIdx = c.getColumnIndex(DBConstants.PRIVATE_DATA);
			
			List<Pair<Long, JSONObject>> ids = new ArrayList<Pair<Long, JSONObject>>(c.getCount());
			while (c.moveToNext())
			{
				long msgId = c.getLong(msgIdIdx);
				long mappedMsgId = c.getLong(mappedMsgIdIdx);
				
				if (mappedMsgId > 0)
				{
					String msgMetadata = c.getString(msgMetadataIdx);
					String privatedata = c.getString(privatedataIdx);
					
					JSONObject dataObject = new JSONObject();
					dataObject.put(HikeConstants.METADATA, msgMetadata);
					dataObject.put(HikeConstants.PRIVATE_DATA, privatedata);
					Pair<Long, JSONObject> pair = new Pair<Long, JSONObject>(mappedMsgId, dataObject);
					ids.add(pair);
				}
				sb.append(msgId);
				if (!c.isLast())
				{
					sb.append(",");
				}
			}
			sb.append(")");
			ContentValues values = new ContentValues();
			values.put(DBConstants.MSG_STATUS, ConvMessage.State.RECEIVED_READ.ordinal());
			int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID + " in " + sb.toString(), null);

			// Resetting the unread count as well
			values.put(DBConstants.UNREAD_COUNT, 0);
			mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MESSAGE_ID + " in " + sb.toString(), null);

			Logger.d("HIKE CONVERSATION DB ", "Rows Updated : " + rowsAffected);
			if (ids.size() == 0)
			{
				return null;
			}
			return ids;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return null;
	}

	public List<Pair<Long, JSONObject>> updateStatusAndSendDeliveryReport(List<ConvMessage> convMessages)
	{
		if (convMessages == null || convMessages.isEmpty())
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("(");
		String msisdn=convMessages.get(0).getMsisdn();

		List<Pair<Long, JSONObject>> ids = new ArrayList<Pair<Long, JSONObject>>();
		for (int j = 0; j < convMessages.size(); j++)
		{
			ConvMessage msg = convMessages.get(j);

			long msgId = msg.getMsgID();
			long mappedMsgId = msg.getMappedMsgID();
			if (mappedMsgId > 0)
			{
				try
				{
					JSONObject dataObject = new JSONObject();
					dataObject.put(HikeConstants.METADATA, msg.getMetadata());
					dataObject.put(HikeConstants.PRIVATE_DATA, msg.getPrivateData());
					Pair<Long, JSONObject> pair = new Pair<Long, JSONObject>(mappedMsgId, dataObject);
					ids.add(pair);
				}
				catch (JSONException ex)
				{
					Logger.e("unread", "exception for msg id : " + msg.getMsgID() + " exception : ", ex);
				}
			}
			sb.append(msgId);
			if (j < convMessages.size() - 1)
			{
				sb.append(",");
			}
		}

		sb.append(")");

		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS, ConvMessage.State.RECEIVED_READ.ordinal());
		int rowsAffected = mDb.update(DBConstants.MESSAGES_TABLE, values, DBConstants.MESSAGE_ID + " in " + sb.toString(), null);
		String[] args = { msisdn };
		// Resetting the unread count as well
		values.put(DBConstants.UNREAD_COUNT, 0);
		int rowsAffect = mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MSISDN + "=?", args);
	
		Logger.d("HIKE CONVERSATION DB ", "Rows Updated : " + rowsAffected + " RowsUpdated " + rowsAffect);
		if (ids.size() == 0)
		{
			return null;
		}
		return ids;
	}
	
	/**
	 * deletes multiple messages corresponding to give msgId
	 *
	 * @param msgIds
	 * @param msisdn
	 * @param containsLastMessage
	 *            null if its value is not known. In this case we need to fetch this value from db. value true implies that given msgIds set contains a messageId of a message which
	 *            is currently last message of that conversation. false implies it does not contains last message's id.
	 */
	public void deleteMessages(ArrayList<Long> msgIds, String msisdn, Boolean containsLastMessage)
	{

		if (msgIds == null || msgIds.isEmpty())
		{
			Logger.e(HikeConversationsDatabase.class.getSimpleName(), "deleteMessages :: msgIds not present");
			return;
		}
		
		StringBuilder inSelection = new StringBuilder("(" + msgIds.get(0));
		for (int i = 0; i < msgIds.size(); i++)
		{
			inSelection.append("," + Long.toString(msgIds.get(i)));
		}
		inSelection.append(")");
		try
		{
			mDb.beginTransaction();
			if (containsLastMessage == null)
			{
				ConvMessage convMessage = getLastMessageForConversation(msisdn);
				if (msgIds.contains(convMessage.getMsgID()))
				{
					containsLastMessage = true;
				}
				else
				{
					containsLastMessage = false;
				}
			}

			mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MESSAGE_ID + " IN " + inSelection.toString());

			mDb.execSQL("DELETE FROM " + DBConstants.SHARED_MEDIA_TABLE + " WHERE " + DBConstants.MESSAGE_ID + " IN " + inSelection.toString());

			if (containsLastMessage)
			{
				deleteMessageFromConversation(msisdn);
			}
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void clearConversation(String msisdn)
	{
		String[] args = new String[] { msisdn };
		/*
		 * Clearing the messages table.
		 */
		mDb.execSQL("DELETE FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MSISDN + "= ?", args);

		mDb.execSQL("DELETE FROM " + DBConstants.SHARED_MEDIA_TABLE + " WHERE " + DBConstants.MSISDN + "= ?", args);

		mDb.execSQL("DELETE FROM " + DBConstants.RECEIPTS_TABLE + " WHERE " + DBConstants.MSISDN + "= ?", args);
		/*
		 * Next we have to clear the conversation table.
		 */
		clearLastConversationMessage(msisdn);
	}

	private void clearLastConversationMessage(String msisdn)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE, "");
		contentValues.put(DBConstants.MESSAGE_METADATA, "");
		contentValues.put(DBConstants.GROUP_PARTICIPANT, "");
		contentValues.put(DBConstants.IS_STATUS_MSG, false);
		contentValues.put(DBConstants.MESSAGE_ID, 0);
		contentValues.put(DBConstants.MAPPED_MSG_ID, 0);
		contentValues.put(DBConstants.UNREAD_COUNT, 0);
		contentValues.put(DBConstants.MSG_STATUS, State.RECEIVED_READ.ordinal());

		mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[]{msisdn});
	}

	private void deleteMessageFromConversation(String msisdn)
	{
		ConvMessage message =  getLastMessage(msisdn);
		boolean conversationEmpty = false;

		if (message != null)
		{
			ContentValues contentValues = getContentValueForConversationMessage(message,message.getTimestamp());
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				updateGroupRecency(message);
				ContactManager.getInstance().removeContact(message.getGroupParticipantMsisdn(), false);
			}
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
		else
		{
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				/*
				 * If we have removed the last message of a group, we should do the same operations we do when clearing a conversation.
				 */
				clearLastConversationMessage(msisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, msisdn);
				return;
			}
			else
			{
				/*
				 * This conversation is empty.
				 */
				clearLastConversationMessage(msisdn);
				conversationEmpty = true;
			}
		}
		ConvMessage newLastMessage = conversationEmpty ? null : getLastMessageForConversation(msisdn);
		HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_MESSAGE_DELETED, new Pair<ConvMessage, String>(newLastMessage, msisdn));
	}

	private void updateGroupRecency(ConvMessage conv)
	{
		if (OneToNConversationUtils.isOneToNConversation(conv.getMsisdn()))
		{
			if (conv.getParticipantInfoState() != ParticipantInfoState.STATUS_MESSAGE)
			{
				ContactManager.getInstance().updateGroupRecency(conv.getMsisdn(), conv.getTimestamp());
			}
		}
	}

	public boolean wasOverlayDismissed(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.OVERLAY_DISMISSED }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			int s = 0;
			if (c.moveToFirst())
			{
				s = c.getInt(0);
			}
			return (s == 0) ? false : true;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void setOverlay(boolean dismiss, String msisdn)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.OVERLAY_DISMISSED, dismiss);
		if (msisdn != null)
		{
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
		else
		{
			mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, null, null);
		}
	}

	/**
	 * Add or remove participants from a group members table based on participants map that is passed as parameter
	 *
	 * @param groupId
	 *            The id of the group to which the participants are to be added
	 * @param participantList
	 *            A list of the participants to be added
	 */
	public int addRemoveGroupParticipants(String groupId, Map<String, PairModified<GroupParticipant, String>> participantList, boolean groupRevived)
	{
		boolean participantsAlreadyAdded = true;
		boolean infoChangeOnly = false;

		Map<String, PairModified<GroupParticipant, String>> currentParticipants = null;

		Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>> groupParticipantsPair = getGroupParticipants(groupId, true, false);
		if (groupParticipantsPair == null)
		{
			currentParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
		}
		else
		{
			currentParticipants = groupParticipantsPair.first;
		}
		
		if (currentParticipants.isEmpty())
		{
			participantsAlreadyAdded = false;
		}
		for (Entry<String, PairModified<GroupParticipant, String>> newParticipantEntry : participantList.entrySet())
		{
			if (!currentParticipants.containsKey(newParticipantEntry.getKey()))
			{
				participantsAlreadyAdded = false;
				infoChangeOnly = false;
			}
			else
			{
				GroupParticipant currentParticipant = currentParticipants.get(newParticipantEntry.getKey()).getFirst();
				if (currentParticipant.onDnd() != newParticipantEntry.getValue().getFirst().onDnd())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				if (currentParticipant.getContactInfo().isOnhike() != newParticipantEntry.getValue().getFirst().getContactInfo().isOnhike())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				if (currentParticipant.isAdmin() != newParticipantEntry.getValue().getFirst().isAdmin())
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				String currentUID=currentParticipant.getUid();
				String newUidFromServer= newParticipantEntry.getValue().getFirst().getUid();

				if (TextUtils.isEmpty(currentUID) && !TextUtils.isEmpty(newUidFromServer))
				{
					participantsAlreadyAdded = false;
					infoChangeOnly = true;
				}
				currentParticipants.remove(newParticipantEntry.getKey());
			}
		}

		if (groupRevived)
		{
			/*
			 * if group is revived then we have to check whether some members are added or removed from the group and update that in the db
			 */
			if (currentParticipants.isEmpty() && participantsAlreadyAdded)
			{
				return HikeConstants.NO_CHANGE;
			}
		}
		else
		{
			/*
			 * if group is not revived that means we have added some members into the group or gcj was already received when adding members the parameter participantList contains
			 * only members which are added and not all the group participants
			 */
			if (participantsAlreadyAdded)
				return HikeConstants.NO_CHANGE;
		}

		Map<String, PairModified<GroupParticipant, String>> newParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
		SQLiteStatement insertStatement = null;
		InsertHelper ih = null;
		try
		{
			mDb.beginTransaction();

			if (groupRevived && !currentParticipants.isEmpty())
			{
				String removeMsisdns = PhoneUtils.getMsisdnStatement(currentParticipants.keySet());
				Logger.d(getClass().getSimpleName(), " remove these from group members table GroupId : " + groupId + " removed msisdns : " + removeMsisdns);
				mDb.delete(DBConstants.GROUP_MEMBERS_TABLE, DBConstants.GROUP_ID + " = ? " + " AND " + DBConstants.MSISDN + " IN " + removeMsisdns, new String[] { groupId });
				ContactManager.getInstance().removeGroupParticipant(groupId, currentParticipants.keySet());
			}

			if(!participantsAlreadyAdded)
			{
				ih = new InsertHelper(mDb, DBConstants.GROUP_MEMBERS_TABLE);
				insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.GROUP_MEMBERS_TABLE + " ( " + DBConstants.GROUP_ID + ", " + DBConstants.MSISDN
						+ ", " + DBConstants.NAME + ", " + DBConstants.ONHIKE + ", " + DBConstants.HAS_LEFT + ", " + DBConstants.ON_DND + ", " + DBConstants.SHOWN_STATUS + ", " + DBConstants.TYPE  + " , " + DBConstants.HIKE_UID
						+" ) "
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, ? ,?)");
				for (Entry<String, PairModified<GroupParticipant, String>> participant : participantList.entrySet())
				{
					GroupParticipant groupParticipant = participant.getValue().getFirst();
					insertStatement.bindString(ih.getColumnIndex(DBConstants.GROUP_ID), groupId);
					insertStatement.bindString(ih.getColumnIndex(DBConstants.MSISDN), participant.getKey());
					insertStatement.bindString(ih.getColumnIndex(DBConstants.NAME), participant.getValue().getSecond());
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.ONHIKE), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.HAS_LEFT), 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.ON_DND), groupParticipant.onDnd() ? 1 : 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.SHOWN_STATUS), groupParticipant.getContactInfo().isOnhike() ? 1 : 0);
					insertStatement.bindLong(ih.getColumnIndex(DBConstants.TYPE), groupParticipant.isAdmin() ? 1 : 0);
					if (!TextUtils.isEmpty(groupParticipant.getUid()))
						insertStatement.bindString(ih.getColumnIndex(DBConstants.HIKE_UID), groupParticipant.getUid());
					insertStatement.executeInsert();
					insertStatement.clearBindings();
					newParticipants.put(participant.getKey(), new PairModified<GroupParticipant, String>(groupParticipant, participant.getValue().getSecond()));
				}
				ContactManager.getInstance().addGroupParticipants(groupId, newParticipants);
			}
			mDb.setTransactionSuccessful();
			return infoChangeOnly ? HikeConstants.PARTICIPANT_STATUS_CHANGE : HikeConstants.NEW_PARTICIPANT;
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
			if (ih != null)
			{
				ih.close();
			}

			mDb.endTransaction();
		}
	}

	public int updateDndStatus(String msisdn)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.ON_DND, 0);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}
	
	
	public int updateShownStatus(String groupId)
	{
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.SHOWN_STATUS, 1);
		return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	/**
	 * Should be called when a participant leaves the group
	 *
	 * @param groupId
	 *            : The group ID of the group containing the participant
	 * @param msisdn
	 *            : The msisdn of the participant
	 */
	public int setParticipantLeft(String groupId, String msisdn)
	{
		Cursor c = null;
		try
		{
			String selection = DBConstants.GROUP_ID + "=? AND " + DBConstants.MSISDN + "=?";
			String[] selectionArgs = new String[] { groupId, msisdn };

			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.HAS_LEFT }, selection, selectionArgs, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			int hasLeft = c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT));
			/*
			 * If member has already left don't do anything.
			 */
			if (hasLeft == 1)
			{
				return 0;
			}

			ContentValues contentValues = new ContentValues(1);
			contentValues.put(DBConstants.HAS_LEFT, 1);

			return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, selection, selectionArgs);

		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	/**
	 * Should be called when a participant changed to admin
	 *
	 * @param groupId
	 *            : The group ID of the group containing the participant
	 * @param msisdn
	 *            : The msisdn of the participant who changed to admin
	 */
	public int setParticipantAdmin(String groupId, String msisdn)
	{
		Cursor c = null;
		try
		{
			String selection = DBConstants.GROUP_ID + "=? AND " + DBConstants.MSISDN + "=?";
			String[] selectionArgs = new String[] { groupId, msisdn };

			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.TYPE }, selection, selectionArgs, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			ContentValues contentValues = new ContentValues(1);
			contentValues.put(DBConstants.TYPE, 1);

			return mDb.update(DBConstants.GROUP_MEMBERS_TABLE, contentValues, selection, selectionArgs);

		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, Map<String, String>> getGroupMembersName(String msisdns)
	{
		Cursor c = null;
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.NAME, DBConstants.GROUP_ID }, DBConstants.MSISDN + " IN " + msisdns,
					null, null, null, null);
			final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int nameIdx = c.getColumnIndex(DBConstants.NAME);
			final int groupidIdx = c.getColumnIndex(DBConstants.GROUP_ID);
			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);
				String groupId = c.getString(groupidIdx);
				Map<String, String> groupParticipant = map.get(groupId);
				if (null == groupParticipant)
				{
					groupParticipant = new HashMap<String, String>();
					map.put(groupId, groupParticipant);
				}
				groupParticipant.put(msisdn, name);
			}
			return map;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Returns a list of participants to a group
	 *
	 * @param groupId
	 * @return
	 */
	public Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly)
	{
		String selection = DBConstants.GROUP_ID + " =? " + (activeOnly ? " AND " + DBConstants.HAS_LEFT + "=0" : "")
				+ (notShownStatusMsgOnly ? " AND " + DBConstants.SHOWN_STATUS + "=0" : "");
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND, DBConstants.TYPE,DBConstants.HIKE_UID },
					selection, new String[] { groupId }, null, null, null);

			Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();
			List<String> allMsisdns = new ArrayList<String>();
			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				allMsisdns.add(msisdn);
				String name = c.getString(c.getColumnIndex(DBConstants.NAME));
				String uid = c.getString(c.getColumnIndex(DBConstants.HIKE_UID));
				ContactInfo ci = new ContactInfo(msisdn, msisdn, name, msisdn, c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0);
				ci.setUid(uid);
				GroupParticipant groupParticipant = new GroupParticipant(ci,
						c.getInt(c.getColumnIndex(DBConstants.HAS_LEFT)) != 0, c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0, c.getInt(c.getColumnIndex(DBConstants.TYPE)), groupId);
				participantList.put(msisdn, new PairModified<GroupParticipant, String>(groupParticipant, name));
			}

			return new Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>>(participantList, allMsisdns);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int getActiveParticipantCount(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.HAS_LEFT + "=0 AND " + DBConstants.GROUP_ID + "=?",
					new String[] { groupId }, null, null, null);
			return c.getCount();
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, Integer> getAllGroupsActiveParticipantCount()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.GROUP_ID, "count(*) as count" },
					(DBConstants.HAS_LEFT + "=0") , null, DBConstants.GROUP_ID, null, null);
			Map<String, Integer> groupCountMap = new HashMap<String, Integer>();
			while(c.moveToNext())
			{
				String groupId = c.getString(c.getColumnIndex(DBConstants.GROUP_ID));
				int count = c.getInt(c.getColumnIndex("count"));
				groupCountMap.put(groupId, count);
			}
			return groupCountMap;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, Mute> getChatMuteMap()
	{
		Cursor muteCursor = null;
		try
		{
			muteCursor = mDb.query(DBConstants.CHAT_PROPERTIES_TABLE, new String[]{DBConstants.MSISDN, DBConstants.IS_MUTE, DBConstants.MUTE_TIMESTAMP, DBConstants.MUTE_DURATION, DBConstants.MUTE_NOTIFICATION}, DBConstants.IS_MUTE + "= 1", null, null, null, null);

			Map<String, Mute> map = new HashMap<>();

			while (muteCursor.moveToNext())
			{
				String msisdn = muteCursor.getString(muteCursor.getColumnIndex(DBConstants.MSISDN));
				Mute mute = new Mute.InitBuilder(msisdn).build();
				boolean isMute = muteCursor.getInt(muteCursor.getColumnIndex(DBConstants.IS_MUTE)) == 1 ? true : false;
				int muteDuration = muteCursor.getInt(muteCursor.getColumnIndex(DBConstants.MUTE_DURATION));
				boolean muteNotification = (muteCursor.getInt(muteCursor.getColumnIndex(DBConstants.MUTE_NOTIFICATION)) == 0 ? false : true);
				long muteTimestamp = muteCursor.getLong(muteCursor.getColumnIndex(DBConstants.MUTE_TIMESTAMP));

				mute.setIsMute(isMute);
				mute.setMuteDuration(muteDuration);
				mute.setShowNotifInMute(muteNotification);
				mute.setMuteTimestamp(muteTimestamp);

				map.put(msisdn, mute);
			}
			return map;
		}
		finally
		{
			if (muteCursor != null)
			{
				muteCursor.close();
			}
		}
	}


	public int getRankCountFromCategoryTable()
	{
		Cursor cursor = null;
		int count;
		try
		{
			cursor = mDb.query(DBConstants.STICKER_CATEGORY_RANK_TABLE, new String[] { DBConstants.RANK }, null, null, null, null, null);
			count = cursor.getCount();
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
		return count;
	}

	public void toggleGroupMute(String groupId, boolean isMuted)
	{
		if (!ContactManager.getInstance().isGroupExist(groupId))
		{
			return ;
		}
		ContactManager.getInstance().setGroupMute(groupId, isMuted);
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.MUTE_GROUP, isMuted);

		mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	public void toggleChatMute(Mute mute)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSISDN, mute.getMsisdn());
		contentValues.put(DBConstants.IS_MUTE, mute.isMute() ? 1 : 0);
		contentValues.put(DBConstants.MUTE_DURATION, mute.getMuteDuration());
		contentValues.put(DBConstants.MUTE_NOTIFICATION, mute.shouldShowNotifInMute() ? 1 : 0);
		contentValues.put(DBConstants.MUTE_TIMESTAMP, mute.getMuteTimestamp());

		int id = (int) mDb.insertWithOnConflict(DBConstants.CHAT_PROPERTIES_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
		if (id < 0)
		{
			mDb.update(DBConstants.CHAT_PROPERTIES_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { mute.getMsisdn() });
		}

		ContactManager.getInstance().setChatMute(mute.getMsisdn(), mute);
	}

	public int setGroupName(String groupId, String groupname)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_NAME }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null, null, null);

			if (!c.moveToFirst())
			{
				return 0;
			}

			String existingName = c.getString(c.getColumnIndex(DBConstants.GROUP_NAME));

			if (groupname.equals(existingName))
			{
				return 0;
			}

			ContentValues values = new ContentValues(1);
			values.put(DBConstants.GROUP_NAME, groupname);
			return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Map<String, String> getGroupParticipantNameMap(String groupId, List<String> msisdns)
	{
		Map<String, String> map = new HashMap<String, String>();

		StringBuilder msisdnsDB = new StringBuilder("(");
		for (String msisdn : msisdns)
		{
			msisdnsDB.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
		}
		int idx = msisdnsDB.lastIndexOf(",");
		if (idx >= 0)
		{
			msisdnsDB.replace(idx, msisdnsDB.length(), ")");
		}

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.NAME }, DBConstants.GROUP_ID + " =? AND " + DBConstants.MSISDN + " IN "
					+ msisdnsDB.toString(), new String[] { groupId }, null, null, null);
			String name = "";

			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				name = c.getString(c.getColumnIndex(DBConstants.NAME));
				map.put(msisdn, name);
			}
			return map;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public GroupParticipant getGroupParticipant(String groupId, String participantMsisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.MSISDN, DBConstants.HAS_LEFT, DBConstants.ONHIKE, DBConstants.NAME, DBConstants.ON_DND , DBConstants.TYPE},
					DBConstants.GROUP_ID + "=? AND " + DBConstants.MSISDN + "=?", new String[] { groupId, participantMsisdn }, null, null, null, null);

			GroupParticipant groupParticipant = null;

			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				String name = c.getString(c.getColumnIndex(DBConstants.NAME));
				groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, name, msisdn, c.getInt(c.getColumnIndex(DBConstants.ONHIKE)) != 0), c.getInt(c
						.getColumnIndex(DBConstants.HAS_LEFT)) != 0, c.getInt(c.getColumnIndex(DBConstants.ON_DND)) != 0, c.getInt(c.getColumnIndex(DBConstants.TYPE)), groupId);
			}

			if (groupParticipant != null)
			{
				ContactInfo contactInfo = ContactManager.getInstance().getContact(participantMsisdn, true, false, true);
				if (contactInfo != null)
				{
					groupParticipant.setContactInfo(contactInfo);
				}
			}
			return groupParticipant;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int toggleGroupDeadOrAlive(String groupId, boolean alive)
	{
		if (!ContactManager.getInstance().isGroupExist(groupId))
		{
			return 0;
		}
		ContactManager.getInstance().setGroupAlive(groupId, alive);
		ContentValues values = new ContentValues(1);
		values.put(DBConstants.GROUP_ALIVE, alive);
		return mDb.update(DBConstants.GROUP_INFO_TABLE, values, DBConstants.GROUP_ID + " = ?", new String[] { groupId });
	}

	public List<String> listOfGroupConversationsWithMsisdn(String msisdn)
	{
		Cursor cursor = null;
		try
		{
			String selection = DBConstants.MSISDN + "=? AND " + DBConstants.HAS_LEFT + "=0 AND " + DBConstants.GROUP_ID + " NOT IN (SELECT " + DBConstants.GROUP_ID + " FROM "
					+ DBConstants.GROUP_INFO_TABLE + " WHERE " + DBConstants.GROUP_ALIVE + " =0)";
			List<String> groupConversations = new ArrayList<String>();
			cursor = mDb.query(DBConstants.GROUP_MEMBERS_TABLE, new String[] { DBConstants.GROUP_ID }, selection, new String[] { msisdn }, null, null, null);
			int groupIdIdx = cursor.getColumnIndex(DBConstants.GROUP_ID);
			while (cursor.moveToNext())
			{
				groupConversations.add(cursor.getString(groupIdIdx));
			}
			return groupConversations;
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	/**
	 * Called when forwarding a message so that the groups can also be displayed in the contact list.
	 *
	 * @return
	 */
	public List<ContactInfo> getGroupNameAndParticipantsAsContacts(Context context)
	{
		Cursor groupCursor = null;
		try
		{
			List<ContactInfo> groups = new ArrayList<ContactInfo>();
			groupCursor = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.GROUP_NAME }, DBConstants.GROUP_ALIVE + "=1", null, null, null,
					null);
			int groupNameIdx = groupCursor.getColumnIndex(DBConstants.GROUP_NAME);
			int groupIdIdx = groupCursor.getColumnIndex(DBConstants.GROUP_ID);
			while (groupCursor.moveToNext())
			{
				String groupId = groupCursor.getString(groupIdIdx);
				String groupName = groupCursor.getString(groupNameIdx);

				List<PairModified<GroupParticipant, String>> groupParticipantMap = ContactManager.getInstance().getGroupParticipants(groupId, true, false, false);
				groupName = TextUtils.isEmpty(groupName) ? OneToNConversation.defaultConversationName(groupParticipantMap) : groupName;
				int numMembers = groupParticipantMap.size();

				// Here we make this string the msisdn so that it can be
				// displayed in the list view when forwarding the message
				String numberMembers = context.getString(R.string.num_people, (numMembers + 1));

				ContactInfo group = new ContactInfo(groupId, groupId, groupName, numberMembers, true);
				groups.add(group);
			}

			return groups;
		}
		finally
		{
			if (groupCursor != null)
			{
				groupCursor.close();
			}
		}
	}

	public void updateRecencyOfEmoticon(int emoticonIndex, long lastUsed)
	{
		SQLiteStatement insertStatement = null;
		try
		{
			insertStatement = mDb.compileStatement("INSERT OR REPLACE INTO " + DBConstants.EMOTICON_TABLE + " ( " + DBConstants.EMOTICON_NUM + ", " + DBConstants.LAST_USED + " ) "
					+ " VALUES (?, ?)");

			insertStatement.bindLong(1, emoticonIndex);
			insertStatement.bindLong(2, lastUsed);

			long id = insertStatement.executeInsert();
			Logger.d(getClass().getSimpleName(), "iNserted row: " + id);
		}
		finally
		{
			if (insertStatement != null)
			{
				insertStatement.close();
			}
		}
	}

	public int[] fetchEmoticonsOfType(int startOffset, int endOffset, int limit)
	{
		Cursor c = null;
		try
		{
			String[] columns = new String[] { DBConstants.EMOTICON_NUM };
			String selection = DBConstants.EMOTICON_NUM + ">=" + startOffset + (endOffset != 0 ? " AND " + DBConstants.EMOTICON_NUM + "<" + (endOffset) : "");
			String orderBy = DBConstants.LAST_USED + " DESC " + (limit != -1 ? (" LIMIT " + limit) : "");

			c = mDb.query(DBConstants.EMOTICON_TABLE, columns, selection, null, null, null, orderBy);
			int[] emoticonIndices = new int[c.getCount()];
			int emoticonIndexIdx = c.getColumnIndex(DBConstants.EMOTICON_NUM);
			int i = 0;
			while (c.moveToNext())
			{
				emoticonIndices[i++] = c.getInt(emoticonIndexIdx);
			}
			return emoticonIndices;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public long addStatusMessage(StatusMessage statusMessage, boolean showInCentralTimeline)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.STATUS_MAPPED_ID, statusMessage.getMappedId());
		values.put(DBConstants.STATUS_TEXT, statusMessage.getText());
		values.put(DBConstants.MSISDN, statusMessage.getMsisdn());
		values.put(DBConstants.STATUS_TYPE, statusMessage.getStatusMessageType().ordinal());
		values.put(DBConstants.TIMESTAMP, statusMessage.getTimeStamp());
		values.put(DBConstants.SHOW_IN_TIMELINE, showInCentralTimeline);
		values.put(DBConstants.MOOD_ID, statusMessage.getMoodId());
		values.put(DBConstants.TIME_OF_DAY, statusMessage.getTimeOfDay());

		if (!TextUtils.isEmpty(statusMessage.getFileKey()))
		{
			values.put(DBConstants.FILE_KEY, statusMessage.getFileKey());
		}
		/*
		 * Inserting -1 to denote that this status is not a part of any conversation yet.
		 */
		values.put(DBConstants.MESSAGE_ID, -1);

		long id = mDb.insert(DBConstants.STATUS_TABLE, null, values);
		statusMessage.setId(id);

		return id;
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, String... msisdnList)
	{
		return getStatusMessages(timelineUpdatesOnly, -1, -1, msisdnList);
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, int limit, int[] types)
	{
		return getStatusMessages(timelineUpdatesOnly,limit,types,false);
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, int limit, int[] types, boolean getOnlyStealthStatus)
	{
		if (types == null || types.length == 0)
		{
			return null;
		}

		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY, DBConstants.FILE_KEY };

		StringBuilder selection = new StringBuilder();

		StringBuilder typeSelection = new StringBuilder("(");
		for (int type : types)
		{
			typeSelection.append(DatabaseUtils.sqlEscapeString(Integer.toString(type)) + ",");
		}
		typeSelection.replace(typeSelection.lastIndexOf(","), typeSelection.length(), ")");

		selection.append(DBConstants.STATUS_TYPE + " IN " + typeSelection.toString() + (timelineUpdatesOnly ? " AND " : ""));

		if (timelineUpdatesOnly)
		{
			selection.append(DBConstants.SHOW_IN_TIMELINE + " =1 ");
		}

		String orderBy = DBConstants.STATUS_ID + " DESC ";

		if (limit != -1)
		{
			orderBy += "LIMIT " + limit;
		}

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection.toString(), null, null, null, orderBy);

			List<StatusMessage> statusMessages = new ArrayList<StatusMessage>(c.getCount());
			Map<String, List<StatusMessage>> statusMessagesMap = new HashMap<String, List<StatusMessage>>();

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);
			int fileKeyIdx = c.getColumnIndex(DBConstants.FILE_KEY);

			List<String> msisdns = new ArrayList<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				if(getOnlyStealthStatus)
				{
					if(!StealthModeManager.getInstance().isStealthMsisdn(msisdn))
					{
						continue;
					}
				}

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx), c.getString(fileKeyIdx));
				statusMessages.add(statusMessage);

				List<StatusMessage> msisdnMessages = statusMessagesMap.get(msisdn);
				if (msisdnMessages == null)
				{
					msisdns.add(msisdn);
					msisdnMessages = new ArrayList<StatusMessage>();
					statusMessagesMap.put(msisdn, msisdnMessages);
				}
				msisdnMessages.add(statusMessage);
			}
			if (msisdns.size() > 0)
			{
				List<ContactInfo> contactList = ContactManager.getInstance().getContact(msisdns, true, true);

				for (ContactInfo contactInfo : contactList)
				{
					List<StatusMessage> msisdnMessages = statusMessagesMap.get(contactInfo.getMsisdn());
					if (msisdnMessages != null)
					{
						for (StatusMessage statusMessage : msisdnMessages)
						{
							statusMessage.setName(contactInfo.getName());
						}
					}
				}
			}

			return statusMessages;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<StatusMessage> getStatusMessages(boolean timelineUpdatesOnly, int limit, int lastStatusId, String... msisdnList)
	{
		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY, DBConstants.FILE_KEY};

		StringBuilder selection = new StringBuilder();

		StringBuilder msisdnSelection = null;
		if (msisdnList != null)
		{
			msisdnSelection = new StringBuilder("(");
			for (String msisdn : msisdnList)
			{
				msisdnSelection.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
			msisdnSelection.replace(msisdnSelection.lastIndexOf(","), msisdnSelection.length(), ")");
		}

		if (!TextUtils.isEmpty(msisdnSelection))
		{
			selection.append(DBConstants.MSISDN + " IN " + msisdnSelection.toString() + (timelineUpdatesOnly ? " AND " : ""));
		}
		if (timelineUpdatesOnly)
		{
			selection.append(DBConstants.SHOW_IN_TIMELINE + " =1 ");
		}
		if (lastStatusId != -1)
		{
			selection.append(" AND " + DBConstants.STATUS_ID + " < " + lastStatusId);
		}

		String orderBy = DBConstants.STATUS_ID + " DESC ";

		if (limit != -1)
		{
			orderBy += "LIMIT " + limit;
		}

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection.toString(), null, null, null, orderBy);

			List<StatusMessage> statusMessages = new ArrayList<StatusMessage>(c.getCount());
			Map<String, List<StatusMessage>> statusMessagesMap = new HashMap<String, List<StatusMessage>>();

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);
			int fileKeyIdx = c.getColumnIndex(DBConstants.FILE_KEY);

			List<String> msisdns = new ArrayList<String>();

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx),c.getString(fileKeyIdx));
				statusMessages.add(statusMessage);

				List<StatusMessage> msisdnMessages = statusMessagesMap.get(msisdn);
				if (msisdnMessages == null)
				{
					msisdns.add(msisdn);
					msisdnMessages = new ArrayList<StatusMessage>();
					statusMessagesMap.put(msisdn, msisdnMessages);
				}
				msisdnMessages.add(statusMessage);
			}
			if (msisdns.size() > 0)
			{
				List<ContactInfo> contactList = ContactManager.getInstance().getContact(msisdns, true, true);

				for (ContactInfo contactInfo : contactList)
				{
					List<StatusMessage> msisdnMessages = statusMessagesMap.get(contactInfo.getMsisdn());
					if (msisdnMessages != null)
					{
						for (StatusMessage statusMessage : msisdnMessages)
						{
							statusMessage.setName(contactInfo.getName());
						}
					}
				}
			}

			return statusMessages;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public StatusMessage getLastStatusMessage(StatusMessage.StatusMessageType[] smTypes, ContactInfo contactInfo)
	{
		ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>(1);
		contactList.add(contactInfo);
		Map<String, StatusMessage> lastSMMap = getLastStatusMessages(false, smTypes, contactList);
		return lastSMMap.get(contactInfo.getMsisdn());
	}

	public Map<String, StatusMessage> getLastStatusMessages(boolean timelineUpdatesOnly, StatusMessage.StatusMessageType[] smTypes, List<ContactInfo> contactList)
	{
		Map<String, StatusMessage> statusMessagesMap = new HashMap<String, StatusMessage>();

		if (contactList == null || contactList.isEmpty())
		{
			return statusMessagesMap;
		}
		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY };

		StringBuilder selection = new StringBuilder();

		StringBuilder msisdnSelection = null;
		msisdnSelection = new StringBuilder("(");
		for (ContactInfo contactInfo : contactList)
		{
			msisdnSelection.append(DatabaseUtils.sqlEscapeString(contactInfo.getMsisdn()) + ",");
		}
		msisdnSelection.replace(msisdnSelection.lastIndexOf(","), msisdnSelection.length(), ")");

		if (!TextUtils.isEmpty(msisdnSelection))
		{
			selection.append(DBConstants.MSISDN + " IN " + msisdnSelection.toString() + (timelineUpdatesOnly ? " AND " : ""));
		}
		if (timelineUpdatesOnly)
		{
			selection.append(DBConstants.SHOW_IN_TIMELINE + " =1 ");
		}

		StringBuilder smTypeSelection = null;
		if (smTypes != null)
		{
			smTypeSelection = new StringBuilder("(");
			for (StatusMessage.StatusMessageType smType : smTypes)
			{
				smTypeSelection.append(smType.ordinal() + ",");
			}
			smTypeSelection.replace(smTypeSelection.lastIndexOf(","), smTypeSelection.length(), ")");
		}

		if (!TextUtils.isEmpty(smTypeSelection))
		{
			selection.append(" AND " + DBConstants.STATUS_TYPE + " IN " + smTypeSelection.toString());
		}
		String orderBy = DBConstants.STATUS_ID + " DESC ";

		String havingSelection = "MAX(" + DBConstants.STATUS_ID + ")=" + DBConstants.STATUS_ID;
		;

		String groupby = DBConstants.MSISDN;
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection.toString(), null, groupby, havingSelection, orderBy);

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				StatusMessage statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx),
						StatusMessageType.values()[c.getInt(typeIdx)], c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx));
				statusMessagesMap.put(msisdn, statusMessage);

			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return statusMessagesMap;
	}
	
	public StatusMessage getStatusMessageFromMappedId(String statusID)
	{
		StatusMessage statusMessage = null;

		String[] columns = new String[] { DBConstants.STATUS_ID, DBConstants.STATUS_MAPPED_ID, DBConstants.MSISDN, DBConstants.STATUS_TEXT, DBConstants.STATUS_TYPE,
				DBConstants.TIMESTAMP, DBConstants.MOOD_ID, DBConstants.TIME_OF_DAY };

		String selection = DBConstants.STATUS_MAPPED_ID + " = ?";
		
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, columns, selection, new String[]{statusID}, null, null, null);

			int idIdx = c.getColumnIndex(DBConstants.STATUS_ID);
			int mappedIdIdx = c.getColumnIndex(DBConstants.STATUS_MAPPED_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			int textIdx = c.getColumnIndex(DBConstants.STATUS_TEXT);
			int typeIdx = c.getColumnIndex(DBConstants.STATUS_TYPE);
			int tsIdx = c.getColumnIndex(DBConstants.TIMESTAMP);
			int moodIdIdx = c.getColumnIndex(DBConstants.MOOD_ID);
			int timeOfDayIdx = c.getColumnIndex(DBConstants.TIME_OF_DAY);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				statusMessage = new StatusMessage(c.getLong(idIdx), c.getString(mappedIdIdx), msisdn, null, c.getString(textIdx), StatusMessageType.values()[c.getInt(typeIdx)],
						c.getLong(tsIdx), c.getInt(moodIdIdx), c.getInt(timeOfDayIdx));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return statusMessage;
	}

	/**
	 * Update column showInTimeline to 1
	 * To show previous SU in Timeline
	 * @return
	 */
	public boolean updateHistoricalStatusMessages(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
		{
			return false;
		}
		
		boolean isComplete = false;
		ContentValues conVal = new ContentValues();
		conVal.put(DBConstants.SHOW_IN_TIMELINE, true);
		
		String whereClause = DBConstants.MSISDN + "=? AND " + DBConstants.SHOW_IN_TIMELINE + "=?";
		String[] whereArgs = new String[] { msisdn, "0" };
		long rowID = mDb.update(DBConstants.STATUS_TABLE, conVal, whereClause, whereArgs);

		if (rowID == -1L)
		{
			isComplete = false;
		}
		else
		{
			isComplete = true;
		}

		return isComplete;
	}
	
	public void setMessageIdForStatus(String statusId, long messageId)
	{
		String whereClause = DBConstants.STATUS_MAPPED_ID + "=?";
		String[] whereArgs = new String[] { statusId };

		ContentValues values = new ContentValues(1);
		values.put(DBConstants.MESSAGE_ID, messageId);

		mDb.update(DBConstants.STATUS_TABLE, values, whereClause, whereArgs);
	}

	public void deleteStatus(String statusId)
	{
		String selection = DBConstants.STATUS_MAPPED_ID + "=?";
		String[] whereArgs = new String[] { statusId };
		/*
		 * First we want the message id corresponding to this status.
		 */
		Cursor c = null;
		long messageId = 0;
		String msisdn = "";
		try
		{
			c = mDb.query(DBConstants.STATUS_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MSISDN }, selection, whereArgs, null, null, null);

			if (c.moveToFirst())
			{
				messageId = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
				msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		/*
		 * Now we delete the status
		 */
		mDb.delete(DBConstants.STATUS_TABLE, selection, whereArgs);

		/*
		 * This would be true if the status message was not a part of any conversation.
		 */
		if (messageId == -1)
		{
			return;
		}
		/*
		 * And we delete the message
		 */
		mDb.delete(DBConstants.MESSAGES_TABLE, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(messageId) });

		/*
		 * Checking if the status message deleted was the last message
		 */
		Cursor c1 = null;
		try
		{
			c1 = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE_METADATA }, DBConstants.IS_STATUS_MSG + "=1 AND " + DBConstants.MSISDN + "=?",
					new String[] { msisdn }, null, null, null);
			if (c1.moveToFirst())
			{
				String metadataString = c1.getString(c1.getColumnIndex(DBConstants.MESSAGE_METADATA));
				try
				{
					MessageMetadata messageMetadata = new MessageMetadata(new JSONObject(metadataString), false);

					if (statusId.equals(messageMetadata.getStatusMessage().getMappedId()))
					{
						deleteMessageFromConversation(msisdn);
					}

				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
		}
		finally
		{
			if (c1 != null)
			{
				c1.close();
			}
		}
	}

	public void deleteStatusMessagesForMsisdn(String msisdn)
	{
		String selection = DBConstants.MSISDN + "=?";
		String[] selectionArgs = { msisdn };

		mDb.delete(DBConstants.STATUS_TABLE, selection, selectionArgs);
	}

	public int getTimelineStatusMessageCount()
	{
		return (int) DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM " + DBConstants.STATUS_TABLE + " WHERE " + DBConstants.SHOW_IN_TIMELINE + " =1", null);
	}

	private void denormaliseConversations(SQLiteDatabase mDb)
	{
		Logger.d(getClass().getSimpleName(), "Denormalisingggg");
		String query = "SELECT " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MSG_STATUS + ", "
				+ DBConstants.MESSAGES_TABLE + "." + DBConstants.TIMESTAMP + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID + ", " + DBConstants.MESSAGES_TABLE
				+ "." + DBConstants.MAPPED_MSG_ID + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_METADATA + ", " + DBConstants.MESSAGES_TABLE + "."
				+ DBConstants.GROUP_PARTICIPANT + ", " + DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID + " FROM messages LEFT OUTER JOIN messages "
				+ "AS max ON messages.convid = max.convid AND max.msgid > messages.msgid" + " WHERE max.msgid IS NULL";

		Cursor c = null;

		try
		{
			c = mDb.rawQuery(query, null);
			mDb.beginTransaction();

			final int convIdx = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.CONV_ID);
			final int msgColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE);
			final int msgStatusColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MSG_STATUS);
			final int tsColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.TIMESTAMP);
			final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MAPPED_MSG_ID);
			final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID);
			final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_METADATA);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.MESSAGES_TABLE + "." + DBConstants.GROUP_PARTICIPANT);

			while (c.moveToNext())
			{
				String message = c.getString(msgColumn);
				int msgState = c.getInt(msgStatusColumn);
				int timeStamp = c.getInt(tsColumn);
				long messageId = c.getLong(msgIdColumn);
				long mappedMessageId = c.getLong(mappedMsgIdColumn);
				String metadata = c.getString(metadataColumn);
				String groupParticipant = c.getString(groupParticipantColumn);
				int convid = c.getInt(convIdx);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE, message);
				contentValues.put(DBConstants.MSG_STATUS, msgState);
				contentValues.put(DBConstants.TIMESTAMP, timeStamp);
				contentValues.put(DBConstants.MESSAGE_ID, messageId);
				contentValues.put(DBConstants.MAPPED_MSG_ID, mappedMessageId);
				contentValues.put(DBConstants.MESSAGE_METADATA, metadata);
				contentValues.put(DBConstants.GROUP_PARTICIPANT, groupParticipant);
				try
				{
					MessageMetadata messageMetadata = new MessageMetadata(new JSONObject(metadata), false);
					contentValues.put(DBConstants.IS_STATUS_MSG, messageMetadata.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE);
				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
				}

				mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.CONV_ID + "=" + convid, null);
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * Called from multi sticker download task
	 * 
	 * @param categoryId
	 * @param totalNum
	 */
	public void updateStickerCountForStickerCategory(String categoryId, int totalNum)
	{
		updateStickerCategoryData(categoryId, null, totalNum, -1, null, null);
	}

	public void removeStickerCategory(String categoryId, boolean forceRemoveCategory)
	{

		try
		{

			mDb.beginTransaction();

			if (forceRemoveCategory)
			{
				Cursor cursor = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, new String[] { DBConstants.UCID }, DBConstants._ID + "=?", new String[] { categoryId }, null, null,
						null);
				if (cursor != null && cursor.moveToFirst())
				{
					String ucid = Integer.toString(cursor.getInt(cursor.getColumnIndex(DBConstants.UCID)));
					mDb.delete(DBConstants.STICKER_CATEGORY_RANK_TABLE, DBConstants.UCID + "=?", new String[] { ucid });
				}
				mDb.delete(DBConstants.STICKER_CATEGORIES_TABLE, DBConstants._ID + "=?", new String[] { categoryId });
			}
			else
			{
				mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, getDefaultStickerCategoryValues(categoryId), DBConstants._ID + "=?", new String[] { categoryId });
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{

			mDb.endTransaction();
		}
	}

	private ContentValues getDefaultStickerCategoryValues(String categoryId)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, categoryId);
		contentValues.put(DBConstants.IS_DOWNLOADED, 0);
		contentValues.put(DBConstants.IS_VISIBLE, 0);
		contentValues.put(DBConstants.CATEGORY_INDEX, -1);
		contentValues.put(DBConstants.UPDATE_AVAILABLE, 0);
		return contentValues;
	}

	public void updateStickerCategoryData(String categoryId, Boolean updateAvailable, int totalStickerCount, int categorySize, String description, String stickerListString)
	{
		ContentValues contentValues = new ContentValues();
		if(updateAvailable != null)
		{
			contentValues.put(DBConstants.UPDATE_AVAILABLE, updateAvailable);
		}
		if(totalStickerCount != -1)
		{
			contentValues.put(DBConstants.TOTAL_NUMBER, totalStickerCount);
		}
		if(categorySize != -1)
		{
			contentValues.put(DBConstants.CATEGORY_SIZE, categorySize);
		}
		if (description != null)
		{
			contentValues.put(DBConstants.CATEGORY_DESCRIPTION, description);
		}
		if (stickerListString != null)
		{
			contentValues.put(DBConstants.STICKER_LIST, stickerListString);
		}

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { categoryId });
	}

	public LinkedHashMap<String, StickerCategory> getAllStickerCategoriesWithVisibility(boolean isVisible)
	{
		Cursor c = null;
		LinkedHashMap<String, StickerCategory> stickerDataMap;

		try
		{
			String selection = DBConstants.IS_VISIBLE + "=?" + " AND " + DBConstants.IS_DOWNLOADED + "=?";
			String[] selectionArgs = { isVisible ? Integer.toString(1) : Integer.toString(0), Integer.toString(1) };  // all categories which are downloaded

			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null, selection, selectionArgs, null, null, null);
			stickerDataMap = parseStickerCategoriesCursor(c);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return stickerDataMap;
	}

	public Map<Integer, StickerCategory> parseCategorySearchResultsCursor(Cursor c)
	{
		Map<Integer, StickerCategory> stickerDataMap = new LinkedHashMap<Integer, StickerCategory>(c.getCount());

		while (c.moveToNext())
		{
				String categoryId = c.getString(c.getColumnIndex(DBConstants._ID));
				String categoryName = c.getString(c.getColumnIndex(DBConstants.CATEGORY_NAME));
				boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
				boolean isDownloaded = c.getInt(c.getColumnIndex(DBConstants.IS_DOWNLOADED)) == 1;
				int totalStickers = c.getInt(c.getColumnIndex(DBConstants.TOTAL_NUMBER));
				int ucid = c.getInt(c.getColumnIndex(DBConstants.UCID));
				int rank = c.getInt(c.getColumnIndex(DBConstants.RANK));

				StickerCategory s = new StickerCategory.Builder().setCategoryId(categoryId).setCategoryName(categoryName).setTotalStickers(totalStickers)
						.setUpdateAvailable(updateAvailable).setIsDownloaded(isDownloaded).setUcid(ucid).setShopRank(rank).build();
				stickerDataMap.put(ucid, s);
		}
		return stickerDataMap;
	}

	public LinkedHashMap<String, StickerCategory> parseStickerCategoriesCursor(Cursor c)
	{
		LinkedHashMap<String, StickerCategory> stickerDataMap = new LinkedHashMap<String, StickerCategory>(c.getCount());

		while (c.moveToNext())
		{
			try
			{
				String categoryId = c.getString(c.getColumnIndex(DBConstants._ID));
				String categoryName = c.getString(c.getColumnIndex(DBConstants.CATEGORY_NAME));
				boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
				boolean isVisible = c.getInt(c.getColumnIndex(DBConstants.IS_VISIBLE)) == 1;
				boolean isCustom = c.getInt(c.getColumnIndex(DBConstants.IS_CUSTOM)) == 1;
				boolean isDownloaded = c.getInt(c.getColumnIndex(DBConstants.IS_DOWNLOADED)) == 1;
				int catIndex = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
				int categorySize = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_SIZE));
				int totalStickers = c.getInt(c.getColumnIndex(DBConstants.TOTAL_NUMBER));
				String categoryDescription = c.getString(c.getColumnIndex(DBConstants.CATEGORY_DESCRIPTION));
				String stickerListJSONString = c.getString(c.getColumnIndex(DBConstants.STICKER_LIST));
				String similarPacksJSONString = c.getString(c.getColumnIndex(DBConstants.SIMILAR_CATEGORIES));
				String author = c.getString(c.getColumnIndex(DBConstants.AUTHOR));
				String copyRightString = c.getString(c.getColumnIndex(DBConstants.COPYRIGHT_STRING));

				StickerCategory s;
				/**
				 * Making sure that Recents category is added as CustomStickerCategory only. This is being done to avoid ClassCast exception on the PlayStore.
				 */
				if (isCustom || categoryId.equals(StickerManager.RECENT))
				{

					s = new CustomStickerCategory.Builder().setCategoryId(categoryId).setCategoryName(categoryName).setCategorySize(categorySize).setTotalStickers(totalStickers)
							.setUpdateAvailable(updateAvailable).setIsVisible(isVisible).setIsCustom(isCustom).setIsAdded(true).setCatIndex(catIndex).build();
				}
				else
				{
					s = new StickerCategory.Builder().setCategoryId(categoryId).setCategoryName(categoryName).setCategorySize(categorySize).setCategoryDesc(categoryDescription)
							.setTotalStickers(totalStickers).setUpdateAvailable(updateAvailable).setIsVisible(isVisible).setIsCustom(isCustom).setIsAdded(true)
							.setIsDownloaded(isDownloaded).setCatIndex(catIndex).setAllStickerListString(stickerListJSONString).setSimilarPacksString(similarPacksJSONString).setAuthor(author).setCopyRightString(copyRightString).build();
				}
				stickerDataMap.put(categoryId, s);
			}
			catch (Exception e)
			{
				Logger.e(getClass().getSimpleName(), e.getMessage());
				e.printStackTrace();
			}
		}
		return stickerDataMap;
	}

	public long addProtip(Protip protip)
	{
		ContentValues contentValues = new ContentValues();

		contentValues.put(DBConstants.PROTIP_MAPPED_ID, protip.getMappedId());
		contentValues.put(DBConstants.HEADER, protip.getHeader());
		contentValues.put(DBConstants.PROTIP_TEXT, protip.getText());
		contentValues.put(DBConstants.IMAGE_URL, protip.getImageURL());
		contentValues.put(DBConstants.WAIT_TIME, protip.getWaitTime());
		contentValues.put(DBConstants.TIMESTAMP, protip.getTimeStamp());
		contentValues.put(DBConstants.PROTIP_GAMING_DOWNLOAD_URL, protip.getGameDownlodURL());
		return mDb.insert(DBConstants.PROTIP_TABLE, null, contentValues);
	}

	public Protip getLastProtip()
	{
		String[] columns = { "max(" + DBConstants.ID + ") as " + DBConstants.ID, DBConstants.PROTIP_MAPPED_ID, DBConstants.HEADER, DBConstants.PROTIP_TEXT, DBConstants.IMAGE_URL,
				DBConstants.WAIT_TIME, DBConstants.TIMESTAMP, DBConstants.PROTIP_GAMING_DOWNLOAD_URL };

		return getProtip(columns, null, null);
	}

	public Protip getProtipForId(long id)
	{
		String[] columns = { DBConstants.ID, DBConstants.PROTIP_MAPPED_ID, DBConstants.HEADER, DBConstants.PROTIP_TEXT, DBConstants.IMAGE_URL, DBConstants.WAIT_TIME,
				DBConstants.TIMESTAMP, DBConstants.PROTIP_GAMING_DOWNLOAD_URL };
		String selection = DBConstants.ID + "=?";
		String[] selectionArgs = { Long.toString(id) };

		return getProtip(columns, selection, selectionArgs);
	}

	private Protip getProtip(String[] columns, String selection, String[] selectionArgs)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.PROTIP_TABLE, columns, selection, selectionArgs, null, null, null);
			if (!c.moveToFirst())
			{
				return null;
			}

			long id = c.getLong(c.getColumnIndex(DBConstants.ID));
			String mappedId = c.getString(c.getColumnIndex(DBConstants.PROTIP_MAPPED_ID));
			String header = c.getString(c.getColumnIndex(DBConstants.HEADER));
			String text = c.getString(c.getColumnIndex(DBConstants.PROTIP_TEXT));
			String url = c.getString(c.getColumnIndex(DBConstants.IMAGE_URL));
			long waitTime = c.getLong(c.getColumnIndex(DBConstants.WAIT_TIME));
			long timeStamp = c.getLong(c.getColumnIndex(DBConstants.TIMESTAMP));
			String gamingDownloadURL = c.getString(c.getColumnIndex(DBConstants.PROTIP_GAMING_DOWNLOAD_URL));
			if (mappedId == null)
			{
				return null;
			}

			return new Protip(id, mappedId, header, text, url, waitTime, timeStamp, gamingDownloadURL);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteProtip(String mappedId)
	{
		mDb.delete(DBConstants.PROTIP_TABLE, DBConstants.PROTIP_MAPPED_ID + "=?", new String[]{mappedId});
	}

	public void incrementUnreadCounter(String msisdn)
	{
		incrementUnreadCounter(msisdn, 1);
	}

	public void incrementUnreadCounter(String msisdn, int incrementValue)
	{
		String sqlString = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.UNREAD_COUNT + "=" + DBConstants.UNREAD_COUNT + " + " + incrementValue + " WHERE "
				+ DBConstants.MSISDN + "=" + DatabaseUtils.sqlEscapeString(msisdn);
		mDb.execSQL(sqlString);
	}

	public void initialiseSharedMediaAndFileThumbnailTable(SQLiteDatabase mDb)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MESSAGE_METADATA }, null, null, null, null, null);

			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int metatdataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

			mDb.beginTransaction();

			while (c.moveToNext())
			{
				long messageId = c.getLong(msgIdIdx);
				String metadata = c.getString(metatdataIdx);

				try
				{
					JSONObject metadataJson = new JSONObject(metadata);

					JSONArray fileJsonArray = metadataJson.optJSONArray(HikeConstants.FILES);

					if (fileJsonArray == null)
					{
						continue;
					}

					/*
					 * we don't need to initialize sharedMediaTable here.
					 */

					JSONObject fileJson = fileJsonArray.getJSONObject(0);

					HikeFile hikeFile = new HikeFile(fileJson, false);

					if (hikeFile.getThumbnail() == null)
					{
						continue;
					}

					byte[] imageBytes = Base64.decode(hikeFile.getThumbnailString(), Base64.DEFAULT);

                    addFileThumbnail(hikeFile.getFileKey(), imageBytes);

					fileJson.remove(HikeConstants.THUMBNAIL);

					ContentValues fileJsonUpdateValues = new ContentValues();
					fileJsonUpdateValues.put(DBConstants.MESSAGE_METADATA, metadataJson.toString());
					mDb.update(DBConstants.MESSAGES_TABLE, fileJsonUpdateValues, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(messageId) });

				}
				catch (JSONException e)
				{
					Logger.w(getClass().getSimpleName(), "Invalid JSON");
				}
			}

			mDb.setTransactionSuccessful();

		}
		finally
		{
			mDb.endTransaction();
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void initialiseSharedMediaAndFileThumbnailTable()
	{
		initialiseSharedMediaAndFileThumbnailTable(mDb);
	}

	public void addSharedMedia(ConvMessage convMessage)
	{
		ContentValues sharedMediaValues = getSharedMediaContentValues(convMessage);

		mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, sharedMediaValues);
	}

	private ContentValues getSharedMediaContentValues(ConvMessage convMessage )
	{
		ContentValues contentValues = new ContentValues();
		boolean isSent = ConvMessage.isMessageSent(convMessage.getState());
		contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
		contentValues.put(DBConstants.SERVER_ID, convMessage.getServerId());
		contentValues.put(DBConstants.MSISDN, convMessage.getMsisdn());
		contentValues.put(DBConstants.GROUP_PARTICIPANT, convMessage.getGroupParticipantMsisdn() != null ? convMessage.getGroupParticipantMsisdn() : "");
		contentValues.put(DBConstants.TIMESTAMP, convMessage.getTimestamp());
		contentValues.put(DBConstants.IS_SENT, isSent);
		if (NativeCardUtils.isNativeCardFTMessage(convMessage)) {
			contentValues.put(DBConstants.HIKE_FILE_TYPE, convMessage.platformMessageMetadata.getHikeFiles().get(0).getHikeFileType().ordinal());
			putMetadataAccordingToFileType(contentValues, convMessage.platformMessageMetadata.getJSON(), true);
		} else {
			contentValues.put(DBConstants.HIKE_FILE_TYPE, convMessage.getMetadata().getHikeFiles().get(0).getHikeFileType().ordinal());
			putMetadataAccordingToFileType(contentValues, convMessage.getMetadata(), true);
		}
		return contentValues;
	}

	private void putMetadataAccordingToFileType(ContentValues contentValues, MessageMetadata metadata, boolean removeThumbnail)
	{
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadata.getJSON().optString(HikeConstants.CONTENT_TYPE)))
		{
			contentValues.put(DBConstants.MESSAGE_METADATA, metadata.getJSON().toString());
		}
		else if (metadata.getJSON().has(HikeConstants.FILES))
		{
			String thumbnailString = null;
			if(removeThumbnail)
			{
				/*
				 * We need to remove thumbnail from json object before saving in sharedMediaTable
				 */
				thumbnailString = metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0).optString(HikeConstants.THUMBNAIL);
				if (!TextUtils.isEmpty(thumbnailString))
				{
					metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0).remove(HikeConstants.THUMBNAIL);
				}
			}
			JSONObject jsonObj = metadata.getJSON().optJSONArray(HikeConstants.FILES).optJSONObject(0);

			String caption = metadata.getJSON().optString(HikeConstants.CAPTION);
			if(!TextUtils.isEmpty(caption))
			{
				try
				{
					jsonObj.put(HikeConstants.CAPTION, caption);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			contentValues.put(DBConstants.MESSAGE_METADATA, jsonObj.toString());

			if (thumbnailString != null && !TextUtils.isEmpty(thumbnailString))
			{
				addThumbnailStringToMetadata(metadata, thumbnailString);
			}
		}
	}
	private void putMetadataAccordingToFileType(ContentValues contentValues, JSONObject metadata, boolean removeThumbnail)
	{
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadata.optString(HikeConstants.CONTENT_TYPE)))
		{
			contentValues.put(DBConstants.MESSAGE_METADATA, metadata.toString());
		}
		else if (metadata.has(HikeConstants.FILES))
		{
			String thumbnailString = null;
			if(removeThumbnail)
			{
				/*
				 * We need to remove thumbnail from json object before saving in sharedMediaTable
				 */
				thumbnailString = metadata.optJSONArray(HikeConstants.FILES).optJSONObject(0).optString(HikeConstants.THUMBNAIL);
				if (!TextUtils.isEmpty(thumbnailString))
				{
					metadata.optJSONArray(HikeConstants.FILES).optJSONObject(0).remove(HikeConstants.THUMBNAIL);
				}
			}
			JSONObject jsonObj = metadata.optJSONArray(HikeConstants.FILES).optJSONObject(0);

			String caption = metadata.optString(HikeConstants.CAPTION);
			if(!TextUtils.isEmpty(caption))
			{
				try
				{
					jsonObj.put(HikeConstants.CAPTION, caption);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			contentValues.put(DBConstants.MESSAGE_METADATA, jsonObj.toString());

			if (thumbnailString != null && !TextUtils.isEmpty(thumbnailString))
			{
				addThumbnailStringToMetadata(metadata, thumbnailString);
			}
		}
	}
	public void addFileThumbnail(String fileKey, byte[] imageBytes)
	{
        ContentValues fileThumbnailValues = new ContentValues();

        Cursor c = null;
        try {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[]{REF_COUNT}, DBConstants.FILE_KEY + "=?", new String[]{fileKey}, null, null, null);


            if (!c.moveToFirst()) {
                fileThumbnailValues.put(DBConstants.FILE_KEY, fileKey);
                fileThumbnailValues.put(DBConstants.IMAGE, imageBytes);
                fileThumbnailValues.put(REF_COUNT, 1);
                mDb.insert(DBConstants.FILE_THUMBNAIL_TABLE, null, fileThumbnailValues);
            } else {
                int refCount = c.getInt(c.getColumnIndex(REF_COUNT));
                fileThumbnailValues.put(REF_COUNT, refCount + 1);
                mDb.update(DBConstants.FILE_THUMBNAIL_TABLE, fileThumbnailValues, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            }
        }
        finally {
            if (c != null)
            {
                c.close();
            }
        }
    }

    public void reduceRefCount(String fileKey){

        ContentValues fileThumbnailValues = new ContentValues();

        Cursor c = null;
        try {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[]{REF_COUNT}, DBConstants.FILE_KEY + "=?", new String[]{fileKey}, null, null, null);
            if (!c.moveToFirst())
                return;

            int refCount = c.getInt(c.getColumnIndex(REF_COUNT));

            if (refCount > 1) {
                fileThumbnailValues.put(REF_COUNT, refCount - 1);
                mDb.update(DBConstants.FILE_THUMBNAIL_TABLE, fileThumbnailValues, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            } else {
                mDb.delete(DBConstants.FILE_THUMBNAIL_TABLE, DBConstants.FILE_KEY + "=?", new String[]{fileKey});
            }

        }
        finally {
            if (c != null)
            {
                c.close();
            }
        }

    }


	public Drawable getFileThumbnail(String fileKey)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.FILE_KEY + "=?", new String[] { fileKey }, null, null, null);

			if (!c.moveToFirst())
			{
				return null;
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return HikeBitmapFactory.getBitmapDrawable(mContext.getResources(), HikeBitmapFactory.decodeBitmapFromByteArray(icondata, Bitmap.Config.RGB_565));
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

    public byte [] getThumbnail(String fileKey){

        Cursor c = null;
        try
        {
            c = mDb.query(DBConstants.FILE_THUMBNAIL_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.FILE_KEY + "=?", new String[] { fileKey }, null, null, null);

            if (!c.moveToFirst())
            {
                return null;
            }

            return c.getBlob(c.getColumnIndex(DBConstants.IMAGE));

        }
        finally
        {
            if (c != null)
            {
                c.close();
            }
        }

    }

	public String getReadByValueForMessageID(long msgId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.READ_BY }, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(msgId) }, null, null, null);
			if (!c.moveToFirst())
			{
				return null;
			}
			String readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
			return readByString;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Pair<String, Long> getReadByValueForGroup(String groupId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.READ_BY, DBConstants.MESSAGE_ID }, DBConstants.GROUP_ID + "=?", new String[] { groupId }, null,
					null, null);
			if (!c.moveToFirst())
			{
				return null;
			}
			String readByString = c.getString(c.getColumnIndex(DBConstants.READ_BY));
			Long msgId = c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
			return new Pair<String, Long>(readByString, msgId);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteAllProtipsBeforeThisId(long id)
	{
		mDb.delete(DBConstants.PROTIP_TABLE, DBConstants.ID + "< ?", new String[] { Long.toString(id) });
	}

	public void setChatBackground(String msisdn, String newBgId, long timeStamp)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		values.put(DBConstants.ChatThemes.THEME_COL_BG_ID, newBgId);
		values.put(ChatThemes.PREV_THEME_ID_COL, getChatThemeIdForMsisdn(msisdn));
		values.put(DBConstants.BG_TIMESTAMP, timeStamp);

		int id = (int) mDb.insertWithOnConflict(DBConstants.CHAT_PROPERTIES_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		if (id < 0)
		{
			mDb.update(DBConstants.CHAT_PROPERTIES_TABLE, values, DBConstants.MSISDN + "=?", new String[] { msisdn });
		}
	}

	/**
	 * this method returns the currentTheme for the chat irrespective of whether it is downloaded or not
	 * @param msisdn
	 * @return
	 */
	public Pair<String, Long> getChatThemeIdAndTimestamp(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CHAT_PROPERTIES_TABLE, new String[] { DBConstants.BG_TIMESTAMP, DBConstants.ChatThemes.THEME_COL_BG_ID }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null,
					null);
			if (c.moveToFirst())
			{
				String chatThemeId = c.getString(c.getColumnIndex(DBConstants.ChatThemes.THEME_COL_BG_ID));
				Long timeStamp = c.getLong(c.getColumnIndex(DBConstants.BG_TIMESTAMP));

				return new Pair<String, Long>(chatThemeId, timeStamp);
			}
			return null;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * the method returns the currentTheme for the chat if the assets for it are downloaded.
	 * else, returns the most recent chat theme for this chat which is completely downloaded
	 * @param msisdn
	 * @return
	 */
	public String getChatThemeIdForMsisdn(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CHAT_PROPERTIES_TABLE, new String[] { ChatThemes.THEME_COL_BG_ID, ChatThemes.PREV_THEME_ID_COL }, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (c.moveToFirst())
			{
				try
				{
					String currentThemeId = c.getString(c.getColumnIndex(DBConstants.ChatThemes.THEME_COL_BG_ID));
					String prevThemeId = c.getString(c.getColumnIndex(ChatThemes.PREV_THEME_ID_COL));
					if(ChatThemeManager.getInstance().isThemeAvailable(currentThemeId))
					{
						return currentThemeId;
					}
					else
					{
						return prevThemeId;
					}
				}
				catch (IllegalArgumentException e)
				{
					/*
					 * For invalid theme id, we return the default id.
					 */
					return ChatThemeManager.getInstance().defaultChatThemeId;
				}
			}
			return ChatThemeManager.getInstance().defaultChatThemeId;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public Object[] getChatProperties(String msisdn)
	{
		Mute mute = new Mute.InitBuilder(msisdn).build();
		String chatThemeId = ChatThemeManager.getInstance().defaultChatThemeId;
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CHAT_PROPERTIES_TABLE, new String[] { DBConstants.IS_MUTE, DBConstants.MUTE_TIMESTAMP, DBConstants.MUTE_DURATION, ChatThemes.THEME_COL_BG_ID, ChatThemes.PREV_THEME_ID_COL }, DBConstants.MSISDN + " =? ", new String[] { msisdn }, null, null, null);

			if (c.moveToNext())
			{
				boolean isMute = c.getInt(c.getColumnIndex(DBConstants.IS_MUTE)) == 1 ? true : false;
				int muteDuration = c.getInt(c.getColumnIndex(DBConstants.MUTE_DURATION));
				mute.setIsMute(isMute);
				mute.setMuteDuration(muteDuration);
				mute.setMuteTimestamp(c.getLong(c.getColumnIndex(DBConstants.MUTE_TIMESTAMP)));

				try
				{
					String currentThemeId = c.getString(c.getColumnIndex(DBConstants.ChatThemes.THEME_COL_BG_ID));
					String prevThemeId = c.getString(c.getColumnIndex(ChatThemes.PREV_THEME_ID_COL));
					if(ChatThemeManager.getInstance().isThemeAvailable(currentThemeId))
					{
						chatThemeId = currentThemeId;
					}
					else
					{
						chatThemeId = prevThemeId;
					}
				}
				catch (IllegalArgumentException e)
				{
					/*
					 * For invalid theme id, we return the default id.
					 */
					chatThemeId = ChatThemeManager.getInstance().defaultChatThemeId;
				}
			}
			return new Object[] {chatThemeId, mute};
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void removeChatThemeForMsisdn(String msisdn)
	{

		ContentValues values = new ContentValues();
		values.put(ChatThemes.THEME_COL_BG_ID, "");
		values.put(DBConstants.BG_TIMESTAMP, 0);
		values.put(ChatThemes.PREV_THEME_ID_COL, "0");

		mDb.update(DBConstants.CHAT_PROPERTIES_TABLE, values, DBConstants.MSISDN + "=?", new String[] { msisdn });
	}

	public void setChatThemesFromArray(JSONArray chatBackgroundArray)
	{
		ContentValues contentValues = new ContentValues();

		try{
			mDb.beginTransaction();
			if (Utils.isEmpty(chatBackgroundArray))
			{
				return;
			}
			for (int i = 0; i < chatBackgroundArray.length(); i++)
			{
				JSONObject chatBgJson = chatBackgroundArray.optJSONObject(i);

				if (chatBgJson == null)
				{
					continue;
				}

				String msisdn = chatBgJson.optString(HikeConstants.MSISDN);
				String bgId = chatBgJson.optString(HikeConstants.BG_ID);

				if (TextUtils.isEmpty(msisdn))
				{
					continue;
				}

				/*
				 * We don't support custom themes yet.
				 */
				if (chatBgJson.optBoolean(HikeConstants.CUSTOM)) {

					Logger.d(getClass().getSimpleName(), "We don't support custom themes yet");
					continue;
				}

				contentValues.put(DBConstants.MSISDN, msisdn);
				contentValues.put(ChatThemes.THEME_COL_BG_ID, bgId);

				int id = (int) mDb.insertWithOnConflict(DBConstants.CHAT_PROPERTIES_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
				if (id < 0)
				{
					mDb.update(DBConstants.CHAT_PROPERTIES_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.CHAT_BACKGROUND_CHANGED, new Pair<String, String>(msisdn, bgId));
			}
			mDb.setTransactionSuccessful();

		} catch (Exception e) {

			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();

		} finally {

			mDb.endTransaction();
		}

	}

	public void changeGroupOwner(String groupId, String msisdn)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.GROUP_OWNER, msisdn);

		mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
	}

	public boolean toggleStealth(String msisdn, boolean isStealth)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.IS_STEALTH, isStealth ? 1 : 0);

		int rowsUpdated = mDb.update(DBConstants.CONVERSATIONS_TABLE, values, DBConstants.MSISDN + "=?", new String[] { msisdn });
		boolean updated = (rowsUpdated > 0);
		
		if(!updated)
		{
			//here if the row does not exist in Conversations table, we find the last ConvMessage of the contact from Messages table
			ConvMessage lastConvMessage = getLastMessage(msisdn);
			if(lastConvMessage == null)
			{
				lastConvMessage = Utils.makeConvMessage(msisdn, mContext.getString(R.string.start_new_chat), true, State.RECEIVED_READ);
			}
			else
			{
				lastConvMessage.setTimestamp((long) (System.currentTimeMillis()/1000));
			}
			
			Conversation conv = addConversation(msisdn, true, null, null, lastConvMessage, -1l, null);
			
			ContentValues contentValues = getContentValueForConversationMessage(lastConvMessage, lastConvMessage.getTimestamp());
			contentValues.put(DBConstants.IS_STEALTH, isStealth ? 1:0);
			rowsUpdated = mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
			updated = (rowsUpdated > 0);
		}
		return updated;
	}

	public void addStealthMsisdnToMap()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.IS_STEALTH + "=1", null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				StealthModeManager.getInstance().markStealthMsisdn(c.getString(msisdnIdx), true, false);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void getBotHashmap()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, null, null, null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIdx);

				BotInfo botInfo = getBotInfoFromCursor(c, msisdn);

				if (botInfo != null)
				{
					Logger.v("BOT", "Putting Bot Info in hashmap " + botInfo.toString());
					HikeMessengerApp.hikeBotInfoMap.put(msisdn, botInfo);
					// Putting the fake bot contact in contact manager as well here
					ContactInfo contact = new ContactInfo(msisdn, msisdn, botInfo.getConversationName(), msisdn);
					ContactManager.getInstance().updateContacts(contact);
				}

				else
				{
					Logger.wtf("BOT", "got null bot Info for msisdn : " + msisdn);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public ConvMessage showParticipantStatusMessage(String groupId)
	{

		List<PairModified<GroupParticipant, String>> smsParticipants = ContactManager.getInstance().getGroupParticipants(groupId, true, true);

		if (smsParticipants.isEmpty())
		{
			return null;
		}

		JSONObject dndJSON = new JSONObject();
		JSONArray dndParticipants = new JSONArray();

		for (PairModified<GroupParticipant, String> smsParticipantEntry : smsParticipants)
		{
			GroupParticipant smsParticipant = smsParticipantEntry.getFirst();
			String msisdn = smsParticipant.getContactInfo().getMsisdn();
			if (smsParticipant.onDnd())
			{
				dndParticipants.put(msisdn);
			}
		}

		if (dndParticipants.length() == 0)
		{
			// No DND participants. Just return
			return null;
		}
		try
		{
			dndJSON.put(HikeConstants.FROM, groupId);
			dndJSON.put(HikeConstants.TYPE, HikeConstants.DND);
			dndJSON.put(HikeConstants.DND_USERS, dndParticipants);

			ConvMessage convMessage = new ConvMessage(dndJSON, null, mContext, false);
			updateShownStatus(groupId);

			return convMessage;
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return null;
	}

	public int getUnreadPinCounter()
	{
		return 0;
	}

	public List<ConvMessage> getAllPinMessage(int startFrom, int limit, String msisdn, Conversation conv)
	{
		String limitStr = (limit == -1) ? null : new Integer(limit).toString();
		String startFromStr = (startFrom < 0) ? "0" : String.valueOf(startFrom);
		String selection = DBConstants.MSISDN + " = ?" + " AND " + DBConstants.MESSAGE_TYPE + "==" + HikeConstants.MESSAGE_TYPE.TEXT_PIN;
		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			String query = "SELECT " + DBConstants.MESSAGE + "," + DBConstants.MSG_STATUS + "," + DBConstants.TIMESTAMP + "," + DBConstants.MESSAGE_ID + ","
					+ DBConstants.MAPPED_MSG_ID + "," + DBConstants.MESSAGE_METADATA + "," + DBConstants.GROUP_PARTICIPANT + "," + DBConstants.IS_HIKE_MESSAGE + ","
					+ DBConstants.READ_BY + "," + DBConstants.MESSAGE_TYPE + ","+DBConstants.HIKE_CONTENT.CONTENT_ID + "," + HIKE_CONTENT.NAMESPACE + "," + DBConstants.MESSAGE_ORIGIN_TYPE + "," + DBConstants.SORTING_ID + " FROM " + DBConstants.MESSAGES_TABLE + " where " + selection + " order by " + DBConstants.SORTING_ID
					+ " DESC LIMIT " + limitStr + " OFFSET " + startFrom;
			c = mDb.rawQuery(query, new String[]{msisdn});

			List<ConvMessage> elements = getMessagesFromDB(c, conv);

			return elements;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void markPinMessagesRead(List<ConvMessage> msgs)
	{

	}

	public void removeLastPinMessageForConv(String convId)
	{

	}

	public ArrayList<String> getOfflineMsisdnsList(String msisdnStatement)
	{
		Cursor c = null;
		ArrayList<String> msisdnResult = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[]{" MIN (" + DBConstants.TIMESTAMP + ") AS TIME", DBConstants.MSISDN}, DBConstants.MSISDN + " IN "
					+ msisdnStatement + " AND " + DBConstants.MSG_STATUS + "=" + State.SENT_CONFIRMED.ordinal() + " AND  " + DBConstants.IS_HIKE_MESSAGE + "=" + "1", null,
				DBConstants.MSISDN, null, null);

			if (c != null)
			{
				msisdnResult = new ArrayList<String>(c.getCount());
				while (c.moveToNext())
				{
					long msgTime = c.getLong(c.getColumnIndex("TIME"));
					if ((System.currentTimeMillis() / 1000 - msgTime) > HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME)
					{
						msisdnResult.add(c.getString(c.getColumnIndex(DBConstants.MSISDN)));
					}

					Logger.d("HikeToOffline", "TimeStamp : " + c.getLong(c.getColumnIndex("TIME")));
					Logger.d("HikeToOffline", "Msisdn : " + c.getString(c.getColumnIndex(DBConstants.MSISDN)));
				}
			}
		}
		catch (Exception e)
		{
			Logger.e("HikeToOffline", "Exception ", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return msisdnResult;
	}

	public void addMessageHashNMsisdnNReadByForGroup()
	{
		try
		{
			mDb.beginTransaction();
			addMessageHashAndMsisdn();
			updateReadByArrayForGroups();
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void upgradeForDatabaseVersion28()
	{
		try
		{
			mDb.beginTransaction();
			addMessageMsisdn();
			updateToNewSharedMediaTable();
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	private void addMessageMsisdn()
	{
		Cursor c = null;
		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);

			final int convIdIndex = c.getColumnIndex(DBConstants.CONV_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				Integer convId = c.getInt(convIdIndex);
				String msisdn = c.getString(msisdnIndex);
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(), msisdn));
			}

			for (Pair<String, String> pair : convIdtoMsisdn)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MSISDN, pair.second);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { pair.first });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in adding msisdn ", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private void addMessageHashAndMsisdn()
	{
		Cursor c = null;
		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);

			final int convIdIndex = c.getColumnIndex(DBConstants.CONV_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				Integer convId = c.getInt(convIdIndex);
				String msisdn = c.getString(msisdnIndex);
				convIdtoMsisdn.add(new Pair<String, String>(convId.toString(), msisdn));
			}

			for (Pair<String, String> pair : convIdtoMsisdn)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MSISDN, pair.second);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.CONV_ID + "=?", new String[] { pair.first });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		try
		{
			ArrayList<Pair<String, String>> convIdtoMsisdn = new ArrayList<Pair<String, String>>();
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID, DBConstants.MAPPED_MSG_ID,
					DBConstants.MSISDN }, null, null, null, null, null);

			final int messageIndex = c.getColumnIndex(DBConstants.MESSAGE);
			final int tsIndex = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int msgIdIndex = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int mappedMsgIdIndex = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);

			while (c.moveToNext())
			{
				String message = c.getString(messageIndex);
				int ts = c.getInt(tsIndex);
				int mappedId = c.getInt(mappedMsgIdIndex);
				String msisdn = c.getString(msisdnIndex);
				String messageHash = createMessageHash(msisdn, (long) mappedId, message, (long) ts);
				Integer msgId = c.getInt(msgIdIndex);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_HASH, messageHash);
				mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[]{msgId.toString()});
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	private void updateReadByArrayForGroups()
	{
		Cursor c = null;
		try
		{
			HashMap<String, String> groupIdMap = getAllOneToNConversations();
			String convIdStatement = PhoneUtils.getMsisdnStatement(groupIdMap.keySet());
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MAX (" + DBConstants.MESSAGE_ID + ") AS msgid", DBConstants.READ_BY, DBConstants.CONV_ID },
					DBConstants.CONV_ID + " IN " + convIdStatement + " AND " + DBConstants.MSG_STATUS + " = " + State.SENT_DELIVERED_READ.ordinal(), null, DBConstants.CONV_ID,
					null, null);

			final int convIdIdx = c.getColumnIndex(DBConstants.CONV_ID);
			final int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int readByIdx = c.getColumnIndex(DBConstants.READ_BY);

			while (c.moveToNext())
			{
				long messageId = c.getLong(msgIdIdx);
				long convId = c.getLong(convIdIdx);
				String readByString = c.getString(readByIdx);
				String groupId = groupIdMap.get(Long.toString(convId));

				Logger.d("readByValues", "conVid : " + convId + " messageId : " + messageId + " groupId : " + groupId + " readby : " + readByString);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_ID, messageId);
				contentValues.put(DBConstants.READ_BY, readByString);
				mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { groupId });
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public HashMap<String, String> getAllOneToNConversations()
	{
		Cursor c = null;
		String msisdn;
		HashMap<String, String> groupIdMap = new HashMap<String, String>();

		try
		{
			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.CONV_ID, DBConstants.MSISDN }, null, null, null, null, null);
			while (c.moveToNext())
			{
				msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				if (OneToNConversationUtils.isOneToNConversation(msisdn))
				{
					groupIdMap.put(Integer.toString(c.getInt(c.getColumnIndex(DBConstants.CONV_ID))), msisdn);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return groupIdMap;
	}
	public ConvMessage getMessageFromID(long messageID,String msisdn){


		String selection = DBConstants.MESSAGE_ID + " = ?";

		Cursor c = null;
		try
		{
			/* TODO this should be ORDER BY timestamp */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSG_STATUS, DBConstants.TIMESTAMP, DBConstants.MESSAGE_ID,
				DBConstants.MAPPED_MSG_ID, DBConstants.MESSAGE_METADATA, DBConstants.GROUP_PARTICIPANT, DBConstants.IS_HIKE_MESSAGE, DBConstants.READ_BY,
				DBConstants.MESSAGE_TYPE,DBConstants.HIKE_CONTENT.CONTENT_ID, HIKE_CONTENT.NAMESPACE,DBConstants.MESSAGE_ORIGIN_TYPE, DBConstants.SORTING_ID}, selection, new String[] { Long.toString(messageID) }, null, null,null);




			if(c!=null) {
				//Get message fromDB
				final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
				final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
				final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
				final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
				final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
				final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
				final int isHikeMessageColumn = c.getColumnIndex(DBConstants.IS_HIKE_MESSAGE);
				final int readByColumn = c.getColumnIndex(DBConstants.READ_BY);
				final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
				final int typeColumn = c.getColumnIndex(DBConstants.MESSAGE_TYPE);
				final int messageOriginTypeColumn = c.getColumnIndex(DBConstants.MESSAGE_ORIGIN_TYPE);
				final int contentIdColumn = c.getColumnIndex(DBConstants.HIKE_CONTENT.CONTENT_ID);
				final int nameSpaceColumn = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
				final int sortId = c.getColumnIndex(DBConstants.SORTING_ID);

				Logger.d("convmessage","cursor count "+c.getCount());

				while (c.moveToNext()) {
					boolean isHikeMessage = true;

					ConvMessage message = new ConvMessage(c.getString(msgColumn), msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), !isHikeMessage, c.getInt(typeColumn), c.getInt(contentIdColumn), c.getString(nameSpaceColumn), c.getLong(sortId));

					message.setReadByArray(c.getString(readByColumn));
					message.setMessageOriginType(ConvMessage.originTypeValue(c.getInt(messageOriginTypeColumn)));
					String metadata = c.getString(metadataColumn);
					try
					{
						if(message.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT){
							message.platformMessageMetadata = new PlatformMessageMetadata(metadata);
						}else if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
							message.webMetadata = new WebMetadata(metadata);
						}else{
							message.setMetadata(metadata);
						}
					}catch (JSONException e)
					{
						Logger.w("ravi", "Invalid JSON metadata", e);
					}
					return message;
				}
			}

		}catch (Exception e){
			Logger.d("ravi", "exception in creating convmessge"+e.getMessage());
		}finally{
			if(c!=null)
				c.close();
		}
		return  null;
	}
	/**
	 * Generates a list of messages based on the query passed to it.
	 *
	 * @param c
	 *            The query on the message table.
	 * @param conversation
	 *            Conversation for which the messages are to be fetched.
	 * @return The list on ConvMessage objects.
	 */
	private List<ConvMessage> getMessagesFromDB(Cursor c, Conversation conversation)
	{
		 return getMessagesFromDB(c,conversation.isOnHike(),conversation.getMsisdn());
	}
	
	private List<ConvMessage> getMessagesFromDB(Cursor c,boolean isOnHike,String msisdn)
	{
		final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
		final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
		final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
		final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
		final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
		final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);
		final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
		final int isHikeMessageColumn = c.getColumnIndex(DBConstants.IS_HIKE_MESSAGE);
		final int readByColumn = c.getColumnIndex(DBConstants.READ_BY);
		final int typeColumn = c.getColumnIndex(DBConstants.MESSAGE_TYPE);
		final int messageOriginTypeColumn = c.getColumnIndex(DBConstants.MESSAGE_ORIGIN_TYPE);
		final int loveIdColumn = c.getColumnIndex(DBConstants.HIKE_CONV_DB.LOVE_ID_REL);
		final int contentIdColumn = c.getColumnIndex(DBConstants.HIKE_CONTENT.CONTENT_ID);
		final int nameSpaceColumn = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
		final int sortId = c.getColumnIndex(DBConstants.SORTING_ID);
		List<ConvMessage> elements = new ArrayList<ConvMessage>(c.getCount());
		SparseArray<ConvMessage> contentMessages = new SparseArray<ConvMessage>();
		StringBuilder loveIds = new StringBuilder("(");
		while (c.moveToNext())
		{
			int hikeMessage = c.getInt(isHikeMessageColumn);
			boolean isHikeMessage = hikeMessage == -1 ?isOnHike : (hikeMessage == 0 ? false : true);

			ConvMessage message = new ConvMessage(c.getString(msgColumn),msisdn, c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
					c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), !isHikeMessage, c.getInt(typeColumn),c.getInt(contentIdColumn), c.getString(nameSpaceColumn), c.getLong(sortId));
			
			//if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT){
//				int loveId = c.getInt(loveIdColumn);
//				// DEFAULT value of love id is -1
//				if(loveId !=- 1){
//				loveIds.append(loveId);
//				loveIds.append(",");
//				contentMessages.put(loveId, message);
//				}
			//}
			String metadata = c.getString(metadataColumn);
			try
			{
                if(message.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT){
                    message.platformMessageMetadata = new PlatformMessageMetadata(metadata, message.isSent());
                }else if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
					message.webMetadata = new WebMetadata(metadata);
				}else{
                    message.setMetadata(metadata);
                }
			}
			catch (JSONException e)
			{
				Logger.w(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
			}
			message.setReadByArray(c.getString(readByColumn));
			message.setMessageOriginType(ConvMessage.originTypeValue(c.getInt(messageOriginTypeColumn)));
			elements.add(elements.size(), message);
		}
		if(contentMessages.size()>0){
			String loveIdsCommaSeparated = loveIds.substring(0, loveIds.length()-1)+")"; // -1 coz of last comma
			// fetch love data from love table
			List<ContentLove> list = getContentLoveData(loveIdsCommaSeparated);
			for(ContentLove love : list){
				ConvMessage message = (ConvMessage) contentMessages.get(love.loveId);
				if(message!=null){
					message.contentLove = love;
				}
			}
		}
		return elements;
	}

	private List<ContentLove> getContentLoveData(String loveIdsCommaSeparated){
		String query = "SELECT " + DBConstants.HIKE_CONV_DB.LOVE_ID + ","
				+ HIKE_CONV_DB.COUNT + "," + HIKE_CONV_DB.USER_STATUS + ","
				+ HIKE_CONV_DB.TIMESTAMP + " FROM " + HIKE_CONV_DB.LOVE_TABLE
				+ " WHERE " + HIKE_CONV_DB.LOVE_ID + " in "
				+ loveIdsCommaSeparated;
		Cursor c = mDb.rawQuery(query, null);
		List<ContentLove> listToReturn = new ArrayList<ContentLove>();
		if(c.getCount()>0){
			int loveId = c.getColumnIndex(HIKE_CONV_DB.LOVE_ID);
			int count = c.getColumnIndex(HIKE_CONV_DB.COUNT);
			int userStatus = c.getColumnIndex(HIKE_CONV_DB.USER_STATUS);
			int timeStamp = c.getColumnIndex(HIKE_CONV_DB.TIMESTAMP);
			while(c.moveToNext()){
				ContentLove love = new ContentLove();
				love.loveId = c.getInt(loveId);
				love.loveCount = c.getInt(count);
				love.userStatus = c.getInt(userStatus) ==1;
				love.updatedTimeStamp = new Date(c.getLong(timeStamp));
				listToReturn.add(love);
			}
		}
		return listToReturn;
	}

	/**
	 * Updates group info table with last sent message id for a group. It also clears readby column for that group
	 *
	 * @param convMessage
	 */
	private void updateReadBy(ConvMessage convMessage)
	{
		if (OneToNConversationUtils.isOneToNConversation(convMessage.getMsisdn()) && convMessage.isSent())
		{
			String readByString = null;
			ContentValues contentValues = new ContentValues();
			contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
			contentValues.put(DBConstants.READ_BY, readByString);
			mDb.update(DBConstants.GROUP_INFO_TABLE, contentValues, DBConstants.GROUP_ID + "=?", new String[] { convMessage.getMsisdn() });
		}
	}

	/**
	 * This function queries message table for max mr id from list of ids that are sent by me with msisdn as groupid
	 *
	 * @param groupId
	 *            -- group id for which mr packet came
	 * @param ids
	 *            -- list of message ids that came in "mr" packet
	 * @return maxMrId from list of ids that are sent by me else -1
	 */
	public long getMrIdForGroup(String groupId, ArrayList<Long> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return -1;
		}
		String inQureyString = Utils.valuesToCommaSepratedString(ids);

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { " MAX (" + DBConstants.MESSAGE_ID + ") AS msgid" }, DBConstants.MSISDN + "=?  AND " + DBConstants.MESSAGE_ID + " IN " + inQureyString,
					new String[] { groupId }, null, null, null);
			if (c.moveToFirst())
			{
				return c.getLong(c.getColumnIndex(DBConstants.MESSAGE_ID));
			}
			return -1;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public int getPinCount(String msisdn)
	{
		String selection = DBConstants.MSISDN + " = ?" + " AND " + DBConstants.MESSAGE_TYPE + "==" + HikeConstants.MESSAGE_TYPE.TEXT_PIN;
		Cursor c = null;
		try
		{
				c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MSISDN }, selection, new String[]{msisdn}, null, null, null);
				return c.getCount();
		}
		finally{
			if(c!=null)
				c.close();
		}
	}

	public void updateToNewSharedMediaTable()
	{
		final String TEMP_SHARED_MEDIA_TABLE = "tempSharedMediaTable";

		/*
		 * Renaming current sharedMediaTable to temp_sharedMediaTable
		 */
		String sql1 = "ALTER TABLE " + DBConstants.SHARED_MEDIA_TABLE + " RENAME TO " + TEMP_SHARED_MEDIA_TABLE;

		/*
		 * creating new sharedMediaTable with updated schema
		 */
		String sql2 = getSharedMediaTableCreateQuery();

		/*
		 * selecting rows from messages table for all msgIds from old shared Media table
		 * SELECT msgid, msisdn, fromMsisdn, timestamp, msgStatus, metadata FROM messages where messages.msgId IN (SELECT msgid FROM tempSharedMediaTable);
		 */
		String sql3 = "SELECT " + DBConstants.MESSAGE_ID + ", " + DBConstants.MSISDN + ", " + DBConstants.GROUP_PARTICIPANT + ", " +  DBConstants.TIMESTAMP + ", " + DBConstants.MSG_STATUS + ", "
				+ DBConstants.MESSAGE_METADATA + " FROM " + DBConstants.MESSAGES_TABLE + " WHERE " + DBConstants.MESSAGES_TABLE + "." + DBConstants.MESSAGE_ID
				+ " IN ( SELECT " + DBConstants.MESSAGE_ID + " FROM " + TEMP_SHARED_MEDIA_TABLE + " )";

		String sql4 = "DROP TABLE IF EXISTS " + TEMP_SHARED_MEDIA_TABLE;

		mDb.execSQL(sql1);
		mDb.execSQL(sql2);

		Cursor c = null;
		try
		{
			c = mDb.rawQuery(sql3, null);

			final int msgIdIndex = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int msisdnIndex = c.getColumnIndex(DBConstants.MSISDN);
			final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
			final int tsIndex = c.getColumnIndex(DBConstants.TIMESTAMP);
			final int msgStatusIndex = c.getColumnIndex(DBConstants.MSG_STATUS);
			final int metadataIndex = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

			while (c.moveToNext())
			{
				long msgId = c.getLong(msgIdIndex);
				String msisdn = c.getString(msisdnIndex);
				long ts = c.getLong(tsIndex);
				int messageStatus = c.getInt(msgStatusIndex);
				String messageMetadataString = c.getString(metadataIndex);
				String groupParticipant = c.getString(groupParticipantColumn);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.MESSAGE_ID, msgId);
				contentValues.put(DBConstants.MSISDN, msisdn);
				contentValues.put(DBConstants.GROUP_PARTICIPANT, groupParticipant);
				contentValues.put(DBConstants.TIMESTAMP, ts);
				contentValues.put(DBConstants.IS_SENT, ConvMessage.isMessageSent(State.values()[messageStatus]));

				/*
				 * Extracting the hikeFileType from message metadata 
				 */
				JSONObject metadataFileArrayJson = new JSONObject(messageMetadataString);
				if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadataFileArrayJson.optString(HikeConstants.CONTENT_TYPE)))
				{
					contentValues.put(DBConstants.MESSAGE_METADATA, metadataFileArrayJson.toString());
				}
				else if (metadataFileArrayJson.has(HikeConstants.FILES))
				{
					JSONObject messageMetadata = metadataFileArrayJson.optJSONArray(HikeConstants.FILES).optJSONObject(0);
					String contentTypeString = messageMetadata.optString(HikeConstants.CONTENT_TYPE);
					boolean isRecording = messageMetadata.optLong(HikeConstants.PLAYTIME, -1) != -1;
					HikeFileType hikeFileType = HikeFileType.fromString(contentTypeString, isRecording);

					contentValues.put(DBConstants.HIKE_FILE_TYPE, hikeFileType.ordinal());
					contentValues.put(DBConstants.MESSAGE_METADATA, messageMetadata.toString());
				}

				mDb.insert(DBConstants.SHARED_MEDIA_TABLE, null, contentValues);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateReadByArrayForGroups",e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		
		/*
		 * Now we can drop the temprory shared media table.
		 */
		mDb.execSQL(sql4);
	}

	public String getSharedMediaTableCreateQuery()
	{
		/*
		 * creating new sharedMediaTable with updated schema
		 */
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.SHARED_MEDIA_TABLE + " (" + DBConstants.MESSAGE_ID + " INTEGER PRIMARY KEY, "
				+ DBConstants.MSISDN + " TEXT, " + DBConstants.GROUP_PARTICIPANT + " TEXT, " + DBConstants.TIMESTAMP + " INTEGER, " + DBConstants.IS_SENT + " INT, " + DBConstants.HIKE_FILE_TYPE + " INTEGER, "
				+ DBConstants.MESSAGE_METADATA + " TEXT, "+ DBConstants.SERVER_ID + " INTEGER "+" )";

		return sql;
	}

	public List<?> getSharedMedia(String msisdn, int limit, long maxMsgId, boolean onlyMedia)
	{
		return getSharedMedia(msisdn, limit, maxMsgId, onlyMedia, false);
	}


	public String getSharedMediaSelection(boolean onlyMedia)
	{
		StringBuilder hfTypeSelection = null;

		HikeFileType[] mediaFileTypes;
		if (onlyMedia)
		{
			mediaFileTypes = new HikeFileType[] { HikeFileType.IMAGE, HikeFileType.VIDEO };
		}
		else
		{
			mediaFileTypes = new HikeFileType[] { HikeFileType.OTHER, HikeFileType.AUDIO ,HikeFileType.APK };
		}

		hfTypeSelection = new StringBuilder("(");
		for (HikeFileType hfType : mediaFileTypes)
		{
			hfTypeSelection.append(hfType.ordinal() + ",");
		}
		hfTypeSelection.replace(hfTypeSelection.lastIndexOf(","), hfTypeSelection.length(), ")");
		return hfTypeSelection.toString();

	}

	/*
	 * itemsToLeft : true implies all items which has msgId less than given msgId : false implies all items which has msgId greater than given msgId
	 * 
	 * returns list in order of msgId max to min.
	 */
	public List<?> getSharedMedia(String msisdn, int limit, long givenMsgId, boolean onlyMedia, boolean itemsToRight)
	{
		String msgIdSelection = DBConstants.MESSAGE_ID + (itemsToRight ? ">" : "<") + givenMsgId;
		String hfTypeSelection = getSharedMediaSelection(onlyMedia);

		String selection =  DBConstants.MSISDN + " = ?" + (givenMsgId == -1 ? "" : " AND " + msgIdSelection) + " AND "
				+ (DBConstants.HIKE_FILE_TYPE + " IN " + hfTypeSelection);

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.SHARED_MEDIA_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.GROUP_PARTICIPANT, DBConstants.TIMESTAMP, DBConstants.IS_SENT,
					DBConstants.MESSAGE_METADATA }, selection, new String[] { msisdn }, null, null, DBConstants.MESSAGE_ID + (itemsToRight ? " ASC" : " DESC"), null);

			List<?> sharedFilesList;
			if(onlyMedia)
			{
				sharedFilesList = new ArrayList<HikeSharedFile>(limit);
			}
			else
			{
				sharedFilesList = new ArrayList<FileListItem>(limit);
			}

			SharedMediaCursorIterator cursorIterator = new SharedMediaCursorIterator(c, msisdn);
			while (cursorIterator.hasNext() && limit>0)
			{
				HikeSharedFile hikeSharedFile = cursorIterator.next();
				if(hikeSharedFile.exactFilePathFileExists())
				{
					limit--;

					if (onlyMedia)
					{
						((List<HikeSharedFile>) sharedFilesList).add(hikeSharedFile);
					}
					else
					{
						((List<FileListItem>) sharedFilesList).add(new FileListItem(hikeSharedFile));
					}
				}
			}

			return sharedFilesList;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteActivityFeedForMsisdn(List<String> stealthMsisdns)
	{
		if(Utils.isEmpty(stealthMsisdns))
		{
			return;
		}

		for(String msisdn: stealthMsisdns)
		{
			mDb.delete(DBConstants.FEED_TABLE, DBConstants.FEED_ACTOR + "=?", new String[] {msisdn});
		}
	}

	public Pair<List<StickerCategory>, List<String>> getStickerCategoriesForDataUpdate(int limit)
	{
		Cursor cursor = null;
		List<StickerCategory> metadataUpdateList = null;
		List<String> tagdataUpdateList = null;
		try
		{
			/*
			 * This will fetch the top 10000 categories from the order table and the corresponding ts of the categories from the categories in sorted order based on order table
			 * whose pack metadata is not updated
			 */
			String query = "Select " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.UCID + "," + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
					+ DBConstants.IS_PACK_METADATA_UPDATED + "," + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.IS_PACK_TAGDATA_UPDATED + ", "
					+ DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UPDATED_METADATA_TIMESTAMP + " from " + DBConstants.STICKER_CATEGORY_RANK_TABLE
					+ " LEFT OUTER JOIN " + DBConstants.STICKER_CATEGORIES_TABLE + " ON " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.UCID + "="
					+ DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UCID  + " order by "
					+ DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.RANK + " asc " + " limit "
					+ limit;
			cursor = mDb.rawQuery(query, null);
			metadataUpdateList = new ArrayList<StickerCategory>(cursor.getCount());
			tagdataUpdateList = new ArrayList<String>(cursor.getCount());

			int ucidIdx = cursor.getColumnIndex(DBConstants.UCID);
			int isMetaUpdatedIdx = cursor.getColumnIndex(DBConstants.IS_PACK_METADATA_UPDATED);
			int isTagUpdatedIdx = cursor.getColumnIndex(DBConstants.IS_PACK_TAGDATA_UPDATED);
            int metadataUpdateIdx = cursor.getColumnIndex(DBConstants.UPDATED_METADATA_TIMESTAMP);
            
            
			if (cursor.getCount() > 0)
			{
				while (cursor.moveToNext())
				{
					int ucid = cursor.getInt(ucidIdx);

					if (cursor.getInt(isMetaUpdatedIdx) == 0)
					{
						StickerCategory stickerCategory = new StickerCategory.Builder().setPackUpdationTime(cursor.getInt(metadataUpdateIdx)).setUcid(ucid).build();
						metadataUpdateList.add(stickerCategory);
					}

					if (cursor.getInt(isTagUpdatedIdx) == 0)
					{
						tagdataUpdateList.add(Integer.toString(ucid));
					}

				}
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}

		return new Pair<List<StickerCategory>, List<String>>(metadataUpdateList, tagdataUpdateList);
	}

	public Cursor getCursorFromStickerCategoryTable(int limit)
	{
		/*
		 * This will fetch the top categories from the order table and the corresponding ts of the categories from the categories in sorted order based on order table whose pack
		 * metadata is not updated
		 */
		String query = "Select " + DBConstants._ID + ", " + DBConstants.CATEGORY_NAME + ", " + DBConstants.TOTAL_NUMBER + ", " + DBConstants.CATEGORY_SIZE + " from "
				+ DBConstants.STICKER_CATEGORY_RANK_TABLE + " LEFT OUTER JOIN " + DBConstants.STICKER_CATEGORIES_TABLE + " ON " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
				+ DBConstants.UCID + "=" + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UCID + " order by " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
				+ DBConstants.RANK + " asc " + " limit " + limit;
		Cursor cursor = mDb.rawQuery(query, null);
		return cursor;
	}

	public Cursor getCursorForShopMetaDataUpdate(int limit)
	{
		/*
		 * This will fetch the top 10000 categories from the order table and the corresponding ts of the categories from the categories in sorted order based on order table whose
		 * pack metadata is not updated
		 */
		String query = "Select " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.UCID + "," + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
				+ DBConstants.IS_PACK_METADATA_UPDATED + ", " + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UPDATED_METADATA_TIMESTAMP + " from "
				+ DBConstants.STICKER_CATEGORY_RANK_TABLE + " LEFT OUTER JOIN " + DBConstants.STICKER_CATEGORIES_TABLE + " ON " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
				+ DBConstants.UCID + "=" + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UCID + " order by " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "."
				+ DBConstants.RANK + " asc " + " limit " + limit;

		Cursor cursor = mDb.rawQuery(query, null);

		return cursor;
	}

	public List<StickerCategory> getCategoriesForShopMetadataUpdate(Cursor cursor)
	{
		List<StickerCategory> list = null;
		try
		{
			/*
			 * This will fetch the top 10000 categories from the order table and the corresponding ts of the categories from the categories in sorted order based on order table
			 * whose pack metadata is not updated
			 */
			if (cursor!= null && cursor.getCount() > 0)
			{
				list = new ArrayList<>(cursor.getCount());

				int updatedMetadataTsIdx = cursor.getColumnIndex(DBConstants.UPDATED_METADATA_TIMESTAMP);
				int ucidIdx = cursor.getColumnIndex(DBConstants.UCID);
				int isPackMetadataUpdatedIdx = cursor.getColumnIndex(DBConstants.IS_PACK_METADATA_UPDATED);
				while (cursor.moveToNext())
				{
					if (cursor.getInt(isPackMetadataUpdatedIdx) == 0)
					{
						StickerCategory stickerCategory = new StickerCategory.Builder().setPackUpdationTime(cursor.getInt(updatedMetadataTsIdx)).setUcid(cursor.getInt(ucidIdx))
								.build();
						list.add(stickerCategory);
					}
				}
			}
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "getStickerCatToBeSendForMetaData", e);
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
		return list;
	}

	private class SharedMediaCursorIterator implements Iterator<HikeSharedFile>
	{

		Cursor cursor;
		String msisdn;
		int msgIdIndex;
		int groupParticipantColumn;
		int tsIndex;
		int isSentIndex;
		int metadataIndex;

		public SharedMediaCursorIterator(Cursor c, String msisdn)
		{
			this.cursor = c;
			msgIdIndex = cursor.getColumnIndex(DBConstants.MESSAGE_ID);
			groupParticipantColumn = cursor.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
			tsIndex = cursor.getColumnIndex(DBConstants.TIMESTAMP);
			isSentIndex = cursor.getColumnIndex(DBConstants.IS_SENT);
			metadataIndex = cursor.getColumnIndex(DBConstants.MESSAGE_METADATA);

			this.msisdn = msisdn;
		}

		@Override
		public boolean hasNext()
		{
			return cursor.getPosition() != cursor.getCount()-1;
		}

		@Override
		public HikeSharedFile next()
		{
			if (cursor.moveToNext())
			{
				long msgId = cursor.getLong(msgIdIndex);
				long ts = cursor.getLong(tsIndex);
				boolean isSent = cursor.getInt(isSentIndex) != 0;
				String messageMetadata = cursor.getString(metadataIndex);
				String groupParticipantMsisdn = cursor.getString(groupParticipantColumn);

				HikeSharedFile hikeSharedFile;
				try
				{
					hikeSharedFile = new HikeSharedFile(new JSONObject(messageMetadata), isSent, msgId, msisdn, ts, groupParticipantMsisdn);
					return hikeSharedFile;
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					return null;
				}
			}
			return null;
		}

		@Override
		public void remove()
		{

		}
	};

	public int getSharedMediaCount(String msisdn, boolean onlyMedia)
	{
		int count = 0;
		String hfTypeSelection = getSharedMediaSelection(onlyMedia);

		Cursor c = null;

		String selection =  DBConstants.MSISDN + " = ?"  + " AND "
				+ (DBConstants.HIKE_FILE_TYPE + " IN " + hfTypeSelection);

		try
		{
			c = mDb.query(DBConstants.SHARED_MEDIA_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.GROUP_PARTICIPANT, DBConstants.TIMESTAMP, DBConstants.IS_SENT,
					DBConstants.MESSAGE_METADATA }, selection, new String[] { msisdn }, null, null, null, null);

			SharedMediaCursorIterator cursorIterator = new SharedMediaCursorIterator(c, msisdn);
			while (cursorIterator.hasNext())
			{
				HikeSharedFile hikeSharedFile = cursorIterator.next();
				if(hikeSharedFile.exactFilePathFileExists())
				{
					count++;
				}
			}
			return count;
		}
		finally
		{
			if(c!=null)
				c.close();
		}
	}

	public void deleteEmptyConversations(SQLiteDatabase mDb)
	{
		Cursor c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.MESSAGE, DBConstants.MSISDN, DBConstants.MESSAGE_METADATA }, null, null, null, null, null);

		int msgIdx = c.getColumnIndex(DBConstants.MESSAGE);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int metadataIdx = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

		StringBuilder deleteSelection = new StringBuilder("(");

		while (c.moveToNext())
		{
			String message = c.getString(msgIdx);
			String metadata = c.getString(metadataIdx);
			String msisdn = c.getString(msisdnIdx);

			if (message == null && metadata == null && !OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				deleteSelection.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
		}

		String deleteSelectionString = deleteSelection.toString();
		if (!"(".equals(deleteSelectionString))
		{
			deleteSelectionString = new String(deleteSelectionString.substring(0, deleteSelectionString.length() - 1) + ")");
			mDb.delete(DBConstants.CONVERSATIONS_TABLE, DBConstants.MSISDN + " IN " + deleteSelectionString, null);
		}
	}

	/**
	 * Returns the sum of unread messages for all conversations.
	 *
	 * @return
	 */
	public int getTotalUnreadMessages()
	{
		int unreadMessages = 0;
		unreadMessages=getTotalUnreadMessagesConversation();
		unreadMessages += Utils.getNotificationCount(mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0), false);

		return unreadMessages;
	}

	public int getTotalUnreadMessagesConversation()
	{
		return getTotalUnreadMessagesConversation(true);
	}

	/**
	 * @param includeStealth
	 *            whether to include the hidden conversations unread count or not
	 * @return total unread count of conversations
	 */
	public int getTotalUnreadMessagesConversation(boolean includeStealth)
	{
		int unreadMessages = 0;
		Cursor c = null;

		try
		{
			String selection = null;
			String[] args = null;
			if (!includeStealth)
			{
				selection = DBConstants.IS_STEALTH + " = ?";
				args = new String[] { "0" };
			}

			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.UNREAD_COUNT }, selection, args, null, null, null);

			final int unreadMessageColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);

			if (c.moveToFirst())
			{
				do
				{
					unreadMessages += c.getInt(unreadMessageColumn);
				}
				while (c.moveToNext());
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return unreadMessages;
	}

	/**
	 * This method calculates the unread count of messages for conversations. This count is used by the
	 * badge counter to update the count of unread message. The method getTotalUnreadMessagesConversation
	 * is not used because for bots like news and games, the unread count stored in the DB is not
	 * reflected exactly on the badgecounter
	 * @param includeStealth
	 * @return unreadMessages count for conversations for badge counter
	 */
	public int getTotalUnreadMessagesConversationBadgeCounter(boolean includeStealth)
	{
		int unreadMessages = 0;
		Cursor c = null;

		try
		{
			String selection = null;
			String[] args = null;
			if (!includeStealth)
			{
				selection = DBConstants.IS_STEALTH + " = ?";
				args = new String[] { "0" };
			}

			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, new String[] { DBConstants.UNREAD_COUNT, DBConstants.MSISDN ,DBConstants.MSG_STATUS}, selection, args, null, null, null);

			if (c!=null && c.moveToFirst())
			{
				final int unreadMessageColumn = c.getColumnIndex(DBConstants.UNREAD_COUNT);
				final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
				final int msgstateColumnIndex=c.getColumnIndex(DBConstants.MSG_STATUS);

				do
				{
					int dbUnreadCount = c.getInt(unreadMessageColumn);
					String msisdn = c.getString(msisdnColumn);
					int msgState=c.getInt(msgstateColumnIndex);
					if (msisdn!=null && BotUtils.isBot(msisdn))
					{

						BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
						if (botInfo.isNonMessagingBot() && dbUnreadCount > 0)
						{
							dbUnreadCount = 1;
						}
					}
					if(msgState< State.RECEIVED_UNREAD.ordinal())
						dbUnreadCount=0;
					unreadMessages += dbUnreadCount;
				}
				while (c.moveToNext());
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return unreadMessages;
	}

	public HashMap<String, ContentValues> getCurrentStickerDataMapping(String tableName)
	{
		HashMap<String, ContentValues> result = new HashMap<String, ContentValues>();
		Cursor c = null;
		try
		{
			c = mDb.query(tableName, new String[] { DBConstants.CATEGORY_ID, DBConstants.TOTAL_NUMBER, DBConstants.UPDATE_AVAILABLE }, null, null,
					null, null, null);

			final int catIdColumnIndex = c.getColumnIndex(DBConstants.CATEGORY_ID);
			final int totalNumColumnIndex = c.getColumnIndex(DBConstants.TOTAL_NUMBER);
			final int updateAvailColumnIndex = c.getColumnIndex(DBConstants.UPDATE_AVAILABLE);

			while (c.moveToNext())
			{
				String catId = c.getString(catIdColumnIndex);
				int totalNumber = c.getInt(totalNumColumnIndex);
				int updateAvailable = c.getInt(updateAvailColumnIndex);

				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants.TOTAL_NUMBER, totalNumber);
				contentValues.put(DBConstants.UPDATE_AVAILABLE, updateAvailable);

				result.put(catId, contentValues);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in updateToNewStickerCategoryTable", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return result;
	}

	public String getStickerCategoryTableCreateQuery()
	{
		/**
		 * creating new Sticker category table with updated schema Category Id : Id of this category Category Name : Category name to show for this category Total Number : total
		 * number of stickers available in this category Reached End : weather we have downloaded all the stickers in this category or Not Update available : weather there is an
		 * update available for this category IS_VISIBLE : weather this category is visible IS_CUSTOM : weather this is a custom category category index : order of this category
		 * among all the categories metadata : other metadata associated` with this category
		 */
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.STICKER_CATEGORIES_TABLE + " (" + DBConstants._ID + " TEXT PRIMARY KEY, " + DBConstants.CATEGORY_NAME + " TEXT, "
				+ DBConstants.TOTAL_NUMBER + " INTEGER, " + DBConstants.UPDATE_AVAILABLE + " INTEGER DEFAULT 0," + DBConstants.IS_VISIBLE + " INTEGER DEFAULT 0,"
				+ DBConstants.IS_CUSTOM + " INTEGER DEFAULT 0," + DBConstants.CATEGORY_INDEX + " INTEGER," + DBConstants.CATEGORY_SIZE + " INTEGER DEFAULT 0,"
				+ DBConstants.CATEGORY_DESCRIPTION + " TEXT," + DBConstants.STICKER_LIST + " TEXT, " + DBConstants.IS_DOWNLOADED + " INTEGER DEFAULT 0, " + DBConstants.SIMILAR_CATEGORIES + " TEXT, " + DBConstants.AUTHOR + " TEXT, " + DBConstants.COPYRIGHT_STRING + " TEXT, "
				+ DBConstants.UCID + " INTEGER," + DBConstants.UPDATED_METADATA_TIMESTAMP + " INTEGER  DEFAULT 0," + DBConstants.UPDATED_PREVIEW_TIMESTAMP + " INTEGER  DEFAULT 0," + DBConstants.IS_DISABLED + " INTEGER DEFAULT 0" + " )";
		return sql;
	}

	public void upgradeForStickerShopVersion1()
	{
		try
		{
			mDb.beginTransaction();
			insertAllCategoriesToStickersTable();
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void insertAllCategoriesToStickersTable()
	{
		final String STICKERS_TABLE = "stickersTable";

		HashMap<String, ContentValues> currentCategoryData = getCurrentStickerDataMapping(STICKERS_TABLE);
		JSONObject jsonObj;
		try
		{
			jsonObj = new JSONObject(Utils.loadJSONFromAsset(mContext, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);
				String categoryName = obj.optString(StickerManager.CATEGORY_NAME);;
				String downloadPreference = obj.optString(StickerManager.DOWNLOAD_PREF);
				/*
				 * All categories which we have set to be visible OR user has already initiated a download for them once will be visible in the pallate
				 */
				int isVisible = obj.optBoolean(StickerManager.IS_VISIBLE)
						|| mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(downloadPreference, false) ? 1 : 0;
				int isCustom = obj.optBoolean(StickerManager.IS_CUSTOM) ? 1 : 0;
				int catIndex = i;

				ContentValues contentValues = currentCategoryData.get(categoryId);
				if (contentValues == null)
				{
					contentValues = new ContentValues();
					if(obj.has(StickerManager.TOTAL_STICKERS))
					{
						contentValues.put(DBConstants.TOTAL_NUMBER, obj.optString(StickerManager.TOTAL_STICKERS));
					}
				}
				contentValues.put(DBConstants._ID, categoryId);
				contentValues.put(DBConstants.CATEGORY_NAME, categoryName);
				contentValues.put(DBConstants.IS_VISIBLE, isVisible);
				contentValues.put(DBConstants.IS_CUSTOM, isCustom);
				contentValues.put(DBConstants.CATEGORY_INDEX, catIndex);

				if(isVisible == 1)
				{
					mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
				}
			}
			/*
			 * Now we can drop the old stickers_table.
			 */
			if(!currentCategoryData.isEmpty())
			{
				mDb.execSQL("DROP TABLE IF EXISTS " + STICKERS_TABLE);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateVisibilityAndIndex(Set<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory stickerCategory : stickerCategories)
			{
				updateVisibilityAndIndex(stickerCategory);
			}
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void updateVisibilityAndIndex(StickerCategory stickerCategory)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_VISIBLE, stickerCategory.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, stickerCategory.getCategoryIndex());

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[]{stickerCategory.getCategoryId()});
	}

	public void insertInToStickerCategoriesTable(StickerCategory stickerCategory)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, stickerCategory.getCategoryId());
		contentValues.put(DBConstants.CATEGORY_NAME, stickerCategory.getCategoryName());
		contentValues.put(DBConstants.TOTAL_NUMBER, stickerCategory.getTotalStickers());
		contentValues.put(DBConstants.UPDATE_AVAILABLE, stickerCategory.isUpdateAvailable());
		contentValues.put(DBConstants.IS_CUSTOM, stickerCategory.isCustom());
		contentValues.put(DBConstants.CATEGORY_SIZE, stickerCategory.getCategorySize());
		contentValues.put(DBConstants.IS_VISIBLE, stickerCategory.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, stickerCategory.getCategoryIndex());
		contentValues.put(DBConstants.CATEGORY_DESCRIPTION, stickerCategory.getDescription());
		contentValues.put(DBConstants.STICKER_LIST, stickerCategory.getAllStickerListString());
		contentValues.put(DBConstants.SIMILAR_CATEGORIES, stickerCategory.getSimilarPacksString());
		contentValues.put(DBConstants.AUTHOR, stickerCategory.getAuthor());
		contentValues.put(DBConstants.COPYRIGHT_STRING, stickerCategory.getCopyRightString());
		contentValues.put(DBConstants.IS_DOWNLOADED, stickerCategory.isDownloaded());
		contentValues.put(DBConstants.UPDATED_PREVIEW_TIMESTAMP, stickerCategory.getPreviewUpdationTime());
		if (mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { stickerCategory.getCategoryId() }) <= 0)
		{
			mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
		}
	}

	public long updateStickerCategoryRanks(JSONArray array, boolean isUpdate)
	{
		ContentValues contentValues;
		long rank = 0;
		try
		{
			mDb.beginTransaction();
			if (!isUpdate)
			{
				rank = 0;
				mDb.delete(DBConstants.STICKER_CATEGORY_RANK_TABLE, null, null);
			}
			else
			{
				rank = getRankCountFromCategoryTable();
			}
			for (int i = 0; i < array.length(); i++)
			{
				contentValues = new ContentValues();
				contentValues.put(DBConstants.RANK, rank);
				contentValues.put(DBConstants.UCID, array.optInt(i, -1));
				mDb.insertWithOnConflict(DBConstants.STICKER_CATEGORY_RANK_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
				rank++;
			}
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.d(getClass().getSimpleName(), e.toString());
		}
		finally
		{
			mDb.endTransaction();
		}
		return rank;
	}

	/*
	 * This method is called from sticker shop call responce.
	 */
	public boolean updateStickerCategoriesInDb(JSONArray jsonArray, boolean insertInShopTable)
	{
		try
		{
			mDb.beginTransaction();
			for (int i = 0; i < jsonArray.length(); i++)
			{
				StickerCategory category = StickerManager.getInstance().parseStickerCategoryMetadata(jsonArray.getJSONObject(i));
				if(category == null)
				{
					continue;
				}
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants._ID, category.getCategoryId());
				contentValues.put(DBConstants.CATEGORY_NAME, category.getCategoryName());
				contentValues.put(DBConstants.TOTAL_NUMBER, category.getTotalStickers());
				contentValues.put(DBConstants.CATEGORY_SIZE, category.getCategorySize());
				contentValues.put(DBConstants.CATEGORY_DESCRIPTION, category.getDescription());
				contentValues.put(DBConstants.STICKER_LIST, category.getAllStickerListString());
				contentValues.put(DBConstants.SIMILAR_CATEGORIES, category.getSimilarPacksString());
				contentValues.put(DBConstants.AUTHOR, category.getAuthor());
				contentValues.put(DBConstants.COPYRIGHT_STRING, category.getCopyRightString());
				if (!insertInShopTable)
				{
					contentValues.put(DBConstants.IS_DISABLED, category.isDisabled() ? 1 : 0);
					contentValues.put(DBConstants.UPDATED_METADATA_TIMESTAMP, category.getPackUpdationTime());
					contentValues.put(DBConstants.UCID, category.getUcid());
				}
				int rowsAffected = mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[]{category.getCategoryId()});
				if(rowsAffected <= 0)
				{
					mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
				}
			}
			mDb.setTransactionSuccessful();
			return true;
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			return false;
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public int updateIsPackMetadataUpdated(List<StickerCategory> list)
	{
		if (Utils.isEmpty(list))
		{
			return -1;
		}

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_PACK_METADATA_UPDATED, 1);
		ArrayList<String> arrayList = new ArrayList<>(list.size());
		for (StickerCategory stickerCategory : list)
		{
			arrayList.add(Integer.toString(stickerCategory.getUcid()));
		}
		return mDb.update(DBConstants.STICKER_CATEGORY_RANK_TABLE, contentValues, DBConstants.UCID + " IN " + Utils.valuesToCommaSepratedString(arrayList), null);

	}

	public int updateIsPackTagdataUpdated(List<CategoryTagData> list)
	{
		if (Utils.isEmpty(list))
		{
			return -1;
		}

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_PACK_TAGDATA_UPDATED, 1);
		ArrayList<String> arrayList = new ArrayList<>(list.size());
		for (CategoryTagData categoryTagData : list)
		{
			arrayList.add(Integer.toString(categoryTagData.getUcid()));
		}
		return mDb.update(DBConstants.STICKER_CATEGORY_RANK_TABLE, contentValues, DBConstants.UCID + " IN " + Utils.valuesToCommaSepratedString(arrayList), null);
	}

    public void updateStickerCategoriesInDb(Collection<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory category : stickerCategories)
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put(DBConstants._ID, category.getCategoryId());
				contentValues.put(DBConstants.CATEGORY_NAME, category.getCategoryName());
				contentValues.put(DBConstants.TOTAL_NUMBER, category.getTotalStickers());
				contentValues.put(DBConstants.CATEGORY_SIZE, category.getCategorySize());

				/*
				 * Adding extra fields as per stickerCategoriesTable
				 */
				contentValues.put(DBConstants.IS_VISIBLE, category.isVisible());
				contentValues.put(DBConstants.CATEGORY_INDEX, category.getCategoryIndex());
				contentValues.put(DBConstants.CATEGORY_DESCRIPTION, category.getDescription());
				contentValues.put(DBConstants.STICKER_LIST, category.getAllStickerListString());
				contentValues.put(DBConstants.SIMILAR_CATEGORIES, category.getSimilarPacksString());
				contentValues.put(DBConstants.AUTHOR, category.getAuthor());
				contentValues.put(DBConstants.COPYRIGHT_STRING, category.getCopyRightString());
				contentValues.put(DBConstants.IS_DOWNLOADED, category.isDownloaded());

				int rowsAffected = mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { category.getCategoryId() });
				/*
				 * if row is not there in stickerCategoriesTable and server specifically tells us to switch on the visibility then we need to insert this item in
				 * stickerCategoriesTable
				 */
				if (category.isVisible() && rowsAffected == 0)
				{
					mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);
				}

			}
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public Map<Integer, StickerCategory> getCategoriesForShopSearch()
	{
		Cursor c = null;

		Map<Integer, StickerCategory> result = null;


		try
		{
			String query = "Select " + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.UCID + "," + DBConstants.STICKER_CATEGORY_RANK_TABLE + "." + DBConstants.RANK
					+ "," + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants._ID + "," + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.CATEGORY_NAME + "," + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.TOTAL_NUMBER + ","
					+ DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.IS_DOWNLOADED + "," + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UPDATE_AVAILABLE
					+ " from " + DBConstants.STICKER_CATEGORY_RANK_TABLE + " INNER JOIN " + DBConstants.STICKER_CATEGORIES_TABLE + " ON " + DBConstants.STICKER_CATEGORY_RANK_TABLE
					+ "." + DBConstants.UCID + "=" + DBConstants.STICKER_CATEGORIES_TABLE + "." + DBConstants.UCID;
			c = mDb.rawQuery(query, null);

            if(c.getCount()>0)
            {
                result = parseCategorySearchResultsCursor(c);
            }

		}
		finally
        {
            if (c != null)
            {
                c.close();
            }
        }

		return result;
	}

	public void clearStickerShop()
	{
		mDb.delete(DBConstants.STICKER_SHOP_TABLE, null, null);
	}

	/**
	 * Returns the maximum category index from StickerCategories Table
	 * @return
	 */
	public int getMaxStickerCategoryIndex()
	{
		Cursor c = null;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, new String[] { "MAX(" + DBConstants.CATEGORY_INDEX + ")" + "AS " + DBConstants.CATEGORY_INDEX }, null, null, null, null, null, null);

			if (c.moveToFirst())
			{
				return c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
			}
			else
				return -1;
		}

		catch (Exception e)
		{
			return -1;
		}

		finally
		{
			if (c != null)
				c.close();
		}
	}

	/**
	 * Returns true if category is inserted.
	 * @param category
	 * @return
	 */
	public boolean insertNewCategoryInPallete(StickerCategory category)
	{
		int rowId = -1;
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants._ID, category.getCategoryId());
		contentValues.put(DBConstants.CATEGORY_NAME, category.getCategoryName());
		contentValues.put(DBConstants.TOTAL_NUMBER, category.getTotalStickers());
		contentValues.put(DBConstants.CATEGORY_SIZE, category.getCategorySize());
		contentValues.put(DBConstants.IS_VISIBLE, category.isVisible());
		contentValues.put(DBConstants.CATEGORY_INDEX, category.getCategoryIndex());
		contentValues.put(DBConstants.CATEGORY_DESCRIPTION, category.getDescription());
		contentValues.put(DBConstants.STICKER_LIST, category.getAllStickerListString());
		contentValues.put(DBConstants.SIMILAR_CATEGORIES, category.getSimilarPacksString());
		contentValues.put(DBConstants.AUTHOR, category.getAuthor());
		contentValues.put(DBConstants.COPYRIGHT_STRING, category.getCopyRightString());
		contentValues.put(DBConstants.IS_DOWNLOADED, category.isDownloaded());

		rowId = (int) mDb.insert(DBConstants.STICKER_CATEGORIES_TABLE, null, contentValues);

		return rowId < 0 ? false : true;
	}

	/**
	 * Used to persist the changes made to the {@link StickerCategory#isUpdateAvailable()} flag
	 *
	 * @param category
	 */
	public void saveUpdateFlagOfStickerCategory(StickerCategory category)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.UPDATE_AVAILABLE, category.isUpdateAvailable());

		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, DBConstants._ID + "=?", new String[] { category.getCategoryId() });

	}

	public void saveUpdateFlagOfStickerCategory(List<StickerCategory> stickerCategories)
	{
		try
		{
			mDb.beginTransaction();
			for (StickerCategory stickerCategory : stickerCategories)
			{
				saveUpdateFlagOfStickerCategory(stickerCategory);
			}
			mDb.setTransactionSuccessful();
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public StickerCategory getStickerCategoryforId(String categoryId)
	{
		Cursor c = null;
		StickerCategory stickerCategory = null;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null, DBConstants._ID + "=?", new String[] { categoryId }, null, null, null);
			stickerCategory = parseStickerCategoryCursor(c);
		}

		catch (Exception e)
		{
			Logger.wtf("StickerManager", "Exception in getStickerCategoryforId  : " + e.toString());
		}

		finally
		{
			if(c != null)
			{
				c.close();
			}
		}

		return stickerCategory;
	}

	private StickerCategory parseStickerCategoryCursor(Cursor c)
	{
		StickerCategory stickerCategory = null;
		if(c.moveToFirst())
		{
			String categoryId = c.getString(c.getColumnIndex(DBConstants._ID));
			String categoryName = c.getString(c.getColumnIndex(DBConstants.CATEGORY_NAME));
			boolean updateAvailable = c.getInt(c.getColumnIndex(DBConstants.UPDATE_AVAILABLE)) == 1;
			boolean isVisible = c.getInt(c.getColumnIndex(DBConstants.IS_VISIBLE)) == 1;
			boolean isCustom = c.getInt(c.getColumnIndex(DBConstants.IS_CUSTOM)) == 1;
			boolean isDownloaded = c.getInt(c.getColumnIndex(DBConstants.IS_DOWNLOADED)) == 1;
			int catIndex = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_INDEX));
			int categorySize = c.getInt(c.getColumnIndex(DBConstants.CATEGORY_SIZE));
			int totalStickers = c.getInt(c.getColumnIndex(DBConstants.TOTAL_NUMBER));
			String categoryDescription = c.getString(c.getColumnIndex(DBConstants.CATEGORY_DESCRIPTION));
			String stickerListJSONString = c.getString(c.getColumnIndex(DBConstants.STICKER_LIST));
			String similarPacksJSONString = c.getString(c.getColumnIndex(DBConstants.SIMILAR_CATEGORIES));
			String author = c.getString(c.getColumnIndex(DBConstants.AUTHOR));
			String copyRightString = c.getString(c.getColumnIndex(DBConstants.COPYRIGHT_STRING));
			boolean isDisabled = c.getInt(c.getColumnIndex(DBConstants.IS_DISABLED)) == 1 ;
			long updatedMetadataTs = c.getLong(c.getColumnIndex(DBConstants.UPDATED_METADATA_TIMESTAMP));
			long updatedPreviewTs = c.getLong(c.getColumnIndex(DBConstants.UPDATED_PREVIEW_TIMESTAMP));
			int ucid = c.getInt(c.getColumnIndex(DBConstants.UCID));
			stickerCategory = new StickerCategory.Builder()
					.setCategoryId(categoryId)
					.setCategoryName(categoryName)
					.setCategorySize(categorySize)
					.setCategoryDesc(categoryDescription)
					.setTotalStickers(totalStickers)
					.setUpdateAvailable(updateAvailable)
					.setIsVisible(isVisible)
					.setIsCustom(isCustom)
					.setIsAdded(true)
					.setIsPreview(false)
					.setIsDownloaded(isDownloaded)
					.setCatIndex(catIndex)
					.setAllStickerListString(stickerListJSONString)
					.setSimilarPacksString(similarPacksJSONString)
					.setAuthor(author)
					.setCopyRightString(copyRightString)
					.setUcid(ucid)
					.setIsDiabled(isDisabled)
					.setPackUpdationTime(updatedMetadataTs)
					.setPreviewUpdationTime(updatedPreviewTs)
					.build();
		}

		return stickerCategory;
	}


	public Cursor getMessage(String messageId)
	{
		String selection = DBConstants.MESSAGE_ID + "=?";
		return mDb.query(MESSAGES_TABLE, null, selection, new String[]{messageId}, null, null, null);
	}

	public void updateMetadataOfMessage(long messageId, String metadata)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(MESSAGE_ID, messageId);
		contentValues.put(MESSAGE_METADATA, metadata);
		mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { String.valueOf(messageId) });
	}

	public String getMetadataOfMessage(long messageId){
		String selection = DBConstants.MESSAGE_ID + "=?";
		Cursor c = mDb.query(MESSAGES_TABLE, new String[]{DBConstants.MESSAGE_METADATA}, selection, new String[]{String.valueOf(messageId)}, null, null, null);
		if(c.moveToFirst()){
			return c.getString(c.getColumnIndex(MESSAGE_METADATA));
		}
		return null;
	}
	
	public String getMetadataOfBot(String botMsisdn)
	{
		String selection = MSISDN + "=?";
		Cursor c = mDb.query(BOT_TABLE, new String[] { CONVERSATION_METADATA }, selection, new String[] { botMsisdn }, null, null, null);
		if (c.moveToFirst())
		{
			return c.getString(c.getColumnIndex(CONVERSATION_METADATA));
		}
		return null;
	}

	public void updateHelperDataForNonMessagingBot(String botMsisdn, String helperData)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(HIKE_CONTENT.HELPER_DATA, helperData);
		mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { botMsisdn });
	}

	/**
	 *
	 * @param msisdn
	 * @return unread count, -1 if conversation does not exist
	 */
	public int getConvUnreadCount(String msisdn){
		String selection = DBConstants.MSISDN + "=?";
		Cursor c = mDb.query(CONVERSATIONS_TABLE, new String[]{UNREAD_COUNT}, selection, new String[]{msisdn}, null, null, null);
		if(c.moveToFirst()){
			return c.getInt(c.getColumnIndex(DBConstants.UNREAD_COUNT));
		}
		return -1;
	}

	public String updateJSONMetadata(int messageId, String newMeta)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			try
			{
				JSONObject metadataJSON = new JSONObject(json);
				JSONObject newMetaData = new JSONObject(newMeta);
				Iterator<String> i = newMetaData.keys();
				while (i.hasNext())
				{
					String key = i.next();
					if (newMetaData.get(key) != null)
					{
						//Algo is that there might be JSONObject as the value and we might want to update some keys inside that JSONObject, so we are using the try
						//catch block for that. If JSONException is thrown, we will directly update the JSON, else will iterate again and then update.
						try
						{

							JSONObject newMetaJSON = new JSONObject(String.valueOf(newMetaData.get(key)));
							JSONObject metaJSON = new JSONObject(String.valueOf(metadataJSON.get(key)));
							Iterator<String> index = newMetaJSON.keys();
							while (index.hasNext())
							{
								String indexKey = index.next();
								metaJSON.put(indexKey, newMetaJSON.get(indexKey));
							}
							metadataJSON.put(key, metaJSON);
							Logger.d(mContext.getClass().getSimpleName(), "values are JSONObject, so updating the iterated JSON.");
						}
						catch (JSONException e)
						{
							Logger.d(mContext.getClass().getSimpleName(), "JSON exception thrown, so updating the original JSON directly.");
							metadataJSON.put(key, newMetaData.get(key));
						}

					}
				}
				json = metadataJSON.toString();
				updateMetadataOfMessage(messageId, json);
				return json;
			}
			catch (JSONException e)
			{
				Logger.e("HikeconversationDB", "JSOn Exception = " + e.getMessage());
			}
		}
		else
		{
			Logger.e("HikeconversationDB", "Meta data of message is null, id= " + messageId);
		}
		return null;
	}

	public void deleteAppAlarm(int messageId)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			{
				JSONObject metadataJSON = null;
				try
				{
					metadataJSON = new JSONObject(json);
					metadataJSON.remove(HikePlatformConstants.ALARM_DATA);
					updateMetadataOfMessage(messageId, metadataJSON.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		}
	}

	public void insertMicroAppALarm(int messageId,String alarmData)
	{
		String json = getMetadataOfMessage(messageId);
		if (json != null)
		{
			{

				try
				{
					JSONObject metadataJSON = new JSONObject(json);
					metadataJSON.put(HikePlatformConstants.ALARM_DATA, alarmData);
					updateMetadataOfMessage(messageId, metadataJSON.toString());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		}
	}

	public boolean upgradeForServerIdField()
	{
		boolean result = false;
		try
		{
			mDb.beginTransaction();

			long startTime = System.currentTimeMillis();

			String update1 = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			String update2 = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			String update3 = "UPDATE " + DBConstants.CONVERSATIONS_TABLE + " SET " + DBConstants.SORTING_TIMESTAMP + " = "  + DBConstants.LAST_MESSAGE_TIMESTAMP;
			String update4 = "UPDATE " + DBConstants.SHARED_MEDIA_TABLE + " SET " + DBConstants.SERVER_ID + " = "  + DBConstants.MESSAGE_ID;
			mDb.execSQL(update1);
			mDb.execSQL(update2);
			mDb.execSQL(update3);
			mDb.execSQL(update4);
			createIndexOverServerIdField(mDb);

			long endTime = System.currentTimeMillis();

			Logger.d("HikeConversationDatadase", " ServerId db upgrade time : "+(startTime-endTime));

			mDb.setTransactionSuccessful();
			result = true;
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();
			result = false;
		}
		finally
		{
			mDb.endTransaction();
		}

		return result;
	}

	public boolean updateSortingTimestamp(String msisdn, long timestamp)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.SORTING_TIMESTAMP, timestamp);
		int rowsUpdated = mDb.update(DBConstants.CONVERSATIONS_TABLE, contentValues, DBConstants.MSISDN + "=?", new String[] { msisdn });
		boolean updated = (rowsUpdated != 0);
		return (updated);
	}

	public boolean isContentMessageExist(String msisdn, String contentId, String nameSpace)

	{
		String where = DBConstants.MSISDN + "=? and " + DBConstants.HIKE_CONTENT.CONTENT_ID + "=? and " + HIKE_CONTENT.NAMESPACE + "=?";
		Cursor c = mDb.query(DBConstants.MESSAGES_TABLE, new String[]{DBConstants.MESSAGE_ID}, where, new String[]{msisdn, contentId, nameSpace}, null, null, null);
		try
		{
			boolean toReturn = c.moveToFirst();
			return toReturn;
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}


	public Map<String, ArrayList<Long>> getMsisdnMapForServerId(Long serverId, String excludingMsisdn)
	{
		ArrayList<Long> al = new ArrayList<Long>();
		al.add(serverId);
		Map<String, ArrayList<Long>> map = getMsisdnMapForServerIds(al, excludingMsisdn);

		return map;
	}
	
	/*
	 * return a map from msisdn to list of server ids. for all given msgIds entries in messages table.
	 */
	public Map<String, ArrayList<Long>> getMsisdnMapForServerIds(ArrayList<Long> serverIds, String userMsisdn)
	{
		Cursor c = null;
		Map<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

		if(serverIds == null || serverIds.isEmpty())
		{
			return map;
		}
		try
		{
			/*
			 * quering for all rows where serverIds are in set of given serverIds
			 * 1. msgid = serverid (normal msg case OR msg of the broadcast conversation in case of broadcast)
			 * 2. OR msisdn = givenMsisdn
			 */
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MSISDN, DBConstants.MESSAGE_ID, DBConstants.SERVER_ID },
					DBConstants.SERVER_ID + " IN " + Utils.valuesToCommaSepratedString(serverIds), null, null, null, null);
			final int msisdnColumnIdIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int messageIdColumnIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			final int serverIdColumnIdIdx = c.getColumnIndex(DBConstants.SERVER_ID);
			while (c.moveToNext())
			{
				String msisdnIdColumnValue = c.getString(msisdnColumnIdIdx);
				long messageIdColumnValue = c.getLong(messageIdColumnIdIdx);
				long serverIdColumnValue = c.getLong(serverIdColumnIdIdx);
				
				/*
				 * selecting only items from result set in which either
				 * 1. msgid = serverid (normal msg case OR msg of the broadcast conversation in case of broadcast)
				 * 2. OR msisdn = givenMsisdn
				 */
				if(messageIdColumnValue == serverIdColumnValue || msisdnIdColumnValue.equals(userMsisdn) )
				{
					ArrayList<Long> msgIdArray = map.get(msisdnIdColumnValue);
					if (msgIdArray == null)
					{
						msgIdArray = new ArrayList<Long>();
						map.put(msisdnIdColumnValue, msgIdArray);
					}
					msgIdArray.add(messageIdColumnValue);
				}
			}
		}
		finally
		{
			if (null != c)
			{
				c.close();
			}

		}
		return map;
	}

	/**
	 * Utility method to Update the config data in BotTable
	 * 
	 * @param msisdn
	 * @param configData
	 */
	public void updateConfigData(String msisdn, String configData)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(CONFIG_DATA, configData);
		mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { msisdn });
	}

	public void toggleMuteBot(String botMsisdn, boolean newMuteState)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(IS_MUTE, newMuteState ? 1 : 0);
		mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { botMsisdn });
	}
	
	public void updateBotConfiguration(String botMsisdn, int configuration)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(BOT_CONFIGURATION, configuration);
		mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[]{botMsisdn});
	}

    /**
     * Utility method to Update the config data in BotTable
     *
     * @param msisdn
     * @param metadata
     */
    public void updateBotMetaData(String botMsisdn, String metadata)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONVERSATION_METADATA, metadata);
        mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[]{botMsisdn});
    }

	/**
	 * Calling this function will update the notif data. The notif data is a JSON Object to enable the app to have
	 * multiple entries. It is having key as the timestamp and value as the notifData given at certain time.
	 * @param botMsisdn: msisdn of non-messaging bot.
	 * @param notifData: the data to be added to the notifData.
	 */
	public void updateNotifDataForMicroApps(String botMsisdn, String notifData)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, new String[] { HIKE_CONTENT.NOTIF_DATA }, DBConstants.MSISDN + "=?" , new String[] { botMsisdn}, null, null, null);
			final int columnIndex = c.getColumnIndex(HIKE_CONTENT.NOTIF_DATA);
			JSONObject notifDataJSON;
			if (c.moveToFirst())
			{
				String notifDataExisting = c.getString(columnIndex);
				notifDataJSON = TextUtils.isEmpty(notifDataExisting) ? new JSONObject() : new JSONObject(notifDataExisting);
			}
			else
			{
				notifDataJSON = new JSONObject();
			}
			JSONObject notifJSON = null;
			try {
			 notifJSON = new JSONObject(notifData);
			}catch(JSONException je) {
				je.printStackTrace();
				notifJSON = new JSONObject();
			}
			
			
			notifDataJSON.put(String.valueOf(System.currentTimeMillis()), notifJSON);
			ContentValues contentValues = new ContentValues();
			contentValues.put(HIKE_CONTENT.NOTIF_DATA, notifDataJSON.toString());
			mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { botMsisdn });
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(botMsisdn);
			if (null != botInfo)
			{
				botInfo.setNotifData(notifDataJSON.toString());
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
			Logger.e(HikePlatformConstants.TAG, "JSON exception in updating Notif data");
		}
		finally
		{
			if (null != c)
			{
				c.close();
			}
		}

	}

	/**
	 * call this function to delete the entire notif data for the given microApp.
	 * @param botMsisdn: msisdn of the non-messaging bot.
	 */
	public void deleteAllNotifDataForMicroApp(String botMsisdn)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(HIKE_CONTENT.NOTIF_DATA, "");
		mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { botMsisdn });
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(botMsisdn);
		if (null != botInfo)
		{
			botInfo.setNotifData("");
		}

	}

	/**
	 * call this function to partially delete the notif data based on the key which notif data to be deleted.
	 * @param key: the key or timestamp for which the notif data is to be deleted.
	 * @param botMsisdn: the msisdn of the non messaging bot.
	 */
	public void deletePartialNotifData(String key, String botMsisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, new String[] { HIKE_CONTENT.NOTIF_DATA }, DBConstants.MSISDN + "=?" , new String[] { botMsisdn}, null, null, null);
			final int columnIndex = c.getColumnIndex(HIKE_CONTENT.NOTIF_DATA);
			if (c.moveToFirst())
			{
				String notifDataExisting = c.getString(columnIndex);
				if (TextUtils.isEmpty(notifDataExisting))
				{
					Logger.e(HikePlatformConstants.TAG, "Existing Notif data is empty or null" + notifDataExisting);
					return;
				}

				JSONObject notifDataJSON = new JSONObject(notifDataExisting);
				if (!notifDataJSON.has(key))
				{
					Logger.e(HikePlatformConstants.TAG, "The key for deleting notif data does not exist" + key);
					return;
				}

				notifDataJSON.remove(key);
				ContentValues contentValues = new ContentValues();
				contentValues.put(HIKE_CONTENT.NOTIF_DATA, notifDataJSON.toString());
				mDb.update(BOT_TABLE, contentValues, MSISDN + "=?", new String[] { botMsisdn });
				BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(botMsisdn);
				if (null != botInfo)
				{
					botInfo.setNotifData(notifDataJSON.toString());
				}

			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
			Logger.e(HikePlatformConstants.TAG, "JSON exception in updating Notif data");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally
		{
			if (null != c)
			{
				c.close();
			}
		}
	}

	/**
	 * This method is a workaround to insert a conversation for non messaging bot. 
	 * It will also insert lastMsgText 
	 * This also
	 * @param msisdn
	 * @param lastMsgText
	 */
	public void addNonMessagingBotconversation(BotInfo botInfo)
	{
		boolean isChatExist=isConversationExist(botInfo.getMsisdn());
		ConvMessage convMessage = Utils.makeConvMessage(botInfo.getMsisdn(), botInfo.getLastMessageText(), true, State.RECEIVED_UNREAD);

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSISDN, botInfo.getMsisdn());
		contentValues.put(DBConstants.CONTACT_ID, botInfo.getMsisdn());
		contentValues.put(DBConstants.ONHIKE, 1);
		contentValues.put(DBConstants.MESSAGE, convMessage.getMessage());
		contentValues.put(DBConstants.MSG_STATUS, convMessage.getState().ordinal());
		contentValues.put(DBConstants.LAST_MESSAGE_TIMESTAMP, convMessage.getTimestamp());
		contentValues.put(DBConstants.SORTING_TIMESTAMP, convMessage.getTimestamp());
		contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
		contentValues.put(DBConstants.IS_STEALTH, StealthModeManager.getInstance().isStealthMsisdn(botInfo.getMsisdn()));
		contentValues.put(DBConstants.UNREAD_COUNT, 1); // inOrder to show 1+ on conv screen, we need to have some unread counter

		/**
		 * InsertWithOnConflict returns -1 on error while inserting/replacing a new row
		 */
		if (mDb.insertWithOnConflict(DBConstants.CONVERSATIONS_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE) != -1)
		{
			botInfo.setLastConversationMsg(convMessage);
			botInfo.setUnreadCount(1);  // inOrder to show 1+ on conv screen, we need to have some unread counter
			botInfo.setConvPresent(true); //In Order to indicate the presence of bot in the conv table
			botInfo.setStealth(StealthModeManager.getInstance().isStealthMsisdn(botInfo.getMsisdn()));
			//If the chat thread already exists and we need only to change the convInfo,we would not want the listeners on new chat created to be fired,like badge counter.
			if (isChatExist)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.LASTMSG_UPDATED,botInfo.getLastConversationMsg());
			}
			else
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, botInfo);
			}
		}

	}

	/**
	 * Utility method for updating the last message text for non messaging bot
	 * 
	 * @param msisdn
	 * @param lastMsgText
	 */
	public void updateLastMessageForNonMessagingBot(String msisdn, String lastMsgText)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, lastMsgText, true, State.RECEIVED_UNREAD);
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE, convMessage.getMessage());
		contentValues.put(DBConstants.MSG_STATUS, convMessage.getState().ordinal());
		contentValues.put(DBConstants.LAST_MESSAGE_TIMESTAMP, convMessage.getTimestamp());

		mDb.updateWithOnConflict(DBConstants.CONVERSATIONS_TABLE, contentValues, MSISDN + "=?", new String[]{msisdn}, SQLiteDatabase.CONFLICT_REPLACE);
	}
	
	/**
	 * Utility method to update the last message state for a conversation with a given msisdn
	 * 
	 * @param msisdn
	 * @param newState
	 */
	public void updateLastMessageStateAndCount(String msisdn, int newState)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSG_STATUS, newState);
		//Reset the unread count
		values.put(DBConstants.UNREAD_COUNT, 0);	
		mDb.updateWithOnConflict(DBConstants.CONVERSATIONS_TABLE, values, MSISDN + "=?", new String[]{msisdn}, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public BotInfo getBotInfoForMsisdn(String msisdn)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, null, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);

			int nameIdx = c.getColumnIndex(DBConstants.NAME);
			int configurationIdx = c.getColumnIndex(DBConstants.BOT_CONFIGURATION);
			int botTypeIdx = c.getColumnIndex(DBConstants.BOT_TYPE);
			int metadataIdx = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
			int muteIdx = c.getColumnIndex(DBConstants.IS_MUTE);
			int namespaceIdx = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
			int configDataidx = c.getColumnIndex(DBConstants.CONFIG_DATA);
			int notifDataIdx = c.getColumnIndex(HIKE_CONTENT.NOTIF_DATA);
			int helperDataIdx = c.getColumnIndex(HIKE_CONTENT.HELPER_DATA);
			int versionIdx = c.getColumnIndex(HIKE_CONTENT.BOT_VERSION);
			int triggerIdx = c.getColumnIndex(HIKE_CONTENT.BOT_TRIGGER_POINT);
			int clientIdIdx = c.getColumnIndex(HIKE_CONTENT.CLIENT_ID);
			int clientHashIdx = c.getColumnIndex(HIKE_CONTENT.CLIENT_HASH);

			if (c.moveToFirst())
			{
				String name = c.getString(nameIdx);
				int config = c.getInt(configurationIdx);
				int botType = c.getInt(botTypeIdx);
				String metadata = c.getString(metadataIdx);
				int mute = c.getInt(muteIdx);
				String namespace = c.getString(namespaceIdx);
				String configData = c.getString(configDataidx);
				String notifData = c.getString(notifDataIdx);
				String helperData = c.getString(helperDataIdx);
				int version = c.getInt(versionIdx);
				int trigger = c.getInt(triggerIdx);
				String clientId = c.getString(clientIdIdx);
				String clientHash = c.getString(clientHashIdx);
				BotInfo botInfo = new BotInfo.HikeBotBuilder(msisdn).setConvName(name).setConfig(config).setType(botType).setMetadata(metadata).setIsMute(mute == 1)
						.setNamespace(namespace).setConfigData(configData).setHelperData(helperData).setNotifData(notifData).setVersion(version).setTriggerPoint(trigger).setClientid(clientId).setClientHash(clientHash).build();
               
				
				botInfo.setBlocked(ContactManager.getInstance().isBlocked(msisdn));
				return botInfo;
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return null;
	}


	public BotInfo getBotInfoFromCursor(Cursor c, String msisdn)
	{

		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int configurationIdx = c.getColumnIndex(DBConstants.BOT_CONFIGURATION);
		int botTypeIdx = c.getColumnIndex(DBConstants.BOT_TYPE);
		int metadataIdx = c.getColumnIndex(DBConstants.CONVERSATION_METADATA);
		int muteIdx = c.getColumnIndex(DBConstants.IS_MUTE);
		int namespaceIdx = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
		int configDataidx = c.getColumnIndex(DBConstants.CONFIG_DATA);
		int notifDataIdx = c.getColumnIndex(HIKE_CONTENT.NOTIF_DATA);
		int helperDataIdx = c.getColumnIndex(HIKE_CONTENT.HELPER_DATA);
		int versionIdx = c.getColumnIndex(HIKE_CONTENT.BOT_VERSION);
		int triggerIdx = c.getColumnIndex(HIKE_CONTENT.BOT_TRIGGER_POINT);
		int clientIdIdx = c.getColumnIndex(HIKE_CONTENT.CLIENT_ID);
		int clientHashIdx = c.getColumnIndex(HIKE_CONTENT.CLIENT_HASH);

		String name = c.getString(nameIdx);
		int config = c.getInt(configurationIdx);
		int botType = c.getInt(botTypeIdx);
		String metadata = c.getString(metadataIdx);
		int mute = c.getInt(muteIdx);
		String namespace = c.getString(namespaceIdx);
		String configData = c.getString(configDataidx);
		String notifData = c.getString(notifDataIdx);
		String helperData = c.getString(helperDataIdx);
		int version = c.getInt(versionIdx);
		int trigger = c.getInt(triggerIdx);
		String clientId = c.getString(clientIdIdx);
		String clientHash = c.getString(clientHashIdx);
	   int mAppVersionCode = 0;
        // Keeping default bot type as web micro apps
        byte nmBotType = HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS;

		// Get mAppVersionCode from the metadata to store in Bot Info Object
		if (!TextUtils.isEmpty(metadata))
		{
			try
			{
				JSONObject mdJsonObject = new JSONObject(metadata);

				if (mdJsonObject != null)
				{
                    String nonMessagingBotType = mdJsonObject.optString(HikePlatformConstants.NON_MESSAGING_BOT_TYPE);

                    if(!TextUtils.isEmpty(nonMessagingBotType) && nonMessagingBotType.equals(HikePlatformConstants.NATIVE_MODE))
                        nmBotType = HikePlatformConstants.PlatformBotType.NATIVE_APPS;

                    JSONObject cardObjectJson = mdJsonObject.optJSONObject(HikePlatformConstants.CARD_OBJECT);
					if (cardObjectJson != null)
						mAppVersionCode = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
        
		BotInfo botInfo = new BotInfo.HikeBotBuilder(msisdn).setConvName(name).setConfig(config).setType(botType).setMetadata(metadata).setIsMute(mute == 1)
				.setNamespace(namespace).setConfigData(configData).setHelperData(helperData).setNotifData(notifData).setVersion(version).setMAppVersionCode(mAppVersionCode).setBotType(nmBotType).setTriggerPoint(trigger).setClientid(clientId).setClientHash(clientHash).build();

		botInfo.setBlocked(ContactManager.getInstance().isBlocked(msisdn));
		return botInfo;
	}

	public String getMessageEventsForMicroapps(String nameSpace, boolean includeNormalEvent)
	{
		Cursor c = null;
		try
		{
			if (!includeNormalEvent)
			{
				c = mDb.query(MESSAGE_EVENT_TABLE, new String[] { EVENT_ID, MESSAGE_HASH, EVENT_METADATA, MSISDN, EVENT_STATUS, EVENT_PARENT_MSISDN, EVENT_FROM_USER_MSISDN, DBConstants.TIMESTAMP},
						HIKE_CONTENT.NAMESPACE + "=? AND " + DBConstants.EVENT_TYPE + "=?", new String[] { nameSpace,
								String.valueOf(HikePlatformConstants.EventType.SHARED_EVENT) }, null, null, DBConstants.TIMESTAMP + " DESC");
			}
			else
			{
				c = mDb.query(MESSAGE_EVENT_TABLE, new String[] { EVENT_ID, MESSAGE_HASH, EVENT_METADATA, MSISDN, EVENT_STATUS, EVENT_PARENT_MSISDN, EVENT_FROM_USER_MSISDN, EVENT_TYPE, DBConstants.TIMESTAMP },
						HIKE_CONTENT.NAMESPACE + "=?", new String[] { nameSpace }, null, null, DBConstants.TIMESTAMP + " DESC");
			}

			if (c.getCount() <=0)
			{
				return "{}";
			}

			ArrayList<JSONObject> dataList = new ArrayList<>();
			int eventIdx = c.getColumnIndex(DBConstants.EVENT_ID);
			int messageHashIdx = c.getColumnIndex(DBConstants.MESSAGE_HASH);
			int eventMetadataIdx = c.getColumnIndex(DBConstants.EVENT_METADATA);
			int msisdnIndex = c.getColumnIndex(MSISDN);
			int eventStatusIdx = c.getColumnIndex(EVENT_STATUS);
			int parentMsisdnIdx = c.getColumnIndex(EVENT_PARENT_MSISDN);
			int fromUserIdx = c.getColumnIndex(EVENT_FROM_USER_MSISDN);
			int timestampIdx = c.getColumnIndex(DBConstants.TIMESTAMP);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIndex);
				JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(msisdn);
				jsonObject.put(HikePlatformConstants.EVENT_DATA, c.getString(eventMetadataIdx));
				jsonObject.put(HikePlatformConstants.MESSAGE_HASH, c.getString(messageHashIdx));
				jsonObject.put(HikePlatformConstants.EVENT_ID , c.getString(eventIdx));
				jsonObject.put(HikePlatformConstants.EVENT_STATUS, c.getInt(eventStatusIdx));
				jsonObject.put(EVENT_PARENT_MSISDN, c.getString(parentMsisdnIdx));
				jsonObject.put(EVENT_FROM_USER_MSISDN, c.getString(fromUserIdx));
				jsonObject.put(HikeConstants.TIMESTAMP, c.getInt(timestampIdx));
				if (includeNormalEvent)
				{
					jsonObject.put(HikePlatformConstants.EVENT_TYPE, c.getInt(c.getColumnIndex(EVENT_TYPE)));
				}
				dataList.add(jsonObject);
			}
			return dataList.toString();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return "{}";
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public String getEventsForMessageHash(String messageHash, String namespace)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(MESSAGE_EVENT_TABLE, new String[] { EVENT_ID, EVENT_METADATA, MSISDN, EVENT_STATUS, EVENT_TYPE, EVENT_PARENT_MSISDN, EVENT_FROM_USER_MSISDN, DBConstants.TIMESTAMP },
					MESSAGE_HASH + "=? AND " + HIKE_CONTENT.NAMESPACE + "=?", new String[] { messageHash, namespace }, null, null, DBConstants.TIMESTAMP + " DESC");
			if (c.getCount() <= 0)
			{
				return "{}";
			}

			ArrayList<JSONObject> dataList = new ArrayList<>();
			int eventIdx = c.getColumnIndex(DBConstants.EVENT_ID);
			int eventMetadataIdx = c.getColumnIndex(DBConstants.EVENT_METADATA);
			int msisdnIndex = c.getColumnIndex(MSISDN);
			int eventStatusIdx = c.getColumnIndex(EVENT_STATUS);
			int parentMsisdnIdx = c.getColumnIndex(EVENT_PARENT_MSISDN);
			int fromUserIdx = c.getColumnIndex(EVENT_FROM_USER_MSISDN);
			int timestampIdx = c.getColumnIndex(DBConstants.TIMESTAMP);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIndex);
				JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(msisdn);
				jsonObject.put(HikePlatformConstants.EVENT_DATA, c.getString(eventMetadataIdx));
				jsonObject.put(HikePlatformConstants.EVENT_ID, c.getString(eventIdx));
				jsonObject.put(HikePlatformConstants.EVENT_STATUS, c.getInt(eventStatusIdx));
				jsonObject.put(EVENT_PARENT_MSISDN, c.getString(parentMsisdnIdx));
				jsonObject.put(EVENT_FROM_USER_MSISDN, c.getString(fromUserIdx));
				jsonObject.put(HikeConstants.TIMESTAMP, c.getInt(timestampIdx));
				jsonObject.put(HikePlatformConstants.EVENT_TYPE, c.getInt(c.getColumnIndex(EVENT_TYPE)));
				dataList.add(jsonObject);
			}
			return dataList.toString();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return "{}";
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public String getEventsForMessageHashFromUser(String messageHash, String namespace, String fromUserId)
	{
		Cursor c = null;

		try
		{
			c = mDb.query(MESSAGE_EVENT_TABLE, new String[] { EVENT_ID, EVENT_METADATA, MSISDN, EVENT_STATUS, EVENT_TYPE, EVENT_PARENT_MSISDN, EVENT_FROM_USER_MSISDN, DBConstants.TIMESTAMP },
					MESSAGE_HASH + "=? AND " + HIKE_CONTENT.NAMESPACE + "=? AND " + EVENT_FROM_USER_MSISDN + "=?", new String[] { messageHash, namespace, fromUserId }, null, null, DBConstants.TIMESTAMP + " DESC");
			if (c.getCount() <= 0)
			{
				return "{}";
			}

			ArrayList<JSONObject> dataList = new ArrayList<>();
			int eventIdx = c.getColumnIndex(DBConstants.EVENT_ID);
			int eventMetadataIdx = c.getColumnIndex(DBConstants.EVENT_METADATA);
			int msisdnIndex = c.getColumnIndex(MSISDN);
			int eventStatusIdx = c.getColumnIndex(EVENT_STATUS);
			int parentMsisdnIdx = c.getColumnIndex(EVENT_PARENT_MSISDN);
			int fromUserIdx = c.getColumnIndex(EVENT_FROM_USER_MSISDN);
			int timestampIdx = c.getColumnIndex(DBConstants.TIMESTAMP);

			while (c.moveToNext())
			{
				String msisdn = c.getString(msisdnIndex);
				JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(msisdn);
				jsonObject.put(HikePlatformConstants.EVENT_DATA, c.getString(eventMetadataIdx));
				jsonObject.put(HikePlatformConstants.EVENT_ID, c.getString(eventIdx));
				jsonObject.put(HikePlatformConstants.EVENT_STATUS, c.getInt(eventStatusIdx));
				jsonObject.put(EVENT_PARENT_MSISDN, c.getString(parentMsisdnIdx));
				jsonObject.put(EVENT_FROM_USER_MSISDN, c.getString(fromUserIdx));
				jsonObject.put(HikeConstants.TIMESTAMP, c.getInt(timestampIdx));
				jsonObject.put(HikePlatformConstants.EVENT_TYPE, c.getInt(c.getColumnIndex(EVENT_TYPE)));
				dataList.add(jsonObject);
			}
			return dataList.toString();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return "{}";
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public void deleteEvent(String eventId)
	{
		mDb.delete(DBConstants.MESSAGE_EVENT_TABLE, DBConstants.EVENT_ID + "=?", new String[]{eventId});
	}

	public void deleteAllEventsForMessage(String messageHash)
	{
		mDb.delete(DBConstants.MESSAGE_EVENT_TABLE, DBConstants.MESSAGE_HASH + "=?", new String[]{messageHash});
	}

	public void deleteAllEventsForNamespace(String namespace)
	{
		mDb.delete(DBConstants.MESSAGE_EVENT_TABLE, HIKE_CONTENT.NAMESPACE + "=?", new String[]{namespace});
	}

	public long insertMessageEvent(MessageEvent messageEvent)
	{
		ContentValues values = new ContentValues();
		values.put(DBConstants.MESSAGE_HASH, messageEvent.getMessageHash());
		values.put(DBConstants.EVENT_METADATA, messageEvent.getEventMetadata());
		values.put(DBConstants.EVENT_STATUS, messageEvent.getEventStatus());
		values.put(DBConstants.EVENT_TYPE, messageEvent.getEventType());
		values.put(DBConstants.TIMESTAMP, messageEvent.getSentTimeStamp());
		values.put(DBConstants.MAPPED_EVENT_ID, messageEvent.getMappedEventId());
		values.put(DBConstants.MSISDN, messageEvent.getMsisdn());
		values.put(DBConstants.EVENT_PARENT_MSISDN, messageEvent.getParent_msisdn());
		values.put(DBConstants.EVENT_FROM_USER_MSISDN, messageEvent.getFromUserMsisdn());
		values.put(HIKE_CONTENT.NAMESPACE, messageEvent.getNameSpace());
		String eventHash = messageEvent.createEventHash();
		if (!TextUtils.isEmpty(eventHash))
		{
			values.put(DBConstants.EVENT_HASH, eventHash);
		}
		return mDb.insert(DBConstants.MESSAGE_EVENT_TABLE, null, values);
	}

	/**
	 * This method is used for the authentication of the message event
	 * @param messageHash
	 * @param from
	 * @return
	 */
	public long getMessageIdFromMessageHash(String messageHash, String from)
	{
		Cursor c = null;

		if (TextUtils.isEmpty(from))
		{
			return -1;
		}

		try
		{

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_ID, DBConstants.MSISDN }, DBConstants.MESSAGE_HASH + " =?", new String[] { messageHash },
					null, null, null, null);

			int msgIdIdx = c.getColumnIndex(DBConstants.MESSAGE_ID);
			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			if (c.moveToFirst())
			{
				long msgId = c.getLong(msgIdIdx);
				String msisdn = c.getString(msisdnIdx);
				if (msisdn.equals(from))
				{
					return msgId;
				}
			}
			return -1;
		}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	/**
	 * This method is used for getting message hash from message id. Message Hash is a base for all Card to card messaging in Platform.
	 * @param messageId
	 * @return
	 */
	public String getMessageHashFromMessageId(long messageId)
	{
		Cursor c = null;

		try
		{

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MESSAGE_HASH }, DBConstants.MESSAGE_ID + " =?", new String[] { String.valueOf(messageId) },
					null, null, null, null);



			if (c.moveToFirst())
			{
				int msgHashIdx = c.getColumnIndex(DBConstants.MESSAGE_HASH);
				return c.getString(msgHashIdx);
			}
			return "";
		}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

	}

	public String getMsisdnFromMessageHash(String messageHash)
	{
		Cursor c = null;

		try
		{

			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { DBConstants.MSISDN }, DBConstants.MESSAGE_HASH + " =?", new String[] { messageHash },
					null, null, null, null);

			int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);

			if (c.moveToFirst())
			{
				return c.getString(msisdnIdx);
			}
			return null;
		}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}


	public int updateMessageOriginType(long msgID,int originTypeOrdinal)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MESSAGE_ORIGIN_TYPE , originTypeOrdinal);
		return mDb.update(DBConstants.MESSAGES_TABLE, contentValues, DBConstants.MESSAGE_ID + "=?", new String[] { Long.toString(msgID) });
	}
	
	/**
	 * Get actions (likes/comments/views) for corresponding UUIDs
	 * 
	 * @param objectType
	 *            {@link ActionsDataModel.ActivityObjectTypes}
	 * @param uuidList
	 * @param actionsData
	 */
	public void getActionsData(String objectType, List<String> uuidList, TimelineActions actionsData)
	{
		//Check input params
		if (TextUtils.isEmpty(objectType) || uuidList == null || uuidList.isEmpty() || actionsData == null)
		{
			throw new IllegalArgumentException(HikeConversationsDatabase.class.getSimpleName() + " getActionsData(): One or more input param is null/empty");
		}

		// Columns required
		String[] columns = new String[] { BaseColumns._ID, DBConstants.ACTION_ID, DBConstants.ACTION_COUNT, DBConstants.ACTORS, DBConstants.ACTION_OBJECT_ID };

		// Selection for UUIDs
		StringBuilder uuidSelection = new StringBuilder("(");
		
		try
		{
			for (String uuid : uuidList)
			{
				if (!TextUtils.isEmpty(uuid))
				{
					uuidSelection.append(DatabaseUtils.sqlEscapeString(uuid) + ",");
				}
			}
			uuidSelection.replace(uuidSelection.lastIndexOf(","), uuidSelection.length(), ")");
		}
		catch (Exception ex)
		{
			//No actions data populated. User will not see likes count
			ex.printStackTrace();
			return;
		}

		StringBuilder selection = new StringBuilder();

		selection.append(DBConstants.ACTION_OBJECT_ID + " IN " + uuidSelection.toString());

		//Add object type (su,card, channel)
		selection.append(" AND " + DBConstants.ACTION_OBJECT_TYPE + " = " +
				DatabaseUtils.sqlEscapeString(objectType));

		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.ACTIONS_TABLE, columns, selection.toString(), null, null, null, null);

			int cIdxActionId = c.getColumnIndexOrThrow(DBConstants.ACTION_ID);
			int cIdxActionCount = c.getColumnIndexOrThrow(DBConstants.ACTION_COUNT);
			int cIdxActors = c.getColumnIndexOrThrow(DBConstants.ACTORS);
			int cIdxObjId = c.getColumnIndexOrThrow(DBConstants.ACTION_OBJECT_ID);

			if (c.moveToFirst())
			{
				do
				{
					int count = c.getInt(cIdxActionCount);
					
					int actionIDKey = c.getInt(cIdxActionId);

					String objectId = c.getString(cIdxObjId);
					
					String msisdnJSONArray = c.getString(cIdxActors);
					
					List<ContactInfo> cInfoList = new ArrayList<ContactInfo>();
					
					if (!TextUtils.isEmpty(msisdnJSONArray))
					{
						JSONArray myList;
						try
						{
							myList = new JSONArray(msisdnJSONArray);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
							continue;
						}

						for (int i = 0; i < myList.length(); i++)
						{
							ContactInfo contactInfo;
							try
							{
								contactInfo = ContactManager.getInstance().getContact(myList.getString(i), true, true);

								if (contactInfo != null)
								{
									cInfoList.add(contactInfo);
								}
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
						}
					}
					
					actionsData.addActionDetails(objectId, cInfoList, ActionTypes.getType(actionIDKey), count,ActivityObjectTypes.getTypeFromString(objectType),false);
				}
				while (c.moveToNext());
			}
		}
		finally
		{
			c.close();
		}
	}

	public void updateActionsData(TimelineActions actionsData, ActivityObjectTypes activityType)
	{
		if (actionsData == null)
		{
			return;
		}

		final HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> actionsDataMap = actionsData.getTimelineActionsMap();

		if (actionsDataMap == null || actionsDataMap.isEmpty())
		{
			return;
		}

		Set<Pair<String, String>> uuidObjSet = actionsDataMap.keySet();

		try
		{
			mDb.beginTransaction();

			for (Pair<String, String> uuidObjType : uuidObjSet)
			{
				ArrayList<ActionsDataModel> actionsDataListForUUID = actionsDataMap.get(uuidObjType);
				for (ActionsDataModel actionDM : actionsDataListForUUID)
				{
					ContentValues cv = new ContentValues();
					cv.put(ACTION_OBJECT_TYPE, activityType.getTypeString());
					cv.put(ACTION_OBJECT_ID, uuidObjType.first);
					cv.put(ACTION_ID, actionDM.getType().getKey());
					cv.put(ACTION_COUNT, actionDM.getTotalCount());
					cv.put(ACTORS, actionDM.getContactsMsisdnJSON());
					mDb.insertWithOnConflict(ACTIONS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
					Logger.d(HikeConstants.TIMELINE_LOGS, " inserting Action Data "+ actionDM);
				}
			}

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

	}
	
	/**
	 * This method will be called only once to handle the upgrade case where the sorting ids will have to initialized to message ids for existing users. This method will be called
	 * from {@link UpgradeIntentService}
	 */
	public boolean upgradeForSortingIdField()
	{
		boolean result = false;
		try
		{
			long startTime = System.currentTimeMillis();

			String updateStatement = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET " + DBConstants.SORTING_ID + " = " + DBConstants.MESSAGE_ID;
			mDb.execSQL(updateStatement);
			long numRows = DatabaseUtils.longForQuery(mDb, "SELECT COUNT(*) FROM " + DBConstants.MESSAGES_TABLE, null);

			long timeTaken = System.currentTimeMillis() - startTime;
			analyticsForUpgradeSortId(numRows, timeTaken);
			Logger.d("HikeConversationsDatabase", " ServerId db upgrade time : " + timeTaken);

			result = true;
		}

		catch (Exception e)
		{
			Logger.e("HikeConversationsDatabase", "Got an exception while upgrading for sorting id field : ", e);
			result = false;
		}

		return result;
	}

	private void analyticsForUpgradeSortId(long numRows, long timeTaken) throws JSONException {
		JSONObject jObj = new JSONObject();
		jObj.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
		jObj.put(AnalyticsConstants.EVENT, "upgrade_sortId");
		
		jObj.put(AnalyticsConstants.LOG_FIELD_5, numRows); //Msg Count
		jObj.put(AnalyticsConstants.LOG_FIELD_6, timeTaken); //Time taken in msec
		
		HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.UPGRADE_EVENT, HAManager.EventPriority.HIGH, jObj, AnalyticsConstants.EVENT_TAG_MOB);
	}


	public ConvMessage updateMessageForGeneralEvent(String msgHash, ConvMessage.State state, String hm,Long mappedMessageID, boolean isGroup, String fromUserMsisdn)
	{
		ConvMessage msg = null;
		try
		{
			mDb.beginTransaction();
			String updateStatement;
			String mappedmsgIdUpdate;
			if(mappedMessageID!=null)
			{
				mappedmsgIdUpdate=","+DBConstants.MAPPED_MSG_ID + " = " + mappedMessageID;
			}
			else
			{
				mappedmsgIdUpdate="";
			}
			if(!isGroup)
			{
				updateStatement = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET " + DBConstants.SORTING_ID + " = "
						+ " ( ( " + "SELECT" + " MAX( " + DBConstants.SORTING_ID + " ) " + " FROM " + DBConstants.MESSAGES_TABLE + " )" + " + 1 ), "
						+ DBConstants.MSG_STATUS + " = " + state.ordinal() + ","
						+ DBConstants.TIMESTAMP + " = " + System.currentTimeMillis() / 1000 + ","
						+ DBConstants.MESSAGE + " = " + DatabaseUtils.sqlEscapeString(hm)
						+ mappedmsgIdUpdate
						+ " WHERE " + DBConstants.MESSAGE_HASH + " = " + DatabaseUtils.sqlEscapeString(msgHash);
			}
			else
			{
				updateStatement = "UPDATE " + DBConstants.MESSAGES_TABLE + " SET " + DBConstants.SORTING_ID + " = "
						+ " ( ( " + "SELECT" + " MAX( " + DBConstants.SORTING_ID + " ) " + " FROM " + DBConstants.MESSAGES_TABLE + " )" + " + 1 ), "
						+ DBConstants.MSG_STATUS + " = " + state.ordinal() + ","
						+ DBConstants.GROUP_PARTICIPANT + " = " + DatabaseUtils.sqlEscapeString(fromUserMsisdn) + ","
						+ DBConstants.TIMESTAMP + " = " + System.currentTimeMillis() / 1000 + ","
						+ DBConstants.MESSAGE + " = " + DatabaseUtils.sqlEscapeString(hm)
						+ mappedmsgIdUpdate
						+ " WHERE " + DBConstants.MESSAGE_HASH + " = " + DatabaseUtils.sqlEscapeString(msgHash);
			}
			mDb.execSQL(updateStatement);

			msg =getMessageFromMessageHash(msgHash);
			updateConvTable(msg, isGroup, fromUserMsisdn);
			mDb.setTransactionSuccessful();
			HikeMessengerApp.getPubSub().publish(HikePubSub.GENERAL_EVENT_STATE_CHANGE,msg);
		}

		catch (Exception e)
		{
			Logger.e("HikeConversationsDatabase", "Got an exception while updating sortingId for a Message");
			msg = null;
		}

		finally
		{
			mDb.endTransaction();
		}

		return msg;
	}

	public boolean isConversationExist(String msisdn)
	{
		Cursor c = null;
		try
		{

			c = mDb.query(DBConstants.CONVERSATIONS_TABLE, null, DBConstants.MSISDN + "=?", new String[] { msisdn }, null, null, null);
			if (c.moveToFirst())
			{
				return true;
			}
			return false;
		}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}
	
	private String getMsisdnAndSortingIdIndex()
	{
		return "CREATE INDEX IF NOT EXISTS " + DBConstants.SORT_ID_COMPOSITE_IDX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.MSISDN + " , " + DBConstants.SORTING_ID + " )";
	}


	private String getUCIDIndexOnStickerCategoryTable()
	{
		return DBConstants.CREATE_INDEX + DBConstants.UCID_INDEX + " ON " + DBConstants.STICKER_CATEGORIES_TABLE + " ( " + DBConstants.UCID + " )";
	}

	private String getUCIDIndexOnStickerCategoryRankTable()
	{
		return DBConstants.CREATE_INDEX + DBConstants.UCID_INDEX + " ON " + DBConstants.STICKER_CATEGORY_RANK_TABLE + " ( " + DBConstants.RANK + " )";
	}

	private String getSortingIndexQuery()
	{
		return "CREATE INDEX IF NOT EXISTS " + DBConstants.SORT_ID_SINGLE_IDX + " ON " + DBConstants.MESSAGES_TABLE + " ( " + DBConstants.SORTING_ID + " )";
	}

	/**
	 * Saves the conversations database version to preferences
	 */
	private void saveCurrentConvDbVersionToPrefs()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.CONV_DB_VERSION_PREF, DBConstants.CONVERSATIONS_DATABASE_VERSION);
	}

	/**
	 * This does only for the last message values for convmessage.I t return convmessage only with fields reqd for last message.
	 * @param msgHash
	 * @return
	 */
	public ConvMessage getMessageFromMessageHash(String msgHash)
	{
		Cursor c=null;
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, null, DBConstants.MESSAGE_HASH + " =?", new String[]{msgHash}, null, null, null, null);

			if (c.moveToFirst())
			{


				final int msisdnColumn = c.getColumnIndex(DBConstants.MSISDN);
				final int msgColumn = c.getColumnIndex(DBConstants.MESSAGE);
				final int msgStatusColumn = c.getColumnIndex(DBConstants.MSG_STATUS);
				final int tsColumn = c.getColumnIndex(DBConstants.TIMESTAMP);
				final int mappedMsgIdColumn = c.getColumnIndex(DBConstants.MAPPED_MSG_ID);
				final int msgIdColumn = c.getColumnIndex(DBConstants.MESSAGE_ID);
				final int groupParticipantColumn = c.getColumnIndex(DBConstants.GROUP_PARTICIPANT);
				final int typeColumn = c.getColumnIndex(DBConstants.MESSAGE_TYPE);
				final int contentIdColumn = c.getColumnIndex(DBConstants.HIKE_CONTENT.CONTENT_ID);
				final int nameSpaceColumn = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);
				final int sortId = c.getColumnIndex(DBConstants.SORTING_ID);
				final int metadataColumn = c.getColumnIndex(DBConstants.MESSAGE_METADATA);

				ConvMessage message = new ConvMessage(c.getString(msgColumn), c.getString(msisdnColumn), c.getInt(tsColumn), ConvMessage.stateValue(c.getInt(msgStatusColumn)),
						c.getLong(msgIdColumn), c.getLong(mappedMsgIdColumn), c.getString(groupParticipantColumn), true, c.getInt(typeColumn), c.getInt(contentIdColumn), c.getString(nameSpaceColumn), c.getLong(sortId));
				String metadata = c.getString(metadataColumn);
				try
				{
					if(message.getMessageType() == com.bsb.hike.HikeConstants.MESSAGE_TYPE.CONTENT){
						message.platformMessageMetadata = new PlatformMessageMetadata(metadata);
					}else if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
						message.webMetadata = new WebMetadata(metadata);
					}else{
						message.setMetadata(metadata);
					}
				}
				catch (JSONException e)
				{
					Logger.w(HikeConversationsDatabase.class.getName(), "Invalid JSON metadata", e);
				}
				
				return message;

			}
		}
		catch(Exception e)
		{
			Logger.d(HikePlatformConstants.TAG,"DB error");
		}

		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}

		return null;
	}

	public void updateConvTable(ConvMessage convMessage, boolean isGroup, String fromUserMsisdn)
	{
		ContentValues value=getContentValueForConversationMessage(convMessage, convMessage.getTimestamp());

			int rows_updated=mDb.update(DBConstants.CONVERSATIONS_TABLE, value, DBConstants.MSISDN + "=?", new String[]{convMessage.getMsisdn()});
		if(rows_updated<=0)
		{
			//We want to create a conversation when an event is replied to if conversation doesn't exist.
			String from = convMessage.getSenderMsisdn();
			ContactInfo contactInfo = OneToNConversationUtils.isOneToNConversation(from) ? new ContactInfo(from, from, from, from)
					: ContactManager.getInstance().getContact(from, false, true);
			Conversation conv = new OneToOneConversation.ConversationBuilder(from).setConvName((contactInfo != null) ? contactInfo.getName() : null).setIsOnHike(true).build();
			if (convMessage != null)
			{
				conv.updateLastConvMessage(convMessage);
			}
			ContentValues contentValues= new ContentValues();
			contentValues.put(DBConstants.MSISDN, convMessage.getMsisdn());
			contentValues.put(DBConstants.CONTACT_ID,contactInfo != null?contactInfo.getId():convMessage.getMsisdn());
			contentValues.put(DBConstants.ONHIKE, 1);
			contentValues.put(DBConstants.MESSAGE, convMessage.getMessage());
			contentValues.put(DBConstants.MSG_STATUS, convMessage.getState().ordinal());
			contentValues.put(DBConstants.LAST_MESSAGE_TIMESTAMP, convMessage.getTimestamp());
			contentValues.put(DBConstants.SORTING_TIMESTAMP, convMessage.getTimestamp());
			contentValues.put(DBConstants.MESSAGE_ID, convMessage.getMsgID());
			if(isGroup)
			{
				contentValues.put(DBConstants.GROUP_PARTICIPANT, fromUserMsisdn);
			}

			/**
			 * InsertWithOnConflict returns -1 on error while inserting/replacing a new row
			 */
			mDb.insertWithOnConflict(DBConstants.CONVERSATIONS_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_CONVERSATION, conv.getConvInfo());
		}

	}

	/**
	 * Returns the maximum sorting id column value index from Messages Table
	 * 
	 * @return
	 */
	public int getMaxSortingIdFromMessages()
	{
		Cursor c = null;

		long time = System.currentTimeMillis();
		try
		{
			c = mDb.query(DBConstants.MESSAGES_TABLE, new String[] { "MAX(" + DBConstants.SORTING_ID + ")" + "AS " + DBConstants.SORTING_ID }, null, null, null, null, null, null);

			if (c.moveToFirst())
			{
				return c.getInt(c.getColumnIndex(DBConstants.SORTING_ID));
			}
			else
				return -1;
		}

		catch (Exception e)
		{
			return -1;
		}

		finally
		{
			if (c != null)
				c.close();

			Logger.d("HikeConversationsDatabase", "Time taken to get max sort Id : " + (System.currentTimeMillis() - time));
		}
	}

	/**
	 * Utility method to get Message Hash present in Message Event Table from event Id
	 * 
	 * @param eventId
	 * @return
	 */
	public long getMessageIdFromEventId(long eventId, String fromMsisdn)
	{
		Cursor c = null;

		try
		{

			c = mDb.query(DBConstants.MESSAGE_EVENT_TABLE, new String[] { DBConstants.MESSAGE_HASH }, DBConstants.EVENT_ID + " =?", new String[] { Long.toString(eventId) }, null,
					null, null, null);

			int msgHashIdx = c.getColumnIndex(DBConstants.MESSAGE_HASH);

			if (c.moveToFirst())
			{
				String msgHash = c.getString(msgHashIdx);

				return getMessageIdFromMessageHash(msgHash, fromMsisdn);
			}
		}

		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return -1;
	}
	//Sql statement to maintain Infra URL table

	private String getURLTableCreateStatement()
	{
		return CREATE_TABLE + DBConstants.URL_TABLE
				+ " ("
				+ DBConstants.URL_KEY + " TEXT PRIMARY KEY , "
				+ DBConstants.URL + " TEXT , "        //URL THAT IS RELATED TO THIS KEY.
				+ DBConstants.LIFE + " INTEGER"       //This column specifies if the particular service access token is short lived(0) or long lived(1).
				+  ")";
	}
	// Function to insert URL in URL table in an encrypted manner

	public long insertURL(String urlKey, String url, int life)
	{
		try
		{
			java.net.URL u = new java.net.URL(url);
		}
		catch (MalformedURLException e)
		{
			return -1;
		}
		try
		{

			ContentValues values = new ContentValues();
			values.put(DBConstants.URL_KEY, urlKey);
			values.put(DBConstants.URL, Utils.encrypt(url));
			values.put(DBConstants.LIFE, life);
			return mDb.insertWithOnConflict(DBConstants.URL_TABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
		}
		catch (Exception e)
		{
			Logger.e("HikeConversationsDatabase", "Error in Inserting URL " + e.toString());
			return -1;
		}
	}

	// Function to get URL and returns decrypted URL
	public Cursor getURL(String urlKey)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.URL_TABLE, new String[] { DBConstants.URL, DBConstants.LIFE }, DBConstants.URL_KEY + "=?", new String[] { urlKey }, null, null, null);

			if (!c.moveToFirst())
			{
				return null;
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in getURL", e);
		}

		return c;


	}

	public void deleteURL(String urlKey)
	{
		mDb.delete(DBConstants.URL_TABLE, DBConstants.URL_KEY + "=?", new String[]{urlKey});
	}


	public void markAllCategoriesAsDownloaded()
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.IS_DOWNLOADED, true);
		mDb.update(DBConstants.STICKER_CATEGORIES_TABLE, contentValues, null, null);
	}

	public boolean upgradeForStickerTable()
	{
		boolean result;
		try
		{
			moveStickerInfoToStickerTable();
			result = true;
		}
		catch(Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception in upgradeForStickerTable : ", e);
			result = false;
		}
		return result;
	}

	private void moveStickerInfoToStickerTable()
	{
		List<Sticker> stickerSet = StickerManager.getInstance().getAllStickers();
		insertStickersToDB(stickerSet, StickerConstants.StickerType.LARGE);
	}

    public List<Sticker> parseStickerTableCursor(Cursor c) {
        if (c == null) {
            return null;
        }

        List<Sticker> resultSet = null;

        try {

            resultSet = new ArrayList<Sticker>(c.getCount());

            int largestickerpathIdx = c.getColumnIndex(DBConstants.LARGE_STICKER_PATH);
            int smallstickerpathIdx = c.getColumnIndex(DBConstants.SMALL_STICKER_PATH);
            int stickerWidthIdx = c.getColumnIndex(DBConstants.WIDTH);
            int stickerHeightIdx = c.getColumnIndex(DBConstants.HEIGHT);
            int stickerIdIdx = c.getColumnIndex(DBConstants.STICKER_ID);
            int categoryIdIdx = c.getColumnIndex(DBConstants.CATEGORY_ID);

            while (c.moveToNext()) {
                String categoryId = c.getString(categoryIdIdx);
                String stickerId = c.getString(stickerIdIdx);

                Sticker sticker = new Sticker(categoryId, stickerId);

                sticker.setSmallStickerPath(c.getString(smallstickerpathIdx));
                sticker.setLargeStickerPath(c.getString(largestickerpathIdx));
                sticker.setWidth(c.getInt(stickerWidthIdx));
                sticker.setHeight(c.getInt(stickerHeightIdx));

                resultSet.add(sticker);
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }

        return resultSet;
    }

    public Sticker getStickerFromStickerTable(Sticker sticker) {
        if (sticker == null || TextUtils.isEmpty(sticker.getStickerId()) || TextUtils.isEmpty(sticker.getCategoryId())) {
            return null;
        }

        Cursor c = null;

        try {
            c = mDb.query(DBConstants.STICKER_TABLE, null, DBConstants.STICKER_ID + " =?" + " AND " + DBConstants.CATEGORY_ID + "=?", new String[]{sticker.getStickerId(), sticker.getCategoryId()}, null,
                    null, null, null);

            int largestickerpathIdx = c.getColumnIndex(DBConstants.LARGE_STICKER_PATH);
            int smallstickerpathIdx = c.getColumnIndex(DBConstants.SMALL_STICKER_PATH);
            int stickerWidthIdx = c.getColumnIndex(DBConstants.WIDTH);
            int stickerHeightIdx = c.getColumnIndex(DBConstants.HEIGHT);

            if (c.moveToFirst()) {
                sticker.setSmallStickerPath(c.getString(smallstickerpathIdx));
                sticker.setLargeStickerPath(c.getString(largestickerpathIdx));
                sticker.setWidth(c.getInt(stickerWidthIdx));
                sticker.setHeight(c.getInt(stickerHeightIdx));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return sticker;
    }


    public List<Sticker> getStickersForCatgeoryId(String categoryId, StickerConstants.StickerType stickerType)
	{
		Cursor c = null;
		List<Sticker> stickerList;
		c = mDb.query(DBConstants.STICKER_TABLE, null, DBConstants.CATEGORY_ID + "=?" + " AND " + DBConstants.IS_ACTIVE + "=?" + " AND " + DBConstants.TYPE + "=?", new String[] {
				categoryId, Integer.toString(DBConstants.DEFAULT_ACTIVE_STATE), Integer.toString(stickerType.ordinal()) }, null, null, null, null);

		return parseStickerTableCursor(c);
	}

	public void insertStickersToDB(List<Sticker> stickers, StickerConstants.StickerType stickerType)
	{
		try
		{
			mDb.beginTransaction();

			ContentValues contentValues = new ContentValues();
			for (Sticker sticker : stickers)
			{
				contentValues.clear();
				contentValues.put(DBConstants.STICKER_ID, sticker.getStickerId());
				contentValues.put(DBConstants.CATEGORY_ID, sticker.getCategoryId());

				if(!TextUtils.isEmpty(sticker.getLargeStickerPath(true)))
				{
					contentValues.put(DBConstants.LARGE_STICKER_PATH, sticker.getLargeStickerPath());
					contentValues.put(DBConstants.SMALL_STICKER_PATH, sticker.getSmallStickerPath());
				}

				contentValues.put(DBConstants.WIDTH, sticker.getWidth());
				contentValues.put(DBConstants.HEIGHT, sticker.getHeight());
				contentValues.put(DBConstants.TYPE, stickerType.ordinal());

				long rowId = mDb.insertWithOnConflict(DBConstants.STICKER_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

	}

	public void deactivateStickerFromDB(List<Sticker> stickers)
	{
        if (Utils.isEmpty(stickers))
        {
            return;
        }

		try
		{
			mDb.beginTransaction();

			ContentValues contentValues = new ContentValues();
			for (Sticker sticker : stickers)
			{
				contentValues.clear();
				contentValues.put(DBConstants.IS_ACTIVE, DBConstants.DEFAULT_INACTIVE_STATE);
				mDb.update(DBConstants.STICKER_TABLE, contentValues, DBConstants.CATEGORY_ID + "=?" + " AND " + DBConstants.STICKER_ID + "=?",
					new String[]{sticker.getCategoryId(), sticker.getStickerId()});
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void activateStickerFromDB(List<Sticker> stickers)
	{
        if (Utils.isEmpty(stickers))
        {
            return;
        }

		try
		{
			mDb.beginTransaction();

			ContentValues contentValues = new ContentValues();
			for (Sticker sticker : stickers)
			{
				contentValues.clear();
				contentValues.put(DBConstants.IS_ACTIVE, DBConstants.DEFAULT_ACTIVE_STATE);
				mDb.update(DBConstants.STICKER_TABLE, contentValues, DBConstants.CATEGORY_ID + "=?" + " AND " + DBConstants.STICKER_ID + "=?",
						new String[] { sticker.getCategoryId(), sticker.getStickerId() });
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void deactivateStickersForCategories(List<StickerCategory> categories)
	{
        if (Utils.isEmpty(categories))
        {
            return;
        }

		try
		{
			mDb.beginTransaction();

			ContentValues contentValues = new ContentValues();
			for (StickerCategory category : categories)
			{
				contentValues.clear();
				contentValues.put(DBConstants.IS_ACTIVE, DBConstants.DEFAULT_INACTIVE_STATE);
				mDb.update(DBConstants.STICKER_TABLE, contentValues, DBConstants.CATEGORY_ID + "=?", new String[]{category.getCategoryId()});
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	public void deleteStickersForCategories(List<StickerCategory> categories)
	{
        if(Utils.isEmpty(categories))
        {
            return;
        }

		try
		{
			mDb.beginTransaction();

			ContentValues contentValues = new ContentValues();
			for (StickerCategory category : categories)
			{
				mDb.delete(DBConstants.STICKER_TABLE, DBConstants.CATEGORY_ID + "=?", new String[]{category.getCategoryId()});
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}

	private void migrateChatBgTableData()
	{
		try
		{
			String create = getChatPropertiesTableCreateStatement();
			mDb.execSQL(create);

			String index = getChatPropertiesIndexCreateStatement();
			mDb.execSQL(index);

			String insert = "INSERT INTO " + DBConstants.CHAT_PROPERTIES_TABLE
					+ "( "
					+ DBConstants.MSISDN + ", "
					+ ChatThemes.THEME_COL_BG_ID + ", "
					+ ChatThemes.PREV_THEME_ID_COL + ", "
					+ DBConstants.BG_TIMESTAMP
					+ " ) SELECT "
					+ DBConstants.MSISDN + ", "
					+ ChatThemes.THEME_COL_BG_ID + ", "
					+ ChatThemes.PREV_THEME_ID_COL + ", "
					+ DBConstants.TIMESTAMP
					+ " FROM " + DBConstants.CHAT_BG_TABLE;

			mDb.execSQL(insert);

			String dropIndex = "DROP INDEX IF EXISTS " + DBConstants.CHAT_BG_INDEX;
			mDb.execSQL(dropIndex);

			String drop = "DROP TABLE IF EXISTS " + DBConstants.CHAT_BG_TABLE;
			mDb.execSQL(drop);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * This method saves the mute data in chatPropTable on upgrade, so as to restore the previous settings
	 */
	private void migrateMuteData()
	{
		migrateGroupMuteData();
		migrateBotMuteData();
	}

	/**
	 * This method restores the mute settings for bots
	 */
	private void migrateBotMuteData()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.BOT_TABLE, new String[] { DBConstants.MSISDN, DBConstants.IS_MUTE }, DBConstants.IS_MUTE + " = 1", null, null, null, null);
			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
				int isMute = c.getInt(c.getColumnIndex(DBConstants.IS_MUTE));
				int muteDuration = HikeConstants.MuteDuration.DURATION_FOREVER;
				long muteTimestamp = System.currentTimeMillis();

				Mute mute = new Mute.InitBuilder(msisdn).setIsMute(isMute == 1).setMuteDuration(muteDuration).setMuteTimestamp(muteTimestamp).setShowNotifInMute(false).build();
				toggleChatMute(mute);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	/**
	 * This method restores the mute settings for groups
	 */
	private void migrateGroupMuteData()
	{
		Cursor c = null;
		try
		{
			c = mDb.query(DBConstants.GROUP_INFO_TABLE, new String[] { DBConstants.GROUP_ID, DBConstants.MUTE_GROUP }, DBConstants.MUTE_GROUP + " = 1", null, null, null, null);
			while (c.moveToNext())
			{
				String msisdn = c.getString(c.getColumnIndex(DBConstants.GROUP_ID));
				int isMute = c.getInt(c.getColumnIndex(DBConstants.MUTE_GROUP));
				int muteDuration = HikeConstants.MuteDuration.DURATION_FOREVER;
				long muteTimestamp = System.currentTimeMillis();

				Mute mute = new Mute.InitBuilder(msisdn).setIsMute(isMute == 1).setMuteDuration(muteDuration).setMuteTimestamp(muteTimestamp).setShowNotifInMute(false).build();
				toggleChatMute(mute);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}
	}

	public List<StickerCategory> getAllStickerCategories()
	{
		Cursor c = null;
		LinkedHashMap<String, StickerCategory> stickerDataMap;

		try
		{
			c = mDb.query(DBConstants.STICKER_CATEGORIES_TABLE, null, null, null, null, null, null);
			stickerDataMap = parseStickerCategoriesCursor(c);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		List<StickerCategory> catList = new ArrayList<StickerCategory>(stickerDataMap.values());
		Collections.sort(catList);
		return catList;
	}

	public Set<Sticker> getRecentStickers()
	{
		Cursor c = null;
		Set<Sticker> stickerSet = null;
		try
		{
			c = mDb.query(DBConstants.RECENT_STICKERS_TABLE, null, null, null, null, null, null);
			int stIdIdx = c.getColumnIndex(DBConstants.STICKER_ID);
			int catIdIdx = c.getColumnIndex(DBConstants.CATEGORY_ID);

			stickerSet = Collections.synchronizedSet(new LinkedHashSet<Sticker>(c.getCount()));

			while(c.moveToNext())
			{
				String stickerId = c.getString(stIdIdx);
				String categoryId = c.getString(catIdIdx);
				stickerSet.add(new Sticker(categoryId, stickerId));
			}
		}
		finally
		{
			if( null != c)
			{
				c.close();
			}
		}
		return stickerSet;
	}

	public void saveRecentStickers(Set<Sticker> recentStickers)
	{
		if(Utils.isEmpty(recentStickers))
		{
			Logger.d("recent sticker", "empty or null recent sticker set. cant save to table");
			return;
		}
		try {
			mDb.beginTransaction();

			/**
			 * First clear recents table
			 */
			mDb.delete(DBConstants.RECENT_STICKERS_TABLE, null, null);

			/**
			 * now insert recent stickers
			 */
			ContentValues contentValues = new ContentValues();
			for (Sticker sticker : recentStickers) {
				contentValues.clear();
				contentValues.put(DBConstants.CATEGORY_ID, sticker.getCategoryId());
				contentValues.put(DBConstants.STICKER_ID, sticker.getStickerId());
				mDb.insertWithOnConflict(DBConstants.RECENT_STICKERS_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}
	public String getReceiptsTableCreateStatement() {
		String sql = "CREATE TABLE IF NOT EXISTS " + DBConstants.RECEIPTS_TABLE
			+ " ( "
			+ DBConstants.MESSAGE_ID + " INTEGER, " // The message id (Unique)
			+ DBConstants.RECEIVER_MSISDN + " TEXT, " // The msisdn of the receiver for which the report is received
			+ DBConstants.MSISDN + " TEXT, " // The msisdn of the receiver for which the report is received
			+ DBConstants.READ_TIMESTAMP + " INTEGER, " // Read Time when the message was read by the recepient.
			+ DBConstants.DELIVERY_TIMESTAMP + " INTEGER, " // Delivery Time when the message was delivered to the recepient.
			+ DBConstants.PLAYED_TIMESTAMP + " INTEGER, " // Delivery Time when the message was delivered to the recepient.
			+ "PRIMARY KEY ("+DBConstants.MESSAGE_ID+", "+DBConstants.RECEIVER_MSISDN+")"
			+ " ) ";
		return sql;
	}

	public QuickSuggestionStickerCategory getQuickStickerSuggestionsForSticker(QuickSuggestionStickerCategory quickSuggestionCategory)
	{
		Sticker quickSuggestSticker = quickSuggestionCategory.getQuickSuggestSticker();
		Cursor c = null;
		try
		{
			c = mDb.query(
					DBConstants.STICKER_TABLE,
					new String[] { DBConstants.QUICK_SUGGESTED_REPLY_STICKERS, DBConstants.QUICK_SUGGESTED_SENT_STICKERS, DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME },
					DBConstants.CATEGORY_ID + "=?" + " AND " + DBConstants.STICKER_ID + "=?",
					new String[] { quickSuggestSticker.getCategoryId(), quickSuggestSticker.getStickerId() },
					null, null, null, null);

			int replyStickerSetStringIndex = c.getColumnIndex(QUICK_SUGGESTED_REPLY_STICKERS);
			int sentStickerSetStringIndex = c.getColumnIndex(QUICK_SUGGESTED_SENT_STICKERS);
			int lastRefreshTimeIdx = c.getColumnIndex(LAST_QUICK_SUGGESTION_REFRESH_TIME);

			if(c.moveToFirst())
			{
					quickSuggestionCategory.setReplyStickers(QuickSuggestionStickerCategory.replyStickerSetFromString(c.getString(replyStickerSetStringIndex)));
					quickSuggestionCategory.setSentStickers(QuickSuggestionStickerCategory.sentStickerSetFromSting(c.getString(sentStickerSetStringIndex)));
					quickSuggestionCategory.setLastRefreshTime(c.getLong(lastRefreshTimeIdx));
			}
		}
		finally
		{
			if(null != c)
			{
				c.close();
			}
		}
		return quickSuggestionCategory;
	}

	public void insertQuickSuggestionData(List<StickerCategory> stickerCategoryList)
	{
		try
		{
			mDb.beginTransaction();
			ContentValues contentValues = new ContentValues();
			for(StickerCategory stickerCategory : stickerCategoryList)
			{
				contentValues.clear();

				QuickSuggestionStickerCategory quickSuggestionStickerCategory = (QuickSuggestionStickerCategory) stickerCategory;
				Sticker quickSuggestionSticker = quickSuggestionStickerCategory.getQuickSuggestSticker();

				String replySetString = quickSuggestionStickerCategory.replyStickerSetToString();
				String sentSetString = quickSuggestionStickerCategory.sentStickerSetToSting();
				Logger.d(getClass().getSimpleName(), "reply string for sticker " + quickSuggestionSticker + " is : " + replySetString);
				Logger.d(getClass().getSimpleName(), "sent string for sticker " + quickSuggestionSticker + " is : " + sentSetString);
				contentValues.put(DBConstants.QUICK_SUGGESTED_REPLY_STICKERS, replySetString);
				contentValues.put(DBConstants.QUICK_SUGGESTED_SENT_STICKERS, sentSetString);
				contentValues.put(DBConstants.LAST_QUICK_SUGGESTION_REFRESH_TIME, System.currentTimeMillis());
				int updatesRows = mDb.update(DBConstants.STICKER_TABLE, contentValues, DBConstants.CATEGORY_ID + "=?" + " AND " + DBConstants.STICKER_ID + "=?", new String[] {quickSuggestionSticker.getCategoryId(), quickSuggestionSticker.getStickerId()});
 				Logger.d(getClass().getSimpleName(), updatesRows + " rows are updated during quick suggestions insert");
			}
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}
	}


	public Set<Sticker> getAllStickers()
	{
		Cursor c = null;
		Set<Sticker> stickerSet = null;
		try
		{
			c = mDb.query(DBConstants.STICKER_TABLE, new String[] { DBConstants.CATEGORY_ID, DBConstants.STICKER_ID }, null, null, null, null, null, null);

			stickerSet = new HashSet<>(c.getCount());

			int categoryIdIdx = c.getColumnIndex(DBConstants.CATEGORY_ID);
			int stickerIdIdx = c.getColumnIndex(DBConstants.STICKER_ID);

			while(c.moveToNext())
			{
				Sticker sticker = new Sticker(c.getString(categoryIdIdx), c.getString(stickerIdIdx));
				stickerSet.add(sticker);
			}
		}
		finally
		{
			if (null != c)
			{
				c.close();
			}
		}
		return stickerSet;
	}

	public Set<String> getAllMsisdnsForMissingUID()
	{
		String query="Select distinct msisdn from " + DBConstants.GROUP_MEMBERS_TABLE + " where uid is null and " + DBConstants.ONHIKE  + " = 1";

		Cursor c =mDb.rawQuery(query,null);

		Set<String> msisdns = new HashSet<>(c.getCount());
		String msisdn = null;
		while(c.moveToNext())
		{
			msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			msisdns.add(msisdn);
		}
		return msisdns;
	}

	public void updateContactUidForGroupMembers(Set<FetchUIDTaskPojo> data) {
		if (Utils.isEmpty(data)) {
			return;
		}
		mDb.beginTransaction();
		try {
			ContentValues cv = new ContentValues();
			for (FetchUIDTaskPojo d : data) {
				if (TextUtils.isEmpty(d.getUid())) {    // onHike is false
					cv.put(DBConstants.ONHIKE, 0);
				} else {
					cv.put(DBConstants.HIKE_UID, d.getUid());
					cv.put(DBConstants.ONHIKE, 1);
				}
				long rows = mDb.update(DBConstants.GROUP_MEMBERS_TABLE, cv, DBConstants.MSISDN + "=?", new String[]{d.getMsisdn()});
				cv.clear();
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}
	//Methods to create the query for table creation for OTA Chat Themes
	/**
	 * Method to create a query for creating CHAT_THEME_ASSET_TABLE in the database
	 * @return string containing a sql query that creates the CHAT_THEME_ASSET_TABLE
	 */
	private static String getAssetTableCreateQuery()
	{
		StringBuilder createAssetTableQuery = new StringBuilder();
		createAssetTableQuery = createAssetTableQuery.append(CREATE_TABLE + ChatThemes.CHAT_THEME_ASSET_TABLE + " (")
				.append(ChatThemes.ASSET_COL_ID 				+ COLUMN_TYPE_TEXT 		+ " PRIMARY KEY" + COMMA_SEPARATOR)
				.append(ChatThemes.ASSET_COL_TYPE 				+ COLUMN_TYPE_INTEGER 	+ COMMA_SEPARATOR)
				.append(ChatThemes.ASSET_COL_VAL 				+ COLUMN_TYPE_TEXT 		+ COMMA_SEPARATOR)
				.append(ChatThemes.ASSET_COL_IS_DOWNLOADED 	+ COLUMN_TYPE_INTEGER 	+ COMMA_SEPARATOR)
				.append(ChatThemes.ASSET_COL_SIZE				+ COLUMN_TYPE_INTEGER	+ COMMA_SEPARATOR)
				.append(ChatThemes.CHAT_THEME_TIMESTAMP_COL + COLUMN_TYPE_INTEGER)
				.append(")");

		return createAssetTableQuery.toString();
	}

	/**
	 * Method to create a query for creating CHAT_THEME_TABLE in the database
	 * @return string containing a sql query that create the CHAT_THEME_TABLE
	 */
	private static String getThemeTableCreateQuery()
	{
		StringBuilder createThemeTableQuery = new StringBuilder();
		createThemeTableQuery = createThemeTableQuery.append(CREATE_TABLE + ChatThemes.CHAT_THEME_TABLE + " (")
				.append(ChatThemes.THEME_COL_BG_ID + COLUMN_TYPE_TEXT + " PRIMARY KEY" + COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_TYPE                        + COLUMN_TYPE_INTEGER 				+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_BG_PORTRAIT                 + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_BG_LANDSCAPE                + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_BUBBLE                      + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_HEADER                      + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_SEND_NUDGE 				  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_RECEIVE_NUDGE 			  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_INLINE_UPDATE_BG 			  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_SMS_BG 					  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_MULTI_SELECT_BUBBLE_COLOR	  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_OFFLINE_MESSAGE_TEXT_COLOR  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_THUMBNAIL 				  + COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.CHAT_THEME_TIMESTAMP_COL			  + COLUMN_TYPE_INTEGER					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_STATUS_BAR_COL 			+ COLUMN_TYPE_TEXT 					+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_BUBBLE_BG 					+ COLUMN_TYPE_TEXT					 + COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_SYSTEM_MESSAGE 			+ COLUMN_TYPE_INTEGER					 + COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_VISIBLE 				+ COLUMN_TYPE_INTEGER 						+ COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_ORDER 					+ COLUMN_TYPE_INTEGER					 + COMMA_SEPARATOR)
				.append(ChatThemes.THEME_COL_METADATA + COLUMN_TYPE_TEXT)
				.append(")");

		return createThemeTableQuery.toString();
	}

	//Saving objects to database for Chat Themes and Chat Theme Assets.

	/**
	 * method to store an asset object in the asset table
	 * @param saveAsset the asset object which is to be saved in the DB
	 * @param insertPrepStmt a compiled statement for inserting a row in asset table
	 * @param updatePrepStmt a compiled statement for updating a row in asset table
	 * @return true if the asset sent is saved or updated successfully in the table, else false
	 */
	private boolean saveChatThemeAsset(HikeChatThemeAsset saveAsset, SQLiteStatement insertPrepStmt, SQLiteStatement updatePrepStmt)
	{
		if(mDb == null || saveAsset == null)
			return false;

		updatePrepStmt.bindString(1, saveAsset.getAssetId());
		updatePrepStmt.bindLong(2, saveAsset.getType());

		if(saveAsset.getValue() != null)
			updatePrepStmt.bindString(3, saveAsset.getValue());
		else
			updatePrepStmt.bindNull(3);

		updatePrepStmt.bindLong(4, saveAsset.getSize());
		updatePrepStmt.bindLong(5, saveAsset.getAssetDownloadStatus());
		updatePrepStmt.bindString(6, saveAsset.getAssetId());

		long rowsAffected = updatePrepStmt.executeUpdateDelete();
		updatePrepStmt.clearBindings();

		if(rowsAffected == 0) // asset is not present
		{
			insertPrepStmt.bindString(1, saveAsset.getAssetId());
			insertPrepStmt.bindLong(2, saveAsset.getType());

			if(saveAsset.getValue() != null)
				insertPrepStmt.bindString(3, saveAsset.getValue());
			else
				insertPrepStmt.bindNull(3);

			insertPrepStmt.bindLong(4, saveAsset.getSize());
			insertPrepStmt.bindLong(5, saveAsset.getAssetDownloadStatus());

			long rowInserted = insertPrepStmt.executeInsert();
			insertPrepStmt.clearBindings();

			if(rowInserted == -1) // insertion didn't happen
				return false;
		}
		return true;
	}

	/**
	 * method to insert a list of asset objects in the asset table
	 * @param saveAssets a list of asset objects to be saved
	 * @return true if all the assets were inserted or updated successfully, else false
	 */
	public boolean saveChatThemeAssets(ArrayList<HikeChatThemeAsset> saveAssets)
	{
		SQLiteStatement insertPrepStmt = null, updatePrepStmt = null;

		insertPrepStmt = prepStmtForChatThemeAssetInsert();
		updatePrepStmt = prepStmtForChatThemeAssetUpdate();

		if(insertPrepStmt == null || updatePrepStmt == null)
			return false;

		boolean allSaved = true;
		mDb.beginTransaction();
		try
		{
			for (HikeChatThemeAsset saveAsset : saveAssets)
				allSaved = allSaved && saveChatThemeAsset(saveAsset, insertPrepStmt, updatePrepStmt);
			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

		return allSaved;
	}

	/**
	 * method to save a single asset in the CHAT_THEME_ASSET_TABLE
	 * @param saveAsset
	 * @return true if the asset has been stored successfully, else false
	 */
	public boolean saveChatThemeAsset(HikeChatThemeAsset saveAsset)
	{
		SQLiteStatement insertPrepStmt = null, updatePrepStmt = null;

		insertPrepStmt = prepStmtForChatThemeAssetInsert();
		updatePrepStmt = prepStmtForChatThemeAssetUpdate();

		if(insertPrepStmt == null || updatePrepStmt == null)
			return false;

		boolean isSaved = saveChatThemeAsset(saveAsset, insertPrepStmt, updatePrepStmt);

		return isSaved;
	}

	/**
	 * method to create a compiled sql query for inserting an asset object into the table
	 * @return a SQLiteStatement object which stores a compiled query to insert an asset
	 */
	private SQLiteStatement prepStmtForChatThemeAssetInsert()
	{
		String sqlQuery = "INSERT INTO " + ChatThemes.CHAT_THEME_ASSET_TABLE + "("
				+ ChatThemes.ASSET_COL_ID + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_TYPE + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_VAL + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_SIZE + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_IS_DOWNLOADED
				+ ") VALUES (" ;

		//placeholders for values
		String insertValues = Utils.repeatString("?,", ChatThemes.CHAT_THEME_ASSET_TABLE_COL_COUNT);
		insertValues = insertValues.substring(0, insertValues.length() - 1);

		sqlQuery += insertValues + ");";

		if(mDb == null)
			return null;

		SQLiteStatement stmt = mDb.compileStatement(sqlQuery);
		return stmt;
	}

	/**
	 * method to create a compiled sql query for updating an asset object into the table
	 * @return a SQLiteStatement object which stores a compiled query to update an asset
	 */
	private SQLiteStatement prepStmtForChatThemeAssetUpdate()
	{
		String sqlQuery = "UPDATE " + ChatThemes.CHAT_THEME_ASSET_TABLE + " SET "
				+ ChatThemes.ASSET_COL_ID 			 +  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_TYPE 		 +  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_VAL 			 +  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_SIZE			 +  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.ASSET_COL_IS_DOWNLOADED +  " = ?"
				+ " WHERE " + ChatThemes.ASSET_COL_ID + " = ?;";

		if(mDb == null)
			return null;

		SQLiteStatement stmt = mDb.compileStatement(sqlQuery);
		return stmt;
	}

	/**
	 * method to save a chat theme object to the theme table
	 * @param saveTheme
	 * @return true if the theme has been saved/updated successfully, false otherwise
	 */
	private boolean saveChatTheme(HikeChatTheme saveTheme, SQLiteStatement insertPrepStmt, SQLiteStatement updatePrepStmt)
	{
		if(mDb == null || saveTheme == null)
			return false;

		updatePrepStmt.bindString(1, saveTheme.getThemeId());

		if(saveTheme.getAssetId(ASSET_INDEX_BG_LANDSCAPE) == null)
			updatePrepStmt.bindNull(2);
		else
			updatePrepStmt.bindString(2, saveTheme.getAssetId(ASSET_INDEX_BG_LANDSCAPE));

		if(saveTheme.getAssetId(ASSET_INDEX_BG_PORTRAIT) == null)
			updatePrepStmt.bindNull(3);
		else
			updatePrepStmt.bindString(3, saveTheme.getAssetId(ASSET_INDEX_BG_PORTRAIT));

		if(saveTheme.getAssetId(ASSET_INDEX_BUBBLE_COLOR) == null)
			updatePrepStmt.bindNull(4);
		else
			updatePrepStmt.bindString(4, saveTheme.getAssetId(ASSET_INDEX_BUBBLE_COLOR));

		if(saveTheme.getAssetId(ASSET_INDEX_CHAT_BUBBLE_BG) == null)
			updatePrepStmt.bindNull(5);
		else
			updatePrepStmt.bindString(5, saveTheme.getAssetId(ASSET_INDEX_CHAT_BUBBLE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_ACTION_BAR_BG) == null)
			updatePrepStmt.bindNull(6);
		else
			updatePrepStmt.bindString(6, saveTheme.getAssetId(ASSET_INDEX_ACTION_BAR_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_INLINE_STATUS_MSG_BG) == null)
			updatePrepStmt.bindNull(7);
		else
			updatePrepStmt.bindString(7, saveTheme.getAssetId(ASSET_INDEX_INLINE_STATUS_MSG_BG));

		if(saveTheme.getMetadata() == null)
			updatePrepStmt.bindNull(8);
		else
			updatePrepStmt.bindString(8, saveTheme.getMetadata());

		if(saveTheme.getAssetId(ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG) == null)
			updatePrepStmt.bindNull(9);
		else
			updatePrepStmt.bindString(9, saveTheme.getAssetId(ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_OFFLINE_MESSAGE_BG) == null)
			updatePrepStmt.bindNull(10);
		else
			updatePrepStmt.bindString(10, saveTheme.getAssetId(ASSET_INDEX_OFFLINE_MESSAGE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_RECEIVED_NUDGE_BG) == null)
			updatePrepStmt.bindNull(11);
		else
			updatePrepStmt.bindString(11, saveTheme.getAssetId(ASSET_INDEX_RECEIVED_NUDGE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_SENT_NUDGE_BG) == null)
			updatePrepStmt.bindNull(12);
		else
			updatePrepStmt.bindString(12, saveTheme.getAssetId(ASSET_INDEX_SENT_NUDGE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_SMS_TOGGLE_BG) == null)
			updatePrepStmt.bindNull(13);
		else
			updatePrepStmt.bindString(13, saveTheme.getAssetId(ASSET_INDEX_SMS_TOGGLE_BG));

		if(saveTheme.getAssetId(ASSET_INDEX_THUMBNAIL) == null)
			updatePrepStmt.bindNull(14);
		else
			updatePrepStmt.bindString(14, saveTheme.getAssetId(ASSET_INDEX_THUMBNAIL));

		if(saveTheme.getAssetId(ASSET_INDEX_STATUS_BAR_BG) == null)
			updatePrepStmt.bindNull(15);
		else
			updatePrepStmt.bindString(15, saveTheme.getAssetId(ASSET_INDEX_STATUS_BAR_BG));

		updatePrepStmt.bindLong(16, saveTheme.getThemeType());
		updatePrepStmt.bindLong(17, System.currentTimeMillis());
		updatePrepStmt.bindLong(18, saveTheme.isVisible() ? 1 : 0);
		updatePrepStmt.bindLong(19, saveTheme.getThemeOrderIndex());
		updatePrepStmt.bindLong(20, saveTheme.getSystemMessageType());
		updatePrepStmt.bindString(21, saveTheme.getThemeId());

		long rowsAffected = updatePrepStmt.executeUpdateDelete();
		updatePrepStmt.clearBindings();

		if(rowsAffected == 0) // new theme
		{
			insertPrepStmt.bindString(1, saveTheme.getThemeId());

			if(saveTheme.getAssetId(ASSET_INDEX_BG_LANDSCAPE) == null)
				insertPrepStmt.bindNull(2);
			else
				insertPrepStmt.bindString(2, saveTheme.getAssetId(ASSET_INDEX_BG_LANDSCAPE));

			if(saveTheme.getAssetId(ASSET_INDEX_BG_PORTRAIT) == null)
				insertPrepStmt.bindNull(3);
			else
				insertPrepStmt.bindString(3, saveTheme.getAssetId(ASSET_INDEX_BG_PORTRAIT));

			if(saveTheme.getAssetId(ASSET_INDEX_BUBBLE_COLOR) == null)
				insertPrepStmt.bindNull(4);
			else
				insertPrepStmt.bindString(4, saveTheme.getAssetId(ASSET_INDEX_BUBBLE_COLOR));

			if(saveTheme.getAssetId(ASSET_INDEX_CHAT_BUBBLE_BG) == null)
				insertPrepStmt.bindNull(5);
			else
				insertPrepStmt.bindString(5, saveTheme.getAssetId(ASSET_INDEX_CHAT_BUBBLE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_ACTION_BAR_BG) == null)
				insertPrepStmt.bindNull(6);
			else
				insertPrepStmt.bindString(6, saveTheme.getAssetId(ASSET_INDEX_ACTION_BAR_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_INLINE_STATUS_MSG_BG) == null)
				insertPrepStmt.bindNull(7);
			else
				insertPrepStmt.bindString(7, saveTheme.getAssetId(ASSET_INDEX_INLINE_STATUS_MSG_BG));

			if(saveTheme.getMetadata() == null)
				insertPrepStmt.bindNull(8);
			else
				insertPrepStmt.bindString(8, saveTheme.getMetadata());

			if(saveTheme.getAssetId(ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG) == null)
				insertPrepStmt.bindNull(9);
			else
				insertPrepStmt.bindString(9, saveTheme.getAssetId(ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_OFFLINE_MESSAGE_BG) == null)
				insertPrepStmt.bindNull(10);
			else
				insertPrepStmt.bindString(10, saveTheme.getAssetId(ASSET_INDEX_OFFLINE_MESSAGE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_RECEIVED_NUDGE_BG) == null)
				insertPrepStmt.bindNull(11);
			else
				insertPrepStmt.bindString(11, saveTheme.getAssetId(ASSET_INDEX_RECEIVED_NUDGE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_SENT_NUDGE_BG) == null)
				insertPrepStmt.bindNull(12);
			else
				insertPrepStmt.bindString(12, saveTheme.getAssetId(ASSET_INDEX_SENT_NUDGE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_SMS_TOGGLE_BG) == null)
				insertPrepStmt.bindNull(13);
			else
				insertPrepStmt.bindString(13, saveTheme.getAssetId(ASSET_INDEX_SMS_TOGGLE_BG));

			if(saveTheme.getAssetId(ASSET_INDEX_THUMBNAIL) == null)
				insertPrepStmt.bindNull(14);
			else
				insertPrepStmt.bindString(14, saveTheme.getAssetId(ASSET_INDEX_THUMBNAIL));

			if(saveTheme.getAssetId(ASSET_INDEX_STATUS_BAR_BG) == null)
				insertPrepStmt.bindNull(15);
			else
				insertPrepStmt.bindString(15, saveTheme.getAssetId(ASSET_INDEX_STATUS_BAR_BG));

			insertPrepStmt.bindLong(16, saveTheme.getThemeType());
			insertPrepStmt.bindLong(17, System.currentTimeMillis());
			insertPrepStmt.bindLong(18, saveTheme.isVisible() ? 1 : 0);
			insertPrepStmt.bindLong(19, saveTheme.getThemeOrderIndex());
			insertPrepStmt.bindLong(20, saveTheme.getSystemMessageType());

			long rowInserted = insertPrepStmt.executeInsert();
			insertPrepStmt.clearBindings();

			if(rowInserted == -1)
				return false;
		}
		return true;

	}

	/**
	 * method to store a list of theme objects into the theme table
	 * @param saveThemes list of themes to be stored
	 * @return true if all the themes in the list are successfully inserted or updated in the table, else false
	 */
	public boolean saveChatThemes(ArrayList<HikeChatTheme> saveThemes)
	{
		SQLiteStatement insertPrepStmt = null, updatePrepStmt = null;

		insertPrepStmt = prepStmtForChatThemeInsert();
		updatePrepStmt = prepStmtForChatThemeUpdate();

		if(insertPrepStmt == null || updatePrepStmt == null)
			return false;

		boolean allSaved = true;

		mDb.beginTransaction();
		try
		{
			for(HikeChatTheme saveTheme : saveThemes)
				allSaved = allSaved && saveChatTheme(saveTheme, insertPrepStmt, updatePrepStmt);

			mDb.setTransactionSuccessful();
		}
		finally
		{
			mDb.endTransaction();
		}

		return allSaved;
	}

	public boolean saveChatTheme(HikeChatTheme theme)
	{
		SQLiteStatement insertPrepStmt = null, updatePrepStmt = null;

		insertPrepStmt = prepStmtForChatThemeInsert();
		updatePrepStmt = prepStmtForChatThemeUpdate();

		if(insertPrepStmt == null || updatePrepStmt == null)
			return false;

		return saveChatTheme(theme, insertPrepStmt, updatePrepStmt);
	}

	/**
	 * method to create a compiled sql query for inserting an asset object into the table
	 * @return a SQLiteStatement object which stores a compiled query to insert an asset
	 */
	private SQLiteStatement prepStmtForChatThemeInsert()
	{
		String sqlQuery = "INSERT INTO " + ChatThemes.CHAT_THEME_TABLE + "("
				+ ChatThemes.THEME_COL_BG_ID + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BG_LANDSCAPE + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BG_PORTRAIT + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BUBBLE + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BUBBLE_BG + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_HEADER + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_INLINE_UPDATE_BG + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_METADATA + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_MULTI_SELECT_BUBBLE_COLOR + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_OFFLINE_MESSAGE_TEXT_COLOR + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_RECEIVE_NUDGE + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SEND_NUDGE + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SMS_BG + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_THUMBNAIL + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_STATUS_BAR_COL + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_TYPE + COMMA_SEPARATOR
				+ ChatThemes.CHAT_THEME_TIMESTAMP_COL + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_VISIBLE + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_ORDER + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SYSTEM_MESSAGE
				+ ") VALUES (" ;

		//placeholders for values
		String insertValues = Utils.repeatString("?,", ChatThemes.CHAT_THEME_TABLE_COL_COUNT);
		insertValues = insertValues.substring(0, insertValues.length() - 1);

		sqlQuery += insertValues + ");";

		if(mDb == null)
			return null;

		SQLiteStatement stmt = mDb.compileStatement(sqlQuery);
		return stmt;
	}

	/**
	 * method to make a compiled query for updating a row in the theme table
	 * @return a compiled statement for the query to update an existing row in the table
	 */
	private SQLiteStatement prepStmtForChatThemeUpdate()
	{
		String sqlQuery = "UPDATE " + ChatThemes.CHAT_THEME_TABLE + " SET "
				+ ChatThemes.THEME_COL_BG_ID 						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BG_LANDSCAPE 				+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BG_PORTRAIT 					+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BUBBLE 						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_BUBBLE_BG 					+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_HEADER 						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_INLINE_UPDATE_BG 			+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_METADATA 					+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_MULTI_SELECT_BUBBLE_COLOR 	+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_OFFLINE_MESSAGE_TEXT_COLOR 	+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_RECEIVE_NUDGE 				+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SEND_NUDGE 					+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SMS_BG 						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_THUMBNAIL 					+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_STATUS_BAR_COL				+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_TYPE 						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.CHAT_THEME_TIMESTAMP_COL 				+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_VISIBLE						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_ORDER						+  " = ?" + COMMA_SEPARATOR
				+ ChatThemes.THEME_COL_SYSTEM_MESSAGE 				+  " = ?"
				+ " WHERE " + ChatThemes.THEME_COL_BG_ID + " = ?;";

		if(mDb == null)
			return null;

		SQLiteStatement stmt = mDb.compileStatement(sqlQuery);
		return stmt;
	}

	/**
	 * method to load all the chat themes from the DB to memory
	 * @return a map of chat theme objects each of which represents a row in the theme table with key as the unique theme id
	 * and value as the HikeChatTheme object
	 */
	public LinkedHashMap<String, HikeChatTheme> getAllChatThemes()
	{
		LinkedHashMap<String, HikeChatTheme> themes = new LinkedHashMap<>();

		String getThemesQuery = "SELECT * FROM " + ChatThemes.CHAT_THEME_TABLE +" ORDER BY "+ChatThemes.THEME_COL_ORDER+" ASC, "+ChatThemes.CHAT_THEME_TIMESTAMP_COL+" DESC";
		Cursor themeListCursor = mDb.rawQuery(getThemesQuery, null);
		try {
			if (themeListCursor.moveToFirst()) {
				//loading all themes (theme id and assets) from the datsabase to the memory
				while (!themeListCursor.isAfterLast()) {
					HikeChatTheme chatTheme = makeChatThemeFromDbRow(themeListCursor);
					themes.put(chatTheme.getThemeId(), chatTheme);
					themeListCursor.moveToNext();
				}
			}
		}
		finally
		{
			if (themeListCursor != null)
			{
				themeListCursor.close();
			}
		}
		return themes;
	}

	/**
	 * method to make a chat theme object from a row cursor of theme table
	 * @param themeDbRow a cursor to a row in the theme table
	 * @return a chat theme object made from the row
	 */
	private HikeChatTheme makeChatThemeFromDbRow(Cursor themeDbRow)
	{
		String themeId = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BG_ID));

		String landscapeBg = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BG_LANDSCAPE)))
			landscapeBg = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BG_LANDSCAPE));

		String portraitBg = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BG_PORTRAIT)))
			portraitBg = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BG_PORTRAIT));

		String bubble = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BUBBLE)))
			bubble = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BUBBLE));

		String bubbleBg = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BUBBLE_BG)))
			bubbleBg = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_BUBBLE_BG));

		String header = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_HEADER)))
			header = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_HEADER));

		String inlineBackground = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_INLINE_UPDATE_BG)))
			inlineBackground = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_INLINE_UPDATE_BG));

		String meta = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_METADATA)))
			meta = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_METADATA));

		String multiSelBubble = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_MULTI_SELECT_BUBBLE_COLOR)))
			multiSelBubble = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_MULTI_SELECT_BUBBLE_COLOR));

		String offMsg = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_OFFLINE_MESSAGE_TEXT_COLOR)))
			offMsg = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_OFFLINE_MESSAGE_TEXT_COLOR));

		String sendNudge = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_SEND_NUDGE)))
			sendNudge = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_SEND_NUDGE));

		String recNudge = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_RECEIVE_NUDGE)))
			recNudge = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_RECEIVE_NUDGE));

		String smsBackground = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_SMS_BG)))
			smsBackground = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_SMS_BG));

		String statusBarCol = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_STATUS_BAR_COL)))
			statusBarCol = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_STATUS_BAR_COL));

		String thumbnail = null;
		if(!themeDbRow.isNull(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_THUMBNAIL)))
			thumbnail = themeDbRow.getString(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_THUMBNAIL));

		int type = themeDbRow.getInt(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_TYPE));
		long timestamp = themeDbRow.getLong(themeDbRow.getColumnIndex(ChatThemes.CHAT_THEME_TIMESTAMP_COL));
		boolean isVisible = (themeDbRow.getInt(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_VISIBLE)) == 1) ? true : false;
		int order = themeDbRow.getInt(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_ORDER));
		int messageType = themeDbRow.getInt(themeDbRow.getColumnIndex(ChatThemes.THEME_COL_SYSTEM_MESSAGE));

		//loading into the object
		HikeChatTheme chatTheme = new HikeChatTheme();
		chatTheme.setThemeId(themeId);
		chatTheme.setAsset(ASSET_INDEX_BG_LANDSCAPE, landscapeBg);
		chatTheme.setAsset(ASSET_INDEX_BG_PORTRAIT, portraitBg);
		chatTheme.setAsset(ASSET_INDEX_BUBBLE_COLOR, bubble);
		chatTheme.setAsset(ASSET_INDEX_CHAT_BUBBLE_BG, bubbleBg);
		chatTheme.setAsset(ASSET_INDEX_ACTION_BAR_BG, header);
		chatTheme.setAsset(ASSET_INDEX_INLINE_STATUS_MSG_BG, inlineBackground);
		chatTheme.setAsset(ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG, multiSelBubble);
		chatTheme.setAsset(ASSET_INDEX_OFFLINE_MESSAGE_BG, offMsg);
		chatTheme.setAsset(ASSET_INDEX_SENT_NUDGE_BG, sendNudge);
		chatTheme.setAsset(ASSET_INDEX_RECEIVED_NUDGE_BG, recNudge);
		chatTheme.setAsset(ASSET_INDEX_SMS_TOGGLE_BG, smsBackground);
		chatTheme.setAsset(ASSET_INDEX_STATUS_BAR_BG, statusBarCol);
		chatTheme.setAsset(ASSET_INDEX_THUMBNAIL, thumbnail);
		chatTheme.setThemeType(type);
		chatTheme.setMetadata(meta);
		chatTheme.setVisibilityStatus(isVisible);
		chatTheme.setThemeOrderIndex(order);
		chatTheme.setSystemMessageType(messageType);

		return chatTheme;
	}

	/**
	 * method to load all assets from the db to memory
	 * @return a map of assets with the key as the UUID of the asset
	 */
	public ConcurrentHashMap<String, HikeChatThemeAsset> getAllChatThemeAssets()
	{
		ConcurrentHashMap<String, HikeChatThemeAsset> assetMap = new ConcurrentHashMap<>();

		String getThemesQuery = "SELECT * FROM " + ChatThemes.CHAT_THEME_ASSET_TABLE;
		Cursor assetListCursor = mDb.rawQuery(getThemesQuery, null);
		try
		{
			if (assetListCursor.moveToFirst())
			{
				//loading all assets from the database to the memory
				while (!assetListCursor.isAfterLast())
				{
					HikeChatThemeAsset asset = makeAssetFromDbRow(assetListCursor);
					if(asset != null)
					{
						assetMap.put(asset.getAssetId(), asset);
					}
					assetListCursor.moveToNext();
				}
			}
		}
		finally
		{
			if (assetListCursor != null)
			{
				assetListCursor.close();
			}
		}
		return assetMap;
	}

	/**
	 * method to make a chat theme asset from a row in the asset table
	 * @param assetDbCursor cursor to the asset to be loaded
	 * @return a chat theme asset object corresponding to the row the cursor points to
	 */
	private HikeChatThemeAsset makeAssetFromDbRow(Cursor assetDbCursor) {
		String assetId = assetDbCursor.getString(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_ID));
		int assetType = assetDbCursor.getInt(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_TYPE));

		String assetVal = null;
		if (!assetDbCursor.isNull(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_VAL)))
			assetVal = assetDbCursor.getString(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_VAL));

		int assetSize = assetDbCursor.getInt(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_SIZE));
		int isDownloaded = assetDbCursor.getInt(assetDbCursor.getColumnIndex(ChatThemes.ASSET_COL_IS_DOWNLOADED));

		HikeChatThemeAsset asset = new HikeChatThemeAsset(assetId, assetType, assetVal, assetSize);
		asset.setIsDownloaded((byte) isDownloaded);

		return asset;
	}

	private String getUserParameterTableCreateQuery()
	{
		String sql = CREATE_TABLE + DBConstants.USER_PARAMETER_TABLE
				+ " ( "
				+ DBConstants.PARAMETER_KEY + " TEXT PRIMARY KEY, "
				+ DBConstants.PARAMETER_VALUE + " TEXT"
				+ " )";

		return sql;
	}

	private String getParameterMappingTableCreateQuery()
	{
		String sql = CREATE_TABLE + DBConstants.PARAMETER_MAPPING_TABLE
				+ " ( "
				+ DBConstants.URL + " TEXT PRIMARY KEY, "
				+ DBConstants.METHOD + " TEXT, "
				+ DBConstants.PARAMETER_LIST + " TEXT"
				+ " )";

		return sql;
	}

	public void insertParameterListInDb(List<Pair<String, String>> parameterList)
	{
		try {
			mDb.beginTransaction();
			ContentValues contentValues = new ContentValues();
			for(Pair<String, String> pair : parameterList)
			{
				contentValues.clear();
				contentValues.put(PARAMETER_KEY, pair.first);
				contentValues.put(PARAMETER_VALUE, pair.second);
				mDb.insertWithOnConflict(USER_PARAMETER_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	public void insertParameterMappingInDb(List<Pair<String, Pair<String, String>>> parameterMapping)
	{
		try {
			mDb.beginTransaction();
			ContentValues contentValues = new ContentValues();
			for(Pair<String, Pair<String, String>> pair : parameterMapping)
			{
				contentValues.clear();
				contentValues.put(URL, pair.first);
				contentValues.put(METHOD, pair.second.first);
				contentValues.put(PARAMETER_LIST, pair.second.second);
				mDb.insertWithOnConflict(PARAMETER_MAPPING_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	public List<String> getParameterListForUrl(String url, String method) {
		Cursor c = null;
		List<String> parameterList = null;

		try {
			c = mDb.query(
					DBConstants.PARAMETER_MAPPING_TABLE,
					new String[]{DBConstants.PARAMETER_LIST},
					DBConstants.URL + "=?" + " AND " + DBConstants.METHOD + "=?",
					new String[]{url, method},
					null, null, null, null);

			int parameterListIdx = c.getColumnIndex(DBConstants.PARAMETER_LIST);
			if (c.moveToFirst()) {
				String parameterString = c.getString(parameterListIdx);

				JSONArray parameterArray = TextUtils.isEmpty(parameterString) ? null : new JSONArray(parameterString);
				parameterList = Utils.jsonArrayToList(parameterArray);
			}
		} catch (JSONException e) {
			Logger.e(HikeConversationsDatabase.class.getName(), "exception in getParameterArrayForUrl ", e);
		} finally {
			if (null != c) {
				c.close();
			}
		}
		return parameterList;
	}

	public List<Pair<String, String >> getParameterMapping(String parameters) {

		Cursor c = null;
		List<Pair<String, String>> parameterMapping = null;
		try {
			if (!TextUtils.isEmpty(parameters)) {

				c = mDb.query(DBConstants.USER_PARAMETER_TABLE, null, DBConstants.PARAMETER_KEY + " IN " + parameters, null, null, null, null);
				parameterMapping = new ArrayList<>(c.getCount());

				int parameterKeyIdx = c.getColumnIndex(DBConstants.PARAMETER_KEY);
				int parameterValueIdx = c.getColumnIndex(DBConstants.PARAMETER_VALUE);

				while (c.moveToNext()) {
					String parameterKey = c.getString(parameterKeyIdx);
					String parameterValue = c.getString(parameterValueIdx);
					parameterMapping.add(new Pair<>(parameterKey, parameterValue));
				}
			}
		} finally {
			if (null != c) {
				c.close();
			}
		}
		return parameterMapping;
	}

	public void upgradeForChatProperties() {

		try {

			mDb.beginTransaction();
			migrateChatBgTableData();
			migrateMuteData();
			mDb.setTransactionSuccessful();

		} catch (Exception e) {

			Logger.e(getClass().getSimpleName(), "Exception : ", e);
			e.printStackTrace();

		} finally {

			mDb.endTransaction();
		}
	}
}
