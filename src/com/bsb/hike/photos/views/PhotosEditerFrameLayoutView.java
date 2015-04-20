package com.bsb.hike.photos.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.photos.views.CanvasImageView.OnDoodleStateChangeListener;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Utils;

/**
 * Custom View extends FrameLayout Packs all the editing layers <filter layer,vignette layer ,doodle layer> into a single view ,in same z-order
 * 
 * @author akhiltripathi
 * 
 */
public class PhotosEditerFrameLayoutView extends FrameLayout implements OnFilterAppliedListener,OnTouchListener
{
	private CanvasImageView doodleLayer;

	private EffectsImageView effectLayer;

	private boolean enableDoodling, savingFinal, compressOutput,enableEffects;

	private Bitmap imageOriginal, imageEdited, imageScaled, scaledImageOriginal;

	private HikeFileType mFileType;

	private String mOriginalName;

	private HikePhotosListener mListener;

	public PhotosEditerFrameLayoutView(Context context)
	{
		super(context);
		doodleLayer = new CanvasImageView(context);
		effectLayer = new EffectsImageView(context);
		init();
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		doodleLayer = new CanvasImageView(context, attrs);
		effectLayer = new EffectsImageView(context, attrs);
		init();
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		doodleLayer = new CanvasImageView(context, attrs, defStyleAttr);
		effectLayer = new EffectsImageView(context, attrs, defStyleAttr);
		init();
	}
	
	private void init()
	{
		addView(effectLayer);
		addView(doodleLayer);
		enableDoodling = false;
		enableEffects = true;
		savingFinal = false;
		compressOutput = true;
		this.setOnTouchListener(this);
	}

	public void setCompressionEnabled(boolean state)
	{
		this.compressOutput = state;
	}

	public boolean isCompressionEnabled()
	{
		return this.compressOutput;
	}

	public int getThumbnailDimen()
	{
		int density = getResources().getDisplayMetrics().densityDpi;
		switch (density)
		{
		case DisplayMetrics.DENSITY_LOW:
		case DisplayMetrics.DENSITY_MEDIUM:
		case DisplayMetrics.DENSITY_HIGH:
			return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_MDPI;
		default:
			boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

			if (!hasBackKey)
			{
				// Do whatever you need to do, this device has a navigation bar
				return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_MDPI;
			}
			else
			{
				return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_HDPI;
			}

		}

	}

	public Bitmap getScaledImageOriginal()
	{
		if (scaledImageOriginal == null)
		{
			scaledImageOriginal = HikePhotosUtils.compressBitamp(imageOriginal, HikePhotosUtils.dpToPx(getContext(), getThumbnailDimen()), HikePhotosUtils.dpToPx(getContext(), getThumbnailDimen()),false);

			if (scaledImageOriginal == null)
			{
				// To Do Out Of Memory Handling
				//Need to take a call on whether to OPEN home activity
			}

		}
		return scaledImageOriginal;
	}

	public void setBrushWidth(int width)
	{
		doodleLayer.setStrokeWidth(width);
	}

	public void applyFilter(FilterType filter)
	{
		effectLayer.applyEffect(filter, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE, this);
		effectLayer.invalidate();

	}

	/**
	 * 
	 * @param filePath
	 *            : absolute address of the file to be handled by the editor object
	 */
	public void loadImageFromFile(String filePath)
	{
		try
		{
			imageOriginal = BitmapFactory.decodeFile(filePath);
		}
		catch (OutOfMemoryError e)
		{
			Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();
			IntentManager.openHomeActivity(getContext(), true);
		}

		handleImage();

	}

	private void handleImage()
	{
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		int height = (int)(metrics.heightPixels * getContext().getResources().getInteger(R.integer.photos_editor_canvas_weight)*1.0f/getContext().getResources().getInteger(R.integer.photos_editor_weightSum));
		if (width != imageOriginal.getWidth())
		{
			imageScaled = HikePhotosUtils.compressBitamp(imageOriginal, width, height,true);
			if(imageScaled == null)
			{
				Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();
				IntentManager.openHomeActivity(getContext(), true);
				return;
			}
			effectLayer.handleImage(imageScaled, true);
		}
		else
		{
			effectLayer.handleImage(imageOriginal, false);
			imageScaled = imageOriginal;
		}
	}

	public void loadImageFromBitmap(Bitmap bmp)
	{
		imageOriginal = bmp;
		handleImage();
	}

	public void enableDoodling()
	{
		enableDoodling = true;
		doodleLayer.setDrawEnabled(true);
	}

	public void disableDoodling()
	{
		enableDoodling = false;
		doodleLayer.setDrawEnabled(false);
	}
	
	public void enableFilters()
	{
		enableEffects = true;
		effectLayer.setAllowTouchMode(true);
	}

	public void disableFilters()
	{
		enableEffects = false;
		effectLayer.setAllowTouchMode(false);
	}

	public void setBrushColor(int Color)
	{
		doodleLayer.setColor(Color);
	}

	public void disable()
	{
		doodleLayer.setDrawEnabled(false);
	}

	public void enable()
	{
		doodleLayer.setDrawEnabled(enableDoodling);
	}

