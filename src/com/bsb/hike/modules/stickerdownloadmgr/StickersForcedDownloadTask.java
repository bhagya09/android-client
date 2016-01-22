package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by akhiltripathi on 11/01/16.
 */
public class StickersForcedDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult
{

    private static final String TAG = StickersForcedDownloadTask.class.getSimpleName();

    private Set<String> languagesSet;

    private Set<String> forcedRecentsStickers;

    private Set<String> stickerToDownloadTagsSet;

    public StickersForcedDownloadTask(Set<String> languagesSet)
    {
        this.languagesSet = languagesSet;
        stickerToDownloadTagsSet = new HashSet<>();
    }

    private IRequestListener getResponseListener() {
        return new IRequestListener() {

            @Override
            public void onRequestSuccess(Response result)
            {
                try
                {
                    JSONObject response = (JSONObject) result.getBody().getContent();

                    if (!Utils.isResponseValid(response))
                    {
                        Logger.e(TAG,"Forced Sticker download failed null or invalid response");
                        doOnFailure( null);
                        return;
                    }
                    Logger.d(TAG, "Got response for Forced download task " + response.toString());

                    JSONObject data = response.optJSONObject(HikeConstants.DATA_2);

                    if (null == data)
                    {
                        Logger.e(TAG,"Sticker download failed null data");
                        doOnFailure(null);
                        return;
                    }

                    Iterator<String> categories = data.keys();

                    while (categories.hasNext())
                    {
                        String category = categories.next();
                        if (Utils.isBlank(category)) {
                            Logger.e(TAG, "onRequestSuccess(),Invalid category id.");
                            continue;
                        }

                        JSONObject categoryData = data.optJSONObject(category);
                        if ((categoryData == null) || (categoryData.length() <= 0)) {
                            Logger.e(TAG, "onRequestSuccess(), Empty json data for pack: " + category);
                            continue;
                        }

                        Iterator<String> stickers = categoryData.keys();

                        while (stickers.hasNext())
                        {
                            String sticker = stickers.next();

                            JSONObject stickersData = data.optJSONObject(sticker).optJSONObject("md");


                            switch(stickersData.getInt("image"))
                            {
                                case 1:
                                    downloadFullSticker(category,sticker);
                                    break;
                            }

                            switch(stickersData.getInt("mini-image"))
                            {
                                case 1:
                                    downloadMiniSticker(category, sticker);
                                    break;
                            }

                            switch(stickersData.getInt("tags"))
                            {
                                case 1:
                                    downloadFullSticker(category,sticker);
                                    break;
                            }

                            if(stickersData.has("recents"))
                            {
                                if(forcedRecentsStickers == null)
                                {
                                    forcedRecentsStickers = new HashSet<String>();
                                }

                                JSONObject recentsSticker = stickersData.getJSONObject("recents");
                                recentsSticker.put("catId",category);
                                recentsSticker.put("sId",sticker);

                                forcedRecentsStickers.add(recentsSticker.toString());
                            }

                        }
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                doOnSuccess(null);
            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {

            }

            @Override
            public void onRequestFailure(HttpException httpException)
            {
                Logger.d(StickerTagWatcher.TAG, "Request failed.");
                doOnFailure(httpException);
            }
        };
    }

    private String getRequestId() {
        return StickerConstants.StickerRequestType.FORCED.getLabel();
    }

    @Override
    public void execute()
    {
        Set<String> stickerSet;

        stickerSet = StickerManager.getInstance().getStickerSet(StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD);

        if(Utils.isEmpty(stickerSet))
        {
            Logger.wtf(TAG, "empty sticker set.");

            return ;
        }

        try
        {
            JSONObject json = new JSONObject();
            json.put(HikeConstants.CATEGORY_ID_LIST, new JSONArray(stickerSet)) ;

            if(Utils.isEmpty(languagesSet))
            {
                languagesSet.add(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE);
            }
            Logger.d(TAG, "language list for download : " + languagesSet);
            json.put(HikeConstants.KEYBOARD_LIST, new JSONArray(languagesSet));


            RequestToken requestToken = HttpRequests.getForcedDownloadListRequest(getRequestId(), getResponseListener(), json);

            if (requestToken.isRequestRunning())
            {
                return;
            }

            requestToken.execute();
        }
        catch (JSONException e)
        {
            Logger.e(TAG, "json exception ", e);
        }

    }

    @Override
    public void cancel() {

    }

    @Override
    public void doOnSuccess(Object result) {

        StickerSearchManager.getInstance().downloadStickerTags(true,StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD,languagesSet,stickerToDownloadTagsSet);
        if(forcedRecentsStickers!=null)
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FORCED_RECENTS_PRESENT, true);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FORCED_RECENTS_LIST, forcedRecentsStickers);
        }
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.e(TAG, "Forced Download Failed ", exception);
    }

    //todo discuss where to place this method
    public void downloadMiniSticker(String categoryId, String stickerId)
    {
        MiniStickerImageDownloadTask miniStickerImageDownloadTask = new MiniStickerImageDownloadTask(categoryId,stickerId);
        miniStickerImageDownloadTask.execute();
    }

    //todo discuss where to place this method
    public void downloadFullSticker(String categoryId, String stickerId)
    {
        SingleStickerDownloadTask singleStickerDownloadTask = new SingleStickerDownloadTask(categoryId,stickerId,null);
        singleStickerDownloadTask.execute();
    }

}