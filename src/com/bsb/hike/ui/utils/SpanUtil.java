package com.bsb.hike.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;

public class SpanUtil
{
	public static ImageSpan getImageSpanFromTextView(Context context, int layoutId, int textviewId, String text)
	{
		LayoutInflater lf = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View view = lf.inflate(layoutId, null);
		TextView textView = (TextView) view.findViewById(textviewId);
		textView.setText(text); // set text
		return getImageSpan(context, view);
	}

	private static ImageSpan getImageSpan(Context context, View view)
	{
		// create bitmap drawable for imagespan
		BitmapDrawable bmpDrawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), HikeBitmapFactory.getBitMapFromTV(view));
		if (bmpDrawable == null)
		{
			return null;
		}
		bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
		return new ImageSpan(bmpDrawable);
	}

}
