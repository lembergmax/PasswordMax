package org.mlprograms.passwordmax.controller;

import lombok.extern.slf4j.Slf4j;
import org.mlprograms.passwordmax.model.Password;

import java.io.File;
import java.util.List;

@Slf4j
public class JsonController {

    private final String JSON_FILE_PATH = "";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createJsonFileIfNecessary() {
        final File file = new File(JSON_FILE_PATH);
        try {
            file.createNewFile();
        } catch (final Exception e) {
            log.error("Fehler beim erstellen der JSON-Datei: {}", JSON_FILE_PATH, e);
        }
    }

    public List<Password> loadDataFromJsonForAccountAndPassword() {
        // TODO:
        //  1. Read the JSON file from the designated directory.
        //  2. Parse the JSON data to extract account and password information.
        //  3. Create and return a list of Password objects based on the parsed data
        return List.of();
    }

}
