package com.bsb.hike.models.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.productpopup.AtomicTipContentModel;
import com.bsb.hike.productpopup.AtomicTipManager;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

public class ConversationTip implements OnClickListener
{
	public static final int NO_TIP = -1;

	public static final int GROUP_CHAT_TIP = 1;

	public static final int STEALTH_FTUE_TIP = 2;

	public static final int RESET_STEALTH_TIP = 3;

	public static final int WELCOME_HIKE_TIP = 4;

	public static final int STEALTH_INFO_TIP = 5;

	public static final int STEALTH_UNREAD_TIP = 6;

	public static final int ATOMIC_PROFILE_PIC_TIP = 7;

	public static final int ATOMIC_FAVOURTITES_TIP = 8;

	public static final int ATOMIC_INVITE_TIP = 9;

	public static final int ATOMIC_STATUS_TIP = 10;

	public static final int ATOMIC_INFO_TIP = 11;

	public static final int ATOMIC_HTTP_TIP = 12;

	public static final int ATOMIC_APP_GENERIC_TIP = 13;

	public static final int STEALTH_REVEAL_TIP = 14;

	public static final int STEALTH_HIDE_TIP = 15;
	
	public static final int UPDATE_CRITICAL_TIP = 16;
	
	public static final int UPDATE_NORMAL_TIP = 17;
	
	public static final int INVITE_TIP = 18;

	public static final int ATOMIC_TIP = 19;
	
	public static final int REQUEST_CODE_URL_OPEN = 101;
	
	public static final int REQUEST_CODE_SEND_INVITE = 102;

	private int tipType;

	private LayoutInflater inflater;

	private Context context;

	CountDownSetter countDownSetter;

	public interface ConversationTipClickedListener
	{
		public void closeTip(int whichTip);
		public void clickTip(int whichTip);
	}

	private ConversationTipClickedListener mListener;

	public ConversationTip(Context context, ConversationTipClickedListener listener)
	{
		this.context = context;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mListener = listener;
	}

	public View getView(int whichTip)
	{
		if (whichTip == NO_TIP)
		{
			return null;
		}
		tipType = whichTip;

		View v;
		HikeSharedPreferenceUtil hikeSharedPrefInstance = HikeSharedPreferenceUtil.getInstance();
		switch (tipType)
		{
		case STEALTH_FTUE_TIP:
			v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, null, false);
			v.findViewById(R.id.close).setOnClickListener(this);
			// TODO Add animation Code here
			return v;

		case RESET_STEALTH_TIP:
			v = inflater.inflate(R.layout.stealth_ftue_conversation_tip, null, false);
			v.findViewById(R.id.close).setOnClickListener(this);
			v.findViewById(R.id.full_tip).setOnClickListener(this);
			TextView headerText = (TextView) v.findViewById(R.id.tip);
			long remainingTime = HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS
					- (System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l));

			if (remainingTime <= 0)
			{
				headerText.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
			}
			else
			{
				if (countDownSetter == null)
				{
					countDownSetter = new CountDownSetter(headerText, remainingTime, 1000);
					countDownSetter.start();

					setTimeRemainingText(headerText, remainingTime);
				}
				else
				{
					countDownSetter.setTextView(headerText);
				}

			}
			return v;

