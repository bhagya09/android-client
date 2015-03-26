package com.bsb.hike.voip;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;

import com.bsb.hike.utils.Logger;


public class VoIPEncryptor {
	private static final int RSA_KEY_SIZE = 2048;
	private static final int AES_KEY_SIZE = 256;		// This depends on the RSA_KEY_SIZE 
	
	private KeyPairGenerator kpg = null;
	private Key publicKey = null;
	private Key privateKey = null;
	
	private SecretKeySpec aesKeySpec = null;
	private Cipher aesEncryptCipher = null;
	private Cipher aesDecryptCipher = null;
	
	private byte[] sessionKey = null;
	
	private SecureRandom sr;
	
	enum EncryptionStage {
		STAGE_INITIAL,
		STAGE_GOT_PUBLIC_KEY,
		STAGE_GOT_SESSION_KEY,
		STAGE_READY
	}
	
	@SuppressLint("TrulyRandom") public VoIPEncryptor() {
		kpg = null;
		publicKey = null;
		privateKey = null;
		aesKeySpec = null;
		aesEncryptCipher = null;
		aesDecryptCipher = null;
		
		PRNGFixes.apply();
		sr = new SecureRandom();

	}

	public void initKeys() {
		if (kpg != null)
			return;		
		
		try {
			// Get RSA public / private keys
			kpg = KeyPairGenerator.getInstance("RSA", "BC");
			kpg.initialize(RSA_KEY_SIZE, sr);
			KeyPair kp = kpg.genKeyPair();
			publicKey = kp.getPublic();
			privateKey = kp.getPrivate();
			
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchAlgorithmException: " + e.toString());
		} catch (NoSuchProviderException e) {
			Logger.d(VoIPConstants.TAG, "initKeys NoSuchProviderException: " + e.toString());
		}
	}
	
	public void initSessionKey() {

		if (sessionKey != null)
			return;
		
		// Generate session key
		sessionKey = new byte[AES_KEY_SIZE / 8];
		Logger.d(VoIPConstants.TAG, "New AES key generated.");
		sr.nextBytes(sessionKey);
		aesKeySpec = null;
		aesDecryptCipher = null;
		aesEncryptCipher = null;
	}
	
	public byte[] getSessionKey() {
		return sessionKey;
	}
	
	public void setSessionKey(byte[] sessionKey) {
		this.sessionKey = sessionKey;
	}
	
	public byte[] getPublicKey() {
		if (publicKey != null)
			return publicKey.getEncoded();
		else
			return null;
	}
	
	public void setPublicKey(byte[] pubKey) {
		
		if (publicKey != null)
			return;
		
		try {
			publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKey));
		} catch (InvalidKeySpecException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeySpecException: " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchAlgorithmException: " + e.toString());
		}
	}

	public byte[] rsaEncrypt(byte[] data, byte[] pubKey) {
		
		if (pubKey == null) {
			Logger.e(VoIPConstants.TAG, "rsaEncrypt() called, but I have no public key.");
			return null;
		}

		byte[] encryptedData = null;
		
		try {
			PublicKey key = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pubKey));
			Cipher cipher = Cipher.getInstance("RSA", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			encryptedData = cipher.doFinal(data);
			
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "rsaEncrypt NoSuchAlgorithmException: " + e.toString());
		} catch (NoSuchPaddingException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchPaddingException: " + e.toString());
		} catch (InvalidKeyException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeyException: " + e.toString());
		} catch (IllegalBlockSizeException e) {
			Logger.d(VoIPConstants.TAG, "IllegalBlockSizeException: " + e.toString());
		} catch (BadPaddingException e) {
			Logger.d(VoIPConstants.TAG, "BadPaddingException: " + e.toString());
		} catch (InvalidKeySpecException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeySpecException: " + e.toString());
		} catch (NoSuchProviderException e) {
			Logger.d(VoIPConstants.TAG, "rsaEncrypt NoSuchProviderException: " + e.toString());
		}

		return encryptedData;
	}
	
	public byte[] rsaDecrypt(byte[] data) {
		byte[] decryptedData = null;
		
		try {
			Cipher cipher = Cipher.getInstance("RSA", "BC");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			decryptedData = cipher.doFinal(data);
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchAlgorithmException: " + e.toString());
		} catch (NoSuchPaddingException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchPaddingException: " + e.toString());
		} catch (InvalidKeyException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeyException: " + e.toString());
		} catch (IllegalBlockSizeException e) {
			Logger.d(VoIPConstants.TAG, "IllegalBlockSizeException: " + e.toString());
		} catch (BadPaddingException e) {
			Logger.d(VoIPConstants.TAG, "BadPaddingException: " + e.toString());
		} catch (NoSuchProviderException e) {
			Logger.d(VoIPConstants.TAG, "rsaDecrypt NoSuchProviderException: " + e.toString());
		}
		
		return decryptedData;
	}
	
	public byte[] aesEncrypt(byte[] data) {
		byte[] encryptedData = null;
		
		if (sessionKey == null) {
			Logger.e(VoIPConstants.TAG, "aesEncrypt() called, but I have no session key.");
			return null;
		}
		
		try {
			if (aesKeySpec == null)
				aesKeySpec = new SecretKeySpec(sessionKey, "AES");
			if (aesEncryptCipher == null) {
				aesEncryptCipher = Cipher.getInstance("AES", "BC");
				aesEncryptCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec);
			}
			encryptedData = aesEncryptCipher.doFinal(data);
			
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchAlgorithmException: " + e.toString());
		} catch (NoSuchPaddingException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchPaddingException: " + e.toString());
		} catch (InvalidKeyException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeyException: " + e.toString());
		} catch (IllegalBlockSizeException e) {
			Logger.d(VoIPConstants.TAG, "IllegalBlockSizeException: " + e.toString());
		} catch (BadPaddingException e) {
			Logger.d(VoIPConstants.TAG, "BadPaddingException: " + e.toString());
		} catch (NoSuchProviderException e) {
			Logger.d(VoIPConstants.TAG, "aesEncrypt NoSuchProviderException: " + e.toString());
		}
		
		return encryptedData;
	}

	public byte[] aesDecrypt(byte[] data) {
		byte[] decryptedData = null;
		
		if (sessionKey == null) {
			Logger.e(VoIPConstants.TAG, "aesDecrypt() called, but I have no session key.");
			return null;
		}
		
		try {
			if (aesKeySpec == null)
				aesKeySpec = new SecretKeySpec(sessionKey, "AES");
			if (aesDecryptCipher == null) {
				aesDecryptCipher = Cipher.getInstance("AES", "BC");
				aesDecryptCipher.init(Cipher.DECRYPT_MODE, aesKeySpec);
			}
			decryptedData = aesDecryptCipher.doFinal(data);
			
		} catch (NoSuchAlgorithmException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchAlgorithmException: " + e.toString());
		} catch (NoSuchPaddingException e) {
			Logger.d(VoIPConstants.TAG, "NoSuchPaddingException: " + e.toString());
		} catch (InvalidKeyException e) {
			Logger.d(VoIPConstants.TAG, "InvalidKeyException: " + e.toString());
		} catch (IllegalBlockSizeException e) {
			Logger.d(VoIPConstants.TAG, "IllegalBlockSizeException: " + e.toString());
		} catch (BadPaddingException e) {
			Logger.d(VoIPConstants.TAG, "BadPaddingException: " + e.toString());
		} catch (NoSuchProviderException e) {
			Logger.d(VoIPConstants.TAG, "aesDecrypt NoSuchProviderException: " + e.toString());
		}
		
		return decryptedData;
	}

}
