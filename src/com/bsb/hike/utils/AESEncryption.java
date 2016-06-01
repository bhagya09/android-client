package com.bsb.hike.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

import com.bsb.hike.utils.Logger;
//AES 128 bit encryption logic using an digest of a password

public class AESEncryption {
	private final static String TAG = "AESEncryption";
	private final static String encryptionAlgo  = "AES";
	private SecretKeySpec secretKey;
	private byte[] key;
	private final static String initV = "0011223344556677";
	private String decryptedString;
	private String encryptedString;

	public AESEncryption(String password, String algorithm){
		MessageDigest sha = null;
		try {
			key = password.getBytes("UTF-8");
			sha = MessageDigest.getInstance(algorithm);
			key = sha.digest(key);
			Logger.d(TAG, "Initial Length of " + algorithm + " key : " + key.length);
			key = Arrays.copyOf(key, 16); // use only first 128 bit
			/*StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < key.length; i++)
				hexString.append(Integer.toHexString(0xFF & key[i]));
			Logger.d(TAG,"Hex String of Key : " + hexString.toString());*/
			Logger.d(TAG,"Base64 String of Key : " + Base64.encode(key, 0).toString());

			secretKey = new SecretKeySpec(key, encryptionAlgo);

		} catch (NoSuchAlgorithmException e) {
			Logger.d(TAG, e.toString());
		} catch (UnsupportedEncodingException e) {
			Logger.d(TAG, e.toString());
		}
	}

	public String getDecryptedString() {
		return decryptedString;
	}

	private void setDecryptedString(String decrypted) {
		decryptedString = decrypted;
	}

	public String getEncryptedString() {
		return encryptedString;
	}

	private void setEncryptedString(String encrypted) {
		encryptedString = encrypted;
	}
	
	public String encrypt(String strToEncrypt) {
		String encrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			encrypted = new String(Base64.encode(
					cipher.doFinal(strToEncrypt.getBytes("UTF-8")),
					Base64.NO_WRAP));
			setEncryptedString(encrypted);
		} catch (Exception e) {
			Logger.d(TAG,"Error while encrypting: " + e.toString());
		}
		return encrypted;
	}

	public String decrypt(String strToDecrypt) {
		String decrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			decrypted = new String(cipher.doFinal(Base64.decode(strToDecrypt, 0)));
			setDecryptedString(decrypted);
		} catch (Exception e) {
			Logger.d(TAG,"Error while decrypting: " + e.toString());
		}
		return decrypted;
	}
}
