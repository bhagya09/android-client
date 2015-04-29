package com.bsb.hike.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;

import com.bsb.hike.utils.Logger;

public class CustomTypeFace
{

	public Typeface bold;

	public Typeface thin;

	public Typeface normal;

	public Typeface medium;

	public String fontName;

	public static ArrayList<CustomTypeFace> customTypeFaceList = new ArrayList<CustomTypeFace>();

	public CustomTypeFace(Context context, String name)
	{
		if (name == null)
		{
			name = "roboto";
		}
		fontName = name;
		name = new String(name.substring(0, 1).toUpperCase() + name.substring(1));
		try
		{
			bold = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + "-Bold.ttf");
		}
		catch (Exception e)
		{
			Logger.w(getClass().getSimpleName(), "Font not found", e);
		}
		try
		{
			thin = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + "-Light.ttf");
		}
		catch (Exception e)
		{
			Logger.w(getClass().getSimpleName(), "Font not found", e);
		}
		try
		{
			normal = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + "-Regular.ttf");
		}
		catch (Exception e)
		{
			Logger.w(getClass().getSimpleName(), "Font not found", e);
		}
		try
		{
			medium = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + "-Medium.ttf");
		}
		catch (Exception e)
		{
			Logger.w(getClass().getSimpleName(), "Font not found", e);
		}
	}

	public static CustomTypeFace getTypeFace(String fontName)
	{
		for (CustomTypeFace customTypeFace : customTypeFaceList)
		{
			if (customTypeFace.fontName.equals(fontName))
			{
				return customTypeFace;
			}
		}
		return null;
	}
}