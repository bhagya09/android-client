package com.bsb.hike.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.chatthread.IChannelSelector;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class AttachmentPicker extends OverFlowMenuLayout
{

	private static final String TAG = "attachmentPicker";

	public static final int CAMERA = 313;

	public static final int GALLERY = 314;

	public static final int AUDIO = 315;

	public static final int VIDEO = 316;

	public static final int FILE = 317;

	public static final int CONTACT = 318;

	public static final int LOCATION = 319;
	
	public static final int EDITOR = 320;

	public static final int APPS = 321;

	public static final int ATTACHMENT_PICKER = -1;

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
	 * By default we show {@link #CAMERA} {@link #GALLARY} {@link #AUDIO} {@link #VIDEO} {@link #FILE} {@link #CONTACT} {@link #LOCATION}
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
			items.add(new OverFlowMenuItem(getString(R.string.location_msg_sent), 0, R.drawable.ic_attach_location, LOCATION));
		}
		this.overflowItems = items;
	}

	@Override
	public View getView()
	{
		return viewToShow != null ? viewToShow : initView();
	}

	@Override
	public View initView()
	{
		// we lazily inflate and
		if (viewToShow != null)
		{
			setOrientation(activity.getResources().getConfiguration().orientation);
			return viewToShow;
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
				case LOCATION:
					requestCode = LOCATION;
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
				case APPS:
					requestCode  = APPS;
					pickIntent = IntentFactory.getApkSelectionActivityIntent(context);
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

				sendClickAnalytics(getType(item.id)); // recording the event on item click at Attachment Picker
			}
		});
		return viewToShow;
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
		Resources resources = context.getResources();
		int numCol = getNumColumnsAttachments();
		grid.setNumColumns(numCol);
		LayoutParams lp = grid.getLayoutParams();
		lp.width = numCol * resources.getDimensionPixelSize(R.dimen.attachment_column_width);
		int displayHeight = resources.getDisplayMetrics().heightPixels;
		int margin = resources.getDimensionPixelSize(R.dimen.attachment_grid_vertical_margin);
		int gridHeight = getGridHeight();
		lp.height = Math.min(gridHeight, displayHeight - margin);
		grid.setLayoutParams(lp);
	}
	
	private int getNumColumnsAttachments()
	{
		return currentConfig == Configuration.ORIENTATION_LANDSCAPE ? 4 : 3;
	}

	private int getGridHeight()
	{
		return currentConfig == Configuration.ORIENTATION_LANDSCAPE ?
				context.getResources().getDimensionPixelSize(R.dimen.attachment_grid_landscape_height) : 
					context.getResources().getDimensionPixelSize(R.dimen.attachment_grid_portrait_height);
	}

	private String getString(int id)
	{
		return context.getString(id);
	}

	private void sendClickAnalytics(String type)
	{
		JSONObject json = Utils.getCoreChatClickJSON(type, ChatThreadUtils.getChatThreadType(msisdn), StealthModeManager.getInstance().isStealthMsisdn(msisdn));
		if (json != null)
		{
			HAManager.getInstance().recordV2(json);
		}
	}

	/**
	 * method to get the type of key for the click event
	 * @param code
	 * @return a string representing the key of the event
	 */
	private String getType(int code)
	{
		switch(code)
		{
			case CAMERA:
				return ChatAnalyticConstants.CAMERA_ICON_CLICK;
			case VIDEO:
				return ChatAnalyticConstants.VIDEO_ICON_CLICK;
			case AUDIO:
				return ChatAnalyticConstants.AUDIO_ICON_CLICK;
			case LOCATION:
				return ChatAnalyticConstants.LOCATION_ICON_CLICK;
			case CONTACT:
				return ChatAnalyticConstants.CONTACT_ICON_CLICK;
			case FILE:
				return ChatAnalyticConstants.FILE_ICON_CLICK;
			case GALLERY:
				return ChatAnalyticConstants.GALLERY_ICON_CLICK;
			case APPS:
				return ChatAnalyticConstants.APPS_ICON_CLICK;
			default:
				return ChatAnalyticConstants.ATTACHMENT_PICKER_CLICK;
		}
	}

	@Override
	public void show(int width, int height, int xOffset, int yOffset, View anchor, int inputMethodMode)
	{
		sendClickAnalytics(ChatAnalyticConstants.ATTACHMENT_PICKER_CLICK); // recording the showing of attachment picker
		super.show(width, height, xOffset, yOffset, anchor, inputMethodMode);
	}

}
