package com.bsb.hike.db;

import com.bsb.hike.models.ContactInfo.FavoriteType;

public interface DBConstants
{
	public static final int CONVERSATIONS_DATABASE_VERSION = 48;

	public static final int USERS_DATABASE_VERSION = 17;

	public static final String HAS_CUSTOM_PHOTO = "hascustomphoto";

	public static final String CONVERSATIONS_DATABASE_NAME = "chats";

	public static final String CONVERSATIONS_TABLE = "conversations";

	public static final String MESSAGES_TABLE = "messages";

	public static final String USERS_DATABASE_NAME = "hikeusers";

	public static final String USERS_TABLE = "users";

	public static final String GROUP_MEMBERS_TABLE = "groupMembers";

	public static final String GROUP_INFO_TABLE = "groupInfo";

	/* Table Constants */

	public static final String MESSAGE = "message";

	public static final String MSG_STATUS = "msgStatus";

	public static final String TIMESTAMP = "timestamp";

	public static final String MESSAGE_ID = "msgid";
	
	public static final String GROUP_CREATOR = "grpCreator";
	
	public static final String MAPPED_MSG_ID = "mappedMsgId";
	
	public static final String MESSAGE_HASH = "msgHash";

	public static final String CONV_ID = "convid";
	
	public static final String MESSAGE_TYPE = "type";

	public static final String CONVERSATION_METADATA = "convMetadata";

	public static final String ONHIKE = "onhike";

	public static final String CONTACT_ID = "contactid";

	public static final String MSISDN = "msisdn";

	public static final String MESSAGE_METADATA = "metadata";

	public static final String CONVERSATION_INDEX = "conversation_idx";

	public static final String ID = "id";

	public static final String NAME = "name";

	public static final String PHONE = "phoneNumber";

	public static final String BLOCK_TABLE = "blocked";

	public static final String THUMBNAILS_TABLE = "thumbnails";

	public static final String IMAGE = "image";

	public static final String OVERLAY_DISMISSED = "overlayDismissed";

	public static final String GROUP_ID = "groupId";

	public static final String GROUP_NAME = "groupName";
	
	public static final String GROUP_CREATION_TIME = "groupCreationTime";

	public static final String GROUP_INDEX = "group_idx";

	public static final String GROUP_PARTICIPANT = "groupParticipant";

	public static final String GROUP_OWNER = "groupOwner";

	public static final String GROUP_ALIVE = "groupAlive";

	public static final String HAS_LEFT = "hasLeft";
	
	public static final String TYPE = "type";

	public static final String LAST_MESSAGED = "lastMessaged";

	public static final String MSISDN_TYPE = "msisdnType";

	public static final String FILE_TABLE = "fileTable";

	public static final String FILE_KEY = "fileKey";

	public static final String FILE_NAME = "fileName";

	public static final String ON_DND = "onDnd";

	public static final String SHOWN_STATUS = "shownStatus";

	public static final String EMOTICON_TABLE = "emoticonTable";

	public static final String EMOTICON_NUM = "emoticonNum";

	public static final String LAST_USED = "lastUsed";

	public static final String EMOTICON_INDEX = "emoticonIdx";

	public static final String MUTE_GROUP = "muteGroup";
	
	public static final String IS_MUTE = "isMute";

	public static final String FAVORITES_TABLE = "favoritesTable";

	public static final String FAVORITE_TYPE = "favoriteType";

	public static final String FAVORITE_TYPE_SELECTION = "COALESCE((SELECT " + FAVORITE_TYPE + " FROM " + FAVORITES_TABLE + " WHERE " + FAVORITES_TABLE + "." + MSISDN + " = "
			+ USERS_TABLE + "." + MSISDN + "), + " + FavoriteType.NOT_FRIEND.ordinal() + ") AS " + FAVORITE_TYPE;

	public static final String STATUS_TABLE = "statusTable";

	public static final String STATUS_ID = "statusId";

	public static final String STATUS_MAPPED_ID = "statusMappedId";

	public static final String STATUS_TEXT = "statusText";
	
	public static final String STATUS_TYPE = "statusType";

	public static final String HIKE_JOIN_TIME = "hikeJoinTime";

