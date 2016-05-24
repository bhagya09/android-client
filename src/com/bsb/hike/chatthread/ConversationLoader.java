package com.bsb.hike.chatthread;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * TODO - Return something saner than Object from loadInBackground
 */
public class ConversationLoader extends AsyncTaskLoader<Object> {

    private static final String LOGTAG = ConversationLoader.class.getSimpleName();

    protected int loaderId;
    private Conversation conversation;
    private List<ConvMessage> list;
    private WeakReference<ChatThread> weakChatThread;

    public ConversationLoader(final Context context, final int loaderId, final ChatThread weakChatThread) {
        super(context);
        Logger.i(LOGTAG, "Conversation loader object " + loaderId);
        this.loaderId = loaderId;
        this.weakChatThread = new WeakReference<>(weakChatThread);
    }

    @Override
    public Object loadInBackground() {
        Logger.i(LOGTAG, "load in background of conversation loader");

        ChatThread chatThread = weakChatThread.get();
        if (chatThread != null) {
            if (loaderId == ChatThread.FETCH_CONV) {
                return conversation = chatThread.fetchConversation();
            } else {
                if(loaderId == ChatThread.LOAD_MORE_MESSAGES) {
                    return list = chatThread.loadMoreMessages();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
     */
    protected void onStartLoading() {
        Logger.i(LOGTAG, "Conversation loader onStartLoading");
        if (loaderId == ChatThread.FETCH_CONV && conversation != null) {
            deliverResult(conversation);
        } else if (loaderId == ChatThread.LOAD_MORE_MESSAGES && list != null) {
            deliverResult(list);
        } else {
            forceLoad();
        }
    }
}


