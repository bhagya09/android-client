package com.bsb.hike.timeline.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.timeline.adapter.StoryListAdapter;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment serves as content for one of home screen tabs. Is the entry point for old timeline. Contains list of friends based on their timeline-related activities.
 * <p/>
 * Created by AtulM on 24/05/16.
 */
public class StoryFragment extends Fragment {
    private View fragmentView;

    private ListView listViewStories;

    private List<StoryItem> storyItemList = new ArrayList<StoryItem>();

    private StoryListAdapter storyAdapter;

    private String mGenus;

    public static StoryFragment newInstance(@Nullable Bundle argBundle) {
        StoryFragment fragmentInstance = new StoryFragment();
        if (argBundle != null) {
            fragmentInstance.setArguments(argBundle);
        }
        return fragmentInstance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Show action buttons
        setHasOptionsMenu(true);

        // Inflate layout
        fragmentView = inflater.inflate(R.layout.fragment_stories, container, false);

        // Get view references
        listViewStories = (ListView) fragmentView.findViewById(R.id.list_view_story);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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

        listViewStories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StoryItem storyItem = storyItemList.get(position);
                if (storyItem.getType() == StoryItem.TYPE_HEADER) {
                    return;
                } else if (storyItem.getType() == StoryItem.TYPE_INTENT) {
                    getActivity().startActivity(storyItem.getIntent());
                } else if (storyItem.getType() == StoryItem.TYPE_FRIEND) {

                } else if (storyItem.getType() == StoryItem.TYPE_BRAND) {
                    // TODO
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.story_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_post:
                int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_CROP_IMAGE | GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE
                        | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

                Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(getActivity(), galleryFlags, Utils.getNewImagePostFilePath());
                startActivityForResult(galleryPickerIntent, UpdatesFragment.TIMELINE_POST_IMAGE_REQ);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        final String genus = data.getStringExtra(HikeConstants.Extras.GENUS);
        if (!TextUtils.isEmpty(genus)) {
            mGenus = genus;
        }

        switch (requestCode) {
            case UpdatesFragment.TIMELINE_POST_IMAGE_REQ:
                ImageParser.parseResult(getActivity(), resultCode, data, new ImageParser.ImageParserListener() {
                    @Override
                    public void imageParsed(String imagePath) {
                        // Open Status update activity
                        getActivity().startActivity(IntentFactory.getPostStatusUpdateIntent(getActivity(), null, imagePath, false));
                    }

                    @Override
                    public void imageParsed(Uri uri) {
                        // Do nothing
                    }

                    @Override
                    public void imageParseFailed() {
                        // Do nothing
                    }
                }, false);
                break;

            default:
                break;
        }
    }
}
