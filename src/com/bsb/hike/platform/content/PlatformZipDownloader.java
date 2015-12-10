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
import java.util.Set;
import java.util.TreeMap;


/**
 * Download and store template. First
 *
 * @author Atul M
 */
public class PlatformZipDownloader
{
	private PlatformContentRequest mRequest;

	private boolean isTemplatingEnabled = false;
	
	private boolean doReplace = false;

	private String callbackId = "";

	// This hashmap contains the mapping of callback id and the progress. This makes sure that we reply the microapp with
	// every 1% of the microapp.
	private static HashMap<String,Float> callbackProgress = new HashMap<String, Float>();

	//This hashmap contains the mapping of every request that Platform Zip Downloader has initiated. Key is the appName
	// and value is the token.
	private static HashMap<String, RequestToken> platformRequests= new HashMap<String, RequestToken>();
	
	private boolean resumeSupported = false;
	
	private int startOffset = 0;
	
	private String stateFilePath;

    private boolean isDeletionReqBasedOnCompatibilityMap = false;

    private String asocCbotMsisdn = "";

    private  float progress_done=0;

    // static builder class used here for generating and returning object of Zip Downloading process
    public static class Builder {
        private PlatformContentRequest argRequest;
        private boolean isTemplatingEnabled;
        private boolean doReplace = false;
        private String callbackId = "";
        private boolean resumeSupported = false;
        private String assocCbotMsisdn = "";
        private boolean isDeletionReqBasedOnCompatibilityMap = false;

        public Builder setArgRequest(PlatformContentRequest argRequest) {
            this.argRequest = argRequest;
            return this;
        }

        public Builder setIsTemplatingEnabled(boolean isTemplatingEnabled) {
            this.isTemplatingEnabled = isTemplatingEnabled;
            return this;
        }

        public Builder setDoReplace(boolean doReplace) {
            this.doReplace = doReplace;
            return this;
        }

        public Builder setCallbackId(String callbackId) {
            this.callbackId = callbackId;
            return this;
        }

        public Builder setResumeSupported(boolean resumeSupported) {
            this.resumeSupported = resumeSupported;
            return this;
        }

        public Builder setAssocCbotMsisdn(String assocCbotMsisdn) {
            this.assocCbotMsisdn = assocCbotMsisdn;
            return this;
        }

        public Builder setMappDeletionBooleanByCompatibilityMap(boolean isDeletionReqBasedOnCompatibilityMap) {
            this.isDeletionReqBasedOnCompatibilityMap = isDeletionReqBasedOnCompatibilityMap;
            return this;
        }

        public PlatformZipDownloader createPlatformZipDownloader() {
            return new PlatformZipDownloader(this);
        }
    }

    /**
     * Instantiates a new platform template download task.
     *
     * @param builder
     */
    private PlatformZipDownloader(Builder builder)
    {
        mRequest = builder.argRequest;
        this.isTemplatingEnabled = builder.isTemplatingEnabled;
        this.doReplace = builder.doReplace;
        this.callbackId = builder.callbackId;
        this.resumeSupported = builder.resumeSupported;
        this.asocCbotMsisdn = builder.assocCbotMsisdn;

        if (resumeSupported)
        {
            setStateFilePath();
            setStartOffset();
        }
    }


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

	public boolean isMicroAppExist()
	{
		try
		{
			String unzipPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS;
			String microAppName = mRequest.getContentData().cardObj.getMicroApp();
			int microAppVersion = mRequest.getContentData().getMappVersionCode();

			// Generate path for the old micro app directory
			File oldMicroAppFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR, microAppName);

			if (oldMicroAppFolder.exists())
				return true;

            // Generate unzip path
            unzipPath = PlatformUtils.generateMappUnZipPathForBotRequestType(mRequest.getRequestType(),unzipPath,microAppName,microAppVersion);

			if (new File(unzipPath).exists())
				return true;

		}
		catch (NullPointerException npe)
		{
			Logger.e("PlatformZipDownloader isMicroAppExist",npe.toString());
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

		File zipFile = getZipPath();

        //If resume is supported we donot want to delete the zipfile on download failure.
		if (zipFile.exists()&&!resumeSupported)
		{
			unzipMicroApp(zipFile);
			return;
		}

		// Download zip file from web on given url
		getZipFromWeb(zipFile);
	}


	/*
	 * Method to get path to store zip files
	 */
	private File getZipPath()
	{
		// Create temp folder
		File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);

