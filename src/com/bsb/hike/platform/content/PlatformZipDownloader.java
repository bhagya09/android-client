package com.bsb.hike.platform.content;

import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Download and store template. First
 *
 * @author Atul M
 */
public class PlatformZipDownloader
{
	private PlatformContentRequest mRequest;

	private boolean isTemplatingEnabled;
	
	private boolean doReplace;

	private String callbackId;

	// This hashmap contains the mapping of callback id and the progress. This makes sure that we reply the microapp with
	// every 1% of the microapp.
	private static HashMap<String,Float> callbackProgress = new HashMap<String, Float>();

	//This hashmap contains the mapping of every request that Platform Zip Downloader has initiated. Key is the url
	// and value is the token.
	private static HashMap<String, RequestToken> platformRequests= new HashMap<String, RequestToken>();
	
	private boolean resumeSupported = false;
	
	private int startOffset = 0;
	
	private String stateFilePath;

	private  float progress_done=0;

	private String asocCbotMsisdn = "";


	/**
	 * Instantiates a new platform template download task.
	 *
	 * @param argRequest: request
	 * @param  isTemplatingEnabled: whether the app requires templating or not.
	 */
	
	public PlatformZipDownloader(PlatformContentRequest argRequest, boolean isTemplatingEnabled)
	{
		this(argRequest, isTemplatingEnabled, false);
	}
	
	public PlatformZipDownloader(PlatformContentRequest argRequest, boolean isTemplatingEnabled,boolean doReplace)
	{
		this(argRequest, isTemplatingEnabled, doReplace, null);
	}

	public PlatformZipDownloader(PlatformContentRequest argRequest, boolean isTemplatingEnabled,boolean doReplace, String callbackId)
	{
		// Get ID from content and call http
		mRequest = argRequest;
		this.isTemplatingEnabled = isTemplatingEnabled;
		this.doReplace = doReplace;
		this.callbackId = callbackId;
	}
	
	public PlatformZipDownloader(PlatformContentRequest argRequest, boolean isTemplatingEnabled,boolean doReplace, String callbackId, boolean resumeSupported,String asocCbot)
	{
		// Get ID from content and call http
		this(argRequest, isTemplatingEnabled, doReplace, callbackId);
		this.resumeSupported = resumeSupported;
		
		if(!TextUtils.isEmpty(asocCbot))
		{
			this.asocCbotMsisdn=asocCbot;
		}

		if (resumeSupported)
		{
			setStateFilePath();
			setStartOffset();
		}
	}


	private void setStartOffset()
	{
		File file = new File(stateFilePath + FileRequestPersistent.STATE_FILE_EXT);
		if (file.exists())
		{
			String data[] = PlatformUtils.readPartialDownloadState(stateFilePath + FileRequestPersistent.STATE_FILE_EXT);
			if (data.length > 1)
			{
				try
				{
					startOffset = Integer.parseInt(data[0]);
					progress_done=Float.parseFloat(data[1]);
				}
				catch (NumberFormatException e)
				{
					Logger.e("PlatformZipDownloader", "Invalid offset");
					startOffset=0;
					e.printStackTrace();
				}
			}
		}
		
	}

	private void setStateFilePath()
	{
		stateFilePath=PlatformContentConstants.PLATFORM_CONTENT_DIR+mRequest.getContentData().getId();
	}

	public  boolean isMicroAppExist()
	{
		try
		{
			File microAppFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR, mRequest.getContentData().getId());
			if (microAppFolder.exists())
			{
				return true;
			}
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Calling this function will download and unzip the micro app. Download will be terminated if the folder already exists and then will try to
	 * get the folder from assets and then will download it from web.
	 */
	public void downloadAndUnzip()
	{
		//When the microapp does not exist, we don't want to replace anything and just unzip the data.
        if (!isMicroAppExist())
        {
            doReplace = false;
        }
		// Create temp folder
		File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);

		tempFolder.mkdirs();
		final File zipFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME, mRequest.getContentData().getId() + ".zip");
        //If resume is supported we donot want to delete the zipfile on download failure.
		if (zipFile.exists()&&!resumeSupported)
		{
			unzipMicroApp(zipFile);
			return;
		}


		// Download zip file from web on given url
		getZipFromWeb(zipFile);


