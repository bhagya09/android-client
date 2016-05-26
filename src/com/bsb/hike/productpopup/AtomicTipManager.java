package com.bsb.hike.productpopup;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import static com.bsb.hike.analytics.AnalyticsConstants.AtomicTipsAnalyticsConstants.*;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Manager class for all atomic tip related handling such as DB and UI interaction
 * @author paramshah
 */
public class AtomicTipManager
{
    private static final AtomicTipManager mAtomicTipManager = new AtomicTipManager();

    private final Handler mHandler;

    private List<AtomicTipContentModel> tipContentModels;

    private static final String TAG = "AtomicTipManager";

    public static final String NO_CTA_ACTION = "noCtaAction";

    public static final int FETCH_TIPS_FROM_DB = 1;

    public static final int ADD_TIP_TO_LIST = 2;

    public static final int REMOVE_TIP_FROM_LIST = 3;

    public static final int REFRESH_TIPS_LIST = 4;

    public static final int CLEAR_TIPS_LIST = 5;

    public static final int SAVE_TIP_TO_DB = 6;

    public static final int FLUSH_TIPS_TABLE = 7;

    public static final int CLEAN_TIPS_TABLE = 8;

    public static final int UPDATE_TIP_STATUS = 9;

    public static final int REMOVE_EXPIRED_TIPS = 10;

    public static final int PARSE_NEW_PACKET = 11;

    private final int DEFAULT_TIP_FROM_NTOIF_ID = -1;

    private int atomicTipFromNotifId;

    //currentlyShowing is maintained primarily for handling click actions
    private AtomicTipContentModel currentlyShowing;

