package com.bsb.hike.tasks;

import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

import java.util.List;
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
        if (Utils.isEmpty(blockedMsisdn)) {
            return true;
        }

        // Move to User Table
        ContactManager.getInstance().block(blockedMsisdn);

        //Drop Block Table
        HikeUserDatabase.getInstance().dropBlockTable();
        Logger.d(TAG,"Migration END");
        return true;

    }
}
