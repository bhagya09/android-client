package com.bsb.hike.modules.contactmgr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;

/**
 * Fetches icon/photo FROM phone contact book FOR on-hike contacts which do not have custom photo (no ic)
 * 
 * @author Atul M
 * 
 */
public class FetchContactIconRunnable implements Runnable
{
	private List<ContactInfo> mContacts;

	private static String TAG = FetchContactIconRunnable.class.getSimpleName();

	public FetchContactIconRunnable(List<ContactInfo> contacts)
	{
		mContacts = contacts;
	}

	@Override
	public void run()
	{
		if (Utils.isEmpty(mContacts))
		{
			Log.e(TAG, "Input contact list empty");
			return;
		}

		Iterator<ContactInfo> iterator = mContacts.iterator();
		while (iterator.hasNext())
		{
			ContactInfo contact = iterator.next();
			if (contact.hasCustomPhoto() || (!contact.isOnhike()))
			{
				iterator.remove();
			}
		}

		for (ContactInfo contact : mContacts)
		{

			long contactId = -1;
			try
			{
				contactId = Long.parseLong(contact.getId());
			}
			catch (NumberFormatException nfe)
			{
				nfe.printStackTrace();
				continue;
			}
			
			if(contactId == -1)
			{
				continue;
			}

			Log.e(TAG, "Trying for " + contact.getNameOrMsisdn());

			// Fetch thumbnail
			byte[] iconBytes = getContactThumbnail(contactId);
			if (iconBytes != null && iconBytes.length > 0)
			{
				try
				{
					Bitmap contactThumbBmp = HikeBitmapFactory.decodeBitmapFromByteArray(iconBytes, Config.RGB_565);
					contactThumbBmp = HikeBitmapFactory.createScaledBitmap(contactThumbBmp, 96, 96, Config.RGB_565, true, false, false);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					contactThumbBmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);

					byte[] byteArray = stream.toByteArray();
					Log.d(TAG, "Found thumbnail for " + contact.getNameOrMsisdn() + " byte size: " + iconBytes.length);
					ContactManager.getInstance().changeUsersDbThumbnail(contact.getMsisdn(), byteArray);
				}
				catch (OutOfMemoryError oome)
				{
					oome.printStackTrace();
					continue;
				}
			}

			// Fetch high-res photo
			iconBytes = getContactPhoto(contactId);
			if (iconBytes != null && iconBytes.length > 0)
			{
				Log.d(TAG, "Found photo for " + contact.getNameOrMsisdn() + " byte size: " + iconBytes.length);
				try
				{
					Bitmap contactPhotoBmp = HikeBitmapFactory.decodeBitmapFromByteArray(iconBytes, Config.RGB_565);
					iconBytes = null;
					
					String profilePicPath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + File.separator
							+ Utils.getProfileImageFileName(contact.getMsisdn());
					File profilePicFile = new File(profilePicPath);

					if (profilePicFile.exists()) //Unlikely
					{
						profilePicFile.delete();
					}
					
					BitmapUtils.saveBitmapToFile(profilePicFile, contactPhotoBmp, CompressFormat.JPEG, 70);
				}
				catch (OutOfMemoryError | IOException oome)
				{
					oome.printStackTrace();
					continue;
				}
			}
		}
	}

	public byte[] getContactPhoto(long contactId)
	{
		Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		Uri displayPhotoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.DISPLAY_PHOTO);
		try
		{
			AssetFileDescriptor fd = HikeMessengerApp.getInstance().getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
			try
			{
				byte[] iconBytes = Utils.readBytes(fd.createInputStream());
				return iconBytes;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		catch (IOException e)
		{
			return null;
		}
	}

	public byte[] getContactThumbnail(long contactId)
	{
		Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);

		Cursor cursor = HikeMessengerApp.getInstance().getContentResolver().query(photoUri, new String[] { Contacts.Photo.PHOTO }, null, null, null);
		if (cursor == null)
		{
			return null;
		}
		try
		{
			if (cursor.moveToFirst())
			{
				byte[] data = cursor.getBlob(0);
				if (data != null)
				{
					try
					{
						byte[] iconBytes = Utils.readBytes(new ByteArrayInputStream(data));
						return iconBytes;
					}
					catch (IOException e)
					{
						e.printStackTrace();
						return null;
					}
				}
			}
		}
		finally
		{
			cursor.close();
		}
		return null;
	}
}
