package com.bsb.hike.ui;

/**
 * Created by gauravmittal on 18/05/16.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.db.HikeContentDatabase;
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
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.hike.transporter.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by gauravmittal on 04/05/16.
 */
public class ServicesActivity extends HikeAppStateBaseFragmentActivity {

    Context mContext;

    List<BotInfo> microappsList;

    private HikeDialog dialog;

    IconLoader iconLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.services);


        GridView botGridview = (GridView) findViewById(R.id.bot_grid);

        iconLoader = new IconLoader(this, getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
        iconLoader.setDefaultAvatarIfNoCustomIcon(true);
        iconLoader.setImageFadeIn(false);
        microappsList = HikeContentDatabase.getInstance().getDiscoveryBotInfoList();

        botGridview.setAdapter(la);

        mContext = this;

        setupActionBar();

    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);


        TextView title = (TextView) actionBarView.findViewById(R.id.title);
        title.setText(R.string.services);
        actionBar.setCustomView(actionBarView);
        Toolbar parent = (Toolbar) actionBarView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null) {
                return;
            }

            BotInfo mBotInfo = microappsList.get((int) v.getTag());

            analyticsForDiscoveryBotTap(mBotInfo.getConversationName());

            boolean userHasBot = BotUtils.isBot(mBotInfo.getMsisdn());

            // User doesn't have the bot.
            if (!userHasBot) {
                showDialog(mBotInfo);

                return;
            }

            //user has the bot already
            String msisdn = mBotInfo.getMsisdn();
            mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

            if (mBotInfo != null && mBotInfo.isNonMessagingBot()) {
                BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
                if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn())) {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.ADD_NM_BOT_CONVERSATION, mBotInfo);
                }
                openBot(mBotInfo);
            } else if (mBotInfo != null && mBotInfo.isMessagingBot()) {
                if (!HikeConversationsDatabase.getInstance().isConversationExist(mBotInfo.getMsisdn())) {
                    // Using the one from the microapp list to get the description of the bot sent in the add_di_bot packet.
                    showDialog(microappsList.get((int) v.getTag()));
                } else {
                    BotUtils.unblockBotIfBlocked(mBotInfo, AnalyticsConstants.BOT_DISCOVERY);
                    openBot(mBotInfo);
                }
            }
        }
    };

    ListAdapter la = new ListAdapter() {
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;

            TextView name;

            LinearLayout showcase_item;

            public ViewHolder(View view) {
                super(view);
                image = (ImageView) view.findViewById(R.id.microapp_image);
                name = (TextView) view.findViewById(R.id.microapp_name);
                showcase_item = (LinearLayout) view.findViewById(R.id.showcase_item_layout);
                image.setOnClickListener(onClickListener);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return microappsList.size();
        }

        @Override
        public Object getItem(int position) {
            return microappsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.microapps_showcase_layout, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.image.setTag(position);
            iconLoader.loadImage(microappsList.get(position).getMsisdn(), holder.image, false, false, true);
            holder.name.setText(microappsList.get(position).getConversationName());
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    };

    private static final String TAG_BOT_DISCOVERY = "BotDiscovery";

    private void openBot(BotInfo mBotInfo) {
        if (mContext != null) {
            Intent intent = IntentFactory.getIntentForBots(mBotInfo, mContext, ChatThreadActivity.ChatThreadOpenSources.SERVICES);

            intent.putExtra(AnalyticsConstants.BOT_NOTIF_TRACKER, AnalyticsConstants.BOT_OPEN_SOURCE_DISC);

            mContext.startActivity(intent);
        } else {
            Logger.e(TAG_BOT_DISCOVERY, "Context is null while trying to open the bot ");
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
    private void initiateBotDownload(final String msisdn) {
        // Json to send to install.json on server requesting for micro app download for bot discovery
        JSONObject json = new JSONObject();

        try {
            // Json object to create adding params to micro app requesting json (In our scenario, we need to receive cbot only with enable bot as false for our scenario)
            JSONObject paramsJsonObject = new JSONObject();
            paramsJsonObject.put(HikePlatformConstants.ENABLE_BOT, true);
            paramsJsonObject.put(HikePlatformConstants.NOTIF, HikePlatformConstants.SETTING_OFF);

            // Json object containing all the information required for one micro app
            JSONObject appsJsonObject = new JSONObject();
            appsJsonObject.put(HikePlatformConstants.NAME, msisdn);
            appsJsonObject.put(HikePlatformConstants.PARAMS, paramsJsonObject);

            // Put apps JsonObject in the final json
            json.put(HikePlatformConstants.APPS, appsJsonObject);
            json.put(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);
            json.put(HikeConstants.SOURCE, HikePlatformConstants.BOT_DISCOVERY);
        } catch (JSONException e) {
            Logger.e("Json Exception :: ", e.toString());
        }

        RequestToken token = HttpRequests.microAppPostRequest(HttpRequestConstants.getBotDownloadUrlV2(), json, new IRequestListener() {

            @Override
            public void onRequestSuccess(Response result) {
                Logger.v(TAG_BOT_DISCOVERY, "Bot download request success for " + msisdn);
            }

            @Override
            public void onRequestProgressUpdate(float progress) {
            }

            @Override
            public void onRequestFailure(Response response, HttpException httpException)
            {
                com.hike.transporter.utils.Logger.v(ServicesActivity.class.getSimpleName(), "Bot download request failure for " + msisdn);
                Toast.makeText(mContext, "" + mContext.getResources().getString(R.string.error_sharing), Toast.LENGTH_SHORT).show();
                if (dialog != null)
                {
                    dialog.dismiss();
                }
            }
        });
        if (!token.isRequestRunning()) {
            token.execute();
        }
    }

    private void showDialog(final BotInfo mBotInfo) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        dialog = HikeDialogFactory.showDialog(mContext, HikeDialogFactory.MAPP_DOWNLOAD_DIALOG, new HikeDialogListener() {
            @Override
            public void positiveClicked(HikeDialog hikeDialog) {

                if (BotUtils.isBot(mBotInfo.getMsisdn())) {
                    BotUtils.unblockBotIfBlocked(BotUtils.getBotInfoForBotMsisdn(mBotInfo.getMsisdn()), AnalyticsConstants.BOT_DISCOVERY);
                }
                /**
                 * On resetting account, a previously blocked microapp will remain blocked.
                 * So we're checking if that msisdn is blocked before we initiate the bot download.
                 */
                else if (ContactManager.getInstance().isBlocked(mBotInfo.getMsisdn())) {
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

        if (dialog != null) {
            dialog.data = mBotInfo.getMsisdn();
            this.iconLoader.loadImage(mBotInfo.getMsisdn(), (ImageView) dialog.findViewById(R.id.bot_icon), false, false, true);
        }

    }

    public void releaseResources() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void analyticsForDiscoveryBotTap(String botName) {
        JSONObject json = new JSONObject();
        try {
            json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
            json.put(AnalyticsConstants.EVENT, AnalyticsConstants.DISCOVERY_BOT_TAP);
            json.put(AnalyticsConstants.LOG_FIELD_1, botName);
        } catch (JSONException e) {
            Logger.e(TAG_BOT_DISCOVERY, "JSON Exception in analyticsForDiscoveryBotTap " + e.getMessage());
        }
        HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.BOT_DISCOVERY, json);
    }

    public void onBotCreated(Object data) {
        if (data == null || (!(data instanceof BotInfo))) {
            return;
        }

        BotInfo botInfo = (BotInfo) data;

        String msisdn = botInfo.getMsisdn();
        Logger.i(TAG_BOT_DISCOVERY, "Bot created : " + msisdn);
        if (dialog != null) {
            dialog.dismiss();
            if (dialog.data instanceof String && msisdn.equals((String) dialog.data)) {
                openBot(botInfo);
            }
        }
    }
}