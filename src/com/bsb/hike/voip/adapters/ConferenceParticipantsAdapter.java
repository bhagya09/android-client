package com.bsb.hike.voip.adapters;

import java.util.List;

import android.animation.LayoutTransition;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient;


public class ConferenceParticipantsAdapter extends ArrayAdapter<VoIPClient> {

	private Context context;
	private List<VoIPClient> clients;
	private IconLoader iconLoader;
	
	private LayoutTransition itemViewTransition;
	
	private final String tag = getClass().getSimpleName();
	
	private OnClickListener conferencePartitcipantClickListenter = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.remove_participant_btn:

				Logger.d(tag,"cross clicked " + clients.get((Integer) v.getTag()).getPhoneNumber() );
				//TODO inform service about remove action
				//((ViewGroup) v.getParent()).setLayoutTransition(new LayoutTransition());
				clients.remove((Integer)v.getTag());
				notifyDataSetChanged();
				break;

			default:
				Logger.d(tag,"unrecognized ID");
				break;
			}
		
		}
	};
	
	public static class ConferenceParticipantHolder
	{
		public ImageView avatarHolder;
		public LinearLayout itemViewHolder;
		public TextView contactNameHolder;
		public ImageView isSpeakingHolder;
		public ImageView crossBtnHolder;
	}
	
	public ConferenceParticipantsAdapter(Context context, int resource,
			int textViewResourceId, List<VoIPClient> objects) {
		super(context, resource, textViewResourceId, objects);
		
		this.context = context;
		this.clients = objects;
		
		iconLoader = new IconLoader(context, context.getResources().getDimensionPixelSize(R.dimen.small_avatar));
		iconLoader.setImageFadeIn(false);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ConferenceParticipantHolder conferenceParticipantHolder;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null)
		{
			conferenceParticipantHolder = new ConferenceParticipantHolder();
			
			convertView = inflater.inflate(R.layout.voip_conference_participant_item, parent, false);
			conferenceParticipantHolder.itemViewHolder = (LinearLayout) convertView.findViewById(R.id.voip_conference_participant_item);
			conferenceParticipantHolder.avatarHolder  = (ImageView) convertView.findViewById(R.id.avatar);
			conferenceParticipantHolder.contactNameHolder = (TextView) convertView.findViewById(R.id.contact);
			conferenceParticipantHolder.isSpeakingHolder = (RecyclingImageView) convertView.findViewById(R.id.is_speaking_view);
			conferenceParticipantHolder.crossBtnHolder = (RecyclingImageView) convertView.findViewById(R.id.remove_participant_btn);
			convertView.setTag(conferenceParticipantHolder);
			
		}
		else
		{
			conferenceParticipantHolder = (ConferenceParticipantHolder)convertView.getTag();
		}
		conferenceParticipantHolder.contactNameHolder.setText(clients.get(position).getName());
		iconLoader.loadImage(clients.get(position).getPhoneNumber(), conferenceParticipantHolder.avatarHolder, false, false, true);

		convertView.setEnabled(false);
		convertView.setOnClickListener(null);
		
		itemViewTransition = new LayoutTransition();
		itemViewTransition.disableTransitionType(LayoutTransition.APPEARING);
		itemViewTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
		itemViewTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
		itemViewTransition.enableTransitionType(LayoutTransition.DISAPPEARING);
		
		conferenceParticipantHolder.itemViewHolder.setLayoutTransition(itemViewTransition);

		conferenceParticipantHolder.crossBtnHolder.setTag(position);
		conferenceParticipantHolder.crossBtnHolder.setEnabled(true);
		conferenceParticipantHolder.crossBtnHolder.setOnClickListener(conferencePartitcipantClickListenter);

		return convertView;
	}

}
