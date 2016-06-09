package com.bsb.hike.media;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.packPreview.PackPreviewFragment;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.ImageWorker;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.Utils;

public class StickerPreviewContainer extends LinearLayout implements HikePubSub.Listener, ImageWorker.ImageLoaderListener
{
	private String[] pubSubListeners = { HikePubSub.STICKER_DOWNLOADED };

	private Handler uiHandler = new Handler(Looper.getMainLooper());

	private View gridView;

	private Sticker sticker;

	private StickerLoader stickerLoader;

	private PackPreviewFragment packPreviewFragment;

	private int gridTopLeftX, gridTopLeftY, gridBottomRightX, gridBottomRightY;

	private float previewWidth, previewHeight;

	private ImageView ivStickerPreview;

	private ProgressBar pbStickerPreview;

	public StickerPreviewContainer(Context context)
	{
		super(context, null);
		initView();
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs)
	{
		super(context, attrs, 0);
		initView();
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr, 0);
		initView();
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		initView();
	}

	private void initView()
	{
		previewWidth = StickerSearchUtils.getStickerSize() + Utils.dpToPx(35);
		previewHeight = StickerSearchUtils.getStickerSize() + Utils.dpToPx(35);

		View stickerPreview = LayoutInflater.from(getContext()).inflate(R.layout.sticker_preview, null);

		addView(stickerPreview);

		pbStickerPreview = (ProgressBar) findViewById(R.id.download_progress_bar);

		int padding = Utils.dpToPx(5);
		ivStickerPreview = (ImageView) findViewById(R.id.ivSticker);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) previewWidth, (int) previewHeight);
		stickerPreview.setLayoutParams(params);
		stickerPreview.setPadding(padding, padding, padding, padding);
		setVisibility(GONE);
	}

	public void initialise(View gridView, PackPreviewFragment packPreviewFragment)
	{
		this.gridView = gridView;
		this.packPreviewFragment = packPreviewFragment;
		calculateGridBounds();

		stickerLoader = new StickerLoader.Builder()
				.downloadLargeStickerIfNotFound(true)
				.build();
		stickerLoader.setImageFadeIn(false);
		stickerLoader.setImageLoaderListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onImageWorkSuccess(ImageView imageView)
	{
		if (imageView != null)
		{
			pbStickerPreview.setVisibility(GONE);
			ivStickerPreview.setVisibility(VISIBLE);
		}
	}

	@Override
	public void onImageWorkFailed(ImageView imageView)
	{

	}

	private void calculateGridBounds()
	{
		gridTopLeftX = 0;
		gridTopLeftY = 0;
		gridBottomRightX = gridTopLeftX + gridView.getMeasuredWidth();
		gridBottomRightY = gridTopLeftY + gridView.getMeasuredHeight();
	}

	public boolean isShowing()
	{
		return gridView != null && getVisibility() == VISIBLE;
	}

	public void show(View view, Sticker sticker)
	{
		this.sticker = sticker;
		setVisibility(VISIBLE);
		float[] xyCoordinates = computePreviewCoorinates(view);
		setX(xyCoordinates[0]);
		setY(xyCoordinates[1]);

		pbStickerPreview.setVisibility(VISIBLE);
		ivStickerPreview.setVisibility(GONE);

		stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ivStickerPreview);
		gridView.setAlpha(0.2f);
	}

	public void dismiss()
	{
		if (isShowing())
		{
			setVisibility(GONE);
			gridView.setAlpha(1f);
		}
	}

	public void releaseResources()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		uiHandler = null;
	}

	private float[] computePreviewCoorinates(View v)
	{
		float vx_0 = v.getX();
		float vy_0 = v.getY();
		float vx_1 = vx_0 + v.getWidth();
		float vy_1 = vy_0 + v.getHeight();

		float midX = (vx_0 + vx_1) / 2;
		float midY = (vy_0 + vy_1) / 2;

		float topLeftX = midX - (previewWidth / 2);
		float topLeftY = midY - (previewHeight / 2);
		float bottomRightX = midX + (previewWidth / 2);
		float bottomRightY = midY + (previewHeight / 2);

		int gridTopLeftX = 0;
		int gridTopLeftY = 0;
		int gridBottomRightX = gridTopLeftX + gridView.getMeasuredWidth();
		int gridBottomRightY = gridTopLeftY + gridView.getMeasuredHeight();

		if (topLeftX < gridTopLeftX)
		{
			topLeftX = gridTopLeftX;
		}

		if (topLeftY < gridTopLeftY)
		{
			topLeftY = gridTopLeftY;
		}

		if (bottomRightX > gridBottomRightX)
		{
			topLeftX = topLeftX - (bottomRightX - gridBottomRightX);
		}

		if (bottomRightY > gridBottomRightY)
		{
			topLeftY = topLeftY - (bottomRightY - gridBottomRightY);
		}

		float[] result = new float[2];
		result[0] = topLeftX;
		result[1] = topLeftY;

		return result;
	}

	@Override
	public void onEventReceived(String type, final Object object)
	{
		if (type.equalsIgnoreCase(HikePubSub.STICKER_DOWNLOADED))
		{
			if(uiHandler == null)
			{
				return ;
			}
			
			uiHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getVisibility() == VISIBLE)
					{
						Sticker stickerToShow = (Sticker) object;
						if (!sticker.toString().equals(stickerToShow.toString()))
						{
							return;
						}
						stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ivStickerPreview);
					}
				}
			});
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (isShowing())
		{
			dismiss();
		}
		return true;
	}
}