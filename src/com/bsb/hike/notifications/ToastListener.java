package com.bsb.hike.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.MqttConstants.MQTTConnectionStatus;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.*;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.productpopup.AtomicTipContentModel;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;

public class ToastListener implements Listener
{
	private WeakReference<Activity> currentActivity;

	private HikeNotification toaster;

	private Context context;

	private MQTTConnectionStatus mCurrentUnnotifiedStatus;

	private static ToastListener mToastListener=new ToastListener();

	String[] hikePubSubListeners = { HikePubSub.PUSH_AVATAR_DOWNLOADED, HikePubSub.PUSH_FILE_DOWNLOADED, HikePubSub.PUSH_STICKER_DOWNLOADED, HikePubSub.MESSAGE_RECEIVED,
			HikePubSub.NEW_ACTIVITY, HikePubSub.CONNECTION_STATUS, HikePubSub.FAVORITE_TOGGLED, HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED,
			HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS, HikePubSub.CANCEL_ALL_NOTIFICATIONS, HikePubSub.PROTIP_ADDED, HikePubSub.UPDATE_PUSH, HikePubSub.APPLICATIONS_PUSH,
			HikePubSub.SHOW_FREE_INVITE_SMS, HikePubSub.STEALTH_POPUP_WITH_PUSH, HikePubSub.HIKE_TO_OFFLINE_PUSH, HikePubSub.ATOMIC_POPUP_WITH_PUSH,
			HikePubSub.BULK_MESSAGE_NOTIFICATION, HikePubSub.USER_JOINED_NOTIFICATION,HikePubSub.ACTIVITY_UPDATE_NOTIF, HikePubSub.FLUSH_PERSISTENT_NOTIF,
			HikePubSub.SHOW_PERSISTENT_NOTIF, HikePubSub.ATOMIC_TIP_WITH_NOTIF, HikePubSub.SHOW_BIRTHDAY_NOTIF, HikePubSub.RICH_USER_JOINED_NOTIFICATION};

	/**
	 * Used to check whether NUJ/RUJ message notifications are disabled
	 */
	private SharedPreferences mDefaultPreferences;

	public static ToastListener getInstance()
	{
		return mToastListener;
	}

