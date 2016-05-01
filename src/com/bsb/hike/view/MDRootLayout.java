package com.bsb.hike.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ScrollView;

import com.bsb.hike.R;

/**
 * @author Piyush
 *         <p/>
 *         This is the top level view for all MaterialDialogs It handles the layout of: titleFrame, content (text, custom view, listview, etc) buttonDefault...
 */
public class MDRootLayout extends ViewGroup
{

	private View mTitleBar;

	private View mContent;

	private View mButtonContainer;

	private View mCheckboxContainer;

	private boolean mDrawTopDivider = false;

	private boolean mDrawBottomDivider = false;

	private boolean mUseFullPadding = true;

	private boolean mReducePaddingNoTitleNoButtons;

	private boolean mNoTitleNoPadding;

	private int mNoTitlePaddingFull;

	private int mButtonPaddingFull;

	private int mButtonBarHeight;

	private int mCheckboxPaddingFull;

	private int mCheckboxBarHeight;

	private Paint mDividerPaint;

	private ViewTreeObserver.OnScrollChangedListener mTopOnScrollChangedListener;

	private ViewTreeObserver.OnScrollChangedListener mBottomOnScrollChangedListener;

	private int mDividerWidth;

	public MDRootLayout(Context context)
	{
		super(context);
		init(context, null, 0);
	}

