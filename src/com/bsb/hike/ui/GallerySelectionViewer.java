package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class GallerySelectionViewer extends HikeAppStateBaseFragmentActivity implements OnItemClickListener, OnScrollListener, OnPageChangeListener, HikePubSub.Listener
{
	public static final String FROM_DEVICE_GALLERY_SHARE = "from_gallery_share";
	
	public static final String EDIT_IMAGES_LIST = "edit_images_list";
	
	public static final int MULTI_EDIT_REQUEST_CODE = 12309;

	private GalleryAdapter gridAdapter;

	private GalleryPagerAdapter pagerAdapter;

	private GridView selectedGrid;

	private ViewPager selectedPager;

	private ArrayList<GalleryItem> galleryItems;

	private ArrayList<GalleryItem> galleryGridItems;
	
	private ArrayList<String> editedImages;

	private volatile InitiateMultiFileTransferTask fileTransferTask;

	private ProgressDialog progressDialog;
	
	private View closeSMLtipView = null;
	
	private int totalSelections;

	private boolean smlDialogShown = false;
	
	private boolean forGalleryShare ;
	
	private boolean editEnabled;
	
	private static final String TAG = "GAllerySelectionViewer";
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery_selection_viewer);
		Object object = getLastCustomNonConfigurationInstance();

		if (object instanceof InitiateMultiFileTransferTask)
		{
			fileTransferTask = (InitiateMultiFileTransferTask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}

		editEnabled = Utils.isPhotosEditEnabled();
		
		Bundle data = null;
		
		if(savedInstanceState == null)
		{
			data = getIntent().getExtras();
		}
		else
		{
			data = savedInstanceState;
		}
		
		if(data == null || !data.containsKey(HikeConstants.Extras.GALLERY_SELECTIONS))
		{
			//To Do : Display appropriate toast
			Logger.e(TAG,"Gallery Selection Viewer started without valid Extras");
			GallerySelectionViewer.this.finish();
			return;
		}
		
		forGalleryShare = getIntent().getBooleanExtra(FROM_DEVICE_GALLERY_SHARE, false);
		
		galleryItems = data.getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);
		totalSelections = galleryItems.size();

		/**
		 * Array to maintain list of edited files so that we dont create unnecessary copies
		 */
		if(editEnabled && (data.containsKey(EDIT_IMAGES_LIST)))
		{
			editedImages = data.getStringArrayList(EDIT_IMAGES_LIST);
				
			initiateEditMode();
		}
		
		/*
		 * Added one for the extra null item.
		 */
		galleryGridItems = new ArrayList<GalleryItem>(galleryItems.size() + (forGalleryShare?0:1));
		galleryGridItems.addAll(galleryItems);
		/*
		 * Adding an empty item which will be used to add more images.
		 */
		if(!forGalleryShare)
        {
            galleryGridItems.add(null);
            HikeMessengerApp.getPubSub().addListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);
        }


		selectedGrid = (GridView) findViewById(R.id.selection_grid);
		selectedPager = (ViewPager) findViewById(R.id.selection_pager);

		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_selection_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int imgSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		gridAdapter = new GalleryAdapter(this, galleryGridItems, true, imgSize, null, true);

		if(galleryGridItems != null && galleryGridItems.size() > 4)
		{
			RelativeLayout.LayoutParams params =  (android.widget.RelativeLayout.LayoutParams) selectedGrid.getLayoutParams();
			params.height = getResources().getDimensionPixelSize(R.dimen.gallery_selected_grid_height);
			selectedGrid.setLayoutParams(params);
		}
		
		selectedGrid.setNumColumns(numColumns);
		selectedGrid.setAdapter(gridAdapter);
		selectedGrid.setOnScrollListener(this);
		selectedGrid.setOnItemClickListener(this);

		pagerAdapter = new GalleryPagerAdapter(imgSize);
		selectedPager.setAdapter(pagerAdapter);
		selectedPager.setOnPageChangeListener(this);

		setSelection(galleryItems.size());
		setupActionBar();

		showTipIfRequired();
	}
	
	private void initiateEditMode()
	{
		if(editedImages == null)
		{
			editedImages = new ArrayList<String>(galleryItems.size());
			for (int i = 0;i<galleryItems.size();i++)
			{
				editedImages.add(null);
			}
		}
		else
		{
			for(int i=editedImages.size()-1;i>=0;i--)
			{
				if(editedImages.get(i)!=null && !new File(editedImages.get(i)).exists())
				{
					editedImages.remove(i);
				}
			}
		}
	}

	private void startAddMoreGalleryIntent()
	{

		Intent intent = new Intent(this, GalleryActivity.class);

		intent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, galleryItems);
		intent.putExtra(HikeConstants.Extras.SELECTED_BUCKET, getIntent().getParcelableExtra(HikeConstants.Extras.SELECTED_BUCKET));
		intent.putExtra(HikeConstants.Extras.MSISDN, getIntent().getStringExtra(HikeConstants.Extras.MSISDN));
		intent.putExtra(HikeConstants.Extras.ON_HIKE, getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true));
		
		if(getIntent().getBooleanExtra(GalleryActivity.START_FOR_RESULT, false))
		{
			intent.putExtra(GalleryActivity.START_FOR_RESULT, true);
		}
		
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		if(editEnabled && haveImagesBeenEdited())
		{
			intent.putStringArrayListExtra(EDIT_IMAGES_LIST, editedImages);
		}
		
		startActivity(intent);
	}
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(gridAdapter != null)
		{
			gridAdapter.getGalleryImageLoader().setExitTasksEarly(true);
		}
		if(pagerAdapter != null)
		{
			pagerAdapter.getGalleryImageLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(gridAdapter != null)
		{
			gridAdapter.getGalleryImageLoader().setExitTasksEarly(false);
			gridAdapter.notifyDataSetChanged();
		}
		if(pagerAdapter != null)
		{
			pagerAdapter.getGalleryImageLoader().setExitTasksEarly(false);
			pagerAdapter.notifyDataSetChanged();
		}
	}
	
	
	@Override
	protected void onStop()
	{
		int successfulSelections = galleryItems.size();
		HikeAnalyticsEvent.sendGallerySelectionEvent(totalSelections, successfulSelections, getApplicationContext());
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);

		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
		super.onDestroy();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		View doneBtn = actionBarView.findViewById(R.id.done_container);
		TextView postText = (TextView) actionBarView.findViewById(R.id.post_btn);
		View actionsView = actionBarView.findViewById(R.id.actionsView);
		
		doneBtn.setVisibility(View.VISIBLE);
		postText.setText(R.string.send);
		
		title.setText(R.string.preview);
		
		if(editEnabled)
		{
			actionsView.setVisibility(View.VISIBLE);
			actionBarView.findViewById(R.id.seprator).setVisibility(View.VISIBLE);
			actionBarView.findViewById(R.id.seprator).setAlpha(0.2f);
			actionsView.setOnClickListener(new OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					editSelectedImage();
				}
			});
		}
		
		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if(forGalleryShare)
				{
					Intent data = new Intent();
					data.putParcelableArrayListExtra(HikeConstants.IMAGE_PATHS, getSelectedFilesAsUri());
					setResult(RESULT_OK,data);
					GallerySelectionViewer.this.finish();
					return;
				}
				
				final ArrayList<Pair<String, String>> fileDetails = new ArrayList<Pair<String, String>>(galleryItems.size());
				long sizeOriginal = 0;
				for (int i = 0;i<galleryItems.size();i++)
				{
					//Using edited filepath if user has edited the current selection other wise the original
					String filePath = getFinalFilePathAtPosition(i);
					
					fileDetails.add(new Pair<String, String> (filePath, HikeFileType.toString(HikeFileType.IMAGE)));
					File file = new File(filePath);
					sizeOriginal += file.length();
				}
				
				final String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
				final boolean onHike = getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true);
				
				final Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(GallerySelectionViewer.this, msisdn, false,false);
				if (!smlDialogShown)
				{
					HikeDialogFactory.showDialog(GallerySelectionViewer.this, HikeDialogFactory.SHARE_IMAGE_QUALITY_DIALOG,  new HikeDialogListener()
					{
						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
							hikeDialog.dismiss();
						}

						@Override
						public void positiveClicked(HikeDialog hikeDialog)
						{
							fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileDetails, msisdn, onHike, FTAnalyticEvents.GALLERY_ATTACHEMENT, intent);
							Utils.executeAsyncTask(fileTransferTask);
							if(!OfflineUtils.isConnectedToSameMsisdn(msisdn))
								progressDialog = ProgressDialog.show(GallerySelectionViewer.this, null, getResources().getString(R.string.multi_file_creation));
							hikeDialog.dismiss();
						}

						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{
							
						}
					}, (Object[]) new Long[]{(long)fileDetails.size(), sizeOriginal});
				}
				else
				{
					fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileDetails, msisdn, onHike, FTAnalyticEvents.GALLERY_ATTACHEMENT, intent);
					Utils.executeAsyncTask(fileTransferTask);
					progressDialog = ProgressDialog.show(GallerySelectionViewer.this, null, getResources().getString(R.string.multi_file_creation));
				}
			}
		});
		
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
		
		actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.gallery_selection_action_bar)));
	}
	
	private void editSelectedImage()
	{
		int currPos = selectedPager.getCurrentItem();
		
		//Using edited filepath if user has edited the current selection other wise the original also writing over the edited file if the user is editing an already edited image
		String selectedFilePath = getFinalFilePathAtPosition(currPos);
		String destinationFilePath = isIndexEdited(currPos)?selectedFilePath:null;
		Intent intent = IntentFactory.getPictureEditorActivityIntent(GallerySelectionViewer.this, selectedFilePath, forGalleryShare,destinationFilePath , false);
		startActivityForResult(intent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (fileTransferTask != null)
		{
			return fileTransferTask;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		gridAdapter.setIsListFlinging(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		GalleryItem galleryItem = galleryGridItems.get(position);
		if (galleryItem == null)
		{
			startAddMoreGalleryIntent();
		}
		else
		{
			setSelection(position);
		}
	}

	@Override
	public void onPageScrollStateChanged(int scrollState)
	{
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2)
	{
	}

	@Override
	public void onPageSelected(int position)
	{
		setSelection(position);
	}

	private void setSelection(int position)
	{
		int scrollPos = position;
		
		if(position >= galleryItems.size() )
		{
			position = galleryItems.size() - 1;
		}
		
		gridAdapter.setSelectedItemPosition(position);
		selectedPager.setCurrentItem(position);
		
		selectedGrid.smoothScrollToPosition(scrollPos);
	}

	private class GalleryPagerAdapter extends PagerAdapter
	{
		LayoutInflater layoutInflater;

		GalleryImageLoader galleryImageLoader;

		int viewerHeight;

		int viewerWidth;

		public GalleryPagerAdapter(int size_image)
		{
			layoutInflater = LayoutInflater.from(GallerySelectionViewer.this);
			galleryImageLoader = new GalleryImageLoader(GallerySelectionViewer.this, size_image);

			int padding = 2 * getResources().getDimensionPixelSize(R.dimen.gallery_selection_padding);

			viewerWidth = getResources().getDisplayMetrics().widthPixels - padding;
			viewerHeight = getResources().getDisplayMetrics().heightPixels - getResources().getDimensionPixelSize(R.dimen.st__action_bar_default_height)
					- getResources().getDimensionPixelSize(R.dimen.notification_bar_height) - getResources().getDimensionPixelSize(R.dimen.gallery_selected_grid_height) - padding;
		}

		@Override
		public int getCount()
		{
			return galleryItems.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			return view == object;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public int getItemPosition(Object object)
		{
			return POSITION_NONE;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			View page = layoutInflater.inflate(R.layout.gallery_preview_item, container, false);

			ImageButton removeImage = (ImageButton) page.findViewById(R.id.remove_selection);

			ImageView galleryImageView = (ImageView) page.findViewById(R.id.album_image);
			galleryImageView.setScaleType(ScaleType.FIT_CENTER);

			//Using edited filepath if user has edited the current selection other wise the original
			String filePath = new String(getFinalFilePathAtPosition(position));
			
			galleryImageLoader.loadImage(GalleryImageLoader.GALLERY_KEY_PREFIX + filePath, galleryImageView, false, true);

			setupButtonSpacing(galleryImageView, removeImage);

			removeImage.setTag(position);
			removeImage.setOnClickListener(removeSelectionClickListener);

			((ViewPager) container).addView(page);
			return page;
		}

		private void setupButtonSpacing(ImageView galleryImageView, ImageButton removeImage)
		{
			Drawable drawable = galleryImageView.getDrawable();
			if (drawable == null)
			{
				return;
			}

			int drawableHeight = drawable.getIntrinsicHeight();
			int drawableWidth = drawable.getIntrinsicWidth();

			int imageWidth;
			int imageHeight;

			if (viewerHeight / drawableHeight <= viewerWidth / drawableWidth)
			{
				imageWidth = drawableWidth * viewerHeight / drawableHeight;
				imageHeight = viewerHeight;
			}
			else
			{
				imageHeight = drawableHeight * viewerWidth / drawableWidth;
				imageWidth = viewerWidth;
			}

			LayoutParams layoutParams = (LayoutParams) removeImage.getLayoutParams();
			layoutParams.leftMargin = imageWidth;
			layoutParams.bottomMargin = imageHeight/2;
		}
		
		public GalleryImageLoader getGalleryImageLoader()
		{
			return galleryImageLoader;
		}
	}

	OnClickListener removeSelectionClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			int postion = (Integer) v.getTag();
			galleryItems.remove(postion);
			galleryGridItems.remove(postion);
			if(editedImages!=null)
			{
				if(isIndexEdited(postion))
				{
					Utils.deleteFile(getApplicationContext(), editedImages.get(postion), HikeFileType.IMAGE);
				}
				editedImages.remove(postion);
			}
			gridAdapter.notifyDataSetChanged();
			pagerAdapter.notifyDataSetChanged();

			if(galleryItems.isEmpty())
			{
				if(forGalleryShare)
				{
					onBackPressed();
				}
				else
				{
					startAddMoreGalleryIntent();
				}
			}

			GallerySelectionViewer.this.selectedPager.setCurrentItem(postion == 0 ? 0 : postion - 1);
		}
	};

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			fileTransferTask = null;
			
			final Intent intent = (Intent) object;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
					
					startActivity(intent);
					finish();
					
				}
			});
		}
	}
	
	private void showTipIfRequired()
	{
		final HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		if(pref.getData(HikeConstants.REMEMBER_IMAGE_CHOICE, false) && pref.getData(HikeConstants.SHOW_IMAGE_QUALITY_TIP, true))
		{
			View view = LayoutInflater.from(this).inflate(R.layout.tip_right_arrow, null);
			ImageView arrowPointer = (ImageView) (view.findViewById(R.id.arrow_pointer));
			arrowPointer.getLayoutParams().width = (int) (74 * Utils.scaledDensityMultiplier);
			arrowPointer.requestLayout();
			arrowPointer.setImageResource(R.drawable.ftue_up_arrow);
			((TextView) view.findViewById(R.id.tip_header)).setText(R.string.image_settings_tip_text);
			((TextView) view.findViewById(R.id.tip_msg)).setText(R.string.image_settings_tip_subtext);
			final View tipView = view;
			closeSMLtipView = view.findViewById(R.id.close_tip);
			closeSMLtipView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					tipView.setVisibility(View.GONE);
					pref.saveData(HikeConstants.SHOW_IMAGE_QUALITY_TIP, false);
				}
			});
			((LinearLayout) findViewById(R.id.tipContainerTop)).addView(view, 0);
		}
	}
	
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS, galleryItems);
		if(haveImagesBeenEdited())
		{
			outState.putStringArrayList(EDIT_IMAGES_LIST, editedImages);
		}
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onBackPressed() {
		if(haveImagesBeenEdited())
		{
				HikeDialog confirmUndo = HikeDialogFactory.showDialog(this, HikeDialogFactory.UNDO_MULTI_EDIT_CHANGES_DIALOG, new HikeDialogListener() {
				
					@Override
					public void positiveClicked(HikeDialog hikeDialog) {
						
						Utils.deleteFiles(getApplicationContext(), editedImages, HikeFileType.IMAGE);
						hikeDialog.dismiss();
						GallerySelectionViewer.super.onBackPressed();	
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
			super.onBackPressed();
		}
		
	}
	
	private boolean haveImagesBeenEdited()
	{
		if(editedImages == null || editedImages.isEmpty())
		{
			return false;
		}
		
		for (int i = 0;i<editedImages.size();i++)
		{
			if(editedImages.get(i)!=null)
			{
				return true;
			}
		}
		
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK)
		{
			switch(requestCode)
			{
			case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
				
				int currPos = selectedPager.getCurrentItem();
				
				//Using edited filepath if user has edited the current selection as previous path other wise the original
				if(isIndexEdited(currPos))
				{
					//Removing the previous edited bitmap from cache since its edited thus no longer will be required
					HikeMessengerApp.getLruCache().removeItemForKey(GalleryImageLoader.GALLERY_KEY_PREFIX + editedImages.get(currPos));
				}
				String editedFilePath = data.getStringExtra(HikeConstants.Extras.IMAGE_PATH);
				
				if(editedImages == null)
				{
					initiateEditMode();
				}
				
				editedImages.set(currPos,editedFilePath);
				galleryGridItems.get(currPos).setFilePath(editedFilePath);
				gridAdapter.notifyDataSetChanged();
				pagerAdapter.notifyDataSetChanged();
				break;
			}
		}
	}
	
	private ArrayList<Uri> getSelectedFilesAsUri()
	{
		ArrayList<Uri> selectedUris = new ArrayList<Uri>(galleryItems.size());
		for (int i = 0;i<galleryItems.size();i++)
		{
			//Using edited filepath if user has edited the current selection other wise the original
			String filePath = getFinalFilePathAtPosition(i);
			
			File file = new File(filePath);
			selectedUris.add(Uri.fromFile(file));
		}
		
		return selectedUris;
		
	}
	
	private boolean isIndexEdited(int index)
	{
		if(!editEnabled || editedImages == null || editedImages.isEmpty())
		{
			return false;
		}
		
		if(index >= editedImages.size())
		{
			return false;
		}
		
		return (editedImages.get(index) != null);
		
	}
	
	private String getFinalFilePathAtPosition(int position)
	{
		String filePath = isIndexEdited(position)?editedImages.get(position):galleryItems.get(position).getFilePath();
		return filePath;
	}
	
}
