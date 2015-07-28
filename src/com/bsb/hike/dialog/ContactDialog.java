package com.bsb.hike.dialog;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Spinner;

public class ContactDialog extends HikeDialog
{
	private ViewGroup parentLayout;

	private Spinner spinner;

	public ContactDialog(Context context, int id)
	{
		super(context, id);
	}

	public ContactDialog(Context context, int theme, int id)
	{
		super(context, theme, id);
	}

	public void setViewReferences(ViewGroup parent, Spinner spinner)
	{
		this.parentLayout = parent;
		this.spinner = spinner;
	}

	@Override
	public void dismiss()
	{
		super.dismiss();
		if (parentLayout != null && spinner != null)
		{
			/*
			 * Workaround for Spinner's dialog leaking current window during rotation See issue #4936 : http://code.google.com/p/android/issues/detail?id=4936
			 */
			parentLayout.removeView(spinner);
		}
	}

}