		case WELCOME_HIKE_TIP:
			v = inflater.inflate(R.layout.welcome_hike_tip, null, false);
			((TextView) v.findViewById(R.id.tip_header)).setText(R.string.new_ui_welcome_tip_header);
			((TextView) v.findViewById(R.id.tip_msg)).setText(R.string.new_ui_welcome_tip_msg);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case STEALTH_INFO_TIP:
			v = inflater.inflate(R.layout.stealth_unread_tip, null, false);
			((TextView) v.findViewById(R.id.tip_header)).setText(R.string.stealth_info_tip_header);
			((TextView) v.findViewById(R.id.tip_msg)).setText(R.string.stealth_info_tip_subtext);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case STEALTH_UNREAD_TIP:
			v = inflater.inflate(R.layout.stealth_unread_tip, null, false);
			String headerTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_HEADER, context.getString(R.string.stealth_unread_tip_header));
			String msgTxt = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_UNREAD_TIP_MESSAGE, context.getString(R.string.stealth_unread_tip_message));
			((TextView) v.findViewById(R.id.tip_header)).setText(headerTxt);
			((TextView) v.findViewById(R.id.tip_msg)).setText(msgTxt);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			v.findViewById(R.id.all_content).setOnClickListener(this);
			return v;
			
		case STEALTH_HIDE_TIP:
			v = inflater.inflate(R.layout.stealth_tip, null, false);
			((TextView) v.findViewById(R.id.tip_text)).setText(R.string.tap_to_hide_stealth_contacts);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;
			
		case STEALTH_REVEAL_TIP:
			v = inflater.inflate(R.layout.stealth_tip, null, false);
			((TextView) v.findViewById(R.id.tip_text)).setText(R.string.tap_to_reveal_stealth_contacts);
			v.findViewById(R.id.close_tip).setOnClickListener(this);
			return v;

		case ATOMIC_PROFILE_PIC_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_profile);
			return v;
		case ATOMIC_FAVOURTITES_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_favorites);
			return v;
		case ATOMIC_INVITE_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_rewards);
			return v;
		case ATOMIC_STATUS_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_status_tip);
			return v;
		case ATOMIC_INFO_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_information);
			v.findViewById(R.id.all_content).setClickable(false);
			return v;
		case ATOMIC_HTTP_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageResource(R.drawable.ic_profile);
			return v;
		case ATOMIC_APP_GENERIC_TIP:
			v = generateAtomicTipViews();
			((ImageView) v.findViewById(R.id.arrow_pointer)).setImageDrawable(null);
			return v;
		case UPDATE_NORMAL_TIP:
		case UPDATE_CRITICAL_TIP:
			v = inflater.inflate(R.layout.update_tip, null, false);
			String tipHeaderText = hikeSharedPrefInstance.getData(HikeConstants.UPDATE_TIP_HEADER, context.getResources().getString(R.string.update_tip_header_text));
			String tipMsgTxt = hikeSharedPrefInstance.getData(HikeConstants.UPDATE_TIP_BODY, context.getResources().getString(R.string.update_tip_body_text));
			String tipLabelTxt = hikeSharedPrefInstance.getData(HikeConstants.UPDATE_TIP_LABEL, context.getResources().getString(R.string.tip_and_notif_update_text));
			String tipDismissTxt = hikeSharedPrefInstance.getData(HikeConstants.UPDATE_TIP_DISMISS, context.getResources().getString(R.string.tip_and_notif_later_text));
			String tipBgColor = hikeSharedPrefInstance.getData(HikeConstants.UPDATE_TIP_BG_COLOR, "");
			
			((TextView) v.findViewById(R.id.update_tip_header)).setText(tipHeaderText);
			((TextView) v.findViewById(R.id.update_tip_msg)).setText(tipMsgTxt);
			((TextView) v.findViewById(R.id.update_tip_action)).setText(tipLabelTxt);
			((ImageView) v.findViewById(R.id.update_tip_icon)).setVisibility(View.VISIBLE);
			((ImageView) v.findViewById(R.id.invite_tip_icon)).setVisibility(View.GONE);
			if(tipType == UPDATE_NORMAL_TIP)
			{
				TextView close_tip = (TextView) v.findViewById(R.id.close_tip);
				close_tip.setText(tipDismissTxt);
				close_tip.setVisibility(View.VISIBLE);
				close_tip.setOnClickListener(this);
			}
			if(!TextUtils.isEmpty(tipBgColor))
			{
				int color;
				try
				{
					color = Color.parseColor(tipBgColor);
				}

				catch (IllegalArgumentException e)
				{
					color = context.getResources().getColor(R.color.default_update_tip_bg);
					Logger.e("ConversationTip", "Seems like you sent a wrong color");
				}
				((LinearLayout) v.findViewById(R.id.tip_upper)).setBackgroundColor(color);
				((LinearLayout) v.findViewById(R.id.tip_lower)).setBackgroundColor(color);
			}			
			v.findViewById(R.id.all_content).setOnClickListener(this);
            HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP,
                    AnalyticsConstants.UPDATE_TIP_SHOWN, AnalyticsConstants.VIEW_EVENT);
			return v;
		case INVITE_TIP:
			v = inflater.inflate(R.layout.update_tip, null, false);
			String invtHeaderText = hikeSharedPrefInstance.getData(HikeConstants.INVITE_TIP_HEADER, context.getResources().getString(R.string.invite_tip_header_text));
			String invtMsgTxt = hikeSharedPrefInstance.getData(HikeConstants.INVITE_TIP_BODY, context.getResources().getString(R.string.invite_tip_body_text));
			String invtLabelTxt = hikeSharedPrefInstance.getData(HikeConstants.INVITE_TIP_LABEL, context.getResources().getString(R.string.invite_tip_bottom_text));
			String invtDismissTxt = hikeSharedPrefInstance.getData(HikeConstants.INVITE_TIP_DISMISS, context.getResources().getString(R.string.tip_and_notif_later_text));
			String invtBgColor = hikeSharedPrefInstance.getData(HikeConstants.INVITE_TIP_BG_COLOR, "");
			((TextView) v.findViewById(R.id.update_tip_header)).setText(invtHeaderText);
			((TextView) v.findViewById(R.id.update_tip_msg)).setText(invtMsgTxt);
			((TextView) v.findViewById(R.id.update_tip_action)).setText(invtLabelTxt);
			((ImageView) v.findViewById(R.id.update_tip_icon)).setVisibility(View.GONE);
			((ImageView) v.findViewById(R.id.invite_tip_icon)).setVisibility(View.VISIBLE);
			if(!TextUtils.isEmpty(invtBgColor))
			{
				((LinearLayout) v.findViewById(R.id.tip_upper)).setBackgroundColor(Color.parseColor(invtBgColor));
				((LinearLayout) v.findViewById(R.id.tip_lower)).setBackgroundColor(Color.parseColor(invtBgColor));
			}
			TextView close_tip = (TextView) v.findViewById(R.id.close_tip);
			close_tip.setText(invtDismissTxt);
			close_tip.setVisibility(View.VISIBLE);
			close_tip.setOnClickListener(this);
			v.findViewById(R.id.all_content).setOnClickListener(this);
            HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_INVITE_TIP,
                    AnalyticsConstants.INVITE_TIP_SHOWN, AnalyticsConstants.VIEW_EVENT);
			return v;
		case ATOMIC_TIP:
			AtomicTipManager atomicTipManager = AtomicTipManager.getInstance();
			atomicTipManager.updateCurrentlyShowing();
			v = atomicTipManager.getAtomicTipView();
			if(v != null)
			{
				v.findViewById(R.id.all_content).setOnClickListener(this);
				if(atomicTipManager.isTipCancellable())
				{
					v.findViewById(R.id.close_tip).setOnClickListener(this);
				}
			}
			return v;
		default:
			tipType = NO_TIP;
			return null;
		}
	}

	private View generateAtomicTipViews()
	{
		View v = inflater.inflate(R.layout.tip_left_arrow, null, false);
		TextView header = (TextView) v.findViewById(R.id.tip_header);
		TextView subText = (TextView) v.findViewById(R.id.tip_msg);
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String headerTxt1 = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, "");
		String message = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_MAIN, "");
		header.setText(headerTxt1);
		subText.setText(message);
		v.findViewById(R.id.close_tip).setOnClickListener(this);
		v.findViewById(R.id.all_content).setOnClickListener(this);
		return v;

	}

	public int getTipType()
	{
		return tipType;
	}

	public void setTipType(int tipType)
	{
		this.tipType = tipType;
	}

	public boolean isGroupChatTip()
	{
		return tipType == GROUP_CHAT_TIP;
	}

	public boolean isStealthFtueTip()
	{
		return tipType == STEALTH_FTUE_TIP;
	}

	public boolean isResetStealthTip()
	{
		return tipType == RESET_STEALTH_TIP;
	}

	public boolean isWelcomeHikeTip()
	{
		return tipType == WELCOME_HIKE_TIP;
	}

	public boolean isStealthInfoTip()
	{
		return tipType == STEALTH_INFO_TIP;
	}

	public boolean isStealthUnreadTip()
	{
		return tipType == STEALTH_UNREAD_TIP;
	}

	private class CountDownSetter extends CountDownTimer
	{
		TextView textView;

		public CountDownSetter(TextView textView, long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			this.textView = textView;
		}

		@Override
		public void onFinish()
		{
			if (textView == null)
			{
				return;
			}
			textView.setText(Html.fromHtml(context.getResources().getString(R.string.tap_to_reset_stealth_tip)));
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			if (textView == null)
			{
				return;
			}

			setTimeRemainingText(textView, millisUntilFinished);
		}

		public void setTextView(TextView tv)
		{
			this.textView = tv;
		}
	}

	private void setTimeRemainingText(TextView textView, long millisUntilFinished)
	{
		long secondsUntilFinished = millisUntilFinished / 1000;
		int minutes = (int) (secondsUntilFinished / 60);
		int seconds = (int) (secondsUntilFinished % 60);
		String text = String.format("%1$02d:%2$02d", minutes, seconds);
		textView.setText(Html.fromHtml(context.getString(R.string.reset_stealth_tip, text)));
	}

	public void resetCountDownSetter()
	{
		if (countDownSetter == null)
		{
			return;
		}

		this.countDownSetter.cancel();
		this.countDownSetter = null;
	}

	@Override
	public void onClick(View v)
	{

		if (v.getId() == R.id.all_content || v.getId() == R.id.full_tip)
		{
			switch (tipType)
			{
			case STEALTH_UNREAD_TIP:
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_UNREAD_TIP_CLICKED, null);
				if (mListener != null)
				{
					mListener.closeTip(tipType);
				}
				break;
			case RESET_STEALTH_TIP:
				if (mListener != null)
				{
					mListener.clickTip(tipType);
				}
				break;
			case UPDATE_CRITICAL_TIP:
			case UPDATE_NORMAL_TIP:
			case INVITE_TIP:
			case ATOMIC_TIP:
				if(mListener != null)
				{
					mListener.clickTip(tipType);
				}
				break;

			case ATOMIC_PROFILE_PIC_TIP:
			case ATOMIC_FAVOURTITES_TIP:
			case ATOMIC_INVITE_TIP:
			case ATOMIC_STATUS_TIP:
			case ATOMIC_HTTP_TIP:
			case ATOMIC_APP_GENERIC_TIP:
				resetAtomicPopUpKey();

			default:
				break;
			}
			
		}

		if (v.getId() == R.id.close_tip || v.getId() == R.id.close)
		{

			switch (tipType)
			{
			case RESET_STEALTH_TIP:
				resetCountDownSetter();
				Utils.cancelScheduledStealthReset();

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RESET_STEALTH_CANCEL);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}

				break;

			case STEALTH_REVEAL_TIP:
			case STEALTH_HIDE_TIP:
			case STEALTH_FTUE_TIP:
				mListener.closeTip(tipType);
				StealthModeManager.getInstance().ftuePending(false);
				break;
				
			case ATOMIC_PROFILE_PIC_TIP:
			case ATOMIC_FAVOURTITES_TIP:
			case ATOMIC_INVITE_TIP:
			case ATOMIC_STATUS_TIP:
			case ATOMIC_INFO_TIP:
			case ATOMIC_HTTP_TIP:
			case ATOMIC_APP_GENERIC_TIP:
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
				break;

			default:
				break;
			}

			if (mListener != null)
			{
				mListener.closeTip(tipType);
			}

			tipType = NO_TIP;
		}
	}

	private void resetAtomicPopUpKey()
	{
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		JSONObject metadata = new JSONObject();

		try
		{
			switch (this.tipType)
			{
			case ConversationTip.ATOMIC_FAVOURTITES_TIP:
				context.startActivity(new Intent(context, PeopleActivity.class));
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_FAVOURITES_TIP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case ConversationTip.ATOMIC_INVITE_TIP:
				context.startActivity(new Intent(context, TellAFriend.class));
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_INVITE_TIP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case ConversationTip.ATOMIC_PROFILE_PIC_TIP:
				context.startActivity(new Intent(context, ProfileActivity.class));
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_PROFILE_PIC_TIP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case ConversationTip.ATOMIC_STATUS_TIP:
				context.startActivity(new Intent(context, StatusUpdate.class));
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_STATUS_TIP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case ConversationTip.ATOMIC_HTTP_TIP:
				String url = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, null);
				if (!TextUtils.isEmpty(url))
				{
					Utils.startWebViewActivity(context, url, pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, ""));
					pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, "");
				}
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_HTTP_TIP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case ConversationTip.ATOMIC_APP_GENERIC_TIP:
				onClickGenericAppTip(pref);
				break;
			}
			if (mListener != null)
			{
				mListener.closeTip(tipType);
			}

			pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN, "");
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private void onClickGenericAppTip(HikeSharedPreferenceUtil pref)
	{
		int what = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT, -1);
		try
		{
			JSONObject metadata = new JSONObject();

			switch (what)
			{
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS:
				IntentFactory.openSetting(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_NOTIF:
				IntentFactory.openSettingNotification(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_NOTIF_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_PRIVACY:
				IntentFactory.openSettingPrivacy(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_PRIVACY_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_SMS:
				IntentFactory.openSettingSMS(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_SMS_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_SETTINGS_MEDIA:
				IntentFactory.openSettingMedia(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_SETTINGS_MEDIA_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_INVITE_FREE_SMS:
				IntentFactory.openInviteSMS(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_INVITE_FREE_SMS_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_INVITE_WATSAPP:
				if (Utils.isPackageInstalled(context, HikeConstants.PACKAGE_WATSAPP))
				{
					IntentFactory.openInviteWatsApp(context);
				}
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_INVITE_WHATSAPP_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_TIMELINE:
				IntentFactory.openTimeLine(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_TIMELINE_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_HIKE_EXTRA:
				IntentFactory.openHikeExtras(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_HIKE_EXTRA_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			case HikeConstants.ATOMIC_APP_TIP_HIKE_REWARDS:
				IntentFactory.openHikeRewards(context);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ATOMIC_APP_TIP_HIKE_REWARDS_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				break;
			default:
				return;
			}
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

}
