package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.gallery.GalleryItemClickHandler;
import com.bsb.hike.gallery.GalleryItemLoaderTask;
import com.bsb.hike.gallery.GalleryItemLoaderTask.GalleryItemLoaderImp;
import com.bsb.hike.gallery.GalleryRecyclerAdapter;
import com.bsb.hike.gallery.MarginDecoration;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.ParcelableSparseArray;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.chatthread.ChatThreadActivity;

public class GalleryActivity extends HikeAppStateBaseFragmentActivity implements GalleryItemLoaderImp
{

	public static final String DISABLE_MULTI_SELECT_KEY = "en_mul_sel";

	public static final String FOLDERS_REQUIRED_KEY = "fold_req";

	public static final String ENABLE_CAMERA_PICK = "cam_pk";

	public static final int GALLERY_ACTIVITY_RESULT_CODE = 97;

	public static final int GALLERY_CROP_IMAGE = 128;
	
	public static final int GALLERY_CROP_FOR_DP_IMAGE = 64;
	
	public static final int GALLERY_ALLOW_MULTISELECT = 32;
	
	public static final int GALLERY_CATEGORIZE_BY_FOLDERS = 16;
	
	public static final int GALLERY_DISPLAY_CAMERA_ITEM = 8;
	
	public static final int GALLERY_EDIT_SELECTED_IMAGE = 4;
	
	public static final int GALLERY_COMPRESS_EDITED_IMAGE = 2;
	
	public static final int GALLERY_FOR_PROFILE_PIC_UPDATE = 1;
	
	private List<GalleryItem> galleryItemList;

	private boolean isInsideAlbum;

	private String msisdn;

	private boolean foldersRequired;

	private boolean multiSelectMode;

	private ArrayList<GalleryItem> selectedGalleryItems;

	private TextView multiSelectTitle;

	private String albumTitle;

	public static final String START_FOR_RESULT = "startForResult";

	/**
	 * This flag indicates whether this was opened for result or not, i.e. was it startActivityForResult
	 */
	private boolean sendResult;

	private boolean disableMultiSelect;

	public static final String ALL_IMAGES_BUCKET_NAME = "All photos";

	public static final String CAMERA_TILE = "gallery_tile_camera";

	public static final String NEW_PHOTO = "New photo";

	private boolean enableCameraPick;
	
	private boolean editEnabled;
	
	private ArrayList<String> editedImages;

	GalleryItemLoaderTask galleryItemLoader = null;
	
	public static final String GALLERY_RESULT_ACTION = "gal_res_act";

	private View progressLoading;

	private GalleryRecyclerAdapter recyclerAdapter;

