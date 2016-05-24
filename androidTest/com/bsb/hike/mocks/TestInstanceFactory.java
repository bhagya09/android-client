package androidTest.com.bsb.hike.mocks;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.AccountBackupRestore;

/**
 * Created by surbhisharma on 09/03/16.
 * Factory for all test
 */
public class TestInstanceFactory {
    static AccountBackupRestore objAccountBackupRestore;


    public static AccountBackupRestore getAccountBackupRestoreInstance(Context context)
    {
        if (objAccountBackupRestore == null)
            synchronized (AccountBackupRestore.class){
                if (objAccountBackupRestore == null)
                    objAccountBackupRestore = AccountBackupRestore.getInstance(context.getApplicationContext());
    }

        return objAccountBackupRestore;
    }

}
