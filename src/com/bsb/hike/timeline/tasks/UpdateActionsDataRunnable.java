package com.bsb.hike.timeline.tasks;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.ActionsDeserializer;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by atul on 08/06/16.
 */
public class UpdateActionsDataRunnable implements Runnable {

    private final int ACTIONS_UPDATE_STATUS_LIMIT = 50;

    List<StatusMessage> statusMessageList;

    private Gson gson;

    public UpdateActionsDataRunnable(List<StatusMessage> argStatusMessageList) {
        statusMessageList = argStatusMessageList;
    }

    @Override
    public void run() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(TimelineActions.class, new ActionsDeserializer());
        gson = gsonBuilder.create();

        if (Utils.isEmpty(statusMessageList)) {
            statusMessageList = HikeConversationsDatabase.getInstance().getStatusMessages(false, ACTIONS_UPDATE_STATUS_LIMIT, new int[]{
                    StatusMessage.StatusMessageType.TEXT_IMAGE.ordinal(),
                    StatusMessage.StatusMessageType.IMAGE.ordinal(),
                    StatusMessage.StatusMessageType.PROFILE_PIC.ordinal()});
        }

        if (!Utils.isEmpty(statusMessageList)) {
            List<String> uuidList = new ArrayList<String>();
            for (StatusMessage suMsg : statusMessageList) {
                uuidList.add(suMsg.getMappedId());
            }

            // Get actions for SU from HTTP
            JSONArray suIDArray = new JSONArray(uuidList);
            JSONObject suUpdateJSON = new JSONObject();
            try {
                Logger.d(HikeConstants.TIMELINE_LOGS, "list of suIDArray, fetching HTTP calls " + suIDArray);
                suUpdateJSON.put(HikeConstants.SU_ID_LIST, suIDArray);
                RequestToken requestToken = HttpRequests.getActionUpdates(suUpdateJSON, actionUpdatesReqListener);
                requestToken.execute();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), uuidList,
                    TimelineActionsManager.getInstance().getActionsData());

            HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.ACTIONS_DATA_UPDATE, null);
        }
    }

    private IRequestListener actionUpdatesReqListener = new IRequestListener() {
        @Override
        public void onRequestSuccess(Response result) {
            final JSONObject response = (JSONObject) result.getBody().getContent();

            Logger.d(HikeConstants.TAG, "responce from http call " + response);

            if (Utils.isResponseValid(response)) {
                TimelineActions actionsData = gson.fromJson(response.toString(), TimelineActions.class);

                if (actionsData == null) {
                    return;
                }

                TimelineActionsManager.getInstance().updateActionsData(actionsData);

                HikeConversationsDatabase.getInstance().updateActionsData(actionsData, ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE);

                HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.ACTIONS_DATA_UPDATE, null);
            }
        }

        @Override
        public void onRequestProgressUpdate(float progress) {
            // Do nothing
        }

        @Override
        public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException) {
            // Do nothing
            Logger.d(HikeConstants.TAG, "responce from http call failed " + httpException.toString());
        }
    };
}