    private AtomicTipManager()
    {
        mHandler = new Handler(HikeHandlerUtil.getInstance().getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg == null)
                {
                    Logger.d(TAG, "Received null message");
                    return;
                }
                handleMessageInBackground(msg);
            }
        };
        tipContentModels = new ArrayList<>();
        currentlyShowing = null;
        atomicTipFromNotifId = DEFAULT_TIP_FROM_NTOIF_ID;
    }

    public static AtomicTipManager getInstance()
    {
        return mAtomicTipManager;
    }

    public void handleMessageInBackground(Message msg)
    {
        switch (msg.what)
        {
            case FETCH_TIPS_FROM_DB:
                fetchTipsFromDb();
                break;
            case ADD_TIP_TO_LIST:
                addTipToList((AtomicTipContentModel) msg.obj);
                break;
            case REMOVE_TIP_FROM_LIST:
                removeTipFromList((AtomicTipContentModel) msg.obj);
                break;
            case REFRESH_TIPS_LIST:
                refreshTipsList();
                break;
            case CLEAR_TIPS_LIST:
                clearTipsList();
                break;
            case SAVE_TIP_TO_DB:
                saveNewTip((AtomicTipContentModel) msg.obj);
                break;
            case FLUSH_TIPS_TABLE:
                flushTipsTable();
                break;
            case CLEAN_TIPS_TABLE:
                cleanTipsTable();
                break;
            case UPDATE_TIP_STATUS:
                updateTipStatus((AtomicTipContentModel) msg.obj, msg.arg1);
                break;
            case REMOVE_EXPIRED_TIPS:
                checkAndRemoveExpiredTips();
                break;
            case PARSE_NEW_PACKET:
                parseNewPacket((JSONObject) msg.obj);
                break;

        }
    }

    public Message getMessage(int what, Object obj)
    {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        return msg;
    }

    public Message getMessage(int what, Object obj, int arg1)
    {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        return msg;
    }

    /**
     * Method to be called only once from onCreate of {@link HikeMessengerApp}
     */
    public void init()
    {
        mHandler.sendMessage(getMessage(CLEAN_TIPS_TABLE, null));
        mHandler.sendMessage(getMessage(FETCH_TIPS_FROM_DB, null));
    }

    private void fetchTipsFromDb()
    {
        tipContentModels.addAll(HikeContentDatabase.getInstance().getSavedAtomicTips());
    }

    /**
     * Method to reorder list based on updated status
     */
    private void refreshTipsList()
    {
        Logger.d(TAG, "refreshing atomic tips list by resorting entries");
        Collections.sort(tipContentModels, AtomicTipContentModel.tipsComparator);
    }

    /**
     * Method to parse mqtt packet to get tip content model and save in content DB
     * @param tipJSON
     */
    public void parseAtomicTipPacket(JSONObject tipJSON)
    {
        mHandler.sendMessage(getMessage(PARSE_NEW_PACKET, tipJSON));
    }

    private void parseNewPacket(JSONObject tipJSON)
    {
        Logger.d(TAG, "parsing new tip packet");

        //creating model from JSON
        if(!tipJSON.has(HikeConstants.TIP_ID))
        {
            Logger.d(TAG, "didnot receive id for tip. aborting");
            return;
        }
        AtomicTipContentModel tipContentModel = AtomicTipContentModel.getAtomicTipContentModel(tipJSON);
        Logger.d(TAG, "new tip hash: " + tipContentModel.hashCode());

        recordTipsAnalytics(getJSONForTipAnalytics(TIP_RECEIVED, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), null, null, tipContentModel.getAnalyticsTag(), null));
        recordTipsAnalytics(getJSONForTipAnalytics(TIP_DECODED, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), null, null, tipContentModel.getAnalyticsTag(), null));

        if(tipContentModels.contains(tipContentModel))
        {
            Logger.d(TAG, "received duplicate atomic tip. not saving it!");
            recordTipsAnalytics(getJSONForTipAnalytics(TIP_VALIDITY, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), TIP_INVALID, HikeConstants.DUPLICATE, tipContentModel.getAnalyticsTag(), null));
            return;
        }

        //processing icon base64 string
        if(!createAndCacheIcon(tipContentModel))
        {
            Logger.d(TAG, "unable to create icon for atomic tip. aborting");
            recordTipsAnalytics(getJSONForTipAnalytics(TIP_VALIDITY, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), TIP_INVALID, HikeConstants.ICON, tipContentModel.getAnalyticsTag(), null));
            return;
        }

        //checking and processing if background also has base64 string
        if(!processTipBg(tipContentModel))
        {
            Logger.d(TAG, "Failure in processing tip bg. aborting");
            recordTipsAnalytics(getJSONForTipAnalytics(TIP_VALIDITY, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), TIP_INVALID, HikeConstants.BACKGROUND, tipContentModel.getAnalyticsTag(), null));
            return;
        }

        recordTipsAnalytics(getJSONForTipAnalytics(TIP_VALIDITY, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), TIP_VALID, null, tipContentModel.getAnalyticsTag(), null));

        //saving model in DB
        saveNewTip(tipContentModel);

        //adding model to tips list and refreshing list
        addTipToList(tipContentModel);
        refreshTipsList();

        if(tipContentModel.isShowNotification())
        {
            createNotifForTip(tipContentModel);
        }
    }

    /**
     * Method to add new tip to arraylist on handler util thread
     * @param tipContentModel
     */
    private void addTipToList(final AtomicTipContentModel tipContentModel)
    {
        tipContentModels.add(tipContentModel);
    }

    /**
     * Method to remove a tip from arraylist on handler util thread
     * @param tipContentModel
     */
    private void removeTipFromList(final AtomicTipContentModel tipContentModel)
    {
        tipContentModels.remove(tipContentModel);
    }

    /**
     * Method to clear tips arraylist on handler util thread
     */
    private void clearTipsList()
    {
        recordFlushedTips();
        tipContentModels.clear();
    }

    /**
     * Method to handle tips flush packet
     */
    public void processFlushPacket()
    {
        //firing pub sub to remove any atomic tip currently visible to user
        removeTipFromView();

        //clearing tips list
        mHandler.sendMessage(getMessage(CLEAR_TIPS_LIST, null));

        //flushing tips table from DB
        mHandler.sendMessage(getMessage(FLUSH_TIPS_TABLE, null));
    }

    /**
     * Method to save new tip into DB via post on handler util
     * @param tipContentModel
     */
    private void saveNewTip(final AtomicTipContentModel tipContentModel)
    {
        HikeContentDatabase.getInstance().saveAtomicTip(tipContentModel, AtomicTipContentModel.UNSEEN);
    }

    /**
     * Method to flush all tips from DB via post on handler util
     */
    private void flushTipsTable()
    {
        HikeContentDatabase.getInstance().flushAtomicTipTable();
    }

    /**
     * Method to clean up atomic tip table by removing dismissed tips from DB via post on handler util
     */
    private void cleanTipsTable()
    {
        HikeContentDatabase.getInstance().checkAndLogExpiredAtomicTips();
        HikeContentDatabase.getInstance().cleanAtomicTipsTable();
    }

    /**
     * Method to update status of Atomic Tip in DB via post on handler util
     * @param tipContentModel
     * @param tipStatus
     */
    private void updateTipStatus(final AtomicTipContentModel tipContentModel, final int tipStatus)
    {
        HikeContentDatabase.getInstance().updateAtomicTipStatus(tipContentModel.hashCode(), tipStatus);
    }

    /**
     * Method to create atomic tip icon from base64 string and putting it in Hike LRU Cache
     * @param tipContentModel
     * @return
     */
    private boolean createAndCacheIcon(AtomicTipContentModel tipContentModel)
    {
        BitmapDrawable iconDrawable;
        try
        {
            iconDrawable = drawableFromString(tipContentModel.getIcon());
        }
        catch (IllegalArgumentException iae)
        {
            Logger.d(TAG, "exception while creating tip icon. possibly invalid base64");
            return false;
        }
        if(iconDrawable != null)
        {
            Logger.d(TAG, "caching atomic tip icon");
            cacheTipAsset(tipContentModel.getIconKey(), iconDrawable);
            return true;
        }
        else
        {
            Logger.d(TAG, "Unable to create image from icon string");
            return false;
        }
    }

    /**
     * Method to create atomic tip icon from base64 string and putting it in Hike LRU Cache
     * @param tipContentModel
     * @return
     */
    private boolean createAndCacheBgImage(AtomicTipContentModel tipContentModel)
    {
        BitmapDrawable bgImageDrawable;
        try
        {
            bgImageDrawable = drawableFromString(tipContentModel.getBgImage());
        }
        catch (IllegalArgumentException iae)
        {
            Logger.d(TAG, "exception while creating tip bg image. possibly invalid base64");
            return false;
        }
        if(bgImageDrawable != null)
        {
            cacheTipAsset(tipContentModel.getBgImgKey(), bgImageDrawable);
            return true;
        }
        else
        {
            Logger.d(TAG, "Failed to create background image from base64");
            return false;
        }
    }

    /**
     * Method to process atomic tip bg based on whether it is color or image
     * @param tipContentModel
     * @return
     */
    private boolean processTipBg(AtomicTipContentModel tipContentModel)
    {
        if(tipContentModel.isBgColor())
        {
            try
            {
                Color.parseColor(tipContentModel.getBgColor());
            }
            catch (IllegalArgumentException iae)
            {
                Logger.d(TAG, "bg color value seems to be wrong");
                return false;
            }

            return true;
        }
        else
        {
            return createAndCacheBgImage(tipContentModel);
        }
    }

    /**
     * Method to convert base64 string into drawable using BitmapFactory method
     * @param base64Txt
     * @return
     */
    public BitmapDrawable drawableFromString(String base64Txt)
    {
        Logger.d(TAG, "creating icon for atomic tip from base64");
        return HikeBitmapFactory.stringToDrawable(base64Txt);
    }

    /**
     * Method to save drawable in LRU cache
     * @param key
     * @param tipDrawable
     */
    public void cacheTipAsset(String key, BitmapDrawable tipDrawable)
    {
        HikeMessengerApp.getLruCache().put(key, tipDrawable);
    }

    /**
     * Method to trigger notification for atomic tip based on content from given model
     * @param tipContentModel
     */
    public void createNotifForTip(AtomicTipContentModel tipContentModel)
    {
        Logger.d(TAG, "firing pubsub to create notif for atomic tip");
        HikeMessengerApp.getPubSub().publish(HikePubSub.ATOMIC_TIP_WITH_NOTIF, tipContentModel);
    }

    /**
     * Method to process tip creation originating from notification click
     * @param tipId - id of the tip whose notification was clicked
     */
    public void processAtomicTipFromNotif(int tipId)
    {
        Logger.d(TAG, "notif clicked for atomic tip: " + tipId);
        atomicTipFromNotifId = tipId;
    }

    /**
     * Method to retrieve tip model from list for given id
     * @param tipId
     * @return
     */
    public AtomicTipContentModel getTipFromId(int tipId)
    {
        for(AtomicTipContentModel tipModel : tipContentModels)
        {
            if(tipModel.hashCode() == tipId)
            {
                return tipModel;
            }
        }

        return null;
    }

    /**
     * Method to check if any atomic tip exists by checking size of tips array list
     * @return
     */
    public boolean doesAtomicTipExist()
    {
        Logger.d(TAG, "list size: "+ tipContentModels.size());
        return (tipContentModels.size() > 0);
    }

    /**
     * Method to check if any unseen tip exists. Since tips were retrieved from DB in order of status
     * and priority, all unseen tips are at top of list
     * @return
     */
    public boolean doesUnseenTipExist()
    {
        return (doesAtomicTipExist() && tipContentModels.get(0).getTipStatus() == AtomicTipContentModel.UNSEEN);
    }

    /**
     * Method to check if any high priority (<2) tip exists. Since tips were retrieved from DB in order of
     * status and priority, all high priority tips (for any given status) will appear first
     * @return
     */
    public boolean doesHighPriorityTipExist()
    {
        return (doesAtomicTipExist() && tipContentModels.get(0).getPriority() < 2);
    }

    /**
     * Method to check if given tip is expired or not
     * @param tipContentModel
     * @return
     */
    public boolean isTipExpired(AtomicTipContentModel tipContentModel)
    {
        return (tipContentModel.getEndTime() < System.currentTimeMillis());
    }

    /**
     * Method to remove all expired tips from the list
     */
    private void checkAndRemoveExpiredTips()
    {
        Logger.d(TAG, "checking for and removing expired tips only - via handler");
        Iterator tipIterator = tipContentModels.iterator();
        while(tipIterator.hasNext())
        {
            AtomicTipContentModel currentModel = (AtomicTipContentModel) tipIterator.next();
            if(isTipExpired(currentModel))
            {
                tipIterator.remove();
                Logger.d(TAG, "expired atomic tip removed");
            }
        }
    }

    /**
     * Method to update currentlyShowing by filtering expired tips and setting it to the first entry
     */
    public void updateCurrentlyShowing()
    {
        if(atomicTipFromNotifId != DEFAULT_TIP_FROM_NTOIF_ID)
        {
            Logger.d(TAG, "updating currently showing to tip referenced from notif");
            currentlyShowing = getTipFromId(atomicTipFromNotifId);
            if(currentlyShowing != null)
            {
                tipFromNotifAnalytics(TIP_NOTIF_CLICKED, currentlyShowing.getTipId(), currentlyShowing.isCancellable(), currentlyShowing.getAnalyticsTag());
            }
            return;
        }
        mHandler.sendMessage(getMessage(REMOVE_EXPIRED_TIPS, null));

        if(doesAtomicTipExist())
        {
            currentlyShowing = tipContentModels.get(0);
        }
        else
        {
            currentlyShowing = null;
        }
    }

    /**
     * Method to check if the currently showing tip is cancellable or not
     * @return
     */
    public boolean isTipCancellable()
    {
        return currentlyShowing.isCancellable();
    }

    public void setAtomicTipBackground(View tipView, BitmapDrawable bgDrawable)
    {
        if(Utils.isJellybeanOrHigher())
        {
            tipView.findViewById(R.id.all_content).setBackground(bgDrawable);
        }
        else
        {
            tipView.findViewById(R.id.all_content).setBackgroundDrawable(bgDrawable);
        }
    }

    /**
     * Method to inflate the atomic tip view by using model for currentlyShowing
     * @return
     */
    public View getAtomicTipView()
    {
        Logger.d(TAG, "inflating atomic tip view");

        //resetting flag in case it was set for tip from notif case
        atomicTipFromNotifId = DEFAULT_TIP_FROM_NTOIF_ID;
        HikeNotification.getInstance().cancelNotification(HikeNotification.NOTIFICATION_PRODUCT_POPUP);

        if(currentlyShowing == null)
        {
            Logger.d(TAG, "No tip to show. Perhaps list contained expired tips");
            return null;
        }

        //since tip is seen by user, we need to update the status if not already done
        if(currentlyShowing.getTipStatus() != AtomicTipContentModel.SEEN)
        {
            currentlyShowing.setTipStatus(AtomicTipContentModel.SEEN);
            mHandler.sendMessage(getMessage(REFRESH_TIPS_LIST, null));
            mHandler.sendMessage(getMessage(UPDATE_TIP_STATUS, currentlyShowing, AtomicTipContentModel.SEEN));
            recordTipsAnalytics(getJSONForTipAnalytics(TIP_DISPLAYED, FUNNEL, currentlyShowing.getTipId(), currentlyShowing.isCancellable(), null, null, currentlyShowing.getAnalyticsTag(), null));
        }

        View tipView = LayoutInflater.from(HikeMessengerApp.getInstance().getApplicationContext()).inflate(R.layout.atomic_tip_view, null);

        Logger.d(TAG, "adding atomic tip background");

        //checking if background is a single color or a background image
        if(currentlyShowing.isBgColor())
        {
            Logger.d(TAG, "atomic tip background is single color. processing it!");
            tipView.findViewById(R.id.all_content).setBackgroundColor(Color.parseColor(currentlyShowing.getBgColor()));
        }
        else
        {
            Logger.d(TAG, "atomic tip background is image. processing it!");
            BitmapDrawable bgDrawable = HikeMessengerApp.getLruCache().get(currentlyShowing.getBgImgKey());
            if(bgDrawable == null)
            {
                Logger.d(TAG, "didn't find atomic tip background image in cache. trying to recreate");
                bgDrawable = drawableFromString(currentlyShowing.getBgImage());
                if(bgDrawable == null)
                {
                    Logger.d(TAG, "failed to create atomic tip bg image. returning null");
                    return null;
                }
                else
                {
                    Logger.d(TAG, "setting image as atomic tip background");
                    cacheTipAsset(currentlyShowing.getBgImgKey(), bgDrawable);
                }
            }

            setAtomicTipBackground(tipView, bgDrawable);
        }

        Logger.d(TAG, "adding atomic tip icon");
        BitmapDrawable tipIcon = HikeMessengerApp.getLruCache().get(currentlyShowing.getIconKey());
        if(tipIcon == null)
        {
            Logger.d(TAG, "didn't find atomic tip icon in cache. trying to recreate.");
            tipIcon = drawableFromString(currentlyShowing.getIcon());
            if(tipIcon == null)
            {
                Logger.d(TAG, "creating tip icon from base64 failed.");
                return null;
            }
            else
            {
                cacheTipAsset(currentlyShowing.getIconKey(), tipIcon);
            }
        }

        ((ImageView)tipView.findViewById(R.id.atomic_tip_icon)).setImageDrawable(tipIcon);
        ((TextView) tipView.findViewById(R.id.atomic_tip_header_text)).setText(currentlyShowing.getHeader());
        ((TextView) tipView.findViewById(R.id.atomic_tip_body_text)).setText(currentlyShowing.getBody());
        if(isTipCancellable())
        {
            ((ViewStub) tipView.findViewById(R.id.close_tip_stub)).setVisibility(View.VISIBLE);
        }
        return  tipView;
    }

    /**
     * Method to handle atomic tip click action
     * @param context - this is required by the {@link PlatformUtils#openActivity(Activity, String)} method
     */
    public void onAtomicTipClicked(Activity context)
    {
        Logger.d(TAG, "processing atomic tip click action");
        String metadata = currentlyShowing.getCtaLink();
        switch(currentlyShowing.getCtaAction())
        {
            case ProductPopupsConstants.PopUpAction.OPENAPPSCREEN:
                actionOpenAppScreen(context, metadata);
                break;

            case ProductPopupsConstants.PopUpAction.CALLTOSERVER:
                callToServer(metadata);
                break;

            case ProductPopupsConstants.PopUpAction.DOWNLOAD_STKPK:
                PlatformUtils.downloadStkPk(metadata);
                removeTipFromView();
                break;

            case ProductPopupsConstants.PopUpAction.ACTIVATE_CHAT_HEAD_APPS:
                PlatformUtils.OnChatHeadPopupActivateClick();
                removeTipFromView();
                break;

            case ProductPopupsConstants.PopUpAction.MAKE_FRIEND:
                actionMakeFriend(context, metadata);
                removeTipFromView();
                break;

            case NO_CTA_ACTION:
                removeTipFromView();
                break;
        }
    }

    /**
     * Method to handle atomic tip close action
     */
    public void onAtomicTipClosed()
    {
        Logger.d(TAG, "processing atomic tip dismiss");

        //removing tip from list
        mHandler.sendMessage(getMessage(REMOVE_TIP_FROM_LIST, currentlyShowing));

        //updating tip status as DISMISSED so it can be cleaned
        mHandler.sendMessage(getMessage(UPDATE_TIP_STATUS, currentlyShowing, AtomicTipContentModel.DISMISSED));

        //cleaning to remove dismissed from DB
        mHandler.sendMessage(getMessage(CLEAN_TIPS_TABLE, null));

        currentlyShowing = null;
    }

    /**
     * Method to fire pub sub to remove any active atomic tip from view.
     */
    public void removeTipFromView()
    {
        HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.ATOMIC_TIP);
    }

    /**
     * Method to handle cta of opening hike app screen, same as for popups
     * @param context
     * @param metadata
     */
    public void actionOpenAppScreen(Activity context, String metadata)
    {
        Logger.d(TAG, "processing open app screen action");
        String activityName;
        JSONObject mmObject;
        try
        {
            mmObject = new JSONObject(metadata);
            activityName = mmObject.optString(HikeConstants.SCREEN);
            recordTipAction(OPEN_SCREEN, activityName, mmObject.optString(HikeConstants.MSISDN, null));

            if (activityName.equals(ProductPopupsConstants.HIKESCREEN.MULTI_FWD_STICKERS.toString()))
            {
                actionMultiFwdSticker(metadata);
            }
            else if (activityName.equals(ProductPopupsConstants.HIKESCREEN.OPEN_WEB_VIEW.toString()))
            {
                Logger.d(TAG, "processing open web view action");
                String url = ProductInfoManager.getInstance().getFormedUrl(metadata);
                String title = mmObject.optString(HikeConstants.Extras.TITLE, "");

                if (!TextUtils.isEmpty(url))
                {
                    Utils.startWebViewActivity(HikeMessengerApp.getInstance().getApplicationContext(), url, title);
                }
            }
            else
            {
                PlatformUtils.openActivity(context, metadata);
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        removeTipFromView();
    }

    /**
     * Method to handle cta of multi forward stickers
     * @param stickerData
     */
    public void actionMultiFwdSticker(String stickerData)
    {
        Logger.d(TAG, "processing multi forward sticker action");
        try
        {
            JSONObject mmObject = new JSONObject(stickerData);
            final String stickerId = mmObject.optString(ProductPopupsConstants.STKID);
            final String categoryId = mmObject.optString(ProductPopupsConstants.CATID);
            final boolean selectAll = mmObject.optBoolean(ProductPopupsConstants.SELECTALL, false);
            final boolean sendAll=mmObject.optBoolean(ProductPopupsConstants.SENDALL,false);
            if (!TextUtils.isEmpty(stickerId) && !TextUtils.isEmpty(categoryId))
            {

                if(sendAll)
                {
                    PlatformUtils.sendStickertoAllHikeContacts(stickerId,categoryId);
                    return;
                }

                PlatformUtils.multiFwdStickers(HikeMessengerApp.getInstance().getApplicationContext(), stickerId, categoryId, selectAll);
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Method to make http call on tip click
     * @param metaData
     */
    public void callToServer(final String metaData)
    {
        Logger.d(TAG, "processing call server action");
        String requestType;
        String url;
        try
        {
            JSONObject jsonObject = new JSONObject(metaData);
            requestType = jsonObject.optString(ProductPopupsConstants.REQUEST_TYPE, HikeConstants.GET);
            url = jsonObject.optString(ProductPopupsConstants.URL);
            url = Utils.appendTokenInURL(url);
            recordTipAction(HTTP_CALL, url, null);
            RequestToken requestToken;
            if(requestType.equals(HikeConstants.GET))
            {
                requestToken = HttpRequests.atomicTipRequestGet(url, requestListener);
            }
            else
            {
                JSONObject payload = jsonObject.optJSONObject(HikeConstants.PAYLOAD);
                requestToken = HttpRequests.atomicTipRequestPost(url, payload, requestListener, jsonObject.optBoolean(HikeConstants.ADD_HEADER, false));
            }
            requestToken.execute();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            Logger.d(TAG, "unable to make http request from atomic tip due to JSON exception");
        }
    }

    public IRequestListener requestListener = new IRequestListener()
    {
        @Override
        public void onRequestSuccess(Response result)
        {
            Logger.d(TAG, "atomic tip http call response code: " + result.getStatusCode());
            //getting response body to check for custom toast message
            JSONObject response = (JSONObject) result.getBody().getContent();
            if (response != null)
            {
                if(response.optBoolean(HikeConstants.TOAST, false))
                {
                    String toastMsg = response.optString(HikeConstants.Toast.TOAST_MESSAGE, "");
                    if(!TextUtils.isEmpty(toastMsg))
                    {
                        showHttpToast(toastMsg);
                    }
                }
            }
            removeTipFromView();
        }

        @Override
        public void onRequestProgressUpdate(float progress)
        {
        }

        @Override
        public void onRequestFailure(HttpException httpException)
        {
            Logger.d(TAG, "atomic tip http call  error code " + httpException.getErrorCode());
            String toastMsg = HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.atomic_tip_http_failure);
            showHttpToast(toastMsg);
        }
    };

    public void showHttpToast(final String toastMsg)
    {
        Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public JSONObject getJSONForTipAnalytics(String unqKey, String cls, String family, boolean genus, String species, String variety, String race, String to_user)
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_EXPERIMENT);
            json.put(AnalyticsConstants.V2.PHYLUM, TIPS);
            json.put(AnalyticsConstants.V2.FORM, HOME_SCREEN);
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, unqKey);
            json.put(AnalyticsConstants.V2.CLASS, cls);
            json.put(AnalyticsConstants.V2.ORDER, unqKey);
            json.put(AnalyticsConstants.V2.FAMILY, family);
            json.put(AnalyticsConstants.V2.GENUS, genus);
            json.put(AnalyticsConstants.V2.RACE, race);
            json.put(AnalyticsConstants.V2.TO_USER, to_user);
            if(!TextUtils.isEmpty(species))
            {
                json.put(AnalyticsConstants.V2.SPECIES, species);
            }
            if(!TextUtils.isEmpty(variety))
            {
                json.put(AnalyticsConstants.V2.VARIETY, variety);
            }

        }
        catch (JSONException jse)
        {
            Logger.d(TAG, "error in preparing analytics json");
            jse.printStackTrace();
            return null;
        }
        return json;
    }

    public void recordTipsAnalytics(JSONObject tipEventJSON)
    {
        Logger.d(TAG, "tip analytics json: " + tipEventJSON.toString());
        if(tipEventJSON != null)
        {
            HAManager.getInstance().recordV2(tipEventJSON);
        }
    }

    public void tipFromNotifAnalytics(String uniqueKey, String tipId, boolean isCancellable, String analyticsTag)
    {
        recordTipsAnalytics(getJSONForTipAnalytics(uniqueKey, FUNNEL, tipId, isCancellable, null, null, analyticsTag, null));
    }

    public void tipUiEventAnalytics(String uniqueKey)
    {
        if(currentlyShowing != null)
        {
            recordTipsAnalytics(getJSONForTipAnalytics(uniqueKey, AnalyticsConstants.UI_EVENT, currentlyShowing.getTipId(), currentlyShowing.isCancellable(), null, null, currentlyShowing.getAnalyticsTag(), null));
        }
    }

    public void recordExpiredTip(AtomicTipContentModel tipContentModel)
    {
        recordTipsAnalytics(getJSONForTipAnalytics(TIP_EXPIRY, FUNNEL, tipContentModel.getTipId(), tipContentModel.isCancellable(), String.valueOf(tipContentModel.getStartTime()), String.valueOf(tipContentModel.getEndTime()), tipContentModel.getAnalyticsTag(), null));
    }

    public void recordFlushedTips()
    {
        Iterator tipIterator = tipContentModels.iterator();
        while(tipIterator.hasNext())
        {
            AtomicTipContentModel currentModel = (AtomicTipContentModel) tipIterator.next();
            recordTipsAnalytics(getJSONForTipAnalytics(TIP_FLUSH, EXIT, currentModel.getTipId(), currentModel.isCancellable(), null, null, currentModel.getAnalyticsTag(), null));
        }
    }

    public void recordTipAction(String species, String variety, String to_user)
    {
        recordTipsAnalytics(getJSONForTipAnalytics(TIP_CLICKED, AnalyticsConstants.UI_EVENT, currentlyShowing.getTipId(), currentlyShowing.isCancellable(), species, variety, currentlyShowing.getAnalyticsTag(), to_user));
    }

    private void actionMakeFriend(Context context, String metadata)
    {
        Logger.d(TAG, "processing makefriend action, metadata is " + metadata);
        JSONObject mmObject;
        int counter = 0;
        try
        {
            mmObject = new JSONObject(metadata);
            JSONArray msisdns = mmObject.optJSONArray(HikeConstants.MSISDNS);
            if(msisdns == null || msisdns.length() == 0)
            {
                return;
            }
            for(int i = 0; i < msisdns.length(); i++)
            {
                JSONObject msisdnObj = msisdns.optJSONObject(i);
                if(msisdnObj != null)
                {
                    String msisdn = msisdnObj.optString(HikeConstants.MSISDN);
                    if(!TextUtils.isEmpty(msisdn))
                    {
                        ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, false, false);
                        Utils.toggleFavorite(context, contactInfo, false, null, false);
                        counter++;
                    }
                }
            }
            if(counter == 1)
            {
                showHttpToast(context.getString(R.string.friend_request_sent));
            }
            else if(counter > 1)
            {
                showHttpToast(context.getString(R.string.friend_request_sent_multiple));
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }
}
