package com.bsb.hike.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendRequestAdapter;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFeatureInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import com.bsb.hike.adapters.FriendRequestAdapter.ViewType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class AddFriendsFragment extends ListFragment {

    private ListView listView;

    private FriendRequestAdapter mAdapter;

    private List<ContactInfo> getToAddContactList() {
        List<ContactInfo> toAddcontacts = getRecommendationsList();
        String quickAdd = getString(R.string.QUICK_ADD);
        ContactInfo info = new ContactInfo(ViewType.PINNED_SECTION.toString(), null, quickAdd, null);
        if (toAddcontacts.size() > 0) {
            toAddcontacts.add(0, info);
        }
        toAddcontacts.add(0, getAddressBookItem());
        return toAddcontacts;
    }

    private List<ContactInfo> getRecommendationsList() {
        HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
        Set<String> msisdnSet = settings.getStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, null);
        List<ContactInfo> finalContactList = new ArrayList<ContactInfo>();
        if (msisdnSet == null)
        {
            String mymsisdn = settings.getData(HikeMessengerApp.MSISDN_SETTING, "");
            String list = settings.getData(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null);
            msisdnSet = Utils.getServerRecommendedContactsSelection(list, mymsisdn);

            HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, msisdnSet);
            Logger.d("tl_ftue_ab_recommendations", "====== List from Server Reco:- " + msisdnSet);
        }
        else
        {
            Logger.d("tl_ftue_ab_recommendations", "====== Going to check fron list received from server packet" + msisdnSet);
        }
        if (msisdnSet != null)
        {
            Iterator<String> iterator = msisdnSet.iterator();
            while (iterator.hasNext())
            {
                String id = iterator.next();
                ContactInfo c = ContactManager.getInstance().getContact(id, true, true);

                if(c == null || c.getFavoriteType() == null || c.getMsisdn() == null)
                {
                    Logger.d("tl_ftue_ab_recommendations", "NPE: favourite null");
                    continue;
                }

                if (!c.isBot() && !c.isMyOneWayFriend())
                {
                    Logger.d("tl_ftue_ab_recommendations", id + " is not a frnd so adding for ftue list :- " + c.getName() +", "+ c.getFavoriteType());
                    finalContactList.add(c);
                }
                else
                {
                    Logger.d("tl_ftue_ab_recommendations", id + " a frnd or a Bot so NOT ADDING.... for ftue list :- " + c.getName() +", "+ c.getFavoriteType());
                }
            }
        }
        return finalContactList;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_add_friend, null);
        mAdapter = new FriendRequestAdapter(getToAddContactList(), getActivity());
        return parent;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupList();
    }

    private void setupList() {
        listView = getListView();
        setListAdapter(mAdapter);
        listView.setOnItemClickListener(onItemClickListener);
    }

    private ContactInfo getAddressBookItem() {
        HikeFeatureInfo info = new HikeFeatureInfo(getString(R.string.add_via_address_book), 0, null, false, IntentFactory.getFriendReqActivityAddFriendsViaABIntent(getContext()));
        info.setId(ViewType.BASIC_ITEM.toString());
        return info;
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ContactInfo contact = mAdapter.getItem(position);
            if (contact.getId().equals(ViewType.PINNED_SECTION.toString())) {
                return;
            } else if (contact.getId().equals(ViewType.BASIC_ITEM.toString())) {
                if (contact.getName().equals(getString(R.string.add_via_address_book)))
                    startActivity(((HikeFeatureInfo) contact).getFireIntent());
            } else {
                Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(getContext(), contact, false, false, ChatThreadActivity.ChatThreadOpenSources.ADD_FRIEND_FRAG);
                startActivity(intent);
            }
        }
    };
}
