package com.bsb.hike.tasks;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.FetchUIDTaskPojo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHttpTaskResult;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by himanshu on 04/05/16.
 */
public class FetchHikeUIDTaskForUpgrade implements IHikeHTTPTask, IHikeHttpTaskResult, IRequestListener {


    private Set<String> addressBookContact;
    private Set<String> activeChats;
    private Set<String> bots;
    private RequestToken token;

    public FetchHikeUIDTaskForUpgrade() {

    }

    private JSONObject getPostData() {
        JSONObject data = new JSONObject();
        try {
            JSONArray addressBook = new JSONArray();
            addressBookContact.removeAll(activeChats);
            addressBookContact.addAll(bots);
            data.put("othr", getJSONArrayFromSet(addressBook, addressBookContact));

            JSONArray activeChat = new JSONArray();
            data.put("subs", getJSONArrayFromSet(activeChat, activeChats));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    private JSONArray getJSONArrayFromSet(JSONArray arr, Set<String> data) {
        for (String s : data) {
            arr.put(s);
        }
        return arr;
    }

    private Set<FetchUIDTaskPojo> parseJSONArrayIntoSet(Set<FetchUIDTaskPojo> data, JSONArray array) {

        if (Utils.isEmpty(array)) {
            return data;
        }
        if (Utils.isEmpty(data)) {
            data = new HashSet<>(array.length());
        }

        for (int i = 0; i < array.length(); i++) {
            try {
                data.add(new FetchUIDTaskPojo(array.getJSONObject(i).optString("m",null), array.getJSONObject(i).optString("u",null)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    @Override
    public void execute() {
        getMsisdnForMissingUID();
        JSONObject d = getPostData();
        token = HttpRequests.fetchUIDForMissingMsisdn(getRequestId(), this,d);

        if (!token.isRequestRunning()) {
            token.execute();
        }


    }

    public String getRequestId() {
        return "reqId";
    }

    @Override
    public void cancel() {

        if (token != null)
            token.cancel();

    }

    @Override
    public void doOnSuccess(Object result) {
        Set<FetchUIDTaskPojo> addressBookContacts = null, botsContact = null, activeChats = null;

        JSONObject data = (JSONObject) result;
        addressBookContacts = parseJSONArrayIntoSet(addressBookContacts, data.optJSONArray("othr"));
        botsContact = new HashSet<>();
        for (FetchUIDTaskPojo pojo:addressBookContacts)
        {
            if(BotUtils.isBot(pojo.getMsisdn()))
            {
                botsContact.add(pojo);
            }
        }
        // removing bots from addressBook:
        addressBookContacts.removeAll(botsContact);

        activeChats = parseJSONArrayIntoSet(activeChats, data.optJSONArray("subs"));

        //Update in User Db
        HikeUserDatabase.getInstance().updateContactUid(addressBookContacts);

        //Update in User Db.
        HikeUserDatabase.getInstance().updateContactUid(activeChats);

        //update Bots Table
        HikeConversationsDatabase.getInstance().updateUIDForBot(botsContact);
        //save Pref for upgrade
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FETCH_UID_UPGRADE_SUCCESSFULL,true);
    }

    @Override
    public void doOnFailure(HttpException exception) {
        Logger.d(getClass().getSimpleName(),exception.getMessage() +"");
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FETCH_UID_UPGRADE_SUCCESSFULL,false);
    }

    @Override
    public void onRequestFailure(HttpException httpException) {
        doOnFailure(httpException);

    }

    @Override
    public void onRequestSuccess(Response result) {
        JSONObject response = (JSONObject) result.getBody().getContent();

        if (Utils.isResponseValid(response)) {
            doOnSuccess(response);
        }

    }

    @Override
    public void onRequestProgressUpdate(float progress) {

    }

    private void getMsisdnForMissingUID() {
        activeChats = new HashSet<>();
        //AddressBook Contact
        addressBookContact = HikeUserDatabase.getInstance().getMsisdnsForMissingHikeUID();


        //Active Chats

        List<ContactInfo> list = (ContactManager.getInstance().getAllConversationContactsSorted(false, false));

        // rejecting ontoNConv,Bots,onSms
        for (ContactInfo ci : list) {
            if (ci.isOnhike() && !OneToNConversationUtils.isOneToNConversation(ci.getMsisdn()) && !BotUtils.isBot(ci.getMsisdn()))
                activeChats.add(ci.getMsisdn());
        }
        //Bots
        bots = HikeMessengerApp.hikeBotInfoMap.keySet();




    }
}
