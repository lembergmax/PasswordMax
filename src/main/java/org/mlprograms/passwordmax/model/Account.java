package org.mlprograms.passwordmax.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Account {

    private String name;
    private String password;
    private List<Entry> entries;

    public Account(String name, String password) {
        this.name = name;
        this.password = password;
        this.entries = List.of();
    }

}
