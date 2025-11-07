package org.mlprograms.passwordmax.persistence;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;

public class AccountManager {

    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final byte[] ADDITIONAL_AUTHENTICATED_DATA = "user:1234".getBytes();
    private final AccountStorage storage = new AccountStorage();

    /**
     * Erstellt ein neues Account-Objekt (in-memory). Um es persistent zu speichern, aufrufen von saveAccount erforderlich.
     */
    public Account createAccount(final String username, final String masterPassword) throws Exception {
        final String verificationHash = cryptoUtils.createVerificationHash(masterPassword);
        final String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();

        final Account account = new Account(username, verificationHash, encryptionSaltBase64, new ArrayList<>());
        System.out.println("Account erstellt: " + username);
        return account;
    }

    public Account loadAccount() throws Exception {
        try {
            final Account account = storage.load();
            System.out.println("Account geladen: " + account.getUsername());
            return account;
        } catch (final Exception e) {
            System.err.println("Fehler beim Laden des Accounts: " + e.getMessage());
            throw e;
        }
    }

    public void saveAccount(final Account account) throws Exception {
        try {
            storage.save(account);
            System.out.println("Account gespeichert: " + storage.getDefaultFile().getAbsolutePath());
        } catch (final Exception e) {
            System.err.println("Fehler beim Speichern des Accounts: " + e.getMessage());
            throw e;
        }
    }

    public void deleteAccount() throws Exception {
        final java.io.File file = storage.getDefaultFile();
        if (file.exists()) {
            if (!file.delete()) {
                throw new java.io.IOException("Datei konnte nicht gelöscht werden: " + file.getAbsolutePath());
            }
            System.out.println("Account-Datei gelöscht: " + file.getAbsolutePath());
        } else {
            System.out.println("Keine Account-Datei vorhanden zum Löschen: " + file.getAbsolutePath());
        }
    }

    public void addEntry(final Account account, final String masterPassword, final Entry entry) {
        try {
            final SecretKey secretKey = cryptoUtils.deriveEncryptionKey(
                    masterPassword,
                    Base64.getDecoder().decode(account.getEncryptionSaltBase64())
            );

            final Cryptographer cryptographer = new Cryptographer();
            entry.encrypt(secretKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
            account.getEntries().add(entry);
            System.out.println("Eintrag hinzugefügt: " + entry.getEntryName());

        } catch (final Exception exception) {
            System.err.println("Fehler beim Ableiten des Verschlüsselungsschlüssels: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    public void removeEntry(final Account account, final String entryName) {
        final Entry toRemove = account.getEntries().stream()
                .filter(e -> e.getEntryName().equals(entryName))
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            account.getEntries().remove(toRemove);
            System.out.println("Eintrag entfernt: " + entryName);
        } else {
            System.out.println("Eintrag nicht gefunden: " + entryName);
        }
    }

    public void updateEntry(final Account account, final String masterPassword, final Entry updatedEntry) {
        final Entry existing = account.getEntries().stream()
                .filter(e -> e.getEntryName().equals(updatedEntry.getEntryName()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            System.out.println("Eintrag zum Aktualisieren nicht gefunden: " + updatedEntry.getEntryName());
            return;
        }

        try {
            final SecretKey secretKey = cryptoUtils.deriveEncryptionKey(
                    masterPassword,
                    Base64.getDecoder().decode(account.getEncryptionSaltBase64())
            );

            final Cryptographer cryptographer = new Cryptographer();
            // Update fields in existing entry (preserve entryName)
            existing.setEncryptedPassword(updatedEntry.getEncryptedPassword());
            existing.setDescription(updatedEntry.getDescription());
            existing.setUrl(updatedEntry.getUrl());
            existing.setUsername(updatedEntry.getUsername());
            existing.setEmail(updatedEntry.getEmail());
            existing.setNotes(updatedEntry.getNotes());

            // Re-encrypt with current key
            existing.encrypt(secretKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
            System.out.println("Eintrag aktualisiert: " + existing.getEntryName());

        } catch (final Exception exception) {
            System.err.println("Fehler beim Aktualisieren des Eintrags: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

}
