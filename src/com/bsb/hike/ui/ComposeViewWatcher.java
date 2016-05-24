package com.bsb.hike.ui;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.CustomKeyboardManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ComposeViewWatcher extends EmoticonTextWatcher implements Runnable, Listener
{
	//change this to read from sharedpref
	private static boolean useWTRevamped;

	private Conversation mConversation;

	private long mTextLastChanged = 0;

	private Handler mUIThreadHandler;

	private HikePubSub mPubSub;

	boolean mInitialized;

	private ImageButton mButton, audButton;

	private EditText mComposeView;

	private int mCredits;

	private Context context;

	public ComposeViewWatcher(Conversation conversation, EditText composeView, ImageButton sendButton, ImageButton audioButton, int initialCredits, Context context)
	{
		this.mConversation = conversation;
		this.mUIThreadHandler = new Handler();
		this.mPubSub = HikeMessengerApp.getPubSub();
		this.mComposeView = composeView;
		this.mButton = sendButton;
		this.audButton = audioButton;
		this.mCredits = initialCredits;
		this.context = context;
		useWTRevamped = ChatThreadUtils.isWT1RevampEnabled(context);
		setBtnEnabled();
	}

	public void init()
	{
		if (mInitialized)
		{
			return;
		}

		mInitialized = true;
		mPubSub.addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		mComposeView.addTextChangedListener(this);
	}

	public void releaseResources()
	{
		mPubSub.removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		mUIThreadHandler.removeCallbacks(this);
		mComposeView.removeTextChangedListener(this);
		mSendBtnListener = null;
		mInitialized = false;
	}

    public void setBtnEnabled()
    {
        CharSequence seq = mComposeView.getText();
		/*
		 * the button is enabled iff there is text AND (this is an IP conversation or we have credits available)
		 */
        boolean canSend = (!TextUtils.isEmpty(seq) && ((mConversation.isOnHike() || mCredits > 0)));
        if (!mConversation.isOnHike() && mCredits <= 0)
        {
            boolean nativeSmsPref = Utils.getSendSmsPref(context);
            canSend = nativeSmsPref;
        }

        if (!canSend)
        {
            if (BotUtils.isBot(mConversation.getMsisdn()) && CustomKeyboardManager.getInstance().shouldShowInputBox(mConversation.getMsisdn()))
            {
                mButton.setImageResource(R.drawable.keyboard_button_selector);
                mButton.setContentDescription(context.getResources().getString(R.string.content_des_send_recorded_audio_text_chatting));
            }
            else
            {
                mButton.setImageResource(R.drawable.walkie_talkie_btn_selector);
                mButton.setContentDescription(context.getResources().getString(R.string.content_des_send_recorded_audio_text_chatting));
            }
        }
        else
        {
            mButton.setImageResource(R.drawable.send_btn_selector);
            mButton.setContentDescription(context.getResources().getString(R.string.content_des_send_message_button));
        }

        if (mConversation instanceof OneToNConversation)
        {
            mButton.setEnabled(((OneToNConversation) mConversation).isConversationAlive());
        }
        else
        {
            mButton.setEnabled(true);
        }
    }

	private void setSendButton(boolean canSend){
		if(!useWTRevamped) {
			if (!canSend) {
				mButton.setImageResource(R.drawable.walkie_talkie_btn_selector);
				mButton.setContentDescription(context.getResources().getString(R.string.content_des_send_recorded_audio_text_chatting));
			} else {
				mButton.setImageResource(R.drawable.send_btn_selector);
				mButton.setContentDescription(context.getResources().getString(R.string.content_des_send_message_button));
			}
		} else {
			if (!canSend)
			{
				audButton.setVisibility(View.VISIBLE);
				mButton.setVisibility(View.GONE);
				if(mSendBtnListener != null) mSendBtnListener.onSendBtnChanged(false);
			}
			else
			{
				audButton.setVisibility(View.GONE);
				mButton.setVisibility(View.VISIBLE);
				if(mSendBtnListener != null) mSendBtnListener.onSendBtnChanged(true);
			}
		}
	}
	public void onTextLastChanged()
	{
		if (!mInitialized)
		{
			Logger.d("ComposeViewWatcher", "not initialized");
			return;
		}

		long lastChanged = System.currentTimeMillis();

		if (mTextLastChanged == 0 || (lastChanged - mTextLastChanged > HikeConstants.RESEND_TYPING_TIME))
		{
			// we're currently not in 'typing' mode
			mTextLastChanged = lastChanged;

			// fire an event
			if(!(mConversation instanceof BroadcastConversation))
			{
				HikeMqttManagerNew.getInstance().sendMessage(mConversation.serialize(HikeConstants.MqttMessageTypes.START_TYPING), MqttConstants.MQTT_QOS_ZERO);
			}
			// create a timer to clear the event
			mUIThreadHandler.removeCallbacks(this);

			mUIThreadHandler.postDelayed(this, HikeConstants.LOCAL_CLEAR_TYPING_TIME);
		}
	}

	public void onMessageSent()
	{
		/*
		 * a message was sent, so reset the typing notifications so they're sent for any subsequent typing
		 */
		mTextLastChanged = 0;
		mUIThreadHandler.removeCallbacks(this);
	}

	@Override
	public void run()
	{
		long current = System.currentTimeMillis();
		if (current - mTextLastChanged >= HikeConstants.LOCAL_CLEAR_TYPING_TIME)
		{
			/* text hasn't changed in 10 seconds, send an event */
			sendEndTyping();
		}
		else
		{
			/* text has changed, fire a new event */
			long delta = HikeConstants.LOCAL_CLEAR_TYPING_TIME - (current - mTextLastChanged);
			mUIThreadHandler.postDelayed(this, delta);
		}
	}

	public boolean wasEndTypingSent()
	{
		return mTextLastChanged == 0;
	}

	public void sendEndTyping()
	{
		mTextLastChanged = 0;
	}

	@Override
	public void afterTextChanged(Editable editable)
	{
		if (!TextUtils.isEmpty(editable))
		{
			onTextLastChanged();
		}
		setBtnEnabled();
		super.afterTextChanged(editable);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		super.beforeTextChanged(s, start, count, after);
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		super.onTextChanged(s, start, before, count);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			mCredits = ((Integer) object).intValue();
		}
	}

	/* Begin CE-487: FTUE red-dot of WT also appears on send message button
	 * Listener for sending callback to the chatthread when we toggle between send msg & audio
	 */
	public interface SendBtnChangedListener {
		public void onSendBtnChanged(boolean enabled);
	}

	private SendBtnChangedListener mSendBtnListener = null;

	public void setSendBtnChangeListener(SendBtnChangedListener listener) {
		mSendBtnListener = listener;
	}
	//End CE-487
}
