package org.mlprograms.passwordmax.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class Cryptographer {

    private final String CIPHER = "AES/GCM/NoPadding";
    private final int INITIALIZATION_VECTOR_LENGTH = 12;
    private final int GCM_TAG_BITS = 128;

    public String encrypt(final SecretKey key, final String plainText, final byte[] aad) {
        try {
            if (key == null || plainText == null) {
                throw new IllegalArgumentException("Parameters must not be null");
            }

            final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH];
            new SecureRandom().nextBytes(initializationVector);

            final Cipher cipher = Cipher.getInstance(CIPHER);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_BITS, initializationVector);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

            if (aad != null) {
                cipher.updateAAD(aad);
            }

            final byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            final byte[] output = new byte[initializationVector.length + cipherBytes.length];
            System.arraycopy(initializationVector, 0, output, 0, initializationVector.length);
            System.arraycopy(cipherBytes, 0, output, initializationVector.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(output);
        } catch (final Exception exception) {
            System.err.println("Encryption failed");
            throw new RuntimeException("Encryption failed");
        }
    }

    public String decrypt(final SecretKey key, final String base64Input, final byte[] aad) {
        try {
            if (key == null || base64Input == null) {
                throw new IllegalArgumentException("Parameters must not be null");
            }

            final byte[] all = Base64.getDecoder().decode(base64Input);
            if (all.length < INITIALIZATION_VECTOR_LENGTH) {
                throw new IllegalArgumentException("Input too short");
            }

            final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH];
            System.arraycopy(all, 0, initializationVector, 0, INITIALIZATION_VECTOR_LENGTH);

            final byte[] cipherBytes = new byte[all.length - INITIALIZATION_VECTOR_LENGTH];
            System.arraycopy(all, INITIALIZATION_VECTOR_LENGTH, cipherBytes, 0, cipherBytes.length);

            final Cipher cipher = Cipher.getInstance(CIPHER);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_BITS, initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            if (aad != null) {
                cipher.updateAAD(aad);
            }

            final byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            System.err.println("Encryption failed");
            throw new RuntimeException("Encryption failed");
        }
    }

}
