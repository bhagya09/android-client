package com.bsb.hike.platform;

import android.content.Context;

import com.bsb.hike.platform.nativecards.HikeDailyViewHolder;
import com.bsb.hike.platform.nativecards.ImageCardHolder;
import com.bsb.hike.platform.nativecards.JFLViewHolder;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.platform.nativecards.ViewHolder;

/**
 * Created by pushkargupta on 15/04/16. This class gets the JsonObject and returns the view
 */
public class ViewHolderFactory {
	private static final String TAG = ViewHolderFactory.class.getSimpleName();

	private Context mContext;

	public ViewHolderFactory(Context context) {
		mContext = context;

	}

	private NativeCardManager.NativeCardType[] cardTypes = NativeCardManager.NativeCardType.values();


	public ViewHolder getViewHolder(int type) {
		switch (cardTypes[type]) {
			case HIKE_DAILY:
				return new HikeDailyViewHolder(mContext);
			case JFL:
				return new JFLViewHolder(mContext);
			case IMAGE_CARD:
				return new ImageCardHolder(mContext);
			default:
				return null;
		}
	}

}
