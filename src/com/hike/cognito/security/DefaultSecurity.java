package com.hike.cognito.security;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
//TODO Finalize on algo.
public class DefaultSecurity extends Security {

    @Override
    public String encryptData(String data) {
        return data;
    }

    @Override
    public String decryptData(String data) {
        return data;
    }
}
