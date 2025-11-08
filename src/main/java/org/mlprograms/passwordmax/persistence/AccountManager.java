package org.mlprograms.passwordmax.persistence;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AccountManager {

    private final CryptoUtils cryptoUtils = new CryptoUtils();
    private final byte[] ADDITIONAL_AUTHENTICATED_DATA = "user:1234".getBytes();
    private AccountStorage storage;
    private final Gson gson = new Gson();

    private AccountStorage getStorage() {
        if (this.storage == null) {
            this.storage = new AccountStorage();
        }
        return this.storage;
    }

    // Encrypt the whole entries list and save into account.encryptedEntries, then persist the account
    public void saveAccountEncrypted(final Account account, final String masterPassword) throws Exception {
        if (account == null) throw new IllegalArgumentException("Account is null");

        // if entries present, serialize and encrypt
        List<Entry> original = account.getEntries();
        if (original != null) {
            final String json = gson.toJson(original);
            final byte[] saltBytes = Base64.getDecoder().decode(account.getEncryptionSaltBase64());
            final SecretKey key = cryptoUtils.deriveEncryptionKey(masterPassword, saltBytes);
            final Cryptographer cryptographer = new Cryptographer();
            final String encrypted = cryptographer.encrypt(json, key, ADDITIONAL_AUTHENTICATED_DATA);
            account.setEncryptedEntries(encrypted);
            // temporarily clear plaintext for storage
            account.setEntries(null);
            try {
                getStorage().save(account);
            } finally {
                // restore in-memory entries
                account.setEntries(original);
            }
        } else {
            // nothing to encrypt, just persist
            getStorage().save(account);
        }
    }

    // Decrypt the encryptedEntries blob and populate account.entries
    public void decryptEntries(final Account account, final String masterPassword) throws Exception {
        if (account == null) throw new IllegalArgumentException("Account is null");
        // If there is no encryptedEntries blob, assume entries are stored in plaintext already.
        if (account.getEncryptedEntries() == null) {
            if (account.getEntries() == null) {
                account.setEntries(new ArrayList<>());
            }
            return;
        }

        final byte[] saltBytes = Base64.getDecoder().decode(account.getEncryptionSaltBase64());
        final SecretKey key = cryptoUtils.deriveEncryptionKey(masterPassword, saltBytes);
        final Cryptographer cryptographer = new Cryptographer();
        final String json = cryptographer.decrypt(account.getEncryptedEntries(), key, ADDITIONAL_AUTHENTICATED_DATA);
        final Type listType = new TypeToken<List<Entry>>() {}.getType();
        final List<Entry> list = gson.fromJson(json, listType);
        account.setEntries(list != null ? list : new ArrayList<>());
    }

    public Account createAccount(final String username, final String masterPassword) throws Exception {
        final String verificationHash = cryptoUtils.createVerificationHash(masterPassword);
        final String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();

        return new Account(username, verificationHash, encryptionSaltBase64, new ArrayList<>(), null);
    }

    // Load a single account by username (case-insensitive)
    public Account loadAccount(final String username) throws Exception {
        try {
            final Account acc = getStorage().load(username);
            if (acc == null) {
                throw new IOException("Account nicht gefunden: " + username);
            }
            return acc;
        } catch (final Exception exception) {
            System.err.println("Fehler beim Laden des Accounts: " + exception.getMessage());
            throw exception;
        }
    }

    public void saveAccount(final Account account) throws Exception {
        try {
            getStorage().save(account);
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
        } else {
            System.out.println("Keine Account-Datei vorhanden zum Löschen: " + file.getAbsolutePath());
        }
    }

    public void addEntry(final Account account, final String masterPassword, final Entry entry) {
        if (entry == null || entry.getEntryName() == null) {
            System.err.println("Ungültiger Eintrag: Eintrag oder Eintragsname ist null");
            return;
        }

        if (account.getEntries() == null) {
            account.setEntries(new ArrayList<>());
        }

        final boolean exists = account.getEntries().stream()
                .anyMatch(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(entry.getEntryName()));
        if (exists) {
            System.out.println("Eintrag mit Namen bereits vorhanden, überspringe: " + entry.getEntryName());
            return;
        }

        try {
            // Store entry as plain in-memory; full-list encryption happens on saveAccountEncrypted
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

        if (account.getEntries() == null) {
            account.setEntries(new ArrayList<>());
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

    public boolean removeEntry(final Account account, final String entryName) {
        if (entryName == null) return false;
        if (account.getEntries() == null) return false;
        final int idx = findEntryIndex(account, entryName);
        if (idx >= 0) {
            account.getEntries().remove(idx);
            return true;
        }
        return false;
    }

    private int findEntryIndex(final Account account, final String entryName) {
        if (account.getEntries() == null) return -1;
        for (int i = 0; i < account.getEntries().size(); i++) {
            final Entry e = account.getEntries().get(i);
            if (e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(entryName)) {
                return i;
            }
        }
        return -1;
    }

    public void updateEntry(final Account account, final String masterPassword, final String originalEntryName, final Entry updatedEntry) {
        final Entry existing = account.getEntries().stream()
                .filter(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(originalEntryName))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            System.out.println("Eintrag zum Aktualisieren nicht gefunden: " + originalEntryName);
            return;
        }

        // Wenn der Name geändert wurde, prüfen wir, ob es einen Konflikt mit einem anderen Eintrag gibt
        if (updatedEntry.getEntryName() != null && !updatedEntry.getEntryName().equalsIgnoreCase(originalEntryName)) {
            final boolean conflict = account.getEntries().stream()
                    .anyMatch(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(updatedEntry.getEntryName()));
            if (conflict) {
                System.out.println("Eintrag mit dem neuen Namen existiert bereits: " + updatedEntry.getEntryName());
                return;
            }
        }

        // Update fields in-memory; encryption is handled at save time via saveAccountEncrypted
        existing.setEntryName(updatedEntry.getEntryName());
        existing.setEncryptedPassword(updatedEntry.getEncryptedPassword());
        existing.setDescription(updatedEntry.getDescription());
        existing.setUrl(updatedEntry.getUrl());
        existing.setUsername(updatedEntry.getUsername());
        existing.setEmail(updatedEntry.getEmail());
        existing.setNotes(updatedEntry.getNotes());
    }

}
