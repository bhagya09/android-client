package com.bsb.hike.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;

/**
 * 
 * @author himanshu 
 * 
 * This class provides details to the invite friends activity.
 *
 */
public class NuxInviteFriends
{
	private String mainReward;

	private String subTextReward;

	private String buttext;

	private boolean showSkipButton;

	private Bitmap bitmap;

	StringToBitmap mmStringToBitmap;

	Boolean isNuxSkippable;

	public NuxInviteFriends(String mainReward, String subTextReward, String buttext, String image, boolean skip_toggle_button, boolean isNuxSkippable)
	{
		this.mainReward = mainReward;
		this.subTextReward = subTextReward;
		this.buttext = buttext;
		this.showSkipButton = skip_toggle_button;

		this.isNuxSkippable = isNuxSkippable;
		if (!TextUtils.isEmpty(image))
		{
			// mmStringToBitmap = new StringToBitmap(image);
			// HikeHandlerUtil.getInstance().postRunnableWithDelay(mmStringToBitmap, 0);
			bitmap = HikeBitmapFactory.stringToBitmap(image);
		}
		else
		{
			bitmap = BitmapFactory.decodeResource(HikeMessengerApp.getInstance().getResources(), R.drawable.art_default_stickers_nux);
		}
	}

	class StringToBitmap implements Runnable
	{
		private String base64image = null;

		public StringToBitmap(String base64)
		{
			base64image = base64;
		}

		@Override
		public void run()
		{
			bitmap = HikeBitmapFactory.stringToBitmap(base64image);
		}

	}

	/**
	 * @return the mainReward
	 */
	public String getRewardTitle()
	{
		return mainReward;
	}

	/**
	 * @return the subTextReward
	 */
	public String getRewardSubText()
	{
		return subTextReward;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the skip_toggle_button
	 */
	public boolean showSkipButton()
	{
		return showSkipButton;
	}

	/**
	 * @return the bitmap
	 */
	public Bitmap getImageBitmap()
	{
		return bitmap;
	}

	public boolean isNuxSkippable()
	{
		return isNuxSkippable;
	}

}
