package com.bsb.hike.chatthread;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * This class is a helper class which contains exhaustive set of tips which can be shown in the chat thread. The tips include Atomic tips which are server triggered as well FTUE
 * tips. Every individual chat thread has the knowledge of its own set of tips, which it passes to this helper class.
 * 
 * It can call utility methods like {@link #showTip()}, {@link #closeTip(int)} etc to hide/show tips.
 * 
 * @author piyush
 * 
 */
public class ChatThreadTips implements OnClickListener, OnTouchListener
{
	/**
	 * Atomic Tips - Tips which are server triggered
	 */
	public static final int ATOMIC_ATTACHMENT_TIP = 1;

	public static final int ATOMIC_STICKER_TIP = 2;

	public static final int ATOMIC_CHAT_THEME_TIP = 3;

	/**
	 * FTUE Tips - Tips which introduce a new feature/functionality
	 */
	public static final int STICKER_TIP = 5;
	
	public static final int STICKER_RECOMMEND_TIP = 6;
	
	public static final int STICKER_RECOMMEND_AUTO_OFF_TIP = 7;

	public static final int WT_RECOMMEND_TIP = 8;

	public static final int QUICK_SUGGESTION_RECEIVED_FIRST_TIP = 9;

	public static final int QUICK_SUGGESTION_RECEIVED_SECOND_TIP = 10;

	public static final int QUICK_SUGGESTION_RECEIVED_THIRD_TIP = 11;

	public static final int QUICK_SUGGESTION_SENT_FIRST_TIP = 12;

	public static final int QUICK_SUGGESTION_SENT_SECOND_TIP = 13;

	public static final int QUICK_SUGGESTION_SENT_THIRD_TIP = 14;

	/**
	 * Class members
	 */
	private Context mContext;

	private int tipId = -1;

	int[] mWhichTips;

	View mainView;

	View tipView;

	HikeSharedPreferenceUtil mPrefs;

	public ChatThreadTips(Context context, View view, int[] whichTipsToShow, HikeSharedPreferenceUtil prefs)
	{
		this.mContext = context;
		this.mainView = view;
		this.mWhichTips = whichTipsToShow;
		this.mPrefs = prefs;
	}

	public void showTip()
	{
		showFtueTips();
		// Is any tip open ?
		if (!isAnyTipOpen())
		{
			int newTipId = whichAtomicTipToShow();

			if (isPresentInArray(newTipId)) // Did the chat Thread pass the tip to be shown?
			{
				tipId = newTipId; // Resetting tipId

				switch (tipId)
				{
				case ATOMIC_ATTACHMENT_TIP:

					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_right_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerTop)).addView(tipView, 0);
					break;

				case ATOMIC_CHAT_THEME_TIP:
					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_middle_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerTop)).addView(tipView, 0);
					break;

				case ATOMIC_STICKER_TIP:
					tipView = LayoutInflater.from(mContext).inflate(R.layout.tip_left_arrow, null);
					((ImageView) (tipView.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_down_arrow);
					setAtomicTipContent(tipView);
					((LinearLayout) mainView.findViewById(R.id.tipContainerBottom)).addView(tipView, 0);
					break;
				}
			}
		}
	}

	private void setAtomicTipContent(View view)
	{
		((TextView) view.findViewById(R.id.tip_header)).setText(mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_CHAT, ""));
		((TextView) view.findViewById(R.id.tip_msg)).setText(mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_CHAT, ""));

		view.findViewById(R.id.close_tip).setOnClickListener(this);
	}

	private int whichAtomicTipToShow()
	{
		String key = mPrefs.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
		switch (key)
		{
		case HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT:
			return ATOMIC_ATTACHMENT_TIP;

		case HikeMessengerApp.ATOMIC_POP_UP_STICKER:
			return ATOMIC_STICKER_TIP;

		case HikeMessengerApp.ATOMIC_POP_UP_THEME:
			return ATOMIC_CHAT_THEME_TIP;

		default:
			return -1;
		}
	}

	private void showFtueTips()
	{
		showStickerFtueTip();
		if(ChatThreadUtils.isWT1RevampEnabled(mContext)) showWalkieTalkieFtueTip();
	}

	private void showWalkieTalkieFtueTip() {
		if (filterTips(WT_RECOMMEND_TIP)) {
			tipId = WT_RECOMMEND_TIP;
			setupWalkieTalkieFTUETip();
		}
	}

	/**
	 * Utility method to show the pulsating dot animation on Stickers Icon
	 */
	public void showStickerFtueTip()
	{
		/**
		 * Proceed only if the calling class had passed in the StickerTip in the list
		 */
		if (filterTips(STICKER_TIP))
		{
			tipId = STICKER_TIP;
			setupStickerFTUETip();
		}
	}
	
	/**
	 * Utility method to show sticker recommend tip
	 */
	public void showStickerRecommendFtueTip()
	{
		/**
		 * Proceed only if the calling class had passed in the StickerTip in the list
		 */
		if (filterTips(STICKER_RECOMMEND_TIP))
		{
			tipId = STICKER_RECOMMEND_TIP;
			setupStickerRecommendFTUETip();
		}
	}
	
	/**
	 * Utility method to show sticker recommend auto off tip
	 */
	public void showStickerRecommendAutopopupOffTip()
	{
		/**
		 * Proceed only if the calling class had passed in the StickerTip in the list
		 */
		if (filterTips(STICKER_RECOMMEND_AUTO_OFF_TIP))
		{
			tipId = STICKER_RECOMMEND_AUTO_OFF_TIP;
			setupStickerRecommendAutoPopupOffTip();
		}
	}

	public void showQuickStickerSuggestionsTip(int whichTip)
	{
		if(!QuickStickerSuggestionController.getInstance().isTipSeen(whichTip) && filterTips(whichTip))
		{
			tipId = whichTip;
			setupQuickStickerSuggestionsTip(whichTip);
		}
	}

	/**
	 * Used to set up pulsating dot views
	 */
	private void setupWalkieTalkieFTUETip() {
		ViewStub pulsatingDot = (ViewStub) mainView.findViewById(R.id.pulsatingDotViewStub_WT);

		if (pulsatingDot != null) {
			pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener() {

				@Override
				public void onInflate(ViewStub stub, View inflated) {
					tipView = inflated;
					startPulsatingDotAnimation(tipView);
				}
			});

			pulsatingDot.inflate();
		}
	}

	/**
	 * Used to set up pulsating dot views
	 */
	private void setupStickerFTUETip()
	{
		ViewStub pulsatingDot = (ViewStub) mainView.findViewById(R.id.pulsatingDotViewStub);
		
		if(pulsatingDot != null)
		{
			pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener()
			{

				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					tipView = inflated;
					startPulsatingDotAnimation(tipView);
				}
			});
			
			pulsatingDot.inflate();
		}
	}
	
	/**
	 * Used to start pulsating dot animation for stickers
	 * 
	 * @param view
	 */
	private void startPulsatingDotAnimation(View view)
	{
		new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring1), 0);
		new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring2), 1500);
	}

	private Runnable getPulsatingRunnable(final View view, final int viewId)
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				ImageView ringView = (ImageView) view.findViewById(viewId);
				ringView.startAnimation(HikeAnimationFactory.getPulsatingDotAnimation(0));
			}
		};
	}	
	
	/**
	 * Used to set up sticker recommendation ftue views
	 */
	private void setupStickerRecommendFTUETip()
	{
		ViewStub stickerRecommendFtue = (ViewStub) mainView.findViewById(R.id.sticker_recommendation_tip);
		
		if(stickerRecommendFtue != null)
		{
			stickerRecommendFtue.setOnInflateListener(new ViewStub.OnInflateListener()
			{

				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					tipView = inflated;
					bindStickerRecommentFtueTipView();
				}
			});
			
			stickerRecommendFtue.inflate();
		}
		else
		{
			showHiddenTip();
		}
	}
	
	private void bindStickerRecommentFtueTipView()
	{
		ImageView close  = (RecyclingImageView) mainView.findViewById(R.id.sticker_recommend_ftue_close);
		close.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				setTipSeen(STICKER_RECOMMEND_TIP);
			}
		});
	}
	
	/**
	 * Used to set up sticker recommendation ftue views
	 */
	private void setupStickerRecommendAutoPopupOffTip()
	{
		ViewStub stickerRecommendAutoOff = (ViewStub) mainView.findViewById(R.id.sticker_recommendation_auto_off_tip);
		
		if(stickerRecommendAutoOff != null)
		{
			stickerRecommendAutoOff.setOnInflateListener(new ViewStub.OnInflateListener()
			{

				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					tipView = inflated;
					bindStickerRecommendAutoPopupOffTip();
				}
			});
			
			stickerRecommendAutoOff.inflate();
		}
		else
		{
			showHiddenTip();
		}
	}
	
	private void bindStickerRecommendAutoPopupOffTip()
	{
		ImageView close  = (RecyclingImageView) mainView.findViewById(R.id.sticker_recommend_ftue_close);
		TextView tipTxt = (TextView) mainView.findViewById(R.id.sticker_recommend_ftue_text);
		tipTxt.setText(mContext.getResources().getString(R.string.sticker_recommend_auto_off_tip_text));
		close.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				setTipSeen(STICKER_RECOMMEND_AUTO_OFF_TIP);
			}
		});
	}

	private void setupQuickStickerSuggestionsTip(int whichTip)
	{
		ViewStub quickStickerSuggestionsVs = (ViewStub) mainView.findViewById(R.id.quick_suggestion_tip_view_stub);

		if(quickStickerSuggestionsVs != null)
		{
			quickStickerSuggestionsVs.inflate();
			bindQuickStickerSuggestionsTip(whichTip);
		}
		else
		{
			bindQuickStickerSuggestionsTip(whichTip);
		}
	}

	private void bindQuickStickerSuggestionsTip(final int whichTip)
	{
		String tipText = QuickStickerSuggestionController.getInstance().getTiptext(whichTip);

		final View container = mainView.findViewById(R.id.container);
		TextView tvTip = (TextView) mainView.findViewById(R.id.tip_text);
		View close = mainView.findViewById(R.id.cross_button);
		tvTip.setText(tipText);

		container.setVisibility(View.VISIBLE);
		close.setVisibility(View.GONE);

		switch (whichTip)
		{
			case QUICK_SUGGESTION_RECEIVED_FIRST_TIP:
			case QUICK_SUGGESTION_SENT_FIRST_TIP:
				showQSFirstTip(container, whichTip);
				break;
			case QUICK_SUGGESTION_RECEIVED_SECOND_TIP:
			case QUICK_SUGGESTION_SENT_SECOND_TIP:
				showQSSecondTip(tvTip, close);
				break;
			case QUICK_SUGGESTION_RECEIVED_THIRD_TIP:
			case QUICK_SUGGESTION_SENT_THIRD_TIP:
				showQSThirdTip(container);
				break;
		}

		close.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				setTipSeen(whichTip);
			}
		});
	}

	private void showQSFirstTip(View container, final int whichTip)
	{
		Animation am = AnimationUtils.loadAnimation(mainView.getContext(), R.anim.up_down_fade_in);
		am.setAnimationListener(new Animation.AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						showQuickStickerSuggestionsTip(whichTip == QUICK_SUGGESTION_RECEIVED_FIRST_TIP ? QUICK_SUGGESTION_RECEIVED_SECOND_TIP :QUICK_SUGGESTION_SENT_SECOND_TIP);
					}
				}, QuickStickerSuggestionController.QUICK_SUGGESTION_TIP_VISIBLE_TIME);
			}
		});

		container.startAnimation(am);
	}

	private void showQSSecondTip(TextView textView, View closeButton)
	{
		Animation fadeIn = AnimationUtils.loadAnimation(mainView.getContext(), R.anim.fade_in_animation);
		textView.startAnimation(fadeIn);

		closeButton.setVisibility(View.VISIBLE);
		closeButton.startAnimation(fadeIn);
	}

	private void showQSThirdTip(final View container)
	{
		Animation am = AnimationUtils.loadAnimation(mainView.getContext(), R.anim.up_down_fade_in);
		am.setAnimationListener(new Animation.AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						Animation am = HikeAnimationFactory.getUpUpPartAnimation(mainView.getContext(), container);
						container.startAnimation(am);
					}
				}, QuickStickerSuggestionController.QUICK_SUGGESTION_TIP_VISIBLE_TIME);
			}
		});

		container.startAnimation(am);
	}
	
	private boolean filterTips(int whichTip)
	{
		return isPresentInArray(whichTip) && !(seenTip(whichTip));
	}

	/**
	 * Have we seen a particular kind of tip before ?
	 * 
	 * @param whichTip
	 * @return
	 */
	public boolean seenTip(int whichTip)
	{
		switch (whichTip)
		{
		case STICKER_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWN_EMOTICON_TIP, false);
		case STICKER_RECOMMEND_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_TIP, false);
		case STICKER_RECOMMEND_AUTO_OFF_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_AUTOPOPUP_OFF_TIP, false);
		case WT_RECOMMEND_TIP:
			boolean isStickerTipSeen = mPrefs.getData(HikeMessengerApp.SHOWN_EMOTICON_TIP, false);
			/* If sticker FTUE is not seen, then its a new user and wont require an FTUE for WT
			   If sticker FTUE is seen then its existing user, we respect the sharepref value and
			   decide to show or not show WT FTUE */
			if(isStickerTipSeen) {
				return mPrefs.getData(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, false);
			} else {
				return mPrefs.saveData(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, true);
			}
		default:
			return false;
		}
	}

	private boolean isPresentInArray(int whichTip)
	{
		for (int i : mWhichTips)
		{
			if (i == whichTip)
			{
				return true;
			}
			continue;
		}
		return false;
	}

	/**
	 * Closes any other open tips
	 */
	private void closeTip()
	{
		if (tipView != null)
		{
			tipId = -1;
			tipView.setVisibility(View.GONE);
			tipView = null;
		}
	}

	/**
	 * Used to temporarily hide any open tips
	 */
	public void hideTip()
	{
		if (tipView != null && tipView.getVisibility() == View.VISIBLE && shouldHideTip())
		{
			tipView.setVisibility(View.INVISIBLE);
		}
	}

	public void hideTip(int whichTip)
	{
		if (tipId == whichTip && tipView != null && tipView.getVisibility() == View.VISIBLE && shouldHideTip())
		{
			tipView.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * There could be certain tips which do not interfere with any UI components. Hence if such a tip is showing we should not hide it.
	 * eg : Sticker_tip. This method is future safe, if let's say we need to show pulsating dots on VoIP buttons or Pin buttons.
	 * @return
	 */
	private boolean shouldHideTip()
	{
		return tipId != STICKER_TIP;
	}

	public void showHiddenTip()
	{
		if (tipView != null && tipView.getVisibility() == View.INVISIBLE)
		{
			tipView.setVisibility(View.VISIBLE);
		}
	}

	public void showHiddenTip(int whichTip)
	{
		if (tipId == whichTip && tipView != null && tipView.getVisibility() == View.INVISIBLE)
		{
			tipView.setVisibility(View.VISIBLE);
		}
	}

	public boolean isAnyTipOpen()
	{
		return tipId != -1;
	}
	
	public boolean isGivenTipShowing(int whichTip)
	{
		return (tipView != null && tipId == whichTip);
	}
	
	public boolean isGivenTipVisible(int whichTip)
	{
		return isGivenTipShowing(whichTip) && (tipView.getVisibility() == View.VISIBLE);
	}

	@Override
	public void onClick(View v)
	{
		switch (tipId)
		{
		case ATOMIC_ATTACHMENT_TIP:
		case ATOMIC_CHAT_THEME_TIP:
		case ATOMIC_STICKER_TIP:
			setTipSeen(tipId);
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		/**
		 * This is being done to eat the double tap to nudge events when one presses the tips
		 */
		return true;
	}

	/**
	 * Function to mark the tip as seen
	 * @param whichTip
	 */
	public void setTipSeen(int whichTip)
	{
		switch (whichTip)
		{
		case STICKER_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOWN_EMOTICON_TIP, true);
			closeTip();
			if (mainView.findViewById(R.id.pulsatingDot) != null)  // Safety null check.
			{
				mainView.findViewById(R.id.pulsatingDot).setVisibility(View.GONE);
			}
			break;
			
		case STICKER_RECOMMEND_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_TIP, true);
			closeTip();
			if (mainView.findViewById(R.id.sticker_recommendation_tip) != null)  // Safety null check.
			{
				mainView.findViewById(R.id.sticker_recommendation_tip).setVisibility(View.GONE);
			}
			break;
			
		case STICKER_RECOMMEND_AUTO_OFF_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOWN_STICKER_RECOMMEND_AUTOPOPUP_OFF_TIP, true);
			closeTip();
			if (mainView.findViewById(R.id.sticker_recommendation_auto_off_tip) != null)  // Safety null check.
			{
				mainView.findViewById(R.id.sticker_recommendation_auto_off_tip).setVisibility(View.GONE);
			}
			break;

		case ATOMIC_ATTACHMENT_TIP:
		case ATOMIC_CHAT_THEME_TIP:
		case ATOMIC_STICKER_TIP:
			mPrefs.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
			/**
			 * Recording click on sticker tip
			 */
			if (whichTip == ATOMIC_STICKER_TIP)
			{
				ChatThreadUtils.recordStickerFTUEClick();
			}
			closeTip();
			break;
		case WT_RECOMMEND_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, true);
			closeTip();
			if (mainView.findViewById(R.id.pulsatingDot) != null)  // Safety null check.
			{
				mainView.findViewById(R.id.pulsatingDot).setVisibility(View.GONE);
			}
			break;
		case QUICK_SUGGESTION_RECEIVED_FIRST_TIP:
		case QUICK_SUGGESTION_RECEIVED_SECOND_TIP:
		case QUICK_SUGGESTION_RECEIVED_THIRD_TIP:
		case QUICK_SUGGESTION_SENT_FIRST_TIP:
		case QUICK_SUGGESTION_SENT_SECOND_TIP:
		case QUICK_SUGGESTION_SENT_THIRD_TIP:
			if(QuickStickerSuggestionController.getInstance().isTipSeen(whichTip))
			{
				return;
			}
			View container = mainView.findViewById(R.id.container);
			Animation am = HikeAnimationFactory.getUpUpPartAnimation(mainView.getContext(), container);
			container.startAnimation(am);
			QuickStickerSuggestionController.getInstance().setFtueTipSeen(whichTip);
			break;
		}
	}
}
