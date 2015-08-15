package com.bsb.hike.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class AttachmentPicker extends OverFlowMenuLayout
{

	private static final String TAG = "attachmentPicker";

	public static final int CAMERA = 313;

	public static final int GALLERY = 314;

	public static final int AUDIO = 315;

	public static final int VIDEO = 316;

	public static final int FILE = 317;

	public static final int CONTACT = 318;

	public static final int LOCATOIN = 319;
	
	public static final int EDITOR = 320;

	private boolean startRespectiveActivities;

	private Activity activity;

	private String msisdn;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

	/**
	 * 
	 * @param overflowItems
	 * @param listener
	 * @param context
	 * @param startRespectiveActivities
	 *            - if true, we will start respective activities on activity behalf and activity has to handle onActivityResult callback where request code is Overflowitem
	 *            uniqueness
	 */
	public AttachmentPicker(String msisdn, List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener, Context context,
			boolean startRespectiveActivities)
	{
		super(overflowItems, listener, onDismissListener, context);
		this.startRespectiveActivities = startRespectiveActivities;
		this.msisdn = msisdn;
	}

	/**
	 * By default we show {@link #CAMERA} {@link #GALLARY} {@link #AUDIO} {@link #VIDEO} {@link #FILE} {@link #CONTACT} {@link #LOCATOIN}
	 * 
	 * @param listener
	 * @param context
	 * @param startRespectiveActivities
	 *            - if true, we will start respective activities on activity behalf and activity has to handle onActivityResult callback and request code will be constants given
	 *            above
	 */
	public AttachmentPicker(String msisdn, OverflowItemClickListener listener, OnDismissListener onDismissListener, Activity activity, boolean startRespectiveActivities)
	{
		this(msisdn, null, listener, onDismissListener, activity.getApplicationContext(), startRespectiveActivities);
		this.activity = activity;
		this.currentConfig = activity.getResources().getConfiguration().orientation;
		initDefaultAttachmentList();
	}

	private void initDefaultAttachmentList()
	{
		List<OverFlowMenuItem> items = new ArrayList<OverFlowMenuItem>(7);
		items.add(new OverFlowMenuItem(getString(R.string.camera), 0, R.drawable.ic_attach_camera, CAMERA));
		items.add(new OverFlowMenuItem(getString(R.string.gallery), 0, R.drawable.ic_attach_gallery, GALLERY));
		items.add(new OverFlowMenuItem(getString(R.string.audio_msg_sent), 0, R.drawable.ic_attach_audio, AUDIO));
		items.add(new OverFlowMenuItem(getString(R.string.video_msg_sent), 0, R.drawable.ic_attach_video, VIDEO));
		items.add(new OverFlowMenuItem(getString(R.string.file_msg_sent), 0, R.drawable.ic_attach_file, FILE));
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION))
		{
			items.add(new OverFlowMenuItem(getString(R.string.location_msg_sent), 0, R.drawable.ic_attach_location, LOCATOIN));
		}
		this.overflowItems = items;
	}

	@Override
	public View getView()
	{
		return viewToShow;
	}

	@Override
	public void initView()
	{
		// we lazily inflate and
		if (viewToShow != null)
		{
			setOrientation(activity.getResources().getConfiguration().orientation);
			return;
		}

		View parentView = viewToShow = LayoutInflater.from(context).inflate(R.layout.attachments, null);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);
		refreshGridView(attachmentsGridView);
		attachmentsGridView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(context, R.layout.attachment_item, R.id.text, overflowItems)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(context).inflate(R.layout.attachment_item, parent, false);
				}
				OverFlowMenuItem menuItem = getItem(position);

				ImageView attachmentImageView = (ImageView) convertView.findViewById(R.id.attachment_icon);
				TextView attachmentTextView = (TextView) convertView.findViewById(R.id.text);

				attachmentImageView.setImageResource(menuItem.drawableId);
				attachmentTextView.setText(menuItem.text);

				return convertView;
			}
		});

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				popUpLayout.dismiss();

				OverFlowMenuItem item = overflowItems.get(position);
				if (!startRespectiveActivities)
				{
					listener.itemClicked(item);
					return;
				}

				// Start respective activities
				int requestCode = -1;
				Intent pickIntent = null;
				switch (item.id)
				{
				case CAMERA:
					requestCode = CAMERA;
					File selectedFile = Utils.createNewFile(HikeFileType.IMAGE, HikeConstants.CAM_IMG_PREFIX);
					if (selectedFile == null)
					{
						Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
						break;
					}
					pickIntent = IntentFactory.getNativeCameraAppIntent(true, selectedFile);
					break;
				case VIDEO:
					requestCode = VIDEO;
					pickIntent = IntentFactory.getVideoRecordingIntent();
					break;
				case AUDIO:
					requestCode = AUDIO;
					pickIntent = IntentFactory.getAudioShareIntent(context);
					break;
				case LOCATOIN:
					requestCode = LOCATOIN;
					pickIntent = IntentFactory.getLocationPickerIntent(context);
					break;
				case CONTACT:
					requestCode = CONTACT;
					pickIntent = IntentFactory.getContactPickerIntent();
					break;
				case FILE:
					requestCode = FILE;
					pickIntent = IntentFactory.getFileSelectActivityIntent(context, msisdn);
					break;
				case GALLERY:
					listener.itemClicked(item);
					break;
				}
				if (pickIntent != null)
				{
					activity.startActivityForResult(pickIntent, requestCode);
				}
				else
				{
					Logger.e(TAG, "intent is null !!");
				}
			}
		});

	}
	
	/**
	 * This function should be called when orientation of screen is changed, it will update its view based on orientation
	 * If picker is being shown, it will first dismiss current picker and then show it again using post on view
	 * 
	 * NOTE : It will not give dismiss callback to listener as this is not explicit dismiss
	 * @param orientation
	 */
	public void onOrientationChange(int orientation)
	{
		setOrientation(orientation);
	}

	public void setOrientation(int orientation)
	{
		if(orientation != currentConfig)
		{
			this.currentConfig = orientation;
			refreshGridView((GridView) viewToShow.findViewById(R.id.attachment_grid));
		}
	}

	public void refreshGridView(GridView grid)
	{
		int numCol = getNumColumnsAttachments();
		grid.setNumColumns(numCol);
		LayoutParams lp = grid.getLayoutParams();
		lp.width = numCol * context.getResources().getDimensionPixelSize(R.dimen.attachment_column_width);
		grid.setLayoutParams(lp);
	}
	
	private int getNumColumnsAttachments()
	{
		return currentConfig == Configuration.ORIENTATION_LANDSCAPE ? 4 : 3;
	}
	
	private String getString(int id)
	{
		return context.getString(id);
	}
}