		/*
		 *  Legacy code flow commented for zips lookup in assets folder added with apk files
		 *  This flow is not in use now
		 */

		/*
		//Check if the zip is present in hike app package
		AssetsZipMoveTask.AssetZipMovedCallbackCallback mCallback = new AssetsZipMoveTask.AssetZipMovedCallbackCallback()
		{

			@Override
			public void assetZipMoved(boolean hasMoved)
			{
				if (hasMoved)
				{
					unzipMicroApp(zipFile);
				}
				else
				{
					getZipFromWeb(zipFile);
				}
			}
		};

		Utils.executeBoolResultAsyncTask(new AssetsZipMoveTask(zipFile, mRequest, mCallback, isTemplatingEnabled));
		*/
	}

	/**
	 * download the zip from web using 3 retries. On success, will unzip the folder.
	 */
	private void getZipFromWeb(final File zipFile)
	{
		RequestToken token = resumeSupported ? downloadZipWithResume(zipFile, stateFilePath, startOffset) : downloadZip(zipFile);

		if (!token.isRequestRunning())
		{
			token.execute();
			platformRequests.put(mRequest.getContentData().getLayout_url(), token);
			HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadStarted"));
			PlatformRequestManager.getCurrentDownloadingTemplates().add(mRequest.getContentData().appHashCode());
		}

	}

	private boolean updateProgress(float progress)
	{
		float lastProgress = 0;
		if (callbackProgress.containsKey(callbackId))
		{
			lastProgress = callbackProgress.get(callbackId);
		}
		return progress - lastProgress >= HikeConstants.ONE_PERCENT_PROGRESS;
	}
	
	private boolean replaceDirectories(String tempPath,String originalPath,String unzipPath)
	{
		boolean replaceSuccess = false;
		File originalDir = new File(originalPath);
		File tempDir = new File(tempPath);
		if(!tempDir.exists())
		{
			tempDir.mkdirs();
		}
		else
		{
			PlatformUtils.deleteDirectory(tempPath);
			tempDir.mkdirs();
		}
		File src = new File(unzipPath + File.separator+ mRequest.getContentData().getId());
		File dest = tempDir;
		try
		{
			if(PlatformUtils.copyDirectoryTo(src,dest) && PlatformUtils.deleteDirectory(originalPath))
			{
				dest.renameTo(originalDir);
				replaceSuccess = true;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		String sentData = replaceSuccess ? AnalyticsConstants.REPLACE_SUCCESS : AnalyticsConstants.REPLACE_FAILURE;
		
		try
		{
			JSONObject json = new JSONObject();
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_REPLACED);
			json.putOpt(AnalyticsConstants.REPLACE_STATUS, sentData);
			json.putOpt(AnalyticsConstants.APP_NAME, mRequest.getContentData().getId());
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return replaceSuccess;
	}

	/**
	 * calling this function will unzip the microApp.
	 * Running unzip on a single thread so that if multiple cbots for the same bot are received within a short span of time reader-writer concurrency problems do not exist
	 */
	private void unzipMicroApp(final File zipFile)
	{
		HikeHandlerUtil mThread;
		mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();

		mThread.postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				if (!zipFile.exists())
				{
					return;
				}

				final String unzipPath = (doReplace) ? PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME
						: PlatformContentConstants.PLATFORM_CONTENT_DIR;

				try
				{
					unzipWebFile(zipFile.getAbsolutePath(), unzipPath, new Observer()
					{
						@Override
						public void update(Observable observable, Object data)
						{

							long fileSize = zipFile.length();
							if (!(data instanceof Boolean))
							{
								return;
							}
							Boolean isSuccess = (Boolean) data;
							if (isSuccess)
							{
								if (!TextUtils.isEmpty(asocCbotMsisdn))
								{
									BotInfo botinfo = BotUtils.getBotInfoForBotMsisdn(asocCbotMsisdn);
									if (botinfo != null)
									{
										NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(botinfo.getMetadata());
										ToastListener.getInstance().showBotDownloadNotification(asocCbotMsisdn,
												nonMessagingBotMetadata.getCardObj().optString(HikePlatformConstants.HIKE_MESSAGE), false);
									}
								}
								if (!isTemplatingEnabled)
								{
									if (doReplace)
									{
										String tempPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + mRequest.getContentData().getId() + "_temp";
										String originalPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + mRequest.getContentData().getId();
										boolean replace = replaceDirectories(tempPath, originalPath, unzipPath);
										if (replace)
										{
											mRequest.getListener().onComplete(mRequest.getContentData());
											PlatformUtils.sendMicroAppServerAnalytics(true, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
										}
										else
										{
											mRequest.getListener().onEventOccured(0, EventCode.UNZIP_FAILED);
											PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
										}
									}
									else
									{
										mRequest.getListener().onComplete(mRequest.getContentData());
										PlatformUtils.sendMicroAppServerAnalytics(true, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
									}
								}
								else
								{
									PlatformRequestManager.setReadyState(mRequest);
									PlatformUtils.sendMicroAppServerAnalytics(true, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
								}
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipSuccess"));
							}
							else
							{
								mRequest.getListener().downloadedContentLength(fileSize);
								mRequest.getListener().onEventOccured(0, EventCode.UNZIP_FAILED);
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipFailed"));
							}
							zipFile.delete();
						}
					});
				}
				catch (IllegalStateException ise)
				{
					ise.printStackTrace();
					PlatformRequestManager.failure(mRequest, EventCode.UNKNOWN, isTemplatingEnabled);
					PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
				}
			}
		});
	}

	public static HashMap<String, RequestToken> getCurrentDownloadingRequests()
	{
		return platformRequests;
	}

	private void unzipWebFile(String zipFilePath, String unzipLocation, Observer observer)
	{
		HikeUnzipFile unzipper = new HikeUnzipFile(zipFilePath, unzipLocation);
		unzipper.addObserver(observer);
		unzipper.unzip();
	}
	
	private RequestToken downloadZip(final File zipFile)
	{
		RequestToken token = HttpRequests.platformZipDownloadRequest(zipFile.getAbsolutePath(), mRequest.getContentData().getLayout_url(),
				getRequestListenerForDownload(false, null, zipFile));

		return token;

	}
	
	private RequestToken downloadZipWithResume(File zipFile, String stateFilePath, long startOffset)
	{
		RequestToken token = HttpRequests.platformZipDownloadRequestWithResume(zipFile.getAbsolutePath(), stateFilePath, mRequest.getContentData().getLayout_url(),
				getRequestListenerForDownload(true, stateFilePath, zipFile), startOffset,progress_done);

		return token;

	}
	
	private IRequestListener getRequestListenerForDownload(final boolean resumeSupported, final String statefilePath, final File zipFile)
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				if(!resumeSupported)
				{
					JSONObject json = new JSONObject();
					try
					{
						json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
						json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.FILE_DOWNLOADED);
						json.putOpt(AnalyticsConstants.LOG_FIELD_6, zipFile.length());
						json.putOpt(AnalyticsConstants.LOG_FIELD_1, mRequest.getContentData().getId());
						json.putOpt(AnalyticsConstants.LOG_FIELD_5,result.getStatusCode());
					} catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
				}

				if (resumeSupported && !TextUtils.isEmpty(statefilePath))
				{
					(new File(statefilePath + FileRequestPersistent.STATE_FILE_EXT)).delete();
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadSuccess"));
				callbackProgress.remove(callbackId);
				platformRequests.remove(mRequest.getContentData().getLayout_url());
				PlatformRequestManager.getCurrentDownloadingTemplates().remove((Integer) mRequest.getContentData().appHashCode());
				unzipMicroApp(zipFile);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				if (!TextUtils.isEmpty(callbackId))
				{
					if (updateProgress(progress))
					{
						callbackProgress.put(callbackId, progress);
						HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, String.valueOf(progress)));
					}
				}
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				callbackProgress.remove(callbackId);
				platformRequests.remove(mRequest.getContentData().getLayout_url());
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String,String>(callbackId, "downloadFailure"));
				PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.appVersion);
				PlatformRequestManager.failure(mRequest, EventCode.LOW_CONNECTIVITY, isTemplatingEnabled);
				PlatformRequestManager.getCurrentDownloadingTemplates().remove((Integer) mRequest.getContentData().appHashCode());
				if (!resumeSupported) //As we would write to the same file on download resume.
					zipFile.delete();
			}
		};
	}

}
