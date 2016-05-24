package com.bsb.hike.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * Provides hike contact's avatar blob data
 * 
 * @author Atul M
 * 
 */
public class HikeProvider extends ContentProvider
{
	private static final String TAG = HikeProvider.class.getName();

	private static final String AUTHORITY = "com.bsb.hike.providers.HikeProvider";

	private static final String BASE_PATH_ROUNDED = "avatarRounded";

	private static final String BASE_PATH_NORMAL = "avatarNormal";

	private static final int ROUNDED_INDEX = 1;

	private static final int NORMAL_INDEX = 2;

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_ROUNDED);

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_ROUNDED, ROUNDED_INDEX);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_NORMAL, NORMAL_INDEX);
	}

	/**
	 * go to https://code.google.com/p/android/issues/detail?id=8727
	 *
	 * and search for @leach.it... if u are getting a npe while writing HikeMessengerApp.getInstance().getApplicationContext();
	 * @return
	 */
	@Override
	public boolean onCreate()
	{
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		ContactManager conMgr = ContactManager.getInstance();
		
		// Authenticate
		List<String> uriPathSegments = uri.getPathSegments();

		if (uriPathSegments != null)
		{
			try
			{
				String accessT = uriPathSegments.get(uriPathSegments.size() - 2);
				String pkgName = uriPathSegments.get(uriPathSegments.size() - 1);
				if (HikeAuthActivity.verifyRequest(getContext(), pkgName, accessT))
				{
					String newUri = "content://" + uri.getAuthority();
					int segmentsListSize = uriPathSegments.size();

					for (int i = 0; i < segmentsListSize; i++)
					{
						if (i == (segmentsListSize - 2))
						{
							break;
						}
						newUri += "/" + uriPathSegments.get(i);
					}
					uri = Uri.parse(newUri);
				}
				else
				{
					throw new RuntimeException("Required missing authentication!");
				}
			}
			catch (IndexOutOfBoundsException iobe)
			{
				iobe.printStackTrace();
				return null;
			}
		}

		Cursor c = null;

		// Identify avatar request
		switch (sURIMatcher.match(uri))
		{
		case ROUNDED_INDEX:
		case NORMAL_INDEX:
			try
			{
				// For better security, use hard-coded selection columns
				if (selection == null)
				{
					c = conMgr.getReadableDatabase().rawQuery("SELECT " + DBConstants.THUMBNAILS_TABLE + "." + DBConstants.IMAGE + ", " + DBConstants.USERS_TABLE + "." + DBConstants.ID + " " + "FROM "
							+ DBConstants.THUMBNAILS_TABLE + " " + "INNER JOIN " + DBConstants.USERS_TABLE + " " + "ON " + DBConstants.THUMBNAILS_TABLE + "." + DBConstants.MSISDN
							+ "=" + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + "", null);
				}
				else
				{
					if (selectionArgs != null && selectionArgs.length > 0)
					{
						// TODO:Improve this. Make it more generic
						if (selectionArgs[0].equals(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null)))
						{
							// self avatar request
							ContactInfo contactInfo = Utils.getUserContactInfo(HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).getPref());
							c = conMgr.getReadableDatabase()
									.query(DBConstants.THUMBNAILS_TABLE, new String[] { DBConstants.IMAGE }, DBConstants.MSISDN + "=?", new String[] { contactInfo.getMsisdn() },
											null, null, null);
						}
						else
						{
							c = conMgr.getReadableDatabase().rawQuery("SELECT " + DBConstants.THUMBNAILS_TABLE + "." + DBConstants.IMAGE + ", " + DBConstants.USERS_TABLE + "." + DBConstants.ID + " "
									+ "FROM " + DBConstants.THUMBNAILS_TABLE + " " + "INNER JOIN " + DBConstants.USERS_TABLE + " " + "ON " + DBConstants.THUMBNAILS_TABLE + "."
									+ DBConstants.MSISDN + "=" + DBConstants.USERS_TABLE + "." + DBConstants.MSISDN + " " + "WHERE " + DBConstants.USERS_TABLE + "."
									+ DBConstants.ID + " IN " + Utils.getMsisdnStatement(Arrays.asList(selectionArgs)), null);
						}
					}
				}
			}
			catch (SQLiteException e)
			{
				e.printStackTrace();
			}
			break;
		default:
			break;
		}

		return c;
	}

	@Override
	public String getType(Uri uri)
	{
		String fileNameRequested = uri.getLastPathSegment();
		String[] name = fileNameRequested.split("\\.");

		try
		{
			String suffix = name[1];

			if (suffix.equals("css"))
			{
				return "text/css";
			}
			else if (suffix.equals("png"))
			{
				return "image/png";
			}
			else if (suffix.equals("js"))
			{
				return "application/javascript";
			}
			else
			{
				return "text/plain";
			}
		}
		catch (ArrayIndexOutOfBoundsException aiobe)
		{
			aiobe.printStackTrace();
			throw new UnsupportedOperationException("Cannot recognize MIME type");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		Logger.d(TAG, "Invalid access");
		throw new UnsupportedOperationException("Invalid access");
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
	{
		Logger.d("FileContentProvider", "fetching: " + uri);

		ParcelFileDescriptor parcel = null;

        // Precautionary check to keep current microapps to work even if versioning migration fails
        String filePath = uri.toString().replace(PlatformContentConstants.CONTENT_AUTHORITY_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR);

        if(!new File(filePath).exists())
            filePath = uri.toString().replace(PlatformContentConstants.CONTENT_AUTHORITY_BASE, PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR);

		try
		{
			parcel = ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY);
		}
		catch (FileNotFoundException e)
		{
			Logger.e("FileContentProvider", "uri " + uri.toString(), e);
		}
		return parcel;
	}

}
