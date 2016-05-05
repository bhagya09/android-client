package com.bsb.hike.db;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.DBConstants.HIKE_CONTENT;
import com.bsb.hike.db.DatabaseErrorHandlers.CustomDatabaseErrorHandler;
import com.bsb.hike.db.dbcommand.SetPragmaModeCommand;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.WhitelistDomain;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.productpopup.AtomicTipContentModel;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeContentDatabase extends SQLiteOpenHelper implements DBConstants, HIKE_CONTENT
{

	private static final HikeContentDatabase hikeContentDatabase=new HikeContentDatabase();

	SQLiteDatabase mDB;

	private HikeContentDatabase()
	{
		super(HikeMessengerApp.getInstance().getApplicationContext(), DB_NAME, null, DB_VERSION, new CustomDatabaseErrorHandler());
		mDB = getWritableDatabase();
		SetPragmaModeCommand setPragmaModeCommand = new SetPragmaModeCommand(mDB);
		setPragmaModeCommand.execute();
	}

	public static HikeContentDatabase getInstance()
	{
		return hikeContentDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String[] createQueries = getCreateQueries();
		for (String create : createQueries)
		{
			db.execSQL(create);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if(mDB == null)
		{
			mDB = db;
		}
		// CREATE all tables, it is possible that few tables are created in this version
		String[] updateQueries = getUpdateQueries(oldVersion, newVersion);

		ArrayList<JSONObject> contentCacheTable = null;

		//migration is needed only for db version 4 as the table was added in db version 4. We are creating a new table with uniqueness based on
		//key and namespace for which we needed to drop the earlier table and create a new one with new uniqueness. If any version other than 4 say
		//version 3 wants to get this data for migration, the app will crash as it does not havs the table yet. After getting the table data, the
		//content cache table is dropped, new table is created and data is migrated.
		if (oldVersion == 4)
		{
			contentCacheTable = getContentCacheTableForMigration();
		}
		for (String update : updateQueries)
		{
			db.execSQL(update);
		}

		if (oldVersion == 4)
		{
			migrateToNewContentCacheTable(contentCacheTable);
		}

		// DO any other update operation here
	}

	private String[] getCreateQueries()
	{
		String[] createAndIndexes = new String[12];
		int i = 0;
		// CREATE TABLE
		// CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		String contentTable = CREATE_TABLE +CONTENT_TABLE
				+ "("
				+_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ CONTENT_ID+" INTEGER UNIQUE, "
				+ NAMESPACE + " TEXT, "
				+ LOVE_ID+ " INTEGER, "
				+CHANNEL_ID+" INTEGER, "
				+HIKE_CONTENT.TIMESTAMP+" INTEGER, "
				+METADATA+" TEXT"
				+")";

		createAndIndexes[i++] = contentTable;
		//CREATE TABLE 
		
		//ALARM TABLE->id,time,willWakeCpu,time,intent
		
		String alarmTable = CREATE_TABLE + ALARM_MGR_TABLE 
				+ "("
				+ _ID + " INTEGER PRIMARY KEY, "
				+ TIME + " TEXT, "
				+ WILL_WAKE_CPU + " INTEGER, "
				+ INTENT + " TEXT," 
				+ HIKE_CONV_DB.TIMESTAMP + " INTEGER" + ")";
		createAndIndexes[i++]=alarmTable;

		String popupDB = CREATE_TABLE + POPUPDATA + "("
				  +_ID +" INTEGER PRIMARY KEY ,"
				  + POPUPDATA + " TEXT ," 
				  + STATUS + " INTEGER ," 
				  + START_TIME + " INTEGER," 
				  + END_TIME + " INTEGER," 
				  + TRIGGER_POINT + " INTEGER " + ")";
		// URL_WHITELIST_TABLE
		String urlWhitelistTable = CREATE_TABLE + URL_WHITELIST + "(" 
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ DOMAIN + " TEXT UNIQUE, "
				+ IN_HIKE + " INTEGER" + ")";
		createAndIndexes[i++]= urlWhitelistTable;
		// URL WHITELIST ENDS
		
		// Auth_TABLE
				String authTable = CREATE_TABLE + AUTH_TABLE + "(" 
						+ MICROAPP_ID + " TEXT PRIMARY KEY, " 
						+ TOKEN + " TEXT "
					 + ")";
				createAndIndexes[i++]= authTable;
		// Auth Table ENDS
		

        //CREATE MAPP_TABLE
        String mAppTable = CREATE_TABLE + MAPP_DATA + "("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT UNIQUE, "
                + VERSION + " INTEGER, "
                + APP_PACKAGE + " TEXT" + ")";
        createAndIndexes[i++]= mAppTable;

		//CREATE ATOMIC_TIP_TABLE
		createAndIndexes[i++] = getAtomicTipTableCreateQuery();

		String contentIndex = CREATE_INDEX + CONTENT_ID_INDEX + " ON " + CONTENT_TABLE + " (" + CONTENT_ID + ")";
		
		createAndIndexes[i++] = popupDB;

		String nameSpaceIndex = CREATE_INDEX + CONTENT_TABLE_NAMESPACE_INDEX + " ON " + CONTENT_TABLE + " (" + NAMESPACE + ")";

		
		createAndIndexes[i++] = contentIndex;
		createAndIndexes[i++] = nameSpaceIndex;

		String cacheDataTable = contentCacheTableCreateStatement();

		createAndIndexes[i++] = cacheDataTable;
		
		createAndIndexes[i++] = getCreateBotDiscoveryTableQuery();

		createAndIndexes[i++] = getPlatformDownloadStateTableQuery();
		// INDEX ENDS HERE

		return createAndIndexes;
	}

	private String[] getUpdateQueries(int oldVersion, int newVersion)
	{
		ArrayList<String> queries = new ArrayList<String>();
		// UPDATE TABLE
		if (oldVersion< 2)
		{
			String popupDB = CREATE_TABLE + POPUPDATA + "("
					+_ID +" INTEGER PRIMARY KEY ,"
					+ POPUPDATA + " TEXT ,"
					+ STATUS + " INTEGER ,"
					+ START_TIME + " INTEGER,"
					+ END_TIME + " INTEGER,"
					+ TRIGGER_POINT + " INTEGER " + ")";

			queries.add(popupDB);


			String alterNamespace = "ALTER TABLE " + CONTENT_TABLE + " ADD COLUMN " + NAMESPACE + " TEXT";
			queries.add(alterNamespace);

			String nameSpaceIndex = CREATE_INDEX + CONTENT_TABLE_NAMESPACE_INDEX + " ON " + CONTENT_TABLE + " (" + NAMESPACE + ")";
			queries.add(nameSpaceIndex);

		}
		if(oldVersion < 3)
		{
			// URL_WHITELIST_TABLE
			String urlWhitelistTable = CREATE_TABLE + URL_WHITELIST + "(" 
					+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
					+ DOMAIN + " TEXT UNIQUE, "
					+ IN_HIKE + " INTEGER" + ")";
			queries.add(urlWhitelistTable);
		}
		if (oldVersion < 4)
		{
			String cacheDataTable = CREATE_TABLE + CONTENT_CACHE_TABLE
					+ "("
					+_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ KEY + " TEXT UNIQUE, "
					+ VALUE + " TEXT, "
					+ NAMESPACE + " TEXT "
					+ ")";
			queries.add(cacheDataTable);
		}

		if (oldVersion < 5)
		{

			String drop = "DROP TABLE IF EXISTS " + HIKE_CONTENT.CONTENT_CACHE_TABLE;
			String cacheDataTable = contentCacheTableCreateStatement();
			queries.add(drop);
			queries.add(cacheDataTable);

		}
		
		if (oldVersion < 6)
		{
			String createBotDiscoveryQuery = getCreateBotDiscoveryTableQuery();
			queries.add(createBotDiscoveryQuery);
		}
		if(oldVersion < 7)
		{
			//Auth_Table
			String authTable = CREATE_TABLE + AUTH_TABLE + "(" 
					+ MICROAPP_ID + " TEXT PRIMARY KEY, " 
					+ TOKEN + " TEXT "
				 + ")";
			queries.add(authTable);

            String createMappTableQuery = getCreateMAppDataTableQuery();
			String botDownloadStateTableQuery = getPlatformDownloadStateTableQuery();
			queries.add(botDownloadStateTableQuery);
            queries.add(createMappTableQuery);
        }
		if(oldVersion < 8)
		{
			String alterNamespace = "ALTER TABLE " + PLATFORM_DOWNLOAD_STATE_TABLE + " ADD COLUMN " + AUTO_RESUME + " INTEGER";
			queries.add(alterNamespace);
			String atomicTipTableCreateQuery = getAtomicTipTableCreateQuery();
			queries.add(atomicTipTableCreateQuery);
		}
		
		return queries.toArray(new String[]{});
	}

	private String contentCacheTableCreateStatement()
	{
		return CREATE_TABLE +CONTENT_CACHE_TABLE
						+ "("
						+_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ KEY + " TEXT, "
						+ VALUE + " TEXT, "
						+ NAMESPACE + " TEXT, "
						+ "UNIQUE ("
						+ KEY + "," + NAMESPACE
						+ ") ON CONFLICT REPLACE"
						+ ")";
	}

	/**
	 * CALLED ONLY ONCE....!!
	 * This function is called to migrate the data from incorrect contentCacheTable to the correct packet
	 * @param contentCacheTable
	 */
	private void migrateToNewContentCacheTable(ArrayList contentCacheTable)
	{
		for (Object obj : contentCacheTable)
		{
			JSONObject cacheRow = (JSONObject) obj;
			String key = cacheRow.optString(HIKE_CONTENT.KEY);
			String value = cacheRow.optString(HIKE_CONTENT.VALUE);
			String namespace = cacheRow.optString(HIKE_CONTENT.NAMESPACE);
			putInContentCache(key, namespace, value);
		}
	}

	public void insertIntoAlarmManagerDB(long time, int requestCode, boolean WillWakeCPU, Intent intent)
	{
		ContentValues cv = new ContentValues();
		cv.put(_ID, requestCode);
		cv.put(TIME, time + "");
		cv.put(WILL_WAKE_CPU, WillWakeCPU);
		cv.put(INTENT, intent.toUri(0));

		mDB.insertWithOnConflict(ALARM_MGR_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void deleteFromAlarmManagerDB(int requestCode)
	{
		mDB.delete(ALARM_MGR_TABLE, _ID + "=" + requestCode, null);
	}

	public void rePopulateAlarmWhenClosed()
	{
		Logger.d(HikeAlarmManager.TAG, "Populating alarm started");
		String selectQuery = "SELECT  * FROM " + ALARM_MGR_TABLE;

		Cursor cursor = mDB.rawQuery(selectQuery, null);
		try
		{
			if (cursor.moveToFirst())
			{
				do
				{
					Logger.d(HikeAlarmManager.TAG, "rePopulating  Alarms");
					int requestCode = cursor.getInt(cursor.getColumnIndex(_ID));
					long time = Long.parseLong(cursor.getString(cursor.getColumnIndex(TIME)));
					int willWakeCpu = cursor.getInt(cursor.getColumnIndex(WILL_WAKE_CPU));
					String intent = cursor.getString(cursor.getColumnIndex(INTENT));
					Uri asd = Uri.parse(intent);

					Intent intentAlarm = Intent.getIntent(asd.toString());

					HikeAlarmManager.setAlarmWithIntent(HikeMessengerApp.getInstance(), time, requestCode, (willWakeCpu != 0), intentAlarm);

				}
				while (cursor.moveToNext());
			}
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param pkt
	 * @param notifTime
	 * @param TriggerPoint
	 * 
	 *            Saving the popUp in the database
	 */
	public void savePopup(ProductContentModel productContentModel, int status)
	{
		ContentValues cv = new ContentValues();
		cv.put(POPUPDATA, productContentModel.toJSONString());
		cv.put(STATUS, status);
		cv.put(START_TIME, productContentModel.getStarttime());
		cv.put(END_TIME, productContentModel.getEndtime());
		cv.put(TRIGGER_POINT, productContentModel.getTriggerpoint());
		cv.put(_ID, productContentModel.hashCode());
		productContentModel = getPopupFromId(productContentModel.hashCode());
		if (productContentModel != null)
		{
			ProductInfoManager.recordPopupEvent(productContentModel.getAppName(), productContentModel.getPid(), productContentModel.isFullScreen(),
					ProductPopupsConstants.RECEIVED_NOT_SHOWN);
		}
		long val = mDB.insertWithOnConflict(POPUPDATA, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		Logger.d("ProductPopup", "DB Inserted Successfully..." + val + "");
	}

	private ProductContentModel getPopupFromId(int id)
	{
		ProductContentModel productContentModel=null;
		Cursor c = null;
		try
		{
			String query = "select * from "+POPUPDATA +" where _id = "+ id;

			c = mDB.rawQuery(query, null);

			if (c.moveToFirst())
			{
				do
				{
					int triggerPoint = c.getInt(c.getColumnIndex(TRIGGER_POINT));
					int startTime = c.getInt(c.getColumnIndex(START_TIME));
					String json = c.getString(c.getColumnIndex(POPUPDATA));
					int endTime = c.getInt(c.getColumnIndex(END_TIME));
					productContentModel = ProductContentModel.makeProductContentModel(new JSONObject(json));
					Logger.d("ProductPopup>", triggerPoint + " >" + startTime + ">>>" + endTime);

				}
				while ((c.moveToNext()));
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}
		return productContentModel;
	}

	/**
	 * 
	 * @return
	 * 
	 * This method is responsible for getting the popup data from the DB to memory.
	 * 
	 * This is called only once from the the HikeMessageApp (onCreate)
	 */
	public SparseArray<ArrayList<ProductContentModel>> getAllPopup()
	{
		Logger.d("ProductPopup", "getAllPopup\n");
		SparseArray<ArrayList<ProductContentModel>> mmSparseArray = new SparseArray<ArrayList<ProductContentModel>>();
		JSONObject productPopupModel=null;
		Cursor c=null;
		ArrayList<ProductContentModel> mmArray=new ArrayList<ProductContentModel>();
		try
		{
			String query = "select * from "+POPUPDATA +" order by "+ TRIGGER_POINT;
			
			c = mDB.rawQuery(query, null);

			if (c.moveToFirst())
			{
				do
				{
					int triggerPoint = c.getInt(c.getColumnIndex(TRIGGER_POINT));
					int startTime = c.getInt(c.getColumnIndex(START_TIME));
					String json = c.getString(c.getColumnIndex(POPUPDATA));
					int endTime = c.getInt(c.getColumnIndex(END_TIME));
					productPopupModel = new JSONObject(json);

					if (mmSparseArray.get(triggerPoint) == null)
					{
						mmArray = new ArrayList<ProductContentModel>();
						mmArray.add(ProductContentModel.makeProductContentModel(productPopupModel));
						mmSparseArray.put(triggerPoint, mmArray);
					}
					else
					{
						mmArray = mmSparseArray.get(triggerPoint);
						mmArray.add(ProductContentModel.makeProductContentModel(productPopupModel));
						mmSparseArray.put(triggerPoint, mmArray);
					}
					Logger.d("ProductPopup>", triggerPoint + " >" + startTime + ">>>" + endTime);

				}
				while ((c.moveToNext()));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}

		return mmSparseArray;
	}

	
	/**
	 * 
	 * @param args
	 * 
	 * Deleting the Popup from the Database
	 */
	public void deletePopup(String[] args)
	{	
			String id=TextUtils.join(", ", args);
			Logger.d("ProductPopup","ids deletd are "+id+"<<<<<command to be excetuing"+String.format("DELETE FROM " + POPUPDATA + " WHERE "+ _ID+ " IN ( "+ id + ")" ));
			mDB.execSQL(String.format("DELETE FROM " + POPUPDATA + " WHERE "+ _ID+ " IN ( "+ id + ")"));
	}
	
	/**
	 * 
	 * @param hashcode
	 * @param status
	 * 
	 * Updating the status of the Popup.
	 */
	public void updatePopupStatus(int hashcode, int status)
	{
		ContentValues cv = new ContentValues();
		cv.put(STATUS, status);
		mDB.update(POPUPDATA, cv, _ID + "= " + hashcode, null);
	}
	
	/**
	 * This method returns whether this domain is whitelisted OR not to open in hike. If this domain is whitelisted, this method returns {@link WhitelistDomain} with domain name and
	 * {@link WhitelistDomain#WHITELISTED_IN_BROWSER} is to open in browser and {@link WhitelistDomain#WHITELISTED_IN_HIKE} is to open in hike. if it is not whitelisted it returns
	 * null
	 * 
	 * @param url
	 *            - url to check in whitelist domain, note : it should be full URL e.g http://www.hike.in
	 * @return
	 */
	public WhitelistDomain getWhitelistedDomain(String url)
	{
		WhitelistDomain whitelistDomain = new WhitelistDomain(url, WhitelistDomain.WHITELISTED_IN_HIKE);
		String domain = whitelistDomain.getDomain();
		Logger.d("whitelist", "url to check is " + url + " and domain is " + whitelistDomain.getDomain());
		for(String validDomains : HikeConstants.WHITELISTED_DOMAINS)
		{
			if (domain.matches(".*" + validDomains))
			{
				return whitelistDomain;
			}
		}
			whitelistDomain = null;
			// querying all domains and matching one by one with regex
			Cursor c = mDB.query(URL_WHITELIST, new String[] { DOMAIN,IN_HIKE }, null, null, null, null, null);
				while(c.moveToNext())
				{
					String dom = c.getString(c.getColumnIndex(DOMAIN));
					if(domain.matches(".*"+dom))
					{
						whitelistDomain = new WhitelistDomain(url, c.getInt(c.getColumnIndex(IN_HIKE)));
						break;
					}
				}
		return whitelistDomain;
	}

	public void addDomainInWhitelist(WhitelistDomain domain)
	{
		addDomainInWhitelist(new WhitelistDomain[]{domain});
	}

	/**
	 * This method insets all domains in table in a loop.
	 * DO NOT CALL IN UI THREAD
	 * 
	 * @param domains
	 */
	public void addDomainInWhitelist(WhitelistDomain[] domains)
	{
		try
		{
			mDB.beginTransaction();
			for (WhitelistDomain domain : domains)
			{
				ContentValues cv = new ContentValues();
				cv.put(DOMAIN, domain.getDomain());
				cv.put(IN_HIKE, domain.getWhitelistState());
				mDB.insert(URL_WHITELIST, null, cv);
			}
			mDB.setTransactionSuccessful();
		}
		finally
		{
			mDB.endTransaction();
		}

	}
	
	
	public void deleteDomainFromWhitelist(String domain)
	{
		deleteDomainFromWhitelist(new String[] { domain });
	}

	/**
	 * This method will be called rarely, it deletes all rows in loop.
	 * DO NOT CALL IN UI THREAD
	 * 
	 * @param domains
	 */
	public void deleteDomainFromWhitelist(String[] domains)
	{
		
		String whereClause = DOMAIN + "=?";
		mDB.delete(URL_WHITELIST, whereClause, domains);
	}

	public void deleteAllDomainsFromWhitelist()
	{
		mDB.delete(URL_WHITELIST, null, null);
	}

	public void deleteAllPopupsFromDatabase()
	{
		mDB.delete(POPUPDATA, null, null);
		
	}

	/**
	 * The microapps call this function to put large data in the content cache.
	 * @param key
	 * @param namespace
	 * @param value
	 */
	public void putInContentCache(String key, String namespace, String value)
	{
		if (TextUtils.isEmpty(key) || TextUtils.isEmpty(namespace))
		{
			Logger.e(HikePlatformConstants.TAG, "entries are incorrect. Send correct keys.");
			return;
		}
		ContentValues values = new ContentValues();
		values.put(KEY, key);
		values.put(VALUE, value);
		values.put(NAMESPACE, namespace);

		mDB.insertWithOnConflict(CONTENT_CACHE_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public String getFromContentCache(String key, String namespace)
	{
		if (TextUtils.isEmpty(key) || TextUtils.isEmpty(namespace))
		{
			Logger.e(HikePlatformConstants.TAG, "entries are incorrect. Send correct keys to search for.");
			return "";
		}
		Cursor c = null;
		try
		{
			c = mDB.query(CONTENT_CACHE_TABLE, new String[] { VALUE }, KEY + "=? AND " + NAMESPACE + "=?", new String[] { key, namespace }, null, null, null);
			if (c.moveToFirst())
			{
				int valueIndex = c.getColumnIndex(VALUE);
				String value = c.getString(valueIndex);
				return value;
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
	
	public void deleteAll()
	{
		mDB.delete(CONTENT_TABLE, null, null);
		mDB.delete(ALARM_MGR_TABLE, null, null);
		mDB.delete(CONTENT_CACHE_TABLE, null, null);
        mDB.delete(MAPP_DATA, null, null);
		mDB.delete(PLATFORM_DOWNLOAD_STATE_TABLE,null,null);
		ProductInfoManager.getInstance().deleteAllPopups();
		deleteAllDomainsFromWhitelist();
		flushAtomicTipTable();
	}

	/**
	 * CALLED ONLY ONCE....!!
	 * This method is called to get the data from incorrect table that only had key as the unique column to correct table that has a
	 * uniqueness of a combination of 2 params, key and namespace.
	 * @return : an arraylist containing the data in the incorrect table.
	 */
	public ArrayList<JSONObject> getContentCacheTableForMigration()
	{
		Cursor c = null;
		try
		{
			c = mDB.query(HIKE_CONTENT.CONTENT_CACHE_TABLE, new String[]{ KEY, VALUE, NAMESPACE }, null, null, null, null, null);

			int keyIdx = c.getColumnIndex(HIKE_CONTENT.KEY);
			int valueIdx = c.getColumnIndex(HIKE_CONTENT.VALUE);
			int namespaceIdx = c.getColumnIndex(HIKE_CONTENT.NAMESPACE);

			ArrayList cacheList = new ArrayList();

			while (c.moveToNext())
			{
				String key = c.getString(keyIdx);
				String value = c.getString(valueIdx);
				String namespace = c.getString(namespaceIdx);

				JSONObject obj = new JSONObject();
				obj.put(HIKE_CONTENT.KEY, key);
				obj.put(HIKE_CONTENT.NAMESPACE, namespace);
				obj.put(HIKE_CONTENT.VALUE, value);
				cacheList.add(obj);
			}
			return cacheList;
		}
		catch (JSONException e)
		{
			Logger.e(HikePlatformConstants.TAG, "JSON Exception in migration " + e.toString());
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

	public void deletePartialMicroAppCacheData(String key, String nameSpace)
	{
		String where = HIKE_CONTENT.NAMESPACE + "=? and " + HIKE_CONTENT.KEY + "=?";
		mDB.delete(HIKE_CONTENT.CONTENT_CACHE_TABLE, where , new String[]{nameSpace, key} );
	}

	public void deleteAllMicroAppCacheData(String nameSpace)
	{
		mDB.delete(HIKE_CONTENT.CONTENT_CACHE_TABLE, HIKE_CONTENT.NAMESPACE + "=?" , new String[]{nameSpace} );
	}
	
	private String getCreateBotDiscoveryTableQuery()
	{
		String createBotDiscoveryTable = CREATE_TABLE + DBConstants.HIKE_CONTENT.BOT_DISCOVERY_TABLE + 
				" ("
				+ DBConstants._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ DBConstants.MSISDN + " TEXT UNIQUE, " 
				+ DBConstants.NAME + " TEXT, "
				+ DBConstants.BOT_TYPE + " INTEGER DEFAULT " +  + BotInfo.MESSAGING_BOT + ", "  
				+ DBConstants.HIKE_CONTENT.BOT_DESCRIPTION + " TEXT, "
				+ DBConstants.HIKE_CONTENT.UPDATED_VERSION + " INTEGER DEFAULT 0" 
				+")";

		return createBotDiscoveryTable;
	}
	
	/**
	 * Utility method to get a cursor for bot discovery table
	 * 
	 * @return
	 */
	public Cursor getCursorForBotDiscoveryTable()
	{
		Cursor c = null;
		try
		{
			c = mDB.query(DBConstants.HIKE_CONTENT.BOT_DISCOVERY_TABLE, new String[] { DBConstants._ID, DBConstants.MSISDN, DBConstants.NAME, DBConstants.BOT_TYPE,
					DBConstants.HIKE_CONTENT.BOT_DESCRIPTION, DBConstants.HIKE_CONTENT.UPDATED_VERSION }, null, null, null, null, null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(getClass().getSimpleName(), "Exception in getCursorForBotDiscoveryTable", e);
		}
		return c;
	}
	
	/**
	 * This method is used to flush out the old entries in the bot discovery table
	 */
	public void flushBotDiscoveryTable()
	{
		Logger.v("HikeContentDatabase", "Fluhsing bot discovery table");
		mDB.delete(DBConstants.HIKE_CONTENT.BOT_DISCOVERY_TABLE, null, null);
	}

	public ContentValues getBotDiscoveryTableContentValues(BotInfo botInfo)
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.MSISDN, botInfo.getMsisdn());
		contentValues.put(DBConstants.NAME, botInfo.getConversationName());
		contentValues.put(DBConstants.BOT_TYPE, botInfo.getType());
		contentValues.put(DBConstants.HIKE_CONTENT.BOT_DESCRIPTION, botInfo.getBotDescription());
		contentValues.put(DBConstants.HIKE_CONTENT.UPDATED_VERSION, botInfo.getUpdatedVersion());

		return contentValues;
	}

	private BotInfo createBotInfoFromBotJSON(JSONObject botJSON)
	{
		try
		{
			String msisdn = botJSON.getString(HikePlatformConstants.MSISDN);

			String name = botJSON.getString(HikePlatformConstants.BOT_NAME);

			int botType = (HikeConstants.NON_MESSAGING_BOT.equalsIgnoreCase(botJSON.optString(HikePlatformConstants.BOT_TYPE, HikeConstants.NON_MESSAGING_BOT))) ? BotInfo.NON_MESSAGING_BOT
					: BotInfo.MESSAGING_BOT;

			String description = botJSON.optString(HikePlatformConstants.BOT_DESCRIPTION, "");

			int latestVersion = botJSON.optInt(HikePlatformConstants.BOT_LATEST_VERSION, 0); // TODO Add current version here

            int mAppVersionCode = 0;

            byte requestType = HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS;

            if (botJSON.has(HikePlatformConstants.METADATA))
            {
                JSONObject mdJsonObject = botJSON.optJSONObject(HikePlatformConstants.METADATA);
                JSONObject cardObjectJson = mdJsonObject.optJSONObject(HikePlatformConstants.CARD_OBJECT);
                mAppVersionCode = cardObjectJson.optInt(HikePlatformConstants.MAPP_VERSION_CODE);
                String nonMessagingBotType = cardObjectJson.optString(HikePlatformConstants.NON_MESSAGING_BOT_TYPE,HikePlatformConstants.MICROAPP_MODE);
                if (nonMessagingBotType == HikePlatformConstants.NATIVE_MODE)
                {
                    requestType = HikePlatformConstants.PlatformBotType.NATIVE_APPS;
                }
            }

			BotInfo mBotInfo = new BotInfo.HikeBotBuilder(msisdn).setConvName(name).description(description).setType(botType).setUpdateVersion(latestVersion).setMAppVersionCode(mAppVersionCode).setBotType(requestType).build();

			String thumbnailString = botJSON.optString(HikePlatformConstants.BOT_DP, "");
			
			if (!TextUtils.isEmpty(thumbnailString))
			{
				BotUtils.createAndInsertBotDp(msisdn, thumbnailString);
			}

			return mBotInfo;
		}

		catch (JSONException e)
		{
			Logger.d("HikeContentDatabase", "Got an exception in createBotInfoFromJSON : " + e.toString() + " \n Returning null");
			return null;
		}

	}

	/**
	 * Utility method used to create content values from a given json array for populating bot discovery table
	 * @param botInfoArray
	 * @return
	 */
	private ContentValues[] parseBotInfoArray(JSONArray botInfoArray)
	{
		if (botInfoArray == null || botInfoArray.length() == 0)
		{
			return null;
		}

		ContentValues[] mContentValues = new ContentValues[botInfoArray.length()];
		int k = 0;

		try
		{
			for (int i = 0; i < botInfoArray.length(); i++)
			{
				JSONObject botObj = (JSONObject) botInfoArray.get(i);
				BotInfo mBotInfo = createBotInfoFromBotJSON(botObj);

				if (mBotInfo != null)
				{
					mContentValues[k++] = getBotDiscoveryTableContentValues(mBotInfo);
				}

			}
		}

		catch (JSONException e)
		{
			Logger.e("HikeContentDatabase", "Got an issue for parseBotInfo Array : " + e.toString());
			return null;
		}

		return mContentValues;
	}

	/**
	 * Utility method used to populate bot discovery table
	 * 
	 * @param botInfoArray
	 * @param flushOldData
	 *            - If true, then we eliminate old entries in the bot discovery table
	 */
	public void populateBotDiscoveryTable(JSONArray botInfoArray, boolean flushOldData)
	{
		Logger.v("HikeContentDatabase", "Populating bot discovery table");
		
		if (flushOldData)
		{
			flushBotDiscoveryTable();
		}

		ContentValues[] mContentValues = parseBotInfoArray(botInfoArray);

		if (mContentValues == null || mContentValues.length == 0)
		{
			Logger.e("HikeContentDatabase", "No Content values found to populate HikeContentDatabase");
			return;
		}

		mDB.beginTransaction();

		try
		{

			for (ContentValues contentValues : mContentValues)
			{
				mDB.insert(DBConstants.HIKE_CONTENT.BOT_DISCOVERY_TABLE, null, contentValues);
			}

			Logger.v("HikeContentDatabase", "Bot discovery Table populated");
			mDB.setTransactionSuccessful();

		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Caught Exception while populating bot discovery table : ", e);
		}

		finally
		{
			mDB.endTransaction();
		}

	}
	
	public List<BotInfo> getDiscoveryBotInfoList()
	{
		List<BotInfo> list = new ArrayList<>(0);
		
		Cursor c = getCursorForBotDiscoveryTable();
		String msisdn;
		String name;
		int botType;
		String botDesc;
		int updateVersion;
		
		while (c.moveToNext())
		{
			msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			name = c.getString(c.getColumnIndex(DBConstants.NAME));
			botType = c.getInt(c.getColumnIndex(DBConstants.BOT_TYPE));
			botDesc = c.getString(c.getColumnIndex(DBConstants.HIKE_CONTENT.BOT_DESCRIPTION));
			updateVersion = c.getInt(c.getColumnIndex(DBConstants.HIKE_CONTENT.UPDATED_VERSION));
			
			BotInfo botInfo = new BotInfo.HikeBotBuilder(msisdn).setConvName(name).setType(botType).description(botDesc).setUpdateVersion(updateVersion).build();
			list.add(botInfo);
		}
		
		if (c != null)
		{
			c.close();
		}
		
		return list;
	}
	
	public JSONArray getDiscoveryBotMsisdnArray()
	{
		JSONArray array = new JSONArray();
		
		Cursor c = mDB.query(DBConstants.HIKE_CONTENT.BOT_DISCOVERY_TABLE, new String[] { DBConstants.MSISDN }, null, null, null, null, null);
		String msisdn;
		
		while(c.moveToNext())
		{
			msisdn = c.getString(c.getColumnIndex(DBConstants.MSISDN));
			array.put(msisdn);
		}
		
		if (c != null)
		{
			c.close();
		}
		
		return array;
	}
	
	public String getTokenForMicroapp(String mappId)
	{
		Cursor c = null;
		try
		{
			c = mDB.query(AUTH_TABLE, new String[] { DBConstants.HIKE_CONTENT.TOKEN }, DBConstants.HIKE_CONTENT.MICROAPP_ID + "=?", new String[] { mappId }, null, null, null);
			if (c.moveToFirst())
			{

				return c.getString(c.getColumnIndex(DBConstants.HIKE_CONTENT.TOKEN));

			}
			return null;
		}
		catch(Exception e){
			Logger.d(getClass().getSimpleName(), "Caught Exception while getTokenForMicroapp : "+ e.getMessage());
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
	
	public void addAuthToken(String mId, String token)
	{
		try
		{
			mDB.beginTransaction();
			ContentValues cv = new ContentValues();
				cv.put(DBConstants.HIKE_CONTENT.MICROAPP_ID, mId);
				cv.put(DBConstants.HIKE_CONTENT.TOKEN, token);
				mDB.insert(AUTH_TABLE, null, cv);
			mDB.setTransactionSuccessful();
		}
		finally
		{
			mDB.endTransaction();
		}

	}
	public void deleteMicroAppFromAuthTAble(String[] mAppId)
	{
		
		String whereClause = DBConstants.HIKE_CONTENT.MICROAPP_ID + "=?";
		mDB.delete(AUTH_TABLE, whereClause, mAppId);
	}

    /*
     * Method to insert entry to MApp Data table for each entry
     */
    public void insertIntoMAppDataTable(String mAppName,int version,String appPackageUrl)
    {
        // values to insert into Mapp Data table
        ContentValues cv = new ContentValues();
        cv.put(NAME,mAppName);
        cv.put(VERSION,version);
        cv.put(APP_PACKAGE,appPackageUrl);

        long insertedRow = mDB.insertWithOnConflict(MAPP_DATA, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if(insertedRow > 0)
            HikeMessengerApp.hikeMappInfo.put(mAppName,version);
    }

    /*
     * Method to get mapp version by its app name
     */
    public void initSdkMap()
    {
        Cursor c = mDB.query(DBConstants.HIKE_CONTENT.MAPP_DATA, new String[]{DBConstants.NAME, DBConstants.HIKE_CONTENT.VERSION}, null, null, null, null, null);

        while(c != null && c.moveToNext())
        {
            String appName = c.getString(c.getColumnIndex(DBConstants.NAME));
            int version = c.getInt(c.getColumnIndex(DBConstants.HIKE_CONTENT.VERSION));
            Logger.v("BOT", "Putting sdk Info in hashmap " + appName + version);
            HikeMessengerApp.hikeMappInfo.put(appName,version);
        }

        if (c != null)
        {
            c.close();
        }
    }

    /*
    * Method to get MApp table data create query
    */
    private String getCreateMAppDataTableQuery()
    {
        String mAppTable = CREATE_TABLE + MAPP_DATA + "("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT UNIQUE, "
                + VERSION + " INTEGER, "
                + APP_PACKAGE + " TEXT" + ")";

        return mAppTable;
    }

	/**
	 * This table maintains download state for cbot/mapp packets for Platform.
	 * @return
	 */
	private String getPlatformDownloadStateTableQuery()
	{

		String botDownloadStateTableQuery = CREATE_TABLE + DBConstants.HIKE_CONTENT.PLATFORM_DOWNLOAD_STATE_TABLE +
				" ("
				+ HikePlatformConstants.APP_NAME + " TEXT, "
				+ HikePlatformConstants.PACKET_DATA + " TEXT, "
				+ HikePlatformConstants.MAPP_VERSION_CODE + " INTEGER, "
				+ HikePlatformConstants.TYPE + " INTEGER, "
				+ HikePlatformConstants.TTL + " INTEGER, "
				+ DBConstants.HIKE_CONTENT.DOWNLOAD_STATE + " INTEGER, "
				+ AUTO_RESUME + " INTEGER DEFAULT " + 0 + ", "
				+ HikePlatformConstants.PREF_NETWORK + " INTEGER DEFAULT " + Utils.getNetworkShortinOrder(HikePlatformConstants.DEFULT_NETWORK)+", "
				+ "UNIQUE ("
				+ HikePlatformConstants.APP_NAME + "," + HikePlatformConstants.MAPP_VERSION_CODE
				+ ")"
				+ ")";

		return botDownloadStateTableQuery;
	}
	/**
	 * Function to add data to Platform Download State Table
	 */
	public void addToPlatformDownloadStateTable(String name, int mAppVersionCode, String data, int type, long ttl,int prefNetwork, int dwnldState,int autoResume)
	{
		ContentValues cv = new ContentValues();
		cv.put(HikePlatformConstants.APP_NAME, name);
		cv.put(HikePlatformConstants.MAPP_VERSION_CODE, mAppVersionCode);
		cv.put(HikePlatformConstants.PACKET_DATA, data);
		cv.put(HikePlatformConstants.TYPE, type);
		cv.put(HikePlatformConstants.TTL, ttl);
		cv.put(HikePlatformConstants.PREF_NETWORK, prefNetwork);
		cv.put(DBConstants.HIKE_CONTENT.DOWNLOAD_STATE, dwnldState);
		cv.put(AUTO_RESUME, autoResume);
		try
		{
			mDB.insertWithOnConflict(HIKE_CONTENT.PLATFORM_DOWNLOAD_STATE_TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
		}
		catch (Exception e)
		{
			Logger.d(getClass().getCanonicalName(), "Error while inserting to DB");

		}
	}

	public void removeFromPlatformDownloadStateTable(String name, int mAppVersionCode)
	{
		long rows =mDB.delete(HIKE_CONTENT.PLATFORM_DOWNLOAD_STATE_TABLE, HikePlatformConstants.APP_NAME + " =? AND " + HikePlatformConstants.MAPP_VERSION_CODE + " = " + mAppVersionCode, new String[]{name});
	}

	public void updatePlatformDownloadState(String name, int mAppVersionCode, int newState)
	{
		ContentValues cv = new ContentValues();
		cv.put(DBConstants.HIKE_CONTENT.DOWNLOAD_STATE, newState);
		mDB.update(HIKE_CONTENT.PLATFORM_DOWNLOAD_STATE_TABLE, cv, HikePlatformConstants.APP_NAME + " =? AND " + HikePlatformConstants.MAPP_VERSION_CODE + " = " + mAppVersionCode, new String[]{name});
	}

	/**
	 * Call this method to get the cursor of the PlatformDownloadStateTable.
	 * @return
	 */
	public Cursor getAllPendingPlatformDownloads()
	{
		Cursor c = null;
			c = mDB.query(HIKE_CONTENT.PLATFORM_DOWNLOAD_STATE_TABLE,null, null, null, null, null, null);
		return c;
	}

	/**
	 * Method to delete all entries from Platform Download State Table
	 */
	public void flushPlatformDownloadStateTable()
	{
		Logger.v("HikeContentDatabase", "Fluhsing Download state table");
		mDB.delete(PLATFORM_DOWNLOAD_STATE_TABLE, null, null);
	}
	/*
	Method to check if a microapp download is in progress
	@param appName whose progress is to be checked .
	 */
	public boolean isMicroAppDownloadRunning(String appName)
	{
		if (TextUtils.isEmpty(appName))
		{
			Logger.e(HikePlatformConstants.TAG, "entries are incorrect. Send correct keys to search for.");
			return false;
		}
		Cursor c = null;
		try
		{
			c = mDB.query(PLATFORM_DOWNLOAD_STATE_TABLE, new String[] { DOWNLOAD_STATE }, HikePlatformConstants.APP_NAME + "=?", new String[] {appName}, null, null, null);
			if (c.moveToFirst())
			{
				int downloadStateIndex = c.getColumnIndex(DOWNLOAD_STATE);
				int value = c.getInt(downloadStateIndex);
				if(value == (HikePlatformConstants.PlatformDwnldState.IN_PROGRESS))
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

	/**
	 * Method to frame and return create table query for atomic tips table
	 * @return
     */
	public String getAtomicTipTableCreateQuery()
	{
		String atomicTipTableCreateQuery = CREATE_TABLE + ATOMIC_TIP_TABLE + "("
				+_ID +" INTEGER PRIMARY KEY ,"
				+ TIP_DATA + " TEXT,"
				+ TIP_STATUS + " INTEGER,"
				+ TIP_PRIORITY + " INTEGER,"
				+ TIP_END_TIME + " INTEGER" + ")";

		return atomicTipTableCreateQuery;
	}

	/**
	 * Method to insert atomic tips received via mqtt into Content DB.
	 * @param tipContentModel
	 * @param tipStatus
     */
	public void saveAtomicTip(AtomicTipContentModel tipContentModel, int tipStatus)
	{
		Logger.d(getClass().getSimpleName(), "Saving new atomic tip");
		ContentValues cv = new ContentValues();
		cv.put(_ID, tipContentModel.hashCode());
		cv.put(TIP_DATA, tipContentModel.getJsonString());
		cv.put(TIP_STATUS, tipStatus);
		cv.put(TIP_PRIORITY, tipContentModel.getPriority());
		cv.put(TIP_END_TIME, tipContentModel.getEndTime());
		long row = mDB.insertWithOnConflict(ATOMIC_TIP_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		Logger.d(getClass().getSimpleName(), "Atomic Tip insertion success: " + row);
	}

	/**
	 * Method to get list of all saved atomic tips in ascending order of status and tip priority
	 * @return
     */
	public List<AtomicTipContentModel> getSavedAtomicTips()
	{
		Logger.d(getClass().getSimpleName(), "Fetching saved atomic tips");
		//first cleaning up tables to remove expired tips
		cleanAtomicTipsTable();
		List<AtomicTipContentModel> atomicTipContentModels = new ArrayList<>();
		Cursor c = null;
		try
		{
			String query = "SELECT * FROM " + ATOMIC_TIP_TABLE + " ORDER BY " + TIP_STATUS + " ASC, " +TIP_PRIORITY + " ASC";

			c = mDB.rawQuery(query, null);

			Logger.d(getClass().getSimpleName(), "atomic tips table cursor size = "+c.getCount());

			while (c.moveToNext())
			{
				String tipJSON = c.getString(c.getColumnIndex(TIP_DATA));
				@AtomicTipContentModel.Status int tipStatus = c.getInt(c.getColumnIndex(TIP_STATUS));
				AtomicTipContentModel tipContentModel = AtomicTipContentModel.getAtomicTipContentModel(new JSONObject(tipJSON));
				tipContentModel.setTipStatus(tipStatus);
				atomicTipContentModels.add(tipContentModel);
			}
		}
		catch (JSONException jse)
		{
			Logger.d(getClass().getSimpleName(), "JSONException while fetching Atomic Tips from Content DB");
		}
		finally
		{
			if(c != null)
			{
				c.close();
			}
		}
		return atomicTipContentModels;
	}

	/**
	 * Method to update status of an atomic tip
	 * @param tipId
	 * @param tipStatus
     */
	public void updateAtomicTipStatus(int tipId, int tipStatus)
	{
		Logger.d(getClass().getSimpleName(), "Updating atomic tip status");
		ContentValues cv = new ContentValues();
		cv.put(TIP_STATUS, tipStatus);
		String whereClause = _ID + "=" + tipId;
		mDB.update(ATOMIC_TIP_TABLE, cv, whereClause, null);
	}

	/**
	 * Method to clean atomic tips table by deleting dismissed & expired tips.
	 */
	public void cleanAtomicTipsTable()
	{
		Logger.d(getClass().getSimpleName(), "Deleting dismissed and expired atomic tips from table.");
		String dismissedClause = TIP_STATUS + "=" + AtomicTipContentModel.DISMISSED;
		String expiredClause = " OR "+TIP_END_TIME+ "<" + System.currentTimeMillis();
		String whereClause = dismissedClause + expiredClause;
		int result = mDB.delete(ATOMIC_TIP_TABLE, whereClause, null);
		Logger.d(getClass().getSimpleName(), "number of cleaned rows from atomic tip table: "+result);
	}

	/**
	 * Method to flush atomic tips table.
	 */
	public void flushAtomicTipTable()
	{
		Logger.d(getClass().getSimpleName(), "Flushing atomic tip table.");
		mDB.delete(ATOMIC_TIP_TABLE, null, null);
	}

}