	public static final String SHOW_IN_TIMELINE = "showInTimeline";

	public static final String MOOD_ID = "moodId";

	public static final String TIME_OF_DAY = "timeOfDay";

	public static final String IS_STATUS_MSG = "isStatusMsg";

	public static final String STATUS_INDEX = "statusIdx";

	public static final String USER_INDEX = "userIdx";

	public static final String THUMBNAIL_INDEX = "thumbnailIdx";

	public static final String FAVORITE_INDEX = "favoriteIdx";

	public static final String IS_HIKE_MESSAGE = "isHikeMessage";

	public static final String STICKER_CATEGORIES_TABLE = "stickerCategoriesTable";
	
	public static final String CATEGORY_ID = "categoryId";

	public static final String TOTAL_NUMBER = "totalNum";

	public static final String UPDATE_AVAILABLE = "updateAvailable";

	public static final String LAST_SEEN = "lastSeen";

	public static final String PROTIP_TABLE = "protipTable";

	public static final String PROTIP_MAPPED_ID = "protipMappedId";

	public static final String HEADER = "header";

	public static final String PROTIP_TEXT = "protipText";

	public static final String IMAGE_URL = "imageUrl";

	public static final String WAIT_TIME = "waitTime";

	public static final String PROTIP_GAMING_DOWNLOAD_URL = "url";

	public static final String IS_OFFLINE = "isOffline";

	public static final String UNREAD_COUNT = "unreadCount";

	public static final String SHARED_MEDIA_TABLE = "sharedMediaTable";

	public static final String FILE_THUMBNAIL_TABLE = "fileThumbnailTable";

	public static final String READ_BY = "readBy";
	
	public static final String READ = "read";

	public static final String ROUNDED_THUMBNAIL_TABLE = "roundedThumbnailTable";

	public static final String ROUNDED_THUMBNAIL_INDEX = "roundedThumbnailIndex";

	public static final String FILE_THUMBNAIL_INDEX = "fileThumbnailIndex";

	public static final String INVITE_TIMESTAMP = "inviteTimestamp";

	public static final String PLATFORM_USER_ID = "platformUserId";

	public static final String CHAT_BG_TABLE = "chatBgTable";

	public static final String BG_ID = "bgId";

	public static final String CHAT_BG_INDEX = "chatBgIndex";

	public static final String IS_STEALTH = "isStealth";
	
	public static final String MESSAGE_HASH_INDEX = "messageHashIndex";

	public static final String HIKE_FILE_TYPE = "hikeFileType";

	public static final String _ID = "_id";
	
	public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";
	public static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS ";
	
	public static final String IS_SENT = "isSent";
	public static final String BOT_TABLE = "botTable";

	public static final String BOT_TYPE = "type";

	public static final String BOT_CONFIGURATION = "config";

	public static final String CONFIG_DATA = "config_data";

	public static final String IS_BOT_ENABLE = "bot_enabled";

	public static final String MESSAGE_EVENT_TABLE = "messageEventTable";

	public static final String EVENT_ID = "eventId";

	public static final String EVENT_HASH = "eventHash";

	public static final String EVENT_STATUS = "eventStatus";

	public static final String MAPPED_EVENT_ID = "mappedEventId";

	public static final String EVENT_HASH_INDEX = "eventHashIndex";

	public static final String EVENT_METADATA = "eventMetadata";

	public static final String EVENT_TYPE = "eventType";

	// ActionsTable
	public static final String ACTIONS_TABLE = "actions";

	public static final String ACTION_OBJECT_TYPE = "obj_type";

	public static final String ACTION_OBJECT_ID = "obj_id";

	public static final String ACTION_ID = "action_id";

	public static final String ACTION_COUNT = "action_count";

	public static final String ACTORS = "actors";

	public static final String ACTION_METADATA = "md";

	public static final String ACTION_LAST_UPDATE = "last_update";

	// FeedTable
	public static final String FEED_TABLE = "feed";

	public static final String FEED_OBJECT_TYPE = "feed_type";

	public static final String FEED_OBJECT_ID = "obj_id";

	public static final String FEED_ACTION_ID = "feed_id";

