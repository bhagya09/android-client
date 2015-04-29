package com.bsb.hike.modules.stickerdownloadmgr;


public interface IStickerResultListener
{
	
	public void onSuccess(Object result);
	
	
	public void onFailure(Object result, StickerException exception);
	
	
	public void onProgressUpdated(double percentage);
	
}
