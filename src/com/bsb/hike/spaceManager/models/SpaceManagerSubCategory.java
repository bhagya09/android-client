package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * @author paramshah
 */
public abstract class SpaceManagerSubCategory extends SpaceManagerItem
{
    public SpaceManagerSubCategory(String header)
    {
        setHeader(header);
        setType(2);
    }

    public abstract void onDelete();

    @Override
    public String toString()
    {
        return new Gson().toJson(this);
    }

}
