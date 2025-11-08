package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class Entry {

    private String entryName; // plaintext (will also support encrypted storage)
    private String encryptedEntryName; // when entryName is stored encrypted
    private String encryptedPassword;
    private String description;
    private String url;
    private String username;
    private String email;
    private String notes;

    public void encrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        if (this.entryName != null) {
            this.encryptedEntryName = cryptographer.encrypt(this.entryName, aesKey, additionalAuthenticationData);
            this.entryName = null; // clear plaintext
        }
        if (this.encryptedPassword != null) {
            this.encryptedPassword = cryptographer.encrypt(this.encryptedPassword, aesKey, additionalAuthenticationData);
        }
        if (this.description != null) {
            this.description = cryptographer.encrypt(this.description, aesKey, additionalAuthenticationData);
        }
        if (this.url != null) {
            this.url = cryptographer.encrypt(this.url, aesKey, additionalAuthenticationData);
        }
        if (this.username != null) {
            this.username = cryptographer.encrypt(this.username, aesKey, additionalAuthenticationData);
        }
        if (this.email != null) {
            this.email = cryptographer.encrypt(this.email, aesKey, additionalAuthenticationData);
        }
        if (this.notes != null) {
            this.notes = cryptographer.encrypt(this.notes, aesKey, additionalAuthenticationData);
        }
    }

    public void decrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        if (this.encryptedEntryName != null) {
            this.entryName = cryptographer.decrypt(this.encryptedEntryName, aesKey, additionalAuthenticationData);
        }
        if (this.encryptedPassword != null) {
            this.encryptedPassword = cryptographer.decrypt(this.encryptedPassword, aesKey, additionalAuthenticationData);
        }
        if (this.description != null) {
            this.description = cryptographer.decrypt(this.description, aesKey, additionalAuthenticationData);
        }
        if (this.url != null) {
            this.url = cryptographer.decrypt(this.url, aesKey, additionalAuthenticationData);
        }
        if (this.username != null) {
            this.username = cryptographer.decrypt(this.username, aesKey, additionalAuthenticationData);
        }
        if (this.email != null) {
            this.email = cryptographer.decrypt(this.email, aesKey, additionalAuthenticationData);
        }
        if (this.notes != null) {
            this.notes = cryptographer.decrypt(this.notes, aesKey, additionalAuthenticationData);
        }
    }

}
