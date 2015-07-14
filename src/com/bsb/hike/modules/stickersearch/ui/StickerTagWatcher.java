package com.bsb.hike.modules.stickersearch.ui;

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
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.listeners.IStickerRecommendFragmentListener;
import com.bsb.hike.modules.stickersearch.listeners.IStickerSearchListener;
import com.bsb.hike.modules.stickersearch.ui.colorspan.ColorSpanPool;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import static com.bsb.hike.modules.stickersearch.StickerSearchConstants.*;

public class StickerTagWatcher implements TextWatcher, IStickerSearchListener, OnTouchListener, IStickerRecommendFragmentListener
{
	public static final String TAG = StickerTagWatcher.class.getSimpleName();

	private HikeAppStateBaseFragmentActivity activity;

	private StickerPickerListener stickerPickerListener;

	private ChatThread chatthread;

	private EditText editText;

	private int color;

	private Editable editable;

	private FrameLayout stickerRecommendView;

	private Fragment fragment;

	private int count;

	private ColorSpanPool colorSpanPool;

	public StickerTagWatcher(HikeAppStateBaseFragmentActivity activity, ChatThread chathread, EditText editText, int color)
	{
		this.activity = activity;
		this.editText = editText;
		this.color = color;
		this.chatthread = chathread;
		this.stickerPickerListener = (StickerPickerListener) chathread;
		colorSpanPool = new ColorSpanPool(this.color, Color.BLACK);
		this.count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_RECOMMEND_SCROLL_FTUE_COUNT, SHOW_SCROLL_FTUE_COUNT);
		StickerSearchManager.getInstance().addStickerSearchListener(this);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		Logger.i(TAG, "beforeTextChanged(), " + "CharSequence: " + s + ", [start: " + start + ", count : " + count + ", after : " + after + "]");
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		Logger.i(TAG, "onTextChanged(), " + "CharSequence: " + s + ", [start: " + start + ", before : " + before + ", count : " + count + "]");
		StickerSearchManager.getInstance().onTextChanged(s, start, before, count);
		colorSpanPool.unMarkAll();
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		this.editable = s;
		Logger.i(TAG, "afterTextChanged(), " + "string: " + editable);
	}

	@Override
	public void highlightText(final int start, final int end)
	{
		if (activity == null)
		{
			return;
		}
		activity.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				Logger.d(TAG, "highlightText [" + " start : " + start + ", end : " + end + "]");
				removeAttachedSpans(start, end);
				editable.setSpan(colorSpanPool.getHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			}
		});
	}

	@Override
	public void unHighlightText(final int start, final int end)
	{
		if (activity == null)
		{
			return;
		}
		activity.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				Logger.d(TAG, "unHighlightText [" + " start : " + start + ", end : " + end + "]");
				removeAttachedSpans(start, end);
				editable.setSpan(colorSpanPool.getUnHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		});
	}

	private void removeAttachedSpans(int start, int end)
	{
		ForegroundColorSpan[] spans = editable.getSpans(start, end, ForegroundColorSpan.class);
		if (spans != null)
		{
			for (int i = 0; i < spans.length; i++)
			{
				editable.removeSpan(spans[i]);
			}
		}
	}

	@Override
	public void showStickerSearchPopup(final List<Sticker> stickerList)
	{
		if (activity == null)
		{
			return;
		}
		activity.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if (stickerList == null || stickerList.size() == 0 || !chatthread.isKeyboardOpen() || isStickerRecommnedPoupShowing())
				{
					Logger.d(
							TAG,
							"showStickerSearchPopup(), No sticker list or popup is already shown: " + isStickerRecommnedPoupShowing() + ", isKeyboardOpen(): "
									+ chatthread.isKeyboardOpen());
					return;
				}

				chatthread.closeStickerRecommendTip();

				Logger.d(TAG, "showStickerSearchPopup() is called: " + stickerList);

				if (stickerRecommendView == null)
				{
					stickerRecommendView = (FrameLayout) activity.findViewById(R.id.sticker_recommendation_parent);
					android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) stickerRecommendView.getLayoutParams();
					params.height = StickerSearchUtils.getStickerSize() + 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_recommend_padding);
					stickerRecommendView.setLayoutParams(params);
					stickerRecommendView.setOnTouchListener(onTouchListener);

					fragment = activity.getSupportFragmentManager().findFragmentByTag(HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG);

					if (fragment == null)
					{
						Logger.d(StickerTagWatcher.TAG, "sticker recommnd fragment is null");

						fragment = StickerRecommendationFragment.newInstance(StickerTagWatcher.this, (ArrayList<Sticker>) stickerList);
						activity.getSupportFragmentManager().beginTransaction()
								.replace(R.id.sticker_recommendation_parent, fragment, HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG).commitAllowingStateLoss();
					}
				}

				((StickerRecommendationFragment) fragment).setAndNotify(stickerList);
				stickerRecommendView.setVisibility(View.VISIBLE);
				showFtueAnimation();
			}
		});
	}

	public void showFtueAnimation()
	{
		if (count > 0)
		{
			stickerRecommendView.removeCallbacks(scrollRunnable);
			stickerRecommendView.postDelayed(scrollRunnable, SCROLL_DELAY);
		}
	}

	private Runnable scrollRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			if (isStickerRecommnedPoupShowing() && fragment != null)
			{
				boolean shown = ((StickerRecommendationFragment) fragment).showFtueAnimation();
				if (shown)
				{
					count--;
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKER_RECOMMEND_SCROLL_FTUE_COUNT, count);
				}
			}
		}
	};

	@Override
	public void dismissStickerSearchPopup()
	{
		if (activity != null)
		{
			activity.runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (stickerRecommendView != null)
					{
						Logger.i(TAG, "dismissStickerSearchPopup()");
						stickerRecommendView.setVisibility(View.INVISIBLE);
					}
				}
			});
		}
		else
		{
			Logger.e(TAG, "dismissStickerSearchPopup(), activity is null.");
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		Logger.i(TAG, "onTouch() called " + editText);
		if (editText == null || (event.getAction() != MotionEvent.ACTION_DOWN))
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
		StickerManager.getInstance().addRecentStickerToPallete(sticker);
		stickerPickerListener.stickerSelected(sticker, StickerManager.FROM_RECOMMENDATION);
		dismissStickerSearchPopup();
		if (StickerSearchManager.getInstance().getFirstContinuousMatchFound())
		{
			clearSearchText();
		}
		else
		{
			highlightSearchText();
		}
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
		if (activity != null && fragment != null)
		{
			activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
			activity.getSupportFragmentManager().executePendingTransactions();
		}
		StickerSearchManager.getInstance().removeStickerSearchListener(this);
		stickerRecommendView = null;
		fragment = null;

		colorSpanPool.releaseResources();
		colorSpanPool = null;
	}

	/**
	 * Consuming touch event on this view
	 */
	private OnTouchListener onTouchListener = new OnTouchListener()
	{

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			return true;
		}
	};

	@Override
	public void showStickerRecommendFtue()
	{
		chatthread.showStickerRecommendTip();
	}

	@Override
	public void clearSearchText()
	{
		chatthread.clearComposeText();
	}

	@Override
	public void highlightSearchText()
	{
		chatthread.selectAllComposeText();
	}
}