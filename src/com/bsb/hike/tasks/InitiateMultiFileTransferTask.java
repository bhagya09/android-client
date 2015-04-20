package com.bsb.hike.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

public class InitiateMultiFileTransferTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private ArrayList<Pair<String, String>> fileDetails;

	private boolean onHike;

	private int attachementType;

	private ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>();
	
	public InitiateMultiFileTransferTask(Context context, ArrayList<Pair<String, String>> fileDetails, String msisdn, boolean onHike, int attachementType)
	{
		this.context = context.getApplicationContext();
		this.fileDetails = fileDetails;
		this.contactList.add(ContactManager.getInstance().getContact(msisdn, false, !OneToNConversationUtils.isGroupConversation(msisdn)));
		this.onHike = onHike;
		this.attachementType = attachementType;
	}

	public InitiateMultiFileTransferTask(Context context, ArrayList<Pair<String, String>> fileDetails, ContactInfo contactInfo, boolean onHike, int attachementType)
	{
		this.context = context.getApplicationContext();
		this.fileDetails = fileDetails;
		this.contactList.add(contactInfo);
		this.onHike = onHike;
		this.attachementType = attachementType;
	}

	public InitiateMultiFileTransferTask(Context context, ArrayList<Pair<String, String>> fileDetails, List<ContactInfo> msisdnList, int attachementType)
	{
		this.context = context.getApplicationContext();
		this.fileDetails = fileDetails;
		this.contactList.addAll(msisdnList);
		this.attachementType = attachementType;
	}

	public boolean isForSingleMsisdn()
	{
		if (contactList.size() == 1)
		{
			return true;
		}

		return false;
	}

	public ArrayList<ContactInfo> getContactList()
	{
		return contactList;
	}

	@Override
	protected Void doInBackground(Void... params)
	{

		for (Pair<String, String> fileDetail : fileDetails)
		{
			initiateFileTransferFromIntentData(fileDetail.first, fileDetail.second);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_TASK_FINISHED, null);
	}

	private void initiateFileTransferFromIntentData(String filePath, String fileType)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);

		if (Utils.isPicasaUri(filePath))
		{
			FileTransferManager.getInstance(context).uploadFile(Uri.parse(filePath), hikeFileType, contactList, onHike);
		}
		else
		{
			File file = new File(filePath);
			if (file.length() == 0)
			{
				return;
			}
			FileTransferManager.getInstance(context).uploadFile(contactList, file, null, fileType, hikeFileType, false, false, onHike, -1, attachementType);
		}
	}
}
