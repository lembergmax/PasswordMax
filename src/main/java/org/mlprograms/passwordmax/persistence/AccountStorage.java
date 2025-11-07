package org.mlprograms.passwordmax.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mlprograms.passwordmax.model.AccountData;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AccountStorage {

    private final Gson gson;

    public AccountStorage() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void save(AccountData accountData, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(accountData, writer);
        }
    }

    public AccountData load(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, AccountData.class);
        }
    }

}

