package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

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

        // Füge weitere Einträge zu dem bestehenden Account hinzu (Batch)
        final Entry more1 = new Entry("Forum", "forumPass", "Forum Account", "https://forum.example", "maxForum", "", "");
        final Entry more2 = new Entry("E-Mail Konto", "anotherPass", "Doppelter Name Test", "", "", "alt@example.com", "");
        // more2 hat denselben Namen wie entry1 in addSampleData -> wird übersprungen
        accountManager.addEntries(account, masterPassword, List.of(more1, more2));

        // Speichere die Änderungen
        accountManager.saveAccount(account);

        final SecretKey aesKey = login(masterPassword, account);
        displayVault(account, aesKey);
    }

    private static void addSampleData(final Account account, final String masterPassword, final AccountManager accountManager) {
        final Entry entry1 = new Entry(
                "E-Mail Konto",
                "12345!@#$%",
                "Mein Haupt E-Mail Konto",
                "",
                "",
                "max.mustermann@gmail.com",
                ""
        );

        final Entry entry2 = new Entry(
                "Social Media",
                "pa$$w0rd",
                "Mein Social Media Konto",
                "https://www.socialmedia.com",
                "maxmustermann",
                "",
                "Hier sind meine Social Media Zugangsdaten."
        );

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
            System.out.println(entry);
        }
    }

}
