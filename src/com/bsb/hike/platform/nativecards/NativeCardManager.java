package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.R;

/**
 * This class acts as the manager class for native cards. It holds the various card types and their respective layout Ids.
 */
public class NativeCardManager
{
	public enum NativeCardType
	{
		HIKE_DAILY(R.layout.hike_daily_card_sent, R.layout.hike_daily_card_received, 0), JFL(R.layout.jfl_card_sent, R.layout.jfl_card_received, 1), IMAGE_CARD(
				R.layout.image_card_sent, R.layout.image_card_received, 2);
		public final int sentLayoutId;

		public final int layoutId;

		public final int templateId;

		NativeCardType(int sentLayoutId, int layoutId, int templateId)
		{
			this.sentLayoutId = sentLayoutId;
			this.layoutId = layoutId;
			this.templateId = templateId;
		}

		public int getTemplateId()
		{
			return templateId;
		}

		public static NativeCardType getTypeByTemplateId(int templateId){
			if(templateId == HIKE_DAILY.templateId){
				return HIKE_DAILY;
			}else if(templateId == JFL.templateId){
				return JFL;
			}else{
				return IMAGE_CARD;
			}
		}
	}

	public static View getInflatedViewAsPerType(Context context, final int cardType, ViewGroup parent, boolean isSent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (cardType == NativeCardType.HIKE_DAILY.templateId)
		{
			return isSent ? inflater.inflate(NativeCardType.HIKE_DAILY.sentLayoutId, parent, false) : inflater.inflate(NativeCardType.HIKE_DAILY.layoutId, parent, false);
		}
		else if (cardType == NativeCardType.JFL.templateId)
		{
			return isSent ? inflater.inflate(NativeCardType.JFL.sentLayoutId, parent, false) : inflater.inflate(NativeCardType.JFL.layoutId, parent, false);
		}
		else if (cardType == NativeCardType.IMAGE_CARD.templateId)
		{
			return isSent ? inflater.inflate(NativeCardType.IMAGE_CARD.sentLayoutId, parent, false) : inflater.inflate(NativeCardType.IMAGE_CARD.layoutId, parent, false);
		}
		return null;
	}
}
