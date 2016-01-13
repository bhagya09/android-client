package com.bsb.hike.modules.stickerdownloadmgr;


/**
 * Created by akhiltripathi on 3/01/16.
 */

import android.graphics.Bitmap;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.singleStickerDownloadRequest;


public class MiniStickerDownloadTask implements IHikeHTTPTask
{
    private static final String TAG = "SingleStickerDownloadTask";

    private String stickerId;

    private String categoryId;

    private String largeStickerPath;

    private String smallStickerPath;

    private RequestToken token;

    private IHikeHttpTaskResult miniStickerDownloadedListener;


    public MiniStickerDownloadTask(String stickerId, String categoryId,IHikeHttpTaskResult mListener)
    {
        this.stickerId = stickerId;
        this.categoryId = categoryId;
        miniStickerDownloadedListener = mListener;
    }

    public void execute()
    {
        if (!StickerManager.getInstance().isMinimumMemoryAvailable())
        {
            miniStickerDownloadedListener.doOnFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
            return;
        }

        String requestId = getRequestId(); // for duplicate check

        token = singleStickerDownloadRequest(
                requestId,
                stickerId,
                categoryId,
                getRequestListener(),
                StickerLanguagesManager.getInstance().listToString(
                        StickerLanguagesManager.getInstance().getAccumulatedSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE,
                                StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE)),
                "true");

        if (token.isRequestRunning()) // return if request is running
        {
            return;
        }
        token.execute();
    }

    @Override
    public void cancel()
    {
        if (null != token)
        {
            token.cancel();
        }
    }

    private String getRequestId()
    {
        return (StickerConstants.StickerRequestType.SINGLE.getLabel() + "\\" + categoryId + "\\" + stickerId);
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
                        Logger.e(TAG, "Sticker download failed null or invalid response");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

                    if (null == data)
                    {
                        Logger.e(TAG, "Sticker download failed null data");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    if (!data.has(HikeConstants.PACKS))
                    {
                        Logger.e(TAG, "Sticker download failed null pack data");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    JSONObject packs = data.getJSONObject(HikeConstants.PACKS);
                    String categoryId = packs.keys().next();

                    if (!packs.has(categoryId))
                    {
                        Logger.e(TAG, "Sticker download failed null category data");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    JSONObject categoryData = packs.getJSONObject(categoryId);

                    if (!categoryData.has(HikeConstants.STICKERS))
                    {
                        Logger.e(TAG, "Sticker download failed null stkrs data");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    JSONObject stickers = categoryData.getJSONObject(HikeConstants.STICKERS);

                    if (!stickers.has(stickerId))
                    {
                        Logger.e(TAG, "Sticker download failed null sticker data");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    JSONObject stickerData = stickers.getJSONObject(stickerId);

                    String stickerImage = stickerData.getString(HikeConstants.IMAGE);

                    String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);

                    if (dirPath == null)
                    {
                        Logger.e(TAG, "Sticker download failed directory does not exist");
                        miniStickerDownloadedListener.doOnFailure(null);
                        return;
                    }

                    largeStickerPath = dirPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
                    smallStickerPath = dirPath + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;

                    File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
                    if (!largeDir.exists())
                    {
                        if (!largeDir.mkdirs())
                        {
                            Logger.e(TAG, "Sticker download failed directory not created");
                            miniStickerDownloadedListener.doOnFailure(null);
                            return;
                        }
                    }
                    File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
                    if (!smallDir.exists())
                    {
                        if (!smallDir.mkdirs())
                        {
                            Logger.e(TAG, "Sticker download failed directory not created");
                            miniStickerDownloadedListener.doOnFailure(null);
                            return;
                        }
                    }

                    Utils.makeNoMediaFile(smallDir);
                    Utils.makeNoMediaFile(largeDir);

                    Utils.saveBase64StringToFile(new File(largeStickerPath), stickerImage);

                    boolean isDisabled = stickerData.optBoolean(HikeConstants.DISABLED_ST);
                    if (!isDisabled)
                    {
                        Bitmap thumbnail = HikeBitmapFactory.scaleDownBitmap(largeStickerPath, StickerManager.SIZE_IMAGE, StickerManager.SIZE_IMAGE, true, false);

                        if (thumbnail != null)
                        {
                            File smallImage = new File(smallStickerPath);
                            BitmapUtils.saveBitmapToFile(smallImage, thumbnail);
                            thumbnail.recycle();
                            StickerManager.getInstance().saveInStickerTagSet(stickerId, categoryId);
                            StickerLanguagesManager.getInstance().checkAndUpdateForbiddenList(data);
                            StickerSearchManager.getInstance().insertStickerTags(data, StickerSearchConstants.STATE_STICKER_DATA_FRESH_INSERT);
                        }
                    }
                    StickerManager.getInstance().checkAndRemoveUpdateFlag(categoryId);
                    miniStickerDownloadedListener.doOnSuccess(categoryId);
                }
                catch (JSONException ex)
                {
                    Logger.e(TAG, "Sticker download Json Exception", ex);
                    miniStickerDownloadedListener.doOnFailure(new HttpException("json exception", ex));
                    return;
                }
                catch (IOException ex)
                {
                    Logger.e(TAG, "Sticker download Io Exception", ex);
                    miniStickerDownloadedListener.doOnFailure(new HttpException("io exception", ex));
                    return;
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void onRequestFailure(HttpException httpException)
            {
                Logger.e(TAG, "Sticker download failed :", httpException);
            }
        };
    }

}