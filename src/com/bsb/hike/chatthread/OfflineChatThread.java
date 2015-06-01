package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class OfflineChatThread extends OneToOneChatThread implements IOfflineCallbacks
{

	OfflineController controller;
	
	private final String TAG="OfflineManager";
	
	public OfflineChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
		controller=new OfflineController(this);
	}

	
	@Override
	public void onCreate()
	{
		super.onCreate();
		checkIfSharingFiles(activity.getIntent());
	}
	@Override
	protected void onStart()
	{
		super.onStart();
		//Check if we are connected to the User
	}
	
	@Override
	protected Conversation fetchConversation()
	{
		return super.fetchConversation();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		if (mConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
			return super.onCreateOptionsMenu(menu);
		}
		return false;
	}
	
	private List<OverFlowMenuItem> getOverFlowItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.view_profile), 0, 0, R.string.view_profile));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme), 0, 0, R.string.chat_theme));
		list.add(new OverFlowMenuItem(mConversation.isBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		return list;
	}
	
	@Override
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			convMessage.setIsOfflineMessage(true);
			addtoMessageMap(convMessage);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);

			controller.sendMessage(convMessage);
		}
	}

	@Override
	protected void sendMessage()
	{
		ConvMessage convMessage = createConvMessageFromCompose();
		sendMessage(convMessage);
	}

	@Override
	public void wifiP2PScanResults(WifiP2pDeviceList peerList)
	{
		
	}

	@Override
	public void wifiScanResults(HashMap<String, ScanResult> list)
	{
		
	}

	@Override
	public void onConnect()
	{
		
	}

	@Override
	public void onDisconnect()
	{
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.attachment:
			showOfflineAttchmentPicker();
			activity.showProductPopup(ProductPopupsConstants.PopupTriggerPoints.ATCH_SCR.ordinal());
			return true;
		}
		return false;
	}
	
	protected void showOfflineAttchmentPicker()
	{
		/**
		 * We can now dismiss the Attachment tip if it is there or we hide any other visible tip
		 */
		if (mTips.isGivenTipShowing(ChatThreadTips.ATOMIC_ATTACHMENT_TIP))
		{
			mTips.setTipSeen(ChatThreadTips.ATOMIC_ATTACHMENT_TIP);
		}
		else
		{
			mTips.hideTip();
		}

		initOfflineAttachmentPicker(mConversation.isOnHike());
		int width = (int) (Utils.scaledDensityMultiplier * 270);
		int xOffset = -(int) (276 * Utils.scaledDensityMultiplier);
		int yOffset = -(int) (0.5 * Utils.scaledDensityMultiplier);
		attachmentPicker.show(width, LayoutParams.WRAP_CONTENT, xOffset, yOffset, activity.findViewById(R.id.attachment_anchor), PopupWindow.INPUT_METHOD_NOT_NEEDED);
		
	}
	
	protected void initOfflineAttachmentPicker(boolean addContact)
	{
		if (attachmentPicker == null)
		{
			attachmentPicker = new AttachmentPicker(msisdn, this, this, activity, true);
			if (addContact)
			{
				attachmentPicker.appendItem(new OverFlowMenuItem(getString(R.string.contact), 0, R.drawable.ic_attach_contact, AttachmentPicker.CONTACT));
			}
			attachmentPicker.removeItem(AttachmentPicker.LOCATOIN);
			attachmentPicker.appendItem(new OverFlowMenuItem(getString(R.string.apps), 0, R.drawable.ic_attach_apk, AttachmentPicker.APPS));
			
		}
	}
	
	
	protected void startHikeGallery(boolean onHike)
	{
		Intent imageIntent = IntentFactory.getHikeGallaryShare(activity.getApplicationContext(), msisdn, onHike);
		imageIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
		imageIntent.putExtra(HikeConstants.Extras.OFFLINE_MODE_ON, true);
		activity.startActivityForResult(imageIntent, AttachmentPicker.GALLERY);
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case AttachmentPicker.GALLERY:
			startHikeGallery(mConversation.isOnHike());
			break;

		default:
			super.itemClicked(item);
		}
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Logger.i(TAG, "on activity result " + requestCode + " result " + resultCode);
		switch (requestCode)
		{
			case  AttachmentPicker.APPS:
					String filePath = data.getStringExtra(HikeConstants.Extras.EXTRAS_APK_PATH);
					String mime  =  data.getStringExtra(HikeConstants.Extras.FILE_TYPE);
					String apkLabel  = data.getStringExtra(HikeConstants.Extras.EXTRAS_APK_NAME);
					controller.sendApps(filePath,mime,apkLabel,msisdn);
				break;
			case AttachmentPicker.FILE:
				controller.sendFile(data,msisdn);
				break;	
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}	
	
	
	@Override
	protected void blockUnBlockUser(boolean isBlocked) {
		/*
		 * If offline mode is on, then disconnect the user if the blocked user's
		 * msisdn is that of the current chat thread
		 */
//		if (isOfflineModeOn && isBlocked)
//		{
//			OfflineManager.getInstance().disconnect();
//			Intent homeIntent = IntentFactory.getHomeActivityIntent(activity);
//			activity.startActivity(homeIntent);
//		}
		
		super.blockUnBlockUser(isBlocked);
	}
	
	@Override
	protected void onStop()
	{
		//Initialate a timer of x seconds to save battery.
		super.onStop();
	}
	
	@Override
	public void positiveClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
		case HikeDialogFactory.CONTACT_SEND_DIALOG:

			break;
		default:
			super.positiveClicked(dialog);
		}
	}
	
	@Override
	public void imageCaptured(String imagePath)
	{
		controller.sendImage(imagePath,msisdn);
	}
	
	@Override
	public void pickFileSuccess(int requestCode, String filePath)
	{
		switch (requestCode)
		{
		case AttachmentPicker.AUDIO:

			controller.sendAudio(filePath, msisdn);

			break;
		case AttachmentPicker.VIDEO:

			controller.sendVideo(filePath, msisdn);

			break;
		default:
			super.pickFileSuccess(requestCode, filePath);
		}
	}
	
	@Override
	public void audioRecordSuccess(String filePath, long duration)
	{		
		controller.sendAudioFile(filePath,duration,msisdn);		
	}
	
	private void checkIfSharingFiles(Intent intent) {
		if (intent.hasExtra(HikeConstants.Extras.OFFLINE_SHARING_INTENT))
		{
			//We are coming from the sharing option.
			//OfflineManager.getInstance().forwardSharingFiles(intent);
		}
	}
}