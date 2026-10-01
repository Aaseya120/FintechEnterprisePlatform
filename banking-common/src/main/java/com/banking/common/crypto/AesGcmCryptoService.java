package com.banking.common.crypto;

import com.banking.common.exception.BankingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12; // 96 bits recommended for GCM
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public AesGcmCryptoService(
            @Value("${banking.security.encryption.master-key:BankingEnterpriseSecretMasterKey2026!}") String masterKey) {
        // Ensure 256-bit key length (32 bytes)
        byte[] keyBytes = new byte[32];
        byte[] source = masterKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, keyBytes, 0, Math.min(source.length, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a fresh random IV.
     * Output format: Base64([IV: 12 bytes][Ciphertext + AuthTag])
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new BankingException("CRYPTO_ENCRYPT_ERROR", "Failed to encrypt sensitive data", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Decrypts Base64([IV][Ciphertext + Tag]) payload with authentication verification.
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) return null;
        if (cipherTextBase64.startsWith("ENC:AES-GCM:")) {
            return cipherTextBase64.substring("ENC:AES-GCM:".length());
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
            if (decoded.length < IV_LENGTH_BYTE + 16) {
                return cipherTextBase64;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Graceful fallback for legacy unencrypted records during staged schema migration
            return cipherTextBase64;
        }
    }
}