	public void saveImage(HikeFileType fileType, String originalName, HikePhotosListener listener)
	{
		doodleLayer.getMeasure();

		this.mFileType = fileType;
		this.mOriginalName = originalName;
		this.mListener = listener;

		savingFinal = true;
		if(compressOutput && HikePhotosUtils.getBitmapArea(imageOriginal)>HikeConstants.HikePhotos.MAXIMUM_ALLOWED_IMAGE_AREA)
		{
			compressOutput = false;
			imageOriginal = HikePhotosUtils.compressBitamp(imageOriginal, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_LOW_FULL_SIZE_PX,true);
		}

		effectLayer.getBitmapWithEffectsApplied(imageOriginal, this);

	}
	
	public void undoLastDoodleDraw()
	{
		doodleLayer.onClickUndo();

	}

	public void setOnDoodlingStartListener(OnDoodleStateChangeListener listener)
	{
		doodleLayer.setOnDoodlingStartListener(listener);
	}

	private void saveImagetoFile()
	{
		File file = null;
		if (mFileType == HikeFileType.IMAGE)
		{
			try
			{
				String timeStamp = Long.toString(System.currentTimeMillis());
				file = File.createTempFile("IMG_" + timeStamp, ".jpg");
				file.deleteOnExit();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				mListener.onFailure();
			}
		}
		else if (mFileType == HikeFileType.PROFILE)
		{
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			/*
			 * Making sure the directory exists before setting a profile image
			 */
			File dir = new File(directory);
			if (!dir.exists())
			{
				dir.mkdirs();
			}

			String fileName = Utils.getTempProfileImageFileName(mOriginalName);
			final String destFilePath = directory + "/" + fileName;
			file = new File(destFilePath);
		}

		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(file);
			imageEdited.compress(Bitmap.CompressFormat.JPEG, 100, out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.flush();
					out.close();

					if (mFileType == HikeFileType.PROFILE)
					{
						HikeHandlerUtil.getInstance().postRunnableWithDelay(
								new CopyFileRunnable(file.getAbsolutePath(), HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.IMAGE_ROOT + File.separator
										+ Utils.getOriginalFile(HikeFileType.IMAGE, null), HikeFileType.IMAGE), 0);
					}
				}
				catch (IOException e)
				{
					// Do nothing
				}
			}
		}

		if (file.exists())
		{
			mListener.onComplete(file);
		}
		else
		{
			mListener.onFailure();
		}

	}

	public class CopyFileRunnable implements Runnable
	{

		private String srcPath, destPath;

		private HikeFileType fileType;

		public CopyFileRunnable(File srcPath, File destPath, HikeFileType fileType)
		{
			this.srcPath = srcPath.getAbsolutePath();
			this.destPath = destPath.getAbsolutePath();
			this.fileType = fileType;
		}

		public CopyFileRunnable(String srcPath, String destPath, HikeFileType fileType)
		{
			this.srcPath = srcPath;
			this.destPath = destPath;
			this.fileType = fileType;
		}

		@Override
		public void run()
		{
			Utils.copyFile(srcPath, destPath, fileType);
		}

	}

	private void flattenLayers()
	{

		if (imageEdited != null)
		{

			if (imageEdited.getHeight() > HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX)
			{
				imageEdited = Bitmap.createScaledBitmap(imageEdited, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, false);
			}

			Canvas canvasResult = new Canvas(imageEdited);

			sendAnalyticsFilterApplied(effectLayer.getCurrentFilter().name());

			if (doodleLayer.getBitmap() != null)
			{
				Bitmap temp = HikePhotosUtils.createBitmap(doodleLayer.getBitmap(), 0, 0, imageEdited.getWidth(), imageEdited.getHeight(), true, true, false, true);

				if (temp != null)
				{
					canvasResult.drawBitmap(temp, 0, 0, doodleLayer.getPaint());
					sendAnalyticsDoodleApplied(doodleLayer.getColor());
					HikePhotosUtils.manageBitmaps(temp);
				}
				else
				{
					Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_save), Toast.LENGTH_SHORT).show();
					IntentManager.openHomeActivity(getContext(), true);

				}
			}
		}

		saveImagetoFile();
	}

	@Override
	public void onFilterApplied(Bitmap preview)
	{

		if (preview == null)
		{
			// To Do Out Of Memory handling
			if (savingFinal)
			{
				// Move Back to Home
				Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_save), Toast.LENGTH_SHORT).show();
				IntentManager.openHomeActivity(getContext(), true);
			}
			else
			{
				Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_retry), Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			if (!savingFinal)
			{
				effectLayer.changeDisplayImage(preview);
			}
			else
			{
				savingFinal = false;
				imageEdited = preview;
				flattenLayers();
			}
		}
	}

	private void sendAnalyticsDoodleApplied(int colorHex)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(HikeConstants.HikePhotos.PHOTOS_DOODLE_COLOR_KEY, Integer.toHexString(colorHex));
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_APPLIED_DOODLE);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void sendAnalyticsFilterApplied(String filterName)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(HikeConstants.HikePhotos.PHOTOS_FILTER_NAME_KEY, filterName);
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_APPLIED_FILTER);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if(enableDoodling)
		{
			return doodleLayer.onTouch(v, event);
		}
		if(enableEffects)
		{
			return effectLayer.onTouch(v, event);
		}
			
		return false;
	}

}