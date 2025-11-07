package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.Base64;

public final class Main {

    private static final CryptoUtils cryptoUtils = new CryptoUtils();
    private static final Cryptographer cryptographer = new Cryptographer();
    private static final byte[] ADDITIONAL_AUTHENTICATED_DATA = "user:1234".getBytes();

    public static void main(final String[] args) throws Exception {
        final AccountManager accountManager = new AccountManager();

        final String masterPassword = "Masterpasswort";

        // Erstelle neuen Account (nur in-memory). Um dauerhaft zu speichern, rufe saveAccount auf.
        final Account account = accountManager.createAccount("max", masterPassword);

        addSampleData(account, masterPassword, accountManager);

        // Speichere Account in die Standard-Datei
        accountManager.saveAccount(account);

        final SecretKey aesKey = login(masterPassword, account);
        displayVault(account, aesKey);
    }

    private static void addSampleData(final Account account, final String masterPassword, final AccountManager accountManager) {
        // Beispiel-Entries hinzufügen (Passwörter werden im addEntry verschlüsselt)
        final Entry entry1 = new Entry();
        entry1.setEntryName("ExampleSite");
        entry1.setEncryptedPassword("password123");
        entry1.setUrl("https://example.com");
        entry1.setUsername("max");

        final Entry entry2 = new Entry();
        entry2.setEntryName("MailService");
        entry2.setEncryptedPassword("mailpass");
        entry2.setUrl("https://mail.example.com");
        entry2.setUsername("max@mail.com");

        accountManager.addEntry(account, masterPassword, entry1);
        accountManager.addEntry(account, masterPassword, entry2);
    }

    private static SecretKey login(final String masterPassword, final Account account) throws Exception {
        if (!cryptoUtils.verifyPassword(masterPassword, account.getVerificationHash())) {
            throw new IllegalArgumentException("Falsches Passwort!");
        }

        return cryptoUtils.deriveEncryptionKey(
                masterPassword,
                Base64.getDecoder().decode(account.getEncryptionSaltBase64())
        );
    }

    private static void displayVault(final Account account, final SecretKey aesKey) throws Exception {
        for (final Entry entry : account.getEntries()) {
            entry.decrypt(aesKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
            System.out.println("Eintrag: " + entry.getEntryName());
            System.out.println("  Passwort: " + entry.getEncryptedPassword());
            System.out.println("  URL: " + entry.getUrl());
            System.out.println("  Username: " + entry.getUsername());
            System.out.println();
        }
    }

}
