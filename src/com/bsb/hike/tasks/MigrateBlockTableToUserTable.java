package com.bsb.hike.tasks;

import android.text.TextUtils;

import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by himanshu on 26/04/16.
 */

/**
 * This class will be used to migrate block and favorite table.
 */
public class MigrateBlockTableToUserTable implements Callable<Boolean> {

    private static final String TAG="MigrateTable";

    @Override
    public Boolean call() throws Exception {

        Logger.d(TAG,"Migration Start");
        List<String> blockedMsisdn = HikeUserDatabase.getInstance().getBlockedMsisdnFromBlockTable();
        if (!Utils.isEmpty(blockedMsisdn)) {


            // Move to User Table
            ContactManager.getInstance().block(blockedMsisdn);

            //Drop Block Table
            HikeUserDatabase.getInstance().dropBlockTable();
        }



        // gettong contacts from fav table:
        Map<String, ContactInfo.FavoriteType> favoriteMsisdnFromFavTable = HikeUserDatabase.getInstance().getFavoriteMsisdnFromFavTable();

        // inserting into User Db
        HikeUserDatabase.getInstance().insertFavIntoUserTable(favoriteMsisdnFromFavTable);

        //DropFav Tab
        HikeUserDatabase.getInstance().dropFavTable();


        //Migrating Unknown Contact(Active Chat to UserDb)

        final List<ConvInfo> convInfoObjects = HikeConversationsDatabase.getInstance().getConvInfoObjects();

        for (ConvInfo ci : convInfoObjects) {
            if (ci == null || TextUtils.isEmpty(ci.getMsisdn()) || OneToNConversationUtils.isOneToNConversation(ci.getMsisdn()) || BotUtils.isBot(ci.getMsisdn()) || !ContactManager.getInstance().isUnknownContact(ci.getMsisdn()) || !ci.isOnHike()) {
                continue;
            }
            Logger.d(TAG, "Migrating msisdn to UD" + ci.getMsisdn());
            HikeUserDatabase.getInstance().updateTableWhenNewConversationCreated(ci);
        }

        Logger.d(TAG,"Migration END");
        new FetchHikeUIDTaskForUpgrade().execute();
        return true;



        // TODO:Make a HTTP Call here:to get All the UID of missing MSISDN
    }


}
