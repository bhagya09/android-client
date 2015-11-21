package com.bsb.hike.modules.kpt;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.view.AdaptxtEditText;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

public class HikeCustomKeyboard extends CustomKeyboard
{

	int eType;

	AdaptxtEditTextEventListner textEventListener;

	AdaptxtKeyboordVisibilityStatusListner keyboardVisibilityListener;

	public HikeCustomKeyboard(Activity host, View viewHolder, int etype, AdaptxtEditTextEventListner listener, AdaptxtKeyboordVisibilityStatusListner listener2)
	{
		super(host, viewHolder);
		this.textEventListener = listener;
		this.eType = etype;
		this.keyboardVisibilityListener = listener2;
	}

	public void registerEditText(int resId)
	{
		registerEditText(resId, this.eType);
	}

	public void registerEditText(final int resId, int eType)
	{
		super.registerEditText(resId, eType, textEventListener, keyboardVisibilityListener);
	}

	public void registerEditText(AdaptxtEditText text)
	{
		registerEditText(text, this.eType);
	}

	public void registerEditText(final AdaptxtEditText text, int eType)
	{
		super.registerEditText(text, eType, textEventListener, keyboardVisibilityListener);
	}

	@Override
	public void showCustomKeyboard(View view, boolean b)
	{
		super.showCustomKeyboard(view, b);
	}
}
