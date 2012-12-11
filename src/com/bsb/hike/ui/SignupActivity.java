package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.Utils;
import com.fiksu.asotracking.FiksuTrackingManager;

public class SignupActivity extends Activity implements
		SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener,
		OnClickListener, FinishableEvent, OnCancelListener {

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;
	private ViewGroup numLayout;
	private ViewGroup pinLayout;
	private ViewGroup nameLayout;
	private ViewGroup booBooLayout;

	private TextView infoTxt;
	private TextView loadingText;
	private ViewGroup loadingLayout;
	private EditText enterEditText;
	private Button tapHereText;
	private Button submitBtn;
	private TextView invalidNum;
	private ImageView errorImage;
	private Button countryPicker;
	private Button callmeBtn;

	private Button tryAgainBtn;
	private Handler mHandler;

	private boolean addressBookError = false;
	private boolean msisdnErrorDuringSignup = false;

	private final int NAME = 2;
	private final int PIN = 1;
	private final int NUMBER = 0;

	private String[] countryNamesAndCodes;

	private String[] countryISOAndCodes;

	private String countryCode;

	private final String defaultCountryCode = "IN +91";

	private ProgressDialog dialog;

	private HikeHTTPTask hikeHTTPTask;

	private boolean showingSecondLoadingTxt = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.signup);

		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);
		errorImage = (ImageView) findViewById(R.id.error_img);

		Object o = getLastNonConfigurationInstance();
		if (o instanceof HikeHTTPTask) {
			hikeHTTPTask = (HikeHTTPTask) o;
			hikeHTTPTask.setActivity(this);
			dialog = ProgressDialog.show(this, null,
					getString(R.string.calling_you));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
		}

		if (savedInstanceState != null) {
			msisdnErrorDuringSignup = savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR);
			int dispChild = savedInstanceState
					.getInt(HikeConstants.Extras.SIGNUP_PART);
			showingSecondLoadingTxt = savedInstanceState
					.getBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT);
			removeAnimation();
			viewFlipper.setDisplayedChild(dispChild);
			switch (dispChild) {
			case NUMBER:
				countryCode = savedInstanceState
						.getString(HikeConstants.Extras.COUNTRY_CODE);
				prepareLayoutForFetchingNumber();
				if (showingSecondLoadingTxt) {
					loadingText.setText(R.string.almost_there_signup);
				}
				break;
			case PIN:
				prepareLayoutForGettingPin();
				break;
			case NAME:
				prepareLayoutForGettingName();
				break;
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING)) {
				startLoading();
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_ERROR)) {
				showErrorMsg();
			}
			enterEditText.setText(savedInstanceState
					.getString(HikeConstants.Extras.SIGNUP_TEXT));
		} else {
			if (getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false)) {
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName();
			} else {
				prepareLayoutForFetchingNumber();
			}

		}
		setAnimation();
		setListeners();
		mTask = SignupTask.startTask(this);

		AnimationDrawable ad = new AnimationDrawable();
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large0), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large1), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large2), 600);
		ad.setOneShot(false);
		ad.setVisible(true, true);

		errorImage.setImageDrawable(ad);
		ad.start();
	}

	@Override
	public void onFinish(boolean success) {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		if (hikeHTTPTask == null) {
			if (success) {
				SharedPreferences prefs = getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

				/*
				 * Setting the app value as to if the user is Indian or not.
				 */
				String countryCode = prefs.getString(
						HikeMessengerApp.COUNTRY_CODE, "");
				HikeMessengerApp.setIndianUser(HikeConstants.INDIA_COUNTRY_CODE
						.equals(countryCode));

				prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF,
						HikeMessengerApp.isIndianUser());
				editor.commit();

				// Tracking the registration event for Fiksu
				FiksuTrackingManager.uploadRegistrationEvent(this, "");

				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Intent i = new Intent(SignupActivity.this,
								MessagesList.class);
						i.putExtra(HikeConstants.Extras.FIRST_TIME_USER, true);
						startActivity(i);
						finish();
					}
				}, 2500);
			} else if (mCurrentState != null && mCurrentState.value != null
					&& mCurrentState.value.equals(HikeConstants.CHANGE_NUMBER)) {
				restartTask();
			}
		} else {
			hikeHTTPTask = null;
		}
	}

	public void onClick(View v) {
		if (v.getId() == submitBtn.getId()) {
			submitClicked();
		} else if (v.getId() == tryAgainBtn.getId()) {
			restartTask();
		} else if (tapHereText != null && v.getId() == tapHereText.getId()) {
			mTask.addUserInput("");
		} else if (callmeBtn != null && v.getId() == callmeBtn.getId()) {
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/pin-call",
					new HikeHttpRequest.HikeHttpCallback() {
						public void onFailure() {
						}

						public void onSuccess(JSONObject response) {
						}
					});
			JSONObject request = new JSONObject();
			try {
				request.put(
						"msisdn",
						getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
								0).getString(HikeMessengerApp.MSISDN_ENTERED,
								null));
			} catch (JSONException e) {
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			hikeHttpRequest.setJSONData(request);

			hikeHTTPTask = new HikeHTTPTask(this, R.string.call_me_fail, false);
			hikeHTTPTask.execute(hikeHttpRequest);

			dialog = ProgressDialog.show(this, null,
					getResources().getString(R.string.calling_you));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (hikeHTTPTask != null && !hikeHTTPTask.isFinished()) {
			return hikeHTTPTask;
		}
		return super.onRetainNonConfigurationInstance();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	private void startLoading() {
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		if (invalidNum != null) {
			invalidNum.setVisibility(View.GONE);
		}
		if (tapHereText != null) {
			tapHereText.setVisibility(View.GONE);
		}
		if (countryPicker != null) {
			countryPicker.setEnabled(false);
		}
		if (callmeBtn != null) {
			callmeBtn.setVisibility(View.GONE);
		}
	}

	private void submitClicked() {
		if (TextUtils.isEmpty(enterEditText.getText())) {
			Toast toast = Toast.makeText(this, R.string.enter_some_text,
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return;
		}
		startLoading();
		if (!addressBookError) {
			if (viewFlipper.getDisplayedChild() == NUMBER
					&& !enterEditText.getText().toString()
							.matches(HikeConstants.VALID_MSISDN_REGEX)) {
				loadingLayout.setVisibility(View.GONE);
				submitBtn.setVisibility(View.VISIBLE);
				invalidNum.setVisibility(View.VISIBLE);
			} else {
				String input = enterEditText.getText().toString();
				if (viewFlipper.getDisplayedChild() == NUMBER) {
					String code = countryPicker.getText().toString();
					code = code.substring(code.indexOf("+"), code.length());
					input = code + input;
				}
				mTask.addUserInput(input);
			}
		} else {
			showErrorMsg();
			addressBookError = false;
		}
	}

	private void initializeViews(ViewGroup layout) {
		switch (layout.getId()) {
		case R.id.name_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_name);
			break;
		case R.id.num_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_num);
			break;
		case R.id.pin_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_pin);
			break;
		}
		infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		tapHereText = (Button) layout.findViewById(R.id.wrong_num);
		submitBtn = (Button) layout.findViewById(R.id.btn_continue);
		invalidNum = (TextView) layout.findViewById(R.id.invalid_num);
		countryPicker = (Button) layout.findViewById(R.id.country_picker);
		callmeBtn = (Button) layout.findViewById(R.id.btn_call_me);

		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
	}

	private void prepareLayoutForFetchingNumber() {
		initializeViews(numLayout);

		countryPicker.setEnabled(true);

		String prevCode = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
				HikeMessengerApp.COUNTRY_CODE, "");
		countryNamesAndCodes = getResources().getStringArray(
				R.array.country_names_and_codes);
		countryISOAndCodes = getResources().getStringArray(
				R.array.country_iso_and_codes);

		if (TextUtils.isEmpty(countryCode)) {
			TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = TextUtils.isEmpty(prevCode) ? manager
					.getNetworkCountryIso().toUpperCase() : prevCode;
			for (String s : countryISOAndCodes) {
				if (!TextUtils.isEmpty(countryIso) && s.contains(countryIso)) {
					Log.d(getClass().getSimpleName(), "COUNTRY CODE: " + s);
					countryCode = s;
					break;
				}
			}
			countryPicker
					.setText(TextUtils.isEmpty(countryCode) ? defaultCountryCode
							: countryCode);
		} else {
			countryPicker.setText(countryCode);
		}
		formatCountryPickerText(countryPicker.getText().toString());

		infoTxt.setText(msisdnErrorDuringSignup ? R.string.enter_phone_again_signup
				: R.string.enter_num_signup);
		invalidNum.setVisibility(View.INVISIBLE);
		loadingText.setText(R.string.verifying_num_signup);
	}

	private void formatCountryPickerText(String code) {
		if (countryPicker == null) {
			return;
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder(code);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, code.indexOf("+"),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		countryPicker.setText(ssb);
	}

	public void onCountryPickerClick(View v) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				SignupActivity.this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				countryNamesAndCodes) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);

				String text = tv.getText().toString();
				SpannableStringBuilder spannable = new SpannableStringBuilder(
						text);
				spannable.setSpan(new ForegroundColorSpan(getResources()
						.getColor(R.color.country_code)), text.indexOf("+"),
						text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.setText(spannable);

				return v;
			}
		};

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						formatCountryPickerText(countryISOAndCodes[which]);
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void prepareLayoutForGettingPin() {
		initializeViews(pinLayout);
		callmeBtn.setVisibility(View.VISIBLE);

		enterEditText.setText("");
		infoTxt.setText(R.string.enter_pin_signup);
		tapHereText.setOnClickListener(this);

		String tapHere = getString(R.string.tap_here_signup);
		String tapHereString = getString(R.string.wrong_num_signup);

		SpannableStringBuilder ssb = new SpannableStringBuilder(tapHereString);
		ssb.setSpan(new ForegroundColorSpan(0xff6edcff),
				tapHereString.indexOf(tapHere), tapHereString.indexOf(tapHere)
						+ tapHere.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		tapHereText.setText(ssb);
	}

	private void prepareLayoutForGettingName() {
		initializeViews(nameLayout);

		String msisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0).getString(HikeMessengerApp.MSISDN_SETTING, null);
		if (TextUtils.isEmpty(msisdn)) {
			Utils.logEvent(SignupActivity.this,
					HikeConstants.LogEvent.SIGNUP_ERROR);
			msisdnErrorDuringSignup = true;
			resetViewFlipper();
			restartTask();
			return;
		}
		String nameText = getString(R.string.all_set_signup, msisdn);
		SpannableStringBuilder ssb = new SpannableStringBuilder(nameText);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), nameText.indexOf(msisdn),
				nameText.indexOf(msisdn) + msisdn.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		infoTxt.setText(ssb);
	}

	private void resetViewFlipper() {
		booBooLayout.setVisibility(View.GONE);
		viewFlipper.setVisibility(View.VISIBLE);
		removeAnimation();
		viewFlipper.setDisplayedChild(NUMBER);
		prepareLayoutForFetchingNumber();
		setAnimation();
	}

	private void restartTask() {
		resetViewFlipper();
		mTask = SignupTask.restartTask(this);
	}

	private void showErrorMsg() {
		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
		booBooLayout.setVisibility(View.VISIBLE);
		viewFlipper.setVisibility(View.GONE);
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(enterEditText.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private void setListeners() {
		enterEditText.setOnEditorActionListener(this);
		enterEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return loadingLayout.getVisibility() == View.VISIBLE
						&& (event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK);
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(HikeConstants.Extras.SIGNUP_PART,
				viewFlipper.getDisplayedChild());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING,
				loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR,
				booBooLayout.getVisibility() == View.VISIBLE);
		outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText
				.getText().toString());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR,
				msisdnErrorDuringSignup);
		outState.putBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT,
				showingSecondLoadingTxt);
		if (viewFlipper.getDisplayedChild() == NUMBER) {
			outState.putString(HikeConstants.Extras.COUNTRY_CODE, countryPicker
					.getText().toString());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (mTask != null) {
			mTask.cancelTask();
			mTask = null;
		}
		SignupTask.isAlreadyFetchingNumber = false;
		super.onBackPressed();
	}

	private void removeAnimation() {
		viewFlipper.setInAnimation(null);
		viewFlipper.setOutAnimation(null);
	}

	private void setAnimation() {
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_in_right));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_out_left));
	}

	@Override
	public void onProgressUpdate(StateValue stateValue) {
		String value = stateValue.value;
		mCurrentState = stateValue;
		Log.d("SignupActivity", "Current State " + mCurrentState.state.name()
				+ " VALUE: " + value);

		if (mHandler == null) {
			mHandler = new Handler();
		}

		showingSecondLoadingTxt = false;
		switch (stateValue.state) {
		case MSISDN:
			if (TextUtils.isEmpty(value)) {
				prepareLayoutForFetchingNumber();
			} else if (value.equals(HikeConstants.DONE)) {
				removeAnimation();
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName();
				setAnimation();
			} else {
				/* yay, got the actual MSISDN */
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName();
			}
			break;
		case PULLING_PIN:
			if (viewFlipper.getDisplayedChild() == NUMBER) {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (viewFlipper != null
								&& viewFlipper.getDisplayedChild() == NUMBER) {
							showingSecondLoadingTxt = true;
							loadingText.setText(R.string.almost_there_signup);
						}
					}
				}, HikeConstants.PIN_CAPTURE_TIME / 2);
			}
			break;
		case PIN:
			viewFlipper.setDisplayedChild(PIN);
			prepareLayoutForGettingPin();

			// Wrong Pin
			if (value != null && value.equals(HikeConstants.PIN_ERROR)) {
				infoTxt.setText(R.string.wrong_pin_signup);
				loadingLayout.setVisibility(View.GONE);
				callmeBtn.setVisibility(View.VISIBLE);
				submitBtn.setVisibility(View.VISIBLE);
				if (tapHereText != null) {
					tapHereText.setVisibility(View.VISIBLE);
				}
				enterEditText.setText("");
			}
			// Manual entry for pin
			else {
				setAnimation();
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value)) {
				prepareLayoutForGettingName();
			} else {
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						loadingText.setText(R.string.getting_in_signup);
					}
				}, 1000);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR)) {
				addressBookError = true;
				if (loadingLayout.getVisibility() == View.VISIBLE) {
					showErrorMsg();
				}
			} else if (value == null
					|| !value.equals(HikeConstants.CHANGE_NUMBER)) {
				showErrorMsg();
			}
			break;
		}
		setListeners();
	}

	@Override
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
		if ((actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& !TextUtils
						.isEmpty(enterEditText.getText().toString().trim())
				&& loadingLayout.getVisibility() != View.VISIBLE) {
			submitClicked();
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		Log.d(getClass().getSimpleName(), "Dialog cancelled");
		if (hikeHTTPTask != null) {
			hikeHTTPTask.setActivity(null);
			hikeHTTPTask = null;
		}
	}
}