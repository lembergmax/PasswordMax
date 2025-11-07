package org.mlprograms.passwordmax;

import org.mlprograms.passwordmax.security.CryptoUtils;
import org.mlprograms.passwordmax.security.Cryptographer;

import javax.crypto.SecretKey;
import java.util.Base64;

public class Main {

    public static void main(String[] args) throws Exception {
        final CryptoUtils cryptoUtils = new CryptoUtils();
        final Cryptographer cryptographer = new Cryptographer();

        // === Registrierung (einmalig) ===
        char[] masterPassword = "SehrSicheresMasterPasswort#2025".toCharArray();
        String verificationString = cryptoUtils.createVerificationHash(masterPassword);
        // Speichere verificationString in DB: z.B. "v1:200000:BASE64(salt):BASE64(hash)"
        System.out.println("Store verification (DB): " + verificationString);

        // Generiere encryptionSalt (einmalig pro Benutzer) und speichere Base64 davon
        String encryptionSaltBase64 = cryptoUtils.generateEncryptionSaltBase64();
        System.out.println("Store encryptionSalt (DB): " + encryptionSaltBase64);

        // wipe registration master variable (beispielhaft)
        cryptoUtils.wipe(masterPassword);

        // === Login / Unlock (bei jedem Login) ===
        char[] attempt = "SehrSicheresMasterPasswort#2025".toCharArray();
        boolean ok = cryptoUtils.verifyPassword(attempt, verificationString);
        System.out.println("Password OK? " + ok);

        if (!ok) {
            cryptoUtils.wipe(attempt);
            return;
        }

        // Schlüssel ableiten (aus attempt und stored encryptionSalt)
        byte[] encryptionSalt = Base64.getDecoder().decode(encryptionSaltBase64);
        SecretKey aesKey = cryptoUtils.deriveEncryptionKey(attempt, encryptionSalt);

        // Wipe attempt asap
        cryptoUtils.wipe(attempt);

        // === Vault Operation: verschlüssele einen Eintrag ===
        String secretEntry = "MeinSuperGeheimesPasswortFürBank";
        // optional: AAD z. B. userId oder entryId als bytes (bindet Kontext an Verschlüsselung)
        byte[] aad = "user:1234".getBytes();

        String cipherText = cryptographer.encrypt(aesKey, secretEntry, aad);
        System.out.println("Encrypted vault entry (store this): " + cipherText);

        // später: entschlüsseln
        String decrypted = cryptographer.decrypt(aesKey, cipherText, aad);
        System.out.println("Decrypted: " + decrypted);

        // Wipe key material where possible (SecretKeySpec lacks explicit wipe)
        // Hinweis: in Java kannst du key material nicht immer sicher löschen; Platform-spezifische Maßnahmen nötig.
    }

}