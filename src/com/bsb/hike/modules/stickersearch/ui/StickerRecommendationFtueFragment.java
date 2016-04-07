package com.bsb.hike.modules.stickersearch.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.modules.stickersearch.listeners.IStickerRecommendFragmentListener;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.smartImageLoader.ImageWorker.ImageLoaderListener;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerRecommendationFtueFragment extends Fragment implements Listener, ImageLoaderListener
{
	private IStickerRecommendFragmentListener listener;
	
	private String[] pubSubListeners = {HikePubSub.STICKER_DOWNLOADED};
	
	private View stickerRecommendFtueStep1;
	
	private View stickerRecommendFtueStep2;
	
	private ImageView ivSticker;
	
	private ProgressBar pbSticker;
	
	private TextView tvHeadingFtueView2;
	
	private TextView tvSubHeadingFtueView2;
	
	private View ivShop;
	
	private View close;
	
	private View settings;
	
	private View stickerImageContainer;
	
	private StickerLoader stickerLoader;
	
	private Sticker sticker;
	
	private List<Sticker> stickerList;
	
	private String word;

	private String phrase;

	public static StickerRecommendationFtueFragment newInstance(IStickerRecommendFragmentListener istickerRecommendFragmentListener, ArrayList<Sticker> stickerList)
	{
		StickerRecommendationFtueFragment stickerRecommendationFtueFragment = new StickerRecommendationFtueFragment();
		stickerRecommendationFtueFragment.setListener(istickerRecommendFragmentListener);
		Bundle args = new Bundle();
		stickerRecommendationFtueFragment.setArguments(args);
		return stickerRecommendationFtueFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		HikeMessengerApp.getPubSub().addListeners(StickerRecommendationFtueFragment.this, pubSubListeners);

		//the sticker loader will attempt to download mini sticker if sticker not present provided the server switch is enabled other wise will download full sticker
		boolean loadMini = StickerManager.getInstance().isMiniStickersEnabled();

        setupStickerLoader(loadMini);

        stickerLoader.setImageLoaderListener(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticker_recommend_ftue, container, false);
		stickerRecommendFtueStep1 = parent.findViewById(R.id.ftueView1);
		stickerRecommendFtueStep2 = parent.findViewById(R.id.ftueView2);
		stickerImageContainer = parent.findViewById(R.id.sticker_image_container);
		ivSticker = (ImageView) parent.findViewById(R.id.sticker_image);
		pbSticker = (ProgressBar) parent.findViewById(R.id.download_progress);
		tvHeadingFtueView2 = (TextView) parent.findViewById(R.id.tvHeadingFtueView2);
		tvSubHeadingFtueView2 = (TextView) parent.findViewById(R.id.tvSubHeadingFtueView2);
		ivShop = parent.findViewById(R.id.shop_icon);
		close = parent.findViewById(R.id.sticker_recommend_popup_close);
		settings = parent.findViewById(R.id.sticker_recommend_popup_settings);

        ivSticker.setOnClickListener(stickerImageClickListener);
		ivShop.setOnClickListener(stickerShopImageClickListener);
		settings.setOnClickListener(settingsListener);
		close.setOnClickListener(closeListener);
		
		setStickerLayoutParameters();
		
		return parent;
	}
	
	@Override
	public void onDestroy()
	{
		Logger.d(StickerTagWatcher.TAG, "recommend ftue fragment on destroy called");
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		listener = null;
		stickerList = null;
		super.onDestroy();
	}
	
	private void setStickerLayoutParameters()
	{
		int stickerSize = StickerSearchUtils.getStickerSize();
		int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.sticker_recommend_sticker_image_padding);
		android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) stickerImageContainer.getLayoutParams();
		params.height = stickerSize;
		params.width = stickerSize;
		stickerImageContainer.setLayoutParams(params);
		stickerImageContainer.setPadding(padding, padding, padding, padding);
	}
	
	private void loadStickerImage(boolean stickerLoaded)
	{
		ivSticker.setScaleType(ScaleType.CENTER_INSIDE);
		stickerLoader.loadSticker(sticker, StickerConstants.StickerType.SMALL, ivSticker);
	}
	
	
	private OnClickListener stickerImageClickListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			if(listener != null)
			{
				listener.shownStickerRecommendFtue();
				listener.stickerSelected(word, phrase, sticker, 0, stickerList.size(), StickerManager.FROM_STICKER_RECOMMENDATION_FTUE, false);

				stickerRecommendFtueStep1.setVisibility(View.GONE);
				stickerRecommendFtueStep2.setVisibility(View.VISIBLE);

				float startOffset = StickerSearchUtils.getStickerSize();

				TranslateAnimation slideIn = new TranslateAnimation(startOffset, 0, 0, 0);
				slideIn.setInterpolator(new LinearInterpolator());
				slideIn.setDuration(400);
				tvHeadingFtueView2.startAnimation(slideIn);

				Animation fadein = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_animation);
				fadein.setDuration(400);
				tvSubHeadingFtueView2.startAnimation(fadein);
			}
		}
	};
	
	private OnClickListener stickerShopImageClickListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			if(!isAdded())
			{
				return ;
			}
			HAManager.getInstance().record(HikeConstants.LogEvent.STKR_SHOP_BTN_CLICKED_FROM_RECOMMENDATION_FTUE, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH);
			Intent i = IntentFactory.getStickerShopIntent(getActivity());
			getActivity().startActivity(i);
		}
	};
	
	private OnClickListener closeListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			if (listener != null)
			{
				listener.shownStickerRecommendFtue();
				listener.onCloseClicked(word, phrase, true);
			}
		}
	};

	private OnClickListener settingsListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			if (listener != null)
			{
				listener.onSettingsClicked();
			}
		}
	};
	
	public void setAndNotify(final String word, final String phrase, final List<Sticker> stickerList)
	{
		this.word = word;
		this.phrase = phrase;
		this.stickerList = stickerList;
		this.sticker = stickerList.get(0);
		
		new Handler().post(new Runnable()
		{
			
			@Override
			public void run()
			{
				pbSticker.setVisibility(View.VISIBLE);
				ivSticker.setVisibility(View.GONE);
				
				loadStickerImage(false);
				stickerRecommendFtueStep1.setVisibility(View.VISIBLE);
				stickerRecommendFtueStep2.setVisibility(View.GONE);
			}
		});
	}
	
	private void refreshStickerList()
	{
		if(!isAdded())
		{
			return;
		}
		getActivity().runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				loadStickerImage(true);
			}
		});
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.STICKER_DOWNLOADED:
			Sticker downloadedSticker = (Sticker) object;
			if(sticker != null && sticker.equals(downloadedSticker))
			{
				refreshStickerList();
			}
			break;
		default:
			break;
		}
	}
	
	public IStickerRecommendFragmentListener getListener()
	{
		return listener;
	}

	public void setListener(IStickerRecommendFragmentListener listener)
	{
		this.listener = listener;
	}
	
	public String getTappedWord()
	{
		return word;
	}

	public String getTaggedPhrase()
	{
		return phrase;
	}
	
	public boolean isFtueScreen1Visible()
	{
		return stickerRecommendFtueStep1.getVisibility() == View.VISIBLE;
	}

	@Override
	public void onImageWorkSuccess(ImageView imageView)
	{
		if(!isAdded())
		{
			return ;
		}

        pbSticker.setVisibility(View.GONE);
        ivSticker.setVisibility(View.VISIBLE);
	}

	@Override
	public void onImageWorkFailed(ImageView imageView)
	{
        if(!isAdded())
        {
            return ;
        }

        pbSticker.setVisibility(View.VISIBLE);
        ivSticker.setVisibility(View.GONE);
	}

    public void setupStickerLoader(boolean loadMini)
    {
        this.stickerLoader = new StickerLoader.Builder()
                .downloadLargeStickerIfNotFound(!loadMini)
                .loadMiniStickerIfNotFound(loadMini)
                .downloadMiniStickerIfNotFound(loadMini)
                .build();
    }
}
