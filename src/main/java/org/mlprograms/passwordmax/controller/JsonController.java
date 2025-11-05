package org.mlprograms.passwordmax.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Password;
import org.mlprograms.passwordmax.util.FolderController;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JsonController {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path jsonPath;

    public JsonController() {
        final FolderController folderController = new FolderController();
        folderController.createKeyFolder();

        final Path appFolder = folderController.getAppFolder();
        this.jsonPath = appFolder.resolve("data.json");
        createJsonFileIfNecessary();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createJsonFileIfNecessary() {
        final File file = jsonPath.toFile();
        try {
            file.createNewFile();

            if (file.length() == 0) {
                Files.writeString(jsonPath, "[]");
            }
        } catch (final Exception e) {
            log.error("Fehler beim erstellen der JSON-Datei: {}", jsonPath, e);
        }
    }

    public List<Account> loadDataFromJson() {
        try (final FileReader fileReader = new FileReader(jsonPath.toFile())) {
            final Type listType = new TypeToken<List<Account>>() {
            }.getType();
            final List<Account> accounts = gson.fromJson(fileReader, listType);

            return accounts != null ? accounts : new ArrayList<>();
        } catch (final Exception exception) {
            log.error("Fehler beim Laden der JSON-Daten", exception);
            return new ArrayList<>();
        }
    }

    private void saveDataToJson(final List<Account> accounts) {
        try (final FileWriter writer = new FileWriter(jsonPath.toFile())) {
            gson.toJson(accounts, writer);
        } catch (final Exception exception) {
            log.error("Fehler beim Speichern der JSON-Daten", exception);
            throw new RuntimeException("Fehler beim Speichern der JSON-Daten", exception);
        }
    }

    public void addAccount(final Account account) {
        final List<Account> accounts = loadDataFromJson();
        final boolean exists = accounts.stream()
                .anyMatch(acc -> acc.getName().equalsIgnoreCase(account.getName()));

        if (exists) {
            log.warn("Konto '{}' existiert bereits", account.getName());
            return;
        }

        accounts.add(new Account(account.getName(), account.getPassword(), new ArrayList<>()));
        saveDataToJson(accounts);
        log.info("Konto '{}' wurde hinzugefügt", account.getName());
    }

    public void addPasswordToAccount(final String accountName, final Password password) {
        final List<Account> accounts = loadDataFromJson();

        for (final Account account : accounts) {
            if (account.getName().equalsIgnoreCase(accountName)) {
                account.getPasswords().add(password);
                saveDataToJson(accounts);
                log.info("Passwort zu Konto '{}' hinzugefügt", accountName);
                return;
            }
        }

        log.warn("Konto '{}' nicht gefunden – Passwort konnte nicht hinzugefügt werden", accountName);
    }

}
