package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ComposeActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;

public class ConversationFragment extends SherlockListFragment implements
		OnItemLongClickListener, Listener, Runnable {

	private class DeleteConversationsAsyncTask extends
			AsyncTask<Conversation, Void, Conversation[]> {

		@Override
		protected Conversation[] doInBackground(Conversation... convs) {
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			ArrayList<String> msisdns = new ArrayList<String>(convs.length);
			Editor editor = getActivity().getSharedPreferences(
					HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).edit();
			for (Conversation conv : convs) {
				ids.add(conv.getConvId());
				msisdns.add(conv.getMsisdn());
				editor.remove(conv.getMsisdn());
			}
			editor.commit();

			db = HikeConversationsDatabase.getInstance();
			db.deleteConversation(ids.toArray(new Long[] {}), msisdns);

			return convs;
		}

		@Override
		protected void onPostExecute(Conversation[] deleted) {
			NotificationManager mgr = (NotificationManager) getActivity()
					.getSystemService(Context.NOTIFICATION_SERVICE);
			for (Conversation conversation : deleted) {
				mgr.cancel((int) conversation.getConvId());
				mAdapter.remove(conversation);
				mConversationsByMSISDN.remove(conversation.getMsisdn());
				mConversationsAdded.remove(conversation.getMsisdn());
			}

			mAdapter.notifyDataSetChanged();
			mAdapter.setNotifyOnChange(false);
		}
	}

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED,
			HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_DELIVERED_READ,
			HikePubSub.MESSAGE_DELIVERED, HikePubSub.NEW_CONVERSATION,
			HikePubSub.MESSAGE_SENT, HikePubSub.MSG_READ,
			HikePubSub.ICON_CHANGED, HikePubSub.GROUP_NAME_CHANGED,
			HikePubSub.CONTACT_ADDED, HikePubSub.LAST_MESSAGE_DELETED,
			HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION,
			HikePubSub.RESET_UNREAD_COUNT, HikePubSub.GROUP_LEFT };

	private ConversationsAdapter mAdapter;
	private HashMap<String, Conversation> mConversationsByMSISDN;
	private HashSet<String> mConversationsAdded;
	private Comparator<? super Conversation> mConversationsComparator;
	private Handler messageRefreshHandler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.conversations, null);

		ListView friendsList = (ListView) parent
				.findViewById(android.R.id.list);
		friendsList.setEmptyView(parent.findViewById(android.R.id.empty));

		Button startChat = (Button) parent.findViewById(R.id.start_chat_btn);
		startChat.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), ComposeActivity.class);
				intent.putExtra(HikeConstants.Extras.EDIT, true);
				startActivity(intent);
			}
		});

		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mConversationsComparator = new Conversation.ConversationComparator();
		fetchConversations();

		for (TypingNotification typingNotification : HikeMessengerApp
				.getTypingNotificationSet().values()) {
			toggleTypingNotification(true, typingNotification);
		}
	}

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Conversation conv = (Conversation) mAdapter.getItem(position);
		if (conv == null) {
			return;
		}
		Intent intent = Utils.createIntentForConversation(getSherlockActivity(), conv);
		startActivity(intent);
	}



	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		if (position >= mAdapter.getCount()) {
			return false;
		}
		ArrayList<String> optionsList = new ArrayList<String>();

		final Conversation conv = (Conversation) mAdapter.getItem(position);

		optionsList.add(getString(R.string.shortcut));
		optionsList.add(getString(R.string.email_conversation));
		if (conv instanceof GroupConversation) {
			optionsList.add(getString(R.string.delete_leave));
		} else {
			optionsList.add(getString(R.string.delete));
		}
		optionsList.add(getString(R.string.deleteconversations));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(
				getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.shortcut).equals(option)) {
							Utils.logEvent(getActivity(),
									HikeConstants.LogEvent.ADD_SHORTCUT);
							Utils.createShortcut(getSherlockActivity(), conv);
						} else if (getString(R.string.delete).equals(option)) {
							Utils.logEvent(getActivity(),
									HikeConstants.LogEvent.DELETE_CONVERSATION);
							DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
							Utils.executeConvAsyncTask(task, conv);
						} else if (getString(R.string.delete_leave).equals(
								option)) {
							Utils.logEvent(getActivity(),
									HikeConstants.LogEvent.DELETE_CONVERSATION);
							leaveGroup(conv);
						} else if (getString(R.string.email_conversation)
								.equals(option)) {
							EmailConversationsAsyncTask task = new EmailConversationsAsyncTask(getSherlockActivity(), ConversationFragment.this);
							Utils.executeConvAsyncTask(task, conv);
						} else if (getString(R.string.deleteconversations).equals(option)) {
							Utils.logEvent(getActivity(),
									HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
							DeleteAllConversations();
						} 

					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void fetchConversations() {
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		List<Conversation> conversations = db.getConversations();

		mConversationsByMSISDN = new HashMap<String, Conversation>(
				conversations.size());
		mConversationsAdded = new HashSet<String>();

		/*
		 * Use an iterator so we can remove conversations w/ no messages from
		 * our list
		 */
		for (Iterator<Conversation> iter = conversations.iterator(); iter
				.hasNext();) {
			Conversation conv = (Conversation) iter.next();
			mConversationsByMSISDN.put(conv.getMsisdn(), conv);
			if (conv.getMessages().isEmpty()
					&& !(conv instanceof GroupConversation)) {
				iter.remove();
			} else {
				mConversationsAdded.add(conv.getMsisdn());
			}
		}

		if (mAdapter != null) {
			mAdapter.clear();
		}

		mAdapter = new ConversationsAdapter(getActivity(),
				R.layout.conversation_item, conversations);

		/*
		 * because notifyOnChange gets re-enabled whenever we call
		 * notifyDataSetChanged it's simpler to assume it's set to false and
		 * always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);

		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void leaveGroup(Conversation conv) {
		if (conv == null) {
			Log.d(getClass().getSimpleName(), "Invalid conversation");
			return;
		}
		HikeMessengerApp
				.getPubSub()
				.publish(
						HikePubSub.MQTT_PUBLISH,
						conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
		deleteConversation(conv);
	}

	private void deleteConversation(Conversation conv) {
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
		Utils.executeConvAsyncTask(task, conv);
	}

	private void toggleTypingNotification(boolean isTyping,
			TypingNotification typingNotification) {
		if (mConversationsByMSISDN == null) {
			return;
		}
		String msisdn = typingNotification.getId();
		Conversation conversation = mConversationsByMSISDN.get(msisdn);
		if (conversation == null) {
			Log.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}
		List<ConvMessage> messageList = conversation.getMessages();
		if (messageList.isEmpty()) {
			Log.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}
		if (isTyping) {
			ConvMessage message = messageList.get(messageList.size() - 1);
			if (!HikeConstants.IS_TYPING.equals(message.getMessage())
					&& message.getMsgID() != -1
					&& message.getMappedMsgID() != -1) {
				// Setting the msg id and mapped msg id as -1 to identify that
				// this is an "is typing..." message.
				ConvMessage convMessage = new ConvMessage(
						HikeConstants.IS_TYPING, msisdn,
						message.getTimestamp(),
						ConvMessage.State.RECEIVED_UNREAD, -1, -1);
				messageList.add(convMessage);
			}
		} else {
			ConvMessage message = messageList.get(messageList.size() - 1);
			if (HikeConstants.IS_TYPING.equals(message.getMessage())
					&& message.getMsgID() == -1
					&& message.getMappedMsgID() == -1) {
				messageList.remove(message);
			}
		}
		getActivity().runOnUiThread(this);
	}

	@Override
	public void run() {
		if (mAdapter == null) {
			return;
		}
		mAdapter.notifyDataSetChanged();
		// notifyDataSetChanged sets notifyonChange to true but we want it to
		// always be false
		mAdapter.setNotifyOnChange(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(String type, Object object) {

		if (!isAdded()) {
			return;
		}
		Log.d(getClass().getSimpleName(), "Event received: " + type);
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type))
				|| (HikePubSub.MESSAGE_SENT.equals(type))) {
			Log.d(getClass().getSimpleName(), "New msg event sent or received.");
			ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			final Conversation conv = mConversationsByMSISDN.get(msisdn);

			if (conv == null) {
				// When a message gets sent from a user we don't have a
				// conversation for, the message gets
				// broadcasted first then the conversation gets created. It's
				// okay that we don't add it now, because
				// when the conversation is broadcasted it will contain the
				// messages
				return;
			}

			if (Utils.shouldIncrementCounter(message)) {
				conv.setUnreadCount(conv.getUnreadCount() + 1);
			}

			if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE) {
				if (!conv.getMessages().isEmpty()) {
					ConvMessage prevMessage = conv.getMessages().get(
							conv.getMessages().size() - 1);
					String metadata = message.getMetadata().serialize();
					message = new ConvMessage(message.getMessage(),
							message.getMsisdn(), prevMessage.getTimestamp(),
							prevMessage.getState(), prevMessage.getMsgID(),
							prevMessage.getMappedMsgID(),
							message.getGroupParticipantMsisdn());
					try {
						message.setMetadata(metadata);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			// For updating the group name if some participant has joined or
			// left the group
			else if ((conv instanceof GroupConversation)
					&& message.getParticipantInfoState() != ParticipantInfoState.NO_INFO) {
				HikeConversationsDatabase hCDB = HikeConversationsDatabase
						.getInstance();
				((GroupConversation) conv).setGroupParticipantList(hCDB
						.getGroupParticipants(conv.getMsisdn(), false, false));
			}

			final ConvMessage finalMessage = message;

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					addMessage(conv, finalMessage);

					messageRefreshHandler
							.removeCallbacks(ConversationFragment.this);
					messageRefreshHandler.postDelayed(
							ConversationFragment.this, 100);
				}
			});

		} else if (HikePubSub.LAST_MESSAGE_DELETED.equals(type)) {
			Pair<ConvMessage, String> messageMsisdnPair = (Pair<ConvMessage, String>) object;

			final ConvMessage message = messageMsisdnPair.first;
			final String msisdn = messageMsisdnPair.second;

			final boolean conversationEmpty = message == null;

			final Conversation conversation = mConversationsByMSISDN
					.get(msisdn);

			final List<ConvMessage> messageList = new ArrayList<ConvMessage>(1);

			if (!conversationEmpty) {
				if (conversation == null) {
					return;
				}
				messageList.add(message);
			}
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (conversationEmpty) {
						mConversationsByMSISDN.remove(msisdn);
						mConversationsAdded.remove(msisdn);
						mAdapter.remove(conversation);
					} else {
						conversation.setMessages(messageList);
					}
					mAdapter.sort(mConversationsComparator);
					mAdapter.notifyDataSetChanged();
					// notifyDataSetChanged sets notifyonChange to true but we
					// want it to always be false
					mAdapter.setNotifyOnChange(false);
				}
			});
		} else if (HikePubSub.NEW_CONVERSATION.equals(type)) {
			final Conversation conversation = (Conversation) object;
			Log.d(getClass().getSimpleName(),
					"New Conversation. Group Conversation? "
							+ (conversation instanceof GroupConversation));
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty()
					&& !(conversation instanceof GroupConversation)) {
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					mAdapter.add(conversation);
					if (conversation instanceof GroupConversation) {
						mAdapter.notifyDataSetChanged();
					}
					mAdapter.setNotifyOnChange(false);
				}
			});
		} else if (HikePubSub.MSG_READ.equals(type)) {
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null) {
				/*
				 * We don't really need to do anything if the conversation does
				 * not exist.
				 */
				return;
			}
			/*
			 * look for the latest received messages and set them to read. Exit
			 * when we've found some read messages
			 */
			List<ConvMessage> messages = conv.getMessages();
			for (int i = messages.size() - 1; i >= 0; --i) {
				ConvMessage msg = messages.get(i);
				if (Utils.shouldChangeMessageState(msg,
						ConvMessage.State.RECEIVED_READ.ordinal())) {
					ConvMessage.State currentState = msg.getState();
					msg.setState(ConvMessage.State.RECEIVED_READ);
					if (currentState == ConvMessage.State.RECEIVED_READ) {
						break;
					}
				}
			}

			getActivity().runOnUiThread(this);
		} else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) {
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_CONFIRMED.ordinal())) {
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				getActivity().runOnUiThread(this);
			}
		} else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type)) {
			Pair<String, long[]> pair = (Pair<String, long[]>) object;

			long[] ids = (long[]) pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++) {
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg,
						ConvMessage.State.SENT_DELIVERED_READ.ordinal())) {
					// If the msisdn don't match we simply return
					if (!msg.getMsisdn().equals(pair.first)) {
						return;
					}
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			getActivity().runOnUiThread(this);
		} else if (HikePubSub.MESSAGE_DELIVERED.equals(type)) {
			Pair<String, Long> pair = (Pair<String, Long>) object;

			long msgId = pair.second;
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_DELIVERED.ordinal())) {
				// If the msisdn don't match we simply return
				if (!msg.getMsisdn().equals(pair.first)) {
					return;
				}
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				getActivity().runOnUiThread(this);
			}
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			/* an icon changed, so update the view */
			getActivity().runOnUiThread(this);
		} else if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) {
			String groupId = (String) object;
			HikeConversationsDatabase db = HikeConversationsDatabase
					.getInstance();
			final String groupName = db.getGroupName(groupId);

			Conversation conv = mConversationsByMSISDN.get(groupId);
			if (conv == null) {
				return;
			}
			conv.setContactName(groupName);

			getActivity().runOnUiThread(this);
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null) {
				return;
			}

			Conversation conversation = this.mConversationsByMSISDN
					.get(contactInfo.getMsisdn());
			if (conversation != null) {
				conversation.setContactName(contactInfo.getName());
				getActivity().runOnUiThread(this);
			}
		} else if (HikePubSub.TYPING_CONVERSATION.equals(type)
				|| HikePubSub.END_TYPING_CONVERSATION.equals(type)) {
			if (object == null) {
				return;
			}

			toggleTypingNotification(
					HikePubSub.TYPING_CONVERSATION.equals(type),
					(TypingNotification) object);
		} else if (HikePubSub.RESET_UNREAD_COUNT.equals(type)) {
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null) {
				return;
			}
			conv.setUnreadCount(0);

			getActivity().runOnUiThread(this);
		} else if (HikePubSub.GROUP_LEFT.equals(type)) {
			String groupId = (String) object;
			final Conversation conversation = mConversationsByMSISDN
					.get(groupId);
			if (conversation == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deleteConversation(conversation);
				}
			});
		}
	}

	private ConvMessage findMessageById(long msgId) {
		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i) {
			Conversation conversation = mAdapter.getItem(i);
			if (conversation == null) {
				continue;
			}
			List<ConvMessage> messages = conversation.getMessages();
			if (messages.isEmpty()) {
				continue;
			}

			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getMsgID() == msgId) {
				return message;
			}
		}

		return null;
	}

	private void addMessage(Conversation conv, ConvMessage convMessage) {
		if (!mConversationsAdded.contains(conv.getMsisdn())) {
			mConversationsAdded.add(conv.getMsisdn());
			mAdapter.add(conv);
		}

		conv.addMessage(convMessage);
		Log.d(getClass().getSimpleName(), "new message is " + convMessage);
		mAdapter.sort(mConversationsComparator);

		if (messageRefreshHandler == null) {
			messageRefreshHandler = new Handler();
		}
	}
	
	public void DeleteAllConversations() {
		DialogInterface.OnClickListener dialoagOnClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialogInterfaceClickListener(dialog, which);
			}
		};
		if (!mAdapter.isEmpty()) {
				Utils.logEvent(getActivity(),
						HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(R.string.delete_all_question)
						.setPositiveButton(R.string.delete,
								dialoagOnClickListener)
						.setNegativeButton(R.string.cancel,
								dialoagOnClickListener).show();
			}
	}

	public void dialogInterfaceClickListener(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			Conversation[] convs = new Conversation[mAdapter.getCount()];
			for (int i = 0; i < convs.length; i++) {
				convs[i] = mAdapter.getItem(i);
				if ((convs[i] instanceof GroupConversation)) {
					HikeMessengerApp
							.getPubSub()
							.publish(
									HikePubSub.MQTT_PUBLISH,
									convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
				}
			}
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(convs);
			break;
		default:
		}
	}	
}