		tempFolder.mkdirs();
		final File zipFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME, mRequest.getContentData().getId() + ".zip");

		return zipFile;
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
			platformRequests.put(mRequest.getContentData().getId(), token);
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

				final String unzipPath = getUnZipPath();

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

                                // Delete previous version micro apps after checking from compatibility matrix if isDeletionReqBasedOnCompatibilityMap flag is set
                                if(isDeletionReqBasedOnCompatibilityMap)
                                    deleteMicroAppsAsPerCompatibilityMap();

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

	/*
	 * Method to determine and create intermediate directories for the unzip path according to the hierarchical structure determined after the new versioning structure
	 */
	private String getUnZipPath()
	{
		String unzipPath = (doReplace) ? PlatformUtils.getMicroAppContentRootFolder() + PlatformContentConstants.TEMP_DIR_NAME : PlatformUtils.getMicroAppContentRootFolder();

        // To determine the path for unzipping zip files based on request type
		switch (mRequest.getRequestType())
		{
		case HikePlatformConstants.PlatformMappRequestType.HIKE_MICRO_APPS:
			unzipPath = generateCBotUnzipPathForRequestType(unzipPath);
			int microAppVersion = mRequest.getContentData().getMappVersionCode();
			new File(unzipPath, HikePlatformConstants.VERSIONING_DIRECTORY_NAME + microAppVersion).mkdirs();
			unzipPath += HikePlatformConstants.VERSIONING_DIRECTORY_NAME + microAppVersion + File.separator;
			break;
		case HikePlatformConstants.PlatformMappRequestType.ONE_TIME_POPUPS:
			unzipPath += PlatformContentConstants.HIKE_ONE_TIME_POPUPS;
			unzipPath = generateCBotUnzipPathForRequestType(unzipPath);
            break;
		case HikePlatformConstants.PlatformMappRequestType.NATIVE_APPS:
			unzipPath += PlatformContentConstants.HIKE_GAMES;
			unzipPath = generateCBotUnzipPathForRequestType(unzipPath);
            break;
		case HikePlatformConstants.PlatformMappRequestType.HIKE_MAPPS:
			unzipPath += PlatformContentConstants.HIKE_MAPPS;
			unzipPath = generateCBotUnzipPathForRequestType(unzipPath);
            break;
		}

		return unzipPath;
	}


    /*
	 * Method for generating micro app subdirectory and create intermediate directories for the unzip path according to the hierarchical structure determined after the new versioning structure
	 */
    private String generateCBotUnzipPathForRequestType(String unzipPath)
    {
        String microAppName = mRequest.getContentData().cardObj.getMicroApp();

        // Create directory for micro app if not exists already
        try {
            new File(unzipPath, microAppName).mkdirs();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        // Create directory for this version for specific micro app
        unzipPath += microAppName + File.separator;

        return unzipPath;
    }

	/*
	 * Method to delete unzipped code as per based on the compatibility map
	 */
	private void deleteMicroAppsAsPerCompatibilityMap()
	{
        String microAppName = mRequest.getContentData().getId();
		TreeMap<Integer, Integer> compatibilityMap = mRequest.getContentData().cardObj.getCompatibilityMap();

		if (compatibilityMap == null || mRequest.getRequestType() != HikePlatformConstants.PlatformMappRequestType.HIKE_MICRO_APPS)
			return;

		// Micro app version for the current cbot packet
		int microAppVersion = mRequest.getContentData().getMappVersionCode();

		// Logic to determine which unzipped micro apps directories are to be deleted as per compatibility Matrix
		int hashMapKey = microAppVersion;

		Set<Integer> keys = compatibilityMap.keySet();
		for (Integer key : keys)
		{
			if (key >= hashMapKey)
			{
				hashMapKey = key;
				break;
			}
		}

		int minSupportedAppVersion = compatibilityMap.get(hashMapKey);
		String unzipPath = (doReplace) ? PlatformUtils.getMicroAppContentRootFolder() + PlatformContentConstants.TEMP_DIR_NAME : PlatformUtils.getMicroAppContentRootFolder();
		unzipPath += microAppName + File.separator;

		// Code to delete micro apps within the compatibility matrix range that is figured above
		while (minSupportedAppVersion != microAppVersion)
		{
			String pathToDelete = unzipPath + HikePlatformConstants.VERSIONING_DIRECTORY_NAME + minSupportedAppVersion + File.separator;
			PlatformUtils.deleteDirectory(pathToDelete);
			minSupportedAppVersion++;
		}

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
				platformRequests.remove(mRequest.getContentData().getId());
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
				platformRequests.remove(mRequest.getContentData().getId());
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
