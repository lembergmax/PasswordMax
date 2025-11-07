package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.model.AccountData;
import org.mlprograms.passwordmax.model.EntryData;
import org.mlprograms.passwordmax.persistence.AccountStorage;
import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

public class Main {

    public static void main(final String[] args) throws Exception {
        final CryptoUtils cryptoUtils = new CryptoUtils();
        final Cryptographer cryptographer = new Cryptographer();
        final AccountStorage accountStorage = new AccountStorage();

        final String filePath = "account.json";

        // === Registrierung: Masterpasswort einmalig ===
        final char[] masterPassword = "SehrSicheresMasterPasswort#2025".toCharArray();
        final String verificationHash = cryptoUtils.createVerificationHash(masterPassword);
        final String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();
        cryptoUtils.wipe(masterPassword);

        // === Login / Schlüsselableitung ===
        final char[] loginAttempt = "SehrSicheresMasterPasswort#2025".toCharArray();
        if (!cryptoUtils.verifyPassword(loginAttempt, verificationHash)) {
            System.out.println("Falsches Passwort!");
            cryptoUtils.wipe(loginAttempt);
            return;
        }

        final SecretKey aesKey = cryptoUtils.deriveEncryptionKey(loginAttempt, Base64.getDecoder().decode(encryptionSaltBase64));
        cryptoUtils.wipe(loginAttempt);

        final byte[] additionalAuthenticationData = "user:1234".getBytes();

        // === Beispiel Vault-Eintrag erstellen und verschlüsseln ===
        final EntryData vaultEntry = new EntryData(
                "Bank",
                "MeinSuperGeheimesPasswortFürBank",
                "Bank-Konto",
                "https://bank.example.com",
                "maxuser",
                "max@example.com",
                "Keine Notizen"
        );
        vaultEntry.encrypt(aesKey, additionalAuthenticationData, cryptographer);

        // === AccountData erstellen ===
        final AccountData accountData = new AccountData(
                "max",
                verificationHash,
                encryptionSaltBase64,
                List.of(vaultEntry)
        );

        // === Speichern ===
        accountStorage.save(accountData, filePath);
        System.out.println("Account gespeichert: " + filePath);

        // === Laden + Entschlüsseln ===
        final AccountData loadedAccount = accountStorage.load(filePath);
        final SecretKey reloadedAesKey = cryptoUtils.deriveEncryptionKey(
                "SehrSicheresMasterPasswort#2025".toCharArray(),
                Base64.getDecoder().decode(loadedAccount.getEncryptionSaltBase64())
        );

        final EntryData decryptedEntry = loadedAccount.getEntries().getFirst();
        decryptedEntry.decrypt(reloadedAesKey, additionalAuthenticationData, cryptographer);

        System.out.println("Entschlüsseltes Passwort: " + decryptedEntry.getEncryptedPassword());
        System.out.println("Entschlüsselte URL: " + decryptedEntry.getUrl());
        System.out.println("Entschlüsselter Username: " + decryptedEntry.getUsername());
    }

}