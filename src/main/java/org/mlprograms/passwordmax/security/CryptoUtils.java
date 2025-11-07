package org.mlprograms.passwordmax.security;

import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@NoArgsConstructor
public class CryptoUtils {

    private final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private final int VERIFICATION_SALT_LEN = 16;
    private final int ENCRYPTION_SALT_LEN = 16;
    private final int KEY_LENGTH_BITS = 256;
    private final int PBKDF2_ITERATIONS = 200_000;

    public String createVerificationHash(char[] masterPassword) throws Exception {
        Objects.requireNonNull(masterPassword);

        final byte[] salt = new byte[VERIFICATION_SALT_LEN];
        new SecureRandom().nextBytes(salt);

        final byte[] hash = pbkdf2(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

        final String stored = "v1:" + PBKDF2_ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);

        wipe(hash);
        return stored;
    }

    public boolean verifyPassword(final char[] attempt, final String stored) throws Exception {
        Objects.requireNonNull(attempt);
        Objects.requireNonNull(stored);

        final String[] parts = stored.split(":");
        if (parts.length != 4 || !parts[0].equals("v1")) {
            throw new IllegalArgumentException("Unsupported verification format");
        }

        final int iterations = Integer.parseInt(parts[1]);
        final byte[] salt = Base64.getDecoder().decode(parts[2]);
        final byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        final byte[] attemptHash = pbkdf2(attempt, salt, iterations, expectedHash.length * 8);
        final boolean matches = MessageDigest.isEqual(expectedHash, attemptHash);

        wipe(attemptHash);
        wipe(expectedHash);

        return matches;
    }

    public SecretKey deriveEncryptionKey(final char[] masterPassword, final byte[] encryptionSalt) throws Exception {
        Objects.requireNonNull(masterPassword);
        Objects.requireNonNull(encryptionSalt);

        final byte[] keyBytes = pbkdf2(masterPassword, encryptionSalt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        final SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        wipe(keyBytes);
        return secretKey;
    }

    public String generateEncryptionSaltBase64() {
        final byte[] salt = new byte[ENCRYPTION_SALT_LEN];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private byte[] pbkdf2(final char[] password, final byte[] salt, final int iterations, final int keyLengthBits) throws Exception {
        final PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KDF_ALGO);
        final byte[] secretKey = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        pbeKeySpec.clearPassword();
        return secretKey;
    }

    public void wipe(byte[] arr) {
        if (arr == null) {
            return;
        }

        Arrays.fill(arr, (byte) 0);
    }

    public void wipe(char[] arr) {
        if (arr == null) {
            return;
        }

        Arrays.fill(arr, (char) 0);
    }

}
