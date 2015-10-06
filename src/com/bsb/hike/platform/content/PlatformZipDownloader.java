package com.bsb.hike.platform.content;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import android.text.TextUtils;
import android.util.Pair;
import com.bsb.hike.HikePubSub;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.bool;
import android.provider.MediaStore.Files;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Utils;

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

		if (zipFile.exists())
		{
			unzipMicroApp(zipFile);
			return;
		}

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

	}

	/**
	 * download the zip from web using 3 retries. On success, will unzip the folder.
	 */
	private void getZipFromWeb(final File zipFile)
	{
		RequestToken token = HttpRequests.platformZipDownloadRequest(zipFile.getAbsolutePath(), mRequest.getContentData().getLayout_url(), new IRequestListener()
		{
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				callbackProgress.remove(callbackId);
				platformRequests.remove(mRequest.getContentData().getLayout_url());
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadFailure"));
				deleteTemporaryFolder();
				PlatformRequestManager.failure(mRequest, EventCode.LOW_CONNECTIVITY, isTemplatingEnabled);
				File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);
				PlatformRequestManager.getCurrentDownloadingTemplates().remove(mRequest.getContentData().appHashCode());
		        PlatformContentUtils.deleteDirectory(tempFolder);
				callbackProgress.remove(callbackId);
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "downloadSuccess"));
				unzipMicroApp(zipFile);
				PlatformRequestManager.getCurrentDownloadingTemplates().remove(mRequest.getContentData().appHashCode());
				callbackProgress.remove(callbackId);
				platformRequests.remove(mRequest.getContentData().getLayout_url());
				PlatformRequestManager.getCurrentDownloadingTemplates().remove(mRequest.getContentData().appHashCode());
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
		});

		if (!token.isRequestRunning()) {
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
		return progress - lastProgress >= .1;
	}
	
	private void replaceDirectories(String tempPath,String originalPath,boolean replaceSuccess,String unzipPath)
	{
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
			if(PlatformUtils.copyDirectoryTo(src,dest) && PlatformUtils.deleteDirectory(unzipPath) && PlatformUtils.deleteDirectory(originalPath))
			{
				dest.renameTo(originalDir);
				replaceSuccess = true;
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String sentData;
		if(replaceSuccess)
		{
			sentData = AnalyticsConstants.REPLACE_SUCCESS;
		}
		else
		{
			sentData = AnalyticsConstants.REPLACE_FAILURE;
		}
		
		
		try
		{
			JSONObject json = new JSONObject();
			json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_REPLACED);
			json.putOpt(AnalyticsConstants.MICRO_APP_REPLACED, sentData);
			json.putOpt(AnalyticsConstants.MICRO_APP_REPLACED, mRequest.getContentData().getId());
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void deleteTemporaryFolder()
	{
		File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);
		PlatformContentUtils.deleteDirectory(tempFolder);
	}

	/**
	 * calling this function will unzip the microApp.
	 */
	private void unzipMicroApp(File zipFile)
	{
		final String unzipPath = (doReplace) ? PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME : PlatformContentConstants.PLATFORM_CONTENT_DIR;

		try
		{
			unzipWebFile(zipFile.getAbsolutePath(), unzipPath, new Observer()
			{
				@Override
				public void update(Observable observable, Object data)
				{
					// delete temp folder
					if(!doReplace)
					{
						deleteTemporaryFolder();
					}
					if (!(data instanceof Boolean))
					{
						return;
					}
					Boolean isSuccess = (Boolean) data;
					if (isSuccess)
					{
						if (!isTemplatingEnabled)
						{
							if(doReplace)
							{
								boolean replaceSuccess = false;
								String tempPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + mRequest.getContentData().getId() + "_temp";
								String originalPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + mRequest.getContentData().getId();
								replaceDirectories(tempPath, originalPath,replaceSuccess,unzipPath);
							}
							mRequest.getListener().onComplete(mRequest.getContentData());
						}
						else
						{
							PlatformRequestManager.setReadyState(mRequest);
						}
						HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipSuccess"));
					}
					else
					{
						mRequest.getListener().onEventOccured(0, EventCode.UNZIP_FAILED);
						HikeMessengerApp.getPubSub().publish(HikePubSub.DOWNLOAD_PROGRESS, new Pair<String, String>(callbackId, "unzipFailed"));
					}
				}
			});
		}
		catch (IllegalStateException ise)
		{
			ise.printStackTrace();
			PlatformRequestManager.failure(mRequest,EventCode.UNKNOWN, isTemplatingEnabled);
		}
	}

	public static HashMap<String, RequestToken> getCurrentDownloadingRequests()
	{
		return platformRequests;
	}

	private void unzipWebFile(String zipFilePath, String unzipLocation, Observer observer)
	{
		HikeUnzipTask unzipper = new HikeUnzipTask(zipFilePath, unzipLocation);
		unzipper.addObserver(observer);
		unzipper.unzip();
	}

}
