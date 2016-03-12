package com.bsb.hike.backup;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.contactmgr.ContactManager;

/**
 * Created by gauravmittal on 10/03/16.
 */
public class DBsBackupRestore implements BackupableRestorable
{

	private String backupToken;

	private List<DB> DBs;

	private DB chatsDB;

	private DB usersDB;

	DBsBackupRestore(String backupToken)
	{
		this.backupToken = backupToken;
		init();
	}

	private void init()
	{
		chatsDB = new DB(DBConstants.CONVERSATIONS_DATABASE_NAME,
				// STICKER_SHOP_TABLE and STICKER_CATEGORIES_TABLE will be skipped
				new String[] { DBConstants.STICKER_SHOP_TABLE, DBConstants.STICKER_CATEGORIES_TABLE },
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
				HikeConversationsDatabase.getInstance().upgradeForStickerShopVersion1();
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
			db.postRestoreSetup();
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
