package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

/**
 * @author paramshah
 */
public abstract class SpaceManagerSubCategory extends SpaceManagerItem implements SpaceManagerItem.SpaceManagerDeleteListener
{
    public SpaceManagerSubCategory(String header)
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
