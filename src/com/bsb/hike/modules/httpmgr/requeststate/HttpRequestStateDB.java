package com.bsb.hike.modules.httpmgr.requeststate;

import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Singleton class for database calls on http request state db used for storing http request states for upload and download requests. This will be later used for persistent retries
 * when network is not there and we want to retry whenever network comes
 */
public class HttpRequestStateDB extends SQLiteOpenHelper
{
	private static final String TAG = "HttpRequestStateDb";

	public static final String HTTP_REQUEST_STATE_DATABASE_NAME = "httpReqStateDB";

	public static final int HTTP_REQUEST_STATE_DATABASE_VERSION = 2;

	public static final String HTTP_REQUEST_STATE_TABLE = "requestStateTable";

	public static final String GCM_NETWORK_MANAGER_TABLE = "gcmNetworkManagerTable";

	public static final String REQUEST_ID = "reqId";

	public static final String METADATA = "metadata";

	public static final String REQUEST_TAG = "request_tag";

	public static final String BUNDLE = "bundle";

	private static volatile HttpRequestStateDB httpRequestStateDB;

	private SQLiteDatabase mDb;

	private Context mContext;

	/**
	 * Constructor to initialize SQLiteDatabase variables
	 * 
	 * @param context
	 */
	private HttpRequestStateDB(Context context)
	{
		super(context, HTTP_REQUEST_STATE_DATABASE_NAME, null, HTTP_REQUEST_STATE_DATABASE_VERSION);
		this.mContext = context;
		mDb = getWritableDatabase();
	}

	/**
	 * Returns {@link HttpRequestStateDB} instance
	 * 
	 * @return
	 */
	public static HttpRequestStateDB getInstance()
	{
		if (httpRequestStateDB == null)
		{
			synchronized (HttpRequestStateDB.class)
			{
				if (httpRequestStateDB == null)
				{
					httpRequestStateDB = new HttpRequestStateDB(HikeMessengerApp.getInstance().getApplicationContext());
				}
			}
		}
		return httpRequestStateDB;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String sql = "CREATE TABLE IF NOT EXISTS " + HTTP_REQUEST_STATE_TABLE + " ( " + REQUEST_ID + " TEXT PRIMARY KEY, " + METADATA + " TEXT" + " )";
		db.execSQL(sql);

		sql  = getGcmNwMgrTableCreateQuery();
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if(oldVersion < 2)
		{
			String sql = getGcmNwMgrTableCreateQuery();
			db.execSQL(sql);
		}
	}

	private String getGcmNwMgrTableCreateQuery()
	{
		String sql = "CREATE TABLE IF NOT EXISTS " + GCM_NETWORK_MANAGER_TABLE
				+ " ( "
				+ REQUEST_TAG + " TEXT PRIMARY KEY, "
				+ BUNDLE + " TEXT"
				+ " )";
		return sql;
	}

	/**
	 * Inserts the http request state or replaces if a row with request id already exists in database
	 * 
	 * @param requestState
	 *            Request state {@link HttpRequestState}
	 * @return
	 */
	public long insertOrReplaceRequestState(HttpRequestState requestState)
	{
		ContentValues cv = getRequestStateContentValues(requestState);
		long rows = mDb.insertWithOnConflict(HTTP_REQUEST_STATE_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		return rows;
	}

	/**
	 * Returns the content values to be stored in database for {@link HttpRequestState}
	 * 
	 * @param requestState
	 *            Request state {@link HttpRequestState}
	 * @return
	 */
	private ContentValues getRequestStateContentValues(HttpRequestState requestState)
	{
		ContentValues cv = new ContentValues();
		cv.put(REQUEST_ID, requestState.getRequestId());
		cv.put(METADATA, requestState.getMetadataString());
		return cv;
	}

	/**
	 * Returns request state {@link HttpRequestState} for a particular request id. Returns null if not found
	 * 
	 * @param requestId
	 * @return
	 */
	public HttpRequestState getRequestState(String requestId)
	{
		Cursor c = null;
		try
		{
			c = mDb.query(HTTP_REQUEST_STATE_TABLE, null, REQUEST_ID + "=?", new String[] { requestId }, null, null, null);
			HttpRequestState state = null;
			if (c.moveToFirst())
			{
				state = processRequestState(c);
			}
			return state;
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
	 * Forms a {@link HttpRequestState} object from a database cursor and returns the same
	 * 
	 * @param c
	 *            Cursor object
	 * @return
	 */
	private HttpRequestState processRequestState(Cursor c)
	{
		final int requestIdIdx = c.getColumnIndex(REQUEST_ID);
		final int metadataIdx = c.getColumnIndex(METADATA);

		String id = c.getString(requestIdIdx);
		String metadata = c.getString(metadataIdx);

		HttpRequestState state = new HttpRequestState(id);
		try
		{
			state.setMetadata(metadata);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "json exception while setting metadata ", e);
		}
		return state;
	}

	/**
	 * Deletes request state from database for particular request id
	 * 
	 * @param id
	 */
	public void deleteState(String id)
	{
		mDb.delete(HTTP_REQUEST_STATE_TABLE, REQUEST_ID + "=?", new String[] { id });
	}

	public Bundle getBundleForTag(String requestTag)
	{
		Cursor c = null;
		Bundle bundle = null;
		try
		{
			c = mDb.query(GCM_NETWORK_MANAGER_TABLE, null, REQUEST_TAG + "=?", new String[] { requestTag }, null, null, null);

			int bundleIdx = c.getColumnIndex(BUNDLE);
			if (c.moveToFirst())
			{
				byte[] bundleBytes = c.getBlob(bundleIdx);
				bundle = Utils.getBundleFromBytes(bundleBytes);
			}
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return bundle;
	}

	public void insertBundleToDb(String requestTag, Bundle bundle)
	{
		byte[] bundleBytes = Utils.getBytesFromBundle(bundle);

		ContentValues contentValues = new ContentValues();
		contentValues.put(REQUEST_TAG, requestTag);
		contentValues.put(BUNDLE, bundleBytes);

		mDb.insertWithOnConflict(GCM_NETWORK_MANAGER_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void deleteAll()
	{
		mDb.delete(HTTP_REQUEST_STATE_TABLE, null, null);
		mDb.delete(GCM_NETWORK_MANAGER_TABLE, null, null);
	}
}
