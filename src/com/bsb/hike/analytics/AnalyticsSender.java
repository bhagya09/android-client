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

import twitter4j.internal.http.HttpResponseCode;
import android.content.Context;
import android.content.SharedPreferences;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 *
 */
public class AnalyticsSender implements Runnable  
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
	
	public boolean isAnalyticsUploadReady()
	{
		boolean isPossible = true;
		
		// get files absolute paths
		String[] fileNames = getFileNames(context);
		
		if(fileNames == null || !(Utils.isUserOnline(context)))
			isPossible = false;	
		
		return isPossible;
	}
	
	/**
	 * This method sends the event log files to the server. Returns if no file is present or there is no connectivity
	 */
	private synchronized void sendData()
	{
		// get files absolute paths
		String[] fileNames = getFileNames(this.context);
		
		if(fileNames == null || !(Utils.isUserOnline(this.context)))
			return;
		
		int size = fileNames.length;
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Number of files to send :" + Integer.toString(size));
		
		for(int i=0; i<size; i++)
		{
			upload(fileNames[i]);
		}		
	}
	
	/**
	 * Used to retry upload of the file to the server
	 */
	private void retryUpload()
	{
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "RETRY NUMBER ::" + retryCount);

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
		if(retryCount >= MAX_RETRY_COUNT)
		{
			resetRetryParams();
		}
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
	 * Used to get an array of file names present in the Analytics directory of the application package
	 * @return array of strings with file names 
	 */
	protected static String[] getFileNames(Context context)
	{
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Looking files in directory :" + context.getFilesDir() + "/Analytics/");
		
		File dir = new File(context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR + File.separator);

		String[] fileNames = dir.list();
		
		return fileNames;
	}
	
	/**
	 * sender thread's run method
	 */
	@Override
	public void run() 
	{
		sendData();
	}
	
	/**
	 * starts the analytics data upload to server as per the set alarm and schedules the next alarm
	 */
	public void startUploadAndScheduleNextAlarm()
	{
		if(!isAnalyticsUploadReady())
			return;
		
		HAManager instance = HAManager.getInstance();

		instance.sendAnalyticsData();
		
		long nextSchedule = 0;
		
		if(instance.getAnalyticsUploadFrequency() < AnalyticsConstants.ANALYTICS_UPLOAD_FREQUENCY)
		{			
			nextSchedule = Utils.getTimeInMillis(Calendar.getInstance(), instance.getWhenToSend() + AnalyticsConstants.UPLOAD_TIME_MULTIPLE, 0, 0, 0);
			instance.incrementAnalyticsUploadFrequency();
		}
		else
		{
			nextSchedule = Utils.getTimeInMillis(Calendar.getInstance(), instance.getWhenToSend(), 0, 0, 0);
			instance.resetAnalyticsUploadFrequency();			
		}
		HikeAlarmManager.setAlarm(context, nextSchedule, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);		
	}
	
	/**
	 * This method actually uploads a specific event file to the server
	 * @param fileName name of the file to be sent to the server
	 */
	private void upload(String fileName)
	{
		while(true)
		{
			String absolutePath = context.getFilesDir() + AnalyticsConstants.EVENT_FILE_DIR + File.separator + fileName;
	
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "uploading file :" + absolutePath);		
			
			SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			String uId = settings.getString(HikeMessengerApp.UID_SETTING, null);
	
			httpClient = new DefaultHttpClient();
			
			HttpPost postCall = new HttpPost(AccountUtils.analyticsUploadUrl);
	
			HttpResponse response = null;
			
			try 
			{
				postCall.addHeader("Connection", "Keep-Alive");
				postCall.addHeader("Content-Name", fileName);
				postCall.addHeader("Cookie", "user=" + token + ";UID=" + uId);
				postCall.setEntity(new FileEntity(new File(absolutePath), "text/plain"));
				
				response = httpClient.execute(postCall);			
			}
			catch(SocketTimeoutException e)
			{			
				retryUpload();
			}
			catch(ConnectTimeoutException e)
			{
				retryUpload();
			}
			catch (FileNotFoundException e) 
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File not found during upload.");
			}
			catch(ClientProtocolException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ClientProtocol exception during upload.");
			}
			catch(IOException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception during upload.");			
			}
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "http response :" + response + response.getStatusLine());
	
			switch (response.getStatusLine().getStatusCode()) 
			{
				case HttpResponseCode.OK:
				{
					resetRetryParams();
					
					new File(absolutePath).delete();
					
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "deleted file :" + fileName);
				}
				return;
	
				case HttpResponseCode.GATEWAY_TIMEOUT:
				case HttpResponseCode.SERVICE_UNAVAILABLE:
				case HttpResponseCode.NOT_FOUND:
				{
					retryUpload();
				}
				break;
				
			default:
				break;
			}
		}
	}		
}

//class NetworkListener extends BroadcastReceiver 
//{
//	Context context;
//		
//	public NetworkListener(Context context) 
//	{
//		this.context = context;
//		
//		this.context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));		
//	}
//
//	@Override
//	public void onReceive(Context context, Intent intent) 
//	{		
//		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
//		{
//			if(Utils.isUserOnline(context))
//			{
//				NetworkType networkType = FileTransferManager.getInstance(this.context).getNetworkType();
//				
//				if(networkType == NetworkType.WIFI)
//				{
//					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Wifi connectivity changed!");
//
//					if(AnalyticsSender.getFileNames(context) != null && Utils.isUserOnline(context))
//					{
//						new Thread(AnalyticsSender.getInstance(context)).start();
//					}
//				}
//			}
//		}				
//	}
//}
