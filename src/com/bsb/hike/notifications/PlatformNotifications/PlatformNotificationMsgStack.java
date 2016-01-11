package com.bsb.hike.notifications.platformNotifications;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.SpannableString;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.PlatformNotificationPreview;

import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.notifications.HikeNotificationUtils;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class keeps a stack of notification model {@link PlatformNotificationPreview} as per individual micro-app msisdn.
 */
public class PlatformNotificationMsgStack
{
    private static volatile PlatformNotificationMsgStack platformNotifMsgStack;

    private final LinkedHashMap<String, LinkedList<PlatformNotificationPreview>> mMessagesMap;

    private final static int MAX_LINES = 4;

    private final Context mContext;

    private StringBuilder mTickerText;

    private String msisdn;

    private final int MAX_DISTINCT_NOTIFS = 7;

    public static PlatformNotificationMsgStack getInstance()
    {
        if (platformNotifMsgStack == null)
            synchronized (PlatformNotificationMsgStack.class)
            {
                if (platformNotifMsgStack == null)
                {
                    Logger.d("notification", "HikeNotificationMsgStack");
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
        Logger.d("notification", "PlatformNotificationMsgStack....size is " + mMessagesMap.size());
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
        totalNewMessages++;
        if (list != null)
        {
            list.add(notif);
            if (!isFromSingleMsisdn())
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
        if (mTickerText != null)
        {
            mTickerText.append("\n" + HikeNotificationUtils.getNameForMsisdn(msisdn) + " - " + notif.getBody());
        }
        else
        {
            mTickerText = new StringBuilder();
            mTickerText.append(HikeNotificationUtils.getNameForMsisdn(msisdn) + " - " + notif.getBody());
        }
    }

    public boolean isFromSingleMsisdn()
    {
        return mMessagesMap.size() == 1 ? true : false;
    }

    private void trimMessageMap(String argMsisdn)
    {
        LinkedList<PlatformNotificationPreview> ll = mMessagesMap.get(argMsisdn);
        if(ll== null)
        {
            return;
        }
       if(ll.size() > MAX_LINES)
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
        totalNewMessages = 0;
    }

    public String getTickerTextForMsisdn(String msisdn)
    {
        return String.format(mContext.getResources().getString(R.string.notifTickerText), getMessageCountforMsisdn(msisdn));
    }

    public int getMessageCountforMsisdn(String msisdn)
    {
        return mMessagesMap.get(msisdn).size();
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
            customBigTextList.add(HikeNotificationUtils.makeNotificationLine(null, listIterator.previous().getBody()));
        }
        return customBigTextList;
    }

    public String getStringFromList(List<SpannableString> list)
    {
        StringBuilder bigText = new StringBuilder();
        for (int i = 0; i < list.size(); i++)
        {
            bigText.append(list.get(i));

            if (i != list.size() - 1)
            {
                bigText.append("\n");
            }
        }
        return bigText.toString();
    }
}
