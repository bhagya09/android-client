package com.kpt.adaptxt.beta.packageinstaller;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KPTDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "compatibledata";

	private static final int DATABASE_VERSION = 1;
	
	
	public static final String TABLE_NAME = "compatability";
	
	public static final String ROW_ID = "_id";
	
	public static final String FILE_NAME = "filename";

	/***
	 * Column name used in Database which describes
	 * Package name of the add-on whose files are installed in core
	 */
	public static final String PKG_NAME_CORE = "pkg_name_core_ins";
	
	/***
	 * Column name used in Database which describes
	 * version (Ex: V0_6, V0_7) of the add-on whose files are installed in core
	 */
	public static final String PKG_CORE_VERSION = "pkg_core_vers";
	
	/***
	 * Column name used in Database which describes
	 * version (Ex: V0_6, V0_7) of the add-on available on device (The core files of this add-on might or
	 * might not be installed i core)
	 */
	public static final String PKG_DEVICE_VERSION = "pkg_dev_ver";
	
	/***
	 * Column name used in Database which describes
	 * the Display of this filename as given by Core Engine
	 */
	public static final String CORE_DISPALY_NAME = "core_display_name";

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "+ TABLE_NAME + " (" +ROW_ID +" integer primary key autoincrement, " + FILE_NAME +" text, " + PKG_NAME_CORE + " text, " +
	PKG_CORE_VERSION + " text, " + PKG_DEVICE_VERSION + " text, " + CORE_DISPALY_NAME + " text);";

	public KPTDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
	}


}
