package com.bsb.hike.platform.bridge;

import java.util.List;

import android.widget.Toast;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.platform.CocosGamingActivity;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.utils.Logger;

public class NativeGameBridge {

	private CocosGamingActivity activity;
	private String TAG = getClass().getSimpleName();
	private BotInfo mBotInfo;
	HikeHandlerUtil mThread;
	PlatformHelper helper;
	
    public native void platformCallback(String id, String response);
    
	public NativeGameBridge(CocosGamingActivity activity, BotInfo mBotInfo) {
		super();
		this.activity = activity;
		this.mBotInfo = mBotInfo;
		mThread = HikeHandlerUtil.getInstance();
		helper = new PlatformHelper(this.mBotInfo, this.activity);
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
	 * @param requestId
	 *            whose requestData is being queried for
	 * @return RequestData associated with the requestId
	 */
	public String getRequestData(String requestId) {
		// TODO fetch challenge data associated with a particular challengeId
		return null;
	}

	/**
	 * 
	 * @param List
	 *            of requestIds whose requestData is being queried for
	 * @return RequestData associated with the list of requestIds
	 */
	public String getRequestData(List<String> requestIds) {
		// TODO fetch all challenges associated with this game
		return null;
	}

	/**
	 * 
	 * @param requestId
	 *            whose requestData is to be deleted
	 * @return
	 */
	public boolean deleteRequest(String requestId) {
		// TODO delete the specific request and return the appropriate status
		return false;
	}

	public void forwardToChat(final String json,final String hikeMessage) {
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				helper.forwardToChat(json, hikeMessage);
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

	public void putInCache(final String key, final String value) {
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				if (mBotInfo == null)
					Logger.d(TAG, "mbotinfoisnull");
				helper.putInCache(key, value);
				Logger.d(TAG, "put in cache");
			}

		});

	}

	public void getFromCache(final String id, final String key) {
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				Logger.d(TAG, "Getting data: " + key);
				final String  cache = helper.getFromCache(id, key);
				Logger.d(TAG, "data from cache" + cache);
				// callBack function to be called here.
				activity.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						// gameCallback(id,cache);
						activity.PlatformCallback(id, cache);
					}
				});
			}

		});
	}

	/**
	 * Platform Bridge Version 0. Call this function to log analytics events.
	 * 
	 * @param isUI
	 *            : whether the event is a UI event or not. This is a string.
	 *            Send "true" or "false".
	 * @param subType
	 *            : the subtype of the event to be logged, eg. send "click", to
	 *            determine whether it is a click event.
	 * @param json
	 *            : any extra info for logging events, including the event key
	 *            that is pretty crucial for analytics.
	 */
	public void logAnalytics(final String isUI,final String subType,final String json) {
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				helper.logAnalytics(isUI, subType, json);
			}
			
		});
	}

}
