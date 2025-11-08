package org.mlprograms.passwordmax.model;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Account {

    private String username;
    private String verificationHash;
    private String encryptionSaltBase64;
    private List<Entry> entries;
    private String encryptedEntries; // base64-encoded encrypted JSON blob of entries

}
