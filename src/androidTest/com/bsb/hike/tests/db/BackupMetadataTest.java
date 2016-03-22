package androidTest.com.bsb.hike.tests.db;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

//import com.android.test.runner.MultiDexTestRunner;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.BackupMetadata;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


//@RunWith(AndroidJUnit4.class)
public class BackupMetadataTest {

//    @Mock
//    Context mMockContext;
//
//    @Mock
//    PackageManager mMockPm;
//
//    @Mock
//    PackageInfo mMockPackageInfo;
//
//    @Mock
//    JSONObject mMockJsonObject;

    private String JSON_STRING = "{\"version\":1611,\"msisdn\":\"+9187654321\",\"ts\":1457360725610}";

//    @Test
//    public void foo() {
//        assertThat(true, is(true));
//    }

    @Test
    public void BackupMetadataTest() {

//        when(mMockContext.getPackageManager()).thenReturn(mMockPm);
//        when(mMockPm.getPackageInfo("com.bsb.hike", 0)).thenReturn(mMockPackageInfo);
//        when(mMockContext.getPackageName()).thenReturn("com.bsb.hike");
//
//        when(mMockJsonObject.)

        Context context = HikeMessengerApp.getInstance().getApplicationContext();

        assertThat(new BackupMetadata(context, JSON_STRING).toString().equals(JSON_STRING), is(true));
    }
}
