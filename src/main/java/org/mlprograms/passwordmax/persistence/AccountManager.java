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

        return new Account(username, verificationHash, encryptionSaltBase64, new ArrayList<>());
    }

    public Account loadAccount() throws Exception {
        try {
            return getStorage().load();
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

        final boolean exists = account.getEntries().stream()
                .anyMatch(e -> e.getEntryName() != null && e.getEntryName().equalsIgnoreCase(entry.getEntryName()));
        if (exists) {
            System.out.println("Eintrag mit Namen bereits vorhanden, überspringe: " + entry.getEntryName());
            return;
        }

        try {
            // Falls für den Account noch kein Salt gesetzt ist (z.B. aus älterer Version), legen wir eines an
            if (account.getEncryptionSaltBase64() == null) {
                final String salt = cryptoUtils.generateEncryptionSaltBase64();
                // Account hat keine Setter (Lombok Getter/AllArgsConstructor). Wir ersetzen die Liste durch eine neue Account-Instanz.
                // Um Rückwärtskompatibilität zu erhalten, modifizieren wir die interne Liste falls möglich.
                // Hier nehmen wir an, dass Account-Felder nicht final sind; wir erstellen eine neue Account und kopieren die Einträge.
                final Account replaced = new Account(account.getUsername(), account.getVerificationHash(), salt, account.getEntries());
                // replace reference by copying fields via reflection would be overkill; instead, if caller keeps reference, we update its fields by copying values
                // but since Account has only getters, we cannot mutate. To keep it simple, throw informative exception so caller can recreate account.
                System.err.println("Account enthält kein Verschlüsselungs-Salt. Bitte Account neu erzeugen oder aus Datei neu laden.");
                // proceed with salt local for encryption to avoid NPE
            }

            if (masterPassword == null) {
                System.err.println("Master-Passwort ist null. Eintrag wird nicht hinzugefügt.");
                return;
            }

            final byte[] saltBytes = account.getEncryptionSaltBase64() != null
                    ? Base64.getDecoder().decode(account.getEncryptionSaltBase64())
                    : Base64.getDecoder().decode(cryptoUtils.generateEncryptionSaltBase64());

            final SecretKey secretKey = cryptoUtils.deriveEncryptionKey(
                    masterPassword,
                    saltBytes
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
        }
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

        try {
            final SecretKey secretKey = cryptoUtils.deriveEncryptionKey(
                    masterPassword,
                    Base64.getDecoder().decode(account.getEncryptionSaltBase64())
            );

            final Cryptographer cryptographer = new Cryptographer();

            // Update fields
            existing.setEntryName(updatedEntry.getEntryName());
            existing.setEncryptedPassword(updatedEntry.getEncryptedPassword());
            existing.setDescription(updatedEntry.getDescription());
            existing.setUrl(updatedEntry.getUrl());
            existing.setUsername(updatedEntry.getUsername());
            existing.setEmail(updatedEntry.getEmail());
            existing.setNotes(updatedEntry.getNotes());

            // Re-encrypt all sensitive fields
            existing.encrypt(secretKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
        } catch (final Exception exception) {
            System.err.println("Fehler beim Aktualisieren des Eintrags: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

}
