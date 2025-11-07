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

    public Account addAccount(final String username, final String masterPassword) throws Exception {
        final String verificationHash = cryptoUtils.createVerificationHash(masterPassword);
        final String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();

        return new Account(username, verificationHash, encryptionSaltBase64, new ArrayList<>());
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

        } catch (final Exception exception) {
            System.err.println("Fehler beim Ableiten des Verschlüsselungsschlüssels: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

}
