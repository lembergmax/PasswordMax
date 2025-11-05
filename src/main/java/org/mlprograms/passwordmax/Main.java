package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.util.AesCryptographer;

import javax.crypto.SecretKey;

public class Main {

    public static void main(String[] args) throws Exception {
        final AesCryptographer aesCryptographer = new AesCryptographer();
        final SecretKey secretKey = aesCryptographer.getSecretKey();
        final byte[] initializationVector = aesCryptographer.getInitializationVector();

        // Beispieltext verschlüsseln
        String original = "Das ist ein geheimer Text!";
        String encrypted = aesCryptographer.encrypt(original, secretKey, initializationVector);
        String decrypted = aesCryptographer.decrypt(encrypted, secretKey, initializationVector);

        System.out.println("Original:     " + original);
        System.out.println("Verschlüsselt:" + encrypted);
        System.out.println("Entschlüsselt:" + decrypted);
    }

}