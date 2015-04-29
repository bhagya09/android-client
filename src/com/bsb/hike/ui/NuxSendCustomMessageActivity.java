package com.bsb.hike.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.Utils;

public class NuxSendCustomMessageActivity extends HikeAppStateBaseFragmentActivity 
{

	private TextView tapToChangeView;

	private EditText textMessageView;

	private HorizontalFriendsFragment newFragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (Utils.requireAuth(this))
		{
			return;
		}

		getSupportActionBar().hide();

		HikeMessengerApp app = (HikeMessengerApp) getApplication();

		app.connectToService();

		setContentView(R.layout.nux_send_custom_message);

		bindViews(savedInstanceState);

		processViewElemets();

	}

	private void bindViews(Bundle savedInstanceState)
	{

		textMessageView = (EditText) findViewById(R.id.multiforward_text_message);
		tapToChangeView = (TextView) findViewById(R.id.tap_to_write);

		if (savedInstanceState == null)
		{

			FragmentManager fm = getSupportFragmentManager();
			newFragment = (HorizontalFriendsFragment) fm.findFragmentByTag("chatFragment");
			if (newFragment == null)
			{
				newFragment = new HorizontalFriendsFragment();
			}

			FragmentTransaction ft = fm.beginTransaction();
			ft.add(R.id.horizontal_friends_placeholder, newFragment, "chatFragment").commit();
		}

		textMessageView.setOnEditorActionListener(new OnEditorActionListener()
		{

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					View view = newFragment.getView();
					view.setId(R.id.nux_next_selection_button);
					newFragment.onClick(view);
				}
				return false;
			}
		});
	}

	private void processViewElemets()
	{
		NUXManager mmNuxManager = NUXManager.getInstance();
		NuxCustomMessage mmCustomMessage = mmNuxManager.getNuxCustomMessagePojo();

		if (!TextUtils.isEmpty(mmCustomMessage.getCustomMessage()))
		{
			textMessageView.setText(mmCustomMessage.getCustomMessage());
			textMessageView.setSelection(mmCustomMessage.getCustomMessage().length());
		}

		tapToChangeView.setText(mmCustomMessage.getIndicatorText());
	}

	public String getCustomMessage()
	{
		if (TextUtils.isEmpty(textMessageView.getText()))
		{
			return NUXManager.getInstance().getNuxCustomMessagePojo().getCustomMessage();
		}
		else
		{
			return textMessageView.getText().toString();
		}
	}
}
