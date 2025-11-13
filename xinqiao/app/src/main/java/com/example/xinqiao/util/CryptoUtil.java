package com.example.xinqiao.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256 GCM encryption/decryption using Android Keystore.
 */
public class CryptoUtil {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "xq_medical_aes";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128; // bits

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        if (key != null) return key;

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(KEY_SIZE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12]; // Recommended IV length for GCM
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Combine IV + ciphertext into one buffer
            ByteBuffer bb = ByteBuffer.allocate(4 + iv.length + cipherText.length);
            bb.putInt(iv.length);
            bb.put(iv);
            bb.put(cipherText);
            return Base64.encodeToString(bb.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            // Fallback: store plaintext in a reversible format to avoid content loss
            try {
                String base64 = Base64.encodeToString(plainText.getBytes("UTF-8"), Base64.NO_WRAP);
                return "PLA:" + base64;
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    public static String decrypt(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            // Fallback format: PLA:<base64 of UTF-8 plain text>
            if (base64.startsWith("PLA:")) {
                String b64 = base64.substring(4);
                byte[] plain = Base64.decode(b64, Base64.NO_WRAP);
                return new String(plain, "UTF-8");
            }

            byte[] combined = Base64.decode(base64, Base64.NO_WRAP);
            ByteBuffer bb = ByteBuffer.wrap(combined);
            int ivLen = bb.getInt();
            byte[] iv = new byte[ivLen];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
