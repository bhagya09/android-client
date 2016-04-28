package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.view.RoundedDrawable;

/**
 *   Abstract class for thumbnails. Any Feature added later must extend this thumbnail class to preview its effect on a thumbnail.
 * 
 * 	@author akhiltripathi
 * 
 * 
 */

public abstract class EffectItemLinearLayout extends LinearLayout
{
	private TextView label;

	private ImageView icon;

	private Bitmap postInflate;

	public EffectItemLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public EffectItemLinearLayout(Context context)
	{
		super(context);
	}


	
	public String getText()
	{
		return (String) this.label.getText();
	}

	public void setText(String text)
	{
		this.label.setGravity(Gravity.CENTER);
		this.label.setText(text);
		this.label.invalidate();
		this.invalidate();
	}

	public void setImage(Drawable drawable)
	{
		this.icon.setImageDrawable(drawable);
		this.icon.invalidate();
		this.invalidate();
	}

	public void setImage(Bitmap bitmap)
	{
		if (this.icon != null && bitmap !=null)
		{
			this.icon.setImageBitmap(bitmap);
			this.icon.invalidate();
		}
		else if(this.icon !=null)
		{
			//To Do Out Of Memory Handling
		}
		else
			postInflate = bitmap;
		this.invalidate();
	}

	public Bitmap getIcon()
	{
		if (icon != null)
		{
			return ((RoundedDrawable) this.icon.getDrawable()).toBitmap();
		}
		else
		{
			return null;
		}

	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		try
		{
			label = (TextView) findViewById(R.id.previewText);
		}
		catch (Exception e)
		{
			// do nothing
		}
		icon = (ImageView) findViewById(R.id.previewIcon);
		if (postInflate != null)
		{
			setImage(postInflate);
		}
	}

}

