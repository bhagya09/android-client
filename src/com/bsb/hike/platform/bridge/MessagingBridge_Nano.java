package com.bsb.hike.platform.bridge;

import android.app.Activity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Mute;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
 * This class caters Platform Bridge version 0
 * 
 * Platform Bridge Version Start = 0
 * Platform Bridge Version End = 0
 */
@DoNotObfuscate
public class MessagingBridge_Nano extends JavascriptBridge
{

	public static final String tag = "platformbridge";
	
	
	protected SparseArray<WebMetadata> metadataMap = new SparseArray<WebMetadata>();
	protected Map<String, Boolean> downloadProgressForContentIdMap = new HashMap<String, Boolean>();

	public static interface WebviewEventsListener{
		public void loadFinished(ConvMessage message);
		
		public void notifyDataSetChanged(); 
	}
	
	
	ConvMessage message;

	JSONObject profilingTime;
	BaseAdapter adapter;
	WebviewEventsListener listener;

	public MessagingBridge_Nano(Activity activity,CustomWebView mWebView)
	{
		super(activity,mWebView);
	}

	public MessagingBridge_Nano(Activity activity,CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity,webView);
		this.message = convMessage;
		this.adapter = adapter;
	}


	/**
	 * Platform Bridge Version 0.
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{

		try
		{
			String msisdn = message.getMsisdn();
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.CHAT_MSISDN, msisdn);
			jsonObject.put(AnalyticsConstants.ORIGIN, Utils.conversationType(msisdn));
			jsonObject.put(HikePlatformConstants.CARD_TYPE, message.webMetadata.getAppName());
			jsonObject.put(AnalyticsConstants.CONTENT_ID, message.getContentId());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
			}
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

	protected void updateMetadata(WebMetadata metadata, String notifyScreen)
	{
		message.webMetadata = metadata;
		if (metadata!=null && notifyScreen != null && Boolean.valueOf(notifyScreen))
		{
			if (null == mHandler)
			{
				return;
			}
			mHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					if(listener!=null)
					{
						listener.notifyDataSetChanged();
					}
				}
			});
		}
	}

	

	/**
	 * Platform Bridge Version 0
	 * calling this method will forcefully mute the chat thread. The user won't receive any more
	 * notifications after calling this.
	 */
	@JavascriptInterface
	public void muteChatThread()
	{
		Mute mute = new Mute.InitBuilder(message.getMsisdn()).build();
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, mute);

	}

	/**
	 * Platform Bridge Version 0
	 * calling this method will forcefully block the chat thread. The user won't see any messages in the
	 * chat thread after calling this.
	 */
	@JavascriptInterface
	public void blockChatThread()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, message.getMsisdn());
	}

	/**
	 * Platform Bridge Version 0
	 */
	@JavascriptInterface
	public void share()
	{
		share(null, null);
	}

	
	
	public void setListener(WebviewEventsListener listener)
	{
		this.listener = listener;
	}
	

	public void updateConvMessage(ConvMessage message)
	{
			this.message = message;
			WebMetadata metadata = metadataMap.get((int)message.getMsgID());
			if( metadata !=null )
			{
				this.message.webMetadata = metadata;
				metadataMap.remove((int) message.getMsgID());
			}
	}

	
	/**
	 * Platform Bridge Version 0.
	 * Calling this function will initiate forward of the message to a friend or group.
	 *
	 * @param json : if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 *             If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 *             and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		try
		{
			Logger.i(tag, "forward to chat called " + json + " , message id=" + message.getMsgID());

			if (!TextUtils.isEmpty(json))
			{
				String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int) (message.getMsgID()), json);
				if (!TextUtils.isEmpty(updatedJSON))
				{
					message.webMetadata = new WebMetadata(updatedJSON);
				}
			}

			PlatformHelper.startComPoseChatActivity(message,weakActivity.get(),null);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Platform Bridge Version 0
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 *
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		super.onLoadFinished(height);
		try
		{
			int requiredHeightinDP = Integer.parseInt(height);
			int requiredHeightInPX = (int) (requiredHeightinDP * Utils.densityMultiplier);
			if (requiredHeightInPX != mWebView.getHeight())
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight() + " : updated in DB as well");
				// lets save in DB, so that from next time onwards we will have less flickering
				message.webMetadata.setCardHeight(requiredHeightinDP);
				HikeConversationsDatabase.getInstance().updateMetadataOfMessage(message.getMsgID(), message.webMetadata.JSONtoString());
				resizeWebview(height);
			}
			else
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight());
			}

		}
		catch (NumberFormatException ne)
		{
			ne.printStackTrace();
		}

	}
	
	public void setData()
	{
		mWebView.loadUrl("javascript:setData('" + message.getMsisdn() + "','" + message.webMetadata.getHelperData().toString() + "','" + message.isSent() + "','" +
				HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null) + "','" + AccountUtils.getAppVersion() + "')");
	}

	public void init()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			getInitJson(jsonObject, message.getMsisdn());
			jsonObject.put(HikePlatformConstants.HELPER_DATA, message.webMetadata.getHelperData());
			jsonObject.put(HikePlatformConstants.IS_SENT, message.isSent());
			jsonObject.put(HikePlatformConstants.PROFILING_TIME, profilingTime);
			jsonObject.put(HikePlatformConstants.NAMESPACE, message.getNameSpace());
			jsonObject.put(HikePlatformConstants.MESSAGE_HASH, HikeConversationsDatabase.getInstance().getMessageHashFromMessageId(message.getMsgID()));
			Logger.d(tag, "init called with:" + jsonObject.toString());
			mWebView.loadUrl("javascript:init('" + jsonObject.toString() + "')");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void alarmPlayed(String alarmData)
	{
		mWebView.loadUrl("javascript:alarmPlayed(" + "'" + alarmData + "')");
	}

	public void updateProfilingTime(JSONObject profilingTime)
	{
		this.profilingTime = profilingTime;
	}

	/**
	 * Platform Bridge Version 0
	 * call this function to delete the message. The message will get deleted instantaneously
	 */
	@JavascriptInterface
	public void deleteMessage()
	{
		MessagingBotBridgeHelper.deleteMessage(message.getMsgID(), message.getMsisdn(), adapter);
	}
	
	/**
	 * Platform Bridge Version 0
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.increase_unread: if increase_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'increase_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 *
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			if(weakActivity.get()!=null){
				MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), (int)message.getMsgID());
			}
	}

	
	/**
	 * Platform Bridge Version 0
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 *
	 * @param json:
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		Logger.i(tag, "update helper data called " + json + " , message id=" + message.getMsgID());
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(message.getMsgID(), json);
		if(metadata!=null)
		{
			message.webMetadata = metadata;
		}
	}
	
	/**
	 * Platform bridge Version 0
	 * calling this function will delete the alarm associated with this javascript.
	 */
	@JavascriptInterface
	public void deleteAlarm()
	{
		MessagingBotBridgeHelper.deleteAlarm((int) message.getMsgID());
	}

	
	/**
	 * Platform Bridge Version 0
	 * Calling this function will update the metadata. If the key is already present, it will be replaced else it will be added to the existent metadata.
	 * If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 * and will add the key-value pair if the key is not present in the existent metadata.
	 *
	 * @param json
	 * @param notifyScreen : if true, the adapter will be notified of the change, else there will be only db update.
	 */
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() + " notifyScren is " + notifyScreen);
		updateMetadata(MessagingBotBridgeHelper.updateMetadata((int) message.getMsgID(), json), notifyScreen);
	}

	public void eventReceived(String event)
	{
		mWebView.loadUrl("javascript:eventReceived(" + "'" + getEncodedDataForJS(event) + "')");
	}

	public boolean isDownloadProgressSubsribe(String platformContentId)
	{
		if(downloadProgressForContentIdMap == null)
		{
			downloadProgressForContentIdMap = new HashMap<String, Boolean>();
		}
		if(downloadProgressForContentIdMap.containsKey(platformContentId)) {
			return downloadProgressForContentIdMap.get(platformContentId);
		}
		return false;
	}

	/**
	 * Platform Bridge Version 12
	 * This method subscribes a card to get the download progress of a given platform request.
	 *
	 * @param platformContentId the platformContentId whose downloadProgress is being subscribed for
	 * @param isSubscribe : if true, the adapter will be notified of the download progress, else it won't.
	 */
	@JavascriptInterface
	public void downloadStatusSubscribe(String platformContentId, String isSubscribe)
	{
		if(TextUtils.isEmpty(platformContentId) || TextUtils.isEmpty(isSubscribe))
		{
			Logger.e(tag, "downloadStatusSubscribe() : platformContentId or isSubscribe is empty");
			return;
		}
		if(downloadProgressForContentIdMap == null)
		{
			downloadProgressForContentIdMap = new HashMap<String, Boolean>();
		}
		downloadProgressForContentIdMap.put(platformContentId, Boolean.valueOf(isSubscribe));
	}

	public void downloadStatus(final String id, final String progress)
	{
		mWebView.loadUrl("javascript:downloadStatus" + "('" + id + "','" + progress + "')");
	}

}
