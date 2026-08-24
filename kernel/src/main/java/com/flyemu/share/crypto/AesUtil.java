package com.flyemu.share.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具，用于保护数据库中的敏感字段（如第三方 API 密码）。
 * 密钥通过系统属性或环境变量 "app.crypto.aes-key" 注入。
 * 默认密钥仅用于开发环境，生产环境必须更换。
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String DEFAULT_KEY = "FlyEmu@2024#JXC!"; // 16 bytes → AES-128

    private AesUtil() {
    }

    private static SecretKeySpec getKey() {
        String key = System.getProperty("app.crypto.aes-key",
                System.getenv().getOrDefault("APP_CRYPTO_AES_KEY", DEFAULT_KEY));
        byte[] keyBytes = new byte[16];
        byte[] src = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }
}
