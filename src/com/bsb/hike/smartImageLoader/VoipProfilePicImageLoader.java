package com.bsb.hike.smartImageLoader;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.adapters.ProfileAdapter;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

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
	protected void setDefaultAvatar(ImageView imageView, String data)
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
