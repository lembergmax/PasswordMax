package org.mlprograms.passwordmax.security;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class CryptoUtils {

    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int VERIFICATION_SALT_LEN = 16;
    private static final int ENCRYPTION_SALT_LEN = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 200_000;

    public String createVerificationHash(final String masterPasswordString) throws Exception {
        Objects.requireNonNull(masterPasswordString);
        final char[] masterPassword = masterPasswordString.toCharArray();

        final byte[] salt = new byte[VERIFICATION_SALT_LEN];
        new SecureRandom().nextBytes(salt);

        final byte[] hash = pbkdf2(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

        return "v1:" + PBKDF2_ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    public boolean verifyPassword(final String attemptString, final String stored) throws Exception {
        Objects.requireNonNull(attemptString);
        Objects.requireNonNull(stored);
        final char[] attempt = attemptString.toCharArray();

        final String[] parts = stored.split(":");
        if (parts.length != 4 || !parts[0].equals("v1")) {
            throw new IllegalArgumentException("Unsupported verification format");
        }

        final int iterations = Integer.parseInt(parts[1]);
        final byte[] salt = Base64.getDecoder().decode(parts[2]);
        final byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        final byte[] attemptHash = pbkdf2(attempt, salt, iterations, expectedHash.length * 8);
        return MessageDigest.isEqual(expectedHash, attemptHash);
    }

    public SecretKey deriveEncryptionKey(final String masterPasswordString, final byte[] encryptionSalt) throws Exception {
        Objects.requireNonNull(masterPasswordString);
        Objects.requireNonNull(encryptionSalt);
        final char[] masterPassword = masterPasswordString.toCharArray();

        final byte[] keyBytes = pbkdf2(masterPassword, encryptionSalt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String generateEncryptionSaltBase64() {
        final byte[] salt = new byte[ENCRYPTION_SALT_LEN];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private byte[] pbkdf2(final char[] password, final byte[] salt, final int iterations, final int keyLengthBits) throws Exception {
        final PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KDF_ALGO);
        final byte[] key = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        pbeKeySpec.clearPassword();
        return key;
    }

}
