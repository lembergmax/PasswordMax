package org.mlprograms.passwordmax.persistence;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AccountManager {

    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final byte[] ADDITIONAL_AUTHENTICATED_DATA = "user:1234".getBytes();
    private AccountStorage storage;

    private AccountStorage getStorage() {
        if (this.storage == null) {
            this.storage = new AccountStorage();
        }
        return this.storage;
    }

    public Account createAccount(final String username, final String masterPassword) throws Exception {
        final String verificationHash = cryptoUtils.createVerificationHash(masterPassword);
        final String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();

        final Account account = new Account(username, verificationHash, encryptionSaltBase64, new ArrayList<>());
        System.out.println("Account erstellt: " + username);
        return account;
    }

    public Account loadAccount() throws Exception {
        try {
            final Account account = getStorage().load();
            System.out.println("Account geladen: " + account.getUsername());
            return account;
        } catch (final Exception exception) {
            System.err.println("Fehler beim Laden des Accounts: " + exception.getMessage());
            throw exception;
        }
    }

    public void saveAccount(final Account account) throws Exception {
        try {
            getStorage().save(account);
            System.out.println("Account gespeichert: " + getStorage().getDefaultFile().getAbsolutePath());
        } catch (final Exception exception) {
            System.err.println("Fehler beim Speichern des Accounts: " + exception.getMessage());
            throw exception;
        }
    }

    public void deleteAccount() throws Exception {
        final File file = getStorage().getDefaultFile();
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Datei konnte nicht gelöscht werden: " + file.getAbsolutePath());
            }
            System.out.println("Account-Datei gelöscht: " + file.getAbsolutePath());
        } else {
            System.out.println("Keine Account-Datei vorhanden zum Löschen: " + file.getAbsolutePath());
        }
    }

    public void addEntry(final Account account, final String masterPassword, final Entry entry) {
        if (entry == null || entry.getEntryName() == null) {
            System.err.println("Ungültiger Eintrag: Eintrag oder Eintragsname ist null");
            return;
        }

        final boolean exists = account.getEntries().stream()
                .anyMatch(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(entry.getEntryName()));
        if (exists) {
            System.out.println("Eintrag mit Namen bereits vorhanden, überspringe: " + entry.getEntryName());
            return;
        }

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

    public void addEntries(final Account account, final String masterPassword, final List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            System.out.println("Keine Einträge zum Hinzufügen übergeben.");
            return;
        }

        int added = 0;
        int skipped = 0;
        for (final Entry entry : entries) {
            final int before = account.getEntries().size();
            addEntry(account, masterPassword, entry);

            final int after = account.getEntries().size();
            if (after > before) {
                added++;
            } else {
                skipped++;
            }
        }

        System.out.println("Batch-Hinzufügen abgeschlossen: " + added + " hinzugefügt, " + skipped + " übersprungen.");
    }

    public void removeEntry(final Account account, final String entryName) {
        final Entry toRemove = account.getEntries().stream()
                .filter(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(entryName))
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
                .filter(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(updatedEntry.getEntryName()))
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
            existing.setEncryptedPassword(updatedEntry.getEncryptedPassword());
            existing.setDescription(updatedEntry.getDescription());
            existing.setUrl(updatedEntry.getUrl());
            existing.setUsername(updatedEntry.getUsername());
            existing.setEmail(updatedEntry.getEmail());
            existing.setNotes(updatedEntry.getNotes());

            existing.encrypt(secretKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
            System.out.println("Eintrag aktualisiert: " + existing.getEntryName());

        } catch (final Exception exception) {
            System.err.println("Fehler beim Aktualisieren des Eintrags: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

}
