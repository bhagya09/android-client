package com.bsb.hike.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

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

	private boolean startRespectiveActivities;

	private Activity activity;
	
	private String msisdn;

	/**
	 * 
	 * @param overflowItems
	 * @param listener
	 * @param context
	 * @param startRespectiveActivities
	 *            - if true, we will start respective activities on activity behalf and activity has to handle onActivityResult callback where request code is Overflowitem
	 *            uniqueness
	 */
	public AttachmentPicker(String msisdn, List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener, Context context, boolean startRespectiveActivities)
	{
		super(overflowItems, listener, onDismissListener,context);
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
		this(msisdn, null, listener, onDismissListener,activity.getApplicationContext(), startRespectiveActivities);
		this.activity = activity;
		initDefaultAttachmentList();
	}

	private void initDefaultAttachmentList()
	{
		List<OverFlowMenuItem> items = new ArrayList<OverFlowMenuItem>(7);
		items.add(new OverFlowMenuItem(getString(R.string.camera_upper_case), 0, R.drawable.ic_attach_camera, CAMERA));
		items.add(new OverFlowMenuItem(getString(R.string.photo), 0, R.drawable.ic_attach_pic, GALLERY));
		items.add(new OverFlowMenuItem(getString(R.string.audio), 0, R.drawable.ic_attach_music, AUDIO));
		items.add(new OverFlowMenuItem(getString(R.string.video), 0, R.drawable.ic_attach_video, VIDEO));
		items.add(new OverFlowMenuItem(getString(R.string.file), 0, R.drawable.ic_attach_file, FILE));
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION))
		{
			items.add(new OverFlowMenuItem(getString(R.string.location_option), 0, R.drawable.ic_attach_location, LOCATOIN));
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
			return;
		}

		View parentView = viewToShow = LayoutInflater.from(context).inflate(R.layout.attachments, null);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);
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
					pickIntent = IntentFactory.getImageCaptureIntent(context);
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

	private String getString(int id)
	{
		return context.getString(id);
	}
}
