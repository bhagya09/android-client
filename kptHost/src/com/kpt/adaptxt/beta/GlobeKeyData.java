package com.kpt.adaptxt.beta;

import java.util.ArrayList;

public class GlobeKeyData {

	public static final int STATUS_OK = 1;
	public static final int STATUS_MAINTANCE_MODE = 2;
	public static final int STATUS_INTERNAL_ERROR = 3;
	
	
	private int  mStatus;
	
	private String[] mDisplayLanguages = null;
	
	private int mCurrentIndex ;
	
	private ArrayList<String>  mUnsupportedLangugeList;

	public int getStatus() {
		return mStatus;
	}

	public void setStatus(int mode) {
		this.mStatus = mode;
	}

	public String[] getDisplayLanguages() {
		return mDisplayLanguages;
	}

	public void setDisplayLanguages(String[] mDisplayLanguages) {
		this.mDisplayLanguages = mDisplayLanguages;
	}

	public int getCurrentIndex() {
		return mCurrentIndex;
	}

	public void setCurrentIndex(int mCurrentIndex) {
		this.mCurrentIndex = mCurrentIndex;
	}

	public ArrayList<String> getUnsupportedLangugeList() {
		return mUnsupportedLangugeList;
	}

	public void setUnsupportedLangugeList(ArrayList<String> list) {
		this.mUnsupportedLangugeList = list;
	}
	
	
	
	
	
	
}
