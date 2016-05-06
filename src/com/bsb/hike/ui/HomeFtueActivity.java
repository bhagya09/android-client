package com.bsb.hike.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.backup.HikeCloudSettingsManager;
import com.bsb.hike.backup.impl.HikeSettingsCloudBackup;
import com.bsb.hike.localisation.LocalLanguage;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by gauravmittal on 19/11/15.
 */
public class HomeFtueActivity extends HikeAppStateBaseFragmentActivity {

    private static final int LOCALIZATION = 1;

    private ViewFlipper flipper;

    private LocalLanguage selectedLocalLanguage;

    private TextView mActionBarTitle;

    private View submitBtnContainer;

    private TextView submitBtnText;

    public static boolean isFtueToBeShown()
    {
        // Localized keyboard is for india users only. Other users still have setting but do not see the FTUE
        // If custom keyboard is disabled no need to show the FTUE.
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOCALIZATION_FTUE_COMPLETE, false) && HikeMessengerApp.isIndianUser()
                && HikeMessengerApp.isLocalisationEnabled())
        {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_ftue);
        setUpView();
        SendLogsForAppLangFtueShown();
        showNextFtue();
    }

    private void SendLogsForAppLangFtueShown()
    {
    	try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.APP_LANGUAGE_FTUE_SHOWN_EVENT);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
    }

    private void setUpView() {
        setupActionBar();
        flipper = (ViewFlipper) findViewById(R.id.home_ftue_flipper);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setIcon(R.drawable.hike_logo_top_bar);

        View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);

        mActionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
        submitBtnText = (TextView) actionBarView.findViewById(R.id.next_btn);
        submitBtnContainer = actionBarView.findViewById(R.id.next_btn_container);

        submitBtnContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitClicked();
            }
        });
        actionBar.setCustomView(actionBarView);
        Toolbar parent = (Toolbar) actionBarView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
    }

    private void showNextFtue() {
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOCALIZATION_FTUE_COMPLETE, false)) {
            showLocalizationFtue();
            return;
        }
        completeFtue();
    }

    private void completeFtue()
    {
        IntentFactory.openHomeActivity(HomeFtueActivity.this);
        //Not calling finish since it updates activity state as back pressed and then onResume is not handled 
    }

    private void refreshActionBar() {
        switch (flipper.getDisplayedChild()) {
            case LOCALIZATION:
                mActionBarTitle.setText(R.string.language);
                submitBtnText.setText(R.string.done);
                submitBtnContainer.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

    }

    private void submitClicked()
    {
        if (flipper.getDisplayedChild() == LOCALIZATION)
        {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LOCALIZATION_FTUE_COMPLETE, true);
            addAnalyticsForDoneButton();
            HikeCloudSettingsManager.getInstance().doBackup();
            showNextFtue();
        }
    }
    
    private void addAnalyticsForDoneButton() {
    	try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.APP_FTUE_DONE_BTN);
			metadata.put(HikeConstants.APP_LANGUAGE, LocalLanguageUtils.getApplicationLocalLanguageLocale());
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
    }
    
    private void showLocalizationFtue() {
        flipper.setDisplayedChild(LOCALIZATION);
        final TextView languageText = (TextView) flipper.findViewById(R.id.txt_lang);

        if (LocalLanguageUtils.isLocalLanguageSelected())
        {
            selectedLocalLanguage = LocalLanguageUtils.getApplicationLocalLanguage(HomeFtueActivity.this);
            languageText.setText(selectedLocalLanguage.getDisplayName());

        }
        else
        {
            languageText.setText(LocalLanguage.PhoneLangauge.getDisplayName());
        }

        flipper.findViewById(R.id.lang_select).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	
                    	sendAppLanguageDialogShownEvent();
                        final ArrayList<LocalLanguage> list = new ArrayList<>(LocalLanguage.getDeviceSupportedHikeLanguages(HomeFtueActivity.this));
                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeFtueActivity.this);
                        ListAdapter adapter = new ArrayAdapter<>(HomeFtueActivity.this, R.layout.alert_item, R.id.item, list);
                        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (LocalLanguageUtils.getApplicationLocalLanguageLocale().equals(list.get(which).getLocale())) {
                                    return;
                                }
                                selectedLocalLanguage = list.get(which);
                                languageText.setText(selectedLocalLanguage.getDisplayName());
                                LocalLanguageUtils.setApplicationLocalLanguage(selectedLocalLanguage, HikeConstants.APP_LANG_CHANGED_FTUE);
                                Utils.sendLocaleToServer();
                                // Relaunching the Activity
                                IntentFactory.openHomeFtueActivity(HomeFtueActivity.this);
                            }
                        });

                        AlertDialog alertDialog = builder.show();
                        alertDialog.getListView().setDivider(null);
                        alertDialog.getListView().setPadding(0, getResources().getDimensionPixelSize(R.dimen.menu_list_padding_top), 0,
                                getResources().getDimensionPixelSize(R.dimen.menu_list_padding_bottom));
                    }
                }
        );
        refreshActionBar();
    }

    private void sendAppLanguageDialogShownEvent()
    {
    	try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.APP_LANGUAGE_DIALOG_OPEN_EVENT);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }
}
