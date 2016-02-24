package com.bsb.hike.chatthemes;

import static com.bsb.hike.chatthemes.ChatThemeHttpRequestHelper.downloadChatThemeAssets;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * Created by sriram on 24/02/16.
 */
public class DownloadAssetsTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	private String[] mAssetIds;

	private RequestToken token;

	private final String TAG = "DownloadAssetsTask";

	public DownloadAssetsTask(String[] ids)
	{
		this.mAssetIds = ids;
	}

	@Override
	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}

		String requestId = System.currentTimeMillis() + "";
		JSONObject body = prepareBodyObject();
		if (body != null)
		{
			token = downloadChatThemeAssets(requestId, body, getRequestListener());
			if (token.isRequestRunning())
			{
				return;
			}
			token.execute();
		}
	}

	@Override
	public void cancel()
	{
		if (token != null)
		{
			token.cancel();
		}
	}

	@Override
	public void doOnSuccess(Object result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS, result);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_FAILURE, exception);
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				doOnFailure(httpException);
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					if (!Utils.isResponseValid(response))
					{
						doOnFailure(null);
						return;
					}
					parseAssetContent(response);
					doOnSuccess(mAssetIds);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
					return;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

		};
	}

	private JSONObject prepareBodyObject()
	{
		try
		{
			JSONObject assetIds = new JSONObject();
			JSONArray ids = new JSONArray(mAssetIds);
			assetIds.put("asset_ids", ids);
			return assetIds;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private void parseAssetContent(JSONObject resp)
	{
		try
		{
			JSONObject data = resp.getJSONObject("data");
			for (int i = 0; i < mAssetIds.length; i++)
			{
				// TODO CHATTHEME Filepath
				String path = "";// ChatThemeManager.getInstance().getThemeAssetStoragePath() + File.separator + value;
				DataParser parser = new DataParser(new File(path), data.getString(mAssetIds[i]));
				parser.start();
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private static class DataParser extends Thread
	{
		private String encodedData = null;

		private File file;

		public DataParser(File file, String data)
		{
			this.encodedData = data;
			this.file = file;
		}

		@Override
		public void run()
		{
			try
			{
				Utils.saveBase64StringToFile(file, encodedData);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}
}
