package org.mlprograms.passwordmax.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class AesCryptographer {

    private final int INITIALIZATION_VECTOR_SIZE = 256;
    private final int KEY_SIZE = 256;

    private final String ALGORITHM = "AES/GCM/NoPadding";
    private final int TAG_LENGTH_BIT = 128;

    private final Path secretKeyPath;
    private final Path initializationVectorPath;

    public AesCryptographer(final String username) {
        final FolderController folderController = new FolderController();
        folderController.createAppFolders();
        folderController.createUserKeyFolder(username);

        this.secretKeyPath = folderController.getKeyFolder().resolve(username + ".key");
        this.initializationVectorPath = folderController.getKeyFolder().resolve(username + ".iv");
    }

    private Optional<SecretKey> generateSecretKey() {
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

    private byte[] generateInitializationVector() {
        final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_SIZE];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);

        return initializationVector;
    }

    public String encrypt(final String text, final SecretKey secretKey, final byte[] initializationVector) {
        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, initializationVector);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            final byte[] ciphertext = cipher.doFinal(text.getBytes());
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (final Exception exception) {
            log.error("Fehler beim Verschlüsseln des Textes", exception);
            throw new RuntimeException(exception);
        }
    }

    public String decrypt(final String text, final SecretKey secretKey, final byte[] initializationVector) {
        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, initializationVector);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            final byte[] decodedText = Base64.getDecoder().decode(text);
            final byte[] plaintext = cipher.doFinal(decodedText);
            return new String(plaintext);
        } catch (final Exception exception) {
            log.error("Fehler beim Entschlüsseln des Textes", exception);
            throw new RuntimeException(exception);
        }
    }

    public SecretKey getSecretKey() {
        try {
            if (Files.exists(secretKeyPath)) {
                return loadSecretKey(secretKeyPath);
            }

            final SecretKey secretKey = generateSecretKey().orElseThrow();
            saveSecretKey(secretKey, secretKeyPath);
            return secretKey;
        } catch (final Exception exception) {
            log.error("Fehler beim Laden des geheimen Schlüssels", exception);
            throw new RuntimeException(exception);
        }
    }

    public byte[] getInitializationVector() {
        try {
            if (Files.exists(initializationVectorPath)) {
                return loadInitializationVector(initializationVectorPath);
            }

            final byte[] initializationVector = generateInitializationVector();
            saveInitializationVector(initializationVector, initializationVectorPath);
            return initializationVector;
        } catch (final Exception exception) {
            log.error("Fehler beim Laden des Initialisierungsvektors", exception);
            throw new RuntimeException(exception);
        }
    }


    private void saveSecretKey(final SecretKey secretKey, final Path filePath) throws Exception {
        final String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        Files.writeString(filePath, base64Key);
    }

    private SecretKey loadSecretKey(final Path filePath) throws Exception {
        final String base64Key = Files.readString(filePath);
        final byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, "AES");
    }

    private void saveInitializationVector(final byte[] initializationVector, final Path filePath) throws Exception {
        final String base64Iv = Base64.getEncoder().encodeToString(initializationVector);
        Files.writeString(filePath, base64Iv);
    }

    private byte[] loadInitializationVector(final Path filePath) throws Exception {
        final String base64Iv = Files.readString(filePath);
        return Base64.getDecoder().decode(base64Iv);
    }

}
