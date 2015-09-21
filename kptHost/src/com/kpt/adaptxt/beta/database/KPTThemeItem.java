/**
 * 
 */
package com.kpt.adaptxt.beta.database;



/**
 * @author KPT Developer
 * 
 */
public class KPTThemeItem {
	
	public int themeID;
	public String themeName;
	public String packageName;
	public int themeType;
	public int themeEnabled;
	public int baseId;
	public String customThemeFontColors;
	public int customThemeKeyShape;
	public byte[] themeImage;
	public String customThemeBGPath;
	public int customTransparency;
	public int customBGPrefs;
	
	public KPTThemeItem() {
		
	}

	public int getThemeID() {
		return themeID;
	}

	public void setThemeID(int themeID) {
		this.themeID = themeID;
	}

	public String getThemeName() {
		return themeName;
	}

	public void setThemeName(String themeName) {
		this.themeName = themeName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public int getThemeType() {
		return themeType;
	}

	public void setThemeType(int themeType) {
		this.themeType = themeType;
	}

	public int getBaseId() {
		return baseId;
	}

	public void setBaseId(int baseId) {
		this.baseId = baseId;
	}

	public void setThemeImage(byte[] themeImage){
		this.themeImage = themeImage;
	}
	
	public byte[] getThemeImage(){
		return themeImage;
	}

	public int getThemeEnabled() {
		return themeEnabled;
	}

	public void setThemeEnabled(int themeEnabled) {
		this.themeEnabled = themeEnabled;
	}

	public String getCustomThemeFontColors() {
		return customThemeFontColors;
	}

	public void setCustomThemeFontColors(String customThemeFontColors) {
		this.customThemeFontColors = customThemeFontColors;
	}

	public int getCustomThemeKeyShape() {
		return customThemeKeyShape;
	}

	public void setCustomThemeKeyShape(int customThemeKeyShape) {
		this.customThemeKeyShape = customThemeKeyShape;
	}

	public String getCustomThemeBGPath() {
		return customThemeBGPath;
	}

	public void setCustomThemeBGPath(String customThemeBGPath) {
		this.customThemeBGPath = customThemeBGPath;
	}

	public int getCustomTransparency() {
		return customTransparency;
	}

	public void setCustomTransparency(int customTransparency) {
		this.customTransparency = customTransparency;
	}

	public int getCustomBGPrefs() {
		return customBGPrefs;
	}

	public void setCustomBGPrefs(int customBGPrefs) {
		this.customBGPrefs = customBGPrefs;
	}

	
	
}

