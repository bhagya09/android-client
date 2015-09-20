/*
 *   Copyright 2012 Hai Bison
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.haibison.android.lockpattern;

import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_CAPTCHA_WIRED_DOTS;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_MAX_RETRIES;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_MIN_WIRED_DOTS;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_STEALTH_MODE;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_AUTO_SAVE_PATTERN;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_ENCRYPTER_CLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bsb.hike.BuildConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.haibison.android.lockpattern.util.IEncrypter;
import com.haibison.android.lockpattern.util.InvalidEncrypterException;
import com.haibison.android.lockpattern.util.LoadingDialog;
import com.haibison.android.lockpattern.util.Settings;
import com.haibison.android.lockpattern.util.Settings.Display;
import com.haibison.android.lockpattern.util.Settings.Security;
import com.haibison.android.lockpattern.util.UI;
import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;
import com.haibison.android.lockpattern.widget.LockPatternView.DisplayMode;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Main activity for this library.
 * <p>
 * You can deliver result to {@link PendingIntent}'s and/ or
 * {@link ResultReceiver} too. See {@link #EXTRA_PENDING_INTENT_OK},
 * {@link #EXTRA_PENDING_INTENT_CANCELLED} and {@link #EXTRA_RESULT_RECEIVER}
 * for more details.
 * </p>
 * 
 * <h1>NOTES</h1>
 * <ul>
 * <li>
 * You must use one of built-in actions when calling this activity. They start
 * with {@code ACTION_*}. Otherwise the library might behave strangely (we don't
 * cover those cases).</li>
 * <li>You must use one of the themes that this library supports. They start
 * with {@code R.style.Alp_42447968_Theme_*}. The reason is the themes contain
 * resources that the library needs.</li>
 * <li>With {@link #ACTION_COMPARE_PATTERN}, there are <b><i>4 possible result
 * codes</i></b>: {@link Activity#RESULT_OK}, {@link Activity#RESULT_CANCELED},
 * {@link #RESULT_FAILED} and {@link #RESULT_FORGOT_PATTERN}.</li>
 * <li>With {@link #ACTION_VERIFY_CAPTCHA}, there are <b><i>3 possible result
 * codes</i></b>: {@link Activity#RESULT_OK}, {@link Activity#RESULT_CANCELED},
 * and {@link #RESULT_FAILED}.</li>
 * </ul>
 * 
 * @author Hai Bison
 * @since v1.0
 */
public class LockPatternActivity extends HikeAppStateBaseFragmentActivity implements AdaptxtEditTextEventListner, AdaptxtKeyboordVisibilityStatusListner {

    private static final String CLASSNAME = LockPatternActivity.class.getName();

    /**
     * Use this action to create new pattern. You can provide an
     * {@link IEncrypter} with
     * {@link Security#setEncrypterClass(android.content.Context, Class)} to
     * improve security.
     * <p/>
     * If the user created a pattern, {@link Activity#RESULT_OK} returns with
     * the pattern ({@link #EXTRA_PATTERN}). Otherwise
     * {@link Activity#RESULT_CANCELED} returns.
     * 
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @since v2.4 beta
     */
    public static final String ACTION_CREATE_PATTERN = CLASSNAME
            + ".create_pattern";

    /**
     * Use this action to compare pattern. You provide the pattern to be
     * compared with {@link #EXTRA_PATTERN}.
     * <p/>
     * If you enabled feature auto-save pattern before (with
     * {@link Security#setAutoSavePattern(android.content.Context, boolean)} ),
     * then you don't need {@link #EXTRA_PATTERN} at this time. But if you use
     * this extra, its priority is higher than the one stored in shared
     * preferences.
     * <p/>
     * You can use {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN} to help your
     * users in case they forgot the patterns.
     * <p/>
     * If the user passes, {@link Activity#RESULT_OK} returns. If not,
     * {@link #RESULT_FAILED} returns.
     * <p/>
     * If the user cancels the task, {@link Activity#RESULT_CANCELED} returns.
     * <p/>
     * In any case, there will have extra {@link #EXTRA_RETRY_COUNT} available
     * in the intent result.
     * 
     * @see #EXTRA_PATTERN
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @see #RESULT_FAILED
     * @see #EXTRA_RETRY_COUNT
     * @since v2.4 beta
     */
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME
            + ".compare_pattern";

    /**
     * Use this action to let the activity generate a random pattern and ask the
     * user to re-draw it to verify.
     * <p/>
     * The default length of the auto-generated pattern is {@code 4}. You can
     * change it with
     * {@link Display#setCaptchaWiredDots(android.content.Context, int)}.
     * 
     * @since v2.7 beta
     */
    public static final String ACTION_VERIFY_CAPTCHA = CLASSNAME
            + ".verify_captcha";

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user fails to "login"
     * after a number of tries, this activity will finish with this result code.
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     */
    public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user forgot his/ her
     * pattern and decided to ask for your help with recovering the pattern (
     * {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN}), this activity will finish
     * with this result code.
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     * @see #EXTRA_PENDING_INTENT_FORGOT_PATTERN
     * @since v2.8 beta
     */
    public static final int RESULT_FORGOT_PATTERN = RESULT_FIRST_USER + 2;

