package com.bsb.hike.adapters;

import java.util.List;

import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.IntentFactory;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MicroappsListAdapter extends RecyclerView.Adapter<MicroappsListAdapter.ViewHolder> implements OnClickListener
{
	Context mContext;
	List<BotInfo> microappsList;
	IconLoader iconLoader;
	
	private OnClickListener onClickListener;
	
	public MicroappsListAdapter(Context context, List<BotInfo> botsList, IconLoader iconLoader)
	{
		this.mContext = context;
		this.microappsList = botsList;
		this.iconLoader = iconLoader;
		onClickListener = this;
	}

	@Override
	public void onClick(View v)
	{
		String msisdn = (String) v.getTag();
		if (BotUtils.isBot(msisdn) && BotUtils.getBotInfoForBotMsisdn(msisdn).isNonMessagingBot())
		{
			mContext.startActivity(IntentFactory.getNonMessagingBotIntent((String)v.getTag(), mContext));
		}
		else
		{
			mContext.startActivity(IntentFactory.createChatThreadIntentFromMsisdn(mContext, (String)v.getTag(), false, false));
		}
	}
	
	public class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView image;
        TextView name;
        
        public ViewHolder(View view){
            super(view);
            image = (ImageView) view.findViewById(R.id.microapp_image);
            name = (TextView) view.findViewById(R.id.microapp_name);
            image.setOnClickListener(onClickListener);
        }
    }

	@Override
	public int getItemCount()
	{
		return this.microappsList.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position)
	{
		holder.image.setTag(microappsList.get(position).getMsisdn());
		this.iconLoader.loadImage(microappsList.get(position).getMsisdn(), holder.image, false, false, true);
		holder.name.setText(microappsList.get(position).getConversationName());
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup arg0, int arg1)
	{
		View microappImageView = LayoutInflater.from(mContext).inflate(R.layout.microapps_showcase_layout, null);
		return new ViewHolder(microappImageView);
	}

}
