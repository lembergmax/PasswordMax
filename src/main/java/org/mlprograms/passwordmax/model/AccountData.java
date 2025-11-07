package org.mlprograms.passwordmax.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class AccountData {

    private final String username;
    private final String verificationHash;
    private final String encryptionSaltBase64;
    private final List<EntryData> entries;

}
