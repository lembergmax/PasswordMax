package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.mlprograms.passwordmax.util.AesCryptographer;

import javax.crypto.SecretKey;
import java.util.List;

@Getter
@AllArgsConstructor
public class Account {

    private String name;
    private String password;
    private List<Entry> entries;

    public Account(String name, String password) {
        this.name = name;
        this.password = password;
        this.entries = List.of();
    }

    public String getDecryptedPassword() {
        final AesCryptographer aesCryptographer = new AesCryptographer(name);
        final SecretKey secretKey = aesCryptographer.getSecretKey();
        final byte[] initializationVector = aesCryptographer.getInitializationVector();
        return aesCryptographer.decrypt(password, secretKey, initializationVector);
    }

    public Entry getDecryptedEntryByEntryName(final String entryName) {
        final AesCryptographer aesCryptographer = new AesCryptographer(name);

        for (final Entry entry : entries) {
            if (entry.getEntryName().equals(entryName)) {
                return entry.decrypt(aesCryptographer);
            }
        }

        throw new RuntimeException("Es wurde kein Eintrag mit dem Namen '" + entryName + "' gefunden.");

    }

    public Entry getEncryptedEntryByEntryName(final String entryName) {
        final AesCryptographer aesCryptographer = new AesCryptographer(name);

        for (final Entry entry : entries) {
            if (entry.getEntryName().equals(entryName)) {
                return entry.encryptEntry(aesCryptographer);
            }
        }

        throw new RuntimeException("Es wurde kein Eintrag mit dem Namen '" + entryName + "' gefunden.");
    }

}
