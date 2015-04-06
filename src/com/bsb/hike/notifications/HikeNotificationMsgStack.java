package com.bsb.hike.notifications;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.NotificationPreview;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.IntentManager;

/**
 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
 * 
 * @author Atul
 * 
 */
public class HikeNotificationMsgStack implements Listener
{

	private static HikeNotificationMsgStack mHikeNotifMsgStack;

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

	public String lastAddedMsisdn;

	private long latestAddedTimestamp;

	private final int MAX_LINES = 7;

	private int totalNewMessages;

	private boolean forceBlockNotificationSound;

	private static void init(Context context)
	{
		if (mHikeNotifMsgStack == null)
		{
			mContext = context.getApplicationContext();
			mHikeNotifMsgStack = new HikeNotificationMsgStack();

			// We register for NEW_ACTIVITY so that when a chat thread is opened,
			// all unread notifications against the msisdn can be cleared
			HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, mHikeNotifMsgStack);
		}
	}

	/**
	 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
	 * 
	 * @param argContext
	 * @return
	 */
	public static HikeNotificationMsgStack getInstance(Context argContext)
	{
		init(argContext);
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
		this.mConvDb = HikeConversationsDatabase.getInstance();
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
		}

		forceBlockNotificationSound = argConvMessage.isSilent();

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
					addMessage(conv.getMsisdn(), conv.webMetadata.getNotifText());
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
	 * @throws IllegalArgumentException
	 */
	public void addMessage(String argMsisdn, String argMessage) throws IllegalArgumentException
	{
		if (TextUtils.isEmpty(argMessage))
		{
			Log.wtf("HikeNotification", "Notification message is empty, check packet, msisdn= "+argMsisdn);
			return;
		}
		addPair(argMsisdn, new NotificationPreview(argMessage, HikeNotificationUtils.getNameForMsisdn(argMsisdn)));
	}

	/**
	 * Adds msisdn-message pair into the notification messages stack. Also performs sorting/grouping/trimming of the existing data
	 * 
	 * @param argMsisdn
	 * @param argMessage
	 */
	private void addPair(String argMsisdn, NotificationPreview notifPrvw)
	{
		lastAddedMsisdn = argMsisdn;

		// If message stack consists of any stealth messages, do not add new message, change
		// stealth message string to notify multiple messages (add s to message"S")
		if (argMsisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY) && mMessagesMap.containsKey(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{

			LinkedList<NotificationPreview> stealthMessageList = mMessagesMap.get(argMsisdn);

			// There should only be 1 item dedicated to stealth message
			// hike - You have new notification(s)
			stealthMessageList.set(0, new NotificationPreview(mContext.getString(R.string.stealth_notification_messages), null));
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

		// If new messages belong to different users/groups, redirect the user
		// to conversations list
		if (!isFromSingleMsisdn() || containsStealthMessage())
		{
			mNotificationIntent = new Intent(mContext, HomeActivity.class);
			mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		// if all the new messages belong to a single user/group
		// we've got to invoke the chat thread from here with the respective
		// users
		else
		{
			if (lastAddedMsisdn.equals(mContext.getString(R.string.app_name)))
			{
				mNotificationIntent = new Intent(mContext, HomeActivity.class);
				mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			}
			else
			{

				mNotificationIntent = IntentManager.getChatThreadIntent(mContext, lastAddedMsisdn);

				/*
				 * notifications appear to be cached, and their .equals doesn't check 'Extra's. In order to prevent the wrong intent being fired, set a data field that's unique to
				 * the conversation we want to open. http://groups .google.com/group/android-developers/browse_thread/thread /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
				 */
				mNotificationIntent.setData((Uri.parse("custom://" + getNotificationId())));
			}
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
	public String getNotificationBigText()
	{
		setBigTextList(new ArrayList<SpannableString>());
		StringBuilder bigText = new StringBuilder();

		ListIterator<Entry<String, LinkedList<NotificationPreview>>> mapIterator = new ArrayList<Map.Entry<String, LinkedList<NotificationPreview>>>(mMessagesMap.entrySet()).listIterator(mMessagesMap
				.size());

		while (mapIterator.hasPrevious())
		{
			Entry<String, LinkedList<NotificationPreview>> conv = mapIterator.previous();

			String msisdn = conv.getKey();

			for (NotificationPreview notifPrvw : conv.getValue())
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
			return String.format(mContext.getString(R.string.num_new_conversations), getNewConversations());
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
				if ((activity instanceof ChatThread))
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
		lastAddedMsisdn = null;
		totalNewMessages = 0;
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

		return String.format(mContext.getString(R.string.num_new_messages), getNewMessages());
	}

	public boolean forceBlockNotificationSound()
	{
		return forceBlockNotificationSound;
	}

	public void setTickerText(StringBuilder mTickerText)
	{
		this.mTickerText = mTickerText;
	}

	private boolean containsStealthMessage()
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
}
