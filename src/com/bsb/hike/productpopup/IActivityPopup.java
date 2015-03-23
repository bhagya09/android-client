package com.bsb.hike.productpopup;

/**
 * 
 * @author himanshu
 *
 *This interface returns the callback to the activity to either show the popup or not
 */
public interface IActivityPopup
{
	public void onSuccess(ProductContentModel productContentModel);

	public void onFailure();
}
