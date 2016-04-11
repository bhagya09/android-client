package com.bsb.hike.backup.impl;

import com.bsb.hike.backup.BackupUtils;
import com.bsb.hike.backup.Prefs;
import com.bsb.hike.backup.iface.BackupableRestorable;
import com.bsb.hike.utils.CBCEncryption;

import java.io.File;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class PrefBackupRestore implements BackupableRestorable {

    private String backupToken;

    Prefs prefBackup;

    public PrefBackupRestore(String backupToken)
    {
        this.backupToken = backupToken;
        prefBackup = new Prefs();
    }

    @Override
    public boolean preBackupSetup() throws Exception {
        return true;
    }

    @Override
    public void backup() throws Exception {
        String prefBackupString = prefBackup.takeBackup().serialize();
        File prefFile = prefBackup.getPrefFile();
        BackupUtils.writeToFile(prefBackupString, prefFile);
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = BackupUtils.getTempFile(prefFileBackup);
        CBCEncryption.encryptFile(prefFile, prefFileBackupTemp , backupToken);
        prefFile.delete();

    }

    @Override
    public void postBackupSetup() throws Exception {
        File prefFile = Prefs.getPrefFile();
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = BackupUtils.getTempFile(prefFileBackup);
        prefFileBackupTemp.renameTo(prefFileBackup);
    }

    @Override
    public boolean preRestoreSetup() throws Exception {
        Prefs prefBackup = new Prefs();
        File prefFile = prefBackup.getPrefFile();
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        if (prefFileBackup == null || !prefFileBackup.exists())
            return false;
        return true;
    }

    @Override
    public void restore() throws Exception {
        File prefFile = prefBackup.getPrefFile();
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        CBCEncryption.decryptFile(prefFileBackup, prefFile, backupToken);
        String prefBackupString = BackupUtils.readStringFromFile(prefFile);
        prefBackup.deserialize(prefBackupString);
        prefFile.delete();

    }

    @Override
    public void postRestoreSetup() throws Exception {
        prefBackup.restore();
    }

    @Override
    public void finish() {
        deleteTemporaryCopies();
    }

    @Override
    public void selfDestruct() {
        deleteAllFiles();
    }

    private static void deleteTemporaryCopies()
    {
        File prefFile = Prefs.getPrefFile();
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = BackupUtils.getTempFile(prefFileBackup);
        if (prefFile != null)
            prefFile.delete();
        if (prefFileBackupTemp != null)
            prefFileBackupTemp.delete();
    }

    private void deleteAllFiles()
    {
        deleteTemporaryCopies();
        File prefFile = Prefs.getPrefFile();
        File prefFileBackup = BackupUtils.getBackupFile(prefFile.getName());
        if (prefFileBackup != null)
            prefFileBackup.delete();
    }
}
