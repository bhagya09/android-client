package com.bsb.hike.modules.platformdownloadmgr;

import android.support.annotation.Nullable;
import android.os.Bundle;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.BotDiscoveryTableDownloadRequest;

import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This call makes a hit to {@link BASE_PLATFORM_URL}/v1/botTable to populate the bot table entries
 * 
 * @author piyush
 * 
 */
public class BotDiscoveryDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

	/**
	 * Indicates the last entry that we have in the bot table
	 */
	private int offset;

	private RequestToken mToken;

	/**
	 * The JSON Object to sent in the body of the HTTP Call. eg : { “bots_in_client” : [“+hike1+”, “+hikenews+”,”+hikecricket+”, “hikegrowth+”] , “all_required” : “false”}
	 */
	private JSONObject mJson;

	private static final String TAG = "BotDiscoveryDownloadTask";

	public BotDiscoveryDownloadTask(int offset, JSONObject json)
	{
		this.offset = offset;
		this.mJson = json;
	}

	@Override
	public void doOnSuccess(Object result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.BOT_DISCOVERY_DOWNLOAD_SUCCESS, result);
	}

	@Override
	public void doOnFailure(HttpException exception)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, exception);
	}

	@Override
	public void execute()
	{
		String requestId = getRequestId();

		this.mToken = BotDiscoveryTableDownloadRequest(requestId, offset, getRequestListener(), mJson);

		/**
		 * Execute the request if not running
		 */
		if (!mToken.isRequestRunning())
		{
			mToken.execute();
		}

	}

	@Override
	public void cancel()
	{
		if (mToken != null)
		{
			mToken.cancel();
		}
	}

    @Override
    public String getRequestId()
	{
		return "bot_discovery_table";
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{

				try
				{
					JSONObject response = (JSONObject) result.getBody().getContent();

					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						doOnFailure(null);
						return;
					}

					doOnSuccess(response);
				}
				catch (Exception e)
				{
					doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
					return;
				}

			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				doOnFailure(httpException);
			}
		};
	}

    @Override
    public Bundle getRequestBundle()
    {
        return null;
    }

}
