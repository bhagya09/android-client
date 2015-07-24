package com.bsb.hike.modules.stickersearch.ui;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
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

	private boolean shownStickerRecommendFtueTip;

	public StickerTagWatcher(HikeAppStateBaseFragmentActivity activity, ChatThread chathread, EditText editText, int color)
	{
		Logger.i(TAG, "Initialising sticker tag watcher...");

		this.activity = activity;
		this.editText = editText;
		this.color = color;
		this.chatthread = chathread;
		this.stickerPickerListener = (StickerPickerListener) chathread;
		this.colorSpanPool = new ColorSpanPool(this.color, Color.BLACK);
		this.count = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_RECOMMEND_SCROLL_FTUE_COUNT, SHOW_SCROLL_FTUE_COUNT);
		this.shownStickerRecommendFtueTip = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_TIP, false);
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
	public void afterTextChanged(Editable editable)
	{
		this.editable = editable;
		Logger.i(TAG, "afterTextChanged(), " + "string: " + editable);
	}

	@Override
	public void highlightText(int start, int end)
	{
		Logger.d(TAG, "highlightText [" + " start : " + start + ", end : " + end + "]");

		if (end > editable.length())
		{
			end = editable.length();
			Logger.d(TAG, "highlightText [" + " start : " + start + ", end : " + end + "]");
		}

		if (end > start)
		{
			removeAttachedSpans(start, end);
			editable.setSpan(colorSpanPool.getHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	@Override
	public void unHighlightText(int start, int end)
	{
		Logger.d(TAG, "unHighlightText [" + " start : " + start + ", end : " + end + "]");

		if ((end > editable.length()) || (end > StickerSearchConstants.SEARCH_MAX_BROKER_LIMIT))
		{
			end = Math.min(editable.length(), StickerSearchConstants.SEARCH_MAX_BROKER_LIMIT);
			Logger.d(TAG, "unHighlightText [" + " start : " + start + ", end : " + end + "]");
		}

		if (end > start)
		{
			removeAttachedSpans(start, end);
			editable.setSpan(colorSpanPool.getUnHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private void removeAttachedSpans(int start, int end)
	{
		try
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
		catch (Throwable e)
		{
			Logger.e(TAG, "removeAttachedSpans(), Error in getSpans() ", e);
		}
	}

	@Override
	public void showStickerSearchPopup(final String word, final String phrase, final List<Sticker> stickerList)
	{
		if (activity == null)
		{
			Logger.wtf(TAG, "showStickerSearchPopup(), text acivity is null");
			return;
		}

		activity.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if ((stickerList == null) || (stickerList.size() == 0) || !chatthread.isKeyboardOpen())
				{
					Logger.d(TAG, "showStickerSearchPopup(), No sticker list or isKeyboardOpen(): " + chatthread.isKeyboardOpen());
					return;
				}

				Logger.d(TAG, "showStickerSearchPopup() is called: " + stickerList);

				if (stickerRecommendView == null)
				{
					Logger.i(StickerTagWatcher.TAG, "sticker recommend view is null, initialising...");

					stickerRecommendView = (FrameLayout) activity.findViewById(R.id.sticker_recommendation_parent);
					android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) stickerRecommendView.getLayoutParams();
					params.height = StickerSearchUtils.getStickerSize() + 2 * activity.getResources().getDimensionPixelSize(R.dimen.sticker_recommend_padding);
					stickerRecommendView.setLayoutParams(params);
					stickerRecommendView.setOnTouchListener(onTouchListener);

					Logger.i(StickerTagWatcher.TAG, "initialising fragment");

					fragment = StickerRecommendationFragment.newInstance(StickerTagWatcher.this, (ArrayList<Sticker>) stickerList);
					activity.getSupportFragmentManager().beginTransaction()
							.replace(R.id.sticker_recommendation_parent, fragment, HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG).commitAllowingStateLoss();
				}

				dismissStickerRecommendFtueTip();
				((StickerRecommendationFragment) fragment).setAndNotify(word, phrase, stickerList);
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

		if ((editText == null) || (event.getAction() != MotionEvent.ACTION_DOWN))
		{
			return false;
		}

		int clickPosition = StickerSearchUtils.getOffsetForPosition(editText, event.getX(), event.getY());
		StickerSearchManager.getInstance().onClickToSendSticker(clickPosition, true);
		return false;
	}

	@Override
	public void stickerSelected(String word, String phrase, Sticker sticker, int selectedIndex, int size)
	{
		Logger.v(TAG, "stickerSelected(" + word + ", " + phrase + ", " + sticker + ", " + selectedIndex + ")");

		if (stickerPickerListener == null)
		{
			throw new IllegalStateException("sticker picker is null but sticker is selected.");
		}

		String source = (StickerSearchManager.getInstance().getFirstContinuousMatchFound() ? StickerManager.FROM_AUTO_RECOMMENDATION_PANEL
				: StickerManager.FROM_BLUE_TAP_RECOMMENDATION_PANEL);
		StickerManager.getInstance().addRecentStickerToPallete(sticker);
		stickerPickerListener.stickerSelected(sticker, source);

		/*
		 * dismiss sticker search pop-up
		 */
		dismissStickerSearchPopup();

		/*
		 * if its first word or first phrase clear edit text
		 */
		if (StickerSearchManager.getInstance().getFirstContinuousMatchFound())
		{
			clearSearchText();
		}
		else
		/*
		 * if sticker is selected from pop-up, then select all the text
		 */
		{
			selectSearchText();
		}

		// send analytics
		StickerManager.getInstance().sendRecommendationSelectionAnalytics(source, sticker.getStickerId(), sticker.getCategoryId(), (selectedIndex + 1), size,
				StickerSearchManager.getInstance().getNumStickersVisibleAtOneTime(), word, phrase);
	}

	@Override
	public void onCloseClicked(String word, String phrase)
	{
		dismissStickerSearchPopup();

		// send analytics
		StickerManager.getInstance().sendRecommendationRejectionAnalytics(StickerSearchManager.getInstance().getFirstContinuousMatchFound(), StickerManager.REJECT_FROM_CROSS,
				word, phrase);
	}

	@Override
	public void onSettingsClicked()
	{
		IntentFactory.openSettingChat(activity);
		StickerManager.getInstance().sendRecommendationPanelSettingsButtonClickAnalytics();
	}

	@Override
	public void showStickerRecommendFtueTip()
	{
		if (!shownStickerRecommendFtueTip && chatthread.isKeyboardOpen())
		{
			Logger.d(TAG, "show recommend ftue tip");
			chatthread.showStickerRecommendTip();
		}
	}

	@Override
	public void setStickerRecommendFtueSeen()
	{
		if (chatthread.isKeyboardOpen())
		{
			Logger.d(TAG, "set recommend ftue tip seen");
			chatthread.setStickerRecommendFtueTipSeen();
		}
	}

	@Override
	public void dismissStickerRecommendFtueTip()
	{
		Logger.d(TAG, "dismiss recommend ftue tip");
		chatthread.dismissStickerRecommendTip();
	}

	@Override
	public void clearSearchText()
	{
		chatthread.clearComposeText();
	}

	@Override
	public void selectSearchText()
	{
		chatthread.selectAllComposeText();
	}

	public boolean isStickerRecommnedPoupShowing()
	{
		return (stickerRecommendView != null) && (stickerRecommendView.getVisibility() == View.VISIBLE);
	}

	public void releaseResources()
	{
		if ((activity != null) && (fragment != null))
		{
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}

		StickerSearchManager.getInstance().removeStickerSearchListener(this);
		stickerRecommendView = null;
		fragment = null;
		activity = null;

		colorSpanPool.releaseResources();
		colorSpanPool = null;
		stickerPickerListener = null;
	}

	/**
	 * Consuming touch event on sticker recommendation view
	 */
	private OnTouchListener onTouchListener = new OnTouchListener()
	{

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			return true;
		}
	};

	public void sendIgnoreAnalytics()
	{
		if ((fragment != null) && isStickerRecommnedPoupShowing())
		{
			StickerManager.getInstance().sendRecommendationRejectionAnalytics(StickerSearchManager.getInstance().getFirstContinuousMatchFound(), StickerManager.REJECT_FROM_IGNORE,
					((StickerRecommendationFragment) fragment).getTappedWord(), ((StickerRecommendationFragment) fragment).getTaggedPhrase());
		}
	}
}