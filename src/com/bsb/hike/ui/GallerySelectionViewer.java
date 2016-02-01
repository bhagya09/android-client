package com.bsb.hike.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.GalleryAdapter;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.smartImageLoader.GalleryPagerImageLoader;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.edmodo.cropper.CropImageView;

public class GallerySelectionViewer extends HikeAppStateBaseFragmentActivity implements OnItemClickListener, OnScrollListener, OnPageChangeListener, HikePubSub.Listener, View.OnClickListener
{
	public static final String FROM_DEVICE_GALLERY_SHARE = "from_gallery_share";

	public static final String FROM_CAMERA_CAPTURE = "from_camera_capture";

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

	private SparseArray<String> captions = new SparseArray<>();

	private boolean isInCropMode;

	private View cropPanel;

	private ImageButton btnRemove, btnEdit, btnCrop;

	private View doneBtn;

	private String TAG_CROP_IV = "cropimageview";

	private String TAG_CAPTION_ET = "captionet";

	private String TAG_CAPTION_LINE = "capline";

	private View containerCrop,containerEdit,containerRemove;

	private View containerRotate;

	private View cropDivider;

	private View btnCropCancel;

	private View btnCropAccept;

	private boolean fromCameraCapture;

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

		fromCameraCapture = getIntent().getBooleanExtra(FROM_CAMERA_CAPTURE, false);

