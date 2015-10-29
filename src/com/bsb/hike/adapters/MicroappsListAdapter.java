package com.bsb.hike.adapters;

import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ActivityInfo;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MicroappsListAdapter extends RecyclerView.Adapter<MicroappsListAdapter.ViewHolder> implements OnClickListener, Listener
{
	Context mContext;

	List<BotInfo> microappsList;

	IconLoader iconLoader;
	
	private static final String TAG = "BotDiscovery";

	private OnClickListener onClickListener;
	
	private static final String APP_NAME = "appName";
	
	private HikeDialog dialog;
	
	private String[] pubSubListeners;

	public MicroappsListAdapter(Context context, List<BotInfo> botsList, IconLoader iconLoader)
	{
		this.mContext = context;
		this.microappsList = botsList;
		this.iconLoader = iconLoader;
		onClickListener = this;
		
		pubSubListeners = new String[]{ HikePubSub.BOT_CREATED };
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onClick(View v)
	{
		if (v.getTag() == null)
		{
			return;
		}
		
		BotInfo mBotInfo = microappsList.get((int)v.getTag());
		
		boolean userHasBot = BotUtils.isBot(mBotInfo.getMsisdn());
		
		// User doesn't have the bot.
		if(!userHasBot)
		{
			showDialog(mBotInfo);
			
			return;
		}
		
		//user has the bot already
		String msisdn = mBotInfo.getMsisdn();
		mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		
		if (mBotInfo != null && mBotInfo.isNonMessagingBot())
		{
			BotUtils.unblockBotIfBlocked(mBotInfo);
			if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn()))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.ADD_NM_BOT_CONVERSATION, mBotInfo);
			}
			openBot(mBotInfo);
		}
		
		else if (mBotInfo != null && mBotInfo.isMessagingBot())
		{
			if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn()))
			{
				// Using the one from the microapp list to get the description of the bot sent in the add_di_bot packet.
				showDialog(microappsList.get((int)v.getTag()));
			}
			else
			{
				BotUtils.unblockBotIfBlocked(mBotInfo);
				openBot(mBotInfo);
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
	
	private void openBot(BotInfo mBotInfo)
	{
		if (mContext != null)
		{
			mContext.startActivity(IntentFactory.getIntentForBots(mBotInfo, mContext));	
		}
		else
		{
			Logger.e(TAG, "Context is null while trying to open the bot ");
		}
	}
	
	private void initiateBotDownload(final String msisdn)
	{
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(msisdn);
		try
		{
			json.put(APP_NAME, jsonArray);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Bot download request Json Exception: "+e.getMessage());
		}
		RequestToken token = HttpRequests.microAppPostRequest(HttpRequestConstants.getBotDownloadUrl(), json, new IRequestListener()
		{
			
			@Override
			public void onRequestSuccess(Response result)
			{
				Logger.v(TAG, "Bot download request success for "+msisdn);
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.v(TAG, "Bot download request failure for "+msisdn);
				Toast.makeText(mContext, ""+mContext.getResources().getString(R.string.error_sharing), Toast.LENGTH_SHORT).show();
				if (dialog != null)
				{
					dialog.dismiss();
				}
			}
		});
		if (!token.isRequestRunning())
		{
			token.execute();
		}
	}
	
	private void showDialog(final BotInfo mBotInfo)
	{
		if (mContext instanceof Activity)
		{
			((Activity)mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		dialog = HikeDialogFactory.showDialog(mContext, HikeDialogFactory.MAPP_DOWNLOAD_DIALOG, new HikeDialogListener() {
			@Override
			public void positiveClicked(HikeDialog hikeDialog) {

				if (BotUtils.isBot(mBotInfo.getMsisdn())) {
					BotUtils.unblockBotIfBlocked(BotUtils.getBotInfoForBotMsisdn(mBotInfo.getMsisdn()));
				}

				initiateBotDownload(mBotInfo.getMsisdn());

				BotUtils.discoveryBotDownloadAnalytics(mBotInfo.getMsisdn(), mBotInfo.getConversationName());

				hikeDialog.findViewById(R.id.bot_description).setVisibility(View.GONE);
				hikeDialog.findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
				hikeDialog.findViewById(R.id.button_panel).setVisibility(View.INVISIBLE);
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog) {
				hikeDialog.dismiss();
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog) {
				hikeDialog.dismiss();
			}
		}, new Object[]{mBotInfo});

		if (dialog != null)
		{
			dialog.data = mBotInfo.getMsisdn();
			this.iconLoader.loadImage(mBotInfo.getMsisdn(), (ImageView) dialog.findViewById(R.id.bot_icon), false, false, true);
		}

	}

	public void releaseResources()
	{
		removePubSubListeners();
		if (dialog != null)
		{
			dialog.dismiss();
		}
	}
	
	public void removePubSubListeners()
	{
		if (pubSubListeners != null)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.BOT_CREATED:
			String msisdn = ((BotInfo)object).getMsisdn();
			Logger.i(TAG, "Bot created : "+msisdn);
			if (dialog != null)
			{
				dialog.dismiss();
				if (dialog.data instanceof String && msisdn.equals((String)dialog.data))
				{
					openBot((BotInfo)object);
				}
			}
			break;
		}
	}
	
}
