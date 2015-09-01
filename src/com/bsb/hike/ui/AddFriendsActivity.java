package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AddFriendAdapter;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactInfo;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class AddFriendsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener {
	private ListView listview;
	private AddFriendAdapter mAdapter;
	private TextView nextBtn;
	private int hikeContactsCount = 0;
	HashMap<Integer, List<ContactInfo>> sectionsData = new HashMap<Integer, List<ContactInfo>>();
	private List<ContactInfo> hikeContacts = new ArrayList<ContactInfo>();
	private List<ContactInfo> recommendedContacts = new ArrayList<ContactInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriends);
		initializeViewComponents();
		Utils.executeAsyncTask((new FetchContactsTask()));
	}

	private void init() {
		listview.setOnItemClickListener(this);
		View header = LayoutInflater.from(this).inflate(
				R.layout.addfriends_listview_header, null);
		header.setOnClickListener(null);
		TextView headerTextView = (TextView) header.findViewById(R.id.header_txt);
		setColorSpannedText(headerTextView);
		listview.addHeaderView(header);
		listview.setAdapter(mAdapter);
		setupActionBar();
		StatusBarColorChanger.setStatusBarColor(getWindow(), HikeConstants.STATUS_BAR_BLUE);
	}

	private void setColorSpannedText(TextView headerTextView)
	{
		String headerTextString = getResources().getString(R.string.add_friends_listview_header, hikeContactsCount);
		Spannable headerTextStringSpan = new SpannableString(headerTextString);  
		
		String numContactsString = getResources().getString(R.string.num_contacts, hikeContactsCount);
		int startSpan = headerTextString.indexOf(numContactsString);
		int endSpan = startSpan + numContactsString.length();
		if(startSpan>=0)
		{
			headerTextStringSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue_color_span)), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		headerTextView.setText(headerTextStringSpan);
		
	}
	
	private class FetchContactsTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... arg0)
		{
			List<ContactInfo> friendsList = new ArrayList<ContactInfo>();
			Utils.getRecommendedAndHikeContacts(AddFriendsActivity.this, recommendedContacts, hikeContacts, friendsList);
			recommendedContacts.removeAll(friendsList);
			hikeContacts.removeAll(recommendedContacts);
			Collections.sort(hikeContacts);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			findViewById(R.id.loading_progress).setVisibility(View.GONE);
			sectionsData.put(0, recommendedContacts);
			sectionsData.put(sectionsData.size(), hikeContacts);
			mAdapter = new AddFriendAdapter(AddFriendsActivity.this, -1, sectionsData);
			hikeContactsCount = hikeContacts.size() + recommendedContacts.size();
			Logger.d("AddFriendsActivity", " size hike contacts = "+hikeContacts.size());
			Logger.d("AddFriendsActivity", " size recommendedContacts = "+recommendedContacts.size());
			init();
			super.onPostExecute(result);
		}
	}

	private void initializeViewComponents() {
		listview = (ListView) findViewById(R.id.addfriend_listview);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);
		
		TextView actionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
		nextBtn = (TextView) actionBarView.findViewById(R.id.next_btn);
		
		View doneBtn = actionBarView.findViewById(R.id.done_container);
		
		actionBarTitle.setText(R.string.favorites_ftue_item_label);
		nextBtn.setText(R.string.skip);
		
		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if(mAdapter.getSelectedFriendsCount()>0)
				{
					sendFriendRequest(mAdapter.getSelectedFriends());
				}
				finish();
			}
		});
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	private void sendFriendRequest(Set<ContactInfo> contacts)
	{
		for (ContactInfo contactInfo : contacts)
		{
			FavoriteType favoriteType;
			if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
			{
				favoriteType = FavoriteType.FRIEND;
			}
			else
			{
				favoriteType = FavoriteType.REQUEST_SENT;
			}
			
			FtueContactInfo ftueContactInfo = new FtueContactInfo(contactInfo);
			ftueContactInfo.setFromFtue(true);
			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(ftueContactInfo, favoriteType);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);					
		}
		if(contacts.size() > 1)
		{
			Toast.makeText(this, R.string.favorite_request_sent_multiple, Toast.LENGTH_SHORT).show();
		}
		else
		{
			Toast.makeText(this, R.string.favorite_request_sent, Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		position = position-listview.getHeaderViewsCount();
		ContactInfo contact = (ContactInfo) mAdapter.getItem(mAdapter.getSectionForPosition(position), mAdapter.getPositionInSectionForPosition(position));
		if (mAdapter.getSelectedFriends().contains(contact)) {
			mAdapter.unSelectItem(contact);
		} else {
			mAdapter.selectItem(contact);
		}
		
		if(mAdapter.getSelectedFriendsCount()>0)
		{
			nextBtn.setText(getResources().getString(R.string.add_count_btn, mAdapter.getSelectedFriendsCount()));
		}
		else
		{
			nextBtn.setText(R.string.skip);
		}
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(mAdapter != null)
		{
			mAdapter.getIconImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(mAdapter != null)
		{
			mAdapter.getIconImageLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onBackPressed()
	{
		if(mAdapter!= null && mAdapter.getSelectedFriendsCount()>0)
		{
			mAdapter.unSelectAllFriends();
			nextBtn.setText(R.string.skip);
			return;
		}
		super.onBackPressed();
	}

}