	public MDRootLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, attrs, 0);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public MDRootLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public MDRootLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs, defStyleAttr);
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr)
	{
		Resources r = context.getResources();

		mNoTitlePaddingFull = r.getDimensionPixelSize(R.dimen.md_notitle_vertical_padding);
		mButtonPaddingFull = r.getDimensionPixelSize(R.dimen.md_button_frame_vertical_padding);

		mButtonBarHeight = r.getDimensionPixelSize(R.dimen.md_button_height);

		mCheckboxPaddingFull = r.getDimensionPixelSize(R.dimen.md_checkbox_frame_vertical_padding);
		mCheckboxBarHeight = r.getDimensionPixelSize(R.dimen.md_checkbox_height);

		mDividerPaint = new Paint();
		mDividerWidth = r.getDimensionPixelSize(R.dimen.md_divider_height);
		mDividerPaint.setColor(r.getColor(R.color.home_screen_list_divider));
		setWillNotDraw(false);
	}

	public void noTitleNoPadding()
	{
		mNoTitleNoPadding = true;
	}

	@Override
	public void onFinishInflate()
	{
		super.onFinishInflate();
		for (int i = 0; i < getChildCount(); i++)
		{
			View v = getChildAt(i);
			if (v.getId() == R.id.title_template)
			{
				mTitleBar = v;
			}
			else if (v.getId() == R.id.button_panel)
			{
				mButtonContainer = v;
			}
			else if (v.getId() == R.id.checkbox_container)
			{
				mCheckboxContainer = v;
			}
			else
			{
				mContent = v;
			}
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		mUseFullPadding = true;
		boolean hasButtons = false;

		if (mButtonContainer != null && isVisible(mButtonContainer))
		{
			measureChild(mButtonContainer, widthMeasureSpec, heightMeasureSpec);
			hasButtons = true;
		}

		int availableHeight = height;
		int fullPadding = 0;
		int minPadding = 0;
		if (hasButtons)
		{
			availableHeight -= mButtonBarHeight;
			fullPadding += 2 * mButtonPaddingFull;
			/* No minPadding */
		}
		else
		{
			/* Content has 8dp, we add 16dp and get 24dp, the frame margin */
			fullPadding += 2 * mButtonPaddingFull;
		}

		boolean hasCheckbox = false;

		if (mCheckboxContainer != null && isVisible(mCheckboxContainer))
		{
			measureChild(mCheckboxContainer, widthMeasureSpec, heightMeasureSpec);
			hasCheckbox = true;
		}

		if (hasCheckbox)
		{
			availableHeight -= mCheckboxBarHeight;
			fullPadding += 2 * mCheckboxPaddingFull;
		}
		else
		{
			fullPadding += 2 * mCheckboxPaddingFull;
		}

		if (isVisible(mTitleBar))
		{
			mTitleBar.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED);
			availableHeight -= mTitleBar.getMeasuredHeight();
		}
		else if (!mNoTitleNoPadding)
		{
			fullPadding += mNoTitlePaddingFull;
		}

		if (isVisible(mContent))
		{
			mContent.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(availableHeight - minPadding, MeasureSpec.AT_MOST));

			if (mContent.getMeasuredHeight() <= availableHeight - fullPadding)
			{
				if (!mReducePaddingNoTitleNoButtons || isVisible(mTitleBar) || hasButtons || hasCheckbox)
				{
					mUseFullPadding = true;
					availableHeight -= mContent.getMeasuredHeight() + fullPadding;
				}
				else
				{
					mUseFullPadding = false;
					availableHeight -= mContent.getMeasuredHeight() + minPadding;
				}
			}
			else
			{
				mUseFullPadding = false;
				availableHeight = 0;
			}

		}

		setMeasuredDimension(width, height - availableHeight);
	}

	private static boolean isVisible(View v)
	{
		boolean visible = v != null && v.getVisibility() != View.GONE;
		return visible;
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mContent != null)
		{
			if (mDrawTopDivider)
			{
				int y = mContent.getTop();
				canvas.drawRect(0, y - mDividerWidth, getMeasuredWidth(), y, mDividerPaint);
			}

			if (mDrawBottomDivider)
			{
				int y = mContent.getBottom();
				canvas.drawRect(0, y, getMeasuredWidth(), y + mDividerWidth, mDividerPaint);
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, final int l, int t, final int r, int b)
	{
		if (isVisible(mTitleBar))
		{
			int height = mTitleBar.getMeasuredHeight();
			mTitleBar.layout(l, t, r, t + height);
			t += height;
		}
		else if (!mNoTitleNoPadding && mUseFullPadding)
		{
			t += mNoTitlePaddingFull;
		}

		if (isVisible(mContent))
			mContent.layout(l, t, r, t + mContent.getMeasuredHeight());

		if (isVisible(mCheckboxContainer))
		{
			mCheckboxContainer.layout(l, b - mCheckboxBarHeight - mButtonBarHeight, r, b - mButtonBarHeight);
		}

		if (isVisible(mButtonContainer))
		{
			mButtonContainer.layout(l, b - mButtonBarHeight, r, b);
		}

		setUpDividersVisibility(mContent, true, true);
	}

	public void setDividerColor(int color)
	{
		mDividerPaint.setColor(color);
		invalidate();
	}

	private void setUpDividersVisibility(final View view, final boolean setForTop, final boolean setForBottom)
	{
		if (view == null)
			return;
		if (view instanceof ScrollView)
		{
			final ScrollView sv = (ScrollView) view;
			if (canScrollViewScroll(sv))
			{
				addScrollListener(sv, setForTop, setForBottom);
			}
			else
			{
				if (setForTop)
					mDrawTopDivider = false;
				if (setForBottom)
					mDrawBottomDivider = false;
			}
		}
		else if (view instanceof AdapterView)
		{
			final AdapterView sv = (AdapterView) view;
			if (canAdapterViewScroll(sv))
			{
				addScrollListener(sv, setForTop, setForBottom);
			}
			else
			{
				if (setForTop)
					mDrawTopDivider = false;
				if (setForBottom)
					mDrawBottomDivider = false;
			}
		}
		else if (view instanceof WebView)
		{
			view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
			{
				@Override
				public boolean onPreDraw()
				{
					if (view.getMeasuredHeight() != 0)
					{
						if (!canWebViewScroll((WebView) view))
						{
							if (setForTop)
								mDrawTopDivider = false;
							if (setForBottom)
								mDrawBottomDivider = false;
						}
						else
						{
							addScrollListener((ViewGroup) view, setForTop, setForBottom);
						}
						view.getViewTreeObserver().removeOnPreDrawListener(this);
					}
					return true;
				}
			});
		}
		else if (view instanceof RecyclerView)
		{
			/*
			 * Scroll offset detection for RecyclerView isn't reliable b/c the OnScrollChangedListener isn't always called on scroll. We can't set a OnScrollListener either because
			 * that will override the user's OnScrollListener if they set one.
			 */
			boolean canScroll = canRecyclerViewScroll((RecyclerView) view);
			if (setForTop)
				mDrawTopDivider = canScroll;
			if (setForBottom)
				mDrawBottomDivider = canScroll;
		}
		else if (view instanceof ViewGroup)
		{
			View topView = getTopView((ViewGroup) view);
			setUpDividersVisibility(topView, setForTop, setForBottom);
			View bottomView = getBottomView((ViewGroup) view);
			if (bottomView != topView)
			{
				setUpDividersVisibility(bottomView, false, true);
			}
		}
	}

	private void addScrollListener(final ViewGroup vg, final boolean setForTop, final boolean setForBottom)
	{
		if ((!setForBottom && mTopOnScrollChangedListener == null || (setForBottom && mBottomOnScrollChangedListener == null)))
		{
			ViewTreeObserver.OnScrollChangedListener onScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener()
			{
				@Override
				public void onScrollChanged()
				{
					boolean hasButtons = false;
					if (isVisible(mButtonContainer))
					{
						hasButtons = true;
					}

					if (vg instanceof WebView)
					{
						invalidateDividersForWebView((WebView) vg, setForTop, setForBottom, hasButtons);
					}
					else
					{
						invalidateDividersForScrollingView(vg, setForTop, setForBottom, hasButtons);
					}
					invalidate();
				}
			};
			if (!setForBottom)
			{
				mTopOnScrollChangedListener = onScrollChangedListener;
				vg.getViewTreeObserver().addOnScrollChangedListener(mTopOnScrollChangedListener);
			}
			else
			{
				mBottomOnScrollChangedListener = onScrollChangedListener;
				vg.getViewTreeObserver().addOnScrollChangedListener(mBottomOnScrollChangedListener);
			}
			onScrollChangedListener.onScrollChanged();
		}
	}

	private void invalidateDividersForScrollingView(ViewGroup view, final boolean setForTop, boolean setForBottom, boolean hasButtons)
	{
		if (setForTop && view.getChildCount() > 0)
		{
			mDrawTopDivider = mTitleBar != null && mTitleBar.getVisibility() != View.GONE &&
			// Not scrolled to the top.
					view.getScrollY() + view.getPaddingTop() > view.getChildAt(0).getTop();

		}
		if (setForBottom && view.getChildCount() > 0)
		{
			mDrawBottomDivider = hasButtons && view.getScrollY() + view.getHeight() - view.getPaddingBottom() < view.getChildAt(view.getChildCount() - 1).getBottom();
		}
	}

	private void invalidateDividersForWebView(WebView view, final boolean setForTop, boolean setForBottom, boolean hasButtons)
	{
		if (setForTop)
		{
			mDrawTopDivider = mTitleBar != null && mTitleBar.getVisibility() != View.GONE &&
			// Not scrolled to the top.
					view.getScrollY() + view.getPaddingTop() > 0;
		}
		if (setForBottom)
		{
			// noinspection deprecation
			mDrawBottomDivider = hasButtons && view.getScrollY() + view.getMeasuredHeight() - view.getPaddingBottom() < view.getContentHeight() * view.getScale();
		}
	}

	public static boolean canRecyclerViewScroll(RecyclerView view)
	{
		if (view == null || view.getAdapter() == null || view.getLayoutManager() == null)
			return false;
		final RecyclerView.LayoutManager lm = view.getLayoutManager();
		final int count = view.getAdapter().getItemCount();
		int lastVisible = -1;

		if (lm instanceof LinearLayoutManager)
		{
			LinearLayoutManager llm = (LinearLayoutManager) lm;
			lastVisible = llm.findLastVisibleItemPosition();
		}

		if (lastVisible == -1)
			return false;
		/* We scroll if the last item is not visible */
		final boolean lastItemVisible = lastVisible == count - 1;
		return !lastItemVisible || (view.getChildCount() > 0 && view.getChildAt(view.getChildCount() - 1).getBottom() > view.getHeight() - view.getPaddingBottom());
	}

	private static boolean canScrollViewScroll(ScrollView sv)
	{
		if (sv.getChildCount() == 0)
			return false;
		final int childHeight = sv.getChildAt(0).getMeasuredHeight();
		return sv.getMeasuredHeight() - sv.getPaddingTop() - sv.getPaddingBottom() < childHeight;
	}

	private static boolean canWebViewScroll(WebView view)
	{
		// noinspection deprecation
		return view.getMeasuredHeight() < view.getContentHeight() * view.getScale();
	}

	private static boolean canAdapterViewScroll(AdapterView lv)
	{
		/* Force it to layout it's children */
		if (lv.getLastVisiblePosition() == -1)
			return false;

		/* We can scroll if the first or last item is not visible */
		boolean firstItemVisible = lv.getFirstVisiblePosition() == 0;
		boolean lastItemVisible = lv.getLastVisiblePosition() == lv.getCount() - 1;

		if (firstItemVisible && lastItemVisible && lv.getChildCount() > 0)
		{
			/* Or the first item's top is above or own top */
			if (lv.getChildAt(0).getTop() < lv.getPaddingTop())
				return true;
			/* or the last item's bottom is beyond our own bottom */
			return lv.getChildAt(lv.getChildCount() - 1).getBottom() > lv.getHeight() - lv.getPaddingBottom();
		}

		return true;
	}

	/**
	 * Find the view touching the bottom of this ViewGroup. Non visible children are ignored, however getChildDrawingOrder is not taking into account for simplicity and because it
	 * behaves inconsistently across platform versions.
	 * 
	 * @return View touching the bottom of this ViewGroup or null
	 */
	@Nullable
	private static View getBottomView(ViewGroup viewGroup)
	{
		if (viewGroup == null || viewGroup.getChildCount() == 0)
			return null;
		View bottomView = null;
		for (int i = viewGroup.getChildCount() - 1; i >= 0; i--)
		{
			View child = viewGroup.getChildAt(i);
			if (child.getVisibility() == View.VISIBLE && child.getBottom() == viewGroup.getMeasuredHeight())
			{
				bottomView = child;
				break;
			}
		}
		return bottomView;
	}

	@Nullable
	private static View getTopView(ViewGroup viewGroup)
	{
		if (viewGroup == null || viewGroup.getChildCount() == 0)
			return null;
		View topView = null;
		for (int i = viewGroup.getChildCount() - 1; i >= 0; i--)
		{
			View child = viewGroup.getChildAt(i);
			if (child.getVisibility() == View.VISIBLE && child.getTop() == 0)
			{
				topView = child;
				break;
			}
		}
		return topView;
	}
}