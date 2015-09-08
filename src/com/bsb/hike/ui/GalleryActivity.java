package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.jess.ui.TwoWayAbsListView;
import com.jess.ui.TwoWayAbsListView.OnScrollListener;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayAdapterView.OnItemClickListener;
import com.jess.ui.TwoWayAdapterView.OnItemLongClickListener;
import com.jess.ui.TwoWayGridView;

public class GalleryActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, OnItemClickListener, OnItemLongClickListener
{

	public static final String DISABLE_MULTI_SELECT_KEY = "en_mul_sel";

	public static final String FOLDERS_REQUIRED_KEY = "fold_req";

	public static final String ENABLE_CAMERA_PICK = "cam_pk";

	public static final int GALLERY_ACTIVITY_RESULT_CODE = 97;
	
	public static final int GALLERY_CROP_IMAGE = 64;
	
	public static final int GALLERY_ALLOW_MULTISELECT = 32;
	
	public static final int GALLERY_CATEGORIZE_BY_FOLDERS = 16;
	
	public static final int GALLERY_DISPLAY_CAMERA_ITEM = 8;
	
	public static final int GALLERY_EDIT_SELECTED_IMAGE = 4;
	
	public static final int GALLERY_COMPRESS_EDITED_IMAGE = 2;
	
	public static final int GALLERY_FOR_PROFILE_PIC_UPDATE = 1;
	
	private List<GalleryItem> galleryItemList;

	private GalleryAdapter adapter;

	private boolean isInsideAlbum;

	private String msisdn;

	private boolean foldersRequired;

	private boolean multiSelectMode;

	private ArrayList<GalleryItem> selectedGalleryItems;

	private TextView multiSelectTitle;

	private String albumTitle;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;

	public static final String START_FOR_RESULT = "startForResult";

	/**
	 * This flag indicates whether this was opened for result or not, i.e. was it startActivityForResult
	 */
	private boolean sendResult;

	private boolean disableMultiSelect;

	private final String ALL_IMAGES_BUCKET_NAME = "All images";

	private final String HIKE_IMAGES = "hike";
	
	private final String CAMERA_TILE = "gallery_tile_camera";

	private final String CAMERA_IMAGES = "Camera";

	private final String NEW_PHOTO = "New photo";

	private final String TYPE_JPG = ".jpg";

	private final String TYPE_JPEG = ".jpeg";

	private final String TYPE_PNG = ".png";

	private boolean enableCameraPick;
	
	private boolean editEnabled;
	
	private ArrayList<String> editedImages;
	
	public static final String GALLERY_RESULT_ACTION = "gal_res_act";
	
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
		Cursor cursor = null;

		Bundle data;
		if (savedInstanceState != null)
		{
			data = savedInstanceState;
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
		
		// Add "pick from camera" button/bucket
		if (enableCameraPick)
		{
			GalleryItem allImgItem = new GalleryItem(GalleryItem.CAMERA_TILE_ID, NEW_PHOTO, CAMERA_TILE, 0);
			galleryItemList.add(allImgItem);
		}


		/*
		 * Creating All images bucket where we will show all images present in the device.
		 */
		if (!isInsideAlbum)
		{
			String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
			cursor = getContentResolver().query(uri, proj, null, null, sortBy);
			if (cursor != null)
			{
				try
				{
					int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
					int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					if (cursor.moveToNext())
					{
						GalleryItem allImgItem = new GalleryItem(cursor.getLong(idIdx), null, ALL_IMAGES_BUCKET_NAME, cursor.getString(dataIdx), cursor.getCount());
						galleryItemList.add(allImgItem);
					}
				}
				finally
				{
					cursor.close();
				}
			}
		}
		
		cursor = getContentResolver().query(uri, projection, selection, args, sortBy);

		if (cursor != null)
		{
			try
			{
				int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
				int bucketIdIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
				int nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
				int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

				while (cursor.moveToNext())
				{
					int count = 0;
					String filePath = cursor.getString(dataIdx);
					if(!isValidFile(filePath))
					{
						continue;
					}
					
					if (!isInsideAlbum)
					{
						count = getGalleryItemCount(new File(filePath).getParent());
					}
					
					GalleryItem galleryItem = new GalleryItem(cursor.getLong(idIdx), cursor.getString(bucketIdIdx), cursor.getString(nameIdx), cursor.getString(dataIdx), count);
					galleryItemList.add(galleryItem);
					if (!isInsideAlbum)
					{
						galleryItemList = reOrderList(galleryItemList);
					}
				}
			}
			finally
			{
				cursor.close();
			}
		}
		
		TwoWayGridView gridView = (TwoWayGridView) findViewById(R.id.gallery);

		int sizeOfImage = getResources().getDimensionPixelSize(isInsideAlbum ? R.dimen.gallery_album_item_size : R.dimen.gallery_cover_item_size);

		int numColumns = isInsideAlbum ? 3 : Utils.getNumColumnsForGallery(getResources(), sizeOfImage);

		int actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		adapter = new GalleryAdapter(this, galleryItemList, isInsideAlbum, actualSize, selectedGalleryItems, false);

		gridView.setNumColumns(numColumns);
		gridView.setAdapter(adapter);
		gridView.setOnScrollListener(this);
		gridView.setOnItemClickListener(this);

		if (isInsideAlbum && !disableMultiSelect)
		{
			gridView.setOnItemLongClickListener(this);
		}

		if (!multiSelectMode)
		{
			setupActionBar(albumTitle);
		}
	}
	
