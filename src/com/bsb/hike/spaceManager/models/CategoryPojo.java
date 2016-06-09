package com.bsb.hike.spaceManager.models;

import com.google.gson.Gson;

import java.util.List;

/**
 * @author paramshah
 */
public class CategoryPojo extends SpaceManagerPojo
{
	private List<SubCategoryPojo> subCategoryList;

	public List<SubCategoryPojo> getSubCategoryList()
	{
		return subCategoryList;
	}

	public void setSubCategoryList(List<SubCategoryPojo> subCategoryList)
	{
		this.subCategoryList = subCategoryList;
	}

	@Override
	public String toString()
	{
		return new Gson().toJson(this);
	}
}
