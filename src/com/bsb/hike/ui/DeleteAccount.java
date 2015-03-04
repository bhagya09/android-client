package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.tasks.DeleteAccountTask.DeleteAccountListener;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class DeleteAccount extends HikeAppStateBaseFragmentActivity implements DeleteAccountListener
{
	private TextView countryName, phoneNum;

	private EditText countryCode;
	
	ProgressDialog progressDialog;

	DeleteAccountTask task;
	
	private String country_code;

	private ArrayList<String> countriesArray = new ArrayList<String>();

	private HashMap<String, String> countriesMap = new HashMap<String, String>();

	private HashMap<String, String> codesMap = new HashMap<String, String>();

	private HashMap<String, String> languageMap = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.delete_account_confirmation);
		
		initViewComponents();
		setupActionBar();
		handleOrientationChanegs();
	}

	private void handleOrientationChanegs()
	{

		task = (DeleteAccountTask) getLastCustomNonConfigurationInstance();
		if (task != null)
		{
			showProgressDialog();
			task.setActivity(this);
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.back_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.delete_account);
		backContainer.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void initViewComponents()
	{
		countryName = (TextView) findViewById(R.id.selected_country_name);
		countryCode = (EditText) findViewById(R.id.country_picker);
		phoneNum = (TextView) findViewById(R.id.et_enter_num);
		Utils.setupCountryCodeData(this, country_code, countryCode, countryName, countriesArray, countriesMap, codesMap, languageMap);
	}

	public void onCountryPickerClick(View v)
	{

		Intent intent = new Intent(this, CountrySelectActivity.class);
		this.startActivityForResult(intent, HikeConstants.ResultCodes.SELECT_COUNTRY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode != RESULT_OK)
		{
			return;
		}
		if (requestCode == HikeConstants.ResultCodes.SELECT_COUNTRY)
		{
			if (data != null)
			{
				selectCountry(data);
			}
		}

	}

	private void selectCountry(Intent intent)
	{
		countryName.setText(intent.getStringExtra(CountrySelectActivity.RESULT_COUNTRY_NAME));
		countryCode.setText(intent.getStringExtra(CountrySelectActivity.RESULT_COUNTRY_CODE));
	}

	public void deleteAccountClicked(View v)
	{
		String phoneNu = phoneNum.getText().toString();
		String countryCod = countryCode.getText().toString();
		String fullMSISDN = "+" + countryCod + phoneNu;
		
		
		if (TextUtils.isEmpty(phoneNu))
		{
			phoneNum.setHintTextColor(getResources().getColor(R.color.red_empty_field));
			phoneNum.setBackgroundResource(R.drawable.bg_phone_bar);
			phoneNum.startAnimation(AnimationUtils.loadAnimation(DeleteAccount.this, R.anim.shake));
			return;			
		}
		else
		{
			String msisdn = getApplicationContext().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			
			if(!fullMSISDN.equalsIgnoreCase(msisdn))
			{				
				final CustomAlertDialog correctMSISDNConfirmDialog = new CustomAlertDialog(this);
				correctMSISDNConfirmDialog.setHeader(R.string.incorrect_msisdn_warning);
				correctMSISDNConfirmDialog.setBody(R.string.incorrect_msisdn_msg);
				
				View.OnClickListener correctMSISDNConfirmListener = new View.OnClickListener() 
				{					
					@Override
					public void onClick(View v) 
					{
						correctMSISDNConfirmDialog.dismiss();
					}
				};
				correctMSISDNConfirmDialog.setOkButton(R.string.ok, correctMSISDNConfirmListener);
				correctMSISDNConfirmDialog.setCancelButtonVisibility(View.GONE);
				correctMSISDNConfirmDialog.show();
			}
			else
			{
				phoneNum.setBackgroundResource(R.drawable.bg_country_picker_selector);
				final CustomAlertDialog firstConfirmDialog = new CustomAlertDialog(this);
				firstConfirmDialog.setHeader(R.string.are_you_sure);
				firstConfirmDialog.setBody(R.string.delete_confirm_msg_1);				
				View.OnClickListener firstDialogContinueClickListener = new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						firstConfirmDialog.dismiss();
						task = new DeleteAccountTask(DeleteAccount.this, true, getApplicationContext());

						Utils.executeBoolResultAsyncTask(task);
						showProgressDialog();
					}
				};

				View.OnClickListener firstDialogOnCancelListener = new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						firstConfirmDialog.dismiss();
					}
				};
				firstConfirmDialog.setOkButton(R.string.confirm, firstDialogContinueClickListener);
				firstConfirmDialog.setCancelButton(R.string.cancel, firstDialogOnCancelListener);
				firstConfirmDialog.show();
			}
		}
	}

	/**
	 * For redirecting back to the Welcome Screen.
	 */
	public void accountDeleted()
	{
		dismissProgressDialog();
		/*
		 * First we send the user to the Main Activity(MessagesList) from there we redirect him to the welcome screen.
		 */
		Intent dltIntent = new Intent(this, HomeActivity.class);
		dltIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(dltIntent);
		task = null;
	}

	private void showProgressDialog()
	{
		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setMessage("Please wait..");
		progressDialog.show();
	}

	public void dismissProgressDialog()
	{
		if (progressDialog != null)
			progressDialog.dismiss();
	}

	@Override
	public void accountDeleted(boolean isSuccess)
	{
		if (isSuccess)
		{
			accountDeleted();
		}
		else
		{
			dismissProgressDialog();
		}
		task = null;
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		// TODO Auto-generated method stub
		return task;
	}

	@Override
	protected void onDestroy()
	{
		if (progressDialog != null)
		{
			progressDialog.cancel();
		}
		if (task != null)
		{
			task.setActivity(null);
		}
		super.onDestroy();
	}
}
