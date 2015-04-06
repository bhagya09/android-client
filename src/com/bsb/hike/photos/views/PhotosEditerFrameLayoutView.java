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
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.HikeFile.HikeFileType;
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
public class PhotosEditerFrameLayoutView extends FrameLayout implements OnFilterAppliedListener
{
	private CanvasImageView doodleLayer;

	private VignetteImageView vignetteLayer;

	private EffectsImageView effectLayer;

	private boolean enableDoodling, savingFinal;

	private Bitmap imageOriginal, imageEdited, imageScaled, scaledImageOriginal;

	private HikeFileType mFileType;

	private String mOriginalName;

	private HikePhotosListener mListener;

	public PhotosEditerFrameLayoutView(Context context)
	{
		super(context);
		doodleLayer = new CanvasImageView(context);
		vignetteLayer = new VignetteImageView(context);
		effectLayer = new EffectsImageView(context);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		savingFinal = false;
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		doodleLayer = new CanvasImageView(context, attrs);
		vignetteLayer = new VignetteImageView(context, attrs);
		effectLayer = new EffectsImageView(context, attrs);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		savingFinal = false;
		// TODO Auto-generated constructor stub
	}

	public PhotosEditerFrameLayoutView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		doodleLayer = new CanvasImageView(context, attrs, defStyleAttr);
		vignetteLayer = new VignetteImageView(context, attrs, defStyleAttr);
		effectLayer = new EffectsImageView(context, attrs, defStyleAttr);
		addView(effectLayer);
		addView(vignetteLayer);
		addView(doodleLayer);
		enableDoodling = false;
		savingFinal = false;
	}

	public int getThumbnailDimen()
	{
		int density = getResources().getDisplayMetrics().densityDpi;
		int dimen = 0;
		switch (density)
		{
		case DisplayMetrics.DENSITY_LOW:
		case DisplayMetrics.DENSITY_MEDIUM:
		case DisplayMetrics.DENSITY_HIGH:
			return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_MDPI;
		default:
			boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
			
			if( !hasBackKey) {
			    // Do whatever you need to do, this device has a navigation bar
				return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_MDPI;
			}

			else{
				return HikeConstants.HikePhotos.PREVIEW_THUMBNAIL_WIDTH_HDPI;
			}

		}

	}

	public Bitmap getScaledImageOriginal()
	{
		if (scaledImageOriginal == null)
		{
			scaledImageOriginal = HikePhotosUtils.createBitmap(imageOriginal, 0, 0, HikePhotosUtils.dpToPx(getContext(), getThumbnailDimen()),
					HikePhotosUtils.dpToPx(getContext(), getThumbnailDimen()), true, true, false, true);

			if(scaledImageOriginal == null)
			{
				//To Do Out Of Memory Handling
			}
			
			// scaledImageOriginal = Bitmap.createScaledBitmap(imageOriginal, HikePhotosUtils.dpToPx(getContext(),getThumbnailDimen()),
			// HikePhotosUtils.dpToPx(getContext(),getThumbnailDimen()), false);
		}
		return scaledImageOriginal;
	}

	public void setBrushWidth(int width)
	{
		doodleLayer.setStrokeWidth(width);
	}

	public void applyFilter(FilterType filter)
	{
		vignetteLayer.setFilter(filter);
		effectLayer.applyEffect(filter, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE, this);
		effectLayer.invalidate();

	}

	/**
	 * 
	 * @param FilePath
	 *            : absolute address of the file to be handled by the editor object
	 */
	public void loadImageFromFile(String FilePath)
	{
		try
		{
			imageOriginal = BitmapFactory.decodeFile(FilePath);
		}
		catch (OutOfMemoryError e)
		{
			Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();
			IntentManager.openHomeActivity(getContext(),true);
		}
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		if (width != imageOriginal.getWidth())
		{
			imageScaled = HikePhotosUtils.createBitmap(imageOriginal, 0, 0, width, width, true, true, false, true);
			if(imageScaled == null)
			{
				Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();
				IntentManager.openHomeActivity(getContext(),true);
			}
			// imageScaled = Bitmap.createScaledBitmap(imageOriginal, width, width, false);
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
		effectLayer.handleImage(bmp, false);
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

	private void flattenLayers()
	{

		if (imageEdited != null)
		{
			Canvas canvasResult = new Canvas(imageEdited);

			sendAnalyticsFilterApplied(effectLayer.getCurrentFilter().name());

			imageEdited = vignetteLayer.applyVignetteToBitmap(imageEdited);

			if (doodleLayer.getBitmap() != null)
			{
				Bitmap temp = HikePhotosUtils.createBitmap(doodleLayer.getBitmap(), 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), true, true, false, true);
				// Bitmap temp = Bitmap.createScaledBitmap(doodleLayer.getBitmap(), imageOriginal.getWidth(), imageOriginal.getHeight(), false);

				if (temp != null)
				{
					canvasResult.drawBitmap(temp, 0, 0, doodleLayer.getPaint());
					sendAnalyticsDoodleApplied(doodleLayer.getColor());
					temp.recycle();
				}
				else
				{
					Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_save), Toast.LENGTH_SHORT).show();
					IntentManager.openHomeActivity(getContext(),true);
					
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
				Toast.makeText(getContext(),  getResources().getString(R.string.photos_oom_save), Toast.LENGTH_SHORT).show();
				IntentManager.openHomeActivity(getContext(),true);
			}
			else
			{
				Toast.makeText(getContext(),getResources().getString(R.string.photos_oom_retry), Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			if (!savingFinal)
			{
				vignetteLayer.setVignetteforFilter();
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

}