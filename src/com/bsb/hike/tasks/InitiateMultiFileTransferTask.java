package com.bsb.hike.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FTMessageBuilder;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.ui.ComposeChatActivity.FileTransferData;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class InitiateMultiFileTransferTask extends AsyncTask<Void, Void, Void>
{
	private Context context;


	private ArrayList<FileTransferData> ftDataList;

	private String msisdn;

	private boolean onHike;

	private int attachementType;

	private Intent intent;

	public InitiateMultiFileTransferTask(Context context, ArrayList<FileTransferData> ftDataList, String msisdn, boolean onHike,
										 int attachementType, Intent intent)
	{
		this.context = context.getApplicationContext();
		this.ftDataList = ftDataList;
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
		for (FileTransferData ftData : ftDataList)
		{
			initiateFileTransferFromIntentData(ftData);
		}
		return null;
	}

	private void initiateFileTransferFromIntentData(FileTransferData fileTransferData)
	{
		String typeOfFileToTransfer = fileTransferData.fileType;
		if(!TextUtils.isEmpty(typeOfFileToTransfer)) {
			typeOfFileToTransfer = typeOfFileToTransfer.toLowerCase();
		}
		HikeFileType hikeFileType = HikeFileType.fromString(typeOfFileToTransfer, false);

		if (OfflineUtils.isConnectedToSameMsisdn(msisdn))
		{
			File file = new File(fileTransferData.filePath);
			if (file.length() == 0)
			{
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_7_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "InitiateFileTransferFromIntentData - File length is 0.");
				return;
			}

			ArrayList<FileTransferData> fileTransferList = new ArrayList<FileTransferData>();
			fileTransferList.add(fileTransferData);
			OfflineController.getInstance().sendFile(fileTransferList, msisdn);
		}
		else
		{
			Logger.d("InitiateMultiFileTransferTask", "isCloudMedia" + Utils.isPicasaUri(fileTransferData.filePath));
			File file = new File(fileTransferData.filePath);
			if (file.length() == 0)
			{
				HikeMessengerApp.getInstance().showToast(R.string.file_size_invalid_error, Toast.LENGTH_SHORT);
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_7_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "InitiateFileTransferFromIntentData - File length is 0.");
				return;
			}
			FTMessageBuilder.Builder mBuilder = new FTMessageBuilder.Builder()
					.setMsisdn(msisdn)
					.setSourceFile(file)
					.setFileKey(null)
					.setFileType(fileTransferData.fileType)
					.setHikeFileType(hikeFileType)
					.setRec(false)
					.setForwardMsg(false)
					.setRecipientOnHike(onHike)
					.setRecordingDuration(-1)
					.setAttachement(attachementType)
					.setCaption(fileTransferData.caption);
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