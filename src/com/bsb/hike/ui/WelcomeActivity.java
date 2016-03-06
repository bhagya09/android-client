package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.WelcomeTutorial;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class WelcomeActivity extends HikeAppStateBaseFragmentActivity implements SignupTask.OnSignupTaskProgressUpdate, OnLongClickListener
{
	private Button mAcceptButton;

	private ViewGroup loadingLayout;

	private View tcText;

	private ViewGroup tcContinueLayout;

	private boolean isMicromaxDevice;

	private Dialog errorDialog;
	
	private int stagingToggle = AccountUtils._PRODUCTION_HOST;

	SignupTask mTask; 
	
	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);
		Logger.d("Signup", "WelcomeActivity onCreate");
		Utils.setupServerURL(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.PRODUCTION, true),
				Utils.switchSSLOn(getApplicationContext()));
		HttpRequestConstants.setUpBase();

		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = findViewById(R.id.terms_and_conditions);
		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);
		findViewById(R.id.welcome_hike_logo_container).setOnLongClickListener(this);

		String model = Build.MODEL;
		String manufacturer = Build.MANUFACTURER;

		if (model != null)
		{
			model = model.toUpperCase();

			if (HikeConstants.MICROMAX.equalsIgnoreCase(manufacturer))
			{
				isMicromaxDevice = true;
			}
			else
			{
				if (model.contains(HikeConstants.MICROMAX))
				{
					isMicromaxDevice = true;
				}
			}
		}
        tcContinueLayout.setVisibility(View.VISIBLE);
		if ((savedState != null) && (savedState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR)))
		{
			showError();
		}
		else if ((savedState != null) && (savedState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING)))
		{
			onClick(mAcceptButton);
		}

		tcText.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(WelcomeActivity.this, WebViewActivity.class);
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, HikeConstants.T_AND_C_URL);
				intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.terms_privacy));
				startActivity(intent);
			}
		});

		ImageView micromaxImage = (ImageView) findViewById(R.id.ic_micromax);
		micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
		
		/**
		 * Conditionally init bots
		 */
		if (HikeMessengerApp.hikeBotInfoMap.isEmpty())
		{
			BotUtils.initBots();
		}
	}

	public void onHikeIconClicked(View v)
	{
		if (AppConfig.ALLOW_STAGING_TOGGLE)
		{
			changeHost();
		}
	}
	
	private void openServerHostChangeActivity()
	{
		if (AppConfig.ALLOW_STAGING_TOGGLE)
		{
			Intent intent = new Intent(this, ServerHostChangeActivity.class);
			startActivity(intent);
		}
	}

	private void changeHost()
	{
		Logger.d(getClass().getSimpleName(), "Hike Icon CLicked");

		SharedPreferences sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		boolean production = !sharedPreferences.getBoolean(HikeMessengerApp.PRODUCTION, true);

        /*
         * Change Platform content storage path based on change of is production variable
         */
        PlatformContent.init(production);

		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putBoolean(HikeMessengerApp.PRODUCTION, production);
		editor.putInt(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, production ? AccountUtils._PRODUCTION_HOST : AccountUtils._STAGING_HOST);
		editor.commit();
		HttpRequestConstants.toggleStaging();

		Utils.setupServerURL(production, Utils.switchSSLOn(this));

		Toast.makeText(WelcomeActivity.this, AccountUtils.base, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, errorDialog!=null);
		super.onSaveInstanceState(outState);
	}

	public void onClick(View v)
	{
		if (v.getId() == mAcceptButton.getId())
		{
			// Disable the t and c button
			tcText.setEnabled(false);
			loadingLayout.setVisibility(View.VISIBLE);
			mAcceptButton.setVisibility(View.GONE);
			mTask = SignupTask.startTask(this);
		}
	}

	@Override
	public void onFinish(boolean success)
	{
	}

	private void showError()
	{
		Logger.d("WelcomeActivity", "showError");
		loadingLayout.setVisibility(View.GONE);
		mAcceptButton.setVisibility(View.VISIBLE);
		showNetworkErrorPopup();
	}
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		Logger.d("Signup", "Welcome onresume");
		super.onResume();
	}

	@Override
	public void onProgressUpdate(StateValue value)
	{
		Logger.d("Signup", " Welcome acitivity  onProgressUpdate state : " + value.state + " val : "+value.value);
		if (value.state == SignupTask.State.ERROR)
		{
			showError();
		}
		else if (value.state == SignupTask.State.MSISDN)
		{
			Intent intent = new Intent(this, SignupActivity.class);
			if (TextUtils.isEmpty(value.value))
			{
				intent.putExtra(HikeConstants.Extras.MSISDN, false);
			}
			else
			{
				intent.putExtra(HikeConstants.Extras.MSISDN, true);
			}
			if(!this.isFinishing())
			{
				startActivity(intent);
			}
			finish();
		}
	}

	public void onBackPressed()
	{
		if (mTask != null)
		{
			mTask.cancelTask();
		}
		super.onBackPressed();
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{
		
		@Override
		public void onPageSelected(int position)
		{
			SharedPreferences sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			int viewed = sharedPreferences.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1); 
			Editor editor = sharedPreferences.edit();
			switch (position)
			{
			case 0:
				if(viewed < HikeConstants.WelcomeTutorial.INTRO_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.INTRO_VIEWED.ordinal());
				}
				break;
			case 1:
				if(viewed < HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.STICKER_VIEWED.ordinal());
				}
				break;
			case 2:
				if(viewed < HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.CHAT_BG_VIEWED.ordinal());
				}
				break;
			}
			editor.commit();
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onPageScrollStateChanged(int position)
		{
			// TODO Auto-generated method stub
		}
	};
	
	
	private void showNetworkErrorPopup()
	{
		errorDialog = new Dialog(this, R.style.Theme_CustomDialog);
		errorDialog.setContentView(R.layout.no_internet_pop_up);
		errorDialog.setCancelable(true);
		Button btnOk = (Button) errorDialog.findViewById(R.id.btn_ok);
		btnOk.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				mAcceptButton.performClick();
				errorDialog.dismiss();
			}
		});
		if(!this.isFinishing())
		{
			errorDialog.show();
		}
	}

	@Override
	public boolean onLongClick(View v)
	{
		if(v != null && v.getId() == R.id.welcome_hike_logo_container)
		{
			openServerHostChangeActivity();
			
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// TODO Auto-generated method stub
		return true;
	}

}