    /**
     * For actions {@link #ACTION_COMPARE_PATTERN} and
     * {@link #ACTION_VERIFY_CAPTCHA}, this key holds the number of tries that
     * the user attempted to verify the input pattern.
     */
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".retry_count";

    /**
     * Sets value of this key to a theme in {@code R.style.Alp_42447968_Theme_*}
     * . Default is the one you set in your {@code AndroidManifest.xml}. Note
     * that theme {@link R.style#Alp_42447968_Theme_Light_DarkActionBar} is
     * available in API 4+, but it only works in API 14+.
     * 
     * @since v1.5.3 beta
     */
    public static final String EXTRA_THEME = CLASSNAME + ".theme";

    /**
     * Key to hold the pattern. It must be a {@code char[]} array.
     * <p/>
     * <ul>
     * <li>If you use encrypter, it should be an encrypted array.</li>
     * <li>If you don't use encrypter, it should be the SHA-1 value of the
     * actual pattern. You can generate the value by
     * {@link LockPatternUtils#patternToSha1(List)}.</li>
     * </ul>
     * 
     * @since v2 beta
     */
    public static final String EXTRA_PATTERN = CLASSNAME + ".pattern";

    /**
     * You can provide an {@link ResultReceiver} with this key. The activity
     * will notify your receiver the same result code and intent data as you
     * will receive them in {@link #onActivityResult(int, int, Intent)}.
     * 
     * @since v2.4 beta
     */
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME
            + ".result_receiver";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before
     * {@link Activity#RESULT_OK} will be returning. If you were calling this
     * activity with {@link #ACTION_CREATE_PATTERN}, key {@link #EXTRA_PATTERN}
     * will be attached to the original intent which the pending intent holds.
     * 
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME
            + ".pending_intent_ok";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before
     * {@link Activity#RESULT_CANCELED} will be returning.
     * 
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME
            + ".pending_intent_cancelled";

    /**
     * You put a {@link PendingIntent} into this extra. The library will show a
     * button <i>"Forgot pattern?"</i> and call your intent later when the user
     * taps it.
     * <p/>
     * <h1>Notes</h1>
     * <ul>
     * <li>If you use an activity, you don't need
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternActivity} .</li>
     * <li>{@link LockPatternActivity} will finish with
     * {@link #RESULT_FORGOT_PATTERN} <i><b>after</b> making a call</i> to start
     * your pending intent.</li>
     * <li>It is your responsibility to make sure the Intent is good. The
     * library doesn't cover any errors when calling your intent.</li>
     * </ul>
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @since v2.8 beta
     * @author Thanks to Yan Cheng Cheok for his idea.
     */
    public static final String EXTRA_PENDING_INTENT_FORGOT_PATTERN = CLASSNAME
            + ".pending_intent_forgot_pattern";

    /**
     * Helper enum for button OK commands. (Because we use only one "OK" button
     * for different commands).
     * 
     * @author Hai Bison
     */
    private static enum ButtonOkCommand {
        CONTINUE, FORGOT_PATTERN, DONE
    }// ButtonOkCommand

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = DateUtils.SECOND_IN_MILLIS;

    /*
     * HIKE KEYBOARD FIELDS
     */
    private CustomKeyboard mCustomKeyboard;
    private boolean systemKeyboard;
    
    /*
     * FIELDS
     */
    private int mMaxRetries, mMinWiredDots, mRetryCount = 0, mCaptchaWiredDots;
    private boolean mAutoSave, mStealthMode;
    private IEncrypter mEncrypter;
    private ButtonOkCommand mBtnOkCmd;
    private Intent mIntentResult;

    /*
     * CONTROLS
     */
    private TextView mTextInfo;
    private LockPatternView mLockPatternView;
    private CustomFontEditText mLockPinView;
    private View mFooter;
    private Button mBtnCancel;
    private Button mBtnConfirm;
    private Boolean mIsRetryBtnVisible;
    private Button changePasswordSetting;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG)
            Log.d(CLASSNAME, "ClassName = " + CLASSNAME);

        /*
         * EXTRA_THEME
         */

        if (getIntent().hasExtra(EXTRA_THEME))
            setTheme(getIntent().getIntExtra(EXTRA_THEME,
                    R.style.Alp_42447968_Theme_Dialog_Dark));

        super.onCreate(savedInstanceState);
        
        Utils.blockOrientationChange(this);

        loadSettings();

        mIntentResult = new Intent();
        setResult(RESULT_CANCELED, mIntentResult);

        initContentView();
    }// onCreate()

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (BuildConfig.DEBUG)
            Log.d(CLASSNAME, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        initContentView();
    }// onConfigurationChanged()

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            /*
             * Use this hook instead of onBackPressed(), because onBackPressed()
             * is not available in API 4.
             */
            finishWithNegativeResult(RESULT_FAILED);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }// onKeyDown()

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*
         * Support canceling dialog on touching outside in APIs < 11.
         * 
         * This piece of code is copied from android.view.Window. You can find
         * it by searching for methods shouldCloseOnTouch() and isOutOfBounds().
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                && event.getAction() == MotionEvent.ACTION_DOWN
                && getWindow().peekDecorView() != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(this)
                    .getScaledWindowTouchSlop();
            final View decorView = getWindow().getDecorView();
            boolean isOutOfBounds = (x < -slop) || (y < -slop)
                    || (x > (decorView.getWidth() + slop))
                    || (y > (decorView.getHeight() + slop));
            if (isOutOfBounds) {
                finishWithNegativeResult(RESULT_CANCELED);
                return true;
            }
        }// if

        return super.onTouchEvent(event);
    }// onTouchEvent()

    /**
     * Loads settings, either from manifest or {@link Settings}.
     */
    private void loadSettings() {
        Bundle metaData = null;
        try {
            metaData = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA).metaData;
        } catch (NameNotFoundException e) {
            /*
             * Never catch this.
             */
            e.printStackTrace();
        }

        if (metaData != null && metaData.containsKey(METADATA_MIN_WIRED_DOTS))
            mMinWiredDots = Settings.Display.validateMinWiredDots(this,
                    metaData.getInt(METADATA_MIN_WIRED_DOTS));
        else
            mMinWiredDots = Settings.Display.getMinWiredDots(this);

        if (metaData != null && metaData.containsKey(METADATA_MAX_RETRIES))
            mMaxRetries = Settings.Display.validateMaxRetries(this,
                    metaData.getInt(METADATA_MAX_RETRIES));
        else
            mMaxRetries = Settings.Display.getMaxRetries(this);

        if (metaData != null
                && metaData.containsKey(METADATA_AUTO_SAVE_PATTERN))
            mAutoSave = metaData.getBoolean(METADATA_AUTO_SAVE_PATTERN);
        else
            mAutoSave = Settings.Security.isAutoSavePattern(this);

        if (metaData != null
                && metaData.containsKey(METADATA_CAPTCHA_WIRED_DOTS))
            mCaptchaWiredDots = Settings.Display.validateCaptchaWiredDots(this,
                    metaData.getInt(METADATA_CAPTCHA_WIRED_DOTS));
        else
            mCaptchaWiredDots = Settings.Display.getCaptchaWiredDots(this);

        if (metaData != null && metaData.containsKey(METADATA_STEALTH_MODE))
            mStealthMode = metaData.getBoolean(METADATA_STEALTH_MODE);
        else
            mStealthMode = Settings.Display.isStealthMode(this);

        /*
         * Encrypter.
         */
        char[] encrypterClass;
        if (metaData != null && metaData.containsKey(METADATA_ENCRYPTER_CLASS))
            encrypterClass = metaData.getString(METADATA_ENCRYPTER_CLASS)
                    .toCharArray();
        else
            encrypterClass = Settings.Security.getEncrypterClass(this);

        if (encrypterClass != null) {
            try {
                mEncrypter = (IEncrypter) Class.forName(
                        new String(encrypterClass), false, getClassLoader())
                        .newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }
    }// loadSettings()

    /**
     * Initializes UI...
     */
    private void initContentView() {
        /*
         * Save all controls' state to restore later.
         */
        CharSequence infoText = mTextInfo != null ? mTextInfo.getText() : null;
        Boolean btnOkEnabled = mBtnConfirm != null ? mBtnConfirm.isEnabled()
                : null;
        LockPatternView.DisplayMode lastDisplayMode = mLockPatternView != null ? mLockPatternView
                .getDisplayMode() : null;
        List<Cell> lastPattern = mLockPatternView != null ? mLockPatternView
                .getPattern() : null;
                
        if(getActionBar() != null)
        {
        	getActionBar().hide();
        }

        setContentView(R.layout.alp_42447968_lock_pattern_parent);
        UI.adjustDialogSizeForLargeScreens(getWindow(), (LinearLayout) findViewById(R.id.parentView));
        
        mTextInfo = (TextView) findViewById(R.id.alp_42447968_textview_info);
        mLockPatternView = (LockPatternView) findViewById(R.id.alp_42447968_view_lock_pattern);
        mLockPinView = (CustomFontEditText) findViewById(R.id.alp_42447968_lock_pin);

        systemKeyboard = HikeMessengerApp.isSystemKeyboard(getApplicationContext());
        if (!systemKeyboard)
        {
            initCustomKeyboard();        	
        }

        mFooter = findViewById(R.id.alp_42447968_viewgroup_footer);
        mBtnCancel = (Button) findViewById(R.id.alp_42447968_button_cancel);
        mBtnConfirm = (Button) findViewById(R.id.alp_42447968_button_confirm);

        mLockPinView.setFocusable(true);
        mLockPatternView.setFocusable(true);
        mLockPinView.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
            	if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                   
                    mBtnConfirm.setEnabled(false);
                    if (mBtnOkCmd == ButtonOkCommand.CONTINUE){
                    	if(s.length()>0)
                    	{
                    		if (s.length() == 4){
                        		mTextInfo.setText(R.string.stealth_msg_pin_recorded);
                        		mBtnConfirm.setEnabled(true);
                        	} 
                    		else 
                        	{
	                    		mTextInfo.setText(R.string.stealth_enter_4_digits);
	                    		mBtnConfirm.setEnabled(false);
	                    		changeCancelToRetry();
                        	}
                    	}
                    	else
                    	{
                    		mTextInfo.setText(R.string.stealth_msg_enter_an_unlock_pin);
                    		changeRetryToCancel();
                    		mBtnConfirm.setEnabled(false);
                    	}
                    	
                    } else {
                    	if(s.length()>0)
                    	{
                    		if (s.length() == 4){
                                boolean check = doCheckAndCreatePin(mLockPinView.getText().toString());
                        		mTextInfo.setText(check ? R.string.stealth_msg_your_new_unlock_pin : R.string.stealth_msg_try_pin_again);
//                        		if(!check) 
//                        			mLockPinView.setText("");   
                        		mBtnConfirm.setEnabled(check);
                        	} 
                    		else 
                        	{
	                    		mTextInfo.setText(R.string.stealth_enter_4_digits);
	                    		mBtnConfirm.setEnabled(false);
	                    		changeCancelToRetry();
                        	}
                    	}
                    	else
                    	{
                    		mTextInfo.setText(R.string.stealth_msg_reenter_pin_to_confirm);
                    		changeRetryToCancel();
                    		mBtnConfirm.setEnabled(false);
                    	}
                    	
                    }
                }// ACTION_CREATE_PATTERN
                else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                	if(s.length() == 4)
                	{

                        if (doComparePin(mLockPinView.getText().toString()))
                        {
                            finishWithResultOk(null);
                            if (mCustomKeyboard != null)
                            {
                                mCustomKeyboard.updateCore();
                            }
                        }
                        else 
                        {
                            finishWithNegativeResult(RESULT_CANCELED); 
                            if (mCustomKeyboard != null)
                            {
                                mCustomKeyboard.updateCore();
                            }                        }
                	}
                    mTextInfo
                            .setText(R.string.stealth_msg_enter_pin_to_unlock);
                }// ACTION_COMPARE_PATTERN
                else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                    mTextInfo
                            .setText(R.string.stealth_msg_enter_an_unlock_pin);
                }// ACTION_VERIFY_CAPTCHA
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        }); 

        changePasswordSetting = (Button) findViewById(R.id.alp_42447968_change_password_setting);
        
        final boolean isReset = getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false);
        
        if(ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) 
        {
        	if(StealthModeManager.getInstance().isPinAsPassword())
        	{
        		mLockPatternView.setVisibility(View.GONE);
        		setGravity(mLockPinView);
        		mLockPinView.setVisibility(View.VISIBLE);
        		mLockPinView.requestFocus();
        		showKeyboard();
        		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        		//Somehow for lollipop, the view was not shifting downwards and hence we needed to add padding manually
        		if (Utils.isLollipopOrHigher() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        		{
        			findViewById(R.id.parentView).setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.bottom_padding_pin_view));
        		}
        	}
        	else
        	{
        		//Somehow for lollipop, the view was not shifting downwards and hence we needed to add padding manually
        		if(Utils.isLollipopOrHigher() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        		{
        			findViewById(R.id.parentView).setPadding(0, 0, 0, 0);
        		}
        		mLockPinView.setVisibility(View.GONE);
        		setGravity(mLockPatternView);
        		mLockPatternView.setVisibility(View.VISIBLE);
        		mLockPatternView.requestFocus();
        		hideKeyboard();
        	}

        	if(isReset)
            {
        		changePasswordSetting.setEnabled(false);
        		setAlphaForView(changePasswordSetting, 0.0f);
            }
        	changePasswordSetting.setText(getString(R.string.change_password_from_privacy));
        	changePasswordSetting.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View arg0)
				{
					Intent intent = Utils.getIntentForHiddenSettings(LockPatternActivity.this);
					startActivity(intent);
					finishWithNegativeResult(RESULT_CANCELED);
				}
			});
        } 
        else if(ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
        {
        	if(!StealthModeManager.getInstance().isPinAsPassword())
        	{
            	changePasswordSetting.setText(getString(R.string.stealth_set_pin));
            	mTextInfo.setText(R.string.stealth_msg_draw_an_unlock_pattern);
            	setGravity(mLockPatternView);
            	mLockPatternView.setVisibility(View.VISIBLE);
            	mLockPinView.setVisibility(View.GONE);
				mLockPatternView.requestFocus();
				hideKeyboard();
        	}
        	else
        	{
				mLockPatternView.setVisibility(View.GONE);
				setGravity(mLockPinView);
				mLockPinView.setVisibility(View.VISIBLE);		
        		changePasswordSetting.setText(getString(R.string.stealth_set_pattern));
            	mTextInfo.setText(R.string.stealth_msg_enter_an_unlock_pin);
            	mLockPinView.requestFocus();
            	showKeyboard();
            	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        	}
        	changePasswordSetting.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View arg0)
				{
					changeRetryToCancel();
					getIntent().removeExtra(EXTRA_PATTERN);
					mBtnConfirm.setEnabled(false);
					if(mLockPinView.getVisibility() != View.VISIBLE)
					{
						mLockPatternView.setVisibility(View.GONE);
						setGravity(mLockPinView);
						mLockPinView.setVisibility(View.VISIBLE);
			        	changePasswordSetting.setText(getString(R.string.stealth_set_pattern));
			        	if(!isReset)
			        	{
			        		StealthModeManager.getInstance().usePinAsPassword(true);
			        	}
						mLockPatternView.clearPattern();
			        	mLockPinView.requestFocus();
			        	showKeyboard();
			        	//Somehow for lollipop, the view was not shifting upwards and hence we needed to add padding manually
						if (Utils.isLollipopOrHigher() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
						{
							findViewById(R.id.parentView).setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.bottom_padding_pin_view));
						}
			        	mTextInfo.setText(R.string.stealth_msg_enter_an_unlock_pin);
					} else 
					{
						//Somehow for lollipop, the view was not shifting downwards and hence we needed to add padding manually
						if(Utils.isLollipopOrHigher() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
						{
							findViewById(R.id.parentView).setPadding(0, 0, 0, 0);
						}
						setGravity(mLockPatternView);
						mLockPatternView.setVisibility(View.VISIBLE);
						mLockPinView.setVisibility(View.GONE);
						mLockPatternView.requestFocus();
						hideKeyboard();
						mLockPinView.setText("");
			        	changePasswordSetting.setText(getString(R.string.stealth_set_pin));
			        	if(!isReset)
			        	{
			        		StealthModeManager.getInstance().usePinAsPassword(false);
			        	}
			        	mTextInfo.setText(R.string.stealth_msg_draw_an_unlock_pattern);
					}
				}
			});
        }
        /*
         * LOCK PATTERN VIEW
         */

        switch (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) {
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
        case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
            final int size = getResources().getDimensionPixelSize(
                    R.dimen.alp_42447968_lockpatternview_size);
            LayoutParams lp = mLockPatternView.getLayoutParams();
            lp.width = size;
            lp.height = size;
            mLockPatternView.setLayoutParams(lp);

            break;
        }// LARGE / XLARGE
        }

        /*
         * Haptic feedback.
         */
        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = android.provider.Settings.System
                    .getInt(getContentResolver(),
                            android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,
                            0) != 0;
        } catch (Throwable t) {
            /*
             * Ignore it.
             */
        }
        mLockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);

        mLockPatternView.setInStealthMode(mStealthMode
                && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()));
        mLockPatternView.setOnPatternListener(mLockPatternViewListener);
        if (lastPattern != null && lastDisplayMode != null
                && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()))
            mLockPatternView.setPattern(lastDisplayMode, lastPattern);

        /*
         * COMMAND BUTTONS
         */

        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
            mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
            mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);

            mBtnCancel.setVisibility(View.VISIBLE);
            mFooter.setVisibility(View.VISIBLE);

            if (infoText != null)
                mTextInfo.setText(infoText);
            else
            {
            	//checking whether this was invoked in a normal flow or stealth reset flow.
            	//we use a different string if this was invoked from the reset flow
            	if( getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false))
            	{
                    mTextInfo.setText(StealthModeManager.getInstance().isPinAsPassword()? R.string.stealth_msg_enter_an_unlock_pin : R.string.stealth_msg_draw_an_unlock_pattern);
            	}
            }

            /*
             * BUTTON OK
             */
            if (mBtnOkCmd == null)
                mBtnOkCmd = ButtonOkCommand.CONTINUE;
            switch (mBtnOkCmd) {
            case CONTINUE:
                mBtnConfirm.setText(R.string.CONTINUE);
                break;
            case DONE:
                mBtnConfirm.setText(R.string.CONFIRM);
                break;
            default:
                /*
                 * Do nothing.
                 */
                break;
            }
            if (btnOkEnabled != null)
                mBtnConfirm.setEnabled(btnOkEnabled);
            
            if(mIsRetryBtnVisible != null && mIsRetryBtnVisible)
            {
            	changeCancelToRetry();
            }
        }// ACTION_CREATE_PATTERN
        else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            if (TextUtils.isEmpty(infoText))
            {
            	if(!StealthModeManager.getInstance().isPinAsPassword())
            		mTextInfo.setText(getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false)
                		?R.string.alp_42447968_msg_draw_pattern_to_unlock_in_reset : R.string.alp_42447968_msg_draw_pattern_to_unlock);
            	else
            		mTextInfo.setText(getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false)
                		?R.string.stealth_msg_enter_pin_to_unlock_in_reset : R.string.stealth_msg_enter_pin_to_unlock);
            		
            }
       
            else
                mTextInfo.setText(infoText);
            if (getIntent().hasExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN)) {
                mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);
                mBtnConfirm.setText(R.string.alp_42447968_cmd_forgot_pattern);
                mBtnConfirm.setEnabled(true);
                mFooter.setVisibility(View.VISIBLE);
            }
            mFooter.setVisibility(View.GONE);
        }// ACTION_COMPARE_PATTERN
        else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
				
        	mTextInfo.setText(getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false)?R.string.alp_42447968_msg_redraw_new_pattern_confirm:R.string.stealth_msg_redraw_pattern_to_confirm);
            /*
             * NOTE: EXTRA_PATTERN should hold a char[] array. In this case we
             * use it as a temporary variable to hold a list of Cell.
             */

            final ArrayList<Cell> pattern;
            if (getIntent().hasExtra(EXTRA_PATTERN))
                pattern = getIntent()
                        .getParcelableArrayListExtra(EXTRA_PATTERN);
            else
                getIntent().putParcelableArrayListExtra(
                        EXTRA_PATTERN,
                        pattern = LockPatternUtils
                                .genCaptchaPattern(mCaptchaWiredDots));

            mLockPatternView.setPattern(DisplayMode.Animate, pattern);
        }// ACTION_VERIFY_CAPTCHA
    }// initContentView()

    protected boolean doCheckAndCreatePin(final String pin) {

		if (pin.length() >= mMinWiredDots) 
		{

			if (getIntent().hasExtra(EXTRA_PATTERN)) 
			{
				return Arrays.equals(getIntent().getCharArrayExtra(EXTRA_PATTERN),LockPatternUtils.pinToSha1(pin).toCharArray());
			}
			else
			{
				getIntent().putExtra(EXTRA_PATTERN,LockPatternUtils.pinToSha1(pin).toCharArray());
	
			}
		}

		return false;
	}
    
    protected boolean doComparePin(final String pin) 
    {
    	
    	 char[] currentPattern = getIntent().getCharArrayExtra(EXTRA_PATTERN);
          if (currentPattern != null) 
         {    
                 return Arrays.equals(currentPattern,LockPatternUtils.pinToSha1(pin).toCharArray());
         }
         return false;
   
	}

	/**
     * Compares {@code pattern} to the given pattern (
     * {@link #ACTION_COMPARE_PATTERN}) or to the generated "CAPTCHA" pattern (
     * {@link #ACTION_VERIFY_CAPTCHA}). Then finishes the activity if they
     * match.
     * 
     * @param pattern
     *            the pattern to be compared.
     */
    private void doComparePattern(final List<Cell> pattern) {
         if (pattern == null)
            return;

        /*
         * Use a LoadingDialog because decrypting pattern might take time...
         */

        new LoadingDialog<Void, Void, Boolean>(this, false) {

            @Override
            protected Boolean doInBackground(Void... params) {
                if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                    char[] currentPattern = getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    if (currentPattern == null)
                        currentPattern = Settings.Security
                                .getPattern(LockPatternActivity.this);
                    if (currentPattern != null) {
                        if (mEncrypter != null)
                            return pattern.equals(mEncrypter.decrypt(
                                    LockPatternActivity.this, currentPattern));
                        else
                            return Arrays.equals(currentPattern,
                                    LockPatternUtils.patternToSha1(pattern)
                                            .toCharArray());
                    }
                }// ACTION_COMPARE_PATTERN
                else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                    return pattern.equals(getIntent()
                            .getParcelableArrayListExtra(EXTRA_PATTERN));
                }// ACTION_VERIFY_CAPTCHA

                return false;
            }// doInBackground()

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result)
                    finishWithResultOk(null);
                else {
                    finishWithNegativeResult(RESULT_CANCELED);    
                    mRetryCount++;
                    mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

                    if (mRetryCount >= mMaxRetries)
                        finishWithNegativeResult(RESULT_FAILED);
                    else {
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mTextInfo.setText(R.string.alp_42447968_msg_try_again);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,
                                DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }// onPostExecute()

        }.execute();
    }// doComparePattern()

    /**
     * Checks and creates the pattern.
     * 
     * @param pattern
     *            the current pattern of lock pattern view.
     */
    private void doCheckAndCreatePattern(final List<Cell> pattern) {
        if (pattern.size() < mMinWiredDots) {
            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            mTextInfo.setText(getResources().getQuantityString(
                    R.plurals.alp_42447968_pmsg_connect_x_dots, mMinWiredDots,
                    mMinWiredDots));
            mLockPatternView.postDelayed(mLockPatternViewReloader,
                    DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }

        if (getIntent().hasExtra(EXTRA_PATTERN)) {
            /*
             * Use a LoadingDialog because decrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, Boolean>(this, false) {

                @Override
                protected Boolean doInBackground(Void... params) {
                    if (mEncrypter != null)
                        return pattern.equals(mEncrypter.decrypt(
                                LockPatternActivity.this, getIntent()
                                        .getCharArrayExtra(EXTRA_PATTERN)));
                    else
                        return Arrays.equals(
                                getIntent().getCharArrayExtra(EXTRA_PATTERN),
                                LockPatternUtils.patternToSha1(pattern)
                                        .toCharArray());
                }// doInBackground()

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);

                    if (result) {
                        mTextInfo
                                .setText(R.string.alp_42447968_msg_your_new_unlock_pattern);
                        mBtnConfirm.setEnabled(true);
                    } else {
                        mTextInfo
                                .setText(R.string.alp_42447968_msg_try_again);
                        mBtnConfirm.setEnabled(false);
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,
                                DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }// onPostExecute()

            }.execute();
        } else {
            /*
             * Use a LoadingDialog because encrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, char[]>(this, false) {

                @Override
                protected char[] doInBackground(Void... params) {
                    return mEncrypter != null ? mEncrypter.encrypt(
                            LockPatternActivity.this, pattern)
                            : LockPatternUtils.patternToSha1(pattern)
                                    .toCharArray();
                }// onCancel()

                @Override
                protected void onPostExecute(char[] result) {
                    super.onPostExecute(result);

                    getIntent().putExtra(EXTRA_PATTERN, result);
                    mTextInfo
                            .setText(R.string.alp_42447968_msg_pattern_recorded);
                    mBtnConfirm.setEnabled(true);
                }// onPostExecute()

            }.execute();
        }
    }// doCheckAndCreatePattern()

    /**
     * Finishes activity with {@link Activity#RESULT_OK}.
     * 
     * @param pattern
     *            the pattern, if this is in mode creating pattern. In any
     *            cases, it can be set to {@code null}.
     */
    private void finishWithResultOk(char[] pattern) {
    	if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
    	{
    		mIntentResult.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET,getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false));
    		mIntentResult.putExtra(EXTRA_PATTERN, pattern);
    	}
    	else if(ACTION_COMPARE_PATTERN.equals(getIntent().getAction()))
        {
            /*
             * If the user was "logging in", minimum try count can not be zero.
             */
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount + 1);
        }

    	Bundle stealthBundle = getIntent().getBundleExtra(HikeConstants.STEALTH);
    	if(stealthBundle!=null)
    	{
    		mIntentResult.putExtras(stealthBundle);
    	}

    	setResult(RESULT_OK, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle bundle = new Bundle();
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
                bundle.putCharArray(EXTRA_PATTERN, pattern);
            else {
                /*
                 * If the user was "logging in", minimum try count can not be
                 * zero.
                 */
                bundle.putInt(EXTRA_RETRY_COUNT, mRetryCount + 1);
            }
            receiver.send(RESULT_OK, bundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_OK);
        if (pi != null) {
            try {
                pi.send(this, RESULT_OK, mIntentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
        overridePendingTransition(0, 0);
    }// finishWithResultOk()

    /**
     * Finishes the activity with negative result (
     * {@link Activity#RESULT_CANCELED}, {@link #RESULT_FAILED} or
     * {@link #RESULT_FORGOT_PATTERN}).
     */
    private void finishWithNegativeResult(int resultCode) {
    	mIntentResult.putExtra(HikeConstants.Extras.STEALTH_PASS_RESET,getIntent().getBooleanExtra(HikeConstants.Extras.STEALTH_PASS_RESET, false));
    	if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

        setResult(resultCode, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, mRetryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_CANCELLED);
        if (pi != null) {
            try {
                pi.send(this, resultCode, mIntentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
        overridePendingTransition(0, 0);
    }// finishWithNegativeResult()

    /*
     * LISTENERS
     */

    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(DisplayMode.Correct);
            changeCancelToRetry();
            mBtnCancel.setEnabled(false);

            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_release_finger_when_done);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE)
                    getIntent().removeExtra(EXTRA_PATTERN);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                mTextInfo
                        .setText(R.string.stealth_msg_redraw_pattern_to_confirm);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternStart()

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                doCheckAndCreatePattern(pattern);
                mBtnCancel.setEnabled(true);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                doComparePattern(pattern);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                if (!DisplayMode.Animate.equals(mLockPatternView
                        .getDisplayMode()))
                    doComparePattern(pattern);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternDetected()

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            changeRetryToCancel();
            mBtnCancel.setEnabled(true);

            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    getIntent().removeExtra(EXTRA_PATTERN);
                    mTextInfo
                            .setText(mLockPinView.getVisibility() == View.VISIBLE ? R.string.stealth_msg_enter_an_unlock_pin : R.string.stealth_msg_draw_an_unlock_pattern);
                } else
                    mTextInfo
                            .setText(mLockPinView.getVisibility() == View.VISIBLE ? R.string.stealth_msg_reenter_pin_to_confirm: R.string.stealth_msg_redraw_pattern_to_confirm);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mTextInfo
                        .setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                mTextInfo
                        .setText(R.string.stealth_msg_redraw_pattern_to_confirm);
                List<Cell> pattern = getIntent().getParcelableArrayListExtra(
                        EXTRA_PATTERN);
                mLockPatternView.setPattern(DisplayMode.Animate, pattern);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternCleared()

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {
            // TODO Auto-generated method stub
        }// onPatternCellAdded()
    };// mLockPatternViewListener

    private final View.OnClickListener mBtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
        	getIntent().removeExtra(EXTRA_PATTERN);
            finishWithNegativeResult(RESULT_CANCELED);
        }// onClick()
    };// mBtnCancelOnClickListener
    
    private final View.OnClickListener mBtnRetryOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
        	mLockPatternViewReloader.run();
        	if (mCustomKeyboard != null)
            {
                mCustomKeyboard.updateCore();
            }
        }// onClick()
    };

    private final void changeCancelToRetry()
    {
    	if (mBtnOkCmd == ButtonOkCommand.CONTINUE) 
    	{
    		mIsRetryBtnVisible = true;
    		mBtnCancel.setText(R.string.RETRY);
    		mBtnCancel.setOnClickListener(mBtnRetryOnClickListener);
    	}
    }
    
    private final void changeRetryToCancel()
    {
    	if (mBtnOkCmd == ButtonOkCommand.CONTINUE) 
    	{
    		mIsRetryBtnVisible = false;
    		mBtnCancel.setText(R.string.CANCEL);
    		mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
    	}
    }
    
    private void setAlphaForView(View v, float alpha) {
		AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
		animation.setDuration(0); 
		animation.setFillAfter(true); 
		v.startAnimation(animation);
	}
    
    private final View.OnClickListener mBtnConfirmOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    doCheckAndCreatePin(mLockPinView.getText().toString());
                	changeRetryToCancel();
                	changePasswordSetting.setEnabled(false);
                	setAlphaForView(changePasswordSetting, 0.0f);
                    mBtnOkCmd = ButtonOkCommand.DONE;
                    mLockPatternView.clearPattern();
                    mLockPinView.setText("");
                    if(mLockPinView.getVisibility() == View.VISIBLE)
                    {
                    	if (mCustomKeyboard != null)
                        {
                            mCustomKeyboard.updateCore();
                        }
                    	mTextInfo.setText(R.string.stealth_msg_reenter_pin_to_confirm);   	
                    } else {
                    	mTextInfo.setText(R.string.stealth_msg_redraw_pattern_to_confirm);
                    }
                    mBtnConfirm.setText(R.string.CONFIRM);
                    mBtnConfirm.setEnabled(false);
                    mIsRetryBtnVisible = null;
                } else {
                    final char[] pattern = getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    StealthModeManager.getInstance().usePinAsPassword(mLockPatternView.getVisibility() != View.VISIBLE && mLockPinView.getVisibility() == View.VISIBLE);
                    if (mAutoSave)
                        Settings.Security.setPattern(LockPatternActivity.this,
                                pattern);
                    finishWithResultOk(pattern);
                }
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                /*
                 * We don't need to verify the extra. First, this button is only
                 * visible if there is this extra in the intent. Second, it is
                 * the responsibility of the caller to make sure the extra is
                 * good.
                 */
                PendingIntent pi = null;
                try {
                    pi = getIntent().getParcelableExtra(
                            EXTRA_PENDING_INTENT_FORGOT_PATTERN);
                    pi.send();
                } catch (Throwable t) {
                    Log.e(CLASSNAME, "Error sending pending intent: " + pi, t);
                }
                finishWithNegativeResult(RESULT_FORGOT_PATTERN);
            }// ACTION_COMPARE_PATTERN
        }// onClick()
    };// mBtnConfirmOnClickListener

    /**
     * This reloads the {@link #mLockPatternView} after a wrong pattern.
     */
    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPinView.setText("");
            mLockPatternViewListener.onPatternCleared();
        }// run()
    };// mLockPatternViewReloader
    
    protected void onStop() {
    	finishWithNegativeResult(RESULT_CANCELED);
    	super.onStop();
    };

    private void initCustomKeyboard()
    {
    	View keyboardHolder = (LinearLayout) findViewById(R.id.keyboardView_holder);
        mCustomKeyboard = new CustomKeyboard(LockPatternActivity.this, keyboardHolder);
        mCustomKeyboard.registerEditText(R.id.alp_42447968_lock_pin, KPTConstants.SINGLE_LINE_EDITOR, this, this);
        mCustomKeyboard.init(mLockPinView);
        mLockPinView.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				if (mCustomKeyboard.isCustomKeyboardVisible())
				{
					return;
				}
				mCustomKeyboard.showCustomKeyboard(mLockPinView, true);
			}
		});
    }

	@Override
	public void analyticalData(String arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputViewCreated()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputviewVisbility(boolean arg0, int arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showGlobeKeyView()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showQuickSettingView()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtFocusChange(View arg0, boolean arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtTouch(View arg0, MotionEvent arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtclick(View arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReturnAction(int arg0)
	{
		// TODO Auto-generated method stub
		
	}
	
	private void showKeyboard()
	{
		if (systemKeyboard)
		{
			mLockPinView.setFocusable(true);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			Utils.showSoftKeyboard(mLockPinView, InputMethodManager.SHOW_FORCED);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(mLockPinView, true);
		}
	}
	
	private void hideKeyboard()
	{
		if (systemKeyboard)
		{
			Utils.hideSoftKeyboard(LockPatternActivity.this, mLockPinView);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(mLockPinView, false);
		}
	}
	
	private void setGravity(View view)
	{
		LinearLayout viewToSet = (LinearLayout) findViewById(R.id.parentView);
		RelativeLayout.LayoutParams lp =  (android.widget.RelativeLayout.LayoutParams) viewToSet.getLayoutParams();
		LinearLayout kbdView = (LinearLayout) findViewById(R.id.keyboardView_holder);
		RelativeLayout.LayoutParams lpKbd = (android.widget.RelativeLayout.LayoutParams) kbdView.getLayoutParams();
		
		lpKbd.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		
		if (view instanceof LockPatternView)
		{
			lp.addRule(RelativeLayout.CENTER_IN_PARENT);
		}
		else
		{
			if (Utils.isLollipopOrHigher() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
    		{
    			findViewById(R.id.parentView).setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.bottom_padding_pin_view));
    		}
			lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		}
		viewToSet.setLayoutParams(lp);
	}
	
	@Override
	protected void onDestroy()
	{
		KptUtils.destroyKeyboardResources(mCustomKeyboard, R.id.alp_42447968_lock_pin);
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		KptUtils.pauseKeyboardResources(mCustomKeyboard, mLockPinView);
		super.onPause();
	}
	
	@Override
	public void onBackPressed()
	{
		if (mCustomKeyboard != null && mCustomKeyboard.isCustomKeyboardVisible())
		{
			mCustomKeyboard.showCustomKeyboard(mLockPinView, false);
			return;
		}
		super.onBackPressed();
	}
}
