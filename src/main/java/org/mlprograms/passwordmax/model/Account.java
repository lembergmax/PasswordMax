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

}
