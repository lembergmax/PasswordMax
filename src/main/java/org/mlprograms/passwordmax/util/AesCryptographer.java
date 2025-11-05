package org.mlprograms.passwordmax.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class AesCryptographer {

    private final int INITIALIZATION_VECTOR_SIZE = 256;
    private final int KEY_SIZE = 256;

    private final String ALGORITHM = "AES/GCM/NoPadding";
    private final int TAG_LENGTH_BIT = 128;

    public Optional<SecretKey> generateSecretKey() {
        Optional<SecretKey> result;

        try {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_SIZE);
            result = Optional.of(keyGenerator.generateKey());
        } catch (final Exception exception) {
            log.error("Fehler beim Generieren des geheimen Schlüssels", exception);
            result = Optional.empty();
        }

        return result;
    }

    public byte[] generateInitializationVector() {
        final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_SIZE];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);

        return initializationVector;
    }

    public String encrypt(final String text, final SecretKey secretKey, final byte[] initializationVector) throws Exception {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, initializationVector);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        final byte[] ciphertext = cipher.doFinal(text.getBytes());
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public String decrypt(final String text, final SecretKey secretKey, final byte[] initializationVector) throws Exception {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, initializationVector);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        final byte[] decodedText = Base64.getDecoder().decode(text);
        final byte[] plaintext = cipher.doFinal(decodedText);
        return new String(plaintext);
    }

}
