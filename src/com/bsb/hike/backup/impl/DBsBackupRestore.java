package com.bsb.hike.backup.impl;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.backup.iface.BackupableRestorable;
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
		chatsDB = new DB(DBConstants.CONVERSATIONS_DATABASE_NAME,
				// STICKER_SHOP_TABLE and STICKER_CATEGORIES_TABLE will be skipped
				new String[] { DBConstants.STICKER_SHOP_TABLE, DBConstants.STICKER_CATEGORIES_TABLE,DBConstants.STICKER_TABLE },
				backupToken)
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

                /**
                 * Resetting upgrade pref for the excluded tables
                 * These prefs insure that upgrade intent actions are taken for these tables
                 *
                 */


                HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1);
                HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_FOR_STICKER_TABLE, 1);
                HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.UPGRADE_STICKER_CATEGORIES_TABLE, false);
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
				add(usersDB);
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
}
