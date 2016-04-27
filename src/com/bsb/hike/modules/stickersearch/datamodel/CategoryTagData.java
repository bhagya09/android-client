package com.bsb.hike.modules.stickersearch.datamodel;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.internal.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akhiltripathi on 12/04/16.
 */
public class CategoryTagData
{
	private static final String TAG = CategoryTagData.class.getSimpleName();

	private final int CATEGORY_META_TAGS_COUNT = 3;

	protected String categoryID;

	protected String language;

	private List<String> keywords;

	protected String theme;

	protected String name;

	protected int forGender;

	public CategoryTagData(String categoryID)
	{
		this.categoryID = categoryID;
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
		return !TextUtils.isEmpty(categoryID);
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

		if (Utils.isEmpty(keywords))
		{
			return result;
		}

		for (String keyword : keywords)
		{
			if (TextUtils.isEmpty(keyword))
			{
				continue;
			}

			result += keyword + HikeConstants.DELIMETER;
		}

		result = result.substring(0, result.length() - 1);

		return result;

	}

	public void setKeywords(ArrayList<String> tags)
	{
		keywords = tags;
	}

	public String getTagList()
	{
		int size = CATEGORY_META_TAGS_COUNT;

		String result = this.getName();
		result += StickerSearchConstants.STRING_SPACE + this.getTheme();

		if (!Utils.isEmpty(keywords))
		{
			for (String keyword : keywords)
			{
				result += StickerSearchConstants.STRING_SPACE + keyword;
			}
		}

		return result;
	}

	@Override
	public String toString()
	{
		return categoryID + " : name = " + name + "; language = " + language + "; theme = " + theme + "; gender = " + forGender + "; keys = " + getKeywordsSet();
	}

	@Override
	public boolean equals(Object o)
	{
		Logger.e("aktt", "CTD equals()");
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CategoryTagData that = (CategoryTagData) o;

		return !(categoryID != null ? !categoryID.equals(that.categoryID) : that.categoryID != null);

	}

	@Override
	public int hashCode()
	{
		return categoryID != null ? categoryID.hashCode() : 0;
	}

}
