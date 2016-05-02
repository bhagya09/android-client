package com.bsb.hike.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.widget.TextView.OnEditorActionListener;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.backup.HikeCloudSettingsManager;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.*;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomFontEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SignupActivity extends ChangeProfileImageBaseActivity implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, OnClickListener,
		OnCancelListener, Listener
{
	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;

	private ViewGroup numLayout;

	private ViewGroup pinLayout;

	private ViewGroup nameLayout;

	private ViewGroup genderLayout;

	private ViewGroup scanningContactsLayout;

	private ViewGroup backupFoundLayout;

	private ViewGroup restoringBackupLayout;

	private TextView infoTxt;

	private TextView loadingText;

	private ViewGroup loadingLayout;

	private CustomFontEditText enterEditText;

	private TextView invalidNum;

	private CustomFontEditText countryPicker;

	private TextView selectedCountryName;

	private Button callmeBtn;

	private ImageView mIconView;

	private CustomFontEditText birthdayText;

	private TextView maleText;

	private TextView femaleText;

	private ImageView profilePicCamIcon;

	private LocalLanguage selectedLocalLanguage;

	private Handler mHandler;

	private boolean addressBookError = false;

	private boolean msisdnErrorDuringSignup = false;

	public static final int POST_SIGNUP = 8;

	public static final int RESTORING_BACKUP = 7;

	public static final int BACKUP_FOUND = 6;

	public static final int SCANNING_CONTACTS = 5;

	public static final int GENDER = 4;

	public static final int NAME = 3;

	public static final int PIN = 2;

	public static final int NUMBER = 1;
	
	public static int callMeWaitTime;

	private String countryCode;

	//private final String defaultCountryName = "India";

	private boolean showingSecondLoadingTxt = false;

	private SharedPreferences accountPrefs;

	private ActivityState mActivityState;

	private Dialog dialog;

	//private Session.StatusCallback statusCallback = new SessionStatusCallback();

	private CountDownTimer countDownTimer;

	private boolean showingNumberConfimationDialog;

	TextView mActionBarTitle;

	private Dialog errorDialog;

	View nextBtn;

	View nextBtnContainer;

	View selectedCountryPicker;

	private TextView invalidPin;

	private View verifiedPin;

	private ArrayList<String> countriesArray = new ArrayList<String>();

	private HashMap<String, String> countriesMap = new HashMap<String, String>();

	private HashMap<String, String> codesMap = new HashMap<String, String>();

	private HashMap<String, String> languageMap = new HashMap<String, String>();

	private ImageView arrow;

	private TextView postText;
	
	private ViewProperties sdCardProp;
	
	private boolean restoreInitialized = false;

	private int defAvBgColor;

	private ProfileImageLoader profileImageLoader;

	private boolean showStickerRestoreDiffDpiDialog = false;

	private class ActivityState
	{
		public RequestToken pinCallRequestToken; /* the task to update the global profile */

		public Thread downloadImageTask; /*
										 * the task to download the picasa image
										 */

		public String destFilePath = null;

		public Bitmap profileBitmap = null; /*
											 * the bitmap before the user saves it
											 */

		public String userName = null;

		public long timeLeft = 0;

		public boolean fbConnected = false;

		public Boolean isFemale = null;

		public Birthday birthday = null;
		
		public String restoreStatus = null;
	}
	
	private class ViewProperties
	{
		public int left;
		
		public int top;
		
		public int width;
		
		public int height;
	}

	String[] mPubSubEvents = {HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED, HikePubSub.CLOUD_SETTINGS_RESTORE_SUCCESS};

        /* Empty onNewIntent is created so as to avoid overriding the existing intent of SignupActivity,
           if we don't do this then SignupActivity will be launched as fresh i.e. requesting msisdn */
	@Override
	protected void onNewIntent(Intent intent)
	{

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Logger.d("Signup", "SingupActivity onCreate");
		setContentView(R.layout.signup);

		mHandler = new Handler();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
	
		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		genderLayout = (ViewGroup) findViewById(R.id.gender_layout);
		scanningContactsLayout = (ViewGroup) findViewById(R.id.scanning_contacts_layout);
		backupFoundLayout = (ViewGroup) findViewById(R.id.backup_found_layout);
		restoringBackupLayout = (ViewGroup) findViewById(R.id.restoring_backup_layout);

		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof ActivityState)
		{
			mActivityState = (ActivityState) o;
			if (mActivityState.pinCallRequestToken != null)
			{
				mActivityState.pinCallRequestToken.addRequestListener(pincallRequestListener);
				dialog = ProgressDialog.show(this, null, getString(R.string.calling_you));
				dialog.setCancelable(true);
				dialog.setOnCancelListener(this);
			}
			else if (mActivityState.downloadImageTask != null && mActivityState.downloadImageTask.getState() != java.lang.Thread.State.TERMINATED)
			{
				dialog = ProgressDialog.show(this, null, getResources().getString(R.string.downloading_image));
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}

		setupActionBar();
		if (savedInstanceState != null)
		{
			msisdnErrorDuringSignup = savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR);
			int dispChild = savedInstanceState.getInt(HikeConstants.Extras.SIGNUP_PART);
			showingSecondLoadingTxt = savedInstanceState.getBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT);
			removeAnimation();
			viewFlipper.setDisplayedChild(dispChild);
			switch (dispChild)
			{
			case NUMBER:
				countryCode = savedInstanceState.getString(HikeConstants.Extras.COUNTRY_CODE);
				prepareLayoutForFetchingNumber();
				if (showingSecondLoadingTxt)
				{
					loadingText.setText(R.string.almost_there_signup);
				}
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				break;
			case PIN:
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				if (savedInstanceState.getBoolean(HikeConstants.Extras.SHOWING_INVALID_PIN_ERROR, false))
				{
					invalidPin.setVisibility(View.VISIBLE);
				}
				break;
			case NAME:
				prepareLayoutForGettingName(savedInstanceState, false);
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				break;
			case GENDER:
				prepareLayoutForGender(savedInstanceState);
				break;
			case SCANNING_CONTACTS:
				prepareLayoutForScanning(savedInstanceState);
				break;
			case BACKUP_FOUND:
				prepareLayoutForBackupFound(savedInstanceState);
				break;
			case RESTORING_BACKUP:
				prepareLayoutForRestoringAnimation(savedInstanceState,null);
				break;
			case POST_SIGNUP:
				prepareLayoutForPostSignup(savedInstanceState);
				break;
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING))
			{
				startLoading();
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR))
			{
				showErrorMsg();
			}
			mTask = SignupTask.startTask(this, mActivityState.userName, mActivityState.isFemale, mActivityState.birthday, mActivityState.profileBitmap);
		}
		else
		{
			if (getIntent().hasExtra(HikeConstants.Extras.MSISDN))
			{
				if (getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false))
				{
					viewFlipper.setDisplayedChild(NAME);
					prepareLayoutForGettingName(savedInstanceState, false);
				}
				else
				{
					viewFlipper.setDisplayedChild(NUMBER);
					prepareLayoutForFetchingNumber();
				}
			}

			mTask = SignupTask.startTask(this);
		}
		setAnimation();
		setListeners();

		HikeMessengerApp.getPubSub().addListeners(this, mPubSubEvents);
		setWindowSoftInputState();
	}

	private void setWindowSoftInputState()
	{
		int displayedChild = viewFlipper.getDisplayedChild();
		switch (displayedChild)
		{
		case NUMBER:
		case PIN:
		case NAME:
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			break;
		default:
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			break;
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);

		mActionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
		nextBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.next_btn);
		nextBtnContainer = actionBarView.findViewById(R.id.next_btn_container);

		nextBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				submitClicked();
			}
		});
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	private void setupActionBarTitle()
	{
		int displayedChild = viewFlipper.getDisplayedChild();
		if (displayedChild == NUMBER)
		{
			mActionBarTitle.setText(R.string.phone_num);
		}
		else if (displayedChild == PIN)
		{
			mActionBarTitle.setText(R.string.pin_signup);
		}
		else if (displayedChild == NAME)
		{
			mActionBarTitle.setText(R.string.about_you);
		}
		else if (displayedChild == GENDER)
		{
			mActionBarTitle.setText(R.string.tell_us_more);
		}
		else if (displayedChild == SCANNING_CONTACTS || displayedChild == POST_SIGNUP)
		{
			mActionBarTitle.setText("");
		}
		else if (displayedChild == BACKUP_FOUND)
		{
			mActionBarTitle.setText(R.string.restore_account);
		}
		else if (displayedChild == RESTORING_BACKUP)
		{
			mActionBarTitle.setText(R.string.account_backup);
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		if (mActivityState.pinCallRequestToken == null)
		{
			if (success)
			{
				/*
				 * Setting the app value as to if the user is Indian or not.
				 */
				String countryCode = accountPrefs.getString(HikeMessengerApp.COUNTRY_CODE, "");
				boolean isIndianUser = countryCode.equals(HikeConstants.INDIA_COUNTRY_CODE);
				boolean isSAUser = countryCode.equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE);

				Utils.setSSLAllowed(countryCode);
				Editor accountEditor = accountPrefs.edit();
				accountEditor.putBoolean(HikeMessengerApp.JUST_SIGNED_UP, true);
				if (mActivityState != null)
				{
					accountEditor.putBoolean(HikeMessengerApp.FB_SIGNUP, mActivityState.fbConnected);
				}
				accountEditor.commit();

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, isIndianUser);
				editor.putBoolean(HikeConstants.SSL_PREF, !(isIndianUser || isSAUser));
				editor.remove(HikeMessengerApp.TEMP_COUNTRY_CODE);
				editor.commit();

				HikeMessengerApp.setIndianUser(isIndianUser);
				/*
				 * Update the urls to use ssl or not.
				 */
				Utils.setupUri();
				HttpRequestConstants.toggleSSL();

				mHandler.removeCallbacks(startWelcomeScreen);

				if (showStickerRestoreDiffDpiDialog)
				{
					showStickerRestoreDialog();
				}

				else
				{
					mHandler.postDelayed(startWelcomeScreen, 2500);
				}

				SharedPreferences settings = getApplication().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
				Editor ed = settings.edit();
				ed.putBoolean(HikeMessengerApp.SIGNUP_COMPLETE, true);
				ed.commit();
				
				StickerManager.getInstance().doSignupTasks();
				JSONObject sessionDataObject = HAManager.getInstance().recordAndReturnSessionStart();
				Utils.sendSessionMQTTPacket(SignupActivity.this, HikeConstants.FOREGROUND, sessionDataObject);
				Utils.appStateChanged(getApplicationContext(), false, false, false, true, false);
				LocalLanguageUtils.requestLanguageOrderListFromServer();
				HAManager.getInstance().logUserGoogleAccounts();
			}
			else if (mCurrentState != null && mCurrentState.value != null && mCurrentState.value.equals(HikeConstants.CHANGE_NUMBER))
			{
				restartTask();
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}
	}
	
	Runnable startWelcomeScreen = new Runnable()
	{
		@Override
		public void run()
		{
			openHomeActivity();
		}
	};

	IRequestListener pincallRequestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			onFinish(true);
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			Toast.makeText(SignupActivity.this, R.string.call_me_fail, Toast.LENGTH_LONG).show();
			onFinish(false);
		}
	};
	
	public void onClick(View v)
	{
		if (callmeBtn != null && v.getId() == callmeBtn.getId())
		{
			JSONObject request = new JSONObject();
			try
			{
				request.put("msisdn", accountPrefs.getString(HikeMessengerApp.MSISDN_ENTERED, null));
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}

			mActivityState.pinCallRequestToken = HttpRequests.signUpPinCallRequest(request, pincallRequestListener);
			mActivityState.pinCallRequestToken .execute();
			
			dialog = ProgressDialog.show(this, null, getResources().getString(R.string.calling_you));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return mActivityState;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}
	
	protected void onDestroy()
	{
		super.onDestroy();

		HikeMessengerApp.getPubSub().removeListeners(this, mPubSubEvents);
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		if (mActivityState != null && mActivityState.pinCallRequestToken != null)
		{
			mActivityState.pinCallRequestToken.removeListener(pincallRequestListener);
		}
		if (countDownTimer != null)
		{
			countDownTimer.cancel();
			countDownTimer = null;
		}
	}

	private void startLoading()
	{
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.VISIBLE);
		}
		if(infoTxt != null)
		{
			infoTxt.setVisibility(View.GONE);
		}
		toggleActionBarElementsEnable(false);
		if (invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (invalidPin != null)
		{
			invalidPin.setVisibility(View.INVISIBLE);
		}
		if (countryPicker != null)
		{
			countryPicker.setEnabled(false);
			selectedCountryPicker.setEnabled(false);
			enterEditText.setEnabled(false);
		}
		if (callmeBtn != null)
		{
			callmeBtn.setEnabled(false);
		}
	}

	private void endLoading()
	{
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		if (infoTxt != null)
		{
			infoTxt.setVisibility(View.VISIBLE);
		}
		toggleActionBarElementsEnable(true);
		if (countryPicker != null)
		{
			countryPicker.setEnabled(true);
			selectedCountryPicker.setEnabled(true);
			enterEditText.setEnabled(true);
		}
		if (callmeBtn != null)
		{
			callmeBtn.setEnabled(true);
		}
	}

	private void submitClicked()
	{
		if (viewFlipper.getDisplayedChild() == BACKUP_FOUND || viewFlipper.getDisplayedChild() == RESTORING_BACKUP)
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BACKUP_RESTORE_SKIP);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			viewFlipper.setDisplayedChild(POST_SIGNUP);
			prepareLayoutForPostSignup(null);
			mTask.addUserInput(null);
			BotUtils.initBots();
			return;
		}
		if (invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (viewFlipper.getDisplayedChild() == NUMBER && isInvalidCountryCode())
		{
			loadingLayout.setVisibility(View.GONE);
			infoTxt.setVisibility(View.VISIBLE);
			toggleActionBarElementsEnable(true);
			invalidNum.setVisibility(View.VISIBLE);
			return;
		}

		if (enterEditText != null && TextUtils.isEmpty(enterEditText.getText().toString().replaceAll(" ", "")))
		{
			int displayedChild = viewFlipper.getDisplayedChild();
			int stringRes;
			if (displayedChild == NUMBER)
			{
				stringRes = R.string.enter_num;
			}
			else if (displayedChild == PIN)
			{
				stringRes = R.string.enter_pin;
			}
			else
			{
				stringRes = R.string.enter_name;
			}
			Toast toast = Toast.makeText(this, stringRes, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return;
		}
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			startLoading();
		}
		if (!addressBookError || viewFlipper.getDisplayedChild() == NAME)
		{
			if (viewFlipper.getDisplayedChild() == NUMBER && !enterEditText.getText().toString().matches(HikeConstants.VALID_MSISDN_REGEX))
			{
				loadingLayout.setVisibility(View.GONE);
				infoTxt.setVisibility(View.VISIBLE);
				toggleActionBarElementsEnable(true);
				invalidNum.setVisibility(View.VISIBLE);
			}
			else if (viewFlipper.getDisplayedChild() == GENDER)
			{
				if (mActivityState.isFemale != null)
				{
					mTask.addGender(mActivityState.isFemale);
					mTask.addUserInput(mActivityState.isFemale.toString());
					viewFlipper.setDisplayedChild(SCANNING_CONTACTS);
					prepareLayoutForScanning(null);
				}
				else
				{
					Toast toast = Toast.makeText(this, "please select your gender", Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
			else
			{
				final String input = enterEditText.getText().toString();
				if (viewFlipper.getDisplayedChild() == NUMBER)
				{
					/*
					 * Adding this check since some device's IME call this part of the code twice.
					 */
					if (showingNumberConfimationDialog)
					{
						return;
					}

					final String code = "+" + countryPicker.getText().toString();
					String number = code + input;

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.number_confirm_title);
					builder.setMessage(getString(R.string.number_confirmation_string, number));
					builder.setPositiveButton(R.string.CONFIRM, new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String number = code + input;
							Editor editor = accountPrefs.edit();
							editor.putString(HikeMessengerApp.TEMP_COUNTRY_CODE, code);
							editor.commit();
							
							Utils.setSSLAllowed(code);
							Utils.setupServerURL(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.PRODUCTION, true),
									Utils.switchSSLOn(getApplicationContext()));
							HttpRequestConstants.setUpBase();
							
							mTask.addUserInput(number);

							startLoading();
							dialog.cancel();
							if (Utils.scaledDensityMultiplier < 1.5)
							{
								Utils.hideSoftKeyboard(SignupActivity.this, enterEditText);
							}
						}
					});
					builder.setNegativeButton(R.string.EDIT, new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});
					Dialog dialog = builder.show();
					dialog.setOnCancelListener(new OnCancelListener()
					{

						@Override
						public void onCancel(DialogInterface dialog)
						{
							showingNumberConfimationDialog = false;
						}
					});
					showingNumberConfimationDialog = true;
				}
				else
				{
					if (!TextUtils.isEmpty(mActivityState.destFilePath))
					{
						mTask.addProfilePicPath(mActivityState.destFilePath, mActivityState.profileBitmap);
					}
					if (viewFlipper.getDisplayedChild() == NAME)
					{
						Utils.hideSoftKeyboard(this, enterEditText);
						mActivityState.userName = input;
						mTask.addUserName(mActivityState.userName);
						viewFlipper.setDisplayedChild(GENDER);
						prepareLayoutForGender(null);
					}
					mTask.addUserInput(input);
					if (birthdayText != null && !TextUtils.isEmpty(birthdayText.getText().toString()))
					{
						Calendar calendar = Calendar.getInstance();
						int currentYear = calendar.get(Calendar.YEAR);
						mActivityState.birthday = new Birthday(0, 0, currentYear - Integer.valueOf(birthdayText.getText().toString()));
						mTask.addBirthdate(mActivityState.birthday);
					}
				}
			}
		}
		else
		{
			showErrorMsg();
			addressBookError = false;
		}
	}

	private void initializeViews(ViewGroup layout)
	{
		switch (layout.getId())
		{
		case R.id.name_layout:
			enterEditText = (CustomFontEditText) layout.findViewById(R.id.et_enter_name);
			enterEditText.addTextChangedListener(nameWatcher);
			birthdayText = (CustomFontEditText) layout.findViewById(R.id.birthday);
			profilePicCamIcon = (ImageView) layout.findViewById(R.id.profile_cam);
			if(profilePicCamIcon != null)
			{
				profilePicCamIcon.setOnClickListener(new OnClickListener()
				{				
					@Override
					public void onClick(View v)
					{
						selectNewProfilePicture(SignupActivity.this, false, true);
					}
				});
			}
			break;
		case R.id.num_layout:
			enterEditText = (CustomFontEditText) layout.findViewById(R.id.et_enter_num);
			infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
			infoTxt.setVisibility(View.VISIBLE);
			verifiedPin = layout.findViewById(R.id.verified_pin);
			break;
		case R.id.pin_layout:
			enterEditText = (CustomFontEditText) layout.findViewById(R.id.et_enter_pin);
			infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
			invalidPin = (TextView) layout.findViewById(R.id.invalid_pin);
			verifiedPin = layout.findViewById(R.id.verified_pin);
			infoTxt.setVisibility(View.VISIBLE);
			invalidPin.setVisibility(View.INVISIBLE);
			break;
		case R.id.gender_layout:
			break;
		}
		infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		invalidNum = (TextView) layout.findViewById(R.id.invalid_num);
		countryPicker = (CustomFontEditText) layout.findViewById(R.id.country_picker);
		selectedCountryName = (TextView) layout.findViewById(R.id.selected_country_name);
		selectedCountryPicker = layout.findViewById(R.id.selected_country);
		callmeBtn = (Button) layout.findViewById(R.id.btn_call_me);
		mIconView = (ImageView) layout.findViewById(R.id.profile);
		
		if(mIconView != null)
		{
			mIconView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					selectNewProfilePicture(SignupActivity.this, false, true);
				}
			});
		}

		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		toggleActionBarElementsEnable(true);
		setupActionBarTitle();
	}


	private void prepareLayoutForFetchingNumber()
	{
		initializeViews(numLayout);

		countryPicker.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View arg0, boolean isFocus) {
				if (isFocus) {
					findViewById(R.id.country_code_view_group).setBackgroundResource(R.drawable.bg_phone_num_selected);
				} else {
					findViewById(R.id.country_code_view_group).setBackgroundResource(R.drawable.bg_phone_num_unselected);
				}

			}
		});

		countryPicker.setEnabled(true);
		selectedCountryPicker.setEnabled(true);
		enterEditText.setEnabled(true);
		enterEditText.requestFocus();

		Utils.setupCountryCodeData(this, countryCode, countryPicker, selectedCountryName, countriesArray, countriesMap, codesMap, languageMap);
		TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String number = manager.getLine1Number();
		if (number != null && number.startsWith("+91"))
		{
			number = number.replace("+91", "");
			enterEditText.setText(number);
			/*
			 * Saw a few crashes here. Catching the exception since we don't seem to be doing anything wrong here.
			 */
			try
			{
				enterEditText.setSelection(number.length());
			}
			catch (IndexOutOfBoundsException e)
			{
				Logger.w(getClass().getSimpleName(), "IOOB thrown while setting the number's textbox selection");
			}
		}
		infoTxt.setText(msisdnErrorDuringSignup ? R.string.enter_phone_again_signup : R.string.whats_your_number);
		invalidNum.setVisibility(View.INVISIBLE);
		loadingText.setText(R.string.verifying);
		nextBtnContainer.setVisibility(View.VISIBLE);
	}

	public void onCountryPickerClick(View v)
	{

		Intent intent = new Intent(this, CountrySelectActivity.class);
	    this.startActivityForResult(intent, HikeConstants.ResultCodes.SELECT_COUNTRY);
	}

	private void prepareLayoutForGettingPin(long timeLeft)
	{
		initializeViews(pinLayout);

		callmeBtn.setVisibility(View.VISIBLE);
		nextBtnContainer.setVisibility(View.VISIBLE);

		enterEditText.setText("");
		infoTxt.setText(R.string.enter_pin_signup);

		String tapHere = getString(R.string.tap_here_signup);
		String tapHereString = getString(R.string.wrong_num_signup);

		SpannableStringBuilder ssb = new SpannableStringBuilder(tapHereString);
		if (tapHereString.indexOf(tapHere) != -1)
		{
			ssb.setSpan(new ForegroundColorSpan(0xff6edcff), tapHereString.indexOf(tapHere), tapHereString.indexOf(tapHere) + tapHere.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		if (timeLeft > 0)
		{
			countDownTimer = new CountDownTimer(timeLeft, 1000)
			{

				@Override
				public void onTick(long millisUntilFinished)
				{
					long secondsUntilFinished = millisUntilFinished / 1000;
					int minutes = (int) (secondsUntilFinished / 60);
					int seconds = (int) (secondsUntilFinished % 60);
					String text = "(" + String.format("%1$02d:%2$02d", minutes, seconds) + " )";
					callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, text));
					mActivityState.timeLeft = millisUntilFinished;
					callmeBtn.setEnabled(false);
				}

				@Override
				public void onFinish()
				{
					callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, ""));
					callmeBtn.setEnabled(true);
					mActivityState.timeLeft = 0;
				}
			};
			countDownTimer.start();
		}
		else
		{
			callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, ""));
			callmeBtn.setEnabled(true);
		}
		loadingText.setText(R.string.verify_pin_signup);
	}

	private void prepareLayoutForGettingName(Bundle savedInstanceState, boolean addressBookScanningDone)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure we create the profile picture folder before the user sets one.
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		initializeViews(nameLayout);

		// Auto fill the name, if possible
		if (enterEditText != null)
		{
			String possibleOwnerName = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_NAME_SETTING, Utils.getOwnerName(SignupActivity.this));
			if (!TextUtils.isEmpty(possibleOwnerName))
			{
				enterEditText.setText(possibleOwnerName);
				try
				{
					enterEditText.setSelection(possibleOwnerName.length());
				} catch (IndexOutOfBoundsException e) {
					Logger.w(getClass().getSimpleName(), "IOOB thrown while setting the name's textbox selection");
				}
			}
		}
		// Autofill birth year if possible
		if (birthdayText != null)
		{
			int serverBirthdayYear = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_BIRTHDAY_YEAR, 0);
			if (serverBirthdayYear > 0)
			{
				Calendar calendar = Calendar.getInstance();
				int age = calendar.get(Calendar.YEAR) - serverBirthdayYear;
				if (age > 0)
					birthdayText.setText("" + age);
			}
		}

		if (!addressBookScanningDone)
		{
			Utils.hideSoftKeyboard(this, enterEditText);
		}

		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);

		if (TextUtils.isEmpty(msisdn))
		{
			Utils.logEvent(SignupActivity.this, HikeConstants.LogEvent.SIGNUP_ERROR);
			msisdnErrorDuringSignup = true;
			resetViewFlipper();
			restartTask();
			return;
		}

		TypedArray bgColorArray = Utils.getDefaultAvatarBG();

		int index = BitmapUtils.iconHash(msisdn) % (bgColorArray.length());

		defAvBgColor = bgColorArray.getColor(index, 0);

		// Remove any pending image selected by user in previous attempt.
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH);
		// Remove any temporary image saved in the process;
		Utils.removeTempProfileImage(msisdn);

		if(mActivityState.profileBitmap == null && savedInstanceState != null)
		{
			mActivityState.profileBitmap = savedInstanceState.getParcelable(HikeConstants.Extras.BITMAP);
		}

		if (mActivityState.profileBitmap != null)
		{
			mIconView.setImageBitmap(mActivityState.profileBitmap);
		}
		else if (getCachedProfilePic() != null)
		{
			mIconView.setImageDrawable(getCachedProfilePic());
		}
		else
		{
			fetchProfilePic(msisdn);
		}

		nextBtnContainer.setVisibility(View.VISIBLE);
		setupActionBarTitle();
	}

	private void fetchProfilePic(String msisdn)
	{
		profileImageLoader = new ProfileImageLoader(this, msisdn, mIconView, HikeConstants.PROFILE_IMAGE_DIMENSIONS, true, true);
		profileImageLoader.setLoaderListener(new ProfileImageLoader.LoaderListener() {
			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) {
				return null;
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1) {

			}

			@Override
			public void onLoaderReset(Loader<Boolean> arg0) {

			}

			@Override
			public void startDownloading() {
				beginImageDownload();
			}
		});
		profileImageLoader.loadProfileImage(getSupportLoaderManager());
	}

	/*
	Download image from the server.
	 */
	private void beginImageDownload()
	{
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String fileName = Utils.getProfileImageFileName(msisdn);
		HikeImageDownloader mImageDownloader = HikeImageDownloader.newInstance(msisdn, fileName, true, false, null, null, null, true, true);
		mImageDownloader.setTaskCallbacks(imageWorkerTaskCallback);
		mImageDownloader.startLoadingTask();
		// Set Default avatar till the image downloads
		setDefaultProfileImage(msisdn);
	}

	private void setDefaultProfileImage(String msisdn)
	{
		Drawable bd = getCachedProfilePic();
		if (bd == null)
		{
			String name = enterEditText.getText() == null? null : enterEditText.getText().toString();
			bd = HikeBitmapFactory.getDefaultTextAvatar(name,-1,defAvBgColor);
		}
		mIconView.setImageDrawable(bd);
	}

	private Drawable getCachedProfilePic()
	{
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		return HikeMessengerApp.getLruCache().getIconFromCache(msisdn + ProfileActivity.PROFILE_PIC_SUFFIX);
	}

	private HikeImageWorker.TaskCallbacks imageWorkerTaskCallback = new HikeImageWorker.TaskCallbacks() {
		@Override
		public void onProgressUpdate(float percent) {}

		@Override
		public void onCancelled() {}

		@Override
		public void onFailed() {}

		@Override
		public void onSuccess(Response result) {
			// This is NOT the main thread!
			setcontactManagerIcon();
			SignupActivity.this.mHandler.post(
					new Runnable() {
						@Override
						public void run() {
							if (profileImageLoader != null) {
								profileImageLoader.loadProfileImage(getSupportLoaderManager());
							}
						}
					}
			);
		}

		@Override
		public void onTaskAlreadyRunning() {}
	};


	private void setcontactManagerIcon()
	{
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		File profileImage = (new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT, Utils.getProfileImageFileName(msisdn)));
		if (profileImage.exists())
		{
			Bitmap smallerBitmap = HikeBitmapFactory.scaleDownBitmap(profileImage.getAbsolutePath(), HikeConstants.PROFILE_IMAGE_DIMENSIONS, HikeConstants.PROFILE_IMAGE_DIMENSIONS, Bitmap.Config.RGB_565,
					true, false);

			if (smallerBitmap != null)
			{
				final byte[] bytes = BitmapUtils.bitmapToBytes(smallerBitmap, Bitmap.CompressFormat.JPEG, 100);
				ContactManager.getInstance().setIcon(msisdn, bytes, true);
			}
		}
	}

	private void prepareLayoutForGender(Bundle savedInstanceState)
	{
		femaleText = (TextView) genderLayout.findViewById(R.id.female);
		maleText = (TextView) genderLayout.findViewById(R.id.male);
		if (savedInstanceState != null && savedInstanceState.containsKey(HikeConstants.Extras.GENDER))
		{
			mActivityState.isFemale = savedInstanceState.getBoolean(HikeConstants.Extras.GENDER);
			selectGender(mActivityState.isFemale);
		}
		// Autofill using data received from server
		else
		{
			String serverGender = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_GENDER_SETTING,"");
			if (!TextUtils.isEmpty(serverGender))
			{
				if (serverGender.equals(HikeConstants.FEMALE))
					selectGender(true);
				else if (serverGender.equals(HikeConstants.MALE))
					selectGender(false);
			}

		}
		nextBtnContainer.setVisibility(View.VISIBLE);
		setupActionBarTitle();
	}

	private void prepareLayoutForScanning(Bundle savedInstanceState)
	{
		infoTxt = (TextView) scanningContactsLayout.findViewById(R.id.txt_img1);
		loadingText = (TextView) scanningContactsLayout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) scanningContactsLayout.findViewById(R.id.loading_layout);
		nextBtnContainer.setVisibility(View.GONE);
		setupActionBarTitle();
	}
	
	private void prepareLayoutForBackupFound(Bundle savedInstanceState)
	{
		nextBtnContainer.setVisibility(View.VISIBLE);
		arrow.setVisibility(View.GONE);
		postText.setText(R.string.skip);
		setupActionBarTitle();
		Button btnRestore = (Button) backupFoundLayout.findViewById(R.id.btn_restore);
		btnRestore.setOnClickListener(btnRestoreClick);
		if (savedInstanceState == null)
		{
			preRestoreAnimation();
		}
	}

	private void prepareLayoutForRestoringAnimation(Bundle savedInstanceState, StateValue stateValue)
	{
		nextBtnContainer.setVisibility(View.GONE);
		setupActionBarTitle();
		String restoreStatus = null;
		if (savedInstanceState != null && savedInstanceState.containsKey(HikeConstants.Extras.RESTORE_STATUS))
		{
			restoreStatus = savedInstanceState.getString(HikeConstants.Extras.RESTORE_STATUS);
		}
		else if (stateValue != null)
		{
			restoreStatus = stateValue.value;
		}
		mActivityState.restoreStatus = restoreStatus;
		
		if (TextUtils.isEmpty(restoreStatus))
		{
			TextView title = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_title);
			TextView hint = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_hint);
			title.setText(R.string.restoring___);
			hint.setText(R.string.restoring____hint);
			if (savedInstanceState == null && stateValue == null)
			{
				onRestoreAnimation();
			}
			else
			{
				setupOnRestoreProgress();
			}
		}
		else if (Boolean.TRUE.toString().equals(restoreStatus))
		{
			TextView title = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_title);
			TextView hint = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_hint);
			title.setText(R.string.restored);
			hint.setText(R.string.restored_hint);
			View restoreItems = (View) restoringBackupLayout.findViewById(R.id.restore_items);
			ImageView restoreSuccess = (ImageView) restoringBackupLayout.findViewById(R.id.restore_success);
			restoreItems.setVisibility(View.INVISIBLE);
			restoreSuccess.setVisibility(View.VISIBLE);
			if (savedInstanceState == null)
			{
				onRestoreSuccessAnimation();
			}
		}
		else
		{
			TextView title = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_title);
			TextView hint = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_hint);
			title.setText(R.string.restore_error);

			String errorMessage = getString(R.string.restore_error_hint);
			// In case of generic restore failure, we send "false" as the state value, else we send the required error to be displayed
			if (!TextUtils.isEmpty(restoreStatus) && !(Boolean.FALSE.toString().equals(restoreStatus)))
			{
				errorMessage = restoreStatus;
			}

			hint.setText(errorMessage);
			nextBtnContainer.setVisibility(View.VISIBLE);
			arrow.setVisibility(View.GONE);
			postText.setText(R.string.skip);
			final View restoreProgress = (View) restoringBackupLayout.findViewById(R.id.restore_progress);
			final ImageView restoreFail = (ImageView) restoringBackupLayout.findViewById(R.id.restore_fail);
			final Button retry  = (Button) restoringBackupLayout.findViewById(R.id.btn_retry);

			TextView restoreTitleTv = (TextView) restoringBackupLayout.findViewById(R.id.txt_restore_title);
			restoreTitleTv.setText(getString(R.string.restore_error));

			if (restoreStatus.equals(Boolean.FALSE.toString()) || restoreStatus.equals(getString(R.string.restore_msisdn_error)))
			// If Restore failed due to generic reasons or if msisdn was different, show a retry button to give the user one more chance
			{
				retry.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						JSONObject metadata = new JSONObject();
						try
						{
							metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BACKUP_RESTORE_RETRY);
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
						}
						catch (JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}
						nextBtnContainer.setVisibility(View.GONE);
						restoreProgress.setVisibility(View.VISIBLE);
						restoreFail.setVisibility(View.INVISIBLE);
						retry.setVisibility(View.INVISIBLE);
						setupOnRestoreProgress();
						mTask.addUserInput("true");
					}
				});

				retry.setText(getString(R.string.retry));

				if (restoreStatus.equals(getString(R.string.restore_msisdn_error))) //If msisdn mismatch, set the title as Bummer!
				{
					restoreTitleTv.setText(getString(R.string.restore_failed));
				}
			}

			else if (restoreStatus.equals(getString(R.string.restore_version_error))) // If Restore failed due to version reasons
			{
				retry.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						// Open PlayStore
						IntentFactory.launchPlayStore(SignupActivity.this.getPackageName(), SignupActivity.this);
					}
				});

				retry.setText(getString(R.string.upgrade_hike));
				restoreTitleTv.setText(getString(R.string.restore_failed));
			}

			restoreProgress.setVisibility(View.INVISIBLE);
			restoreFail.setVisibility(View.VISIBLE);
			if (savedInstanceState == null)
			{
				onRestoreFailAnimation();
			}

			retry.setVisibility(View.VISIBLE);
		}
	}

	private void prepareLayoutForPostSignup(Bundle savedInstanceState)
	{
		nextBtnContainer.setVisibility(View.GONE);
		setupActionBarTitle();
	}
	
	private void preRestoreAnimation()
	{
		long smileyOffset = 800;
		long smileyDuration = 300;
		long convOffset = smileyOffset + smileyDuration;
		long convDuration = smileyDuration;
		long profileOffset = convOffset + convDuration;
		long profileDuration = smileyDuration;
		long restoreOffset = profileOffset + profileDuration;
		long restoreDuration = 200;
		
		ImageView artProfile = (ImageView) backupFoundLayout.findViewById(R.id.art_profile);
		ImageView artConv = (ImageView) backupFoundLayout.findViewById(R.id.art_conversation);
		ImageView artSmiley = (ImageView) backupFoundLayout.findViewById(R.id.art_smiley);
		Button btnRestore = (Button) backupFoundLayout.findViewById(R.id.btn_restore);
		
		btnRestore.setOnClickListener(btnRestoreClick);
		
		// Animation Setup for smiley image
		ScaleAnimation smileyScaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f);
		smileyScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation smileyAlphaAnimation = new AlphaAnimation(0.1f, 1.0f);
		
		AnimationSet smileyAnimSet = new AnimationSet(false);
		smileyAnimSet.addAnimation(smileyScaleAnimation);
		smileyAnimSet.addAnimation(smileyAlphaAnimation);
		smileyAnimSet.setDuration(smileyDuration);
		smileyAnimSet.setFillAfter(true);
		smileyAnimSet.setStartOffset(smileyOffset);
		
		artSmiley.setVisibility(View.INVISIBLE);
		artSmiley.startAnimation(smileyAnimSet);
		
		// Animation setup for conv image
		ScaleAnimation convScaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 1.8f);
		convScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation convAlphaAnimation = new AlphaAnimation(0.1f, 1.0f);
		
		AnimationSet convAnimSet = new AnimationSet(false);
		convAnimSet.addAnimation(convScaleAnimation);
		convAnimSet.addAnimation(convAlphaAnimation);
		convAnimSet.setDuration(convDuration);
		convAnimSet.setFillAfter(true);
		convAnimSet.setStartOffset(convOffset);
		
		artConv.setVisibility(View.INVISIBLE);
		artConv.startAnimation(convAnimSet);

		// Animation setup for profile image
		ScaleAnimation profileScaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 2.2f);
		profileScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation profileAlphaAnimation = new AlphaAnimation(0.1f, 1.0f);
		
		AnimationSet profileAnimSet = new AnimationSet(false);
		profileAnimSet.addAnimation(profileScaleAnimation);
		profileAnimSet.addAnimation(profileAlphaAnimation);
		profileAnimSet.setDuration(profileDuration);
		profileAnimSet.setFillAfter(true);
		profileAnimSet.setStartOffset(profileOffset);
		
		artProfile.setVisibility(View.INVISIBLE);
		artProfile.startAnimation(profileAnimSet);
		
		// Starting up the dots animation
		setupDotsAnimation(smileyOffset,smileyDuration,0);
		
		// Animation setup for restore and skip buttons
		AlphaAnimation restoreAlphaAnimation = new AlphaAnimation(0.0f, 1.0f);
		restoreAlphaAnimation.setDuration(restoreDuration);
		restoreAlphaAnimation.setFillAfter(true);
		restoreAlphaAnimation.setStartOffset(restoreOffset);
		
		btnRestore.setVisibility(View.INVISIBLE);
		btnRestore.startAnimation(restoreAlphaAnimation);
	}

	private void finishPreRestoreAnimation()
	{
		long smileyOffset = 300;
		long smileyDuration = 300;
		long convOffset = smileyOffset + smileyDuration;
		long convDuration = smileyDuration;
		long profileOffset = convOffset + convDuration;
		long profileDuration = smileyDuration;
		long restoreOffset = profileOffset + profileDuration;
		long restoreDuration = 200;
		
		ImageView artProfile = (ImageView) backupFoundLayout.findViewById(R.id.art_profile);
		ImageView artConv = (ImageView) backupFoundLayout.findViewById(R.id.art_conversation);
		ImageView artSmiley = (ImageView) backupFoundLayout.findViewById(R.id.art_smiley);
		Button btnRestore = (Button) backupFoundLayout.findViewById(R.id.btn_restore);
		TextView textBackup = (TextView) backupFoundLayout.findViewById(R.id.txt_backup_title);
		TextView textView = (TextView) backupFoundLayout.findViewById(R.id.txt_backup_hint);
		
		btnRestore.setClickable(false);
		
		// Animation Setup for smiley image
		ScaleAnimation smileyScaleAnimation = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f);
		smileyScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation smileyAlphaAnimation = new AlphaAnimation(1.0f, 0.1f);
		
		AnimationSet smileyAnimSet = new AnimationSet(false);
		smileyAnimSet.addAnimation(smileyScaleAnimation);
		smileyAnimSet.addAnimation(smileyAlphaAnimation);
		smileyAnimSet.setDuration(smileyDuration);
		smileyAnimSet.setFillAfter(true);
		smileyAnimSet.setStartOffset(smileyOffset);
		
		artSmiley.setVisibility(View.INVISIBLE);
		artSmiley.startAnimation(smileyAnimSet);
		
		// Animation setup for conv image
		ScaleAnimation convScaleAnimation = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 1.8f);
		convScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation convAlphaAnimation = new AlphaAnimation(1.0f, 0.1f);
		
		AnimationSet convAnimSet = new AnimationSet(false);
		convAnimSet.addAnimation(convScaleAnimation);
		convAnimSet.addAnimation(convAlphaAnimation);
		convAnimSet.setDuration(convDuration);
		convAnimSet.setFillAfter(true);
		convAnimSet.setStartOffset(convOffset);
		
		artConv.setVisibility(View.INVISIBLE);
		artConv.startAnimation(convAnimSet);

		// Animation setup for profile image
		ScaleAnimation profileScaleAnimation = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 2.2f);
		profileScaleAnimation.setInterpolator(new OvershootInterpolator());
		
		AlphaAnimation profileAlphaAnimation = new AlphaAnimation(1.0f, 0.1f);
		
		AnimationSet profileAnimSet = new AnimationSet(false);
		profileAnimSet.addAnimation(profileScaleAnimation);
		profileAnimSet.addAnimation(profileAlphaAnimation);
		profileAnimSet.setDuration(profileDuration);
		profileAnimSet.setFillAfter(true);
		profileAnimSet.setStartOffset(profileOffset);
		
		artProfile.setVisibility(View.INVISIBLE);
		artProfile.startAnimation(profileAnimSet);

		// Starting up the dots animation
		setupDotsAnimation(smileyOffset,smileyDuration,smileyDuration);

		// Animation setup for restore and skip buttons
		AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
		fadeOutAnimation.setDuration(restoreDuration);
		fadeOutAnimation.setFillAfter(true);
		fadeOutAnimation.setStartOffset(restoreOffset);
		
		fadeOutAnimation.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			@Override
			public void onAnimationEnd(Animation animation)
			{
				viewFlipper.setDisplayedChild(RESTORING_BACKUP);
				restoreInitialized = false;
				prepareLayoutForRestoringAnimation(null,null);
			}
		});
		
		btnRestore.setVisibility(View.INVISIBLE);
		btnRestore.startAnimation(fadeOutAnimation);
		textBackup.setVisibility(View.INVISIBLE);
		textBackup.startAnimation(fadeOutAnimation);
		textView.setVisibility(View.INVISIBLE);
		textView.startAnimation(fadeOutAnimation);
	}
	
	private void setupDotsAnimation(long smileyOffset, long smileyDuration, long onsetTime)
	{
		long animationDuation = 50;
		
		final View dot0 = (View) backupFoundLayout.findViewById(R.id.dot_right);
		final View dot1 = (View) backupFoundLayout.findViewById(R.id.dot_left);
		final View dot2 = (View) backupFoundLayout.findViewById(R.id.dot_top);
		
		ShapeDrawable circle0 = new ShapeDrawable(new OvalShape());
		circle0.getPaint().setColor(this.getResources().getColor(R.color.restoring_red));
		dot0.setBackgroundDrawable(circle0);
		ShapeDrawable circle1 = new ShapeDrawable(new OvalShape());
		circle1.getPaint().setColor(this.getResources().getColor(R.color.restoring_orange));
		dot1.setBackgroundDrawable(circle1);
		ShapeDrawable circle2 = new ShapeDrawable(new OvalShape());
		circle2.getPaint().setColor(this.getResources().getColor(R.color.restoring_green));
		dot2.setBackgroundDrawable(circle2);
		
		AlphaAnimation dotIn = new AlphaAnimation(0, 1);
		dotIn.setDuration(animationDuation);
		AlphaAnimation dotOut = new AlphaAnimation(1, 0);
		dotOut.setDuration(animationDuation);
		dotOut.setStartOffset(animationDuation);
		AnimationSet dot0Anim = new AnimationSet(true);
		dot0Anim.addAnimation(dotIn);
		dot0Anim.addAnimation(dotOut);
		dot0Anim.setStartOffset(smileyOffset + smileyDuration - 50 - onsetTime);
		dot0.startAnimation(dot0Anim);
	
		AlphaAnimation dot1In = new AlphaAnimation(0, 1);
		dot1In.setDuration(animationDuation);
		AlphaAnimation dot1Out = new AlphaAnimation(1, 0);
		dot1Out.setDuration(animationDuation);
		dot1Out.setStartOffset(animationDuation);
		AnimationSet dot1Anim = new AnimationSet(true);
		dot1Anim.addAnimation(dot1In);
		dot1Anim.addAnimation(dot1Out);
		dot1Anim.setStartOffset(smileyOffset + 2*smileyDuration - 50 - onsetTime);
		dot1.startAnimation(dot1Anim);

		
		AlphaAnimation dot2In = new AlphaAnimation(0, 1);
		dot2In.setDuration(animationDuation);
		AlphaAnimation dot2Out = new AlphaAnimation(1, 0);
		dot2Out.setDuration(animationDuation);
		dot2Out.setStartOffset(animationDuation);
		AnimationSet dot2Anim = new AnimationSet(true);
		dot2Anim.addAnimation(dot2In);
		dot2Anim.addAnimation(dot2Out);
		dot2Anim.setStartOffset(smileyOffset + 3*smileyDuration - 50 - onsetTime);
		dot2.startAnimation(dot2Anim);
	}

	private void initializeRestore()
	{
		restoreInitialized = true;
		sdCardProp = new ViewProperties();
		ImageView sdCard = (ImageView) backupFoundLayout.findViewById(R.id.sd_card);
		int [] screenLocation = new int[2];
		sdCard.getLocationOnScreen(screenLocation);
		sdCardProp.left = screenLocation[0];
		sdCardProp.top = screenLocation[1];
		sdCardProp.width = sdCard.getWidth();
		sdCardProp.height = sdCard.getHeight();
		finishPreRestoreAnimation();
	}
	private void onRestoreAnimation()
	{
		final ImageView sdCard = (ImageView) restoringBackupLayout.findViewById(R.id.sd_card);
		final ImageView profilePic = (ImageView) restoringBackupLayout.findViewById(R.id.profile_pic);
		final View restoreProgress = (View) restoringBackupLayout.findViewById(R.id.restore_progress);
		profilePic.setVisibility(View.INVISIBLE);
		restoreProgress.setVisibility(View.INVISIBLE);
		
		sdCard.post(new Runnable() {
			@Override
			public void run() {
				final int[] screenLocation = new int[2];
				sdCard.getLocationOnScreen(screenLocation);
				float widthScale = (float) sdCardProp.width / sdCard.getWidth();
				float heightScale = (float) sdCardProp.height / sdCard.getHeight();

				TranslateAnimation sdCardTranslateAnimation = new TranslateAnimation(Animation.ABSOLUTE, (sdCardProp.left - screenLocation[0]) / widthScale, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, (sdCardProp.top - screenLocation[1]) / heightScale, Animation.RELATIVE_TO_SELF, 0);
				ScaleAnimation sdCardScaleAnimation = new ScaleAnimation(widthScale, 1, heightScale, 1);
				AnimationSet sdCardAnimationSet = new AnimationSet(true);

				sdCardAnimationSet.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						AlphaAnimation ppAnimation = new AlphaAnimation(0, 1);
						ppAnimation.setDuration(200);
						ppAnimation.setFillAfter(true);
						profilePic.startAnimation(ppAnimation);

						restoreProgress.setVisibility(View.VISIBLE);
						setupOnRestoreProgress();
					}
				});
				sdCardAnimationSet.addAnimation(sdCardTranslateAnimation);
				sdCardAnimationSet.addAnimation(sdCardScaleAnimation);
				sdCardAnimationSet.setDuration(500);
				sdCardAnimationSet.setFillAfter(true);
				sdCardAnimationSet.setInterpolator(new DecelerateInterpolator());
				sdCard.startAnimation(sdCardAnimationSet);
			}
		});
	}
	
	private void onRestoreSuccessAnimation()
	{
		long uiDelay = HikeConstants.BACKUP_RESTORE_UI_DELAY;
		uiDelay = 0;
		View restoreItems = (View) restoringBackupLayout.findViewById(R.id.restore_items);
		ImageView restoreSuccess = (ImageView) restoringBackupLayout.findViewById(R.id.restore_success);
		
		AlphaAnimation fadeout = new AlphaAnimation(1, 0);
		ScaleAnimation scaleDown = new ScaleAnimation(1, 0, 1, 0.4f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.3f);
		AnimationSet itemsRemove = new AnimationSet(true);
		itemsRemove.addAnimation(fadeout);
		itemsRemove.addAnimation(scaleDown);
		itemsRemove.setInterpolator(new DecelerateInterpolator());
		itemsRemove.setStartOffset(uiDelay);
		itemsRemove.setDuration(400);
		itemsRemove.setFillAfter(true);
		
		ScaleAnimation scaleUp = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		scaleUp.setInterpolator(new OvershootInterpolator());
		scaleUp.setStartOffset(200 + uiDelay);
		scaleUp.setDuration(400);
		scaleUp.setFillAfter(true);
		
		restoreItems.startAnimation(itemsRemove);
		restoreSuccess.startAnimation(scaleUp);
	}
	
	private void onRestoreFailAnimation()
	{
		final ImageView restoreFail = (ImageView) restoringBackupLayout.findViewById(R.id.restore_fail);

		ScaleAnimation scaleUp = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		scaleUp.setInterpolator(new OvershootInterpolator());
		scaleUp.setStartOffset(100);
		scaleUp.setDuration(300);
		
		AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator());
		fadeIn.setStartOffset(100);
		fadeIn.setDuration(200);
		
		restoreFail.startAnimation(scaleUp);
		restoreFail.setVisibility(View.VISIBLE);
	}
	
	private void setupOnRestoreProgress()
	{
		final View dot0 = (View) restoringBackupLayout.findViewById(R.id.dot_left);
		final View dot1 = (View) restoringBackupLayout.findViewById(R.id.dot_center);
		final View dot2 = (View) restoringBackupLayout.findViewById(R.id.dot_right);
		
		ShapeDrawable circle0 = new ShapeDrawable(new OvalShape());
		circle0.getPaint().setColor(this.getResources().getColor(R.color.restoring_red));
		dot0.setBackgroundDrawable(circle0);
		ShapeDrawable circle1 = new ShapeDrawable(new OvalShape());
		circle1.getPaint().setColor(this.getResources().getColor(R.color.restoring_green));
		dot1.setBackgroundDrawable(circle1);
		ShapeDrawable circle2 = new ShapeDrawable(new OvalShape());
		circle2.getPaint().setColor(this.getResources().getColor(R.color.restoring_orange));
		dot2.setBackgroundDrawable(circle2);
		
		AlphaAnimation dotIn0 = new AlphaAnimation(0, 1);
		dotIn0.setDuration(100);
		AlphaAnimation dotOut0 = new AlphaAnimation(1, 0);
		dotOut0.setDuration(100);
		dotOut0.setStartOffset(200);
		RotateAnimation dotStay0 = new RotateAnimation(0, 360);
		dotStay0.setDuration(400);
		dotStay0.setStartOffset(300);
		
		final AnimationSet dota0 = new AnimationSet(true);
		dota0.addAnimation(dotIn0);
		dota0.addAnimation(dotOut0);
		dota0.addAnimation(dotStay0);
		dota0.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(400 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setAnimationListener(this);
				dot0.startAnimation(dot);
			}
		});

		AlphaAnimation dotIn1 = new AlphaAnimation(0, 1);
		dotIn1.setDuration(100);
		AlphaAnimation dotOut1 = new AlphaAnimation(1, 0);
		dotOut1.setDuration(100);
		dotOut1.setStartOffset(200);
		RotateAnimation dotStay1 = new RotateAnimation(0, 360);
		dotStay1.setDuration(200);
		dotStay1.setStartOffset(300);
		
		final AnimationSet dota1 = new AnimationSet(true);
		dota1.addAnimation(dotIn1);
		dota1.addAnimation(dotOut1);
		dota1.addAnimation(dotStay1);
		dota1.setStartOffset(200);
		dota1.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(200 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setStartOffset(200);
				dot.setAnimationListener(this);
				dot1.startAnimation(dot);
			}
		});
		
		AlphaAnimation dotIn2 = new AlphaAnimation(0, 1);
		dotIn2.setDuration(100);
		AlphaAnimation dotOut2 = new AlphaAnimation(1, 0);
		dotOut2.setDuration(100);
		dotOut2.setStartOffset(200);
		
		final AnimationSet dota2 = new AnimationSet(true);


		dota2.addAnimation(dotIn2);
		dota2.addAnimation(dotOut2);
		dota2.setStartOffset(400);
		dota2.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(0 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setAnimationListener(this);
				dot.setStartOffset(400);
				dot2.startAnimation(dot);
			}
		});
		
		dot0.startAnimation(dota0);
		dot1.startAnimation(dota1);
		dot2.startAnimation(dota2);
	}
	
	private OnClickListener btnRestoreClick = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BACKUP_RESTORE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			mTask.addUserInput("true");
		}
	};

	public void onGenderClick(View v)
	{
		if (v.getId() == R.id.female)
		{
			selectGender(true);
		}
		else
		{
			selectGender(false);
		}
	}

	private void selectGender(Boolean isFemale)
	{
		femaleText.setSelected(isFemale);
		maleText.setSelected(!isFemale);
		mActivityState.isFemale = isFemale;
	}

	private void resetViewFlipper()
	{
		errorDialog = null;
		toggleActionBarElementsEnable(true);
		viewFlipper.setVisibility(View.VISIBLE);
	}

	private void restartTask()
	{
		resetViewFlipper();
		mTask = SignupTask.restartTask(this);
	}

	private void restartTask(String userName, Boolean isFemale, Birthday birthday)
	{
		resetViewFlipper();
		mTask = SignupTask.restartTask(this, userName, isFemale, birthday, mActivityState.profileBitmap);
	}

	private void showErrorMsg()
	{
		toggleActionBarElementsEnable(false);
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		if (infoTxt != null)
		{
			infoTxt.setVisibility(View.VISIBLE);
		}
		if (errorDialog == null)
		{
			showNetworkErrorPopup();
		}
	}

	private void showNetworkErrorPopup()
	{
		errorDialog = new Dialog(this, R.style.Theme_CustomDialog);
		errorDialog.setContentView(R.layout.no_internet_pop_up);
		errorDialog.setCancelable(true);
		Button btnOk = (Button) errorDialog.findViewById(R.id.btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (errorDialog != null) {
					errorDialog.dismiss();
					v.setEnabled(false);
					if (countryPicker != null) {
						countryPicker.setEnabled(true);
					}
					if (selectedCountryPicker != null) {
						selectedCountryPicker.setEnabled(true);
					}
					if (enterEditText != null) {
						enterEditText.setEnabled(true);
					}
					if (viewFlipper.getDisplayedChild() != SCANNING_CONTACTS) {
						/*
						 * Delaying this by 100 ms to allow the signup task to setup to the last input point.
						 */
						SignupActivity.this.mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								Logger.d("tesst", "submit clicked");
								submitClicked();
							}
						}, 100);
					}
					restartTask(mActivityState.userName, mActivityState.isFemale, mActivityState.birthday);

				}
			}
		});

		errorDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				endLoading();

			}
		});
		if (!SignupActivity.this.isFinishing())
		{
			errorDialog.show();
		}
	}

	private void setListeners()
	{
		if (enterEditText != null)
		{
			enterEditText.setOnEditorActionListener(this);
			enterEditText.setOnKeyListener(new OnKeyListener()
			{
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					return loadingLayout != null && loadingLayout.getVisibility() == View.VISIBLE && (event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK);
				}
			});
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		int displayedChild = viewFlipper.getDisplayedChild();
		if (restoreInitialized)
		{
			if (displayedChild == BACKUP_FOUND || displayedChild == RESTORING_BACKUP)
				displayedChild = RESTORING_BACKUP;
		}
		outState.putInt(HikeConstants.Extras.SIGNUP_PART, displayedChild);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout != null && loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, errorDialog != null);
		if (enterEditText != null)
		{
			outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText.getText().toString());
		}
		outState.putBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR, msisdnErrorDuringSignup);
		outState.putBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT, showingSecondLoadingTxt);
		outState.putBoolean(HikeConstants.Extras.SHOWING_INVALID_PIN_ERROR, invalidPin != null && invalidPin.getVisibility() == View.VISIBLE);
		if (viewFlipper.getDisplayedChild() == NUMBER)
		{
			outState.putString(HikeConstants.Extras.COUNTRY_CODE, countryPicker.getText().toString());
		}
		if (viewFlipper.getDisplayedChild() == GENDER && mActivityState.isFemale != null)
		{
			outState.putBoolean(HikeConstants.Extras.GENDER, mActivityState.isFemale);
		}
		outState.putString(HikeConstants.Extras.RESTORE_STATUS, mActivityState.restoreStatus);
		outState.putParcelable(HikeConstants.Extras.BITMAP, mActivityState.profileBitmap);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		SharedPreferences settings = getApplicationContext().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (viewFlipper.getDisplayedChild() == BACKUP_FOUND || viewFlipper.getDisplayedChild() == RESTORING_BACKUP)
		{
			return;
		}
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			if (countDownTimer != null)
			{
				countDownTimer.cancel();
			}
			mTask.addUserInput("");
			return;
		}
		if (mTask != null)
		{
			mTask.cancelTask();
		}
		SignupTask.isAlreadyFetchingNumber = false;
		
		super.onBackPressed();
	}

	private void removeAnimation()
	{
		viewFlipper.setInAnimation(null);
		viewFlipper.setOutAnimation(null);
	}

	private void setAnimation()
	{
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_animation));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out_animation));
	}

	public int getDisplayItem()
	{
		return viewFlipper.getDisplayedChild();
	}

	@Override
	public void onProgressUpdate(StateValue stateValue)
	{
		/*
		 * Making sure the countdown timer doesn't keep running when the state values changes.
		 */
		if (countDownTimer != null && stateValue.state != State.PIN)
		{
			countDownTimer.cancel();
		}
		String value = stateValue.value;
		mCurrentState = stateValue;
		Logger.d("SignupActivity", "Current State " + mCurrentState.state.name() + " VALUE: " + value);

		showingSecondLoadingTxt = false;
		switch (stateValue.state)
		{
		case MSISDN:
			if (TextUtils.isEmpty(value))
			{
				viewFlipper.setDisplayedChild(NUMBER);
				prepareLayoutForFetchingNumber();
			}
			else
			{
				if (Utils.getExternalStorageState() == ExternalStorageState.WRITEABLE)
				{
					// we should delete old profile image of the returning user
					String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
					Utils.removeLargerProfileImageForMsisdn(msisdn);
				}
			}
			break;
		case PULLING_PIN:
			break;
		case PIN:
			viewFlipper.setDisplayedChild(PIN);
			// Wrong Pin
			if (value != null && value.equals(HikeConstants.PIN_ERROR))
			{
				if (countDownTimer != null)
				{
					countDownTimer.cancel();
				}
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				invalidPin.setVisibility(View.VISIBLE);
				loadingLayout.setVisibility(View.GONE);
				callmeBtn.setVisibility(View.VISIBLE);
				callmeBtn.setEnabled(true);
				toggleActionBarElementsEnable(true);
				enterEditText.setText("");
			}
			// Manual entry for pin
			else
			{
				prepareLayoutForGettingPin(callMeWaitTime);
				setAnimation();
			}
			break;
		case PIN_VERIFIED:
			if (verifiedPin != null)
			{
				verifiedPin.setVisibility(View.VISIBLE);
				loadingLayout.setVisibility(View.GONE);
				toggleActionBarElementsEnable(false);
				/*
				 * after verifying pin we would wait for 2 second to get user to the next screen and show him/her that pin is verified
				 */
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						if (mTask != null)
						{
							mTask.addUserInput("");
						}
					}
				}, 1500);
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value) && viewFlipper.getDisplayedChild() != NAME)
			{
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null, true);
			}
			break;
		case GENDER:
			if (TextUtils.isEmpty(value) && viewFlipper.getDisplayedChild() != GENDER)
			{
				viewFlipper.setDisplayedChild(GENDER);
				prepareLayoutForGender(null);
			}
			break;
		case SCANNING_CONTACTS:
			if (TextUtils.isEmpty(value) && viewFlipper.getDisplayedChild() != SCANNING_CONTACTS)
			{
				viewFlipper.setDisplayedChild(SCANNING_CONTACTS);
				prepareLayoutForScanning(null);
			}
			break;
		case PROFILE_IMAGE:
			if (SignupTask.START_UPLOAD_PROFILE.equals(value))
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						if (loadingText != null)
						{
							loadingText.setText(R.string.setting_profile);
						}
					}
				}, 500);
			}
			else if (SignupTask.FINISHED_UPLOAD_PROFILE.equals(value))
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						if (loadingText != null)
						{
							loadingText.setText(R.string.you_are_all_set);
						}
					}
				}, 1000);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR))
			{
				addressBookError = true;
				if (viewFlipper.getDisplayedChild() == SCANNING_CONTACTS)
				{
					showErrorMsg();
				}
			}
			else if (value == null || !value.equals(HikeConstants.CHANGE_NUMBER))
			{
				showErrorMsg();
			}
			break;
		case BACKUP_AVAILABLE:
			if (viewFlipper.getDisplayedChild() != BACKUP_FOUND)
			{
				viewFlipper.setDisplayedChild(BACKUP_FOUND);
				prepareLayoutForBackupFound(null);
			}
			setWindowSoftInputState();
			break;
		case RESTORING_BACKUP:
			if (viewFlipper.getDisplayedChild() == BACKUP_FOUND)
			{
				initializeRestore();
			}
			else
			{
				if (viewFlipper.getDisplayedChild() != RESTORING_BACKUP)
				{
					viewFlipper.setDisplayedChild(RESTORING_BACKUP);
				}
				prepareLayoutForRestoringAnimation(null, stateValue);
			}
			break;

		case RESTORING_CLOUD_SETTINGS:
			// Fetch user settings from server
			HikeCloudSettingsManager.getInstance().doRestoreSkipEnableCheck(null);
			break;

		case SHOW_STICKER_RESTORE_DIALOG:
			showStickerRestoreDiffDpiDialog = true;
			break;
		}
		setListeners();
	}

	@Override
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event)
	{
		if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& !TextUtils.isEmpty(enterEditText.getText().toString().trim()) && (loadingLayout == null || loadingLayout.getVisibility() != View.VISIBLE))
		{
			if (viewFlipper.getDisplayedChild() == NAME)
			{
				if (enterEditText.isFocused())
				{
					birthdayText.requestFocus();
				}
				else
				{
					Utils.hideSoftKeyboard(this, enterEditText);
				}
			}
			else
			{
				submitClicked();
				Utils.hideSoftKeyboard(this, enterEditText);
			}
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		Logger.d(getClass().getSimpleName(), "Dialog cancelled");
		if (mActivityState.pinCallRequestToken != null)
		{
			mActivityState.pinCallRequestToken.cancel();
			mActivityState = new ActivityState();
		}
	}

	private TextWatcher nameWatcher = new TextWatcher()
	{
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		public void onTextChanged(CharSequence s, int start, int before, int count)
		{

		}

		public void afterTextChanged(Editable s)
		{
			String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
			if (s == null || mActivityState.profileBitmap != null || getCachedProfilePic() != null)
			{
				return;
			}
			setDefaultProfileImage(msisdn);
		}

	};

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	public interface ImageDownloadResult
	{
		public void downloadFinished(boolean result);
	}

	@Override
	protected String getNewProfileImagePath(boolean toUseTimestamp)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);

		if (!dir.exists())
		{
			dir.mkdirs();
		}

		String fileName = Utils.getTempProfileImageFileName(accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));
		String destFilePath = directory + File.separator + fileName;
		return destFilePath;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		switch (requestCode)
		{

		case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
			mActivityState.destFilePath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);
			if (mActivityState.destFilePath == null)
			{
				Toast.makeText(getApplicationContext(), R.string.error_setting_profile, Toast.LENGTH_SHORT).show();
				return;
			}

			setProfileImage();
			break;
		case HikeConstants.ResultCodes.SELECT_COUNTRY:
			if (resultCode == RESULT_OK)
			{
				String countryName = data.getStringExtra(HikeConstants.Extras.SELECTED_COUNTRY);
				selectCountry(countryName);
			}
			break;
		}
	}

	private void setProfileImage()
	{
		if (mIconView == null)
		{
			return;
		}
		if (!new File(mActivityState.destFilePath).exists())
		{
			Toast.makeText(getApplicationContext(), R.string.image_failed, Toast.LENGTH_SHORT).show();
			return;
		}

		Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(mActivityState.destFilePath, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS,
				HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, Bitmap.Config.RGB_565, true, false);

		mActivityState.profileBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);
		
		if (mActivityState.profileBitmap == null)
		{
			Toast.makeText(getApplicationContext(), R.string.image_failed, Toast.LENGTH_SHORT).show();
			return;
		}
		
		mIconView.setImageBitmap(mActivityState.profileBitmap);
		mIconView.setBackgroundResource(R.color.transparent);
		profilePicCamIcon.setImageResource(R.drawable.ic_edit_group);

		tempBitmap = null;
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.FACEBOOK_IMAGE_DOWNLOADED.equals(type))
		{
			final boolean result = (Boolean) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (dialog != null)
					{
						dialog.dismiss();
						dialog = null;
					}
					if (!result)
					{
						return;
					}
					if (mActivityState.destFilePath == null)
					{
						return;
					}
					setProfileImage();

					enterEditText.setText(mActivityState.userName);

					/*
					 * Saw a few crashes here. Catching the exception since we don't seem to be doing anything wrong here.
					 */
					try
					{
						enterEditText.setSelection(mActivityState.userName.length());
					}
					catch (IndexOutOfBoundsException e)
					{
						Logger.w(getClass().getSimpleName(), "IOOB thrown while setting the name's textbox selection");
					}

				}
			});
		}
		else if (HikePubSub.CLOUD_SETTINGS_RESTORE_SUCCESS.equals(type) || HikePubSub.CLOUD_SETTINGS_RESTORE_FAILED.equals(type))
		{
			// We are OK to continue even if cloud settings restore failed. User wont be getting his/her pre-set settings.
			mTask.addUserInput("");
		}
	}

	private boolean selectCountry(String countryName)
	{
		int index = countriesArray.indexOf(countryName);
		if (index != -1)
		{
			countryCode = countriesMap.get(countryName);
			countryPicker.setText(countryCode);
			selectedCountryName.setText(countryName);
		}
		return !TextUtils.isEmpty(countryCode);
	}

	private boolean isInvalidCountryCode()
	{
		String countryName = codesMap.get(countryPicker.getText().toString());
		return !(countryName != null && countriesArray.indexOf(countryName) != -1);
	}

	public void autoFillPin(String pin)
	{
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			enterEditText.setText(pin);
			submitClicked();
		}
	}
	
	public void toggleActionBarElementsEnable( boolean enabled)
	{
		nextBtn.setEnabled(enabled);
		arrow.setEnabled(enabled);
		postText.setEnabled(enabled);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// TODO Auto-generated method stub
		return true;
	}

	private void showStickerRestoreDialog()
	{
		HikeDialogFactory.showDialog(SignupActivity.this,
				HikeDialogFactory.STICKER_RESTORE_DIFF_DPI_DIALOG, new HikeDialogListener()
				{
					@Override
					public void negativeClicked(HikeDialog hikeDialog)
					{

					}

					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						hikeDialog.dismiss();
						openHomeActivity();
					}

					@Override
					public void neutralClicked(HikeDialog hikeDialog)
					{

					}
				}, null);
	}

	private void openHomeActivity()
	{
		final LocalLanguage localLanguage = LocalLanguageUtils.getApplicationLocalLanguage(getApplicationContext());
		for (LocalLanguage language : localLanguage.getDeviceSupportedHikeLanguages(getApplicationContext()))
		{
			if (language.getDisplayName().equalsIgnoreCase(localLanguage.getDisplayName()))
			{
				LocalLanguageUtils.setApplicationLocalLanguage(language, HikeConstants.APP_LANG_CHANGED_SETTINGS);
				break;
			}
		}
		Intent i = new Intent(SignupActivity.this, HomeActivity.class);
		i.putExtra(HikeConstants.Extras.NEW_USER, true);
		startActivity(i);
		finish();
	}

}
