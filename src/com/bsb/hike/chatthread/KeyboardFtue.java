package com.bsb.hike.chatthread;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.KPTAddonItem;

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gauravmittal on 28/10/15.
 */
public class KeyboardFtue implements HikePubSub.Listener
{
    public interface OnKeyboardFTUEStateChangeListener
    {
        void onStateChange(int state);
        void onDestroyed();
    }

    private final String KEYBOARD_FTUE_STATE = "keyboardFTUEState";
    public static final int NOT_STARTED = 0;
    public static final int LANGUAGE_SELECTION_COMPLETE = 1;
    public static final int COMPLETE = 3;
    private int mState;
    private boolean mInitialised;

    private ViewGroup container;

    private Activity mActivity;

    private View rootView;

    private ViewFlipper flipper;

    private OnKeyboardFTUEStateChangeListener stateChangeListener;

    ArrayList<KPTAddonItem> addonItems;

    LanguageItemAdapter addonItemAdapter;

    String[] mPubSubListeners = new String[] { HikePubSub.KPT_LANGUAGES_UPDATED, HikePubSub.KPT_LANGUAGES_INSTALLATION_FINISHED };

    private int originallyInstalledLanguageCount;

    private int toInstallLanguageCount;

    public KeyboardFtue()
    {
        mState = HikeSharedPreferenceUtil.getInstance().getData(KEYBOARD_FTUE_STATE,NOT_STARTED);
    }

    public void init(Activity activity, LayoutInflater inflater, ViewGroup container, OnKeyboardFTUEStateChangeListener listener)
    {
        this.mActivity = activity;
        this.container = container;
        this.stateChangeListener = listener;
        rootView = inflater.inflate(R.layout.keyboard_ftue_layout, container, false);
        mInitialised = true;
        addToPubSub();
    }

    private void addToPubSub()
    {
        HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
    }

