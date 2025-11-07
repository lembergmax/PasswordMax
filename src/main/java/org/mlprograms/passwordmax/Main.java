package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.persistence.AccountManager;
import org.mlprograms.passwordmax.persistence.AccountStorage;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class Main {

    private static final CryptoUtils cryptoUtils = new CryptoUtils();
    private static final Cryptographer cryptographer = new Cryptographer();
    private static final AccountStorage accountStorage = new AccountStorage();
    private static final byte[] ADDITIONAL_AUTHENTICATED_DATA = "user:1234".getBytes();

    public static void main(final String[] args) throws Exception {
        final AccountManager accountManager = new AccountManager();

        final String masterPassword = "Masterpasswort";

        final Account account = ;
        accountManager.addAccount(masterPassword);

        addSampleData(account, masterPassword);

        saveAccount(account);

        final SecretKey aesKey = login(masterPassword, account);
        displayVault(account, aesKey);
    }

    private static void saveAccount(final Account account) {
        try {
            accountStorage.save(account);
            System.out.println("Account gespeichert: " + accountStorage.getDefaultFile().getAbsolutePath());
        } catch (final Exception e) {
            System.out.println("Speichern nicht möglich: " + e.getMessage());
        }
    }

    private static SecretKey login(final String masterPassword, final Account account) throws Exception {
        if (!cryptoUtils.verifyPassword(masterPassword, account.getVerificationHash())) {
            throw new IllegalArgumentException("Falsches Passwort!");
        }

        return cryptoUtils.deriveEncryptionKey(
                masterPassword,
                Base64.getDecoder().decode(account.getEncryptionSaltBase64())
        );
    }

    private static void displayVault(final Account account, final SecretKey aesKey) throws Exception {
        for (final Entry entry : account.getEntries()) {
            entry.decrypt(aesKey, ADDITIONAL_AUTHENTICATED_DATA, cryptographer);
            System.out.println("Eintrag: " + entry.getEntryName());
            System.out.println("  Passwort: " + entry.getEncryptedPassword());
            System.out.println("  URL: " + entry.getUrl());
            System.out.println("  Username: " + entry.getUsername());
            System.out.println();
        }
    }

}
