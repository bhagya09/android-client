package com.bsb.hike.utils;

import org.json.JSONException;
import org.json.JSONObject;

public final class Sim {

	private String imei;
	private String phoneNumber;
	private String networkOperator;
	private int roaming;
	private String countryISO;
	private int slotIndex;
	public String toJSONString()
	{
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("imei", imei);
			jsonObj.put("phone_number", phoneNumber);
			jsonObj.put("operator", networkOperator);
			jsonObj.put("roaming", roaming==1?true:false);
			jsonObj.put("countryISO",countryISO);
			jsonObj.put("slotIndex", slotIndex);
		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return jsonObj.toString();
	}

	public String getCountryISO() {
		return countryISO;
	}

	public void setCountryISO(String countryISO) {
		this.countryISO = countryISO;
	}

	public int getSlotIndex() {
		return slotIndex;
	}

	public void setSlotIndex(int slotIndex) {
		this.slotIndex = slotIndex;
	}

	public int getRoaming() {
		return roaming;
	}
	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getNetworkOperator() {
		return networkOperator;
	}

	public void setNetworkOperator(String networkOperator) {
		this.networkOperator = networkOperator;
	}

	public int isRoaming() {
		return roaming;
	}

	public void setRoaming(int roaming) {
		this.roaming = roaming;
	}


}