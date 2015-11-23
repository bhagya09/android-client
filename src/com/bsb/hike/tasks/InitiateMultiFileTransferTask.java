package com.bsb.hike.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FTMessageBuilder;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.HikeConverter;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.ui.ComposeChatActivity.FileTransferData;
import com.bsb.hike.utils.Logger;
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
	protected void onPostExecute(Void result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_FILE_TASK_FINISHED, this.intent);
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

	private void initiateFileTransferFromIntentData(String filePath, String fileType)
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
			Logger.d("InitiateMultiFileTransferTask", "isCloudMedia" + Utils.isPicasaUri(filePath));
				File file = new File(filePath);
				if (file.length() == 0)
				{
					return;
				}
				FTMessageBuilder.Builder mBuilder = new FTMessageBuilder.Builder()
				.setMsisdn(msisdn)
				.setSourceFile(file)
				.setFileKey(null)
				.setFileType(fileType)
				.setHikeFileType(hikeFileType)
				.setRec(false)
				.setForwardMsg(false)
				.setRecipientOnHike(onHike)
				.setRecordingDuration(-1)
				.setAttachement(attachementType);
				List<ConvMessage> ftConvMsgs = mBuilder.buildInSync();
				Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
				for (Iterator<ConvMessage> iterator = ftConvMsgs.iterator(); iterator.hasNext();)
				{
					ConvMessage convMessage = (ConvMessage) iterator.next();
					if(hikeFileType == HikeFileType.CONTACT || hikeFileType == HikeFileType.LOCATION)
					{
						FileTransferManager.getInstance(mContext).uploadContactOrLocation(convMessage, hikeFileType == HikeFileType.CONTACT);
					}
					else
					{
						FileTransferManager.getInstance(mContext).uploadFile(convMessage, null);
					}
				}
		}
	}
}
