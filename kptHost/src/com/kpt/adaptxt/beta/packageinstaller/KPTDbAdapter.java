package com.kpt.adaptxt.beta.packageinstaller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


public class KPTDbAdapter {

	private Context context;
	private SQLiteDatabase database;
	private KPTDatabaseHelper dbHelper;

	public KPTDbAdapter(Context context) {
		this.context = context;
	}

	public KPTDbAdapter open()  {
		try {
			dbHelper = new KPTDatabaseHelper(context);
			database = dbHelper.getWritableDatabase();	
		} catch (SQLiteException e) {
		}
		
		return this;
	}

	public void close() {
		try {
			dbHelper.close();
		} catch (SQLiteException e) {
		}
		
	}


	public long createItem(String fileName, String pkgNameCore,
			String pkgCoreVer, String pkgDevVer, String coreDisplayName) {
		ContentValues initialValues = createContentValues(fileName, pkgNameCore,
				pkgCoreVer, pkgDevVer, coreDisplayName);
		return database.insert(KPTDatabaseHelper.TABLE_NAME, null, initialValues);
	}



	public boolean updateItem(String fileName, String pkgNameCore,
			String pkgCoreVer, String pkgDevVer, String coreDisplayName) {
		ContentValues updateValues = updateContentValues(pkgNameCore,
				pkgCoreVer, pkgDevVer, coreDisplayName);

		return database.update(KPTDatabaseHelper.TABLE_NAME, updateValues, KPTDatabaseHelper.FILE_NAME  + "=" + "'" + fileName + "'", null) > 0;
	}



	public boolean deleteItem(String fileName) {
		return database.delete(KPTDatabaseHelper.TABLE_NAME, KPTDatabaseHelper.FILE_NAME  + "=" + "'" + fileName + "'", null) > 0;
	}



	private ContentValues createContentValues(String fileName, String pkgNameCore,
			String pkgCoreVer, String pkgDevVer, String coreDisplayName) {
		ContentValues values = new ContentValues();
		values.put(KPTDatabaseHelper.FILE_NAME, fileName);
		values.put(KPTDatabaseHelper.PKG_NAME_CORE, pkgNameCore);
		values.put(KPTDatabaseHelper.PKG_CORE_VERSION, pkgCoreVer);
		values.put(KPTDatabaseHelper.PKG_DEVICE_VERSION, pkgDevVer);
		values.put(KPTDatabaseHelper.CORE_DISPALY_NAME, coreDisplayName);
		return values;
	}


	private ContentValues updateContentValues (String pkgNameCore,
			String pkgCoreVer, String pkgDevVer, String coreDisplayName) {

		ContentValues values = new ContentValues();

		if(pkgNameCore!=null) {
			values.put(KPTDatabaseHelper.PKG_NAME_CORE, pkgNameCore);
		}
		if(pkgCoreVer!=null) {
			values.put(KPTDatabaseHelper.PKG_CORE_VERSION, pkgCoreVer);
		}
		if(pkgDevVer!=null) {
			values.put(KPTDatabaseHelper.PKG_DEVICE_VERSION, pkgDevVer);
		}
		if(coreDisplayName !=null) {
			values.put(KPTDatabaseHelper.CORE_DISPALY_NAME, coreDisplayName);
		}
		return values;
	}


	public Cursor fetchAllItems() {
		Cursor cursor = null;
		try {
			cursor = database.query(KPTDatabaseHelper.TABLE_NAME, new String[] { KPTDatabaseHelper.FILE_NAME,
					KPTDatabaseHelper.PKG_NAME_CORE, KPTDatabaseHelper.PKG_CORE_VERSION, KPTDatabaseHelper.PKG_DEVICE_VERSION, KPTDatabaseHelper.CORE_DISPALY_NAME }, null, null, null,
					null, null);
		} catch (SQLException e) {
			
		}
		
		return cursor;
	}


	public int getCount() throws SQLException {
		int count = -1;
		Cursor cursor = null;
		try {
			if(database == null){
				return 0;
			}else{
				cursor =  database.query(KPTDatabaseHelper.TABLE_NAME, new String[] { KPTDatabaseHelper.ROW_ID, KPTDatabaseHelper.FILE_NAME,
						KPTDatabaseHelper.PKG_NAME_CORE, KPTDatabaseHelper.PKG_CORE_VERSION, KPTDatabaseHelper.PKG_DEVICE_VERSION , KPTDatabaseHelper.CORE_DISPALY_NAME }, null, null, null,
						null, null);
				count = cursor.getCount();
			}
		} catch (SQLiteException e) {
		}
		if(cursor !=null) {
			cursor.close();
		}
		return count ;
	}

	public Cursor fetchItem(String fileName) throws SQLException, CursorIndexOutOfBoundsException {
		Cursor cursor = null;
		try { 
			cursor = database.query(true, KPTDatabaseHelper.TABLE_NAME, new String[] {
				KPTDatabaseHelper.ROW_ID, KPTDatabaseHelper.FILE_NAME, KPTDatabaseHelper.PKG_NAME_CORE, KPTDatabaseHelper.PKG_CORE_VERSION, KPTDatabaseHelper.PKG_DEVICE_VERSION , KPTDatabaseHelper.CORE_DISPALY_NAME  },
				KPTDatabaseHelper.FILE_NAME + "=" + "'" + fileName + "'", null, null, null, null, null);
		} catch (Exception e) {
		}
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public boolean itemExists(String fileName)  {
		Cursor cursor = null;
		boolean retVal;
		try {
			 cursor = database.query(true, KPTDatabaseHelper.TABLE_NAME, new String[] {
					KPTDatabaseHelper.ROW_ID, KPTDatabaseHelper.FILE_NAME, KPTDatabaseHelper.PKG_NAME_CORE, KPTDatabaseHelper.PKG_CORE_VERSION, KPTDatabaseHelper.PKG_DEVICE_VERSION , KPTDatabaseHelper.CORE_DISPALY_NAME  },
					KPTDatabaseHelper.FILE_NAME + "=" + "'" + fileName + "'", null, null, null, null, null);
		} catch (SQLException e) {
		} catch (CursorIndexOutOfBoundsException e) {
		}
		
		if (cursor != null && cursor.moveToFirst()) {
			retVal = true;
		} else {
			retVal = false;
		}
		
		if(cursor != null) {
			cursor.close();
		}
		
		return retVal;
	}
}
