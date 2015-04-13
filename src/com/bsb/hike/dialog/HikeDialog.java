package com.bsb.hike.dialog;

import android.app.Dialog;
import android.content.Context;

/**
 * @author piyush
 * 
 */
public class HikeDialog extends Dialog
{
	public final int id;

	public Object data;

	public HikeDialog(Context context, int theme, int id)
	{
		super(context, theme);
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId()
	{
		return id;
	}
	
}
