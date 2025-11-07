package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.mlprograms.passwordmax.util.AesCryptographer;

import javax.crypto.SecretKey;

@Getter
@AllArgsConstructor
@ToString
public class Entry {

    private String entryName;
    private String description;
    private String url;

    private String username;
    private String password;
    private String email;
    private String notes;

    public Entry decrypt(final AesCryptographer aesCryptographer) {
        final SecretKey secretKey = aesCryptographer.getSecretKey();
        final byte[] initializationVector = aesCryptographer.getInitializationVector();

        return new Entry(
                entryName,
                aesCryptographer.decrypt(description, secretKey, initializationVector),
                aesCryptographer.decrypt(url, secretKey, initializationVector),
                aesCryptographer.decrypt(username, secretKey, initializationVector),
                aesCryptographer.decrypt(password, secretKey, initializationVector),
                aesCryptographer.decrypt(email, secretKey, initializationVector),
                aesCryptographer.decrypt(notes, secretKey, initializationVector)
        );
    }

    public Entry encryptEntry(final AesCryptographer aesCryptographer) {
        final SecretKey secretKey = aesCryptographer.getSecretKey();
        final byte[] initializationVector = aesCryptographer.getInitializationVector();

        return new Entry(
                entryName,
                aesCryptographer.encrypt(description, secretKey, initializationVector),
                aesCryptographer.encrypt(url, secretKey, initializationVector),
                aesCryptographer.encrypt(username, secretKey, initializationVector),
                aesCryptographer.encrypt(password, secretKey, initializationVector),
                aesCryptographer.encrypt(email, secretKey, initializationVector),
                aesCryptographer.encrypt(notes, secretKey, initializationVector)
        );
    }

}
