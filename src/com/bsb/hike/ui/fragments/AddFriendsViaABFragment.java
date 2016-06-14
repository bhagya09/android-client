package com.bsb.hike.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendRequestAdapter;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class AddFriendsViaABFragment extends ListFragment {

    private ListView listView;

    private MenuItem searchMenuItem;

    private FriendRequestAdapter mAdapter;

    private List<ContactInfo> getToAddContactList() {
        List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts(true);
        List<ContactInfo> toAddHikecontacts = new ArrayList<>();
        List<ContactInfo> toAddSMScontacts = new ArrayList<>();
        for (ContactInfo info : allContacts) {
            if (!info.isBot() && !info.isMyOneWayFriend()) {
                if (info.isOnhike()) {
                    toAddHikecontacts.add(info);
                } else {
                    toAddSMScontacts.add(info);
                }
            }
        }
        if (toAddHikecontacts.size() > 0) {
            String hikeContacts = getString(R.string.hike_contacts);
            ContactInfo info = new ContactInfo(FriendRequestAdapter.ViewType.PINNED_SECTION.toString(), hikeContacts, hikeContacts, hikeContacts);
            toAddHikecontacts.add(0, info);
        }
        if (toAddSMScontacts.size() > 0){
            String smsContacts = getString(R.string.sms_contacts);
            ContactInfo info = new ContactInfo(FriendRequestAdapter.ViewType.PINNED_SECTION.toString(), smsContacts, smsContacts, smsContacts);
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
        mAdapter = new FriendRequestAdapter(getToAddContactList(), getActivity());
        return parent;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = getListView();
        setListAdapter(mAdapter);
        listView.setOnItemClickListener(onItemClickListener);
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
        inflater.inflate(R.menu.friend_req_menu, menu);
        setupSearchOptionItem(menu);
        super.onCreateOptionsMenu(menu, inflater);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
