package com.bsb.hike.modules.stickersearch.datamodel;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by akhiltripathi on 12/04/16.
 */
public class CategoryTagData
{
	private static final String TAG = CategoryTagData.class.getSimpleName();

	private final int CATEGORY_META_TAGS_COUNT = 3;

    protected int ucid;

	private List<String> languages;

	private List<String> keywords;

	private List<String> themes;

	protected String name;

	protected int forGender;

	private long categoryLastUpdatedTime;

	public CategoryTagData(int ucid)
	{
		this.ucid = ucid;
	}

	public boolean isValid()
	{
		return  ucid >= 0;
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

	public long getCategoryLastUpdatedTime()
	{
		return categoryLastUpdatedTime;
	}

	public void setCategoryLastUpdatedTime(long categoryLastUpdatedTime)
	{
		this.categoryLastUpdatedTime = categoryLastUpdatedTime;
	}

	public List<String> getLanguages()
	{
		return languages;
	}

	public void setLanguages(List<String> languages)
	{
		this.languages = languages;
	}

	public void setLanguages(String languageSet)
	{
        if(!TextUtils.isEmpty(languageSet))
        {
            this.languages = new ArrayList<String>(Arrays.asList(languageSet.split(HikeConstants.DELIMETER)));
        }
	}

	public List<String> getThemes()
	{
		return themes;
	}

	public void setThemes(List<String> themes)
	{
		this.themes = themes;
	}

	public void setThemes(String themeSet)
	{
        if(!TextUtils.isEmpty(themeSet))
        {
            this.themes = new ArrayList<String>(Arrays.asList(themeSet.split(HikeConstants.DELIMETER)));
        }
	}

	public int getUcid()
	{
		return ucid;
	}

	public void setUcid(int ucid)
	{
		this.ucid = ucid;
	}

	public String getKeywordsString()
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

	public String getThemesString()
	{
		String result = "";

		if (Utils.isEmpty(themes))
		{
			return result;
		}

		for (String theme : themes)
		{
			if (TextUtils.isEmpty(theme))
			{
				continue;
			}

			result += theme + HikeConstants.DELIMETER;
		}

		result = result.substring(0, result.length() - 1);

		return result;

	}

	public String getLanguagesString()
	{
		String result = "";

		if (Utils.isEmpty(languages))
		{
			return result;
		}

		for (String language : languages)
		{
			if (TextUtils.isEmpty(language))
			{
				continue;
			}

			result += language + HikeConstants.DELIMETER;
		}

		result = result.substring(0, result.length() - 1);

		return result;

	}

	public List<String> getKeywords()
	{
		return keywords;
	}

	public void setKeywords(List<String> keywords)
	{
		this.keywords = keywords;
	}

	public void setKeywords(String keywordSet)
	{
        if(!TextUtils.isEmpty(keywordSet))
        {
            this.keywords = new ArrayList<String>(Arrays.asList(keywordSet.split(HikeConstants.DELIMETER)));
        }
	}

	public String getCategoryDocument()
	{
		int size = CATEGORY_META_TAGS_COUNT;

		String result = this.getName();

		if (!Utils.isEmpty(themes))
		{
			for (String theme : themes)
			{
				result += StickerSearchConstants.STRING_SPACE + theme;
			}
		}

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
		return ucid + " : name = " + name + "; language = " + getLanguagesString() + "; theme = " + getThemesString() + "; gender = " + forGender + "; keys = "
				+ getKeywordsString();
	}


	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof CategoryTagData))
			return false;

		CategoryTagData that = (CategoryTagData) o;

		return ucid == that.ucid;

	}

	@Override
	public int hashCode()
	{
		return ucid;
	}


}
