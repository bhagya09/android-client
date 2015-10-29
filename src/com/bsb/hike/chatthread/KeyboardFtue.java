package com.bsb.hike.chatthread;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.kpt.adaptxt.beta.KPTAddonItem;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by gauravmittal on 28/10/15.
 */
public class KeyboardFtue implements HikePubSub.Listener
{
    public interface OnKeyboardFTUEDestroyedListener
    {
        void onDestroyed();
    }

    private final String KEYBOARD_FTUE_STATE = "keyboardFTUEState";
    private final int NOT_STARTED = 0;
    private final int LANGUAGE_SELECTION_COMPLETE = 1;
    private final int COMPLETE = 3;
    private int mState;
    private boolean mInitialised;

    private ViewGroup container;

    private Activity mActivity;

    private View rootView;

    private ViewFlipper flipper;

    private OnKeyboardFTUEDestroyedListener destroyedListener;

    ArrayList<KPTAddonItem> addonItems;

    LanguageItemAdapter addonItemAdapter;

    String[] mPubSubListeners = new String[] { HikePubSub.KPT_LANGUAGES_UPDATED, HikePubSub.KPT_LANGUAGES_INSTALLATION_FINISHED };

    private int originallyInstalledLanguageCount;

    private int toInstallLanguageCount;

    public KeyboardFtue()
    {
        mState = HikeSharedPreferenceUtil.getInstance().getData(KEYBOARD_FTUE_STATE,NOT_STARTED);
    }

    public void init(Activity activity, LayoutInflater inflater, ViewGroup container, OnKeyboardFTUEDestroyedListener listener)
    {
        this.mActivity = activity;
        this.container = container;
        this.destroyedListener = listener;
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
        flipper.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipLanguageSelection();
            }
        });
        flipper.findViewById(R.id.txt_choose_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareLanguageListLayout();
            }
        });
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
            }
        });
        flipper.findViewById(R.id.btn_positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                updateState(COMPLETE);
                showNextFtue();
            }
        });
    }

    private void refreshActionPanel()
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

    private void setupLanguageList()
    {
        addonItems = new ArrayList<KPTAddonItem>();
        addonItemAdapter = new LanguageItemAdapter(mActivity, R.layout.keyboard_ftue_language_list_item, addonItems);
        ListView langList = (ListView) flipper.findViewById(R.id.lang_list);
        langList.setAdapter(addonItemAdapter);
        refreshLanguageList();
    }

    private void refreshLanguageList()
    {
        addonItems.clear();
        addonItems.addAll(KptKeyboardManager.getInstance(mActivity).getInstalledLanguagesList());
        addonItems.addAll(KptKeyboardManager.getInstance(mActivity).getUninstalledLanguagesList());
        addonItemAdapter.notifyDataSetChanged();
    }

    private void updateState(int state)
    {
        mState = state;
        HikeSharedPreferenceUtil.getInstance().saveData(KEYBOARD_FTUE_STATE, state);
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
        container.removeAllViews();
        container.invalidate();
        removeFromPubSub();
        mInitialised = false;
        if (destroyedListener != null)
            destroyedListener.onDestroyed();
    }

    private void removeFromPubSub()
    {
        HikeMessengerApp.getPubSub().removeListeners(this, mPubSubListeners);
    }

    class LanguageItemAdapter extends ArrayAdapter<KPTAddonItem> {

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
        public View getView(int position, View convertView, ViewGroup parent)
        {
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
            KPTAddonItem item = getItem(position);

            viewHolder.checkBoxItem.setText(item.getDisplayName());
            KptKeyboardManager.LanguageDictionarySatus status = KptKeyboardManager.getInstance(mContext).getDictionaryLanguageStatus(item);
            if (status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED
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

            if(KptKeyboardManager.getInstance(mContext).getCurrentState() != KptKeyboardManager.WAITING || status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED)
            {
                viewHolder.checkBoxItem.setClickable(false);
            }
            else
            {
                viewHolder.checkBoxItem.setClickable(true);
            }
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
