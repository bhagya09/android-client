package com.bsb.hike.voip.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPConstants;

public class ConferenceParticipantsAdapter extends ArrayAdapter<VoIPClient> {

	private Context context;
	private List<VoIPClient> clients;
	private IconLoader iconLoader;
	
	private final String tag = VoIPConstants.TAG + " Conf Adapter";
	
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
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.voip_conference_participant_item, parent, false);
		
		ImageView avatar = (ImageView) rowView.findViewById(R.id.avatar);
		TextView contact = (TextView) rowView.findViewById(R.id.contact);
		ImageButton removeParticipant = (ImageButton) rowView.findViewById(R.id.remove_participant_btn);
		
		contact.setText(clients.get(position).getName());
		iconLoader.loadImage(clients.get(position).getPhoneNumber(), 
				avatar, false, false, true);
		
		removeParticipant.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Logger.d(tag, "click!");
			}
		});
		
		return rowView;
	}

}
