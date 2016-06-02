package com.bsb.hike.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.NotificationPreview;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
 * 
 * @author Atul
 * 
 */
public class HikeNotificationMsgStack implements Listener
{

	private static volatile HikeNotificationMsgStack mHikeNotifMsgStack;

	private static Context mContext;

	// Construct to store msisdn - message1,message2,message3
	private LinkedHashMap<String, LinkedList<NotificationPreview>> mMessagesMap;

	private Intent mNotificationIntent;

	private HikeConversationsDatabase mConvDb;

	private ArrayList<SpannableString> mBigTextList;

	private ConvMessage mLastInsertedConvMessage;

	// Saving ticker text here. A line is inserted for each new message added to the stack.
	// This is cleared once getNotificationTickerText() is called.
	private StringBuilder mTickerText;

	public String lastAddedMsisdn = "";  // FIX FOR AND - 4316

	private long latestAddedTimestamp;

	private final int MAX_LINES = 7;

	private int totalNewMessages;

	private boolean forceBlockNotificationSound;
	
	private int maxRetryCount=0;

	private NotificationRetryCountModel mmCountModel;

	/**
	 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
	 * 
	 * @param argContext
	 * @return
	 */
	public static HikeNotificationMsgStack getInstance()
	{
		if(mHikeNotifMsgStack==null)
		synchronized (HikeNotificationMsgStack.class)
		{
			if(mHikeNotifMsgStack==null)
			{
				Logger.d("notification","HikeNotificationMsgStack");
				mHikeNotifMsgStack=new HikeNotificationMsgStack();
				HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, mHikeNotifMsgStack);
			}
		}
		return mHikeNotifMsgStack;
	}

	private HikeNotificationMsgStack()
	{
		
		mMessagesMap = new LinkedHashMap<String, LinkedList<NotificationPreview>>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, LinkedList<NotificationPreview>> eldest)
			{
				return size() > MAX_LINES;
			}
		};
		Logger.d("notification","HikeNotificationMsgStack....size is "+mMessagesMap.size());
		this.mConvDb = HikeConversationsDatabase.getInstance();
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		// We register for NEW_ACTIVITY so that when a chat thread is opened,
		// all unread notifications against the msisdn can be cleared
		
	}

	/**
	 * Add a message to existing notification message stack
	 * 
	 * @param argConvMessage
	 */
	public void addConvMessage(ConvMessage argConvMessage)
	{
		if (argConvMessage != null)
		{

			NotificationPreview convNotifPrvw = HikeNotificationUtils.getNotificationPreview(mContext, argConvMessage);

			addPair(argConvMessage.getMsisdn(), convNotifPrvw);

			mLastInsertedConvMessage = argConvMessage;
			
			forceBlockNotificationSound = argConvMessage.isSilent();
			
			Logger.d("NotificationRetry", "addConvMessage called");
		}

		

	}

	/**
	 * Add list of message to existing notification message stack
	 * 
	 * @param argConvMessageList
	 */
	public void addConvMessageList(List<ConvMessage> argConvMessageList)
	{
		if (argConvMessageList != null)
		{
			for (ConvMessage conv : argConvMessageList)
			{
				if (conv.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || conv.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT)
				{
					addMessage(conv.getMsisdn(), conv.webMetadata.getNotifText(),conv.getNotificationType());
					mLastInsertedConvMessage = conv;
					forceBlockNotificationSound = conv.isSilent();
				}
				else
				{
					addConvMessage(conv);					
				}
			}
		}
	}

	/**
	 * Add msisdn-message pair to be included in big view notificaitons
	 * 
	 * @param argMsisdn
	 * @param argMessage
	 * @param notificationType 
	 * @throws IllegalArgumentException
	 */
	public void addMessage(String argMsisdn, String argMessage, int notificationType) throws IllegalArgumentException
	{
		if (TextUtils.isEmpty(argMessage))
		{
			Log.wtf("HikeNotification", "Notification message is empty, check packet, msisdn= "+argMsisdn);
			return;
		}
		Logger.d("NotificationRetry", "addMessage called");
		addPair(argMsisdn, new NotificationPreview(argMessage, HikeNotificationUtils.getNameForMsisdn(argMsisdn),notificationType));
	}

	/**
	 * Adds msisdn-message pair into the notification messages stack. Also performs sorting/grouping/trimming of the existing data
	 * 
	 * @param argMsisdn
	 * @param argMessage
	 */
	private void addPair(String argMsisdn, NotificationPreview notifPrvw)
	{
		Logger.d("ToastListener","The Type is "+notifPrvw.getNotificationType()+"and the retry count is "+getNotificationCount(notifPrvw.getNotificationType()));
		lastAddedMsisdn = argMsisdn;

		maxRetryCount=Math.max(maxRetryCount, getNotificationCount(notifPrvw.getNotificationType()));
		Logger.d("NotificationRetry","Adding NOtification to stack and max retry value is "+maxRetryCount);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.MAX_REPLY_RETRY_NOTIF_COUNT, maxRetryCount);
		// If message stack consists of any stealth messages, do not add new message, change
		// stealth message string to notify multiple messages (add s to message"S")
		if (argMsisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY) && mMessagesMap.containsKey(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{
			int notificationType=NotificationType.HIDDEN;
			LinkedList<NotificationPreview> stealthMessageList = mMessagesMap.get(argMsisdn);

			// There should only be 1 item dedicated to stealth message
			// hike - You have new notification(s)
				stealthMessageList.set(0, new NotificationPreview(mContext.getString(R.string.stealth_notification_messages),null, notificationType));
		}
		else
		{
			// Add message to corresponding msisdn key in messages map
			if (mMessagesMap.containsKey(argMsisdn))
			{
				LinkedList<NotificationPreview> messagesList = mMessagesMap.get(argMsisdn);

				// Add message to the end of message list for a particular msisdn
				messagesList.add(notifPrvw);
				totalNewMessages++;
				if (!isFromSingleMsisdn())
				{
					// Move the conversation map to first index
					LinkedList<NotificationPreview> lastModifiedMapList = mMessagesMap.remove(argMsisdn);
					mMessagesMap.put(argMsisdn, lastModifiedMapList);
				}
			}
			else
			{
				LinkedList<NotificationPreview> newMessagesList = new LinkedList<NotificationPreview>();
				newMessagesList.add(notifPrvw);
				totalNewMessages++;
				mMessagesMap.put(argMsisdn, newMessagesList);
			}
		}

		trimMessageMap();

		latestAddedTimestamp = System.currentTimeMillis();

		if (mTickerText != null)
		{
			mTickerText.append("\n" + HikeNotificationUtils.getNameForMsisdn(argMsisdn) + " - " + notifPrvw.getMessage());
		}
		else
		{
			mTickerText = new StringBuilder();
			mTickerText.append(HikeNotificationUtils.getNameForMsisdn(argMsisdn) + " - " + notifPrvw.getMessage());
		}
	}

	private void trimMessageMap()
	{
		boolean trimmedAll = false;
		ListIterator<Entry<String, LinkedList<NotificationPreview>>> mapIterator = new ArrayList<Map.Entry<String, LinkedList<NotificationPreview>>>(mMessagesMap.entrySet()).listIterator();

		while (totalNewMessages > MAX_LINES  && !trimmedAll)
		{
			while (mapIterator.hasNext())
			{
				Entry<String, LinkedList<NotificationPreview>> entry = mapIterator.next();
				if (entry.getValue().size() > 1)
				{
					// Remove first message
					entry.getValue().removeFirst();
					return;
				}
			}

			trimmedAll = true;
		}
	}

	/**
	 * Invalidate object - use if there are changes to notifications messages stack
	 */
	public void invalidateConvMsgList()
	{
		updateNotificationIntent();
	}

	/**
	 * Determine whether the messages in stack are from single/multiple msisdns
	 * 
	 * @return null if the messages in stack are from multiple msisdns, else returns msisdn of the only participant
	 */
	public boolean isFromSingleMsisdn()
	{
		return mMessagesMap.size() == 1 ? true : false;
	}

	/**
	 * Update notification intent based on msisdns present in the message stack. If multiple msisdns are present, take user to HomeActivity else to the particular chat thread.
	 */
	private void updateNotificationIntent()
	{
		// TODO Maintain notification type globally.
		// To add notification types
		ExtendedHashSet uniqueNotifTypes = new ExtendedHashSet();

		// Iterate all the notification types in current notification stack
		ListIterator<Entry<String, LinkedList<NotificationPreview>>> mapIterator = new ArrayList<Map.Entry<String, LinkedList<NotificationPreview>>>(mMessagesMap.entrySet())
				.listIterator();
		List<NotificationPreview> notifListSingleMsisdn=null;
		while (mapIterator.hasNext())
		{
			Entry<String, LinkedList<NotificationPreview>> entry = mapIterator.next();

			if(entry.getValue()!=null)
			notifListSingleMsisdn = new LinkedList<>(entry.getValue());

			for (NotificationPreview preview : notifListSingleMsisdn)
			{
				uniqueNotifTypes.add(preview.getNotificationType());
			}
		}
		
		if (uniqueNotifTypes.equals(NotificationType.ACTIVITYUPDATE))
		{
			mNotificationIntent = Utils.getTimelineActivityIntent(mContext, true, true);
		}
		else if (uniqueNotifTypes.equals(NotificationType.FAVADD))
		{
			mNotificationIntent = Utils.getPeopleActivityIntent(mContext);
		}
		else if (containsStealthMessage())
		{
			mNotificationIntent = IntentFactory.getHomeActivityIntent(mContext);
		}
		else
		{
			// If new messages belong to different users/groups
			if (!isFromSingleMsisdn())
			{
				// Multiple msisdn, but only of type su,imagepost,activity,dp
				if (uniqueNotifTypes.containsOnly(new int[] { NotificationType.STATUSUPDATE, NotificationType.IMAGE_POST, NotificationType.DPUPDATE,
						NotificationType.ACTIVITYUPDATE, NotificationType.FAVADD }))
				{
					// General timeline
					mNotificationIntent = Utils.getTimelineActivityIntent(mContext, false, true);
				}
				else
				{
					// Multiple msisdn, mixed types
					// Home activity
					mNotificationIntent = IntentFactory.getHomeActivityIntent(mContext);
				}
			}
			// if all the new messages belong to a single user/group
			else
			{
				// Single msisdn, but only of type su,image post,activity,dp
				if (uniqueNotifTypes.containsOnly(new int[] { NotificationType.STATUSUPDATE, NotificationType.IMAGE_POST, NotificationType.DPUPDATE,
						NotificationType.ACTIVITYUPDATE, NotificationType.FAVADD }))
				{
					mNotificationIntent = Utils.getTimelineActivityIntent(mContext, false, true);
				}
				else if (lastAddedMsisdn.equals(mContext.getString(R.string.app_name)))
				{
					// Single msisdn, from hike team
					mNotificationIntent = new Intent(mContext, HomeActivity.class);
					mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				}
				else if (BotUtils.isBot(lastAddedMsisdn))
				{
					// Single msisdn, bot
					mNotificationIntent = IntentFactory.getIntentForBots(mContext, lastAddedMsisdn);

					if (mNotificationIntent == null)
					{
						mNotificationIntent = IntentFactory.createChatThreadIntentFromMsisdn(mContext, lastAddedMsisdn, false, false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
					}
					// Adding the notif tracker to bot notifications
					mNotificationIntent.putExtra(AnalyticsConstants.BOT_NOTIF_TRACKER, AnalyticsConstants.BOT_OPEN_SOURCE_NOTIF);
				}
				else
				{
					// Single msisdn, mixed notification types
					mNotificationIntent = IntentFactory.createChatThreadIntentFromMsisdn(mContext, lastAddedMsisdn, false, false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
				}
			}

		}

		/*
		 * notifications appear to be cached, and their .equals doesn't check 'Extra's. In order to prevent the wrong intent being fired, set a data field that's unique to the
		 * conversation we want to open. http://groups .google.com/group/android-developers/browse_thread/thread /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */
		if (mNotificationIntent != null)
		{
			mNotificationIntent.setData((Uri.parse("custom://" + getNotificationId())));
		}
	}

	
	/**
	 * Returns notification intent based on messages present in stack
	 * 
	 * @return
	 */
	public Intent getNotificationIntent()
	{
		if (mNotificationIntent == null)
		{
			updateNotificationIntent();
		}
		return mNotificationIntent;
	}

	/**
	 * TODO Improve this.
	 * 
	 * @return
	 */
	public int getNotificationIcon()
	{

		if (isFromSingleMsisdn())
		{
			return R.drawable.ic_stat_notify;
		}
		else
		{
			return R.drawable.ic_contact_logo;
		}
	}

	/**
	 * If there are multiple msisdns associated with the messages present in the stack, use a defined constant as notification id, else use the msisdn hashcode.
	 * 
	 * @return
	 */
	public int getNotificationId()
	{
		return HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID;
	}

	/**
	 * Creates big text string based on notification messages stack
	 * 
	 * @return
	 */
	public String getNotificationBigText(int retryCount)
	{
		setBigTextList(new ArrayList<SpannableString>());
		StringBuilder bigText = new StringBuilder();

		ListIterator<Entry<String, LinkedList<NotificationPreview>>> mapIterator = new ArrayList<>(mMessagesMap.entrySet()).listIterator(mMessagesMap
				.size());
		
		if (retryCount > 0)
		{
			maxRetryCount--;
		}
		while (mapIterator.hasPrevious())
		{
			Entry<String, LinkedList<NotificationPreview>> conv = mapIterator.previous();
			String msisdn = conv.getKey();

			// we are getting a concurrent modification excep here as we have created a new iterator with old reference to linklist.
			if (conv.getValue() == null)
				continue;

			List<NotificationPreview> snapShot=new LinkedList<>(conv.getValue());

			for (NotificationPreview notifPrvw :snapShot)
			{

				String notificationMsgTitle = mContext.getString(R.string.app_name);

				notificationMsgTitle = HikeNotificationUtils.getNameForMsisdn(msisdn);

				if (!isFromSingleMsisdn())
				{
					getBigTextList().add(HikeNotificationUtils.makeNotificationLine(notificationMsgTitle, notifPrvw.getMessage()));
				}
				else
				{
					getBigTextList().add(HikeNotificationUtils.makeNotificationLine(null, notifPrvw.getMessage()));
				}
			}

		}

		bigText = new StringBuilder();
		for (int i = 0; i < getBigTextList().size(); i++)
		{
			bigText.append(getBigTextList().get(i));

			if (i != getBigTextList().size() - 1)
			{
				bigText.append("\n");
			}
		}
		return bigText.toString();
	}

	/**
	 * Returns text of the latest received messages
	 * 
	 * @return
	 */
	public String getNotificationTickerText()
	{
		if (mTickerText != null)
		{
			String tickerTextString = mTickerText.toString();
			mTickerText = null;
			return tickerTextString;
		}
		else
		{
			return "";
		}
	}

	/**
	 * Returns the summary of messages present in the stack. Returns null in-case of 1 new message since
	 * then we do not want to show the message count summary at all.
	 * 
	 * @return
	 */
	public String getNotificationSubText()
	{
		if (isFromSingleMsisdn())
		{
			if (getNewMessages() <= 1)
			{
				return null;
			}
			else
			{
				return String.format(mContext.getString(R.string.num_new_messages), getNewMessages());
			}
		}
		else
		{
			return String.format(mContext.getString(R.string.num_notification_sub_text_for_multi_msisdn), getNewMessages(), getNewConversations());
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		/**
		 * Here we will determine if the user has seen the messages present in the notification stack. We remove the messages viewed from the stack so that it does not get
		 * displayed on next bulk notification update.
		 */
		if (HikePubSub.NEW_ACTIVITY.equals(type))
		{
			if (object instanceof Activity)
			{
				Activity activity = (Activity) object;
				if ((activity instanceof ChatThreadActivity))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
				}
			}
		}
	}

	/**
	 * Returns lines (List<String>) which can be used for big view summary notification
	 * 
	 * @return
	 */
	public ArrayList<SpannableString> getBigTextList()
	{
		return mBigTextList;
	}

	private void setBigTextList(ArrayList<SpannableString> bigTextList)
	{
		this.mBigTextList = bigTextList;
	}

	/**
	 * Returns number of unread messages in the conversations database
	 * 
	 * @return
	 */
	public int getUnreadMessages()
	{
		return mConvDb.getTotalUnreadMessages();
	}

	/**
	 * Returns ConvMessage object of the last message inserted into the messages stack
	 * 
	 * @return
	 */
	public ConvMessage getLastInsertedConvMessage()
	{
		return mLastInsertedConvMessage;
	}

	/**
	 * Clear all messages in the notifications stack
	 */
	public void resetMsgStack()
	{
		mMessagesMap.clear();
		lastAddedMsisdn = ""; // FIX FOR AND-4316
		totalNewMessages = 0;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.MAX_REPLY_RETRY_NOTIF_COUNT, 0);
		maxRetryCount=0;
	}

	/**
	 * Get number of total messages present in stack
	 * 
	 * @return
	 */
	public int getSize()
	{
		return totalNewMessages;
	}

	/**
	 * Check whether the notification messages stack is empty
	 * 
	 * @return
	 */
	public boolean isEmpty()
	{
		if (getSize() == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Return time-stamp of latest notification received. Used for summary views.
	 * 
	 * @return Latest notification added time-stamp
	 */
	public long getLatestAddedTimestamp()
	{
		return latestAddedTimestamp;
	}

	/**
	 * Provides the number of new messages present in the cached messages stack
	 * 
	 * @return Number of new messages
	 */
	public int getNewMessages()
	{
		return totalNewMessages;
	}

	/**
	 * Provides the number of new conversations present in the cached messages stack
	 * 
	 * @return Number of new conversations
	 */
	public int getNewConversations()
	{
		return mMessagesMap.size();
	}

	/**
	 * Returns notification title based on unique msisdns present in messages stack
	 * 
	 * @return Notification title string
	 */
	public String getNotificationTitle()
	{
		if (isFromSingleMsisdn())
		{
			String title = mMessagesMap.get(lastAddedMsisdn).getLast().getTitle();
			
			if(getNewMessages() <=1 && !TextUtils.isEmpty(title))
			{
				return title;
			}
			
			return HikeNotificationUtils.getNameForMsisdn(lastAddedMsisdn);
		}

		return mContext.getString(R.string.app_name);
	}

	public boolean forceBlockNotificationSound()
	{
		return forceBlockNotificationSound;
	}

	public void setTickerText(StringBuilder mTickerText)
	{
		this.mTickerText = mTickerText;
	}

	public boolean containsStealthMessage()
	{
		if (mMessagesMap.keySet().contains(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
/**
 * 	
 * @param notificationType
 * @return int
 * 
 * This function returns the notification count wrt notifivation type
 */
	private int getNotificationCount(int notificationType)
	{
		int retryCount = 0;

		String str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NOTIFICATION_RETRY_JSON, "{}");

		mmCountModel = new Gson().fromJson(str, NotificationRetryCountModel.class);
		switch (notificationType)
		{
		case NotificationType.STATUSUPDATE:
			retryCount = mmCountModel.getStatusUpdate();
			break;
			case NotificationType.BOTMSG:
				retryCount = mmCountModel.getHikeBot();
				break;
			case NotificationType.CHATTHEMECHNG:
				retryCount = mmCountModel.getChatThemeChange();
				break;
			case NotificationType.DPUPDATE:
				retryCount = mmCountModel.getDpUpdate();
				break;

			case NotificationType.FAVADD:
				retryCount = mmCountModel.getFav();
				break;

			case NotificationType.H2O:
				retryCount = mmCountModel.getH2o();
				break;

			case NotificationType.NORMALGC:
				retryCount = mmCountModel.getGc();
				break;

			case NotificationType.NORMALMSG1TO1:
				retryCount = mmCountModel.getH2h();
				break;

			case NotificationType.NUJORRUJ:
				retryCount = mmCountModel.getNuj();
				break;

			case NotificationType.OTHER:
				retryCount = mmCountModel.getOther();
				break;

			case NotificationType.HIDDEN:
				retryCount = mmCountModel.getHidden();
				break;
				
			case NotificationType.ACTIVITYUPDATE:
				retryCount = mmCountModel.getAcUp();
				break;
			}
		
		return retryCount;
		
		
	}

/**
 * This function is called before retrying to remove all the unwanted message that we dont want to retry to avoid spamming.
 * 
 * If the notification count is -1 ,it should not  be included in retry. 
 */
	public  void processPreNotificationWork()
	{
		ListIterator<Entry<String, LinkedList<NotificationPreview>>> mapIterator = new ArrayList<>(mMessagesMap.entrySet())
				.listIterator(mMessagesMap.size());

		LinkedList<NotificationPreview> keySet = null;

		while (mapIterator.hasPrevious())
		{
			Entry<String, LinkedList<NotificationPreview>> conv = mapIterator.previous();
			String msisdn = conv.getKey();

			if (keySet == null)
			{
				keySet = new LinkedList<>();
			}
			List<NotificationPreview> snapShot=new LinkedList<>();
			if (conv.getValue() != null)
				snapShot.addAll(conv.getValue());

			for (NotificationPreview notifPrvw : snapShot)
			{

				if (getNotificationCount(notifPrvw.getNotificationType()) == -1)
				{
					totalNewMessages -= 1;
					keySet.add(notifPrvw);
					// remove from the stack;
				}

			}
			for (NotificationPreview notificationPreview : keySet)
			{
				mMessagesMap.get(msisdn).remove(notificationPreview);
			}

			if (mMessagesMap.get(msisdn).size() == 0)
			{
				mMessagesMap.remove(msisdn);
			}

			if (keySet.size() > 0)
			{
				keySet.clear();
			}
		}

	}
	
	@SuppressWarnings("serial")
	private class ExtendedHashSet extends HashSet<Integer>
	{
		public boolean containsOnly(Object object) //Checks if set has no other values than the input array
		{
			if (object instanceof int[])
			{
				int[] intArray = (int[]) object;
				for (int i : this)
				{
					boolean found = false;
					for (int j : intArray)
					{
						if (i == j)
						{
							found = true;
							break;
						}
					}

					if (!found)
					{
						return false;
					}
				}
				return true;
			}
			else
			{
				return super.contains(object);
			}
		}
		
		@Override
		public boolean equals(Object object) // Checks if set has EXACTLY ONLY the input 1. Integer 2. Integer Array 3. Set
		{
			if (object == this)
			{
				return true;
			}

			if (object instanceof Set)
			{
				@SuppressWarnings("rawtypes")
				Collection c = (Collection) object;
				if (c.size() != size())
				{
					return false;
				}
				try
				{
					return containsAll(c);
				}
				catch (ClassCastException unused)
				{
					return false;
				}
				catch (NullPointerException unused)
				{
					return false;
				}
			}
			else if (object instanceof Integer)
			{
				if(size() != 1)
				{
					return false;
				}
				
				return contains(object);
			}
			else if (object instanceof int[])
			{
				int[] intArray = (int[]) object;

				if (intArray.length != size())
				{
					return false;
				}
				
				for (int i : intArray)
				{
					if (!contains(i))
					{
						return false;
					}
				}
				return true;
			}

			return false;
		}
	}
}
