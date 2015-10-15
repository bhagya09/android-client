package com.bsb.hike.platform.content;

import com.bsb.hike.platform.content.PlatformContent.EventCode;

/**
 * The listener interface for receiving platformContent events. The class that is interested in processing a platformContent event implements this interface, and the object created
 * with that class is registered with a component using the component's <code>addPlatformContentListener<code> method. When
 * the platformContent event occurs, that object's appropriate
 * method is invoked.
 * 
 * @param <T>
 *            the generic type
 * @see PlatformContentEvent
 */
public abstract class PlatformContentListener<T>
{

	/**
	 * On complete.
	 * 
	 * @param content
	 *            the content
	 */
	public abstract void onComplete(T content);

	public abstract void onEventOccured(int uniqueId,EventCode event);

	/**
	 * Implement this to set the current downloaded content length.
	 * @param length
	 */
	public void downloadedContentLength(long length)
	{
		return;
	}
}
