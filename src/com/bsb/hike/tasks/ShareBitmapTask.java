package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.ChatHeadViewManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.view.View;

public class ShareBitmapTask extends AsyncTask<Void, Void, Void>
{
	View share;

	Context mContext;

	boolean isFromChatHead;
	
	File shareImageFile = null;
	
	String imgCap;
	
	String pkgName;

	public ShareBitmapTask(View view, Context context, String imgCap, String pkgName, boolean isFromChatHead)
	{
		share = view;
		mContext = context;
		this.isFromChatHead = isFromChatHead;
		this.imgCap = imgCap;
		this.pkgName = pkgName;
	}

	@Override
	protected void onPostExecute(Void result)
	{
		super.onPostExecute(result);
		Intent intent = IntentFactory.shareIntent("image/jpeg", "file://" + shareImageFile.getAbsolutePath(), imgCap, HikeConstants.Extras.ShareTypes.IMAGE_SHARE, pkgName,
				isFromChatHead);
		if (intent != null)
		{
			if (isFromChatHead)
			{
				if (ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS).contains(ChatHeadViewManager.sharableActivePackage))
				{
					mContext.startActivity(intent);
				}
			}
			else
			{
				mContext.startActivity(intent);
				HikeMessengerApp.getPubSub().publish(HikePubSub.SHARED_WHATSAPP, true);
			}
		}
		deleteFile(shareImageFile);
	}

	private void deleteFile(File toDelete)
	{
		if (toDelete != null && toDelete.exists())
		{
			toDelete.deleteOnExit();
		}
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		FileOutputStream fos = null;
		try
		{
			shareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
			fos = new FileOutputStream(shareImageFile);
			Bitmap shB = Utils.undrawnViewToBitmap(share);
			shB.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
		}
		catch (Exception e)
		{
			Logger.d("ShareBitmapTask", "Exception");
		}
		finally
		{
			Utils.closeStreams(fos);
		}
		return null;
	}
}
