package com.banking.common.crypto;

import com.banking.common.exception.BankingException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AesGcmCryptoService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12; // 96 bits recommended for GCM
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public AesGcmCryptoService(
            @Value("${banking.security.encryption.master-key}") String masterKey) {
        // Ensure 256-bit key length (32 bytes)
        byte[] keyBytes = new byte[32];
        byte[] source = masterKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, keyBytes, 0, Math.min(source.length, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @PostConstruct
    void validateKeyConfiguration() {
        // Smoke-test: ensure key can encrypt/decrypt a known value
        String testValue = "crypto_init_selftest";
        String encrypted = encrypt(testValue);
        String decrypted = decrypt(encrypted);
        if (!testValue.equals(decrypted)) {
            throw new IllegalStateException("AES-GCM encryption self-test failed. Master key may be corrupted or misconfigured.");
        }
        log.info("AES-256-GCM encryption service initialized and self-test passed successfully.");
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
     * Throws BankingException on tampered or corrupt ciphertext instead of silently returning raw data.
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
            if (decoded.length < IV_LENGTH_BYTE + 16) {
                throw new BankingException("CRYPTO_DECRYPT_ERROR",
                        "Ciphertext too short to contain valid AES-GCM payload (IV + AuthTag minimum)",
                        HttpStatus.INTERNAL_SERVER_ERROR);
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
        } catch (BankingException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            // Not valid Base64 — likely a legacy plaintext value from pre-encryption migration
            log.warn("Decrypt encountered non-Base64 value (likely pre-encryption legacy data): returning as-is");
            return cipherTextBase64;
        } catch (Exception e) {
            throw new BankingException("CRYPTO_DECRYPT_ERROR",
                    "Failed to decrypt data. Ciphertext may be tampered or encrypted with a different key.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
