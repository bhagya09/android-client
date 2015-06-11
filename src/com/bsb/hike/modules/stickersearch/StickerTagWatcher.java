package com.bsb.hike.modules.stickersearch;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerTagWatcher implements TextWatcher, IStickerSearchListener, OnTouchListener, IStickerRecommendFragmentListener
{
	public static final String TAG = "stickerSearch";

	private HikeAppStateBaseFragmentActivity activity;

	private StickerPickerListener stickerPickerListener;
	
	private ChatThread chatthread;

	EditText editText;

	int color;

	private Editable editable;

	private FrameLayout stickerRecommendView;
	
	private Fragment fragment ;

	public StickerTagWatcher(HikeAppStateBaseFragmentActivity activity, ChatThread chathread, EditText editText, int color)
	{
		this.activity = activity;
		this.editText = editText;
		this.color = color;
		this.chatthread = chathread;
		this.stickerPickerListener = (StickerPickerListener) chathread;
		StickerSearchManager.getInstance().addStickerSearchListener(this);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		Logger.d(TAG, "before text changed " + "string: " + s + " start: " + start + " count : " + count + " after : " + after);
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		Logger.d(TAG, "on text changed " + "string: " + s + " start: " + start + " before : " + before + " count : " + count);
		StickerSearchManager.getInstance().onTextChanged(s, start, before, count);
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		this.editable = s;
		StickerSearchManager.getInstance().afterTextChanged(s);
	}

	@Override
	public void highlightText(int start, int end)
	{
		Logger.d(TAG, "unhighlight called  : " + " start : " + start + " end : " + end);
		editable.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	@Override
	public void unHighlightText(int start, int end)
	{
		editable.setSpan(new ForegroundColorSpan(Color.BLACK), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	@Override
	public void showStickerSearchPopup(List<Sticker> stickerList)
	{
		if (stickerList == null)
		{
			Logger.d(TAG, " no sticker list or keyboard not open");
			return;
		}

		Logger.d(TAG, " on show sticker popup is called");

		fragment = activity.getSupportFragmentManager().findFragmentByTag(HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG);

		if (fragment == null)
		{
			Logger.d("anubhav", "sticker recommnd fragment is null");
			stickerRecommendView = (FrameLayout) activity.findViewById(R.id.sticker_recommendation_parent);
			android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) stickerRecommendView.getLayoutParams();
			params.height = StickerSearchUtils.getStickerSize() + 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_recommend_padding);
			stickerRecommendView.setLayoutParams(params);
			fragment = StickerRecommendationFragment.newInstance(this, (ArrayList<Sticker>) stickerList);
			activity.getSupportFragmentManager().beginTransaction().replace(R.id.sticker_recommendation_parent, fragment, HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG)
					.commitAllowingStateLoss();
		}
		stickerRecommendView.setVisibility(View.VISIBLE);
		((StickerRecommendationFragment) fragment).setAndNotify(stickerList);
	}

	@Override
	public void dismissStickerSearchPopup()
	{
		if (stickerRecommendView != null)
		{
			Logger.d(TAG, "on dismiss is called");
			stickerRecommendView.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		Logger.d(TAG, "ontouch called " + editText);

		if (editText == null)
		{
			return false;
		}
		int clickPosition = StickerSearchUtils.getOffsetForPosition(editText, event.getX(), event.getY());
		StickerSearchManager.getInstance().onClickToSendSticker(clickPosition);
		return false;
	}

	@Override
	public void stickerSelected(Sticker sticker)
	{
		if (stickerPickerListener == null)
		{
			throw new IllegalStateException("sticker picker is null but sticker is selected");
		}
		stickerPickerListener.stickerSelected(sticker, StickerManager.FROM_RECOMMENDATION);
		dismissStickerSearchPopup();
	}

	@Override
	public void onCloseClicked()
	{
		dismissStickerSearchPopup();
	}

	@Override
	public void onSettingsClicked()
	{
		IntentFactory.openSettingChat(activity);
	}

	public boolean isStickerRecommnedPoupShowing()
	{
		if (stickerRecommendView == null)
		{
			return false;
		}
		return stickerRecommendView.getVisibility() == View.VISIBLE;
	}

	public void releaseResources()
	{
		StickerSearchManager.getInstance().removeStickerSearchListener(this);
		stickerRecommendView = null;
	}
}