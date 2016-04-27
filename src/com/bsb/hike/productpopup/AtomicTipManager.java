package com.bsb.hike.productpopup;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Manager class for all atomic tip related handling such as DB and UI interaction
 * @author paramshah
 */
public class AtomicTipManager
{
    private static final AtomicTipManager mAtomicTipManager = new AtomicTipManager();

    private final Handler mHandler;

    private ArrayList<AtomicTipContentModel> tipContentModels;

    private static final String TAG = "AtomicTipManager";

    public static final String NO_CTA_ACTION = "noCtaAction";

    private final int DEFAULT_BG_COLOR = R.color.credits_blue;

    //currentlyShowing is maintained primarily for handling click actions
    private AtomicTipContentModel currentlyShowing;

    private AtomicTipManager()
    {
        mHandler = new Handler(HikeHandlerUtil.getInstance().getLooper());
        tipContentModels = new ArrayList<>();
        currentlyShowing = null;
    }

    public static AtomicTipManager getInstance()
    {
        return mAtomicTipManager;
    }

    /**
     * Method to be called only once from onCreate of {@link HikeMessengerApp}
     */
    public void init()
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                tipContentModels.addAll(HikeContentDatabase.getInstance().getSavedAtomicTips());
            }
        });
    }

    /**
     * Method to reorder list based on updated status
     */
    public void refreshTipsList()
    {
        Logger.d(TAG, "refreshing atomic tips list by resorting entries");
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Collections.sort(tipContentModels, AtomicTipContentModel.tipsComparator);
            }
        });

    }

    /**
     * Method to parse mqtt packet to get tip content model and save in content DB
     * @param tipJSON
     */
    public void parseAtomicTipPacket(JSONObject tipJSON)
    {
        Logger.d(TAG, "parsing new tip packet");

        //creating model from JSON
        AtomicTipContentModel tipContentModel = AtomicTipContentModel.getAtomicTipContentModel(tipJSON);
        Logger.d(TAG, "new tip hash: " + tipContentModel.hashCode());

        if(tipContentModels.contains(tipContentModel))
        {
            Logger.d(TAG, "received duplicate atomic tip. not saving it!");
            return;
        }

        //saving model in DB
        saveNewTip(tipContentModel);

        //processing icon base64 string
        createAndCacheIcon(tipContentModel);

        //checking and processing if background also has base64 string
        if(!tipContentModel.isBgColor())
        {
            createAndCacheBgImage(tipContentModel);
        }

        //adding model to tips list and refreshing list
        addTipToList(tipContentModel);
        refreshTipsList();


    }

    /**
     * Method to add new tip to arraylist on handler util thread
     * @param tipContentModel
     */
    public void addTipToList(final AtomicTipContentModel tipContentModel)
    {
        mHandler.post(new Runnable() {
            @Override
            public void run()
            {
                tipContentModels.add(tipContentModel);
            }
        });
    }

    /**
     * Method to remove a tip from arraylist on handler util thread
     * @param tipContentModel
     */
    public void removeTipFromList(final AtomicTipContentModel tipContentModel)
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                tipContentModels.remove(tipContentModel);
            }
        });
    }

    /**
     * Method to clear tips arraylist on handler util thread
     */
    public void clearTipsList()
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                tipContentModels.clear();
            }
        });
    }

    /**
     * Method to handle tips flush packet
     */
    public void processFlushPacket()
    {
        //firing pub sub to remove any atomic tip currently visible to user
        removeTipFromView();

        //clearing tips list
        clearTipsList();

        //flushing tips table from DB
        flushTips();
    }

    /**
     * Method to save new tip into DB via post on handler util
     * @param tipContentModel
     */
    public void saveNewTip(final AtomicTipContentModel tipContentModel)
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                HikeContentDatabase.getInstance().saveAtomicTip(tipContentModel, AtomicTipContentModel.AtomicTipStatus.UNSEEN.getValue());
            }
        });
    }

    /**
     * Method to flush all tips from DB via post on handler util
     */
    public void flushTips()
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                HikeContentDatabase.getInstance().flushAtomicTipTable();
            }
        });
    }

    /**
     * Method to clean up atomic tip table by removing dismissed tips from DB via post on handler util
     */
    public void cleanTipsTable()
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                HikeContentDatabase.getInstance().cleanAtomicTipsTable();
            }
        });
    }

    /**
     * Method to update status of Atomic Tip in DB via post on handler util
     * @param tipContentModel
     * @param tipStatus
     */
    public void updateTipStatus(final AtomicTipContentModel tipContentModel, final int tipStatus)
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                HikeContentDatabase.getInstance().updateAtomicTipStatus(tipContentModel.hashCode(), tipStatus);
            }
        });
    }

    /**
     * Method to create atomic tip icon from base64 string and putting it in Hike LRU Cache
     * @param tipContentModel
     * @return
     */
    public BitmapDrawable createAndCacheIcon(AtomicTipContentModel tipContentModel)
    {
        Logger.d(TAG, "creating icon for atomic tip from base64");
        BitmapDrawable iconDrawable = HikeBitmapFactory.stringToDrawable(tipContentModel.getIcon());
        if(iconDrawable != null)
        {
            Logger.d(TAG, "caching atomic tip icon");
            HikeMessengerApp.getLruCache().put(tipContentModel.getIconKey(), iconDrawable);
        }
        else
        {
            Logger.d(TAG, "Unable to create image from icon string. Returning ic_error");
            iconDrawable = (BitmapDrawable) HikeMessengerApp.getInstance().getApplicationContext().getResources().getDrawable(R.drawable.ic_error);
        }
        return iconDrawable;
    }

    /**
     * Method to create atomic tip icon from base64 string and putting it in Hike LRU Cache
     * @param tipContentModel
     * @return
     */
    public BitmapDrawable createAndCacheBgImage(AtomicTipContentModel tipContentModel)
    {
        Logger.d(TAG, "creating background image for atomic tip from base64");
        BitmapDrawable bgImageDrawable = HikeBitmapFactory.stringToDrawable(tipContentModel.getBgImage());
        if(bgImageDrawable != null)
        {
            HikeMessengerApp.getLruCache().put(tipContentModel.getBgImgKey(), bgImageDrawable);
        }
        else
        {
            Logger.d(TAG, "Failed to create image from base64. will use default color as atomic tip background");
        }
        return bgImageDrawable;
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
        return (doesAtomicTipExist() && tipContentModels.get(0).getTipStatus() == AtomicTipContentModel.AtomicTipStatus.UNSEEN.getValue());
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
    public void checkAndRemoveExpiredTips()
    {
        Logger.d(TAG, "checking for and removing expired tips only - via handler");
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
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
        });
    }

    /**
     * Method to update currentlyShowing by filtering expired tips and setting it to the first entry
     */
    public void updateCurrentlyShowing()
    {
        checkAndRemoveExpiredTips();

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

    /**
     * Method to inflate the atomic tip view by using model for currentlyShowing
     * @return
     */
    public View getAtomicTipView()
    {
        Logger.d(TAG, "inflating atomic tip view");

        if(currentlyShowing == null)
        {
            Logger.d(TAG, "No tip to show. Perhaps list contained expired tips");
            return null;
        }

        //since tip is seen by user, we need to update the status if not already done
        if(currentlyShowing.getTipStatus() != AtomicTipContentModel.AtomicTipStatus.SEEN.getValue())
        {
            currentlyShowing.setTipStatus(AtomicTipContentModel.AtomicTipStatus.SEEN.getValue());
            refreshTipsList();
            updateTipStatus(currentlyShowing, AtomicTipContentModel.AtomicTipStatus.SEEN.getValue());
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
                bgDrawable = createAndCacheBgImage(currentlyShowing);
                if(bgDrawable == null)
                {
                    Logger.d(TAG, "failed to create atomic tip bg image. setting default color as background");
                    tipView.findViewById(R.id.all_content).setBackgroundColor(HikeMessengerApp.getInstance().getApplicationContext().getResources().getColor(DEFAULT_BG_COLOR));
                }
                else
                {
                    Logger.d(TAG, "setting image as atomic tip background");
                    tipView.findViewById(R.id.all_content).setBackground(bgDrawable);
                }
            }
            else
            {
                Logger.d(TAG, "setting cached image as atomic tip backround");
                tipView.findViewById(R.id.all_content).setBackground(bgDrawable);
            }

        }

        Logger.d(TAG, "adding atomic tip icon");
        BitmapDrawable tipIcon = HikeMessengerApp.getLruCache().get(currentlyShowing.getIconKey());
        if(tipIcon == null)
        {
            Logger.d(TAG, "didn't find atomic tip icon in cache. trying to recreate.");
            tipIcon = createAndCacheIcon(currentlyShowing);
        }
        ((ImageView)tipView.findViewById(R.id.atomic_tip_icon)).setImageDrawable(tipIcon);

        ((TextView) tipView.findViewById(R.id.atomic_tip_header_text)).setText(currentlyShowing.getHeader());
        ((TextView) tipView.findViewById(R.id.atomic_tip_body_text)).setText(currentlyShowing.getBody());
        if(isTipCancellable())
        {
            tipView.findViewById(R.id.close_tip).setVisibility(View.VISIBLE);
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
        removeTipFromList(currentlyShowing);

        //updating tip status as DISMISSED so it can be cleaned
        updateTipStatus(currentlyShowing, AtomicTipContentModel.AtomicTipStatus.DISMISSED.getValue());

        //cleaning to remove dismissed from DB
        cleanTipsTable();

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

                mHandler.post(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        PlatformUtils.multiFwdStickers(HikeMessengerApp.getInstance().getApplicationContext(), stickerId, categoryId, selectAll);
                    }
                });
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
            RequestToken requestToken;
            if(requestType.equals(HikeConstants.GET))
            {
                requestToken = HttpRequests.atomicTipRequestGet(url, requestListener);
            }
            else
            {
                JSONObject payload = jsonObject.optJSONObject(HikeConstants.PAYLOAD);
                requestToken = HttpRequests.atomicTipRequestPost(url, payload, requestListener);
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
            Logger.d(TAG, "atmoic tip http call response code " + result.getStatusCode());
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
            Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
            Toast.makeText(hikeAppContext, hikeAppContext.getString(R.string.atomic_tip_http_failure), Toast.LENGTH_SHORT).show();
        }
    };
}
