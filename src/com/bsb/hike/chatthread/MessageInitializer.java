package com.bsb.hike.chatthread;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import com.bsb.hike.utils.Logger;

import java.lang.ref.WeakReference;

/**
 * TODO
 */
public class MessageInitializer extends AsyncTaskLoader<Object> {

    private static final String LOGTAG = MessageInitializer.class.getSimpleName();

    static final String START_INDX = "startIndex";
    static final String END_INDX = "endIndex";

    private WeakReference<ChatThread> weakChatThread;
    private int startIndex;
    private int endIndex;
    private boolean taskComplete = false;

    public MessageInitializer(final Context context, final ChatThread chatThread, final Bundle bundle) {
        super(context);
        Logger.i(LOGTAG, "MessageInitializer loader object");
        this.weakChatThread = new WeakReference<>(chatThread);
        this.startIndex = bundle.getInt(START_INDX);
        this.endIndex = bundle.getInt(END_INDX);
    }

    @Override
    public Object loadInBackground() {
        Logger.i(LOGTAG, "Load in background of conversation loader");

        ChatThread chatThread = weakChatThread.get();
        if (chatThread != null) {
            chatThread.getMessagesFromDB(chatThread.messages, startIndex, endIndex);
        }
        taskComplete = true;
        return null;
    }

    /**
     * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
     */
    protected void onStartLoading() {
        Logger.i(LOGTAG, "Conversation loader onStartLoading");
        if (taskComplete) {
            deliverResult(null);
        } else {
            forceLoad();
        }
    }
}
