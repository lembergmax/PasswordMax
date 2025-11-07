package org.mlprograms.passwordmax.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.mlprograms.passwordmax.model.Account;
import org.mlprograms.passwordmax.model.Entry;
import org.mlprograms.passwordmax.util.AesCryptographer;
import org.mlprograms.passwordmax.util.FolderController;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class JsonController {

    public static final String DATA_JSON_FILE = "data.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path jsonPath;

    public JsonController() {
        final FolderController folderController = new FolderController();
        folderController.createAppFolders();

        final Path appFolder = folderController.getAppFolder();
        this.jsonPath = appFolder.resolve(DATA_JSON_FILE);
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

    public void addAccount(final Account accountToAdd) {
        final List<Account> accounts = loadDataFromJson();
        final boolean existsAccount = accounts.stream()
                .anyMatch(account -> account.getName().equalsIgnoreCase(accountToAdd.getName()));

        if (existsAccount) {
            log.warn("Konto '{}' existiert bereits", accountToAdd.getName());
            return;
        }

        final AesCryptographer aesCryptographer = new AesCryptographer(accountToAdd.getName());

        accounts.add(new Account(
                accountToAdd.getName(),
                aesCryptographer.encrypt(accountToAdd.getPassword(), aesCryptographer.getSecretKey(), aesCryptographer.getInitializationVector()),
                new ArrayList<>()));
        saveDataToJson(accounts);
        log.info("Konto '{}' wurde hinzugefügt", accountToAdd.getName());
    }

    public void addPasswordToAccount(final String accountName, final Entry entryToAdd) {
        final List<Account> accounts = loadDataFromJson();
        final Optional<Account> optionalAccount = accounts.stream().filter(acc -> acc.getName().equals(accountName)).findFirst();

        if (optionalAccount.isEmpty()) {
            log.warn("Konto '{}' nicht gefunden – Passwort konnte nicht hinzugefügt werden", accountName);
            return;
        }

        final Account account = optionalAccount.get();
        final boolean existsEntry = account.getEntries().stream()
                .anyMatch(entry -> entry.getEntryName().equals(entryToAdd.getEntryName()));

        if (existsEntry) {
            log.warn("Passwort-Eintrag '{}' existiert bereits in Konto '{}'", entryToAdd.getEntryName(), accountName);
            return;
        }

        account.getEntries().add(entryToAdd);
        saveDataToJson(accounts);
        log.info("Passwort zu Konto '{}' hinzugefügt", accountName);
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

}
