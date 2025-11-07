package org.mlprograms.passwordmax.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class Cryptographer {

    private final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private final int SALT_LENGTH = 16;
    private final int INITIALIZATION_VECTOR_LENGTH = 12;
    private final int KEY_LENGTH = 256;
    private final int ITERATIONS = 100_000;
    private final String CIPHER = "AES/GCM/NoPadding";
    private final int GCM_TAG_LENGTH = 128;

    public String encrypt(final String text) {
        return encrypt(text, text);
    }

    public String encrypt(final String text, final String encryptionKey) {
        final SecureRandom secureRandom = new SecureRandom();
        try {
            final byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH];
            secureRandom.nextBytes(initializationVector);

            final SecretKey secretKey = deriveKey(encryptionKey.toCharArray(), salt);

            final Cipher cipher = Cipher.getInstance(CIPHER);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, initializationVector);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            final byte[] cipherBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            final byte[] output = new byte[salt.length + initializationVector.length + cipherBytes.length];
            System.arraycopy(salt, 0, output, 0, salt.length);
            System.arraycopy(initializationVector, 0, output, salt.length, initializationVector.length);
            System.arraycopy(cipherBytes, 0, output, salt.length + initializationVector.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(output);
        } catch (final Exception exception) {
            System.err.println("Encryption failed: " + exception.getMessage());
            throw new RuntimeException("Encryption failed");
        }
    }

    public String decrypt(final String text, final String decryptionKey) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(text);

            if (decodedBytes.length < SALT_LENGTH + INITIALIZATION_VECTOR_LENGTH) {
                throw new IllegalArgumentException("Input too short");
            }

            byte[] salt = Arrays.copyOfRange(decodedBytes, 0, SALT_LENGTH);
            byte[] initializationVector = Arrays.copyOfRange(decodedBytes, SALT_LENGTH, SALT_LENGTH + INITIALIZATION_VECTOR_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(decodedBytes, SALT_LENGTH + INITIALIZATION_VECTOR_LENGTH, decodedBytes.length);

            final SecretKey key = deriveKey(decryptionKey.toCharArray(), salt);

            final Cipher cipher = Cipher.getInstance(CIPHER);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            final byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            System.err.println("Decryption failed: " + exception.getMessage());
            throw new RuntimeException("Decryption failed");
        }
    }

    private SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        final PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] keyBytes = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

}
