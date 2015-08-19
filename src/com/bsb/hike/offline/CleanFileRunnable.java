package com.bsb.hike.offline;

import java.util.ArrayList;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.hike.transporter.Transporter;

/**
 * 
 * @author himanshu This runnable is responsible for 3 things 1)delete the partial downloaded file. 2)delte the partial file from DB and UI 3)Post the disconnection inLine msg
 */
public class CleanFileRunnable implements Runnable
{

	@Override
	public void run()
	{
		Transporter.getInstance().deleteTempFiles(HikeMessengerApp.getInstance().getApplicationContext());

		long msgId = HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.CURRENT_RECIEVING_MSG_ID, -1l);

		if (msgId != -1)
		{
			ArrayList<Long> msgArrayList = new ArrayList<Long>(1);
			msgArrayList.add(msgId);

			OfflineController.getInstance().deleteRemainingFiles(msgArrayList, HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.OFFLINE_MSISDN, ""));
			}
		OfflineController.getInstance().sendDisconnectInlineMsg(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.OFFLINE_MSISDN, ""));

		HikeSharedPreferenceUtil.getInstance().removeData(OfflineConstants.OFFLINE_MSISDN);
		HikeSharedPreferenceUtil.getInstance().removeData(OfflineConstants.CURRENT_RECIEVING_MSG_ID);
		OfflineAnalytics.recordDisconnectionAnalytics(OfflineException.APP_SWIPE);
		
	}

}
