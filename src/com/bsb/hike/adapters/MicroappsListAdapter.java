package com.bsb.hike.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MicroappsListAdapter extends RecyclerView.Adapter<MicroappsListAdapter.ViewHolder> implements OnClickListener
{
	Context mContext;

	List<BotInfo> microappsList;

	IconLoader iconLoader;
	
	private static final String TAG = "BotDiscovery";

	private OnClickListener onClickListener;
	
	private static final String APP_NAME = "appName";
	
	private HikeDialog dialog;
	
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

		analyticsForDiscoveryBotTap(mBotInfo.getConversationName());
		
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
			BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
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
				BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
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
			Intent intent = IntentFactory.getIntentForBots(mBotInfo, mContext);
			
			intent.putExtra(AnalyticsConstants.BOT_NOTIF_TRACKER, AnalyticsConstants.BOT_OPEN_SOURCE_DISC);
			
			mContext.startActivity(intent);	
		}
		else
		{
			Logger.e(TAG, "Context is null while trying to open the bot ");
		}
	}

    /*
	 * Method to make a post call to server with necessary params requesting for bot discovery cbot
	 * Sample Json to be sent in network call ::
	 * {
            "app": [{
                "name": "+hikenews+",
                "params": {
                            "enable_bot": true,
                            "notif": "off"
                            }
                        }],
                "platform_version": 10
        }
	 */
	private void initiateBotDownload(final String msisdn)
	{
        // Json to send to install.json on server requesting for micro app download for bot discovery
        JSONObject json = new JSONObject();

        try
        {
            // Json object to create adding params to micro app requesting json (In our scenario, we need to receive cbot only with enable bot as false for our scenario)
            JSONObject paramsJsonObject = new JSONObject();
            paramsJsonObject.put(HikePlatformConstants.ENABLE_BOT,true);
            paramsJsonObject.put(HikePlatformConstants.NOTIF,HikePlatformConstants.SETTING_OFF);

            // Json object containing all the information required for one micro app
            JSONObject appsJsonObject = new JSONObject();
            appsJsonObject.put(HikePlatformConstants.NAME, msisdn);
            appsJsonObject.put(HikePlatformConstants.PARAMS,paramsJsonObject);

            // Put apps JsonObject in the final json
            json.put(HikePlatformConstants.APPS, appsJsonObject);
            json.put(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);
            json.put(HikeConstants.SOURCE, HikePlatformConstants.BOT_DISCOVERY);
        }
        catch (JSONException e)
        {
            Logger.e("Json Exception :: ", e.toString());
        }

		RequestToken token = HttpRequests.microAppPostRequest(HttpRequestConstants.getBotDownloadUrlV2(), json, new IRequestListener()
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

				if (BotUtils.isBot(mBotInfo.getMsisdn()))
				{
					BotUtils.unblockBotIfBlocked(BotUtils.getBotInfoForBotMsisdn(mBotInfo.getMsisdn()), AnalyticsConstants.BOT_DISCOVERY);
				}
				/**
				 * On resetting account, a previously blocked microapp will remain blocked.
				 * So we're checking if that msisdn is blocked before we initiate the bot download.
				 */
				else if (ContactManager.getInstance().isBlocked(mBotInfo.getMsisdn()))
				{
					ContactManager.getInstance().unblock(mBotInfo.getMsisdn());
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
		if (dialog != null)
		{
			dialog.dismiss();
		}
	}
	
	private void analyticsForDiscoveryBotTap(String botName)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
			json.put(AnalyticsConstants.EVENT, AnalyticsConstants.DISCOVERY_BOT_TAP);
			json.put(AnalyticsConstants.LOG_FIELD_1, botName);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSON Exception in analyticsForDiscoveryBotTap "+e.getMessage());
		}
		HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.BOT_DISCOVERY, json);
	}

	public void onBotCreated(Object data)
	{
		if (data == null || (!(data instanceof BotInfo)))
		{
			return;
		}

		BotInfo botInfo = (BotInfo) data;

		String msisdn = botInfo.getMsisdn();
		Logger.i(TAG, "Bot created : " + msisdn);
		if (dialog != null)
		{
			dialog.dismiss();
			if (dialog.data instanceof String && msisdn.equals((String) dialog.data))
			{
				openBot(botInfo);
			}
		}
	}
	
}
