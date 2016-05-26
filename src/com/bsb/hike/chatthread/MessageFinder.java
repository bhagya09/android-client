package com.bsb.hike.chatthread;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MovingList;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class MessageFinder extends AsyncTaskLoader<Object> {

    private static final String LOGTAG = MessageFinder.class.getSimpleName();
    private static final int MaxMsgLoadCount = 3200;
    protected int loaderId;
    private int position = -2;
    private int loadMessageCount = 50;
    private boolean taskComplete = false;

    private WeakReference<ChatThread> weakChatThread;

    public MessageFinder(final Context context, final int loaderId, final ChatThread chatThread) {
        super(context);
        Logger.i(LOGTAG, "Message finder object " + loaderId);
        this.loaderId = loaderId;
        this.weakChatThread = new WeakReference<>(chatThread);
    }

    @Override
    public Object loadInBackground() {
        Logger.i(LOGTAG, "Search in background: " + loaderId);

        ChatThread chatThread = weakChatThread.get();
        if (chatThread != null && !chatThread.isMessageListEmpty()) {
            chatThread.loadingMoreMessages = true;
            int msgSize = chatThread.messages.size();
            int firstVisibleItem = chatThread.mConversationsView.getFirstVisiblePosition();
            if (firstVisibleItem < msgSize - 1) {
                firstVisibleItem++;
            }
            List<ConvMessage> msgList;
            if (loaderId == ChatThread.SEARCH_PREVIOUS || loaderId == ChatThread.SEARCH_LOOP) {
                long maxId = chatThread.messages.getUniqueId(firstVisibleItem);
                long minId = -1;
                List<Long> ids = new ArrayList<>();
                //position = messageSearchManager.searchFirstItem(chatThread.messages, firstVisibleItem - 1, 0, chatThread.searchText);
                while (position < 0 && ChatThread.messageSearchManager.isActive()) {
                    msgList = new ArrayList<>(chatThread.loadMoreMessages(loadMessageCount, maxId, minId));
                    if (msgList == null || msgList.isEmpty() || !ChatThread.messageSearchManager.isActive()) {
                        break;
                    }
                    position = ChatThread.messageSearchManager.searchFirstItem(msgList, msgList.size(), 0, chatThread.searchText);
                    ids.addAll(0, MovingList.getIds(msgList));
                    if (position >= 0) {
                        Logger.d(LOGTAG, "Found at pos: " + position + ", id:" + msgList.get(position).getSortingId());
                        int start = Math.max(position - HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, 0);
                        int end = Math.min(position + HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, msgList.size() - 1);
                        List<ConvMessage> toBeAddedList = new ArrayList<>(ids.size());
                        Utils.preFillArrayList(toBeAddedList, ids.size());
                        for (int i = start; i <= end; i++) {
                            toBeAddedList.set(i, msgList.get(i));
                        }
                        MovingList<ConvMessage> movingList = new MovingList<ConvMessage>(toBeAddedList, ids, chatThread.mOnItemsFinishedListener);
                        movingList.setLoadBufferSize(HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME);
                        chatThread.sendUIMessage(chatThread.UPDATE_MESSAGE_LIST, new Pair<>(movingList, firstVisibleItem));
                    } else {
                        //No need to load more than 3200 messaging in one go.
                        if (loadMessageCount < MaxMsgLoadCount) {
                            loadMessageCount *= 2;
                        }
                        maxId = msgList.get(0).getSortingId();
                    }
                }
                if (loaderId == ChatThread.SEARCH_LOOP && position < 0) {
                    Logger.d(LOGTAG, "Shifting to next");
                    loaderId = ChatThread.SEARCH_NEXT;
                }
            }
            if (loaderId == ChatThread.SEARCH_NEXT) {
                msgSize = chatThread.messages.size();
                int count = firstVisibleItem;
                long minId = chatThread.messages.getUniqueId(firstVisibleItem);
                int maxIdPosition = Math.min(count + loadMessageCount, msgSize - 1);
                long maxId = chatThread.messages.getUniqueId(maxIdPosition);
                while (position < 0 && ChatThread.messageSearchManager.isActive()) {
                    Logger.d(LOGTAG, "loadmoremessages for search: " + loadMessageCount + " " + maxId + " " + minId);
                    msgList = new ArrayList<>(chatThread.loadMoreMessages(loadMessageCount, maxId + 1, minId));
                    if (msgList == null || msgList.isEmpty() || !ChatThread.messageSearchManager.isActive()) {
                        break;
                    }
                    position = ChatThread.messageSearchManager.searchFirstItem(msgList, 0, msgList.size(), chatThread.searchText);
                    if (position >= 0) {
                        Logger.d(LOGTAG, "found at pos: " + position + ", id:" + msgList.get(position).getSortingId());
                        count += (position + 1);
                        position = count;
                    } else {
                        count += msgList.size();
                        //No need to load more than 3200 messaging in one go.
                        if (loadMessageCount < MaxMsgLoadCount) {
                            loadMessageCount *= 2;
                        }
                        minId = msgList.get(msgList.size() - 1).getSortingId();
                        maxIdPosition = Math.min(count + loadMessageCount, msgSize - 1);
                        maxId = chatThread.messages.getUniqueId(maxIdPosition);
                    }
                }
            }
        }
        taskComplete = true;
        Logger.d(LOGTAG, "found at position: " + position);
        return position;
    }

    /**
     * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
     */
    protected void onStartLoading() {
        Logger.i(LOGTAG, "message finder onStartLoading");
        // The search manager returns the values greater than equal to -1
        // So if the loader has executed, the result is always greater than -2.
        // Else we need to start the loader.
        if (taskComplete) {
            deliverResult(position);
        } else {
            forceLoad();
        }
    }
}

