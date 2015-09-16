package com.bsb.hike.dialog;

import com.bsb.hike.R;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;

/**
 * @author piyush
 * 
 */
public class HikeDialog extends Dialog
{
	public final int id;

	public Object data;
	
	public HikeDialog(Context context,int id)
	{
		this(context, R.style.Theme_CustomDialog, id);
	}

	public HikeDialog(Context context, int theme, int id)
	{
		this(context, theme, id, true);
	}
	
	/**
	 * <p>
	 * Often you will want to have a Dialog display on top of the current input method, because there is no reason for it to accept text. You can do this by setting the
	 * {@link WindowManager.LayoutParams#FLAG_ALT_FOCUSABLE_IM WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM} window flag (assuming your Dialog takes input focus, as it the
	 * default) with the following code:
	 * 
	 * <pre>
	 * getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	 * </pre>
	 */
	public HikeDialog (Context context, int theme, int id, boolean showOverKeyboard)
	{
		super(context, theme);
		this.id = id;
		
		if (showOverKeyboard)
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		}
	}

	/**
	 * @return the id
	 */
	public int getId()
	{
		return id;
	}
	
}
