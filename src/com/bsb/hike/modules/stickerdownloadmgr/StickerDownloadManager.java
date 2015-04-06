package com.bsb.hike.modules.stickerdownloadmgr;

import org.json.JSONArray;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.NetworkHandler.NetworkType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StickerDownloadManager
{
	private final Context context;

	private final RequestQueue queue;

	private static volatile StickerDownloadManager _instance = null;

	private final Handler handler;
	
	private final NetworkHandler networkHandler;
	
	public static final String TAG = "StickerDownloadManager";

	private StickerDownloadManager()
	{
		queue = new RequestQueue();
		context = HikeMessengerApp.getInstance().getApplicationContext();
		handler = new Handler(context.getMainLooper());
		networkHandler = new NetworkHandler(context, queue);
	}

	public static StickerDownloadManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (StickerDownloadManager.class)
			{
				if (_instance == null)
				{
					_instance = new StickerDownloadManager();
				}
			}
		}
		return _instance;
		
	}

	/*
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		queue.shutdown();
		_instance = null;
	}

	public void DownloadSingleSticker(String catId, String stkId, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.SINGLE, stkId, catId);
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "Download SingleStickerTask for catId : " + catId + "  stkId : " + stkId + " alreadyExists");
			return;
		}
		
		if(Utils.isOkHttp())
		{
			SingleStickerDownloadTaskOkHttp singleTask = new SingleStickerDownloadTaskOkHttp(stkId, catId, callback);
			singleTask.execute();
		}
		else
		{
			BaseStickerDownloadTask singleTask = new SingleStickerDownloadTask(handler, context, taskId, stkId, catId, callback);
			Request request = new Request(singleTask);
			queue.addTask(taskId, request);
		}
	}

	public void DownloadMultipleStickers(StickerCategory cat, StickerConstants.DownloadType downloadType, DownloadSource source, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.MULTIPLE, null, cat.getCategoryId());
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadMultipleStickers task for catId : " + cat.getCategoryId() +  " already exists");
			return;
		}
		
		if (Utils.isOkHttp())
		{
			MultiStickerDownloadTaskOkHttp stickerCategoryTask = new MultiStickerDownloadTaskOkHttp(cat, downloadType, source);
			stickerCategoryTask.execute();
		}
		else
		{
			BaseStickerDownloadTask stickerCategoryTask = new MultiStickerDownloadTask(handler, context, taskId, cat, downloadType, source, callback);
			Request request = new Request(stickerCategoryTask);
			request.setPrioity(Request.PRIORITY_HIGH);
			queue.addTask(taskId, request);
		}
	}
	
	public void DownloadStickerPreviewImage(String categoryId, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.PREVIEW, null, categoryId);
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadStickersPrevieImage task for catId : " + categoryId +  " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerPreviewImageDownloadTask(handler, context, taskId, categoryId, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}
	
	public void DownloadEnableDisableImage(String categoryId, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.ENABLE_DISABLE, null, categoryId);
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadStickersEnableDisable task for catId : " + categoryId +  " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerEDImageDownloadTask(handler, context, taskId, categoryId, callback);
		Request request = new Request(stickerCategoryTask);
		
		// Setting priority between sticker shop task and enable_disable icon task
		request.setPrioity(10);
		queue.addTask(taskId, request);
	}
	
	public void DownloadStickerSize(StickerCategory cat, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.SIZE, null, cat.getCategoryId());
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadStickersSize task for catId : " + cat.getCategoryId() +  " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerSizeDownloadTask(handler, context, taskId, cat, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}
	
	public void DownloadStickerShopTask(int offset, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.SHOP, null, null);
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadStickersShop task " + " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerShopDownloadTask(handler, context, taskId, offset, callback);
		Request request = new Request(stickerCategoryTask);
		request.setPrioity(Request.PRIORITY_HIGHEST);
		queue.addTask(taskId, request);
	}
	
	public void DownloadStickerSignupUpgradeTask(JSONArray categoryList, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.SHOP, null, null);
		if (queue.isTaskAlreadyExist(taskId))
		{
			Logger.d(TAG, "DownloadStickersSignupUpdrade task " + " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerSignupUpgradeDownloadTask(handler, context, taskId, categoryList, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}

	public boolean isTaskAlreadyExists(StickerRequestType reqType, String stkId, String catId)
	{
		String taskId = getTaskId(reqType, stkId, catId);
		return queue.isTaskAlreadyExist(taskId);
	}

	protected void removeTask(String taskId)
	{
		queue.removeTask(taskId);
	}

	String getTaskId(StickerRequestType reqType, String stkId, String catId)
	{
		StringBuilder builder = new StringBuilder();

		if (reqType.getType() == StickerRequestType.SINGLE.getType())
		{
			builder.append(StickerRequestType.SINGLE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
			builder.append(stkId);

		}
		else if (reqType.getType() == StickerRequestType.MULTIPLE.getType())
		{
			builder.append(StickerRequestType.MULTIPLE.getLabel());
			builder.append("\\");
			builder.append(catId);

		}
		else if (reqType.getType() == StickerRequestType.PREVIEW.getType())
		{
			builder.append(StickerRequestType.PREVIEW.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");

		}
		else if (reqType.getType() == StickerRequestType.ENABLE_DISABLE.getType())
		{
			builder.append(StickerRequestType.ENABLE_DISABLE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SIZE.getType())
		{
			builder.append(StickerRequestType.SIZE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SIGNUP_UPGRADE.getType())
		{
			builder.append(StickerRequestType.SIGNUP_UPGRADE.getLabel());
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SHOP.getType())
		{
			builder.append(StickerRequestType.SHOP.getLabel());
			builder.append("\\");
		}

		return builder.toString();
	}
	
	NetworkType getNetworkType()
	{
		return networkHandler.getNetworkType();
	}
}
