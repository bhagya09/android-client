package com.bsb.hike.modules.stickerdownloadmgr.retry;

public interface IRetryPolicy
{
	
	int getRetryCount();
	
	void retry(Exception error) throws Exception;
	
	void reset();
}
