package com.bsb.hike.platform;

import android.annotation.SuppressLint;
import android.os.Handler;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformRequestManager;
import com.bsb.hike.platform.content.PlatformZipDownloader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.samskivert.mustache.Template;

/**
 * This class is responsible for handling content requests. Directly communicates with cache, template engine and download task.
 */
public class PlatformContentLoader extends Handler
{
	private static String TAG = "PlatformContentLoader";

	private static PlatformContentLoader mLoader = new PlatformContentLoader();

	/**
	 * Instantiates a new platform content loader.
	 */
	private PlatformContentLoader()
	{
		super(HikeHandlerUtil.getInstance().getLooper());
	}

	/**
	 * Gets the loader.
	 *
	 * @return the loader
	 */
	public static PlatformContentLoader getLoader()
	{
		return mLoader;
	}

	/**
	 * Handle request.
	 *
	 * @param platformContentModel
	 *            the platform content model
	 * @param listener
	 *            the listener
	 */
	public void handleRequest(final PlatformContentRequest argContentRequest)
	{
		Logger.d(TAG, "handling request");

        // Stop the flow and return from here in case any exception occurred and contentData becomes null
        if(argContentRequest.getContentData() == null)
            return;

		PlatformContentModel formedContent = PlatformContentCache.getFormedContent(argContentRequest);

		if (formedContent != null)
		{
			Logger.d(TAG, "found formed content");
			argContentRequest.getListener().onComplete(formedContent);
			return;
		}
		else
		{
			Logger.d(TAG, "formed content not found");
			PlatformRequestManager.addRequest(argContentRequest);
		}
	}

	public void loadData(PlatformContentRequest argContentRequest)
	{
		// Get from template
		Template template = PlatformContentCache.getTemplate(argContentRequest);
		if (template != null)
		{
			Logger.d(TAG, "found cached template");
			// Compile template
			if (PlatformTemplateEngine.execute(template, argContentRequest))
			{
				Logger.d(TAG, "data binded");
				// Add to cache
				PlatformContentCache.putFormedContent(argContentRequest.getContentData());
				
				argContentRequest.getListener().onEventOccured(argContentRequest.getContentData().getUniqueId(), EventCode.LOADED);

				PlatformRequestManager.completeRequest(argContentRequest);
			}
			else
			{
				// Incorrect data. Could not execute. Remove request from queue.
				PlatformRequestManager.reportFailure(argContentRequest, PlatformContent.EventCode.INVALID_DATA);
				PlatformRequestManager.remove(argContentRequest);
			}
		}
		else
		{
			if (argContentRequest.getState() != PlatformContentRequest.STATE_CANCELLED)
			{
				// Fetch from remote
				getTemplateFromRemote(argContentRequest);
			}
		}
	}

	private void getTemplateFromRemote(PlatformContentRequest argContentRequest)
	{
		PlatformRequestManager.setWaitState(argContentRequest);

		if (PlatformZipDownloader
				.getCurrentDownloadingRequests().containsKey(argContentRequest.getContentData().getId()))
		{
			PairModified<RequestToken, Integer> requestTokenIntegerPair = PlatformZipDownloader.getCurrentDownloadingRequests().get(argContentRequest.getContentData().getId());

			if (requestTokenIntegerPair != null && (requestTokenIntegerPair.getSecond() < 1))
			{
				PlatformRequestManager.reportFailure(argContentRequest, PlatformContent.EventCode.INVALID_DATA);
				return; // MAX DOWNLOAD CAPPING LIMIT REACHED!
			}
		}

		Logger.d(TAG, "fetching template from remote");

        // Setting up parameters for downloadAndUnzip call
        boolean isTemplatingEnabled = true;
        boolean doReplace = true;
        String callbackId = null;
        boolean resumeSupported = false;
        String assocCbot = "";

		PlatformUtils.downloadAndUnzip(argContentRequest, isTemplatingEnabled, doReplace, callbackId, resumeSupported, assocCbot,false,-1,-1);
	}
}
