/**
 * 
 */
package com.bsb.hike.platform.bridge;

/**
 * This class serves as a callback for various events happening on the Bridge
 * 
 * @author piyush
 * 
 */
public interface IBridgeCallback
{
	public void overflowMenuUpdated();

	public void openFullPage(String url);
	
	public void openFullPageWithTitle(String url, String title);

	public void changeActionBarTitle(String title);
	
	public void changeStatusBarColor(String color);
	
}
