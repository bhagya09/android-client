package com.bsb.hike.modules.httpmgr.request;

/**
 * This interface is used by upload and download with resume functionality requests. The upload/download request caller can set their own get chunksize policy which we will be used
 * internally by http manager during request execution
 */
public interface IGetChunkSize
{
	/**
	 * Return an int that will be used as chunk size during upload and download, It will be called after each chunk size gets uploaded/downloaded
	 * 
	 * @return
	 */
	int getChunkSize();
}
