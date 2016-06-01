package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Utils;

public class MicroappsListAdapter extends RecyclerView.Adapter<MicroappsListAdapter.ViewHolder> implements OnClickListener
{
	Context mContext;

	List<BotInfo> microappsList;

	IconLoader iconLoader;
	
	private static final String TAG = "BotDiscovery";

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
		if (v.getTag() == null)
		{
			return;
		}

		BotInfo mBotInfo = microappsList.get((int)v.getTag());

		BotUtils.analyticsForDiscoveryBotTap(mBotInfo.getConversationName());
		
		boolean userHasBot = BotUtils.isBot(mBotInfo.getMsisdn());
		
		// User doesn't have the bot.
		if(!userHasBot)
		{
			BotUtils.showDialog(iconLoader,mContext,mBotInfo);
			
			return;
		}
		
		//user has the bot already
		String msisdn = mBotInfo.getMsisdn();
		mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		
		if (mBotInfo != null && mBotInfo.isNonMessagingBot())
		{
			BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
			if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn()))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.ADD_NM_BOT_CONVERSATION, mBotInfo);
			}
			BotUtils.openBot(mContext, mBotInfo);
		}
		
		else if (mBotInfo != null && mBotInfo.isMessagingBot())
		{
			if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn()))
			{
				// Using the one from the microapp list to get the description of the bot sent in the add_di_bot packet.
				BotUtils.showDialog(iconLoader, mContext, microappsList.get((int) v.getTag()));
			}
			else
			{
				BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
				BotUtils.openBot(mContext, mBotInfo);
			}
		}
	}

	public class ViewHolder extends RecyclerView.ViewHolder
	{
		ImageView image;

		TextView name;
		
		LinearLayout showcase_item;

		public ViewHolder(View view)
		{
			super(view);
			image = (ImageView) view.findViewById(R.id.microapp_image);
			name = (TextView) view.findViewById(R.id.microapp_name);
			showcase_item = (LinearLayout) view.findViewById(R.id.showcase_item_layout);
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
		if (position == 0)
		{
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins((int)Utils.densityMultiplier * 8, 0, 0, 0);
			holder.showcase_item.setLayoutParams(params);
		}
		else if (position == microappsList.size() - 1)
		{
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 0, (int)Utils.densityMultiplier * 8, 0);
			holder.showcase_item.setLayoutParams(params);
		}
		holder.image.setTag(position);
		this.iconLoader.loadImage(microappsList.get(position).getMsisdn(), holder.image, false, false, true);
		holder.name.setText(microappsList.get(position).getConversationName());
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup arg0, int arg1)
	{
		View microappImageView = LayoutInflater.from(mContext).inflate(R.layout.microapps_showcase_layout, null);
		return new ViewHolder(microappImageView);
	}

	public void releaseResources()
	{
		BotUtils.releaseResources();
	}

	public void onBotCreated(Object data)
	{
		BotUtils.onBotCreated(mContext,data);
	}
	
}
