package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class HikeUserDatabase extends SQLiteOpenHelper {
	private SQLiteDatabase mDb;

	private SQLiteDatabase mReadDb;

	private Context mContext;

	private static HikeUserDatabase hikeUserDatabase;

	public static void init(Context context) {
		if (hikeUserDatabase == null) {
			hikeUserDatabase = new HikeUserDatabase(context);
		}
	}

	public static HikeUserDatabase getInstance() {
		return hikeUserDatabase;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = "CREATE TABLE IF NOT EXISTS " + DBConstants.USERS_TABLE
				+ " ( " + DBConstants.ID + " STRING , " + DBConstants.NAME
				+ " TEXT, " + DBConstants.MSISDN + " TEXT COLLATE nocase, "
				+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE
				+ " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
				+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
				+ DBConstants.MSISDN_TYPE + " STRING, "
				+ DBConstants.LAST_MESSAGED + " INTEGER, "
				+ DBConstants.FAVORITE + " INTEGER DEFAULT "
				+ FavoriteType.NOT_FAVORITE.ordinal() + " )";

		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.BLOCK_TABLE
				+ " ( " + DBConstants.MSISDN + " TEXT " + " ) ";
		db.execSQL(create);

		create = "CREATE TABLE IF NOT EXISTS " + DBConstants.THUMBNAILS_TABLE
				+ " ( " + DBConstants.MSISDN + " TEXT PRIMARY KEY, "
				+ DBConstants.IMAGE + " BLOB" + " ) ";
		db.execSQL(create);
	}

	private HikeUserDatabase(Context context) {
		super(context, DBConstants.USERS_DATABASE_NAME, null,
				DBConstants.USERS_DATABASE_VERSION);
		mDb = getWritableDatabase();
		mReadDb = getReadableDatabase();
		this.mContext = context;
	}

	@Override
	public void close() {
		mDb.close();
		mReadDb.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(getClass().getSimpleName(), "Upgrading users table from "
				+ oldVersion + " to " + newVersion);
		if (oldVersion < 3) {
			String alter1 = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " ADD COLUMN " + DBConstants.MSISDN_TYPE + " STRING";
			String alter2 = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " ADD COLUMN " + DBConstants.LAST_MESSAGED + " INTEGER";
			db.execSQL(alter1);
			db.execSQL(alter2);
		}
		// Changing the datatype of the name column
		if (oldVersion < 4) {
			Log.d(getClass().getSimpleName(), "Updating table");
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " RENAME TO " + "temp_table";

			String create = "CREATE TABLE IF NOT EXISTS "
					+ DBConstants.USERS_TABLE + " ( " + DBConstants.ID
					+ " STRING , " + DBConstants.NAME + " TEXT, "
					+ DBConstants.MSISDN + " TEXT COLLATE nocase, "
					+ DBConstants.ONHIKE + " INTEGER, " + DBConstants.PHONE
					+ " TEXT, " + DBConstants.HAS_CUSTOM_PHOTO + " INTEGER, "
					+ DBConstants.OVERLAY_DISMISSED + " INTEGER, "
					+ DBConstants.MSISDN_TYPE + " STRING, "
					+ DBConstants.LAST_MESSAGED + " INTEGER" + " )";

			String insert = "INSERT INTO " + DBConstants.USERS_TABLE
					+ " SELECT * FROM temp_table";

			String drop = "DROP TABLE temp_table";

			db.execSQL(alter);
			db.execSQL(create);
			db.execSQL(insert);
			db.execSQL(drop);
		}
		// Add favorite column
		if (oldVersion < 5) {
			String alter = "ALTER TABLE " + DBConstants.USERS_TABLE
					+ " ADD COLUMN " + DBConstants.FAVORITE
					+ " INTEGER DEFAULT " + FavoriteType.NOT_FAVORITE.ordinal();
			db.execSQL(alter);
		}
	}

	public void addContacts(List<ContactInfo> contacts, boolean isFirstSync)
			throws DbException {
		SQLiteDatabase db = mDb;
		db.beginTransaction();

		Map<String, String> msisdnTypeMap = new HashMap<String, String>();
		/*
		 * Since this is the first sync, we just run one query and pickup all
		 * the extra info required. For all subsequent syncs we run the query
		 * for each contact separately.
		 */
		if (isFirstSync) {
			// Adding the last contacted and phone type info
			Cursor extraInfo = this.mContext.getContentResolver().query(
					Phone.CONTENT_URI,
					new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL },
					null, null, null);

			int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
			int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
			int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

			while (extraInfo.moveToNext()) {
				String msisdnType = Phone.getTypeLabel(
						this.mContext.getResources(),
						extraInfo.getInt(typeIdx),
						extraInfo.getString(labelIdx)).toString();

				msisdnTypeMap.put(extraInfo.getString(msisdnIdx), msisdnType);
			}
			extraInfo.close();
		}

		InsertHelper ih = null;
		try {
			ih = new InsertHelper(db, DBConstants.USERS_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			final int idColumn = ih.getColumnIndex(DBConstants.ID);
			final int nameColumn = ih.getColumnIndex(DBConstants.NAME);
			final int onHikeColumn = ih.getColumnIndex(DBConstants.ONHIKE);
			final int phoneColumn = ih.getColumnIndex(DBConstants.PHONE);
			final int msisdnTypeColumn = ih
					.getColumnIndex(DBConstants.MSISDN_TYPE);
			for (ContactInfo contact : contacts) {
				ih.prepareForReplace();
				ih.bind(nameColumn, contact.getName());
				ih.bind(msisdnColumn, contact.getMsisdn());
				ih.bind(idColumn, contact.getId());
				ih.bind(onHikeColumn, contact.isOnhike());
				ih.bind(phoneColumn, contact.getPhoneNum());
				if (!isFirstSync) {
					String selection = Phone.CONTACT_ID + " =? " + " AND "
							+ Phone.NUMBER + " =? ";
					// Adding the last contacted and phone type info
					Cursor additionalInfo = this.mContext.getContentResolver()
							.query(Phone.CONTENT_URI,
									new String[] { Phone.TYPE, Phone.LABEL },
									selection,
									new String[] { contact.getId(),
											contact.getPhoneNum() }, null);

					int typeIdx = additionalInfo.getColumnIndex(Phone.TYPE);
					int labelIdx = additionalInfo.getColumnIndex(Phone.LABEL);
					if (additionalInfo.moveToFirst()) {
						contact.setMsisdnType(Phone.getTypeLabel(
								this.mContext.getResources(),
								additionalInfo.getInt(typeIdx),
								additionalInfo.getString(labelIdx)).toString());
					}
					Log.d(getClass().getSimpleName(),
							"Msisdn Type: " + contact.getMsisdnType());
					additionalInfo.close();

					ih.bind(msisdnTypeColumn, contact.getMsisdnType());
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.CONTACT_ADDED, contact);
				} else {
					ih.bind(msisdnTypeColumn,
							msisdnTypeMap.get(contact.getPhoneNum()));
					/*
					 * We're saving this parameter to notify that the extra info
					 * that we are now fetching (Msisdn type) has been synced.
					 * So for apps that update from an older version, we can
					 * just check this value to verify whether the contacts have
					 * their extra info synced.
					 */
					Editor editor = mContext.getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putBoolean(
							HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
					editor.commit();
				}
				ih.execute();
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			db.endTransaction();
		}
	}

	public void addBlockList(List<String> msisdns) throws DbException {
		if (msisdns == null) {
			return;
		}

		SQLiteDatabase db = mDb;
		db.beginTransaction();

		InsertHelper ih = null;
		try {
			ih = new InsertHelper(db, DBConstants.BLOCK_TABLE);
			final int msisdnColumn = ih.getColumnIndex(DBConstants.MSISDN);
			for (String msisdn : msisdns) {
				ih.prepareForReplace();
				ih.bind(msisdnColumn, msisdn);
				ih.execute();
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e("HikeUserDatabase", "Unable to insert contacts", e);
			throw new DbException(e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			db.endTransaction();
		}
	}

	/**
	 * Sets the address book from the list of contacts Deletes any existing
	 * contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 */
	public void setAddressBookAndBlockList(List<ContactInfo> contacts,
			List<String> blockedMsisdns) throws DbException {
		/* delete all existing entries from database */
		mDb.delete(DBConstants.USERS_TABLE, null, null);

		mDb.delete(DBConstants.BLOCK_TABLE, null, null);

		addContacts(contacts, true);
		addBlockList(blockedMsisdns);
	}

	public Cursor findUsers(String partialName, String selectedContacts) {
		List<String> contacts = Utils.splitSelectedContacts(selectedContacts);
		StringBuilder selectedNumbers = new StringBuilder("");

		if (contacts.size() > 0) {
			for (String contact : contacts) {
				selectedNumbers.append("'" + contact + "',");
			}
			selectedNumbers.delete(selectedNumbers.length() - 1,
					selectedNumbers.length());
		}

		String[] columns = new String[] { DBConstants.NAME,
				DBConstants.ID + " AS _id", DBConstants.MSISDN,
				DBConstants.ONHIKE, DBConstants.ONHIKE + "=0 AS NotOnHike",
				DBConstants.PHONE, DBConstants.LAST_MESSAGED,
				DBConstants.MSISDN_TYPE, DBConstants.HAS_CUSTOM_PHOTO };

		String selection = "((" + DBConstants.NAME + " LIKE ? OR "
				+ DBConstants.MSISDN + " LIKE ?) AND " + DBConstants.MSISDN
				+ " NOT IN (" + selectedNumbers + ")) AND "
				+ DBConstants.MSISDN + " != 'null'";

		String[] selectionArgs = new String[] { partialName, partialName };

		String orderBy = "NotOnHike";

		Cursor cursor = mDb.query(DBConstants.USERS_TABLE, columns, selection,
				selectionArgs, null, null, orderBy);
		return cursor;
	}

	public ContactInfo getContactInfoFromMSISDN(String msisdn,
			boolean ifNotFoundReturnNull) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO,
				DBConstants.FAVORITE }, DBConstants.MSISDN + "=?",
				new String[] { msisdn }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			Log.d(getClass().getSimpleName(), "No contact found");
			return ifNotFoundReturnNull ? null : new ContactInfo(msisdn,
					msisdn, null, msisdn, false);
		}

		return contactInfos.get(0);
	}

	private List<ContactInfo> extractContactInfo(Cursor c) {
		return extractContactInfo(c, false);
	}

	private List<ContactInfo> extractContactInfo(Cursor c, boolean distinct) {
		List<ContactInfo> contactInfos = new ArrayList<ContactInfo>(
				c.getCount());
		int idx = c.getColumnIndex(DBConstants.ID);
		int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
		int nameIdx = c.getColumnIndex(DBConstants.NAME);
		int onhikeIdx = c.getColumnIndex(DBConstants.ONHIKE);
		int phoneNumIdx = c.getColumnIndex(DBConstants.PHONE);
		int msisdnTypeIdx = c.getColumnIndex(DBConstants.MSISDN_TYPE);
		int lastMessagedIdx = c.getColumnIndex(DBConstants.LAST_MESSAGED);
		int hasCustomPhotoIdx = c.getColumnIndex(DBConstants.HAS_CUSTOM_PHOTO);
		int favoriteIdx = c.getColumnIndex(DBConstants.FAVORITE);

		Set<String> msisdnSet = null;

		if (distinct) {
			msisdnSet = new HashSet<String>();
		}
		while (c.moveToNext()) {
			String msisdn = c.getString(msisdnIdx);
			if (distinct && msisdnSet.contains(msisdn)) {
				continue;
			} else if (distinct) {
				msisdnSet.add(msisdn);
			}
			ContactInfo contactInfo = new ContactInfo(c.getString(idx),
					c.getString(msisdnIdx), c.getString(nameIdx),
					c.getString(phoneNumIdx), c.getInt(onhikeIdx) != 0,
					c.getString(msisdnTypeIdx), c.getLong(lastMessagedIdx),
					c.getInt(hasCustomPhotoIdx) == 1);
			if (favoriteIdx != -1) {
				int favoriteTypeOrd = c.getInt(favoriteIdx);
				contactInfo
						.setFavoriteType(FavoriteType.values()[favoriteTypeOrd]);
			}
			contactInfos.add(contactInfo);
		}
		c.close();
		return contactInfos;
	}

	public ContactInfo getContactInfoFromId(String id) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				DBConstants.ID + "=?", new String[] { id }, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return null;
		}

		return contactInfos.get(0);
	}

	/**
	 * Adds a single contact to the hike user db
	 * 
	 * @param hikeContactInfo
	 *            contact to add
	 * @return true if the insert was successful
	 */
	public void addContact(ContactInfo hikeContactInfo) throws DbException {
		List<ContactInfo> l = new LinkedList<ContactInfo>();
		l.add(hikeContactInfo);
		addContacts(l, false);
	}

	public List<ContactInfo> getNonHikeContacts() {
		Cursor c = mReadDb.rawQuery("SELECT " + DBConstants.USERS_TABLE + "."
				+ DBConstants.MSISDN + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.ID + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.NAME + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.ONHIKE + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.PHONE + ", " + DBConstants.USERS_TABLE + "."
				+ DBConstants.MSISDN_TYPE + ", " + DBConstants.USERS_TABLE
				+ "." + DBConstants.HAS_CUSTOM_PHOTO + ", "
				+ DBConstants.USERS_TABLE + "." + DBConstants.LAST_MESSAGED
				+ " FROM " + DBConstants.USERS_TABLE + " WHERE "
				+ DBConstants.USERS_TABLE + "." + DBConstants.MSISDN
				+ " NOT IN (SELECT " + DBConstants.MSISDN + " FROM "
				+ DBConstants.BLOCK_TABLE + ") AND " + DBConstants.USERS_TABLE
				+ "." + DBConstants.ONHIKE + " =0 ", null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return contactInfos;
		}

		return contactInfos;
	}

	public List<ContactInfo> getContacts() {
		return getContacts(true);
	}

	/**
	 * Returns a list of contact ordered by when they were last messaged
	 * 
	 * @param limit
	 *            The number of contacts required. -1 if limit is not to be
	 *            considered
	 * @param favorite
	 *            Whether we only need the favorite contacts or non favorite. -1
	 *            if we want to ignore this
	 * @return
	 */
	public List<ContactInfo> getContactsOrderedByLastMessaged(int limit,
			FavoriteType favorite, int onHike, boolean distinct, boolean hikeFirst) {
		String selection = DBConstants.MSISDN
				+ " != 'null'"
				+ (favorite != null ? " AND " + DBConstants.FAVORITE + "="
						+ favorite.ordinal() : "")
				+ (onHike != -1 ? " AND " + DBConstants.ONHIKE + "=" + onHike : "");
		Log.d(getClass().getSimpleName(), "Selection: " + selection);
		String orderBy = (hikeFirst ? DBConstants.ONHIKE + " DESC, " : "") + DBConstants.LAST_MESSAGED + " DESC, "
				+ DBConstants.NAME + " COLLATE NOCASE"
				+ (limit > -1 ? " LIMIT " + limit : "");
		String[] columns = { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO, DBConstants.FAVORITE };
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
				null, null, null, orderBy);
		List<ContactInfo> contactInfos = extractContactInfo(c, distinct);
		c.close();
		if (contactInfos.isEmpty()) {
			return contactInfos;
		}
		return contactInfos;
	}

	public List<ContactInfo> getContacts(boolean ignoreEmpty) {
		String selection = ignoreEmpty ? DBConstants.MSISDN + " != 'null'"
				: null;
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				selection, null, null, null, null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return contactInfos;
		}
		return contactInfos;
	}

	public void deleteMultipleRows(Collection<String> ids) {
		String ids_joined = "(" + Utils.join(ids, ",", "\"", "\"") + ")";
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + " in "
				+ ids_joined, null);
	}

	public void deleteRow(String id) {
		mDb.delete(DBConstants.USERS_TABLE, DBConstants.ID + "=?",
				new String[] { id });
	}

	public void updateContacts(List<ContactInfo> updatedContacts) {
		if (updatedContacts == null) {
			return;
		}

		ArrayList<String> ids = new ArrayList<String>(updatedContacts.size());
		for (ContactInfo c : updatedContacts) {
			ids.add(c.getId());
		}
		deleteMultipleRows(ids);
		try {
			addContacts(updatedContacts, false);
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateHikeContact(String msisdn, boolean onhike) {
		ContentValues vals = new ContentValues(1);
		vals.put(DBConstants.ONHIKE, onhike);
		mDb.update(DBConstants.USERS_TABLE, vals, "msisdn=?",
				new String[] { msisdn });
	}

	public void deleteAll() {
		mDb.delete(DBConstants.USERS_TABLE, null, null);
		mDb.delete(DBConstants.BLOCK_TABLE, null, null);
		mDb.delete(DBConstants.THUMBNAILS_TABLE, null, null);
	}

	public ContactInfo getContactInfoFromPhoneNo(String number) {
		Cursor c = mReadDb.query(DBConstants.USERS_TABLE, new String[] {
				DBConstants.MSISDN, DBConstants.ID, DBConstants.NAME,
				DBConstants.ONHIKE, DBConstants.PHONE, DBConstants.MSISDN_TYPE,
				DBConstants.LAST_MESSAGED, DBConstants.HAS_CUSTOM_PHOTO },
				DBConstants.PHONE + "=?", new String[] { number }, null, null,
				null);
		List<ContactInfo> contactInfos = extractContactInfo(c);
		c.close();
		if (contactInfos.isEmpty()) {
			return null;
		}

		return contactInfos.get(0);
	}

	public void unblock(String msisdn) {
		mDb.delete(DBConstants.BLOCK_TABLE, DBConstants.MSISDN + "=?",
				new String[] { msisdn });
	}

	public void block(String msisdn) {
		ContentValues values = new ContentValues();
		values.put(DBConstants.MSISDN, msisdn);
		mDb.insert(DBConstants.BLOCK_TABLE, null, values);
	}

	public boolean isBlocked(String msisdn) {
		Cursor c = null;
		try {
			c = mDb.query(DBConstants.BLOCK_TABLE, null, DBConstants.MSISDN
					+ "=?", new String[] { msisdn }, null, null, null);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public Set<String> getBlockedUsers() {
		Set<String> blocked = new HashSet<String>();
		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.BLOCK_TABLE,
					new String[] { DBConstants.MSISDN }, null, null, null,
					null, null);
			int idx = c.getColumnIndex(DBConstants.MSISDN);
			while (c.moveToNext()) {
				blocked.add(c.getString(idx));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return blocked;
	}

	public void setIcon(String msisdn, byte[] data, boolean isProfileImage) {
		if (!isProfileImage) {
			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0,
					data.length);
			Bitmap roundedBitmap = Utils.getRoundedCornerBitmap(tempBitmap);
			data = Utils
					.bitmapToBytes(roundedBitmap, Bitmap.CompressFormat.PNG);
			tempBitmap.recycle();
			roundedBitmap.recycle();
		}
		IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		ContentValues vals = new ContentValues(2);
		vals.put(DBConstants.MSISDN, msisdn);
		vals.put(DBConstants.IMAGE, data);
		mDb.replace(DBConstants.THUMBNAILS_TABLE, null, vals);

		String whereClause = DBConstants.MSISDN + "=?"; // msisdn;
		ContentValues customPhotoFlag = new ContentValues(1);
		customPhotoFlag.put(DBConstants.HAS_CUSTOM_PHOTO, 1);
		mDb.update(DBConstants.USERS_TABLE, customPhotoFlag, whereClause,
				new String[] { msisdn });
	}

	public Drawable getIcon(String msisdn) {
		Cursor c = mDb.query(DBConstants.THUMBNAILS_TABLE,
				new String[] { DBConstants.IMAGE }, "msisdn=?",
				new String[] { msisdn }, null, null, null);
		try {
			if (!c.moveToFirst()) {
				/* lookup based on this msisdn */
				return Utils.getDefaultIconForUser(mContext, msisdn);
			}

			byte[] icondata = c.getBlob(c.getColumnIndex(DBConstants.IMAGE));
			return new BitmapDrawable(BitmapFactory.decodeByteArray(icondata,
					0, icondata.length));
		} finally {
			c.close();
		}
	}

	public void updateContactRecency(String msisdn, long timeStamp) {
		ContentValues updatedTime = new ContentValues(1);
		updatedTime.put(DBConstants.LAST_MESSAGED, timeStamp);

		String whereClause = DBConstants.MSISDN + "=?";
		int rows = mDb.update(DBConstants.USERS_TABLE, updatedTime,
				whereClause, new String[] { msisdn });
		Log.d(getClass().getSimpleName(), "Row has been updated: " + rows);
	}

	public void syncContactExtraInfo() {
		Cursor extraInfo = this.mContext.getContentResolver().query(
				Phone.CONTENT_URI,
				new String[] { Phone.NUMBER, Phone.TYPE, Phone.LABEL }, null,
				null, null);

		int msisdnIdx = extraInfo.getColumnIndex(Phone.NUMBER);
		int typeIdx = extraInfo.getColumnIndex(Phone.TYPE);
		int labelIdx = extraInfo.getColumnIndex(Phone.LABEL);

		mDb.beginTransaction();

		try {
			while (extraInfo.moveToNext()) {
				String msisdnType = Phone.getTypeLabel(
						this.mContext.getResources(),
						extraInfo.getInt(typeIdx),
						extraInfo.getString(labelIdx)).toString();
				Log.d(getClass().getSimpleName(), "Msisdntype: " + msisdnType);
				ContentValues contentValues = new ContentValues(1);
				contentValues.put(DBConstants.MSISDN_TYPE, msisdnType);
				String whereClause = DBConstants.PHONE + " =? ";
				mDb.update(DBConstants.USERS_TABLE, contentValues, whereClause,
						new String[] { extraInfo.getString(msisdnIdx) });
			}
			mDb.setTransactionSuccessful();
			Editor editor = mContext.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, true);
			editor.commit();
		} finally {
			extraInfo.close();
			mDb.endTransaction();
		}
	}

	public void toggleContactFavorite(String msisdn, FavoriteType favoriteType) {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.FAVORITE, favoriteType.ordinal());

		mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN
				+ "=?", new String[] { msisdn });
	}

	public void addAutoRecommendedFavorites() {
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBConstants.FAVORITE,
				FavoriteType.AUTO_RECOMMENDED_FAVORITE.ordinal());

		int rows = mDb.update(DBConstants.USERS_TABLE, contentValues, DBConstants.MSISDN
				+ " IN (SELECT DISTINCT " + DBConstants.MSISDN + " FROM "
				+ DBConstants.USERS_TABLE + " WHERE "
				+ DBConstants.LAST_MESSAGED + ">0 ORDER BY "
				+ DBConstants.LAST_MESSAGED + " DESC LIMIT "
				+ HikeConstants.MAX_AUTO_RECOMMENDED_FAVORITE + ")", null);

		Log.d(getClass().getSimpleName(), "Auto recommended favorites added: " + rows);
	}

	public boolean isContactFavorite(String msisdn) {
		Cursor c = null;
		try {
			c = mDb.query(DBConstants.USERS_TABLE,
					new String[] { DBConstants.MSISDN }, DBConstants.MSISDN
							+ "=? AND " + DBConstants.FAVORITE + "="
							+ FavoriteType.FAVORITE, new String[] { msisdn },
					null, null, null);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<ContactInfo> getNonHikeContactsFromListOfNumbers(
			List<String> numbersList) {
		String[] columns = new String[] { DBConstants.MSISDN, DBConstants.ID,
				DBConstants.NAME, DBConstants.ONHIKE, DBConstants.PHONE,
				DBConstants.MSISDN_TYPE, DBConstants.LAST_MESSAGED,
				DBConstants.HAS_CUSTOM_PHOTO };

		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < numbersList.size(); i++) {
			sb.append("'" + numbersList.get(i) + "'");
			if (i != numbersList.size() - 1) {
				sb.append(",");
			}
		}
		sb.append(")");
		String selection = DBConstants.PHONE + " IN " + sb.toString() + " AND "
				+ DBConstants.ONHIKE + "=0 AND " + DBConstants.MSISDN
				+ " LIKE '+91%'";
		Log.d(getClass().getSimpleName(), "Selection query: " + selection);
		Cursor c = null;
		try {
			c = mReadDb.query(DBConstants.USERS_TABLE, columns, selection,
					null, null, null, null);
			return extractContactInfo(c, true);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<ContactInfo> getContactNamesFromMsisdnList(StringBuilder msisdns) {
		// select max(name), msisdn from users where msisdn in (...) group by
		// msisdn;
		Cursor c = mReadDb.rawQuery("SELECT max(" + DBConstants.NAME + ") AS "
				+ DBConstants.NAME + ", " + DBConstants.MSISDN + ", "
				+ DBConstants.ONHIKE + " from " + DBConstants.USERS_TABLE
				+ " WHERE " + DBConstants.MSISDN + " IN " + msisdns.toString()
				+ "GROUP BY " + DBConstants.MSISDN, null);
		try {
			List<ContactInfo> contactList = new ArrayList<ContactInfo>();

			final int nameIdx = c.getColumnIndex(DBConstants.NAME);
			final int msisdnIdx = c.getColumnIndex(DBConstants.MSISDN);
			final int onHikeIdx = c.getColumnIndex(DBConstants.ONHIKE);

			while (c.moveToNext()) {
				String msisdn = c.getString(msisdnIdx);
				String name = c.getString(nameIdx);
				boolean onHike = c.getInt(onHikeIdx) != 0;
				Log.d(getClass().getSimpleName(), "Name: " + name);
				contactList.add(new ContactInfo(null, msisdn, name, null,
						onHike));
			}
			return contactList;
		} finally {
			c.close();
		}
	}
}
