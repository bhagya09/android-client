package com.bsb.hike.backup.impl;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.backup.BackupUtils;
import com.bsb.hike.backup.iface.BackupableRestorable;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.backup.model.BackupMetadata;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.StickerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class DBsBackupRestore implements BackupableRestorable
{

	private String backupToken;

	private List<DB> DBs;

	private DB chatsDB;

	private DB usersDB;

	public DBsBackupRestore(String backupToken)
	{
		this.backupToken = backupToken;
		init();
	}

	private void init()
	{
		chatsDB = new DB(DBConstants.CONVERSATIONS_DATABASE_NAME, new String[] {}, backupToken)
		{
			@Override
			public void postRestoreSetup() throws Exception
			{
				super.postRestoreSetup();
				HikeConversationsDatabase.getInstance().reinitializeDB();
				for (String table : tablesToReset)
				{
					HikeConversationsDatabase.getInstance().clearTable(table);
				}

                StickerManager.getInstance().postRestoreSetup();
				ChatThemeManager.getInstance().postRestoreSetup();
				postRestoreMuteSetup();
			}
		};

		usersDB = new DB(DBConstants.USERS_DATABASE_NAME,
				// USERS_TABLE, BLOCK_TABLE and FAVORITES_TABLE will be skipped
				new String[] { DBConstants.USERS_TABLE, DBConstants.BLOCK_TABLE, DBConstants.FAVORITES_TABLE },
				backupToken,
				true)
		{
			@Override
			public void postRestoreSetup() throws Exception {
				super.postRestoreSetup();
				ContactManager.getInstance().reinitializeUserDB();
				for (String table : tablesToReset)
				{
					ContactManager.getInstance().clearUserDbTable(table);
				}
			}
		};

		DBs = new ArrayList<DB>()
		{
			{

				// Adding Chats DB
				add(chatsDB);

				// Adding timeline DB
				//add(usersDB);
			}
		};
	}

	@Override
	public boolean preBackupSetup() throws Exception
	{

		for (DB db : DBs)
		{
			if (!db.preBackupSetup())
				return false;
		}
		return true;
	}

	@Override
	public void backup() throws Exception
	{
		for (DB db : DBs)
			db.backup();
	}

	@Override
	public void postBackupSetup() throws Exception
	{
		for (DB db : DBs)
			db.postBackupSetup();
	}

	@Override
	public boolean preRestoreSetup() throws Exception
	{
		for (DB db : DBs)
		{
			if (!db.preRestoreSetup())
				return false;
		}
		return true;
	}

	@Override
	public void restore() throws Exception
	{
		for (DB db : DBs)
			db.restore();
	}

	@Override
	public void postRestoreSetup() throws Exception
	{
		for (DB db : DBs)
		{
			db.postRestoreSetup();
		}

		IntentFactory.startUpgradeIntent(HikeMessengerApp.getInstance().getApplicationContext());
	}

	@Override
	public void finish()
	{
		for (DB db : DBs)
			db.finish();
	}

	@Override
	public void selfDestruct() {
		for (DB db : DBs)
			db.selfDestruct();
	}

	public void postRestoreMuteSetup() {

		BackupMetadata metadata = BackupUtils.getBackupMetadata();
		if (metadata != null) {
			int oldBackupVersion = metadata.getAppVersion();

			if (oldBackupVersion <= AccountBackupRestore.MUTE_BACKUP_THRESHOLD_VERSION) {
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPGRADE_FOR_CHAT_PROPERTIES, 0);
			}
		}
	}
}
