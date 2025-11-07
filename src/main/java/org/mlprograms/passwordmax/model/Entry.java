package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Entry {

    private String entryName;
    private String description;
    private String url;
    private String username;
    private String password;
    private String email;
    private String notes;

}
