package com.bsb.hike.platform;

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
import android.util.Pair;
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
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.models.MovingList;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.bridge.MessagingBridge_Alto;
import com.bsb.hike.platform.bridge.MessagingBridge_Nano;
import com.bsb.hike.platform.bridge.MessagingBridge_Nano.WebviewEventsListener;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.platform.content.PlatformRequestManager;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private static final String TAG = "WebViewCardRenderer";

    // Map having list of view holders as value added for listening to pubsub events
    public ConcurrentHashMap<String,CopyOnWriteArrayList<WebViewHolder>> webViewHolderMap = new ConcurrentHashMap<String,CopyOnWriteArrayList<WebViewHolder>>();

    // usually we have seen 3 cards will be inflated, so 3 holders will be initiated (just an optimizations)
	ArrayList<WebViewHolder> holderList = new ArrayList<WebViewCardRenderer.WebViewHolder>(3);

    private String[] pubsub = new String[]{HikePubSub.PLATFORM_CARD_ALARM, HikePubSub.MESSAGE_EVENT_RECEIVED,  HikePubSub.BOT_CREATED, HikePubSub.MAPP_CREATED};

	public WebViewCardRenderer(Activity context, MovingList<ConvMessage> convMessages, BaseAdapter adapter)
	{
		this.mContext = context;
		this.adapter = adapter;
		this.convMessages = convMessages;
		cardAlarms = new SparseArray<String>(3);
		events = new SparseArray<String>(3);
        HikeMessengerApp.getPubSub().addListeners(this, pubsub);
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
            ConvMessage viewHolderConvMessage = (ConvMessage) holder.customWebView.getTag(R.id.conv_message_key);
            CopyOnWriteArrayList<WebViewHolder> webViewHolders = webViewHolderMap.get(viewHolderConvMessage.webMetadata.getAppName());
            if(webViewHolders != null)
                webViewHolders.remove(holder);
		}
		final WebViewHolder viewHolder = (WebViewHolder) view.getTag();
		final CustomWebView web = viewHolder.customWebView;

		web.setTag(R.id.msg_id_key,((int)convMessage.getMsgID()));
        web.setTag(R.id.position_key,position);
        web.setTag(R.id.conv_message_key,convMessage);

		orientationChangeHandling(web);
		
		if (viewHolder.id != getItemId(position))
		{
			showLoadingState(viewHolder);
			viewHolder.inflationTime = System.currentTimeMillis() - startTime;

            // Fetch latest micro app by calling install v2 api based on view type (Fetch would only be required if web view card receive case occurs)
            switch (type)
			{
			case WEBVIEW_CARD:
                loadContent(position, convMessage, viewHolder, false);
			case FORWARD_WEBVIEW_CARD_SENT:
				loadContent(position, convMessage, viewHolder, false);
				break;
			case FORWARD_WEBVIEW_CARD_RECEIVED:
				fetchContent(position, convMessage, viewHolder, false);
				break;
			}
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

    /*
     * Method to fetch forward card content before loading it in web view
     */
	private void fetchContent(final int position, final ConvMessage convMessage,  WebViewHolder viewHolder, boolean isFromErrorPress)
	{
		JSONObject cardObj = convMessage.webMetadata.getCardobj();

        String appName = convMessage.webMetadata.getAppName();

		// Check if the micro app already exists on device, load the content else call the installer service
		if (new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS, PlatformContentConstants.HIKE_MICRO_APPS, appName)).exists())
		{
            // Added hard code addition here as msisdn string can always be made from app name in this way
            String msisdn = "+" + appName + "+";
            BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

            int requestedMappVersionCode = cardObj.optInt(HikePlatformConstants.MAPP_VERSION_CODE, -1);
            int currentMappVersionCode = 0;
            if (botInfo != null)
                currentMappVersionCode = botInfo.getMAppVersionCode();

            // Compare requested forwarded card mapp version code with the current mapp version code and initiate cbot request based on it
            if(requestedMappVersionCode <= currentMappVersionCode)
                loadContent(position, convMessage, viewHolder, isFromErrorPress);
            else
                initiateCBotDownload(appName, viewHolder, convMessage, position);
		}
        else
        {
            // Call initiate cbot server api here and loading webview directly for required packet request and sending other params for showing error screen for request api failure case
            initiateCBotDownload(appName, viewHolder, convMessage, position);
        }
	}

    /*
     * Method to load card content in respective web view
     */
	private void loadContent(final int position, final ConvMessage convMessage, final WebViewHolder viewHolder, boolean isFromErrorPress)
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
					if(viewHolder.customWebView.getTag(R.id.msg_id_key) instanceof Integer && (Integer)viewHolder.customWebView.getTag(R.id.msg_id_key) == uniqueId)
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
						if(viewHolder.customWebView.getTag(R.id.msg_id_key) instanceof Integer && (Integer)viewHolder.customWebView.getTag(R.id.msg_id_key) == content.getUniqueId())
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
		},isFromErrorPress);
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
        HikeMessengerApp.getPubSub().removeListeners(this, pubsub);
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
			if (object instanceof MessageEvent)
			{
				MessageEvent messageEvent = (MessageEvent) object;

				try
				{
					JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(messageEvent.getMsisdn());
					jsonObject.put(HikePlatformConstants.EVENT_DATA, messageEvent.getEventMetadata());
					jsonObject.put(HikePlatformConstants.EVENT_ID , messageEvent.getEventId());
					jsonObject.put(HikePlatformConstants.EVENT_STATUS, messageEvent.getEventStatus());

					jsonObject.put(HikePlatformConstants.EVENT_TYPE, messageEvent.getEventType());
					events.put((int) messageEvent.getMessageId(),jsonObject.toString());
					uiHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							adapter.notifyDataSetChanged(); // it will make sure eventReceived is called if required
						}
					});
				}
				catch (JSONException e)
				{
					Logger.e(tag, "JSON Exception in message event received");
				}
			}
			else
			{
				Logger.e(tag, "Expected Message in PubSub but received " + object.getClass());
			}
		}
        else if (HikePubSub.BOT_CREATED.equals(type))
        {
            // Bot creation pubsub would send a Pair of BotInfo and its isSuccess field
            if (object instanceof Pair)
            {
                BotInfo botInfo = null;
                Boolean isBotCreationSuccess = false;

                if((((Pair) object).first) instanceof  BotInfo)
                     botInfo = (BotInfo)(((Pair) object).first);

                if((((Pair) object).second) instanceof  Boolean)
                    isBotCreationSuccess = (Boolean) (((Pair) object).second);

                NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
                String appName = metadata.getAppName();

                // Precautionary check for checking if appName is null, return from here
                if(appName == null)
                    return;
                CopyOnWriteArrayList<WebViewHolder> viewHolders = webViewHolderMap.get(appName);

                if(viewHolders != null) {
                    for (WebViewHolder viewHolder : viewHolders) {
                        int position = (Integer) viewHolder.customWebView.getTag(R.id.position_key);
                        ConvMessage convMessage = (ConvMessage) viewHolder.customWebView.getTag(R.id.conv_message_key);

                        // If the required bot is successfully created, try to load that content in webview
                        if (isBotCreationSuccess)
                            loadContent(position, convMessage, viewHolder, false);
                        else
                            showConnErrState(viewHolder, convMessage, position);
                    }
                }
            }
        }
        else if (HikePubSub.MAPP_CREATED.equals(type))
        {
            // Mapp creation pubsub would send a Pair of appName and its isSuccess field
            if (object instanceof Pair)
            {
                String appName = null;
                Boolean isMappCreationSuccess = false;

                if((((Pair) object).first) instanceof  String)
                    appName = (String)(((Pair) object).first);

                if((((Pair) object).second) instanceof  Boolean)
                    isMappCreationSuccess = (Boolean) (((Pair) object).second);

                // Precautionary check for checking if appName is null, return from here
                if(appName == null)
                    return;
                CopyOnWriteArrayList<WebViewHolder> viewHolders = webViewHolderMap.get(appName);

                if(viewHolders != null) {
                    for (WebViewHolder viewHolder : viewHolders) {
                        int position = (Integer) viewHolder.customWebView.getTag(R.id.position_key);
                        ConvMessage convMessage = (ConvMessage) viewHolder.customWebView.getTag(R.id.conv_message_key);

                        // If the required bot is successfully created, try to load that content in webview
                        if (isMappCreationSuccess)
                            loadContent(position, convMessage, viewHolder, false);
                        else
                            showConnErrState(viewHolder, convMessage, position);
                    }
                }
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
						fetchContent(position, convMessage, argViewHolder, true);
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


    /*
	 * Method to make a post call to server with necessary params requesting for initiating Cbot Sample Json to be sent in network call ::
	 * {
                  "apps":
                    {
                      "name": "hikenews",
                      "params": {
                        "enable_bot":false,
                      }
                 }
       }
	 */
    private void initiateCBotDownload(final String appName,final WebViewHolder webViewHolder,final ConvMessage convMessage,final int position)
    {
        // Json to send to install.json on server requesting for micro app download
        JSONObject json = new JSONObject();

        try
        {
            // Json object to create adding params to micro app requesting json (In our scenario, we need to receive cbot only with enable bot as false for our scenario)
            JSONObject paramsJsonObject = new JSONObject();
            paramsJsonObject.put(HikePlatformConstants.ENABLE_BOT,false);
            paramsJsonObject.put(HikePlatformConstants.NOTIF,HikePlatformConstants.SETTING_OFF);

            // Json object containing all the information required for one micro app
            JSONObject appsJsonObject = new JSONObject();
            appsJsonObject.put(HikePlatformConstants.NAME, appName);
            appsJsonObject.put(HikePlatformConstants.PARAMS,paramsJsonObject);

            // Put apps JsonObject in the final json
            json.put(HikePlatformConstants.APPS, appsJsonObject);
            json.put(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);
            json.put(HikeConstants.SOURCE, HikePlatformConstants.CARD_FORWARD);
        }
        catch (JSONException e)
        {
            Logger.d("Json Exception :: ", e.toString());
        }

        // Code for micro app request to the server
        RequestToken token = HttpRequests.forwardCardsMISPostRequest(HttpRequestConstants.getBotDownloadUrlV2(), json, new IRequestListener()
        {
            @Override
			public void onRequestSuccess(Response result)
			{
                Logger.d(TAG, "Bot download request success for " + appName + result.getBody().getContent());

				if (result.getBody().getContent() instanceof String)
				{
					String responseJsonString = (String) result.getBody().getContent();
					try
					{
						JSONObject responseJson = new JSONObject(responseJsonString);
						JSONArray apps = responseJson.optJSONArray(HikePlatformConstants.APPS);

                        // Check if server responded with updated app name , set pubsub acc to it else proceed with message appName
                        if (apps != null)
						{
							for (int i = 0; i < apps.length(); i++)
							{
                                JSONObject appsJSONObject = apps.getJSONObject(i);
                                String appStatus = appsJSONObject.optString(HikePlatformConstants.APP_STATUS,"");

                                // Load content from the conv Message if app is not found on MIS V2 call
								if (appStatus.equals(HikePlatformConstants.APP_NOT_FOUND))
								{
									loadContent(position, convMessage, webViewHolder, false);
									return;
								}
                                JSONObject appsJson = apps.getJSONObject(i);
                                String updatedAppName = appsJson.optString(HikePlatformConstants.UPDATED_APP_NAME,appName);
								JSONObject cardObj = convMessage.webMetadata.getCardobj();

								cardObj.put(HikePlatformConstants.APP_NAME, updatedAppName);
								convMessage.webMetadata.setCardobj(cardObj);
                                convMessage.webMetadata.setAppName(updatedAppName);

                                CopyOnWriteArrayList<WebViewHolder> viewHolders = webViewHolderMap.get(updatedAppName);

								// In case view holders array list is null , initialize it with an empty array list
								if (viewHolders == null)
									viewHolders = new CopyOnWriteArrayList<>();

								viewHolders.add(webViewHolder);
								webViewHolderMap.put(updatedAppName, viewHolders);
							}
						}
						else
						{
                            CopyOnWriteArrayList<WebViewHolder> viewHolders = webViewHolderMap.get(appName);

							// In case view holders array list is null , initialize it with an empty array list
							if (viewHolders == null)
								viewHolders = new CopyOnWriteArrayList<>();

							viewHolders.add(webViewHolder);
							webViewHolderMap.put(appName, viewHolders);
						}
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}

            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {

            }

            @Override
            public void onRequestFailure(HttpException httpException)
            {
                Logger.v(TAG, "Bot download request failure for " + appName);
                showConnErrState(webViewHolder,convMessage,position);
            }
        },appName + position);

        if (token != null && !token.isRequestRunning())
        {
            CopyOnWriteArrayList<WebViewHolder> viewHolders = webViewHolderMap.get(appName);

            // In case view holders array list is null , initialize it with an empty array list
            if (viewHolders == null)
                viewHolders = new CopyOnWriteArrayList<>();

            viewHolders.add(webViewHolder);
            webViewHolderMap.put(appName, viewHolders);
            token.execute();
        }
    }

}
