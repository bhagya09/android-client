package com.bsb.hike.backup;

import com.bsb.hike.utils.CBCEncryption;

import java.io.File;

import com.bsb.hike.backup.Utils;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class DB implements BackupableRestorable {

    private String backupToken;

    private String dbName;

    protected String[] tablesToReset;

    /*
    @param name: name of the db
    @param tableList: list of tables to skip restore.
     */
    DB(String name, String[] tableList, String backupToken)
    {
        this.dbName = name;
        this.tablesToReset = tableList;
        this.backupToken = backupToken;
    }

    @Override
    public boolean preBackupSetup() throws Exception {
        if (Utils.isDBCorrupt(dbName))
            return false;
        return true;
    }

    @Override
    public void backup() throws Exception {
        File dbCopy = Utils.exportDatabse(dbName);
        if (dbCopy == null || !dbCopy.exists())
        {
            throw new Exception("Backup file " + dbCopy + " is missing");
        }
        File backup = Utils.getBackupFile(dbName);
        File backupTemp = Utils.getTempFile(backup);
        CBCEncryption.encryptFile(dbCopy, backupTemp, backupToken);
        dbCopy.delete();

    }

    @Override
    public void postBackupSetup() throws Exception {
        File backup = Utils.getBackupFile(dbName);
        File backupTemp = Utils.getTempFile(backup);
        backupTemp.renameTo(backup);
    }

    @Override
    public boolean preRestoreSetup() throws Exception {
        return true;
    }

    @Override
    public void restore() throws Exception {
        File currentDB = Utils.getCurrentDBFile(dbName);
        File dbCopy = Utils.getDBCopyFile(currentDB.getName());
        File backup = Utils.getBackupFile(dbCopy.getName());
        CBCEncryption.decryptFile(backup, dbCopy, backupToken);
    }

    @Override
    public void postRestoreSetup() throws Exception {
        File dbCopy = Utils.getDBCopyFile(dbName);
        Utils.importDatabase(dbCopy);
    }

    @Override
    public void finish() {
        deleteTemporaryCopies();
    }

    private void deleteTemporaryCopies() {
        File dbCopy = Utils.getDBCopyFile(dbName);
        if (dbCopy != null)
            dbCopy.delete();

        File backup = Utils.getBackupFile(dbName);
        File backupTemp = Utils.getTempFile(backup);
        if (backupTemp != null)
            backupTemp.delete();
    }

}