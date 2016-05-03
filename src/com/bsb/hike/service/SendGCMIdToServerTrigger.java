package com.bsb.hike.service;

import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.GCMIntentService;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.userlogs.UserLogInfo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;

public class SendGCMIdToServerTrigger extends BroadcastReceiver
{

	/**
	 * This class sends the GCMID to server both pre and post activation.It uses a exponential backoff for retrying the request.
	 */

	private HikeSharedPreferenceUtil mprefs = null;

	/**
	 * This class is called when a SMS/Network change occurs preactivation.So has to maintain a single reference so that we can cancel the previous task.
	 */

	private static RunnableGCMIdToServer mGcmIdToServer = null;

	private HikeHandlerUtil mHikeHandler = null;
	
	private RequestToken requestToken;
	
	/**
	 * 
	 * This class is called when a SMS/Network change occurs preactivation.So has to maintain a single reference so that we can maintain the status  of the  task.
	 */

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (mGcmIdToServer == null)
			mGcmIdToServer = new RunnableGCMIdToServer();

		mprefs = HikeSharedPreferenceUtil.getInstance();
		mprefs.saveData(HikeMessengerApp.LAST_BACK_OFF_TIME, 0);

		mHikeHandler = HikeHandlerUtil.getInstance();
		// if (mHikeHandler != null)
		mHikeHandler.startHandlerThread();

		scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME, mGcmIdToServer);
	}

	private void scheduleNextSendToServerAction(String lastBackOffTimePref, Runnable postRunnableReference)
	{

		Logger.d(getClass().getSimpleName(), "Scheduling next " + lastBackOffTimePref + " send");

		int lastBackOffTime = mprefs.getData(lastBackOffTimePref, 0);

		lastBackOffTime = lastBackOffTime == 0 ? HikeConstants.RECONNECT_TIME : (lastBackOffTime * 2);
		lastBackOffTime = Math.min(HikeConstants.MAX_RECONNECT_TIME, lastBackOffTime);

		Logger.d(getClass().getSimpleName(), "Scheduling the next disconnect");

		mHikeHandler.removeRunnable(postRunnableReference);
		mHikeHandler.postRunnableWithDelay(postRunnableReference, lastBackOffTime * 1000);
		mprefs.saveData(lastBackOffTimePref, lastBackOffTime);
	}

	private class RunnableGCMIdToServer implements Runnable
	{
		@Override
		public void run()
		{
			sentToServer();
		}
	};

	private void sentToServer()
	{
		Logger.d(getClass().getSimpleName(), "Sending GCM ID");
		
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		final String regId = GCMRegistrar.getRegistrationId(context);

		if (regId.isEmpty())
		{
			Intent in = new Intent(HikeService.REGISTER_TO_GCM_ACTION);
			context.sendBroadcast(in);

			Logger.d(getClass().getSimpleName(), "GCM id not found");
			return;
		}

		Logger.d(getClass().getSimpleName(), "GCM id was not sent. Sending now");

		JSONObject requestBody = null;
		
		switch (mprefs.getData(HikeConstants.REGISTER_GCM_SIGNUP, 0))
		{
		case HikeConstants.REGISTEM_GCM_AFTER_SIGNUP:

			if (mprefs.getData(HikeMessengerApp.GCM_ID_SENT, false))
			{
				Logger.d(getClass().getSimpleName(), "GCM id sent");

			}
			else
			{
				requestBody = new JSONObject();
				try
				{
					// Sending the incentive id to the server.

					requestBody.put(PreloadNotificationSchedular.INCENTIVE_ID, mprefs.getData(PreloadNotificationSchedular.INCENTIVE_ID, "-1"));
					requestBody.put(GCMIntentService.DEV_TYPE, HikeConstants.ANDROID);
					requestBody.put(GCMIntentService.DEV_TOKEN, regId);
				}
				catch (JSONException e)
				{
					Logger.d(getClass().getSimpleName(), "Invalid JSON", e);
				}
				requestToken = HttpRequests.sendDeviceDetailsRequest(requestBody, getRequestListener());
				if (!requestToken.isRequestRunning())
				{
					requestToken.execute();
				}
			}
			break;
		case HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP:

			if (mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false))
			{
				Logger.d(getClass().getSimpleName(), "GCM id sent");
			}
			else
			{
				requestBody = Utils.getPostDeviceDetails(context);
				try
				{
					String oldGcmId = mprefs.getData(HikeConstants.OLD_GCM_ID,"");
					requestBody.put(GCMIntentService.DEV_TOKEN, regId);
					requestBody.put(GCMIntentService.OLD_DEV_TOKEN, oldGcmId);

					Logger.d("pa","new gcm ID is : " + regId);

					Logger.d("pa","old gcm ID is : " + oldGcmId);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				requestToken = HttpRequests.sendPreActivationRequest(requestBody, getRequestListener());
				if (!requestToken.isRequestRunning())
				{
					requestToken.execute();
				}
			}
			break;
		default:
		}
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{	
			@Override
			public void onRequestSuccess(Response result)
			{
				String newGcmID = mprefs.getData(HikeConstants.GCM_ID,"");
				if(!TextUtils.isEmpty(newGcmID))
				{
					mprefs.saveData(HikeConstants.OLD_GCM_ID, newGcmID);
				}
				Logger.d(SendGCMIdToServerTrigger.this.getClass().getSimpleName(), "Send successful");
				JSONObject response = (JSONObject) result.getBody().getContent();
				switch (mprefs.getData(HikeConstants.REGISTER_GCM_SIGNUP, 0))
				{
				case HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP:

					if (response != null)
					{
						mprefs.saveData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, true);

						/**
						 * Sample String // String x = System.currentTimeMillis() + 60000 + ""; // String y = System.currentTimeMillis() + 180000 + ""; // String r =
						 * "{  'notification_schedule':[{'timestamp':'" + x + "','incentive_id':'1','title':'hello hike','text':'20 free smms'},{'timestamp':'" + y // +
						 * "','incentive_id':'2','title':'hello hike2','text':'50 free smms'}],'stat':'ok'}";
						 */
						mprefs.saveData(PreloadNotificationSchedular.NOTIFICATION_TIMELINE, response.toString());

						PreloadNotificationSchedular.scheduleNextAlarm(HikeMessengerApp.getInstance().getApplicationContext());

						/**
						 * 
						 * DeRegistering the NetworkChange Listener as we do not require anymore to listen to network changes.
						 * 
						 * 
						 */
						try {
							String enKey = response.getString("pa_encryption_key");
							String pa_uid = response.getString("pa_uid");
							String pa_token = response.getString("pa_token");

							Logger.d("pa","pa_encryption_key : " + enKey);
							mprefs.saveData("pa_encryption_key", enKey);
							Logger.d("pa","pa_uid : " + pa_uid);
							mprefs.saveData("pa_uid", pa_uid);
							Logger.d("pa","pa_token : " + pa_token);
							mprefs.saveData("pa_token", pa_token);

						} catch (JSONException e) {
							e.printStackTrace();
						}
						//Utils.disableNetworkListner(HikeMessengerApp.getInstance().getApplicationContext());
					}
					try {
						UserLogInfo.requestUserLogs(255);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					break;
				case HikeConstants.REGISTEM_GCM_AFTER_SIGNUP:

					mprefs.saveData(HikeMessengerApp.GCM_ID_SENT, true);
					break;
				}
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{	
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.d(SendGCMIdToServerTrigger.this.getClass().getSimpleName(), "Send unsuccessful");
				// TODO Below code is for investigating an issue where invalid json is received at server end , should be removed once issue is solved
				if (httpException.getErrorCode() == HttpURLConnection.HTTP_BAD_REQUEST)
				{
					IRequestBody requestBody = requestToken.getRequestBody();
					if (requestBody instanceof JsonBody)
					{
						byte[] bytes = ((JsonBody) requestBody).getBytes();
						recordSendDeviceDetailsFailException(new String(bytes));
					}
				}
				scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME, mGcmIdToServer);
			}
		};
	};

	private void recordSendDeviceDetailsFailException(String jsonString)
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.EXCEPTION_ANALYTIS_ENABLED, false))
		{
			return;
		}
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.PAYLOAD, jsonString);
			Logger.d(getClass().getSimpleName(), "recording send device details fail event. json = " + jsonString);
			HAManager.getInstance().record(HikeConstants.EXCEPTION, HikeConstants.LogEvent.SEND_DEVICE_DETAILS, metadata);
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "invalid json");
		}
	}
}
