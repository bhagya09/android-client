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
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformContentRequest;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;

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

	private boolean isTemplatingEnabled = false;

	private String callbackId = "";

	// This hashmap contains the mapping of callback id and the progress. This makes sure that we reply the microapp with
	// every 1% of the microapp.
	private static HashMap<String,Float> callbackProgress = new HashMap<String, Float>();

	//This hashmap contains the mapping of every request that Platform Zip Downloader has initiated. Key is the appName
	// and value is the token.
	private static HashMap<String, PairModified<RequestToken, Integer>> platformRequests= new HashMap<String, PairModified<RequestToken, Integer>>();
	
	private boolean resumeSupported = false;
	
	private int startOffset = 0;
	
	private String stateFilePath;

    private String asocCbotMsisdn = "";

    private  float progress_done=0;

	private boolean autoResume = false;

	private int tagType =-1;

	private int tagId = -1;

    // static builder class used here for generating and returning object of Zip Downloading process
    public static class Builder {
        private PlatformContentRequest argRequest;
        private boolean isTemplatingEnabled;
        private String callbackId = "";
        private boolean resumeSupported = false;
        private String assocCbotMsisdn = "";
		private boolean autoResume = false;
		private int tagType =-1;
		private int tagId = -1;

        public Builder setArgRequest(PlatformContentRequest argRequest) {
            this.argRequest = argRequest;
            return this;
        }

        public Builder setIsTemplatingEnabled(boolean isTemplatingEnabled) {
            this.isTemplatingEnabled = isTemplatingEnabled;
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

        public PlatformZipDownloader createPlatformZipDownloader() {
            return new PlatformZipDownloader(this);
        }

		public Builder setAutoResume(boolean autoResume) {
			this.autoResume = autoResume;
			return this;
		}

		public Builder setTagType(int tagType)
		{
			this.tagType = tagType;
			return this;
		}

		public Builder setTagId(int tagId)
		{
			this.tagId = tagId;
			return this;
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
        this.callbackId = builder.callbackId;
        this.resumeSupported = builder.resumeSupported;
        this.asocCbotMsisdn = builder.assocCbotMsisdn;
		this.autoResume = builder.autoResume;
		this.tagId = builder.tagId;
		this.tagType = builder.tagType;

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
		stateFilePath= PlatformContentConstants.PLATFORM_CONTENT_DIR+mRequest.getContentData().getId();
	}

	/**
	 * Calling this function will download and unzip the micro app. Download will be terminated if the folder already exists and then will try to
	 * get the folder from assets and then will download it from web.
	 */
	public void downloadAndUnzip()
	{
		// Instead of getting an ex
		if (TextUtils.isEmpty(mRequest.getContentData().getLayout_url()))
		{
			if (null != mRequest.getListener())
			{
				mRequest.getListener().onEventOccured(0, EventCode.INVALID_DATA);
			}

			return;
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
        // Can make a synchronous call here to the server to fetch latest micro app url for forward card scenario
        String downloadUrl = mRequest.getContentData().getLayout_url();

        RequestToken token = resumeSupported ? downloadZipWithResume(zipFile, stateFilePath, startOffset,downloadUrl) : downloadZip(zipFile,downloadUrl);

		if (!token.isRequestRunning())
		{
			token.execute();
			HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadStarted"));
			HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>("downloadStarted", mRequest)));
//			PlatformRequestManager.getCurrentDownloadingTemplates().add(mRequest.getContentData().appHashCode());
			PlatformZipDownloader.putInCurrentDownloadingRequests(mRequest.getContentData().getId(), new PairModified<RequestToken, Integer>(token, HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MAX_RETRY_COUNT_MAPPS, HikePlatformConstants.MAPP_DEFAULT_RETRY_COUNT)),autoResume);
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
			if(PlatformUtils.copyDirectoryTo(src,dest))
			{
                PlatformUtils.deleteDirectory(originalPath);
                PlatformUtils.deleteDirectory(unzipPath + File.separator+ mRequest.getContentData().getId());
                PlatformUtils.deleteDirectory(unzipPath + File.separator+ PlatformContentConstants.MICROAPPS_MACOSX_DIR);
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

								if(tagId >0 && tagType >0)
								{
									JSONObject json = new JSONObject();
									try {
										json.put(HikePlatformConstants.TAG_TYPE, tagType);
										json.put(HikePlatformConstants.TAG_ID, tagId);
									}
									catch(JSONException e)
									{
										Logger.e("PlatformZipDownloader","Subscription error");
									}
									RequestToken token = HttpRequests.microAppSubscribeRequest(HttpRequestConstants.getBotSubscribeUrl(), json, new IRequestListener() {
										@Override
										public void onRequestFailure(HttpException httpException) {
											Logger.e("PlatformZipDownloader","Subscription error");

										}

										@Override
										public void onRequestSuccess(Response result) {

										}

										@Override
										public void onRequestProgressUpdate(float progress) {

										}
									});
									if(token !=null && !token.isRequestRunning()) {
										token.execute();
									}
								}

								String filePath = PlatformUtils.generateMappUnZipPathForBotType(mRequest.getBotType(), PlatformUtils.getMicroAppContentRootFolder(),
										mRequest.getContentData().cardObj.getAppName());
								String tempPath = filePath.substring(0, filePath.length() - 1) + "_temp";
								String originalPath = filePath.substring(0, filePath.length() - 1);
								boolean replace = replaceDirectories(tempPath, originalPath, unzipPath);
								if (replace)
								{
									mRequest.getListener().onComplete(mRequest.getContentData());
									PlatformUtils.sendMicroAppServerAnalytics(true, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode);
									PlatformUtils.microAppDiskConsumptionAnalytics(mRequest.getContentData().cardObj.appName);
								}
								else
								{
									mRequest.getListener().onEventOccured(0, EventCode.UNZIP_FAILED);
									PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode);
								}

								if(isTemplatingEnabled)
								{
									PlatformRequestManager.setReadyState(mRequest);
									PlatformUtils.sendMicroAppServerAnalytics(true, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode);
									PlatformUtils.microAppDiskConsumptionAnalytics(mRequest.getContentData().cardObj.appName);
								}
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipSuccess"));
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>("unzipSuccess", mRequest)));
							}
							else
							{
								mRequest.getListener().downloadedContentLength(fileSize);
								mRequest.getListener().onEventOccured(0, EventCode.UNZIP_FAILED);
								PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode);
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipFailed"));
								HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>("unzipFailed", mRequest)));

									PlatformUtils.removeFromPlatformDownloadStateTable(mRequest.getContentData().cardObj.appName,
											mRequest.getContentData().cardObj.getmAppVersionCode()); // Incase of unzip fail we will remove from state table.

								String appName= mRequest.getContentData().cardObj.appName;
								if (!TextUtils.isEmpty(appName))
								{
									PlatformUtils.deleteDirectory(unzipPath + appName); // Deleting incorrect unzipped file.
								}
							}
							zipFile.delete();
						}
					});
				}
				catch (IllegalStateException ise)
				{
					ise.printStackTrace();
					PlatformRequestManager.failure(mRequest, EventCode.UNKNOWN, isTemplatingEnabled);
					PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode);
				}
			}
		});
	}

	/*
	 * Method to determine and create intermediate directories for the unzip path according to the hierarchical structure determined after the new versioning structure
	 */
	private String getUnZipPath()
	{
		String unzipPath = PlatformUtils.getMicroAppContentRootFolder();

		// To determine the path for unzipping zip files based on request type
		switch (mRequest.getBotType())
		{
		case HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS:
            unzipPath += PlatformContentConstants.HIKE_WEB_MICRO_APPS;
			break;
		case HikePlatformConstants.PlatformBotType.ONE_TIME_POPUPS:
			unzipPath += PlatformContentConstants.HIKE_ONE_TIME_POPUPS;
			break;
		case HikePlatformConstants.PlatformBotType.NATIVE_APPS:
			unzipPath += PlatformContentConstants.HIKE_GAMES;
			break;
		case HikePlatformConstants.PlatformBotType.HIKE_MAPPS:
			unzipPath += PlatformContentConstants.HIKE_MAPPS;
			break;
		}

		return unzipPath + PlatformContentConstants.TEMP_DIR_NAME + File.separator;
	}

	/**
	 * Map Methods : get, put, remove, reduce ref count
	 */

	public static HashMap<String, PairModified<RequestToken, Integer>> getCurrentDownloadingRequests()
	{
		return platformRequests;
	}

	public static void putInCurrentDownloadingRequests(String key, PairModified<RequestToken, Integer> requestTokenIntegerPair, Boolean autoResume)
	{
		if (platformRequests != null)
		{
			if (platformRequests.containsKey(key) && !autoResume)
			{
				reduceRefCountInDownloadingRequests(key,requestTokenIntegerPair);
			}

			else
			{
				Logger.d("PlatformRequests", "Adding Key to Map : " + key + " RequestRetryCount : " + requestTokenIntegerPair.getSecond());
				platformRequests.put(key, requestTokenIntegerPair);
			}
		}
	}

	private static void reduceRefCountInDownloadingRequests(String key,PairModified<RequestToken, Integer> requestTokenIntegerPair)
	{
		if (platformRequests != null)
		{
			if (platformRequests.containsKey(key))
			{
				PairModified<RequestToken, Integer> tokenIntegerPair = platformRequests.get(key);
				tokenIntegerPair.setFirst(requestTokenIntegerPair.getFirst()); // Replacing the token also
				tokenIntegerPair.setSecond(tokenIntegerPair.getSecond() - 1);
				Logger.d("PlatformRequests", "Reducing Ref Count For :  " + key + " New Ref Count : "+ tokenIntegerPair.getSecond());
			}
		}
	}

	public static void removeDownloadingRequest(String key)
	{
		if (platformRequests != null)
		{
			Logger.d("PlatformRequests", "Removing key : " + key);
			platformRequests.remove(key);
		}
	}

	/**
	 * Map method ends here
	 */

	private void unzipWebFile(String zipFilePath, String unzipLocation, Observer observer)
	{
		HikeUnzipFile unzipper = new HikeUnzipFile(zipFilePath, unzipLocation,mRequest.getContentData().cardObj.getAppName());
		unzipper.addObserver(observer);
		unzipper.unzip();
	}
	
	private RequestToken downloadZip(final File zipFile, String downloadUrl)
	{
		RequestToken token = HttpRequests.platformZipDownloadRequest(zipFile.getAbsolutePath(), downloadUrl,
				getRequestListenerForDownload(false, null, zipFile));

		return token;

	}
	
	private RequestToken downloadZipWithResume(File zipFile, String stateFilePath, long startOffset, String downloadUrl)
	{
		RequestToken token = HttpRequests.platformZipDownloadRequestWithResume(zipFile.getAbsolutePath(), stateFilePath, downloadUrl,
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
				long zipFileLength = zipFile.length();
				if(!resumeSupported)
				{

                    // Check being added here for checking content length of downloaded zip with the length received in http headers, in case of incorrect downloaded file size , it would be considered as download failure
                    if(result.getBody() != null && result.getBody().getContentLength() > 0 && zipFileLength > 0)
                    {
                        if(zipFileLength != result.getBody().getContentLength())
                        {
                            HttpException exception = new HttpException(HttpException.REASON_CODE_INCOMPLETE_REQUEST);
                            onRequestFailure(exception);
                            return;
                        }
                    }
                    else if(zipFileLength == 0)
                    {
                        HttpException exception = new HttpException(HttpException.REASON_CODE_ZERO_BYTE_ZIP_DOWNLOAD);
                        onRequestFailure(exception);
                        return;
                    }

                    JSONObject json = new JSONObject();
					try
					{
						json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
						json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.FILE_DOWNLOADED);
						json.putOpt(AnalyticsConstants.LOG_FIELD_6, zipFileLength);
						json.putOpt(AnalyticsConstants.LOG_FIELD_1, mRequest.getContentData().getId());
                        json.putOpt(AnalyticsConstants.LOG_FIELD_2, String.valueOf(mRequest.getContentData().cardObj.getmAppVersionCode()));
						json.putOpt(AnalyticsConstants.LOG_FIELD_5,result.getStatusCode());
					} catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
                }


				if (resumeSupported && !TextUtils.isEmpty(statefilePath))
				{
					String range = "";
					long totalLength = 0;
					for (Header header : result.getHeaders())
					{
						if (header != null && HikeConstants.CONTENT_RANGE.equals(header.getName()))
						{
								range = header.getValue();
								break;
						}
					}
					try
					{
						totalLength = Integer.valueOf(range.substring(range.lastIndexOf("/") + 1, range.length()));
					}
					catch (Exception e)
					{
						Logger.e(getClass().getCanonicalName(), e.toString());
						HttpException exception = new HttpException(HttpException.REASON_CODE_INCOMPLETE_REQUEST);
						onRequestFailure(exception);
					}
					if (zipFileLength != totalLength)
					{
						HttpException exception = new HttpException(HttpException.REASON_CODE_INCOMPLETE_REQUEST);
						onRequestFailure(exception);
						return;
					}

					(new File(statefilePath + FileRequestPersistent.STATE_FILE_EXT)).delete();
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadSuccess"));
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>("downloadSuccess", mRequest)));
				callbackProgress.remove(callbackId);
				unzipMicroApp(zipFile);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				Logger.d("PlatformZipDownloader", mRequest.getContentData().getId() + " Progress " + progress);
				if (!TextUtils.isEmpty(callbackId))
				{
					if (updateProgress(progress))
					{
						callbackProgress.put(callbackId, progress);
						HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, String.valueOf(progress)));
						HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>(String.valueOf(progress), mRequest)));
					}
				}
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
                // Check to make event code as per http exception received
                EventCode eventCode = EventCode.LOW_CONNECTIVITY;
				eventCode.setErrorCode(httpException.getErrorCode()); //Setting error code also
                if(httpException.getErrorCode() == HttpException.REASON_CODE_INCOMPLETE_REQUEST)
                    eventCode = EventCode.INCOMPLETE_ZIP_DOWNLOAD;
                else if(httpException.getErrorCode() == HttpException.REASON_CODE_ZERO_BYTE_ZIP_DOWNLOAD)
                    eventCode = EventCode.ZERO_BYTE_ZIP_DOWNLOAD;

				callbackProgress.remove(callbackId);
				if (!autoResume)
				{
					PlatformUtils.removeFromPlatformDownloadStateTable(mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.getmAppVersionCode());
				}
				PlatformZipDownloader.removeDownloadingRequest(mRequest.getContentData().getId());
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadFailure"));
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS_CARD, new Pair<String, Pair<String, PlatformContentRequest>>(callbackId, new Pair<String, PlatformContentRequest>("downloadFailure", mRequest)));
				PlatformUtils.sendMicroAppServerAnalytics(false, mRequest.getContentData().cardObj.appName, mRequest.getContentData().cardObj.mAppVersionCode,httpException.getErrorCode());

				PlatformRequestManager.failure(mRequest, eventCode, isTemplatingEnabled);
//				PlatformRequestManager.getCurrentDownloadingTemplates().remove((Integer) mRequest.getContentData().appHashCode());

				if (!resumeSupported) //As we would write to the same file on download resume.
					zipFile.delete();
			}
		};
	}

}
