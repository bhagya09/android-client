package androidTest.com.bsb.hike.tests.db;

import android.support.test.runner.AndroidJUnit4;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import androidTest.com.bsb.hike.framework.parameterizationWrappers.*;
import androidTest.com.bsb.hike.mocks.*;
import com.bsb.hike.HikeMessengerApp;
import junit.framework.Assert;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;
import java.io.File;
import android.database.sqlite.SQLiteDatabase;
import com.bsb.hike.backup.AccountBackupRestore;
import javax.xml.parsers.ParserConfigurationException;
import androidTest.com.bsb.hike.utility.IO;
import androidTest.com.bsb.hike.utility.SQLiteUtilityFunctions;

//Tests the backup feature
//@RunWith(AndroidJUnit4.class)
@RunWith(Parameterized.class)
public class BackupAndRestoreInstrumentedTest {
    String path;
    String tocompare;
    boolean expectedRestoreOutput = false;
    String testCaseName;
    static IParameterizationWrapper wrapper;
    @Before
    public void pushfiles()
    {
        //stub push test adta to device
    }
    public BackupAndRestoreInstrumentedTest(String testCaseName,String path, String tocompare, String pexpectedRestoreOutput)
    {
        this.testCaseName = testCaseName;
        this.path = path;
        this.tocompare = tocompare;        if (pexpectedRestoreOutput.equalsIgnoreCase("true"))
            this.expectedRestoreOutput = true;
    }
    @Parameterized.Parameters(name = "{0},Expected Output{3}")
    public static Iterable<Object[]> data1() throws ParserConfigurationException, SAXException, IOException {
        wrapper = new XMLWrapper("/storage/emulated/0/UT_Data/parameters/BackupRestoreTest.xml");
        return wrapper.getParameters(new File("/storage/emulated/0/UT_Data/parameters/BackupRestoreTest.xml"));
    }

    @Test
    public void testRestore() throws IOException{
        String testDataBackupFolder = path;
        ArrayList<String> filesToCompare = new ArrayList<String>();
        filesToCompare.add(tocompare);
        AccountBackupRestore objAccountBackupRestore = null;
        IO.copyFolder(new File(testDataBackupFolder), new File(HikeConstants.HIKE_BACKUP_DIRECTORY_ROOT));

        try {
            objAccountBackupRestore = TestInstanceFactory.getAccountBackupRestoreInstance(HikeMessengerApp.getInstance().getApplicationContext());
        } catch (Exception e) {

        }
        if (expectedRestoreOutput)
        objAccountBackupRestore.restore();
        else
        {
            if(testCaseName.contains("MSISDN"))
            Assert.assertEquals(AccountBackupRestore.STATE_RESTORE_FAILURE_MSISDN_MISMATCH,objAccountBackupRestore.restore());
            else
                Assert.assertEquals(AccountBackupRestore.STATE_RESTORE_FAILURE_INCOMPATIBLE_VERSION,objAccountBackupRestore.restore());
            return;
        }

        //Gets the restored DB
        SQLiteDatabase objDB = HikeConversationsDatabase.getInstance().getWritableDatabase();

        //Compares the restored db to the db dump for all db's in filesToCompare
        Assert.assertTrue(SQLiteUtilityFunctions.compareCurrentDBandBackup(objDB, new File(testDataBackupFolder), filesToCompare));

    }



}
