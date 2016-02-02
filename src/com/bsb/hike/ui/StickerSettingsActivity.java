package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.StickerSettingsFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class StickerSettingsActivity extends HikeAppStateBaseFragmentActivity
{
	private StickerSettingsFragment stickerSettingsFragment;
	private int stickerSettingsTask;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_settings_page);
		Intent intent = getIntent();
		stickerSettingsTask = intent.getIntExtra(HikeConstants.Extras.STICKER_SETTINGS_TASK, HikeConstants.StickerSettingsTask.STICKER_REORDER_TASK);
		setupSettingsFragment(savedInstanceState);
		setupActionBar();
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STICKER_SHOP_SETTINGS.ordinal());
		
	}

	public int getStickerSettingsTask() { return stickerSettingsTask; }

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);
		View stickerSettingsBtn = actionBarView.findViewById(R.id.sticker_settings_btn);
		stickerSettingsBtn.setVisibility(View.GONE);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.my_stickers);
		TextView hideOption = (TextView) actionBarView.findViewById(R.id.hide_pack);
		if (stickerSettingsTask == HikeConstants.StickerSettingsTask.STICKER_HIDE_TASK)
		{
			hideOption.setVisibility(View.VISIBLE);
		}
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	private void setupSettingsFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			return;
		else
			stickerSettingsFragment = StickerSettingsFragment.newInstance();
		getSupportFragmentManager().beginTransaction().add(R.id.sticker_settings_parent, stickerSettingsFragment).commit();

	}
	
	@Override
	public void onBackPressed()
	{
		if(stickerSettingsFragment != null)
		{
			if(stickerSettingsFragment.getIsUpdateAllTapped())
			{
				stickerSettingsFragment.hideConfirmAllView();
				return;
			}
		}
		
		super.onBackPressed();
		
	}

}
