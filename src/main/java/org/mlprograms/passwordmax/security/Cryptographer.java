package org.mlprograms.passwordmax.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class Cryptographer {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    public String encrypt(SecretKey key, String plainText, byte[] aad) throws Exception {
        if (key == null || plainText == null)
            throw new IllegalArgumentException("Parameters must not be null");

        byte[] iv = new byte[IV_LENGTH];
        RNG.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        if (aad != null) cipher.updateAAD(aad);

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipherBytes, 0, out, iv.length, cipherBytes.length);

        return Base64.getEncoder().encodeToString(out);
    }

    public String decrypt(SecretKey key, String base64Input, byte[] aad) throws Exception {
        if (key == null || base64Input == null)
            throw new IllegalArgumentException("Parameters must not be null");

        byte[] all = Base64.getDecoder().decode(base64Input);
        if (all.length < IV_LENGTH)
            throw new IllegalArgumentException("Input too short");

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(all, 0, iv, 0, IV_LENGTH);
        byte[] cipherBytes = new byte[all.length - IV_LENGTH];
        System.arraycopy(all, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(CIPHER);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        if (aad != null) cipher.updateAAD(aad);

        byte[] plain = cipher.doFinal(cipherBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }

}
