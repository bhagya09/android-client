package com.bsb.hike.tests.db;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.model.BackupMetadata;

import junit.framework.Assert;

import org.junit.Test;



//@RunWith(AndroidJUnit4.class)
public class BackupMetadataTest {


    private String JSON_STRING = "{\"version\":1611,\"msisdn\":\"+9187654321\",\"ts\":1457360725610}";

    @Test
    public void BackupMetadataTest() {


        Context context = HikeMessengerApp.getInstance().getApplicationContext();

        //Assert(new BackupMetadata(context, JSON_STRING).toString(), JSON_STRING);
    }
}
