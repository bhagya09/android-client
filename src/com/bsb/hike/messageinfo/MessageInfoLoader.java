package com.bsb.hike.messageinfo;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Created by ravi on 5/15/16.
 */
public class MessageInfoLoader extends AsyncTaskLoader<MessageInfoLoaderData> {
    MessageInfoDataModel dataModel;
    public MessageInfoLoader(Context context,MessageInfoDataModel datamodel) {
        super(context);
        this.dataModel=datamodel;
    }

    @Override
    public MessageInfoLoaderData loadInBackground() {

        return dataModel.fetchAllParticipantsInfo();
    }
}
