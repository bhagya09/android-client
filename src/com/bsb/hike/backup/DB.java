package com.bsb.hike.backup;

import com.bsb.hike.utils.CBCEncryption;

import java.io.File;

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
        if (BackupUtils.isDBCorrupt(dbName))
            return false;
        return true;
    }

    @Override
    public void backup() throws Exception {
        File dbCopy = BackupUtils.exportDatabse(dbName);
        if (dbCopy == null || !dbCopy.exists())
        {
            throw new Exception("Backup file " + dbCopy + " is missing");
        }
        File backup = BackupUtils.getBackupFile(dbName);
        File backupTemp = BackupUtils.getTempFile(backup);
        CBCEncryption.encryptFile(dbCopy, backupTemp, backupToken);
        dbCopy.delete();

    }

    @Override
    public void postBackupSetup() throws Exception {
        File backup = BackupUtils.getBackupFile(dbName);
        File backupTemp = BackupUtils.getTempFile(backup);
        backupTemp.renameTo(backup);
    }

    @Override
    public boolean preRestoreSetup() throws Exception {
        File currentDB = BackupUtils.getCurrentDBFile(dbName);
        File dbCopy = BackupUtils.getDBCopyFile(currentDB.getName());
        File backup = BackupUtils.getBackupFile(dbCopy.getName());
        if (backup == null || !backup.exists())
            return false;
        return true;
    }

    @Override
    public void restore() throws Exception {
        File currentDB = BackupUtils.getCurrentDBFile(dbName);
        File dbCopy = BackupUtils.getDBCopyFile(currentDB.getName());
        File backup = BackupUtils.getBackupFile(dbCopy.getName());
        CBCEncryption.decryptFile(backup, dbCopy, backupToken);
    }

    @Override
    public void postRestoreSetup() throws Exception {
        File dbCopy = BackupUtils.getDBCopyFile(dbName);
        BackupUtils.importDatabase(dbCopy);
    }

    @Override
    public void finish() {
        deleteTemporaryCopies();
    }

    @Override
    public void selfDestruct() {
        deleteAllFiles();
    }

    private void deleteTemporaryCopies() {
        File dbCopy = BackupUtils.getDBCopyFile(dbName);
        if (dbCopy != null)
            dbCopy.delete();

        File backup = BackupUtils.getBackupFile(dbName);
        File backupTemp = BackupUtils.getTempFile(backup);
        if (backupTemp != null)
            backupTemp.delete();
    }

    private void deleteAllFiles()
    {
        deleteTemporaryCopies();
        File backup = BackupUtils.getBackupFile(dbName);
        if (backup != null)
            backup.delete();
    }

}