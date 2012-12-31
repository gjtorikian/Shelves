package com.miadzin.shelves.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Eric Bergman-Terrell
 * 
 */
public class Crypto {
	private static byte[] getPasswordMessageDigest(String password)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
		byte[] passwordMessageDigest = messageDigest.digest(password
				.getBytes("UTF-8"));

		return passwordMessageDigest;
	}

	private static SecretKey createSecretKey(String password)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			UnsupportedEncodingException {
		int keyLengthBits = 128;
		int keyLengthBytes = keyLengthBits / 8;
		String keyAlgorithm = "AES";

		byte[] passwordMessageDigest = getPasswordMessageDigest(password);

		List<Byte> passwordBytes = new ArrayList<Byte>();

		for (byte passwordByte : passwordMessageDigest) {
			passwordBytes.add(passwordByte);
		}

		if (passwordBytes.size() < keyLengthBytes) {
			passwordBytes.add((byte) 0);
		}

		while (passwordBytes.size() > keyLengthBytes) {
			passwordBytes.remove(passwordBytes.size() - 1);
		}

		byte[] passwordByteArray = new byte[keyLengthBytes];

		for (int i = 0; i < keyLengthBytes; i++) {
			passwordByteArray[i] = passwordBytes.get(i);
		}

		SecretKey secretKey = new SecretKeySpec(passwordByteArray, keyAlgorithm);

		return secretKey;
	}

	public static byte[] encrypt(String password, String plainText)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeySpecException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException {
		SecretKey secretKey = createSecretKey(password);

		Cipher cipher = Cipher.getInstance("AES");

		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		byte[] ptb = plainText.getBytes();
		byte[] cipherText = cipher.doFinal(ptb);

		return cipherText;
	}

	public static String decrypt(String password, byte[] cipherText)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeySpecException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException {
		Cipher cipher = Cipher.getInstance("AES");

		SecretKey secretKey = createSecretKey(password);

		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		byte[] plainText = cipher.doFinal(cipherText);

		return plainText.toString();
	}
}