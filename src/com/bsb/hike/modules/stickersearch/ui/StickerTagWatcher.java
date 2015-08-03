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
import android.util.Pair;
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
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
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

	private static int MAXIMUM_SEARCH_TEXT_BROKER_LIMIT;

	private HikeAppStateBaseFragmentActivity activity;

	private StickerPickerListener stickerPickerListener;

	private ChatThread chatthread;

	private EditText editText;

	private int color;

	private Editable editable;

	private FrameLayout stickerRecommendView;

	private Fragment fragment;

	private Fragment fragmentFtue;

	private int count;

	private ColorSpanPool colorSpanPool;

	private boolean shownStickerRecommendFtueTip;

	public StickerTagWatcher(HikeAppStateBaseFragmentActivity activity, ChatThread chathread, EditText editText, int color)
	{
		Logger.i(TAG, "Initialising sticker tag watcher...");

		MAXIMUM_SEARCH_TEXT_BROKER_LIMIT = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER,
				StickerSearchConstants.MAXIMUM_SEARCH_TEXT_BROKER_LIMIT);

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
		Logger.i(TAG, "afterTextChanged(), current text: " + editable);
	}

	@Override
	public void highlightText(int start, int end)
	{
		Logger.d(TAG, "highlightText [" + " start : " + start + ", end : " + end + "]");

		if (editable == null)
		{
			Logger.wtf(TAG, "highlightText [" + start + " - " + end + "]");
			return;
		}

		if (end > editable.length())
		{
			end = editable.length();
			Logger.d(TAG, "highlightText [" + " start : " + start + ", end : " + end + "]");
		}

		if (end > start)
		{
			try
			{
				removeAttachedSpans(start, end);
				editable.setSpan(colorSpanPool.getHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Error while executing highlight spanning !!!", e);
			}
		}
	}

	@Override
	public void unHighlightText(int start, int end)
	{
		Logger.d(TAG, "unHighlightText [" + " start : " + start + ", end : " + end + "]");

		if (editable == null)
		{
			Logger.wtf(TAG, "unHighlightText [" + start + " - " + end + "]");
			return;
		}

		if ((end > editable.length()) || (end > MAXIMUM_SEARCH_TEXT_BROKER_LIMIT))
		{
			end = Math.min(editable.length(), MAXIMUM_SEARCH_TEXT_BROKER_LIMIT);
			Logger.d(TAG, "unHighlightText [" + " start : " + start + ", end : " + end + "]");
		}

		if (end > start)
		{
			try
			{
				removeAttachedSpans(start, end);
				editable.setSpan(colorSpanPool.getUnHighlightSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Error while executing unhighlight spanning !!!", e);
			}
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
				if ((stickerList == null) || !chatthread.isKeyboardOpen())
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
				stickerRecommendView.setVisibility(View.VISIBLE);

				Logger.d(TAG, "fetch new list starting ..");
				Pair<Boolean, List<Sticker>> result = StickerSearchUtils.shouldShowStickerFtue(stickerList);
				Logger.d(TAG, "fetch new list completed ..");

				if (!result.first) // no available stickers present show ftue
				{
					FragmentManager fm = activity.getSupportFragmentManager();
					fragmentFtue = fm.findFragmentByTag(HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_FTUE_TAG);
					if (fragmentFtue == null)
					{
						fragmentFtue = StickerRecommendationFtueFragment.newInstance(StickerTagWatcher.this, (ArrayList<Sticker>) stickerList);
						fm.beginTransaction().add(R.id.sticker_recommendation_parent, fragmentFtue, HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_FTUE_TAG)
								.commitAllowingStateLoss();
					}
					hideFragment(fragment);
					showFragment(fragmentFtue);
					((StickerRecommendationFtueFragment) fragmentFtue).setAndNotify(word, phrase, result.second);
				}
				else
				// show only available stickers
				{
					hideFragment(fragmentFtue);
					showFragment(fragment);

					((StickerRecommendationFragment) fragment).setAndNotify(word, phrase, result.second);
					showFtueAnimation();
				}
			}
		});
	}

	public void showFtueAnimation()
	{
		if (count > 0)
		{
			stickerRecommendView.removeCallbacks(scrollRunnable);
			stickerRecommendView.postDelayed(scrollRunnable, WAIT_TIME_IN_FTUE_SCROLL);
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
	public void stickerSelected(String word, String phrase, Sticker sticker, int selectedIndex, int size, String source, boolean dismissAndClear)
	{
		Logger.v(TAG, "stickerSelected(" + word + ", " + phrase + ", " + sticker + ", " + selectedIndex + ")");

		if (stickerPickerListener == null)
		{
			throw new IllegalStateException("sticker picker is null but sticker is selected.");
		}

		StickerManager.getInstance().addRecentStickerToPallete(sticker);
		stickerPickerListener.stickerSelected(sticker, source);

		if (dismissAndClear)
		{
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
			/*
			 * if sticker is selected from pop-up, then select all the text
			 */
			else
			{
				selectSearchText();
			}
		}
		// send analytics
		StickerManager.getInstance().sendRecommendationSelectionAnalytics(source, sticker.getStickerId(), sticker.getCategoryId(), (selectedIndex + 1), size,
				StickerSearchManager.getInstance().getNumStickersVisibleAtOneTime(), word, phrase);
	}

	@Override
	public void onCloseClicked(String word, String phrase, boolean ftue)
	{
		dismissStickerSearchPopup();

		// send analytics
		if (ftue && fragmentFtue != null)
		{
			StickerRecommendationFtueFragment stickerRecommendationFtueFragment = (StickerRecommendationFtueFragment) fragmentFtue;
			StickerManager.getInstance().sendRecommendationRejectionAnalyticsFtue(stickerRecommendationFtueFragment.isFtueScreen1Visible(), StickerManager.REJECT_FROM_CROSS, word,
					phrase);
		}
		else
		{
			StickerManager.getInstance().sendRecommendationRejectionAnalytics(StickerSearchManager.getInstance().getFirstContinuousMatchFound(), StickerManager.REJECT_FROM_CROSS,
					word, phrase);
		}
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

	private void hideFragment(Fragment fragment)
	{
		if ((activity != null) && (fragment != null))
		{
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			fragmentManager.beginTransaction().hide(fragment).commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
		fragment = null;
	}

	private void showFragment(Fragment fragment)
	{
		if ((activity != null) && (fragment != null))
		{
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			fragmentManager.beginTransaction().show(fragment).commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
		fragment = null;
	}

	public void releaseResources()
	{
		StickerSearchHostManager.getInstance().clearTransientResources();
		StickerSearchManager.getInstance().removeStickerSearchListener(this);
		stickerRecommendView = null;
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
		if (isStickerRecommnedPoupShowing())
		{
			if (fragmentFtue != null)
			{
				StickerRecommendationFtueFragment stickerRecommendationFtueFragment = (StickerRecommendationFtueFragment) fragmentFtue;
				StickerManager.getInstance().sendRecommendationRejectionAnalyticsFtue(stickerRecommendationFtueFragment.isFtueScreen1Visible(), StickerManager.REJECT_FROM_IGNORE,
						stickerRecommendationFtueFragment.getTappedWord(), stickerRecommendationFtueFragment.getTaggedPhrase());
			}
			else if (fragment != null)
			{
				StickerRecommendationFragment stickerRecommendationFragment = (StickerRecommendationFragment) fragment;
				StickerManager.getInstance().sendRecommendationRejectionAnalytics(StickerSearchManager.getInstance().getFirstContinuousMatchFound(),
						StickerManager.REJECT_FROM_IGNORE, stickerRecommendationFragment.getTappedWord(), stickerRecommendationFragment.getTaggedPhrase());
			}
		}
	}
}