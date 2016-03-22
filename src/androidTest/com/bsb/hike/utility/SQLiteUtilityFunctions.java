package androidTest.com.bsb.hike.utility;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by surbhisharma on 08/03/16.
 */
public class SQLiteUtilityFunctions {

    /**
     * Currently we compare the following
     * Are all tables in backupFolder present in current DB
     * Do all the tables have the same number of rows
     */


    public static boolean compareCurrentDBandBackup(SQLiteDatabase db,File backupFolder,ArrayList<String> filesToCompare)
    {
        Map<String,Integer> dumpDatabaseInfo = new HashMap<String,Integer>();
        Map<String,Integer> appDatabaseInfo = new HashMap<String,Integer>();

        SQLiteDatabase db1 = SQLiteDatabase.openOrCreateDatabase("/storage/emulated/0/UT_Data/backup_Restore/good/chats",null,null);

        /*
        //Read DumpFiles and create the dump Map
        for(String dbDump :filesToCompare) {

            Connection conn = null;
            Statement stmt = null;

            try {

                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:/storage/emulated/0/UT_Data/backup_Restore/good/chats");
                stmt = conn.createStatement();
                ResultSet tableNameInBackup = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
                while(tableNameInBackup.next())
                {
                    String currentTableName = tableNameInBackup.getString(0);
                    int currenttableSize = 0;
                    ResultSet tableRows = stmt.executeQuery("SELECT count(*) FROM "+currentTableName);
                    while(tableRows.next())
                    {
                        currenttableSize = tableRows.getInt(0);
                    }
                    dumpDatabaseInfo.put(currentTableName,currenttableSize);
                }

            } catch (Exception e) {
                System.out.println(e.getStackTrace());
                System.out.println(e.getCause());
                System.out.println(e.getClass().getName() + ": " + e.getMessage());

            }
            System.out.println("Opened database successfully");
        }
        **/
try {
    Cursor cs1 = db1.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
    while (cs1.moveToNext()) {
        String currentTableName = cs1.getString(0);
        int currenttableSize = 0;
        Cursor tableRows = db1.rawQuery("SELECT count(*) FROM " + currentTableName, null);
        while (tableRows.moveToNext()) {
            currenttableSize = tableRows.getInt(0);
            dumpDatabaseInfo.put(currentTableName, currenttableSize);
        }

    }

    //Populate appDatabase info
    Cursor cs = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
    while (cs.moveToNext()) {
        String currentTableName = cs.getString(0);
        int currenttableSize = 0;
        Cursor tableRows = db.rawQuery("SELECT count(*) FROM " + currentTableName, null);
        while (tableRows.moveToNext()) {
            currenttableSize = tableRows.getInt(0);
            appDatabaseInfo.put(currentTableName, currenttableSize);
        }

    }
}
catch(Exception ex)
        {
            Log.d("DEBUG",ex.getStackTrace().toString());
        }


        return dumpDatabaseInfo.equals(appDatabaseInfo);


    }
}
