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
 *
 * Model class to store Category Tag Information
 *
 */
public class CategoryTagData
{
	private static final String TAG = CategoryTagData.class.getSimpleName();

	protected int ucid;

	private List<String> languages; // Language / Script list of the Category

	private List<String> keywords; // Keywords / Tags list describing the Category

	private List<String> themes; // Category level Theme/Mood list

	protected String name; // Category Display Name

	protected int forGender; // Category Targeted Gender

	private long categoryLastUpdatedTime; // Timestamp in millis since info was last updated

	protected CategoryTagData(Builder builder)
	{
		this.ucid = builder.ucid;
		this.name = builder.name;
		this.categoryLastUpdatedTime = builder.categoryLastUpdatedTime;
		this.keywords = builder.keywords;
		this.themes = builder.themes;
		this.languages = builder.languages;
		this.forGender = builder.forGender;
	}

	public boolean isValid()
	{
		return ucid >= 0;
	}

	public String getName()
	{
		return name;
	}

	public int getGender()
	{
		return forGender;
	}

	public long getCategoryLastUpdatedTime()
	{
		return categoryLastUpdatedTime;
	}

	public List<String> getLanguages()
	{
		return languages;
	}

	public List<String> getThemes()
	{
		return themes;
	}

	public int getUcid()
	{
		return ucid;
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

	public String getCategoryDocument()
	{
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

	public void setName(String name)
	{
		this.name = name;
	}

	public void setGender(int forGender)
	{
		this.forGender = forGender;
	}

	public void setCategoryLastUpdatedTime(long categoryLastUpdatedTime)
	{
		this.categoryLastUpdatedTime = categoryLastUpdatedTime;
	}

	public void setLanguages(List<String> languages)
	{
		this.languages = languages;
	}

	public void setThemes(List<String> themes)
	{
		this.themes = themes;
	}

	public void setKeywords(List<String> keywords)
	{
		this.keywords = keywords;
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

	public static class Builder<T extends Builder>
	{

		protected int ucid;

		private List<String> languages;

		private List<String> keywords;

		private List<String> themes;

		protected String name;

		protected int forGender;

		private long categoryLastUpdatedTime;

		public Builder(int ucid)
		{
			this.ucid = ucid;
		}

		public T setLanguages(List<String> languages)
		{
			this.languages = languages;
			return (T) this;
		}

		public T setLanguages(String languageSet)
		{
			if (!TextUtils.isEmpty(languageSet))
			{
				this.languages = new ArrayList<String>(Arrays.asList(languageSet.split(HikeConstants.DELIMETER)));
			}
			return (T) this;
		}

		public T setGender(int forGender)
		{
			this.forGender = forGender;
			return (T) this;
		}

		public T setName(String name)
		{
			this.name = name;
			return (T) this;
		}

		public T setCategoryLastUpdatedTime(long categoryLastUpdatedTime)
		{
			this.categoryLastUpdatedTime = categoryLastUpdatedTime;
			return (T) this;
		}

		public T setThemes(List<String> themes)
		{
			this.themes = themes;
			return (T) this;
		}

		public T setThemes(String themeSet)
		{
			if (!TextUtils.isEmpty(themeSet))
			{
				this.themes = new ArrayList<String>(Arrays.asList(themeSet.split(HikeConstants.DELIMETER)));
			}
			return (T) this;
		}

		public T setKeywords(List<String> keywords)
		{
			this.keywords = keywords;
			return (T) this;
		}

		public T setKeywords(String keywordSet)
		{
			if (!TextUtils.isEmpty(keywordSet))
			{
				this.keywords = new ArrayList<String>(Arrays.asList(keywordSet.split(HikeConstants.DELIMETER)));
			}
			return (T) this;
		}

		public CategoryTagData build()
		{
			return new CategoryTagData(this);
		}
	}

}
