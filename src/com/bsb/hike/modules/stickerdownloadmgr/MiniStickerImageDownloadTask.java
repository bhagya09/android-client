package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.diskcache.request.Base64StringRequest;
import com.bsb.hike.modules.diskcache.request.CacheRequest;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.singleStickerImageDownloadRequest;

/**
 * Created by anubhavgupta on 03/01/16.
 */
public class MiniStickerImageDownloadTask implements IHikeHTTPTask, IHikeHttpTaskResult {

    private final String TAG = MiniStickerImageDownloadTask.class.getSimpleName();

    private String categoryId;

    private String stickerId;

    private RequestToken requestToken;

    public MiniStickerImageDownloadTask(String categoryId, String stickerId) {
        this.categoryId = categoryId;
        this.stickerId = stickerId;
    }

    @Override
    public void execute() {
        if (!StickerManager.getInstance().isMinimumMemoryAvailable()) {
            doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
            return;
        }

        String requestId = getRequestId();

        requestToken = singleStickerImageDownloadRequest(
                requestId,
                stickerId,
                categoryId,
                true,
                getRequestListener());


        if (requestToken.isRequestRunning()) // return if request is running
        {
            return;
        }
        requestToken.execute();
    }

    private String getRequestId() {
        return (StickerConstants.StickerRequestType.MINI.getLabel() + "\\" + categoryId + "\\" + stickerId);
    }

    @Override
    public void cancel() {
        if (requestToken != null) {
            requestToken.cancel();
        }
    }

    private IRequestListener getRequestListener() {
        return new IRequestListener() {
            @Override
            public void onRequestFailure(HttpException httpException) {
                Logger.e(TAG, "Mini Sticker download failed :", httpException);
            }

            @Override
            public void onRequestSuccess(Response result) {

                try {
                    JSONObject response = (JSONObject) result.getBody().getContent();
                    if (!Utils.isResponseValid(response)) {
                        Logger.e(TAG, "Sticker download failed null or invalid response");
                        doOnFailure(null);
                        return;
                    }

                    JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

                    if (null == data) {
                        Logger.e(TAG, "Sticker download failed null data");
                        doOnFailure(null);
                        return;
                    }

                    if (!data.has(HikeConstants.PACKS)) {
                        Logger.e(TAG, "Sticker download failed null pack data");
                        doOnFailure(null);
                        return;
                    }

                    JSONObject packs = data.getJSONObject(HikeConstants.PACKS);
                    String categoryId = packs.keys().next();

                    if (!packs.has(categoryId)) {
                        Logger.e(TAG, "Sticker download failed null category data");
                        doOnFailure(null);
                        return;
                    }

                    JSONObject categoryData = packs.getJSONObject(categoryId);

                    if (!categoryData.has(HikeConstants.STICKERS)) {
                        Logger.e(TAG, "Sticker download failed null stkrs data");
                        doOnFailure(null);
                        return;
                    }

                    JSONObject stickers = categoryData.getJSONObject(HikeConstants.STICKERS);

                    if (!stickers.has(stickerId)) {
                        Logger.e(TAG, "Sticker download failed null sticker data");
                        doOnFailure(null);
                        return;
                    }

                    JSONObject stickerData = stickers.getJSONObject(stickerId);

                    String stickerImage = stickerData.getString(HikeConstants.IMAGE);
                    CacheRequest cacheRequest = new Base64StringRequest.Builder()
                            .setKey(StickerManager.getInstance().getMiniStickerKey(stickerId, categoryId))
                            .setString(stickerImage)
                            .setTtl(StickerConstants.DEFAULT_TTL_MINI_STICKERS)
                            .build();
                    HikeMessengerApp.getDiskCache().put(cacheRequest);
                    doOnSuccess(categoryId);
                } catch (JSONException ex) {
                    Logger.e(TAG, "Sticker download Json Exception", ex);
                    doOnFailure(new HttpException("json exception", ex));
                    return;
                }

            }

            @Override
            public void onRequestProgressUpdate(float progress) {

            }
        };
    }

    @Override
    public void doOnSuccess(Object result) {
        HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED, new Sticker(categoryId, stickerId));
    }

    @Override
    public void doOnFailure(HttpException exception) {

    }
}
