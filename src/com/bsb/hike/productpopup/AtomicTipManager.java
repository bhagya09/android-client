package com.bsb.hike.productpopup;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

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

    //currentlyShowing is maintained primarily for handling click actions
    private static AtomicTipContentModel currentlyShowing;

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
                tipContentModels = HikeContentDatabase.getInstance().getSavedAtomicTips();
            }
        });
    }

    public void refreshTipsList()
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
        Logger.d(TAG, "parsing new tip packet");

        //creating model from JSON
        AtomicTipContentModel tipContentModel = AtomicTipContentModel.getAtomicTipContentModel(tipJSON);
        Logger.d(TAG, "new tip hash: " + tipContentModel.hashCode());

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
        tipContentModels.add(tipContentModel);
        refreshTipsList();
    }

    /**
     * Method to handle tips flush packet
     */
    public void processFlushPacket()
    {
        //firing pub sub to remove any atomic tip currently visible to user
        HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.ATOMIC_TIP);

        //clearing tips list
        tipContentModels.clear();

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
            Logger.d(TAG, "caching atomic tip background image");
            Logger.d(TAG, "Unable to create image from icon string. Returning credits_blue color drawable");
            bgImageDrawable = (BitmapDrawable) HikeMessengerApp.getInstance().getApplicationContext().getResources().getDrawable(R.color.credits_blue);
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
        Logger.d(TAG, "checking for and removing expired tips only");
        for(AtomicTipContentModel tipContentModel : tipContentModels)
        {
            if(isTipExpired(tipContentModel))
            {
                tipContentModels.remove(tipContentModel);
                Logger.d(TAG, "expired atomic tip removed");
            }
        }
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
                Logger.d(TAG, "didn't find atomic tip background image in cache. trying to recreate.");
                createAndCacheBgImage(currentlyShowing);
            }
            tipView.findViewById(R.id.all_content).setBackground(bgDrawable);
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
        return  tipView;
    }

    /**
     * Method to handle atomic tip click action
     */
    public void onAtomicTipClicked()
    {
        Logger.d(TAG, "processing atomic tip click action");
        Toast.makeText(HikeMessengerApp.getInstance(), "Atomic Tip Clicked!", Toast.LENGTH_SHORT).show();
        HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.ATOMIC_TIP);
    }

    /**
     * Method to handle atomic tip close action
     */
    public void onAtomicTipClosed()
    {
        Logger.d(TAG, "processing atomic tip dismiss");

        //removing tip from list
        tipContentModels.remove(currentlyShowing);

        //updating tip status as DISMISSED so it can be cleaned
        updateTipStatus(currentlyShowing, AtomicTipContentModel.AtomicTipStatus.DISMISSED.getValue());

        //cleaning to remove dismissed from DB
        cleanTipsTable();

        currentlyShowing = null;
    }
}
