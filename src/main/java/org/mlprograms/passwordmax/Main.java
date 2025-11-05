package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.controller.JsonController;
import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Password;
import org.mlprograms.passwordmax.util.AesCryptographer;

import javax.crypto.SecretKey;

public class Main {

    public static void main(String[] args) throws Exception {
//        final AesCryptographer aesCryptographer = new AesCryptographer();
//        final SecretKey secretKey = aesCryptographer.getSecretKey();
//        final byte[] initializationVector = aesCryptographer.getInitializationVector();
//
//        // Beispieltext verschlüsseln
//        String original = "Das ist ein geheimer Text!";
//        String encrypted = aesCryptographer.encrypt(original, secretKey, initializationVector);
//        String decrypted = aesCryptographer.decrypt(encrypted, secretKey, initializationVector);
//
//        System.out.println("Original:     " + original);
//        System.out.println("Verschlüsselt:" + encrypted);
//        System.out.println("Entschlüsselt:" + decrypted);

        final JsonController jsonController = new JsonController();
        Account account = new Account("Max1", "1234");
        Password password = new Password("username1", "password1", "email1", "notes1");

        jsonController.addAccount(account);
        jsonController.addPasswordToAccount(account.getName(), password);

        account = new Account("Max2", "1234");
        password = new Password("username2", "password2", "email2", "notes2");

        jsonController.addAccount(account);
        jsonController.addPasswordToAccount(account.getName(), password);

        jsonController.loadDataFromJson().forEach(acc -> {
            System.out.println("Konto: " + acc.getName());
            acc.getPasswords().forEach(p ->
                    System.out.println("  - Passwort: " + p.getPassword() + " (" + p.getUsername() + ")"));
        });
    }

}