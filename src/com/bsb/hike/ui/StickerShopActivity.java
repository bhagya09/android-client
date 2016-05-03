package com.bsb.hike.ui;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.StickerShopFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

public class StickerShopActivity extends HikeAppStateBaseFragmentActivity
{
	private StickerShopFragment stickerShopFragment;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_shop_parent);
		setupShopFragment(savedInstanceState);
		setupActionBar();
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STICKER_SHOP.ordinal());

	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	private void setupShopFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			return;
		}
		stickerShopFragment = StickerShopFragment.newInstance();

		getSupportFragmentManager().beginTransaction().add(R.id.sticker_shop_parent, stickerShopFragment).commit();

	}

	public void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);

		View stickerSettingsBtn = actionBarView.findViewById(R.id.sticker_settings_btn);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.sticker_shop);

		stickerSettingsBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_SETTING_BTN_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				
				IntentFactory.openStickerSettingsActivity(StickerShopActivity.this);

			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	@Override
	public void onBackPressed()
	{
		if (getCallingActivity() != null)
		{
			setResult(ChatThread.RESULT_CODE_STICKER_SHOP_ACTIVITY);
		}
		// TODO Auto-generated method stub
		finish();
	}

}
