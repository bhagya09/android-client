package com.bsb.hike.chatthread;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OfflineConversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

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
	protected void init()
	{
		// TODO Auto-generated method stub
		super.init();
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		checkIfSharingFiles(activity.getIntent());
		checkIfWeNeedToConnect(activity.getIntent());
		boolean  isFirstOfflineUser = checkAndMarkIfFirstOfflineChat();
		if(isFirstOfflineUser)
		{
			checkAndAddOfflineHeaderMessage();
		}
		activity.updateActionBarColor(new ColorDrawable(Color.BLACK));
	}


	
	private boolean checkAndMarkIfFirstOfflineChat() {
		
		boolean  isFirstOfflineUser = HikeSharedPreferenceUtil.getInstance().contains(OfflineConstants.OFFLINE_FTUE_INFO);
		if(!isFirstOfflineUser)
		{
			JSONObject offlineFtueInfo  =  new JSONObject();
			try {
				offlineFtueInfo.put(OfflineConstants.OFFLINE_FTUE_SHOWN_AND_CANCELLED,false);
				offlineFtueInfo.put(OfflineConstants.FIRST_OFFLINE_MSISDN,msisdn);
			} catch (JSONException e) {
				Logger.e(TAG, "Problems with JSON");
			}
			HikeSharedPreferenceUtil.getInstance().saveData(OfflineConstants.OFFLINE_FTUE_INFO,offlineFtueInfo.toString());
			
		}
		return isFirstOfflineUser;
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
			case R.string.chat_theme:
				if (TextUtils.isEmpty(controller.getConnectedDevice()) || (!controller.getConnectedDevice().equals(mConversation.getDisplayMsisdn())))
				{
					overFlowMenuItem.enabled=false;
				}
				else
				{
					overFlowMenuItem.enabled=true;
				}
			}
		}

	}

	@Override
	public void onNewIntent()
	{
		super.onNewIntent();
		takeActionBasedOnIntent();
		checkIfWeNeedToConnect(activity.getIntent());
		activity.updateActionBarColor(new ColorDrawable(Color.BLACK));
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
	protected void showNetworkError(boolean isNetworkError) {
		// Do no show any pop in offline chat 
	}
	private void checkAndAddOfflineHeaderMessage() {
		try {
			Boolean offlineFtueInfoAvailable = HikeSharedPreferenceUtil.getInstance().contains(OfflineConstants.OFFLINE_FTUE_INFO);
			if(offlineFtueInfoAvailable)
			{
				//status -  true means Ftue card has been cancelled
				//			false means Ftue card has not been cancelled
				JSONObject offlineFtueInfo  = new  JSONObject(HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.OFFLINE_FTUE_INFO,null));
				boolean status = offlineFtueInfo.getBoolean(OfflineConstants.OFFLINE_FTUE_SHOWN_AND_CANCELLED);
				if(!status)
				{
					String firstOfflineMsisdn = offlineFtueInfo.getString(OfflineConstants.FIRST_OFFLINE_MSISDN);
					if(firstOfflineMsisdn.compareTo(msisdn)==0)
					{
						if (mContactInfo != null  && messages != null)
						{
							if(messages.size()>0)
							{
								ConvMessage convMessage1 = messages.get(0);
								/**
								 * Check if the conv message was previously a block header or not
								 */
								if (!convMessage1.isBlockAddHeader() &&  !convMessage1.isOfflineFtueHeader())
								{
									/**
									 * Creating a new conv message to be appended at the 0th position.
									 */
									convMessage1 = new ConvMessage(0, 0l, 0l);
									convMessage1.setOfflineFtueHeader(true);
									messages.add(0, convMessage1);
									Logger.d(TAG, "Adding unknownContact Header to the chatThread");

									if (mAdapter != null)
									{
										mAdapter.notifyDataSetChanged();
									}
								}
								else if(convMessage1.isBlockAddHeader())
								{	
									ConvMessage  convMessage2 = new ConvMessage(0, 0l, 0l);
									convMessage2.setOfflineFtueHeader(true);
									if(messages.size()>1)
									{
										if(!messages.get(1).isOfflineFtueHeader())
										{
											messages.add(1,convMessage2);
										}
									}
									else
									{
										messages.add(1, convMessage1);
									}
									if (mAdapter != null)
									{
										mAdapter.notifyDataSetChanged();
									}
								}
							}
							else
							{
								ConvMessage  convMessage2 = new ConvMessage(0, 0l, 0l);
								convMessage2.setOfflineFtueHeader(true);
								messages.add(1,convMessage2);	
								if (mAdapter != null)
								{
									mAdapter.notifyDataSetChanged();
								}
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			Logger.d(TAG, "Problem with JSON");
		}

	}

	@Override
	public void onResume()
	{

		super.onResume();
		switch (controller.getOfflineState())
		{
		case CONNECTED:
			if (mConversation.getDisplayMsisdn().equals(controller.getConnectedDevice()))
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
//			if (TextUtils.isEmpty(controller.getConnectedDevice()) || (!controller.getConnectedDevice().equals(mConversation.getDisplayMsisdn())))
//			{
//				showToast("You are Disconnected.Kindly connect Hike Direct");
//			}
//			else
//			{
				showOfflineAttchmentPicker();
				activity.showProductPopup(ProductPopupsConstants.PopupTriggerPoints.ATCH_SCR.ordinal());
			//}
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
			if (item.text.equals(getString(R.string.connect_offline)))
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
		Logger.d("OfflineManager","The activity focus is "+activity.hasWindowFocus());
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
				ArrayList<ApplicationInfo> results = data.getParcelableArrayListExtra(OfflineConstants.APK_SELECTION_RESULTS);
				
				for(ApplicationInfo apk: results)
				{
					String filePath = apk.sourceDir;
					String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
					String apkLabel = (String)activity.getPackageManager().getApplicationLabel(apk);
					controller.sendApps(filePath, mime, apkLabel, msisdn);
				}
				
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
			ConvMessage convMessage =  OfflineUtils.createOfflineContactConvMessage(msisdn,((PhonebookContact) dialog.data).jsonData,mContactInfo.isOnhike());
			sendMessage(convMessage);
			dialog.dismiss();
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
    protected void takeActionBasedOnIntent() 
    {
    	Intent intent = activity.getIntent();
		/**
		 * 1. Trying to forward a file
		 */
		if (intent.hasExtra(HikeConstants.Extras.FILE_PATH))
		{
			controller.sendFile(intent,msisdn);
			// Making sure the file does not get forwarded again on
			// orientation change.
			intent.removeExtra(HikeConstants.Extras.FILE_PATH);
		}

		/**
		 * 2. Multi Forward :
		 */
		else if (intent.hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
		{
			String jsonString = intent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);

			try
			{
				JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
				int msgCount = multipleMsgFwdArray.length();
				for (int i = 0; i < msgCount; i++)
				{
					JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
					if (msgExtrasJson.has(HikeConstants.Extras.MSG))
					{
						String msg = msgExtrasJson.getString(HikeConstants.Extras.MSG);
						ConvMessage convMessage = Utils.makeConvMessage(msisdn, msg, mConversation.isOnHike());
						sendMessage(convMessage);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.POKE))
					{
						sendPoke();
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
					{
						    controller.sendFile(intent,msgExtrasJson,msisdn);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.LATITUDE) && msgExtrasJson.has(HikeConstants.Extras.LONGITUDE)
							&& msgExtrasJson.has(HikeConstants.Extras.ZOOM_LEVEL))
					{
						double latitude = msgExtrasJson.getDouble(HikeConstants.Extras.LATITUDE);
						double longitude = msgExtrasJson.getDouble(HikeConstants.Extras.LONGITUDE);
						int zoomLevel = msgExtrasJson.getInt(HikeConstants.Extras.ZOOM_LEVEL);
						ChatThreadUtils.initialiseLocationTransfer(activity.getApplicationContext(), msisdn, latitude, longitude, zoomLevel, mConversation.isOnHike(),true);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
					{
						try
						{
							JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
							ConvMessage offlineConvMessage = OfflineUtils.createOfflineContactConvMessage(msisdn,contactJson,mConversation.isOnHike());
							sendMessage(offlineConvMessage);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
					else if (msgExtrasJson.has(StickerManager.FWD_CATEGORY_ID))
					{
						String categoryId = msgExtrasJson.getString(StickerManager.FWD_CATEGORY_ID);
						String stickerId = msgExtrasJson.getString(StickerManager.FWD_STICKER_ID);
						Sticker sticker = new Sticker(categoryId, stickerId);
						sendSticker(sticker, StickerManager.FROM_FORWARD);
						boolean isDis = sticker.isDisabled(sticker, activity.getApplicationContext());
						// add this sticker to recents if this sticker is not disabled
						if (!isDis)
						{
							StickerManager.getInstance().addRecentSticker(sticker);
						}
						/*
						 * Making sure the sticker is not forwarded again on orientation change
						 */
						intent.removeExtra(StickerManager.FWD_CATEGORY_ID);
					}

                    else if(msgExtrasJson.optInt(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.CONTENT){
                        // as we will be changing msisdn and hike status while inserting in DB
                        ConvMessage convMessage = Utils.makeConvMessage(msisdn, mConversation.isOnHike());
                        convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.CONTENT);
                        convMessage.platformMessageMetadata = new PlatformMessageMetadata(msgExtrasJson.optString(HikeConstants.METADATA), activity.getApplicationContext());
                        convMessage.platformMessageMetadata.addThumbnailsToMetadata();
                        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);

                        sendMessage(convMessage);

                    }

					else if(msgExtrasJson.optInt(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || msgExtrasJson.optInt(
							HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(msisdn,msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE), mConversation.isOnHike());
						convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
						convMessage.webMetadata = new WebMetadata(msgExtrasJson.optString(HikeConstants.METADATA));
						JSONObject json = new JSONObject();
						try
						{
							json.put(HikePlatformConstants.CARD_TYPE, convMessage.webMetadata.getAppName());
							json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_FORWARD);
							json.put(AnalyticsConstants.TO, msisdn);
							HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						catch (NullPointerException e)
						{
							e.printStackTrace();
						}
						sendMessage(convMessage);

					}


				}
				
				if (mActionMode != null && mActionMode.isActionModeOn())
				{
					mActionMode.finish();
				}
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Invalid JSON Array", e);
			}
			intent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
		}
		/**
		 * 5. Multiple files
		 */
		else if (intent.hasExtra(HikeConstants.Extras.FILE_PATHS))
		{
			controller.sendFile(intent, msisdn);
			intent.removeExtra(HikeConstants.Extras.FILE_PATHS);
		}
		else
		{
			super.takeActionBasedOnIntent();
		}
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
		//uiHandler.sendMessage(msg);
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
		//activity.findViewById(R.id.compose_container).setVisibility(isEnabled ? View.VISIBLE : View.GONE);
		disableEnableControls(isEnabled, (RelativeLayout)activity.findViewById(R.id.compose_container));
	}
	
	
	private void disableEnableControls(boolean enable, ViewGroup vg)
	{
		for (int i = 0; i < vg.getChildCount(); i++)
		{
			View child = vg.getChildAt(i);
			child.setEnabled(enable);
			if (child instanceof ViewGroup)
			{
				disableEnableControls(enable, (ViewGroup) child);
			}
		}
	}
	
	/**
	 * Overriding this method as we have to set FileKey if is a File ConvObject.As w/o file key the file will not open.
	 */
	@Override
	protected boolean onMessageDelivered(Object object)
	{
		Pair<String, Long> pair = (Pair<String, Long>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first))
		{
			return false;
		}
		long msgID = pair.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		ConvMessage msg = findMessageById(msgID);
		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
		{
			msg.setState(ConvMessage.State.SENT_DELIVERED);
			if (OfflineUtils.isFileTransferMessage(msg.serialize()))
			{
				if (TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
				{
					msg.getMetadata().getHikeFiles().get(0).setFileKey("OfflineFileKey" + System.currentTimeMillis() / 1000);
				}
			}
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			return true;
		}
		return false;
	}

	@Override
	protected void setAvatar()
	{
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