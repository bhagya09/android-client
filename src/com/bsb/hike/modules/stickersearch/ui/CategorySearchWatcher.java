package com.bsb.hike.modules.stickersearch.ui;

import static com.bsb.hike.modules.stickersearch.StickerSearchConstants.SHOW_SCROLL_FTUE_COUNT;
import static com.bsb.hike.modules.stickersearch.StickerSearchConstants.WAIT_TIME_IN_FTUE_SCROLL;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;
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
import com.bsb.hike.chatthread.ChatThreadTips;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.listeners.CategorySearchListener;
import com.bsb.hike.modules.stickersearch.listeners.IStickerPickerRecommendationListener;
import com.bsb.hike.modules.stickersearch.listeners.IStickerRecommendFragmentListener;
import com.bsb.hike.modules.stickersearch.listeners.IStickerSearchListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.modules.stickersearch.tasks.CategorySearchTask;
import com.bsb.hike.modules.stickersearch.ui.colorspan.ColorSpanPool;
import com.bsb.hike.ui.fragments.StickerShopSearchFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class CategorySearchWatcher implements CategorySearchListener,SearchView.OnQueryTextListener
{
	public static final String TAG = CategorySearchWatcher.class.getSimpleName();

	private StickerShopSearchFragment fragment;

	public CategorySearchWatcher(StickerShopSearchFragment fragment)
	{
		Logger.i(TAG, "Initialising Category tag watcher...");
        this.fragment = fragment;
	}

	public void releaseResources()
	{
		CategorySearchManager.getInstance().clearTransientResources();
		fragment = null;
	}

	@Override
	public void onSearchCompleted(final List<StickerCategory> categories)
	{
        CategorySearchManager.getInstance().getSearchEngine().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.onSearchCompleted(categories);
            }
        },0);
	}

	@Override
	public void onNoCategoriesFound(final String query)
	{
        CategorySearchManager.getInstance().getSearchEngine().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.onNoCategoriesFound(query);
            }
        },0);
	}

	@Override
	public boolean onQueryTextSubmit(String query)
	{
         return CategorySearchManager.getInstance().onQueryTextSubmit(query,this);
	}

	@Override
	public boolean onQueryTextChange(String query)
	{
        return CategorySearchManager.getInstance().onQueryTextChange(query, this);
	}
}
