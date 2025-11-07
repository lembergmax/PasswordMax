package org.mlprograms.passwordmax.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class Account {

    private String username;
    private String verificationHash;
    private String encryptionSaltBase64;
    private List<Entry> entries;

}
