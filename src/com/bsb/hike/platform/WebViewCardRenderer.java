package com.bsb.hike.platform;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.bsb.hike.models.MovingList;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.bridge.MessagingBridge_Alto;
import com.bsb.hike.platform.bridge.MessagingBridge_Nano;
import com.bsb.hike.platform.bridge.MessagingBridge_Nano.WebviewEventsListener;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformRequestManager;
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

	private long startTime;

	Activity mContext;

	MovingList<ConvMessage> convMessages;

	BaseAdapter adapter;

	private SparseArray<String> cardAlarms;

	private SparseArray<String> events;
	
	// usually we have seen 3 cards will be inflated, so 3 holders will be initiated (just an optimizations)
	ArrayList<WebViewHolder> holderList = new ArrayList<WebViewCardRenderer.WebViewHolder>(3);

	public WebViewCardRenderer(Activity context, MovingList<ConvMessage> convMessages, BaseAdapter adapter)
	{
		this.mContext = context;
		this.adapter = adapter;
		this.convMessages = convMessages;
		cardAlarms = new SparseArray<String>(3);
		events = new SparseArray<String>(3);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.PLATFORM_CARD_ALARM, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_EVENT_RECEIVED, this);
	}
	
	public void updateMessageList(MovingList<ConvMessage> objects)
	{
		this.convMessages = objects;
	}

	public static class WebViewHolder extends MessagesAdapter.DetailViewHolder
	{
		public long id = 0;

		public long inflationTime;

		public long templatingTime;

		CustomWebView customWebView;

		MessagingBridge_Nano platformJavaScriptBridge;

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
		holder.customWebView = (CustomWebView) view.findViewById(R.id.webcontent);
		holder.selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
		holder.loadingSpinner = view.findViewById(R.id.loading_data);
		holder.cardFadeScreen = view.findViewById(R.id.card_fade_screen);
		holder.loadingFailed = view.findViewById(R.id.loading_failed);
		holder.dayStub = (ViewStub) view.findViewById(R.id.day_stub);
		holder.webViewClient = new CustomWebViewClient(convMessage, holder);
		attachJSBridge(convMessage, holder);
		webViewStates(holder);

		return holder;
	}
	
	private void attachJSBridge(ConvMessage convMessage,WebViewHolder holder)
	{
		Logger.i(tag, "ataching bridge version "+convMessage.webMetadata.getPlatformJSCompatibleVersion());

		if (convMessage.webMetadata.getPlatformJSCompatibleVersion() >= HikePlatformConstants.VERSION_ALTO)
		{
			holder.platformJavaScriptBridge = new MessagingBridge_Alto(mContext, holder.customWebView, convMessage, adapter);
		}
		else
		{
			holder.platformJavaScriptBridge = new MessagingBridge_Nano(mContext, holder.customWebView, convMessage, adapter);
		}
		holder.platformJavaScriptBridge.setListener(holder.webViewClient);
	}

	@SuppressLint("NewApi")
	private void webViewStates(WebViewHolder holder)
	{
		holder.customWebView.addJavascriptInterface(holder.platformJavaScriptBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
		holder.customWebView.setWebViewClient(holder.webViewClient);

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
		startTime = System.currentTimeMillis();
		Logger.i(tag, "get view with called with position " + position);
		int type = getItemViewType(position);
		View view = convertView;
		final ConvMessage convMessage = (ConvMessage) getItem(position);
		if (view == null)
		{
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
			viewHolder.customWebView.setTag(viewHolder);
			int height = convMessage.webMetadata.getCardHeight();
			Logger.i("HeightAnim", "minimum height given in card is =" + height);

			if (height != 0)
			{
				int minHeight = (int) (height * Utils.densityMultiplier);
				LayoutParams lp = viewHolder.customWebView.getLayoutParams();
				lp.height = minHeight;
				Logger.i("HeightAnim", position + "set height given in card is =" + minHeight);
				viewHolder.customWebView.setLayoutParams(lp);
			}
			holderList.add(viewHolder);
		}
		else
		{
			final WebViewHolder holder = (WebViewHolder) view.getTag();
			Logger.i(tag, "view reused "+ ((Integer)holder.customWebView.getTag() +" into "+((int)convMessage.getMsgID())));
		}
		final WebViewHolder viewHolder = (WebViewHolder) view.getTag();

		final CustomWebView web = viewHolder.customWebView;
		
		web.setTag(((int)convMessage.getMsgID()));

		orientationChangeHandling(web);
		
		if (viewHolder.id != getItemId(position))
		{
			showLoadingState(viewHolder);
			viewHolder.inflationTime = System.currentTimeMillis() - startTime;
			loadContent(position, convMessage, viewHolder);
		}
		else
		{
			viewHolder.inflationTime = -1;
			int mId = (int) convMessage.getMsgID();
			String alarm;
			if ((alarm = cardAlarms.get(mId))!=null)
			{
				viewHolder.platformJavaScriptBridge.alarmPlayed(alarm);
				cardAlarms.remove(mId);
			}
			String event;
			if ((event = events.get(mId))!=null)
			{
				viewHolder.platformJavaScriptBridge.eventReceived(event);
				events.remove(mId);
			}
		}

		return view;

	}

	private void loadContent(final int position, final ConvMessage convMessage, final WebViewHolder viewHolder)
	{
		Logger.i(tag, "laoding content for "+((int)convMessage.getMsgID()));
		PlatformContent.getContent(((int)convMessage.getMsgID()),convMessage.webMetadata.JSONtoString(), new PlatformContentListener<PlatformContentModel>()
		{

			@Override
			public void onEventOccured(int uniqueId,EventCode reason)
			{

				if (reason == EventCode.DOWNLOADING)
				{
					//do nothing
					Logger.e(tag, "in downloading state");
					return;
				}
				else if (reason == EventCode.LOADED)
				{
					cardLoadAnalytics(convMessage);
				}
				else
				{
					viewHolder.templatingTime = -1;
					viewHolder.id = 0;
					if((Integer)viewHolder.customWebView.getTag() == uniqueId)
					{
						Logger.e(tag, "error");
						showConnErrState(viewHolder, convMessage, position);
						HikeAnalyticsEvent.cardErrorAnalytics(reason, convMessage);
					}else{
						
					}
				}
			}

			public void onComplete(PlatformContentModel content)
			{
				Logger.i(tag, "laoding complete for "+content.getUniqueId());
				if (position < getCount())
				{
					if(content!= null && content.getFormedData()!=null)
					{
						// If webview has not been used
						if((Integer)viewHolder.customWebView.getTag() == content.getUniqueId())
						{
							viewHolder.id = getItemId(position);
							viewHolder.templatingTime = System.currentTimeMillis() - viewHolder.inflationTime - startTime;
							fillContent(content, convMessage, viewHolder);
						}
						else
						{
							Logger.e(tag, "Webview has been reused before platform content retunred templated data, so not loading");
						}
					}else{
						showConnErrState(viewHolder, convMessage, position);	
					}
				}
				else
				{
					Logger.e(tag, "Platform Content returned data view no more exist");
				}
			}
		});
	}

	private static void cardLoadAnalytics(ConvMessage message)
	{
		JSONObject platformJSON = new JSONObject();

		try
		{
			String state = message.webMetadata.getLayoutId();
			state = state.substring(0,state.length() - 5);
			String origin = Utils.conversationType(message.getMsisdn());
			platformJSON.put(AnalyticsConstants.CHAT_MSISDN, message.getMsisdn());
			platformJSON.put(AnalyticsConstants.ORIGIN, origin);
			platformJSON.put(HikePlatformConstants.CARD_TYPE, message.webMetadata.getAppName());
			platformJSON.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_LOADED);
			platformJSON.put(HikePlatformConstants.CARD_STATE, state);
			platformJSON.put(AnalyticsConstants.CONTENT_ID, message.getContentId());
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.VIEW_EVENT, platformJSON);
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

	private void fillContent(PlatformContentModel content, ConvMessage convMessage,WebViewHolder holder)
	{
		
		Logger.d("content"+holder.id, content == null ? "CONTENT IS NULL!!":""+content.getFormedData());
		holder.webViewClient.convMessage = convMessage;
		holder.platformJavaScriptBridge.updateConvMessage(convMessage);
		holder.customWebView.loadDataWithBaseURL("content://"+content.getUniqueId(), content.getFormedData(), "text/html", "UTF-8", "");
		JSONObject time = new JSONObject();
		try
		{
			time.put(HikePlatformConstants.INFLATION_TIME, holder.inflationTime);
			time.put(HikePlatformConstants.TEMPLATING_TIME, holder.templatingTime);
			holder.platformJavaScriptBridge.updateProfilingTime(time);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	private class CustomWebViewClient extends HikeWebClient implements WebviewEventsListener
	{

		ConvMessage convMessage;
		WebViewHolder holder;
		String url = "";
		public CustomWebViewClient(ConvMessage convMessage,WebViewHolder holder)
		{
			this.convMessage = convMessage;
			this.holder = holder;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			super.onPageStarted(view, url, favicon);
			try
			{
//				WebViewHolder holder = (WebViewHolder) view.getTag();
//				showLoadingState(holder);
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
			this.url = url;
			// check if webview has been reused, it mean another load is in progress 
			CookieManager.getInstance().setAcceptCookie(true);
			Logger.i(tag, "onpage finished called for "+((int)convMessage.getMsgID()) +" URL "+url);
			if(url.contains(Integer.toString(((int)convMessage.getMsgID()))))
			{
				try
				{
					showCard(holder);
					if(convMessage.webMetadata.getPlatformJSCompatibleVersion() == HikePlatformConstants.VERSION_NANO)
					{
						holder.platformJavaScriptBridge.setData();
					}

					String alarmData = convMessage.webMetadata.getAlarmData();
					Logger.d(tag, "alarm data to html is " + alarmData);
					if (!TextUtils.isEmpty(alarmData))
					{
						holder.platformJavaScriptBridge.alarmPlayed(alarmData);
						cardAlarms.remove((int) convMessage.getMsgID()); // to avoid calling from getview
					}
					String event;
					if ((event = events.get((int)convMessage.getMsgID()))!=null)
					{
						holder.platformJavaScriptBridge.eventReceived(event);
						events.remove((int) convMessage.getMsgID());
					}
					
				}
				catch (NullPointerException npe)
				{
					npe.printStackTrace();
				}
			}
			else
			{
				Logger.e(tag, "Webview onpagefinished called but webview another load is in progress");
			}
		}

		@Override
		public void loadFinished(ConvMessage message)
		{
			Logger.i(tag, "onload finsihed called from platform bridge "+url +" message: "+message.getMsgID());
		}
		
		@Override
		public void notifyDataSetChanged()
		{
			holder.id = -1;
			adapter.notifyDataSetChanged();
		}
	}

	public void onDestroy()
	{
		PlatformRequestManager.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.PLATFORM_CARD_ALARM, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_EVENT_RECEIVED, this);
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
		else if (HikePubSub.MESSAGE_EVENT_RECEIVED.equals(type))
		{
			if (object instanceof Message)
			{
				Message m = (Message) object;
				events.put(m.arg1, (String) m.obj);
				uiHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						adapter.notifyDataSetChanged(); // it will make sure eventReceived is called if required
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

	private void showLoadingState(WebViewHolder viewHolder)
	{
		if (viewHolder == null)
		{
			return;
		}

		viewHolder.loadingSpinner.setVisibility(View.VISIBLE);
		viewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
		viewHolder.loadingFailed.setVisibility(View.GONE);

	}

	private void showConnErrState(final WebViewHolder argViewHolder, final ConvMessage convMessage, final int position)
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
				}

				argViewHolder.cardFadeScreen.setVisibility(View.VISIBLE);
				argViewHolder.loadingSpinner.setVisibility(View.GONE);
				argViewHolder.loadingFailed.setVisibility(View.VISIBLE);

				argViewHolder.loadingFailed.findViewById(R.id.loading_progress_bar).setVisibility(View.GONE);
				argViewHolder.loadingFailed.findViewById(R.id.progress_bar_image).setVisibility(View.VISIBLE);
				argViewHolder.loadingFailed.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						argViewHolder.loadingFailed.findViewById(R.id.loading_progress_bar).setVisibility(View.VISIBLE);
						argViewHolder.loadingFailed.findViewById(R.id.progress_bar_image).setVisibility(View.GONE);
						loadContent(position, convMessage, argViewHolder);
					}
				});
			}
		});

	}
	
	public void showCard(final WebViewHolder argViewHolder)
	{

		if (argViewHolder == null)
		{
			return;
		}

		Logger.d(tag, " Show Card state "+(Integer)argViewHolder.customWebView.getTag());
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				{
					// Values are based on self observation
					argViewHolder.loadingSpinner.animate().alpha(0.0f).setDuration(500).setListener(new WebViewAnimationListener(argViewHolder.loadingSpinner)).start();
					argViewHolder.loadingFailed.animate().alpha(0.0f).setDuration(500).setListener(new WebViewAnimationListener(argViewHolder.loadingFailed)).start();
					argViewHolder.cardFadeScreen.animate().setStartDelay(300).setInterpolator(decInterpolator).alpha(0.0f).setDuration(1000)
							.setListener(new WebViewAnimationListener(argViewHolder.cardFadeScreen)).start();
				}
				else
				{
					argViewHolder.loadingSpinner.setVisibility(View.GONE);
					argViewHolder.loadingFailed.setVisibility(View.GONE);
					argViewHolder.cardFadeScreen.setVisibility(View.GONE);
				}
	}

	private static DecelerateInterpolator decInterpolator = new DecelerateInterpolator();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private class WebViewAnimationListener implements AnimatorListener
	{
		private View mTargetView;


		public WebViewAnimationListener(View targetView)
		{
			mTargetView = targetView;
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
				mTargetView.setVisibility(View.GONE);
				mTargetView.setAlpha(1.0f);
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
	
	/**
	 * This function is called when activity onActivityResult is called from system, it checks which platformbridge started some activity for result and
	 * then calls its onActivityResult
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public void onActivityResult(int requestCode,int resultCode, Intent data)
	{
		int platformBridgeHashcode = data.getIntExtra(JavascriptBridge.tag, -1);
		if(platformBridgeHashcode != -1)
		{
			for(WebViewHolder holder : holderList)
			{
				if(holder.platformJavaScriptBridge.hashCode() == platformBridgeHashcode)
				{
					holder.platformJavaScriptBridge.onActivityResult(requestCode,resultCode, data);
					break;
				}
			}
		}
	}
}
