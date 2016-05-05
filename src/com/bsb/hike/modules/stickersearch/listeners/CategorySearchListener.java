package com.bsb.hike.modules.stickersearch.listeners;

import com.bsb.hike.models.StickerCategory;

import java.util.List;

/**
 * Created by akhiltripathi on 14/04/16.
 */
public interface CategorySearchListener
{
    public void onSearchCompleted(List<StickerCategory> categories);

    public void onNoCategoriesFound(String query);

    public void onSearchInitiated();

}
