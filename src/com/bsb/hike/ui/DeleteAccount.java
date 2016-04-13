package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.tasks.DeleteAccountTask.DeleteAccountListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomFontTextView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

public class DeleteAccount extends HikeAppStateBaseFragmentActivity implements DeleteAccountListener
{
	private CustomFontTextView countryName;
	
	private CustomFontEditText countryCode, phoneNum;
	
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

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.delete_account);
		
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	private void initViewComponents()
	{
		countryName = (CustomFontTextView) findViewById(R.id.selected_country_name);
		countryCode = (CustomFontEditText) findViewById(R.id.country_picker);
		phoneNum = (CustomFontEditText) findViewById(R.id.et_enter_num);
		Utils.setupCountryCodeData(this, country_code, countryCode, countryName, countriesArray, countriesMap, codesMap, languageMap);
	}

	public void onCountryPickerClick(View v)
	{

		Intent intent = new Intent(this, CountrySelectActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_DELETE_ACCOUNT, true);
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
				HikeDialogFactory.showDialog(this, HikeDialogFactory.DELETE_ACCOUNT_DIALOG, new HikeDialogListener()
				{
					
					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
					}
					
					@Override
					public void neutralClicked(HikeDialog hikeDialog)
					{
						
					}
					
					@Override
					public void negativeClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
					}
				});
			}
			else
			{
				phoneNum.setBackgroundResource(R.drawable.bg_country_picker_selector);
				
				HikeDialogFactory.showDialog(this, HikeDialogFactory.DELETE_ACCOUNT_CONFIRM_DIALOG, new HikeDialogListener()
				{
					
					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
						task = new DeleteAccountTask(DeleteAccount.this, true, getApplicationContext());
						task.execute();
						showProgressDialog();
					}
					
					@Override
					public void neutralClicked(HikeDialog hikeDialog)
					{
					}
					
					@Override
					public void negativeClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
					}
				});
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
	public void accountDeleted(final boolean isSuccess)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (isSuccess)
				{
					accountDeleted();
				}
				else
				{
					dismissProgressDialog();
					int duration = Toast.LENGTH_LONG;
					Toast toast = Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.delete_account_failed), duration);
					toast.show();
				}
				task = null;
			}
		});
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
	
	@Override
	public void onBackPressed()
	{
		finish();
	}

}
