package com.bsb.hike.offline;

/**
 * 
 * @author himanshu
 *	This class deals with the amount of file transferred in case of Offline Messaging
 */
public class TransferProgress
{
	private long currentChunks;

	private long totalChunks;

	public TransferProgress()
	{
		setCurrentChunks(0);
		setTotalChunks(0);
	}

	public TransferProgress(long cChunks, long tChunks)
	{
		this.setCurrentChunks(cChunks);
		this.setTotalChunks(tChunks);
	}

	/**
	 * @return the currentChunks
	 */
	public long getCurrentChunks()
	{
		return currentChunks;
	}

	/**
	 * @param currentChunks the currentChunks to set
	 */
	public void setCurrentChunks(long currentChunks)
	{
		this.currentChunks = currentChunks;
	}

	/**
	 * @return the totalChunks
	 */
	public long getTotalChunks()
	{
		return totalChunks;
	}

	/**
	 * @param totalChunks the totalChunks to set
	 */
	public void setTotalChunks(long totalChunks)
	{
		this.totalChunks = totalChunks;
	}
}