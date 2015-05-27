package com.bsb.hike.offline;

public class TransferProgress
{
	public int currentChunks;

	public int totalChunks;

	public TransferProgress()
	{
		currentChunks = 0;
		totalChunks = 0;
	}

	public TransferProgress(int cChunks, int tChunks)
	{
		this.currentChunks = cChunks;
		this.totalChunks = tChunks;
	}
}