package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.bsb.hike.R;

public class VoipProfilePicImageLoader extends ProfilePicImageLoader
{

	private ScaleType defaultAvatarScaleType;

	private int defaultAvatarWidth;

	private int defaultAvatarHeight;

	public VoipProfilePicImageLoader(Context ctx, int imageSize) 
	{
		super(ctx, imageSize);
		// TODO Auto-generated constructor stub
	}
	
	public void setDefaultAvatarScaleType(ScaleType scaleType)
	{
		defaultAvatarScaleType = scaleType;
	}

	public void setDefaultAvatarBounds(int width, int height)
	{
		defaultAvatarWidth = width;
		defaultAvatarHeight = height;
	}

	@Override
	protected void setDefaultAvatar(ImageView imageView, String data, Object ignored)
	{
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)imageView.getLayoutParams();
		layoutParams.width = defaultAvatarWidth;
		layoutParams.height = defaultAvatarHeight;
		imageView.setLayoutParams(layoutParams);
		if(defaultAvatarScaleType!=null)
		{
			imageView.setScaleType(defaultAvatarScaleType);
		}
		imageView.setImageResource(R.drawable.ic_avatar_voip_hires);
	}
}
