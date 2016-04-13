package com.bsb.hike.platform.content;

import android.os.Handler;
import android.os.Looper;

import com.bsb.hike.platform.PlatformContentRequest;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;

import static com.bsb.hike.platform.PlatformContentLoader.getLoader;


public class PlatformRequestManager
{
	private static String TAG = "PlatformRequestManager";

	@SuppressWarnings("serial")
	private volatile static ArrayList<PlatformContentRequest> requestQueue = new ArrayList<PlatformContentRequest>()
	{
		public boolean add(PlatformContentRequest object)
		{
			Logger.d(TAG, "adding to requestQueue");
			Iterator<PlatformContentRequest> iterator = iterator();

			while (iterator.hasNext())
			{
				PlatformContentRequest platformContentRequest = (PlatformContentRequest) iterator.next();
				if (platformContentRequest.hashCode() == object.hashCode())
				{
					Logger.d(TAG, "set duplicate as probably dead");
					platformContentRequest.setState(PlatformContentRequest.STATE_PROBABLY_DEAD);
				}
			}

			super.add(object);

			return true;
		};
	};

//	private static volatile ArrayList<Integer> currentDownloadingTemplates = new ArrayList<Integer>();

//	public static ArrayList<Integer> getCurrentDownloadingTemplates()
//	{
//		return currentDownloadingTemplates;
//	}

	/**
	 * Add request to executing pool. Check for wait states. Check for duplicates (change priority)
	 * 
	 * @param argRequest
	 */
	public static void addRequest(final PlatformContentRequest argRequest)
	{
		getLoader().post(new Runnable()
		{
			@Override
			public void run()
			{
				requestQueue.add(argRequest); 
				processNextRequest();
			}
		});
	}

	/**
	 * Grab READY requests from pool and process
	 */
	private static void processNextRequest()
	{
		if (!requestQueue.isEmpty())
		{
			Logger.d(TAG, "processNextRequest");

			int requestQueueSize = requestQueue.size() - 1;

			PlatformContentRequest nextRequest = null;

			for (int i = requestQueueSize; i >= 0; i--)
			{
				PlatformContentRequest request = requestQueue.get(i);

				byte requestState = request.getState();

				if (requestState == PlatformContentRequest.STATE_READY || requestState == PlatformContentRequest.STATE_PROBABLY_DEAD)
				{
					nextRequest = request;
					nextRequest.setState(PlatformContentRequest.STATE_PROCESSING);
					break;
				}
				else if (requestState == PlatformContentRequest.STATE_CANCELLED)
				{
					// Not probable. Just a safety check
					requestQueue.remove(request);
					processNextRequest();
					break;
				}
			}

			if (nextRequest != null)
			{
				getLoader().loadData(nextRequest);
			}
		}

	}

	public static void setState(PlatformContentRequest argRequest, byte newStateId)
	{
		if (argRequest == null)
		{
			return;
		}

		int appId = argRequest.getContentData().appHashCode();

		for (PlatformContentRequest savedReq : requestQueue)
		{
			if (savedReq.getContentData().appHashCode() == appId)
			{
				savedReq.setState(newStateId);
			}
		}

	}

	public static synchronized void setWaitState(PlatformContentRequest argRequest)
	{
		setState(argRequest, PlatformContentRequest.STATE_WAIT);
	}

	public static synchronized void setReadyState(final PlatformContentRequest argRequest)
	{
		getLoader().post(new Runnable()
		{
			@Override
			public void run()
			{
				setState(argRequest, PlatformContentRequest.STATE_READY);
				processNextRequest();
			}
		});
	}

	public static void remove(final PlatformContentRequest argRequest)
	{

		getLoader().post(new Runnable()
		{
			@Override
			public void run()
			{

				if (argRequest == null)
				{
					return;
				}

				argRequest.setState(PlatformContentRequest.STATE_CANCELLED);

				Logger.d(TAG, "remove request - " + argRequest.getContentData().getContentJSON());

				PlatformZipDownloader.removeDownloadingRequest(argRequest.getContentData().getId());

				requestQueue.remove(argRequest);

				processNextRequest();
			}
		});

	}

	public static void failure(PlatformContentRequest mRequest, EventCode event, boolean isTemplatingEnabled)
	{

		reportFailure(mRequest, event);
		if (isTemplatingEnabled)
		{
			remove(mRequest);
		}
	}

	public static void reportFailure(final PlatformContentRequest argRequest, final EventCode error)
	{
		getLoader().post(new Runnable()
		{
			@Override
			public void run()
			{
				argRequest.getListener().onEventOccured(argRequest.getContentData().getUniqueId(),error);
			}
		});
	}

	public static void removeAll()
	{
		for (PlatformContentRequest request : requestQueue)
		{
			request.setState(PlatformContentRequest.STATE_CANCELLED);
		}

		requestQueue.clear();
	}

	public static void completeRequest(final PlatformContentRequest argContentRequest)
	{

		if (argContentRequest == null)
		{
			return;
		}

		if (argContentRequest.getState() == PlatformContentRequest.STATE_CANCELLED)
		{
			// We assume the request has already been removed from request queue
			return;
		}

		Logger.d(TAG, "complete request - " + argContentRequest.getContentData().getContentJSON());

		requestQueue.remove(argContentRequest);

		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				if (argContentRequest.getState() == PlatformContentRequest.STATE_PROBABLY_DEAD)
				{
					try
					{
						argContentRequest.getListener().onComplete(argContentRequest.getContentData());
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					argContentRequest.getListener().onComplete(argContentRequest.getContentData());
				}
			}
		});

		if (!requestQueue.isEmpty())
		{
			processNextRequest();
		}
	}
	
	public static void onDestroy()
	{
		getLoader().post(new Runnable()
		{
			@Override
			public void run()
			{
				PlatformRequestManager.removeAll();
			}
		});
	}

}
