package com.hike.cognito.security;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public abstract class Security {

    public static Security getSecurity(String url) {
        if (url == null) {
            return new DefaultSecurity();
        } else {
            //For specific destination, use AES Algo
            return new AESSecurity();
        }

    }

    public abstract String encryptData(String data);

    public abstract String decryptData(String data);
}
