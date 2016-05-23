package com.bsb.hike.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bsb.hike.R;

import java.util.HashMap;

/**
 * Created by gauravmittal on 13/05/16.
 */
public class CustomTabsBar {

    private Context mContext;

    private ViewGroup parentLayout;

    private LinearLayout actionBar;

    private HashMap<Integer, Tab> tabs;

    private Tab currentSelectedTab;

    private LayoutInflater inflater;

    CustomTabsBar(Context ctx, ViewGroup layout) {
        this.mContext = ctx;
        this.parentLayout = layout;
        tabs = new HashMap<>();
        initLayouts();
    }

    public void initLayouts() {
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        actionBar = new LinearLayout(mContext);
        actionBar.setOrientation(LinearLayout.HORIZONTAL);
        actionBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parentLayout.addView(actionBar);
    }

    public Tab newTab(int tabId)
    {
        return new Tab(tabId);
    }

    public void addTab(Tab tab)
    {
        View tabView = tab.getView();
        tabs.put(tab.getId(), tab);
        actionBar.addView(tabView);
        tabView.setTag(tab);
        tabView.setOnClickListener(onTabClickListener);
    }

    View.OnClickListener onTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectTab((Tab) v.getTag());
        }
    };

    public void selectTab(int tabId)
    {
        selectTab(tabs.get(tabId));
    }

    public void selectTab(Tab tab)
    {
        if (currentSelectedTab != null && currentSelectedTab.getId() == tab.getId())
        {
            tab.reselect();
        }
        else
        {
            Tab oldTab = currentSelectedTab;
            currentSelectedTab = tab;
            currentSelectedTab.select();
            if (oldTab != null) {
                oldTab.unselect();
            }
        }
    }

    public class Tab {

        int id;

        View mView;

        View mCustomView;

        Drawable iconDrawable;

        int iconResId;

        CustomTabListener customTabListener;

        Tab(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public CustomTabsBar.Tab setIcon(Drawable icon) {
            iconDrawable = icon;
            return this;
        }

        public CustomTabsBar.Tab setIcon(int resId) {
            iconResId = resId;
            return this;
        }

        public CustomTabsBar.Tab setCustomView(View view) {
            mCustomView = view;
            return this;
        }

        public View getCustomView() {
            return mCustomView;
        }

        private View getView() {
            return mCustomView == null ? getDefaultView() : mCustomView;
        }

        private View getDefaultView() {
            if (mView == null) {
                mView = inflater.inflate(R.layout.custom_tab, actionBar, false);
                if (iconResId > 0)
                    ((ImageView)mView.findViewById(R.id.tab_icon)).setImageResource(iconResId);
                else if (iconDrawable != null)
                    ((ImageView)mView.findViewById(R.id.tab_icon)).setImageDrawable(iconDrawable);
            }
            return mView;
        }

        public CustomTabsBar.Tab setCustomTabListener(CustomTabListener listener) {
            this.customTabListener = listener;
            return this;
        }

        private void select() {
            ((ViewGroup) getView()).dispatchSetSelected(true);
            if (customTabListener != null)
                customTabListener.onTabSelected(this);

        }

        public void unselect() {
            ((ViewGroup) getView()).dispatchSetSelected(false);
            if (customTabListener != null)
                customTabListener.onTabUnselected(this);

        }

        public void reselect() {
            if (customTabListener != null)
                customTabListener.onTabReselected(this);
        }
    }

    public interface CustomTabListener {
        void onTabSelected(Tab var1);

        void onTabUnselected(Tab var1);

        void onTabReselected(Tab var1);
    }

    public interface CustomTabBadgeCounterListener {
        void onBadgeCounterUpdated(int newCount);
    }
}
