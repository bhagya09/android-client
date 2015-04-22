package com.bsb.hike.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.Sticker;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ShareUtils
{
	static Intent intent;

	static Context mContext = HikeMessengerApp.getInstance().getApplicationContext();

	public static Intent shareContent(int type, String path)
	{

		switch (type)
		{

		case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
			intent = stickerShare(path);
			break;

		case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
			intent = imageShare(path);
			break;

		case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
			intent = textShare(path);
			break;
		}

		return intent;

	}

	private static void deleteFile(File toDelete)
	{
		if (toDelete != null && toDelete.exists())
		{
			toDelete.deleteOnExit();
		}

	}

	private static void closeFos(FileOutputStream fos)
	{
		if (fos != null)
		{
			try
			{
				fos.close();
			}
			catch (IOException e)
			{
				// Do nothing
				e.printStackTrace();
			}
		}

	}

	// will give the scale ratio based on the screen width and screen height
	private static float scaleRatio(int imgWidth, int imgHeight)
	{
		float sRatio;
		int screenWidth = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
		int screenHeight = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
		if (screenHeight < screenWidth)
		{
			if (imgHeight != 0)
				sRatio = (float) screenHeight / imgHeight;
			else
				sRatio = 1;
		}
		else
		{
			if (imgWidth != 0)
				sRatio = (float) screenWidth / imgWidth;
			else
				sRatio = 1;
		}
		if (sRatio > 1)
		{
			sRatio = 1;
		}
		return sRatio;

	}

	private static View setViewImage(String filePath)
	{

		String imgHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_HEADING, mContext.getString(R.string.image_share_heading));
		String imgDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_DESCRIPTION, mContext.getString(R.string.image_share_description));

		View share = LayoutInflater.from(mContext).inflate(R.layout.image_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.user_image);
		Bitmap bmp = HikeBitmapFactory.decodeFile(filePath);
		int imgWidth = bmp.getWidth();
		int imgHeight = bmp.getHeight();
		// gives the scaling ratio for the image
		float sRatio = scaleRatio(imgWidth, imgHeight);
		// will scale the image to the 0.7 of the screen size proportionally for width and height
		bmp = Bitmap.createScaledBitmap(bmp, (int) (imgWidth * sRatio * 0.7), (int) (imgHeight * sRatio * 0.7), true);
		image.setImageBitmap(bmp);
		TextView heading = (TextView) share.findViewById(R.id.imageShareHeading);
		heading.setText(imgHead);
		TextView tv = (TextView) share.findViewById(R.id.imageShareDescription);
		tv.setText(Html.fromHtml(imgDesc));

		return share;
	}

	private static Intent imageShare(String imagePath)
	{
		Logger.d("ShareUtils", "imageShareMethod");
		File shareImageFile = null;
		FileOutputStream fos = null;
		Intent imageIntent;
		try
		{
			shareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareImageFile);
			View share = setViewImage(imagePath);
			Bitmap shB = Utils.undrawnViewToBitmap(share);

			shB.compress(CompressFormat.JPEG, 100, fos);

			fos.flush();

			String imgCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_CAPTION, mContext.getString(R.string.image_share_caption));
			imageIntent = IntentFactory.shareIntent("image/jpeg", "file://" + shareImageFile.getAbsolutePath(), imgCap, HikeConstants.Extras.ShareTypes.IMAGE_SHARE, true);
			return imageIntent;
		}

		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(mContext, mContext.getString(com.bsb.hike.R.string.error_sharing), Toast.LENGTH_SHORT).show();
			return null;
		}
		finally
		{
			closeFos(fos);
			deleteFile(shareImageFile);
		}
	}

	private static Intent textShare(String text)
	{
		String textHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_HEADING, mContext.getString(R.string.text_share_heading));
		String textCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_CAPTION, mContext.getString(R.string.text_share_caption));
		Logger.d("ShareUtils", "smsShare");
		text = text + "\n\n" + textHead + "\n" + textCap;
		Intent textIntent = IntentFactory.shareIntent("text/plain", null, text, HikeConstants.Extras.ShareTypes.TEXT_SHARE, true);
		return textIntent;

	}

	private static View setViewSticker(String stickerFilePath)
	{
		String stiHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_HEADING, mContext.getString(R.string.sticker_share_heading));
		String stiDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_DESCRIPTION, mContext.getString(R.string.sticker_share_description));

		View share = LayoutInflater.from(mContext).inflate(R.layout.sticker_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.sticker_image);

		Bitmap bmp = BitmapFactory.decodeFile(stickerFilePath);
		image.setImageBitmap(bmp);

		TextView heading = (TextView) share.findViewById(R.id.stickerShareHeading);
		heading.setText(stiHead);

		TextView tv = (TextView) share.findViewById(com.bsb.hike.R.id.stickerShareDescription);
		tv.setText(Html.fromHtml(stiDesc));

		return share;
	}

	private static Intent stickerShare(String stickerPath)
	{
		Logger.d("ShareUtils", "stickerShare");
		File shareStickerFile = null;
		FileOutputStream fos = null;
		Intent stickerIntent;
		try
		{
			shareStickerFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareStickerFile);

			View share = setViewSticker(stickerPath);

			Bitmap shB = Utils.undrawnViewToBitmap(share);
			shB.compress(CompressFormat.JPEG, 100, fos);

			fos.flush();
			String stiCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_CAPTION, mContext.getString(R.string.sticker_share_caption));
			stickerIntent = IntentFactory.shareIntent("image/jpeg", "file://" + shareStickerFile.getAbsolutePath(), stiCap, HikeConstants.Extras.ShareTypes.STICKER_SHARE, true);
			return stickerIntent;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		finally
		{
			closeFos(fos);
			deleteFile(shareStickerFile);

		}

	}

}
