package com.bsb.hike.models;


/**
 * 
 * @author himanshu
 *This class has all the details regarding the nux task
 */
		

public class NUXTaskDetails
{

	private String incentiveId;

	private String activityId;

	private int min;

	private int incentiveAmount;

	/**
	 * @param incentiveId
	 * @param activityId
	 * @param incrMax
	 * @param incrMin
	 * @param min
	 * @param max
	 * @param incentiveAmount
	 */
	public NUXTaskDetails(String incentiveId, String activityId, int min, int max, int incentiveAmount)
	{
		super();
		this.incentiveId = incentiveId;
		this.activityId = activityId;
		this.min = min;
		this.max = max;
		this.incentiveAmount = incentiveAmount;
	}

	private int max;

	/**
	 * @return the incentiveId
	 */
	public String getIncentiveId()
	{
		return incentiveId;
	}

	/**
	 * @return the activityId
	 */
	public String getActivityId()
	{
		return activityId;
	}

	/**
	 * @return the min
	 */
	public int getMin()
	{
		return min;
	}

	/**
	 * @return the max
	 */
	public int getMax()
	{
		return max;
	}

	public int getIncentiveAmount()
	{
		return incentiveAmount;
	}
}
