package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.util.AesCryptographer;

import javax.crypto.SecretKey;

public class Main {

    public static void main(String[] args) throws Exception {
        final AesCryptographer aesCryptographer = new AesCryptographer();
        SecretKey secretKey = aesCryptographer.generateSecretKey().get();
        byte[] initializationVector = aesCryptographer.generateInitializationVector();

        // Text verschlüsseln
        String originalText = "Das ist ein geheimer Text!";
        String encryptedText = aesCryptographer.encrypt(originalText, secretKey, initializationVector);
        String decryptedText = aesCryptographer.decrypt(encryptedText, secretKey, initializationVector);

        System.out.println("Ursprünglich: " + originalText);
        System.out.println("Verschlüsselt: " + encryptedText);
        System.out.println("Entschlüsselt: " + decryptedText);
    }

}