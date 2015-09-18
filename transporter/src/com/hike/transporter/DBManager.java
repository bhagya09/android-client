package com.hike.transporter;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hike.transporter.interfaces.IPersistanceInterface;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants.TDBConstants;

public class DBManager extends SQLiteOpenHelper implements IPersistanceInterface
{

	private static final String TAG = "DBManger";

	private Context context = null;

	private static volatile SQLiteDatabase mDb;

	private static volatile DBManager _instance = null;

	public static IPersistanceInterface getInstance()
	{
//		if (_instance == null)
//		{
//			synchronized (DBManager.class)
//			{
//				if (_instance == null)
//				{
//					_instance = new DBManager(Transporter.getInstance().getApplicationContext());
//				}
//			}
//		}
		return Transporter.getInstance().getPersistance();
	}

	private DBManager(Context context)
	{
		super(context, TDBConstants.TRANSPOTER, null, TDBConstants.DBVERION);
		this.context = context;
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		mDb = db;
		String sql = TDBConstants.CREATE_TABLE + TDBConstants.PERSISTANCE_TABLE + " ( " + TDBConstants._ID + " INTEGER PRIMARY KEY AUTOINCREMENT , " + TDBConstants.NAMESPACE
				+ " TEXT," + TDBConstants.MESSAGE + " TEXT ," + TDBConstants.AWB + " TEXT " + ")";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		onCreate(db);
	}

	@Override
	public void addToPersistance(String nameSpace, String message, long awb)
	{
		
		Transporter.getInstance().getPersistance().addToPersistance(nameSpace, message, awb);
//		ContentValues cv = new ContentValues();
//		cv.put(TDBConstants.NAMESPACE, nameSpace);
//		cv.put(TDBConstants.MESSAGE, message);
//		cv.put(TDBConstants.AWB, awb + "");
//		long val = mDb.insertWithOnConflict(TDBConstants.PERSISTANCE_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
//		Logger.d(TAG, "Inserted in to Db with Value" + val+"and AWB number is "+awb);
	}

	@Override
	public void deleteFromPersistance(long awb)
	{
		Transporter.getInstance().getPersistance().deleteFromPersistance(awb);
	}

	@Override
	public void deleteFromPersistance(String nameSpace)
	{
		Transporter.getInstance().deleteFromPersistance(nameSpace);
	}

	@Override
	public void deleteAll()
	{
		Transporter.getInstance().getPersistance().deleteAll();
	}

	@Override
	public List<SenderConsignment> getAllPendingMsgs(String nameSpace)
	{
		return Transporter.getInstance().getPersistance().getAllPendingMsgs(nameSpace);
	}

	//TODO:Change to a single query ....
	@Override
	public void deleteFromPersistance(List<Long> listAwbNumber)
	{
		Transporter.getInstance().getPersistance().deleteFromPersistance(listAwbNumber);
	}
}
