package com.bsb.hike.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class AddFriendsViaABFragment extends ListFragment implements HikePubSub.Listener {

    private ListView listView;

    private MenuItem searchMenuItem;

    private AddFriendAdapter mAdapter;

    private String[] pubSubListeners = {HikePubSub.CONTACT_SYNC_STARTED, HikePubSub.CONTACT_SYNCED};

    private List<ContactInfo> getToAddContactList() {
        List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts(true);
        List<ContactInfo> toAddHikecontacts = new ArrayList<>();
        List<ContactInfo> toAddSMScontacts = new ArrayList<>();
        for (ContactInfo info : allContacts) {
            if (!info.isBot()) {
                if (info.isOnhike()) {
                    toAddHikecontacts.add(info);
                } else {
                    toAddSMScontacts.add(info);
                }
            }
        }
        if (toAddHikecontacts.size() > 0) {
            String hikeContacts = getString(R.string.hike_contacts);
            ContactInfo info = new ContactInfo(AddFriendAdapter.ViewType.PINNED_SECTION.toString(), null, hikeContacts,  null);
            toAddHikecontacts.add(0, info);
        }
        if (toAddSMScontacts.size() > 0){
            String smsContacts = getString(R.string.sms_contacts);
            ContactInfo info = new ContactInfo(AddFriendAdapter.ViewType.PINNED_SECTION.toString(), null, smsContacts, null);
            toAddSMScontacts.add(0, info);
        }
        allContacts.clear();
        allContacts.addAll(toAddHikecontacts);
        allContacts.addAll(toAddSMScontacts);
        return allContacts;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View parent = inflater.inflate(R.layout.fragment_add_friend, null);
        mAdapter = new AddFriendAdapter(getToAddContactList(), getActivity());
        return parent;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = getListView();
        setListAdapter(mAdapter);
        listView.setOnItemClickListener(onItemClickListener);
        HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
    }

    @Override
    public void onDestroyView() {
        HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
        super.onDestroyView();
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ContactInfo contact = mAdapter.getItem(position);
            Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(getContext(), contact, false, false, ChatThreadActivity.ChatThreadOpenSources.ADD_FRIEND_FRAG);
            startActivity(intent);
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.add_friend_menu, menu);
        setupSearchOptionItem(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.refresh_contacts)
        {
            if(HikeMessengerApp.syncingContacts)
                return super.onOptionsItemSelected(item);
            if(!Utils.isUserOnline(getContext()))
            {
                Utils.showNetworkUnavailableDialog(getContext());
                return super.onOptionsItemSelected(item);
            }
            Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
            contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
            getActivity().sendBroadcast(contactSyncIntent);

            try
            {
                JSONObject metadata = new JSONObject();
                metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ADD_FRIEND_AB_REFRESH_CONTACTS);
                HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
            }
            catch(JSONException e)
            {
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSearchOptionItem(final Menu menu) {
        searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(onQueryTextListener);
        searchView.setQueryHint(getString(R.string.search));
        searchView.clearFocus();

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                setupSearch();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                endSearch();
                return true;
            }
        });
    }

    private SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            Utils.hideSoftKeyboard(getActivity().getApplicationContext(), searchMenuItem.getActionView());
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (!TextUtils.isEmpty(newText))
                newText = newText.toLowerCase().trim();
            mAdapter.onSearchQueryChanged(newText, null);
            updateListEmptyState(newText);
            return true;
        }
    };

    public boolean isSearchModeOn() {
        return searchMenuItem.isActionViewExpanded();
    }

    private void setupSearch() {
        mAdapter.setupSearchMode();
    }

    private void endSearch() {
        mAdapter.endSearchMode();
    }

    private void updateListEmptyState(String searchText) {
        String emptyText = String.format(getActivity().getString(R.string.home_search_empty_text), searchText);
        TextView emptyTextView = (TextView) getView().findViewById(R.id.empty_search_txt);
        if (emptyTextView != null) {
            if (!TextUtils.isEmpty(searchText)) {
                SpannableString spanEmptyText = new SpannableString(emptyText);
                String darkText = "'" + searchText + "'";
                int start = spanEmptyText.toString().indexOf(darkText);
                int end = start + darkText.length();
                spanEmptyText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.standard_light_grey2)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                emptyTextView.setText(spanEmptyText, TextView.BufferType.SPANNABLE);
            } else {
                emptyTextView.setText(emptyText);
            }
        }
    }

    @Override
    public void onEventReceived(String type, Object object) {
        if (HikePubSub.CONTACT_SYNC_STARTED.equals(type)) {
        } else if (HikePubSub.CONTACT_SYNCED.equals(type)) {
            Pair<Boolean, Byte> ret = (Pair<Boolean, Byte>) object;
            final byte contactSyncResult = ret.second;
            // Dont repopulate list if no sync changes
            if (contactSyncResult == ContactManager.SYNC_CONTACTS_CHANGED) {
                mAdapter.updateList(getToAddContactList());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

    }
}
