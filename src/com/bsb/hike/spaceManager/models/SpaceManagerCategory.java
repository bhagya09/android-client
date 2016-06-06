package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author paramshah
 */
public abstract class SpaceManagerCategory extends SpaceManagerItem
{
    private List<SpaceManagerSubCategory> subCategoryList;

    public SpaceManagerCategory(String header, ArrayList<SpaceManagerSubCategory> subCategories)
    {
        setHeader(header);
        setType(CATEGORY);
        this.subCategoryList = subCategories;
    }

    public long computeSize()
    {
        long size = 0;
        for(SpaceManagerSubCategory subCategory : subCategoryList)
        {
            size += subCategory.getSize();
        }
        return size;
    }

    public List<SpaceManagerSubCategory> getSubCategoryList()
    {
        return subCategoryList;
    }

    @Override
    public String toString()
    {
        return new Gson().toJson(this);
    }
}