	public static final String FEED_ACTOR = "actor";

	public static final String FEED_METADATA = "md";

	public static final String FEED_TS = "ts";

	//StickerTable

	public static final String STICKER_TABLE = "sticker_table";

	public static final String STICKER_ID = "st_id";

	public static final String SMALL_STICKER_PATH = "sm_st_path";

	public static final String LARGE_STICKER_PATH = "lr_st_path";

	public static final String WIDTH = "width";

	public static final String HEIGHT = "height";

	public static final String FREQUENCY_FUNCTION = "frequency_function";

	public static final String RELEVANCE_FUNCTION = "relevance_function";

	public static final String LANGUAGE = "language";

	public static final String AGE = "age";

	public static final String IS_ACTIVE = "is_active";

	public static final int DEFAULT_ACTIVE_STATE = 1;

	public static final int DEFAULT_IN_ACTIVE_STATE = 0;

	public static interface HIKE_CONV_DB
	{
		// CHANNEL TABLE -> _id,channel_id,name,visibility,index
		public static final String CHANNEL_TABLE = "channel";
		public static final String CHANNEL_ID = "channel_id";
		public static final String CHANNEL_NAME = "name";
		public static final String VISIILITY = "visibility";
		public static final String INDEX_ORDER = "index";
		// CHANNEL TABLE ENDS HERE
		// LOVE TABLE -> _id,love_id,count,user_status,ref_count,timestamp
		//DEPRECATED
		public static final String LOVE_TABLE = "love";
		public static final String LOVE_ID = "love_id";
		public static final String COUNT = "count";
		public static final String USER_STATUS = "user_status";
		public static final String REF_COUNT = "ref_count";
		public static final String TIMESTAMP = "timestamp";
		// LOVE TABLE ENDS HERE
		// MESSAGE TABLE
		public static final String LOVE_ID_REL = "love_id";
		// MESSAGE TABLE ENDS HERE
	}
	/**
	 * 
	 * @author gauravKhanna
	 *
	 */
	public static interface HIKE_CONTENT{
		public static final int DB_VERSION = 6;
		public static final String DB_NAME = "hike_content_db";
		// CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		public static final String CONTENT_TABLE = "content";
		public static final String CONTENT_ID = "content_id";
		public static final String LOVE_ID = "love_id";
		public static final String CHANNEL_ID = "channel_id";
		public static final String TIMESTAMP = "timestamp";
		public static final String METADATA = "metadata";
		
		//ALARM TABLE->id,time,willWakeCpu,time,intent
		
		public static final String ALARM_MGR_TABLE = "HikeAlaMge";

		public static final String TIME = "time";

		public static final String WILL_WAKE_CPU = "willwakecpu";

		public static final String INTENT = "intent";

		// CONTENT TABLE ENDS HERE
		// APP DATA TABLE
		
		// APP DATA TABLE ENDS HERE
		// APP ALARM -> id, data 
		public static final String APP_ALARM_TABLE = "app_alarms";
		public static final String ID = "id";
		public static final String ALARM_DATA = "data";
		// APP ALARM ENDS HERE

		// PopupDB -->json,start time,end time ,trigger point,Status

		public static final String POPUPDATA = "popupdata";

		public static final String START_TIME = "start_time";

		public static final String END_TIME = "end_time";

		public static final String TRIGGER_POINT = "trigger_point";

		public static final String STATUS = "status";
		
		// URL WHITELIST TABLE --> domain, in_hike
		public static final String URL_WHITELIST = "url_whitelist";
		
		public static final String DOMAIN = "domain";
		
		public static final String IN_HIKE = "in_hike"; 
		// URL Whitelist ends here
		
		public static final String CONTENT_ID_INDEX = "contentTableContentIdIndex";
		public static final String CONTENT_TABLE_NAMESPACE_INDEX = "contentTableNamespaceIndex";
		String NAMESPACE = "nameSpace";
		public static final String NOTIF_DATA = "notif_data";
		public static final String VALUE = "value";
		public static final String CONTENT_CACHE_TABLE = "contentCache";
		public static final String KEY = "key";
		public static final String HELPER_DATA = "helper_data";
		
