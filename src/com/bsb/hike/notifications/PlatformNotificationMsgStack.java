package com.bsb.hike.notifications;

import android.content.Context;
import android.text.SpannableString;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

import com.bsb.hike.models.PlatformNotificationPreview;

import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class keeps a stack of notification model {@link PlatformNotificationPreview} as per individual micro-app msisdn.
 */
public class PlatformNotificationMsgStack
{
	private static volatile PlatformNotificationMsgStack platformNotifMsgStack;

	private final LinkedHashMap<String, LinkedList<PlatformNotificationPreview>> mMessagesMap;

	private final static int MAX_LINES = 4;

	private final Context mContext;

	private String msisdn;

	private final int MAX_DISTINCT_NOTIFS = 7;

    private static final String TAG = PlatformNotificationMsgStack.class.getCanonicalName();

	public static PlatformNotificationMsgStack getInstance()
	{
		if (platformNotifMsgStack == null)
			synchronized (PlatformNotificationMsgStack.class)
			{
				if (platformNotifMsgStack == null)
				{
					Logger.d(TAG, "HikeNotificationMsgStack");
					platformNotifMsgStack = new PlatformNotificationMsgStack();
				}
			}
		return platformNotifMsgStack;
	}

	private PlatformNotificationMsgStack()
	{

		mMessagesMap = new LinkedHashMap<String, LinkedList<PlatformNotificationPreview>>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, LinkedList<PlatformNotificationPreview>> eldest)
			{
				return size() > MAX_DISTINCT_NOTIFS;
			}
		};
		Logger.d(TAG, "PlatformNotificationMsgStack....size is " + mMessagesMap.size());
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
	}

	public void addNotif(PlatformNotificationPreview notif)
	{
		if (notif == null)
		{
			throw new IllegalArgumentException("notification model cannot be null");
		}
		addNotifToMap(notif);

	}

	private void addNotifToMap(PlatformNotificationPreview notif)
	{
		msisdn = notif.getMsisdn();
		LinkedList list = mMessagesMap.get(notif.getMsisdn());
		if (list != null)
		{
			list.add(notif);
			if (!isStackSizeOne())
			{
				// Move the conversation map to first index
				LinkedList<PlatformNotificationPreview> lastModifiedMapList = mMessagesMap.remove(notif.getMsisdn());
				mMessagesMap.put(msisdn, lastModifiedMapList);
			}
		}
		else
		{
			list = new LinkedList();
			list.add(notif);
			mMessagesMap.put(notif.getMsisdn(), list);
		}
		trimMessageMap(msisdn);
	}

	public boolean isStackSizeOne()
	{
		return mMessagesMap.size() == 1 ? true : false;
	}

	private void trimMessageMap(String argMsisdn)
	{
		LinkedList<PlatformNotificationPreview> ll = mMessagesMap.get(argMsisdn);
		if (ll == null)
		{
			return;
		}
		if (ll.size() > MAX_LINES)
		{
			ll.removeFirst();
		}
	}

	/**
	 * Clear all messages in the notifications stack
	 */
	public void resetMsgStack()
	{
		mMessagesMap.clear();
		msisdn = "";
	}

	public String getTickerTextForMsisdn(String msisdn)
	{
		return String.format(mContext.getResources().getString(R.string.num_new_messages), getMessageCountForMsisdn(msisdn));
	}

	public int getMessageCountForMsisdn(String msisdn)
	{
        List list = mMessagesMap.get(msisdn);
        if(list == null){
            return 0;
        }
		return list.size();
	}

	/**
	 * Creates big text string based on notification messages stack
	 *
	 * @return
	 */

	public List<SpannableString> getBigTextList(String argMsisdn)
	{
		List<SpannableString> customBigTextList = new ArrayList<>();
		LinkedList<PlatformNotificationPreview> ll = mMessagesMap.get(argMsisdn);
		if (ll == null)
		{
			return customBigTextList;
		}
		ListIterator<PlatformNotificationPreview> listIterator = ll.listIterator(ll.size());
		while (listIterator.hasPrevious())
		{
			customBigTextList.add(HikeNotificationUtils.makeNotificationLine(null, listIterator.previous().getSubText()));
		}
		return customBigTextList;
	}
}