    private void setupFlipper()
    {
        container.addView(rootView);
        flipper = (ViewFlipper) rootView.findViewById(R.id.flipper);
        flipper.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            }
        });
    }

    public boolean isFTUEComplete()
    {
        return (mState == COMPLETE);
    }

    public boolean isReadyForFTUE()
    {
        if (!mInitialised)
            return false;

        if (mState < COMPLETE && KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList().size() > KptKeyboardManager.PREINSTALLED_LANGUAGE_COUNT)
            return true;
        else if (mState == NOT_STARTED)
            return true;
        else
            return false;
    }

    public void showNextFtue()
    {
        if (!mInitialised)
            return;

        if (flipper == null)
            setupFlipper();

        if (mState < COMPLETE && KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList().size() > KptKeyboardManager.PREINSTALLED_LANGUAGE_COUNT)
            showLanguageUseFtue();
        else if (mState == NOT_STARTED)
            showLanguageSelectionFtue();
        else
            destroy();
    }

    public void showLanguageSelectionFtue()
    {
        prepareIntroLayout();
    }

    public void showLanguageUseFtue()
    {
        prepareSwipeLayout();
    }

    private void prepareIntroLayout()
    {
        flipper.setDisplayedChild(0);
        trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_INITIATED);
        
        flipper.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipLanguageSelection();
                trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_CLOSE_BUTTON);
            }
        });
        flipper.findViewById(R.id.btn_choose_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareLanguageListLayout();
                trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_CHOOSE_LANGUAGE_BUTTON);
            }
        });
    }

    /*
     * This method is to track the analytic events on various ftue clicks
     */
    private void trackClickAnalyticEvents(String event)
    {
    	try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, event);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
    }
    
    private void prepareLanguageListLayout()
    {
        flipper.setDisplayedChild(1);
        refreshActionPanel();
        setupLanguageList();
        flipper.findViewById(R.id.btn_negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipLanguageSelection();
                trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_CLOSE_LANG_SELECTION);
            }
        });
        flipper.findViewById(R.id.btn_positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_INSTALL_SELECTED_LANGUAGES);
                installSelectedLangauges();
                refreshActionPanel();
            }
        });
    }

    private void prepareSwipeLayout()
    {
        if (flipper.getDisplayedChild() == 2)
            return;
        flipper.setDisplayedChild(2);
        flipper.findViewById(R.id.langauage_layout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
//            	tracking analytic event for keyboard ftue completion
                trackClickAnalyticEvents(HikeConstants.LogEvent.KEYBOARD_FTUE_COMPLETES);
            	
            	updateState(COMPLETE);
                showNextFtue();
            }
        });
        resetSwipeAnimation();
    }

    private void resetSwipeAnimation()
    {
        int startOffsetTime = 1200;
        int durationTime = 1000;
        int holdTime = 1200;
        TranslateAnimation translateText1Animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,-1.0f,Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,0.0f);
        translateText1Animation.setStartOffset(startOffsetTime);
        translateText1Animation.setDuration(durationTime);
        translateText1Animation.setFillAfter(true);
        translateText1Animation.setFillBefore(true);
        translateText1Animation.setFillEnabled(true);
        TranslateAnimation translateText2Animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,1.0f,Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,0.0f);
        translateText2Animation.setStartOffset(startOffsetTime);
        translateText2Animation.setDuration(durationTime);
        translateText2Animation.setFillAfter(true);
        translateText2Animation.setFillBefore(true);
        translateText2Animation.setFillEnabled(true);
        TranslateAnimation translateHandAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,2.2f,Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF,0.0f);
        translateHandAnimation.setStartOffset(startOffsetTime);
        translateHandAnimation.setDuration(durationTime);
        AlphaAnimation alphaHandAnimation = new AlphaAnimation(1,0);
        alphaHandAnimation.setStartOffset(startOffsetTime);
        alphaHandAnimation.setDuration(durationTime);
        RotateAnimation fakeAnimation = new RotateAnimation(0,1);
        fakeAnimation.setStartOffset(startOffsetTime + durationTime);
        fakeAnimation.setDuration(holdTime);

        AnimationSet handAnimationSet = new AnimationSet(true);
        handAnimationSet.addAnimation(translateHandAnimation);
        handAnimationSet.addAnimation(alphaHandAnimation);
        handAnimationSet.addAnimation(fakeAnimation);
        handAnimationSet.setFillAfter(true);
        handAnimationSet.setFillBefore(true);
        handAnimationSet.setFillEnabled(true);
        handAnimationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                resetSwipeAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        flipper.findViewById(R.id.txt_language_1).startAnimation(translateText1Animation);
        flipper.findViewById(R.id.txt_language_2).startAnimation(translateText2Animation);
        flipper.findViewById(R.id.hand).startAnimation(handAnimationSet);
    }
    private void refreshActionPanel()
    {
        if (flipper != null)
        {
            Byte keyboardManagerState = KptKeyboardManager.getInstance(mActivity).getCurrentState();
            if (keyboardManagerState == KptKeyboardManager.WAITING)
            {
                flipper.findViewById(R.id.action_panel).setVisibility(View.VISIBLE);
                flipper.findViewById(R.id.waiting_panel).setVisibility(View.GONE);
            }
            else
            {
                flipper.findViewById(R.id.action_panel).setVisibility(View.GONE);
                flipper.findViewById(R.id.waiting_panel).setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupLanguageList()
    {
        addonItems = new ArrayList<KPTAddonItem>();
        addonItemAdapter = new LanguageItemAdapter(mActivity, R.layout.keyboard_ftue_language_list_item, addonItems);
        ListView langList = (ListView) flipper.findViewById(R.id.lang_list);
        langList.setAdapter(addonItemAdapter);
        langList.setOnItemClickListener(addonItemAdapter);
        refreshLanguageList();
    }

    private void refreshLanguageList()
    {
        if (addonItems != null)
        {
            addonItems.clear();
            addonItems.addAll(KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList());
            addonItems.addAll(KptKeyboardManager.getInstance(mActivity).getUninstalledLanguagesList());
            addonItemAdapter.notifyDataSetChanged();
        }
    }

    private void updateState(int state)
    {
        mState = state;
        HikeSharedPreferenceUtil.getInstance().saveData(KEYBOARD_FTUE_STATE, state);
        if (stateChangeListener != null)
            stateChangeListener.onStateChange(mState);
    }

    private void skipLanguageSelection()
    {
        finishLanguageSelectionFTUE();
    }

    private void finishLanguageSelectionFTUE()
    {
        updateState(LANGUAGE_SELECTION_COMPLETE);
        showNextFtue();
    }

    private void installSelectedLangauges()
    {
        originallyInstalledLanguageCount = KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList().size();
        toInstallLanguageCount = addonItemAdapter.getSelectedItems().size();
        if (toInstallLanguageCount > 0)
        {
            for (KPTAddonItem item : addonItemAdapter.getSelectedItems())
            {
                KptKeyboardManager.getInstance(mActivity).downloadAndInstallLanguage(item);
                
//                tracking download of each language in ftue
                try
        		{
        			JSONObject metadata = new JSONObject();
        			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.KEYBOARD_FTUE_LANGUAGE_DOWNLOADED);
        			metadata.put(HikeConstants.KEYBOARD_LANGUAGE, item.getDisplayName());
        			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
        		}
        		catch(JSONException e)
        		{
        			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
        		}
            }
        }
        else
        {
            onInstallationComplete();
        }
    }

    private void onInstallationComplete()
    {
        if (KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList().size()
                >= (originallyInstalledLanguageCount + toInstallLanguageCount))
        {
            onInstallationSuccess();
        }
        else
        {
            onInstallationFailed();
        }
    }

    private void onInstallationSuccess()
    {
        finishLanguageSelectionFTUE();
    }

    private void onInstallationFailed()
    {
        Toast.makeText(mActivity,mActivity.getString(R.string.restore_error),Toast.LENGTH_SHORT).show();
        Button retry = (Button) flipper.findViewById(R.id.btn_positive);
        retry.setText(mActivity.getString(R.string.retry));
        addonItemAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEventReceived(final String type, Object object)
    {
        mActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                switch (type)
                {
                    case HikePubSub.KPT_LANGUAGES_UPDATED:
                        refreshLanguageList();
                        refreshActionPanel();
                        break;
                    case HikePubSub.KPT_LANGUAGES_INSTALLATION_FINISHED:
                        onInstallationComplete();
                        break;
                }
            }
        });
    }

    public void destroy()
    {
        if (mInitialised)
        {
            container.removeAllViews();
            container.invalidate();
            removeFromPubSub();
            mInitialised = false;
            if (stateChangeListener != null)
                stateChangeListener.onDestroyed();
        }
    }

    private void removeFromPubSub()
    {
        HikeMessengerApp.getPubSub().removeListeners(this, mPubSubListeners);
    }

    class LanguageItemAdapter extends ArrayAdapter<KPTAddonItem> implements AdapterView.OnItemClickListener {

        Context mContext;

        LayoutInflater inflater;

        HashSet<KPTAddonItem> selectedItems;

        class ViewHolder
        {
            CheckBox checkBoxItem;
        }

        public LanguageItemAdapter(Context context, int resource, ArrayList<KPTAddonItem> addonItems)
        {
            super(context, resource, addonItems);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
            selectedItems = new HashSet<KPTAddonItem>();
        }

        public HashSet<KPTAddonItem> getSelectedItems()
        {
            return selectedItems;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            ViewHolder viewHolder;
            viewHolder = (ViewHolder) view.getTag();
            if (viewHolder.checkBoxItem.isEnabled())
                viewHolder.checkBoxItem.performClick();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            KPTAddonItem item = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null)
            {
                convertView = inflater.inflate(R.layout.keyboard_ftue_language_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.checkBoxItem = (CheckBox) convertView.findViewById(R.id.checkbox_item);
                convertView.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.checkBoxItem.setText(item.getDisplayName());
            KptKeyboardManager.LanguageDictionarySatus status = KptKeyboardManager.getInstance(mContext).getDictionaryLanguageStatus(item);
            if (status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_LOADED
                    || status == KptKeyboardManager.LanguageDictionarySatus.PROCESSING
                    || status == KptKeyboardManager.LanguageDictionarySatus.IN_QUEUE
                    || selectedItems.contains(item))
            {
                viewHolder.checkBoxItem.setChecked(true);
            }
            else
            {
                viewHolder.checkBoxItem.setChecked(false);
            }

            if(KptKeyboardManager.getInstance(mContext).getCurrentState() != KptKeyboardManager.WAITING || status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_LOADED
                    || status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_UNLOADED)
            {
                viewHolder.checkBoxItem.setEnabled(false);
            }
            else
            {
                viewHolder.checkBoxItem.setEnabled(true);
            }
            viewHolder.checkBoxItem.setClickable(false);
            viewHolder.checkBoxItem.setOnCheckedChangeListener(langItemCheckChangeListener);
            viewHolder.checkBoxItem.setTag(item);
            return convertView;
        }

        CompoundButton.OnCheckedChangeListener langItemCheckChangeListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                KPTAddonItem item = (KPTAddonItem) buttonView.getTag();
                if (isChecked)
                    selectedItems.add(item);
                else
                    selectedItems.remove(item);
            }
        };

        @Override
        public int getItemViewType(int position)
        {
            return -1;
        }

        @Override
        public int getViewTypeCount()
        {
            return 1;
        }
    }
}
