package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author paramshah
 */
public abstract class CategoryItem extends SpaceManagerItem
{
    private List<SubCategoryItem> subCategoryList;

    public CategoryItem(String header, ArrayList<SubCategoryItem> subCategories)
    {
        setHeader(header);
        setType(CATEGORY);
        this.subCategoryList = subCategories;
    }

    public long computeSizeToDelete()
    {
        long size = 0;
        for(SubCategoryItem subCategory : subCategoryList)
        {
            if(subCategory.isSelected())
            {
                size += subCategory.getSize();
            }
        }
        return size;
    }

    public long computeSize()
    {
        long size = 0;
        for(SubCategoryItem subCategory : subCategoryList)
        {
            size += subCategory.getSize();
        }
        return size;
    }

    public List<SubCategoryItem> getSubCategoryList()
    {
        return subCategoryList;
    }

    @Override
    public String toString()
    {
        return new Gson().toJson(this);
    }
}
