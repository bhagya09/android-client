package com.bsb.hike.chatthread;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Message;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToNConversationMetadata;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.utils.HashSpanWatcher;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.IntentFactory;

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
	
	protected static final int CONVERSATION_NAME_CHANGED = 209;

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

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		Logger.d(TAG, "Calling super Class' itemClicked");
		super.itemClicked(item);
	}
	@Override
	protected void openMessageInfoScreen(ConvMessage convMessage){
		Intent intent= IntentFactory.messageInfoIntent(activity, convMessage.getMsgID());
		intent.putExtra(HikeConstants.MESSAGE_INFO.MESSAGE_INFO_TYPE,HikeConstants.MESSAGE_INFO.GROUP);
		Logger.d("MessageInfo","Msisdn is "+msisdn);
		intent.putExtra(HikeConstants.MSISDN, msisdn);
		activity.startActivity(intent);

	}
	/**
	 * NON UI
	 */
	@Override
	protected Conversation fetchConversation()
	{
		Logger.i(TAG, "fetch group conversation " + Thread.currentThread().getName());

		// Set participant read by list
		Pair<String, Long> pair = HikeConversationsDatabase.getInstance().getReadByValueForGroup(oneToNConversation.getMsisdn());
		if (pair != null)
		{
			String readBy = pair.first;
			long msgId = pair.second;
			oneToNConversation.setupReadByList(readBy, msgId);
		}

		// fetch theme
		String currentThemeId = mConversationDb.getChatThemeIdForMsisdn(msisdn);
		Logger.d("ChatThread", "Calling setchattheme from createConversation");
		oneToNConversation.setChatThemeId(currentThemeId);
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
		oneToNConversation.setConversationParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), true, false));
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
		/**
		 * If the last message was visible before receiving the read notification it can be hidden, hence we need to scroll to bottom.
		 */
		if (mConversationsView != null && (mConversationsView.getLastVisiblePosition() == mConversationsView.getCount() - 1))
		{
			uiHandler.sendEmptyMessage(SCROLL_TO_END);
		}
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
		case HikePubSub.PARTICIPANT_LEFT_ONETONCONV:
			onParticipantJoinedOrLeftGroup(object);
			break;
		case HikePubSub.BULK_MESSAGE_RECEIVED:
			onBulkMessageReceived(object);
			break;
		case HikePubSub.PARTICIPANT_JOINED_SYSTEM_MESSAGE:
			onMessageReceived(object);
			break;
		case HikePubSub.ONETONCONV_NAME_CHANGED:
			onConversationNameChanged((String) object);
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
		case PARTICIPANT_JOINED_OR_LEFT_CONVERSATION:
			showActiveConversationMemberCount();
			break;
		case GROUP_REVIVED:
			handleGroupRevived();
			break;
		case BULK_MESSAGE_RECEIVED:
			addBulkMessages((LinkedList<ConvMessage>) msg.obj);
			break;
		case CONVERSATION_NAME_CHANGED:
			setLabel(oneToNConversation.getConversationName());
			setAvatar();
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
		activity.findViewById(R.id.send_message_audio).setEnabled(alive);//CE-450
		activity.findViewById(R.id.msg_compose).setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		activity.findViewById(R.id.emoticon_btn).setEnabled(alive);
		activity.findViewById(R.id.sticker_btn).setEnabled(alive);
		// TODO : Hide popup OR dialog if visible
	}

	@Override
	protected void sendNudge() {
		super.sendNudge();
		if (!oneToNConversation.isMuted()) {
			Utils.vibrateNudgeReceived(activity.getApplicationContext());
		}
	}

	@Override
	protected void setupDefaultActionBar(boolean firstInflation)
	{
		super.setupDefaultActionBar(firstInflation);
		
		//Added for QA Automation
		ImageView avatar = (ImageView) mActionBarView.findViewById(R.id.avatar);
		if(avatar != null)
		{
			avatar.setContentDescription(getResources().getString(R.string.content_des_round_group_image_chatthread_actionbar));
		}

		setLabel(mConversation.getLabel());
		showActiveConversationMemberCount();
	}

	/**
	 * Setting the group participant count
	 */
	protected void showActiveConversationMemberCount()
	{
		int numActivePeople = oneToNConversation.getParticipantListSize();
		
		TextView memberCountTextView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		if (numActivePeople >= 0)
		{
			/**
			 * Incrementing numActivePeople by + 1 to add self
			 */
			int id = numActivePeople == 0 ? R.string.num_member: R.string.num_members;
			memberCountTextView.setText(activity.getResources().getString(id, (numActivePeople + 1)));
			memberCountTextView.setSelected(true);
		}
	}

	@Override
	protected String getMsisdnMainUser()
	{
		return oneToNConversation.getConversationOwner();
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
	 * Called from PubSub Thread when conversation name is changed
	 * 
	 * @param object
	 */
	protected void onConversationNameChanged(String msisdn)
	{
		/**
		 * Received message for current open chatThread
		 */
		if (shouldProcessPubSubEvent(msisdn)) // Defensive check
		{
			ContactManager conMgr = ContactManager.getInstance();
			String convName = conMgr.getName(oneToNConversation.getMsisdn());
			oneToNConversation.setConversationName(convName);
			uiHandler.sendEmptyMessage(CONVERSATION_NAME_CHANGED);
		}
	}
	
	/**
	 * Called from PubSub Thread
	 * 
	 * @param object
	 */
	private void onParticipantJoinedOrLeftGroup(Object object)
	{
		/**
		 * Received message for current open chatThread
		 */
		if (shouldProcessPubSubEvent(object)) // Defensive check
		{
			ContactManager conMgr = ContactManager.getInstance();
			oneToNConversation.setConversationParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), true, false));
			uiHandler.sendEmptyMessage(PARTICIPANT_JOINED_OR_LEFT_CONVERSATION);
		}
	}

	/**
	 * Indicates whether we should process a PubSub or not.
	 * 
	 * @param object
	 * @return
	 */
	private boolean shouldProcessPubSubEvent(Object object)
	{
		if (object instanceof JSONObject)
		{
			String msgMsisdn = ((JSONObject) object).optString(HikeConstants.TO);
			if (msgMsisdn != null && oneToNConversation.getMsisdn().equals(msgMsisdn))
			{
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Indicates whether we should process a PubSub or not.
	 * 
	 * @param object
	 * @return
	 */
	private boolean shouldProcessPubSubEvent(String msisdn)
	{
		if (msisdn != null)
		{
			return msisdn.equals(this.msisdn);
		}
		
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
					oneToNConversation.setConversationParticipantList(contactManager.getGroupParticipants(oneToNConversation.getMsisdn(), true, false));
				}

				bulkLabel = convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? oneToNConversation.getLabel() : null;

				if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
				{

					SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message, AudioManager.STREAM_RING);
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
	
	@Override
	protected String getBlockedUserLabel()
	{
		return oneToNConversation.getConversationParticipantName(oneToNConversation.getConversationOwner());
	}

	@Override
	protected boolean shouldShowKeyboard()
	{
		return (oneToNConversation.isConversationAlive() && super.shouldShowKeyboard());
	}
}