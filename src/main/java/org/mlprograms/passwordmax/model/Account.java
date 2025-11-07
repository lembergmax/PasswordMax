package org.mlprograms.passwordmax.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class Account {

    private final String username;
    private final String verificationHash;
    private final String encryptionSaltBase64;
    private final List<Entry> entries;

}
