package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.R;

/**
 * Created by varunarora on 03/05/16.
 */
public class NativeCardManager {

    private static NativeCardType cardTypes[] = NativeCardType.values();
    public enum NativeCardType{
        HIKE_DAILY(R.layout.hike_daily_card_sent, R.layout.hike_daily_card_received, 0),
        JFL(R.layout.jfl_card_sent, R.layout.jfl_card_received, 1);
        public final int sentLayoutId;
        public final int layoutId;
        public final int templateId;
        NativeCardType(int sentLayoutId, int layoutId, int templateId){
            this.sentLayoutId = sentLayoutId;
            this.layoutId = layoutId;
            this.templateId = templateId;
        }

        public int getTemplateId(){
            return templateId;
        }
    };

    public static View getInflatedViewAsPerType(Context context, final int cardType, ViewGroup parent, boolean isSent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        switch(cardTypes[cardType])
        {
            case HIKE_DAILY:
                if (isSent)
                {
                    return inflater.inflate(NativeCardType.HIKE_DAILY.sentLayoutId, parent, false);
                }
                else
                {
                    return inflater.inflate(NativeCardType.HIKE_DAILY.layoutId, parent, false);
                }
            case JFL:
                if (isSent)
                {
                    return inflater.inflate(NativeCardType.JFL.sentLayoutId, parent, false);
                }
                else
                {
                    return inflater.inflate(NativeCardType.JFL.layoutId, parent, false);
                }
//            case CardConstants.GAME_CARD_LAYOUT_RECEIVED :
//                return inflater.inflate(R.layout.card_layout_games_received, parent, false);
//            case CardConstants.DEMO_CARD:
//                return inflater.inflate(R.layout.card_demo, parent, false);
            default:
                return null;


        }
    }
}
