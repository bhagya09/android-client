package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

/**
 * @author paramshah
 */
public abstract class SubCategoryItem extends SpaceManagerItem implements SpaceManagerItem.SpaceManagerDeleteListener
{
    private boolean isSelected;

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

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
