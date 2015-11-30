package com.bsb.hike.timeline.view;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.media.EmoticonPicker;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.media.ImageParser.ImageParserListener;
import com.bsb.hike.media.PopupListener;
import com.bsb.hike.modules.kpt.HikeCustomKeyboard;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.modules.kpt.KptUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.RoundedImageView;
import com.kpt.adaptxt.beta.KPTAddonItem;
import com.kpt.adaptxt.beta.RemoveDialogData;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class StatusUpdate extends HikeAppStateBaseFragmentActivity implements Listener, OnSoftKeyboardListener, PopupListener, View.OnClickListener,
		AdaptxtKeyboordVisibilityStatusListner
{

	private class ActivityTask
	{
		int moodId = -1;

		int moodIndex = -1;

		StatusUpdateTask task;

		boolean emojiShowing = false;

		boolean moodShowing = false;
		
		boolean keyboardShowing = false;

		boolean imageDeleted = false;
	}
	
	private HikeCustomKeyboard mCustomKeyboard;
	
	private boolean systemKeyboard;

	private ActivityTask mActivityTask;

	private static final String TAG = "statusupdate";
	
	private ProgressDialog progressDialog;

	private String[] pubsubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED, HikePubSub.STATUS_POST_REQUEST_DONE };

	private ViewGroup moodParent;

	private RoundedImageView avatar;

	private CustomFontEditText statusTxt;

	private CustomLinearLayout parentLayout;

	private View doneBtn;

	private TextView postText;

	private ImageView arrow;
	
	private EmoticonPicker mEmoticonPicker;
	
	private static final int SHOW_EMOJI_PALETTE = 1;
	
	private boolean wasEmojiPreviouslyVisible;
	
	private String IS_IMAGE_DELETED = "is_img_d";

	private String SELECTED_MOOD_ID = "mId";

	private String SELECTED_MOOD_INDEX = "smIdx";

	private int keyboardHeight;

	private String INPUT_INTENT = "ip_in";

	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			/**
			 * Defensive check
			 */
			if (msg == null)
			{
				Logger.e(TAG, "Getting a null message in chat thread");
				return;
			}
			handlingMessage(msg);
		}
	};

	private String mImagePath;

	private ImageView statusImage;
	
	protected void handlingMessage(android.os.Message msg)
	{
		switch (msg.what)
		{
		case SHOW_EMOJI_PALETTE:
			showEmoticonPicker();
			break;
		default:
			Logger.d(TAG, "Did not find any matching event for msg.what : " + msg.what);
			break;
		}
	}
	

	private IconLoader mIconImageLoader;

	private ImageButton btnRemovePhoto;

	private View addMoodLayout;

	private boolean isForeground;

	public static final String STATUS_UPDATE_IMAGE_PATH = "SUIMGPTH";
	
	StatusUpdateTaskFinishedRunnable suUploadTaskFinishRunnable;

	private View addPhotoLayout;

	private View addItemsLayout;

	private String mInputIntentData;

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (wasEmojiPreviouslyVisible)
		{
			mActivityTask.emojiShowing = true;
		}
		return mActivityTask;
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.status_dialog);

		Object o = getLastCustomNonConfigurationInstance();

		if (o instanceof ActivityTask)
		{
			mActivityTask = (ActivityTask) o;
			if (mActivityTask.task != null)
			{
				progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
			}
		}
		else
		{
			mActivityTask = new ActivityTask();
		}

		initVarRef();
		
		initEmoticonPicker();
		
		systemKeyboard = HikeMessengerApp.isSystemKeyboard();
		mEmoticonPicker.setCustomKeyBoard(!systemKeyboard);
		if (!systemKeyboard)
		{
			initCustomKeyboard();			
		}
		
		addOnClickListeners();

		RoundedImageView roundAvatar = (RoundedImageView) avatar;
		roundAvatar.setOval(true);

		setupActionBar();

		if(savedInstanceState != null)
		{
			mActivityTask.imageDeleted = savedInstanceState.getBoolean(IS_IMAGE_DELETED);
			
			int savedMoodId = savedInstanceState.getInt(SELECTED_MOOD_ID, -1);
			if (savedMoodId != -1)
			{
				int savedMoodIndex = savedInstanceState.getInt(SELECTED_MOOD_INDEX, -1);
				setMood(savedMoodId, savedMoodIndex);
			}
			
			mImagePath = savedInstanceState.getString(STATUS_UPDATE_IMAGE_PATH);
			if (!TextUtils.isEmpty(mImagePath))
			{
				addPhoto(mImagePath);
			}
		}
		else
		{
			readArguments(getIntent());			
		}
		
		mIconImageLoader = new IconLoader(getApplicationContext(), getApplicationContext().getResources().getDimensionPixelSize(R.dimen.icon_picture_size));

		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(false);

		mIconImageLoader.setDefaultDrawableNull(false);

		String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

		avatar.setImageDrawable(HikeMessengerApp.getLruCache().getDefaultAvatar(selfMsisdn, false));

		mIconImageLoader.loadImage(selfMsisdn, avatar, false, true, false);

		parentLayout.setOnSoftKeyboardListener(this);

		if (!TextUtils.isEmpty(mImagePath) && !mActivityTask.imageDeleted)
		{
			Bitmap bmp = HikeBitmapFactory.decodeFile(mImagePath);
			if(bmp == null)
			{
				removePhoto(null);
				Toast.makeText(getApplicationContext(), R.string.photos_oom_load, Toast.LENGTH_SHORT).show();
			}
			else
			{
				statusImage.setImageBitmap(bmp);
				statusTxt.setHint(R.string.status_hint_image);
				ChatThreadUtils.applyMatrixTransformationToImageView(statusImage.getDrawable(), statusImage);
			}
			
		}
		else
		{
			removePhoto(null);
		}

		setMood(mActivityTask.moodId, mActivityTask.moodIndex);

		statusTxt.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				toggleEnablePostButton();
			}
		});

		statusTxt.addTextChangedListener(new EmoticonTextWatcher());
		
		if (mActivityTask.emojiShowing)
		{
			sendUIMessage(SHOW_EMOJI_PALETTE, null);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		else if (mActivityTask.moodShowing)
		{
			showMoodSelector();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		else if (TextUtils.isEmpty(mImagePath) || mActivityTask.keyboardShowing)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubsubListeners);

		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STATUS.ordinal());

		refreshLayouts();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		outState.putBoolean(IS_IMAGE_DELETED, mActivityTask.imageDeleted);
		outState.putString(STATUS_UPDATE_IMAGE_PATH, mImagePath);
		outState.putInt(SELECTED_MOOD_ID, mActivityTask.moodId);
		outState.putInt(SELECTED_MOOD_INDEX, mActivityTask.moodIndex);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		showKeyboard(false);
		isForeground = true;
		if (statusImage != null && statusImage.getDrawable() != null)
		{
			ChatThreadUtils.applyMatrixTransformationToImageView(statusImage.getDrawable(), statusImage);
		}

		if (suUploadTaskFinishRunnable != null)
		{
			runOnUiThread(suUploadTaskFinishRunnable);
			suUploadTaskFinishRunnable = null;
		}
	}
	
	private void readArguments(Intent intent)
	{
		if(intent == null)
		{
			// In this case, there will be no image post present. Rest functional.
			return;
		}
		mImagePath = intent.getStringExtra(STATUS_UPDATE_IMAGE_PATH);
		mInputIntentData = intent.toUri(Intent.URI_INTENT_SCHEME);
	}

	/**
	 * Initialize variables and references
	 */
	public void initVarRef()
	{
		moodParent = (ViewGroup) findViewById(R.id.mood_parent_status);
		avatar = (RoundedImageView) findViewById(R.id.avatar);
		statusTxt = (CustomFontEditText) findViewById(R.id.status_txt);
		parentLayout = (CustomLinearLayout) findViewById(R.id.parent_layout);
		statusImage = (ImageView) findViewById(R.id.status_image);
		btnRemovePhoto = (ImageButton) findViewById(R.id.btn_remove_photo);
		addMoodLayout = findViewById(R.id.addMoodLayout);
		addPhotoLayout = findViewById(R.id.addPhotoLayout);
		addItemsLayout = findViewById(R.id.addItemsLayout);
		ScrollView sv = (ScrollView)findViewById(R.id.scroll);
        sv.setEnabled(false);
	}
	
	private void initCustomKeyboard()
	{
		View keyboardHolder = (LinearLayout) findViewById(R.id.keyboardView_holder);
		mCustomKeyboard = new HikeCustomKeyboard(StatusUpdate.this, keyboardHolder, KPTConstants.MULTILINE_LINE_EDITOR, null, this);
		mCustomKeyboard.registerEditText(R.id.status_txt);
		mCustomKeyboard.init(statusTxt);
		findViewById(R.id.status_txt).setOnClickListener(this);
		mEmoticonPicker.setCustomKeyBoardHeight((keyboardHeight == 0) ? mCustomKeyboard.getKeyBoardAndCVHeight() : keyboardHeight);
		if (!(getIntent().hasExtra(STATUS_UPDATE_IMAGE_PATH)))
		{
			mCustomKeyboard.showCustomKeyboard(statusTxt, true);			
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, (keyboardHeight == 0) ? mCustomKeyboard.getKeyBoardAndCVHeight() : keyboardHeight);
		}
	}

	@Override
	protected void onStop()
	{
		releaseEmoticon();

		super.onStop();


		if (mActivityTask.moodShowing || mActivityTask.emojiShowing)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.status);
		doneBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		postText.setText(R.string.post);

		doneBtn.setVisibility(View.VISIBLE);

		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				postStatus();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}

	public void onTitleIconClick(View v)
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			hideEmojiOrMoodLayout();
		}
		else
		{
			postStatus();
		}
	}

	public void onEmojiClick()
	{
		if (mActivityTask.emojiShowing)
		{
			mActivityTask.emojiShowing = false;
			hideEmojiOrMoodLayout();
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, 0);
			setEmoticonButtonSelected(false);
		}
		else
		{
			showEmoticonPicker();
		}
	}
	
	public void showEmoticonPicker()
	{
		wasEmojiPreviouslyVisible = false;
		hideEmojiOrMoodLayout();
		if (mEmoticonPicker.showEmoticonPicker(getResources().getConfiguration().orientation))
		{
			mActivityTask.emojiShowing = true;
			hideKeyboard();
			showCancelButton(false);
			setEmoticonButtonSelected(true);
		}
		else
		{
			if (!retryToInflateEmoticons())
			{
				setEmoticonButtonSelected(false);
				mActivityTask.emojiShowing = false;
				Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Got a failure while opening emoticon pallete possibly due to null context, mainView is null or mainView.getWindowToken() is null (this happens during
	 * device orientation change)
	 * @return
	 */
	private boolean retryToInflateEmoticons()
	{
		String errorMsg = "Inside method : retry to inflate emoticons. Houston!, something's not right here";
		HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
		mEmoticonPicker = null;
		initEmoticonPicker();
		mActivityTask.emojiShowing = true;
		return	mEmoticonPicker.showEmoticonPicker(getResources().getConfiguration().orientation);
	}
	
	protected void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}
	
	public void onMoodClick(View v)
	{
		if (mEmoticonPicker!= null && mEmoticonPicker.isShowing())
		{
			mActivityTask.emojiShowing = false;
			hideEmojiOrMoodLayout();
		}
		showMoodSelector();
	}

	public void onPhotoClick(View v)
	{
		hideEmojiOrMoodLayout();
		
		Utils.hideSoftKeyboard(getApplicationContext(), getWindow().getDecorView());
		
		int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE | GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE
				| GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

		Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(StatusUpdate.this, galleryFlags, Utils.getNewImagePostFilePath());
		startActivityForResult(galleryPickerIntent, UpdatesFragment.TIMELINE_POST_IMAGE_REQ);
	}
	
	public void removePhoto(View dontUseThis)
	{
		btnRemovePhoto.setVisibility(View.GONE);
		statusImage.setImageResource(0);
		statusImage.setVisibility(View.GONE);
		if (mActivityTask.moodId == -1)
		{
			statusTxt.setHint(R.string.status_hint);
		}
		mActivityTask.imageDeleted = true;
		mImagePath = null;
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		refreshLayouts();
	}
	
	public void addPhoto(String imagePath)
	{
		mImagePath = imagePath;
		Bitmap bmp = HikeBitmapFactory.decodeFile(mImagePath);
		if (bmp == null)
		{
			removePhoto(null);
			Toast.makeText(getApplicationContext(), R.string.photos_oom_load, Toast.LENGTH_SHORT).show();
		}
		else
		{
			statusImage.setVisibility(View.VISIBLE);
			BitmapDrawable bmpDrawable = new BitmapDrawable(getResources(), bmp);
			statusImage.setImageDrawable(bmpDrawable);
			ChatThreadUtils.applyMatrixTransformationToImageView(bmpDrawable, statusImage);
			statusImage.invalidate();
			
			if(mActivityTask.moodId == -1)
			{
				statusTxt.setHint(R.string.status_hint_image);
			}
			
			btnRemovePhoto.setVisibility(View.VISIBLE);
			mActivityTask.imageDeleted = false;
 		}
		
		refreshLayouts();
	}

	@Override
	public void onBackPressed()
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			hideEmojiOrMoodLayout();
		}
		else if(mCustomKeyboard!=null && mCustomKeyboard.isCustomKeyboardVisible())
		{
			hideKeyboard();
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	public void actionBarBackPressed()
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			if (moodParent.getVisibility() == View.VISIBLE)
			{
				hideEmojiOrMoodLayout();
			}
			else
			{
				super.onBackPressed();
			}
		}
		else
		{
			super.onBackPressed();
		}
	}

	private boolean isEmojiOrMoodLayoutVisible()
	{
		return ((moodParent.getVisibility() == View.VISIBLE) || mActivityTask.emojiShowing);
	}

	private void hideEmojiOrMoodLayout()
	{
		if (moodParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.moodShowing = false;
			moodParent.setVisibility(View.GONE);
		}
		else if (mEmoticonPicker != null && mEmoticonPicker.isShowing())
		{
			mActivityTask.emojiShowing = false;
			mEmoticonPicker.dismiss();
			setEmoticonButtonSelected(false);
		}
		
		refreshLayouts();
	}
	
	private void refreshLayouts()
	{
		toggleEnablePostButton();

		if (shouldShowMoodsButton())
		{
			addMoodLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			addMoodLayout.setVisibility(View.GONE);
		}

		if (shouldShowAddPhotoButton())
		{
			addPhotoLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			addPhotoLayout.setVisibility(View.GONE);
		}
		
		if((!shouldShowAddPhotoButton() && !shouldShowMoodsButton()))
		{
			addItemsLayout.setVisibility(View.GONE);
		}
		else if(!isEmojiOrMoodLayoutVisible())
		{
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

			p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

			addItemsLayout.setLayoutParams(p);
			
			addItemsLayout.setVisibility(View.VISIBLE);
		}
	}

	private void postStatus()
	{
		String status = null;
		/*
		 * If the text box is empty, the we take the hint text which is a prefill for moods.
		 */
		if (TextUtils.isEmpty(statusTxt.getText()) && mActivityTask.moodId != -1)
		{
			status = statusTxt.getHint().toString();
		}
		else
		{
			status = statusTxt.getText().toString();
		}

		try
		{
			mActivityTask.task = new StatusUpdateTask(status, mActivityTask.moodId, mImagePath);
			addLanguageAnalytics();
		}
		catch (IOException e)
		{
			Toast.makeText(getApplicationContext(), R.string.could_not_post_pic, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		}
		
		if(mActivityTask.task != null)
		{
			mActivityTask.task.execute();

			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
		}
		
	}

	protected void addLanguageAnalytics()
	{
		JSONObject metadata = new JSONObject();
		try 
		{
			metadata.put(HikeConstants.LogEvent.KPT, KptKeyboardManager.getInstance(StatusUpdate.this).getCurrentLanguageAddonItem().getlocaleName());
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void showMoodSelector()
	{
		hideKeyboard();

//		addItemsLayout.setVisibility(View.GONE);

		mActivityTask.moodShowing = true;

		showCancelButton(true);

		moodParent.setClickable(true);
		GridView moodPager = (GridView) findViewById(R.id.mood_pager);

		parentLayout.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (moodParent != null)
				{
					moodParent.setVisibility(View.VISIBLE);

					RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

					p.addRule(RelativeLayout.ABOVE, R.id.mood_parent_status);

					addItemsLayout.setLayoutParams(p);
				}
			}
		}, 300); // TODO Remove hack. Use Shareable popup layout

		boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		int columns = portrait ? 4 : 6;

		moodPager.setNumColumns(columns);
		MoodAdapter moodAdapter = new MoodAdapter(this, columns);
		moodPager.setAdapter(moodAdapter);
		moodPager.setOnItemClickListener(moodAdapter);
	}

	public void setMood(int moodId, int moodIndex)
	{
		if (moodId == -1)
		{
			resetMood();
			return;
		}
		mActivityTask.moodId = moodId;
		mActivityTask.moodIndex = moodIndex;

		avatar.setImageResource(EmoticonConstants.moodMapping.get(moodId));
		avatar.setOval(false);

		String[] moodsArray = getResources().getStringArray(R.array.mood_headings);
		statusTxt.setHint(moodsArray[moodIndex]);
		
		toggleEnablePostButton();
	}
	
	private void resetMood()
	{
		mActivityTask.moodId = -1;
		mActivityTask.moodIndex = -1;
		mIconImageLoader.loadImage(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null), avatar, false, true, false);
		avatar.setOval(true);
	}

	private void showCancelButton(boolean moodLayout)
	{
		if (moodLayout)
		{
		}
	}

	@Override
	public void onPopupDismiss()
	{
		if(mActivityTask.emojiShowing)
		{
			wasEmojiPreviouslyVisible = true;
			mActivityTask.emojiShowing = false;
		}
	}
	
	public void toggleEnablePostButton()
	{
		/*
		 * Enabling if the text length is > 0 or if the user has selected a mood with some prefilled text.
		 */
		boolean enable = mActivityTask.moodId >= 0 || statusTxt.getText().toString().trim().length() > 0 || !TextUtils.isEmpty(mImagePath);
		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, enable);
	}

	@Override
	public void onEventReceived(final String type, Object object)
	{
		if (HikePubSub.STATUS_POST_REQUEST_DONE.equals(type))
		{
			final boolean statusPosted = (Boolean) object;

			suUploadTaskFinishRunnable = new StatusUpdateTaskFinishedRunnable(statusPosted);

			if (isForeground)
			{
				runOnUiThread(suUploadTaskFinishRunnable);
				suUploadTaskFinishRunnable = null;
			}
		}
	}
	
	class StatusUpdateTaskFinishedRunnable implements Runnable
	{
		private boolean mStatusPosted;

		public StatusUpdateTaskFinishedRunnable(boolean statusPosted)
		{
			mStatusPosted = statusPosted;
		}
		
		@Override
		public void run()
		{
			mActivityTask.task = null;
			if (progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}
			if (mStatusPosted)
			{
				Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
				Intent in = new Intent(StatusUpdate.this, TimelineActivity.class);
				in.putExtra(HikeConstants.HikePhotos.HOME_ON_BACK_PRESS, true);
				StatusUpdate.this.startActivity(in);
				StatusUpdate.this.finish();
			}
			else
			{
				Toast.makeText(getApplicationContext(), R.string.update_status_fail, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onDestroy()
	{
		KptUtils.destroyKeyboardResources(mCustomKeyboard, R.id.status_txt);
		
		/*
		 * We need to unregister all pubsublisteners whenever activity gets destroyed. Otherwise reference to this activity gets attached with our HikeMessengerApp which doesn't
		 * let GC pick up any instance of this activity. So whenever this activity gets destroyed its instance doesn't get cleared from heap.
		 */
		HikeMessengerApp.getPubSub().removeListeners(this, pubsubListeners);
		super.onDestroy();
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
		
	}

	@Override
	protected void onPause()
	{
		KptUtils.pauseKeyboardResources(mCustomKeyboard);
		KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, 0);
		Utils.hideSoftKeyboard(getApplicationContext(), statusTxt);
		isForeground = false;
		super.onPause();
	}
	
	@Override
	public void onShown()
	{
		mActivityTask.keyboardShowing = true;
		hideEmojiOrMoodLayout();
		Logger.d(StatusUpdate.class.getSimpleName(), "shown keyboard");
	}

	@Override
	public void onHidden()
	{
		mActivityTask.keyboardShowing = false;
		Logger.d(StatusUpdate.class.getSimpleName(), "hidden keyboard");
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.emoji_btn:
			setEmoticonButtonSelected(true);
			if (mCustomKeyboard != null)
			{
				mEmoticonPicker.setCustomKeyBoardHeight((keyboardHeight == 0) ? mCustomKeyboard.getKeyBoardAndCVHeight() : keyboardHeight);
			}
			onEmojiClick();
			break;
		case R.id.status_txt:
			setEmoticonButtonSelected(false);
			showKeyboard(true);
			break;
		default:
			Logger.e(TAG, "onClick Registered but not added in onClick : " + v.toString());
			break;
		}
	}
	
	private void addOnClickListeners()
	{
		findViewById(R.id.emoji_btn).setOnClickListener(this);
	}
	
	private void initEmoticonPicker()
	{
		int[] dontEatThisTouch = {R.id.emoji_btn, R.id.status_txt};
		if (mEmoticonPicker == null)
		{
			mEmoticonPicker = new EmoticonPicker(this, statusTxt, findViewById(R.id.parent_layout), (int)getResources().getDimension(R.dimen.emoticon_pallete), dontEatThisTouch);
		}
		mEmoticonPicker.setOnDismissListener(this);
		mEmoticonPicker.setDisableExtraPadding(false);
		mEmoticonPicker.useStatusUpdateEmojisList(true);
	}
	
	private void setEmoticonButtonSelected(boolean selected)
	{
		findViewById(R.id.emoji_btn).setSelected(selected);
	}
	
	private void releaseEmoticon()
	{
		if (mEmoticonPicker.isShowing())
		{
			hideEmojiOrMoodLayout();
		}
		/**
		 * It is important that along with releasing resources for Emoticons, we also close its window to prevent any BadWindow Exceptions later on.
		 */
		if (mEmoticonPicker != null)
		{
			mEmoticonPicker.releaseReources();
		}
	}

	private boolean shouldShowMoodsButton()
	{
		return true;
	}
	
	private boolean shouldShowAddPhotoButton()
	{
		return true;
	}
	
	@Override
	protected void onStart()
	{
		initEmoticonPicker();
		super.onStart();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);
		if (mEmoticonPicker!=null)
		{
			
			if (mEmoticonPicker.isShowing())
			{
				int currentEmoticonPage = mEmoticonPicker.getCurrentItem();
				hideEmojiOrMoodLayout();
				initEmoticonPicker();
				mEmoticonPicker.setCurrentItem(currentEmoticonPage);
				mEmoticonPicker.onOrientationChange(newConfig.orientation);
				mActivityTask.emojiShowing = true;
			}
			else if (mActivityTask.moodShowing)
			{
				moodParent.setVisibility(View.GONE);
				showMoodSelector();
			}
			else
			{
				hideEmojiOrMoodLayout();
				initEmoticonPicker();
			}
		}
	}

	@Override
	public String toString()
	{
		return "StatusUpdate [statusTxt=" + statusTxt + "mImagePath=" + mImagePath + ", statusImage=" + statusImage + "]";
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId()==android.R.id.home)
		{
			hideKeyboard();
			actionBarBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void analyticalData(KPTAddonItem kptAddonItem)
	{
		KptUtils.generateKeyboardAnalytics(kptAddonItem);
	}

	@Override
	public void onInputViewCreated()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputviewVisbility(boolean visible, int height)
	{
		if (visible)
		{
			mActivityTask.keyboardShowing = true;
			hideEmojiOrMoodLayout();
			keyboardHeight = height;
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, (keyboardHeight == 0) ? mCustomKeyboard.getKeyBoardAndCVHeight() : keyboardHeight);
			Logger.d(StatusUpdate.class.getSimpleName(), "shown keyboard");
		}
		else
		{
			mActivityTask.keyboardShowing = false;
			Logger.d(StatusUpdate.class.getSimpleName(), "hidden keyboard");
		}
	}

	@Override
	public void showGlobeKeyView()
	{
		KptUtils.onGlobeKeyPressed(StatusUpdate.this, mCustomKeyboard);
	}

	@Override
	public void showQuickSettingView()
	{
		KptUtils.onGlobeKeyPressed(StatusUpdate.this, mCustomKeyboard);
	}
	
	private void hideKeyboard()
	{

		if (systemKeyboard)
		{
			Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(statusTxt, false);
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, 0);
		}
	}

	@Override
	public void dismissRemoveDialog() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showRemoveDialog(RemoveDialogData arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private void showKeyboard(boolean directClick)
	{
		if (!(getIntent().hasExtra(STATUS_UPDATE_IMAGE_PATH))||directClick)
		{
			if (systemKeyboard)
			{
				statusTxt.setFocusable(true);
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
				Utils.showSoftKeyboard(statusTxt, InputMethodManager.SHOW_FORCED);
			}
			else
			{
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
				mCustomKeyboard.showCustomKeyboard(statusTxt, true);
				hideEmojiOrMoodLayout();
				KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, (keyboardHeight == 0) ? mCustomKeyboard.getKeyBoardAndCVHeight() : keyboardHeight);
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == Activity.RESULT_CANCELED)
		{
			return;
		}

		switch (requestCode)
		{
		case UpdatesFragment.TIMELINE_POST_IMAGE_REQ:
			ImageParser.parseResult(StatusUpdate.this, resultCode, data, new ImageParserListener()
			{
				@Override
				public void imageParsed(String imagePath)
				{
					addPhoto(imagePath);
				}

				@Override
				public void imageParsed(Uri uri)
				{
					// Do nothing
				}

				@Override
				public void imageParseFailed()
				{
					// Do nothing
				}
			}, false);
			break;

		default:
			break;
		}
	}
	
}