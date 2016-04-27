package com.bsb.hike.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerSettingsTask;
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
	private StickerSettingsTask stickerSettingsTask;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_settings_page);
		Intent intent = getIntent();
		stickerSettingsTask = (StickerSettingsTask) intent.getSerializableExtra(HikeConstants.Extras.STICKER_SETTINGS_TASK);
		setupSettingsFragment(savedInstanceState);
		setupActionBar();
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STICKER_SHOP_SETTINGS.ordinal());
		
	}

	private void setTitle(TextView title)
	{
		switch(stickerSettingsTask) {
			case STICKER_REORDER_TASK:
				title.setText(R.string.sticker_reorder_setting_header);
				break;

			case STICKER_DELETE_TASK:
				title.setText(R.string.sticker_delete_setting_header);
				break;

			case STICKER_HIDE_TASK:
				title.setText(R.string.sticker_hide_setting_header);
				break;

			case STICKER_UPDATE_TASK:
				title.setText(R.string.sticker_update_setting_header);
				break;

			default:
				title.setText(R.string.my_stickers);
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		setTitle(title);
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	private void setupSettingsFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			return;
		else
		{
			Bundle stickerSettingsTaskArg = new Bundle();
			stickerSettingsTaskArg.putSerializable(StickerConstants.STICKER_SETTINGS_TASK_ARG, stickerSettingsTask);
			stickerSettingsFragment = StickerSettingsFragment.newInstance();
			stickerSettingsFragment.setArguments(stickerSettingsTaskArg);
		}

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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if ((stickerSettingsFragment != null) && (stickerSettingsFragment.getStickerCategoriesList().size() == 0))
		{
			//Handling orientation change in case of all packs updated/deleted view
			Bundle stickerSettingsTaskArg = new Bundle();
			stickerSettingsTaskArg.putSerializable(StickerConstants.STICKER_SETTINGS_TASK_ARG, stickerSettingsTask);
			stickerSettingsFragment = StickerSettingsFragment.newInstance();
			stickerSettingsFragment.setArguments(stickerSettingsTaskArg);
			getSupportFragmentManager().beginTransaction().replace(R.id.sticker_settings_parent, stickerSettingsFragment).commit();
		}
	}


}
