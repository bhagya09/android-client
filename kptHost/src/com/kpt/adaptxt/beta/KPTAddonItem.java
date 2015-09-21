package com.kpt.adaptxt.beta;

public class KPTAddonItem {
	
	private String addonID;
	private String displayName;
	private int componentId;
	
	private String fileName;
	private String zipFileName;
	//private String englishDisplayName;
	
	public void setAddonID(String addOnID) {
		this.addonID = addOnID;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public int getComponentId() {
		return componentId;
	}
	public void setComponentId(int componentId) {
		this.componentId = componentId;
	}
	public String getAddonID() {
		return addonID;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getZipFileName() {
		return zipFileName;
	}
	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}
	/*public String getEnglishDisplayName() {
		return englishDisplayName;
	}
	public void setEnglishDisplayName(String englishDisplayName) {
		this.englishDisplayName = englishDisplayName;
	}*/
	
	

}