		galleryItems = data.getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);
		totalSelections = galleryItems.size();

		if(galleryItems.isEmpty())
		{
			//To Do : Display appropriate toast
			Logger.e(TAG,"Gallery Selection Viewer started without valid Extras");
			GallerySelectionViewer.this.finish();
			return;
		}

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

		for (int i = 0; i < galleryItems.size(); i++)
		{
			captions.put(i,"");
		}


		selectedGrid = (GridView) findViewById(R.id.selection_grid);
		selectedPager = (ViewPager) findViewById(R.id.selection_pager);
		selectedPager.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if(isInCropMode)
				{
					return true;
				}
				return false;
			}
		});

		btnCrop = (ImageButton)findViewById(R.id.ib_crop);
		btnEdit = (ImageButton)findViewById(R.id.ib_edit);
		btnRemove = (ImageButton) findViewById(R.id.ib_remove);
		cropPanel = findViewById(R.id.crop_actions_panel);
		cropDivider = findViewById(R.id.crop_panel_divider);
		containerCrop = findViewById(R.id.container_crop);
		containerEdit = findViewById(R.id.container_edit);
		containerRemove = findViewById(R.id.container_remove);
		containerRotate = findViewById(R.id.container_rotate);
		btnCropCancel = findViewById(R.id.cancel);
		btnCropAccept = findViewById(R.id.accept);

		btnCropAccept.setOnClickListener(this);
		btnCropCancel.setOnClickListener(this);

		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable() {
			@Override
			public void run()
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						containerCrop.animate().alpha(1f);
						containerEdit.animate().setStartDelay(50).alpha(1f);
						containerRemove.animate().setStartDelay(100).alpha(1f);
					}
				});
			}
		},500);

		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_selection_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int imgSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		gridAdapter = new GalleryAdapter(this, galleryGridItems, true, imgSize, null, true);

		arrangeHeights();

		selectedGrid.setNumColumns(numColumns);
		selectedGrid.setAdapter(gridAdapter);
		selectedGrid.setOnScrollListener(this);
		selectedGrid.setOnItemClickListener(this);

		pagerAdapter = new GalleryPagerAdapter();
		selectedPager.setAdapter(pagerAdapter);
		selectedPager.setOnPageChangeListener(this);

		setSelection(galleryItems.size());
		setupActionBar();

		showTipIfRequired();
	}

	private void arrangeHeights()
	{
		if(galleryGridItems != null && galleryGridItems.size() > 4)
		{
			RelativeLayout.LayoutParams params =  (android.widget.RelativeLayout.LayoutParams) selectedGrid.getLayoutParams();
			params.height = getResources().getDimensionPixelSize(R.dimen.gallery_selected_grid_height);
			selectedGrid.setLayoutParams(params);
		}
		else if(galleryGridItems != null)
		{
			RelativeLayout.LayoutParams params =  (android.widget.RelativeLayout.LayoutParams) selectedGrid.getLayoutParams();
			params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			selectedGrid.setLayoutParams(params);
		}
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
		super.onPause();
		if(gridAdapter != null)
		{
			gridAdapter.getGalleryImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if(gridAdapter != null)
		{
			gridAdapter.getGalleryImageLoader().setExitTasksEarly(false);
			gridAdapter.notifyDataSetChanged();
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
		doneBtn = actionBarView.findViewById(R.id.done_container);
		TextView postText = (TextView) actionBarView.findViewById(R.id.post_btn);
		View actionsView = actionBarView.findViewById(R.id.actionsView);

		doneBtn.setVisibility(View.VISIBLE);
		postText.setText(R.string.send);

		title.setText(R.string.preview);

		if(editEnabled)
		{
			actionsView.setVisibility(View.INVISIBLE);
			actionBarView.findViewById(R.id.seprator).setVisibility(View.INVISIBLE);
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
					ArrayList<Uri> selectedFiles = getSelectedFilesAsUri();
					data.putParcelableArrayListExtra(HikeConstants.IMAGE_PATHS,selectedFiles);

					if (captions.size() > 0)
					{
						ArrayList<String> captionsArrayList = new ArrayList<String>();
						for (int i = 0; i < captions.size(); i++)
						{
							int key = captions.keyAt(i);
							String captionString = captions.get(key);
							captionsArrayList.add(captionString);
						}
						data.putStringArrayListExtra(HikeConstants.CAPTION, captionsArrayList);
					}

					setResult(RESULT_OK, data);
					GallerySelectionViewer.this.finish();
					return;
				}

                final ArrayList<ComposeChatActivity.FileTransferData> ftDataList = new ArrayList<ComposeChatActivity.FileTransferData>(galleryItems.size());

                final String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
                final boolean onHike = getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true);

                ArrayList<ContactInfo> list = new ArrayList<ContactInfo>();
                list.add(ContactManager.getInstance().getContact(msisdn));

                long sizeOriginal = 0;
				for (int i = 0;i<galleryItems.size();i++)
				{
					//Using edited filepath if user has edited the current selection other wise the original
					String filePath = getFinalFilePathAtPosition(i);

					File file = new File(filePath);
					sizeOriginal += file.length();

                    String caption = null;
                    if(!TextUtils.isEmpty(captions.get(i)))
                    {
                        caption = captions.get(i);
                    }

                    ComposeChatActivity.FileTransferData fileTransferData = new ComposeChatActivity.FileTransferData(filePath, null, HikeFileType.IMAGE, HikeFileType.toString(HikeFileType.IMAGE).toLowerCase(), false, -1, false, list, file,caption);
                    ftDataList.add(fileTransferData);
                }

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
							fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), ftDataList, msisdn, onHike, FTAnalyticEvents.GALLERY_ATTACHEMENT, intent);
							Utils.executeAsyncTask(fileTransferTask);
							if(!OfflineUtils.isConnectedToSameMsisdn(msisdn))
								progressDialog = ProgressDialog.show(GallerySelectionViewer.this, null, getResources().getString(R.string.multi_file_creation));
							hikeDialog.dismiss();
						}

						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{

						}
					}, (Object[]) new Long[]{(long)ftDataList.size(), sizeOriginal});
				}
				else
				{
					fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), ftDataList, msisdn, onHike, FTAnalyticEvents.GALLERY_ATTACHEMENT, intent);
					Utils.executeAsyncTask(fileTransferTask);
					progressDialog = ProgressDialog.show(GallerySelectionViewer.this, null, getResources().getString(R.string.multi_file_creation));
				}
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);

		actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.gallery_selection_action_bar)));
		StatusBarColorChanger.setStatusBarColor(getWindow(),HikeConstants.STATUS_BAR_TRANSPARENT);
	}

	private void editSelectedImage()
	{
		int currPos = selectedPager.getCurrentItem();

		//Using edited filepath if user has edited the current selection other wise the original also writing over the edited file if the user is editing an already edited image
		String selectedFilePath = getFinalFilePathAtPosition(currPos);
		String destinationFilePath = isIndexEdited(currPos) || fromCameraCapture?selectedFilePath:null;
		Intent intent = IntentFactory.getPictureEditorActivityIntent(GallerySelectionViewer.this, selectedFilePath, forGalleryShare,destinationFilePath , false);
		startActivityForResult(intent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
		HikeMessengerApp.getLruCache().removeItemForKey(GalleryImageLoader.GALLERY_KEY_PREFIX + selectedFilePath);
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

	@Override
	public void onClick(View view) {
		CropImageView cropImageView = (CropImageView) selectedPager.findViewWithTag(TAG_CROP_IV + selectedPager.getCurrentItem());
		switch (view.getId())
		{
			case R.id.ib_crop:
				Utils.hideSoftKeyboard(GallerySelectionViewer.this);
				setCropViewVisibility(true);
				break;
			case R.id.ib_edit:
				Utils.hideSoftKeyboard(GallerySelectionViewer.this);
				editSelectedImage();
				break;
			case R.id.ib_remove:
                removeSelectionClickListener.onClick(null);
				break;
			case R.id.rotateLeft:
				cropImageView.rotateImage(90);
				break;
			case R.id.cancel:
				int rotation = cropImageView.getDegreesRotated();
				if(rotation != 0)
				{
					cropImageView.rotateImage(-1*rotation);
				}

			setCropViewVisibility(false);
			break;
		case R.id.accept:
			Bitmap croppedImage = cropImageView.getCroppedImage();
			int currPos = selectedPager.getCurrentItem();

			String destinationFileHandle = null;
			if (isIndexEdited(currPos))
			{
				destinationFileHandle = editedImages.get(currPos);
			}
			else
			{
				destinationFileHandle = PictureEditer.getEditImageSaveDirectory(true) + File.separator + Utils.getUniqueFilename(HikeFileType.IMAGE);
			}

			try
			{
				BitmapUtils.saveBitmapToFile(new File(destinationFileHandle), croppedImage, Bitmap.CompressFormat.JPEG, 85);

				if (editedImages == null)
				{
					initiateEditMode();
				}
				editedImages.set(currPos, destinationFileHandle);
				galleryGridItems.get(currPos).setFilePath(destinationFileHandle);
				setCropViewVisibility(false);
				gridAdapter.notifyDataSetChanged();
				pagerAdapter.notifyDataSetChanged();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	private void recordOriginalXY()
	{
		if (!originalRecordered)
		{
			originalRecordered = true;
			btnXorig = btnRemove.getX();
			gridYorig = selectedGrid.getY();
		}
	}
	boolean originalRecordered = false;
	private float btnXorig = 0;
	private float gridYorig = 0;
	
	private void setCropViewVisibility(final boolean enableCrop)
	{
		CropImageView cropImageView = (CropImageView) selectedPager.findViewWithTag(TAG_CROP_IV + selectedPager.getCurrentItem());
		if (cropImageView == null || cropImageView.getBitmap() == null)
		{
			return;
		}

		recordOriginalXY();

		isInCropMode = enableCrop;

		btnRemove.animate().setStartDelay(100).x(enableCrop ? btnXorig + 200f : btnXorig);
		btnEdit.animate().setStartDelay(50).x(enableCrop ? btnXorig + 200f : btnXorig);
		btnCrop.animate().x(enableCrop ? btnXorig + 200f : btnXorig);

		selectedGrid.animate().y(galleryGridItems.size() > 4 ? enableCrop ? gridYorig + 600f : gridYorig : enableCrop ? gridYorig + 300f : gridYorig);
		cropPanel.setVisibility(enableCrop ? View.VISIBLE : View.GONE);
		containerRotate.setVisibility(enableCrop ? View.VISIBLE : View.GONE);
		cropDivider.setVisibility(enableCrop ? View.VISIBLE : View.GONE);
		if (enableCrop)
		{
			cropImageView.showCropOverlay();
		}
		else
		{
			cropImageView.hideCropOverlay();
		}

		selectedPager.findViewWithTag(TAG_CAPTION_ET + selectedPager.getCurrentItem()).setVisibility(enableCrop ? View.GONE : View.VISIBLE);
		selectedPager.findViewWithTag(TAG_CAPTION_LINE + selectedPager.getCurrentItem()).setVisibility(enableCrop ? View.GONE : View.VISIBLE);

		doneBtn.setVisibility(enableCrop ? View.GONE : View.VISIBLE);

		containerCrop.animate().alpha(enableCrop ? 0f : 1f);
		containerEdit.animate().setStartDelay(50).alpha(enableCrop ? 0f : 1f);
		containerRemove.animate().setStartDelay(100).alpha(enableCrop ? 0f : 1f);
	}

	private class GalleryPagerAdapter extends PagerAdapter
	{
		LayoutInflater layoutInflater;

		GalleryPagerImageLoader galleryPagerImageLoader;

		public GalleryPagerAdapter()
		{
			layoutInflater = LayoutInflater.from(GallerySelectionViewer.this);
			galleryPagerImageLoader = new GalleryPagerImageLoader();
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
		public View instantiateItem(ViewGroup container, final int position)
		{
			View page = layoutInflater.inflate(R.layout.gallery_preview_item, container, false);

			final CropImageView galleryImageView = (CropImageView) page.findViewById(R.id.cropimageview);
//			galleryImageView.setScaleType(ScaleType.FIT_CENTER);

			galleryImageView.setTag(TAG_CROP_IV+position);

			// Using edited filepath if user has edited the current selection other wise the original
			final String filePath = new String(getFinalFilePathAtPosition(position));

			HikeHandlerUtil.getInstance().postRunnable(new Runnable()
			{
				@Override
				public void run()
				{
					Log.d("Atul", "STARTING TO COMPRESS"+filePath);

					final Bitmap bmp = galleryPagerImageLoader.processBitmap(filePath);

					Log.d("Atul", "QUEUED TO VIEW ON SCREEN "+filePath);
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							galleryImageView.setImageBitmap(bmp);
							Log.d("Atul", "DISPLAYED  ON SCREEN " + filePath);
							galleryImageView.hideCropOverlay();
						}
					});
				}
			});

			((ViewPager) container).addView(page);

            final EditText captionEt = (EditText) page.findViewById(R.id.et_caption);
			captionEt.setTag(TAG_CAPTION_ET + position);
            if(!TextUtils.isEmpty(captions.get(position)))
            {
                captionEt.setText(captions.get(position));
            }

			page.findViewById(R.id.caption_underline).setTag(TAG_CAPTION_LINE + position);

            captionEt.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

				}

				@Override
				public void afterTextChanged(Editable editable) {
					captions.put(position, editable.toString());
				}
			});
			captionEt.addTextChangedListener(new EmoticonTextWatcher());

			captionEt.setOnLongClickListener(new View.OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View view)
				{
					String text = Utils.getClipboardText(getApplicationContext());
					if(text!=null)
					{
						Utils.setClipboardText(text,getApplicationContext());
					}
					return false;
				}
			});

			return page;
		}

		public GalleryPagerImageLoader getGalleryImageLoader()
		{
			return galleryPagerImageLoader;
		}
	}

	OnClickListener removeSelectionClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			int position = selectedPager.getCurrentItem();
			galleryItems.remove(position);
			galleryGridItems.remove(position);
			removeCaption(position);
			if(editedImages!=null)
			{
				if(isIndexEdited(position))
				{
					Utils.deleteFile(getApplicationContext(), editedImages.get(position), HikeFileType.IMAGE);
				}
				editedImages.remove(position);
			}
			gridAdapter.notifyDataSetChanged();
			pagerAdapter.notifyDataSetChanged();

			arrangeHeights();

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


			GallerySelectionViewer.this.selectedPager.setCurrentItem(position == 0 ? 0 : position - 1);
		}
	};

	private void removeCaption(int position)
	{
		int captionsSize = captions.size();
		for (int i = position; i < (captionsSize - 1); i++)
		{
			captions.put(i,captions.get(i + 1));
		}
		captions.remove(captions.size() - 1);
	}

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

		if(isInCropMode)
		{
			setCropViewVisibility(false);
			return;
		}

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
