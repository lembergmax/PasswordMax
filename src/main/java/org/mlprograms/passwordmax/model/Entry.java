package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Entry {

    private String entryName; // bleibt im Klartext
    private String encryptedPassword;
    private String description;
    private String url;
    private String username;
    private String email;
    private String notes;

    public void encrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        this.encryptedPassword = cryptographer.encrypt(this.encryptedPassword, aesKey, additionalAuthenticationData);
        this.description = cryptographer.encrypt(this.description, aesKey, additionalAuthenticationData);
        this.url = cryptographer.encrypt(this.url, aesKey, additionalAuthenticationData);
        this.username = cryptographer.encrypt(this.username, aesKey, additionalAuthenticationData);
        this.email = cryptographer.encrypt(this.email, aesKey, additionalAuthenticationData);
        this.notes = cryptographer.encrypt(this.notes, aesKey, additionalAuthenticationData);
    }

    public void decrypt(final SecretKey aesKey, final byte[] additionalAuthenticationData, final Cryptographer cryptographer) throws Exception {
        this.encryptedPassword = cryptographer.decrypt(this.encryptedPassword, aesKey, additionalAuthenticationData);
        this.description = cryptographer.decrypt(this.description, aesKey, additionalAuthenticationData);
        this.url = cryptographer.decrypt(this.url, aesKey, additionalAuthenticationData);
        this.username = cryptographer.decrypt(this.username, aesKey, additionalAuthenticationData);
        this.email = cryptographer.decrypt(this.email, aesKey, additionalAuthenticationData);
        this.notes = cryptographer.decrypt(this.notes, aesKey, additionalAuthenticationData);
    }

}
