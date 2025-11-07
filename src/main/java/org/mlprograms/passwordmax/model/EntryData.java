package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EntryData {

    private String entryName; // bleibt im Klartext
    private String encryptedPassword;
    private String description;
    private String url;
    private String username;
    private String email;
    private String notes;

    public void encrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        this.encryptedPassword = cryptographer.encrypt(aesKey, this.encryptedPassword, additionalAuthenticationData);
        this.description = cryptographer.encrypt(aesKey, this.description, additionalAuthenticationData);
        this.url = cryptographer.encrypt(aesKey, this.url, additionalAuthenticationData);
        this.username = cryptographer.encrypt(aesKey, this.username, additionalAuthenticationData);
        this.email = cryptographer.encrypt(aesKey, this.email, additionalAuthenticationData);
        this.notes = cryptographer.encrypt(aesKey, this.notes, additionalAuthenticationData);
    }

    public void decrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        this.encryptedPassword = cryptographer.decrypt(aesKey, this.encryptedPassword, additionalAuthenticationData);
        this.description = cryptographer.decrypt(aesKey, this.description, additionalAuthenticationData);
        this.url = cryptographer.decrypt(aesKey, this.url, additionalAuthenticationData);
        this.username = cryptographer.decrypt(aesKey, this.username, additionalAuthenticationData);
        this.email = cryptographer.decrypt(aesKey, this.email, additionalAuthenticationData);
        this.notes = cryptographer.decrypt(aesKey, this.notes, additionalAuthenticationData);
    }

}
