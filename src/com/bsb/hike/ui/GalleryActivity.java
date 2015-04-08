package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class GalleryActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, OnItemClickListener, OnItemLongClickListener
{

	public static final String DISABLE_MULTI_SELECT_KEY = "en_mul_sel";

	public static final String FOLDERS_REQUIRED_KEY = "fold_req";

	public static final String PENDING_INTENT_KEY = "pen_intent";

	public static final String ACTION_BAR_TYPE_KEY = "action_bar";

	public static final String RETURN_RESULT_KEY = "return_result";

	public static final int PHOTOS_EDITOR_ACTION_BAR_TYPE = 1;

	private int actionBarType;

	private List<GalleryItem> galleryItemList;

	private GalleryAdapter adapter;

	private boolean isInsideAlbum, returnResult;

	private String msisdn;

	private boolean foldersRequired;

	private boolean multiSelectMode;

	private Map<Long, GalleryItem> selectedGalleryItems;

	private TextView multiSelectTitle;

	private String albumTitle;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;

	private boolean disableMultiSelect;

	private PendingIntent pendingIntent;
	
	private final String ALL_IMAGES_BUCKET_NAME = "All images";

	private final String HIKE_IMAGES = "hike";

	private final String CAMERA_IMAGES = "Camera";

	private final String TYPE_JPG = ".jpg";

	private final String TYPE_JPEG = ".jpeg";

	private final String TYPE_PNG = ".png";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.gallery);

		selectedGalleryItems = new HashMap<Long, GalleryItem>();
		galleryItemList = new ArrayList<GalleryItem>();

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
		disableMultiSelect = data.getBoolean(DISABLE_MULTI_SELECT_KEY);
		pendingIntent = data.getParcelable(PENDING_INTENT_KEY);

		if (data.containsKey(FOLDERS_REQUIRED_KEY))
		{
			foldersRequired = data.getBoolean(FOLDERS_REQUIRED_KEY);
		}
		else
		{
			foldersRequired = true;// default hike settings
		}

		if (data.containsKey(ACTION_BAR_TYPE_KEY))
		{
			actionBarType = data.getInt(ACTION_BAR_TYPE_KEY);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		}
		else
		{
			actionBarType = 0;// default hike settings
		}

		returnResult = data.containsKey(RETURN_RESULT_KEY) ? data.getBoolean(RETURN_RESULT_KEY) : false;

		String sortBy;
		if (selectedBucket != null)
		{
			if(selectedBucket.getName().equals(ALL_IMAGES_BUCKET_NAME))
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
				for (GalleryItem galleryItem : prevSelectedItems)
				{
					selectedGalleryItems.put(galleryItem.getId(), galleryItem);
				}

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

		/*
		 * Creating All images bucket where we will show all images present in the device.
		 */
		if(!isInsideAlbum)
		{
			String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
			cursor = getContentResolver().query(uri, proj, null, null, sortBy);
			if (cursor != null)
			{
				try
				{
					int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
					int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					if(cursor.moveToNext())
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
					if(!isInsideAlbum)
					{
						String dirPath = cursor.getString(dataIdx);
						dirPath = dirPath.substring(0, dirPath.lastIndexOf("/"));
						count = getGalleryItemCount(dirPath);
					}
					GalleryItem galleryItem = new GalleryItem(cursor.getLong(idIdx), cursor.getString(bucketIdIdx), cursor.getString(nameIdx), cursor.getString(dataIdx), count);
					galleryItemList.add(galleryItem);
					if(!isInsideAlbum)
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

		GridView gridView = (GridView) findViewById(R.id.gallery);

		int sizeOfImage = getResources().getDimensionPixelSize(isInsideAlbum ? R.dimen.gallery_album_item_size : R.dimen.gallery_cover_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
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

	private ArrayList<GalleryItem>  reOrderList(List<GalleryItem> list)
	{
		ArrayList<GalleryItem> resultList = new ArrayList<GalleryItem>();
		GalleryItem allImgItem = null;
		GalleryItem hikeImages = null;
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			GalleryItem galleryItem = (GalleryItem) iterator.next();
			if(galleryItem.getName().startsWith(HIKE_IMAGES))
			{
				hikeImages = galleryItem;
				iterator.remove();
			}
			else if(galleryItem.getName().startsWith(CAMERA_IMAGES))
			{
				resultList.add(galleryItem);
				iterator.remove();
			}
			else if(galleryItem.getName().startsWith(ALL_IMAGES_BUCKET_NAME))
			{
				allImgItem = galleryItem;
				iterator.remove();
			}
		}
		if(allImgItem != null)
			resultList.add(allImgItem);
		if(hikeImages != null)
			resultList.add(hikeImages);
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			GalleryItem galleryItem = (GalleryItem) iterator.next();
			resultList.add(galleryItem);
		}
		return resultList;
	}

	private int getGalleryItemCount(String dirPath)
	{
		File dir = new File(dirPath);
		int number = 0;
		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile())// Check file or directory
					{
						if (isImage(file.getName().toLowerCase())) {
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
		if (fileName.endsWith(TYPE_JPG) || fileName.endsWith(TYPE_JPEG)
				|| fileName.endsWith(TYPE_PNG)) {
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
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS, new ArrayList<GalleryItem>(selectedGalleryItems.values()));

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if (multiSelectMode)
		{
			selectedGalleryItems.clear();
			adapter.notifyDataSetChanged();

			setupActionBar(albumTitle);
			multiSelectMode = false;
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void setupActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		View actionBarView;
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		switch (actionBarType)
		{
		case PHOTOS_EDITOR_ACTION_BAR_TYPE:
			actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
			actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onBackPressed();
				}
			});

			TextView titleView = (TextView)actionBarView.findViewById(R.id.title);
			
			titleView.setText(getString(R.string.photo_gallery_choose_pic));
			
			titleView.setVisibility(View.VISIBLE);
			
			actionBarView.findViewById(R.id.done_container).setVisibility(View.INVISIBLE);
			break;
		default:
			actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

			View backContainer = actionBarView.findViewById(R.id.back);

			TextView title = (TextView) actionBarView.findViewById(R.id.title);
			title.setText(titleString == null ? getString(R.string.gallery) : titleString);

			if (isInsideAlbum)
			{
				TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
				subText.setVisibility(View.VISIBLE);
				subText.setText(R.string.tap_hold_multi_select);
			}

			backContainer.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					finish();
				}
			});
		}
		actionBar.setCustomView(actionBarView);
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View sendBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent();
				intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, new ArrayList<GalleryItem>(selectedGalleryItems.values()));

				sendGalleryIntent(intent);
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

		startActivity(intent);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
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
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		GalleryItem galleryItem = galleryItemList.get(position);

		Intent intent;

		if (!isInsideAlbum)
		{
			intent = new Intent(this, GalleryActivity.class);
			intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, galleryItem);
			intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
			intent.putExtra(PENDING_INTENT_KEY, pendingIntent);
			intent.putExtra(DISABLE_MULTI_SELECT_KEY, disableMultiSelect);
			startActivity(intent);
		}
		else
		{
			if (multiSelectMode)
			{
				if (selectedGalleryItems.containsKey(galleryItem.getId()))
				{
					selectedGalleryItems.remove(galleryItem.getId());
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
					selectedGalleryItems.put(galleryItem.getId(), galleryItem);
					setMultiSelectTitle();
				}
				adapter.notifyDataSetChanged();
			}
			else
			{
				intent = new Intent();

				ArrayList<GalleryItem> item = new ArrayList<GalleryItem>(1);
				item.add(galleryItem);
				intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, item);

				if (pendingIntent != null)
				{
					try
					{
						pendingIntent.send(GalleryActivity.this, RESULT_OK, intent);
					}
					catch (CanceledException e)
					{
						e.printStackTrace();
					}
				}
				else if (returnResult)
				{
					setResult(RESULT_OK, intent);
					finish();
				}
				else
				{
					sendGalleryIntent(intent);
				}
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
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

		selectedGalleryItems.put(galleryItem.getId(), galleryItem);

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
}
