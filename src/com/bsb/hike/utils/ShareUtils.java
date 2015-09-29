package com.bsb.hike.utils;

import java.io.File;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.tasks.ShareBitmapTask;

public class ShareUtils
{
	static Context mContext = HikeMessengerApp.getInstance().getApplicationContext();

	public static void shareContent(int type, String path, String pkgName, boolean isFromChatHead)
	{

		switch (type)
		{

		case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
			String stiDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_DESCRIPTION, mContext.getString(R.string.sticker_share_description));
			String stiCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.STICKER_CAPTION, mContext.getString(R.string.sticker_share_caption));
			imageShare(path, null, stiDesc, stiCap, pkgName, true, isFromChatHead);
			break;

		case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
			String imgHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_HEADING, mContext.getString(R.string.image_share_heading));
			String imgDesc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_DESCRIPTION, mContext.getString(R.string.image_share_description));
			String imgCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.IMAGE_CAPTION, mContext.getString(R.string.image_share_caption));
			imageShare(path, imgHead, imgDesc, imgCap, pkgName, false, isFromChatHead);
			break;

		case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
			textShare(path, pkgName, isFromChatHead);
			break;
		}

		return;

	}
	
	public static void shareContent(int type, String path, String pkgName)
	{
		switch (type)
		{
		case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
			textShare(path, pkgName);
			break;
		}

		return;

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
		return sRatio;

	}

	private static View setViewSticker(String filePath, String stiDesc)
	{
		View share = LayoutInflater.from(mContext).inflate(R.layout.sticker_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.user_image);
		Bitmap bmp = HikeBitmapFactory.decodeFile(filePath);
		if (bmp != null)
		{
			int imgWidth = bmp.getWidth();
			int imgHeight = bmp.getHeight();
			// gives the scaling ratio for the image
			float sRatio = scaleRatio(imgWidth, imgHeight);
			// will scale the image to the 0.7 of the screen size proportionally for width and height
			if (sRatio > 1)
			{
				bmp = Bitmap.createScaledBitmap(bmp, imgWidth, imgHeight, true);
			}
			else
			{
				bmp = Bitmap.createScaledBitmap(bmp, (int) (imgWidth * sRatio * 0.7), (int) (imgHeight * sRatio * 0.7), true);
			}
			image.setImageBitmap(bmp);
			TextView stickerDesc = (TextView) share.findViewById(R.id.stickerShareDescription);
			stickerDesc.setText(Html.fromHtml(stiDesc));
			return share;
		}
		else
		{
			return null;
		}
	}

	private static View setViewImage(String filePath, String imgHead, String imgDesc)
	{
		View share = LayoutInflater.from(mContext).inflate(R.layout.image_share_layout, null);
		ImageView image = (ImageView) share.findViewById(R.id.user_image);
		Bitmap bmp = HikeBitmapFactory.decodeFile(filePath);

		if (bmp != null)
		{
			int imgWidth = bmp.getWidth();
			int imgHeight = bmp.getHeight();
			// gives the scaling ratio for the image
			float sRatio = scaleRatio(imgWidth, imgHeight);
			// will scale the image to the 0.7 of the screen size proportionally for width and height
			if (sRatio > 1)
			{
				bmp = Bitmap.createScaledBitmap(bmp, imgWidth, imgHeight, true);
			}
			else
			{
				bmp = Bitmap.createScaledBitmap(bmp, (int) (imgWidth * sRatio * 0.7), (int) (imgHeight * sRatio * 0.7), true);
			}
			image.setImageBitmap(bmp);
			TextView heading = (TextView) share.findViewById(R.id.imageShareHeading);
			heading.setText(imgHead);
			TextView tv = (TextView) share.findViewById(R.id.imageShareDescription);
			tv.setText(Html.fromHtml(imgDesc));
			return share;
		}
		else
		{
			return null;
		}
	}

	private static void imageShare(String imagePath, String imgHead, String imgDesc, String imgCap, String pkgName, boolean isSticker, boolean isFromChatHead)
	{
		View share;
		if (isSticker)
		{
			share = setViewSticker(imagePath, imgDesc);
		}
		else
		{
			share = setViewImage(imagePath, imgHead, imgDesc);
		}
		if (share != null)
		{
			ShareBitmapTask task = new ShareBitmapTask(share, mContext, imgCap, pkgName, isFromChatHead);
			task.execute();
		}
	}

	private static void textShare(String text, String pkgName, boolean isFromChatHead)
	{
		String textHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_HEADING, mContext.getString(R.string.text_share_heading));
		String textCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_CAPTION, mContext.getString(R.string.text_share_caption));
		text = text + "\n\n" + textHead + "\n" + textCap;
		Intent textIntent = IntentFactory.shareIntent("text/plain", null, text, HikeConstants.Extras.ShareTypes.TEXT_SHARE, pkgName, isFromChatHead);
		mContext.startActivity(textIntent);
	}

	private static void textShare(String text, String pkgName)
	{
		String textHead = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_HEADING, mContext.getString(R.string.text_share_heading));
		String textCap = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.TEXT_CAPTION, mContext.getString(R.string.text_share_caption));
		text = text + "\n\n" + textHead + "\n" + textCap;
		Intent textIntent = IntentFactory.shareIntent("text/plain", null, text, HikeConstants.Extras.ShareTypes.TEXT_SHARE, pkgName, false);
		mContext.startActivity(textIntent);
	}
}
