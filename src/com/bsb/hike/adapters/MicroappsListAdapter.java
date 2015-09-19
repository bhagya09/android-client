package com.bsb.hike.adapters;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.IntentFactory;
import com.hike.transporter.utils.Logger;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
		BotInfo mBotInfo = microappsList.get((int)v.getTag());
		
		boolean userHasBot = BotUtils.isBot(mBotInfo.getMsisdn());
		
		// User doesn't have the bot.
		if(!userHasBot)
		{
			HikeDialogFactory.showDialog(mContext, HikeDialogFactory.MICROAPP_DIALOG, new HikeDialogListener(){

				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}

				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					Toast.makeText(mContext, "DOWNLOAD", Toast.LENGTH_SHORT).show();
					//TODO : download request
					String url = "https://qa-content.hike.in/mapps/api/v1/apps/install.json";
					JSONObject json = new JSONObject();
					JSONArray jsonArray = new JSONArray();
					jsonArray.put("news");
					try
					{
						json.put("appName", jsonArray);
						json.put("msisdn", "+hikegames+");
					}
					catch (JSONException e)
					{
						// TODO Auto-generated catch block
						Logger.i("Aman", "Request jsonException");
					}
					RequestToken token = HttpRequests.microAppPostRequest(url, json, new IRequestListener()
					{
						
						@Override
						public void onRequestSuccess(Response result)
						{
							// TODO Auto-generated method stub
							Logger.i("Aman", "Request success");
						}
						
						@Override
						public void onRequestProgressUpdate(float progress)
						{
							// TODO Auto-generated method stub
							Logger.i("Aman", "Request progress");
						}
						
						@Override
						public void onRequestFailure(HttpException httpException)
						{
							// TODO Auto-generated method stub
							Logger.i("Aman", "Request failure");
						}
					});
					if (!token.isRequestRunning())
					{
						token.execute();
					}
					hikeDialog.dismiss();
				}

				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
				}
				}, 
					new Object[]{"Download "+mBotInfo.getConversationName()+"?", ""+mBotInfo.getBotDescription(), "DOWNLOAD", "NOT NOW"});
			
			return;
		}
		//user has the bot already
		
		String msisdn = mBotInfo.getMsisdn();
		mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		
		if (mBotInfo.isBlocked())
		{
			mBotInfo.setBlocked(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, mBotInfo.getMsisdn());
			
			// TODO add to conversation list as well. Request bot from server if user has blocked and deleted
			
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(mBotInfo);
			
			openBot(mBotInfo);
			
			return;
		}
		
		if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn()))
		{
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(mBotInfo);
		}
		
		openBot(mBotInfo);
	}

	public class ViewHolder extends RecyclerView.ViewHolder
	{
		ImageView image;

		TextView name;

		public ViewHolder(View view)
		{
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
	
	public void openBot(BotInfo mBotInfo)
	{
		if (mBotInfo.isNonMessagingBot())
		{
			mContext.startActivity(IntentFactory.getNonMessagingBotIntent(mBotInfo.getMsisdn(), mContext));
		}
		else
		{
			mContext.startActivity(IntentFactory.createChatThreadIntentFromMsisdn(mContext, mBotInfo.getMsisdn(), false, false));
		}
	}

}
