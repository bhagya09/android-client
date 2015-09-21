package com.bsb.hike.adapters;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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
	
	private static final String TAG = "BotDiscovery";

	private OnClickListener onClickListener;
	
	private static final String URL = "https://qa-content.hike.in/mapps/api/v1/apps/install.json";

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
			final CustomAlertDialog dialog = new CustomAlertDialog(mContext, HikeDialogFactory.MAPP_DOWNLOAD_DIALOG, R.layout.mapp_download_dialog);

			this.iconLoader.loadImage(mBotInfo.getMsisdn(), (ImageView) dialog.findViewById(R.id.bot_icon), false, false, true);
			
			TextView bot_name = (TextView) dialog.findViewById(R.id.bot_name);
			bot_name.setText(mBotInfo.getConversationName());
			
			TextView description = (TextView) dialog.findViewById(R.id.bot_description);
			description.setText(mBotInfo.getBotDescription());
			
			String loadingText = String.format(mContext.getResources().getString(R.string.getting_mapp_shortly), mBotInfo.getConversationName());
			TextView loadingTextView = (TextView) dialog.findViewById(R.id.loading_text);
			loadingTextView.setText(loadingText);
			
			dialog.setPositiveButton(mContext.getResources().getString(R.string.okay), new HikeDialogListener()
			{
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
				
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
				
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
			});
			dialog.setCancelable(true);
			dialog.show();
			
			botDownloadAnalytics(mBotInfo.getMsisdn(), mBotInfo.getConversationName());
			
			initiateBotDownload(mBotInfo.getMsisdn());
			
			return;
		}
		
		//user has the bot already
		String msisdn = mBotInfo.getMsisdn();
		mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		
		if (mBotInfo != null && mBotInfo.isBlocked())
		{
			unblockBot(mBotInfo);
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
	
	private void openBot(BotInfo mBotInfo)
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
	
	private void initiateBotDownload(String msisdn)
	{
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(msisdn);
		try
		{
			json.put("appName", jsonArray);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Bot download request Json Exception: "+e.getMessage());
		}
		RequestToken token = HttpRequests.microAppPostRequest(URL, json, new IRequestListener()
		{
			
			@Override
			public void onRequestSuccess(Response result)
			{
				Logger.v(TAG, "Bot download request success");
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.v(TAG, "Bot download request failure");
				Toast.makeText(mContext, ""+mContext.getResources().getString(R.string.error_sharing), Toast.LENGTH_SHORT);
			}
		});
		if (!token.isRequestRunning())
		{
			token.execute();
		}
	}
	
	private void botDownloadAnalytics(String msisdn, String name)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.DISCOVERY_BOT_DOWNLOAD);
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			json.put(AnalyticsConstants.BOT_NAME, name);
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSON Exception in botDownloadAnalytics "+e.getMessage());
		}
		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.BOT_DISCOVERY, json);
	}
	
	public void unblockBot(BotInfo mBotInfo)
	{
		mBotInfo.setBlocked(false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, mBotInfo.getMsisdn());
		
		HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(mBotInfo);
	}

}
