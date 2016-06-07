package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

/**
 * @author paramshah
 */
public abstract class SubCategoryItem extends SpaceManagerItem implements SpaceManagerItem.SpaceManagerDeleteListener
{
    public SubCategoryItem(String header)
    {
        setHeader(header);
        setType(SUBCATEGORY);
    }

    @Override
    public String toString()
    {
        return new Gson().toJson(this);
    }

}