	//returns true if the filePath was edited in the current MultiEdit Flow
	private boolean isImageEdited(String filePath)
	{
		if(editedImages == null || !filePath.contains(PictureEditer.getEditImageSaveDirectory(false)))
		{
			return false;
		}
		
		String filename=(new File(filePath)).getName();
		for(String editedImage:editedImages)
		{
			if(editedImage == null)
			{
				continue;
			}
			String editFilename=(new File(editedImage)).getName();
			if(editFilename.equalsIgnoreCase(filename))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isValidFile(String filePath)
	{
		if (TextUtils.isEmpty(filePath))
		{
			Logger.d(GalleryActivity.class.getSimpleName(), "onCreate() filePath: empty " + filePath);
			return false;
		}

		if(editEnabled && isImageEdited(filePath))
		{
			//Skipping this file since this is a temp file created by multi-edit 
			return false;
		}
		return true;
	}

	private ArrayList<GalleryItem> reOrderList(List<GalleryItem> list)
	{
		ArrayList<GalleryItem> resultList = new ArrayList<GalleryItem>();
		LinkedList<GalleryItem> customList = new LinkedList<GalleryItem>();
		
		int customCount = 0;
		
		for (Iterator<GalleryItem> iterator = list.iterator(); iterator.hasNext();)
		{
			GalleryItem galleryItem = (GalleryItem) iterator.next();
			if (galleryItem.getName().startsWith(HIKE_IMAGES) || galleryItem.getName().startsWith(ALL_IMAGES_BUCKET_NAME) || galleryItem.getName().startsWith(CAMERA_IMAGES))
			{
				customList.addLast(galleryItem);
				iterator.remove();
			}
			else if (galleryItem.getType() == GalleryItem.CUSTOM)
			{
				customList.add(customCount, galleryItem);
				customCount++;
				iterator.remove();
			}
		}
		
		for (Iterator<GalleryItem> iterator = customList.iterator(); iterator.hasNext();)
		{
			GalleryItem galleryItem = (GalleryItem) iterator.next();
			resultList.add(galleryItem);
		}
		
		for (Iterator<GalleryItem> iterator = list.iterator(); iterator.hasNext();)
		{
			GalleryItem galleryItem = (GalleryItem) iterator.next();
			resultList.add(galleryItem);
		}
		return resultList;
	}

	private int getGalleryItemCount(String dirPath)
	{
		File dir = new File(dirPath);
		int number = 0;
		if (dir != null && dir.exists())
		{
			File[] files = dir.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (file.isFile())// Check file or directory
					{
						if (isImage(file.getName().toLowerCase()))
						{
							number++;
						}
					}
				}
			}
		}
		return number;
	}

