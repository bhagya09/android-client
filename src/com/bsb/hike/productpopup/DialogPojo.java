package com.bsb.hike.productpopup;


/**
 * 
 * @author himanshu
 * 
 * This class has the information that the HikeDialogFragment may need to function properly.
 *
 */
public class DialogPojo
{

	boolean isFullScreen;

	int heigth;

	String html;
	
	Object data;

	
	/**
	 * @param isFullScreen
	 * @param heigth
	 * @param html
	 */
	public DialogPojo(boolean isFullScreen, int heigth, String html)
	{
		this(isFullScreen,heigth,html,null);
	}

	
	
	/**
	 * @param isFullScreen
	 * @param height
	 * @param html
	 * @param data
	 */
	public DialogPojo(boolean isFullScreen, int heigth, String html, Object data)
	{
		this.isFullScreen = isFullScreen;
		this.heigth = heigth;
		this.html = html;
		this.data = data;
	}



	/**
	 * @return the isFullScreen
	 */
	public boolean isFullScreen()
	{
		return isFullScreen;
	}

	/**
	 * @return the heigth
	 */
	public int getHeight()
	{
		return heigth;
	}

	/**
	 * @return the html
	 */
	public String getFormedData()
	{
		return html;
	}
	
	/**
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}


	/**
	 * @param data the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}


	

	
}
