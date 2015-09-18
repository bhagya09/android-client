package com.hike.transporter.models;

/**
 * 
 * @author himanshu/GauravK
 * 
 * This class contains all the information of the topic
 *
 */
public class Topic
{
	private final String name;

	private int fixedThreads = 1, maxThreads = 1;

	public Topic(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setFixedThreads(int fixedThreads)
	{
		this.fixedThreads = fixedThreads;
	}

	public int getFixedThreads()
	{
		return fixedThreads;
	}

	public int getMaxThreads()
	{
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}

}