	private ToastListener()
	{
		HikeMessengerApp.getPubSub().addListeners(this, hikePubSubListeners);
		this.toaster = HikeNotification.getInstance();
		this.context = HikeMessengerApp.getInstance().getApplicationContext();
		mCurrentUnnotifiedStatus = MQTTConnectionStatus.NOT_CONNECTED;
		mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.NEW_ACTIVITY.equals(type))
		{
			Activity activity = (Activity) object;
			if ((activity != null) && (mCurrentUnnotifiedStatus != MQTTConnectionStatus.NOT_CONNECTED))
			{
				notifyConnStatus(mCurrentUnnotifiedStatus);
				mCurrentUnnotifiedStatus = MQTTConnectionStatus.NOT_CONNECTED;
			}

			currentActivity = new WeakReference<Activity>(activity);
		}
		else if (HikePubSub.CONNECTION_STATUS.equals(type))
		{
			MqttConstants.MQTTConnectionStatus status = (MqttConstants.MQTTConnectionStatus) object;
			mCurrentUnnotifiedStatus = status;
			notifyConnStatus(status);
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			/*
			 * Only notify when someone has added the user as a favorite.
			 */
			if (favoriteType != FavoriteType.REQUEST_RECEIVED)
			{
				return;
			}
			Activity activity = (currentActivity != null) ? currentActivity.get() : null;
			if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				this.toaster.notifyStealthMessage(NotificationType.HIDDEN,contactInfo.getMsisdn());
			}
			else
			{
				toaster.notifyFavorite(contactInfo, NotificationType.FAVADD);
			}
		}
		else if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type))
		{
			int notificationType=NotificationType.STATUSUPDATE;
			Activity activity = (currentActivity != null) ? currentActivity.get() : null;
			if (activity instanceof TimelineActivity)
			{
				Utils.resetUnseenStatusCount(activity);
				HikeMessengerApp.getPubSub().publish(HikePubSub.UNSEEN_STATUS_COUNT_CHANGED, null);
				
				if (((TimelineActivity) activity).isUpdatesFrgamentOnTop())
				{
					HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, null);
					return;
				}
			}
			StatusMessage statusMessage = (StatusMessage) object;
			String msisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			if (!ContactManager.getInstance().shouldShowNotifForMutedConversation(statusMessage.getMsisdn()))
			{
				Logger.d(getClass().getSimpleName(), "Conversation has been muted");
				return;
			}
			if (msisdn.equals(statusMessage.getMsisdn()) || statusMessage.isHistoricalUpdate())
			{
				return;
			}
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				notificationType = NotificationType.DPUPDATE;
			}
			HikeMessengerApp.getInstance().getPubSub().publish(HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, null);

			
			if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) == 0)
			{
				// Is from Stealth contact? Show stealth notif
				if (StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()) && !StealthModeManager.getInstance().isActive())
				{
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE, true);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_ANIM_ON_RESUME, HikeConstants.STEALTH_INDICATOR_RESUME_EXPIRED);
					this.toaster.notifyStealthMessage(NotificationType.HIDDEN, statusMessage.getMsisdn());
				}
				else
				{
					toaster.notifyStatusMessage(statusMessage, notificationType);
				}
			}
		}
		else if (HikePubSub.ACTIVITY_UPDATE_NOTIF.equals(type))
		{
			int notificationType = NotificationType.ACTIVITYUPDATE;
			Activity activity = (currentActivity != null) ? currentActivity.get() : null;
			if (!(activity instanceof TimelineActivity))
			{
				FeedDataModel activityFeed = (FeedDataModel) object;
				if (StealthModeManager.getInstance().isStealthMsisdn(activityFeed.getActor()) && !StealthModeManager.getInstance().isActive())
				{
					if (activityFeed.getActionType() == ActionsDataModel.ActionTypes.LIKE)
					{
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE, true);
						HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_ANIM_ON_RESUME, HikeConstants.STEALTH_INDICATOR_RESUME_EXPIRED);
						this.toaster.notifyStealthMessage(NotificationType.HIDDEN, activityFeed.getActor());
					}
				}
				else
				{
					toaster.notifyActivityMessage(activityFeed, notificationType);
				}
			}
		}
		else if (HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED.equals(type))
		{
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			Pair<String, String> batchSU = (Pair<String, String>) object;
			toaster.notifyBatchUpdate(batchSU.first, batchSU.second, NotificationType.OTHER);
		}
		else if (HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS.equals(type))
		{
			toaster.cancelAllStatusNotifications();
		}
		else if (HikePubSub.PUSH_AVATAR_DOWNLOADED.equals(type))
		{
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
	
			/*
			 * this object contains a Bundle containing 3 strings among which one is imagepath of downloaded avtar. and other two are msisdn and name from which notification has
			 * come.
			 */
			Bundle notifyBundle = (Bundle) object;
			
			String statusId = notifyBundle.getString(HikeConstants.STATUS_ID,null);
			
			if(!TextUtils.isEmpty(statusId))
			{
				StatusMessage statusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(statusId);
				
				if(statusMessage == null)
				{
					return;
				}

				if (StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()))
				{
					return;
				}

				if (!ContactManager.getInstance().shouldShowNotifForMutedConversation(statusMessage.getMsisdn()))
				{
					Logger.d(getClass().getSimpleName(), "Conversation has been muted");
					return;
				}

				if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
				{
					toaster.notifyBigPictureStatusNotification(notifyBundle.getString(HikeConstants.Extras.PATH), notifyBundle.getString(HikeConstants.Extras.MSISDN),
							notifyBundle.getString(HikeConstants.Extras.NAME), NotificationType.IMAGE_POST);
				}
				else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					toaster.notifyBigPictureStatusNotification(notifyBundle.getString(HikeConstants.Extras.IMAGE_PATH), notifyBundle.getString(HikeConstants.Extras.MSISDN),
							notifyBundle.getString(HikeConstants.Extras.NAME), NotificationType.DPUPDATE);
				}
			}
			
			
		}
		else if (HikePubSub.PUSH_FILE_DOWNLOADED.equals(type) || HikePubSub.PUSH_STICKER_DOWNLOADED.equals(type))
		{
			if (object == null)
				return;
			ConvMessage message = (ConvMessage) object;
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}

			if (!message.isShouldShowPush())
			{
				return;
			}

			if (!ContactManager.getInstance().shouldShowNotifForMutedConversation(message.getMsisdn()))
			{
				Logger.d(getClass().getSimpleName(), "Conversation has been muted");
				return;
			}

			if(StealthModeManager.getInstance().isStealthMsisdn(message.getMsisdn()))
			{
				Logger.d(getClass().getSimpleName(), "this conversation is stealth");
				return;
			}
			final Bitmap bigPicture = returnBigPicture(message, context);
			if (bigPicture != null)
			{
				ContactInfo contactInfo;
				if (message.isOneToNChat())
				{
					Logger.d("ToastListener", "GroupName is " + ContactManager.getInstance().getName(message.getMsisdn()));
					contactInfo = new ContactInfo(message.getMsisdn(), message.getMsisdn(), ContactManager.getInstance().getName(message.getMsisdn()), message.getMsisdn());
				}
				else
				{
					contactInfo = ContactManager.getInstance().getContact(message.getMsisdn(), true, true);
				}

				toaster.notifyMessage(contactInfo, message, true, bigPicture, NotificationType.OTHER);
			}
		}
		else if (HikePubSub.CANCEL_ALL_NOTIFICATIONS.equals(type))
		{
			toaster.cancelAllNotifications();
		}
		else if (HikePubSub.PROTIP_ADDED.equals(type))
		{
			Protip proTip = (Protip) object;
			if (currentActivity != null && currentActivity.get() != null)
			{
				return;
			}
			// the only check we now need is to check whether the pro tip has to
			// push flag true or not
			if (proTip.isShowPush())
				toaster.notifyMessage(proTip, NotificationType.OTHER);
		}
		else if (HikePubSub.UPDATE_PUSH.equals(type))
		{
			int update = ((Integer) object).intValue();
			// future todo: possibly handle the case where the alert has been
			// shown in
			// the app once for the update and
			// now the user has got a push update from our server.
			// if its critical, let it go through, if its normal, check the
			// preference.
			toaster.notifyUpdatePush(update, context.getPackageName(),
					context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.Extras.UPDATE_MESSAGE, ""), false);
		}
		else if (HikePubSub.APPLICATIONS_PUSH.equals(type))
		{
			if (object instanceof String)
			{
				String packageName = ((String) object);
				toaster.notifyUpdatePush(-1, packageName,
						context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.Extras.APPLICATIONSPUSH_MESSAGE, ""), true);
			}

		}
		else if(HikePubSub.SHOW_PERSISTENT_NOTIF.equals(type))
		{
			Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "Creating persistent notification!");
			HikeSharedPreferenceUtil settingPref = HikeSharedPreferenceUtil.getInstance();
			toaster.notifyPersistentUpdate(
					settingPref.getData(HikeConstants.UPDATE_TITLE, context.getResources().getString(R.string.pers_notif_title)),
					settingPref.getData(HikeConstants.Extras.UPDATE_MESSAGE, context.getResources().getString(R.string.pers_notif_message)),
					settingPref.getData(HikeConstants.UPDATE_ACTION, context.getResources().getString(R.string.tip_and_notif_update_text)),
					settingPref.getData(HikeConstants.UPDATE_LATER, context.getResources().getString(R.string.tip_and_notif_later_text)),
					Uri.parse(settingPref.getData(HikeConstants.Extras.URL, "market://details?id=" + context.getPackageName())),
					settingPref.getData(HikeConstants.UPDATE_ALARM, HikeConstants.PERS_NOTIF_ALARM_DEFAULT));
		}
		else if(HikePubSub.FLUSH_PERSISTENT_NOTIF.equals(type))
		{
			toaster.cancelPersistNotif();
		}
		else if (HikePubSub.SHOW_FREE_INVITE_SMS.equals(type))
		{
			if (object != null && object instanceof Bundle)
			{
				Bundle bundle = (Bundle) object;
				String bodyString = bundle.getString(HikeConstants.Extras.FREE_SMS_POPUP_BODY);
				// TODO: we may need the title tomorrow, so we can extract that
				// too from the bundle
				if (!TextUtils.isEmpty(bodyString))
				{
					toaster.notifySMSPopup(bodyString, NotificationType.OTHER);
				}
			}
		}
		else if (HikePubSub.STEALTH_POPUP_WITH_PUSH.equals(type))
		{
			if (object != null && object instanceof Bundle)
			{
				Bundle bundle = (Bundle) object;
				String header = bundle.getString(HikeConstants.Extras.STEALTH_PUSH_BODY);
				if (!TextUtils.isEmpty(header))
				{
					toaster.notifyStealthPopup(header, NotificationType.OTHER); // TODO: toasting header
					// for now
				}
			}
		}
		else if (HikePubSub.ATOMIC_POPUP_WITH_PUSH.equals(type))
		{
			if (object != null && object instanceof Bundle)
			{
				Bundle bundle = (Bundle) object;
				String header = bundle.getString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_MESSAGE);
				String className = bundle.getString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_SCREEN);
				if (!TextUtils.isEmpty(header))
				{
					Intent notificationIntent;
					try
					{
						notificationIntent = new Intent(context, Class.forName(className));
						notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						notificationIntent.putExtra(HikeConstants.Extras.HAS_TIP, true);
						toaster.notifyAtomicPopup(header, notificationIntent, NotificationType.OTHER);
					}
					catch (ClassNotFoundException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		else if(HikePubSub.ATOMIC_TIP_WITH_NOTIF.equals(type))
		{
			Logger.d(getClass().getSimpleName(), "Recived pubsub to show notif for atomic tip");
			if (object != null && object instanceof AtomicTipContentModel)
			{
				AtomicTipContentModel tipContentModel = (AtomicTipContentModel) object;
				toaster.notifyAtomicTip(tipContentModel);
			}
		}
		else if(HikePubSub.RICH_USER_JOINED_NOTIFICATION.equals(type))
		{
			Logger.d(getClass().getSimpleName(), "Recived pubsub to show rich uj notif");
			if (object != null && object instanceof JSONObject)
			{
				JSONObject jsonObject = (JSONObject) object;
				toaster.notifyRichUJ(jsonObject);
			}
		}
		else if (HikePubSub.HIKE_TO_OFFLINE_PUSH.equals(type))
		{
			int notificationType=NotificationType.H2O;
			if (object != null && object instanceof Bundle)
			{
				Bundle bundle = (Bundle) object;
				String offlineMsisdnsString = bundle.getString(HikeConstants.Extras.OFFLINE_PUSH_KEY);
				try
				{
					JSONObject offlineMsisdnsObject = new JSONObject(offlineMsisdnsString);
					JSONArray offlineMsisdnsArray = offlineMsisdnsObject.optJSONArray(HikeConstants.Extras.OFFLINE_MSISDNS);

					if (null != offlineMsisdnsArray && offlineMsisdnsArray.length() > 0)
					{
						int length = offlineMsisdnsArray.length();
						ArrayList<String> msisdnList = new ArrayList<String>(length); // original msisdn list
						for (int i = 0; i < length; i++)
						{
							msisdnList.add(offlineMsisdnsArray.getString(i));
						}

						String msisdnStatement = PhoneUtils.getMsisdnStatement(msisdnList);

						ArrayList<String> filteredMsisdnList = HikeConversationsDatabase.getInstance().getOfflineMsisdnsList(msisdnStatement); // this db query will
																																				// return new list
																																				// which can be of
																																				// different order
																																				// and different
																																				// length

						if (filteredMsisdnList == null || filteredMsisdnList.size() == 0)
						{
							Logger.e("HikeToOffline", "no chats with undelivered messages");
							return;
						}

						msisdnStatement = PhoneUtils.getMsisdnStatement(filteredMsisdnList);
						List<ContactInfo> contactList = ContactManager.getInstance().getContact(filteredMsisdnList, true, false); // contact info list

						HashMap<String, String> nameMap = new HashMap<String, String>(); // nameMap to map msisdn to corresponding name
						for (ContactInfo contactInfo : contactList)
						{
							nameMap.put(contactInfo.getMsisdn(), contactInfo.getName());
						}

						for (String msisdn : filteredMsisdnList)
						{
							if (nameMap.get(msisdn) == null)
							{
								nameMap.put(msisdn, msisdn);
							}
						}

						filteredMsisdnList.clear();
						for (String msisdn : msisdnList) // running loop to
															// bring back
															// original order
						{
							if (nameMap.containsKey(msisdn))
							{
								filteredMsisdnList.add(msisdn);
							}
						}

						Activity activity = (currentActivity != null) ? currentActivity.get() : null;

						if ((activity instanceof ChatThreadActivity))
						{
							String contactNumber = ((ChatThreadActivity) activity).getContactNumber();
							if (filteredMsisdnList.get(0).equals(contactNumber))
							{
								Logger.e("HikeToOffline", "same chat thread open");
								return;
							}
						}
						toaster.notifyHikeToOfflinePush(msisdnList, nameMap, NotificationType.H2O);

					}
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					Logger.e("HikeToOffline", "Json Exception", e);
				}

			}
		}
		else if (HikePubSub.BULK_MESSAGE_NOTIFICATION.equals(type) || HikePubSub.MESSAGE_RECEIVED.equals(type) || HikePubSub.USER_JOINED_NOTIFICATION.equals(type))
		{
			int notificationType;
			// Received bulk message map (msisdn - ConvMessage(s) pairs)
			LinkedList<ConvMessage> messageList = null;
			Map<String, LinkedList<ConvMessage>> messageListMap = null;
			if (object instanceof ConvMessage)
			{
				if (messageList == null)
				{
					messageList = new LinkedList<ConvMessage>();
				}

				ConvMessage receivedMsg = (ConvMessage) object;
				messageList.add(receivedMsg);
			}
			else if (object instanceof List)
			{
				try
				{
					messageList = (LinkedList<ConvMessage>) object;
				}
				catch (ClassCastException ex)
				{
					ex.printStackTrace();
					Logger.e("BulkMessageNotification", "Class cast exception", ex);
				}
			}
			else if (object instanceof Map)
			{
				try
				{
					messageListMap = (Map<String, LinkedList<ConvMessage>>) object;
					if (messageList == null)
					{
						messageList = new LinkedList<ConvMessage>();
					}

					for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
					{
						messageList.addAll(entry.getValue());
					}

				}
				catch (ClassCastException ex)
				{
					ex.printStackTrace();
					Logger.e("BulkMessageNotification", "Class cast exception", ex);
				}
			}

			if (messageList == null || messageList.isEmpty())
			{
				return;
			}

			// Iterate through all messages, removing the ones not to be
			// displayed/ included in notification
			// Maintain a list to store the ones to be included in the
			// notification
			ArrayList<ConvMessage> filteredMessageList = new ArrayList<ConvMessage>();

			for (ConvMessage message : messageList)
			{
				if (message.isShouldShowPush())
				{
					String msisdn = message.getMsisdn();

					if (OneToNConversationUtils.isGroupConversation(msisdn) && !ContactManager.getInstance().isConvExists(msisdn))
					{
						Logger.w(getClass().getSimpleName(), "The client did not get a GCJ message for us to handle this message.");
						continue;
					}

					if (!ContactManager.getInstance().shouldShowNotifForMutedConversation(msisdn))
					{
						Logger.d(getClass().getSimpleName(), "Conversation has been muted");
						continue;
					}

					if (message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT && message.webMetadata.getPushType().equals(HikePlatformConstants.NO_PUSH))
					{
						continue;
					}
					ParticipantInfoState participantInfoState = message.getParticipantInfoState();
					if (participantInfoState == ParticipantInfoState.USER_JOIN && (!mDefaultPreferences.getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true)))

					{
						// User has disabled NUJ/RUJ message notifications
						continue;
					}

					if (participantInfoState == ParticipantInfoState.PARTICIPANT_JOINED )
					{
						if(message.getMetadata().isNewGroup() || message.getMetadata().isNewBroadcast())
						{
							continue;
						}
					}
					if (message.getPlatformData() != null && message.getPlatformData().optString(HikeConstants.PLAY_NOTIFICATION).equals(HikeConstants.OFF))
					{
						continue;
					}
					if (participantInfoState == ParticipantInfoState.NO_INFO || participantInfoState == ParticipantInfoState.PARTICIPANT_JOINED
						|| participantInfoState == ParticipantInfoState.USER_JOIN || participantInfoState == ParticipantInfoState.CHAT_BACKGROUND 
						|| message.isVoipMissedCallMsg() || participantInfoState == ParticipantInfoState.OFFLINE_INLINE_MESSAGE ||participantInfoState == ParticipantInfoState.CHANGE_ADMIN)
					{
						if (participantInfoState == ParticipantInfoState.CHAT_BACKGROUND)
						{
							boolean showNotification = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.CHAT_BG_NOTIFICATION_PREF, true);
							if (!showNotification)
							{
								continue;
							}
						}

						Activity activity = (currentActivity != null) ? currentActivity.get() : null;
						if ((activity instanceof ChatThreadActivity))
						{
							String contactNumber = ((ChatThreadActivity) activity).getContactNumber();
							if (message.getMsisdn().equals(contactNumber))
							{
								continue;
							}
						}

						if (StealthModeManager.getInstance().isStealthMsisdn(msisdn))
						{
							notificationType = NotificationType.HIDDEN;
							this.toaster.notifyStealthMessage(notificationType,msisdn);
						}
						else
						{
							
							notificationType = getNotificationType(message);
							message.setNotificaionType(notificationType);
							Logger.d("NotificationRetry", "Toast Listener msg received call with type" + notificationType);
							filteredMessageList.add(message);
						}
					}
				}
			}
			if (!filteredMessageList.isEmpty())
			{
				this.toaster.notifySummaryMessage(filteredMessageList);
			}
			// Remove unused references
			filteredMessageList.clear();
			filteredMessageList = null;
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_MESSAGE_CHANGED, null);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, null);
		}
		else if(HikePubSub.SHOW_BIRTHDAY_NOTIF.equals(type))
		{
			if (object != null && object instanceof Pair)
			{
				toaster.notifyBdayNotif((Pair<ArrayList<String>, String>)object);
			}
		}
	}

	

	private int getNotificationType(ConvMessage convMessage)
	{
		int notificationType = NotificationType.NORMALMSG1TO1;
		if (BotUtils.isBot(convMessage.getMsisdn()))
			notificationType = NotificationType.BOTMSG;

		if (OneToNConversationUtils.isGroupConversation(convMessage.getMsisdn()))
			notificationType = NotificationType.NORMALGC;

		if (convMessage.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
			notificationType = NotificationType.CHATTHEMECHNG;

		if (convMessage.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
			notificationType = NotificationType.NUJORRUJ;

		return notificationType;
	}

	public static Bitmap returnBigPicture(ConvMessage convMessage, Context context)
	{

		HikeFile hikeFile = null;
		Bitmap bigPictureImage = null;

		// Check if this is a file transfer message of image type
		// construct a bitmap only if the big picture condition matches
		if (convMessage.isFileTransferMessage())
		{
			hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile != null)
			{
				if (hikeFile.getHikeFileType() == HikeFileType.IMAGE && hikeFile.wasFileDownloaded() && hikeFile.getThumbnail() != null)
				{
					final String filePath = hikeFile.getFilePath();

					bigPictureImage = HikeBitmapFactory.scaleDownBitmap(filePath, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX,
							Bitmap.Config.RGB_565, true, false);
				}
			}

		}

		// check, if this is a sticker message and find, if it is not downloaded or not present.
		if (convMessage.isStickerMessage())
		{
			final Sticker sticker = convMessage.getMetadata().getSticker();
			final String filePath = sticker.getLargeStickerPath();
			if (!TextUtils.isEmpty(filePath))
			{
				bigPictureImage = HikeBitmapFactory.decodeFile(filePath);
			}
		}

		return bigPictureImage;
	}

	private void notifyConnStatus(MQTTConnectionStatus status)
	{
		/* only show the trying to connect message after we've connected once */
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		boolean connectedOnce = settings.getBoolean(HikeMessengerApp.CONNECTED_ONCE, false);
		if (status == MqttConstants.MQTTConnectionStatus.CONNECTED)
		{
			HikeNotification.getInstance().cancelNotification(HikeConstants.HIKE_SYSTEM_NOTIFICATION);
			if (!connectedOnce)
			{
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.CONNECTED_ONCE, true);
				editor.commit();
			}
			return;
		}

	}

	public void notifyUser(String text, String title)
	{
		Intent intent = IntentFactory.getHomeActivityIntent(context);
		toaster.showBigTextStyleNotification(intent, 0, System.currentTimeMillis(), HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID, title, text, title, "",
				null, null, false, 0);
	}

	/**
	 * This method is called from @link GeneralEventMessagesManager. It is used to notify a user for events related to a message
	 *
	 * @param msisdn
	 * @param message
	 * @param forceNotPlaySound
	 */
	public void showMessageEventNotification(String msisdn, String message, boolean forceNotPlaySound)
	{
		Activity activity = (currentActivity != null) ? currentActivity.get() : null;

		if ((activity instanceof ChatThreadActivity))
		{
			String contactNumber = ((ChatThreadActivity) activity).getContactNumber();
			if (TextUtils.isEmpty(msisdn) || (msisdn.equals(contactNumber)))
			{
				Logger.e("ToastListener", "Either the same chat thread was open or the msisdn passed is null");
				return;
			}
		}

		toaster.sendNotificationToChatThread(msisdn, message, forceNotPlaySound);

	}

	public void showBotDownloadNotification(String msisdn, String message, boolean forceNotPlaySound)
	{
		Activity activity = (currentActivity != null) ? currentActivity.get() : null;

		if ((activity instanceof WebViewActivity))
		{
			String activityMsisdn=((WebViewActivity) activity).msisdn;
			BotInfo botInfo=BotUtils.getBotInfoForBotMsisdn(msisdn);
			NonMessagingBotMetadata nmData=new NonMessagingBotMetadata(botInfo.getMetadata());
			if (TextUtils.isEmpty(msisdn) || TextUtils.isEmpty(activityMsisdn)|| botInfo==null ||nmData==null ||  (activityMsisdn.equals(nmData.getParentMsisdn())))
			{
				Logger.e("ToastListener", "Either the parent bot was open or the msisdn passed is null or botinfo is null");
				return;
			}
		}
		else //Suppressing game notifs if already playing a game as game switch is not possible
		{
			BotInfo botInfo=BotUtils.getBotInfoForBotMsisdn(msisdn);
			if(botInfo==null)
				return;
			NonMessagingBotMetadata nmData=new NonMessagingBotMetadata(botInfo.getMetadata());
			if(nmData==null)
				return;
			if(nmData.isNativeMode())
			{
				SharedPreferences settings = context.getSharedPreferences(HikePlatformConstants.GAME_PROCESS, context.MODE_MULTI_PROCESS);
				boolean gameActive = settings.getBoolean(HikePlatformConstants.GAME_ACTIVE, false);
				if(gameActive)
				{
					return;
				}
			}
		}

		toaster.sendNotificationToChatThread(msisdn, message, forceNotPlaySound);

	}


}
