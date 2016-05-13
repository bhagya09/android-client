package com.bsb.hike.platform.bridge;


import android.app.Activity;
import android.content.Intent;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * This class was introduced to cater platform bridge Platform Bridge Version 1 onwards. We have introduced message id and platform Platform Bridge Version concept here.
 * 
 *  Now all communication between micro app and js bridge is made after message id Platform Bridge Version check
 *  
 *  We share message id with micro app during moustache templating 
 *
 *
 *  Platform Bridge Version Start = 0
 *  Platform Bridge Version End = ~
 */
@DoNotObfuscate
public class MessagingBridge_Alto extends MessagingBridge_Nano
{

	private static final String TAG = MessagingBridge_Alto.class.getSimpleName();

	public MessagingBridge_Alto(Activity activity, CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity, webView, convMessage, adapter);
	}

	public MessagingBridge_Alto(Activity activity, CustomWebView mWebView)
	{
		super(activity, mWebView);
	}
	
	private static final int UPDATE_METDATA = 1201; 
	
	private static final String tag = "MessagingBotBridgev2";
	


	private void notAllowedMethodCalled(String methodName)
	{
		//Logger.e(tag, "Native Error Not Allowed Methid called : "+methodName);
		mWebView.loadUrl("javascript:nativeError('" + methodName + " is not allowed to call in this Platform Bridge Version')");
	}
	
	private boolean isCorrectMessage(String messageId,String function)
	{
		try{
		if(Long.parseLong(messageId) == message.getMsgID())
		{
			return true;
		}
		}catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		Logger.e(tag, function + " called but conv message has been updated, message id did not match, got from card : " + messageId + " and current is " + message.getMsgID());
		return false;
	}


	/**
	 * @deprecated
	 *
	 * Platform Bridge Version 0
	 */
	@Override
	@JavascriptInterface
	public void deleteAlarm()
	{
		notAllowedMethodCalled("deleteAlarm");
	}
	
	/**
	 * Platform Bridge Version 1
	 * calling this function will delete the alarm associated with this javascript.
	 * @param messageId for which you want to delete alarm
	 */
	@JavascriptInterface
	public void deleteAlarm(String messageId)
	{
		MessagingBotBridgeHelper.deleteAlarm(Integer.parseInt(messageId));
	}

	
	/**
	 * @deprecated
	 * Platform Bridge Version 0
	 */
	@Override
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		notAllowedMethodCalled("forwardToChat");
	}
	
	/**
	 * Platform Bridge Version 1
	 *	Calling this function will initiate forward of the message to a friend or group.
	 * @param messageId : to validate whether you are forwarding the proper message
	 * @param json : if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 *             If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 *             and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String messageId,String json)
	{
		if(isCorrectMessage(messageId,"forwardToChat"))
		{
			super.forwardToChat(json);
		}

	}

	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 * @param height : The height of the loaded content
	 */
	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		notAllowedMethodCalled("onLoadFinished");
	}

	/**
	 * Platform Bridge Version 1
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * @param messageId :to validate whether you are calling the function for proper message
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String messageId,String height)
	{
		if(isCorrectMessage(messageId, "onLoadFinished")){
			super.onLoadFinished(height);
			if(message.webMetadata.getPlatformJSCompatibleVersion() >= HikePlatformConstants.VERSION_ALTO)
			{
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Logger.i(tag, "inside run onloadfinished "+listener);
						init();
						sendAlarmData();
					}
				});
			}
		}
	}
	
	private void sendAlarmData()
	{
		String alarmData = message.webMetadata.getAlarmData();
		Logger.d(tag, "alarm data to html is " + alarmData);
		if (!TextUtils.isEmpty(alarmData))
		{
			alarmPlayed(alarmData);
		}
			
	}


	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		notAllowedMethodCalled("setAlarm");
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.inc_unread: if inc_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'inc_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 * @param messageId:to validate whether you are setting alarm for proper message
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String messageId,String json, String timeInMills){
		if(weakActivity.get()!=null){
			MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), Integer.parseInt(messageId));
		}
	}


	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void onResize(String height)
	{
		notAllowedMethodCalled("onResize");
	}

	/**
	 * Platform Bridge Version 1
	 * Whenever the content's height is changed, the html will call this function to resize the height of the Android Webview.
	 * Calling this function is MUST, whenever the height of the content changes.
	 * @param messageId : : to validate whether you are calling the function for the proper message
	 * @param height : the new height when the content is reloaded.
	 */
	@JavascriptInterface
	public void onResize(String messageId,String height)
	{
		if (isCorrectMessage(messageId, "onResize"))
		{
			super.onResize(height);
		}
	}
	
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void deleteMessage()
	{
		notAllowedMethodCalled("deleteMessage");
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to delete the message. The message will get deleted instantaneously
	 * @param messageId
	 */
	@JavascriptInterface
	public void deleteMessage(String messageId)
	{
		MessagingBotBridgeHelper.deleteMessage(Long.parseLong(messageId), message.getMsisdn(), adapter);
	}

	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share()
	{
		notAllowedMethodCalled("share");
	}

	/**
	 * Platform Bridge Version 1
	 * @param messageId
	 */
	@JavascriptInterface
	public void share(String messageId)
	{
		if(isCorrectMessage(messageId, "share"))
		{
			super.share();
		}
	}
	
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void share(String text, String caption)
	{
		notAllowedMethodCalled("share with params");
	}

	/**
	 * Platform Bridge Version 1
	 * calling this function will share the screenshot of the webView along with the text at the top and a caption text
	 * to all social network platforms by calling the system's intent.
	 * @param messageId : to validate whether sharing for proper message
	 * @param text : heading of the image with the webView's screenshot.
	 * @param caption : intent caption
	 */
	@JavascriptInterface
	public void share(String messageId,String text, String caption)
	{
		if(isCorrectMessage(messageId, "shareWithparams"))
		{
			super.share(text, caption);
		}
	}


	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 * @param json :
	 */
	@Override
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		notAllowedMethodCalled("updateHelperData");
	}

	/**
	 * Platform Bridge Version 1
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 * @param messageId : the message id to validate whether updating helper data for proper message.
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String messageId,String json)
	{
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(Long.parseLong(messageId), json);
		if(metadata!=null)
		{
			sendMessageToUiThread(UPDATE_METDATA, Integer.parseInt(messageId), metadata);
		}
	}
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		notAllowedMethodCalled("updateMetadata");
	}

	/**
	 * Platform Bridge Version 1
	 * @param messageId : the message id to validate whether updating helper data for proper message.
	 * @param json : the json that you want to update the metadata with
	 * @param notifyScreen: if true, the adapter will be notified of the change, else there will be only db update.
	 */
	@JavascriptInterface
	public void updateMetadata(String messageId,String json, String notifyScreen)
	{
		WebMetadata metadata = MessagingBotBridgeHelper.updateMetadata(Integer.parseInt(messageId), json);
		if(metadata!=null)
		{
			sendMessageToUiThread(UPDATE_METDATA, Integer.parseInt(messageId), Boolean.valueOf(notifyScreen) ? 1 : 0, metadata);
		}
	}
	
	/**
	 * Platform Bridge Version 0
	 * @deprecated
	 */
	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		notAllowedMethodCalled("loganalytics");
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to log analytics events.
	 * @param messageId: : to validate whether you are logging for proper message
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	public void logAnalytics(String messageId,String isUI, String subType, String json)
	{
		if(isCorrectMessage(messageId, "logAnalytics"))
		{
			super.logAnalytics(isUI, subType, json);
		}
	}

	/**
	 * Platform Bridge Version 3
	 * Call this method to put data in cache. This will be a key-value pair. A microapp can have different key-value pairs
	 * in the native's cache.
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 * @param value: : the data that the app need to cache.
	 * @param nameSpace: the namespace for unique microapp.
	 */
	@JavascriptInterface
	public void putInCache(String key, String value, String nameSpace)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, nameSpace, value);
	}

	/**
	 * Platform Bridge Version 3
	 * Call this method to put bulk large data in cache. Earlier large data will be replaced by this new data and there will
	 * be only one entry per microapp.
	 * @param value: the data that the app need to cache.
	 * @param nameSpace: : the namespace for unique microapp.
	 */
	@JavascriptInterface
	public void putLargeDataInCache(String value, String nameSpace)
	{
		HikeContentDatabase.getInstance().putInContentCache(nameSpace, nameSpace, value);

	}

	/**
	 * Platform Bridge Version 3
	 * call this function to get the data from the native memory
	 * @param messageId: to validate whether you are getting cache data for proper message
	 * @param id: the id of the function that native will call to call the js .
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 */
	@JavascriptInterface
	public void getFromCache(String messageId,String id, String key)
	{
		if(isCorrectMessage(messageId, "getFromCache"))
		{
			String value = HikeContentDatabase.getInstance().getFromContentCache(key, message.getNameSpace());
			callbackToJS(id, value);
		}

	}

	/**
	 * Platform Bridge Version 3
	 * Call this function to get the bulk large data from the native memory
	 * @param messageId: : : to validate whether you are getting bulk cache data for proper message
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getLargeDataFromCache(String messageId,String id)
	{
		if(isCorrectMessage(messageId, "getLargeDataFromCache"))
		{
			String value = HikeContentDatabase.getInstance().getFromContentCache(message.getNameSpace(), message.getNameSpace());
			callbackToJS(id, value);
		}
	}
	/**
	 * Platform Bridge Version 3
	 * call this function to delete the entire caching related to the namespace of the bot.
	 * @param nameSpace : the namespace for unique microapp.
	 */
	@JavascriptInterface
	public void deleteAllCacheData(String nameSpace)
	{
		HikeContentDatabase.getInstance().deleteAllMicroAppCacheData(nameSpace);
	}

	/**
	 * Platform Bridge Version 3
	 * Call this function to delete partial cached data pertaining to the namespace of the bot, The key is  provided by Javascript
	 * @param key: the key of the saved data. Will remain unique for a unique microApp.
	 * @param nameSpace : the namespace for unique microapp.
	 */
	@JavascriptInterface
	public void deletePartialCacheData(String key, String nameSpace)
	{
		HikeContentDatabase.getInstance().deletePartialMicroAppCacheData(key, nameSpace);
	}
	
	@Override
	protected void handleUiMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_METDATA:
			if(msg.arg1 == message.getMsgID())
			{
				updateMetadata((WebMetadata) msg.obj, msg.arg2 == 1 ? "true" : "");
			}else{
				metadataMap.put(msg.arg1, (WebMetadata) msg.obj);
				Logger.e(tag, "update metadata called but message id is different, called with "+msg.arg1 + " and current is "+message.getMsgID());
			}
			break;
		default:
			super.handleUiMessage(msg);
		}
	}
	
	
	/**
	 * Platform Version 2 called by the special packet sent in the bot to delete the conversation of the particular bot
	 */
	@JavascriptInterface
	public void deleteBotConversation()
	{
		Logger.i(tag, "delete bot conversation and removing from conversation fragment");
		final Activity context = weakActivity.get();
		if (context != null)
		{
			ConversationsAdapter.removeBotMsisdn = message.getMsisdn();
			final Intent intent = Utils.getHomeActivityIntent(context);
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					context.finish();
					context.startActivity(intent);
				}
			});
		}
	}

	/**
	 * Platform Version 6
	 * Call this function to delete an event from the list of events that are shared with the microapp.
	 *
	 * @param eventId: the event that will be deleted from the shared messages table.
	 */
	@JavascriptInterface
	public void deleteEvent(String eventId)
	{
		if (TextUtils.isEmpty(eventId))
		{
			Logger.e(tag, "event can't be deleted as the event id is " + eventId);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteEvent(eventId);
	}

	/**
	 * Platform Version 6
	 * Call this function to delete all the events, be it shared data or normal event pertaining to a single message.
	 *
	 * @param messageHash : the hash of the corresponding message.
	 */
	@JavascriptInterface
	public void deleteAllEventsForMessage(String messageHash)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(tag, "the events corresponding to the message hash can't be deleted as the message hash is " + messageHash);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForMessage(messageHash);

	}

	/**
	 * Platform Version 6
	 * Call this function to delete all the events for a particular microapp, be it shared data or normal event.
	 *
	 * @param namespace: the namespace whose shared events are being asked
	 */
	@JavascriptInterface
	public void deleteAllEventsForMicroapp(String namespace)
	{
		if (TextUtils.isEmpty(namespace) || !namespace.equals(message.getNameSpace()))
		{
			Logger.e(tag, "the events corresponding to the namespace can't be deleted as the namespace is " + namespace + " and message namespace is " + message.getNameSpace());
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForNamespace(namespace);
	}

	/**
	 * Platform Version 6
	 * Call this function to get all the shared messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 * @param messageId : the message id to validate whether proper message.
	 */
	@JavascriptInterface
	public void getSharedEventsData(String functionId, String messageId)
	{

		if(isCorrectMessage(messageId, "getSharedEventsData"))
		{
			String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(message.getNameSpace(), false);
			callbackToJS(functionId, messageData);
		}

	}

	/**
	 * Platform Version 6
	 * Call this function to get all the event messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "et": the type of message. 0 if shared event, and 1 if normal event.
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 * @param messageId : the message id to validate whether proper message.
	 */
	@JavascriptInterface
	public void getAllEventsData(String functionId, String messageId)
	{

		if(isCorrectMessage(messageId, "getAllEventsData"))
		{
			String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(message.getNameSpace(), true);
			callbackToJS(functionId, messageData);
		}

	}

	/**
	 * Platform Version 6
	 * Call this function to get all the event messages data. The data is a stringified list that contains:
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "d" : the data that has been sent/received for the card message
	 * "et": the type of message. 0 if shared event, and 1 if normal event.
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId:  function id to call back to the js.
	 * @param messageId : the message id to validate whether proper message.
	 */
	@JavascriptInterface
	public void getAllEventsForMessage(String functionId, String messageId ,String messageHash)
	{
		if(isCorrectMessage(messageId, "getAllEventsData"))
		{
			String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHash(messageHash, message.getNameSpace());
			callbackToJS(functionId, eventData);
		}

	}

	@JavascriptInterface
	public void getAllEventsForMessageHashFromUser(String functionId, String messageId ,String messageHash, String fromUser)
	{
		if(isCorrectMessage(messageId, "getAllEventsData"))
		{
			String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHashFromUser(messageHash, message.getNameSpace(), fromUser);
			callbackToJS(functionId, eventData);
		}

	}

	/**
	 * Platform Version 6
	 * Call this function to send a shared message to the contacts of the user. This function when forwards the data, returns with the contact details of
	 * the users it has sent the message to.
	 * It will call JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where each JSONObject reflects one user. As of now
	 * each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1 for success NOTE : JSONArray could be null as
	 * well, a micro app has to take care of this instance
	 *
	 * @param json: if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 *             If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 *             and will add the key-value pair if the key is not present in the existent metadata.
	 * @param sharedData: the stringified json data to be shared among different bots. A mandatory field "recipients" is a must. It specifies what all namespaces
	 *                    to share the data with.
	 */
	@JavascriptInterface
	public void sendSharedMessage(String json, String sharedData)
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
			JSONObject sharedDataJson = new JSONObject(sharedData);
			sharedDataJson.put(HikePlatformConstants.EVENT_TYPE, HikePlatformConstants.SHARED_EVENT);
			message.setPlatformData(sharedDataJson);

			message.setParticipantInfoState(ConvMessage.ParticipantInfoState.NO_INFO);
			if (null == mHandler)
			{
				return;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		pickContactAndSend(message);

	}

	/**
	 * Platform version 6
	 * Call this method to send a normal event.
	 *
	 * @param messageHash : the message hash that determines the uniqueness of the card message, to which the data is being sent.
	 * @param namespace  : : the namespace of the message.
	 * @param eventData   : the stringified json data to be sent. It should contain the following things :
	 *                       "cd" : card data, "increase_unread" : true/false, "notification" : the string to be notified to the user, "notification_sound" : true/ false, play sound or not.
	 */
	@JavascriptInterface
	public void sendNormalEvent(String messageHash, String namespace, String eventData)
	{
		String botMsisdn = message.webMetadata.getParentMsisdn();
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(botMsisdn);
		PlatformUtils.sendPlatformMessageEvent(eventData, messageHash, namespace, botInfo);
	}

	/**
	 * Platform version 6
	 * Call this function to block/unblock the parent bot.
	 * @param block : Stringified boolean whether to block or unblock the parent bot.
	 */
	@JavascriptInterface
	public void blockParentBot(String block)
	{
		//Check to prevent NPE
		//java.lang.NullPointerException
		//at java.util.concurrent.ConcurrentHashMap.containsKey(ConcurrentHashMap.java:781)
		//at com.bsb.hike.bots.BotUtils.isBot(SourceFile:169)
		//at com.bsb.hike.platform.bridge.MessagingBridge_Alto.blockParentBot(SourceFile:737)
		if (TextUtils.isEmpty(message.webMetadata.getParentMsisdn())) {
			Logger.e(TAG, "block is null");
			return;
		}
		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		if (Boolean.valueOf(block))
		{
			botInfo.setBlocked(true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, botInfo.getMsisdn());
		}

		else
		{
			botInfo.setBlocked(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, botInfo.getMsisdn());
		}

	}

	/**
	 * Platform Version 6
	 * Call this method to know whether the bot pertaining to the msisdn is blocked or not.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isParentBotBlocked(String id)
	{
		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		callbackToJS(id, String.valueOf(botInfo.isBlocked()));
	}

	/**
	 * Platform Version 6
	 * Call this method to know whether the bot pertaining to the msisdn is enabled or not.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isParentBotEnabled(String id)
	{
		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}
		String value = String.valueOf(HikeConversationsDatabase.getInstance().isConversationExist(message.webMetadata.getParentMsisdn()));
		callbackToJS(id, value);
	}

	/**
	 * Platform Version 6
	 * Call this method to enable/disable bot. Enable means to show the bot in the conv list and disable is vice versa.
	 * @param enable : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void enableParentBot(String enable)
	{
		enableParentBot(enable, false);

	}

	/**
	 * Platform Version 7
	 * Call this method to mute/unmute the parent bot.
	 * @param mute : send true to mute the bot in Conversation Fragment and false to unmute.
	 */
	@JavascriptInterface
	public void muteParentBot(String mute)
	{

		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}

		Boolean muteBot = Boolean.valueOf(mute);
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		botInfo.setMute(muteBot);
		HikeConversationsDatabase.getInstance().toggleMuteBot(botInfo.getMsisdn(), muteBot);
	}

	/**
	 * Platform Version 7
	 * Call this method to know whether the bot pertaining to the parent msisdn is muted or not.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isParentBotMute(String id)
	{
		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}

		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		callbackToJS(id, String.valueOf(botInfo.isMute()));
	}

	/**
	 * Platform Version 7
	 * Call this function to get the parent bot version.
	 * @param id: the id of the function that native will call to call the js .
	 * returns -1 if bot not exists
	 */
	@JavascriptInterface
	public void getParentBotVersion(String id)
	{
		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			callbackToJS(id,"-1");
			return;
		}

		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		callbackToJS(id, String.valueOf(botInfo.getVersion()));
	}
	/**
	 * Platform Version 9
	 * Call this method to enable/disable bot. Enable means to show the bot in the conv list and disable is vice versa.
	 * @param enable : the id of the function that native will call to call the js .
	 * @param increaseUnread : boolean
	 */
	@JavascriptInterface
	public void enableParentBot(String enable,Boolean increaseUnread)
	{

		if (!BotUtils.isBot(message.webMetadata.getParentMsisdn()))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		boolean enableBot = Boolean.valueOf(enable);
		if (enableBot)
		{
			PlatformUtils.enableBot(botInfo, true,increaseUnread);
		}
		else
		{
			BotUtils.deleteBotConversation(botInfo.getMsisdn(), false);
		}
	}

	/**
	 * Platform Version 12
	 * Method to get list of children bots
	 */
	@JavascriptInterface
	public void getChildrenBots(String id)
	{
		BotInfo mBotInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		try {
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
			{
				String childrenBotInformation = PlatformHelper.getChildrenBots(mBotInfo.getMsisdn());
				callbackToJS(id, childrenBotInformation);
				return;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		callbackToJS(id, "[]");
	}

	/**
	 * Platform Version 12
	 * Method to get bot information as string
	 */
	@JavascriptInterface
	public void getBotInfoAsString(String id)
	{
		BotInfo mBotInfo = BotUtils.getBotInfoForBotMsisdn(message.webMetadata.getParentMsisdn());
		try
		{
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
			{
				String botInformation = BotUtils.getBotInfoAsString(mBotInfo).toString();
				callbackToJS(id, botInformation);
				return;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		callbackToJS(id, "{}");
	}

}
