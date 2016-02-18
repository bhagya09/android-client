package com.bsb.hike.media;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.packPreview.PackPreviewFragment;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.Utils;

public class StickerPreviewContainer extends LinearLayout implements HikePubSub.Listener
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

	public StickerPreviewContainer(Context context)
	{
		this(context, null);
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs, int defStyleAttr)
	{
		this(context, attrs, defStyleAttr, 0);
	}

	public StickerPreviewContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		setVisibility(GONE);
	}

	public void initialise(View gridView, PackPreviewFragment packPreviewFragment)
	{
		this.gridView = gridView;
		this.packPreviewFragment = packPreviewFragment;
		calculateGridBounds();

		previewWidth = StickerSearchUtils.getStickerSize() + Utils.dpToPx(35);
		previewHeight = StickerSearchUtils.getStickerSize() + Utils.dpToPx(35);

		int padding = Utils.dpToPx(5);
		ivStickerPreview = new ImageView(getContext());
		ivStickerPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) previewWidth, (int) previewHeight);
		ivStickerPreview.setLayoutParams(params);
		ivStickerPreview.setPadding(padding, padding, padding, padding);

		addView(ivStickerPreview);

		stickerLoader = new StickerLoader(getContext(), true);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
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

		stickerLoader.loadImage(sticker.getStickerPath(), ivStickerPreview);
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
		uiHandler = null;
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
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
			uiHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getVisibility() == VISIBLE)
					{
						Sticker stickerToShow = (Sticker) object;
						if(!sticker.toString().equals(stickerToShow.toString()))
						{
							return ;
						}
						stickerLoader.loadImage(sticker.getStickerPath(), ivStickerPreview);
					}
				}
			});
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if(isShowing())
		{
			dismiss();
		}
		return true;
	}
}