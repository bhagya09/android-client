package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends Activity implements Listener
{
	private LinearLayout creditItemContainer;
	private TextView mTitleView;
	private TextView creditsNum;
	private Button inviteFriendsBtn;
	private TextView friendsNumTxt;
	private SharedPreferences settings;
	private TextView everyMonthTxt;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);

		if(getIntent().getBooleanExtra(HikeConstants.Extras.FIRST_TIME_USER, false))
		{
			Button mFeedbackButton = (Button) findViewById(R.id.title_icon);
			View mButtonBar = (View) findViewById(R.id.button_bar_2);

			mFeedbackButton.setText(R.string.done);
			mFeedbackButton.setVisibility(View.VISIBLE);
			mButtonBar.setVisibility(View.VISIBLE);
		}

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		mTitleView = (TextView) findViewById(R.id.title);
		creditItemContainer = (LinearLayout) findViewById(R.id.credit_item_container);
		creditsNum = (TextView) findViewById(R.id.credit_no);
		inviteFriendsBtn = (Button) findViewById(R.id.invite_now);
		friendsNumTxt = (TextView) findViewById(R.id.friends_num);
		everyMonthTxt = (TextView) findViewById(R.id.every_month_text);

		String everyMonth = getString(R.string.every_month);
		SpannableString everyMonthSpan = new SpannableString(everyMonth);
		String stringToBeFormatted = getString(R.string.string_to_be_formatted);
		String stringToBeFormatted2 = getString(R.string.string_to_be_formatted2);
		everyMonthSpan.setSpan(
								new StyleSpan(Typeface.BOLD), 
								everyMonth.indexOf(stringToBeFormatted), 
								everyMonth.indexOf(stringToBeFormatted) + stringToBeFormatted.length() + 1,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
								);
		everyMonthSpan.setSpan(
								new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), 
								everyMonth.indexOf(stringToBeFormatted), 
								everyMonth.indexOf(stringToBeFormatted) + stringToBeFormatted.length() + 1,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
								);
		everyMonthSpan.setSpan(
				new StyleSpan(Typeface.BOLD), 
				everyMonth.indexOf(stringToBeFormatted2), 
				everyMonth.indexOf(stringToBeFormatted2) + stringToBeFormatted2.length() + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
		everyMonthSpan.setSpan(
				new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), 
				everyMonth.indexOf(stringToBeFormatted2), 
				everyMonth.indexOf(stringToBeFormatted2) + stringToBeFormatted2.length() + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
		everyMonthTxt.setText(everyMonthSpan);

		mTitleView.setText("Free SMS");

		updateCredits();

		updateInviteeNum();

		inviteFriendsBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
				Intent i = new Intent(CreditsActivity.this, HikeListActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(HikeConstants.ADAPTER_NAME, HikeInviteAdapter.class.getName());
				startActivity(i);
			}
		});

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.INVITEE_NUM_CHANGED, this);
	}

	@Override
	protected void onDestroy() 
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.INVITEE_NUM_CHANGED, this);
		super.onDestroy();
	}

	public void onTitleIconClick(View v)
	{
		finish();
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateCredits();
				}
			});
		}
		else if(HikePubSub.INVITEE_NUM_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateInviteeNum();
				}
			});
		}
	}

	private void updateCredits()
	{
		creditsNum.setText(settings.getInt(HikeMessengerApp.SMS_SETTING, 0) + "");
	}

	private void updateInviteeNum()
	{
		int numInvited = settings.getInt(HikeMessengerApp.INVITED, 0);
		int numHike = settings.getInt(HikeMessengerApp.INVITED_JOINED, 0);

		String formatString = getResources().getString(R.string.friends_on_hike_0);
		String num = Integer.toString(numInvited);
		String formatted = String.format(formatString, num);

		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
				start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lightblack)), start, start + num.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		friendsNumTxt.setText(str);

		creditItemContainer.removeAllViews();

		LayoutInflater layoutInflater = LayoutInflater.from(CreditsActivity.this);
		View v = layoutInflater.inflate(R.layout.credits_item, null);
		TextView friendNum = (TextView) v.findViewById(R.id.friends_no);
		TextView smsNum = (TextView) v.findViewById(R.id.sms_no);

		v.setBackgroundResource(R.drawable.credit_item_bckg_selected);
		int smsNo = numHike * HikeConstants.NUM_SMS_PER_FRIEND;
		friendNum.setText(numHike + "");
		smsNum.setText("+"+smsNo+"");
		creditItemContainer.addView(v);
	}
}
