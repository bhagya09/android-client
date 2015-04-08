package com.bsb.hike.photos;

import java.io.File;

import android.graphics.Bitmap;

public interface HikePhotosListener
{
	void onComplete(File f);

	void onComplete(Bitmap bmp);

	void onFailure();
}
