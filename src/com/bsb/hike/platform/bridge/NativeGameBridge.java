package com.bsb.hike.platform.bridge;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CocosGamingActivity;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

public class NativeGameBridge {

	private CocosGamingActivity activity;
	private String TAG = getClass().getSimpleName();
	private BotInfo mBotInfo;
	private Handler handler;

	public NativeGameBridge(CocosGamingActivity activity, BotInfo mBotInfo) {
		super();
		this.activity = activity;
		this.mBotInfo = mBotInfo;
		this.handler = new Handler(HikeMessengerApp.getInstance().getMainLooper()) {
			public void handleMessage(Message msg) {
				// TODO handle message
			};
		};
	}

	/**
	 * This function calls the native game with the initialization parameters.
	 * The params can be the challenge data
	 * 
	 * @param jsonData
	 */
	public native void initParams(String jsonData);

	/**
	 * 
	 * @return List of request ids associated with the game
	 */
	public String getRequestIds() {
		// TODO fetch all challenges associated with this game
		return null;
	}

	/**
	 * 
	 * @param requestId whose requestData is being queried for
	 * @return RequestData associated with the requestId
	 */
	public String getRequestData(String requestId) {
		// TODO fetch challenge data associated with a particular challengeId
		return null;
	}

	/**
	 * 
	 * @param List of requestIds whose requestData is being queried for
	 * @return RequestData associated with the list of requestIds
	 */
	public String getRequestData(List<String> requestIds) {
		// TODO fetch all challenges associated with this game
		return null;
	}

	/**
	 * 
	 * @param requestId whose requestData is to be deleted
	 * @return
	 */
	public boolean deleteRequest(String requestId) {
		// TODO delete the specific request and return the appropriate status
		return false;
	}

	public void forwardToChat(String json, String hikeMessage) {
		Logger.i(TAG, "Received this json in forward to chat : " + json + "\n Received this hm : " + hikeMessage);

		if (TextUtils.isEmpty(json) || TextUtils.isEmpty(hikeMessage)) {
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}

		try {
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(mBotInfo.getMsisdn());
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
			JSONObject cardObj = new JSONObject(json);

			/**
			 * Blindly inserting the appName in the cardObj JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());

			JSONObject webMetadata = new JSONObject();
			webMetadata.put(HikePlatformConstants.CARD_OBJECT, cardObj);
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());

			if (message != null) {
				startComPoseChatActivity(message);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startComPoseChatActivity(final ConvMessage message) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (activity != null) {
					final Intent intent = IntentFactory.getForwardIntentForConvMessage(activity, message,
							PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));
					activity.startActivity(intent);
				}
			}
		});
	}

	public void logMessage(String tag, String message) {
		Logger.d(tag, message);
	}

	public void showToastMessage(final String message) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(activity, message, Toast.LENGTH_SHORT);
			}
		});
	}
	
	/**
	 * Platform Bridge Version 0.
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	public void logAnalytics(String isUI, String subType, String json)
	{

		try
		{
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.BOT_MSISDN, mBotInfo.getMsisdn());
			jsonObject.put(AnalyticsConstants.BOT_NAME, mBotInfo.getConversationName());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
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

}
