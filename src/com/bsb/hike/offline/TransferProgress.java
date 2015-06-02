package com.bsb.hike.offline;

/**
 * 
 * @author himanshu
 *	This class deals with the amount of file transferred in case of Offline Messaging
 */
public class TransferProgress
{
	private int currentChunks;

	private int totalChunks;

	public TransferProgress()
	{
		setCurrentChunks(0);
		setTotalChunks(0);
	}

	public TransferProgress(int cChunks, int tChunks)
	{
		this.setCurrentChunks(cChunks);
		this.setTotalChunks(tChunks);
	}

	/**
	 * @return the currentChunks
	 */
	public int getCurrentChunks()
	{
		return currentChunks;
	}

	/**
	 * @param currentChunks the currentChunks to set
	 */
	public void setCurrentChunks(int currentChunks)
	{
		this.currentChunks = currentChunks;
	}

	/**
	 * @return the totalChunks
	 */
	public int getTotalChunks()
	{
		return totalChunks;
	}

	/**
	 * @param totalChunks the totalChunks to set
	 */
	public void setTotalChunks(int totalChunks)
	{
		this.totalChunks = totalChunks;
	}
}