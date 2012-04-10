package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.ui.MessagesList;

public class ConversationsAdapter extends ArrayAdapter<Conversation>
{

	private int mResourceId;
	private MessagesList mMessagesList;

	public ConversationsAdapter(MessagesList messagesList, int textViewResourceId, List<Conversation> objects)
	{
		super(messagesList, textViewResourceId, objects);
		this.mResourceId = textViewResourceId;
		mMessagesList = messagesList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Context context = getContext();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Conversation conversation = getItem(position);

		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(mResourceId, parent, false);
		}

		TextView contactView = (TextView) v.findViewById(R.id.contact);
		String name = conversation.getContactName();
		if (name == null)
		{
			name = conversation.getMsisdn();
		}

		contactView.setText(name);
		List<ConvMessage> messages = conversation.getMessages();
		if (!messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);

			ImageView imgStatus = (ImageView) v.findViewById(R.id.msg_status_indicator);
			int resId = message.getImageState();
			if (resId > 0)
			{
				imgStatus.setImageResource(resId);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				imgStatus.setImageResource(R.drawable.ic_unread);
				imgStatus.setVisibility(View.VISIBLE);
			}
			else
			{
				imgStatus.setImageResource(0);
				imgStatus.setVisibility(View.GONE);
			}

			TextView messageView = (TextView) v.findViewById(R.id.last_message);
			messageView.setText(message.getMessage());
			TextView tsView = (TextView) v.findViewById(R.id.last_message_timestamp);
			tsView.setText(message.getTimestampFormatted(true));
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				/* set unread messages to BLUE */
				messageView.setTextColor(mMessagesList.getResources().getColor(R.color.unread_message_blue));
			}
			else
			{
				messageView.setTextColor(mMessagesList.getResources().getColor(R.color.grey));
			}
		}

		ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
		avatarView.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(conversation.getMsisdn()));

		return v;
	}
}
