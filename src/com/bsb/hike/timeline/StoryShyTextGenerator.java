package com.bsb.hike.timeline;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by atul on 01/06/16.
 */
public class StoryShyTextGenerator {

    private volatile static StoryShyTextGenerator instance;

    private ArrayList<Integer> recentCameraShySuggestions;

    private Random random;

    private String[] subTextArray;

    public static StoryShyTextGenerator getInstance() {
        if (instance == null) {
            synchronized (StoryShyTextGenerator.class) {
                if (instance == null) {
                    instance = new StoryShyTextGenerator();
                }
            }
        }
        return instance;
    }

    private StoryShyTextGenerator() {
        recentCameraShySuggestions = new ArrayList<Integer>() {
            @Override
            public boolean add(Integer object) {
                //We want to store only last 2 suggestions (to make sure that those are never repeated again)
                if (size() > 2) {
                    remove(0);
                }
                return super.add(object);
            }
        };
        random = new Random();
        subTextArray = HikeMessengerApp.getInstance().getResources().getStringArray(R.array.story_camera_shy_subtext);
    }


    public String getCameraShySubText() {
        int randomPos = -1;
        do {
            randomPos = getRandomPosition(subTextArray.length);
        } while (recentCameraShySuggestions.contains(randomPos));

        recentCameraShySuggestions.add(randomPos);
        return subTextArray[randomPos];
    }

    private int getRandomPosition(int size) {
        return random.nextInt(size);
    }
}
