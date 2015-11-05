package com.bsb.hike.tasks;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.HikeConverter;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.ui.ComposeChatActivity.FileTransferData;
import com.bsb.hike.utils.Utils;

public class InitiateMultiFileTransferTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private ArrayList<Pair<String, String>> fileDetails;

	private String msisdn;

	private boolean onHike;

	private int attachementType;
	
	private Intent intent;

	public InitiateMultiFileTransferTask(Context context, ArrayList<Pair<String, String>> fileDetails, String msisdn, boolean onHike, 
											int attachementType, Intent intent)
	{
		this.context = context.getApplicationContext();
		this.fileDetails = fileDetails;
		this.msisdn = msisdn;
		this.onHike = onHike;
		this.attachementType = attachementType;
		this.intent = intent;
	}

	public String getMsisdn()
	{
		return msisdn;
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
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_TASK_FINISHED, this.intent);
	}

	private void initiateFileTransferFromIntentData(String filePath, String fileType, String caption)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);
		
		if (OfflineUtils.isConnectedToSameMsisdn(msisdn))
		{
			File file = new File(filePath);
			if (file.length() == 0)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_7_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "InitiateFileTransferFromIntentData - File length is 0.");
				return;
			}
			ArrayList<ContactInfo> list = new ArrayList<ContactInfo>();
			list.add(ContactManager.getInstance().getContact(msisdn));
			FileTransferData fileTransferData = new FileTransferData(filePath, null, hikeFileType, fileType, false, -1, false, list, file);
			
			ArrayList<FileTransferData> fileTransferList = new ArrayList<FileTransferData>();
			fileTransferList.add(fileTransferData);
			OfflineController.getInstance().sendFile(fileTransferList, msisdn);
		}
		else
		{

			if (Utils.isPicasaUri(filePath))
			{
				FileTransferManager.getInstance(context).uploadFile(Uri.parse(filePath), hikeFileType, msisdn, onHike);
			}
			else
			{
				File file = new File(filePath);
				if (file.length() == 0)
				{
					return;
				}
				FileTransferManager.getInstance(context).uploadFile(msisdn, file, null, fileType, hikeFileType, false, false, onHike, -1, attachementType, caption);
			}
		}
	}
}
