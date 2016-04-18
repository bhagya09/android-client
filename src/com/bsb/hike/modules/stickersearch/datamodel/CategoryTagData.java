package com.bsb.hike.modules.stickersearch.datamodel;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.internal.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by akhiltripathi on 12/04/16.
 */
public class CategoryTagData implements Comparable<CategoryTagData>
{
    private static final String TAG = CategoryTagData.class.getSimpleName();

    private final int CATEGORY_META_TAGS_COUNT = 3;

    private String categoryID;

    private StickerCategory category;

    private String language;

    private ArrayList<String> keywords;

    private String theme;

    private String name;

    private int forGender;

    public StickerCategory getCategory() {
        return category;
    }

    public CategoryTagData(String categoryID)
    {
        this.categoryID = categoryID;
        this.category = StickerManager.getInstance().getCategoryForId(categoryID);
    }

    
	public String getCategoryID()
	{
		return categoryID;
	}

	public void setCategoryID(String categoryID)
	{
		this.categoryID = categoryID;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public String getTheme()
	{
		return theme;
	}

	public void setTheme(String theme)
	{
		this.theme = theme;
	}

    public boolean isValid()
    {
        return true;
    }

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

    public int getGender()
    {
        return forGender;
    }

    public void setGender(int forGender)
    {
        this.forGender = forGender;
    }

    public String getKeywordsSet()
    {
        String result = "";

//        for(String keyword : keywords)
//        {
//            if(TextUtils.isEmpty(keyword))
//            {
//                continue;
//            }
//
//            result += keyword + HikeConstants.DELIMETER;
//        }
//
//        result = result.substring(0,result.length() - 1);

        return result;

    }

	public List<String> getTagList()
	{
        int size = CATEGORY_META_TAGS_COUNT;

        boolean hasKeywords = !Utils.isEmpty(keywords);

        if(hasKeywords)
        {
            size += keywords.size();
        }

		ArrayList<String> result = new ArrayList<String>(size);
        result.add(this.getTheme());
        result.add(this.getName());
        result.add(this.getLanguage());

        if(hasKeywords)
        {
            result.addAll(keywords);
        }

        return result;
	}

    @Override
    public int compareTo(CategoryTagData another)
    {
        return Integer.compare(this.category.getCategoryIndex(),another.getCategory().getCategoryIndex());
    }
}
