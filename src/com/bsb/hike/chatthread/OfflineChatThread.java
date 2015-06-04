package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OfflineConversation;
import com.bsb.hike.models.Conversation.OneToOneConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author himanshu
 *
 *	This chat Chat deals with Offline Messages.
 */
public class OfflineChatThread extends OneToOneChatThread implements IOfflineCallbacks
{

	private static final int UPDATE_CONNECTION_STATUS = 401;

	OfflineController controller;
	
	private final String TAG="OfflineManager";
	
	OfflineConversation mConversation;
	
	public OfflineChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
		controller=new OfflineController(this);
	}

	
	@Override
	protected void handleUIMessage(Message msg)
	{
		switch(msg.what)
		{
		case UPDATE_CONNECTION_STATUS:
			updateStatus((String)msg.obj);
			break;
		}
		super.handleUIMessage(msg);
	}
	
	
	
	private void updateStatus(String status)
	{
		final TextView statusView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		if (TextUtils.isEmpty(status))
		{
			return;
		}
		statusView.setVisibility(View.VISIBLE);
		statusView.setText(status);
	}


	@Override
	public void onCreate()
	{
		super.onCreate();
		checkIfSharingFiles(activity.getIntent());
		checkIfWeNeedToConnect(activity.getIntent());
	}
	
	private void checkIfWeNeedToConnect(Intent intent)
	{
		if(intent.hasExtra(OfflineConstants.START_CONNECT_FUNCTION))
		{
			connectClicked();
		}
	}

	@Override
	public void onNewIntent()
	{
		super.onNewIntent();
		checkIfWeNeedToConnect(activity.getIntent());
	}

	

	@Override
	protected void onStart()
	{
		super.onStart();
		//Check if we are connected to the User
	}
	
	@Override
	protected String getConvLabel()
	{
		return mConversation.getConversationName();
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
	}
	protected Conversation fetchConversation()
	{
		mConversation = (OfflineConversation)mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, false);

		if (mConversation == null)
		{
			mConversation = new OfflineConversation.ConversationBuilder(msisdn).setIsOnHike(true).build();
			mConversation.setMessages(HikeConversationsDatabase.getInstance().getConversationThread(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, mConversation, -1));
		}
		mContactInfo = ContactManager.getInstance().getContact(mConversation.getDisplayMsisdn(), true, true);
		mConversation.setConversationName( mContactInfo==null ? mConversation.getDisplayMsisdn():  mContactInfo.getName());
		ChatTheme chatTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d(TAG, "Calling setchattheme from createConversation");
		mConversation.setChatTheme(chatTheme);

		mConversation.setBlocked(ContactManager.getInstance().isBlocked(mConversation.getDisplayMsisdn()));

		return mConversation;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		if (mConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
			menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(this);
			mActionBar.setOverflowViewListener(this);
		}

		return false;
	}
	
	private List<OverFlowMenuItem> getOverFlowItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.connect_offline), 0, 0, R.string.connect_offline));
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
		sendUpdateStatusMessageOnHandler(R.string.connection_established);
		
		ConvMessage convMessage=OfflineUtils.createOfflineInlineConvMessage(msisdn,activity.getString(R.string.connection_established),OfflineConstants.OFFLINE_MESSAGE_CONNECTED_TYPE);
		
		addMessage(convMessage);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
	}
	
	private void sendUpdateStatusMessageOnHandler(int id)
	{
		Message msg=Message.obtain();
		msg.what=UPDATE_CONNECTION_STATUS;
		msg.obj=getString(id);
		uiHandler.sendMessage(msg);
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
	
	
//	protected void startHikeGallery(boolean onHike)
//	{
//		Intent imageIntent = IntentFactory.getHikeGallaryShare(activity.getApplicationContext(), msisdn, onHike);
//		imageIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
//		imageIntent.putExtra(HikeConstants.Extras.OFFLINE_MODE_ON, true);
//		activity.startActivityForResult(imageIntent, AttachmentPicker.GALLERY);
//	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.connect_offline:
			connectClicked();
			onConnect();
			break;
		default:
			super.itemClicked(item);
		}
	}
	
	public void connectClicked()
	{
		Toast.makeText(activity, "Start the Scan Process here ", Toast.LENGTH_SHORT).show();
		controller.connectAsPerMsisdn(mConversation.getDisplayMsisdn());
		sendUpdateStatusMessageOnHandler(R.string.awaiting_response);
	}

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Logger.i(TAG, "on activity result " + requestCode + " result " + resultCode);
		switch (requestCode)
		{
			case  AttachmentPicker.APPS:
					String filePath = data.getStringExtra(OfflineConstants.EXTRAS_APK_PATH);
					String mime  =  data.getStringExtra(HikeConstants.Extras.FILE_TYPE);
					String apkLabel  = data.getStringExtra(OfflineConstants.EXTRAS_APK_NAME);
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
	public void imageParsed(String imagePath)
	{
		controller.sendImage(imagePath,msisdn);
	}
	
	@Override
	public void imageParsed(Uri uri)
	{
		controller.sendImage(Utils.getRealPathFromUri(uri,activity.getApplicationContext()),msisdn);
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
//		if (intent.hasExtra(HikeConstants.Extras.OFFLINE_SHARING_INTENT))
//		{
//			//We are coming from the sharing option.
//			//OfflineManager.getInstance().forwardSharingFiles(intent);
//		}
	}


	@Override
	public void onDisconnect(ERRORCODE errorCode)
	{
		switch (errorCode)
		{
		case OUT_OF_RANGE:
			break;
		case TIMEOUT:
			break;
		case USERDISCONNECTED:
			break;
		default:
			break;
		}
		
	}
}