package com.bsb.hike.platform.auth;

public interface AuthListener
{
	public void onTokenResponse(String authToken);
	
	public void onTokenErrorResponse(String error);
	
	
}