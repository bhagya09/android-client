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

	HikeAdaptxtEditTextEventListner textEventListener;

	AdaptxtKeyboordVisibilityStatusListner keyboardVisibilityListener;

	public HikeCustomKeyboard(Activity host, View viewHolder, int etype, HikeAdaptxtEditTextEventListner listener, AdaptxtKeyboordVisibilityStatusListner listener2)
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
		AdaptxtEditTextEventListner listener = null;

		if (textEventListener != null)
			listener = new AdaptxtEditTextEventListner()
			{
				@Override
				public void onReturnAction(int i, AdaptxtEditText adaptxtEditText) {
					textEventListener.onReturnAction(resId, i);
				}

				@Override
				public void onAdaptxtclick(View arg0)
				{
					// TODO Auto-generated method stub
				}

				@Override
				public void onAdaptxtTouch(View arg0, MotionEvent arg1)
				{
					// TODO Auto-generated method stub
				}

				@Override
				public void onAdaptxtFocusChange(View arg0, boolean arg1)
				{
					// TODO Auto-generated method stub
				}
			};
		super.registerEditText(resId, eType, listener, keyboardVisibilityListener);
	}

	public void registerEditText(AdaptxtEditText text)
	{
		registerEditText(text, this.eType);
	}

	public void registerEditText(final AdaptxtEditText text, int eType)
	{
		AdaptxtEditTextEventListner listener = null;

		if (textEventListener != null)
			listener = new AdaptxtEditTextEventListner()
			{
				@Override
				public void onReturnAction(int i, AdaptxtEditText adaptxtEditText) {
					textEventListener.onReturnAction(text.getId(), i);
				}

				@Override
				public void onAdaptxtclick(View arg0)
				{
					// TODO Auto-generated method stub
				}

				@Override
				public void onAdaptxtTouch(View arg0, MotionEvent arg1)
				{
					// TODO Auto-generated method stub
				}

				@Override
				public void onAdaptxtFocusChange(View arg0, boolean arg1)
				{
					// TODO Auto-generated method stub
				}
			};
		super.registerEditText(text, eType, listener, keyboardVisibilityListener);
	}
}