	private boolean isImage(String fileName)
	{
		boolean isImg = false;
		if (fileName.endsWith(TYPE_JPG) || fileName.endsWith(TYPE_JPEG) || fileName.endsWith(TYPE_PNG))
		{
			isImg = true;
		}
		return isImg;
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if (adapter != null)
		{
			adapter.getGalleryImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if (adapter != null)
		{
			adapter.getGalleryImageLoader().setExitTasksEarly(false);
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putAll(getIntent().getExtras());
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS, (ArrayList<GalleryItem>)selectedGalleryItems);
		if(editEnabled && getIntent().hasExtra(GallerySelectionViewer.EDIT_IMAGES_LIST))
		{
			outState.putStringArrayList(GallerySelectionViewer.EDIT_IMAGES_LIST, editedImages);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
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
				adapter.notifyDataSetChanged();
				
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

		titleView.setText(getString(R.string.photo_gallery_choose_pic));

		titleView.setVisibility(View.VISIBLE);

		actionBarView.findViewById(R.id.done_container).setVisibility(View.INVISIBLE);
		actionBar.setCustomView(actionBarView);
		actionBar.setDisplayHomeAsUpEnabled(true);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
		
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

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				deleteJunkTempFiles();
				
				Intent intent = getIntent();
				
				Bundle bundle = new Bundle();
				
				/**
				 * Setting class loader due class not found exception on GalleryItem whie parcing
				 * @link : http://stackoverflow.com/questions/28589509/android-e-parcel-class-not-found-when-unmarshalling-only-on-samsung-tab3
				 * @see : http://stackoverflow.com/questions/13421582/parcelable-inside-bundle-which-is-added-to-parcel
				 */
				bundle.setClassLoader(GalleryItem.class.getClassLoader());
				bundle.putString(HikeConstants.Extras.GALLERY_SELECTION_SINGLE, selectedGalleryItems.get(0).getFilePath());
				intent.putExtras(bundle);
								
				if(selectedGalleryItems.size() == 1 && hasDelegateActivities())
				{
					launchNextDelegateActivity(bundle);
				}
				else if(!sendResult && isStartedForResult())
				{
					//since sendResult is active when we need to send result to selection viewer
					intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, selectedGalleryItems);
					setGalleryResult(RESULT_OK, intent);
					finish();
				}
				else
				{
					intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, selectedGalleryItems);
					sendGalleryIntent(intent);
				}
			}
		});

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
		}
		
		startActivity(intent);
	}

	@Override
	public void onScroll(TwoWayAbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
		{
			switch(requestCode)
			{
			case GALLERY_ACTIVITY_RESULT_CODE:
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
				 * Setting class loader due class not found exception on GalleryItem whie parcing
				 * @link : http://stackoverflow.com/questions/28589509/android-e-parcel-class-not-found-when-unmarshalling-only-on-samsung-tab3
				 * @see : http://stackoverflow.com/questions/13421582/parcelable-inside-bundle-which-is-added-to-parcel
				 */
				bundle.setClassLoader(GalleryItem.class.getClassLoader());
				
				
				bundle.putString(HikeConstants.Extras.GALLERY_SELECTION_SINGLE, cameraFilename);
				//Added to ensure delegate activity passes destination path to editer
				bundle.putString(HikeConstants.HikePhotos.DESTINATION_FILENAME, cameraFilename); 
				intent.putExtras(bundle);
				
				if(hasDelegateActivities())
				{
					launchNextDelegateActivity(bundle);
				}
				else if(isStartedForResult())
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

	@Override
	public void onScrollStateChanged(TwoWayAbsListView view, int scrollState)
	{
		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	public void setGalleryResult(int resultCode,Intent data)
	{
		//setting gallery result action if action is not already set for the data
		if(data.getAction() == null)
		{
			data.setAction(GALLERY_RESULT_ACTION);
		}
		setResult(resultCode,data);
	}
	
	@Override
	public void onItemClick(TwoWayAdapterView<?> adapterView, View view, int position, long id)
	{
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
				adapter.notifyDataSetChanged();
			}
			else
			{
				deleteJunkTempFiles();
				intent = new Intent();
				Bundle bundle = new Bundle();
				
				ArrayList<GalleryItem> item = new ArrayList<GalleryItem>(1);
				item.add(galleryItem);
				
				File file = new File(item.get(0).getFilePath());
				if (!file.exists())
				{
					Toast.makeText(GalleryActivity.this, getResources().getString(R.string.file_expire), Toast.LENGTH_SHORT).show();
					return;
				}
				
				/**
				 * Setting class loader due class not found exception on GalleryItem whie parcing
				 * @link : http://stackoverflow.com/questions/28589509/android-e-parcel-class-not-found-when-unmarshalling-only-on-samsung-tab3
				 * @see : http://stackoverflow.com/questions/13421582/parcelable-inside-bundle-which-is-added-to-parcel
				 */
				
				bundle.putString(HikeConstants.Extras.GALLERY_SELECTION_SINGLE, galleryItem.getFilePath());
				intent.putExtras(bundle);
				
				if(hasDelegateActivities())
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
			}
		}
	}

	@Override
	public boolean onItemLongClick(TwoWayAdapterView<?> adapterView, View view, int position, long id)
	{
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

		adapter.notifyDataSetChanged();

		setMultiSelectTitle();

		return true;
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
	protected void setStatusBarColor(Window window, String color) {
		// TODO Auto-generated method stub
		return;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// TODO Auto-generated method stub
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
	
}
