package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Set;


/**
 * 
 * @author himanshu
 * 
 * This class has all the details that will be supplied to Select friends screen ..
 *
 */
public class NuxSelectFriends
{

	private String sectionTitle;

	private String title2;
	
	private String title3;

	private String recoSectionTitle;

	private boolean toggleRecoSection;

	private Set<String> recoList;

	private ArrayList<String> hideList;

	private boolean toggleContactSection;

	private String butText;

	private boolean moduleToggle;

	private int contactSectionType;

	/**
	 * @param sectionTitle
	 * @param title2
	 * @param title3
	 * @param recoSectionTitle
	 * @param toggleRecoSection
	 * @param recoList
	 * @param hideList
	 * @param toggleContactSection
	 * @param butText
	 * @param moduleToggle
	 * @param contactSectionType
	 */
	public NuxSelectFriends(String sectionTitle, String title2, String title3, String recoSectionTitle, Set<String> recoList, ArrayList<String> hideList, String butText,
			boolean moduleToggle, int contactSectionType)
	{
		super();
		this.sectionTitle = sectionTitle;
		this.title2 = title2;
		this.title3 = title3;
		this.recoSectionTitle = recoSectionTitle;
		this.recoList = recoList;
		this.hideList = hideList;
		this.butText = butText;
		this.moduleToggle = moduleToggle;
		this.contactSectionType = contactSectionType;
	}

	/**
	 * @return the sectionTitle
	 */
	public String getSectionTitle()
	{
		return sectionTitle;
	}

	/**
	 * @return the title2
	 */
	public String getTitle2()
	{
		return title2;
	}

	/**
	 * @return the title3
	 */
	public String getTitle3()
	{
		return title3;
	}

	/**
	 * @return the recoSectionTitle
	 */
	public String getRecoSectionTitle()
	{
		return recoSectionTitle;
	}

	/**
	 * @return the toggleRecoSection
	 */
	public boolean isToggleRecoSection()
	{
		return toggleRecoSection;
	}

	/**
	 * @return the recoList
	 */
	public Set<String> getRecoList()
	{
		return recoList;
	}

	/**
	 * @return the hideList
	 */
	public ArrayList<String> getHideList()
	{
		return hideList;
	}

	/**
	 * @return the butText
	 */
	public String getButText()
	{
		return butText;
	}

	/**
	 * @return the NUXModuleToggle
	 */
	public boolean isModuleToggle()
	{
		return moduleToggle;
	}

	/**
	 * @return the contactSectionType
	 */
	public int getContactSectionType()
	{
		return contactSectionType;
	}


}
