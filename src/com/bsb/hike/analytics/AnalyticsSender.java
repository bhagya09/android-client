package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import twitter4j.internal.http.HttpResponseCode;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 *
 */
public class AnalyticsSender  
{
	private Context context;
	
	private static AnalyticsSender _instance;
	
	private HttpClient httpClient;

	private int retryCount;
	
	private long retryDelay = DELAY_BEFORE_RETRY;
	
	/** maximum retry counts */
	private static final int MAX_RETRY_COUNT = 3;
	
	/** delay before making first retry request(in ms) */
	private static final long DELAY_BEFORE_RETRY = 5000;		

	/**
	 * parameterized constructor of the class
	 * @param context application context
	 */
	private AnalyticsSender(Context context)
	{
		this.context = context.getApplicationContext();
	}
	
	/**
	 * static constructor of AnalyticsSender class
	 * @param context application context
	 * @return singleton instance of AnalyticsSender
	 */
	public static AnalyticsSender getInstance(Context context)
	{
		if(_instance == null)
		{
			synchronized (AnalyticsSender.class)
			{
				if(_instance == null)
				{
					_instance = new AnalyticsSender(context.getApplicationContext());
				}
			}
		}
		return _instance;
	}
	
	/**
	 * Used to check if there is analytics data logged on client
	 * @return true if there is analytics data logged, false otherwise
	 */
	private boolean isAnalyticsUploadReady()
	{
		boolean isPossible = true;
		
		// get files absolute paths
		String[] fileNames = HAManager.getFileNames(context);
		
		if(fileNames == null || fileNames.length == 0)
		{
			isPossible = false;
		}
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "DO FILES EXIT :" + isPossible);
		return isPossible;
	}
	
	/**
	 * This method sends the event log files to the server. Returns if no file is present or there is no connectivity
	 * Use this method carefully as its not thread-safe
	 */
	public void sendData()
	{
		// get files absolute paths
		String[] fileNames = HAManager.getFileNames(this.context);
		
		if(fileNames == null)
			return;
		
		int size = fileNames.length;		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Number of files to send :" + Integer.toString(size));
		
		boolean success = false;
		
		for(int i=0; i<size; i++)
		{
			success = upload(fileNames[i]);			
		}
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Upload state :" + success);
		HAManager instance = HAManager.getInstance();
		instance.setIsSendAnalyticsDataWhenConnected(!success);
		
		if(success)
		{
			long nextSchedule = instance.getWhenToSend();
			nextSchedule = System.currentTimeMillis() + instance.getAnalyticsSendFrequency() * AnalyticsConstants.ONE_MINUTE;
			instance.setNextSendTimeToPrefs(nextSchedule);
			HikeAlarmManager.setAlarm(context, nextSchedule, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, true);		
	
			// don't remove! added for testing purpose. 
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(nextSchedule);
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOAD WAS SUCCESSFUL---");
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm Date :" + cal.get(Calendar.DATE));
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));			
		}
		else
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOAD FAILED---");			
		}
	}
	
	/**
	 * Used to retry upload of the file to the server
	 * true if upload should be retried, false otherwise
	 */
	private boolean retryUpload()
	{	
		if(retryCount < MAX_RETRY_COUNT)
		{
			retryCount++;
			
			retryDelay = retryCount * retryDelay;
			
			try
			{
				Thread.sleep(retryDelay);
			}
			catch(InterruptedException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Interrupted exception while thread sleeping before retry upload");
			}
		}
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "RETRY NUMBER ::" + retryCount);

		if(retryCount >= MAX_RETRY_COUNT)
		{
			resetRetryParams();
			
			return false;
		}
		return true;
	}
	
	/**
	 * Used to reset retry parameters
	 */
	private void resetRetryParams()
	{
		retryCount = 0;
		retryDelay = DELAY_BEFORE_RETRY;
	}
	
	/**
	 * starts the analytics data upload to server as per the set alarm and schedules the next alarm
	 */
	public void startUploadAndScheduleNextAlarm()
	{
		HAManager instance = HAManager.getInstance();

		// if user is offline, save true to prefs so that logs could be sent when Internet is available
		if(!Utils.isUserOnline(context))
		{		
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is offline, set true in prefs and return");
			HAManager.getInstance().setIsSendAnalyticsDataWhenConnected(true);			
		}
		// user is connected
		else
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User is online.....");
			
			long nextSchedule = instance.getWhenToSend();

			// if there are no logs on disk, set next alarm and return 
			if(!isAnalyticsUploadReady())
			{
				nextSchedule = System.currentTimeMillis() + instance.getAnalyticsSendFrequency() * AnalyticsConstants.ONE_MINUTE;
				
				// don't remove! added for testing purpose. 
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(nextSchedule);
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm Date :" + cal.get(Calendar.DATE));
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));			
				instance.setNextSendTimeToPrefs(nextSchedule);
				HikeAlarmManager.setAlarm(context, nextSchedule, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);		
				return;
			}
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "---UPLOADING FROM ALARM ROUTE---");			
			instance.sendAnalyticsData(true, false);
		}
	}
	
	/**
	 * This method actually uploads a specific event file to the server
	 * @param fileName name of the file to be sent to the server
	 */
	private boolean upload(String fileName)
	{
		boolean wasUploadSuccessful = true;
		
		while(true)
		{
			String absolutePath = HAManager.getInstance().getAnalyticsDirectory() + File.separator + fileName;
	
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "uploading file :" + absolutePath);		
			
			SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
	
			httpClient = new DefaultHttpClient();
			httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());
			
			HttpPost postCall = new HttpPost(AccountUtils.analyticsUploadUrl);
	
			HttpResponse response = null;
			
			try 
			{
				postCall.addHeader("Connection", "Keep-Alive");
				postCall.addHeader("Content-Name", fileName);
				postCall.addHeader("Content-Encoding", "gzip");
				postCall.addHeader("Cookie", "user=" + token + ";UID=" + uId);
				postCall.setEntity(new FileEntity(new File(absolutePath), "text/plain"));

				response = httpClient.execute(postCall);			
			}
			catch(SocketTimeoutException e)
			{			
				if(!retryUpload())
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
					wasUploadSuccessful = false;
					return wasUploadSuccessful;
				}
			}
			catch(ConnectTimeoutException e)
			{
				if(!retryUpload())
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
					wasUploadSuccessful = false;
					return wasUploadSuccessful;
				}
			}
			catch (FileNotFoundException e) 
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File not found during upload.");
				wasUploadSuccessful = false;
				return wasUploadSuccessful;
			}
			catch(ClientProtocolException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ClientProtocol exception during upload.");
				wasUploadSuccessful = false;
				return wasUploadSuccessful;
			}
			catch(IOException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception during upload.");
				wasUploadSuccessful = false;
				return wasUploadSuccessful;
			}
			if (response != null)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "http response :" + response.getStatusLine());
				switch (response.getStatusLine().getStatusCode())
				{
					case HttpResponseCode.OK:
					{
						resetRetryParams();
	
						new File(absolutePath).delete();
	
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "deleted file :" + fileName);
					}				
					return wasUploadSuccessful;

					case HttpResponseCode.GATEWAY_TIMEOUT:
					case HttpResponseCode.SERVICE_UNAVAILABLE:
					case HttpResponseCode.INTERNAL_SERVER_ERROR:
					case HttpResponseCode.NOT_FOUND:
					case HttpResponseCode.BAD_GATEWAY:
					case HttpResponseCode.TOO_MANY_REQUESTS:

					if (!retryUpload())
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Exiting upload process....");
						wasUploadSuccessful = false;
					}
					return wasUploadSuccessful;
					
					case HttpResponseCode.FORBIDDEN:
					case HttpResponseCode.UNAUTHORIZED:
					{
						wasUploadSuccessful = true;
					}
					return wasUploadSuccessful;
				}
			}
			else
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "null response while uploading file to server.");
				
				if(!retryUpload())
				{
					wasUploadSuccessful = false;
				}
				return wasUploadSuccessful;
			}
		}
	}		
}

class NetworkListener extends BroadcastReceiver 
{
	Context context;
		
	public NetworkListener(Context context) 
	{
		this.context = context;
		
		this.context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));		
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{		
		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			if(Utils.isUserOnline(context))
			{
				HAManager instance = HAManager.getInstance();
				
				if(instance.isSendAnalyticsDataWhenConnected())
				{
					instance.sendAnalyticsData(true, false);
				}
			}
		}				
	}
}
