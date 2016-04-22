package com.bsb.hike.productpopup;

import android.os.Handler;

import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;

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

    private AtomicTipManager()
    {
        mHandler = new Handler(HikeHandlerUtil.getInstance().getLooper());
        tipContentModels = new ArrayList<>();
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
}
