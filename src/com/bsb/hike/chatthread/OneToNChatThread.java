package com.bsb.hike.chatthread;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Message;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.utils.HashSpanWatcher;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public abstract class OneToNChatThread extends ChatThread implements HashTagModeListener
{
	protected static final int SHOW_IMP_MESSAGE = 205;

	protected static final int GROUP_REVIVED = 206;

	private static final int PARTICIPANT_JOINED_OR_LEFT_CONVERSATION = 207;
	
	protected static final int BULK_MESSAGE_RECEIVED = 208;

	private static final String TAG = "onetonchatthread";

	protected HashSpanWatcher mHashSpanWatcher;

	protected OneToNConversation oneToNConversation;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public OneToNChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeEnded(String parameter)
	{
		// TODO implement me
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeStarted(String parameter)
	{
		// TODO implement me
	}

	/**
	 * Returns whether the group is mute or not
	 * 
	 * @return
	 */
	protected boolean isMuted()
	{
		/**
		 * Defensive check
		 */

		if (oneToNConversation == null)
		{
			return false;
		}
		return oneToNConversation.isMuted();
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.chat_theme:
			showThemePicker();
			break;
		default:
			Logger.d(TAG, "Calling super Class' itemClicked");
			super.itemClicked(item);
		}
	}

	/**
	 * NON UI
	 */
	@Override
	protected Conversation fetchConversation()
	{
		Logger.i(TAG, "fetch group conversation " + Thread.currentThread().getName());
		
		if (mConversation == null)
		{
			/* the user must have deleted the chat. */
			Message message = Message.obtain();
			message.what = SHOW_TOAST;
			message.arg1 = R.string.invalid_group_chat;
			uiHandler.sendMessage(message);
			return null;
		}

		// Set participant read by list
		Pair<String, Long> pair = HikeConversationsDatabase.getInstance().getReadByValueForGroup(oneToNConversation.getMsisdn());
		if (pair != null)
		{
			String readBy = pair.first;
			long msgId = pair.second;
			oneToNConversation.setupReadByList(readBy, msgId);
		}

		// fetch theme
		ChatTheme currentTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d("ChatThread", "Calling setchattheme from createConversation");
		oneToNConversation.setChatTheme(currentTheme);

		oneToNConversation.setBlocked(ContactManager.getInstance().isBlocked(oneToNConversation.getConversationOwner()));

		return oneToNConversation;
	}

	@Override
	protected int getContentView()
	{
		return R.layout.chatthread;
	}
	
	/**
	 * Called from {@link ChatThread}'s {@link #onMessageReceived(Object)}, to handle System messages like User joined group, user left group etc.}
	 * 
	 */

	@Override
	protected void handleSystemMessages()
	{
		ContactManager conMgr = ContactManager.getInstance();
		oneToNConversation.setConversationParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), false, false));
	}
	
	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		super.addMessage(convMessage);
	}
	
	/**
	 * This overrides : {@link ChatThread}'s {@link #setTypingText(boolean, TypingNotification)}
	 */

	@Override
	protected void setTypingText(boolean direction, TypingNotification typingNotification)
	{
		if (direction)
		{
			super.setTypingText(direction, typingNotification);
		}

		else
		{
			if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
			{
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) messages.get(messages.size() - 1).getTypingNotification();
				if (groupTypingNotification.getGroupParticipantList().isEmpty())
				{
					messages.remove(messages.size() - 1);
				}

				mAdapter.notifyDataSetChanged();
			}
		}
	}

	protected void onMessageRead(Object object)
	{
		Pair<String, Pair<Long, String>> pair = (Pair<String, Pair<Long, String>>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first) || messages == null || messages.isEmpty())
		{
			return;
		}
		Long mrMsgId = pair.second.first;
		for (int i = messages.size() - 1; i >= 0; i--)
		{
			ConvMessage msg = messages.get(i);
			if (msg != null && msg.isSent())
			{
				long id = msg.getMsgID();
				if (id > mrMsgId)
				{
					continue;
				}
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
					removeFromMessageMap(msg);
				}
				else
				{
					break;
				}
			}
		}
		String participant = pair.second.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		oneToNConversation.updateReadByList(participant, mrMsgId);
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.ONETON_MESSAGE_DELIVERED_READ:
			onMessageRead(object);
			break;
		case HikePubSub.CONV_META_DATA_UPDATED:
			onConvMetadataUpdated(object);
			break;
		case HikePubSub.CONVERSATION_REVIVED:
			onGroupRevived(object);
			break;
		case HikePubSub.PARTICIPANT_JOINED_ONETONCONV:
			onParticipantJoinedOrLeftGroup(object, true);
			break;
		case HikePubSub.PARTICIPANT_LEFT_ONETONCONV:
			onParticipantJoinedOrLeftGroup(object, false);
			break;
		case HikePubSub.BULK_MESSAGE_RECEIVED:
			onBulkMessageReceived(object);
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in Group ChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
		}
	}

	/**
	 * Performs tasks on the UI thread.
	 */
	@Override
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_AVATAR:
			setAvatar(R.drawable.ic_default_avatar_group);
			break;
		case PARTICIPANT_JOINED_OR_LEFT_CONVERSATION:
			incrementGroupParticipants((int) msg.obj);
			break;
		case GROUP_REVIVED:
			handleGroupRevived();
			break;
		case BULK_MESSAGE_RECEIVED:
			addBulkMessages((LinkedList<ConvMessage>) msg.obj);
			break;
		default:
			Logger.d(TAG, "Did not find any matching event in Group ChatThread. Calling super class' handleUIMessage");
			super.handleUIMessage(msg);
			break;
		}
	}
	
	/**
	 * This method is called on the UI thread
	 * 
	 */
	private void handleGroupRevived()
	{
		toggleGroupLife(true);
	}
	
	protected void toggleGroupLife(boolean alive)
	{
		oneToNConversation.setConversationAlive(alive);
		activity.findViewById(R.id.send_message).setEnabled(alive);
		activity.findViewById(R.id.msg_compose).setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		activity.findViewById(R.id.emo_btn).setEnabled(alive);
		activity.findViewById(R.id.sticker_btn).setEnabled(alive);
		// TODO : Hide popup OR dialog if visible
	}

	/**
	 * This overrides sendPoke from ChatThread
	 */
	@Override
	protected void sendPoke()
	{
		super.sendPoke();
		if (!oneToNConversation.isMuted())
		{
			Utils.vibrateNudgeReceived(activity.getApplicationContext());
		}
	}

	@Override
	protected void setupActionBar()
	{
		super.setupActionBar();

		setLabel(mConversation.getLabel());

		incrementGroupParticipants(0);
	}

	/**
	 * Setting the group participant count
	 * 
	 * @param morePeopleCount
	 */
	protected void incrementGroupParticipants(int morePeopleCount)
	{
		int numActivePeople = oneToNConversation.getParticipantListSize() + morePeopleCount;

		TextView groupCountTextView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		if (numActivePeople > 0)
		{
			/**
			 * Incrementing numActivePeople by + 1 to add self
			 */
			groupCountTextView.setText(activity.getResources().getString(R.string.num_people, (numActivePeople + 1)));
		}
	}

	@Override
	protected String getMsisdnMainUser()
	{
		return oneToNConversation.getConversationOwner();
	}

	@Override
	protected String getBlockedUserLabel()
	{
		return oneToNConversation.getConversationParticipantName(oneToNConversation.getConversationOwner());
	}

	/**
	 * Perform's actions relevant to clear conversation for a GroupChat
	 */
	@Override
	protected void clearConversation()
	{
		super.clearConversation();
	}

	@Override
	public void onClick(View v)
	{
		Logger.i(TAG, "onclick of view " + v.getId());
		switch (v.getId())
		{
		case R.id.emo_btn:
			emoticonClicked();
			break;
		default:
			super.onClick(v);
		}
	}

	@Override
	public boolean onBackPressed()
	{
		return super.onBackPressed();
	}

	/**
	 * Called from the pubSub thread
	 * 
	 * @param object
	 */
	private void onConvMetadataUpdated(Object object)
	{
		Pair<String, OneToNConversationMetadata> pair = (Pair<String, OneToNConversationMetadata>) object;
		if (msisdn.equals(pair.first))
		{
			oneToNConversation.setMetadata(pair.second);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

	}

	/**
	 * This method is used to update readByList
	 * 
	 */

	@Override
	protected void updateReadByInLoop(long mrMsgId, Set<String> second)
	{
		for (String msgMsisdn : second)
		{
			oneToNConversation.updateReadByList(msgMsisdn, mrMsgId);
		}
	}

	private void onGroupRevived(Object object)
	{
		String groupId = (String) object;

		if (msisdn.equals(groupId))
		{
			uiHandler.sendEmptyMessage(GROUP_REVIVED);
		}
	}

	/**
	 * Called from PubSub Thread
	 * 
	 * @param object
	 */
	private void onParticipantJoinedOrLeftGroup(Object object, boolean joined)
	{
		/**
		 * Received message for current open chatThread
		 */
		if (shouldProcessGCJOrGCK(object)) // Defensive check
		{
			int addPeopleCount = 0;
			if (joined) // Participants added
			{
				JSONObject jObj = (JSONObject) object;

				JSONArray participants = jObj.optJSONArray(HikeConstants.DATA);

				if (participants == null) // If we don't get participants, we simply return here.
				{
					Logger.wtf(TAG, "onParticipantJoinedOrLeftGroup : Getting null participants array in : " + object.toString());
					return;
				}

				addPeopleCount = participants.length();
			}

			else
			// A participant has been kicked out
			{
				addPeopleCount = -1;
			}

			sendUIMessage(PARTICIPANT_JOINED_OR_LEFT_CONVERSATION, addPeopleCount);
		}
	}

	/**
	 * Indicates whether we should process a GCJ/GCK or not.
	 * 
	 * @param object
	 * @return
	 */
	private boolean shouldProcessGCJOrGCK(Object object)
	{
		if (object instanceof JSONObject)
		{
			String msgMsisdn = ((JSONObject) object).optString(HikeConstants.TO);
			if (msgMsisdn != null && oneToNConversation.getMsisdn().equals(msgMsisdn))
			{
				return true;
			}
		}

		// Default case :

		return false;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		if (mHashSpanWatcher != null)
		{
			mHashSpanWatcher.onTextChanged(s, start, count, after);
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (mHashSpanWatcher != null)
		{
			mHashSpanWatcher.afterTextChanged(s);
		}
	}

	@Override
	protected void showThemePicker()
	{
		super.showThemePicker();
		themePicker.showThemePicker(activity.findViewById(R.id.cb_anchor), currentTheme, R.string.chat_theme_tip_group);
	}
	
	protected void onBulkMessageReceived(Object object)
	{
		HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;

		LinkedList<ConvMessage> messagesList = messageListMap.get(msisdn);

		String bulkLabel = null;

		/**
		 * Proceeding only if messages list is not null
		 */

		if (messagesList != null)
		{
			ConvMessage pinConvMessage = null;

			JSONArray ids = new JSONArray();

			for (ConvMessage convMessage : messagesList)
			{
				if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
				{
					pinConvMessage = convMessage;
				}

				if (activity.hasWindowFocus())
				{

					convMessage.setState(ConvMessage.State.RECEIVED_READ);

					if (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
					{
						ids.put(String.valueOf(convMessage.getMappedMsgID()));
					}
				}

				if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
				{
					ContactManager contactManager = ContactManager.getInstance();
					oneToNConversation.setConversationParticipantList(contactManager.getGroupParticipants(oneToNConversation.getMsisdn(), false, false));
				}

				bulkLabel = convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? oneToNConversation.getLabel() : null;

				if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
				{

					SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message);
				}

			}

			sendUIMessage(SET_LABEL, bulkLabel);

			sendUIMessage(BULK_MESSAGE_RECEIVED, messagesList);

			sendUIMessage(SHOW_IMP_MESSAGE, pinConvMessage);

			if (ids != null && ids.length() > 0)
			{
				ChatThreadUtils.doBulkMqttPublish(ids, msisdn);
			}
		}
	}

	/**
	 * Adds a complete list of messages at the end of the messages list and updates the UI at once
	 * 
	 * @param messagesList
	 *            The list of messages to be added
	 */

	private void addBulkMessages(LinkedList<ConvMessage> messagesList)
	{
		/**
		 * Proceeding only if the messages are not null
		 */

		if (messagesList != null)
		{

			/**
			 * If we were showing the typing bubble, we remove it, add the new messages and add the typing bubble again
			 */

			TypingNotification typingNotification = removeTypingNotification();

			mAdapter.addMessages(messagesList, messages.size());

			reachedEnd = false;

			ConvMessage convMessage = messagesList.get(messagesList.size() - 1);

			/**
			 * We add back the typing notification if the message was sent by the user.
			 */

			if (typingNotification != null && (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty()))
			{
				Logger.d(TAG, "Size in chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
				mAdapter.addMessage(new ConvMessage(typingNotification));
			}

			mAdapter.notifyDataSetChanged();

			/**
			 * Don't scroll to bottom if the user is at older messages. It's possible user might be reading them.
			 */
			tryScrollingToBottom(convMessage, messagesList.size());

		}
	}

}