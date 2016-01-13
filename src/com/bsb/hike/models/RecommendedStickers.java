package com.bsb.hike.models;

import android.util.Pair;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akhiltripathi on 1/01/16.
 */
public class RecommendedStickers
{

    private List<Sticker> availableRecommendedStickers;

    private List<Pair<Sticker,Integer>> notAvailableRecommendedStickers;


    public RecommendedStickers(List<Sticker> stickerList)
    {
        int length = stickerList.size();

        for (int i = 0; i < length; i++)
        {
            Sticker sticker = stickerList.get(i);
            int availabilityStatus = sticker.getStickerCurrentAvailabilityStatus();
            switch(availabilityStatus)
            {
                case  HikeStickerSearchBaseConstants.LARGE_AND_MINI_STICKERS_AVAILABLE:
                case  HikeStickerSearchBaseConstants.LARGE_STICKER_AVAILABLE_ONLY:
                case  HikeStickerSearchBaseConstants.MINI_STICKER_AVAILABLE_ONLY:
                    availableRecommendedStickers.add(sticker);
                    break;
                case  HikeStickerSearchBaseConstants.STICKER_NOT_AVAILABLE:
                    notAvailableRecommendedStickers.add(new Pair<Sticker, Integer>(sticker,i));
                    break;
                default:
                        continue;

            }
        }
    }

    public boolean anyStickerToShow()
    {
        return availableRecommendedStickers.size()>0;
    }

    public List<Sticker> getNotAvailableRecommendedStickers()
    {
        List<Sticker> result = new ArrayList<Sticker>();
        for(int i =0;i<notAvailableRecommendedStickers.size();i++)
        {
            result.add(notAvailableRecommendedStickers.get(i).first);
        }

        return result;
    }

    public List<Sticker> getAvailableRecommendedStickers() {
       return this.availableRecommendedStickers;
    }
}