	private ParcelableSparseArray captions;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery);

		selectedGalleryItems = new ArrayList<GalleryItem>();
		galleryItemList = new ArrayList<GalleryItem>();
		editEnabled = Utils.isPhotosEditEnabled();

		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA };

		String selection = null;
		String[] args = null;

		Bundle data;
		if (savedInstanceState != null)
		{
			data = savedInstanceState;
			galleryItemList =  data.getParcelableArrayList(HikeConstants.Extras.GALLERY_ITEMS);
		}
		else
		{
			data = getIntent().getExtras();
		}

		GalleryItem selectedBucket = data.getParcelable(HikeConstants.Extras.SELECTED_BUCKET);
		msisdn = data.getString(HikeConstants.Extras.MSISDN);
		sendResult = data.getBoolean(START_FOR_RESULT);
		disableMultiSelect = data.getBoolean(DISABLE_MULTI_SELECT_KEY);

		if(editEnabled && data.containsKey(GallerySelectionViewer.EDIT_IMAGES_LIST) && data.getStringArrayList(GallerySelectionViewer.EDIT_IMAGES_LIST)!=null)
		{
			editedImages = new ArrayList<String>(data.getStringArrayList(GallerySelectionViewer.EDIT_IMAGES_LIST));
		}

		if(data.containsKey(HikeConstants.CAPTION) && data.get(HikeConstants.CAPTION) != null)
		{
			captions  = (ParcelableSparseArray) data.get(HikeConstants.CAPTION);
		}

		if (data.containsKey(FOLDERS_REQUIRED_KEY))
		{
			foldersRequired = data.getBoolean(FOLDERS_REQUIRED_KEY);
		}
		else
		{
			foldersRequired = true;// default hike settings
		}

		if (data.containsKey(ENABLE_CAMERA_PICK))
		{
			enableCameraPick = data.getBoolean(ENABLE_CAMERA_PICK);
		}

		String sortBy;
		if (selectedBucket != null)
		{
			if (selectedBucket.getName().equals(ALL_IMAGES_BUCKET_NAME))
			{
				selection = null;
				args = null;
			}
			else
			{
				selection = MediaStore.Images.Media.BUCKET_ID + "=?";
				args = new String[] { selectedBucket.getBucketId() };
			}

			isInsideAlbum = true;
			albumTitle = selectedBucket.getName();
			/*
			 * Adding the previously selected items.
			 */
			List<GalleryItem> prevSelectedItems = data.getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);
			if (prevSelectedItems != null && !prevSelectedItems.isEmpty())
			{
				selectedGalleryItems.addAll(prevSelectedItems);

				if (!multiSelectMode && !disableMultiSelect)
				{
					multiSelectMode = true;
					setupMultiSelectActionBar();
				}
				setMultiSelectTitle();
			}
			sortBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
		}
		else
		{
			if (foldersRequired)
			{
				selection = "1) GROUP BY (" + MediaStore.Images.Media.BUCKET_ID;

				sortBy = MediaStore.Images.Media.DATE_ADDED + " DESC";

				isInsideAlbum = false;
			}
			else
			{
				isInsideAlbum = true;

				sortBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
			}
		}

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.gallery_recyclerview);
		recyclerView.addItemDecoration(new MarginDecoration(this));
		recyclerView.setHasFixedSize(true);

		int sizeOfImage = getResources().getDimensionPixelSize(isInsideAlbum ? R.dimen.gallery_album_item_size : R.dimen.gallery_cover_item_size);
		int numColumns = isInsideAlbum ? 3 : Utils.getNumColumnsForGallery(getResources(), sizeOfImage);

		sizeOfImage = getUpdatedSizeOfImage(numColumns, sizeOfImage);

		recyclerAdapter = new GalleryRecyclerAdapter(this, galleryItemList, isInsideAlbum, sizeOfImage, selectedGalleryItems, false);
		GalleryItemClickHandler.addTo(recyclerView).setOnItemClickListener(new GalleryItemClickHandler.OnItemClickListener() {
			@Override
			public void onItemClicked(RecyclerView recyclerView, int position, View v) {
				onGalleryItemClick(position);
			}
		});
		if (isInsideAlbum && !disableMultiSelect)
		{
			GalleryItemClickHandler.addTo(recyclerView).setOnItemLongClickListener(new GalleryItemClickHandler.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClicked(RecyclerView recyclerView, int position,
						View v) {
					return onGalleryLongItemClick(position);
				}
			});
		}

		recyclerView.setLayoutManager(new GridLayoutManager(this, numColumns));
		recyclerView.setAdapter(recyclerAdapter);
		recyclerView.setVisibility(View.VISIBLE);

		StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);

		if (!multiSelectMode)
		{
			setupActionBar(albumTitle);
		}

		if(galleryItemList.isEmpty())
		{
			galleryItemLoader = new GalleryItemLoaderTask(this, isInsideAlbum, enableCameraPick);
			galleryItemLoader.buildQuery(uri, projection, selection, args, sortBy, editEnabled, editedImages);
			galleryItemLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			progressLoading = findViewById(R.id.progressLoading);
			if(!isInsideAlbum)
			{
				progressLoading.setVisibility(View.VISIBLE);
			}
			else
			{
				progressLoading.setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (recyclerAdapter != null)
		{
			recyclerAdapter.getGalleryImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (recyclerAdapter != null)
		{
			recyclerAdapter.getGalleryImageLoader().setExitTasksEarly(false);
			recyclerAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putAll(getIntent().getExtras());
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS, (ArrayList<GalleryItem>) selectedGalleryItems);
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_ITEMS, (ArrayList<GalleryItem>) galleryItemList);
		if(editEnabled && getIntent().hasExtra(GallerySelectionViewer.EDIT_IMAGES_LIST))
		{
			outState.putStringArrayList(GallerySelectionViewer.EDIT_IMAGES_LIST, editedImages);
		}
		outState.putParcelable(HikeConstants.CAPTION,captions);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if(galleryItemLoader != null)
		{
			galleryItemLoader.cancelTask();
		}
		if (multiSelectMode)
		{
			if( editEnabled && editedImages != null)
			{
				HikeDialog confirmUndo = HikeDialogFactory.showDialog(this, HikeDialogFactory.UNDO_MULTI_EDIT_CHANGES_DIALOG, new HikeDialogListener() {
					
					@Override
					public void positiveClicked(HikeDialog hikeDialog) {
						
						Utils.deleteFiles(getApplicationContext(), editedImages, HikeFileType.IMAGE);
						editedImages.clear();
						editedImages = null;
						hikeDialog.dismiss();
						GalleryActivity.this.onBackPressed();
					}
					
					@Override
					public void neutralClicked(HikeDialog hikeDialog) {
					}
					
					@Override
					public void negativeClicked(HikeDialog hikeDialog) {
						hikeDialog.dismiss();
					}
				}, null);
			}
			else
			{
				selectedGalleryItems.clear();
				recyclerAdapter.notifyDataSetChanged();
				
				setupActionBar(albumTitle);
				multiSelectMode = false;
			}
			
		}
		else
		{
			deleteJunkTempFiles();
			super.onBackPressed();
		}
	}
	
	

	private void setupActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		View actionBarView;
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		actionBarView.setBackgroundResource(android.R.color.transparent);

		TextView titleView = (TextView) actionBarView.findViewById(R.id.title);

		if (isInsideAlbum && !TextUtils.isEmpty(albumTitle))
		{
			titleView.setText(albumTitle);
		}
		else
		{
			titleView.setText(getString(R.string.photo_gallery_choose_pic));
		}

		titleView.setVisibility(View.VISIBLE);

		actionBar.setCustomView(actionBarView);
		actionBar.setDisplayHomeAsUpEnabled(true);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);

	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View sendBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		((TextView) actionBarView.findViewById(R.id.save)).setText(R.string.next_signup);
		
		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(sendButtonClickListener);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}

    OnClickListener sendButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteJunkTempFiles();

            Intent intent = getIntent();

            Bundle bundle = new Bundle();
			Bundle extras = getIntent().getExtras();
			if (extras != null)
			{
				bundle.putAll(extras);
			}
			intent.putExtras(bundle);

            /**
             * Setting class loader due class not found exception on GalleryItem whie parcing
             * @link : http://stackoverflow.com/questions/28589509/android-e-parcel-class-not-found-when-unmarshalling-only-on-samsung-tab3
             * @see : http://stackoverflow.com/questions/13421582/parcelable-inside-bundle-which-is-added-to-parcel
             */
            bundle.setClassLoader(GalleryItem.class.getClassLoader());
            bundle.putString(HikeConstants.Extras.GALLERY_SELECTION_SINGLE, selectedGalleryItems.get(0).getFilePath());
			bundle.putString(HikeConstants.Extras.GENUS, HomeAnalyticsConstants.SU_GENUS_GALLERY);

            if (hasDelegateActivities()) {
                launchNextDelegateActivity(bundle);
            } else if (!sendResult && isStartedForResult()) {
                //since sendResult is active when we need to send result to selection viewer
                intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, selectedGalleryItems);
                setGalleryResult(RESULT_OK, intent);
                finish();
            } else {
                intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, selectedGalleryItems);
                sendGalleryIntent(intent);
            }
        }
    };

	private void sendGalleryIntent(Intent intent)
	{
		intent.setClass(this, GallerySelectionViewer.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
		intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, getIntent().getParcelableExtra(HikeConstants.Extras.SELECTED_BUCKET));
		
		if (sendResult)
		{
			intent.putExtra(START_FOR_RESULT, sendResult);
			if(editEnabled && editedImages != null)
			{
				intent.putStringArrayListExtra(GallerySelectionViewer.EDIT_IMAGES_LIST, editedImages);
				editedImages=null;
			}

			if(captions != null)
			{
				intent.putExtra(HikeConstants.CAPTION,captions);
			}
		}
		
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case GALLERY_ACTIVITY_RESULT_CODE:
				Utils.setGenus(HomeAnalyticsConstants.SU_GENUS_GALLERY, data);
				setGalleryResult(RESULT_OK, data.putExtras(getIntent().getExtras()));
				finish();
				break;
			case HikeConstants.CAMERA_RESULT:
				String cameraFilename = Utils.getCameraResultFile();
				if (cameraFilename == null)
					return;

				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				ArrayList<GalleryItem> item = new ArrayList<GalleryItem>(1);
				item.add(new GalleryItem(GalleryItem.CAMERA_TILE_ID, CAMERA_TILE, NEW_PHOTO, cameraFilename, 0));

				/**
				 * Setting class loader due class not found exception on GalleryItem while parsing
				 * 
				 * @link : http://stackoverflow.com/questions/28589509/android-e-parcel-class-not-found-when-unmarshalling-only-on-samsung-tab3
				 * @see : http://stackoverflow.com/questions/13421582/parcelable-inside-bundle-which-is-added-to-parcel
				 */
				bundle.setClassLoader(GalleryItem.class.getClassLoader());

				bundle.putString(HikeConstants.Extras.GALLERY_SELECTION_SINGLE, cameraFilename);
				// Added to ensure delegate activity passes destination path to editer
				bundle.putString(HikeConstants.HikePhotos.DESTINATION_FILENAME, cameraFilename);

				bundle.putString(HikeConstants.Extras.GENUS, HomeAnalyticsConstants.SU_GENUS_CAMERA);

				Bundle extras = getIntent().getExtras();
				if (extras != null)
				{
					bundle.putAll(extras);
				}
				intent.putExtras(bundle);

				Utils.setGenus(HomeAnalyticsConstants.SU_GENUS_CAMERA, intent);

				if (hasDelegateActivities())
				{
					launchNextDelegateActivity(bundle);
				}
				else if (isStartedForResult())
				{
					intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, item);
					setGalleryResult(RESULT_OK, intent);
					finish();
				}
				else
				{
					intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, item);
					sendGalleryIntent(intent);
				}
				break;
			}
		}
	}

	public void setGalleryResult(int resultCode,Intent data)
	{
		//setting gallery result action if action is not already set for the data
		if(data.getAction() == null)
		{
			data.setAction(GALLERY_RESULT_ACTION);
		}
		setResult(resultCode, data);
	}


	private void setMultiSelectTitle()
	{
		if (multiSelectTitle == null)
		{
			return;
		}
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedGalleryItems.size()));
	}

	private void sendAnalyticsCameraClicked()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_CAMERA_CLICK);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true ;
	}
	
	private void deleteJunkTempFiles()
	{
		if(!editEnabled || !getIntent().hasExtra(GallerySelectionViewer.EDIT_IMAGES_LIST) || editedImages == null)
		{
			return;
		}

		ArrayList<String> initialEditList = getIntent().getStringArrayListExtra(GallerySelectionViewer.EDIT_IMAGES_LIST);

		if(initialEditList == null)
		{
			return;
		}

		initialEditList.removeAll(editedImages);

		Utils.deleteFiles(getApplicationContext(), initialEditList, HikeFileType.IMAGE);
	}

	@Override
	public void onGalleryItemLoaded(final GalleryItem galleryItem) {
		GalleryActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isInsideAlbum && (galleryItemLoader == null || !galleryItemLoader.isRunning()) && progressLoading != null)
				{
					progressLoading.setVisibility(View.GONE);
				}
				try
				{
					int position = recyclerAdapter.addItem(galleryItem);
					recyclerAdapter.notifyItemInserted(position);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public void onGalleryItemClick(int position)
	{
		if(position < 0)
		{
			return;
		}
		GalleryItem galleryItem = galleryItemList.get(position);

		Intent intent;

		if (galleryItem.getId() == GalleryItem.CAMERA_TILE_ID)
		{
			sendAnalyticsCameraClicked();
			File selectedFile = Utils.createNewFile(HikeFileType.IMAGE, HikeConstants.CAM_IMG_PREFIX);
			if (selectedFile == null)
			{
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			startActivityForResult(IntentFactory.getNativeCameraAppIntent(true, selectedFile), HikeConstants.CAMERA_RESULT);
			return;
		}
		else if (!isInsideAlbum)
		{
			intent = new Intent(this, GalleryActivity.class);
			intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, galleryItem);
			intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
			intent.putExtra(DISABLE_MULTI_SELECT_KEY, disableMultiSelect);

			String sourceSpecies = getIntent().getStringExtra(HikeConstants.Extras.SPECIES);
			if(!TextUtils.isEmpty(sourceSpecies))
			{
				intent.putExtra(HikeConstants.Extras.SPECIES, sourceSpecies);
			}
			
			if(hasDelegateActivities())
			{
				intent.putParcelableArrayListExtra(HikeBaseActivity.DESTINATION_INTENT, getDestinationIntents());
			}
			
			if(sendResult)
			{
				intent.putExtra(START_FOR_RESULT, sendResult);
			}
			
			if(isStartedForResult())
			{
				startActivityForResult(intent, GALLERY_ACTIVITY_RESULT_CODE);
			}
			else
			{
				startActivity(intent);
			}
		}
		else
		{
			if (multiSelectMode)
			{
				int index = selectedGalleryItems.indexOf(galleryItem);
				if (index >=0)
				{
					selectedGalleryItems.remove(index);
//					captions.remove(index);
					if(editedImages !=null)
					{
						editedImages.remove(index);
					}
					
					if (selectedGalleryItems.isEmpty())
					{
						setupActionBar(albumTitle);
						multiSelectMode = false;
					}
					else
					{
						setMultiSelectTitle();
					}
				}
				else
				{
					if (selectedGalleryItems.size() >= FileTransferManager.getInstance(this).remainingTransfers())
					{
						Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
						return;
					}
					selectedGalleryItems.add(galleryItem);
					if(editedImages !=null)
					{
						editedImages.add(null);
					}
					setMultiSelectTitle();
				}
				recyclerAdapter.notifyItemChanged(position);
			}
			else
			{
                selectedGalleryItems.add(galleryItem);
				sendButtonClickListener.onClick(null);
                selectedGalleryItems.clear();
			}
		}
	}

	public boolean onGalleryLongItemClick(int position)
	{
		if(position < 0)
		{
			return false;
		}
		if (!multiSelectMode)
		{
			multiSelectMode = true;
			setupMultiSelectActionBar();
		}

		if (selectedGalleryItems.size() >= FileTransferManager.getInstance(this).remainingTransfers())
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			return false;
		}

		GalleryItem galleryItem = galleryItemList.get(position);

		selectedGalleryItems.add(galleryItem);

		recyclerAdapter.notifyItemChanged(position);

		setMultiSelectTitle();
		return true;
	}

	/**
	 * Returns new size of image for gallery item based on extra horizontal space.
	 * It will be used to maintain the fixed horizontal margin between the gallery item in case when screen width is higher than the ((sizeOfImage*numOfColumns) + (numOfColumns*margin)).
	 * @param numColumns
	 * @param oldSizeOfImage
	 */
	private int getUpdatedSizeOfImage(int numColumns, int oldSizeOfImage)
	{
		int newSizeOfImage = oldSizeOfImage;
		int extraSpace = getResources().getDisplayMetrics().widthPixels - (numColumns * oldSizeOfImage);
		int margin = getResources().getDimensionPixelOffset(R.dimen.gallery_grid_spacing);
		if(extraSpace > (numColumns * margin))
		{
			extraSpace = extraSpace - (numColumns * margin);
			newSizeOfImage += extraSpace/numColumns;
		}
		return newSizeOfImage;
	}

	@Override
	public void onNoGalleryItemFound() {
		GalleryActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isInsideAlbum)
				{
					progressLoading.setVisibility(View.GONE);
				}
			}
		});
	}
}
