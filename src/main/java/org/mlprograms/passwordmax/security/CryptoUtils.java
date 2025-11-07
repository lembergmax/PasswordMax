package org.mlprograms.passwordmax.security;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Arrays;

public class CryptoUtils {

    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int VERIFICATION_SALT_LEN = 16;
    private static final int ENCRYPTION_SALT_LEN = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 200_000;
    private static final SecureRandom RNG = new SecureRandom();

    public String createVerificationHash(char[] masterPassword) throws Exception {
        Objects.requireNonNull(masterPassword);

        byte[] salt = new byte[VERIFICATION_SALT_LEN];
        RNG.nextBytes(salt);

        byte[] hash = pbkdf2(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

        String stored = "v1:" + PBKDF2_ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);

        wipe(hash);
        return stored;
    }

    public boolean verifyPassword(char[] attempt, String stored) throws Exception {
        Objects.requireNonNull(attempt);
        Objects.requireNonNull(stored);

        String[] parts = stored.split(":");
        if (parts.length != 4 || !parts[0].equals("v1"))
            throw new IllegalArgumentException("Unsupported verification format");

        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        byte[] attemptHash = pbkdf2(attempt, salt, iterations, expectedHash.length * 8);
        boolean matches = MessageDigest.isEqual(expectedHash, attemptHash);

        wipe(attemptHash);
        wipe(expectedHash);
        return matches;
    }

    // === AES-Schlüssel für Vault-Einträge ===
    public SecretKey deriveEncryptionKey(char[] masterPassword, byte[] encryptionSalt) throws Exception {
        Objects.requireNonNull(masterPassword);
        Objects.requireNonNull(encryptionSalt);

        byte[] keyBytes = pbkdf2(masterPassword, encryptionSalt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        wipe(keyBytes);
        return key;
    }

    public String generateEncryptionSaltBase64() {
        byte[] salt = new byte[ENCRYPTION_SALT_LEN];
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] key = skf.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return key;
    }

    public void wipe(byte[] arr) {
        if (arr != null) Arrays.fill(arr, (byte) 0);
    }

    public void wipe(char[] arr) {
        if (arr != null) Arrays.fill(arr, (char) 0);
    }

}
