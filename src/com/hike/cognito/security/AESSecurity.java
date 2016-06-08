package com.hike.cognito.security;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.AESEncryption;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public class AESSecurity extends Security {
    private static final String HASH_SCHEME = "MD5";

    @Override
    public String encryptData(String data) {
        HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
        String key, salt;
        if (Utils.isMsisdnVerified(HikeMessengerApp.getInstance().getApplicationContext())) {
            key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
            salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
        } else {
            key = settings.getData(HikeConstants.Preactivation.UID, null);
            //for the case when AI packet will not send us the backup Token
            salt = settings.getData(HikeConstants.Preactivation.ENCRYPT_KEY, null);
            // if salt or key is empty, we do not send anything
        }

        AESEncryption aesObj = new AESEncryption(key + salt, HASH_SCHEME);
        return aesObj.encrypt(data);
    }

    @Override
    public String decryptData(String data) {
        return null;
    }
}
