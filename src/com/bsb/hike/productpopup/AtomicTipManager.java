package com.bsb.hike.productpopup;

import android.os.Handler;
import android.view.View;

import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONObject;

import java.util.ArrayList;

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

    /**
     * Method to parse mqtt packet to get tip content model and save in content DB
     * @param tipJSON
     */
    public void parseAtomicTipPacket(JSONObject tipJSON)
    {
        AtomicTipContentModel tipContentModel = AtomicTipContentModel.getAtomicTipContentModel(tipJSON);
        saveNewTip(tipContentModel);
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
     * Method to check if any atomic tip exists by checking size of tips array list
     * @return
     */
    public boolean doesAtomicTipExist()
    {
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

    public AtomicTipContentModel getFirstAtomicTip()
    {
        return tipContentModels.get(0);
    }

    public View getAtomicTipView(AtomicTipContentModel tipContentModel)
    {
        currentlyShowing = tipContentModel;
        Logger.d(TAG, "inflating atomic tip view");
        return  null;
    }

    public void onAtomicTipClicked()
    {
        Logger.d(TAG, "processing atomic tip click action");
    }

    public void onAtomicTipClosed()
    {
        Logger.d(TAG, "processing atomic tip dismiss");
    }
}