		public static final String BOT_VERSION = "version";
		
		//Bot Discrovery Table starts here
		//Bot Discovery Table --> _id , msisdn, name, type, description, updated_version
		
		public static final String BOT_DESCRIPTION = "description";
		
		public static final String UPDATED_VERSION = "u_v";
		
		public static final String BOT_DISCOVERY_TABLE = "bot_discovery";
		// --- Bot Discovery Table ends here ---
	}
	
	public static interface HIKE_PERSISTENCE
	{
		public static final String DATABASE_NAME = "mqttpersistence";

		public static final int DATABASE_VERSION = 3;

		public static final String MQTT_DATABASE_TABLE = "messages";

		public static final String MQTT_MESSAGE_ID = "msgId";

		public static final String MQTT_PACKET_ID = "mqttId";
		
		public static final String MQTT_PACKET_TYPE = "mqttType";

		public static final String MQTT_MESSAGE = "data";

		public static final String MQTT_MSG_ID_INDEX = "mqttMsgIdIndex";

		public static final String MQTT_TIME_STAMP = "mqttTimeStamp";

		//Added for Instrumentation
		public static final String MQTT_MSG_TRACK_ID = "mqttMsgTrackId";
		
		//Added for Instrumentation
		public static final String MQTT_MSG_MSG_TYPE = "mqttMsgMsgType";
		
		public static final String MQTT_TIME_STAMP_INDEX = "mqttTimeStampIndex";

		public static final String OFFLINE_DATABASE_TABLE = "offlineMessages";

		public static final String OFFLINE_MESSAGE_ID = "msgId";
		
		public static final String OFFLINE_MESSAGE = "data";
		
		public static final String OFFLINE_TIME_STAMP = "offlineTimeStamp";

		public static final String OFFLINE_MSISDN = "offlineMsisdn";
		
		public static final String OFFLINE_PACKET_ID = "offlineId";

		//Added for Instrumentation
		public static final String OFFLINE_MSG_TRACK_ID = "offlineMsgTrackId";
		
		public static final String OFFLINE_MSG_ID_INDEX = "offlineMsgIdIndex";
		
		public static final String OFFLINE_TIME_STAMP_INDEX = "offlineTimeStampIndex";
	}

	public static final String CATEGORY_NAME = "categoryName";

	public static final String IS_VISIBLE = "isVisible";

	public static final String IS_CUSTOM = "isCustom";

	public static final String IS_ADDED = "isAdded";

	public static final String CATEGORY_INDEX = "catIndex";

	public static final String CATEGORY_SIZE = "categorySize";

	public static final String STICKER_SHOP_TABLE = "stickerShopTable";
	
	public static final String MESSAGE_TABLE_CONTENT_INDEX = "messageContentIndex";
	public static final String SERVER_ID = "serverId";
	
	public static final String MESSAGE_ORIGIN_TYPE = "messageOriginType";
	
	public static final int NORMAL_TYPE = 0;
	
	public static final int BROADCAST_TYPE = 1;

	//We are just using a different name for old timestamp field here.
	public static final String LAST_MESSAGE_TIMESTAMP = "timestamp";

	public static final String SORTING_TIMESTAMP = "sortingTimeStamp";

	public static final String SERVER_ID_INDEX = "serverid_idx";

	public static final String PRIVATE_DATA = "pd";

	public static final String MESSAGE_TABLE_NAMESPACE_INDEX = "messageNamespaceIndex";

	public static final String SEND_TIMESTAMP = "sendTimestamp";
	
	/**
	 * Introduced in ConvDb v44, this column will be used henceforth for sorting the messages pertaining to a single conversation
	 */
	public static final String SORTING_ID = "sortingId";

	/**
	 * Introduced in db version 47, this index is used for optimizing the query for fetching messages from messages table. This index consists of msisdn and sortId
	 */
	public static final String SORT_ID_COMPOSITE_IDX = "srtIdx";

	/**
	 * Introduced in db version 47, this index is used for optimizing the query for fetching max value of sortId from messages table. The max query is used frequently, hence the need for index
	 */

	public static final String SORT_ID_SINGLE_IDX = "srt_Index";
	
}
