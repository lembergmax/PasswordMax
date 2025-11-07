package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountData {

    private String username;
    private String verificationHash; // Masterpasswort Hash
    private String encryptionSaltBase64; // für AES-Schlüssel
    private List<EntryData> entries;

}
