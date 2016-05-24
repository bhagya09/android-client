package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.R;
import com.bsb.hike.timeline.adapter.StoryListAdapter;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.utils.IntentFactory;

/**
 *
 * This fragment serves as content for one of home screen tabs. Is the entry point for old timeline. Contains list of friends based on their timeline-related activities.
 *
 * Created by AtulM on 24/05/16.
 */
public class StoryFragment extends Fragment
{
	private View fragmentView;

	private ListView listViewStories;

	private List<StoryItem> storyItemList = new ArrayList<StoryItem>();

	private StoryListAdapter storyAdapter;

	public static StoryFragment newInstance(@Nullable Bundle argBundle)
	{
		StoryFragment fragmentInstance = new StoryFragment();
		if (argBundle != null)
		{
			fragmentInstance.setArguments(argBundle);
		}
		return fragmentInstance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate layout
		fragmentView = inflater.inflate(R.layout.fragment_stories, container, false);

		// Get view references
		listViewStories = (ListView) fragmentView.findViewById(R.id.list_view_story);

		return fragmentView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		// Manually add "Timeline" as first option in list
		StoryItem timelineItem = new StoryItem(StoryItem.TYPE_INTENT, getString(R.string.timeline));
		timelineItem.setIntent(IntentFactory.getTimelineIntent(getActivity()));

		// TODO
		timelineItem.setSubText("10 updates from 3 friends");
		storyItemList.add(timelineItem);

		// Setup adapters
		storyAdapter = new StoryListAdapter(storyItemList);

		listViewStories.setAdapter(storyAdapter);

		listViewStories.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				StoryItem storyItem = storyItemList.get(position);
				if (storyItem.getType() == StoryItem.TYPE_HEADER)
				{
					return;
				}
				else if (storyItem.getType() == StoryItem.TYPE_INTENT)
				{
					getActivity().startActivity(storyItem.getIntent());
				}
				else if (storyItem.getType() == StoryItem.TYPE_FRIEND)
				{

				}
				else if (storyItem.getType() == StoryItem.TYPE_BRAND)
				{
					// TODO
				}
			}
		});
	}
}
