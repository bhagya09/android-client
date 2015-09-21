package com.kpt.adaptxt.beta.settings;

import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;

/**
 * @author nikhil joshi
 * 
 */

public class KPTAddATRShortcut extends Activity implements
		KPTCoreServiceListener {

	private TextView shortcut_alert;
	private EditText shortcutEditText;
	private EditText substitutionEditText;
	private String mShortcut;
	private String mSubstitution;

	private String regularExpression;

	private Button generateShortcutButton;
	private Button doneButton;
	private Button cancelButton;

	/**
	 * Core interface handle
	 */
	private KPTCoreEngine mCoreEngine = null;

	/**
	 * Service handler to initiate and obtain core interface handle
	 */
	private KPTCoreServiceHandler mCoreServiceHandler = null;

	public static boolean DISABLE_DOUBLE_SPACE_TAB = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.kpt_atr_addshortcut);

		regularExpression = getResources().getString(
				R.string.kpt_atr_regular_expression);

		if (mCoreServiceHandler == null) {
			mCoreServiceHandler = KPTCoreServiceHandler
					.getCoreServiceInstance(getApplicationContext());
			mCoreEngine = mCoreServiceHandler.getCoreInterface();

			if (mCoreEngine != null) {
				mCoreEngine.initializeCore(this);
			}
		}

		if (mCoreEngine == null) {
			mCoreServiceHandler.registerCallback(this);
		}

		shortcutEditText = (EditText) findViewById(R.id.shortcutField);
		shortcutEditText.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_FILTER);


		shortcutEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				DISABLE_DOUBLE_SPACE_TAB = hasFocus;
			}
		});

		generateShortcutButton = (Button) findViewById(R.id.generate_shortcut);
		substitutionEditText = (EditText) findViewById(R.id.substitutionField);
		substitutionEditText
				.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
						| InputType.TYPE_TEXT_FLAG_MULTI_LINE);

		shortcut_alert = (TextView) findViewById(R.id.shortcut_alert);
		shortcut_alert.setTypeface(null, Typeface.BOLD);

		doneButton = (Button) findViewById(R.id.done);
		cancelButton = (Button) findViewById(R.id.cancel);

		doneButton.setEnabled(false);

		generateShortcutButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				mSubstitution = substitutionEditText.getText().toString()
						.trim();

				if (mSubstitution == null || "".equalsIgnoreCase(mSubstitution)) {
					/* case WHERE "Substitution must be 3 characters"; */
					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_7_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mSubstitution.length() < 3) {
					/* case WHERE "Substitution must be 3 characters"; */

					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_4_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mSubstitution.length() > 256) {
					shortcut_alert.setText(getResources().getString(
							R.string.kpt_atr_substitutn_alert));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else {
					
					//Removed random selection of characters and fixed enhanchement according #9348
					String originalString = substitutionEditText.getText()
							.toString().trim();
				
					originalString = originalString.replaceAll("[*]","");
					originalString = replaceCharacters(originalString,
							regularExpression);
					String newshortcut = generateString( originalString);

					if (newshortcut.equalsIgnoreCase("")) {
						String heading = "Enter characters to generate shortcut";
						final AlertDialog alert = new AlertDialog.Builder(
								KPTAddATRShortcut.this).create();
						alert.setTitle(heading);
						alert.setButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										alert.dismiss();

									}
								});
						alert.show();
					} else if (newshortcut.length() < 3) {
						/*
						 * case where Shortcut must be 3 characters
						 */

						shortcut_alert.setText(getResources().getString(
								R.string.kpt_UI_STRING_MENUITEM_DESC_3_7003));
						shortcut_alert.setTextColor(Color.RED);
						doneButton.setEnabled(false);

					} else {
						shortcutEditText.setText(newshortcut);
					}
				}
			}
		});

		doneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mShortcut = shortcutEditText.getText().toString().trim()
						.toLowerCase();
				mSubstitution = substitutionEditText.getText().toString()
						.trim();

				if (mSubstitution == null || "".equalsIgnoreCase(mSubstitution)) {
					/* case WHERE "Substitution must be 3 characters"; */
					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_7_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mSubstitution.length() < 3) {
					/* case WHERE "Substitution must be 3 characters"; */
					// ContextThemeWrapper ctw = new ContextThemeWrapper(
					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_4_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mShortcut == null || "".equalsIgnoreCase(mShortcut)) {
					/*
					 * case where
					 * "Shortcut cannot be empty (or) Select Generate ATR shortcut button"
					 * ;
					 */

					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_6_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mShortcut.length() < 2) {
					/*
					 * case where Shortcut must be 3 characters
					 */

					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_MENUITEM_DESC_3_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else if (mCoreEngine.getATRShortcuts().contains(
						shortcutEditText.getText().toString().trim()
								.toLowerCase())) {
					/*
					 * case where This Shortcut already exists
					 */
					shortcut_alert.setText(getResources().getString(
							R.string.kpt_UI_STRING_DIALOG_MSG_5_7003));
					shortcut_alert.setTextColor(Color.RED);
					doneButton.setEnabled(false);

				} else {
					
					String shortCut = shortcutEditText.getText().toString()
							.trim();
					String expansion = substitutionEditText.getText()
							.toString().trim();

					shortcutEditText.setText("");
					substitutionEditText.setText("");

					mCoreEngine.addATRShortcutAndExpansion(shortCut, expansion);

					Intent intent = new Intent(KPTAddATRShortcut.this,
							KPTATRListview.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				}
			}
		});
		
		

		substitutionEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				if (shortcutEditText.getText().length() > 0)
					doneButton.setEnabled(true);
				else
					doneButton.setEnabled(false);
			}

			@Override
			public void afterTextChanged(Editable s) {
				shortcut_alert.setText(getResources().getString(
						R.string.kpt_UI_STRING_MENUITEM_DESC_3_7003));
				shortcut_alert.setTypeface(null, Typeface.BOLD);
				shortcut_alert.setTextColor(Color.WHITE);
			}
		});

		InputFilter filter = new InputFilter() {

			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (!Character.isLetter(source.charAt(i))) {
						KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB = true;
						return "";
					}
				}
				return null;
			}
		};

		InputFilter[] FilterArray = new InputFilter[2];

		FilterArray[0] = new InputFilter.LengthFilter(4);
		FilterArray[1] = filter;
		shortcutEditText.setFilters(FilterArray);


		shortcutEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//DISABLE_DOUBLE_SPACE_TAB = true;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				if (substitutionEditText.getText().length() > 0)
					doneButton.setEnabled(true);
				else
					doneButton.setEnabled(false);
			}

			@Override
			public void afterTextChanged(Editable s) {
				shortcut_alert.setText(getResources().getString(
						R.string.kpt_UI_STRING_MENUITEM_DESC_3_7003));
				shortcut_alert.setTypeface(null, Typeface.BOLD);
				shortcut_alert.setTextColor(Color.WHITE);
				//DISABLE_DOUBLE_SPACE_TAB = false;
			}
		});

		substitutionEditText
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {

							doneButton.performClick();
							return true;
						}
						return false;
					}
				});

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				//TP 13236
				shortcutEditText.setText("");
				substitutionEditText.setText("");
				
				Intent intent = new Intent(KPTAddATRShortcut.this,
						KPTATRListview.class);
				startActivity(intent);

			}
		});

	}
	
	


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mCoreEngine != null) {
			mCoreEngine.saveUserContext();
			mCoreEngine.destroyCore();
		}

		if (mCoreServiceHandler != null) {
			mCoreServiceHandler.unregisterCallback(this);
			mCoreServiceHandler.destroyCoreService();
		}
		mCoreEngine = null;
		mCoreServiceHandler = null;
	}

	public static String generateString(String characters)
			 {
		StringTokenizer st = new StringTokenizer(characters, " ");
		StringBuffer text =  new StringBuffer();
		

		String token;
		
		
		switch (st.countTokens()) {
		
		case 0:
		case -1:
			break;
		case 1:
			token = st.nextToken();
			text.append(token.charAt(0));
			if(token.length()>2){
			text.append(token.charAt(1));
			text.append(token.charAt(2));
			}
			break;
		case 2:
			
			token= st.nextToken();
			text.append(token.charAt(0));
			token= st.nextToken();
			text.append(token.charAt(0));
			if(token.length()>=2){
				text.append(token.charAt(1));
			}
			
			
			break;
		default:
			token= st.nextToken();
			text.append(token.charAt(0));
			token= st.nextToken();
			text.append(token.charAt(0));
			token= st.nextToken();
			text.append(token.charAt(0));
			break;
		}
		
		return new String(text);
	}

	public static String replaceCharacters(String originalString,
			String regExpression) {
		String newString = originalString;

		newString = newString.replaceAll(regExpression, "");
		return newString;
	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK &&  event.getRepeatCount() == 0) {
			Intent intent = new Intent(KPTAddATRShortcut.this,
					KPTATRListview.class);
			startActivity(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
