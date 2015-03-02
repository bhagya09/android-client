package com.bsb.hike.platform;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformRequestManager;
import com.bsb.hike.platform.content.PlatformWebClient;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by shobhitmandloi on 14/01/15.
 */
public class WebViewCardRenderer extends BaseAdapter implements Listener
{

	static final String tag = "webviewcardRenderer";

	private static final int WEBVIEW_CARD = 0;

	private static final int FORWARD_WEBVIEW_CARD_RECEIVED = 1;

	private static final int FORWARD_WEBVIEW_CARD_SENT = 2;

	private static final int WEBVIEW_CARD_COUNT = 3;

	Context mContext;

	ArrayList<ConvMessage> convMessages;

	BaseAdapter adapter;

	private SparseArray<String> cardAlarms;
	
	// usually we have seen 3 cards will be inflated, so 3 holders will be initiated (just an optimizations)
	ArrayList<WebViewHolder> holderList = new ArrayList<WebViewCardRenderer.WebViewHolder>(3);

	public WebViewCardRenderer(Context context, ArrayList<ConvMessage> convMessages, BaseAdapter adapter)
	{
		this.mContext = context;
		this.adapter = adapter;
		this.convMessages = convMessages;
		cardAlarms = new SparseArray<String>(3);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.PLATFORM_CARD_ALARM, this);
	}

	public static class WebViewHolder extends MessagesAdapter.DetailViewHolder
	{
		long id = 0;

		CustomWebView myBrowser;

		PlatformJavaScriptBridge platformJavaScriptBridge;

		public View selectedStateOverlay;

		public View loadingSpinner;

		public View cardFadeScreen;

		public View loadingFailed;

		public CustomWebViewClient webViewClient;
		
		public View main;

		private void initializeHolderForForward(View view, boolean isReceived)
		{
			time = (TextView) view.findViewById(R.id.time);
			status = (ImageView) view.findViewById(R.id.status);
			timeStatus = (View) view.findViewById(R.id.time_status);
			messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
			messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);

			if (isReceived)
			{
				senderDetails = view.findViewById(R.id.sender_details);
				senderName = (TextView) view.findViewById(R.id.sender_name);
				senderNameUnsaved = (TextView) view.findViewById(R.id.sender_unsaved_name);
				avatarImage = (ImageView) view.findViewById(R.id.avatar);
				avatarContainer = (ViewGroup) view.findViewById(R.id.avatar_container);
			}

		}

	}

	private WebViewHolder initializeHolder(WebViewHolder holder, View view, ConvMessage convMessage)
	{
		holder.main = view;
		holder.myBrowser = (CustomWebView) view.findViewById(R.id.webcontent);
		holder.platformJavaScriptBridge = new PlatformJavaScriptBridge(mContext, holder.myBrowser, convMessage, adapter);
		holder.selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
		holder.loadingSpinner = view.findViewById(R.id.loading_data);
		holder.cardFadeScreen = view.findViewById(R.id.card_fade_screen);
		holder.loadingFailed = view.findViewById(R.id.loading_failed);
		holder.webViewClient = new CustomWebViewClient(convMessage);
		holder.dayStub = (ViewStub) view.findViewById(R.id.day_stub);
		holder.platformJavaScriptBridge = new PlatformJavaScriptBridge(mContext, holder.myBrowser, convMessage, adapter);
		webViewStates(holder);

		return holder;
	}

	private void webViewStates(WebViewHolder holder)
	{
		holder.myBrowser.setVerticalScrollBarEnabled(false);
		holder.myBrowser.setHorizontalScrollBarEnabled(false);
		holder.myBrowser.addJavascriptInterface(holder.platformJavaScriptBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
		holder.myBrowser.setWebViewClient(holder.webViewClient);
		holder.myBrowser.getSettings().setDomStorageEnabled(true);
		holder.platformJavaScriptBridge.allowUniversalAccess();
		holder.myBrowser.getSettings().setJavaScriptEnabled(true);

	}

	@Override
	public int getItemViewType(int position)
	{
		if (convMessages.get(position).getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT)
		{
			return WEBVIEW_CARD;
		}
		else if (convMessages.get(position).isSent())
		{
			return FORWARD_WEBVIEW_CARD_SENT;
		}
		else
		{
			return FORWARD_WEBVIEW_CARD_RECEIVED;
		}

	}

	@Override
	public int getViewTypeCount()
	{
		return WEBVIEW_CARD_COUNT;
	}

	@Override
	public int getCount()
	{
		return convMessages.size();
	}

	@Override
	public Object getItem(int position)
	{
		return convMessages.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return convMessages.get(position).getMsgID();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		Logger.i(tag, "get view with called with position " + position);
		int type = getItemViewType(position);
		View view = convertView;
		final ConvMessage convMessage = (ConvMessage) getItem(position);
		if (view == null)
		{
			Logger.i(tag, "view inflated");
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			WebViewHolder viewHolder = new WebViewHolder();
			switch (type)
			{
			case WEBVIEW_CARD:
				view = inflater.inflate(R.layout.html_item, parent, false);
				initializeHolder(viewHolder, view, convMessage);
				break;

			case FORWARD_WEBVIEW_CARD_SENT:
				view = inflater.inflate(R.layout.forward_html_item_sent, parent, false);
				initializeHolder(viewHolder, view, convMessage);
				viewHolder.initializeHolderForForward(view, false);
				break;

			case FORWARD_WEBVIEW_CARD_RECEIVED:
				view = inflater.inflate(R.layout.forward_html_item_received, parent, false);
				initializeHolder(viewHolder, view, convMessage);
				viewHolder.initializeHolderForForward(view, true);
				break;
			}

			view.setTag(viewHolder);
			viewHolder.myBrowser.setTag(viewHolder);
			Logger.d(tag, "inflated");
			int height = convMessage.platformWebMessageMetadata.getCardHeight();
			Logger.i("HeightAnim", "minimum height given in card is =" + height);

			if (height != 0)
			{
				int minHeight = (int) (height * Utils.densityMultiplier);
				LayoutParams lp = viewHolder.myBrowser.getLayoutParams();
				lp.height = minHeight;
				Logger.i("HeightAnim", position + "set height given in card is =" + minHeight);
				viewHolder.myBrowser.setLayoutParams(lp);
			}
			holderList.add(viewHolder);
		}
		else
		{
			Logger.i(tag, "view reused");
		}

		final WebViewHolder viewHolder = (WebViewHolder) view.getTag();

		final CustomWebView web = viewHolder.myBrowser;
		
		web.setTag(viewHolder);

		orientationChangeHandling(web);
		
		if (viewHolder.id != getItemId(position))
		{
			showLoadingState(viewHolder);

			PlatformContent.getContent(convMessage.platformWebMessageMetadata.JSONtoString(), new PlatformContentListener<PlatformContentModel>()
			{

				@Override
				public void onEventOccured(EventCode reason)
				{
					Logger.e(tag, "on failure called " + reason);

					if (reason == EventCode.DOWNLOADING)
					{
						//do nothing
						return;
					}
					else if (reason == EventCode.LOADED)
					{
						cardLoadAnalytics(convMessage);
					}
					else
					{
						showConnErrState(viewHolder);
						cardErrorAnalytics(reason);
					}
				}

				public void onComplete(PlatformContentModel content)
				{
					if(position < getCount())
					{
						viewHolder.id = getItemId(position);
						fillContent(web, content, convMessage, viewHolder);
					}else
					{
						Logger.e(tag, "Platform Content returned data view no more exist");
					}
				}
			});
		}
		else
		{
			Logger.i(tag, "either tag is not null ");
			int mId = (int) convMessage.getMsgID();
			String alarm;
			if ((alarm = cardAlarms.get(mId))!=null)
			{
				viewHolder.myBrowser.loadUrl("javascript:alarmPlayed(" + "'" + alarm + "')");
				cardAlarms.remove(mId);
			}
		}

		return view;

	}

	private void cardErrorAnalytics(EventCode reason)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(HikePlatformConstants.ERROR_CODE, reason.toString());
			json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BOT_ERROR);
			HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ERROR_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	private static void cardLoadAnalytics(ConvMessage message)
	{
		JSONObject platformJSON = new JSONObject();

		try
		{
			String state = message.platformWebMessageMetadata.getLayoutId();
			state = state.substring(0,state.length() - 5);
			String origin = Utils.conversationType(message.getMsisdn());
			platformJSON.put(AnalyticsConstants.CHAT_MSISDN, message.getMsisdn());
			platformJSON.put(AnalyticsConstants.ORIGIN, origin);
			platformJSON.put(HikePlatformConstants.CARD_TYPE, message.platformWebMessageMetadata.getAppName());
			platformJSON.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_LOADED);
			platformJSON.put(HikePlatformConstants.CARD_STATE, state);
			HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.UI_EVENT, AnalyticsConstants.VIEW_EVENT, platformJSON);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
		catch (IndexOutOfBoundsException ie)
		{
			ie.printStackTrace();
		}
	}

	private void orientationChangeHandling(CustomWebView web)
	{
		int orientation = Utils.getDeviceOrientation(mContext);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			LayoutParams lp =  web.getLayoutParams();
			lp.width = display.getHeight();
		}
	}

	private void fillContent(CustomWebView web, PlatformContentModel content, ConvMessage convMessage,WebViewHolder holder)
	{
		holder.webViewClient.convMessage = convMessage;
		holder.platformJavaScriptBridge.updateConvMessage(convMessage);
		Logger.d("content"+holder.id, content == null ? "CONTENT IS NULL!!":""+content.getFormedData());
		web.loadDataWithBaseURL("", content.getFormedData(), "text/html", "UTF-8", "");
	}

	private class CustomWebViewClient extends PlatformWebClient
	{

		ConvMessage convMessage;


		public CustomWebViewClient(ConvMessage convMessage)
		{
			this.convMessage = convMessage;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			super.onPageStarted(view, url, favicon);
			try
			{
				WebViewHolder holder = (WebViewHolder) view.getTag();
				showLoadingState(holder);
			}
			catch (NullPointerException npe)
			{
				npe.printStackTrace();
			}
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			CookieManager.getInstance().setAcceptCookie(true);
			Logger.d("HeightAnim", "Height of webView after loading is " + String.valueOf(view.getMeasuredHeight()) + "px");
			view.loadUrl("javascript:setData('"  + convMessage.getMsisdn() + "','"
					+ convMessage.platformWebMessageMetadata.getHelperData().toString() + "','" + convMessage.isSent() +  "')");
			String alarmData = convMessage.platformWebMessageMetadata.getAlarmData();
			Logger.d(tag, "alarm data to html is " + alarmData);
			if (!TextUtils.isEmpty(alarmData))
			{
				view.loadUrl("javascript:alarmPlayed(" + "'" + alarmData + "')");
				cardAlarms.remove((int)convMessage.getMsgID()); // to avoid calling from getview
			}
			try
			{
				WebViewHolder holder = (WebViewHolder) view.getTag();
				showCard(holder);
			}
			catch (NullPointerException npe)
			{
				npe.printStackTrace();
			}
		}
	}

	public void onDestroy()
	{
		PlatformRequestManager.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.PLATFORM_CARD_ALARM, this);
		for(WebViewHolder holder : holderList)
		{
			holder.platformJavaScriptBridge.onDestroy();
		}
		holderList.clear();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.PLATFORM_CARD_ALARM.equals(type))
		{
			if (object instanceof Message)
			{
				Message m = (Message) object;
				cardAlarms.put(m.arg1, (String) m.obj);
				uiHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						adapter.notifyDataSetChanged(); // it will make sure alarmPlayed is called if required
					}
				});
			}
			else
			{
				Logger.e(tag, "Expected Message in PubSub but received " + object.getClass());
			}
		}
	}

	// TODO Replace with HikeUiHandler utility
	static Handler uiHandler = new Handler(Looper.getMainLooper());

	private void showLoadingState(final WebViewHolder argViewHolder)
	{
		if (argViewHolder == null)
		{
			return;
		}

		Logger.d("CardState", "Loading");
		uiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				{
					// TODO Add animations here if required
					argViewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
					argViewHolder.loadingFailed.setVisibility(View.GONE);
					argViewHolder.loadingSpinner.setVisibility(View.VISIBLE);
				}
				else
				{
					argViewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
					argViewHolder.loadingFailed.setVisibility(View.GONE);
					argViewHolder.loadingSpinner.setVisibility(View.VISIBLE);
				}
			}
		});

	}

	private void showConnErrState(final WebViewHolder argViewHolder)
	{
		if (argViewHolder == null)
		{
			return;
		}

		Logger.d("CardState", "Error");
		uiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				{
					// TODO Add animations here if required
					argViewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
					argViewHolder.loadingSpinner.setVisibility(View.GONE);
					argViewHolder.loadingFailed.setVisibility(View.VISIBLE);
				}
				else
				{
					argViewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
					argViewHolder.loadingSpinner.setVisibility(View.GONE);
					argViewHolder.loadingFailed.setVisibility(View.VISIBLE);
				}
			}
		});

	}

	private void showCard(final WebViewHolder argViewHolder)
	{

		if (argViewHolder == null)
		{
			return;
		}

		Logger.d("CardState", "Card");
		uiHandler.postDelayed(new Runnable()
		{
			@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			@Override
			public void run()
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				{
					// Values are based on self observation
					argViewHolder.loadingSpinner.animate().alpha(0.0f).setDuration(500).setListener(new WebViewAnimationListener(argViewHolder.loadingSpinner, true)).start();
					argViewHolder.loadingFailed.animate().alpha(0.0f).setDuration(500).setListener(new WebViewAnimationListener(argViewHolder.loadingSpinner, true)).start();
					argViewHolder.cardFadeScreen.animate().setStartDelay(300).setInterpolator(decInterpolator).alpha(0.0f).setDuration(1000)
							.setListener(new WebViewAnimationListener(argViewHolder.loadingSpinner, true)).start();
				}
				else
				{
					argViewHolder.loadingSpinner.setVisibility(View.GONE);
					argViewHolder.loadingFailed.setVisibility(View.GONE);
					argViewHolder.cardFadeScreen.setVisibility(View.GONE);
				}
			}
		}, 300);
	}

	private static DecelerateInterpolator decInterpolator = new DecelerateInterpolator();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private class WebViewAnimationListener implements AnimatorListener
	{
		private View mTargetView;

		private boolean mShouldRemove;

		public WebViewAnimationListener(View targetView, boolean shouldRemove)
		{
			mTargetView = targetView;
			mShouldRemove = shouldRemove;
		}

		@Override
		public void onAnimationStart(Animator animation)
		{
		}

		@Override
		public void onAnimationEnd(Animator animation)
		{
			if (mTargetView != null)
			{
				mTargetView.setVisibility(mShouldRemove ? View.GONE : View.VISIBLE);
				mTargetView = null;
			}
		}

		@Override
		public void onAnimationCancel(Animator animation)
		{
		}

		@Override
		public void onAnimationRepeat(Animator animation)
		{
		}
	}
}
