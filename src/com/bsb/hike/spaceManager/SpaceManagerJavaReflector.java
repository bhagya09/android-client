package com.bsb.hike.spaceManager;

import com.bsb.hike.spaceManager.models.CategoryItem;
import com.bsb.hike.spaceManager.models.CategoryPojo;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.spaceManager.models.SubCategoryPojo;
import com.bsb.hike.utils.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author paramshah
 */
public class SpaceManagerJavaReflector
{
    private static List<CategoryItem> categoryList;

    private static final String TAG = "SpaceManagerJavaReflector";

    public static List<CategoryItem> reflect(List<CategoryPojo> categoryPojoList) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IllegalArgumentException
    {
        Logger.d(TAG, "in reflect");
        categoryList = new ArrayList<>();
        for(CategoryPojo categoryPojo : categoryPojoList)
        {
            categoryList.add(getCategoryFromPojo(categoryPojo));
        }
        return categoryList;
    }

    private static CategoryItem getCategoryFromPojo(CategoryPojo categoryPojo) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IllegalArgumentException
    {
        Logger.d(TAG, "in getCategoryFromPojo");
        List<SubCategoryItem> subCategories = new ArrayList<>();
        for(SubCategoryPojo subCategoryPojo : categoryPojo.getSubCategoryList())
        {
            subCategories.add(getSubCategoryFromPojo(subCategoryPojo));
        }
        Class categoryClass = Class.forName(categoryPojo.getClassName());
        Constructor categoryConstructor = categoryClass.getConstructor(String.class, ArrayList.class);
        return (CategoryItem) categoryConstructor.newInstance(categoryPojo.getHeader(), subCategories);

    }

    private static SubCategoryItem getSubCategoryFromPojo(SubCategoryPojo subCategoryPojo) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IllegalArgumentException
    {
        Logger.d(TAG, "in getSubCategoryFromPojo");
        Class subCategoryClass = Class.forName(subCategoryPojo.getClassName());
        Constructor subCategoryConstructor = subCategoryClass.getConstructor(String.class);
        return (SubCategoryItem) subCategoryConstructor.newInstance(subCategoryPojo.getHeader());
    }
}
