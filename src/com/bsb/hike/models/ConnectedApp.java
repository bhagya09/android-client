package com.bsb.hike.models;

import android.graphics.drawable.Drawable;

/**
 * Model class.
 * 
 * @author Atul M
 */
public class ConnectedApp
{

	/** The package name. */
	private String packageName;

	/** The title. */
	private String title;

	/** The app icon. */
	private Drawable appIcon;

	/** The version. */
	private String version;

	/**
	 * Gets the package name.
	 * 
	 * @return the package name
	 */
	public String getPackageName()
	{
		return packageName;
	}

	/**
	 * Sets the package name.
	 * 
	 * @param packageName
	 *            the new package name
	 */
	public void setPackageName(String packageName)
	{
		this.packageName = packageName;
	}

	/**
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * Gets the app icon.
	 * 
	 * @return the app icon
	 */
	public Drawable getAppIcon()
	{
		return appIcon;
	}

	/**
	 * Sets the app icon.
	 * 
	 * @param appIcon
	 *            the new app icon
	 */
	public void setAppIcon(Drawable appIcon)
	{
		this.appIcon = appIcon;
	}

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Sets the version.
	 * 
	 * @param version
	 *            the new version
	 */
	public void setVersion(String version)
	{
		this.version = version;
	}
}
