package com.bsb.hike.backup;

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
        Utils.writeToFile(prefBackupString, prefFile);
        File prefFileBackup = Utils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = Utils.getTempFile(prefFileBackup);
        CBCEncryption.encryptFile(prefFile, prefFileBackupTemp , backupToken);
        prefFile.delete();

    }

    @Override
    public void postBackupSetup() throws Exception {
        File prefFile = Prefs.getPrefFile();
        File prefFileBackup = Utils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = Utils.getTempFile(prefFileBackup);
        prefFileBackupTemp.renameTo(prefFileBackup);
    }

    @Override
    public boolean preRestoreSetup() throws Exception {
        Prefs prefBackup = new Prefs();
        File prefFile = prefBackup.getPrefFile();
        File prefFileBackup = Utils.getBackupFile(prefFile.getName());
        if (prefFileBackup == null || !prefFileBackup.exists())
            return false;
        return true;
    }

    @Override
    public void restore() throws Exception {
        File prefFile = prefBackup.getPrefFile();
        File prefFileBackup = Utils.getBackupFile(prefFile.getName());
        CBCEncryption.decryptFile(prefFileBackup, prefFile, backupToken);
        String prefBackupString = Utils.readStringFromFile(prefFile);
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

    private static void deleteTemporaryCopies()
    {
        File prefFile = Prefs.getPrefFile();
        File prefFileBackup = Utils.getBackupFile(prefFile.getName());
        File prefFileBackupTemp = Utils.getTempFile(prefFileBackup);
        prefFile.delete();
        prefFileBackupTemp.delete();
    }
}
