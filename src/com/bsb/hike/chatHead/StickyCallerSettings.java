package com.bsb.hike.chatHead;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.hike.transporter.utils.Logger;

public class StickyCallerSettings extends HikeAppStateBaseFragmentActivity implements OnCheckedChangeListener
{

	private SwitchCompat stickyCallerCheckbox, enableSavedContactViewCheckbox;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticky_caller_settings);
		setupActionBar();
		stickyCallerCheckbox = (SwitchCompat) findViewById(R.id.sticky_caller_checkbox);
		stickyCallerCheckbox.setOnCheckedChangeListener(this);
		stickyCallerCheckbox.setChecked(HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.ACTIVATE_STICKY_CALLER, false));
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_KNOWN_NUMBER_CARD, false))
		{
			enableSavedContactViewCheckbox = (SwitchCompat) findViewById(R.id.saved_contact_card_checkbox);
			enableSavedContactViewCheckbox.setOnCheckedChangeListener(this);
			enableSavedContactViewCheckbox.setChecked(HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.ENABLE_KNOWN_NUMBER_CARD, true));
		}
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.ACTIVATE_STICKY_CALLER, false))
		{
			ChatHeadUtils.registerCallReceiver();
		}
		else
		{
			ChatHeadUtils.unregisterCallReceiver();
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getString(R.string.sticky_caller_settings));
		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		switch (buttonView.getId())
		{
		case R.id.saved_contact_card_checkbox:
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.ENABLE_KNOWN_NUMBER_CARD, isChecked);
			break;

		case R.id.sticky_caller_checkbox:
			HikeSharedPreferenceUtil.getInstance().saveData(StickyCaller.ACTIVATE_STICKY_CALLER, isChecked);
			if (isChecked)
			{
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_TOGGLE, null,
						AnalyticsConstants.StickyCallerEvents.ACTIVATE_BUTTON, null);
			}
			else
			{
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_TOGGLE, null,
						AnalyticsConstants.StickyCallerEvents.DEACTIVATE_BUTTON, null);
			}
			break;
		}
	}

}
