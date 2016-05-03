package com.bsb.hike.chatthemes;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssetId;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.downloadChatThemeAssets;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;


public class DownloadThemeContentTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

    private String[] mThemeIds;

    private RequestToken token;

    private final String TAG = "DownloadThemeContentTask";

    public DownloadThemeContentTask(String[] themeIds)
    {
        this.mThemeIds = themeIds;
    }

    @Override
    public void execute()
    {
        JSONObject body = prepareBodyObject();
        if (body != null)
        {
            updateAssetIdDownloadStatus(HikeChatThemeConstants.CHAT_THEME_ID_DOWNLOADING);
            token = downloadChatThemeAssetId(body, getRequestListener());
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
        Logger.d(TAG, "chat theme asset id download complete");
        updateAssetIdDownloadStatus(HikeChatThemeConstants.CHAT_THEME_ID_DOWNLOADED);
    }

    @Override
    public void doOnFailure(HttpException exception)
    {
        Logger.d(TAG, "chat theme asset id download failed");
        updateAssetIdDownloadStatus(HikeChatThemeConstants.CHAT_THEME_ID_NOT_DOWNLOADED);
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
                    doOnSuccess(parseAssetContent(response));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
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
            JSONObject themeIds = new JSONObject();
            JSONArray ids = new JSONArray(mThemeIds);
            themeIds.put(HikeChatThemeConstants.JSON_DWNLD_THEME_ID, ids);
            return themeIds;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private String[] parseAssetContent(JSONObject resp)
    {
        try
        {
            JSONArray data = resp.getJSONArray(HikeConstants.DATA_2);
            ChatThemeManager.getInstance().processNewThemeSignal(data, false);
        }
        catch (JSONException e)
        {
            doOnFailure(new HttpException(HttpException.REASON_CODE_UNEXPECTED_ERROR, e));
            e.printStackTrace();
        }

        return mThemeIds;
    }

    private void updateAssetIdDownloadStatus(String value)
    {
        for(String themeId : mThemeIds)
        {
            ChatThemeManager.getInstance().getTheme(themeId).setMetadata(value);
        }
    }
}
