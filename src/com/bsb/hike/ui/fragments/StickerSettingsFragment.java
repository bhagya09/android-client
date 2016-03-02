package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragScrollProfile;
import com.bsb.hike.DragSortListView.DragSortListView.DropListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerSettingsAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerSettingsTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerSettingsFragment extends Fragment implements Listener, DragScrollProfile, OnItemClickListener
{
	private String[] pubSubListeners = {HikePubSub.STICKER_PACK_DELETED, HikePubSub.ALL_PACKS_UPDATED};

	private List<StickerCategory> stickerCategories = new ArrayList<StickerCategory>();
	
	private Set<StickerCategory> updateStickerSet = new HashSet<StickerCategory>();  //Stores the categories which have update available and are visible

	private StickerSettingsAdapter mAdapter;
	
	private DragSortListView mDslv;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;
	
	private HikeSharedPreferenceUtil prefs;
	
	private View footerView;
	
	private boolean isUpdateAllTapped = false;

	private StickerSettingsTask stickerSettingsTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticker_settings, null);
		stickerSettingsTask = (StickerSettingsTask) getArguments().getSerializable(StickerConstants.STICKER_SETTINGS_TASK_ARG);
		return parent;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		prefs = HikeSharedPreferenceUtil.getInstance();
		initAdapterAndList();
		showTipIfRequired();
		checkAndInflateUpdateView();
		
		registerListener();
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void checkAndInflateUpdateView()
	{
		if(shouldAddUpdateView())
		{
			final View parent = getView();
			final View updateAll = parent.findViewById(R.id.update_all_ll);
			final View confirmAll = parent.findViewById(R.id.confirmation_ll);

			Animation alphaIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(800);
			updateAll.setAnimation(alphaIn);
			updateAll.setVisibility(View.VISIBLE);
			alphaIn.start();
			
			updateAll.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					isUpdateAllTapped = true;
					if(shouldAddUpdateView())
					{
						updateAll.setVisibility(View.INVISIBLE);
						confirmAll.setVisibility(View.VISIBLE);
						setUpdateDetails(parent, confirmAll);
					}
					else
					{
						Toast.makeText(getActivity(), R.string.update_all_fail_string, Toast.LENGTH_SHORT).show();
					}
				
					mDslv.removeFooterView(footerView);
				}
			});
		}
		
		else
		{
			mDslv.removeFooterView(footerView);
		}
	}

	private void setUpdateDetails(View parent, final View confirmView)
	{
		TextView categoryCost = (TextView) parent.findViewById(R.id.sticker_cost);
		TextView totalPacks = (TextView) parent.findViewById(R.id.total_packs);
		TextView totalStickers = (TextView) parent.findViewById(R.id.pack_details);
		TextView cancelBtn = (TextView) parent.findViewById(R.id.cancel_btn);
		TextView confirmBtn = (TextView) parent.findViewById(R.id.confirm_btn);
		totalPacks.setText(updateStickerSet.size() == 1 ? getString(R.string.singular_packs, updateStickerSet.size()) : getString(R.string.n_packs, updateStickerSet.size()));
		categoryCost.setText(R.string.sticker_pack_free);
		
		displayTotalStickersCount(totalStickers);
		
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isUpdateAllTapped = false;
				confirmView.setVisibility(View.GONE);

				try {
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.UPDATE_ALL_CANCEL_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				} catch (JSONException e) {
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		});
		
		confirmBtn.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				isUpdateAllTapped = false;
				for(StickerCategory category : updateStickerSet)
				{
					StickerManager.getInstance().initialiseDownloadStickerPackTask(category, DownloadSource.SETTINGS, getActivity());
				}
				
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.UPDATE_ALL_CONFIRM_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				mAdapter.notifyDataSetChanged();
				confirmView.setVisibility(View.GONE);
			}
		});
	}

	private void displayTotalStickersCount(TextView totalStickers)
	{
		int totalCount = 0;
		int totalSize = 0;
		for(StickerCategory category : updateStickerSet)
		{
			if(category.getMoreStickerCount() > 0)
			{
				totalCount += category.getMoreStickerCount(); 
			}
			
			if(category.getCategorySize() > 0)
			{
				totalSize += category.getCategorySize();
			}
		}
		if(totalCount > 0)
		{
			String text = totalCount == 1 ? getActivity().getResources().getString(R.string.singular_stickers, totalCount) : getActivity().getResources().getString(R.string.n_stickers, totalCount);
			if(totalSize > 0)
			{
				text += ", " + Utils.getSizeForDisplay(totalSize);
			}
			
			totalStickers.setText(text);
		}
	}

	private boolean shouldAddUpdateView()
	{
		if (stickerSettingsTask != StickerSettingsTask.STICKER_UPDATE_TASK)
		{
			return false;
		}

		if (stickerCategories.size() == 0)
		{
			//Displaying "All Updated" message along with sticker when update list is empty
			View parent = getView();
			parent.findViewById(R.id.all_updated_message).setVisibility(View.VISIBLE);
			View redirectToShopBtn = parent.findViewById(R.id.redirect_to_shop_btn);
			redirectToShopBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(getActivity(), StickerShopActivity.class);
					startActivity(i);
					getActivity().finish();
				}
			});
			parent.findViewById(R.id.sticker_settings).setVisibility(View.GONE);

			return false;
		}
		else
		{
			initUpdateStickerSet();
			if (updateStickerSet.size() > 0) {
				return true;
			} else {
				return false;
			}
		}
	}

	private void initUpdateStickerSet()
	{
		updateStickerSet.clear();
		for (StickerCategory category : stickerCategories) {
			if (category.shouldAddToUpdateAll())                //the update option will have only packs with update available; so checking for only done and downloading state
			{
				updateStickerSet.add(category);
			}
		}
	}

	/**
	 * Utility method to show category reordering tip
	 * @param parent
	 */
	private void showTipIfRequired()
	{
		if(stickerSettingsTask == StickerSettingsTask.STICKER_REORDER_TASK)
		{
			showDragTip();
		}
	}

	private void showDragTip()
	{
		//Showing drag tip every time reorder packs is selected
		final View parent = getView().findViewById(R.id.list_ll);
		final View v =(View) parent.findViewById(R.id.reorder_tip);
		v.setVisibility(View.VISIBLE);
		prefs.saveData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false); // resetting the tip flag

		mDslv.addDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				if (!prefs.getData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, false)) {
					StickerCategory category = mAdapter.getDraggedCategory();

					if ((from == to) || (category == null) || (!category.isVisible())) // Dropping at the same position. No need to perform Drop.
					{
						return;
					}

					if (from > mAdapter.getLastVisibleIndex() && to > mAdapter.getLastVisibleIndex() + 1) {
						return;
					}

					// Setting the tip flag so that drag tip disappears after first reorder is done
					prefs.saveData(HikeMessengerApp.IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN, true);

					try {
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SEEN_REORDERING_TIP);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					} catch (JSONException e) {
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					ImageView tickImage = (ImageView) parent.findViewById(R.id.reorder_indicator);
					tickImage.setImageResource(R.drawable.art_tick);
					View tapAndDragTip = parent.findViewById(R.id.tap_and_drag_tip);
					tapAndDragTip.setVisibility(View.GONE);
					parent.findViewById(R.id.great_job).setVisibility(View.VISIBLE);

					TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0,
							Animation.ABSOLUTE, -v.getHeight());
					animation.setDuration(400);
					animation.setStartOffset(800);
					parent.setAnimation(animation);

					animation.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							v.setVisibility(View.GONE);
							TranslateAnimation temp = new TranslateAnimation(0, 0, 0, 0);
							temp.setDuration(1l);
							parent.startAnimation(temp);
						}
					});
				}
			}
		});
	}

	private void initStickerCategoriesList() {
		stickerCategories = StickerManager.getInstance().getMyStickerCategoryList();

		if (stickerSettingsTask == StickerSettingsTask.STICKER_UPDATE_TASK)
		{
			Iterator it = stickerCategories.iterator();
			StickerCategory category;

			while (it.hasNext()) {
				category = (StickerCategory) it.next();
				if (!category.shouldShowUpdateAvailable())
				{
					it.remove();
				}

			}
		}
	}

	private void initAdapterAndList()
	{
		View parent = getView();
		initStickerCategoriesList();
		mAdapter = new StickerSettingsAdapter(getActivity(), stickerCategories, stickerSettingsTask);
		mDslv = (DragSortListView) parent.findViewById(R.id.item_list);
		//mDslv.setOnScrollListener(this);
		footerView = getActivity().getLayoutInflater().inflate(R.layout.sticker_settings_footer, null);
		mDslv.addFooterView(footerView);
		mDslv.setAdapter(mAdapter);
		mDslv.setClickable(true);
		mDslv.setOnItemClickListener(this);
		if (stickerSettingsTask == StickerSettingsTask.STICKER_REORDER_TASK)
		{
			mDslv.setDragEnabled(true);
			mDslv.setDragScrollProfile(this);
		}
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		unregisterListeners();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	public void onStop()
	{
		mAdapter.persistChanges();
		super.onStop();
	}

	@Override
	public void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if (mAdapter != null)
		{
			mAdapter.getStickerPreviewLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch(type) {
			case HikePubSub.STICKER_PACK_DELETED:
				final StickerCategory category = (StickerCategory) object;

				if (!isAdded()) {
					return;
				}
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mAdapter == null) {
							return;
						}
						Toast.makeText(getActivity(), getString(R.string.pack_deleted) + " " + category.getCategoryName(), Toast.LENGTH_SHORT).show();
						mAdapter.onStickerPackDelete(category);
					}
				});

				break;

			case HikePubSub.ALL_PACKS_UPDATED:
				if (!isAdded()) {
					return;
				}
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						//Displaying All Done after last pack in update list gets updated
						View parent = getView();
						View updateAll = parent.findViewById(R.id.update_all_ll);
						updateAll.setClickable(false);
						TextView updateText = (TextView) parent.findViewById(R.id.update_text);
						updateText.setText(R.string.all_done);
						updateAll.setVisibility(View.VISIBLE);
					}
				});

				break;
		}
	}

	public static StickerSettingsFragment newInstance()
	{
		StickerSettingsFragment stickerSettingsFragment = new StickerSettingsFragment();
		return stickerSettingsFragment;
	}

	@Override
	public float getSpeed(float w, long t)
	{
		// TODO Fine tune these parameters further
		if (w > 0.8f)
		{
			return ((float) mAdapter.getCount()) / 1.0f;
		}
		else
		{
			return 1.0f * w;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(position >= mAdapter.getCount())
		{
			return;
		}
		StickerCategory category = mAdapter.getItem(position);
		if(category.getState() == StickerCategory.RETRY && category.isVisible())
		{
			category.setState(StickerCategory.DOWNLOADING);
			StickerManager.getInstance().initialiseDownloadStickerPackTask(category, DownloadSource.SETTINGS, getActivity());
			mAdapter.notifyDataSetChanged();
		}
		
		else
		{
			return;
		}
	}
	
	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKERS_UPDATED);
		filter.addAction(StickerManager.STICKERS_FAILED);
		filter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		filter.addAction(StickerManager.STICKER_PREVIEW_DOWNLOADED);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(!isAdded())
			{
				return;
			}
			if (intent.getAction().equals(StickerManager.STICKERS_UPDATED) || intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED))
			{
				if(getActivity() == null)
				{
					return;
				}
				getActivity().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						mAdapter.notifyDataSetChanged();
					}
				});
			}
			else if(intent.getAction().equals(StickerManager.STICKER_PREVIEW_DOWNLOADED))
			{
				if(mAdapter == null)
				{
					return ;
				}
				mAdapter.notifyDataSetChanged();
			}
			else if(intent.getAction().equals(StickerManager.STICKERS_FAILED))
			{
				Bundle b = intent.getBundleExtra(StickerManager.STICKER_DATA_BUNDLE);
				String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
				final StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
				if(category == null)
				{
					return;
				}
				final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{

						@Override
						public void run()
						{
							if(failedDueToLargeFile)
							{
								Toast.makeText(getActivity(), R.string.out_of_space, Toast.LENGTH_SHORT).show();
							}
							category.setState(StickerCategory.RETRY);
							mAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};
	
	public void unregisterListeners()
	{
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	public boolean getIsUpdateAllTapped()
	{
		return isUpdateAllTapped;
	}

	public void hideConfirmAllView()
	{
		isUpdateAllTapped = false;
		View confirmAll = getView().findViewById(R.id.confirmation_ll);
		confirmAll.setVisibility(View.GONE);
	}
}
