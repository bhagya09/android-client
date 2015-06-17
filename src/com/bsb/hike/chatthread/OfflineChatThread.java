package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

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
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OfflineConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.media.OverFlowMenuItem;

/**
 * 
 * @author himanshu
 * 
 *         This chat Chat deals with Offline Messages.
 */
public class OfflineChatThread extends OneToOneChatThread implements IOfflineCallbacks
{

	private static final int UPDATE_CONNECTION_STATUS = 401;

	private static final int UPDATE_COMPOSE_VIEW = 402;

	OfflineController controller;

	private final String TAG = "OfflineManager";

	OfflineConversation mConversation;
	
	boolean canPoke=true;

	public OfflineChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
		controller = new OfflineController(this);
	}

	@Override
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_CONNECTION_STATUS:
			updateStatus((String) msg.obj);
			break;
		case UPDATE_COMPOSE_VIEW:
			toggleComposeView((boolean) msg.obj);
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

		if (status.equals(getString(R.string.connection_deestablished)))
		{
			statusView.setVisibility(View.GONE);
		}
		{
			statusView.setVisibility(View.VISIBLE);
			statusView.setText(status);
		}
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
		if (intent.hasExtra(OfflineConstants.START_CONNECT_FUNCTION))
		{
			connectClicked();
		}
	}

	@Override
	public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems)
	{
		if (overflowItems == null)
		{
			return;
		}
		super.onPrepareOverflowOptionsMenu(overflowItems);
		for (OverFlowMenuItem overFlowMenuItem : overflowItems)
		{
			switch (overFlowMenuItem.id)
			{
			case R.string.connect_offline:
				if (TextUtils.isEmpty(controller.getConnectedDevice()) || (!controller.getConnectedDevice().equals(mConversation.getDisplayMsisdn())))
				{
					overFlowMenuItem.text = getString(R.string.connect_offline);
				}
				else
				{
					overFlowMenuItem.text = getString(R.string.disconnect_offline);
				}
				break;
			}
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
		// Check if we are connected to the User

	}

	@Override
	protected void sendPoke()
	{
		if (canPoke)
		{
			super.sendPoke();
		}
	}
	@Override
	protected String getConvLabel()
	{
		return mConversation.getLabel();
	}

	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
		if (TextUtils.isEmpty(controller.getConnectedDevice()) || (!controller.getConnectedDevice().equals(mConversation.getDisplayMsisdn())))
		{
			toggleComposeView(false);
		}
	}

	@Override
	public void onResume()
	{

		super.onResume();
		switch (controller.getOfflineState())
		{
		case CONNECTED:
			if (mConversation.getDisplayMsisdn() == controller.getConnectedDevice())
			{
				updateStatus(getString(R.string.connection_established));
			}
			break;
		case CONNECTING:

			break;
		case NOT_CONNECTED:
			break;
		default:
			break;
		}
	}

	
	
	protected Conversation fetchConversation()
	{
		mConversation = (OfflineConversation) mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, false);

		if (mConversation == null)
		{
			mConversation = new OfflineConversation.ConversationBuilder(msisdn).setIsOnHike(true).build();
			mConversation.setMessages(HikeConversationsDatabase.getInstance().getConversationThread(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, mConversation, -1));
		}
		mContactInfo = ContactManager.getInstance().getContact(mConversation.getDisplayMsisdn(), true, true);
		mConversation.setConversationName(mContactInfo == null ? mConversation.getDisplayMsisdn() : mContactInfo.getName());
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
			addMessage(convMessage);

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
	public void wifiScanResults(Map<String, ScanResult> list)
	{

	}

	private void sendUpdateStatusMessageOnHandler(int id)
	{
		Message msg = Message.obtain();
		msg.what = UPDATE_CONNECTION_STATUS;
		msg.obj = getString(id);
		uiHandler.sendMessage(msg);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.attachment:
			if (TextUtils.isEmpty(controller.getConnectedDevice()) || (!controller.getConnectedDevice().equals(mConversation.getDisplayMsisdn())))
			{
				showToast("You are Disconnected.Kindly connect Hike Direct");
			}
			else
			{
				showOfflineAttchmentPicker();
				activity.showProductPopup(ProductPopupsConstants.PopupTriggerPoints.ATCH_SCR.ordinal());
			}
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

	// protected void startHikeGallery(boolean onHike)
	// {
	// Intent imageIntent = IntentFactory.getHikeGallaryShare(activity.getApplicationContext(), msisdn, onHike);
	// imageIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
	// imageIntent.putExtra(HikeConstants.Extras.OFFLINE_MODE_ON, true);
	// activity.startActivityForResult(imageIntent, AttachmentPicker.GALLERY);
	// }

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.connect_offline:
			if (item.text == getString(R.string.connect_offline))
			{
				connectClicked();
			}
			else
			{
				disconnectClicked();
			}
			break;
		default:
			super.itemClicked(item);
		}
	}

	private void disconnectClicked()
	{
		Toast.makeText(activity, "Disconnected", Toast.LENGTH_SHORT).show();
		controller.shutDown();
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
		case AttachmentPicker.APPS:
			if (data != null)
			{
				String filePath = data.getStringExtra(OfflineConstants.EXTRAS_APK_PATH);
				String mime = data.getStringExtra(HikeConstants.Extras.FILE_TYPE);
				String apkLabel = data.getStringExtra(OfflineConstants.EXTRAS_APK_NAME);
				controller.sendApps(filePath, mime, apkLabel, msisdn);
			}
			break;
		case AttachmentPicker.FILE:
			if (data != null)
			{
				controller.sendFile(data, msisdn);
			}
			break;

		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void blockUnBlockUser(boolean isBlocked)
	{
		/*
		 * If offline mode is on, then disconnect the user if the blocked user's msisdn is that of the current chat thread
		 */
		// if (isOfflineModeOn && isBlocked)
		// {
		// OfflineManager.getInstance().disconnect();
		// Intent homeIntent = IntentFactory.getHomeActivityIntent(activity);
		// activity.startActivity(homeIntent);
		// }

		super.blockUnBlockUser(isBlocked);
	}

	@Override
	protected void onStop()
	{
		// Initialate a timer of x seconds to save battery.
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
		controller.sendImage(imagePath, msisdn);
	}

	@Override
	public void imageParsed(Uri uri)
	{
		controller.sendImage(Utils.getRealPathFromUri(uri, activity.getApplicationContext()), msisdn);
	}

	protected void showToast(String message)
	{
		Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void pickFileSuccess(int requestCode, String filePath)
	{

		if (filePath == null)
		{
			showToast("filePath is null");
			return;
		}
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
		controller.sendAudioFile(filePath, duration, msisdn);
	}

	private void checkIfSharingFiles(Intent intent)
	{
		// if (intent.hasExtra(HikeConstants.Extras.OFFLINE_SHARING_INTENT))
		// {
		// //We are coming from the sharing option.
		// //OfflineManager.getInstance().forwardSharingFiles(intent);
		// }
	}

	@Override
	public void onDisconnect(ERRORCODE errorCode)
	{
		sendUpdateStatusMessageOnHandler(R.string.connection_deestablished);
		switch (errorCode)
		{
		case OUT_OF_RANGE:
			break;
		case TIMEOUT:
			sendComposeViewStatusonHandler(false);
			break;
		case USERDISCONNECTED:
			final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage(msisdn, activity.getString(R.string.connection_deestablished),
					OfflineConstants.OFFLINE_MESSAGE_DISCONNECTED_TYPE);
			Message msg = Message.obtain();
			msg.what = MESSAGE_RECEIVED;
			msg.obj = convMessage;
			//uiHandler.sendMessage(msg);
			sendComposeViewStatusonHandler(false);
			break;
		case COULD_NOT_CONNECT:
			break;
		default:
			break;
		}
	}

	@Override
	public void onDestroy()
	{
		if (controller != null)
		{
			controller.removeListener(this);
		}
		super.onDestroy();
	}

	@Override
	public void connectedToMsisdn(String connectedDevice)
	{
		Logger.d(TAG, "I am connected to " + connectedDevice);
		sendUpdateStatusMessageOnHandler(R.string.connection_established);
		sendComposeViewStatusonHandler(true);
		final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage(msisdn, activity.getString(R.string.connection_established),
				OfflineConstants.OFFLINE_MESSAGE_CONNECTED_TYPE);

		Message msg = Message.obtain();
		msg.what = MESSAGE_RECEIVED;
		msg.obj = convMessage;
		uiHandler.sendMessage(msg);
	}

	private void sendComposeViewStatusonHandler(boolean b)
	{
		Message msg = Message.obtain();
		msg.what = UPDATE_COMPOSE_VIEW;
		msg.obj = b;
		uiHandler.sendMessage(msg);
	}
	
	private void toggleComposeView(boolean isEnabled)
	{
		canPoke = isEnabled;
		activity.findViewById(R.id.compose_container).setVisibility(isEnabled ? View.VISIBLE : View.GONE);
	}
	
	@Override
	protected void setAvatar() {
		ImageView avatar = (ImageView) mActionBarView.findViewById(R.id.avatar);
		if (avatar == null)
		{
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn.replace("o:", ""));
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(msisdn.replace("o:", ""), false);
		}

		setAvatarStealthBadge();
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);

	}

}